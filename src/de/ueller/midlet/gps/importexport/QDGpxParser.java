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

import de.ueller.gps.tools.QDXMLParser.*;
import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

public class QDGpxParser extends QDParser implements DocHandler, GpxParser {
	/**
	 * This is a wrapper for the QDParser parser. We need this wrapper so that we
	 * can use the same code for both the QDParser parser and the JSR-172 parser.
	 * Although they are close to identical, they aren't completely. Hence the
	 * need for this wrapper. We also need the wrapper to guard against missing
	 * JSRs
	 */

	private final static Logger logger = Logger.getInstance(
			QDGpxParser.class, Logger.DEBUG);

	private XmlParserContentHandler contentH;

	public void startElement(String tag, Hashtable h) {
		contentH.startElement(null, null, tag, h);
	}

	public void endElement(String tag) {
		contentH.endElement(null, null, tag);
	}

	public void startDocument() {
		contentH.startDocument();
	}

	public void endDocument() {
		contentH.endDocument();
	}

	public void text(String str) {
		char buf[] = new char[str.length()];
		str.getChars(0, str.length(), buf, 0);
		contentH.characters(buf, 0, str.length());
	}

	public boolean parse(InputStream in, XmlParserContentHandler contentH) {
		logger.debug("Starting XML parsing with QDXML");
		this.contentH = contentH;
		if (contentH == null) {
			return false;
		}
		try {
			QDParser.parse( (DocHandler) this, new InputStreamReader(in, Configuration.getUtf8Encoding()));
			return true;
		} catch (Exception e) {
			logger.exception("Error while parsing the XML file", e);
		}
		return false;
	}
}
