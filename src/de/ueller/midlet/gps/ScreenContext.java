package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Projection;

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
	
	public ScreenContext cloneToScreenContext() {
		ScreenContext sc=new ScreenContext();
		sc.scale=scale;
		sc.screenLD=screenLD.clone();
		sc.screenRU=screenRU.clone();
		sc.xSize=xSize;
		sc.ySize=ySize;
		sc.center=center.clone();
		sc.trace=trace;
		return sc;
	}


}
