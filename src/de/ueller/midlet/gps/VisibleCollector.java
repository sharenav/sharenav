package de.ueller.midlet.gps;

import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.Tile;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 * 
 * this class collects all visible obeject for later painting . Run in a low proirity
 * to avoid interruptin GUI.
 */


//import de.ueller.midlet.gps.Logger;

public class VisibleCollector implements Runnable {
//	private final static Logger logger=Logger.getInstance(Names.class,Logger.TRACE);

	private final static byte STATE_WAIT_FOR_SC = 0;
	private final static byte STATE_SC_READY = 1;
	private final static byte STATE_ELEMETS_COLLETING = 2;
	private final static byte STATE_ELEMETS_READY = 3;
	

	boolean lockg=false;
	boolean lockc=false;
	boolean newPaintAvail=false;
	boolean shutdown=false;
	private final Tile t[];
	private Thread processorThread;
	private ScreenContext sc;
	private ScreenContext nextSc;
	private VisibleElements veCollect=new VisibleElements(1);
	private VisibleElements vePaint=new VisibleElements(2);
	private VisibleElements veREADY=new VisibleElements(3);
	byte stat=0;

	
	public VisibleCollector(Tile[] t) {
		super();
		this.t=t;
		processorThread = new Thread(this,"VisibleCollector");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}

	public void run() {
		try {
			while (stat == STATE_WAIT_FOR_SC){
				synchronized (this) {
					wait();
				}				
			}
			while (! shutdown){
				sc=nextSc;
				veCollect.cleanAll();
				if (sc.scale < 90000 && t[3] != null){
					t[3].collect(sc, veCollect);
				} 
				Thread.yield();
				if (sc.scale < 360000 && t[2] != null){
					t[2].collect(sc, veCollect);
				} 
				Thread.yield();
				if (sc.scale < 1800000f && t[1] != null){
					t[1].collect(sc, veCollect);
				} 
				Thread.yield();
				if ( t[0] != null){
					t[0].collect(sc, veCollect);
				}
				Thread.yield();
				newCollected();
				Thread.yield();
				synchronized (this) {
					wait(5000);
				}			

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void stop(){
		shutdown=true;
	}
	
	public void initScreenContext(ScreenContext nextSc){
		if (stat != STATE_WAIT_FOR_SC)
			throw new IllegalStateException("not waiting for ScreenContext");
		sc=nextSc;
		stat=STATE_SC_READY;
		notify();
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
		VisibleElements ve=veREADY;
		veREADY=vePaint;
		vePaint=ve;
		newPaintAvail=false;
		lockg=false;
		}
		vePaint.paint(pc);
	}
//	public synchronized VisibleElements getElements(ScreenContext nextSc){
//		lockg=true;
//		while (lockc){
//			try {
//				wait();
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		VisibleElements ve=veREADY;
//		veREADY=vePaint;
//		vePaint=ve;
//		lockg=false;
//		notify();
//		this.nextSc = nextSc.cloneToScreenContext();
//		return vePaint;
//	}
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
		VisibleElements ve=veREADY;
		veREADY=veCollect;
		veCollect=ve;
		newPaintAvail=true;
		lockc=false;
	}
	
}
