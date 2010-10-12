/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (c) 2009  Kai Krueger
 */
//#if polish.api.osm-editing
package de.ueller.midlet.gps;

import java.util.Vector;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HTTPhelper;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.GuiOSMChangeset;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.GuiPOItypeSelectMenu.POItypeSelectMenuItem;
import de.ueller.midlet.gps.data.KeySelectMenuItem;
import de.ueller.midlet.gps.data.OSMdataEntity;
import de.ueller.midlet.gps.data.OSMdataNode;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.names.NumberCanon;
import de.ueller.midlet.gps.tile.POIdescription;

import de.enough.polish.util.Locale;

public class GuiOSMPOIDisplay extends GuiOSMEntityDisplay implements KeySelectMenuReducedListener{
	
	private final static Logger logger = Logger.getInstance(GuiOSMPOIDisplay.class,Logger.DEBUG);
	private final static int LOAD_POI_STATE_NONE = 0;
	private final static int LOAD_POI_STATE_LOAD = 1;
	private final static int LOAD_POI_STATE_UPLOAD = 2;
	
	private int nodeID;
	private HTTPhelper http;
	
	private int loadPOIxmlState;
	
	private GuiPOItypeSelectMenu poiTypeForm;
	private ChoiceGroup poiSelectionCG;
	private boolean showPoiTypeForm;
	private boolean showParent;
	private byte poiType;
	
	public GuiOSMPOIDisplay(int nodeID, SingleTile t, float lat, float lon, GpsMidDisplayable parent) {
		super(Locale.get("guiosmpoidisplay.Node")/*Node*/, parent);
		this.nodeID = nodeID;
		showParent = false;
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
		try { 
			poiTypeForm = new GuiPOItypeSelectMenu(this, this);
			poiTypeForm.show();
		} catch (Exception e) {
			logger.exception(Locale.get("guiosmpoidisplay.POItypeFormInvalid")/*POI type form invalid*/, e);
		}
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
			if ((changesetGui == null) || (changesetGui.getChangesetID() < 0)) {
				loadState = LOAD_STATE_CHANGESET;
				changesetGui = new GuiOSMChangeset(parent,this);
				changesetGui.show();
			} else {
				loadState = LOAD_STATE_UPLOAD;
				uploadXML();
			}
		}
		
		if (c == OK_CMD) {
			poiType = (byte)poiSelectionCG.getSelectedIndex();
			String [] tags = Legend.getNodeOsmTags(poiType);
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
		if (showParent) {
			parent.show();
			showParent = false;
			return;
		}
		if (showPoiTypeForm) {
			poiTypeForm.show();
			showParent = true;
		} else {
			setupScreen();
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
				GpsMid.getInstance().alert(Locale.get("guiosmpoidisplay.AddingPOI")/*Adding POI*/, Locale.get("guiosmpoidisplay.PoiSuccessfullyAdded")/*Poi was successfully added to OpenStreetMap*/, 1000);
				
				if (osmentity instanceof OSMdataEntity) {
					logger.info("Adding Waypoint to mark where POI was uploaded to OSM");
					OSMdataNode poi = (OSMdataNode)osmentity;
					PositionMark waypt = new PositionMark(poi.getLat(), poi.getLon());
					waypt.displayName = Locale.get("guiosmpoidisplay.POI")/*POI: */ + Legend.getNodeTypeDesc(poiType);
					Trace.getInstance().gpx.addWayPt(waypt);
				}

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
			logger.error(Locale.get("guiosmpoidisplay.ServerOperationFailed")/*Server operation failed: */ + message);
		}
	}

	public void keySelectMenuCancel() {
		showPoiTypeForm = false;
	}

	public void keySelectMenuItemSelected(KeySelectMenuItem item) {
		POItypeSelectMenuItem poiTypeIt = (POItypeSelectMenuItem)item;
		poiType = poiTypeIt.getIdx();
		String [] tags = Legend.getNodeOsmTags(poiType);
		System.out.println("poiType: " + poiTypeIt + "  tags " + tags + " ed: " + osmentity);
		if ((tags != null) && (osmentity != null)) {
			for (int i = 0; i < tags.length/2; i++) {
				osmentity.getTags().put(tags[i*2], tags[i*2 + 1]);
			}
		}
		showPoiTypeForm = false;
		showParent = false;
		
	}


}
//#endif