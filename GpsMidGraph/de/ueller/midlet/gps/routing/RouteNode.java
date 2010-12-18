package de.ueller.midlet.gps.routing;

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gpsMid.mapData.RouteBaseTile;
import de.ueller.gpsMid.mapData.RouteTile;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.Logger;


public class RouteNode {
	public float lat;
	public float lon;
//	public int conFp;
	private byte conSizeAndFlags;
	public int id;
//	public short fid;
	//#debug error
	private final static Logger logger=Logger.getInstance(RouteNode.class,Logger.TRACE);

	// the upper flags of consize are used to indicate special informations about the node
	public static final int CS_MASK_CONNECTEDLINECOUNT = 0x3F;
	public static final int CS_FLAG_HASTURNRESTRICTIONS = 0x80;
	public static final int CS_FLAG_TRAFFICSIGNALS_ROUTENODE = 0x40;

	
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
	
	public void setConSizeWithFlags(byte sizeAndFlags) {
		this.conSizeAndFlags=sizeAndFlags;
	}	

	public byte getConSize() {
		return (byte) ((int) conSizeAndFlags & CS_MASK_CONNECTEDLINECOUNT);
	}
	
	public boolean hasTurnRestrictions() {
		return (conSizeAndFlags & CS_FLAG_HASTURNRESTRICTIONS) > 0;
	}

	public boolean isAtTrafficSignals() {
		return (conSizeAndFlags & CS_FLAG_TRAFFICSIGNALS_ROUTENODE) > 0;
	}
	
	
	public String toString(){
		return "RouteNode(" + id +") RAD:" + lat + "/" + lon + " DEG: " + MoreMath.FAC_RADTODEC * lat + "/" + MoreMath.FAC_RADTODEC * lon + ")"; 
	}

}
