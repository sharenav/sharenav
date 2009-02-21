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
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.data.EditableWay;
import de.ueller.midlet.gps.data.OSMdataWay;

public class GUIosmWayDisplay extends Form implements GpsMidDisplayable, CommandListener, UploadListener, ItemCommandListener {

	private final Command EXIT_CMD = new Command("Back", Command.BACK, 1);
	private final Command OK_CMD = new Command("OK", Command.OK, 1);
	private final Command ADD_CMD = new Command("Add tag", Command.ITEM, 2);
	private final Command EDIT_CMD = new Command("Edit tag", Command.ITEM, 2);
	private final Command REMOVE_CMD = new Command("Remove tag", Command.ITEM, 3);
	private final Command REVERSE_CMD = new Command("Reverse direction", Command.ITEM, 4);
	private final Command PRESET_CMD = new Command("Add preset", Command.ITEM, 5);
	private final Command UPLOAD_CMD = new Command("Upload to OSM", Command.OK, 6);
	
	
	private EditableWay eway;
	private SingleTile t;
	private OSMdataWay  osmway;
	private GpsMidDisplayable parent;
	private List presets;
	
	private boolean addTag;
	
	public GUIosmWayDisplay(EditableWay way, SingleTile t, GpsMidDisplayable parent) {
		super("Way " + way.osmID);
		this.parent = parent;
		this.eway = way;
		addCommand(EXIT_CMD);
		addCommand(ADD_CMD);
		addCommand(REVERSE_CMD);
		addCommand(PRESET_CMD);
		addCommand(UPLOAD_CMD);
		setCommandListener(this);
		this.t = t;
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
	
	private void setupScreen() {
		this.deleteAll();
		addTag = false;
		if (osmway == null) {
			this.append(new StringItem("No Data available","..."));
			return;
		}
		this.append(new StringItem("Edited ", null)); this.append(bearingArrow());
		this.append(new StringItem("    at:", osmway.getEditTime()));
		this.append(new StringItem("    by: ", osmway.getEditor()));
		
		Hashtable tags = osmway.getTags();
		if (tags == null)
			return;
		Enumeration keysEn = tags.keys();
		while (keysEn.hasMoreElements()) {
			String key = (String)keysEn.nextElement();
			Item i = new StringItem(key, (String)tags.get(key));
			i.addCommand(EDIT_CMD);
			i.addCommand(REMOVE_CMD);
			i.setItemCommandListener(this);
			this.append(i);
		}
		
	}

	public void commandAction(Command c, Displayable d) {
		if (c == EXIT_CMD) {
			if (d == this) {
				parent.show();
			} else {
				show();
			}
		}
		if (c == ADD_CMD) {
			this.addTag = true;
			TextField tf = new TextField("key","", 100, TextField.ANY);
			tf.addCommand(OK_CMD);
			tf.setItemCommandListener(this);
			this.append(tf);
			Display.getDisplay(GpsMid.getInstance()).setCurrentItem(tf);
			tf = new TextField("value","", 100, TextField.ANY);
			tf.addCommand(OK_CMD);
			tf.setItemCommandListener(this);
			this.append(tf);
		}
		if (c == UPLOAD_CMD) {
			parent.show();
			eway.uploadXML(this);
		}
		
		if (c == REVERSE_CMD) {
			osmway.reverseWay();
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
			presets.addCommand(EXIT_CMD);
			presets.setCommandListener(this);
			presets.setSelectCommand(OK_CMD);
			GpsMid.getInstance().show(presets);
		}
		
		if (c == OK_CMD) {
			String addPreset = presets.getString(presets.getSelectedIndex());
			int split = addPreset.indexOf("=");
			osmway.getTags().put(addPreset.substring(0,split), addPreset.substring(split + 1, addPreset.length()));
			presets = null;
			setupScreen();
			show();
		}
		
	}
	
	public void refresh() {
		eway.loadXML(this);
	}

	public void show() {
		GpsMid.getInstance().show(this);
	}

	public void startProgress(String title) {
		// Not supported/used at the moment.
	}
	
	public void setProgress(String message) {
		// Not supported/used at the moment.
		
	}
	
	public void updateProgress(String message)
	{
		// Not supported/used at the moment.

	}

	public void completedUpload(boolean success, String message) {
		if (success) {
			osmway = eway.getOSMdata();
			setupScreen();
		} else {
			
		}
	}
	
	

	public void uploadAborted() {
		// TODO Auto-generated method stub
		
	}

	public void commandAction(Command c, Item it) {
		System.out.println("Command " + c + " Item " + it);
		Hashtable tags = osmway.getTags();
		if (c == REMOVE_CMD) {
			tags.remove(((StringItem)it).getLabel());
			setupScreen();
		}
		if (c == EDIT_CMD) {
			for (int i = 0; i < this.size(); i++) {
				if (this.get(i) == it) {
					StringItem si = (StringItem)it;
					this.delete(i);
					TextField tf = new TextField(it.getLabel(),si.getText(),100, TextField.ANY);
					tf.addCommand(OK_CMD);
					tf.setItemCommandListener(this);
					this.insert(i, tf);
					Display.getDisplay(GpsMid.getInstance()).setCurrentItem(tf);
				}
			}
		}
		
		if (c == OK_CMD) {
			if (addTag) {
				tags.put(((TextField)this.get(this.size() - 2)).getString(), ((TextField)this.get(this.size() - 1)).getString());
			} else {
				for (int i = 0; i < this.size(); i++) {
					if (this.get(i) == it) {
						TextField tf = (TextField)it;
						tags.put(it.getLabel(), tf.getString());
					}
				}
			}
			addTag = false;
			setupScreen();
		}
	}

}
//#endif