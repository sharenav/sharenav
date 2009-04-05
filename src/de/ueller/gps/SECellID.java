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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

import de.ueller.gps.data.Position;
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.gps.tools.intTree;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.LocationMsgReceiverList;
import de.ueller.midlet.gps.Logger;

/**
 * 
 * This location provider tries to use the cell-id of the currently
 * connected cell to retrieve a very rough estimate of position. This
 * estimate can be off by up to the range of kilometers. In order to
 * map the cell-id to a location we use OpenCellID.org, that uses
 * crowd sourcing to determine the locations. As such, many cell-ids
 * may not yet be in their database.
 * 
 * This LocationProvider can only retrieve cell-ids for Sony Ericsson phones
 *
 */
public class SECellID implements LocationMsgProducer {
	
	private static final byte CELLDB_LACIDX = 1;
	private static final byte CELLDB_LACLIST = 2;
	private static final byte CELLDB_VERSION = 1;
	
	private static final String CELLDB_NAME = "GpsMid-CellIds";

	/**
	 * 
	 * This object contains the location and information of a single cell tower
	 *
	 */
	public class CellIdLoc {
		public int cellID;
		public short mcc;
		public short mnc;
		public short lac;
		public float lat;
		public float lon;
		
		public CellIdLoc () {
			/**
			 * Default constructor;
			 */
		}
		public CellIdLoc (DataInputStream dis) throws IOException {
			mcc = (short)dis.readShort();
			mnc = (short)dis.readShort();
			lac = (short)dis.readInt();
			cellID = dis.readInt();
			lat = dis.readFloat();
			lon = dis.readFloat();
		}
		
		public String toString() {
			String s = "Cell (id=" + cellID + " mcc=" + mcc + " mnc=" + mnc +
			" lac=" + lac + "  coord=" + lat + "|" + lon +")";
			return s;
		}
		
		public void serialise(DataOutputStream dos) throws IOException{
			dos.writeShort(mcc);
			dos.writeShort(mnc);
			dos.writeInt(lac);
			dos.writeInt(cellID);
			dos.writeFloat(lat);
			dos.writeFloat(lon);
		}
	}

	/**
	 * This object is an index entry to list in which
	 * RecordStore entry the information for a given
	 * (mcc,mnc,lac) area is in.
	 * 
	 *
	 */
	public class LacIdxEntry {
		public short mcc;
		public short mnc;
		public short lac;
		public int	 recordId;
		
		public LacIdxEntry() {
			// TODO Auto-generated constructor stub
		}
		
		public LacIdxEntry (DataInputStream dis) throws IOException {
			mcc = (short)dis.readShort();
			mnc = (short)dis.readShort();
			lac = (short)dis.readInt();
			recordId = dis.readInt();
		}
		
		public void serialize(DataOutputStream dos) throws IOException {
			dos.writeShort(mcc);
			dos.writeShort(mnc);
			dos.writeInt(lac);
			dos.writeInt(recordId);
		}
		
		public int hashCode(short mcc, short mnc, short lac) {
			return lac + mnc << 16 + mcc << 24;
		}
		public int hashCode() {
			return hashCode(mcc,mnc,lac);
		}
	}

	/**
	 * Periodically retrieve the current Cell-id and
	 * convert cell id to a location and send it
	 * to the LocationReceiver
	 *
	 */
	public class RetrievePosition extends TimerTask {
		

		public void run() {
			CellIdLoc cellLoc = null;
			try {
				if (closed) {
					this.cancel();
					return;
				}

				cellLoc = obtainCurrentCellId();
				if (cellLoc == null) {
					/**
					 * This can either be the case because
					 * we currently don't have cell coverage,
					 * or because the phone doesn't support this
					 * feature. Return a not-connected solution
					 * to indicate this
					 */
					//#debug debug
					logger.debug("No valid cell-id available");
					receiver.receiveSolution("~~");
					return;
				}

				/**
				 * Check if we have the cell ID already cached
				 */
				CellIdLoc loc = retrieveFromCache(cellLoc);
				if (loc == null) {
					//#debug debug
					logger.debug(cellLoc + " was not in cache, retrieving from persistent cache");
					loc = retrieveFromPersistentCache(cellLoc);
					if (loc == null) {
						//#debug info
						logger.info(cellLoc + " was not in persistent cache, retrieving from OpenCellId.org");
						loc = retrieveFromOpenCellId(cellLoc);
						if (loc != null) {
							cellPos.put(loc.cellID, loc);
							if ((loc.lat != 0.0) || (loc.lon != 0.0)) {
								storeCellIDtoRecordStore(loc);
							}
						} else {
							logger.error("Failed to get cell-id");
							return;
						}
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
					receiver.receivePosition(new Position(loc.lat, loc.lon, 0, 0, 0, 0,
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
	private RetrievePosition rp;

	private intTree cellPos;
	private intTree lacidx;
	
	private int dblacidx;
	
	private static boolean retrieving;
	
	public SECellID() {
		this.receiver = new LocationMsgReceiverList();
	}

	public boolean init(LocationMsgReceiver receiver) {
		try {
			this.receiver.addReceiver(receiver);
			cellPos = new intTree();
			lacidx = new intTree();
			
			if (obtainCurrentCellId() == null) {
				//#debug info
				logger.info("No valid cell-id, closing down");
				return false;
			}
			closed = false;
			
			//#debug info
			logger.info("Opening persistent Cell-id database");
			RecordStore db = RecordStore.openRecordStore(CELLDB_NAME, true);
			if (db.getNumRecords() == 0) {
				logger.info("Persisten Cell-id database is empty, initialising it");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeByte(CELLDB_LACIDX);
				dos.writeByte(CELLDB_VERSION);
				dos.writeInt(0);
				dos.flush();
				dblacidx = db.addRecord(baos.toByteArray(), 0, baos.size());
			} else {
				/**
				 * Find the record store entry containing the index
				 * mapping (mcc, mnc, lac) to a recordstore entry with the
				 * list of corresponding cells
				 */
				boolean indexFound = false;
				RecordEnumeration re = db.enumerateRecords(null, null, false);
				while (!indexFound) {
					if (!re.hasNextElement()) {
						logger.error("Failed to find index for Cell-id database");
						dblacidx = -1;
					}
					dblacidx = re.nextRecordId();
					byte [] buf = db.getRecord(dblacidx);
					DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
					if (dis.readByte() == CELLDB_LACIDX) {
						if (dis.readByte() != CELLDB_VERSION) {
							logger.error("Wrong version of CellDb, expected " + CELLDB_VERSION);
							db.closeRecordStore();
							return false;
						}
						
						int size = dis.readInt();
						//#debug info
						logger.info("Found valid lacidx with " + size + " entries");
						for (int i = 0; i < size; i++) {
							LacIdxEntry idxEntry = new LacIdxEntry(dis);
							lacidx.put(idxEntry.hashCode(), idxEntry);
							//#debug debug
							logger.debug("Adding index entry for " + idxEntry);
						}
						indexFound = true;
					}
				}
				
			}
			db.closeRecordStore();
			
			Timer t = new Timer();
			rp = new RetrievePosition();
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
		logger.debug("Tring to retrieve cell-id");

		String cellidS = System
		.getProperty("com.sonyericsson.net.cellid");
		String mccS = System.getProperty("com.sonyericsson.net.cmcc");
		String mncS = System.getProperty("com.sonyericsson.net.cmnc");
		String lacS = System.getProperty("com.sonyericsson.net.lac");

		/*
		 * This code is used for debugging cell-id data on the emulator
		 * by generating one of 7 random cell-ids 
		 *
		Random r = new Random();
		int rr = r.nextInt(4) + 1;
		System.out.println("RR: " +rr);
		switch (rr) {
		case 1:
			cellidS = "2627"; mccS = "234"; mncS = "33"; lacS = "133";
			break;
		case 2:
			cellidS = "2628"; mccS = "234"; mncS = "33"; lacS = "133";
			break;
		case 3:
			cellidS = "2629"; mccS = "234"; mncS = "33"; lacS = "133";
			break;
		case 4:
			cellidS = "2620"; mccS = "234"; mncS = "33"; lacS = "134";
			break;
		case 5:
			cellidS = "2619"; mccS = "234"; mncS = "33"; lacS = "134";
			break;
		case 6:
			cellidS = "2629"; mccS = "234"; mncS = "33"; lacS = "135";
			break;
		}
		*/
		if ((cellidS == null) || (mccS == null) || (mncS == null) || (lacS == null)) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		try {
			cell.cellID = Integer.parseInt(cellidS, 16);
			cell.mcc = (short) Integer.parseInt(mccS);
			cell.mnc = (short) Integer.parseInt(mncS);
			cell.lac = (short) Integer.parseInt(lacS, 16);
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS + " lac: " + lacS, nfe);
			return null;
		}
		//#debug debug
		logger.debug("Got cell-id: " + cell);
		return cell;
	}
	
	private CellIdLoc retrieveFromCache(CellIdLoc cell) {
		CellIdLoc loc = (CellIdLoc) cellPos.get(cell.cellID);
		if ((loc != null) && (loc.lac == cell.lac) && (loc.mcc == cell.mcc)
				&& (loc.mnc == cell.mnc)) {
			//#debug debug
			logger.debug("Found a valid cached cell: " + loc);
			return loc;
		} else {
			return null;
		}
	}

	
	private CellIdLoc retrieveFromOpenCellId(CellIdLoc cellLoc) {
		
		CellIdLoc loc = null;
		if (retrieving) {
			logger.info("Still retrieving previous ID");
			return null;
		}
		retrieving = true;
		
		/**
		 * Connect to the Internet and retrieve location information
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
				InputStream inputstream = connection.openInputStream();
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
				logger.debug("Cell-ID retrieval: " + str);
				
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
						retrieving = false;
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
			rp.cancel();
			close("Cell-id: Not permitted");
		} catch (Exception e) {
			rp.cancel();
			logger.silentexception("Something went wrong while contacting Opencellid.org",e);
			close("No connection to opencellid.org");
		}
		retrieving = false;
		return loc;
	}

	
	private CellIdLoc retrieveFromPersistentCache(CellIdLoc cell) {
		//#debug info
		logger.info("Looking for " + cell + " in persistent cache");
		try {
			RecordStore db = RecordStore.openRecordStore(CELLDB_NAME, false);
			LacIdxEntry idx = new LacIdxEntry();
			idx = (LacIdxEntry) lacidx.get(idx.hashCode(cell.mcc, cell.mnc, cell.lac));
			if (idx == null) {
				return null;
			} else {
				/**
				 * Load the entries for the current area from the
				 * record store db into the cache;
				 */
				byte [] buf = db.getRecord(idx.recordId);
				ByteArrayInputStream bais = new ByteArrayInputStream(buf);
				if (bais.read() == CELLDB_LACLIST) {
					
					int size = bais.read();
					for (int i = 0; i < size; i++) {
						CellIdLoc tmpCell = new CellIdLoc(new DataInputStream(bais));
						//#debug debug
						logger.debug("Adding " + tmpCell + " from persistent store to cache");
						cellPos.put(tmpCell.cellID, tmpCell);
					}
				} else {
					logger.error("Persisten Cell-id cache is corrupt");
				}
			}
			db.closeRecordStore();
			/**
			 * The entry should now be in the cache, so
			 * retrieve it and return it
			 */
			return retrieveFromCache(cell);
		} catch (Exception e) {
			logger.exception("Failed to look for " + cell + " in persitent cache", e);
		}
		return null;
	}
	
	private void storeCellIDtoRecordStore(CellIdLoc cell) {
		try {
			//#debug info
			logger.info("Storing " + cell + " in persitent cell cache");
			RecordStore db = RecordStore.openRecordStore(CELLDB_NAME, false);
			LacIdxEntry idx = new LacIdxEntry();
			idx = (LacIdxEntry) lacidx.get(idx.hashCode(cell.mcc, cell.mnc, cell.lac));

			if (idx == null) {
				//#debug debug
				logger.debug("First cell in this area");
				idx = new LacIdxEntry();
				idx.lac = cell.lac;
				idx.mcc = cell.mcc;
				idx.mnc = cell.mnc;

				/**
				 * Writing the cell to its area entry
				 */
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.write(CELLDB_LACLIST);
				dos.write(1); //Size
				cell.serialise(dos);
				dos.flush();
				idx.recordId = db.addRecord(baos.toByteArray(), 0, baos.size());
				dos.close();
				dos = null;

				lacidx.put(idx.hashCode(), idx);
				
				/**
				 * Adding area to the area index
				 */
				byte [] buf = db.getRecord(dblacidx);
				DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
				baos = new ByteArrayOutputStream();
				dos = new DataOutputStream(baos);
				if (dis.read() == CELLDB_LACIDX) {
					if (dis.read() != CELLDB_VERSION) {
						logger.error("Wrong version of CellDb, expected " + CELLDB_VERSION);
						db.closeRecordStore();
						return;
					}
					dos.write(CELLDB_LACIDX);
					dos.write(CELLDB_VERSION);
					int size = dis.read();
					dos.write(size + 1);
					for (int i = 0; i < size; i++) {
						LacIdxEntry lie = new LacIdxEntry(dis);
						lie.serialize(dos);
					}
					idx.serialize(dos);
					db.setRecord(dblacidx, baos.toByteArray(), 0, baos.size());

				}
				db.closeRecordStore();
			} else {
				/**
				 * There is already a cell in this area, so add it to the
				 * correct entry.
				 */
				byte [] buf = db.getRecord(idx.recordId);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				DataInputStream dis = new DataInputStream( new ByteArrayInputStream(buf));
				if (dis.read() == CELLDB_LACLIST) {
					baos.write(CELLDB_LACLIST);
					int size = dis.read();
					baos.write(size + 1);
					for (int i = 0; i < size; i++) {
						CellIdLoc tmpCell = new CellIdLoc(dis);
						tmpCell.serialise(dos);
					}
					cell.serialise(dos);
					db.setRecord(idx.recordId, baos.toByteArray(), 0, baos.size());
					//#debug debug
					logger.debug("Added Cell to area list");
				} else {
					logger.error("Persistent Cellid-cache is corrupt");
				}
			}
		} catch (Exception e) {
			logger.exception("Failed to save cell-id to persistent cache", e);
		}

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
