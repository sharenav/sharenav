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

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HTTPhelper;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.GuiOSMChangeset;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.OSMdataNode;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.POIdescription;

public class GuiOSMPOIDisplay extends GuiOSMEntityDisplay {
	
	private final static Logger logger = Logger.getInstance(GuiOSMPOIDisplay.class,Logger.DEBUG);
	private final static int LOAD_POI_STATE_NONE = 0;
	private final static int LOAD_POI_STATE_LOAD = 1;
	private final static int LOAD_POI_STATE_UPLOAD = 2;
	
	private int nodeID;
	private HTTPhelper http;
	
	private int loadPOIxmlState;
	
	private Form poiTypeForm;
	private ChoiceGroup poiSelectionCG;
	private boolean showPoiTypeForm;
	
	public GuiOSMPOIDisplay(int nodeID, SingleTile t, float lat, float lon, GpsMidDisplayable parent) {
		super("Node", parent);
		this.nodeID = nodeID;
		loadPOIxmlState = LOAD_POI_STATE_NONE;
		if (nodeID < 0) {
			osmentity = new OSMdataNode(nodeID, lat, lon);
			showPoiTypeForm = true;
			setupPoiTypeForm();
		} else {
			showPoiTypeForm = false;
			setupScreen();
		}
		
	}
	
	private void setupPoiTypeForm() {
		poiTypeForm = new Form("POI type");
		poiSelectionCG = new ChoiceGroup("Select type to add: ", ChoiceGroup.EXCLUSIVE);
		for (byte i = 0; i < C.getMaxType(); i++) {				
			poiSelectionCG.append(C.getNodeTypeDesc(i), C.getNodeSearchImage(i));
		}
		poiTypeForm.append(poiSelectionCG);
		poiTypeForm.addCommand(BACK_CMD);
		poiTypeForm.addCommand(OK_CMD);
		poiTypeForm.setCommandListener(this);
	}
	
	public void refresh() {
		if (nodeID < 0) {
			
		} else {
			logger.debug("Retrieving XML for Node " + nodeID);
			String url = Configuration.getOsmUrl() + "node/" + nodeID;
			if (http == null) { 
				http = new HTTPhelper();
			}
			loadPOIxmlState = LOAD_POI_STATE_LOAD;
			http.getURL(url, this);
		}
		
	}
	
	public void uploadXML() {
		//#debug debug
		logger.debug("Uploading XML for " + this);
		String fullXML = osmentity.toXML(changesetGui.getChangesetID());
		String url;
		if (nodeID < 0) {
			url = Configuration.getOsmUrl() + "node/create";
		} else {
			url = Configuration.getOsmUrl() + "node/" + nodeID;
		}
		if (http == null) { 
			http = new HTTPhelper();
		}
		loadPOIxmlState = LOAD_POI_STATE_UPLOAD;
		http.uploadData(url, fullXML, true, this, Configuration.getOsmUsername(), Configuration.getOsmPwd());
	}
	
	public void commandAction(Command c, Displayable d) {
		super.commandAction(c, d);

		if (c == UPLOAD_CMD) {
			parent.show();
			if (changesetGui == null) {
				loadState = LOAD_STATE_CHANGESET;
				changesetGui = new GuiOSMChangeset(parent,this);
				changesetGui.show();
			} else {
				loadState = LOAD_STATE_UPLOAD;
				uploadXML();
			}
		}
		
		if (c == OK_CMD) {
			byte poiType = (byte)poiSelectionCG.getSelectedIndex();
			String [] tags = C.getNodeOsmTags(poiType);
			for (int i = 0; i < tags.length/2; i++) {
				osmentity.getTags().put(tags[i*2], tags[i*2 + 1]);
			}
			showPoiTypeForm = false;
			setupScreen();
			show();
		}
		
		//Normal BACK_CMD is handled in the super class
		if ((c == BACK_CMD) && (showPoiTypeForm)) {
			parent.show();
		}
	}

	public void show() {
		if (showPoiTypeForm) {
			GpsMid.getInstance().show(poiTypeForm);
		} else {
			GpsMid.getInstance().show(this);
		}
	}
	
	public void completedUpload(boolean success, String message) {
		if (success) {
			switch (loadState) {
			case LOAD_STATE_LOAD: {
				osmentity = new OSMdataNode(http.getData(), nodeID);
				setupScreen();
				loadState = LOAD_STATE_NONE;
				break;
			}
			case LOAD_STATE_UPLOAD: {
				GpsMid.getInstance().alert("Adding POI", "Poi was successfully added to OpenStreetMap", 1000);
				loadState = LOAD_POI_STATE_NONE;
				break;
			}
			case LOAD_STATE_CHANGESET: {
				loadState = LOAD_STATE_UPLOAD;
				uploadXML();
				break;
			}
			}
		} else {
			logger.error("Server operation failed: " + message);
		}
	}

}
//#endif