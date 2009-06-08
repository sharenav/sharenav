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
import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.tile.C;

public class GuiOSMChangeset extends Form implements GpsMidDisplayable,
		Runnable, CommandListener {

	private final static Logger logger = Logger.getInstance(
			GuiOSMChangeset.class, Logger.DEBUG);

	private final Command BACK_CMD = new Command("Back", Command.BACK, 1);
	private final Command SAVE_CMD = new Command("Save", Command.OK, 1);

	private UploadListener ul;
	private GpsMidDisplayable parent;
	private String comment;
	private TextField commentField;

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
				+ C.getAppVersion() + "\"/>\n");
		xml.append("<tag k=\"comment\" v=\"" + comment + "\" />\n");
		xml.append("</changeset>\n</osm>\n");
		return xml.toString();
	}

	private void uploadCreate() {
		//#debug debug
		logger.debug("Uploading XML for " + this);
		int respCode = 0;
		String respMessage = null;
		try {
			String fullXML = toXML();
			String url = Configuration.getOsmUrl()
					+ "changeset/create";
			//#debug info
			logger.info("HTTP POST: " + url);
			//#debug debug
			logger.debug("data:\n" + fullXML);
			HttpConnection connection = (HttpConnection) Connector.open(url);
			connection.setRequestMethod(HttpConnection.POST);
			connection.setRequestProperty("Connection", "close");
			connection.setRequestProperty("User-Agent", "GpsMid");
			connection.setRequestProperty("X_HTTP_METHOD_OVERRIDE", "PUT");

			connection.setRequestProperty("Authorization", "Basic "
					+ Base64.encode(Configuration.getOsmUsername() + ":"
							+ Configuration.getOsmPwd()));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(baos);

			osw.write(fullXML);
			osw.flush();
			connection.setRequestProperty("Content-Length", Integer
					.toString(baos.toByteArray().length));
			OutputStream os = connection.openOutputStream();
			os.write(baos.toByteArray());
			os.flush();

			// HTTP Response
			respCode = connection.getResponseCode();
			respMessage = connection.getResponseMessage();

			if (respCode == HttpConnection.HTTP_OK) {
				//#debug debug
				logger.debug("Uploaded successfully, reading response");
				InputStreamReader isr = new InputStreamReader(connection
						.openInputStream());
				char[] buf = new char[16];
				int noRead = isr.read(buf);
				String changeID = new String(buf, 0, noRead);
				//#debug debug
				logger.debug("Retrieved changeset-id: " + changeID);
				changesetID = Integer.parseInt(changeID);
				//#debug info
				logger.info("Successfully created Changeset " + changesetID);
				ul.completedUpload(true, "Successfully created Changeset "
						+ changesetID);
			} else {
				logger.error("Changeset was not created (" + respCode + "): "
						+ respMessage);
				ul.completedUpload(false, "Failed to save changeset");
			}
		} catch (Exception e) {
			logger.exception("Failed to upload way", e);
		}
	}

	private void uploadClose() {
		//#debug debug
		logger.debug("Closing " + this);
		int respCode = 0;
		String respMessage = null;
		try {
			String url = Configuration.getOsmUrl() + "changeset/" + changesetID
					+ "/close?_method=put";
			//#debug info
			logger.info("HTTP POST: " + url);
			HttpConnection connection = (HttpConnection) Connector.open(url);
			connection.setRequestMethod(HttpConnection.POST);
			connection.setRequestProperty("Connection", "close");
			connection.setRequestProperty("User-Agent", "GpsMid");

			connection.setRequestProperty("Authorization", "Basic "
					+ Base64.encode(Configuration.getOsmUsername() + ":"
							+ Configuration.getOsmPwd()));

			// HTTP Response
			respCode = connection.getResponseCode();
			respMessage = connection.getResponseMessage();

			if (respCode == HttpConnection.HTTP_OK) {
				//#debug debug
				logger.debug("Uploaded successfully, reading response");
				ul.completedUpload(true, "Successfully closed Changeset "
						+ changesetID);
			} else {
				logger.error("Changeset was not created (" + respCode + "): "
						+ respMessage);
				ul.completedUpload(false, "Failed to close changeset");
			}
		} catch (Exception e) {
			logger.exception("Failed to upload way", e);
		}
	}

	public void run() {
		if (closing) {
			uploadClose();
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
		closing = true;
		Thread t = new Thread(this);
		t.start();
	}

	public int getChangesetID() {
		return changesetID;
	}

}
//#endif