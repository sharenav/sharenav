/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;
import de.ueller.gps.data.Legend;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.midlet.gps.Logger;

import de.ueller.midlet.gps.GpsMid;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;


public class IconMenuPage extends LayoutManager { 
	private final static Logger logger = Logger.getInstance(LayoutManager.class,Logger.DEBUG);

	protected int numCols;
	protected int numRows;
	
	protected int currentCol;
	protected int currentRow;
	
	private IconActionPerformer actionPerformer;
	
	public String title;
	
	/** background image for icons on this page */
	public Image bgImage = null;
	
	/** active ele */
	public int rememberEleId = 0;
	public int gridHor;
	public int gridVer;
	
	
	public IconMenuPage(String title, IconActionPerformer actionPerformer, int numCols, int numRows, int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY);
		this.title = title;
		this.numCols = numCols;
		this.numRows = numRows;
		this.actionPerformer = actionPerformer;
		setCursor(rememberEleId);
		// divide the available region into a grid making the the number of icon columns and rows fit
		gridHor = 100 / numCols;
		gridVer = 100 / numRows;
	}

	public void loadIconBackgroundImage() {
		// load icon background image
		try {
			Image bgImage = Image.createImage("/i_bg.png");
			if (bgImage != null) {
				Font font = Font.getFont(Font.FACE_PROPORTIONAL, 0 , Font.SIZE_SMALL);
				bgImage = LayoutElement.scaleIconImage(bgImage, this, font.getHeight());
				//#debug debug
				logger.debug("bgImage loaded and scaled to " + bgImage.getWidth() + "x" + bgImage.getHeight());
			}
			this.bgImage = bgImage;
		} catch (Exception ec) {
			//#debug debug
			logger.debug("EXCEPTION loading bgImage");
		}
	}

	public void setCursor(int eleId) {
		this.currentRow = eleId / numCols;
		this.currentCol = eleId % numCols;
		rememberEleId = eleId;
	}
	
		
	public LayoutElement createAndAddIcon(String label, String imageName, int actionId) {
		LayoutElement e = super.createAndAddElement(
			LayoutElement.FLAG_ICONMENU_ICON |
			LayoutElement.FLAG_FONT_SMALL
		);
//		System.out.println("eleNr:" + eleNr + " x:" + (eleNr % numCols) + "y:" + (eleNr / numCols));
		if (GpsMid.legend == null) {
		    // make text visible even when map couldn't be read
		    e.setColor(0x00FFFF00);
		} else {
		    e.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_ICON_TEXT]);
		}
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
		
		if (currentRow + offsRow < 0) { // bottom boundary
			return false;
		}
//		if (currentY + offsY >= numRows) { // Bottom boundary coming from top
//			return false;
//		}
		if (getEleId(currentCol, currentRow + offsRow) < this.size()) {
			currentRow += offsRow;
		}
//		else {  // after last element coming from above
//			return false;
//		}
		rememberEleId = getEleId(currentCol, currentRow);
		return true;
	}
	
	private int getEleId(int col, int row) {
		return col + row * numCols;
	}
	
	protected int getActiveEleActionId() {
		return this.getElementAt(rememberEleId).actionID;
	}
	
	
	/**
	 * paints the Icons
	 */
	public void paint(Graphics g, boolean showCursor) {
		LayoutElement e;
		// draw to boxes under the still to be drawn active icon to create a border
		if (showCursor) {
			e = (LayoutElement) this.elementAt(getEleId(currentCol, currentRow));		
			g.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_ICON_BORDER_HIGHLIGHT]);
			g.fillRect(e.left - 2, e.top - 2, e.right - e.left + 4, e.bottom - e.top + 4);
			g.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_BACKGROUND]);
			g.fillRect(e.left, e.top, e.right - e.left, e.bottom - e.top);
		}
		// draw all icons
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			e.paint(g);
		}
	}
}
