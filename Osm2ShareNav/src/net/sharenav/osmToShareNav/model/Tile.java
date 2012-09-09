/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * Copyright (C) 2007  Kai Krueger
 * 
 */

package net.sharenav.osmToShareNav.model;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import net.sharenav.osmToShareNav.Configuration;
import net.sharenav.osmToShareNav.CreateShareNavData;
import net.sharenav.osmToShareNav.MyMath;
import net.sharenav.osmToShareNav.area.SeaGenerator;
import net.sharenav.osmToShareNav.tools.FileTools;


public class Tile {

	/** The boundaries of this tile or tile tree. */
	public Bounds bounds = new Bounds();

	/**
	 * Center latitude of the tile in degrees.
	 * This is used as a reference point for the relative coordinates 
	 * stored in the file.
	 */
	public float centerLat;

	/**
	 * Center longitude of the tile in degrees.
	 * This is used as a reference point for the relative coordinates 
	 * stored in the file.
	 */
	public float centerLon;

	/** If this tile is a container, this holds the reference to the first sub-tile. */
	public Tile t1 = null;

	/** If this tile is a container, this holds the reference to the second sub-tile. */
	public Tile t2 = null;

	/** TODO: I love self-explaining variable names likes this! */
	public int fid;

	/** The type of this tile, see the TYPE_XXX constants for possible values. */
	public byte type;
	
	/** The data of this tile is for this zoom level. */
	public byte zl;

	public Collection<Way> ways = null;
	private ArrayList<RouteNode> routeNodes = null;
	public Collection<Node> nodes = new ArrayList<Node>();
	int idxMin = Integer.MAX_VALUE;
	int idxMax = 0;
	short numMainStreetRouteNodes = 0;
	public static int numTrafficSignalRouteNodes = 0;
	
	private static int minConnectionId = 0;
	
	public static final byte TYPE_MAP = 1;
	public static final byte TYPE_CONTAINER = 2;
	public static final byte TYPE_FILETILE = 4;
	public static final byte TYPE_EMPTY = 3;
	public static final byte TYPE_ROUTEDATA = 5;
	public static final byte TYPE_ROUTECONTAINER = 6;
	public static final byte TYPE_ROUTEFILE = 7;

	/** TODO: Explain */
	public static final float RTEpsilon = 0.012f;
	

	public Tile() {
		super();
	}
	
	public Tile(byte zl) {
		this.zl = zl;
	}

	public Tile(byte zl, ArrayList<Way> ways, Collection<Node> nodes) {
		this.zl = zl;
		this.ways = ways;
		this.nodes = nodes;
	}

	public Tile(Bounds b) {
		bounds = b.clone();		
	}

	public void dissolveTileReferences() {
		if ( ways!=null)
			ways.clear();
		if (nodes!=null)
			nodes.clear();
		if (routeNodes!=null)
			routeNodes.clear();
		ways = null;
		nodes = null;
		routeNodes = null;
		bounds = null;

		if ( t1 != null )	{
			t1.dissolveTileReferences();
			t1=null;
		}

		if ( t2 != null )	{
			t2.dissolveTileReferences();
			t2=null;
		}
	}
	
	/** Writes the dictionary file(s) for this tile. Walks down the tile tree to do this.
	 * 
	 * @param ds Stream to use for writing if this is a container or route container tile 
	 * @param deep Current depth of this tile
	 * @param fid File ID to use
	 * @param path Base path to use for the map data
	 * @throws IOException if the file could not be created or if there was an IO error
	 */
	public void writeTileDict(DataOutputStream ds, Integer deep, 
			Sequence fid, String path) throws IOException {
		DataOutputStream lds;
		boolean openStream;
//		System.out.println("Write Tile type=" + type + " deep=" + deep + " fid=" + fid);
		if ((type == TYPE_CONTAINER || type == TYPE_ROUTECONTAINER) 
				&& deep >= Configuration.getConfiguration().getMaxDictDepth()) {
//			System.out.println("Start new Dict file");
			// Write this containerTile as a FileTile. This container will be then 
			// placed within this new FileTile. 
			if (zl != CreateShareNavData.ROUTEZOOMLEVEL) {
				ds.writeByte(TYPE_FILETILE);
				ds.writeFloat(degToRad(bounds.minLat));
				ds.writeFloat(degToRad(bounds.minLon));
				ds.writeFloat(degToRad(bounds.maxLat));
				ds.writeFloat(degToRad(bounds.maxLon));
			} else {
				ds.writeByte(TYPE_ROUTEFILE);				
				ds.writeFloat(degToRad(bounds.minLat - RTEpsilon));
				ds.writeFloat(degToRad(bounds.minLon - RTEpsilon));
				ds.writeFloat(degToRad(bounds.maxLat + RTEpsilon));
				ds.writeFloat(degToRad(bounds.maxLon + RTEpsilon));
				ds.writeInt(idxMin);
				ds.writeInt(idxMax);
			}
			ds.writeShort(fid.get());
			openStream = true;
			FileOutputStream fo = FileTools.createFileOutputStream(
					path + "/d" + zl + "/" + fid.get() + ".d");
			lds = new DataOutputStream(fo);
			lds.writeUTF("DictMid");
			fid.inc();
			deep = 1;
		} else {
			openStream = false;
			lds = ds;
			deep++;
		}
		switch (type) {
			case TYPE_MAP:
			case TYPE_ROUTEDATA:
//				System.out.println("Type 1");
				if (zl != CreateShareNavData.ROUTEZOOMLEVEL) {
					lds.writeByte(TYPE_MAP);
					lds.writeFloat(degToRad(bounds.minLat));
					lds.writeFloat(degToRad(bounds.minLon));
					lds.writeFloat(degToRad(bounds.maxLat));
					lds.writeFloat(degToRad(bounds.maxLon));
				} else {
					lds.writeByte(TYPE_ROUTEDATA);	
					lds.writeFloat(degToRad(bounds.minLat - RTEpsilon));
					lds.writeFloat(degToRad(bounds.minLon - RTEpsilon));
					lds.writeFloat(degToRad(bounds.maxLat + RTEpsilon));
					lds.writeFloat(degToRad(bounds.maxLon + RTEpsilon));
					lds.writeInt(idxMin);
					lds.writeInt(idxMax);
				}
				// TODO: Sometimes, the fid which is passed to this method is written,
				// but here, fid from the this object is used. Is this correct?
				lds.writeInt(this.fid);
//				ds.writeInt(ds.size());
				break;
			case TYPE_CONTAINER:
			case TYPE_ROUTECONTAINER:
//				System.out.println("Type 2");
				if (zl != CreateShareNavData.ROUTEZOOMLEVEL) {
					lds.writeByte(TYPE_CONTAINER);
					lds.writeFloat(degToRad(bounds.minLat));
					lds.writeFloat(degToRad(bounds.minLon));
					lds.writeFloat(degToRad(bounds.maxLat));
					lds.writeFloat(degToRad(bounds.maxLon));
				} else {
					lds.writeByte(TYPE_ROUTECONTAINER);					
					lds.writeFloat(degToRad(bounds.minLat - RTEpsilon));
					lds.writeFloat(degToRad(bounds.minLon - RTEpsilon));
					lds.writeFloat(degToRad(bounds.maxLat + RTEpsilon));
					lds.writeFloat(degToRad(bounds.maxLon + RTEpsilon));
					lds.writeInt(idxMin);
					lds.writeInt(idxMax);
				}				
				t1.writeTileDict(lds, deep, fid, path);
				t2.writeTileDict(lds, deep, fid, path);
				break;
			case TYPE_EMPTY:
//				System.out.println("Type 3");
				lds.writeByte(TYPE_EMPTY);
//			case 4:
//				System.out.println("Type 4");
//				lds.writeByte(4);
//				lds.writeFloat(degToRad(bounds.minLat));
//				lds.writeFloat(degToRad(bounds.minLon));
//				lds.writeFloat(degToRad(bounds.maxLat));
//				lds.writeFloat(degToRad(bounds.maxLon));
//				lds.writeShort(fid);
		}
		if (openStream) {
			lds.writeUTF("END"); // Magic number
			lds.close();
		}
	}

    public float degToRad(double deg) {
        return (float) (deg * (Math.PI / 180.0d));
    }
	    
    /**
     * Recalculate the bounds of this tile tree starting from
     * the current tile in case new tiles have been added to the
     * tree. Leaf tiles are assumed to have the right bound.
     * 
     * This function updates the bounds along the way 
     * 
     * @return the bounds for the current tile tree.
     */
    public Bounds recalcBounds() {
    	Bounds b1 = null;
    	Bounds b2 = null;

    	if (type == TYPE_MAP || type == TYPE_ROUTEDATA) {
    		/**
    		 * This is a leaf of the tile tree and should have correct bounds
    		 * anyway, so don't update and return the current bounds
    		 */
			return bounds;
		}

		Bounds ret = bounds.clone();

		if (t1 != null && (t1.type != TYPE_EMPTY)) {								
			b1 = t1.recalcBounds();				
			ret = b1.clone();				
		}
		
		if (t2 != null && (t2.type != TYPE_EMPTY)) {				
			b2 = t2.recalcBounds();				
			if (ret != null) {
				ret.extend(b2);
			}
		}
		bounds = ret; //Update current bounds			
		return ret;
    }
    
    public Bounds getBounds() {
    	return bounds;
    }
		
	public Collection<Way> getWays() {
		return ways;
	}
		
	public void setWays(LinkedList<Way> ways) {
		this.ways = ways;
	}
	
	public void addWay(Way w) {
		if (ways != null) {
			ways.add(w);
		}		
	}

	public ArrayList<RouteNode> getRouteNodes() {
		return routeNodes;
	}
	
	public void setRouteNodes(ArrayList<RouteNode> routeNodes) {
		this.routeNodes = routeNodes;
	}
	
	/**
	 * @param routeNode
	 */
	public void addRouteNode(RouteNode routeNode) {
		if (routeNodes == null) {
			routeNodes = new ArrayList<RouteNode>();
		}
		routeNodes.add(routeNode);
		// System.out.println("RouteNodes.add(" + routeNode + ")");
	}

	/**
	  * 
	  */
	public void renumberRouteNode(Sequence rnSeq) {
		if (type == TYPE_ROUTECONTAINER) {
			if (t1 != null) {
				t1.renumberRouteNode(rnSeq);
			}
			if (t2 != null) {
				t2.renumberRouteNode(rnSeq);
			}
		}
		if (type == TYPE_ROUTEDATA) {
			boolean isOnMainStreetNet = false;
			// renumber the mainStreetNet routeNodes in the first loop, 
			// in the second loop the remaining ones
			for (int writeStreetNets = 0; writeStreetNets <= 1; writeStreetNets++) {
				for (RouteNode rn : routeNodes) {
					isOnMainStreetNet = rn.isOnMainStreetNet();
					if (writeStreetNets == 0 && isOnMainStreetNet
						||
						writeStreetNets > 0 && !isOnMainStreetNet
					) { 
						rn.id = rnSeq.get();
//						if (isOnMainStreetNet) {
//							System.out.println("main" + rn.id);
//						} else {
//							System.out.println("norm" + rn.id);							
//						}
						rnSeq.inc();
						if (isOnMainStreetNet) {
							numMainStreetRouteNodes++;
						}
					}
				}
			}
		}
	}

	public void markTrafficSignalsRouteNodes(Node n) {
		if (type == TYPE_ROUTECONTAINER) {
			if (t1 != null) {
				t1.markTrafficSignalsRouteNodes(n);
			}
			if (t2 != null) {
				t2.markTrafficSignalsRouteNodes(n);
			}
		} else if (type == TYPE_ROUTEDATA && bounds.isInOrAlmostIn(n.lat, n.lon)) {
			for (RouteNode rn : routeNodes) {
				if (MyMath.dist(n, rn.node) < 25) {
					if (!rn.node.isNeverTrafficSignalsRouteNode()) {
						rn.node.markAsTrafficSignalsRouteNode();
						numTrafficSignalRouteNodes++;
						// System.out.println(MyMath.dist(n, rn.node) + "Traffic Light Delay from traffic light" + 
						// n.toUrl() + " at " + rn.node.toUrl()); 
					} else {
						;
						//System.out.println("Route node became no traffic signal delay route node because it's at a tunnel or bridge: " + rn.node.toUrl());
					}
				}
			}
		}
	}

	/**
	 * Checks for coast lines and creates one (or more) sea polygons which extend(s) to
	 * the boundaries of this tile.
	 * If this tile is a container, it makes its children create these areas.
	 */
	public void generateSeaPolygon() {
		if (type == TYPE_CONTAINER) {
			if (t1 != null) {
				t1.generateSeaPolygon();
			}
			if (t2 != null) {
				t2.generateSeaPolygon();
			}
		} else if (type == TYPE_MAP) {
			System.out.println("info:  Tile zl=" + zl + ", fid=" + fid);
			SeaGenerator.generateSeaPolygon(this);
		}

		if (bounds.getFixPtSpan() <= 65000) {
			//System.out.println("info:  Checking for coast line: Tile zl=" + zl + ", fid=" + fid);
			SeaGenerator.generateSeaPolygon(this);
		}
	}	

	/**
	 * Prints the correct sequence of RouteNodes for debugging.
	 * @param deep
	 * @param maxDeep
	 */
	public void printHiLo(int deep, int maxDeep) {
		if (type == TYPE_ROUTECONTAINER) {
			if (deep < maxDeep) {
				System.out.print(":");
				if (t1 == null) {
					System.out.print("(empty)");
				} else {
					t1.printHiLo(deep + 1, maxDeep);
				}
				System.out.print("-");
				if (t2 == null) {
					System.out.print("(empty)");
				} else {
					t2.printHiLo(deep + 1, maxDeep);
				}
				System.out.print(":");
			}
			if (deep == maxDeep) {
				System.out.print("((C)" + idxMin + "/" + idxMax + ")");
			}
		} else if (type == TYPE_ROUTEDATA) {
			if (deep == maxDeep) {
			  System.out.print("((D" + fid + ")" + idxMin + "/" + idxMax + ")");
			}
		} else {
			System.out.print(" type(" + type + ")");
		}
	}
	
	/**
	 * Recalculates the idxMin and idxMax on RouteContainerTiles and RouteDataTiles
	 * @return
	 */
	public HiLo calcHiLo() {
		if (type == TYPE_ROUTEDATA) {
			HiLo retHiLo1 = new HiLo();
			if (routeNodes != null) {
				for (RouteNode rn: routeNodes) {
					retHiLo1.extend(rn.id);
				}
			}
			idxMin = retHiLo1.lo;
			idxMax = retHiLo1.hi;
			return retHiLo1;
		} else if (type == TYPE_ROUTECONTAINER) {
			HiLo retHiLo = new HiLo();
			if (t1 != null) {
				retHiLo.extend(t1.calcHiLo());
			}
			if (t2 != null) {
				retHiLo.extend(t2.calcHiLo());
			}
			idxMin = retHiLo.lo;
			idxMax = retHiLo.hi;
			return retHiLo;
		} else if (type == TYPE_EMPTY) {
			return new HiLo();
		} else {
			throw new Error("Wrong type of tile in " + this);
		}
	}

	/**
	 * @param path
	 * @throws IOException 
	 */
	public void writeConnections(String path, HashMap<Long, 
			TurnRestriction> turnRestrictions) throws IOException {
		if (t1 != null) {
			t1.writeConnections(path, turnRestrictions);
//			System.out.println("resolve T1 with " + idxMin + " to "+ idxMax);
		}
		if (t2 != null) {
			t2.writeConnections(path, turnRestrictions);
//			System.out.println("resolve T2 with " + idxMin + " to "+ idxMax);
		}
		if (routeNodes != null) {
			//System.out.println("Write Routenodes " + fid + " nodes " + 
			// routeNodes.size() +"  with " + idxMin + " to " + idxMax);
			FileOutputStream cfo = FileTools.createFileOutputStream(
					path + "/c/" + fid + ".d");
			DataOutputStream cds = new DataOutputStream(new BufferedOutputStream(cfo));
			FileOutputStream fo = FileTools.createFileOutputStream(
					path + "/t" + zl + "/" + fid + ".d");
			DataOutputStream nds = new DataOutputStream(new BufferedOutputStream(fo));
			// write out the number of mainStreetNet RouteNodes
			nds.writeShort(numMainStreetRouteNodes);
			// write out the number of normalStreetNet RouteNodes
			nds.writeShort(routeNodes.size() - numMainStreetRouteNodes);

			cds.writeInt(minConnectionId);

			short countTurnRestrictions[] = new short[2];
			TurnRestriction turnWrite = null;
			boolean hasTurnRestriction = false;
			boolean isOnMainStreetNet = false;
			int connected2 = 0;

			// count how many turn restrictions we will write for this tile
			// turn restrictions at mainStreetNet routeNodes are counted in the first loop, 
			// in the second loop the remaining ones
			for (int writeStreetNets = 0; writeStreetNets <= 1; writeStreetNets++) {
				for (RouteNode n : routeNodes) {
					isOnMainStreetNet = n.isOnMainStreetNet();
					if (writeStreetNets == 0 && isOnMainStreetNet
						||
						writeStreetNets > 0 && !isOnMainStreetNet
					) { 
						turnWrite = turnRestrictions.get(n.node.id);
						while (turnWrite != null) {
							if (turnWrite.isComplete()) {
								countTurnRestrictions[writeStreetNets]++;
							}
							turnWrite = turnWrite.nextTurnRestrictionAtThisNode;
						}
					}
				}
				// write counter how many turn restrictions are in this tile in mainstreet/normalNet
				nds.writeShort(countTurnRestrictions[writeStreetNets]);
			}

			// write mainStreetNet routeNodes / turn restrictions in the first loop, 
			// in the second loop the remaining ones
			for (int writeStreetNets = 0; writeStreetNets <= 1; writeStreetNets++) {								
				for (RouteNode n : routeNodes) {
					isOnMainStreetNet = n.isOnMainStreetNet();
					if (writeStreetNets == 0 && isOnMainStreetNet
						||
						writeStreetNets > 0 && !isOnMainStreetNet
					) { 
						nds.writeFloat(MyMath.degToRad(n.node.lat));
						nds.writeFloat(MyMath.degToRad(n.node.lon));
						//nds.writeInt(cds.size());
		
						hasTurnRestriction = false;
						turnWrite = turnRestrictions.get(n.node.id);
						while (turnWrite != null) {
							if (turnWrite.isComplete()) {
								countTurnRestrictions[writeStreetNets]++;
								hasTurnRestriction = true;
							}
							turnWrite = turnWrite.nextTurnRestrictionAtThisNode;
						}
						connected2 = n.getConnected().length;
						if (hasTurnRestriction) {
							// Write indicator that this route node has turn restrictions attached
							connected2 |= RouteNode.CS_FLAG_HASTURNRESTRICTIONS;
						}
						if (n.node.isTrafficSignalsRouteNode()) {
							// Write indicator that this route node is at traffic lights
							connected2 |= RouteNode.CS_FLAG_TRAFFICSIGNALS_ROUTENODE;
						}
						nds.writeByte((byte) connected2);					
		
						byte routeNodeWayFlags = 0;
						for (Connection c : n.getConnected()) {
							minConnectionId ++;
							routeNodeWayFlags |= c.connTravelModes;
							cds.writeInt(c.to.id);
							// set or clear flag for additional byte (connTravelModes is transferred from wayTravelMode were this is Connection.CONNTYPE_TOLLROAD, so clear it if not required) 
							c.connTravelModes |= Connection.CONNTYPE_CONNTRAVELMODES_ADDITIONAL_BYTE;
							if (c.connTravelModes2 == 0) {
								c.connTravelModes ^= Connection.CONNTYPE_CONNTRAVELMODES_ADDITIONAL_BYTE;
							}
							// write out wayTravelModes flag
							cds.writeByte(c.connTravelModes);
							if (c.connTravelModes2 != 0) {
								cds.writeByte(c.connTravelModes2);				
							}

							for (int i = 0; i < TravelModes.travelModeCount; i++) {
								// only store times for available travel modes of the connection
								if ( (c.connTravelModes & (1 << i)) != 0 ) {
									/**
									 * If we can't fit the values into short,
									 * we write an int. In order for the other
									 * side to know if we wrote an int or a short,
									 * we encode the length in the top most (sign) bit
									 */
									int time = c.times[i];
									if (time > Short.MAX_VALUE) {
										cds.writeInt(-1 * time);
									} else {
										cds.writeShort((short) time);
									}
								}
							}
							if (c.length > Short.MAX_VALUE) {
								cds.writeInt(-1 * c.length);
							} else {
								cds.writeShort((short) c.length);
							}
							
							cds.writeByte(c.startBearing);
							cds.writeByte(c.endBearing);
						}
						// count in which travel modes route node is used
						for (int i = 0; i < TravelModes.travelModeCount; i++) {
							if ( (routeNodeWayFlags & (1<<i)) != 0) {
								TravelModes.getTravelMode(i).numRouteNodes++;
							}
						}
					} // end of street net condition
				} // end of routeNodes loop
								
				// attach turn restrictions at the end of the mainstreet / normal street node data
				for (RouteNode n : routeNodes) {
					isOnMainStreetNet = n.isOnMainStreetNet();
					if (writeStreetNets == 0 && isOnMainStreetNet
						||
						writeStreetNets > 0 && !isOnMainStreetNet
					) { 
						turnWrite = turnRestrictions.get(n.node.id);
						while (turnWrite != null) {
							if (turnWrite.isComplete()) {					
								nds.writeInt(turnWrite.viaRouteNode.id);
								// just a prevention against renumbered RouteNodes
								if (turnWrite.viaRouteNode.id != n.id) {
									System.out.println("RouteNode ID mismatch for turn restrictions");
								}
								nds.writeInt(turnWrite.fromRouteNode.id);
								nds.writeInt(turnWrite.toRouteNode.id);
								nds.writeByte(turnWrite.affectedTravelModes);
								nds.writeByte(turnWrite.flags);
								if (turnWrite.isViaTypeWay()) {
									nds.writeByte(turnWrite.additionalViaRouteNodes.length);
									for (RouteNode rn:turnWrite.additionalViaRouteNodes) {
										nds.writeInt(rn.id);
									}
								}
							}
							// System.out.println(turnWrite.toString(OxParser.getWayHashMap()));
							turnWrite = turnWrite.nextTurnRestrictionAtThisNode;
						}
					} // end of street net condition for turn restrictions
				} // end of routeNodes loop for turn restrictions								
			} // end of writeStreetNets loop
			/**
			 * Write a special marker, so that we can detect if something
			 * went wrong with decoding the variable length encoding
			 */
			cds.writeInt(0xdeadbeaf);			
			
			nds.close();
			cds.close();
		}
	}
}
