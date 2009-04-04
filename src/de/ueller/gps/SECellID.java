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
 * connected cell to retrieve a very rough estimate of position. This
 * estimate can be off by upto the range of kilometers. In order to
 * map the cell id to a location we use OpenCellID.org, that uses
 * crowd sourceing to determine the locations. As such, many cell-ids
 * may not yet be in their database.
 * 
 * This LocationProvider can only retrieve cell ids for Sony Ericsson phones
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
		
		public String toString() {
			String s = "Cell (id=" + cellID + " mcc=" + mcc + " mnc=" + mnc +
			" lac=" + lac + "  coord=" + lat + "|" + lon +")";
			return s;
		}
	}

	public class RetrivePosition extends TimerTask {
		private boolean retreaving;
		
		
		private CellIdLoc retreaveFromOpenCellId(CellIdLoc cellLoc) {
			CellIdLoc loc = null;
			if (retreaving) {
				logger.info("Still retreaving previous ID");
				return null;
			}
			retreaving = true;
			
			/**
			 * Connect to the internet and retrieve location information
			 * for the current cell-id from OpenCellId.org
			 */
			try {
				String url = "http://www.opencellid.org/cell/get?mcc="
						+ cellLoc.mcc + "&mnc=" + cellLoc.mnc + "&cellid=" + cellLoc.cellID
						+ "&lac=" + cellLoc.lac + "&fmt=txt";
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
					//#debug debug
					logger.debug("Retrieving String of length: "
							+ length);
					if (length != -1) {
						byte incomingData[] = new byte[length];
						int idx = 0;
						while (idx < length) {
							int readB = inputstream.read(incomingData,
									idx, length - idx);
							//#debug trace
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
					//#debug debug
					logger.debug("Cell-ID retreaval: " + str);
					
					if (str != null) {
						String[] pos = StringTokenizer.getArray(str,
								",");
						float lat = Float.parseFloat(pos[0]);
						float lon = Float.parseFloat(pos[1]);
						int accuracy = Integer.parseInt(pos[2]);
						loc = new CellIdLoc();
						loc.cellID = cellLoc.cellID;
						loc.mcc = cellLoc.mcc;	loc.mnc = cellLoc.mnc;	loc.lac = cellLoc.lac;
						loc.lat = lat;	loc.lon = lon;
						if (cellPos == null) {
							logger.error("Cellpos == null");
							retreaving = false;
							return null;
						}
						
					}

				} else {
					logger.error("Request failed ("
							+ connection.getResponseCode() + "): "
							+ connection.getResponseMessage());
					receiver.receiveSolution("NoFix");
				}
			} catch (SecurityException se) {
				logger.silentexception(
						"Do not have permission to retrieve cell-id", se);
				this.cancel();
				close("Cell-id: Not permitted");
			} catch (Exception e) {
				logger.exception("fdsafds",e);
			}
			retreaving = false;
			return loc;
		}

		public void run() {
			CellIdLoc cellLoc = null;
			try {
				if (closed) {
					this.cancel();
					return;
				}

				cellLoc = obtainCurrentCellId();
				if (cellLoc == null) {
					//#debug info
					logger.info("No valid cell-id available");
					receiver.receiveSolution("NoFix");
					return;
				}

				/**
				 * Check if we have the cell ID already cached
				 */
				CellIdLoc loc = (CellIdLoc) cellPos.get(cellLoc.cellID);
				logger.info("Got loc from intTree: " + loc);
				if ((loc != null) && (loc.lac == cellLoc.lac) && (loc.mcc == cellLoc.mcc)
						&& (loc.mnc == cellLoc.mnc)) {
					//#debug debug
					logger.debug("Found a valid cached cell: " + loc);
				} else {
					//#debug debug
					logger.debug("Cellid not cached, retrieving Cellid: "
							+ cellLoc);
					loc = retreaveFromOpenCellId(cellLoc);
					
					if (loc != null) {
						cellPos.put(loc.cellID, loc);
					} else {
						logger.error("Failed to get cell-id");
					}
				}
				if (rawDataLogger != null) {
					String logStr = "Cell-id: " + loc.cellID + "  mcc: " + loc.mcc + "  mnc: " + loc.mnc
					+ "  lac: " + loc.lac + " --> " + loc.lat + " | " + loc.lon;
					rawDataLogger.write(logStr.getBytes());
					rawDataLogger.flush();
				}
				if ((loc.lat != 0.0) && (loc.lon != 0.0)) {
					if (receiver == null) {
						logger.error("ReceiverList == null");
					}
					//#debug info
					logger.info("Obtained a position from " + loc);
					receiver.receiveSolution("Cell");
					receiver.receivePosItion(new Position(loc.lat, loc.lon, 0, 0, 0, 0,
							new Date()));
				} else {
					receiver.receiveSolution("NoFix");
				} 
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
			if (obtainCurrentCellId() == null) {
				//#debug info
				logger.info("No valid Cell-id, closing down");
				return false;
			}
			closed = false;
			Timer t = new Timer();
			rp = new RetrivePosition();
			t.schedule(rp, 1000, 5000);
			return true;
		} catch (SecurityException se) {
			logger.silentexception(
					"Do not have permission to retrieve cell-id", se);
		} catch (Exception e) {
			logger.silentexception("Could not retrieve cell-id", e);
		}
		return false;
	}
	
	private CellIdLoc obtainCurrentCellId() throws Exception {
		CellIdLoc cell = new CellIdLoc();
		//#debug debug
		logger.debug("Tring to retreave Cell-id");

		String cellidS = System
		.getProperty("com.sonyericsson.net.cellid");
		String mccS = System.getProperty("com.sonyericsson.net.cmcc");
		String mncS = System.getProperty("com.sonyericsson.net.cmnc");
		String lacS = System.getProperty("com.sonyericsson.net.lac");

		//cellidS = "2627"; mccS = "234"; mncS = "33"; lacS = "133";


		if ((cellidS == null) || (mccS == null) || (mncS == null) || (lacS == null)) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		try {
			cell.cellID = Integer.parseInt(cellidS, 16);
			cell.mcc = (short) Integer.parseInt(mccS);
			cell.mnc = (byte) Integer.parseInt(mncS);
			cell.lac = (short) Integer.parseInt(lacS, 16);
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS + " lac: " + lacS, nfe);
			return null;
		}

		//#debug debug
		logger.debug("Got Cellid: " + cell);
		return cell;
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
