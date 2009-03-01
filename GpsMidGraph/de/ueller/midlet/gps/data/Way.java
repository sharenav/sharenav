package de.ueller.midlet.gps.data;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import net.sourceforge.jmicropolygon.PolygonGraphics;
import de.enough.polish.util.DrawUtil;
import de.ueller.gps.data.Configuration;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.RouteInstructions;
import de.ueller.midlet.gps.routing.ConnectionWithNode;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.WayDescription;

// TODO: explain
/* Questions:
 * - What classes are involved for getting the necessary Way data from the Jar?
 * - Which region is covered by a SingleTile, does it always contain all Nodes of the complete way?
 * - Where are the way nodes combined if a tile was split in Osm2GpsMid?
 */

public class Way extends Entity{
	
	public static final byte WAY_FLAG_NAME = 1;
	public static final byte WAY_FLAG_MAXSPEED = 2;
	public static final byte WAY_FLAG_LAYER = 4;
	public static final byte WAY_FLAG_LONGWAY = 8;
	public static final byte WAY_FLAG_ONEWAY = 16;	
//	public static final byte WAY_FLAG_MULTIPATH = 4;	
	public static final byte WAY_FLAG_NAMEHIGH = 32;
	public static final byte WAY_FLAG_AREA = 64;
//	public static final byte WAY_FLAG_ISINHIGH = 64;	
	public static final int WAY_FLAG_ADDITIONALFLAG = 128;

	public static final byte WAY_FLAG2_ROUNDABOUT = 1;
	public static final byte WAY_FLAG2_TUNNEL = 2;
	public static final byte WAY_FLAG2_BRIDGE = 4;
	
	
	public static final byte DRAW_BORDER=1;
	public static final byte DRAW_AREA=2;
	public static final byte DRAW_FULL=3;
	
	private static final int MaxSpeedMask = 0xff;
	private static final int MaxSpeedShift = 0;
	private static final int ModMask = 0xff00;
	private static final int ModShift = 8;
	
	public static final int WAY_ONEWAY=1 << ModShift;
	public static final int WAY_AREA=2 << ModShift;
	public static final int WAY_ROUNDABOUT=4 << ModShift;
	public static final int WAY_TUNNEL=8 << ModShift;
	public static final int WAY_BRIDGE=16 << ModShift;

	public static final byte PAINTMODE_COUNTFITTINGCHARS = 0;
	public static final byte PAINTMODE_DRAWCHARS = 1;
	public static final byte INDENT_PATHNAME = 2;

	private static final int PATHSEG_DO_NOT_HIGHLIGHT = -1;
	private static final int PATHSEG_DO_NOT_DRAW = -2;
	
	private static final int HIGHLIGHT_NONE = 0;
	private static final int HIGHLIGHT_TARGET = 1;
	private static final int HIGHLIGHT_ROUTEPATH_CONTAINED = 2;
	
	protected static final Logger logger = Logger.getInstance(Way.class,Logger.TRACE);

	private int flags=0;


	public short[] path;
	public short minLat;
	public short minLon;
	public short maxLat;
	public short maxLon;
	
	/**
	 * This is a buffer for the drawing routines
	 * so that we don't have to allocate new
	 * memory at each time we draw a way. This
	 * saves some time on memory managment
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
	private static Font pathFont;
	private static int pathFontHeight;
	private static int pathFontMaxCharWidth;
	private static int pathFontBaseLinePos;
	
	

	static final IntPoint l1b = new IntPoint();
	static final IntPoint l1e = new IntPoint();
	static final IntPoint l2b = new IntPoint();
	static final IntPoint l2e = new IntPoint();
	static final IntPoint l3b = new IntPoint();
	static final IntPoint l3e = new IntPoint();
	static final IntPoint l4b = new IntPoint();
	static final IntPoint l4e = new IntPoint();
	static final IntPoint s1 = new IntPoint();
	static final IntPoint s2 = new IntPoint();
	
	/**
	 * Enables or disables the match travelling direction for actual way calculation heuristic 
	 */
	private static boolean addDirectionalPenalty = false;
	/**
	 * Sets the scale of the directional penalty dependent on the projection used.
	 */
	private static float scalePen = 0;
	/*
	 * Precalculated normalised vector for the direction of current travel,
	 * used in the the directional penalty heuristic
	 */
	private static float courseVecX = 0;
	private static float courseVecY = 0;
	

//	private final static Logger logger = Logger.getInstance(Way.class,
//			Logger.TRACE);

	/**
	 * the flag should be readed by caller. if Flag == 128 this is a dummy Way
	 * and can ignored.
	 * 
	 * @param is Tile inputstream
	 * @param f flags
	 * @param t Tile
	 * @param layers: this is somewhat awkward. We need to get the layer information back out to 
	 * 			the caller, so use a kind of call by reference
	 * @paran idx index into the layers array where to store the layer info.
	 * @param nodes
	 * @throws IOException
	 */
	public Way(DataInputStream is, byte f, Tile t, byte[] layers, int idx) throws IOException {
		minLat = is.readShort();
		minLon = is.readShort();
		maxLat = is.readShort();
		maxLon = is.readShort();
//		if (is.readByte() != 0x58){
//			logger.error("worng magic after way bounds");
//		}
		//System.out.println("Way flags: " + f);

		type = is.readByte();
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
			flags = is.readByte();
		}
		
		byte f2=0;
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
		}
		
		layers[idx] = 0;
		if ((f & WAY_FLAG_LAYER) == WAY_FLAG_LAYER) {
			/**
			 * TODO: We are currently ignoring the layer info
			 * Please implement proper support for this when rendering
			 */
			layers[idx] = is.readByte();
		}
		if ((f & WAY_FLAG_ONEWAY) == WAY_FLAG_ONEWAY) {
			flags += WAY_ONEWAY;
		}
		if (((f & WAY_FLAG_AREA) == WAY_FLAG_AREA) || C.getWayDescription(type).isArea) {
			if ((f & WAY_FLAG_AREA) == WAY_FLAG_AREA){
				//#debug debug
				logger.debug("Loading explicit Area: " + this);
			}
			flags += WAY_AREA;
		}

		boolean longWays=false;
		if ((f & 8) == 8) {
			longWays=true;
		}

			int count;
			if (longWays){
				count = is.readShort();
				if (count < 0) {
					count+=65536;
				}
			} else {
				count = is.readByte();
				if (count < 0) {
					count+=256;
				}
				
			}
			path = new short[count];
			for (short i = 0; i < count; i++) {
				path[i] = is.readShort();
//				logger.debug("read node id=" + path[i]);
			}
//			if (is.readByte() != 0x59 ){
//				logger.error("wrong magic code after path");
//			}			
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
		if ((speed < 7) || !gpsRecenter) {
			addDirectionalPenalty = false;
			scalePen = 0;
			return;
		}
		addDirectionalPenalty = true;
		Projection p = pc.getP();
		float lat1 = pc.center.radlat;
		float lon1 = pc.center.radlon;
		IntPoint lineP1 = new IntPoint();
		IntPoint lineP2 = new IntPoint();
		float lat2 = pc.center.radlat + (float) (0.00001*Math.cos(pc.course * MoreMath.FAC_DECTORAD));
		float lon2 = pc.center.radlon + (float) (0.00001*Math.sin(pc.course * MoreMath.FAC_DECTORAD));
		p.forward(lat1, lon1, lineP1);
		p.forward(lat2, lon2, lineP2);
		
		courseVecX = lineP1.x - lineP2.x;
		courseVecY = lineP1.y - lineP2.y;
		float norm = (float)Math.sqrt(courseVecX*courseVecX + courseVecY*courseVecY);
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
		scalePen = 35.0f/d*100.0f;
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
		
		return (float) MoreMath.bearing_int(t.nodeLat[path[0]]*SingleTile.fpminv + t.centerLat,
				t.nodeLon[path[0]]*SingleTile.fpminv + t.centerLon, 
				t.nodeLat[path[path.length - 1]]*SingleTile.fpminv + t.centerLat,
				t.nodeLon[path[path.length - 1]]*SingleTile.fpminv + t.centerLon);
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
		short containsCon1At = 0;
		short containsCon2At = 0;
		short searchCon1Lat = (short) ((pc.searchCon1Lat - t.centerLat) * t.fpm);
		short searchCon1Lon = (short) ((pc.searchCon1Lon - t.centerLon) * t.fpm);
		short searchCon2Lat = (short) ((pc.searchCon2Lat - t.centerLat) * t.fpm);
		short searchCon2Lon = (short) ((pc.searchCon2Lon - t.centerLon) * t.fpm);
		
		
		// check if way contains both search connections
		// System.out.println("search path nodes: " + path.length);
		for (short i = 0; i < path.length; i++) {
			int idx = path[i];
			// System.out.println("lat:" + t.nodeLat[idx] + "/" + searchCon1Lat);
			if ( (Math.abs(t.nodeLat[idx] - searchCon1Lat) < 2)
					&&
				 (Math.abs(t.nodeLon[idx] - searchCon1Lon) < 2)
			) {
				if (
					C.getWayDescription(this.type).routable
					// count in roundabouts only once (search connection could match at start and end node)
					&& !containsCon1
					// count only if it's not a oneway ending at this connection 
					&& !(isOneway() && i == path.length - 1)
				) {
					// remember bearings of the ways at this connection, so we can later check out if we have multiple straight-ons requiring a bearing instruction
					// we do this twice for each found way to add the bearings for forward and backward
					for (int d = -1 ; d <= 1; d += 2) {
						// do not add bearings against direction if this is a oneway
						if (d == -1 && isOneway()) {
							continue;
						}
						if ( (i + d) < path.length && (i + d) >= 0) {
							short rfCurr = (short) C.getWayDescription(this.type).routeFlags;
							// count bearings for entering / leaving the motorway. We don't need to give bearing instructions if there's only one motorway alternative at the connection
							if (RouteInstructions.isEnterMotorway(pc.searchConPrevWayRouteFlags, rfCurr)
								||
								RouteInstructions.isLeaveMotorway(pc.searchConPrevWayRouteFlags, rfCurr)
							) {
								pc.conWayNumMotorways++;
							}							
							int idxC = path[i + d];
							byte bearing = MoreMath.bearing_start(
									(pc.searchCon1Lat),
									(pc.searchCon1Lon),
									(t.centerLat + t.nodeLat[idxC] *  t.fpminv),
									(t.centerLon + t.nodeLon[idxC] *  t.fpminv)
							);
							pc.conWayBearings.addElement(new Byte(bearing) );
						}
					}
					// remember nameIdx's leading away from the connection, so we can later on check if multiple ways lead to the same street name
					changeCountNameIdx(pc, 1);						
					//System.out.println("add 1 " + "con1At: " + i + " pathlen-1: " + (path.length-1) );
					pc.conWayNumRoutableWays++;
					// if we are in the middle of the way, count the way once more
					if (i != 0 && i != path.length-1 && !isOneway()) {
						pc.conWayNumRoutableWays++;
						changeCountNameIdx(pc, 1);
						//System.out.println("add middle 1 " + "con1At: " + i + " pathlen-1: " + (path.length-1) );
					}			
				}
				containsCon1 = true;
				containsCon1At = i;
				
				// System.out.println("con1 match");
				if (containsCon1 && containsCon2) break;
			}
			if ( (Math.abs(t.nodeLat[idx] - searchCon2Lat) < 2)
					&&
				 (Math.abs(t.nodeLon[idx] - searchCon2Lon) < 2)
				) {
				containsCon2 = true;
				containsCon2At = i;
				// System.out.println("con2 match");
				if (containsCon1 && containsCon2) break;
			}   
		}
	  
		// we've got a match
		if (containsCon1 && containsCon2) {
			float conWayRealDistance = 0;
			short from = containsCon1At;
			short to = containsCon2At;
			int direction = 1;
			if (from > to && !isRoundAbout()) {
				// if this is a oneway but not a roundabout it can't be the route path as we would go against the oneway's direction
				if (isOneway()) return;
				// swap direction
				from = to;
				to = containsCon1At;
				direction = -1;
			}
			
			
			int idx1 = path[from];
			int idx2;

			// sum up the distance of the segments between searchCon1 and searchCon2
			for (short i = from; i != to; i++) {
				if (isRoundAbout() && i >= (path.length-2))  {
					i=-1; // if in roundabout at end of path continue at first node
					if (to == (path.length-1) ) {
						break;
					}
				}
				idx2 = path[i+1];
				float dist = ProjMath.getDistance(	(t.centerLat + t.nodeLat[idx1] *  t.fpminv),
													(t.centerLon + t.nodeLon[idx1] *  t.fpminv),
													(t.centerLat + t.nodeLat[idx2] *  t.fpminv),
													(t.centerLon + t.nodeLon[idx2] *  t.fpminv));
				conWayRealDistance += dist;
				idx1 = idx2;
			}
			/* check if this is a better match than a maybe previous one:
			if the distance is closer than the already matching one
			this way contains a better path between the connections
			*/
			if (conWayRealDistance < pc.conWayDistanceToNext &&
				/* check if wayDescription has the routable flag set
				 * (there are situations where 2 ways have the same nodes, e.g. a tram and a highway)
				*/
				C.getWayDescription(this.type).routable 
			) {
//				if (pc.conWayDistanceToNext != Float.MAX_VALUE) {
//					String name1=null, name2=null;
//					if (pc.conWayNameIdx != -1) name1=Trace.getInstance().getName(pc.conWayNameIdx);
//					if (this.nameIdx != -1) name2=Trace.getInstance().getName(this.nameIdx);
//					System.out.println("REPLACE " + pc.conWayDistanceToNext + "m (" + C.getWayDescription(pc.conWayType).description + " " + (name1==null?"":name1) + ")");
//					System.out.println("WITH " + conWayRealDistance + "m (" + C.getWayDescription(this.type).description + " " + (name2==null?"":name2) + ")");
//				}				
				// this is currently the best path between searchCon1 and searchCon2
				pc.conWayDistanceToNext = conWayRealDistance;
				pc.conWayFromAt = containsCon1At;
				pc.conWayToAt = containsCon2At;
				pc.conWayNameIdx= this.nameIdx;
				pc.conWayType = this.type;
				short routeFlags = (short) C.getWayDescription(this.type).routeFlags; 
				if (isRoundAbout()) routeFlags += C.ROUTE_FLAG_ROUNDABOUT;
				if (isTunnel()) routeFlags += C.ROUTE_FLAG_TUNNEL;
				if (isBridge()) routeFlags += C.ROUTE_FLAG_BRIDGE;
				pc.conWayRouteFlags = routeFlags;
				
				// substract way we are coming from from turn options with same name
				changeCountNameIdx(pc, -1);
				//System.out.println("sub 1");
				
				// calculate bearings
				if ( (direction==1 && containsCon1At < (path.length - 1)) 
						||
						(direction==-1 && containsCon1At > 0)
					) {
						int idxC = path[containsCon1At + direction];
						pc.conWayStartBearing = MoreMath.bearing_start(
							(pc.searchCon1Lat),
							(pc.searchCon1Lon),
							(t.centerLat + t.nodeLat[idxC] *  t.fpminv),
							(t.centerLon + t.nodeLon[idxC] *  t.fpminv)
						);
				}

				if ( (direction==1 && containsCon2At > 0) 
						||
						(direction==-1 && containsCon2At < (path.length - 1))
				) {
					int idxC = path[containsCon2At - direction];
					pc.conWayEndBearing = MoreMath.bearing_start(
						(t.centerLat + t.nodeLat[idxC] *  t.fpminv),
						(t.centerLon + t.nodeLon[idxC] *  t.fpminv),
						(pc.searchCon2Lat),
						(pc.searchCon2Lon)
					);				
				}	
			}
		}
	}

	
	
	public void processPath(PaintContext pc, SingleTile t, int mode, byte layer) {		
		WayDescription wayDesc = C.getWayDescription(type);
		int w = 0;
		byte highlight=HIGHLIGHT_NONE;
		
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
			byte om = C.getWayOverviewMode(type);    

			switch (om & C.OM_MODE_MASK) {
			case C.OM_SHOWNORMAL: 
				// if not in Overview Mode check for scale
				if (pc.scale > wayDesc.maxScale * Configuration.getDetailBoostMultiplier()) {			
					return;
				}
				break;
			case C.OM_HIDE: 
				if (wayDesc.hideable) {
					return;
				}
				break;
			}
	
			switch (om & C.OM_NAME_MASK) {
				case C.OM_WITH_NAMEPART: 
					if (nameIdx == -1) return;
					String name = pc.trace.getName(nameIdx);
					if (name == null) return;
					if (name.toUpperCase().indexOf(C.get0Poi1Area2WayNamePart((byte) 2).toUpperCase()) == -1) return;
					break;
				case C.OM_WITH_NAME: 
					if (nameIdx == -1) return;
					break;
				case C.OM_NO_NAME: 
					if (nameIdx != -1) return;
					break;
			}
			
			/*
			 * check if way matches to one or more route connections,
			 * so we can highlight the route line parts 
			 */  
			Vector route=pc.trace.getRoute();
			ConnectionWithNode c;
			if (route!=null && route.size()!=0) { 
				for (int i=0; i<route.size()-1; i++){
					c = (ConnectionWithNode) route.elementAt(i);
					if (c.wayNameIdx == this.nameIdx && wayDesc.routable) {
						if (path.length > c.wayFromConAt && path.length > c.wayToConAt) {
							int idx = path[c.wayFromConAt];
							short searchCon1Lat = (short) ((c.to.lat - t.centerLat) * t.fpm);
							if ( (Math.abs(t.nodeLat[idx] - searchCon1Lat) < 2) ) {
								short searchCon1Lon = (short) ((c.to.lon - t.centerLon) * t.fpm);
								if ( (Math.abs(t.nodeLon[idx] - searchCon1Lon) < 2) ) {
									idx = path[c.wayToConAt];
									ConnectionWithNode c2 = (ConnectionWithNode) route.elementAt(i+1);
									searchCon1Lat = (short) ((c2.to.lat - t.centerLat) * t.fpm);
									if ( (Math.abs(t.nodeLat[idx] - searchCon1Lat) < 2) ) {
										searchCon1Lon = (short) ((c2.to.lon - t.centerLon) * t.fpm);
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
											short from = c.wayFromConAt;
											short to = c.wayToConAt;
											if (from > to  && !isRoundAbout()) {
												// swap direction
												to = from;
												from = c.wayToConAt;
											}
											
											for (int n = from; n != to; n++) {
												hl[n] = i;
												if (isRoundAbout() && n >= (path.length-1) )  {
													n=-1; //  // if in roundabout at end of path continue at first node
													if (to == (path.length-1) ) {
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

		if (highlight == HIGHLIGHT_NONE && (mode & Tile.OPT_HIGHLIGHT) != 0) {
			return;
		}		
		
		IntPoint lineP1 = pc.lineP1;
		IntPoint lineP2 = pc.lineP2;
		IntPoint swapLineP = pc.swapLineP;
		Projection p = pc.getP();
		
		int pi=0;		
		
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2,t);
			if (lineP1 == null) {
				lineP1 = lineP2;
				lineP2 = swapLineP;	
				x[pi] = lineP1.x;
				y[pi++] = lineP1.y;				
			} else {
				/**
				 * We save some rendering time, by doing a line simplifation on the fly.
				 * If two nodes are very close by, then we can simply drop one of the nodes
				 * and draw the line between the other points. 
				 */
				if (highlight == HIGHLIGHT_ROUTEPATH_CONTAINED || ! lineP1.approximatelyEquals(lineP2)){					
					/* 
					 * calculate closest distance to specific ways
					 */
					float dst = MoreMath.ptSegDistSq(lineP1.x, lineP1.y,
							lineP2.x, lineP2.y, pc.xSize / 2, pc.ySize / 2);
					
					if (dst < pc.squareDstToWay) {
						/**
						 * Add a heuristic so that the direction of travel and the direction
						 * of the way should more or less match if we are travelling on this way
						 */
						if (addDirectionalPenalty) {
							int segDirVecX = lineP1.x-lineP2.x;
							int segDirVecY = lineP1.y-lineP2.y;
							float norm = (float) Math.sqrt((double)(segDirVecX*segDirVecX + segDirVecY*segDirVecY));
							//This is a hack to use a linear approximation to keep computational requirements down
							float pen = scalePen*(1.0f - Math.abs((segDirVecX*courseVecX + segDirVecY*courseVecY)/norm));
							pen*=pen;
							if (dst + pen < pc.squareDstToWay) {
								pc.squareDstToWay = dst + pen;
								pc.actualWay = this;
								pc.actualSingleTile = t;
							}
						} else {
							pc.squareDstToWay = dst;
							pc.actualWay = this;
							pc.actualSingleTile = t;
						}
					}
					if (dst < pc.squareDstToRoutableWay && wayDesc.routable) {
						pc.squareDstToRoutableWay = dst;
						pc.nearestRoutableWay = this;
					}
					if (dst < pc.squareDstToRoutePath && hl[i1-1] > PATHSEG_DO_NOT_HIGHLIGHT) {
						pc.squareDstToRoutePath = dst;						
						pc.routePathConnection = hl[i1-1];
						pc.pathIdxInRoutePathConnection = pi;
					}				
					x[pi] = lineP2.x;
					y[pi++] = lineP2.y;
					swapLineP = lineP1;
					lineP1 = lineP2;
					lineP2 = swapLineP;
				} else if ((i1+1) == path.length){
					/**
					 * This is an endpoint, so we can't simply drop it, as the lines would potentially look disconnected
					 */
					//System.out.println(" endpoint " + lineP2.x + "/" + lineP2.y+ " " +pc.trace.getName(nameIdx));					
					if (!lineP1.equals(lineP2)){
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
		
		if ((pc.nearestRoutableWay == this) && ((pc.currentPos == null) || (pc.currentPos.e != this))) {
			pc.currentPos=new PositionMark(pc.center.radlat,pc.center.radlon);
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

			if (pc.target != null && this.equals(pc.target.e)) {
				highlight=HIGHLIGHT_TARGET;
				draw(pc, t, (w == 0) ? 1 : w, x, y, hl, pi - 1, highlight);
			} else {
				// if render as lines and no part of the way is highlighted
				if (w == 0 && highlight != HIGHLIGHT_ROUTEPATH_CONTAINED) {
					setColor(pc);
					PolygonGraphics.drawOpenPolygon(pc.g, x, y, pi - 1);
				// if render as streets or a part of the way is highlighted
				} else {
					draw(pc, t, w, x, y, hl, pi - 1, highlight);
				}
			}

			if (isOneway()) {
				paintPathOnewayArrows(pc, t);
			}
			paintPathName(pc, t);
		}
	}

    public void paintPathName(PaintContext pc, SingleTile t) {
		//boolean info=false;
    	
    	// exit if not zoomed in enough
    	WayDescription wayDesc = C.getWayDescription(type);
		if (pc.scale > wayDesc.maxTextScale * Configuration.getDetailBoostMultiplier() ) {			
			return;
		}	

		if ( !Configuration.getCfgBitState(Configuration.CFGBIT_WAYTEXTS)) {
			return;
		}

		
		//remember previous font
		Font originalFont = pc.g.getFont();
		if (pathFont==null) {
			pathFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			pathFontHeight=pathFont.getHeight();
			pathFontMaxCharWidth = pathFont.charWidth('W');
			pathFontBaseLinePos = pathFont.getBaselinePosition();
		}
		// At least the half font height must fit to the on-screen-width of the way
		// (is calculation of w correct???)
		int w = (int)(pc.ppm*wayDesc.wayWidth);
		if (pathFontHeight/4>w) {
			return;
		}
		
		String name=null;
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE)) {
			name=(this.isRoundAbout()?"rab ":"") + wayDesc.description;
		} else {			
			if (nameIdx != -1) {
				name=Trace.getInstance().getName(nameIdx);
			}
		}

		
		if (name==null) {
			return;
		}		

		// determine region in which chars can be drawn
		int minCharScreenX = pc.g.getClipX() - pathFontMaxCharWidth;
		int minCharScreenY = pc.g.getClipY() - pathFontBaseLinePos - (w/2);
		int maxCharScreenX = minCharScreenX + pc.g.getClipWidth() + pathFontMaxCharWidth;
		int maxCharScreenY = minCharScreenY + pc.g.getClipHeight() + pathFontBaseLinePos * 2;
		
		StringBuffer sbName= new StringBuffer();
  	
		pc.g.setFont(pathFont);
		pc.g.setColor(0,0,0);

		IntPoint posChar = new IntPoint();
		char letter=' ';
		short charsDrawable=0;
		Projection p = pc.getP();

		//if(info)System.out.println("Draw "  + name + " from " + path.length + " points");
		
		boolean reversed=false;
		boolean abbreviated=false;
		int iNameRepeatable=0;
		int iNameRepeated=0;

    	// 2 passes:
    	// - 1st pass only counts fitting chars, so we can correctly
    	//   abbreviate reversed strings
    	// - 2nd pass actually draws
    	for (byte mode=PAINTMODE_COUNTFITTINGCHARS;mode<=PAINTMODE_DRAWCHARS; mode++) { 
    		double posChar_x = 0;
    		double posChar_y = 0;
    		double slope_x=0;
    		double slope_y=0;
    		double nextDeltaSub=0;
    		int delta=0;
    		IntPoint lineP1 = pc.lineP1;
    		IntPoint lineP2 = pc.lineP2;
    		IntPoint swapLineP = pc.swapLineP;
    		// do indent because first letter position would often
    		// be covered by other connecting  streets
			short streetNameCharIndex=-INDENT_PATHNAME;

			// draw name again and again until end of path
			for (int i1 = 0; i1 < path.length; i1++) {
				// get the next line point coordinates into lineP2
				int idx = this.path[i1];
				// forward() is in Mercator.java
				p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
				// if we got only one line point, get a second one 
				if (lineP1 == null) {
					lineP1 = lineP2;
					lineP2 = swapLineP;
					continue;
				}
				// calculate the slope of the new line 
				double distance = Math.sqrt( ((double)lineP2.y-(double)lineP1.y)*((double)lineP2.y-(double)lineP1.y) +
						((double)lineP2.x-(double)lineP1.x)*((double)lineP2.x-(double)lineP1.x) );
				if (distance!=0) {
					slope_x = ((double)lineP2.x-(double)lineP1.x)/distance;
					slope_y = ((double)lineP2.y-(double)lineP1.y)/distance;
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
							(slope_x<=0 && posChar_x >= lineP2.x) ||
							(slope_x>=0 && posChar_x <= lineP2.x)
						) && (
							(slope_y<=0 && posChar_y >= lineP2.y) ||
							(slope_y>=0 && posChar_y <= lineP2.y)
						)
				) {
					

					// get the street name into the buffer
					if (streetNameCharIndex==-INDENT_PATHNAME) {
						// use full name to count fitting chars
						sbName.setLength(0);
						sbName.append(name);
						abbreviated=false;
						reversed=false;
						if(mode==PAINTMODE_DRAWCHARS) {
							if (
								iNameRepeated>=iNameRepeatable &&
								charsDrawable>0 &&
								charsDrawable<name.length()
							) {
								//if(info)System.out.println(sbName.toString() + " i1: " + i1 + " lastFitI1 " + lastFittingI1 + " charsdrawable: " + charsDrawable );
								sbName.setLength(charsDrawable-1);
								abbreviated=true;
								if (sbName.length()==0) {
									sbName.append(".");
								}
							}
							// always begin drawing street names
							// left to right
							if (lineP1.x > lineP2.x) {
								sbName.reverse();
								reversed=true;
							}
						}
					}	
					// draw letter
					if (streetNameCharIndex >=0) {
						// char to draw
						letter=sbName.charAt(streetNameCharIndex);
						
						if (mode==PAINTMODE_DRAWCHARS) {
							// draw char only if it's at least partly on-screen
							if ( (int)posChar_x >= minCharScreenX &&
								 (int)posChar_x <= maxCharScreenX &&
								 (int)posChar_y >= minCharScreenX &&
								 (int)posChar_y <= maxCharScreenY									
							) {
								if (abbreviated) {
									pc.g.setColor(100,100,100);
								} else {
									pc.g.setColor(0,0,0);
								}
								pc.g.drawChar(
									letter,
									(int)posChar_x, (int)(posChar_y+(w/2)),
									Graphics.BASELINE | Graphics.HCENTER
								);
							}
						}
//						if (mode==PAINTMODE_COUNTFITTINGCHARS ) {
//							pc.g.setColor(150,150,150);
//							pc.g.drawChar(letter,
//							(int)posChar_x, (int)(posChar_y+(w/2)),
//							Graphics.BASELINE | Graphics.HCENTER);
//						}

						// delta calculation should be improved
						if (Math.abs(slope_x) > Math.abs(slope_y)) {
							delta=(pathFont.charWidth(letter) + pathFontHeight ) /2;							
						} else {
							delta=pathFontHeight*3/4;							
						}
					} else {
						// delta for indent 
						delta=pathFontHeight;
					}

					streetNameCharIndex++;
					if(mode==PAINTMODE_COUNTFITTINGCHARS) {
						charsDrawable=streetNameCharIndex;
					}
					// if at end of name
					if (streetNameCharIndex>=sbName.length()) {
						streetNameCharIndex=-INDENT_PATHNAME;
						if(mode==PAINTMODE_COUNTFITTINGCHARS) {
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
						nextDeltaSub=(lineP2.x-posChar_x) / slope_x;
					} else {
						nextDeltaSub=0;
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

    public void paintPathOnewayArrows(PaintContext pc, SingleTile t) {
    	// exit if not zoomed in enough
    	WayDescription wayDesc = C.getWayDescription(type);
		if (pc.scale > wayDesc.maxOnewayArrowScale /* * pc.config.getDetailBoostMultiplier() */ ) {			
			return;
		}	

		if ( !Configuration.getCfgBitState(Configuration.CFGBIT_ONEWAY_ARROWS)) {
			return;
		}
		
		// calculate on-screen-width of the way
		double w = (int)(pc.ppm*wayDesc.wayWidth + 1);
		 
		// if arrow would get too small do not draw
		if(w<3) {
			return;
		}
		// if arrow would be very small make it a bit larger
		if(w<5) {
			w=5;
		}
		// limit maximum arrow width
		if (w > 10) {
			w=10;
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
				
		Projection p = pc.getP();
		
		double posArrow_x = 0;
    	double posArrow_y = 0;    	
    	double slope_x=0;
    	double slope_y=0;
    	
//    	int delta=0;
//    	double nextDeltaSub=0;
    	
    	IntPoint lineP1 = pc.lineP1;
    	IntPoint lineP2 = pc.lineP2;
    	IntPoint swapLineP = pc.swapLineP;
    	
		// draw arrow in each segment of path
		for (int i1 = 0; i1 < path.length; i1++) {
			// get the next line point coordinates into lineP2
			int idx = this.path[i1];
			// forward() is in Mercator.java
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
			// if we got only one line point, get a second one 
			if (lineP1 == null) {
				lineP1 = lineP2;
				lineP2 = swapLineP;
				continue;
			}
			// calculate the slope of the new line 
			double distance = Math.sqrt( ((double)lineP2.y-(double)lineP1.y)*((double)lineP2.y-(double)lineP1.y) +
					((double)lineP2.x-(double)lineP1.x)*((double)lineP2.x-(double)lineP1.x) );

			if (distance > completeLen || sumTooSmallLen > completeLen) {
				if (sumTooSmallLen > completeLen) {
					sumTooSmallLen = 0;
					// special color for not completely fitting arrows
					pc.g.setColor(80,80,80);
				} else {
					// normal color
					pc.g.setColor(50,50,50);
				}
				if (distance!=0) {
					slope_x = ((double)lineP2.x-(double)lineP1.x)/distance;
					slope_y = ((double)lineP2.y-(double)lineP1.y)/distance;
				} else {
					//logger.debug("ZERO distance in path segment " + i1 + "/" + path.length + " of " + name);
					break;
				}
				// new arrow position is middle of way segment
				posArrow_x = lineP1.x + slope_x * (distance-completeLen)/2;
				posArrow_y = lineP1.y + slope_y * (distance-completeLen)/2;				
				
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
//				if (slope_x==0 && slope_y==0) {
//					break;
//				}
//
//				// how much would we start to draw the next arrow over the end point
//				if (slope_x != 0) {
//					nextDeltaSub=(lineP2.x-posArrow_x) / slope_x;
//				}
			} else {
				sumTooSmallLen += distance;
			}		
				
			// continue in next path segment
			swapLineP = lineP1;
			lineP1 = lineP2;
			lineP2 = swapLineP;	
		} // end segment for-loop		
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
			(int)(x2 + slopeY * w/2), (int)(y2 - slopeX * w/2),
			(int)(x2 - slopeY * w/2), (int)(y2 + slopeX * w/2),
			(int)(x2 + slopeX * lenTriangle), (int)(y2 + slopeY * lenTriangle)
    	);
    }
    
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
	
	

	private void draw(PaintContext pc, SingleTile t, int w, int xPoints[], int yPoints[], int hl[], int count,byte highlight/*,byte mode*/) {
		
		float roh1=0.0f;
		float roh2;

		IntPoint closestP = new IntPoint();
		int wClosest = 0;
		boolean dividedSeg = false;
		boolean dividedHighlight = true;
		int originalX = 0;
		int originalY = 0;
		int max = count ;
		int beforeMax = max - 1;
		int wOriginal = w;
		if (w <1) w=1;
		int wDraw = w;
		int oldWDraw = 0; // setting this to 0 gets roh1 in the first step
		for (int i = 0; i < max; i++) {
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
				xPoints[i] = xPoints[i+1]; 
				yPoints[i] = yPoints[i+1]; 
				xPoints[i+1] = originalX;
				yPoints[i+1] = originalY;
				dividedHighlight = !dividedHighlight;
			} else {
				// if not turn off the highlight
				dividedHighlight = false;
			}
			if (highlight == HIGHLIGHT_ROUTEPATH_CONTAINED && hl[i] >= 0
					// if this is the closest segment of the closest connection
					&& RouteInstructions.routePathConnection == hl[i]
				    && i==RouteInstructions.pathIdxInRoutePathConnection - 1
				    && !dividedSeg
			) {
				IntPoint centerP = new IntPoint();
				// this is a divided seg (partly prior route line, partly current route line)
				dividedSeg = true;
				pc.getP().forward( (short) (SingleTile.fpm * (pc.center.radlat - t.centerLat)), (short) (SingleTile.fpm * (pc.center.radlon - t.centerLon)), centerP, t);
				// get point dividing the seg
				closestP = closestPointOnLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1], centerP.x, centerP.y);
				// remember original next point
				originalX = xPoints[i + 1];
				originalY = yPoints[i + 1];
				// replace next point with closest point
				xPoints[i + 1] = closestP.x;
				yPoints[i + 1] = closestP.y;
				// remember width for drawing the closest point
				wClosest = wDraw;
				// get direction we go on the way
				Vector route=pc.trace.getRoute();
				ConnectionWithNode c = (ConnectionWithNode) route.elementAt(hl[i]);
				dividedHighlight = (c.wayFromConAt > c.wayToConAt);
			} else {
				dividedSeg = false;
			}			
			// get parLine again, if width has changed (when switching between highlighted / non-highlighted parts of the way)
			if (wDraw != oldWDraw) {
				roh1 = getParLines(xPoints, yPoints, i, wDraw, l1b, l2b, l1e, l2e);
			}
			if (i < beforeMax) {
				roh2 = getParLines(xPoints, yPoints, i + 1, wDraw, l3b, l4b, l3e,
						l4e);
				if (!MoreMath.approximately_equal(roh1, roh2, 0.5f)) {
					intersectionPoint(l1b, l1e, l3b, l3e, s1);
					intersectionPoint(l2b, l2e, l4b, l4e, s2);
					l1e.set(s1);
					l2e.set(s2);
					l3b.set(s1);
					l4b.set(s2);
				}
			}
			if (hl[i] != PATHSEG_DO_NOT_DRAW) {
	//			if (mode == DRAW_AREA){
					if (highlight == HIGHLIGHT_ROUTEPATH_CONTAINED && hl[i] >= 0) {
						if (isCurrentRoutePath(pc, i) || dividedHighlight) {
							pc.g.setColor(C.ROUTE_COLOR);
						} else {
							pc.g.setColor(C.ROUTEPRIOR_COLOR);
						}
					} else {
						setColor(pc);
					}
					// when this is not render as lines (for the non-highlighted part of the way) or it is a highlighted part, draw as area
					if (wOriginal != 0 || hl[i] >= 0) {
						pc.g.fillTriangle(l2b.x, l2b.y, l1b.x, l1b.y, l1e.x, l1e.y);
						pc.g.fillTriangle(l1e.x, l1e.y, l2e.x, l2e.y, l2b.x, l2b.y);
						// draw borders of way
						if (highlight == HIGHLIGHT_TARGET){
							pc.g.setColor(255,50,50);
						} else if (highlight == HIGHLIGHT_ROUTEPATH_CONTAINED && hl[i] >= 0){
							if (isCurrentRoutePath(pc, i) || dividedHighlight) {
								pc.g.setColor(C.ROUTE_BORDERCOLOR);
							} else {
								pc.g.setColor(C.ROUTEPRIOR_BORDERCOLOR);								
							}
						} else {
							setBorderColor(pc);
						}
						pc.g.drawLine(l1b.x, l1b.y, l1e.x, l1e.y);
						pc.g.drawLine(l2b.x, l2b.y, l2e.x, l2e.y);
					} else {
						pc.g.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
					}
			}
			l1b.set(l3b);
			l2b.set(l4b);
			l1e.set(l3e);
			l2e.set(l4e);
			if (dividedSeg) {
				// if this is a divided seg, in the next step draw the second part
				i--;
			}
		}
		if (wClosest != 0) {
			// if we got a closest seg, draw closest point to the center in it
			pc.g.setColor(C.ROUTEDOT_COLOR);
			pc.g.fillArc(closestP.x-wClosest, closestP.y-wClosest, wClosest*2, wClosest*2, 0, 360);
			pc.g.setColor(C.ROUTEDOT_BORDERCOLOR);
			pc.g.drawArc(closestP.x-wClosest, closestP.y-wClosest, wClosest*2, wClosest*2, 0, 360);
		}
	}
	
	/**
	 * 
	 * @param lineP1x - in screen coordinates
	 * @param lineP1y - in screen coordinates
	 * @param lineP2x - in screen coordinates
	 * @param lineP2y - in screen coordinates
	 * @param offPointX - point outside the line in screen coordinates
	 * @param offPointY - point outside the line in screen coordinates
	 * @return IntPoint - closest point on line in screen coordinates
	 */
	private static IntPoint closestPointOnLine(int lineP1x, int lineP1y, int lineP2x, int lineP2y, int offPointX, int offPointY) {
		float uX = (float) (lineP2x - lineP1x);
		float uY = (float) (lineP2y - lineP1y);
		float  u = ( (offPointX - lineP1x) * uX + (offPointY  - lineP1y) * uY) / (uX * uX + uY * uY);
		if (u > 1.0) {
			return new IntPoint(lineP2x, lineP2y);
		} else if (u <= 0.0) {
			return new IntPoint(lineP1x, lineP1y);
		} else {
			return new IntPoint( (int)(lineP2x * u + lineP1x * (1.0 - u ) + 0.5), (int) (lineP2y * u + lineP1y * (1.0-u) + 0.5));
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
			(c.wayFromConAt < c.wayToConAt)
			?RouteInstructions.pathIdxInRoutePathConnection <= i 
			:RouteInstructions.pathIdxInRoutePathConnection > i+1
		);
	}

	private IntPoint intersectionPoint(IntPoint p1, IntPoint p2, IntPoint p3,
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
		return ret;
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

	public void paintAsArea(PaintContext pc, SingleTile t) {
		WayDescription wayDesc = C.getWayDescription(type);
		
		byte om = C.getWayOverviewMode(type);
		switch (om & C.OM_MODE_MASK) {
			case C.OM_SHOWNORMAL: 
				// if not in Overview Mode check for scale
				if (pc.scale > wayDesc.maxScale * Configuration.getDetailBoostMultiplier()) {			
					return;
				}
				if (wayDesc.hideable && !Configuration.getCfgBitState(Configuration.CFGBIT_AREAS)) {
					return;
				}
				break;
			case C.OM_HIDE: 
				if (wayDesc.hideable) {
					return;
				}
				break;
		}
		switch (om & C.OM_NAME_MASK) {
			case C.OM_WITH_NAMEPART: 
				if (nameIdx == -1) return;
				String name = pc.trace.getName(nameIdx);
				if (name == null) return;
				if (name.toUpperCase().indexOf(C.get0Poi1Area2WayNamePart((byte) 1).toUpperCase()) == -1) return;
				break;
			case C.OM_WITH_NAME: 
				if (nameIdx == -1) return;
				break;
			case C.OM_NO_NAME: 
				if (nameIdx != -1) return;
				break;
		}
		
		IntPoint lineP2 = pc.lineP2;
		Projection p = pc.getP();
		/**
		 * we should probably use the static x and y variables
		 * but that would require to rewrite the fillPolygon
		 * function
		 */
		int[] x = new int[path.length];
		int[] y = new int[path.length];
		
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];			
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
			x[i1] = lineP2.x;
			y[i1] = lineP2.y;
		}
		/*if ((x[0] != x[path.length - 1]) || (y[0] != y[path.length - 1])){
			System.out.println("WARNING: start and end coordinates of area don't match " + this);			
			return;
		}*/
		//PolygonGraphics.drawPolygon(g, x, y);
		//DrawUtil.fillPolygon(x, y, wayDesc.lineColor, pc.g);
		PolygonGraphics.fillPolygon(pc.g, x, y);
		paintAreaName(pc,t);
	}

	public void paintAreaName(PaintContext pc, SingleTile t) {
		WayDescription wayDesc = C.getWayDescription(type);
		if (pc.scale > wayDesc.maxTextScale * Configuration.getDetailBoostMultiplier() ) {			
			return;
		}		
		
		if (wayDesc.hideable && !Configuration.getCfgBitState(Configuration.CFGBIT_AREATEXTS)) {
			return;
		}

		String name=null;
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE)) {
			name=wayDesc.description;
		} else {			
			if (nameIdx != -1) {
				name=Trace.getInstance().getName(nameIdx);
			}
		}
		// if zoomed in enough, show description 
		if (pc.scale < wayDesc.maxDescriptionScale) {
		// show waydescription
			if (name==null) {
				name=wayDesc.description;
			} else {
				name=name + " (" + wayDesc.description + ")";
			}
		}
		if (name == null)
			return;
		IntPoint lineP2 = pc.lineP2;
		Projection p = pc.getP();
		int x;
		int y;

		// get screen clip
		int clipX=pc.g.getClipX();
		int clipY=pc.g.getClipY();
		int clipMaxX=clipX+pc.g.getClipWidth();
		int clipMaxY=clipY+pc.g.getClipHeight();;

		// find center of area
		int minX=clipMaxX;
		int maxX=clipX;
		int minY=clipMaxY;
		int maxY=clipY;
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];			
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
			x = lineP2.x;
			y = lineP2.y;
			if (minX>x) minX=x;
			if (minY>y) minY=y;
			if (maxX<x) maxX=x;
			if (maxY<y) maxY=y;
		}
	
		// System.out.println("name:" + name + " ClipX:" + clipX + " ClipMaxX:" + clipMaxX + " ClipY:" + clipY + " ClipMaxY:" + clipMaxY + " minx:" + minX + " maxX:"+maxX + " miny:"+minY+ " maxY" + maxY);

		Font originalFont = pc.g.getFont();
		if (areaFont==null) {
			areaFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			areaFontHeight=areaFont.getHeight();
		}
		// find out how many chars of the name fit into the area
		int i=name.length()+1;
		int w;
		do {
			i--;
			w=areaFont.substringWidth(name,0,i);
		} while (w>(maxX-minX) && i>1);
		// is area wide enough to draw at least a dot into it?
		if ((maxX-minX)>=3 ) {
			pc.g.setColor(0,0,0);
			// if at least two chars have fit or name is a fitting single char, draw name
			if (i>1 || (i==name.length() && w<=(maxX-minX))  ) {
				pc.g.setFont(areaFont);
				// center vertically in area
				int y1=(minY+maxY-areaFontHeight)/2;
				// draw centered into area
				pc.g.drawSubstring(name, 0, i, (minX+maxX-w)/2, y1, Graphics.TOP | Graphics.LEFT);
				// if name fits not completely, append "..."
				if (i!=name.length()) {
					pc.g.drawString("...", (minX+maxX+w)/2, y1, Graphics.TOP | Graphics.LEFT);
				}
				pc.g.setFont(originalFont);
				// else draw a dot to indicate there's a name for this area available
			} else {
				pc.g.drawRect((minX+maxX)/2, (minY+maxY)/2, 0, 0 );
			}
		}		
	}
	
	
	public void setColor(PaintContext pc) {		
		WayDescription wayDesc = C.getWayDescription(type);
		pc.g.setStrokeStyle(wayDesc.lineStyle);
		if ((pc.target != null) && (pc.target.e == this)) {
			/*Color the target way red */			
			pc.g.setColor(0x00ff0000);
		} else {
			pc.g.setColor(wayDesc.lineColor);
		}
	}

	public int getWidth(PaintContext pc) {
		WayDescription wayDesc = C.getWayDescription(type);
		return wayDesc.wayWidth;
	}

	public void setBorderColor(PaintContext pc) {
		pc.g.setStrokeStyle(Graphics.SOLID);
		WayDescription wayDesc = C.getWayDescription(type);
		pc.g.setColor(wayDesc.boardedColor);
	}
	
	public boolean isOneway(){
		return ((flags & WAY_ONEWAY) == WAY_ONEWAY);
	}
	
	public boolean isArea() {
		return ((flags & WAY_AREA) > 0);
	}

	public boolean isRoundAbout() {
		return ((flags & WAY_ROUNDABOUT) > 0);
	}
	public boolean isTunnel() {
		return ((flags & WAY_TUNNEL) > 0);
	}
	public boolean isBridge() {
		return ((flags & WAY_BRIDGE) > 0);
	}
	
	public int getMaxSpeed() {
		return ((flags & MaxSpeedMask) >> MaxSpeedShift);
	}
	
/*	private float[] getFloatNodes(SingleTile t, short[] nodes, float offset) {
	    float [] res = new float[nodes.length];
	    for (int i = 0; i < nodes.length; i++) {
		res[i] = nodes[i]*SingleTile.fpminv + offset;
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
			lat[i] = nodes[path[i]]*SingleTile.fpminv + offset;
		}
		return lat;
	}
	
	public String toString() {
		return "Way " + Trace.getInstance().getName(nameIdx) + " type: " +  C.getWayDescription(type).description;
	}
}
