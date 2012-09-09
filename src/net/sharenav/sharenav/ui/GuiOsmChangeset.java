/**
 * ShareNav - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net
 * 
 * This file is part of ShareNav
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
package net.sharenav.sharenav.ui;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import de.enough.polish.util.Locale;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import de.enough.polish.util.base64.Base64;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.HttpHelper;
import net.sharenav.util.Logger;

public class GuiOsmChangeset extends Form implements ShareNavDisplayable,
		Runnable, CommandListener, UploadListener {

	private final static Logger logger = Logger.getInstance(
			GuiOsmChangeset.class, Logger.DEBUG);

	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 1);
	private final Command SAVE_CMD = new Command(Locale.get("guiosmchangeset.Save")/*Save*/, Command.OK, 1);

	private UploadListener ul;
	private ShareNavDisplayable parent;
	private String comment;
	private TextField commentField;
	
	private HttpHelper http;

	private int changesetID;

	private boolean closing;

	public GuiOsmChangeset(ShareNavDisplayable parent, UploadListener ul) {
		super("Changeset");
		changesetID = -1;
		this.ul = ul;
		this.parent = parent;
		commentField = new TextField(Locale.get("guiosmchangeset.Comment")/*Comment*/, "", 255, TextField.ANY);
		append(commentField);
		addCommand(BACK_CMD);
		addCommand(SAVE_CMD);
		setCommandListener(this);
	}

	public void show() {
		ShareNav.getInstance().show(this);
	}

	public String toXML() {
		StringBuffer xml = new StringBuffer();
		xml.append("<osm>\n<changeset>\n")
		   .append("<tag k=\"created_by\" v=\"ShareNav_"
				+ Legend.getAppVersion() + "\"/>\n")
		   .append("<tag k=\"comment\" v=\"" + HttpHelper.escapeXML(comment) + "\" />\n")
		   .append("</changeset>\n</osm>\n");
		return xml.toString();
	}

	private void uploadCreate() {
		if (http == null) { 
			http = new HttpHelper();
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
			http = new HttpHelper();
		}
		String url = Configuration.getOsmUrl() + "changeset/" + changesetID
		+ "/close";
		http.uploadData(url, "", true, ul, Configuration.getOsmUsername(), Configuration.getOsmPwd());
		changesetID = -1;
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
				changesetID = -1;
				logger.exception(Locale.get("guiosmchangeset.ReturnedchangesetIDNonNumerical")/*Returned changesetID was non numerical*/, nfe);
				ul.completedUpload(false, Locale.get("guiosmchangeset.NoValidIDReturned")/*No valid changeset ID was returned*/);
				return;
			}
			//#debug info
			logger.info("Successfully created Changeset " + changesetID);
			ul.completedUpload(true, Locale.get("guiosmchangeset.SuccessfullyCreatedChangeset")/*Successfully created Changeset */
					+ changesetID);
		} else {
			changesetID = -1;
			logger.error(Locale.get("guiosmchangeset.FailedToCreateChangeset")/*Failed to create Changeset */ + message);
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
