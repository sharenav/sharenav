package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Projection;

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
	
	public float ppm=1f;
	/**
	 * hold, if there is any, the actual target position and element to the
	 * navigation target.
	 */
	public PositionMark target;
    /**
     * store the actual position and the actual way;
     */
	public PositionMark currentPos=new PositionMark(0f,0f);


	
	public ScreenContext cloneToScreenContext() {
		ScreenContext sc=new ScreenContext();
		sc.scale=scale;
		sc.screenLD=screenLD.clone();
		sc.screenRU=screenRU.clone();
		sc.xSize=xSize;
		sc.ySize=ySize;
		sc.center=center.clone();
		sc.trace=trace;		
		sc.ppm=ppm;
		sc.target=target;
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

	public boolean isVisible(float lat, float lon){
		if (lat < screenLD.radlat) {
			return false;
		}
		if (lon < screenLD.radlon) {
			return false;
		}
		if (lat > screenRU.radlat) {
			return false;
		}
		if (lon > screenRU.radlon) {
			return false;
		}
		return true;
	}
}
