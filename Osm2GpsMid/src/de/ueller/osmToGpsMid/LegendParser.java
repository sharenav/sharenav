package de.ueller.osmToGpsMid;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.Way;

public class LegendParser extends DefaultHandler{
	private Hashtable<String, Hashtable<String,POIdescription>> poiMap;
	private LongTri<POIdescription> pois;
	private POIdescription current;
	private String currentKey;
	private Hashtable<String,POIdescription> keyValues;
	private boolean readingPOIs = false;
	private byte poiIdx;
	
	
	public LegendParser(InputStream i) {
		System.out.println("Style file parser started...");		
		init(i);
	}

	private void init(InputStream i) {
		try {			
			poiMap = new Hashtable<String, Hashtable<String,POIdescription>>();
			pois = new LongTri<POIdescription>();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			// Parse the input
            SAXParser saxParser = factory.newSAXParser();
            System.out.println("Validating: " + saxParser.isValidating());
            System.out.println("Schema: " + saxParser.getSchema());
            saxParser.parse(i, this);            
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("SAXException: " + e);
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Other Exception: " + e);
			e.printStackTrace();
		}
	}

	public void startDocument() {
		System.out.println("Start of Document");
	}

	public void endDocument() {
		System.out.println("End of Document");
	}
	
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {		
//		System.out.println("start " + localName + " " + qName);
		if (qName.equals("pois")) {
			readingPOIs = true;			
		}
		if (qName.equals("key")) {
			currentKey = atts.getValue("tag");
			keyValues = poiMap.get(currentKey);
			if (keyValues == null) {
				keyValues = new Hashtable<String,POIdescription>();
				poiMap.put(currentKey, keyValues);
			}
		}
		if (qName.equals("value")) {
			current = new POIdescription();
			current.typeNum = poiIdx++;
			current.key = currentKey;
			current.value = atts.getValue("name"); 
			keyValues.put(current.value, current);
			pois.put(current.typeNum, current);
		}
		if (qName.equals("description")) {
			current.description = atts.getValue("desc");
		}
		if (qName.equals("namekey")) {
			current.nameKey = atts.getValue("tag");
		}
		if (qName.equals("namefallback")) {
			current.nameFallbackKey = atts.getValue("tag");
		}
		if (qName.equals("scale")) {
			current.minImageScale = Integer.parseInt(atts.getValue("scale"));
			if (current.minTextScale == 0)
				current.minTextScale = current.minImageScale;
		}
		if (qName.equals("textscale")) {
			current.minTextScale = Integer.parseInt(atts.getValue("scale"));
		}
		if (qName.equals("image")) {
			current.image = atts.getValue("src");
		}
		if (qName.equals("searchIcon")) {			
			current.searchIcon = atts.getValue("src");
		}
		if (qName.equals("imageCentered")) {
			current.imageCenteredOnNode = atts.getValue("value").equalsIgnoreCase("true");
		}
	} // startElement

	public void endElement(String namespaceURI, String localName, String qName) {		
		if (qName.equals("pois")) {
			readingPOIs = false;
		}				
	} // endElement

	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println("Error: " + e);
		throw e;	
	}
	public Hashtable<String, Hashtable<String,POIdescription>> getPOIlegend() {
		return poiMap;
	}
	
	public POIdescription getPOIDesc(byte type) {
		return pois.get(type);
	}
	
	public Collection<POIdescription> getPOIDescs() {
		return pois.values();
	}
}
