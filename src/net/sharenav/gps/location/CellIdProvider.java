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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import net.sharenav.gps.location.GetCompass;
import net.sharenav.gps.location.SocketGateway;
import net.sharenav.util.Logger;

//#if polish.android
import android.content.Context;
import de.enough.polish.android.midlet.MidletBridge;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
//#endif

import de.enough.polish.util.Locale;

public class CellIdProvider {
	private static final int CELLMETHOD_NONE = 0;
	private static final int CELLMETHOD_SE = 1;
	private static final int CELLMETHOD_S60FP2 = 2;
	private static final int CELLMETHOD_MOTO = 3;
	private static final int CELLMETHOD_SOCKET = 4;
	private static final int CELLMETHOD_DEBUG = 5;
	private static final int CELLMETHOD_ANDROID = 6;
	private static final int CELLMETHOD_SAMSUNG = 7;
	private static final int CELLMETHOD_LG = 8;
	
	private static CellIdProvider singelton;
	
	private static final Logger logger = Logger.getInstance(CellIdProvider.class,
			Logger.TRACE);
	
	private int cellRetrievelMethod = -1;
	
	GsmCell cachedCell = null;
	
	private CellIdProvider() {
		//#debug info
		logger.info("Trying to find a suitable cell id provider");
		//#if polish.android
		try {
			//#debug info
			logger.info("Trying to see if android method is available");
			GsmCell cell = obtainAndroidCell();
			if (cell != null) {
				cellRetrievelMethod = CELLMETHOD_ANDROID;
				//#debug info
				logger.info("   Yes, the Android method works");
				return;
			} else {
				//#debug info
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving CellID as Android failed", e);
			//Nothing to do here, just fall through to the next method
		}
		//#endif
		try {
			//#debug info
			logger.info("Trying to see if Sony-Ericcson method is available");
			GsmCell cell = obtainSECell();
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
			logger.info("Trying to see if Motorola method is available");
			GsmCell cell = obtainMotoOrSamsungCell(false);
			if (cell != null) {
				logger.error(Locale.get("cellidprovider.MotorolaCellIDPleseCheck")/*Motorola CellID is experimental and may be wrong. Please check data before uploading*/);
				cellRetrievelMethod = CELLMETHOD_MOTO;
				//#debug info
				logger.info("   Yes, the Motorola method works");
				return;
			} else {
				//#debug info
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving CellID as a Motorola failed", e);
			//Nothing to do here, just fall through to the next method
		}
		try {
			//#debug info
			logger.info("Trying to see if Samsung method is available");
			GsmCell cell = obtainMotoOrSamsungCell(true);
			if (cell != null) {
				cellRetrievelMethod = CELLMETHOD_SAMSUNG;
				//#debug info
				logger.info("   Yes, the Samsung method works");
				return;
			} else {
				//#debug info
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving CellID as a Samsung failed", e);
			//Nothing to do here, just fall through to the next method
		}
		try {
			//#debug info
			logger.info("Trying to see if LG method is available");
			GsmCell cell = obtainLGCell();
			if (cell != null) {
				cellRetrievelMethod = CELLMETHOD_LG;
				//#debug info
				logger.info("   Yes, the LG method works");
				return;
			} else {
				//#debug info
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving CellID as an LG failed", e);
			//Nothing to do here, just fall through to the next method
		}
		try {
			//#debug info
			logger.info("Trying to see if there is a cellid server running on this device");
			GsmCell cell = obtainSocketCell();
			// FIXME 
			// cellRetrievelMethod = CELLMETHOD_SOCKET;
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
			GsmCell cell = obtainS60FP2Cell();
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
		logger.error(Locale.get("cellidprovider.NoCellIDUsable")/*No method of retrieving CellID is valid, can not use CellID*/);
		
	}
	
	public synchronized static CellIdProvider getInstance() {
		if (singelton == null) {
			singelton = new CellIdProvider();
		}
		return singelton;
	}
	
	//#if polish.android
	private GsmCell obtainAndroidCell() {
		GsmCell cell = new GsmCell();
		
		TelephonyManager tm  = 
			(TelephonyManager) MidletBridge.instance.getSystemService(Context.TELEPHONY_SERVICE); 
		GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();
		cell.cellID = location.getCid();
                cell.lac = location.getLac();

		String networkOperator = tm.getNetworkOperator();
		if (networkOperator != null && networkOperator.length() > 0) {
			try {
				cell.mcc = (short) Integer.parseInt(networkOperator.substring(0, 3));
				cell.mnc = (short) Integer.parseInt(networkOperator.substring(3));
			} catch (NumberFormatException e) {
			}
		}

		if (location == null) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		
		return cell;
	}
	//#endif
	
	private GsmCell obtainSECell() {
		String cellidS = null;
		String mccS = null;
		String mncS = null;
		String lacS = null;
		GsmCell cell = new GsmCell();
		
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
	
	private GsmCell obtainS60FP2Cell() {
		String cellidS = null;
		String mccS = null;
		String mncS = null;
		String lacS = null;
		GsmCell cell = new GsmCell();
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
		 * if there are no results. So for retrieving Cells, not having
		 * the LAC looks ok, but we won't be able to submit new cells
		 * without the LAC
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
				logger.info("This Nokia device supports LAC! Please report this to ShareNav so that we can correctly use the LAC");
			}
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS + " lac: " + lacS, nfe);
			return null;
		}
		
		return cell;
	}
	
	private GsmCell obtainMotoOrSamsungCell(boolean samsung) {
		String cellidS = null;
		String mccS = null;
		String mncS = null;
		String lacS = null;
		String imsi = null;
		GsmCell cell = new GsmCell();
		if (samsung) {
			cellidS = System.getProperty("CELLID");
			lacS = System.getProperty("LAC");
		} else {
			cellidS = System.getProperty("CellID");
			lacS = System.getProperty("LocAreaCode");
		}
		
		/*
		 * This method of getting MNC and MCC seems
		 * highly problematic, as it will produce
		 * broken data when abroad or otherwise
		 * roaming outside the home network.
		 * I hope this won't cause corrupt data
		 * 
		 * Also, it seems that not all networks use
		 * the same format for the imsi. Some have
		 * a two digit mnc and some a three digit mnc
		 * So this method will fail in some countries.
		 */
		imsi  = System.getProperty("IMSI");
		if (imsi != null) {
			mccS  = imsi.substring(0,3);
			mncS  = imsi.substring(3,5);
		}
		if ((cellidS == null) || (mccS == null) || (mncS == null)) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		
		try {
			cell.cellID = Integer.parseInt(cellidS);
			cell.mcc = (short) Integer.parseInt(mccS);
			cell.mnc = (short) Integer.parseInt(mncS);
			cell.lac = Integer.parseInt(lacS);
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS + " lac: " + lacS, nfe);
			return null;
		}
		
		return cell;
	}
	
	private GsmCell obtainLGCell() {
		String cellidS = null;
		String mccS = null;
		String mncS = null;
		GsmCell cell = new GsmCell();
		
		if (System.getProperty("com.lge.lgjp") == null) {
			// should be "LGJP1", "LGJP2" or "LGJP3 according to https://sourceforge.net/tracker/index.php?func=detail&aid=3310226&group_id=192084&atid=939977
			// FIXME should we check the value?
			return null;
		}
		cellidS = System.getProperty("com.lge.net.cellid");
		mccS  = System.getProperty("com.lge.net.cmcc");
		mncS = System.getProperty("com.lge.net.cmnc");
		// apparently we can't get LAC
		if ((cellidS == null) || (mccS == null) || (mncS == null)) {
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		
		try {
			cell.cellID = Integer.parseInt(cellidS);
			cell.mcc = (short) Integer.parseInt(mccS);
			cell.mnc = (short) Integer.parseInt(mncS);
		} catch (NumberFormatException nfe) {
			logger.silentexception("Failed to parse cell-id (cellid: " + cellidS +
					" mcc: " + mccS + " mnc: " + mncS, nfe);
			return null;
		}
		
		return cell;
	}
	
	private GsmCell obtainSocketCell() {
		int retval;
		retval = SocketGateway.getSocketData(SocketGateway.TYPE_CELLID);
		if (retval == SocketGateway.RETURN_OK) {
			return SocketGateway.getCell();
		}
		if (cellRetrievelMethod == CELLMETHOD_SOCKET && retval == SocketGateway.RETURN_IOE) {
			/*
			 * The local helper daemon seems to have died.
			 * No point in trying to continue trying,
			 * as otherwise we will get an exception every time
			 */
			//cellRetrievelMethod = CELLMETHOD_NONE;
			return null;
		}
		return null;
	}
	
	private GsmCell obtainDebugCell() {
		/*
		 * This code is used for debugging cell-id data on the emulator
		 * by generating one of 16 random cell-ids 
		 */
		Random r = new Random();
		switch (r.nextInt(16)) {
		case  0: return new GsmCell(0x2627,    (short) 234, (short)  33, 0x0133);
		case  1: return new GsmCell(0x2628,    (short) 234, (short)  33, 0x0133);
		case  2: return new GsmCell(0x2629,    (short) 234, (short)  33, 0x0133);
		case  3: return new GsmCell(0x2620,    (short) 234, (short)  33, 0x0134);
		case  4: return new GsmCell(0x2619,    (short) 234, (short)  33, 0x0134);
		case  5: return new GsmCell(0x2629,    (short) 234, (short)  33, 0x0135);
		case  6: return new GsmCell(0x2649,    (short) 234, (short)  33, 0x0136);
		case  7: return new GsmCell(0x2659,    (short) 234, (short)  33, 0x0137);
		case  8: return new GsmCell(0xB1D1,    (short) 310, (short) 260, 0xB455);
		case  9: return new GsmCell(0x79D9,    (short) 310, (short) 260, 0x004D);
		case 10: return new GsmCell(0x3E92FFF, (short) 284, (short)   3, 0x03E9);
		case 11: return new GsmCell(0x1B0,     (short) 250, (short)  20, 0x666D);
		case 12: return new GsmCell(0x23EC45A, (short) 234, (short)  10, 0x958C);
		case 13: return new GsmCell(0x8589A,   (short) 234, (short)  10, 0x8139);
		case 14: return new GsmCell(0x85A67,   (short) 234, (short)  10, 0x8139);
		case 15: return new GsmCell(0x151E,    (short) 724, (short)   5, 0x0552);
		default:
			//#debug debug
			logger.debug("No valid cell-id");
			return null;
		}
		
	}
	
	public GsmCell obtainCachedCellID() {
		return cachedCell;
	}
	
	public GsmCell obtainCurrentCellId() throws Exception {
		
		//#debug info
		logger.info("Tring to retrieve cell-id");
		
		switch (cellRetrievelMethod) {
		case CELLMETHOD_NONE:
			//#debug info
			logger.info("Can't retrieve CellID, as there is no valid method available");
			return null;
		case CELLMETHOD_SE:
			cachedCell =  obtainSECell();
			break;
		//#if polish.android
		case CELLMETHOD_ANDROID:
			cachedCell =  obtainAndroidCell();
			break;
		//#endif
		case CELLMETHOD_MOTO:
			cachedCell =  obtainMotoOrSamsungCell(false);
			break;
		case CELLMETHOD_SAMSUNG:
			cachedCell =  obtainMotoOrSamsungCell(true);
			break;
		case CELLMETHOD_LG:
			cachedCell =  obtainLGCell();
			break;
		case CELLMETHOD_S60FP2:
			cachedCell = obtainS60FP2Cell();
			break;
		case CELLMETHOD_SOCKET:
			cachedCell = obtainSocketCell();
			break;
		case CELLMETHOD_DEBUG:
			cachedCell = obtainDebugCell();
			break;
		default:
			//#debug error
			logger.error("Unknown CellID retrieval method selected!");
			return null;
		}
		//#debug debug
		logger.debug("Retrieved " + cachedCell);
		return cachedCell;
	}

}
