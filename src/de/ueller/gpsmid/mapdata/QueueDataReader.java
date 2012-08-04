/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.mapdata;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.tile.SingleTile;
import de.ueller.gpsmid.tile.Tile;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.util.Logger;
//#if polish.api.osm-editing
import de.ueller.gpsmid.data.EditableWay;
//#endif

import de.enough.polish.util.Locale;


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
		// work around overflows
		InputStream is = Configuration.getMapResource("/t" + tt.zl + "/" +
							      (tt.fileId < 0 ?
							       tt.fileId + 65536 : tt.fileId)
							      + ".d");
		if (is == null) {
		    //#debug error
			logger.error(Locale.get("queuedatareader.FileInputStream")/*File inputStream/t*/ 
					+ tt.zl + "/" + tt.fileId + Locale.get("queuedatareader.dNotFound")/*.d not found*/);
			tt.setState((byte)0);
			return;
		}
//		logger.info("open DataInputStream");
		DataInputStream ds = new DataInputStream(is);
		if (ds == null) {
//			logger.error("file DataImputStream "+url+" not found" );
			tt.setState((byte)0);
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
			throwError( Locale.get("queuedatareader.NotMapMidFile")/*Not a MapMid-file*/, tt);
		}
		tt.centerLat = ds.readFloat();
		tt.centerLon = ds.readFloat();
		//#debug debug
		logger.debug("Center coordinate of tile: " + tt.centerLat + "/" + tt.centerLon);
		int nodeCount = ds.readShort();
		if (nodeCount < 0) {
		    nodeCount += 65536;
		}
		short[] radlat = new short[nodeCount];
		short[] radlon = new short[nodeCount];
		int iNodeCount = ds.readShort();
		if (iNodeCount < 0) {
		    iNodeCount += 65536;
		}
		//#debug trace
		logger.trace("nodes total: " + nodeCount + " interestNode: " + iNodeCount);
		int[] nameIdx = new int[iNodeCount];
		int[] urlIdx;
		int[] phoneIdx;
		if (Legend.enableUrlTags) {
			urlIdx=new int[iNodeCount];
		} else {
			urlIdx = null;
		}
		if (Legend.enablePhoneTags) {
			phoneIdx=new int[iNodeCount];
		} else {
			phoneIdx = null;
		}
		for (int i = 0; i < iNodeCount; i++) {
			nameIdx[i] = -1;
		}
		//#if polish.api.bigstyles
		short[] type = new short[iNodeCount];
		//#else
		byte[] type = new byte[iNodeCount];
		//#endif
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
		byte flag2 = 0;
		//#debug trace
		logger.trace("About to read nodes");
		try {
			for (int i = 0; i < nodeCount; i++) {
				//#debug error
				//	logger.info("read coord :"+i+"("+nodeCount+")");
				
				flag = ds.readByte();
				flag2 = 0;
				if ((flag & Legend.NODE_MASK_ADDITIONALFLAG) == Legend.NODE_MASK_ADDITIONALFLAG) {
					flag2 = ds.readByte();
				}
		
				radlat[i] = ds.readShort();
				radlon[i] = ds.readShort();
				
				if ((flag & Legend.NODE_MASK_NAME) > 0) {
					if ((flag & Legend.NODE_MASK_NAMEHIGH) > 0) {
						nameIdx[i] = ds.readInt();
					} else {
						nameIdx[i] = ds.readShort();
					}
				} 
				if ((flag & Legend.NODE_MASK_URL) > 0){
					if ((flag & Legend.NODE_MASK_URLHIGH) > 0) {
						urlIdx[i]=ds.readInt();
					} else {
						urlIdx[i]=ds.readShort();
					}
				} 

				if ((flag2 & Legend.NODE_MASK2_PHONE) > 0){
					if ((flag2 & Legend.NODE_MASK2_PHONEHIGH) > 0) {
						phoneIdx[i]=ds.readInt();
					} else {
						phoneIdx[i]=ds.readShort();
					}
				} 
				if ((flag & Legend.NODE_MASK_TYPE) > 0) {
					//#if polish.api.bigstyles
					if (Legend.enableBigStyles) {
						type[i] = ds.readShort();
					} else {
						type[i] = (short) (ds.readByte() & 0xff);
					}
					//#else
					type[i] = ds.readByte();
					//#endif
					//#if polish.api.osm-editing
					if (Legend.enableEdits) {
						osmID[i] = ds.readInt();
					}
					//#else
					if (Legend.enableEdits) {
						int x = ds.readInt();
					}
					//#endif
					
				}
			}
			tt.nameIdx = nameIdx;
			tt.urlIdx = urlIdx;
			tt.phoneIdx = phoneIdx;
			tt.nodeLat = radlat;
			tt.nodeLon = radlon;
			tt.type = type;
			//#if polish.api.osm-editing
			if (Legend.enableEdits) {
				tt.osmID = osmID;
			}
			//#endif
		} catch (RuntimeException e) {
			throwError(e, "Reading nodes", tt);
		}
		logger.info("read nodes");
		readVerify((byte) 0x55,"start of ways " ,ds);
		int wayCount = ds.readShort();
		if (wayCount < 0) {
			wayCount += 65536;
		}
//		logger.trace("reading " + wayCount + " ways");

		// FIXME check from legend if the outline format data exists
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS)
		    //#if polish.api.areaoutlines
		    && true
		    //#else
		    && false
		    //#endif
			) {
			byte[] ignoreLayers = new byte[wayCount];
			// read & ignore the old format ways & areas
			for (int i = 0; i < wayCount; i++) {
				SingleTile dummyTile = new SingleTile();
				byte flags = ds.readByte();
				//#if polish.api.osm-editing
				if (Legend.enableEdits) {
					new EditableWay(ds, flags, dummyTile, ignoreLayers, i);
				} else {
					new Way(ds, flags, dummyTile, ignoreLayers, i);
				}
				//#else
				new Way(ds, flags, tt, ignoreLayers, i);
				if (Legend.enableEdits) {
					ds.readInt();
				}
				//#endif
			}
			readVerify((byte) 0x56,"read final magic value",ds);
			wayCount = ds.readShort();
			// read wayCount for the areaoutline way block
			if (wayCount < 0) {
				wayCount += 65536;
			}
		}

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
					if (Legend.enableEdits) {
						int x = ds.readInt();
					}
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
			 * TODO: shouldn't this moved to OSM2GpsMid 
			 * I'll suggest that in font of ways are 5 short values
			 * for each layer one value for the count of ways in this layer.
			 * So the Way itself don't need a layer and all array can create 
			 * at start and have not to be sorted afterward into the correct layer.
			 * This saves allocation of tempWays!
			 * if more then 10 ways/areas have a layer this version will save bytes.
			 * 
			 * a value for the layer
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
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS)
		    //#if polish.api.areaoutlines
		    && true
		    //#else
		    && false
		    //#endif
			) {
			readVerify((byte) 0x57,"read final magic value",ds);
		} else {
			readVerify((byte) 0x56,"read final magic value",ds);
		}
	}
	
	private void readVerify(byte expect,String msg,DataInputStream is){
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
			logger.error(Locale.get("queuedatareader.ErrorWhileVerify")/* Error while verify */ 
					+ msg + " " + e.getMessage());
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
			ret.append(tt.toString())
			   .append(" ");
		}
		ret.append("\nrequest ");
		for (loop = 0; loop < requestQueue.size(); loop++) {
			tt = (SingleTile) requestQueue.elementAt(loop);
			ret.append(tt.toString())
			   .append(" ");
		}
		return ret.toString();
	}

}
