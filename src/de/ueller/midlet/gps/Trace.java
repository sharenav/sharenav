package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;
import de.ueller.gps.nmea.NmeaInput;
import de.ueller.gps.sirf.SirfInput;
import de.ueller.midlet.gps.data.Mercator;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.tile.DictReader;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.Names;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.QueueDataReader;
import de.ueller.midlet.gps.tile.QueueDictReader;
import de.ueller.midlet.gps.tile.QueueReader;
import de.ueller.midlet.gps.tile.Tile;

public class Trace extends Canvas implements CommandListener, LocationMsgReceiver,
		Runnable , de.ueller.midlet.gps.Displayable{
	/** Soft button for exiting the demo. */
	private final Command EXIT_CMD = new Command("Back", Command.BACK, 2);

	private final Command REFRESH_CMD = new Command("Refresh", Command.OK, 1);

	private final Command CONNECT_GPS_CMD = new Command("Start gps",
			Command.SCREEN, 1);
	private final Command DISCONNECT_GPS_CMD = new Command("Stop gps",
			Command.SCREEN, 1);

	private InputStream inputStream;

	
	private SirfInput si;
	private NmeaInput ni;

	private String solution = "No";

	private Position pos = new Position(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1,
			new Date());

	Node center = new Node(49.328010f, 11.352556f);

	Projection projection;

	private final GpsMid parent;

	private String lastMsg;

	private long collected = 0;

	PaintContext pc;

	public float scale = 15000f;

	int showAddons = 0;

	Tile t[] = new Tile[4];

	// String[] wayNames=null;

	StreamConnection conn;

	// private DataReader data;
	private final static Logger logger = Logger.getInstance(Trace.class,
			Logger.INFO);


	public static final String statMsg[] = { "no Start1:", "no Start2:",
			"to long  :", "interrupt:", "checksum :", "no End1  :",
			"no End2  :" };

	private byte qualtity;

	private int[] statRecord;

	private long sirfcount;

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


	private JSR179Input input;

	private final Configuration config;

	public Trace(GpsMid parent, Configuration config) throws Exception {
		this.config = config;
		logger.info("init Trace Class");
		this.parent = parent;
		addCommand(EXIT_CMD);
		addCommand(CONNECT_GPS_CMD);

//		setTitle("Trace");
		setCommandListener(this);
		show();
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
			satelit = Image.createImage("/images/satelit.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// start the LocationProvider in background
	public void run() {
		System.out.println(config.getLocationProvider());
		System.out.println(config.getBtUrl());
		System.out.println(config.getRender());
		switch (config.getLocationProvider()){
		case Configuration.LOCATIONPROVIDER_SIRF:
			if (config.getBtUrl() != null){
				try {
					startGpsSirf(config.getBtUrl());
				} catch (Exception e) {
				}
			}
			break;
		case Configuration.LOCATIONPROVIDER_NMEA:
			
			if (config.getBtUrl() != null){
				try {
					startGpsNmea(config.getBtUrl());
				} catch (Exception e) {
				}
			}
			break;
		case Configuration.LOCATIONPROVIDER_JSR179:
			try {
				input = new JSR179Input(this);
			} catch (Exception e) {
//				setTitle("nl" + e.getMessage());
			}
			break;
		}
//		setTitle("lp="+config.getLocationProvider() + " " + config.getBtUrl());
	}
	
	public void pause(){
		switch (config.getLocationProvider()){
		case 0:sirfDecoderEnd();
		break;
		case 1:
			nemaDecoderEnd();
			break;
		case 2:
			if (input != null){
				
			}
		}
	}
	public void resume(){
		Thread thread = new Thread(this);
		thread.start();
	}

	public void startGpsSirf(String url) {
		if (si != null){
			return;
		}
		try {
			conn = (StreamConnection) Connector.open(url);
			inputStream = conn.openInputStream();
			
			si = new SirfInput(inputStream, this);
			addCommand(DISCONNECT_GPS_CMD);
			// logger.debug("messagereader Started");
		} catch (Exception e) {
//			addCommand(CONNECT_GPS_CMD);
			si=null;
			conn=null;
			inputStream=null;
		}
		repaint(0, 0, getWidth(), getHeight());

	}
	public void startGpsNmea(String url) {
		if (ni != null){
			return;
		}
		try {
			conn = (StreamConnection) Connector.open(url);
			inputStream = conn.openInputStream();
			
			ni = new NmeaInput(inputStream, this);
			addCommand(DISCONNECT_GPS_CMD);
			// logger.debug("messagereader Started");
		} catch (Exception e) {
//			addCommand(CONNECT_GPS_CMD);
			ni=null;
			conn=null;
			inputStream=null;
		}
		repaint(0, 0, getWidth(), getHeight());

	}

	public void commandAction(Command c, Displayable d) {
		try {
			if (c == EXIT_CMD) {
				// shutdown();
				parent.show();
				pause();
				return;
			}
			if (c == REFRESH_CMD) {
				repaint(0, 0, getWidth(), getHeight());
			}
			if (c == CONNECT_GPS_CMD){
				if (si == null){
//					removeCommand(CONNECT_GPS_CMD);
					
				}
			}
			if (c == DISCONNECT_GPS_CMD){
				si.close();
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void startup() throws Exception {
//		logger.info("reading Data ...");
		namesThread = new Names();
		new DictReader(this);
		Thread thread = new Thread(this);
		thread.start();
//		logger.info("Create queueDataReader");
		tileReader = new QueueDataReader(this);
//		logger.info("create imageCollector");
		dictReader = new QueueDictReader();
		Images i = new Images();
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
		if (si != null){
			si.close();
		}
		if (ni != null){
			ni.close();
		}
	}


	protected void paint(Graphics g) {
		
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
			if (si != null){
				g.drawString(solution, getWidth() - 1, 1, Graphics.TOP
							| Graphics.RIGHT);
			} else {
				g.drawString("off", getWidth() - 1, 1, Graphics.TOP
						| Graphics.RIGHT);
				
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("continue ..");
		}
	}

	public void cleanup() {
		namesThread.cleanup();
		tileReader.incUnusedCounter();
		dictReader.incUnusedCounter();
	}

	private int showConnectStatistics(Graphics g, int yc, int la) {
		if (statRecord == null) {
			return yc;
		}
		g.setColor(0, 0, 0);
		for (byte i = 0; i < LocationMsgReceiver.SIRF_FAIL_COUNT; i++) {
			g.drawString(statMsg[i] + statRecord[i], 0, yc, Graphics.TOP
					| Graphics.LEFT);
			yc += la;
		}
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
		lastMsg = new String(s);
		repaint(0, 0, getWidth(), getHeight());

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

	public void sirfDecoderEnd() {
//		removeCommand(DISCONNECT_GPS_CMD);
//		addCommand(CONNECT_GPS_CMD);
		if (si == null){
			return;
		}
		si.close();
		si = null;
		try {
			inputStream.close();
			conn.close();
		} catch (IOException e) {
		}
		conn=null;
		inputStream=null;
//		addCommand(CONNECT_GPS_CMD);
//		repaint(0, 0, getWidth(), getHeight());
	}
	public void nemaDecoderEnd() {
		if (ni == null){
			return;
		}
		ni.close();
		ni = null;
		try {
			inputStream.close();
			conn.close();
		} catch (IOException e) {
		}
		conn=null;
		inputStream=null;
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
}
