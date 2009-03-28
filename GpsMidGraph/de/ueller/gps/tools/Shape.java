package de.ueller.gps.tools;
/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import de.ueller.midlet.gps.tile.PaintContext;
import javax.microedition.lcdui.Graphics;


public class Shape  {
	private static final byte POLYGON = 101;
	int minX = Integer.MAX_VALUE;
	int minY = Integer.MAX_VALUE;
	int maxX = 0;
	int maxY = 0;
	byte [] coords;

	/** coords in percent of screen height for the zoom in button */
	private static final byte coordsZoomIn[] = {
		POLYGON,
		0, 0,	// start point relative to anchor point (x in percent of screen width, y in percent of screen height)
		-12, 0, // diffX and diffY in percent of screen height
		0, -12,
		12, 0,
		0, 12,
		POLYGON, 
		-10, -6,	// start point relative to anchor point (x in percent of screen width, y in percent of screen height)
		8, 0,		// diffX and diffY in percent of screen height
		POLYGON, 
		-6, -10,	
		0, 8		// diffX and diffY in percent of screen height
	};
	
	/** coords in percent of screen height for the zoom in button */
	private static final byte coordsZoomOut[] = {
		POLYGON,
		0, 0,	// start point relative to anchor point (x in percent of screen width, y in percent of screen height)
		-12, 0, // diffX and diffY in percent of screen height
		0, 12,
		12, 0,
		0, -12,
		POLYGON, 
		-10, 6,	// start point relative to anchor point (x in percent of screen width, y in percent of screen height)
		8, 0	// diffX and diffY in percent of screen height
	};
	
	public Shape(char shapeID) {
		switch (shapeID) {
			case '+':
				coords = coordsZoomIn;
				break;
			case '-':
				coords = coordsZoomOut;
				break;
			default:
				return;
			}
	}
	
	
	/** Draw the shape at the anchor coordinates in percent of the screen width / height */
	public void drawShape(PaintContext pc, int anchorX, int anchorY) {
		int screenWidth = pc.xSize;
		int screenHeight = pc.ySize;
		
		// determine min / max positions on the fly
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		maxX = 0;
		maxY = 0;
		
		int i = 0;
		while (i < coords.length - 2) {
			if (coords[i] == POLYGON) {
				i++;
				int x1 = anchorX + (coords[i] * screenHeight) / 100;
				int y1 = anchorY + coords[i + 1] * screenHeight / 100;
				int x2 = 0;
				int y2 = 0;
				while (i < coords.length - 2) {
					i += 2;
					if (coords[i] >= POLYGON) {
						break;
					}
					adjustMinMax(x1, y1);
					x2 = x1 + (coords[i] * screenHeight / 100);			
					y2 = y1 + (coords[i + 1] * screenHeight / 100);
					// System.out.println("Draw" + i + ": " + x1 + "," + y1 + " " + x2 + "," + y2  + " scr: " + screenWidth + "," + screenHeight);
					pc.g.drawLine(x1, y1, x2, y2);
					x1 = x2;
					y1 = y2;
				}
				adjustMinMax(x1, y1);
			}
		}
	}
	
	private void adjustMinMax(int x, int y) {
		if (x < minX) minX = x;
		if (x > maxX) maxX = x;				
		if (y < minY) minY = y;
		if (y > maxY) maxY = y;
	}
	
	/** checks if the passed coordinates are inside a rectangle around the polygon */
	public boolean isInRectangleAroundPolygon(int x, int y) {
		//System.out.println("Min Max: " + minX + "," + minY + " " + maxX + "," + maxY + " click: " + x + "," + y);
		return (x > minX && x < maxX && y > minY && y < maxY);
	}
	
	public int getMaxX() {
		return maxX;
	}
	public int getMaxY() {
		return maxY;
	}
}
