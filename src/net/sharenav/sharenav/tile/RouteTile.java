/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See file COPYING
 */

package net.sharenav.sharenav.tile;

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Graphics;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.PaintContext;
import net.sharenav.sharenav.mapdata.DictReader;
import net.sharenav.sharenav.routing.Connection;
import net.sharenav.sharenav.routing.RouteNode;
import net.sharenav.sharenav.routing.RouteTileRet;
import net.sharenav.sharenav.routing.Routing;
import net.sharenav.sharenav.routing.TurnRestriction;
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.util.IntPoint;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;

import de.enough.polish.util.Locale;


public class RouteTile extends RouteBaseTile {

	RouteNode[] nodes = null;
	TurnRestriction[] turns = null;
	Connection[][] connections = null;
	boolean onlyMainStreetNetLoaded = true;
	short numMainStreetRouteNodes = 0;
	private final static Connection[] emptyCons = new Connection[0];
	
	private final static Logger logger = Logger.getInstance(RouteTile.class, Logger.INFO);

	
	public RouteTile(DataInputStream dis, int deep, byte zl) throws IOException {
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
    	minId=dis.readInt();
    	maxId=dis.readInt();
		fileId = dis.readInt();
		//#debug error
		logger.debug("created RouteTile deep:" + deep + ":RT Nr=" + fileId + "id("+minId+"/"+maxId+")");
	}

	public boolean cleanup(int level) {
		if (nodes != null) {
			if (level > 0 && permanent) {
//				logger.debug("Protected content for " + this +" with level " + level);
				return false;
			}
			if (lastUse >= level) {
				nodes = null;
				connections = null;
				turns = null;
				onlyMainStreetNetLoaded = true;
//				logger.debug("discard content for " + this +" with level " + level);
				return true;
			} else {
				lastUse++;
				return false;
			}
		}
		return false;
	}

	public boolean loadNodesRequired() {
		return (nodes == null || (onlyMainStreetNetLoaded && !Routing.onlyMainStreetNet));
	}
	
	/**
	 * Only for debugging purposes to show the routeNode and their connections / turn restrictions
	 * This debugging can be activated by setting the corresponding Setup / Debug Options
	 */
	public void paint(PaintContext pc, byte layer) {
		// ignore layer as this is always called with layer 0
		if (pc == null) {
			return;
		}

		boolean showConnections = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS);
		boolean showTurnRestrictions = Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS);
		// return if neither connections nor turn restrictions have to be shown
		if (! (showConnections || showTurnRestrictions)) {
			return;
		}

		RouteBaseTile dict = (RouteBaseTile) Trace.getInstance().getDict((byte)DictReader.ROUTEZOOMLEVEL);
		
		// show route cost only if Alternative Info/Type Information in Map Features is activated
		boolean showCost = Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE);			

		if (contain(pc)) {

//			drawBounds(pc, 255, 255, 255);
			if (nodes == null || onlyMainStreetNetLoaded) {
				try {
					loadNodes(false, -1);
				} catch (IOException e) {
					logger.exception(Locale.get("routetile.FailedLoadingRoutingNodes")/*Failed to load routing nodes*/, e);
					return;
				}
			}
			if (showConnections && connections == null) {
				try {
					loadConnections(!Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AIM));
				} catch (IOException e) {
					logger.exception(Locale.get("routetile.FailedLoadingRoutingConnections")/*Failed to load routing connections*/, e);
					return;
				}
			}
			
			Graphics g = pc.g;

//			int turnPaint = 0;
			
			for (int i = 0; i < nodes.length; i++) {
				if (pc.getP().isPlotable(nodes[i].lat, nodes[i].lon)) {
					pc.getP().forward(nodes[i].lat, nodes[i].lon, pc.swapLineP);
					if (showConnections) {
						g.drawRect(pc.swapLineP.x - 2, pc.swapLineP.y - 2, 5, 5); //Draw node
						if (nodes[i].isAtTrafficSignals()) {
							g.setColor(0x00FF0000);
							g.drawRect(pc.swapLineP.x - 3, pc.swapLineP.y - 3, 7, 7); // mark traffic lights
							g.setColor(0x0000FF00);
							g.drawRect(pc.swapLineP.x - 1, pc.swapLineP.y - 1, 3, 3); // mark traffic lights
						}
						for (int ii = 0; ii < connections[i].length; ii++) {
							Connection c = connections[i][ii];
							Connection [] reverseCons = null;
							RouteNode rnt = getRouteNode(c.toId);						
							if (rnt == null) {
								rnt = dict.getRouteNode(c.toId);
								if (rnt != null) {
									reverseCons = dict.getConnections(rnt.id, dict, !Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AIM));
								} else {
									g.setColor(255, 70, 70);
									final byte radius = 10;
									g.fillArc(pc.swapLineP.x - radius / 2, 
											pc.swapLineP.y - radius / 2, radius, radius, 
											0, 359);
								}
							} else {
								reverseCons = getConnections(rnt.id, this, true);
							}
							if (rnt == null) {
								//#debug info
								logger.info("Routenode not found");
							} else {
								/**
								 * Try and determine if the connection is a one way connection
								 */
								boolean oneway = true; 
								for (int iii = 0; iii < reverseCons.length; iii ++) {
									Connection c2=reverseCons[iii];
									if (c2.toId - minId == i) {
										oneway = false;
//										if (c.isMainStreetNet() != c2.isMainStreetNet()) {
//											System.out.println("WARNING: Con and ReverseCon in different street nets at " + rnt.toString());
//										}
									}
								}
								if (oneway) {
									if (c.isMainStreetNet()) {
										g.setColor(255, 200, 200);
									} else {
										g.setColor(255, 50, 50);										
									}
								} else {
									if (c.isMainStreetNet()) {
										g.setColor(100, 200, 255);
									} else {
										g.setColor(0, 50, 200);
									}
								}
								pc.getP().forward(rnt.lat, rnt.lon, pc.lineP2);
								g.drawLine(pc.swapLineP.x, pc.swapLineP.y, pc.lineP2.x, pc.lineP2.y);
								g.setColor(0, 0, 0);
								if (showCost) {
									g.drawString(Integer.toString(c.getCost()), 
											(pc.swapLineP.x + pc.lineP2.x) / 2, 
											(pc.swapLineP.y + pc.lineP2.y) / 2, 
											Graphics.TOP | Graphics.RIGHT);
								}
							}
						}
					}
					if (showTurnRestrictions && nodes[i].hasTurnRestrictions()) {
						IntPoint viaNodeP = new IntPoint();
						viaNodeP.set(pc.swapLineP);
						g.setColor(0xD00000);
						//Draw viaTo node
						g.fillRect(viaNodeP.x - 3, viaNodeP.y - 2, 7, 7);
						TurnRestriction turnRestriction = getTurnRestrictions(nodes[i].id);
						int drawOffs = 0;
						while (turnRestriction != null) {						
//							turnPaint++;
//							if (turnPaint > 2) { // paint only a certain turn restriction for debugging
//								turnRestriction = turnRestriction.nextTurnRestrictionAtThisNode;
//								continue;
//							}

							g.setColor(0);
							g.drawString(turnRestriction.getRestrictionType(), 
									viaNodeP.x, viaNodeP.y + drawOffs * g.getFont().getHeight(), 
									Graphics.TOP | Graphics.HCENTER);
							RouteNode to = dict.getRouteNode(turnRestriction.toRouteNodeId);
							if (to != null) {
								if (turnRestriction.isOnlyTypeRestriction()) {
									g.setColor(0x0000FF00); // light green
								} else {
									g.setColor(0x00FF0000); // light red						
								}
								pc.getP().forward(to.lat, to.lon, pc.lineP2);
								g.setStrokeStyle(Graphics.SOLID);
								g.drawLine(viaNodeP.x, viaNodeP.y + drawOffs, 
										pc.lineP2.x, pc.lineP2.y + drawOffs);									
							}
							//#mdebug debug
							else {								
								logger.debug("to not found");
							}
							//#enddebug

							// draw the viaFrom nodes if this turnRestriction has a viaWay, in the end the coordinates of the real viaFrom node will be in swapLineP
							if (turnRestriction.isViaTypeWay()) {
								pc.lineP2.set(viaNodeP);
								for (int a = turnRestriction.extraViaNodes.length-1; a >= 0; a--) { // count backwards so in the end the coordinates of the real viaFrom will be in swapLineP
									RouteNode viaFrom = dict.getRouteNode(turnRestriction.extraViaNodes[a]);
									if (viaFrom != null) {
										pc.getP().forward(viaFrom.lat, viaFrom.lon, pc.swapLineP);														
										g.setStrokeStyle(Graphics.SOLID);
										g.setColor(0xFFFFFF); // draw the viaWay white
										g.drawLine(pc.swapLineP.x, pc.swapLineP.y + drawOffs, pc.lineP2.x, pc.lineP2.y + drawOffs);
										pc.lineP2.set(pc.swapLineP);
										if (a == 0) {
											g.setColor(0x0000D0); // draw the real viaFrom node in blue
											g.fillRect(pc.swapLineP.x-3, pc.swapLineP.y-2, 7, 7); //Draw node
										} else {
											g.setColor(0xFFFFFF); // draw the other nodes in white
											g.drawRect(pc.swapLineP.x-3, pc.swapLineP.y-2, 7, 7); //Draw node											
										}
									}
								}
							}
							
							RouteNode from = dict.getRouteNode(turnRestriction.fromRouteNodeId);
							if (from != null) {
								if (turnRestriction.isOnlyTypeRestriction()) {
									g.setColor(0x00008000); // dark green
								} else {
									g.setColor(0x00800000); // dark red						
								}
								pc.getP().forward(from.lat, from.lon, pc.lineP2);
								g.setStrokeStyle(Graphics.DOTTED);
								g.drawLine(pc.swapLineP.x, pc.swapLineP.y + drawOffs, 
										pc.lineP2.x, pc.lineP2.y + drawOffs);
							}
							//#mdebug debug
							else {
								logger.debug("from not found");
							}
							//#enddebug
							turnRestriction = turnRestriction.nextTurnRestrictionAtThisNode;
							drawOffs++;
						}							
					}
				}
			}
			lastUse = 0;
		}
	}


	public RouteNode getRouteNode(int id) {
		if (minId <= id && maxId >= id){
			//#debug debug
			logger.debug("getRouteNode("+id+")");
			lastUse=0;
			if (loadNodesRequired() || id - minId >= nodes.length ){
				try {
					loadNodes(Routing.onlyMainStreetNet, id);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			//#debug debug
			logger.debug("getRouteNode("+id+") at "+(id-minId));
			return nodes[id - minId];
		} else 
			return null;
	}


	private void loadNodes(boolean onlyMainStreetNet, int idRequested) throws IOException {
		// when we (re)read the nodes and turn restrictions, the connections must be reread as well because the normal streetNet might be included now as well
		connections = null;
		
		DataInputStream ts=new DataInputStream(Configuration.getMapResource("/t4/" + fileId + ".d"));
		numMainStreetRouteNodes = ts.readShort();
		short numNormalStreetRouteNodes = ts.readShort();

		short numMainStreetTurnRestrictions = ts.readShort();
		short numNormalStreetTurnRestrictions = ts.readShort();
		
		/* a routeNode that's not in the mainStreetNet might have been requested e.g. from getConnections() if since the last getRouteNode()
		 * this tile has been cleaned up and Routing.onlyMainstreetNet also changed to true
		 */
		if (idRequested != -1 && idRequested - minId >= numMainStreetRouteNodes) {
			/* in this case we must load all nodes, even if onlyMainStreetNet was requested */
			onlyMainStreetNet = false;
		}
		
		int maxReadStreetNets = 1;
		int totalRouteNodesToLoad = numMainStreetRouteNodes + numNormalStreetRouteNodes;
		int totalTurnRestrictionsToLoad = numMainStreetTurnRestrictions + numNormalStreetTurnRestrictions;
		if (onlyMainStreetNet) {
			maxReadStreetNets = 0;
			totalRouteNodesToLoad = numMainStreetRouteNodes;
			totalTurnRestrictionsToLoad = numMainStreetTurnRestrictions;
//			System.out.println("mainstreet load tile: " + fileId);
		} else {
//			System.out.println("full load tile: " + fileId);
			onlyMainStreetNetLoaded = false;
		}
		
		short idx = 0;
		short idxTurn = 0;
		short count = numMainStreetRouteNodes;
		short countTurnRestrictions = numMainStreetTurnRestrictions;
		//#debug debug		
		logger.debug("load nodes "+count+" ("+minId+"/"+maxId+") in Tile t4/" + fileId + ".d");
		nodes = new RouteNode[totalRouteNodesToLoad];
		turns = new TurnRestriction[totalTurnRestrictionsToLoad];
		for (int readStreetNets = 0; readStreetNets <= maxReadStreetNets; readStreetNets++) {
			for (short i = 0; i< count ; i++){
				RouteNode n = new RouteNode();
				n.lat=ts.readFloat();
				n.lon=ts.readFloat();
	//			n.conFp=ts.readInt();
	//			ts.readInt();
				n.setConSizeWithFlags(ts.readByte());
	//			n.fid=fileId;
				n.id = idx + minId;
				nodes[idx++]=n;
			}
					
			loadTurnRestrictions(ts, idxTurn, countTurnRestrictions);
			// in the next loop read in the normalStreetNet routeNodes
			count = numNormalStreetRouteNodes;
			countTurnRestrictions = numNormalStreetTurnRestrictions;
			idxTurn = numMainStreetTurnRestrictions;
		}
	
		ts.close();
	}
	
	private void loadTurnRestrictions(DataInputStream ts, short idxTurn, short count) throws IOException {
		for (short i = 0; i < count; i++) {
			TurnRestriction turn = new TurnRestriction();
			turn.viaRouteNodeId = ts.readInt();
			turn.fromRouteNodeId = ts.readInt();
			turn.toRouteNodeId = ts.readInt();
			turn.affectedTravelModes = ts.readByte();
			turn.flags = ts.readByte();
			if (idxTurn > 0 && turn.viaRouteNodeId == turns[idxTurn-1].viaRouteNodeId) {
				turns[idxTurn-1].nextTurnRestrictionAtThisNode = turn;
			}
			if (turn.isViaTypeWay()) {
				int count2 = MoreMath.signedToInt(ts.readByte());
				int [] addViaNodes = new int[count2];
				for (int a=0; a < count2; a++) {
					addViaNodes[a] = ts.readInt();
				}
				turn.extraViaNodes = addViaNodes;
			}
			
			turns[idxTurn++] = turn;
		}
//		if (count > 0) {
//			System.out.println(count + " turn restrictions loaded");
//		}
	}

	// use epsilon < 0.00001f to look for best exact match
	public RouteNode getRouteNode(RouteNode best, float epsilon, float lat, float lon) {
		if (contain(lat,lon,epsilon)){
			if (loadNodesRequired()){
				try {
					loadNodes(Routing.onlyMainStreetNet, -1);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			int bestDist = Integer.MAX_VALUE;
			if (best != null) {
				bestDist = MoreMath.dist(best.lat, best.lon, lat, lon);
			}
			int nodesLength = Routing.onlyMainStreetNet ? numMainStreetRouteNodes : nodes.length;
			for (int i = 0; i < nodesLength; i++){
				RouteNode n = nodes[i];
				int dist = MoreMath.dist(n.lat, n.lon, lat, lon);
				if (dist < bestDist){
					if (
							/*
							 * if specified a very small epsilon accept only if match is an approximately equal match to the given coordinates
							 */
							(
								epsilon < 0.00001f &&
								MoreMath.approximately_equal(n.lat,lat,0.0000005f) &&
								MoreMath.approximately_equal(n.lon,lon,0.0000005f) 
							)
							||
							/*
							 *  if specified a bigger epsilon accept every better match than before
							 */
							 epsilon >= 0.00001f
							){
								best=n;
								bestDist=dist;						
					}
				}
			}
			lastUse=0;
		}
		return best;
	}

	public TurnRestriction getTurnRestrictions(int rnId) {
		if (minId <= rnId && maxId >= rnId){
			//#debug debug
			logger.debug("getTurnRestricition for RouteNode("+rnId+")");
			lastUse=0;
			if (loadNodesRequired() || rnId - minId >= nodes.length ){
				try {
					loadNodes(Routing.onlyMainStreetNet, rnId);
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			if (turns != null) {
				for (int i=0; i<turns.length; i++) {
					if (turns[i].viaRouteNodeId == rnId) {
						return turns[i];
					}
				}
			}
			return null;			
		} else 
			return null;
	}
	
	

	/* (non-Javadoc)
	 * @see net.sharenav.gpsMid.mapData.RouteBaseTile#getRouteNode(int, net.sharenav.midlet.gps.routing.RouteTileRet)
	 */
	public RouteNode getRouteNode(int id,RouteTileRet retTile){
		RouteNode ret=getRouteNode(id);
		if (ret != null){
			this.permanent=true;
			if (this instanceof RouteTile){
				retTile.tile=(RouteTile) this;
			}
		}
		return ret;
	}


	public Connection[] getConnections(int id,RouteBaseTile tile,boolean bestTime){
		if (minId <= id && maxId >= id){
			lastUse=0;
			try {
				if (loadNodesRequired() || id - minId >= nodes.length ){
					loadNodes(Routing.onlyMainStreetNet, id);
				}
				if (connections == null){
					loadConnections(bestTime);
				}
				tile.lastNodeHadTurnRestrictions = nodes[id-minId].hasTurnRestrictions();
				tile.lastRouteNode = nodes[id-minId];
				return connections[id-minId];
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}


	/**
	 * @param bestTime
	 * @throws IOException
	 */
	private void loadConnections(boolean bestTime) throws IOException {
		int numTravelModes= Legend.getTravelModes().length;
		int currentTravelMode = Configuration.getTravelModeNr();			
			
		connections = new Connection[nodes.length][];
		//#debug debug
		logger.debug("getConnections in file " + "/c/" + fileId + ".d");
		DataInputStream cs=new DataInputStream(Configuration.getMapResource("/c/" + fileId + ".d"));

		int minConnectionId = cs.readInt();
		
		for (int in=0; in<nodes.length;in++){
			RouteNode n=nodes[in];
			int conSizeTravelMode=0;
			int conSize=n.getConSize();
			Connection[] cons=new Connection[conSize];
			for (int i = 0; i<conSize;i++){
				Connection c=new Connection();
				if (n.isAtTrafficSignals()) {
					c.setStartsAtTrafficSignals();
				}
				c.connectionId = minConnectionId++;
				int nodeId = cs.readInt();
				// fill in DestNode but only if in the same Tile
				// don't store this costs a lot of memory this can taken from the cache if needed 
//				if (nodeId <= maxId && nodeId >= minId){
//					c.to=nodes[nodeId - minId];
//				}
				c.toId=nodeId;
				c.connTravelModes=cs.readByte();
				if (c.hasConnTravelModes2()) {
					c.connTravelModes2=cs.readByte();				
				}
				
				/**
				 * The connection time and connection length can either be encoded as a int or a short
				 * We indicate if it is a int by using the top most bit (sign bit). So if the read
				 * short is negative, we need to read the second short and combine it into an int.
				 * This is done, as only a small fraction of connection costs are larger than what
				 * fits into 16 bit, so we save 16 bit on most connections.
				 */
				
				int costTime = 0;
				for (int i2=0;i2<numTravelModes;i2++) { // read connection times			
					if ( (c.connTravelModes & (1 << i2)) != 0) {
						int upper = cs.readShort();
						if (upper < 0) {
							int lower = cs.readShort();
							upper = -1*(((0xffff & upper) << 16) + (0xffff & lower));
						}
						// count only connections for the current travel mode and for the loaded streetNet
						if (i2==currentTravelMode
							&& (!onlyMainStreetNetLoaded || onlyMainStreetNetLoaded && c.isMainStreetNet())
						) {
							costTime = upper;
							conSizeTravelMode++;
						}
					}
				}	
				
				int upper = cs.readShort(); // read connection length
				if (upper < 0) {
					int lower = cs.readShort();
					upper = -1*(((0xffff & upper) << 16) + (0xffff & lower));
				}
				int costLength = upper;
				if (bestTime){
					c.setCost(costTime);
				} else {
					c.setCost(costLength);
				}
				c.setDurationFSecsFromTSecs(costTime);
				c.startBearing=cs.readByte();
				c.endBearing=cs.readByte();
				cons[i]=c;
			}
			
			
			if (conSizeTravelMode == 0) { // when there are no connections from a route node for the used travel mode, save memory by reusing a final static empty connection
				connections[in] = emptyCons; 				
			} else if (conSizeTravelMode == conSize) {
				connections[in]=cons;
			} else {
				// if not all connections are used, copy only the used connections to a new array that will get used
				// TODO: therefore try to optimise this with a static array for cons
				Connection[] cons2=new Connection[conSizeTravelMode];
				int i2=0;
				for (int i = 0; i<conSize;i++){
					// copy only connections for the current travel mode and for the loaded streetNet
					if ( (cons[i].connTravelModes
						& (1 << currentTravelMode)) != 0  && (!onlyMainStreetNetLoaded || onlyMainStreetNetLoaded && cons[i].isMainStreetNet())
					) {
						cons2[i2] = cons[i];
						i2++;
					}
				}
				connections[in]=cons2;
			}
				
		}
		
		if (!onlyMainStreetNetLoaded) {
			/**
			 * Check to see if everything went well with reading the tile.
			 */
			int endMarker = cs.readInt();
			if (endMarker != 0xdeadbeaf) {
				logger.error(Locale.get("routetile.RouteTileDidNotReadCorrectly")/*RouteTile did not read correctly*/);
				throw new IOException(Locale.get("routetile.FailedToReadCorrectEOF")/*Failed to read correct end of file marker. Read */ + 
						endMarker + " but expected " + 0xdeadbeaf);
			}
		}
		cs.close();
	}
	public String toString() {
		return "RT" + "-" + fileId + ":" + lastUse + ((permanent)?" perm":"");
	}


	public void addConnection(RouteNode rn, Connection newCon, boolean bestTime) {
		int addIdx=-1;
		for (int i=0; i<nodes.length;i++){
			if (rn.id==nodes[i].id){ // compare id instead of rn==n because of cached RouteNodes
				addIdx=i;
				break;
			}
		}
		if (connections == null){
			try {
				loadConnections(bestTime);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (addIdx > 0 ){
			Connection[] cons=connections[addIdx];
			Connection[] newCons=new Connection[cons.length + 1];
			System.arraycopy(cons, 0, newCons, 0, cons.length);
			newCons[cons.length]=newCon;
			connections[addIdx]=newCons;
		}
	}
}
