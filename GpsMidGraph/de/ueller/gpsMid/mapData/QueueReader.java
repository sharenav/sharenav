package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

//import javax.microedition.io.Connector;
//import javax.microedition.io.file.FileConnection;

import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

public abstract class QueueReader implements Runnable{
	//#debug error
	protected static final Logger logger = Logger.getInstance(QueueReader.class,Logger.TRACE);
	protected final Vector requestQueue = new Vector();
	protected final Vector livingQueue = new Vector();
	private boolean shut = false;
	private Thread	processorThread;
	private static boolean fromJar=true;
	
	public QueueReader(){
		super();
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY+1);
		processorThread.start();

	}


	public abstract void readData(Tile tt) throws IOException;

	public synchronized void shutdown() {
		shut=true;
	}

	public static InputStream openFile(String name){
		if (fromJar){
			InputStream is = QueueReader.class.getResourceAsStream(name);
			return is;
		} else {
//			try {
//				FileConnection fc = (FileConnection)Connector.open("file:///e:" + name);
//				return(fc.openInputStream());
//			} catch (IOException e) {
				return null;
//			}
		}
	}
	public synchronized void incUnusedCounter() {
		Tile tt;
		int loop;
		for (loop=0; loop < livingQueue.size(); loop++){
			tt=(Tile) livingQueue.elementAt(loop);
			tt.lastUse++;
		}
		for (loop=0; loop < requestQueue.size(); loop++){
			tt=(Tile) requestQueue.elementAt(loop);
			tt.lastUse++;
		}
	
	}
	
	public synchronized void add(Tile st){
		st.lastUse=0;
		requestQueue.addElement(st);
		notify();
	}
	
	public void dropCache() {
		Tile tt;
		int loop;
		for (loop=0; loop < livingQueue.size(); loop++){
			tt=(Tile) livingQueue.elementAt(loop);
			tt.cleanup(0);
		}
		for (loop=0; loop < requestQueue.size(); loop++){
			tt=(Tile) requestQueue.elementAt(loop);
			tt.cleanup(0);
		}
	}


	public void run() {
			Tile tt;
			int loop;
	//		logger.info("DataReader Thread start ");
			while (! shut){
				try {
//					logger.info("loop: " + livingQueue.size() + " / " + requestQueue.size());
//					logger.info(toString());
					for (loop=0; loop < livingQueue.size(); loop++){
						tt=(Tile) livingQueue.elementAt(loop);
						if (tt.cleanup(3)){
//							logger.info("cleanup living " + tt.fileId);
							livingQueue.removeElementAt(loop--);
						}
					}
					for (loop=0; loop < requestQueue.size(); loop++){
						tt=(Tile) requestQueue.elementAt(loop);
						if (tt.cleanup(2)){
//							logger.info("cleanup live " + tt.fileId);
							requestQueue.removeElementAt(loop--);
						}
					}
					try {
						Runtime runtime = Runtime.getRuntime();
						if (runtime.freeMemory() > 25000){
							if (requestQueue.size() > 0){
								//#debug error
								logger.debug("requestQueue size="+requestQueue.size());
								tt=(Tile) requestQueue.firstElement();
								requestQueue.removeElementAt(0);
								readData(tt);
								livingQueue.addElement(tt);
							}
						} else {
							logger.info("Not much memory left, cleaning up an trying again");
							Trace.getInstance().cleanup();
							System.gc();
						}
					} catch (OutOfMemoryError oome) {
						logger.error("Out of memory reading tiles, trying to recover");
						Trace.getInstance().dropCache();
					}catch (IOException e) {
						tt=(Tile) requestQueue.firstElement();
//						logger.info(e.getMessage()+ "in read dict " + tt.fileId);
						requestQueue.removeElementAt(0);
						e.printStackTrace();
					}
					if (requestQueue.size() == 0){
						synchronized (this) {
							try {
								wait(10000);
							} catch (InterruptedException e) {
	
							}
						}
					}
				} catch (OutOfMemoryError oome) {
					logger.error("Out of memory while trying to read tiles. Not recovering");
				} catch (RuntimeException e) {
	//				logger.error(e.getMessage()+" continue thread");
					e.printStackTrace();
				}
			}
	//		logger.info("DataReader Thread end ");		
		}

	public int getLivingTilesCount() {
		return livingQueue.size();
	}

	public int getRequestQueueSize() {
		return requestQueue.size();
	}

}
