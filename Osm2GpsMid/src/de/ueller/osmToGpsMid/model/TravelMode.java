/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750
 * 
 */

package de.ueller.osmToGpsMid.model;

import java.util.Vector;

public class TravelMode {
	/**
	 * e. g. motorcar, bicycle, etc.
	 */
	private	String	travelModeName;	
	public short maxPrepareMeters;
	public short maxInMeters;
	public long numRouteNodes=0;
	public long numConnections=0;
	public long numOneWayConnections=0;
	
	private Vector<RouteAccessRestriction> routeAccessRestrictions;

	public TravelMode(String name) {
		travelModeName = name;
		routeAccessRestrictions = new Vector<RouteAccessRestriction>();
	}
	
	public String getName() {
		return travelModeName;
	}	

	public Vector<RouteAccessRestriction> getRouteAccessRestrictions() {
		return routeAccessRestrictions;
	}	

	public String toString() {
		return " " + travelModeName + ": " + numRouteNodes + " nodes / " + numConnections + " dual connections / " + numOneWayConnections + " oneway connections";
	}
}
