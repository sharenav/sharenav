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
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.sharenav.sharenav.importexport.QDGpxParser;
import net.sharenav.sharenav.importexport.XmlParserContentHandler;
import net.sharenav.util.HttpHelper;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class OsmDataWay extends OsmDataEntity implements XmlParserContentHandler{
	private final static Logger logger = Logger.getInstance(
			OsmDataWay.class, Logger.DEBUG);
	
	private Vector nodes;

	public OsmDataWay(String fullXML, int osmID) {
		super(fullXML, osmID);
		
	}
	
	public OsmDataWay(int osmID) {
		super(osmID);
	}
	
	protected void parseXML() {
		nodes = new Vector();
		tags = new Hashtable();
		
		QDGpxParser parser = new QDGpxParser();
		logger.debug("Starting Way XML parsing with QDXML");
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
		if (name.equalsIgnoreCase("way")) {
			try {
				int id = Integer.parseInt((String)atts.get("id"));
				if (id == osmID) {
					ignoring = false;
					editBy = (String)atts.get("user");
					editTime = (String)atts.get("timestamp");
					version = Integer.parseInt((String)atts.get("version"));
					changesetID = Integer.parseInt((String)atts.get("changeset"));
				} else {
					ignoring = true;
				}
			} catch (NumberFormatException nfe) {
				logger.exception(Locale.get("osmdataway.FailedParsingOSMId")/*Failed to parse osm id*/, nfe);
			}
		}
		if (ignoring) {
			return;
		}
		if (name.equalsIgnoreCase("nd")) {
			try {
				String tmp = (String)atts.get("ref");
				long nd_ref = Long.parseLong(tmp);
				nodes.addElement(new Long(nd_ref));
			} catch (NumberFormatException nfe) {
				logger.exception(Locale.get("osmdataway.FailedParsingOSMId")/*Failed to parse osm id*/, nfe);
			}
		}
		if (name.equalsIgnoreCase("tag")) {
			tags.put(atts.get("k"), atts.get("v"));
		}
	}

	public void reverseWay() {
		Vector revNodes = new Vector();
		for (int i = nodes.size() - 1; i >= 0; i--) {
			revNodes.addElement(nodes.elementAt(i));
		}
		nodes = revNodes;
	}
	
	public String toXML(int commitChangesetID) {
		String xml;
		
		xml  = "<?xml version='1.0' encoding='utf-8'?>\r\n";
		xml += "<osm version='0.6' generator='ShareNav'>\r\n";
		xml += "<way id='" + osmID + "' version='" + version + 
				"' changeset='" + commitChangesetID + "'>\r\n";
		for (int i = 0; i < nodes.size(); i++) {
			xml += "<nd ref='" + nodes.elementAt(i) + "' />\r\n";
		}
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			if (key.equalsIgnoreCase("created_by")) {
				//Drop created_by tag, as it has moved into changesets
			} else {
				xml += "<tag k='" + HttpHelper.escapeXML(key) + "' v='" + HttpHelper.escapeXML((String)tags.get(key)) + "' />\r\n";
			}
		}
		xml += "</way>\r\n";
		xml += "</osm>\r\n";
		
		return xml;
	}
	
	public String toDeleteXML(int commitChangesetID) {
		String xml;
		
		// FIXME test and correct this functions if necessary
		xml  = "<?xml version='1.0' encoding='utf-8'?>\r\n";
		xml += "<osm version='0.6' generator='ShareNav'>\r\n";
		xml += "<way id='" + osmID + "' version='" + version + 
				"' changeset='" + commitChangesetID + "'/>\r\n";
		xml += "</osm>\r\n";
		
		return xml;
	}
	
	public String toString() {
		String res;
		res = "\n" + Locale.get("osmdataway.OSMway")/*OSM way*/ + " " + osmID + "\n";
		res += "     " + Locale.get("osmdataway.LastEditedBy")/*last edited by*/ + " " + editBy + " " + Locale.get("osmdataway.at")/*at*/ + " " + editTime + "\n";
		res += "     " + Locale.get("osmdataway.version")/*version*/ + " " + version + " " + Locale.get("osmdataway.InChangeset")/*in changeset*/ + " " + changesetID + "\n";
		res += " Tags:\n";
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			res += "   " + key + " = " + tags.get(key) + "\n";
		}
		res += " " + Locale.get("osmdataway.Nodes")/*Nodes*/ + ":\n";
		for (int i = 0; i < nodes.size(); i++) {
			res += "   ref=" + nodes.elementAt(i) + "\n";
		}
		return res;
	}
}
//#endif