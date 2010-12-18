package de.ueller.midlet.gps.importexport;

import java.util.Hashtable;

public interface XmlParserContentHandler {
	
	public void startElement(String namespaceURI, String localName, String qName, Hashtable atts); 
	public void endElement(String namespaceURI, String localName, String qName);
	public void startDocument();
	public void endDocument();
	public void characters(char[] ch, int start, int length);

}
