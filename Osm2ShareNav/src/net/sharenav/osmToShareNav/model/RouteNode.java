/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.model;

/**
 * @author hmueller
 *
 */
public class RouteNode {
	public Node node;
	private Connection[] connected=new Connection[0];
	private Connection[] connectedFrom=new Connection[0];
	public int id;
	
	/* the upper flags of consize are used to indicate special informations about the node
	 * we don't have an explicit conSize field in Osm2ShareNav, however this is written out in the Tile class for ShareNav
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

	public Connection[] getConnected() {
		return connected;
	}

	public void addConnected(Connection connection) {
		Connection[] newConnected = new Connection[connected.length+1];
		for ( int i = 0; i < connected.length; i++)	{
			newConnected[i]=connected[i];
		}
		newConnected[newConnected.length-1]=connection;
		connected=newConnected;
	}

	public Connection[] getConnectedFrom() {
		return connectedFrom;
	}


	public void addConnectedFrom(Connection connectionFrom) {
		Connection[] newConnectedFrom = new Connection[connectedFrom.length+1];
		for ( int i = 0; i < connectedFrom.length; i++)	{
			newConnectedFrom[i]=connectedFrom[i];
		}
		newConnectedFrom[newConnectedFrom.length-1]=connectionFrom;
		connectedFrom=newConnectedFrom;
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
