/*
 * GpsMid - Copyright (c) 2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net 
 *
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

//#if polish.android
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Style;
//#endif

/**
 * class for windows
 * @author jkpj
 *
 */

public class DisplayWindow {
	
	// public static final byte WINDOW_FLAG_NAME = 1;

	private int minX = 0;
	private int minY = 0;
	private int maxX = 0;
	private int maxY = 0;

	private int xPosition = 0;
	private int yPosition = 0;
	
	private boolean hidden = false;
	private boolean isShown = false;
	private boolean handlesTouch = false;
	private boolean handlesKeys = false;
	private boolean activeForTouch = false;
	private boolean activeForKeys = false;

	public DisplayWindow(int minX, int maxX, int minY, int maxY, boolean touch, boolean keys) {
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.handlesTouch = touch;
		this.handlesKeys = keys;
	}
	public int getMinX() {
		return minX;
	}
	public int getMaxX() {
		return maxX;
	}
	public int getMinY() {
		return minY;
	}
	public int getMaxY() {
		return maxY;
	}

	public void setMinX(int val) {
		minX = val;
	}
	public void setMaxX(int val) {
		maxX = val;
	}
	public void setMinY(int val) {
		minY = val;
	}
	public void setMaxY(int val) {
		maxY = val;
	}


	public int getWidth() {
		return maxX - minX;
	}
	public int getHeight() {
		return maxY - minY;
	}

	public void setYPosition(int val) {
		yPosition = val;
	}
}
