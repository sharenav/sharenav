package de.ueller.midlet.graphics;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.tools.ImageTools;
import de.ueller.midlet.gps.Logger;

public class LcdNumericFont {

	private final static Logger logger = Logger.getInstance(
			LcdNumericFont.class, Logger.DEBUG);

	private final static byte SEGMENT_BOTTOM_LEFT = 1;
	private final static byte SEGMENT_TOP_LEFT = 2;
	private final static byte SEGMENT_TOP = 4;
	private final static byte SEGMENT_TOP_RIGHT = 8;
	private final static byte SEGMENT_BOTTOM_RIGHT = 16;
	private final static byte SEGMENT_BOTTOM = 32;
	private final static byte SEGMENT_MIDDLE = 64;

	private Image vert_bar_orig; //Original sized image
	private Image horiz_bar_orig;
	private Image vert_bar; //Image scaled to the current font size
	private Image horiz_bar;
	private Image vert_bar_cache; //Image scaled to previouse size for faster switching
	private Image horiz_bar_cache;
	
	private int fontSize = 48; //Current font size
	private int fontSize_cache = -1; //Font size corresponding to the cached images

	public LcdNumericFont() {
		try {
			vert_bar_orig = Image.createImage("/LCD_vert.png");
			horiz_bar_orig = Image.createImage("/LCD_horiz.png");
			vert_bar = vert_bar_orig;
			horiz_bar = horiz_bar_orig;
		} catch (IOException ioe) {
			logger.exception("Could not load the LCD font segments", ioe);
		}
	}

	public void setFontSize(int size) {
		if (fontSize == size) {
			return;
		}
		if (size == fontSize_cache) {
			int tmpSize = fontSize;
			fontSize = fontSize_cache;
			fontSize_cache = tmpSize;
			Image tmpImage = vert_bar;
			vert_bar = vert_bar_cache;
			vert_bar_cache = tmpImage;
			tmpImage = horiz_bar;
			horiz_bar = horiz_bar_cache;
			horiz_bar_cache = tmpImage;
		} else {
			fontSize_cache = fontSize;
			vert_bar_cache = vert_bar;
			horiz_bar_cache = horiz_bar;
			fontSize = size;
			vert_bar = ImageTools.scaleImage(vert_bar_orig, 12 * size / 48, (size >> 1));
			horiz_bar = ImageTools.scaleImage(horiz_bar_orig, (size >> 1), 12 * size / 48);
		}
	}

	/**
	 * Draw an integer to the graphics canvas in the LCD font
	 * @param g Graphics context for drawing
	 * @param i integer to draw
	 * @param x x-coordinate of the bottom right corner
	 * @param y y-coordinate of the bottom right corner
	 */
	public void drawInt(Graphics g, int i, int x, int y) {
		drawInt(g, i, 0, x, y);
	}

	/**
	 * Draw an integer to the graphics canvas in the LCD font
	 * @param g Graphics context for drawing
	 * @param i integer to draw
	 * @param minDigit left fill with 0 up to minDigit digits
	 * @param x x-coordinate of the bottom right corner
	 * @param y y-coordinate of the bottom right corner
	 */
	public void drawInt(Graphics g, int i, int minDigit, int x, int y) {
		boolean negative = (i < 0);
		int digitPos = 0;
		if (negative) {
			i *= -1;
		}
		int digit = i % 10;
		drawDigit(g, (byte) digit, x, y);
		digitPos++;
		i /= 10;
		while ((i > 0) || (digitPos < minDigit)) {
			digit = i % 10;
			drawDigit(g, (byte) digit, x - digitPos * fontSize, y);
			digitPos++;
			i /= 10;
		}
		if (negative) {
			drawDigit(g, (byte) -1, x - digitPos * fontSize, y);
			}
	}

	/**
	 * Draw a floating point number to the graphics canvas in the LCD font
	 * @param g Graphics context for drawing
	 * @param f number to draw
	 * @param decimalPlaces number of decimal places to show
	 * @param x x-coordinate of the bottom right corner
	 * @param y y-coordinate of the bottom right corner
	 */
	public void drawFloat(Graphics g, float f, int decimalPlaces, int x, int y) {
		logger.info("Drawing float " + f);
		int multi = 1;
		for (int i = 0; i < decimalPlaces; i++) {
			multi *= 10;
		}
		int frac = ((int) (f * multi)) % multi;
		drawInt(g, (frac < 0) ? -1 * frac : frac, decimalPlaces, x, y);
		g.fillRect(x - (fontSize * decimalPlaces + (fontSize >> 3)), y
				- (fontSize >> 3), (fontSize >> 3), (fontSize >> 3));
		drawInt(g, (int) f, x - (fontSize * decimalPlaces + (fontSize >> 2)), y);
	}

	/**
	 * Draw a dash for every decimal place to indicate that the number is 
	 * invalid / not available.
	 * @param g Graphics context for drawing
	 * @param decimalPlaces number of decimal places to show
	 * @param x x-coordinate of the bottom right corner
	 * @param y y-coordinate of the bottom right corner
	 */
	public void drawInvalid(Graphics g, int decimalPlaces, int x, int y) {
		for (int i = 0; i < decimalPlaces; i++) {
			drawDigit(g, (byte) -1, x, y);
			x -= fontSize;
		}
	}
	
	private void drawDigit(Graphics g, byte digit, int x, int y) {
		switch (digit) {
		case -1: {
			drawSegment(g, (byte) (SEGMENT_MIDDLE), x, y);
			break;
		}
		case 0: {
			drawSegment(g, (byte) (SEGMENT_BOTTOM | SEGMENT_TOP
					| SEGMENT_BOTTOM_LEFT | SEGMENT_BOTTOM_RIGHT
					| SEGMENT_TOP_LEFT | SEGMENT_TOP_RIGHT), x, y);
			break;
		}
		case 1: {
			drawSegment(g, (byte) (SEGMENT_BOTTOM_RIGHT | SEGMENT_TOP_RIGHT),
					x, y);
			break;
		}
		case 2: {
			drawSegment(g,
					(byte) (SEGMENT_BOTTOM | SEGMENT_TOP | SEGMENT_MIDDLE
							| SEGMENT_BOTTOM_LEFT | SEGMENT_TOP_RIGHT), x, y);
			break;
		}
		case 3: {
			drawSegment(g,
					(byte) (SEGMENT_BOTTOM | SEGMENT_TOP | SEGMENT_MIDDLE
							| SEGMENT_BOTTOM_RIGHT | SEGMENT_TOP_RIGHT), x, y);
			break;
		}
		case 4: {
			drawSegment(g, (byte) (SEGMENT_BOTTOM_RIGHT | SEGMENT_TOP_LEFT
					| SEGMENT_TOP_RIGHT | SEGMENT_MIDDLE), x, y);
			break;
		}
		case 5: {
			drawSegment(g,
					(byte) (SEGMENT_BOTTOM | SEGMENT_TOP | SEGMENT_MIDDLE
							| SEGMENT_BOTTOM_RIGHT | SEGMENT_TOP_LEFT), x, y);
			break;
		}
		case 6: {
			drawSegment(g, (byte) (SEGMENT_BOTTOM | SEGMENT_TOP
					| SEGMENT_MIDDLE | SEGMENT_BOTTOM_LEFT
					| SEGMENT_BOTTOM_RIGHT | SEGMENT_TOP_LEFT), x, y);
			break;
		}
		case 7: {
			drawSegment(
					g,
					(byte) (SEGMENT_TOP | SEGMENT_BOTTOM_RIGHT | SEGMENT_TOP_RIGHT),
					x, y);
			break;
		}
		case 8: {
			drawSegment(g,
					(byte) (SEGMENT_BOTTOM | SEGMENT_TOP | SEGMENT_MIDDLE
							| SEGMENT_BOTTOM_LEFT | SEGMENT_BOTTOM_RIGHT
							| SEGMENT_TOP_LEFT | SEGMENT_TOP_RIGHT), x, y);
			break;
		}
		case 9: {
			drawSegment(g,
					(byte) (SEGMENT_TOP | SEGMENT_MIDDLE | SEGMENT_BOTTOM_RIGHT
							| SEGMENT_TOP_LEFT | SEGMENT_TOP_RIGHT), x, y);
			break;
		}
		}
	}

	private void drawSegment(Graphics g, byte segments, int x, int y) {
		if ((segments & SEGMENT_BOTTOM_RIGHT) != 0) {
			g.drawImage(vert_bar, x, y, Graphics.BOTTOM | Graphics.RIGHT);
		}
		if ((segments & SEGMENT_TOP_RIGHT) != 0) {
			g.drawImage(vert_bar, x, y - (int) (fontSize * 0.62),
					Graphics.BOTTOM | Graphics.RIGHT);
		}
		if ((segments & SEGMENT_BOTTOM) != 0) {
			g.drawImage(horiz_bar, x - (fontSize >> 2), y + (fontSize >> 3),
					Graphics.BOTTOM | Graphics.RIGHT);
		}
		if ((segments & SEGMENT_MIDDLE) != 0) {
			g.drawImage(horiz_bar, x - (fontSize >> 2), y
					- (int) (fontSize * 0.4375), Graphics.BOTTOM
					| Graphics.RIGHT);
		}
		if ((segments & SEGMENT_TOP) != 0) {
			g.drawImage(horiz_bar, x - (fontSize >> 2), y - fontSize,
					Graphics.BOTTOM | Graphics.RIGHT);
		}
		if ((segments & SEGMENT_BOTTOM_LEFT) != 0) {
			g.drawImage(vert_bar, x - ((3 * fontSize) >> 2), y, Graphics.BOTTOM
					| Graphics.RIGHT);
		}
		if ((segments & SEGMENT_TOP_LEFT) != 0) {
			g
					.drawImage(vert_bar, x - ((3 * fontSize) >> 2), y
							- (int) (fontSize * 0.62), Graphics.BOTTOM
							| Graphics.RIGHT);
		}
	}
}
