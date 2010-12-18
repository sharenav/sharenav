package de.ueller.midlet.gps.routing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

public class RouteNodeTools {
	RecordStore routeNodeRecordStore;
	RecordStore connectionRecordStore;
	
	public RouteNodeTools() {
		try {
			routeNodeRecordStore=RecordStore.openRecordStore("RouteNode", false);
			connectionRecordStore=RecordStore.openRecordStore("Connection", false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public RouteNode getRouteNode(int i) throws Exception{
//		RouteNode rn=new RouteNode();
//		byte[]data=routeNodeRecordStore.getRecord(i);
//	    ByteArrayInputStream bin = new ByteArrayInputStream(data );
//	    DataInputStream din = new DataInputStream( bin );
//	    rn.lat= din.readFloat();
//	    rn.lon= din.readFloat();
//	    rn.conFp=din.readInt();
//	    rn.conSize= din.readByte();
////	    rn.id=new Integer(i);
//		return rn;
//	}
	
//	public Connection[] getConnections(RouteNode n) throws Exception{
//		Connection[] ret=new Connection[n.conSize];
//		for (int i=0; i<n.conSize;i++){
//			byte[]data=connectionRecordStore.getRecord(i+n.conFp);
//			ByteArrayInputStream bin = new ByteArrayInputStream(data );
//			DataInputStream din = new DataInputStream( bin );
//			Connection c = new Connection();
//			c.to = getRouteNode(din.readInt());
//			c.time= din.readShort();
//			c.length= din.readShort();
//			c.startBearing= din.readByte();
//			c.endBearing= din.readByte();
//			ret[i]=c;
//		}
//		return ret;
//	}
	
//	public static void initRecordStore(){
//		try {
//			DataInputStream nodeIs=new DataInputStream(Routing.class.getResourceAsStream("/rn.d"));
//			DataInputStream conIs=new DataInputStream(Routing.class.getResourceAsStream("/rc.d"));
//			try {
//				RecordStore.deleteRecordStore("RouteNode");
//				RecordStore.deleteRecordStore("Connection");
//			} catch (Exception e) {
//			}
//			RecordStore routeNodeRecordStore=RecordStore.openRecordStore("RouteNode", true);
//			RecordStore connectionRecordStore=RecordStore.openRecordStore("Connection", true);
//			int count=nodeIs.readInt();
//			byte connectionCount;
//			int toId;
//			int connRecordId=1;
//			Connection c=new Connection();
//			for (int l=0;l<count;l++){
//				RouteNode rn=new RouteNode();
//				rn.lat=nodeIs.readFloat();
//				rn.lon=nodeIs.readFloat();
//				rn.conFp=nodeIs.readInt();
//				connectionCount=nodeIs.readByte();
//				ByteArrayOutputStream brout = new ByteArrayOutputStream();
//				DataOutputStream drout = new DataOutputStream( brout );
//				drout.writeFloat(rn.lat);
//				drout.writeFloat(rn.lon);
//				drout.writeInt(connRecordId);
//				drout.writeByte(connectionCount);
//				byte[] rdata = brout.toByteArray();
//				routeNodeRecordStore.addRecord(rdata, 0, rdata.length);
//				for (int lc=0;lc<connectionCount;lc++){
//					connRecordId++;
//					toId=conIs.readInt();
//					c.time=conIs.readShort();
//					c.length=conIs.readShort();
//					c.startBearing=conIs.readByte();
//					c.endBearing=conIs.readByte();
//					ByteArrayOutputStream bout = new ByteArrayOutputStream();
//					DataOutputStream dout = new DataOutputStream( bout );
//					dout.writeInt(toId);
//					dout.writeShort(c.time);
//					dout.writeShort(c.length);
//					dout.writeByte(c.startBearing);
//					dout.writeByte(c.endBearing);
//					byte[] data = bout.toByteArray();
//					connectionRecordStore.addRecord(data, 0, data.length);
//				}
//
//			}
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
}
