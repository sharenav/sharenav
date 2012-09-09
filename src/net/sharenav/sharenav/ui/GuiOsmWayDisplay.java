/**
 * This file is part of ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */
//#if polish.api.osm-editing
package net.sharenav.sharenav.ui;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.EditableWay;
import net.sharenav.sharenav.data.OsmDataWay;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.sharenav.ui.GuiOsmChangeset;
import net.sharenav.sharenav.ui.GuiOsmEntityDisplay;
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.HttpHelper;
import net.sharenav.util.Logger;

//#if polish.android
import de.enough.polish.android.midlet.MidletBridge;
//#endif
//#if polish.api.online
//#endif

import de.enough.polish.util.Locale;

public class GuiOsmWayDisplay extends GuiOsmEntityDisplay implements ShareNavDisplayable, CommandListener, UploadListener, ItemCommandListener {
	
	private final static Logger logger = Logger.getInstance(GuiOsmWayDisplay.class,Logger.DEBUG);

	private final Command REVERSE_CMD = new Command(Locale.get("guiosmwaydisplay.ReverseDirection")/*Reverse direction*/, Command.ITEM, 4);
	private final Command PRESET_CMD = new Command(Locale.get("guiosmwaydisplay.AddPreset")/*Add preset*/, Command.ITEM, 5);
	
	//#if polish.api.online
	private UploadListener ul;
	private HttpHelper http = null;
	//#endif
	private EditableWay eway;
	private int wayID;
	private SingleTile t;
	private List presets;
	
	public GuiOsmWayDisplay(EditableWay way, SingleTile t, ShareNavDisplayable parent) {
	        super(Locale.get("guiosmwaydisplay.Way")/*Way */ + way.osmID, parent);
		this.eway = way;
		addCommand(REVERSE_CMD);
		addCommand(PRESET_CMD);
		this.t = t;
		typeImage = bearingArrow();
	}
	
	public GuiOsmWayDisplay(long wayID, ShareNavDisplayable parent) {
	        super(Locale.get("guiosmwaydisplay.Way")/*Way */ + wayID, parent);
		this.wayID = (int) wayID;
		osmentity = new OsmDataWay(this.wayID);
		//addCommand(REVERSE_CMD);
		//addCommand(PRESET_CMD);
		//typeImage = bearingArrow();
	}
	
	private Image bearingArrow() {
		Image img = Image.createImage(16, 16);
		
		float wayBearing = eway.wayBearing(t);
		if (wayBearing < 0)
			wayBearing += 2*Math.PI;
//		System.out.println(wayBearing);
		double x0,y0,x1,y1,x2,y2;
		
		x0 =  0; y0 = -6;
		y1 = y0*Math.cos(wayBearing) - x0*Math.sin(wayBearing) + 8;
		x1 = x0*Math.cos(wayBearing) - y0*Math.sin(wayBearing) + 8;
		x0 =  0; y0 = 6;
		y2 = y0*Math.cos(wayBearing) - x0*Math.sin(wayBearing) + 8;
		x2 = x0*Math.cos(wayBearing) - y0*Math.sin(wayBearing) + 8;
		img.getGraphics().drawLine((int)x1, (int)y1, (int)x2, (int)y2);
		
		x0 = -4; y0 = -2;
		y1 = y0*Math.cos(wayBearing) - x0*Math.sin(wayBearing) + 8;
		x1 = x0*Math.cos(wayBearing) - y0*Math.sin(wayBearing) + 8;
		x0 =  0; y0 = -6;
		y2 = y0*Math.cos(wayBearing) - x0*Math.sin(wayBearing) + 8;
		x2 = x0*Math.cos(wayBearing) - y0*Math.sin(wayBearing) + 8;
		img.getGraphics().drawLine((int)x1, (int)y1, (int)x2, (int)y2);
		
		x0 =  4; y0 = -2;
		y1 = y0*Math.cos(wayBearing) - x0*Math.sin(wayBearing) + 8;
		x1 = x0*Math.cos(wayBearing) - y0*Math.sin(wayBearing) + 8;
		x0 =  0; y0 = -6;
		y2 = y0*Math.cos(wayBearing) - x0*Math.sin(wayBearing) + 8;
		x2 = x0*Math.cos(wayBearing) - y0*Math.sin(wayBearing) + 8;
		img.getGraphics().drawLine((int)x1, (int)y1, (int)x2, (int)y2);
		
		return img;
	}
	

	public void commandAction(Command c, Displayable d) {
		super.commandAction(c, d);

		if (c == UPLOAD_CMD) {
			parent.show();
			if ((changesetGui == null) || changesetGui.getChangesetID() < 0) {
				loadState = LOAD_STATE_CHANGESET;
				changesetGui = new GuiOsmChangeset(parent,this);
				changesetGui.show();
			} else {
				loadState = LOAD_STATE_UPLOAD;
				eway.uploadXML(changesetGui.getChangesetID(),this);
			}
		}
		
		if (c == REVERSE_CMD) {
			if (osmentity instanceof OsmDataWay) {
				((OsmDataWay)osmentity).reverseWay();
			}
		}
		
		if (c == PRESET_CMD) {
		        presets = new List(Locale.get("guiosmwaydisplay.TaggingPresets")/*Tagging Presets*/, List.IMPLICIT);
			presets.append(Locale.get("guiosmwaydisplay.maxspeed20mph")/*maxspeed=20 mph*/, null);
			presets.append(Locale.get("guiosmwaydisplay.maxspeed30mph")/*maxspeed=30 mph*/, null);
			presets.append("oneway=yes", null);
			presets.append("name=", null);
			presets.append("ref=", null);
			presets.append("lcn_ref=", null);
			presets.append("foot=yes", null);
			presets.append("bicycle=yes", null);
			presets.append("motorcar=yes", null);
			presets.append("access=private", null);
			presets.append("maxweight=7.5", null);
			presets.append("maxlength=18", null);
			presets.append("maxwidth=3", null);
			presets.append("maxheigt=4", null);
			presets.addCommand(OK_CMD);
			presets.addCommand(BACK_CMD);
			presets.setCommandListener(this);
			presets.setSelectCommand(OK_CMD);
			ShareNav.getInstance().show(presets);
		}

		if (c == OK_CMD) {
			String addPreset = presets.getString(presets.getSelectedIndex());
			int split = addPreset.indexOf("=");
			osmentity.getTags().put(addPreset.substring(0,split), addPreset.substring(split + 1, addPreset.length()));
			presets = null;
			setupScreen();
			show();
		}
		
	}
	
	public void refresh() {
		//#if polish.api.online
		if (eway != null) {
			eway.loadXML(this);
		} else {
			//#debug debug
			logger.debug("Retrieving XML for " + this);
			this.ul = ul;
			loadState = LOAD_STATE_LOAD;
			String url = Configuration.getOsmUrl() + "way/" + wayID;
			if (http == null) { 
				http = new HttpHelper();
			}
			http.getURL(url, this);
			//#endif
		}
	}

	public void completedUpload(boolean success, String message) {
		if (success) {
			if (loadState == LOAD_STATE_CHANGESET) {
				loadState = LOAD_STATE_UPLOAD;
				eway.uploadXML(changesetGui.getChangesetID(),this);
			} else if (loadState == LOAD_STATE_LOAD) {
				osmentity = new OsmDataWay(http.getData(), wayID);
				//#if polish.android
				runSetupScreen();
				//#else
				setupScreen();
				//#endif
				loadState = LOAD_STATE_NONE;
			} else {
				if (ShareNav.getInstance().shouldBeShown() == this) {
					osmentity = eway.getOSMdata();
					//#if polish.android
					runSetupScreen();
					//#else
					setupScreen();
					//#endif
				}
			}
		} else {
			
		}
	}

	//#if polish.android
	public void runSetupScreen() {
		MidletBridge.instance.runOnUiThread(
			new Runnable() {
				public void run() {
					setupScreen();
				}
			});
	}
	//#endif
}
//#endif