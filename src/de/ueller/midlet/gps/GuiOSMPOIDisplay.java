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
import de.ueller.midlet.gps.data.KeySelectMenuItem;
import de.ueller.midlet.gps.data.OSMdataNode;
import de.ueller.midlet.gps.names.NumberCanon;
import de.ueller.midlet.gps.tile.POIdescription;

public class GuiOSMPOIDisplay extends GuiOSMEntityDisplay implements KeySelectMenuListener{
	
	class POITSelectMenuItem implements KeySelectMenuItem {
		private Image img;
		private String  name;
		private byte idx;
		private String canon;
		
		public POITSelectMenuItem(Image img, String name, byte idx) {
			this.img = img;
			this.name = name;
			this.idx = idx;
			this.canon = NumberCanon.canonial(name);
		}

		public Image getImage() {
			return img;
		}

		public String getName() {
			return name;
		}
		
		public String getCanon() {
			return canon;
		}
		
		public byte getIdx() {
			return idx;
		}
		
		public String toString() {
			return name + " [" + canon + "] (" + idx +")";
		}
		
	}
	
	private final static Logger logger = Logger.getInstance(GuiOSMPOIDisplay.class,Logger.DEBUG);
	private final static int LOAD_POI_STATE_NONE = 0;
	private final static int LOAD_POI_STATE_LOAD = 1;
	private final static int LOAD_POI_STATE_UPLOAD = 2;
	
	private int nodeID;
	private HTTPhelper http;
	
	private int loadPOIxmlState;
	
	private KeySelectMenu poiTypeForm;
	private ChoiceGroup poiSelectionCG;
	private boolean showPoiTypeForm;
	private boolean showParent;
	private Vector  poiTypes;
	
	public GuiOSMPOIDisplay(int nodeID, SingleTile t, float lat, float lon, GpsMidDisplayable parent) {
		super("Node", parent);
		this.nodeID = nodeID;
		poiTypes = null;
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
			poiTypeForm = new KeySelectMenu(this, this);
			resetMenu();
			poiTypeForm.show();
		} catch (Exception e) {
			logger.exception("POI type form invalid", e);
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
			byte poiType = (byte)poiSelectionCG.getSelectedIndex();
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

	public void cancel() {
		showPoiTypeForm = false;
	}

	public void itemSelected(KeySelectMenuItem item) {
		POITSelectMenuItem poiType = (POITSelectMenuItem)item;
		String [] tags = Legend.getNodeOsmTags(poiType.getIdx());
		System.out.println("poiType: " + poiType + "  tags " + tags + " ed: " + osmentity);
		if ((tags != null) && (osmentity != null)) {
			for (int i = 0; i < tags.length/2; i++) {
				osmentity.getTags().put(tags[i*2], tags[i*2 + 1]);
			}
		}
		showPoiTypeForm = false;
		showParent = false;
		
	}

	public void resetMenu() {
		logger.info("Resetting menu");
		poiTypeForm.removeAll();
		if (poiTypes == null) {
			poiTypes = new Vector();
			for (byte i = 1; i < Legend.getMaxType(); i++) {
				KeySelectMenuItem menuItem = new POITSelectMenuItem(Legend.getNodeSearchImage(i),Legend.getNodeTypeDesc(i),i);
				poiTypes.addElement(menuItem);
			}
		}
		poiTypeForm.addResult(poiTypes);
	}

	public void searchString(String searchString) {
		poiTypeForm.removeAll();
		Vector vec = new Vector();
		for (byte i = 0; i < poiTypes.size(); i++) {
			POITSelectMenuItem poiType = (POITSelectMenuItem)poiTypes.elementAt(i); 
			if (poiType.getCanon().startsWith(searchString)) {
				logger.info(poiType + " matches searchString " + searchString);
				vec.addElement(poiType);
			}
		}
		poiTypeForm.addResult(vec);
	}

}
//#endif