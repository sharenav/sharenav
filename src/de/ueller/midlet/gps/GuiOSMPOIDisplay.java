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
	
	private int nodeID;
	private HTTPhelper http;
	
	private boolean loadPOIxml;
	
	private Form poiTypeForm;
	private ChoiceGroup poiSelectionCG;
	private boolean showPoiTypeForm;
	
	public GuiOSMPOIDisplay(int nodeID, SingleTile t, float lat, float lon, GpsMidDisplayable parent) {
		super("Node", parent);
		this.nodeID = nodeID;
		loadPOIxml = false;
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
			loadPOIxml = true;
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
		http.uploadData(url, fullXML, true, this, Configuration.getOsmUsername(), Configuration.getOsmPwd());
	}
	
	public void commandAction(Command c, Displayable d) {
		super.commandAction(c, d);

		if (c == UPLOAD_CMD) {
			parent.show();
			if (changesetGui == null) {
				createChangeset = true;
				changesetGui = new GuiOSMChangeset(parent,this);
				changesetGui.show();
			} else {
				createChangeset = false;
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
			if (createChangeset) {
				createChangeset = false;
				uploadXML();
			} else if (loadPOIxml) {
				osmentity = new OSMdataNode(http.getData(), nodeID);
				setupScreen();
				loadPOIxml = false;
			} else {
				if (GpsMid.getInstance().shouldBeShown() == this) {
					
					//osmway = eway.getOSMdata();
					//setupScreen();
				}
			}
		} else {
			
		}
	}

}
//#endif