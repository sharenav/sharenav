/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 */
package de.ueller.gpsMid;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;

import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.data.EditableWay;

public class GUIosmWayDisplay extends TextBox implements GpsMidDisplayable, CommandListener, UploadListener {

	private final Command EXIT_CMD = new Command("Back", Command.BACK, 5);
	private EditableWay eway;
	private GpsMidDisplayable parent;
	
	public GUIosmWayDisplay(EditableWay way, GpsMidDisplayable parent) {
		super("Way " + way.osmID,"",2048, TextField.ANY);
		this.parent = parent;
		this.eway = way;
		addCommand(EXIT_CMD);
		setCommandListener(this);
	}
	
	public void commandAction(Command c, Displayable d) {
		if (c == EXIT_CMD) {
			parent.show();
		}
	}
	
	public void refresh() {
		eway.loadXML(this);
	}

	public void show() {
		GpsMid.getInstance().show(this);
	}

	public void updateProgress(String message)
	{
		// Not supported/used at the moment.

	}

	public void completedUpload(boolean success, String message) {
		if (success) {
			this.setString(eway.getXML());
		} else {
			
		}
	}

	public void uploadAborted() {
		// TODO Auto-generated method stub
		
	}

}
