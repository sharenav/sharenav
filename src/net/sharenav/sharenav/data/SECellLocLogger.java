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
 * See file COPYING.
 */

package net.sharenav.sharenav.data;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

//#if polish.api.fileconnection
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif

import javax.microedition.lcdui.Alert;

import net.sharenav.gps.Satellite;
import net.sharenav.gps.location.CellIdProvider;
import net.sharenav.gps.location.GsmCell;
import net.sharenav.gps.location.LocationMsgReceiver;
import net.sharenav.gps.location.LocationMsgReceiverList;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.util.HelperRoutines;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;

import de.enough.polish.util.Locale;

/**
 * The SECellLocLogger is a LocationMsgReceiver that listens to location updates
 * and if the location has changed significantly saves a location and cell id to
 * a text file. This can then be used to upload to openCellId.org to help fill
 * their database with cell tower locations.
 * 
 * This currently only works for Sony Ericsson JP-7.3 and later phones.
 */
public class SECellLocLogger implements LocationMsgReceiver {

	private static final Logger logger = Logger.getInstance(
			SECellLocLogger.class, Logger.TRACE);

	private Position prevPos;

	//#if polish.api.fileconnection
	private FileConnection logCon;
	private Writer wr;
	//#endif
	
	private int noSamples;
	private int noValid;

	private boolean posValid;
	private static boolean cellIDLogging = false;
	private static boolean loggingSuccess = false;
	private CellIdProvider cellProvider;

	public boolean init() {
		//#if polish.api.fileconnection
		try {
			//#debug info
			logger.info("Attempting to enable cell-id logging for OpenCellId.org");

			prevPos = null;
			posValid = false;
			noSamples = 0;
			noValid = -5;
			cellIDLogging = false;
			
			String url = Configuration.getGpsRawLoggerUrl();
			url += "cellIDLog" + HelperRoutines.formatSimpleDateNow() + ".txt";
			
			cellProvider = CellIdProvider.getInstance();

			if (cellProvider.obtainCurrentCellId() != null) {
				try {
					Connection con = Connector.open(url);
					if (con instanceof FileConnection) {
						logCon = (FileConnection) con;
						if (!logCon.exists()) {
							logCon.create();
						}
						wr = new OutputStreamWriter(logCon.openOutputStream());
						wr.write("lat,lon,mcc,mnc,lac,cellid,\n");
					} else {
						logger.info("Trying to perform cell-id logging on anything else than filesystem is currently not supported");
						return false;
					}
					//#debug info
					logger.info("Enabling cell-id logging");
					cellIDLogging = true;
					return true;
				} catch (SecurityException se) {
					logger.exception(
						Locale.get("secellloclogger.LoggingCellIDsNotPermitted")/*Logging of Cell-IDs is not permitted on this phone.*/, se);
					return false;
				} catch (IOException ioe) {
					logger.exception(
						Locale.get("secellloclogger.FailedToWriteCellIDLog")/*Failed to write Cell-ID log file.*/, ioe);
					if ((logCon != null) && (logCon.exists())) {
						logCon.delete();
					}
						
				}
			} else {
				//#debug info
				logger.info("Cell-ID properties were empty, this is only supported on newer Sony Ericsson phones.");
				ShareNav.getInstance().alert(Locale.get("secellloclogger.CellLogging")/*Cell logging*/, 
							   Locale.get("secellloclogger.EmptyCellIDProp")/*Cell-ID properties were empty, this is only supported on some phones.*/,
					Alert.FOREVER);
			}
		} catch (Exception e) {
			logger.silentexception(
				"Logging of Cell-IDs is not supported on this phone.", e);
			ShareNav.getInstance().alert(Locale.get("secellloclogger.CellLogging")/*Cell logging*/, 
						   Locale.get("secellloclogger.CellIDLogNotSupported")/*Logging of Cell-IDs is not supported on this phone.*/, 
				Alert.FOREVER);
		}
		//#endif
		//#debug info
		logger.info("NOT enabling cell-id logging");
		return false;
	}

	public void locationDecoderEnd() {
		locationDecoderEnd(Locale.get("secellloclogger.Closing")/*Closing*/);
		cellIDLogging = false;
	}

	public void locationDecoderEnd(String msg) {
		logger.info("Closing Cell-id logger with msg: " + msg);
		//#if polish.api.fileconnection
		
		try {
			if (wr != null) {
				wr.flush();
				wr.close();
				wr = null;
			}
			if (noSamples == 0) {
				//#debug info
				logger.info("No Cell-IDs recorded, deleting empty log file");
				logCon.delete();
			}
			//			else {
			    //			    if (Configuration.getCfgBitState(Configuration.CFGBIT_CELLID_ALWAYS) ||
			    //				Configuration.getCfgBitState(Configuration.CFGBIT_CELLID_CONFIRM))
			    //				OpencellidUpload(ShareNav.getInstance(), null );
			//			}
		} catch (IOException ioe) {
			logger.exception(Locale.get("secellloclogger.FailedToCloseCellidLogger")/*Failed to close cell-id logger*/, ioe);
		}
		//#endif
		cellIDLogging = false;
	}

	public void receiveMessage(String s) {
		// Nothing to do
	}

	public void receivePosition(Position pos) {
		//#if polish.api.fileconnection

		//#debug trace
		logger.trace("Received position update: " + pos);

		if (posValid == false) {
			//#debug debug
			logger.debug("Currently no valid fix, so skipping CellID logging");
			loggingSuccess = false;
			return;
		}
		noValid++;
		if (noValid < 6) {
			/*
			 * We throw away the first 5 samples after
			 * the GPS has become valid again. This is as
			 * the first samples are often still lower quality.
			 * Either has the lock hasn't fully been established,
			 * or as the first sample might be "the last known position"
			 * which can be completely off from where we (and the cell) are
			 * at the moment. This hopefully reduces the chance of saving
			 * bogus cell positions. At a sampling rate of 1 Hz, this should
			 * only be 5 seconds 
			 */
			loggingSuccess = false;
			return;
		}

		if (prevPos != null) {
			float dist = ProjMath.getDistance(pos.latitude
					* MoreMath.FAC_DECTORAD, pos.longitude
					* MoreMath.FAC_DECTORAD, prevPos.latitude
					* MoreMath.FAC_DECTORAD, prevPos.longitude
					* MoreMath.FAC_DECTORAD);
			//#debug debug
			logger.debug("Distance from previously saved pos: " + dist);
			if (dist < 25.0f) {
				return;
			}
		}
		

		try {

			GsmCell cell = cellProvider.obtainCurrentCellId();

			if ((cell != null) && (cell.mcc > 0) && (cell.mnc > 0) && (cell.lac > 0) && (cell.cellID > 0)){
				//#debug debug
				logger.debug("Cellid: " + cell.cellID + "  mcc: " + cell.mcc
						+ "  mnc: " + cell.mnc + "  lac: " + cell.lac + " -> "
						+ pos.latitude + "," + pos.longitude);
				wr.write(pos.latitude + "," + pos.longitude + "," + cell.mcc
						+ "," + cell.mnc + "," + cell.lac + "," + cell.cellID + "\n");
				wr.flush();
				loggingSuccess = true;
				noSamples++;
				/**
				 * We need to clone the prevPos, as the pos object gets reused for each
				 * new position
				 */
				prevPos = new Position(pos);
			} else {
				loggingSuccess = false;
			}
		} catch (Exception e) {
			logger.silentexception("Failed to retrieve Cell-id for logging", e);
		}
		//#endif
	}

	public void receiveStatus(byte status, int satsReceived) {
		if (LocationMsgReceiverList.isPosValid(status)) {
			posValid = true;
		} else {
			posValid = false;
			if (noValid > 0) {
				noValid = 0;
			}
		}
	}

	public void receiveSatellites(Satellite[] sats) {
		// Nothing to do
	}

	public void receiveStatistics(int[] statRecord, byte qualtity) {
		// Nothing to do
	}
	
	public static int isCellIDLogging() {
		if (!cellIDLogging) {
			return 0;
		}
		if (loggingSuccess) {
			return 2;
		} else {
			return 1;
		}
	}
}
