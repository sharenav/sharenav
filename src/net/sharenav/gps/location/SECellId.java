/*
 * ShareNav - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
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
package net.sharenav.gps.location;

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

//#if polish.api.online
import net.sharenav.util.HttpHelper;
//#endif
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.IntTree;
import net.sharenav.util.Logger;
import net.sharenav.util.StringTokenizer;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Position;
import net.sharenav.sharenav.ui.ShareNav;

import de.enough.polish.util.Locale;

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
public class SECellId implements LocationMsgProducer, UploadListener {
	
	private static final byte CELLDB_LACIDX = 1;
	private static final byte CELLDB_LACLIST = 2;
	private static final byte CELLDB_VERSION = 1;
	
	private static final String CELLDB_NAME = "ShareNav-CellIds";
	
	private CellIdProvider cellProvider;

	

	/**
	 * This object is an index entry to list in which
	 * RecordStore entry the information for a given
	 * (mcc,mnc,lac) area is in.
	 * 
	 *
	 */
	public final static class LacIdxEntry {
		public short mcc;
		public short mnc;
		public int lac;
		public int	 recordId;
		
		public LacIdxEntry() {
			// TODO Auto-generated constructor stub
		}
		
		public LacIdxEntry (DataInputStream dis) throws IOException {
			mcc = dis.readShort();
			mnc = dis.readShort();
			lac = dis.readInt();
			recordId = dis.readInt();
		}
		
		public void serialize(DataOutputStream dos) throws IOException {
			dos.writeShort(mcc);
			dos.writeShort(mnc);
			dos.writeInt(lac);
			dos.writeInt(recordId);
		}
		
		public int hashCode(short mcc, short mnc, int lac) {
			return lac + (mnc << 16) + (mcc << 23);
		}
		public int hashCode() {
			return hashCode(mcc,mnc,lac);
		}
		
		public String toString() {
			return Locale.get("secellid.LacIdxEntry")/* LacIdxEntry (mcc=*/ + mcc + ", mnc=" + mnc + ", lac=" + lac 
			+ " -> " + recordId + " |" + hashCode() + "|)";
		}
	}

	public void triggerLastKnownPositionUpdate() {
	}

	public void triggerPositionUpdate() {
		if (rp == null) {
			rp = new RetrievePosition();
		}
		rp.run();
	}

	/**
	 * Periodically retrieve the current Cell-id and
	 * convert cell id to a location and send it
	 * to the LocationReceiver
	 *
	 */
	public class RetrievePosition extends TimerTask {
		

		public void run() {
			GsmCell loc;
			GsmCell cellLoc = null;
			try {
				if (closed) {
					this.cancel();
					return;
				}

				cellLoc = cellProvider.obtainCurrentCellId();
				if ((cellLoc == null) || (cellLoc.mcc == 0)) {
					/**
					 * This can either be the case because
					 * we currently don't have cell coverage,
					 * or because the phone doesn't support this
					 * feature. Return a not-connected solution
					 * to indicate this
					 */
					//#debug debug
					logger.debug("No valid cell-id available");
					receiverList.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, 0);
					return;
				}

				/**
				 * Check if we have the cell ID already cached
				 */
				loc = retrieveFromCache(cellLoc);
				if (loc == null) {
					//#debug debug
					logger.debug(cellLoc + " was not in cache, retrieving from FS cache");
					if (Configuration.getCfgBitState(Configuration.CFGBIT_CELLID_ONLINEONLY)) {
					    loc = null;
					} else {
					    loc = retrieveFromFS(cellLoc);
					}
					if (loc == null) {
						//#debug debug
						logger.debug(cellLoc + " was not in FS cache, retrieving from persistent cache");

						if (Configuration.getCfgBitState(Configuration.CFGBIT_CELLID_ONLINEONLY)) {
						    loc = null;
						} else {
						    loc = retrieveFromPersistentCache(cellLoc);
						}
						if (loc == null) {
							//#if polish.api.online
							//#debug info
							logger.info(cellLoc + " was not in persistent cache, retrieving from OpenCellId.org");
							if (! Configuration.getCfgBitState(Configuration.CFGBIT_CELLID_OFFLINEONLY)) {
								loc = retrieveFromOpenCellId(cellLoc);
							}
							//#endif
							if (loc != null) {
								cellPos.put(loc.cellID, loc);
								if ((loc.lat != 0.0) || (loc.lon != 0.0)) {
									storeCellIDtoRecordStore(loc);
								} else {
									//#debug debug
									logger.debug("Not storing cell, as it has no valid coordinates");
								}
							} else {
								//#debug info
								logger.info("Cell is unknown, can't calculate a location based on it");
								receiverList.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, 0);
								return;
							}
						}
					} else {
						cellPos.put(loc.cellID, loc);
					}
				}
				if (rawDataLogger != null) {
					String logStr = "Cell-id: " + loc.cellID + "  mcc: " + loc.mcc + "  mnc: " + loc.mnc
					+ "  lac: " + loc.lac + " --> " + loc.lat + " | " + loc.lon;
					rawDataLogger.write(logStr.getBytes());
					rawDataLogger.flush();
				}
				if ((loc.lat != 0.0) && (loc.lon != 0.0)) {
					if (receiverList == null) {
						logger.error(Locale.get("secellid.ReceiverListNull")/*ReceiverList == null*/);
					}
					//#debug info
					logger.info("Obtained a position from " + loc);
					receiverList.receiveStatus(LocationMsgReceiver.STATUS_CELLID, 0);
					receiverList.receivePosition(new Position(loc.lat, loc.lon, 0, 0, 0, 0,
							  System.currentTimeMillis(), Position.TYPE_CELLID));
				} else {
					receiverList.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, 0);
				} 
			} catch (Exception e) {
				logger.silentexception("Could not retrieve cell-id", e);
				this.cancel();
				close(Locale.get("secellid.AlCellIDretfail")/*Cell-id retrieval failed*/);
			}
		}
	}

	private static final Logger logger = Logger.getInstance(SECellId.class,
			Logger.TRACE);

	protected OutputStream rawDataLogger;
	protected Thread processorThread;
	protected final LocationMsgReceiverList receiverList = new LocationMsgReceiverList();
	protected boolean closed = false;
	private String message;
	private RetrievePosition rp = null;

	private IntTree cellPos;
	private IntTree lacidx;
	
	private int dblacidx;
	
	private static boolean retrieving;
	private static boolean retrieved;
	
	
	
	public SECellId() {
	}

	public static void deleteCellIDRecordStore() {
		try {
			//#debug info
			logger.info("deleting cellID recordstore to clear cell cache");
			RecordStore.deleteRecordStore(CELLDB_NAME);
		} catch (Exception e) {
			logger.exception(Locale.get("secellid.ExCellIDCachClearFail")/*Failed to delete cell-id to clear persistent cache*/, e);
		}
	}

	public boolean init(LocationMsgReceiver receiver) {
		try {
			this.receiverList.addReceiver(receiver);
			cellPos = new IntTree();
			lacidx = new IntTree();
			
			cellProvider = CellIdProvider.getInstance();
			
			if (cellProvider.obtainCurrentCellId() == null) {
				//#debug info
				logger.error("No valid cell-id, closing down");
				this.receiverList.locationDecoderEnd(Locale.get("secellid.AlNoValidCellID")/*No valid cell-id*/);
				return false;
			}
			closed = false;
			
			//#debug info
			logger.info("Opening persistent Cell-id database");
			RecordStore db = RecordStore.openRecordStore(CELLDB_NAME, true);
			if (db.getNumRecords() > 0){
				/**
				 * Find the record store entry containing the index
				 * mapping (mcc, mnc, lac) to a recordstore entry with the
				 * list of corresponding cells
				 */
				try {
					boolean indexFound = false;
					RecordEnumeration re = db.enumerateRecords(null, null, false);
					while (!indexFound) {
						if (!re.hasNextElement()) {
							throw new IOException(Locale.get("secellid.ExCellDBIdxFail")/*Failed to find index for Cell-id database*/);
						}
						dblacidx = re.nextRecordId();
						byte [] buf = db.getRecord(dblacidx);
						DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
						if (dis.readByte() == CELLDB_LACIDX) {
							if (dis.readByte() != CELLDB_VERSION) {
								throw new IOException(Locale.get("secellid.ErCellDBVersionMismatch")/*Wrong version of CellDb, expected */ + CELLDB_VERSION);

							}

							int size = dis.readInt();
							//#debug info
							logger.info("Found valid lacidx with " + size + " entries");
							for (int i = 0; i < size; i++) {
								//#debug debug
								logger.debug("Reading lac entry " + i + " of " + size);
								LacIdxEntry idxEntry = new LacIdxEntry(dis);
								lacidx.put(idxEntry.hashCode(), idxEntry);
								//#debug debug
								logger.debug("Adding index entry for " + idxEntry);
							}
							if (dis.readInt() != 0xbeafdead) {
								throw new IOException(Locale.get("secellid.ErCellPersIdxFail")/*Persistent cell-id index is corrupt*/);
							}
							indexFound = true;
						} else {
							//ignore other types of record entries, as we are currently only interested
							//in the index entry
						}
					}
				} catch (IOException ioe) {
					logger.exception(Locale.get("secellid.ExCellcacheDropping")/*Could not read persistent cell-id cache. Dropping to recover*/, ioe);
					db.closeRecordStore();
					RecordStore.deleteRecordStore(CELLDB_NAME);
					db = RecordStore.openRecordStore(CELLDB_NAME, true);
				}
				
			}
			if (db.getNumRecords() == 0) {
				logger.info("Persistent Cell-id database is empty, initialising it");
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeByte(CELLDB_LACIDX);
				dos.writeByte(CELLDB_VERSION);
				dos.writeInt(0);
				dos.writeInt(0xbeafdead);
				dos.flush();
				dblacidx = db.addRecord(baos.toByteArray(), 0, baos.size());
			}
			db.closeRecordStore();
			
			return true;
		} catch (SecurityException se) {
			logger.silentexception(
					"Do not have permission to retrieve cell-id", se);
		} catch (Exception e) {
			logger.silentexception("Could not retrieve cell-id", e);
		}
		this.receiverList.locationDecoderEnd(Locale.get("secellid.AlCellIDLocFail")/*Cant use Cell-id for location*/);
		return false;
	}
	
	
	public boolean activate(LocationMsgReceiver receiver) {
		rp = new RetrievePosition();
		ShareNav.getTimer().schedule(rp, 1000, 5000);
		return true;
	}
	public boolean deactivate(LocationMsgReceiver receiver) {
		return true;
	}
	
	
	private GsmCell retrieveFromCache(GsmCell cell) {
		GsmCell loc = (GsmCell) cellPos.get(cell.cellID);
		if ((loc != null) && (loc.lac == cell.lac) && (loc.mcc == cell.mcc)
				&& (loc.mnc == cell.mnc)) {
			//#debug debug
			logger.debug("Found a valid cached cell: " + loc);
			return loc;
		} else {
			return null;
		}
	}

	//#if polish.api.online
	private synchronized GsmCell retrieveFromOpenCellId(GsmCell cellLoc) {
		
		GsmCell loc = null;
		if (retrieving) {
			logger.info("Still retrieving previous ID");
			return null;
		}
		retrieving = true;
		retrieved = false;
		
		/**
		 * Connect to the Internet and retrieve location information
		 * for the current cell-id from OpenCellId.org
		 */
		String url = "http://www.opencellid.org/cell/get?mcc="
			+ cellLoc.mcc + "&mnc=" + cellLoc.mnc + "&cellid=" + cellLoc.cellID
			+ "&lac=" + cellLoc.lac + "&fmt=txt";
		HttpHelper http = new HttpHelper();
		http.getURL(url, this);
		
		try {
			if (!retrieved) {
				wait();
			}
		} catch (InterruptedException ie) {
			retrieving = false;
			return loc;
		}
		
		String str = http.getData();
		if (str == null) {
			retrieving = false;
			return loc;
		}

		//#debug debug
		logger.debug("Cell-ID retrieval: " + str);
				
		
		String[] pos = StringTokenizer.getArray(str,",");
		float lat = Float.parseFloat(pos[0]);
		float lon = Float.parseFloat(pos[1]);
		int accuracy = Integer.parseInt(pos[2]);
		loc = new GsmCell();
		loc.cellID = cellLoc.cellID;
		loc.mcc = cellLoc.mcc;	loc.mnc = cellLoc.mnc;	loc.lac = cellLoc.lac;
		loc.lat = lat;	loc.lon = lon;
		if (cellPos == null) {
			logger.error(Locale.get("secellid.CellposNull")/*Cellpos == null*/);
			retrieving = false;
			return null;
		}

		retrieving = false;
		return loc;
	}
	//#endif

	private GsmCell retrieveFromFS(GsmCell cellLoc) {
		GsmCell ret;
		String filename = "/c" + cellLoc.mcc + cellLoc.mnc + cellLoc.lac + ".id";
		InputStream is ;
		try {
			// assuming here 0 for LAC is not valid; don't try to open the db for an invalid LAC
			if (cellLoc.lac == 0) {
				throw new IOException("LAC == 0");
			}
			is = Configuration.getMapResource(filename);
		} catch (IOException ioe) {
			//#debug debug
			logger.debug("Could not find Operator CellID file " + filename);
			try {
				/**
				 * In order to reduce the number of cells, we combine all the lacs that
				 * have less than 20 cells in them into a single file
				 */
				filename = "/c" + cellLoc.mcc + cellLoc.mnc +".id";
				is = Configuration.getMapResource(filename);
			} catch (IOException ioe2) {
				//#debug debug
				logger.debug("Could not find file " + filename + " either");
				return null;
			}
		}
		try {
			if (is != null) {
				DataInputStream dis = new DataInputStream(is);
				int noCellsRead = 0;
				while (dis.available() > 0) {
					noCellsRead++;
					int cellLAC = dis.readInt();
					int cellID = dis.readInt();
					float lat = dis.readFloat();
					float lon = dis.readFloat();
					if ((cellLoc.lac == 0 || cellLAC == cellLoc.lac) && (cellID == cellLoc.cellID)) {
						ret = new GsmCell();
						ret.mcc = cellLoc.mcc;
						ret.mnc = cellLoc.mnc;
						ret.lac = cellLoc.lac;
						ret.cellID = cellID;
						ret.lat = lat;
						ret.lon = lon;
						//#debug debug
						logger.debug("Found Cell in FS cache " + ret);
						return ret;
						
					}
				}
				//#debug debug
				logger.debug("Read " + noCellsRead + " Cells, but not the one we are looking for");
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}
	
	private GsmCell retrieveFromPersistentCache(GsmCell cell) {
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
				DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));
				if (dis.readByte() == CELLDB_LACLIST) {
					
					int size = dis.readInt();
					for (int i = 0; i < size; i++) {
						GsmCell tmpCell = new GsmCell(dis);
						//#debug debug
						logger.debug("Adding " + tmpCell + " to cache from persistent store " + idx);
						cellPos.put(tmpCell.cellID, tmpCell);
					}
					if (dis.readInt() != 0xdeadbeaf) {
						logger.error(Locale.get("secellid.ErCellcacheCorrupt2")/*Persistent Cell-id cache is corrupt*/);
					}
				} else {
					logger.error(Locale.get("secellid.ErCellcacheCorrupt2")/*Persistent Cell-id cache is corrupt*/);
				}
			}
			db.closeRecordStore();
			/**
			 * The entry should now be in the cache, so
			 * retrieve it and return it
			 */
			return retrieveFromCache(cell);
		} catch (Exception e) {
			logger.exception(Locale.get("secellid.FailedToLookFor")/*Failed to look for */ + cell + Locale.get("secellid.inPersistentCache")/* in persistent cache*/, e);
		}
		return null;
	}
	
	private void storeCellIDtoRecordStore(GsmCell cell) {
		try {
			//#debug info
			logger.info("Storing " + cell + " in persistent cell cache");
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
				dos.writeByte(CELLDB_LACLIST);
				dos.writeInt(1); //Size
				cell.serialise(dos);
				dos.writeInt(0xdeadbeaf);
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
				if (dis.readByte() == CELLDB_LACIDX) {
					if (dis.readByte() != CELLDB_VERSION) {
						logger.error(Locale.get("secellid.ErCellDBVersionMismatch")/*Wrong version of CellDb, expected */ + CELLDB_VERSION);
						db.closeRecordStore();
						return;
					}
					dos.writeByte(CELLDB_LACIDX);
					dos.writeByte(CELLDB_VERSION);
					int size = dis.readInt();
					dos.writeInt(size + 1);
					for (int i = 0; i < size; i++) {
						LacIdxEntry lie = new LacIdxEntry(dis);
						lie.serialize(dos);
					}
					if (dis.readInt() != 0xbeafdead) {
						logger.error(Locale.get("secellid.ErCellPersIdxFail")/*Persistent cell-id index is corrupt*/);
					}
					idx.serialize(dos);
					dos.writeInt(0xbeafdead);
					dos.flush();
					db.setRecord(dblacidx, baos.toByteArray(), 0, baos.size());

				} else {
					logger.error(Locale.get("secellid.ErrCellDBreadCorrupt")/*Corrupted read of Cell-id db*/);
				}
				db.closeRecordStore();
			} else {
				/**
				 * There is already a cell in this area, so add it to the
				 * correct entry.
				 */
				//#debug debug
				logger.debug("Adding " + cell + " to " + idx);
				byte [] buf = db.getRecord(idx.recordId);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				DataInputStream dis = new DataInputStream( new ByteArrayInputStream(buf));
				if (dis.readByte() == CELLDB_LACLIST) {
					dos.writeByte(CELLDB_LACLIST);
					int size = dis.readInt();
					dos.writeInt(size + 1);
					for (int i = 0; i < size; i++) {
						GsmCell tmpCell = new GsmCell(dis);
						tmpCell.serialise(dos);
					}
					if (dis.readInt() != 0xdeadbeaf) {
						logger.error(Locale.get("secellid.ErCellDBPersCorrupt1")/*Persistent Cellid-cache is corrupt*/);
					}
					cell.serialise(dos);
					dos.writeInt(0xdeadbeaf);
					dos.flush();
					db.setRecord(idx.recordId, baos.toByteArray(), 0, baos.size());
					//#debug debug
					logger.debug("Added Cell to area list");
				} else {
					logger.error(Locale.get("secellid.ErCellDBPersCorrupt1")/*Persistent Cellid-cache is corrupt*/);
				}
			}
		} catch (Exception e) {
			logger.exception(Locale.get("secellid.ExSaveCellPersFail")/*Failed to save cell-id to persistent cache*/, e);
		}

	}


	public void close() {
		logger.info("Location producer closing");
		closed = true;
		if (processorThread != null) {
			processorThread.interrupt();
		}
		receiverList.locationDecoderEnd();
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
				logger.exception(Locale.get("secellid.ExCloseGPSLogFail")/*Couldnt close raw GPS logger*/, e);
			}
			rawDataLogger = null;
		}
	}

	public void addLocationMsgReceiver(LocationMsgReceiver receiver) {
		receiverList.addReceiver(receiver);
	}

	public boolean removeLocationMsgReceiver(LocationMsgReceiver receiver) {
		return receiverList.removeReceiver(receiver);
	}

	public synchronized void completedUpload(boolean success, String message) {
		logger.info("Download of cellid completed!");
		retrieved = true;
		notifyAll();
	}

	public void setProgress(String message) {
		// TODO Auto-generated method stub
		
	}

	public void startProgress(String title) {
		// TODO Auto-generated method stub
		
	}

	public void updateProgress(String message) {
		// TODO Auto-generated method stub
		
	}

	public void updateProgressValue(int increment) {
		// TODO Auto-generated method stub
		
	}

	public void uploadAborted() {
		// TODO Auto-generated method stub
		
	}

}
