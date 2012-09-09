/*
 * ShareNav - Copyright (c) 2009
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

package net.sharenav.gps.location;

import java.util.Enumeration;
import java.util.Vector;

import de.enough.polish.util.Locale;

import net.sharenav.gps.Satellite;
import net.sharenav.sharenav.data.Position;
import net.sharenav.util.Logger;

/**
 * Wrapper that wraps a List of LocationMsgReceivers in a way that Classes can
 * use it like a single LocationMsgReceiver
 * 
 * @author jan rose
 * 
 */
public class LocationMsgReceiverList implements LocationMsgReceiver {
	private final static Logger logger = 
		Logger.getInstance(LocationMsgReceiverList.class, Logger.DEBUG);

	/**
	 * Vector of all the LocationMsgReceivers
	 */
	private final Vector mReceiverList;
	
	private volatile byte mCurrentStatus;
	
	private volatile String mCurrentStatusString = "";
	
	private volatile int mCurrentNumSats;

	public LocationMsgReceiverList() {
		mReceiverList = new Vector(2);
	}

	public void locationDecoderEnd() {
		Enumeration en = mReceiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.locationDecoderEnd();
		}
	}

	public void locationDecoderEnd(String msg) {
		Enumeration en = mReceiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.locationDecoderEnd(msg);
		}
	}

	public void receiveMessage(String msg) {
		Enumeration en = mReceiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveMessage(msg);
		}
	}

	public void receivePosition(Position pos) {
		Enumeration en = mReceiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receivePosition(pos);
		}
	}

	public void receiveStatus(byte status, int satsReceived) {
		mCurrentStatus = status;
		mCurrentNumSats = satsReceived;
		switch (mCurrentStatus)
		{
		case LocationMsgReceiver.STATUS_OFF:
			mCurrentStatusString = Locale.get("solution.Off");
			break;
		case LocationMsgReceiver.STATUS_SECEX:
			mCurrentStatusString = Locale.get("solution.SecEx");
			break;
		case LocationMsgReceiver.STATUS_NOFIX:
			mCurrentStatusString = Locale.get("solution.NoFix")
			    + ((mCurrentNumSats != 0) ? mCurrentNumSats + "S" : "");
			break;
		case LocationMsgReceiver.STATUS_RECONNECT:
			mCurrentStatusString = Locale.get("solution.tildes");
			break;
		case LocationMsgReceiver.STATUS_ON:
			mCurrentStatusString = ((mCurrentNumSats != 0) ? mCurrentNumSats + "S" : Locale.get("solution.On"));
			break;
		case LocationMsgReceiver.STATUS_2D:
			mCurrentStatusString = mCurrentNumSats + "S";
			break;
		case LocationMsgReceiver.STATUS_3D:
			mCurrentStatusString = mCurrentNumSats + "S";
			break;
		case LocationMsgReceiver.STATUS_DGPS:
			mCurrentStatusString = "D" + mCurrentNumSats + "S";
			break;
		case LocationMsgReceiver.STATUS_CELLID:
			mCurrentStatusString = Locale.get("solution.Cell");
			break;
		case LocationMsgReceiver.STATUS_MANUAL:
			mCurrentStatusString = Locale.get("solution.ManualLoc");
			break;
		}
		Enumeration en = mReceiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveStatus(status, satsReceived);
		}
	}
	
	public byte getCurrentStatus() {
		return mCurrentStatus;
	}

	/** Returns the string representation of the current location status and number
	 * of satellites received.
	 * 
	 * @return Status as string representation in the current UI language
	 */
	public String getCurrentStatusString() {
		return mCurrentStatusString;
	}

	public int getNumSats() {
		return mCurrentNumSats;
	}

	/** Checks if the current position is valid.
	 * Note that a CellID-based location is not considered valid as it is only a rough
	 * position which is not good enough e.g. for waypoints.
	 * 
	 * @return True if position is good, false if it isn't.
	 */
	public boolean isPosValid() {
		return (mCurrentStatus == STATUS_ON) || (mCurrentStatus == STATUS_2D) 
			|| (mCurrentStatus == STATUS_3D) || (mCurrentStatus == STATUS_DGPS)
			|| (mCurrentStatus == STATUS_MANUAL);
	}
	
	/** Returns the string representation of the location status.
	 * It is only a temporary solution that the caller needs to pass the current
	 * status and satsReceived, but it is currently necessary because callers
	 * don't have access to this class's instance.
	 * Instead, it should be possible for callers to get everything centrally from this
	 * class, which will avoid recalculations.
	 * The same applies for isPosValid(byte status).
	 * 
	 * @param status Status for which to generate the string
	 * @param satsReceived Number of sats to use to generate the string
	 * @return Status as string representation in the current UI language
	 */
	public static String getCurrentStatusString(byte status, int satsReceived) {
		switch (status)
		{
		case LocationMsgReceiver.STATUS_OFF:
		default:
			return Locale.get("solution.Off");
		case LocationMsgReceiver.STATUS_SECEX:
			return Locale.get("solution.SecEx");
		case LocationMsgReceiver.STATUS_NOFIX:
			return Locale.get("solution.NoFix")
			    + ((satsReceived != 0) ? satsReceived + "S" : "");
		case LocationMsgReceiver.STATUS_RECONNECT:
			return Locale.get("solution.tildes");
		case LocationMsgReceiver.STATUS_ON: // JSR-179, num of sats unknown
			return ((satsReceived != 0) ? satsReceived + "S" : Locale.get("solution.On"));
		case LocationMsgReceiver.STATUS_2D:
			return satsReceived + "S";
		case LocationMsgReceiver.STATUS_3D:
			return satsReceived + "S";
		case LocationMsgReceiver.STATUS_DGPS:
			return "D" + satsReceived + "S";
		case LocationMsgReceiver.STATUS_CELLID:
			return Locale.get("solution.Cell");
		case LocationMsgReceiver.STATUS_MANUAL:
			return Locale.get("solution.ManualLoc");
		}
	}
	
	/** Checks if position is valid.
	 * Only temporary, see the note for getCurrentStatusString(byte status, int satsReceived).
	 * 
	 * @param status Status to check
	 * @return True if position is valid, false if it isn't
	 */
	public static boolean isPosValid(byte status) {
		return (status == STATUS_ON) || (status == STATUS_2D) 
		|| (status == STATUS_3D) || (status == STATUS_DGPS)
		|| (status == STATUS_MANUAL);		
	}

	public void receiveSatellites(Satellite[] sats) {
		Enumeration en = mReceiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveSatellites(sats);
		}
	}

	public void receiveStatistics(int[] statRecord, byte quality) {
		Enumeration en = mReceiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveStatistics(statRecord, quality);
		}
	}

	/**
	 * add a receiver to the List
	 * 
	 * @param rec
	 *            LocationMsgReceiver to add
	 */
	public void addReceiver(LocationMsgReceiver receiver) {
		//#debug info
		logger.info("Adding a location receiver to the list (" + receiver + ")");
		mReceiverList.addElement(receiver);
	}

	/**
	 * remove a given receiver from the List
	 * 
	 * @param rec
	 *            LocationMsgReceiver to be removed
	 */
	public boolean removeReceiver(LocationMsgReceiver receiver) {
		//#debug info
		logger.info("Removing location receiver from the list (" + receiver + ")");
		// should be safe to remove the first occurrence
		return mReceiverList.removeElement(receiver);
	}
}
