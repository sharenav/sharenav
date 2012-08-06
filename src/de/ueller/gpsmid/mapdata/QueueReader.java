package de.ueller.gpsmid.mapdata;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net See Copying
 */

import java.io.IOException;
import java.util.Vector;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.graphics.ImageCollector;
import de.ueller.gpsmid.tile.SingleTile;
import de.ueller.gpsmid.tile.Tile;
import de.ueller.gpsmid.ui.GpsMid;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.util.Logger;

import de.enough.polish.util.Locale;

public abstract class QueueReader implements Runnable {

	protected static final Logger	logger				= Logger.getInstance(QueueReader.class, Logger.ERROR);
	protected final Vector			requestQueue		= new Vector();
	protected final Vector			notificationQueue	= new Vector();
	protected final Vector			livingQueue			= new Vector();
	private volatile boolean		shut				= false;
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
			if (tt.lastUse < 126) tt.lastUse++;
		}
		for (loop = 0; loop < requestQueue.size(); loop++) {
			tt = (Tile) requestQueue.elementAt(loop);
			if (tt.lastUse < 126) tt.lastUse++;
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
		cleanOldLivingTiles(0);
		// Should the request queue made empty here?
		for (loop = 0; loop < requestQueue.size(); loop++) {
			tt = (Tile) requestQueue.elementAt(loop);
			tt.cleanup(0);
		}
	}
	
	private void cleanupUnused() {
		int loop;
		Tile tt;
		
		if (GpsMid.getInstance().needsFreeingMemory()) {
			loop = cleanOldLivingTiles(3);
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
		} else {
			//#debug debug
			logger.debug("Not cleaning up caches, still enough memory left");
			// it make not really sense to keep all tiles in memory
			// if there is enough so remove very old tile in every case:
			// very old is in this case not used for the last 120 paints
			cleanOldLivingTiles(120);
		}		
	}

	/**
	 * Removes tile requests for SingleTiles which contain data that is not rendered by ImageCollector because zoomed out too far,
	 * also removes tile requests for SingleTiles that are offScreen for ImageCollector because zoomed in too far.
	 * 
	 * This allows zooming out, panning around and zooming in without having to wait ages for obsolete tile requests.
	 */
	private void cleanupUnnecessarySingleTileRequests() {
		Tile tt;
		SingleTile st;
		int droppedCountZoomedOut = 0;
		int droppedCountZoomedIn = 0;
		Trace trace = Trace.getInstance();
		for (int loop = 0; loop < requestQueue.size(); loop++) {
			tt = (Tile) requestQueue.elementAt(loop);
			if (tt instanceof SingleTile) {
				st = (SingleTile) tt;
				if ( (st.zl > ImageCollector.minTile || !trace.isTileRequiredByImageCollector(tt)) && tt.cleanup(1)) {
					if (st.zl > ImageCollector.minTile) {
						droppedCountZoomedOut++;
					} else {
						droppedCountZoomedIn++;						
					}
					synchronized (this) {
						notificationQueue.removeElementAt(loop);
						requestQueue.removeElementAt(loop--);
					}
				}
			}
		}
		if ((droppedCountZoomedOut > 0 || droppedCountZoomedIn > 0) && Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_TILE_REQUESTS_DROPPED)) {
			StringBuffer sb = new StringBuffer();
			if (droppedCountZoomedOut > 0) {
				sb.append(droppedCountZoomedOut + " zl>" + ImageCollector.minTile);
			}
			if (droppedCountZoomedIn > 0) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(droppedCountZoomedIn + " offScreen");
			}
			sb.append(" tile requests dropped");
			trace.receiveMessage(sb.toString());
		}
	}

	
	private int cleanOldLivingTiles(int age) {
		int loop;
		Tile tt;
		for (loop = 0; loop < livingQueue.size(); loop++) {
			tt = (Tile) livingQueue.elementAt(loop);
			if (tt.cleanup(age)) {
				// logger.info("cleanup living " + tt.fileId);
				synchronized (this) {
					livingQueue.removeElementAt(loop--);
				}
			}
		}
		return loop;
	}
	
	public void run() {
		Tile tt;
		// logger.info("DataReader Thread start ");
		try {
			while (!shut) {
				//#debug debug
				logger.debug("DataReader Thread looped ");
				try {
					// logger.info("loop: " + livingQueue.size() + " / " + requestQueue.size());
					// logger.info(toString());
					
					cleanupUnused();
					cleanupUnnecessarySingleTileRequests();
					
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
							//#debug info
							logger.info("Not much memory left, cleaning up and trying again");
							Trace.getInstance().cleanup();
							System.gc();
						}
					} catch (final OutOfMemoryError oome) {
						logger.error(Locale.get("queuereader.OOMReadingTiles")/*Out of memory reading tiles, trying to recover*/);
						Trace.getInstance().dropCache();
					} catch (final IOException e) {
						logger.exception(Locale.get("queuereader.FailedToReadTile")/*Failed to read tile*/, e);
						synchronized (this) {
							/* Start a fresh */
							requestQueue.removeAllElements();
							notificationQueue.removeAllElements();
						}
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
					logger.error(Locale.get("queuereader.OOMReadTilesNotRec")/*Out of memory while trying to read tiles. Not recovering*/);
				} catch (final RuntimeException e) {
					logger.exception(Locale.get("queuereader.ExceptionReadingTilesCont")/*Exception reading tiles, continuing never the less*/, e);
				}
			}
		} catch (final Exception e) {
			logger.fatal(Locale.get("queuereader.QueueReaderCrashed")/*QueueReader thread crashed unexpectedly with error */ + e.getMessage());
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
