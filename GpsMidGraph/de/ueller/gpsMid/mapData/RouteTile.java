package de.ueller.gpsMid.mapData;

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Graphics;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.routing.RouteTileRet;
import de.ueller.midlet.gps.routing.TurnRestriction;
import de.ueller.midlet.gps.tile.PaintContext;

public class RouteTile extends RouteBaseTile {

	RouteNode[] nodes=null;
	TurnRestriction[] turns=null;
	Connection[][] connections=null;

	private final static Logger logger=Logger.getInstance(RouteTile.class,Logger.INFO);

	RouteTile(DataInputStream dis, int deep, byte zl) throws IOException {
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
    	minId=dis.readInt();
    	maxId=dis.readInt();
		fileId = (short) dis.readInt();
		//#debug error
		logger.debug("created RouteTile deep:" + deep + ":RT Nr=" + fileId + "id("+minId+"/"+maxId+")");
	}

	
	public RouteTile() {
		// TODO Auto-generated constructor stub
	}


	public boolean cleanup(int level) {
		if (nodes != null){
			if (level > 0 && permanent){
//				logger.debug("Protected content for " + this +" with level " + level);
				return false;
			}
			if (lastUse >= level){
				nodes=null;
				connections=null;
//				logger.debug("discard content for " + this +" with level " + level);
				return true;
			} else {
				lastUse++;
				return false;
			}
		}
		return false;
	}

	/**
	 * Only for debugging purposes to show the routeNode and their connections / turn restrictions
	 * This debugging can be activated by setting the corresponding Setup / Debug Options
	 */
	public void paint(PaintContext pc, byte layer) {
		if (pc == null){ // ignore layer as this is always called with layer 0
			return;
		}
		if (contain(pc)){
			boolean showConnections = Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS);
			// show route cost only if Alternative Info/Type Information in Map Features is activated
			boolean showCost = Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE);			
			boolean showTurnRestrictions = Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS);
			
//			drawBounds(pc, 255, 255, 255);
			if (nodes == null){
				try {
					loadNodes();
				} catch (IOException e) {
					logger.exception("Failed to load routing nodes", e);
					return;
				}
			}
			if (showConnections && connections == null){
				try {
					loadConnections(true);
				} catch (IOException e) {
					logger.exception("Failed to load routing connections", e);
					return;
				}
			}
			
			Graphics g = pc.g;
			for (int i=0; i< nodes.length;i++){
				if (pc.getP().isPlotable(nodes[i].lat, nodes[i].lon)){
					pc.getP().forward(nodes[i].lat, nodes[i].lon, pc.swapLineP);
					if (showConnections) {
						g.drawRect(pc.swapLineP.x-2, pc.swapLineP.y-2, 5, 5); //Draw node
						for (int ii=0; ii< connections[i].length;ii++){
							Connection c=connections[i][ii];
							Connection [] reverseCons = null;
							RouteNode rnt=getRouteNode(c.toId);						
							if (rnt == null){
								RouteBaseTile dict = (RouteBaseTile) Trace.getInstance().getDict((byte)4);
								rnt=dict.getRouteNode(c.toId);
								if (rnt != null) {
									reverseCons = dict.getConnections(rnt.id,dict,true);
								} else {
									g.setColor(255, 70, 70);
									final byte radius = 10;
									g.fillArc(pc.swapLineP.x-radius/2,pc.swapLineP.y-radius/2,radius,radius,0,359);
								}
							} else {
								reverseCons = getConnections(rnt.id, this, true);
							}
							if (rnt == null){
								//#debug info
								logger.info("Routenode not found");
							} else {
								/**
								 * Try and determine if the connection is a one way connection
								 */
								boolean oneway = true; 
								for (int iii = 0;iii < reverseCons.length; iii ++) {
									Connection c2=reverseCons[iii];
									if (c2.toId - minId == i) {
										oneway = false;
									}
								}
								if (oneway) {
									g.setColor(255, 100, 100);
								} else {
									g.setColor(0, 100, 255);
								}
								pc.getP().forward(rnt.lat, rnt.lon, pc.lineP2);
								g.drawLine(pc.swapLineP.x, pc.swapLineP.y, pc.lineP2.x, pc.lineP2.y);
								g.setColor(0, 0, 0);
								if (showCost) {
									g.drawString(Integer.toString(c.cost), (pc.swapLineP.x + pc.lineP2.x) / 2, (pc.swapLineP.y + pc.lineP2.y) / 2, Graphics.TOP | Graphics.RIGHT);
								}
							}
						}
					}
					if (showTurnRestrictions && nodes[i].hasTurnRestrictions()) {
						g.setColor(0xD00000);
						g.fillRect(pc.swapLineP.x-3, pc.swapLineP.y-2, 7, 7); //Draw node
						TurnRestriction turnRestriction = getTurnRestrictions(nodes[i].id);
						int drawOffs = 0;
						RouteBaseTile dict = (RouteBaseTile) Trace.getInstance().getDict((byte)4);
						while (turnRestriction != null) {
							g.setColor(0);
							g.drawString(turnRestriction.getRestrictionType(), pc.swapLineP.x, pc.swapLineP.y + drawOffs * g.getFont().getHeight(), Graphics.TOP | Graphics.HCENTER);
							RouteNode from = dict.getRouteNode(turnRestriction.fromRouteNodeId);
							if (from != null) {
								if (turnRestriction.isOnlyTypeRestriction()) {
									g.setColor(0x00008000); // dark green
								} else {
									g.setColor(0x00800000); // dark red						
								}
								pc.getP().forward(from.lat, from.lon, pc.lineP2);
								g.setStrokeStyle(Graphics.DOTTED);
								g.drawLine(pc.swapLineP.x, pc.swapLineP.y + drawOffs, pc.lineP2.x, pc.lineP2.y + drawOffs);
							}
							//#mdebug debug
							else {
								logger.debug("from not found");
							}
							//#enddebug
							RouteNode to = dict.getRouteNode(turnRestriction.toRouteNodeId);
							if (to != null) {
								if (turnRestriction.isOnlyTypeRestriction()) {
									g.setColor(0x0000FF00); // light green
								} else {
									g.setColor(0x00FF0000); // light red						
								}
								pc.getP().forward(to.lat, to.lon, pc.lineP2);
								g.setStrokeStyle(Graphics.SOLID);
								g.drawLine(pc.swapLineP.x, pc.swapLineP.y + drawOffs, pc.lineP2.x, pc.lineP2.y + drawOffs);									
							}
							//#mdebug debug
							else {								
								logger.debug("to not found");
							}
							//#enddebug
							turnRestriction = turnRestriction.nextTurnRestrictionAtThisNode;
							drawOffs++;
						}							
					}
				}
			}
			lastUse=0;
		}
	}


	public RouteNode getRouteNode(int id) {
		if (minId <= id && maxId >= id){
			//#debug debug
			logger.debug("getRouteNode("+id+")");
			lastUse=0;
			if (nodes == null){
				try {
					loadNodes();
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


	private void loadNodes() throws IOException {
		DataInputStream ts=new DataInputStream(Configuration.getMapResource("/t4" + fileId + ".d"));
		short count = ts.readShort();
		//#debug debug
		logger.debug("load nodes "+count+" ("+minId+"/"+maxId+") in Tile t4" + fileId + ".d");
		nodes = new RouteNode[count];
		for (short i = 0; i< count ; i++){
			RouteNode n = new RouteNode();
			n.lat=ts.readFloat();
			n.lon=ts.readFloat();
//			n.conFp=ts.readInt();
//			ts.readInt();
			n.setConSize(ts.readByte());
//			n.fid=fileId;
			n.id=i+minId;
			nodes[i]=n;
		}
				
		loadTurnRestrictions(ts);
		
		ts.close();
	}
	
	private void loadTurnRestrictions(DataInputStream ts) throws IOException {
		short count = ts.readShort();
		turns = new TurnRestriction[count];
		for (short i = 0; i < count; i++) {
			TurnRestriction turn = new TurnRestriction();
			turn.viaRouteNodeId = ts.readInt();
			turn.fromRouteNodeId = ts.readInt();
			turn.toRouteNodeId = ts.readInt();
			turn.affectedTravelModes = ts.readByte();
			turn.flags = ts.readByte();
			if (i > 0 && turn.viaRouteNodeId == turns[i-1].viaRouteNodeId) {
				turns[i-1].nextTurnRestrictionAtThisNode = turn;
			}
			turns[i] = turn;
		}
//		if (count > 0) {
//			System.out.println(count + " turn restrictions loaded");
//		}
	}

	public RouteNode getRouteNode(RouteNode best, float lat, float lon) {
		if (contain(lat,lon,0.03f)){
			if (nodes == null){
				try {
					loadNodes();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			if (best == null){
				best=nodes[0];
			}
			int bestDist = MoreMath.dist(best.lat, best.lon, lat, lon);
			for (int i=0; i<nodes.length;i++){
				RouteNode n = nodes[i];
				int dist = MoreMath.dist(n.lat, n.lon, lat, lon);
				if (dist < bestDist){
					best=n;
					bestDist=dist;
				}
			}
			lastUse=0;
		}
		return best;
	}

	public RouteNode getRouteNode(float lat, float lon) {
		if (contain(lat,lon)){
			if (nodes == null){
				try {
					loadNodes();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			for (int i=0; i<nodes.length;i++){
				RouteNode n = nodes[i];
				if (n.lat == lat && n.lon == lon){
					return n;
				}
				// due the shorts in map data we don't match exactly
				if (MoreMath.approximately_equal(n.lat,lat,0.0000005f) &&
					MoreMath.approximately_equal(n.lon,lon,0.0000005f)){
					Connection cons[] = this.getConnections(n.id, this, true);					
					//#debug debug
					logger.debug("aprox equal matches...");
					if (cons.length > 0) {
						//#debug debug
						logger.debug("...and has connections in current travel mode");
						return n;
					}
				}
			}
			lastUse=0;
		}
		return null;
	}

	public TurnRestriction getTurnRestrictions(int rnId) {
		if (minId <= rnId && maxId >= rnId){
			//#debug debug
			logger.debug("getTurnRestricition for RouteNode("+rnId+")");
			lastUse=0;
			if (nodes == null){
				try {
					loadNodes();
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
	 * @see de.ueller.gpsMid.mapData.RouteBaseTile#getRouteNode(float, float, de.ueller.midlet.gps.routing.RouteTileRet)
	 */
	public RouteNode getRouteNode(float lat,float lon,RouteTileRet retTile){
		RouteNode ret=getRouteNode(lat,lon);
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
				if (nodes == null){
					loadNodes();
				}
				if (connections == null){
					loadConnections(bestTime);
				}
				tile.lastNodeHadTurnRestrictions = nodes[id-minId].hasTurnRestrictions();
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
		int numTravelModes= C.getTravelModes().length;
		int currentTravelMode = Configuration.getTravelModeNr();			
			
		connections = new Connection[nodes.length][];
		//#debug debug
		logger.debug("getConnections in file " + "/c" + fileId + ".d");
		DataInputStream cs=new DataInputStream(Configuration.getMapResource("/c" + fileId + ".d"));
		for (int in=0; in<nodes.length;in++){
			RouteNode n=nodes[in];
			int conSizeTravelMode=0;
			int conSize=n.getConSize();
			Connection[] cons=new Connection[conSize];
			for (int i = 0; i<conSize;i++){
				Connection c=new Connection();
				int nodeId = cs.readInt();
				// fill in TargetNode but only if in the same Tile
				// don't store this costs a lot of memory this can taken from the cache if needed 
//				if (nodeId <= maxId && nodeId >= minId){
//					c.to=nodes[nodeId - minId];
//				}
				c.toId=nodeId;
				if (numTravelModes > 1) {
					c.travelModes=cs.readByte();
				} else {
					c.travelModes=0x01;
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
					if ( (c.travelModes & (1 << i2)) != 0) {
						int upper = cs.readShort();
						if (upper < 0) {
							int lower = cs.readShort();
							upper = -1*(((0xffff & upper) << 16) + (0xffff & lower));
						}
						if (i2==currentTravelMode) {
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
					c.cost = costTime;
				} else {
					c.cost = costLength;
				}
				c.startBearing=cs.readByte();
				c.endBearing=cs.readByte();
				cons[i]=c;
			}
			
			
			if (conSizeTravelMode == conSize) {
				connections[in]=cons;
			} else {
				// if not all connections are uses, copy only the used connections to a new array that will get used
				// TODO: therefore try to optimise this with a static array for cons
				Connection[] cons2=new Connection[conSizeTravelMode];
				int i2=0;
				for (int i = 0; i<conSize;i++){
					if ( (cons[i].travelModes & (1 << currentTravelMode)) != 0) {
						cons2[i2] = cons[i];
						i2++;
					}
				}
				connections[in]=cons2;
			}
				
		}
		/**
		 * Check to see if everything went well with reading the tile.
		 */
		int endMarker = cs.readInt();
		if (endMarker != 0xdeadbeaf) {
			logger.error("RouteTile did not read correctly");
			throw new IOException("Failed to read correct end of file marker. Read " + 
					endMarker + " but expected " + 0xdeadbeaf);
		}
	}
	public String toString() {
		return "RT" + "-" + fileId + ":" + lastUse + ((permanent)?" perm":"");
	}


	public void addConnection(RouteNode rn, Connection newCon, boolean bestTime) {
		int addIdx=-1;
		for (int i=0; i<nodes.length;i++){
			RouteNode n = nodes[i];
			if (rn==n){
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
