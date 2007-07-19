package de.ueller.midlet.gps;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Mercator;
import de.ueller.midlet.gps.tile.Images;
import de.ueller.midlet.gps.tile.PaintContext;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 * 
 * this class collects all visible obeject to a offline image for later painting .
 * Run in a low proirity to avoid interruptin GUI.
 */


//import de.ueller.midlet.gps.Logger;

public class ImageCollector implements Runnable {
//	private final static Logger logger=Logger.getInstance(ImageCollector.class,Logger.TRACE);

	private final static byte STATE_WAIT_FOR_SC = 0;
	private final static byte STATE_SC_READY = 1;
	

	boolean lockg=false;
	boolean lockc=false;
	boolean newPaintAvail=false;
	boolean shutdown=false;
	private final Tile t[];
	private Thread processorThread;
	private ScreenContext nextSc;

	private Image[] img=new Image[2];
	private PaintContext[] pc=new PaintContext[2];
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
	
	public ImageCollector(Tile[] t,int x,int y,Trace tr,QueueDataReader tir,QueueReader dir,Images i) {
		super();
		this.t=t;
		this.tr = tr;
		xSize=x+10;
		ySize=y+10;
		img[0]=Image.createImage(xSize,ySize);
		img[1]=Image.createImage(xSize,ySize);
		try {
			pc[0]=new PaintContext(tr,tir,dir,i);
			pc[1]=new PaintContext(tr,tir,dir,i);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		processorThread = new Thread(this,"ImageCollector");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}

	public void run() {
//		logger.info("wait for sc");
			while (stat == STATE_WAIT_FOR_SC){
				synchronized (this) {
					try {
						wait();
					} catch (InterruptedException e) {
					}
				}				
			}
			while (! shutdown){
//				logger.info("loop");
				while (pc[nextCreate].state != PaintContext.STATE_READY){
					synchronized (this) {
						try {
//							System.out.println("img not ready");
							wait();
						} catch (InterruptedException e) {
						}
					}				
				}
				pc[nextCreate].state=PaintContext.STATE_IN_CREATE;
				try {
//				create PaintContext
					pc[nextCreate].xSize=xSize;
					pc[nextCreate].ySize=ySize;
					pc[nextCreate].center=nextSc.center.clone();
					pc[nextCreate].scale=nextSc.scale;
					Mercator p = new Mercator(pc[nextCreate].center,nextSc.scale,xSize,ySize);
					pc[nextCreate].setP(p);
					p.inverse(xSize, 0,pc[nextCreate].screenRU);
					p.inverse(0,ySize,pc[nextCreate].screenLD);
//					pcCollect.trace=nextSc.trace;
//					pcCollect.dataReader=nextSc.dataReader;
				// cleans the screen
					pc[nextCreate].g=img[nextCreate].getGraphics();
					pc[nextCreate].g.setColor(155, 255, 155);
					pc[nextCreate].g.fillRect(0, 0, xSize, ySize);
					pc[nextCreate].squareDstToWay=Float.MAX_VALUE;
					pc[nextCreate].config=tr.getConfig();
					pc[nextCreate].target=nextSc.target;
//				System.out.println("create " + pcCollect);
				
				if ((pc[nextCreate].scale < 45000) && (t[3] != null)){
					t[3].paint(pc[nextCreate]);
					Thread.yield();
				} 
				if ((pc[nextCreate].scale < 180000) && (t[2] != null)){
					t[2].paint(pc[nextCreate]);
					Thread.yield();
				} 
				if ((pc[nextCreate].scale < 900000f) && (t[1] != null)){
					t[1].paint(pc[nextCreate]);
					Thread.yield();
				} 
				if ( t[0] != null){
					t[0].paint(pc[nextCreate]);
				}
				newCollected();
				createImageCount++;
				if (needRedraw){
					tr.requestRedraw();
					needRedraw=false;
				}
				Thread.yield();
				tr.cleanup();
//				System.out.println("create ready");
				System.gc();
				synchronized (this) {
					wait(10000);
				}
				
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

	}
	
	public void stop(){
		shutdown=true;
		notify();
	}
	
	/** copy the last created image to the real sceen
	 *  but with the last collected position in the center
	 */
	public void paint(PaintContext screenPc){
		nextSc=screenPc.cloneToScreenContext();
		
		stat=STATE_SC_READY;
		synchronized (this) {
			notify();
		}		
		if (newPaintAvail){
			lockg=true;
			synchronized (this) {
				while (lockc) {
					System.err.println("locked from Collect");
					try {
						wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}		

			newPaintAvail=false;
			lockg=false;
		}
		screenPc.getP().forward(pc[nextPaint].center, oldCenter);
//		System.out.println("old Center = " + oldCenter.x + "/" + oldCenter.y);
//		System.out.println("paint: " +pc);
		screenPc.g.drawImage(img[nextPaint], 
				oldCenter.x,
				oldCenter.y,
				Graphics.VCENTER|Graphics.HCENTER); 
		if (pc[nextPaint].actualWay != null && pc[nextPaint].actualWay.nameIdx != null){
			String name=screenPc.trace.getName(pc[nextPaint].actualWay.nameIdx);
			String maxspeed=null;
			if (pc[nextPaint].actualWay.maxspeed != 0){
				maxspeed=" SL:" + pc[nextPaint].actualWay.maxspeed;
				if (name == null){
					name = maxspeed;
				} else {
					name = name + maxspeed;
				}
			}
			if (name != null){
				screenPc.g.setColor(255,255,255);
				screenPc.g.fillRect(0,screenPc.ySize-15, screenPc.xSize, 15);
				screenPc.g.setColor(0,0,0);
				screenPc.g.drawString(name,
					screenPc.xSize/2, screenPc.ySize, Graphics.BOTTOM|Graphics.HCENTER);
			}
		}
		if (pc[nextPaint].scale != screenPc.scale){
			needRedraw=true;
		}
	}
	private synchronized void newCollected(){
		pc[nextCreate].state=PaintContext.STATE_READY;
		lockc=true;
		while (lockg){
			try {
				System.err.println("locked from Paint");
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		nextPaint=nextCreate;
		nextCreate=(byte) ((nextCreate + 1) % 2);
		newPaintAvail=true;
		lockc=false;
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
