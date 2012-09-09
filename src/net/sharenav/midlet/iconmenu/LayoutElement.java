/*
 * ShareNav - Copyright (c) 2009 sk750 at users dot sourceforge dot net
 * See file COPYING
 */

package net.sharenav.midlet.iconmenu;

import java.io.IOException;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.midlet.util.ImageCache;
import net.sharenav.midlet.util.ImageTools;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;


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
	/** use numCols / numRows from IconMenuPage for positioning */
	public static final int FLAG_ICONMENU_ICON = (1<<5);
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
	public static final int FLAG_BACKGROUND_HEIGHTPERCENT_WIDTH = (1<<18);
	public static final int FLAG_BACKGROUND_HEIGHTPERCENT_HEIGHT = (1<<19);

	public static final int FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH = (1<<20);
	public static final int FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT = (1<<21);

	public static final int FLAG_FONT_SMALL = (1<<22);
	public static final int FLAG_FONT_MEDIUM = (1<<23);
	public static final int FLAG_FONT_LARGE = (1<<24);
	public static final int FLAG_FONT_BOLD = (1<<25);
	public static final int FONT_FLAGS = FLAG_FONT_BOLD | FLAG_FONT_LARGE | FLAG_FONT_MEDIUM | FLAG_FONT_SMALL;
	public static final int FLAG_IMAGE_GREY = (1<<26);
	public static final int FLAG_IMAGE_TOGGLEABLE = (1<<27);


	public static final int FLAG_TRANSPARENT_BACKGROUND_BOX = (1<<28);
	
	protected LayoutManager lm = null;
	private int flags = 0;
	
	private LayoutElement vRelativeTo = null;
	private LayoutElement hRelativeTo = null;
	public boolean usedAsRelative = false;
	protected boolean textIsValid = false;
	protected boolean oldTextIsValid = false;
	protected boolean isOnScreen = false;
	protected boolean touched = false;
	
	/** make the element width a percentage of the LayoutManager width or font height*/
	private int widthPercent = 100;
	/** make the element high a percentage of the font height or of the LayoutManager height */
	private int heightPercent = 100;
	private Font font = null;
	private int fontHeight = 0;
	private int height = 0;
	
	private String text = "";
	private String imageName;
	private String toggleImageName;
	private Image image = null;
	private boolean imageToggleState = false;
	
	private int textWidth = 0;
	/** number of chars fitting (if a width flag is set) */
	private short numDrawChars = 0;
	
	private int width = 0;

	private int bgColor = 0x00FFFFFF;
	private int fgColor = 0x00000000;

	public int textLeft = 0;
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
	
	private int eleNr;
	
	public int actionID = -1;

	public LayoutElement(LayoutManager lm) {
		this.lm = lm;
	}

	public void init(int flags) {
		this.flags = flags;
		initFont();
	}

	private void initFont() {
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
		lm.recalcPositionsRequired = true;
	}
	
	protected void calcSizeAndPosition() {
		calcSize();
		calcPosition();
	}

	public void setTouched(boolean b) {
		this.touched = b;
	}

	public boolean isTouched() {
		return this.touched;
	}

	protected void calcSize() {
		if ( textIsValid ) {
			width = textWidth;
		} else {
			width = 0;
		}
				
		if ( (flags & FLAG_BACKGROUND_HEIGHTPERCENT_WIDTH) > 0 ) {
			width = ((lm.maxY - lm.minY) * widthPercent) / 100;
		} else if ( (flags & FLAG_BACKGROUND_FONTHEIGHTPERCENT_WIDTH) > 0 ) {
			width = (fontHeight * widthPercent) / 100;
		} else if ( (flags & FLAG_BACKGROUND_FULL_WIDTH) > 0 ) {
			width = lm.maxX - lm.minX;
		}
		
		//#debug debug
		logger.trace("width:" + width);
		shortenTextToWidth();

		if (specialElementID != 0) {
			width = lm.getSpecialElementWidth(specialElementID, text, font);
			height = lm.getSpecialElementHeight(specialElementID, fontHeight);
		}
				
		
		if ( (flags & FLAG_ICONMENU_ICON) > 0 ) {
			Image im = image;
			IconMenuPage imp = (IconMenuPage) lm;
			if (imp.bgImage != null) {
				// the width of this element is the width of the icon background image
				im = imp.bgImage;
			}
			width = im.getWidth();
			height = im.getHeight();
		}
	}

	private void shortenTextToWidth() {
		int realWidth = width;
		if ( (flags & FLAG_ICONMENU_ICON) > 0 ) {
			realWidth = calcIconReservedWidth((IconMenuPage) lm);
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
		} else if ( (flags & FLAG_HALIGN_LEFTTO_RELATIVE) > 0 ) {
			left = getLeftToOrRightToNextVisibleRelative(true);
			textLeft = left + (width - textWidth) / 2;
		} else if ( (flags & FLAG_HALIGN_RIGHTTO_RELATIVE) > 0 ) {
			left = getLeftToOrRightToNextVisibleRelative(false);
			textLeft = left + (width - textWidth) / 2;
		} else if ( (flags & FLAG_ICONMENU_ICON) > 0 ) {
			IconMenuPage imp = (IconMenuPage) lm;
			int x = eleNr % imp.numCols;
			int y = eleNr / imp.numCols;
			if (imp.numCols == 4 && Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS)) {
                // imp.numCols == 4 - arrange elements similarly as they are arranged in the 3-column setup
				x = eleNr % 3;
				y = eleNr / 3;
				if (eleNr >= 9) {
					x = 3;
					y = eleNr - 9;
				}
			}
			left = imp.minX + x * calcIconReservedWidth(imp) + (calcIconReservedWidth(imp) - imp.bgImage.getWidth()) / 2;
			textLeft = imp.minX + x * calcIconReservedWidth(imp) + (calcIconReservedWidth(imp) - textWidth) / 2 ;
			top = imp.minY + y * calcIconReservedHeight(imp);
		}
		
		left += addOffsX;
		textLeft += addOffsX;

		if ( (flags & FLAG_HALIGN_CENTER_TEXT_IN_BACKGROUND) > 0 ) {
			textLeft = left + (width - textWidth) / 2;
		}
		
		right = left + width;
		
		
		if ( (flags & FLAG_BACKGROUND_HEIGHTPERCENT_HEIGHT) > 0 ) {
			height = ((lm.maxY - lm.minY) * heightPercent) / 100;
		} else if ( (flags & FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT) > 0 ) {
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
		if ( (flags & (FLAG_BACKGROUND_FONTHEIGHTPERCENT_HEIGHT + FLAG_BACKGROUND_HEIGHTPERCENT_HEIGHT)) > 0 ) {
			textTop = top +  (height - fontHeight) / 2;
		}
		if ( (flags & FLAG_ICONMENU_ICON) > 0 ) {
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
			if (getLeft) {
				newLeft = eRelative.left - width;
			} else {
				newLeft = eRelative.right;
			}
			if ( eRelative.textIsValid || (eRelative.flags & FLAG_RESERVE_SPACE) > 0 ) {
				break;
			}
			eRelative = eRelative.hRelativeTo;
		}
		return newLeft;
	}

	/** Sets the base icon name.
	 * For elements which can be toggled, the following applies:
	 * If setToggleImageName() is not used, '0' and '1' get appended to this name
	 * to derive the actual file names. If it is used, the name set here is the name 
	 * of the normal icon.  
	 * @param name Base name of icon
	 */
	public void setImageNameOnly(String name) {
		imageName = name;
		unloadImage();
	}

	/** Explicitly sets the name of the alternative icon. If this is set, '0' won't be
	 * needed at the end of the normal icon.
	 * This is the only chance to reuse icons, as else versions of them with '0' and '1'
	 * appended to the names would be needed.
	 * @param name Name of the alternative icon
	 */
	public void setToggleImageName(String name) {
		toggleImageName = name;
	}

	public void unloadImage() {
		image = null;
	}

	public void loadImage() {
		if (image == null) {
			setAndLoadImage(imageName);
			calcSizeAndPosition();
		}
	}

	public void setAndLoadImage(String name) {
		imageName = name;
		try {
			String imageName2 = imageName;
			if (hasFlag(FLAG_IMAGE_TOGGLEABLE)) {
				if (toggleImageName == null) {
					imageName2 += (imageToggleState ? "1" : "0");
				} else {
					if (imageToggleState) {
						imageName2 = toggleImageName;
					}
				}
				//#debug debug
				logger.debug("Load toggle image: " + imageName2);
			}
			Image orgImage;
			try {
				orgImage = Image.createImage("/" + Configuration.getIconPrefix() + imageName2 + ".png");
			} catch (IOException ioe) {
				//#debug debug
				logger.debug("Fall back to i_*.png for " + imageName2);
				try {
					orgImage = Image.createImage("/" + imageName2 + ".png");
				} catch (IOException ioe2) {
					//#debug debug
					logger.debug("Fall back to *_i_bg.png for " + imageName2);
					try {
						orgImage = Image.createImage("/" + Configuration.getIconPrefix() + "i_bg.png");
					} catch (IOException ioe3) {
						//#debug debug
						logger.debug("Fall back to i_bg.png for " + imageName2);
						orgImage = Image.createImage("/i_bg.png");
					}
				}
			}
			if ( (flags & FLAG_ICONMENU_ICON) > 0) {
				orgImage = scaleIconImage(orgImage, (IconMenuPage) lm, fontHeight, 0);
				if ( (flags & FLAG_IMAGE_GREY) > 0 ) {
					orgImage = ImageTools.getGreyImage(orgImage);
				}
			}
			image = orgImage;
		} catch (IOException ioe) {
			logger.exception(Locale.get("layoutelement.FailedToLoadIcon")/*Failed to load icon*/ + " " + imageName, ioe);
		}
	}
	
	private static int calcIconReservedHeight(IconMenuPage imp) {
		//System.out.println("maxY " + imp.maxY + " minY: " + imp.minY);
		return (imp.maxY - imp.minY) / imp.numRows;
	}

	private static int calcIconReservedWidth(IconMenuPage imp) {
		return (imp.maxX - imp.minX) / imp.numCols;
	}
	
	public static Image scaleIconImage(Image image, IconMenuPage imp, int fontHeight, int extraSize) {
		int imgWidth = calcIconReservedWidth(imp);
		int imgHeight = calcIconReservedHeight(imp) - fontHeight;
		
		int imgHeightRelativeToWidth = (image.getHeight() * imgWidth) / image.getWidth();
		int imgWidthRelativeToHeight = (image.getWidth() * imgHeight) / image.getHeight();

		if (imgWidth > imgWidthRelativeToHeight) {
			imgWidth = imgWidthRelativeToHeight;
		} else if (imgHeight > imgHeightRelativeToWidth) {
			imgHeight = imgHeightRelativeToWidth;
		}
				
		// shrink the icons until there's enough space between them
		while (imgWidth > 10 && imgHeight > 10 &&
				(calcIconReservedWidth(imp) - imgWidth < fontHeight / 2 || calcIconReservedHeight(imp) - fontHeight - imgHeight < fontHeight / 2)
				||
				// also shrink the icon while it is wider/higher than the bgImage's width/height
				(imp.bgImage != null && ( imp.bgImage.getWidth() < imgWidth || imp.bgImage.getHeight() < imgHeight) )
		) {
//			System.out.println("h: " + (calcIconReservedHeight(imp) - fontHeight - imgHeight) + " fh: " +  fontHeight / 2);
//			System.out.println("w: " + (calcIconReservedWidth(imp) - imgWidth) );
			imgWidth = imgWidth * 9 / 10;
			imgHeight = imgHeight * 9 / 10;
		}
		
//		System.out.println("actual Width/Height " + imgWidth + " " + imgHeight);
		return ImageTools.scaleImage(image, imgWidth + extraSize, imgHeight + extraSize);
	}
	
	/** Sets the state for toggling between two icons.
	 * True = use the alternative icon (with '1' appended to the base name)
	 * False = use the normal icon (with '0' appended to the base name)
	 * @see setToggleImageName to set the image names explicitly
	 * @param state New state
	 */
	public void setImageToggleState(boolean state) {
		boolean oldState = imageToggleState;
		if (oldState != state) {
			imageToggleState = state;
			if (image != null) {
				image = null;
				loadImage();
			}
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
			//TODO: There are 6 NPEs at startup on Android in the following line.
			textWidth = font.stringWidth(text);
			numDrawChars = (short) text.length();
			lm.recalcPositionsRequired = true;
			if (image != null) {
				//TODO: there should be evaluated a better way to change the label of icons
				Image orgImage = image;
				// recalc available width and shorten text to available width (without image)
				unloadImage();
				calcSize();
				image = orgImage;
			}
		}
		oldTextIsValid = textIsValid;
		textIsValid = numDrawChars!=0;
	}
	
	public String getText() {
		return text;
	}
	
	public int getFlags() {
		return flags;
	}
	
	/** Set vertical relative to this relative element's position and height combine
	 *	with FLAG_VALIGN_ABOVE_RELATIVE or FLAG_VALIGN_BELOW_RELATIVE for the relative direction.
	 */
	public void setVRelative(LayoutElement e) {
		vRelativeTo = e;
		vRelativeTo.usedAsRelative = true;
		lm.recalcPositionsRequired = true;
		if (vRelativeTo.flags == 0) {
			logger.error(Locale.get("layoutelement.WarningTriedUninitialisedElement")/*Warning: Tried to use uninitialised element*/ + " " + e + " " + Locale.get("layoutelement.asvRelative")/*as vRelative*/);
		}
	}

	public void setEleNr(int eleNr) {
		this.eleNr = eleNr;
	}
	
	public void setFlag(int flag) {
		if ((flag & FONT_FLAGS) > 0) {
			flags &= ~FONT_FLAGS;
			flags |= flag;
			initFont();
		} else {
			flags |= flag;
		}
	}
	
	public void clearFlag(int flag) {
		flags &= ~flag;
	}
	public boolean hasFlag(int flag) {
		return (flags & flag) > 0;
	}
	
	/**
	* Set horizontal relative to this relative element's position and width.
	* Combine with FLAG_VALIGN_LEFTTO_RELATIVE or FLAG_RIGHTTO_RELATIVE for the relative direction.
	*/
	public void setHRelative(LayoutElement e) {
		hRelativeTo = e;
		hRelativeTo.usedAsRelative = true;
		lm.recalcPositionsRequired = true;
		if (hRelativeTo.flags == 0) {
			logger.error(Locale.get("layoutelement.WarningTriedUninitialisedElement")/*Warning: Tried to use uninitialised element*/ + " " + e + " " + Locale.get("layoutelement.ashRelative")/*as hRelative*/);
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

	public Font getFont() {
		return font;
	}
	
	public String getValidationError() {
		if (flags == 0) {
			return "not initialised";
		}
		if ( (flags & (FLAG_HALIGN_LEFT | FLAG_HALIGN_CENTER | FLAG_HALIGN_RIGHT | FLAG_HALIGN_LEFTTO_RELATIVE | FLAG_ICONMENU_ICON | FLAG_HALIGN_RIGHTTO_RELATIVE)) == 0) {
			return "horizontal position flag missing";
		}
		if ( (flags & (FLAG_VALIGN_BOTTOM | FLAG_VALIGN_CENTER |FLAG_VALIGN_CENTER_AT_TOP_OF_ELEMENT | FLAG_VALIGN_TOP | FLAG_ICONMENU_ICON |FLAG_VALIGN_ABOVE_RELATIVE | FLAG_VALIGN_BELOW_RELATIVE | FLAG_VALIGN_WITH_RELATIVE)) == 0) {
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

	public void paintHighlighted(Graphics g) {
		//#debug trace
		logger.trace("draw highlight box at " + left + "," + top + " size: " + (right-left) + "/" + (bottom - top));
		g.setColor(lm.touchedElementBackgroundColor);
		g.fillRect(left, top, right-left, bottom - top);
		int oldFlags = flags;
		int oldBgColor = bgColor;
		clearFlag(FLAG_BACKGROUND_BOX);
		paint(g);
		flags = oldFlags;
	}
	
	public void paint(Graphics g) {
		if (specialElementID != 0 && textIsValid) {
			if ( (flags & FLAG_TRANSPARENT_BACKGROUND_BOX) > 0 ) {				
				Image imgBackground = ImageCache.getOneColorImage(0x80FFFFFF, right -left - 1, bottom - top - 1);
				if (imgBackground != null) {
					g.drawImage(imgBackground, left + 1, top + 1, Graphics.TOP | Graphics.LEFT);
				}
			}
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
			if ( (flags & FLAG_TRANSPARENT_BACKGROUND_BOX) > 0 ) {				
				Image imgBackground = ImageCache.getOneColorImage(0x80FFFFFF, right -left - 1, bottom - top - 1);
				if (imgBackground != null) {
					g.drawImage(imgBackground, left + 1, top + 1, Graphics.TOP | Graphics.LEFT);
				}
			}
			if ( (flags & FLAG_ICONMENU_ICON) > 0 ) {
				IconMenuPage imp = (IconMenuPage) lm;
				if (imp.bgImage != null) {
					g.drawImage(imp.bgImage, left, top, Graphics.TOP|Graphics.LEFT);
					g.drawImage(image, left + (imp.bgImage.getWidth() - image.getWidth()) / 2, top + (imp.bgImage.getHeight() - image.getHeight()) / 2, Graphics.TOP|Graphics.LEFT);
				} else {
					g.drawImage(image, left, top, Graphics.TOP|Graphics.LEFT);
				}
				if (this.touched || imp.touchedElement == this) {
					g.drawImage(imp.hlImage, left - 1, top - 1, Graphics.TOP|Graphics.LEFT);					
				}
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
		return (isOnScreen && x >= left && x <= right && y >= top && y <= bottom);
	}
	
	public boolean hasAnyValidActionId() {
		return actionID != -1;
	}
	
}