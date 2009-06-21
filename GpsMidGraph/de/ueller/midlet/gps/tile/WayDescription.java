package de.ueller.midlet.gps.tile;

public class WayDescription {
	public String description;
	public int maxScale;
	public int maxTextScale;
	public int maxOnewayArrowScale;
	public int maxDescriptionScale;
	public int lineColor;
	public int lineStyle;
	public int boardedColor;
	public boolean isArea;
	public byte wayWidth;
	public boolean hideable;
	public byte overviewMode;
	public byte routeFlags;
	//#if polish.api.osm-editing
	public String[]	osmTags;
	//#endif
	
	// line styles
	public final static int LINESTYLE_SOLID = 0x00;  // same as Graphics.SOLID
	public final static int LINESTYLE_DOTTED = 0x01; // same as Graphics.DOTTED;
	public final static int LINESTYLE_RAIL = 0x02;
	public final static int LINESTYLE_STEPS = 0x04;
	public final static int LINESTYLE_POWERLINE = 0x08;
	
	public int getGraphicsLineStyle() {
		return lineStyle & 0x01;
	}
	
	public boolean isLineStyleRail() {
		return (lineStyle & LINESTYLE_RAIL) > 0;
	}

	public boolean isLineStyleSteps() {
		return (lineStyle & LINESTYLE_STEPS) > 0;
	}

	public boolean isLineStylePowerLine() {
		return (lineStyle & LINESTYLE_POWERLINE) > 0;
	}
}

