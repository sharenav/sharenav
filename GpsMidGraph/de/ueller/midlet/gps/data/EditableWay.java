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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.enough.polish.util.base64.Base64;
import de.ueller.gps.data.Configuration;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.UploadListener;

public class EditableWay extends Way implements Runnable{
	private final static Logger logger = Logger.getInstance(EditableWay.class,Logger.DEBUG);
	private UploadListener ul;
	private boolean upload;
	public int osmID;
	private OSMdataWay OSMdata;
	
	public EditableWay(DataInputStream is, byte f, Tile t, byte[] layers, int idx) throws IOException {
		super(is, f, t, layers, idx);
		osmID = is.readInt();
		OSMdata = null;
		upload = false;
	}
	
	public void loadXML(UploadListener ul) {
		upload = false;
		this.ul = ul;
		Thread t = new Thread(this);
		t.start();
	}
	
	public void uploadXML(UploadListener ul) {
		upload = true;
		this.ul = ul;
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		if (upload) {
			upload();
		} else {
			download();
		}
	}
	
	private void upload() {
		logger.debug("Uploading XML for " + this);
		int respCode = 0;
		String respMessage = null;
		boolean success = false;
		try {
			String fullXML = OSMdata.toXML();
			String url = Configuration.getOsmUrl() + "way/" + osmID + "?_method=put";
			logger.info("HTTP POST: " + url);
			HttpConnection connection = (HttpConnection) Connector
			.open(url);
			connection.setRequestMethod(HttpConnection.POST);
			connection.setRequestProperty("Connection", "close");
			connection.setRequestProperty("User-Agent", "GpsMid");
			
			
			connection.setRequestProperty("Authorization", "Basic " + Base64.encode(Configuration.getOsmUsername() +":" + Configuration.getOsmPwd()));
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(baos);
			
			
			osw.write(fullXML);
			osw.flush();
			connection.setRequestProperty("Content-Length", Integer.toString(baos.toByteArray().length));
			OutputStream os = connection.openOutputStream();
			os.write(baos.toByteArray());
			os.flush();
			
			
			// HTTP Response
			respCode = connection.getResponseCode();
			respMessage = connection.getResponseMessage();
		} catch (Exception e) {
			logger.exception("Failed to upload way", e);
		}
		if (respCode == HttpConnection.HTTP_OK) {
			logger.info("Successfully uploaded way");
			success = true;
			
		} else {
			success = false;
			logger.error("Way was not uploaded (" + respCode + "): " + respMessage);
		}
		ul.completedUpload(success, "Uploaded XML for way " + osmID);
	}
	
	private void download() {
		logger.debug("Retrieving XML for " + this);
		try {
			String fullXML = null;
			String url = Configuration.getOsmUrl() + "way/" + osmID;
			logger.info("HTTP get " + url);
			HttpConnection connection = (HttpConnection) Connector
					.open(url);
			connection.setRequestMethod(HttpConnection.GET);
			connection.setRequestProperty("Content-Type", "//text plain");
			connection.setRequestProperty("Connection", "close");
			// HTTP Response
			if (connection.getResponseCode() == HttpConnection.HTTP_OK) {
				String str;
				InputStream inputstream = connection.openInputStream();
				int length = (int) connection.getLength();
				//#debug debug
				logger.debug("Retrieving String of length: " + length);
				if (length != -1) {
					byte incomingData[] = new byte[length];
					int idx = 0;
					while (idx < length) {
						int readB = inputstream.read(incomingData,idx, length - idx);
						//#debug debug
						logger.debug("Read: " + readB  + " bytes");
						idx += readB;
					}
					str = new String(incomingData);
				} else {
					ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
					int ch;
					while ((ch = inputstream.read()) != -1) {
						bytestream.write(ch);
					}
					bytestream.flush();
					str = new String(bytestream.toByteArray());
					bytestream.close();
				}
				//#debug info
				logger.info(str);
				fullXML = str;
				if (str != null)
					OSMdata = new OSMdataWay(fullXML, osmID);
					//#debug
					logger.debug(OSMdata.toString());
			} else {
				logger.error("Request failed (" + connection.getResponseCode() + "): " + connection.getResponseMessage());
			}
		} catch (IOException ioe) {
			logger.error("Failed to retrieve Way: " + ioe.getMessage(), true);
		} catch (SecurityException se) {
			logger.error("Failed to retrieve Way. J2me dissallowed it", true);
		}
		ul.completedUpload(OSMdata != null, "Retrieved XML for way " + osmID);
	}
	
	public OSMdataWay getOSMdata() {
		return OSMdata;
	}
	
	public String getXML() {
		if (OSMdata != null) {
			OSMdata.getXML();
		}
		return null;
	}

}
