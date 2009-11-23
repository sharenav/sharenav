package de.ueller.midlet.gps.routing;
import java.io.IOException;
import java.lang.Math;
import java.util.Vector;

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.intTree;
import de.ueller.gpsMid.mapData.RouteBaseTile;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.RouteInstructions;
import de.ueller.midlet.gps.RouteLineProducer;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.names.NumberCanon;




public class Routing implements Runnable {
	private Thread processorThread;
	public boolean bestTime = true;
	private final Vector nodes = new Vector();
	private final intTree open = new intTree();
	private final intTree closed = new intTree();
	private Runtime runtime = Runtime.getRuntime();

	private final static Logger logger = Logger.getInstance(Routing.class, Logger.ERROR);
	private final RouteBaseTile tile;
	private final Tile[] tiles;
	private RouteNode routeFrom;
	private RouteNode routeTo;
	private final Trace parent;
	private int bestTotal;
	private long nextUpdate;
	private static volatile boolean stopRouting = false;
	private float estimateFac = 1.40f;
	/** maximum speed estimated in m/s */
	private int maxEstimationSpeed;
	/** use maximum route calulation speed instead of deep search */
	private boolean roadRun = false;
	
	/** when true, the RouteTile will only load and return mainStreetNet RouteNodes, Connections and TurnRestrictions */ 
	public static volatile boolean onlyMainStreetNet = false;
	public int motorwayConsExamined = 0;
	public int motorwayEntrancesExamined = 0;
	
	public boolean tryFindMotorway = false;
	public boolean boostMotorways = false;
	public boolean boostTrunksAndPrimarys = false;	
	
	private int oomCounter = 0;
	private int expanded;
	private RouteNode sourcePathSegNodeDummyRouteNode = new RouteNode();
	private Connection sourcePathSegNodeDummyConnection = new Connection();
	private int currentTravelMask = 0;
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
	 * alternatives of path segments closest to the target
	 * One of these arbitrary positions on the way's path will be after the route calculation
	 * filled into the last connection (which is initially a connection at the target position).
	 * Thus it will detected as part of the route line by searchConnection2Ways 
	 * 
	 * Which position will be filled in,
	 * depends on which final connection gets selected by the routing algorithm
	 */
	int finalNodeId1 = 0;
	Node finalDestPathSegNodeDummy1 = new Node();
	int finalNodeId2 = 0;
	Node finalDestPathSegNodeDummy2 = new Node();
	
	public Routing(Tile[] tile,Trace parent) throws IOException {
		this.parent = parent;
		this.tile = (RouteBaseTile) tile[4];
		this.tiles = tile;
		estimateFac = (Configuration.getRouteEstimationFac() / 10f) + 0.8f;
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
				// for non-motorized vehicles simply set highest estimateFac
				roadRun = false;
				estimateFac = 1.8f;
			}
		}
		currentTravelMask = Configuration.getTravelMask();
	}
	
	private GraphNode search(RouteNode target) throws Exception {
		GraphNode currentNode;
		int successorCost;
		Vector children = new Vector();
		expanded=0;
		
		boolean checkForTurnRestrictions =
			Configuration.getCfgBitState(Configuration.CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION) &&
			Configuration.getTravelMode().isWithTurnRestrictions();
		
		tryFindMotorway = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_TRY_FIND_MOTORWAY);
		boostMotorways = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BOOST_MOTORWAYS);
		boostTrunksAndPrimarys = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS);
		
		// set a mainStreetNetDistance that is unreachable to indicate the mainStreetNet is disabled
		int mainStreetNetDistanceMeters = 40000000;
		int maxTimesToReduceMainStreetNet = 0;
		if (Configuration.getTravelMode().useMainStreetNetForLargeRoutes()) {
			mainStreetNetDistanceMeters = Configuration.getMainStreetDistanceKm() * 1000;
			maxTimesToReduceMainStreetNet = 4;
//			System.out.println("use main street net");
		}
		
		for (int noSolutionRetries = 0; noSolutionRetries <= maxTimesToReduceMainStreetNet; noSolutionRetries++) {
		int mainStreetConsExamined = 0;
		motorwayConsExamined = 0;
		while (!(nodes.isEmpty())) {
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
			if (!(currentNode.total == bestTotal)) {
				if (setBest(currentNode.total,currentNode.costs)) {
					break; // cancel route calculation 1/2
				}
			} 
			if (currentNode.state.toId == target.id) 
				return currentNode;
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
			if (successor == null){
				successor=new Connection[0];
			}
			
			// check for turn restrictions
			boolean turnRestricted []= new boolean[successor.length];
			if (checkForTurnRestrictions && tile.lastNodeHadTurnRestrictions) {
				int nextId;
				TurnRestriction turnRestriction = tile.getTurnRestrictions(currentNode.state.toId);
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
			
			// use only mainstreet net if at least mainStreetNetDistanceMeters away from start and target points and already enough main street connections have been examined
			// MainStreet Net Distance Check - this will turn on the MainStreetNet mode if we are far away enough from routeStart and routeTarget
			if (mainStreetConsExamined > 20
				&& MoreMath.dist(tile.lastRouteNode.lat,tile.lastRouteNode.lon,target.lat,target.lon) > mainStreetNetDistanceMeters
				&& MoreMath.dist(tile.lastRouteNode.lat,tile.lastRouteNode.lon,routeFrom.lat,routeFrom.lon) > mainStreetNetDistanceMeters
			) {
				// System.out.println(mainStreetConsExamined + " mainStreetConsExamined " + MoreMath.dist(tile.lastRouteNode.lat,tile.lastRouteNode.lon,target.lat,target.lon));
				// turn on mainStreetNetMode
				Routing.onlyMainStreetNet = true;
			} else {
				Routing.onlyMainStreetNet = false;
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
				if (currentNode.parent != null && nodeSuccessor.toId == currentNode.parent.state.toId) {
					continue;
				}

				int dTurn=currentNode.fromBearing-nodeSuccessor.startBearing;
				int turnCost=getTurnCost(dTurn);
				successorCost = currentNode.costs + nodeSuccessor.cost+turnCost;
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
						theNode.total = theNode.costs + theNode.distance;
						theNode.parent = currentNode; 
						theNode.fromBearing=currentNode.state.endBearing;
						children.addElement(theNode);
					}
				// not in open or closed
				} else { 
					int estimation;
					GraphNode newNode;
					estimation = estimate(currentNode.state,nodeSuccessor, target);
					newNode = new GraphNode(nodeSuccessor, currentNode, successorCost, estimation, currentNode.fromBearing);
					if (checkForTurnRestrictions) {
						open.put(nodeSuccessor.connectionId, newNode);
					} else {
						open.put(nodeSuccessor.toId, newNode);
					}
//					parent.getRouteNodes().addElement(new RouteHelper(newNode.state.to.lat,newNode.state.to.lon,"t"+expanded));
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
		if (Routing.stopRouting) {
			break;
		} else {
			if (mainStreetNetDistanceMeters < 40000000) {
				// when using the mainStreetNet has given no solution retry with less mainStreetNet by increasing the estimated distance to the mainstreetNet
				mainStreetNetDistanceMeters *= 2;
				parent.receiveMessage("retry less mainStreetNet");
			} else {
				break; // when no mainstreetnet mode is active no retries are required
			}
		}
		} // end noSolutionRetries loop
		if (!Routing.stopRouting) {
			parent.receiveMessage("no Solution found");
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
					+ "min " + (100*actual/total)
					+ "% m:" + runtime.freeMemory()/1000 
					+ "k s:" + oomCounter+"/"+expanded+"/"+open.size());
		} else {
			parent.receiveMessage("" + (bestTotal/1000f) 
					+ "km " + (100*actual/total)
					+ "% m:" + runtime.freeMemory()/1000 
					+ "k s:" + oomCounter+"/"+expanded);
		}
		nextUpdate=now + 1000;
		}
		return false;
	}

	private void addToNodes(Vector children) {
		for (int i = 0; i < children.size(); i++) { 
			GraphNode newNode = (GraphNode) children.elementAt(i);
			long newTotal = newNode.total;
			long newCosts = newNode.costs;
			boolean done = false;
			int idx = bsearch(0, nodes.size()-1, newTotal, newCosts);
			nodes.insertElementAt(newNode, idx); 
		}
	}

	private int bsearch(int l, int h, long tot, long costs){
		int lo = l;
		int hi = h;
		while(lo<=hi) {
			int cur = (lo+hi)/2;
			long ot = ((GraphNode)nodes.elementAt(cur)).total;
			if((tot < ot) || (tot == ot && costs >= ((GraphNode) nodes.elementAt(cur)).costs)) 
				hi = cur - 1;
			else lo = cur + 1;
		} 
		return lo; //insert before lo 
	} 

	
	private int getTurnCost(int turn) {
		int adTurn=Math.abs(turn*2);
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
	/**
	 * @param nodeSuccessor
	 * @param target
	 * @return
	 */
	private int estimate(Connection from,Connection to, RouteNode target) {
//		if (noHeuristic){
//			return 0;
//		}
		int dTurn=from.endBearing-to.startBearing;
		int turnCost=getTurnCost(dTurn);
		RouteNode toNode=getRouteNode(to.toId);
		if (toNode == null){
			//#debug error
			logger.info("RouteNode ("+to.toId+") = null" );
			return (10000000);
		}
		if (target == null){
			throw new Error("Target is NULL");
		}
		int dist = MoreMath.dist(toNode.lat,toNode.lon,target.lat,target.lon);
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
						/* estimate 80 Km/h (22 m/s) as average speed if we are on the motorway and only 1 motorway entrance has been examined yet
						 * This gives the 2nd entrance in the opposite direction also a chance to get examined
						 */
						return (int) (((dist/2.2f)+turnCost)*estimateFac);
					}
				}
				// estimate 120 Km/h (36 m/s) as average speed to enter the motorway or on the motorway if at least 2 entrances where examined 
				return (int) (((dist/3.6f))*estimateFac);
			}

			
			// if the air distance is more than 20km try to find a motorway that is at maximum 20 km or half of the distance to route start away from the route start 
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
			return (int) ((dist*1.1f + turnCost)*estimateFac);
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
		return tile.getRouteNode(id);
	} 


	public void solve (PositionMark fromMark,PositionMark toMark) {

		logger.info("Calculating route from " + fromMark + " to " + toMark);

		// when we search the closest routeNode, we must be able to access all routeNodes, not only the mainStreetNet one's
		Routing.onlyMainStreetNet = false;
		
		try {
			if (toMark == null) {
				parent.receiveMessage("Please set target first");
				parent.setRoute(null);
				return;
			} 
			
			RouteNode startNode=new RouteNode();
			startNode.lat=fromMark.lat;
			startNode.lon=fromMark.lon;

			// always search a way for the from position as the travel mode might have changed
			parent.receiveMessage("search for start element");
			parent.searchNextRoutableWay(fromMark);
			if (fromMark.entity == null){
				parent.receiveMessage("No Way found for start point");
			} 

			// if the target way is not routable, e.g. an area, remove it as target entity and thus search for a routable way nearby the target node
			if (toMark.entity != null && ( !(toMark.entity instanceof Way) || !((Way) toMark.entity).isRoutableWay() ) ) {
				toMark.entity = null;
			}

			if (toMark.entity == null) {
				// if there is no element in the to Mark, fill it from tile-data
				parent.receiveMessage("search target way");
				parent.searchNextRoutableWay(toMark);
				if (toMark.entity == null) {
					parent.receiveMessage("No way at target");
					parent.setRoute(null);
					return;
				}
			}
			
			logger.info("Calculating route from " + fromMark + " to " + toMark);
			
			if (fromMark.entity instanceof Way) {
				// search the next route node in all accessible directions. Then create 
				// connections that lead form the start point to the next route nodes.
				parent.receiveMessage("create from Connections");
				Way w=(Way) fromMark.entity;
				int nearestSegment=getNearestSeg(w, startNode.lat, startNode.lon, fromMark.nodeLat,fromMark.nodeLon);
				// roundabouts don't need to be explicitely tagged as oneways in OSM according to http://wiki.openstreetmap.org/wiki/Tag:junction%3Droundabout
					
//				parent.getRouteNodes().addElement(new RouteHelper(fromMark.nodeLat[nearestSegment],fromMark.nodeLon[nearestSegment],"oneWay sec"));
				RouteNode rn=findPrevRouteNode(nearestSegment-1, startNode.lat, startNode.lon, fromMark.nodeLat,fromMark.nodeLon);
				if (rn != null) {
					routeFrom = rn;
					firstNodeId1 = rn.id; // must be before the oneDirection check as this routeNode might be the source node for connection/duration determination
					if (! w.isOneDirectionOnly() ) { // if no against oneway rule applies
//						parent.getRouteNodes().addElement(new RouteHelper(rn.lat,rn.lon,"next back"));
						// TODO: fill in bearings and cost
						Connection initialState=new Connection(rn,0,(byte)0,(byte)0, -1);
						GraphNode firstNode=new GraphNode(initialState,null,0,0,(byte)0);
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
					Connection initialState=new Connection(rn,0,(byte)0,(byte)0, -2);
					GraphNode firstNode=new GraphNode(initialState,null,0,0,(byte)0);
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
			parent.receiveMessage("create to Connections");
			Way w = (Way) toMark.entity;
			int nearestSeg = getNearestSeg(w,toMark.lat, toMark.lon, toMark.nodeLat, toMark.nodeLon);
			RouteTileRet nodeTile=new RouteTileRet();
			// roundabouts don't need to be explicitly tagged as oneways in OSM according to http://wiki.openstreetmap.org/wiki/Tag:junction%3Droundabout
			RouteNode nextNode = findNextRouteNode(nearestSeg, toMark.lat, toMark.lon, toMark.nodeLat, toMark.nodeLon);
			if (nextNode != null) {
				finalNodeId2 = nextNode.id; // must be before the oneDirection check as this routeNode might be the destination node for connection/duration determination
				if (! w.isOneDirectionOnly() ){ // if no against oneway rule applies
					// TODO: fill in bearings and cost
					Connection newCon=new Connection(routeTo,0,(byte)0,(byte)0, -3);
					tile.getRouteNode(nextNode.lat, nextNode.lon, nodeTile);
					nodeTile.tile.addConnection(nextNode,newCon,bestTime);
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
			RouteNode prefNode = findPrevRouteNode(nearestSeg - 1, toMark.lat, toMark.lon, toMark.nodeLat, toMark.nodeLon);
			// TODO: fill in bearings and cost
			Connection newCon=new Connection(routeTo,0,(byte)0,(byte)0, -4);
			tile.getRouteNode(prefNode.lat, prefNode.lon, nodeTile);
			nodeTile.tile.addConnection(prefNode,newCon,bestTime);
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
			finalNodeId1 = prefNode.id;
			finalDestPathSegNodeDummy1.radlat = toMark.nodeLat[finalSeg];
			finalDestPathSegNodeDummy1.radlon = toMark.nodeLon[finalSeg];			
			if (routeTo != null) {
				parent.cleanup();
				System.gc();
				//#debug error
				logger.info("free mem: "+runtime.freeMemory());
				processorThread = new Thread(this);
				processorThread.setPriority(Thread.NORM_PRIORITY);
				processorThread.start();
			} else {
				parent.setRoute(null);
			}
		} catch (Exception e) {
			parent.receiveMessage("Routing Ex " + e.getMessage());
			//#debug error
			e.printStackTrace();
			parent.setRoute(null);
		}
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
			rn=tile.getRouteNode(lats[v], lons[v]);
			if (rn !=null){return rn;}
		} 
		return null;
	}
	private RouteNode findPrevRouteNode(int end,float lat, float lon,float[] lats,float[] lons){
		RouteNode rn=null;
		for (int v=end;v >= 0; v--){
			//#debug debug
			logger.debug("search point "+ lats[v] +"," + lons[v]);
			rn=tile.getRouteNode(lats[v], lons[v]);
			if (rn !=null){return rn;}
		} 
		return null;
	}
	
	private final Vector solve () {
		try {
			GraphNode solution=search(routeTo);
			nodes.removeAllElements();
			open.removeAll();
			closed.removeAll();
			tile.cleanup(-1);
			if (solution == null) {
				return null; // cancel route calculation 2/2
			}
			if (bestTime){
				if (Configuration.getDebugSeverityDebug()) {
					parent.receiveMessage("Route found: " + (bestTotal/600) + "min");
				} else {
					parent.receiveMessage("Route found");					
				}
			} else {
				parent.receiveMessage("Route found: " + (bestTotal/1000f) + "km");
			}
			// when finally we get the sequence we must be able to access all route nodes, not only the mainstreet net's
			Routing.onlyMainStreetNet = false;
			Vector sequence = getSequence(solution);

			// move connection durations so that they represent the time to travel to the next node, rather than to the current node
			ConnectionWithNode c;
			ConnectionWithNode cPrev = (ConnectionWithNode) sequence.elementAt(0);
			for (int i = 0; i < sequence.size(); i++) {
				c = (ConnectionWithNode) sequence.elementAt(i);
				cPrev.durationFSecsToNext = c.durationFSecsToNext;
				cPrev = c;
				if (i == sequence.size() - 1) {
					c.durationFSecsToNext = 5555;
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
			ConnectionWithNode cSecond = (ConnectionWithNode) sequence.elementAt(1);

			if (firstNodeId1 == cFirst.to.id) {
				sourcePathSegNodeDummyConnectionNode.to.lat=firstSourcePathSegNodeDummy1.radlat;
				sourcePathSegNodeDummyConnectionNode.to.lon=firstSourcePathSegNodeDummy1.radlon;
				sourcePathSegNodeDummyConnectionNode.to.id = firstNodeId2;  //remember id to be able to determine the connection with its duration
			}
			if (firstNodeId2 == cFirst.to.id) {
				sourcePathSegNodeDummyConnectionNode.to.lat=firstSourcePathSegNodeDummy2.radlat;
				sourcePathSegNodeDummyConnectionNode.to.lon=firstSourcePathSegNodeDummy2.radlon;
				sourcePathSegNodeDummyConnectionNode.to.id = firstNodeId1;  //remember id to be able to determine the connection with its duration
			}

			sequence.insertElementAt(sourcePathSegNodeDummyConnectionNode, 0);
			sourcePathSegNodeDummyConnectionNode.durationFSecsToNext = getConnectionDurationFSecsForRouteNodes(sourcePathSegNodeDummyConnectionNode.to.id, cFirst.to.id);
			
			/**
			 * same for the final connection (which is initially at the target)
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
			parent.receiveMessage("Routing Ex " + e.getMessage());
			//#debug error
			e.printStackTrace();
			return null;
		}
	}
	private Vector getSequence(GraphNode n) { 
		Vector result;
		if (n == null) {
			result = new Vector();
		} else { 
			result = getSequence (n.parent);
			ConnectionWithNode c=new ConnectionWithNode(getRouteNode(n.state.toId),n.state);
			result.addElement(c);
		} 
		return result; 
	}

	private short getConnectionDurationFSecsForRouteNodes(int rnFromId, int rnToId) {
		Connection successor[] = tile.getConnections(rnFromId,tile,bestTime);
		if (successor != null) {
			for (int cl=0;cl < successor.length;cl++){
				Connection nodeSuccessor=successor[cl];
				if (nodeSuccessor.toId == rnToId) {
					System.out.println("CONNECTION FOUND: " + nodeSuccessor.durationFSecs / 5 );
					return nodeSuccessor.durationFSecs;
				}
			}
		}
		return 0;
	}
	
	public void run() {
		RouteInstructions.abortRouteLineProduction();
		Routing.stopRouting = false;
		
		try {
			//#debug error
			logger.info("Start Routing thread");
			Vector solve = solve();
			parent.setRoute(solve);
		} catch (NullPointerException npe) {
			parent.setRoute(null);
			parent.receiveMessage(npe.getMessage());
			logger.fatal("Routing thread crashed unexpectadly with error " +  npe.getMessage());			
			npe.printStackTrace();
			
		} catch (Exception e) {
			parent.setRoute(null);
			parent.receiveMessage(e.getMessage());
			//#debug error
			logger.fatal("Routing thread crashed unexpectadly with error " +  e.getMessage());
			//#debug			
			e.printStackTrace();
		} catch (Error e1){
			parent.setRoute(null);
			parent.receiveMessage(e1.getMessage());
			e1.printStackTrace();
		}
		
	} 

	public void cancelRouting() {
		Routing.stopRouting = true;
		RouteInstructions.abortRouteLineProduction();
	}

}
