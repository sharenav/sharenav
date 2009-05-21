/**
 * OSM2GpsMid 
 *  
 *
 *
 * Copyright (C) 2008
 */
package de.ueller.osmToGpsMid.model;

import de.ueller.osmToGpsMid.Configuration;
import java.util.List;


public class WayDescription extends EntityDescription{
	public int		minOnewayArrowScale;
	public int		minDescriptionScale;
	public int		lineColor;
	public boolean	lineStyleDashed; //
	public int		boardedColor;
	public boolean	isArea;
	public int		wayWidth;
	/** Travel Modes (motorcar, bicycle, etc.) supported by this WayDescription (1 bit per travel mode) */
	public byte		wayDescTravelModes;
	/** typical speed of this WayDescription for up to 8 travel modes */
	public int		typicalSpeed[] = new int[8];
	public int		noWaysOfType;
	public byte		forceToLayer;
	
	public WayDescription() {
		lineStyleDashed = false; //TODO: This needs to be corrected to Graphics.SOLID
		boardedColor = 0;
		isArea = false;
		wayWidth = 2;
		wayDescTravelModes = 0;
		for (int i = 0; i < TravelModes.travelModeCount; i++) {
			typicalSpeed[i] = 5;
		}
		rulePriority = 0;
	}
	
	
}
