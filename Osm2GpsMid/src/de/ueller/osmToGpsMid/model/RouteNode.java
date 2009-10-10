/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.ArrayList;

/**
 * @author hmueller
 *
 */
public class RouteNode {
	public Node node;
	public ArrayList<Connection> connected=new ArrayList<Connection>();
	public ArrayList<Connection> connectedFrom=new ArrayList<Connection>();
	public int id;
	
	/* the upper flags of consize are used to indicate special informations about the node
	 * we don't have an explicit conSize field in Osm2GpsMid, however this is written out in the Tile class for GpsMid
	 */
	public static final int CS_MASK_CONNECTEDLINECOUNT = 0x3F;
	public static final int CS_FLAG_HASTURNRESTRICTIONS = 0x80;
	public static final int CS_FLAG_TRAFFICSIGNALS_ROUTENODE = 0x40;

	
//	public float g; // total cost 
//	public float h; //heuristic
//	public float f; //sum of g and h; 
	public RouteNode(Node n){
		node=n;
	}
	public String toString(){
		return ("RouteNode id=" + id+"(" + node.renumberdId + ")");
	}
	
	public boolean isOnMainStreetNet() {
		for (Connection c: connected) {
			if ( (c.connTravelModes & Connection.CONNTYPE_MAINSTREET_NET) > 0) {
				return true;
			}
		}
		for (Connection c: connectedFrom) {
			if ( (c.connTravelModes & Connection.CONNTYPE_MAINSTREET_NET) > 0) {
				return true;
			}
		}
		return false;
	}
	
}
