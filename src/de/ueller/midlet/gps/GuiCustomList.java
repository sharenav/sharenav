//package de.ueller.midlet.gps;
///*
// * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
// * See Copying
// */
//import java.util.Vector;
// 
//import javax.microedition.lcdui.Canvas;
//import javax.microedition.lcdui.ChoiceGroup;
//import javax.microedition.lcdui.Command;
//import javax.microedition.lcdui.CommandListener;
//import javax.microedition.lcdui.Display;
//import javax.microedition.lcdui.Displayable;
//import javax.microedition.lcdui.Form;
//import javax.microedition.lcdui.Graphics;
//import javax.microedition.lcdui.Image;
//import javax.microedition.lcdui.List;
//import javax.microedition.lcdui.TextField;
//
//
//
//public class GuiCustomList extends Canvas implements CommandListener,
//		GpsMidDisplayable {
//
//	private final static Logger logger = Logger.getInstance(GuiSearch.class,Logger.DEBUG);
//
//	private final Command SELECT = new Command("Select", Command.OK, 1);
//	private final Command DESELECT = new Command("Deselect", Command.OK, 1);
//
//	
//	private Vector entries = new Vector();
//	
//	private int cursor = 0;	
//	private listEntry e = new listEntry();
//	
//	private int topEntryNr;
//	private int numEntriesFitting = 0;
//	private int oldEntriesSize = 0;
//	
//	
//	private boolean oldCmdIsSelect = true;
//	
//	private String sTitle = null;
//	
//	public GuiCustomList(String sTitle, int listStyle) {
//		this.sTitle = sTitle; 
//		setTitle(sTitle);
//		addCommand (SELECT);
//	}
//	
//
//	
//	public void deleteAll() {
//		entries.removeAllElements();
//	}
//
//	public int append(String stringPart, Image imagePart)  {
//		listEntry e = new listEntry();
//		e.name = stringPart;
//		entries.addElement(e);
//		return entries.size();
//	}
//
//	public void commandAction(Command c, Displayable d) {
//		e = (listEntry) entries.elementAt(cursor);
//		if (c == SELECT) {			
//			e.selected = true;
//		}
//		if (c == DESELECT) {			
//			e.selected = false;
//		}
//		repaint(0, 0, getWidth(), getHeight());
//	}
//	
//	public int getSelectedFlags(boolean[] selectedArray_return) {
//		int count = 0;
//		for (int i = 0; i < entries.size(); i++ ) {
//			e = (listEntry) entries.elementAt(i);
//			selectedArray_return[i] = e.selected;
//			if (e.selected) {
//				count++;
//			}
//		}
//		return count;
//	}
// 	
//	public void setSelectedFlags(boolean[] selectedArray) {
//		for (int i = 0; i < entries.size(); i++ ) {
//			e = (listEntry) entries.elementAt(i);
//			e.selected = selectedArray[i]; 
//		}
//	}
// 	 
//	public void set(int elementNum, String stringPart, Image imagePart) {
//		e = (listEntry) entries.elementAt(elementNum);
//		e.name = stringPart;
//	}
//	
//	public void setSelectedIndex(int elementNum, boolean selected) {
//		e = (listEntry) entries.elementAt(elementNum);
//		e.selected = selected;
//	}
//	 
//	public void show() {
//	}
//
//	protected void paint(Graphics gc) {
//		int fontSize = gc.getFont().getHeight();
//		if (oldEntriesSize != entries.size() ) {
//			oldEntriesSize = entries.size();
//			//setTitle (oldEntriesSize + " " + this.sTitle);
//		}
//		if (cursor > entries.size()) {
//			cursor = entries.size();
//		}
//		numEntriesFitting = getHeight() / fontSize;
//		gc.setColor(0, 0, 0);	
//		gc.fillRect(0, 0, getWidth(), getHeight());
//		int y = 0;
//		for (int i = topEntryNr; (i < (topEntryNr + numEntriesFitting)) && (i < entries.size()) ; i++ ) {
//			e = (listEntry) entries.elementAt(i);
//			// cursor bar
//			if (i == cursor) {			
//				gc.setColor(150, 150, 150);
//				int top = (cursor - topEntryNr) * fontSize;
//				gc.fillRect(0, top , getWidth(), fontSize);
//				if (e.selected) {
//					if (oldCmdIsSelect) {
//						removeCommand (SELECT);
//						addCommand (DESELECT);
//						oldCmdIsSelect = false;
//					}
//				} else {
//					if (!oldCmdIsSelect) {
//						removeCommand (DESELECT);
//						addCommand (SELECT);
//						oldCmdIsSelect = true;
//					}
//				}
//			}
//			// change color if selected
//			if (e.selected) {
//				gc.setColor(255, 255, 128);
//			} else {
//				gc.setColor(255 , 255, 255);
//			}
//			gc.drawString(e.name, 0, y, Graphics.TOP | Graphics.LEFT);
//			y += fontSize;
//		}
//	}
//
//	protected void keyPressed(int keyCode) {
//		int action = getGameAction(keyCode);
//		if (action == UP) {
//			if (cursor > 0)
//				cursor--;			
//			if (cursor < topEntryNr) {
//				topEntryNr--;
//			}
//		} else if (action == DOWN) {
//			if (cursor < entries.size() - 1)
//				cursor++;			
//			if (cursor > (topEntryNr + numEntriesFitting - 1) ) {
//				topEntryNr++;
//			}
//		}
//		repaint(0, 0, getWidth(), getHeight());
//		return;
//	}
//	
//	protected void keyRepeated(int keyCode) {
//		//Moving the cursor should work with repeated keys the same
//		//as pressing the key multiple times
//		int action = this.getGameAction(keyCode);
//		if ( (action == UP) || (action == DOWN) ) {
//			keyPressed(keyCode);
//			return;
//        }
//	}
//	
//	private class listEntry {
//		public String name;
//		public boolean selected = false;
//	}
//		
//}
