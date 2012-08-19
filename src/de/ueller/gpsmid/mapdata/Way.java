/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.mapdata;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import net.sourceforge.jmicropolygon.PolygonGraphics;
import de.ueller.gps.Node;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.data.RoutePositionMark;
import de.ueller.gpsmid.graphics.Projection;
import de.ueller.gpsmid.routing.Connection;
import de.ueller.gpsmid.routing.ConnectionWithNode;
import de.ueller.gpsmid.routing.RouteInstructions;
import de.ueller.gpsmid.routing.RouteLineProducer;
import de.ueller.gpsmid.routing.TravelMode;
import de.ueller.gpsmid.tile.SingleTile;
import de.ueller.gpsmid.tile.Tile;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.midlet.graphics.FilledTriangle;
import de.ueller.util.IntPoint;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import de.ueller.util.ProjMath;

//#if polish.android
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
//#endif
/**
 * handle Ways and arrays. Be careful, all paint parts useing static vars so painting
 *  is NOT thread save, because of Imagecollector is a single Thread which is the only one that
 *  uses painting of obects, its assured that one Way will be painted after the other.
 * @author hmu
 *
 */

/* Questions:
 * - What classes are involved for getting the necessary Way data from the Jar?
 *   QueueDataReader read the whole tile, and calls this constructor to read the way details
 * - Which region is covered by a SingleTile, does it always contain all Nodes of the complete way?
 *   Yes/No ;-) from the GpsMid data perspective a way is complete within a tile.
 *   from OSM Perspective ways are splited into two ways if they are to large 
 * - Where are the way nodes combined if a tile was split in Osm2GpsMid?
 *   in case of a split way, we have one part in one tile and a other part in an other tile.
 *   so the endpoint of one way is the same as the start point of the other way. In this case
 *   both tiles contains its own copy of this Node.
 */
public class Way extends Entity {
	
	public static final byte WAY_FLAG_NAME = 1;
	public static final byte WAY_FLAG_MAXSPEED = 2;
	public static final byte WAY_FLAG_LAYER = 4;
	public static final byte WAY_FLAG_RESERVED_FLAG = 8;
	public static final byte WAY_FLAG_ONEWAY = 16;	
//	public static final byte WAY_FLAG_MULTIPATH = 4;	
	public static final byte WAY_FLAG_NAMEHIGH = 32;
	public static final byte WAY_FLAG_AREA = 64;
//	public static final byte WAY_FLAG_ISINHIGH = 64;	
	public static final int WAY_FLAG_ADDITIONALFLAG = 128;

	public static final byte WAY_FLAG2_ROUNDABOUT = 1;
	public static final byte WAY_FLAG2_TUNNEL = 2;
	public static final byte WAY_FLAG2_BRIDGE = 4;
	public static final byte WAY_FLAG2_CYCLE_OPPOSITE = 8;
	public static final byte WAY_FLAG2_LONGWAY = 16;
	public static final byte WAY_FLAG2_MAXSPEED_WINTER = 32;
	/** http://wiki.openstreetmap.org/wiki/WikiProject_Haiti */
	public static final byte WAY_FLAG2_DAMAGED = 64;
	public static final int WAY_FLAG2_ADDITIONALFLAG = 128;
	
	public static final byte WAY_FLAG3_URL = 1;
	public static final byte WAY_FLAG3_URLHIGH = 2;
	public static final byte WAY_FLAG3_PHONE = 4;
	public static final byte WAY_FLAG3_PHONEHIGH = 8;
	public static final byte WAY_FLAG3_NAMEASFORAREA = 16;
	public static final byte WAY_FLAG3_HAS_HOUSENUMBERS = 32;
	public static final byte WAY_FLAG3_LONGHOUSENUMBERS = 64;
	public static final int WAY_FLAG3_ADDITIONALFLAG = 128;

	public static final byte WAY_FLAG4_ALERT = 1;
	public static final byte WAY_FLAG4_CLICKABLE = 2;
	public static final byte WAY_FLAG4_HOLES = 4;
	public static final byte WAY_FLAG4_TRIANGLES_IN_OUTLINE_MODE = 8;

	public static final byte DRAW_BORDER=1;
	public static final byte DRAW_AREA=2;
	public static final byte DRAW_FULL=3;
	
	private static final int MaxSpeedMask = 0xff;
	private static final int MaxSpeedShift = 0;
	//private static final int ModMask = 0xff00;
	private static final int ModShift = 8;
	
	public static final int WAY_ONEWAY = 1 << ModShift;
	public static final int WAY_AREA = 2 << ModShift;
	public static final int WAY_ROUNDABOUT = 4 << ModShift;
	public static final int WAY_TUNNEL = 8 << ModShift;
	public static final int WAY_BRIDGE = 16 << ModShift;
	public static final int WAY_CYCLE_OPPOSITE = 32 << ModShift;
	/** http://wiki.openstreetmap.org/wiki/WikiProject_Haiti */
	public static final int WAY_DAMAGED = 64 << ModShift;
	public static final int WAY_NAMEASFORAREA = 128 << ModShift;
	public static final int WAY_RATHER_BIG = 256 << ModShift;
	public static final int WAY_EVEN_BIGGER = 512 << ModShift;
	public static final int WAY_TRIANGLES_IN_OUTLINE_MODE = 1024 << ModShift;

	public static final byte PAINTMODE_COUNTFITTINGCHARS = 0;
	public static final byte PAINTMODE_DRAWCHARS = 1;
	public static final byte INDENT_PATHNAME = 2;

	private static final int PATHSEG_DO_NOT_HIGHLIGHT = -1;
	private static final int PATHSEG_DO_NOT_DRAW = -2;
	
	private static final int HIGHLIGHT_NONE = 0;
	private static final int HIGHLIGHT_DEST = 1;
	private static final int HIGHLIGHT_ROUTEPATH_CONTAINED = 2;
	
	protected static final Logger logger = Logger.getInstance(Way.class, Logger.TRACE);

	public int flags = 0;
	private int flagswinter = 0;

	/** indicate by which route modes this way can be used (motorcar, bicycle, etc.)
	 * Each bit represents one of the route modes in the midlet, with bit 0 being the first route mode.
	 * The routeability of the way in each route mode is determined by Osm2GpsMid,
	 * which derives it from the accessible flag of the wValue of way type's description
	 * and the routeAccess specifications in the style file 
	 * Higher bits (bit 4 to 7 are used for the same flags like in Connection)
	 */
	private byte wayRouteModes = 0;

	public short[] path;
	public short[][] holes;
	public short minLat;
	public short minLon;
	public short maxLat;
	public short maxLon;
	
	public short wayNrInFile = 0;
	public int nameIdx = -1;
	public int urlIdx = -1;
	public int phoneIdx = -1;
	
	/**
	 * This is a buffer for the drawing routines
	 * so that we don't have to allocate new
	 * memory at each time we draw a way. This
	 * saves some time on memory management
	 * too.
	 * 
	 * This makes this function thread unsafe,
	 * so make sure we only have a single thread
	 * drawing a way at a time
	 */
	private static int [] x = new int[100];
	private static int [] y = new int[100];
	/**
	 * contains either PATHSEG_DO_NOT_HIGHLIGHT, PATHSEG_DO_NOT_DRAW
	 * or if this way's segment is part of the route path,
	 * the index of the corresponding ConnectionWithNode in the route 
	 */
	private static int [] hl = new int[100];
	private static Font areaFont;
	private static int areaFontHeight;
	private static int areaFontMinRenderWidth;
	private static Font pathFont;
	private static int pathFontHeight;
	private static int pathFontMaxCharWidth;
	private static int pathFontBaseLinePos;
	
	
	/** Point Line 1 begin; note: Line 1 and Line 2 form a waysegment (read as Line1Begin) */
	static final IntPoint l1b = new IntPoint();
	/** Point Line 1 begin; note: Line 1 and Line 2 form a waysegment (read as Line1End) */
	static final IntPoint l1e = new IntPoint();
	/** Point Line 2 begin; note: Line 1 and Line 2 form a waysegment (read as Line2Begin) */
	static final IntPoint l2b = new IntPoint();
	/** Point Line 2 begin; note: Line 1 and Line 2 form a waysegment (read as Line2End) */
	static final IntPoint l2e = new IntPoint();
	/** Point Line 3 begin; note: Line 3 and Line 4 form a waysegment (read as Line3Begin) */
	static final IntPoint l3b = new IntPoint();
	/** Point Line 3 begin; note: Line 3 and Line 4 form a waysegment (read as Line3End) */
	static final IntPoint l3e = new IntPoint();
	/** Point Line 4 begin; note: Line 3 and Line 4 form a waysegment (read as Line4Begin) */
	static final IntPoint l4b = new IntPoint();
	/** Point Line 5 begin; note: Line 3 and Line 4 form a waysegment (read as Line4End) */
	static final IntPoint l4e = new IntPoint();
	
	/** stores the intersection-point of the inner turn	 */
	static final IntPoint intersecP = new IntPoint();
	
	/** Enables or disables the match travelling direction for actual way calculation heuristic */
	private static boolean addDirectionalPenalty = false;
	
	/** Sets the scale of the directional penalty dependent on the projection used.	 */
	private static float scalePen = 0;
	/*
	 * Precalculated normalised vector for the direction of current travel,
	 * used in the the directional penalty heuristic
	 */
	private static float courseVecX = 0;
	private static float courseVecY = 0;
	
	private static WaySegment waySegment = new WaySegment();

	public static float fSegmentLong1;
	public static float fSegmentLat1;
	public static float fSegmentLong2;
	public static float fSegmentLat2;

//	private final static Logger logger = Logger.getInstance(Way.class,
//			Logger.TRACE);
	
	public Way() {
		super();
	}

	/**
	 * The flag should be read by caller. If FLAGS == 128 this is a dummy way
	 * and can be ignored.
	 * 
	 * @param is Tile inputstream
	 * @param f flags this is read by the caller
	 * @param t Tile
	 * @param layers: this is somewhat awkward. We need to get the layer information back out to 
	 * 			the caller, so use a kind of call by reference
	 * @param idx index into the layers array where to store the layer info.
	 * @param nodes
	 * @throws IOException
	 */
	public Way(DataInputStream is, byte f, Tile t, byte[] layers, int idx, boolean outlineFlag) throws IOException {

		minLat = is.readShort();
		minLon = is.readShort();
		maxLat = is.readShort();
		maxLon = is.readShort();
		
		//#if polish.api.bigstyles
		if (Legend.enableBigStyles) {
			type = is.readShort();
		} else {
			type = (short) (is.readByte() & 0xff);
		}
		//#else
		type = is.readByte();
		//#endif
		setWayRouteModes(is.readByte());	
		
		if ((f & WAY_FLAG_NAME) == WAY_FLAG_NAME) {
			if ((f & WAY_FLAG_NAMEHIGH) == WAY_FLAG_NAMEHIGH) {
				nameIdx = is.readInt();
				//System.out.println("Name_High " + f );
			} else {
				nameIdx = is.readShort();
			}
		}
		if ((f & WAY_FLAG_MAXSPEED) == WAY_FLAG_MAXSPEED) {
//			logger.debug("read maxspeed");
			flags = is.readByte() & 0xff; // apply an 8 bit mask to the maxspeed byte read so values >127 won't result in a negative integer with wrong bits set for the flags
		}
		
		/* calculate diameter of the rectangle around the way to see if this is a rather big way that is worth to be rendered in lower zoom levels
		 * (used coordinates do not represent exactly the real ones but should be close enough for calculating the approximate diameter)
		*/ 
		float diameter = ProjMath.getDistance(minLat * MoreMath.FIXPT_MULT_INV + t.centerLat, minLon * MoreMath.FIXPT_MULT_INV + t.centerLon, maxLat * MoreMath.FIXPT_MULT_INV + t.centerLat, maxLon * MoreMath.FIXPT_MULT_INV + t.centerLon);
		if (diameter > 500) {
			flags |= WAY_RATHER_BIG;
		}
		if (diameter > 5000) {
			flags |= WAY_EVEN_BIGGER;
		}
		
		byte f2=0;
		byte f3=0;
		byte f4=0;
		if ( (f & WAY_FLAG_ADDITIONALFLAG) > 0 ) {
			f2 = is.readByte();
			if ( (f2 & WAY_FLAG2_ROUNDABOUT) > 0 ) {
				flags += WAY_ROUNDABOUT;
			}
			if ( (f2 & WAY_FLAG2_TUNNEL) > 0 ) {
				flags += WAY_TUNNEL;
			}
			if ( (f2 & WAY_FLAG2_BRIDGE) > 0 ) {
				flags += WAY_BRIDGE;
			}
			if ( (f2 & WAY_FLAG2_DAMAGED) > 0 ) {
				flags += WAY_DAMAGED;
			}
			if ( (f2 & WAY_FLAG2_CYCLE_OPPOSITE) > 0 ) {
				flags += WAY_CYCLE_OPPOSITE;
			}
			if ( (f2 & WAY_FLAG2_ADDITIONALFLAG) == WAY_FLAG2_ADDITIONALFLAG ) {
				f3 = is.readByte();
				if ( (f3 & WAY_FLAG3_ADDITIONALFLAG) == WAY_FLAG3_ADDITIONALFLAG ) {
					f4 = is.readByte();
				}
				if ( (f3 & WAY_FLAG3_NAMEASFORAREA) > 0) {
					flags += WAY_NAMEASFORAREA;
				}

				if ( (f3 & WAY_FLAG3_URL) > 0 ) {
					if ( (f3 & WAY_FLAG3_URLHIGH) > 0 ) {
						urlIdx = is.readInt();
					} else {
						urlIdx = is.readShort();
					}
				}
				if ( (f3 & WAY_FLAG3_PHONE) > 0 ) {
					if ( (f3 & WAY_FLAG3_PHONEHIGH) > 0 ) {
						phoneIdx = is.readInt();
					} else {
						phoneIdx = is.readShort();
					}
				}
			}
			if ((f2 & WAY_FLAG2_MAXSPEED_WINTER) == WAY_FLAG2_MAXSPEED_WINTER) {
				setFlagswinter(is.readByte());
			}
		}
		layers[idx] = 0;
		if ((f & WAY_FLAG_LAYER) == WAY_FLAG_LAYER) {
			/**
			 * TODO: We are currently ignoring the layer info
			 * Please implement proper support for this when rendering
			 */
			layers[idx] = is.readByte();
		}
		if ( (f3 & WAY_FLAG3_HAS_HOUSENUMBERS) > 0 ) {
			int hcount;
			// Ignore the data for now
			if ( (f3 & WAY_FLAG3_LONGHOUSENUMBERS) > 0 ) {
				hcount = is.readShort();
				if (hcount < 0) {
					hcount += 65536;
				}
			} else {
				hcount = is.readByte();
				if (hcount < 0) {
					hcount += 256;
				}
			}
			logger.debug("expecting " + hcount + " housenumber nodes");
			for (int i = 0; i < hcount; i++) {
				is.readLong();
			}
		}

		if ((f & WAY_FLAG_ONEWAY) == WAY_FLAG_ONEWAY) {
			flags += WAY_ONEWAY;
		}
		if ((f & WAY_FLAG_AREA) > 0){
			flags += WAY_AREA;
			logger.debug("Area = true");
		}
//		if (((f & WAY_FLAG_AREA) == WAY_FLAG_AREA) || Legend.getWayDescription(type).isArea) {
//			if ((f & WAY_FLAG_AREA) == WAY_FLAG_AREA) {
//				//#debug debug
//				logger.debug("Loading explicit Area: " + this);
//			}
//			flags += WAY_AREA;
//		}

		boolean longWays = false;
		if ((f2 & WAY_FLAG2_LONGWAY) > 0) {
			longWays = true;
			logger.debug("longway = true");
		}
		int count;
		if (longWays) {
			count = is.readShort();
			if (count < 0) {
				count += 65536;
			}
		} else {
			count = is.readByte();
			if (count < 0) {
				count += 256;
			}

		}
		path = new short[count];
		logger.debug("expecting " + count + " nodes");
		for (int i = 0; i < count; i++) {
			path[i] = is.readShort();
		}
		if ((f4 & WAY_FLAG4_TRIANGLES_IN_OUTLINE_MODE) == WAY_FLAG4_TRIANGLES_IN_OUTLINE_MODE) {
			flags += WAY_TRIANGLES_IN_OUTLINE_MODE;
		}

		if ((f4 & WAY_FLAG4_HOLES) > 0 && (f & WAY_FLAG_AREA) > 0) {
			int holeCount = is.readShort();
			if (holeCount < 0) {
				holeCount += 65536;
			}
			holes = new short[holeCount][];
			for (int i = 0; i < holeCount; i++) {
				count = is.readShort();
				if (count < 0) {
					count += 65536;
				}
				holes[i] = new short[count];
				for (int j = 0; j < count; j++) {
					// ignore holes for now
					holes[i][j] = is.readShort();
				}
			}
		}
	}
	
	private void readVerify(byte expect,String msg,DataInputStream is){
		try {
			byte next=is.readByte();
			if (next == expect) {
				logger.debug("Verify Way " + expect + " OK" +msg);
				return;
			}
			logger.debug("Error while verify Way " + msg + " expect " + expect + " got " + next);
			for (int l=0; l<10 ; l++){
				logger.debug(" " + is.readByte());
			}
			System.exit(-1);
		} catch (IOException e) {
			logger.error(Locale.get("way.ErrorWhileVerify")/*Error while verify */ + msg + " " + e.getMessage());
		}
	}
	
	public boolean isRoutableWay() {
		return (getWayRouteModes() & Configuration.getTravelMask()) != 0;
	}
	
	/**
	 * Precalculate parameters to quickly test the the directional overlap between
	 * a way and the current direction of travelling. This is used in processWay
	 * to calculate the most likely way we are currently travelling on.
	 * 
	 * @param pc
	 * @param speed
	 */
	public static void setupDirectionalPenalty(PaintContext pc, int speed, boolean gpsRecenter) {
		//Only use this heuristic if we are travelling faster than 6 km/h, as otherwise
		//the direction estimate is too bad to be of much use. Also, only use the direction
		//if we are actually using the gps to center the map and not manually panning around.

		pc.bUsedGpsCenter = gpsRecenter;
		pc.nodeGpsPos  = new Node(pc.trace.getGpsLat(), pc.trace.getGpsLon());


		if ((speed < 7) || !gpsRecenter) {
			addDirectionalPenalty = false;
			scalePen = 0;
			return;
		}

			addDirectionalPenalty = true;
		
			// 100m max penalty at equator.
			scalePen = MoreMath.RADIANT_PER_METER * 100f;

			Projection p = pc.getP();
			float lat1 = pc.center.radlat;
			float lon1 = pc.center.radlon;
			//IntPoint lineP1 = new IntPoint();
			//IntPoint lineP2 = new IntPoint();
			float lat2 = pc.center.radlat + (float) (0.00001 * Math.cos(pc.course * MoreMath.FAC_DECTORAD));
			float lon2 = pc.center.radlon + (float) (0.00001 * Math.sin(pc.course * MoreMath.FAC_DECTORAD));
			//p.forward(lat1, lon1, lineP1);
			//p.forward(lat2, lon2, lineP2);

			//courseVecX = lineP1.x - lineP2.x;
			//courseVecY = lineP1.y - lineP2.y;
			courseVecX = lon1 - lon2;
			courseVecY = lat1 - lat2;
			float norm = (float)Math.sqrt(courseVecX * courseVecX + courseVecY * courseVecY);
			courseVecX /= norm;
			courseVecY /= norm;

/*
			bUsedGpsCenter = false;
			addDirectionalPenalty = true;
			Projection p = pc.getP();
			float lat1 = pc.center.radlat;
			float lon1 = pc.center.radlon;
			IntPoint lineP1 = new IntPoint();
			IntPoint lineP2 = new IntPoint();
			float lat2 = pc.center.radlat + (float) (0.00001 * Math.cos(pc.course * MoreMath.FAC_DECTORAD));
			float lon2 = pc.center.radlon + (float) (0.00001 * Math.sin(pc.course * MoreMath.FAC_DECTORAD));
			p.forward(lat1, lon1, lineP1);
			p.forward(lat2, lon2, lineP2);

			courseVecX = lineP1.x - lineP2.x;
			courseVecY = lineP1.y - lineP2.y;
			float norm = (float)Math.sqrt(courseVecX * courseVecX + courseVecY * courseVecY);
			courseVecX /= norm; courseVecY /= norm;

			//Calculate the lat and lon coordinates of two
			//points that are 35 pixels apart
			Node n1 = new Node();
			Node n2 = new Node();
			pc.getP().inverse(10, 10, n1);
			pc.getP().inverse(45, 10, n2);

			//Calculate the distance between them in meters
			float d = ProjMath.getDistance(n1, n2);
			//Set the scale of the direction penalty to roughly
			//match that of a penalty of 100m at a 90 degree angle
			scalePen = 35.0f / d * 100.0f;
		*/
	}

	public boolean isOnScreen( short pcLDlat, short pcLDlon, short pcRUlat, short pcRUlon) { 
		if ((maxLat < pcLDlat) || (minLat > pcRUlat)) {
			return false;
		}
		if ((maxLon < pcLDlon) || (minLon > pcRUlon)) {
			return false;
		}
		return true; 
	}
	
	public float wayBearing(SingleTile t) {
		return (float) MoreMath.bearing_int(
				t.nodeLat[path[0]] * MoreMath.FIXPT_MULT_INV + t.centerLat,
				t.nodeLon[path[0]] * MoreMath.FIXPT_MULT_INV + t.centerLon, 
				t.nodeLat[path[path.length - 1]] * MoreMath.FIXPT_MULT_INV + t.centerLat,
				t.nodeLon[path[path.length - 1]] * MoreMath.FIXPT_MULT_INV + t.centerLon);
	}

	public void paintAsPath(PaintContext pc, SingleTile t, byte layer) {
		processPath(pc,t,Tile.OPT_PAINT, layer);
	}

	public void paintHighlightPath(PaintContext pc, SingleTile t, byte layer) {
		processPath(pc,t,Tile.OPT_PAINT | Tile.OPT_HIGHLIGHT, layer);
	}

	private void changeCountNameIdx(PaintContext pc, int diff) {
		if (nameIdx >= 0) {
			Integer oCount = (Integer) pc.conWayNameIdxs.get(nameIdx);
			int nCount = 0;
			if (oCount != null) {
				nCount = oCount.intValue();
			}
			pc.conWayNameIdxs.put(nameIdx, new Integer(nCount + diff) );
		}
	}

	/* check if the way contains the nodes searchCon1 and searchCon2
	* if it does, but we already have a matching way,
	* only take this way if it has the shortest path from searchCon1 to searchCon2
	**/
	public void connections2WayMatch(PaintContext pc, SingleTile t) {
		boolean containsCon1 = false;
		boolean containsCon2 = false;
		int containsCon1At = 0;
		int containsCon2At = 0;
		short searchCon1Lat = (short) ((pc.searchCon1Lat - t.centerLat) * MoreMath.FIXPT_MULT);
		short searchCon1Lon = (short) ((pc.searchCon1Lon - t.centerLon) * MoreMath.FIXPT_MULT);
		short searchCon2Lat = (short) ((pc.searchCon2Lat - t.centerLat) * MoreMath.FIXPT_MULT);
		short searchCon2Lon = (short) ((pc.searchCon2Lon - t.centerLon) * MoreMath.FIXPT_MULT);
		
		boolean isCircleway = isCircleway();
		byte bearingForward = 0;
		byte bearingBackward = 0;
		// check if way contains both search connections
		// System.out.println("search path nodes: " + path.length);
		for (int i = 0; i < path.length; i++) {
			int idx = path[i];
			// System.out.println("lat:" + t.nodeLat[idx] + "/" + searchCon1Lat);
			if (idx < 0) {
				idx += 65536;
			}
			if ( (Math.abs(t.nodeLat[idx] - searchCon1Lat) < 2)
					&&
				 (Math.abs(t.nodeLon[idx] - searchCon1Lon) < 2)
			) {
				if (
					this.isRoutableWay()
					// count in roundabouts only once (search connection could match at start and end node)
					&& !containsCon1
					// count only if it's not a oneway ending at this connection (and no against oneway rule applies)
					&& !(isOneDirectionOnly() && (i == path.length - 1 && !isCircleway) )
				) {
					// remember bearings of the ways at this connection, so we can later check out if we have multiple straight-ons requiring a bearing instruction
					// we do this twice for each found way to add the bearings for forward and backward
					for (int d = -1 ; d <= 1; d += 2) {
						// do not add bearings against direction if this is a oneway (and no against oneway rule applies)
						if (d == -1 && isOneDirectionOnly()) {
							continue;
						}
						if ( (i + d) < path.length && (i + d) >= 0) {
							short rfCurr = (short) Legend.getWayDescription(this.type).routeFlags;
							// count bearings for entering / leaving the motorway. We don't need to give bearing instructions if there's only one motorway alternative at the connection
							if (RouteInstructions.isEnterMotorway(pc.searchConPrevWayRouteFlags, rfCurr)
								||
								RouteInstructions.isLeaveMotorway(pc.searchConPrevWayRouteFlags, rfCurr)
							) {
								pc.conWayNumMotorways++;
							}							
							if (pc.conWayBearingsCount < 8) {
								pc.conWayBearingHasName[pc.conWayBearingsCount] = (nameIdx >= 0);
								pc.conWayBearingWayType[pc.conWayBearingsCount] = this.type;
								int idxC = path[i + d];
								if (idxC < 0) {
									idxC += 65536;
								}
								/* if the route node and the next node on the way are very close together, use one node later on the way for the bearing calculation:
								 * this especially prevents issues with almost identical coordinates from the start point to the next node on the way like
								 * http://www.openstreetmap.org/browse/node/429201917 and http://www.openstreetmap.org/browse/node/21042290
								 * (as we can't distinguish them exactly enough with fix point coordinates)
								 */
								int iAfterNext = i + d + d; 
								if (
										MoreMath.dist(
											pc.searchCon1Lat,
											pc.searchCon1Lon,
											t.centerLat + t.nodeLat[idxC] * MoreMath.FIXPT_MULT_INV,
											t.centerLon + t.nodeLon[idxC] * MoreMath.FIXPT_MULT_INV
										)
										< 3
										&&
										// FIXME: It might even make sense to take the next way into account if there's no next node on this way
										iAfterNext < path.length
										&&
										iAfterNext >= 0)
								{
									idxC = path [iAfterNext];
									if (idxC < 0) {
										idxC += 65536;
									}
									//System.out.println("EXAMINE AFTER NEXT");
								}
								
								pc.conWayBearings[pc.conWayBearingsCount] =
									MoreMath.bearing_start(
										(pc.searchCon1Lat),
										(pc.searchCon1Lon),
										(t.centerLat + t.nodeLat[idxC] * MoreMath.FIXPT_MULT_INV),
										(t.centerLon + t.nodeLon[idxC] * MoreMath.FIXPT_MULT_INV)
									);
								if (d == 1) {
									bearingForward = pc.conWayBearings[pc.conWayBearingsCount];
								} else {
									bearingBackward = pc.conWayBearings[pc.conWayBearingsCount];									
								}
//								System.out.println("Bearing: " + pc.conWayBearings[pc.conWayBearingsCount] + new Node(t.centerLat+ t.nodeLat[idxC] * MoreMath.FIXPT_MULT_INV, t.centerLon + t.nodeLon[idxC] * MoreMath.FIXPT_MULT_INV, true).toString());
								pc.conWayBearingsCount++;
							} else {
//								System.out.println("Bearing count is > 8");
							}
						}
					}
					// remember nameIdx's leading away from the connection, so we can later on check if multiple ways lead to the same street name
					changeCountNameIdx(pc, 1);						
					//System.out.println("add 1 " + "con1At: " + i + " pathlen - 1: " + (path.length - 1) );
					pc.conWayNumToRoutableWays++;
					// if we are in the middle of the way, count the way once more (if no against oneway rule applies)
					if (i != 0 && i != path.length - 1 && !isOneDirectionOnly()) {
						pc.conWayNumToRoutableWays++;
						changeCountNameIdx(pc, 1);
						//System.out.println("add middle 1 " + "con1At: " + i + " pathlen - 1: " + (path.length - 1) );
					}			
				}
				containsCon1 = true;
				containsCon1At = i;
				
				// System.out.println("con1 match");
				if (containsCon1 && containsCon2) {
					break;
				}
			}
			if ( (Math.abs(t.nodeLat[idx] - searchCon2Lat) < 2)
					&&
				 (Math.abs(t.nodeLon[idx] - searchCon2Lon) < 2)
				) {
				containsCon2 = true;
				containsCon2At = i;
				// System.out.println("con2 match");
				if (containsCon1 && containsCon2) {
					break;
				}
			}   
		}
	  
		// we've got a match
		if (containsCon1 && containsCon2) {
			WayDescription wayDesc = Legend.getWayDescription(this.type);
			float conWayRealDistance = 0;
			int from = containsCon1At;
			int to = containsCon2At;
			int direction = 1;
			if (from > to) {
				/* 
				 * if from > to and this is a oneway but no circleway it can't be
				 * the route path as we would go against a oneway's direction
				 * (if no against oneway rule applies)
				 * 
				 * Therefore return because this is no correct match.
				 */
				if (isOneDirectionOnly() && (!isCircleway)) {
					return;
				}
				// swap direction
				from = to;
				to = containsCon1At;
				direction = -1;
			}
			int idx1 = path[from];
			if (idx1 < 0) {
				idx1 += 65536;
			}
			int idx2;

			// sum up the distance of the segments between searchCon1 and searchCon2
			for (int i = from; i != to; i++) {
				if ( (isRoundAbout() || isCircleway) && i >= (path.length - 2))  {
					i=-1; // if in roundabout at end of path continue at first node
					if (to == (path.length - 1) ) {
						break;
					}
				}
				idx2 = path[i+1];
				if (idx2 < 0) {
					idx2 += 65536;
				}
				float dist = ProjMath.getDistance(
						(t.centerLat + t.nodeLat[idx1] * MoreMath.FIXPT_MULT_INV),
						(t.centerLon + t.nodeLon[idx1] * MoreMath.FIXPT_MULT_INV),
						(t.centerLat + t.nodeLat[idx2] * MoreMath.FIXPT_MULT_INV),
						(t.centerLon + t.nodeLon[idx2] * MoreMath.FIXPT_MULT_INV));
				conWayRealDistance += dist;
				idx1 = idx2;
			}
			
			/* check if this is a better match than a maybe previous one:
			if the travelling time is shorter than the already matching one
			this way contains a better path between the connections
			*/
			int maxspeed = (short) getMaxSpeed();
			//if (maxspeed == 0) {
			//	maxspeed = Legend.getWayMaxSpeed(this, Configuration.getTravelModeNr());
			//}
			// FIXME is this good? Winter speed handling might be inconsistent
			if (getMaxSpeedWinter() != 0 && Configuration.getCfgBitState(Configuration.CFGBIT_MAXSPEED_WINTER)) {
				maxspeed = (short) getMaxSpeedWinter();
			}
			//System.out.println("maxspeed: " + maxspeed);
			//System.out.println("pc.conWayMaxSpeed: " + pc.conWayMaxSpeed);
			//System.out.println("conWayRealDistance: " + conWayRealDistance);
			//System.out.println("pc.conWayDistanceToNext: " + pc.conWayDistanceToNext);
			// FIXME how to best take variable speed into account?
			if (
				/* check if way is routable
				 * (there are situations where 2 ways have the same nodes, e.g. a tram and a highway)
				*/
				this.isRoutableWay() &&
				conWayRealDistance / (maxspeed == 0 ? 1 : maxspeed)
				< pc.conWayDistanceToNext / (maxspeed == 0 ? 1 : pc.conWayMaxSpeed)
			) {
//				if (pc.conWayDistanceToNext != Float.MAX_VALUE) {
//					String name1=null, name2=null;
//					if (pc.conWayNameIdx != -1) name1=Trace.getInstance().getName(pc.conWayNameIdx);
//					if (this.nameIdx != -1) name2=Trace.getInstance().getName(this.nameIdx);
//					System.out.println("REPLACE " + pc.conWayDistanceToNext + "m (" + Legend.getWayDescription(pc.conWayType).description + " " + (name1==null?"":name1) + ")");
//					System.out.println("WITH " + conWayRealDistance + "m (" + Legend.getWayDescription(this.type).description + " " + (name2==null?"":name2) + ")");
//				}
				// this is currently the best path between searchCon1 and searchCon2
				pc.conWayDistanceToNext = conWayRealDistance;
				pc.conWayMaxSpeed = (short) maxspeed;
				if (pc.conWayMaxSpeed == Legend.MAXSPEED_MARKER_NONE) {
					pc.conWayMaxSpeed = (short) Legend.MAXSPEED_ESTIMATE_NONE;
				} else if (pc.conWayMaxSpeed == Legend.MAXSPEED_MARKER_VARIABLE) {
					pc.conWayMaxSpeed = (short) Legend.MAXSPEED_ESTIMATE_VARIABLE;
				}
				pc.conWayCombinedFileAndWayNr = getWayId(t);
				pc.conWayFromAt = containsCon1At;
				pc.conWayToAt = containsCon2At;
				pc.conWayNameIdx= this.nameIdx;
				pc.conWayType = this.type;
				short routeFlags = (short) wayDesc.routeFlags; 
				if (isRoundAbout()) {
					routeFlags += Legend.ROUTE_FLAG_ROUNDABOUT;
				}
				if (isTunnel()) {
					routeFlags += Legend.ROUTE_FLAG_TUNNEL;
				}
				if (isBridge()) {
					routeFlags += Legend.ROUTE_FLAG_BRIDGE;
				}
				if (isTollRoad()) {
					routeFlags += Legend.ROUTE_FLAG_TOLLROAD;
				}
				if (isOneDirectionOnly()) {
					routeFlags += Legend.ROUTE_FLAG_ONEDIRECTION_ONLY;
				}
				pc.conWayRouteFlags = routeFlags;
				
				// substract way we are coming from from turn options with same name
				changeCountNameIdx(pc, -1);
				//System.out.println("sub 1");
				
				// calculate bearings
				if ( (direction == 1 && containsCon1At < (path.length - 1)) 
						||
						(direction == -1 && containsCon1At > 0)
					) {
						pc.conWayStartBearing = (direction==1) ? bearingForward : bearingBackward;							

//						pc.conWayStartBearing = MoreMath.bearing_start(
//							(pc.searchCon1Lat),
//							(pc.searchCon1Lon),
//							(t.centerLat + t.nodeLat[idxC] * MoreMath.FIXPT_MULT_INV),
//							(t.centerLon + t.nodeLon[idxC] * MoreMath.FIXPT_MULT_INV)
//						);
				}
//				System.out.println("pc.conWayStartBearing: " + pc.conWayStartBearing);

				if (
					containsCon2At - direction >= 0 
					&&
					containsCon2At - direction < path.length
				) {
					int idxC = path[containsCon2At - direction];
					if (idxC < 0) {
						idxC += 65536;
					}
					/* if the route node and the previous node on the way are very close together, use one node before on the way for the bearing calculation:
					 * this especially prevents issues with almost identical coordinates from the previous node on the way and the route node
					 * (as we can't distinguish them exactly enough with fix point coordinates)
					 */
					int iBeforePrevious = containsCon2At - direction - direction;
					if (
							MoreMath.dist(
									t.centerLat + t.nodeLat[idxC] * MoreMath.FIXPT_MULT_INV,
									t.centerLon + t.nodeLon[idxC] * MoreMath.FIXPT_MULT_INV,
									pc.searchCon2Lat,
									pc.searchCon2Lon
							)
							< 3
							// FIXME: It might even make sense to take the previous way into account if there's no previous node on this way
							&&
							iBeforePrevious >= 0 
							&&
							iBeforePrevious < path.length
					) {
						idxC = path [iBeforePrevious];
						if (idxC < 0) {
							idxC += 65536;
						}
					}
					pc.conWayEndBearing = MoreMath.bearing_start(
						(t.centerLat + t.nodeLat[idxC] * MoreMath.FIXPT_MULT_INV),
						(t.centerLon + t.nodeLon[idxC] * MoreMath.FIXPT_MULT_INV),
						(pc.searchCon2Lat),
						(pc.searchCon2Lon)
					);				
				}	
			}
		}
	}

	private int getWayId(SingleTile t) {
		return (t.fileId << 16) + wayNrInFile;
	}

	/**
	 *  check if the area contains the nodes searchCon1 and searchCon2
	 **/
	public void connections2AreaMatch(PaintContext pc, SingleTile t) {
		boolean containsCon1 = false;
		boolean containsCon2 = false;
		short searchCon1Lat = (short)((pc.searchCon1Lat - t.centerLat) * MoreMath.FIXPT_MULT);
		short searchCon1Lon = (short)((pc.searchCon1Lon - t.centerLon) * MoreMath.FIXPT_MULT);
		short searchCon2Lat = (short)((pc.searchCon2Lat - t.centerLat) * MoreMath.FIXPT_MULT);
		short searchCon2Lon = (short)((pc.searchCon2Lon - t.centerLon) * MoreMath.FIXPT_MULT);
		
		// check if area way contains both search connections
//		System.out.println("search area nodes: " + path.length);
		for (int i = 0; i < path.length; i++) {
			int idx = path[i];
			if (idx < 0) {
				idx += 65536;
			}
			// System.out.println("lat:" + t.nodeLat[idx] + "/" + searchCon1Lat);
			if ( (Math.abs(t.nodeLat[idx] - searchCon1Lat) < 2)
					&&
				 (Math.abs(t.nodeLon[idx] - searchCon1Lon) < 2)
			) {
				containsCon1 = true;
				
//				System.out.println("con1 match");
				if (containsCon1 && containsCon2) {
					break;
				}
			}
			if ( (Math.abs(t.nodeLat[idx] - searchCon2Lat) < 2)
					&&
				 (Math.abs(t.nodeLon[idx] - searchCon2Lon) < 2)
				) {
				containsCon2 = true;
//				System.out.println("con2 match");
				if (containsCon1 && containsCon2) {
					break;
				}
			}   
		}
	  
		// we've got a match
		if (containsCon1 && containsCon2) {
			pc.conWayDistanceToNext = 0; // must be filled in later
			pc.conWayNameIdx= this.nameIdx;
			pc.conWayType = this.type;
			short routeFlags = (short) Legend.getWayDescription(this.type).routeFlags; 
			routeFlags |= Legend.ROUTE_FLAG_AREA;
			pc.conWayRouteFlags = routeFlags;				
		}
	}

	
	
	public void processPath(PaintContext pc, SingleTile t, int mode, byte layer) {
		//#if polish.api.bigstyles
		WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
		WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif

		/** width of the way to be painted */
		int w = 0;
		int highlight=HIGHLIGHT_NONE;
		
		/**
		 * If the static array is not large enough, increase it
		 */
		if (x.length < path.length) {		
			x = new int[path.length];
			y = new int[path.length];
			hl = new int[path.length];
		}

		// initialize with default draw states for the path's segments
		int	hlDefault = PATHSEG_DO_NOT_HIGHLIGHT;
		if ( (mode & Tile.OPT_HIGHLIGHT) != 0) {
			hlDefault = PATHSEG_DO_NOT_DRAW;
		}
		for (int i1 = 0; i1 < path.length; i1++) {
			hl[i1] = hlDefault;
		}

		if ((mode & Tile.OPT_PAINT) > 0) {
			//#if polish.api.bigstyles
			byte om = Legend.getWayOverviewMode(type);
			//#else
			byte om = Legend.getWayOverviewMode((short) (type & 0xff));
			//#endif

			switch (om & Legend.OM_MODE_MASK) {
			case Legend.OM_SHOWNORMAL: 
				// if not in Overview Mode check for scale
				if (pc.scale > wayDesc.maxScale * Configuration.getDetailBoostMultiplier()) {			
					return;
				}
				break;
			case Legend.OM_HIDE: 
				if (wayDesc.hideable) {
					return;
				}
				break;
			}
	
			switch (om & Legend.OM_NAME_MASK) {
				case Legend.OM_WITH_NAMEPART: 
					if (nameIdx == -1) {
						return;
					}
					String name = pc.trace.getName(nameIdx);
					String url = pc.trace.getUrl(urlIdx);
					if (name == null) {
						return;
					} else {
						if (url != null) {
							name = name + url;
						}
					}
					/**
					 * The overview filter mode allows you to restrict showing ways if their name
					 * does not match a substring. So check if this condition is fulfilled.
					 */
					if (name.toUpperCase().indexOf(Legend.get0Poi1Area2WayNamePart((byte) 2).toUpperCase()) == -1) {
						return;
					}
					break;
				case Legend.OM_WITH_NAME: 
					if (nameIdx == -1) {
						return;
					}
					break;
				case Legend.OM_NO_NAME: 
					if (nameIdx != -1) {
						return;
					}
					break;
			}
			
			/**
			 * check if way matches to one or more route connections,
			 * so we can highlight the route line parts 
			 */
			// TODO: Refactor this to a method of its own to avoid the many levels
			// of indentation here. This will also make the code easier to understand
			// as the method name can describe what is being done.
			if (RouteLineProducer.isWayIdUsedByRouteLine(getWayId(t)) ) {
				Vector route=pc.trace.getRoute();
				ConnectionWithNode c;
				if (route!=null && route.size()!=0) { 
					for (int i = 0; i < route.size() - 1; i++) {
						c = (ConnectionWithNode) route.elementAt(i);
						if (c.wayNameIdx == this.nameIdx) {
							if (path.length > c.wayFromConAt && path.length > c.wayToConAt) {
								int idx = path[c.wayFromConAt];
								if (idx < 0) {
									idx += 65536;
								}
								short searchCon1Lat = (short) ((c.to.lat - t.centerLat) * MoreMath.FIXPT_MULT);
								if ( (Math.abs(t.nodeLat[idx] - searchCon1Lat) < 2) ) {
									short searchCon1Lon = (short) ((c.to.lon - t.centerLon) * MoreMath.FIXPT_MULT);
									if ( (Math.abs(t.nodeLon[idx] - searchCon1Lon) < 2) ) {
										idx = path[c.wayToConAt];
										if (idx < 0) {
											idx += 65536;
										}
										ConnectionWithNode c2 = (ConnectionWithNode) route.elementAt(i + 1);
										searchCon1Lat = (short) ((c2.to.lat - t.centerLat) * MoreMath.FIXPT_MULT);
										if ( (Math.abs(t.nodeLat[idx] - searchCon1Lat) < 2) ) {
											searchCon1Lon = (short) ((c2.to.lon - t.centerLon) * MoreMath.FIXPT_MULT);
											if ( (Math.abs(t.nodeLon[idx] - searchCon1Lon) < 2) ) {
												// if we are not in highlight mode, flag that this layer contains a path segment to highlight 
												if ((mode & Tile.OPT_HIGHLIGHT) == 0) {
													pc.hlLayers |= (1<<layer);
												}
												
												// set way highlight flag so we can quickly determine if this way contains a route line part
												highlight = HIGHLIGHT_ROUTEPATH_CONTAINED;
												/*
												 *  flag the parts of the way as to be highlighted
												 *  by putting in the index of the corresponding connection 
												 */
												int from = c.wayFromConAt;
												int to = c.wayToConAt;
												boolean isCircleway = isCircleway();
												if (from > to  && !(isRoundAbout() || isCircleway) ) {
													// swap direction
													to = from;
													from = c.wayToConAt;
												}
												
												for (int n = from; n != to; n++) {
													hl[n] = i;
													if ( ( isRoundAbout() || isCircleway ) && n >= (path.length - 1) )  {
														// if in roundabout at end of path continue at first node
														n = -1;
														if (to == (path.length - 1) ) {
															break;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}				
				}
			}
		}

		/**
		 * If this way is not to be highlighted but we are in highlight mode,
		 * skip the rest of the processing of this way
		 */
		if (highlight == HIGHLIGHT_NONE && (mode & Tile.OPT_HIGHLIGHT) != 0) {
			return;
		}
		/**
		 * We go through the way segment by segment
		 * (a segment is a line between two consecutive points on the path).
		 * lineP1 is first node of the segment, lineP2 is the second.
		 */
		IntPoint lineP1 = pc.lineP1;
		IntPoint lineP2 = pc.lineP2;
		IntPoint swapLineP = pc.swapLineP;
		Projection p = pc.getP();
		boolean orthogonal=p.isOrthogonal();
		
		int pi=0;
		
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];
			if (idx < 0) {
				idx += 65536;
			}
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2,t);
			
			if (lineP1 == null) {
				/**
				 * This is the first segment of the path, so we don't have a lineP1 yet
				 */
				lineP1 = lineP2;
				lineP2 = swapLineP;
				x[pi] = lineP1.x;
				y[pi++] = lineP1.y;
			} else {
				/**
				 * We save some rendering time, by doing a line simplification on the fly.
				 * If two nodes are very close by, then we can simply drop one of the nodes
				 * and draw the line between the other points. If the way is highlighted,
				 * we can't do any simplification, as we need to determine the index of the segment 
				 * that is closest to the center (= map center) of the PaintContext
				 */
				if (highlight == HIGHLIGHT_ROUTEPATH_CONTAINED || ! lineP1.approximatelyEquals(lineP2)){
					/**
					 * calculate closest distance to specific ways
					 */

					float dst;
					if (pc.bUsedGpsCenter == true) {
						// Use way geo coordinates for distance calculation
						// instead of screen coordinates if the screen is
						// centered to the gps position.
						int idxp = path[i1-1];
						if (idxp < 0) {
							idxp += 65536;
						}
						fSegmentLong1 =  t.nodeLon[idxp] *  MoreMath.FIXPT_MULT_INV + t.centerLon;
						fSegmentLat1 = t.nodeLat[idxp] *  MoreMath.FIXPT_MULT_INV + t.centerLat;
						fSegmentLong2 = t.nodeLon[idx] *  MoreMath.FIXPT_MULT_INV + t.centerLon;
						fSegmentLat2 = t.nodeLat[idx] *  MoreMath.FIXPT_MULT_INV + t.centerLat;
						dst = MoreMath.ptSegDistSq(
								fSegmentLat1, fSegmentLong1,
								fSegmentLat2, fSegmentLong2,
								pc.nodeGpsPos.radlat, pc.nodeGpsPos.radlon);
					} else {
						IntPoint center = pc.getP().getImageCenter();
						 dst = MoreMath.ptSegDistSq(lineP1.x, lineP1.y,
								lineP2.x, lineP2.y, center.x, center.y);
					}
//					String name = pc.trace.getName(nameIdx);
//					if (dst < pc.squareDstToWay){
//						System.out.println(" Way " + name + " dst=" + dst);
//					}
					// let see where the imageCenter is at this point
					// TODO: hmu comment out
//					try {
//						pc.g.drawLine(center.x-5, center.y-7, center.x+5, center.y+7);
//						pc.g.drawLine(center.x-5, center.y+7, center.x+5, center.y-7);
//					} catch(Exception e){
//						
//					}
					
					if (dst < pc.squareDstWithPenToWay || dst < pc.squareDstWithPenToActualRoutableWay || dst < pc.squareDstToRoutePath) {
						float pen = 0;
						/**
						 * Add a heuristic so that the direction of travel and the direction
						 * of the way should more or less match if we are travelling on this way
						 */
						if (addDirectionalPenalty) {
							//int segDirVecX = lineP1.x - lineP2.x;
							//int segDirVecY = lineP1.y - lineP2.y;
							float segDirVecX = fSegmentLong1 - fSegmentLong2;
							float segDirVecY = fSegmentLat1 - fSegmentLat2;
							float norm = (float) Math.sqrt((double)(segDirVecX * segDirVecX + segDirVecY * segDirVecY));
							//This is a hack to use a linear approximation to keep computational requirements down
							if (this.isOneway()) {
								// Panelty for one ways can be increased until 180 degree
								pen = scalePen * (1.0f - ((segDirVecX * courseVecX + segDirVecY * courseVecY) / norm));
							} else {
								pen = scalePen * (1.0f - Math.abs((segDirVecX * courseVecX + segDirVecY * courseVecY) / norm));
							}
							pen *= pen;
						}
						float dstWithPen = dst + pen;
						// if this way is closer including penalty than the old one set it as new actualWay
						if (dstWithPen < pc.squareDstWithPenToWay) {
								pc.squareDstWithPenToWay = dst + pen;
								pc.actualWay = this;
								pc.actualSingleTile = t;
								if (pc.bUsedGpsCenter == true) {
									pc.fNearestSegmentWayLat1 = fSegmentLat1;
									pc.fNearestSegmentWayLong1 = fSegmentLong1;
									pc.fNearestSegmentWayLat2 = fSegmentLat2;
									pc.fNearestSegmentWayLong2 = fSegmentLong2;
								}
						}
						if (this.isRoutableWay()) {
							// if this routable way is closer including penalty than the old one set it as new actualRoutableWay
							if (dstWithPen < pc.squareDstWithPenToActualRoutableWay) {
								pc.squareDstWithPenToActualRoutableWay = dst + pen;
								pc.actualRoutableWay = this;
								if (pc.bUsedGpsCenter == true) {
									pc.fNearestSegmentRouteableWayLat1 = fSegmentLat1;
									pc.fNearestSegmentRouteableWayLong1 = fSegmentLong1;
									pc.fNearestSegmentRouteableWayLat2 = fSegmentLat2;
									pc.fNearestSegmentRouteableWayLong2 = fSegmentLong2;
								}
							}
							// if this is a highlighted path seg and it's closer than the old one including penalty set it as new one 
							if ((hl[i1 - 1] > PATHSEG_DO_NOT_HIGHLIGHT) && (dstWithPen < pc.squareDstWithPenToRoutePath)) {
								pc.squareDstWithPenToRoutePath = dst + pen;
								pc.squareDstToRoutePath = dst;
								pc.routePathConnection = hl[i1 - 1];
								pc.pathIdxInRoutePathConnection = pi;
								pc.actualRoutePathWay = this;
								if (pc.bUsedGpsCenter == true) {
									pc.fNearestSegmentRoutePathLat1 = fSegmentLat1;
									pc.fNearestSegmentRoutePathLong1 = fSegmentLong1;
									pc.fNearestSegmentRoutePathLat2 = fSegmentLat2;
									pc.fNearestSegmentRoutePathLong2 = fSegmentLong2;
								}
							}
						}
					}
					x[pi] = lineP2.x;
					y[pi++] = lineP2.y;
					swapLineP = lineP1;
					lineP1 = lineP2;
					lineP2 = swapLineP;
				} else if ((i1+1) == path.length) {
					/**
					 * This is an endpoint, so we can't simply drop it, as the lines would potentially look disconnected
					 */
					//System.out.println(" endpoint " + lineP2.x + "/" + lineP2.y+ " " + pc.trace.getName(nameIdx));					
					if (!lineP1.equals(lineP2)) {
						x[pi] = lineP2.x;
						y[pi++] = lineP2.y;												
					} else {
						//System.out.println("   discarding never the less");
					}
				} else {
					/**
					 * Drop this point, it is redundant
					 */					
					//System.out.println(" discard " + lineP2.x + "/" + lineP2.y+ " " +pc.trace.getName(nameIdx));
				}
			}
		}
		swapLineP = lineP1;
		lineP1 = null;
		
		/**
		 * TODO: Can we remove this call and do it the same way as with actualWay and introduce a
		 * pc.actualRoutableWaySingleTile and only calculate the entity and position mark when we need them?
		 */
		if ((pc.actualRoutableWay == this) && ((pc.currentPos == null) || (pc.currentPos.entity != this))) {
			pc.currentPos = new RoutePositionMark(pc.center.radlat, pc.center.radlon);
			pc.currentPos.setEntity(this, getNodesLatLon(t, true), getNodesLatLon(t, false));
		}
		
		if ((mode & Tile.OPT_PAINT) > 0) {
			/**
			 * Calculate the width of the path to be drawn. A width of 1
			 * corresponds to it being draw as a thin line rather than as a
			 * street
			 */
			if (Configuration
					.getCfgBitState(Configuration.CFGBIT_STREETRENDERMODE)
					&& wayDesc.wayWidth > 1) {
				w = (int) (pc.ppm * wayDesc.wayWidth / 2 + 0.5);
			}

			if (highlight != HIGHLIGHT_ROUTEPATH_CONTAINED && pc.dest != null 
					&& this.equals(pc.dest.entity)) {
				highlight = HIGHLIGHT_DEST;
			}
			// if render as lines and no part of the way is highlighted
			if (w == 0 && highlight == HIGHLIGHT_NONE) {
				setColor(pc);
				PolygonGraphics.drawOpenPolygon(pc.g, x, y, pi - 1);
                if (isOneway()) {
                    // Loop through all waysegments for painting the OnewayArrows as overlay
                    // TODO: Maybe, we can integrate this one day in the main loop. Currently, we have troubles
                    // with "not completely fitting arrows" getting painted over by the next waysegment.
                    paintPathOnewayArrows(pi - 1, wayDesc, pc);
                }
			// if render as streets or a part of the way is highlighted
			} else {
				draw(pc, t, (w == 0) ? 1 : w, x, y, hl, pi - 1, highlight,orthogonal);
			}

			paintPathName(pc, t);
		}
	}

    public void paintPathName(PaintContext pc, SingleTile t) {
		//boolean info=false;
    	
    	// exit if not zoomed in enough
		//#if polish.api.bigstyles
	    	WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
	    	WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif
		if (pc.scale > wayDesc.maxTextScale * Configuration.getDetailBoostMultiplier() ) {			
			return;
		}	

		if ( !Configuration.getCfgBitState(Configuration.CFGBIT_WAYTEXTS)) {
			return;
		}

		//remember previous font
		Font originalFont = pc.g.getFont();
		if (pathFont == null) {
			pathFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			pathFontHeight = pathFont.getHeight();
			pathFontMaxCharWidth = pathFont.charWidth('W');
			pathFontBaseLinePos = pathFont.getBaselinePosition();
		}
		// At least the half font height must fit to the on-screen-width of the way
		// (is calculation of w correct???)
		int w = (int)(pc.ppm * wayDesc.wayWidth);
		if (pathFontHeight / 4 > w) {
			return;
		}
		
		String name = null;
		String url = null;
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE)) {
			name = (this.isRoundAbout() ? "rab " : "") + wayDesc.description;
		} else {			
			if (nameIdx != -1) {
				name = Trace.getInstance().getName(nameIdx);
			}
		}
		
		if (urlIdx != -1) {
			url=Trace.getInstance().getUrl(urlIdx);
		}

		if (name == null) {
			return;
		}		

		if (url != null) {
			name = name + url;
		}

		// determine region in which chars can be drawn
		int minCharScreenX = pc.g.getClipX() - pathFontMaxCharWidth;
		int minCharScreenY = pc.g.getClipY() - pathFontBaseLinePos - (w / 2);
		int maxCharScreenX = minCharScreenX + pc.g.getClipWidth() + pathFontMaxCharWidth;
		int maxCharScreenY = minCharScreenY + pc.g.getClipHeight() + pathFontBaseLinePos * 2;
		
		StringBuffer sbName= new StringBuffer();
  	
		pc.g.setFont(pathFont);

		char letter = ' ';
		short charsDrawable = 0;
		Projection p = pc.getP();

		//if(info)System.out.println("Draw "  + name + " from " + path.length + " points");
		
		boolean abbreviated = false;
		int iNameRepeatable = 0;
		int iNameRepeated = 0;

    	// 2 passes:
    	// - 1st pass only counts fitting chars, so we can correctly
    	//   abbreviate reversed strings
    	// - 2nd pass actually draws
    	for (byte mode = PAINTMODE_COUNTFITTINGCHARS; mode <= PAINTMODE_DRAWCHARS; mode++) { 
    		double posChar_x = 0;
    		double posChar_y = 0;
    		double slope_x = 0;
    		double slope_y = 0;
    		double nextDeltaSub = 0;
    		int delta = 0;
    		IntPoint lineP1 = pc.lineP1;
    		IntPoint lineP2 = pc.lineP2;
    		IntPoint swapLineP = pc.swapLineP;
    		// do indent because first letter position would often
    		// be covered by other connecting  streets
			short streetNameCharIndex = -INDENT_PATHNAME;

			// draw name again and again until end of path
			for (int i1 = 0; i1 < path.length; i1++) {
				// get the next line point coordinates into lineP2
				int idx = this.path[i1];
				if (idx < 0) {
					idx += 65536;
				}
				// forward() is in Mercator.java
				p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
				// if we got only one line point, get a second one 
				if (lineP1 == null) {
					lineP1 = lineP2;
					lineP2 = swapLineP;
					continue;
				}
				// calculate the slope of the new line 
				double distance = Math.sqrt( ((double)lineP2.y - (double)lineP1.y) * ((double)lineP2.y - (double)lineP1.y) +
						((double)lineP2.x - (double)lineP1.x) * ((double)lineP2.x - (double)lineP1.x) );
				if (distance != 0) {
					slope_x = ((double)lineP2.x - (double)lineP1.x) / distance;
					slope_y = ((double)lineP2.y - (double)lineP1.y) / distance;
				} else {
					//logger.debug("ZERO distance in path segment " + i1 + "/" + path.length + " of " + name);
					break;
				}
				// new char position is first line point position
				// minus the delta we've drawn over the previous
				// line point
				posChar_x = lineP1.x - nextDeltaSub * slope_x;
				posChar_y = lineP1.y - nextDeltaSub * slope_y;
				
				// as long as we have not passed the next line point
				while( 	(
							(slope_x <= 0 && posChar_x >= lineP2.x) ||
							(slope_x >= 0 && posChar_x <= lineP2.x)
						) && (
							(slope_y <= 0 && posChar_y >= lineP2.y) ||
							(slope_y >= 0 && posChar_y <= lineP2.y)
						)
				) {
					
					// get the street name into the buffer
					if (streetNameCharIndex == -INDENT_PATHNAME) {
						// use full name to count fitting chars
						sbName.setLength(0);
						sbName.append(name);
						abbreviated = false;
						if (mode == PAINTMODE_DRAWCHARS) {
							if (
								iNameRepeated >= iNameRepeatable &&
								charsDrawable > 0 &&
								charsDrawable < name.length()
							) {
								//if(info)System.out.println(sbName.toString() + " i1: " + i1 + " lastFitI1 " + lastFittingI1 + " charsdrawable: " + charsDrawable );
								sbName.setLength(charsDrawable - 1);
								abbreviated = true;
								if (sbName.length() == 0) {
									sbName.append(".");
								}
							}
							// always begin drawing street names
							// left to right
							if (lineP1.x > lineP2.x) {
								sbName.reverse();
							}
						}
					}	
					// draw letter
					if (streetNameCharIndex >= 0) {
						// char to draw
						letter = sbName.charAt(streetNameCharIndex);
						
						if (mode == PAINTMODE_DRAWCHARS) {
							// draw char only if it's at least partly on-screen
							if ( (int)posChar_x >= minCharScreenX &&
								 (int)posChar_x <= maxCharScreenX &&
								 (int)posChar_y >= minCharScreenX &&
								 (int)posChar_y <= maxCharScreenY									
							) {
								if (abbreviated) {
									pc.g.setColor(Legend.COLORS[Legend.COLOR_WAY_LABEL_TEXT_ABBREVIATED]);
								} else {
									pc.g.setColor(Legend.COLORS[Legend.COLOR_WAY_LABEL_TEXT]);
								}
								pc.g.drawChar(
									letter,
									(int)posChar_x, (int)(posChar_y + (w / 2)),
									Graphics.BASELINE | Graphics.HCENTER
								);
							}
						}
//						if (mode == PAINTMODE_COUNTFITTINGCHARS ) {
//							pc.g.setColor(150, 150, 150);
//							pc.g.drawChar(letter,
//							(int)posChar_x, (int)(posChar_y + (w / 2)),
//							Graphics.BASELINE | Graphics.HCENTER);
//						}

						// delta calculation should be improved
						if (Math.abs(slope_x) > Math.abs(slope_y)) {
							delta = (pathFont.charWidth(letter) + pathFontHeight ) /2;							
						} else {
							delta = pathFontHeight * 3 / 4;							
						}
					} else {
						// delta for indent 
						delta = pathFontHeight;
					}

					streetNameCharIndex++;
					if (mode == PAINTMODE_COUNTFITTINGCHARS) {
						charsDrawable = streetNameCharIndex;
					}
					// if at end of name
					if (streetNameCharIndex >= sbName.length()) {
						streetNameCharIndex = -INDENT_PATHNAME;
						if (mode == PAINTMODE_COUNTFITTINGCHARS) {
							// increase number of times the name fitted completely
							iNameRepeatable++;
						} else {
							iNameRepeated++;							
						}
					}
					
					// add slope to char position
					posChar_x += slope_x * delta;
					posChar_y += slope_y * delta;
					
					// how much would we start to draw the next char over the end point
					if (slope_x != 0) {
						nextDeltaSub = (lineP2.x - posChar_x) / slope_x;
					} else {
						nextDeltaSub = 0;
					}
					
				} // end while loop
							
				
				// continue in next path segment
				swapLineP = lineP1;
				lineP1 = lineP2;
				lineP2 = swapLineP;	
			} // end segment for-loop

		} // end mode for-loop
		
		pc.g.setFont(originalFont);
    }

    public void paintPathOnewayArrows(int count, WayDescription wayDesc, PaintContext pc) {
    	// exit if not zoomed in enough
		if (pc.scale > wayDesc.maxOnewayArrowScale /* * pc.config.getDetailBoostMultiplier() */ ) {			
			return;
		}	
		// Exit if user configured to not display OnewayArrows
		if ( !Configuration.getCfgBitState(Configuration.CFGBIT_ONEWAY_ARROWS)) {
			return;
		}
		
		// calculate on-screen-width of the way
		float w = (int)(pc.ppm * wayDesc.wayWidth + 1);
		 
		// if arrow would get too small do not draw
		if (w < 3) {
			return;
		}
		// if arrow would be very small make it a bit larger
		if (w < 5) {
			w = 5;
		}
		// limit maximum arrow width
		if (w > 10) {
			w = 10;
		}
		// calculate arrow length
		int lenTriangle = (int) ((w * 5) / 4);
		int lenLine = (int) ((w * 4) / 3);

		int completeLen = lenTriangle + lenLine;
		int sumTooSmallLen = 0;
			
		// determine region in which arrows can be drawn
		int minArrowScreenX = pc.g.getClipX() - completeLen;
		int minArrowScreenY = pc.g.getClipY() - completeLen;
		int maxArrowScreenX = minArrowScreenX + pc.g.getClipWidth() + completeLen;
		int maxArrowScreenY = minArrowScreenY + pc.g.getClipHeight() + completeLen;
				
		float posArrow_x = 0;
		float posArrow_y = 0;
		float slope_x = 0;
		float slope_y = 0;
    	
    	// cache i + 1 to i2
    	int i2 = 0; 
    	
    	// draw arrow in each segment of path
		for (int i = 0; i < count; i++) {
			i2 = i + 1;
			// calculate the slope of the new line 
			float distance = (float) Math.sqrt( (y[i2] - y[i]) * (y[i2] - y[i]) +
												(x[i2] - x[i]) * (x[i2] - x[i]) );

			if (distance > completeLen || sumTooSmallLen > completeLen) {
				if (sumTooSmallLen > completeLen) {
					sumTooSmallLen = 0;
					// special color for not completely fitting arrows
					pc.g.setColor(Legend.COLORS[Legend.COLOR_ONEWAY_ARROW_NON_FITTING]);
				} else {
					// normal color
					pc.g.setColor(Legend.COLORS[Legend.COLOR_ONEWAY_ARROW]);
				}
				if (distance!=0) {
					slope_x = (x[i2] - x[i]) / distance;
					slope_y = (y[i2] - y[i]) / distance;
				} 
				
				// new arrow position is middle of way segment
				posArrow_x = x[i] + slope_x * (distance - completeLen) / 2;
				posArrow_y = y[i] + slope_y * (distance - completeLen) / 2;				
				
				// draw arrow only if it's at least partly on-screen
				if ( (int)posArrow_x >= minArrowScreenX &&
					 (int)posArrow_x <= maxArrowScreenX &&
					 (int)posArrow_y >= minArrowScreenX &&
					 (int)posArrow_y <= maxArrowScreenY									
				) {
					drawArrow(pc,
							  posArrow_x, posArrow_y,
							  slope_x, slope_y,
							  w, lenLine, lenTriangle 
					);					
				}
				
//				// delta calculation should be improved
//				delta = completeLen * 3;							
//				// add slope to arrow position
//				posArrow_x += slope_x * delta;
//				posArrow_y += slope_y * delta;
//				if (slope_x == 0 && slope_y == 0) {
//					break;
//				}
//
//				// how much would we start to draw the next arrow over the end point
//				if (slope_x != 0) {
//					nextDeltaSub=(x[i1 + 1] - posArrow_x) / slope_x;
//				}
			} else {
				sumTooSmallLen += distance;
			}		
		} // End For: continue in next path segment, if there is any
    }
    
    private void drawArrow( PaintContext pc,
    						double x, double y,
    						double slopeX, double slopeY,
    						double w, int lenLine, int lenTriangle)
    {
    	double x2 = x + slopeX * (double) lenLine;
    	double y2 = y + slopeY * (double) lenLine;
    	
    	pc.g.drawLine((int) x, (int) y, (int) x2, (int) y2);
    	pc.g.fillTriangle(
			(int)(x2 + slopeY * w / 2), (int)(y2 - slopeX * w / 2),
			(int)(x2 - slopeY * w / 2), (int)(y2 + slopeX * w / 2),
			(int)(x2 + slopeX * lenTriangle), (int)(y2 + slopeY * lenTriangle)
    	);
    }
    
   	/** Calculates the turn 2 vectors make (left, right, straight) 
   	 * @return s < 0 for right turn, s > 0 for left turn. 0 for straight
   	 */
    public int getVectorTurn(int ax, int ay, int bx, int by, int cx, int cy) {
  	   //    	 	s < 0      Legend is left of AB
  	   //         	s > 0      Legend is right of AB
  	   //         	s = 0      Legend is on AB
     
     	    return (ay - cy) * (bx - ax) - (ax - cx) * (by - ay);
     }    
    
   /** Calculate the end points for 2 paralell lines with distance w which
    * has the origin line (Xpoint[i]/Ypoint[i]) to (Xpoint[i+1]/Ypoint[i+1])
	* as centre line
    * @param xPoints list off all XPoints for this Way
    * @param yPoints list off all YPoints for this Way
    * @param i the index out of (X/Y) Point for the line segement we looking for
    * @param w the width of the segment out of the way
    * @param p1 left start point
    * @param p2	right start point
    * @param p3 left end point
    * @param p4 right end point
    * @return the angle of the line in radians ( -Pi .. +Pi )
    */
	private float getParLines(int xPoints[], int yPoints[], int i, int w,
			IntPoint p1, IntPoint p2, IntPoint p3, IntPoint p4) {
		int i1 = i + 1;
		int dx = xPoints[i1] - xPoints[i];
		int dy = yPoints[i1] - yPoints[i];
		int l2 = dx * dx + dy * dy;
		float l2f = (float) Math.sqrt(l2);
		float lf = w / l2f;
		int xb = (int) ((Math.abs(lf * dy)) + 0.5f);
		int yb = (int) ((Math.abs(lf * dx)) + 0.5f);
		int rfx = 1;
		int rfy = 1;
		if (dy < 0) {
			rfx = -1;
		}
		if (dx > 0) {
			rfy = -1;
		}
		int xd = rfx * xb;
		int yd = rfy * yb;
		p1.x = xPoints[i] + xd;	
		p1.y = yPoints[i] + yd;
		p2.x = xPoints[i] - xd;
		p2.y = yPoints[i] - yd;
		p3.x = xPoints[i1] + xd;
		p3.y = yPoints[i1] + yd;
		p4.x = xPoints[i1] - xd;
		p4.y = yPoints[i1] - yd;
		if (dx != 0) {
			return (MoreMath.atan(1f * dy / dx));
		} else {
			if (dy > 0) {
				return MoreMath.PiDiv2;
			} else {
				return -MoreMath.PiDiv2;
			}
		}
	}
	
	private void draw(PaintContext pc, SingleTile t, int w, int xPoints[], int yPoints[], 
			int hl[], int count, int highlight,boolean ortho /*, byte mode*/) {
		
		IntPoint closestP = new IntPoint();
		int wClosest = 0;
		boolean dividedSeg = false;
		boolean dividedHighlight = true;
		IntPoint closestDestP = new IntPoint();
		boolean dividedFinalRouteSeg = false;
		boolean dividedFinalDone = false;
		int originalFinalRouteSegX = 0;
		int originalFinalRouteSegY = 0;
		int pathIndexOfNewSeg = 0;
		boolean routeIsForwardOnWay = false;
		int originalX = 0;
		int originalY = 0;
		int wOriginal = w;
		if (w <1) {
			w=1;
		}
		int wDraw = w;
		int turn = 0;
		
		//#if polish.api.bigstyles
		WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
		WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif

		Vector route = pc.trace.getRoute();
		
		//#if polish.api.areaoutlines
		if (highlight == HIGHLIGHT_NONE && Configuration.getCfgBitState(Configuration.CFGBIT_NOSTREETBORDERS)) {
		// Draw streets as lines
		setColor(pc, wayDesc, false, 
			 false, 
			 (highlight == HIGHLIGHT_DEST));
		Paint paint = pc.g.getPaint();
		paint.setStyle(Style.STROKE);
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUND_WAY_ENDS)) {
			paint.setStrokeJoin(Paint.Join.ROUND);
			paint.setStrokeCap(Paint.Cap.ROUND);
		} else {
			paint.setStrokeJoin(Paint.Join.BEVEL);
			paint.setStrokeCap(Paint.Cap.BUTT);
		}
		float strokeWidth = paint.getStrokeWidth();
		paint.setStrokeWidth(w);
		//pc.g.getPaint().setPathEffect(null);
		Path wPath = new Path();
		wPath.moveTo(xPoints[0] + pc.g.getTranslateX(), yPoints[0] + pc.g.getTranslateY());
		for (int i = 1; i < count+1; i++) {
			wPath.lineTo(xPoints[i] + pc.g.getTranslateX(), yPoints[i] + pc.g.getTranslateY());
		}
		pc.g.getCanvas().drawPath(wPath, paint);
		paint.setStrokeWidth(strokeWidth);
		} else {
		//#endif
		for (int i = 0; i < count; i++) {
			wDraw = w;
			// draw route line wider
			if (
				highlight == HIGHLIGHT_ROUTEPATH_CONTAINED
				&& hl[i] >= 0
				&& wDraw < Configuration.getMinRouteLineWidth()
			) {
				wDraw = Configuration.getMinRouteLineWidth();
			}
			if (dividedSeg) {
				// if this is a divided seg, draw second part of it
				xPoints[i] = xPoints[i + 1]; 
				yPoints[i] = yPoints[i + 1]; 
				xPoints[i + 1] = originalX;
				yPoints[i + 1] = originalY;
				dividedHighlight = !dividedHighlight;
			} else {
				// if not turn off the highlight
				dividedHighlight = false;
				if (dividedFinalRouteSeg) {
					//System.out.println ("restore i " + i); 
					xPoints[i] = xPoints[i + 1]; 
					yPoints[i] = yPoints[i + 1]; 
					xPoints[i + 1] = originalFinalRouteSegX;
					yPoints[i + 1] = originalFinalRouteSegY;
					wDraw = w;
					if (routeIsForwardOnWay) {
						hl[i] = PATHSEG_DO_NOT_HIGHLIGHT;
					} else {
						hl[i] = route.size() - 2;
						if (wDraw < Configuration.getMinRouteLineWidth()) {
							wDraw = Configuration.getMinRouteLineWidth();
						}
					}
					dividedFinalRouteSeg = false;
					dividedFinalDone = true;
				}
			}

			// divide final segment on the route
			if (
				//false && 
				highlight == HIGHLIGHT_ROUTEPATH_CONTAINED
				&& route != null
				&& hl[i] == route.size() - 2
			) {
				// get direction we go on the way
				ConnectionWithNode c = (ConnectionWithNode) route.elementAt(hl[i]);
				routeIsForwardOnWay = (c.wayFromConAt < c.wayToConAt);
				pathIndexOfNewSeg = (routeIsForwardOnWay ? c.wayToConAt - 1 : c.wayToConAt);
				//System.out.println ("waytoconat: " + c.wayToConAt + " wayfromconat: " + c.wayFromConAt + " i: " + i);
				if (
					i == pathIndexOfNewSeg
					&& !dividedFinalDone
					&& !dividedFinalRouteSeg
				) {
					pc.getP().forward(RouteInstructions.getClosestPointOnDestWay(), closestDestP);					
					originalFinalRouteSegX = xPoints[i + 1];
					originalFinalRouteSegY = yPoints[i + 1];
					xPoints[i + 1] = closestDestP.x;
					yPoints[i + 1] = closestDestP.y;
					if (routeIsForwardOnWay) {
						;
					} else {
						hl[i] = PATHSEG_DO_NOT_HIGHLIGHT;
						wDraw = w;
					}
					//pc.g.setColor(0x00ff00);
					//pc.g.drawString("closest:" + i, closestDestP.x, closestDestP.y, Graphics.TOP | Graphics.LEFT);
					//System.out.println("Insert closest point at " + (i + 1) );
					dividedFinalRouteSeg = true;
				}
			} else {
				dividedFinalRouteSeg = false;
			}


			if (hl[i] >= 0
					// if this is the closest segment of the closest connection
					&& RouteInstructions.routePathConnection == hl[i]
				    && i == RouteInstructions.pathIdxInRoutePathConnection - 1
				    && !dividedSeg
			) {
				IntPoint centerP = new IntPoint();
				// this is a divided seg (partly prior route line, partly current route line)
				dividedSeg = true;
				centerP=pc.getP().getImageCenter();
				/** centerP is now provided by Projection implementation */
//				pc.getP().forward( 
//						(short)(MoreMath.FIXPT_MULT * (pc.center.radlat - t.centerLat)), 
//						(short)(MoreMath.FIXPT_MULT * (pc.center.radlon - t.centerLon)), centerP, t);
				// get point dividing the seg
				closestP = MoreMath.closestPointOnLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1], centerP.x, centerP.y);
				// remember original next point
				originalX = xPoints[i + 1];
				originalY = yPoints[i + 1];
				// replace next point with closest point
				xPoints[i + 1] = closestP.x;
				yPoints[i + 1] = closestP.y;
				// remember width for drawing the closest point
				wClosest = wDraw;
				// get direction we go on the way
				ConnectionWithNode c = (ConnectionWithNode) route.elementAt(hl[i]);
				dividedHighlight = (c.wayFromConAt > c.wayToConAt);					
				if (isCircleway() && isOneway() && dividedHighlight) {
					dividedHighlight = false;
				}
				// fix dstToRoutePath on way part of divided final route seg for off route display
				if ( (dividedFinalDone || dividedFinalRouteSeg)
					 &&closestP.equals(closestDestP)
				) {
					pc.squareDstToRoutePath = MoreMath.ptSegDistSq(	closestDestP.x, closestDestP.y,
																	closestDestP.x, closestDestP.y,
																	centerP.x, centerP.y
					);
				}
			} else {
				dividedSeg = false;
			}			
			
//			if (hl[i] >= 0) {
//				pc.g.setColor(0xffff80);
//				pc.g.drawString("i:" + i, xPoints[i], yPoints[i],Graphics.TOP | Graphics.LEFT);
//			}
			
			// Get the four outer points of the wDraw-wide waysegment
			if (ortho){
				getParLines(xPoints, yPoints, i , wDraw, l1b, l2b, l1e, l2e);
			} else {
				pc.getP().getParLines(xPoints, yPoints, i , wDraw, l1b, l2b, l1e, l2e);
			}

			
			if (hl[i] != PATHSEG_DO_NOT_DRAW) {
	//			if (mode == DRAW_AREA) {
				
				setColor(pc, wayDesc, (hl[i] >= 0), 
						(isCurrentRoutePath(pc, i) || dividedHighlight), 
						(highlight == HIGHLIGHT_DEST));
						
				int xbcentre = (l2b.x + l1b.x) / 2;
				int xecentre = (l2e.x + l1e.x) / 2;
				int ybcentre = (l2b.y + l1b.y) / 2;
				int yecentre = (l2e.y + l1e.y) / 2;
				int xbdiameter = Math.abs(l2b.x - l1b.x);
				int xediameter = Math.abs(l2e.x - l1e.x);
				int ybdiameter = Math.abs(l2b.y - l1b.y);
				int yediameter = Math.abs(l2e.y - l1e.y);
				// FIXME we would need rotated ellipsis for clean rounded endings,
				// but lacking that we can apply some heuristics
				if (xbdiameter/3 > ybdiameter) {
					ybdiameter = xbdiameter/3;
				}
				if (ybdiameter/3 > xbdiameter) {
					xbdiameter = ybdiameter/3;
				}
				if (xediameter/3 > yediameter) {
					yediameter = xediameter/3;
				}
				if (yediameter/3 > xediameter) {
					xediameter = yediameter/3;
				}
				// when this is not render as lines (for the non-highlighted part of the way) or it is a highlighted part, draw as area
				if (wOriginal != 0 || hl[i] >= 0) {
					//#if polish.api.areaoutlines
					// FIXME would be more efficient to construct a line
					// from all the triangles (for filling turn corners and
					// round road ends)
					//
					Path aPath = new Path();
					pc.g.getPaint().setStyle(Style.FILL);
					aPath.moveTo(l2b.x + pc.g.getTranslateX(), l2b.y + pc.g.getTranslateY());
					aPath.lineTo(l1b.x + pc.g.getTranslateX(), l1b.y + pc.g.getTranslateY());
					aPath.lineTo(l1e.x + pc.g.getTranslateX(), l1e.y + pc.g.getTranslateY());
					aPath.lineTo(l2e.x + pc.g.getTranslateX(), l2e.y + pc.g.getTranslateY());
					aPath.close();
					pc.g.getCanvas().drawPath(aPath, pc.g.getPaint());
					pc.g.getPaint().setStyle(Style.STROKE);
					//#else
					pc.g.fillTriangle(l2b.x, l2b.y, l1b.x, l1b.y, l1e.x, l1e.y);
					pc.g.fillTriangle(l1e.x, l1e.y, l2e.x, l2e.y, l2b.x, l2b.y);
					//#endif

					if (i == 0) {  // if this is the first segment, draw the lines
						// draw circular endings
						if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUND_WAY_ENDS) && wDraw > 2) {
							if (ortho) {
								circleWayEnd(pc, xbcentre, ybcentre, wDraw);
								circleWayEnd(pc, xecentre, yecentre, wDraw);
							} else {
								// ellipse is close, but rotation is missing
								// FIXME fix the bad appearance for eagle projection
								pc.g.fillArc(xbcentre - xbdiameter/2, ybcentre - ybdiameter/2, xbdiameter, ybdiameter, 0, 360);
								pc.g.fillArc(xecentre - xediameter/2, yecentre - yediameter/2, xediameter, yediameter, 0, 360);
							}
						}
						setBorderColor(pc, wayDesc,(hl[i] >= 0), 
							       (isCurrentRoutePath(pc, i) || dividedHighlight), 
							       (highlight == HIGHLIGHT_DEST));
						if (! Configuration.getCfgBitState(Configuration.CFGBIT_NOSTREETBORDERS) || isCurrentRoutePath(pc, i)) {
						    pc.g.drawLine(l2b.x, l2b.y, l2e.x, l2e.y);
						    pc.g.drawLine(l1b.x, l1b.y, l1e.x, l1e.y);
						}
					}

					
					// Now look at the turns(corners) of the waysegment and fill them if necessary.
					// We always look back to the turn between current and previous waysegment.
					if (i > 0) {
						// as we look back, there is no turn at the first segment
						turn = getVectorTurn(xPoints[i - 1], yPoints[i - 1], xPoints[i],
								yPoints[i], xPoints[i + 1], yPoints[i + 1] );
						if (turn < 0 ) {
							// turn right
							intersectionPoint(l4b, l4e, l2b, l2e, intersecP, 1);
							
							setColor(pc, wayDesc,(hl[i] >= 0), 
									(isCurrentRoutePath(pc, i) || dividedHighlight), 
									(highlight == HIGHLIGHT_DEST));
							// Fills the gap of the corner with a small triangle								     
							pc.g.fillTriangle(xPoints[i], yPoints[i] , l3e.x, l3e.y, l1b.x,l1b.y);
 
							if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUND_WAY_ENDS) && wDraw > 2) {
								if (ortho) {
									circleWayEnd(pc, xbcentre, ybcentre, wDraw);
									circleWayEnd(pc, xecentre, yecentre, wDraw
										);
								} else {
									// ellipse is close, but rotation is missing
									// FIXME fix the bad appearance for eagle projection
									pc.g.fillArc(xbcentre - xbdiameter/2, ybcentre - ybdiameter/2, xbdiameter, ybdiameter, 0, 360);
									pc.g.fillArc(xecentre - xediameter/2, yecentre - yediameter/2, xediameter, yediameter, 0, 360);
								}
							}
							setBorderColor(pc, wayDesc, (hl[i] >= 0), 
								       (isCurrentRoutePath(pc, i) || dividedHighlight), 
								       (highlight == HIGHLIGHT_DEST));
							if (! Configuration.getCfgBitState(Configuration.CFGBIT_NOSTREETBORDERS) || isCurrentRoutePath(pc, i)) {
							    if (highlight == HIGHLIGHT_NONE) {
								//paint the inner turn border to the intersection point between old and current waysegment 
								pc.g.drawLine(intersecP.x, intersecP.y, l2e.x, l2e.y);
							    } else {
								//painting full border of the inner turn while routing
								pc.g.drawLine(l2b.x, l2b.y, l2e.x, l2e.y);
							    }
							    // paint the full outer turn border
							    pc.g.drawLine(l1b.x, l1b.y, l1e.x, l1e.y);
							    // paint the full outer turn border
							    pc.g.drawLine(l1b.x, l1b.y, l3e.x, l3e.y);
							}
						}
						else if (turn > 0 ) {
							// turn left
							intersectionPoint(l3b,l3e,l1b,l1e,intersecP,1);
							setColor(pc, wayDesc, (hl[i] >= 0), 
									(isCurrentRoutePath(pc, i) || dividedHighlight), 
									(highlight == HIGHLIGHT_DEST));
							// Fills the gap of the corner with a small triangle
							pc.g.fillTriangle(xPoints[i], yPoints[i] , l4e.x, l4e.y, l2b.x,l2b.y);

							if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUND_WAY_ENDS) && wDraw > 2) {
								if (ortho) {
									circleWayEnd(pc, xbcentre, ybcentre, wDraw);
									circleWayEnd(pc, xecentre, yecentre, wDraw);
								} else {
									// ellipse is close, but rotation is missing
									// FIXME fix the bad appearance for eagle projection
									pc.g.fillArc(xbcentre - xbdiameter/2, ybcentre - ybdiameter/2, xbdiameter, ybdiameter, 0, 360);
									pc.g.fillArc(xecentre - xediameter/2, yecentre - yediameter/2, xediameter, yediameter, 0, 360);
								}
							}
							setBorderColor(pc, wayDesc, (hl[i] >= 0), 
									(isCurrentRoutePath(pc, i) || dividedHighlight), 
									(highlight == HIGHLIGHT_DEST));
							if (! Configuration.getCfgBitState(Configuration.CFGBIT_NOSTREETBORDERS) || isCurrentRoutePath(pc, i)) {
								if (highlight == HIGHLIGHT_NONE) {
									//see comments above
									pc.g.drawLine(intersecP.x, intersecP.y, l1e.x, l1e.y);
								} else {
									pc.g.drawLine(l1b.x, l1b.y, l1e.x, l1e.y);
								}
								pc.g.drawLine(l2b.x, l2b.y, l2e.x, l2e.y);
								//corner
								pc.g.drawLine(l2b.x, l2b.y, l4e.x, l4e.y);
							}
						}
						else {
							//no turn, way is straight
							setColor(pc, wayDesc, (hl[i] >= 0), 
									(isCurrentRoutePath(pc, i) || dividedHighlight), 
									(highlight == HIGHLIGHT_DEST));
							if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUND_WAY_ENDS) && wDraw > 2) {
								if (ortho) {
									circleWayEnd(pc, xbcentre, ybcentre, wDraw);
									circleWayEnd(pc, xecentre, yecentre, wDraw);
								} else {
									// ellipse is close, but rotation is missing
									// FIXME fix the bad appearance for eagle projection
									pc.g.fillArc(xbcentre - xbdiameter/2, ybcentre - ybdiameter/2, xbdiameter, ybdiameter, 0, 360);
									pc.g.fillArc(xecentre - xediameter/2, yecentre - yediameter/2, xediameter, yediameter, 0, 360);
								}
							}
							setBorderColor(pc, wayDesc, (hl[i] >= 0), 
									(isCurrentRoutePath(pc, i) || dividedHighlight), 
									(highlight == HIGHLIGHT_DEST));
							if (! Configuration.getCfgBitState(Configuration.CFGBIT_NOSTREETBORDERS) || isCurrentRoutePath(pc, i)) {
								pc.g.drawLine(l2b.x, l2b.y, l2e.x, l2e.y);
								// paint the full outer turn border
								pc.g.drawLine(l1b.x, l1b.y, l1e.x, l1e.y);
							}
						}
					}
				} else {
					// Draw streets as lines (only 1px wide) 
					setColor(pc,wayDesc, (hl[i] >= 0), 
							(isCurrentRoutePath(pc, i) || dividedHighlight), 
							(highlight == HIGHLIGHT_DEST));
					pc.g.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
				}
				if (isBridge()) {
					waySegment.drawBridge(pc, xPoints, yPoints, i, count - 1, w, 
							l1b, l1e, l2b, l2e);
				}
				if (isTunnel()) {
					waySegment.drawTunnel(pc, xPoints, yPoints, i, count - 1, w, 
							l1b, l1e, l2b, l2e);
				}
				if (isTollRoad()) {
					waySegment.drawTollRoad(pc, xPoints, yPoints, i, count - 1, w, 
							l1b, l1e, l2b, l2e);
				}
				if (isDamaged()) {
					waySegment.drawDamage(pc, xPoints, yPoints, i, count - 1, w, 
							l1b, l1e, l2b, l2e);
				}
			}
			//Save the way-corners for the next loop to fill segment-gaps
			l3b.set(l1b);
			l4b.set(l2b);
			l3e.set(l1e);
			l4e.set(l2e);
			if (dividedSeg || dividedFinalRouteSeg) {
				// if this is a divided seg, in the next step draw the second part
				i--;
			}
		}
		//#if polish.api.areaoutlines
		}
		//#endif
		if (isOneway()) {
			// Loop through all waysegments for painting the OnewayArrows as overlay
			// TODO: Maybe, we can integrate this one day in the main loop. Currently, we have troubles
			// with "not completely fitting arrows" getting overpainted by the next waysegment. 
			paintPathOnewayArrows(count, wayDesc, pc);
		}
		//now as we painted all ways, do the things we should only do once
		if (wClosest != 0) {
			// if we got a closest seg, draw closest point to the center in it
			RouteInstructions.drawRouteDot(pc.g, closestP, wClosest);
		}
		if (nameAsForArea()) {
			paintAreaName(pc,t);
		}
	}

	
	/**
	 * @param pc
	 * @param i - segment idx in the way's path to check
	 * @return true, if the current segment in the way's path is not reached yet, else false  
	 */
	private boolean isCurrentRoutePath(PaintContext pc, int i) {
		if (RouteInstructions.routePathConnection != hl[i]) {
			return (RouteInstructions.routePathConnection < hl[i]);
		}
		ConnectionWithNode c = (ConnectionWithNode) pc.trace.getRoute().elementAt(RouteInstructions.routePathConnection);
		return	(
			(
				c.wayFromConAt < c.wayToConAt
				||
				(isCircleway() && isOneway() && c.wayFromConAt > c.wayToConAt)
			)
			?RouteInstructions.pathIdxInRoutePathConnection <= i 
			:RouteInstructions.pathIdxInRoutePathConnection > i+1
		);
	}
	
	private void intersectionPoint(IntPoint p1, IntPoint p2, IntPoint p3,
								   IntPoint p4, IntPoint ret, int aprox) {

		if (p2.approximatelyEquals(p3, aprox)) {
			// as p2 and p3 are (approx) equal, the intersectionpoint is infinite
			ret.x = p3.x; //  returning p3 as this is the best solution
			ret.y = p3.y;
		}
		else {
			intersectionPointCalc(p1, p2,p3,p4,ret);
		}
	}


//	private void intersectionPoint(IntPoint p1, IntPoint p2, IntPoint p3,
//								   IntPoint p4, IntPoint ret) {
//
//		if (p2.equals(p3)) {
//			// as p2 and p3 are (approx) equal, the intersectionpoint is infinite
//			ret.x = p3.x; //  returning p3 as this is the best solution
//			ret.y = p3.y;
//		} else {
//			intersectionPointCalc(p1, p2,p3,p4,ret);
//		}
//	}

	private void intersectionPointCalc(IntPoint p1, IntPoint p2, IntPoint p3,
			IntPoint p4, IntPoint ret) {
		
		int dx1 = p2.x - p1.x;
		int dx2 = p4.x - p3.x;
		int dx3 = p1.x - p3.x;
		int dy1 = p2.y - p1.y;
		int dy2 = p1.y - p3.y;
		int dy3 = p4.y - p3.y;
		float r = dx1 * dy3 - dy1 * dx2;
		if (r != 0f) {
			r = (1f * (dy2 * (p4.x - p3.x) - dx3 * dy3)) / r;
			ret.x = (int) ((p1.x + r * dx1) + 0.5);
			ret.y = (int) ((p1.y + r * dy1) + 0.5);
		} else {
			if (((p2.x - p1.x) * (p3.y - p1.y) - (p3.x - p1.x) * (p2.y - p1.y)) == 0) {
				ret.x = p3.x;
				ret.y = p3.y;
			} else {
				ret.x = p4.x;
				ret.y = p4.y;
			}
		}
	}

	
/*	private boolean getLineLineIntersection(IntPoint p1, IntPoint p2,
			IntPoint p3, IntPoint p4, IntPoint ret) {

		float x1 = p1.x;
		float y1 = p1.y;
		float x2 = p2.x;
		float y2 = p2.y;
		float x3 = p3.x;
		float y3 = p3.y;
		float x4 = p4.x;
		float y4 = p4.y;

		ret.x = (int) ((det(det(x1, y1, x2, y2), x1 - x2, det(x3, y3, x4, y4),
				x3 - x4) / det(x1 - x2, y1 - y2, x3 - x4, y3 - y4)) + 0.5);
		ret.y = (int) ((det(det(x1, y1, x2, y2), y1 - y2, det(x3, y3, x4, y4),
				y3 - y4) / det(x1 - x2, y1 - y2, x3 - x4, y3 - y4)) + 0.5);

		return true;
	}
*/
	
/*	private static float det(float a, float b, float c, float d) {
		return a * d - b * c;
	} */

	private void circleWayEnd (PaintContext pc, int x, int y, int radius) {
		pc.g.fillArc(x - radius, y - radius, radius*2, radius*2, 0, 360);
	}

	public void paintAsArea(PaintContext pc, SingleTile t) {
		//#if polish.api.bigstyles
		WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
		WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif
		
		//#if polish.api.bigstyles
		byte om = Legend.getWayOverviewMode(type);
		//#else
		byte om = Legend.getWayOverviewMode((short) (type & 0xff));
		//#endif
		switch (om & Legend.OM_MODE_MASK) {
			case Legend.OM_SHOWNORMAL: 
				// if not in Overview Mode check for scale
				if (pc.scale > wayDesc.maxScale * Configuration.getDetailBoostMultiplier()) {			
					return;
				}
				// building areas
				if (wayDesc.isBuilding()) {
					if (!Configuration.getCfgBitState(Configuration.CFGBIT_BUILDINGS)) {
						return;
					}
				// non-building areas
				} else {
					if(wayDesc.hideable && !Configuration.getCfgBitState(Configuration.CFGBIT_AREAS)) {
						return;
					}
				}
				break;
			case Legend.OM_HIDE: 
				if (wayDesc.hideable) {
					return;
				}
				break;
		}
		switch (om & Legend.OM_NAME_MASK) {
			case Legend.OM_WITH_NAMEPART: 
				if (nameIdx == -1) {
					return;
				}
				String name = pc.trace.getName(nameIdx);
				String url = pc.trace.getName(urlIdx);
				if (name == null) {
					return;
				}
				if (name.toUpperCase().indexOf(Legend.get0Poi1Area2WayNamePart((byte) 1).toUpperCase()) == -1) {
					return;
				}
				break;
			case Legend.OM_WITH_NAME: 
				if (nameIdx == -1) {
					return;
				}
				break;
			case Legend.OM_NO_NAME: 
				if (nameIdx != -1) {
					return;
				}
				break;
		}
		
		Projection p = pc.getP();
		IntPoint p1 = pc.lineP2;
		IntPoint p2 = pc.swapLineP;
		IntPoint p3 = pc.tempPoint;
		Graphics g = pc.g;
		g.setColor(wayDesc.lineColor);
		boolean dashed = (wayDesc.getGraphicsLineStyle() == WayDescription.WDFLAG_LINESTYLE_DOTTED);

		//#if polish.android
		boolean doNotSimplifyMap = !Configuration.getCfgBitState(Configuration.CFGBIT_SIMPLIFY_MAP_WHEN_BUSY);
		//#endif
		
		int idx;
		if (Configuration.getCfgBitState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS)
		    && Legend.getLegendMapFlag(Legend.LEGEND_MAPFLAG_OUTLINE_AREA_BLOCK)) {
			if (path.length > 0) {
				//#if polish.android
				//#if polish.api.areaoutlines
				Path aPath = new Path();
				g.getPaint().setStyle(Style.FILL);
				aPath.setFillType(Path.FillType.EVEN_ODD);
				//#endif
				//#endif
				idx = path[0];
				if (idx < 0) {
					idx += 65536;
				}
				p.forward(t.nodeLat[idx],t.nodeLon[idx],p1,t);
				//#if polish.android
				//#if polish.api.areaoutlines
				aPath.moveTo(p1.x + g.getTranslateX(), p1.y + g.getTranslateY());
				//#endif
				//#endif
				for (int i1 = 1; i1 < path.length; i1++){
					idx = path[i1];	
					if (idx < 0) {
						idx += 65536;
					}
					p.forward(t.nodeLat[idx],t.nodeLon[idx],p2,t);
					//#if polish.android
					//#if polish.api.areaoutlines
					if (((flags & WAY_TRIANGLES_IN_OUTLINE_MODE) == WAY_TRIANGLES_IN_OUTLINE_MODE) && (i1 % 3 == 0)) {
						//System.out.println("Drawing triangle in outline mode");
						aPath.close();
						aPath.moveTo(p2.x + g.getTranslateX(), p2.y + g.getTranslateY());

					} else {

						aPath.lineTo(p2.x + g.getTranslateX(), p2.y + g.getTranslateY());
					}
					//#endif
					//#endif
				}
				//#if polish.android
				//#if polish.api.areaoutlines
				aPath.close();
				if (holes != null && (flags & WAY_TRIANGLES_IN_OUTLINE_MODE) == 0) {
					for (short hole[] : holes) {
						idx = hole[0];
						if (idx < 0) {
							idx += 65536;
						}
						p.forward(t.nodeLat[idx],t.nodeLon[idx],p1,t);
						aPath.moveTo(p1.x + g.getTranslateX(), p1.y + g.getTranslateY());
						for (int i1 = 1; i1 < hole.length; ) {
							idx = hole[i1++];	
							if (idx < 0) {
								idx += 65536;
							}
							p.forward(t.nodeLat[idx],t.nodeLon[idx],p2,t);
							aPath.lineTo(p2.x + g.getTranslateX(), p2.y + g.getTranslateY());
						}
						aPath.close();
					}
				}
				// FIXME currently this depends on a modified Graphics.java of J2MEPolish to expose the private canvas and paint
				// variables in Graphics.java to other modules; must find a way to do this with normal J2MEPolish or to
				// get a modification to J2MEPolish
				g.getCanvas().drawPath(aPath, g.getPaint());
				g.getPaint().setStyle(Style.STROKE);
				//#endif
				//#endif
			}
		} else {

		for (int i1 = 0; i1 < path.length; ){
//			pc.g.setColor(wayDesc.lineColor);
			idx = path[i1++];	
			if (idx < 0) {
				idx += 65536;
			}
			p.forward(t.nodeLat[idx],t.nodeLon[idx],p1,t);
			idx = path[i1++];	
			if (idx < 0) {
				idx += 65536;
			}
			p.forward(t.nodeLat[idx],t.nodeLon[idx],p2,t);
			idx = path[i1++];	
			if (idx < 0) {
				idx += 65536;
			}
			p.forward(t.nodeLat[idx],t.nodeLon[idx],p3,t);
			if (dashed) {
				FilledTriangle.fillTriangle(pc, p1.x,p1.y,p2.x,p2.y,p3.x,p3.y);
			} else {
				if (p1.x != p2.x || p2.x != p3.x || p1.y != p2.y || p2.y != p3.y) {
					g.fillTriangle(p1.x,p1.y,p2.x,p2.y,p3.x,p3.y);
// Without these, there are ugly light-color gaps in filled areas on Android devices - but this cuts down on performance,
// remove for now on 2012-07-23 for configurations with "simplify map when busy" turned on
// possibly could be helped by antialiasing or some other tweaks in J2MEPolish code:
//
// https://github.com/Enough-Software/j2mepolish/blob/4718fe3b9f55eb8a2c8172316ed416d87f9bf86c/enough-polish-j2me/source/src/de/enough/polish/android/lcdui/Graphics.java
//
// see also http://stackoverflow.com/questions/7608362/how-to-draw-smooth-rounded-path
// though better yet would be to use direct polygons without triangulation
//
// with 250 m zoom on Sams. Galaxy Note, paint times with the drawLines:
// I/System.out( 7740): Painting map took 18176 ms 992/1524
// I/System.out( 7740): Painting map took 19300 ms 992/1524
// I/System.out( 7740): Painting map took 14855 ms 992/1524
// I/System.out( 7740): Painting map took 14030 ms 992/1524
// I/System.out( 7740): Painting map took 14450 ms 992/1524
// I/System.out( 7740): Painting map took 13993 ms 992/1524
// I/System.out( 7740): Painting map took 13572 ms 992/1524
// without drawLines:
// I/System.out( 8938): Painting map took 6304 ms 992/1524
// I/System.out( 8938): Painting map took 5430 ms 992/1524
// I/System.out( 8938): Painting map took 5587 ms 992/1524
// I/System.out( 8938): Painting map took 5219 ms 992/1524
// I/System.out( 8938): Painting map took 4609 ms 992/1524
// I/System.out( 8938): Painting map took 4386 ms 992/1524
// I/System.out( 8938): Painting map took 4287 ms 992/1524


//#if polish.android
				if (doNotSimplifyMap) {
					g.drawLine(p1.x,p1.y,p2.x,p2.y);
					g.drawLine(p2.x,p2.y,p3.x,p3.y);
					g.drawLine(p1.x,p1.y,p3.x,p3.y);
				}
//#endif
				} else {
					// optimisation: render triangles consisting of 3 equal coordinates simply as a dot 
					g.drawRect(p1.x, p1.y, 0, 0 );
				}
			}			
		}
		}
		paintAreaName(pc,t);
	}

	public void paintAreaName(PaintContext pc, SingleTile t) {
		//#if polish.api.bigstyles
		WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
		WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif
		if (pc.scale > wayDesc.maxTextScale * Configuration.getDetailBoostMultiplier() ) {			
			return;
		}		
		
		// building areas
		if (wayDesc.isBuilding()) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_BUILDING_LABELS)) {
				return;
			}
		// non-building areas
		} else {
			if (wayDesc.hideable && !Configuration.getCfgBitState(Configuration.CFGBIT_AREATEXTS)) {
				return;
			}
		}

		IntPoint lineP2 = pc.lineP2;
		Projection p = pc.getP();
		int x;
		int y;

		// get screen clip
		int clipX = pc.g.getClipX();
		int clipY = pc.g.getClipY();
		int clipMaxX = clipX + pc.g.getClipWidth();
		int clipMaxY = clipY + pc.g.getClipHeight();;

		// find center of area
		int minX = clipMaxX;
		int maxX = clipX;
		int minY = clipMaxY;
		int maxY = clipY;
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];			
			if (idx < 0) {
				idx += 65536;
			}
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
			x = lineP2.x;
			y = lineP2.y;
			if (minX > x) {
				minX = x;
			}
			if (minY > y) {
				minY = y;
			}
			if (maxX < x) {
				maxX = x;
			}
			if (maxY < y) {
				maxY = y;
			}
		}

		// if area is not wide enough to draw at least a dot into it, return
		if ((maxX - minX) < 3 ) {
			return;
		}
		
		// System.out.println("name:" + name + " ClipX:" + clipX + " ClipMaxX:" + clipMaxX + " ClipY:" + clipY + " ClipMaxY:" + clipMaxY + " minx:" + minX + " maxX:"+maxX + " miny:"+minY+ " maxY" + maxY);

		Font originalFont = pc.g.getFont();
		if (areaFont == null) {
			areaFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			areaFontHeight = areaFont.getHeight();
			areaFontMinRenderWidth = areaFont.stringWidth("ii");
		}
		
		pc.g.setColor(Legend.COLORS[Legend.COLOR_AREA_LABEL_TEXT]);
		
		String name = null;
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE)) {
			name = wayDesc.description;
		// if the area has a name, resolve it
		} else if (nameIdx != -1) {
			// if not at least "ii" would be renderable, just paint a dot to indicate there would be a name available
			if ((maxX - minX) < areaFontMinRenderWidth) {
				pc.g.drawRect((minX + maxX) / 2, (minY + maxY) / 2, 0, 0 );
				return;			
			}		
			// resolve the name
			name = Trace.getInstance().getName(nameIdx);
		}
		
		// if zoomed in enough, show description 
		if (pc.scale < wayDesc.maxDescriptionScale) {
		// show way description
			if (name == null) {
				name = wayDesc.description;
			} else {
				name = name + " (" + wayDesc.description + ")";
			}
		}
		if (name == null) {
			return;
		}

		// find out how many chars of the name fit into the area
		int i = name.length() + 1;
		int w;
		do {
			i--;
			w = areaFont.substringWidth(name, 0, i);
		} while (w > (maxX - minX) && i > 1);

		// if at least two chars have fit or name is a fitting single char, draw name
		if (i > 1 || (i == name.length() && w <= (maxX - minX))  ) {
			pc.g.setFont(areaFont);
			// center vertically in area
			int y1 = (minY + maxY - areaFontHeight) / 2;
			// draw centered into area
			pc.g.drawSubstring(name, 0, i, (minX + maxX - w) / 2, y1, Graphics.TOP | Graphics.LEFT);
			// if name fits not completely, append "..."
			if (i != name.length()) {
				pc.g.drawString("...", (minX + maxX + w) / 2, y1, Graphics.TOP | Graphics.LEFT);
			}
			pc.g.setFont(originalFont);
			// else draw a dot to indicate there's a name for this area available
		} else {
			pc.g.drawRect((minX + maxX) / 2, (minY + maxY) / 2, 0, 0 );
		}
	}
	
	public void setColor(PaintContext pc,WayDescription wayDesc,boolean routing,boolean isCurrentRoutePath, boolean highlight) {
		
		if (routing) {
			// set the way(area)-color
			if (isCurrentRoutePath) {
				// set this color if way is part of current route
				pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE]);
			} else {
				// set this color if way was part of current route
				pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_PRIOR_ROUTELINE]);
			}
		} else if (highlight) {
			// way is highlighted as destination for routing
			// TODO: Shouldn't this colour be configurable too?
			pc.g.setColor(255, 50, 50);						
		} else {
			// use color from style.xml
			pc.g.setStrokeStyle(wayDesc.getGraphicsLineStyle());
			pc.g.setColor(this.isRoutableWay() ? wayDesc.lineColor : darkenNonRoutableColor(wayDesc.lineColor));				
		}
	}

	public void setColor(PaintContext pc) {		
		//#if polish.api.bigstyles
		WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
		WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif
		pc.g.setStrokeStyle(wayDesc.getGraphicsLineStyle());
		pc.g.setColor(isDamaged() ? Legend.COLORS[Legend.COLOR_WAY_DAMAGED_DECORATION] : darkenNonRoutableColor(wayDesc.lineColor));
	}
	
	private int darkenNonRoutableColor(int color) {
		return (this.isRoutableWay() || !Configuration.getCfgBitState(Configuration.CFGBIT_DRAW_NON_TRAVELMODE_WAYS_DARKER)) ? color : ((color & 0xfefefe) >> 1);
	}
	
	
	public int getWidth(PaintContext pc) {
		//#if polish.api.bigstyles
		WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
		WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif
		return wayDesc.wayWidth;
	}

	/**
	 * Advanced setting of the BorderColor
	 */
	public void setBorderColor(PaintContext pc, WayDescription wayDesc, boolean routing, 
			boolean dividedHighlight, boolean highlight) {
		
		if (routing) {
			if (dividedHighlight) {
				pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE_BORDER]);
			} else {
				pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_PRIOR_ROUTELINE_BORDER]);
			}
		} else if (highlight) {
			// TODO: Shouldn't this colour be configurable too?
			pc.g.setColor(255, 50, 50);
		} else {
			pc.g.setStrokeStyle(Graphics.SOLID);
			pc.g.setColor(isDamaged() ? Legend.COLORS[Legend.COLOR_WAY_DAMAGED_BORDER] : wayDesc.boardedColor);
		}
	}

	public void setBorderColor(PaintContext pc) {
		pc.g.setStrokeStyle(Graphics.SOLID);
		//#if polish.api.bigstyles
		WayDescription wayDesc = Legend.getWayDescription(type);
		//#else
		WayDescription wayDesc = Legend.getWayDescription((short) (type & 0xff));
		//#endif
		pc.g.setColor(isDamaged() ? Legend.COLOR_WAY_DAMAGED_BORDER : wayDesc.boardedColor);
	}
	
	public boolean isOneway() {
		return (flags & WAY_ONEWAY) != 0;
	}
	
	
	/**
	 * Checks if the way is a circle way (a closed way - with the same node at the start and the end)
	 * @return true if this way is a circle way  
	 */
	public boolean isCircleway() {
		return (path[0] == path[path.length - 1]);
	}
	
	public boolean isArea() {
		return ((flags & WAY_AREA) > 0);
	}

	public boolean isRatherBig() {
		return ((flags & WAY_RATHER_BIG) > 0);
	}

	public boolean isEvenBigger() {
		return ((flags & WAY_EVEN_BIGGER) > 0);
	}
	
    public boolean nameAsForArea() {
               return ((flags & WAY_NAMEASFORAREA) > 0);
    }

	public boolean isRoundAbout() {
		return ((flags & WAY_ROUNDABOUT) > 0);
	}
	
	public boolean isOneDirectionOnly() {
		return (flags & (WAY_ONEWAY + WAY_ROUNDABOUT)) != 0
				&& !(
						(Configuration.getTravelMode().travelModeFlags & TravelMode.AGAINST_ALL_ONEWAYS) > 0
					||	((Configuration.getTravelMode().travelModeFlags & TravelMode.BICYLE_OPPOSITE_EXCEPTIONS) > 0
							&& (flags & WAY_CYCLE_OPPOSITE) != 0
						)				
				);
	}
	
	
	public boolean isTunnel() {
		return ((flags & WAY_TUNNEL) > 0);
	}
	
	public boolean isBridge() {
		return ((flags & WAY_BRIDGE) > 0);
	}

	public boolean isTollRoad() {
		return ((wayRouteModes & Connection.CONNTYPE_TOLLROAD) > 0);
	}

	public boolean isDamaged() {
		return ((flags & WAY_DAMAGED) > 0);
	}
	
	public int getMaxSpeed() {
		return ((flags & MaxSpeedMask) >> MaxSpeedShift);
	}
	
	public int getMaxSpeedWinter() {
		return ((getFlagswinter() & MaxSpeedMask) >> MaxSpeedShift);
	}

/*	private float[] getFloatNodes(SingleTile t, short[] nodes, float offset) {
	    float [] res = new float[nodes.length];
	    for (int i = 0; i < nodes.length; i++) {
		res[i] = nodes[i] * SingleTile.fpminv + offset;
	    }
	    return res;
	}
*/

	public float[] getNodesLatLon(SingleTile t, boolean latlon) {
		float offset; 
		short [] nodes;
		int len = path.length;
		float [] lat = new float[len];
		
		if (latlon) { 
			offset = t.centerLat;
			nodes = t.nodeLat;
		} else {
			offset = t.centerLon;
			nodes = t.nodeLon;
		}
		for (int i = 0; i < len; i++) {
			int pathI = path[i];
			if (pathI < 0) {
				pathI += 65536;
			}
			lat[i] = nodes[pathI] * MoreMath.FIXPT_MULT_INV + offset;
		}
		return lat;
	}
	
	public String toString() {
		//#if polish.api.bigstyles
		return Locale.get("way.Way")/*Way*/ + " " + Trace.getInstance().getName(nameIdx) + " " + Locale.get("way.type")/*type*/ + ": " +  Legend.getWayDescription(type).description;
		//#else
		return Locale.get("way.Way")/*Way*/ + " " + Trace.getInstance().getName(nameIdx) + " " + Locale.get("way.type")/*type*/ + ": " +  Legend.getWayDescription((short) (type & 0xff)).description;
		//#endif
	}

	/**
	 * @param wayRouteModes the wayRouteModes to set
	 */
	public void setWayRouteModes(byte wayRouteModes) {
		this.wayRouteModes = wayRouteModes;
	}

	/**
	 * @return the wayRouteModes
	 */
	public byte getWayRouteModes() {
		return wayRouteModes;
	}

	/**
	 * @param flagswinter the flagswinter to set
	 */
	public void setFlagswinter(int flagswinter) {
		this.flagswinter = flagswinter;
	}

	/**
	 * @return the flagswinter
	 */
	public int getFlagswinter() {
		return flagswinter;
	}
}
