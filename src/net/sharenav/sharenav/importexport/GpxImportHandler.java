/*
 * ShareNav - Copyright (c) 2008, 2009 Kai Krueger apm at users dot sourceforge dot net
 *        - Copyright (c) 2008                 sk750 at users dot sourceforge dot net
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
 */

package net.sharenav.sharenav.importexport;

import java.util.Hashtable;

import net.sharenav.sharenav.data.Gpx;
import net.sharenav.sharenav.data.Position;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.sharenav.graphics.ImageCollector;
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;

import de.enough.polish.util.Locale;

public class GpxImportHandler implements XmlParserContentHandler {

	private final static Logger logger = Logger.getInstance(
			GpxImportHandler.class, Logger.DEBUG);

	/** The way point that is currently parsed. */
	private PositionMark wayPt;
	/** The track point that is currently parsed. */
	private Position trackPt = new Position(0, 0, 0, 0, 0, 0, System.currentTimeMillis());
	/** Flag if current element is "name" */
	private boolean name = false;
	/** Flag if current element is "desc" */
	private boolean desc = false;
	/** Flag if current element is "ele" */
	private boolean ele = false;
	/** Flag if current element is "time" */
	private boolean time = false;
	/** Temporarily holds the name of the way point. */
	private String wptName = null;
	/** Temporarily holds the description of the way point. */
	private String wptDesc = null;
	/** Way points further away than this (in kilometers) will not be imported. */
	private float maxDistance;
	/** Counter how many way points were imported. */
	private int importedWpts;
	/** Counter how many track points were imported. */
	private int importedTpts;
	/** Counter how many way points were ignored because they are too far away. */
	private int tooFarWpts;
	/** Counter how many way points were ignored because there is an identical one. */
	private int duplicateWpts;
	/** Parent which manages the way points and track points. */
	private Gpx gpx;
	/** Listener for progress messages */
	private UploadListener ul;

	public GpxImportHandler(float maxDistance, Gpx gpx, UploadListener ul) {
		this.maxDistance = maxDistance;
		this.gpx = gpx;
		this.ul = ul;
		importedWpts = 0;
		importedTpts = 0;
		tooFarWpts = 0;
		duplicateWpts = 0;
	}

	public void characters(char[] ch, int start, int length) {
		if (wayPt != null) {
			if (name) {
				wptName = new String(ch, start, length);
			} else if (desc) {
				wptDesc = new String(ch, start, length);				
			}
		} else if (trackPt != null) {
			if (ele) {
				trackPt.altitude = Float.parseFloat(new String(ch, start, length));
			} else if (time) {
				// Time stamps of track points are not used by ShareNav. 
			}
		}
	}

	public void endDocument() {
		// #debug debug
		logger.debug("Finished parsing XML document");
	}

	public void endElement(String namespaceURI, String localName, String qName) {
		if (qName.equalsIgnoreCase("wpt")) {
			if (wayPt != null) {
				// Only use "desc" if there is no "name".
				if (wptName != null) {
					wayPt.displayName = wptName;
				} else if (wptDesc != null) {
					wayPt.displayName = wptDesc;
				}
				logger.info("Received waypoint: " + wayPt);
				if (!gpx.existsWayPt(wayPt)) {
					gpx.addWayPt(wayPt);
					importedWpts++;
				} else {
					duplicateWpts++;
				}
				wayPt = null;
				wptName = null;
				wptDesc = null;
			}

		} else if (qName.equalsIgnoreCase("name")) {
			name = false;
		} else if (qName.equalsIgnoreCase("desc")) {
			desc = false;
		} else if (qName.equalsIgnoreCase("trk")) {
			// This is already running in the processorThread of class Gpx,
			// so we have to do the save directly (doSaveTrk()) instead of 
			// triggering a thread job (saveTrk()).
			gpx.doSaveTrk();
		} else if (qName.equalsIgnoreCase("trkseg")) {
			// Ignored, new track is only started at <trk>!
		} else if (qName.equalsIgnoreCase("trkpt")) {
			gpx.addTrkPt(trackPt);
			importedTpts++;
		} else if (qName.equalsIgnoreCase("ele")) {
			ele = false;
		} else if (qName.equalsIgnoreCase("time")) {
			time = false;
		}
	}

	public void startDocument() {
		// #debug debug
		logger.debug("Started parsing XML document");
		if (ul != null) {
			ul.startProgress(Locale.get("gpximporthandler.GPXImport")/*GPX Import*/); // this is shown for both waypoint and track import
			ul.updateProgress(Locale.get("gpximporthandler.StartingGPXimport")/*Starting GPX import*/ + "\n");
		}
	}

	public void startElement(String namespaceURI, String localName,
			String qName, Hashtable atts) {
		if (qName.equalsIgnoreCase("wpt")) {
			/**
			 * Gpx files have coordinates in degrees. We store them internally
			 * as radians. So we need to convert these.
			 */
			float node_lat = Float.parseFloat((String) atts.get("lat"))
					* MoreMath.FAC_DECTORAD;
			float node_lon = Float.parseFloat((String) atts.get("lon"))
					* MoreMath.FAC_DECTORAD;
			float distance = 0;

			boolean inRadius = true;
			if (maxDistance != 0) {
				distance = ProjMath.getDistance(node_lat, node_lon,
						ImageCollector.mapCenter.radlat,
						ImageCollector.mapCenter.radlon);
				distance = (int) (distance / 100.0f) / 10.0f;
				if (distance > maxDistance) {
					inRadius = false;
					tooFarWpts++;
				}
			}
			// System.out.println("MaxDist: " + maxDistance + " Distance: " +
			// distance + " inRadius: " + inRadius);
			if (inRadius) {
				wayPt = new PositionMark(node_lat, node_lon);
			}
		} else if (qName.equalsIgnoreCase("name")) {
			name = true;
		} else if (qName.equalsIgnoreCase("desc")) {
			desc = true;
		} else if (qName.equalsIgnoreCase("trk")) {
			gpx.newTrk(false);
		} else if (qName.equalsIgnoreCase("trkseg")) {

		} else if (qName.equalsIgnoreCase("trkpt")) {
			/**
			 * Positions seem to be handled in degree rather than radians as
			 * all the other coordinates. Be careful with the conversions!
			 */
			trackPt.latitude = Float.parseFloat((String) atts.get("lat"));
			trackPt.longitude = Float.parseFloat((String) atts.get("lon"));
			trackPt.altitude = 0;
			trackPt.course = 0;
			trackPt.speed = 0;
		} else if (qName.equalsIgnoreCase("ele")) {
			ele = true;
		} else if (qName.equalsIgnoreCase("time")) {
			time = true;
		}
	}

	public String getMessage() {
		StringBuffer sb = new StringBuffer();
		if (maxDistance != 0) {
			sb.append("\n(" + Locale.get("gpximporthandler.MaxDistance")/*Max. distance*/ + ": " + maxDistance + " " + Locale.get("gpximporthandler.km")/*km*/ + ")");
		}
		sb.append("\n\n" + importedWpts + " " + Locale.get("gpximporthandler.WaypointsImported")/*waypoints imported*/);
		if (tooFarWpts != 0 || duplicateWpts != 0) {
			sb.append("\n\n" + Locale.get("gpximporthandler.SkippedWaypoints")/*Skipped waypoints*/ + ":");
			if (maxDistance != 0) {
				sb.append("\n" + tooFarWpts + " " + Locale.get("gpximporthandler.TooFarAway")/*too far away*/);
			}
			if (duplicateWpts != 0) {
				sb.append("\n" + duplicateWpts + " " + Locale.get("gpximporthandler.AlreadyExisting")/*already existing*/);
			}
		}
		sb.append("\n\n" + importedTpts + " " + Locale.get("gpximporthandler.TrackpointsImported")/*trackpoints imported*/);
		return sb.toString();

	}
}
