/*
 * GpsMid - Copyright (c) 2008, 2009 
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
//#if polish.api.webservice
package de.ueller.midlet.gps.importexport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.midlet.gps.Logger;

public class Jsr172GpxParser extends DefaultHandler implements GpxParser {

	/**
	 * This is a wrapper for the JSR172 parser. We need this wrapper so that we
	 * can use the same code for both the QDGpx parser and the JSR-172 parser.
	 * Although they are close to identical, they aren't completely. Hence the
	 * need for this wrapper. We also need the wrapper to guard against missing
	 * JSRs by calling this through the Class.forName()
	 */

	private final static Logger logger = Logger.getInstance(
			Jsr172GpxParser.class, Logger.DEBUG);

	private XmlParserContentHandler contentH;

	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {
		Hashtable attTable = new Hashtable();
		for (int i = 0; i < atts.getLength(); i++) {
			attTable.put(atts.getQName(i), atts.getValue(i));
		}
		contentH.startElement(namespaceURI, localName, qName, attTable);
	}

	public void endElement(String namespaceURI, String localName, String qName) {
		contentH.endElement(namespaceURI, localName, qName);
	}

	public void startDocument() {
		contentH.startDocument();
	}

	public void endDocument() {
		contentH.endDocument();
	}

	public void characters(char[] ch, int start, int length) {
		contentH.characters(ch, start, length);
	}

	public boolean parse(InputStream in, XmlParserContentHandler contentH) {
		if (contentH == null) {
			logger.error("Can't parse XML without a content handler");
			return false;
		}
		this.contentH = contentH;
		try {
			logger.debug("Starting to parse XML document with JSR-172 parser");
			SAXParserFactory factory = SAXParserFactory.newInstance();
			// Parse the input
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(in, this);
			return true;
		} catch (SAXException e) {
			logger.exception("Sax Parser Error", e);
		} catch (IOException e) {
			logger.exception("A Problem reading the XML file occured", e);
		} catch (ParserConfigurationException e) {
			logger.exception("Your parser was misconfigured", e);
		}
		return false;

	}
}
//#endif