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

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.CellIdProvider;
import de.ueller.midlet.gps.data.SocketGateway;

//#if polish.android
import java.util.List;
import android.content.Context;
import de.enough.polish.android.midlet.MidletBridge;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorListener;
//#endif


public class CompassProvider {
	private static final int COMPASSMETHOD_NONE = 0;
	private static final int COMPASSMETHOD_SE = 1;
	private static final int COMPASSMETHOD_S60FP5 = 2;
	private static final int COMPASSMETHOD_MOTO = 3;
	private static final int COMPASSMETHOD_SOCKET = 4;
	private static final int COMPASSMETHOD_DEBUG = 5;
	private static final int COMPASSMETHOD_ANDROID = 6;
	
	private static CompassProvider singelton;
	
	private static final Logger logger = Logger.getInstance(CompassProvider.class,
			Logger.TRACE);
	
//#if polish.android
	boolean sensorrunning = false;
//#endif

	private int compassRetrievelMethod = -1;
	
	volatile float direction = 0.0f;
	static boolean inited = false;

	Compass cachedCompass = null;
	
	private CompassProvider() {
		//#debug info
		logger.info("Trying to find a suitable compass id provider");
		//#if polish.android
		try {
			//#debug info
			logger.info("Trying to see if android method is available");
			Compass compass = obtainAndroidCompass();
			if (compass != null) {
				compassRetrievelMethod = COMPASSMETHOD_ANDROID;
				//#debug info
				logger.info("   Yes, the Android method works");
				return;
			} else {
				//#debug info
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving Compass as Android failed", e);
			//Nothing to do here, just fall through to the next method
		}
		//#endif
		try {
			//#debug info
			logger.info("Trying to see if Sony-Ericcson method is available");
			//#debug info
			logger.info("   No, need to use a different method");
		} catch (Exception e) {
			logger.silentexception("Retrieving Compass as a Sony-Ericsson failed", e);
			//Nothing to do here, just fall through to the next method
		}
		try {
			//#debug info
			logger.info("Trying to see if Motorola method is available");
			//#debug info
			logger.info("   No, need to use a different method");
		} catch (Exception e) {
			logger.silentexception("Retrieving Compass as a Motorola failed", e);
			//Nothing to do here, just fall through to the next method
		}
		try {
			//#debug info
			logger.info("Trying to see if there is a compassid server running on this device");
			Compass compass = obtainSocketCompass();
			// FIXME
			// compassRetrievelMethod = COMPASSMETHOD_SOCKET;
			if (compass != null) {
				compassRetrievelMethod = COMPASSMETHOD_SOCKET;
				logger.info("   Yes, there is a server running and we can get a compass from it");
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
			logger.info("Trying to see if S60 3rd FP5 method is available");
			Compass compass = obtainS60FP5Compass();
			if (compass != null) {
				compassRetrievelMethod = COMPASSMETHOD_S60FP5;
				logger.info("   Yes, the S60 3rd FP5 method works");
				return;
			} else {
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving Compass as a Nokia S60 3rd FP2 failed", e);
		}
		compassRetrievelMethod = COMPASSMETHOD_NONE;
		//#debug info
		logger.error("No method of retrieving Compass is valid, can't use Compass");
		
	}
	
	public synchronized static CompassProvider getInstance() {
		if (singelton == null) {
			singelton = new CompassProvider();
		}
		return singelton;
	}
	
	//#if polish.android
	private Compass obtainAndroidCompass() {
		Compass compass = new Compass();
		
		SensorManager mySensorManager = (SensorManager)MidletBridge.instance.getSystemService(Context.SENSOR_SERVICE);

		List<Sensor> mySensors = mySensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
  
		if(mySensors.size() > 0){
		    mySensorManager.registerListener(mySensorEventListener, mySensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
		    sensorrunning = true;
		}
		else{
		    sensorrunning = false;
		}

		if (! inited) {
			// wait for compass
			try {
				Thread.sleep(200);
			} catch (Exception ex) {}
			inited = true;
		}			
		compass.direction = direction;
		return compass;
	}

	private SensorEventListener mySensorEventListener = new SensorEventListener(){

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// handle accuracy here

		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			// direction here
			direction = event.values[0];
		}
	};
        //#endif


	private Compass obtainSECompass() {
		Compass compass = new Compass();
		
		return null;
	}
	
	private Compass obtainS60FP5Compass() {
		Compass compass = new Compass();

		return null;
	}
	
	private Compass obtainMotoCompass() {
		Compass compass = new Compass();

		return null;
	}
	
	private Compass obtainSocketCompass() {
		int retval;
		retval = SocketGateway.getSocketData(SocketGateway.TYPE_COMPASS);
		if (retval == SocketGateway.RETURN_OK) {
			return SocketGateway.getCompass();
		}
		if (compassRetrievelMethod == COMPASSMETHOD_SOCKET && retval == SocketGateway.RETURN_IOE) {
			/*
			 * The local helper daemon seems to have died.
			 * No point in trying to continue trying,
			 * as otherwise we will get an exception every time
			 */
			//compassRetrievelMethod = COMPASSMETHOD_NONE;
			return null;
		}
		return null;
	}
	
	private Compass obtainDebugCompass() {
		/*
		 * This code is a stub for debugging compass data on the emulator
		 */
		Compass compass = new Compass();
		
		Random r = new Random();

		return null;
	}
	
	public Compass obtainCachedCompass() {
		return cachedCompass;
	}
	
	public Compass obtainCurrentCompass() throws Exception {
		
		//#debug info
		logger.info("Tring to retrieve compass-id");
		
		if (compassRetrievelMethod ==  COMPASSMETHOD_NONE) {
			//#debug info
			logger.info("Can't retrieve Compass, as there is no valid method available");
			return null;
		}

		if (compassRetrievelMethod == COMPASSMETHOD_SE) {
			cachedCompass =  obtainSECompass();
		}
		//#if polish.android
		if (compassRetrievelMethod == COMPASSMETHOD_ANDROID) {
			cachedCompass =  obtainAndroidCompass();
		}
		//#endif
		if (compassRetrievelMethod == COMPASSMETHOD_MOTO) {
			cachedCompass =  obtainMotoCompass();
		}
		if (compassRetrievelMethod == COMPASSMETHOD_S60FP5) {
			cachedCompass = obtainS60FP5Compass();
		}
		if (compassRetrievelMethod == COMPASSMETHOD_SOCKET) {
			cachedCompass = obtainSocketCompass();
		}
		if (compassRetrievelMethod == COMPASSMETHOD_DEBUG) {
			cachedCompass = obtainDebugCompass();
		}
		//#debug debug
		logger.debug("Retrieved " + cachedCompass);
		return cachedCompass;
	}

}
