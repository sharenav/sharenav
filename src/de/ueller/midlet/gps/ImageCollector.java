package de.ueller.midlet.gps;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjFactory;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.PaintContext;


/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 * 
 * this class collects all visible object to a off line image for later painting .
 * Run in a low priority to avoid interrupting GUI.
 */


//import de.ueller.midlet.gps.Logger;

public class ImageCollector implements Runnable {
	private final static Logger logger=Logger.getInstance(ImageCollector.class,Logger.TRACE);

	private final static byte STATE_WAIT_FOR_SC = 0;
	private final static byte STATE_SC_READY = 1;
	

	//private boolean lockg=false;
	//private boolean lockc=false;
	private volatile boolean newPaintAvail=false;
	private volatile boolean shutdown=false;
	private volatile boolean suspended=true;
	private final Tile t[];
	private Thread processorThread;
	private ScreenContext nextSc=new ScreenContext() ;

	private Image[] img=new Image[2];
	private PaintContext[] pc=new PaintContext[2];
	public static Node mapCenter = new Node();
	byte nextCreate=0;
	byte nextPaint=0;

	volatile byte stat=0;
	int xSize;
	int ySize;
	IntPoint newCenter=new IntPoint(0,0);
	IntPoint oldCenter=new IntPoint(0,0);
	float oldCourse;
	private volatile boolean needRedraw=false;
	int createImageCount=0;
	private final Trace tr;
	public int statusFontHeight=0;
	
	public ImageCollector(Tile[] t,int x,int y,Trace tr, Images i, C c) {
		super();
		this.t=t;
		this.tr = tr;
		xSize=x;
		ySize=y;
		img[0]=Image.createImage(xSize,ySize);
		img[1]=Image.createImage(xSize,ySize);
		try {
			Node n=new Node(2f,0f);
			pc[0]=new PaintContext(tr, i);
			pc[0].c = c;
			pc[0].setP(ProjFactory.getInstance(n, 0, 1500, xSize, ySize));
			pc[1]=new PaintContext(tr, i);
			pc[1].c = c;
			pc[1].setP(ProjFactory.getInstance(n, 0, 1500, xSize, ySize));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		processorThread = new Thread(this,"ImageCollector");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}

	public void run() {
		final byte MAXCRASHES = 5;
		byte crash=0;
		do {
		try {
			// logger.info("wait for sc");
			while (stat == STATE_WAIT_FOR_SC && !shutdown) {
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}
			}
			while (!shutdown) {
				synchronized (this) {
					try {
						wait(10000);
					} catch (InterruptedException e) {
						continue; // Recheck condition of the loop
					}
				}
				// logger.info("loop");
				while (pc[nextCreate].state != PaintContext.STATE_READY && !shutdown) {
					synchronized (this) {
						try {
							// System.out.println("img not ready");
							wait();
						} catch (InterruptedException e) {
						}
					}
				}
				if (suspended || shutdown)
					continue;
				pc[nextCreate].state = PaintContext.STATE_IN_CREATE;
				
				//#debug
				long startTime = System.currentTimeMillis();

				// create PaintContext
				pc[nextCreate].xSize = xSize;
				pc[nextCreate].ySize = ySize;
				pc[nextCreate].center = nextSc.center.clone();
				mapCenter=nextSc.center.clone();
				pc[nextCreate].scale = nextSc.scale;
				Projection p = ProjFactory.getInstance(pc[nextCreate].center,nextSc.course, nextSc.scale, xSize, ySize);
				pc[nextCreate].setP(p);
//				p.inverse(xSize, 0, pc[nextCreate].screenRU);
//				p.inverse(0, ySize, pc[nextCreate].screenLD);
				// pcCollect.trace=nextSc.trace;
				// pcCollect.dataReader=nextSc.dataReader;
				// cleans the screen
				pc[nextCreate].g = img[nextCreate].getGraphics();
				pc[nextCreate].g.setColor(C.BACKGROUND_COLOR);
				pc[nextCreate].g.fillRect(0, 0, xSize, ySize);
//				pc[nextCreate].g.setColor(0x00FF0000);
//				pc[nextCreate].g.drawRect(0, 0, xSize-1, ySize-1);
//				pc[nextCreate].g.drawRect(20, 20, xSize-41, ySize-41);
				pc[nextCreate].squareDstToWay = Float.MAX_VALUE;
				pc[nextCreate].config = tr.getConfig();
				pc[nextCreate].target = nextSc.target;
				// System.out.println("create " + pcCollect);


				float boost=tr.getConfig().getDetailBoostMultiplier();
				
				/**
				 * At the moment we don't really have proper layer support
				 * in the data yet, so only split it into Area, Way and Node
				 * layers
				 */
				byte layersToRender[] = {Tile.LAYER_AREA, 1 | Tile.LAYER_AREA , 2 | Tile.LAYER_AREA,
						3 | Tile.LAYER_AREA, 4 | Tile.LAYER_AREA,  0, 1, 2, 3, 4, Tile.LAYER_NODE};
				
				/**
				 * Draw each layer seperately to enforce paint ordering.
				 *   
				 */				
				for (byte layer = 0; layer < layersToRender.length; layer++) {
					byte minTile = pc[nextCreate].c.scaleToTile((int)(pc[nextCreate].scale / boost));
					if ((minTile >= 3) && (t[3] != null)) {
						t[3].paint(pc[nextCreate],layersToRender[layer]);
						Thread.yield();
					}
					if ((minTile >= 2) && (t[2] != null)) {
						t[2].paint(pc[nextCreate], layersToRender[layer]);
						Thread.yield();
					}
					if ((minTile >= 1) && (t[1] != null)) {
						t[1].paint(pc[nextCreate], layersToRender[layer]);
						Thread.yield();
					}
					if (t[0] != null) {
						t[0].paint(pc[nextCreate], layersToRender[layer]);
					}
					/**
					 * Drawing waypoints
					 */
					if (t[5] != null) {
						t[5].paint(pc[nextCreate], layersToRender[layer]);
					}
					/**
					 * Drawing debuginfo for routing
					 */
//					if (t[4] != null) {
//						t[4].paint(pc[nextCreate], layersToRender[layer]);
//					}
					if (suspended) {
						// Don't continue rendering if suspended
						pc[nextCreate].state = PaintContext.STATE_READY;
						break;
					}
				}
				//#mdebug
				long endTime = System.currentTimeMillis();
				logger.info("Painting map took " + (endTime - startTime) + "ms");
				//#enddebug

				newCollected();
				createImageCount++;				
				needRedraw = false;
				tr.cleanup();
				// System.out.println("create ready");
				//System.gc();

			}
		} catch (OutOfMemoryError oome) {
		   String recoverZoomedIn="";
		   crash++;
		   if(tr.scale>10000 && crash < MAXCRASHES) {
		    tr.scale/= 1.5f;
		    recoverZoomedIn=" Zooming in to recover.";
		   }   
		   logger.fatal("ImageCollector thread crashed with out of memory: " + oome.getMessage() + recoverZoomedIn);
		} catch (Exception e) {
			crash++;
			logger.silentexception("ImageCollector thread crashed unexpectadly with error ", e);
		}
		if(crash>=MAXCRASHES) {
		   logger.fatal("ImageCollector crashed too often. Aborting.");
		}
		} while (!shutdown && crash <MAXCRASHES);
	}
	
	public void suspend() {
		suspended = true;
	}
	public void resume() {
		suspended = false;
	}
	
	public synchronized void stop(){
		shutdown=true;
		notify();
	}
	public void restart(){
		processorThread = new Thread(this,"ImageCollector");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	/** copy the last created image to the real screen
	 *  but with the last collected position and direction in the center
	 */
	public void paint(PaintContext screenPc){
		System.out.println("paint this: " +screenPc);
		System.out.println("paint image: " +pc[nextPaint]);
		if (suspended) return;
		
//		nextSc=screenPc.cloneToScreenContext();
		if (screenPc == null){
			System.out.println("ScreenPc ist null");
		}
		if (screenPc.center == null){
			System.out.println("ScreenPc.center ist null");
		}
		nextSc.center=screenPc.center.clone();
		nextSc.course=screenPc.course;
		nextSc.scale=screenPc.scale;
		nextSc.xSize=xSize;
		nextSc.ySize=ySize;
		Projection p = ProjFactory.getInstance(nextSc.center,nextSc.course, nextSc.scale, xSize, ySize);
		nextSc.setP(p);
		screenPc.setP(p);
		screenPc.xSize=xSize;
		screenPc.ySize=ySize;
		stat=STATE_SC_READY;
		newPaintAvail=false;
		Projection p2 = pc[nextPaint].getP();
		if (p2 == null){
			pc[nextPaint].setP(nextSc.getP());
		}
		p2.forward(nextSc.center, oldCenter);
		System.out.println("old Center = " + oldCenter.x + "/" + oldCenter.y);
		System.out.println("paint nextCreate: " +pc[nextCreate]);

		screenPc.g.drawImage(img[nextPaint], 
				nextSc.xSize-oldCenter.x,
				nextSc.ySize-oldCenter.y,
				Graphics.VCENTER|Graphics.HCENTER); 
		//Test if the new center is in the midle of the screen, in which 
		//case we don't need to redraw, as nothing has changed. 
		if (oldCenter.x != nextSc.xSize/2 || oldCenter.y != nextSc.ySize/2 || p2.getCourse() != nextSc.course ) { 
			//The center of the screen has moved, so need 
			//to redraw the map image  
			needRedraw = true; 
			System.out.println("Moved, needs redrawing"); 
		} else {
			System.out.println("same Pos no needs  for redrawing"); 

		}

		String name = null;
		if (pc[nextPaint].actualWay != null && pc[nextPaint].actualWay.nameIdx != -1){
			name=screenPc.trace.getName(pc[nextPaint].actualWay.nameIdx);
			String maxspeed=null;
			if (pc[nextPaint].actualWay.maxspeed != 0){
				maxspeed=" SL:" + pc[nextPaint].actualWay.maxspeed;
				if (name == null){
					name = maxspeed;
				} else {
					name = name + maxspeed;
				}
			}
			tr.source=pc[nextPaint].currentPos;
		}
		if(statusFontHeight==0) {
			statusFontHeight=screenPc.g.getFont().getHeight();
		}
		boolean showLatLon=tr.getConfig().getCfgBitState(Configuration.CFGBIT_SHOWLATLON);
		if ( showLatLon || name!=null) {
			screenPc.g.setColor(255,255,255);
			screenPc.g.fillRect(0,screenPc.ySize-statusFontHeight, screenPc.xSize, statusFontHeight);
			screenPc.g.setColor(0,0,0);
		}
		if (showLatLon) {
			screenPc.g.drawString("lat: " + Float.toString(pc[nextPaint].center.radlat*MoreMath.FAC_RADTODEC),5,screenPc.ySize, Graphics.LEFT | Graphics.BOTTOM);
			screenPc.g.drawString("lon: " + Float.toString(pc[nextPaint].center.radlon*MoreMath.FAC_RADTODEC),screenPc.xSize/2 + 5,screenPc.ySize, Graphics.LEFT | Graphics.BOTTOM);
		} else {
			if (name != null){
				screenPc.g.drawString(name,
					screenPc.xSize/2, screenPc.ySize, Graphics.BOTTOM|Graphics.HCENTER);
			}
		}
			
		if (pc[nextPaint].scale != screenPc.scale){
			needRedraw=true;
		}
		if (needRedraw) {
			synchronized (this) {
				notifyAll();
			}
		} else {
			//System.out.println("No need to redraw after painting");
		}
	}
	private synchronized void newCollected(){
		pc[nextCreate].state=PaintContext.STATE_READY;
		nextPaint=nextCreate;
		nextCreate=(byte) ((nextCreate + 1) % 2);
		newPaintAvail=true;		
		tr.requestRedraw();
	}

	/**
	 * inform the ImagecColloctor, that new vector-Data is available
	 * and its tim to create a new Image
	 */
	public synchronized void newDataReady() {
		needRedraw=true;
		notify();
	}
	
	public Projection getCurrentProjection(){
		return pc[nextPaint].getP();
	}
	
}
