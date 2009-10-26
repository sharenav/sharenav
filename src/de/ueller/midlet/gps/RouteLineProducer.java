/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying 
 */

package de.ueller.midlet.gps;

import java.util.Vector;

import de.ueller.gps.data.Legend;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.intTree;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Proj2D;
import de.ueller.midlet.gps.routing.ConnectionWithNode;
import de.ueller.midlet.gps.tile.PaintContext;


public class RouteLineProducer {
	public static intTree routeLineTree;
	public final static Boolean trueObject = new Boolean(true);
	private static int connsFound = 0;
	private static Trace trace;

	
	public static void determineRoutePath(Trace trace, Vector route) throws Exception {
		RouteLineProducer.routeLineTree = new intTree();
		RouteLineProducer.trace = trace;
		PaintContext pc = new PaintContext(trace, null);
		connsFound=0;
		float routeLen=0f;
		long startTime = System.currentTimeMillis();
		pc.searchConPrevWayRouteFlags = 0;
		if (route != null && route.size() > 1){
//			LayoutElement e = trace.tl.ele[TraceLayout.ROUTE_DISTANCE];
			for (int i=0; i<route.size()-1; i++){
				//#debug debug
				logger.debug("determineRoutePath " + i + "/" + (route.size() - 2) );
				routeLen += searchConnection2Ways(pc, route, i);
//				e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE]);
//				e.setText("" + (int) routeLen + "m");
//				e.paint(trace.pc.g);
//				if (i % 10 == 0) {
//					trace.repaint();
//				}
			}
//			e.setBackgroundColor(Legend.COLORS[Legend.COLOR_RI_DISTANCE_BACKGROUND]);
			//parent.alert ("Connection2Ways", "found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms", 3000);
			//#debug debug
			logger.debug("Connection2Ways found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms");
			trace.receiveMessage ("Route: " + (int) routeLen + "m" + (connsFound==(route.size()-1)?"":" (" + connsFound + "/" + (route.size()-1) + ")"));
		}
	}
		
	public static float searchConnection2Ways(PaintContext pc, Vector route, int iConnFrom) throws Exception {		
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
			cFrom.wayDurationToNext = pc.conWayDurationToNext;
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
					byte riReal = RouteInstructions.convertTurnToRouteInstruction( (cTo.wayConStartBearing - cFrom.wayConEndBearing) * 2 );
					byte riCheck = RouteInstructions.convertTurnToRouteInstruction( (bearing - cFrom.wayConEndBearing) * 2 );
					// if we got a second straight-on way at the connection, we need to tell the bearing
					if (
						(riReal == RouteInstructions.RI_STRAIGHT_ON && riCheck == RouteInstructions.RI_STRAIGHT_ON)
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

}

