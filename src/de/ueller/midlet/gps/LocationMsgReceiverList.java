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
	private final static Logger logger=Logger.getInstance(LocationMsgReceiverList.class,Logger.DEBUG);
	/**
	 * Vector of all the LocationMsgReceivers
	 */
	private Vector receivers;

	public LocationMsgReceiverList() {
		receivers = new Vector(2);
	}

	public void locationDecoderEnd() {
		Enumeration en = receivers.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.locationDecoderEnd();
		}
	}

	public void locationDecoderEnd(String msg) {
		Enumeration en = receivers.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.locationDecoderEnd(msg);
		}
	}

	public void receiveMessage(String s) {
		Enumeration en = receivers.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveMessage(s);
		}
	}

	public void receivePosItion(Position pos) {
		Enumeration en = receivers.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receivePosItion(pos);
		}
	}

	public void receiveSolution(String s) {
		Enumeration en = receivers.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveSolution(s);
		}
	}

	public void receiveStatelit(Satelit[] sat) {
		Enumeration en = receivers.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveStatelit(sat);
		}
	}

	public void receiveStatistics(int[] statRecord, byte qualtity) {
		Enumeration en = receivers.elements();
		LocationMsgReceiver receiver;
		while (en.hasMoreElements()) {
			receiver = (LocationMsgReceiver) en.nextElement();
			receiver.receiveStatistics(statRecord, qualtity);
		}
	}

	/**
	 * add a receiver to the List
	 * 
	 * @param rec
	 *            LocationMsgReceiver to add
	 */
	public void addReceiver(LocationMsgReceiver rec) {
		//#debug info
		logger.info("Adding a location receiver to the list (" + rec + ")");
		receivers.addElement(rec);
	}

	/**
	 * remove a given receiver from the List
	 * 
	 * @param rec
	 *            LocationMsgReceiver to be removed
	 */
	public boolean removeReceiver(LocationMsgReceiver rec) {
		//#debug info
		logger.info("Removing location receiver from the list (" + rec + ")");
		// should be safe to remove the first occurrence
		return receivers.removeElement(rec);
	}
}
