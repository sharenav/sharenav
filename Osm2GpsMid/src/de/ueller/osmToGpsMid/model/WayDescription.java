/**
 * OSM2GpsMid 
 *  
 *
 *
 * Copyright (C) 2008
 */
package de.ueller.osmToGpsMid.model;

import java.util.List;


public class WayDescription extends EntityDescription{
	public int		minOnewayArrowScale;
	public int		minDescriptionScale;
	public int		lineColor;
	public boolean	lineStyleDashed; //
	public int		boardedColor;
	public boolean	isArea;
	public int		wayWidth;
	public boolean  routable;
	public int		typicalSpeed;
	public int		noWaysOfType;
	public byte		forceToLayer;
	
	public WayDescription() {
		lineStyleDashed = false; //TODO: This needs to be corrected to Graphics.SOLID
		boardedColor = 0;
		isArea = false;
		wayWidth = 2;
		routable = false;
		typicalSpeed = 60;
		rulePriority = 0;
	}
	
	
}
