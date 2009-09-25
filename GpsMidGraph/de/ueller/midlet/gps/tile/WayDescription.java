package de.ueller.midlet.gps.tile;

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
	public byte overviewMode;
	public byte routeFlags;
	public short typicalSpeed[];
	//#if polish.api.osm-editing
	public String[]	osmTags;
	//#endif
	
	// line styles
	public final static int WDFLAG_LINESTYLE_SOLID = 0x00;  // same as Graphics.SOLID
	public final static int WDFLAG_LINESTYLE_DOTTED = 0x01; // same as Graphics.DOTTED;
	public final static int WDFLAG_LINESTYLE_RAIL = 0x02;
	public final static int WDFLAG_LINESTYLE_STEPS = 0x04;
	public final static int WDFLAG_LINESTYLE_POWERLINE = 0x08;
	
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
}

