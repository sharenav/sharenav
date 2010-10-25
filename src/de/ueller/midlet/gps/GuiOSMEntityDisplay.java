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
package de.ueller.midlet.gps;

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
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.GuiOSMChangeset;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.data.OSMdataEntity;

import de.enough.polish.util.Locale;

public abstract class GuiOSMEntityDisplay extends Form implements GpsMidDisplayable, CommandListener, UploadListener, ItemCommandListener {
	
	private final static Logger logger = Logger.getInstance(GuiOSMEntityDisplay.class,Logger.DEBUG);

	protected final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 1);
	protected final Command OK_CMD = new Command(Locale.get("generic.OK")/*OK*/, Command.OK, 1);
	protected final Command ADD_CMD = new Command(Locale.get("guiosmentitydisplay.AddTag")/*Add tag*/, Command.ITEM, 2);
	protected final Command EDIT_CMD = new Command(Locale.get("guiosmentitydisplay.EditTag")/*Edit tag*/, Command.ITEM, 2);
	protected final Command REMOVE_CMD = new Command(Locale.get("guiosmentitydisplay.RemoveTag")/*Remove tag*/, Command.ITEM, 3);
	protected final Command UPLOAD_CMD = new Command(Locale.get("guiosmentitydisplay.UploadToOSM")/*Upload to OSM*/, Command.OK, 6);
	protected final Command CREATE_CHANGE_CMD = new Command(Locale.get("guiosmentitydisplay.CreateChangeset")/*Create changeset*/, Command.OK, 6);
	protected final Command CLOSE_CHANGE_CMD = new Command(Locale.get("guiosmentitydisplay.CloseChangeset")/*Close changeset*/, Command.OK, 6);
	
	protected final static int LOAD_STATE_NONE = 0;
	protected final static int LOAD_STATE_LOAD = 1;
	protected final static int LOAD_STATE_UPLOAD = 2;
	protected final static int LOAD_STATE_CHANGESET = 3;
	
	
	protected GpsMidDisplayable parent;
	protected static GuiOSMChangeset changesetGui;
	protected int loadState;
	
	protected OSMdataEntity osmentity;
	protected Image typeImage;
	
	protected boolean addTag;
	
	public GuiOSMEntityDisplay(String title, GpsMidDisplayable parent) {
		super(title);
		this.parent = parent;
		addCommand(BACK_CMD);
		addCommand(ADD_CMD);
		addCommand(UPLOAD_CMD);
		addCommand(CREATE_CHANGE_CMD);
		addCommand(CLOSE_CHANGE_CMD);
		setCommandListener(this);
		
		osmentity = null;
	}

	protected void setupScreen() {
		try {
			this.deleteAll();
			addTag = false;
			if (osmentity == null) {
				this.append(new StringItem(Locale.get("guiosmentitydisplay.NoDataAvailable")/*No Data available*/,"..."));
				return;
			}
			if (osmentity.getVersion() > 0) {
				this.append(new StringItem(Locale.get("guiosmentitydisplay.Edited")/*Edited */, null));
				this.append(new StringItem(Locale.get("guiosmentitydisplay.at")/*    at:*/, osmentity.getEditTime()));
				this.append(new StringItem(Locale.get("guiosmentitydisplay.by")/*    by:*/, osmentity.getEditor()));
				this.append(new StringItem(Locale.get("guiosmentitydisplay.ver")/*    ver:*/, Integer.toString(osmentity.getVersion())));
			}

			Hashtable tags = osmentity.getTags();
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
		} catch (Exception e) { 
			logger.exception(Locale.get("guiosmentitydisplay.InitialisingEntityTagScreenFailed")/*Initialising entity tag screen failed: */ , e);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			if (d == this) {
				parent.show();
			} else {
				show();
			}
		}
		if (c == ADD_CMD) {
			this.addTag = true;
			TextField tf = new TextField(Locale.get("guiosmentitydisplay.key")/*key*/,"", 100, TextField.ANY);
			tf.addCommand(OK_CMD);
			tf.setItemCommandListener(this);
			this.append(tf);
			Display.getDisplay(GpsMid.getInstance()).setCurrentItem(tf);
			tf = new TextField(Locale.get("guiosmentitydisplay.value")/*value*/,"", 100, TextField.ANY);
			tf.addCommand(OK_CMD);
			tf.setItemCommandListener(this);
			this.append(tf);
		}
		
		if (c == CREATE_CHANGE_CMD) {
			changesetGui = new GuiOSMChangeset(this,this);
			changesetGui.show();
		}
		
		if (c == CLOSE_CHANGE_CMD) {
			if (changesetGui == null) {
				logger.error(Locale.get("guiosmentitydisplay.NoChangesetIsCurrentlyOpen")/*No changeset is currently open*/);
			} else {
				changesetGui.closeChangeset();
				changesetGui = null;
			}
		}

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

	public abstract void completedUpload(boolean success, String message);
	

	public void uploadAborted() {
		// TODO Auto-generated method stub
		
	}
	
	public void updateProgressValue(int value) {
		// TODO Auto-generated method stub
		
	}

	public void commandAction(Command c, Item it) {
		//#debug info
		logger.info("Command " + c + " Item " + it);
		Hashtable tags = osmentity.getTags();
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
	
	public void refresh() {
		
	}



}
//#endif
