/**
 * OSM2ShareNav 
 *  
 *
 *
 * Copyright (C) 2008
 */
package net.sharenav.osmToShareNav.model;

import net.sharenav.osmToShareNav.Configuration;
import java.util.List;


public class WayDescription extends EntityDescription{
	public int		minOnewayArrowScale;
	public int		minDescriptionScale;
	public int		lineColor;
	public int		lineColorAtNight = -1;
	public int		wayDescFlags;
	public int		boardedColor;
	public int		boardedColorAtNight = -1;
	public boolean	isArea;
	public boolean	ignoreOsmAreaTag;
	public boolean	showNameAsForArea;
	public int		wayWidth;
	/** Up to 4 travel Modes (motorcar, bicycle, etc.) supported by this WayDescription (1 bit per travel mode)
	 *  The upper 4 bits equal to Connection.CONNTYPE_* flags
	 */
	public byte		wayDescTravelModes;
	/** typical speed of this WayDescription for up to MAXTRAVELMODES travel modes */
	public float		typicalSpeed[] = new float[TravelModes.MAXTRAVELMODES];
	public int		noWaysOfType;
	public byte		forceToLayer;
	
	// line styles
	public final static int WDFLAG_LINESTYLE_SOLID = 0x00;  // same as Graphics.SOLID
	public final static int WDFLAG_LINESTYLE_DOTTED = 0x01; // same as Graphics.DOTTED;
	public final static int WDFLAG_LINESTYLE_RAIL = 0x02;
	public final static int WDFLAG_LINESTYLE_STEPS = 0x04;
	public final static int WDFLAG_LINESTYLE_POWERLINE = 0x08;
	public final static int WDFLAG_BUILDING = 0x10;
	public final static int WDFLAG_HIGHWAY_LINK = 0x20;
	public final static int WDFLAG_MOTORWAY = 0x40;
	public final static int WDFLAG_MAINSTREET_NET = 0x80;
	
	public WayDescription() {
		wayDescFlags = WDFLAG_LINESTYLE_SOLID;
		boardedColor = 0;
		isArea = false;
		ignoreOsmAreaTag = false;
		showNameAsForArea = false;
		wayWidth = 2;
		wayDescTravelModes = 0;
		for (int i = 0; i < TravelModes.travelModeCount; i++) {
			typicalSpeed[i] = 5f;
		}
		rulePriority = 0;
	}
	
	public boolean isHighwayLink() {
		return (wayDescFlags & WDFLAG_HIGHWAY_LINK) > 0;
	}

	public boolean isMotorway() {
		return (wayDescFlags & WDFLAG_MOTORWAY) > 0;
	}
	
	public boolean isMainstreet() {
		return (wayDescFlags & WDFLAG_MAINSTREET_NET) > 0;		
	}
}
