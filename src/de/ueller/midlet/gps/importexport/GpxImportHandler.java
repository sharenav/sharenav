/*
 * GpsMid - Copyright (c) 2008, 2009 Kai Krueger apm at users dot sourceforge dot net
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
package de.ueller.midlet.gps.importexport;

import java.util.Date;
import java.util.Hashtable;

import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.ImageCollector;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.ProjMath;

public class GpxImportHandler implements XmlParserContentHandler {

	private final static Logger logger = Logger.getInstance(
			GpxImportHandler.class, Logger.DEBUG);

	private PositionMark wayPt;
	private Position p = new Position(0, 0, 0, 0, 0, 0, new Date());
	private boolean name = false;
	private boolean ele = false;
	private boolean time = false;
	private float maxDistance;
	private int importedWpts;
	private int importedTpts;
	private int tooFarWpts;
	private int duplicateWpts;

	private Gpx gpx;
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
				String wptName = new String(ch, start, length);
				if (wayPt.displayName == null) {
					wayPt.displayName = wptName;
				} else {
					wayPt.displayName += wptName;
				}
			}
		} else if (p != null) {
			if (ele) {
				p.altitude = Float.parseFloat(new String(ch, start, length));
			} else if (time) {
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
				logger.info("Received waypoint: " + wayPt);
				if (!gpx.existsWayPt(wayPt)) {
					gpx.addWayPt(wayPt);
					importedWpts++;
				} else {
					duplicateWpts++;
				}
				wayPt = null;
			}

		} else if (qName.equalsIgnoreCase("name")) {
			name = false;
		} else if (qName.equalsIgnoreCase("desc")) {
			name = false;
		} else if (qName.equalsIgnoreCase("trk")) {
			gpx.saveTrk();
		} else if (qName.equalsIgnoreCase("trkseg")) {

		} else if (qName.equalsIgnoreCase("trkpt")) {
			gpx.addTrkPt(p);
			importedTpts++;
			if (((importedTpts & 0x7f) == 0x7f) && (ul != null)) {
				ul.updateProgress("Imported trackpoints: " + importedTpts
						+ "\n");
			}
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
			ul.startProgress("Importing tracks");
			ul.updateProgress("Starting GPX import\n");
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
			name = true;
		} else if (qName.equalsIgnoreCase("trk")) {
			gpx.newTrk();
		} else if (qName.equalsIgnoreCase("trkseg")) {

		} else if (qName.equalsIgnoreCase("trkpt")) {
			/**
			 * Positions seem to be handeled in degree rather than radians as
			 * all the other coordinates. Be careful with the conversions!
			 */
			p.latitude = Float.parseFloat((String) atts.get("lat"));
			p.longitude = Float.parseFloat((String) atts.get("lon"));
			p.altitude = 0;
			p.course = 0;
			p.speed = 0;
		} else if (qName.equalsIgnoreCase("ele")) {
			ele = true;
		} else if (qName.equalsIgnoreCase("time")) {
			time = true;
		}
	}

	public String getMessage() {
		StringBuffer sb = new StringBuffer();
		if (maxDistance != 0) {
			sb.append("\n(max. distance: " + maxDistance + " km)");
		}
		sb.append("\n\n" + importedWpts + " waypoints imported");
		if (tooFarWpts != 0 || duplicateWpts != 0) {
			sb.append("\n\nSkipped waypoints:");
			if (maxDistance != 0) {
				sb.append("\n" + tooFarWpts + " too far away");
			}
			if (duplicateWpts != 0) {
				sb.append("\n" + duplicateWpts + " already existing");
			}
		}
		sb.append("\n\n" + importedTpts + " trackpoints imported");
		return sb.toString();

	}
}
