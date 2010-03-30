/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsMid.mapData;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
//#if polish.api.osm-editing
import de.ueller.midlet.gps.data.EditableWay;
//#endif
import de.ueller.midlet.gps.data.Way;


public class QueueDataReader extends QueueReader implements Runnable {
    //#debug error
	private final static Logger logger = 
		Logger.getInstance(QueueDataReader.class, Logger.TRACE);

	private final Trace trace;

	public QueueDataReader(Trace trace) {
		super("DataReader");
		this.trace = trace;
		
	}
		
	public void readData(Tile t, Object notifyReady) throws IOException {
		logger.info("Reading tile: " + t);
		SingleTile tt = (SingleTile) t;
		InputStream is = Configuration.getMapResource("/t" + tt.zl + tt.fileId + ".d");
		if (is == null) {
		    //#debug error
			logger.error("File inputStream/t" + tt.zl + tt.fileId + ".d not found");
			tt.state = 0;
			return;
		}
//		logger.info("open DataInputStream");
		DataInputStream ds = new DataInputStream(is);
		if (ds == null) {
//			logger.error("file DataImputStream "+url+" not found" );
			tt.state = 0;
			is.close();
			return;
		}
//		end open data from JAR
//		logger.info("read Magic code");
		readSingleTile(tt, ds);
		ds.close();

		tt.dataReady();
		trace.newDataReady();
		if (notifyReady != null) {			
			synchronized(notifyReady) {
				notifyReady.notifyAll();
			}
		}
		//#debug debug
		logger.debug("DataReader ready "+ tt.fileId + " " + tt.nodeLat.length + 
				" Nodes " + tt.getWays().length + " Ways" );

//		}

	}

	/**
	 * @param tt
	 * @param ds
	 * @throws IOException
	 */
	private void readSingleTile(SingleTile tt, DataInputStream ds) throws IOException {
		if (ds.readByte() != 0x54) {
//			logger.error("not a MapMid-file");
			throwError( "Not a MapMid-file", tt);
		}
		tt.centerLat = ds.readFloat();
		tt.centerLon = ds.readFloat();
		//#debug debug
		logger.debug("Center coordinate of tile: " + tt.centerLat + "/" + tt.centerLon);
		int nodeCount = ds.readShort();
		short[] radlat = new short[nodeCount];
		short[] radlon = new short[nodeCount];
		int iNodeCount = ds.readShort();
		//#debug trace
		logger.trace("nodes total: " + nodeCount + " interestNode: " + iNodeCount);
		int[] nameIdx = new int[iNodeCount];
		for (int i = 0; i < iNodeCount; i++) {
			nameIdx[i] = -1;
		}
		byte[] type = new byte[iNodeCount];
		int[] osmID;
		//#if polish.api.osm-editing
		if (Legend.enableEdits) {
			osmID = new int[iNodeCount];
		} else {
			osmID = null;
		}
		//#else
		osmID = null;
		//#endif
		byte flag = 0;
		//#debug trace
		logger.trace("About to read nodes");
		try {
			for (int i = 0; i < nodeCount; i++) {
				//#debug error
				//	logger.info("read coord :"+i+"("+nodeCount+")");
				
				flag = ds.readByte();
		
				radlat[i] = ds.readShort();
				radlon[i] = ds.readShort();
				
				if ((flag & Legend.NODE_MASK_NAME) > 0) {
					if ((flag & Legend.NODE_MASK_NAMEHIGH) > 0) {
						nameIdx[i] = ds.readInt();
					} else {
						nameIdx[i] = ds.readShort();
					}
				} 
				if ((flag & Legend.NODE_MASK_TYPE) > 0) {
					type[i] = ds.readByte();
					//#if polish.api.osm-editing
					if (Legend.enableEdits) {
						osmID[i] = ds.readInt();
					}
					//#endif
					
				}
			}
			tt.nameIdx = nameIdx;
			tt.nodeLat = radlat;
			tt.nodeLon = radlon;
			tt.type = type;
		} catch (RuntimeException e) {
			throwError(e, "Reading nodes", tt);
		}
		logger.info("read nodes");
		readVerify((byte) 0x55,"start of ways " ,ds,tt);
		int wayCount = ds.readShort();
//		logger.trace("reading " + wayCount + " ways");
		int lastread = 0;
		try {
			Way[] tmpWays = new Way[wayCount];
			byte[] layers = new byte[wayCount];
			short[] layerCount = new short[5];			
			for (int i = 0; i < wayCount; i++) {
				byte flags = ds.readByte();
//				if (flags != 0x80) {
					Way w;
					//#if polish.api.osm-editing
					if (Legend.enableEdits) {
						w = new EditableWay(ds, flags, tt, layers, i);
					} else {
						w = new Way(ds, flags, tt, layers, i);
					}
					//#else
					w = new Way(ds, flags, tt, layers, i);
					w.wayNrInFile = (short) i;
					//#endif
					tmpWays[i] = w;
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
//				} else {
//					logger.debug("got empty Way");
//				}
				lastread = i;
				
			}			
			
			/**
			 * Split way list into different layers
			 */
			tt.setWays(new Way[5][]);
			for (int i = 0; i < 5; i++) {
				if (layerCount[i] > 0) {
					tt.getWays()[i] = new Way[layerCount[i]];
					int k = 0;
					for (int j = 0; j < wayCount; j++) {
						if (layers[j] == i) {
							tt.getWays()[i][k++] = tmpWays[j];
						}
					}
				}
			}			
		} catch (RuntimeException e) {			
			throwError(e, "Ways (last ok index " + lastread + " out of " + 
					wayCount + ")", tt);
		}
		readVerify((byte) 0x56,"read final magic value",ds,tt);
	}
	private void readVerify(byte expect,String msg,DataInputStream is, Tile tt){
		try {
			byte next=is.readByte();
			if (next == expect) {
				logger.debug("Verify Reader " + expect + " OK " +msg);
				return;
			}
			logger.debug("Error while verify Reader " + msg + " expect " + expect + " got " + next);
			for (int l=0; l<10 ; l++){
				logger.debug(" " + is.readByte());
			}
			throwError(msg, null);
			
		} catch (IOException e) {
			logger.error("Error while verify " + msg + " " + e.getMessage());
		}
	}


	private void throwError(String string, SingleTile tt) throws IOException {
		throw new IOException("MapMid-file corrupt: " + string + " zoomlevel=" + tt.zl + 
				" fid=" + tt.fileId);
		
	}

	private void throwError(RuntimeException rte, String string, SingleTile tt) throws IOException {
		throw new IOException("MapMid-file corrupt: " + string + 
				" Problem was in: zoomlevel=" + tt.zl + " fid=" + tt.fileId + ": " + 
				rte.getMessage());
	}

	public String toString() {
		int loop;
		StringBuffer ret = new StringBuffer();
		SingleTile tt;
		ret.append("\nliving ");
		for (loop = 0; loop < livingQueue.size(); loop++) {
			tt = (SingleTile) livingQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		ret.append("\nrequest ");
		for (loop = 0; loop < requestQueue.size(); loop++) {
			tt = (SingleTile) requestQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		return ret.toString();
	}

}
