package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
//#if ENABLE_EDIT
import de.ueller.midlet.gps.data.EditableWay;
//#endif
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.tile.C;


public class QueueDataReader extends QueueReader implements Runnable {
    //#debug error
	private final static Logger logger=Logger.getInstance(QueueDataReader.class,Logger.ERROR);

	private final Trace trace;
	public QueueDataReader(Trace trace){
		super("DataReader");
		this.trace = trace;
		
	}
		
	public void readData(Tile t, Object notifyReady) throws IOException{
		logger.info("Reading tile: " + t);
		SingleTile tt=(SingleTile) t;
		InputStream is=Configuration.getMapResource("/t"+tt.zl+tt.fileId+".d");
		if (is == null){
		    //#debug error
				logger.error("file inputStream"+"/t"+tt.zl+tt.fileId+".d"+" not found" );
			tt.state=0;
			return;
		}
//		logger.info("open DataInputStream");
		DataInputStream ds=new DataInputStream(is);
		if (ds == null){
//			logger.error("file DataImputStream "+url+" not found" );
			tt.state=0;
			return;
		}
//		end open data from JAR
//		logger.info("read Magic code");
		if (ds.readByte()!=0x54){
//			logger.error("not a MapMid-file");
			throwError( "not a MapMid-file", tt);
		}
		tt.centerLat = ds.readFloat();
		tt.centerLon = ds.readFloat();
		//#debug debug
		logger.debug("Center coordinates of tile: " + tt.centerLat + "/" + tt.centerLon);
		int nodeCount=ds.readShort();
		short[] radlat = new short[nodeCount];
		short[] radlon = new short[nodeCount];
		int iNodeCount=ds.readShort();
		//#debug trace
		logger.trace("nodes total :"+nodeCount + "  interestNode :" + iNodeCount);
		int[] nameIdx=new int[iNodeCount];
		for (int i = 0; i < iNodeCount; i++) {
			nameIdx[i] = -1;
		}
		byte[] type = new byte[iNodeCount];
		int[] osmID;
		//#if ENABLE_EDIT
		if (C.enableEdits) {
			osmID = new int[iNodeCount];
		} else {
			osmID = null;
		}
		//#else
		osmID = null;
		//#endif
		byte flag=0;
		//#debug trace
		logger.trace("About to read nodes");
		try {
			for (int i=0; i< nodeCount;i++){
				//#debug error
				//	logger.info("read coord :"+i+"("+nodeCount+")");
				
				// FIXME: reading the RouteNodeLink is obsolete as it is no more written by Osm2GpsMid, it should be removed on the next MAP_VERSION update
				flag=ds.readByte();
				if ((flag & C.NODE_MASK_ROUTENODELINK) > 0){
					ds.readShort();
				}
								
				radlat[i] = ds.readShort();
				radlon[i] = ds.readShort();
				
				if ((flag & C.NODE_MASK_NAME) > 0){
					if ((flag & C.NODE_MASK_NAMEHIGH) > 0) {
						nameIdx[i]=ds.readInt();
					} else {
						nameIdx[i]=ds.readShort();
					}
				} 
				if ((flag & C.NODE_MASK_TYPE) > 0){
					type[i]=ds.readByte();
					//#if ENABLE_EDIT
					if (C.enableEdits) {
						osmID[i] = ds.readInt();
					}
					//#endif
					
				}
			}
			tt.nameIdx=nameIdx;
			tt.nodeLat=radlat;
			tt.nodeLon=radlon;
			tt.type=type;
		} catch (RuntimeException e) {
			throwError(e, "reading Nodes", tt);
		}
		logger.info("read nodes");
		if (ds.readByte()!=0x55){			
			logger.error("Reading Nodes whent wrong / Start of Ways not found");
			throwError("Nodes not OK", tt);
		}
		int wayCount=ds.readByte();
//		logger.trace("reading " + wayCount + " ways");
		if (wayCount < 0) {
			wayCount+=256;
		}
//		logger.trace("reading " + wayCount + " ways");
		int lastread=0;
		try {
			Way[] tmpWays = new Way[wayCount];
			byte[] layers = new byte[wayCount];
			short[] layerCount = new short[5];			
			for (int i=0; i< wayCount;i++){				
				byte flags=ds.readByte();
				if (flags != 128){
					Way w;
					//#if ENABLE_EDIT
					if (C.enableEdits) {
						w = new EditableWay(ds,flags,tt,layers,i);
					} else {
						w = new Way(ds,flags,tt,layers,i);
					}
					//#else
					w=new Way(ds,flags,tt, layers, i);
					//#endif
					tmpWays[i]=w;
					/**
					 * To save resources, only allow layers -2 .. +2
					 */
					if (layers[i] < -2) {
						layers[i] = -2;
					} else if (layers[i] > 2) {
						layers[i] = 2;
					}
					layers[i] += 2;
					layerCount[layers[i]]++;
				}
				lastread=i;
			}			
			
			/**
			 * Split way list into different layers
			 */
			tt.ways = new Way[5][];
			for (int i = 0; i < 5; i++) {
				if (layerCount[i] > 0) {
					tt.ways[i] = new Way[layerCount[i]];
					int k = 0;
					for (int j = 0; j < wayCount; j++) {
						if (layers[j] == i) {
							tt.ways[i][k++] = tmpWays[j];
						}
					}
				}
			}			
		} catch (RuntimeException e) {			
			throwError(e,"Ways(last ok index " + lastread + " out of " + wayCount +")",tt);
		}
		if (ds.readByte() != 0x56){
			throwError("Ways not OK, failed to read final magic value", tt);
		} else {
//			logger.info("ready");
		}
		ds.close();

		tt.dataReady();
		trace.newDataReady();
		if (notifyReady != null) {			
			synchronized(notifyReady) {
				notifyReady.notifyAll();
			}
		}
		//#debug debug
		logger.debug("DataReader ready "+ tt.fileId + " " + tt.nodeLat.length + " Nodes " + tt.ways.length + " Ways" );

//		}

	}
	private void throwError(String string, SingleTile tt) throws IOException {
		throw new IOException("MapMid-file corrupt: " + string + " zl=" + tt.zl + " fid=" + tt.fileId);
		
	}
	private void throwError(RuntimeException e, String string, SingleTile tt) throws IOException {
		e.printStackTrace();
		throw new IOException("MapMid-file corrupt: " + string + " Problem was in: zl=" + tt.zl + " fid=" + tt.fileId + " :" + e.getMessage());
	}
	public String toString(){
		int loop;
		StringBuffer ret=new StringBuffer();
		SingleTile tt;
		ret.append("\nliving ");
		for (loop=0; loop < livingQueue.size(); loop++){
			tt=(SingleTile) livingQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		ret.append("\nrequest ");
		for (loop=0; loop < requestQueue.size(); loop++){
			tt=(SingleTile) requestQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		return ret.toString();
	}

}
