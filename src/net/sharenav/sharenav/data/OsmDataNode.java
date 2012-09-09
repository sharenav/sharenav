/**
 * This file is part of ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */

//#if polish.api.osm-editing
package net.sharenav.sharenav.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import net.sharenav.sharenav.importexport.QDGpxParser;
import net.sharenav.sharenav.importexport.XmlParserContentHandler;
import net.sharenav.util.HttpHelper;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class OsmDataNode extends OsmDataEntity implements XmlParserContentHandler{
	private final static Logger logger = Logger.getInstance(
			OsmDataNode.class, Logger.DEBUG);
	
	/**
	 * latitude and longitude of the OSM data node in radians
	 */
	private float lat;
	private float lon;

	public OsmDataNode(String fullXML, int osmID) {
		super(fullXML, osmID);
	}
	
	/**
	 * 
	 * @param osmID OSM id of the node, negative if it is a new node
	 * @param lat latitude in radians
	 * @param lon longitude in radians
	 */
	public OsmDataNode(int osmID, float lat, float lon) {
		super(osmID);
		this.lat = lat;
		this.lon = lon;
	}
	
	public float getLat() {
		return lat;
	}
	
	public float getLon() {
		return lon;
	}
	
	protected void parseXML() {
		QDGpxParser parser = new QDGpxParser();
		logger.debug("Starting Node XML parsing with QDXML");
		try {
			InputStream in = new ByteArrayInputStream(fullXML.getBytes(Configuration.getUtf8Encoding()));
			parser.parse(in, this);
		} catch (java.io.UnsupportedEncodingException uee) {
		}
	}
	
	public void characters(char[] ch, int start, int length) {
	}

	public void endDocument() {
	}

	public void endElement(String namespaceURI, String localName, String name) {
	}

	public void startDocument() {
	}

	public void startElement(String namespaceURI, String localName,
			String name, Hashtable atts) {
		if (name.equalsIgnoreCase("node")) {
			try {
				int id = Integer.parseInt((String)atts.get("id"));
				if (id == osmID) {
					ignoring = false;
					editBy = (String)atts.get("user");
					editTime = (String)atts.get("timestamp");
					version = Integer.parseInt((String)atts.get("version"));
					changesetID = Integer.parseInt((String)atts.get("changeset"));
					// FIXME for OSM POI editing, using a float loses some accuracy,
					// eg. 	60,2077779, 24,6411467 to 60,20778, 24,641148
					// - would be proper to store strings to keep the coordinates the same
					lat = Float.parseFloat((String)atts.get("lat"));
					lon = Float.parseFloat((String)atts.get("lon"));
				} else {
					ignoring = true;
				}
			} catch (NumberFormatException nfe) {
				logger.exception(Locale.get("osmdatanode.FailedParseingOSMid")/*Failed to parse osm id*/, nfe);
			}
		}
		if (ignoring) {
			return;
		}

		if (name.equalsIgnoreCase("tag")) {
			tags.put(atts.get("k"), atts.get("v"));
		}
	}

	public String toXML(int commitChangesetID) {
		String xml;
		
		xml  = "<?xml version='1.0' encoding='utf-8'?>\r\n";
		xml += "<osm version='0.6' generator='ShareNav'>\r\n";
		xml += "<node id='" + osmID + "' lat='" + lat + 
			"' lon='" + lon + "' version='" + version + 
				"' changeset='" + commitChangesetID + "'>\r\n";
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			if (key.equalsIgnoreCase("created_by")) {
				//Drop created_by tag, as it has moved into changesets
			} else {
				xml += "<tag k='" + HttpHelper.escapeXML(key) + "' v='" + HttpHelper.escapeXML((String)tags.get(key)) + "' />\r\n";
			}
		}
		xml += "</node>\r\n";
		xml += "</osm>\r\n";
		
		return xml;
	}
	
	public String toDeleteXML(int commitChangesetID) {
		String xml;

		// FIXME should be correct but doesn't work, gives a 400 bad request error
		xml  = "<?xml version='1.0' encoding='utf-8'?>\r\n";
		xml += "<osm version='0.6' generator='ShareNav'>\r\n";
		xml += "<node id='" + osmID + "' version='" + version + "'"
			+ " lat='" + lat + "' lon='" + lon
			+ "' changeset='" + commitChangesetID + "'/>\r\n";
		xml += "</osm>\r\n";
		
		return xml;
	}
	
	public String toString() {
		String res;
		res = "\n " + Locale.get("osmdatanode.OSMnode")/*OSM node*/ + " " + osmID + "\n";
		res += "     " + Locale.get("osmdatanode.LastEditedBy")/*last edited by*/ + editBy + Locale.get("osmdatanode.at")/* at */ + editTime + "\n";
		res += "     " + Locale.get("osmdatanode.version")/*version*/ + " " + version + " " + Locale.get("osmdatanode.inChangeset")/*in changeset*/ + " " + changesetID + "\n";
		res += " " + Locale.get("osmdatanode.Tags")/*Tags*/ + ":\n";
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			res += "   " + key + " = " + tags.get(key) + "\n";
		}
		return res;
	}
}
//#endif