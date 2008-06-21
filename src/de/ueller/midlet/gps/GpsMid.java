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
import java.io.OutputStream;
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

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HelperRoutines;




public class GpsMid extends MIDlet implements CommandListener{
	/** */
	private static GpsMid instance;
    /** A menu list instance */
    private static final String[] elements = { "Map","Search","Setup","About","Log"};

    /** Soft button for exiting GpsMid. */
    private final Command EXIT_CMD = new Command("Exit", Command.EXIT, 2);

    /** Soft button for launching a client or sever. */
    private final Command OK_CMD = new Command("Ok", Command.SCREEN, 1);
    /** Soft button to go back from about screen. */
    private final Command BACK_CMD = new Command("Back", Command.BACK, 1);
    /** Soft button to show Debug Log. */
 //   private final Command DEBUG_CMD = new Command("", Command.BACK, 1);
    /** Soft button to go back from about screen. */
    private final Command CLEAR_DEBUG_CMD = new Command("Clear", Command.BACK, 1);

    /** A menu list instance */
    private final List menu = new List("GPSMid", Choice.IMPLICIT, elements, null);
//	private boolean	isInit=false;

    private final List loghist=new List("Log Hist",Choice.IMPLICIT);
	private String	root;
	private Configuration config;
//	#debug
	private Logger l;
	
	private OutputStreamWriter logFile;
	
	/**
	 * This Thread is used to periodically prod the display
	 * to keep the backlight illuminator if this is wanted
	 * by the user
	 */
	private Thread lightTimer;
	
	private Displayable shouldBeDisplaying;
	
	/**
	 * Runtime detected properties of the phone
	 */
	private long phoneMaxMemory;

private Trace trace=null;


	public GpsMid() {		
		instance = this;
		System.out.println("Init GpsMid");		
		l=new Logger(this);
		l.setLevel(Logger.INFO);
		config = new Configuration();
		
		enableDebugFileLogging();
		Logger.setGlobalLevel();
		
		phoneMaxMemory = determinPhoneMaxMemory();
		
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		loghist.addCommand(BACK_CMD);
		loghist.addCommand(CLEAR_DEBUG_CMD);
		loghist.setCommandListener(this);
		
//		
		new Splash(this);
//		RouteNodeTools.initRecordStore();
		startBackLightTimer();
	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
//		#debug
		System.out.println("destroy GpsMid");
	}

	protected void pauseApp() {
//		#debug
		System.out.println("Pause GpsMid");
		if (trace != null){
			trace.pause();
		}
	// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
//		#debug
		System.out.println("Start GpsMid");
		if (trace == null){
			try {
				trace = new Trace(this,config);
//				trace.show();
			} catch (Exception e) {
				trace=null;
				e.printStackTrace();
			}
			} else {
				trace.resume();
//				trace.show();
		}
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
        if (c == CLEAR_DEBUG_CMD){
        	loghist.deleteAll();
        }
        switch (menu.getSelectedIndex()) {
            case 0:
            	try {
            		if (trace == null){
            			trace = new Trace(this,config);
            			trace.show();
            		} else {
            			trace.resume();
            			trace.show();
            		}
				} catch (Exception e) {
					l.exception("Failed to display map " , e);
            		return;
				} 
                break;
            case 1:
        		try {
					if (trace == null){
						trace = new Trace(this,config);
					}
					GuiSearch search = new GuiSearch(trace);
					search.show();
				} catch (Exception e) {
					l.exception("Failed to display search screen " , e);
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
//            	#debug
                System.err.println("Unexpected choice...");

                break;
            }

//            isInit = true;


		
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
    void show() {
        show(menu);
    }
    
    public void alert(String title, String message, int timeout) {
    	Alert alert = new Alert(title);
		alert.setTimeout(timeout);
		alert.setString(message);
		//#debug info
		l.info("Showing Alert: " + message);
		try {
			Display.getDisplay(this).setCurrent(alert, shouldBeDisplaying);
		} catch (IllegalArgumentException iae) {
			/**
    		 * Nokia S40 phones seem to throw an exception
    		 * if one tries to set an Alert displayable when
    		 * the current displayable is an alert too.
    		 * 
    		 * Not much we can do about this, other than just
    		 * ignore the exception and not display the new
    		 * alert. 
    		 */
    		l.info("Could not display this alert (" + message + "), " + iae.getMessage());			
		}
    }
    
    public void show(Displayable d) {
    	try{
    		Display.getDisplay(this).setCurrent(d);
    	} catch (IllegalArgumentException iae) {    		
    		l.info("Could not display the new displayable " + d + ", " + iae.getMessage());
    	}
		 /**
		  * Keep track of what the Midlet should be displaying.
		  * This is necessary, as a call to getDisplay following
		  * a setDisplay does not necessarily return the display just
		  * set, as the display actually gets set asynchronously.
		  * There also doesn't seem to be a way to find out what will
		  * be displayed next, or "serialise" on the setDisplay call.
		  * 
		  * This can cause problems, if an Alert is set just after
		  * a call to setDisplay. The call uses getDisplay to determine what
		  * to show after the Alert is dismissed, but the setDisplay might
		  * not have had an effect yet. Hence, keep manually track.
		  */
		 shouldBeDisplaying = d;		 
	}
    
    public Displayable shouldBeShown() {
    	return shouldBeDisplaying;
    }


	public void log(String msg){
		if (l != null){
			//#debug
			System.out.println(msg);
			/**
			 * Adding the log hist seems to cause very wierd problems
			 * even in the emulator. So leave this commented out
			 */
			//loghist.append(msg, null);
			
			if (logFile != null) {
				try {
					logFile.write(System.currentTimeMillis() + " " + msg + "\n");					
				} catch (IOException e) {
					//Nothing much we can do here, we are
					//already in the debugging routines.
					System.out.println("Failed to write to the log file: " + msg + " with error: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}

	public Configuration getConfig() {
		return config;
	}

	public static GpsMid getInstance() {
		return instance;
	}
	
	public void enableDebugFileLogging() {
		//#if polish.api.fileconnection
		String url = config.getDebugRawLoggerUrl();
		if (config.getDebugRawLoggerEnable() && url != null) {
			try {
				url = url + "GpsMid_log_" + HelperRoutines.formatSimpleDateNow() + ".txt";
				Connection debugLogConn = Connector.open(url);
				if (debugLogConn instanceof FileConnection) {
					if (!((FileConnection)debugLogConn).exists()) {
						((FileConnection)debugLogConn).create();
					}
					logFile = new OutputStreamWriter(((FileConnection)debugLogConn).openOutputStream());
				}				
			} catch (IOException e) {
				l.exception("Couldn't connect to the debug log file", e);
				e.printStackTrace();
			} catch (SecurityException se) {
				/**
				 * we were denied access to the log file, nothing we can do about this.
				 */
				logFile = null;
			}
		} else {
			if (logFile != null) {
				try {
					logFile.close();
				} catch (IOException ioe) {
					/*
					 * We were closing anyway, so there is not much to do
					 * in case of error
					 */
				}
				logFile = null;
			}			
		}
		//#endif
	}
	
	public void startBackLightTimer() {		
		if (config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON) ) {
			// Warn the user if none of the methods
			// to keep backlight on was selected
			if( ! (config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2) || 
				   config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA) ||
				   config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH)
				  )
			) {				
				l.error("Backlight cannot be kept on when no 'with'-method is specified in Setup");
			}
			if (lightTimer == null) {
				lightTimer = new Thread(new Runnable() {
					private final Logger logger=Logger.getInstance(GpsMid.class,Logger.DEBUG);
					public void run() {
						try {
							boolean notInterupted = true;
							while(notInterupted) {							
								// only when map is displayed or
								// option "only when map is displayed" is off 
								if ( (Trace.getInstance()!=null && Trace.getInstance().isShown())
									|| !config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MAPONLY)
								) {
									//Method to keep the backlight on
									//some MIDP2 phones
									if (config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2) ) {
										Display.getDisplay(GpsMid.getInstance()).flashBacklight(6000);						
									//#if polish.api.nokia-ui
									//Method to keep the backlight on
									//on SE K750i and some other models
									} else if (config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH) ) {  
										DeviceControl.flashLights(1);								
									//Method to keep the backlight on
									//on those phones that support the nokia-ui 
									} else if (config.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA) ) {  
										DeviceControl.setLights(0, 100);
									//#endif		
									}
								}
								try {
									synchronized(this) {
										wait(5000);
									}
								} catch (InterruptedException e) {
									notInterupted = false;
								}
							}
						} catch (RuntimeException rte) {
							// Backlight prodding sometimes fails when minimizing the
							// application. Don't display an alert because of this
							//#debug info
							logger.info("Blacklight prodding failed: " + rte.getMessage());
						} catch (NoClassDefFoundError ncdfe) {
							logger.error("Blacklight prodding failed, API not supported: " + ncdfe.getMessage());
						}
					}
				});
				lightTimer.setPriority(Thread.MIN_PRIORITY);
				lightTimer.start();
			}
		}
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
	 * Try and determine the maximum available memory.
	 * As some phones have dynamic growing heaps, we need
	 * to try and cause a out of memory error, as that
	 * indicates the maximum size to which the heap can
	 * grow 
	 * @return maximum heap size
	 */
	private long determinPhoneMaxMemory() {
		long maxMem = Runtime.getRuntime().totalMemory();
		int [][] buf = new int[2048][];
		try {			
			for (int i = 0; i < 2048; i++) {
				buf[i] = new int[16000]; 
			}
		} catch (OutOfMemoryError oome) {
			//l.info("Hit out of memory while determining maximum heap size");
			maxMem = Runtime.getRuntime().totalMemory();
		} finally {
			for (int i = 0; i < 2048; i++) {
				buf[i] = null; 
			}
		}
		System.gc();
		l.info("Maximum phone memory: " + maxMem);
		return maxMem;
	}
	
	public long getPhoneMaxMemory () {
		return phoneMaxMemory;
	}
	
	public boolean needsFreeingMemory() {
		Runtime runt = Runtime.getRuntime();
		long freeMem = runt.freeMemory();
		long totalMem = runt.totalMemory();
		if ((freeMem < 30000) || ((totalMem >= phoneMaxMemory) && ((float)freeMem/(float)totalMem < 0.10f))) {
			l.trace("Memory is low, need freeing " + freeMem);
			return true;
		} else {
			l.trace("Enough memory, no need to cleanup");
			return false;
		}
	}
}

