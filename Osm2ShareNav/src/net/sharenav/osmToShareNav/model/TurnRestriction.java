/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750 (at) users (dot) sourceforge (dot) net
 * 
 */

package net.sharenav.osmToShareNav.model;

import java.util.HashMap;

import net.sharenav.osmToShareNav.model.Relation;

public class TurnRestriction {
	public long fromWayRef;
	public long toWayRef;
	/** The ref to the via way if VIA_TYPE_IS_WAY flag is set */  
	public long viaWayRef;
	/** the viaRouteNode is directly leading to the toWay */
	public RouteNode viaRouteNode;
	/** the additional Route Nodes on the via way if the via role is a way - the first entry is the one leading from the fromWay to the viaWay */	
	public RouteNode additionalViaRouteNodes[] = null;
	public float viaLat;
	public float viaLon;
	public RouteNode fromRouteNode;
	public RouteNode toRouteNode;
	public byte flags = 0;
	/** for which travel modes the turn restriction applies */
	public byte affectedTravelModes = 0;
	public String restrictionType; // stored only for debugging
	public TurnRestriction nextTurnRestrictionAtThisNode = null;
	
	public final static byte NO_LEFT_TURN = 0x01;
	public final static byte NO_RIGHT_TURN = 0x02;
	public final static byte NO_STRAIGHT_ON = 0x03;
	public final static byte NO_U_TURN = 0x04;
	public final static byte ONLY_LEFT_TURN = 0x05;
	public final static byte ONLY_RIGHT_TURN = 0x06;
	public final static byte ONLY_STRAIGHT_ON = 0x07;
	public final static byte VIA_TYPE_IS_WAY = 0x20;
	public final static byte IS_ONLY_TYPE_RESTRICTION = 0x40;

	
	public TurnRestriction(Relation relation) {
		for (Member m : relation.members) {
			switch (m.getRole()) {
				case Member.ROLE_FROM: {							
					fromWayRef = m.getRef();
					break;
				}
				case Member.ROLE_TO: {
					toWayRef = m.getRef();
					break;
				}
			}
		}
		restrictionType = relation.getAttribute("restriction").toLowerCase();
		if (restrictionType.startsWith("only_")) {
			flags |= IS_ONLY_TYPE_RESTRICTION;
		}
		if (restrictionType.equalsIgnoreCase("no_left_turn")) {
			flags += NO_LEFT_TURN;
		} else if (restrictionType.equalsIgnoreCase("no_right_turn")) {
			flags += NO_RIGHT_TURN;
		} else if (restrictionType.equalsIgnoreCase("no_straight_on")) {
			flags += NO_STRAIGHT_ON;
		} else if (restrictionType.equalsIgnoreCase("no_u_turn")) {
			flags += NO_U_TURN;
		} else if (restrictionType.equalsIgnoreCase("only_left_turn")) {
			flags += ONLY_LEFT_TURN;
		} else if (restrictionType.equalsIgnoreCase("only_right_turn")) {
			flags += ONLY_RIGHT_TURN;
		} else if (restrictionType.equalsIgnoreCase("only_straight_on")) {
			flags += ONLY_STRAIGHT_ON;
		}
		
	}	

	public String toString(HashMap<Long, Way> wayHashMap) {
		return restrictionType + " from '" + getWayNameFromRefId(wayHashMap, fromWayRef) + 
		"' (" + fromWayRef + ") into '" + getWayNameFromRefId(wayHashMap, toWayRef) + 
		"' (" + toWayRef + ")";  
	}

	public boolean isViaTypeWay() {
		return (flags & VIA_TYPE_IS_WAY) > 0;
	}
	
	public boolean isComplete() {
		return (viaRouteNode != null && fromRouteNode != null && toRouteNode != null);
	}

	public String getWayNameFromRefId(HashMap<Long,Way> wayHashMap, long wayRef) {
		String name = "?";
		Way w = wayHashMap.get(new Long(wayRef));
		if (w != null) {
			String s = w.getName();
			if (s != null) {
				name = s;
			}
		}
		return name;
	}
}
