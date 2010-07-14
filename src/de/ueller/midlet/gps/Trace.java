/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.midlet.gps;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.midlet.MIDlet;



import de.ueller.gps.SECellID;
import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;

import de.ueller.gps.nmea.NmeaInput;
import de.ueller.gps.sirf.SirfInput;
import de.ueller.gps.tools.DateTimeTools;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gpsMid.mapData.DictReader;
//#if polish.api.osm-editing
import de.ueller.gpsMid.GUIosmWayDisplay;
import de.ueller.midlet.gps.data.EditableWay;
//#endif
import de.ueller.gpsMid.CancelMonitorInterface;
import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueDictReader;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.CellIdProvider;
import de.ueller.midlet.gps.data.GSMCell;
import de.ueller.midlet.gps.data.Proj2D;
import de.ueller.midlet.gps.data.ProjFactory;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.data.RoutePositionMark;
import de.ueller.midlet.gps.data.SECellLocLogger;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.names.Names;
import de.ueller.midlet.gps.urls.Urls;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.routing.Routing;
import de.ueller.midlet.gps.GuiMapFeatures;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.GpsMidDisplayable;

/**
 * Implements the main "Map" screen which displays the map, offers track recording etc.
 * @author Harald Mueller
 * 
 */
public class Trace extends KeyCommandCanvas implements LocationMsgReceiver,
Runnable , GpsMidDisplayable, CompletionListener, IconActionPerformer {
	/** Soft button for exiting the map screen */
	protected static final int EXIT_CMD = 1;
	protected static final int CONNECT_GPS_CMD = 2;
	protected static final int DISCONNECT_GPS_CMD = 3;
	protected static final int START_RECORD_CMD = 4;
	protected static final int STOP_RECORD_CMD = 5;
	protected static final int MANAGE_TRACKS_CMD = 6;
	protected static final int SAVE_WAYP_CMD = 7;
	protected static final int ENTER_WAYP_CMD = 8;
	protected static final int MAN_WAYP_CMD = 9;
	protected static final int ROUTING_TOGGLE_CMD = 10;
	protected static final int CAMERA_CMD = 11;
	protected static final int CLEAR_DEST_CMD = 12;
	protected static final int SET_DEST_CMD = 13;
	protected static final int MAPFEATURES_CMD = 14;
	protected static final int RECORDINGS_CMD = 16;
	protected static final int ROUTINGS_CMD = 17;
	protected static final int OK_CMD =18;
	protected static final int BACK_CMD = 19;
	protected static final int ZOOM_IN_CMD = 20;
	protected static final int ZOOM_OUT_CMD = 21;
	protected static final int MANUAL_ROTATION_MODE_CMD = 22;
	protected static final int TOGGLE_OVERLAY_CMD = 23;
	protected static final int TOGGLE_BACKLIGHT_CMD = 24;
	protected static final int TOGGLE_FULLSCREEN_CMD = 25;
	protected static final int TOGGLE_MAP_PROJ_CMD = 26;
	protected static final int TOGGLE_KEY_LOCK_CMD = 27;
	protected static final int TOGGLE_RECORDING_CMD = 28;
	protected static final int TOGGLE_RECORDING_SUSP_CMD = 29;
	protected static final int RECENTER_GPS_CMD = 30;
	protected static final int DATASCREEN_CMD = 31;
	protected static final int OVERVIEW_MAP_CMD = 32;
	protected static final int RETRIEVE_XML = 33;
	protected static final int PAN_LEFT25_CMD = 34;
	protected static final int PAN_RIGHT25_CMD = 35;
	protected static final int PAN_UP25_CMD = 36;
	protected static final int PAN_DOWN25_CMD = 37;
	protected static final int PAN_LEFT2_CMD = 38;
	protected static final int PAN_RIGHT2_CMD = 39;
	protected static final int PAN_UP2_CMD = 40;
	protected static final int PAN_DOWN2_CMD = 41;
	protected static final int REFRESH_CMD = 42;
	protected static final int SEARCH_CMD = 43;
	protected static final int TOGGLE_AUDIO_REC = 44;
	protected static final int ROUTING_START_CMD = 45;
	protected static final int ROUTING_STOP_CMD = 46;
	protected static final int ONLINE_INFO_CMD = 47;
	protected static final int ROUTING_START_WITH_MODE_SELECT_CMD = 48;
	protected static final int RETRIEVE_NODE = 49;
	protected static final int ICON_MENU = 50;
	protected static final int ABOUT_CMD = 51;
	protected static final int SETUP_CMD = 52;
	protected static final int SEND_MESSAGE_CMD = 53;
	protected static final int SHOW_DEST_CMD = 54;
	protected static final int EDIT_ADDR_CMD = 55;
	protected static final int OPEN_URL_CMD = 56;

	private final Command [] CMDS = new Command[57];

	public static final int DATASCREEN_NONE = 0;
	public static final int DATASCREEN_TACHO = 1;
	public static final int DATASCREEN_TRIP = 2;
	public static final int DATASCREEN_SATS = 3;
	
//	private SirfInput si;
	private LocationMsgProducer locationProducer;

	public String solution = "NoFix";
	
	/** Flag if the map is centered to the current GPS position (true)
	 * or if the user moved the map away from this position (false).
	 */
	public boolean gpsRecenter = true;
	/** Flag if the map is autoZoomed
	 */
	public boolean autoZoomed = true;
	
	private Position pos = new Position(0.0f, 0.0f,
			PositionMark.INVALID_ELEVATION, 0.0f, 0.0f, 1,
			System.currentTimeMillis());

	/**
	 * this node contains actually RAD coordinates
	 * although the constructor for Node(lat, lon) requires parameters in DEC format
	 * - e. g. "new Node(49.328010f, 11.352556f)"
	 */
	public Node center = new Node(49.328010f, 11.352556f);

//	Projection projection;

	private final GpsMid parent;
	
	public static TraceLayout tl = null;

	private String lastTitleMsg;
	private String currentTitleMsg;
	private volatile int currentTitleMsgOpenCount = 0;
	private volatile int setTitleMsgTimeout = 0;
	private String lastTitleMsgClock;
	
	private String currentAlertTitle;
	private String currentAlertMessage;
	private volatile int currentAlertsOpenCount = 0;
	private volatile int setAlertTimeout = 0;

	private long lastBackLightOnTime = 0;
	
	private volatile static long lastUserActionTime = 0;
	
	private long collected = 0;

	public PaintContext pc;
	
	public float scale = 15000f;
	
	int showAddons = 0;
	
	/** x position display was touched last time */
	private static int touchX = 0;
	/** y position display was touched last time */
	private static int touchY = 0;
	/** center when display was touched last time */
	private static Node	centerPointerPressedN = new Node();
	/** indicates whether this is a touch button or drag action*/
	private static boolean pointerDragAction = false;
	
	public volatile boolean routeCalc=false;
	public Tile tiles[] = new Tile[6];
	public Way actualSpeedLimitWay;

	// this is only for visual debugging of the routing engine
	Vector routeNodes = new Vector();

	private long oldRecalculationTime;

	private List recordingsMenu = null;
	private List routingsMenu = null;
	private GuiTacho guiTacho = null;
	private GuiTrip guiTrip = null;
	private GuiSatellites guiSatellites = null;
	private GuiWaypointSave guiWaypointSave = null;
	private static TraceIconMenu traceIconMenu = null;
	
	private final static Logger logger = Logger.getInstance(Trace.class, Logger.DEBUG);

//#mdebug info
	public static final String statMsg[] = { "no Start1:", "no Start2:",
			"to long  :", "interrupt:", "checksum :", "no End1  :",
			"no End2  :" };
//#enddebug
	/**
	 * Quality of Bluetooth reception, 0..100.
	 */
	private byte btquality;

	private int[] statRecord;

	/**
	 * Current speed from GPS in km/h.
	 */
	public volatile int speed;

	/**
	 * Current altitude from GPS in m.
	 */
	public volatile int altitude;
	
	/**
	 * Flag if we're speeding
	 */
	private volatile boolean speeding = false;
	private long lastTimeOfSpeedingSound = 0;
	private long startTimeOfSpeedingSign = 0;
	private int speedingSpeedLimit = 0;

	/**
	 * Current course from GPS in compass degrees, 0..359.
	 */
	private int course = 0;

	public boolean atDest = false;
	public boolean movedAwayFromDest = true;

	private Names namesThread;

	private Urls urlsThread;

	private ImageCollector imageCollector;
	
	private QueueDataReader tileReader;

	private QueueDictReader dictReader;

	private final Runtime runtime = Runtime.getRuntime();

	private RoutePositionMark dest = null;
	public Vector route = null;
	private RouteInstructions ri = null;
	
	private boolean running = false;
	private static final int CENTERPOS = Graphics.HCENTER | Graphics.VCENTER;

	public Gpx gpx;
	public AudioRecorder audioRec;
	
	private static volatile Trace traceInstance = null;

	private Routing	routeEngine;

	/*
	private static Font smallBoldFont;
	private static int smallBoldFontHeight;
	*/
	
	public boolean manualRotationMode = false;
	
	public Vector locationUpdateListeners;
	
	private Trace() throws Exception {
		//#debug
		logger.info("init Trace");
		
		this.parent = GpsMid.getInstance();
		
		CMDS[EXIT_CMD] = new Command("Exit", Command.EXIT, 2);
		CMDS[REFRESH_CMD] = new Command("Refresh", Command.ITEM, 4);
		CMDS[SEARCH_CMD] = new Command("Search", Command.OK, 1);
		CMDS[CONNECT_GPS_CMD] = new Command("Start GPS",Command.ITEM, 2);
		CMDS[DISCONNECT_GPS_CMD] = new Command("Stop GPS",Command.ITEM, 2);
		CMDS[START_RECORD_CMD] = new Command("Start record",Command.ITEM, 4);
		CMDS[STOP_RECORD_CMD] = new Command("Stop record",Command.ITEM, 4);
		CMDS[MANAGE_TRACKS_CMD] = new Command("Manage tracks",Command.ITEM, 5);
		CMDS[SAVE_WAYP_CMD] = new Command("Save waypoint",Command.ITEM, 7);
		CMDS[ENTER_WAYP_CMD] = new Command("Enter waypoint",Command.ITEM, 7);
		CMDS[MAN_WAYP_CMD] = new Command("Manage waypoints",Command.ITEM, 7);
		CMDS[ROUTING_TOGGLE_CMD] = new Command("Toggle routing",Command.ITEM, 3);
		CMDS[CAMERA_CMD] = new Command("Camera",Command.ITEM, 9);
		CMDS[CLEAR_DEST_CMD] = new Command("Clear destination",Command.ITEM, 10);
		CMDS[SET_DEST_CMD] = new Command("As destination",Command.ITEM, 11);
		CMDS[MAPFEATURES_CMD] = new Command("Map Features",Command.ITEM, 12);
		CMDS[RECORDINGS_CMD] = new Command("Recordings...",Command.ITEM, 4);
		CMDS[ROUTINGS_CMD] = new Command("Routing...",Command.ITEM, 3);
		CMDS[OK_CMD] = new Command("OK",Command.OK, 14);
		CMDS[BACK_CMD] = new Command("Back",Command.BACK, 15);
		CMDS[ZOOM_IN_CMD] = new Command("Zoom in",Command.ITEM, 100);
		CMDS[ZOOM_OUT_CMD] = new Command("Zoom out",Command.ITEM, 100);
		CMDS[MANUAL_ROTATION_MODE_CMD] = new Command("Manual rotation mode",Command.ITEM, 100);
		CMDS[TOGGLE_OVERLAY_CMD] = new Command("Next overlay",Command.ITEM, 100);
		CMDS[TOGGLE_BACKLIGHT_CMD] = new Command("Keep backlight on/off",Command.ITEM, 100);
		CMDS[TOGGLE_FULLSCREEN_CMD] = new Command("Switch to fullscreen",Command.ITEM, 100);
		CMDS[TOGGLE_MAP_PROJ_CMD] = new Command("Next map projection",Command.ITEM, 100);
		CMDS[TOGGLE_KEY_LOCK_CMD] = new Command("(De)Activate Keylock",Command.ITEM, 100);
		CMDS[TOGGLE_RECORDING_CMD] = new Command("(De)Activate recording",Command.ITEM, 100);
		CMDS[TOGGLE_RECORDING_SUSP_CMD] = new Command("Suspend recording",Command.ITEM, 100);
		CMDS[RECENTER_GPS_CMD] = new Command("Recenter on GPS",Command.ITEM, 100);
		CMDS[SHOW_DEST_CMD] = new Command("Show destination",Command.ITEM, 100);
		CMDS[DATASCREEN_CMD] = new Command("Tacho", Command.ITEM, 15);
		CMDS[OVERVIEW_MAP_CMD] = new Command("Overview/Filter map", Command.ITEM, 20);
		CMDS[RETRIEVE_XML] = new Command("Retrieve XML",Command.ITEM, 200);
		CMDS[PAN_LEFT25_CMD] = new Command("left 25%",Command.ITEM, 100);
		CMDS[PAN_RIGHT25_CMD] = new Command("right 25%",Command.ITEM, 100);
		CMDS[PAN_UP25_CMD] = new Command("up 25%",Command.ITEM, 100);
		CMDS[PAN_DOWN25_CMD] = new Command("down 25%",Command.ITEM, 100);
		CMDS[PAN_LEFT2_CMD] = new Command("left 2",Command.ITEM, 100);
		CMDS[PAN_RIGHT2_CMD] = new Command("right 2",Command.ITEM, 100);
		CMDS[PAN_UP2_CMD] = new Command("up 2",Command.ITEM, 100);
		CMDS[PAN_DOWN2_CMD] = new Command("down 2",Command.ITEM, 100);
		CMDS[TOGGLE_AUDIO_REC] = new Command("Audio recording",Command.ITEM, 100);
		CMDS[ROUTING_START_CMD] = new Command("Calculate route",Command.ITEM, 100);
		CMDS[ROUTING_STOP_CMD] = new Command("Stop routing",Command.ITEM, 100);
		CMDS[ONLINE_INFO_CMD] = new Command("Online info",Command.ITEM, 100);
		CMDS[OPEN_URL_CMD] = new Command("Open URL",Command.ITEM, 100);
		CMDS[ROUTING_START_WITH_MODE_SELECT_CMD] = new Command("Calculate route...",Command.ITEM, 100);
		CMDS[RETRIEVE_NODE] = new Command("Add POI to OSM...",Command.ITEM, 100);
		CMDS[ICON_MENU] = new Command("Menu",Command.OK, 100);
		CMDS[SETUP_CMD] = new Command("Setup", Command.ITEM, 25);
		CMDS[ABOUT_CMD] = new Command("About", Command.ITEM, 30);
		//#if polish.api.wmapi
		CMDS[SEND_MESSAGE_CMD] = new Command("Send SMS (map pos)",Command.ITEM, 20);
		//#endif
		CMDS[EDIT_ADDR_CMD] = new Command("Add Addr node"/*i:AddAddrNode*/,Command.ITEM,100);

		addAllCommands();
		
		Configuration.loadKeyShortcuts(gameKeyCommand, singleKeyPressCommand,
				repeatableKeyPressCommand, doubleKeyPressCommand, longKeyPressCommand,
				nonReleasableKeyPressCommand, CMDS);

		if (!Configuration.getCfgBitState(Configuration.CFGBIT_DISPLAYSIZE_SPECIFIC_DEFAULTS_DONE)) {
			if (getWidth() > 219) {
				// if the map display is wide enough, show the clock in the map screen by default
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_CLOCK_IN_MAP, true);
				// if the map display is wide enough, use big tab buttons by default
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS, true);
			}
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_DISPLAYSIZE_SPECIFIC_DEFAULTS_DONE, true);
		}
		
		try {
			startup();
		} catch (Exception e) {
			logger.fatal("Got an exception during startup: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		// setTitle("initTrace ready");
		
		locationUpdateListeners = new Vector();
		
		traceInstance = this;
	}
	
	/**
	 * Returns the instance of the map screen. If none exists yet,
	 * a new instance is generated
	 * @return Reference to singleton instance
	 */
	public static synchronized Trace getInstance() {
		if (traceInstance == null) {
			try {
				traceInstance = new Trace();
			} catch (Exception e) {
				logger.exception("Failed to initialise Map screen", e);
			}
		}
		return traceInstance;
	}

	/**
	 * Starts the LocationProvider in the background
	 */
	public void run() {
		try {
			
			if (running) {
				receiveMessage("GPS starter already running");
				return;
			}

			//#debug info
			logger.info("start thread init locationprovider");
			if (locationProducer != null) {
				receiveMessage("Location provider already running");
				return;
			}
			if (Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_NONE) {
				receiveMessage("No location provider");
				return;
			}
			running=true;
			int locprov = Configuration.getLocationProvider();
			receiveMessage("Connect to " + Configuration.LOCATIONPROVIDER[locprov]);
			switch (locprov) {
				case Configuration.LOCATIONPROVIDER_SIRF:
					locationProducer = new SirfInput();
					break;
				case Configuration.LOCATIONPROVIDER_NMEA:
					locationProducer = new NmeaInput();
					break;
				case Configuration.LOCATIONPROVIDER_SECELL:
					locationProducer = new SECellID();
					break;
				case Configuration.LOCATIONPROVIDER_JSR179:
					//#if polish.api.locationapi
					try {
						String jsr179Version = null;
						try {
							jsr179Version = System.getProperty("microedition.location.version");
						} catch (RuntimeException re) {
							// Some phones throw exceptions if trying to access properties that don't
							// exist, so we have to catch these and just ignore them.
						} catch (Exception e) {
							// As above
						}
						if (jsr179Version != null && jsr179Version.length() > 0) {
							Class jsr179Class = Class.forName("de.ueller.gps.location.JSR179Input");
							locationProducer = (LocationMsgProducer) jsr179Class.newInstance();
						}
					} catch (ClassNotFoundException cnfe) {
						locationDecoderEnd();
						logger.exception("Your phone does not support JSR179, please use a different location provider", cnfe);
						running = false;
						return;
					}
					//#else
					// keep Eclipse happy
					if (true) {
						logger.error("JSR179 is not compiled in this version of GpsMid");
						running = false;
						return;
					}
					//#endif
					break;

			}
			
			//#if polish.api.fileconnection
			/**
			 * Allow for logging the raw data coming from the gps
			 */
			String url = Configuration.getGpsRawLoggerUrl();
			//logger.error("Raw logging url: " + url);
			if (url != null) {
				try {
					if (Configuration.getGpsRawLoggerEnable()) {
						logger.info("Raw Location logging to: " + url);
						url += "rawGpsLog" + HelperRoutines.formatSimpleDateNow() + ".txt";
						javax.microedition.io.Connection logCon = Connector.open(url);
						if (logCon instanceof FileConnection) {
							FileConnection fileCon = (FileConnection)logCon;
							if (!fileCon.exists()) {
								fileCon.create();
							}
							locationProducer.enableRawLogging(((FileConnection)logCon).openOutputStream());
						} else {
							logger.info("Raw logging of NMEA is only to filesystem supported");
						}
					}
						
					/**
					 * Help out the OpenCellId.org project by gathering and logging
					 * data of cell ids together with current Gps location. This information
					 * can then be uploaded to their web site to determine the position of the
					 * cell towers. It currently only works for SE phones
					 */
					if (Configuration.getCfgBitState(Configuration.CFGBIT_CELLID_LOGGING)) {
						SECellLocLogger secl = new SECellLocLogger();
						if (secl.init()) {
							locationProducer.addLocationMsgReceiver(secl);
						}
					}
				} catch (IOException ioe) {
					logger.exception("Couldn't open file for raw logging of Gps data",ioe);
				} catch (SecurityException se) {
					logger.error("Permission to write data for NMEA raw logging was denied");
				}
			}
			//#endif
			if (locationProducer == null) {
				logger.error("Your phone does not seem to support this method of location input, please choose a different one");
				running  = false;
				return;
			}
			if (!locationProducer.init(this)) {
				logger.info("Failed to initialise location producer");
				running = false;
				return;
			}
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT)) {
				GpsMid.mNoiseMaker.playSound("CONNECT");
			}
			//#debug debug
			logger.debug("rm connect, add disconnect");
			removeCommand(CMDS[CONNECT_GPS_CMD]);
			addCommand(CMDS[DISCONNECT_GPS_CMD]);
			//#debug info
			logger.info("end startLocationPovider thread");
			//		setTitle("lp="+Configuration.getLocationProvider() + " " + Configuration.getBtUrl());
		} catch (SecurityException se) {
			/**
			 * The application was not permitted to connect to the required resources
			 * Not much we can do here other than gracefully shutdown the thread			 *
			 */
		} catch (OutOfMemoryError oome) {
			logger.fatal("Trace thread crashed as out of memory: " + oome.getMessage());
			oome.printStackTrace();
		} catch (Exception e) {
			logger.fatal("Trace thread crashed unexpectadly with error " +  e.getMessage());
			e.printStackTrace();
		} finally {
			running = false;
		}
		running = false;
	}
	
	// add the command only if icon menus are not used
	public void addCommand(Command c) {
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			super.addCommand(c);
		}
	}

	// remove the command only if icon menus are not used
	public void removeCommand(Command c) {
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			super.removeCommand(c);
		}
	}

	public synchronized void pause() {
		logger.debug("Pausing application");
		if (imageCollector != null) {
			imageCollector.suspend();
		}
		if (locationProducer != null) {
			locationProducer.close();
		} else {
			return;
		}
		int polling = 0;
		while ((locationProducer != null) && (polling < 7)) {
			polling++;
			try {
				wait(200);
			} catch (InterruptedException e) {
				break;
			}
		}
		if (locationProducer != null) {
			logger.error("LocationProducer took too long to close, giving up");
		}
	}

	public void resume() {
		logger.debug("resuming application");
		if (imageCollector != null) {
			imageCollector.resume();
		}
		Thread thread = new Thread(this,"LocationProducer init");
		thread.start();
	}

	public void autoRouteRecalculate() {
		if ( gpsRecenter && Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AUTO_RECALC) ) {
			if (Math.abs(System.currentTimeMillis()-oldRecalculationTime) >= 7000 ) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
					GpsMid.mNoiseMaker.playSound(RouteSyntax.getInstance().getRecalculationSound(), (byte) 5, (byte) 1 );
				}
				//#debug debug
				logger.debug("autoRouteRecalculate");
				// recalculate route
				commandAction(CMDS[ROUTING_START_CMD],(Displayable) null);
			}
		}
	}

	
	public boolean isGpsConnected() {
		return locationProducer != null && !solution.equalsIgnoreCase("Off");
	}

	/**
	 * Adds all commands for a normal menu.
	 */
	public void addAllCommands() {
		addCommand(CMDS[EXIT_CMD]);
		addCommand(CMDS[SEARCH_CMD]);
		if (isGpsConnected()) {
			addCommand(CMDS[DISCONNECT_GPS_CMD]);
		} else {
			addCommand(CMDS[CONNECT_GPS_CMD]);
		}
		addCommand(CMDS[MANAGE_TRACKS_CMD]);
		addCommand(CMDS[MAN_WAYP_CMD]);
		addCommand(CMDS[ROUTINGS_CMD]);
		addCommand(CMDS[RECORDINGS_CMD]);
		addCommand(CMDS[MAPFEATURES_CMD]);
		addCommand(CMDS[DATASCREEN_CMD]);
		addCommand(CMDS[OVERVIEW_MAP_CMD]);
		//#if polish.api.online
		addCommand(CMDS[ONLINE_INFO_CMD]);
		addCommand(CMDS[OPEN_URL_CMD]);
		//#if polish.api.osm-editing
		addCommand(CMDS[RETRIEVE_XML]);
		addCommand(CMDS[RETRIEVE_NODE]);
		addCommand(CMDS[EDIT_ADDR_CMD]);
		//#endif
		//#endif
		addCommand(CMDS[SETUP_CMD]);
		addCommand(CMDS[ABOUT_CMD]);
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN)) {
				super.addCommand(CMDS[ICON_MENU]);
			}
		}
		setCommandListener(this);
	}
	
	/**
	 * This method must remove all commands that were added by addAllCommands().
	 */
	public void removeAllCommands() {
		//setCommandListener(null);
		/* Although j2me documentation says removeCommand for a non-attached command is allowed
		 * this would crash MicroEmulator. Thus we only remove the commands attached.
		 */
		removeCommand(CMDS[EXIT_CMD]);
		removeCommand(CMDS[SEARCH_CMD]);
		if (isGpsConnected()) {
			removeCommand(CMDS[DISCONNECT_GPS_CMD]);
		} else {
			removeCommand(CMDS[CONNECT_GPS_CMD]);
		}
		removeCommand(CMDS[MANAGE_TRACKS_CMD]);
		removeCommand(CMDS[MAN_WAYP_CMD]);
		removeCommand(CMDS[MAPFEATURES_CMD]);
		removeCommand(CMDS[RECORDINGS_CMD]);
		removeCommand(CMDS[ROUTINGS_CMD]);
		removeCommand(CMDS[DATASCREEN_CMD]);
		removeCommand(CMDS[OVERVIEW_MAP_CMD]);
		//#if polish.api.online
		removeCommand(CMDS[ONLINE_INFO_CMD]);
		removeCommand(CMDS[OPEN_URL_CMD]);
		//#if polish.api.osm-editing
		removeCommand(CMDS[RETRIEVE_XML]);
		removeCommand(CMDS[RETRIEVE_NODE]);
		removeCommand(CMDS[EDIT_ADDR_CMD]);
		//#endif
		//#endif
		removeCommand(CMDS[SETUP_CMD]);
		removeCommand(CMDS[ABOUT_CMD]);
		
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN)) {
				super.removeCommand(CMDS[ICON_MENU]);
			}
		}
	}

	/** Sets the Canvas to fullScreen or windowed mode
	 * when icon menus are active the Menu command gets removed
	 * so the Canvas will not unhide the menu bar first when pressing fire (e.g. on SE mobiles)
	*/
	public void setFullScreenMode(boolean fullScreen) {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			if (fullScreen) {
				super.removeCommand(CMDS[ICON_MENU]);
			} else {
				super.addCommand(CMDS[ICON_MENU]);
			}
		}
		super.setFullScreenMode(fullScreen);
	}

	
	
	public void commandAction(Command c, Displayable d) {
		updateLastUserActionTime();

		try {
			if((keyboardLocked) && (d != null)) {
				// show alert in keypressed() that keyboard is locked
				keyPressed(0);
				return;
			}
			
			if ((c == CMDS[PAN_LEFT25_CMD]) || (c == CMDS[PAN_RIGHT25_CMD])
					|| (c == CMDS[PAN_UP25_CMD]) || (c == CMDS[PAN_DOWN25_CMD])
					|| (c == CMDS[PAN_LEFT2_CMD]) || (c == CMDS[PAN_RIGHT2_CMD])
					|| (c == CMDS[PAN_UP2_CMD]) || (c == CMDS[PAN_DOWN2_CMD])) {
				int panX = 0; int panY = 0;
				int courseDiff = 0;
				int backLightLevelDiff = 0;
				if (c == CMDS[PAN_LEFT25_CMD]) {
					panX = -25;
				} else if (c == CMDS[PAN_RIGHT25_CMD]) {
					panX = 25;
				} else if (c == CMDS[PAN_UP25_CMD]) {
					panY = -25;
				} else if (c == CMDS[PAN_DOWN25_CMD]) {
					panY = 25;
				} else if (c == CMDS[PAN_LEFT2_CMD]) {
					if (TrackPlayer.isPlaying) {
						TrackPlayer.slower();
					} else if (manualRotationMode) {
						courseDiff=-5;
					} else {
						panX = -2;
					}
					backLightLevelDiff = -25;
				} else if (c == CMDS[PAN_RIGHT2_CMD]) {
					if (TrackPlayer.isPlaying) {
						TrackPlayer.faster();
					} else if (manualRotationMode) {
						courseDiff=5;
					} else {
						panX = 2;
					}
					backLightLevelDiff = 25;
				} else if (c == CMDS[PAN_UP2_CMD]) {
					if (route!=null && Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BROWSING)) {
						RouteInstructions.toNextInstruction(1);
					} else {
						panY = -2;
					}
				} else if (c == CMDS[PAN_DOWN2_CMD]) {
					if (route!=null && Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BROWSING)) {
						RouteInstructions.toNextInstruction(-1);
					} else {
						panY = 2;
					}
				}
				
				if (backLightLevelDiff !=0  &&  System.currentTimeMillis() < (lastBackLightOnTime + 1500)) {
					// turn backlight always on when dimming
					Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON, true, false);
					lastBackLightOnTime = System.currentTimeMillis();
					Configuration.addToBackLightLevel(backLightLevelDiff);
					parent.showBackLightLevel();
				} else if (imageCollector != null) {
					if (courseDiff == 360) {
						course = 0; //N
					} else {
						course += courseDiff;
						course %= 360;
						if (course < 0) {
							course += 360;
						}
					}
					if (panX != 0 || panY != 0) {
						gpsRecenter = false;
					}
					imageCollector.getCurrentProjection().pan(center, panX, panY);
				}
				return;
			}
			if (c == CMDS[EXIT_CMD]) {
				// FIXME: This is a workaround. It would be better if recording
				// would not be stopped when leaving the map.
				if (gpx.isRecordingTrk()) {
					alert("Record Mode", "Please stop recording before exit." , 2500);
					return;
				}
				
				pause();
				parent.exit();
				return;
			}
			if (c == CMDS[START_RECORD_CMD]) {
				try {
					gpx.newTrk(false);
					alert("Gps track recording", "Starting to record", 1250);
				} catch (RuntimeException e) {
					receiveMessage(e.getMessage());
				}
				recordingsMenu = null; // refresh recordings menu
				return;
			}
			if (c == CMDS[STOP_RECORD_CMD]) {
				gpx.saveTrk(false);
				alert("Gps track recording", "Stopping to record", 1250);
				recordingsMenu = null; // refresh recordings menu
				return;
			}
			if (c == CMDS[MANAGE_TRACKS_CMD]) {
				if (gpx.isRecordingTrk()) {
					alert("Record Mode", "You need to stop recording before managing tracks." , 4000);
					return;
				}

			    GuiGpx guiGpx = new GuiGpx(this);
			    guiGpx.show();
			    return;
			}
			if (c == CMDS[REFRESH_CMD]) {
				repaint();
				return;
			}
			if (c == CMDS[CONNECT_GPS_CMD]) {
				if (locationProducer == null) {
					if (TrackPlayer.isPlaying) {
						TrackPlayer.getInstance().stop();
						alert("Trackplayer", "Playing stopped for connecting to GPS", 2500);
					}
					Thread thread = new Thread(this,"LocationProducer init");
					thread.start();
				}
				return;
			}
			if (c == CMDS[SEARCH_CMD]) {
				GuiSearch guiSearch = new GuiSearch(this);
				guiSearch.show();
				return;
			}
			if (c == CMDS[DISCONNECT_GPS_CMD]) {
				if (locationProducer != null) {
					locationProducer.close();
				}
				return;
			}
			if (c == CMDS[ENTER_WAYP_CMD]) {
				if (gpx.isLoadingWaypoints()) {
					showAlertLoadingWpt();
				} else {
					GuiWaypointEnter gwpe = new GuiWaypointEnter(this);
					gwpe.show();
				}
				return;
			}
			if (c == CMDS[MAN_WAYP_CMD]) {
				if (gpx.isLoadingWaypoints()) {
					showAlertLoadingWpt();
				} else {
					GuiWaypoint gwp = new GuiWaypoint(this);
					gwp.show();
				}
				return;
			}
			if (c == CMDS[MAPFEATURES_CMD]) {
				GuiMapFeatures gmf = new GuiMapFeatures(this);
				gmf.show();
				repaint();
				return;
			}
			if (c == CMDS[OVERVIEW_MAP_CMD]) {
				GuiOverviewElements ovEl = new GuiOverviewElements(this);
				ovEl.show();
				repaint();
				return;
			}
			//#if polish.api.wmapi
			if (c == CMDS[SEND_MESSAGE_CMD]) {
				GuiSendMessage sendMsg = new GuiSendMessage(this);
				sendMsg.show();
				repaint();
				return;
			}
			//#endif
			if (c == CMDS[RECORDINGS_CMD]) {
				if (recordingsMenu == null) {
					boolean hasJSR120 = Configuration.hasDeviceJSR120();
					int noElements = 3;
					//#if polish.api.mmapi
					noElements += 2;
					//#endif
					//#if polish.api.wmapi
					if (hasJSR120) {
						noElements++;
					}
					//#endif
					
					int idx = 0;
					String[] elements = new String[noElements];
					if (gpx.isRecordingTrk()) {
						elements[idx++] = "Stop Gpx tracklog";
					} else {
						elements[idx++] = "Start Gpx tracklog";
					}
					
					elements[idx++] = "Save waypoint";
					elements[idx++] = "Enter waypoint";
					//#if polish.api.mmapi
					elements[idx++] = "Take pictures";
					if (audioRec.isRecording()) {
						elements[idx++] = "Stop audio recording";
					} else {
						elements[idx++] = "Start audio recording";
						
					}
					//#endif
					//#if polish.api.wmapi
					if (hasJSR120) {
						elements[idx++] = "Send SMS (map pos)";
					}
					//#endif
					
					recordingsMenu = new List("Recordings...",Choice.IMPLICIT,elements,null);
					recordingsMenu.addCommand(CMDS[OK_CMD]);
					recordingsMenu.addCommand(CMDS[BACK_CMD]);
					recordingsMenu.setSelectCommand(CMDS[OK_CMD]);
					parent.show(recordingsMenu);
					recordingsMenu.setCommandListener(this);
				}
				if (recordingsMenu != null) {
					recordingsMenu.setSelectedIndex(0, true);
					parent.show(recordingsMenu);
				}
				return;
			}
			if (c == CMDS[ROUTINGS_CMD]) {
				if (routingsMenu == null) {
					String[] elements = new String[4];
					if (routeCalc || route != null) {
						elements[0] = "Stop routing";
					} else {
						elements[0] = "Calculate route";
					}
					elements[1] = "Set destination";
					elements[2] = "Show destination";
					elements[3] = "Clear destination";
					routingsMenu = new List("Routing..", Choice.IMPLICIT, elements, null);
					routingsMenu.addCommand(CMDS[OK_CMD]);
					routingsMenu.addCommand(CMDS[BACK_CMD]);
					routingsMenu.setSelectCommand(CMDS[OK_CMD]);
					routingsMenu.setCommandListener(this);
				}
				if (routingsMenu != null) {
					routingsMenu.setSelectedIndex(0, true);
					parent.show(routingsMenu);
				}
				return;
			}
			if (c == CMDS[SAVE_WAYP_CMD]) {
				if (gpx.isLoadingWaypoints()) {
					showAlertLoadingWpt();
				} else {
    				if (guiWaypointSave == null) {
    					guiWaypointSave = new GuiWaypointSave(this);
    				}
    				if (guiWaypointSave != null) {
    					if (gpsRecenter) {
    						// TODO: If we lose the fix the old position and height
    						// will be used silently -> we should inform the user
    						// here that we have no fix - he may not know what's going on.
    						guiWaypointSave.setData(new PositionMark(center.radlat,
    								center.radlon, (int)pos.altitude, pos.timeMillis,
    								/* fix */ (byte)-1, /* sats */ (byte)-1, 
    								/* sym */ (byte)-1, /* type */ (byte)-1));
    					} else {
    						// Cursor does not point to current position
    						// -> it does not make sense to add elevation and GPS fix info.
    						guiWaypointSave.setData(new PositionMark(center.radlat, 
    								center.radlon, PositionMark.INVALID_ELEVATION,
    								pos.timeMillis, /* fix */ (byte)-1, 
    								/* sats */ (byte)-1, /* sym */ (byte)-1, 
    								/* type */ (byte)-1));
    					}
    					guiWaypointSave.show();					
    				}
				}
				return;
			}
			if (c == CMDS[ONLINE_INFO_CMD]) {
				//#if polish.api.online
					Position oPos = new Position(center.radlat, center.radlon,
							0.0f, 0.0f, 0.0f, 0, 0);
					GuiWebInfo gWeb = new GuiWebInfo(this, oPos, pc);
					gWeb.show();
				//#else
					alert("No online capabilites",
							"Set app=GpsMid-Generic-editing and enableEditing=true in " +
							".properties file and recreate GpsMid with Osm2GpsMid.",
							Alert.FOREVER);
				//#endif
			}
			if (c == CMDS[OPEN_URL_CMD]) {
				//#if polish.api.online
				String url;
				if ((pc.actualWay != null) && ((url = getUrl(pc.actualWay.urlIdx)) != null)) {
					try {
						// #debug info
						logger.info("Platform request for " + url);
						GpsMid.getInstance().platformRequest(getUrl(pc.actualWay.urlIdx));
					}
					catch (Exception e) {
						logger.exception("Could not open url " + url, e);
					}
				} else {
					alert("Open URL"/*i*/, "No URL available"/*i*/, 1500);
				}
				return;
				//#else
					alert("No online capabilites",
							"Set app=GpsMid-Generic-editing and enableEditing=true in " +
							".properties file and recreate GpsMid with Osm2GpsMid.",
							Alert.FOREVER);
				//#endif
			}
			if (c == CMDS[BACK_CMD]) {
				show();
				return;
			}
			if (c == CMDS[OK_CMD]) {
				if (d == recordingsMenu) {
					 switch (recordingsMenu.getSelectedIndex()) {
			            case 0: {
		    				if ( gpx.isRecordingTrk() ) {
		    					commandAction(CMDS[STOP_RECORD_CMD],(Displayable) null);
								if (! Configuration.getCfgBitState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_STOP)) {
							    	show();
								}
		    				} else {
		    					commandAction(CMDS[START_RECORD_CMD],(Displayable) null);
								if (! Configuration.getCfgBitState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_START)) {
							    	show();
								}
		    				}
			            	break;
			            }
			            case 1: {
			            	commandAction(CMDS[SAVE_WAYP_CMD], null);
							break;
			            }
			            case 2: {
			            	commandAction(CMDS[ENTER_WAYP_CMD], null);
							break;
			            }
			          //#if polish.api.mmapi
			            case 3: {
			            	commandAction(CMDS[CAMERA_CMD], null);
			            	break;
			            }
			            case 4: {
			            	show();
			            	commandAction(CMDS[TOGGLE_AUDIO_REC], null);
			            	break;
			            }
			          //#endif
			          //#if polish.api.mmapi && polish.api.wmapi
			            case 5: {
			            	commandAction(CMDS[SEND_MESSAGE_CMD], null);
			            	break;
			            }
			          //#elif polish.api.wmapi
			            case 3: {
			            	commandAction(CMDS[SEND_MESSAGE_CMD], null);
			            	break;
			            }
			          //#endif
					 }
				}
				if (d == routingsMenu) {
					show();
					switch (routingsMenu.getSelectedIndex()) {
					case 0: {
						if (routeCalc || route != null) {
							commandAction(CMDS[ROUTING_STOP_CMD], null);
						} else {
							commandAction(CMDS[ROUTING_START_WITH_MODE_SELECT_CMD], null);
						}
						break;
					}
					case 1: {
						commandAction(CMDS[SET_DEST_CMD], null);
						break;
					}
					case 2: {
						commandAction(CMDS[SHOW_DEST_CMD], null);
						break;
					}
					case 3: {
						commandAction(CMDS[CLEAR_DEST_CMD], null);
						break;
					}
					}
				}
				return;
			}
			//#if polish.api.mmapi
			if (c == CMDS[CAMERA_CMD]) {
				try {
					Class GuiCameraClass = Class.forName("de.ueller.midlet.gps.GuiCamera");
					Object GuiCameraObject = GuiCameraClass.newInstance();
					GuiCameraInterface cam = (GuiCameraInterface)GuiCameraObject;
					cam.init(this);
					cam.show();
				} catch (ClassNotFoundException cnfe) {
					logger.exception("Your phone does not support the necessary JSRs to use the camera", cnfe);
				}
				return;
			}
			if (c == CMDS[TOGGLE_AUDIO_REC]) {
				if (audioRec.isRecording()) {
					audioRec.stopRecord();
				} else {
					audioRec.startRecorder();
				}
				recordingsMenu = null; // refresh recordings menu
				return;
			}
			//#endif
			if (c == CMDS[ROUTING_TOGGLE_CMD]) {
				if (routeCalc || route != null) {
					commandAction(CMDS[ROUTING_STOP_CMD],(Displayable) null);
				} else {
					commandAction(CMDS[ROUTING_START_WITH_MODE_SELECT_CMD],(Displayable) null);
				}
				return;
			}

			if (c == CMDS[ROUTING_START_WITH_MODE_SELECT_CMD]) {
				gpsRecenter = true;
				GuiRoute guiRoute = new GuiRoute(this, false);
				guiRoute.show();
				return;
			}
			
			if (c == CMDS[ROUTING_START_CMD]) {
				if (! routeCalc || RouteLineProducer.isRunning()) {
					routeCalc = true;
					if (Configuration.getContinueMapWhileRouteing() != Configuration.continueMap_Always ) {
	  				   stopImageCollector();
					}
					RouteInstructions.resetOffRoute(route, center);
					// center of the map is the route source
					RoutePositionMark routeSource = new RoutePositionMark(center.radlat, center.radlon);
					logger.info("Routing source: " + routeSource);
					routeNodes=new Vector();
					routeEngine = new Routing(tiles, this);
					routeEngine.solve(routeSource, dest);
//					resume();
				}
				routingsMenu = null; // refresh routingsMenu
				return;
			}
			if (c == CMDS[ROUTING_STOP_CMD]) {
				NoiseMaker.stopPlayer();
				if (routeCalc) {
					if (routeEngine != null) {
						routeEngine.cancelRouting();
					}
					alert("Route Calculation", "Cancelled", 1500);
				} else {
					alert("Routing", "Off", 750);
				}
				endRouting();
				routingsMenu = null; // refresh routingsMenu
				// redraw immediately
				synchronized (this) {
					if (imageCollector != null) {
						imageCollector.newDataReady();
					}
				}
				routingsMenu = null; // refresh routingsMenu
				return;
			}
			if (c == CMDS[ZOOM_IN_CMD]) {
				scale = scale / 1.5f;
				autoZoomed = false;
				return;
			}
			if (c == CMDS[ZOOM_OUT_CMD]) {
				scale = scale * 1.5f;
				autoZoomed = false;
				return;
			}
			if (c == CMDS[MANUAL_ROTATION_MODE_CMD]) {
				manualRotationMode = !manualRotationMode;
				if (manualRotationMode) {
					if (hasPointerEvents()) {
						alert("Manual Rotation", "Change course with zoom buttons", 3000);
					} else {
						alert("Manual Rotation", "Change course with left/right keys", 3000);
					}
				} else {
					alert("Manual Rotation", "Off", 750);
				}
				return;
			}
			if (c == CMDS[TOGGLE_OVERLAY_CMD]) {
				showAddons++;
				repaint();
				return;
			}
			if (c == CMDS[TOGGLE_BACKLIGHT_CMD]) {
//				toggle Backlight
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON,
									!(Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON)),
									false);
				lastBackLightOnTime = System.currentTimeMillis();
				parent.showBackLightLevel();
				return;
			}
			if (c == CMDS[TOGGLE_FULLSCREEN_CMD]) {
				boolean fullScreen = !Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN);
				Configuration.setCfgBitState(Configuration.CFGBIT_FULLSCREEN, fullScreen, false);
				setFullScreenMode(fullScreen);
				return;
			}
			if (c == CMDS[TOGGLE_MAP_PROJ_CMD]) {
				alert("Map Rotation", ProjFactory.nextProj(), 750);
				// redraw immediately
				synchronized (this) {
					if (imageCollector != null) {
						imageCollector.newDataReady();
					}
				}
				return;
			}
			if (c == CMDS[TOGGLE_KEY_LOCK_CMD]) {
				keyboardLocked = !keyboardLocked;
				if (keyboardLocked) {
					// show alert that keys are locked
					keyPressed(0);
				} else {
					alert("GpsMid", "Keys unlocked", 1000);
				}
				return;
			}
			if (c == CMDS[TOGGLE_RECORDING_CMD]) {
				if ( gpx.isRecordingTrk() ) {
					commandAction(CMDS[STOP_RECORD_CMD],(Displayable) null);
				} else {
					commandAction(CMDS[START_RECORD_CMD],(Displayable) null);
				}
				return;
			}
			if (c == CMDS[TOGGLE_RECORDING_SUSP_CMD]) {
				if (gpx.isRecordingTrk()) {
					if ( gpx.isRecordingTrkSuspended() ) {
						alert("Gps track recording", "Resuming recording", 1000);
						gpx.resumeTrk();
					} else {
						alert("Gps track recording", "Suspending recording", 1000);
						gpx.suspendTrk();
					}
				}
				return;
			}
			if (c == CMDS[RECENTER_GPS_CMD]) {
				gpsRecenter = true;
				autoZoomed = true;
				newDataReady();
				return;
			}
			if (c == CMDS[SHOW_DEST_CMD]) {
				if (dest != null) {
					//We are explicitly setting the map to this position, so we probably don't
					//want it to be recentered on the GPS immediately.
					gpsRecenter = false;
					
					center.setLatLonRad(dest.lat, dest.lon);
					updatePosition();
					movedAwayFromDest = false;
				}
				else {
					alert("Show destination", "Destination is not specified yet", 3000);
				}
				
				return;
			}
			if (c == CMDS[DATASCREEN_CMD]) {
				showNextDataScreen(DATASCREEN_NONE);
				return;
			}

			if (c == CMDS[ICON_MENU] && Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
				showIconMenu();
				return;
			}
			if (c == CMDS[SETUP_CMD]) {
				new GuiDiscover(parent);
				return;
			}
			if (c == CMDS[ABOUT_CMD]) {
				new Splash(parent);
				return;
			}

			
			if (! routeCalc) {
				//#if polish.api.osm-editing
				if (c == CMDS[RETRIEVE_XML]) {
					if (Legend.enableEdits) {
						// -1 alert ("Editing", "Urlidx: " + pc.actualWay.urlIdx, Alert.FOREVER);
						if ((pc.actualWay != null) && (getUrl(pc.actualWay.urlIdx) != null)) {
							parent.alert ("Url", "Url: " + getUrl(pc.actualWay.urlIdx), Alert.FOREVER);
						}

						if ((pc.actualWay != null) && (pc.actualWay instanceof EditableWay)) {
							EditableWay eway = (EditableWay)pc.actualWay;
							GUIosmWayDisplay guiWay = new GUIosmWayDisplay(eway, pc.actualSingleTile, this);
							guiWay.show();
							guiWay.refresh();
						}
					} else {
						parent.alert("Editing", "Editing support was not enabled in Osm2GpsMid", Alert.FOREVER);
					}
				}
				if (c == CMDS[RETRIEVE_NODE]) {
					if (Legend.enableEdits) {
						GuiOSMPOIDisplay guiNode = new GuiOSMPOIDisplay(-1, null,
								center.radlat, center.radlon, this);
						guiNode.show();
						guiNode.refresh();
					} else {
						logger.error("Editing is not enabled in this map");
					}
				}
				if (c == CMDS[EDIT_ADDR_CMD]) {
					if (Legend.enableEdits) {
						String streetName = "";
						if ((pc != null) && (pc.actualWay != null)) {
							streetName = getName(pc.actualWay.nameIdx);
						}
						GuiOSMAddrDisplay guiAddr = new GuiOSMAddrDisplay(-1, streetName, null,
								center.radlat, center.radlon, this);
						guiAddr.show();
					} else {
						logger.error("Editing is not enabled in this map");
					}
				}
				//#else
				if (c == CMDS[RETRIEVE_XML] || c == CMDS[RETRIEVE_NODE] || c == CMDS[EDIT_ADDR_CMD]) {
					alert("No online capabilites",
						"Set app=GpsMid-Generic-editing and enableEditing=true in " +
						".properties file and recreate GpsMid with Osm2GpsMid.",
						Alert.FOREVER);
				}
				//#endif
				if (c == CMDS[SET_DEST_CMD]) {
					RoutePositionMark pm1 = new RoutePositionMark(center.radlat, center.radlon);
					setDestination(pm1);
					return;
				}
				if (c == CMDS[CLEAR_DEST_CMD]) {
					setDestination(null);
					return;
				}
			} else {
				alert("Error", "Currently in route calculation", 2000);
			}
		} catch (Exception e) {
 			logger.exception("In Trace.commandAction", e);
		}

	}
	
	private void startImageCollector() throws Exception {
		//#debug info
		logger.info("Starting ImageCollector");
		Images images = new Images();
		pc = new PaintContext(this, images);
		pc.legend = GpsMid.legend;
		/* move responsibility for overscan to ImmageCollector
		int w = (this.getWidth() * 125) / 100;
		int h = (this.getHeight() * 125) / 100;
		*/
		imageCollector = new ImageCollector(tiles, this.getWidth(), this.getHeight(), this, images, pc.legend);
//		projection = ProjFactory.getInstance(center,course, scale, getWidth(), getHeight());
//		pc.setP(projection);
		pc.center = center.copy();
		pc.scale = scale;
		pc.xSize = this.getWidth();
		pc.ySize = this.getHeight();
	}

	private void stopImageCollector(){
		//#debug info
		logger.info("Stopping ImageCollector");
		cleanup();
		if (imageCollector != null ) {
			imageCollector.stop();
			imageCollector=null;
		}
		System.gc();
	}

	public void startup() throws Exception {
//		logger.info("reading Data ...");
		namesThread = new Names();
		urlsThread = new Urls();
		new DictReader(this);
		if (Configuration.getCfgBitState(Configuration.CFGBIT_AUTO_START_GPS)) {
			Thread thread = new Thread(this, "Trace");
			thread.start();
		}
//		logger.info("Create queueDataReader");
		tileReader = new QueueDataReader(this);
//		logger.info("create imageCollector");
		dictReader = new QueueDictReader(this);
		this.gpx = new Gpx();
		this.audioRec = new AudioRecorder();
		setDict(gpx, (byte)5);
		startImageCollector();
		recreateTraceLayout();
	}

	public void shutdown() {
		if (gpx != null) {
			gpx.saveTrk(true);
		}
		//#debug debug
		logger.debug("Shutdown: stopImageCollector");
		stopImageCollector();
		if (namesThread != null) {
			//#debug debug
			logger.debug("Shutdown: namesThread");
			namesThread.stop();
			namesThread = null;
		}
		if (urlsThread != null) {
			urlsThread.stop();
			urlsThread = null;
		}
		if (dictReader != null) {
			//#debug debug
			logger.debug("Shutdown: dictReader");
			dictReader.shutdown();
			dictReader = null;
		}
		if (tileReader != null) {
			//#debug debug
			logger.debug("Shutdown: tileReader");
			tileReader.shutdown();
			tileReader = null;
		}
		if (locationProducer != null) {
			//#debug debug
			logger.debug("Shutdown: locationProducer");
			locationProducer.close();
		}
		if (TrackPlayer.isPlaying) {
			//#debug debug
			logger.debug("Shutdown: TrackPlayer");
			TrackPlayer.getInstance().stop();
		}
	}
	
	protected void sizeChanged(int w, int h) {
		updateLastUserActionTime();
		if (imageCollector != null) {
			logger.info("Size of Canvas changed to " + w + "|" + h);
			stopImageCollector();
			try {
				startImageCollector();
				imageCollector.resume();
				imageCollector.newDataReady();
			} catch (Exception e) {
				logger.exception("Could not reinitialise Image Collector after size change", e);
			}
			/**
			 * Recalculate the projection, as it may depends on the size of the screen
			 */
			updatePosition();
		}

		tl = new TraceLayout(0, 0, w, h);
	}


	protected void paint(Graphics g) {
		//#debug debug
		logger.debug("Drawing Map screen");
		
		try {
			int yc = 1;
			int la = 18;
			getPC();
			// cleans the screen
			g.setColor(Legend.COLORS[Legend.COLOR_MAP_BACKGROUND]);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			pc.g = g;
			if (imageCollector != null) {
				/*
				 *  When painting we receive a copy of the center coordinates
				 *  where the imageCollector has drawn last
				 *  as we need to base the routing instructions on the information
				 *  determined during the way drawing (e.g. the current routePathConnection)
				 */
				Node drawnCenter = imageCollector.paint(pc);
				if (route != null && ri != null) {
					pc.getP().forward(drawnCenter.radlat, drawnCenter.radlon, pc.lineP2);
					/*
					 * we also need to make sure the current way for the real position
					 * has really been detected by the imageCollector which happens when drawing the ways
					 * So we check if we just painted an image that did not cover
					 * the center of the display because it was too far painted off from
					 * the display center position and in this case we don't give route instructions
					 * Thus e.g. after leaving a long tunnel without gps fix there will not be given an
					 * obsolete instruction from inside the tunnel
					 */
					int maxAllowedMapMoveOffs = Math.min(pc.xSize/2, pc.ySize/2);
					if ( Math.abs(pc.lineP2.x - pc.getP().getImageCenter().x) < maxAllowedMapMoveOffs
						 &&
						 Math.abs(pc.lineP2.y - pc.getP().getImageCenter().y) < maxAllowedMapMoveOffs
					) {
						/*
						 *  we need to synchronize the route instructions on the informations determined during way painting
						 *  so we give the route instructions right after drawing the image with the map
						 *  and use the center of the last drawn image for the route instructions
						 */
						ri.showRoute(pc, drawnCenter,imageCollector.xScreenOverscan,imageCollector.yScreenOverscan);
					}
				}
			}
			
			/* Beginning of voice instructions started from overlay code (besides showRoute above)
			 */
			// Determine if we are at the destination
			if (dest != null) {
				float distance = ProjMath.getDistance(dest.lat, dest.lon, center.radlat, center.radlon);
				atDest = (distance < 25);
				if (atDest) {
					if (movedAwayFromDest && Configuration.getCfgBitState(Configuration.CFGBIT_SND_DESTREACHED)) {
						GpsMid.mNoiseMaker.playSound(RouteSyntax.getInstance().getDestReachedSound(), (byte) 7, (byte) 1);
					}
				} else if (!movedAwayFromDest) {
					movedAwayFromDest = true;
				}
			}
			// determine if we are currently speeding
			speeding = false;
			int maxSpeed = 0;
			// only detect speeding when gpsRecentered and there is a current way
			if (gpsRecenter && actualSpeedLimitWay != null) {
				maxSpeed = actualSpeedLimitWay.getMaxSpeed();
				// check for winter speed limit if configured
				if (Configuration.getCfgBitState(Configuration.CFGBIT_MAXSPEED_WINTER)
                		&& (actualSpeedLimitWay.getMaxSpeedWinter() > 0)) {
					maxSpeed = actualSpeedLimitWay.getMaxSpeedWinter();
                }
				if (maxSpeed != 0 && speed > (maxSpeed + Configuration.getSpeedTolerance()) ) {
					speeding = true;
				}
			}
			if (speeding && Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_SND)) {
				// give speeding alert only every 10 seconds
				if ( (System.currentTimeMillis() - lastTimeOfSpeedingSound) > 10000 ) {
					lastTimeOfSpeedingSound = System.currentTimeMillis();
					GpsMid.mNoiseMaker.immediateSound(RouteSyntax.getInstance().getSpeedLimitSound());
				}
			}
			/*
			 *  end of voice instructions started from overlay code
			 */
			
			/*
			 * the final part of the overlay should not give any voice instructions
			 */
			g.setColor(Legend.COLOR_MAP_TEXT);
			switch (showAddons) {
			case 1:
				yc = showMemory(g, yc, la);
				break;
			case 2:
				yc = showConnectStatistics(g, yc, la);
				break;
			default:
				showAddons = 0;
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_SCALE_BAR)) {
					tl.calcScaleBarWidth(pc);
					tl.ele[TraceLayout.SCALEBAR].setText(" ");
				}

				setPointOfTheCompass();
			}
			showMovement(g);

			// Show gpx track recording status
			LayoutElement eSolution = tl.ele[TraceLayout.SOLUTION];
			LayoutElement eRecorded = tl.ele[TraceLayout.RECORDED_COUNT];
			if (locationProducer != null) {
				eSolution.setText(solution);

				if (gpx.isRecordingTrk()) {
					// we are recording tracklogs
					if (gpx.isRecordingTrkSuspended()) {
						eRecorded.setColor(Legend.COLORS[Legend.COLOR_RECORDING_SUSPENDED_TEXT]); // blue
					} else {
						eRecorded.setColor(Legend.COLORS[Legend.COLOR_RECORDING_ON_TEXT]); // red
					}
					eRecorded.setText(gpx.getTrkPointCount() + "r");
				}
			} else {
				if (TrackPlayer.isPlaying) {
					eSolution.setText("Replay");
				} else {
					eSolution.setText("Off");
				}
			}

			LayoutElement e = tl.ele[TraceLayout.CELLID];
			// show if we are logging cellIDs
			if (SECellLocLogger.isCellIDLogging() > 0) {
				if (SECellLocLogger.isCellIDLogging() == 2) {
					e.setColor(Legend.COLORS[Legend.COLOR_CELLID_LOG_ON_TEXT]); // yellow
				} else {
					//Attempting to log, but currently no valid info available
					e.setColor(Legend.COLORS[Legend.COLOR_CELLID_LOG_ON_ATTEMPTING_TEXT]); // red
				}
				e.setText("cellIDs");
			}

			// show audio recording status
			e = tl.ele[TraceLayout.AUDIOREC];
			if (audioRec.isRecording()) {
				e.setColor(Legend.COLORS[Legend.COLOR_AUDIOREC_TEXT]); // red
				e.setText("AudioRec");
			}
			
			if (pc != null) {
				showDestination(pc);
			}

			if (speed > 0 &&
					Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_SPEED_IN_MAP)) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
					tl.ele[TraceLayout.SPEED_CURRENT].setText(" " + Integer.toString(speed) + " km/h");
				} else {
					tl.ele[TraceLayout.SPEED_CURRENT].setText(" " + Integer.toString((int)(speed / 1.609344f)) + " mph");
				}
			}
			
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_ALTITUDE_IN_MAP)
					&&
				locationProducer != null
					&&
				";off;nofix;cell;0s;~~;".indexOf(";" + solution.toLowerCase() + ";") == -1
			) {
				tl.ele[TraceLayout.ALTITUDE].setText(Integer.toString(altitude) + "m");
			}

			if (dest != null && (route == null || (!RouteLineProducer.isRouteLineProduced() && !RouteLineProducer.isRunning()) ) && Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_IN_MAP)) {
				e = Trace.tl.ele[TraceLayout.ROUTE_DISTANCE];
				e.setBackgroundColor(Legend.COLORS[Legend.COLOR_RI_DISTANCE_BACKGROUND]);
				double distLine = ProjMath.getDistance(center.radlat, center.radlon, dest.lat, dest.lon);
				e.setText("Air:" + (int) distLine + "m");
			}
			
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_CLOCK_IN_MAP)) {
				e = tl.ele[TraceLayout.CURRENT_TIME]; // e is used *twice* below (also as vRelative)
				e.setText(DateTimeTools.getClock(System.currentTimeMillis(), true));

 				/*
				don't use new Date() - it is very slow on some Nokia devices
				currentTime.setTime( new Date( System.currentTimeMillis() ) );
				e.setText(
					currentTime.get(Calendar.HOUR_OF_DAY) + ":"
					+ HelperRoutines.formatInt2(currentTime.get(Calendar.MINUTE)));
				*/

				// if current time is visible, positioning OFFROUTE above current time will work
				tl.ele[TraceLayout.ROUTE_OFFROUTE].setVRelative(e);
			}
			
			setSpeedingSign(maxSpeed);
			
			if (hasPointerEvents()) {
				tl.ele[TraceLayout.ZOOM_IN].setText("+");
				tl.ele[TraceLayout.ZOOM_OUT].setText("-");
				tl.ele[TraceLayout.RECENTER_GPS].setText("|");
				tl.ele[TraceLayout.SHOW_DEST].setText(">");
			}

			e = tl.ele[TraceLayout.TITLEBAR];
			if (currentTitleMsgOpenCount != 0) {
				e.setText(currentTitleMsg);

				// setTitleMsgTimeOut can be changed in receiveMessage()
				synchronized (this) {
					if (setTitleMsgTimeout != 0) {
						TimerTask timerT;
						Timer tm = new Timer();
						timerT = new TimerTask() {
							public synchronized void run() {
								currentTitleMsgOpenCount--;
								lastTitleMsg = currentTitleMsg;
								if (currentTitleMsgOpenCount == 0) {
									//#debug debug
									logger.debug("clearing title");
									repaint();
								}
							}
						};
						tm.schedule(timerT, setTitleMsgTimeout);
						setTitleMsgTimeout = 0;
					}
				}
			}
			
			tl.paint(g);
			
			if (currentAlertsOpenCount > 0) {
				showCurrentAlert(g);
			}
		} catch (Exception e) {
			logger.silentexception("Unhandled exception in the paint code", e);
		}
	}

	/** Show an alert telling the user that waypoints are not ready yet.
	 */
	private void showAlertLoadingWpt() {
		alert("Way points", "Way points are not ready yet.", 3000);
	}
	
	private void showCurrentAlert(Graphics g) {
		Font font = g.getFont();
		// request same font in bold for title
		Font titleFont = Font.getFont(font.getFace(), Font.STYLE_BOLD, font.getSize());
		int fontHeight = font.getHeight();
		// add alert title height plus extra space of one line for calculation of alertHeight
		int y = titleFont.getHeight() + 2 + fontHeight;
		// extra width for alert
		int extraWidth = font.charWidth('W');
		// alert is at least as wide as alert title
		int alertWidth = titleFont.stringWidth(currentAlertTitle);
		// width each alert message line must fit in
		int maxWidth = getWidth() - extraWidth;
		// Two passes: 1st pass calculates placement and necessary size of alert,
		// 2nd pass actually does the drawing
		for (int i = 0; i <= 1; i++) {
			int nextSpaceAt = 0;
			int prevSpaceAt = 0;
			int start = 0;
			// word wrap
			do {
				int width = 0;
				// add word by word until string part is too wide for screen
				while (width < maxWidth && nextSpaceAt <= currentAlertMessage.length() ) {
					prevSpaceAt = nextSpaceAt;
					nextSpaceAt = currentAlertMessage.indexOf(' ', nextSpaceAt);
					if (nextSpaceAt == -1) {
						nextSpaceAt = currentAlertMessage.length();
					}
					width = font.substringWidth(currentAlertMessage, start, nextSpaceAt - start);
					nextSpaceAt++;
				}
				nextSpaceAt--;
				// Reduce line word by word or if not possible char by char until the
				// remaining string part fits to display width
				while (width > maxWidth) {
					if (prevSpaceAt > start && nextSpaceAt > prevSpaceAt) {
						nextSpaceAt = prevSpaceAt;
					} else {
						nextSpaceAt--;
					}
					width = font.substringWidth(currentAlertMessage, start, nextSpaceAt - start);
				}
				// determine maximum alert width
				if (alertWidth < width ) {
					alertWidth = width;
				}
				// during the second pass draw the message text lines
				if (i==1) {
					g.drawSubstring(currentAlertMessage, start, nextSpaceAt - start,
							getWidth()/2, y, Graphics.TOP|Graphics.HCENTER);
				}
				y += fontHeight;
				start = nextSpaceAt;
			} while (nextSpaceAt < currentAlertMessage.length() );
			
			// at the end of the first pass draw the alert box and the alert title
			if (i == 0) {
				alertWidth += extraWidth;
				int alertHeight = y;
				int alertTop = (getHeight() - alertHeight) /2;
				//alertHeight += fontHeight/2;
				int alertLeft = (getWidth() - alertWidth) / 2;
				// alert background color
				g.setColor(Legend.COLORS[Legend.COLOR_ALERT_BACKGROUND]);
				g.fillRect(alertLeft, alertTop, alertWidth, alertHeight);
				// background color for alert title
				g.setColor(Legend.COLORS[Legend.COLOR_ALERT_TITLE_BACKGROUND]);
				g.fillRect(alertLeft, alertTop, alertWidth, fontHeight + 3);
				// alert border
				g.setColor(Legend.COLORS[Legend.COLOR_ALERT_BORDER]);
				g.setStrokeStyle(Graphics.SOLID);
				g.drawRect(alertLeft, alertTop, alertWidth, fontHeight + 3); // title border
				g.drawRect(alertLeft, alertTop, alertWidth, alertHeight); // alert border
				// draw alert title
				y = alertTop + 2; // output position of alert title
				g.setFont(titleFont);
				g.setColor(Legend.COLORS[Legend.COLOR_ALERT_TEXT]);
				g.drawString(currentAlertTitle, getWidth()/2, y , Graphics.TOP|Graphics.HCENTER);
				g.setFont(font);
				// output alert message 1.5 lines below alert title in the next pass
				y += (fontHeight * 3 / 2);
			}
		} // end for
		// setAlertTimeOut can be changed in receiveMessage()
		synchronized (this) {
			if (setAlertTimeout != 0) {
				TimerTask timerT;
				Timer tm = new Timer();
				timerT = new TimerTask() {
					public synchronized void run() {
						currentAlertsOpenCount--;
						if (currentAlertsOpenCount == 0) {
							//#debug debug
							logger.debug("clearing alert");
							repaint();
						}
					}
				};
				tm.schedule(timerT, setAlertTimeout);
				setAlertTimeout = 0;
			}
		}
	}

	private void setSpeedingSign(int maxSpeed) {
//		speeding = true;
		if (Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_VISUAL)
			&&
			(
				speeding
				||
				(System.currentTimeMillis() - startTimeOfSpeedingSign) < 3000
			)
		) {
			if (speeding) {
				speedingSpeedLimit = maxSpeed;
				startTimeOfSpeedingSign = System.currentTimeMillis();
			}
			if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				tl.ele[TraceLayout.SPEEDING_SIGN].setText(Integer.toString(speedingSpeedLimit));
			} else {
				tl.ele[TraceLayout.SPEEDING_SIGN].setText(Integer.toString((int)(speedingSpeedLimit / 1.609344f)));
			}
		} else {
			startTimeOfSpeedingSign = 0;
		}
	}

	/**
	 * 
	 */
	private void getPC() {
			pc.course = course;
			pc.scale = scale;
			pc.center = center.copy();
//			pc.setP( projection);
//			projection.inverse(pc.xSize, 0, pc.screenRU);
//			projection.inverse(0, pc.ySize, pc.screenLD);
			pc.dest = dest;
	}

	public void cleanup() {
		namesThread.cleanup();
		urlsThread.cleanup();
		tileReader.incUnusedCounter();
		dictReader.incUnusedCounter();
	}
	
//	public void searchElement(PositionMark pm) throws Exception {
//		PaintContext pc = new PaintContext(this, null);
//		// take a bigger angle for lon because of positions near to the poles.
//		Node nld = new Node(pm.lat - 0.0001f, pm.lon - 0.0005f, true);
//		Node nru = new Node(pm.lat + 0.0001f, pm.lon + 0.0005f, true);
//		pc.searchLD = nld;
//		pc.searchRU = nru;
//		pc.dest = pm;
//		pc.setP(new Proj2D(new Node(pm.lat, pm.lon, true), 5000, 100, 100));
//		for (int i = 0; i < 4; i++) {
//			tiles[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD);
//		}
//	}
	
	
	public void searchNextRoutableWay(RoutePositionMark pm) throws Exception {
		PaintContext pc = new PaintContext(this, null);
		// take a bigger angle for lon because of positions near to the pols.
//		Node nld=new Node(pm.lat - 0.0001f,pm.lon - 0.0005f,true);
//		Node nru=new Node(pm.lat + 0.0001f,pm.lon + 0.0005f,true);
//		pc.searchLD=nld;
//		pc.searchRU=nru;
		pc.squareDstToActualRoutableWay = Float.MAX_VALUE;
		pc.xSize = 100;
		pc.ySize = 100;
		// retry searching an expanding region at the position mark
		Projection p;
		do {
			p = new Proj2D(new Node(pm.lat,pm.lon, true),5000,pc.xSize,pc.ySize);
			pc.setP(p);
			for (int i=0; i<4; i++) {
				tiles[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD | Tile.OPT_FIND_CURRENT);
			}
			// stop the search when a routable way is found
			if (pc.actualRoutableWay != null) {
				break;
			}
			// expand the region that gets searched for a routable way
			pc.xSize += 100;
			pc.ySize += 100;
			// System.out.println(pc.xSize);
		} while(MoreMath.dist(p.getMinLat(), p.getMinLon(), p.getMinLat(), p.getMaxLon()) < 500); // until we searched at least 500 m edge length
		Way w = pc.actualRoutableWay;
		pm.setEntity(w, pc.currentPos.nodeLat, pc.currentPos.nodeLon);
	}
	
	private void setPointOfTheCompass() {
		String c = "";
		if (ProjFactory.getProj() != ProjFactory.NORTH_UP
				&& Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS)) {
			c = Configuration.getCompassDirection(course);
		}
		tl.ele[TraceLayout.POINT_OF_COMPASS].setText(c);
	}
	
	private int showConnectStatistics(Graphics g, int yc, int la) {
		g.setColor(Legend.COLORS[Legend.COLOR_MAP_TEXT]);
		GSMCell cell = CellIdProvider.getInstance().obtainCachedCellID();
		if (cell == null) {
			g.drawString("No Cell ID available", 0, yc, Graphics.TOP
					| Graphics.LEFT);
			yc += la;
		} else {
			g.drawString("Cell: MCC=" + cell.mcc + " MNC=" + cell.mnc, 0, yc, Graphics.TOP
					| Graphics.LEFT);
			yc += la;
			g.drawString("LAC=" + cell.lac, 0, yc, Graphics.TOP
					| Graphics.LEFT);
			yc += la;
			g.drawString("cellID=" + cell.cellID, 0, yc, Graphics.TOP
					| Graphics.LEFT);
			yc += la;
		}
		if (statRecord == null) {
			g.drawString("No stats yet", 0, yc, Graphics.TOP
					| Graphics.LEFT);
			return yc+la;
		}
		//#mdebug info
		for (byte i = 0; i < LocationMsgReceiver.SIRF_FAIL_COUNT; i++) {
			g.drawString(statMsg[i] + statRecord[i], 0, yc, Graphics.TOP
					| Graphics.LEFT);
			yc += la;
		}
		//#enddebug
		g.drawString("BtQual : " + btquality, 0, yc, Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("Count : " + collected, 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		return yc;
	}

	public void showDestination(PaintContext pc) {
		try {
		if (dest != null) {
			pc.getP().forward(dest.lat, dest.lon, pc.lineP2);
//			System.out.println(dest.toString());
			int x = pc.lineP2.x-imageCollector.xScreenOverscan;
			int y = pc.lineP2.y-imageCollector.yScreenOverscan;
			pc.g.drawImage(pc.images.IMG_DEST, x, y, CENTERPOS);
			pc.g.setColor(Legend.COLORS[Legend.COLOR_DEST_TEXT]);
			if (dest.displayName != null) {
				pc.g.drawString(dest.displayName, x, y+8,
					Graphics.TOP | Graphics.HCENTER);
			}
			pc.g.setColor(Legend.COLORS[Legend.COLOR_DEST_LINE]);
			pc.g.setStrokeStyle(Graphics.DOTTED);
			pc.g.drawLine(x, y, pc.getP().getImageCenter().x-imageCollector.xScreenOverscan, pc.getP().getImageCenter().y-imageCollector.yScreenOverscan);
		}
		} catch (Exception e) {
			if (imageCollector == null){
				System.out.println("No ImmageCollector");
			}
			e.printStackTrace();
		}
	}


	/**
	 * Draws the position square, the movement line and the center cross.
	 * 
	 * @param g Graphics context for drawing
	 */
	public void showMovement(Graphics g) {
		IntPoint centerP=null;
		try {
			if (imageCollector != null){
			g.setColor(Legend.COLORS[Legend.COLOR_MAP_CURSOR]);
			centerP = pc.getP().getImageCenter();
			int centerX = centerP.x-imageCollector.xScreenOverscan;
			int centerY = centerP.y-imageCollector.yScreenOverscan;
			int posX, posY;
			if (!gpsRecenter) {
				IntPoint p1 = new IntPoint(0, 0);
				pc.getP().forward((pos.latitude * MoreMath.FAC_DECTORAD),
								  (pos.longitude * MoreMath.FAC_DECTORAD), p1);
				posX = p1.getX();
				posY = p1.getY();
			} else {
				posX = centerX;
				posY = centerY;
			}
			pc.g.drawImage(pc.images.IMG_POS_BG, posX, posY, CENTERPOS);

			g.setColor(Legend.COLORS[Legend.COLOR_MAP_POSINDICATOR]);
			float radc = (course * MoreMath.FAC_DECTORAD);
			int px = posX + (int) (Math.sin(radc) * 20);
			int py = posY - (int) (Math.cos(radc) * 20);
			g.drawRect(posX - 4, posY - 4, 8, 8);
			g.drawLine(posX, posY, px, py);
			if (!gpsRecenter) {
				g.drawLine(centerX, centerY - 12, centerX, centerY + 12);
				g.drawLine(centerX - 12, centerY, centerX + 12, centerY);
				g.drawArc(centerX - 5, centerY - 5, 10, 10, 0, 360);
			}
			}
		} catch (Exception e) {
			if (imageCollector == null){
				System.out.println("No ImageCollector");
			}
			if (centerP == null){
				System.out.println("No centerP");
			}
			e.printStackTrace();
		}
	}
	
	/**
	 * Show next screen in the sequence of data screens
	 * (tacho, trip, satellites).
	 * @param currentScreen Data screen currently shown, use the DATASCREEN_XXX
	 *    constants from this class. Use DATASCREEN_NONE if none of them
	 *    is on screen i.e. the first one should be shown.
	 */
	public void showNextDataScreen(int currentScreen) {
		switch (currentScreen)
		{
			case DATASCREEN_TACHO:
				// Tacho is followed by Trip.
				if (guiTrip == null) {
					guiTrip = new GuiTrip(this);
				}
				if (guiTrip != null) {
					guiTrip.show();
				}
				break;
			case DATASCREEN_TRIP:
				// Trip is followed by Satellites.
				if (guiSatellites == null) {
					guiSatellites = new GuiSatellites(this, locationProducer);
				}
				if (guiSatellites != null) {
					guiSatellites.show();
				}
				break;
			case DATASCREEN_SATS:
				// After Satellites, go back to map.
				this.show();
				break;
			case DATASCREEN_NONE:
			default:
				// Tacho is first data screen
				if (guiTacho == null) {
					guiTacho = new GuiTacho(this);
				}
				if (guiTacho != null) {
					guiTacho.show();
				}
				break;
		}
	}

	public int showMemory(Graphics g, int yc, int la) {
		g.setColor(0);
		g.drawString("Freemem: " + runtime.freeMemory(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("Totmem: " + runtime.totalMemory(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("Percent: "
				+ (100f * runtime.freeMemory() / runtime.totalMemory()), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("Threads running: "
				+ Thread.activeCount(), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("Names: " + namesThread.getNameCount(), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("Single T: " + tileReader.getLivingTilesCount() + "/"
				+ tileReader.getRequestQueueSize(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("File T: " + dictReader.getLivingTilesCount() + "/"
				+ dictReader.getRequestQueueSize() + " Map: " + ImageCollector.icDuration + " ms", 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("LastMsg: " + lastTitleMsg, 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString( "at " + lastTitleMsgClock, 0, yc,
				Graphics.TOP | Graphics.LEFT );
		return (yc);

	}

	private void updatePosition() {
		if (pc != null) {
			pc.center = center.copy();
			pc.scale = scale;
			pc.course=course;
			repaint();
			
			if (locationUpdateListeners != null && !TrackPlayer.isPlaying) {
				synchronized (locationUpdateListeners) {
					for (int i = 0; i < locationUpdateListeners.size(); i++) {
						((LocationUpdateListener)locationUpdateListeners.elementAt(i)).loctionUpdated();
					}
				}
			}
			
		}
	}
	
	public synchronized void receivePosition(float lat, float lon, float scale) {
		//#debug debug
		logger.debug("Now displaying: " + (lat * MoreMath.FAC_RADTODEC) + " | " +
			     (lon * MoreMath.FAC_RADTODEC));
		//We are explicitly setting the map to this position, so we probably don't
		//want it to be recentered on the GPS immediately.
		gpsRecenter = false;
		
		center.setLatLonRad(lat, lon);
		this.scale = scale;
		updatePosition();
	}

	public static void updateLastUserActionTime() {
		lastUserActionTime = System.currentTimeMillis();
	}
	
	public static long getDurationSinceLastUserActionTime() {
		return System.currentTimeMillis() - lastUserActionTime;
	}
	
	public synchronized void receivePosition(Position pos) {
		//#debug info
		logger.info("New position: " + pos);
		this.pos = pos;
		collected++;
		if (Configuration.getAutoRecenterToGpsMilliSecs() !=0 &&
			getDurationSinceLastUserActionTime() > Configuration.getAutoRecenterToGpsMilliSecs()
			&& isShown()
		) {
			gpsRecenter = true;
			//autoZoomed = true;
		}
		if (gpsRecenter) {
			center.setLatLonDeg(pos.latitude, pos.longitude);
			if (speed > 2) {
				/*  don't rotate too fast
				 *  FIXME: the following line to not rotate too fast
				 * 	is commented out because it causes the map to perform
				 *  almost a 360 degree rotation when course and pos.course
				 *  are on different sides of North, e.g. at 359 and 1 degrees
				 */
				// course = (int) ((pos.course * 3 + course) / 4)+360;
				// use pos.course directly without rotation slow-down
				course = (int) pos.course;
				while (course > 360) {
					course -= 360;
				}
			}
		}
		speed = (int) (pos.speed * 3.6f);
		altitude = (int) (pos.altitude);
		if (gpx.isRecordingTrk()) {
			try {
				gpx.addTrkPt(pos);
			} catch (Exception e) {
				receiveMessage(e.getMessage());
			}
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_AUTOZOOM)
				&& gpsRecenter
				&& (isGpsConnected() || TrackPlayer.isPlaying)
				&& autoZoomed
				&& pc.getP() != null
		) {
			// the minimumScale at 20km/h and below is equivalent to having zoomed in manually once from the startup zoom level
			final float minimumScale = 10000;
			final int minimumSpeed = 20;
			// the maximumScale at 160km/h and above is equivalent to having zoomed out manually once from the startup zoom level
			final float maximumScale = 22500;
			final int maximumSpeed = 160;
			int speedForScale = speed;
			float newScale = minimumScale + (maximumScale - minimumScale) * (speedForScale - minimumSpeed) / (maximumSpeed - minimumSpeed);
			// make sure the new scale is within the minimum / maximum scale values
			if (newScale < minimumScale) {
				newScale = minimumScale;
			} else if (newScale > maximumScale) {
				newScale = maximumScale;
			}
			scale = newScale;
			
//			// calculate meters to top of screen
//			float topLat = pc.getP().getMaxLat();
//			float topLon = (pc.getP().getMinLon() + pc.getP().getMaxLon()) / 2f;
//			float distance = MoreMath.dist(center.radlat, center.radlon, topLat, topLon );
//			System.out.println("current distance to top of screen: " + distance);
//
//			// avoid zooming in or out too far
//			int speedForScale = course;
//			if (speedForScale < 30) {
//				speedForScale = 30;
//			} else if (speedForScale > 160) {
//				speedForScale = 160;
//			}
//
//			final float SECONDS_TO_PREVIEW = 45f;
//			float metersToPreview = (float) speedForScale / 3.6f * SECONDS_TO_PREVIEW;
//			System.out.println(metersToPreview + "meters to preview at " + speedForScale + "km/h");
//
//			if (metersToPreview < 2000) {
//			// calculate top position that needs to be visible to preview the metersToPreview
//			topLat = center.radlat + (topLat - center.radlat) * metersToPreview / distance;
//			topLon = center.radlon + (topLon - center.radlon) * metersToPreview / distance;
//			System.out.println("new distance to top:" + MoreMath.dist(center.radlat, center.radlon, topLat, topLon ));
//
//			/*
//			 *  calculate scale factor, we multiply this again with 2 * 1.2 = 2.4 to take into account we calculated half the screen height
//			 *  and 1.2f is probably the factor the PaintContext is larger than the display size
//			 *  (new scale is calculated similiar to GuiWaypoint)
//			 */
//			IntPoint intPoint1 = new IntPoint(0, 0);
//			IntPoint intPoint2 = new IntPoint(getWidth(), getHeight());
//			Node n1 = new Node(center.radlat, center.radlon, true);
//			Node n2 = new Node(topLat, topLon, true);
//			scale = pc.getP().getScale(n1, n2, intPoint1, intPoint2) * 2.4f;
//			}
							
		}
		
		updatePosition();
	}
	
	public synchronized Position getCurrentPosition() {
		return this.pos;
	}

	public synchronized void receiveMessage(String s) {
//		#debug info
		logger.info("Setting title: " + s);
		currentTitleMsg = s;
		synchronized (this) {
			/*
			 *  only increase the number of current title messages
			 *  if the timer already has been set in the display code
			 */
			if (setTitleMsgTimeout == 0) {
				currentTitleMsgOpenCount++;
			}
			setTitleMsgTimeout = 3000;
		}
		lastTitleMsgClock = DateTimeTools.getClock(System.currentTimeMillis(), false);
		repaint();
	}

	public void receiveSatellites(Satelit[] sats) {
		// Not interested
	}

	/** Shows an alert message
	 * 
	 * @param title The title of the alert
	 * @param message The message text
	 * @param timeout Timeout in ms. Please reserve enough time so the user can
	 *   actually read the message. Use Alert.FOREVER if you want no timeout.
	 */
	public synchronized void alert(String title, String message, int timeout) {
//		#debug info
		logger.info("Showing trace alert: " + title + ": " + message);
		if (timeout == Alert.FOREVER) {
			timeout = 10000;
		}
		currentAlertTitle = title;
		currentAlertMessage = message;
		synchronized (this) {
			/*
			 *  only increase the number of current open alerts
			 *  if the timer already has been set in the display code
			 */
			if (setAlertTimeout == 0) {
				currentAlertsOpenCount++;
			}
			setAlertTimeout = timeout;
		}
		repaint();
	}

	public MIDlet getParent() {
		return parent;
	}

	protected void pointerPressed(int x, int y) {
		updateLastUserActionTime();
		pointerDragAction = true;
		
		// check for touchable buttons
//		#debug debug
		logger.debug("Touch button: " + tl.getActionIdAtPointer(x, y) + " x: " + x + " y: " + y);
		int actionId = tl.getActionIdAtPointer(x, y);
		if (actionId > 0) {
			if (System.currentTimeMillis() < (lastBackLightOnTime + 1500)) {
				if (actionId == ZOOM_IN_CMD) {
					actionId = PAN_RIGHT2_CMD;
				} else if (actionId == ZOOM_OUT_CMD) {
					actionId = PAN_LEFT2_CMD;
				}
			} else if (manualRotationMode) {
				if (actionId == ZOOM_IN_CMD) {
					actionId = PAN_LEFT2_CMD;
				} else if (actionId == ZOOM_OUT_CMD) {
					actionId = PAN_RIGHT2_CMD;
				}
			} else if (TrackPlayer.isPlaying) {
				if (actionId == ZOOM_IN_CMD) {
					actionId = PAN_RIGHT2_CMD;
				} else if (actionId == ZOOM_OUT_CMD) {
					actionId = PAN_LEFT2_CMD;
				}
			}
			commandAction(CMDS[actionId], (Displayable) null);
			repaint();
			pointerDragAction = false;
		}
		
		// remember positions for dragging
		// remember position the pointer was pressed
		Trace.touchX = x;
		Trace.touchY = y;
		// remember center when the pointer was pressed
		centerPointerPressedN = center.copy();
	}
	
	protected void pointerReleased(int x, int y) {
		if (pointerDragAction) {
			pointerDragged(x , y);
		}
	}
	
	protected void pointerDragged (int x, int y) {
		updateLastUserActionTime();
		if (pointerDragAction && imageCollector != null) {
			// difference between where the pointer was pressed and is currently dragged
			int diffX = Trace.touchX - x;
			int diffY = Trace.touchY - y;
			
			IntPoint centerPointerPressedP = new IntPoint();
			imageCollector.getCurrentProjection().forward(centerPointerPressedN, centerPointerPressedP);
			imageCollector.getCurrentProjection().inverse(centerPointerPressedP.x + diffX, centerPointerPressedP.y + diffY, center);
			imageCollector.newDataReady();
			gpsRecenter = false;
		}
	}
	
	/**
	 * Returns the command used to go to the data screens.
	 * Needed by the data screens so they can find the key codes used for this
	 * as they have to use them too.
	 * @return Command
	 */
	public Command getDataScreenCommand() {
		return CMDS[DATASCREEN_CMD];
	}
	
	public Tile getDict(byte zl) {
		return tiles[zl];
	}
	
	public void setDict(Tile dict, byte zl) {
		tiles[zl] = dict;
		// Tile.trace=this;
		//addCommand(REFRESH_CMD);
//		if (zl == 3) {
//			setTitle(null);
//		} else {
//			setTitle("dict " + zl + "ready");
//		}
		if (zl == 0) {
			// read saved position from Configuration
			Configuration.getStartupPos(center);
			if (center.radlat == 0.0f && center.radlon == 0.0f) {
				// if no saved position use center of map
				dict.getCenter(center);
			}

			if (pc != null) {
				pc.center = center.copy();
				pc.scale = scale;
				pc.course = course;
			}
		}
		updatePosition();
	}

	public void receiveStatistics(int[] statRecord, byte quality) {
		this.btquality = quality;
		this.statRecord = statRecord;
		repaint();
	}

	
	public synchronized void locationDecoderEnd() {
//#debug info
		logger.info("enter locationDecoderEnd");
		if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_DISCONNECT)) {
			GpsMid.mNoiseMaker.playSound("DISCONNECT");
		}
		if (gpx != null) {
			/**
			 * Close and Save the gpx recording, to ensure we don't loose data
			 */
			gpx.saveTrk(true);
		}
		removeCommand(CMDS[DISCONNECT_GPS_CMD]);
		if (locationProducer == null) {
//#debug info
			logger.info("leave locationDecoderEnd no producer");
			return;
		}
		locationProducer = null;
		notify();
		addCommand(CMDS[CONNECT_GPS_CMD]);
//		addCommand(START_RECORD_CMD);
//#debug info
		logger.info("end locationDecoderEnd");
	}

	public void receiveSolution(String s) {
		solution = s;
		repaint();
	}

	public String getName(int idx) {
		if (idx < 0) {
			return null;
		}
		return namesThread.getName(idx);
	}
	
	public String getUrl(int idx) {
		if (idx < 0)
			return null;
		return urlsThread.getUrl(idx);
	}

	public Vector fulltextSearch (String snippet, CancelMonitorInterface cmi) {
		return namesThread.fulltextSearch(snippet, cmi);
	}

	// this is called by ImageCollector
	public void requestRedraw() {
		repaint();
	}

	public void newDataReady() {
		if (imageCollector != null) {
			imageCollector.newDataReady();
		}
	}

	public void show() {
		//Display.getDisplay(parent).setCurrent(this);
		GpsMid.getInstance().show(this);
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		updateLastUserActionTime();
		repaint();
	}
	
	public void recreateTraceLayout() {
		tl = new TraceLayout(0, 0, getWidth(), getHeight());
	}

	public void locationDecoderEnd(String msg) {
		receiveMessage(msg);
		locationDecoderEnd();
	}

	public PositionMark getDestination() {
		return dest;
	}

	public void setDestination(RoutePositionMark dest) {
		endRouting();
		this.dest = dest;
		pc.dest = dest;
		if (dest != null) {
			//#debug info
			logger.info("Setting destination to " + dest.toString());
			
			// move map only to the destination, if GUI is not optimized for routing
			if (! Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
				commandAction(CMDS[SHOW_DEST_CMD],(Displayable) null);
			}
		} else {
			//#debug info
			logger.info("Setting destination to null");
		}
	}

	public void endRouting() {
		RouteInstructions.initialRecalcDone = false;
		RouteInstructions.icCountOffRouteDetected = 0;
		RouteInstructions.routeInstructionsHeight = 0;
		RouteInstructions.abortRouteLineProduction();
		setRoute(null);
		setRouteNodes(null);
	}
	
	/**
	 * This is the callback routine if RouteCalculation is ready
	 * @param route
	 */
	public void setRoute(Vector route) {
		synchronized(this) {
			this.route = route;
		}
		if (this.route != null) {
			// reset off-route as soon as first route connection is known
			RouteInstructions.resetOffRoute(this.route, center);

			if (ri == null) {
				ri = new RouteInstructions(this);
			}
			// show map during route line production
			if (Configuration.getContinueMapWhileRouteing() == Configuration.continueMap_At_Route_Line_Creation) {
				resumeImageCollectorAfterRouteCalc();
			}
			ri.newRoute(this.route);
			oldRecalculationTime = System.currentTimeMillis();
		}
		// show map always after route calculation
		resumeImageCollectorAfterRouteCalc();
		routeCalc=false;
		routeEngine=null;
	}

	private void resumeImageCollectorAfterRouteCalc() {
		try {
			if (imageCollector == null) {
				startImageCollector();
				// imageCollector thread starts up suspended,
				// so we need to resume it
				imageCollector.resume();
			} else if (imageCollector != null) {
				imageCollector.newDataReady();
			}
			repaint();
		} catch (Exception e) {
			logger.exception("In Trace.resumeImageCollector", e);
		}
	}
	
	/**
	 * If we are running out of memory, try
	 * dropping all the caches in order to try
	 * and recover from out of memory errors.
	 * Not guaranteed to work, as it needs
	 * to allocate some memory for dropping
	 * caches.
	 */
	public void dropCache() {
		tileReader.dropCache();
		dictReader.dropCache();
		System.gc();
		namesThread.dropCache();
		urlsThread.dropCache(); 
		System.gc();
		if (gpx != null) {
			gpx.dropCache();
		}
	}
	
	public QueueDataReader getDataReader() {
		return tileReader;
	}
	
	public QueueDictReader getDictReader() {
		return dictReader;
	}

	public Vector getRouteNodes() {
		return routeNodes;
	}

	public void setRouteNodes(Vector routeNodes) {
		this.routeNodes = routeNodes;
	}
	
	protected void hideNotify() {
		//#debug debug
		logger.debug("Hide notify has been called, screen will no longer be updated");
		if (imageCollector != null) {
			imageCollector.suspend();
		}
	}
	
	protected void showNotify() {
		//#debug debug
		logger.debug("Show notify has been called, screen will be updated again");
		if (imageCollector != null) {
			imageCollector.resume();
			imageCollector.newDataReady();
		}
	}
	
	public Vector getRoute() {
		return route;
	}
	
	public void actionCompleted() {
		boolean reAddCommands = true;

		if (reAddCommands && Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN)) {
			addAllCommands();
		}
	}
	
	public void showIconMenu() {
		if (traceIconMenu == null) {
			traceIconMenu = new TraceIconMenu(this, this);
		}
		traceIconMenu.show();
	}
	
	/** uncache the icon menu to reflect changes in the setup or save memory */
	public static void uncacheIconMenu() {
		//#mdebug trace
		if (traceIconMenu != null) {
			logger.trace("uncaching TraceIconMenu");
		}
		//#enddebug
		traceIconMenu = null;
	}
	
	/** interface for IconMenuWithPages: recreate the icon menu from scratch and show it (introduced for reflecting size change of the Canvas) */
	public void recreateAndShowIconMenu() {
		uncacheIconMenu();
		showIconMenu();
	}
	
	/** interface for received actions from the IconMenu GUI */
	public void performIconAction(int actionId) {
		updateLastUserActionTime();
		// when we are low on memory or during route calculation do not cache the icon menu (including scaled images)
		if (routeCalc || GpsMid.getInstance().needsFreeingMemory()) {
			//#debug info
			logger.info("low mem: Uncaching traceIconMenu");
			uncacheIconMenu();
		}
		if (actionId != IconActionPerformer.BACK_ACTIONID) {
			commandAction(CMDS[actionId], null);
		}
	}


}
