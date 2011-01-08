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

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
//#if polish.api.online
import de.ueller.gps.tools.HTTPhelper;
//#endif
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.gps.tools.intTree;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.CompassProducer;
import de.ueller.midlet.gps.CompassReceiver;
import de.ueller.midlet.gps.CompassReceiverList;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.data.CompassProvider;
import de.ueller.midlet.gps.data.Compass;
import de.ueller.gps.data.Configuration;

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
public class GetCompass implements CompassProducer {
	
	private CompassProvider compassProvider;

	public void triggerLastKnownPositionUpdate() {
	}

	public void triggerPositionUpdate() {
	}

	/**
	 * Periodically retrieve the current Cell-id and
	 * convert cell id to a location and send it
	 * to the LocationReceiver
	 *
	 */
	public class RetrievePosition extends TimerTask {
		
		public void run() {
			Compass compass = null;
			try {
				if (closed) {
					this.cancel();
					return;
				}

				compass = compassProvider.obtainCurrentCompass();
				if ((compass == null)) {
					//#debug debug
					logger.debug("No compass direction available");
					receiverList.receiveCompassStatus(0);
					return;
				}

				//#debug info
				logger.info("Obtained a compass reading " + compass.direction);
				receiverList.receiveCompassStatus(1);
				receiverList.receiveCompass(compass.direction);
			} catch (Exception e) {
				logger.silentexception("Could not retrieve compass direction", e);
				this.cancel();
				close(Locale.get("getcompass.CompDirFailed")/*Compass direction retrieval failed*/);
			}
		}
	}

	private static final Logger logger = Logger.getInstance(GetCompass.class,
			Logger.TRACE);

	protected Thread processorThread;
	protected CompassReceiverList receiverList;
	protected boolean closed = false;
	private String message;
	private RetrievePosition rp;

	public GetCompass() {
		this.receiverList = new CompassReceiverList();
	}

	public boolean init(CompassReceiver receiver) {
		try {
			this.receiverList.addReceiver(receiver);
			
			compassProvider = CompassProvider.getInstance();
			
			if (compassProvider.obtainCurrentCompass() == null) {
				//#debug info
				logger.info("No valid compass direction, closing down");
				//this.receiverList.locationDecoderEnd(Locale.get("getcompass.NoValidCompass")/*No valid compass direction*/);
				return false;
			}
			closed = false;
			
			return true;
		} catch (Exception e) {
			logger.silentexception("Could not retrieve compass direction", e);
		}
		//this.receiverList.locationDecoderEnd(Locale.get("getcompass.CompassFail")/*Can't use compass for direction*/);
		return false;
	}
	
	public void enableRawLogging(OutputStream os) {
		//rawDataLogger = os;
	}

	public void disableRawLogging() {
	}

	public boolean activate(CompassReceiver receiver) {
		rp = new RetrievePosition();
		GpsMid.getTimer().schedule(rp, 250, 250);
		return true;
	}
	public boolean deactivate(CompassReceiver receiver) {
		return true;
	}
	
	public void close() {
		logger.info("Location producer closing");
		closed = true;
		if (processorThread != null)
			processorThread.interrupt();
		//receiverList.locationDecoderEnd();
	}

	public void close(String message) {
		this.message = message;
		close();
	}

	public void addCompassReceiver(CompassReceiver receiver) {
		receiverList.addReceiver(receiver);
	}

	public boolean removeCompassReceiver(CompassReceiver receiver) {
		return receiverList.removeReceiver(receiver);
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
