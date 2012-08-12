/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.routing;

import java.util.Vector;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;

import de.ueller.gps.Node;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.data.Position;
import de.ueller.gpsmid.data.RoutePositionMark;
import de.ueller.gpsmid.graphics.ImageCollector;
import de.ueller.gpsmid.mapdata.Way;
import de.ueller.gpsmid.mapdata.WayDescription;
import de.ueller.gpsmid.mapdata.WaySegment;
import de.ueller.gpsmid.tile.Tile;
import de.ueller.gpsmid.ui.GpsMid;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.gpsmid.ui.TraceLayout;
import de.ueller.midlet.iconmenu.LayoutElement;
import de.ueller.util.DateTimeTools;
import de.ueller.util.IntPoint;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import de.ueller.util.ProjMath;

import de.enough.polish.util.Locale;

public class RouteInstructions {
	protected static final int RI_NONE = 0;
	protected static final int RI_HARD_RIGHT = 1;
	protected static final int RI_RIGHT = 2;
	protected static final int RI_HALF_RIGHT = 3;
	public static final int RI_STRAIGHT_ON = 4;
	protected static final int RI_HALF_LEFT = 5;
	protected static final int RI_LEFT = 6;
	protected static final int RI_HARD_LEFT = 7; // don't change ordering, this is used in a "<= comparison" 
	protected static final int RI_BEAR_RIGHT = 8; // don't change ordering, this is used in RouteSyntax to calculate an array index offset for bearing
	protected static final int RI_BEAR_LEFT = 9;  // don't change ordering, this is used in a "<= comparison"
	protected static final int RI_UTURN = 10;
	protected static final int RI_DEST_REACHED = 11;
	protected static final int RI_ENTER_MOTORWAY = 12;
	protected static final int RI_LEAVE_MOTORWAY = 13;
	protected static final int RI_BEAR_RIGHT_ENTER_MOTORWAY = 14; // don't change ordering, this is used in RouteSyntax to calculate an array index offset for bearing
	protected static final int RI_BEAR_LEFT_ENTER_MOTORWAY = 15;
	protected static final int RI_BEAR_RIGHT_LEAVE_MOTORWAY = 16; // don't change ordering, this is used in RouteSyntax to calculate an array index offset for bearing
	protected static final int RI_BEAR_LEFT_LEAVE_MOTORWAY = 17;
	protected static final int RI_AREA_CROSS = 18;
	protected static final int RI_AREA_CROSSED = 19;
	protected static final int RI_1ST_EXIT = 20;
	protected static final int RI_2ND_EXIT = 21;
	protected static final int RI_3RD_EXIT = 22;
	protected static final int RI_4TH_EXIT = 23;
	protected static final int RI_5TH_EXIT = 24;
	protected static final int RI_6TH_EXIT = 25;
	protected static final int RI_INTO_TUNNEL = 26;
	protected static final int RI_OUT_OF_TUNNEL = 27;
	protected static final int RI_SKIPPED = 28;
	
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
	private static int iInInstructionSaidArrow = -1;
	private static int iFollowStreetInstructionSaidArrow = -1;
	private static int iNamedArrow = 0;
	private static String sLastInstruction = "";
	private static long lastArrowChangeTime = 0;
	
	/** the index of the route element until which the route instructions are fully determined */
	private static volatile int maxDeterminedRouteInstruction = 0;

	public static volatile int routeInstructionsHeight = 0;

	private static byte cachedPicArrow;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;
	private static Image scaledPict = null;

	private static volatile RouteLineProducer rlp;
	
	private static String nameNow = null;
	private static String nameThen = null;

	private static Trace trace;
	private static Vector route;
		
	public volatile static int dstToRoutePath=1;
	public volatile static int routePathConnection=0;
	public volatile static int pathIdxInRoutePathConnection=0;
	public volatile static Way actualRoutePathWay = null;

	// variables for determining against route path 
	private static int prevRoutePathConnection = 0;
	private static int prevPathIdxInRoutePathConnection = 0;
	private static int iBackwardCount = 0;
	private static long againstDirectionDetectedTime = 0;

	public final static int DISTANCE_UNKNOWN = Integer.MAX_VALUE;
	
	private	static int routeInstructionColor=0x00E6E6E6;
	
	public static float maxScaleLevelForRouteInstructionSymbols = 0;
	
	private final static Logger logger = Logger.getInstance(RouteInstructions.class,Logger.DEBUG);

	private static volatile Node closestPointOnDestWay = null;
	
	private static volatile float dstRouteToDestination = 0;
	
	private static WaySegment waySegment = null;
	
	public RouteInstructions(Trace trace) {
		RouteInstructions.trace = trace;
		RouteInstructions.maxScaleLevelForRouteInstructionSymbols = 15000f * 1.5f * 1.5f * 1.5f;
	}
	
	public void newRoute(Vector route) {
		RouteInstructions.route = route;
		iPassedRouteArrow=0;
		prevRoutePathConnection = 0;
		prevPathIdxInRoutePathConnection = 0;
		iBackwardCount = 0;
		againstDirectionDetectedTime = 0;
		GpsMid.mNoiseMaker.resetSoundRepeatTimes();		
		try {
			if (rlp == null) {
				rlp = new RouteLineProducer();
			}
			rlp.determineRoutePath(trace, route);
			createRouteInstructions();
			outputRoutePath();
		} catch (Exception e) {
			//#debug error
			logger.error(Locale.get("routeinstructions.RIthreadCrashedUnexpectadlyWithError")/*RI thread crashed unexpectadly with error */ +  e.getMessage());
			e.printStackTrace();
		}			
	}
	

	public void showRoute(PaintContext pc, Node center,int xo,int yo) {
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
		boolean drawRouteInstructionSymbols = (pc.scale <= RouteInstructions.maxScaleLevelForRouteInstructionSymbols);

		RouteSyntax routeSyntax = RouteSyntax.getInstance();
		
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

			if (RouteHelpers.routeHelpers != null) {
				RouteHelpers.paint(pc, xo, yo);
			}
			
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTECONNECTION_TRACES)) {
				RouteConnectionTraces.paint(pc, xo, yo);
			}

			boolean routeRecalculationRequired=false;
			float remainingDistance = 0;
			/** remaining duration in 1/5 seconds */
			int remainingDurationFSecs = 0;
			float lastTrafficSignalsDistance = -200;
			synchronized(this) {
				if (route != null && route.size() > 0){
					//#debug debug
					logger.debug("showRoute - route.size(): " + route.size() + " routePathConnection: " + routePathConnection);						
					
					// there's a route so no calculation required
					routeRecalculationRequired=false;
		
					for (int i = 1; i < maxDeterminedRouteInstruction; i++){
						c = (ConnectionWithNode) route.elementAt(i);
						if (c == null){
							logger.error(Locale.get("routeinstructions.showRouteGotNullConnection")/*showRoute got null connection*/);
							break;
						}
						if (c.to == null){
							logger.error(Locale.get("routeinstructions.showRouteNullDestination")/*showRoute got connection with null as destination*/);
							break;
						}
						if (pc == null){
							logger.error(Locale.get("routeinstructions.showRoutePcIsNull")/*showRoute pc is null*/);
							break;
						}
						if (c.wayRouteInstruction == RI_AREA_CROSS) {
							areaStart.setLatLonRad(c.to.lat, c.to.lon);
						} else if (c.wayRouteInstruction == RI_AREA_CROSSED) {
							IntPoint lineP1 = new IntPoint();
							IntPoint lineP2 = new IntPoint();							
							IntPoint centerP = new IntPoint();

							pc.getP().forward(areaStart.radlat, areaStart.radlon, lineP1);
							pc.getP().forward(c.to.lat, c.to.lon, lineP2);
							pc.getP().forward(center.radlat, center.radlon, centerP);
							
							int dst = pc.getDstFromSquareDst( MoreMath.ptSegDistSq(lineP1.x, lineP1.y,
									lineP2.x, lineP2.y, centerP.x, centerP.y ));		
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
				    	// add the distance to the next route node or when this is the final route node add the distance up to the closest point on the destination way
				    	boolean finalRouteSeg = iRealNow == route.size() - 1;
				    	double distRealNow=ProjMath.getDistance(center.radlat, center.radlon, finalRouteSeg ? closestPointOnDestWay.radlat : cRealNow.to.lat, finalRouteSeg ? closestPointOnDestWay.radlon : cRealNow.to.lon);
				    	remainingDistance += distRealNow;

				    	ConnectionWithNode cToRealNow = (ConnectionWithNode) route.elementAt(routePathConnection);
				    	remainingDurationFSecs += (distRealNow * cToRealNow.durationFSecsToNext / cToRealNow.wayDistanceToNext); 

				    	// distance to next instruction or when the next instruction is the destination to the closest point on the destination way
				    	finalRouteSeg = iNow == route.size() - 1;
				    	distNow = ProjMath.getDistance(center.radlat, center.radlon, finalRouteSeg ? closestPointOnDestWay.radlat : cNow.to.lat, finalRouteSeg ? closestPointOnDestWay.radlon : cNow.to.lon);
						if (Configuration.getCfgBitState(Configuration.CFGBIT_NAVI_ARROWS_BIG)) {
							outputRouteIcons(pc, iNow, distNow);
						}
						// if necessary, convert to yards for voice output
						if (!Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
							distNow = distNow / 0.9144 + 0.5;
						}
				    	
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
							/* String name = */ getInstructionWayName(iThen);
						}
						//#debug debug
						logger.debug("showRoute - iRealNow: " + iRealNow + " iNow: " + iNow + " iThen: " + iThen);						
					}					
					
					for (int i = 1; i < maxDeterminedRouteInstruction; i++){
						c = (ConnectionWithNode) route.elementAt(i);						
						
						// Calculate distance and duration to destination
						if (i >= iRealNow && c.wayDistanceToNext != Float.MAX_VALUE) {
							remainingDistance += c.wayDistanceToNext;
							remainingDurationFSecs += c.durationFSecsToNext;
					    	// add 20 secs ( 100 x 1/5th second) delay for traffic lights that are at least 200 meters away from the previous one
					    	if (c.to.isAtTrafficSignals() && remainingDistance - lastTrafficSignalsDistance > 200) {
					    		lastTrafficSignalsDistance = remainingDistance;
					    		remainingDurationFSecs += 100;
					    		// System.out.println("Traffic Light at " + remainingDistance);
					    	}
						}
						
						// draw cross area instruction
						if (c.wayRouteInstruction == RI_AREA_CROSS) {
							areaStart.setLatLonRad(c.to.lat, c.to.lon);
						} else if (c.wayRouteInstruction == RI_AREA_CROSSED) {
							if (waySegment == null) {
								waySegment = new WaySegment();
							}
							// draw line for crossing area
							IntPoint lineP1 = new IntPoint();
							IntPoint lineP2 = new IntPoint();							
							pc.getP().forward(areaStart.radlat, areaStart.radlon, lineP1);
							lineP1.x-=xo;
							lineP1.y-=yo;

							pc.getP().forward(c.to.lat, c.to.lon, lineP2);
							lineP2.x-=xo;
							lineP2.y-=yo;

							if (iNow != i) {
								// we are currently not crossing the area, so we simply draw a line in the given color
						    	waySegment.drawWideLineSimple(Legend.COLORS[iNow > i ? Legend.COLOR_ROUTE_PRIOR_ROUTELINE : Legend.COLOR_ROUTE_ROUTELINE], lineP1, lineP2, 2, pc);
							} else {
								// we are currently crossing the area, so we need to divide the line and draw a route dot onto it
								IntPoint centerP = new IntPoint();
								pc.getP().forward(center.radlat, center.radlon, centerP);
								centerP.x-=xo;
								centerP.y-=yo;

								IntPoint closestP = MoreMath.closestPointOnLine(lineP1.x, lineP1.y, lineP2.x, lineP2.y, centerP.x, centerP.y);
								waySegment.drawWideLineSimple(Legend.COLORS[Legend.COLOR_ROUTE_PRIOR_ROUTELINE], lineP1, closestP, 2, pc);
								waySegment.drawWideLineSimple(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE], closestP, lineP2, 2, pc);
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
	
						aPaint = c.wayRouteInstruction;
						Image pict = getRoutingImage(pc, aPaint, false);
						if (trace.atDest) {
							aPaint = RI_DEST_REACHED;
						}
						pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
						pc.lineP2.x-=xo;
						pc.lineP2.y-=yo;
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
					    	
					    	ConnectionWithNode cBefore = (ConnectionWithNode) route.elementAt(iNow - 1);
					    	
					    	if (intDistNow>=PASSINGDISTANCE && !checkDirectionSaid) {
								//System.out.println("iNow :" + iNow + " iPassedRA: " + iPassedRouteArrow + " prepareSaidArrow: " + iPrepareInstructionSaidArrow + " iNamedArrow: " + iNamedArrow);
								if (
									intDistNow <= PREPAREDISTANCE
									// give prepare instruction only if the last prepareInstruction was not already for this arrow (this avoids possibly wrong prepare instructions after passing the arrow)
									&& iNow != iPrepareInstructionSaidArrow
									&& iNow != iInstructionSaidArrow
									&& intDistNow <= Configuration.getTravelMode().maxPrepareMeters
									
								) {
									soundToPlay.append(routeSyntax.getSoundInstructionPrepare(aNow));
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
									&& iNow != iInInstructionSaidArrow
									&& intDistNow <= Configuration.getTravelMode().maxInMeters
									// never tell in-instructions earlier than 350 meters before the arrow unless the route speed of the way to the arrow is more than 70 km/h 
									&& (intDistNow <= 350 || (3.6f * 5 * cBefore.wayDistanceToNext / cBefore.durationFSecsToNext) > 70)
								) {
									soundRepeatDelay=60;
									soundToPlay.append(routeSyntax.getSoundInstructionIn(aNow, intDistNow));								
									iInInstructionSaidArrow = iNow;
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
								    	soundToPlay.append(routeSyntax.getFollowStreetSound());
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
						    	sbRouteInstruction.append(routeSyntax.getTextInstruction(aNow));					    	
								if (iInstructionSaidArrow != iNow) { 
									// give routing instruction directly at the arrow
									soundToPlay.append (routeSyntax.getSoundInstruction(aNow));
							    	iInstructionSaidArrow = iNow;
									soundMaxTimesToPlay=1;
								}
							} else {
						    	sbRouteInstruction.append(routeSyntax.getTextInstructionIn(aNow, intDistNow ));					    	
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
									soundToPlay.append(routeSyntax.getSoundInstructionThen(aThen, distNowThen > PASSINGDISTANCE, aNow==aThen));
									
									//System.out.println(soundToPlay.toString());
								}
							}
						}
					    
					    if (drawRouteInstructionSymbols && (drawQuietArrows || (c.wayRouteFlags & Legend.ROUTE_FLAG_QUIET) == 0) ) {
						    if (aPaint == RI_SKIPPED) {
								pc.g.setColor(0x00FDDF9F);
								pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
								pc.lineP2.x-=xo;
								pc.lineP2.x-=yo;
								final byte radius=6;
								pc.g.fillArc(pc.lineP2.x-radius/2,pc.lineP2.y-radius/2,radius,radius,0,359);
							} else {
								//#debug debug
								if (pict==null) logger.debug("got NULL pict");													
								if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_INVISIBLE) == 0 ) {
									if (Configuration.getCfgBitState(Configuration.CFGBIT_NAVI_ARROWS_IN_MAP)) {
										pc.g.drawImage(pict,pc.lineP2.x,pc.lineP2.y,CENTERPOS);
									}
//									pc.g.setColor(0x0);
//									pc.g.drawString("" + i, pc.lineP2.x+7, pc.lineP2.y+5, Graphics.BOTTOM | Graphics.LEFT);
								}
							}
					    }
					    
						// display bearings for debugging
						if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BEARINGS)) {					
							// end bearings
							pc.g.setStrokeStyle(Graphics.SOLID);
							drawBearing(pc, pc.lineP2.x, pc.lineP2.y, c.endBearing, true, 0x00800000);
							pc.g.setStrokeStyle(Graphics.DOTTED);
							drawBearing(pc, pc.lineP2.x, pc.lineP2.y, c.wayConEndBearing, true, 0x00FF0000);							
							byte startBearingCon = 0;
							byte startBearingWay = 0;
							if ( i < route.size()-1 ) {
								ConnectionWithNode cNext = (ConnectionWithNode) route.elementAt(i + 1);
								startBearingCon = cNext.startBearing;
								startBearingWay = cNext.wayConStartBearing;
							}
							// start bearings
							pc.g.setStrokeStyle(Graphics.SOLID);
							drawBearing(pc, pc.lineP2.x, pc.lineP2.y, startBearingCon, false, 0x00008000);
							pc.g.setStrokeStyle(Graphics.DOTTED);
							drawBearing(pc, pc.lineP2.x, pc.lineP2.y, startBearingWay, false, 0x0000FF00);
							if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_INCONSISTENT_BEARING) > 0) {
								// draw red circle aroud inconsistent bearing
								pc.g.setColor(0x00FF6600);
								pc.g.setStrokeStyle(Graphics.SOLID);
								final byte radius = 40;
								pc.g.drawArc(pc.lineP2.x - radius / 2, 
										pc.lineP2.y - radius / 2, radius, radius, 0, 359);
							}
						}
					}
					routeRecalculationRequired = isOffRoute(route, center);
					if (trace.atDest || (aNow == RI_DEST_REACHED && intDistNow < PASSINGDISTANCE)) {
						routeInstructionColor = Legend.COLORS[Legend.COLOR_RI_AT_DEST];
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
						soundToPlay.append (routeSyntax.getCheckDirectionSound());
						soundRepeatDelay = 15;
						nameNow = null;
						sbRouteInstruction.setLength(0);
						sbRouteInstruction.append(routeSyntax.getCheckDirectionText());
						routeInstructionColor = Legend.COLORS[Legend.COLOR_RI_CHECK_DIRECTION];
;
					}
				}
			}
			
			dstRouteToDestination = remainingDistance;

			// When routing was started at the destination but the map has not been 
			// moved away from the destination yet, give no voice instruction 
			if (trace.atDest && ! trace.movedAwayFromDest) {
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
			logger.debug("complete route instruction: " + sbRouteInstruction.toString() + 
					" (" + soundToPlay.toString() + ")");													
			
			// Route instruction text output
			LayoutElement e = Trace.tl.ele[TraceLayout.ROUTE_INSTRUCTION];
			if (sbRouteInstruction.length() != 0) {
				e.setBackgroundColor(routeInstructionColor);				
				e.setColor(Legend.COLORS[Legend.COLOR_RI_TEXT]);				
				e.setText(sbRouteInstruction.toString());

				e = Trace.tl.ele[TraceLayout.ROUTE_INTO];
				e.setBackgroundColor(routeInstructionColor);				
				e.setColor(Legend.COLORS[Legend.COLOR_RI_TEXT]);				
				if (nameNow != null && nameNow.length() > 0) {
					e.setText("=> " + nameNow);
				}				
				if(Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP)) {
					e = Trace.tl.ele[TraceLayout.ROUTE_OFFROUTE];
					e.setText(Locale.get("routeinstructions.Offset")/*off:*/ + Trace.showDistance(dstToRoutePath, (dstToRoutePath == RouteInstructions.DISTANCE_UNKNOWN ? Trace.DISTANCE_UNKNOWN : Trace.DISTANCE_AIR)));
				}
				e = Trace.tl.ele[TraceLayout.ROUTE_DISTANCE];
				String airDistance = "";
				if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_WHEN_ROUTING)) {
					RoutePositionMark dest = trace.getDest();
					if (dest != null) {
						airDistance = "(" + Trace.showDistance((int) ProjMath.getDistance(center.radlat, center.radlon, dest.lat, dest.lon), Trace.DISTANCE_AIR) + ")";
					}
				}
				if (RouteLineProducer.isRunning()) {
					// use routeLine Color for distance while route line is produced
					e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE]);
					e.setText(">" + Trace.showDistance((int) remainingDistance, Trace.DISTANCE_ROAD) + airDistance);
				} else if (RouteLineProducer.isRouteLineProduced()) {
					e.setBackgroundColor(Legend.COLORS[Legend.COLOR_RI_DISTANCE_BACKGROUND]);
					e.setText(" " + Trace.showDistance((int) remainingDistance, Trace.DISTANCE_ROAD) + airDistance + 
							(
							 Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_ROUTE_DURATION_IN_MAP)?
							 " " + ((remainingDurationFSecs >= 300)?remainingDurationFSecs / 300 + "min": remainingDurationFSecs / 5 + "s")
							 :""
							)
							 );
					
					if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_ETA_IN_MAP)) {
						e = Trace.tl.ele[TraceLayout.ETA]; // e is used *twice* below (also as vRelative)
						Position pos = trace.getCurrentPosition();
						if (Configuration.getCfgBitState(Configuration.CFGBIT_GPS_TIME)) {
							if (pos.gpsTimeMillis != 0) {
								e.setText(DateTimeTools.getClock(pos.gpsTimeMillis + Configuration.getTimeDiff()*1000*60 + remainingDurationFSecs * 200, true));
							} else if (Configuration.getCfgBitState(Configuration.CFGBIT_GPS_TIME_FALLBACK)) {
								e.setText(DateTimeTools.getClock(System.currentTimeMillis() + Configuration.getTimeDiff()*1000*60 + remainingDurationFSecs * 200, true));
							} else {
								e.setText(" ");
							}
						} else {
							e.setText(DateTimeTools.getClock(System.currentTimeMillis() + Configuration.getTimeDiff()*1000*60 + remainingDurationFSecs * 200, true));
						}
						/*
						don't use new Date() - it is very slow on some Nokia devices			
						Calendar currentTime = Calendar.getInstance();
						currentTime.setTime( new Date( System.currentTimeMillis() + remainingDurationFSecs * 200) );		
						e.setText(
							currentTime.get(Calendar.HOUR_OF_DAY) + ":"  
							+ HelperRoutines.formatInt2(currentTime.get(Calendar.MINUTE)));
						*/
						
						// if ETA is visible, position OFFROUTE above ETA will work
						Trace.tl.ele[TraceLayout.ROUTE_OFFROUTE].setVRelative(e);
					}
				}
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

	private Image getRoutingImage(PaintContext pc, int instruction, boolean forBigNavigationArrow) {
		Image pict = pc.images.IMG_MARK;
		switch (instruction) {
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
		case RI_ENTER_MOTORWAY:
		case RI_BEAR_LEFT_ENTER_MOTORWAY:
		case RI_BEAR_RIGHT_ENTER_MOTORWAY:
			pict=pc.images.IMG_MOTORWAYENTER; break;
		case RI_LEAVE_MOTORWAY:
		case RI_BEAR_LEFT_LEAVE_MOTORWAY:
		case RI_BEAR_RIGHT_LEAVE_MOTORWAY:
			pict=pc.images.IMG_MOTORWAYLEAVE; break;					
		case RI_INTO_TUNNEL:	pict=pc.images.IMG_TUNNEL_INTO; break;
		case RI_OUT_OF_TUNNEL:	pict=pc.images.IMG_TUNNEL_OUT_OF; break;					
		}

		if (forBigNavigationArrow) {
			if (instruction == RI_DEST_REACHED) {
				pict=pc.images.IMG_DESTREACHED;
			} else if (instruction == RI_AREA_CROSS) {
				pict = pc.images.IMG_AREACROSS;
			} else if (instruction == RI_AREA_CROSSED) {
				pict = pc.images.IMG_AREACROSSED;
			} else if (instruction >= RI_1ST_EXIT && instruction <= RI_6TH_EXIT) {
				pict = pc.images.IMG_ROUNDABOUT;
			}
		}
						
		return pict;
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
		if (
			// never recalculate during route calculation			
			(trace.routeCalc && !RouteLineProducer.isRunning())
				||
			// never recalculate when tiles are still requested as the route line might not be loaded yet		
			(trace.getDataReader().getRequestQueueSize() > 0)
		) {
			return false;
		}

		/* If we did no initial recalculation,
		 * the map is gpscentered
		 * and we just moved away from the destination
		 * ==> initial recalculation
		 */
		if (!initialRecalcDone && trace.gpsRecenter && trace.movedAwayFromDest
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
		} else if (routePathConnection == prevRoutePathConnection && actualRoutePathWay != null && !actualRoutePathWay.isCircleway()) {
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
	
	public static void forceAgainstDirection() {
		iBackwardCount = 99;
	}
	
	
	public static void resetOffRoute(Vector route, Node center) {
		haveBeenOnRouteSinceCalculation = false;
		dstToRoutePath=RouteInstructions.DISTANCE_UNKNOWN;
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
		ConnectionWithNode cPrev;
		int nextStartBearing;
		int iAreaStart = 0;
		int iInstructionStart = 0;
		maxDeterminedRouteInstruction = 0;
		
		if (route.size() < 3) {
			return;
		}
		for (int i=0; i<route.size(); i++){
			iInstructionStart = i;
			c = getRouteElement(i);

			short rfCurr=c.wayRouteFlags;
			short rfPrev=0;
			if (i > 0) {
				cPrev = getRouteElement(i-1);
				rfPrev=cPrev.wayRouteFlags;
			} else {				
				// use first connection as previous connection to avoid NPEs
				cPrev = getRouteElement(0);
			}
			nextStartBearing = 0;
			if (i < route.size()-1) {
				c2 = getRouteElement(i+1);
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
				if ( (rfCurr & Legend.ROUTE_FLAG_BEAR_LEFT) > 0) {
					ri = RI_BEAR_LEFT_ENTER_MOTORWAY;
				} else if ((rfCurr & Legend.ROUTE_FLAG_BEAR_RIGHT) > 0) {
					ri = RI_BEAR_RIGHT_ENTER_MOTORWAY;
				} else {
					ri = RI_ENTER_MOTORWAY;					
				}
			}
			// leave motorway
			if ( isLeaveMotorway(rfPrev, rfCurr) ) {
				if ( (rfCurr & Legend.ROUTE_FLAG_BEAR_LEFT) > 0) {
					ri = RI_BEAR_LEFT_LEAVE_MOTORWAY;
				} else if ((rfCurr & Legend.ROUTE_FLAG_BEAR_RIGHT) > 0) {
					ri = RI_BEAR_RIGHT_LEAVE_MOTORWAY;
				} else {
					ri = RI_LEAVE_MOTORWAY;					
				}
				fillInMotorWayJunctionName(c);
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
				ConnectionWithNode cAreaStart = getRouteElement(iAreaStart);
				cAreaStart.wayDistanceToNext = ProjMath.getDistance(cAreaStart.to.lat, cAreaStart.to.lon, c.to.lat, c.to.lon);
			}

			// determine roundabout exit
			if ( 	(rfPrev & Legend.ROUTE_FLAG_ROUNDABOUT) == 0
				&& 	(rfCurr & Legend.ROUTE_FLAG_ROUNDABOUT) > 0
			) {
				ri = RI_1ST_EXIT;	
				int i2;
				for (i2=i+1; i2<route.size()-1 && (ri < RI_6TH_EXIT); i2++) {
					c2 = getRouteElement(i2);
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
					c2 = getRouteElement(i3);
					c2.wayRouteInstruction=ri;					
					c2.wayRouteFlags |= Legend.ROUTE_FLAG_QUIET;
				}
				i=i2-1;				
			}
			// if we've got no better instruction, just use the direction
			if (ri==0) {				
				// ri = convertTurnToRouteInstruction( (nextStartBearing - c.endBearing) * 2 );
				ri = convertTurnToRouteInstruction( (nextStartBearing - c.wayConEndBearing) * 2 );
				// give bear instruction if bearing is available
				if ( (rfCurr & Legend.ROUTE_FLAG_BEAR_LEFT) > 0 ) {
					ri = RI_BEAR_LEFT;
				}
				if ( (rfCurr & Legend.ROUTE_FLAG_BEAR_RIGHT) > 0 ) {
					ri = RI_BEAR_RIGHT;
				}
			}
			c.wayRouteInstruction = ri;
			combineCloseInstructions(iInstructionStart, i);
			makeArrowsQuiet(iInstructionStart, i);
			maxDeterminedRouteInstruction = i - 1;
		}
		// combine
		combineCloseInstructions(iInstructionStart, route.size()-2);
		makeArrowsQuiet(iInstructionStart, route.size()-2);
		
		c = getRouteElement(route.size()-1);
		c.wayRouteInstruction = RI_DEST_REACHED;				
		maxDeterminedRouteInstruction = route.size();
		
		// reset arrow markers
		iNamedArrow = -1;
		iInstructionSaidArrow = -1;
		iPrepareInstructionSaidArrow = -1;
		iInInstructionSaidArrow = - 1;
		iFollowStreetInstructionSaidArrow = -1;
	}
	
	private ConnectionWithNode getRouteElement(int i) {
		if (i <= RouteLineProducer.maxRouteElementDone) {
			return (ConnectionWithNode) route.elementAt(i);
		}
		rlp.waitForRouteLine(i);
		return (ConnectionWithNode) route.elementAt(i);
	}
	
	/** returns the next (in the current routeMode) route Element with alternatives
		if there's no next one with alternatives, the last route element will be returned
	 */
	private ConnectionWithNode getNextExistingRouteElement(int i) {
		ConnectionWithNode current = (ConnectionWithNode) route.elementAt(i);
		while (current.numToRoutableWays <= 1 && i < route.size() - 2) {
			i++;
			current = getRouteElement(i);
		}
		return getRouteElement(i + 1);
	}

	/** returns the previous (in the current routeMode) existing route Element with alternatives */
	private ConnectionWithNode getPreviousExistingRouteElement(int i) {
		ConnectionWithNode prev = (ConnectionWithNode) route.elementAt(i - 1);
		while (prev.numToRoutableWays <= 1 && i > 0) {
			i--;
			prev = getRouteElement(i);
		}
		return prev;
	}	
	
	/** combine instructions that are closer than 25 m to the previous one into single instructions */
	private void combineCloseInstructions(int iInstructionStart, int iInstructionCurrent) {
		if (iInstructionStart < 2) {
			return;
		}		
		ConnectionWithNode c;
		ConnectionWithNode cPrev = (ConnectionWithNode) route.elementAt(iInstructionStart-1);
		for (int i = iInstructionStart; i <= iInstructionCurrent; i++){
			c = (ConnectionWithNode) route.elementAt(i);
			if (cPrev.numToRoutableWays <= 1) {
				// ignore connections that only exist because of other route modes
				cPrev=c;
				continue;
			}
//			if( (c.wayDistanceToNext < 3)) {
//				c.wayRouteInstruction = RI_SKIPPED;
//				c.wayRouteFlags |= Legend.ROUTE_FLAG_VERY_SMALL_DISTANCE;
//				cPrev=c;
//				continue;
//			}
			// skip connections that are closer than 25 m to the previous one
			if( (i<route.size()-1 && ProjMath.getDistance(c.to.lat, c.to.lon, cPrev.to.lat, cPrev.to.lon) < 25)
			// only combine direction instructions
			&& (cPrev.wayRouteInstruction <= RI_HARD_LEFT && c.wayRouteInstruction <= RI_HARD_LEFT)
			// do not skip a bear instruction that follows close after another instruction
			// && (c.wayRouteFlags & (Legend.ROUTE_FLAG_BEAR_LEFT | Legend.ROUTE_FLAG_BEAR_RIGHT)) == 0
			// do not combine a bear instruction with the one following close after it
			// && (cPrev.wayRouteFlags & (Legend.ROUTE_FLAG_BEAR_LEFT | Legend.ROUTE_FLAG_BEAR_RIGHT)) == 0
			)	{
				c.wayRouteInstruction = RI_SKIPPED;
				c.wayRouteFlags |= Legend.ROUTE_FLAG_QUIET;
				ConnectionWithNode cNext = (ConnectionWithNode) route.elementAt(i+1);
				// cPrev.wayRouteInstruction = convertTurnToRouteInstruction( (cNext.startBearing - cPrev.endBearing) * 2 );
				cPrev.wayRouteInstruction = convertTurnToRouteInstruction( (cNext.wayConStartBearing - cPrev.wayConEndBearing) * 2 );
			}
			cPrev=c;
		}
	}
	
	/**
	 *  replace redundant straight-ons and direction arrow with same name by quiet arrows
	 *  and add way distance to starting arrow of the street
	 */
	private void makeArrowsQuiet(int iInstructionStart, int iInstructionCurrent) {
		if (iInstructionStart < 2) {
			return;
		}
		// replace redundant straight-ons and direction arrow with same name by quiet arrows and add way distance to starting arrow of the street
		ConnectionWithNode cStart;
		ConnectionWithNode c;
		ConnectionWithNode cPrev;
		ConnectionWithNode cNext;
		int oldNameIdx = -2;
		for (int i = iInstructionStart - 1; i < iInstructionCurrent; i++){
			c = (ConnectionWithNode) route.elementAt(i);
			cStart = getPreviousExistingRouteElement(i);
			cPrev = cStart;
			oldNameIdx = cStart.wayNameIdx;
			cNext = (ConnectionWithNode) getNextExistingRouteElement(i);
			while (	i < iInstructionCurrent
					&&
					(
						// while straight on
						c.wayRouteInstruction == RI_STRAIGHT_ON
						||
						/* or no alternative way to go to (and this is a direction instruction)
						 * but the next and previous instruction must not be a skipped one with alternatives
						*/
						(
							c.numToRoutableWays == 1
							&&
							c.wayRouteInstruction <= RI_BEAR_LEFT
							&&
							c.wayRouteInstruction >= RI_HARD_RIGHT
							&&
							(cPrev.wayRouteInstruction != RI_SKIPPED || cPrev.numToRoutableWays == 1)
							&&
							(cNext.wayRouteInstruction != RI_SKIPPED || cNext.numToRoutableWays == 1)
						)
						||
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
				cPrev = c;
				c = cNext;
				if (i < iInstructionCurrent) {
					cNext = getNextExistingRouteElement(i);
				}
			}			
		}
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

	/**
	 * @param c
	 */
	private void fillInMotorWayJunctionName(ConnectionWithNode c) {
		int nodeNameIdx;
		for (int i1 = 0; i1 < 4; i1++) {
			// TODO: look explicitely for nodes with type motorway junction 
			nodeNameIdx = trace.tiles[i1].getNameIdx(c.to.lat, c.to.lon, (short) -1);
			if (nodeNameIdx != -1) {
				c.nodeNameIdx = nodeNameIdx;
			}
		}
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
		StringBuffer sbName = new StringBuffer();
		String name;
		if (c.nodeNameIdx != -1) {
			name = trace.getName(c.nodeNameIdx);
			if (name != null) {
				sbName.append(name);
			}
		}
		if (c.wayNameIdx != -1) {
			name = trace.getName(c.wayNameIdx);
			if (name != null) {
				if (sbName.length() > 0) {
					sbName.append(" / ");
				}
				sbName.append(name);
			}
		} else {
			WayDescription wayDesc = Legend.getWayDescription(c.wayType);
			boolean nextWayIsOriginalNextWay = true;
			// for unnamed links give the name or wayDescription of the next named way following the link
			while (wayDesc.isHighwayLink() && i < route.size() - 2) {
				nextWayIsOriginalNextWay = false;
				i++;
				c2 = (ConnectionWithNode) route.elementAt(i);
				wayDesc = Legend.getWayDescription(c2.wayType);
				if (c2.wayNameIdx != -1) {
					name = trace.getName(c2.wayNameIdx);
					if (name != null) {
						if (sbName.length() > 0) {
							sbName.append (" / ");
						}
						sbName.append("...");
						sbName.append(name);
					}
					return sbName.toString();
				}
			}
			/* 
			 * if we did not find a name on the following links or the first way after the links return unnamed way type,
			 * only prefix "..." if there's a link way before the way
			 */
			if (sbName.length() > 0) {
				sbName.append (" / ");
			}
			if (!nextWayIsOriginalNextWay) {
				sbName.append ("... ");				
			}
			sbName.append( Locale.get("imagecollector.unnamed")/*(unnamed */ + wayDesc.description + ")");
		}
		return sbName.toString();
	}

	public static void toNextInstruction(int direction) {
		if (routePathConnection != -1 && routePathConnection < route.size() - 1) {
			int i;
			i = idxNextInstructionArrow (routePathConnection + 1);			
			if (direction > 0) {
				i = idxNextInstructionArrow (i + 1);
			} else {
				i = idxPrevInstructionArrow (i - 1);				
			}
			ConnectionWithNode c = (ConnectionWithNode) route.elementAt(i);
			double rad = Math.toRadians((double) (c.wayConEndBearing * 2));
			trace.center.setLatLonRad(	c.to.lat - 0.0000025f * (float) Math.cos(rad),
										c.to.lon - 0.0000025f * (float) Math.sin(rad) );
			trace.gpsRecenter = false;
			// allow to output same instruction again
			GpsMid.mNoiseMaker.resetSoundRepeatTimes();
		}
	}
		
	public static byte convertTurnToRouteInstruction(int turn) {
		if (turn > 180) {
			turn -= 360;
		}
		if (turn < -180) {
			turn += 360;
		}
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
	
	public static void reallowInInstruction() {
		iInInstructionSaidArrow = -1;
	}

	public void outputRouteIcons(PaintContext pc, int start, double firstDist) {
		String name = null;
		RouteSyntax routeSyntax = RouteSyntax.getInstance();
		ConnectionWithNode c;
		ConnectionWithNode cPrev;
		double dist = firstDist;
		int i = start;
		c = (ConnectionWithNode) route.elementAt(i);
		for (int iconCount = 0; iconCount < 2; iconCount++) {
			name = getInstructionWayName(i);
			Image icon = getRoutingImage(pc, c.wayRouteInstruction, true);
			if (icon == pc.images.IMG_MARK) {
				icon = null;
			}
			int roundAboutExitNr = 0;
			if (c.wayRouteInstruction >= RI_1ST_EXIT && c.wayRouteInstruction <= RI_6TH_EXIT) {
				roundAboutExitNr = c.wayRouteInstruction - RI_1ST_EXIT + 1;
			}
			// output no distance when close to the arrow
			if( dist < 25 && iconCount == 0) {
				dist = 0;
			}
			// if necessary, convert to yards
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				dist = dist / 0.9144 + 0.5;
			}
			trace.tl.setRouteIcon(iconCount, icon, roundAboutExitNr, (int) dist);

			if (c.wayRouteInstruction == RI_DEST_REACHED) {		
				break;
			}
			
			// calculate parameters for next instruction
			i = idxNextInstructionArrow(i + 1);
			cPrev = c;
			c = (ConnectionWithNode) route.elementAt(i);
			boolean finalInstruction = (c.wayRouteInstruction == RI_DEST_REACHED);		
			dist = ProjMath.getDistance(cPrev.to.lat, cPrev.to.lon, finalInstruction ? closestPointOnDestWay.radlat : c.to.lat, finalInstruction ? closestPointOnDestWay.radlon : c.to.lon);
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
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_VERY_SMALL_DISTANCE) > 0) { 
					sb.append("(small distance) ");
				}
				sb.append(RouteSyntax.getInstance().getTextInstruction(ri))
				  .append(" into ")
				  .append((name==null?"":name))
				  .append(" then go ")
				  .append(Configuration.getCfgBitState(Configuration.CFGBIT_METRIC) ? dist : dist / 0.9144 + 0.5 )
				  .append(Configuration.getCfgBitState(Configuration.CFGBIT_METRIC) ? Locale.get("guitacho.m") : Locale.get("guitacho.yd") );
				//sb.append(Trace.showDistance(dist, Trace.DISTANCE_GENERIC));
				if ( (c.wayRouteFlags & Legend.ROUTE_FLAG_ONEDIRECTION_ONLY) > 0) { 
					sb.append(" (onedirection_only)");
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
				sb.append(" Cons:" + c.to.getConSize() + " numRoutableWays: " + c.numToRoutableWays + " startBearing: " + c.startBearing + "/" + c.wayConStartBearing + " endBearing: "+ c.endBearing + "/" + c.wayConEndBearing)
				  .append(" Duration: " + c.durationFSecsToNext / 5)
				  .append(" Route Speed in km/h: " + (int) (3.6f * 5 * c.wayDistanceToNext / c.durationFSecsToNext));
				System.out.println(sb.toString());
			}
		}		
	}

	public static void abortRouteLineProduction() {
		if (rlp != null) {
			rlp.abort();
		}
	}

	public static void setClosestPointOnDestWay(Node closestPoint) {
		closestPointOnDestWay = closestPoint;
	}

	public static Node getClosestPointOnDestWay() {
		return closestPointOnDestWay;
	}

	public static float getDstRouteToDestination() {
		return dstRouteToDestination;
	}
	
}
