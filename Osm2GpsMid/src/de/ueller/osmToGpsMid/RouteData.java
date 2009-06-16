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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.RouteNode;
import de.ueller.osmToGpsMid.model.SubPath;
import de.ueller.osmToGpsMid.model.TravelMode;
import de.ueller.osmToGpsMid.model.TravelModes;
import de.ueller.osmToGpsMid.model.TurnRestriction;
import de.ueller.osmToGpsMid.model.Way;

/**
 * @author hmueller
 *
 */
public class RouteData {
	private OxParser parser;
	private String path;
	public Map<Long,RouteNode> nodes = new HashMap<Long,RouteNode>();
	public ArrayList<Connection> connections = new ArrayList<Connection>();

	public RouteData(OxParser parser,String path) {
		super();
		this.parser = parser;
		this.path = path;
	}
	
	public void create(){
		// reset connectedLineCount for each Node to 0
		for (Node n:parser.getNodes()){
			n.connectedLineCount=0;
		}
		// count all connections for all nodes
		for (Way w:parser.getWays()){
			w.determineWayRouteModes();
			if (! w.isAccessForAnyRouting()){
				continue;
			}
			//TODO: explain what are subpaths?
			for (SubPath s:w.getSubPaths()){
				Node lastNode=null;
				for (Node n:s.getNodes()){
					n.connectedLineCount++;
					if (lastNode != null){
						n.connectedLineCount++;
					}
					lastNode=n;
				}
				if (lastNode != null){
					lastNode.connectedLineCount--;
				}
			}
		}
		 
		for (Way w:parser.getWays()){
			if (!w.isAccessForAnyRouting()){
				continue;
			}
			for (SubPath s:w.getSubPaths()){
				addConnections(s.getNodes(),w);
			}
		}
		createIds();
		calculateTurnRestrictions();
	}

	
	private void calculateTurnRestrictions() {
		System.out.println("calculating turn restrictions");
		int numTurnRestrictions = 0;
		for (RouteNode n: nodes.values()){
			TurnRestriction turn = (TurnRestriction) parser.getTurnRestrictionHashMap().get(new Long(n.node.id));
			while (turn!=null) {
				Way restrictionFromWay = parser.getWayHashMap().get(turn.fromWayRef);
				// skip if restrictionFromWay is not in available wayData				
				if (restrictionFromWay == null) {
					continue;
				}
				// skip if restrictionToWay is not in available wayData				
				Way restrictionToWay = parser.getWayHashMap().get(turn.toWayRef);
				if (restrictionToWay == null) {
					continue;
				}
				
				turn.viaRouteNodeId = n.id;
				turn.viaLat = n.node.lat;
				turn.viaLon = n.node.lon;				
				//System.out.println(turn.toString(parser.getWayHashMap()));
				int numFromConnections = 0;
				for (Connection c:n.connectedFrom) {
					if (restrictionFromWay.containsNode(c.from.node)) {
						turn.fromRouteNodeId = c.from.id;
						numFromConnections++;
					}
				}
				if (numFromConnections != 1) {
					System.out.println("Warning: " + numFromConnections + " from_connections matched for: " + turn.toString(parser.getWayHashMap()));
				}
				int numToConnections = 0;
				for (Connection c:n.connected) {
					if (restrictionToWay.containsNode(c.to.node)) {
						turn.toRouteNodeId = c.to.id;
						numToConnections++;
					}
				}
				if (numToConnections != 1) {
					System.out.println("Warning: " + numToConnections + " to_connections matched for: "  + turn.toString(parser.getWayHashMap()));
				}
				if (numFromConnections == 1 && numToConnections == 1) {
					numTurnRestrictions++;
				}
				
				turn = turn.nextTurnRestrictionAtThisNode;
			}
		}
		System.out.println(numTurnRestrictions + " turn restrictions valid");
	}


	/**
	 * @param nl
	 */
	// TODO: explain
	private void addConnections(List<Node> nl,Way w) {
		RouteNode from=null;
		int lastIndex=nl.size();
		Node lastNode=null;
		int thisIndex=0;
		int dist=0;
		int count=0;
		byte bearing=0;
		for (Node n:nl){
			thisIndex++;
			if (from==null){
				lastNode=n;
				from=getRouteNode(n);
				count++;
				dist=0;
			} else {
				dist += MyMath.dist(lastNode, n);
				count++;
				if (count==2){
					bearing=MyMath.bearing_start(lastNode, n);
				}
				if (thisIndex==lastIndex || (n.connectedLineCount != 2)){
					RouteNode next=getRouteNode(n);
					byte endBearing=MyMath.bearing_start(lastNode,n);
					addConnection(from, next, dist, w, bearing, endBearing);
					from=next;
					dist=0;
					count=1;
				}
				lastNode=n;
			}
		}
		nl=new ArrayList<Node>();
	}

	/**
	 * @param l
	 */
	private RouteNode getRouteNode(Node n) {
		RouteNode routeNode;
		if (! nodes.containsKey(n.id)){
			routeNode = new RouteNode(n);
			n.routeNode=routeNode;
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
		
		byte againstDirectionTravelModes = 0;		
		
		// create an array of routing times with an entry for each travel mode
		int times[] = new int[TravelModes.travelModeCount];
		for (int i=0; i<TravelModes.travelModeCount; i++) {
			if (w.isAccessForRouting(i)) {
				TravelMode tm = TravelModes.getTravelMode(i);
				if (w.isExplicitArea()) {
					tm.numAreaCrossConnections++;
				}
				tm.numOneWayConnections++;
				float speed=w.getRoutingSpeed(i);
				float time=dist * 10.0f / speed;
				times[i] = (int)time;

				boolean bicycleOppositeDirection = (tm.againstOneWayMode & TravelMode.BICYLE_OPPOSITE_EXCEPTIONS) > 0 && w.isOppositeDirectionForBicycleAllowed();				
				// you can go against the direction of the way if it's not a oneway or an against direction rule applies
				if (! (w.isOneWay() || w.isRoundabout())
					||
					(tm.againstOneWayMode & TravelMode.AGAINST_ALL_ONEWAYS) > 0
					||
					bicycleOppositeDirection
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
		
		nodes.put(from.node.id, from);
		nodes.put(to.node.id, to);
		Connection c=new Connection(to,dist,times,bs,be,w);
		from.connected.add(c);
		to.connectedFrom.add(c);
		// roundabouts don't need to be explicitly tagged as oneways in OSM according to http://wiki.openstreetmap.org/wiki/Tag:junction%3Droundabout
		if (againstDirectionTravelModes != 0 ){
			// add connection in the other direction as well, if this is no oneWay
			// TODO: explain Doesn't this add duplicates when addconnection() is called later on with from and to exchanged or does this not happen?
			Connection cr=new Connection(from,dist,times,MyMath.inversBearing(be),MyMath.inversBearing(bs),w);
			cr.from=to;
			to.connected.add(cr);
			from.connectedFrom.add(cr);
			connections.add(cr);

			// flag connections useable for travel modes you can go against the ways direction
			cr.wayTravelModes = againstDirectionTravelModes;

			for (int i=0; i<TravelModes.travelModeCount; i++) {
				if ( (againstDirectionTravelModes & (1<<i)) != 0 ) {
					TravelMode tm = TravelModes.getTravelMode(i);
					tm.numDualConnections++;
					tm.numOneWayConnections--;
				}
			}
		}
		// need only for debugging not for live
		c.from=from;
		connections.add(c);
		
	}
	
    @Deprecated
	public boolean isRelevant(Node n){
		int count=0;
//		for (Line l:parser.lines.values()){
//			if (n.id == l.from.id){
//				count++;
//			}
//			if (n.id == l.to.id){
//				count++;
//			}
//		}
		if (count == 2){
			return false;
		} else {
			return true;
		}
	}
	
	private void createIds(){
		int id=1;
		for (RouteNode n: nodes.values()){
			n.id=id++;
		}
	}
	
	public void optimise(){
//		System.out.println("RoutNodes for optimise " + nodes.size());
//		ArrayList<RouteNode> removeNodes=new ArrayList<RouteNode>();
//		for (RouteNode n:nodes.values()){
//			// find nodes that are only a point between to nodes without
//			// any junction. This test does not cover one ways
//			// for normal ways the second connection will removed 
//			if (n.connected.size() == 2 && n.connectedFrom.size()==2){
//				Connection c1=n.connected.get(0);
//				RouteNode n1=c1.to;
//				Connection c2=null;
//				Connection c3=n.connected.get(1);
//				RouteNode n2=c3.to;
//				Connection c4=null;
//				for (Connection ct:n.connectedFrom){
//					if (ct.from == n2 ){
//						c2=ct;
//					} 
//					if (ct.from == n1){
//						c4=ct;
//					}
//				}
//				if (c2 != null && c4 != null){
//					if (c2.to != n){
//						System.out.println("c2.to != n");
//					}
//					if (c4.to != n){
//						System.out.println("c4.to != n");
//					}
//					if (c1.to != c4.from){
//						System.out.println("c1.to != c4.from");
//					}
//					if (c2.from != c3.to){
//						System.out.println("c2.from != c3.to");
//					}
//					c2.endBearing=c1.endBearing;
//					c2.time += c1.time;
//					c2.length += c1.length;
//					c2.to=c1.to;
//					c3.from=c1.to;
//					c3.startBearing=MyMath.inversBearing(c1.endBearing);
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
//			if (n.connected.size() == 1 && n.connectedFrom.size()==1){
//				Connection c1=n.connected.get(0);
//				RouteNode n1=c1.to;
//				Connection c2=n.connectedFrom.get(0);
//				RouteNode n2=c2.from;
//				if (n2 != n1){
//					c2.endBearing=c1.endBearing;
//					c2.time += c1.time;
//					c2.length += c1.length;
//					c2.to=c1.to;
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
//		System.out.println("remove " + removeNodes.size() + " RouteNode due optimation");
//		for (RouteNode n:removeNodes){
//			n.node.routeNode=null;
//			nodes.remove(n.node.id);
//		}
	}
	
	
	/**
	 * normaly not used, only for test
	 * @param args
	 */
	public static void main(String[] args) {

				try {					
					Configuration conf=new Configuration(args);
					FileInputStream fr = new FileInputStream("/Massenspeicher/myStreetMap0.5.osm");
//					FileInputStream fr = new FileInputStream("/Massenspeicher/planet-070725.osm");
					OxParser parser = new OxParser(fr,conf);
					System.out.println("read Nodes " + parser.getNodes().size());
					System.out.println("read Ways  " + parser.getNodes().size());
					new CleanUpData(parser,conf);
					RouteData rd=new RouteData(parser,"");

					rd.create();
					
					int rid=10000;
//					for (RouteNode r:rd.nodes.values()){
//						r.node.renumberdId=rid++;
//					}
					rid=1;
					rd.optimise();
					for (RouteNode r:rd.nodes.values()){
						r.node.renumberdId=rid++;
					}

//					rd.write("/Temp");
					System.out.println("relNodes contain " + rd.nodes.size());
					System.out.println("Connections contain " + rd.connections.size());
					RouteNode start=rd.nodes.get(new Long(1955808));
					System.out.println("start " + start);
//					RouteNode target=rd.nodes.get(new Long(25844378));
//					RouteNode target=rd.nodes.get(new Long(33141402));
//					RouteNode target=rd.nodes.get(new Long(28380647));
//					System.out.println("target " + target);
//					AStar2 astar=new AStar2();
//					Vector<Connection> solve = astar.solve(start, target);
//					System.out.println("\n\nSolution:");
					PrintWriter fo = new PrintWriter("/Massenspeicher/routetestErg.osm");
//					exportResultOSM(fo, rd, solve);
					fo = new PrintWriter("/Massenspeicher/routetestConnections.osm");
					exportResultOSM(fo, rd, null);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
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
		int rid=1;
		for (RouteNode r:rd.nodes.values()){
			fo.write("<node id='" + r.node.renumberdId);
			fo.write("' timestamp='2007-02-15 10:32:17' visible='true' lat='" +  r.node.lat);
			fo.write("' lon='" +r.node.lon + "'>\n");
			fo.write("  <tag k='connectCount' v='" + r.node.connectedLineCount + "'/>\n");
			fo.write("  <tag k='connectTo' v='");
			for (Connection c:r.connected){
				fo.write(","+c.to.node.renumberdId);
			}
			fo.write("'/>\n");
			fo.write("  <tag k='connectFrom' v='");
			for (Connection c:r.connectedFrom){
				fo.write(","+c.from.node.renumberdId);
			}
			fo.write("'/>\n");
			fo.write("</node>\n");

		}
		int id=1;
		for (RouteNode r:rd.nodes.values()){
			for (Connection c:r.connected){
		fo.write("<way id='"+ id++ + "' timestamp='2007-02-14 23:41:43' visible='true' >\n");
		fo.write("  <nd ref='"+c.from.node.renumberdId+"'/>\n");
		fo.write("  <nd ref='"+c.to.node.renumberdId+"'/>\n");
		fo.write("  <tag k='name' v='laenge="+c.length+"' />\n");
		System.out.println("RouteData.exportResultOSM(): only first route mode");
		fo.write("  <tag k='time' v='"+c.times[0]+"' />\n");
		fo.write("  <tag k='bs' v='"+c.startBearing*2+"' />\n");
		fo.write("  <tag k='be' v='"+c.endBearing*2+"' />\n");
		fo.write("</way>\n");
		}
	}
		
		
		RouteNode last=null;
		Connection lastCon=null;
		int lb=0;
//		if (solve != null) {
//		for (Connection c:solve){
//			if (last==null){
//				last=c.to;
//				lastCon=c;
//				lb=c.endBearing;
//			} else {
//				System.out.println(c.printTurn(lastCon));
//				fo.write("<segment id='"+ id++ + "' timestamp='2007-02-14 23:41:43' visible='true' from='" +
//						last.node.renumberdId
//						+ "' to='"
//						+ c.to.node.renumberdId
//						+ "'>\n");
//				fo.write("  <tag k='length' v='"+c.length+"' />\n");
//				fo.write("  <tag k='time' v='"+c.time+"' />\n");
//				fo.write("  <tag k='bs' v='"+c.startBearing*2+"' />\n");
//				fo.write("  <tag k='be' v='"+c.endBearing*2+"' />\n");
//				fo.write("</segment>\n");
//				last=c.to;
//				lastCon=c;
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
		FileOutputStream fo = new FileOutputStream(path+"/t"+zl+fid+".d");
		DataOutputStream tds = new DataOutputStream(fo);
		for (Node n: nodes2){
			if (nodes.containsKey(n.id)){
				RouteNode rn=nodes.get(n.id);
				
			}
		}
	}

	/**
	 * deprecated but still used by routeTiles 
	 * @param canonicalPath
	 * @throws IOException 
	 */
	@Deprecated
	public void write(String canonicalPath) throws IOException {
		DataOutputStream nodeStream = new DataOutputStream(new FileOutputStream(canonicalPath+"/rn.d"));
		File f=new File(canonicalPath+"/rc");
		f.mkdir();
//		DataOutputStream connStream = new DataOutputStream(new FileOutputStream(canonicalPath+"/rc.d"));
		int[] connectionIndex=new int[nodes.size()];
		int i=0;
		for (RouteNode rde : nodes.values()){
			rde.node.renumberdId=i++;
			rde.id=rde.node.renumberdId;
		}
		i=0;
		nodeStream.writeInt(nodes.size());
		for (RouteNode rde : nodes.values()){
			connectionIndex[i++]= nodeStream.size();
			nodeStream.writeFloat(MyMath.degToRad(rde.node.lat));
			nodeStream.writeFloat(MyMath.degToRad(rde.node.lon));
			nodeStream.writeInt(rde.node.renumberdId);
			System.out.println("id="+rde.node.renumberdId);
			nodeStream.writeByte(rde.connected.size());
			DataOutputStream connStream = new DataOutputStream(new FileOutputStream(canonicalPath+"/"+rde.node.renumberdId+".d"));
			for (Connection c : rde.connected){
				connStream.writeInt(c.to.node.renumberdId);
				System.out.println("RouteData.write(): only first route mode");
				connStream.writeShort((int) c.times[0]); // only first route mode
				connStream.writeShort((int) c.length);
				connStream.writeByte(c.startBearing);
				connStream.writeByte(c.endBearing);
			}
			connStream.close();
		}
//		System.out.println("size " + ro.size());
		nodeStream.close();
		DataOutputStream indexStream = new DataOutputStream(new FileOutputStream(canonicalPath+"/rd.idx"));
		for (int il : connectionIndex){
			indexStream.write(il);
		}
		indexStream.close();
	}

}
