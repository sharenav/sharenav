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
package de.ueller.midlet.gps.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

import de.ueller.midlet.gps.Logger;

public class CellIdProvider {
	final int PROTO_REQ_CELLID = 6574723;
	
	private static final int CELLMETHOD_NONE = 0;
	private static final int CELLMETHOD_SE = 1;
	private static final int CELLMETHOD_S60FP2 = 2;
	private static final int CELLMETHOD_SOCKET = 3;
	private static final int CELLMETHOD_DEBUG = 4;
	
	private static CellIdProvider singelton;
	
	private static final Logger logger = Logger.getInstance(CellIdProvider.class,
			Logger.TRACE);
	
	private int cellRetrievelMethod = -1;
	
	SocketConnection clientSock = null;
	DataInputStream clientIS = null;
	DataOutputStream clientOS = null;
	
	GSMCell cachedCell = null;
	
	private CellIdProvider() {
		//#debug info
		logger.info("Trying to find a suitable cell id provider");
		try {
			//#debug info
			logger.info("Trying to see if Sony-Ericcson method is available");
			GSMCell cell = obtainSECell();
			if (cell != null) {
				cellRetrievelMethod = CELLMETHOD_SE;
				//#debug info
				logger.info("   Yes, the Sony-Ericcsson method works");
				return;
			} else {
				//#debug info
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving CellID as a Sony-Ericsson failed", e);
			//Nothing to do here, just fall through to the next method
		}
		try {
			//#debug info
			logger.info("Trying to see if there is a cellid server running on this device");
			GSMCell cell = obtainSocketCell();
			if (cell != null) {
				cellRetrievelMethod = CELLMETHOD_SOCKET;
				logger.info("   Yes, there is a server running and we can get a cell from it");
				return;
			} else {
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Could not connect to socket", e);
			//Nothing to do here, just fall through to the next method
		}
		
		try {
			//#debug info
			logger.info("Trying to see if S60 3rd FP2 method is available");
			GSMCell cell = obtainS60FP2Cell();
			if (cell != null) {
				cellRetrievelMethod = CELLMETHOD_S60FP2;
				logger.info("   Yes, the S60 3rd FP2 method works");
				return;
			} else {
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving CellID as a Nokia S60 3rd FP2 failed", e);
		}
		cellRetrievelMethod = CELLMETHOD_NONE;
		//#debug info
		logger.error("No method of retrieving CellID is valid, can't use CellID");
		
	}
	
	public synchronized static CellIdProvider getInstance() {
		if (singelton == null) {
			singelton = new CellIdProvider();
		}
		return singelton;
	}
	
	private GSMCell obtainSECell() {
		String cellidS = null;
		String mccS = null;
		String mncS = null;
		String lacS = null;
		GSMCell cell = new GSMCell();
		
		cellidS = System.getProperty("com.sonyericsson.net.cellid");
		mccS = System.getProperty("com.sonyericsson.net.cmcc");
		mncS = System.getProperty("com.sonyericsson.net.cmnc");
		lacS = System.getProperty("com.sonyericsson.net.lac");
		
		if ((cellidS == null) || (mccS == null) || (mncS == null) || (lacS == null)) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		
		try {
			cell.cellID = Integer.parseInt(cellidS, 16);
			cell.mcc = (short) Integer.parseInt(mccS);
			cell.mnc = (short) Integer.parseInt(mncS);
			cell.lac = Integer.parseInt(lacS, 16);
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS + " lac: " + lacS, nfe);
			return null;
		}
		
		return cell;
	}
	
	private GSMCell obtainS60FP2Cell() {
		String cellidS = null;
		String mccS = null;
		String mncS = null;
		String lacS = null;
		GSMCell cell = new GSMCell();
		cellidS = System.getProperty("com.nokia.mid.cellid");
		/**
		 * The documentation claims that the country code is returned as
		 * two letter iso country code, but at least my phone Nokia 6220 seems
		 * to return the mcc instead, so assume this gives the mcc for the moment. 
		 */
		mccS = System.getProperty("com.nokia.mid.countrycode");
		mncS = System.getProperty("com.nokia.mid.networkid");
		if (mncS.indexOf(" ") > 0) {
			mncS = mncS.substring(0, mncS.indexOf(" "));
		}
		//System.getProperty("com.nokia.mid.networksignal");
		/*
		 * Lac is not currently supported for S60 devices
		 * The com.nokia.mid.lac comes from S40 devices.
		 * We include this here for the moment, in the hope
		 * that future software updates will include this into
		 * S60 as well.
		 * 
		 * The LAC is needed to uniquely identify cells, but openCellID
		 * seems to do a lookup ignoring LAC at first and only using it
		 * if there are no results. So for retreaving Cells, not having
		 * the LAC looks ok, but we won't be able to submit new cells
		 * with out the LAC
		 */
		lacS = System.getProperty("com.nokia.mid.lac");
		
		if ((cellidS == null) || (mccS == null) || (mncS == null)) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		
		try {
			cell.cellID = Integer.parseInt(cellidS);
			cell.mcc = (short) Integer.parseInt(mccS);
			cell.mnc = (short) Integer.parseInt(mncS);
			if (lacS != null) {
				//#debug info
				logger.info("This Nokia device supports LAC! Please report this to GpsMid so that we can correctly use the LAC");
			}
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS + " lac: " + lacS, nfe);
			return null;
		}
		
		return cell;
	}
	
	private GSMCell obtainSocketCell() {
		if (clientSock == null) {
			try {
				logger.info("Connecting to socket://127.0.0.1:59721");
				clientSock = (SocketConnection) Connector.open("socket://127.0.0.1:59721");
				clientSock.setSocketOption(SocketConnection.KEEPALIVE, 0);
				clientOS = new DataOutputStream(clientSock.openOutputStream());
				clientIS = new DataInputStream(clientSock.openInputStream());
				logger.info("Connected to socket");
				
				
			} catch (SecurityException se) {
				logger.exception("Sorry, you declined to try and connect to a local helper deamon", se);
				clientSock = null;
				return null;
			} catch (ConnectionNotFoundException cnfe) {
				//This is quite common, so silently ignore this;
				logger.silentexception("Could not open a connection to local helper deamon", cnfe);
				clientSock = null;
				return null;
			} catch (IOException ioe) {
				logger.exception("Failed to open connection to a local helper deamon", ioe);
				clientSock = null;
				if (cellRetrievelMethod == CELLMETHOD_SOCKET) {
					/*
					 * The local helper daemon seems to have died.
					 * No point in trying to continue trying,
					 * as otherwise we will get an exception every time
					 */
					cellRetrievelMethod = CELLMETHOD_NONE;
				}
				return null;
			}
		}
		
		try {
			byte [] buf = new byte[4096];
			logger.info("Requesting next CellID");
			int noAvail = clientIS.available();
			while (noAvail > 0) {
				if (noAvail > 4096) {
					noAvail = 4096;
				}
				//#debug debug
				logger.debug("Emptying Buffer of length " + noAvail);
				clientIS.read(buf,0,noAvail);
				noAvail = clientIS.available();
			}
			clientOS.writeInt(PROTO_REQ_CELLID);
			clientOS.flush();
			//debug trace
			logger.trace("Wrote Cell request");
			GSMCell cell = new GSMCell();
			if (clientIS.available() < 18) {
				//#debug debug
				logger.debug("Not Enough Data wait 50");
				Thread.sleep(50);
			}
			if (clientIS.available() < 18) {
				//#debug debug
				logger.debug("Not Enough Data wait 500");
				Thread.sleep(500);
			}
			if (clientIS.available() > 17) {
				//#debug debug
				logger.debug("Reading");
				cell.mcc = (short)clientIS.readInt();
				cell.mnc = (short)clientIS.readInt();
				cell.lac = clientIS.readInt();
				cell.cellID = clientIS.readInt();
				short signal = clientIS.readShort();
				logger.info("Read Cell: " + cell);
				return cell;
			}
			logger.info("Not enough data available from socket, can't retrieve Cell: " + clientIS.available());
		} catch (IOException ioe) {
			logger.silentexception("Failed to read cell", ioe);
			clientSock = null;
			return null;
		} catch (InterruptedException ie) {
			return null;
		}
		return null;
	}
	
	private GSMCell obtainDebugCell() {
		/*
		 * This code is used for debugging cell-id data on the emulator
		 * by generating one of 7 random cell-ids 
		 */
		String cellidS = null;
		String mccS = null;
		String mncS = null;
		String lacS = null;
		GSMCell cell = new GSMCell();
		
		Random r = new Random();
		int rr = r.nextInt(16) + 1;
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
		case 7:
			cellidS = "2649"; mccS = "234"; mncS = "33"; lacS = "136";
			break;
		case 8:
			cellidS = "2659"; mccS = "234"; mncS = "33"; lacS = "137";
			break;
		case 9:
			cellidS = "B1D1"; mccS = "310"; mncS = "260"; lacS = "B455";
			break;
		case 10:
			cellidS = "79D9"; mccS = "310"; mncS = "260"; lacS = "4D";
			break;
		
		case 11:
			cellidS = "3E92FFF"; mccS = "284"; mncS = "3"; lacS = "3E9";
			break;
		case 12:
			cellidS = "1B0"; mccS = "250"; mncS = "20"; lacS = "666D";
			break;
		case 13:
			cellidS = "23EC45A"; mccS = "234"; mncS = "10"; lacS = "958C";
			break;
		case 14:
			cellidS = "8589A"; mccS = "234"; mncS = "10"; lacS = "8139";
			break;
		case 15:
			cellidS = "85A67"; mccS = "234"; mncS = "10"; lacS = "8139";
			break;
		case 16:
			cellidS = "151E"; mccS = "724"; mncS = "5"; lacS = "552";
			break;
		}
		
		if ((cellidS == null) || (mccS == null) || (mncS == null) || (lacS == null)) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		
		try {
			cell.cellID = Integer.parseInt(cellidS, 16);
			cell.mcc = (short) Integer.parseInt(mccS);
			cell.mnc = (short) Integer.parseInt(mncS);
			cell.lac = Integer.parseInt(lacS, 16);
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS + " lac: " + lacS, nfe);
			return null;
		}
		
		return cell;
	}
	
	public GSMCell obtainCachedCellID() {
		return cachedCell;
	}
	
	public GSMCell obtainCurrentCellId() throws Exception {
		
		//#debug info
		logger.info("Tring to retrieve cell-id");
		
		if (cellRetrievelMethod ==  CELLMETHOD_NONE) {
			//#debug info
			logger.info("Can't retrieve CellID, as there is no valid method available");
			return null;
		}

		if (cellRetrievelMethod == CELLMETHOD_SE) {
			cachedCell =  obtainSECell();
		}
		if (cellRetrievelMethod == CELLMETHOD_S60FP2) {
			cachedCell = obtainS60FP2Cell();
		}
		if (cellRetrievelMethod == CELLMETHOD_SOCKET) {
			cachedCell = obtainSocketCell();
		}
		if (cellRetrievelMethod == CELLMETHOD_DEBUG) {
			cachedCell = obtainDebugCell();
		}
		//#debug debug
		logger.debug("Retrieved " + cachedCell);
		return cachedCell;
	}

}
