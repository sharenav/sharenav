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
package de.ueller.midlet.gps.importexport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Hashtable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import uk.co.wilson.xml.MinML2;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

public class MinML2GpxParser extends MinML2 implements GpxParser {
	/**
	 * This is a wrapper for the MinML2 parser. We need this wrapper so that we
	 * can use the same code for both the MinML2 parser and the JSR-172 parser.
	 * Although they are close to identical, they aren't completely. Hence the
	 * need for this wrapper. We also need the wrapper to guard against missing
	 * JSRs
	 */

	private final static Logger logger = Logger.getInstance(
			MinML2GpxParser.class, Logger.DEBUG);

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
		logger.debug("Starting XML parsing with MinML");
		this.contentH = contentH;
		if (contentH == null) {
			return false;
		}
		try {
			parse(new InputStreamReader(in, Configuration.getUtf8Encoding()));
			return true;
		} catch (SAXException e) {
			logger.exception("Error while parsing the XML file", e);
		} catch (IOException e) {
			logger.exception("Error while reading the XML file", e);
		}
		return false;
	}
}
