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
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
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

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.tile.C;

public class GpsMid extends MIDlet implements CommandListener {
	/** */
	private static GpsMid instance;
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
	private String root;
	// #debug
	private Logger log;

	private OutputStreamWriter logFile;

	public static NoiseMaker mNoiseMaker = null;

	public static C c;

	/**
	 * This Thread is used to periodically prod the display to keep the
	 * backlight illuminator if this is wanted by the user
	 */
	private Thread lightTimer;
	private volatile int backLightLevel = 100;

	private Displayable shouldBeDisplaying;
	private Displayable prevDisplayable;

	/**
	 * Runtime detected properties of the phone
	 */
	private long phoneMaxMemory;

	private Trace trace = null;

	public GpsMid() {
		String errorMsg = null;
		instance = this;
		System.out.println("Init GpsMid");		
		log = new Logger(this);
		log.setLevel(Logger.INFO);
		Configuration.read();

		enableDebugFileLogging();
		Logger.setGlobalLevel();

		mNoiseMaker = new NoiseMaker();

		// read in legend.dat to have i.e. bundle date already accessable from
		// the splash screen
		try {
			c = new C();
		} catch (Exception e) {
			e.printStackTrace();
			errorMsg = "Failed to load basic configuration! Check your map data source: "
					+ e.getMessage();
		}

		phoneMaxMemory = determinPhoneMaxMemory();

		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		loghist.addCommand(BACK_CMD);
		loghist.addCommand(CLEAR_DEBUG_CMD);
		loghist.setCommandListener(this);

		//
		if (Configuration
				.getCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN)) {
			this.show();
		} else {
			new Splash(this);
		}

		// RouteNodeTools.initRecordStore();
		startBackLightTimer();
		if (errorMsg != null) {
			log.fatal(errorMsg);
		}
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		// remember last position
		if (trace != null) {
			if (Configuration
					.getCfgBitState(Configuration.CFGBIT_AUTOSAVE_MAPPOS)) {
				// use current display center on next startup
				Configuration.setStartupPos(trace.center);
			} else {
				// use center of map on next startup
				Configuration.setStartupPos(new Node(0.0f, 0.0f));
			}
		}
		//#debug
		System.out.println("destroy GpsMid");
	}

	protected void pauseApp() {
		//#debug
		System.out.println("Pause GpsMid");
		if (trace != null) {
			trace.pause();
		}
	}

	protected void startApp() throws MIDletStateChangeException {
		//#debug
		System.out.println("Start GpsMid");
		if (trace == null) {
			try {
				// trace = new Trace(this);
				// trace.show();
			} catch (Exception e) {
				trace = null;
				e.printStackTrace();
			}
		} else {
			trace.resume();
			// trace.show();
		}

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
					trace = new Trace(this);
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
					trace = new Trace(this);
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
					.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_SIEMENS))) {
				log.error("Backlight cannot be kept on when no 'with'-method is specified in Setup");
			}
			if (lightTimer == null) {
				lightTimer = new Thread(new Runnable() {
					private final Logger logger = Logger.getInstance(
							GpsMid.class, Logger.DEBUG);

					public void run() {
						try {
							boolean notInterupted = true;
							while (notInterupted) {
								// only when map is displayed or
								// option "only when map is displayed" is off
								if ((Trace.getInstance() != null && Trace
										.getInstance().isShown())
										|| !Configuration
												.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MAPONLY)) {
									// Method to keep the backlight on
									// some MIDP2 phones
									if (Configuration
											.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2)) {
										Display
												.getDisplay(
														GpsMid.getInstance())
												.flashBacklight(6000);
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
										DeviceControl.setLights(0,
												backLightLevel);
										//#endif
										//#if polish.api.min-siemapi
									} else if (Configuration
											.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_SIEMENS)) {
										try {
											Class
													.forName("com.siemens.mp.game.Light");
											SiemGameLight.SwitchOn();
										} catch (Exception e) {
											log.exception("Siemens API error: ",
													e);
										}
										//#endif
									}

								}
								try {
									synchronized (this) {
										wait(5000);
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
							logger
									.error("Backlight prodding failed, API not supported: "
											+ ncdfe.getMessage());
						}
					} // run()
				});
				lightTimer.setPriority(Thread.MIN_PRIORITY);
				lightTimer.start();
			}
		}
	}

	public void addToBackLightLevel(int diffBacklight) {
		backLightLevel += diffBacklight;
		if (backLightLevel > 100
				|| !Configuration
						.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA)) {
			backLightLevel = 100;
		}
		if (backLightLevel < 25) {
			backLightLevel = 25;
		}
	}

	public int getBackLightLevel() {
		return backLightLevel;
	}

	public void showBackLightLevel() {
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON, 
				false) ) {
			alert("Backlight", "Backlight " 
					+ (backLightLevel == 100 ? "ON" : (backLightLevel + "%")), 
					1000);
		} else {
			alert("Backlight", "Backlight off", 1000);
		}
		stopBackLightTimer();
		startBackLightTimer();
	}

	public void stopBackLightTimer() {
		if (lightTimer != null) {
			lightTimer.interrupt();
			try {
				lightTimer.join();
			} catch (Exception e) {

			}
			lightTimer = null;
		}
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
		// int [][] buf = new int[2048][];
		// try {
		// for (int i = 0; i < 2048; i++) {
		// buf[i] = new int[16000];
		// }
		// } catch (OutOfMemoryError oome) {
		// //l.info("Hit out of memory while determining maximum heap size");
		// maxMem = Runtime.getRuntime().totalMemory();
		// } finally {
		// for (int i = 0; i < 2048; i++) {
		// buf[i] = null;
		// }
		// }
		// System.gc();
		log.info("Maximum phone memory: " + maxMem);
		return maxMem;
	}

	public long getPhoneMaxMemory() {
		return phoneMaxMemory;
	}

	public boolean needsFreeingMemory() {
		Runtime runt = Runtime.getRuntime();
		long freeMem = runt.freeMemory();
		long totalMem = runt.totalMemory();
		if (totalMem > phoneMaxMemory) {
			phoneMaxMemory = totalMem;
			log.info("New phoneMaxMemory: " + phoneMaxMemory);
		}
		if ((freeMem < 30000)
				|| ((totalMem >= phoneMaxMemory) && ((float) freeMem
						/ (float) totalMem < 0.10f))) {
			log.trace("Memory is low, need freeing " + freeMem);
			return true;
		} else {
			log.trace("Enough memory, no need to cleanup");
			return false;
		}
	}
}
