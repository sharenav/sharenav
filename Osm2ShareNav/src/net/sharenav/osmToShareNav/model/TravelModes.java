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

public class TravelModes {
	public static TravelMode travelModes[];
	public static int travelModeCount = 0;
	public static final int MAXTRAVELMODES = 4; // bit 4 is used for CONNTYPE_TOLLROAD, bit 5 is used for CONNTYPE_TRUNK_OR_PRIMARY, bit 6 for CONNTYPE_MOTORWAY, bit 7 for CONNTYPE_MAINSTREET_NET 
	public static byte applyTurnRestrictionsTravelModes = 0;
	
	public static long numMotorwayConnections=0;
	public static long numMainStreetNetConnections=0;
	public static long numTrunkOrPrimaryConnections=0;

	public static void stringToTravelModes(String modes) {
		travelModes = new TravelMode[MAXTRAVELMODES];
		travelModeCount = 0;
		String s[] = modes.split("[;,]", MAXTRAVELMODES);
		for (int i=0; i < s.length; i++ ) {
			add(s[i].trim());
		}
	}
	

	public static TravelMode getTravelMode(String modeName) {
		for (int i=0; i < travelModeCount; i++) {
			if (travelModes[i].getName().equalsIgnoreCase(modeName)) {
				return travelModes[i];
			}
		}
		return null;
	}

	public static TravelMode getTravelMode(int modeNr) {
		return travelModes[modeNr];
	}
	
	
	public static int getTravelModeNrByName(String modeName) {
		for (int i=0; i < travelModeCount; i++) {
			if (travelModes[i].getName().equalsIgnoreCase(modeName)) {
				return i;
			}
		}
		return -1;
	}
	
	private static void add(String modeName) {
		travelModes[travelModeCount]= new TravelMode(modeName);
		travelModeCount++;
	}
	
}
