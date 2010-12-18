/*
 * GpsMid - Copyright (c) 2009
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
 * See Copying
 */
package de.ueller.midlet.gps;

import java.util.Enumeration;
import java.util.Vector;
import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;

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
	private Vector receiverList;
	
	private volatile String currentSolution = ""; 

	public LocationMsgReceiverList() {
		receiverList = new Vector(2);
	}

	public void locationDecoderEnd() {
		Enumeration en = receiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.locationDecoderEnd();
		}
	}

	public void locationDecoderEnd(String msg) {
		Enumeration en = receiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.locationDecoderEnd(msg);
		}
	}

	public void receiveMessage(String msg) {
		Enumeration en = receiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveMessage(msg);
		}
	}

	public void receivePosition(Position pos) {
		Enumeration en = receiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receivePosition(pos);
		}
	}

	public void receiveSolution(String solution) {
		currentSolution = solution;
		Enumeration en = receiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveSolution(solution);
		}
	}
	
	public String getCurrentSolution() {
		return currentSolution;
	}

	public void receiveSatellites(Satelit[] sats) {
		Enumeration en = receiverList.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveSatellites(sats);
		}
	}

	public void receiveStatistics(int[] statRecord, byte quality) {
		Enumeration en = receiverList.elements();
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
		receiverList.addElement(receiver);
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
		return receiverList.removeElement(receiver);
	}
}
