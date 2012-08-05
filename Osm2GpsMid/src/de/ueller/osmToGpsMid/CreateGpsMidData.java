/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007        Harald Mueller
 * Copyright (C) 2007, 2008  Kai Krueger
 * Copyright (C) 2008        sk750
 * 
 */

package de.ueller.osmToGpsMid;

import static de.ueller.osmToGpsMid.GetText._;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;

import de.ueller.osmToGpsMid.area.Triangle;
import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.ConditionTuple;
import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.EntityDescription;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.WayDescription;
import de.ueller.osmToGpsMid.model.Path;
import de.ueller.osmToGpsMid.model.RouteNode;
import de.ueller.osmToGpsMid.model.Sequence;
import de.ueller.osmToGpsMid.model.Tile;
import de.ueller.osmToGpsMid.model.TravelModes;
import de.ueller.osmToGpsMid.model.Way;
import de.ueller.osmToGpsMid.model.name.Names;
import de.ueller.osmToGpsMid.model.name.WayRedirect;
import de.ueller.osmToGpsMid.model.url.Urls;
import de.ueller.osmToGpsMid.tools.FileTools;



public class CreateGpsMidData implements FilenameFilter {
	
	/**
	 * This class is used in order to store a tuple on a dedicated stack.
	 * So that it is not necessary to use the OS stack in recursion.
	 */
	class TileTuple {
		public Tile t;
		public Bounds bound;
		TileTuple(Tile t, Bounds b) {
			this.t = t;
			this.bound = b;
		}
	}
	
	public final static byte LEGEND_FLAG_IMAGE = 0x01;
	public final static byte LEGEND_FLAG_SEARCH_IMAGE = 0x02;
	public final static byte LEGEND_FLAG_MIN_IMAGE_SCALE = 0x04;
	public final static byte LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE = LEGEND_FLAG_MIN_IMAGE_SCALE;
	public final static byte LEGEND_FLAG_TEXT_COLOR = 0x08;
	public final static byte LEGEND_FLAG_NON_HIDEABLE = 0x10;
	// public final static byte LEGEND_FLAG_NON_ROUTABLE = 0x20; routable flag has been  moved to Way
	public final static byte LEGEND_FLAG_ALERT = 0x20;
	public final static byte LEGEND_FLAG_MIN_DESCRIPTION_SCALE = 0x40;
	public final static int	 LEGEND_FLAG_ADDITIONALFLAG = 0x80;

	public final static byte LEGEND_FLAG2_CLICKABLE = 0x01;

	public final static byte ROUTE_FLAG_MOTORWAY = 0x01;
	public final static byte ROUTE_FLAG_MOTORWAY_LINK = 0x02;
	public final static byte ROUTE_FLAG_ROUNDABOUT = 0x04;
		
	// public  final static int MAX_DICT_DEEP = 5; replaced by Configuration.maxDictDepth
	public  final static int ROUTEZOOMLEVEL = 4;
	
	/** The parser which parses the OSM data. The nodes, ways and relations are 
	 * retrieved from it for further processing. */
	OsmParser parser;
	
	/** This array contains one tile for each zoom level or level of detail and
	 * the route tile. Each one is actually a tree of tiles because container tiles
	 * contain two child tiles. */
	Tile tile[] = new Tile[ROUTEZOOMLEVEL + 1];

	/** Output length of the route connection for statistics */
	long outputLengthConns = 0;
	
	private final String path;
	Names names1;
	Urls urls1;
	StringBuffer sbCopiedMedias = new StringBuffer();
	short mediaInclusionErrors = 0;
	
	private final static int INODE = 1;
	private final static int SEGNODE = 2;
//	private Bounds[] bounds = null;
	private Configuration configuration;
	private int totalWaysWritten = 0;
	private int totalSegsWritten = 0;
	private int totalNodesWritten = 0;
	private int totalPOIsWritten = 0;
	private static int dictFilesWritten = 0;
	private static int tileFilesWritten = 0;
	private RouteData rd;
	private static double MAX_RAD_RANGE = (Short.MAX_VALUE - Short.MIN_VALUE - 2000) / MyMath.FIXPT_MULT;
	private String[] useLang = null;

	WayRedirect wayRedirect = null;
		

	public CreateGpsMidData(OsmParser parser, String path) {
		super();
		this.parser = parser;
		if (Configuration.getConfiguration().sourceIsApk) {
			path = path + "/assets";
		}
		this.path = path;
		File dir = new File(path);
		wayRedirect = new WayRedirect();
		// first of all, delete all data-files from a previous run or files that comes
		// from the mid jar file
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (File f : files) {
				if (f.getName().endsWith(".d") || f.getName().endsWith(".dat")) {
					if (! f.delete()) {
						System.out.println("ERROR: Failed to delete file " + f.getName());
					}
				}
			}
		}
	}
	
	/** Prepares and writes the complete map data.
	 */
	public void exportMapToMid() {
		names1 = getNames1();
		urls1 = getUrls1();
		exportLegend(path);
		SearchList sl = new SearchList(names1, urls1, wayRedirect);
		UrlList ul = new UrlList(urls1);
		sl.createNameList(path);
		ul.createUrlList(path);
		for (int i = 0; i <= 3; i++) {
			System.out.println("Exporting tiles for zoomlevel " + i );
			System.out.println("===============================");
			if (!LegendParser.tileScaleLevelContainsRoutableWays[i]) {
				System.out.println("Info: This tile level contains no routable ways");
			}
			long startTime = System.currentTimeMillis();
			long bytesWritten = exportMapToMid(i);
			long time = (System.currentTimeMillis() - startTime);
			System.out.println("  Zoomlevel " + i + ": " + 
					Configuration.memoryWithUnit(bytesWritten) + " in " + 
					tileFilesWritten  + " files indexed by " + dictFilesWritten + 
					" dictionary files");
			System.out.println("  Time taken: " + time / 1000 + " seconds");
		}
		if (Configuration.attrToBoolean(configuration.useRouting) >= 0) {
			System.out.println("Exporting route tiles");
			System.out.println("=====================");
			long startTime = System.currentTimeMillis();
			long bytesWritten = exportMapToMid(ROUTEZOOMLEVEL);
			long time = (System.currentTimeMillis() - startTime);
			System.out.println("  " + Configuration.memoryWithUnit(bytesWritten) + 
				" for nodes in " + tileFilesWritten + " files, " +
				Configuration.memoryWithUnit(outputLengthConns) + " for connections in " + 
				tileFilesWritten + " files");
			System.out.println("    The route tiles have been indexed by " + 
					dictFilesWritten + " dictionary files");
			System.out.println("  Time taken: " + time / 1000 + " seconds");
		} else {
			System.out.println("No route tiles to export");
		}
//		for (int x = 1; x < 12; x++) {
//			System.out.print("\n" + x + " :");
//			tile[ROUTEZOOMLEVEL].printHiLo(1, x);
//		}
//		System.exit(2);
		// create search list for whole items
		//sl.createSearchList(path, SearchList.INDEX_NAME);
		// create search list for names, including data for housenumber matching, primary since map version 66
		sl.createSearchList(path, SearchList.INDEX_BIGNAME);
		// create search list for words
		if (Configuration.getConfiguration().useWordSearch) {
			sl.createSearchList(path, SearchList.INDEX_WORD);
			// create search list for whole words / house numbers
			sl.createSearchList(path, SearchList.INDEX_WHOLEWORD);
		}
		if (Configuration.getConfiguration().useHouseNumbers) {
			sl.createSearchList(path, SearchList.INDEX_HOUSENUMBER);
		}

		// Output statistics for travel modes
		if (Configuration.attrToBoolean(configuration.useRouting) >= 0) {
			for (int i = 0; i < TravelModes.travelModeCount; i++) {
				System.out.println(TravelModes.getTravelMode(i).toString());			
			}
		}		
		System.out.println("  MainStreet_Net Connections: " + 
				TravelModes.numMotorwayConnections + " motorway  " + 
				TravelModes.numTrunkOrPrimaryConnections + " trunk/primary  " +
				TravelModes.numMainStreetNetConnections + " total");			
		System.out.print("  Connections with toll flag:");
		for (int i = 0; i < TravelModes.travelModeCount; i++) {
			System.out.print(" " + TravelModes.getTravelMode(i).getName() + "(" + TravelModes.getTravelMode(i).numTollRoadConnections + ")" );
		}
		System.out.println("");
		System.out.println("Total ways: "+ totalWaysWritten 
				         + ", segments: " + totalSegsWritten
				         + ", nodes: " + totalNodesWritten
				         + ", POI: " + totalPOIsWritten);
	}
	
	private Names getNames1() {
		Names na = new Names();
		for (Way w : parser.getWays()) {
			na.addName(w, wayRedirect);		
		}
		for (Node n : parser.getNodes()) {
			na.addName(n, wayRedirect);
		}
		System.out.println("Found " + na.getNames().size() + " names, " + 
				na.getCanons().size() + " canon");
		na.calcNameIndex();
		return (na);
	}
	
	private Urls getUrls1() {
		Urls na = new Urls();
		for (Way w : parser.getWays()) {
			na.addUrl(w);		
			na.addPhone(w);		
		}
		for (Node n : parser.getNodes()) {
			na.addUrl(n);
			na.addPhone(n);
		}
		System.out.println("found " + na.getUrls().size() + " urls, including phones ");
		na.calcUrlIndex();
		return (na);
	}
	
	private void exportLegend(String path) {
		FileOutputStream foi;
		String outputMedia;
		try {
			FileTools.createPath(new File(path + "/dat"));
			foi = new FileOutputStream(path + "/legend.dat");
			DataOutputStream dsi = new DataOutputStream(foi);
			dsi.writeShort(Configuration.MAP_FORMAT_VERSION);
			Configuration config = Configuration.getConfiguration();
			/**
			 * Write application version
			 */
			dsi.writeUTF(config.getVersion());
			/**
			 * Write bundle date
			 */
			dsi.writeUTF(config.getBundleDate());
			/**
			 * Note if additional information is included that can enable editing of OSM data
			 */
			dsi.writeBoolean(config.enableEditingSupport);
			/* Note what languages are enabled
			 */
			useLang = configuration.getUseLang().split("[;,]", 200);
			String useLangName[] = configuration.getUseLangName().split("[;,]", 200);
			if (useLangName.length != useLang.length) {
				System.out.println("");
				System.out.println("  Warning: useLang count " + useLang.length + 
						" different than useLangName count " + useLangName.length + 
						" - ignoring useLangNames");
				System.out.println("");
				useLangName = useLang;
			}

			// make all available languages the same for now
			for (int i = 1; i <= 5 ; i++) {
				dsi.writeShort(useLang.length);
				for (int j = 0 ; j < useLang.length ; j++) {
					dsi.writeUTF(useLang[j]);
					dsi.writeUTF(useLangName[j]);
				}
			}
			// remove unneeded .loc files
			if (!Configuration.getConfiguration().allLang) {
				String langs = configuration.getUseLang() + ",en";
				removeFilesWithExt(path, "loc", langs.split("[;,]", 200));
			}

			// remove class files (midlet code) if building just the map
			if (!configuration.getMapName().equals("")) {
				removeFilesWithExt(path, "class", null);
			}

 			/**
			 * Note if urls and phones are in the midlet
			 */
			dsi.writeBoolean(config.useUrlTags);
			dsi.writeBoolean(config.usePhoneTags);
			/**
			 * Writing colors 
			 */
			dsi.writeShort((short) Configuration.COLOR_COUNT);
			for (int i = 0; i < Configuration.COLOR_COUNT; i++) {
				if (Configuration.COLORS_AT_NIGHT[i] != -1) {
					dsi.writeInt(0x01000000 | Configuration.COLORS[i]);
					dsi.writeInt(Configuration.COLORS_AT_NIGHT[i]);
				} else {
					dsi.writeInt(Configuration.COLORS[i]);
				}
			}
			/**
			 * Write Tile Scale Levels
			 */
			for (int i = 0; i < 4; i++) {
				if (LegendParser.tileScaleLevelContainsRoutableWays[i]) {
					dsi.writeInt(LegendParser.tileScaleLevel[i]);					
				} else {
					dsi.writeInt( -LegendParser.tileScaleLevel[i] );
				}
			}
			/**
			 * Write Travel Modes
			 */
			dsi.writeByte(TravelModes.travelModeCount);
			for (int i = 0; i < TravelModes.travelModeCount; i++) {
				dsi.writeUTF(_(TravelModes.getTravelMode(i).getName()));
				dsi.writeShort(TravelModes.getTravelMode(i).maxPrepareMeters);
				dsi.writeShort(TravelModes.getTravelMode(i).maxInMeters);
				dsi.writeShort(TravelModes.getTravelMode(i).maxEstimationSpeed);
				dsi.writeByte(TravelModes.getTravelMode(i).travelModeFlags);
			}
			
			/**
			 * Writing POI legend data
			 */
			/**
			 * // polish.api.bigstyles * Are there more way or poi styles than 126
			 */
         		//System.err.println("Big styles:" + config.bigStyles);
			// polish.api.bigstyles
			// backwards compatibility - use "0" as a marker that we use a short for # of styles
			if (config.bigStyles) {
				dsi.writeByte((byte) 0);
				dsi.writeShort(config.getPOIDescs().size());
			} else {
				dsi.writeByte(config.getPOIDescs().size());
			}
			for (EntityDescription entity : config.getPOIDescs()) {
				POIdescription poi = (POIdescription) entity; 
				byte flags = 0;
				byte flags2 = 0;
				if (poi.image != null && !poi.image.equals("")) {
					flags |= LEGEND_FLAG_IMAGE;
				}
				if (poi.searchIcon != null) {
					flags |= LEGEND_FLAG_SEARCH_IMAGE;
				}
				if (poi.minEntityScale != poi.minTextScale) {
					flags |= LEGEND_FLAG_MIN_IMAGE_SCALE;
				}
				if (poi.textColor != 0) {
					flags |= LEGEND_FLAG_TEXT_COLOR;
				}				
				if (!poi.hideable) {
					flags |= LEGEND_FLAG_NON_HIDEABLE;
				}
				if (poi.alert) {
					flags |= LEGEND_FLAG_ALERT;
				}
				if (poi.clickable) {
					flags2 |= LEGEND_FLAG2_CLICKABLE;
				}				
				// polish.api.bigstyles
				if (config.bigStyles) {
					dsi.writeShort(poi.typeNum);
					//System.out.println("poi typenum: " + poi.typeNum);
				} else {
					dsi.writeByte(poi.typeNum);
				}
				if (flags2 != 0) {
					flags |= LEGEND_FLAG_ADDITIONALFLAG;
				}
				dsi.writeByte(flags);
				if (flags2 != 0) {
					dsi.writeByte(flags2);
				}
				dsi.writeUTF(_(poi.description));
				dsi.writeBoolean(poi.imageCenteredOnNode);
				dsi.writeInt(poi.minEntityScale);
				if ((flags & LEGEND_FLAG_IMAGE) > 0) {
					outputMedia = copyMediaToMid(poi.image, path, "png");
					dsi.writeUTF(outputMedia);
				}
				if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {
					outputMedia = copyMediaToMid(poi.searchIcon, path, "png");
					dsi.writeUTF(outputMedia);
				}
				if ((flags & LEGEND_FLAG_MIN_IMAGE_SCALE) > 0) {
					dsi.writeInt(poi.minTextScale);
				}
				if ((flags & LEGEND_FLAG_TEXT_COLOR) > 0) {
					dsi.writeInt(poi.textColor);
				}
				if (config.enableEditingSupport) {
					int noKVpairs = 1;
					if (poi.specialisation != null) {
						for (ConditionTuple ct : poi.specialisation) {
							if (!ct.exclude) {
								noKVpairs++;
							}
						}
					}
					dsi.writeShort(noKVpairs);
					dsi.writeUTF(poi.key);
					dsi.writeUTF(poi.value);
					if (poi.specialisation != null) {
						for (ConditionTuple ct : poi.specialisation) {
							if (!ct.exclude) {
								dsi.writeUTF(ct.key);
								dsi.writeUTF(ct.value);
							}
						}
					}
					
				}
				// System.out.println(poi);
	
			}
			/**
			 * Writing Way legend data 
			 */
			// polish.api.bigstyles
			if (config.bigStyles) {
				System.out.println("waydesc size: " + Configuration.getConfiguration().getWayDescs().size());

				// backwards compatibility - use "0" as a marker that we use a short for # of styles
				dsi.writeByte((byte) 0);
				dsi.writeShort(Configuration.getConfiguration().getWayDescs().size());
			} else {
				dsi.writeByte(Configuration.getConfiguration().getWayDescs().size());
			}
			for (EntityDescription entity : Configuration.getConfiguration().getWayDescs()) {
				WayDescription way = (WayDescription) entity;
				byte flags = 0;
				byte flags2 = 0;
				if (!way.hideable) {
					flags |= LEGEND_FLAG_NON_HIDEABLE;
				}				
				if (way.alert) {
					flags |= LEGEND_FLAG_ALERT;
				}				
				if (way.clickable) {
					flags2 |= LEGEND_FLAG2_CLICKABLE;
				}				
				if (way.image != null && !way.image.equals("")) {
					flags |= LEGEND_FLAG_IMAGE;
				}
				if (way.searchIcon != null) {
					flags |= LEGEND_FLAG_SEARCH_IMAGE;
				}
				if (way.minOnewayArrowScale != 0) {
					flags |= LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE;
				}
				if (way.minDescriptionScale != 0) {
					flags |= LEGEND_FLAG_MIN_DESCRIPTION_SCALE;
				}
				// polish.api.bigstyles
				if (config.bigStyles) {
					dsi.writeShort(way.typeNum);
				} else {
					dsi.writeByte(way.typeNum);
				}
				if (flags2 != 0) {
					flags |= LEGEND_FLAG_ADDITIONALFLAG;
				}
				dsi.writeByte(flags);
				if (flags2 != 0) {
					dsi.writeByte(flags2);
				}
				byte routeFlags = 0;
				if (way.value.equalsIgnoreCase("motorway")) {
					routeFlags |= ROUTE_FLAG_MOTORWAY;
				}
				if (way.value.equalsIgnoreCase("motorway_link")) {
					routeFlags |= ROUTE_FLAG_MOTORWAY_LINK;
				}					
				dsi.writeByte(routeFlags);
				dsi.writeUTF(_(way.description));
				dsi.writeInt(way.minEntityScale);
				dsi.writeInt(way.minTextScale);				
				if ((flags & LEGEND_FLAG_IMAGE) > 0) {
					outputMedia = copyMediaToMid(way.image, path, "png");
					dsi.writeUTF(outputMedia);
				}
				if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {
					outputMedia = copyMediaToMid(way.searchIcon, path, "png");
					dsi.writeUTF(outputMedia);
				}
				dsi.writeBoolean(way.isArea);
				if (way.lineColorAtNight != -1) {
					dsi.writeInt(0x01000000 | way.lineColor);
					dsi.writeInt(way.lineColorAtNight);
				} else {
					dsi.writeInt(way.lineColor);
				}
				if (way.boardedColorAtNight != -1) {
					dsi.writeInt(0x01000000 | way.boardedColor);
					dsi.writeInt(way.boardedColorAtNight);
				} else {
					dsi.writeInt(way.boardedColor);					
				}
				dsi.writeByte(way.wayWidth);
				dsi.writeInt(way.wayDescFlags);
				if ((flags & LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE) > 0) {
					dsi.writeInt(way.minOnewayArrowScale);
				}
				if ((flags & LEGEND_FLAG_MIN_DESCRIPTION_SCALE) > 0) {
					dsi.writeInt(way.minDescriptionScale);
				}
				if (config.enableEditingSupport) {
					int noKVpairs = 1;
					if (way.specialisation != null) {
						for (ConditionTuple ct : way.specialisation) {
							if (!ct.exclude) {
								noKVpairs++;
							}
						}
					}
					dsi.writeShort(noKVpairs);
					dsi.writeUTF(way.key);
					dsi.writeUTF(way.value);
					if (way.specialisation != null) {
						for (ConditionTuple ct : way.specialisation) {
							if (!ct.exclude) {
								dsi.writeUTF(ct.key);
								dsi.writeUTF(ct.value);
							}
						}
					}
					
				}
				// System.out.println(way);
			}


			if (Configuration.attrToBoolean(configuration.useIcons) < 0) {
				System.out.println("Icons disabled - removing icon files from midlet.");
				removeUnusedIconSizes(path, true);
			} else {				
				// show summary for copied icon files
				System.out.println("Icon inclusion summary:");
				System.out.println("  " + FileTools.copyDir("icon", path, true, true) + 
						" internal icons replaced from " + "icon" + 
						System.getProperty("file.separator") + " containing " + 
						FileTools.countFiles("icon") + " files");

				// if useIcons == small or useIcons == big rename the corresponding icons to normal icons
				if (Configuration.attrToBoolean(configuration.useIcons) == 0) {
					renameAlternativeIconSizeToUsedIconSize(configuration.useIcons + "_");
				}
				removeUnusedIconSizes(path, false);
			}
						
			/**
			 * Copy sounds for all sound formats to midlet 
			 */
			String soundFormat[] = configuration.getUseSounds().split("[;,]", 10);
			// write sound format infos 
			dsi.write((byte) soundFormat.length);
	    	for (int i = 0; i < soundFormat.length; i++) {
		    	dsi.writeUTF(soundFormat[i].trim());
	    	}
			
	    	/**
	    	 * write all sound files in each sound directory for all sound formats
	    	 */
	    	String soundFileDirectoriesHelp[] = configuration.getSoundFiles().split("[;,]", 10);
		String soundDirsFound = "";
	    	for (int i = 0; i < soundFileDirectoriesHelp.length; i++) {
			// test existence of dir
			InputStream is = null;	
			try {			
				is = new FileInputStream(configuration.getStyleFileDirectory() + soundFileDirectoriesHelp[i].trim() + "/syntax.cfg");
			} catch (Exception e) {
				// try internal syntax.cfg
				try {			
					is = getClass().getResourceAsStream("/media/" + soundFileDirectoriesHelp[i].trim() + "/syntax.cfg");
				} catch (Exception e2) {
					;
				}
			}			

			if (is != null) {
				if (soundDirsFound.equals("")) {
					soundDirsFound = soundFileDirectoriesHelp[i];
				} else {
					soundDirsFound = soundDirsFound + ";" + soundFileDirectoriesHelp[i];
				}
			} else {
				System.out.println ("ERROR: syntax.cfg not found in the " + soundFileDirectoriesHelp[i].trim() + " directory");
			}
		}
	    	String soundFileDirectories[] = soundDirsFound.split("[;,]", 10);
		dsi.write((byte) soundFileDirectories.length);
	    	for (int i = 0; i < soundFileDirectories.length; i++) {
	    		String destSoundPath = path + "/" + soundFileDirectories[i].trim();
	    		// System.out.println("create sound directory: " + destSoundPath);
	    		FileTools.createPath(new File(destSoundPath));
	    		dsi.writeUTF(soundFileDirectories[i].trim());
	    		
	    		// create soundSyntax for current sound directory
				RouteSoundSyntax soundSyn = new RouteSoundSyntax(configuration.getStyleFileDirectory(), 
					 soundFileDirectories[i].trim(), destSoundPath + "/syntax.dat");			
	    		
		    	String soundFile;
				Object soundNames[] = soundSyn.getSoundNames(); 
				for (int j = 0; j < soundNames.length ; j++) {
					soundFile = (String) soundNames[j];
					soundFile = soundFile.toLowerCase();
			    	for (int k = 0; k < soundFormat.length; k++) {
						outputMedia = copyMediaToMid(soundFile + "." + 
							soundFormat[k].trim(), destSoundPath, soundFileDirectories[i].trim());					
					}
				}
				removeUnusedSoundFormats(destSoundPath);
	    	}

			
			// show summary for copied media files
			try {
				if (sbCopiedMedias.length() != 0) {
					System.out.println("External media inclusion summary:");
					sbCopiedMedias.append("\r\n");
				} else {				
					System.out.println("No external media included.");
				}
				sbCopiedMedias.append("  Media Sources for external medias\r\n");
				sbCopiedMedias.append("  referenced in " + configuration.getStyleFileName() + " have been:\r\n");
				sbCopiedMedias.append("    " + 
						(configuration.getStyleFileDirectory().length() == 0 ? 
								"Current directory" : configuration.getStyleFileDirectory()) + 
						" and its png and " + configuration.getSoundFiles() + " subdirectories");
				System.out.println(sbCopiedMedias.toString());
				if (mediaInclusionErrors != 0) {
					System.out.println("");
					System.out.println("  WARNING: " + mediaInclusionErrors + 
							" media files could NOT be included - see details above");
					System.out.println("");
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			dsi.writeFloat((float)Configuration.mapPrecisionInMeters);
			dsi.close();
			foi.close();
			
			if (Configuration.attrToBoolean(configuration.useRouting) < 0) {
				System.out.println("Routing disabled - removing routing sound files from midlet:");
				for (int i = 0; i < Configuration.SOUNDNAMES.length; i++) {
					if (";CONNECT;DISCONNECT;DEST_REACHED;SPEED_LIMIT;".indexOf(";" + Configuration.SOUNDNAMES[i] + ";") == -1) {
						removeSoundFile(Configuration.SOUNDNAMES[i]);
					}
				}
			}
		} catch (FileNotFoundException fnfe) {
			System.err.println("Unhandled FileNotFoundException: " + fnfe.getMessage());
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("Unhandled IOException: " + ioe.getMessage());
			ioe.printStackTrace();
		}
	} 

	public void renameAlternativeIconSizeToUsedIconSize(String prefixToRename) {
    	File dir = new File(path);
		File[] iconsToRename = dir.listFiles(this);
    	
    	if (iconsToRename != null) {
    		for (File file : iconsToRename) {
    			if (file.getName().startsWith(prefixToRename)) {
    				File fileRenamed = new File(path + "/" + file.getName().substring(prefixToRename.length()));
    				if (fileRenamed.exists()) {
    					fileRenamed.delete();
    				}
    				file.renameTo(fileRenamed);
//    				System.out.println("Rename " + file.getName() + " to " + fileRenamed.getName());
    			}
    		}
    	}		
	}
	
    public void removeUnusedIconSizes(String path, boolean deleteAllIcons) {
    	File dir = new File(path);
    	File[] iconsToDelete = dir.listFiles(this);
    	
    	if (iconsToDelete != null) {
    		for (File file : iconsToDelete) {
    			if (file.getName().matches("(small|big|large|huge)_(is?|r)_.*\\.png")
    				|| (deleteAllIcons && file.getName().matches("(is?|r)_.*\\.png") && !file.getName().equalsIgnoreCase("i_bg.png")) 
    			) {
    				//System.out.println("Delete " + file.getName());
    				file.delete();
    			}
    		}
    	}
    }
    
	public boolean accept(File directory, String filename) {
		return filename.endsWith(".png") || filename.endsWith(".amr") || filename.endsWith(".mp3") || filename.endsWith(".wav");
	}

    public void removeUnusedSoundFormats(String path) {
    	File dir = new File(path);
    	File[] sounds = dir.listFiles(this);

    	String soundFormat[] = configuration.getUseSounds().split("[;,]", 10);
    	
    	String soundMatch = ";";
    	for (int i = 0; i < soundFormat.length; i++) {
    		soundMatch += soundFormat[i].trim() + ";";
    	}
    	
    	int deletedFiles = 0;
    	if (sounds != null) {
    		for (File file : sounds) {
    			if (
    				file.getName().matches(".*\\.amr") && soundMatch.indexOf(";amr;") == -1
    				||
    				file.getName().matches(".*\\.mp3") && soundMatch.indexOf(";mp3;") == -1
    				||
    				file.getName().matches(".*\\.wav") && soundMatch.indexOf(";wav;") == -1
    			) {
    				//System.out.println("Delete " + file.getName());
    				file.delete();
    				deletedFiles++;
    			}
    		}
    	}
    	if (deletedFiles > 0) {
    		System.out.println(deletedFiles + " files of unused sound formats removed");
    	}
    }

	
	
	// remove files with a certain extension from dir, except strings with basename list in exceptions
	public void removeFilesWithExt(String path, String ext, String exceptions[]) {
		//System.out.println ("Removing files from " + path + " with ext " + ext + " exceptions: " + exceptions);
		File dir = new File(path);
		String[] files = dir.list();

		int deletedFiles = 0;
		int retainedFiles = 0;
		File file = null;
		if (files != null) {
			for (String name : files) {
				boolean remove = false;
				file = new File(name);
				if (name.matches(".*\\." + ext)) {
					remove = true;
					if (exceptions != null) {
						for (String basename : exceptions) {
							//System.out.println ("Testing filename " + file.getName() + " for exception " + basename);
							if (file.getName().startsWith(basename)) {				
								remove = false;
								//System.out.println ("Test for string " + file.getName() + " exception " + basename + " matched");
								retainedFiles++;
							}
						}
					}
					if (remove) {
						file = new File(path, name);
						file.delete();
						deletedFiles++;
					}
				} else {
					//System.out.println ("checking if it's a dir: " + name);
					file = new File(path, name);
					try {
						if (file.isDirectory()) {
							//System.out.println ("checking subdir: " + file + " (" + file.getCanonicalPath() + ")");
							removeFilesWithExt(file.getCanonicalPath(), ext, exceptions);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		if (retainedFiles > 0) {
			System.out.println("retained " + retainedFiles + " files with extension " + ext);
		}
		if (deletedFiles > 0) {
			System.out.println("deleted " + deletedFiles + " files with extension " + ext);
		}
	}

	private void removeSoundFile(String soundName) {		
		final String soundFormat[] = { "amr", "wav", "mp3" };		
		String soundFile;
		for (int i = 0; i < soundFormat.length; i++) {			
			soundFile = soundName.toLowerCase() + "." + soundFormat[i];			
			File target = new File(path + "/" + soundFile);
			if (target.exists()) {
				target.delete();
				System.out.println(" - removed " + soundFile);
			}		
		}
	}
	
	
	/* Copies the given file in mediaPath to destDir.
	 * - If you specify a filename only it will look for the file in this order:
	 *   1. current directory 2. additional source subdirectory 3.internal file
	 * - For file names only preceded by a single "/", Osm2GpsMid will always assume 
	 *   you want to explicitly use the internal media file.
	 * - Directory path information as part of source media path is allowed, 
	 *   however the media file will ALWAYS be copied to destDir root.
	 * - Remembers copied files in sbCopiedMedias (adds i.e. "(REPLACED)" for replaced files)
	 */
	private String copyMediaToMid(String mediaPath, String destDir, String additionalSrcPath) {
		// output filename is just the name part of the imagePath filename preceded by "/"  
		int iPos = mediaPath.lastIndexOf("/");
		String realMediaPath = configuration.getStyleFileDirectory() + mediaPath;
		String outputMediaName;
		// System.out.println("Processing: " + configuration.getStyleFileDirectory() + 
		//		additionalSrcPath + "/" + mediaPath);
		// if no "/" is contained look for file in current directory and /png
		if (iPos == -1) {
			outputMediaName = "/" + mediaPath;
			// check if file exists in current directory of Osm2GpsMid / the style file
			if (! (new File(realMediaPath).exists())) {
				// check if file exists in current directory of Osm2GpsMid / the style file + "/png" or "/sound"
				realMediaPath = configuration.getStyleFileDirectory() + additionalSrcPath + "/" + mediaPath;
//				System.out.println("checking for realMediaPath: " + realMediaPath);
				if (! (new File(realMediaPath).exists())) {
//					System.out.println("realMediaPath not found: " + realMediaPath);
					// if not check if we can use the version included in Osm2GpsMid.jar
					if (CreateGpsMidData.class.getResource("/media/"  + additionalSrcPath + "/" + mediaPath) == null) {
						// if not check if we can use the internal image file
						if (!(new File(path + outputMediaName).exists())) {	
							// append media name if first media or " ," + media name for the following ones
							sbCopiedMedias.append( (sbCopiedMedias.length() == 0) ? mediaPath : ", " + mediaPath);				
							sbCopiedMedias.append("(ERROR: file not found)");
							mediaInclusionErrors++;
						}
						return outputMediaName;
					} else {
						/**
						 * Copy the file from Osm2GpsMid.jar to the destination directory
						 */
						try {
							BufferedInputStream bis = new BufferedInputStream(
								CreateGpsMidData.class.getResourceAsStream("/media/" + 
										additionalSrcPath + "/"  + mediaPath)
							);
							BufferedOutputStream bos = new BufferedOutputStream(
									new FileOutputStream(destDir + outputMediaName));
							byte[] buf = new byte[4096];
							while (bis.available() > 0) {
								int len = bis.read(buf);
								bos.write(buf, 0, len);
							}
							bos.flush();
							bos.close();
							bis.close();

						} catch (IOException ioe) {
							ioe.printStackTrace();
							sbCopiedMedias.append((sbCopiedMedias.length() == 0) ? mediaPath
											: ", " + mediaPath);
							sbCopiedMedias.append("(ERROR: file not found)");
							mediaInclusionErrors++;
						}
						return outputMediaName;
					}
				}
			}
		// if the first and only "/" is at the beginning its the explicit syntax for internal images
		} else if (iPos == 0) {
			if (!(new File(path + mediaPath).exists())) {	
				// append media name if first media or " ," + media name for the following ones
				sbCopiedMedias.append( (sbCopiedMedias.length() == 0) ? mediaPath : ", " + mediaPath);				
				sbCopiedMedias.append("(ERROR: INTERNAL media file not found)");
				mediaInclusionErrors++;
			}
			return mediaPath;
		// else it's an external file with explicit path
		} else {
			outputMediaName = mediaPath.substring(iPos);
		}
		
		// append media name if first media or " ," + media name for the following ones
		sbCopiedMedias.append( (sbCopiedMedias.length() == 0) ? mediaPath : ", " + mediaPath);					

		try {
//			System.out.println("Copying " + mediaPath + " as " + outputMediaName + " into the midlet");
			FileChannel fromChannel = new FileInputStream(realMediaPath).getChannel();
			// Copy Media file
			try {
				// check if output file already exists
				boolean alreadyExists = (new File(destDir + outputMediaName).exists());
				FileChannel toChannel = new FileOutputStream(destDir + outputMediaName).getChannel();
				fromChannel.transferTo(0, fromChannel.size(), toChannel);
				toChannel.close();
				if(alreadyExists) {
					sbCopiedMedias.append("(REPLACED " + outputMediaName + ")");
				}
			}
			catch (Exception e) {
				sbCopiedMedias.append("(ERROR accessing destination file " + destDir + outputMediaName + ")");
				mediaInclusionErrors++;
				e.printStackTrace();
			}
			fromChannel.close();
		}
		catch (Exception e) {
			System.err.println("Error accessing source file: " + mediaPath);
			sbCopiedMedias.append("(ERROR accessing source file " + mediaPath + ")");
			mediaInclusionErrors++;
			e.printStackTrace();
		}
		return outputMediaName;
	}

	/** Prepares and writes the whole tile data for the specified zoom level to the 
	 * files for dict and tile data.
	 * The tile tree's root tile is put into the member array 'tile'.
	 * 
	 * @param zl Zoom level or level of detail 
	 * @return Number of bytes written
	 */
	private long exportMapToMid(int zl) {
		// System.out.println("Total ways : " + parser.ways.size() + " Nodes : " +
		// parser.nodes.size());
		OsmParser.printMemoryUsage(1);
		long outputLength = 0;
		try {
			FileOutputStream fo = new FileOutputStream(path + "/dat/dict-" + zl + ".dat");
			DataOutputStream ds = new DataOutputStream(fo);
			// magic number
			ds.writeUTF("DictMid");
			Bounds allBound = new Bounds();
			for (Way w1 : parser.getWays()) {				
				if (w1.getZoomlevel(configuration) != zl) {
					continue;
				}
				w1.used = false;
				allBound.extend(w1.getBounds());
			}			
			if (zl == ROUTEZOOMLEVEL) {
				// for RouteNodes
				for (Node n : parser.getNodes()) {
					n.used = false;
					if (n.routeNode == null) {
						continue;
					}
					allBound.extend(n.lat, n.lon);
				}
			} else {
				for (Node n : parser.getNodes()) {
					if (n.getZoomlevel(configuration) != zl) {
						continue;
					}
					allBound.extend(n.lat, n.lon);
				}
			}			
			tile[zl] = new Tile((byte) zl);
			Sequence tileSeq = new Sequence();
			tile[zl].ways = parser.getWays();
			tile[zl].nodes = parser.getNodes();
			// create the tiles and write the content
			outputLength += exportTile(tile[zl], tileSeq, allBound);
			tileFilesWritten = tileSeq.get();
			
			if (tile[zl].type != Tile.TYPE_ROUTECONTAINER && tile[zl].type != Tile.TYPE_CONTAINER) {
				/*
				 * We must have so little data, that it completely fits within one tile.
				 * Never the less, the top tile should be a container tile
				 */								
				Tile ct = new Tile((byte)zl);
				ct.t1 = tile[zl];
				ct.t2 = new Tile((byte)zl);
				ct.t2.type = Tile.TYPE_EMPTY;
				if (zl == ROUTEZOOMLEVEL) {
					ct.type = Tile.TYPE_ROUTECONTAINER;
				} else {
					ct.type = Tile.TYPE_CONTAINER;
				}
				tile[zl] = ct;
			}
			tile[zl].recalcBounds();
			if (zl == ROUTEZOOMLEVEL) {
				long startTime = System.currentTimeMillis();
				for (Node n : parser.getDelayingNodes()) {
					if (n != null) {
						if (n.isTrafficSignals()) {
							tile[zl].markTrafficSignalsRouteNodes(n);
						}
					} else {
						// this should not happen anymore because trafficSignalCount gets decremented now when the node id is duplicate
						System.out.println("Warning: Delaying node is NULL");
					}
				}
				parser.freeUpDelayingNodes();
				
				long time = (System.currentTimeMillis() - startTime);
				System.out.println("  Applied " + parser.trafficSignalCount + 
						" traffic signals to " + Tile.numTrafficSignalRouteNodes + 
						" route nodes, took " + time + " ms");

				Sequence rnSeq = new Sequence();
				tile[zl].renumberRouteNode(rnSeq);
				tile[zl].calcHiLo();
				tile[zl].writeConnections(path, parser.getTurnRestrictionHashMap());
		        tile[zl].type = Tile.TYPE_ROUTECONTAINER;
			}
			Sequence s = new Sequence();
			tile[zl].writeTileDict(ds, 1, s, path);			
			dictFilesWritten = s.get();
			// Magic number
			ds.writeUTF("END");
			ds.close();
			fo.close();

		} catch (FileNotFoundException fnfe) {
			System.err.println("Unhandled FileNotFoundException: " + fnfe.getMessage());
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("Unhandled IOException: " + ioe.getMessage());
			ioe.printStackTrace();
		}
		tile[zl].dissolveTileReferences();
		tile[zl]=null;
		return outputLength;
	}
	
	/** Prepares and writes the tile's node and way data.
	 * It splits the tile, creating two sub tiles, if necessary and continues to
	 * prepare and write their data down the tree.
	 * For writing, it calls writeRenderTile() or writeRouteTile().
	 * 
	 * @param t Tile to export
	 * @param tileSeq 
	 * @param tileBound Bounds to use
	 * @return Number of bytes written
	 * @throws IOException if there is 
	 */
	private long exportTile(Tile t, Sequence tileSeq, Bounds tileBound) throws IOException {
		Bounds realBound = new Bounds();
		ArrayList<Way> ways;
		Collection<Node> nodes;
		int maxSize;
		int maxWays = 0;
		boolean unsplittableTile;
		boolean tooLarge;
		long outputLength = 0;
		/*
		 * Using recursion can cause a stack overflow on large projects,
		 * so we need an explicit stack that can grow larger.
		 */
		Stack<TileTuple> expTiles = new Stack<TileTuple>();
		byte [] out = new byte[1];
		expTiles.push(new TileTuple(t, tileBound));
		byte [] connOut = new byte[1];
		// System.out.println("Exporting Tiles");
		while (!expTiles.isEmpty()) {			
			TileTuple tt = expTiles.pop();
			unsplittableTile = false;
			tooLarge = false;
			t = tt.t; 
			tileBound = tt.bound;
			// System.out.println("try create tile for " + t.zl + " " + tileBound);
			ways = new ArrayList<Way>();
			nodes = new ArrayList<Node>();
			realBound = new Bounds();

			if (t.zl != ROUTEZOOMLEVEL) {
				// Reduce the content of 'ways' and 'nodes' to all relevant elements
				// in the given bounds and create the binary map representation
				maxSize = configuration.getMaxTileSize();
				maxWays = configuration.getMaxTileWays(t.zl);

				ways = getWaysInBound(t.ways, t.zl, tileBound, realBound);

				if (realBound.getFixPtSpan() > 65000
//				    && (t.nodes.size() == nodes.size()) && (t.ways.size() == ways.size())
				    && (tileBound.maxLat - tileBound.minLat < 0.001)) {
					System.out.println("ERROR: Tile spacially too large (" + 
							   MAX_RAD_RANGE +	"tileBound: " + tileBound);
					System.out.println("ERROR:: Could not reduce tile size for tile " + t);
					System.out.println("  t.ways=" + t.ways.size() + ", t.nodes=" + t.nodes.size());
					System.out.println("  realBound=" + realBound);
					System.out.println("  tileBound.maxLat " + tileBound.maxLat
							   + " tileBound.minLat: " + tileBound.minLat);
					for (Way w : t.ways) {
						System.out.println("  Way: " + w);						
					}
					System.out.println("Trying to recover, but at least some map data is lost");
					realBound = tileBound;
				}
				nodes = getNodesInBound(t.nodes, t.zl, tileBound);
				// System.out.println("found " + nodes.size() + " node and " + 
				//		ways.size() + " ways maxSize=" + maxSize + " maxWay=" + maxWays);
				for (Node n : nodes) {
					realBound.extend(n.lat, n.lon);
				}
				if (ways.size() == 0) {
					t.type = Tile.TYPE_EMPTY;
				}
				int mostlyInBound = ways.size();
				addWaysCompleteInBound(ways, t.ways, t.zl, realBound);
				
				//System.out.println("ways.size : " + ways.size() + " mostlyInBound: " + mostlyInBound);		
				if (ways.size() > 2 * mostlyInBound) {
					// System.out.println("ways.size > 2 * mostlyInBound, mostlyInBound: " + mostlyInBound);		
					realBound = new Bounds();
					ways = getWaysInBound(t.ways, t.zl, tileBound, realBound);
					// add nodes as well to the bound HMu: 29.3.2010
					for (Node n : nodes) {
						realBound.extend(n.lat, n.lon);
					}
				}				
				
				if (ways.size() <= maxWays) {
					t.bounds = realBound.clone();
					if (t.bounds.getFixPtSpan() > 65000) {
//						System.out.println("Tile spacially too large (" + 
//								MAX_RAD_RANGE +	": " + t.bounds);
						tooLarge = true;
						// TODO: Doesn't this mean that tile data which should be
						// processed is dropped? I think createMidContent() is never
						// called for it.
					} else {
						t.centerLat = (t.bounds.maxLat + t.bounds.minLat) / 2;
						t.centerLon = (t.bounds.maxLon + t.bounds.minLon) / 2;
						
						// TODO: Isn't this run for tiles which will be split down in
						// this method (below comment "Tile is too large, try to split it.")?
						out = createMidContent(ways, nodes, t);
					}
				}
				/**
				 * If the number of nodes and ways in the new tile is the same, and the bound
				 * has already been shrunk to less than 0.001°, then give up and declare it a
				 * unsplittable tile and just live with the fact that this tile is too big.
				 * Otherwise we can get into an endless loop of trying to split up this tile.
				 */
				if ((t.nodes.size() == nodes.size()) && (t.ways.size() == ways.size()) 
					&& (tileBound.maxLat - tileBound.minLat < 0.001)
					&& (tileBound.maxLon - tileBound.minLon < 0.001)) 
				{
					System.out.println("WARNING: Could not reduce tile size for tile " + t);
					System.out.println("  t.ways=" + t.ways.size() + ", t.nodes=" + t.nodes.size());
					System.out.println("  t.bounds=" + t.bounds);
					System.out.println("  tileBound.maxLat " + tileBound.maxLat
							   + " tileBound.minLat: " + tileBound.minLat);
					System.out.println("  tileBound.maxLon " + tileBound.maxLon
							   + " tileBound.minLon: " + tileBound.minLon);
					for (Way w : t.ways) {
						System.out.println("  Way: " + w);						
					}
					
					unsplittableTile = true;										
					if (tooLarge) {
						out = createMidContent(ways, nodes, t);
					}
				}
				t.nodes = nodes;
				t.ways = ways;
				//t.generateSeaPolygon();
				// TODO: Check if createMidContent() should be here.
			} else {
				// Route Nodes
				maxSize = configuration.getMaxRouteTileSize();
				nodes = getRouteNodesInBound(t.nodes, tileBound, realBound);
				byte[][] erg = createMidContent(nodes, t);
				out = erg[0];
				connOut = erg[1];
				t.nodes = nodes;
			}
			
			if (unsplittableTile && tooLarge) {
				System.out.println("ERROR: Tile is unsplittable, but too large. Can't deal with this! Will try to recover, but some map data has not been processed and the map will probably have errors.");
			}

			// Split tile if more then 255 Ways or binary content > MAX_TILE_FILESIZE but not if only one Way
			
			// System.out.println("out.length=" + out.length + " ways=" + ways.size());
			boolean tooManyWays = ways.size() > maxWays;
			boolean tooManyBytes = out.length > maxSize;
			if ((!unsplittableTile) && ((tooManyWays || (tooManyBytes && ways.size() != 1) || tooLarge))) {
				// Tile is too large, try to split it.
				// System.out.println("create Subtiles size=" + out.length + " ways=" + ways.size());
				t.bounds = realBound.clone();
				if (t.zl != ROUTEZOOMLEVEL) {
					t.type = Tile.TYPE_CONTAINER;				
				} else {
					t.type = Tile.TYPE_ROUTECONTAINER;
				}
				t.t1 = new Tile(t.zl, ways, nodes);
				t.t2 = new Tile(t.zl, ways, nodes);
				t.setRouteNodes(null);
				// System.out.println("split tile because it`s too big, tooLarge=" + tooLarge + 
				//	" tooManyWays=" + tooManyWays + " tooManyBytes=" + tooManyBytes);
				if ((tileBound.maxLat-tileBound.minLat) > (tileBound.maxLon-tileBound.minLon)) {
					// split to half latitude
					float splitLat = (tileBound.minLat + tileBound.maxLat) / 2;
					Bounds nextTileBound = tileBound.clone();
					nextTileBound.maxLat = splitLat;				
					expTiles.push(new TileTuple(t.t1, nextTileBound));
					nextTileBound = tileBound.clone();
					nextTileBound.minLat = splitLat;				
					expTiles.push(new TileTuple(t.t2, nextTileBound));
				} else {
					// split to half longitude
					float splitLon = (tileBound.minLon + tileBound.maxLon) / 2;
					Bounds nextTileBound = tileBound.clone();
					nextTileBound.maxLon = splitLon;				
					expTiles.push(new TileTuple(t.t1, nextTileBound));
					nextTileBound = tileBound.clone();
					nextTileBound.minLon = splitLon;				
					expTiles.push(new TileTuple(t.t2, nextTileBound));
				}
				t.ways = null;
				t.nodes = null;

				// System.gc();
			} else {
				// Tile has the right size or is not splittable, so it can be written.  
				// System.out.println("use this tile, will write " + out.length + " bytes");
				if (ways.size() > 0 || nodes.size() > 0) {
					// Write as dataTile
					t.fid = tileSeq.next();
					if (t.zl != ROUTEZOOMLEVEL) {
						writeRenderTile(t, tileBound, realBound, nodes, out);
						outputLength += out.length;						
					} else {
						writeRouteTile(t, tileBound, realBound, nodes, out);
						outputLength += out.length;
						outputLengthConns += connOut.length;
					}

				} else {
					//Write as empty box
					// System.out.println("this is an empty box");
					t.type = Tile.TYPE_EMPTY;
				}
			}
		}
		return outputLength;
	}

	/**
	 * @param t
	 * @param tileBound
	 * @param realBound
	 * @param nodes
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeRouteTile(Tile t, Bounds tileBound, Bounds realBound,
			Collection<Node> nodes, byte[] out) {
		//System.out.println("Writing render tile " + t.zl + ":" + t.fid + 
		//	" nodes:" + nodes.size());
		t.type = Tile.TYPE_MAP;
		t.bounds = tileBound.clone();
		t.type = Tile.TYPE_ROUTEDATA;
		for (RouteNode n:t.getRouteNodes()) {
			n.node.used = true;
		}
	}

	/**
	 * Writes the byte array to a file for the file t i.e. the name of the file is
	 * derived from the zoom level and fid of this tile.
	 * Also marks all ways of t and sets the fid of these ways and of all nodes in 'nodes'.
	 * Plus it sets the type of t to Tile.TYPE_MAP, sets its bounds to realBound and 
	 * updates totalNodesWritten, totalWaysWritten, totalSegsWritten and totalPOIsWritten.
	 * 
	 * @param t Tile to work on
	 * @param tileBound Bounds of tile will be set to this if it's a route tile
	 * @param realBound Bounds of tile will be set to this
	 * @param nodes Nodes to update with the fid
	 * @param out Byte array to write to the file
	 * @throws FileNotFoundException if file could not be created
	 * @throws IOException if an IO error occurs while writing the file
	 */
	private void writeRenderTile(Tile t, Bounds tileBound, Bounds realBound,
			 Collection<Node> nodes, byte[] out)
			throws FileNotFoundException, IOException {
//		System.out.println("Writing render tile " + t.zl + ":" + t.fid + 
//			" ways:" + t.ways.size() + " nodes:" + nodes.size());
		totalNodesWritten += nodes.size();
		totalWaysWritten += t.ways.size();
		
		//TODO: Is this safe to comment out??
		//Hmu: this was used to have a defined order of drawing. Small ways first, highways last.
		//Collections.sort(t.ways);
		for (Way w: t.ways) {
			totalSegsWritten += w.getLineCount();
		}
		if (t.zl != ROUTEZOOMLEVEL) {
			for (Node n : nodes) {
				if (n.getType(null) > -1 ) {
					totalPOIsWritten++;
				}
			}
		}
		
		t.type = Tile.TYPE_MAP;
		// RouteTiles will be written later because of renumbering
		if (t.zl != ROUTEZOOMLEVEL) {
			t.bounds = realBound.clone();
			String lpath = path  + "/t" + t.zl  ;
			FileOutputStream fo = FileTools.createFileOutputStream(lpath + "/" + t.fid + ".d");
			DataOutputStream tds = new DataOutputStream(fo);
			tds.write(out);
			tds.close();
			fo.close();
			// mark nodes as written to MidStorage 
			for (Node n : nodes) { 
				if (n.fid) {
					System.out.println("DATA DUPLICATION: This node has been written already! " + n);
				}
				n.fid = true; 
			}
			// mark ways as written to MidStorage
			for (Iterator<Way> wi = t.ways.iterator(); wi.hasNext(); ) {
				Way w1 = wi.next();
				w1.used = true;
				// triangles can be cleared but not set to null because of SearchList.java
				if ( w1.triangles != null ) {
					w1.triangles.clear();
				}
			}
		} else {
			t.bounds = tileBound.clone();
			t.type = Tile.TYPE_ROUTEDATA;
			for (RouteNode n:t.getRouteNodes()) {
				n.node.used = true;
			}
		}
	}

	/** Collects all ways from parentWays which 
	 * 1) have a type >= 1 (whatever that means)
	 * 2) belong to the zoom level zl
	 * 3) aren't already marked as used and
	 * 4) are mostly inside the boundaries of targetBounds.
	 * realBound is extended to cover all these ways.
	 * 
	 * @param parentWays the collection that will be used for search
	 * @param zl the level of detail
	 * @param targetBounds bounds used for the search
	 * @param realBound bounds to extend to cover all ways found
	 * @return LinkedList of all ways which meet the described conditions.
	 */
	private ArrayList<Way> getWaysInBound(Collection<Way> parentWays, int zl,
			Bounds targetBounds, Bounds realBound) {
		ArrayList<Way> ways = new ArrayList<Way>();
//		System.out.println("Searching for ways mostly in " + targetTile + " from " + 
//			parentWays.size() + " ways");
		// Collect all ways that are in this rectangle
		for (Way w1 : parentWays) {
			// polish.api.bigstyles
			short type = w1.getType();
			if (type < 1) {
				continue;
			}
			if (w1.getZoomlevel(configuration) != zl) {
				continue;
			}
			if (w1.used) {
				continue;
			}
			Bounds wayBound = w1.getBounds();
			if (targetBounds.isMostlyIn(wayBound)) {
				realBound.extend(wayBound);
				ways.add(w1);
			}
		}
//		System.out.println("getWaysInBound found " + ways.size() + " ways");
		return ways;
	}

	/** Collects all ways from parentWays which 
	 * 1) have a type >= 1 (whatever that means)
	 * 2) belong to the zoom level zl
	 * 3) aren't already marked as used
	 * 4) are completely inside the boundaries of targetTile.
	 * Ways are only added once to the list.
	 * 
	 * @param ways Initial list of ways to which to add
	 * @param parentWays the list that will be used for search
	 * @param zl the level of detail
	 * @param targetBounds bounds used for the search
	 * @return The list 'ways' plus the ways found
	 */
	private ArrayList<Way> addWaysCompleteInBound(ArrayList<Way> ways,
			Collection<Way> parentWays, int zl, Bounds targetBounds) {
		// collect all way that are in this rectangle
//		System.out.println("Searching for ways total in " + targetBounds + 
//			" from " + parentWays.size() + " ways");
		//This is a bit of a hack. We should probably propagate the TreeSet through out,
		//But that needs more effort and time than I currently have. And this way we get
		//rid of a O(n^2) bottle neck
		TreeSet<Way> waysTS = new TreeSet<Way>(ways);
		for (Way w1 : parentWays) {
			// polish.api.bigstyles
			short type = w1.getType();
			if (type < 1) {
				continue;
			}
			if (w1.getZoomlevel(configuration) != zl) {
				continue;
			}
			if (w1.used) {
				continue;
			}
			if (waysTS.contains(w1)) {
				continue;
			}
			Bounds wayBound = w1.getBounds();
			if (targetBounds.isCompleteIn(wayBound)) {
				waysTS.add(w1);
				ways.add(w1);
			}
		}
//		System.out.println("addWaysCompleteInBound found " + ways.size() + " ways");
		return ways;
	}
	
	/**
	 * Find all nodes out of the given collection that are within the bounds and in the correct zoom level.
	 * 
	 * @param parentNodes the collection that will be used for search
	 * @param zl the level of detail
	 * @param targetBound the target boundaries
	 * @return Collection of the nodes found
	 */
	public Collection<Node> getNodesInBound(Collection<Node> parentNodes, int zl, Bounds targetBound) {
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes) {
			//Check to see if the node has already been written to MidStorage
			//If yes, then ignore the node here, to prevent duplicate nodes
			//due to overlapping tiles
			if (node.fid) {
				continue;
			}
			if (node.getType(configuration) < 0) {
				continue;
			}
			if (node.getZoomlevel(configuration) != zl) {
				continue;
			}
			if (! targetBound.isIn(node.lat, node.lon)) {
				continue;
			}
			nodes.add(node);
		}
//		System.out.println("getNodesInBound found " + nodes.size() + " nodes");
		return nodes;
	}

	public Collection<Node> getRouteNodesInBound(Collection<Node> parentNodes, 
			Bounds targetBound,	Bounds realBound) {
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes) {
			if (node.routeNode == null) {
				continue;
			}
			if (! targetBound.isIn(node.lat, node.lon)) {
				continue;
			}
//			System.out.println(node.used);
			if (! node.used) {
				realBound.extend(node.lat, node.lon);
				nodes.add(node);
//				node.used = true;
			} 
		}
		return nodes;
	}

	/**
	 * Create the data-content for a route-tile, containing a list of nodes and a list
	 * of connections from each node.
	 * @param interestNodes list of all Nodes that should be included in this tile
	 * @param t the tile that holds the meta-data
	 * @return in array[0][] the file-format for all nodes and in array[1][] the
	 * file-format for all connections within this tile.
	 * @throws IOException
	 */
	public byte[][] createMidContent(Collection<Node> interestNodes, Tile t) throws IOException {
		ByteArrayOutputStream nfo = new ByteArrayOutputStream();
		DataOutputStream nds = new DataOutputStream(nfo);
		ByteArrayOutputStream cfo = new ByteArrayOutputStream();
		DataOutputStream cds = new DataOutputStream(cfo);
		nds.writeByte(0x54); // magic number
		
		nds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			writeRouteNode(n, nds, cds);
				if (n.routeNode != null) {
					t.addRouteNode(n.routeNode);
				}
		}

		nds.writeByte(0x56); // magic number
		byte [][] ret = new byte[2][];
		ret[0] = nfo.toByteArray();
		ret[1] = cfo.toByteArray();
		nds.close();
		cds.close();
		nfo.close();
		cfo.close();
		return ret;
	}

	/**
	 * Create the Data-content for a SingleTile in memory. This will later directly 
	 * be written to disk if the byte array is not too big, otherwise this tile will
	 * be split in smaller tiles. 
	 * @param ways A collection of ways that are chosen to be in this tile.
	 * @param interestNodes all additional nodes like places, parking and so on 
	 * @param t the tile, holds the metadata for this area.
	 * @return a byte array that represents a file content. This could be written
	 * directly to disk.
	 * @throws IOException
	 */
	public byte[] createMidContent(Collection<Way> ways, Collection<Node> interestNodes, Tile t) throws IOException {
		Map<Long, Node> wayNodes = new HashMap<Long, Node>();
		int ren = 0;
		// reset all used flags of all Nodes that are part of ways in <code>ways</code>
		for (Way way : ways) {
				for (Node n : way.getNodes()) {
					n.used = false;
				}
		}
		// mark all interestNodes as used
		for (Node n1 : interestNodes) {
			n1.used = true;
		}
		// find all nodes that are part of a way but not in interestNodes
		for (Way w1 : ways) {
			if (w1.isArea()) {
				if (w1.checkTriangles() != null) {
					for (Triangle tri : w1.checkTriangles()) {
						for (int lo = 0; lo < 3; lo++) {
							addUnusedNode(wayNodes, tri.getVert()[lo].getNode());
						}
					}
				}
				if (Configuration.getConfiguration().getOutlineAreaFormat()) {
					for (Node n : w1.getNodes()) {
						addUnusedNode(wayNodes, n);
					}
					ArrayList<Path> holes = w1.getHoles();
					if (holes != null) {
						for (Path hole : holes) {
							for (Node n : hole.getNodes()) {
								addUnusedNode(wayNodes, n);
							}
						}
					}
				}
			} else {
				for (Node n : w1.getNodes()) {
					addUnusedNode(wayNodes, n);
				}
			}
		}

		// Create a byte arrayStream which holds the Singletile-Data,
		// this is created in memory and written later if file is 
		// not too big.
		ByteArrayOutputStream fo = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(fo);
		ds.writeByte(0x54); // Magic number
		ds.writeFloat(MyMath.degToRad(t.centerLat));
		ds.writeFloat(MyMath.degToRad(t.centerLon));
		ds.writeShort(interestNodes.size() + wayNodes.size());
		ds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			n.renumberdId = (short) ren++;
			//The exclusion of nodes is not perfect, as there
			//is a time between adding nodes to the write buffer
			//and before marking them as written, so we might
			//still hit the case when a node is written twice.
			//Warn about this fact to fix this correctly at a
			//later stage
			if (n.fid) {
				System.out.println("WARNING: Writing interest node twice, " + n);
			}
			writeNode(n, ds, INODE, t);
		}
		for (Node n : wayNodes.values()) {
			n.renumberdId = (short) ren++;
			writeNode(n, ds, SEGNODE, t);
		}
		ds.writeByte(0x55); // Magic number
		ds.writeShort(ways.size());
		for (Way w : ways) {
			w.write(ds, names1, urls1, t, false);
		}
		ds.writeByte(0x56); // Magic number
		if (Configuration.getConfiguration().getOutlineAreaFormat()) {
			ds.writeShort(ways.size());
			for (Way w : ways) {
				w.write(ds, names1, urls1, t, true);
			}
			ds.writeByte(0x57); // Magic number
		}
		ds.close();
		byte[] ret = fo.toByteArray(); 
		fo.close();
		return ret; 
	}

	/**
	 * Adds the node n and its ID to the map wayNodes if it's an unused node and if
	 * it isn't already in the map.
	 * 
	 * @param wayNodes
	 * @param n
	 */
	private void addUnusedNode(Map<Long, Node> wayNodes, Node n) {
		Long id = new Long(n.id);
		if ((!wayNodes.containsKey(id)) && !n.used) {
			wayNodes.put(id, n);
		}
	}
	
	
	/* FIXME: This is not actually the data written to the file system but rather is used to calculate route tile sizes
	 * The actual route data is written in Tile.writeConnections() and sizes from this data should be used
	 */
	private void writeRouteNode(Node n, DataOutputStream nds, DataOutputStream cds) throws IOException {
		nds.writeByte(4);
		nds.writeFloat(MyMath.degToRad(n.lat));
		nds.writeFloat(MyMath.degToRad(n.lon));
		nds.writeInt(cds.size());
		nds.writeByte(n.routeNode.getConnected().length);
		for (Connection c : n.routeNode.getConnected()) {
			cds.writeInt(c.to.node.renumberdId);
			// set or clear flag for additional byte (connTravelModes is transferred from wayTravelMode were this is Connection.CONNTYPE_TOLLROAD, so clear it if not required) 
			c.connTravelModes |= Connection.CONNTYPE_CONNTRAVELMODES_ADDITIONAL_BYTE;
			if (c.connTravelModes2 == 0) {
				c.connTravelModes ^= Connection.CONNTYPE_CONNTRAVELMODES_ADDITIONAL_BYTE;
			}
			// write out wayTravelModes flag
			cds.writeByte(c.connTravelModes);
			if (c.connTravelModes2 != 0) {
				cds.writeByte(c.connTravelModes2);				
			}
			for (int i = 0; i < TravelModes.travelModeCount; i++) {
				// only store times for available travel modes of the connection
				if ( (c.connTravelModes & (1 << i)) !=0 ) {
					/**
					 * If we can't fit the values into short,
					 * we write an int. In order for the other
					 * side to know if we wrote an int or a short,
					 * we encode the length in the top most (sign) bit
					 */
					int time = c.times[i];
					if (time > Short.MAX_VALUE) {
						cds.writeInt(-1 * time);
					} else {
						cds.writeShort((short) time);
					}
				}
			}
			if (c.length > Short.MAX_VALUE) {
				cds.writeInt(-1 * c.length);
			} else {
				cds.writeShort((short) c.length);
			}
			cds.writeByte(c.startBearing);
			cds.writeByte(c.endBearing);
		}
	}

	private void writeNode(Node n, DataOutputStream ds, int type, Tile t) throws IOException {
		int flags = 0;
		int flags2 = 0;
		int nameIdx = -1;
		int urlIdx = -1;
		int phoneIdx = -1;
		Configuration config = Configuration.getConfiguration();
		if (type == INODE) {
			if (! "".equals(n.getName())) {
				flags += Constants.NODE_MASK_NAME;
				nameIdx = names1.getNameIdx(n.getName());
				if (nameIdx >= Short.MAX_VALUE) {
					flags += Constants.NODE_MASK_NAMEHIGH;
				} 
			}
			if (config.useUrlTags) {
				if (! "".equals(n.getUrl())) {
					flags += Constants.NODE_MASK_URL;
					urlIdx = urls1.getUrlIdx(n.getUrl());
					if (urlIdx >= Short.MAX_VALUE) {
						flags += Constants.NODE_MASK_URLHIGH;
					} 
				}
			}
			if (config.usePhoneTags) {
				if (! "".equals(n.getPhone())) {
					flags2 += Constants.NODE_MASK2_PHONE;
					phoneIdx = urls1.getUrlIdx(n.getPhone());
					if (phoneIdx >= Short.MAX_VALUE) {
						flags2 += Constants.NODE_MASK2_PHONEHIGH;
					} 
				}
			}

			if (n.getType(configuration) != -1) {
				flags += Constants.NODE_MASK_TYPE;
			}
		}
		if (flags2 != 0) {
			flags += Constants.NODE_MASK_ADDITIONALFLAG;
		}
		ds.writeByte(flags);
		if (flags2 != 0) {
			ds.writeByte(flags2);
		}
		
		/**
		 * Convert coordinates to relative fixpoint (integer) coordinates
		 * The reference point is the center of the tile.
		 * With 16bit shorts, this should allow for tile sizes of
		 * about 65 km in width and with 1 m accuracy at the equator.  
		 */
		double tmpLat = (MyMath.degToRad(n.lat - t.centerLat)) * MyMath.FIXPT_MULT;
		double tmpLon = (MyMath.degToRad(n.lon - t.centerLon)) * MyMath.FIXPT_MULT;
		if ((tmpLat > Short.MAX_VALUE) || (tmpLat < Short.MIN_VALUE)) {
			System.err.println("ERROR: Numeric overflow of latitude for node: " + n.id + ", trying to handle");
			if (tmpLat > Short.MAX_VALUE) {
				tmpLat = Short.MAX_VALUE;
			}
			if (tmpLat < Short.MIN_VALUE) {
				tmpLat = Short.MIN_VALUE;
			}
		}
		if ((tmpLon > Short.MAX_VALUE) || (tmpLon < Short.MIN_VALUE)) {
			System.err.println("ERROR: Numeric overflow of longitude for node: " + n.id + ", trying to handle");
			if (tmpLon > Short.MAX_VALUE) {
				tmpLon = Short.MAX_VALUE;
			}
			if (tmpLon < Short.MIN_VALUE) {
				tmpLon = Short.MIN_VALUE;
			}
		}

		ds.writeShort((short)tmpLat);
		ds.writeShort((short)tmpLon);
		
		
		if ((flags & Constants.NODE_MASK_NAME) > 0) {
			if ((flags & Constants.NODE_MASK_NAMEHIGH) > 0) {
				ds.writeInt(nameIdx);
			} else {
				ds.writeShort(nameIdx);
			}
					
		}
		if ((flags & Constants.NODE_MASK_URL) > 0) {
			if ((flags & Constants.NODE_MASK_URLHIGH) > 0) {
				ds.writeInt(urlIdx);
			} else {
				ds.writeShort(urlIdx);
			}
					
		}
		if ((flags2 & Constants.NODE_MASK2_PHONE) > 0) {
			if ((flags2 & Constants.NODE_MASK2_PHONEHIGH) > 0) {
				ds.writeInt(phoneIdx);
			} else {
				ds.writeShort(phoneIdx);
			}
					
		}
		if ((flags & Constants.NODE_MASK_TYPE) > 0) {
			// polish.api.bigstyles
			if (Configuration.getConfiguration().bigStyles) {
				ds.writeShort(n.getType(configuration));
			} else {
				ds.writeByte(n.getType(configuration));
			}
			if (configuration.enableEditingSupport) {
				if (n.id > Integer.MAX_VALUE) {
					// FIXME enable again after Relations.java doesn't use fake ids
					//System.err.println("WARNING: Node OSM-ID won't fit in 32 bits for way " + n);
					ds.writeInt(-1);
				} else {
					ds.writeInt((int)n.id);
				}
			}
		}
	}

	/**
	 * @param c
	 */
	public void setConfiguration(Configuration c) {
		this.configuration = c;
	}

	/**
	 * @param rd
	 */
	public void setRouteData(RouteData rd) {
		this.rd = rd;
	}

}
