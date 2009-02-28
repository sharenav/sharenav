package de.ueller.midlet.gps;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjFactory;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.WayDescription;


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

	//private boolean lockg=false;
	//private boolean lockc=false;
	private volatile boolean shutdown=false;
	private volatile boolean suspended=true;
	private final Tile t[];
	private Thread processorThread;
	private ScreenContext nextSc=new ScreenContext() ;

	private Image[] img=new Image[2];
	private volatile PaintContext[] pc=new PaintContext[2];
	public static Node mapCenter = new Node();
	public static volatile long icDuration = 0;
	byte nextCreate=1;
	byte nextPaint=0;

//	volatile byte stat=0;
	int xSize;
	int ySize;
	IntPoint newCenter=new IntPoint(0,0);
	IntPoint oldCenter=new IntPoint(0,0);
	float oldCourse;
	private volatile boolean needRedraw=false;
	public static volatile int createImageCount=0;
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
		PaintContext createPC = null;
		final byte MAXCRASHES = 5;
		byte crash=0;
		do {
		try {
			while (!shutdown) {
				if (!needRedraw || suspended) {
					synchronized (this) {
						try {
							wait(30000);
						} catch (InterruptedException e) {
							continue; // Recheck condition of the loop
						}
					}
				}
				//#debug debug
				logger.debug("Redrawing Map");
				synchronized (this) {
					while (pc[nextCreate].state != PaintContext.STATE_READY && !shutdown) {
						try {
							// System.out.println("img not ready");
							wait(1000);
						} catch (InterruptedException e) {
						}
					}
					if (suspended || shutdown)
						continue;
					pc[nextCreate].state = PaintContext.STATE_IN_CREATE;
				}
				createPC = pc[nextCreate];				
				
				long startTime = System.currentTimeMillis();

				// create PaintContext
				createPC.xSize = xSize;
				createPC.ySize = ySize;
				createPC.center = nextSc.center.clone();
				mapCenter=nextSc.center.clone();
				createPC.scale = nextSc.scale;
				Projection p = ProjFactory.getInstance(createPC.center,nextSc.course, nextSc.scale, xSize, ySize);
				createPC.setP(p);
//				p.inverse(xSize, 0, createPC.screenRU);
//				p.inverse(0, ySize, createPC.screenLD);
				// pcCollect.trace=nextSc.trace;
				// pcCollect.dataReader=nextSc.dataReader;
				// cleans the screen
				createPC.g = img[nextCreate].getGraphics();
				createPC.g.setColor(C.BACKGROUND_COLOR);
				createPC.g.fillRect(0, 0, xSize, ySize);
//				createPC.g.setColor(0x00FF0000);
//				createPC.g.drawRect(0, 0, xSize-1, ySize-1);
//				createPC.g.drawRect(20, 20, xSize-41, ySize-41);
				createPC.squareDstToWay = Float.MAX_VALUE;
				createPC.squareDstToRoutableWay = Float.MAX_VALUE;
				createPC.squareDstToRoutePath = Float.MAX_VALUE;
				createPC.dstToRoutePath = Integer.MAX_VALUE;
				createPC.target = nextSc.target;
				createPC.course = nextSc.course;
				// System.out.println("create " + pcCollect);
				
				Way.setupDirectionalPenalty(createPC, Trace.getInstance().speed, Trace.getInstance().gpsRecenter);


				float boost=Configuration.getDetailBoostMultiplier();
				
				/*
				 * layers containing highlighted path segments
				 */
				createPC.hlLayers = 0;
				
				// highlighted path only on top if gpsCentered
				createPC.highlightedPathOnTop = tr.gpsRecenter;
				
				/**
				 * At the moment we don't really have proper layer support
				 * in the data yet, so only split it into Area, Way and Node
				 * layers
				 */
				byte layersToRender[] = {Tile.LAYER_AREA, 1 | Tile.LAYER_AREA , 2 | Tile.LAYER_AREA,
						3 | Tile.LAYER_AREA, 4 | Tile.LAYER_AREA,  0, 1, 2, 3, 4,
						0 | Tile.LAYER_HIGHLIGHT, 1 | Tile.LAYER_HIGHLIGHT,
						2 | Tile.LAYER_HIGHLIGHT, 3 | Tile.LAYER_HIGHLIGHT,
						Tile.LAYER_NODE};
				
				/**
				 * Draw each layer seperately to enforce paint ordering.
				 *
				 */
				for (byte layer = 0; layer < layersToRender.length; layer++) {
					// render only highlight layers which actually have highlighted path segments
					if (
						(layersToRender[layer] & Tile.LAYER_HIGHLIGHT) > 0
						&& layersToRender[layer] != Tile.LAYER_NODE
					) {
						/**
						 * as we do two passes for each way layer when gps recentered - one for the ways and one for the route line on top,
						 * we can use in the second pass the determined route path connection / idx
						 * to highlight the route line in the correct / prior route line color.
						 * when not gps recentered, this info will be by one image obsolete however
						 */ 
						if (layersToRender[layer] == (0 | Tile.LAYER_HIGHLIGHT)) {
							if (createPC.squareDstToRoutePath != Float.MAX_VALUE) {
								Node n1 = new Node();
								Node n2 = new Node();
								createPC.getP().inverse(0, 0, n1);
								createPC.getP().inverse( (int) Math.sqrt(createPC.squareDstToRoutePath), 0, n2);
								RouteInstructions.dstToRoutePath = (int) ProjMath.getDistance(n1, n2);
								RouteInstructions.routePathConnection = createPC.routePathConnection;
								RouteInstructions.pathIdxInRoutePathConnection = createPC.pathIdxInRoutePathConnection;
							}

						}
						byte relLayer = (byte)(((int)layersToRender[layer]) & 0x0000000F);
						if ( (createPC.hlLayers & (1 << relLayer)) == 0) {
							continue;
						}
					}
					byte minTile = C.scaleToTile((int)(createPC.scale / boost));
					if ((minTile >= 3) && (t[3] != null)) {
						t[3].paint(createPC,layersToRender[layer]);
						Thread.yield();
					}
					if ((minTile >= 2) && (t[2] != null)) {
						t[2].paint(createPC, layersToRender[layer]);
						Thread.yield();
					}
					if ((minTile >= 1) && (t[1] != null)) {
						t[1].paint(createPC, layersToRender[layer]);
						Thread.yield();
					}
					if (t[0] != null) {
						t[0].paint(createPC, layersToRender[layer]);
					}
					/**
					 * Drawing waypoints
					 */
					if (t[5] != null) {
						t[5].paint(createPC, layersToRender[layer]);
					}
					/**
					 * Drawing debuginfo for routing
					 */
					if (t[4] != null && Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS)) {
						t[4].paint(createPC, layersToRender[layer]);
					}
					if (suspended) {
						// Don't continue rendering if suspended
						createPC.state = PaintContext.STATE_READY;
						break;
					}
				}

				icDuration = System.currentTimeMillis() - startTime;
				//#mdebug
				logger.info("Painting map took " + icDuration + " ms");
				//#enddebug
				createPC.state=PaintContext.STATE_READY;
				if (!shutdown)
					newCollected();
				createImageCount++;				
				needRedraw = false;
				tr.cleanup();
				// System.out.println("create ready");
				//System.gc();
			}
		} catch (OutOfMemoryError oome) {
			if (createPC != null) {
				createPC.state = PaintContext.STATE_READY;
			}
		   String recoverZoomedIn="";
		   crash++;
		   if(tr.scale>10000 && crash < MAXCRASHES) {
		    tr.scale/= 1.5f;
		    recoverZoomedIn=" Zooming in to recover.";
		   }   
		   logger.fatal("ImageCollector thread crashed with out of memory: " + oome.getMessage() + recoverZoomedIn);
		} catch (Exception e) {
			crash++;
			logger.exception("ImageCollector thread crashed unexpectadly with error ", e);
		}
		if(crash>=MAXCRASHES) {
		   logger.fatal("ImageCollector crashed too often. Aborting.");
		}
		} while (!shutdown && crash <MAXCRASHES);
		processorThread = null;
		synchronized (this) {
			notifyAll();
		}
	}
	
	public void suspend() {
		suspended = true;
	}
	public void resume() {
		suspended = false;
	}
	
	public synchronized void stop(){
		shutdown=true;
		notifyAll();
		try {
			while ((processorThread != null) && (processorThread.isAlive())) {
				wait(1000);
			}
		} catch (InterruptedException e) {
			//Nothing to do
		}
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
		PaintContext paintPC;
//		System.out.println("paint this: " +screenPc);
//		System.out.println("paint image: " +pc[nextPaint]);
		if (suspended) return;
		
//		nextSc=screenPc.cloneToScreenContext();
		nextSc.center=screenPc.center.clone();
		nextSc.course=screenPc.course;
		nextSc.scale=screenPc.scale;
		nextSc.target=screenPc.target;
		nextSc.xSize=screenPc.xSize;
		nextSc.ySize=screenPc.ySize;
		Projection p = ProjFactory.getInstance(nextSc.center,nextSc.course, nextSc.scale, nextSc.xSize, nextSc.ySize);
		nextSc.setP(p);
		screenPc.setP(p);
		
		synchronized (this) {
			if (pc[nextPaint].state != PaintContext.STATE_READY) {
				logger.error("ImageCollector was trying to draw a non ready PaintContext " + pc[nextPaint].state);
				return;
			}
			paintPC = pc[nextPaint];
			paintPC.state = PaintContext.STATE_IN_PAINT;
		}

		p.forward(paintPC.center, oldCenter);
		screenPc.g.drawImage(img[nextPaint], 
				oldCenter.x, oldCenter.y,
				Graphics.VCENTER|Graphics.HCENTER); 
		//Test if the new center is in the midle of the screen, in which 
		//case we don't need to redraw, as nothing has changed. 
		if (oldCenter.x != nextSc.xSize/2 || oldCenter.y != nextSc.ySize/2 || paintPC.course != nextSc.course ) { 
			//The center of the screen has moved, so need 
			//to redraw the map image  
			needRedraw = true; 
		} 

		String name = null;
		/*
		 * As we are double buffering pc, nothing should be writing to paintPC
		 * therefore it should be safe to access the volatile variable actualWay 
		 */
		if (paintPC.actualWay != null){
			screenPc.actualWay = paintPC.actualWay;
			screenPc.actualSingleTile = paintPC.actualSingleTile;
			String maxspeed="";
			if (paintPC.actualWay.getMaxSpeed() != 0){
				maxspeed=" SL:" + pc[nextPaint].actualWay.getMaxSpeed();
			}

			if (paintPC.actualWay.nameIdx != -1) {
				name=screenPc.trace.getName(paintPC.actualWay.nameIdx);
			} else {
				WayDescription wayDesc = C.getWayDescription(paintPC.actualWay.type);
				name = "(unnamed " + wayDesc.description + ")";
			}
			if (name == null){
				name = maxspeed;
			} else {
				name = name + maxspeed;
			}
			tr.actualWay = pc[nextPaint].actualWay;
		}
		if (paintPC.nearestRoutableWay != null){
			tr.source=paintPC.currentPos;
		}
		if(statusFontHeight==0) {
			statusFontHeight=screenPc.g.getFont().getHeight();
		}
		boolean showLatLon=Configuration.getCfgBitState(Configuration.CFGBIT_SHOWLATLON);
		if ( showLatLon || name!=null) {
			screenPc.g.setColor(255,255,255);
			screenPc.g.fillRect(0,screenPc.ySize-statusFontHeight, screenPc.xSize, statusFontHeight);
			screenPc.g.setColor(0,0,0);
		}
		if (showLatLon) {
			screenPc.g.drawString("lat: " + Float.toString(paintPC.center.radlat*MoreMath.FAC_RADTODEC),5,screenPc.ySize, Graphics.LEFT | Graphics.BOTTOM);
			screenPc.g.drawString("lon: " + Float.toString(paintPC.center.radlon*MoreMath.FAC_RADTODEC),screenPc.xSize/2 + 5,screenPc.ySize, Graphics.LEFT | Graphics.BOTTOM);
		} else {
			if (name != null){
				screenPc.g.drawString(name,
					screenPc.xSize/2, screenPc.ySize, Graphics.BOTTOM|Graphics.HCENTER);
			}
		}
		
		
			
		if (paintPC.scale != screenPc.scale){
			needRedraw=true;
		}
		synchronized (this) {
			paintPC.state = PaintContext.STATE_READY;
			if (needRedraw) {
				notifyAll();
			} else {
				//System.out.println("No need to redraw after painting");
			}
		}
	}
	private synchronized void newCollected(){
		while ((pc[nextPaint].state != PaintContext.STATE_READY) || (pc[nextCreate].state != PaintContext.STATE_READY)) {
			try {
				wait(1000);
			} catch (InterruptedException e) {
			}
		}
		nextPaint=nextCreate;
		nextCreate=(byte) ((nextCreate + 1) % 2);
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
