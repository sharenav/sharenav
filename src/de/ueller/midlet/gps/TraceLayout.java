/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.tile.C;
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
	public static final int SCALEBAR = 11;
	public static final int SPEEDING_SIGN = 12;
	public static final int SPEED_CURRENT = 13;
	public static final int ELE_COUNT = 14;

	// special element ids
	public static final byte SE_SCALEBAR = 1;
	public static final byte SE_SPEEDING_SIGN = 2;
	public static final byte SE_ZOOM_IN = 3;
	public static final byte SE_ZOOM_OUT = 4;
	
	public boolean usingVerticalLayout = false;
	private PaintContext pc = null;
	
	// variables for scale bar
	private int scalePx = 0;
	private float scale;
	
	// variables for speeding sign
	private int char0Width = 0;
	private int char0Height = 0;
	private int speedingSignWidth = 0;
	private String sOldSpeed = "";
	
	public TraceLayout(int minX, int minY, int maxX, int maxY) {
		super(ELE_COUNT, minX, minY, maxX, maxY);
		
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
		e = ele[TITLEBAR]; e.init(
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_TOP |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);	

		e = ele[SCALEBAR]; e.init(
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setAdditionalOffsX(10);
		e.setAdditionalOffsY(8);
		e.setVRelative(TITLEBAR);
		e.setSpecialElementID(SE_SCALEBAR);

		e = ele[POINT_OF_COMPASS]; e.init(
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX
		);	
		e.setVRelative(TITLEBAR);
		e.setBackgroundColor(0x00FFFF96);
	
		e = ele[SOLUTION]; e.init(
			LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM
		);	
		e.setAdditionalOffsX(-1);
		e.setVRelative(TITLEBAR);
	
		e = ele[RECORDED_COUNT]; e.init(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setVRelative(SOLUTION);
	
		e = ele[CELLID]; e.init(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setVRelative(RECORDED_COUNT);
	
		e = ele[AUDIOREC]; e.init(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setVRelative(CELLID);
		
		e = ele[WAYNAME]; e.init(
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_BOTTOM |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH |
			LayoutElement.FLAG_RESERVE_SPACE
		);
		
		e = ele[ROUTE_INTO]; e.init(
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM | LayoutElement.FLAG_FONT_BOLD |  
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);
		e.setVRelative(WAYNAME);
		e.setBackgroundColor(0x00008000);
	
		e = ele[ROUTE_INSTRUCTION]; e.init(
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM | LayoutElement.FLAG_FONT_BOLD |  
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);
		e.setBackgroundColor(0x00008000);
		e.setVRelative(ROUTE_INTO);		
	
		e = ele[ROUTE_OFFROUTE]; e.init(
			LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_SMALL  
		);
		e.setVRelative(ROUTE_INSTRUCTION);		
	
		e = ele[ROUTE_DISTANCE]; e.init(
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setBackgroundColor(0x00B0B030);
		e.setVRelative(ROUTE_INSTRUCTION);		

		e = ele[SPEED_CURRENT]; e.init(
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM |
				LayoutElement.FLAG_BACKGROUND_BOX
			);
		e.setBackgroundColor(C.BACKGROUND_COLOR);
		e.setVRelative(ROUTE_DISTANCE);		
		
		e = ele[SPEEDING_SIGN]; e.init(
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
				LayoutElement.FLAG_FONT_LARGE
			);
		e.setSpecialElementID(SE_SPEEDING_SIGN);
		e.setAdditionalOffsY(-5);
		e.setVRelative(SPEED_CURRENT);	
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
				showScale(pc, g, left, top);
				break;		
			case SE_SPEEDING_SIGN:
				showSpeedingSign(g, text, left, top);
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
	public void showScale(PaintContext pc, Graphics g, int left, int top) {
		//Draw the scale bar
		g.setColor(0x00000000);
		g.setStrokeStyle(Graphics.SOLID);
		int right = left + scalePx;
		g.drawLine(left, top + 2, right, top + 2);
		g.drawLine(left, top + 3, right, top + 3); //double line width
		g.drawLine(left, top, left, top + 5);
		g.drawLine(left + scalePx, top, right, top + 5);
		if (scale > 1000) {
			g.drawString(Integer.toString((int)(scale/1000.0f)) + "km", left + scalePx/2, top + 4, Graphics.HCENTER | Graphics.TOP);
		} else {
			g.drawString(Integer.toString((int)scale) + "m", left + scalePx/2, top + 4, Graphics.HCENTER | Graphics.TOP);
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
		//round this distance up to the nearest 5 or 10
		int ordMag = (int)(MoreMath.log(d)/MoreMath.log(10.0f));
		if (d < 2.5*MoreMath.pow(10,ordMag)) {
			scale = 2.5f*MoreMath.pow(10,ordMag);
		} else if (d < 5*MoreMath.pow(10,ordMag)) {
			scale = 5*MoreMath.pow(10,ordMag);
		} else {
			scale = 10*MoreMath.pow(10,ordMag);
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
		g.setColor(0x00FF0000);
		g.fillArc(left, top, speedingSignWidth, speedingSignWidth, 0, 360);
		g.setColor(0x00FFFFFF);
		g.fillArc(left + char0Width, top + char0Width, speedingSignWidth - (char0Width * 2), speedingSignWidth - (char0Width * 2), 0, 360);
		g.setColor(0x00000000);
		g.drawString(sMaxSpeed, left + speedingSignWidth/2, top + speedingSignWidth/2 - (char0Height / 2), Graphics.TOP | Graphics.HCENTER);
	}
	
}