/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007        Harald Mueller
 * Copyright (C) 2007, 2008  Kai Krueger
 */
package de.ueller.osmToGpsMid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.ueller.osmToGpsMid.model.Hash;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Storage;
import de.ueller.osmToGpsMid.model.Way;
/**
 * @author hmueller
 *
 */
public class CleanUpData {

	private final OsmParser parser;
	private final Configuration conf;

	
	private HashMap<Node,Node> replaceNodes = new HashMap<Node,Node>(); 

	public CleanUpData(OsmParser parser, Configuration conf) {
		this.parser = parser;
		this.conf = conf;
		removeEmptyWays();
		removeDupNodes();
		removeUnusedNodes();
		parser.dropHashMap();
		parser.resize();
		System.out.println("Remaining after cleanup:");
		System.out.println("  Nodes: " + parser.getNodes().size());
		System.out.println("  Ways: " + parser.getWays().size());
		System.out.println("  Relations: " + parser.getRelations().size());

	}

	private static class NodeHash implements Hash<Node, Node> {

		/* (non-Javadoc)
		 * @see de.ueller.osmToGpsMid.model.Hash#equals(java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean equals(Node k, Node t) {
			return k.lat == t.lat && k.lon == t.lon;
		}

		/* (non-Javadoc)
		 * @see de.ueller.osmToGpsMid.model.Hash#getHashCode(java.lang.Object)
		 */
		@Override
		public int getHashCode(Node k) {
			return Float.floatToIntBits(k.lat) + Float.floatToIntBits(k.lon);
		}

	}

	private static class NodeHash2 implements Hash<Node, Node> {

		/* (non-Javadoc)
		 * @see de.ueller.osmToGpsMid.model.Hash#equals(java.lang.Object, java.lang.Object)
		 */
		@Override
		public boolean equals(Node k, Node t) {
			return k.lat == t.lat && k.lon == t.lon && k.getType(Configuration.getConfiguration()) == t.getType(Configuration.getConfiguration());
		}

		/* (non-Javadoc)
		 * @see de.ueller.osmToGpsMid.model.Hash#getHashCode(java.lang.Object)
		 */
		@Override
		public int getHashCode(Node k) {
			return Float.floatToIntBits(k.lat) + Float.floatToIntBits(k.lon) + k.getType(Configuration.getConfiguration());
		}
	}


	/**
	 *
	 */
	private void removeDupNodes() {
		int progressCounter = 0;
		int duplicates = 0;
		int noNodes = parser.getNodes().size() / 20;
		Storage<Node> nodes = new Storage<Node>(new NodeHash());
		Storage<Node> nodesReEnabled = new Storage<Node>(new NodeHash2());

		System.out.println("PLEASE HELP and fix reported duplicates in OpenStreetMap");

		long startTime = System.currentTimeMillis();

		int iReSetToUsed = 0;
		for (Node n : parser.getNodes()) {

			progressCounter++;
			if (noNodes > 0 && progressCounter % noNodes == 0) {
				System.out.println("Processed " + progressCounter + " of "
						+ noNodes * 20 + " nodes, " + duplicates + " duplicates found");
			}

			n.used = true;
			//latlonKey = MyMath.latlon2XYZ(n);

			if (!nodes.add(n)) {
				duplicates++;
				n.used = false;
				Node rn = nodes.get(n);
				Node nReEnabled = nodesReEnabled.get(n);
				if (n.getType(conf) != rn.getType(conf)
						&& nReEnabled == null) {
					System.out.println("Differing duplicate nodes: " + n + " / " + rn);
					System.out.println("  Detail URL: " + n.toUrl());
					// Shouldn't substitute in this case;
					n.used = true;
					nodesReEnabled.put(n);
					iReSetToUsed++;
				} else {
					if (nReEnabled != null) {
						// A node of same position and type is in the list
						// of re enabled nodes. Substitude with this node later.
						//System.out.println("In re enabled: " + n);
						replaceNodes.put(n, nReEnabled);
					} else {
						replaceNodes.put(n, rn);
					}
				}
			}
		}

		Iterator<Node> it = parser.getNodes().iterator();
		int rm = 0;
		while (it.hasNext()) {
			Node n = it.next();
			if (n.used == false) {
				it.remove();
				rm++;
			}
		}
		substitute();
		long time = (System.currentTimeMillis() - startTime);
		System.out.println("Removed " + rm + " duplicate nodes, took "
				+ time + " ms, reset to used " + iReSetToUsed);
	}

	/**
	 * Replaces all duplicate nodes in the ways which use them.
	 * Uses the replaceNodes HashMap for this.
	 */
	private boolean substitute() {
		for (Way w:parser.getWays()) {
			w.replace(replaceNodes);
		}
		return true;
	}

	
	private void removeEmptyWays() {
		/**
		 * At this point, the area triangulation should have happened
		 * that might reference empty ways. Also the tag information for
		 * area POIs (whos ways has no type) should have been copied over too.
		 * So it should be safe to remove all ways (and in the second step free the nodes)
		 * of ways that have no type. This can save quite a lot of memory depending on style-file
		 */
		ArrayList<Way> rmWays = new ArrayList<Way>();
		for (Way w : parser.getWays()) {
			if (w.getType(conf) < 0) {
				rmWays.add(w);
			}
		}
		for (Way w : rmWays) {
			parser.removeWay(w);
		}
	}
	
	/**
	 *
	 */
	private void removeUnusedNodes() {
		long startTime = System.currentTimeMillis();

		for (Node n:parser.getNodes()) {
			if (n.getType(conf) < 0 ) {
				n.used = false;
			} else {
				n.used = true;
			}
		}

		for (Way w:parser.getWays()){
			for (Node n:w.getNodes()) {
				n.used = true;
			}
		}
		ArrayList<Node> rmNodes = new ArrayList<Node>();
		for (Node n:parser.getNodes()) {
			if (n.used == false) {
				rmNodes.add(n);
			}
		}
		parser.removeNodes(rmNodes);
		long time = (System.currentTimeMillis() - startTime);
		System.out.println("Removed " + rmNodes.size() + " unused nodes, took "
				+ time + " ms");
	}
}
