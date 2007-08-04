/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;


import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.Line;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.RouteNode;
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
		for (Node n:parser.nodes.values()){
			n.connectedLineCount=0;
		}
		for (Line l:parser.lines.values()){
			l.from.connectedLineCount++;
			l.to.connectedLineCount++;
		}
		for (Way w:parser.ways){
			if (! w.isAccessByCar()){
				continue;
			}
			Node from=null;
			ArrayList<Node> nl=new ArrayList<Node>();
			for (Line l:w.lines){
				if (l.from == null || l.to == null){
					continue;
				}
				if (from == null){
					from=l.from;
					nl.add(from);
				}
				if (from.id != l.from.id){
					// not connected to previous line
					// so create the part up to here
					nl.add(from);
					addConnections(nl,w);
					// and start a new one
					from=l.from;
					nl.add(from);
				}
				nl.add(l.to);

				from=l.to;
			}
			addConnections(nl,w);
		}
	}

	/**
	 * @param nl
	 */
	private void addConnections(ArrayList<Node> nl,Way w) {
		RouteNode from=null;
		int lastIndex=nl.size();
		int thisIndex=0;
		long dist=0;
		int count=0;
		byte bearing=0;
		for (Node n:nl){
			thisIndex++;
			if (from==null){
				from=getRouteNode(n);
				count++;
			} else {
				dist += MyMath.dist(from.node, n);
				count++;
				if (count==2){
					bearing=MyMath.bearing_start(from.node, n);
				}
				if (thisIndex==lastIndex || (n.connectedLineCount != 2)){
					RouteNode next=getRouteNode(n);
					byte endBearing=MyMath.bearing_start(from.node,n);
					addConnection(from, next,dist,w,bearing,endBearing);
					from=next;
					dist=0;
					count=1;
				}
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
	private void addConnection(RouteNode from, RouteNode to, long dist, Way w,byte bs, byte be) {
		float speed=w.getSpeed();
		long time=(int)(dist/speed);
		nodes.put(from.node.id, from);
		nodes.put(to.node.id, to);
		Connection c=new Connection(to,dist,time,bs,be);
		from.connected.add(c);
		if (! w.isOneWay()){
			Connection cr=new Connection(from,dist,time,MyMath.inversBearing(be),MyMath.inversBearing(bs));
			to.connected.add(cr);
		}
		// need only for debugging not for live
		c.from=from;
		connections.add(c);
		
	}
	

	public boolean isRelevant(Node n){
		int count=0;
		for (Line l:parser.lines.values()){
			if (n.id == l.from.id){
				count++;
			}
			if (n.id == l.to.id){
				count++;
			}
		}
		if (count == 2){
			return false;
		} else {
			return true;
		}
	}
	
	
	
	
	/**
	 * normaly not used, only for test
	 * @param args
	 */
	public static void main(String[] args) {

				try {
					Configuration conf=new Configuration("/Massenspeicher/planet-070725.osm","bavaria");
					FileInputStream fr = new FileInputStream("/Massenspeicher/routetest.osm");
//					FileInputStream fr = new FileInputStream("/Massenspeicher/planet-070725.osm");
					OxParser parser = new OxParser(fr,conf);
					System.out.println("read Nodes " + parser.nodes.size());
					System.out.println("read Lines " + parser.lines.size());
					System.out.println("read Ways  " + parser.ways.size());
					RouteData rd=new RouteData(parser,"");
					rd.create();
					System.out.println("relNodes contain " + rd.nodes.size());
					RouteNode start=rd.nodes.get(new Long(26679764));
//					RouteNode target=rd.nodes.get(new Long(25844378));
//					RouteNode target=rd.nodes.get(new Long(33141402));
					RouteNode target=rd.nodes.get(new Long(27236345));
					AStar2 astar=new AStar2();
					Vector<Connection> solve = astar.solve(start, target);
					System.out.println("\n\nSolution:");
					PrintWriter fo = new PrintWriter("/Massenspeicher/routetestErg.osm");
					fo.write("<?xml version='1.0' encoding='UTF-8'?>\n");
					fo.write("<osm version='0.4' generator='JOSM'>\n");
					int rid=1;
					for (RouteNode r:rd.nodes.values()){
						r.node.renumberdId=rid++;
						fo.write("<node id='" + r.node.renumberdId);
						fo.write("' timestamp='2007-02-15 10:32:17' visible='true' lat='" +  r.node.lat);
						fo.write("' lon='" +r.node.lon + "'></node>\n");

					}
					RouteNode last=null;
					Connection lastCon=null;
					long id=1;
					int lb=0;
					for (Connection c:solve){
						if (last==null){
							last=c.to;
							lastCon=c;
							lb=c.endBearing;
						} else {
							System.out.println(c.printTurn(lastCon));
							fo.write("<segment id='"+ id++ + "' timestamp='2007-02-14 23:41:43' visible='true' from='" +
									last.node.renumberdId
									+ "' to='"
									+ c.to.node.renumberdId
									+ "'>\n");
							fo.write("  <tag k='length' v='"+c.length+"' />\n");
							fo.write("  <tag k='time' v='"+c.time+"' />\n");
							fo.write("  <tag k='bs' v='"+c.startBearing*2+"' />\n");
							fo.write("  <tag k='be' v='"+c.endBearing*2+"' />\n");
							fo.write("</segment>\n");
							last=c.to;
							lastCon=c;
						}
					} 
					long lastWayline=id;

//						for (Connection c: rd.connections){
//							fo.write("<segment id='"+ id++ + "' timestamp='2007-02-14 23:41:43' visible='true' from='" +
//									c.from.node.renumberdId
//									+ "' to='"
//									+ c.to.node.renumberdId
//									+ "'>\n");
//							fo.write("  <tag k='length' v='"+c.length+"' />\n");
//							fo.write("  <tag k='time' v='"+c.time+"' />\n");
//							fo.write("  <tag k='bs' v='"+c.startBearing*2+"' />\n");
//							fo.write("  <tag k='be' v='"+c.endBearing*2+"' />\n");
//							fo.write("</segment>\n");
//						
//					}
					fo.write("<way id='1' timestamp='2007-07-30 20:34:18' user='ulfl' visible='true'>\n");
					for (int l=1;l<lastWayline;l++){
					   fo.write("<seg id='"+l+"' />\n");	
					}
					fo.write("</way>\n");
					fo.write("</osm>");
					fo.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}

}
