/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps.tile;

import java.util.Vector;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.intTree;
import de.ueller.gpsMid.mapData.QueueDataReader;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.midlet.gps.ScreenContext;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Way;

//TODO: explain - short overview what the PaintContext does - it seems like it has nothing to do with painting but routing - perhaps the name should be changed
public class PaintContext extends ScreenContext {
	
	public final static byte DRAW_AREAS_NO=0;
	public final static byte DRAW_AREAS_OUTLINE=1;
	public final static byte DRAW_AREAS_FILL=2;
	public final static byte STATE_IN_CREATE=1;
	public final static byte STATE_IN_PAINT=2;
	public final static byte STATE_READY=0;
	public final static byte STATE_IN_COPY=3;
	
	public volatile byte state=0;
	
	/** layers containing highlighted path */
	public byte hlLayers=0;
	
	public Graphics g;
	/** 
	 * used to avoid frequent memory allocations. this point have to have
	 * a valid object after method exit 
	 */
	public IntPoint swapLineP=new IntPoint(0,0);
	/** 
	 * used to avoid frequent memory allocations. this point have to have
	 * null after method exit. Point will used as startpoint of a line to
	 * indicate the fact that there is no startpoint at the begin of painting,
	 * this points to null 
	 */
	public IntPoint lineP1=null;
	/** 
	 * used to avoid frequent memory allocations. this point have to have
	 * a valid Object after method exit. Point will used as end point of a line.
	 * the calculation go directly to the literals inside the object.
	 */
	public IntPoint lineP2=new IntPoint(0,0);
	public Images images;
	public byte drawAreas=DRAW_AREAS_NO;
	public boolean showTileOutline=false;
	public C c;

	/**
	 * the the paint-process will store Street which is nearest to the center
	 * of projection. 
	 */
	public volatile Way actualWay=null;
	public volatile Way nearestRoutableWay=null;
	public volatile SingleTile actualSingleTile = null;
	
	/* variables for searching matching route connections in ways */
	public volatile float searchCon1Lat;
	public volatile float searchCon1Lon;
	public volatile float searchCon2Lat;
	public volatile float searchCon2Lon;
	public volatile short searchConPrevWayRouteFlags; // Way route flags of previous connection
	// results
	/** used to vaguely identify ways that might contain a solution path for highlighting */
	public volatile int conWayNameIdx;
	/** highlight way from this path node # */
	public volatile short conWayFromAt;
	/** highlight way to this path node # */
	public volatile short conWayToAt;
	/** highlight way to node idx */
	public volatile byte conWayType;
	/** modifiers like motorway and motorway_link (from description) and roundabout/bridge tunnel (from way) */
	public volatile short conWayRouteFlags;
	/** number of routable ways at the connection (result should always be >= 1)*/
	public volatile byte conWayNumToRoutableWays;
	/** bearing at the end of the path leading to this connection */
	public volatile byte conWayStartBearing;
	//TODO: explain what are "motorway instructions" in this context? is there a better word for it
	/** number of possible motorway instructions this way leads to */
	public volatile byte conWayNumMotorways;
	/** bearing at the beginning of the path leading to the next connection */
	public volatile byte conWayEndBearing;
	//TODO: explain - which measuring unit
	/** distance to next connection when following the route path */
	public volatile float conWayDistanceToNext;
	/** when painting draw highlighted path on top */
	public volatile boolean highlightedPathOnTop;
	/** used to find out if the connection leads to multiple same named ways*/
	public volatile intTree conWayNameIdxs = new intTree();
	/** used to find out if the connection leads straight-on to multiple routable ways for giving a bearing instruction*/  
	public volatile Vector conWayBearings = new Vector(8);
	/** used to vaguely identify ways that might contain a solution path for highlighting*/
	public volatile int conWayNumNameIdxs;  
	
	/** the square of distance from center to the nearest point of actualWay */
	public float squareDstToWay;
	/** the square of distance from center to the nearest point of nearestRoutableWay */
	public float squareDstToRoutableWay;
	//TODO: explain - what is the route solution path
	/** the square of distance from center to the nearest point of the route solution path */
	public float squareDstToRoutePath;
//	TODO: explain
	public int dstToRoutePath;
//	TODO: explain
	public int routePathConnection;
//	TODO: explain
	public int pathIdxInRoutePathConnection;
	
	/**
	 * the actual configuration
	 */	
	public Configuration config;
	
	/**public float actualNodeLat;
	public float actualNodeLon;**/
	
	/**
	 * @param tr Reference to the main Trace screen
	 * @param i Reference to the Images class
	 */
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
