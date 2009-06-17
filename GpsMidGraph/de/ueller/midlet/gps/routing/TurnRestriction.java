/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750 (at) users (dot) sourceforge (dot) net
 * 
 */

package de.ueller.midlet.gps.routing;


public class TurnRestriction {
	public int viaRouteNodeId;
	public int fromRouteNodeId;
	public int toRouteNodeId;
	public byte flags = 0;
	public byte affectedTravelModes = 0x01; // TODO: make configurable for which travel modes the turn restriction applies
	public TurnRestriction nextTurnRestrictionAtThisNode = null;
	
	public final static byte IS_ONLY_TYPE_RESTRICTION = 1;

	public TurnRestriction() {
	}	
	
	public boolean isOnlyTypeRestriction() {
		return (flags & IS_ONLY_TYPE_RESTRICTION) > 0;
	}
	
}
