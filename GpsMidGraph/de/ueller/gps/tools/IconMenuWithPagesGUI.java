package de.ueller.gps.tools;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * 			Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */


import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;

import java.util.Vector;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.tile.C;
import de.ueller.gps.tools.IconMenuPage;


public class IconMenuWithPagesGUI extends Canvas implements CommandListener,
		GpsMidDisplayable {

	private final static Logger logger = Logger.getInstance(IconMenuWithPagesGUI.class,Logger.DEBUG);

	private final Command OK_CMD = new Command("Ok", Command.OK, 1);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);

	private GpsMidDisplayable parent;
	private IconActionPerformer actionPerformer;

	private int minX;
	private int maxX;
	private int minY;
	private int maxY;
	private int tabNr = 0;
	private int leftMostTabNr = 0;
	private boolean inTabRow = false;
	/** LayoutManager for the prev / next direction buttons */
	private LayoutManager tabDirectionButtonManager;
	/** " < " button */
	private LayoutElement ePrevTab;
	/** " > " button */
	private LayoutElement eNextTab;

	/** the tab label buttons */
	private LayoutManager tabButtonManager;
	private boolean recreateTabButtonsRequired = false;
	
	/** the tab Pages with the icons */
	private Vector iconMenuPages = new Vector();
	
	public IconMenuWithPagesGUI(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		// create Canvas
		super();
		this.parent = parent;
		this.actionPerformer = actionPerformer;
		this.minX = 0;
		this.minY = 0;
		this.maxX = getWidth();
		this.maxY = getHeight();
		createTabPrevNextButtons();
		tabButtonManager = new LayoutManager(ePrevTab.right, minY, eNextTab.left, maxY);
		setCommandListener(this);
		addCommands();
	}

	public IconMenuPage createAndAddMenuPage(String pageTitle, int numCols, int numRows) {
		IconMenuPage imp = new IconMenuPage( pageTitle, actionPerformer, numCols, numRows, minX, minY + eNextTab.bottom + 6, maxX, maxY);
		iconMenuPages.addElement(imp);
		recreateTabButtonsRequired = true;		
		return imp;
	}
	
	/** create a layout manager with the direction buttons */
	private void createTabPrevNextButtons() {
		tabDirectionButtonManager = new LayoutManager(minX, minY, maxX, maxY);
		ePrevTab = tabDirectionButtonManager.createAndAddElement(
				LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_FONT_SMALL
		);					
		ePrevTab.setBackgroundColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_BORDER]);
		ePrevTab.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT]);
		ePrevTab.setText( " < ");
		eNextTab = tabDirectionButtonManager.createAndAddElement(
				LayoutElement.FLAG_HALIGN_RIGHT | LayoutElement.FLAG_VALIGN_TOP |
				LayoutElement.FLAG_BACKGROUND_BORDER |
				LayoutElement.FLAG_FONT_SMALL
		);
		eNextTab.setBackgroundColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_BORDER]);
		eNextTab.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT]);
		eNextTab.setText( " > ");
		tabDirectionButtonManager.recalcPositions();
	}

	
	/** recreates the tab buttons for all the iconMenuPages */
	private void recreateTabButtons() {
		tabButtonManager.removeAllElements();
		LayoutElement e = null;
		IconMenuPage imp = null;
		for (int i=0; i < iconMenuPages.size(); i++) {
			imp = (IconMenuPage) iconMenuPages.elementAt(i);
			if (tabButtonManager.size() == 0) {
				e = tabButtonManager.createAndAddElement(
						LayoutElement.FLAG_HALIGN_LEFT | LayoutElement.FLAG_VALIGN_TOP |
						LayoutElement.FLAG_BACKGROUND_BORDER | 
						LayoutElement.FLAG_FONT_SMALL
				);
			// all the other tab buttons are positioned rightto-relative to the previous one
			} else {
				e = tabButtonManager.createAndAddElement(
						LayoutElement.FLAG_HALIGN_RIGHTTO_RELATIVE | LayoutElement.FLAG_VALIGN_TOP |
						LayoutElement.FLAG_BACKGROUND_BORDER |
						LayoutElement.FLAG_FONT_SMALL
				);
				e.setHRelative(tabButtonManager.getElementAt(tabButtonManager.size() - 2));
			}
			e.setBackgroundColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_BORDER]);
			e.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT]);
			e.setText(imp.title);
		}
		setActiveTab(tabNr);
	}
	

	public void show() {
		GpsMid.getInstance().show(this);
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN));
		repaint();
	}
	
	/** adds the commands to this Displayable but no Ok command in full screen mode - press fire there */
	private void addCommands() {
		removeCommand(OK_CMD);
		removeCommand(BACK_CMD);
		if (! Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN)) {
			addCommand(OK_CMD);
		}
		addCommand(BACK_CMD);
	}

	
	protected void sizeChanged(int w, int h) {
		recreateTabButtonsRequired = true;
		IconMenuPage imp = null;
		for (int i=0; i < iconMenuPages.size(); i++) {
			imp = (IconMenuPage) iconMenuPages.elementAt(i);
			imp.maxY = h;
			imp.unloadIcons();
			imp.loadIcons();
			imp.recalcPositions();
		}
		addCommands();
	}
	
	
	public void setActiveTab(int tabNr) {
		// clear the FLAG_BACKGROUND_BOX for all other tab buttons except the current one, where it needs to get set 
		for (int i=0; i < tabButtonManager.size(); i++) {
			if (i == tabNr) {
				tabButtonManager.getElementAt(i).setFlag(LayoutElement.FLAG_BACKGROUND_BOX);
			} else {
				tabButtonManager.getElementAt(i).clearFlag(LayoutElement.FLAG_BACKGROUND_BOX);				
			}
		}
		// load all icons for the new icon page 
		getActiveMenuPage().loadIcons();		
		//#debug debug
		logger.debug("set tab " + tabNr);
		this.tabNr = tabNr;
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
			if (tabNr < leftMostTabNr) {
				leftMostTabNr = tabNr;
			}
			//#debug debug
			logger.debug("prev tab " + tabNr);
		}
		// set flags for tab buttons
		setActiveTab(tabNr);
	}
	
	private IconMenuPage getActiveMenuPage() {
		 return (IconMenuPage) iconMenuPages.elementAt(tabNr);
	}
	
	public void commandAction(Command c, Displayable d) {
		if (c == OK_CMD) {
			if (inTabRow) {
				inTabRow = false;
			} else {
				parent.show();
				performIconAction(getActiveMenuPage().getActiveEleActionId());				
			}
		} else if (c == BACK_CMD) {
			parent.show();
		}
	}
	
	/** process keycodes  */
	protected void keyPressed(int keyCode) {
		//#debug debug
		logger.debug("got key " + keyCode);
		int action = getGameAction(keyCode);
		if (action != 0) {
			if (inTabRow) {
				if (action ==  Canvas.FIRE) {
					inTabRow = false;
				} else if (action ==  Canvas.LEFT) {
						prevTab();
				} else if (action ==  Canvas.RIGHT) {
					nextTab();
				} else if (action ==  Canvas.DOWN) {
					inTabRow = false;
				}
			} else {
				if (action ==  Canvas.FIRE) {
					parent.show();
					performIconAction(getActiveMenuPage().getActiveEleActionId());				
					return;
				} else if (action ==  Canvas.LEFT) {
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
				}
			}
			repaint();
		}
	}

	protected void keyRepeated(int keyCode) {
		keyPressed(keyCode);
	}
	
	protected void pointerPressed(int x, int y) {
		//#debug debug
		logger.debug("pointer pressed at " + x + " " + y);
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
				if (actionId > 0) {
					parent.show();
					performIconAction(actionId);
					return;
				}
			}
		}
		repaint();
	}
	

	private void performIconAction(int actionId) {
		//#debug debug
		logger.debug("perform action " + actionId);		
		// pass the action id to the listening action performer
		actionPerformer.performIconAction(actionId);
	}
	
	protected void paint(Graphics g) {
		//#debug debug
		logger.debug("Painting Icon Menu");
		// clean the Canvas
		g.setColor(C.COLORS[C.COLOR_ICONMENU_BACKGROUND]);
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
					e.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT_HIGHLIGHT]); // when in tab button row draw the current tab button in yellow text
				} else {
					e.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT]); // else draw it in white text				
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
		// let the layout manager draw the tab buttons
		tabButtonManager.paint(g);

		// draw the direction buttons
		// set flags for directions buttons
		if (tabNr == 0) {
			ePrevTab.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT_INACTIVE]); // grey
		} else {
			ePrevTab.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT]); // white										
		}
		if (tabNr == tabButtonManager.size() - 1) {
			eNextTab.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT_INACTIVE]); // grey
		} else {
			eNextTab.setColor(C.COLORS[C.COLOR_ICONMENU_TABBUTTON_TEXT]); // white
		}
		// clear the area of the right button as it might have been overdrawn by a tab button
		g.setColor(0);
		g.fillRect(eNextTab.left, eNextTab.top, eNextTab.right, eNextTab.bottom);
		ePrevTab.setTextValid();
		eNextTab.setTextValid();
		tabDirectionButtonManager.paint(g);
		
		getActiveMenuPage().paint(g, !inTabRow);
	}
}
