/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
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
package net.sharenav.gps.location;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

//#if polish.api.locationapi
import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;
import javax.microedition.location.QualifiedCoordinates;
//#endif
import net.sharenav.sharenav.data.Position;
import net.sharenav.util.Logger;
import net.sharenav.util.StringTokenizer;

import de.enough.polish.util.Locale;

/**
 * This class implements a location producer which uses the JSR-179 API
 * to get the device's current position.
 * This API is supported by most phones with an internal GPS receiver.
 */
public class Jsr179Input 
		//#if polish.api.locationapi
		implements LocationListener, LocationMsgProducer
		//#endif
{
	private final static Logger logger = Logger.getInstance(Jsr179Input.class,
			Logger.TRACE);

	//#if polish.api.locationapi
	private LocationProvider locationProvider = null;
	private final LocationMsgReceiverList receiverList;
	private NmeaMessage smsg;
	Position pos = new Position(0f, 0f, 0f, 0f, 0f, 0, System.currentTimeMillis());

	private int numSatellites = 0;

	private OutputStream rawDataLogger;

	
	public Jsr179Input() {
		this.receiverList = new LocationMsgReceiverList();
	}


	public boolean init(LocationMsgReceiver receiver) {
		logger.info("Start Jsr179 LocationProvider");
		this.receiverList.addReceiver(receiver);
		// We may be able to get some additional information such as the number
		// of satellites form the NMEA string
		smsg = new NmeaMessage(receiverList);
		createLocationProvider();
		if (locationProvider != null) {
			return true;
		} else {
			return false;
		}
	}

	public boolean activate(LocationMsgReceiver receiver) {
		// FIXME move activation code (code to enable continuos location feed) here
		return true;
	}
	public boolean deactivate(LocationMsgReceiver receiver) {
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
					logger.exception(Locale.get("jsr179input.unexpectedExceptioninLocProv")/*unexpected exception while probing LocationProvider criteria.*/,e);
				}
			}
			if (locationProvider != null) {
				try {
					locationProvider.setLocationListener(this, 1, -1, -1);
				} catch (Exception e) {
					receiverList.receiveStatus(LocationMsgReceiver.STATUS_SECEX, 0);
					locationProvider = null;
				}
				if (locationProvider != null)  {
					updateSolution(locationProvider.getState());
				}
			} else {
				receiverList.locationDecoderEnd(Locale.get("jsr179input.NoJSR179Provider")/*no JSR179 Provider*/);
				//#debug info
				logger.info("Cannot create LocationProvider for criteria.");
			}
		}
		//#debug
		logger.trace("exit createLocationProvider()");
	}

	public void locationUpdated(LocationProvider provider, Location location) {
		locationUpdated(provider, location, false);
	}

	public void locationUpdated(LocationProvider provider, Location location, boolean lastKnown) {
		//#debug info
		logger.info("updateLocation: " + location);
		if (location == null) {
			return;
		}
		numSatellites = 0;
		//#debug debug
		logger.debug("received Location: " + location.getLocationMethod());
		
		String nmeaString = location
				.getExtraInfo("application/X-jsr179-location-nmea");
		//nmeaString =
		//	 "$GPGSA,A,3,32,23,31,11,20,,,,,,,,2.8,2.6,1.0*3D$GPGSV,3,1,10,20,71,071,38,23,60,168,41,17,42,251,25,11,36,148,37*73$GPGSV,3,2,10,04,29,300,16,13,26,197,,31,21,054,47,32,10,074,38*70$GPGSV,3,3,10,12,04,339,17,05,01,353,15*75";
		// ;
		//#debug info
		logger.info("Using extra NMEA info in JSR179: " + nmeaString);
		
		// FIXME combine to one, duplicated in Jsr179Input and AndroidLocationInput
		if (nmeaString != null) {
			if (rawDataLogger != null) {
				try {
					rawDataLogger.write(nmeaString.getBytes());
					rawDataLogger.flush();
				} catch (IOException ioe) {
					logger.exception(Locale.get("jsr179input.CouldNotWriteGPSLog")/*Could not write raw GPS log*/, ioe);
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
					if ((nmeaMessage != null) && (nmeaMessage.length() > 5)) {
						smsg.decodeMessage(nmeaMessage, false, true);
						// get *DOP from the message
						pos.pdop = smsg.getPosition().pdop;
						pos.hdop = smsg.getPosition().hdop;
						pos.vdop = smsg.getPosition().vdop;
						numSatellites = smsg.getMAllSatellites();
					}
				}
			}
		}

		if (location.isValid()) {
			/* e.g. SE C702 only receives from getExtraInfo() $GPGSV sentences,
			 * therefore use On as solution when it's still set to NoFix though the location is valid
			 */
			if (receiverList.getCurrentStatus() == LocationMsgReceiver.STATUS_NOFIX) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_ON, 0);				
			}
			QualifiedCoordinates coordinates = location.getQualifiedCoordinates();
			pos.latitude = (float) coordinates.getLatitude();
			pos.longitude = (float) coordinates.getLongitude();
			pos.altitude = coordinates.getAltitude();
			pos.course = location.getCourse();
			pos.speed = location.getSpeed();
			pos.timeMillis = location.getTimestamp();
			pos.accuracy = coordinates.getHorizontalAccuracy();
			if (lastKnown) {
				pos.type = Position.TYPE_GPS_LASTKNOWN;
			} else {
				pos.type = Position.TYPE_GPS;
			}
			receiverList.receivePosition(pos);
		} else {
			if (receiverList != null) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, numSatellites);
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
/* locationProvider.setLocationListener(null, 0, 0, 0) tends to freeze Samsung S8000 frequently when stopping GPS
 * was replaced by locationProvider.reset() until 2010-10-21.
 * So if other mobile devices have issues with locationProvider.reset(),
 * please document this here
 * - Aparently Gps is left on, on at least Nokia S40 phones and SE G705, without 
 * locationProvider.setLocationListener(null, 0, 0, 0) - hopefully both reset()
 * and setLocationListener(null... will work on all phones.
 */  
			locationProvider.reset();
			locationProvider.setLocationListener(null, 0, 0, 0);
		}
		locationProvider = null;
		receiverList.locationDecoderEnd();
		//#debug
		logger.trace("exit close()");
	}

	private void updateSolution(int state) {
		logger.info("Update Solution");
		locationUpdated(locationProvider, LocationProvider.getLastKnownLocation(), true);
		if (state == LocationProvider.AVAILABLE) {
			if (receiverList != null) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, 0);
			}
		}
		//#if polish.android
		// FIXME when j2mepolish is fixed; 2010-06 j2mepolish gives OUT_OF_SERVICE
		// even when waiting for a fix, so we don't want to switch GPS off here
		//#else
		if (state == LocationProvider.OUT_OF_SERVICE) {
			if (receiverList != null) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_OFF, 0);
				receiverList.receiveMessage(Locale.get("jsr179input.ProviderStopped")/*provider stopped*/);
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
			if (receiverList != null) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, 0);
			}
		}
	}

	public void triggerPositionUpdate() {
	}

	public void triggerLastKnownPositionUpdate() {
		locationUpdated(locationProvider, LocationProvider.getLastKnownLocation(), true);
	}

	public void disableRawLogging() {
		if (rawDataLogger != null) {
			try {
				rawDataLogger.close();
			} catch (IOException e) {
				logger.exception(Locale.get("jsr179input.CouldntCloseRawGpsLogger")/*Couldnt close raw gps logger*/, e);
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
