/*
 * GpsMid - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
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
 * 
 * See COPYING
 */
package de.ueller.util;
//#if polish.api.online
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import de.enough.polish.util.base64.Base64;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.midlet.ui.UploadListener;

import de.enough.polish.util.Locale;

public class HttpHelper implements Runnable{
	private final static Logger logger = Logger.getInstance(HttpHelper.class, Logger.DEBUG);
	private boolean upload;
	private UploadListener ul;
	private volatile boolean  busy;
	private String url;
	private String username;
	private String password;
	private String data;
	private byte[] binaryData;
	private boolean methodPut;
	private boolean methodDelete;
	
	public void getURL(String url, UploadListener ul) {
		getURL(url, ul, false);
	}

	public void getURL(String url, UploadListener ul, boolean binary) {
		if ((ul == null) || (url == null)) {
			logger.error(Locale.get("httphelper.BrokenCodeRetrUrl")/*Broken code retrieving url */ + url);
			return;
		}
		if (busy) {
			ul.completedUpload(false, "HTTPhelper was still busy");
			return;
		} else {
			busy = true;
			this.url = url;
			this.ul = ul;
			download(binary);
		}
	}
	
	// FIXME consider removing this and changing calls to uploadData
	public void uploadData(String url, String data, boolean putMethod, UploadListener ul, String username, String password) {
		uploadData(url, data, putMethod, false, ul, username, password);
	}

	public void deleteData(String url, String data, UploadListener ul, String username, String password) {
		uploadData(url, data, false, true, ul, username, password);
	}

	public void uploadData(String url, String data, boolean putMethod, boolean deleteMethod, UploadListener ul, String username, String password) {
		if ((ul == null) || (url == null)) {
			logger.error(Locale.get("httphelper.BrokenCodePostingToUrl")/*Broken code posting to url */ + url);
			return;
		}
		if (busy) {
			ul.completedUpload(false, "HTTPhelper was still busy");
			return;
		} else {
			busy = true;
			this.url = url;
			this.data = data;
			this.methodPut = putMethod;
			this.methodDelete = deleteMethod;
			this.ul = ul;
			this.username = username;
			this.password = password;
			upload();
		}
	}
	
	
	public void run() {
		if (upload) {
			upload();
		} else {
			download(false);
		}
	}
	
	private void upload(){
		int respCode = 0;
		String respMessage = null;
		boolean success = false;
		try {
			//#debug info
			logger.info("HTTP POST: " + url);
			//#debug debug
			logger.debug("data:\n" + data);
			HttpConnection connection = (HttpConnection) Connector
			.open(url);
			connection.setRequestMethod(HttpConnection.POST);
			connection.setRequestProperty("Connection", "close");
			connection.setRequestProperty("User-Agent", "GpsMid");
			if (methodPut) {
				connection.setRequestProperty("X_HTTP_METHOD_OVERRIDE", "PUT");
				logger.info("HTTP METHOD OVERRIDE: PUT: " + url);
			} else if (methodDelete) {
				connection.setRequestProperty("X_HTTP_METHOD_OVERRIDE", "DELETE");
				logger.info("HTTP METHOD OVERRIDE: DELETE: " + url);
			}
			
			if ((username != null) && (password != null)) {
				connection.setRequestProperty("Authorization", "Basic " + Base64.encode(username +":" + password));
			}
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(baos,Configuration.getUtf8Encoding());
			
			
			osw.write(data);
			osw.flush();
			connection.setRequestProperty("Content-Length", Integer.toString(baos.toByteArray().length));
			OutputStream os = connection.openOutputStream();
			os.write(baos.toByteArray());
			os.flush();
			
			
			// HTTP Response
			respCode = connection.getResponseCode();
			respMessage = connection.getResponseMessage();
			
			if (respCode == HttpConnection.HTTP_OK) {
				//#debug info
				logger.info("Successfully uploaded data, reading response");
				
				InputStream inputstream = connection.openInputStream();
				
				int length = (int) connection.getLength();
				//#debug debug
				logger.debug("Retrieving String of length: " + length);
				//#if polish.android
				// For some reason, getLength() is too small on Android
				if (length != -1 && false) {
				//#else
				if (length != -1) {
				//#endif
					byte incomingData[] = new byte[length];
					int idx = 0;
					while (idx < length) {
						int readB = inputstream.read(incomingData,idx, length - idx);
						//#debug debug
						logger.debug("Read: " + readB  + " bytes");
						idx += readB;
					}
					data = new String(incomingData);
				} else {
					ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
					int ch;
					while ((ch = inputstream.read()) != -1) {
						bytestream.write(ch);
					}
					bytestream.flush();
					data = new String(bytestream.toByteArray());
					bytestream.close();
				}
				//#debug info
				logger.info("HTTP post response: " + data);
				
				success = true;
				
			} else {
				success = false;
				logger.error(Locale.get("httphelper.DataWasNotUploaded")/*Data was not uploaded*/ + " (" + respCode + "): " + respMessage);
			}
			
		} catch (Exception e) {
			logger.exception(Locale.get("httphelper.FailedToUploadData")/*Failed to upload data to */ + url + ": ", e);
			success = false;
		}
		busy = false;
		if (ul != null) {
			ul.completedUpload(success, "Uploaded data to "+  url);
		} else {
			logger.info("UL shouldn't have been null");
		};
		
	}
	
	private void download(boolean binary) {
		//#debug debug
		logger.info("Retrieving " + url);
		try {
			HttpConnection connection = (HttpConnection) Connector
					.open(url);
			connection.setRequestMethod(HttpConnection.GET);
			connection.setRequestProperty("User-Agent", "GpsMid");
			connection.setRequestProperty("Content-Type", "//text plain");
			connection.setRequestProperty("Connection", "close");
			// HTTP Response
			if (connection.getResponseCode() == HttpConnection.HTTP_OK) {
				InputStream inputstream = connection.openInputStream();
				int length = (int) connection.getLength();
				//#debug debug
				logger.debug("Retrieving String of length: " + length);
				//#if polish.android
				// For some reason, getLength() is too small on Android
				if (length != -1 && false) {
				//#else
				if (length != -1) {
				//#endif
					byte incomingData[] = new byte[length];
					int idx = 0;
					while (idx < length) {
						int readB = inputstream.read(incomingData,idx, length - idx);
						//#debug debug
						logger.debug("Read: " + readB  + " bytes");
						idx += readB;
					}
					if (binary) {
						binaryData = incomingData;
					} else {
						data = new String(incomingData,Configuration.getUtf8Encoding());
					}
				} else {
					ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
					int ch;
					while ((ch = inputstream.read()) != -1) {
						bytestream.write(ch);
					}
					bytestream.flush();
					if (binary) {
						binaryData = bytestream.toByteArray();
					} else {
						data = new String(bytestream.toByteArray(),Configuration.getUtf8Encoding());
					}
					bytestream.close();
				}
				//#debug info
				logger.info("HTTP: " + data);
			} else {
				logger.error(Locale.get("httphelper.RequestFailed")/*Request failed*/ + " (" + connection.getResponseCode() + "): " + connection.getResponseMessage());
			}
		} catch (IOException ioe) {
			logger.error(Locale.get("httphelper.FailedToRetrieveURL")/*Failed to retrieve URL*/ +  ": " + ioe.getMessage(), true);
		} catch (SecurityException se) {
			logger.error(Locale.get("httphelper.FailedToRetrieveURLDenied")/*Failed to retrieve URL. J2me disallowed it*/, true);
		}
		if (ul != null) {
			ul.completedUpload(data != null, Locale.get("httphelper.RetrievedURL")/*Retrieved URL */ + url);
		}
		busy = false;
	}
	
	public String getData() {
		return data;
	}

	public byte[] getBinaryData() {
		return binaryData;
	}
	
	/**
	 * Escape the necessary characters to form a valid XML string
	 * These are ' & < > "
	 * 
	 * FIXME: This seems to be a duplicate of the function HelperRoutines.utf2xml()
	 * 
	 * @param xml
	 * @return
	 */
	public static String escapeXML(String xml) {
		
		int idx = xml.indexOf('&');
		while (idx >= 0) {
			xml = xml.substring(0,idx ) + "&amp;" + xml.substring(idx + 1);
			idx = xml.indexOf('&', idx + 1);
		}
		
		idx = xml.indexOf('\'');
		while (idx >= 0) {
			xml = xml.substring(0,idx) + "&apos;" + xml.substring(idx + 1);
			idx = xml.indexOf('\'');
		}
		
		idx = xml.indexOf('"');
		while (idx >= 0) {
			xml = xml.substring(0,idx) + "&quot;" + xml.substring(idx + 1);
			idx = xml.indexOf('"');
		}
		
		idx = xml.indexOf('<');
		while (idx >= 0) {
			xml = xml.substring(0,idx) + "&lt;" + xml.substring(idx + 1);
			idx = xml.indexOf('<');
		}
		
		idx = xml.indexOf('>');
		while (idx >= 0) {
			xml = xml.substring(0,idx) + "&gt;" + xml.substring(idx + 1);
			idx = xml.indexOf('>');
		}
		
		return xml;
	}


}
//#endif