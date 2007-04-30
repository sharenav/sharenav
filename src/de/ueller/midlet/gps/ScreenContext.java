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
	public Projection p;
	public Trace trace;
	public QueueDataReader dataReader;
	public QueueReader dictReader;
	
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
		return sc;
	}


}
