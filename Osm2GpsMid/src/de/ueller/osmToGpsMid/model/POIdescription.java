/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.List;

/**
 * @author kai
 *
 */
public class POIdescription {
	public String	description;
	public String	key;
	public String	value;
	public List<ConditionTuple> specialisation;
	public byte		typeNum;
	public String	nameKey;
	public String	nameFallbackKey;
	public String	image;
	public String   searchIcon;
	public int		minTextScale;
	public int		minImageScale;
	public int 		textColor;
	public boolean	imageCenteredOnNode;
	public byte     rulePriority;
	public boolean	hideable;
	
	public String toString() {
		return "Desc: " + description + "; " + key + "=" + value + " nametag: " + nameKey + " or " + nameFallbackKey + " Scale: " + minImageScale + " hideable: " + hideable;
	}
}
