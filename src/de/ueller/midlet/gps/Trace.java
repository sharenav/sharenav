package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

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
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.midlet.MIDlet;



import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;

import de.ueller.gps.nmea.NmeaInput;
import de.ueller.gps.sirf.SirfInput;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.gps.tools.intTree;
import de.ueller.gpsMid.mapData.DictReader;
//#if polish.api.osm-editing
import de.ueller.gpsMid.GUIosmWayDisplay;
import de.ueller.midlet.gps.data.EditableWay;
//#endif
import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueDictReader;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.Proj2D;
import de.ueller.midlet.gps.data.ProjFactory;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.names.Names;
import de.ueller.midlet.gps.routing.ConnectionWithNode;
import de.ueller.midlet.gps.routing.RouteHelper;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.routing.Routing;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.GuiMapFeatures;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.WayDescription;
import de.ueller.midlet.gps.GpsMidDisplayable;

/** 
 * Implements the main "Map" screen which displays the map, offers track recording etc. 
 * @author Harald Mueller 
 * 
 */
public class Trace extends KeyCommandCanvas implements LocationMsgReceiver,
Runnable , GpsMidDisplayable{
	/** Soft button for exiting the map screen */
	private static final int EXIT_CMD = 1;
	private static final int CONNECT_GPS_CMD = 2;
	private static final int DISCONNECT_GPS_CMD = 3;
	private static final int START_RECORD_CMD = 4;
	private static final int STOP_RECORD_CMD = 5;
	private static final int MANAGE_TRACKS_CMD = 6;
	private static final int SAVE_WAYP_CMD = 7;
	private static final int ENTER_WAYP_CMD = 8;
	private static final int MAN_WAYP_CMD = 9;
	private static final int ROUTE_TO_CMD = 10;
	private static final int CAMERA_CMD = 11;
	private static final int CLEARTARGET_CMD = 12;
	private static final int SETTARGET_CMD = 13;
	private static final int MAPFEATURES_CMD = 14;
	private static final int RECORDINGS_CMD = 16;
	private static final int ROUTINGS_CMD = 17;
	private static final int OK_CMD =18;
	private static final int BACK_CMD = 19;
	private static final int ZOOM_IN_CMD = 20;
	private static final int ZOOM_OUT_CMD = 21;
	private static final int MANUAL_ROTATION_MODE_CMD = 22;
	private static final int TOGGLE_OVERLAY_CMD = 23;
	private static final int TOGGLE_BACKLIGHT_CMD = 24;
	private static final int TOGGLE_FULLSCREEN_CMD = 25;
	private static final int TOGGLE_MAP_PROJ_CMD = 26;
	private static final int TOGGLE_KEY_LOCK_CMD = 27;
	private static final int TOGGLE_RECORDING_CMD = 28;
	private static final int TOGGLE_RECORDING_SUSP_CMD = 29;
	private static final int RECENTER_GPS_CMD = 30;
	private static final int TACHO_CMD = 31;
	private static final int OVERVIEW_MAP_CMD = 32;
	private static final int RETRIEVE_XML = 33;
	private static final int PAN_LEFT25_CMD = 34;
	private static final int PAN_RIGHT25_CMD = 35;
	private static final int PAN_UP25_CMD = 36;
	private static final int PAN_DOWN25_CMD = 37;
	private static final int PAN_LEFT2_CMD = 38;
	private static final int PAN_RIGHT2_CMD = 39;
	private static final int PAN_UP2_CMD = 40;
	private static final int PAN_DOWN2_CMD = 41;
	private static final int REFRESH_CMD = 42;
	private static final int SEARCH_CMD = 43;
	//#if polish.api.wmapi
	private static final int SEND_MESSAGE_CMD = 44;
	//#endif

	private final Command [] CMDS = new Command[45];
	
	private InputStream btGpsInputStream;
	private OutputStream btGpsOutputStream;
	private StreamConnection conn;

	
//	private SirfInput si;
	private LocationMsgProducer locationProducer;

	public String solution = "NoFix";
	
	public boolean gpsRecenter = true;
	
	private Position pos = new Position(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1,
			new Date());

	/**
	 * this node contains actually RAD coordinates
	 * although the constructor for Node(lat, lon) requires parameters in DEC format
	 * - e. g. "new Node(49.328010f, 11.352556f)"
	 */
	Node center = new Node(49.328010f, 11.352556f);

//	Projection projection;

	private final GpsMid parent;

	private String lastMsg;
	private String currentMsg;
	private Calendar lastMsgTime = Calendar.getInstance();
	
	private String currentAlertTitle;
	private String currentAlertMessage;
	private volatile int currentAlertsOpenCount; 

	private long lastBackLightOnTime = 0;
	
	private long collected = 0;

	public PaintContext pc;
	
	public float scale = 15000f;
	
	int showAddons = 0;
	private int fontHeight = 0;
	private int compassRectHeight = 0;
	
	
	// position display was touched last time
	private static int touchX = 0;
	private static int touchY = 0;
	// center when display was touched last time
	private static Node	centerPointerPressedN = new Node();
	
	public volatile boolean routeCalc=false;
	public Tile t[] = new Tile[6];
	public Way actualWay;
	PositionMark source;

	// this is only for visual debugging of the routing engine
	Vector routeNodes=new Vector();

	private long oldRecalculationTime;

	private List recordingsMenu;
	private List routingsMenu;

	private final static Logger logger = Logger.getInstance(Trace.class,Logger.DEBUG);

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

	private Satelit[] sat;

	private Image satelit;

	/** 
	 * Current speed from GPS in km/h. 
	 */
	public volatile int speed;

	/** 
	 * Flag if we're speeding
	 */
	private volatile boolean speeding=false;
	private long lastTimeOfSpeedingSound = 0;
	private long startTimeOfSpeedingSign = 0;
	private int speedingSpeedLimit = 0;
	
	/**
	 * Current course from GPS in compass degrees, 0..359.  
	 */
	private int course=0;

	public boolean atTarget=false;
	public boolean movedAwayFromTarget=true;

	private Names namesThread;

	private ImageCollector imageCollector;
	
	private QueueDataReader tileReader;

	private QueueDictReader dictReader;

	private Runtime runtime = Runtime.getRuntime();

	private PositionMark target;
	private Vector route=null;
	private RouteInstructions ri=null;
	
	private boolean running=false;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;

	public Gpx gpx;
	private AudioRecorder audioRec;
	
	private static Trace traceInstance=null;

	private Routing	routeEngine;

	/*
	private static Font smallBoldFont;
	private static int smallBoldFontHeight;
	*/
	
	private boolean manualRotationMode=false;
	
	public Vector locationUpdateListeners;
	
	public Trace(GpsMid parent) throws Exception {
		//#debug
		logger.info("init Trace");
		
		this.parent = parent;
		
		CMDS[EXIT_CMD] = new Command("Back", Command.BACK, 2);
		CMDS[REFRESH_CMD] = new Command("Refresh", Command.ITEM, 4);
		CMDS[SEARCH_CMD] = new Command("Search", Command.OK, 1);
		CMDS[CONNECT_GPS_CMD] = new Command("Start gps",Command.ITEM, 2);
		 
		CMDS[DISCONNECT_GPS_CMD] = new Command("Stop gps",Command.ITEM, 2);
		CMDS[START_RECORD_CMD] = new Command("Start record",Command.ITEM, 4);
		CMDS[STOP_RECORD_CMD] = new Command("Stop record",Command.ITEM, 4);
		CMDS[MANAGE_TRACKS_CMD] = new Command("Manage tracks",Command.ITEM, 5);
		CMDS[SAVE_WAYP_CMD] = new Command("Save waypoint",Command.ITEM, 7);
		CMDS[ENTER_WAYP_CMD] = new Command("Enter waypoint",Command.ITEM, 7);
		CMDS[MAN_WAYP_CMD] = new Command("Manage waypoints",Command.ITEM, 7);
		CMDS[ROUTE_TO_CMD] = new Command("Route",Command.ITEM, 3);
		CMDS[CAMERA_CMD] = new Command("Camera",Command.ITEM, 9);
		CMDS[CLEARTARGET_CMD] = new Command("Clear Target",Command.ITEM, 10);
		CMDS[SETTARGET_CMD] = new Command("As Target",Command.ITEM, 11);
		CMDS[MAPFEATURES_CMD] = new Command("Map Features",Command.ITEM, 12);
		CMDS[RECORDINGS_CMD] = new Command("Recordings...",Command.ITEM, 4);
		CMDS[ROUTINGS_CMD] = new Command("Routing...",Command.ITEM, 3);
		CMDS[OK_CMD] = new Command("OK",Command.OK, 14);
		CMDS[BACK_CMD] = new Command("Back",Command.BACK, 15);
		CMDS[ZOOM_IN_CMD] = new Command("Zoom in",Command.ITEM, 100);
		CMDS[ZOOM_OUT_CMD] = new Command("Zoom out",Command.ITEM, 100);
		CMDS[MANUAL_ROTATION_MODE_CMD] = new Command("Manual Rotation Mode",Command.ITEM, 100);
		CMDS[TOGGLE_OVERLAY_CMD] = new Command("Next overlay",Command.ITEM, 100);
		CMDS[TOGGLE_BACKLIGHT_CMD] = new Command("Keep backlight on/off",Command.ITEM, 100);
		CMDS[TOGGLE_FULLSCREEN_CMD] = new Command("Switch to fullscreen",Command.ITEM, 100);
		CMDS[TOGGLE_MAP_PROJ_CMD] = new Command("Next map projection",Command.ITEM, 100);
		CMDS[TOGGLE_KEY_LOCK_CMD] = new Command("(De)Activate Keylock",Command.ITEM, 100);
		CMDS[TOGGLE_RECORDING_CMD] = new Command("(De)Activate recording",Command.ITEM, 100);
		CMDS[TOGGLE_RECORDING_SUSP_CMD] = new Command("Suspend recording",Command.ITEM, 100);
		CMDS[RECENTER_GPS_CMD] = new Command("Recenter on GPS",Command.ITEM, 100);
		CMDS[TACHO_CMD] = new Command("Tacho",Command.ITEM, 100);
		CMDS[OVERVIEW_MAP_CMD] = new Command("Overview/Filter Map",Command.ITEM, 200);
		CMDS[RETRIEVE_XML] = new Command("Retrieve XML",Command.ITEM, 200);
		CMDS[PAN_LEFT25_CMD] = new Command("left 25%",Command.ITEM, 100);
		CMDS[PAN_RIGHT25_CMD] = new Command("right 25%",Command.ITEM, 100);
		CMDS[PAN_UP25_CMD] = new Command("up 25%",Command.ITEM, 100);
		CMDS[PAN_DOWN25_CMD] = new Command("down 25%",Command.ITEM, 100);
		CMDS[PAN_LEFT2_CMD] = new Command("left 2",Command.ITEM, 100);
		CMDS[PAN_RIGHT2_CMD] = new Command("right 2",Command.ITEM, 100);
		CMDS[PAN_UP2_CMD] = new Command("up 2",Command.ITEM, 100);
		CMDS[PAN_DOWN2_CMD] = new Command("down 2",Command.ITEM, 100);
		//#if polish.api.wmapi
		CMDS[SEND_MESSAGE_CMD] = new Command("Send SMS (map pos)",Command.ITEM, 200);
		//#endif

		addCommand(CMDS[EXIT_CMD]);
		addCommand(CMDS[SEARCH_CMD]);
		addCommand(CMDS[CONNECT_GPS_CMD]);
		addCommand(CMDS[MANAGE_TRACKS_CMD]);
		addCommand(CMDS[MAN_WAYP_CMD]);
		addCommand(CMDS[MAPFEATURES_CMD]);
		addCommand(CMDS[RECORDINGS_CMD]);
		addCommand(CMDS[ROUTINGS_CMD]);
		addCommand(CMDS[TACHO_CMD]);
		//#if polish.api.osm-editing
		addCommand(CMDS[RETRIEVE_XML]);
		//#endif
		setCommandListener(this);
		
		Configuration.loadKeyShortcuts(gameKeyCommand, singleKeyPressCommand, 
				repeatableKeyPressCommand, doubleKeyPressCommand, longKeyPressCommand, 
				nonReleasableKeyPressCommand, CMDS);
		
		try {
			satelit = Image.createImage("/satelit.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
	
	public static Trace getInstance() {
		return traceInstance;
	}

	// start the LocationProvider in background
	public void run() {
		try {
			if (running){
				receiveMessage("GPS starter already running");
				return;
			}

			//#debug info
			logger.info("start thread init locationprovider");
			if (locationProducer != null){
				receiveMessage("Location provider already running");
				return;
			}
			if (Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_NONE){
				receiveMessage("No location provider");
				return;
			}
			running=true;
			receiveMessage("Connect to "+Configuration.LOCATIONPROVIDER[Configuration.getLocationProvider()]);
			//		System.out.println(Configuration.getBtUrl());
			//		System.out.println(Configuration.getRender());
			switch (Configuration.getLocationProvider()){
				case Configuration.LOCATIONPROVIDER_SIRF:
				case Configuration.LOCATIONPROVIDER_NMEA:
					//#debug debug
					logger.debug("Connect to "+Configuration.getBtUrl());
					if (! openBtConnection(Configuration.getBtUrl())){
						running=false;
						return;
					}
					receiveMessage("BT Connected");
			}
			switch (Configuration.getLocationProvider()){
				case Configuration.LOCATIONPROVIDER_SIRF:
					locationProducer = new SirfInput();
					break;
				case Configuration.LOCATIONPROVIDER_NMEA:
					locationProducer = new NmeaInput();
					break;
				case Configuration.LOCATIONPROVIDER_JSR179:
					//#if polish.api.locationapi
					try {
						String jsr179Version = null;
						try {
							jsr179Version = System.getProperty("microedition.location.version");
						} catch (RuntimeException re) {
							/**
							 * Some phones throw exceptions if trying to access properties that don't
							 * exist, so we have to catch these and just ignore them.
							 */
						} catch (Exception e) {
							/**
							 * See above 
							 */				
						}
						if (jsr179Version != null && jsr179Version.length() > 0) {
							Class jsr179Class = Class.forName("de.ueller.gps.jsr179.JSR179Input");
							locationProducer = (LocationMsgProducer) jsr179Class.newInstance();
						}
					} catch (ClassNotFoundException cnfe) {
						locationDecoderEnd();
						logger.exception("Your phone does not support JSR179, please use a different location provider", cnfe);
						running = false;
						return;
					}
					//#else
					// keep eclipse happy 
					if (true){
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
			if (Configuration.getGpsRawLoggerEnable() && (url != null)) {
				try {
					logger.info("Raw Location logging to: " + url);
					url += "rawGpsLog" + HelperRoutines.formatSimpleDateNow() + ".txt";

					javax.microedition.io.Connection logCon = Connector.open(url);				
					if (logCon instanceof FileConnection) {
						FileConnection fileCon = (FileConnection)logCon;
						if (!fileCon.exists())
							fileCon.create();
						locationProducer.enableRawLogging(((FileConnection)logCon).openOutputStream());
					} else {
						logger.info("Trying to perform raw logging of NMEA on anything else than filesystem is currently not supported");
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
			locationProducer.init(btGpsInputStream, btGpsOutputStream, this);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT)) {
				parent.mNoiseMaker.playSound("CONNECT");
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
	
	public synchronized void pause(){
		logger.debug("Pausing application");
		if (imageCollector != null) {
			imageCollector.suspend();
		}
		if (locationProducer != null){
			locationProducer.close();
		} else {
			return;
		}
		while (locationProducer != null){
			try {
				wait(200);
			} catch (InterruptedException e) {
			}
		}		
	}

	public void resume(){
		logger.debug("resuming application");
		if (imageCollector != null) {
			imageCollector.resume();
		}
		Thread thread = new Thread(this);
		thread.start();
	}


	private boolean openBtConnection(String url){
		if (btGpsInputStream != null){
			return true;
		}
		if (url == null)
			return false;
		try {
			conn = (StreamConnection) Connector.open(url);
			btGpsInputStream = conn.openInputStream();
			/**
			 * There is at least one, perhaps more BT gps receivers, that
			 * seem to kill the bluetooth connection if we don't send it
			 * something for some reason. Perhaps due to poor powermanagment?
			 * We don't have anything to send, so send an arbitrary 0.
			 */
			if (Configuration.getBtKeepAlive()) {
				btGpsOutputStream = conn.openOutputStream();								
			}
			
		} catch (SecurityException se) {
			/**
			 * The application was not permitted to connect to bluetooth  
			 */
			receiveMessage("Connectiong to BT not permitted");
			return false;
			
		} catch (IOException e) {
			receiveMessage("err BT:"+e.getMessage());
			return false;
		}
		return true;
	}
	
	private void closeBtConnection() {
		if (btGpsInputStream != null){
			try {
				btGpsInputStream.close();
			} catch (IOException e) {
			}
			btGpsInputStream=null;
		}
		if (btGpsOutputStream != null){
			try {
				btGpsOutputStream.close();
			} catch (IOException e) {
			}
			btGpsOutputStream=null;
		}
		if (conn != null){
			try {
				conn.close();
			} catch (IOException e) {
			}
			conn=null;			
		}		
	}
	
	/**
	 * This function tries to reconnect to the bluetooth
	 * it retries for up to 40 seconds and blocks in the
	 * mean time, so this function has to be called from
	 * within a separate thread. If successful, it will
	 * reinitialise the location producer with the new
	 * streams.
	 * 
	 * @return whether the reconnect was successful
	 */
	public boolean autoReconnectBtConnection() {
		if (!Configuration.getBtAutoRecon()) {
			logger.info("Not trying to reconnect");
			return false;
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_DISCONNECT)) {
			parent.mNoiseMaker.playSound("DISCONNECT");			
		}
		/**
		 * If there are still parts of the old connection
		 * left over, close these cleanly.
		 */
		closeBtConnection();
		int reconnectFailures = 0;
		while ((reconnectFailures < 4) && (! openBtConnection(Configuration.getBtUrl()))){
			reconnectFailures++;
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				return false;
			}
		}
		if (reconnectFailures < 4) {
			if (locationProducer != null) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT)) {
					parent.mNoiseMaker.playSound("CONNECT");
				}
				locationProducer.init(btGpsInputStream, btGpsOutputStream, this);
				return true;
			}
		}
		return false;
	}

	public void autoRouteRecalculate() {
		if ( gpsRecenter && Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AUTO_RECALC) ) {
			if (Math.abs(System.currentTimeMillis()-oldRecalculationTime) >= 7000 ) {
				// if map is gps-centered recalculate route
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
					GpsMid.mNoiseMaker.playSound("ROUTE_RECALCULATION", (byte) 5, (byte) 1 );
				}
				commandAction(CMDS[ROUTE_TO_CMD],(Displayable) null);
			}
		}
	}
	
	public void commandAction(Command c, Displayable d) {
		try {
			if((keyboardLocked) && (d != null)) {
				// show alert in keypressed() that keyboard is locked
				keyPressed(0);
				return;
			}
			if ((c == CMDS[PAN_LEFT25_CMD]) || (c == CMDS[PAN_RIGHT25_CMD]) || (c == CMDS[PAN_UP25_CMD]) || (c == CMDS[PAN_DOWN25_CMD])
					|| (c == CMDS[PAN_LEFT2_CMD]) || (c == CMDS[PAN_RIGHT2_CMD]) || (c == CMDS[PAN_UP2_CMD]) || (c == CMDS[PAN_DOWN2_CMD])) {
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
					if (manualRotationMode) {
						courseDiff=-5;
					} else {
						panX = -2;
					}
					backLightLevelDiff = -25;
				} else if (c == CMDS[PAN_RIGHT2_CMD]) {
					if (manualRotationMode) {
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
				
				if (backLightLevelDiff !=0  &&  System.currentTimeMillis() < (lastBackLightOnTime + 1000)) { 
					// turn backlight always on when dimming
					Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON, true, false);
					lastBackLightOnTime = System.currentTimeMillis();
					parent.addToBackLightLevel(backLightLevelDiff);
					parent.showBackLightLevel();
				} else {
					imageCollector.getCurrentProjection().pan(center, panX, panY);
					if (courseDiff == 360) {
						course = 0; //N
					} else {
						course += courseDiff;
						course %= 360;
						if (course < 0) {
							course += 360;
						}
					}
					gpsRecenter = false;
				}
				return;
			}
			if (c == CMDS[EXIT_CMD]) {
				// FIXME: This is a workaround. It would be better if recording would not be stopped when returning to map
				if (gpx.isRecordingTrk()) {
					parent.alert("Record Mode", "Please stop recording before returning to the main screen." , 2000);
					return;
				}
				
				if (locationProducer != null){
					locationProducer.close();
				}
				
				// shutdown();
				pause();
				parent.show();
				return;
			}
			if (! routeCalc){
			if (c == CMDS[START_RECORD_CMD]){
				try {
					gpx.newTrk();
				} catch (RuntimeException e) {
					receiveMessage(e.getMessage());
				}
			}
			if (c == CMDS[STOP_RECORD_CMD]){
				gpx.saveTrk();
				addCommand(CMDS[MANAGE_TRACKS_CMD]);
			}
			if (c == CMDS[MANAGE_TRACKS_CMD]){
				if (gpx.isRecordingTrk()) {
					parent.alert("Record Mode", "You need to stop recording before managing tracks." , 2000);
					return;
				}

			    GuiGpx gpx = new GuiGpx(this);
			    gpx.show();
			}
			if (c == CMDS[REFRESH_CMD]) {
				repaint();
			}
			if (c == CMDS[CONNECT_GPS_CMD]){
				if (locationProducer == null){
					Thread thread = new Thread(this);
					thread.start();
				}
			}
			if (c == CMDS[SEARCH_CMD]){
				GuiSearch search = new GuiSearch(this);
				search.show();
			}
			if (c == CMDS[DISCONNECT_GPS_CMD]){
				if (locationProducer != null){
					locationProducer.close();
				}
			}
			if (c == CMDS[ROUTE_TO_CMD]){
				routeCalc = true; 
				if (Configuration.isStopAllWhileRouteing()){
  				   stopImageCollector();
				}
				RouteInstructions.resetOffRoute(route, center);
				logger.info("Routing source: " + source);
				routeNodes=new Vector();
				routeEngine = new Routing(t,this);
				routeEngine.solve(source, target);
//				resume();
			}
			if (c == CMDS[SAVE_WAYP_CMD]) {
				GuiWaypointSave gwps = new GuiWaypointSave(this, new PositionMark(center.radlat, center.radlon));
				gwps.show();
			}
			if (c == CMDS[ENTER_WAYP_CMD]) {
				GuiWaypointEnter gwpe = new GuiWaypointEnter(this);
				gwpe.show();
			}
			if (c == CMDS[MAN_WAYP_CMD]) {
				GuiWaypoint gwp = new GuiWaypoint(this);
				gwp.show();
			}
			if (c == CMDS[MAPFEATURES_CMD]) {
				GuiMapFeatures gmf = new GuiMapFeatures(this);
				gmf.show();
				repaint();
			}
			if (c == CMDS[OVERVIEW_MAP_CMD]) {
				GuiOverviewElements ovEl = new GuiOverviewElements(this);
				ovEl.show();
				repaint();
			}
			//#if polish.api.wmapi
			if (c == CMDS[SEND_MESSAGE_CMD]) {
				GuiSendMessage sendMsg = new GuiSendMessage(this);
				sendMsg.show();
				repaint();
			}
			//#endif
			if (c == CMDS[RECORDINGS_CMD]) {
				int noElements = 4;
				//#if polish.api.mmapi
				noElements = 6;
				//#endif
				String[] elements = new String[noElements];
				if (gpx.isRecordingTrk()) {
					elements[0] = "Stop Gpx tracklog";
				} else {
					elements[0] = "Start Gpx tracklog";
				}
				elements[1] = "Add waypoint";
				elements[2] = "Enter waypoint";
				//#if polish.api.mmapi
				elements[3] = "Take pictures";
				if (audioRec.isRecording()) {
					elements[4] = "Stop audio recording";
				} else {
					elements[4] = "Start audio recording";					
					//#if polish.api.wmapi
					if (Configuration.hasDeviceJSR120()) {
						elements[5] = "Send SMS (map pos)";									
					}
					//#endif
				}				
				//#else
					//#if polish.api.wmapi
					if (Configuration.hasDeviceJSR120()) {
						elements[3] = "Send SMS (map pos)";									
					}
					//#endif
				//#endif
				
				recordingsMenu = new List("Recordings...",Choice.IMPLICIT,elements,null);
				recordingsMenu.addCommand(CMDS[OK_CMD]);
				recordingsMenu.addCommand(CMDS[BACK_CMD]);
				recordingsMenu.setSelectCommand(CMDS[OK_CMD]);
				parent.show(recordingsMenu);
				recordingsMenu.setCommandListener(this);				
			}
			if (c == CMDS[ROUTINGS_CMD]) {
				String[] elements = {"Calculate route", "Set target" , "Clear target"};					
				routingsMenu = new List("Routing..",Choice.IMPLICIT,elements,null);
				routingsMenu.addCommand(CMDS[OK_CMD]);
				routingsMenu.addCommand(CMDS[BACK_CMD]);
				routingsMenu.setSelectCommand(CMDS[OK_CMD]);
				parent.show(routingsMenu);
				routingsMenu.setCommandListener(this);				
			}
			if (c == CMDS[BACK_CMD]) {
				show();
			}
			if (c == CMDS[OK_CMD]) {
				if (d == recordingsMenu) {
					 switch (recordingsMenu.getSelectedIndex()) {
			            case 0: {
			            	if (!gpx.isRecordingTrk()){			    				
			    					gpx.newTrk();
			    			} else {			    			
			    				gpx.saveTrk();			    				
			    			}
			            	show();
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
			            	if (audioRec.isRecording()) {
			            		audioRec.stopRecord();
			            	} else {
			            		audioRec.startRecorder();
			            	}			            	
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
						commandAction(CMDS[ROUTE_TO_CMD], null);
						break;
					}
					case 1: {
						commandAction(CMDS[SETTARGET_CMD], null);
						break;
					}
					case 2: {
						commandAction(CMDS[CLEARTARGET_CMD], null);
						break;
					}			            	
					}
				}
			}
			//#if polish.api.mmapi
			if (c == CMDS[CAMERA_CMD]){
				try {
					Class GuiCameraClass = Class.forName("de.ueller.midlet.gps.GuiCamera");
					Object GuiCameraObject = GuiCameraClass.newInstance();					
					GuiCameraInterface cam = (GuiCameraInterface)GuiCameraObject;
					cam.init(this);
					cam.show();
				} catch (ClassNotFoundException cnfe) {
					logger.exception("Your phone does not support the necessary JSRs to use the camera", cnfe);
				}
				
			}
			//#endif
			if (c == CMDS[CLEARTARGET_CMD]) {
				setTarget(null);
			} else if (c == CMDS[SETTARGET_CMD]) {
				if (source != null) {
					setTarget(source);
				}
			} else if (c == CMDS[ZOOM_IN_CMD]) {
				scale = scale / 1.5f;
			} else if (c == CMDS[ZOOM_OUT_CMD]) {
				scale = scale * 1.5f;
			} else if (c == CMDS[MANUAL_ROTATION_MODE_CMD]) {
				manualRotationMode = !manualRotationMode;
				if (manualRotationMode) {
					parent.alert("Manual Rotation", "Change course with left/right keys" , 750);
				} else {
					parent.alert("Manual Rotation", "Off" , 500);
				}
			} else if (c == CMDS[TOGGLE_OVERLAY_CMD]) {
				showAddons++;
			} else if (c == CMDS[TOGGLE_BACKLIGHT_CMD]) {
//				 toggle Backlight
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON,
									!(Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON)),
									false);
				lastBackLightOnTime = System.currentTimeMillis(); 
				parent.showBackLightLevel();
			} else if (c == CMDS[TOGGLE_FULLSCREEN_CMD]) {
				boolean fullScreen = !Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN);
				Configuration.setCfgBitState(Configuration.CFGBIT_FULLSCREEN, fullScreen, false);
				setFullScreenMode(fullScreen);
			} else if (c == CMDS[TOGGLE_MAP_PROJ_CMD]) {
				if (ProjFactory.getProj() == ProjFactory.NORTH_UP ) {
					ProjFactory.setProj(ProjFactory.MOVE_UP);
					parent.alert("Map Rotation", "Rotate to Driving Direction" , 500);
				} else {
					if (manualRotationMode) {
						course = 0;
						parent.alert("Manual Rotation", "to North" , 500);
					} else {
						ProjFactory.setProj(ProjFactory.NORTH_UP);					
						parent.alert("Map Rotation", "NORTH UP" , 500);
					}
				}
				// redraw immediately
				synchronized (this) {
					if (imageCollector != null) {
						imageCollector.newDataReady();
					}
				}
			} else if (c == CMDS[TOGGLE_KEY_LOCK_CMD]) {
				keyboardLocked=!keyboardLocked;
				if(keyboardLocked) {
					// show alert that keys are locked
					keyPressed(0);
				} else {
					parent.alert("GpsMid", "Keys unlocked",750);					
				}
			} else if (c == CMDS[TOGGLE_RECORDING_CMD]) {
				if ( gpx.isRecordingTrk() ) {
					parent.alert("Gps track recording", "Stopping to record" , 750);                                        
					commandAction(CMDS[STOP_RECORD_CMD],(Displayable) null);
				} else {
					parent.alert("Gps track recording", "Starting to record" , 750);
					commandAction(CMDS[START_RECORD_CMD],(Displayable) null);
				}
			} else if (c == CMDS[TOGGLE_RECORDING_SUSP_CMD]) {
				if (gpx.isRecordingTrk()) {
					if ( gpx.isRecordingTrkSuspended() ) {
						parent.alert("Gps track recording", "Resuming recording" , 750);
						gpx.resumTrk();
					} else {
						parent.alert("Gps track recording", "Suspending recording" , 750);
						gpx.suspendTrk();
					}
				}
			} else if (c == CMDS[RECENTER_GPS_CMD]) {
				gpsRecenter = true;
				newDataReady();
			} else if (c == CMDS[TACHO_CMD]) {
				GuiTacho tacho = new GuiTacho(this);
				tacho.show();
			}
			//#if polish.api.osm-editing 
				else if (c == CMDS[RETRIEVE_XML]) {
					if (C.enableEdits) {
						if ((pc.actualWay != null) && (pc.actualWay instanceof EditableWay)) {
							EditableWay eway = (EditableWay)pc.actualWay;
							GUIosmWayDisplay guiWay = new GUIosmWayDisplay(eway, pc.actualSingleTile, this);
							guiWay.show();
							guiWay.refresh();
						}
					} else {
						parent.alert("Editing", "Editing support was not enabled in Osm2GpsMid", 2000);
					}
			}
			//#endif
			} else {
				logger.error(" currently in route Caclulation");
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private void startImageCollector() throws Exception {
		//#debug info
		logger.info("Starting ImageCollector");
		Images i;
		i = new Images();
		pc = new PaintContext(this, i);
		pc.c = parent.c;
		int w = (this.getWidth() * 125) / 100;
		int h = (this.getHeight() * 125) / 100;
		imageCollector = new ImageCollector(t, w, h, this,
				i, pc.c);
//		projection = ProjFactory.getInstance(center,course, scale, getWidth(), getHeight());
//		pc.setP(projection);
		pc.center = center.clone();
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
		new DictReader(this);
//		Thread thread = new Thread(this);
//		thread.start();
//		logger.info("Create queueDataReader");
		tileReader = new QueueDataReader(this);
//		logger.info("create imageCollector");
		dictReader = new QueueDictReader(this);
		this.gpx = new Gpx();
		this.audioRec = new AudioRecorder();
		setDict(gpx, (byte)5);
		startImageCollector();
		
	}

	public void shutdown() {
		try {
			stopImageCollector();
			if (btGpsInputStream != null) {
				btGpsInputStream.close();
				btGpsInputStream = null;
			}
			if (namesThread != null) {
				namesThread.stop();
				namesThread = null;
			}
			if (dictReader != null) {
				dictReader.shutdown();
				dictReader = null;
			}
			if (tileReader != null) {
				tileReader.shutdown();
				tileReader = null;
			}

		} catch (IOException e) {
		}
		if (locationProducer != null){
			locationProducer.close();
		}

	}
	
	protected void sizeChanged(int w, int h) {
		if (imageCollector != null){
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
	}


	protected void paint(Graphics g) {
		//#debug debug
		logger.debug("Drawing Map screen");
		
		try {
			int yc = 1;
			int la = 18;
			getPC();
			// cleans the screen
			g.setColor(C.BACKGROUND_COLOR);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
			pc.g = g;
			if (imageCollector != null){				
				/*
				 *  When painting we receive a copy of the center coordinates
				 *  where the imageCollector has drawn last
				 *  as we need to base the routing instructions on the information
				 *  determined during the way drawing (e.g. the current routePathConnection)
				 */
				Node drawnCenter = imageCollector.paint(pc);
				if (route != null && ri!=null) {
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
					if ( Math.abs(pc.lineP2.x - pc.xSize/2) < maxAllowedMapMoveOffs
						 &&
						 Math.abs(pc.lineP2.y - pc.ySize/2) < maxAllowedMapMoveOffs
					) {
						int yPos=pc.ySize;
						yPos-=imageCollector.statusFontHeight;
						/*
						 *  we need to synchronize the route instructions on the informations determined during way painting
						 *  so we give the route instructions right after drawing the image with the map
						 *  and use the center of the last drawn image for the route instructions
						 */
						ri.showRoute(pc, source, drawnCenter, yPos);
					}
				}
			}
			switch (showAddons) {
			case 1:
				showScale(pc);				
				break;
			case 2:
				yc = showSpeed(g, yc, la);
				yc = showDistanceToTarget(g, yc, la);
				break;
			case 3:
				showSatelite(g);
				break;
			case 4:
				yc = showConnectStatistics(g, yc, la);
				break;
			case 5:
				yc = showMemory(g, yc, la);
				break;
			default:
				showAddons = 0;
				if (ProjFactory.getProj() == ProjFactory.MOVE_UP
					&& Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS)
				) {
					showPointOfTheCompass(pc);
				}
			}
			showMovement(g);
			g.setColor(0, 0, 0);
			if (locationProducer != null){
				if (gpx.isRecordingTrk()) {// we are recording tracklogs
					if(fontHeight==0) {
						fontHeight=g.getFont().getHeight();
					}
					if (gpx.isRecordingTrkSuspended()) {
						g.setColor(0, 0, 255);
					} else {
						g.setColor(255, 0, 0);
					}
					
					g.drawString(gpx.recorded+"r", getWidth() - 1, 1+fontHeight, Graphics.TOP
							| Graphics.RIGHT);
					g.setColor(0);
				}
					
				g.drawString(solution, getWidth() - 1, 1, Graphics.TOP
							| Graphics.RIGHT);
			} else {
				g.drawString("Off", getWidth() - 1, 1, Graphics.TOP
						| Graphics.RIGHT);
				
			}
			if (pc != null){
				showTarget(pc);
			}
			
			// determine if we are currently speeding
			speeding = false;
			int maxSpeed = 0;
			if (actualWay != null) {
				maxSpeed = actualWay.getMaxSpeed();
				if (maxSpeed != 0 && speed > (maxSpeed + Configuration.getSpeedTolerance()) ) {
					speeding = true;
				}
			}
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_VISUAL)
				&& (
					speeding
					||
					(System.currentTimeMillis() - startTimeOfSpeedingSign) < 3000
				)
			) {
				if (speeding) {
					startTimeOfSpeedingSign = System.currentTimeMillis();
					speedingSpeedLimit = maxSpeed;
				}
				
				String sSpeed = Integer.toString(speedingSpeedLimit);
				int oldColor = g.getColor();
				Font oldFont = g.getFont();
				Font speedingFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
				int w = speedingFont.stringWidth(sSpeed);
				int w0 = speedingFont.charWidth('0');
				int h0 = speedingFont.getHeight();
				w += w0 * 4;
				g.setColor(0x00FF0000);
				int yPos = pc.ySize - w - (h0 / 2) - imageCollector.statusFontHeight - RouteInstructions.routeInstructionsHeight;
				g.fillArc(0, yPos, w, w, 0, 360);
				g.setColor(0x00FFFFFF);
				g.fillArc(w0, yPos + w0, w - (w0 * 2), w - (w0 * 2), 0, 360);
				g.setColor(0x00000000);
				g.setFont(speedingFont);
				g.drawString(sSpeed, w/2, yPos + w/2 - (h0 / 2), Graphics.TOP | Graphics.HCENTER);
				g.setFont(oldFont);
				g.setColor(oldColor);
			} else {
				startTimeOfSpeedingSign = 0;
			}
			
			if (speeding && Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_SND)) {
				// give speeding alert only every 10 seconds
				if ( (System.currentTimeMillis() - lastTimeOfSpeedingSound) > 10000 ) {
					lastTimeOfSpeedingSound = System.currentTimeMillis();
					parent.mNoiseMaker.immediateSound("SPEED_LIMIT");					
				}
			}

			if (currentMsg != null) {
				if (compassRectHeight == 0) {
					compassRectHeight = g.getFont().getHeight()-2;
				}
				g.setColor(255,255,255);
				g.fillRect(0,0, getWidth(), compassRectHeight + 2);
				g.setColor(0,0,0);
				if (g.getFont().stringWidth(currentMsg) < getWidth()) {
					g.drawString(currentMsg, 
							getWidth()/2, 0, Graphics.TOP|Graphics.HCENTER);
				} else {
					g.drawString(currentMsg, 
							0, 0, Graphics.TOP|Graphics.LEFT);					
				}
				
				if (System.currentTimeMillis() 
						> (lastMsgTime.getTime().getTime() + 5000)) {			
					logger.info("Setting title back to null");
					lastMsg = currentMsg;
					currentMsg = null;
				}
			}
			
			if (currentAlertsOpenCount > 0) {
				Font font = g.getFont();
				// request same font in bold for title
				Font titleFont = Font.getFont(font.getFace(), Font.STYLE_BOLD, font.getSize());
				int fontHeight = font.getHeight();
				int y = titleFont.getHeight() + 2 + fontHeight; // add alert title height plus extra space of one line for calculation of alertHeight
				int extraWidth = font.charWidth('W'); // extra width for alert
				int alertWidth = titleFont.stringWidth(currentAlertTitle); // alert is at least as wide as alert title
				int maxWidth = getWidth() - extraWidth; // width each alert message line must fit in
				for (int i = 0; i<=1; i++) { // two passes: 1st pass calculates placement and necessary size of alert, 2nd pass actually does the drawing
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
						// reduce line word by word or if not possible char by char until the remaining string part fits to display width
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
					if (i==0) {
						alertWidth += extraWidth;
						int alertHeight = y;
						int alertTop = (getHeight() - alertHeight) /2;
						//alertHeight += fontHeight/2;
						int alertLeft = (getWidth() - alertWidth) / 2;
						// alert background color
						g.setColor(222, 222, 222);
						g.fillRect(alertLeft, alertTop , alertWidth, alertHeight);
						// background color for alert title
						pc.g.setColor(255, 255, 255); 
						g.fillRect(alertLeft, alertTop , alertWidth, fontHeight + 3);
						// alert border
						g.setColor(0, 0, 0);
						g.setStrokeStyle(Graphics.SOLID);
						g.drawRect(alertLeft, alertTop, alertWidth, fontHeight + 3); // title border
						g.drawRect(alertLeft, alertTop, alertWidth, alertHeight); // alert border
						// draw alert title
						y = alertTop + 2; // output position of alert title
						g.setFont(titleFont);
						g.drawString(currentAlertTitle, getWidth()/2, y , Graphics.TOP|Graphics.HCENTER);
						g.setFont(font);
						// output alert message 1.5 lines below alert title in the next pass
						y += (fontHeight * 3 / 2); 
					}
				} // end for
			}
			
		} catch (Exception e) {
			logger.silentexception("Unhandled exception in the paint code", e);
		}
	}

	/**
	 * 
	 */
	private void getPC() {
			pc.course=course;
			pc.scale=scale;
			pc.center=center.clone();
//			pc.setP( projection);
//			projection.inverse(pc.xSize, 0, pc.screenRU);
//			projection.inverse(0, pc.ySize, pc.screenLD);
			pc.target=target;
	}

	public void cleanup() {
		namesThread.cleanup();
		tileReader.incUnusedCounter();
		dictReader.incUnusedCounter();		
	}
	
	public void searchElement(PositionMark pm) throws Exception{
		PaintContext pc = new PaintContext(this, null);
		// take a bigger angle for lon because of positions near to the pols.
		Node nld=new Node(pm.lat - 0.0001f,pm.lon - 0.0005f,true);
		Node nru=new Node(pm.lat + 0.0001f,pm.lon + 0.0005f,true);		
		pc.searchLD=nld;
		pc.searchRU=nru;
		pc.target=pm;
		pc.setP(new Proj2D(new Node(pm.lat,pm.lon, true),5000,100,100));
		for (int i=0; i<4; i++){
			t[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD);

		}
	}
	
	
	public void searchNextRoutableWay(PositionMark pm) throws Exception{
		PaintContext pc = new PaintContext(this, null);
		// take a bigger angle for lon because of positions near to the pols.
		Node nld=new Node(pm.lat - 0.0001f,pm.lon - 0.0005f,true);
		Node nru=new Node(pm.lat + 0.0001f,pm.lon + 0.0005f,true);
		pc.searchLD=nld;
		pc.searchRU=nru;
		pc.squareDstToRoutableWay = Float.MAX_VALUE;
		pc.xSize = 100;
		pc.ySize = 100;
		pc.setP(new Proj2D(new Node(pm.lat,pm.lon, true),5000,100,100));
		for (int i=0; i<4; i++){
			t[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD | Tile.OPT_FIND_CURRENT);
		}
		Way w = pc.nearestRoutableWay;
		pm.setEntity(w, pc.currentPos.nodeLat, pc.currentPos.nodeLon);
	}
	
	private void showPointOfTheCompass(PaintContext pc) {
		if (compassRectHeight == 0) {
			compassRectHeight = pc.g.getFont().getHeight()-2;
		}
		String c = Configuration.getCompassDirection(course);
		int compassRectWidth = pc.g.getFont().stringWidth(c);
		pc.g.setColor(255, 255, 150); 
		pc.g.fillRect(getWidth()/2 - compassRectWidth / 2 , 0,
					  compassRectWidth, compassRectHeight);
		pc.g.setColor(0, 0, 0); 
		pc.g.drawString(c, getWidth()/2,  0 , Graphics.HCENTER | Graphics.TOP);
	}
	
	private int showConnectStatistics(Graphics g, int yc, int la) {
		if (statRecord == null) {
			g.drawString("No stats yet", 0, yc, Graphics.TOP
					| Graphics.LEFT);
			return yc+la;
		}
		g.setColor(0, 0, 0);
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

	private void showSatelite(Graphics g) {
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		int dia = Math.min(getWidth(), getHeight()) - 6;
		int r = dia / 2;
		g.setColor(255, 50, 50);
		g.drawArc(centerX - r, centerY - r, dia, dia, 0, 360);
		if (sat == null) return;
		for (byte i = 0; i < sat.length; i++) {			
			Satelit s = sat[i];
			if (s == null)
				continue; //This array may be sparsely filled.
			if (s.id != 0) {
				double el = s.elev / 180d * Math.PI;
				double az = s.azimut / 180 * Math.PI;
				double sr = r * Math.cos(el);
				if (s.isLocked())
					g.setColor(0, 255, 0);
				else
					g.setColor(255, 0, 0);
				
				int px = centerX + (int) (Math.sin(az) * sr);
				int py = centerY - (int) (Math.cos(az) * sr);
				// g.drawString(""+s.id, px, py,
				// Graphics.BASELINE|Graphics.HCENTER);
				g.drawImage(satelit, px, py, Graphics.HCENTER
						| Graphics.VCENTER);
				py += 9;
				// draw a bar under image that indicates green/red status and
				// signal strength
				g.fillRect(px - 9, py, (int)(s.snr*18.0/100.0), 2);				
			}
		}
		// g.drawImage(satelit, 5, 5, 0);
	}

	public void showTarget(PaintContext pc){
		if (target != null){
			pc.getP().forward(target.lat, target.lon, pc.lineP2);
//			System.out.println(target.toString());
			pc.g.drawImage(pc.images.IMG_TARGET,pc.lineP2.x,pc.lineP2.y,CENTERPOS);
			pc.g.setColor(0,0,0);
			if (target.displayName != null)
				pc.g.drawString(target.displayName, pc.lineP2.x, pc.lineP2.y+8,
					Graphics.TOP | Graphics.HCENTER);
			pc.g.setColor(255,50,50);
			pc.g.setStrokeStyle(Graphics.DOTTED);
			pc.g.drawLine(pc.lineP2.x,pc.lineP2.y,pc.xSize/2,pc.ySize/2);
			float distance = ProjMath.getDistance(target.lat, target.lon, center.radlat, center.radlon);
			atTarget = (distance < 25);
			if (atTarget) {
				if (movedAwayFromTarget && Configuration.getCfgBitState(Configuration.CFGBIT_SND_TARGETREACHED)) {
					parent.mNoiseMaker.playSound("TARGET_REACHED", (byte) 7, (byte) 1);
				}
			} else if (!movedAwayFromTarget) {
				movedAwayFromTarget=true;
			}
		}

	}

	/**
	 * Draws a map scale onto screen.
	 * This calculation is currently horribly
	 * inefficient. There must be a better way
	 * than this.
	 * 
	 * @param pc
	 */
	public void showScale(PaintContext pc) {
		Node n1 = new Node();
		Node n2 = new Node();
		
		float scale;
		int scalePx;
		
		//Calculate the lat and lon coordinates of two
		//points that are 35 pixels apart
		pc.getP().inverse(10, 10, n1);
		pc.getP().inverse(45, 10, n2);
		
		//Calculate the distance between them in meters
		float d = ProjMath.getDistance(n1, n2);
		//round this distance up to the nearest 5 or 10
		int ordMag = (int)(MoreMath.log(d)/MoreMath.log(10.0f));
		if (d < 2.5*MoreMath.pow(10,ordMag)) {
			scale = 2.5f*MoreMath.pow(10,ordMag);
		} else if (d < 5*MoreMath.pow(10,ordMag)) {
			scale = 5*MoreMath.pow(10,ordMag);
		} else {
			scale = 10*MoreMath.pow(10,ordMag);
		}
		//Calculate how many pixels this distance is apart
		scalePx = (int)(35.0f*scale/d);
		
		//Draw the scale bar
		pc.g.setColor(0x00000000);
		pc.g.drawLine(10,10, 10 + scalePx, 10);
		pc.g.drawLine(10,11, 10 + scalePx, 11); //double line width
		pc.g.drawLine(10, 8, 10, 13);
		pc.g.drawLine(10 + scalePx, 8, 10 + scalePx, 13);
		if (scale > 1000) {
			pc.g.drawString(Integer.toString((int)(scale/1000.0f)) + "km", 10 + scalePx/2 ,12, Graphics.HCENTER | Graphics.TOP);
		} else {
			pc.g.drawString(Integer.toString((int)scale) + "m", 10 + scalePx/2 ,12, Graphics.HCENTER | Graphics.TOP);
		}
	}

	public void showMovement(Graphics g) {
		g.setColor(0, 0, 0);
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		int posX, posY;
		if (!gpsRecenter) {
			IntPoint p1 = new IntPoint(0,0);				
			pc.getP().forward((float)(pos.latitude/360.0*2*Math.PI), (float)(pos.longitude/360.0*2*Math.PI),p1);
			posX = p1.getX();
			posY = p1.getY();		
		} else {
			posX = centerX;
			posY = centerY;
		}
		float radc = (float) (course * Math.PI / 180d);
		int px = posX + (int) (Math.sin(radc) * 20);
		int py = posY - (int) (Math.cos(radc) * 20);
		g.drawRect(posX - 2, posY - 2, 4, 4);
		g.drawLine(posX, posY, px, py);
		g.drawLine(centerX-2, centerY - 2, centerX + 2, centerY + 2);
		g.drawLine(centerX-2, centerY + 2, centerX + 2, centerY - 2);
	}

	public int showMemory(Graphics g, int yc, int la) {
		g.setColor(0, 0, 0);
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
		g.drawString("LastMsg: " + lastMsg, 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString( "at " + lastMsgTime.get(Calendar.HOUR_OF_DAY) + ":"  
				+ HelperRoutines.formatInt2(lastMsgTime.get(Calendar.MINUTE)) + ":"  
				+ HelperRoutines.formatInt2(lastMsgTime.get(Calendar.SECOND)), 0, yc,  
				Graphics.TOP | Graphics.LEFT );
		return (yc);

	}

	public int showSpeed(Graphics g, int yc, int la) {
		g.setColor(0, 0, 0);
		g.drawString("speed : " + speed, 0, yc, Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("course  : " + course, 0, yc, Graphics.TOP
						| Graphics.LEFT);
		yc += la;
		g.drawString("height  : " + pos.altitude, 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		return yc;
	}

	public int showDistanceToTarget(Graphics g, int yc, int la) {
		g.setColor(0, 0, 0);
		String text;
		if (target == null) {
			text = "Distance: N/A";
		} else {
			
			float distance = ProjMath.getDistance(target.lat, target.lon, center.radlat, center.radlon); 
			if (distance > 10000) {
				text = "Distance: " + Integer.toString((int)(distance/1000.0f)) + "km";
			} else if (distance > 1000) {
				text = "Distance: " + Float.toString(((int)(distance/100.0f))/10.0f) + "km";
			} else {
				text = "Distance: " + Integer.toString((int)distance) + "m";
			}
			
		}
		g.drawString(text , 0, yc, Graphics.TOP | Graphics.LEFT);
		yc += la;
		return yc;
	}

	private void updatePosition() {
		if (pc != null){
//			projection = ProjFactory.getInstance(center,course, scale, getWidth(), getHeight());
//			pc.setP(projection);
			pc.center = center.clone();
			pc.scale = scale;
			pc.course=course;
			repaint();
			
			if (locationUpdateListeners != null) {
				synchronized (locationUpdateListeners) {
					for (int i = 0; i < locationUpdateListeners.size(); i++) {
						((LocationUpdateListener)locationUpdateListeners.elementAt(i)).loctionUpdated();
					}
				}
			}
			
		}
	}
	
	public synchronized void receivePosItion(float lat, float lon, float scale) {
		logger.debug("Now displaying: " + (lat*MoreMath.FAC_RADTODEC) + "|" + (lon*MoreMath.FAC_RADTODEC));
		center.setLatLon(lat, lon,true);
		this.scale = scale;
		updatePosition();
	}

	public synchronized void receivePosItion(Position pos) {
		//#debug info
		logger.info("New position: " + pos);
		this.pos = pos;
		collected++;
		if (gpsRecenter) {
			center.setLatLon(pos.latitude, pos.longitude);
			// don't rotate too fast
			if (speed > 2) {
				course = (int) ((pos.course * 3 + course) / 4)+360;
				while (course > 360) course-=360;
			}
		}		
		speed = (int) (pos.speed * 3.6f);		
		if (gpx.isRecordingTrk()){
			try {
				gpx.addTrkPt(pos);				
			} catch (Exception e) {
				receiveMessage(e.getMessage());
			} 
		}
		updatePosition();		
	}
	
	public synchronized Position getCurrentPosition() {
		return this.pos;
	}

	public synchronized void receiveMessage(String s) {
//		#debug info
		logger.info("Setting title: " + s);
		currentMsg = s;
		lastMsgTime.setTime( new Date( System.currentTimeMillis() ) );
		repaint();
	}

	public synchronized void alert(String title, String message, int timeout) {
//		#debug info
		logger.info("Showing trace alert: " + title + ": " + message);
		currentAlertTitle = title;
		currentAlertMessage = message;
		currentAlertsOpenCount++;
		repaint();
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
		tm.schedule(timerT, timeout);
	}

	
	
	public void receiveStatelit(Satelit[] sat) {
		this.sat = sat;

	}

	public MIDlet getParent() {
		return parent;
	}

	protected void pointerPressed(int x, int y) {
		// remember position the pointer was pressed
		this.touchX = x;
		this.touchY = y;
		// remember center when the pointer was pressed
		centerPointerPressedN = center.clone();
	}
	
	protected void pointerReleased(int x, int y) {
		pointerDragged(x , y);
	}
	
	protected void pointerDragged (int x, int y) {
		if (imageCollector != null) {
			// difference between where the pointer was pressed and is currently dragged
			int diffX = this.touchX - x;
			int diffY = this.touchY - y;
			
			IntPoint centerPointerPressedP = new IntPoint();
			imageCollector.getCurrentProjection().forward(centerPointerPressedN, centerPointerPressedP);
			imageCollector.getCurrentProjection().inverse(centerPointerPressedP.x + diffX, centerPointerPressedP.y + diffY, center);
			imageCollector.newDataReady();
			gpsRecenter = false;			
		}
	}
	
	public Tile getDict(byte zl) {
		return t[zl];
	}
	
	public void setDict(Tile dict, byte zl) {		
		t[zl] = dict;
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
			if(center.radlat==0.0f && center.radlon==0.0f) {
				// if no saved position use center of map
				dict.getCenter(center);
			}

			if (pc != null) {				
				pc.center = center.clone();
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
			parent.mNoiseMaker.playSound("DISCONNECT");			
		}
		if (gpx != null) {
			/**
			 * Close and Save the gpx recording, to ensure we don't loose data
			 */
			gpx.saveTrk();
		}
		removeCommand(CMDS[DISCONNECT_GPS_CMD]);
		if (locationProducer == null){
//#debug info
			logger.info("leave locationDecoderEnd no producer");
			return;
		}
		locationProducer = null;
		closeBtConnection();
		notify();		
		addCommand(CMDS[CONNECT_GPS_CMD]);
//		addCommand(START_RECORD_CMD);
//#debug info
		logger.info("end locationDecoderEnd");
	}

	public void receiveSolution(String s) {
		solution = s;

	}

	public String getName(int idx) {
		if (idx < 0)
			return null;
		return namesThread.getName(idx);
	}
	
	public Vector fulltextSearch (String snippet) {
		return namesThread.fulltextSearch(snippet);
	}

	// this is called by ImageCollector
	public void requestRedraw() {
		repaint();
	}

	public void newDataReady() {
		if (imageCollector != null)
			imageCollector.newDataReady();
	}

	public void show() {
		//Display.getDisplay(parent).setCurrent(this);
		GpsMid.getInstance().show(this);
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		repaint();
	}

	public void locationDecoderEnd(String msg) {
		receiveMessage(msg);
		locationDecoderEnd();
	}

	public PositionMark getTarget() {
		return target;
	}

	public void setTarget(PositionMark target) {
		RouteInstructions.initialRecalcDone = false;
		RouteInstructions.icCountOffRouteDetected = 0;
		RouteInstructions.routeInstructionsHeight = 0;
		setRoute(null);
		setRouteNodes(null);
		this.target = target;
		pc.target = target;
		if(target!=null) {
			center.setLatLon(target.lat, target.lon,true);
			pc.center = center.clone();
			pc.scale = scale;
			pc.course = course;
		}
		movedAwayFromTarget=false;
		repaint();
	}

	/**
	 * This is the callback routine if RouteCalculation is ready
	 * @param route
	 */
	public void setRoute(Vector route) {
		synchronized(this) {
			this.route = route;
			if (route!=null) {
				ri = new RouteInstructions(this, route, target);
				oldRecalculationTime = System.currentTimeMillis();
				RouteInstructions.resetOffRoute(route, center);
			}
			routeCalc=false;
			routeEngine=null;
		}
		try {
			if ((Configuration.isStopAllWhileRouteing())&&(imageCollector == null)){
				startImageCollector();
				// imageCollector thread starts up suspended,
				// so we need to resume it
				imageCollector.resume();
			} else {
//				resume();
			}
			repaint();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		logger.info("Hide notify has been called, screen will nolonger be updated");
		if (imageCollector != null) {
			imageCollector.suspend();
		}
	}
	
	protected void showNotify() {
		logger.info("Show notify has been called, screen will be updated again");
		if (imageCollector != null) {
			imageCollector.resume();
			imageCollector.newDataReady();
		}
	}
	
	public Vector getRoute() {
		return route;
	}
}
