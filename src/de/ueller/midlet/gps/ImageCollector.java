package de.ueller.midlet.gps;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Mercator;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.PaintContext;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 * 
 * this class collects all visible obeject to a offline image for later painting .
 * Run in a low proirity to avoid interruptin GUI.
 */


//import de.ueller.midlet.gps.Logger;

public class ImageCollector implements Runnable {
	private final static Logger logger=Logger.getInstance(ImageCollector.class,Logger.TRACE);

	private final static byte STATE_WAIT_FOR_SC = 0;
	private final static byte STATE_SC_READY = 1;
	

	//private boolean lockg=false;
	//private boolean lockc=false;
	private boolean newPaintAvail=false;
	private boolean shutdown=false;
	private boolean suspended=true;
	private final Tile t[];
	private Thread processorThread;
	private ScreenContext nextSc;

	private Image[] img=new Image[2];
	private PaintContext[] pc=new PaintContext[2];
	public static Node mapCenter = new Node();
	byte nextCreate=0;
	byte nextPaint=0;

	byte stat=0;
	int xSize;
	int ySize;
	IntPoint newCenter=new IntPoint(0,0);
	IntPoint oldCenter=new IntPoint(0,0);
	private boolean needRedraw=false;
	int createImageCount=0;
	private final Trace tr;
	
	public ImageCollector(Tile[] t,int x,int y,Trace tr, Images i, C c) {
		super();
		this.t=t;
		this.tr = tr;
		xSize=x+10;
		ySize=y+10;
		img[0]=Image.createImage(xSize,ySize);
		img[1]=Image.createImage(xSize,ySize);
		try {
			pc[0]=new PaintContext(tr, i);
			pc[0].c = c;
			pc[1]=new PaintContext(tr, i);
			pc[1].c = c;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		processorThread = new Thread(this,"ImageCollector");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}

	public void run() {
		try {
			// logger.info("wait for sc");
			while (stat == STATE_WAIT_FOR_SC) {
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
				while (pc[nextCreate].state != PaintContext.STATE_READY) {
					synchronized (this) {
						try {
							// System.out.println("img not ready");
							wait();
						} catch (InterruptedException e) {
						}
					}
				}
				if (suspended)
					continue;
				pc[nextCreate].state = PaintContext.STATE_IN_CREATE;

				// create PaintContext
				pc[nextCreate].xSize = xSize;
				pc[nextCreate].ySize = ySize;
				pc[nextCreate].center = nextSc.center.clone();
				mapCenter=nextSc.center.clone();
				pc[nextCreate].scale = nextSc.scale;
				Mercator p = new Mercator(pc[nextCreate].center, nextSc.scale, xSize, ySize);
				pc[nextCreate].setP(p);
				p.inverse(xSize, 0, pc[nextCreate].screenRU);
				p.inverse(0, ySize, pc[nextCreate].screenLD);
				// pcCollect.trace=nextSc.trace;
				// pcCollect.dataReader=nextSc.dataReader;
				// cleans the screen
				pc[nextCreate].g = img[nextCreate].getGraphics();
				pc[nextCreate].g.setColor(155, 255, 155);
				pc[nextCreate].g.fillRect(0, 0, xSize, ySize);
				pc[nextCreate].squareDstToWay = Float.MAX_VALUE;
				pc[nextCreate].config = tr.getConfig();
				pc[nextCreate].target = nextSc.target;
				// System.out.println("create " + pcCollect);

				/**
					There's no pow()-function in J2ME so manually
					calculate 1.5^ScaleDetailBoost to get factor
					to multiply with Zoom Level limits
				**/
				int detailBoost=tr.getConfig().getDetailBoost();
				float boost=1;
				for(int i=1;i<=detailBoost;i++) {
					boost*=1.5;
				}
				/**
				 * At the moment we don't really have proper layer support
				 * in the data yet, so only split it into Area, Way and Node
				 * layers
				 */
				byte layersToRender[] = {Tile.LAYER_AREA, 0, Tile.LAYER_NODE};
				
				/**
				 * Draw each layer seperately to enforce paint ordering.
				 *   
				 */
				logger.info("Drawing image collector");
				for (byte layer = 0; layer < layersToRender.length; layer++) {
					logger.info("Drawing image collector layer " + layer);
					if ((pc[nextCreate].scale < 45000 * boost) && (t[3] != null)) {
						t[3].paint(pc[nextCreate],layersToRender[layer]);
						Thread.yield();
					}
					if ((pc[nextCreate].scale < 180000 * boost) && (t[2] != null)) {
						t[2].paint(pc[nextCreate], layersToRender[layer]);
						Thread.yield();
					}
					if ((pc[nextCreate].scale < 900000f * boost) && (t[1] != null)) {
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
					if (suspended) {
						// Don't continue rendering if suspended
						pc[nextCreate].state = PaintContext.STATE_READY;
						break;
					}
				}
				logger.info("finished Drawing image collector");

				newCollected();
				createImageCount++;
				tr.requestRedraw();
				needRedraw = false;
				tr.cleanup();
				// System.out.println("create ready");
				System.gc();

			}
		} catch (OutOfMemoryError oome) {
			logger.fatal("ImageCollector thread crashed with out of memory: " + oome.getMessage());			
		} catch (Exception e) {
			logger.exception("ImageCollector thread crashed unexpectadly with error ", e);
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
		notify();
	}
	public void restart(){
		processorThread = new Thread(this,"ImageCollector");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	/** copy the last created image to the real screen
	 *  but with the last collected position in the center
	 */
	public void paint(PaintContext screenPc){
		
		if (suspended) return;
		
		nextSc=screenPc.cloneToScreenContext();
		
		stat=STATE_SC_READY;		
		newPaintAvail=false;
		screenPc.getP().forward(pc[nextPaint].center, oldCenter);
//		System.out.println("old Center = " + oldCenter.x + "/" + oldCenter.y);
//		System.out.println("paint: " +pc);
		screenPc.g.drawImage(img[nextPaint], 
				oldCenter.x,
				oldCenter.y,
				Graphics.VCENTER|Graphics.HCENTER); 
		//Test if the new center is in the midle of the screen, in which 
		//case we don't need to redraw, as nothing has changed. 
		if (oldCenter.x != nextSc.xSize/2 || oldCenter.y != nextSc.ySize/2) { 
			//The center of the screen has moved, so need 
			//to redraw the map image  
			needRedraw = true; 
			//System.out.println("Moved, needs redrawing"); 
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
		if (Trace.showLatLon == 1 || name!=null) {
			screenPc.g.setColor(255,255,255);
			screenPc.g.fillRect(0,screenPc.ySize-15, screenPc.xSize, 15);
			screenPc.g.setColor(0,0,0);
		}
		if (Trace.showLatLon == 1) {
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
	
}
