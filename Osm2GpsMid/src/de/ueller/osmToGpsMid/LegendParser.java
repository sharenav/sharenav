package de.ueller.osmToGpsMid;

import java.io.FileNotFoundException;
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

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.Way;
import de.ueller.osmToGpsMid.model.WayDescription;

public class LegendParser extends DefaultHandler{
	public static Configuration config;
	
	private Hashtable<String, Hashtable<String,POIdescription>> poiMap;
	private Hashtable<String, Hashtable<String,WayDescription>> wayMap;
	private LongTri<POIdescription> pois;
	private LongTri<WayDescription> ways;
	private POIdescription currentPoi;
	private WayDescription currentWay;
	private String currentKey;
	private Hashtable<String,POIdescription> keyValuesPoi;
	private Hashtable<String,WayDescription> keyValuesWay;
	private boolean readingPOIs = false;
	private byte poiIdx = 0;
	private byte wayIdx = 0;
	private boolean nonValidStyleFile;
	
	public LegendParser(InputStream i) {
		System.out.println("Style file parser started...");
		if (config == null) {
			config = Configuration.getConfiguration();
		}
		init(i);
	}

	private void init(InputStream i) {
		try {			
			poiMap = new Hashtable<String, Hashtable<String,POIdescription>>();
			pois = new LongTri<POIdescription>();
			currentPoi = new POIdescription();
			/**
			 * Add a bogous POI description, to reserve type 0 as a special marker
			 */
			currentPoi.typeNum = poiIdx++;
			currentPoi.key = "A key that should never be hot";
			currentPoi.value = "A value that should never be triggered";
			currentPoi.description = "No description";
			pois.put(currentPoi.typeNum, currentPoi);
			
			wayMap = new Hashtable<String, Hashtable<String,WayDescription>>();
			ways = new LongTri<WayDescription>();
			currentWay = new WayDescription();
			/**
			 * Add a bogous Way description, to reserve type 0 as a special marker
			 */
			currentWay.typeNum = wayIdx++;
			currentWay.key = "A key that should never be hot";
			currentWay.value = "A value that should never be triggered";
			currentWay.description = "No description";
			ways.put(currentWay.typeNum, currentWay);
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			// Parse the input
            SAXParser saxParser = factory.newSAXParser();
            nonValidStyleFile = false;
            saxParser.parse(i, this);
            if (nonValidStyleFile) {
            	System.out.println("ERROR: your style file is not valid. Please correct the file and try Osm2GpsMid again");
            	System.exit(1);
            }
		} catch (FileNotFoundException fnfe) {
			System.out.println("ERROR, could not find necessary file: " + fnfe.getMessage());
			System.out.println("If the missing file is the style-file.dtd you can");
			System.out.println("rename a copy of Osm2GpsMid*.jar to Osm2GpsMid*.zip");
			System.out.println("and extract the style-file.dtd from there.");
			System.exit(1);
		} catch (IOException e) {
			System.out.println("ERROR: IOException: " + e);			
			System.exit(1);
		} catch (SAXException e) {
			System.out.println("ERROR: SAXException: " + e);			
			System.exit(1);
		} catch (Exception e) {
			System.out.println("ERROR: Other Exception: " + e);			
			System.exit(1);
		}
	}

	public void startDocument() {		
	}

	public void endDocument() {		
	}
	
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {		
//		System.out.println("start " + localName + " " + qName);
		if (qName.equals("pois")) {
			readingPOIs = true;			
		}
		if (qName.equals("background")) {
			try {
				Configuration.getConfiguration().background_color = Integer.parseInt(atts.getValue("color"),16);
			} catch (NumberFormatException nfe){
				System.out.println("ERROR: Couldn't read the background color correctly, using default");
				Configuration.getConfiguration().background_color = 0x009bFF9b;				
			}
		}
		if (readingPOIs) {
			if (qName.equals("key")) {
				currentKey = atts.getValue("tag");
				keyValuesPoi = poiMap.get(currentKey);
				if (keyValuesPoi == null) {
					keyValuesPoi = new Hashtable<String,POIdescription>();
					poiMap.put(currentKey, keyValuesPoi);
				}
			}
			if (qName.equals("value")) {				
				currentPoi = new POIdescription();
				currentPoi.typeNum = poiIdx++;
				currentPoi.key = currentKey;
				currentPoi.value = atts.getValue("name"); 
				keyValuesPoi.put(currentPoi.value, currentPoi);
				pois.put(currentPoi.typeNum, currentPoi);
			}
			if (qName.equals("description")) {
				currentPoi.description = atts.getValue("desc");
			}
			if (qName.equals("namekey")) {
				currentPoi.nameKey = atts.getValue("tag");
			}
			if (qName.equals("namefallback")) {
				currentPoi.nameFallbackKey = atts.getValue("tag");
			}
			if (qName.equals("scale")) {
				try {
					currentPoi.minImageScale = config.getRealScale( Integer.parseInt(atts.getValue("scale")) );
				} catch (NumberFormatException nfe) {
					System.out.println("Error: scale for " +currentPoi.description + " is incorrect");
				}
				if (currentPoi.minTextScale == 0)
					currentPoi.minTextScale = currentPoi.minImageScale;
			}
			if (qName.equals("textscale")) {
				try {
					currentPoi.minTextScale = config.getRealScale( Integer.parseInt(atts.getValue("scale")) );
				} catch (NumberFormatException nfe) {
					System.out.println("Error: textscale for " +currentPoi.description + " is incorrect");
				}
			}
			if (qName.equals("image")) {
				currentPoi.image = atts.getValue("src");
			}
			if (qName.equals("searchIcon")) {			
				currentPoi.searchIcon = atts.getValue("src");
			}
			if (qName.equals("imageCentered")) {
				currentPoi.imageCenteredOnNode = atts.getValue("value").equalsIgnoreCase("true");
			}
		} else {
			if (qName.equals("keyW")) {
				currentKey = atts.getValue("tag");
				keyValuesWay = wayMap.get(currentKey);
				if (keyValuesWay == null) {
					keyValuesWay = new Hashtable<String,WayDescription>();
					wayMap.put(currentKey, keyValuesWay);
				}
			}
			if (qName.equals("Wvalue")) {
				currentWay = new WayDescription();
				currentWay.typeNum = wayIdx++;
				currentWay.key = currentKey;
				currentWay.value = atts.getValue("name"); 
				keyValuesWay.put(currentWay.value, currentWay);
				ways.put(currentWay.typeNum, currentWay);
			}
			if (qName.equals("description")) {
				currentWay.description = atts.getValue("desc");
			}
			if (qName.equals("namekey")) {
				currentWay.nameKey = atts.getValue("tag");
			}
			if (qName.equals("namefallback")) {
				currentWay.nameFallbackKey = atts.getValue("tag");
			}
			if (qName.equals("scale")) {
				try {
					currentWay.minScale = config.getRealScale( Integer.parseInt(atts.getValue("scale")) );				
				} catch (NumberFormatException nfe) {
					System.out.println("Error: scale for " +currentWay.description + " is incorrect");
				}
				if (currentWay.minTextScale == 0)
						currentWay.minTextScale = currentWay.minScale;			}

			if (qName.equals("textscale")) {
				try {
					currentWay.minTextScale = config.getRealScale( Integer.parseInt(atts.getValue("scale")) );				
				} catch (NumberFormatException nfe) {
					System.out.println("Error: textscale for " +currentWay.description + " is incorrect");
				}
			}
			
			if (qName.equals("isArea")) {
				currentWay.isArea = atts.getValue("area").equalsIgnoreCase("true");
			}
			if (qName.equals("lineColor")) {
				try {
					currentWay.lineColor = Integer.parseInt(atts.getValue("color"),16);
				} catch (NumberFormatException nfe){
					System.out.println("Error: lineColor for " + currentWay.description + " is incorrect. Must be a hex coded ARGB value");
				}
			}
			if (qName.equals("borderColor")) {
				try {
					currentWay.boardedColor = Integer.parseInt(atts.getValue("color"),16);				
				} catch (NumberFormatException nfe){
					System.out.println("Error: borderColor for " + currentWay.description + " is incorrect. Must be a hex coded ARGB value");
				}
			}
			if (qName.equals("wayWidth")) {
				try {
					currentWay.wayWidth = Integer.parseInt(atts.getValue("width"));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: wayWidth for " +currentWay.description + " is incorrect");
				}
			}
			if (qName.equals("lineStyle")) {				
				currentWay.lineStyleDashed = atts.getValue("dashed").equalsIgnoreCase("true");				
			}
			if (qName.equals("routing")) {
				currentWay.routable = atts.getValue("accessible").equalsIgnoreCase("true");
				String typicalSpeed = atts.getValue("speed");
				if (typicalSpeed != null) {
					try {
						currentWay.typicalSpeed = Integer.parseInt(typicalSpeed);
					} catch (NumberFormatException nfe) {
						System.out.println("Invalid speed for " + currentWay.description);
					}
				} else
					currentWay.typicalSpeed = 50;								
			}
			if (qName.equals("force_to")) {
				try {
					currentWay.forceToLayer = Byte.parseByte(atts.getValue("layer"));
				} catch (NumberFormatException nfe) {
					// Just ignore this entry if it is not correct
				}
			}
			
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
	
	public Hashtable<String, Hashtable<String,WayDescription>> getWayLegend() {
		return wayMap;
	}
	
	public WayDescription getWayDesc(byte type) {
		return ways.get(type);
	}
	
	public Collection<WayDescription> getWayDescs() {
		return ways.values();
	}
	
	public void warning(SAXParseException e) throws SAXException {
        System.out.println("Warning: " + e.getMessage()); 
        
     }
     public void error(SAXParseException e) throws SAXException {
        System.out.println("Error on line " + e.getLineNumber()+ " (remember ordering matters): " + e.getMessage());
        nonValidStyleFile = true;
     }
}
