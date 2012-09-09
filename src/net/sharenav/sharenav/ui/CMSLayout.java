/*
 * ShareNav - Copyright (c) 2009 sk750 at users dot sourceforge dot net
 * ShareNav - Copyright (c) 2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net
 * derived from TraceLayout.java
 * See COPYING
 */

package net.sharenav.sharenav.ui;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.PaintContext;
import net.sharenav.midlet.iconmenu.LayoutElement;
import net.sharenav.midlet.iconmenu.LayoutManager;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class CMSLayout extends LayoutManager {
	public static final int TITLEBAR = 0;
	public static final int SCALEBAR = 1;
	public static final int POINT_OF_COMPASS = 2;
	public static final int SOLUTION = 3;
	public static final int ALTITUDE = 4;
	public static final int RECORDED_COUNT = 5;
	public static final int CELLID = 6;
	public static final int AUDIOREC = 7;
	public static final int WAYNAME = 8;
	public static final int ROUTE_INTO = 9;
	public static final int ROUTE_INSTRUCTION = 10;
	public static final int ROUTE_DISTANCE = 11;
	//public static final int ROUTE_DURATION = 10;
	public static final int SPEED_CURRENT = 12;
	public static final int CURRENT_TIME = 13;
	public static final int ETA = 14;
	public static final int ROUTE_OFFROUTE = 15;
	public static final int ZOOM_OUT = 16;
	public static final int ZOOM_IN = 17;
	public static final int SHOW_DEST = 18;
	public static final int RECENTER_GPS = 19;
	public static final int RECORDINGS = 20;
	public static final int SEARCH = 21;
	public static final int SPEEDING_SIGN = 22;
	public static final int REQUESTED_TILES = 23;
	public static final int TRAVEL_MODE = 24;
	public static final int ELE_COUNT = 25;

	// special element ids
	public static final byte SE_SCALEBAR = 1;
	public static final byte SE_SPEEDING_SIGN = 2;
	
	public static boolean bigOnScreenButtons = false;
	
	public LayoutElement ele[] = new LayoutElement[ELE_COUNT];
	
	// variables for scale bar
	private int scalePx = 0;
	private float scale;
	
	// variables for speeding sign
	private int char0Width = 0;
	private int char0Height = 0;
	private int speedingSignWidth = 0;
	private String sOldSpeed = "";
	
	
	public CMSLayout(int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY, Legend.COLORS[Legend.COLOR_MAP_TOUCHED_BUTTON_BACKGROUND]);
		
		for (int i = 0; i < ELE_COUNT; i++) {
			ele[i] = new LayoutElement(this);
		}
		
		if ( maxX - minX < (maxY - minY) * 3 / 2 ) {
			// portrait layout
			createLayout(true);
		} else {
			// landscape layout
			createLayout(false);
		}
		
		validate();
	}
	
	/**
	 * Layout
	 */
	private void createLayout(boolean isPortraitLayout) {
		LayoutElement e;
		e = ele[TITLEBAR]; addElement(e,
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_TOP |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_TITLEBAR_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_TITLEBAR_TEXT]);
		e.setActionID(Trace.ICON_MENU);
		
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
		e.setColor(Legend.COLORS[Legend.COLOR_COMPASS_DIRECTION_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_COMPASS_DIRECTION_BACKGROUND]);
		e.setActionID(Trace.MANUAL_ROTATION_MODE_CMD + (Trace.TOGGLE_MAP_PROJ_CMD << 8)
			      + (Trace.NORTH_UP_CMD << 16));
	
		e = ele[SOLUTION]; addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM | LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setColor(Legend.COLORS[Legend.COLOR_SOLUTION_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_SOLUTION_BACKGROUND]);
		e.setAdditionalOffsX(-1);
		e.setVRelative(ele[TITLEBAR]);
		e.setActionID((Trace.TOGGLE_GPS_CMD << 16) + (Trace.CELLID_LOCATION_CMD << 8));

		e = ele[ALTITUDE]; addElement(e,
				LayoutElement.FLAG_HALIGN_LEFTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_SMALL | LayoutElement.FLAG_BACKGROUND_BOX 
			);
			e.setColor(Legend.COLORS[Legend.COLOR_ALTITUDE_TEXT]);
			e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ALTITUDE_BACKGROUND]);
			e.setAdditionalOffsX(-8);
			e.setHRelative(ele[SOLUTION]);
			e.setVRelative(ele[TITLEBAR]);
		
		e = ele[RECORDED_COUNT]; addElement(e,
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM | LayoutElement.FLAG_BACKGROUND_BOX);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_RECORDING_BACKGROUND]);
		e.setAdditionalOffsX(-1);
		e.setVRelative(ele[SOLUTION]);
		e.setActionID(Trace.TOGGLE_RECORDING_SUSP_CMD << 16);
	
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
		e.setActionID(Trace.ICON_MENU + (Trace.SEARCH_CMD << 8));
		
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
		e.setActionID(Trace.ROUTING_TOGGLE_CMD + (Trace.ROUTING_START_CMD << 16) );
		
		e = ele[SPEED_CURRENT]; addElement(e,
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM |
				LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_SPEED_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_SPEED_TEXT]);
		e.setAdditionalOffsX(40);
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
		e.setActionID(Trace.TOGGLE_BACKLIGHT_CMD + (Trace.DATASCREEN_CMD << 8) + (Trace.TOGGLE_OVERLAY_CMD << 16));

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

		e = ele[ZOOM_OUT]; addElement(e,
				LayoutElement.FLAG_HALIGN_RIGHT |
				LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
				LayoutElement.FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT |
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_FONT_BOLD |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
		);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.ZOOM_OUT_CMD);
		
		e = ele[ZOOM_IN]; addElement(e,
				LayoutElement.FLAG_HALIGN_RIGHT |
				LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
				LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_FONT_BOLD |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
		);
		e.setVRelative(ele[ZOOM_OUT]);
		e.setAdditionalOffsY(-2);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.ZOOM_IN_CMD);

		e = ele[SHOW_DEST]; addElement(e,
					       LayoutElement.FLAG_HALIGN_LEFT |
					       LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
					       LayoutElement.FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT |
					       LayoutElement.FLAG_FONT_LARGE |
						   LayoutElement.FLAG_FONT_BOLD |
					       LayoutElement.FLAG_BACKGROUND_BORDER |
						   LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
					       LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH |
					       LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
					       );
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		// Please note: this action ID is also set in Trace
		e.setActionID(Trace.SHOW_DEST_CMD + (Trace.SET_DEST_CMD << 16) );
		
		e = ele[RECENTER_GPS]; addElement(e,
						  LayoutElement.FLAG_HALIGN_LEFT |
						  LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
						  LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
						  LayoutElement.FLAG_FONT_LARGE |
						  LayoutElement.FLAG_FONT_BOLD |
						  LayoutElement.FLAG_BACKGROUND_BORDER |
						  LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
						  LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH |
						  LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
						  );
		e.setVRelative(ele[SHOW_DEST]);
		e.setAdditionalOffsY(-2);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.RECENTER_GPS_CMD + (Trace.MANUAL_LOCATION_CMD << 8));
		
		e = ele[RECORDINGS]; 
		addElement(e,
				(isPortraitLayout ? LayoutElement.FLAG_HALIGN_LEFT : LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE) |
				LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
				LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |				
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_FONT_BOLD |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
				);
		if (isPortraitLayout) {		
			e.setVRelative(ele[SHOW_DEST]);
		} else {
			e.setVRelative(ele[RECENTER_GPS]);			
			e.setHRelative(ele[RECENTER_GPS]);
			e.setAdditionalOffsX(2);
		}
		e.setAdditionalOffsY(2);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.SAVE_WAYP_CMD + (Trace.TOGGLE_RECORDING_CMD << 16));

		e = ele[SEARCH]; 
		addElement(e,
				(isPortraitLayout ? LayoutElement.FLAG_HALIGN_LEFT : LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE) |
				LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
				LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |				
				LayoutElement.FLAG_FONT_LARGE |
				LayoutElement.FLAG_FONT_BOLD |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH |
				LayoutElement.FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT
				);
		if (isPortraitLayout) {
			e.setVRelative(ele[RECENTER_GPS]);
		} else {
			e.setVRelative(ele[SHOW_DEST]);
			e.setHRelative(ele[RECENTER_GPS]);			
			e.setAdditionalOffsX(2);
		}
		e.setAdditionalOffsY(-2);
		e.setColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON_TEXT]);
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ZOOM_BUTTON]);
		e.setActionID(Trace.SEARCH_CMD + (Trace.MANAGE_WAYP_CMD << 8) + (Trace.MANAGE_TRACKS_CMD << 16));
		
		
		e = ele[SPEEDING_SIGN]; addElement(e,
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_LARGE
		);
		e.setSpecialElementID(SE_SPEEDING_SIGN);
		e.setAdditionalOffsY(-5);
		e.setVRelative(ele[SPEED_CURRENT]);		
		
		e = ele[REQUESTED_TILES];
		addElement(e, LayoutElement.FLAG_HALIGN_LEFT |
				LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_LARGE);
		e.setAdditionalOffsY(5);
		e.setVRelative(ele[SCALEBAR]);

		e = ele[TRAVEL_MODE];
		addElement(e, 
				   LayoutElement.FLAG_HALIGN_LEFTTO_RELATIVE | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM |
				LayoutElement.FLAG_BACKGROUND_BOX);
		// FIXME create a color for this at map format change
		e.setBackgroundColor(Legend.COLORS[Legend.COLOR_CLOCK_BACKGROUND]);
		e.setColor(Legend.COLORS[Legend.COLOR_CLOCK_TEXT]);
		e.setVRelative(ele[ROUTE_INSTRUCTION]);
		e.setHRelative(ele[ETA]);
		e.setAdditionalOffsX(-2);
		e.setActionID(Trace.ROTATE_TRAVEL_MODE_CMD);

		setOnScreenButtonSize();
	}	
	
	public void setOnScreenButtonSize(boolean big) {
		CMSLayout.bigOnScreenButtons = big;
		setOnScreenButtonSize();
	}
	
	private void setOnScreenButtonSize() {
		float factor;
		int fontFlag;
		int fontFlag2;
		factor = 1;
		fontFlag = LayoutElement.FLAG_FONT_MEDIUM;
		fontFlag2 = fontFlag;
		
		LayoutElement e = ele[ZOOM_IN];
		e.setWidthPercent((int) (170 * factor));
		e.setHeightPercent((int) (170 * factor));
		e.setFlag(fontFlag2);
		e = ele[ZOOM_OUT];
		e.setWidthPercent((int) (170 * factor));
		e.setHeightPercent((int) (170 * factor));		
		e.setFlag(fontFlag2);
		e = ele[SHOW_DEST];
		e.setWidthPercent((int) (170 * factor));
		e.setHeightPercent((int) (170 * factor));
		e.setFlag(fontFlag2);
		e = ele[RECENTER_GPS];
		e.setWidthPercent((int) (170 * factor));
		e.setHeightPercent((int) (170 * factor));
		e.setFlag(fontFlag2);
		e = ele[RECORDINGS];
		e.setWidthPercent((int) (170 * factor));
		e.setHeightPercent((int) (170 * factor));
		e.setFlag(fontFlag2);
		e = ele[SEARCH];
		e.setWidthPercent((int) (170 * factor));
		e.setHeightPercent((int) (170 * factor));
		e.setFlag(fontFlag2);
		e = ele[POINT_OF_COMPASS];
		e.setFlag(fontFlag2);
		e = ele[SOLUTION];
		e.setFlag(fontFlag2);
		e = ele[RECORDED_COUNT];
		e.setFlag(fontFlag2);
		e = ele[CURRENT_TIME];
		e.setFlag(fontFlag);
		e = ele[TRAVEL_MODE];
		e.setFlag(fontFlag);
		e = ele[ROUTE_DISTANCE];
		e.setFlag(fontFlag);
		e = ele[WAYNAME];
		e.setFlag(fontFlag);
		e = ele[SCALEBAR];
		e.setFlag(fontFlag2);
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
			return scalePx + 1;
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
	 * Draws the map scale to the screen.
	 * This calculation is currently horribly inefficient.
	 * There must be a better way than this.
	 * 
	 * @param g Graphics context for drawing
	 * @param left Screen X of top left corner of scale bar
	 * @param top Screen Y of top left corner of scale bar
	 */
	public void showScale(Graphics g, int left, int top) {
		g.setColor(Legend.COLORS[Legend.COLOR_SCALEBAR]);
		g.setStrokeStyle(Graphics.SOLID);
		int right = left + scalePx;
		g.drawLine(left, top + 2, right, top + 2);
		g.drawLine(left, top + 3, right, top + 3); // -> double line width
		g.drawLine(left, top, left, top + 5);
		g.drawLine(left + scalePx, top, right, top + 5);
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
			if (scale > 1609.344) {
				g.drawString(Integer.toString((int)(scale / 1609.344f + 0.5)) + "mi",
						left + scalePx / 2, top + 4, Graphics.HCENTER | Graphics.TOP);
			} else {
				g.drawString(Integer.toString((int)(scale / 0.9144 + 0.5)) + "yd",
						left + scalePx / 2, top + 4, Graphics.HCENTER | Graphics.TOP);
			}
		} else {
			if (scale > 1000) {
				g.drawString(Integer.toString((int)(scale / 1000.0f)) + "km",
						left + scalePx / 2, top + 4, Graphics.HCENTER | Graphics.TOP);
			} else {
				g.drawString(Integer.toString((int)scale) + "m",
						left + scalePx / 2, top + 4, Graphics.HCENTER | Graphics.TOP);
			}
		}
	}
	
	public void paint(Graphics g) {
		super.paint(g);
	}

	public void calcScaleBarWidth(PaintContext pc) {
		// Avoid exception after route calculation
		if ( pc.getP() == null )
			return;
		try {
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
			int ordMag = (int)(MoreMath.log((d / conv)) / MoreMath.log(10.0f));
			if ((d / conv) < 2.5 * MoreMath.pow(10, ordMag)) {
				scale = 2.5f * MoreMath.pow(10, ordMag) * conv;
			} else if ((d / conv) < 5 * MoreMath.pow(10, ordMag)) {
				scale = 5 * MoreMath.pow(10, ordMag) * conv;
			} else {
				scale = 10 * MoreMath.pow(10, ordMag) * conv;
			}

			//Calculate how many pixels this distance is apart
			//The scale/d factor should be between 1 and 2.5
			//due to rounding
			scalePx = (int)((basePx) * scale / d);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		g.fillArc(left + char0Width, top + char0Width, speedingSignWidth - (char0Width * 2),
				speedingSignWidth - (char0Width * 2), 0, 360);
		g.setColor(Legend.COLORS[Legend.COLOR_SPEEDING_SIGN_TEXT]);
		g.drawString(sMaxSpeed, left + speedingSignWidth / 2,
				top + speedingSignWidth / 2 - (char0Height / 2),
				Graphics.TOP | Graphics.HCENTER);
	}
	public void pointerPressed(int x, int y) {
	}	
	public void pointerReleased(int x, int y) {
	}	
	public void pointerDragged(int x, int y) {
	}	
	public void sizeChanged(int x, int y) {
	}	
	public int getMaxX() {
		return maxX;
	}	
	public int getMinX() {
		return minX;
	}	
}
