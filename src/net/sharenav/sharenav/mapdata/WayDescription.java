package net.sharenav.sharenav.mapdata;

import javax.microedition.lcdui.Image;

public class WayDescription {
	public String description;
	public int maxScale;
	public int maxTextScale;
	public int maxOnewayArrowScale;
	public int maxDescriptionScale;
	public int lineColor;
	public int wayDescFlags;
	public int boardedColor;
	public boolean isArea;
	public byte wayWidth;
	public boolean hideable;
	public boolean	alert;
	public boolean	clickable;
	public byte overviewMode;
	public byte routeFlags;
	public Image	image;
	public Image	searchIcon;
	//#if polish.api.osm-editing
	public String[]	osmTags;
	//#endif
	
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
	public final static int WDFLAG_SEARCHICON_FROM_FILE = 0x100;
	
	public int getGraphicsLineStyle() {
		return wayDescFlags & 0x01;
	}
	
	public boolean isLineStyleRail() {
		return (wayDescFlags & WDFLAG_LINESTYLE_RAIL) > 0;
	}

	public boolean isLineStyleSteps() {
		return (wayDescFlags & WDFLAG_LINESTYLE_STEPS) > 0;
	}

	public boolean isLineStylePowerLine() {
		return (wayDescFlags & WDFLAG_LINESTYLE_POWERLINE) > 0;
	}

	public boolean isBuilding() {
		return (wayDescFlags & WDFLAG_BUILDING) > 0;
	}

	public boolean isHighwayLink() {
		return (wayDescFlags & WDFLAG_HIGHWAY_LINK) > 0;
	}

	public boolean isMotorway() {
		return (wayDescFlags & WDFLAG_MOTORWAY) > 0;
	}

	public boolean isMainstreetNet() {
		return (wayDescFlags & WDFLAG_MAINSTREET_NET) > 0;
	}

	public boolean hasSearchIconFromFile() {
		return (wayDescFlags & WDFLAG_SEARCHICON_FROM_FILE) > 0;
	}
	
}

