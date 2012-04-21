/*
 * GpsMid - Copyright (c) 2008 Jyrki Kuoppala jkpj at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.gpsmid.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;

import de.enough.polish.util.Locale;

import de.ueller.midlet.iconmenu.IconActionPerformer;
import de.ueller.midlet.iconmenu.LayoutElement;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.ui.GpsMid;

public class TouchHelper extends KeyCommandCanvas implements CommandListener,
		GpsMidDisplayable {

	public static volatile TraceLayout tl = null;

	private static final int DISPLAY = 1;
	private static final int SINGLE_TAP = 2;
	private static final int DOUBLE_TAP = 3;
	private static final int LONG_TAP = 4;

	private static final Command CMD_BACK = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 3);
	private GpsMidDisplayable parent;

	private LayoutElement e;

	// one for map, one for lower than, three for selecting mode
	// (singletap, double tap or long tap)
	private static final int addHelpEle = 5;

	public LayoutElement helpEle[] = new LayoutElement[TraceLayout.ELE_COUNT + addHelpEle];

	public TouchHelper(GpsMidDisplayable parent) {
		//super(Locale.get("trace.touchhelp")/*Touchscreen functions*/, List.IMPLICIT);
		setFullScreenMode(Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		addCommand(CMD_BACK);

		// Set up this Displayable to listen to command events
		setCommandListener(this);
		this.parent = parent;

		int w = this.getWidth();
		int h = this.getHeight();

		tl = new TraceLayout(0, 0, w, h);
		createHelpElements();

		repaint();
	}

	protected void keyPressed(int keyCode) {
		parent.show();
		
	}

	public void sizeChanged(int w, int h) {
		tl = new TraceLayout(0, 0, w, h);
		createHelpElements();
		repaint();
	}

	private void createTraceLayoutElements() {
		if (hasPointerEvents()) {
			tl.ele[TraceLayout.TITLEBAR].setText(Locale.get("touchhelp.backhelp"));
		} else {
			tl.ele[TraceLayout.TITLEBAR].setText(Locale.get("touchhelp.displaybackhelp"));
		}
		tl.ele[TraceLayout.SCALEBAR].setText("  ");
		tl.ele[TraceLayout.RECORDINGS].setText("R");
		tl.ele[TraceLayout.POINT_OF_COMPASS].setText("N");
		tl.ele[TraceLayout.SOLUTION].setText("7S");
		tl.ele[TraceLayout.ZOOM_IN].setText("+");
		tl.ele[TraceLayout.ZOOM_OUT].setText("-");
		tl.ele[TraceLayout.RECENTER_GPS].setText("|");
		tl.ele[TraceLayout.SHOW_DEST].setText(">");
		tl.ele[TraceLayout.ROUTE_DISTANCE].setText("5.5km");
		tl.ele[TraceLayout.WAYNAME].setText(" ");
		tl.ele[TraceLayout.CURRENT_TIME].setText("12:34");
		tl.ele[TraceLayout.RECORDINGS].setText("*");
		tl.ele[TraceLayout.SEARCH].setText("_");
	}

	private void createHelpElements() {
		for (int i = 0; i < TraceLayout.ELE_COUNT + addHelpEle; i++) {
			helpEle[i] = new LayoutElement(tl);
		}
		// FIXME problematic thing is these need to be added in the same
		// order as in TraceLayout.java
		tl.addElement(helpEle[TraceLayout.TITLEBAR],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_CENTER |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.TITLEBAR].setVRelative(tl.ele[TraceLayout.WAYNAME]);
		helpEle[TraceLayout.TITLEBAR].setAdditionalOffsY(0);
		helpEle[TraceLayout.TITLEBAR].setAdditionalOffsX(20);

		tl.addElement(helpEle[TraceLayout.SCALEBAR],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.SCALEBAR].setVRelative(tl.ele[TraceLayout.SCALEBAR]);
		helpEle[TraceLayout.SCALEBAR].setAdditionalOffsY(14);
		helpEle[TraceLayout.SCALEBAR].setAdditionalOffsX(8);

		tl.addElement(helpEle[TraceLayout.POINT_OF_COMPASS],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_CENTER |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.POINT_OF_COMPASS].setVRelative(tl.ele[TraceLayout.POINT_OF_COMPASS]);
		helpEle[TraceLayout.POINT_OF_COMPASS].setAdditionalOffsY(14);

		tl.addElement(helpEle[TraceLayout.SOLUTION],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.SOLUTION].setVRelative(tl.ele[TraceLayout.SOLUTION]);
		helpEle[TraceLayout.SOLUTION].setAdditionalOffsY(16);
		helpEle[TraceLayout.SOLUTION].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.ALTITUDE],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ALTITUDE].setVRelative(tl.ele[TraceLayout.ALTITUDE]);
		helpEle[TraceLayout.ALTITUDE].setAdditionalOffsY(16);
		helpEle[TraceLayout.ALTITUDE].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.RECORDED_COUNT],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.RECORDED_COUNT].setVRelative(tl.ele[TraceLayout.RECORDED_COUNT]);
		helpEle[TraceLayout.RECORDED_COUNT].setAdditionalOffsY(16);
		helpEle[TraceLayout.RECORDED_COUNT].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.CELLID],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.CELLID].setVRelative(tl.ele[TraceLayout.CELLID]);
		helpEle[TraceLayout.CELLID].setAdditionalOffsY(16);
		helpEle[TraceLayout.CELLID].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.AUDIOREC],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.AUDIOREC].setVRelative(tl.ele[TraceLayout.AUDIOREC]);
		helpEle[TraceLayout.AUDIOREC].setAdditionalOffsY(16);
		helpEle[TraceLayout.AUDIOREC].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.WAYNAME],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.WAYNAME].setVRelative(tl.ele[TraceLayout.WAYNAME]);
		helpEle[TraceLayout.WAYNAME].setAdditionalOffsY(16);
		helpEle[TraceLayout.WAYNAME].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.ROUTE_INTO],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ROUTE_INTO].setVRelative(tl.ele[TraceLayout.ROUTE_INTO]);
		helpEle[TraceLayout.ROUTE_INTO].setAdditionalOffsY(8);
		helpEle[TraceLayout.ROUTE_INTO].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.ROUTE_INSTRUCTION],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ROUTE_INSTRUCTION].setVRelative(tl.ele[TraceLayout.ROUTE_INSTRUCTION]);
		helpEle[TraceLayout.ROUTE_INSTRUCTION].setAdditionalOffsY(8);
		helpEle[TraceLayout.ROUTE_INSTRUCTION].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.ROUTE_DISTANCE],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ROUTE_DISTANCE].setVRelative(tl.ele[TraceLayout.ROUTE_DISTANCE]);
		helpEle[TraceLayout.ROUTE_DISTANCE].setAdditionalOffsY(8);
		helpEle[TraceLayout.ROUTE_DISTANCE].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.SPEED_CURRENT],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.SPEED_CURRENT].setVRelative(tl.ele[TraceLayout.SPEED_CURRENT]);
		helpEle[TraceLayout.SPEED_CURRENT].setAdditionalOffsY(8);
		helpEle[TraceLayout.SPEED_CURRENT].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.CURRENT_TIME],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.CURRENT_TIME].setVRelative(tl.ele[TraceLayout.CURRENT_TIME]);
		helpEle[TraceLayout.CURRENT_TIME].setAdditionalOffsY(-14);
		helpEle[TraceLayout.CURRENT_TIME].setAdditionalOffsX(-8);

		tl.addElement(helpEle[TraceLayout.ETA],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ETA].setVRelative(tl.ele[TraceLayout.ETA]);
		helpEle[TraceLayout.ETA].setAdditionalOffsY(-20);
		helpEle[TraceLayout.ETA].setAdditionalOffsX(-8);

		tl.addElement(helpEle[TraceLayout.ROUTE_OFFROUTE],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ROUTE_OFFROUTE].setVRelative(tl.ele[TraceLayout.ROUTE_OFFROUTE]);
		helpEle[TraceLayout.ROUTE_OFFROUTE].setAdditionalOffsY(-20);
		helpEle[TraceLayout.ROUTE_OFFROUTE].setAdditionalOffsX(-8);

		tl.addElement(helpEle[TraceLayout.ZOOM_OUT],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ZOOM_OUT].setVRelative(tl.ele[TraceLayout.ZOOM_OUT]);
		helpEle[TraceLayout.ZOOM_OUT].setAdditionalOffsY(4);
		helpEle[TraceLayout.ZOOM_OUT].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.ZOOM_IN],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ZOOM_IN].setVRelative(tl.ele[TraceLayout.ZOOM_IN]);
		helpEle[TraceLayout.ZOOM_IN].setAdditionalOffsY(16);
		helpEle[TraceLayout.ZOOM_IN].setAdditionalOffsX(-24);

		tl.addElement(helpEle[TraceLayout.SHOW_DEST],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.SHOW_DEST].setVRelative(tl.ele[TraceLayout.SHOW_DEST]);
		helpEle[TraceLayout.SHOW_DEST].setAdditionalOffsY(16);
		helpEle[TraceLayout.SHOW_DEST].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.RECENTER_GPS],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.RECENTER_GPS].setVRelative(tl.ele[TraceLayout.RECENTER_GPS]);
		helpEle[TraceLayout.RECENTER_GPS].setAdditionalOffsY(4);
		helpEle[TraceLayout.RECENTER_GPS].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.RECORDINGS],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.RECORDINGS].setVRelative(tl.ele[TraceLayout.RECORDINGS]);
		helpEle[TraceLayout.RECORDINGS].setAdditionalOffsY(8);
		helpEle[TraceLayout.RECORDINGS].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.SEARCH],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.SEARCH].setVRelative(tl.ele[TraceLayout.SEARCH]);
		helpEle[TraceLayout.SEARCH].setAdditionalOffsY(8);
		helpEle[TraceLayout.SEARCH].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.SPEEDING_SIGN],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.SPEEDING_SIGN].setVRelative(tl.ele[TraceLayout.SPEEDING_SIGN]);
		helpEle[TraceLayout.SPEEDING_SIGN].setAdditionalOffsY(8);
		helpEle[TraceLayout.SPEEDING_SIGN].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.REQUESTED_TILES],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.REQUESTED_TILES].setVRelative(tl.ele[TraceLayout.REQUESTED_TILES]);
		helpEle[TraceLayout.REQUESTED_TILES].setAdditionalOffsY(8);
		helpEle[TraceLayout.REQUESTED_TILES].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.TRAVEL_MODE],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.TRAVEL_MODE].setVRelative(tl.ele[TraceLayout.TRAVEL_MODE]);
		helpEle[TraceLayout.TRAVEL_MODE].setAdditionalOffsY(8);
		helpEle[TraceLayout.TRAVEL_MODE].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.ELE_COUNT],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_LEFT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ELE_COUNT].setVRelative(tl.ele[TraceLayout.SHOW_DEST]);
		helpEle[TraceLayout.ELE_COUNT].setAdditionalOffsY(0);
		helpEle[TraceLayout.ELE_COUNT].setAdditionalOffsX(28);

		tl.addElement(helpEle[TraceLayout.ELE_COUNT+1],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_CENTER |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ELE_COUNT+1].setVRelative(tl.ele[TraceLayout.TITLEBAR]);
		helpEle[TraceLayout.ELE_COUNT+1].setAdditionalOffsY(60);
		helpEle[TraceLayout.ELE_COUNT+1].setAdditionalOffsX(-28);

		tl.addElement(helpEle[TraceLayout.ELE_COUNT+2],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ELE_COUNT+2].setVRelative(tl.ele[TraceLayout.TITLEBAR]);
		helpEle[TraceLayout.ELE_COUNT+2].setAdditionalOffsY(4);
		helpEle[TraceLayout.ELE_COUNT+2].setAdditionalOffsX(-28);

		tl.addElement(helpEle[TraceLayout.ELE_COUNT+3],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ELE_COUNT+3].setVRelative(tl.ele[TraceLayout.TITLEBAR]);
		helpEle[TraceLayout.ELE_COUNT+3].setAdditionalOffsY(4);
		helpEle[TraceLayout.ELE_COUNT+3].setAdditionalOffsX(-28);

		tl.addElement(helpEle[TraceLayout.ELE_COUNT+4],
			      LayoutElement.FLAG_FONT_MEDIUM |
			      LayoutElement.FLAG_VALIGN_WITH_RELATIVE |
			      LayoutElement.FLAG_HALIGN_RIGHT |
			      LayoutElement.FLAG_TRANSPARENT_BACKGROUND_BOX);
		helpEle[TraceLayout.ELE_COUNT+4].setVRelative(tl.ele[TraceLayout.TITLEBAR]);
		helpEle[TraceLayout.ELE_COUNT+4].setAdditionalOffsY(4);
		helpEle[TraceLayout.ELE_COUNT+4].setAdditionalOffsX(-28);
	}

	protected void paint(Graphics g) {
		//int w = (this.getWidth() * 125) / 100;
		//int h = (this.getHeight() * 125) / 100;
		int w = this.getWidth();
		int h = this.getHeight();

		// FIXME use COLOR_HELP_BACKGROUND?
		g.setColor(Legend.COLORS[Legend.COLOR_MAP_BACKGROUND]);
		g.fillRect(0, 0, w, h);
		createTraceLayoutElements();
		if (hasPointerEvents()) {
			setSingleTap();
		} else {
			setDisplay();
		}
		tl.paint(g);
	}
	private void setDisplay() {
		helpEle[TraceLayout.ROUTE_DISTANCE].setText(Locale.get("displayhelp.Distance"));
		helpEle[TraceLayout.CURRENT_TIME].setText(Locale.get("displayhelp.Clock"));
		helpEle[TraceLayout.SCALEBAR].setText(Locale.get("displayhelp.ScaleBar"));
		helpEle[TraceLayout.POINT_OF_COMPASS].setText(Locale.get("displayhelp.Compass"));
		helpEle[TraceLayout.SOLUTION].setText(Locale.get("displayhelp.Solution"));
		helpEle[TraceLayout.TITLEBAR].setText(Locale.get("displayhelp.StatusBar"));
		helpEle[TraceLayout.WAYNAME].setText(Locale.get("displayhelp.WayName"));
		//helpEle[TraceLayout.ROUTE_INSTRUCTION].setText(Locale.get("displayhelp.routeinst"));
	}
	private void setSingleTap() {
		helpEle[TraceLayout.ZOOM_IN].setText(Locale.get("touchhelp.plus"));
		helpEle[TraceLayout.ZOOM_OUT].setText(Locale.get("touchhelp.minus"));
		helpEle[TraceLayout.RECENTER_GPS].setText(Locale.get("touchhelp.pipe"));
		helpEle[TraceLayout.RECORDINGS].setText(Locale.get("touchhelp.star"));
		helpEle[TraceLayout.SEARCH].setText(Locale.get("touchhelp.us"));
		helpEle[TraceLayout.ROUTE_DISTANCE].setText(Locale.get("touchhelp.Distance"));
		//#if polish.android
		helpEle[TraceLayout.CURRENT_TIME].setText(Locale.get("touchhelp.ClockAndroid"));
		//#else
		helpEle[TraceLayout.CURRENT_TIME].setText(Locale.get("touchhelp.Clock"));
		//#endif
		helpEle[TraceLayout.SHOW_DEST].setText(Locale.get("touchhelp.lt"));
		helpEle[TraceLayout.SCALEBAR].setText(Locale.get("touchhelp.ScaleBar"));
		helpEle[TraceLayout.POINT_OF_COMPASS].setText(Locale.get("touchhelp.Compass"));
		//helpEle[TraceLayout.SOLUTION].setText(Locale.get("touchhelp.Solution"));
		helpEle[TraceLayout.TITLEBAR].setText(Locale.get("touchhelp.StatusBar"));
		helpEle[TraceLayout.ELE_COUNT].setText(Locale.get("touchhelp.gt"));
		helpEle[TraceLayout.ELE_COUNT+1].setText(Locale.get("touchhelp.Map"));
	}

	protected void pointerPressed(int x, int y) {
		// remember the LayoutElement the pointer is pressed down at, this will also highlight it on the display
		int touchedElementId = tl.getElementIdAtPointer(x, y);
		if (touchedElementId > TraceLayout.ELE_COUNT) {
			touchedElementId -= (TraceLayout.ELE_COUNT);
		}
		if (touchedElementId >= 0) {
			tl.setTouchedElement((LayoutElement) tl.elementAt(touchedElementId));
			//tl.setTouchedElement(helpEle[touchedElementId]);
			if (touchedElementId < helpEle.length) {
				helpEle[touchedElementId].setTouched(true);
			}
			repaint();
		}
	}

	protected void pointerReleased(int x, int y) {	
		// releasing the pointer will clear the highlighting of the touched element
		LayoutElement e = tl.getTouchedElement();
		int eleId = tl.getElementId(e);
		if (e != null) {
			tl.clearTouchedElement();
			if (eleId > TraceLayout.ELE_COUNT) {
				eleId -= TraceLayout.ELE_COUNT;
			}
			if (eleId != -1 && eleId < helpEle.length) {
				helpEle[eleId].setTouched(false);
				GpsMid.getInstance().alert(Locale.get("traceiconmenu.HelpPage"),
							   helpEle[eleId].getText(), 20000);
			}
			repaint();
			if (e == tl.ele[TraceLayout.TITLEBAR]) {
				parent.show();
			}
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == CMD_BACK) {
			parent.show();
		}
	}

	public void show() {
		GpsMid.getInstance().show(this);
	}

}
