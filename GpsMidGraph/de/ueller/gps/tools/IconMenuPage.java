/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.Trace;

import javax.microedition.lcdui.Graphics;

import java.util.Vector;

public class IconMenuPage extends LayoutManager { 

	protected int numCols;
	protected int numRows;
	
	protected int currentCol;
	protected int currentRow;
	
	private IconActionPerformer actionPerformer;
	
	public String title;
	
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
		this.rememberEleId = 0;
		this.currentRow = rememberEleId / numCols;
		this.currentCol = rememberEleId % numCols;
		// divide the available region into a grid making the the number of icon columns and rows fit
		gridHor = 100 / numCols;
		gridVer = 100 / numRows;
	}

	public LayoutElement createAndAddIcon(String label, String imageName, int actionId) {
		LayoutElement e = createIcon(label, imageName, actionId);
		return e;
	};
		
	public LayoutElement createIcon(String label, String imageName, int actionId) {
		int eleNr = this.size();
		LayoutElement e = super.createAndAddElement(
			LayoutElement.FLAG_HALIGN_LEFT_SCREENWIDTH_PERCENT | LayoutElement.FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT |
			LayoutElement.FLAG_BACKGROUND_SCREENPERCENT_WIDTH | LayoutElement.FLAG_BACKGROUND_SCREENPERCENT_HEIGHT |
			LayoutElement.FLAG_SCALE_IMAGE_TO_ELEMENT_WIDTH_OR_HEIGHT_KEEPRATIO |
			LayoutElement.FLAG_FONT_SMALL
		);
//		System.out.println("eleNr:" + eleNr + " x:" + (eleNr % numCols) + "y:" + (eleNr / numCols));
		setIconPositionAndSize(eleNr, e);
		e.setColor(C.ICONMENU_ICON_TEXTCOLOR);
		e.setActionID(actionId);
		e.setText(label);
		e.setImageNameOnly(imageName);
		return e;
	}
	
	private void setIconPositionAndSize(int eleNr, LayoutElement e) {
		e.setLeftPercent((eleNr % numCols) * gridHor + gridHor / 2);
		e.setTopPercent((eleNr / numCols) * gridVer + gridVer / 2);
		e.setWidthPercent(gridHor - gridHor / 8);
		e.setHeightPercent(gridVer - gridVer / 8);
	}

	private void recalculateIconPositions() {
		LayoutElement e;
		for (int i=0; i < this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			setIconPositionAndSize(i, e);
		}
	}
	
	public void removeElement(LayoutElement e) {
		super.removeElement(e);
		recalculateIconPositions();
	}
	
	
	/** load all icons for this icon page */
	protected void loadIcons() {
		LayoutElement e;
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			e.loadImage();
			// recalculate icon positions
			e.setFlag(LayoutElement.FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT);
		}
	}
	
	/** unload all icons for this icon page, e. g. for handling size changes */
	protected void unloadIcons() {
		LayoutElement e;
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			e.unloadImage();
		}
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
			g.setColor(C.ICONMENU_ICON_HIGHLIGHT_BORDERCOLOR);
			g.fillRect(e.left - 2, e.top - 2, e.right - e.left + 4, e.bottom - e.top + 4);
			g.setColor(C.ICONMENU_BGCOLOR);
			g.fillRect(e.left, e.top, e.right - e.left, e.bottom - e.top);
		}
		// draw all icons
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			e.paint(g);
		}
	}
}
