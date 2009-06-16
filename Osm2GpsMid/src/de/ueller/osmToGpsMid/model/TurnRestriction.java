/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750 (at) users (dot) sourceforge (dot) net
 * 
 */

package de.ueller.osmToGpsMid.model;

import java.util.HashMap;

import de.ueller.osmToGpsMid.model.Relation;

public class TurnRestriction {
	public long fromWayRef;
	public long toWayRef;
	public int viaRouteNodeId;
	public int fromRouteNodeId;
	public int toRouteNodeId;
	public byte flags = 0;
	public byte affectedTravelModes = 0x01; // TODO: make configurable for which travel modes the turn restriction applies
	public String restrictionType; // stored only for debugging
	public TurnRestriction nextTurnRestrictionAtThisNode = null;
	
	public final static byte IS_ONLY_TYPE_RESTRICTION = 1;

	public TurnRestriction(Relation relation) {
		for (Member m : relation.members) {
			switch (m.getRole()) {
				case Member.ROLE_FROM: {							
					fromWayRef = m.getRef();
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
	}	

	public String toString(HashMap<Long,Way> wayHashMap) {
		return restrictionType + " from " + getWayNameFromRefId(wayHashMap, fromWayRef) + " into " + getWayNameFromRefId(wayHashMap, toWayRef);  
	}

	public String getWayNameFromRefId(HashMap<Long,Way> wayHashMap, long wayRef) {
		String name = "?";
		Way w = wayHashMap.get(wayRef);
		if (w != null) {
			String s = w.getName();
			if (s != null) {
				name = s;
			}
		}
		return name;
	}
	
}
