/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.midlet.gps.Logger;


public class LayoutElement {
	private final static Logger logger = Logger.getInstance(LayoutElement.class,Logger.DEBUG);

	/** align element at minX of LayoutManager area */
	public static final int FLAG_HALIGN_LEFT = (1<<0);
	/** align element right at maxX of LayoutManager area */
	public static final int FLAG_HALIGN_RIGHT = (1<<1);
	/** center element between minX and maxX of the LayoutManager area */
	public static final int FLAG_HALIGN_CENTER = (1<<2);
	
	/** align element at minY of the LayoutManager area */
	public static final int FLAG_VALIGN_TOP = (1<<3);
	/** align element at percentage of  the LayoutManager area height that has to be set with setTopPercent() */
	public static final int FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT = (1<<4);
	/** align element at maxY of the LayoutManager area */
	public static final int FLAG_VALIGN_BOTTOM = (1<<5);
	/** center element between minY and maxY of the LayoutManager area */
	public static final int FLAG_VALIGN_CENTER = (1<<6);
	/** position element above the other element that has to be set with setVRelative() */
	public static final int FLAG_VALIGN_ABOVE_RELATIVE = (1<<7);
	/** position element below the other element that has to be set with setVRelative() */
	public static final int FLAG_VALIGN_BELOW_RELATIVE = (1<<8);
	/** position element left to the other element that has to be set with setHRelative() */
	public static final int FLAG_HALIGN_LEFTTO_RELATIVE = (1<<9);
	/** position element right to the other element that has to be set with setHRelative() */
	public static final int FLAG_HALIGN_RIGHTTO_RELATIVE = (1<<10);
	/** when this element becomes a relative reserve space for this element even if text is empty */
	public static final int FLAG_RESERVE_SPACE = (1<<11);
	
	/** draw a border as background */
	public static final int FLAG_BACKGROUND_BORDER = (1<<12);
	/** draw a box below as background */
	public static final int FLAG_BACKGROUND_BOX = (1<<13);
	/** make the background as wide as the LayoutManager area */
	public static final int FLAG_BACKGROUND_FULL_WIDTH = (1<<14);
	/** make the background as wide as a percentage of the LayoutManager area.
		Specify with setWidthPercent();
	*/
	public static final int FLAG_BACKGROUND_SCREENPERCENT_WIDTH = (1<<15);

	public static final int FLAG_FONT_SMALL = (1<<16);
	public static final int FLAG_FONT_MEDIUM = (1<<17);
	public static final int FLAG_FONT_LARGE = (1<<18);
	public static final int FLAG_FONT_BOLD = (1<<19);

	
	protected LayoutManager lm = null;
	private int flags = 0;
	
	private LayoutElement vRelativeTo = null;
	private LayoutElement hRelativeTo = null;
	public boolean usedAsRelative = false;
	protected boolean textIsValid = false;
	protected boolean oldTextIsValid = false;

	/** make the element width a percentage of the LayoutManager width */
	private int widthPercent = 100;
	/** position the element at a percentage of the LayoutManager height */
	private int topPercent = 100;
	private Font font = null;
	private int fontHeight = 0;
	private int height = 0;
	
	private String text = "";
	
	private int textWidth = 0;
	/** number of chars fitting (if a width flag is set) */
	private int numDrawChars = 0;
	
	private int width = 0;

	private int bgColor = 0x00FFFFFF;
	private int fgColor = 0x00000000;

	private int textLeft = 0;
	private int left = 0;
	/** additional offset to be added to left and textLeft */
	private int addOffsX = 0;
	/** additional offset to be added to top */
	private int addOffsY = 0;
	public int top = 0;
	private int right = 0;
	private int bottom = 0;
	
	private byte specialElementID;
	
	
	public LayoutElement(LayoutManager lm) {
		this.lm = lm;
	}

	public void init(int flags) {
		this.flags = flags;
		
		int fontSize = Font.SIZE_LARGE;
		int fontStyle = Font.STYLE_PLAIN;
		if ( (flags & FLAG_FONT_SMALL) > 0 ) {
			fontSize = Font.SIZE_SMALL;
		}
		if ( (flags & FLAG_FONT_MEDIUM) > 0 ) {
			fontSize = Font.SIZE_MEDIUM;
		}
		if ( (flags & FLAG_FONT_BOLD) > 0 ) {
			fontStyle |= Font.STYLE_BOLD;
		}
		this.font = Font.getFont(Font.FACE_PROPORTIONAL, fontStyle, fontSize);
		this.fontHeight = this.font.getHeight();
		this.height = this.fontHeight;
		
		if ( (flags & FLAG_BACKGROUND_FULL_WIDTH) > 0 ) {
			this.flags |= FLAG_BACKGROUND_SCREENPERCENT_WIDTH;
			widthPercent = 100;
			//#debug debug
			logger.debug("percent width " + widthPercent); 
		}
	}

	
	
	protected void calcPosition() {
		if ( textIsValid ) {
			width = textWidth;
		} else {
			width = 0;
		}
				
		if ( (flags & FLAG_BACKGROUND_SCREENPERCENT_WIDTH) > 0 ) {
			width = ((lm.maxX - lm.minX) * widthPercent) / 100;
			//#debug debug
			logger.debug("percent width " + width); 
		}

		if (specialElementID != 0) {			
			width = lm.getSpecialElementWidth(specialElementID, text, font);
			height = lm.getSpecialElementHeight(specialElementID, fontHeight);
		}

		
		//#debug debug
		logger.debug("width:" + width); 
		// check how many characters we can draw if the available width is limited
		while (textWidth > width) {
			numDrawChars--;
			textWidth = font.substringWidth(text, 0, numDrawChars);
		}
				
		if ( (flags & FLAG_HALIGN_LEFT) > 0 ) {
			textLeft = lm.minX;
			left = lm.minX;
		} else if ( (flags & FLAG_HALIGN_RIGHT) > 0 ) {
			textLeft =  lm.maxX - textWidth;
			left = lm.maxX - width;
		} else if ( (flags & FLAG_HALIGN_CENTER) > 0 ) {
			textLeft = lm.minX + ( lm.maxX - lm.minX - textWidth ) / 2;
			left = lm.minX + ( lm.maxX - lm.minX - width ) / 2;
		} else if ( (flags & FLAG_HALIGN_LEFTTO_RELATIVE) > 0 ) {
			left = getLeftToOrRightToNextVisibleRelative(true);
		} else if ( (flags & FLAG_HALIGN_RIGHTTO_RELATIVE) > 0 ) {
			left = getLeftToOrRightToNextVisibleRelative(false);
		}
		
		left += addOffsX;
		textLeft += addOffsX;
		right = left + textWidth;		
		
		if ( (flags & FLAG_BACKGROUND_SCREENPERCENT_WIDTH) > 0 ) {
			right = left + width;
		}
		
		if ( (flags & FLAG_VALIGN_TOP) > 0 ) {
			top = lm.minY;
		} else if ( (flags & FLAG_VALIGN_BOTTOM) > 0 ) {
			top = lm.maxY - height;
		} else if ( (flags & FLAG_VALIGN_CENTER) > 0 ) {
			top = lm.minY + (lm.maxY - lm.minY - height) / 2;
		} else if ( (flags & FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT) > 0 ) {
			top = lm.minY + ((lm.maxY - lm.minY) * topPercent) / 100;;
		} else if ( (flags & FLAG_VALIGN_BELOW_RELATIVE) > 0 ) {
			top = getAboveOrBelowNextVisibleRelative(false);
		} else if ( (flags & FLAG_VALIGN_ABOVE_RELATIVE) > 0 ) {
			top = getAboveOrBelowNextVisibleRelative(true);
		}
		
		top += addOffsY;
		bottom = top + height;
	}

	private int getAboveOrBelowNextVisibleRelative(boolean getAbove) {
		LayoutElement eRelative = vRelativeTo;
		int newTop = 0;
		while (eRelative != null) {
			if ( eRelative.textIsValid || (eRelative.flags & FLAG_RESERVE_SPACE) > 0 ) {
				if (getAbove) {
					newTop = eRelative.top - height;
				} else {
					newTop = eRelative.bottom;
				}
				break;
			} else {
				newTop = eRelative.top;
			}
			eRelative = eRelative.vRelativeTo;
		}
		return newTop;
	}

	private int getLeftToOrRightToNextVisibleRelative(boolean getLeft) {
		LayoutElement eRelative = hRelativeTo;
		int newLeft = 0;
		while (eRelative != null) {
			if ( eRelative.textIsValid || (eRelative.flags & FLAG_RESERVE_SPACE) > 0 ) {
				if (getLeft) {
					newLeft = eRelative.left - width;
				} else {
					newLeft = eRelative.right;
				}
				break;
			} else {
				newLeft = eRelative.left;
			}
			eRelative = eRelative.hRelativeTo;
		}
		return newLeft;
	}

	
	
	
	public void setText(String text) {
		if (! text.equalsIgnoreCase(this.text) ) {
			this.text = text; 
			textWidth = font.stringWidth(text);
			numDrawChars = text.length();
			lm.recalcPositionsRequired = true;
		}
		oldTextIsValid = textIsValid;
		textIsValid = numDrawChars!=0;
	}
	
	/**
		Set vertical relative to this relative element's position and height
		combine with FLAG_VALIGN_ABOVE_RELATIVE or FLAG_VALIGN_BELOW_RELATIVE for the relative direction
	*/
	public void setVRelative(int eleID) {
		vRelativeTo = lm.ele[eleID];
		vRelativeTo.usedAsRelative = true;
		lm.recalcPositionsRequired = true;
		if (vRelativeTo.flags == 0) {
			logger.error("Warning: Tried to use uninitialised element " + eleID + " as vRelative");
		}
	}

	/**
	Set horizontal relative to this relative element's position and width
	combine with FLAG_VALIGN_LEFTTO_RELATIVE or FLAG_RIGHTTO_RELATIVE for the relative direction
	*/
	public void setHRelative(int eleID) {
		hRelativeTo = lm.ele[eleID];
		hRelativeTo.usedAsRelative = true;
		lm.recalcPositionsRequired = true;
		if (hRelativeTo.flags == 0) {
			logger.error("Warning: Tried to use uninitialised element " + eleID + " as hRelative");
		}
	}

	public void setWidthPercent(int p) {
		widthPercent = p;
		lm.recalcPositionsRequired = true;
	}

	public void setTopPercent(int p) {
		topPercent = p;
		lm.recalcPositionsRequired = true;
	}
	
	public void setColor(int color) {
		fgColor = color;
	}

	public void setBackgroundColor(int color) {
		bgColor = color;
	}

	public void setAdditionalOffsX(int offsX) {
		addOffsX = offsX;
	}
	
	public void setAdditionalOffsY(int offsY) {
		addOffsY = offsY;
	}

	public void setSpecialElementID(byte id) {
		specialElementID = id;
	}

	
	public String getValidationError() {
		if (flags == 0) {
			return "not initialised";
		}
		if ( (flags & (FLAG_HALIGN_LEFT | FLAG_HALIGN_CENTER | FLAG_HALIGN_RIGHT)) == 0) {
			return "horizontal position flag missing";
		}
		if ( (flags & (FLAG_VALIGN_BOTTOM | FLAG_VALIGN_CENTER | FLAG_VALIGN_TOP | FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT |FLAG_VALIGN_ABOVE_RELATIVE | FLAG_VALIGN_BELOW_RELATIVE)) == 0) {
			return "vertical position flag missing";
		}
		if (vRelativeTo == null && (flags & (FLAG_VALIGN_ABOVE_RELATIVE | FLAG_VALIGN_BELOW_RELATIVE)) > 0) {
			return "vRelativeTo parameter missing";
		}
		if ( (flags & (FLAG_FONT_SMALL | FLAG_FONT_MEDIUM | FLAG_FONT_LARGE)) == 0) {
			return "font size missing";
		}		
		return null;
	}

	
	public void paint(Graphics g) {
		if (specialElementID != 0 && textIsValid) {			
			g.setFont(font);
			lm.drawSpecialElement(g, specialElementID, text, left, top);
		} else if (numDrawChars > 0 && textIsValid ) {
			if ( (flags & FLAG_BACKGROUND_BOX) > 0 ) {
				g.setColor(bgColor);
				//#debug debug
				logger.debug("draw box at " + left + "," + top + " size: " + (right-left) + "/" + (bottom - top));
				g.fillRect(left, top, right-left, bottom - top);
			}
			if ( (flags & FLAG_BACKGROUND_BORDER) > 0 ) {
				g.setColor(bgColor);
				//#debug debug
				logger.debug("draw border at " + left + "," + top + " size: " + (right-left) + "/" + (bottom - top));
				g.fillRect(left, top, right-left, bottom - top);
			}			
			if (font != null) {
				g.setColor(fgColor);
				g.setFont(font);
				//#debug debug
				logger.debug("draw " + text + " at " + textLeft + "," + top );
				
				g.drawSubstring(text, 0, numDrawChars, textLeft, top, Graphics.TOP|Graphics.LEFT);
			} else {
				//#debug debug
				logger.debug("no font for element");			
			}
		}
		oldTextIsValid = textIsValid;
		textIsValid = false; // text is invalid after drawing until it is set with setText() again
	}				
	
}