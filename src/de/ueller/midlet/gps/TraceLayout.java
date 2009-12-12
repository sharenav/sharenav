/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Legend;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.tile.PaintContext;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;


public class TraceLayout extends LayoutManager {
	public static final int TITLEBAR = 0;
	public static final int POINT_OF_COMPASS = 1;
	public static final int SOLUTION = 2;
	public static final int RECORDED_COUNT = 3;
	public static final int CELLID = 4;
	public static final int AUDIOREC = 5;
	public static final int WAYNAME = 6;
	public static final int ROUTE_INTO = 7;
	public static final int ROUTE_INSTRUCTION = 8;
	public static final int ROUTE_OFFROUTE = 9;
	public static final int ROUTE_DISTANCE = 10;
	public static final int ROUTE_DURATION = 11;
	public static final int SCALEBAR = 12;
	public static final int SPEEDING_SIGN = 13;
	public static final int SPEED_CURRENT = 14;
	public static final int ZOOM_IN = 15;
	public static final int ZOOM_OUT = 16;
	public static final int RECENTER_GPS = 17;
	public static final int SHOW_DEST = 18;
	public static final int ALTITUDE = 19;
	public static final int CURRENT_TIME = 20;
	public static final int ETA = 21;
	public static final int ELE_COUNT = 22;

	// special element ids
	public static final byte SE_SCALEBAR = 1;
	public static final byte SE_SPEEDING_SIGN = 2;
	
	public boolean usingVerticalLayout = false;
	
	public LayoutElement ele[] = new LayoutElement[ELE_COUNT];
	
	// variables for scale bar
	private int scalePx = 0;
	private float scale;
	
	// variables for speeding sign
	private int char0Width = 0;
	private int char0Height = 0;
	private int speedingSignWidth = 0;
	private String sOldSpeed = "";
	
	public TraceLayout(int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY);
		
		for (int i=0; i<ELE_COUNT; i++){
			ele[i] = new LayoutElement(this);
		}
		
		if ( maxX - minX < (maxY - minY) * 2 ) {
			createHorizontalLayout();
			usingVerticalLayout = false;
		} else {
			createVerticalLayout();
			usingVerticalLayout = true;
		}
		
		validate();		
	}
	
	/*
	 * layout for most mobiles
	 */
	private void createHorizontalLayout() { 
		LayoutElement e;
		e = ele[TITLEBAR]; addElement(e, 
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_TOP |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_TITLEBAR_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_TITLEBAR_TEXT]);
		
		e = ele[SCALEBAR]; addElement(e, 
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setAdditionalOffsX(10);
		e.setAdditionalOffsY(8);
		e.setVRelative(ele[TITLEBAR]);
		e.setSpecialElementID(SE_SCALEBAR);
		e.setActionID(Trace.MAPFEATURES_CMD);

		e = ele[POINT_OF_COMPASS]; addElement(e, 
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX
		);	
		e.setVRelative(ele[TITLEBAR]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_COMPASS_DIRECTION_BACKGROUND]);
		e.setActionID(Trace.MANUAL_ROTATION_MODE_CMD);
	
		e = ele[SOLUTION]; addElement(e, 
			LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setColor(Legend.COLORS[Legend.COLOR_MAP_TEXT]);
		e.setAdditionalOffsX(-1);
		e.setVRelative(ele[TITLEBAR]);

		e = ele[ALTITUDE]; addElement(e, 
				LayoutElement.FLAG_HALIGN_LEFTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_SMALL
			);
			e.setColor(Legend.COLORS[Legend.COLOR_MAP_TEXT]);
			e.setAdditionalOffsX(-8);
			e.setHRelative(ele[SOLUTION]);		
			e.setVRelative(ele[TITLEBAR]);		
		
		e = ele[RECORDED_COUNT]; addElement(e, 
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setVRelative(ele[SOLUTION]);
	
		e = ele[CELLID]; addElement(e, 
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setVRelative(ele[RECORDED_COUNT]);
	
		e = ele[AUDIOREC]; addElement(e, 
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setVRelative(ele[CELLID]);
		
		e = ele[WAYNAME]; addElement(e, 
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_BOTTOM |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH |
			LayoutElement.FLAG_RESERVE_SPACE
		);
		e.setColor(Legend.COLORS[Legend.COLOR_WAYNAME_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_WAYNAME_BACKGROUND]);
		e.setActionID(Trace.ICON_MENU);
		
		e = ele[ROUTE_INTO]; addElement(e, 
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM | LayoutElement.FLAG_FONT_BOLD |  
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);
		e.setVRelative(ele[WAYNAME]);
	
		e = ele[ROUTE_INSTRUCTION]; addElement(e, 
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM | LayoutElement.FLAG_FONT_BOLD |  
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);
		e.setVRelative(ele[ROUTE_INTO]);		
		
		e = ele[ROUTE_DISTANCE]; addElement(e, 
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_RI_DISTANCE_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_RI_DISTANCE_TEXT]);
		e.setVRelative(ele[ROUTE_INSTRUCTION]);			
		
		e = ele[SPEED_CURRENT]; addElement(e, 
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM |
				LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_SPEED_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_SPEED_TEXT]);
		e.setVRelative(ele[ROUTE_DISTANCE]);		
		
		e = ele[CURRENT_TIME]; addElement(e, 
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM |
				LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_CLOCK_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_CLOCK_TEXT]);
		e.setAdditionalOffsX(1); // FIXME: This should not be necessary to be exactly right aligned on the display
		e.setVRelative(ele[ROUTE_INSTRUCTION]);		
		e.setActionID(Trace.TOGGLE_BACKLIGHT_CMD);

		e = ele[ETA]; addElement(e, 
				LayoutElement.FLAG_HALIGN_LEFTTO_RELATIVE | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM |
				LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_RI_ETA_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_RI_ETA_TEXT]);
		e.setVRelative(ele[ROUTE_INSTRUCTION]);	
		e.setHRelative(ele[CURRENT_TIME]);	
		e.setAdditionalOffsX(-2);
		
		e = ele[ROUTE_OFFROUTE]; addElement(e, 
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_SMALL  
			);
		e.setColor(Legend.COLORS[Legend.COLOR_RI_OFF_DISTANCE_TEXT]);
		e.setVRelative(ele[CURRENT_TIME]);		
		
		e = ele[SPEEDING_SIGN]; addElement(e, 
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_LARGE
		);
		e.setSpecialElementID(SE_SPEEDING_SIGN);
		e.setAdditionalOffsY(-5);
		e.setVRelative(ele[SPEED_CURRENT]);	

		e = ele[ZOOM_OUT]; addElement(e, 
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND | LayoutElement.FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT |
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH | LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
		);
		e.setWidthPercent(150);
		e.setHeightPercent(150);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.ZOOM_OUT_CMD);
		
		e = ele[ZOOM_IN]; addElement(e, 
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH | LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
		);
		e.setWidthPercent(150);
		e.setHeightPercent(150);
		e.setVRelative(ele[ZOOM_OUT]);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.ZOOM_IN_CMD);

		e = ele[SHOW_DEST]; addElement(e, 
				LayoutElement.FLAG_HALIGN_LEFTTO_RELATIVE | LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND | LayoutElement.FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT |
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH | LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
		);
		e.setWidthPercent(90);
		e.setHeightPercent(150);
		e.setHRelative(ele[ZOOM_OUT]);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.SHOW_DEST_CMD);
		
		e = ele[RECENTER_GPS]; addElement(e, 
				LayoutElement.FLAG_HALIGN_LEFTTO_RELATIVE | LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH | LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
		);
		e.setWidthPercent(90);
		e.setHeightPercent(150);
		e.setHRelative(ele[ZOOM_IN]);
		e.setVRelative(ele[SHOW_DEST]);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.RECENTER_GPS_CMD);

	}

	/*
	 * layout for mobiles with very wide displays like Nokia E90
	 */
	private void createVerticalLayout() { 
		// TODO: create vertical layout - currently this layout is still the same as the horizontal layout
		createHorizontalLayout();
	}
	
	protected void drawSpecialElement(Graphics g, byte id, String text, int left, int top) {
		switch(id) {
			case SE_SCALEBAR:
				showScale(g, left, top);
				break;		
			case SE_SPEEDING_SIGN:
				showSpeedingSign(g, text, left, top);
				break;		
		}
	}
	
	protected int getSpecialElementWidth(byte id, String text, Font font) {
		switch(id) {
		case SE_SCALEBAR:
			return scalePx;
		case SE_SPEEDING_SIGN:
			return getSpeedingSignWidth(font, text); 
		}
		return 0;
	}
	
	protected int getSpecialElementHeight(byte id, int fontHeight) {
		switch(id) {
			case SE_SCALEBAR:
				return fontHeight + 4;
			case SE_SPEEDING_SIGN:
				return speedingSignWidth;
		}
		return 0;
	}
	
	
	/**
	 * Draws a map scale onto screen.
	 * This calculation is currently horribly
	 * inefficient. There must be a better way
	 * than this.
	 * 
	 * @param pc Paint context for drawing
	 */
	public void showScale(Graphics g, int left, int top) {
		//Draw the scale bar
		g.setColor(Legend.COLORS[Legend.COLOR_SCALEBAR]);
		g.setStrokeStyle(Graphics.SOLID);
		int right = left + scalePx;
		g.drawLine(left, top + 2, right, top + 2);
		g.drawLine(left, top + 3, right, top + 3); //double line width
		g.drawLine(left, top, left, top + 5);
		g.drawLine(left + scalePx, top, right, top + 5);
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
			if (scale > 1609.344) {
				g.drawString(Integer.toString((int)(scale/1609.344f + 0.5)) + "mi", left + scalePx/2, top + 4, Graphics.HCENTER | Graphics.TOP);
			} else {
				g.drawString(Integer.toString((int)(scale/0.9144 + 0.5)) + "yd", left + scalePx/2, top + 4, Graphics.HCENTER | Graphics.TOP);
			}
		} else {
			if (scale > 1000) {
				g.drawString(Integer.toString((int)(scale/1000.0f)) + "km", left + scalePx/2, top + 4, Graphics.HCENTER | Graphics.TOP);
			} else {
				g.drawString(Integer.toString((int)scale) + "m", left + scalePx/2, top + 4, Graphics.HCENTER | Graphics.TOP);
			}
		}
		
	}
	
	public void calcScaleBarWidth(PaintContext pc) {
		Node n1 = new Node();
		Node n2 = new Node();
		
		//Calculate the lat and lon coordinates of two
		//points that are 1/7th of the screen width apart
		int basePx = (pc.xSize / 7);
		pc.getP().inverse(10, 10, n1);
		pc.getP().inverse(10 + basePx, 10, n2);
		
		//Calculate the distance between them in meters
		float d = ProjMath.getDistance(n1, n2);
		float conv = 1.0f;
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
			
			if (d > 1609.344) {
				conv = 1609.344f;
				
			} else {
				conv = 0.9144f;
			}
		}
		//round this distance up to the nearest 5 or 10
		int ordMag = (int)(MoreMath.log((d/conv))/MoreMath.log(10.0f));
		if ((d/conv) < 2.5*MoreMath.pow(10,ordMag)) {
			scale = 2.5f*MoreMath.pow(10,ordMag) * conv;
		} else if ((d/conv) < 5*MoreMath.pow(10,ordMag)) {
			scale = 5*MoreMath.pow(10,ordMag) * conv;
		} else {
			scale = 10*MoreMath.pow(10,ordMag) * conv;
		}

		//Calculate how many pixels this distance is apart
		//The scale/d factor should be between 1 and 2.5
		//due to rounding
		scalePx = (int)(((float)basePx)*scale/d);
	}
	
	
	private int getSpeedingSignWidth(Font font, String sSpeed) {
		if (sSpeed.length() != sOldSpeed.length()) {
			speedingSignWidth = font.stringWidth(sSpeed);
			char0Width = font.charWidth('0');
			char0Height = font.getHeight();
			speedingSignWidth += char0Width * 4;
			sOldSpeed = sSpeed;
		}
		return speedingSignWidth;
	}
	
	private void showSpeedingSign(Graphics g, String sMaxSpeed, int left, int top) {
		g.setColor(Legend.COLORS[Legend.COLOR_SPEEDING_SIGN_BORDER]);
		g.fillArc(left, top, speedingSignWidth, speedingSignWidth, 0, 360);
		g.setColor(Legend.COLORS[Legend.COLOR_SPEEDING_SIGN_INNER]);
		g.fillArc(left + char0Width, top + char0Width, speedingSignWidth - (char0Width * 2), speedingSignWidth - (char0Width * 2), 0, 360);
		g.setColor(Legend.COLORS[Legend.COLOR_SPEEDING_SIGN_TEXT]);
		g.drawString(sMaxSpeed, left + speedingSignWidth/2, top + speedingSignWidth/2 - (char0Height / 2), Graphics.TOP | Graphics.HCENTER);
	}
	
}

 	  	 
