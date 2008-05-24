/**
 * OSM2GpsMid 
 *  
 *
 *
 * Copyright (C) 2008
 */
package de.ueller.osmToGpsMid.model;

import java.awt.Graphics;


public class WayDescription {
	public String	description;
	public String	key;
	public String	value;
	public byte		typeNum;
	public String	nameKey;
	public String	nameFallbackKey;
	public int		minScale;
	public int		lineColor;	
	public boolean	lineStyleDashed; //
	public int		boardedColor;
	public boolean	isArea;
	public int		wayWidth;
	public boolean  routable;
	public int		typicalSpeed;
	public String   searchIcon;
	public int      noWaysOfType;
	public byte		forceToLayer;
	
	public WayDescription() {
		lineStyleDashed = false; //TODO: This needs to be corrected to Graphics.SOLID
		boardedColor = 0;
		isArea = false;
		wayWidth = 2;
		routable = false;
		typicalSpeed = 60;
	}
	
	public String toString() {
		return "Desc: " + description + "; " + key + "=" + value + " nametag: " + nameKey + " or " + nameFallbackKey + " Scale: " + minScale;
	}
}
