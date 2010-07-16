/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 *        - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 *        - Copyright (c) 2008 sk750 at users dot sourceforge dot net
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
package de.ueller.gps.location;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

//#if polish.api.locationapi
import javax.microedition.location.Coordinates;
import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;
//#endif
import de.ueller.gps.data.Position;
import de.ueller.gps.nmea.NmeaMessage;
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.LocationMsgReceiverList;
import de.ueller.midlet.gps.Logger;

/**
 * This class implements a location producer which uses the JSR-179 API
 * to get the device's current position.
 * This API is supported by most phones with an internal GPS receiver.
 */
public class JSR179Input 
		//#if polish.api.locationapi
		implements LocationListener, LocationMsgProducer
		//#endif
{
	private final static Logger logger = Logger.getInstance(JSR179Input.class,
			Logger.TRACE);

	//#if polish.api.locationapi
	private LocationProvider locationProvider = null;
	private LocationMsgReceiverList receiverList;
	private NmeaMessage smsg;
	Position pos = new Position(0f, 0f, 0f, 0f, 0f, 0, System.currentTimeMillis());

	private OutputStream rawDataLogger;

	
	public JSR179Input() {
		this.receiverList = new LocationMsgReceiverList();
	}


	public boolean init(LocationMsgReceiver receiver) {
		logger.info("start JSR179 LocationProvider");
		this.receiverList.addReceiver(receiver);
		// We may be able to get some additional information such as the number
		// of satellites form the NMEA string
		smsg = new NmeaMessage(receiverList);
		createLocationProvider();
		
		return true;
	}

	/**
	 * Initializes LocationProvider uses default criteria
	 * 
	 * @throws Exception
	 */
	void createLocationProvider() {
		logger.trace("enter createLocationProvider()");
		if (locationProvider == null) {
			// try out different locationprovider criteria combinations, the
			// ones with maximum features first
			for (int i = 0; i <= 3; i++) {
				try {
					Criteria criteria = new Criteria();
					switch (i) {
					case 0:
						criteria.setAltitudeRequired(true);
						criteria.setSpeedAndCourseRequired(true);
						break;
					case 1:
						criteria.setSpeedAndCourseRequired(true);
						break;
					case 2:
						criteria.setAltitudeRequired(true);
					}
					locationProvider = LocationProvider.getInstance(criteria);
					if (locationProvider != null) {
						logger.info("Chosen location provider:" + locationProvider);
						break; // we are using this criteria
					}
				} catch (LocationException le) {
					logger.info("LocationProvider criteria not fitting: " + i);
					locationProvider = null;
				} catch (Exception e) {
					logger.exception("unexpected exception while probing LocationProvider criteria.",e);
				}
			}
			if (locationProvider != null) {
				updateSolution(locationProvider.getState());
			} else {
				receiverList.locationDecoderEnd("no JSR179 Provider"/* i:NoJSR179Provider */);
				//#debug info
				logger.info("Cannot create LocationProvider for criteria.");
			}
		}
		//#debug
		logger.trace("exit createLocationProvider()");
	}

	public void locationUpdated(LocationProvider provider, Location location) {
		//#debug info
		logger.info("updateLocation: " + location);
		if (location == null) {
			return;
		}
		//#debug debug
		logger.debug("received Location: " + location.getLocationMethod());
		
		Coordinates coordinates = location.getQualifiedCoordinates();
		pos.latitude = (float) coordinates.getLatitude();
		pos.longitude = (float) coordinates.getLongitude();
		pos.altitude = (float) coordinates.getAltitude();
		pos.course = location.getCourse();
		pos.speed = location.getSpeed();
		pos.timeMillis = location.getTimestamp();
		receiverList.receivePosition(pos);
		String nmeaString = location
				.getExtraInfo("application/X-jsr179-location-nmea");
		//nmeaString =
		//	 "$GPGSA,A,3,32,23,31,11,20,,,,,,,,2.8,2.6,1.0*3D$GPGSV,3,1,10,20,71,071,38,23,60,168,41,17,42,251,25,11,36,148,37*73$GPGSV,3,2,10,04,29,300,16,13,26,197,,31,21,054,47,32,10,074,38*70$GPGSV,3,3,10,12,04,339,17,05,01,353,15*75";
		// ;
		//#debug info
		logger.info("Using extra NMEA info in JSR179: " + nmeaString);
		
		if (nmeaString != null) {
			if (rawDataLogger != null) {
				try {
					rawDataLogger.write(nmeaString.getBytes());
					rawDataLogger.flush();
				} catch (IOException ioe) {
					logger.exception("Could not write raw GPS log", ioe);
				}
			}
			Vector messages = StringTokenizer.getVector(nmeaString, "$");
			if (messages != null) {
				for (int i = 0; i < messages.size(); i++) {
					String nmeaMessage = (String) messages.elementAt(i);
					if (nmeaMessage == null) {
						continue;
					}
					if (nmeaMessage.startsWith("$")) {
						// Cut off $GP from the start
						nmeaMessage = nmeaMessage.substring(3);
					} else if (nmeaMessage.startsWith("GP")) {
						// Cut off GP from the start
						nmeaMessage = nmeaMessage.substring(2);
					}
					int delimiterIdx = nmeaMessage.indexOf("*");
					if (delimiterIdx > 0) {
						// remove the checksum
						nmeaMessage = nmeaMessage.substring(0, delimiterIdx);
					}
					delimiterIdx = nmeaMessage.indexOf(" ");
					if (delimiterIdx > 0) {
						// remove trailing whitespace because some mobiles like HTC Touch Diamond 2 with JavaFX
						// receive NMEA sentences terminated by a space instead of a star followed by the checksum
						nmeaMessage = nmeaMessage.substring(0, delimiterIdx);
					}
					//#debug info
					logger.info("Decoding: " + nmeaMessage);
					if ((nmeaMessage != null) && (nmeaMessage.length() > 5))
						smsg.decodeMessage(nmeaMessage, false);
				}
			}
		}

		// logger.trace("exit locationUpdated(provider,location)");
	}

	public void providerStateChanged(LocationProvider lprov, int state) {
		//#debug info
		logger.info("providerStateChanged(" + lprov + "," + state + ")");
		updateSolution(state);
	}

	public void close() {
		//#debug
		logger.trace("enter close()");
		// if (locationProvider != null){
		// locationProvider.setLocationListener(null, -1, -1, -1);
		// }
		if (locationProvider != null) {
			// locationProvider.reset();
			try {
				locationProvider.setLocationListener(null, 0, 0, 0);
			} catch (Exception e) {
			}
		}
		locationProvider = null;
		receiverList.locationDecoderEnd();
		//#debug
		logger.trace("exit close()");
	}

	private void updateSolution(int state) {
		logger.info("Update Solution");
		if (state == LocationProvider.AVAILABLE) {
			locationProvider.setLocationListener(this, 1, -1, -1);
			if (receiverList != null) {
				receiverList.receiveSolution("On"/* i:On */);
			}
			
		}
		//#if polish.android
		// FIXME current (2010-06) android j2mepolish gives OUT_OF_SERVICE even when a fix exists
		//#else
		if (state == LocationProvider.OUT_OF_SERVICE) {
			locationProvider.setLocationListener(this, 1, -1, -1);
			if (receiverList != null) {
				receiverList.receiveSolution("Off"/* i:Off */);
				receiverList.receiveMessage("provider stopped"/* i:ProviderStopped */);
			}
		}
		//#endif
		//#if polish.android
		if (state == LocationProvider.TEMPORARILY_UNAVAILABLE || state == LocationProvider.OUT_OF_SERVICE) {
		//#else
		if (state == LocationProvider.TEMPORARILY_UNAVAILABLE) {
		//#endif
			/**
			 * Even though the receiver is temporarily un-available,
			 * we still need to receive updates periodically, as some
			 * implementations will otherwise not reacquire the GPS signal.
			 * So setting setLocationListener to 0 interval, which should have given
			 * you all the status changes, does not work.
			 */
			locationProvider.setLocationListener(this, 1, -1, -1);
			if (receiverList != null) {
				receiverList.receiveSolution("NoFix"/* i:NoFix */);
			}
		}
		locationUpdated(locationProvider, LocationProvider.getLastKnownLocation());
	}

	public void disableRawLogging() {
		if (rawDataLogger != null) {
			try {
				rawDataLogger.close();
			} catch (IOException e) {
				logger.exception("Couldn't close raw gps logger", e);
			}
			rawDataLogger = null;
		}
	}

	public void enableRawLogging(OutputStream os) {
		rawDataLogger = os;
	}

	public void addLocationMsgReceiver(LocationMsgReceiver receiver) {
		receiverList.addReceiver(receiver);
	}

	public boolean removeLocationMsgReceiver(LocationMsgReceiver receiver) {
		return receiverList.removeReceiver(receiver);
	}
	//#endif
}
