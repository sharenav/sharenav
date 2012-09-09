/**
 * This file is part of ShareNav.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 * Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.gps.location;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

/**
 * This class shares the functionality to read from the Bluetooth GPS receiver
 * and handles common functionality such as dealing with receiver quality and
 * lost connections.
 * 
 * The protocol specific decoding is handled in the abstract process() function
 * of subclasses.
 */
public abstract class BtReceiverInput implements Runnable, LocationMsgProducer {
	private static final Logger logger = Logger.getInstance(NmeaInput.class,
			Logger.TRACE);

	protected InputStream btGpsInputStream;
	private OutputStream btGpsOutputStream;
	private StreamConnection conn;
	protected OutputStream rawDataLogger;
	protected Thread processorThread;
	protected LocationMsgReceiverList receiverList;
	protected boolean closed = false;
	protected byte connectQuality = 100;
	protected int bytesReceived = 0;
	protected int[] connectError = new int[LocationMsgReceiver.SIRF_FAIL_COUNT];
	protected String message;
	protected int msgsReceived = 1;
	
	public BtReceiverInput() {
		this.receiverList = new LocationMsgReceiverList();
	}

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
		if (receiver != null) {
			this.receiverList.addReceiver(receiver);
		}
		
		//#debug info
		logger.info("Connect to "+Configuration.getBtUrl());
		if (! openBtConnection(Configuration.getBtUrl())){
			this.receiverList.locationDecoderEnd();
			return false;
		}
		this.receiverList.receiveMessage(Locale.get("btreceiverinput.BTconnected")/*BT Connected*/);
		
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
			ShareNav.getTimer().schedule(tt, 1000, 1000);
		}
		return true;
	}

	public boolean activate(LocationMsgReceiver receiver) {
		// FIXME move activation code (code to enable continuos location feed) here
		return true;
	}
	public boolean deactivate(LocationMsgReceiver receiver) {
		return true;
	}
	abstract protected void process() throws IOException;

	public void run() {
		receiverList.receiveMessage(Locale.get("btreceiverinput.StartBTreceiver")/*Start Bt GPS receiver*/);
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
				receiverList.receiveMessage(Locale.get("btreceiverinput.BTClosing2")/*Closing: */ + e1.getMessage());
				close(Locale.get("btreceiverinput.BTClosing2")/*Closing: */ + e1.getMessage());
			}

			byte timeCounter = 21;
			while (!closed) {
				//#debug debug
				logger.debug("Bt receiver thread looped");
				try {
					timeCounter++;
					// 20 * 250 ms = 5 s
					if (timeCounter > 20) {
						timeCounter = 0;
						if (connectQuality > 100) {
							connectQuality = 100;
						}
						if (connectQuality < 0) {
							connectQuality = 0;
						}
						receiverList.receiveStatistics(connectError, connectQuality);
						// Watchdog: if no bytes received in 10 sec then exit thread
						if (bytesReceived == 0) {
							throw new IOException(Locale.get("btreceiverinput.BTNoData")/*No Data from GPS*/);
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
					receiverList.receiveStatus(LocationMsgReceiver.STATUS_RECONNECT, 0);
					if (!autoReconnectBtConnection()) {
						logger.info("GPS bluethooth could not reconnect");
						receiverList.receiveMessage(Locale.get("btreceiverinput.BTClosing2")/*Closing: */ + e.getMessage());
						close(Locale.get("btreceiverinput.BTAutoClose")/*Closed: */ + e.getMessage());
					} else {
						logger.info("GPS bluetooth reconnect was successful");
						return;
					}
				}
				if (!closed) {
					try {
						synchronized (this) {
							connectError[LocationMsgReceiver.SIRF_FAIL_MSG_INTERUPTED]++;
							wait(250);
						}
					} catch (InterruptedException e) {
						// Nothing to do in this case
					}
				}

			}
			
		} catch (OutOfMemoryError oome) {
			closed = true;
			logger.fatal(Locale.get("btreceiverinput.ExOOM")/*BtReceiverInput thread ran out of memory: */
					+ oome.getMessage());
			oome.printStackTrace();
		} catch (Exception e) {
			closed = true;
			logger.fatal(Locale.get("btreceiverinput.ExCrashUnexp")/*BtReceiverInput thread crashed unexpectedly: */
					+ e.getMessage());
			e.printStackTrace();
		} finally {
			if (closed) {
				logger.info("Finished LocationProducer thread, closing bluetooth");
				closeBtConnection();
				if (message == null) {
					receiverList.locationDecoderEnd();
				} else {
					receiverList.locationDecoderEnd(message);
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
	 * @see net.sharenav.gps.nmea.LocationMsgProducer#close()
	 */
	public void close() {
		logger.info("Location producer closing");
		closed = true;
		if (processorThread != null) {
			processorThread.interrupt();
		}
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
				logger.exception(Locale.get("btreceiverinput.ExCloserawGPS")/*Could not close raw GPS logger*/, e);
			}
			rawDataLogger = null;
		}
	}

	public void triggerLastKnownPositionUpdate() {
	}

	public void triggerPositionUpdate() {
		//FIXME make a proper interface for passing fix age information instead of accessing trace variable directly
		//tr.gpsRecenterInvalid = true;
		//tr.gpsRecenterStale = true;
		//locationUpdated(locationProvider, LocationProvider.getLastKnownLocation());
	}
	public void addLocationMsgReceiver(LocationMsgReceiver receiver) {
		receiverList.addReceiver(receiver);
	}
	
	public boolean removeLocationMsgReceiver(LocationMsgReceiver receiver) {
		return receiverList.removeReceiver(receiver);
	}

	private synchronized boolean openBtConnection(String url){
		if (btGpsInputStream != null){
			return true;
		}
		if (url == null) {
			return false;
		}
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
			receiverList.receiveMessage(Locale.get("btreceiverinput.AlBTConnectNotPermit")/*Connecting to BT not permitted*/);
			return false;
			
		} catch (IOException e) {
			receiverList.receiveMessage(Locale.get("btreceiverinput.AlErr")/*BT error:*/ + e.getMessage());
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
			ShareNav.mNoiseMaker.playSound("DISCONNECT");
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
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				logger.silentexception("INTERRUPTED!", e);
				return false;
			}
		}
		if (reconnectFailures < 8 && !closed) {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT)) {
				ShareNav.mNoiseMaker.playSound("CONNECT");
			}
			init(null);
			return true;
		}
		if (!closed) {
			logger.error(Locale.get("btreceiverinput.ErLostConnection")/*Lost connection to GPS and failed to reconnect*/);
		}
		return false;
	}

}
