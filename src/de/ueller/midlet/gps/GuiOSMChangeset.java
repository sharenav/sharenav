/**
 * GpsMid - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net
 * 
 * This file is part of GpsMid
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

//#if polish.api.osm-editing
package de.ueller.midlet.gps;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import de.enough.polish.util.base64.Base64;
import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HTTPhelper;

public class GuiOSMChangeset extends Form implements GpsMidDisplayable,
		Runnable, CommandListener, UploadListener {

	private final static Logger logger = Logger.getInstance(
			GuiOSMChangeset.class, Logger.DEBUG);

	private final Command BACK_CMD = new Command("Back", Command.BACK, 1);
	private final Command SAVE_CMD = new Command("Save", Command.OK, 1);

	private UploadListener ul;
	private GpsMidDisplayable parent;
	private String comment;
	private TextField commentField;
	
	private HTTPhelper http;

	private int changesetID;

	private boolean closing;

	public GuiOSMChangeset(GpsMidDisplayable parent, UploadListener ul) {
		super("Changeset");
		this.ul = ul;
		this.parent = parent;
		commentField = new TextField("Comment", "", 255, TextField.ANY);
		append(commentField);
		addCommand(BACK_CMD);
		addCommand(SAVE_CMD);
		setCommandListener(this);
	}

	public void show() {
		GpsMid.getInstance().show(this);
	}

	public String toXML() {
		StringBuffer xml = new StringBuffer();
		xml.append("<osm>\n<changeset>\n");
		xml.append("<tag k=\"created_by\" v=\"GpsMid_"
				+ Legend.getAppVersion() + "\"/>\n");
		xml.append("<tag k=\"comment\" v=\"" + HTTPhelper.escapeXML(comment) + "\" />\n");
		xml.append("</changeset>\n</osm>\n");
		return xml.toString();
	}

	private void uploadCreate() {
		if (http == null) { 
			http = new HTTPhelper();
		}
		//#debug debug
		logger.debug("Uploading XML for " + this);
		String fullXML = toXML();
		String url = Configuration.getOsmUrl()
				+ "changeset/create";
		http.uploadData(url, fullXML, true, this, Configuration.getOsmUsername(),Configuration.getOsmPwd());
	}

	public void run() {
		if (closing) {
		} else {
			uploadCreate();
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			parent.show();
		}
		if (c == SAVE_CMD) {
			comment = commentField.getString();
			System.out.println(toXML());
			Thread t = new Thread(this);
			closing = false;
			parent.show();
			t.start();

		}

	}

	public void closeChangeset() {
		if (http == null) { 
			http = new HTTPhelper();
		}
		String url = Configuration.getOsmUrl() + "changeset/" + changesetID
		+ "/close";
		http.uploadData(url, "", true, ul, Configuration.getOsmUsername(), Configuration.getOsmPwd());
	}

	public int getChangesetID() {
		return changesetID;
	}

	public void completedUpload(boolean success, String message) {
		if (success) {
			String changeID = http.getData();
			//#debug debug
			logger.debug("Retrieved changeset-id: " + changeID);
			try {
				changesetID = Integer.parseInt(changeID);
			} catch (NumberFormatException nfe) {
				logger.exception("Returned changesetID was non numerical", nfe);
				ul.completedUpload(false, "No valid changeset ID was returned");
				return;
			}
			//#debug info
			logger.info("Successfully created Changeset " + changesetID);
			ul.completedUpload(true, "Successfully created Changeset "
					+ changesetID);
		} else {
			logger.error("Failed to created Changeset " + message);
			ul.completedUpload(false, message);
		}
	}

	public void setProgress(String message) {
		// TODO Auto-generated method stub
		
	}

	public void startProgress(String title) {
		// TODO Auto-generated method stub
		
	}

	public void updateProgress(String message) {
		// TODO Auto-generated method stub
		
	}

	public void updateProgressValue(int increment) {
		// TODO Auto-generated method stub
		
	}

	public void uploadAborted() {
		// TODO Auto-generated method stub
		
	}

}
//#endif