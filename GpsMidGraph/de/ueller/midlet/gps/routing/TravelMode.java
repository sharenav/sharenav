/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  sk750
 * 
 */

package de.ueller.midlet.gps.routing;

public class TravelMode {
	/**
	 * e. g. motorcar, bicycle, etc.
	 */
	public String	travelModeName;	
	public short maxPrepareMeters;
	public short maxInMeters;
	public byte againstOneWayMode = 0;
	
	public final static byte AGAINST_ALL_ONEWAYS = 1;
	public final static byte BICYLE_OPPOSITE_EXCEPTIONS = 2;	

	
	public TravelMode() {
		
	}
	
	public String getName() {
		return travelModeName;
	}	
}
