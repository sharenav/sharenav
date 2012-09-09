/*
 * ShareNav - Copyright (c) 2009 sk750 at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.midlet.iconmenu;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.ui.ShareNav;

import net.sharenav.midlet.iconmenu.IconActionPerformer;
import net.sharenav.util.Logger;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;


public class IconMenuPage extends LayoutManager {
	private final static Logger logger = Logger.getInstance(LayoutManager.class,Logger.DEBUG);

	protected int numCols;
	protected int numRows;
	
	protected int currentCol;
	protected int currentRow;
	
	private final IconActionPerformer actionPerformer;
	
	public String title;
	
	/** background image for icons on this page */
	public Image bgImage = null;

	/** highlight image for icons on this page */
	public Image hlImage = null;
	
	/** active ele */
	public int rememberEleId = 0;

	
	/** the horizontal offset the icons on this page should be drawn at */
	public volatile int dragOffsX = 0;

	/** the vertical offset the icons on this page should be drawn at */
	public volatile int dragOffsY = 0;

	/** how many icon rows the menu is scrolled down */
	public volatile int scrollOffsY = 0;
	
	
	public IconMenuPage(String title, IconActionPerformer actionPerformer, int numCols, int numRows, int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY, 0);
		this.title = title;
		this.numCols = numCols;
		this.numRows = numRows;
		this.actionPerformer = actionPerformer;
		setCursor(rememberEleId);
	}

	public Image loadIconImage(String name) {
		try {
			Image image = Image.createImage("/" + name);
			if (image != null) {
				Font font = Font.getFont(Font.FACE_PROPORTIONAL, 0 , Font.SIZE_SMALL);
				image = LayoutElement.scaleIconImage(image, this, font.getHeight(), 0);
				//#debug debug
				logger.debug(name + " loaded and scaled to " + image.getWidth() + "x" + image.getHeight());
			}
			return image;
		} catch (Exception ec) {
			//#debug debug
			logger.debug("EXCEPTION loading " + name);
		}
		return null;
	}
	
	public void loadIconBackgroundImage() {
		// load icon background image
		this.bgImage = loadIconImage(Configuration.getIconPrefix() + "i_bg.png");
	}

	public void loadIconHighlighterImage() {
		// load icon highlighter image
		this.hlImage = loadIconImage(Configuration.getIconPrefix() + "i_hl.png");
	}

	public void setCursor(int eleId) {
		this.currentCol = eleId % numCols;
		this.currentRow = eleId / numCols;
		int oldscrollOffsY = this.scrollOffsY;
		this.scrollOffsY = 0;
		if (numCols == 4 && Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS)) {
			// numCols == 4 - arrange elements similarly as they are arranged in the 3-column setup
			this.currentCol = eleId % 3;
			this.currentRow = eleId / 3;
			if (eleId >= 9) {
				this.currentCol = 3;
				this.currentRow = eleId - 9;
			}
		}
		rememberEleId = eleId;
		if (this.currentRow >= numRows) {
			int diff = this.currentRow - numRows + 1;
			this.currentRow -= diff;
			this.scrollOffsY += diff;
			recalcPositions();
		}
		if (this.scrollOffsY != oldscrollOffsY) {
		    recalcPositions();
		}
	}
	
	public LayoutElement createAndAddIcon(String label, String imageName, int actionId) {
		LayoutElement e = super.createAndAddElement(
			LayoutElement.FLAG_ICONMENU_ICON |
			LayoutElement.FLAG_FONT_SMALL
		);
//		System.out.println("eleNr:" + eleNr + " x:" + (eleNr % numCols) + "y:" + (eleNr / numCols));
	    e.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_ICON_TEXT]);
		e.setActionID(actionId);
		e.setText(label);
		e.setImageNameOnly(imageName);
		return e;
	}
	
	
	public void removeElement(LayoutElement e) {
		super.removeElement(e);
	}
	
	
	/** load all icons for this icon page */
	protected void loadIcons() {
		loadIconBackgroundImage();
		loadIconHighlighterImage();
		LayoutElement e;
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			e.loadImage();
		}
	}
	
	/** unload all icons for this icon page, e. g. for handling size changes */
	protected void unloadIcons() {
		LayoutElement e;
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			e.unloadImage();
		}
		this.bgImage = null;
		this.hlImage = null;
	}
	
	protected boolean changeSelectedColRow(int offsCol, int offsRow) {
		if (currentCol + offsCol < 0) { // left boundary
			return false;
		}
		if (currentCol + offsCol >= numCols) { // right boundary
			return false;
		}
		if (getEleId(currentCol + offsCol, currentRow) < this.size()) {
			currentCol += offsCol;
		} else { // after last element coming from left
			return false;
		}
		
		if (currentRow == 0 && scrollOffsY > 0 && offsRow == -1) {
			scrollOffsY += offsRow;
			recalcPositions();
		} else if (currentRow + offsRow < 0) { // bottom boundary
                       return false;
		} else if (currentRow + offsRow >= numRows) { // Bottom boundary coming from top
			if (scrollOffsY + numRows - 1 + offsRow < (this.size() + numCols - 1) / numCols) {
				scrollOffsY += offsRow;
				if (getEleId(currentCol, currentRow + offsRow + scrollOffsY) >= this.size()) {
					currentRow -= 1;
				}
				recalcPositions();
			} else {
				return false;
			}
		} else if (getEleId(currentCol, currentRow + offsRow + scrollOffsY) < this.size()) {
			currentRow += offsRow;
		} else {
			return false;
		}
		// don't go to fourth column in 3 rows, 4 columns mode, when going down from first column
		//if (getEleId(currentCol, currentRow + offsRow) < this.size() && (currentRow + offsRow) < numRows) {
		//	currentRow += offsRow;
		//}
//		else {  // after last element coming from above
//			return false;
//		}
		updateRememberEleId();
		return true;
	}
	
	public void updateRememberEleId() {
		rememberEleId = getEleId(currentCol, currentRow, scrollOffsY);
	}

	private int getEleId(int col, int row) {
		int eleId = 0;
		if (numCols != 4 || ! Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS)) { // 3 or more than 4
			eleId = col + row * numCols;
		} else { // numCols == 4 - arrange elements similarly as they are arranged in the 3-column setup
			if (col == 3) {
				eleId = 9 + row;
			} else {
				eleId = row * 4 + (col-row);
			}
		}
		// make sure we don't get index out of bounds, i.e. return element at last valid position if col & row would be incorrect
//		if (eleId >= this.size()) {
		while (eleId >= 0 && eleId >= this.size()) {
			eleId--;
		}
		// setCursor(eleId);
//		}
		return eleId;
	}
	
	private int getEleId(int col, int row, int scrollOffset) {
		int eleId = 0;
		if (numCols != 4 || ! Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS)) { // 3 or more than 4
			eleId = col + (row + scrollOffset) * numCols;
		} else { // numCols == 4 - arrange elements similarly as they are arranged in the 3-column setup
			if (col == 3) {
				eleId = 9 + row;
			} else {
				eleId = (row + scrollOffset) * 4 + (col-row);
			}
		}
		// make sure we don't get index out of bounds, i.e. return element at last valid position if col & row would be incorrect
//		if (eleId >= this.size()) {
		while (eleId >= 0 && eleId >= this.size()) {
			eleId--;
		}
		// setCursor(eleId);
//		}
		return eleId;
	}
	
	protected int getActiveEleActionId() {
		return this.getElementAt(rememberEleId).actionID;
	}

	protected String getActiveEleChoiceName() {
		return this.getElementAt(rememberEleId).getText();
	}
	
	// @Override
	public void recalcPositions() {
		LayoutElement e;
		for (int i=scrollOffsY * numCols; i < this.size() && i < scrollOffsY * numCols + numRows * numCols; i++){
			e = (LayoutElement) this.elementAt(i);
			e.setEleNr(i-scrollOffsY*numCols);
			//#debug debug
			logger.trace("calc positions for element " + i);
			e.calcSizeAndPosition();
		}
		recalcPositionsRequired = false;
	}

	// @Override
	public int getElementIdAtPointer(int x, int y) {
		LayoutElement e;
		for (int i=scrollOffsY * numCols; i < this.size() && i < scrollOffsY * numCols + numRows * numCols; i++){
			//for (int i = this.size() - 1; i >= 0 ; i--){
			e = getElementAt(i);
			if (e.isInElement(x, y) && e.hasAnyValidActionId()) {
				return i;
			}
		}
		return -1;	
	}

	/**
	 * Paints the icons
	 */
	public void paint(Graphics g, boolean showCursor) {
		//#debug debug
		logger.debug("Painting IconMenuPage " + title);
		
		LayoutElement e;
		// draw to boxes under the still to be drawn active icon to create a border
		if (showCursor) {
			e = (LayoutElement) this.elementAt(getEleId(currentCol, currentRow, scrollOffsY));
			g.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_ICON_BORDER_HIGHLIGHT]);
			g.fillRect(e.left + dragOffsX - 2, e.top - 2 + dragOffsY, e.right - e.left + 4, e.bottom - e.top + 4);
			g.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_BACKGROUND]);
			g.fillRect(e.left + dragOffsX, e.top + dragOffsY, e.right - e.left, e.bottom - e.top);
		}
		int orgLeft;
		int orgTextLeft;
		int orgTop;
		int orgTextTop;
		// draw all visible icons
		for (int i=scrollOffsY * numCols; i < this.size() && i < scrollOffsY * numCols + numRows * numCols; i++){
			e = (LayoutElement) this.elementAt(i);
			if (dragOffsX == 0 && dragOffsY == 0) {
				e.paint(g);				
			} else {
				// paint with drag offset
				orgTextLeft = e.textLeft;
				orgLeft = e.left;
				orgTextTop = e.textTop;
				orgTop = e.top;
				e.left += dragOffsX;
				e.textLeft += dragOffsX;
				e.top = e.top + dragOffsY;
				e.textTop = e.textTop + dragOffsY;
				e.paint(g);
				e.left = orgLeft;
				e.textLeft = orgTextLeft;
				e.top = orgTop;
				e.textTop = orgTextTop;
			}
		}
	}
}
