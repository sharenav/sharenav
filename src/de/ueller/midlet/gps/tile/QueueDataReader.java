package de.ueller.midlet.gps.tile;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.Way;


public class QueueDataReader implements Runnable {
	private final Vector requestQueue=new Vector();
	private final Vector livingQueue=new Vector();
	private Thread	processorThread;
	private boolean shut=false;
//	private final static Logger logger=Logger.getInstance(QueueDataReader.class,Logger.INFO);

	public QueueDataReader(){
		super();
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY+1);
		processorThread.start();

	}
	public synchronized void add(SingleTile st){ 
		requestQueue.addElement(st);
//		logger.info("add " + st.fileId + " to queue size=" + requestQueue.size());
		notify();
	}
	public synchronized void shutdown() {
		shut=true;
	}
	
	public synchronized void incUnusedCounter(){
		SingleTile tt;
		int loop;
		for (loop=0; loop < livingQueue.size(); loop++){
			tt=(SingleTile) livingQueue.elementAt(loop);
			tt.lastUse++;
		}
	}
	public void run() {
		SingleTile tt;
		int loop;
//		logger.info("DataReader Thread start ");
		while (! shut){
			try {
//				logger.info("loop: " + livingQueue.size() + " / " + requestQueue.size());
				for (loop=0; loop < livingQueue.size(); loop++){
					tt=(SingleTile) livingQueue.elementAt(loop);
					if (tt.cleanup()){
//						logger.info("cleanup " + tt.fileId);
						livingQueue.removeElementAt(loop--);
					}
				}
				try {
					if (requestQueue.size() > 0){
//						logger.trace("try to read first queue element");
						tt=(SingleTile) requestQueue.firstElement();
//						logger.trace("remove fom request queue " + tt);
						requestQueue.removeElementAt(0);
//						logger.trace("read content " + tt);
						readData(tt);
//						logger.trace("add to living queue " + tt);
						livingQueue.addElement(tt);
					}
				} catch (IOException e) {
					tt=(SingleTile) requestQueue.firstElement();
//					logger.info(e.getMessage()+ "in read tile " + tt.fileId);
					e.printStackTrace();
				}
				if (requestQueue.size() == 0){
					synchronized (this) {
						try {
							wait(2000);
						} catch (InterruptedException e) {

						}
					}
				}
			} catch (RuntimeException e) {
//				logger.error(e.getMessage()+" continue thread");
				e.printStackTrace();
			}
		}
//		logger.info("DataReader Thread end ");		
	}

	private void readData(SingleTile tt) throws IOException{

//		logger.info("open " + tt.fileId);
		InputStream is = getClass().getResourceAsStream("/map/t"+tt.zl+tt.fileId+".d");
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
//		logger.info("DataReader ready "+ tt.fileId + tt.nodes.length + " Nodes " + tt.ways.length + " Ways" );

//		}

	}
	public int getLivingTilesCount()  {
		return livingQueue.size();
	}
	public int getRequestQueueSize() {
		return requestQueue.size();
	}
}
