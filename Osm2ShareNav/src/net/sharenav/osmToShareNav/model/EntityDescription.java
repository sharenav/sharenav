/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.model;

import java.util.List;

public class EntityDescription {
	public String	description;
	public String	image;
	public String	key;
	public String	value;
	public List<ConditionTuple> specialisation;
	// polish.api.bigstyles
	public short	typeNum;
	public String	nameKey;
	public String	nameFallbackKey;
	public String	helperTag;
	public String	houseNumberMatchTag;
	public byte		rulePriority;
	public String	searchIcon;
	public int		minEntityScale;
	public int		minTextScale;
	public boolean	hideable;
	public boolean	alert;
	public boolean	clickable;
	public boolean	houseNumberIndex;
	
	public String toString() {
		return "Desc: " + description + "; " + key + "=" + value + " nametag: " + nameKey + " or " + nameFallbackKey
		    + " Helpertag: " + helperTag + " housenumber match tag: " + houseNumberMatchTag
		    + " Scale: " + minEntityScale + " hideable: " + hideable + " houseNumberIndex: " + houseNumberIndex;
	}
}
