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
	public int used=0;
	public byte startBearing=0;
	public byte endBearing=0;
	public byte wayTravelModes=0;
	
	
	public static final int CONNTYPE_MOTORWAY = 64;
	public static final int CONNTYPE_MAINSTREET_NET = 32;
	
	public Connection(RouteNode to, int dist, int times[], byte bs, byte be, Way w) {
		super();
		this.to = to;
		length = dist;
		this.times = times;
		wayTravelModes = w.wayTravelModes;
		startBearing=bs;
		endBearing=be;
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
