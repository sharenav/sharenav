/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;
import de.ueller.gps.tools.IconMenu;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;


public class IconMenuTabs {
	public boolean visible = false;

	protected String tabLabels[];
	protected int minX;
	protected int maxX;
	protected int minY;
	protected int maxY;
	protected int numIconPages;
	protected int tabNr;
	protected int leftMostTabNr = 0;
	protected boolean inTabRow = false;
	protected LayoutManager pageTabs;
	protected LayoutManager directionButtonManager;
	
	public IconMenuTabs(String [] tabLabels, int minX, int minY, int maxX, int maxY) {
		this.numIconPages = tabLabels.length;
		this.tabLabels = tabLabels;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;

		// create a layout manager for the direction buttons
		directionButtonManager = new LayoutManager(2, minX, minY, maxX, maxY);
		LayoutElement e;
		e = directionButtonManager.ele[0];
		e.init(
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_FONT_SMALL
		);					
		e.setBackgroundColor(0x00707070);
		e.setColor(0x00FFFFFF);
		e.setText( " < ");
		e = directionButtonManager.ele[1];
		e.init(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_TOP |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_FONT_SMALL
		);					
		e.setBackgroundColor(0x00707070);
		e.setColor(0x00FFFFFF);
		e.setText( " > ");
		directionButtonManager.recalcPositions();
		
		// create a layout manager for the tab buttons
		pageTabs = new LayoutManager(numIconPages, directionButtonManager.ele[0].right, minY, directionButtonManager.ele[1].left, maxY);
		
		for (int i=0; i<numIconPages; i++) {
			e = pageTabs.ele[i];
			// the first tab button is not relative to the others
			if (i==0) {
				e.init(
						LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
						LayoutElement.FLAG_BACKGROUND_BORDER | 
						LayoutElement.FLAG_FONT_SMALL
				);
			// all the other tab buttons are positioned rightto-relative to the previous one
			} else {
				e.init(
						LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_TOP |
						LayoutElement.FLAG_BACKGROUND_BORDER |
						LayoutElement.FLAG_FONT_SMALL
				);
				e.setHRelative(i-1);
			}
			e.setBackgroundColor(0x00707070);
			e.setColor(0x00FFFFFF);
		}
		setActiveTab(tabNr);
	}
	
	public void pointerPressed(int x, int y) {
		int directionButton = directionButtonManager.getElementIdAtPointer(x, y);
		if (directionButton == 0) {
			iconMenuPageAction(IconMenuPageInterface.IMP_ACTION_PREV_TAB);
		} else if (directionButton == 1) {
			iconMenuPageAction(IconMenuPageInterface.IMP_ACTION_NEXT_TAB);				
		} else {
			int newTab = pageTabs.getElementIdAtPointer(x, y);
			if (newTab >= 0) {
				setActiveTab(newTab);
			}
		}
	}
	
	/** processes keycodes when in the icon tab buttons */
	public void keyAction(int keyCode) {
		int action = Trace.getInstance().getGameAction(keyCode);
		if (action != 0) {
			if (action == Canvas.FIRE) {
				inTabRow = false;
			} else if (action ==  Canvas.LEFT) {
				iconMenuPageAction(IconMenuPageInterface.IMP_ACTION_PREV_TAB);
			} else if (action ==  Canvas.RIGHT) {
				iconMenuPageAction(IconMenuPageInterface.IMP_ACTION_NEXT_TAB);
			} else if (action ==  Canvas.DOWN) {
				inTabRow = false;
			}
		}
	}
	
	public void iconMenuPageAction(int impAction) {
		switch (impAction) {
			case IconMenuPageInterface.IMP_ACTION_NEXT_TAB:
				if (tabNr < numIconPages - 1) {
					tabNr++;
				}
				break;
			case IconMenuPageInterface.IMP_ACTION_PREV_TAB:
				if (tabNr > 0) {
					tabNr--;
					if (tabNr < leftMostTabNr) {
						leftMostTabNr = tabNr;
					}
				}
				break;
			case IconMenuPageInterface.IMP_ACTION_ENTER_TAB_BUTTONS:
				inTabRow = true;
				break;
			case IconMenuPageInterface.IMP_ACTION_END_MENU:
				visible = false;
				break;
		}
	}
	
	public void setActiveTab(int tabNr) {
		// clear the FLAG_BACKGROUND_BOX for all other tab buttons except the current one, where it needs to get set 
		for (int i=0; i < pageTabs.ele.length; i++) {
			if (i == tabNr) {
				pageTabs.ele[i].setFlag(LayoutElement.FLAG_BACKGROUND_BOX);
			} else {
				pageTabs.ele[i].clearFlag(LayoutElement.FLAG_BACKGROUND_BOX);				
			}
		}
		this.tabNr = tabNr;
	}
	
	/** draws the tab buttons */
	public void paint(Graphics g) {
		// refresh the button texts, so the LayoutManager knows they have to be drawn
		directionButtonManager.ele[0].setText(" < ");
		directionButtonManager.ele[1].setText(" > ");
		directionButtonManager.recalcPositions();

		LayoutElement e;
		boolean activeTabVisible = false;
		do {
			for (int i=0; i<numIconPages; i++) {
				e = pageTabs.ele[i];
				if (inTabRow && i == tabNr) {
					e.setColor(0x00FFFF00); // when in tab button row draw the current tab button in yellow text
				} else {
					e.setColor(0x00FFFFFF); // else draw it in white text				
				}
				// clear the button text
				e.setText("");
				if ( i >= leftMostTabNr) {
					// set the button text, so the LayoutManager knows it has to be drawn
					e.setText(tabLabels[i]);
				}
			}
			// recalc positions for currently drawn buttons
			pageTabs.recalcPositions();
			
			// if the right button does not fit, scroll the bar
			if (directionButtonManager.ele[1].left < pageTabs.ele[tabNr].right) {
				leftMostTabNr++;
				
			} else {
				activeTabVisible = true;
			}
		} while (!activeTabVisible);
		// let the LayoutManager draw the tabs
		pageTabs.paint(g);
		// draw the direction buttons
		e = directionButtonManager.ele[0];
		if (tabNr == 0) {
			e.setColor(0x00808080); // grey
		} else {
			e.setColor(0x00FFFFFF); // white										
		}
		e = directionButtonManager.ele[1];
		if (tabNr == numIconPages - 1) {
			e.setColor(0x00808080); // grey
		} else {
			e.setColor(0x00FFFFFF); // white
		}
		// clear the area of the right button as it might have been overdrawn by a tab button
		g.setColor(0);
		g.fillRect(e.left, e.top, e.right, e.bottom);
		directionButtonManager.paint(g);
	}
}