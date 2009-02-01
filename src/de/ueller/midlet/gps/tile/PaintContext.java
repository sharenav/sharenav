/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps.tile;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.ScreenContext;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Way;


public class PaintContext extends ScreenContext {
	public final static byte DRAW_AREAS_NO=0;
	public final static byte DRAW_AREAS_OUTLINE=1;
	public final static byte DRAW_AREAS_FILL=2;
	public final static byte STATE_IN_CREATE=1;
	public final static byte STATE_IN_PAINT=2;
	public final static byte STATE_READY=0;
	public final static byte STATE_IN_COPY=3;
	
	public volatile byte state=0;
	public Graphics g;
	/** 
	 * used to avoid frequent memory allocations this point have to have
	 * a valid object after method exit 
	 */
	public IntPoint swapLineP=new IntPoint(0,0);
	/** 
	 * used to avoid frequent memory allocations this point have to have
	 * null after method exit. Point will used as startpoint of a line to
	 * indicate the fact that there is no startpoint at the begin of painting,
	 * this points to null 
	 */
	public IntPoint lineP1=null;
	/** 
	 * used to avoid frequent memory allocations this point have to have
	 * a valid Object after method exit. Point will used as end point of a line.
	 * the calculation go directly to the literals inside the object.
	 */
	public IntPoint lineP2=new IntPoint(0,0);
	public Images images;
	public byte drawAreas=DRAW_AREAS_NO;
	public boolean showTileOutline=false;
	public C c;

	/**
	 * 
	 * the the paint-process will store Street which is nearest to the center
	 * of projection. 
	 */
	public volatile Way actualWay=null;
	public volatile Way nearestRoutableWay=null;
	
	/* variables for searching matching route connections in ways */
	public volatile float searchCon1Lat;
	public volatile float searchCon1Lon;
	public volatile float searchCon2Lat;
	public volatile float searchCon2Lon;
	// results
	public volatile int conWayNameIdx; // used to vaguely identify ways that might contain a solution path for highlighting  
	public volatile short conWayFromAt; // highlight way from this path node #
	public volatile short conWayToAt;   // highlight way to this path node #
	public volatile byte conWayType;   // highlight way to node idx
	public volatile byte conWayRouteFlags; // modifiers like motorway and motorway_link (from description) and roundabout/bridge tunnel (from way) 
	public volatile float conWayDistanceToNext; // distance to next connection when following the route path
	
	/**
	 * the square of distance from center to the nearest point of actualWay
	 */
	public float squareDstToWay;
	/**
	 * the square of distance from center to the nearest point of nearestRoutableWay
	 */
	public float squareDstToRoutableWay;
	/**
	 * the square of distance from center to the nearest point of the route solution path
	 */
	public float squareDstToRoutePath;
	public int dstToRoutePath;
	/**
	 * the actual configuration
	 */	
	public Configuration config;
	
	/**public float actualNodeLat;
	public float actualNodeLon;**/
	
	public PaintContext(Trace tr,Images i) throws Exception{
		super();
		images=i;
		trace=tr;
		state=STATE_READY;
		// TODO Auto-generated constructor stub
	}
	
//	public String boundToString(){
//		return (screenRU + " / " + screenLD);
//	}
	public String toString(){
		return "PC c:" + center + " s:" + scale + " w:" + xSize + " h:" + ySize;
	}
}
