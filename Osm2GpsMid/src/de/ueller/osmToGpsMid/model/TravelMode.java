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

public class TravelMode {
	/**
	 * e. g. motorway, bicycle, etc.
	 */
	private	String	travelModeName;	
	
	public TravelMode(String name) {
		travelModeName = name;
	}
	
	public String getName() {
		return travelModeName;
	}	
}
