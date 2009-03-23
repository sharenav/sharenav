/*
 * GpsMid - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * See COPYING
 */
package de.ueller.gps;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.ueller.gps.data.Position;
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.gps.tools.intTree;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.LocationMsgReceiverList;
import de.ueller.midlet.gps.Logger;

/**
 * 
 * This location provider tries to use the Cell-ID of the currently
 * connected cell to retrieve a very rough extimate of position. This
 * estimate can be off by upto the range of kilometers. In order to
 * map the cell id to a location we use OpenCellID.org, that uses
 * crowd sourcing to determine the locations. As such, many cell-ids
 * may not yet be in their database.
 * 
 * This LocationProvider can only retrieve cell ids for Sony Ericson phones
 *
 */
public class SECellID implements LocationMsgProducer {

	public class CellIdLoc {
		public int cellID;
		public short mcc;
		public byte mnc;
		public short lac;
		public float lat;
		public float lon;
	}

	public class RetrivePosition extends TimerTask {
		private boolean retreaving;

		public void run() {
			int cellid;
			short mcc;
			byte mnc;
			short lac;
			try {
				//#debug debug
				logger.debug("Tring to retreave Cell-id");
				
				String cellidS = System
						.getProperty("com.sonyericsson.net.cellid");
				String mccS = System.getProperty("com.sonyericsson.net.cmcc");
				String mncS = System.getProperty("com.sonyericsson.net.cmnc");
				String lacS = System.getProperty("com.sonyericsson.net.lac");
				
				cellid = Integer.parseInt(cellidS, 16);
				mcc = (short) Integer.parseInt(mccS);
				mnc = (byte) Integer.parseInt(mncS);
				lac = (short) Integer.parseInt(lacS, 16);
				
				//#debug debug
				logger.debug("Got Cellid: " + cellid + "  mcc: " + mcc
						+ "  mnc: " + mnc + "  lac: " + lac);

				/**
				 * Check if we have the cell ID already cached
				 */
				CellIdLoc loc = (CellIdLoc) cellPos.get(cellid);
				if ((loc != null) && (loc.lac == lac) && (loc.mcc == mcc)
						&& (loc.mnc == mnc)) {
					
					float lat = loc.lat;
					float lon = loc.lon;
					if (rawDataLogger != null) {
						String logStr = "Cell-id: " + cellid + "  mcc: " + mcc + "  mnc: " + mnc
						+ "  lac: " + lac + " --> " + lat + " | " + lon;
						rawDataLogger.write(logStr.getBytes());
						rawDataLogger.flush();
					}
					if ((lat != 0.0) && (lon != 0.0)) {
						receiver.receiveSolution("Cell");
								receiver.receivePosItion(new Position(lat, lon, 0, 0, 0, 0,
										new Date()));
					} else {
						receiver.receiveSolution("NoFix");
					}
				} else {
					//#debug debug
					logger.debug("Cellid not cached, retrieving Cellid: "
							+ cellid + "  mcc: " + mcc + "  mnc: " + mnc
							+ "  lac: " + lac);
					
					if (retreaving) {
						logger.info("Still retreaving previous ID");
						return;
					}
					retreaving = true;
					
					/**
					 * Connect to the internet and retrieve location information
					 * for the current cell-id from OpenCellId.org
					 */
					try {
						String url = "http://www.opencellid.org/cell/get?mcc="
								+ mcc + "&mnc=" + mnc + "&cellid=" + cellid
								+ "&lac=" + lac + "&fmt=txt";
						logger.info("HTTP get " + url);
						HttpConnection connection = (HttpConnection) Connector
								.open(url);
						connection.setRequestMethod(HttpConnection.GET);
						connection.setRequestProperty("Content-Type",
								"//text plain");
						connection.setRequestProperty("Connection", "close");
						// HTTP Response
						if (connection.getResponseCode() == HttpConnection.HTTP_OK) {
							String str;
							InputStream inputstream = connection
									.openInputStream();
							int length = (int) connection.getLength();
							// #debug debug
							logger.debug("Retrieving String of length: "
									+ length);
							if (length != -1) {
								byte incomingData[] = new byte[length];
								int idx = 0;
								while (idx < length) {
									int readB = inputstream.read(incomingData,
											idx, length - idx);
									// #debug debug
									logger.debug("Read: " + readB + " bytes");
									idx += readB;
								}
								str = new String(incomingData);
							} else {
								ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
								int ch;
								while ((ch = inputstream.read()) != -1) {
									bytestream.write(ch);
								}
								bytestream.flush();
								str = new String(bytestream.toByteArray());
								bytestream.close();
							}
							// #debug info
							logger.info(str);
							
							if (str != null) {
								String[] pos = StringTokenizer.getArray(str,
										",");
								float lat = Float.parseFloat(pos[0]);
								float lon = Float.parseFloat(pos[1]);
								int accuracy = Integer.parseInt(pos[2]);
								loc = new CellIdLoc();
								loc.cellID = cellid;
								loc.mcc = mcc;	loc.mnc = mnc;	loc.lac = lac;
								loc.lat = lat;	loc.lon = lon;
								if (cellPos == null)
									logger.error("Cellpos == null");
								cellPos.put(cellid, loc);

								if (rawDataLogger != null) {
									String logStr = "Received new cell-pos: " + str;
									rawDataLogger.write(logStr.getBytes());
									logStr = "Cell-id: " + cellid + "  mcc: " + mcc + "  mnc: " + mnc
									+ "  lac: " + lac + " --> " + lat + " | " + lon;
									rawDataLogger.write(logStr.getBytes());
									rawDataLogger.write("\n".getBytes());
									rawDataLogger.flush();
								}

								if ((lat != 0.0) && (lon != 0.0)) {
									if (receiver == null) {
										logger.error("ReceiverList == null");
									}
									receiver.receiveSolution("Cell");
											receiver.receivePosItion(new Position(lat, lon, 0, 0, 0, 0,
													new Date()));
								} else {
									receiver.receiveSolution("NoFix");
								}
							}

						} else {
							logger.error("Request failed ("
									+ connection.getResponseCode() + "): "
									+ connection.getResponseMessage());
							receiver.receiveSolution("NoFix");
						}
					} catch (IOException ioe) {
						logger.error("Failed to retrieve CellID: "
								+ ioe.getMessage(), true);
					} catch (SecurityException se) {
						logger
								.error(
										"Failed to retrieve CellID. J2me dissallowed it",
										true);
					} finally {
						retreaving = false;
					}
				}
			} catch (SecurityException se) {
				logger.silentexception(
						"Do not have permission to retrieve cell-id", se);
				this.cancel();
				close("Cell-id: Not permitted");
			} catch (Exception e) {
				logger.silentexception("Could not retrieve cell-id", e);
				this.cancel();
				close("Cell-id retrieval failed");
			}
		}
	}

	private static final Logger logger = Logger.getInstance(SECellID.class,
			Logger.TRACE);

	protected OutputStream rawDataLogger;
	protected Thread processorThread;
	protected LocationMsgReceiverList receiver;
	protected boolean closed = false;
	private String message;
	private RetrivePosition rp;

	private intTree cellPos;

	public boolean init(LocationMsgReceiver receiver) {
		try {
			this.receiver = new LocationMsgReceiverList();
			this.receiver.addReceiver(receiver);
			cellPos = new intTree();
			String cellid = System.getProperty("com.sonyericsson.net.cellid");
			String mcc = System.getProperty("com.sonyericsson.net.cmcc");
			String mnc = System.getProperty("com.sonyericsson.net.cmnc");
			String lac = System.getProperty("com.sonyericsson.net.lac");
			if ((cellid == null) || (mcc == null) || (mnc == null)) {
				//return false;
			}
			Timer t = new Timer();
			rp = new RetrivePosition();
			t.schedule(rp, 100, 5000);
			return true;
		} catch (SecurityException se) {
			logger.silentexception(
					"Do not have permission to retrieve cell-id", se);
		} catch (Exception e) {
			logger.silentexception("Could not retrieve cell-id", e);
		}
		return false;
	}

	public void close() {
		logger.info("Location producer closing");
		closed = true;
		if (processorThread != null)
			processorThread.interrupt();
		receiver.locationDecoderEnd();
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

}
