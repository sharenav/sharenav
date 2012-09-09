package net.sharenav.sharenav.mapdata;


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
	public boolean	alert;
	public boolean	clickable;
	public byte		overviewMode;
	//#if polish.api.osm-editing
	public String[]	osmTags;
	//#endif

	public String toString() {
		return description;
	}
}
