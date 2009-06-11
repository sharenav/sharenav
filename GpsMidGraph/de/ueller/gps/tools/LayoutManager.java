/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.RouteInstructions;

import java.util.Vector;


public class LayoutManager {
	private final static Logger logger = Logger.getInstance(LayoutManager.class,Logger.DEBUG);
	
	public LayoutElement ele[];
	protected int minX;
	protected int minY;
	protected int maxX;
	protected int maxY;
	protected volatile boolean recalcPositionsRequired = true;

	/**
	 * @param numElements - number of LayoutElements in this layout manager 
	 * @param minX - layout area left
	 * @param minY - layout area top
	 * @param maxX - layout area right
	 * @param maxY - layout area bottom
	 */
	public LayoutManager(int numElements, int minX, int minY, int maxX, int maxY) {
		ele = new LayoutElement[numElements];
		for (int i=0; i<ele.length; i++){
			ele[i] = new LayoutElement(this);
		}
		
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
	}


	/**
	 * checks and outputs errors in the LayoutElements
	 */
	public void validate() {
		for (int i=0; i<ele.length; i++){
			if (ele[i].getValidationError() != null) {
				logger.error("Element " + i + ": " + ele[i].getValidationError());
			}
		}
	}
	
	/**
	 * paints the LayoutElements
	 */
	public void paint(Graphics g) {
		if (!recalcPositionsRequired) {
			// if an element that has been visible before has not been set, the positions of all elements must be recalculated
			for (int i=0; i<ele.length; i++){
				if (ele[i].textIsValid != ele[i].oldTextIsValid) {
					recalcPositionsRequired = true;
					break;
				}
			}
		}
		
		if (recalcPositionsRequired) {
			for (int i=0; i<ele.length; i++){
				//#debug debug
				logger.trace("calc positions for element " + i);
				ele[i].calcPosition();
			}
			recalcPositionsRequired = false;
		}
		int oldColor = g.getColor();
		Font oldFont = g.getFont();
		for (int i=0; i<ele.length; i++){
			//#debug debug
			logger.trace("paint element " + i);
			ele[i].paint(g);
		}
		g.setFont(oldFont);
		g.setColor(oldColor);
	}				
	
}