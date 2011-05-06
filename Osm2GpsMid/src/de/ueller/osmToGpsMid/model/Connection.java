/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

/**
 * @author hmueller
 *
 */
public class Connection {
	/** contains one entry for each available travel mode for the way */
	public int times[];
	public int length;
	public RouteNode to;
	// only for debuging
	public RouteNode from;
	public byte startBearing=0;
	public byte endBearing=0;
	public byte connTravelModes=0;
	
	// the upper bits of connTravelModes are used to indicate special informations about the connection rather than which travelModes are allowed
	public static final int CONNTYPE_MAINSTREET_NET = 128;
	public static final int CONNTYPE_MOTORWAY = 64;
	public static final int CONNTYPE_TRUNK_OR_PRIMARY = 32;
	public static final int CONNTYPE_TOLLROAD = 16;
	
	public Connection(RouteNode to, int dist, int times[], byte bs, byte be, Way w) {
		super();
		this.to = to;
		length = dist;
		this.times = times;
		connTravelModes = w.wayTravelModes;
		startBearing=bs;
		endBearing=be;
		if ( (w.wayTravelModes & Connection.CONNTYPE_MOTORWAY) > 0 ) {
			TravelModes.numMotorwayConnections++;
		}
		if ( (w.wayTravelModes & Connection.CONNTYPE_TRUNK_OR_PRIMARY) > 0 ) {
			TravelModes.numTrunkOrPrimaryConnections++;
		}
		if ( (w.wayTravelModes & Connection.CONNTYPE_MAINSTREET_NET) > 0 ) {
			TravelModes.numMainStreetNetConnections++;
		}
		if ( (w.wayTravelModes & Connection.CONNTYPE_TOLLROAD) > 0 ) {
			TravelModes.numTollRoadConnections++;
		}
	}

	public String printTurn(Connection last) {
		long cost;
		String turnString;
		int turn = (last.endBearing-startBearing)*2;
		int adTurn=Math.abs(turn);
		if (adTurn > 150){
			cost=20;
			turnString="wende ";
		} else if (adTurn > 120){
			cost=15;
			turnString="scharf ";
		} else if (adTurn > 60){
			cost=10;
			turnString="";
		} else if (adTurn > 30){
			cost=5;
			turnString="halb ";
		} else {
			cost=0;
			turnString="gerade ";			
		}
		if (cost==0){
			return("gerade aus für " + length/10 + "m");
		} else {
			return (turnString + ((turn < 0)?"rechts ":"links ") + adTurn + " Grad dann " + (length/10)  +  "m der Straße folgen");
		}
	}


}
