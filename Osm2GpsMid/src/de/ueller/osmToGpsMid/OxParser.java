/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.TurnRestriction;
import de.ueller.osmToGpsMid.model.Way;

public class OxParser extends DefaultHandler {
	/**
	 * The current processed primitive
	 */
	private Entity current = null;
	/**
	 * Maps id to already read nodes.
	 * Key: Long   Value: Node
	 */
	private HashMap<Long, Node> nodes = new HashMap<Long, Node>(80000, 0.60f);
	private static HashMap<Long, Way> ways = new HashMap<Long, Way>(); // must be static only to be able to call getWayHashMap() from Tile for debugging
	private HashMap<Long, Relation> relations = new HashMap<Long, Relation>();
	private HashMap<Long,TurnRestriction> turnRestrictions = new HashMap<Long, TurnRestriction>();
	private ArrayList<TurnRestriction> turnRestrictionsWithViaWays = new ArrayList<TurnRestriction>();
	private Hashtable<String, String> tagsCache = new Hashtable<String, String>();
	private int nodeTot, nodeIns;
	private int wayTot, wayIns;
	/** Nodes that delay routing if close to a routeNode, e.g. traffic signals */
	private Node[] delayingNodes;
	public int trafficSignalCount = 0;
	private int ele;
	private int relTot, relPart, relIns;
	private Vector<Bounds> bounds = null;
	private Configuration configuration;
	/**
	 * Keep track of ways that get split, as at the time of splitting
	 * not all tags have been added. So need to add them to all duplicates.
	 */
	private LinkedList<Way> duplicateWays;
	private long startTime;
	
	private Node previousNodeWithThisId;
	
	/**
	 * @param i InputStream from which planet file is read
	 */
	public OxParser(InputStream i) {
		System.out.println("OSM XML parser started...");
		configuration = new Configuration();
		init(i);
	}

	/**
	 * @param i InputStream from which planet file is read
	 * @param c Configuration which supplies the bounds
	 */
	public OxParser(InputStream i, Configuration c) {
		this.configuration = c;
		this.bounds = c.getBounds();
		System.out.println("OSM XML parser with bounds started...");
		init(i);
	}

	private void init(InputStream i) {
		try {
			startTime = System.currentTimeMillis();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			// Parse the input
			factory.setValidating(false);
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(i, this);
			//parse(new InputStreamReader(new BufferedInputStream(i,10240), "UTF-8"));
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			e.printStackTrace();
			/* The planet file is presumably corrupt. So there is no point in continuing,
			 * as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		} catch (SAXException e) {
			System.out.println("SAXException: " + e);
			e.printStackTrace();
			/* The planet file is presumably corrupt. So there is no point in continuing,
			 * as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		} catch (Exception e) {
			System.out.println("Other Exception: " + e);
			e.printStackTrace();
			/* The planet file is presumably corrupt. So there is no point in continuing,
			 * as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		}
	}

	public void startDocument() {
		System.out.println("Start of Document");
		System.out.println("Nodes read/used, Ways read/used, Relations read/partial/used");
	}

	public void endDocument() {
	    long time = (System.currentTimeMillis() - startTime) / 1000;

		System.out.println("Nodes " + nodeTot + "/" + nodeIns + 
				", Ways "+ wayTot + "/" + wayIns + 
				", Relations " + relTot + "/" + relPart + "/" + relIns);
		System.out.println("End of document, reading took " + time + " seconds");
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {		
//		System.out.println("start " + localName + " " + qName);
		if (qName.equals("node")) {
			float node_lat = Float.parseFloat(atts.getValue("lat"));
			float node_lon = Float.parseFloat(atts.getValue("lon"));
			
			long id = Long.parseLong(atts.getValue("id"));
			current = new Node(node_lat, node_lon, id);
			
		}
		if (qName.equals("way")) {
			long id = Long.parseLong(atts.getValue("id"));
			current = new Way(id);
			((Way)current).used = true;
		}
		if (qName.equals("nd")) {
			if (current instanceof Way) {
				Way way = ((Way)current);
				long ref = Long.parseLong(atts.getValue("ref"));
				Node node = nodes.get(new Long(ref));
				if (node != null) {
					way.add(node);
				} else {
					// Node for this Way is missing, problem in OS or simply out of Bounding Box
					// tree different cases are possible
					// missing at the start, in the middle or at the end
					// we simply add the current way and start a new one with shared attributes.
					// degenerate ways are not added, so don't care about this here.
					if (way.path != null) {
						/**
						 * Attributes might not be fully known yet, so keep
						 * track of which ways are duplicates and clone
						 * the tags once the XML for this way is fully parsed
						 */
						if (duplicateWays == null) {
							duplicateWays = new LinkedList<Way>();
						}
						duplicateWays.add(way);
						current = new Way(way);
					}
				}
			}
		}
		if (qName.equals("tag")) {
			if (current != null) {				
				String key = atts.getValue("k");
				String val = atts.getValue("v");
				/**
				 * atts.getValue creates a new String every time
				 * If we store key and val directly in current.setAttribute
				 * we end up having thousands of duplicate Strings for e.g.
				 * "name" and "highway". By filtering it through a Hashtable
				 * we can reuse the String objects thereby saving a significant
				 * amount of memory.
				 */
				if (key != null && val != null) {
					/**
					 * Filter out common tags that are definitely not used such as created_by
					 * If this is the only tag on a Node, we end up saving creating a Hashtable
					 * object to store the tags, saving some memory.
					 */
					if (!key.equalsIgnoreCase("created_by") && !key.equalsIgnoreCase("converted_by") &&
							!key.equalsIgnoreCase("source") && !key.startsWith("tiger") && 
							!key.equalsIgnoreCase("attribution") && !key.equalsIgnoreCase("note")) {
						if (!tagsCache.containsKey(key)) {
							tagsCache.put(key, key);
						}
						if (!tagsCache.containsKey(val)) {
							tagsCache.put(val, val);
						}
						current.setAttribute(tagsCache.get(key), tagsCache.get(val));
					}
				}				
			} else {
				System.out.println("Tag at unexpected position " + current);
			}
		}
		if (qName.equals("relation")) {
			long id = Long.parseLong(atts.getValue("id"));
			current=new Relation(id);
		}
		if (qName.equals("member")) {
			if (current instanceof Relation) {
				Relation r = (Relation)current;
				Member m = new Member(atts.getValue("type"), atts.getValue("ref"), atts.getValue("role"));
				switch(m.getType()) {
				case Member.TYPE_NODE: {
					if (!nodes.containsKey(new Long(m.getRef()))) {
						r.setPartial();
						return;
					}
					break;
				}
				case Member.TYPE_WAY: {
					if (!ways.containsKey(new Long(m.getRef()))) {
						r.setPartial();
						return;
					}
					break;
				}
				case Member.TYPE_RELATION: {
					if (m.getRef() > r.id) {
						//We haven't parsed this relation yet, so
						//we have to assume it is valid for the moment
					} else {
						if (!relations.containsKey(new Long(m.getRef()))) {
							r.setPartial();
							return;
						}
					}
					break;
				}
				}
				r.add(m);
			}
		}

	} // startElement

	public void endElement(String namespaceURI, String localName, String qName) {		
//		System.out.println("end  " + localName + " " + qName);
		ele++;
		if (ele > 1000000) {
			ele = 0;
			System.out.println("Nodes " + nodeTot + "/" + nodeIns + 
					", Ways "+ wayTot + "/" + wayIns + 
					", Relations " + relTot + "/" + relPart + "/" + relIns);
		}
		if (qName.equals("node")) {
			Node n = (Node) current;
			boolean inBound = false;
			nodeTot++;
			if (bounds != null && bounds.size() != 0) {
				for (Bounds b : bounds) {
					if (b.isIn(n.lat, n.lon)) {
						inBound = true;
						break;
					}
				}
			} else {
				inBound = true;
			}

			if (inBound) {
				previousNodeWithThisId = nodes.put(new Long(current.id), (Node) current);
				nodeIns++;
				if (current.getAttribute("highway") != null && current.getAttribute("highway").equalsIgnoreCase("traffic_signals")) {
					// decrement trafficSignalCount if a previous node with this id got replaced but was a traffic signal node
					if (previousNodeWithThisId != null && previousNodeWithThisId.isTrafficSignals()) {
						trafficSignalCount--;
						System.out.println("DUPLICATE TRAFFIC SIGNAL NODE ID: " + previousNodeWithThisId.id + " more than once in osm file");
					}
					n.markAsTrafficSignals();
					trafficSignalCount++;
				}
			}
			current = null;
		} 
		if (qName.equals("way")) {
			wayTot++;
			Way w = (Way) current;
			// TODO: this seems to be not useful, because the list of tags is shared (only one list)
			//       so a add of an attribute to one if the ways already adds it to the
			//		 other as well.
			if (duplicateWays != null) {
				for (Way ww : duplicateWays) {
					ww.cloneTags(w);
					addWay(ww);
				}
				duplicateWays = null;
			}
			addWay(w);

			current = null;
		} 
		if (qName.equals("relation")) {
			relTot++;
			/** Store way and node refs temporarily in the same variable
			 *  Way refs must be resolved later to nodes when we actually know all the ways
			 */
			long viaNodeOrWayRef = 0;
			Relation r=(Relation) current;
			if (r.isValid()) {				
				if (!r.isPartial()) {
					relIns++;
					viaNodeOrWayRef = r.getViaNodeOrWayRef(); 
				} else {
					relPart++;
				}
				if (viaNodeOrWayRef != 0) {
					TurnRestriction turnRestriction = new TurnRestriction(r);
					if (r.isViaWay()) {
						//  Store the ref to the via way
						turnRestriction.viaWayRef = viaNodeOrWayRef;
						// add a flag to the turn restriction if it's a way
						turnRestriction.flags |= TurnRestriction.VIA_TYPE_IS_WAY;
						// add restrictions with viaWays into an ArrayList to be resolved later
						turnRestrictionsWithViaWays.add(turnRestriction);
					} else { // remember normal turn restrictions now because we already know the via node 
						addTurnRestriction(viaNodeOrWayRef, turnRestriction);
					}
				}
				else {
					relations.put(new Long(r.id),r);
				}
			}			
			
			current = null;
		}
	} // endElement

	/**
	 * @param viaNodeOrWayRef
	 * @param turnRestriction
	 */
	public void addTurnRestriction(long viaNodeOrWayRef,
			TurnRestriction turnRestriction) {
		if (! turnRestrictions.containsKey(new Long(viaNodeOrWayRef))) {
			turnRestrictions.put(new Long(viaNodeOrWayRef), turnRestriction);
			// System.out.println("Put turn restrictions at " + viaNodeOrWayRef); 
		} else {
			TurnRestriction baseTurnRestriction = (TurnRestriction) turnRestrictions.get(new Long(viaNodeOrWayRef));
			while (baseTurnRestriction.nextTurnRestrictionAtThisNode != null) {
				baseTurnRestriction = baseTurnRestriction.nextTurnRestrictionAtThisNode;
			}
			baseTurnRestriction.nextTurnRestrictionAtThisNode = turnRestriction;
			// System.out.println("Multiple turn restrictions at " + viaNodeOrWayRef); 
		}
	}
	
	
	/**
	 * @param w
	 */
	public void addWay(Way w) {
		byte t = w.getType(configuration);
		/**
		 * We seem to have a bit of a mess with respect to type -1 and 0.
		 * Both are used to indicate invalid type it seems.
		 */
		if (w.isValid() && t > 0) {
			if (ways.get(new Long(w.id)) != null) {
				/**
				 * This way is already in data storage.
				 * This results from splitting a single
				 * osm way into severals GpsMid ways.
				 * We can simply invent an id in this
				 * case, as we currently don't use them
				 * for anything other than checking if
				 * an id is valid for use in relations
				 */				
				ways.put(new Long(-1 * wayIns), w);
			} else {
				ways.put(new Long(w.id), w);
			}
			wayIns++;
		}
	}
	
	public void removeWay(Way w){
		ways.remove(new Long(w.id));
	}

	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println("Error: " + e);
		throw e;
	}

	public Collection<Node> getNodes() {
		return nodes.values();
	}


	public Collection<Way> getWays() {
		return ways.values();
	}
	
	public Collection<Relation> getRelations() {
		return relations.values();
	}

	public HashMap<Long,TurnRestriction> getTurnRestrictionHashMap() {
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

	public HashMap<Long,Way> getWayHashMap() {
		return ways;
	}

	
	public void removeNode(long id) {
		nodes.remove(new Long(id));
	}
	
	/**
	 * 
	 */
	public void resize() {
		System.gc();
		System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
		System.out.println("Resizing nodes HashMap");
		nodes=new HashMap<Long, Node>(nodes);
		relations=new HashMap<Long, Relation>(relations);
		System.gc();
		System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
	}
}
