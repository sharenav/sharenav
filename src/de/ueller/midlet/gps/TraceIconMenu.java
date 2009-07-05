/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.LayoutElement;
import de.ueller.gps.tools.LayoutManager;
import de.ueller.gps.tools.IconMenu;
import de.ueller.gps.tools.IconMenuPageInterface;
import de.ueller.gps.tools.IconMenuTabs;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;


public class TraceIconMenu extends IconMenuTabs implements IconMenuPageInterface {

	/** contains the iconMenu for each page */
	private IconMenu[] iconMenuPage = new IconMenu[3];
	/** active element on each page */
	private static int eleNr[] = new int[3];

	private CompletionListener compListener;
	private int minX;
	private int maxX;
	private int minY;
	private int maxY;
	
	
	private static final String[][] iconsMain  =
	{ 	{"satelit", "Start Gps"},	{"cinema", "Search"},	{"museum", "Map Features"},
		{"taxi", "Setup"},			{"fuel", "Tacho"},		{"left", "Online"},
		{"recycling", "Back"},		{"GpsMid", "About"}, {"tunnel_end", "Exit"}
	};
	private static final int[] iconActionsMain  =
	{	Trace.CONNECT_GPS_CMD,	Trace.SEARCH_CMD,		Trace.MAPFEATURES_CMD,
		Trace.SETUP_CMD,		Trace.DATASCREEN_CMD,	Trace.ONLINE_INFO_CMD,
		Trace.BACK_CMD,			Trace.ABOUT_CMD,		Trace.EXIT_CMD
	};

	private static final String[][] iconsRecording =
	{ 	{"restaurant", "Tracks"},	{"mark", "Waypoints"},	{"target", "Save Wpt"},
		{"target", "Rec Track"},	{"museum", "TakePic"},	{"pub", "AudioRec"},
		{"telephone", "Send SMS"},	{"target", "Enter Wpt"}
	};
	private static final int[] iconActionsRecording  =
	{	Trace.MANAGE_TRACKS_CMD,	Trace.MAN_WAYP_CMD,		Trace.SAVE_WAYP_CMD,
		Trace.START_RECORD_CMD,		Trace.CAMERA_CMD,		Trace.TOGGLE_AUDIO_REC,
		Trace.SEND_MESSAGE_CMD,		Trace.ENTER_WAYP_CMD
	};

	
	private static final String[][] iconsRouting  =
	{ 	{"motorway", "Calculate"},	{"target", "Set target"},	{"parking", "Clear Target"}
	};
	private static final int[] iconActionsRouting  =
	{	Trace.ROUTING_TOGGLE_CMD,	Trace.SETTARGET_CMD,	Trace.CLEARTARGET_CMD
	};

	private static final String[] tabLabels = {" Main ", " Recordings ", " Route "};
	private static final String[] tabLabelsSmall = {" Main ", " Rec ", " Route "};
	
	public TraceIconMenu(CompletionListener compListener, int minX, int minY, int maxX, int maxY) {
		super((maxX - minX) < 176 ? tabLabelsSmall : tabLabels, minX, minY, maxX, maxY);
		
		this.compListener = compListener;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
				
		showIconPage(tabNr);
		//validate();		
	}
	
	private void showIconPage(int tabNr) {
		int iconPageTop = minY + pageTabs.ele[0].getFontHeight() + 6;
		// create the iconPage only if it is not cached
		if (iconMenuPage[tabNr] == null) {
			switch(tabNr) {
				case 0:
					iconMenuPage[tabNr] = new IconMenu(this.compListener, this, iconsMain, iconActionsMain, eleNr[0], 3, 4, minX, iconPageTop, maxX, maxY);
					break;
				case 1:
					iconMenuPage[tabNr] = new IconMenu(this.compListener, this, iconsRecording, iconActionsRecording, eleNr[1], 3, 4, minX, iconPageTop, maxX, maxY);
					break;
				case 2:
					iconMenuPage[tabNr] = new IconMenu(this.compListener, this, iconsRouting, iconActionsRouting, eleNr[2], 3, 4, minX, iconPageTop, maxX, maxY);
					break;
			}
		}
		iconMenuPage[tabNr].rememberEleId = eleNr[tabNr];
		setActiveTab(tabNr);
	}
	
	public void iconMenuPageAction(int impAction) {
		super.iconMenuPageAction(impAction);
		switch (impAction) {
			case IMP_ACTION_PREV_TAB:
			case IMP_ACTION_NEXT_TAB:
				showIconPage(tabNr);
				break;
		}
	}

	public void keyAction(int keyCode) {
		if (inTabRow) {
			super.keyAction(keyCode);
			showIconPage(tabNr);
		} else {
			eleNr[tabNr] = iconMenuPage[tabNr].rememberEleId;
			iconMenuPage[tabNr].keyAction(keyCode); 
		}
	}
	
	public void pointerPressed(int x, int y) {
		eleNr[tabNr] = iconMenuPage[tabNr].rememberEleId;
		// if an icon was clicked on the active page hide the page
		if (iconMenuPage[tabNr].pointerPressed(x, y)) {
			visible = false;
		} else {
			int oldTabNr = tabNr;
			super.pointerPressed(x, y);
			if (tabNr != oldTabNr) {
				showIconPage(tabNr);
			}
		}
	}
	
	public void paint(Graphics g) {
		IconMenu im = iconMenuPage[tabNr];
		Trace trace = Trace.getInstance();
		// for commands that can be toggled, fill in the appropriate text and/or actionId
		switch(tabNr) {
			case 0:
				im.ele[0].setText( trace.isGpsConnected() ? "Stop GPS" : "Start GPS");
				im.setIconAction(0, trace.isGpsConnected() ? Trace.DISCONNECT_GPS_CMD : Trace.CONNECT_GPS_CMD);
				break;
			case 1:
				im.ele[3].setText( trace.gpx.isRecordingTrk() ? "Stop Rec" : "Rec Track");
				im.setIconAction(3, trace.gpx.isRecordingTrk() ? Trace.STOP_RECORD_CMD : Trace.START_RECORD_CMD);
				im.ele[5].setText( trace.audioRec.isRecording() ? "Stop AudioRec" : "AudioRec");
				break;
			case 2:
				im.ele[0].setText( (trace.route != null || trace.routeCalc) ? "Stop Route" : "Calculate");				
				break;
		}
		
		iconMenuPage[tabNr].paint(g, !inTabRow);
		super.paint(g);
	}
	
}