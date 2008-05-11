package de.ueller.gpsMid.mapData;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net See Copying
 */

import java.io.IOException;
import java.util.Vector;

import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

public abstract class QueueReader implements Runnable {

	protected static final Logger	logger				= Logger.getInstance(QueueReader.class, Logger.ERROR);
	protected final Vector			requestQueue		= new Vector();
	protected final Vector			notificationQueue	= new Vector();
	protected final Vector			livingQueue			= new Vector();
	private boolean					shut				= false;
	private final Thread			processorThread;

	public QueueReader(String name) {
		super();
		processorThread = new Thread(this,name);
		processorThread.setPriority(Thread.MIN_PRIORITY + 1);
		processorThread.start();

	}

	public abstract void readData(Tile tt, Object notifyReady) throws IOException;

	public synchronized void shutdown() {
		shut = true;
	}

	public synchronized void incUnusedCounter() {
		Tile tt;
		int loop;
		for (loop = 0; loop < livingQueue.size(); loop++) {
			tt = (Tile) livingQueue.elementAt(loop);
			tt.lastUse++;
		}
		for (loop = 0; loop < requestQueue.size(); loop++) {
			tt = (Tile) requestQueue.elementAt(loop);
			tt.lastUse++;
		}

	}

	public synchronized void add(Tile st, Object notifyReady) {
		st.lastUse = 0;
		requestQueue.addElement(st);
		notificationQueue.addElement(notifyReady);
		notify();
	}

	public synchronized void dropCache() {
		Tile tt;
		int loop;
		for (loop = 0; loop < livingQueue.size(); loop++) {
			tt = (Tile) livingQueue.elementAt(loop);
			tt.cleanup(0);
		}
		for (loop = 0; loop < requestQueue.size(); loop++) {
			tt = (Tile) requestQueue.elementAt(loop);
			tt.cleanup(0);
		}
	}

	public void run() {
		Tile tt;
		int loop;
		// logger.info("DataReader Thread start ");
		try {
			while (!shut) {
				try {
					// logger.info("loop: " + livingQueue.size() + " / " + requestQueue.size());
					// logger.info(toString());
					for (loop = 0; loop < livingQueue.size(); loop++) {
						tt = (Tile) livingQueue.elementAt(loop);
						if (tt.cleanup(3)) {
							// logger.info("cleanup living " + tt.fileId);
							synchronized (this) {
								livingQueue.removeElementAt(loop--);
							}
						}
					}
					for (loop = 0; loop < requestQueue.size(); loop++) {
						tt = (Tile) requestQueue.elementAt(loop);
						if (tt.cleanup(2)) {
							// logger.info("cleanup live " + tt.fileId);
							synchronized (this) {
								notificationQueue.removeElementAt(loop);
								requestQueue.removeElementAt(loop--);
							}
						}
					}
					try {
						final Runtime runtime = Runtime.getRuntime();
						if ((runtime.freeMemory() > 25000) || (runtime.totalMemory() < GpsMid.getInstance().getPhoneMaxMemory())) {
							if (requestQueue.size() > 0) {
								Object notifyReady;
								synchronized (this) {
									tt = (Tile) requestQueue.firstElement();
									requestQueue.removeElementAt(0);
									notifyReady = notificationQueue.firstElement();
									notificationQueue.removeElementAt(0);
								}
								readData(tt, notifyReady);
								synchronized (this) {
									livingQueue.addElement(tt);
								}
							}

						} else {
							logger.info("Not much memory left, cleaning up and trying again");
							Trace.getInstance().cleanup();
							System.gc();
						}
					} catch (final OutOfMemoryError oome) {
						logger.error("Out of memory reading tiles, trying to recover");
						Trace.getInstance().dropCache();
					} catch (final IOException e) {
						tt = (Tile) requestQueue.firstElement();
						// logger.info(e.getMessage()+ "in read dict " + tt.fileId);
						requestQueue.removeElementAt(0);
						notificationQueue.removeElementAt(0);
						e.printStackTrace();
					}

					synchronized (this) {
						if (requestQueue.size() == 0) {
							try {
								wait(10000);
							} catch (final InterruptedException e) {

							}
						}
					}
				} catch (final OutOfMemoryError oome) {
					logger.error("Out of memory while trying to read tiles. Not recovering");
				} catch (final RuntimeException e) {
					logger.exception("Excpetion in reading tiles, continueing never the less: ", e);
				}
			}
		} catch (final Exception e) {
			logger.fatal("QueueReader thread crashed unexpectadly with error " + e.getMessage());
		}
		// logger.info("DataReader Thread end ");
	}

	public int getLivingTilesCount() {
		return livingQueue.size();
	}

	public int getRequestQueueSize() {
		return requestQueue.size();
	}

}
