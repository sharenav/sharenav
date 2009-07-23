/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gps.tools;

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.CompletionListener;
import de.ueller.midlet.gps.Trace;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;


public class CustomMenu {

	private String title;
	private String menuEntries[];
	private int selectedEntry = 0;
	private int commandID = 0;
	private Trace trace;
	private CompletionListener compListener;
	
	private int entriesTop = 0;
	private int entriesLeft = 0;
	private int entriesRight = 0;
	private int entriesHeight = 0;

	public CustomMenu(Trace trace, CompletionListener compListener, String title, String menuEntries[], int commandID) {
		this.trace = trace;
		this.compListener = compListener;
		this.title = title;
		this.menuEntries = menuEntries;
		this.commandID = commandID;
	}
	
	public void setMenuEntries(String menuEntries[]) {
		this.menuEntries = menuEntries;
	}
	
	public void paint(Graphics g) {
		Font font = g.getFont();
		// request same font in bold for title
		Font titleFont = Font.getFont(font.getFace(), Font.STYLE_BOLD, font.getSize());
		int fontHeight = font.getHeight();
		int y = titleFont.getHeight() + 2 + fontHeight; // add menu title height plus extra space of one line for calculation of alertHeight
		int extraWidth = font.charWidth('W'); // extra width for menu
		int menuWidth = titleFont.stringWidth(this.title); // menu is at least as wide as its title
		for (int i=0; i < menuEntries.length; i++) {
			int width= font.stringWidth(menuEntries[i]);
			if (width > menuWidth) {
				menuWidth = width;
			}
		}
		menuWidth += extraWidth;
		int menuHeight=titleFont.getHeight() + (menuEntries.length + 1)* font.getHeight();
		int menuLeft = (trace.getWidth() - menuWidth) / 2; 
		int menuTop = (trace.getHeight() - menuHeight) / 2; 
		// background color
		g.setColor(Legend.COLORS[Legend.COLOR_CUSTOMMENU_BACKGROUND]); 
		g.fillRect(menuLeft, menuTop, menuWidth, menuHeight);
		// color for title
		g.setColor(Legend.COLORS[Legend.COLOR_CUSTOMMENU_TITLE_BACKGROUND]); 
		g.fillRect(menuLeft, menuTop, menuWidth, fontHeight + 3);
		// border
		g.setColor(Legend.COLORS[Legend.COLOR_CUSTOMMENU_BORDER]);
		g.setStrokeStyle(Graphics.SOLID);
		g.drawRect(menuLeft, menuTop, menuWidth, fontHeight + 3); // title border
		g.drawRect(menuLeft, menuTop, menuWidth, menuHeight); // menu border

		y = menuTop + 2;
		g.setFont(titleFont);
		g.drawString(this.title, trace.getWidth()/2, y , Graphics.TOP|Graphics.HCENTER);
		g.setFont(font);
		// output entries 1.5 lines below title
		y += (fontHeight * 3 / 2); 
		entriesTop = y;
		entriesLeft = menuLeft + 1;
		entriesRight = entriesLeft + menuWidth;
		entriesHeight = fontHeight;
		for (int i = 0; i < this.menuEntries.length; i++) {
			if (i == this.selectedEntry) {
				g.setColor(Legend.COLORS[Legend.COLOR_CUSTOMMENU_HIGHLIGHT_BACKGROUND]); 
				g.fillRect(entriesLeft, y, menuWidth - 2, fontHeight);				
				g.setColor(Legend.COLORS[Legend.COLOR_CUSTOMMENU_TEXT]);
			}
			g.drawString(menuEntries[i], menuLeft + extraWidth / 2, y , Graphics.TOP|Graphics.LEFT);	
			y += entriesHeight;
		}
	}				
	
	public void setSelectedEntry(int i) {
		this.selectedEntry = i;
	}	
	
	public int getSelectedEntry() {
		return selectedEntry;
	}

	public int getCommandID() {
		return commandID;
	}
		
	public void increaseSelectedEntry() {
		if (this.selectedEntry < menuEntries.length - 1) {
			this.selectedEntry++;
		}
	}

	public void decreaseSelectedEntry() {
		if (this.selectedEntry > 0) {
			this.selectedEntry--;
		}
	}
	
	public boolean keyAction(int keyCode) {
		int action = trace.getGameAction(keyCode);
		if (action != 0) {
			if (action == Canvas.FIRE) {
				customMenuSelect(true);
				return true;
			} else if (action ==  Canvas.UP) {
				decreaseSelectedEntry();
				return true;
			} else if (action ==  Canvas.DOWN) {
				increaseSelectedEntry();
				return true;
			}
		}
		return false;
	}

	public boolean pointerPressed(int x, int y) {
		if (x < entriesLeft || x > entriesRight || y < entriesTop) {
			return false;
		}
		int selection = (y - entriesTop) / entriesHeight;
		if (selection <= menuEntries.length-1) {
			selectedEntry = selection;
			customMenuSelect(true);
			return true;
		}
		return false;
	}
	
	public void customMenuSelect(boolean performAction) {
		this.compListener.actionCompleted("OK");
	}
	
}