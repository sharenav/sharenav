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

import net.sharenav.util.Logger;

/**
 * Wrapper that wraps a List of CompassReceivers in a way that classes can
 * use it like a single CompassReceiver.
 * 
 * @author jan rose
 */
public class CompassReceiverList implements CompassReceiver {
	private final static Logger logger = 
		Logger.getInstance(CompassReceiverList.class, Logger.DEBUG);

	/**
	 * Vector of all the CompassReceivers
	 */
	private final Vector receiverList;
	
	private volatile int currentSolution = 0; 

	public CompassReceiverList() {
		receiverList = new Vector(2);
	}

	public void locationDecoderEnd() {
		Enumeration en = receiverList.elements();
		CompassReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (CompassReceiver) en.nextElement();
			receiver.locationDecoderEnd();
		}
	}

	public void locationDecoderEnd(String msg) {
		Enumeration en = receiverList.elements();
		CompassReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (CompassReceiver) en.nextElement();
			receiver.locationDecoderEnd(msg);
		}
	}

	public void receiveMessage(String msg) {
		Enumeration en = receiverList.elements();
		CompassReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (CompassReceiver) en.nextElement();
			receiver.receiveMessage(msg);
		}
	}

	public void receiveCompass(float direction) {
		Enumeration en = receiverList.elements();
		CompassReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (CompassReceiver) en.nextElement();
			receiver.receiveCompass(direction);
		}
	}

	public void receiveCompassStatus(int solution) {
		currentSolution = solution;
		Enumeration en = receiverList.elements();
		CompassReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (CompassReceiver) en.nextElement();
			receiver.receiveCompassStatus(solution);
		}
	}
	
	public int getCurrentSolution() {
		return currentSolution;
	}

	public void receiveStatistics(int[] statRecord, byte quality) {
		Enumeration en = receiverList.elements();
		CompassReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (CompassReceiver) en.nextElement();
			receiver.receiveStatistics(statRecord, quality);
		}
	}

	/**
	 * add a receiver to the List
	 * 
	 * @param rec
	 *            CompassReceiver to add
	 */
	public void addReceiver(CompassReceiver receiver) {
		//#debug info
		logger.info("Adding a location receiver to the list (" + receiver + ")");
		receiverList.addElement(receiver);
	}

	/**
	 * remove a given receiver from the List
	 * 
	 * @param rec
	 *            CompassReceiver to be removed
	 */
	public boolean removeReceiver(CompassReceiver receiver) {
		//#debug info
		logger.info("Removing location receiver from the list (" + receiver + ")");
		// should be safe to remove the first occurrence
		return receiverList.removeElement(receiver);
	}
}
