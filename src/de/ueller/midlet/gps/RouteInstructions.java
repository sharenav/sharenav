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
	};
	private static final String[] soundDirections  = { "",
		"HARD;RIGHT", "RIGHT", "HALF;RIGHT",
		"STRAIGHTON",
		"HALF;LEFT", "LEFT", "HARD;LEFT", "TARGET_REACHED",
	};

	private int connsFound = 0;
	
	private int sumWrongDirection=0;
	private int oldAwayFromNextArrow=0;
	private int oldRouteInstructionColor=0x00E6E6E6;
	private static boolean prepareInstructionSaid=false;
	private static boolean checkDirectionSaid=false;
	private static boolean autoRecalcIfRequired=false;
	
	private int iPassedRouteArrow=0;
	private static Font routeFont;
	private static int routeFontHeight;

	private byte arrow;
	private static final int CENTERPOS = Graphics.HCENTER|Graphics.VCENTER;
	private Image scaledPict = null;

	private static Trace trace;
	private static Vector route;
	private static PositionMark target;

	
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
		if (route != null && route.size() > 0){
			for (int i=0; i<route.size()-1; i++){
				routeLen += searchConnection2Ways(pc, i);
			}
			//parent.alert ("Connection2Ways", "found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms", 3000);
			//#debug debug
			logger.debug("Connection2Ways found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms");
			trace.receiveMessage ("Route: " + (int) routeLen + "m");
		}
	}
		
	public float searchConnection2Ways(PaintContext pc, int iConnFrom) throws Exception {		
		ConnectionWithNode c;
		c = (ConnectionWithNode) route.elementAt(iConnFrom);
		// take a bigger angle for lon because of positions near to the pols.		
		Node nld=new Node(c.to.lat - 0.0001f,c.to.lon - 0.0005f,true);
		Node nru=new Node(c.to.lat + 0.0001f,c.to.lon + 0.0005f,true);
		pc.searchCon1Lat = c.to.lat;
		pc.searchCon1Lon = c.to.lon;
		c = (ConnectionWithNode) route.elementAt(iConnFrom+1);
		pc.searchCon2Lat = c.to.lat;
		pc.searchCon2Lon = c.to.lon;
		
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
			c = (ConnectionWithNode) route.elementAt(iConnFrom);
			c.wayFromConAt = pc.conWayFromAt;
			c.wayToConAt = pc.conWayToAt;
			c.wayNameIdx = pc.conWayNameIdx;
			c.wayType = pc.conWayType;
			c.wayDistanceToNext = pc.conWayDistanceToNext;
			connsFound++;
			return c.wayDistanceToNext;
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
						if (a!=arrow) {
							arrow=a;
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
				System.out.println("offRoute1");
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

	
}