/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */
//#if polish.api.osm-editing
package de.ueller.gpsMid;

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

import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.GuiOSMChangeset;
import de.ueller.midlet.gps.GuiOSMEntityDisplay;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.data.EditableWay;
import de.ueller.midlet.gps.data.OSMdataWay;

public class GUIosmWayDisplay extends GuiOSMEntityDisplay implements GpsMidDisplayable, CommandListener, UploadListener, ItemCommandListener {
	
	private final static Logger logger = Logger.getInstance(GUIosmWayDisplay.class,Logger.DEBUG);

	private final Command REVERSE_CMD = new Command("Reverse direction", Command.ITEM, 4);
	private final Command PRESET_CMD = new Command("Add preset", Command.ITEM, 5);
	
	
	
	private EditableWay eway;
	private SingleTile t;
	private List presets;
	
	public GUIosmWayDisplay(EditableWay way, SingleTile t, GpsMidDisplayable parent) {
		super("Way " + way.osmID, parent);
		this.eway = way;
		addCommand(REVERSE_CMD);
		addCommand(PRESET_CMD);
		this.t = t;
		typeImage = bearingArrow();
	}
	
	private Image bearingArrow() {
		Image img = Image.createImage(16, 16);
		
		float wayBearing = eway.wayBearing(t);
		if (wayBearing < 0)
			wayBearing += 2*Math.PI;
		System.out.println(wayBearing);
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
			if (changesetGui == null) {
				loadState = LOAD_STATE_CHANGESET;
				changesetGui = new GuiOSMChangeset(parent,this);
				changesetGui.show();
			} else {
				loadState = LOAD_STATE_UPLOAD;
				eway.uploadXML(changesetGui.getChangesetID(),this);
			}
		}
		
		if (c == REVERSE_CMD) {
			if (osmentity instanceof OSMdataWay) {
				((OSMdataWay)osmentity).reverseWay();
			}
		}
		
		if (c == PRESET_CMD) {
			presets = new List("Tagging Presets", List.IMPLICIT);
			presets.append("maxspeed=20 mph", null);
			presets.append("maxspeed=30 mph", null);
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
			GpsMid.getInstance().show(presets);
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
		eway.loadXML(this);
	}

	public void completedUpload(boolean success, String message) {
		if (success) {
			if (loadState == LOAD_STATE_CHANGESET) {
				loadState = LOAD_STATE_UPLOAD;
				eway.uploadXML(changesetGui.getChangesetID(),this);
			} else {
				if (GpsMid.getInstance().shouldBeShown() == this) {
					osmentity = eway.getOSMdata();
					setupScreen();
				}
			}
		} else {
			
		}
	}

}
//#endif