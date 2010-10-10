/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2010 Kai Krueger
 */
package de.ueller.osmToGpsMid;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.TurnRestriction;
import de.ueller.osmToGpsMid.model.Way;

public abstract class OsmParser {
	/**
	 * Maps id to already read nodes. Key: Long Value: Node
	 */
	protected HashMap<Long, Node> nodes = new HashMap<Long, Node>(80000, 0.60f);
	protected Vector<Node> nodes2 = null;
	protected HashMap<Long, Way> ways = new HashMap<Long, Way>();
	protected HashMap<Long, Relation> relations = new HashMap<Long, Relation>();
	protected HashMap<Long, TurnRestriction> turnRestrictions = new HashMap<Long, TurnRestriction>();
	protected ArrayList<TurnRestriction> turnRestrictionsWithViaWays = new ArrayList<TurnRestriction>();
	private Node[] delayingNodes;
	public int trafficSignalCount = 0;
	private Vector<Bounds> bounds = null;
	private Configuration configuration;

	protected int wayIns;

	/**
	 * @param i
	 *            InputStream from which planet file is read
	 */
	public OsmParser(InputStream i) {
		System.out.println(parserType() + " parser started...");
		configuration = new Configuration();
		init(i);
	}

	/**
	 * @param i
	 *            InputStream from which planet file is read
	 * @param c
	 *            Configuration which supplies the bounds
	 */
	public OsmParser(InputStream i, Configuration c) {
		this.configuration = c;
		this.bounds = c.getBounds();
		System.out.println(parserType() + " parser with bounds started...");
		init(i);
	}

	protected abstract String parserType();

	protected abstract void init(InputStream i);

	protected boolean nodeInArea(float lat, float lon) {
		boolean inBound = false;

		if (configuration.getArea() != null
				&& configuration.getArea().contains(lat, lon)) {
			inBound = true;
		}
		if (bounds != null && bounds.size() != 0) {
			for (Bounds b : bounds) {
				if (b.isIn(lat, lon)) {
					inBound = true;
					break;
				}
			}
		}
		if ((bounds == null || bounds.size() == 0)
				&& configuration.getArea() == null) {
			inBound = true;
		}

		return inBound;
	}

	/**
	 * @param viaNodeOrWayRef
	 * @param turnRestriction
	 */
	public void addTurnRestriction(long viaNodeOrWayRef,
			TurnRestriction turnRestriction) {
		if (!turnRestrictions.containsKey(new Long(viaNodeOrWayRef))) {
			turnRestrictions.put(new Long(viaNodeOrWayRef), turnRestriction);
			// System.out.println("Put turn restrictions at " +
			// viaNodeOrWayRef);
		} else {
			TurnRestriction baseTurnRestriction = (TurnRestriction) turnRestrictions
					.get(new Long(viaNodeOrWayRef));
			while (baseTurnRestriction.nextTurnRestrictionAtThisNode != null) {
				baseTurnRestriction = baseTurnRestriction.nextTurnRestrictionAtThisNode;
			}
			baseTurnRestriction.nextTurnRestrictionAtThisNode = turnRestriction;
			// System.out.println("Multiple turn restrictions at " +
			// viaNodeOrWayRef);
		}
	}

	/**
	 * @param w
	 */
	public void addWay(Way w) {
		byte t = w.getType(configuration);
		/**
		 * We seem to have a bit of a mess with respect to type -1 and 0. Both
		 * are used to indicate invalid type it seems.
		 */
		if (w.isValid() /* && t > 0 */) {
			if (ways.get(w.id) != null) {
				/**
				 * This way is already in data storage. This results from
				 * splitting a single osm way into severals GpsMid ways. We can
				 * simply invent an id in this case, as we currently don't use
				 * them for anything other than checking if an id is valid for
				 * use in relations
				 */
				ways.put(new Long(-1 * wayIns), w);
			} else {
				ways.put(w.id, w);
			}
			wayIns++;
		}
	}

	public void removeWay(Way w) {
		ways.remove(w.id);
	}

	public Collection<Node> getNodes() {
		if (nodes == null) {
			return nodes2;
		} else {
			return nodes.values();
		}
	}
	
	/**
	 * WARNING: This function may return null, after dropHashMap has been called
	 * @return
	 */
	public HashMap<Long,Node> getNodeHashMap() { 
		return nodes; 
	}

	public Collection<Way> getWays() {
		return ways.values();
	}

	public Collection<Relation> getRelations() {
		return relations.values();
	}

	public HashMap<Long, TurnRestriction> getTurnRestrictionHashMap() {
		return turnRestrictions;
	}

	public Node[] getDelayingNodes() {
		return delayingNodes;
	}

	public void freeUpDelayingNodes() {
		delayingNodes = null;
	}

	public void setDelayingNodes(Node[] nodes) {
		delayingNodes = nodes;
	}

	public ArrayList<TurnRestriction> getTurnRestrictionsWithViaWays() {
		return turnRestrictionsWithViaWays;
	}

	public HashMap<Long, Way> getWayHashMap() {
		return ways;
	}

	public void removeNodes(Collection<Node> nds) {
		if (nodes == null) {
			// This operation appears rather slow,
			// so try and avoid calling remove nodes once it is in the nodes2
			// format
			nodes2.removeAll(nds);
		} else {
			for (Node n : nds) {
				nodes.remove(new Long(n.id));
			}
		}
	}

	/**
	 * 
	 */
	public void resize() {
		System.gc();
		System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
		System.out.println("Resizing nodes HashMap");
		if (nodes == null) {
			nodes2 = new Vector<Node>(nodes2);
		} else {
			nodes = new HashMap<Long, Node>(nodes);
		}
		relations = new HashMap<Long, Relation>(relations);
		System.gc();
		System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
	}

	public void dropHashMap() {
		nodes2 = new Vector<Node>(nodes.values());
		nodes = null;
	}

	/**
	 * Print memory usage.
	 * 
	 * @param numberOfGarbageLoops
	 *            Number of times to call the garbage colector and print the
	 *            memory usage again.
	 */
	public static void printMemoryUsage(int numberOfGarbageLoops) {
		System.out.print("---> Used memory: "
				+ (Runtime.getRuntime().totalMemory() - Runtime.getRuntime()
						.freeMemory()) / 1024 + " KB / "
				+ Runtime.getRuntime().maxMemory() / 1024 + " KB");
		for (int i = 0; i < numberOfGarbageLoops; i++) {
			System.gc();
			System.out.print(" --> gc: "
					+ (Runtime.getRuntime().totalMemory() - Runtime
							.getRuntime().freeMemory()) / 1024 + " KB");
			try {
				if (i + 1 < numberOfGarbageLoops) {
					Thread.sleep(100);
				}
			} catch (InterruptedException ex) {
			}
		}
		System.out.println("");
	}

}
