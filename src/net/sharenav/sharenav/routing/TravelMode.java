/**
 * This file is part of ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750
 * 
 */

package net.sharenav.sharenav.routing;

public class TravelMode {
	/**
	 * e. g. motorcar, bicycle, etc.
	 */
	public String	travelModeName;	
	public short maxPrepareMeters;
	public short maxInMeters;
	public short maxEstimationSpeed;
	public byte travelModeFlags = 0;
	
	public final static byte AGAINST_ALL_ONEWAYS = 1;
	public final static byte BICYLE_OPPOSITE_EXCEPTIONS = 2;	
	public final static byte WITH_TURN_RESTRICTIONS = 4;	
	public final static byte MAINSTREET_NET_FOR_LARGE_ROUTES = 8;
	
	public TravelMode() {
		
	}
	
	public boolean isWithTurnRestrictions() {
		return (travelModeFlags & WITH_TURN_RESTRICTIONS) > 0;
	}
	
	public boolean useMainStreetNetForLargeRoutes() {
		return (travelModeFlags & MAINSTREET_NET_FOR_LARGE_ROUTES) > 0;
	}
	
	
	public String getName() {
		return travelModeName;
	}	
}
