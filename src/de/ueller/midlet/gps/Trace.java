package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;


import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;

import de.enough.polish.util.DrawUtil;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;
//#if polish.api.locationapi
import de.ueller.gps.jsr179.JSR179Input;
//#endif

import de.ueller.gps.nmea.NmeaInput;
import de.ueller.gps.sirf.SirfInput;
import de.ueller.midlet.gps.data.Mercator;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.names.Names;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.DictReader;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.QueueDataReader;
import de.ueller.midlet.gps.tile.QueueDictReader;
import de.ueller.midlet.gps.tile.QueueReader;
import de.ueller.midlet.gps.tile.Tile;
import de.ueller.midlet.gps.GpsMidDisplayable;

public class Trace extends Canvas implements CommandListener, LocationMsgReceiver,
		Runnable , GpsMidDisplayable{
	/** Soft button for exiting the demo. */
	private final Command EXIT_CMD = new Command("Back", Command.ITEM, 5);

	private final Command REFRESH_CMD = new Command("Refresh", Command.ITEM, 4);
	private final Command SEARCH_CMD = new Command("Search", Command.ITEM, 1);

	private final Command CONNECT_GPS_CMD = new Command("Start gps",Command.ITEM, 2);
	private final Command DISCONNECT_GPS_CMD = new Command("Stop gps",Command.ITEM, 2);

	private InputStream inputStream;
	private StreamConnection conn;

	
//	private SirfInput si;
	private LocationMsgProducer locationProducer;

	private String solution = "No";

	private Position pos = new Position(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1,
			new Date());

	Node center = new Node(49.328010f, 11.352556f);

	Projection projection;

	private final GpsMid parent;

	private String lastMsg;
	private long lastMsgTime=0;

	private long collected = 0;

	PaintContext pc;

	public float scale = 15000f;

	int showAddons = 0;

	Tile t[] = new Tile[4];


	// private DataReader data;
//#debug error
	private final static Logger logger = Logger.getInstance(Trace.class,Logger.DEBUG);

//#mdebug info
	public static final String statMsg[] = { "no Start1:", "no Start2:",
			"to long  :", "interrupt:", "checksum :", "no End1  :",
			"no End2  :" };
//#enddebug
	private byte qualtity;

	private int[] statRecord;

	private Satelit[] sat;

	private Image satelit, car72;

	private int speed;

	private int course;


	private Names namesThread;

	// private VisibleCollector vc;
	private ImageCollector vc;

	private QueueDataReader tileReader;

	private QueueReader dictReader;

	private Runtime runtime = Runtime.getRuntime();

	private PositionMark target;
	private final Configuration config;

	private boolean running=false;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;

	public Trace(GpsMid parent, Configuration config) throws Exception {
		//#debug
		System.out.println("init Trace");
		this.config = config;

		this.parent = parent;
		addCommand(EXIT_CMD);
		addCommand(SEARCH_CMD);
		addCommand(CONNECT_GPS_CMD);


		setCommandListener(this);

		try {
			startup();
		} catch (Exception e) {
			e.printStackTrace();
			Alert alert = new Alert("Error:" + e.getMessage());
			Display.getDisplay(parent).setCurrent(alert, this);
			parent.show();
		}
		// setTitle("initTrace ready");
		try {
			satelit = Image.createImage("/satelit.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// start the LocationProvider in background
	public void run() {
		if (running){
			//#debug error
			logger.error("thread already running");
			return;
		}
		running=true;
		//#debug info
		logger.info("start thread init locationprovider");
		if (locationProducer != null){
			receiveMessage("locationprovider already running");
			return;
		}
		if (config.getLocationProvider() == Configuration.LOCATIONPROVIDER_NONE){
			receiveMessage("no Location Provider");
			return;
		}
		receiveMessage("connect to "+Configuration.LOCATIONPROVIDER[config.getLocationProvider()]);
//		System.out.println(config.getBtUrl());
//		System.out.println(config.getRender());
		switch (config.getLocationProvider()){
		case Configuration.LOCATIONPROVIDER_SIRF:
			
		case Configuration.LOCATIONPROVIDER_NMEA:
			//#debug debug
			logger.debug("connect to "+config.getBtUrl());
			if (! openBtConnection(config.getBtUrl()))
					return;
		}
		receiveMessage("connected");
		//#debug debug
		logger.debug("rm connect, add disconnect");
		removeCommand(CONNECT_GPS_CMD);
		addCommand(DISCONNECT_GPS_CMD);
		switch (config.getLocationProvider()){
		case Configuration.LOCATIONPROVIDER_SIRF:
			locationProducer = new SirfInput(inputStream, this);
			break;
		case Configuration.LOCATIONPROVIDER_NMEA:
			locationProducer = new NmeaInput(inputStream, this);
			break;
			//#if polish.api.locationapi
		case Configuration.LOCATIONPROVIDER_JSR179:
//			locationProducer = new 
			locationProducer = new JSR179Input(this);
			break;
			//#endif
		}
		//#debug info
		logger.info("end startLocationPovider thread");
//		setTitle("lp="+config.getLocationProvider() + " " + config.getBtUrl());
		running=false;
	}
	
	public synchronized void pause(){
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
		Thread thread = new Thread(this);
		thread.start();
	}


	private boolean openBtConnection(String url){
		if (inputStream != null){
			return true;
		}
		if (url == null)
			return false;
		try {
			conn = (StreamConnection) Connector.open(url);
			inputStream = conn.openInputStream();
		} catch (IOException e) {
			receiveMessage("err BT:"+e.getMessage());
			return false;
		}
		return true;
	}

	public void commandAction(Command c, Displayable d) {
		try {
			if (c == EXIT_CMD) {
				// shutdown();
				pause();
				parent.show();
				return;
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
				if (locationProducer != null){
					locationProducer.close();
				}
				GuiSearch search = new GuiSearch(this);
				search.show();
			}
			if (c == DISCONNECT_GPS_CMD){
				pause();
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
		dictReader = new QueueDictReader();
		Images i;
		i = new Images();
		vc = new ImageCollector(t, this.getWidth(), this.getHeight(), this,
				tileReader, dictReader,i);
//		logger.info("create PaintContext");
		pc = new PaintContext(this, tileReader, dictReader,i);
//		logger.info("init Projection");
		projection = new Mercator(center, pc.scale, getWidth(), getHeight());
//		logger.info("set Center");
		pc.center = center;
	}

	public void shutdown() {
		try {
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
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


	protected void paint(Graphics g) {
		if (lastMsg != null){
			if (System.currentTimeMillis() > lastMsgTime){
				lastMsg=null;
				setTitle(null);
			}
		}
		try {
			int yc = 1;
			int la = 18;
//		if (si != null ){
//			try {
//				outputStream.flush();
//			} catch (IOException e) {
//				si.close();
//			}
//		}
			pc.xSize = this.getWidth();
			pc.ySize = this.getHeight();
			pc.setP( projection);
			projection.inverse(pc.xSize, 0, pc.screenRU);
			projection.inverse(0, pc.ySize, pc.screenLD);
			pc.target=target;
			pc.g = g;
			// cleans the screen
			g.setColor(155, 255, 155);
			g.fillRect(0, 0, pc.xSize, pc.ySize);
			if (vc != null)
				vc.paint(pc);
			switch (showAddons) {
			case 1:
				yc = showConnectStatistics(g, yc, la);
				break;
			case 2:
				showSatelite(g);
				break;
			case 3:
				yc = showMemory(g, yc, la);
				yc = showSpeed(g, yc, la);
				break;
			case 4:
				showAddons = 0;

			}
			showMovement(g);
			g.setColor(0, 0, 0);
			if (locationProducer != null){
				g.drawString(solution, getWidth() - 1, 1, Graphics.TOP
							| Graphics.RIGHT);
			} else {
				g.drawString("off", getWidth() - 1, 1, Graphics.TOP
						| Graphics.RIGHT);
				
			}
			showTarget(pc);
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
//			System.out.println("continue ..");
		}
	}

	public void cleanup() {
		namesThread.cleanup();
		tileReader.incUnusedCounter();
		dictReader.incUnusedCounter();
	}

	private int showConnectStatistics(Graphics g, int yc, int la) {
		if (statRecord == null) {
			g.drawString("no stats jet", 0, yc, Graphics.TOP
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
		g.drawString("Qual : " + qualtity, 0, yc, Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("count : " + collected, 0, yc, Graphics.TOP
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
			if (s.id != 0) {
				double el = s.elev / 180d * Math.PI;
				double az = s.azimut / 180 * Math.PI;
				double sr = r * Math.cos(el);
				g.setColor(s.signal[0] * 5, 0, 0);
				int px = centerX + (int) (Math.sin(az) * sr);
				int py = centerY - (int) (Math.cos(az) * sr);
				// g.drawString(""+s.id, px, py,
				// Graphics.BASELINE|Graphics.HCENTER);
				g.drawImage(satelit, px, py, Graphics.HCENTER
						| Graphics.VCENTER);
				py += 9;
				// draw a bar under image tha indicates green/red status and
				// signal strength

			}
		}
		// g.drawImage(satelit, 5, 5, 0);
	}

	public void showTarget(PaintContext pc){
		if (target == null){
			return;
		}
		pc.getP().forward(target.lat, target.lon, pc.lineP2,true);
//		System.out.println(target.toString());
		pc.g.drawImage(pc.images.IMG_TARGET,pc.lineP2.x,pc.lineP2.y,CENTERPOS);
		pc.g.setColor(0,0,0);
		pc.g.drawString(target.displayName, pc.lineP2.x, pc.lineP2.y+8,
				Graphics.TOP | Graphics.HCENTER);
		pc.g.setColor(255,50,50);
		pc.g.setStrokeStyle(Graphics.DOTTED);
		pc.g.drawLine(pc.lineP2.x,pc.lineP2.y,pc.xSize/2,pc.ySize/2);

	}
	public void showMovement(Graphics g) {
		g.setColor(0, 0, 0);
		int centerX = getWidth() / 2;
		int centerY = getHeight() / 2;
		float radc = (float) (course * Math.PI / 180d);
		int px = centerX + (int) (Math.sin(radc) * 20);
		int py = centerY - (int) (Math.cos(radc) * 20);
		g.drawRect(centerX - 2, centerY - 2, 4, 4);
		g.drawLine(centerX, centerY, px, py);

	}

	public int showMemory(Graphics g, int yc, int la) {
		g.setColor(0, 0, 0);
		g.drawString("freemem : " + runtime.freeMemory(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("totmem  : " + runtime.totalMemory(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("Percent : "
				+ (100f * runtime.freeMemory() / runtime.totalMemory()), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("Names   : " + namesThread.getNameCount(), 0, yc,
				Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("Single T: " + tileReader.getLivingTilesCount() + "/"
				+ tileReader.getRequestQueueSize(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("File T  : " + dictReader.getLivingTilesCount() + "/"
				+ dictReader.getRequestQueueSize(), 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		g.drawString("d:" + lastMsg, 0, yc, Graphics.TOP
				| Graphics.LEFT);
		yc += la;
		return (yc);

	}

	public int showSpeed(Graphics g, int yc, int la) {
		g.setColor(0, 0, 0);
		g.drawString("speed : " + speed, 0, yc, Graphics.TOP | Graphics.LEFT);
		yc += la;
		g.drawString("course  : " + course, 0, yc, Graphics.TOP
						| Graphics.LEFT);
		yc += la;
		return yc;

	}

	public synchronized void receivePosItion(float lat, float lon) {
		center.setLatLon(lat, lon,true);
		projection = new Mercator(center, scale, getWidth(), getHeight());
		pc.setP( projection);
		pc.center = center.clone();
		pc.scale = scale;
		repaint(0, 0, getWidth(), getHeight());
	}

	public synchronized void receivePosItion(Position pos) {
		this.pos = pos;
		collected++;
		center.setLatLon(pos.latitude, pos.longitude);
		projection = new Mercator(center, scale, getWidth(), getHeight());
		pc.setP( projection);
		pc.center = center.clone();
		pc.scale = scale;
		speed = (int) (pos.speed * 3.6f);
		if (speed > 1){
			course = (int) pos.course;
		}
		repaint(0, 0, getWidth(), getHeight());
	}

	public synchronized void receiveMessage(String s) {
		lastMsg = s;
		//#debug
		parent.log(s);
		setTitle(lastMsg);
		lastMsgTime=System.currentTimeMillis()+5000;
	}

	public void receiveStatelit(Satelit[] sat) {
		this.sat = sat;

	}

	public MIDlet getParent() {
		return parent;
	}

	protected void keyPressed(int keyCode) {
		float f = 0.00003f / 15000f;
		int keyStatus;
		if (this.getGameAction(keyCode) == UP) {
			keyStatus = KEY_NUM2;
			center.radlat += f * scale;
		} else if (this.getGameAction(keyCode) == DOWN) {
			keyStatus = KEY_NUM8;
			center.radlat -= f * scale;
		} else if (this.getGameAction(keyCode) == LEFT) {
			keyStatus = KEY_NUM4;
			center.radlon -= f * scale;
		} else if (this.getGameAction(keyCode) == RIGHT) {
			keyStatus = KEY_NUM6;
			center.radlon += f * scale;
		} else if (keyCode == KEY_NUM1) {
			scale = scale * 1.5f;
		} else if (keyCode == KEY_NUM3) {
			scale = scale / 1.5f;
		} else if (keyCode == KEY_NUM7) {
			showAddons++;
		} else if (keyCode == KEY_NUM9) {
			course += 5;
			;
		} else {
			keyStatus = keyCode;
		}
		projection = new Mercator(center, scale, getWidth(), getHeight());
		pc.setP(projection);
		pc.center = center.clone();
		pc.scale = scale;
		repaint(0, 0, getWidth(), getHeight());
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
			dict.getCenter(center);
			projection = new Mercator(center, scale, getWidth(), getHeight());
		}
		repaint(0, 0, getWidth(), getHeight());
	}

	public void receiveStatistics(int[] statRecord, byte qualtity) {
		this.qualtity = qualtity;
		this.statRecord = statRecord;
		repaint(0, 0, getWidth(), getHeight());
	}

	
	public synchronized void locationDecoderEnd() {
//#debug info
		logger.info("enter locationDecoderEnd");
		removeCommand(DISCONNECT_GPS_CMD);
		if (locationProducer == null){
//#debug info
			logger.info("leave locationDecoderEnd no producer");
			return;
		}
		locationProducer = null;
		if (inputStream != null){
			try {
				inputStream.close();
			} catch (IOException e) {
			}
			inputStream=null;
		}
		if (conn != null){
			try {
				conn.close();
			} catch (IOException e) {
			}
			conn=null;
			inputStream=null;
		}
		notify();		
		addCommand(CONNECT_GPS_CMD);
//#debug info
		logger.info("end locationDecoderEnd");
	}

	public void receiveSolution(String s) {
		solution = s;

	}

	public String getName(Short idx) {
		if (idx == null)
			return null;
		return namesThread.getName(idx);
	}

	public void requestRedraw() {
		repaint(0, 0, getWidth(), getHeight());
	}

	public void newDataReady() {
		vc.newDataReady();
	}

	public void show() {
		Display.getDisplay(parent).setCurrent(this);
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
		center.setLatLon(target.lat, target.lon,true);
		projection = new Mercator(center, scale, getWidth(), getHeight());
		pc.setP( projection);
		pc.center = center.clone();
		pc.scale = scale;
		repaint(0, 0, getWidth(), getHeight());

	}
}
