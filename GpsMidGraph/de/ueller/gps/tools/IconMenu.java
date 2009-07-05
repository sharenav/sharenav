/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;
import de.ueller.midlet.gps.CompletionListener;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;


public class IconMenu extends LayoutManager {

	protected int numCols;
	protected int numRows;
	
	protected int currentX;
	protected int currentY;
	
	private CompletionListener compListener;
	private IconMenuPageInterface iconMenuTabHandler;
	
	/** array containing the icons' filename and texts */
	private String[][] icons;
	/** array containing the icons' action ids */	
	private int[] iconActions;
	/** active ele */
	public int rememberEleId = 0;
	
	public IconMenu(CompletionListener compListener, IconMenuPageInterface iconMenuTabHandler, String[][] icons, int[] iconActions, int initialEleId, int numCols, int numRows, int minX, int minY, int maxX, int maxY) {
		super(iconActions.length, minX, minY, maxX, maxY);
		this.numCols = numCols;
		this.numRows = numRows;
		this.compListener = compListener;
		this.iconMenuTabHandler = iconMenuTabHandler;
		this.icons = icons;
		this.iconActions = iconActions;
		this.rememberEleId = initialEleId;
		this.currentY = initialEleId / numCols;
		this.currentX = initialEleId % numCols;
		createLayout();
		//validate();		
	}
	
	protected void createLayout() { 
		// divide the available region into a grid making the the number of icon columns and rows fit
		int gridHor = 100 / numCols;
		int gridVer = 100 / numRows;

		LayoutElement e;		
		int i=0;
		for (int y=0; y < numRows; y++) {
			for (int x=0; x < numCols; x++) {
				if (i>=ele.length) {
					break;
				}
				// create a LayoutElement for each icon
				e = ele[i]; e.init(
						LayoutElement.FLAG_HALIGN_LEFT_SCREENWIDTH_PERCENT | LayoutElement.FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT |
						LayoutElement.FLAG_BACKGROUND_SCREENPERCENT_WIDTH | LayoutElement.FLAG_BACKGROUND_SCREENPERCENT_HEIGHT |
						LayoutElement.FLAG_SCALE_IMAGE_TO_ELEMENT_WIDTH_OR_HEIGHT_KEEPRATIO |
						LayoutElement.FLAG_FONT_SMALL
				);
				e.setLeftPercent(x * gridHor + gridHor / 2);
				e.setTopPercent(y * gridVer + gridVer / 2);
				e.setWidthPercent(gridHor - gridHor / 8);
				e.setHeightPercent(gridVer - gridVer / 8);
				e.setColor(0xFFFFFF);
				e.setText(icons[i][1]);
				e.setImage(icons[i][0]);
				i++;
			}
		}
		// calculate the icons' positions immediately (so when drawing the cursor the first time, we already know where to draw it)
		super.recalcPositions();
	}
	
	
	/**
	 * @return: IconMenuPageInterface.IMP_ACTION_*
	 */
	public int changeSelectedXY(int offsX, int offsY) {
		if (currentX + offsX < 0) { // left boundary
			return IconMenuPageInterface.IMP_ACTION_PREV_TAB;
		}
		if (currentX + offsX >= numCols) { // right boundary
			return IconMenuPageInterface.IMP_ACTION_NEXT_TAB;
		}
		if (getElId(currentX + offsX, currentY) < ele.length) {
			currentX += offsX;
		} else { // after last element coming from left
			return IconMenuPageInterface.IMP_ACTION_NEXT_TAB;
		}
		
		if (currentY + offsY < 0) { // bottom boundary
			return IconMenuPageInterface.IMP_ACTION_ENTER_TAB_BUTTONS;
		}
//		if (currentY + offsY >= numRows) { // Bottom boundary
//			return IconMenuPageInterface.IMP_ACTION_NONE;
//		}
		if (getElId(currentX, currentY + offsY) < ele.length) {
			currentY += offsY;
		}
//		else {  // after last element coming from above
//			return IconMenuPageInterface.IMP_ACTION_NONE;
//		}
		rememberEleId = getElId(currentX, currentY);
		return IconMenuPageInterface.IMP_ACTION_NONE;
	}

	public void setIconAction(int nr, int actionId) {
		iconActions[nr] = actionId;
	}
	
	public void iconMenuSelect(int eleId) {
		// pass the action id to the completion listener
		this.compListener.actionCompleted("" + iconActions[eleId]);
	}
	
	public int getElId(int x, int y) {
		return x + y * numCols;
	}
	
	public void keyAction(int keyCode) {
		int action = Trace.getInstance().getGameAction(keyCode);
		int impAction = IconMenuPageInterface.IMP_ACTION_NONE;
		if (action != 0) {
			if (action == Canvas.FIRE) {
				iconMenuSelect(getElId(currentX, currentY));
			} else if (action ==  Canvas.LEFT) {
				impAction = changeSelectedXY(-1, 0);
			} else if (action ==  Canvas.RIGHT) {
				impAction = changeSelectedXY(1, 0);
			} else if (action ==  Canvas.UP) {
				impAction = changeSelectedXY(0, -1);
			} else if (action ==  Canvas.DOWN) {
				impAction = changeSelectedXY(0, 1);
			}
		}
		if (iconMenuTabHandler != null && impAction != IconMenuPageInterface.IMP_ACTION_NONE) {
			iconMenuTabHandler.iconMenuPageAction(impAction);
		}
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @return true, if an element has been clicked
	 */
	public boolean pointerPressed(int x, int y) {
		int i=getElementIdAtPointer(x, y);
		if (i>=0) {
			iconMenuSelect(i);
			return true;
		}
		return false;
	}
	
	
	public void paint(Graphics g, boolean showCursor) {
		// clean the displayable
		g.setColor(0);
		g.fillRect(0, 0, maxX, maxY);
		// draw a box under the still to be drawn active icon
		if (showCursor) {
			LayoutElement e = ele[getElId(currentX, currentY)];		
			g.setColor(0x00FFFF00);
			g.fillRect(e.left - 2, e.top - 2, e.right - e.left + 4, e.bottom - e.top + 4);
		}
		// draw all icons
		super.paint(g);
	}
}