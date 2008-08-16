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
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotOpenException;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;

import de.ueller.gps.nmea.NmeaInput;
import de.ueller.gps.sirf.SirfInput;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.gpsMid.mapData.DictReader;
import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueDictReader;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.MapName;
import de.ueller.midlet.gps.data.Proj2D;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Projection;
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
public class Trace extends Canvas implements CommandListener, LocationMsgReceiver,
		Runnable , GpsMidDisplayable{
	/** Soft button for exiting the map screen */
	private final Command EXIT_CMD = new Command("Back", Command.BACK, 5);

	private final Command REFRESH_CMD = new Command("Refresh", Command.ITEM, 4);
	private final Command SEARCH_CMD = new Command("Search", Command.OK, 1);

	private final Command CONNECT_GPS_CMD = new Command("Start gps",Command.ITEM, 2);
	private final Command DISCONNECT_GPS_CMD = new Command("Stop gps",Command.ITEM, 2);
	private final Command START_RECORD_CMD = new Command("Start record",Command.ITEM, 4);
	private final Command STOP_RECORD_CMD = new Command("Stop record",Command.ITEM, 4);
	private final Command MANAGE_TRACKS_CMD = new Command("Manage tracks",Command.ITEM, 5);
	private final Command SAVE_WAYP_CMD = new Command("Save waypoint",Command.ITEM, 7);
	private final Command MAN_WAYP_CMD = new Command("Manage waypoints",Command.ITEM, 7);
	private final Command ROUTE_TO_CMD = new Command("Route",Command.ITEM, 3);
	private final Command CAMERA_CMD = new Command("Camera",Command.ITEM, 9);
	private final Command CLEARTARGET_CMD = new Command("Clear Target",Command.ITEM, 10);
	private final Command SETTARGET_CMD = new Command("As Target",Command.ITEM, 11);
	private final Command MAPFEATURES_CMD = new Command("Map Features",Command.ITEM, 12);


	private InputStream btGpsInputStream;
	private OutputStream btGpsOutputStream;
	private StreamConnection conn;

	
//	private SirfInput si;
	private LocationMsgProducer locationProducer;

	private String solution = "NoFix";
	
	private boolean gpsRecenter = true;
	
	private Position pos = new Position(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1,
			new Date());

	Node center = new Node(49.328010f, 11.352556f);

	Projection projection;

	private final GpsMid parent;

	private String lastMsg;
	private Calendar lastMsgTime = Calendar.getInstance();

	private long collected = 0;

	public PaintContext pc;

	public float scale = 15000f;
	
	int showAddons = 0;
	private int fontHeight = 0;
	
	private static long pressedKeyTime = 0;
	private static int pressedKeyCode = 0;
	private static int ignoreKeyCode = 0;
	
	private boolean rootCalc=false;
	Tile t[] = new Tile[6];
	PositionMark source;

	// this is only for visual debugging of the routing engine
	Vector routeNodes=new Vector(); 

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
	private int speed;

	/**
	 * Current course from GPS in compass degrees, 0..359.  
	 */
	private int course;


	private Names namesThread;

	private ImageCollector imageCollector;

	private QueueDataReader tileReader;

	private QueueDictReader dictReader;

	private Runtime runtime = Runtime.getRuntime();

	private PositionMark target;
	private Vector route=null;
	private final Configuration config;
	private Vector recordMark=null;

	private boolean running=false;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;

	public Gpx gpx;
	
	private static Trace traceInstance=null;

	private Routing	routeEngine;

	private byte arrow;
	private Image scaledPict = null;

	private int iPassedRouteArrow=0;
	private static Font routeFont;
	private static int routeFontHeight;
	private static final String[] directions  = { "mark",
		"hard right", "right", "half right",
		"straight on",
		"half left", "left", "hard left", "Target reached"};
	private static final String[] soundDirections  = { "",
		"HARD;RIGHT", "RIGHT", "HALF;RIGHT",
		"STRAIGHTON",
		"HALF;LEFT", "LEFT", "HARD;LEFT"};
	private boolean keyboardLocked=false;
	private boolean movedAwayFromTarget=true;
	private boolean atTarget=false;
	
	
	public Trace(GpsMid parent, Configuration config) throws Exception {
		//#debug
		System.out.println("init Trace");
		this.config = config;
		
		this.parent = parent;
		addCommand(EXIT_CMD);
		addCommand(SEARCH_CMD);
		addCommand(CONNECT_GPS_CMD);
		addCommand(ROUTE_TO_CMD);
		addCommand(START_RECORD_CMD);
		addCommand(MANAGE_TRACKS_CMD);		           
		addCommand(SAVE_WAYP_CMD);
		addCommand(MAN_WAYP_CMD);
		//#if polish.api.mmapi && polish.api.advancedmultimedia
		addCommand(CAMERA_CMD);
		//#endif
		addCommand(CLEARTARGET_CMD);
		addCommand(SETTARGET_CMD);
		addCommand(MAPFEATURES_CMD);
		setCommandListener(this);
		
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
			if (config.getLocationProvider() == Configuration.LOCATIONPROVIDER_NONE){
				receiveMessage("No location provider");
				return;
			}
			running=true;
			receiveMessage("Connect to "+Configuration.LOCATIONPROVIDER[config.getLocationProvider()]);
			//		System.out.println(config.getBtUrl());
			//		System.out.println(config.getRender());
			switch (config.getLocationProvider()){
				case Configuration.LOCATIONPROVIDER_SIRF:
				case Configuration.LOCATIONPROVIDER_NMEA:
					//#debug debug
					logger.debug("Connect to "+config.getBtUrl());
					if (! openBtConnection(config.getBtUrl())){
						running=false;
						return;
					}
			}
			receiveMessage("BT Connected");
			if (config.getCfgBitState(config.CFGBIT_SND_CONNECT)) {
				parent.mNoiseMaker.playSound("CONNECT");
			}
			//#debug debug
			logger.debug("rm connect, add disconnect");
			removeCommand(CONNECT_GPS_CMD);
			addCommand(DISCONNECT_GPS_CMD);
			switch (config.getLocationProvider()){
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

			String url = config.getGpsRawLoggerUrl();
			//logger.error("Raw logging url: " + url);
			if (config.getGpsRawLoggerEnable() && (url != null)) {
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
			//		setTitle("lp="+config.getLocationProvider() + " " + config.getBtUrl());			
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
			if (getConfig().getBtKeepAlive()) {
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
	 * re initialise the location producer with the new
	 * streams.
	 * 
	 * @return weather the reconnect was successful
	 */
	public boolean autoReconnectBtConnection() {
		if (!getConfig().getBtAutoRecon()) {
			logger.info("Not trying to reconnect");
			return false;
		}
		/**
		 * If there are still parts of the old connection
		 * left over, close these cleanly.
		 */
		closeBtConnection();
		int reconnectFailures = 0;
		while ((reconnectFailures < 4) && (! openBtConnection(config.getBtUrl()))){
			reconnectFailures++;
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				return false;
			}
		}
		if (reconnectFailures < 4) {
			if (locationProducer != null) {
				locationProducer.init(btGpsInputStream, btGpsOutputStream, this);
				return true;
			}
		}
		return false;
	}

	public void commandAction(Command c, Displayable d) {
		try {
			if(keyboardLocked) {
				// show alert in keypressed() that keyboard is locked
				keyPressed(0);
				return;
			}
			if (c == EXIT_CMD) {
				// FIXME: This is a workaround. It would be better if recording would not be stopped when returning to map
				if (gpx.isRecordingTrk()) {
					parent.getInstance().alert("Record Mode", "Please stop recording before returning to the main screen." , 2000);
					return;
				}
				
				if (locationProducer != null){
					locationProducer.close();
				}
				if (imageCollector != null) {
					imageCollector.suspend();
				}
				
				// shutdown();
				pause();
				parent.show();
				return;
			}
			if (! rootCalc){
			if (c == START_RECORD_CMD){
				try {
					gpx.newTrk();
					removeCommand(START_RECORD_CMD);
					addCommand(STOP_RECORD_CMD);
				} catch (RuntimeException e) {
					receiveMessage(e.getMessage());
				}
				
			}
			if (c == STOP_RECORD_CMD){
				gpx.saveTrk();
				removeCommand(STOP_RECORD_CMD);
				addCommand(START_RECORD_CMD);
				addCommand(MANAGE_TRACKS_CMD);
			}
			if (c == MANAGE_TRACKS_CMD){
				if (gpx.isRecordingTrk()) {
					parent.getInstance().alert("Record Mode", "You need to stop recording before managing tracks." , 2000);
					return;
				}

//				if (gpx.isRecordingTrk()) {
//			    	gpx.saveTrk();
//					removeCommand(STOP_RECORD_CMD);
//					addCommand(START_RECORD_CMD);			    	
//			    }
				if (imageCollector != null) { 
                    imageCollector.suspend(); 
                } 
			    GuiGpx gpx = new GuiGpx(this);
			    gpx.show();
			}
			if (c == REFRESH_CMD) {
				repaint(0, 0, getWidth(), getHeight());
			}
			if (c == CONNECT_GPS_CMD){
				if (locationProducer == null){
					Thread thread = new Thread(this);
					thread.start();
				}
			}
			if (c == SEARCH_CMD){				
				if (imageCollector != null) {
					imageCollector.suspend();
				}
				GuiSearch search = new GuiSearch(this);
				search.show();
			}
			if (c == DISCONNECT_GPS_CMD){
				pause();
			}
			if (c == ROUTE_TO_CMD){
				if (config.isStopAllWhileRouteing()){
  				   stopImageCollector();
				}
				System.out.println("Routing source: " + source);
				routeNodes=new Vector();
				routeEngine = new Routing(t,this);
				routeEngine.solve(source, pc.target);
//				resume();
			}
			if (c == SAVE_WAYP_CMD) {				
				GuiWaypointSave gwps = new GuiWaypointSave(this,new PositionMark(center.radlat, center.radlon));
				gwps.show();
			}
			if (c == MAN_WAYP_CMD) {				
				GuiWaypoint gwp = new GuiWaypoint(this);
				gwp.show();
			}
			if (c == MAPFEATURES_CMD) {				
				GuiMapFeatures gmf = new GuiMapFeatures(this);
				gmf.show();
				repaint(0, 0, getWidth(), getHeight());
			}
			//#if polish.api.mmapi && polish.api.advancedmultimedia
			if (c == CAMERA_CMD){				
				if (imageCollector != null) {
					imageCollector.suspend();
				}
				try {
					Class GuiCameraClass = Class.forName("de.ueller.midlet.gps.GuiCamera");
					Object GuiCameraObject = GuiCameraClass.newInstance();					
					GuiCameraInterface cam = (GuiCameraInterface)GuiCameraObject;
					cam.init(this, getConfig());
					cam.show();
				} catch (ClassNotFoundException cnfe) {
					logger.exception("Your phone does not support the necessary JSRs to use the camera", cnfe);
				}
				
			}
			//#endif
			if (c == CLEARTARGET_CMD) {				
				setTarget(null);
				setRoute(null);
				setRouteNodes(null);				
			}
			if (c == SETTARGET_CMD) {				
				if (source != null) {
					setTarget(source);
				}
			}
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
		Images i;
		i = new Images();
		pc = new PaintContext(this, i);
		pc.c = new C();
		imageCollector = new ImageCollector(t, this.getWidth(), this.getHeight(), this,
				i, pc.c);
		projection = new Proj2D(center, scale, getWidth(), getHeight());
		pc.setP(projection);
		pc.center = center.clone();
		pc.scale = scale;
		pc.xSize = this.getWidth();
		pc.ySize = this.getHeight();
	}
	private void stopImageCollector(){
		cleanup();
		imageCollector.stop();
		imageCollector=null;
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
			if (w > imageCollector.xSize || h > imageCollector.ySize) {
				System.out.println(pc.xSize + " | " + pc.ySize);
				stopImageCollector();
				try {
					startImageCollector();
					imageCollector.resume();
					imageCollector.newDataReady();
				} catch (Exception e) {
					logger.exception("Could not reinitialise Image Collector after size change", e);
				}
			}
			/**
			 * Recalculate the projection, as it may depends on the size of the screen
			 */
			updatePosition();
		}
	}


	protected void paint(Graphics g) {
		if (lastMsg != null && getTitle() != null) {
			if (System.currentTimeMillis() 
					> (lastMsgTime.getTime().getTime() + 5000))
			{
				/**
				 * Be careful with calling setTitle in paint.
				 * setTitle may cause a repaint, which would
				 * cause the painter thread to spinn slowing
				 * everything else down 
				 */
				setTitle(null);
			}
		}
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

			}
			showMovement(g);
			g.setColor(0, 0, 0);
			if (locationProducer != null){
				if (gpx.isRecordingTrk()) {// we are recording tracklogs
					if(fontHeight==0) {
						fontHeight=g.getFont().getHeight();
					}
					g.setColor(255, 0, 0);
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			System.out.println("continue ..");
		}
	}

	/**
	 * 
	 */
	private void getPC() {
			pc.xSize = this.getWidth();
			pc.ySize = this.getHeight();
			pc.setP( projection);
			projection.inverse(pc.xSize, 0, pc.screenRU);
			projection.inverse(0, pc.ySize, pc.screenLD);
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
		pc.screenLD=nld;
		pc.screenRU=nru;
		pc.target=pm;
		for (int i=0; i<4; i++){
			t[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD);
		}
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
				if (movedAwayFromTarget && config.getCfgBitState(config.CFGBIT_SND_TARGETREACHED)) {
					parent.mNoiseMaker.playSound("TARGET_REACHED", (byte) 7);
				}
			} else {
				movedAwayFromTarget=true;
			}
			showRoute(pc);
		}
		if (recordMark != null){
			PositionMark pm;
			pc.g.setStrokeStyle(Graphics.SOLID);
			pc.g.setColor(255, 100, 100);
			for (int i=0; i<recordMark.size();i++){
				pm = (PositionMark) recordMark.elementAt(i);
				if (pm.lat < pc.screenLD.radlat) {
					continue;
				}
				if (pm.lon < pc.screenLD.radlon) {
					continue;
				}
				if (pm.lat > pc.screenRU.radlat) {
					continue;
				}
				if (pm.lon > pc.screenRU.radlon) {
					continue;
				}

				pc.getP().forward(pm.lat, pm.lon, pc.lineP2);
				pc.g.drawImage(pc.images.IMG_MARK,pc.lineP2.x,pc.lineP2.y,CENTERPOS);				
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
		byte soundRepeatDelay=3;
		float nearestLat=0.0f;
		float nearestLon=0.0f;
		

		// this makes the distance when prepare-sound is played depending on the speed
		int PREPAREDISTANCE=75;
		if (speed>150) {
			PREPAREDISTANCE=1000;							
		} else if (speed>120) {
			PREPAREDISTANCE=700;							
		} else if (speed>100) {
			PREPAREDISTANCE=500;							
		} else if (speed>80) {
			PREPAREDISTANCE=300;							
		} else if (speed>70) {
			PREPAREDISTANCE=250;							
		} else if (speed>50) {
			PREPAREDISTANCE=150;							
		} else if (speed>40) {
			PREPAREDISTANCE=125;							
		} else if (speed>25) {
			PREPAREDISTANCE=100;							
		}
		
		ConnectionWithNode c;
		// Show helper nodes for Routing
		for (int x=0; x<routeNodes.size();x++){
			RouteHelper n=(RouteHelper) routeNodes.elementAt(x);
			pc.getP().forward(n.node.radlat, n.node.radlon, pc.lineP2);
			pc.g.drawRect(pc.lineP2.x-5, pc.lineP2.y-5, 10, 10);
			pc.g.drawString(n.name, pc.lineP2.x+7, pc.lineP2.y+5, Graphics.BOTTOM | Graphics.LEFT);
		}
		if (route != null && route.size() > 0){
			RouteNode lastTo;

			// find nearest routing arrow (to center of screen)
			int iNearest=0;
			if (config.getCfgBitState(config.CFGBIT_ROUTING_HELP)) {
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
					iPassedRouteArrow=iNearest;
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
			byte lastEndBearing=c.endBearing;			
			lastTo=c.to;
			byte a=0;
			byte aNearest=0;
			for (int i=1; i<route.size();i++){
				c = (ConnectionWithNode) route.elementAt(i);
				if (c == null){
					System.out.println("show Route got null connection");
				}
				if (c.to == null){
					System.out.println("show Route got connection with NULL as target");
				}
				if (lastTo == null){
					System.out.println("show Route strange lastTo is null");
				}
				if (pc == null){
					System.out.println("show Route strange pc is null");
				}
				if (pc.screenLD == null){
					System.out.println("show Route strange pc.screenLD is null");
				}

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
				
				if(i!=iNearest) {
					if (lastTo.lat < pc.screenLD.radlat) {
						lastEndBearing=c.endBearing;
						lastTo=c.to;
						continue;
					}
					if (lastTo.lon < pc.screenLD.radlon) {
						lastEndBearing=c.endBearing;
						lastTo=c.to;
						continue;
					}
					if (lastTo.lat > pc.screenRU.radlat) {
						lastEndBearing=c.endBearing;
						lastTo=c.to;
						continue;
					}
					if (lastTo.lon > pc.screenRU.radlon) {
						lastEndBearing=c.endBearing;
						lastTo=c.to;
						continue;
					}
				}

				Image pict = pc.images.IMG_MARK; a=0;
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
			    	Font originalFont = pc.g.getFont();
			    	if (routeFont==null) {
						routeFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
						routeFontHeight=routeFont.getHeight();
					}
			    	pc.g.setFont(routeFont);
			    	pc.g.setColor(230,230,230);
					pc.g.fillRect(0,pc.ySize-imageCollector.statusFontHeight-routeFontHeight, pc.xSize, routeFontHeight);
					pc.g.setColor(0,0,0);
					double distance=ProjMath.getDistance(center.radlat, center.radlon, lastTo.lat, lastTo.lon);
					int intDistance=new Double(distance).intValue();
					pc.g.drawString(directions[a]
					                      +
					                      ((intDistance<PASSINGDISTANCE)?"":" in " + intDistance + "m"),
					                      pc.xSize/2,pc.ySize-imageCollector.statusFontHeight, Graphics.HCENTER | Graphics.BOTTOM
                    );
					if(!atTarget && intDistance<PASSINGDISTANCE) {
						soundToPlay.append (soundDirections[a]);							
					}
					if(intDistance>=PASSINGDISTANCE && intDistance<=PREPAREDISTANCE) {
						soundToPlay.append ("PREPARE;" + soundDirections[a]);
						soundRepeatDelay=5;
					}
					pc.g.setFont(originalFont);
					if (a!=arrow) {
						arrow=a;
						scaledPict=doubleImage(pict);
					}
					pict=scaledPict;
			    }
				if (i == iNearest + 1) {
					double distance=ProjMath.getDistance(nearestLat, nearestLon, lastTo.lat, lastTo.lon);
					// if there is a close direction arrow after the current one
					// inform the user about its direction
					if (distance <= PASSINGDISTANCE*3 &&
						// only if not both arrows are STRAIGHT_ON
						!(a==4 && aNearest == 4) ) {
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
				pc.g.drawImage(pict,pc.lineP2.x,pc.lineP2.y,CENTERPOS);
				lastEndBearing=c.endBearing;
				lastTo=c.to;
			}
		}
		if (soundToPlay.length()!=0 && config.getCfgBitState(config.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
			parent.mNoiseMaker.playSound(soundToPlay.toString(), (byte) soundRepeatDelay);			
		}
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
			projection = new Proj2D(center, scale, getWidth(), getHeight());
			pc.setP(projection);
			pc.center = center.clone();
			pc.scale = scale;
			repaint(0, 0, getWidth(), getHeight());
		}
	}
	
	public synchronized void receivePosItion(float lat, float lon, float scale) {
		logger.debug("Now displaying: " + (lat*MoreMath.FAC_RADTODEC) + "|" + (lon*MoreMath.FAC_RADTODEC));
		center.setLatLon(lat, lon,true);
		this.scale = scale;
		updatePosition();
	}

	public synchronized void receivePosItion(Position pos) {
		logger.info("New position: " + pos);
		this.pos = pos;
		collected++;
		if (gpsRecenter) {
			center.setLatLon(pos.latitude, pos.longitude);
		}		
		speed = (int) (pos.speed * 3.6f);		
		if (gpx.isRecordingTrk()){
			try {
				gpx.addTrkPt(pos);				
			} catch (Exception e) {
				receiveMessage(e.getMessage());
			} 
		}
		course = (int) pos.course;
		updatePosition();		
	}

	public synchronized void receiveMessage(String s) {
		lastMsg = s;
		//#debug
		parent.log(s);
		setTitle(lastMsg);
		lastMsgTime.setTime( new Date( System.currentTimeMillis() ) );
	}

	public void receiveStatelit(Satelit[] sat) {
		this.sat = sat;

	}

	public MIDlet getParent() {
		return parent;
	}

	protected void keyRepeated(int keyCode) {
	// strange seem to be working in emulator only with this debug line
		logger.debug("keyRepeated " + keyCode);
		//Scrolling should work with repeated keys the same
		//as pressing the key multiple times
		if (keyCode==ignoreKeyCode) {
			logger.debug("key ignored " + keyCode);
			return;
		}
		int gameActionCode = this.getGameAction(keyCode);
		if ((gameActionCode == UP) || (gameActionCode == DOWN) ||
				(gameActionCode == RIGHT) || (gameActionCode == LEFT)) {
			keyPressed(keyCode);
			return;
		}
		if ((keyCode == KEY_NUM2) || (keyCode == KEY_NUM8)
				|| (keyCode == KEY_NUM4) || (keyCode == KEY_NUM6)) {
			keyPressed(keyCode);
			return;
		}

		long keyTime=System.currentTimeMillis();
		// other key is held down
		if ( (keyTime-pressedKeyTime)>=1000 &&
			 pressedKeyCode==keyCode)
		{
			if(keyCode == KEY_NUM5) {
				ignoreKeyCode=keyCode;
				commandAction(SAVE_WAYP_CMD,(Displayable) null);
				return;
			}
			if(keyCode == KEY_NUM9) {
				ignoreKeyCode=keyCode;
				keyboardLocked=!keyboardLocked;
				if(keyboardLocked) {
					// show alert that keys are locked
					keyPressed(0);
				} else {
					parent.getInstance().alert("GpsMid", "Keys unlocked",750);					
				}
			}	
			if(keyCode == KEY_STAR) {
				ignoreKeyCode=keyCode;
				commandAction(MAN_WAYP_CMD,(Displayable) null);
				return;
			}		
			if(keyCode == KEY_POUND) {
				ignoreKeyCode=keyCode;
				commandAction(MANAGE_TRACKS_CMD,(Displayable) null);
				return;
			}
			if(keyCode == KEY_NUM0) {
				ignoreKeyCode=keyCode;				
				if ( gpx.isRecordingTrk() ) {
					GpsMid.getInstance().alert("Gps track recording", "Stopping to record" , 750);					
					commandAction(STOP_RECORD_CMD,(Displayable) null);
				} else {
					GpsMid.getInstance().alert("Gps track recording", "Starting to record" , 750);
					commandAction(START_RECORD_CMD,(Displayable) null);
				}
				return;
			}		
		}	
	}

	
//	// manage keys that would have different meanings when
//	// held down in keyReleased
	protected void keyReleased(int keyCode) {
		// if key was not handled as held down key
	// strange seem to be working in emulator only with this debug line
		logger.debug("keyReleased " + keyCode);
		if (keyCode == ignoreKeyCode) {
			ignoreKeyCode=0;
			return;
		}
		// handle this key normally (shortly pressed)
		if (keyCode == KEY_NUM5) {
			gpsRecenter = true;
		} else if (keyCode == KEY_NUM9) {
			if(keyboardLocked) {
				keyPressed(0);
				return;
			}
			course += 5;
		} else if (keyCode == KEY_STAR) {
			commandAction(MAPFEATURES_CMD,(Displayable) null);
			return;
		} else if (keyCode == KEY_POUND) {
			// toggle Backlight
			config.setCfgBitState(config.CFGBIT_BACKLIGHT_ON,
								!(config.getCfgBitState(config.CFGBIT_BACKLIGHT_ON)),
								false);			
			if ( config.getCfgBitState(config.CFGBIT_BACKLIGHT_ON, false) ) {
				GpsMid.getInstance().alert("Backlight", "Backlight ON" , 750);
				
			} else {
				GpsMid.getInstance().alert("Backlight", "Backlight off" , 750);				
			}			
			parent.stopBackLightTimer();
			parent.startBackLightTimer();
		} else if (keyCode == KEY_NUM0) {
			boolean fullScreen = !config.getCfgBitState(config.CFGBIT_FULLSCREEN);
			config.setCfgBitState(config.CFGBIT_FULLSCREEN, fullScreen, false);
			setFullScreenMode(fullScreen);			
		}
		repaint(0, 0, getWidth(), getHeight());	
	}
	
	protected void keyPressed(int keyCode) {
//		logger.debug("keyPressed " + keyCode);		
		ignoreKeyCode=0;
		pressedKeyCode=keyCode;
		pressedKeyTime=System.currentTimeMillis();
		if(keyboardLocked && keyCode!=KEY_NUM9) {
			parent.getInstance().alert("GpsMid", "Keys locked. Hold down '9' to unlock.",1500);
			ignoreKeyCode=keyCode;
			return;
		}
	
		float f = 0.00003f / 15000f;
		int keyStatus;		
		if (this.getGameAction(keyCode) == UP) {			
			center.radlat += f * scale * 0.1f;
			gpsRecenter = false;
		} else if (this.getGameAction(keyCode) == DOWN) {			
			center.radlat -= f * scale * 0.1f;
			gpsRecenter = false;
		} else if (this.getGameAction(keyCode) == LEFT) {			
			center.radlon -= f * scale * 0.1f;
			gpsRecenter = false;
		} else if (this.getGameAction(keyCode) == RIGHT) {			
			center.radlon += f * scale * 0.1f;
			gpsRecenter = false;
		}				
		if (keyCode == KEY_NUM2) {			
			center.radlat += f * scale;
		} else if (keyCode == KEY_NUM8) {
			center.radlat -= f * scale;
		} else if (keyCode == KEY_NUM4) {
			center.radlon -= f * scale;
		} else if (keyCode == KEY_NUM6) {
			center.radlon += f * scale;
		} else if (keyCode == KEY_NUM1) {
			scale = scale * 1.5f;
		} else if (keyCode == KEY_NUM3) {
			scale = scale / 1.5f;
		} else if (keyCode == KEY_NUM7) {
			showAddons++;
		/** Non standard Key: hopefully is mapped to
		 * the delete / clear key. According to
		 * www.j2meforums.com/wiki/index.php/Canvas_Keycodes
		 * most major mobiles that have this key map to -8
		 **/
		} else if (keyCode == -8) {
			   commandAction(ROUTE_TO_CMD,(Displayable) null);
			   return;   		
		//#if polish.api.mmapi && polish.api.advancedmultimedia
		} else if (keyCode == Configuration.KEYCODE_CAMERA_COVER_OPEN) {
			if (imageCollector != null) {
				imageCollector.suspend();
			}
			try {
				Class GuiCameraClass = Class.forName("de.ueller.midlet.gps.GuiCamera");
				Object GuiCameraObject = GuiCameraClass.newInstance();					
				GuiCameraInterface cam = (GuiCameraInterface)GuiCameraObject;
				cam.init(this, getConfig());
				cam.show();
			} catch (ClassNotFoundException cnfe) {
				logger.exception("Your phone does not support the necessary JSRs to use the camera", cnfe);
			} catch (InstantiationException e) {
				logger.exception("Your phone does not support the necessary JSRs to use the camera", e);
			} catch (IllegalAccessException e) {
				logger.exception("Your phone does not support the necessary JSRs to use the camera", e);
			}
		//#endif
 
		} else {		
			keyStatus = keyCode;
		}
		projection = new Proj2D(center, scale, getWidth(), getHeight());
		pc.setP(projection);
		pc.center = center.clone();
		pc.scale = scale;
		repaint(0, 0, getWidth(), getHeight());
	}

	public Tile getDict(byte zl) {
		return t[zl];
	}
	
	public void setDict(Tile dict, byte zl) {		
		t[zl] = dict;
		// Tile.trace=this;
		addCommand(REFRESH_CMD);
//		if (zl == 3) {
//			setTitle(null);
//		} else {
//			setTitle("dict " + zl + "ready");
//		}
		if (zl == 0) {
			// read saved position from config
			config.getStartupPos(center);
			if(center.radlat==0.0f && center.radlon==0.0f) {
				// if no saved position use center of map
				dict.getCenter(center);
			}

			projection = new Proj2D(center, scale, getWidth(), getHeight());
			if (pc != null) {				
				pc.setP(projection);
				pc.center = center.clone();
				pc.scale = scale;
			}
		}
		updatePosition();
	}

	public void receiveStatistics(int[] statRecord, byte quality) {
		this.btquality = quality;
		this.statRecord = statRecord;
		repaint(0, 0, getWidth(), getHeight());
	}

	
	public synchronized void locationDecoderEnd() {
//#debug info
		logger.info("enter locationDecoderEnd");
		if (config.getCfgBitState(config.CFGBIT_SND_DISCONNECT)) {
			parent.mNoiseMaker.playSound("DISCONNECT");			
		}
		if (gpx != null) {
			/**
			 * Close and Save the gpx recording, to ensure we don't loose data
			 */
			gpx.saveTrk();
			
			removeCommand(STOP_RECORD_CMD);
			removeCommand(START_RECORD_CMD);
		}
		removeCommand(DISCONNECT_GPS_CMD);
		if (locationProducer == null){
//#debug info
			logger.info("leave locationDecoderEnd no producer");
			return;
		}
		locationProducer = null;
		closeBtConnection();
		notify();		
		addCommand(CONNECT_GPS_CMD);
		addCommand(START_RECORD_CMD);
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

	public void requestRedraw() {
		repaint(0, 0, getWidth(), getHeight());
	}

	public void newDataReady() {
		if (imageCollector != null)
			imageCollector.newDataReady();
	}

	public void show() {
		//Display.getDisplay(parent).setCurrent(this);
		GpsMid.getInstance().show(this);
		setFullScreenMode(config.getCfgBitState(config.CFGBIT_FULLSCREEN));
		if (imageCollector != null) {
			imageCollector.resume();
			imageCollector.newDataReady();			
		}
		requestRedraw();
	}

	public Configuration getConfig() {
		return config;
	}

	public void locationDecoderEnd(String msg) {
		receiveMessage(msg);
		locationDecoderEnd();
	}

	public PositionMark getTarget() {
		return target;
	}

	public void setTarget(PositionMark target) {
		this.target = target;
		pc.target = target;
		if(target!=null) {
			center.setLatLon(target.lat, target.lon,true);
			projection = new Proj2D(center, scale, getWidth(), getHeight());
			pc.setP( projection);
			pc.center = center.clone();
			pc.scale = scale;
		}
		repaint(0, 0, getWidth(), getHeight());
		movedAwayFromTarget=false;
	}

	/**
	 * This is the callback routine if RouteCalculation is ready
	 * @param route
	 */
	public void setRoute(Vector route) {
		this.route = route;
		rootCalc=false;
		routeEngine=null;
		try {
			if (config.isStopAllWhileRouteing()){
				startImageCollector();
				// imageCollector thread starts up suspended,
				// so we need to resume it
				imageCollector.resume();
			} else {
//				resume();
			}
			iPassedRouteArrow=0;
			repaint(0, 0, getWidth(), getHeight());
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
	}
	
	protected void showNotify() {
		logger.info("Show notify has been called, screen will be updated again");
	}

}
