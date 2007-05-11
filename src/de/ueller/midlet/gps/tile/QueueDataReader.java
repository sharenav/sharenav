package de.ueller.midlet.gps.tile;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.Way;


public class QueueDataReader extends QueueReader implements Runnable {
	private final Trace trace;
	public QueueDataReader(Trace trace){
		super();
		this.trace = trace;
		
	}
	public synchronized void add(SingleTile st){
		st.lastUse=0;
		requestQueue.addElement(st);
//		logger.info("add " + st.fileId + " to queue size=" + requestQueue.size());
		notify();
	}
	protected void readData(Tile t) throws IOException{
		SingleTile tt=(SingleTile) t;
		InputStream is=openFile("/map/t"+tt.zl+tt.fileId+".d");
		if (is == null){
//			logger.error("file inputStream"+url+" not found" );
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
			throw new IOException("not a MapMid-file");
		}
		int nodeCount=ds.readShort();
		float[] radlat = new float[nodeCount];
		float[] radlon = new float[nodeCount];
		int iNodeCount=ds.readShort();
//		logger.trace("nodes total :"+nodeCount + "  interestNode :" + iNodeCount);
		Short[] nameIdx=new Short[iNodeCount];
		byte[] type = new byte[iNodeCount];
		for (int i=0; i< nodeCount;i++){
//			logger.trace("read coord :"+nodeCount);
			radlat[i] = ds.readFloat();
			radlon[i] = ds.readFloat();
			if (i < iNodeCount){
//				logger.trace("read ext :"+i+"/"+nodeCount );
				short name=ds.readShort();
				if ( name != 0){
					nameIdx[i]=new Short(name);
				} else {
					nameIdx[i]=null;
				}
//				logger.trace("read type :"+i+"/"+nodeCount);
				type[i]=ds.readByte();
			}
		}
		tt.nameIdx=nameIdx;
		tt.nodeLat=radlat;
		tt.nodeLon=radlon;
		tt.type=type;
		if (ds.readByte()!=0x55){
//			logger.error("Start of Ways not found");
			throw new IOException("MapMid-file corrupt: Nodes not OK");
		}
		int wayCount=ds.readByte();
//		logger.trace("reading " + wayCount + " ways");
		if (wayCount < 0) {
			wayCount+=256;
		}
//		logger.trace("reading " + wayCount + " ways");
		tt.ways = new Way[wayCount];
		for (int i=0; i< wayCount;i++){
			byte flags=ds.readByte();
			if (flags != 128){
//				showAlert("create Way " + i);
				Way w=new Way(ds,flags);
				tt.ways[i]=w;
			}
		}
		if (ds.readByte() != 0x56){
			throw new IOException("MapMid-file corrupt: Ways not OK");
		} else {
//			logger.info("ready");
		}
		ds.close();

		tt.dataReady();
		trace.newDataReady();
//		logger.info("DataReader ready "+ tt.fileId + tt.nodes.length + " Nodes " + tt.ways.length + " Ways" );

//		}

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
