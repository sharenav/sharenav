package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.tile.QueueDataReader;
import de.ueller.midlet.gps.tile.QueueReader;

public class ScreenContext {

	public int xSize;
	public int ySize;
	public Node screenRU = new Node();
	public Node screenLD = new Node();
	public Node center = new Node();
	public float scale = 15000f;
	byte viewId = 1;
	private Projection p;
	public Trace trace;
	public QueueDataReader dataReader;
	public QueueReader dictReader;
	public float ppm=1f;
	
	public ScreenContext cloneToScreenContext() {
		ScreenContext sc=new ScreenContext();
		sc.scale=scale;
		sc.screenLD=screenLD.clone();
		sc.screenRU=screenRU.clone();
		sc.xSize=xSize;
		sc.ySize=ySize;
		sc.center=center.clone();
		sc.trace=trace;
		sc.dataReader=dataReader;
		sc.ppm=ppm;
		return sc;
	}

	public Projection getP() {
		return p;
	}

	public void setP(Projection p) {
		this.p = p;
		float scale = p.getScale();
		ppm=(40075016.6855784861531768177614f/scale/p.getPPM());
	}


}
