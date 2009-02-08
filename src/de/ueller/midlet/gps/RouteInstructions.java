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

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.ImageTools;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Proj2D;
import de.ueller.midlet.gps.routing.ConnectionWithNode;
import de.ueller.midlet.gps.routing.RouteHelper;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;

public class RouteInstructions {
	private static final String[] directions  = { "mark",
		"hard right", "right", "half right",
		"straight on",
		"half left", "left", "hard left", "Target reached",
		"enter motorway", "leave motorway",
		"Roundabout exit #1", "Roundabout exit #2", "Roundabout exit #3",
		"Roundabout exit #4", "Roundabout exit #5", "Roundabout exit #6",
		"qstraight on", "into tunnel", "out of tunnel"
	};
	private static final String[] soundDirections  = { "",
		"HARD;RIGHT", "RIGHT", "HALF;RIGHT",
		"STRAIGHTON",
		"HALF;LEFT", "LEFT", "HARD;LEFT", "TARGET_REACHED",
		"ENTER_MOTORWAY", "LEAVE_MOTORWAY",
		"RAB;1ST;RABEXIT", "RAB;2ND;RABEXIT", "RAB;3RD;RABEXIT",
		"RAB;4TH;RABEXIT", "RAB;5TH;RABEXIT", "RAB;6TH;RABEXIT",
		"AGAIN;STRAIGHTON","INTO_TUNNEL", "OUT_OF_TUNNEL"
	};

	private static final int RI_HARD_RIGHT = 1;
	private static final int RI_RIGHT = 2;
	private static final int RI_HALF_RIGHT = 3;
	private static final int RI_STRAIGHT_ON = 4;
	private static final int RI_HALF_LEFT = 5;
	private static final int RI_LEFT = 6;
	private static final int RI_HARD_LEFT = 7;
	private static final int RI_TARGET_REACHED = 8;
	private static final int RI_ENTER_MOTORWAY = 9;
	private static final int RI_LEAVE_MOTORWAY = 10;
	private static final int RI_1ST_EXIT = 11;
	private static final int RI_2ND_EXIT = 12;
	private static final int RI_3RD_EXIT = 13;
	private static final int RI_4TH_EXIT = 14;
	private static final int RI_5TH_EXIT = 15;
	private static final int RI_6TH_EXIT = 16;
	private static final int RI_STRAIGHT_ON_QUIET = 17;
	private static final int RI_INTO_TUNNEL = 18;
	private static final int RI_OUT_OF_TUNNEL = 19;
	private static final int RI_SKIPPED = 99;
	
	private int connsFound = 0;
	
	private int sumWrongDirection=0;
	private int oldAwayFromNextArrow=0;
	private int oldRouteInstructionColor=0x00E6E6E6;
	private static boolean prepareInstructionSaid=false;
	private static boolean checkDirectionSaid=false;
	private static boolean autoRecalcIfRequired=false;
	public volatile static boolean initialRecalcDone=false;
	
//	public static int riCounter = 0; 
	
	public static volatile boolean haveBeenOnRouteSinceCalculation = false;
	public static volatile int startDstToFirstArrowAfterCalculation = 0;
	
	private int iPassedRouteArrow=0;
	private static String sLastInstruction = "";

	private static Font routeFont;
	private static int routeFontHeight;

	private byte cachedPicArrow;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;
	private Image scaledPict = null;

	private static Trace trace;
	private static Vector route;
	private static PositionMark target;
	
	public volatile static int dstToRoutePath=1;
	public volatile static int routePathConnection=0;

	private	static int routeInstructionColor=0x00E6E6E6;
	
	private final static Logger logger = Logger.getInstance(RouteInstructions.class,Logger.DEBUG);

	public RouteInstructions(Trace trace, Vector route, PositionMark target) {
		RouteInstructions.trace = trace;
		RouteInstructions.route = route;
		RouteInstructions.target = target;
		iPassedRouteArrow=0;
		sumWrongDirection=-1;
		oldRouteInstructionColor = 0x00E6E6E6;
		resetVoiceInstructions();			
		GpsMid.mNoiseMaker.resetSoundRepeatTimes();		
		try {
			determineRoutePath();
		} catch (Exception e) {
			//#debug error
			logger.error("Routing thread crashed unexpectadly with error " +  e.getMessage());
		}			
	}
	
	
	public void determineRoutePath() throws Exception {
		PaintContext pc = new PaintContext(trace, null);
		connsFound=0;
		float routeLen=0f;
		long startTime = System.currentTimeMillis();
		if (route != null && route.size() > 1){
			for (int i=0; i<route.size()-1; i++){
				routeLen += searchConnection2Ways(pc, i);
			}
			//parent.alert ("Connection2Ways", "found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms", 3000);
			//#debug debug
			logger.debug("Connection2Ways found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms");
			trace.receiveMessage ("Route: " + (int) routeLen + "m");
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
			cFrom.wayRouteFlags = pc.conWayRouteFlags;
			connsFound++;
			return cFrom.wayDistanceToNext;
		}
		return 0f;
	}
	
	private static void resetVoiceInstructions() {
		prepareInstructionSaid = false;
		checkDirectionSaid = false;
	}
	
	
	/**
	 * @param pc
	 */
	public void oldShowRoute(PaintContext pc, PositionMark source, Node center, int textYPos) {
		/*	PASSINGDISTANCE is the distance when a routing arrow
			is considered to match to the current position.
			We currently can't adjust this value according to the speed
			because if we would be slowing down during approaching the arrow,
			then PASSINGDISTANCE could become smaller than the distance
			to the arrow due and thus the routines would already use the
			next arrow for routing assistance
		*/
		final int PASSINGDISTANCE=25;

		StringBuffer soundToPlay = new StringBuffer();
		String routeInstruction = null;
    	// backgound colour for standard routing instructions
		int routeInstructionColor=0x00E6E6E6;
		int diffArrowDist=0;
		byte soundRepeatDelay=3;
		byte soundMaxTimesToPlay=2;
		float nearestLat=0.0f;
		float nearestLon=0.0f;
		

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
		Vector routeNodes = trace.getRouteNodes();
		// Show helper nodes for Routing
		for (int x=0; x<routeNodes.size();x++){
			RouteHelper n=(RouteHelper) routeNodes.elementAt(x);
			pc.getP().forward(n.node.radlat, n.node.radlon, pc.lineP2);
			pc.g.drawRect(pc.lineP2.x-5, pc.lineP2.y-5, 10, 10);
			pc.g.drawString(n.name, pc.lineP2.x+7, pc.lineP2.y+5, Graphics.BOTTOM | Graphics.LEFT);
		}
		boolean routeRecalculationRequired=false;
		synchronized(this) {
			if (route != null && route.size() > 0){
				// there's a route so no calculation required
				routeRecalculationRequired=false;
				RouteNode lastTo;
	
				// find nearest routing arrow (to center of screen)
				int iNearest=0;
				if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTING_HELP)) {
					c = (ConnectionWithNode) route.elementAt(0);
					lastTo=c.to;
					float minimumDistance=99999;
					float distance=99999;
					for (int i=1; i<route.size();i++){
						c = (ConnectionWithNode) route.elementAt(i);
						if (c!=null && c.to!=null && lastTo!=null) {
							// skip connections that are closer than 25 m to the previous one
							if( i<route.size()-1 && ProjMath.getDistance(c.to.lat, c.to.lon, lastTo.lat, lastTo.lon) < 25 ) {
								continue;
							}
							distance = ProjMath.getDistance(center.radlat, center.radlon, lastTo.lat, lastTo.lon); 
							if (distance<minimumDistance) {
								minimumDistance=distance;
								iNearest=i;
							}
							lastTo=c.to;
						}
					}
					//System.out.println("iNearest "+ iNearest + "dist: " + minimumDistance);				    	
					// if nearest route arrow is closer than PASSINGDISTANCE meters we're currently passing this route arrow
					if (minimumDistance<PASSINGDISTANCE) {
						if (iPassedRouteArrow != iNearest) {
							iPassedRouteArrow = iNearest;
							// if there's i.e. a 2nd left arrow in row "left" must be repeated
							if (!trace.atTarget) {
								GpsMid.mNoiseMaker.resetSoundRepeatTimes();
							}
						}
						// after passing an arrow all instructions, i.e. saying "in xxx metres" are allowed again 
						resetVoiceInstructions();
						//System.out.println("iPassedRouteArrow "+ iPassedRouteArrow);
					} else {
						c = (ConnectionWithNode) route.elementAt(iPassedRouteArrow);
						// if we got away more than PASSINGDISTANCE m of the previously passed routing arrow
						if (ProjMath.getDistance(center.radlat, center.radlon, c.to.lat, c.to.lon) >= PASSINGDISTANCE) {
							// assume we should start to emphasize the next routing arrow now
							iNearest=iPassedRouteArrow+1;
						}
					}
				}
				c = (ConnectionWithNode) route.elementAt(0);
				int lastEndBearing=c.endBearing;			
				lastTo=c.to;
				byte a=0;
				byte aNearest=0;
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
					if (lastTo == null){
						logger.error("show Route strange lastTo is null");
						break;
					}
					if (pc == null){
						logger.error("show Route strange pc is null");
						break;
					}
	//				if (pc.screenLD == null){
	//					System.out.println("show Route strange pc.screenLD is null");
	//				}
	
					// skip connections that are closer than 25 m to the previous one
					if( i<route.size()-1 && ProjMath.getDistance(c.to.lat, c.to.lon, lastTo.lat, lastTo.lon) < 25 ) {
						// draw small circle for left out connection
						pc.g.setColor(0x00FDDF9F);
						pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
						final byte radius=6;
						pc.g.fillArc(pc.lineP2.x-radius/2,pc.lineP2.y-radius/2,radius,radius,0,359);
						//System.out.println("Skipped routing arrow " + i);
						// if this would have been our iNearest, use next one as iNearest
						if(i==iNearest) iNearest++;
						continue;
					}
					
					//****************************skip straight on-s start********************************

//					int turnb=(c.startBearing-lastEndBearing) * 2;
//					if (turnb > 180) turnb -= 360;
//					if (turnb < -180) turnb += 360;
//					if (i<route.size()-1 && turnb >= -20 && turnb <= 20){
//					if(i==iNearest) iNearest++;
//					}

					//***************************skip straightnsend************************
					
					// no off-screen check for current and next route arrow,
					// so we can do DIRECTION-THEN-DIRECTION instructions
					// even if both arrows are off-screen
					if(i!=iNearest && i!=iNearest+1) {
						if (lastTo.lat < pc.getP().getMinLat()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
						if (lastTo.lon < pc.getP().getMinLon()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
						if (lastTo.lat > pc.getP().getMaxLat()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
						if (lastTo.lon > pc.getP().getMaxLon()) {
							lastEndBearing=c.endBearing;
							lastTo=c.to;
							continue;
						}
					}
	
					Image pict = pc.images.IMG_MARK; a=0;
					// make bearing relative to current course for the first route arrow
					if (i==1) {
						lastEndBearing = (pc.course%360) / 2;
					}
					int turn=(c.startBearing-lastEndBearing) * 2;
					if (turn > 180) turn -= 360;
					if (turn < -180) turn += 360;
	//				System.out.println("from: " + lastEndBearing*2 + " to:" +c.startBearing*2+ " turn " + turn);
					if (turn > 110) {
						pict=pc.images.IMG_HARDRIGHT; a=1;
					} else if (turn > 70){
						pict=pc.images.IMG_RIGHT; a=2;
					} else if (turn > 20){
						pict=pc.images.IMG_HALFRIGHT; a=3;
					} else if (turn >= -20){
						pict=pc.images.IMG_STRAIGHTON; a=4;
					} else if (turn >= -70){
						pict=pc.images.IMG_HALFLEFT; a=5;
					} else if (turn >= -110){
						pict=pc.images.IMG_LEFT;  a=6;
					} else {
						pict=pc.images.IMG_HARDLEFT; a=7;
					} 
					if (trace.atTarget) {
						a=8;
					}
					pc.getP().forward(lastTo.lat, lastTo.lon, pc.lineP2);
				    // optionally scale nearest arrow
				    if (i==iNearest) {
				    	nearestLat=lastTo.lat;
				    	nearestLon=lastTo.lon;
						aNearest=a;
						double distance=ProjMath.getDistance(center.radlat, center.radlon, lastTo.lat, lastTo.lon);
						int intDistance=new Double(distance).intValue();
	
						routeInstruction=directions[a] + ((intDistance<PASSINGDISTANCE)?"":" in " + intDistance + "m");
	
				    	if(intDistance<PASSINGDISTANCE) {
							if (!trace.atTarget) { 
								soundToPlay.append (soundDirections[a]);
							}
							soundMaxTimesToPlay=1;
							sumWrongDirection = -1;
							diffArrowDist = 0;
							oldRouteInstructionColor=0x00E6E6E6;
						} else if (sumWrongDirection == -1) {
							oldAwayFromNextArrow = intDistance;
							sumWrongDirection=0;
							diffArrowDist = 0;
						} else {
							diffArrowDist = (intDistance - oldAwayFromNextArrow);
						}
						if ( diffArrowDist == 0 ) {
			    			routeInstructionColor=oldRouteInstructionColor;
						} else if (intDistance < PASSINGDISTANCE) {
					    	// background colour if currently passing
				    		routeInstructionColor=0x00E6E6E6;
			    		} else if ( diffArrowDist > 0) {
					    	// background colour if distance to next arrow has just increased
				    		routeInstructionColor=0x00FFCD9B;
						} else {
					    	// background colour if distance to next arrow has just decreased
				    		routeInstructionColor=0x00B7FBBA;
				    		// we are going towards the next arrow again, so we need to warn again
				    		// if we go away
				    		checkDirectionSaid = false;
						}
						sumWrongDirection += diffArrowDist;
						//System.out.println("Sum wrong direction: " + sumWrongDirection);
						oldAwayFromNextArrow = intDistance;
						if (intDistance>=PASSINGDISTANCE && !checkDirectionSaid) {
							if (intDistance <= PREPAREDISTANCE) {
								soundToPlay.append( (a==4 ? "CONTINUE" : "PREPARE") + ";" + soundDirections[a]);
								soundMaxTimesToPlay=1;
								// Because of adaptive-to-speed distances for "prepare"-instructions
								// GpsMid could fall back from "prepare"-instructions to "in xxx metres" voice instructions
								// Remembering and checking if the prepare instruction already was given since the latest passing of an arrow avoids this
								prepareInstructionSaid = true;
							} else if (intDistance < 900  && !prepareInstructionSaid) {
								soundRepeatDelay=60;
								soundToPlay.append("IN;" + Integer.toString(intDistance / 100)+ "00;METERS;" + soundDirections[a]);								
							}							
						}
						if (a!=cachedPicArrow) {
							cachedPicArrow=a;
							scaledPict=doubleImage(pict);
						}
						pict=scaledPict;
				    }
					if (i == iNearest + 1 && !checkDirectionSaid) {
						double distance=ProjMath.getDistance(nearestLat, nearestLon, lastTo.lat, lastTo.lon);
						// if there is a close direction arrow after the current one
						// inform the user about its direction
						if (distance <= PREPAREDISTANCE &&
							// only if not both arrows are STRAIGHT_ON
							!(a==4 && aNearest == 4) &&
							// and only as continuation of instruction
							soundToPlay.length()!=0
						   ) {
							soundToPlay.append(";THEN;");
							if (distance > PASSINGDISTANCE) {
								soundToPlay.append("SOON;");
							}
							soundToPlay.append(soundDirections[a]);
							// same arrow as currently nearest arrow?
							if (a==aNearest) {
								soundToPlay.append(";AGAIN");							
							}
							
							//System.out.println(soundToPlay.toString());
						}
					}
					// if the sum of movement away from the next arrow
					// is much too high then recalculate route
					if ( sumWrongDirection >= PREPAREDISTANCE * 2 / 3
							|| sumWrongDirection >= 300) {
							routeRecalculationRequired = true;
					// if the sum of movement away from the next arrow is high
			    	} else if ( sumWrongDirection >= PREPAREDISTANCE / 3
						|| sumWrongDirection >= 150) {
			    		// if distance to next arrow is high
		    			// and moving away from next arrow
			    		// ask user to check direction
			    		if (diffArrowDist > 0) {
				    		soundToPlay.setLength(0);
				    		soundToPlay.append ("CHECK_DIRECTION");
				    		soundRepeatDelay=30;
				    		checkDirectionSaid = true;
				    		routeInstructionColor=0x00E6A03C;
			    		} else if (diffArrowDist == 0) {
			    			routeInstructionColor = oldRouteInstructionColor;
			    		}
			    	}
					pc.g.drawImage(pict,pc.lineP2.x,pc.lineP2.y,CENTERPOS);
					/*
					Font originalFont = pc.g.getFont();
					if (smallBoldFont==null) {
						smallBoldFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
						smallBoldFontHeight=smallBoldFont.getHeight();
					}
					pc.g.setFont(smallBoldFont);
					pc.g.setColor(0,0,0);
					int turnOrg=(c.startBearing - lastEndBearing)*2;
					pc.g.drawString("S: " + c.startBearing*2 + " E: " + lastEndBearing*2 + " C: " + course + " T: " + turn + "(" + turnOrg + ")",
							pc.lineP2.x,
							pc.lineP2.y-smallBoldFontHeight / 2,
							Graphics.HCENTER | Graphics.TOP
					);
					pc.g.setFont(originalFont);
					*/
					
					lastEndBearing=c.endBearing;
					lastTo=c.to;
				}
			}
			/* if we just moved away from target,
			 * and the map is gpscentered
			 * and there's only one or no route arrow
			 * ==> auto recalculation
			 */
			if (trace.movedAwayFromTarget
				&& trace.gpsRecenter
				&& (route != null && route.size()==2)
				&& ProjMath.getDistance(target.lat, target.lon, center.radlat, center.radlon) > PREPAREDISTANCE
			) {
				routeRecalculationRequired=true;
			}
			if ( routeRecalculationRequired && !trace.atTarget ) {
				soundToPlay.setLength(0);
				trace.autoRouteRecalculate();				
				if (diffArrowDist > 0) {
					// use red background color if moving away
					routeInstructionColor=0x00FF5402;
				} else if (diffArrowDist == 0) {
					routeInstructionColor = oldRouteInstructionColor;
				}
			}
		}
		// Route instruction text output
		if ((routeInstruction != null)) {
			Font originalFont = pc.g.getFont();
			if (routeFont==null) {
				routeFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
				routeFontHeight=routeFont.getHeight();
			}
			pc.g.setFont(routeFont);
			pc.g.setColor(routeInstructionColor);
			oldRouteInstructionColor=routeInstructionColor;
			pc.g.fillRect(0,textYPos-routeFontHeight, pc.xSize, routeFontHeight);
			pc.g.setColor(0,0,0);
			pc.g.drawString(routeInstruction,
					pc.xSize/2,
					textYPos,
					Graphics.HCENTER | Graphics.BOTTOM
			);
			pc.g.setFont(originalFont);
		}
		// Route instruction sound output
		if (soundToPlay.length()!=0 && Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
			GpsMid.mNoiseMaker.playSound(soundToPlay.toString(), (byte) soundRepeatDelay, (byte) soundMaxTimesToPlay);
		}
	}

	/**
	 * @param pc
	 */
	public void showRoute(PaintContext pc, PositionMark source, Node center, int textYPos) {
		/*	PASSINGDISTANCE is the distance when a routing arrow
			is considered to match to the current position.
			We currently can't adjust this value according to the speed
			because if we would be slowing down during approaching the arrow,
			then PASSINGDISTANCE could become smaller than the distance
			to the arrow due and thus the routines would already use the
			next arrow for routing assistance
		*/
		final int PASSINGDISTANCE=25;

		StringBuffer soundToPlay = new StringBuffer();
		StringBuffer sbRouteInstruction = new StringBuffer();
    	// backgound colour for standard routing instructions
		byte soundRepeatDelay=3;
		byte soundMaxTimesToPlay=2;
		

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
		synchronized(this) {
			if (route != null && route.size() > 0){
				// there's a route so no calculation required
				routeRecalculationRequired=false;
	
				// find nearest routing arrow (to center of screen)
				int iNow=0;
				int iRealNow=0;
				byte aNow=RI_STRAIGHT_ON_QUIET;
				int iThen=0;
				byte aThen=RI_STRAIGHT_ON_QUIET;
				byte aPaint=RI_STRAIGHT_ON_QUIET;
				double distNow=0;
				int intDistNow=0;
				
				if (routePathConnection != -1 && routePathConnection < route.size()-1) {
					iRealNow = routePathConnection+1;
					iNow = idxNextInstructionArrow (iRealNow);
					cNow = (ConnectionWithNode) route.elementAt(iNow);
					aNow = cNow.wayRouteInstruction;
			    	distNow=ProjMath.getDistance(center.radlat, center.radlon, cNow.to.lat, cNow.to.lon);
					intDistNow=new Double(distNow).intValue();
					iThen = idxNextInstructionArrow (iNow+1);
					if (routePathConnection < route.size()-2) {
						cThen = (ConnectionWithNode) route.elementAt(iThen);
						aThen = cThen.wayRouteInstruction;
					}
				}

				c = (ConnectionWithNode) route.elementAt(0);
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
						case RI_HALF_RIGHT:		pict=pc.images.IMG_HALFRIGHT; break;
						case RI_STRAIGHT_ON_QUIET:
						case RI_STRAIGHT_ON: 	pict=pc.images.IMG_STRAIGHTON; break;
						case RI_HALF_LEFT:		pict=pc.images.IMG_HALFLEFT; break;
						case RI_LEFT:			pict=pc.images.IMG_LEFT; break;
						case RI_HARD_LEFT:		pict=pc.images.IMG_HARDLEFT; break;
						case RI_ENTER_MOTORWAY:	pict=pc.images.IMG_MOTORWAYENTER; break;
						case RI_LEAVE_MOTORWAY:	pict=pc.images.IMG_MOTORWAYLEAVE; break;					
						case RI_INTO_TUNNEL:	pict=pc.images.IMG_TUNNEL_INTO; break;
						case RI_OUT_OF_TUNNEL:	pict=pc.images.IMG_TUNNEL_OUT_OF; break;					
					}
					
					if (trace.atTarget) {
						aPaint=RI_TARGET_REACHED;
					}
					pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
				    // optionally scale nearest arrow
				    if (i==iNow) {
						if (aNow!=cachedPicArrow) {
							cachedPicArrow=aNow;							
							if (aNow < RI_ENTER_MOTORWAY) {
								scaledPict=doubleImage(pict); //ImageTools.scaleImage(pict, pict.getWidth() * 3 / 2, pict.getHeight() * 3 / 2);
							} else {
								scaledPict=pict;
							}
						}
						pict=scaledPict;						
				    		
				    	sbRouteInstruction.append(directions[aNow]);
						// if nearest route arrow is closer than PASSINGDISTANCE meters we're currently passing this route arrow
				    	if(distNow<PASSINGDISTANCE) {
							if (iPassedRouteArrow != iNow) {
								iPassedRouteArrow = iNow;
								// if there's i.e. a 2nd left arrow in row "left" must be repeated
								if (!trace.atTarget) {
									sLastInstruction="";
								}
							}
							// after passing an arrow all instructions, i.e. saying "in xxx metres" are allowed again 
							resetVoiceInstructions();

							if (!trace.atTarget) { 
								soundToPlay.append (soundDirections[aNow]);
							}
							soundMaxTimesToPlay=1;
						} else {
							sbRouteInstruction.append(" in " + intDistNow + "m");
						}
						if (intDistNow>=PASSINGDISTANCE && !checkDirectionSaid) {
							if (intDistNow <= PREPAREDISTANCE) {
								if (aNow < RI_ENTER_MOTORWAY) {
									soundToPlay.append( (aNow==RI_STRAIGHT_ON ? "CONTINUE" : "PREPARE") + ";" + soundDirections[aNow]);
								} else if (aNow>=RI_ENTER_MOTORWAY && aNow<=RI_LEAVE_MOTORWAY) {
									soundToPlay.append("PREPARE;TO;" + soundDirections[aNow]);
								} else if (aNow>=RI_1ST_EXIT && aNow<=RI_6TH_EXIT) {
									soundToPlay.append(soundDirections[aNow]);
								}
								soundMaxTimesToPlay=1;
								// Because of adaptive-to-speed distances for "prepare"-instructions
								// GpsMid could fall back from "prepare"-instructions to "in xxx metres" voice instructions
								// Remembering and checking if the prepare instruction already was given since the latest passing of an arrow avoids this
								prepareInstructionSaid = true;
							} else if (intDistNow < 900 && intDistNow < getTellDistance(iNow) ) { //&& !prepareInstructionSaid) {
								soundRepeatDelay=60;
								soundToPlay.append("IN;" + Integer.toString(intDistNow / 100)+ "00;METERS;" + soundDirections[aNow]);								
							}							
						}
						
						double distNowThen=ProjMath.getDistance(cNow.to.lat, cNow.to.lon, cThen.to.lat, cThen.to.lon);
						// if there is a close direction arrow after the current one
						// inform the user about its direction
						if (distNowThen <= PREPAREDISTANCE &&
							// only if not both arrows are STRAIGHT_ON
							!(aNow==RI_STRAIGHT_ON && (aThen == RI_STRAIGHT_ON || aThen == RI_STRAIGHT_ON_QUIET) ) &&
							// and it is not a round about exit instruction
							!(aNow>=RI_1ST_EXIT && aNow<=RI_6TH_EXIT) &&
							// and only as continuation of instruction
							soundToPlay.length()!=0
						   ) {
							soundToPlay.append(";THEN;");
							if (distNowThen > PASSINGDISTANCE) {
								soundToPlay.append("SOON;");
							}
							soundToPlay.append(soundDirections[aThen]);
							// same arrow as currently nearest arrow?
							if (aNow==aThen) {
								soundToPlay.append(";AGAIN");							
							}
							
							//System.out.println(soundToPlay.toString());
						}
					}
					if (aPaint == RI_SKIPPED) {
						pc.g.setColor(0x00FDDF9F);
						pc.getP().forward(c.to.lat, c.to.lon, pc.lineP2);
						final byte radius=6;
						pc.g.fillArc(pc.lineP2.x-radius/2,pc.lineP2.y-radius/2,radius,radius,0,359);
					} else {
						pc.g.drawImage(pict,pc.lineP2.x,pc.lineP2.y,CENTERPOS);					
					}
				}
			}
			routeRecalculationRequired = isOffRoute(route, center);
			if ( routeRecalculationRequired && !trace.atTarget ) {
				soundToPlay.setLength(0);
				trace.autoRouteRecalculate();				
			}
		}
		// Route instruction text output
		if (sbRouteInstruction.length() != 0) {
			Font originalFont = pc.g.getFont();
			if (routeFont==null) {
				routeFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
				routeFontHeight=routeFont.getHeight();
			}
			pc.g.setFont(routeFont);
			pc.g.setColor(routeInstructionColor);
			pc.g.fillRect(0,textYPos-routeFontHeight, pc.xSize, routeFontHeight);
			pc.g.setColor(0,0,0);
			pc.g.drawString(sbRouteInstruction.toString(),
					pc.xSize/2,
					textYPos,
					Graphics.HCENTER | Graphics.BOTTOM
			);
			pc.g.drawString("off:" + dstToRoutePath + "m",
					pc.xSize,
					textYPos - routeFontHeight,
					Graphics.RIGHT | Graphics.BOTTOM
			);
			
			pc.g.setFont(originalFont);
		}
		// Route instruction sound output
		if (soundToPlay.length()!=0 && Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS)) {
			if ( !sLastInstruction.equals( soundToPlay.toString() ) ) {
				sLastInstruction = soundToPlay.toString();
				GpsMid.mNoiseMaker.playSound(sLastInstruction, (byte) soundRepeatDelay, (byte) soundMaxTimesToPlay);
				System.out.println(sLastInstruction);
			}
		}
	}
	
	private boolean isOffRoute(Vector route, Node center) {
		// never recalculate during route calculation
		if (trace.rootCalc) return false;

		/* if we did no initial recalculation,
		 * the map is gpscentered
		 * and we just moved away from target
		 * ==> auto recalculation
		 */
		if (
			!initialRecalcDone
			&& trace.gpsRecenter
			&& trace.movedAwayFromTarget
		) {
//			System.out.println("initial recalc");
			initialRecalcDone = true;
			return true;
		}
		
//		System.out.println("Counter: " + riCounter++);
		if (dstToRoutePath < 25) {
			// FIXME: dstToRoutePath is 0 instead of Integer.Max_VALUE for a short time after Route Calculation
			if (dstToRoutePath > 0) {
		    	// green background color if onroute
	    		routeInstructionColor=0x00B7FBBA;
				haveBeenOnRouteSinceCalculation=true;
				System.out.println("on route dstToRoutePath: " + dstToRoutePath);
			}
		}
		ConnectionWithNode c0 = (ConnectionWithNode) route.elementAt(0);
		// calculate distance to first arrow after calculation
		int dstToFirstArrow = (int) (ProjMath.getDistance(center.radlat, center.radlon, c0.to.lat, c0.to.lon));
		if ((haveBeenOnRouteSinceCalculation && dstToRoutePath >= 50) ||
			(!haveBeenOnRouteSinceCalculation && (dstToFirstArrow - startDstToFirstArrowAfterCalculation) > 50)
		) {
			// use red background color
			routeInstructionColor=0x00FF5402;
//			System.out.println("recalc startDst: " + startDstToFirstArrowAfterCalculation);
//			System.out.println("recalc dst1st: " + dstToFirstArrow);
//			System.out.println("haveBeenOnRouteSinceCalculation: " + haveBeenOnRouteSinceCalculation);
//			System.out.println("dstToRoutePath: " + dstToRoutePath);
			if (trace.source != null) {
				return true;
			}
		} else if (
			(haveBeenOnRouteSinceCalculation && dstToRoutePath >= 25) ||
			(!haveBeenOnRouteSinceCalculation && (dstToFirstArrow - startDstToFirstArrowAfterCalculation) > 25)
		) {
			// use orange background color
			routeInstructionColor=0x00FFCD9B;
		}		
		return false;
	}
	
	
	public static void resetOffRoute(Vector route, Node center) {
		haveBeenOnRouteSinceCalculation = false;
		dstToRoutePath=Integer.MAX_VALUE;
		if (route!=null) {
			ConnectionWithNode c0 = (ConnectionWithNode) route.elementAt(0);
			// calculate distance to first arrow after calculation
			startDstToFirstArrowAfterCalculation = (int) (ProjMath.getDistance(center.radlat, center.radlon, c0.to.lat, c0.to.lon));
		} else {
			startDstToFirstArrowAfterCalculation=50;
		}
		// dark grey
		routeInstructionColor=0x00808080;
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
		
		if (route.size() < 3) {
			return;
		}
		for (int i=0; i<route.size()-1; i++){
			c = (ConnectionWithNode) route.elementAt(i);

			byte rfCurr=c.wayRouteFlags;
			byte rfPrev=0;
			if (i > 0) {
				c2 = (ConnectionWithNode) route.elementAt(i-1);
				rfPrev=c2.wayRouteFlags;
			}			
			byte rfNext=0;
			nextStartBearing = 0;
			if (i < route.size()-2) {
				c2 = (ConnectionWithNode) route.elementAt(i+1);
				rfNext=C.getWayDescription(c2.wayType).routeFlags;
				nextStartBearing = c2.startBearing;
			}
			
			byte ri=0;
			// into tunnel
			if (	(rfPrev & C.ROUTE_FLAG_TUNNEL) == 0
				&& 	(rfCurr & C.ROUTE_FLAG_TUNNEL) > 0
			) {
				ri = RI_INTO_TUNNEL;
			}
			// out of tunnel
			if (	(rfPrev & C.ROUTE_FLAG_TUNNEL) > 0
				&& 	(rfCurr & C.ROUTE_FLAG_TUNNEL) == 0
			) {
				ri = RI_OUT_OF_TUNNEL;
			}
			// enter motorway
			if ( 	(rfPrev & (C.ROUTE_FLAG_MOTORWAY | C.ROUTE_FLAG_MOTORWAY_LINK)) == 0
				&& 	(rfCurr & (C.ROUTE_FLAG_MOTORWAY | C.ROUTE_FLAG_MOTORWAY_LINK)) > 0
			) {
				ri = RI_ENTER_MOTORWAY;
			}
			// leave motorway
			if (	(rfPrev & C.ROUTE_FLAG_MOTORWAY) > 0
				&& 	(rfCurr & C.ROUTE_FLAG_MOTORWAY) == 0
			) {
				ri = RI_LEAVE_MOTORWAY;
			}

			// determine roundabout exit
			if ( 	(rfPrev & C.ROUTE_FLAG_ROUNDABOUT) == 0
				&& 	(rfCurr & C.ROUTE_FLAG_ROUNDABOUT) > 0
			) {
				ri = RI_1ST_EXIT;	
				int i2;
				for (i2=i+1; i2<route.size()-1 && (ri < RI_6TH_EXIT); i2++) {
					c2 = (ConnectionWithNode) route.elementAt(i2);
					if ( (c2.wayRouteFlags & C.ROUTE_FLAG_ROUNDABOUT) == 0 ) { 
						break;
					}
					ri++;
				}
				for (int i3=i2-1; i3>i; i3--) {
					c2 = (ConnectionWithNode) route.elementAt(i3);
					c2.wayRouteInstruction=ri;					
				}
				i=i2-1;				
			}
			// if we've got no better instruction, just use the direction
			if (ri==0) {				
				ri = convertTurnToRouteInstruction( (nextStartBearing - c.endBearing) * 2 );
			}
			c.wayRouteInstruction = ri;
		}
		
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
				cPrev.wayDistanceToNext += c.wayDistanceToNext;
				//c.wayDistanceToNext = 0;
				cPrev.wayNameIdx = c.wayNameIdx;
				ConnectionWithNode cNext = (ConnectionWithNode) route.elementAt(i+1);
				cPrev.wayRouteInstruction = convertTurnToRouteInstruction( (cNext.startBearing - cPrev.endBearing) * 2 );				
			}
			cPrev=c;
		}
		
		// replace redundant straight-ons by quiet arrows and add way distance to starting arrow of the street
		ConnectionWithNode cStart;
		for (int i=2; i<route.size()-1; i++){
			c = (ConnectionWithNode) route.elementAt(i);
			cStart = (ConnectionWithNode) route.elementAt(i-1);
			while (c.wayRouteInstruction == RI_STRAIGHT_ON && c.wayNameIdx == cStart.wayNameIdx && i<route.size()-2) {
				cStart.wayDistanceToNext += c.wayDistanceToNext;
				c.wayRouteInstruction = RI_STRAIGHT_ON_QUIET;
				// c.wayDistanceToNext = 0;
				i++;
				c = (ConnectionWithNode) route.elementAt(i);
			}			
		}
	}
	
	
	private int getTellDistance(int iConnection) {
		ConnectionWithNode cPrev = (ConnectionWithNode) route.elementAt(iConnection -1);
		
		int distFromSpeed = 200;
		int distFromPrevConn = (int) cPrev.wayDistanceToNext + 50;
		int speed=trace.speed;
		if (speed>100) {
			distFromSpeed=500;							
		} else if (speed>80) {
			distFromSpeed=400;							
		} else if (speed>40) {
			distFromSpeed=300;																			
		}
		return Math.max(distFromSpeed, distFromPrevConn);
	}
	
	
	private int idxNextInstructionArrow(int i) {
		ConnectionWithNode c;
		int a;
		for (a=i; a<route.size()-2; a++){
			c = (ConnectionWithNode) route.elementAt(a);
			if (
				c.wayRouteInstruction != RI_STRAIGHT_ON_QUIET
			&&	c.wayRouteInstruction != RI_SKIPPED
			) {
				break;
			}
		}
		return a;
	}
	
	private byte convertTurnToRouteInstruction(int turn) {
		if (turn > 180) turn -= 360;
		if (turn < -180) turn += 360;
		if (turn > 110) {
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
		} else {
			return RI_HARD_LEFT;
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
			if (ri!=RI_STRAIGHT_ON_QUIET && ri!=RI_SKIPPED) {
				sb.append(i + ". ");
				sb.append(directions[ri]);
				sb.append(" into ");
				sb.append((name==null?"":name));
				sb.append(" then go ");
				sb.append(dist);
				sb.append("m");
				if ( (c.wayRouteFlags & C.ROUTE_FLAG_ROUNDABOUT) > 0) { 
					sb.append(" (in roundabout)");
				}
				sb.append(" Cons:" + c.to.conSize);
				System.out.println(sb.toString());
			}
		}		
	}
}
