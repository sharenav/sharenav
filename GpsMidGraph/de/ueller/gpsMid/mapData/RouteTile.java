package de.ueller.gpsMid.mapData;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.routing.RouteTileRet;
import de.ueller.midlet.gps.tile.PaintContext;

public class RouteTile extends RouteBaseTile {

	RouteNode[] nodes=null;
	Connection[][] connections=null;
	//#debug error
	private final static Logger logger=Logger.getInstance(RouteTile.class,Logger.INFO);

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

	
	public RouteTile() {
		// TODO Auto-generated constructor stub
	}


	public boolean cleanup(int level) {
		if (nodes != null){
			if (level > 0 && !permanent){
				return false;
			}
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
		DataInputStream ts=new DataInputStream(GpsMid.getInstance().getConfig().getMapResource("/t4" + fileId + ".d"));
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
		if (contain(lat,lon,0.03f)){
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

	public RouteNode getRouteNode(float lat, float lon) {
		if (contain(lat,lon)){
			if (nodes == null){
				try {
					loadNodes();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			for (int i=0; i<nodes.length;i++){
				RouteNode n = nodes[i];
				if (n.lat == lat && n.lon == lon){
					return n;
				}
//				if (MoreMath.approximately_equal(n.lat,lat,0.000001f) &&
//					MoreMath.approximately_equal(n.lon,lon,0.000001f)){
//					System.out.println("aprox equal matches");
//					return n;
//				}
			}
			lastUse=0;
		}
		return null;
	}


	/* (non-Javadoc)
	 * @see de.ueller.gpsMid.mapData.RouteBaseTile#getRouteNode(float, float, de.ueller.midlet.gps.routing.RouteTileRet)
	 */
	public RouteNode getRouteNode(float lat,float lon,RouteTileRet retTile){
		RouteNode ret=getRouteNode(lat,lon);
		if (ret != null){
			this.permanent=true;
			if (this instanceof RouteTile){
				retTile.tile=(RouteTile) this;
			}
		}
		return ret;
	}


	public Connection[] getConnections(int id,RouteBaseTile tile,boolean bestTime){
		if (minId <= id && maxId >= id){
			lastUse=0;
			try {
				if (nodes == null){
					loadNodes();
				}
				if (connections == null){
					loadConnections(bestTime);
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
		//#debug error
		logger.error("catch connections at  "+(id-minId) + "(" + minId + "/" + maxId + ")");
		return null;
	}


	/**
	 * @param bestTime
	 * @throws IOException
	 */
	private void loadConnections(boolean bestTime) throws IOException {
		connections = new Connection[nodes.length][];
		//#debug error
		logger.debug("getConnections in file " + "/c" + fileId + ".d");
		DataInputStream cs=new DataInputStream(GpsMid.getInstance().getConfig().getMapResource("/c" + fileId + ".d"));
		for (int in=0; in<nodes.length;in++){
			RouteNode n=nodes[in];
			Connection[] cons=new Connection[n.conSize];
			for (int i = 0; i<n.conSize;i++){
				Connection c=new Connection();
				int nodeId = cs.readInt();
				// fill in TargetNode but only if in the same Tile
				if (nodeId <= maxId && nodeId >= minId){
					c.to=nodes[nodeId - minId];
				}
				// This is used as Key for HashMap later so use
				// an object.
				c.toId=nodeId;
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
	public String toString() {
		return "RT" + "-" + fileId + ":" + lastUse + ((permanent)?" perm":"");
	}


	public void addConnection(RouteNode rn, Connection newCon, boolean bestTime) {
		int addIdx=-1;
		for (int i=0; i<nodes.length;i++){
			RouteNode n = nodes[i];
			if (rn==n){
				addIdx=i;
				break;
			}
		}
		if (connections == null){
			try {
				loadConnections(bestTime);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (addIdx > 0 ){
			Connection[] cons=connections[addIdx];
			Connection[] newCons=new Connection[cons.length + 1];
			System.arraycopy(cons, 0, newCons, 0, cons.length);
			newCons[cons.length]=newCon;
			connections[addIdx]=newCons;
		}
	}

	public void paintAreaOnly(PaintContext pc) {
		// TODO Auto-generated method stub
		
	}


	public void paintNonArea(PaintContext pc) {
		paint(pc);		
	}


}
