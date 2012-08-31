/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * See file COPYING.
 */

package de.ueller.gpsmid.ui;

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
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
//#if polish.android
import de.enough.polish.android.lcdui.AndroidDisplay;
import android.content.Context;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.os.Looper;
import android.os.PowerManager;
import android.util.FloatMath;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;
//#else
import javax.microedition.lcdui.game.GameCanvas;
//#endif
import javax.microedition.midlet.MIDlet;

import de.enough.polish.util.Locale;


import de.ueller.gps.Node;
import de.ueller.gps.Satellite;

import de.ueller.gps.location.CellIdProvider;
import de.ueller.gps.location.Compass;
import de.ueller.gps.location.CompassProducer;
import de.ueller.gps.location.CompassReceiver;
import de.ueller.gps.location.GsmCell;
import de.ueller.gps.location.GetCompass;
import de.ueller.gps.location.LocationMsgProducer;
import de.ueller.gps.location.LocationMsgReceiver;
import de.ueller.gps.location.LocationMsgReceiverList;
import de.ueller.gps.location.LocationUpdateListener;
import de.ueller.gps.location.NmeaInput;
import de.ueller.gps.location.SECellId;
import de.ueller.gps.location.SirfInput;
//#if polish.api.osm-editing
import de.ueller.gpsmid.data.EditableWay;
//#endif
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Gpx;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.data.Position;
import de.ueller.gpsmid.data.PositionMark;
import de.ueller.gpsmid.data.RoutePositionMark;
import de.ueller.gpsmid.data.SECellLocLogger;
import de.ueller.gpsmid.data.ScreenContext;
import de.ueller.gpsmid.data.TrackPlayer;
import de.ueller.gpsmid.graphics.ImageCollector;
import de.ueller.gpsmid.graphics.Images;
import de.ueller.gpsmid.graphics.Proj2D;
import de.ueller.gpsmid.graphics.Proj3D;
import de.ueller.gpsmid.graphics.ProjFactory;
import de.ueller.gpsmid.graphics.Projection;
import de.ueller.gpsmid.mapdata.DictReader;
import de.ueller.gpsmid.mapdata.QueueDataReader;
import de.ueller.gpsmid.mapdata.QueueDictReader;
import de.ueller.gpsmid.mapdata.Way;
import de.ueller.gpsmid.mapdata.WaySegment;
import de.ueller.gpsmid.names.Names;
import de.ueller.gpsmid.names.Urls;
import de.ueller.gpsmid.routing.RouteConnectionTraces;
import de.ueller.gpsmid.routing.RouteHelpers;
import de.ueller.gpsmid.routing.RouteInstructions;
import de.ueller.gpsmid.routing.RouteLineProducer;
import de.ueller.gpsmid.routing.RouteNode;
import de.ueller.gpsmid.routing.RouteSyntax;
import de.ueller.gpsmid.routing.Routing;
import de.ueller.gpsmid.tile.Tile;
import de.ueller.gpsmid.tile.SingleTile;
import de.ueller.gpsmid.ui.DisplayWindow;
import de.ueller.midlet.iconmenu.IconActionPerformer;
import de.ueller.midlet.iconmenu.LayoutElement;
import de.ueller.midlet.ui.CompletionListener;
import de.ueller.midlet.util.ImageCache;
import de.ueller.midlet.util.ImageTools;
import de.ueller.util.CancelMonitorInterface;
import de.ueller.util.DateTimeTools;
import de.ueller.util.HelperRoutines;
import de.ueller.util.IntPoint;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import de.ueller.util.ProjMath;

//#if polish.android
import android.view.Display;
import de.enough.polish.android.lcdui.CanvasBridge;
import de.enough.polish.android.midlet.MidletBridge;
//#endif

/**
 * Implements the main "Map" screen which displays the map, offers track recording etc.
 * @author Harald Mueller
 * 
 */
public class Trace extends KeyCommandCanvas implements LocationMsgReceiver,
//#if polish.android
View.OnTouchListener,
//#endif
CompassReceiver, Runnable , GpsMidDisplayable, CompletionListener, IconActionPerformer {
	/** Soft button for exiting the map screen */
	protected static final int EXIT_CMD = 1;
	protected static final int CONNECT_GPS_CMD = 2;
	protected static final int DISCONNECT_GPS_CMD = 3;
	protected static final int START_RECORD_CMD = 4;
	protected static final int STOP_RECORD_CMD = 5;
	protected static final int MANAGE_TRACKS_CMD = 6;
	protected static final int SAVE_WAYP_CMD = 7;
	protected static final int ENTER_WAYP_CMD = 8;
	protected static final int MANAGE_WAYP_CMD = 9;
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
	// protected static final int OPEN_URL_CMD = 56;
	protected static final int SHOW_PREVIOUS_POSITION_CMD = 57;
	protected static final int TOGGLE_GPS_CMD = 58;
	protected static final int CELLID_LOCATION_CMD = 59;
	protected static final int MANUAL_LOCATION_CMD = 60;
	protected static final int EDIT_ENTITY = 61;
	protected static final int NORTH_UP_CMD = 62;
	protected static final int HELP_ONLINE_TOUCH_CMD = 63;
	protected static final int HELP_ONLINE_WIKI_CMD = 64;
	protected static final int KEYS_HELP_CMD = 65;
	protected static final int ROUTE_TO_FAVORITE_CMD = 66;
	protected static final int ROTATE_TRAVEL_MODE_CMD = 67;
	protected static final int SAVE_PREDEF_WAYP_CMD = 68;
	protected static final int TOUCH_HELP_CMD = 69;
	protected static final int CMS_CMD = 70;
	protected static final int TOGGLE_UNUSEABLEWAYS_DARKER = 71;
	protected static final int HELP_ONLINE_WIKI_ANDROID_CMD = 72;
	protected static final int ROUTING_RECALC_CMD = 73;
	protected static final int ROUTING_START_WITH_OPTIONAL_MODE_SELECT_CMD = 74;
	protected static final int OPEN_MAP_CREDIT_URL = 75;
	protected static final int SAVE_ROUTE_AS_GPX = 76;

	private final Command [] CMDS = new Command[77];

	public static final int DATASCREEN_NONE = 0;
	public static final int DATASCREEN_TACHO = 1;
	public static final int DATASCREEN_TRIP = 2;
	public static final int DATASCREEN_SATS = 3;
	
	public final static int DISTANCE_GENERIC = 1;
	public final static int DISTANCE_ALTITUDE = 2;
	public final static int DISTANCE_ROAD = 3;
	public final static int DISTANCE_AIR = 4;
	public final static int DISTANCE_UNKNOWN = 5;

	//#if polish.android
	//public static MidletBridge midletBridge;
	private PowerManager.WakeLock wl = null;
	private PowerManager pm = null;
	//#endif

	// FIXME should be set based on something like pixels per inch value
	private final static int DRAGGEDMUCH_THRESHOLD = 24;

//	private SirfInput si;
	private volatile LocationMsgProducer locationProducer;
	private LocationMsgProducer cellIDLocationProducer = null;
	private CompassProducer compassProducer = null;
	private volatile int compassDirection = 0;
	private volatile int compassDeviation = 0;
	private volatile int compassDeviated = 0;

	// min?, max? for whole screen, mapMin?/mapMax? for map area
	public DisplayWindow rootWindow = null;
	public DisplayWindow mapWindow = null;

	public volatile byte solution = LocationMsgReceiver.STATUS_OFF;
	public String solutionStr = Locale.get("solution.Off");
	
	/** Flag if the user requested to be centered to the current GPS position (true)
	 * or if the user moved the map away from this position (false).
	 */
	public boolean gpsRecenter = true;
	/** Flag if the gps position is not yet valid after recenter request
	 */
	public volatile boolean gpsRecenterInvalid = true;
	/** Flag if the gps position is stale (last known position instead of current) after recenter request
	 */
	public volatile boolean gpsRecenterStale = true;
	/** Flag if the map is autoZoomed
	 */
	public boolean autoZoomed = true;
	/** Flag if we're manually panning, zooming or rotating the map
	 */
	public boolean mapBrowsing = false;
	/** Is touchscreen pointer pressed
	 */
	public boolean pointerDown = false;
	
	private volatile boolean currentLayoutIsPortrait = true;
	private volatile int previousAngle = 0;
	private volatile int currentRotation = 0;

	private Position pos = new Position(0.0f, 0.0f,
			PositionMark.INVALID_ELEVATION, 0.0f, 0.0f, 1,
			System.currentTimeMillis());

	/**
	 * this node contains actually RAD coordinates
	 * although the constructor for Node(lat, lon) requires parameters in DEC format
	 * - e. g. "new Node(49.328010f, 11.352556f)"
	 */
	public Node center = new Node(49.328010f, 11.352556f);

	private Node prevPositionNode = null;
	
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
	
	public float scale = Configuration.getRealBaseScale();

	int showAddons = 0;
	
	/** x position display was touched last time (on pointerPressed() ) */
	private static int touchX = 0;
	/** y position display was touched last time (on pointerPressed() ) */
	private static int touchY = 0;
	/** x position display was released last time (on pointerReleased() ) */
	private static int touchReleaseX = 0;
	/** y position display was released last time (on pointerReleased() ) */
	private static int touchReleaseY = 0;
	/** center when display was touched last time (on pointerReleased() ) */
	private static Node	centerPointerPressedN = new Node();
	private static Node	pickPointStart = new Node();
	private static Node	pickPointEnd = new Node();
	private static Node	centerNode = new Node();
	/**
	 * time at which a pointer press occured to determine
	 * single / double / long taps 
	 */
	private long pressedPointerTime;
	/**
	 * indicates if the next release event is valid or the corresponding pointer pressing has already been handled
	 */
	private volatile boolean pointerActionDone;
	/** timer checking for single tap */
	private volatile TimerTask singleTapTimerTask = null;
	/** timer checking for long tap */
	private volatile TimerTask longTapTimerTask = null;
	/** timer for returning to small buttons */
	private volatile TimerTask bigButtonTimerTask = null;
	/**
	 * Indicates that there was any drag event since the last pointerPressed
	 */
	private static volatile boolean pointerDragged = false;
	/**
	 * Indicates that there was a rather far drag event since the last pointerPressed
	 */
	private static volatile boolean pointerDraggedMuch = false;
	/** indicates whether we already are checking for a single tap in the TimerTask */
	private static volatile boolean checkingForSingleTap = false;
	
	private final int DOUBLETAP_MAXDELAY = 300;
	private final int LONGTAP_DELAY = 1000;
	
	/** Flag if a route is currently being calculated */
	public volatile boolean routeCalc = false;
	public Tile tiles[] = new Tile[DictReader.NUM_DICT_ZOOMLEVELS];
	public volatile boolean baseTilesRead = false;
	
	public Way actualSpeedLimitWay;
	public volatile Way actualWay;
	public volatile SingleTile actualSingleTile;

	private long oldRecalculationTime;

	/** List representing the submenu "Recordings" */
	private List recordingsMenu = null;
	/** Array of command numbers corresponding to the items in recordingsMenu */
	private int[] recordingsMenuCmds = null;
	/** List representing the submenu "Routing" */
	private List routingsMenu = null;
	private GuiTacho guiTacho = null;
	private GuiTrip guiTrip = null;
	private GuiSatellites guiSatellites = null;
	private GuiWaypointSave guiWaypointSave = null;
	private final GuiWaypointPredefined guiWaypointPredefined = null;
	private static TraceIconMenu traceIconMenu = null;
	private static GuiDiscover guiDiscover = null;
	private static GuiDiscoverIconMenu guiDiscoverIconMenu = null;
	private static CMSLayout cmsl = null;
	
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
	public volatile float fspeed;

	/**
	 * variables for setting course from GPS movement
	 * TODO: make speed threshold (currently courseMinSpeed)
	 * user-settable by transfer mode in the style file
	 * and/or in user menu
	 */
	// was three until release 0.7; less than three allows
	// for course setting even with slow walking, while
	// the heuristics filter away erratic courses
	// I've even tested with 0.5f with good results --jkpj
	private final float courseMinSpeed = 1.5f;
	private volatile int prevCourse = -1;
	private volatile int secondPrevCourse = -1;
	private volatile int thirdPrevCourse = -1;

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

	//#if polish.api.finland
	private volatile boolean cameraAlert = false;
	private long startTimeOfCameraAlert = 0;
	//#endif
	private volatile boolean nodeAlert = false;
	private volatile int alertNodeType = 0;
	private long startTimeOfAlertSign = 0;

	/**
	 * Current course from GPS in compass degrees, 0..359.
	 */
	private int course = 0;
	private int coursegps = 0;
	// is current course valid for deciding on how to route
	private boolean courseValid = false;

	public boolean atDest = false;
	public volatile boolean movedAwayFromDest = true;

	private Names namesThread;

	private Urls urlsThread;

	private ImageCollector imageCollector;
	
	private QueueDataReader tileReader;

	private QueueDictReader dictReader;

	private final Runtime runtime = Runtime.getRuntime();

	private StringBuffer sbTemp = new StringBuffer();
	private long lLastDragTime = 0;
	
	private RoutePositionMark dest = null;
	
	WaySegment waySegment = new WaySegment();
	
	public Vector route = null;
	private RouteInstructions ri = null;
	
	private boolean running = false;
	private static final int CENTERPOS = Graphics.HCENTER | Graphics.VCENTER;

	public Gpx gpx;
	public AudioRecorder audioRec;
	
	private static volatile Trace traceInstance = null;

	private volatile Routing	routeEngine;

	/*
	private static Font smallBoldFont;
	private static int smallBoldFontHeight;
	*/
	private static Font fontRouteIcon = null;
	
	public boolean manualRotationMode = false;
	
	public Vector locationUpdateListeners;
	private Projection panProjection;

	private boolean showingTraceIconMenu = false;
	private boolean showingSplitSearch = false;
	private boolean showingSplitSetup = false;
	private boolean showingSplitCMS = false;

	private GuiWaypointPredefinedForm mForm;

	private Vector clickableMarkers;

	private IntPoint centerP;

	private GuiSearch guiSearch;

	//#if polish.android
	private static final int INVALID_POINTER_ID = -1;
	private int pointerId = INVALID_POINTER_ID;
	private int mtPointerId = INVALID_POINTER_ID;
	private float pinchZoomDistance = 0f;
	private float pinchZoomScale = 0;
	private float pinchZoomRotation = 0;
	private float pinchZoomOrigAngle = 0;
	private volatile boolean rotationStarted = false;
	private volatile boolean zoomStarted = false;
	private CanvasBridge canvas;
	//#endif

	public class ClickableCoords {
		int x;
		int y;
		int urlIdx;
		int phoneIdx;
		int nodeID;
	}

	private Trace() throws Exception {
		//#debug
		logger.info("init Trace");
		
		rootWindow = new DisplayWindow(0, 0, getWidth(), getHeight(), true, false);
		mapWindow = new DisplayWindow(0, 0, getWidth(), getHeight(), true, false);

		this.parent = GpsMid.getInstance();
		
		Configuration.setHasPointerEvents(hasPointerEvents());	
		
		if (Configuration.getCfgBitState(Configuration.CFGBIT_TMS_BACKGROUND)) {
			scale = Configuration.getRasterScale();
		}

		CMDS[EXIT_CMD] = new Command(Locale.get("generic.Exit")/*Exit*/, Command.EXIT, 2);
		CMDS[REFRESH_CMD] = new Command(Locale.get("trace.Refresh")/*Refresh*/, Command.ITEM, 4);
		CMDS[SEARCH_CMD] = new Command(Locale.get("generic.Search")/*Search*/, Command.OK, 1);
		CMDS[CONNECT_GPS_CMD] = new Command(Locale.get("trace.StartGPS")/*Start GPS*/,Command.ITEM, 2);
		CMDS[DISCONNECT_GPS_CMD] = new Command(Locale.get("trace.StopGPS")/*Stop GPS*/,Command.ITEM, 2);
		CMDS[TOGGLE_GPS_CMD] = new Command(Locale.get("trace.ToggleGPS")/*Toggle GPS*/,Command.ITEM, 2);
		CMDS[START_RECORD_CMD] = new Command(Locale.get("trace.StartRecord")/*Start record*/,Command.ITEM, 4);
		CMDS[STOP_RECORD_CMD] = new Command(Locale.get("trace.StopRecord")/*Stop record*/,Command.ITEM, 4);
		CMDS[MANAGE_TRACKS_CMD] = new Command(Locale.get("trace.ManageTracks")/*Manage tracks*/,Command.ITEM, 5);
		CMDS[SAVE_WAYP_CMD] = new Command(Locale.get("trace.SaveWaypoint")/*Save waypoint*/,Command.ITEM, 7);
		CMDS[ENTER_WAYP_CMD] = new Command(Locale.get("trace.EnterWaypoint")/*Enter waypoint*/,Command.ITEM, 7);
		CMDS[MANAGE_WAYP_CMD] = new Command(Locale.get("trace.ManageWaypoints")/*Manage waypoints*/,Command.ITEM, 7);
		CMDS[ROUTING_TOGGLE_CMD] = new Command(Locale.get("trace.ToggleRouting")/*Toggle routing*/,Command.ITEM, 3);
		CMDS[CAMERA_CMD] = new Command(Locale.get("trace.Camera")/*Camera*/,Command.ITEM, 9);
		CMDS[CLEAR_DEST_CMD] = new Command(Locale.get("trace.ClearDestination")/*Clear destination*/,Command.ITEM, 10);
		CMDS[SET_DEST_CMD] = new Command(Locale.get("trace.AsDestination")/*As destination*/,Command.ITEM, 11);
		CMDS[MAPFEATURES_CMD] = new Command(Locale.get("trace.MapFeatures")/*Map Features*/,Command.ITEM, 12);
		CMDS[RECORDINGS_CMD] = new Command(Locale.get("trace.Recordings")/*Recordings...*/,Command.ITEM, 4);
		CMDS[ROUTINGS_CMD] = new Command(Locale.get("trace.Routing3")/*Routing...*/,Command.ITEM, 3);
		CMDS[OK_CMD] = new Command(Locale.get("generic.OK")/*OK*/,Command.OK, 14);
		CMDS[BACK_CMD] = new Command(Locale.get("generic.Back")/*Back*/,Command.BACK, 15);
		CMDS[ZOOM_IN_CMD] = new Command(Locale.get("trace.ZoomIn")/*Zoom in*/,Command.ITEM, 100);
		CMDS[ZOOM_OUT_CMD] = new Command(Locale.get("trace.ZoomOut")/*Zoom out*/,Command.ITEM, 100);
		CMDS[MANUAL_ROTATION_MODE_CMD] = new Command(Locale.get("trace.ManualRotation2")/*Manual rotation mode*/,Command.ITEM, 100);
		CMDS[TOGGLE_OVERLAY_CMD] = new Command(Locale.get("trace.NextOverlay")/*Next overlay*/,Command.ITEM, 100);
		CMDS[TOGGLE_BACKLIGHT_CMD] = new Command(Locale.get("trace.KeepBacklight")/*Keep backlight on/off*/,Command.ITEM, 100);
		CMDS[TOGGLE_FULLSCREEN_CMD] = new Command(Locale.get("trace.SwitchToFullscreen")/*Switch to fullscreen*/,Command.ITEM, 100);
		CMDS[TOGGLE_MAP_PROJ_CMD] = new Command(Locale.get("trace.NextMapProjection")/*Next map projection*/,Command.ITEM, 100);
		CMDS[TOGGLE_KEY_LOCK_CMD] = new Command(Locale.get("trace.ToggleKeylock")/*(De)Activate Keylock*/,Command.ITEM, 100);
		CMDS[TOGGLE_RECORDING_CMD] = new Command(Locale.get("trace.ToggleRecording")/*(De)Activate recording*/,Command.ITEM, 100);
		CMDS[TOGGLE_RECORDING_SUSP_CMD] = new Command(Locale.get("trace.SuspendRecording")/*Suspend recording*/,Command.ITEM, 100);
		CMDS[RECENTER_GPS_CMD] = new Command(Locale.get("trace.RecenterOnGPS")/*Recenter on GPS*/,Command.ITEM, 100);
		CMDS[SHOW_DEST_CMD] = new Command(Locale.get("trace.ShowDestination")/*Show destination*/,Command.ITEM, 100);
		CMDS[SHOW_PREVIOUS_POSITION_CMD] = new Command(Locale.get("trace.ShowPreviousPosition")/*Previous position*/,Command.ITEM, 100);
		CMDS[DATASCREEN_CMD] = new Command(Locale.get("trace.Tacho")/*Tacho*/, Command.ITEM, 15);
		CMDS[OVERVIEW_MAP_CMD] = new Command(Locale.get("trace.OverviewFilterMap")/*Overview/Filter map*/, Command.ITEM, 20);
		CMDS[RETRIEVE_XML] = new Command(Locale.get("trace.RetrieveXML")/*Retrieve XML*/,Command.ITEM, 200);
		CMDS[EDIT_ENTITY] = new Command(Locale.get("traceiconmenu.EditPOI")/*Edit POI*/,Command.ITEM, 200);
		CMDS[PAN_LEFT25_CMD] = new Command(Locale.get("trace.left25")/*left 25%*/,Command.ITEM, 100);
		CMDS[PAN_RIGHT25_CMD] = new Command(Locale.get("trace.right25")/*right 25%*/,Command.ITEM, 100);
		CMDS[PAN_UP25_CMD] = new Command(Locale.get("trace.up25")/*up 25%*/,Command.ITEM, 100);
		CMDS[PAN_DOWN25_CMD] = new Command(Locale.get("trace.down25")/*down 25%*/,Command.ITEM, 100);
		CMDS[PAN_LEFT2_CMD] = new Command(Locale.get("trace.left2")/*left 2*/,Command.ITEM, 100);
		CMDS[PAN_RIGHT2_CMD] = new Command(Locale.get("trace.right2")/*right 2*/,Command.ITEM, 100);
		CMDS[PAN_UP2_CMD] = new Command(Locale.get("trace.up2")/*up 2*/,Command.ITEM, 100);
		CMDS[PAN_DOWN2_CMD] = new Command(Locale.get("trace.down2")/*down 2*/,Command.ITEM, 100);
		CMDS[TOGGLE_AUDIO_REC] = new Command(Locale.get("trace.AudioRecording")/*Audio recording*/,Command.ITEM, 100);
		CMDS[ROUTING_START_CMD] = new Command(Locale.get("trace.CalculateRoute")/*Calculate route*/,Command.ITEM, 100);
		CMDS[ROUTING_RECALC_CMD] = new Command(Locale.get("trace.ReCalculateRoute")/*Recalculate route*/,Command.ITEM, 100);
		CMDS[ROUTING_STOP_CMD] = new Command(Locale.get("trace.StopRouting")/*Stop routing*/,Command.ITEM, 100);
		CMDS[ONLINE_INFO_CMD] = new Command(Locale.get("trace.OnlineInfo")/*Online info*/,Command.ITEM, 100);
		CMDS[ROUTING_START_WITH_MODE_SELECT_CMD] = new Command(Locale.get("trace.CalculateRoute2")/*Calculate route...*/,Command.ITEM, 100);
		CMDS[ROUTING_START_WITH_OPTIONAL_MODE_SELECT_CMD] = new Command(Locale.get("trace.CalculateRoute3")/*Calculate route...*/,Command.ITEM, 100);
		CMDS[RETRIEVE_NODE] = new Command(Locale.get("trace.AddPOI")/*Add POI to OSM...*/,Command.ITEM, 100);
		CMDS[ICON_MENU] = new Command(Locale.get("trace.Menu")/*Menu*/,Command.OK, 100);
		CMDS[SETUP_CMD] = new Command(Locale.get("trace.Setup")/*Setup*/, Command.ITEM, 25);
		CMDS[ABOUT_CMD] = new Command(Locale.get("generic.About")/*About*/, Command.ITEM, 30);
		//#if polish.api.wmapi
		CMDS[SEND_MESSAGE_CMD] = new Command(Locale.get("trace.SendSMSMapPos")/*Send SMS (map pos)*/,Command.ITEM, 20);
		//#endif
		CMDS[EDIT_ADDR_CMD] = new Command(Locale.get("trace.AddAddrNode")/*Add Addr node*/,Command.ITEM,100);
		CMDS[CELLID_LOCATION_CMD] = new Command(Locale.get("trace.CellidLocation")/*Set location from CellID*/,Command.ITEM,100);
		CMDS[MANUAL_LOCATION_CMD] = new Command(Locale.get("trace.ManualLocation")/*Set location manually*/,Command.ITEM,100);
		CMDS[HELP_ONLINE_TOUCH_CMD] = new Command(Locale.get("guidiscovericonmenu.Touch")/**/,Command.ITEM,100);
		CMDS[HELP_ONLINE_WIKI_CMD] = new Command(Locale.get("guidiscovericonmenu.Wiki")/**/,Command.ITEM,100);
		//#if polish.android
		CMDS[HELP_ONLINE_WIKI_ANDROID_CMD] = new Command(Locale.get("guidiscovericonmenu.AndroidWiki")/**/,Command.ITEM,100);
		//#endif
		CMDS[KEYS_HELP_CMD] = new Command(Locale.get("guidiscover.KeyShortcuts")/**/,Command.ITEM,100);
		CMDS[ROUTE_TO_FAVORITE_CMD] = new Command(Locale.get("guidiscover.KeyShortcuts")/**/,Command.ITEM,100);
		CMDS[ROTATE_TRAVEL_MODE_CMD] = new Command(Locale.get("guiroute.TravelBy")/**/,Command.ITEM,100);
		CMDS[TOUCH_HELP_CMD] = new Command(Locale.get("trace.touchhelp")/*Touchscreen functions*/,Command.ITEM,100);
		CMDS[CMS_CMD] = new Command(Locale.get("trace.Tacho")/*Tacho*/, Command.ITEM, 100);
		CMDS[TOGGLE_UNUSEABLEWAYS_DARKER] = new Command(Locale.get("trace.ToggleUnuseableWaysDarker")/*Toggle unuseable ways darker */, Command.ITEM, 100);
		CMDS[OPEN_MAP_CREDIT_URL] = new Command(Locale.get("trace.showmapcredit")/*Show map credit/license webpage*/, Command.ITEM, 100);
		CMDS[SAVE_ROUTE_AS_GPX] = new Command(Locale.get("traceiconmenu.SaveRouteGpx")/*Save route as GPX*/, Command.ITEM, 100);

		addAllCommands();
		
		if (Legend.isValid) {
			Configuration.loadKeyShortcuts(gameKeyCommand, singleKeyPressCommand,
						       repeatableKeyPressCommand, doubleKeyPressCommand, longKeyPressCommand,
						       nonReleasableKeyPressCommand, CMDS);
		}

		// FIXME getWidth() && getHeight() might be incorrect at startup on
		// some platforms
		Configuration.setCanvasSpecificDefaults(getWidth(), getHeight());
		
		try {
			if (Legend.isValid) {
				startup();
			}
		} catch (Exception e) {
			logger.fatal(Locale.get("trace.GotExceptionDuringStartup")/*Got an exception during startup: */ + e.getMessage());
			e.printStackTrace();
			return;
		}
		// setTitle("initTrace ready");
		
		locationUpdateListeners = new Vector();
		
		traceInstance = this;
	}
	
	//#if polish.android
	@Override
	public boolean onTouch(View view, MotionEvent event) {
		// modeled after J2MEPolish source, multitouch added
		float x = event.getX();
		float y = event.getY();
		int truncatedX = (int)x;
		int truncatedY = (int)y;

		int action = event.getActionMasked();
		CanvasBridge.current().requestFocus();
		switch(action) {
			case MotionEvent.ACTION_DOWN:
				CanvasBridge.current().onTouch(view, event);
				pointerId = event.getPointerId(0);
				mtPointerId = INVALID_POINTER_ID;
				return true;
			case MotionEvent.ACTION_POINTER_DOWN:
				zoomStarted = false;
				rotationStarted = false;
				pointerDragged = false;
				pointerDraggedMuch = false;
				mapBrowsing = true;
				pinchZoomDistance = dist(event);
				pinchZoomRotation = course + angle(event);
				pinchZoomOrigAngle = angle(event);
				pinchZoomScale = scale;
				mtPointerId = event.getPointerId(1);
				if (tl.pointerHasDoubleTapAction(touchX, touchY)) {
					doubleTap(touchX, touchY);
				} else {
					if (tl.pointerHasDoubleTapAction(truncatedX, truncatedY)) {
						doubleTap(truncatedX, truncatedY);
					}
				}
				pointerActionDone = true;
				return true;
			case MotionEvent.ACTION_POINTER_UP:
				// FIXME don't just assume it's the second, but check it
				mtPointerId = INVALID_POINTER_ID;
				mapBrowsing = false;
				repaint();
				return true;
			case MotionEvent.ACTION_UP:
				CanvasBridge.current().onTouch(view, event);
				pointerId = INVALID_POINTER_ID;
				mtPointerId = INVALID_POINTER_ID;
				mapBrowsing = false;
				repaint();
				return true;
			case MotionEvent.ACTION_MOVE:
				final int pointerIndex = event.findPointerIndex(mtPointerId);
				int mCount = event.getPointerCount();
				// pinch zoom when at map screen but not in other screens
				if (imageCollector != null && imageCollector.isRunning() && mCount > 1 && mtPointerId != INVALID_POINTER_ID) {
					// possible FIXME should we skip this if we're getting compass readings?
					if (angleDiff((int) pinchZoomOrigAngle, (int) angle(event)) > 20) {
						rotationStarted = true;
						// restore zoom at start of rotation gesture
						// to avoid bug is 3450292 on some devices
						mtPointerDragged(pinchZoomScale);
					}
					if (!rotationStarted && ((pinchZoomDistance / dist(event)) > 1.08f || (pinchZoomDistance / dist(event)) < 0.92f)) {
						zoomStarted = true;
					}
					// stop zoom when rotation starts, to avoid bug id 3560292 
					if (zoomStarted && !rotationStarted) {
						mtPointerDragged(pinchZoomDistance / dist(event) * pinchZoomScale);
					}
					if (rotationStarted) {
						mtPointerRotated((360*3 + pinchZoomRotation - angle(event)) % 360);
					}
				}
				CanvasBridge.current().onTouch(view, event);
				return true;
			default: return view.onTouchEvent(event);
		}
	}
	private float dist(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return FloatMath.sqrt(x * x + y * y);
	}
	private float angle(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		if (event.getPointerId(0) != pointerId) {
			x = 0 - x;
			y = 0 - y;
		}
		if (x == 0.0) {
			x = (float) 0.000001;
		}
		float r = MoreMath.atan(y/x) * MoreMath.FAC_RADTODEC;
		if (x < 0) {
			r = r + 180;
		}
		return r;
	}
	//#endif


	public Command getCommand(int command) {
		return CMDS[command];
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
				logger.exception(Locale.get("trace.FailedToInitialiseMapScreen")/*Failed to initialise Map screen*/, e);
			}
		}
		return traceInstance;
	}

	public float getGpsLat() {
		return pos.latitude;
	}

	public float getGpsLon() {
		return pos.longitude;
	}


	public void stopCompass() {
		if (compassProducer != null) {
			compassProducer.close();
		}
		compassProducer = null;
	}
	public void startCompass() {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION) && compassProducer == null) {
			compassProducer = new GetCompass();
			if (!compassProducer.init(this)) {
				logger.info("Failed to init compass producer");
				compassProducer = null;
			} else if (!compassProducer.activate(this)) {
				logger.info("Failed to activate compass producer");
				compassProducer = null;
			}
		}
	}

	/**
	 * Starts the LocationProvider in the background
	 */
	public void run() {
		try {
			if (running) {
				receiveMessage(Locale.get("trace.GpsStarterRunning")/*GPS starter already running*/);
				return;
			}

			//#debug info
			logger.info("start thread init locationprovider");
			if (locationProducer != null) {
				receiveMessage(Locale.get("trace.LocProvRunning")/*Location provider already running*/);
				return;
			}
			if (Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_NONE) {
				receiveMessage(Locale.get("trace.NoLocProv")/*"No location provider*/);
				return;
			}
			running=true;
			//#if polish.android
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_GPS_CONNECTED, true);
			currentRotation = getAndroidRotationAngle();
			//#endif
			currentLayoutIsPortrait = deviceLayoutIsPortrait();
			//#if polish.android
			previousAngle = getAndroidRotationAngle();
			//#else
			previousAngle = deviceLayoutIsPortrait() ? 0 : 90;
			//#endif
			startCompass();
			int locprov = Configuration.getLocationProvider();
			receiveMessage(Locale.get("trace.ConnectTo")/*Connect to */ + Configuration.LOCATIONPROVIDER[locprov]);
			if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_STARTUP)) {
				// Don't do initial lookup if we're going to start primary cellid location provider anyway
				if (Configuration.getLocationProvider() != Configuration.LOCATIONPROVIDER_SECELL || !Configuration.getCfgBitState(Configuration.CFGBIT_AUTO_START_GPS)) {
					commandAction(CELLID_LOCATION_CMD);
				}
			}
			switch (locprov) {
				case Configuration.LOCATIONPROVIDER_SIRF:
					locationProducer = new SirfInput();
					break;
				case Configuration.LOCATIONPROVIDER_NMEA:
					locationProducer = new NmeaInput();
					break;
				case Configuration.LOCATIONPROVIDER_SECELL:
					if (cellIDLocationProducer != null) {
						cellIDLocationProducer.close();
					}
					locationProducer = new SECellId();
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
						//#if polish.android
						// FIXME current (2010-06, 2011-07 (2.2.1)) android j2mepolish doesn't give this info
						//#else
						if (jsr179Version != null && jsr179Version.length() > 0) {
						//#endif
							Class jsr179Class = Class.forName("de.ueller.gps.location.Jsr179Input");
							locationProducer = (LocationMsgProducer) jsr179Class.newInstance();
						//#if polish.android
						//#else
						}
						//#endif
					} catch (ClassNotFoundException cnfe) {
						locationDecoderEnd();
						logger.exception(Locale.get("trace.NoJSR179Support")/*Your phone does not support JSR179, please use a different location provider*/, cnfe);
						running = false;
						return;
					}
					//#else
					// keep Eclipse happy
					if (true) {
						logger.error(Locale.get("trace.JSR179NotCompiledIn")/*JSR179 is not compiled in this version of GpsMid*/);
						running = false;
						return;
					}
					//#endif
					break;
				case Configuration.LOCATIONPROVIDER_ANDROID:
					//#if polish.android
					try {
						Class AndroidLocationInputClass = Class.forName("de.ueller.gps.location.AndroidLocationInput");
						locationProducer = (LocationMsgProducer) AndroidLocationInputClass.newInstance();
					} catch (ClassNotFoundException cnfe) {
						locationDecoderEnd();
						logger.exception(Locale.get("trace.NoAndroidSupport")/*Your phone does not support Android location API, please use a different location provider*/, cnfe);
						running = false;
						return;
					}
					//#else
					// keep Eclipse happy
					if (true) {
						logger.error(Locale.get("trace.AndroidNotCompiledIn")/*Location API for Android is not compiled in this version of GpsMid*/);
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
						//#if polish.android
						de.enough.polish.android.io.Connection logCon = Connector.open(url);
						//#else
						javax.microedition.io.Connection logCon = Connector.open(url);
						//#endif
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
					logger.exception(Locale.get("trace.CouldntOpenFileForRawLogging")/*Could not open file for raw logging of Gps data*/,ioe);
				} catch (SecurityException se) {
					logger.error(Locale.get("trace.PermissionWritingDataDenied")/*Permission to write data for NMEA raw logging was denied*/);
				}
			}
			//#endif
			if (locationProducer == null) {
				logger.error(Locale.get("trace.ChooseDiffLocMethod")/*Your phone does not seem to support this method of location input, please choose a different one*/);
				running  = false;
				return;
			}
			if (!locationProducer.init(this)) {
				logger.info("Failed to initialise location producer");
				running = false;
				return;
			}
			if (!locationProducer.activate(this)) {
				logger.info("Failed to activate location producer");
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
			logger.fatal(Locale.get("trace.TraceThreadCrashOOM")/*Trace thread crashed as out of memory: */ + oome.getMessage());
			oome.printStackTrace();
		} catch (Exception e) {
			logger.fatal(Locale.get("trace.TraceThreadCrashWith")/*Trace thread crashed unexpectedly with error */ +  e.getMessage());
			e.printStackTrace();
		} catch (Throwable t) {
			running = false;
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

	public RoutePositionMark getDest() {
		return dest;
	}

	// remove the command only if icon menus are not used
	public void removeCommand(Command c) {
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			super.removeCommand(c);
		}
	}

	public synchronized void pause() {
		logger.debug("Pause application called");
		if (imageCollector != null) {
			if (! (routeCalc || route != null)) {
				logger.debug("Suspending imageCollector");
				imageCollector.suspend();
			}
		}
		// don't pause if we're logging GPX or routing
		if (locationProducer != null && !gpx.isRecordingTrk() && ! (routeCalc || route != null)) {
			logger.debug("Closing locationProducer");
			locationProducer.close();
			// wait for locationProducer to close
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
				logger.error(Locale.get("trace.LocationProducerTookTooLong")/*LocationProducer took too long to close, giving up*/);
			}
		}
	}

	public void resumeAfterPause() {
		logger.debug("resuming application after pause");
		if (imageCollector != null) {
			imageCollector.resume();
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_AUTO_START_GPS) && !running && (locationProducer == null)
		    //#if polish.android
		    && Configuration.getCfgBitState(Configuration.CFGBIT_GPS_CONNECTED)
		    //#endif
		    
			) {
			Thread thread = new Thread(this,"LocationProducer init");
			thread.start();
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
		if ( gpsRecenter
				&&
			 Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AUTO_RECALC)
			    &&
			 !isZoomedOutTooFarForRouteCalculation()
		) {
			if (Math.abs(System.currentTimeMillis()-oldRecalculationTime) >= 7000) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
					GpsMid.mNoiseMaker.playSound(RouteSyntax.getInstance().getRecalculationSound(), (byte) 5, (byte) 1 );
				}
				//#debug debug
				logger.debug("autoRouteRecalculate");
				// recalculate route
				commandAction(ROUTING_START_CMD);
			}
		}
	}
	
	private boolean isZoomedOutTooFarForRouteCalculation() {	
		// never recalculate when zoomed out so far that not all routable ways are rendered		
		if (scale > (Legend.lowestTileScaleLevelWithRoutableWays * Configuration.getMaxDetailBoostMultiplier())) {
			receiveMessage(Locale.get("trace.ZoomedOutTooFarToCalculateRoute")); /* Zoomed out too far to calculate route */
			return true;
		}
		return false;
	}

	
	
	public boolean isGpsConnected() {
		return (locationProducer != null && solution != LocationMsgReceiver.STATUS_OFF);
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
		//TODO Cleanup addCommand(CMDS[MANAGE_TRACKS_CMD]);
		//addCommand(CMDS[MAN_WAYP_CMD]);
		addCommand(CMDS[ROUTINGS_CMD]);
		addCommand(CMDS[RECORDINGS_CMD]);
		addCommand(CMDS[MAPFEATURES_CMD]);
		addCommand(CMDS[DATASCREEN_CMD]);
		addCommand(CMDS[OVERVIEW_MAP_CMD]);
		//#if polish.api.online
		addCommand(CMDS[ONLINE_INFO_CMD]);
		//#if polish.api.osm-editing
		addCommand(CMDS[RETRIEVE_XML]);
		addCommand(CMDS[EDIT_ENTITY]);
		addCommand(CMDS[RETRIEVE_NODE]);
		addCommand(CMDS[EDIT_ADDR_CMD]);
		//#endif
		//#endif
		addCommand(CMDS[SETUP_CMD]);
		addCommand(CMDS[ABOUT_CMD]);
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN)) {
				//#ifndef polish.android
				super.addCommand(CMDS[ICON_MENU]);
				//#endif
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
		//TODO Cleanup removeCommand(CMDS[MANAGE_TRACKS_CMD]);
		//removeCommand(CMDS[MAN_WAYP_CMD]);
		removeCommand(CMDS[MAPFEATURES_CMD]);
		removeCommand(CMDS[RECORDINGS_CMD]);
		removeCommand(CMDS[ROUTINGS_CMD]);
		removeCommand(CMDS[DATASCREEN_CMD]);
		removeCommand(CMDS[OVERVIEW_MAP_CMD]);
		//#if polish.api.online
		removeCommand(CMDS[ONLINE_INFO_CMD]);
		//#if polish.api.osm-editing
		removeCommand(CMDS[RETRIEVE_XML]);
		removeCommand(CMDS[EDIT_ENTITY]);
		removeCommand(CMDS[RETRIEVE_NODE]);
		removeCommand(CMDS[EDIT_ADDR_CMD]);
		//#endif
		//#endif
		removeCommand(CMDS[SETUP_CMD]);
		removeCommand(CMDS[ABOUT_CMD]);
		removeCommand(CMDS[CELLID_LOCATION_CMD]);
		removeCommand(CMDS[MANUAL_LOCATION_CMD]);
		
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN)) {
				//#ifndef polish.android
				super.removeCommand(CMDS[ICON_MENU]);
				//#endif
			}
		}
	}

	/** Sets the Canvas to fullScreen or windowed mode
	 * when icon menus are active the Menu command gets removed
	 * so the Canvas will not unhide the menu bar first when pressing fire (e.g. on SE mobiles)
	*/
	public void setFullScreenMode(boolean fullScreen) {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
			//#ifndef polish.android
			if (fullScreen) {
				super.removeCommand(CMDS[ICON_MENU]);
			} else
			//#endif
			{
				//#ifndef polish.android
				super.addCommand(CMDS[ICON_MENU]);
				//#endif
			}
		}
//#if polish.android
		if (fullScreen) {
			MidletBridge.instance.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			MidletBridge.instance.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
//#else
		super.setFullScreenMode(fullScreen);
//#endif

	}

	public void commandAction(int actionId) {
		// take care we'll update actualWay
		if (actionId == RETRIEVE_XML || actionId == EDIT_ADDR_CMD) {
			repaint();
		}
		//#if polish.android
		final int actionToRun = actionId;
		// FIXME would be better to use AsyncTask,
		// see http://developer.android.com/resources/articles/painless-threading.html
		MidletBridge.instance.runOnUiThread(
			new Runnable() {
				public void run() {
					commandAction(CMDS[actionToRun], null);
				}
			});
		//#else
		commandAction(CMDS[actionId], null);
		//#endif
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
				int backLightLevelIndexDiff = 0;
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
					backLightLevelIndexDiff = -1;
				} else if (c == CMDS[PAN_RIGHT2_CMD]) {
					if (TrackPlayer.isPlaying) {
						TrackPlayer.faster();
					} else if (manualRotationMode) {
						courseDiff=5;
					} else {
						panX = 2;
					}
					backLightLevelIndexDiff = 1;
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
				
				if (backLightLevelIndexDiff !=0  &&  System.currentTimeMillis() < (lastBackLightOnTime + 5000)) {
					// turn backlight always on when dimming
					Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON, true, false);
					lastBackLightOnTime = System.currentTimeMillis();
					Configuration.addToBackLightLevel(backLightLevelIndexDiff);
					parent.showBackLightLevel();
				} else if (imageCollector != null) {
					if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION) && compassProducer != null) {
						// set compass compassDeviation
						if (compassDeviation == 360) {
							compassDeviation = 0;
						} else {
							compassDeviation += courseDiff;
							compassDeviation %= 360;
							if (compassDeviation < 0) {
								compassDeviation += 360;
							}
							deviateCompass();
							course = compassDeviated;
							updatePosition();
						}
					} else {
						// manual rotation 
						if (courseDiff == 360) {
							course = 0; //N
						} else {
							course += courseDiff;
							validateCourse();
							course %= 360;
							if (course < 0) {
								course += 360;
							}
						}
						if (panX != 0 || panY != 0) {
							gpsRecenter = false;
						}
					}
					imageCollector.getCurrentProjection().pan(center, panX, panY);
				}
				gpsRecenter = false;
				return;
			}
			if (c == CMDS[EXIT_CMD]) {
				// FIXME: This is a workaround. It would be better if recording
				// would not be stopped when leaving the map.
				if (Legend.isValid && gpx.isRecordingTrk()) {
					alert(Locale.get("trace.RecordMode")/*Record Mode*/, Locale.get("trace.PleaseStopRecording")/*Please stop recording before exit.*/ , 2500);
					return;
				}
				
				if (Legend.isValid) {
					pause();
				}
				parent.exit();
				return;
			}
			if (c == CMDS[START_RECORD_CMD]) {
				try {
					//#if polish.android
					getPersistent();
					//#endif
					gpx.newTrk(false);
					alert(Locale.get("trace.GpsRecording")/*Gps track recording*/, Locale.get("trace.StartingToRecord")/*Starting to record*/, 1250);
				} catch (RuntimeException e) {
					receiveMessage(e.getMessage());
				}
				recordingsMenu = null; // refresh recordings menu
				return;
			}
			if (c == CMDS[STOP_RECORD_CMD]) {
				gpx.saveTrk(false);
				//#if polish.android
				leavePersistent();
				//#endif
				alert(Locale.get("trace.GpsRecording")/*Gps track recording*/, Locale.get("trace.StoppingToRecord")/*Stopping to record*/, 1250);
				recordingsMenu = null; // refresh recordings menu
				return;
			}
			if (c == CMDS[MANAGE_TRACKS_CMD]) {
				if (gpx.isRecordingTrk() && !gpx.isRecordingTrkSuspended()) {
					// FIXME it's not strictly necessary to stop, after there are translation for the pause
					// message, change to Locale.get("trace.YouNeedStopPauseRecording")
					alert(Locale.get("trace.RecordMode")/*Record Mode*/, Locale.get("trace.YouNeedStopRecording")/*You need to stop recording before managing tracks.*/ , 4000);
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
						alert(Locale.get("trace.Trackplayer")/*Trackplayer*/, Locale.get("trace.PlayingStopped")/*Playing stopped for connecting to GPS*/, 2500);
					}
					Thread thread = new Thread(this,"LocationProducer init");
					thread.start();
				}
				return;
			}

			if (c == CMDS[DISCONNECT_GPS_CMD]) {
				//#if polish.android
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_GPS_CONNECTED, false);
				//#endif
				if (locationProducer != null) {
					locationProducer.close();
				}
				return;
			}
			
			if (c == CMDS[TOGGLE_GPS_CMD]) {
				if (isGpsConnected()) {
					commandAction(DISCONNECT_GPS_CMD);
				} else {
					commandAction(CONNECT_GPS_CMD);					
				}
				return;
			}

			if (c == CMDS[SEARCH_CMD]) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)
				    && hasPointerEvents()) {
					showingTraceIconMenu = false;
					showingSplitCMS = false;
					showingSplitSearch = true;
					guiSearch = new GuiSearch(this, GuiSearch.ACTION_DEFAULT);
					guiSearch.sizeChanged(getWidth(), getHeight());
					refreshWindowLayout();
				} else {
					guiSearch = new GuiSearch(this, GuiSearch.ACTION_DEFAULT);
					guiSearch.show();
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
			if (c == CMDS[MANAGE_WAYP_CMD]) {
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
					int noElements = 5;
					//#if polish.api.mmapi
					noElements += 2;
					//#endif
					//#if polish.api.wmapi
					if (hasJSR120) {
						noElements++;
					}
					//#endif
					
					int idx = 0;
					String[] elements;
					if (gpx.isRecordingTrk()) {
						noElements++;
						elements = new String[noElements];
						recordingsMenuCmds = new int[noElements];
						recordingsMenuCmds[idx] = STOP_RECORD_CMD;
						elements[idx++] = Locale.get("trace.StopGpxTracklog")/*Stop GPX tracklog*/;
						if (gpx.isRecordingTrkSuspended()) {
							recordingsMenuCmds[idx] = TOGGLE_RECORDING_SUSP_CMD;
							elements[idx++] = Locale.get("trace.ResumeRecording")/*Resume recording*/;							
						} else {
							recordingsMenuCmds[idx] = TOGGLE_RECORDING_SUSP_CMD;
							elements[idx++] = Locale.get("trace.SuspendRecording")/*Suspend recording*/;
						}
					} else {
						elements = new String[noElements];
						recordingsMenuCmds = new int[noElements];
						recordingsMenuCmds[idx] = START_RECORD_CMD;
						elements[idx++] = Locale.get("trace.StartGpxTracklog")/*Start GPX tracklog*/;
					}
					
					recordingsMenuCmds[idx] = SAVE_WAYP_CMD;
					elements[idx++] = Locale.get("trace.SaveWaypoint")/*Save waypoint*/;
					recordingsMenuCmds[idx] = ENTER_WAYP_CMD;
					elements[idx++] = Locale.get("trace.EnterWaypoint")/*Enter waypoint*/;
					recordingsMenuCmds[idx] = MANAGE_TRACKS_CMD;
					elements[idx++] = Locale.get("trace.ManageTracks")/*Manage tracks*/;
					recordingsMenuCmds[idx] = MANAGE_WAYP_CMD;
					elements[idx++] = Locale.get("trace.ManageWaypoints")/*Manage waypoints*/;
					//#if polish.api.mmapi
					recordingsMenuCmds[idx] = CAMERA_CMD;
					elements[idx++] = Locale.get("trace.TakePictures")/*Take pictures*/;
					if (audioRec.isRecording()) {
						recordingsMenuCmds[idx] = TOGGLE_AUDIO_REC;
						elements[idx++] = Locale.get("trace.StopAudioRecording")/*Stop audio recording*/;
					} else {
						recordingsMenuCmds[idx] = TOGGLE_AUDIO_REC;
						elements[idx++] = Locale.get("trace.StartAudioRecording")/*Start audio recording*/;
						
					}
					//#endif
					//#if polish.api.wmapi
					if (hasJSR120) {
						recordingsMenuCmds[idx] = SEND_MESSAGE_CMD;
						elements[idx++] = Locale.get("trace.SendSMSMapPos")/*Send SMS (map pos)*/;
					}
					//#endif
					
					recordingsMenu = new List(Locale.get("trace.Recordings")/*Recordings...*/,
							Choice.IMPLICIT, elements, null);
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
						elements[0] = Locale.get("trace.StopRouting")/*Stop routing*/;
					} else {
						elements[0] = Locale.get("trace.CalculateRoute")/*Calculate route*/;
					}
					elements[1] = Locale.get("trace.SetDestination")/*Set destination*/;
					elements[2] = Locale.get("trace.ShowDestination")/*Show destination*/;
					elements[3] = Locale.get("trace.ClearDestination")/*Clear destination*/;
					routingsMenu = new List(Locale.get("trace.Routing2")/*Routing..*/, Choice.IMPLICIT, elements, null);
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
					PositionMark posMark = getPosMark();
					/*
					  if (Configuration.getCfgBitState(Configuration.CFGBIT_WAYPT_OFFER_PREDEF)) {
					  if (guiWaypointPredefined == null) {
					  guiWaypointPredefined = new GuiWaypointPredefined(this);
					  }
					  if (guiWaypointPredefined != null) {
					  guiWaypointPredefined.setData(posMark);
					  guiWaypointPredefined.show();
					  }
					  } else {
					  showGuiWaypointSave(posMark);
					  }
					*/
					showGuiWaypointSave(posMark);
				}
				return;
			}
			if (c == CMDS[ONLINE_INFO_CMD] && internetAccessAllowed()) {
				contextMenu();
			}
			if (c == CMDS[BACK_CMD]) {
				show();
				return;
			}
			if (c == CMDS[OK_CMD]) {
				if (d == recordingsMenu) {
					int recCmd = recordingsMenu.getSelectedIndex();
					if (recCmd >= 0 && recCmd < recordingsMenuCmds.length) {
						recCmd = recordingsMenuCmds[recCmd];
						if (recCmd == STOP_RECORD_CMD) {
	    					commandAction(STOP_RECORD_CMD);
							if (! Configuration.getCfgBitState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_STOP)) {
						    	show();
							}
						} else if (recCmd == START_RECORD_CMD) {
							commandAction(START_RECORD_CMD);
							if (! Configuration.getCfgBitState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_START)) {
							   	show();
							}
						} else if (recCmd == TOGGLE_RECORDING_SUSP_CMD) {
							commandAction(TOGGLE_RECORDING_SUSP_CMD);
							show();
			    		} else {
			    			commandAction(recCmd);
			    		}
					}
				} else if (d == routingsMenu) {
					show();
					switch (routingsMenu.getSelectedIndex()) {
					case 0: {
						if (routeCalc || route != null) {
							commandAction(ROUTING_STOP_CMD);
						} else {
							commandAction(ROUTING_START_WITH_OPTIONAL_MODE_SELECT_CMD);
						}
						break;
					}
					case 1: {
						commandAction(SET_DEST_CMD);
						break;
					}
					case 2: {
						commandAction(SHOW_DEST_CMD);
						break;
					}
					case 3: {
						commandAction(CLEAR_DEST_CMD);
						break;
					}
					}
				}
				return;
			}
			//#if polish.api.mmapi
			if (c == CMDS[CAMERA_CMD]) {
				try {
					Class GuiCameraClass = Class.forName("de.ueller.gpsmid.ui.GuiCamera");
					Object GuiCameraObject = GuiCameraClass.newInstance();
					GuiCameraInterface cam = (GuiCameraInterface)GuiCameraObject;
					cam.init(this);
					cam.show();
				} catch (ClassNotFoundException cnfe) {
					logger.exception(Locale.get("trace.YourPhoneNoCamSupport")/*Your phone does not support the necessary JSRs to use the camera*/, cnfe);
				}
				return;
			}
			if (c == CMDS[TOGGLE_AUDIO_REC]) {
				if (audioRec.isRecording()) {
					audioRec.stopRecord();
				} else {
					audioRec.startRecorder();
				}
				recordingsMenu = null; // Refresh recordings menu
				return;
			}
			//#endif
			if (c == CMDS[ROUTING_TOGGLE_CMD]) {
				if (routeCalc || route != null) {
					commandAction(ROUTING_STOP_CMD);
				} else {
					commandAction(ROUTING_START_WITH_OPTIONAL_MODE_SELECT_CMD);
				}
				return;
			}

			if (c == CMDS[ROUTING_START_WITH_MODE_SELECT_CMD]) {
				gpsRecenter = true;
				gpsRecenterInvalid = true;
				gpsRecenterStale = true;
				GuiRoute guiRoute = new GuiRoute(this, false);
				guiRoute.show();
				return;
			}

			if (c == CMDS[ROUTING_START_WITH_OPTIONAL_MODE_SELECT_CMD]) {
				gpsRecenter = true;
				gpsRecenterInvalid = true;
				gpsRecenterStale = true;
				if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_DONT_ASK_FOR_ROUTING_OPTIONS)) {
					commandAction(ROUTING_START_CMD);
				} else {
					GuiRoute guiRoute = new GuiRoute(this, false);
					guiRoute.show();
				}
				return;
			}
			
			if (c == CMDS[SAVE_ROUTE_AS_GPX]) {
				if (route != null) {
					RouteInstructions.outputRouteAsGpx(route);
				} else {
					alert(Locale.get("guidiscovericonmenu.Routing"), Locale.get("trace.NoRoute")/*Route hasn't been calculated*/, 3000);
				}
				return;
			}
			if (c == CMDS[ROUTING_START_CMD]) {
				startRouting();
				return;
			}
			if (c == CMDS[ROUTING_STOP_CMD]) {
				stopRouting(true);
				return;
			}
			if (c == CMDS[ROUTING_RECALC_CMD]) {
				stopRouting(false);
				startRouting();
				return;
			}
			if (c == CMDS[ZOOM_IN_CMD]) {
				scale = scale / Configuration.getZoomFactor();
				autoZoomed = false;
				return;
			}
			if (c == CMDS[ZOOM_OUT_CMD]) {
				scale = scale * Configuration.getZoomFactor();
				autoZoomed = false;
				return;
			}
			if (c == CMDS[MANUAL_ROTATION_MODE_CMD]) {
				manualRotationMode = !manualRotationMode;
				if (manualRotationMode) {
					if (hasPointerEvents()) {
						alert(Locale.get("trace.ManualRotation")/*Manual Rotation*/, Locale.get("trace.ChangeCourse")/*Change course with zoom buttons*/, 3000);
					} else {
						alert(Locale.get("trace.ManualRotation")/*Manual Rotation*/, Locale.get("trace.ChangeCourseWithLeftRightKeys")/*Change course with left/right keys*/, 3000);
					}
				} else {
					alert(Locale.get("trace.ManualRotation")/*Manual Rotation*/, Locale.get("generic.Off")/*Off*/, 750);
				}
				return;
			}
			if (c == CMDS[TOGGLE_OVERLAY_CMD]) {
				showAddons++;
				repaint();
				return;
			}
			if (c == CMDS[TOGGLE_UNUSEABLEWAYS_DARKER]) {
				Configuration.setCfgBitState(Configuration.CFGBIT_DRAW_NON_TRAVELMODE_WAYS_DARKER,
						!(Configuration.getCfgBitState(Configuration.CFGBIT_DRAW_NON_TRAVELMODE_WAYS_DARKER)),
						false);
				newDataReady();
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
			if (c == CMDS[HELP_ONLINE_TOUCH_CMD] && internetAccessAllowed()) {
				GuiWebInfo.openUrl(GuiWebInfo.getStaticUrlForSite(Locale.get("guiwebinfo.helptouch")));
				return;
			}
			if (c == CMDS[HELP_ONLINE_WIKI_CMD] && internetAccessAllowed()) {
				GuiWebInfo.openUrl(GuiWebInfo.getStaticUrlForSite(Locale.get("guiwebinfo.helpwiki")));
				return;
			}
			//#if polish.android
			if (c == CMDS[HELP_ONLINE_WIKI_ANDROID_CMD] && internetAccessAllowed()) {
				GuiWebInfo.openUrl(GuiWebInfo.getStaticUrlForSite(Locale.get("guiwebinfo.helpwikiandroid")));
				return;
			}
			//#endif
			if (c == CMDS[OPEN_MAP_CREDIT_URL] && internetAccessAllowed()) {
				GuiWebInfo.openUrl(GuiWebInfo.getStaticUrlForSite(Locale.get("trace.showmapcredit")));
				return;
			}
			if (c == CMDS[KEYS_HELP_CMD]) {
				GuiKeyShortcuts gks = new GuiKeyShortcuts(this);
				gks.show();
				return;
			}
			if (c == CMDS[TOUCH_HELP_CMD]) {
				TouchHelper th = new TouchHelper(this);
				th.show();
				return;
			}
			if (c == CMDS[NORTH_UP_CMD]) {
				course = 0;
				invalidateCourse();
				alert(Locale.get("trace.ManualRotation"), Locale.get("trace.ManualToNorth"), 750);
				return;
			}
			if (c == CMDS[TOGGLE_MAP_PROJ_CMD]) {
				if (manualRotationMode) {
					if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION) && compassProducer != null) {
						compassDeviation = 0;
					} else {
						course = 0;
						invalidateCourse();
					}
					alert(Locale.get("trace.ManualRotation"), Locale.get("trace.ManualToNorth"), 750);
				} else {
					// FIXME rename string to generic
					alert(Locale.get("guidiscover.MapProjection")/*Map Projection*/, ProjFactory.nextProj(), 750);
				}
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
					alert(Locale.get("trace.GpsMid")/*GpsMid*/, hasPointerEvents() ? Locale.get("trace.KeysAndTouchscreenUnlocked")/*Keys and touch screen unlocked*/ : Locale.get("trace.KeysUnlocked")/*Keys unlocked*/, 1000);
				}
				return;
			}
			if (c == CMDS[TOGGLE_RECORDING_CMD]) {
				if ( gpx.isRecordingTrk() ) {
					commandAction(STOP_RECORD_CMD);
				} else {
					commandAction(START_RECORD_CMD);
				}
				return;
			}
			if (c == CMDS[TOGGLE_RECORDING_SUSP_CMD]) {
				if (gpx.isRecordingTrk()) {
					if ( gpx.isRecordingTrkSuspended() ) {
						alert(Locale.get("trace.GpsRecording")/*Gps track recording*/, Locale.get("trace.ResumingRecording")/*Resuming recording*/, 1000);
						gpx.resumeTrk();
					} else {
						alert(Locale.get("trace.GpsRecording")/*Gps track recording*/, Locale.get("trace.SuspendingRecording")/*Suspending recording*/, 1000);
						gpx.suspendTrk();
					}
				}
				recordingsMenu = null; // Refresh recordings menu
				return;
			}
			if (c == CMDS[RECENTER_GPS_CMD]) {
				gpsRecenter = true;
				gpsRecenterInvalid = true;
				gpsRecenterStale = true;
				autoZoomed = true;
				if (pos.latitude != 0.0f) {
					receivePosition(pos);
				}
				newDataReady();
				return;
			}
			if (c == CMDS[SHOW_DEST_CMD]) {
				if (dest != null) {
					//We are explicitly setting the map to this position, so we probably don't
					//want it to be recentered on the GPS immediately.
					gpsRecenter = false;
										
					prevPositionNode = center.copy(); 
					center.setLatLonRad(dest.lat, dest.lon);						
					movedAwayFromDest = false;
					updatePosition();
				}
				else {
					alert(Locale.get("trace.ShowDestination")/*Show destination*/, Locale.get("trace.DestinationNotSpecifiedYet")/*Destination is not specified yet*/, 3000);
				}
				
				return;
			}
			if (c == CMDS[SHOW_PREVIOUS_POSITION_CMD]) {
				if (prevPositionNode != null) {
					//We are explicitly setting the map to this position, so we probably don't
					//want it to be recentered on the GPS immediately.
					gpsRecenter = false;
					
					center.setLatLon(prevPositionNode);						
	
					updatePosition();
				}
				return;
			}
			if (c == CMDS[CMS_CMD]) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)
				    && hasPointerEvents()) {
					cmsl = new CMSLayout(mapWindow.getMinX(), 0 + rootWindow.getHeight() / 2, rootWindow.getMaxX(), rootWindow.getMaxY());
					guiTrip = new GuiTrip();
					guiTrip.init();
					guiTacho = new GuiTacho();
					guiTacho.init();
					showingSplitCMS = true;
					showingTraceIconMenu = false;
					cmsl.setOnScreenButtonSize(false);
					//cmsl.sizeChanged(getWidth(), getHeight());
					refreshWindowLayout();
				}
				return;
			}

			if (c == CMDS[DATASCREEN_CMD]) {
				showNextDataScreen(DATASCREEN_NONE);
				return;
			}

			if (c == CMDS[ICON_MENU] && Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
				// make markers accessible on keyboard-only phones
				if (!hasPointerEvents() && Configuration.getCfgBitState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS)) {
					touchX = pc.getP().getImageCenter().x - imageCollector.xScreenOverscan;
					touchY = pc.getP().getImageCenter().y - imageCollector.yScreenOverscan;
				}

				if (!hasPointerEvents() && Configuration.getCfgBitState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS) && getClickableMarker(touchX, touchY) != null) {
					contextMenu();
				} else {
					if (isShowingSplitScreen()) {
						stopShowingSplitScreen();
					} else {
						showIconMenu();
					}
				}
				return;
			}
			if (c == CMDS[SETUP_CMD]) {
				guiDiscover = new GuiDiscover(parent);
				if (isShowingSplitIconMenu()) {
					guiDiscoverIconMenu = new GuiDiscoverIconMenu(guiDiscover, guiDiscover);
					showingTraceIconMenu = false;
					showingSplitSetup = true;
					showingSplitCMS = false;
					refreshWindowLayout();
					guiDiscoverIconMenu.sizeChanged(getWidth(), getHeight());
				}
				return;
			}
			if (c == CMDS[ABOUT_CMD]) {
				new Splash(parent, GpsMid.initDone);
				return;
			}
			if (c == CMDS[CELLID_LOCATION_CMD]) {
				if (Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_SECELL && locationProducer != null) {
						locationProducer.triggerPositionUpdate();
						newDataReady();
				} else {
					if (cellIDLocationProducer == null) {
						// init sleeping cellid location provider if cellid is not primary
						cellIDLocationProducer = new SECellId();
						if (cellIDLocationProducer != null && !cellIDLocationProducer.init(this)) {
							logger.info("Failed to initialise CellID location producer");
						}
					}
					if (cellIDLocationProducer != null) {
						cellIDLocationProducer.triggerPositionUpdate();
						newDataReady();
					}
				}
				return;
			}
			if (c == CMDS[MANUAL_LOCATION_CMD]) {
				Position setpos = new Position(center.radlat / MoreMath.FAC_DECTORAD,
    								center.radlon / MoreMath.FAC_DECTORAD,
								    PositionMark.INVALID_ELEVATION, 0.0f, 0.0f, 1,
							       	    System.currentTimeMillis(), Position.TYPE_MANUAL);
				// implies center to gps, to give feedback as the gps rectangle
				gpsRecenter = true;
				// gpsRecenterInvalid = true;
				// gpsRecenterStale = true;
				autoZoomed = true;
				receivePosition(setpos);
				receiveStatus(LocationMsgReceiver.STATUS_MANUAL, 0);
				newDataReady();
				return;
			}
			
			if (! routeCalc) {
				//#if polish.api.osm-editing
				if (c == CMDS[RETRIEVE_XML] && internetAccessAllowed()) {
					if (Legend.enableEdits) {
						// -1 alert ("Editing", "Urlidx: " + pc.actualWay.urlIdx, Alert.FOREVER);
						if ((pc.actualWay != null) && (getUrl(pc.actualWay.urlIdx) != null)) {
							parent.alert ("Url", "Url: " + getUrl(pc.actualWay.urlIdx), Alert.FOREVER);
						}

						if ((actualWay != null) && (actualWay instanceof EditableWay)) {
							EditableWay eway = (EditableWay)actualWay;
							GuiOsmWayDisplay guiWay = new GuiOsmWayDisplay(eway, actualSingleTile, this);
							guiWay.show();
							guiWay.refresh();
						}
					} else {
						parent.alert("Editing", "Editing support was not enabled in Osm2GpsMid", Alert.FOREVER);
					}
				}
				if (c == CMDS[EDIT_ENTITY] && internetAccessAllowed()) {
					// if we clicked a clickable marker, get coords from the marker instead of tap
					int x = centerP.x;
					int y = centerP.y;
					int nodeID = -1;
					int xOverScan = 0;
					int yOverScan = 0;
					if (imageCollector != null) {
						xOverScan = imageCollector.xScreenOverscan;
						yOverScan = imageCollector.yScreenOverscan;
						panProjection=imageCollector.getCurrentProjection();
					} else {
						panProjection = null;
					}

					ClickableCoords coords = getClickableMarker(x - xOverScan, y - yOverScan);
					if (coords != null) {
						x = coords.x;
						y = coords.y;
						nodeID = coords.nodeID;
						System.out.println("NodeID: " + nodeID);
					}
					if (Legend.enableEdits) {
						// FIXME: do the following:
						// * set a flag that default operation is OSM edit
						// * do a search for nearby POIs, asking for type
						// * when the user selects, open OSM editing

						if (nodeID != -1) {
							GuiOsmPoiDisplay guiNode = new GuiOsmPoiDisplay(nodeID, null,
										 center.radlat, center.radlon, this);
							guiNode.show();
							guiNode.refresh();
							return;
						} else {
							GuiSearch guiSearch = new GuiSearch(this, GuiSearch.ACTION_EDIT_ENTITY);
							guiSearch.show();
							return;
						}
					} else {
						parent.alert("Editing", "Editing support was not enabled in Osm2GpsMid", Alert.FOREVER);
					}
				}
				if (c == CMDS[RETRIEVE_NODE] && internetAccessAllowed()) {
					if (Legend.enableEdits) {
						GuiOsmPoiDisplay guiNode = new GuiOsmPoiDisplay(-1, null,
								center.radlat, center.radlon, this);
						guiNode.show();
						guiNode.refresh();
					} else {
						logger.error(Locale.get("trace.EditingIsNotEnabled")/*Editing is not enabled in this map*/);
					}
				}
				if (c == CMDS[EDIT_ADDR_CMD] && internetAccessAllowed()) {
					if (Legend.enableEdits) {
						String streetName = "";
						//if ((pc != null) && (pc.actualWay != null)) {
						//	streetName = getName(pc.actualWay.nameIdx);
						//}
						if (actualWay != null) {
							streetName = getName(actualWay.nameIdx);
						}
						GuiOsmAddrDisplay guiAddr = new GuiOsmAddrDisplay(-1, streetName, null,
								center.radlat, center.radlon, this);
						guiAddr.show();
					} else {
						logger.error(Locale.get("trace.EditingIsNotEnabled")/*Editing is not enabled in this map*/);
					}
				}
				//#else
				if (c == CMDS[RETRIEVE_XML] || c == CMDS[RETRIEVE_NODE] || c == CMDS[EDIT_ADDR_CMD]) {
					alert("No online capabilites",
					      Locale.get("trace.SetAppGeneric")/*Set app=GpsMid-Generic-editing and enableEditing=true in*/ +
					      Locale.get("trace.PropertiesFile")/*.properties file and recreate GpsMid with Osm2GpsMid.*/,
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
				if (c == CMDS[ROTATE_TRAVEL_MODE_CMD]) {
					int mode = Configuration.getTravelModeNr();
					mode++;
					if (mode >= Legend.getTravelModes().length) {
						mode = 0;
					}
					Configuration.setTravelMode(mode);
					if (Configuration.getCfgBitState(Configuration.CFGBIT_DRAW_NON_TRAVELMODE_WAYS_DARKER)) {
							newDataReady();
					}
					return;
				}
			} else {
				alert(Locale.get("trace.Error")/*Error*/, Locale.get("trace.CurrentlyInRouteCalculation")/*Currently in route calculation*/, 2000);
			}
		} catch (Exception e) {
 			logger.exception(Locale.get("trace.InTraceCommandAction")/*In Trace.commandAction*/, e);
		}

	}
	
	private void startRouting() {
		try {
			if (isZoomedOutTooFarForRouteCalculation()) {
				return;
			}
				
			if (!routeCalc || RouteLineProducer.isRunning()) { // if not in route calc or already producing the route line
				// if the route line is currently being produced stop it  
				if (RouteLineProducer.isRunning()) {
					RouteInstructions.abortRouteLineProduction();						
				}
				routeCalc = true;
				if (Configuration.getContinueMapWhileRouteing() != Configuration.continueMap_Always ) {
					stopImageCollector();
				}
				RouteInstructions.resetOffRoute(route, center);
				// center of the map is the route source
				RoutePositionMark routeSource = new RoutePositionMark(center.radlat, center.radlon);
				logger.info("Routing source: " + routeSource);
				routeEngine = new Routing(this);
				routeEngine.solve(routeSource, dest);
//					resume();
			}
			routingsMenu = null; // refresh routingsMenu
		} catch (Exception e) {
 			logger.exception(Locale.get("trace.InTraceCommandAction")/*In Trace.commandAction*/, e);
		}
	}

	private ClickableCoords getCenterMarkerCoords() {
		// if we clicked a clickable marker, get coords from the marker instead of tap
		int x = centerP.x;
		int y = centerP.y;
		int xOverScan = 0;
		int yOverScan = 0;
		if (imageCollector != null) {
			xOverScan = imageCollector.xScreenOverscan;
			yOverScan = imageCollector.yScreenOverscan;
			panProjection=imageCollector.getCurrentProjection();
		} else {
			panProjection = null;
		}
		return getClickableMarker(x - xOverScan, y - yOverScan);
	}

	private void contextMenu() {
		int x = 0;
		int y = 0;
		ClickableCoords coords = getCenterMarkerCoords();
		String url = null;
		String phone = null;
		if (coords != null) {
			x = coords.x;
			y = coords.y;
			url = getUrl(coords.urlIdx);
			phone = getUrl(coords.phoneIdx);
		}
		// open a place&marker-related menu
		// use the place of touch instead of old center as position,
		centerNode=panProjection.inverse(x,
						 y, centerNode);
		Position oPos = new Position(centerNode.radlat, centerNode.radlon,
					     0.0f, 0.0f, 0.0f, 0, 0);
		GuiWebInfo gWeb = new GuiWebInfo(this, oPos, pc, true, coords != null ? url : null,
						 coords != null ? phone : null,
						 coords != null ? coords.nodeID : -1);
		gWeb.show();
		//#if 0
		alert(Locale.get("trace.NoOnlineCapa")/*No online capabilites*/,
		      Locale.get("trace.SetAppGeneric")/*Set app=GpsMid-Generic-editing and enableEditing=true in*/ +
		      Locale.get("trace.PropertiesFile")/*.properties file and recreate GpsMid with Osm2GpsMid.*/,
		      Alert.FOREVER);
		//#endif
	}

	private void stopRouting(boolean showAlert) {
				NoiseMaker.stopPlayer();
				if (routeCalc) {
					if (routeEngine != null) {
						routeEngine.cancelRouting();
					}
					if (showAlert) {
						alert(Locale.get("trace.RouteCalculation")/*Route Calculation*/, Locale.get("trace.Cancelled")/*Cancelled*/, 1500);
					}
				} else {
					if (showAlert) {
						alert(Locale.get("trace.Routing")/*Routing*/, Locale.get("generic.Off")/*Off*/, 750);
					}
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
	}

	private void startImageCollector() throws Exception {
		//#debug info
		logger.info("Starting ImageCollector");
		Images images = new Images();
		pc = new PaintContext(this, images);
		/* move responsibility for overscan to ImageCollector
		int w = (this.getMapWidth() * 125) / 100;
		int h = (this.getMapHeight() * 125) / 100;
		*/
		
		// Ensure that only one image collector runs at the same time.
		if ( imageCollector != null ) {
			imageCollector.stop();
			// alert("FIXME", "Avoided duplicate ImageCollector", 1500);
		}
		
		// FIXME pass layout params to imagecollector
		//refreshWindowLayout();
		//tl = new TraceLayout(mapMinX, mapMinY, mapMaxX, mapMaxY);

		// ImageCollector must not be started with 0x0 image size
		if (mapWindow.getWidth() <= 0 || mapWindow.getHeight() <= 0) {
			refreshWindowLayout(this.getWidth(), this.getHeight());
		}
		int x = (mapWindow.getWidth() > 0) ? mapWindow.getWidth() : this.getWidth();
		int y = (mapWindow.getHeight() > 0) ? mapWindow.getHeight() : this.getHeight();
		System.out.println("Starting image colector " + x + " | " + y);

		imageCollector = new ImageCollector(tiles, x, y, this, images);
//		projection = ProjFactory.getInstance(center,course, scale, getMapWidth(), getMapHeight());
//		pc.setP(projection);
		pc.center = center.copy();
		pc.scale = scale;
		pc.xSize = x;
		pc.ySize = y;
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
	
	public boolean isTileRequiredByImageCollector(Tile t) {
		if (imageCollector != null ) {
			ScreenContext sc = imageCollector.getScreenContext(); 
			if (sc != null && t.contain(sc)) {
				return true;
			}
		}
		return false;
	}
	

	public void startup() throws Exception {
//		logger.info("reading Data ...");
		namesThread = new Names();
		urlsThread = new Urls();
		new DictReader(this);
		// wait until base tiles are read; otherwise there's apparently
		// a race condition triggered on Symbian s60r3 phones, see
		// https://sourceforge.net/support/tracker.php?aid=3284022
		if (Legend.isValid) {
			while (!baseTilesRead) {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e1) {
					// nothing to do in that case						
				}
			}
		} else {
			if (Configuration.usingBuiltinMap()) {
				logger.fatal(Locale.get("legend.bigstyleserrtitle")/*Map format error*/);
			} else {
				logger.fatal(Locale.get("legend.bigstyleserrtitle")/*Map format error*/ + " " + Configuration.getMapUrl());
			}
			commandAction(SETUP_CMD);
		}
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
		setDict(gpx, (byte) DictReader.GPXZOOMLEVEL);
		startImageCollector();
		resetSize();
		// resetSize() does this
		// recreateTraceLayout();
	}

	public void shutdown() {
		if (gpx != null) {
			// change to "false" to ask for track name if configure to
			// currently "true" to not ask for name to ensure fast
			// quit & try to avoid loss of data which might result from
			// waiting for user to type the name
			//#if polish.android
			leavePersistent();
			//#endif
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
	
	public void restart() {
		shutdown();
		setBaseTilesRead(false);
		tiles = new Tile[6];
		try {
			startup();
		} catch (Exception e) {
			logger.fatal(Locale.get("trace.GotExceptionDuringStartup")/*Got an exception during startup: */ + e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	public void restartImageCollector() {
		// don't re-half in split-screen mode
		// refreshWindowLayout((mapMaxX - mapMinX), (mapMaxY - mapMinY));
		updateLastUserActionTime();
		if (imageCollector != null) {
			stopImageCollector();
			try {
				startImageCollector();
				imageCollector.resume();
				imageCollector.newDataReady();
			} catch (Exception e) {
				logger.exception(Locale.get("trace.CouldNotReinitialiseImageCollector")/*Could not reinitialise Image Collector after size change*/, e);
			}
			/**
			 * Recalculate the projection, as it may depends on the size of the screen
			 */
			updatePosition();
		}
		recreateTraceLayout();
	}

	public void sizeChanged(int w, int h) {
		updateLastUserActionTime();
		if (imageCollector != null) {
			logger.info("Size of Canvas changed to " + w + "|" + h);
			System.out.println("Size of Canvas changed to " + w + "|" + h);
			stopImageCollector();
			try {
				refreshWindowLayout(w, h);
				startImageCollector();
				imageCollector.resume();
				imageCollector.newDataReady();
			} catch (Exception e) {
				logger.exception(Locale.get("trace.CouldNotReinitialiseImageCollector")/*Could not reinitialise Image Collector after size change*/, e);
			}
			/**
			 * Recalculate the projection, as it may depends on the size of the screen
			 */
			updatePosition();
		}

		refreshWindowLayout(w, h);
		recreateTraceLayout();
		
		//#if polish.android
		int newAngle = getAndroidRotationAngle();
		//#else
		int newAngle = deviceLayoutIsPortrait() ? 0 : 90;
		//#endif

		if (newAngle != previousAngle) {
			currentLayoutIsPortrait = deviceLayoutIsPortrait();
			compassDeviation += (newAngle - previousAngle);
			if (compassDeviation < 0) {
				compassDeviation += 360;
			}
			compassDeviation %= 360;
		}
		previousAngle = newAngle;

		if (isShowingSplitIconMenu() && (traceIconMenu != null)) {
			traceIconMenu.sizeChanged(w, h);
		}
		if (isShowingSplitSearch() && (guiSearch != null)) {
			guiSearch.sizeChanged(w, h);
		}
		if (isShowingSplitSetup() && (guiDiscoverIconMenu != null)) {
			guiDiscoverIconMenu.sizeChanged(w, h);
		}
		if (isShowingSplitCMS() && (cmsl != null)) {
			cmsl.sizeChanged(w, h);
		}
	}

	//#if polish.android
	public int getAndroidRotationAngle() {
		int angle = 0;
		switch (MidletBridge.instance.getWindowManager().getDefaultDisplay().getRotation()) {
		case Surface.ROTATION_0:
			angle = 0;
			break;
		case Surface.ROTATION_90:
			angle = 90;
			break;
		case Surface.ROTATION_180:
			angle = 180;
			break;
		case Surface.ROTATION_270:
			angle = 270;
			break;
		default:
		}
		return angle;
	}
	//#endif

	private int getMapWidth() {
		return mapWindow.getWidth();
	}

	private int getMapHeight() {
		return mapWindow.getHeight();
	}

	private void refreshWindowLayout(int w, int h) {
		rootWindow.setMinX(0);
		rootWindow.setMinY(0);
		rootWindow.setMaxX(w);
		rootWindow.setMaxY(h);
		mapWindow.setMinX(0);
		mapWindow.setMinY(0);
		mapWindow.setMaxX(w);
		mapWindow.setYPosition(0);
		if (isShowingSplitScreen()) {
			if (mapWindow.getMaxY() != h / 2) {
				mapWindow.setMaxY(h / 2);
				restartImageCollector();
				// FIXME this sets position to other windows,
				// not map window
				// mapWindow.setYPosition(h / 2);
			}
		} else {
			if (mapWindow.getMaxY() != h) {
				mapWindow.setMaxY(h);
				restartImageCollector();
			}
		}
		repaint();
	}

	// used when splitscreen mode changes
	private void refreshWindowLayout() {
		refreshWindowLayout(rootWindow.getMaxX(), rootWindow.getMaxY());
	}


	// check if pointer operation coordinates are for some other function than trace
	private boolean coordsForOthers(int x, int y) {
		if (keyboardLocked) {
			return false;
		}
		boolean notForTrace = (x > mapWindow.getMaxX());
		if (isShowingSplitScreen()) {
			if (y > mapWindow.getMaxY() + mapWindow.getYPosition()) {
				notForTrace = true;
			}
		}
		return notForTrace;
	}

	private void updateCMS(Graphics g) {
		//LayoutElement e = null;
		if (cmsl != null) {
			int maxSpeed = 0;
			if (actualSpeedLimitWay != null) {
				maxSpeed = actualSpeedLimitWay.getMaxSpeed();
			}
			if (maxSpeed != 0) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
					cmsl.ele[CMSLayout.SPEEDING_SIGN].setText(Integer.toString(maxSpeed));
				} else {
					cmsl.ele[CMSLayout.SPEEDING_SIGN].setText(Integer.toString((int)(maxSpeed / 1.609344f + 0.5f)));
				}
			}
			if (guiTrip != null) {
				int y = 48;
				Position pos = getCurrentPosition();
		
				//y = guiTrip.paintTrip(g, (cmsl.getMaxX() - cmsl.getMinX()), this.getWidth()/2, this.getWidth() / 2, this.getHeight() - 40, y, pos, getDestination(), this);
				y = guiTrip.paintTrip(g, this.getWidth() / 4, this.getHeight()/2, this.getWidth() / 2, this.getHeight(), y, pos, getDestination(), this);

				guiTrip.calcSun(this);

				// Draw sunrise and sunset time
				y += 24;
				y = guiTrip.paintSun(g, this.getWidth() / 4,
						     this.getHeight()/2, this.getWidth()/2, this.getHeight(), y);

			}
			if (guiTacho != null) {
				guiTacho.setValues(this, g);
				guiTacho.paintTacho(g, rootWindow.getWidth() / 2,
						    rootWindow.getHeight() / 2,
						    rootWindow.getWidth(),
						    rootWindow.getHeight() / 4 * 3, getCurrentPosition());
			}
		}
	}
		
	protected void paint(Graphics g) {
		//#debug debug
		logger.debug("Drawing Map screen");
		
		if (!Legend.isValid) {
			commandAction(SETUP_CMD);
		}
		try {
			int yc = 1;
			int la = 18;
			getPC();
			//#if polish.api.paintdirect
			g.getCanvas().clipRect(0, 0, this.getWidth(), this.getHeight(), Region.Op.REPLACE);
			//#endif
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
				//#if polish.api.paintdirect
				Node drawnCenter = imageCollector.paintDirect(pc);
				//#else
				Node drawnCenter = imageCollector.paint(pc);
				//#endif
				if (route != null && ri != null && pc.lineP2 != null && pc.getP() != null/*avoids exception at route calc*/) {
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
			} else {
				// show the way bar even if ImageCollector is not running because it's necessary on touch screens to get to the icon menu
				tl.ele[TraceLayout.WAYNAME].setText(" ");
			}

			lLastDragTime = System.currentTimeMillis();
			
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
					if (movedAwayFromDest
					    && Configuration.getCfgBitState(Configuration.CFGBIT_STOP_ROUTING_AT_DESTINATION)) {
						// stop routing
						if (routeCalc) {
							if (routeEngine != null) {
								routeEngine.cancelRouting();
							}
						}
						endRouting();
						// redraw immediately
						synchronized (this) {
							if (imageCollector != null) {
								imageCollector.newDataReady();
							}
						}
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
					if (pc != null) {
						tl.calcScaleBarWidth(pc);
						tl.ele[TraceLayout.SCALEBAR].setText(" ");
					}
				}

				setPointOfTheCompass();
			}
			showMovement(g);

			// Show gpx track recording status
			LayoutElement eSolution = tl.ele[TraceLayout.SOLUTION];
			LayoutElement eRecorded = tl.ele[TraceLayout.RECORDED_COUNT];
			if (gpx.isRecordingTrk()) {
				// we are recording tracklogs
				if (gpx.isRecordingTrkSuspended()) {
					eRecorded.setColor(Legend.COLORS[Legend.COLOR_RECORDING_SUSPENDED_TEXT]); // blue
				} else {
					eRecorded.setColor(Legend.COLORS[Legend.COLOR_RECORDING_ON_TEXT]); // red
				}
				eRecorded.setText(gpx.getTrkPointCount() + Locale.get("trace.r")/*r*/);
			}
			if (TrackPlayer.isPlaying) {
				eSolution.setText(Locale.get("trace.Replay")/*Replay*/);
			} else {
				if (locationProducer == null && !(solution == LocationMsgReceiver.STATUS_CELLID ||
										  solution == LocationMsgReceiver.STATUS_MANUAL)) {
					eSolution.setText(Locale.get("solution.Off")/*Off*/);
				} else {
					if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_ACCURACY) && pos.accuracy != Float.NaN && pos.accuracy > 0) {
						eSolution.setText(solutionStr + "/" + (int) pos.accuracy);
					} else {
						eSolution.setText(solutionStr);
					}
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
				e.setText(Locale.get("trace.cellIDs")/*cellIDs*/);
			}

			// Display tile and paint state
			if ( true ) {
				sbTemp.setLength(0);
				e = tl.ele[TraceLayout.REQUESTED_TILES];
				if (tileReader != null && tileReader.getRequestQueueSize() != 0) {
					// Display the number of not yet loaded tiles.
					sbTemp.append("T ");
					sbTemp.append( tileReader.getRequestQueueSize());
					// sbTemp.append("/" + scale);
				}
				if (imageCollector != null && imageCollector.iDrawState != 0 ) {
					// Display a + if the image collector prepares the image.
					if ( imageCollector.iDrawState == 1 )
						sbTemp.append("+");
					// Display a * if the image collector is drawing.
					else if ( imageCollector.iDrawState == 2 )
						sbTemp.append("*");
				}
				if (sbTemp.length() != 0 )	{
					e.setText(sbTemp.toString());
				}
			}

			
			// show audio recording status
			e = tl.ele[TraceLayout.AUDIOREC];
			if (audioRec.isRecording()) {
				e.setColor(Legend.COLORS[Legend.COLOR_AUDIOREC_TEXT]); // red
				e.setText(Locale.get("trace.AudioRec")/*AudioRec*/);
			}
			
			if (pc != null) {
				showDestination(pc);
			}

			if (speed > 0 &&
					Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_SPEED_IN_MAP)) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
					tl.ele[TraceLayout.SPEED_CURRENT].setText(" " + Integer.toString(speed) + Locale.get("guitacho.kmh")/* km/h*/);
				} else {
					tl.ele[TraceLayout.SPEED_CURRENT].setText(" " + Integer.toString((int)(speed / 1.609344f)) + Locale.get("guitacho.mph"));
				}
			}

			if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_ALTITUDE_IN_MAP)
				&& locationProducer != null
				&& LocationMsgReceiverList.isPosValid(solution)
			) {
				tl.ele[TraceLayout.ALTITUDE].setText(showDistance(altitude, DISTANCE_ALTITUDE));
			}

			if (dest != null && (route == null || (!RouteLineProducer.isRouteLineProduced() && !RouteLineProducer.isRunning()) ) 
				&& Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_IN_MAP)) {
				e = Trace.tl.ele[TraceLayout.ROUTE_DISTANCE];
				e.setBackgroundColor(Legend.COLORS[Legend.COLOR_RI_DISTANCE_BACKGROUND]);
				double distLine = ProjMath.getDistance(center.radlat, center.radlon, dest.lat, dest.lon);
				e.setText(Locale.get("trace.Air")/*Air:*/ + showDistance((int) distLine, DISTANCE_AIR));
			}
			
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_TRAVEL_MODE_IN_MAP)) {
				e = tl.ele[TraceLayout.TRAVEL_MODE];
				e.setText(Configuration.getTravelMode().getName());
			}

			if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_CLOCK_IN_MAP)) {
				e = tl.ele[TraceLayout.CURRENT_TIME]; // e is used *twice* below (also as vRelative)
				if (Configuration.getCfgBitState(Configuration.CFGBIT_GPS_TIME)) {
					if (pos.gpsTimeMillis != 0) {
						e.setText(DateTimeTools.getClock(pos.gpsTimeMillis + Configuration.getTimeDiff()*1000*60, true));
					} else if (Configuration.getCfgBitState(Configuration.CFGBIT_GPS_TIME_FALLBACK)) {
						e.setText(DateTimeTools.getClock(System.currentTimeMillis() + Configuration.getTimeDiff()*1000*60, true));
					} else {
						e.setText(" ");
					}
				} else {
					e.setText(DateTimeTools.getClock(System.currentTimeMillis() + Configuration.getTimeDiff()*1000*60, true));
				}

 				/*
				don't use new Date() - it is very slow on some Nokia devices
				currentTime.setTime( new Date( System.currentTimeMillis() ) );
				e.setText(
					currentTime.get(Calendar.HOUR_OF_DAY) + ":"
					+ HelperRoutines.formatInt2(currentTime.get(Calendar.MINUTE)));
				*/

				// if current time is visible, positioning OFFROUTE above current time will work
				tl.ele[TraceLayout.ROUTE_OFFROUTE].setVRelative(e);

				if (isShowingSplitCMS()) {
					updateCMS(g);
				}
			}
			
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_MAP_CREDITS)) {
				tl.ele[TraceLayout.MAP_INFO].setText(getMapCreditsText());
			}

			setAlertSign(Legend.getNodeTypeDesc((short) alertNodeType));
			
			setSpeedingSign(maxSpeed);
			
			if (hasPointerEvents()) {
				tl.ele[TraceLayout.ZOOM_IN].setText("+");
				tl.ele[TraceLayout.ZOOM_OUT].setText("-");
				tl.ele[TraceLayout.RECENTER_GPS].setText("|");
				e = tl.ele[TraceLayout.SHOW_DEST];
				if (atDest && prevPositionNode != null) {
					e.setText("<");
					e.setActionID(SHOW_PREVIOUS_POSITION_CMD);
				} else {
					e.setText(">");
					e.setActionID(SHOW_DEST_CMD + (Trace.SET_DEST_CMD << 16) );					
				}
				tl.ele[TraceLayout.RECORDINGS].setText("*");
				tl.ele[TraceLayout.SEARCH].setText("_");
			}

			e = tl.ele[TraceLayout.TITLEBAR];
			if (currentTitleMsgOpenCount != 0) {
				e.setText(currentTitleMsg);

				// setTitleMsgTimeOut can be changed in receiveMessage()
				synchronized (this) {
					if (setTitleMsgTimeout != 0) {
						TimerTask timerT = new TimerTask() {
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
						GpsMid.getTimer().schedule(timerT, setTitleMsgTimeout);
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
		if (isShowingSplitTraceIconMenu() && traceIconMenu != null) {
			traceIconMenu.paint(g);
		}
		if (isShowingSplitSearch() && guiSearch != null) {
			guiSearch.paint(g);

		}
		if (isShowingSplitSetup() && guiDiscoverIconMenu != null) {
			guiDiscoverIconMenu.paint(g);
		}
		if (isShowingSplitCMS() && cmsl != null) {
			cmsl.paint(g);
		}
	}

	public String getMapCreditsText() {
		String credits = Locale.get("trace.mapcredit")/*Map:*/;
		boolean added = false;
		if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_OSM_CC_BY_SA)) {
			if (added) {
				credits = credits + ",";
			}
			credits = credits + Locale.get("trace.mapcreditOsmCC");
			added = true;
		}
		if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_OSM_ODBL)) {
			if (added) {
				credits = credits + ",";
			}
			credits = credits + Locale.get("trace.mapcreditOsmODbL");
			added = true;
		}
		//#if polish.api.finland
		if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_FI_LANDSURVEY)) {
			if (added) {
				credits = credits + ",";
			}
			credits = credits + Locale.get("trace.mapcreditFiLandSurvey12");
			added = true;
		}
		if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_FI_DIGIROAD)) {
			if (added) {
				credits = credits + ",";
			}
			credits = credits + Locale.get("trace.mapcreditFiDigiroad");
			added = true;
		}
		//#endif
		return credits;
	}

	public boolean isShowingSplitScreen() {
		return showingTraceIconMenu || showingSplitSearch || showingSplitSetup || showingSplitCMS;
	}

	public static void clearTraceInstance() {
		if (!Legend.isValid) {
			traceInstance = null;
		}
	}

	public boolean isShowingSplitSetup() {
		return showingSplitSetup;
	}

	public boolean isShowingSplitCMS() {
		return showingSplitCMS;
	}

	public void clearShowingSplitSetup() {
		showingSplitSetup = false;
		//restartImageCollector();		
	}

	public void setShowingSplitTraceIconMenu() {
		showingTraceIconMenu = true;
	}

	public boolean isShowingSplitIconMenu() {
		return showingTraceIconMenu || showingSplitSetup;
	}

	public boolean isShowingSplitTraceIconMenu() {
		return showingTraceIconMenu;
	}

	public boolean isShowingSplitSearch() {
		return showingSplitSearch;
	}

	public void showGuiWaypointSave(PositionMark posMark) {
    	if (guiWaypointSave == null) {
    		guiWaypointSave = new GuiWaypointSave(this);
    	}
    	if (guiWaypointSave != null) {
    		guiWaypointSave.setData(posMark);
    		guiWaypointSave.show();
    	}
	}

	/** Show an alert telling the user that waypoints are not ready yet.
	 */
	private void showAlertLoadingWpt() {
		alert("Way points", "Way points are not ready yet.", 3000);
	}
	
	private void showCurrentAlert(Graphics g) {
		Font font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
		// same font for title
		Font titleFont = font;
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
				int alertTop = (mapWindow.getHeight() - alertHeight) /2;
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
				TimerTask timerT = new TimerTask() {
					public synchronized void run() {
						currentAlertsOpenCount--;
						if (currentAlertsOpenCount == 0) {
							//#debug debug
							logger.debug("clearing alert");
							repaint();
						}
					}
				};
				GpsMid.getTimer().schedule(timerT, setAlertTimeout);
				setAlertTimeout = 0;
			}
		}
	}

	private void setSpeedingSign(int maxSpeed) {
		//speeding = true;
		if (Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_VISUAL)
			&&
			(
				speeding
				||
				(System.currentTimeMillis() - startTimeOfSpeedingSign) < 3000
			)
		) {
			//#if polish.api.finland
			String cameraString = "";
			if (cameraAlert && startTimeOfCameraAlert == 0) {
				startTimeOfCameraAlert = System.currentTimeMillis();
				// FIXME get camera sound from config file, requires
				// map format change to pass in legend
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_SND)) {
					GpsMid.mNoiseMaker.immediateSound("CAMERA_ALERT");
				}
			}
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDCAMERA_ALERT)
				&& cameraAlert) {
				cameraString = Locale.get("trace.cameraText");
			}
			//#endif
			if (speeding) {
				speedingSpeedLimit = maxSpeed;
				startTimeOfSpeedingSign = System.currentTimeMillis();
			}
			//#if polish.api.finland
			if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				tl.ele[TraceLayout.SPEEDING_SIGN].setText(cameraString + Integer.toString(speedingSpeedLimit));
			} else {
				tl.ele[TraceLayout.SPEEDING_SIGN].setText(cameraString + Integer.toString((int)(speedingSpeedLimit / 1.609344f + 0.5f)));
			}
			if ((System.currentTimeMillis() - startTimeOfCameraAlert) >= 8000) {
				cameraAlert = false;
				startTimeOfCameraAlert = 0;
			}
			//#else
			if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				tl.ele[TraceLayout.SPEEDING_SIGN].setText(Integer.toString(speedingSpeedLimit));
			} else {
				tl.ele[TraceLayout.SPEEDING_SIGN].setText(Integer.toString((int)(speedingSpeedLimit / 1.609344f + 0.5f)));
			}
			//#endif
		} else {
			startTimeOfSpeedingSign = 0;
			//#if polish.api.finland
			startTimeOfCameraAlert = 0;
			//#endif
		}
	}

	private void setAlertSign(String alert) {
		//FIXME use alert sign visual instead, get from legend after
		// map format change
		if (Configuration.getCfgBitState(Configuration.CFGBIT_NODEALERT_VISUAL)
		    &&
		    (
			    nodeAlert
			    ||
			    (System.currentTimeMillis() - startTimeOfAlertSign) < 8000) 
			) {
			if (nodeAlert && startTimeOfAlertSign == 0) {
				startTimeOfAlertSign = System.currentTimeMillis();
				//FIXME get sound from config file, requires
				// map format change to pass in legend
				if (Configuration.getCfgBitState(Configuration.CFGBIT_NODEALERT_SND)) {
					GpsMid.mNoiseMaker.immediateSound("ALERT");
				}
			}
			tl.ele[TraceLayout.SPEEDING_SIGN].setText(alert.substring(0,7));
			if ((System.currentTimeMillis() - startTimeOfAlertSign) >= 8000) {
				nodeAlert = false;
				startTimeOfAlertSign = 0;
			}
		} else {
			startTimeOfAlertSign = 0;
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
		pc.squareDstWithPenToActualRoutableWay = Float.MAX_VALUE;
		pc.xSize = 100;
		pc.ySize = 100;
		// retry searching an expanding region at the position mark
		Projection p;
		do {
			p = new Proj2D(new Node(pm.lat,pm.lon, true),5000,pc.xSize,pc.ySize);
			pc.setP(p);
			for (int i=0; i<4; i++) {
				if (Legend.tileScaleLevelContainsRoutableWays[i]) {
					tiles[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD | Tile.OPT_FIND_CURRENT);
				}
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
		StringBuffer c = new StringBuffer(5);
		if (ProjFactory.getProj() != ProjFactory.NORTH_UP
				&& Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS)) {
			c.append(Configuration.getCompassDirection(course));
		}
		// if tl shows big onscreen buttons add spaces to short compass directions
		if (tl.bigOnScreenButtons) {
			if (ProjFactory.getProj() == ProjFactory.NORTH_UP) {
				c.setLength(0);
				c.append('(').append(Configuration.getCompassDirection(0)).append(')');
			}
			while (c.length() <= 3) {
				c.insert(0,' ').append(' ');
			}
		}
		tl.ele[TraceLayout.POINT_OF_COMPASS].setText(c.toString());
	}
	
	private int showConnectStatistics(Graphics g, int yc, int la) {
		g.setColor(Legend.COLORS[Legend.COLOR_MAP_TEXT]);
		// only try to show compass id and cell id if user has somehow switched them on
		GsmCell cell = null;
		if (cellIDLocationProducer != null || Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_SECELL || Configuration.getCfgBitState(Configuration.CFGBIT_CELLID_LOGGING)) {
			cell = CellIdProvider.getInstance().obtainCachedCellID();
		}
		Compass compass = null;

		if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION)) {
			compass = GetCompass.getInstance().obtainCachedCompass();
		}

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
		if (compass == null) {
			g.drawString("No compass direction available", 0, yc, Graphics.TOP
					| Graphics.LEFT);
			yc += la;
		} else {
			g.drawString("Compass direction: " + compass.direction, 0, yc, Graphics.TOP
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
		// Avoid exception after route calculation
		if ( pc.getP() == null || imageCollector == null )
			return;
		try {
		if (dest != null) {
			pc.getP().forward(dest.lat, dest.lon, pc.lineP2);
//			System.out.println(dest.toString());
			int x = pc.lineP2.x - imageCollector.xScreenOverscan;
			int y = pc.lineP2.y - imageCollector.yScreenOverscan;
			pc.g.drawImage(pc.images.IMG_DEST, x, y, CENTERPOS);
			pc.g.setColor(Legend.COLORS[Legend.COLOR_DEST_TEXT]);
			if (dest.displayName != null) {
				pc.g.drawString(dest.displayName, x, y+8,
					Graphics.TOP | Graphics.HCENTER);
			}
			pc.g.setStrokeStyle(Graphics.SOLID);
			waySegment.drawWideLineSimple(
				Legend.COLORS[Legend.COLOR_DEST_LINE],
				new IntPoint(pc.getP().getImageCenter().x - imageCollector.xScreenOverscan, pc.getP().getImageCenter().y - imageCollector.yScreenOverscan),
				new IntPoint(x, y),
				Configuration.getDestLineWidth(), pc
			);
			
		}
		} catch (Exception e) {
			if (imageCollector == null) {
				logger.silentexception("No ImageCollector", e);
			}
			e.printStackTrace();
		}
	}


	/**
	 *
	 * Show marker texts, if marker under cursor
	 */
	public void showMarker(Graphics g) {
	}

	/**
	 * Draws the position square, the movement line and the center cross.
	 * 
	 * @param g Graphics context for drawing
	 */
	public void showMovement(Graphics g) {
		// Avoid exception after route calculation
		if ( pc.getP() == null )
			return;
		centerP = null;
		try {
			if (imageCollector != null) {
			g.setColor(Legend.COLORS[Legend.COLOR_MAP_CURSOR]);
			centerP = pc.getP().getImageCenter();
			int centerX = centerP.x - imageCollector.xScreenOverscan;
			int centerY = centerP.y - imageCollector.yScreenOverscan;
			int posX, posY;
			if (!gpsRecenter) {
				IntPoint p1 = new IntPoint(0, 0);
				pc.getP().forward((pos.latitude * MoreMath.FAC_DECTORAD),
								  (pos.longitude * MoreMath.FAC_DECTORAD), p1);
				posX = p1.getX()-imageCollector.xScreenOverscan;
				posY = p1.getY()-imageCollector.yScreenOverscan;
			} else {
				posX = centerX;
				posY = centerY;
			}
			g.setColor(Legend.COLORS[Legend.COLOR_MAP_POSINDICATOR]);
			float radc = course * MoreMath.FAC_DECTORAD;
			
			int px = posX + (int) (Math.sin(radc) * 20);
			int py = posY - (int) (Math.cos(radc) * 20);
			// crosshair center cursor
			if (!gpsRecenter || gpsRecenterInvalid) {
				g.drawLine(centerX, centerY - 12, centerX, centerY + 12);
				g.drawLine(centerX - 12, centerY, centerX + 12, centerY);
				g.drawArc(centerX - 5, centerY - 5, 10, 10, 0, 360);
			}
			if (! gpsRecenterInvalid) {
				// gps position spot
				pc.g.drawImage(gpsRecenterStale ? pc.images.IMG_POS_BG_STALE : pc.images.IMG_POS_BG, posX, posY, CENTERPOS);
				// gps position rectangle
				g.drawRect(posX - 4, posY - 4, 8, 8);
				g.drawLine(posX, posY, px, py);
				// draw accuracy circle for position
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_ACCURACY)
					&& pc.getP().isOrthogonal()) {
					Position pos = getCurrentPosition();
					if (pos.accuracy != Float.NaN && pos.accuracy > 0) {
						int diaM = (int) pos.accuracy * 2;
						int dia = 0;

						Node n = new Node();
						float scale = pc.getP().getScale();
						Projection p = new Proj2D(center,scale, mapWindow.getWidth(), mapWindow.getHeight());
						p.inverse(posX, posY, n);
						n.radlat += MoreMath.RADIANT_PER_METER * diaM;
						IntPoint ip = p.forward(n);
						dia = ip.y - posY;
						pc.g.drawArc(posX - dia / 2, posY - dia / 2, dia, dia, 0, 360);
					}
				}
			}
			}
		} catch (Exception e) {
			if (imageCollector == null) {
				logger.silentexception("No ImageCollector", e);
			}
			if (centerP == null) {
				logger.silentexception("No centerP", e);
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
		g.drawString(Locale.get("trace.Freemem")/*Freemem: */ + runtime.freeMemory(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString(Locale.get("trace.Totmem")/*Totmem: */ + runtime.totalMemory(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString(Locale.get("trace.Percent")/*Percent: */
				+ (100f * runtime.freeMemory() / runtime.totalMemory()), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString(Locale.get("trace.ThreadsRunning")/*Threads running: */
				+ Thread.activeCount(), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString(Locale.get("trace.Names")/*Names: */ + namesThread.getNameCount(), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString(Locale.get("trace.SingleT")/*Single T: */ + tileReader.getLivingTilesCount() + "/"
				+ tileReader.getRequestQueueSize(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString(Locale.get("trace.FileT")/*File T: */ + dictReader.getLivingTilesCount() + "/"
				+ dictReader.getRequestQueueSize() + " Map: " + ImageCollector.icDuration + " ms", 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString(Locale.get("trace.LastMsg")/*LastMsg: */ + lastTitleMsg, 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString( Locale.get("trace.at")/*at */ + lastTitleMsgClock, 0, yc,
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

	public synchronized void receiveCompassStatus(int status) {
	}

	public void deviateCompass() {
		compassDeviated = (compassDirection + compassDeviation + 360) % 360;
	}

	public synchronized void receiveCompass(float direction) {
		//#debug debug
		logger.debug("Got compass reading: " + direction);
		compassDirection = (int) direction;
		deviateCompass();
		// TODO: allow for user to use compass for turning the map in panning mode
		// (gpsRenter test below switchable by user setting)
		//if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION) && compassProducer != null && gpsRecenter) {
		//	if (!Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)) {
		//		updateCourse(compassDeviated);
		//	}
			//repaint();
		//}
		//	course = compassDeviated;
		//}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_ALWAYS_ROTATE)
		    // if user is panning the map, don't rotate by compass
		    && gpsRecenter
		    // if we have autoswitch, rotate by compass only when movement course is not valid
		    && !(Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)
			&& (fspeed >= courseMinSpeed && thirdPrevCourse != -1))) {
			course = compassDeviated;
			updatePosition();
		}
	}

	public static void updateLastUserActionTime() {
		lastUserActionTime = System.currentTimeMillis();
	}
	
	public static long getDurationSinceLastUserActionTime() {
		return System.currentTimeMillis() - lastUserActionTime;
	}
	
	public int getCourse() {
		return course;
	}
	
	public void updateCourse(int newcourse) {
		coursegps = newcourse;
		/*  don't rotate too fast
		 */
		int courseToSet = course;

		if ((newcourse - courseToSet) > 180) {
			courseToSet = courseToSet + 360;
		}
                                                              
		if ((courseToSet - newcourse) > 180) {
			newcourse = newcourse + 360;
		}
                                                 
		if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION) && compassProducer != null) {
			courseToSet = newcourse;
		} else {
			// FIXME I think this is too slow a turn at least when course is
			// of good quality, turning should be faster. This probably alleviates
			// the trouble caused by unreliable gps course. However,
			// some kind of heuristic, averaging course or evaluating the
			// quality of course and fast or instant rotation with a known good GPS fix
		        // should be implemented instead of assuming course is always unreliable.
		        // The course fluctuations are lesser in faster speeds, so if we're constantly
		        // (for three consecutive locations) above say 5 km/h, a reasonable approach
		        // could be to use direct course change in that case, and for speed below
		        // 5 km/h use the slow turning below.
			// jkpj 2010-01-17
		        // on 2011-04-11: jkpj switched from 1/4 rotation back to 3/4 rotation,
		        // returning to what it was supposed to do before 2010-11-30.
			courseToSet = courseToSet + ((newcourse - courseToSet)*3)/4 + 360;
		}
		course = courseToSet % 360;
		validateCourse();
	}

	public boolean isCourseValid() {
		return courseValid;
	}
	private void invalidateCourse() {
		courseValid = false;
	}

	private void validateCourse() {
		courseValid = true;
	}

	public synchronized void receivePosition(Position pos) {
		// FIXME signal on location gained
		//#debug info
		logger.info("New position: " + pos);
		collected++;
		if (Configuration.getAutoRecenterToGpsMilliSecs() !=0 &&
			getDurationSinceLastUserActionTime() > Configuration.getAutoRecenterToGpsMilliSecs()
			&& isShown()
		) {
			gpsRecenter = true;
			//autoZoomed = true;
		}
		if (Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_JSR179) {
			if (pos.type == Position.TYPE_GPS_LASTKNOWN) {
				// if we have a current cell id fix from cellid location,
				// don't overwrite it with a stale GPS location, but ignore the position
				// FIXME perhaps compare timestamps here in case the last known gps is later
				if (this.pos.type == Position.TYPE_CELLID) {
					return;
				}
				gpsRecenterInvalid = false;
				gpsRecenterStale = true;
			}
		}
		this.pos = pos;
		if (pos.type == Position.TYPE_GPS || pos.type == Position.TYPE_CELLID || pos.type == Position.TYPE_MANUAL) {
			gpsRecenterInvalid = false;
			gpsRecenterStale = false;
		}
		if (gpsRecenter) {
			center.setLatLonDeg(pos.latitude, pos.longitude);
			speed = (int) (pos.speed * 3.6f);
			fspeed = pos.speed * 3.6f;
			if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION) && compassProducer != null) {
				deviateCompass();
			}
			// auto-fallback mode where course is from GPS at high speeds and from compass
			// at low speeds
			if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_DIRECTION) && compassProducer != null && !Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)) {
				updateCourse(compassDeviated);
			} else if (fspeed >= courseMinSpeed && pos.course != Float.NaN ) {
                               // Problem: resolve issue erratic course due to GPS fluctuation
                               // when GPS reception is poor (maybe 3..7S),
                               // causes unnecessary and disturbing map rotation when device is in one location
                               // Heuristic for solving: After being still, require
                               // three consecutive over-the-limit speed readings with roughly the
                               // same course
                               if (thirdPrevCourse != -1) {
                                       // first check for normal flow of things, we've had three valid courses after movement start
                                       updateCourse((int) pos.course);
				       thirdPrevCourse = secondPrevCourse;
				       secondPrevCourse = prevCourse;
				       // check for compass deviation auto-update, do it if set
				       if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AUTOCALIBRATE)) {
					       compassDeviation = (int) pos.course - compassDirection;
					       deviateCompass();
				       }
                               } else if (prevCourse == -1) {
				       if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)) {
					       updateCourse(compassDeviated);
				       }                                       // previous course was invalid,
                                       // don't set course yet, but set the first tentatively good course
                                       prevCourse = (int) pos.course;
                               } else if (secondPrevCourse == -1) {
                                       // the previous course was the first good one.
                                       // If this course is in the same 60-degree
                                       // sector as the first course, we have two valid courses
                                       if (Math.abs(prevCourse - (int)pos.course) < 30 || Math.abs(prevCourse - (int)pos.course) > 330) {
                                               secondPrevCourse = prevCourse;
                                       }
				       if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)) {
					       updateCourse(compassDeviated);
				       }
                               } else {
                                       // we have received two previous valid curses, check for this one
                                       if (Math.abs(prevCourse - (int)pos.course) < 30 || Math.abs(prevCourse - (int)pos.course) > 330) {
                                               thirdPrevCourse = secondPrevCourse;
                                               secondPrevCourse = prevCourse;
                                               updateCourse((int) pos.course);
                                       } else {
                                               secondPrevCourse = -1;
					       if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)) {
						       updateCourse(compassDeviated);
					       }
                                       }
                               }
			       prevCourse = (int) pos.course;
			} else {
				// speed under the minimum. If it went under the limit now, do a heuristic
				// to decide a proper course.
				// invalidate all prev courses
				if (thirdPrevCourse != -1) {
					// speed just went under the threshold
					// - if the last readings are not within the 30 degree sector of
					// the previous one, restore course to the third previous course,
					// it's probable that it's more reliable than the last
					// course, as we see that at traffic light stops
					// an incorrect course is shown relatively often
					if ((Math.abs(prevCourse - secondPrevCourse) < 15 || Math.abs(prevCourse - secondPrevCourse) > 345)
					    && (Math.abs(thirdPrevCourse - secondPrevCourse) < 15 || Math.abs(thirdPrevCourse - secondPrevCourse) > 345)) {
						// we're OK
					} else {
						updateCourse(thirdPrevCourse);
					}
				}
				prevCourse = -1;
				secondPrevCourse = -1;
				thirdPrevCourse = -1;
				if (Configuration.getCfgBitState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)) {
					updateCourse(compassDeviated);
				}
			}
		}
		pos.altitude += Configuration.getAltitudeCorrection();
		altitude = (int) (pos.altitude);
		if (gpx.isRecordingTrk()) {
			try {
				// don't tracklog manual cellid position or gps start/stop last known position
				if ((Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_JSR179
				     && pos.type == Position.TYPE_CELLID) || pos.type == Position.TYPE_GPS_LASTKNOWN) {
				} else {
					gpx.addTrkPt(pos);
				}
			} catch (Exception e) {
				receiveMessage(e.getMessage());
			}
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_AUTOZOOM)
				&& gpsRecenter
				&& (isGpsConnected() || TrackPlayer.isPlaying)
				&& autoZoomed
				&& pc.getP() != null
				&& pos.speed != Float.NaN // if speed is unknown do not autozoom
				&& pos.speed != 0 // if speed is 0 do not autozoom
		) {
			// the minimumScale at 20km/h and below is equivalent to having zoomed in manually once from the startup zoom level
			final float minimumScale = Configuration.getRealBaseScale() / Configuration.getZoomFactor();
			final int minimumSpeed = 20;
			// the maximumScale at 160km/h and above is equivalent to having zoomed out manually once from the startup zoom level
			final float maximumScale = Configuration.getRealBaseScale() * Configuration.getZoomFactor();
			final int maximumSpeed = 160;
			int speedForScale = speed;
			float newScale = minimumScale + (maximumScale - minimumScale) * (speedForScale - minimumSpeed) / (maximumSpeed - minimumSpeed);
			// make sure the new scale is within the minimum / maximum scale values
			if (newScale < minimumScale) {
				newScale = minimumScale;
			} else if (newScale > maximumScale) {
				newScale = maximumScale;
			}
			
			// autozoom in more at the last 200 m of the route
			if (route != null && RouteInstructions.getDstRouteToDestination() <= 200) {
				float newScale2 = newScale;
				newScale2 = newScale / (1f + (200f - RouteInstructions.getDstRouteToDestination())/ 200f);
				// fixed increased zoom for the last 100 m
				newScale = Math.max(newScale2, newScale / Configuration.getZoomFactor());
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

	public void receiveSatellites(Satellite[] sats) {
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

	public void pointerPressed(int x, int y) {
		//#if polish.android
		previousBackPress = false;
		//#endif
		pointerDown = true;
	    
		if (coordsForOthers(x, y)) {
			// for icon menu
			if (isShowingSplitTraceIconMenu() && traceIconMenu != null) {
				traceIconMenu.pointerPressed(x, y);
				return;
			}
			if (isShowingSplitSearch() && guiSearch != null) {
				guiSearch.pointerPressed(x, y);
				return;
			}
			if (isShowingSplitSetup() && guiDiscoverIconMenu != null) {
				guiDiscoverIconMenu.pointerPressed(x, y);
				return;
			}
			if (isShowingSplitCMS() && cmsl != null) {
				cmsl.pointerPressed(x, y);
				return;
			}
		}

		updateLastUserActionTime();
		long currTime = System.currentTimeMillis();
		pointerDragged = false;
		pointerDraggedMuch = false;
		pointerActionDone = false;

		// remember center when the pointer was pressed for dragging
		centerPointerPressedN = center.copy();
		if (imageCollector != null) {
			panProjection=imageCollector.getCurrentProjection();
			pickPointStart=panProjection.inverse(x,y, pickPointStart);
		} else {
			panProjection = null;
		}

		// remember the LayoutElement the pointer is pressed down at, this will also highlight it on the display
		int touchedElementId = tl.getElementIdAtPointer(x, y);
		if (touchedElementId >= 0 && (!keyboardLocked || tl.getActionIdAtPointer(x, y) == Trace.ICON_MENU)
				&&
			tl.isAnyActionIdAtPointer(x, y)
		) {
			tl.setTouchedElement((LayoutElement) tl.elementAt(touchedElementId));
			repaint();
		}
		
		// check for double press
		if (!keyboardLocked && currTime - pressedPointerTime < DOUBLETAP_MAXDELAY) {
			doubleTap(x, y);
			return;
		}
		
		// Remember the time and position the pointer was pressed after the check for double tap,
		// so the double tap code will check for the position of the first of the two taps
		pressedPointerTime = currTime;
		Trace.touchX = x;
		Trace.touchY = y;

		// Give a message if keyboard/user interface is locked.
		// This must be done after remembering the touchX/Y positions as they are needed to unlock
		if (keyboardLocked) {
			keyPressed(0);
			return;
		}		
		
//		// when these statements are reached, no double tap action has been executed,
//		// so check here if there's currently already a TimerTask waiting for a single tap.
//		// If yes, perform the current single tap action immediately before starting the next TimerTask
//		if (checkingForSingleTap && !pointerDraggedMuch) {
//			singleTap();
//			pointerActionDone = false;
//		}
//		
		longTapTimerTask = new TimerTask() {
			public void run() {
				// if no action (e.g. from double tap) is already done
				// and the pointer did not move or if it was pressed on a control and not moved much
				if (!pointerActionDone) {
					if (System.currentTimeMillis() - pressedPointerTime >= LONGTAP_DELAY){
						longTap(false);
					}
				}
			}
		};
		try {
			// set timer to continue check if this is a long tap
			GpsMid.getTimer().schedule(longTapTimerTask, LONGTAP_DELAY);
		} catch (Exception e) {
			logger.error(Locale.get("trace.NoLongTapTimerTask")/*No LongTap TimerTask: */ + e.toString());
		}
	}
	
	public void pointerReleased(int x, int y) {	
		pointerDown = false;
		mapBrowsing = false;
		if (coordsForOthers(x, y)) {
			// for icon menu
			if (isShowingSplitTraceIconMenu() && traceIconMenu != null) {
				traceIconMenu.pointerReleased(x, y);
				return;
			}
			if (isShowingSplitSearch() && guiSearch != null) {
				guiSearch.pointerReleased(x, y);
				return;
			}
			if (isShowingSplitSetup() && guiDiscoverIconMenu != null) {
				guiDiscoverIconMenu.pointerReleased(x, y);
				return;
			}
			if (isShowingSplitCMS() && cmsl != null) {
				cmsl.pointerReleased(x, y);
				return;
			}
		}

		// releasing the pointer cancels the check for long tap
		if (longTapTimerTask != null) {
			longTapTimerTask.cancel();
		}
		// releasing the pointer will clear the highlighting of the touched element
		if (tl.getTouchedElement() != null) {
			tl.clearTouchedElement();
			repaint();
		}
		if (pointerDraggedMuch && 
		    Math.abs(x - Trace.touchX) <= DRAGGEDMUCH_THRESHOLD
				&& 
			Math.abs(y - Trace.touchY) <= DRAGGEDMUCH_THRESHOLD
		) {
			// reset draggedmuch if we come back to the original
			// touch place (e.g. finger was moved momentarily away and
			// back due to a bump on the road).
			pointerDraggedMuch = false;
		}
		
		if (!pointerActionDone && !keyboardLocked) {
			touchReleaseX = x;
			touchReleaseY = y;
			if (Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_SINGLE)
			    && (!Configuration.getCfgBitState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS) || getClickableMarker(x, y) == null)
			    && !tl.isAnyActionIdAtPointer(x, y)
			    && !pointerDraggedMuch) {
				highlightOnScreenButtons();
			}
			startDoubleTapTimer(x, y);
		
			//#if not polish.android
			if (pointerDragged) {
				pointerDragged(x , y);
				return;
			}
			//#endif
		}
	}
	
	private void startDoubleTapTimer(int x, int y) {
		// check for a single tap in a timer started after the maximum double tap delay
		// if the timer will not be cancelled by a double tap, the timer will execute the single tap command
		//#if polish.android
		if (doubleTapActive(x, y)) {
			singleTapTimerTask = new TimerTask() {
				public void run() {
					if (!keyboardLocked) {
						singleTap(touchReleaseX, touchReleaseY);
					}
				}
			};
		}
		//#else
		singleTapTimerTask = new TimerTask() {
			public void run() {
				if (!keyboardLocked) {
					singleTap(touchReleaseX, touchReleaseY);
				}
			}
		};
		//#endif
		try {
			// set timer to check if this is a single tap

			// FIXME activate these tests also for J2ME after testing

			//#if polish.android
			if (doubleTapActive(x, y)) {
				GpsMid.getTimer().schedule(singleTapTimerTask, DOUBLETAP_MAXDELAY);
			} else {
				singleTap(touchReleaseX, touchReleaseY);
			}
			//#else
			GpsMid.getTimer().schedule(singleTapTimerTask, DOUBLETAP_MAXDELAY);
			//#endif
		} catch (Exception e) {
			logger.error(Locale.get("trace.NoSingleTapTimerTask")/*No SingleTap TimerTask: */ + e.toString());
		}
	}

	//#if polish.android
	public void mtPointerDragged (float newscale) {
		scale = newscale;
		autoZoomed = false;
		updateLastUserActionTime();
		repaint();
		return;
	}
	public void mtPointerRotated (float newangle) {
		course = (int) newangle;
		updateLastUserActionTime();
		updatePosition();
		repaint();
		return;
	}
	//#endif

	public int angleDiff(int a, int b) {
		int diff = a - b;
		if (diff < 0) {
			diff += 360;
		}
		if (diff > 180) {
			diff = 360 - diff;
		}
		return diff;
	}

	public boolean doubleTapActive(int x, int y) {
		return (Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_DOUBLE)
			&& !tl.isAnyActionIdAtPointer(x, y))
			|| tl.pointerHasDoubleTapAction(x, y);
	}

	public void pointerDragged (int x, int y) {
		if (coordsForOthers(x, y)) {
			// for icon menu
			if (isShowingSplitTraceIconMenu() && traceIconMenu != null) {
				traceIconMenu.pointerDragged(x, y);
				return;
			}
			if (isShowingSplitSearch() && guiSearch != null) {
				guiSearch.pointerDragged(x, y);
				return;
			}
			if (isShowingSplitSetup() && guiDiscoverIconMenu != null) {
				guiDiscoverIconMenu.pointerDragged(x, y);
				return;
			}
			if (isShowingSplitCMS() && cmsl != null) {
				cmsl.pointerDragged(x, y);
				return;
			}
		}

		// check if there's been much movement, do this before the slide lock/unlock
		// to avoid a single tap action when not sliding enough
		if (Math.abs(x - Trace.touchX) > DRAGGEDMUCH_THRESHOLD
				|| 
			Math.abs(y - Trace.touchY) > DRAGGEDMUCH_THRESHOLD
		) {
			pointerDraggedMuch = true;
			// avoid double tap triggering on fast consecutive drag actions starting at almost the same position
			pressedPointerTime = 0; 
			// zero long press timer which will fire otherwise with setting
			// pressedPointerTime to 0; we don't want a pointer dragged much
			// to be interpreted as a long tap
			if (longTapTimerTask != null) {
				longTapTimerTask.cancel();
			}
		}
		
		if (pointerDown && pointerDraggedMuch) {
			mapBrowsing = true;
		}
		updateLastUserActionTime();
		LayoutElement e = tl.getElementAtPointer(x, y);
		if (tl.getTouchedElement() != e) {
			// leaving the touched element cancels the check for long tap
			if (longTapTimerTask != null) {
				longTapTimerTask.cancel();
			}			
			tl.clearTouchedElement();
			repaint();
		}
		// If the initially touched element is reached again during dragging, highlight it 
		if (tl.getElementAtPointer(touchX, touchY) == e && tl.isAnyActionIdAtPointer(x, y)) {
			tl.setTouchedElement(e);
			repaint();
		}
		
		if (pointerActionDone) {
			return;
		}		
		// slide at least 1/4 display width to lock / unlock GpsMid		
		if (tl.getActionIdAtPointer(touchX, touchY) == Trace.ICON_MENU) {
			if ( tl.getActionIdAtPointer(x, y) ==  Trace.ICON_MENU
					&&
				x - touchX > getWidth() / 4
			) {
				commandAction(TOGGLE_KEY_LOCK_CMD);
				pointerActionDone = true;
			}
			return;
		}

		if (keyboardLocked) {
			return;
		}

		pointerDragged = true;
		
				
		// do not start map dragging on a touch control if only dragged slightly
		// on Android, not even when not on a touch control, so
		// pinch zoom won't always turn off follow GPS mode
		if (!pointerDraggedMuch
		    //#if polish.android
		    //#else
		    && tl.getElementIdAtPointer(touchX, touchY) >= 0
		    //#endif
		    ) {
			return;
		}
		
		if (tl.getElementIdAtPointer(touchX, touchY) < 0 && imageCollector != null && panProjection != null) {
			// difference between where the pointer was pressed and is currently dragged
//			int diffX = Trace.touchX - x;
//			int diffY = Trace.touchY - y;
//			
//			IntPoint centerPointerPressedP = new IntPoint();
			pickPointEnd=panProjection.inverse(x,y, pickPointEnd);
			center.radlat=centerPointerPressedN.radlat-(pickPointEnd.radlat-pickPointStart.radlat);
			center.radlon=centerPointerPressedN.radlon-(pickPointEnd.radlon-pickPointStart.radlon);
//			System.out.println("diff " + diffX + "/" + diffY + "  " + (pickPointEnd.radlat-pickPointStart.radlat) + "/" + (pickPointEnd.radlon-pickPointStart.radlon) ); 
			imageCollector.newDataReady();
			gpsRecenter = false;

			long lCurrentTime = System.currentTimeMillis();
			if ( lCurrentTime - lLastDragTime > 333) {
				lLastDragTime = lCurrentTime;
				repaint();
			}
		}
	}
	
	private void singleTap(int x, int y) {
		pointerActionDone = true;
		// if not tapping a control, then the map area must be tapped so we set the touchable button sizes
		if (tl.getElementIdAtPointer(touchX, touchY) < 0) {							
			if (Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_SINGLE)) {
				// if pointer was dragged much, do not recognize a single tap on the map
				if (pointerDraggedMuch) {
					return;
				}
				// #debug debug
				logger.debug("single tap map");
	
				// check for clickable markers
				// System.out.println("Checking for clickable markers");
				boolean markerClicked = false;
				ClickableCoords coords = getClickableMarker(x, y);
				if (coords != null) {
					//if (internetAccessAllowed()) {
					markerClicked = true;
					// TODO maybe later add an
					// option or options to have 
					// marker click directly either
					// * open the URL
					// * edit node in OSM
					// * save a waypoint
					// * log the place to a webservice
					// * something else
					// if (coords.url != null) {
					//	GuiWebInfo.openUrl(coords.url);
					//	return;
					//} else {
					longTap(true);
					//}
					//} else {
					//String text = "";
					//if (coords.url != null) {
					//text += coords.url;
					//}
					//if (coords.phone != null) {
					//if (text.length() != 0) {
					//text += " / ";
					//}
					//text += coords.phone;
					//}
					//alert("Contact", text, 5000);
					//}
				}
				if (!markerClicked && !tl.bigOnScreenButtons) {
					highlightOnScreenButtons();
				}
			}
			repaint();
		} else if (tl.getElementIdAtPointer(x, y) == tl.getElementIdAtPointer(touchX, touchY)) {
			tl.clearTouchedElement();
			int actionId = tl.getActionIdAtPointer(x, y);
			if (actionId > 0) {
				// #debug debug
				logger.debug("single tap button: " + actionId + " x: " + touchX + " y: " + touchY);
				if (System.currentTimeMillis() < (lastBackLightOnTime + 5000)) {
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
				commandAction(actionId);
				repaint();
			}
		}
	}
	
	private void highlightOnScreenButtons() {
		tl.setOnScreenButtonSize(true);
		
		// to enable clickable markers only when single-tapped
		//if (Configuration.getCfgBitState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS)) {
		//	newDataReady();
		//}

		// set timer to continuously check if the last user interaction is old enough to make the buttons small again
		final long BIGBUTTON_DURATION = 5000;
		bigButtonTimerTask = new TimerTask() {
			public void run() {
				if (System.currentTimeMillis() - lastUserActionTime > BIGBUTTON_DURATION ) {
					// make the on screen touch buttons small again
					tl.setOnScreenButtonSize(false);
					requestRedraw();						
					// to enable clickable markers only when single-tapped
					//if (Configuration.getCfgBitState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS)) {
					//	newDataReady();
					//}
					bigButtonTimerTask.cancel();
				}
			}
		};
		try {
			GpsMid.getTimer().schedule(bigButtonTimerTask, BIGBUTTON_DURATION, 500);
		} catch (Exception e) {
			logger.error("Error scheduling bigButtonTimerTask: " + e.toString());
		}
	
	}
	private void doubleTap(int x, int y) {
		// if not double tapping a control, then the map area must be double tapped and we zoom in
		if (tl.getElementIdAtPointer(touchX, touchY) < 0) {
			if (doubleTapActive(touchX, touchY)) {
				// if this is a double press on the map, cancel the timer checking for a single press
				if (singleTapTimerTask != null) {
					singleTapTimerTask.cancel();
				}
				// if pointer was dragged much, do not recognize a double tap on the map
				if (pointerDraggedMuch) {
					return;
				}
				//#debug debug
				logger.debug("double tap map");
				pointerActionDone = true;
				commandAction(ZOOM_IN_CMD);
			}
			repaint();
			return;
		} else if (tl.getTouchedElement() == tl.getElementAtPointer(x, y) ){
		// double tapping a control
			int actionId = tl.getActionIdDoubleAtPointer(x, y);
			//#debug debug
			logger.debug("double tap button: " + actionId + " x: " + x + " y: " + x);
			if (actionId > 0) {
				// if this is a double press on a control, cancel the timer checking for a single press
				if (singleTapTimerTask != null) {
					singleTapTimerTask.cancel();
				}
				pointerActionDone = true;
				commandAction(actionId);
				tl.clearTouchedElement();
				repaint();
				return;
			} else {
				singleTap(x, y);
			}
		}
	}
	
	private ClickableCoords getClickableMarker(int x, int y) {
		System.out.println("Click coords: " + x + " " + y);
		if (clickableMarkers != null) {
			for (int i = 0; i < clickableMarkers.size(); i++) {
				ClickableCoords coords = (ClickableCoords)clickableMarkers.elementAt(i);
				// System.out.println("Marker coords: " + coords.x + " " + coords.y);
				if (Math.abs(coords.x - x) <= Configuration.getTouchMarkerDiameter() / 2
				    && Math.abs(coords.y - y) <= Configuration.getTouchMarkerDiameter() / 2) {
					return coords;
				}
			}
		}
		return null;
	}

	private void longTap(boolean force) {
		// if not tapping a control, then the map area must be tapped so we do the long tap action for the map area
		if (tl.getElementIdAtPointer(touchX, touchY) < 0 && panProjection != null) {
			if (!pointerDraggedMuch && (Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_LONG) || force || getClickableMarker(touchX, touchY) != null)) {
				pointerActionDone = true;
				//#debug debug
				logger.debug("long tap map");										
				// if we clicked a clickable marker, get coords from the marker instead of tap
				ClickableCoords coords = getClickableMarker(touchX, touchY);
				String url = null;
				String phone = null;
				if (coords != null) {
					touchX = coords.x;
					touchY = coords.y;
					url = getUrl(coords.urlIdx);
					phone = getUrl(coords.phoneIdx);
				}
				// long tap map to open a place-related menu
				// use the place of touch instead of old center as position,
				pickPointEnd=panProjection.inverse(touchX + imageCollector.xScreenOverscan,
								   touchY + imageCollector.yScreenOverscan, pickPointEnd);
				Position oPos = new Position(pickPointEnd.radlat, pickPointEnd.radlon,
							     0.0f, 0.0f, 0.0f, 0, 0);
				GuiWebInfo gWeb = new GuiWebInfo(this, oPos, pc, true, coords != null ? url : null,
								 coords != null ? phone : null,
								 coords != null ? coords.nodeID : -1);
				gWeb.show();

			}
			return;
		// long tapping a control											
		} else {
			int actionId = tl.getActionIdLongAtPointer(touchX, touchY);
			if (actionId > 0 && tl.getElementAtPointer(touchX, touchY) == tl.getTouchedElement()) {
				tl.clearTouchedElement();
				repaint();
				pointerActionDone = true;
				//#debug debug
				logger.debug("long tap button: " + actionId + " x: " + touchX + " y: " + touchY);
				commandAction(actionId);
			}
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

			// read saved destination position from Configuration
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SAVED_DESTPOS_VALID)) {
				Node destNode = new Node();
				Configuration.getDestPos(destNode);
				setDestination(new RoutePositionMark(destNode.radlat, destNode.radlon));
			}
			
			if (pc != null) {
				pc.center = center.copy();
				pc.scale = scale;
				pc.course = course;
			}
		}
		updatePosition();
	}

	public void setBaseTilesRead(boolean read) {
		baseTilesRead = read;
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
			// some fault tolerance  - will crash without a map
			if (Legend.isValid) {
				GpsMid.mNoiseMaker.playSound("DISCONNECT");
			}
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_GPS_TIME)) {
			pos.gpsTimeMillis = 0;
		}
		//if (gpx != null) {
		//	/**
		//	 * Close and Save the gpx recording, to ensure we don't loose data
		//	 */
		//	gpx.saveTrk(false);
		//}
		removeCommand(CMDS[DISCONNECT_GPS_CMD]);
		if (locationProducer == null) {
//#debug info
			logger.info("leave locationDecoderEnd no producer");
			return;
		}
		locationProducer = null;
		running = false;
		notify();
		addCommand(CMDS[CONNECT_GPS_CMD]);
//		addCommand(START_RECORD_CMD);
//#debug info
		logger.info("end locationDecoderEnd");
	}

	public void receiveStatus(byte status, int satsReceived) {
		if (status != LocationMsgReceiver.STATUS_ON
		    && status != LocationMsgReceiver.STATUS_2D
		    && status != LocationMsgReceiver.STATUS_3D
		    && status != LocationMsgReceiver.STATUS_DGPS) {
			// no fix, invalidate speed heuristic and GPS time
			prevCourse = -1;
			secondPrevCourse = -1;
			thirdPrevCourse = -1;
			if (Configuration.getCfgBitState(Configuration.CFGBIT_GPS_TIME)) {
				pos.gpsTimeMillis = 0;
			}
		}
		// FIXME signal a sound on location gained or lost
		solution = status;
		solutionStr = LocationMsgReceiverList.getCurrentStatusString(status, satsReceived);
		repaint();
		// to update e.g. tacho
		if (locationUpdateListeners != null && !TrackPlayer.isPlaying) {
			synchronized (locationUpdateListeners) {
				for (int i = 0; i < locationUpdateListeners.size(); i++) {
					((LocationUpdateListener)locationUpdateListeners.elementAt(i)).loctionUpdated();
				}
			}
		}
	}

	public String getName(int idx) {
		if (idx < 0) {
			return null;
		}
		return namesThread.getName(idx);
	}
	
	public String getUrl(int idx) {
		if (idx < 0) {
			return null;
		}
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

	//#if polish.android
	public void activateAndroidListeners() {
		View androidView = (View) CanvasBridge.current();
		androidView.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN
				    || event.getAction() == KeyEvent.ACTION_UP)
				{
					//eat menu code in the map screen so the system won't handle it
					if (keyCode == KeyEvent.KEYCODE_MENU && imageCollector != null && imageCollector.isRunning())
					{
						if (event.getAction() == KeyEvent.ACTION_UP) {
							if (keyboardLocked) {
								// show alert in keypressed() that keyboard is locked
								keyPressed(0);
							} else {
								commandAction(Trace.ICON_MENU);
							}
						}
						// but if icon menu is not enabled, return false so
						// J2MEPolish will handle this and user gets a text
						// menu
						return Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS);
					}
				}
				if (keyCode == KeyEvent.KEYCODE_SEARCH) {
					// #debug debug
					logger.debug("  Turning key into SEARCH_CMD");
					commandAction(Trace.getInstance().getCommand(Trace.SEARCH_CMD), (Displayable) null);
					return true;
				}
				return false;
			}
		});
		androidView.setFocusable(true);
		androidView.setFocusableInTouchMode(true);
		androidView.requestFocus();
		androidView.setOnTouchListener(this);
	}
	//#endif

	public void show() {
		//Display.getDisplay(parent).setCurrent(this);
		Legend.freeDrawnWayAndAreaSearchImages();
		GpsMid.getInstance().show(this);
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		updateLastUserActionTime();
		repaint();
		//#if polish.android
		activateAndroidListeners();
		//#endif
	}
	
	public void recreateTraceLayout() {
		// don't re-half screen size in split-screen mode
		// refreshWindowLayout((mapMaxX - mapMinX), (mapMaxY - mapMinY));
		tl = new TraceLayout(mapWindow);
	}

	public boolean mapLayoutIsPortrait() {
		return mapWindow.isPortrait();
	}		

	public boolean deviceLayoutIsPortrait() {
		return rootWindow.isPortrait();
	}		

	public void resetSize() {
		sizeChanged(getWidth(), getHeight());
	}

	public void locationDecoderEnd(String msg) {
		receiveMessage(msg);
		locationDecoderEnd();
	}

	public PositionMark getDestination() {
		return dest;
	}

	public PositionMark getPosMark() {
		PositionMark posMark = null;
		if (gpsRecenter) {
			// TODO: If we lose the fix the old position and height
			// will be used silently -> we should inform the user
			// here that we have no fix - he may not know what's going on.
			posMark = new PositionMark(center.radlat, center.radlon,
						   (int)pos.altitude, pos.timeMillis,
						   /* fix */ (byte)-1, /* sats */ (byte)-1,
						   /* sym */ (byte)-1, /* type */ (byte)-1);
		} else {
			// Cursor does not point to current position
			// -> it does not make sense to add elevation and GPS fix info.
			posMark = new PositionMark(center.radlat, center.radlon,
						   PositionMark.INVALID_ELEVATION,
						   pos.timeMillis, /* fix */ (byte)-1,
						   /* sats */ (byte)-1, /* sym */ (byte)-1,
						   /* type */ (byte)-1);
		}
		return posMark;
	}
	public void setDestination(RoutePositionMark dest) {
		Routing.dropToConnectionsCache();
		movedAwayFromDest = false;
		endRouting();
		this.dest = dest;
		pc.dest = dest;
		if (dest != null) {
			//#debug info
			logger.info("Setting destination to " + dest.toString());
			
			// move map only to the destination, if GUI is not optimized for routing
			//if (! Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
			//	commandAction(SHOW_DEST_CMD);
			//}
			if (Configuration.getCfgBitState(Configuration.CFGBIT_AUTOSAVE_DESTPOS)) {
				Configuration.setDestPos(new Node(dest.lat, dest.lon, true));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SAVED_DESTPOS_VALID, true);
			}
		} else {
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_SAVED_DESTPOS_VALID, false);
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
		RouteConnectionTraces.clear();
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

			if (routeEngine != null) {
				if (routeEngine.routeStartsAgainstMovingDirection) {
					ri.forceAgainstDirection();
				}
			} else {
				alert("Warning", "routeEngine==null", 3000);
			}
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
			logger.exception(Locale.get("trace.InTraceResumeImageCollector")/*In Trace.resumeImageCollector*/, e);
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

	public void stopShowingSplitScreen() {
		showingSplitSetup = false;
		showingTraceIconMenu = false;
		showingSplitSearch = false;
		showingSplitCMS = false;
		refreshWindowLayout();
	}

	public void showIconMenu() {
		if (traceIconMenu == null) {
			traceIconMenu = new TraceIconMenu(this, this);
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)
		    && hasPointerEvents()) {
			showingTraceIconMenu = true;
			refreshWindowLayout();
			traceIconMenu.sizeChanged(rootWindow.getMaxX(), rootWindow.getMaxY());
		} else {
			traceIconMenu.show();
		}
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
	public void performIconAction(int actionId, String choiceName) {
		System.out.println("choiceName: " + choiceName);
		if (!isShowingSplitScreen()) {
			show();
		}
		updateLastUserActionTime();
		// when we are low on memory or during route calculation do not cache the icon menu (including scaled images)
		if (routeCalc || GpsMid.getInstance().needsFreeingMemory()) {
			//#debug info
			logger.info("low mem: Uncaching traceIconMenu");
			uncacheIconMenu();
		}
		if (actionId == IconActionPerformer.BACK_ACTIONID) {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)) {
				stopShowingSplitScreen();
			}
		}
		if (actionId == SAVE_PREDEF_WAYP_CMD && gpx != null && choiceName != null) {
			if (gpx.isLoadingWaypoints()) {
				showAlertLoadingWpt();
			} else {
				PositionMark posMark = getPosMark();
				if (choiceName.indexOf("%s") != -1) {
					mForm = new GuiWaypointPredefinedForm(this, this);
					mForm.setData(choiceName, choiceName, TextField.ANY, posMark);
					mForm.show();
				} else if (choiceName.indexOf("%f") != -1) {
					mForm = new GuiWaypointPredefinedForm(this, this);
					mForm.setData(choiceName, choiceName, TextField.DECIMAL, posMark);
					mForm.show();
				} else {
					posMark.displayName = choiceName;
				}
				gpx.addWayPt(posMark);
				newDataReady();
			}
			return;
		} else if (actionId == ROUTE_TO_FAVORITE_CMD && gpx != null && choiceName != null) {
			// set destination from choiceName
			choiceName += "*";
			Vector wpt = gpx.listWayPoints();
			PositionMark[] wayPts = new PositionMark[wpt.size()];
			wpt.copyInto(wayPts);
			PositionMark result = null;
			for (int i = 0; i < wayPts.length; i++ ) {
				if (choiceName.equals(wayPts[i].displayName)) {
					result = wayPts[i];
				}
			}
			if (result != null) {
				RoutePositionMark pm1 = new RoutePositionMark(result.lat, result.lon);
				setDestination(pm1);
				commandAction(ROUTING_START_CMD);
			}
		} else if (actionId != IconActionPerformer.BACK_ACTIONID) {
			commandAction(actionId);
		}
	}
	/** convert distance to string based on user preferences */

	// The default way to show a distance is  "10km" for
	// distances 10km or more, "2,6km" for distances under 10, and
	// "600m" for distances under 1km.

	public static String showDistance(int meters) {
		return showDistance(meters, DISTANCE_GENERIC);
	}
	public static String showDistance(int meters, int type) {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
			if (type == DISTANCE_UNKNOWN) {
				return "???" + Locale.get("guitacho.m");
			}
			int MajorUnit = meters / 1000;
			int MinorUnit = meters % 1000;
			// km.mm
			//String MinorShort = (MinorUnit / 10 < 10 ? "0" : "") + (MinorUnit / 10);
			// km.m
			String MinorShort = Integer.toString((int)MinorUnit / 100);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_DISTANCE_VIEW) && (type != DISTANCE_ALTITUDE)) {
				//if (MajorUnit >= 10) {
				//	return Integer.toString(MajorUnit) + Locale.get("guitacho.km");
				//} else 
				if (MajorUnit == 0) {
					return Integer.toString(MinorUnit) + Locale.get("guitacho.m");
				} else {
					// FIXME use e.g. getDecimalSeparator() for decimal comma/point selection
					return Integer.toString(MajorUnit) + "." + MinorShort + Locale.get("guitacho.km");
				}
			} else {
				return Integer.toString(meters) + Locale.get("guitacho.m");
			}
		} else {
			if (type == DISTANCE_UNKNOWN) {
				return "???" + Locale.get("guitacho.yd");
			}
			int MajorUnit = (int)(meters / 1609.344f);
			int MinorUnit = (int)(meters % 1609.344f + 0.5f);
			// mi.dd
			//String MinorShort = (MinorUnit / 16.09344f < 10.0f ? "0" : "") + (int)(MinorUnit / 16.09344f);
			// mi.d
			String MinorShort = Integer.toString((int)(MinorUnit / 160.9344f));
			if (Configuration.getCfgBitState(Configuration.CFGBIT_DISTANCE_VIEW) && (type != DISTANCE_ALTITUDE)) {
				//if (MajorUnit >= 10) {
				//	return Integer.toString(MajorUnit) + Locale.get("guitacho.mi");
				//} else
				if (MajorUnit == 0) {
					return Integer.toString(MinorUnit) + Locale.get("guitacho.yd");
				} else {
					return Integer.toString(MajorUnit) + "." + MinorShort + Locale.get("guitacho.mi");
				}
			} else {
				return Integer.toString((int)(meters / 0.9144 + 0.5f)) + Locale.get("guitacho.yd");
			}
		}
	}
	// FIXME: get and remember coordinates to keep track of distance
	// to alert POI. Handle multiple alert POIs.
	public void setNodeAlert(int type) {
		if (!nodeAlert) {
			nodeAlert = true;
			alertNodeType = type;
		}
	}
	//#if polish.api.finland
	public void setCameraAlert(int type) {
		if (!cameraAlert) {
			cameraAlert = true;
		}
	}
	//#endif
	public void resetClickableMarkers() {
		clickableMarkers = new Vector();
	}
	
	//#if polish.android
	// FIXME should rather start a background service for recording track or
	// other fuctionality which requires staying active
	public void getPersistent() {
		//if (Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WAKELOCK)
		//    && Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON)) {
		//	// wakelock already active
		//	return;
		//}
		if (pm == null) {
			pm = (PowerManager) MidletBridge.instance.getSystemService(Context.POWER_SERVICE);
		}
		if (wl == null) {
			wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "GpsMidRecord");
			wl.acquire();
		}
	}
	public void leavePersistent() {
		if (wl != null) {
			wl.release();
			wl = null;
		}
	}
	//#endif

    public void addClickableMarker(int x, int y, int urlIdx, int phoneIdx, int nodeID) {
		ClickableCoords coords = new ClickableCoords();
		coords.x = x - imageCollector.xScreenOverscan;
		coords.y = y - imageCollector.yScreenOverscan;
		coords.urlIdx = urlIdx;
		coords.phoneIdx = phoneIdx;
		coords.nodeID = nodeID;
		clickableMarkers.addElement(coords);
	}
    
    public boolean internetAccessAllowed() {
	    if (Configuration.getCfgBitState(Configuration.CFGBIT_INTERNET_ACCESS)) {
		    return true;
	    }
	    GpsMid.getInstance().alert("GpsMid", Locale.get("trace.OnlineFeaturesDisabledIn") + " " + Locale.get("traceiconmenu.Setup") + " / " + Locale.get("guidiscovericonmenu.Online"), 5000); // Online features are disabled in Setup / Online 
	    return false;
    }
    
}
