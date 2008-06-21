package de.ueller.midlet.gps.routing;

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gpsMid.mapData.RouteBaseTile;
import de.ueller.gpsMid.mapData.RouteTile;
import de.ueller.midlet.gps.Logger;

public class RouteNode {
	public float lat;
	public float lon;
//	public int conFp;
	public byte conSize;
	public int id;
//	public short fid;
	//#debug error
	private final static Logger logger=Logger.getInstance(RouteNode.class,Logger.TRACE);

//	/** 
//	 * get one Connection from the file to save memory. But this is slow.
//	 * there is another version that takes all connections from one Tile
//	 * in the memory. This is much faster.
//	 * @see RouteTile#getConnections(int, RouteBaseTile)
//	 * @param tile
//	 * @return
//	 */ 
//	public Connection[] getConnections(RouteBaseTile tile,boolean bestTime){
//		//#debug error
//		logger.debug("getConnections("+this+") count=" + conSize + " in file " + "/c" + fid + ".d");
//		Connection[] cons=new Connection[conSize];
//		DataInputStream cs=new DataInputStream(QueueReader.openFile("/c" + fid + ".d"));
//		try {
//			//#debug error
//			logger.debug("seek to " + conFp);
//			cs.skipBytes(conFp);
//			for (int i = 0; i<conSize;i++){
//				Connection c=new Connection();
//				int nodeId = cs.readInt();
//				c.toId=new Integer(nodeId);
//				c.to=null;
//				if (bestTime){
//					c.cost=cs.readShort();
//					cs.readShort();
//				} else {
//					cs.readShort();
//					c.cost=cs.readShort();								
//				}
//				c.startBearing=cs.readByte();
//				c.endBearing=cs.readByte();
//				cons[i]=c;
//			}
//			cs.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//		return cons;
//	}
	
	public String toString(){
		return "RouteNode(" + id +") " + lat + "/" + lon + " "; 
	}

}
