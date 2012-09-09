/**
 * This file is part of ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750 (at) users (dot) sourceforge (dot) net
 * 
 */

package net.sharenav.sharenav.routing;


public class TurnRestriction {
	public int viaRouteNodeId;
	public int fromRouteNodeId;
	public int toRouteNodeId;
	public byte flags = 0;
	/** for which travel modes the turn restriction applies */
	public byte affectedTravelModes = 0;
	/** the route nodes on the via member if isViaTypeWay() */
	public int [] extraViaNodes = null;
	public TurnRestriction nextTurnRestrictionAtThisNode = null;
	
	public final static byte VIA_TYPE_IS_WAY = 0x20;
	public final static byte IS_ONLY_TYPE_RESTRICTION = 0x40;

	private static final String[] restrictionNames  =
	{ "?", "no_left_turn", "no_right_turn", "no_straight_on", "no_u_turn", "only_left_turn", "only_right_turn", "only_straight_on" };

	public TurnRestriction() {
	}	
	
	public boolean isOnlyTypeRestriction() {
		return (flags & IS_ONLY_TYPE_RESTRICTION) > 0;
	}

	public boolean isViaTypeWay() {
		return (flags & VIA_TYPE_IS_WAY) > 0;
	}
	
	public String getRestrictionType() {
		return restrictionNames[flags & 0x7];
	}
	
}
