/*
 * GpsMid - Copyright (c) 2009 Kai Krueger apm at users dot sourceforge dot net 
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
package de.ueller.midlet.gps;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import de.enough.polish.util.base64.Base64;
import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.importexport.ExportSession;

public class GuiGPXOSMUpload extends Form implements GpsMidDisplayable, CommandListener, ExportSession{
	
	private final static Logger logger = Logger.getInstance(GuiGPXOSMUpload.class,Logger.DEBUG);
	
	private final static Command BACK_CMD = new Command("Cancel", Command.BACK, 2);
	private final static Command OK_CMD = new Command("Upload", Command.OK, 1);
	
	private TextField descriptionTF;
	private TextField tagsTF;
	private ChoiceGroup publicCG;
	
	private String description;
	private String tags;
	
	private boolean proceed = false;
	
	private ByteArrayOutputStream gpxOS;
	
	private String url;
	private String name;
	private boolean publicFlag;

	public GuiGPXOSMUpload() {
		super("Gpx upload to OSM");
		
		descriptionTF = new TextField("Description:","", 255, TextField.ANY);
		tagsTF = new TextField("Tags:","", 255, TextField.ANY);
		String [] items = new String[1];
		items[0] = "Public";
		publicCG = new ChoiceGroup("",Choice.MULTIPLE,items,null);
		
		this.append(descriptionTF);
		this.append(tagsTF);
		this.append(publicCG);
		this.addCommand(OK_CMD);
		this.addCommand(BACK_CMD);
		this.setCommandListener(this);
	}
	
	public void upload() {
		logger.info("Uploading OSM GPX");
		try {
			HttpConnection connection = (HttpConnection) Connector
			.open(url);
			System.out.println("Connection: " + connection);
			connection.setRequestMethod(HttpConnection.POST);
			connection.setRequestProperty("Connection", "close");
			connection.setRequestProperty("User-Agent", "GpsMid");
			
			
			connection.setRequestProperty("Authorization", "Basic " + Base64.encode(Configuration.getOsmUsername() +":" + Configuration.getOsmPwd()));
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(baos);
			
			
			osw.write("-----------------------------12132519071893744613145780879\r\n");
			osw.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + name + "\"\r\n");
			osw.write("Content-Type: application/octet-stream\r\n\r\n");
			osw.write(new String(gpxOS.toByteArray()));
			osw.write("-----------------------------12132519071893744613145780879\r\n");
			osw.write("Content-Disposition: form-data; name=\"tags\"\r\n\r\n");
			osw.write(tags + "\r\n");
			osw.write("-----------------------------12132519071893744613145780879\r\n");
			osw.write("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
			osw.write(description + "\r\n");
			osw.write("-----------------------------12132519071893744613145780879\r\n");
			osw.write("Content-Disposition: form-data; name=\"public\"\r\n\r\n");
			osw.write(publicFlag?"1":"0" + "\r\n");
			osw.write("-----------------------------12132519071893744613145780879--\r\n");
			osw.flush();
			connection.setRequestProperty("Content-Length", Integer.toString(baos.toByteArray().length));
			connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=---------------------------12132519071893744613145780879");
			OutputStream os = connection.openOutputStream();
			os.write(baos.toByteArray());
			os.flush();
			
			
			// HTTP Response
			if (connection.getResponseCode() == HttpConnection.HTTP_OK) {
				logger.info("Successfully uploaded GPX");
				
			} else {
				logger.error("Failed to upload GPX (" + connection.getResponseCode() + "): " + connection.getResponseMessage());
			}
		} catch (Exception e) {
			logger.exception("Failed uploading GPX", e);
		}
	}

	public void show() {
		GpsMid.getInstance().show(this);
		
	}

	public synchronized void commandAction(Command c, Displayable d) {
		if (c == OK_CMD) {
			
			description = descriptionTF.getString();
			tags = tagsTF.getString();
			boolean[] publicFlags = new boolean[1];
			publicCG.getSelectedFlags(publicFlags);
			publicFlag = publicFlags[0];
			proceed = true;
			logger.info("Uploading GPX: desc=" + description + " tags=" + tags + " public=" + publicFlag);
			GpsMid.getInstance().showPreviousDisplayable();
			notifyAll();
		}
		if (c == BACK_CMD) {
			proceed = false;
			GpsMid.getInstance().showPreviousDisplayable();
			notifyAll();
		}
		
	}

	public void closeSession() {
		upload();
	}

	public OutputStream openSession(String url, String name) {
		this.url = url;
		this.name = name;
		show();
		synchronized(this) {
			try {
				wait();
			} catch (InterruptedException e) {
				return null;
			}
		}
		if (!proceed)
			return null;
		gpxOS = new ByteArrayOutputStream();
		return gpxOS;
	}

}
