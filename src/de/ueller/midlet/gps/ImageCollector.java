package de.ueller.midlet.gps;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Mercator;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.QueueDataReader;
import de.ueller.midlet.gps.tile.Tile;
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
	private ScreenContext sc;
	private ScreenContext nextSc;
	private Image imgCollect;
	private PaintContext pcCollect;
	private Image imgPaint;
	private PaintContext pcPaint;
	private Image imgReady;
	private PaintContext pcReady;
	private PaintContext pc;
	byte stat=0;
	int xSize;
	int ySize;
	IntPoint newCenter=new IntPoint(0,0);
	IntPoint oldCenter=new IntPoint(0,0);
	private boolean needRedraw=false;
	
	public ImageCollector(Tile[] t,int x,int y,Trace tr,QueueDataReader tir) {
		super();
		this.t=t;
		try {
			pc=new PaintContext(tr,tir);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		xSize=x+80;
		ySize=y+80;
		imgCollect=Image.createImage(xSize,ySize);
		imgPaint=Image.createImage(xSize,ySize);
		imgReady=Image.createImage(xSize,ySize);
		try {
			pcCollect=new PaintContext(tr,tir);
			pcPaint=new PaintContext(tr,tir);
			pcReady=new PaintContext(tr,tir);
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
				try {
//				create PaintContext
					pcCollect.xSize=xSize;
					pcCollect.ySize=ySize;
					pcCollect.center=nextSc.center.clone();
					pcCollect.scale=nextSc.scale;
					pcCollect.p=new Mercator(pcCollect.center,pcCollect.scale,xSize,ySize);
					pcCollect.p.inverse(pcCollect.xSize, 0,pcCollect.screenRU);
					pcCollect.p.inverse(0,pcCollect.ySize,pcCollect.screenLD);
					pcCollect.g=imgCollect.getGraphics();
//					pcCollect.trace=nextSc.trace;
//					pcCollect.dataReader=nextSc.dataReader;
				// cleans the screen
					pcCollect.g.setColor(155, 255, 155);
					pcCollect.g.fillRect(0, 0, pcCollect.xSize, pcCollect.ySize);
					pcCollect.squareDstToWay=Float.MAX_VALUE;
//				System.out.println("create " + pcCollect);
				
				if ((pcCollect.scale < 45000) && (t[3] != null)){
					t[3].paint(pcCollect);
					Thread.yield();
				} 
				if ((pcCollect.scale < 180000) && (t[2] != null)){
					t[2].paint(pcCollect);
					Thread.yield();
				} 
				if ((pcCollect.scale < 900000f) && (t[1] != null)){
					t[1].paint(pcCollect);
					Thread.yield();
				} 
				if ( t[0] != null){
					t[0].paint(pcCollect);
				}
				newCollected();
				if (needRedraw){
					pcCollect.trace.requestRedraw();
					needRedraw=false;
				}
				Thread.yield();
//				System.out.println("create ready");
				System.gc();
				synchronized (this) {
					wait(5000);
				}			
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

	}
	
	public void stop(){
		shutdown=true;
	}
	
	
	public void paint(PaintContext pc){
		nextSc=pc.cloneToScreenContext();
		
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
		Image img=imgReady;
		PaintContext p=pcReady;
		imgReady=imgPaint;
		pcReady=pcPaint;
		imgPaint=img;
		pcPaint=p;
		newPaintAvail=false;
		lockg=false;
		}
//		System.out.println("create pc" + pc);
//		System.out.println("create pcPaint" + pcPaint);
//		pcPaint.p.forward(pc.center, newCenter);
//		System.out.println("new Center = " + newCenter.x + "/" + newCenter.y);
		pc.p.forward(pcPaint.center, oldCenter);
//		System.out.println("old Center = " + oldCenter.x + "/" + oldCenter.y);
//		System.out.println("paint: " +pc);
		pc.g.drawImage(imgPaint, 
				oldCenter.x,
				oldCenter.y,
				Graphics.VCENTER|Graphics.HCENTER); 
		if (pcPaint.actualWay != null && pcPaint.actualWay.nameIdx != null){
			String name=pc.trace.getName(pcPaint.actualWay.nameIdx);
			String maxspeed=null;
			if (pcPaint.actualWay.maxspeed != 0){
				maxspeed=" TL:" + pcPaint.actualWay.maxspeed;
				if (name == null){
					name = maxspeed;
				} else {
					name = name + maxspeed;
				}
			}
			if (name != null){
				pc.g.setColor(255,255,255);
				pc.g.fillRect(0,pc.ySize-15, pc.xSize, 15);
				pc.g.setColor(0,0,0);
				pc.g.drawString(name,
					pc.xSize/2, pc.ySize, Graphics.BOTTOM|Graphics.HCENTER);
			}
		}
		if (pcPaint.scale != pc.scale){
			needRedraw=true;
		}
	}
	private synchronized void newCollected(){
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
		Image img=imgReady;
		PaintContext p=pcReady;
		imgReady=imgCollect;
		pcReady=pcCollect;
		imgCollect=img;
		pcCollect=p;
		newPaintAvail=true;
		lockc=false;
	}
	
}
