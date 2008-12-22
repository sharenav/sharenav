/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 */
package de.ueller.midlet.gps.data;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.UploadListener;

public class EditableWay extends Way implements Runnable{
	private final static Logger logger = Logger.getInstance(EditableWay.class,Logger.DEBUG);
	private UploadListener ul;
	public int osmID;
	private String fullXML;
	
	public EditableWay(DataInputStream is, byte f, Tile t, byte[] layers, int idx) throws IOException {
		super(is, f, t, layers, idx);
		osmID = is.readInt();
	}
	
	public void loadXML(UploadListener ul) {
		this.ul = ul;
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		logger.info("Retrieving XML for " + this);
		try {
			fullXML = null;
			HttpConnection connection = (HttpConnection) Connector
					.open("http://www.openstreetmap.org/api/0.5/way/" + osmID);
			connection.setRequestMethod(HttpConnection.GET);
			connection.setRequestProperty("Content-Type", "//text plain");
			connection.setRequestProperty("Connection", "close");
			// HTTP Response
			if (connection.getResponseCode() == HttpConnection.HTTP_OK) {
				String str;
				InputStream inputstream = connection.openInputStream();
				int length = (int) connection.getLength();
				if (length != -1) {
					byte incomingData[] = new byte[length];
					inputstream.read(incomingData);
					str = new String(incomingData);
				} else {
					ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
					int ch;
					while ((ch = inputstream.read()) != -1) {
						bytestream.write(ch);
					}
					str = new String(bytestream.toByteArray());
					bytestream.close();
				}
				logger.debug(str);
				fullXML = str;
			} else {
				logger.info("Request failed (" + connection.getResponseCode() + "): " + connection.getResponseMessage());
			}
		} catch (IOException ioe) {

		} catch (SecurityException se) {

		}
		ul.completedUpload(fullXML != null, "Retrieved XML for way " + osmID);
	}
	
	public String getXML() {
		return fullXML;
	}

}
