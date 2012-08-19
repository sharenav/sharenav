/**
 * This file is part of OSM2GpsMid
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 * Copyright (C) 2008  sk750
 */

package de.ueller.osmToGpsMid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.model.ConditionTuple;
import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.Damage;
import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.EntityDescription;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.RouteAccessRestriction;
import de.ueller.osmToGpsMid.model.TollRule;
import de.ueller.osmToGpsMid.model.TravelMode;
import de.ueller.osmToGpsMid.model.TravelModes;
import de.ueller.osmToGpsMid.model.WayDescription;

public class LegendParser extends DefaultHandler implements ErrorHandler {
	public static Configuration config;

	private Hashtable<String, Hashtable<String, Set<EntityDescription>>> poiMap;
	private Hashtable<String, Hashtable<String, Set<EntityDescription>>> wayMap;
	private LongTri<EntityDescription> pois;
	private LongTri<EntityDescription> ways;
	private POIdescription currentPoi;
	private TravelMode currentTravelMode;
	private WayDescription currentWay;
	private String currentKey;
	private Hashtable<String, Set<EntityDescription>> keyValuesPoi;
	private Hashtable<String, Set<EntityDescription>> keyValuesWay;
	private Hashtable<String, Integer> maxSpeedTemplates;
	private Hashtable<String, Boolean> relationExpansions;
	private Hashtable<String, Boolean> relationExpansionsCombine;
	private static final Vector<Damage> damages = new Vector<Damage>();
	private final byte READING_WAYS = 0;
	private final byte READING_POIS = 1;
	private final byte READING_ROUTEMODES = 3;
	private final byte READING_COLORS = 4;
	private final byte READING_MAXSPEED = 5;
	private final byte READING_TILESCALELEVELS = 6;
	private final byte READING_DAMAGES = 7;
	private byte readingType = READING_WAYS;

	private int poiIdx = 0;
	private int wayIdx = 0;
	private boolean nonValidStyleFile;
	public static int tileScaleLevel[] = { Integer.MAX_VALUE, 900000, 180000, 45000 };
	public static boolean tileScaleLevelIsAllowedForRoutableWays[] = {true, true, true, true};
	public static boolean tileScaleLevelContainsRoutableWays[] = {false, false, false, false};
	public static int tileLevelAttractsAreasWithSmallerBoundsDiameterThan[] = {0, 0, 0, 0};
	
	private static HashSet<String> relevantKeys;

	public class DTDresolver implements EntityResolver {

		/*
		 * (non-Javadoc)
		 * @see org.xml.sax.EntityResolver#resolveEntity(java.lang.String,
		 * java.lang.String)
		 */
		@Override
		public InputSource resolveEntity(String publicId, String systemId)
				throws SAXException, IOException {
			/* Use the style-file.dtd that is internl to the Osm2GpsMid jar */
			if (systemId.endsWith("style-file.dtd")) {
				InputStream is = this.getClass().getResourceAsStream("/style-file.dtd");
				if (is != null) {
					return new InputSource(is);
				} else {
					System.out.println("Warning: Could not read internal dtd file");
					return null;
				}
			}
			System.out.println("Trying to resolve XML entity " + systemId); 
			try {
				InputStream is = new URL(systemId).openStream();
				if (is != null) {
					System.out.println("Resolved entity externally");
					return new InputSource(is);
				}
			} catch (FileNotFoundException fnfe) {
				InputStream is = this.getClass().getResourceAsStream(systemId.substring(systemId.lastIndexOf('/')));
				if (is != null) {
					System.out.println("No such file, resolved entity internally instead");
					return new InputSource(is);
				}
			}
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
			relevantKeys = new HashSet<String>();
			initSpecialcasedRelevantKeys();
			poiMap = new Hashtable<String, Hashtable<String, Set<EntityDescription>>>();
			pois = new LongTri<EntityDescription>();
			currentPoi = new POIdescription();
			maxSpeedTemplates = new Hashtable<String, Integer>();
			relationExpansions = new Hashtable<String, Boolean>();
			relationExpansionsCombine = new Hashtable<String, Boolean>();
			/* Add a bogous POI description, to reserve type 0 as a special marker */
			// polish.api.bigstyles
			currentPoi.typeNum = (short) poiIdx++;
			currentPoi.key = "A key that should never be hot";
			currentPoi.value = "A value that should never be triggered";
			currentPoi.description = "No description";
			pois.put(currentPoi.typeNum, currentPoi);

			wayMap = new Hashtable<String, Hashtable<String, Set<EntityDescription>>>();
			ways = new LongTri<EntityDescription>();
			currentWay = new WayDescription();
			/**
			 * Add a bogous Way description, to reserve type 0 as a special
			 * marker
			 */
			// polish.api.bigstyles
			currentWay.typeNum = (short) wayIdx++;
			currentWay.key = "A key that should never be hot";
			currentWay.value = "A value that should never be triggered";
			currentWay.description = "No description";
			ways.put(currentWay.typeNum, currentWay);

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			// Parse the input
			nonValidStyleFile = false;
			XMLReader xmlReader = factory.newSAXParser().getXMLReader();
			xmlReader.setEntityResolver(new DTDresolver());
			xmlReader.setContentHandler(this);
			xmlReader.setErrorHandler(this);
			xmlReader.parse(new InputSource(i));
			if (nonValidStyleFile) {
				System.out.println("ERROR: your style file is not valid. Please correct the file and try Osm2GpsMid again");
				System.exit(1);
			}
			System.out.println("Style-file: You have " + (poiIdx - 1)
					+ " POI types defined and " + (wayIdx - 1) + " way types");
			if (poiIdx > 126) {
				// polish.api.bigstyles
				config.mediumStyles = true;
				config.map66search = true;
				if (poiIdx > 253) {
					config.bigStyles = true;
					System.out.println("WARNING: your style file contains > 253 POI types, you'll need a \"bigstyles\" GpsMid.");
				}
				//System.exit(1);
			}
			if (wayIdx > 126) {
				// polish.api.bigstyles
				config.mediumStyles = true;
				config.map66search = true;
				if (poiIdx > 253) {
					config.bigStyles = true;
					System.out.println("WARNING: your style file contains > 253 way types, you'll need a \"bigstyles\" GpsMid.");
				}
				//System.exit(1);
			}

			// check if all colors are defined in the style-file
			boolean colorsComplete = true;
			for (int n = 0; n < Configuration.COLOR_COUNT; n++) {
				if (Configuration.COLORS[n] == 0xFFFFFFFF) {
					colorsComplete = false;
					System.out.println("ERROR: your style-file contains no color for \""
							+ Configuration.COLORNAMES[n] + "\"");
				}
			}
			if (!colorsComplete) {
				System.exit(1);
			}

			// check if all routeModes specified with useRouting are also
			// defined in the style-file
			if (Configuration.attrToBoolean(config.useRouting) >= 0) {
				for (int n = 0; n < TravelModes.travelModeCount; n++) {
					if (!TravelModes.travelModes[n].routeModeDefined) {
						System.out.println("ERROR: useRouting="	+ config.useRouting
								+ " is specified in your .properties file but in your style-file there's no routeMode called "
								+ TravelModes.travelModes[n].getName());
						System.exit(1);
					}
				}
			}
			
			/**
			 * Calculate the relevant keys that we need to care about in the XML stream
			 */
			for (EntityDescription e : pois.values()) {
				relevantKeys.add(e.key);
				relevantKeys.add(e.nameKey);
				relevantKeys.add(e.nameFallbackKey);
				relevantKeys.add(e.helperTag);
				if (e.houseNumberMatchTag != null) {
					relevantKeys.add(e.houseNumberMatchTag);
				}
				if (e.specialisation != null) {
					for (ConditionTuple ct : e.specialisation) {
						relevantKeys.add(ct.key);
					}
				}
			}
			for (EntityDescription e : ways.values()) {
				relevantKeys.add(e.key);
				relevantKeys.add(e.nameKey);
				relevantKeys.add(e.nameFallbackKey);
				relevantKeys.add(e.helperTag);
				if (e.houseNumberMatchTag != null) {
					relevantKeys.add(e.houseNumberMatchTag);
				}
				if (e.specialisation != null) {
					for (ConditionTuple ct : e.specialisation) {
						if (!ct.properties) {
							relevantKeys.add(ct.key);
						}
					}
				}
			}
			for (TravelMode tm : TravelModes.travelModes) {
				if (tm == null) continue;
				if (tm.getRouteAccessRestrictions() != null) {
					for (RouteAccessRestriction rar : tm.getRouteAccessRestrictions()) {
						relevantKeys.add(rar.key);
					}
				}
				if (tm.getTollRules() != null) {
					for (TollRule tr : tm.getTollRules()) {
						relevantKeys.add(tr.key);
					}
				}
			}
			for (Damage d : damages) {
				relevantKeys.add(d.key);
			}
		} catch (FileNotFoundException fnfe) {
			System.out.println("ERROR, could not find necessary file: "
					+ fnfe.getMessage());
			System.exit(1);
		} catch (IOException e) {
			System.out.println("ERROR: IOException: " + e);
			System.exit(1);
		} catch (SAXException e) {
			System.out.println("ERROR: SAXException: " + e);
			System.exit(1);
		} catch (javax.xml.parsers.ParserConfigurationException e) {
			System.out.println("ERROR: ParserConfigurationException: " + e);
			System.exit(1);
		// } catch (Exception e) {
		// System.out.println("ERROR: Other Exception: " + e);
		// System.exit(1);
		}
	}
	
	private void initSpecialcasedRelevantKeys() {
		/* 
		 * Add all the keys that are hardcoded in Osm2GpsMid to be included in the relevantKeys set
		 */
		relevantKeys.add("type");
		relevantKeys.add("admin_level");
		relevantKeys.add("boundary");
		relevantKeys.add("natural");
		relevantKeys.add("highway");
		relevantKeys.add("is_in");
		relevantKeys.add("maxspeed");
		relevantKeys.add("maxspeed:seasonal:winter");
		relevantKeys.add("access");
		relevantKeys.add("toll");
		relevantKeys.add("junction");
		relevantKeys.add("cycleway");
		relevantKeys.add("oneway");
		relevantKeys.add("restriction");
		relevantKeys.add("bridge");
		relevantKeys.add("tunnel");
		relevantKeys.add("place");
		relevantKeys.add("area");
		relevantKeys.add("layer");
		if (config.useUrlTags) {
			for (String urlTag : Entity.urlTags) {
				relevantKeys.add(urlTag);
			}
		}
		if (config.usePhoneTags) {
			for (String phoneTag : Entity.phoneTags) {
				relevantKeys.add(phoneTag);
			}
		}
		relevantKeys.add("phone");
		// FIXME: switch this on a flag, if true, index all
		// nodes with addr:housenumber, regardless of whether there's a housenumberindex element
		if (config.useHouseNumbers) {
			relevantKeys.add("addr:interpolation");
			relevantKeys.add("addr:housenumber");
			relevantKeys.add("addr:street");
		}
		relevantKeys.add("barrier");
		//relevantKeys.add("direction");
		//relevantKeys.add("crossing");
		
	}

	@Override
	public void startDocument() {
	}

	@Override
	public void endDocument() {
	}

	@Override
	public void startElement(String namespaceURI, String localName, String qName,
			Attributes atts) {
		// System.out.println("start " + localName + " " + qName);
		if (qName.equals("pois")) {
			readingType = READING_POIS;
		} else if (qName.equals("ways")) {
			readingType = READING_WAYS;
		} else if (qName.equals("colors")) {
			readingType = READING_COLORS;
		} else if (qName.equals("routeModes")) {
			readingType = READING_ROUTEMODES;
		} else if (qName.equals("maxSpeedTemplates")) {
			readingType = READING_MAXSPEED;
		} else if (qName.equals("tileScaleLevels")) {
			readingType = READING_TILESCALELEVELS;
		} else if (qName.equals("damages")) {
			readingType = READING_DAMAGES;
		}
		// TODO: Suppose the method is called with qName="pois". I think the
		// 'case'
		// below with the checks against "key" and "value" and ... will be
		// called
		// (same for all the other readingTypes) but it can never match
		// anything!
		// Check if this is true, and if yes, the switch needs to go into an
		// else-clause!
		switch (readingType) {
		case READING_POIS:
			if (qName.equals("key")) {
				currentKey = atts.getValue("tag");
				keyValuesPoi = poiMap.get(currentKey);
				if (keyValuesPoi == null) {
					keyValuesPoi = new Hashtable<String, Set<EntityDescription>>();
					poiMap.put(currentKey, keyValuesPoi);
				}
			}
			if (qName.equals("value")) {
				currentPoi = new POIdescription();
				// polish.api.bigstyles
				currentPoi.typeNum = (short)poiIdx++;
				currentPoi.key = currentKey;
				currentPoi.value = atts.getValue("name");
				currentPoi.hideable = true;
				String rulePrio = atts.getValue("priority");
				if (rulePrio != null) {
					try {
						currentPoi.rulePriority = (byte) Integer.parseInt(rulePrio);
					} catch (NumberFormatException nfe) {
						System.out.println("WARNING: Rule priority is invalid, using default");
					}
				}
				Set<EntityDescription> poiDescs = keyValuesPoi.get(currentPoi.value);
				if (poiDescs == null) {
					poiDescs = new HashSet<EntityDescription>();
				}
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
				ct.regexp = "true".equalsIgnoreCase(atts.getValue("regexp"));
				ct.properties = "true".equalsIgnoreCase(atts.getValue("properties"));
				String condition = atts.getValue("condition");
				if (condition.equalsIgnoreCase("exclude")) {
					ct.exclude = true;
				} else {
					ct.exclude = false;
				}
				currentPoi.specialisation.add(ct);
			}
			if (qName.equals("AreaPOI")) {
				currentPoi.createPOIsForAreas = atts.getValue("createForAreas")
						.equalsIgnoreCase("true");
			}
			if (qName.equals("description")) {
				currentPoi.description = atts.getValue("desc");
			}
			if (qName.equals("namekey")) {
				currentPoi.nameKey = atts.getValue("tag");
			}
			if (qName.equals("helpertag")) {
				currentPoi.helperTag = atts.getValue("tag");
			}
			if (qName.equals("housenumberindex")) {
				currentPoi.houseNumberIndex = true;
				currentPoi.houseNumberMatchTag = atts.getValue("matchtag");
			}
			if (qName.equals("namefallback")) {
				currentPoi.nameFallbackKey = atts.getValue("tag");
			}
			if (qName.equals("scale")) {
				try {
					currentPoi.minEntityScale = config.getRealScale(Integer.parseInt(atts
							.getValue("scale")));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: scale for " + currentPoi.description
							+ " is incorrect");
				}
				if (currentPoi.minTextScale == 0) {
					currentPoi.minTextScale = currentPoi.minEntityScale;
				}
			}
			if (qName.equals("textscale")) {
				try {
					currentPoi.minTextScale = config.getRealScale(Integer.parseInt(atts
							.getValue("scale")));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: textscale for " + currentPoi.description
							+ " is incorrect");
				}
			}
			if (qName.equals("image")) {
				currentPoi.image = atts.getValue("src");
			}
			if (qName.equals("searchIcon")) {
				currentPoi.searchIcon = atts.getValue("src");
			}
			if (qName.equals("imageCentered")) {
				currentPoi.imageCenteredOnNode = atts.getValue("value").equalsIgnoreCase(
						"true");
			}
			if (qName.equals("hideable")) {
				currentPoi.hideable = atts.getValue("hideable").equalsIgnoreCase("true");
			}
			if (qName.equals("alert")) {
				currentPoi.alert = atts.getValue("alert").equalsIgnoreCase("true");
			}
			if (qName.equals("clickable")) {
				currentPoi.clickable = atts.getValue("clickable").equalsIgnoreCase("true");
			}
			break;
		case READING_WAYS:
			if (qName.equals("keyW")) {
				currentKey = atts.getValue("tag");
				keyValuesWay = wayMap.get(currentKey);
				if (keyValuesWay == null) {
					keyValuesWay = new Hashtable<String, Set<EntityDescription>>();
					wayMap.put(currentKey, keyValuesWay);
				}
			}
			if (qName.equals("Wvalue")) {
				currentWay = new WayDescription();
				// polish.api.bigstyles
				currentWay.typeNum = (short) wayIdx++;
				currentWay.key = currentKey;
				currentWay.value = atts.getValue("name");
				currentWay.hideable = true;
				if (currentKey.equalsIgnoreCase("building")) {
					currentWay.wayDescFlags |= WayDescription.WDFLAG_BUILDING;
				}
				if (currentKey.equalsIgnoreCase("highway")) {
					if (currentWay.value.toLowerCase().endsWith("_link")) {
						currentWay.wayDescFlags |= WayDescription.WDFLAG_HIGHWAY_LINK;
						// System.out.println("Waydescription is a highway link: " + currentWay.value);
					}
					String wayValue = ";" + currentWay.value.toLowerCase() + ";";
					if (";motorway;motorway_link;".indexOf(wayValue) >= 0) {
						currentWay.wayDescFlags |= WayDescription.WDFLAG_MOTORWAY;
						currentWay.wayDescTravelModes |= Connection.CONNTYPE_MOTORWAY;
					}
					if (";trunk;trunk_link;primary;primary_link;".indexOf(wayValue) >= 0) {
						currentWay.wayDescTravelModes |= Connection.CONNTYPE_TRUNK_OR_PRIMARY;
					}
					if (";motorway;motorway_link;trunk;trunk_link;primary;primary_link;secondary;secondary_link;tertiary;".indexOf(wayValue) >= 0) {
						currentWay.wayDescTravelModes |= Connection.CONNTYPE_MAINSTREET_NET;
						currentWay.wayDescFlags |= WayDescription.WDFLAG_MAINSTREET_NET;
					}
				}
				
				/* Assign a small default speed for the case that the way
				 * becomes accessible for routing by a RouteAccessRestriction
				 * but the way description itself in the style file contains no
				 * routing information.
				 */
				for (int i = 0; i < TravelModes.travelModeCount; i++) {
					currentWay.typicalSpeed[i] = 5;
				}
				Set<EntityDescription> wayDescs = keyValuesWay.get(currentWay.value);
				if (wayDescs == null) {
					wayDescs = new HashSet<EntityDescription>();
				}
				wayDescs.add(currentWay);
				keyValuesWay.put(currentWay.value, wayDescs);
				String rulePrio = atts.getValue("priority");
				if (rulePrio != null) {
					try {
						currentWay.rulePriority = (byte) Integer.parseInt(rulePrio);
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
				ct.regexp = "true".equalsIgnoreCase(atts.getValue("regexp"));
				ct.properties = "true".equalsIgnoreCase(atts.getValue("properties"));
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
			if (qName.equals("housenumberindex")) {
				currentWay.houseNumberIndex = true;
				currentWay.houseNumberMatchTag = atts.getValue("matchtag");
			}
			if (qName.equals("scale")) {
				try {
					currentWay.minEntityScale = config.getRealScale(Integer.parseInt(atts
							.getValue("scale")));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: scale for " + currentWay.description
							+ " is incorrect");
				}
				if (currentWay.minTextScale == 0) {
					currentWay.minTextScale = currentWay.minEntityScale;
				}
			}
			if (qName.equals("textscale")) {
				try {
					currentWay.minTextScale = config.getRealScale(Integer.parseInt(atts
							.getValue("scale")));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: textscale for " + currentWay.description
							+ " is incorrect");
				}
			}
			if (qName.equals("arrowscale")) {
				try {
					currentWay.minOnewayArrowScale = config.getRealScale(Integer
							.parseInt(atts.getValue("scale")));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: oneway arrowscale for "
							+ currentWay.description + " is incorrect");
				}
			}
			if (qName.equals("descriptionscale")) {
				try {
					currentWay.minDescriptionScale = config.getRealScale(Integer
							.parseInt(atts.getValue("scale")));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: descriptionscale for "
							+ currentWay.description + " is incorrect");
				}
			}
			if (qName.equals("image")) {
				currentWay.image = atts.getValue("src");
			}
			if (qName.equals("searchIcon")) {
				currentWay.searchIcon = atts.getValue("src");
			}
			if (qName.equals("showName")) {
				currentWay.showNameAsForArea = atts.getValue("style").equalsIgnoreCase("area");
			}
			if (qName.equals("isArea")) {
				currentWay.isArea = atts.getValue("area").equalsIgnoreCase("true");
			}
			if (qName.equals("asRelation")) {
				if ("true".equalsIgnoreCase(atts.getValue("relation"))) {
					if (currentWay.key != null && currentWay.value != null) {
						System.out.println("Shall expand type=" + currentWay.key + ","
								   + currentWay.key + "=" + currentWay.value
								   + " relations");
						relationExpansions.put(currentWay.key + "=" +  currentWay.value, true);
					}
				}
				if ("true".equalsIgnoreCase(atts.getValue("combined"))) {
					if (currentWay.key != null && currentWay.value != null) {
						System.out.println("Shall combine type=" + currentWay.key + ","
								   + currentWay.key + "=" + currentWay.value
								   + " relations");
						relationExpansionsCombine.put(currentWay.key + "=" +  currentWay.value, true);
					}
				}
			}
			if (qName.equals("ignoreOsmAreaTag")) {
				currentWay.ignoreOsmAreaTag = atts.getValue("ignore").equalsIgnoreCase("true");
			}
			if (qName.equals("lineColor")) {
				try {
					currentWay.lineColor = Integer.parseInt(atts.getValue("color"), 16);
				} catch (NumberFormatException nfe) {
					System.out.println("Error: lineColor for " + currentWay.description
							+ " is incorrect. Must be a hex coded ARGB value");
				}
				String nightColor = atts.getValue("colorAtNight");
				if (nightColor != null) {
					try {
						currentWay.lineColorAtNight = Integer.parseInt(nightColor, 16);
					} catch (NumberFormatException nfe) {
						System.out.println("Error: lineColor colorAtNight for "
								+ currentWay.description
								+ " is incorrect. Must be a hex coded ARGB value");
					}
				}
			}
			if (qName.equals("borderColor")) {
				try {
					currentWay.boardedColor = Integer
							.parseInt(atts.getValue("color"), 16);
				} catch (NumberFormatException nfe) {
					System.out.println("Error: borderColor for " + currentWay.description
							+ " is incorrect. Must be a hex coded ARGB value");
				}
				String nightColor = atts.getValue("colorAtNight");
				if (nightColor != null) {
					try {
						currentWay.boardedColorAtNight = Integer.parseInt(nightColor, 16);
					} catch (NumberFormatException nfe) {
						System.out.println("Error: borderColor colorAtNight for "
								+ currentWay.description
								+ " is incorrect. Must be a hex coded ARGB value");
					}
				}
			}
			if (qName.equals("wayWidth")) {
				try {
					currentWay.wayWidth = Integer.parseInt(atts.getValue("width"));
				} catch (NumberFormatException nfe) {
					System.out.println("Error: wayWidth for " + currentWay.description
							+ " is incorrect");
				}
			}
			if (qName.equals("lineStyle")) {
				if (isAttributeActivated(atts, "dashed")) {
					currentWay.wayDescFlags |= WayDescription.WDFLAG_LINESTYLE_DOTTED;
				}
				if (isAttributeActivated(atts, "rail")) {
					currentWay.wayDescFlags |= WayDescription.WDFLAG_LINESTYLE_RAIL;
				}
				if (isAttributeActivated(atts, "steps")) {
					currentWay.wayDescFlags |= WayDescription.WDFLAG_LINESTYLE_STEPS;
				}
				if (isAttributeActivated(atts, "powerLine")) {
					currentWay.wayDescFlags |= WayDescription.WDFLAG_LINESTYLE_POWERLINE;
				}
			}
			if (qName.equals("routing")) {
				// only use routing rules for the with-parameter specified in
				// .properties, e.g. useRouting=motorcar
				int travelModeNr = TravelModes.getTravelModeNrByName(atts.getValue("with"));
				if (travelModeNr >= 0) {
					if (atts.getValue("accessible").equalsIgnoreCase("true")) {
						currentWay.wayDescTravelModes |= 1 << travelModeNr;
					}
					String typicalSpeed = atts.getValue("speed");
					if (typicalSpeed != null) {
						try {
							Float speed = Float.parseFloat(typicalSpeed);
							currentWay.typicalSpeed[travelModeNr] = Float.parseFloat(typicalSpeed);
							//currentWay.typicalSpeed[travelModeNr] = Integer.parseInt(typicalSpeed);
						} catch (NumberFormatException nfe) {
							System.out.println("Invalid speed for "
									+ currentWay.description);
						}
					} else {
						System.out.println("Warning: no typical speed for "
								+ currentWay.description + ". Using 5 km/h.");
						currentWay.typicalSpeed[travelModeNr] = 5;
					}
					// System.out.println(currentWay.description + " with " +
					// atts.getValue("with") + ": " +
					// atts.getValue("accessible") + " typicalSpeed: " +
					// typicalSpeed + "km/h");
				}
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
			if (qName.equals("alert")) {
				currentWay.alert = atts.getValue("alert").equalsIgnoreCase("true");
			}
			if (qName.equals("clickable")) {
				currentWay.clickable = atts.getValue("clickable").equalsIgnoreCase("true");
			}
			break;
		case READING_COLORS:
			if (qName.equals("color")) {
				String colorName = atts.getValue("of");
				boolean colorFound = false;
				for (int i = 0; i < Configuration.COLORNAMES.length; i++) {
					if (Configuration.COLORNAMES[i].equalsIgnoreCase(colorName)) {
						int rgb = 0;
						try {
							rgb = Integer.parseInt(atts.getValue("is"), 16);
						} catch (NumberFormatException nfe) {
							System.out.println("Error: color of " + colorName
									+ " is invalid. Must be a hex coded ARGB value");
						}
						Configuration.COLORS[i] = rgb;
						String nightColor = atts.getValue("isAtNight");
						if (nightColor != null) {
							try {
								rgb = Integer.parseInt(nightColor, 16);
								Configuration.COLORS_AT_NIGHT[i] = rgb;
							} catch (NumberFormatException nfe) {
								System.out.println("Error: color isAtNight of "
										+ colorName
										+ " is invalid. Must be a hex coded ARGB value");
							}
						}
						colorFound = true;
						break;
					}
				}
				if (!colorFound) {
					System.out.println("Error: style file contains non-existent colorName: "
							+ colorName);
				}
				break;
			}
			break;
		case READING_ROUTEMODES:
			if (qName.equals("routeMode")) {
				currentTravelMode = TravelModes.getTravelMode(atts.getValue("modeName"));
				if (currentTravelMode != null) {
					currentTravelMode.routeModeDefined = true;
					String maxPrepareMeters = atts.getValue("maxPrepareMeters");
					if (maxPrepareMeters != null) {
						try {
							currentTravelMode.maxPrepareMeters = (short) Integer
									.parseInt(maxPrepareMeters);
						} catch (NumberFormatException nfe) {
							System.out.println("Invalid maxPrepareMeters for "
									+ currentTravelMode.getName());
						}
					} else {
						System.out.println("Warning: no maxPrepareMeters  for "
								+ currentTravelMode.getName() + ". Using 500m.");
						currentTravelMode.maxPrepareMeters = 500;
					}
					String maxInMeters = atts.getValue("maxInMeters");
					if (maxInMeters != null) {
						try {
							currentTravelMode.maxInMeters = (short) Integer.parseInt(maxInMeters);
						} catch (NumberFormatException nfe) {
							System.out.println("Invalid maxInMeters for "
									+ currentTravelMode.getName());
						}
					} else {
						System.out.println("Warning: no maxInMeters  for "
								+ currentTravelMode.getName() + ". Using 899m.");
						currentTravelMode.maxInMeters = 899;
					}
					String maxEstimationSpeed = atts.getValue("maxEstimationSpeed");
					if (maxEstimationSpeed != null) {
						try {
							currentTravelMode.maxEstimationSpeed = (short) Integer
									.parseInt(maxEstimationSpeed);
						} catch (NumberFormatException nfe) {
							System.out.println("Invalid maxEstimationSpeed for "
									+ currentTravelMode.getName());
						}
					} else {
						System.out.println("Warning: no maxEstimationSpeed  for "
								+ currentTravelMode.getName() + ". Using 150km/h.");
						currentTravelMode.maxEstimationSpeed = 150;
					}

					String applyTurnRestrictions = atts.getValue("applyTurnRestrictions");
					if (applyTurnRestrictions != null) {
						if (applyTurnRestrictions.equalsIgnoreCase("true")) {
							TravelModes.applyTurnRestrictionsTravelModes |= (1 << TravelModes
									.getTravelModeNrByName(atts.getValue("modeName")));
							currentTravelMode.travelModeFlags |= TravelMode.WITH_TURN_RESTRICTIONS;
						}
					}

					String mainStreetNet = atts.getValue("mainStreetNet");
					if (mainStreetNet != null) {
						if (mainStreetNet.equalsIgnoreCase("true")) {
							currentTravelMode.travelModeFlags |= TravelMode.MAINSTREET_NET_FOR_LARGE_ROUTES;
						}
					}

					String againstAllOneWays = atts.getValue("againstAllOneWays");
					if (againstAllOneWays != null) {
						if (againstAllOneWays.equalsIgnoreCase("true")) {
							currentTravelMode.travelModeFlags |= TravelMode.AGAINST_ALL_ONEWAYS;
						}
					}
					String bicycleOppositeExceptions = atts
							.getValue("bicycleOppositeExceptions");
					if (bicycleOppositeExceptions != null) {
						if (bicycleOppositeExceptions.equalsIgnoreCase("true")) {
							currentTravelMode.travelModeFlags |= TravelMode.BICYLE_OPPOSITE_EXCEPTIONS;
						}
					}
				}
			} // routeMode
			if (qName.equals("routeAccessRestriction")) {
				if (currentTravelMode != null) {
					currentTravelMode.getRouteAccessRestrictions().addElement(
							new RouteAccessRestriction(atts.getValue("restrictionKey"),
									atts.getValue("restrictionValues") + "|",
									Configuration.attrToBoolean(atts
											.getValue("restrictionPermit")) > 0));
				}
			}
			if (qName.equals("tollRule")) {
				if (currentTravelMode != null) {
					currentTravelMode.getTollRules().addElement(
							new TollRule(atts.getValue("tollKey"),
									atts.getValue("tollValues") + "|",
									Configuration.attrToBoolean(atts.getValue("enableToll")) > 0,
									Configuration.attrToBoolean(atts.getValue("debugTollRule")) > 0
							)
					);
				}
			}
			break;
		case READING_MAXSPEED:
			if (qName.equals("maxSpeedTemplate")) {
				try {
					maxSpeedTemplates.put(atts.getValue("name"), new Integer(Integer
							.parseInt(atts.getValue("maxspeed"))));
				} catch (NumberFormatException nfe) {
					System.out.println("Maxspeed in template " + atts.getValue("name")
							+ " must be integer, but was " + atts.getValue("maxspeed"));
					nonValidStyleFile = true;
				}
			}
			break;
		case READING_TILESCALELEVELS:
			if (qName.equals("tileScaleLevel")) {
				int level = -1;
				try {
					level = Integer.parseInt(atts.getValue("level"));
				} catch (NumberFormatException nfe) {
					System.out
							.println("level in tileScaleLevel must be integer, but was "
									+ atts.getValue("level"));
					nonValidStyleFile = true;
				}
				if (level >= 0 && level < 4) {
					int scale = 0;
					try {
						scale = Integer.parseInt(atts.getValue("minScale"));
						scale = (config.getRealScale(scale) + config
								.getRealScale(scale + 1) / 2);
						tileScaleLevel[level] = scale;
						System.out.println("tileScaleLevel " + level + ": " + scale);
					} catch (NumberFormatException nfe) {
						System.out
								.println("scale in tileScaleLevel must be integer, but was "
										+ atts.getValue("scale"));
						nonValidStyleFile = true;
					}
					String allowedForRoutableWays = atts.getValue("allowedForRoutableWays");
					if (allowedForRoutableWays != null) {
						tileScaleLevelIsAllowedForRoutableWays[level] = allowedForRoutableWays.equalsIgnoreCase("true");
					}
					String s = atts.getValue("attractAreasWithSmallerBoundsDiameterThan");
					if (s != null) {
						tileLevelAttractsAreasWithSmallerBoundsDiameterThan[level] = Integer.parseInt(atts.getValue("attractAreasWithSmallerBoundsDiameterThan"));
					}
					
				}
			}
			break;  // tileScaleLevels
		case READING_DAMAGES:
			if (qName.equals("damage")) {
				damages.add(new Damage(atts.getValue("damageKey"), atts.getValue("damageValues")));
			}
			break; // damages
		}
	} // startElement

	/**
	 * @param string
	 * @return
	 */
	private boolean isAttributeActivated(Attributes atts, String string) {
		String value = atts.getValue(string);
		if (value != null) {
			return (value.equalsIgnoreCase("true"));
		}
		return false;
	}

	@Override
	public void endElement(String namespaceURI, String localName, String qName) {
	}

	@Override
	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println("Error: " + e);
		throw e;
	}

	public Hashtable<String, Hashtable<String, Set<EntityDescription>>> getPOIlegend() {
		return poiMap;
	}

	// polish.api.bigstyles
	public POIdescription getPOIDesc(short type) {
		return (POIdescription) pois.get(type);
	}

	public Collection<EntityDescription> getPOIDescs() {
		return pois.values();
	}

	public Hashtable<String, Hashtable<String, Set<EntityDescription>>> getWayLegend() {
		return wayMap;
	}

	// polish.api.bigstyles
	public WayDescription getWayDesc(short type) {
		return (WayDescription) ways.get(type);
	}

	public Collection<EntityDescription> getWayDescs() {
		return ways.values();
	}

	public Hashtable<String, Integer> getMaxspeedTemplates() {
		return maxSpeedTemplates;
	}

	public Hashtable<String, Boolean> getRelationExpansions() {
		return relationExpansions;
	}

	public Hashtable<String, Boolean> getRelationExpansionsCombine() {
		return relationExpansionsCombine;
	}

	public static Vector<Damage> getDamages() {
		return damages;
	}
	
	@Override
	public void warning(SAXParseException e) throws SAXException {
		System.out.println("Warning on line " + e.getLineNumber() + ": " + e.getMessage());
	}

	@Override
	public void error(SAXParseException e) throws SAXException {
		System.out.println("Error on line " + e.getLineNumber()
				+ " (remember ordering matters): " + e.getMessage());
		nonValidStyleFile = true;
	}
	
	public static Set<String> getRelevantKeys() {
		return relevantKeys;
	}
}
