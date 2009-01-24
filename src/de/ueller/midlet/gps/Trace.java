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
//#if ENABLE_EDIT
import de.ueller.gpsMid.GUIosmWayDisplay;
import de.ueller.midlet.gps.data.EditableWay;
//#endif
import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueDictReader;
import de.ueller.gpsMid.mapData.Tile;
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
	
	private boolean gpsRecenter = true;
	
	private Position pos = new Position(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1,
			new Date());

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
	
	private boolean rootCalc=false;
	Tile t[] = new Tile[6];
	public Way actualWay;
	PositionMark source;

	// this is only for visual debugging of the routing engine
	Vector routeNodes=new Vector();
	
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
	public volatile boolean speeding=false;

	/**
	 * Current course from GPS in compass degrees, 0..359.  
	 */
	private int course=0;


	private Names namesThread;

	private ImageCollector imageCollector;

	private QueueDataReader tileReader;

	private QueueDictReader dictReader;

	private Runtime runtime = Runtime.getRuntime();

	private PositionMark target;
	private Vector route=null;

	private boolean running=false;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;

	public Gpx gpx;
	private AudioRecorder audioRec;
	
	private static Trace traceInstance=null;

	private Routing	routeEngine;

	private byte arrow;
	private Image scaledPict = null;

	private int iPassedRouteArrow=0;
	private static Font routeFont;
	private static int routeFontHeight;

	/*
	private static Font smallBoldFont;
	private static int smallBoldFontHeight;
	*/
	
	private static final String[] directions  = { "mark",
		"hard right", "right", "half right",
		"straight on",
		"half left", "left", "hard left", "Target reached"};
	private static final String[] soundDirections  = { "",
		"HARD;RIGHT", "RIGHT", "HALF;RIGHT",
		"STRAIGHTON",
		"HALF;LEFT", "LEFT", "HARD;LEFT"};

	private boolean manualRotationMode=false;
	private boolean movedAwayFromTarget=true;
	private long oldRecalculationTime;
	private boolean atTarget=false;
	private int sumWrongDirection=0;
	private int oldAwayFromNextArrow=0;
	private int oldRouteInstructionColor=0x00E6E6E6;
	private static boolean prepareInstructionSaid=false;
	private static boolean checkDirectionSaid=false;
	private static boolean routeRecalculationRequired=false;
	// private int routerecalculations=0;
	
	public Vector locationUpdateListeners;
	
	public Trace(GpsMid parent) throws Exception {
		//#debug
		logger.info("init Trace");
		
		this.parent = parent;
		
		CMDS[EXIT_CMD] = new Command("Exit", Command.EXIT, 2);
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
		//#if ENABLE_EDIT
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
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT)) {
				parent.mNoiseMaker.playSound("CONNECT");
			}
			//#debug debug
			logger.debug("rm connect, add disconnect");
			removeCommand(CMDS[CONNECT_GPS_CMD]);
			addCommand(CMDS[DISCONNECT_GPS_CMD]);
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
					panY = -2;
				} else if (c == CMDS[PAN_DOWN2_CMD]) {
					panY = 2;
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
			if (! rootCalc){
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
				if (Configuration.isStopAllWhileRouteing()){
  				   stopImageCollector();
				}
				logger.info("Routing source: " + source);
				// route recalculation is required until route calculation successful
				routeRecalculationRequired=true;
				routeNodes=new Vector();
				routeEngine = new Routing(t,this);
				routeEngine.solve(source, pc.target);
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
			} else if (c == CMDS[TACHO_CMD]) {
				GuiTacho tacho = new GuiTacho(this);
				tacho.show();
			}
			//#if ENABLE_EDIT 
				else if (c == CMDS[RETRIEVE_XML]) {
					if (C.enableEdits) {
						if ((pc.actualWay != null) && (pc.actualWay instanceof EditableWay)) {
							EditableWay eway = (EditableWay)pc.actualWay;
							GUIosmWayDisplay guiWay = new GUIosmWayDisplay(eway,this);
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
				imageCollector.paint(pc);
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

			if (speeding && Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_VISUAL)) {
			    parent.mNoiseMaker.playSound("SPEED_LIMIT", (byte) 10, (byte) 10);
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
		for (int i=0; i<4; i++){
			t[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD);
//			t[i].walk(nld,nru, Tile.OPT_WAIT_FOR_LOAD);
		}
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
				// wait with possible route recalculation until we've got a new source
				// as current source might still contain an old position
				source = null;
			}
			if (route != null)
				showRoute(pc);
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
	
	/**
	 * @param pc
	 */
	private void showRoute(PaintContext pc) {
		/*	PASSINGDISTANCE is the distance when a routing arrow
			is considered to match to the current position.
			We currently can't adjust this value according to the speed
			because if we would be slowing down during approaching the arrow,
			then PASSINGDISTANCE could become smaller than the distance
			to the arrow due and thus the routines would already use the
			next arrow for routing assistance
		*/
		final int PASSINGDISTANCE=25;

		StringBuffer soundToPlay = new StringBuffer();
		String routeInstruction = null;
    	// backgound colour for standard routing instructions
		int routeInstructionColor=0x00E6E6E6;
		int diffArrowDist=0;
		byte soundRepeatDelay=3;
		byte soundMaxTimesToPlay=2;
		float nearestLat=0.0f;
		float nearestLon=0.0f;
		

		// this makes the distance when prepare-sound is played depending on the speed
		int PREPAREDISTANCE=100;
		if (speed>100) {
			PREPAREDISTANCE=500;							
		} else if (speed>80) {
			PREPAREDISTANCE=300;							
		} else if (speed>55) {
			PREPAREDISTANCE=200;							
		} else if (speed>45) {
			PREPAREDISTANCE=150;							
		} else if (speed>35) {
			PREPAREDISTANCE=125;													
		}
		
		ConnectionWithNode c;
		// Show helper nodes for Routing
		for (int x=0; x<routeNodes.size();x++){
			RouteHelper n=(RouteHelper) routeNodes.elementAt(x);
			pc.getP().forward(n.node.radlat, n.node.radlon, pc.lineP2);
			pc.g.drawRect(pc.lineP2.x-5, pc.lineP2.y-5, 10, 10);
			pc.g.drawString(n.name, pc.lineP2.x+7, pc.lineP2.y+5, Graphics.BOTTOM | Graphics.LEFT);
		}
		synchronized(this) {
			if (route != null && route.size() > 0){
				// there's a route so no calculation required
				routeRecalculationRequired = false;
				RouteNode lastTo;
	
				// find nearest routing arrow (to center of screen)
				int iNearest=0;
				if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTING_HELP)) {
					c = (ConnectionWithNode) route.elementAt(0);
					lastTo=c.to;
					float minimumDistance=99999;
					float distance=99999;
					for (int i=1; i<route.size();i++){
						c = (ConnectionWithNode) route.elementAt(i);
						if (c!=null && c.to!=null && lastTo!=null) {
							// skip connections that are closer than 25 m to the previous one
							if( i<route.size()-1 && ProjMath.getDistance(c.to.lat, c.to.lon, lastTo.lat, lastTo.lon) < 25 ) {
								continue;
							}
							distance = ProjMath.getDistance(center.radlat, center.radlon, lastTo.lat, lastTo.lon); 
							if (distance<minimumDistance) {
								minimumDistance=distance;
								iNearest=i;
							}
							lastTo=c.to;
						}
					}
					//System.out.println("iNearest "+ iNearest + "dist: " + minimumDistance);				    	
					// if nearest route arrow is closer than PASSINGDISTANCE meters we're currently passing this route arrow
					if (minimumDistance<PASSINGDISTANCE) {
						if (iPassedRouteArrow != iNearest) {
							iPassedRouteArrow = iNearest;
							// if there's i.e. a 2nd left arrow in row "left" must be repeated
							if (!atTarget) {
								parent.mNoiseMaker.resetSoundRepeatTimes();
							}
						}
						// after passing an arrow all instructions, i.e. saying "in xxx metres" are allowed again 
						resetVoiceInstructions();
						//System.out.println("iPassedRouteArrow "+ iPassedRouteArrow);
					} else {
						c = (ConnectionWithNode) route.elementAt(iPassedRouteArrow);
						// if we got away more than PASSINGDISTANCE m of the previously passed routing arrow
						if (ProjMath.getDistance(center.radlat, center.radlon, c.to.lat, c.to.lon) >= PASSINGDISTANCE) {
							// assume we should start to emphasize the next routing arrow now
							iNearest=iPassedRouteArrow+1;
						}
					}
				}
				c = (ConnectionWithNode) route.elementAt(0);
				int lastEndBearing=c.endBearing;			
				lastTo=c.to;
				byte a=0;
				byte aNearest=0;
				for (int i=1; i<route.size();i++){
					c = (ConnectionWithNode) route.elementAt(i);
					if (c == null){
						logger.error("show Route got null connection");
						break;
					}
					if (c.to == null){
						logger.error("show Route got connection with NULL as target");
						break;
					}
					if (lastTo == null){
						logger.error("show Route strange lastTo is null");
						break;
					}
					if (pc == null){
						logger.error("show Route strange pc is null");
						break;
					}
	//				if (pc.screenLD == null){
	//					System.out.println("show Route strange pc.screenLD is null");
	//				}
	
					// skip connections that are closer than 25 m to the previous one
					if( i<route.size()-1 && ProjMath.getDistance(c.to.lat, c.to.lon, lastTo.lat, lastTo.lon) < 25 ) {
						// draw small circle for left out connection
						pc.g.setColor(0x00FDDF9F);
						pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
						final byte radius=6;
						pc.g.fillArc(pc.lineP2.x-radius/2,pc.lineP2.y-radius/2,radius,radius,0,359);
						//System.out.println("Skipped routing arrow " + i);
						// if this would have been our iNearest, use next one as iNearest
						if(i==iNearest) iNearest++;
						continue;
					}
					
					//****************************skip straight on-s start********************************

//					int turnb=(c.startBearing-lastEndBearing) * 2;
//					if (turnb > 180) turnb -= 360;
//					if (turnb < -180) turnb += 360;
//					if (i<route.size()-1 && turnb >= -20 && turnb <= 20){
//					if(i==iNearest) iNearest++;
//					}

					//***************************skip straightnsend************************
					
					// no off-screen check for current and next route arrow,
					// so we can do DIRECTION-THEN-DIRECTION instructions
					// even if both arrows are off-screen
					if(i!=iNearest && i!=iNearest+1) {
						if (lastTo.lat < pc.getP().getMinLat()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
						if (lastTo.lon < pc.getP().getMinLon()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
						if (lastTo.lat > pc.getP().getMaxLat()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
						if (lastTo.lon > pc.getP().getMaxLon()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
					}
	
					Image pict = pc.images.IMG_MARK; a=0;
					// make bearing relative to current course for the first route arrow
					if (i==1) {
						lastEndBearing = (course%360) / 2;
					}
					int turn=(c.startBearing-lastEndBearing) * 2;
					if (turn > 180) turn -= 360;
					if (turn < -180) turn += 360;
	//				System.out.println("from: " + lastEndBearing*2 + " to:" +c.startBearing*2+ " turn " + turn);
					if (turn > 110) {
						pict=pc.images.IMG_HARDRIGHT; a=1;
					} else if (turn > 70){
						pict=pc.images.IMG_RIGHT; a=2;
					} else if (turn > 20){
						pict=pc.images.IMG_HALFRIGHT; a=3;
					} else if (turn >= -20){
						pict=pc.images.IMG_STRAIGHTON; a=4;
					} else if (turn >= -70){
						pict=pc.images.IMG_HALFLEFT; a=5;
					} else if (turn >= -110){
						pict=pc.images.IMG_LEFT;  a=6;
					} else {
						pict=pc.images.IMG_HARDLEFT; a=7;
					} 
					if (atTarget) {
						a=8;
					}
					pc.getP().forward(lastTo.lat, lastTo.lon, pc.lineP2);
				    // optionally scale nearest arrow
				    if (i==iNearest) {
				    	nearestLat=lastTo.lat;
				    	nearestLon=lastTo.lon;
						aNearest=a;
						double distance=ProjMath.getDistance(center.radlat, center.radlon, lastTo.lat, lastTo.lon);
						int intDistance=new Double(distance).intValue();
	
						routeInstruction=directions[a] + ((intDistance<PASSINGDISTANCE)?"":" in " + intDistance + "m");
	
				    	if(intDistance<PASSINGDISTANCE) {
							if (!atTarget) { 
								soundToPlay.append (soundDirections[a]);
							}
							soundMaxTimesToPlay=1;
							sumWrongDirection = -1;
							diffArrowDist = 0;
							oldRouteInstructionColor=0x00E6E6E6;
						} else if (sumWrongDirection == -1) {
							oldAwayFromNextArrow = intDistance;
							sumWrongDirection=0;
							diffArrowDist = 0;
						} else {
							diffArrowDist = (intDistance - oldAwayFromNextArrow);
						}
						if ( diffArrowDist == 0 ) {
			    			routeInstructionColor=oldRouteInstructionColor;
						} else if (intDistance < PASSINGDISTANCE) {
					    	// background colour if currently passing
				    		routeInstructionColor=0x00E6E6E6;
			    		} else if ( diffArrowDist > 0) {
					    	// background colour if distance to next arrow has just increased
				    		routeInstructionColor=0x00FFCD9B;
						} else {
					    	// background colour if distance to next arrow has just decreased
				    		routeInstructionColor=0x00B7FBBA;
				    		// we are going towards the next arrow again, so we need to warn again
				    		// if we go away
				    		checkDirectionSaid = false;
						}
						sumWrongDirection += diffArrowDist;
						//System.out.println("Sum wrong direction: " + sumWrongDirection);
						oldAwayFromNextArrow = intDistance;
						if (intDistance>=PASSINGDISTANCE && !checkDirectionSaid) {
							if (intDistance <= PREPAREDISTANCE) {
								soundToPlay.append( (a==4 ? "CONTINUE" : "PREPARE") + ";" + soundDirections[a]);
								soundMaxTimesToPlay=1;
								// Because of adaptive-to-speed distances for "prepare"-instructions
								// GpsMid could fall back from "prepare"-instructions to "in xxx metres" voice instructions
								// Remembering and checking if the prepare instruction already was given since the latest passing of an arrow avoids this
								prepareInstructionSaid = true;
							} else if (intDistance < 900  && !prepareInstructionSaid) {
								soundRepeatDelay=60;
								soundToPlay.append("IN;" + Integer.toString(intDistance / 100)+ "00;METERS;" + soundDirections[a]);								
							}							
						}
						if (a!=arrow) {
							arrow=a;
							scaledPict=doubleImage(pict);
						}
						pict=scaledPict;
				    }
					if (i == iNearest + 1 && !checkDirectionSaid) {
						double distance=ProjMath.getDistance(nearestLat, nearestLon, lastTo.lat, lastTo.lon);
						// if there is a close direction arrow after the current one
						// inform the user about its direction
						if (distance <= PREPAREDISTANCE &&
							// only if not both arrows are STRAIGHT_ON
							!(a==4 && aNearest == 4) &&
							// and only as continuation of instruction
							soundToPlay.length()!=0
						   ) {
							soundToPlay.append(";THEN;");
							if (distance > PASSINGDISTANCE) {
								soundToPlay.append("SOON;");
							}
							soundToPlay.append(soundDirections[a]);
							// same arrow as currently nearest arrow?
							if (a==aNearest) {
								soundToPlay.append(";AGAIN");							
							}
							
							//System.out.println(soundToPlay.toString());
						}
					}
					// if the sum of movement away from the next arrow
					// is much too high then recalculate route
					if ( sumWrongDirection >= PREPAREDISTANCE * 2 / 3
							|| sumWrongDirection >= 300) {
							routeRecalculationRequired = true;
					// if the sum of movement away from the next arrow is high
			    	} else if ( sumWrongDirection >= PREPAREDISTANCE / 3
						|| sumWrongDirection >= 150) {
			    		// if distance to next arrow is high
		    			// and moving away from next arrow
			    		// ask user to check direction
			    		if (diffArrowDist > 0) {
				    		soundToPlay.setLength(0);
				    		soundToPlay.append ("CHECK_DIRECTION");
				    		soundRepeatDelay=30;
				    		checkDirectionSaid = true;
				    		routeInstructionColor=0x00E6A03C;
			    		} else if (diffArrowDist == 0) {
			    			routeInstructionColor = oldRouteInstructionColor;
			    		}
			    	}
					pc.g.drawImage(pict,pc.lineP2.x,pc.lineP2.y,CENTERPOS);
					/*
					Font originalFont = pc.g.getFont();
					if (smallBoldFont==null) {
						smallBoldFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
						smallBoldFontHeight=smallBoldFont.getHeight();
					}
					pc.g.setFont(smallBoldFont);
					pc.g.setColor(0,0,0);
					int turnOrg=(c.startBearing - lastEndBearing)*2;
					pc.g.drawString("S: " + c.startBearing*2 + " E: " + lastEndBearing*2 + " C: " + course + " T: " + turn + "(" + turnOrg + ")",
							pc.lineP2.x,
							pc.lineP2.y-smallBoldFontHeight / 2,
							Graphics.HCENTER | Graphics.TOP
					);
					pc.g.setFont(originalFont);
					*/
					
					lastEndBearing=c.endBearing;
					lastTo=c.to;
				}
			}
			/* if we just moved away from target,
			 * and the map is gpscentered
			 * and there's only one or no route arrow
			 * ==> auto recalculation
			 */
			if (movedAwayFromTarget
				&& gpsRecenter
				&& (route != null && route.size()==2)
				&& ProjMath.getDistance(target.lat, target.lon, center.radlat, center.radlon) > PREPAREDISTANCE
			) {
				routeRecalculationRequired=true;
			}
			if ( routeRecalculationRequired && !atTarget ) {
				long recalculationTime=System.currentTimeMillis();
				if ( source != null
					 && gpsRecenter
					 && Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AUTO_RECALC)
					// do not recalculate route more often than every 7 seconds
					 && Math.abs(recalculationTime-oldRecalculationTime) >= 7000
				) {
					// if map is gps-centered recalculate route
					soundToPlay.setLength(0);
					if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
						parent.mNoiseMaker.playSound("ROUTE_RECALCULATION", (byte) 5, (byte) 1 );
					}
					commandAction(CMDS[ROUTE_TO_CMD],(Displayable) null);
					// set source to null to not recalculate
					// route again before map was drawn
					source=null;
					oldRecalculationTime = recalculationTime;
					// routerecalculations++;
				}
				if (diffArrowDist > 0) {
					// use red background color if moving away
					routeInstructionColor=0x00FF5402;
				} else if (diffArrowDist == 0) {
					routeInstructionColor = oldRouteInstructionColor;
				}
			}
		}
		// Route instruction text output
		if ((routeInstruction != null) && (imageCollector != null)) {
			Font originalFont = pc.g.getFont();
			if (routeFont==null) {
				routeFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
				routeFontHeight=routeFont.getHeight();
			}
			pc.g.setFont(routeFont);
			pc.g.setColor(routeInstructionColor);
			oldRouteInstructionColor=routeInstructionColor;
			pc.g.fillRect(0,pc.ySize-imageCollector.statusFontHeight-routeFontHeight, pc.xSize, routeFontHeight);
			pc.g.setColor(0,0,0);
//			pc.g.drawString(""+routerecalculations,
//					0,
//					pc.ySize-imageCollector.statusFontHeight,
//					Graphics.LEFT | Graphics.BOTTOM
//			);
			pc.g.drawString(routeInstruction,
					pc.xSize/2,
					pc.ySize-imageCollector.statusFontHeight,
					Graphics.HCENTER | Graphics.BOTTOM
			);
			pc.g.setFont(originalFont);
		}
		// Route instruction sound output
		if (soundToPlay.length()!=0 && Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
			parent.mNoiseMaker.playSound(soundToPlay.toString(), (byte) soundRepeatDelay, (byte) soundMaxTimesToPlay);
		}
	}
	
	private static void resetVoiceInstructions() {
		prepareInstructionSaid = false;
		checkDirectionSaid = false;
	}
	
	public static Image doubleImage(Image original)
    {        
        int w=original.getWidth();
        int h=original.getHeight();
		int[] rawInput = new int[w * h];
        original.getRGB(rawInput, 0, w, 0, 0, h, h);
        
        int[] rawOutput = new int[w*h*4];        

        int outOffset= 1;
        int inOffset=  0;
        int lineInOffset=0;
        int val=0;
        
        for (int y=0;y<h*2;y++) {            
        	if((y&1)==1) {
        		outOffset++;
        		inOffset=lineInOffset;
        	} else {
        		outOffset--;
        		lineInOffset=inOffset;
        	}
        	for (int x=0; x<w; x++) {
        		/* unfortunately many devices can draw semitransparent
        		   pictures but only support transparent and opaque
        		   in their graphics routines. So we just keep full transparency
        		   as otherwise the device will convert semitransparent from the png
        		   to full transparent pixels making the new picture much too bright
        		*/
        		val=rawInput[inOffset];
            	if( (val & 0xFF000000) != 0 ) {
            		val|=0xFF000000;
            	}
            	rawOutput[outOffset]=val; 
                /// as a workaround for semitransparency we only draw every 2nd pixel
            	outOffset+=2;
                inOffset++;
            }            
        }               
        return Image.createRGBImage(rawOutput, w*2, h*2, true);        
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
				+ dictReader.getRequestQueueSize(), 0, yc, Graphics.TOP
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
		routeRecalculationRequired = false;
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
			rootCalc=false;
			routeEngine=null;
			iPassedRouteArrow=0;
			sumWrongDirection=-1;
			oldRouteInstructionColor = 0x00E6E6E6;
			resetVoiceInstructions();			
			parent.mNoiseMaker.resetSoundRepeatTimes();
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

}
