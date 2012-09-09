/*
 * ShareNav - Copyright (c) 2009 sk750 at users dot sourceforge dot net
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


public class GuiSearchLayout extends LayoutManager {
	public static final int KEY_KEYPAD = 0;
	public static final int KEY_CLOSE = 1;
	public static final int KEY_BACKSPACE = 2;
	public static final int KEY_1 = 3;
	public static final int KEY_2 = 4;
	public static final int KEY_3 = 5;
	public static final int KEY_4 = 6;
	public static final int KEY_5 = 7;
	public static final int KEY_6 = 8;
	public static final int KEY_7 = 9;
	public static final int KEY_8 = 10;
	public static final int KEY_9 = 11;
	public static final int KEY_STAR = 12;
	public static final int KEY_0 = 13;
	public static final int KEY_POUND = 14;
	//#if polish.android
	public static final int TEXT = 15;
	//#endif

	//#if polish.android
	public static final int ELE_COUNT = 16;
	//#else
	public static final int ELE_COUNT = 15;
	//#endif

	public static final byte SE_KEY = 1;
	public static final byte SE_TEXT = 2;

	public boolean usingVerticalLayout = false;
	
	private static int xdiff = 0;
	private static int ydiff = 0;

	private volatile static int buttonw = 0;
	private volatile static int buttonh = 0;

	public LayoutElement ele[] = new LayoutElement[ELE_COUNT];
	
	public GuiSearchLayout(int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY, Legend.COLORS[Legend.COLOR_SEARCH_TOUCHED_BUTTON_BACKGROUND]);
		
		xdiff = (maxX - minX) / 3;
		ydiff = (maxY - minY) / 8;
		//System.out.println ("xdiff: " + xdiff + " ydiff: " + ydiff);
		buttonw = (maxX-minX-xdiff-6)/3;
		buttonh = (maxY-minY-ydiff-10)/5;

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

		//ydiff = 0;

		e = ele[KEY_KEYPAD];
		addElement(e,
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setAdditionalOffsX(xdiff);
		e.setAdditionalOffsY(ydiff);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_CLOSE];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_TOP |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setAdditionalOffsY(ydiff);
		e.setHRelative(ele[KEY_KEYPAD]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);
		
		e = ele[KEY_BACKSPACE];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_TOP |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setHRelative(ele[KEY_CLOSE]);
		e.setAdditionalOffsY(ydiff);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_1];
		addElement(e,
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_KEYPAD]);
		e.setAdditionalOffsX(xdiff);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_2];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_CLOSE]);
		e.setHRelative(ele[KEY_1]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);
		
		e = ele[KEY_3];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_BACKSPACE]);
		e.setHRelative(ele[KEY_2]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_4];
		addElement(e,
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setAdditionalOffsX(xdiff);
		e.setVRelative(ele[KEY_1]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_5];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_2]);
		e.setHRelative(ele[KEY_4]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);
		
		e = ele[KEY_6];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_3]);
		e.setHRelative(ele[KEY_5]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);
		
		e = ele[KEY_7];
		addElement(e,
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setAdditionalOffsX(xdiff);
		e.setVRelative(ele[KEY_4]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_8];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_5]);
		e.setHRelative(ele[KEY_7]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);
		
		e = ele[KEY_9];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_6]);
		e.setHRelative(ele[KEY_8]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);
		
		e = ele[KEY_STAR];
		addElement(e,
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setAdditionalOffsX(xdiff);
		e.setVRelative(ele[KEY_9]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_0];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_8]);
		e.setHRelative(ele[KEY_STAR]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		e = ele[KEY_POUND];
		addElement(e,
			LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_BELOW_RELATIVE |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_MEDIUM
		);
		e.setVRelative(ele[KEY_9]);
		e.setHRelative(ele[KEY_0]);
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_KEY);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);

		//#if polish.android
		e = ele[TEXT];
		addElement(e,
			LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
			LayoutElement.FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND |
			LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX |
			LayoutElement.FLAG_FONT_LARGE
		);
		Font font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
		//e.setAdditionalOffsX(xdiff);
		//e.setAdditionalOffsY(ydiff - font.getHeight());
		e.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
		e.setSpecialElementID(SE_TEXT);
		e.setActionID(GuiSearch.VIRTUALKEY_PRESSED);
		//#endif
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
		case SE_KEY:
			g.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_BORDER]);
			g.drawLine(left, top, left+buttonw, top);
			g.drawLine(left, top, left, top+buttonh);
			g.drawLine(left+buttonw, top, left+buttonw, top+buttonh);
			g.drawLine(left, top+buttonh, left+buttonw, top+buttonh);
			// problem with Nokia 5230 (S60r5), works with android&microemulator
			//g.drawSubstring(text, 0, 5, left+buttonw/2, top+buttonh/2, Graphics.HCENTER|Graphics.VCENTER);
			g.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
			g.drawString(text, left, top, Graphics.TOP|Graphics.LEFT);
			break;
		case SE_TEXT:
			g.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_BORDER]);
			// problem with Nokia 5230 (S60r5), works with android&microemulator
			//g.drawSubstring(text, 0, 5, left+buttonw/2, top+buttonh/2, Graphics.HCENTER|Graphics.VCENTER);
			g.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
			g.drawString(text, left, top, Graphics.TOP|Graphics.LEFT);
			break;
		}
	}
	

	protected int getSpecialElementWidth(byte id, String text, Font font) {
		switch(id) {
		case SE_KEY:
			return buttonw;
		case SE_TEXT:
			Font tFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
			return tFont.stringWidth(ele[id].getText());
		}
		return 0;
	}
	
	protected int getSpecialElementHeight(byte id, int fontHeight) {
		switch(id) {
		case SE_KEY:
			return buttonh;
		case SE_TEXT:
			return ele[id].getFontHeight();
		}
		return 0;
	}
}

 	  	 
