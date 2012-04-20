/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 * 			Copyright (c) 2009 sk750 at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.midlet.iconmenu;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

//#if polish.android
import android.view.KeyEvent;
//#endif

import java.util.Vector;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.ui.GpsMid;
import de.ueller.gpsmid.ui.GpsMidDisplayable;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.midlet.iconmenu.IconActionPerformer;
import de.ueller.midlet.iconmenu.IconMenuPage;
import de.ueller.util.Logger;

import de.enough.polish.util.Locale;

public class IconMenuWithPagesGui extends Canvas implements CommandListener,
		GpsMidDisplayable {

	private final static Logger logger = Logger.getInstance(IconMenuWithPagesGui.class,Logger.DEBUG);

	private final Command OK_CMD = new Command("Ok", Command.OK, 1);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);

	protected GpsMidDisplayable parent = null;
	protected IconActionPerformer actionPerformer = null;

	private int minX;
	private int maxX;
	private int minY;
	private int maxY;
	public volatile int tabNr = 0;
	private volatile int leftMostTabNr = 0;
	private boolean inTabRow = false;
	/** LayoutManager for the prev / next direction buttons */
	private LayoutManager tabDirectionButtonManager;
	/** " < " button */
	private LayoutElement ePrevTab;
	/** " > " button */
	private LayoutElement eNextTab;
	/** status bar */
	private LayoutElement eStatusBar;

	/** the tab label buttons */
	private LayoutManager tabButtonManager;
	private boolean recreateTabButtonsRequired = false;
	
	/** the tab Pages with the icons */
	private final Vector iconMenuPages = new Vector();

	protected static long pressedKeyTime = 0;
	protected static int pressedKeyCode = 0;

	/** x position display was touched last time */
	private static int touchX = 0;
	/** y position display was touched last time */
	private static int touchY = 0;
	/** indicates if the pointer is currently pressed */	
	private static boolean pointerPressedDown = false;

	public IconMenuWithPagesGui(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		// create Canvas
		super();
		initIconMenuWithPagesGUI(parent, actionPerformer);
	}

	public IconMenuWithPagesGui(GpsMidDisplayable parent) {
		// create Canvas
		super();
		initIconMenuWithPagesGUI(parent, null);
	}

	private void initIconMenuWithPagesGUI(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN));
		this.parent = parent;
		// must be below setFullScreenMode() for not recreating this icon menu because of the size change
		this.actionPerformer = actionPerformer;
		this.minX = 0;
		this.minY = 0;
		this.maxX = getWidth();
		this.maxY = getHeight();
		recreateTabButtons();
		setCommandListener(this);
		addCommands();
	}
	
	public void setIconActionPerformer(IconActionPerformer actionPerformer) {
		this.actionPerformer = actionPerformer;
	}

	public IconMenuPage createAndAddMenuPage(String pageTitle, int numCols, int numRows) {
		/* swap numCols with numRows according to display size to make icons bigger
		   (we don't take the actual width/height of the LayoutManager
		   because the icons would then would be mapped wrongly to keys on 176*220 because
		   of buttons and status bar)
		*/
		if (getWidth() > getHeight() && numRows > numCols
				||
			getWidth() < getHeight() && numRows < numCols
		) {
			int tmp = numCols;
			numCols = numRows;
			numRows = tmp;
		}
		IconMenuPage imp = new IconMenuPage( pageTitle, actionPerformer, numCols, numRows, minX, calcIconMenuMinY(), maxX, calcIconMenuMaxY());
		iconMenuPages.addElement(imp);
		recreateTabButtonsRequired = true;
		return imp;
	}
	
	private int getFontFlag() {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS)) {
			return LayoutElement.FLAG_FONT_MEDIUM;
		}
		return LayoutElement.FLAG_FONT_SMALL;
	}
	
	/** create a layout manager with the direction buttons */
	private void createTabPrevNextButtons() {
		tabDirectionButtonManager = new LayoutManager(minX, minY, maxX, maxY, Legend.COLORS[Legend.COLOR_ICONMENU_TOUCHED_BUTTON_BACKGROUND_COLOR]);
		ePrevTab = tabDirectionButtonManager.createAndAddElement(
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				getFontFlag()
		);
		ePrevTab.setBackgroundColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_BORDER]);
		ePrevTab.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT]);
		ePrevTab.setText( " < ");
		eNextTab = tabDirectionButtonManager.createAndAddElement(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_TOP |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				getFontFlag()
		);
		eNextTab.setBackgroundColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_BORDER]);
		eNextTab.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT]);
		eNextTab.setText( " > ");
		eStatusBar = tabDirectionButtonManager.createAndAddElement(
				LayoutElement.FLAG_HALIGN_CENTER | LayoutElement.FLAG_VALIGN_BOTTOM |
				LayoutElement.FLAG_BACKGROUND_BOX | LayoutElement.FLAG_BACKGROUND_FULL_WIDTH |
				getFontFlag()
		);
		eStatusBar.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT]);
		eStatusBar.setBackgroundColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_BORDER]);
		eStatusBar.setText(" ");
		
		tabDirectionButtonManager.recalcPositions();
	}

	
	/** recreates the tab buttons for all the iconMenuPages */
	private void recreateTabButtons() {
		createTabPrevNextButtons();
		tabButtonManager = new LayoutManager(ePrevTab.right, minY, eNextTab.left, maxY, Legend.COLORS[Legend.COLOR_ICONMENU_TOUCHED_BUTTON_BACKGROUND_COLOR]);
		LayoutElement e = null;
		IconMenuPage imp = null;
		for (int i=0; i < iconMenuPages.size(); i++) {
			imp = (IconMenuPage) iconMenuPages.elementAt(i);
			if (tabButtonManager.size() == 0) {
				e = tabButtonManager.createAndAddElement(
						LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
						LayoutElement.FLAG_BACKGROUND_BORDER |
						getFontFlag()
				);
			// all the other tab buttons are positioned rightto-relative to the previous one
			} else {
				e = tabButtonManager.createAndAddElement(
						LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_TOP |
						LayoutElement.FLAG_BACKGROUND_BORDER |
						getFontFlag()
				);
				e.setHRelative(tabButtonManager.getElementAt(tabButtonManager.size() - 2));
			}
			e.setBackgroundColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_BORDER]);
			e.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT]);
			e.setText(imp.title);
		}
		setActiveTab(tabNr);
		recreateTabButtonsRequired = false;
	}
	

	public void show() {
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN));
		repaint();
		GpsMid.getInstance().show(this);
	}
	
	/** adds the commands to this Displayable but no Ok command in full screen mode - press fire there */
	private void addCommands() {
		removeCommand(OK_CMD);
		removeCommand(BACK_CMD);
		if (! Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN)) {
			addCommand(OK_CMD);
			addCommand(BACK_CMD);
		} else {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_BACK_CMD)) {
				addCommand(BACK_CMD);
			}
		}
		
	}
	
	public void sizeChanged(int w, int h) {
		this.maxX = w;
		this.maxY = h;
		recreateTabButtons();
		IconMenuPage imp = null;
		for (int i=0; i < iconMenuPages.size(); i++) {
			imp = (IconMenuPage) iconMenuPages.elementAt(i);
			imp.maxX = maxX;
			imp.minY = calcIconMenuMinY();
			imp.maxY = calcIconMenuMaxY();
			/* swap numCols with numRows according to display size to make icons bigger
			   (we don't take the actual width/height of the LayoutManager
			   because the icons would then would be mapped wrongly to keys on 176*220 because
			   of buttons and status bar)
			*/
			if (getWidth() > getHeight() && imp.numRows > imp.numCols
					||
				getWidth() < getHeight() && imp.numRows < imp.numCols
			) {
				int tmp = imp.numCols;
				imp.numCols = imp.numRows;
				imp.numRows = tmp;
			}
			
			imp.unloadIcons();
		}
		recreateTabButtonsRequired = true;
	}
	
	private int calcIconMenuMinY() {
		Font font = Font.getFont(Font.FACE_PROPORTIONAL, 0 , Font.SIZE_SMALL);
		return minY + eNextTab.bottom + font.getHeight();
	}

	private int calcIconMenuMaxY() {
		return eStatusBar.top;
	}
	
	public void setActiveTab(int tabNr) {
		if(tabNr >= iconMenuPages.size()) {
			return;
		}
		// clear the FLAG_BACKGROUND_BOX for all other tab buttons except the current one, where it needs to get set
		for (int i=0; i < tabButtonManager.size(); i++) {
			if (i == tabNr) {
				tabButtonManager.getElementAt(i).setFlag(LayoutElement.FLAG_BACKGROUND_BOX);
			} else {
				tabButtonManager.getElementAt(i).clearFlag(LayoutElement.FLAG_BACKGROUND_BOX);
			}
		}
		//#debug debug
		logger.debug("set tab " + tabNr);
		this.tabNr = tabNr;
		if (tabNr < leftMostTabNr) {
			leftMostTabNr = tabNr;
		}
		// load all icons for the new icon page
		getActiveMenuPage().loadIcons();
	}
	
	public void setActiveTabAndCursor(int tabNr, int eleId) {
		setActiveTab(tabNr);
		getActiveMenuPage().setCursor(eleId);
	}
	
	public void nextTab() {
		if (tabNr < tabButtonManager.size() - 1) {
			tabNr++;
			//#debug debug
			logger.debug("next tab " + tabNr);
		}
		// set flags for tab buttons
		setActiveTab(tabNr);
	}

	public void prevTab() {
		if (tabNr > 0) {
			tabNr--;
			//#debug debug
			logger.debug("prev tab " + tabNr);
		}
		// set flags for tab buttons
		setActiveTab(tabNr);
	}
	
	public IconMenuPage getActiveMenuPage() {
		 return (IconMenuPage) iconMenuPages.elementAt(tabNr);
	}
	
	public void commandAction(Command c, Displayable d) {
		if (c == OK_CMD) {
			if (inTabRow) {
				inTabRow = false;
				repaint();
			} else {
				performIconAction(getActiveMenuPage().getActiveEleActionId(),
						  getActiveMenuPage().getActiveEleChoiceName());
			}
		} else if (c == BACK_CMD) {
			performIconAction(IconActionPerformer.BACK_ACTIONID, null);
		}
	}
	
	/** process keycodes  */
	protected void keyPressed(int keyCode) {
		//#debug debug
		logger.debug("got key " + keyCode);
		int action = getGameAction(keyCode);
		logger.debug("got action key " + action);
		
		//Icons directly mapped on keys mode
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS)
			&& (keyCode == KEY_STAR || keyCode == KEY_POUND || keyCode <= KEY_NUM9 && keyCode >= KEY_NUM0)) {
			pressedKeyTime = System.currentTimeMillis();
			pressedKeyCode = keyCode;
			return;
		}
//#if polish.android
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			performIconAction(IconActionPerformer.BACK_ACTIONID, null);
			return;
		}
//#endif

		if (action != 0) {
			// handle the fire button same as the Ok button
			if (action ==  Canvas.FIRE) {
				commandAction(OK_CMD, (Displayable) null);
				return;
			} else if (inTabRow) {
				if (action ==  Canvas.LEFT) {
						prevTab();
				} else if (action ==  Canvas.RIGHT) {
					nextTab();
				} else if (action ==  Canvas.DOWN) {
					inTabRow = false;
				} else {
					action = 0; // no game key action
				}
			} else {
				if (action ==  Canvas.LEFT) {
					if (!getActiveMenuPage().changeSelectedColRow(-1, 0)) {
						prevTab();
					}
				} else if (action ==  Canvas.RIGHT) {
					if (!getActiveMenuPage().changeSelectedColRow(1, 0) ) {
						nextTab();
					}
				} else if (action ==  Canvas.DOWN) {
					getActiveMenuPage().changeSelectedColRow(0, 1);
				} else if (action ==  Canvas.UP) {
					if (!getActiveMenuPage().changeSelectedColRow(0, -1)) {
						inTabRow = true;
					}
				} else {
					action = 0; // no game key action
				}
			}
		}
//		// if it was no game key or no handled game key action
//		if( action == 0) {
//			if (keyCode == KEY_NUM1) {
//				prevTab();
//			} else if (keyCode == KEY_NUM3) {
//				nextTab();
//			}
//		}
		repaint();
	}

	protected void keyRepeated(int keyCode) {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS)
			&& (keyCode == KEY_STAR || keyCode == KEY_POUND || keyCode <= KEY_NUM9 && keyCode >= KEY_NUM0)) {
			if ((System.currentTimeMillis() - pressedKeyTime) >= 1000 && pressedKeyCode == keyCode) {
				keyReleased(keyCode);
			}
			} else {
				keyPressed(keyCode);
			}
		}
		
	protected void keyReleased(int keyCode) {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS)) {
			int iconFromKeyCode = -1;
			if (keyCode == KEY_NUM0) {
				iconFromKeyCode = 10;
			} else if (keyCode == KEY_STAR) {
				iconFromKeyCode = 9;
			} else if (keyCode == KEY_POUND) {
				iconFromKeyCode = 11;
			} else if (keyCode <= KEY_NUM9 && keyCode >= KEY_NUM1) {
				iconFromKeyCode = keyCode - KEY_NUM1;
			}
			if (iconFromKeyCode >= 0) {
				if ((System.currentTimeMillis() - pressedKeyTime) >= 1000 && pressedKeyCode == keyCode) {
						setActiveTab(iconFromKeyCode);
						repaint();
				} else {
					if(iconFromKeyCode < getActiveMenuPage().size()){
						performIconAction(getActiveMenuPage().getElementAt(iconFromKeyCode).actionID,
								  getActiveMenuPage().getElementAt(iconFromKeyCode).getText());
					}
				}
			}
		}
	}
	
	protected void pointerReleased(int x, int y) {
		getActiveMenuPage().clearTouchedElement();
		tabButtonManager.clearTouchedElement();
		tabDirectionButtonManager.clearTouchedElement();
		pointerPressedDown = false;
		if (Math.abs(x - touchX) < 10) { // if this is no drag action
			//#debug debug
			logger.debug("pointer released at " + x + " " + y);
			int directionId = tabDirectionButtonManager.getElementIdAtPointer(x, y);
			if (directionId == 0) {
				prevTab();
			} else if (directionId == 1) {
				nextTab();
			} else {
				int newTab = tabButtonManager.getElementIdAtPointer(x, y);
				if (newTab >= 0) {
					setActiveTab(newTab);
				} else {
					int actionId = getActiveMenuPage().getActionIdAtPointer(x, y);
					String choiceName = getActiveMenuPage().getChoiceNameAtPointer(x, y);
					if (actionId >= 0) {
						performIconAction(actionId, choiceName);
						return;
					}
				}
			}
		}

		// reset the dragOffsX of the current tab when releasing the pointer
		getActiveMenuPage().dragOffsX = 0;
		
		// sliding to the right at least a quarter of the display will show the previous menu page
		if ( (x - touchX) > (maxX - minX) / 4) {
			prevTab();
		// sliding to the left at least a quarter of the display will show the next menu page
		} else if ( (touchX - x) > (maxX - minX) / 4) {
			nextTab();
		}

		repaint();
	}
	
	
	protected void pointerPressed(int x, int y) {
		pointerPressedDown = true;
		touchX = x;
		touchY = y;
		LayoutElement e = getActiveMenuPage().getElementAtPointer(x, y);
		if (e != null && e.actionID >= 0) {
			getActiveMenuPage().setTouchedElement(e);
			getActiveMenuPage().setCursor(getActiveMenuPage().getElementIdAtPointer(x, y));
		} else {
			e = tabButtonManager.getElementAtPointer(x, y);
			if (e != null) {
				tabButtonManager.setTouchedElement(e);
			} else {
				e = tabDirectionButtonManager.getElementAtPointer(x, y);
				if (e != null && e != eStatusBar) {
					tabDirectionButtonManager.setTouchedElement(e);
				}
			}
		}
		if (e != null) {
			repaint();
		}
	}
	
	protected void pointerDragged (int x, int y) {
		// return if drag event was not in this canvas but rather the previous one
		if (!pointerPressedDown) {
			return;
		}
		if (Math.abs(x - touchX) < 10) {
			LayoutElement e = getActiveMenuPage().getElementAtPointer(touchX, touchY);
			if (e != null) {
				getActiveMenuPage().setTouchedElement(e);
			}
		} else {
			getActiveMenuPage().clearTouchedElement();
		}
		getActiveMenuPage().dragOffsX = x - touchX;
		repaint();
	}
	

	private void performIconAction(int actionId, String choiceName) {
		//#debug debug
		logger.debug("Perform action " + actionId);
		// Pass the action id to the listening action performer
		if (actionPerformer != null) {
			actionPerformer.performIconAction(actionId, choiceName);
		} else {
			//#debug error
			logger.error(Locale.get("iconmenuwithpagesgui.NoIconActionPerformerSet")/*No IconActionPerformer set!*/);
		}
	}
	
	protected void paint(Graphics g) {
		//#debug debug
		logger.debug("Painting IconMenu");
		
		// Clean the Canvas
		g.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_BACKGROUND]);
		g.fillRect(0, 0, getWidth(), getHeight());

		if (recreateTabButtonsRequired) {
			recreateTabButtons();
		}
		
		LayoutElement e;
		boolean activeTabVisible = false;
		do {
			for (int i=0; i < tabButtonManager.size(); i++) {
				e = tabButtonManager.getElementAt(i);
				if (inTabRow && i == tabNr) {
					e.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT_HIGHLIGHT]); // when in tab button row draw the current tab button in yellow text
				} else {
					e.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT]); // else draw it in white text
				}
				if ( i >= leftMostTabNr) {
					// set the button text, so the LayoutManager knows it has to be drawn
					e.setTextValid();
				} else {
					e.setTextInvalid();
				}
			}
			// recalc positions for currently drawn buttons
			tabButtonManager.recalcPositions();
			
			// if the right button does not fit, scroll the bar
			if (eNextTab.left < tabButtonManager.getElementAt(tabNr).right) {
				leftMostTabNr++;
			} else {
				activeTabVisible = true;
			}
		} while (!activeTabVisible);

		//#debug debug
		logger.debug("  Painting tab buttons");
		// let the layout manager draw the tab buttons
		tabButtonManager.paint(g);

		// draw the direction buttons
		// set flags for directions buttons
		if (tabNr == 0) {
			ePrevTab.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT_INACTIVE]); // grey
		} else {
			ePrevTab.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT]); // white
		}
		if (tabNr == tabButtonManager.size() - 1) {
			eNextTab.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT_INACTIVE]); // grey
		} else {
			eNextTab.setColor(Legend.COLORS[Legend.COLOR_ICONMENU_TABBUTTON_TEXT]); // white
		}
		// clear the area of the right button as it might have been overdrawn by a tab button
		g.setColor(0);
		g.fillRect(eNextTab.left, eNextTab.top, eNextTab.right, eNextTab.bottom);
		ePrevTab.setTextValid();
		eNextTab.setTextValid();
		
		IconMenuPage imp = getActiveMenuPage();
		if (!inTabRow) {
			if (imp.touchedElement != null) {
				eStatusBar.setText(imp.touchedElement.getText());				
			} else {
				eStatusBar.setText(imp.getElementAt(imp.rememberEleId).getText());
			}
		}
		tabDirectionButtonManager.paint(g);
		
		getActiveMenuPage().paint(g, !inTabRow);

		//#debug debug
		logger.debug("Painting IconMenu finished");
	}
}
