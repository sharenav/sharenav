/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying 
 */

package de.ueller.midlet.gps;

import java.io.InputStream;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.ImageTools;
import de.ueller.gps.tools.intTree;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Proj2D;
import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.ConnectionWithNode;
import de.ueller.midlet.gps.routing.RouteHelper;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.routing.TravelMode;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.WayDescription;

public class RouteInstructions {
	private static final String[] directions  = { "mark",
		"hard right", "right", "half right",
		"bear right", "straight on", "bear left",
		"half left", "left", "hard left", "u-turn",
		"Target reached",
		"enter motorway", "leave motorway",
		"cross area", "leave area",
		"Roundabout exit #1", "Roundabout exit #2", "Roundabout exit #3",
		"Roundabout exit #4", "Roundabout exit #5", "Roundabout exit #6",
		"into tunnel", "out of tunnel", "skip"
	};
	private static final String[] soundDirections  = { "",
		"HARD;RIGHT", "RIGHT", "HALF;RIGHT",
		"BEAR;RIGHT", "STRAIGHTON", "BEAR;LEFT",
		"HALF;LEFT", "LEFT", "HARD;LEFT", "UTURN",
		"TARGET_REACHED",
		"ENTER_MOTORWAY", "LEAVE_MOTORWAY",
		"AREA_CROSS", "AREA_CROSSED",
		"RAB;1ST;RABEXIT", "RAB;2ND;RABEXIT", "RAB;3RD;RABEXIT",
		"RAB;4TH;RABEXIT", "RAB;5TH;RABEXIT", "RAB;6TH;RABEXIT",
		"INTO_TUNNEL", "OUT_OF_TUNNEL"
	};

	private static final int RI_NONE = 0;
	private static final int RI_HARD_RIGHT = 1;
	private static final int RI_RIGHT = 2;
	private static final int RI_HALF_RIGHT = 3;
	private static final int RI_BEAR_RIGHT = 4;
	private static final int RI_STRAIGHT_ON = 5;
	private static final int RI_BEAR_LEFT = 6;
	private static final int RI_HALF_LEFT = 7;
	private static final int RI_LEFT = 8;
	private static final int RI_HARD_LEFT = 9;
	private static final int RI_UTURN = 10;
	private static final int RI_TARGET_REACHED = 11;
	private static final int RI_ENTER_MOTORWAY = 12;
	private static final int RI_LEAVE_MOTORWAY = 13;
	private static final int RI_AREA_CROSS = 14;
	private static final int RI_AREA_CROSSED = 15;
	private static final int RI_1ST_EXIT = 16;
	private static final int RI_2ND_EXIT = 17;
	private static final int RI_3RD_EXIT = 18;
	private static final int RI_4TH_EXIT = 19;
	private static final int RI_5TH_EXIT = 20;
	private static final int RI_6TH_EXIT = 21;
	private static final int RI_INTO_TUNNEL = 22;
	private static final int RI_OUT_OF_TUNNEL = 23;
	private static final int RI_SKIPPED = 24;
	
	private int connsFound = 0;
	
	private static boolean checkDirectionSaid=false;
	public volatile static boolean initialRecalcDone=false;
	/**
	 * number of time the image collector has drawn the map when offRoute was detected
	 */
	public volatile static int icCountOffRouteDetected=0;
	
//	public static int riCounter = 0; 
	
	public static volatile boolean haveBeenOnRouteSinceCalculation = false;
	public static volatile int startDstToFirstArrowAfterCalculation = 0;
	
	private static int iPassedRouteArrow=0;
	private static int iInstructionSaidArrow = -1;
	private static int iPrepareInstructionSaidArrow = -1;
	private static int iFollowStreetInstructionSaidArrow = -1;
	private static int iNamedArrow = 0;
	private static String sLastInstruction = "";
	private static long lastArrowChangeTime = 0;

	private static Font routeFont;
	private static int routeFontHeight = 0;
	public static volatile int routeInstructionsHeight = 0;

	private static byte cachedPicArrow;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;
	private static Image scaledPict = null;

	private static String nameNow = null;
	private static String nameThen = null;

	private static Trace trace;
	private static Vector route;
	public static intTree routeLineTree;
	public static Boolean trueObject = new Boolean(true);
	
	private static PositionMark target;
	
	public volatile static int dstToRoutePath=1;
	public volatile static int routePathConnection=0;
	public volatile static int pathIdxInRoutePathConnection=0;

	// variables for determining against route path 
	private static int prevRoutePathConnection = 0;
	private static int prevPathIdxInRoutePathConnection = 0;
	private static int iBackwardCount = 0;
	private static long againstDirectionDetectedTime = 0;

	private	static int routeInstructionColor=0x00E6E6E6;
	
	public static float maxScaleLevelForRouteInstructionSymbols = 0;
	
	private final static Logger logger = Logger.getInstance(RouteInstructions.class,Logger.DEBUG);

	public RouteInstructions(Trace trace) {
		RouteInstructions.trace = trace;
		RouteInstructions.maxScaleLevelForRouteInstructionSymbols = 15000f * 1.5f * 1.5f * 1.5f;
	}
	
	public void newRoute(Vector route, PositionMark target) {
		RouteInstructions.routeLineTree = new intTree();
		RouteInstructions.route = route;
		RouteInstructions.target = target;
		iPassedRouteArrow=0;
		prevRoutePathConnection = 0;
		prevPathIdxInRoutePathConnection = 0;
		iBackwardCount = 0;
		againstDirectionDetectedTime = 0;
		GpsMid.mNoiseMaker.resetSoundRepeatTimes();		
		try {
			determineRoutePath();
		} catch (Exception e) {
			//#debug error
			logger.error("RI thread crashed unexpectadly with error " +  e.getMessage());
			e.printStackTrace();
		}			
	}
	
	public void determineRoutePath() throws Exception {
		PaintContext pc = new PaintContext(trace, null);
		connsFound=0;
		float routeLen=0f;
		long startTime = System.currentTimeMillis();
		pc.searchConPrevWayRouteFlags = 0;
		if (route != null && route.size() > 1){
			for (int i=0; i<route.size()-1; i++){
				routeLen += searchConnection2Ways(pc, i);
			}
			//parent.alert ("Connection2Ways", "found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms", 3000);
			//#debug debug
			logger.debug("Connection2Ways found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms");
			trace.receiveMessage ("Route: " + (int) routeLen + "m" + (connsFound==(route.size()-1)?"":" (" + connsFound + "/" + (route.size()-1) + ")"));
			createRouteInstructions();
			outputRoutePath();
		}
	}
		
	public float searchConnection2Ways(PaintContext pc, int iConnFrom) throws Exception {		
		ConnectionWithNode cFrom;
		ConnectionWithNode cTo;
		cFrom = (ConnectionWithNode) route.elementAt(iConnFrom);
		// take a bigger angle for lon because of positions near to the pols.		
		Node nld=new Node(cFrom.to.lat - 0.0001f, cFrom.to.lon - 0.0005f,true);
		Node nru=new Node(cFrom.to.lat + 0.0001f,cFrom.to.lon + 0.0005f,true);
		pc.searchCon1Lat = cFrom.to.lat;
		pc.searchCon1Lon = cFrom.to.lon;
		cTo = (ConnectionWithNode) route.elementAt(iConnFrom+1);
		pc.searchCon2Lat = cTo.to.lat;
		pc.searchCon2Lon = cTo.to.lon;
		
		pc.searchLD=nld;
		pc.searchRU=nru;
		pc.conWayDistanceToNext = Float.MAX_VALUE;
		pc.xSize = 100;
		pc.ySize = 100;
		pc.conWayNumToRoutableWays = 0;
		pc.conWayNumMotorways = 0;
		// clear stored nameidxs
		pc.conWayNumNameIdxs = 0;
		pc.conWayNameIdxs.removeAll();
		pc.conWayBearings.removeAllElements();
		pc.setP(new Proj2D(new Node(pc.searchCon1Lat,pc.searchCon1Lon, true),5000,100,100));
		for (int i=0; i<4; i++){
			trace.t[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD | Tile.OPT_CONNECTIONS2WAY);
		}
		// if we've got a match
		if (pc.conWayDistanceToNext != Float.MAX_VALUE ) {
			cFrom = (ConnectionWithNode) route.elementAt(iConnFrom);
			cFrom.wayFromConAt = pc.conWayFromAt;
			cFrom.wayToConAt = pc.conWayToAt;
			cFrom.wayNameIdx = pc.conWayNameIdx;
			cFrom.wayType = pc.conWayType;
			cFrom.wayDistanceToNext = pc.conWayDistanceToNext;
			cFrom.wayRouteFlags |=  (pc.conWayRouteFlags & ~Legend.ROUTE_FLAG_COMING_FROM_ONEWAY);
			cTo.wayRouteFlags |= (pc.conWayRouteFlags & Legend.ROUTE_FLAG_COMING_FROM_ONEWAY);
			pc.searchConPrevWayRouteFlags = cFrom.wayRouteFlags;
			cFrom.numToRoutableWays = pc.conWayNumToRoutableWays;
			cTo.wayConStartBearing = pc.conWayStartBearing;
			cTo.wayConEndBearing = pc.conWayEndBearing;
			if (Math.abs(cTo.wayConEndBearing - cTo.endBearing) > 3) {
				cFrom.wayRouteFlags |= Legend.ROUTE_FLAG_INCONSISTENT_BEARING;				
			}
			if (Math.abs(cTo.wayConStartBearing - cTo.startBearing) > 3) {
				cTo.wayRouteFlags |= Legend.ROUTE_FLAG_INCONSISTENT_BEARING;				
			}
			
//			System.out.println(iConnFrom + ": " + cTo.wayConStartBearing);
			// check if we need a bearing instruction at this connection
			for (int b = 0; b < pc.conWayBearings.size(); b++) {
				Byte oBearing = (Byte) pc.conWayBearings.elementAt(b);
				byte bearing = oBearing.byteValue();
//				System.out.println(bearing);
				
				if (cTo.wayConStartBearing != bearing) {					
					byte riReal = convertTurnToRouteInstruction( (cTo.wayConStartBearing - cFrom.wayConEndBearing) * 2 );
					byte riCheck = convertTurnToRouteInstruction( (bearing - cFrom.wayConEndBearing) * 2 );
					// if we got a second straight-on way at the connection, we need to tell the bearing
					if (
						(riReal == RI_STRAIGHT_ON && riCheck == RI_STRAIGHT_ON)
						// if there's exactly one alternative to leave/enter the motorway don't add the bearing
						&& pc.conWayNumMotorways != 1
					) {
						int iBearing = (int) (bearing) + 180;
						if ((int) (cTo.wayConStartBearing) + 180 < iBearing) {
							cFrom.wayRouteFlags |= Legend.ROUTE_FLAG_BEAR_LEFT;
						} else {
							cFrom.wayRouteFlags |= Legend.ROUTE_FLAG_BEAR_RIGHT;							
						}
					}
				}				
			}		

			// get ways with same names leading away from the connection
			int iNumWaysWithThisNameConnected = 99;
			if (pc.conWayNameIdx >= 0) { // only valid name idxs
				Integer oNum = (Integer) (pc.conWayNameIdxs.get(pc.conWayNameIdx));
				if (oNum != null) { 
					iNumWaysWithThisNameConnected = oNum.intValue();
				}
			}
			if (iNumWaysWithThisNameConnected > 1) {
				cFrom.wayRouteFlags |= Legend.ROUTE_FLAG_LEADS_TO_MULTIPLE_SAME_NAMED_WAYS;
			}
			//System.out.println(iConnFrom + ": " + iNumWaysWithThisNameConnected);
			
			connsFound++;
		} else {
			// if we had no way match, look for an area match
//			System.out.println("search AREA MATCH FOR: " + iConnFrom);
			for (int i=0; i<4; i++){
				trace.t[i].walk(pc, Tile.OPT_WAIT_FOR_LOAD | Tile.OPT_CONNECTIONS2AREA);
			}
			// if we've got an area match
			if (pc.conWayDistanceToNext != Float.MAX_VALUE ) {
				cFrom = (ConnectionWithNode) route.elementAt(iConnFrom);
				cFrom.wayFromConAt = pc.conWayFromAt;
				cFrom.wayToConAt = pc.conWayToAt;
				cFrom.wayNameIdx = pc.conWayNameIdx;
				cFrom.wayType = pc.conWayType;
				cFrom.wayDistanceToNext = pc.conWayDistanceToNext;
				cFrom.wayRouteFlags |= Legend.ROUTE_FLAG_AREA;				
				System.out.println("AREA MATCH FOR: " + iConnFrom);
				connsFound++;
			} else {
				System.out.println("NO MATCH FOR: " + iConnFrom);
				return 0f;
			}
		}
		routeLineTree.put(pc.conWayCombinedFileAndWayNr, trueObject);
		return cFrom.wayDistanceToNext;
	}

	public static boolean isWayIdUsedByRouteLine(int wayId) {
		return (routeLineTree != null && routeLineTree.get(wayId) != null);
	}

	public void showRoute(PaintContext pc, PositionMark source, Node center) {
		/*	PASSINGDISTANCE is the distance when a routing arrow
			is considered to match to the current position.
			We currently can't adjust this value according to the speed
			because if we would be slowing down during approaching the arrow,
			then PASSINGDISTANCE could become smaller than the distance
			to the arrow due and thus the routines would already use the
			next arrow for routing assistance
		*/
		final int PASSINGDISTANCE=25;
		Node areaStart = new Node();
		int iAreaStart = 0;
		boolean drawRouteInstructionSymbols = (pc.scale <= RouteInstructions.maxScaleLevelForRouteInstructionSymbols);
		
		try {
			StringBuffer soundToPlay = new StringBuffer();
			StringBuffer sbRouteInstruction = new StringBuffer();
	    	// backgound colour for standard routing instructions
			byte soundRepeatDelay=3;
			byte soundMaxTimesToPlay=2;
			boolean drawQuietArrows = ! Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_HIDE_QUIET_ARROWS);
			
			// this makes the distance when prepare-sound is played depending on the speed
			int PREPAREDISTANCE=100;
			int speed=trace.speed;
			if (speed>100) {
				PREPAREDISTANCE=500;							
			} else if (speed>80) {
				PREPAREDISTANCE=300;							
			} else if (speed>55) {
				PREPAREDISTANCE=200;							
			} else if (speed>45) {
				PREPAREDISTANCE=150;							
			} else if (speed>35) {
				PREPAREDISTANCE=125;													
			}
			
			ConnectionWithNode c;
			ConnectionWithNode cNow = null;
			ConnectionWithNode cThen =null;
			Vector routeNodes = trace.getRouteNodes();
			// Show helper nodes for Routing
			for (int x=0; x<routeNodes.size();x++){
				RouteHelper n=(RouteHelper) routeNodes.elementAt(x);
				pc.getP().forward(n.node.radlat, n.node.radlon, pc.lineP2);
				pc.g.drawRect(pc.lineP2.x-5, pc.lineP2.y-5, 10, 10);
				pc.g.drawString(n.name, pc.lineP2.x+7, pc.lineP2.y+5, Graphics.BOTTOM | Graphics.LEFT);
			}

			boolean routeRecalculationRequired=false;
			float remainingDistance = 0;
			synchronized(this) {
				if (route != null && route.size() > 0){
					//#debug debug
					logger.debug("showRoute - route.size(): " + route.size() + " routePathConnection: " + routePathConnection);						
					
					// there's a route so no calculation required
					routeRecalculationRequired=false;
		
					for (int i=1; i<route.size();i++){
						c = (ConnectionWithNode) route.elementAt(i);
						if (c == null){
							logger.error("show Route got null connection");
							break;
						}
						if (c.to == null){
							logger.error("show Route got connection with NULL as target");
							break;
						}
						if (pc == null){
							logger.error("show Route strange pc is null");
							break;
						}
						if (c.wayRouteInstruction == RI_AREA_CROSS) {
							areaStart.setLatLon(c.to.lat, c.to.lon, true);
						} else if (c.wayRouteInstruction == RI_AREA_CROSSED) {
							IntPoint lineP1 = new IntPoint();
							IntPoint lineP2 = new IntPoint();							
							pc.getP().forward(areaStart.radlat, areaStart.radlon, lineP1);
							pc.getP().forward(c.to.lat, c.to.lon, lineP2);
							int dst = pc.getDstFromSquareDst( MoreMath.ptSegDistSq(lineP1.x, lineP1.y,
									lineP2.x, lineP2.y, pc.xSize / 2, pc.ySize / 2));		
//							System.out.println("Area dst:" + dst  + " way: " + dstToRoutePath);
							if (dst < dstToRoutePath) {
								routePathConnection = i - 1;
								dstToRoutePath = dst;
//								System.out.println("Area is closest");
							}							
						}
					}
					
					int iNow=0;
					int iRealNow=0;
					byte aNow=RI_NONE;
					int iThen=0;
					byte aThen=RI_NONE;
					byte aPaint=RI_NONE;
					double distNow=0;
					int intDistNow=0;
					if (routePathConnection != -1 && routePathConnection < route.size()-1) {
						iRealNow = routePathConnection+1;
						iNow = idxNextInstructionArrow (iRealNow);
						cNow = (ConnectionWithNode) route.elementAt(iNow);
						aNow = cNow.wayRouteInstruction;
						/* TODO: we are cheating a bit here as we add the distance to the next visible route connection as the crow flies
							We should rather take the distances of full segs to the current route node plus the distance of the divided seg from Way.processPath()
						*/
				    	ConnectionWithNode cRealNow = (ConnectionWithNode) route.elementAt(iRealNow);
				    	double distRealNow=ProjMath.getDistance(center.radlat, center.radlon, cRealNow.to.lat, cRealNow.to.lon);
				    	remainingDistance += distRealNow;

				    	distNow=ProjMath.getDistance(center.radlat, center.radlon, cNow.to.lat, cNow.to.lon);
						intDistNow=new Double(distNow).intValue();
						if (iNow < route.size() - 1) {
							iThen = idxNextInstructionArrow (iNow+1);
							cThen = (ConnectionWithNode) route.elementAt(iThen);
							if (cThen==null) {
								//#debug debug
								logger.debug("cThen is NULL connection");
							} else {
								aThen = cThen.wayRouteInstruction;
							}
						}
						if (
								iNamedArrow != iNow
							) {
							nameNow = null;
							nameThen = null;
							iNamedArrow = iNow;
						}
						// get name for next street
						nameNow=getInstructionWayName(iNow);
						// start searching for the 2nd next street for having it in the cache when needed
						if (nameThen == null && iThen != 0) {
							String name = getInstructionWayName(iThen);
						}
						//#debug debug
						logger.debug("showRoute - iRealNow: " + iRealNow + " iNow: " + iNow + " iThen: " + iThen);						
					}					
					
					for (int i=1; i<route.size();i++){
						c = (ConnectionWithNode) route.elementAt(i);						
						
						// calculate distance to target
						if (i >= iRealNow && c.wayDistanceToNext != Float.MAX_VALUE) {
							remainingDistance += c.wayDistanceToNext;
						}
						
						// draw cross area instruction
						if (c.wayRouteInstruction == RI_AREA_CROSS) {
							areaStart.setLatLon(c.to.lat, c.to.lon, true);
						} else if (c.wayRouteInstruction == RI_AREA_CROSSED) {
							// draw line for crossing area
							IntPoint lineP1 = new IntPoint();
							IntPoint lineP2 = new IntPoint();							
							pc.getP().forward(areaStart.radlat, areaStart.radlon, lineP1);
							pc.getP().forward(c.to.lat, c.to.lon, lineP2);
							pc.g.setStrokeStyle(Graphics.SOLID);
							if (iNow > i) {
								pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_PRIOR_ROUTELINE]);														
							} else if (iNow < i) {
								pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE]);														
							}
							if (iNow != i) {
								// we are currently not crossing the area, so we simply draw a line in the given color
						    	pc.g.drawLine(lineP1.x, lineP1.y, lineP2.x, lineP2.y);
							} else {
								// we are currently crossing the area, so we need to divide the line and draw a route dot onto it
								IntPoint centerP = new IntPoint();
								pc.getP().forward(center.radlat, center.radlon, centerP);
								IntPoint closestP = MoreMath.closestPointOnLine(lineP1.x, lineP1.y, lineP2.x, lineP2.y, centerP.x, centerP.y);
								pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_PRIOR_ROUTELINE]);														
						    	pc.g.drawLine(lineP1.x, lineP1.y, closestP.x, closestP.y);
								pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE]);														
						    	pc.g.drawLine(closestP.x, closestP.y, lineP2.x, lineP2.y);
						    	drawRouteDot(pc.g, closestP, Configuration.getMinRouteLineWidth());
							}
						}

						// no off-screen check for current route arrow
						if(i!=iNow) {
							if (c.to.lat < pc.getP().getMinLat()) {
								continue;
							}
							if (c.to.lon < pc.getP().getMinLon()) {
								continue;
							}
							if (c.to.lat > pc.getP().getMaxLat()) {
								continue;
							}
							if (c.to.lon > pc.getP().getMaxLon()) {
								continue;
							}
						}
	
						Image pict = pc.images.IMG_MARK; aPaint=0;
						aPaint = c.wayRouteInstruction;
						switch (aPaint) {
							case RI_HARD_RIGHT:		pict=pc.images.IMG_HARDRIGHT; break;
							case RI_RIGHT:			pict=pc.images.IMG_RIGHT; break;
							case RI_BEAR_RIGHT:
							case RI_HALF_RIGHT:		pict=pc.images.IMG_HALFRIGHT; break;
							case RI_STRAIGHT_ON:	pict=pc.images.IMG_STRAIGHTON; break;
							case RI_BEAR_LEFT:
							case RI_HALF_LEFT:		pict=pc.images.IMG_HALFLEFT; break;
							case RI_LEFT:			pict=pc.images.IMG_LEFT; break;
							case RI_HARD_LEFT:		pict=pc.images.IMG_HARDLEFT; break;
							case RI_UTURN:			pict=pc.images.IMG_UTURN; break;
//							case RI_BEAR_LEFT:
//							case RI_BEAR_RIGHT:		pict=pc.images.IMG_STRAIGHTON;
//													if (
//														(c.wayRouteFlags & (Legend.ROUTE_FLAG_BEAR_LEFT + Legend.ROUTE_FLAG_BEAR_RIGHT)) > 0
//														&& i < route.size()-1
//													) {
//														ConnectionWithNode cNext = (ConnectionWithNode) route.elementAt(i+1);  
//														int turn = (int) ((cNext.wayConStartBearing - c.wayConEndBearing) * 2); 
//														if (turn > 180) turn -= 360;
//														if (turn < -180) turn += 360;
//														if (Math.abs(turn) > 5) {
//															if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_BEAR_LEFT) > 0) {
//																pict=pc.images.IMG_HALFLEFT;
//															} else {
//																pict=pc.images.IMG_HALFRIGHT;
//															}
//														}
//													}
//													break;
							case RI_ENTER_MOTORWAY:	pict=pc.images.IMG_MOTORWAYENTER; break;
							case RI_LEAVE_MOTORWAY:	pict=pc.images.IMG_MOTORWAYLEAVE; break;					
							case RI_INTO_TUNNEL:	pict=pc.images.IMG_TUNNEL_INTO; break;
							case RI_OUT_OF_TUNNEL:	pict=pc.images.IMG_TUNNEL_OUT_OF; break;					
						}
						
						if (trace.atTarget) {
							aPaint=RI_TARGET_REACHED;
						}
						pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
						// handle current arrow
						if (i == iNow && aNow != RI_NONE) {
						    // scale nearest arrow
							if (aNow!=cachedPicArrow || scaledPict == null) {
								cachedPicArrow=aNow;							
								if (aNow < RI_ENTER_MOTORWAY) {
									scaledPict=doubleImage(pict); //ImageTools.scaleImage(pict, pict.getWidth() * 3 / 2, pict.getHeight() * 3 / 2);
								} else {
									scaledPict=pict;
								}
							}
							pict=scaledPict;						
					    	
					    	sbRouteInstruction.append(getInstruction(cNow.wayRouteFlags, aNow));					    	
					    	
					    	if (intDistNow>=PASSINGDISTANCE && !checkDirectionSaid) {
								//System.out.println("iNow :" + iNow + " iPassedRA: " + iPassedRouteArrow + " prepareSaidArrow: " + iPrepareInstructionSaidArrow + " iNamedArrow: " + iNamedArrow);
								if (
									intDistNow <= PREPAREDISTANCE
									// give prepare instruction only if the last prepareInstruction was not already for this arrow (this avoids possibly wrong prepare instructions after passing the arrow)
									&& iNow != iPrepareInstructionSaidArrow
									&& iNow != iInstructionSaidArrow
									&& intDistNow <= Configuration.getTravelMode().maxPrepareMeters
									
								) {
									if (aNow < RI_ENTER_MOTORWAY) {
										soundToPlay.append( (aNow==RI_STRAIGHT_ON ? "CONTINUE" : "PREPARE") + ";" + soundDirections[aNow]);
									} else if (aNow>=RI_ENTER_MOTORWAY && aNow<=RI_LEAVE_MOTORWAY) {
										soundToPlay.append("PREPARE;TO;" + getSoundInstruction(cNow.wayRouteFlags, aNow));
									} else if (aNow==RI_AREA_CROSS || aNow==RI_AREA_CROSSED) {
										soundToPlay.append("PREPARE;TO;" + getSoundInstruction(cNow.wayRouteFlags, aNow));
									} else if (aNow>=RI_1ST_EXIT && aNow<=RI_6TH_EXIT) {
										soundToPlay.append(getSoundInstruction(cNow.wayRouteFlags, aNow));
									}
									soundMaxTimesToPlay=1;
									// Because of adaptive-to-speed distances for "prepare"-instructions
									// GpsMid could fall back from "prepare"-instructions to "in xxx metres" voice instructions
									// Remembering and checking if the prepare instruction already was given for an arrow avoids this
									iPrepareInstructionSaidArrow = iNow;
								} else if (
									intDistNow >=100 && intDistNow < 900 && intDistNow < getTellDistance(iNow, aNow)
									// give in-xxx-m instruction only if the last prepareInstruction was not already for this arrow (this avoids possibly wrong in-xxx-m instructions after passing the arrow)
									&& iNow != iPrepareInstructionSaidArrow
									&& iNow != iInstructionSaidArrow
									&& intDistNow <= Configuration.getTravelMode().maxInMeters
								) {
									soundRepeatDelay=60;
									soundToPlay.append("IN;" + Integer.toString(intDistNow / 100)+ "00;METERS;" + getSoundInstruction(cNow.wayRouteFlags, aNow));								
								} else if (
										// follow-street instruction
										intDistNow > 1200
										&& iNow != iFollowStreetInstructionSaidArrow
										&& haveBeenOnRouteSinceCalculation
										&& dstToRoutePath < 25
										&& (System.currentTimeMillis() - lastArrowChangeTime) > 7000 
								) {
									// only tell "follow street" if we are at least 50 meters away from the previous arrow
									// this avoids tell follow street too early if the gps signal gets inaccurate while waiting at a crossing
									ConnectionWithNode cPassed = (ConnectionWithNode) route.elementAt(iPassedRouteArrow);
							    	float distPassed=ProjMath.getDistance(center.radlat, center.radlon, cPassed.to.lat, cPassed.to.lon);
									if (distPassed > 50) {
								    	soundToPlay.append("FOLLOW_STREET");
								    	iFollowStreetInstructionSaidArrow = iNow;
									}
								}
					    	}
							
					    	if (
								iPassedRouteArrow != iNow
							) {
								iPassedRouteArrow = iNow;
								iFollowStreetInstructionSaidArrow = -1;
								lastArrowChangeTime = System.currentTimeMillis();
								// if there's e.g. a 2nd left arrow in row "left" must be repeated
								sLastInstruction="";
							}
							// if nearest route arrow is closer than PASSINGDISTANCE meters we're currently passing this route arrow
							if (intDistNow < PASSINGDISTANCE) {
								if (iInstructionSaidArrow != iNow) { 
									soundToPlay.append (getSoundInstruction(cNow.wayRouteFlags, aNow));
							    	iInstructionSaidArrow = iNow;
									soundMaxTimesToPlay=1;
								}
							} else {
								sbRouteInstruction.append(" in " + intDistNow + "m");
							}
							
							// when we have a then instruction and this is not a follow street instruction
							if (cThen != null && intDistNow < 900) {
								double distNowThen=ProjMath.getDistance(cNow.to.lat, cNow.to.lon, cThen.to.lat, cThen.to.lon);
								// if there is a close direction arrow after the current one
								// inform the user about its direction
								if (distNowThen <= PREPAREDISTANCE &&
									// only if it is not a round about exit instruction
									!(aNow>=RI_1ST_EXIT && aNow<=RI_6TH_EXIT) &&
									// and only as continuation of instruction
									soundToPlay.length()!=0
								   ) {
									soundToPlay.append(";THEN;");
									if (distNowThen > PASSINGDISTANCE) {
										soundToPlay.append("SOON;");
									}
									soundToPlay.append(getSoundInstruction(cThen.wayRouteFlags, aThen));
									// same arrow as currently nearest arrow?
									if (aNow==aThen) {
										soundToPlay.append(";AGAIN");							
									}
									
									//System.out.println(soundToPlay.toString());
								}
							}
						}
					    
					    if (drawRouteInstructionSymbols && (drawQuietArrows || (c.wayRouteFlags & Legend.ROUTE_FLAG_QUIET) == 0) ) {
						    if (aPaint == RI_SKIPPED) {
								pc.g.setColor(0x00FDDF9F);
								pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
								final byte radius=6;
								pc.g.fillArc(pc.lineP2.x-radius/2,pc.lineP2.y-radius/2,radius,radius,0,359);
							} else {
								//#debug debug
								if (pict==null) logger.debug("got NULL pict");													
								if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_INVISIBLE) == 0 ) {
									pc.g.drawImage(pict,pc.lineP2.x,pc.lineP2.y,CENTERPOS);
//									pc.g.setColor(0x0);
//									pc.g.drawString("" + i, pc.lineP2.x+7, pc.lineP2.y+5, Graphics.BOTTOM | Graphics.LEFT);
								}
							}
					    }
					    
						// display bearings for debugging
						if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BEARINGS)) {					
							// end bearings
							pc.g.setStrokeStyle(Graphics.SOLID);
							drawBearing(pc, pc.lineP2.x,pc.lineP2.y, c.endBearing, true, 0x00800000);
							pc.g.setStrokeStyle(Graphics.DOTTED);
							drawBearing(pc, pc.lineP2.x,pc.lineP2.y, c.wayConEndBearing, true, 0x00FF0000);							
							byte startBearingCon = 0;
							byte startBearingWay = 0;
							if ( i < route.size()-1 ) {
								ConnectionWithNode cNext = (ConnectionWithNode) route.elementAt(i + 1);
								startBearingCon = cNext.startBearing;
								startBearingWay = cNext.wayConStartBearing;
							}
							// start bearings
							pc.g.setStrokeStyle(Graphics.SOLID);
							drawBearing(pc, pc.lineP2.x,pc.lineP2.y, startBearingCon, false, 0x00008000);
							pc.g.setStrokeStyle(Graphics.DOTTED);
							drawBearing(pc, pc.lineP2.x,pc.lineP2.y, startBearingWay, false, 0x0000FF00);
							if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_INCONSISTENT_BEARING) > 0) {
								// draw red circle aroud inconsistent bearing
								pc.g.setColor(0x00FF6600);
								pc.g.setStrokeStyle(Graphics.SOLID);
								final byte radius=40;
								pc.g.drawArc(pc.lineP2.x-radius/2,pc.lineP2.y-radius/2,radius,radius,0,359);
							}
						}
					}
					routeRecalculationRequired = isOffRoute(route, center);
					if (trace.atTarget || (aNow == RI_TARGET_REACHED && intDistNow < PASSINGDISTANCE)) {
						routeInstructionColor = Legend.COLORS[Legend.COLOR_RI_AT_TARGET];
					} else if ( routeRecalculationRequired && trace.gpsRecenter) {
						//#debug debug
						logger.debug("off route detected");												
						if (icCountOffRouteDetected == 0) {
							// remember number of recalcs so we can check in RouteInstructions if the new source way must have been determined in the meanwhile
							icCountOffRouteDetected = ImageCollector.createImageCount;
						/*
						 * if at least 1-2 times the map image has been collected since detecting off-route
						 * we can be sure the source way has been updated with one at the new position
						 */ 
						} else if (ImageCollector.createImageCount > icCountOffRouteDetected + 1) {
							soundToPlay.setLength(0);
							trace.autoRouteRecalculate();
							icCountOffRouteDetected = 0;
						}
					} else if (checkAgainstDirection()) {
						soundToPlay.setLength(0);
						soundToPlay.append ("CHECK_DIRECTION");
						soundRepeatDelay = 15;
						nameNow = null;
						sbRouteInstruction.setLength(0);
						sbRouteInstruction.append("check direction");
						routeInstructionColor = Legend.COLORS[Legend.COLOR_RI_CHECK_DIRECTION];
;
					}
				}
			}

			// when routing was started at the target but the map has not been moved away from the target yet, give no voice instruction 
			if (trace.atTarget && ! trace.movedAwayFromTarget) {
				soundToPlay.setLength(0);
				// tell why there are no route instructions given yet
//				nameNow = null;
//				sbRouteInstruction.setLength(0);
//				if (!trace.gpsRecenter) {
//					sbRouteInstruction.append("Not Gps-Centered");
//				} else {
//					sbRouteInstruction.append("Routing waits for GPS");
//				}
//				routeInstructionColor = Legend.ROUTE_COLOR;				
			}
						
			//#debug debug
			logger.debug("complete route instruction: " + sbRouteInstruction.toString() + " (" + soundToPlay.toString() + ")");													
			
			// Route instruction text output
			LayoutElement e = Trace.tl.ele[TraceLayout.ROUTE_INSTRUCTION];
			if (sbRouteInstruction.length() != 0) {
				e.setBackgroundColor(routeInstructionColor);				
				e.setColor(Legend.COLORS[Legend.COLOR_RI_TEXT]);				
				e.setText(sbRouteInstruction.toString());

				e = Trace.tl.ele[TraceLayout.ROUTE_INTO];
				e.setBackgroundColor(routeInstructionColor);				
				e.setColor(Legend.COLORS[Legend.COLOR_RI_TEXT]);				
				if (nameNow != null) {
					e.setText("into " + nameNow);
				}				
				e = Trace.tl.ele[TraceLayout.ROUTE_OFFROUTE];
				e.setText("off:" + (dstToRoutePath == Integer.MAX_VALUE ? "???" : "" + dstToRoutePath) + "m");
				e = Trace.tl.ele[TraceLayout.ROUTE_DISTANCE];
				e.setText("dst:" + (int) remainingDistance + "m");
			}
			// Route instruction sound output
			if (soundToPlay.length()!=0 && Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
				if ( !sLastInstruction.equals( soundToPlay.toString() ) ) {
					sLastInstruction = soundToPlay.toString();
					GpsMid.mNoiseMaker.playSound(sLastInstruction, (byte) soundRepeatDelay, (byte) soundMaxTimesToPlay);
					System.out.println(sLastInstruction);
				}
			}
		} catch (Exception e) {
			logger.silentexception("Unhandled exception in showRoute()", e);
		}
	}

	private String getSoundInstruction(short routeFlags, byte aNow) {
		StringBuffer sb = new StringBuffer();
		// prefix motorway instructions
		if (aNow == RI_ENTER_MOTORWAY || aNow == RI_LEAVE_MOTORWAY) {
			if ( (routeFlags & Legend.ROUTE_FLAG_BEAR_LEFT) > 0) {
				sb.append("BEAR;LEFT;TO;");
			}
			if ( (routeFlags & Legend.ROUTE_FLAG_BEAR_RIGHT) > 0) {
				sb.append("BEAR;RIGHT;TO;");
			}
		}
		sb.append (soundDirections[aNow]);
		return sb.toString();
	}

	private String getInstruction(short routeFlags, byte aNow) {
		StringBuffer sb = new StringBuffer();
		// prefix motorway instructions
		if (aNow == RI_ENTER_MOTORWAY || aNow == RI_LEAVE_MOTORWAY) {
			if ( (routeFlags & Legend.ROUTE_FLAG_BEAR_LEFT) > 0) {
				sb.append("b.left ");
			}
			if ( (routeFlags & Legend.ROUTE_FLAG_BEAR_RIGHT) > 0) {
				sb.append("b.right ");
			}
		}
		sb.append(directions[aNow]);	
		return sb.toString();
	}

	public static void drawRouteDot(Graphics g, IntPoint p, int radius) {
		g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTEDOT]);
		g.fillArc(p.x-radius, p.y-radius, radius*2, radius*2, 0, 360);
		g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTEDOT_BORDER]);
		g.drawArc(p.x-radius, p.y-radius, radius*2, radius*2, 0, 360);
	}
	
	
	private void drawBearing(PaintContext pc, int posX, int posY, byte halfBearing, boolean isStartBearing, int color) {
		pc.g.setColor(color);
		float radc = (float) (halfBearing * Math.PI / 90d);
		int dx = (int) (Math.sin(radc) * 20);
		int dy = (int) (Math.cos(radc) * 20);
		if (isStartBearing) {
			pc.g.drawLine(posX, posY, posX - dx, posY + dy);
		} else {
			pc.g.drawLine(posX, posY, posX + dx, posY - dy);			
		}
	}
	
	private boolean isOffRoute(Vector route, Node center) {
		// never recalculate during route calculation
		if (trace.routeCalc) return false;

		/* if we did no initial recalculation,
		 * the map is gpscentered
		 * and we just moved away from target
		 * ==> initial recalculation
		 */
		if (!initialRecalcDone && trace.gpsRecenter && trace.movedAwayFromTarget
		) {
			initialRecalcDone = true;
			return true;
		}
		
//		System.out.println("Counter: " + riCounter++);
		if (dstToRoutePath < 25) {
		    // green background color if onroute
	    	routeInstructionColor=Legend.COLORS[Legend.COLOR_RI_ON_ROUTE];
			haveBeenOnRouteSinceCalculation=true;
//			System.out.println("on route dstToRoutePath: " + dstToRoutePath);
		}
		ConnectionWithNode c0 = (ConnectionWithNode) route.elementAt(0);
		//#debug debug
		if (c0==null) logger.debug("isOffRoute got NULL connection");

		// calculate distance to first arrow after calculation
		int dstToFirstArrow = (int) (ProjMath.getDistance(center.radlat, center.radlon, c0.to.lat, c0.to.lon));
		if ((haveBeenOnRouteSinceCalculation && dstToRoutePath >= 50) ||
			(!haveBeenOnRouteSinceCalculation && (dstToFirstArrow - startDstToFirstArrowAfterCalculation) > 50)
		) {
			// use red background color
			routeInstructionColor=Legend.COLORS[Legend.COLOR_RI_OFF_ROUTE_FULL];
			//#mdebug debug
			logger.debug("=== Off Route ===");
			logger.debug("recalc startDst: " + startDstToFirstArrowAfterCalculation);
			logger.debug("recalc dst1st: " + dstToFirstArrow);
			logger.debug("haveBeenOnRouteSinceCalculation: " + haveBeenOnRouteSinceCalculation);
			logger.debug("dstToRoutePath: " + dstToRoutePath);
			//#enddebug
			return true;
		} else if (
			(haveBeenOnRouteSinceCalculation && dstToRoutePath >= 25) ||
			(!haveBeenOnRouteSinceCalculation && (dstToFirstArrow - startDstToFirstArrowAfterCalculation) > 25)
		) {
			// use orange background color
			routeInstructionColor=Legend.COLORS[Legend.COLOR_RI_OFF_ROUTE_SLIGHT];
		}		
		return false;
	}

	
	private static boolean checkAgainstDirection() {
		// going the route connections backward adds to the backward counter
		if (routePathConnection < prevRoutePathConnection) {
			iBackwardCount++;
		// going the route connections forward reduces the forward counter
		} else if (routePathConnection > prevRoutePathConnection) {
			iBackwardCount--;			
		// if the routePathConnection stayed the same but the segment inside the way changed
		// it depends on the direction of the route along the way if we go forward or backward 
		// along the route
		} else if (routePathConnection == prevRoutePathConnection) {
			ConnectionWithNode c = (ConnectionWithNode) route.elementAt(routePathConnection);
			// determine the direction of the route on the way
			// by checking if the route goes on the way forward or backward
			int addToCounter = (c.wayFromConAt <= c.wayToConAt) ? 1 : -1; 
			if (pathIdxInRoutePathConnection < prevPathIdxInRoutePathConnection) {			
				iBackwardCount += addToCounter;
			} else if (pathIdxInRoutePathConnection > prevPathIdxInRoutePathConnection) {			
				iBackwardCount -= addToCounter;
			}			
		}
		
		prevRoutePathConnection = routePathConnection; 
		prevPathIdxInRoutePathConnection = pathIdxInRoutePathConnection; 

		// handle when the counter got  below zero because going forward
		if (iBackwardCount < 0) {
			iBackwardCount = 0;
			// when we went forward, reset being against direction time
			againstDirectionDetectedTime = 0;
		// if we went twice backward without going forward in the meantime, assume we are against direction 
		} else if (iBackwardCount > 1) {		
    		iBackwardCount = 0;
    		againstDirectionDetectedTime = System.currentTimeMillis();
		}
		/* 
		 * Also return against direction if last detection was less than 5 secs ago.
		 * This will stop the route instructions code from giving other instructions
		 */
		if (Math.abs((System.currentTimeMillis() - againstDirectionDetectedTime)) < 5000) {			
			return true;
		}
		return false;
	}
	
	
	public static void resetOffRoute(Vector route, Node center) {
		haveBeenOnRouteSinceCalculation = false;
		dstToRoutePath=Integer.MAX_VALUE;
		if (route!=null && route.size() >= 2 ) {
			ConnectionWithNode c0 = (ConnectionWithNode) route.elementAt(1); // don't take arrow 0 as this is our dummy connection
			// calculate distance to first arrow after calculation
			startDstToFirstArrowAfterCalculation = (int) (ProjMath.getDistance(center.radlat, center.radlon, c0.to.lat, c0.to.lon));
		} else {
			startDstToFirstArrowAfterCalculation=50;
		}
		// dark grey
		routeInstructionColor=Legend.COLORS[Legend.COLOR_RI_NOT_AT_ROUTE_LINE_YET];
//		System.out.println("resetOffRoute: " + startDstToFirstArrowAfterCalculation);
	}

	
	public static Image doubleImage(Image original)
    {        
        int w=original.getWidth();
        int h=original.getHeight();
		int[] rawInput = new int[w * h];
        original.getRGB(rawInput, 0, w, 0, 0, h, h);
        
        int[] rawOutput = new int[w*h*4];        

        int outOffset= 1;
        int inOffset=  0;
        int lineInOffset=0;
        int val=0;
        
        for (int y=0;y<h*2;y++) {            
        	if((y&1)==1) {
        		outOffset++;
        		inOffset=lineInOffset;
        	} else {
        		outOffset--;
        		lineInOffset=inOffset;
        	}
        	for (int x=0; x<w; x++) {
        		/* unfortunately many devices can draw semitransparent
        		   pictures but only support transparent and opaque
        		   in their graphics routines. So we just keep full transparency
        		   as otherwise the device will convert semitransparent from the png
        		   to full transparent pixels making the new picture much too bright
        		*/
        		val=rawInput[inOffset];
            	if( (val & 0xFF000000) != 0 ) {
            		val|=0xFF000000;
            	}
            	rawOutput[outOffset]=val; 
                /// as a workaround for semitransparency we only draw every 2nd pixel
            	outOffset+=2;
                inOffset++;
            }            
        }               
        return Image.createRGBImage(rawOutput, w*2, h*2, true);        
    }

	public void createRouteInstructions() {
		ConnectionWithNode c;
		ConnectionWithNode c2;
		int nextStartBearing;
		int iAreaStart = 0;
		
		if (route.size() < 3) {
			return;
		}
		for (int i=0; i<route.size(); i++){
			c = (ConnectionWithNode) route.elementAt(i);

			short rfCurr=c.wayRouteFlags;
			short rfPrev=0;
			if (i > 0) {
				c2 = (ConnectionWithNode) route.elementAt(i-1);
				rfPrev=c2.wayRouteFlags;
			}			
			short rfNext=0;
			nextStartBearing = 0;
			if (i < route.size()-1) {
				c2 = (ConnectionWithNode) route.elementAt(i+1);
				rfNext=Legend.getWayDescription(c2.wayType).routeFlags;
				// nextStartBearing = c2.startBearing;
				nextStartBearing = c2.wayConStartBearing;
			}
			
			byte ri=0;
			// into tunnel
			if (	(rfPrev & Legend.ROUTE_FLAG_TUNNEL) == 0
				&& 	(rfCurr & Legend.ROUTE_FLAG_TUNNEL) > 0
			) {
				ri = RI_INTO_TUNNEL;
			}
			// out of tunnel
			if (	(rfPrev & Legend.ROUTE_FLAG_TUNNEL) > 0
				&& 	(rfCurr & Legend.ROUTE_FLAG_TUNNEL) == 0
			) {
				ri = RI_OUT_OF_TUNNEL;
			}
			// enter motorway
			if ( isEnterMotorway(rfPrev, rfCurr) ) {
				ri = RI_ENTER_MOTORWAY;
			}
			// leave motorway
			if ( isLeaveMotorway(rfPrev, rfCurr) ) {
				ri = RI_LEAVE_MOTORWAY;
			}
			// area start
			if ( isAreaStart(rfPrev, rfCurr) ) {
				ri = RI_AREA_CROSS;
				iAreaStart = i;
			}
			// areaEnd
			if ( isAreaEnd(rfPrev, rfCurr) ) {
				ri = RI_AREA_CROSSED;
				// calculate distance for area crossing as the crow flies
				ConnectionWithNode cAreaStart = (ConnectionWithNode) route.elementAt(iAreaStart);
				cAreaStart.wayDistanceToNext = ProjMath.getDistance(cAreaStart.to.lat, cAreaStart.to.lon, c.to.lat, c.to.lon);
			}

			// determine roundabout exit
			if ( 	(rfPrev & Legend.ROUTE_FLAG_ROUNDABOUT) == 0
				&& 	(rfCurr & Legend.ROUTE_FLAG_ROUNDABOUT) > 0
			) {
				ri = RI_1ST_EXIT;	
				int i2;
				for (i2=i+1; i2<route.size()-1 && (ri < RI_6TH_EXIT); i2++) {
					c2 = (ConnectionWithNode) route.elementAt(i2);
					if ( (c2.wayRouteFlags & Legend.ROUTE_FLAG_ROUNDABOUT) == 0 ) { 
						break;
					}
					// count only exits in roundabouts
					if (c2.numToRoutableWays > 1) {
						ri++;						
					} else {
						c2.wayRouteFlags |= Legend.ROUTE_FLAG_INVISIBLE;
					}
				}
				for (int i3=i2-1; i3>i; i3--) {
					c2 = (ConnectionWithNode) route.elementAt(i3);
					c2.wayRouteInstruction=ri;					
				}
				i=i2-1;				
			}
			// if we've got no better instruction, just use the direction
			if (ri==0) {				
				// ri = convertTurnToRouteInstruction( (nextStartBearing - c.endBearing) * 2 );
				ri = convertTurnToRouteInstruction( (nextStartBearing - c.wayConEndBearing) * 2 );
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_BEAR_LEFT) > 0 ) {
					ri = RI_BEAR_LEFT;
				}
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_BEAR_RIGHT) > 0 ) {
					ri = RI_BEAR_RIGHT;
				}
			}
			c.wayRouteInstruction = ri;
		}
		
		c = (ConnectionWithNode) route.elementAt(route.size()-1);
		c.wayRouteInstruction = RI_TARGET_REACHED;				
		
		// combine instructions that are closer than 25 m to the previous one into single instructions
		ConnectionWithNode cPrev = (ConnectionWithNode) route.elementAt(1);
		for (int i=2; i<route.size()-1; i++){
			c = (ConnectionWithNode) route.elementAt(i);
			// skip connections that are closer than 25 m to the previous one
			if( (i<route.size()-1 && ProjMath.getDistance(c.to.lat, c.to.lon, cPrev.to.lat, cPrev.to.lon) < 25)
				// only combine direction instructions
				&& (cPrev.wayRouteInstruction <= RI_HARD_LEFT && c.wayRouteInstruction <= RI_HARD_LEFT)
			)	{
				c.wayRouteInstruction = RI_SKIPPED;
				c.wayRouteFlags |= Legend.ROUTE_FLAG_QUIET;
				ConnectionWithNode cNext = (ConnectionWithNode) route.elementAt(i+1);
				// cPrev.wayRouteInstruction = convertTurnToRouteInstruction( (cNext.startBearing - cPrev.endBearing) * 2 );
				cPrev.wayRouteInstruction = convertTurnToRouteInstruction( (cNext.wayConStartBearing - cPrev.wayConEndBearing) * 2 );				
			}
			cPrev=c;
		}
		
		// replace redundant straight-ons and direction arrow with same name by quiet arrows and add way distance to starting arrow of the street
		ConnectionWithNode cStart;
		ConnectionWithNode cNext;
		int oldNameIdx = -2;
		for (int i=1; i<route.size()-1; i++){
			c = (ConnectionWithNode) route.elementAt(i);
			cStart = (ConnectionWithNode) route.elementAt(i-1);
			oldNameIdx = cStart.wayNameIdx;
			cNext = (ConnectionWithNode) route.elementAt(i+1);
			// set maximum value of connections that are allowed to be there for hiding this arrow
			int maxToRoutableWays = 2;
			// when we are coming from a one way arrow we must not count the way we are coming from 
			if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_COMING_FROM_ONEWAY) > 0) {
				maxToRoutableWays--;
			}
			while (	i < route.size()-1
					&&
					(
						// while straight on
						c.wayRouteInstruction == RI_STRAIGHT_ON
						||
//						(
//							// or no alternative to go to
//							(
//							 	c.numToRoutableWays <= maxToRoutableWays
//								&& c.wayRouteInstruction <= RI_HARD_LEFT
//								&& ((c.wayRouteFlags & Legend.ROUTE_FLAG_BEAR_LEFT+Legend.ROUTE_FLAG_BEAR_RIGHT) == 0)
//								// and the following arrow must not be a skipped arrow
//								&& cNext.wayRouteInstruction != RI_SKIPPED
//							)
//						)
//						|| 
						// or named direction arrow with same name and way type as previous one but not multiple same named options
						(
							(
								c.wayRouteInstruction == RI_HALF_LEFT
								||
								c.wayRouteInstruction == RI_HALF_RIGHT
							)
							&&
							c.wayNameIdx > 0
							&&
							c.wayNameIdx == oldNameIdx
							&&
							c.wayType == cStart.wayType
							&&
							(c.wayRouteFlags & Legend.ROUTE_FLAG_LEADS_TO_MULTIPLE_SAME_NAMED_WAYS) == 0
							// the following arrow must not be a skipped arrow or the same name and type must continue
							&& (
								cNext.wayRouteInstruction != RI_SKIPPED
								|| (
									cNext.wayNameIdx == cStart.wayNameIdx
									&& cNext.wayType == cStart.wayType
								)	
							)
						)
					)
				) {
				oldNameIdx = c.wayNameIdx;  // if we went straight on into a way with new name we continue comparing with the new name 
				c.wayRouteFlags |= Legend.ROUTE_FLAG_QUIET;
				i++;
				c = cNext;
				if (i < route.size()-1) {
					cNext = (ConnectionWithNode) route.elementAt(i+1);
				}
			}			
		}
		
		// reset arrow markers
		iNamedArrow = -1;
		iInstructionSaidArrow = -1;
		iPrepareInstructionSaidArrow = -1;
		iFollowStreetInstructionSaidArrow = -1;
	}
	
	
	/**
	 * 
	 * @param rfPrev - routeFlag of previous connection
	 * @param rfCurr - routeFlag of current connection
	 * @return
	 */
	public static boolean isEnterMotorway(short rfPrev, short rfCurr) {
		return (	(rfPrev & (Legend.ROUTE_FLAG_MOTORWAY | Legend.ROUTE_FLAG_MOTORWAY_LINK)) == 0
					&& 	(rfCurr & (Legend.ROUTE_FLAG_MOTORWAY | Legend.ROUTE_FLAG_MOTORWAY_LINK)) > 0
		);
	}
	
	/**
	 * 
	 * @param rfPrev - routeFlag of previous connection
	 * @param rfCurr - routeFlag of current connection
	 * @return
	 */
	public static boolean isLeaveMotorway(short rfPrev, short rfCurr) {
		return (	(rfPrev & Legend.ROUTE_FLAG_MOTORWAY) > 0
					&& 	(rfCurr & Legend.ROUTE_FLAG_MOTORWAY) == 0
		);
	}

	/**
	 * 
	 * @param rfPrev - routeFlag of previous connection
	 * @param rfCurr - routeFlag of current connection
	 * @return
	 */
	public static boolean isAreaStart(short rfPrev, short rfCurr) {
		return (	(rfPrev & Legend.ROUTE_FLAG_AREA) == 0
					&& 	(rfCurr & Legend.ROUTE_FLAG_AREA) > 0
		);
	}
	
	/**
	 * 
	 * @param rfPrev - routeFlag of previous connection
	 * @param rfCurr - routeFlag of current connection
	 * @return
	 */
	public static boolean isAreaEnd(short rfPrev, short rfCurr) {
		return (	(rfPrev & Legend.ROUTE_FLAG_AREA) > 0
					&& 	(rfCurr & Legend.ROUTE_FLAG_AREA) == 0
		);
	}

	
	
	
	private int getTellDistance(int iConnection, byte aNow) {
		ConnectionWithNode cPrev = (ConnectionWithNode) route.elementAt(iConnection -1);
		//#debug debug
		if (cPrev==null) logger.debug("getTellDistance got NULL connection");
		
		int distFromSpeed = 200;
		int distFromPrevConn = (int) cPrev.wayDistanceToNext + 50;
		int speed=trace.speed;
		if (speed>100 || aNow == RI_INTO_TUNNEL) {
			distFromSpeed=500;							
		} else if (speed>80 || aNow == RI_BEAR_LEFT || aNow == RI_BEAR_RIGHT) {
			distFromSpeed=400;							
		} else if (speed>40) {
			distFromSpeed=300;																			
		}
		return Math.max(distFromSpeed, distFromPrevConn);
	}
	
	
	private static int idxNextInstructionArrow(int i) {
		ConnectionWithNode c;
		int a;
		for (a=i; a<route.size()-1; a++){
			c = (ConnectionWithNode) route.elementAt(a);
			//#debug debug
			if (c==null) logger.debug("idxNextInstructionArrow got NULL connection");
			if (
				(c.wayRouteFlags & Legend.ROUTE_FLAG_QUIET) == 0
			&&	c.wayRouteInstruction != RI_SKIPPED
			) {
				break;
			}
		}
		if (a==route.size()) {
			a--;
		}
		return a;
	}

	private static int idxPrevInstructionArrow(int i) {
		ConnectionWithNode c;
		int a;
		for (a=i; a>=0; a--){
			c = (ConnectionWithNode) route.elementAt(a);
			//#debug debug
			if (c==null) logger.debug("idxPrevInstructionArrow got NULL connection");
			if (
				(c.wayRouteFlags & Legend.ROUTE_FLAG_QUIET) == 0
			&&	c.wayRouteInstruction != RI_SKIPPED
			) {
				break;
			}
		}
		return a;
	}

	/*
	 * get wayname for the current instruction -
	 * in roundabouts we have to search the first non-roundabout
	 * and if the next one is a skipped instruction we need to use the name from the skipped instruction
	 * as this contains the name of the way to go into
	 */
	 
	private String getInstructionWayName(int i) {
		// never display a way name for the final instruction
		if (i >= route.size()-1) {
			return null;
		}
		ConnectionWithNode c = (ConnectionWithNode) route.elementAt(i);
		ConnectionWithNode c2;
		int a;
		for (a=i; a<route.size()-2; a++){
			c = (ConnectionWithNode) route.elementAt(a);
			c2 = (ConnectionWithNode) route.elementAt(a+1);
			if (
				(c.wayRouteFlags & Legend.ROUTE_FLAG_ROUNDABOUT) == 0 
				&&
				c2.wayRouteInstruction != RI_SKIPPED
			) {
				break;
			}
		}
		if (c.wayNameIdx != -1) {
			return trace.getName(c.wayNameIdx);
		} else {
			WayDescription wayDesc = Legend.getWayDescription(c.wayType);
			return "(unnamed " + wayDesc.description + ")";
		}
	}

	
	public static void toNextInstruction(int direction) {
		if (routePathConnection != -1 && routePathConnection < route.size()-1) {
			int i;
			i=idxNextInstructionArrow (routePathConnection+1);			
			if (direction > 0) {
				i = idxNextInstructionArrow (i + 1);
			} else {
				i = idxPrevInstructionArrow (i - 1);				
			}
			ConnectionWithNode c = (ConnectionWithNode) route.elementAt(i);
			double rad=Math.toRadians((double) (c.wayConEndBearing*2));
			trace.center.setLatLon(	c.to.lat - 0.0000025f * (float) Math.cos(rad),
									c.to.lon - 0.0000025f * (float) Math.sin(rad),
									true
			);
			trace.gpsRecenter=false;
			// allow to output same instruction again
			GpsMid.mNoiseMaker.resetSoundRepeatTimes();
		}
	}
		
	private byte convertTurnToRouteInstruction(int turn) {
		if (turn > 180) turn -= 360;
		if (turn < -180) turn += 360;
		if (turn > 160) {
			return RI_UTURN;
		} else if (turn > 110) {
			return RI_HARD_RIGHT;
		} else if (turn > 70){
			return RI_RIGHT;
		} else if (turn > 20){
			return RI_HALF_RIGHT;
		} else if (turn >= -20){
			return RI_STRAIGHT_ON;
		} else if (turn >= -70){
			return RI_HALF_LEFT;
		} else if (turn >= -110){
			return RI_LEFT;
		} else if (turn >= -160){
			return RI_HARD_LEFT;
		} else {
			return RI_UTURN;
		}
	}
	
	public void outputRoutePath() {
		String name=null;
		int dist=0;
		StringBuffer sb=new StringBuffer();
		ConnectionWithNode c;
		for (int i=0; i<route.size()-1; i++){
			c = (ConnectionWithNode) route.elementAt(i);
			name=null;
			if (c.wayNameIdx != -1) {
				name=trace.getName(c.wayNameIdx);
			}
			dist= (int) c.wayDistanceToNext;
			
			sb.setLength(0);			
			byte ri=c.wayRouteInstruction;
			//if ((c.wayRouteFlags & Legend.ROUTE_FLAG_QUIET) == 0 && ri!=RI_SKIPPED) {
			if (true) {
				sb.append(i + ". ");
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_QUIET) > 0) { 
					sb.append("(quiet) ");
				}
				sb.append(directions[ri]);
				sb.append(" into ");
				sb.append((name==null?"":name));
				sb.append(" then go ");
				sb.append(dist);
				sb.append("m");
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_COMING_FROM_ONEWAY) > 0) { 
					sb.append(" (from oneway)");
				}
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_ROUNDABOUT) > 0) { 
					sb.append(" (in roundabout)");
				}
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_LEADS_TO_MULTIPLE_SAME_NAMED_WAYS) > 0) { 
					sb.append(" (multiple name matches)");
				}
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_BEAR_LEFT) > 0) { 
					sb.append(" (bear left)");
				}
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_BEAR_RIGHT) > 0) { 
					sb.append(" (bear right)");
				}
				sb.append(" Cons:" + c.to.getConSize() + " numRoutableWays: " + c.numToRoutableWays + " startBearing: " + c.startBearing + "/" + c.wayConStartBearing + " endBearing: "+ c.endBearing + "/" + c.wayConEndBearing);
				System.out.println(sb.toString());
			}
		}		
	}
}
