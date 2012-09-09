/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 *        - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 *        - Copyright (c) 2008 sk750 at users dot sourceforge dot net
 *        - Copyright (c) 2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net
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
//#if polish.android
import android.os.Bundle;
import android.os.Looper;
import de.enough.polish.android.midlet.MidletBridge;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.GpsStatus.NmeaListener;
import android.location.GpsSatellite;
import java.util.Iterator;
//#endif

import net.sharenav.gps.Satellite;
import net.sharenav.sharenav.data.Position;
import net.sharenav.util.Logger;
import net.sharenav.util.StringTokenizer;

import de.enough.polish.util.Locale;

/**
 * This class implements a location producer which uses the Android Location API
 * to get the device's current position.
 */
public class AndroidLocationInput 
		//#if polish.android
		implements GpsStatus.Listener, GpsStatus.NmeaListener, LocationListener, LocationMsgProducer
		//#endif
{
//#if polish.android
	private final static Logger logger = Logger.getInstance(AndroidLocationInput.class,
			Logger.TRACE);

	private Thread looperThread;
	private LocationManager locationManager = null;
	private final LocationMsgReceiverList receiverList;
	private NmeaMessage smsg;
	Position pos = new Position(0f, 0f, 0f, 0f, 0f, 0, System.currentTimeMillis());

	private volatile int numSatellites = 0;
	private volatile boolean hasFix = false;
	private volatile int currentState = LocationMsgReceiver.STATUS_OFF;
	private volatile long lastFixTimestamp;
	private volatile int gpsState = 0;
	private volatile int lmState = 0;
	private Looper locationLooper = null;
	private Criteria savedCriteria = null;

	private OutputStream rawDataLogger;

	private static String provider;

	/** Array with information about the satellites */
	private Satellite satellites[] = new Satellite[36];
	
	public AndroidLocationInput() {
		this.receiverList = new LocationMsgReceiverList();
	}

	public boolean init(LocationMsgReceiver receiver) {
		logger.info("Start AndroidLocation LocationProvider");
		this.receiverList.addReceiver(receiver);
		looperThread = new Thread(new Runnable() {
			public void run() {
				Looper.prepare();
				locationLooper = Looper.myLooper();
				createLocationProvider();
				// We may be able to get some additional information such as the number
				// of satellites form the NMEA string
				smsg = new NmeaMessage(receiverList);
				Looper.loop();
			}
		} );
		looperThread.run();
		if (locationManager != null) {
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
		if (locationManager == null) {
			locationManager = (LocationManager)MidletBridge.instance.getSystemService(Context.LOCATION_SERVICE);
			// try out different locationprovider criteria combinations, the
			// ones with maximum features first
			for (int i = 0; i <= 3; i++) {
				try {
					Criteria criteria = new Criteria();
					switch (i) {
					case 0:
						criteria.setAccuracy(Criteria.ACCURACY_FINE);
						criteria.setAltitudeRequired(true);
						criteria.setBearingRequired(true);
						criteria.setSpeedRequired(true);
						break;
					case 1:
						criteria.setAccuracy(Criteria.ACCURACY_FINE);
						criteria.setBearingRequired(true);
						criteria.setSpeedRequired(true);
						break;
					case 2:
						criteria.setAccuracy(Criteria.ACCURACY_FINE);
						criteria.setAltitudeRequired(true);
					}
					provider = locationManager.getBestProvider(criteria, true);
					if (provider != null) {
						logger.info("Chosen location manager:" + locationManager);
						savedCriteria = criteria;
						break; // we are using this criteria
					}
				} catch (Exception e) {
					logger.exception(Locale.get("androidlocationinput.unexpectedExceptioninLocProv")/*unexpected exception while probing LocationManager criteria.*/,e);
				}
			}
			if (locationManager != null && provider != null) {
				try {
					locationManager.requestLocationUpdates(provider, 0, 0, this);
					locationManager.addGpsStatusListener(this);
					locationManager.addNmeaListener(this);
				} catch (Exception e) {
					logger.fatal("requestLocationUpdates fail: " +  e.getMessage());

					receiverList.receiveStatus(LocationMsgReceiver.STATUS_SECEX, 0);
					locationManager = null;
				}
				if (locationManager != null)  {
					//FIXME
					updateSolution(LocationProvider.TEMPORARILY_UNAVAILABLE);
				}
				
			} else {
				receiverList.locationDecoderEnd(Locale.get("androidlocationinput.nointprovider")/*no internal location provider*/);
				//#debug info
				logger.info("Cannot create LocationProvider for criteria.");
			}
		}
		//#debug
		logger.trace("exit createLocationProvider()");
	}

	public void locationUpdated(LocationManager manager, Location location) {
		locationUpdated(manager, location, false);
	}

	public void locationUpdated(LocationManager manager, Location location, boolean lastKnown) {
		//#debug info
		logger.info("updateLocation: " + location);
		if (location == null) {
			//hasFix = false;
			return;
		}
		lastFixTimestamp = System.currentTimeMillis();
		hasFix = true;
		if (currentState != LocationProvider.AVAILABLE && currentState != LocationProvider.OUT_OF_SERVICE) {
			updateSolution(LocationProvider.AVAILABLE);
		}
		//#debug debug
		logger.debug("received Location: " + location);
		
		pos.latitude = (float) location.getLatitude();
		pos.longitude = (float) location.getLongitude();
		pos.altitude = (float) location.getAltitude();
		pos.course = location.getBearing();
		pos.speed = location.getSpeed();
		pos.timeMillis = location.getTime();
		pos.accuracy = location.getAccuracy();
		if (lastKnown) {
			pos.type = Position.TYPE_GPS_LASTKNOWN;
		} else {
			pos.type = Position.TYPE_GPS;
		}
		receiverList.receivePosition(pos);
		// logger.trace("exit locationUpdated(provider,location)");
	}

	public void providerStateChanged(LocationManager lman, int state) {
		//#debug info
		logger.info("providerStateChanged(" + lman + "," + state + ")");
		updateSolution(state);
	}

	public void close() {
		//#debug
		logger.trace("enter close()");
		// if (locationManager != null){
		// locationManager.setLocationListener(null, -1, -1, -1);
		// }
		if (locationManager != null) {
			locationManager.removeUpdates(this);
			locationManager.removeGpsStatusListener(this);
			locationManager.removeNmeaListener(this);
			if (looperThread != null) {
				locationLooper.quit();
			}
		}
		locationManager = null;
		receiverList.locationDecoderEnd();
		//#debug
		logger.trace("exit close()");
	}

	private void updateSolution(int state) {
		logger.info("Update Solution");
		currentState = state;
		if ((System.currentTimeMillis() - lastFixTimestamp) > 3000) {
			hasFix = false;
		}
		//locationUpdated(locationManager, locationManager.getLastKnownLocation(provider), true);
		if (state == LocationProvider.OUT_OF_SERVICE) {
			if (receiverList != null) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_OFF, 0);
				receiverList.receiveMessage(Locale.get("androidlocationinput.ProviderStopped")/*provider stopped*/);
			}
			// some devices, e.g. Samsung Galaxy Note will claim LocationProvider.AVAILABLE
			// even when there's no fix, so use a timestamp to detect
		} else if (state == LocationProvider.TEMPORARILY_UNAVAILABLE || !hasFix) {
			if (receiverList != null) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, numSatellites);
			}
		} else if (state == LocationProvider.AVAILABLE || hasFix) {
			if (receiverList != null) {
				receiverList.receiveStatus(LocationMsgReceiver.STATUS_ON, numSatellites);
			}
		}
	}

	public void invalidateFix() {
		hasFix = false;
		if (currentState != LocationProvider.TEMPORARILY_UNAVAILABLE
			&& currentState != LocationProvider.OUT_OF_SERVICE) {
			updateSolution(LocationProvider.TEMPORARILY_UNAVAILABLE);
		}
	}
	public void triggerPositionUpdate() {
	}

	public void onNmeaReceived(long timestamp, String nmeaString) {
		//#debug info
		logger.info("Using extra NMEA info in Android location provider: " + nmeaString);
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
						smsg.decodeMessage(nmeaMessage, false, false);
						// get *DOP from the message
						pos.pdop = smsg.getPosition().pdop;
						pos.hdop = smsg.getPosition().hdop;
						pos.vdop = smsg.getPosition().vdop;
						// disable for now if we have a fix; could be this is incorrect for devices with GPS & GLONASS, and we get this from getGpsStatus()
						if (!hasFix) {
							numSatellites = smsg.getMAllSatellites();
							if (currentState == LocationProvider.OUT_OF_SERVICE) {
								updateSolution(LocationProvider.OUT_OF_SERVICE);
							} else {
								updateSolution(LocationProvider.TEMPORARILY_UNAVAILABLE);
							}
						}
					}
				}
			}
		}
	}

	public void onGpsStatusChanged(int state) {
		GpsStatus gpsStatus = locationManager.getGpsStatus(null);
		if (state == GpsStatus.GPS_EVENT_STOPPED) {
			hasFix = false;
			numSatellites = 0;
			updateSolution(LocationProvider.OUT_OF_SERVICE);
		} else if ((System.currentTimeMillis() - lastFixTimestamp) > 3000) {
			invalidateFix();
		}
		if (state == GpsStatus.GPS_EVENT_STARTED) {
			// FIXME do what's needed
			updateSolution(LocationProvider.AVAILABLE);
		}
		gpsState = state;
		if (state == GpsStatus.GPS_EVENT_SATELLITE_STATUS && gpsStatus != null) {
			for (int j = 0; j < 36; j++) {
				/**
				 * Resetting all the satellites to non locked
				 */
				if ((satellites[j] != null)) {
					satellites[j].isLocked(false);
				}
			}
			Iterable<GpsSatellite> andSatellites = gpsStatus.getSatellites();
			Iterator<GpsSatellite> sat = andSatellites.iterator();
			int i = 0;
			while (sat.hasNext()) {
				GpsSatellite satellite = sat.next();
				if (satellites[i] == null) {
					satellites[i] = new Satellite();
				}
				if (i < 36) {
					satellites[i].isLocked(satellite.usedInFix());
					satellites[i].id = i;
					satellites[i].azimut = satellite.getAzimuth();
					satellites[i].elev = satellite.getElevation();
					satellites[i].snr = (int) satellite.getSnr();
					if (satellite.usedInFix()) {
						i++;
					}
				}
			}
			numSatellites = i;
			if (numSatellites > 0) {
				receiverList.receiveSatellites(satellites);
			}
		}
		//updateSolution(state);
	}

	public void onProviderDisabled(String provider) {
		hasFix = false;
		updateSolution(LocationProvider.OUT_OF_SERVICE);
	}

	public void onProviderEnabled(String provider) {
		updateSolution(LocationProvider.AVAILABLE);
	}

	public void onStatusChanged(String provider, int state, Bundle b) {
		//#debug info
		logger.info("onStatusChanged(" + provider + "," + state + ")");
		lmState = state;
		updateSolution(state);
	}

	public void onLocationChanged(Location loc) {
		locationUpdated(locationManager, loc, false);
	}

	public void triggerLastKnownPositionUpdate() {
		locationUpdated(locationManager, locationManager.getLastKnownLocation(provider), true);
	}

	public void disableRawLogging() {
		if (rawDataLogger != null) {
			try {
				rawDataLogger.close();
			} catch (IOException e) {
				logger.exception(Locale.get("androidlocationinput.CouldntCloseRawGpsLogger")/*Couldnt close raw gps logger*/, e);
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
