/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps;

import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;


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
	public static final int ELE_COUNT = 11;

	public boolean usingVerticalLayout = false;

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
		
		e = ele[POINT_OF_COMPASS]; e.init(
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX
		);	
		e.setRelative(TITLEBAR);
		e.setBackgroundColor(0x00FFFF96);
	
		e = ele[SOLUTION]; e.init(
			LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM
		);	
		e.setAdditionalOffsX(-1);
		e.setRelative(TITLEBAR);
	
		e = ele[RECORDED_COUNT]; e.init(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setRelative(SOLUTION);
	
		e = ele[CELLID]; e.init(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setRelative(RECORDED_COUNT);
	
		e = ele[AUDIOREC]; e.init(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
				LayoutElement.FLAG_FONT_MEDIUM
			);	
		e.setAdditionalOffsX(-1);
		e.setRelative(CELLID);
		
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
		e.setRelative(WAYNAME);
		e.setBackgroundColor(0x00008000);
	
		e = ele[ROUTE_INSTRUCTION]; e.init(
			LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM | LayoutElement.FLAG_FONT_BOLD |  
			LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH
		);
		e.setBackgroundColor(0x00008000);
		e.setRelative(ROUTE_INTO);		
	
		e = ele[ROUTE_OFFROUTE]; e.init(
			LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_SMALL  
		);
		e.setRelative(ROUTE_INSTRUCTION);		
	
		e = ele[ROUTE_DISTANCE]; e.init(
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_ABOVE_RELATIVE |
			LayoutElement.FLAG_FONT_MEDIUM |
			LayoutElement.FLAG_BACKGROUND_BOX
		);
		e.setBackgroundColor(0x00B0B030);
		e.setRelative(ROUTE_INSTRUCTION);		
	}

	/*
	 * layout for mobiles with very wide displays like Nokia E90
	 */
	private void createVerticalLayout() { 
		// TODO: create vertical layout - currently this layout is still the same as the horizontal layout
		createHorizontalLayout();
	}
}