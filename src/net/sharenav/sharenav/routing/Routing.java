/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.sharenav.routing;

import java.io.IOException;
import java.lang.Math;
import java.util.Vector;
import de.enough.polish.util.Locale;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.sharenav.data.RoutePositionMark;
import net.sharenav.sharenav.mapdata.DictReader;
import net.sharenav.sharenav.mapdata.Way;
import net.sharenav.sharenav.tile.RouteBaseTile;
import net.sharenav.sharenav.tile.Tile;
import net.sharenav.sharenav.ui.ShareNav;
//#if polish.api.finland
import net.sharenav.sharenav.ui.GuiWebInfo;
//#endif
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.util.IntTree;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;



public class Routing implements Runnable {
	private Thread processorThread;
	public boolean bestTime = !Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AIM);

	private final Vector nodes = new Vector();
	private final IntTree open = new IntTree();
	private final IntTree closed = new IntTree();
	private Runtime runtime = Runtime.getRuntime();

	private final static Logger logger = Logger.getInstance(Routing.class, Logger.ERROR);
	private RouteBaseTile tile;
	private RouteBaseTile currentTile = null;
	private boolean markSuccessorsToFullFillMainstreetNetDistance = false;
	private RouteNode routeFrom;
	private RouteNode routeTo;
	private volatile RoutePositionMark fromMark;
	private final Trace parent;
	private int bestTotal;
	private long nextUpdate;
	private static volatile boolean stopRouting = false;
	private float estimateFac = 1.40f;
	/** maximum speed estimated in m/s */
	private int maxEstimationSpeed;
	/** use maximum route calulation speed instead of deep search */
	private boolean roadRun = false;
	private boolean determinedAgainstDirectionPenalty = false;
	private boolean avoidStartingInOsmWayDirection;
	public boolean routeStartsAgainstMovingDirection = false;
	
	/** when true, the RouteTile will only load and return mainStreetNet RouteNodes, Connections and TurnRestrictions */ 
	public static volatile boolean onlyMainStreetNet = false;
	public int motorwayConsExamined = 0;
	public int motorwayEntrancesExamined = 0;
	
	public boolean tryFindMotorway = false;
	public boolean boostMotorways = false;
	public boolean boostTrunksAndPrimarys = false;	
	public boolean useMotorways = false;
	public boolean useTollRoads = false;	
	public boolean showRouteHelpers = false;
	public boolean showConnectionTraces = false;
	
	private int oomCounter = 0;
	private int expanded;
	private RouteNode sourcePathSegNodeDummyRouteNode = new RouteNode();
	private Connection sourcePathSegNodeDummyConnection = new Connection();
	/**
	 * Dummy ConnectionWithNode at the path segment node of the source way to begin
	 * This arbitrary position on the way's path will be detected as part of the route line by searchConnection2Ways 
	 */
	private ConnectionWithNode sourcePathSegNodeDummyConnectionNode = new ConnectionWithNode(sourcePathSegNodeDummyRouteNode, sourcePathSegNodeDummyConnection);
	int firstNodeId1 = 0;
	Node firstSourcePathSegNodeDummy1 = new Node();
	int firstNodeId2 = 0;
	Node firstSourcePathSegNodeDummy2 = new Node();

	/**
	 * Alternatives of path segments closest to the destination
	 * One of these arbitrary positions on the way's path will be after the route calculation
	 * filled into the last connection (which is initially a connection at the dest. position).
	 * Thus it will be detected as part of the route line by searchConnection2Ways .
	 * 
	 * Which position will be filled in,
	 * depends on which final connection gets selected by the routing algorithm.
	 */
	int finalNodeId1 = 0;
	Node finalDestPathSegNodeDummy1 = new Node();
	int finalNodeId2 = 0;
	Node finalDestPathSegNodeDummy2 = new Node();
	
	private long searchStartTime;
	
	// to-Connections-Cache
	private static volatile RouteNode destRn = null;
	private static volatile RouteNode destPrevRn = null;
	private static volatile int distToMainstreetNetRouteNode = 0;
	private static volatile int currentTravelMask = 0;
	private static volatile RoutePositionMark toMark;	

	public Routing(Trace parent) throws IOException {
		this.parent = parent;
		
		estimateFac = (Configuration.getRouteEstimationFac() / 10f) + 0.85f;
		if (Configuration.getRouteEstimationFac() > 0) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_SUPPRESS_ROUTE_WARNING)) {
				parent.alert(Locale.get("routing.RoutingWarningTitle")/*Routing warning*/,
					     Locale.get("routing.RoutingWarning")/*Routes may be longer than necessary, as allow poor routes setting is on*/, ShareNav.isRunningInMicroEmulator() ? 1500 : 5000);
			}
		}
		maxEstimationSpeed = (int) ( (Configuration.getTravelMode().maxEstimationSpeed * 10) / 36);
		if (maxEstimationSpeed == 0) {
			maxEstimationSpeed = 1; // avoid division by zero
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_TURBO_ROUTE_CALC) ) {
			// activate turbo route calculation
			if (maxEstimationSpeed >= 14) {
				// set roadRun mode if travel mode's maxEstimationSpeed >= 14 m/s (50 km/h), i.e. for motorized vehicles
				roadRun = true;
			} else {
				// for non-motorized vehicles don't set highest estimateFac,
				// it results in bad routes
				// estimateFac = 1.8f;
				roadRun = false;
			}
		}
		if (currentTravelMask != Configuration.getTravelMask()) {
			dropToConnectionsCache();
			currentTravelMask = Configuration.getTravelMask();
		}
		showRouteHelpers = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTEHELPERS);
		showConnectionTraces = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTECONNECTION_TRACES);
	}
	
	private GraphNode search(RouteNode dest) throws Exception {
		currentTile = tile;
		GraphNode currentNode;
		int successorCost;
		Vector children = new Vector();
		searchStartTime = System.currentTimeMillis();
		expanded=0;
		
		boolean checkForTurnRestrictions =
			Configuration.getCfgBitState(Configuration.CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION) &&
			Configuration.getTravelMode().isWithTurnRestrictions();
		
		useMotorways = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_USE_MOTORWAYS);
		useTollRoads = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_USE_TOLLROADS);
		tryFindMotorway = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_TRY_FIND_MOTORWAY);
		boostMotorways = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BOOST_MOTORWAYS);
		boostTrunksAndPrimarys = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS);
		int trafficSignalCalcDelayTSecs = Configuration.getTrafficSignalCalcDelay() * 10;
		
		// set a mainStreetNetDistance that is unreachable to indicate the mainStreetNet is disabled
		int mainStreetNetDistanceMeters = 40000000;
		if (Configuration.getTravelMode().useMainStreetNetForLargeRoutes()) {
			mainStreetNetDistanceMeters = Configuration.getMainStreetDistanceKm() * 1000;
//			System.out.println("use main street net");
		}
		
		/* if the closest mainstreetnet route node is more far away
		 * than the configured distance to mainstreet net
		 * examine full net at destination 2 km earlier
		 */
		int destMainStreetNetDistanceMeters = mainStreetNetDistanceMeters;
		if (destMainStreetNetDistanceMeters < distToMainstreetNetRouteNode) {
			destMainStreetNetDistanceMeters = distToMainstreetNetRouteNode + 2000;
		}
		
		/*
		 * Implement A* route search
		 * (see e.g. http://en.wikipedia.org/wiki/A*_search_algorithm) 
		 * where GraphNode variables are as follows
		 * costs = g_score[start]
		 * distance = h_score[start] (heuristic estimate from estimate())
		 * total = f_score[start], costs + distance
		 *
		 * nodes is the openset
		 * closed is the closedset
		 */

		int mainStreetConsExamined = 0;
		motorwayConsExamined = 0;
		
		/* calculation statistics for debug purposes */
//		int maxNodesSize = 0;
//		int maxOpenSize = 0;
//		int maxClosedSize = 0;

		while (!(nodes.isEmpty())) {
			/* calculation statistics for debug purposes */
//			if (nodes.size() > maxNodesSize) maxNodesSize = nodes.size();
//			if (open.size() > maxOpenSize) maxOpenSize = open.size();
//			if (closed.size() > maxClosedSize) maxClosedSize = closed.size();
			
			currentNode = (GraphNode) nodes.firstElement();
			if (checkForTurnRestrictions) {
				if(closed.get(currentNode.state.connectionId) != null) { // to avoid having to remove
					nodes.removeElementAt(0);// improved nodes from nodes
					continue;
				}
			} else {
				if(closed.get(currentNode.state.toId) != null) { // to avoid having to remove
					nodes.removeElementAt(0);// improved nodes from nodes
					continue;
				}
			}

			if (!(currentNode.getTotal() == bestTotal)) {
				if (setBest(currentNode.getTotal(),currentNode.costs)) {
					break; // cancel route calculation 1/2
				}
			} 
			if (currentNode.state.toId == dest.id) {
				/* calculation statistics for debug purposes */
//				parent.alert("Max values", "Nodes:" + maxNodesSize + " Open:" + maxOpenSize + " Closed:" + maxClosedSize + " OutOfMem: " + oomCounter, 10000);

				return currentNode;
			}
			children.removeAllElements();

			expanded++;
			// Fetch all connections.
			Connection successor[];

//			// try to free up some Memory if necessary.
//			// TODO: this has to be reviewed.
//			if (runtime.freeMemory() < 25000) {
//				System.gc();
//				//#debug error
//				System.err.println("gc() only " +  runtime.freeMemory() + " exp=" + expanded +  " open " + open.size() + "  closed " + closed.size());
//			}
//			if (runtime.freeMemory() < 25000) {
//				tile.cleanup(2);
//				System.gc();
//				//#debug error
//				System.err.println("cleanup(2) + gc() " +  runtime.freeMemory() + " exp=" + expanded +  " open " + open.size() + "  closed " + closed.size());
//			}
//			if (runtime.freeMemory() < 25000) {
//				// load from resource only the Connections to this node
//				// avoid loading unused Connection because memory-expensive.
//				System.err.println("load only single " +  runtime.freeMemory() + " exp=" + expanded);
//				successor=currentNode.state.to.getConnections(tile);
//			} else {
//				// load all Connection to this Tile and return the connections to this node
//				// this avoid the usages of skip(int) because its CPU-expensive.
//				successor=tile.getConnections(currentNode.state.toId.shortValue(),tile);
//			}
//			//#debug error
//			System.out.println("Begin load connections MEM " +  runtime.freeMemory() + " exp=" + expanded +  " open " + open.size() + "  closed " + closed.size());
			
			Routing.onlyMainStreetNet =	currentNode.getFlag(GraphNode.GN_FLAG_CONNECTION_STARTS_INSIDE_MAINSTREETDISTANCE) && mainStreetConsExamined > 20;

			/*
			 *  when we are examining the mainstreet net, connections not on the mainstreet net
			 *  do not need to be examined if we've seen already enough mainstreet net connections
			 */
			if (Routing.onlyMainStreetNet && !currentNode.state.isMainStreetNet()) {
				successor = null;
			} else {
				currentTile = tile;
				try {
					tile.cleanup(50);
					successor=tile.getConnections(currentNode.state.toId,tile,bestTime);
				} catch (OutOfMemoryError e) {
					oomCounter++;
					tile.cleanup(0);
					System.gc();
					//#debug error
					logger.debug("after cleanUp : " + runtime.freeMemory());
	//				successor=currentNode.state.to.getConnections(tile);
					estimateFac += 0.02f;
					successor=tile.getConnections(currentNode.state.toId,tile,bestTime);
					//#debug error
					logger.debug("after load single Conection : " + runtime.freeMemory());
				}
			}
			if (successor == null){
				successor=new Connection[0];
			}
			
			// check for turn restrictions
			boolean turnRestricted []= new boolean[successor.length];
			if (checkForTurnRestrictions && currentTile.lastNodeHadTurnRestrictions) {
				int nextId;
				TurnRestriction turnRestriction = currentTile.getTurnRestrictions(currentNode.state.toId);
				while (turnRestriction != null) { // loop through all turn restrictions at this route node
					if ( (turnRestriction.affectedTravelModes & currentTravelMask) > 0 ){
						GraphNode parentNode;
						for (int cl=0;cl < successor.length;cl++){
							Connection nodeSuccessor=successor[cl];

							nextId = nodeSuccessor.toId;
							if (nextId == Integer.MAX_VALUE) {
								// if this is the final node use the alternative final node determined on start of the route calculation
								if (currentNode.state.toId == finalNodeId1) {
									nextId = finalNodeId2;
								} else {
									nextId = finalNodeId1;
								}
							}
							
							if (turnRestriction.toRouteNodeId == nextId) {
								//#debug debug
								logger.debug("to node matches");
								int numAdditionalViaNodes = 0; // default to no additional via nodes for via members of node type
								if (turnRestriction.isViaTypeWay()) {
									// set additional number of via Nodes on via members of way type
									numAdditionalViaNodes = turnRestriction.extraViaNodes.length;
									//#debug debug
									logger.debug(numAdditionalViaNodes + " additional nodes on via member to examine");
								}
								parentNode = currentNode;
								do {
									int prevId = -1;
									parentNode = parentNode.parent;
									if (parentNode != null) {
										prevId = parentNode.state.toId;
									} else {
										// if we have no parent node use the alternatve first node determined on start of the route calculation
										if (currentNode.state.toId == firstNodeId1) {
											prevId = firstNodeId2;
										} else {
											prevId = firstNodeId1;
										}
									}
									if (numAdditionalViaNodes == 0) { // when we already examined all via Nodes of a via Way or there were none
										// check if the route node id of the way before the via member matches
										if (turnRestriction.fromRouteNodeId == prevId) {
											if (! turnRestriction.isOnlyTypeRestriction()) {
												//#debug debug
												logger.debug("NO_ turn restriction match");
												turnRestricted[cl]=true;
											} else {
												//#debug debug
												logger.debug("ONLY_ turn restriction match");
												// disable all other connections
												for (int cl2=0;cl2 < successor.length;cl2++){
													if (cl2 != cl) {
														turnRestricted[cl2]=true;										
													}
												}
											}
										}
										parentNode = null; // the check is complete, so exit the loop
									} else {
										numAdditionalViaNodes--;
										if (turnRestriction.extraViaNodes[numAdditionalViaNodes] == prevId ) {
											//#debug debug
											logger.debug("additional via node match" + numAdditionalViaNodes);
										} else {
											parentNode = null; // one of the additional via Nodes did not match, so exit the loop
										}
									}
								} while (parentNode != null);
							}
						}
					}
					turnRestriction = turnRestriction.nextTurnRestrictionAtThisNode;
				}
			}	// end of check for turn restrictions

			/*  MainStreet Net Distance Check - this will turn on the MainStreetNet mode
			 *  for the successors of this GraphNode if we are far away enough from routeStart and routeDest.
			 */
			if (	bestTime
					&& MoreMath.dist(currentTile.lastRouteNode.lat, currentTile.lastRouteNode.lon, dest.lat, dest.lon) > destMainStreetNetDistanceMeters
					&& MoreMath.dist(currentTile.lastRouteNode.lat, currentTile.lastRouteNode.lon, routeFrom.lat, routeFrom.lon) > mainStreetNetDistanceMeters
			) {
				markSuccessorsToFullFillMainstreetNetDistance = true;
			} else {
				markSuccessorsToFullFillMainstreetNetDistance = false;
			}

			for (int cl=0;cl < successor.length;cl++){
				if ( successor[cl].isMainStreetNet() ) {
					// count how many mainStreetNet connections have already been examined
					mainStreetConsExamined++;
					if (successor[cl].isMotorwayConnection()) {
						motorwayConsExamined++;
					}
				} else if (Routing.onlyMainStreetNet) {
				// do not examine non-mainStreetNet connections
					turnRestricted[cl] = true;
				}
			} 
			
			
			for (int cl=0;cl < successor.length;cl++){
				if (turnRestricted[cl]) {
					continue;
				}
				Connection nodeSuccessor=successor[cl];
				// do not try a u-turn back to the route node we are coming from
				if ( (currentNode.parent != null && nodeSuccessor.toId == currentNode.parent.state.toId)) {
					continue;
				}

				// also do not try a u-turn back at the first connection
				if ( (nodeSuccessor.toId == firstNodeId1 || nodeSuccessor.toId == firstNodeId2) &&
					 (currentNode.state.connectionId == -1 || currentNode.state.connectionId == -2)  ) {
					//System.out.println("currentNode.state.toId: " + currentNode.state.toId + " " + firstNodeId1 +  " " + firstNodeId2 + " " + currentNode.state.connectionId);
					continue;
				}
						
				int turnCost;
				//System.out.println ("currentNode frombearing " + currentNode.fromBearing
				//		    + " nodeSuccessor.startBearing " + nodeSuccessor.startBearing);
				successorCost = currentNode.costs + nodeSuccessor.getCost();
				
				if (bestTime) {
					turnCost = getTurnCost(currentNode.fromBearing,nodeSuccessor.startBearing);
					successorCost += turnCost;
				}
				
				/* make motorways and toll roads very expensive if they are not allowed
				  but still make them usable as the route might start on them */
				if ( !useMotorways && nodeSuccessor.isMotorwayConnection()
						||
					 !useTollRoads && nodeSuccessor.isTollRoadConnection()
				) {
					successorCost += (100 * nodeSuccessor.getCost() + 100000);
				}
				//System.out.println("successor " + nodeSuccessor + " cost " + successorCost + " turnCost : " + turnCost + " node " + currentNode + " current node costs " + currentNode.costs + " nodesucc cost " + nodeSuccessor.getCost());
				// when the next connection starts at traffic signals and the current one isn't also starting at a traffic signal but we are not on a motorway connection
				if (bestTime && nodeSuccessor.startsAtTrafficSignals() && !currentNode.state.startsAtTrafficSignals() && !nodeSuccessor.isMotorwayConnection() ) {
					//System.out.println("TRAFFIC SIGNAL");
					// add configured secs traffic signal calc delay
					successorCost += trafficSignalCalcDelayTSecs;
				}
				GraphNode openNode = null;
				GraphNode theNode = null;
				GraphNode closedNode;
				if (checkForTurnRestrictions) {
					closedNode =  (GraphNode) closed.get(nodeSuccessor.connectionId);
					if (closedNode == null) {
						openNode = (GraphNode) open.get(nodeSuccessor.connectionId);
					}
				} else {
					closedNode =  (GraphNode) closed.get(nodeSuccessor.toId);
					if (closedNode == null) {
						openNode = (GraphNode) open.get(nodeSuccessor.toId);
					}
				}
				theNode = (openNode != null) ? openNode : closedNode;
				// in open or closed				
				if (theNode != null) {
					//System.out.println("successor cost " + successorCost + ", theNode.costs " + theNode.costs);
					//System.out.println("theNode.distance " + theNode.distance);
					if (successorCost < theNode.costs) {
						if (closedNode != null) {
							if (checkForTurnRestrictions) {
								open.put(nodeSuccessor.connectionId, theNode);
								closed.remove(nodeSuccessor.connectionId);
							} else {
								open.put(nodeSuccessor.toId, theNode);
								closed.remove(nodeSuccessor.toId);
							}
						} else {
							int dist = theNode.distance;
							theNode = new GraphNode(nodeSuccessor, currentNode, successorCost, dist, currentNode.fromBearing);
							if (checkForTurnRestrictions) {
								open.put(nodeSuccessor.connectionId, theNode); 
							} else {
								open.put(nodeSuccessor.toId, theNode);
							}
						} 
						theNode.costs = successorCost;
						//theNode.total = theNode.costs + theNode.distance;
						theNode.parent = currentNode; 
						theNode.fromBearing=currentNode.state.endBearing;
						children.addElement(theNode);
					}
				// not in open or closed
				} else { 
					int estimation;
					GraphNode newNode;
					estimation = estimate(currentNode.state,nodeSuccessor, dest);
					newNode = new GraphNode(nodeSuccessor, currentNode, successorCost, estimation, currentNode.fromBearing);
					// mark new GraphNode to examine only mainstreetNet successors
					if (markSuccessorsToFullFillMainstreetNetDistance) {
						newNode.setFlag(GraphNode.GN_FLAG_CONNECTION_STARTS_INSIDE_MAINSTREETDISTANCE);
					}
					if (showConnectionTraces) {
						RouteNode rn1 = getRouteNode(currentNode.state.toId);
						RouteNode rn2 = getRouteNode(nodeSuccessor.toId);
						if (rn1 != null && rn2 != null) {
							RouteConnectionTraces.addRouteConnectionTrace(rn1.lat, rn1.lon, rn2.lat, rn2.lon, Routing.onlyMainStreetNet ? ConnectionTrace.WIDELINE : 0, "");						
						}
					}
					if (checkForTurnRestrictions) {
						open.put(nodeSuccessor.connectionId, newNode);
					} else {
						open.put(nodeSuccessor.toId, newNode);
					}
					if (showRouteHelpers) {
						RouteNode rn = getRouteNode(newNode.state.toId);
						if (rn != null) {
							RouteHelpers.addRouteHelper(rn.lat, rn.lon, "t"+expanded);
						}
					}
//					evaluated++;
					children.addElement(newNode);
				}
			}
			if (checkForTurnRestrictions) {
				open.remove(currentNode.state.connectionId);
				closed.put(currentNode.state.connectionId, currentNode);
			} else {
				open.remove(currentNode.state.toId);
				closed.put(currentNode.state.toId, currentNode);
			}
			nodes.removeElementAt(0);
			addToNodes(children); // update nodes
		}
		if (!Routing.stopRouting) {
			parent.receiveMessage(Locale.get("routing.NoSolutionFound")/*no Solution found*/);
		}
		return null;

	}
	
	/**
	 * Update title bar during routing
	 * @param total
	 * @param actual
	 * @return true if routing is canceled
	 */
	private boolean setBest(int total,int actual) {
		bestTotal=total;
		long now=System.currentTimeMillis();
		if (now > nextUpdate){
		if (Routing.stopRouting) {
			return true;
		}
		if (bestTime){
			parent.receiveMessage("" + (bestTotal/600) 
					      + Locale.get("routing.min")/*min*/ + " " + (100*actual/total)
					      + Locale.get("routing.memperc:")/*% m:*/ + runtime.freeMemory()/1000 
					      + Locale.get("routing.ks")/*k s:*/ + oomCounter+"/"+expanded+"/"+open.size());
		} else {
			parent.receiveMessage("" + (bestTotal/1000f) 
					      + Locale.get("routing.km")/*km*/ + " " + (100*actual/total)
					      + Locale.get("routing.memperc:")/*% m:*/ + runtime.freeMemory()/1000 
					      + Locale.get("routing.ks")/*k s:*/ + oomCounter+"/"+expanded);
		}
		nextUpdate=now + 1000;
		}
		return false;
	}

	private void addToNodes(Vector children) {
		for (int i = 0; i < children.size(); i++) { 
			GraphNode newNode = (GraphNode) children.elementAt(i);
			long newTotal = newNode.getTotal();
			long newCosts = newNode.costs;
			int idx = bsearch(0, nodes.size()-1, newTotal, newCosts);
			nodes.insertElementAt(newNode, idx); 
		}
	}

	private int bsearch(int l, int h, long tot, long costs){
		int lo = l;
		int hi = h;
		while(lo<=hi) {
			int cur = (lo+hi)/2;
			long ot = ((GraphNode)nodes.elementAt(cur)).getTotal();
			// in a tie, the one is better which has a better real cost
			if((tot < ot) || (tot == ot && costs < ((GraphNode) nodes.elementAt(cur)).costs)) 
				hi = cur - 1;
			else lo = cur + 1;
		} 
		return lo; //insert before lo 
	} 


	private int getTurnAngle(int endBearing, int startBearing) {
		if (endBearing == 99 || startBearing == 99) {
			return 0;
		}
		int angle = Math.abs((endBearing - startBearing)*2);
		if (angle > 180) {
			angle = 360 - angle;
		}
		//System.out.println ("GetTurn:  " + angle + " endBearing: " + endBearing +
		//	" startBearing " + startBearing);
		return angle;
	}

	private int getTurnCost(int endBearing, int startBearing) {
		int adTurn=getTurnAngle(endBearing, startBearing);
		if (adTurn > 150){
			return 20;
		} else if (adTurn > 120){
			return 15;
		} else if (adTurn > 60){
			return 10;
		} else if (adTurn > 30){
			return 5;
		} else {
			return 0;			
		}
	}

	/** TODO: Explain
	 * @param nodeSuccessor
	 * @param dest
	 * @return
	 */
	private int estimate(Connection from,Connection to, RouteNode dest) {
//		if (noHeuristic){
//			return 0;
//		}
		boolean angles = !Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_ESTIMATION_NO_ANGLES);

		//int dTurn=from.endBearing-to.startBearing;
		int turnCost=angles ? getTurnCost(from.endBearing, to.startBearing) : 0;
		RouteNode toNode=getRouteNode(to.toId);
		if (toNode == null) {
			//#debug info
			logger.info("RouteNode (" + to.toId + ") = null" );
			return (10000000);
		}
		if (dest == null) {
			throw new Error(Locale.get("routing.DestinationIsNULL")/*Destination is NULL*/);
		}
		int dist = MoreMath.dist(toNode.lat, toNode.lon, dest.lat, dest.lon);
		
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_ESTIMATION_120KMH)) {
			// estimate 120 km/h as average speed
			return (int) (dist/3.33f * (Configuration.getRouteEstimationFac() / 10f + 1));
		}

		/* sharp turns > 100 degrees between motorways and especially motorway links
		 * can't be accepted without checking for a better alternative
		 * to avoid entering and then immediately leaving motorway links
		 * like at http://www.openstreetmap.org/?mlat=48.061419&mlon=11.639106&zoom=18&layers=B000FTF
		 * or vice versa like at http://www.openstreetmap.org/browse/node/673142
		 */
		if (angles && from.isMotorwayConnection() && to.isMotorwayConnection() && getTurnAngle(from.endBearing, to.startBearing) > 100 ) {
			return (int) (dist * 2);
		}
		
		
		if (bestTime) {
			if (roadRun) {
				return (dist+turnCost)* 3 / 2;
			}
			// if max estimated speed is smaller than 50 Km/h (14 m/s) use this for estimation 
			if (maxEstimationSpeed < 14) {
				   return (int) (((dist/ maxEstimationSpeed * 10)+turnCost)*estimateFac);				
			}

			// if we are on the motorway search it very far
			if (boostMotorways && to.isMotorwayConnection()) {
				if (!from.isMotorwayConnection()) {
					motorwayEntrancesExamined++;					
					System.out.println("Motorway entrance");
				}
				if (from.isMotorwayConnection()) {
					if (motorwayEntrancesExamined < 2) {
						/* Estimate 80 Km/h (22 m/s) as average speed if we are on the 
						 * motorway and only 1 motorway entrance has been examined yet
						 * This gives the 2nd entrance in the opposite direction also 
						 * a chance to get examined
						 */
						return (int) (((dist/2.2f)+turnCost)*estimateFac);
					}
				}
				// Estimate 100 Km/h (28 m/s) as average speed to enter the motorway or 
				// on the motorway if at least 2 entrances where examined 
				return (int) (((dist/2.8f))*estimateFac);
			}

			
			// If the air distance is more than 20km try to find a motorway that is at 
			// maximum 20 km or half of the distance to route start away from the route start 
			if (tryFindMotorway && motorwayConsExamined < 10 && dist > 20000 && MoreMath.dist(toNode.lat,toNode.lon,routeFrom.lat,routeFrom.lon) < Math.max(dist / 2, 20000)){
				// estimate 80 Km/h (22 m/s) as average speed 
				return (int) (((dist/2.2f)+turnCost)*estimateFac);				
			}
			
			
			if (Routing.onlyMainStreetNet) {
				// if we are on a trunk or primary search it far
				if (boostTrunksAndPrimarys && to.isTrunkOrPrimaryConnection()) {
				   // estimate 65 Km/h (18 m/s) as average speed 
					return (int) (((dist/1.8f)+turnCost)*estimateFac);
				}
//				if (dist > 100000){
//					// estimate 100 Km/h (28 m/s) as average speed 
//					return (int) (((dist/2.8f)+turnCost)*estimateFac);
//				}
//				if (dist > 50000){
//				   // estimate 80 Km/h (22 m/s) as average speed 
//					return (int) (((dist/2.2f)+turnCost)*estimateFac);
//				}

				if (dist > 10000){
					// estimate 60 Km/h (17 m/s) as average speed 
					return (int) (((dist/1.7f)+turnCost)*estimateFac);
				}
			}

			// estimate 50 Km/h (14 m/s) as average speed 
			return (int) (((dist/1.7f)+turnCost)*estimateFac);
		} else {
			//return (int) ((dist*1.1f + turnCost)*estimateFac);
			// we're aiming for shortest distance; return just the distance,
			// unadjusted if user has set factor to 0, adjusted by estimation
			// factor otherwise
			return (int) (dist * (Configuration.getRouteEstimationFac() / 10f + 1));
		}
	}

	/**
	 * @param to
	 * @return
	 */
	private RouteNode getRouteNode(int id) {
		if (id == Integer.MAX_VALUE){
			return routeTo;
		}
		return currentTile.getRouteNode(id);
	} 


	public void solve(RoutePositionMark fromMark,RoutePositionMark toMark) {
		this.fromMark = fromMark;
		this.toMark = toMark;
		
		
//#if polish.api.finland
		if (Locale.get("travelmodes.ReittiopasCycle").equals(Configuration.getTravelMode().getName())) {
			logger.info("Calculating online route from " + fromMark + " to " + toMark);

			String url = GuiWebInfo.getReittiopasUrl() +
				"&request=cycling"
				+ "&from=" 
				+ (fromMark.lon * MoreMath.FAC_RADTODEC) + ","
				+ (fromMark.lat * MoreMath.FAC_RADTODEC)
				+ "&to=" 
				+ (toMark.lon * MoreMath.FAC_RADTODEC) + ","
				+ (toMark.lat * MoreMath.FAC_RADTODEC)
				+ "&format=txt";
			GuiWebInfo.openUrl(url);
		} else if (Locale.get("travelmodes.ReittiopasPublic").equals(Configuration.getTravelMode().getName())) {
			logger.info("Calculating online route from " + fromMark + " to " + toMark);

			String url = GuiWebInfo.getReittiopasUrl() +
				"&request=route"
				+ "&from=" 
				+ (fromMark.lon * MoreMath.FAC_RADTODEC) + ","
				+ (fromMark.lat * MoreMath.FAC_RADTODEC)
				+ "&to=" 
				+ (toMark.lon * MoreMath.FAC_RADTODEC) + ","
				+ (toMark.lat * MoreMath.FAC_RADTODEC)
				+ "&format=txt";
			// FIXME instead of showing the route in a browser
			// as text, instead take it into memory
			// 
			GuiWebInfo.openUrl(url);
		}
		else {
//#endif
			logger.info("Calculating route from " + fromMark + " to " + toMark);
			processorThread = new Thread(this);
			processorThread.setPriority(Thread.NORM_PRIORITY);
			processorThread.start();		
//#if polish.api.finland
		}
//#endif
	}
		

	private int getNearestSeg(Way w,float lat, float lon,float[] lats,float[] lons){
			float minDistSq=Float.MAX_VALUE;
			int startAt=0;
			int max=lats.length -1;
			for (int u=0;u<max;u++){
				  float distSq = MoreMath.ptSegDistSq(
						  lats[u],
						  lons[u],
						  lats[u+1],
						  lons[u+1],
						  lat,
						  lon);
//				  logger.debug("dist:" + distSq + "  minDist:" + minDistSq);
				  if (distSq < minDistSq){
//					  logger.debug("index " + (u+1) + " is actual min");
					  minDistSq=distSq;
					  startAt=u+1;
				  }
			}
		return startAt;
	}
	
	private RouteNode findNextRouteNode(int begin,float lat, float lon,float[] lats,float[] lons){
		RouteNode rn=null;
		for (int v=begin;v < lats.length; v++){
			//#debug debug
			logger.debug("search point "+ lats[v] +"," + lons[v]);
			rn=tile.getRouteNode(rn, RouteBaseTile.EPSILON_SEARCH_EXACT_MATCH, lats[v], lons[v]);
			if (rn !=null){return rn;}
		} 
		return null;
	}
	private RouteNode findPrevRouteNode(int end,float lat, float lon,float[] lats,float[] lons){
		RouteNode rn=null;
		for (int v=end;v >= 0; v--){
			//#debug debug
			logger.debug("search point "+ lats[v] +"," + lons[v]);
			rn=tile.getRouteNode(rn, RouteBaseTile.EPSILON_SEARCH_EXACT_MATCH, lats[v], lons[v]);
			if (rn !=null){return rn;}
		} 
		return null;
	}
	

	/**
	 * @return some value that is the bigger the more course direction and segment direction differ
	 */
	public static float getDirectionPenalty(int segIdx, int segmentDirection, float[] lats, float[] lons, int course) {
		return getDirectionPenalty(lats[segIdx], lons[segIdx], lats[segIdx+segmentDirection], lons[segIdx+segmentDirection], course);
	}
	

	/**
	 * @return some value that is the bigger the more course direction and segment direction differ
	 */
	// FIXME: this can be surely determined in a more efficient way
	public static float getDirectionPenalty(float lat1, float lon1, float lat2, float lon2, int course) {
		float latCourse = lat1 + (float) (0.0001 * Math.cos(course * MoreMath.FAC_DECTORAD));
		float lonCourse = lon1 + (float) (0.0001 * Math.sin(course * MoreMath.FAC_DECTORAD));

		float courseVecX = lon1 - lonCourse;
		float courseVecY = lat1 - latCourse;
		float norm = (float) Math.sqrt(courseVecX * courseVecX + courseVecY * courseVecY);
		courseVecX /= norm;
		courseVecY /= norm;
	
		float segDirVecX = lon1 - lon2;
		float segDirVecY = lat1 - lat2;
		norm = (float) Math.sqrt((double)(segDirVecX * segDirVecX + segDirVecY * segDirVecY));
		return (1.0f - (segDirVecX * courseVecX + segDirVecY * courseVecY) / norm);
	}
	
		/**
	 * prepares for solving a requested route, e.g. by determining the closest route nodes at the start and destination ways
	 * @return true, if preparing was successful
	 */
	private boolean prepareSolving() {
		try {
			if (toMark == null) {
				parent.receiveMessage(Locale.get("routing.PleaseSetDestinationFirst")/*Please set destination first*/);
				return false;
			} 
			
			RouteNode startNode=new RouteNode();
			startNode.lat=fromMark.lat;
			startNode.lon=fromMark.lon;

			/*
			 * always search for the best start way in current travel mode
			 */
			parent.receiveMessage(Locale.get("routing.SearchingStartWay")/*Searching start way*/);
			parent.searchNextRoutableWay(fromMark);
			if (fromMark.entity == null){
				parent.receiveMessage(Locale.get("routing.NoWayFoundForStartPoint")/*No way found for start point*/);
			}

			/*
			 * search for the best destination way in current travel mode if travel mode did not change in the meanwhile
			 * (toMark.entity will have been cleared by dropToConnectionsCache() in this case)
			 */
			if (toMark.entity == null) {
				parent.receiveMessage(Locale.get("routing.SearchingDestinationWay")/*Searching destination way*/);
				parent.searchNextRoutableWay(toMark);
				if (toMark.entity == null) {
					parent.receiveMessage(Locale.get("routing.NoWayAtDestination")/*No way at destination*/);
					return false;
				}
			}
			
			logger.info("Calculating route from " + fromMark + " to " + toMark);
			
			if (fromMark.entity instanceof Way) {
				// search the next route node in all accessible directions. Then create 
				// connections that lead form the start point to the next route nodes.
				parent.receiveMessage(Locale.get("routing.CreatingFromConnections")/*Creating from connections*/);
				Way w=(Way) fromMark.entity;
				int nearestSegment=getNearestSeg(w, startNode.lat, startNode.lon, fromMark.nodeLat,fromMark.nodeLon);
				
				int penOsmDirection = 0;
				int penAgainstOsmDirection = 0;
				determinedAgainstDirectionPenalty = false;
				// FIXME add support for routemodes speed option, see https://sourceforge.net/tracker/index.php?func=detail&aid=3420838&group_id=192084&atid=939977
				//if (parent.manualRotationMode || parent.speed > 20) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION) &&
				    Configuration.getTravelMode().isWithTurnRestrictions()
				    && parent.isCourseValid()) {
					determinedAgainstDirectionPenalty = true;
					int course = parent.getCourse();
	//				System.out.println ("Nearest: " + nearestSegment + "/" + fromMark.nodeLat.length);
					float penaltyOSMWayDirection = getDirectionPenalty(nearestSegment - 1, 1, fromMark.nodeLat,fromMark.nodeLon, course );
					float penaltyAgainstOSMWayDirection = getDirectionPenalty(nearestSegment, -1, fromMark.nodeLat,fromMark.nodeLon,course);
	//				System.out.println ("Trace Course: " + course);
	//				System.out.println ("Penalty OSM Direction: " + penaltyOSMWayDirection);
	//				System.out.println ("Penalty against OSM Direction: " + penaltyAgainstOSMWayDirection);
					if (penaltyOSMWayDirection > penaltyAgainstOSMWayDirection) {
	//					System.out.println("Avoid driving OSM Way Direction");
						penOsmDirection = 1000;
						avoidStartingInOsmWayDirection = false;
					} else {
	//					System.out.println("Avoid driving against OSM Way Direction");
						penAgainstOsmDirection = 1000;
						avoidStartingInOsmWayDirection = true;
					}
				}
				routeStartsAgainstMovingDirection = false;
				
				// Roundabouts don't need to be explicitly tagged as oneways in OSM 
				// according to http://wiki.openstreetmap.org/wiki/Tag:junction%3Droundabout
				
				if (showRouteHelpers) {
					RouteHelpers.addRouteHelper(fromMark.nodeLat[nearestSegment],fromMark.nodeLon[nearestSegment],"oneWay sec");
				}
				RouteNode rn=findPrevRouteNode(nearestSegment-1, startNode.lat, startNode.lon, fromMark.nodeLat,fromMark.nodeLon);
				if (rn != null) {
					routeFrom = rn;
					firstNodeId1 = rn.id; // must be before the oneDirection check as this routeNode might be the source node for connection/duration determination
					if (! w.isOneDirectionOnly() ) { // if no against oneway rule applies
						if (showRouteHelpers) {
							RouteHelpers.addRouteHelper(rn.lat,rn.lon,"next back");
						}
						// TODO: fill in bearings and cost
						Connection initialState=new Connection(rn,0,(byte)99,(byte)99, -1);
						GraphNode firstNode=new GraphNode(initialState,null,penAgainstOsmDirection,0,(byte)99);
						open.put(initialState.toId, firstNode);
						nodes.addElement(firstNode);
						/*
						 *  remember coordinates of this alternative for dummy route node on the path
						 *  this will allow us to find the path segment
						 *  including the closest source position to highlight
						 *  when searching the ways for matching connections
						 */
						int firstSeg = nearestSegment;
						if (firstSeg >= fromMark.nodeLat.length) {
							firstSeg = fromMark.nodeLat.length - 1;	
						}
						firstSourcePathSegNodeDummy1.radlat = fromMark.nodeLat[firstSeg];
						firstSourcePathSegNodeDummy1.radlon = fromMark.nodeLon[firstSeg];
					}
				} 
				rn=findNextRouteNode(nearestSegment, startNode.lat, startNode.lon, fromMark.nodeLat,fromMark.nodeLon);
				if (rn != null) {
					routeFrom = rn;
					firstNodeId2 = rn.id;
					// TODO: fill in bearings and cost
					Connection initialState=new Connection(rn,0,(byte)99,(byte)99, -2);
					GraphNode firstNode=new GraphNode(initialState,null,penOsmDirection,0,(byte)99);
					open.put(initialState.toId, firstNode);
					nodes.addElement(firstNode);						
					/*
					 *  remember coordinates of this alternative for dummy route node on the path
					 *  this will allow us to find the path segment
					 *  including the closest source position to highlight
					 *  when searching the ways for matching connections
					 */
					int firstSeg = nearestSegment - 1;
					if (firstSeg < 0) {
						firstSeg = 0;
					}
					firstSourcePathSegNodeDummy2.radlat = fromMark.nodeLat[firstSeg];
					firstSourcePathSegNodeDummy2.radlon = fromMark.nodeLon[firstSeg];
				}
			}


			// same for the endpoint
			
			routeTo=new RouteNode();
			routeTo.id=Integer.MAX_VALUE;
			routeTo.setConSizeWithFlags((byte) 0);
			routeTo.lat=toMark.lat;
			routeTo.lon=toMark.lon;
			parent.receiveMessage(Locale.get("routing.CreatingToConnections")/*Creating to connections*/);
			Way w = (Way) toMark.entity;
			int nearestSeg = getNearestSeg(w,toMark.lat, toMark.lon, toMark.nodeLat, toMark.nodeLon);
			// remember closest point on dest way in RouteInstructions
			RouteInstructions.setClosestPointOnDestWay(
					MoreMath.closestPointOnLine(new Node(toMark.nodeLat[nearestSeg - 1], toMark.nodeLon[nearestSeg - 1], true), 
												new Node(toMark.nodeLat[nearestSeg], toMark.nodeLon[nearestSeg], true),
												new Node(toMark.lat, toMark.lon, true)
					)
			);
			// System.out.println("Closest " + RouteInstructions.getClosestPointOnDestWay() );
			// RouteHelpers.addRouteHelper(RouteInstructions.getClosestPointOnDestWay().radlat, RouteInstructions.getClosestPointOnDestWay().radlon, "closest");
			RouteTileRet nodeTile=new RouteTileRet();
			if (destRn == null) {
				destRn = findNextRouteNode(nearestSeg, toMark.lat, toMark.lon, toMark.nodeLat, toMark.nodeLon);
			}
			if (destRn != null) {
				// RouteHelpers.addRouteHelper(nextNode.lat, nextNode.lon, "nextNode dest");
				finalNodeId2 = destRn.id; // must be before the oneDirection check as this routeNode might be the destination node for connection/duration determination
				// roundabouts don't need to be explicitly tagged as oneways in OSM according to http://wiki.openstreetmap.org/wiki/Tag:junction%3Droundabout
				if (! w.isOneDirectionOnly() ){ // if no against oneway rule applies
					// TODO: fill in bearings and cost
					Connection newCon=new Connection(routeTo,0,(byte)99,(byte)99, -3);
					tile.getRouteNode(destRn.id, nodeTile);
					nodeTile.tile.addConnection(destRn,newCon,bestTime);
					/*
					 *  remember coordinates of this alternative for dummy route node on the path
					 *  this will allow to find the path segment
					 *  including the closest dest position to highlight / on route
					 *  when searching the ways for matching connections
					 */
					int finalSeg = nearestSeg - 1;
					if (finalSeg < 0) {
						finalSeg = 0;
					}
					finalDestPathSegNodeDummy2.radlat = toMark.nodeLat[finalSeg];
					finalDestPathSegNodeDummy2.radlon = toMark.nodeLon[finalSeg];
				}
			}
			if (destPrevRn == null) {
				destPrevRn = findPrevRouteNode(nearestSeg - 1, toMark.lat, toMark.lon, toMark.nodeLat, toMark.nodeLon);
			}
			if (destPrevRn == null) {
				parent.receiveMessage(Locale.get("routing.NoPrevRouteNodeAtDestination")/*No prev route node at destination*/);
				return false;
			}
			// RouteHelpers.addRouteHelper(prefNode.lat, prefNode.lon, "prevNode dest");
			// TODO: fill in bearings and cost
			Connection newCon=new Connection(routeTo,0,(byte)99,(byte)99, -4);
			tile.getRouteNode(destPrevRn.id, nodeTile);
			nodeTile.tile.addConnection(destPrevRn,newCon,bestTime);
			/*
			 *  remember coordinates of this alternative for dummy route node on the path
			 *  this will allow to find the path segment
			 *  including the closest dest position to highlight / on route
			 *  when searching the ways for matching connections
			 */
			int finalSeg = nearestSeg;
			if (finalSeg >= toMark.nodeLat.length) {
				finalSeg = toMark.nodeLat.length - 1;	
			}
			finalNodeId1 = destPrevRn.id;
			finalDestPathSegNodeDummy1.radlat = toMark.nodeLat[finalSeg];
			finalDestPathSegNodeDummy1.radlon = toMark.nodeLon[finalSeg];
			
			if (routeTo != null) {
				if (Configuration.getTravelMode().useMainStreetNetForLargeRoutes() && distToMainstreetNetRouteNode == 0) {
					/* search closest MainstreetRouteNode at destination */
					Routing.onlyMainStreetNet = true;
					// calc lat/lon per km
					RouteNode best = new RouteNode();
					best.lat = routeTo.lat + 0.001f;
					best.lon = routeTo.lon;
					float distLat = (int) ProjMath.getDistance(routeTo.lat, routeTo.lon, best.lat, best.lon);		
					best.lat = routeTo.lat;
					best.lon = routeTo.lon + 0.001f;
					float distLon = (int) ProjMath.getDistance(routeTo.lat, routeTo.lon, best.lat, best.lon);		
					float latLonPerKm = 1.0f / Math.max(distLat, distLon); // same as 0.001f * 1000 / Math.Max(distLat, distLon)
					float bestRouteNodeSearchEpsilon = latLonPerKm;
					best = null;
					// search closest mainstreet net route node within about 20 km around destination 
					do {
						best = tile.getRouteNode(best, bestRouteNodeSearchEpsilon, routeTo.lat, routeTo.lon);
						bestRouteNodeSearchEpsilon += latLonPerKm;
					} while (best == null && bestRouteNodeSearchEpsilon < latLonPerKm * 20);
					
					if (best != null) {
						distToMainstreetNetRouteNode = (int) ProjMath.getDistance(routeTo.lat, routeTo.lon, best.lat, best.lon);
					} else if (Configuration.getMainStreetDistanceKm() <= 20){
						parent.alert("Routing", "No MainstreetNet RouteNode found within 20 km at destination", 5000);
					}
					// RouteHelpers.addRouteHelper(best.lat, best.lon, "Closest MainstreetRouteNode at destination");
				}
				Routing.onlyMainStreetNet = false;
				
				parent.cleanup();
				System.gc();
				//#debug error
				logger.info("free mem: "+runtime.freeMemory());
				return true;
			}

		} catch (Exception e) {
			//parent.receiveMessage("Routing exception " + e.getMessage());
			// show that there was exception as an alert so we can see in the title bar where the exception occured
			parent.alert(Locale.get("routing.RoutingException")/*Routing Exception*/, "" + e.getMessage(), 5000);
			e.printStackTrace();
		}
		return false;
	}
	
	
	private final Vector solve () {
		RouteHelpers.clear();
		RouteConnectionTraces.clear();
				
		// when we search the closest routeNode, we must be able to access all routeNodes, not only the mainStreetNet one's
		Routing.onlyMainStreetNet = false;

		// when we search for the closest routeNode, we need to reload the route tiles because the travel mode might have changed 
		tile.cleanup(-1);
		
/*
 * TODO: if we would cleanup the route tiles only when the travel mode changed, this would result in very fast route recalculations
 * However it would require better memory management, maybe to clean up the route tiles only as much as required
		// when we search for the closest routeNode, reload the route tiles only if the travel mode changed 
		if (tile.travelModeNr != Configuration.getTravelModeNr()) { 
			tile.cleanup(-1);
			tile.travelModeNr = Configuration.getTravelModeNr();
		}
*/
		
		// create from and to connections
		if (! prepareSolving()) {
			return null;
		}		
		
		try {
			GraphNode solution=search(routeTo);
			nodes.removeAllElements();
			open.removeAll();
			closed.removeAll();
			// cleanup the route tiles after searching the route
			tile.cleanup(-1);
			if (solution == null) {
				return null; // cancel route calculation 2/2
			}
			int seconds = (int) (System.currentTimeMillis() - searchStartTime) / 1000;
			logger.info("Route calculation took " + seconds + " seconds");
			System.out.println("Route calculation took " + seconds + " seconds");
			if (bestTime){
				if (Configuration.getDebugSeverityDebug()) {
					parent.receiveMessage(Locale.get("routing.RouteFoundIn")/*Route found in*/ + seconds + " " + Locale.get("routing.sec") + ": " + (bestTotal/600) + Locale.get("routing.min"));
				} else {
					parent.receiveMessage(Locale.get("routing.RouteFoundIn")/*Route found in*/ + " " + seconds + " " + Locale.get("routing.sec"));
				}
			} else {
				parent.receiveMessage(Locale.get("routing.RouteFoundIn")/*Route found in*/ + " " + seconds + " " + Locale.get("routing.sec") + ": " + (bestTotal/1000f) + Locale.get("routing.km")/*km*/);
			}
			// when finally we get the sequence we must be able to access all route nodes, not only the mainstreet net's
			Routing.onlyMainStreetNet = false;
			
			Vector solutionVector = new Vector();
			while (solution != null) {
			    solutionVector.addElement(solution);
			    solution = solution.parent;
			}
			GraphNode solutionNode;
			Vector sequence = new Vector();
			for (int i = solutionVector.size() - 1; i >= 0; i--) {
			    solutionNode = (GraphNode) solutionVector.elementAt(i);
			    ConnectionWithNode c = new ConnectionWithNode(getRouteNode(solutionNode.state.toId), solutionNode.state);
			    sequence.addElement(c);
			} 

			// move connection durations so that they represent the time to travel to the next node, rather than to the current node
			ConnectionWithNode c;
			ConnectionWithNode cPrev = (ConnectionWithNode) sequence.elementAt(0);
			for (int i = 0; i < sequence.size(); i++) {
				c = (ConnectionWithNode) sequence.elementAt(i);
				cPrev.durationFSecsToNext = c.durationFSecsToNext;
				cPrev = c;
				if (i == sequence.size() - 1) {
					c.durationFSecsToNext = Short.MAX_VALUE;
				}
			}

			/*
			 * fill in the coordinates of the path segment closest to the source
			 * depending on the chosen alternative first route node in the route calculation
			 * into a dummy connection on the path.
			 * This will allow us to find the path segment
			 * including the closest source position to highlight
			 * when searching the ways for matching connections
			 */
			ConnectionWithNode cFirst = (ConnectionWithNode) sequence.firstElement();

			if (firstNodeId1 == cFirst.to.id) {
				sourcePathSegNodeDummyConnectionNode.to.lat=firstSourcePathSegNodeDummy1.radlat;
				sourcePathSegNodeDummyConnectionNode.to.lon=firstSourcePathSegNodeDummy1.radlon;
				sourcePathSegNodeDummyConnectionNode.to.id = firstNodeId2;  //remember id to be able to determine the connection with its duration
				if (avoidStartingInOsmWayDirection && determinedAgainstDirectionPenalty) {
					routeStartsAgainstMovingDirection = true;
				}
			}
			if (firstNodeId2 == cFirst.to.id) {
				sourcePathSegNodeDummyConnectionNode.to.lat=firstSourcePathSegNodeDummy2.radlat;
				sourcePathSegNodeDummyConnectionNode.to.lon=firstSourcePathSegNodeDummy2.radlon;
				sourcePathSegNodeDummyConnectionNode.to.id = firstNodeId1;  //remember id to be able to determine the connection with its duration
				if (!avoidStartingInOsmWayDirection && determinedAgainstDirectionPenalty) {
					routeStartsAgainstMovingDirection = true;
				}
			}

			sequence.insertElementAt(sourcePathSegNodeDummyConnectionNode, 0);
			sourcePathSegNodeDummyConnectionNode.durationFSecsToNext = getConnectionDurationFSecsForRouteNodes(sourcePathSegNodeDummyConnectionNode.to.id, cFirst.to.id);
			
			/**
			 * same for the final connection (which is initially at the destination)
			 */
			ConnectionWithNode cBeforeFinal = (ConnectionWithNode) sequence.elementAt(sequence.size()-2);
			ConnectionWithNode cFinal = (ConnectionWithNode) sequence.lastElement();
			if (finalNodeId1 == cBeforeFinal.to.id) {
				cFinal.to.lat = finalDestPathSegNodeDummy1.radlat;
				cFinal.to.lon = finalDestPathSegNodeDummy1.radlon;
				cFinal.to.id= finalNodeId2; //remember id to be able to determine the connection with its duration
			}
			if (finalNodeId2 == cBeforeFinal.to.id) {
				cFinal.to.lat = finalDestPathSegNodeDummy2.radlat;
				cFinal.to.lon = finalDestPathSegNodeDummy2.radlon;
				cFinal.to.id = finalNodeId1; //remember id to be able to determine the connection with its duration
			}
			cBeforeFinal.durationFSecsToNext = getConnectionDurationFSecsForRouteNodes(cBeforeFinal.to.id, cFinal.to.id);
	
//			logger.info("Ready with route discovery");
			return sequence;
		} catch (Exception e) {
			// cleanup the route tiles also when an exception occurred
			tile.cleanup(-1);
			parent.receiveMessage(Locale.get("routing.RoutingEx")/*Routing Ex*/ + " " + e.getMessage());
			//#debug error
			e.printStackTrace();
			return null;
		}
	}

	private short getConnectionDurationFSecsForRouteNodes(int rnFromId, int rnToId) {
		Connection successor[] = tile.getConnections(rnFromId,tile,bestTime);
		if (successor != null) {
			for (int cl=0;cl < successor.length;cl++){
				Connection nodeSuccessor=successor[cl];
				if (nodeSuccessor.toId == rnToId) {
					//System.out.println("CONNECTION FOUND: " + nodeSuccessor.durationFSecs / 5 );
					return nodeSuccessor.durationFSecs;
				}
			}
		}
		return 0;
	}
	
	public void run() {
		RouteInstructions.abortRouteLineProduction();
		Routing.stopRouting = false;
		
		/* Wait for the route tile to be initialized by the DictReader thread
		 * (This is necessary if trying to calculate a route very soon after midlet startup) 
		*/
		while (!parent.baseTilesRead) {
			parent.receiveMessage("Waiting for base tiles");
			try {
				Thread.sleep(250);
			} catch (InterruptedException e1) {
				// nothing to do in that case						
			}
		}
		this.tile = (RouteBaseTile) parent.getDict((byte) DictReader.ROUTEZOOMLEVEL);
		if (this.tile == null) {
			parent.receiveMessage("No route tile in map data");
			parent.setRoute(null);
			return;
		}

		try {
			//#debug error
			logger.info("Starting routing thread");
			Vector solve = solve();
			parent.setRoute(solve);
		} catch (NullPointerException npe) {
			parent.setRoute(null);
			parent.receiveMessage(npe.getMessage());
			logger.fatal(Locale.get("routing.RoutingRhreadCrashedWith")/*Routing thread crashed unexpectedly with error */ +  npe.getMessage());			
			npe.printStackTrace();
			
		} catch (Exception e) {
			parent.setRoute(null);
			parent.receiveMessage(e.getMessage());
			//#debug error
			logger.fatal(Locale.get("routing.RoutingRhreadCrashedWith")/*Routing thread crashed unexpectedly with error */ +  e.getMessage());
			//#debug			
			e.printStackTrace();
		} catch (Error e1){
			parent.setRoute(null);
			parent.receiveMessage(e1.getMessage());
			e1.printStackTrace();
		}
		
	} 

	public static void dropToConnectionsCache() {
		destRn = null;
		destPrevRn = null;
		distToMainstreetNetRouteNode = 0;
		if (toMark != null) {
			toMark.entity = null;
		}
	}

	public void cancelRouting() {
		Routing.stopRouting = true;
		RouteInstructions.abortRouteLineProduction();
	}

}
