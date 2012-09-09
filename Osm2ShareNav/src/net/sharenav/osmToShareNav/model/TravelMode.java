/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750
 * 
 */

package net.sharenav.osmToShareNav.model;

import java.util.Vector;

public class TravelMode {
	/**
	 * e. g. motorcar, bicycle, etc.
	 */
	private	String	travelModeName;	
	public short maxPrepareMeters;
	public short maxInMeters;
	/** maximum speed in km/h estimated by the route calculation */
	public short maxEstimationSpeed = 0;
	public long numRouteNodes = 0;
	public long numAreaCrossConnections = 0;
	public long numDualConnections = 0;
	public long numOneWayConnections = 0;
	public long numBicycleOppositeConnections = 0;
	public byte travelModeFlags = 0;
	/** Flag if this travelMode is specified in the style-file */
	public boolean routeModeDefined = false;
	public long numTollRoadConnections=0;

	public final static byte AGAINST_ALL_ONEWAYS = 1;
	public final static byte BICYLE_OPPOSITE_EXCEPTIONS = 2;	
	public final static byte WITH_TURN_RESTRICTIONS = 4;
	public final static byte MAINSTREET_NET_FOR_LARGE_ROUTES = 8;
	
	private Vector<RouteAccessRestriction> routeAccessRestrictions;
	private Vector<TollRule> tollRules;

	public TravelMode(String name) {
		travelModeName = name;
		routeAccessRestrictions = new Vector<RouteAccessRestriction>();
		tollRules = new Vector<TollRule>();
	}
	
	public String getName() {
		return travelModeName;
	}	

	public Vector<RouteAccessRestriction> getRouteAccessRestrictions() {
		return routeAccessRestrictions;
	}	

	public Vector<TollRule> getTollRules() {
		return tollRules;
	}	

	public String toString() {
		return "  " + travelModeName + ": nodes: " + numRouteNodes + ", connections: " +
				numDualConnections + " dual (" + 
				numAreaCrossConnections + " crossarea" +
				(numBicycleOppositeConnections != 0 ? (" / " + 
						numBicycleOppositeConnections + " opposite for bicycles") : "") +
				") / " + numOneWayConnections + " oneway";
	}
}
