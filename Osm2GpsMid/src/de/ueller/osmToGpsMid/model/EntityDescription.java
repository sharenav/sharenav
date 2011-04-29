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

public class EntityDescription {
	public String	description;
	public String	image;
	public String	key;
	public String	value;
	public List<ConditionTuple> specialisation;
	public byte		typeNum;
	public String	nameKey;
	public String	nameFallbackKey;
	public String	helperTag;
	public byte		rulePriority;
	public String	searchIcon;
	public int		minEntityScale;
	public int		minTextScale;
	public boolean	hideable;
	public boolean	houseNumberIndex;
	
	public String toString() {
		return "Desc: " + description + "; " + key + "=" + value + " nametag: " + nameKey + " or " + nameFallbackKey + " Scale: " + minEntityScale + " hideable: " + hideable;
	}
}
