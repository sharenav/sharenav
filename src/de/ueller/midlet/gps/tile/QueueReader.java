package de.ueller.midlet.gps.tile;

import java.io.IOException;
import java.util.Vector;

import de.ueller.midlet.gps.Logger;

public abstract class QueueReader implements Runnable{
//	protected static final Logger logger = Logger.getInstance(QueueReader.class,Logger.TRACE);
	protected final Vector requestQueue = new Vector();
	protected final Vector livingQueue = new Vector();
	private boolean shut = false;
	private Thread	processorThread;
	
	public QueueReader(){
		super();
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY+1);
		processorThread.start();

	}


	protected abstract void readData(Tile tt) throws IOException;

	public synchronized void shutdown() {
		shut=true;
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
//		logger.info("add " + st.fileId + " to queue size=" + requestQueue.size());
		notify();
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
						if (tt.cleanup(4)){
//							logger.info("cleanup living " + tt.fileId);
							livingQueue.removeElementAt(loop--);
						}
					}
					for (loop=0; loop < requestQueue.size(); loop++){
						tt=(Tile) requestQueue.elementAt(loop);
						if (tt.cleanup(4)){
//							logger.info("cleanup live " + tt.fileId);
							requestQueue.removeElementAt(loop--);
						}
					}
					try {
						if (requestQueue.size() > 0){
	//						logger.trace("try to read first queue element");
							tt=(Tile) requestQueue.firstElement();
	//						logger.trace("remove fom request queue " + tt);
							requestQueue.removeElementAt(0);
	//						logger.trace("read content " + tt);
							readData(tt);
	//						logger.trace("add to living queue " + tt);
							livingQueue.addElement(tt);
						}
					} catch (IOException e) {
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
