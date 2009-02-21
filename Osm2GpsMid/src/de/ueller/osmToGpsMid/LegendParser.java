/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 * Copyright (C) 2008  sk750
 * 
 */
package de.ueller.osmToGpsMid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.model.ConditionTuple;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.SoundDescription;
import de.ueller.osmToGpsMid.model.WayDescription;

public class LegendParser extends DefaultHandler{
	public static Configuration config;
	
	private Hashtable<String, Hashtable<String,Set<POIdescription>>> poiMap;
	private Hashtable<String, Hashtable<String,Set<WayDescription>>> wayMap;
	private LongTri<POIdescription> pois;
	private LongTri<WayDescription> ways;
	private Vector<SoundDescription> sounds;
	private POIdescription currentPoi;
	private SoundDescription currentSound;
	private WayDescription currentWay;
	private String currentKey;
	private Hashtable<String,Set<POIdescription>> keyValuesPoi;
	private Hashtable<String,Set<WayDescription>> keyValuesWay;
	private final byte READING_WAYS=0;
	private final byte READING_POIS=1;
	private final byte READING_SOUNDS=2;
	private byte readingType = READING_WAYS;
	
	private byte poiIdx = 0;
	private byte wayIdx = 0;
	private boolean nonValidStyleFile;
	
	public class DTDresolver implements EntityResolver {

		/* (non-Javadoc)
		 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String, java.lang.String)
		 */
		@Override
		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
			
			/**
			 * Use the style-file.dtd that is internl to the Osm2GpsMid jar
			 */
			if (systemId.endsWith("style-file.dtd")) {
				
					InputStream is = this.getClass().getResourceAsStream("/style-file.dtd");
					if (is != null)
						return new InputSource(is);
					else {
						System.out.println("Warning: Could not read internal dtd file");
						return null;
					}
				
			}
			System.out.println("Resolving entity: " + systemId);
			
			return null;
		}
		
	}
	
	public LegendParser(InputStream i) {
		System.out.println("Style file parser started...");
		if (config == null) {
			config = Configuration.getConfiguration();
		}
		init(i);
	}

	private void init(InputStream i) {
		try {			
			poiMap = new Hashtable<String, Hashtable<String,Set<POIdescription>>>();
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
			
			wayMap = new Hashtable<String, Hashtable<String,Set<WayDescription>>>();
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
			
			sounds = new Vector<SoundDescription>();		
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			// Parse the input
			nonValidStyleFile = false;
            XMLReader xmlReader = factory.newSAXParser().getXMLReader();
            xmlReader.setEntityResolver(new DTDresolver());            
            xmlReader.setContentHandler(this);
            xmlReader.parse(new InputSource(i));            
            if (nonValidStyleFile) {
            	System.out.println("ERROR: your style file is not valid. Please correct the file and try Osm2GpsMid again");
            	System.exit(1);
            }
		} catch (FileNotFoundException fnfe) {
			System.out.println("ERROR, could not find necessary file: " + fnfe.getMessage());
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
			readingType=READING_POIS;			
		} else if (qName.equals("ways")) {
			readingType=READING_WAYS;			
		} else if (qName.equals("sounds")) {
			readingType=READING_SOUNDS;			
		} else if (qName.equals("background")) {
			try {
				Configuration.getConfiguration().background_color = Integer.parseInt(atts.getValue("color"),16);
			} catch (NumberFormatException nfe){
				System.out.println("ERROR: Couldn't read the background color correctly, using default");
				Configuration.getConfiguration().background_color = 0x009bFF9b;				
			}
		} else if (qName.equals("routeLine")) {
			try {
				Configuration.getConfiguration().routeColor = Integer.parseInt(atts.getValue("lineColor"),16);
			} catch (NumberFormatException nfe){
				System.out.println("ERROR: Couldn't read the routeColor correctly, using default");
			}
			try {
				Configuration.getConfiguration().routeBorderColor = Integer.parseInt(atts.getValue("borderColor"),16);
			} catch (NumberFormatException nfe){
				System.out.println("ERROR: Couldn't read the routeBorderColor correctly, using default");
			}
		} else if (qName.equals("priorRouteLine")) {
			try {
				Configuration.getConfiguration().priorRouteColor = Integer.parseInt(atts.getValue("lineColor"),16);
			} catch (NumberFormatException nfe){
				System.out.println("ERROR: Couldn't read the priorRouteColor correctly, using default");
			}
			try {
				Configuration.getConfiguration().priorRouteBorderColor = Integer.parseInt(atts.getValue("borderColor"),16);
			} catch (NumberFormatException nfe){
				System.out.println("ERROR: Couldn't read the priorRouteBorderColor correctly, using default");
				Configuration.getConfiguration().priorRouteBorderColor = 0x009bFF9b;				
			}
		}
		switch (readingType) {
			case READING_POIS:
				if (qName.equals("key")) {
					currentKey = atts.getValue("tag");
					keyValuesPoi = poiMap.get(currentKey);
					if (keyValuesPoi == null) {
						keyValuesPoi = new Hashtable<String,Set<POIdescription>>();
						poiMap.put(currentKey, keyValuesPoi);
					}
				}
				if (qName.equals("value")) {				
					currentPoi = new POIdescription();
					currentPoi.typeNum = poiIdx++;
					currentPoi.key = currentKey;
					currentPoi.value = atts.getValue("name");
					currentPoi.hideable = true;
					String rulePrio = atts.getValue("priority");
					if (rulePrio != null) {
						try {
							currentPoi.rulePriority = (byte)Integer.parseInt(rulePrio);
						} catch (NumberFormatException nfe) {
							System.out.println("WARNING: Rule priority is invalid, using default");
						}
					}
					Set<POIdescription> poiDescs = keyValuesPoi.get(currentPoi.value);
					if (poiDescs == null)
						poiDescs = new HashSet<POIdescription>();
					poiDescs.add(currentPoi);
					keyValuesPoi.put(currentPoi.value, poiDescs);					
					pois.put(currentPoi.typeNum, currentPoi);
				}
				if (qName.equals("specialisation")) {
					if (currentPoi.specialisation == null) {
						currentPoi.specialisation = new LinkedList<ConditionTuple>();
					}
					ConditionTuple ct = new ConditionTuple();
					ct.key = atts.getValue("key");
					ct.value = atts.getValue("value");
					String condition = atts.getValue("condition");
					if (condition.equalsIgnoreCase("exclude")) {
						ct.exclude = true;
					} else {
						ct.exclude = false;
					}
					currentPoi.specialisation.add(ct);
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
				if (qName.equals("hideable")) {
					currentPoi.hideable = atts.getValue("hideable").equalsIgnoreCase("true");
				}
				break;
			case READING_WAYS:
				if (qName.equals("keyW")) {
					currentKey = atts.getValue("tag");
					keyValuesWay = wayMap.get(currentKey);
					if (keyValuesWay == null) {
						keyValuesWay = new Hashtable<String,Set<WayDescription>>();
						wayMap.put(currentKey, keyValuesWay);
					}
				}
				if (qName.equals("Wvalue")) {
					currentWay = new WayDescription();
					currentWay.typeNum = wayIdx++;
					currentWay.key = currentKey;
					currentWay.value = atts.getValue("name");
					currentWay.hideable = true;
					Set<WayDescription> wayDescs = keyValuesWay.get(currentWay.value);
					if (wayDescs == null)
						wayDescs = new HashSet<WayDescription>();
					wayDescs.add(currentWay);
					keyValuesWay.put(currentWay.value, wayDescs);
					String rulePrio = atts.getValue("priority");
					if (rulePrio != null) {
						try {
							currentWay.rulePriority = (byte)Integer.parseInt(rulePrio);
						} catch (NumberFormatException nfe) {
							System.out.println("WARNING: Rule priority is invalid, using default");
						}
					}
					ways.put(currentWay.typeNum, currentWay);
				}
				if (qName.equals("specialisation")) {
					if (currentWay.specialisation == null) {
						currentWay.specialisation = new LinkedList<ConditionTuple>();
					}
					ConditionTuple ct = new ConditionTuple();
					ct.key = atts.getValue("key");
					ct.value = atts.getValue("value");
					String condition = atts.getValue("condition");
					if (condition.equalsIgnoreCase("exclude")) {
						ct.exclude = true;
					} else {
						ct.exclude = false;
					}
					currentWay.specialisation.add(ct);
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

				if (qName.equals("arrowscale")) {
					try {
						currentWay.minOnewayArrowScale = config.getRealScale( Integer.parseInt(atts.getValue("scale")) );				
					} catch (NumberFormatException nfe) {
						System.out.println("Error: oneway arrowscale for " +currentWay.description + " is incorrect");
					}
				}

				if (qName.equals("descriptionscale")) {
					try {
						currentWay.minDescriptionScale = config.getRealScale( Integer.parseInt(atts.getValue("scale")) );				
					} catch (NumberFormatException nfe) {
						System.out.println("Error: descriptionscale for " +currentWay.description + " is incorrect");
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
				if (qName.equals("hideable")) {
					currentWay.hideable = atts.getValue("hideable").equalsIgnoreCase("true");
				}
				break;
			case READING_SOUNDS:
				if (qName.equals("sound")) {				
					currentSound = new SoundDescription();
					currentSound.name = atts.getValue("name");
					sounds.addElement(currentSound);
				} else if (qName.equals("soundFile")) {
					if (currentSound!=null) {
						currentSound.soundFile  = atts.getValue("src");
					} else {
						System.out.println("soundFile without sound in style file");						
					}
				} else if (qName.equals("changeFileExtensionTo")) {
					Configuration.getConfiguration().changeSoundFileExtensionTo = atts.getValue("fileExt");
				}
				break;
		}
	} // startElement

	public void endElement(String namespaceURI, String localName, String qName) {		
//		if (qName.equals("pois")) {
//			readingPOIs = false;
//		}				
	} // endElement

	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println("Error: " + e);
		throw e;	
	}
	public Hashtable<String, Hashtable<String,Set<POIdescription>>> getPOIlegend() {
		return poiMap;
	}
	
	public POIdescription getPOIDesc(byte type) {
		return pois.get(type);
	}
	
	public Collection<POIdescription> getPOIDescs() {
		return pois.values();
	}
	
	public Hashtable<String, Hashtable<String,Set<WayDescription>>> getWayLegend() {
		return wayMap;
	}
	
	public WayDescription getWayDesc(byte type) {
		return ways.get(type);
	}
	
	public Collection<WayDescription> getWayDescs() {
		return ways.values();
	}
	public Vector<SoundDescription> getSoundDescs() {
		return sounds;
	}

	
	
	public void warning(SAXParseException e) throws SAXException {
        System.out.println("Warning: " + e.getMessage()); 
        
     }
     public void error(SAXParseException e) throws SAXException {
        System.out.println("Error on line " + e.getLineNumber()+ " (remember ordering matters): " + e.getMessage());
        nonValidStyleFile = true;
     }
}
