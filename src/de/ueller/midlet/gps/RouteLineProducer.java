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


public class RouteLineProducer implements Runnable {
	private final static Logger logger = Logger.getInstance(RouteLineProducer.class,Logger.DEBUG);

	private static volatile boolean abort = false;

	/** the index of the route element until which the route line is produced and thus we can determine route instructions for */
	public static volatile int maxRouteElementDone;

	private static Trace trace;
	public static Vector route;
	
	public static intTree routeLineTree;
	public final static Boolean trueObject = new Boolean(true);
	
	private static int connsFound = 0;
	public static volatile int notifyWhenAtElement = Integer.MAX_VALUE;
	private static volatile Thread producerThread = null;
	

	public void determineRoutePath(Trace trace, Vector route) throws Exception {
		// terminate any previous RouteLineProducers
		abort();
		RouteLineProducer.maxRouteElementDone = 0;
		routeLineTree = new intTree();
		RouteLineProducer.trace = trace;
		RouteLineProducer.route = route;		
		producerThread = new Thread(this, "RouteLineProducer");
		producerThread.setPriority(Thread.MIN_PRIORITY);
		producerThread.start();
	}
	
	public void run() {	
		try {
			PaintContext pc = new PaintContext(trace, null);
			connsFound=0;
			float routeLen=0f;
			long startTime = System.currentTimeMillis();
			pc.searchConPrevWayRouteFlags = 0;
			if (route != null && route.size() > 1){
	//			LayoutElement e = trace.tl.ele[TraceLayout.ROUTE_DISTANCE];
				for (int i=0; i<route.size()-1 && !RouteLineProducer.abort; i++){
					//#debug debug
					logger.debug("determineRoutePath " + i + "/" + (route.size() - 2) );
					routeLen += searchConnection2Ways(pc, i);
					RouteLineProducer.maxRouteElementDone = i;
					// when route line is produced until notifyWhenAtElement, wake up getRouteElement()
					if (i >= notifyWhenAtElement) {
						synchronized (this) {
							//#debug debug
							logger.debug("notifying " + i );
							notifyWhenAtElement = Integer.MAX_VALUE;
							notify();
						}
					}
				}
				if (!RouteLineProducer.abort) {
					maxRouteElementDone = route.size();
					//#debug debug
					logger.debug("Connection2Ways found: " + connsFound + "/" + (route.size()-1) + " in " + (long)(System.currentTimeMillis() - startTime) + " ms");
					trace.receiveMessage ("Route: " + (int) routeLen + "m" + (connsFound==(route.size()-1)?"":" (" + connsFound + "/" + (route.size()-1) + ")"));
				} else {
					//#debug debug
					logger.debug("RouteLineProducer aborted at " + connsFound + "/" + (route.size()-1));					
					maxRouteElementDone = 0;
				}
			}
		} catch (Exception e) {
			//#debug error
			logger.error("RouteLineProducer crashed with " +  e.getMessage());
			e.printStackTrace();
		}
		producerThread = null;
		synchronized (this) {
			notifyAll();
		}
	}

			
	public static float searchConnection2Ways(PaintContext pc, int iConnFrom) throws Exception {		
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
		pc.conWayBearingsCount = 0;
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
			cFrom.numToRoutableWays = pc.conWayNumToRoutableWays;
			if ( (pc.searchConPrevWayRouteFlags & Legend.ROUTE_FLAG_ONEDIRECTION_ONLY) == 0) {
				/* if we are NOT coming from a oneway substract the way we are coming from as a way we can route to
				 * (for oneways the way we are coming from has not been counted anyway)
				 */
				cFrom.numToRoutableWays--;
			}
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
			for (int b = 0; b < pc.conWayBearingsCount; b++) {
				byte bearingAlternative = pc.conWayBearings[b];
//				System.out.println(bearing);
				
				if (cTo.wayConStartBearing != bearingAlternative) {					
					byte riRoute = RouteInstructions.convertTurnToRouteInstruction( (cTo.wayConStartBearing - cFrom.wayConEndBearing) * 2 );
					byte riCheck = RouteInstructions.convertTurnToRouteInstruction( (bearingAlternative - cFrom.wayConEndBearing) * 2 );
					// if we got a second straight-on way at the connection, we need to tell the bearing
					if (
						(riRoute == RouteInstructions.RI_STRAIGHT_ON && riCheck == RouteInstructions.RI_STRAIGHT_ON)
						// if there's exactly one alternative to leave/enter the motorway don't add the bearing
						&& pc.conWayNumMotorways != 1
					) {
						int iBearingAlternative = (int) (bearingAlternative) * 2;
						if (iBearingAlternative < 0) iBearingAlternative += 360;

						int iBearingRoute = (int) (cTo.wayConStartBearing) * 2;
						if (iBearingRoute < 0) iBearingRoute += 360;

						//System.out.println("" + iBearingRoute + " " + iBearingAlternative);

						// if the bearing difference is more than 180 degrees, the angle in the other direction is smaller,
						// so simply swap signs of the bearings to make the later comparison give the opposite result 
						if (Math.abs(iBearingRoute - iBearingAlternative) > 180) {
							iBearingAlternative = -iBearingAlternative;
							iBearingRoute = -iBearingRoute;
							//System.out.println("changed signs: " + iBearingRoute + " " + iBearingAlternative);
						}
						
						if (iBearingRoute < iBearingAlternative) {
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
			
			pc.searchConPrevWayRouteFlags = cFrom.wayRouteFlags;
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
		if (!RouteLineProducer.abort) {
			routeLineTree.put(pc.conWayCombinedFileAndWayNr, trueObject);
		}
		return cFrom.wayDistanceToNext;
	}	


	public static boolean isWayIdUsedByRouteLine(int wayId) {
		return (routeLineTree != null && routeLineTree.get(wayId) != null);
	}

	public static boolean isRouteLineProduced() {
		return (route != null && maxRouteElementDone == route.size());
	}
	
	
	/** abort the current route line production */
	
	public synchronized void abort() {
		RouteLineProducer.abort = true;
		notifyAll();
		try {
			while ((producerThread != null) && (producerThread.isAlive())) {
				wait(1000);
			}
		} catch (InterruptedException e) {
			//Nothing to do
		}
		RouteLineProducer.abort = false;
	}
	
	/** returns if a route line is currently produced */
	public static boolean isRunning() {
		return (producerThread != null); 
	}
	
	/** wait until route line is produced up to route element at index i */
	public void waitForRouteLine(int i) {
		//#debug debug
		logger.debug("waitForRouteLine:" + i + ", maxRouteElement: " + maxRouteElementDone);
		while (i >= maxRouteElementDone && !RouteLineProducer.abort) {
			synchronized (this) {
				try {
					notifyWhenAtElement = i;
					wait(5000);
					//#debug debug
					logger.debug(" routeLineWait timed out waiting for: " + i + " maxRouteElement: " + maxRouteElementDone);
				} catch(InterruptedException e) {
					//#debug debug
					logger.debug(" routeLineWait notified " + i);
				}
			}
		}
	}

}