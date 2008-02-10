package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.tile.PaintContext;

public class RouteTile extends RouteBaseTile {

	RouteNode[] nodes=null;
	Connection[][] connections=null;
	//#debug error
	private final static Logger logger=Logger.getInstance(RouteTile.class,Logger.TRACE);

	RouteTile(DataInputStream dis, int deep, byte zl) throws IOException {
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
    	minId=dis.readInt();
    	maxId=dis.readInt();
		fileId = (short) dis.readInt();
		//#debug error
		logger.debug("created RouteTile deep:" + deep + ":RT Nr=" + fileId + "id("+minId+"/"+maxId+")");
	}

	
	public boolean cleanup(int level) {
		if (nodes != null){
			if (lastUse >= level){
				nodes=null;
				connections=null;
				//#debug error
				logger.info("discard content for " + this);
				return true;
			} else {
				lastUse++;
				return false;
			}
		}
		return false;
	}

	public void paint(PaintContext pc) {
		if (pc == null){
			return;
		}
		if (contain(pc)){
			drawBounds(pc, 255, 255, 255);
			if (nodes == null){
				try {
					loadNodes();
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

			}
			pc.g.setColor(255, 100, 100);
			for (int i=0; i< nodes.length;i++){
				if (pc.isVisible(nodes[i].lat, nodes[i].lon)){
					pc.getP().forward(nodes[i].lat, nodes[i].lon, pc.swapLineP,true);
					pc.g.drawRect(pc.swapLineP.x-2, pc.swapLineP.y-2, 5, 5);
				}
			}
			lastUse=0;
		}
	}


	public RouteNode getRouteNode(int id) {
		if (minId <= id && maxId >= id){
			//#debug error
			logger.debug("getRouteNode("+id+")");
			lastUse=0;
			if (nodes == null){
				try {
					loadNodes();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			//#debug error
			logger.debug("getRouteNode("+id+") at "+(id-minId));
			return nodes[id - minId];
		} else 
			return null;
	}


	private void loadNodes() throws IOException {
		DataInputStream ts=new DataInputStream(QueueReader.openFile("/t4" + fileId + ".d"));
		short count = ts.readShort();
		//#debug error
		logger.debug("load nodes "+count+" ("+minId+"/"+maxId+") in Tile t4" + fileId + ".d");
		nodes = new RouteNode[count];
		for (short i = 0; i< count ; i++){
			RouteNode n = new RouteNode();
			n.lat=ts.readFloat();
			n.lon=ts.readFloat();
//			n.conFp=ts.readInt();
//			ts.readInt();
			n.conSize=ts.readByte();
//			n.fid=fileId;
			n.id=(short) (i+minId);
			nodes[i]=n;
		}
		ts.close();
	}
	


	public RouteNode getRouteNode(RouteNode best, float lat, float lon) {
		if (contain(lat,lon)){
			if (nodes == null){
				try {
					loadNodes();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			if (best == null){
				best=nodes[0];
			}
			int bestDist = MoreMath.dist(best.lat, best.lon, lat, lon);
			for (int i=0; i<nodes.length;i++){
				RouteNode n = nodes[i];
				int dist = MoreMath.dist(n.lat, n.lon, lat, lon);
				if (dist < bestDist){
					best=n;
					bestDist=dist;
				}
			}
			lastUse=0;
		}
		return best;
	}

	public Connection[] getConnections(int id,RouteBaseTile tile,boolean bestTime){
		if (minId <= id && maxId >= id){
			lastUse=0;
			try {
				if (nodes == null){
					loadNodes();
				}
				if (connections == null){
					connections = new Connection[nodes.length][];
					//#debug error
					logger.debug("getConnections("+id+") in file " + "/c" + fileId + ".d");
					DataInputStream cs=new DataInputStream(QueueReader.openFile("/c" + fileId + ".d"));
					for (int in=0; in<nodes.length;in++){
						RouteNode n=nodes[in];
						Connection[] cons=new Connection[n.conSize];
						for (int i = 0; i<n.conSize;i++){
							Connection c=new Connection();
							int nodeId = cs.readInt();
							if (nodeId <= maxId && nodeId >= minId){
								c.to=nodes[nodeId - minId];
							}
							// This is used as Key for HashMap later
							c.toId=new Integer(nodeId);
							if (bestTime){
								c.cost=cs.readShort();
								cs.readShort();
							} else {
								cs.readShort();
								c.cost=cs.readShort();								
							}
							c.startBearing=cs.readByte();
							c.endBearing=cs.readByte();
							cons[i]=c;
						}
						connections[in]=cons;
					}
				}
				//#debug error
				logger.debug("catch connections at  "+(id-minId) + "(" + minId + "/" + maxId + ")");
				return connections[id-minId];
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	public String toString() {
		return "RT" + "-" + fileId + ":" + lastUse;
	}


	public void paintAreaOnly(PaintContext pc) {
		// TODO Auto-generated method stub
		
	}


	public void paintNonArea(PaintContext pc) {
		paint(pc);		
	}

}
