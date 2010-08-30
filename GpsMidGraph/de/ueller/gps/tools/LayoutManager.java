/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gps.tools;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.midlet.gps.Logger;

import java.util.Vector;


public class LayoutManager extends Vector {
	private final static Logger logger = Logger.getInstance(LayoutManager.class,Logger.DEBUG);
	
	protected int minX;
	protected int minY;
	protected int maxX;
	protected int maxY;
	protected volatile boolean recalcPositionsRequired = true;
	
	/**
	 * @param minX layout area left
	 * @param minY layout area top
	 * @param maxX layout area right
	 * @param maxY layout area bottom
	 */
	public LayoutManager(int minX, int minY, int maxX, int maxY) {	
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}

	public LayoutElement createAndAddElement(int initialFlags) {
		LayoutElement e = new LayoutElement(this);
		addElement(e, initialFlags);
		return e;
	}
	public void addElement(LayoutElement e, int initialFlags) {
		e.init(initialFlags);
		addElement(e);
	}

	public void addElement(LayoutElement e) {
		super.addElement(e);
		refreshEleIds();
		recalcPositionsRequired = true;
	}

	private void refreshEleIds() {
		LayoutElement e;
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			e.setEleNr(i);
		}	
	}
	
	public void validate() {
		LayoutElement e;
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			if (e.getValidationError() != null) {
				logger.error("Element " + i + ": " + e.getValidationError());
			}
		}
	}
	
	public LayoutElement getElementAt(int i) {
		return (LayoutElement) this.elementAt(i);
	}

	protected void drawSpecialElement(Graphics g, byte id, String text, int left, int top) {
		System.out.println("drawSpecialElement not overridden!");
	}
	
	protected int getSpecialElementWidth(byte id, String text, Font font) {
		System.out.println("getSpecialElementWidth not overridden!");
		return 0;
	}
	protected int getSpecialElementHeight(byte id, int fontHeight) {
		System.out.println("getSpecialElementHeight not overridden!");
		return 0;
	}
	
	public void recalcPositions() {
		LayoutElement e;
		for (int i=0; i<this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			//#debug debug
			logger.trace("calc positions for element " + i);
			e.calcSizeAndPosition();
		}
		recalcPositionsRequired = false;
	}
	
	/**
	 * paints the LayoutElements
	 */
	public void paint(Graphics g) {
		LayoutElement e;
		if (!recalcPositionsRequired) {
			// if an element that has been visible before has not been set, the positions of all elements must be recalculated
			for (int i=0; i<this.size(); i++){
				e = (LayoutElement) this.elementAt(i);
				if (e.textIsValid != e.oldTextIsValid) {
					recalcPositionsRequired = true;
					break;
				}
			}
		}
		
		if (recalcPositionsRequired) {
			recalcPositions();
		}
		
		int oldColor = g.getColor();
		Font oldFont = g.getFont();
		for (int i = 0; i < this.size(); i++){
			e = (LayoutElement) this.elementAt(i);
			//#debug debug
			logger.trace("paint element " + i);
			e.paint(g);
		}
		g.setFont(oldFont);
		g.setColor(oldColor);
	}				
	

	public int getElementIdAtPointer(int x, int y) {
		LayoutElement e;
		for (int i = 0; i < this.size(); i++){
			e = getElementAt(i);
			if (e.isInElement(x, y)) {
				return i;
			}
		}
		return -1;	
	}

	public int getActionIdAtPointer(int x, int y) {
		return getActionIdShiftedAtPointer(x, y, 0);
	}
	
	public int getActionIdDoubleAtPointer(int x, int y) {
		return getActionIdShiftedAtPointer(x, y, 8);
	}

	public int getActionIdLongAtPointer(int x, int y) {
		return getActionIdShiftedAtPointer(x, y, 16);
	}
	
	public int getActionIdShiftedAtPointer(int x, int y, int shift) {
		int i = getElementIdAtPointer(x, y);
		if (i != -1) {			
			i = this.getElementAt(i).actionID;
			if (i != -1) {
				return (i >> shift) & 0x000000FF;
			}
		}
		return -1;
	}
	
}