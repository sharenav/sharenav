/**
  * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 2008 Harald Mueller
 * 
 */
package de.ueller.osmToGpsMid;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.RouteNode;
import de.ueller.osmToGpsMid.model.TravelMode;
import de.ueller.osmToGpsMid.model.TravelModes;
import de.ueller.osmToGpsMid.model.TurnRestriction;
import de.ueller.osmToGpsMid.model.Way;
import de.ueller.osmToGpsMid.model.WayDescription;
import de.ueller.osmToGpsMid.tools.FileTools;

/**
 * @author hmueller
 *
 */
public class RouteData {
	private OsmParser parser;
	private String path;
	public Map<Long, RouteNode> nodes = new HashMap<Long, RouteNode>();

	public RouteData(OsmParser parser, String path) {
		super();
		this.parser = parser;
		this.path = path;
	}
	
	public void create(Configuration config) {
		if (config.sourceIsApk) {
			this.path = this.path + "/assets";
		}
		// reset connectedLineCount for each Node to 0
		for (Node n:parser.getNodes()) {
			n.resetConnectedLineCount();
		}

		boolean neverTrafficSignalsRouteNode = false;
		// count all connections for all nodes
		for (Way w:parser.getWays()) {
			if (! w.isAccessForAnyRouting()) {
				continue;
			}

			// mark nodes in tunnels / on bridges / on motorways to not get later marked as traffic signal delay route node by nearby traffic signals
			WayDescription wayDesc = config.getWayDesc(w.type);
			neverTrafficSignalsRouteNode = (w.isBridge() || w.isTunnel() || (wayDesc.isMotorway() && !wayDesc.isHighwayLink()) );
			
				Node lastNode = null;
				for (Node n:w.getNodes()) {
					n.incConnectedLineCount();
					if (lastNode != null) {
						n.incConnectedLineCount();
					}
					lastNode = n;
					if (neverTrafficSignalsRouteNode) {
						n.markAsNeverTrafficSignalsRouteNode();
						//System.out.println("Mark to never become a traffic signal delay route node: " + n.toString());
					}
				}
				if (lastNode != null) {
					lastNode.decConnectedLineCount();
				}
		}
		 
		for (Way w:parser.getWays()) {
			if (!w.isAccessForAnyRouting()) {
				continue;
			}
			addConnections(w.getNodes(), w);

		}
		System.out.println("Created " + nodes.size() + " route nodes.");
		createIds();
		calculateTurnRestrictions();
	}


	/**
	 * calculate turn restrictions
	 */		
	private void calculateTurnRestrictions() {
		resolveViaWays();
		
		System.out.println("info: Calculating turn restrictions");
		int numTurnRestrictions = 0;
		for (RouteNode n: nodes.values()) {
			TurnRestriction turn = (TurnRestriction) parser.getTurnRestrictionHashMap().get(new Long(n.node.id));
			while (turn != null) {
				Way restrictionFromWay = parser.getWayHashMap().get(new Long(turn.fromWayRef));
				// skip if restrictionFromWay is not in available wayData				
				if (restrictionFromWay == null) {
					System.out.println("  no fromWay");
					turn = turn.nextTurnRestrictionAtThisNode;
					continue;
				}
				// skip if restrictionToWay is not in available wayData				
				Way restrictionToWay = parser.getWayHashMap().get(new Long(turn.toWayRef));
				if (restrictionToWay == null) {
					System.out.println("  no toWay");
					turn = turn.nextTurnRestrictionAtThisNode;
					continue;
				}
				
				turn.viaRouteNode = n;
				turn.viaLat = n.node.lat;
				turn.viaLon = n.node.lon;
				turn.affectedTravelModes = TravelModes.applyTurnRestrictionsTravelModes;

				// search the from way for the RouteNode connected to the viaWay
				RouteNode nViaFrom = n;
				if (turn.isViaTypeWay()) {
					nViaFrom = turn.additionalViaRouteNodes[0];
				}

				//System.out.println(turn.toString(parser.getWayHashMap()));
				int numFromConnections = 0;
				long lastId = -1;
				for (Connection c:nViaFrom.getConnectedFrom()) { // TODO: Strange: there are sometimes multiple connections connecting to the same node, filter those out by checking lastId 
					/* [ gpsmid-Bugs-3159017 ] Can't parse turn restriction 
					 * rather than only checking if the restrictionFromWay contains the from node
					 * compare the travel length on the way between from node and via node
					 * with the connection length to make more sure this is the right connection
					 * with the via way
					 * FIXME: we need to check for the right connection with this workaround
					 * for not having to store the way in the connection, otherwise we could check for c.way == restrictionWay
					 */
					if (getDistOnWay(restrictionFromWay, nViaFrom.node, c.from.node) == c.length && c.from.id != lastId) { 
						turn.fromRouteNode = c.from;
						numFromConnections++;
						lastId = c.from.id;
					}
				}
				if (numFromConnections != 1) {
					if (restrictionFromWay.isAccessForRoutingInAnyTurnRestrictionTravelMode()) {
						System.out.println("warning: ignoring map data: Can't parse turn restriction: " + numFromConnections + " from_connections matched for: " + turn.toString(parser.getWayHashMap()));
						if (numFromConnections == 0) {
							System.out.println("warning: ignoring map data:   Reason may be: from/to swapped on oneways or a gap between viaNode and fromWay");						
						} else {
							System.out.println("warning: ignoring map data:   Reason may be: fromWay not split at via member");												
							turn.fromRouteNode = null; // make the turn restriction incomplete so it won't get passed to GpsMid 
						}
						for (Connection c:nViaFrom.getConnectedFrom()) {
							if (restrictionFromWay.containsNode(c.from.node)) {
								System.out.println("  FromNode: " + c.from.node.id);
							}
						}
						System.out.println("warning: ignoring map data:   URL for via node: " + n.node.toUrl());
					}
				}

				// search the RouteNode following the viaRouteNode on the toWay
				int numToConnections = 0;
				lastId = -1;
				for (Connection c:n.getConnected()) {
					/* FIXME: we do not store the way in the connection, otherwise we could check for c.way == restrictionWay */
					if (getDistOnWay(restrictionToWay, n.node, c.to.node) == c.length && c.to.id != lastId) { 
						// TODO: Strange: there are sometimes multiple connections connecting to the same node, filter those out by checking lastId
						turn.toRouteNode = c.to;
						numToConnections++;
						lastId = c.to.id;
					}
				}
				if (numToConnections != 1) {
					if (restrictionToWay.isAccessForRoutingInAnyTurnRestrictionTravelMode()) {
						System.out.println("warning: ignoring map data: Can't parse turn restriction: " + numToConnections + " to_connections matched for: "  + turn.toString(parser.getWayHashMap()));
						if (numToConnections == 0) {
							System.out.println("warning: ignoring map data:   Reason may be: from/to swapped on oneways or a gap between viaNode and toWay");						
						} else {
							System.out.println("warning: ignoring map data:   Reason may be: toWay not split at via member");												
							turn.toRouteNode = null; // make the turn restriction incomplete so it won't get passed to GpsMid 
						}
						for (Connection c:n.getConnected()) {
							if (restrictionToWay.containsNode(c.to.node)) {
								System.out.println("warning: ignoring map data:  ToNode: " + c.to.node.id);
							}
						}
						System.out.println("warning: ignoring map data:  URL for via node: " + n.node.toUrl());
					}
				}
				if (numFromConnections == 1 && numToConnections == 1) {
					numTurnRestrictions++;
				}
				
				turn = turn.nextTurnRestrictionAtThisNode;
			}
		}
		System.out.println("info: " + numTurnRestrictions + " turn restrictions valid");
	}


	/**
	 * Resolve viaWays to route nodes
	 */
	private void resolveViaWays() {
		int numViaWaysResolved = 0;
		System.out.println("info: Resolving " + parser.getTurnRestrictionsWithViaWays().size() + " viaWays for turn restrictions");
		for (TurnRestriction turn: parser.getTurnRestrictionsWithViaWays()) {
			Way restrictionFromWay = parser.getWayHashMap().get(new Long(turn.fromWayRef));
			// skip if restrictionFromWay is not in available wayData				
			if (restrictionFromWay == null) {
				continue;
			}
			// skip if restrictionToWay is not in available wayData				
			Way restrictionToWay = parser.getWayHashMap().get(new Long(turn.toWayRef));
			if (restrictionToWay == null) {
				continue;
			}
			// skip if restrictionViaWay is not in available wayData				
			Way restrictionViaWay = parser.getWayHashMap().get(new Long(turn.viaWayRef));
			if (restrictionViaWay == null) {
				continue;
			}
			
			ArrayList<RouteNode> viaWayRouteNodes = restrictionViaWay.getAllRouteNodesOnTheWay();
			
			// if it's a circle way remove the first viaRouteNode
			if (viaWayRouteNodes.size()>0 && viaWayRouteNodes.get(0).id == viaWayRouteNodes.get(viaWayRouteNodes.size() -1 ).id) {
				viaWayRouteNodes.remove(0);
			}
			ArrayList<RouteNode> additionalViaRouteNodesCache = new ArrayList<RouteNode>();
			
			int startEntry = 0;
			// find the index of the element crossing the fromWay (start entry)
			for (RouteNode n:viaWayRouteNodes) {
				if (restrictionFromWay.containsNode(n.node)) { // this is where viaWay and fromWay are connected
					additionalViaRouteNodesCache.add(n); // and becomes the first entry in the additionalViaRouteNode array
					System.out.println("info:  Resolved viaWay x fromWay to node " + n.node.id);
					break;
				}
				startEntry++;
			}

			// find the index of the element crossing the toWay (the end entry)
			int endEntry = 0;
			RouteNode rn = null;
			// the direction to fill in the remaining viaRouteNodes into the array so the result is ordered with route nodes from the fromWay to the toWay exclusively
			int direction = 1;			
			for (int i = startEntry; i < viaWayRouteNodes.size() * 2; i++) {
				// use index with modulo because of circle ways in roundabouts
				rn = viaWayRouteNodes.get(i % viaWayRouteNodes.size());
				if (restrictionToWay.containsNode(rn.node)) {
					turn.viaRouteNode = rn;
					endEntry = i % viaWayRouteNodes.size();
					if (i == endEntry || restrictionViaWay.isOneWay()) {
						direction = 1;
					} else {
						direction = -1;
					}
					System.out.println("info:  Resolved viaWay x toWay to node " + rn.node.id);					
					break;
				} else {
					
				}
			}
			
			// fill in routeNodes between start and end entry
			if (turn.viaRouteNode != null && additionalViaRouteNodesCache.size() != 0) {
				//  fill in the remaining viaRouteNodes into the array so the result is ordered with route nodes from the fromWay to the toWay exclusively
				for (int i = startEntry + direction; i != startEntry && i % viaWayRouteNodes.size() != endEntry; i += direction) {
					// use index with modulo because of circle ways in roundabouts
					i %= viaWayRouteNodes.size();
//					System.out.println(i + " " + startEntry + " " + endEntry + " dir " + direction );
					rn = viaWayRouteNodes.get(i);
					additionalViaRouteNodesCache.add(rn);
				}
				
				// transfer the route nodes from the ArrayList to the additionalViaRouteNodes array
				turn.additionalViaRouteNodes = new RouteNode[additionalViaRouteNodesCache.size()];
				for (int i=0; i < turn.additionalViaRouteNodes.length; i++) {
					turn.additionalViaRouteNodes[i] = additionalViaRouteNodesCache.get(i);
				}
				
				System.out.println("info:  viaRouteNodes on viaWay " + restrictionViaWay.toUrl() + ":");
				for (RouteNode n:turn.additionalViaRouteNodes) {
					if (n != null && n.node != null) {
						System.out.println("info:    " + n.node.toUrl());
					} else {
						if (n == null) {
							System.out.println("info:    n is null");
						} else {
							System.out.println("info:    n.node is null");
						}
						continue;
					}
				}
				System.out.println("info:    " + turn.viaRouteNode.node.toUrl());									

				// add the resolved viaWay turn restriction to its viaRouteNode
				parser.addTurnRestriction(new Long(turn.viaRouteNode.node.id), turn);
				
				numViaWaysResolved++;
			} else {
				System.out.println("  WARNING: Could not resolve viaRouteNodes");
				System.out.println("    for viaWay " + restrictionViaWay.toUrl());
				if ( turn.additionalViaRouteNodes == null) {
					System.out.println("    viaWay " + restrictionFromWay.toUrl() + " does not end at start of toWay");						
				} else if (turn.additionalViaRouteNodes[0] == null) {
					System.out.println("    fromWay " + restrictionFromWay.toUrl() + " is not connected");	
				}
				if (turn.viaRouteNode == null) {
					System.out.println("    toWay " + restrictionToWay.toUrl() + " is not connected");	
				}
			}
		}
		System.out.println("  " + numViaWaysResolved + " viaWays resolved");

		parser.getTurnRestrictionsWithViaWays().clear();		
	}
	
	/**
	 * @param w
	 * @param n1 - a node on the way
	 * @param n2 - another node on the way
	 * @return distance for travelling from n1 to n2 on w or -1 if no match
	 */
	private int getDistOnWay(Way w, Node n1, Node n2) {
		boolean startNodeFound = false;
		Node lastNode = null;
		int dist = 0;
		for (Node n:w.getNodes()) {			
			if (startNodeFound) {
				dist += MyMath.dist(lastNode, n);
				if (n.id == n1.id || n.id == n2.id) {
					return dist;
				}				
			}
			if (n.id == n1.id || n.id == n2.id) {
				// start measuring distance
				startNodeFound = true;				
			}
			lastNode = n;			
		}
		return -1;
	}
	
	/**
	 * @param nl
	 */
	// TODO: explain
	private void addConnections(List<Node> nl, Way w) {
		RouteNode from = null;
		int lastIndex = nl.size();
		Node lastNode = null;
		int thisIndex = 0;
		int dist = 0;
		int count = 0;
		byte bearing = 0;
		for (Node n:nl) {
			thisIndex++;
			if (from == null) {
				lastNode = n;
				from = getRouteNode(n);
				count++;
				dist = 0;
			} else {
				dist += MyMath.dist(lastNode, n);
				count++;
				if (count == 2) {
					bearing = MyMath.bearing_start(lastNode, n);
				}
				if (thisIndex == lastIndex || (n.getConnectedLineCount() != 2)) {
					RouteNode next = getRouteNode(n);
					byte endBearing = MyMath.bearing_start(lastNode, n);
					addConnection(from, next, dist, w, bearing, endBearing);
					from = next;
					dist = 0;
					count = 1;
				}
				lastNode = n;
			}
		}
		nl = new ArrayList<Node>();
	}

	/**
	 * @param l
	 */
	private RouteNode getRouteNode(Node n) {
		RouteNode routeNode;
		if (! nodes.containsKey(n.id)) {
			routeNode = new RouteNode(n);
			n.routeNode = routeNode;
		} else {
			routeNode = nodes.get(n.id);
		}
		return routeNode;
	}
	
	/**
	 * @param from
	 * @param f 
	 * @param dist 
	 * @param routeNode
	 */
	private void addConnection(RouteNode from, RouteNode to, int dist, Way w, byte bs, byte be) {
		/** travel modes with no barrier detected */
		int noBarrierTravelModes = 0xFFFFFFFF;
		
		byte againstDirectionTravelModes = 0;		
		
		// create an array of routing times with an entry for each travel mode
		int times[] = new int[TravelModes.travelModeCount];
		for (int i = 0; i < TravelModes.travelModeCount; i++) {
			if (w.isAccessForRouting(i)) {
				// check for barriers in non-area ways
				if (!w.isArea()) {
					int a = 0;
					boolean fromNodeFound = false;
					boolean toNodeFound = false;
					for (Iterator<Node> si = w.path.iterator(); si.hasNext();) {
						Node t = si.next();
						if (from.node.id == t.id /* && a != 0 && si.hasNext() */) {
							fromNodeFound = true;
						}
						if (to.node.id == t.id /* && a != 0 && si.hasNext() */) {
							toNodeFound = true;
						}
						if (
							(
								(fromNodeFound && !toNodeFound)
								||
								(toNodeFound && !fromNodeFound)
							)
							&&
							t.isBarrier()
						) {
							if (t.isAccessPermittedOrForbiddenFor(i) <= 0) {
//								if (noBarrierTravelModes == 0xFFFFFFFF) {
//									System.out.println("Barrier found on " + w.toString() + " at \n" + t.toUrl());
//								}
//								System.out.println("    affects route mode " + i);
								noBarrierTravelModes &= ~(1 << i);
								break;
							}
						}
						a++;
					}
				}

				
				TravelMode tm = TravelModes.getTravelMode(i);
				if (w.isExplicitArea()) {
					tm.numAreaCrossConnections++;
				}
				tm.numOneWayConnections++;
				float speed = w.getRoutingSpeed(i);
				float time = dist * 10.0f / speed;
				times[i] = (int)time;

				boolean bicycleOppositeDirection = (tm.travelModeFlags & TravelMode.BICYLE_OPPOSITE_EXCEPTIONS) > 0 && w.isOppositeDirectionForBicycleAllowed();				
				// you can go against the direction of the way if it's not a oneway or an against direction rule applies
				if (
					!w.isRoundabout() // FIXME: workaround to never route against direction in roundabouts, not even walk because we have no routing instruction for this
					&&
					(
						! w.isOneWay()
							||
						(tm.travelModeFlags & TravelMode.AGAINST_ALL_ONEWAYS) > 0
							||
						bicycleOppositeDirection
					)
				) {
					againstDirectionTravelModes |= (1<<i);
					if (bicycleOppositeDirection) {
						tm.numBicycleOppositeConnections++;
					}
				}
			} else {
				times[i] = 0;				
			}
		}
		
		boolean allBarriered = true;
		for (int i = 0; i < TravelModes.travelModeCount; i++) {
			if ( (noBarrierTravelModes & (1<<i)) > 0) {
				allBarriered = false;
			}
		}
		if (allBarriered) {
//			System.out.println("Connection barriered for all route modes");
			// avoid to create a connection that cannot be travelled in any route mode
			return;
		}
		
		nodes.put(from.node.id, from);
		nodes.put(to.node.id, to);
		Connection c = new Connection(to, dist, times, bs, be, w);
		c.connTravelModes &= noBarrierTravelModes; // disable connection for travelmodes that are barriered
		from.addConnected(c);
		to.addConnectedFrom(c);
		// roundabouts don't need to be explicitly tagged as oneways in OSM according to http://wiki.openstreetmap.org/wiki/Tag:junction%3Droundabout
		if (againstDirectionTravelModes != 0 ) {
			// add connection in the other direction as well, if this is no oneWay
			// TODO: explain Doesn't this add duplicates when addconnection() is called later on with from and to exchanged or does this not happen?
			Connection cr = new Connection(from, dist, times, MyMath.inverseBearing(be),
					MyMath.inverseBearing(bs), w);
			cr.from = to;
			to.addConnected(cr);
			from.addConnectedFrom(cr);

			// flag connections useable for travel modes you can go against the ways direction
			cr.connTravelModes = againstDirectionTravelModes;
			cr.connTravelModes &= noBarrierTravelModes; // disable connection for travelmodes that are barriered
			cr.connTravelModes |= w.wayTravelModes & (Connection.CONNTYPE_MAINSTREET_NET | Connection.CONNTYPE_MOTORWAY | Connection.CONNTYPE_TRUNK_OR_PRIMARY);
			
			for (int i=0; i < TravelModes.travelModeCount; i++) {
				if ( (againstDirectionTravelModes & (1<<i)) != 0 ) {
					TravelMode tm = TravelModes.getTravelMode(i);
					tm.numDualConnections++;
					tm.numOneWayConnections--;
				}
			}
		}
		// need only for debugging not for live
		c.from = from;
		
	}
	
    @Deprecated
	public boolean isRelevant(Node n) {
		int count = 0;
//		for (Line l:parser.lines.values()) {
//			if (n.id == l.from.id) {
//				count++;
//			}
//			if (n.id == l.to.id) {
//				count++;
//			}
//		}
		if (count == 2) {
			return false;
		} else {
			return true;
		}
	}
	
	private void createIds() {
		int id = 1;
		for (RouteNode n: nodes.values()) {
			n.id = id++;
		}
	}
	
	public void optimise() {
//				System.out.println("Optimizing route data");
//		System.out.println("RouteNodes for optimise " + nodes.size());
//		ArrayList<RouteNode> removeNodes = new ArrayList<RouteNode>();
//		for (RouteNode n:nodes.values()) {
//			// find nodes that are only a point between to nodes without
//			// any junction. This test does not cover one ways
//			// for normal ways the second connection will removed 
//			if (n.connected.size() == 2 && n.connectedFrom.size() == 2) {
//				Connection c1 = n.connected.get(0);
//				RouteNode n1 = c1.to;
//				Connection c2 = null;
//				Connection c3 = n.connected.get(1);
//				RouteNode n2 = c3.to;
//				Connection c4 = null;
//				for (Connection ct:n.connectedFrom) {
//					if (ct.from == n2 ) {
//						c2 = ct;
//					} 
//					if (ct.from == n1) {
//						c4 = ct;
//					}
//				}
//				if (c2 != null && c4 != null) {
//					if (c2.to != n) {
//						System.out.println("c2.to != n");
//					}
//					if (c4.to != n) {
//						System.out.println("c4.to != n");
//					}
//					if (c1.to != c4.from) {
//						System.out.println("c1.to != c4.from");
//					}
//					if (c2.from != c3.to) {
//						System.out.println("c2.from != c3.to");
//					}
//					c2.endBearing = c1.endBearing;
//					c2.time += c1.time;
//					c2.length += c1.length;
//					c2.to = c1.to;
//					c3.from = c1.to;
//					c3.startBearing = MyMath.inversBearing(c1.endBearing);
//					c3.time += c1.time;
//					c3.length += c1.length;
//					n1.connectedFrom.remove(c1);
//					n1.connected.remove(c4);
//					n1.connectedFrom.add(c2);
//					n1.connected.add(c3);
//					connections.remove(c1);
//					connections.remove(c4);
//					removeNodes.add(n);
//				}
//
//			}
//			// for one ways
//			if (n.connected.size() == 1 && n.connectedFrom.size() == 1) {
//				Connection c1 = n.connected.get(0);
//				RouteNode n1 = c1.to;
//				Connection c2 = n.connectedFrom.get(0);
//				RouteNode n2 = c2.from;
//				if (n2 != n1) {
//					c2.endBearing = c1.endBearing;
//					c2.time += c1.time;
//					c2.length += c1.length;
//					c2.to = c1.to;
//					n1.connectedFrom.remove(c1);
//					n1.connectedFrom.add(c2);
//					n.connected.remove(c1);
//					n.connectedFrom.remove(c2);
//					connections.remove(c1);
//					removeNodes.add(n);
//				
//				}
//			}
//		}
//		System.out.println("Removed " + removeNodes.size() + " RouteNodes due to optimization");
//		for (RouteNode n:removeNodes) {
//			n.node.routeNode = null;
//			nodes.remove(n.node.id);
//		}
	}
	
	
	/**
	 * normaly not used, only for test
	 * @param args
	 */
	public static void main(String[] args) {

				try {					
					Configuration conf = new Configuration(args);
					FileInputStream fr = new FileInputStream("/Massenspeicher/myStreetMap0.5.osm");
//					FileInputStream fr = new FileInputStream("/Massenspeicher/planet-070725.osm");
					OxParser parser = new OxParser(fr, conf);
					System.out.println("Read nodes " + parser.getNodes().size());
					System.out.println("Read ways  " + parser.getNodes().size());
					new CleanUpData(parser, conf);
					RouteData rd = new RouteData(parser, "");

					rd.create(conf);
					
					int rid = 10000;
//					for (RouteNode r:rd.nodes.values()) {
//						r.node.renumberdId=rid++;
//					}
					rid = 1;
					rd.optimise();
					for (RouteNode r:rd.nodes.values()) {
						r.node.renumberdId = rid++;
					}

//					rd.write("/Temp");
					System.out.println("RelNodes contain " + rd.nodes.size());
					//System.out.println("Connections contain " + rd.connections.size());
					RouteNode start = rd.nodes.get(new Long(1955808));
					System.out.println("Start " + start);
//					RouteNode dest = rd.nodes.get(new Long(25844378));
//					RouteNode dest = rd.nodes.get(new Long(33141402));
//					RouteNode dest = rd.nodes.get(new Long(28380647));
//					System.out.println("Destination " + dest);
//					AStar2 astar = new AStar2();
//					Vector<Connection> solve = astar.solve(start, dest);
//					System.out.println("\n\nSolution:");
					PrintWriter fo = new PrintWriter("/Massenspeicher/routetestErg.osm");
//					exportResultOSM(fo, rd, solve);
					fo = new PrintWriter("/Massenspeicher/routetestConnections.osm");
					exportResultOSM(fo, rd, null);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
	}

	/**
	 * @param fo
	 * @param rd
	 * @param solve
	 */
	private static void exportResultOSM(PrintWriter fo, RouteData rd,
			Vector<Connection> solve) {
		fo.write("<?xml version='1.0' encoding='UTF-8'?>\n");
		fo.write("<osm version='0.5' generator='JOSM'>\n");

		for (RouteNode r:rd.nodes.values()) {
			fo.write("<node id='" + r.node.renumberdId);
			fo.write("' timestamp='2007-02-15 10:32:17' visible='true' lat='" +  r.node.lat);
			fo.write("' lon='" + r.node.lon + "'>\n");
			fo.write("  <tag k='connectCount' v='" + r.node.getConnectedLineCount() + "'/>\n");
			fo.write("  <tag k='connectTo' v='");
			for (Connection c:r.getConnected()) {
				fo.write("," + c.to.node.renumberdId);
			}
			fo.write("'/>\n");
			fo.write("  <tag k='connectFrom' v='");
			for (Connection c:r.getConnectedFrom()) {
				fo.write("," + c.from.node.renumberdId);
			}
			fo.write("'/>\n");
			fo.write("</node>\n");

		}
		int id = 1;
		for (RouteNode r:rd.nodes.values()) {
			for (Connection c:r.getConnected()) {
		fo.write("<way id='" + id++ + "' timestamp='2007-02-14 23:41:43' visible='true' >\n");
		fo.write("  <nd ref='" + c.from.node.renumberdId + "'/>\n");
		fo.write("  <nd ref='" + c.to.node.renumberdId + "'/>\n");
		fo.write("  <tag k='name' v='laenge=" + c.length + "' />\n");
		System.out.println("RouteData.exportResultOSM(): only first route mode");
		fo.write("  <tag k='time' v='" + c.times[0] + "' />\n");
		fo.write("  <tag k='bs' v='" + c.startBearing * 2 + "' />\n");
		fo.write("  <tag k='be' v='" + c.endBearing * 2 + "' />\n");
		fo.write("</way>\n");
		}
	}
		
		
//		RouteNode last = null;
//		Connection lastCon = null;
//		int lb = 0;
//		if (solve != null) {
//		for (Connection c:solve) {
//			if (last == null) {
//				last = c.to;
//				lastCon = c;
//				lb = c.endBearing;
//			} else {
//				System.out.println(c.printTurn(lastCon));
//				fo.write("<segment id='" + id++ + "' timestamp='2007-02-14 23:41:43' visible='true' from='" +
//						last.node.renumberdId
//						+ "' to='"
//						+ c.to.node.renumberdId
//						+ "'>\n");
//				fo.write("  <tag k='length' v='" + c.length + "' />\n");
//				fo.write("  <tag k='time' v='" + c.time + "' />\n");
//				fo.write("  <tag k='bs' v='" + c.startBearing * 2 + "' />\n");
//				fo.write("  <tag k='be' v='" + c.endBearing * 2 + "' />\n");
//				fo.write("</segment>\n");
//				last = c.to;
//				lastCon = c;
//			}
//		} 
//		}
		fo.write("</osm>");
		fo.close();
	}

	/**
	 * @param zl
	 * @param fid
	 * @param nodes2
	 * @throws FileNotFoundException 
	 */
	public void write(int zl, int fid, Collection<Node> nodes2) throws FileNotFoundException {
		FileOutputStream fo = FileTools.createFileOutputStream(path + "/t" + zl +"/"+ fid + ".d");
		DataOutputStream tds = new DataOutputStream(fo);
		for (Node n: nodes2) {
			if (nodes.containsKey(n.id)) {
				RouteNode rn = nodes.get(n.id);
			}
		}
	}

//	/**
//	 * deprecated but still used by routeTiles 
//	 * @param canonicalPath
//	 * @throws IOException 
//	 */
//	@Deprecated
//	public void write(String canonicalPath) throws IOException {
//		DataOutputStream nodeStream = new DataOutputStream(new FileOutputStream(canonicalPath + "/rn.d"));
//		File f = new File(canonicalPath + "/rc");
//		f.mkdir();
////		DataOutputStream connStream = new DataOutputStream(new FileOutputStream(canonicalPath + "/rc.d"));
//		int[] connectionIndex = new int[nodes.size()];
//		int i = 0;
//		for (RouteNode rde : nodes.values()) {
//			rde.node.renumberdId = i++;
//			rde.id = rde.node.renumberdId;
//		}
//		i = 0;
//		nodeStream.writeInt(nodes.size());
//		for (RouteNode rde : nodes.values()) {
//			connectionIndex[i++] = nodeStream.size();
//			nodeStream.writeFloat(MyMath.degToRad(rde.node.lat));
//			nodeStream.writeFloat(MyMath.degToRad(rde.node.lon));
//			nodeStream.writeInt(rde.node.renumberdId);
//			System.out.println("id=" + rde.node.renumberdId);
//			nodeStream.writeByte(rde.connected.size());
//			DataOutputStream connStream = new DataOutputStream(new FileOutputStream(canonicalPath + "/" + rde.node.renumberdId + ".d"));
//			for (Connection c : rde.connected) {
//				connStream.writeInt(c.to.node.renumberdId);
//				System.out.println("RouteData.write(): only first route mode");
//				connStream.writeShort((int) c.times[0]); // only first route mode
//				connStream.writeShort((int) c.length);
//				connStream.writeByte(c.startBearing);
//				connStream.writeByte(c.endBearing);
//			}
//			connStream.close();
//		}
////		System.out.println("size " + ro.size());
//		nodeStream.close();
//		DataOutputStream indexStream = new DataOutputStream(new FileOutputStream(canonicalPath + "/rd.idx"));
//		for (int il : connectionIndex) {
//			indexStream.write(il);
//		}
//		indexStream.close();
//	}

	/** Remember traffic signals nodes in own array so they can be removed by CleanupData
	 * (traffic signals nodes must not be marked as used because otherwise they are written to the midlet) 
	 */
	public void rememberDelayingNodes() {
		Node[] delayingNodes = new Node[parser.trafficSignalCount];
		int i = 0;
		for (Node n:parser.getNodes()) {
			if (n.isTrafficSignals()) {
				delayingNodes[i++] = n;
			}
		}
		parser.setDelayingNodes(delayingNodes);
	}

}
