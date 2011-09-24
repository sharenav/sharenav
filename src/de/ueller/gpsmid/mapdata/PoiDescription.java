package de.ueller.gpsmid.mapdata;


import javax.microedition.lcdui.Image;

public class PoiDescription {
	public String	description;
	public boolean	imageCenteredOnNode;
	public Image	image;
	public Image	searchIcon;
	public int		textColor;
	public int		maxTextScale;
	public int		maxImageScale;
	public boolean	hideable;
	public byte		overviewMode;
	//#if polish.api.osm-editing
	public String[]	osmTags;
	//#endif
}

