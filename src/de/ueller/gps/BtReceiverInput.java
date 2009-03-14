package de.ueller.gps;

/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 * Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.nmea.NmeaInput;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.LocationMsgReceiverList;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

/**
 * 
 * This class shares the functionality to read from the Bluetooth GPS receiver
 * and handles common functionality such as dealing with receiver quality and
 * lost connections
 * 
 * The protocol specific decoding is handled in the abstract process() function
 * of subclasses
 * 
 */
public abstract class BtReceiverInput implements Runnable, LocationMsgProducer {
	private static final Logger logger = Logger.getInstance(NmeaInput.class,
			Logger.TRACE);

	protected InputStream btGpsInputStream;
	private OutputStream btGpsOutputStream;
	private StreamConnection conn;
	protected OutputStream rawDataLogger;
	protected Thread processorThread;
	protected LocationMsgReceiverList receiver;
	protected boolean closed = false;
	protected byte connectQuality = 100;
	protected int bytesReceived = 0;
	protected int[] connectError = new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	protected String message;
	protected int msgsReceived = 1;

	protected class KeepAliveTimer extends TimerTask {
		public void run() {
			if (btGpsOutputStream != null) {
				try {
					logger.debug("Writing bogus keep-alive");
					btGpsOutputStream.write(0);
				} catch (IOException e) {
					logger.info("Closing keep alive timer");
					this.cancel();
				} catch (IllegalArgumentException iae) {
					logger.silentexception("KeepAliveTimer went wrong", iae);
				}
			}
		}
	}

	public boolean init(LocationMsgReceiver receiver) {
		
		this.receiver = new LocationMsgReceiverList();
		this.receiver.addReceiver(receiver);
		
		//#debug info
		logger.info("Connect to "+Configuration.getBtUrl());
		if (! openBtConnection(Configuration.getBtUrl())){
			receiver.locationDecoderEnd();
			return false;
		}
		receiver.receiveMessage("BT Connected");
		
		processorThread = new Thread(this, "Bluetooth Receiver Decoder");
		processorThread.setPriority(Thread.MAX_PRIORITY);
		processorThread.start();
		/**
		 * There is at least one, perhaps more Bt GPS receivers, that seem to
		 * kill the Bluetooth connection if we don't send it something for some
		 * reason. Perhaps due to poor power management? We don't have anything
		 * to send, so send an arbitrary 0.
		 */
		if (Configuration.getBtKeepAlive()) {
			TimerTask tt = new KeepAliveTimer();
			logger.info("Setting keep alive timer: " + tt);
			Timer t = new Timer();
			t.schedule(tt, 1000, 1000);
		}
		return true;
	}

	abstract protected void process() throws IOException;

	public void run() {
		receiver.receiveMessage("Start Bt GPS receiver");
		// Eat the buffer content
		try {
			try {
				byte[] buf = new byte[512];
				while (btGpsInputStream.available() > 0) {
					int received = btGpsInputStream.read(buf);
					bytesReceived += received;
					if (rawDataLogger != null) {
						rawDataLogger.write(buf, 0, received);
						rawDataLogger.flush();
					}
				}
				// #debug debug
				logger.debug("Erased " + bytesReceived + " bytes");
				bytesReceived = 100;
			} catch (IOException e1) {
				receiver.receiveMessage("Closing: " + e1.getMessage());
				close("Closing: " + e1.getMessage());
			}

			byte timeCounter = 41;
			while (!closed) {
				//#debug debug
				logger.debug("Bt receiver thread looped");
				try {
					timeCounter++;
					if (timeCounter > 40) {
						timeCounter = 0;
						if (connectQuality > 100) {
							connectQuality = 100;
						}
						if (connectQuality < 0) {
							connectQuality = 0;
						}
						receiver
								.receiveStatistics(connectError, connectQuality);
						// watchdog if no bytes received in 10 sec then exit
						// thread
						if (bytesReceived == 0) {
							throw new IOException("No Data from GPS");
						} else {
							bytesReceived = 0;
						}
					}
					process();
				} catch (IOException e) {
					/**
					 * The bluetooth connection seems to have died, try and
					 * reconnect. If the reconnect was successful the reconnect
					 * function will call the init function, which will create a
					 * new thread. In that case we simply exit this old thread.
					 * If the reconnect was unsuccessful then we close the
					 * connection and give an error message.
					 */
					logger.info("Failed to read from GPS trying to reconnect: "
							+ e.getMessage());
					receiver.receiveSolution("~~");
					if (!autoReconnectBtConnection()) {
						logger.info("GPS bluethooth could not reconnect");
						receiver.receiveMessage("Closing: " + e.getMessage());
						close("Closed: " + e.getMessage());
					} else {
						logger.info("GPS bluetooth reconnect was successful");
						return;
					}
				}
				if (!closed)
					try {
						synchronized (this) {
							connectError[LocationMsgReceiver.SIRF_FAIL_MSG_INTERUPTED]++;
							wait(250);
						}
					} catch (InterruptedException e) {
						// Nothing to do in this case
					}

			}
			
		} catch (OutOfMemoryError oome) {
			logger.fatal("GpsInput thread crashed as out of memory: "
					+ oome.getMessage());
			oome.printStackTrace();
		} catch (Exception e) {
			logger.fatal("GpsInput thread crashed unexpectedly: "
					+ e.getMessage());
			e.printStackTrace();
		} finally {
			if (closed) {
				logger.info("Finished LocationProducer thread, closing bluetooth");
				closeBtConnection();
				if (message == null) {
					receiver.locationDecoderEnd();
				} else {
					receiver.locationDecoderEnd(message);
				}
			} else {
				/**
				 * Don't need to do anything here.
				 * This is the case when we are auto-reconnecting
				 * and starting up a new bluetooth processing thread
				 */
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.ueller.gps.nmea.LocationMsgProducer#close()
	 */
	public void close() {
		logger.info("Location producer closing");
		closed = true;
		if (processorThread != null)
			processorThread.interrupt();
	}

	public void close(String message) {
		this.message = message;
		close();
	}

	public void enableRawLogging(OutputStream os) {
		rawDataLogger = os;
	}

	public void disableRawLogging() {
		if (rawDataLogger != null) {
			try {
				rawDataLogger.close();
			} catch (IOException e) {
				logger.exception("Couldn't close raw GPS logger", e);
			}
			rawDataLogger = null;
		}
	}

	public void addLocationMsgReceiver(LocationMsgReceiver rec) {
		receiver.addReceiver(rec);
	}
	
	public boolean removeLocationMsgReceiver(LocationMsgReceiver rec) {
		return receiver.removeReceiver(rec);
	}

	private synchronized boolean openBtConnection(String url){
		if (btGpsInputStream != null){
			return true;
		}
		if (url == null)
			return false;
		try {
			logger.info("Connector.open()");
			conn = (StreamConnection) Connector.open(url);
			logger.info("conn.openInputStream()");
			btGpsInputStream = conn.openInputStream();
			/**
			 * There is at least one, perhaps more BT gps receivers, that
			 * seem to kill the bluetooth connection if we don't send it
			 * something for some reason. Perhaps due to poor powermanagment?
			 * We don't have anything to send, so send an arbitrary 0.
			 */
			if (Configuration.getBtKeepAlive()) {
				btGpsOutputStream = conn.openOutputStream();
			}
			
		} catch (SecurityException se) {
			/**
			 * The application was not permitted to connect to bluetooth  
			 */
			receiver.receiveMessage("Connectiong to BT not permitted");
			return false;
			
		} catch (IOException e) {
			receiver.receiveMessage("err BT:"+e.getMessage());
			return false;
		}
		return true;
	}
	
	private synchronized void closeBtConnection() {
		disableRawLogging();
		if (btGpsInputStream != null){
			try {
				btGpsInputStream.close();
			} catch (IOException e) {
			}
			btGpsInputStream=null;
		}
		if (btGpsOutputStream != null){
			try {
				btGpsOutputStream.close();
			} catch (IOException e) {
			}
			btGpsOutputStream=null;
		}
		if (conn != null){
			try {
				conn.close();
			} catch (IOException e) {
			}
			conn=null;
		}		
	}
	
	/**
	 * This function tries to reconnect to the bluetooth
	 * it retries for up to 40 seconds and blocks in the
	 * mean time, so this function has to be called from
	 * within a separate thread. If successful, it will
	 * reinitialise the location producer with the new
	 * streams.
	 * 
	 * @return whether the reconnect was successful
	 */
	private boolean autoReconnectBtConnection() {
		if (!Configuration.getBtAutoRecon()) {
			logger.info("Not trying to reconnect");
			return false;
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_DISCONNECT)) {
			GpsMid.mNoiseMaker.playSound("DISCONNECT");
		}
		/**
		 * If there are still parts of the old connection
		 * left over, close these cleanly.
		 */
		closeBtConnection();
		int reconnectFailures = 0;
		logger.info("Trying to reconnect to bluetooth");
		while (!closed && (reconnectFailures < 4) && (! openBtConnection(Configuration.getBtUrl()))){
			reconnectFailures++;
			logger.info("Failed to reconnect for the " + reconnectFailures + " time");
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				logger.silentexception("INTERRUPTED!", e);
				return false;
			}
		}
		if (reconnectFailures < 4 && !closed) {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT)) {
				GpsMid.mNoiseMaker.playSound("CONNECT");
			}
			init(receiver);
			return true;
		}
		if (!closed)
			logger.error("Lost connection to GPS and failed to reconnect");
		return false;
	}

}
