/*
 * ShareNav - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
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
package net.sharenav.sharenav.ui;

//#if polish.api.osm-editing
import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.OsmDataNode;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.HttpHelper;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class GuiOsmAddrDisplay extends Form implements ShareNavDisplayable,
		CommandListener, UploadListener {
	private final static Logger logger = Logger.getInstance(
			GuiOsmAddrDisplay.class, Logger.DEBUG);

	private final Command UPLOAD_CMD = new Command(Locale.get("guiosmaddrdisplay.UploadToOSM")/*Upload to OSM*/, Command.OK,
			6);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 1);

	private ShareNavDisplayable parent;
	private int loadState;

	private TextField tfHousenumber;
	private TextField tfHousename;
	private TextField tfStreet;
	private TextField tfPostcode;

	private String sHousenumber;
	private String sHousename;
	private String sStreet;
	private String sPostcode;
	private int nodeID;

	private float lat;
	private float lon;

	private OsmDataNode osmNode;

	private HttpHelper http;

	public GuiOsmAddrDisplay(int nodeID, String streetName, SingleTile t,
			float lat, float lon, ShareNavDisplayable parent) {
		super(Locale.get("guiosmaddrdisplay.AddrTitle")/*Addressing*/);

		addCommand(UPLOAD_CMD);
		addCommand(BACK_CMD);

		this.parent = parent;
		this.lat = lat;
		this.lon = lon;
		this.nodeID = nodeID;

		setCommandListener(this);

		tfHousenumber = new TextField(Locale.get("guiosmaddrdisplay.HouseNumber")/*Housenumber*/, "", 100, TextField.ANY);
		tfHousenumber.setInitialInputMode("IS_LATIN_DIGITS");
		append(tfHousenumber);
		tfHousename = new TextField(Locale.get("guiosmaddrdisplay.HouseName")/*Housename*/, "", 100, TextField.ANY);
		append(tfHousename);
		tfPostcode = new TextField(Locale.get("guiosmaddrdisplay.Postcode")/*Postcode*/, "", 100, TextField.ANY);
		append(tfPostcode);
		tfStreet = new TextField(Locale.get("guiosmaddrdisplay.Street")/*Street*/, (streetName != null ? streetName
				: ""), 100, TextField.ANY);
		append(tfStreet);

	}

	public void commandAction(Command c, Displayable d) {

		if (c == UPLOAD_CMD) {
			sHousenumber = tfHousenumber.getString().trim();
			sHousename = tfHousename.getString().trim();
			sPostcode = tfPostcode.getString().trim();
			sStreet = tfStreet.getString().trim();

			osmNode = new OsmDataNode(-1, lat, lon);
			Hashtable tags = osmNode.getTags();
			if (sHousenumber.length() > 0) {
				tags.put("addr:housenumber", sHousenumber);
			}
			if (sHousename.length() > 0) {
				tags.put("addr:housename", sHousename);
			}
			if (sPostcode.length() > 0) {
				tags.put("addr:postcode", sPostcode);
			}

			if (sStreet.length() > 0) {
				tags.put("addr:street", sStreet);
			}

			parent.show();
			if ((GuiOsmEntityDisplay.changesetGui == null)
					|| (GuiOsmEntityDisplay.changesetGui.getChangesetID() < 0)) {
				loadState = GuiOsmEntityDisplay.LOAD_STATE_CHANGESET;
				GuiOsmEntityDisplay.changesetGui = new GuiOsmChangeset(parent,
						this);
				GuiOsmEntityDisplay.changesetGui.show();
			} else {
				loadState = GuiOsmEntityDisplay.LOAD_STATE_UPLOAD;
				uploadXML();
			}
		}

		// Normal BACK_CMD is handled in the super class
		if (c == BACK_CMD) {
			parent.show();
		}
	}

	public void uploadXML() {
		// #debug debug
		logger.debug("Uploading XML for " + this);
		String fullXML = osmNode.toXML(GuiOsmEntityDisplay.changesetGui
				.getChangesetID());
		// #debug info
		logger.info("Uploading: " + fullXML);
		String url;
		if (nodeID < 0) {
			url = Configuration.getOsmUrl() + "node/create";
		} else {
			url = Configuration.getOsmUrl() + "node/" + nodeID;
		}
		if (http == null) {
			http = new HttpHelper();
		}
		loadState = GuiOsmEntityDisplay.LOAD_STATE_UPLOAD;
		http.uploadData(url, fullXML, true, this, Configuration
				.getOsmUsername(), Configuration.getOsmPwd());
	}

	public void show() {
		ShareNav.getInstance().show(this);
	}

	public void setProgress(String message) {
		// Nothing to do here
	}

	public void startProgress(String title) {
		// Nothing to do here
	}

	public void updateProgress(String message) {
		// Nothing to do here
	}

	public void updateProgressValue(int increment) {
		// Nothing to do here
	}

	public void uploadAborted() {
		// Nothing to do here
	}

	public void completedUpload(boolean success, String message) {
		if (success) {
			switch (loadState) {
			case GuiOsmEntityDisplay.LOAD_STATE_UPLOAD: {
				ShareNav.getInstance().alert(Locale.get("guiosmaddrdisplay.AddingAddr")/*Adding Addr*/,
						Locale.get("guiosmaddrdisplay.AddrUploadSuccess")/*Addr was successfully added to OpenStreetMap*/, 1000);
				logger.info("Adding Waypoint to mark where Addr was uploaded to OSM");
				PositionMark waypt = new PositionMark(lat, lon);
				waypt.displayName = Locale.get("guiosmaddrdisplay.adr")/*adr: */ + sHousenumber;
				Trace.getInstance().gpx.addWayPt(waypt);

				loadState = GuiOsmEntityDisplay.LOAD_STATE_NONE;
				break;
			}
			case GuiOsmEntityDisplay.LOAD_STATE_CHANGESET: {
				loadState = GuiOsmEntityDisplay.LOAD_STATE_UPLOAD;
				uploadXML();
				break;
			}
			}
		} else {
			logger.error(Locale.get("guiosmaddrdisplay.ServerError")/*Server operation failed: */ + message);
		}
	}

}
//#endif
