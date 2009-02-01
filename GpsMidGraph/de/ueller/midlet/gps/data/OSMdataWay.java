package de.ueller.midlet.gps.data;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.importexport.QDGpxParser;
import de.ueller.midlet.gps.importexport.XmlParserContentHandler;

public class OSMdataWay implements XmlParserContentHandler{
	private final static Logger logger = Logger.getInstance(
			OSMdataWay.class, Logger.DEBUG);
	
	private String fullXML;
	private int osmID;
	
	private String editTime;
	private String editBy;
	private Vector nodes;
	private Hashtable tags;
	
	private boolean ignoring;

	public OSMdataWay(String fullXML, int osmID) {
		this.fullXML = fullXML;
		this.osmID = osmID;
		this.nodes = new Vector();
		this.tags = new Hashtable();
		parseXML();
	}
	
	private void parseXML() {
		QDGpxParser parser = new QDGpxParser();
		logger.debug("Starting Way XML parsing with QDXML");
		InputStream in = new ByteArrayInputStream(fullXML.getBytes());
		parser.parse(in, this);
	}
	
	public String getXML() {
		return fullXML;
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
				} else {
					ignoring = true;
				}
			} catch (NumberFormatException nfe) {
				logger.exception("Failed to parse osm id", nfe);
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
				logger.exception("Failed to parse osm id", nfe);
			}
		}
		if (name.equalsIgnoreCase("tag")) {
			tags.put(atts.get("k"), atts.get("v"));
		}
	}
	
	public Hashtable getTags() {
		return tags;
	}
	
	public String getEditor() {
		return editBy;
	}
	
	public String getEditTime() {
		return editTime;
	}
	
	public String toXML() {
		String xml;
		
		xml  = "<?xml version='1.0' encoding='utf-8'?>\r\n";
		xml += "<osm version='0.5' generator='GpsMid'>\r\n";
		xml += "<way id='" + osmID + "'>\r\n";
		for (int i = 0; i < nodes.size(); i++) {
			xml += "<nd ref='" + nodes.elementAt(i) + "' />\r\n";
		}
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			if (key.equalsIgnoreCase("created_by")) {
				xml += "<tag k='created_by' v='GpsMid' />\r\n";
			} else {
				xml += "<tag k='" + key + "' v='" + tags.get(key) + "' />\r\n";
			}
		}
		xml += "</way>\r\n";
		xml += "</osm>\r\n";
		
		return xml;
	}
	
	public String toString() {
		String res;
		res = "\nOSM way " + osmID + "\n";
		res += "     last edited by " + editBy + " at " + editTime + "\n";
		res += " Tags:\n";
		Enumeration enKey = tags.keys();
		while (enKey.hasMoreElements()) {
			String key = (String)enKey.nextElement();
			res += "   " + key + " = " + tags.get(key) + "\n";
		}
		res += " Nodes:\n";
		for (int i = 0; i < nodes.size(); i++) {
			res += "   ref=" + nodes.elementAt(i) + "\n";
		}
		return res;
	}
}
