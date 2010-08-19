/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
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
 */
package de.ueller.midlet.gps;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

//#if polish.api.fileconnection
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif

//#if polish.api.nokia-ui
import com.nokia.mid.ui.DeviceControl;
//#endif

//#if polish.api.contenthandler
import de.ueller.midlet.gps.importexport.Jsr211ContentHandlerInterface;
//#endif

//#if polish.api.min-siemapi
import de.ueller.midlet.gps.importexport.SiemGameLight;
//#endif

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.data.Node;

import de.enough.polish.util.Locale;

//#if polish.android
import de.enough.polish.android.midlet.MidletBridge;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.Context;
//#endif

/**
 * Central class of GpsMid which implements the MIDlet interface.
 */
public class GpsMid extends MIDlet implements CommandListener {
	/** Class variable with the Singleton reference. */
	private volatile static GpsMid instance;
	/** A menu list instance */
	private static final String[] elements = { "Map", "Search", "Setup",
			"About", "Log" };

	/** Soft button for exiting GpsMid. */
	private final Command EXIT_CMD = new Command("Exit", Command.EXIT, 2);

	/** Soft button for launching a client or server. */
	private final Command OK_CMD = new Command("Ok", Command.SCREEN, 1);
	/** Soft button to go back from about screen. */
	private final Command BACK_CMD = new Command("Back", Command.BACK, 1);
	/** Soft button to show Debug Log. */
	// private final Command DEBUG_CMD = new Command("", Command.BACK, 1);
	/** Soft button to go back from about screen. */
	private final Command CLEAR_DEBUG_CMD = new Command("Clear", Command.BACK,
			1);

	/** A menu list instance */
	private final List menu = new List("GPSMid", Choice.IMPLICIT, elements,
			null);
	// private boolean isInit=false;

	private final List loghist = new List("Log Hist", Choice.IMPLICIT);

	// #debug
	private Logger log;

	private OutputStreamWriter logFile;

	public static NoiseMaker mNoiseMaker = null;

	public static Legend legend = null;

	/**
	 * This Thread is used to periodically prod the display to keep the
	 * backlight illuminator if this is wanted by the user
	 */
	private Thread lightTimer;

	private Displayable shouldBeDisplaying;
	private Displayable prevDisplayable;

	/**
	 * Runtime detected properties of the phone
	 */
	private long phoneMaxMemory;

	private static volatile Trace trace = null;

	public static String errorMsg = null;

//#if polish.android
	//public static MidletBridge midletBridge;
	private PowerManager.WakeLock wl = null;
	private PowerManager pm = null;
//#endif
	public GpsMid() {
		instance = this;
		System.out.println("Init GpsMid");		
		log = new Logger(this);
		log.setLevel(Logger.INFO);
		Configuration.read();
		
//#if polish.android
		// midletBridge = instance._getMidletBridge();
		// on android, switch backlight always on to keep in foreground
//		midletBridge.backlightOn();
//		midletBridge.setKeepScreenOn(true);
//		instance.setSystemProperty("keepScreenOn", "true");
//		instance.backlightOn();
//		midletBridge.setSystemProperty("keepScreenOn", "true");
//		midletBridge.showSoftKeyboard();
//#endif

		enableDebugFileLogging();
		Logger.setGlobalLevel();

		//#debug info
		log.debug("Phone Model: " + Configuration.getPhoneModel());
		
		mNoiseMaker = new NoiseMaker();

		// read in legend.dat to have i.e. bundle date already accessable from
		// the splash screen
		try {
			legend = new Legend();
		} catch (Exception e) {
			e.printStackTrace();
			errorMsg = "Failed to load basic configuration! Check your map data source: "
					+ e.getMessage();
//#if polish.android
			errorMsg += " - For Android, you must manually install the map (e.g. unzip the J2ME map jar bundle with same settings & version as the .apk) on the SD card for now";
//#endif
			legend = null;
		}

		if (legend != null) {
			int langNum = 0;  // default is the first in bundle
			if (Legend.numUiLang > 1) {
				langNum = 0; 
				String lang = Configuration.getUiLang();
				for (int i = 0; i < Legend.numUiLang; i++) {
					if (Legend.uiLang[i].equalsIgnoreCase(lang)) {
						langNum = i;
					}
				}
			}
			try {
				Locale.loadTranslations( "/" + Legend.uiLang[langNum] + ".loc" );
			} catch (IOException ioe) {
				System.out.println("Couldn't open translations file");
			}
		}

		phoneMaxMemory = determinPhoneMaxMemory();		
		
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		loghist.addCommand(BACK_CMD);
		loghist.addCommand(CLEAR_DEBUG_CMD);
		loghist.setCommandListener(this);

		if (errorMsg != null) {
			log.fatal(errorMsg);
		}
	}

	public static void showMapScreen() {
		if (trace == null) {
			trace = Trace.getInstance();
		}
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_INITIAL_SETUP_DONE)) {
			if (isRunningInMicroEmulator()) {
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_FULLSCREEN, true);				
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS, true);				
			} else {
				GuiSetupGui gsg = new GuiSetupGui(trace, true);
				gsg.show();							
			}
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_INITIAL_SETUP_DONE, true);
		} else {
			trace.show();
		}
	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		if (trace != null) {
			trace.shutdown();
			// remember last position
			if (Configuration
					.getCfgBitState(Configuration.CFGBIT_AUTOSAVE_MAPPOS)) {
				// use current display center on next startup
				Configuration.setStartupPos(trace.center);
			} else {
				// use center of map on next startup
				Configuration.setStartupPos(new Node(0.0f, 0.0f));
			}
		}
		if (logFile != null) {
			//#debug info
			log.info("Shutting down logfile");
			try {
				logFile.flush();
				logFile.close();
			} catch (IOException ioe) {
				System.out.println("Couldn't close log file");
			}
			logFile = null;
		}
		//#debug
		System.out.println("destroy GpsMid");
	}

	protected void pauseApp() {
		//#debug
		System.out.println("Pause GpsMid");
//#ifndef polish.android
		// FIXME we should save state here to prepare for Android killing us
		if (trace != null) {
			trace.pause();
		}
//#endif
	}

	protected void startApp() throws MIDletStateChangeException {
		//#debug
		System.out.println("Start GpsMid");
//		if (trace != null) {
//			trace.resume();
//		}

		//#if polish.api.contenthandler
		try {
			log.info("Trying to register JSR211 Content Handler");
			Class handlerClass = Class
					.forName("de.ueller.midlet.gps.importexport.Jsr211Impl");
			Object handlerObject = handlerClass.newInstance();
			Jsr211ContentHandlerInterface handler = (Jsr211ContentHandlerInterface) handlerObject;
			handler.registerContentHandler();
		} catch (NoClassDefFoundError ncdfe) {
			log.error("JSR211 is not available", true);
		} catch (ClassNotFoundException cnfe) {
			log.error("JSR211 is not available", true);
		} catch (InstantiationException e) {
			log.exception("ContentHandler invokation failed", e);
		} catch (IllegalAccessException e) {
			log.exception("ContentHandler invokation failed", e);
		}
		//#endif

		if (Configuration.getCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN) && legend != null) {
			showMapScreen();
		} else {
			new Splash(this);
		}

		// RouteNodeTools.initRecordStore();
		startBackLightTimer();
	}

	public void commandAction(Command c, Displayable d) {
		if (c == EXIT_CMD) {
			exit();
			return;
		}
		if (c == BACK_CMD) {
			show();
			return;
		}
		if (c == CLEAR_DEBUG_CMD) {
			loghist.deleteAll();
		}
		switch (menu.getSelectedIndex()) {
		case 0:
			try {
				if (trace == null) {
					trace = Trace.getInstance();
				}
				trace.resume();
				trace.show();
			} catch (Exception e) {
				log.exception("Failed to display map ", e);
				return;
			}
			break;
		case 1:
			try {
				if (trace == null) {
					trace = Trace.getInstance();
				}
				GuiSearch search = new GuiSearch(trace);
				search.show();
			} catch (Exception e) {
				log.exception("Failed to display search screen ", e);
			}
			break;
		case 2:
			new GuiDiscover(this);
			break;
		case 3:
			new Splash(this);
			break;
		case 4:
			show(loghist);
			break;
		default:
			//#debug
			System.err.println("Unexpected choice...");

			break;
		}

		// isInit = true;

	}

	public void restart() {
		if (trace != null) {
			trace.pause();
			trace = null;
			System.gc();
			show();
		}
	}

	public void exit() {
		try {
//#if polish.android
			if (Configuration
			    .getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WAKELOCK)) {
				stopBackLightTimer();
			}
//#endif
			destroyApp(true);
		} catch (MIDletStateChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		notifyDestroyed();
	}

	/** Shows main menu of MIDlet on the screen. */
	public void show() {
		show(menu);
	}

	public void alert(String title, String message, int timeout) {
		//#debug info
		log.info("Showing Alert: " + message);
		if ((trace != null) && (trace.isShown()) && (timeout != Alert.FOREVER)) {
			trace.alert(title, message, timeout);
		} else {
			Alert alert = new Alert(title);
			alert.setTimeout(timeout);
			alert.setString(message);
			try {
				if (shouldBeDisplaying == null)
					Display.getDisplay(this).setCurrent(alert);
				else
					Display.getDisplay(this).setCurrent(alert,
							shouldBeDisplaying);
			} catch (IllegalArgumentException iae) {
				/**
				 * Nokia S40 phones seem to throw an exception if one tries to
				 * set an Alert displayable when the current displayable is an
				 * alert too.
				 * 
				 * Not much we can do about this, other than just ignore the
				 * exception and not display the new alert.
				 */
				log.info("Could not display this alert (" + message + "), "
						+ iae.getMessage());
			}
		}
	}

	public void showPreviousDisplayable() {
		show(prevDisplayable);
	}

	public void show(Displayable d) {
		try {
			prevDisplayable = shouldBeDisplaying;
			Display.getDisplay(this).setCurrent(d);
		} catch (IllegalArgumentException iae) {
			log.info("Could not display the new displayable " + d + ", "
					+ iae.getMessage());
		}
		/**
		 * Keep track of what the Midlet should be displaying. This is
		 * necessary, as a call to getDisplay following a setDisplay does not
		 * necessarily return the display just set, as the display actually gets
		 * set asynchronously. There also doesn't seem to be a way to find out
		 * what will be displayed next, or "serialise" on the setDisplay call.
		 * 
		 * This can cause problems, if an Alert is set just after a call to
		 * setDisplay. The call uses getDisplay to determine what to show after
		 * the Alert is dismissed, but the setDisplay might not have had an
		 * effect yet. Hence, keep manually track.
		 */
		shouldBeDisplaying = d;
	}

	public Displayable shouldBeShown() {
		return shouldBeDisplaying;
	}

	public void log(String msg) {
		if (log != null) {
			//#debug
			System.out.println(msg);
			/**
			 * Adding the log hist seems to cause very weird problems even in
			 * the emulator. So leave this commented out
			 */
			// loghist.append(msg, null);
			if (logFile != null) {
				try {
					logFile
							.write(System.currentTimeMillis() + " " + msg
									+ "\n");
					logFile.flush();
				} catch (IOException e) {
					// Nothing much we can do here, we are
					// already in the debugging routines.
					System.out.println("Failed to write to the log file: "
							+ msg + " with error: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	public static GpsMid getInstance() {
		return instance;
	}

	public void enableDebugFileLogging() {
		//#if polish.api.fileconnection
		String url = Configuration.getDebugRawLoggerUrl();
		if (Configuration.getDebugRawLoggerEnable() && url != null) {
			try {
				if (logFile != null) {
					logFile.close();
					logFile = null;
				}
				url = url + "GpsMid_log_"
						+ HelperRoutines.formatSimpleDateNow() + ".txt";
				Connection debugLogConn = Connector.open(url);
				if (debugLogConn instanceof FileConnection) {
					if (!((FileConnection) debugLogConn).exists()) {
						((FileConnection) debugLogConn).create();
					}
					logFile = new OutputStreamWriter(
							((FileConnection) debugLogConn).openOutputStream());
				}
			} catch (IOException e) {
				log.exception("Couldn't connect to the debug log file", e);
				e.printStackTrace();
			} catch (SecurityException se) {
				/**
				 * we were denied access to the log file, nothing we can do
				 * about this.
				 */
				logFile = null;
			}
		} else {
			if (logFile != null) {
				try {
					logFile.close();
				} catch (IOException ioe) {
					/*
					 * We were closing anyway, so there is not much to do in
					 * case of error
					 */
				}
				logFile = null;
			}
		}
		//#endif
	}

	public void startBackLightTimer() {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON)) {
			// Warn the user if none of the methods
			// to keep backlight on was selected
			if (!(Configuration
					.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2)
					|| Configuration
							.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA)
					|| Configuration
							.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH) || Configuration
					.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_SIEMENS) || Configuration
					.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WAKELOCK))) {
				log.error("Backlight cannot be kept on when no 'with'-method is specified in Setup");
			}
			if (lightTimer == null) {
				lightTimer = new Thread(new Runnable() {
					private final Logger logger = Logger.getInstance(
							GpsMid.class, Logger.DEBUG);

					public void run() {
						Trace trace = Trace.getInstance();
						try {
							boolean notInterupted = true;
							while (notInterupted) {
								if (trace != null
									// only when GPS is connected or option "only with GPS" is off
									&& (trace.isGpsConnected() || TrackPlayer.isPlaying
											|| ! Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED))
								) {
									// Method to keep the backlight on
									// some MIDP2 phones
									if (Configuration
											.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2)) {
										Display.getDisplay(
											GpsMid.getInstance()).flashBacklight(1000);
										//#if polish.api.nokia-ui
										// Method to keep the backlight on
										// on SE K750i and some other models
									} else if (Configuration
											.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH)) {
										DeviceControl.flashLights(1);
										// Method to keep the backlight on
										// on those phones that support the
										// nokia-ui
									} else if (Configuration
											.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA)) {
										DeviceControl.setLights(0, Configuration.getBackLightLevel());
										//#endif
										//#if polish.android
										// Method to keep the backlight on
										// with Android WakeLock
									} else if (Configuration
											.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WAKELOCK)) {
										if (pm == null) {
											pm = (PowerManager) MidletBridge.instance.getSystemService(Context.POWER_SERVICE);
										}
										if (wl == null) {
											wl = pm.newWakeLock(
												(Configuration.getBackLightLevel() <= 50) ?
												PowerManager.SCREEN_DIM_WAKE_LOCK : PowerManager.FULL_WAKE_LOCK,
												"GpsMid");
											wl.acquire();
										}
										// Method to keep the backlight on
										// on those phones that support the
										//#endif
										//#if polish.api.min-siemapi
									} else if (Configuration
											.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_SIEMENS)) {
										try {
											Class.forName("com.siemens.mp.game.Light");
											SiemGameLight.SwitchOn();
										} catch (Exception e) {
											log.exception("Siemens API error: ", e);
										}
										//#endif
									}
								}
								try {
									synchronized (this) {
										if (Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ONLY_KEEPALIVE)) {
											wait(60000);
										} else {
											// if MIDP 2.0 method is used, wait only 500 ms instead of 5000 ms
											wait(Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2) ? 500 : 5000);
										}
									}
								} catch (InterruptedException e) {
									notInterupted = false;
								}
							}
						} catch (RuntimeException rte) {
							// Backlight prodding sometimes fails when
							// minimizing the
							// application. Don't display an alert because of
							// this
							//#debug info
							logger.info("Backlight prodding failed: "
									+ rte.getMessage());
						} catch (NoClassDefFoundError ncdfe) {
							logger.error("Backlight prodding failed, API not supported: "
									+ ncdfe.getMessage());
						}
					} // run()
				} );
				lightTimer.setPriority(Thread.MIN_PRIORITY);
				lightTimer.start();
			}
		}
	}


	public void showBackLightLevel() {
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON,
				false) ) {
			trace.alert("Backlight", "Backlight "
					+ (Configuration.getBackLightLevel() == 100 ? "ON" : (Configuration.getBackLightLevel() + "%"))
					+ (Configuration.getCfgBitState(
							Configuration.CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED)
								? " while GPS started" : ""), 1000);
		} else {
			trace.alert("Backlight", "Backlight off", 1000);
		}
		stopBackLightTimer();
		startBackLightTimer();
	}

	public void stopBackLightTimer() {
//#if polish.android
		if (wl != null) {
			wl.release();
			wl = null;
		}
//#endif
		if (lightTimer != null) {
			lightTimer.interrupt();
			try {
				lightTimer.join();
			} catch (Exception e) {

			}
			lightTimer = null;
		}
	}

	public void restartBackLightTimer() {
		stopBackLightTimer();
		startBackLightTimer();
	}
	
	/**
	 * Try and determine the maximum available memory. As some phones have
	 * dynamic growing heaps, we need to try and cause a out of memory error, as
	 * that indicates the maximum size to which the heap can grow
	 * 
	 * @return maximum heap size
	 */
	private long determinPhoneMaxMemory() {
		long maxMem = Runtime.getRuntime().totalMemory();
		log.info("Maximum phone memory: " + maxMem);
		if (maxMem < Configuration.getPhoneAllTimeMaxMemory()) {
			maxMem = Configuration.getPhoneAllTimeMaxMemory();
			log.info("Using all time maximum phone memory from Configuration: " + maxMem);
		}
		return maxMem;
	}

	public long getPhoneMaxMemory() {
		return phoneMaxMemory;
	}

	
	
	public boolean needsFreeingMemory() {
		Runtime runt = Runtime.getRuntime();
		long totalMem = runt.totalMemory();
		if (totalMem > phoneMaxMemory) {
			phoneMaxMemory = totalMem;
			if (phoneMaxMemory > Configuration.getPhoneAllTimeMaxMemory() + 100000) {
				if (trace != null) {
					trace.receiveMessage("Inc maxMem: " + phoneMaxMemory);
				}
			}
			if (phoneMaxMemory > Configuration.getPhoneAllTimeMaxMemory()) {
				Configuration.setPhoneAllTimeMaxMemory(phoneMaxMemory);
			}
			log.info("New phoneMaxMemory: " + phoneMaxMemory);
		}
		long freeMem = 0;
		for (int i=0; i < 3; i++) {
			if ( phoneMaxMemory > totalMem ) {
				// Phone with increasing heap
				freeMem = phoneMaxMemory - totalMem + runt.freeMemory();;
			} else {
				// Phone with fixed heap
				freeMem = runt.freeMemory();
			}

			if ((freeMem < 30000)
				|| (((float) freeMem / (float) totalMem) < 0.10f)) {
				switch(i) {
					case 0:
						//#debug trace
						log.trace("Memory is low, trying GC");
						break;
					case 1:
						//#debug trace
						log.trace("Memory is low, freeing traceIconMenu");
						Trace.uncacheIconMenu();
						break;
					case 2:
						//#debug trace
						log.trace("Memory is low, need freeing " + freeMem);
						if (trace != null) {
							trace.receiveMessage("Freeing mem");
						}
						return true;						
				}
				System.gc();
			}
		}
		//#debug trace
		log.trace("Enough memory");
		return false;
	}
	
	public final static boolean isRunningInMicroEmulator() {
		String microEmu = getInstance().getAppProperty("microedition.platform");
		if (microEmu == null || !microEmu.equalsIgnoreCase("MicroEmulator") ) {
			return false;
		}
		return true;
	}
	
}
