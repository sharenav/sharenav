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
package de.ueller.gpsmid.ui;

import java.util.Vector;

import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.OsmDataEntity;
import de.ueller.gpsmid.data.OsmDataNode;
import de.ueller.gpsmid.data.PositionMark;
import de.ueller.gpsmid.mapdata.PoiDescription;
import de.ueller.gpsmid.names.NumberCanon;
import de.ueller.gpsmid.tile.SingleTile;
import de.ueller.gpsmid.ui.GuiOsmChangeset;
import de.ueller.gpsmid.ui.GuiPoiTypeSelectMenu.PoiTypeSelectMenuItem;
import de.ueller.midlet.ui.KeySelectMenuItem;
import de.ueller.util.HttpHelper;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;

import de.enough.polish.util.Locale;

public class GuiOsmPoiDisplay extends GuiOsmEntityDisplay implements KeySelectMenuReducedListener{
	
	private final static Logger logger = Logger.getInstance(GuiOsmPoiDisplay.class,Logger.DEBUG);
	
	private int nodeID;
	private HttpHelper http;
	
	//private int loadPOIxmlState;
	
	private GuiPoiTypeSelectMenu poiTypeForm;
	private ChoiceGroup poiSelectionCG;
	private boolean showPoiTypeForm;
	private boolean showParent;
	private short poiType;
	
	public GuiOsmPoiDisplay(int nodeID, SingleTile t, float lat, float lon, GpsMidDisplayable parent) {
		super(Locale.get("guiosmpoidisplay.Node")/*Node*/, parent);
		this.nodeID = nodeID;
		showParent = false;
		loadState = LOAD_STATE_NONE;
		if (nodeID < 0) {
			osmentity = new OsmDataNode(nodeID, lat*MoreMath.FAC_RADTODEC, lon*MoreMath.FAC_RADTODEC);
			showPoiTypeForm = true;
			setupPoiTypeForm();
		} else {
			showPoiTypeForm = false;
			setupScreen();
		}
		
	}
	
	private void setupPoiTypeForm() {
		try { 
			poiTypeForm = new GuiPoiTypeSelectMenu(this, this);
			poiTypeForm.show();
		} catch (Exception e) {
			logger.exception(Locale.get("guiosmpoidisplay.POItypeFormInvalid")/*POI type form invalid*/, e);
		}
	}
	
	public void refresh() {
		if (nodeID < 0) {
			
		} else {
			//System.out.println("Retrieving XML for Node " + nodeID);
			logger.debug("Retrieving XML for Node " + nodeID);
			String url = Configuration.getOsmUrl() + "node/" + nodeID;
			if (http == null) { 
				http = new HttpHelper();
			}
			loadState = LOAD_STATE_LOAD;
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
			http = new HttpHelper();
		}
		loadState = LOAD_STATE_UPLOAD;
		http.uploadData(url, fullXML, true, this, Configuration.getOsmUsername(), Configuration.getOsmPwd());
	}
	
	public void uploadDeleteXML() {
		//#debug debug
		logger.debug("Uploading XML for deleting " + this);
		String fullXML = osmentity.toDeleteXML(changesetGui.getChangesetID());
		String url;
		if (nodeID < 0) {
			url = Configuration.getOsmUrl() + "node/create";
		} else {
			url = Configuration.getOsmUrl() + "node/" + nodeID;
		}
		if (http == null) { 
			http = new HttpHelper();
		}
		loadState = LOAD_STATE_DELETE;
		http.deleteData(url, fullXML, this, Configuration.getOsmUsername(), Configuration.getOsmPwd());
	}
	
	public void commandAction(Command c, Displayable d) {
		super.commandAction(c, d);

		if (c == UPLOAD_CMD) {
			parent.show();
			if ((changesetGui == null) || (changesetGui.getChangesetID() < 0)) {
				loadState = LOAD_STATE_CHANGESET;
				changesetGui = new GuiOsmChangeset(parent,this);
				changesetGui.show();
			} else {
				loadState = LOAD_STATE_UPLOAD;
				uploadXML();
			}
		}
		
		if (c == OK_CMD) {
			poiType = (short)poiSelectionCG.getSelectedIndex();
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
		if (c == REMOVE_ENTITY_CMD) {
			parent.show();
			if ((changesetGui == null) || (changesetGui.getChangesetID() < 0)) {
				loadState = LOAD_STATE_DELETE_CHANGESET;
				changesetGui = new GuiOsmChangeset(parent,this);
				changesetGui.show();
			} else {
				loadState = LOAD_STATE_DELETE;
				uploadDeleteXML();
			}
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
				osmentity = new OsmDataNode(http.getData(), nodeID);
				setupScreen();
				loadState = LOAD_STATE_NONE;
				break;
			}
			case LOAD_STATE_DELETE:
			case LOAD_STATE_UPLOAD: {
				if (loadState == LOAD_STATE_UPLOAD) {
					GpsMid.getInstance().alert(Locale.get("guiosmpoidisplay.SavingPOI")/*Saving POI*/, Locale.get("guiosmpoidisplay.PoiSuccessfullySaved")/*Poi was successfully saved to OpenStreetMap*/, 1000);
					;
					if (osmentity instanceof OsmDataEntity) {
						logger.info("Adding Waypoint to mark where POI was uploaded to OSM");
						OsmDataNode poi = (OsmDataNode)osmentity;
						PositionMark waypt = new PositionMark(poi.getLat()*MoreMath.FAC_DECTORAD, poi.getLon()*MoreMath.FAC_DECTORAD);
						waypt.displayName = Locale.get("guiosmpoidisplay.POI")/*POI: */ + Legend.getNodeTypeDesc(poiType);
						Trace.getInstance().gpx.addWayPt(waypt);
					}
				}  else {
					GpsMid.getInstance().alert(Locale.get("guiosmpoidisplay.DeletingPOI")/*Deleting POI*/, Locale.get("guiosmpoidisplay.PoiSuccessfullyDeleted")/*Poi was successfully deleted from OpenStreetMap*/, 1000);
				}
				loadState = LOAD_STATE_NONE;
				break;
			}
			case LOAD_STATE_DELETE_CHANGESET:
			case LOAD_STATE_CHANGESET: {
				if (loadState == LOAD_STATE_CHANGESET) {
					loadState = LOAD_STATE_UPLOAD;
					uploadXML();
				} else {
					loadState = LOAD_STATE_DELETE;
					uploadDeleteXML();
				}
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
		PoiTypeSelectMenuItem poiTypeIt = (PoiTypeSelectMenuItem)item;
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