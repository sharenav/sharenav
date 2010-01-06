/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.data.RoutePositionMark;

public class ScreenContext {

	public int xSize;
	public int ySize;
	public Node searchRU = new Node();
	public Node searchLD = new Node();
	public Node center = new Node();
	public float scale = 15000f;
	byte viewId = 1;
	private Projection p;
	public int course = 0;
	public Trace trace;
	
	public float ppm = 1f;
	
	/** Hold, if there is any, the current destination position and element. */
	public RoutePositionMark dest;
	
    /** Stores the current position */
	public RoutePositionMark currentPos = new RoutePositionMark(0f, 0f);


	
	public ScreenContext cloneToScreenContext() {
		ScreenContext sc = new ScreenContext();
		sc.scale = scale;
//		sc.screenLD = screenLD.clone();
//		sc.screenRU = screenRU.clone();
		sc.xSize = xSize;
		sc.ySize = ySize;
		sc.center = center.clone();
		sc.trace = trace;		
		sc.ppm = ppm;
		sc.dest = dest;
		sc.course = course;
		return sc;
	}

	public Projection getP() {
		return p;
	}

	public void setP(Projection p) {
		this.p = p;
		float scale = p.getScale();
		// TODO: Explain where this constant is derived from.
		ppm = (40075016.6855784861531768177614f / scale / p.getPPM());
	}

//	public boolean isVisible(float lat, float lon){
//		if (lat < screenLD.radlat) {
//			return false;
//		}
//		if (lon < screenLD.radlon) {
//			return false;
//		}
//		if (lat > screenRU.radlat) {
//			return false;
//		}
//		if (lon > screenRU.radlon) {
//			return false;
//		}
//		return true;
//	}
	
	public String toString() {
		return "ScreenContext: " + p;
	}
}
