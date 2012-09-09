/*
 * ShareNav - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
 *          Copyright (c) 2011,2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net 
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

//#if polish.api.locationapi
import javax.microedition.location.LocationException;
import javax.microedition.location.Orientation;
//#endif

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

import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.IntTree;
import net.sharenav.util.Logger;
import net.sharenav.util.StringTokenizer;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Position;
import net.sharenav.sharenav.ui.ShareNav;

import de.enough.polish.util.Locale;

/**
 * Compass implementation
 * 
 */
public class GetCompass implements CompassProducer {
	private static final int COMPASSMETHOD_NONE = 0;
	private static final int COMPASSMETHOD_SE = 1;
	private static final int COMPASSMETHOD_S60FP5 = 2;
	private static final int COMPASSMETHOD_MOTO = 3;
	private static final int COMPASSMETHOD_SOCKET = 4;
	private static final int COMPASSMETHOD_DEBUG = 5;
	private static final int COMPASSMETHOD_ANDROID = 6;
	private static final int COMPASSMETHOD_ORIENTATION = 7;
	
	private static GetCompass singelton;
	
	private static final Logger logger = Logger.getInstance(GetCompass.class,
			Logger.TRACE);

	protected Thread processorThread;
	protected CompassReceiverList receiverList;
	protected boolean closed = false;
	private String message;
	private RetrievePosition rp;

//#if polish.android
	boolean sensorrunning = false;
//#endif

	public boolean needsPolling = true;

	private int compassRetrievelMethod = -1;
	
	volatile float direction = 0.0f;
	static boolean inited = false;

	Compass cachedCompass = null;
	
	public void triggerLastKnownPositionUpdate() {
	}

	public void triggerPositionUpdate() {
	}

	public class RetrievePosition extends TimerTask {
		
		public void run() {
			Compass compass = null;
			try {
				if (closed) {
					this.cancel();
					return;
				}

				compass = obtainCurrentCompass();
				if (compass == null) {
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

	public GetCompass() {
		this.receiverList = new CompassReceiverList();
	}

	public void getCompassMethod() {
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
			logger.info("Trying to see if Orientation method is available");
			Compass compass = obtainOrientationCompass();
			if (compass != null) {
				compassRetrievelMethod = COMPASSMETHOD_ORIENTATION;
				//#debug info
				logger.info("   Yes, the JSR179 Orientation class method works");
				return;
			} else {
				//#debug info
				logger.info("   No, need to use a different method");
			}
		} catch (Exception e) {
			logger.silentexception("Retrieving Compass with JSR179 Orientation class failed", e);
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
		logger.error(Locale.get("compassprovider.NoMethodForCompass")/*No method of retrieving Compass is valid, can not use Compass*/);
	}

	public boolean init(CompassReceiver receiver) {
		getCompassMethod();
		try {
			this.receiverList.addReceiver(receiver);
			
			if (obtainCurrentCompass() == null) {
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
		Compass compass = null;
		try {
			compass = obtainCurrentCompass();
		} catch (Exception e) {
			logger.silentexception("Could not retrieve compass direction", e);
		}
		if (compass == null) {
			//#debug debug
			logger.debug("No compass direction available");
			receiverList.receiveCompassStatus(0);
			return false;
		}
		receiverList.receiveCompassStatus(1);
		receiverList.receiveCompass(compass.direction);
		if (needsPolling) {
			rp = new RetrievePosition();
			ShareNav.getTimer().schedule(rp, 250, 250);
		}
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
	
	public synchronized static GetCompass getInstance() {
		if (singelton == null) {
			singelton = new GetCompass();
		}
		return singelton;
	}
	
	//#if polish.android
	private Compass obtainAndroidCompass() {
		Compass compass = new Compass();
		
		SensorManager mySensorManager = (SensorManager)MidletBridge.instance.getSystemService(Context.SENSOR_SERVICE);

		List<Sensor> mySensors = mySensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
  
		if (mySensors.size() > 0) {
			mySensorManager.registerListener(mySensorEventListener, mySensors.get(0), SensorManager.SENSOR_DELAY_NORMAL);
			sensorrunning = true;
			needsPolling = false;
		} else {
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
			Compass compass = new Compass();
			compass.direction = event.values[0];
			receiverList.receiveCompass(compass.direction);
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
	
	private Compass obtainOrientationCompass() {
		Compass compass = new Compass();
		
		//#if polish.api.locationapi
		Orientation orientation;
		float azimuth;

		try {
			Class.forName("javax.microedition.location.Orientation");
			orientation = Orientation.getOrientation();
			compass.direction = orientation.getCompassAzimuth();
			return compass;
		} catch (NullPointerException np) {
			// Calibration needed
			// FIXME signal this to the user, tell them to calibrate & try
			// again
		} catch (NoClassDefFoundError ncdfe) {
			//#debug info
			logger.info("JSR179 Orientation class for compass is not available");
                } catch (LocationException le) {
			//#debug info
			logger.info("JSR179 Orientation class for compass is not available");
		} catch (Exception e) {
			//#debug info
			logger.info("JSR179 Orientation class for compass is not available");
		}
		//#endif
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
		if (compassRetrievelMethod == COMPASSMETHOD_ORIENTATION) {
			cachedCompass = obtainOrientationCompass();
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
