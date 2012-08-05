/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 */
//#if polish.api.osm-editing
package de.ueller.gpsmid.data;

import java.io.DataInputStream;
import java.io.IOException;
//#if polish.api.online
import de.ueller.util.HttpHelper;
import de.ueller.util.Logger;
//#endif
import de.ueller.gpsmid.mapdata.Way;
import de.ueller.gpsmid.tile.Tile;
import de.ueller.midlet.ui.UploadListener;

import de.enough.polish.util.Locale;

public class EditableWay extends Way implements UploadListener {
	private final static Logger logger = Logger.getInstance(EditableWay.class,Logger.DEBUG);
	private UploadListener ul;
	public int osmID;
	private OsmDataWay OSMdata;
	//#if polish.api.online
	private HttpHelper http = null;
	//#endif
	
	public EditableWay(DataInputStream is, byte f, Tile t, byte[] layers, int idx, boolean outlineFlag) throws IOException {
		super(is, f, t, layers, idx, outlineFlag);
		osmID = is.readInt();
		OSMdata = null;
	}
	
	public void loadXML(UploadListener ul) {
		//#if polish.api.online
		//#debug debug
		logger.debug("Retrieving XML for " + this);
		this.ul = ul;
		String url = Configuration.getOsmUrl() + "way/" + osmID;
		if (http == null) { 
			http = new HttpHelper();
		}
		http.getURL(url, this);
		//#endif
	}
	
	public void uploadXML(int commitChangesetID, UploadListener ul) {
		this.ul = ul;
		//#debug debug
		logger.debug("Uploading XML for " + this);
		String fullXML = OSMdata.toXML(commitChangesetID);
		String url = Configuration.getOsmUrl() + "way/" + osmID;
		if (http == null) { 
			http = new HttpHelper();
		}
		http.uploadData(url, fullXML, true, ul, Configuration.getOsmUsername(), Configuration.getOsmPwd());
	}
	

	public OsmDataWay getOSMdata() {
		return OSMdata;
	}
	
	public String getXML() {
		if (OSMdata != null) {
			OSMdata.getXML();
		}
		return null;
	}

	public void completedUpload(boolean success, String message) {
		//#if polish.api.online
		if (success) {
			
			String fullXML = http.getData();
			OSMdata = new OsmDataWay(fullXML, osmID);
			//#debug
			logger.debug(OSMdata.toString());
			if (ul != null) {
				ul.completedUpload(true, Locale.get("editableway.RetrievedXMLForWay")/*Retrieved XML for way */ + osmID);
			}
		} else {
			if (ul != null) {
				ul.completedUpload(false, Locale.get("editableway.RetrievedXMLForWay")/*Retrieved XML for way */ + osmID);
			}
		}
		//#endif
	} 
	
	public void setProgress(String message) {
	}

	public void startProgress(String title) {
	}

	public void updateProgress(String message) {
	}

	public void updateProgressValue(int increment) {
	}

	public void uploadAborted() {
	}

}
//#endif