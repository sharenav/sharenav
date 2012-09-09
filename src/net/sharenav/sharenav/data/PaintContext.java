/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

package net.sharenav.sharenav.data;

import javax.microedition.lcdui.Graphics;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.graphics.Images;
import net.sharenav.sharenav.mapdata.Way;
import net.sharenav.sharenav.routing.RouteInstructions;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.util.IntPoint;
import net.sharenav.util.IntTree;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;

//TODO: explain - short overview what the PaintContext does - it seems like it has nothing to do with painting but routing - perhaps the name should be changed
/*
 * For routing:
 * In the Way class the route connections of the route solution path (route line)
 * are matched against the ways around the calculated route connections (in connections2WayMatch())
 * For this purpose the PaintContext contains some variable passed on to the Way Class and also for passing back
 * the results for each connection by/to RouteInstructions.searchConnection2Ways() 
 */
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
	/** 
	 * used to avoid frequent memory allocations. this point have to have
	 * a valid Object after method exit. Point will used as end point of a line.
	 * the calculation go directly to the literals inside the object.
	 */
	public IntPoint tempPoint=new IntPoint(0,0);
	public Images images;
	public byte drawAreas=DRAW_AREAS_NO;
	public boolean showTileOutline=false;

	/**
	 * the the paint-process will store Street which is nearest to the center
	 * of projection. 
	 */
	public volatile Way actualWay=null;
	/** nearest routable way taking penalty into account */ 
	public volatile Way actualRoutableWay=null;
	/** nearest way on the route path taking penalty into account */ 
	public volatile Way actualRoutePathWay=null;

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
	public volatile int conWayFromAt;
	/** a way id of the Way between the from and to-RouteNodes */
	public volatile int conWayCombinedFileAndWayNr;
	/** highlight way to this path node # */
	public volatile int conWayToAt;
	/** highlight way to node idx */
	public volatile short conWayType;
	/** modifiers like motorway and motorway_link (from description) and roundabout/bridge tunnel (from way) */
	public volatile short conWayRouteFlags;
	/** number of routable ways at the connection (result should always be >= 1)*/
	public volatile byte conWayNumToRoutableWays;
	/** bearing at the end of the path leading to this connection */
	public volatile byte conWayStartBearing;
	/** number of possible motorway instructions (enter / leave motorway) this way leads to */
	public volatile byte conWayNumMotorways;
	/** bearing at the beginning of the path leading to the next connection */
	public volatile byte conWayEndBearing;
	/** distance to next connection in meters when following the route path */
	public volatile float conWayDistanceToNext;
	/** maximum (winter) speed in km/h of this connection */
	public volatile short conWayMaxSpeed;
	/** when painting draw highlighted path on top */
	public volatile boolean highlightedPathOnTop;
	/** used to find out if the connection leads to multiple same named ways*/
	public volatile IntTree conWayNameIdxs = new IntTree();

	/** count of bearings used to find out if the connection leads straight-on to multiple named routable ways for giving a bearing instruction*/  
	public volatile int conWayBearingsCount;
	/** used to find out if the connection leads straight-on to multiple routable ways for giving a bearing instruction*/  
	public volatile byte conWayBearings[] = new byte[8];
	/** used to find out if the connection leads straight-on to multiple named routable ways for giving a bearing instruction*/  
	public volatile boolean conWayBearingHasName[] = new boolean[8];
	/** used to find out if the connection leads straight-on to multiple named routable ways for giving a bearing instruction*/  
	public volatile short conWayBearingWayType[] = new short[8];

	/** used to vaguely identify ways that might contain a solution path for highlighting*/
	public volatile int conWayNumNameIdxs;  
	
	/** the square of distance from center to the nearest point of actualWay */
	public float squareDstWithPenToWay;
	/** the square of distance from center to the nearest point of actualRoutableWay */
	public float squareDstWithPenToActualRoutableWay;
	/** the square of distance including penalty from center to the nearest point of the route solution path (=route line)*/
	public float squareDstWithPenToRoutePath;
	/** the square of distance from center to the nearest point of the route solution path (=route line)*/
	public float squareDstToRoutePath;
	/** distance (from the map center) to the nearest point on the route line */
	public int routePathConnection;
	/** the index of the path segment (of the way) where the nearest point on the route line is on */
	public int pathIdxInRoutePathConnection;
	/** the hotspot in pixel */
	public IntPoint hotspot=new IntPoint();
	
	/** ways painted in last paint call */
	public int waysPainted;

	public Node nodeGpsPos = null;
	public boolean bUsedGpsCenter = false;
	public float fNearestSegmentRoutePathLong1;
	public float fNearestSegmentRoutePathLat1;
	public float fNearestSegmentRoutePathLong2;
	public float fNearestSegmentRoutePathLat2;
	public float fNearestSegmentRouteableWayLong1;
	public float fNearestSegmentRouteableWayLat1;
	public float fNearestSegmentRouteableWayLong2;
	public float fNearestSegmentRouteableWayLat2;
	public float fNearestSegmentWayLong1;
	public float fNearestSegmentWayLat1;
	public float fNearestSegmentWayLong2;
	public float fNearestSegmentWayLat2;


	
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
	
	/**
	 * @param squareDst - square of distance in this PaintContext
	 * @return distance in meters 
	 */
	public int getDstFromSquareDst(float squareDst) {
		if (squareDst != Float.MAX_VALUE) {
			if (getP().isOrthogonal()) {
				Node n1 = new Node();
				Node n2 = new Node();
				getP().inverse(0, 0, n1);
				getP().inverse( (int) Math.sqrt(squareDst), 0, n2);
				return (int) ProjMath.getDistance(n1, n2);
			} else {
				// FIXME: this is definitely the wrong way to calculate the distance even for orthogonal projections
				return (int) (Math.sqrt(squareDst)/ppm);
			}
//			Node n1 = new Node();
//			Node n2 = new Node();
//			/* have to use a point at the hotspot because not all projections are ortognoal */
//			/* TODO: find a better way to calc this */
//			getP().inverse(hotspot.x, hotspot.y, n1);
//			getP().inverse( hotspot.x + (int) Math.sqrt(squareDst), hotspot.y, n2);
//			return (int) ProjMath.getDistance(n1, n2);
		} else {
			return RouteInstructions.DISTANCE_UNKNOWN;
		}
	}

	/**
	 * @return distance in meters
	 */
	public int getDstFromRouteSegment() {
		if (this.squareDstToRoutePath == Float.MAX_VALUE) {
			return RouteInstructions.DISTANCE_UNKNOWN;
		}

		if (this.bUsedGpsCenter == true) {
			Node node1 = new Node(this.fNearestSegmentRoutePathLat1, this.fNearestSegmentRoutePathLong1, true);
			Node node2 = new Node(this.fNearestSegmentRoutePathLat2, this.fNearestSegmentRoutePathLong2, true);
			Node nearest = MoreMath.closestPointOnLine(node1, node2, this.nodeGpsPos);
			return MoreMath.dist(nearest.radlat, nearest.radlon, this.nodeGpsPos.radlat, this.nodeGpsPos.radlon);
		} else {
			return (int) (getDstFromSquareDst(this.squareDstToRoutePath));
		}
	}

	/**
	 * @return distance in meters
	 */
	public int getDstFromRouteableWay() {
		if (this.squareDstWithPenToActualRoutableWay == Float.MAX_VALUE) {
			return RouteInstructions.DISTANCE_UNKNOWN;
		}

		if (this.bUsedGpsCenter == true) {
			Node node1 = new Node(this.fNearestSegmentRouteableWayLat1, this.fNearestSegmentRouteableWayLong1, true);
			Node node2 = new Node(this.fNearestSegmentRouteableWayLat2, this.fNearestSegmentRouteableWayLong2, true);
			Node nearest = MoreMath.closestPointOnLine(node1, node2, this.nodeGpsPos);
			return MoreMath.dist(nearest.radlat, nearest.radlon, this.nodeGpsPos.radlat, this.nodeGpsPos.radlon);
		} else {
			return (int) (getDstFromSquareDst(this.squareDstWithPenToActualRoutableWay));
		}
	}

	/**
	 * @return distance in meters
	 */
	public int getDstFromWay() {
		if (this.squareDstWithPenToWay == Float.MAX_VALUE) {
			return RouteInstructions.DISTANCE_UNKNOWN;
		}

		if (this.bUsedGpsCenter == true) {
			Node node1 = new Node(this.fNearestSegmentWayLat1, this.fNearestSegmentWayLong1, true);
			Node node2 = new Node(this.fNearestSegmentWayLat2, this.fNearestSegmentWayLong2, true);
			Node nearest = MoreMath.closestPointOnLine(node1, node2, this.nodeGpsPos);
			return MoreMath.dist(nearest.radlat, nearest.radlon, this.nodeGpsPos.radlat, this.nodeGpsPos.radlon);
		} else {
			return (int) (getDstFromSquareDst(this.squareDstWithPenToWay));
		}
	}

//	public String boundToString(){
//		return (screenRU + " / " + screenLD);
//	}
	public String toString(){
		return "PC c:" + center + " s:" + scale + " w:" + xSize + " h:" + ySize;
	}
}
