/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;

import java.io.IOException;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.midlet.gps.Logger;


public class LayoutElement {
	private final static Logger logger = Logger.getInstance(LayoutElement.class,Logger.DEBUG);

	/** align element at minX of LayoutManager area */
	public static final int FLAG_HALIGN_LEFT = (1<<0);
	/** align element right at maxX of LayoutManager area */
	public static final int FLAG_HALIGN_RIGHT = (1<<1);
	/** center element between minX and maxX of the LayoutManager area */
	public static final int FLAG_HALIGN_CENTER = (1<<2);
	/** center element between minX and maxX of the LayoutManager area */
	public static final int FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND = (1<<3);
	
	/** align element at minY of the LayoutManager area */
	public static final int FLAG_VALIGN_TOP = (1<<4);
	/** align element at percentage of  the LayoutManager area height that has to be set with setTopPercent() */
	public static final int FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT = (1<<5);
	/** align element at maxY of the LayoutManager area */
	public static final int FLAG_VALIGN_BOTTOM = (1<<6);
	/** center element between minY and maxY of the LayoutManager area */
	public static final int FLAG_VALIGN_CENTER = (1<<7);
	/** center top of element between minY and maxY of the LayoutManager area */
	public static final int FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT = (1<<8);
	/** position element above the other element that has to be set with setVRelative() */
	public static final int FLAG_VALIGN_ABOVE_RELATIVE = (1<<9);
	/** position element below the other element that has to be set with setVRelative() */
	public static final int FLAG_VALIGN_BELOW_RELATIVE = (1<<10);
	/** position element on same vertical position as the other element that has to be set with setVRelative() */
	public static final int FLAG_VALIGN_WITH_RELATIVE = (1<<11);
	/** position element left to the other element that has to be set with setHRelative() */
	public static final int FLAG_HALIGN_LEFTTO_RELATIVE = (1<<12);
	/** position element right to the other element that has to be set with setHRelative() */
	public static final int FLAG_HALIGN_RIGHTTO_RELATIVE = (1<<13);
	/** when this element becomes a relative reserve space for this element even if text is empty */
	public static final int FLAG_RESERVE_SPACE = (1<<14);
	
	/** draw a border as background */
	public static final int FLAG_BACKGROUND_BORDER = (1<<15);
	/** draw a box below as background */
	public static final int FLAG_BACKGROUND_BOX = (1<<16);
	/** make the background as wide as the LayoutManager area */
	public static final int FLAG_BACKGROUND_FULL_WIDTH = (1<<17);
	/** make the background as wide as a percentage of the LayoutManager area.
		Specify with setWidthPercent();
	*/
	public static final int FLAG_BACKGROUND_SCREENPERCENT_WIDTH = (1<<18);

	public static final int FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH = (1<<19);
	public static final int FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT = (1<<20);

	public static final int FLAG_FONT_SMALL = (1<<21);
	public static final int FLAG_FONT_MEDIUM = (1<<22);
	public static final int FLAG_FONT_LARGE = (1<<24);
	public static final int FLAG_FONT_BOLD = (1<<25);
	public static final int FLAG_HALIGN_LEFT_SCREENWIDTH_PERCENT = (1<<26);
	public static final int FLAG_SCALE_IMAGE_TO_ELEMENT_WIDTH_OR_HEIGHT_KEEPRATIO = (1<<27);
	public static final int FLAG_BACKGROUND_SCREENPERCENT_HEIGHT = (1<<28);
	public static final int FLAG_IMAGE_GREY = (1<<29);

	
	protected LayoutManager lm = null;
	private int flags = 0;
	
	private LayoutElement vRelativeTo = null;
	private LayoutElement hRelativeTo = null;
	public boolean usedAsRelative = false;
	protected boolean textIsValid = false;
	protected boolean oldTextIsValid = false;
	protected boolean isOnScreen = false;
	
	/** make the element width a percentage of the LayoutManager width or font height*/
	private int widthPercent = 100;
	/** make the element high a percentage of the font height or of the LayoutManager height */
	private int heightPercent = 100;
	/** position the element at a percentage of the LayoutManager height */
	private int topPercent = 100;
	/** position the element at a percentage of the LayoutManager width */
	private int leftPercent = 100;
	private Font font = null;
	private int fontHeight = 0;
	private int height = 0;
	
	private String text = "";
	private String imageName;
	private Image image = null;
	
	private int textWidth = 0;
	/** number of chars fitting (if a width flag is set) */
	private short numDrawChars = 0;
	
	private int width = 0;

	private int bgColor = 0x00FFFFFF;
	private int fgColor = 0x00000000;

	private int textLeft = 0;
	public int left = 0;
	/** additional offset to be added to left and textLeft */
	private int addOffsX = 0;
	/** additional offset to be added to top */
	private int addOffsY = 0;
	public int textTop = 0;
	public int top = 0;
	public int right = 0;
	public int bottom = 0;
	
	private byte specialElementID;
	
	public int actionID = -1;

	
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
			logger.trace("percent width " + widthPercent); 
		}
	}


	protected void calcSizeAndPosition() {
		calcSize();
		calcPosition();
	}

	
	protected void calcSize() {		
		if ( textIsValid ) {
			width = textWidth;
		} else {
			width = 0;
		}
				
		if ( (flags & FLAG_BACKGROUND_SCREENPERCENT_WIDTH) > 0 ) {
			width = ((lm.maxX - lm.minX) * widthPercent) / 100;
			//#debug debug
			logger.trace("percent width " + width);
		}
		if ( (flags & FLAG_BACKGROUND_SCREENPERCENT_HEIGHT) > 0 ) {
			height = ((lm.maxY - lm.minY) * heightPercent) / 100;
		}
		if ( (flags & FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH) > 0 ) {
			width = (fontHeight * widthPercent) / 100;
		}
		//#debug debug
		logger.trace("width:" + width); 
		shortenTextToWidth();

		if (specialElementID != 0) {			
			width = lm.getSpecialElementWidth(specialElementID, text, font);
			height = lm.getSpecialElementHeight(specialElementID, fontHeight);
		}
		
		if (image != null) {
			width = image.getWidth();
			height = image.getHeight();
		}
	}

	private void shortenTextToWidth() {
		int realWidth = width;
		if ( (flags & FLAG_BACKGROUND_SCREENPERCENT_WIDTH) > 0 ) {
			realWidth = ((lm.maxX - lm.minX) * widthPercent) / 100;
		}
		// check how many characters we can draw if the available width is limited
		while (textWidth > realWidth) {
			numDrawChars--;
			textWidth = font.substringWidth(text, 0, numDrawChars);
		}
	}
	
	protected void calcPosition() {
		if ( (flags & FLAG_HALIGN_LEFT) > 0 ) {
			textLeft = lm.minX;
			left = lm.minX;
		} else if ( (flags & FLAG_HALIGN_RIGHT) > 0 ) {
			textLeft =  lm.maxX - textWidth - 1;
			left = lm.maxX - width - 1;
		} else if ( (flags & FLAG_HALIGN_CENTER) > 0 ) {
			textLeft = lm.minX + ( lm.maxX - lm.minX - textWidth ) / 2;
			left = lm.minX + ( lm.maxX - lm.minX - width ) / 2;
		} else if ( (flags & FLAG_HALIGN_LEFT_SCREENWIDTH_PERCENT) > 0 ) {
			left = lm.minX + ((lm.maxX - lm.minX) * leftPercent) / 100 - width / 2;			
			textLeft = left + (width - textWidth) / 2;
		} else if ( (flags & FLAG_HALIGN_LEFTTO_RELATIVE) > 0 ) {
			left = getLeftToOrRightToNextVisibleRelative(true);
			textLeft = left + (width - textWidth) / 2;
		} else if ( (flags & FLAG_HALIGN_RIGHTTO_RELATIVE) > 0 ) {
			left = getLeftToOrRightToNextVisibleRelative(false);
			textLeft = left + (width - textWidth) / 2;
		}
		
		left += addOffsX;
		textLeft += addOffsX;

		if ( (flags & FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND) > 0 ) {
			textLeft = left + (width - textWidth) / 2;
		}
		
		right = left + width;		
		
		
		if ( (flags & FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT) > 0 ) {
			height = (int) ((float) (fontHeight * heightPercent) / 100);
		}

		if ( (flags & FLAG_VALIGN_TOP) > 0 ) {
			top = lm.minY;
		} else if ( (flags & FLAG_VALIGN_BOTTOM) > 0 ) {
			top = lm.maxY - height;
		} else if ( (flags & FLAG_VALIGN_CENTER) > 0 ) {
			top = lm.minY + (lm.maxY - lm.minY - height) / 2;
		} else if ( (flags & FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT) > 0 ) {
			top = lm.minY + (lm.maxY - lm.minY) / 2;
		} else if ( (flags & FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT) > 0 ) {
			top = lm.minY + ((lm.maxY - lm.minY) * topPercent) / 100 - height / 2;
		} else if ( (flags & FLAG_VALIGN_BELOW_RELATIVE) > 0 ) {
			top = getAboveOrBelowNextVisibleRelative(false);
		} else if ( (flags & FLAG_VALIGN_ABOVE_RELATIVE) > 0 ) {
			top = getAboveOrBelowNextVisibleRelative(true);
		} else if ( (flags & FLAG_VALIGN_WITH_RELATIVE) > 0 ) {
			top = vRelativeTo.top;
		}
		
		//System.out.println("Height for " + text + ": " + height + "/" + fontHeight);
		top += addOffsY;
		textTop = top;
		bottom = top + height;
		if ( (flags & FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT) > 0 ) {
			textTop = top +  (height - fontHeight) / 2;
		}
		if (image != null) {
			textTop = bottom + 1;
		}
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

	public void setImageNameOnly(String imageName) {
		this.imageName = imageName;
		unloadImage();
	}

	public void unloadImage() {
		this.image = null;
	}

	public void loadImage() {
		if (image == null) {
			setAndLoadImage(imageName);
			calcSizeAndPosition();
		}
	}

	public void setAndLoadImage(String imageName) {
		this.imageName = imageName;
		try {
			Image orgImage = Image.createImage("/" + imageName + ".png");
			if ( (flags & FLAG_SCALE_IMAGE_TO_ELEMENT_WIDTH_OR_HEIGHT_KEEPRATIO) > 0) {
				image = null;
				calcSize(); // behaves different if image is null
				if (text.length() > 0) {
					width -= ((width * fontHeight) / height);
					height -= fontHeight;
					calcPosition();
					// also adjust the top of the element
					top -= (fontHeight) / 2;
					// and do not recalculate its position if it's in percent of screen height
					clearFlag(FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT);
				}

				int heightRelativeToWidth = (orgImage.getHeight() * width) / orgImage.getWidth();
				int widthRelativeToHeight = (orgImage.getWidth() * height) / orgImage.getHeight();
//				System.out.println("calced Width/Height " + width + " " + height);
				if (width > widthRelativeToHeight) {
					width = widthRelativeToHeight;
				} else if (height > heightRelativeToWidth) {
					height = heightRelativeToWidth;
				}
//				System.out.println("actual Width/Height " + width + " " + height);
				
				if ( (flags & FLAG_IMAGE_GREY) > 0 ) {
					orgImage = ImageTools.getGreyImage(orgImage);
				}
				orgImage = ImageTools.scaleImage(orgImage, width, height);
			}
			image = orgImage;
		} catch (IOException ioe) {
			logger.exception("Failed to load icon " + imageName, ioe);
		}
	}
	
	public void makeImageGreyed() {
		setFlag(FLAG_IMAGE_GREY);
		if (image != null) {
			//#debug debug
			logger.debug("Reload image greyed");
			image = null;
			loadImage();
		}
	}

	public void makeImageColored() {
		clearFlag(FLAG_IMAGE_GREY);
		if (image != null) {
			//#debug debug
			logger.debug("Reload image colored");
			image = null;
			loadImage();
		}
	}
	
	public void setTextValid() {
		setText(text);
	}
	
	public void setTextInvalid() {
		textIsValid = false;
	}

	public void setText(String text) {
		if (!textIsValid || !text.equalsIgnoreCase(this.text) ) {
			this.text = text; 
			textWidth = font.stringWidth(text);
			numDrawChars = (short) text.length();
			lm.recalcPositionsRequired = true;
			if (image != null) {
				//TODO: there should be evaluated a better way to change the label of icons
				Image orgImage = image;
				// recalc available width and shorten text to available width (without image)
				unloadImage();
				calcSize();
				setFlag(LayoutElement.FLAG_SCALE_IMAGE_TO_ELEMENT_WIDTH_OR_HEIGHT_KEEPRATIO);
				image = orgImage;
			}
		}
		oldTextIsValid = textIsValid;
		textIsValid = numDrawChars!=0;
	}
	
	public String getText() {
		return text;
	}
	
	/**
		Set vertical relative to this relative element's position and height
		combine with FLAG_VALIGN_ABOVE_RELATIVE or FLAG_VALIGN_BELOW_RELATIVE for the relative direction
	*/
	public void setVRelative(LayoutElement e) {
		vRelativeTo = e;
		vRelativeTo.usedAsRelative = true;
		lm.recalcPositionsRequired = true;
		if (vRelativeTo.flags == 0) {
			logger.error("Warning: Tried to use uninitialised element " + e + " as vRelative");
		}
	}

	public void setFlag(int flag) {
		flags |= flag;
	}
	public void clearFlag(int flag) {
		flags &= ~flag;
	}
	
	
	/**
	Set horizontal relative to this relative element's position and width
	combine with FLAG_VALIGN_LEFTTO_RELATIVE or FLAG_RIGHTTO_RELATIVE for the relative direction
	*/
	public void setHRelative(LayoutElement e) {
		hRelativeTo = e;
		hRelativeTo.usedAsRelative = true;
		lm.recalcPositionsRequired = true;
		if (hRelativeTo.flags == 0) {
			logger.error("Warning: Tried to use uninitialised element " + e + " as hRelative");
		}
	}

	public void setWidthPercent(int p) {
		widthPercent = p;
		lm.recalcPositionsRequired = true;
	}

	public void setHeightPercent(int p) {
		heightPercent = p;
		lm.recalcPositionsRequired = true;
	}

	public void setTopPercent(int p) {
		topPercent = p;
		lm.recalcPositionsRequired = true;
	}

	public void setLeftPercent(int p) {
		leftPercent = p;
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

	public void setActionID(int id) {
		actionID = id;
	}

	public int getFontHeight() {
		return fontHeight;
	}
		
	public String getValidationError() {
		if (flags == 0) {
			return "not initialised";
		}
		if ( (flags & (FLAG_HALIGN_LEFT | FLAG_HALIGN_CENTER | FLAG_HALIGN_RIGHT | FLAG_HALIGN_LEFTTO_RELATIVE | FLAG_HALIGN_LEFT_SCREENWIDTH_PERCENT | FLAG_HALIGN_RIGHTTO_RELATIVE)) == 0) {
			return "horizontal position flag missing";
		}
		if ( (flags & (FLAG_VALIGN_BOTTOM | FLAG_VALIGN_CENTER |FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT | FLAG_VALIGN_TOP | FLAG_VALIGN_TOP_SCREENHEIGHT_PERCENT |FLAG_VALIGN_ABOVE_RELATIVE | FLAG_VALIGN_BELOW_RELATIVE | FLAG_VALIGN_WITH_RELATIVE)) == 0) {
			return "vertical position flag missing";
		}
		if (vRelativeTo == null && (flags & (FLAG_VALIGN_ABOVE_RELATIVE | FLAG_VALIGN_BELOW_RELATIVE)) > 0) {
			return "vRelativeTo parameter missing";
		}
		if (hRelativeTo == null && (flags & (FLAG_HALIGN_LEFTTO_RELATIVE | FLAG_HALIGN_RIGHTTO_RELATIVE )) > 0) {
			return "hRelativeTo parameter missing";
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
			isOnScreen = true;
		} else if ( (numDrawChars > 0 && textIsValid) || image != null) {
			if ( (flags & FLAG_BACKGROUND_BOX) > 0 ) {
				g.setColor(bgColor);
				//#debug trace
				logger.trace("draw box at " + left + "," + top + " size: " + (right-left) + "/" + (bottom - top));
				g.fillRect(left, top, right-left, bottom - top);
			}
			if ( (flags & FLAG_BACKGROUND_BORDER) > 0 ) {
				g.setColor(bgColor);
				//#debug trace
				logger.trace("draw border at " + left + "," + top + " size: " + (right-left) + "/" + (bottom - top));
				g.setStrokeStyle(Graphics.SOLID);
				g.drawRect(left, top, right-left, bottom - top);
			}			
			if (image != null) {
				g.drawImage(image, left, top, Graphics.TOP|Graphics.LEFT);
			}
			if (font != null) {
				g.setColor(fgColor);
				g.setFont(font);
				//#debug trace
				logger.trace("draw " + text + " at " + textLeft + "," + top );
				
				g.drawSubstring(text, 0, numDrawChars, textLeft, textTop, Graphics.TOP|Graphics.LEFT);
				isOnScreen = true;
			} else {
				//#debug debug
				logger.debug("no font for element");			
			}
		} else {
			isOnScreen = false;
		}
		oldTextIsValid = textIsValid;
		textIsValid = false; // text is invalid after drawing until it is set with setText() again
	}				

	public boolean isInElement(int x, int y) {
		return (isOnScreen && x > left && x < right && y > top && y < bottom);
	}	
}