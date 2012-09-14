/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * 	      Copyright (c) 2010-2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.data;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.graphics.Projection;
import net.sharenav.sharenav.ui.Trace;

public class ScreenContext {
	/** width of the screen map area (without overscan) */
	public int xSize;
	/** hight of the screen map area (without overscan) */
	public int ySize;
	public Node searchRU = new Node();
	public Node searchLD = new Node();
	/** this are the real world coordinates that represents the midpoint of interested area
	 * Not in every case the center of the image
	 */
	public Node center = new Node();
	/** When in keep on road mode, use this for GPS position
	 */
	public Node gpsNode = new Node();
	public float scale = 15000f;
	byte viewId = 1;
	private volatile Projection p;
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
		sc.center = center.copy();
		sc.gpsNode = gpsNode.copy();
		sc.trace = trace;	
		/**
		 * PIXEL_PER_METER
		 */
		sc.ppm = ppm;
		sc.dest = dest;
		sc.course = course;
		sc.p=p;
		return sc;
	}

	public Projection getP() {
		return p;
	}

	public void setP(Projection p) {
		this.p = p;
		float scale = p.getScale();
		// Earth circumference / scale / pixel per meter
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
