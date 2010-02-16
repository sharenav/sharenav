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


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.ConditionTuple;
import de.ueller.osmToGpsMid.model.Connection;
import de.ueller.osmToGpsMid.model.EntityDescription;
import de.ueller.osmToGpsMid.model.MapName;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.TurnRestriction;
import de.ueller.osmToGpsMid.model.WayDescription;
import de.ueller.osmToGpsMid.model.RouteNode;
import de.ueller.osmToGpsMid.model.Sequence;
import de.ueller.osmToGpsMid.model.SubPath;
import de.ueller.osmToGpsMid.model.Tile;
import de.ueller.osmToGpsMid.model.TravelModes;
import de.ueller.osmToGpsMid.model.Way;
import de.ueller.osmToGpsMid.model.name.Names;
import de.ueller.osmToGpsMid.tools.FileTools;




public class CreateGpsMidData implements FilenameFilter {
	
	/**
	 * This class is used in order to store a tuple on a dedicated stack.
	 * So that it is not necessary to use the OS stack in recursion
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
	public final static byte LEGEND_FLAG_MIN_DESCRIPTION_SCALE = 0x40;

	public final static byte ROUTE_FLAG_MOTORWAY = 0x01;
	public final static byte ROUTE_FLAG_MOTORWAY_LINK = 0x02;
	public final static byte ROUTE_FLAG_ROUNDABOUT = 0x04;
		
	// public  final static int MAX_DICT_DEEP=5; replaced by Configuration.maxDictDepth
	public  final static int ROUTEZOOMLEVEL=4;
	OxParser parser;
	Tile tile[]= new Tile[ROUTEZOOMLEVEL+1];
	/** output length of the route connection for statistics */
	long outputLengthConns = 0;
	
	private final String	path;
	TreeSet<MapName> names;
	Names names1;
	StringBuffer sbCopiedMedias= new StringBuffer();
	short mediaInclusionErrors=0;
	
	private final static int INODE=1;
	private final static int SEGNODE=2;
//	private Bounds[] bounds=null;
	private Configuration configuration;
	private int totalWaysWritten=0;
	private int totalSegsWritten=0;
	private int totalNodesWritten=0;
	private int totalPOIsWritten=0;
	private static int dictFilesWritten = 0;
	private static int tileFilesWritten = 0;
	private RouteData rd;
	
	
	public CreateGpsMidData(OxParser parser,String path) {
		super();
		this.parser = parser;
		this.path = path;
		File dir=new File(path);
		// first of all, delete all data-files from a previous run or files that comes
		// from the mid jar file
		if (dir.isDirectory()){
			File[] files = dir.listFiles();
			for (File f : files) {
				if (f.getName().endsWith(".d") || f.getName().endsWith(".dat")){
					if (! f.delete()){
						System.out.println("ERROR: Failed to delete file " + f.getName());
					}
				}
			}
		}
	}
	

	public void exportMapToMid() {
		names1=getNames1();
		exportLegend(path);
		SearchList sl=new SearchList(names1);
		sl.createNameList(path);
		for (int i=0;i<=3;i++){
			System.out.println("Exporting tiles for zoomlevel " + i);
			long bytesWritten = exportMapToMid(i);
			System.out.println("  Zoomlevel " + i + ": " + Configuration.memoryWithUnit(bytesWritten) + " in " + tileFilesWritten  + " files indexed by " + dictFilesWritten + " dictionary files");
		}
		if (Configuration.attrToBoolean(configuration.useRouting) >= 0) {
			System.out.println("Exporting route tiles");
			long bytesWritten = exportMapToMid(ROUTEZOOMLEVEL);
			System.out.println("  " + Configuration.memoryWithUnit(bytesWritten) + " for nodes in " + tileFilesWritten + " files, " +
			Configuration.memoryWithUnit(outputLengthConns) + " for connections in " + tileFilesWritten + " files");
			System.out.println("    The route tiles have been indexed by " + dictFilesWritten + " dictionary files");
		} else {
			System.out.println("No route tiles to export");
		}
//		for (int x=1;x<12;x++){
//			System.out.print("\n" + x + " :");
//			tile[ROUTEZOOMLEVEL].printHiLo(1, x);
//		}
//		System.exit(2);
		sl.createSearchList(path);
		
		// Output statistics for travel modes
		if (Configuration.attrToBoolean(configuration.useRouting) >= 0) {
			for (int i=0; i<TravelModes.travelModeCount; i++) {
				System.out.println(TravelModes.getTravelMode(i).toString());			
			}
		}		
		System.out.println("  MainStreet_Net Connections: " + TravelModes.numMotorwayConnections + " motorway  " 
				+ TravelModes.numTrunkOrPrimaryConnections + " trunk/primary  "
				+ TravelModes.numMainStreetNetConnections + " total");			
		System.out.println("Total ways: "+ totalWaysWritten 
				         + ", segments: " + totalSegsWritten
				         + ", nodes: " + totalNodesWritten
				         + ", POI: " + totalPOIsWritten);
	}
	

	private Names getNames1(){
		Names na=new Names();
		for (Way w : parser.getWays()) {
			na.addName(w);		
		}
		for (Node n : parser.getNodes()) {
			na.addName(n);
		}
		System.out.println("Found " + na.getNames().size() + " names, " + 
				na.getCanons().size() + " canon");
		na.calcNameIndex();
		return (na);
	}
	
	private void exportLegend(String path) {
		FileOutputStream foi;
		String outputMedia;
		try {
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
			/**
			 * Writing colors 
			 */
			dsi.writeShort((short) Configuration.COLOR_COUNT);
			for (int i=0; i < Configuration.COLOR_COUNT; i++) {
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
				dsi.writeInt(LegendParser.tileScaleLevel[i]);
			}
			/**
			 * Write Travel Modes
			 */
			dsi.writeByte(TravelModes.travelModeCount);
			for (int i=0; i<TravelModes.travelModeCount; i++) {
				dsi.writeUTF(TravelModes.getTravelMode(i).getName());
				dsi.writeShort(TravelModes.getTravelMode(i).maxPrepareMeters);
				dsi.writeShort(TravelModes.getTravelMode(i).maxInMeters);
				dsi.writeShort(TravelModes.getTravelMode(i).maxEstimationSpeed);
				dsi.writeByte(TravelModes.getTravelMode(i).travelModeFlags);
			}
			
			/**
			 * Writing POI legend data			 * 
			 */
			dsi.writeByte(config.getPOIDescs().size());
			for (EntityDescription entity : config.getPOIDescs()) {
				POIdescription poi = (POIdescription) entity; 
				byte flags = 0;
				if (poi.image != null && !poi.image.equals(""))
					flags |= LEGEND_FLAG_IMAGE;
				if (poi.searchIcon != null)
					flags |= LEGEND_FLAG_SEARCH_IMAGE;
				if (poi.minEntityScale != poi.minTextScale)
					flags |= LEGEND_FLAG_MIN_IMAGE_SCALE;
				if (poi.textColor != 0)
					flags |= LEGEND_FLAG_TEXT_COLOR;				
				if (!poi.hideable)
					flags |= LEGEND_FLAG_NON_HIDEABLE;
				dsi.writeByte(poi.typeNum);
				dsi.writeByte(flags);
				dsi.writeUTF(poi.description);
				dsi.writeBoolean(poi.imageCenteredOnNode);
				dsi.writeInt(poi.minEntityScale);
				if ((flags & LEGEND_FLAG_IMAGE) > 0) {
					outputMedia=copyMediaToMid(poi.image, path, "png");
					dsi.writeUTF(outputMedia);
				}
				if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {					
					outputMedia=copyMediaToMid(poi.searchIcon, path, "png");
					dsi.writeUTF(outputMedia);
				}
				if ((flags & LEGEND_FLAG_MIN_IMAGE_SCALE) > 0)
					dsi.writeInt(poi.minTextScale);
				if ((flags & LEGEND_FLAG_TEXT_COLOR) > 0)
					dsi.writeInt(poi.textColor);
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
			dsi.writeByte(Configuration.getConfiguration().getWayDescs().size());
			for (EntityDescription entity : Configuration.getConfiguration().getWayDescs()) {
				WayDescription way = (WayDescription) entity;
				byte flags = 0;
				if (!way.hideable)
					flags |= LEGEND_FLAG_NON_HIDEABLE;				
				if (way.minOnewayArrowScale != 0)
					flags |= LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE;
				if (way.minDescriptionScale != 0)
					flags |= LEGEND_FLAG_MIN_DESCRIPTION_SCALE;
				dsi.writeByte(way.typeNum);
				dsi.writeByte(flags);
				byte routeFlags=0;
				if (way.value.equalsIgnoreCase("motorway"))
					routeFlags |= ROUTE_FLAG_MOTORWAY;
				if (way.value.equalsIgnoreCase("motorway_link"))
					routeFlags |= ROUTE_FLAG_MOTORWAY_LINK;					
				dsi.writeByte(routeFlags);
				dsi.writeUTF(way.description);								
				dsi.writeInt(way.minEntityScale);
				dsi.writeInt(way.minTextScale);				
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
				if ((flags & LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE) > 0)
					dsi.writeInt(way.minOnewayArrowScale);
				if ((flags & LEGEND_FLAG_MIN_DESCRIPTION_SCALE) > 0)
					dsi.writeInt(way.minDescriptionScale);
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
			
			RouteSoundSyntax soundSyn = new RouteSoundSyntax(configuration.getSoundFiles(), path + "/syntax.dat");
			
			
			/**
			 * Copy sounds for all sound formats to midlet 
			 */
	    	String soundFormat[] = configuration.getUseSounds().split("[;,]", 2);
	    	dsi.write((byte) soundFormat.length);
	    	for (int j = 0; j < soundFormat.length; j++) {
		    	dsi.writeUTF(soundFormat[j].trim());
	    	}
			
			String soundFile;
			Object soundNames[] = soundSyn.getSoundNames(); 
			for (int i = 0; i < soundNames.length ; i++) {
				soundFile = (String) soundNames[i];
				soundFile = soundFile.toLowerCase();
		    	for (int j = 0; j < soundFormat.length; j++) {
					outputMedia = copyMediaToMid(soundFile + "." + soundFormat[j].trim(), path, configuration.getSoundFiles());					
				}
			}
			removeUnusedSoundFormats(path);

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

				// if useIcons==small or useIcons==big rename the corresponding icons to normal icons
				if (Configuration.attrToBoolean(configuration.useIcons) == 0) {
					renameAlternativeIconSizeToUsedIconSize(configuration.useIcons + "_");
				}
				removeUnusedIconSizes(path, false);
			}

			
			// show summary for copied media files
			if (sbCopiedMedias.length()!=0) {
				System.out.println("External media inclusion summary:");
				sbCopiedMedias.append("\r\n");
			} else {				
				System.out.println("No external medias included.");
			}
			sbCopiedMedias.append("  Media Sources for external medias\r\n");
			sbCopiedMedias.append("  referenced in " + configuration.getStyleFileName() +" have been:\r\n");
			sbCopiedMedias.append("    " + (configuration.getStyleFileDirectory().length() == 0 ? "Current directory" : configuration.getStyleFileDirectory()) + " and its png and sound subdirectories");
			System.out.println(sbCopiedMedias.toString());
			if (mediaInclusionErrors!=0) {
				System.out.println("");
				System.out.println("  WARNING: " + mediaInclusionErrors + 
						" media files could NOT be included - see details above");
				System.out.println("");
			}
			
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
    			if (file.getName().matches("(small|big)_is?_.*\\.png")
    				|| (deleteAllIcons && file.getName().matches("is?_.*\\.png") && !file.getName().equalsIgnoreCase("i_bg.png")) 
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

    	String soundFormat[] = configuration.getUseSounds().split("[;,]", 2);
    	
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

	
	
	private void removeSoundFile(String soundName) {		
		final String soundFormat[] = {"amr", "wav", "mp3"};		
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
	
	
	/* Copies the given file in mediaPath to destDir
	 * - if you specify a filename only it will look for the file in this order 1. current directory 2. additional source subdirectory 3.internal file
	 * - for file names only preceded by a single "/" Osm2GpsMid will always assume you want to explicitely use the internal media file
	 * - directory path information as part of source media path is allowed, however the media file will ALWAYS be copied to destDir root
	 * - remembers copied files in sbCopiedMedias (adds i.e. "(REPLACED)" for replaced files)
	 */
	private String copyMediaToMid(String mediaPath, String destDir, String additionalSrcPath) {
		// output filename is just the name part of the imagePath filename preceded by "/"  
		int iPos=mediaPath.lastIndexOf("/");
		String realMediaPath = configuration.getStyleFileDirectory() + mediaPath;
		String outputMediaName;
//		System.out.println("Processing: " + configuration.getStyleFileDirectory() + additionalSrcPath+"/"+mediaPath);
		// if no "/" is contained look for file in current directory and /png
		if(iPos==-1) {
			outputMediaName="/" + mediaPath;
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
							// append media name if first media or " ,"+media name for the following ones
							sbCopiedMedias.append( (sbCopiedMedias.length()==0)?mediaPath:", " + mediaPath);				
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
									CreateGpsMidData.class.getResourceAsStream("/media/" + additionalSrcPath + "/"  + mediaPath)
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
		} else if(iPos==0) {
			if (!(new File(path + mediaPath).exists())) {	
				// append media name if first media or " ,"+media name for the following ones
				sbCopiedMedias.append( (sbCopiedMedias.length()==0)?mediaPath:", " + mediaPath);				
				sbCopiedMedias.append("(ERROR: INTERNAL media file not found)");
				mediaInclusionErrors++;
			}
			return mediaPath;
		// else it's an external file with explicit path
		} else {
			outputMediaName=mediaPath.substring(iPos);
		}
		
		// append media name if first media or " ,"+media name for the following ones
		sbCopiedMedias.append( (sbCopiedMedias.length()==0)?mediaPath:", " + mediaPath);					

		try {
//			System.out.println("Copying " + mediaPath + " as " + outputMediaName + " into the midlet");
			FileChannel fromChannel = new FileInputStream(realMediaPath).getChannel();
			// Copy Media file
			try {
				// check if output file already exists
				boolean alreadyExists= (new File(destDir + outputMediaName).exists());
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

	
	private long exportMapToMid(int zl) {
		// System.out.println("Total ways : " + parser.ways.size() + " Nodes : " +
		// parser.nodes.size());
		long outputLength = 0;
		try {
			FileOutputStream fo = new FileOutputStream(path+"/dict-"+zl+".dat");
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
					n.used=false;
					if (n.routeNode == null) continue;
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
			if (zl == ROUTEZOOMLEVEL){
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
			fo.close();

		} catch (FileNotFoundException fnfe) {
			System.err.println("Unhandled FileNotFoundException: " + fnfe.getMessage());
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("Unhandled IOException: " + ioe.getMessage());
			ioe.printStackTrace();
		}
		return outputLength;
	}
	
	private long exportTile(Tile t, Sequence tileSeq, Bounds tileBound) throws IOException {
		Bounds realBound = new Bounds();
		LinkedList<Way> ways;
		Collection<Node> nodes;
		int maxSize;
		int maxWays = 0;
		boolean unsplittableTile;
		boolean tooLarge;
		long outputLength = 0;
		/*
		 * Using recursion can cause a stack overflow on large projects,
		 * so need an explicit stack that can grow larger;
		 */
		Stack<TileTuple> expTiles = new Stack<TileTuple>();
		byte [] out=new byte[1];
		expTiles.push(new TileTuple(t,tileBound));
		byte [] connOut;
		//System.out.println("Exporting Tiles");
		while (!expTiles.isEmpty()) {			
			TileTuple tt = expTiles.pop();
			unsplittableTile = false;
			tooLarge = false;
			t = tt.t; tileBound = tt.bound;
			ways=new LinkedList<Way>();
			nodes=new ArrayList<Node>();
			realBound=new Bounds();

			// Reduce the content of t.ways and t.nodes to all relevant elements
			// in the given bounds and create the binary midlet representation
			if (t.zl != ROUTEZOOMLEVEL){
				maxSize = configuration.getMaxTileSize();
				maxWays = configuration.getMaxTileWays(t.zl);

				ways=getWaysInBound(t.ways, t.zl,tileBound,realBound);
				nodes=getNodesInBound(t.nodes,t.zl,tileBound);
				for (Node n : nodes) {
					realBound.extend(n.lat,n.lon);
				}
				if (ways.size() == 0){
					t.type=3;
				}
				int mostlyInBound=ways.size();
				addWaysCompleteInBound(ways,t.ways,t.zl,realBound);
				if (ways.size() > 2*mostlyInBound){
					realBound=new Bounds();
					ways=getWaysInBound(t.ways, t.zl,tileBound,realBound);
				}				
				
				if (ways.size() <= maxWays){
					t.bounds = realBound.clone();
					if ((MyMath.degToRad(t.bounds.maxLat - t.bounds.minLat) > 
							(Short.MAX_VALUE - Short.MIN_VALUE - 2000) / MyMath.FIXPT_MULT) ||
						(MyMath.degToRad(t.bounds.maxLon - t.bounds.minLon) > 
							(Short.MAX_VALUE - Short.MIN_VALUE - 2000) / MyMath.FIXPT_MULT))
					{
						//System.out.println("Tile spacially too large (" + 
						//	((Short.MAX_VALUE - Short.MIN_VALUE - 2000) / MyMath.FIXPT_MULT) +
						//	": " + t.bounds);
						tooLarge = true;
							
					} else {
						t.centerLat = (t.bounds.maxLat - t.bounds.minLat) / 2 + t.bounds.minLat;
						t.centerLon = (t.bounds.maxLon - t.bounds.minLon) / 2 + t.bounds.minLon;
						out=createMidContent(ways,nodes,t);
						outputLength += out.length;						
					}
				}
				/**
				 * If the number of nodes and ways in the new tile is the same, and the bound
				 * has already been shrunk to less than 0.001°, then give up and declare it a
				 * unsplittable tile and just live with the fact that this tile is too big.
				 * Otherwise we can get into an endless loop of trying to split up this tile.
				 */
				if ((t.nodes.size() == nodes.size()) && (t.ways.size() == ways.size()) && (tileBound.maxLat - tileBound.minLat < 0.001)) {
					System.out.println("WARNING: Could not reduce tile size for tile " + t);
					System.out.println("  t.ways=" + t.ways.size() + ", t.nodes=" + t.nodes.size());
					for (Way w : t.ways) {
						System.out.println("  Way: " + w);						
					}
					
					unsplittableTile = true;										
				}
				t.nodes = nodes;
				t.ways = ways;
			} else {
				// Route Nodes
				maxSize=configuration.getMaxRouteTileSize();
				nodes=getRouteNodesInBound(t.nodes,tileBound,realBound);
				byte[][] erg=createMidContent(nodes,t);
				out=erg[0];
				outputLength += out.length;
				connOut = erg[1];
				outputLengthConns += connOut.length;
				t.nodes=nodes;
			}
			
			if (unsplittableTile && tooLarge) {
				System.out.println("WARNING: Tile is unsplittable, but too large. Can't deal with this!");
			}

			// Split tile if more then 255 Ways or binary content > MAX_TILE_FILESIZE but not if only one Way
			if ((!unsplittableTile) && ((ways.size() > maxWays || (out.length > maxSize && ways.size() != 1) || tooLarge))){
				//System.out.println("create Subtiles size="+out.length+" ways=" + ways.size());
				t.bounds=realBound.clone();
				if (t.zl != ROUTEZOOMLEVEL){
					t.type=Tile.TYPE_CONTAINER;				
				} else {
					t.type=Tile.TYPE_ROUTECONTAINER;
				}
				t.t1=new Tile((byte) t.zl,ways,nodes);
				t.t2=new Tile((byte) t.zl,ways,nodes);
				t.setRouteNodes(null);
				if ((tileBound.maxLat-tileBound.minLat) > (tileBound.maxLon-tileBound.minLon)){
					// split to half latitude
					float splitLat=(tileBound.minLat+tileBound.maxLat)/2;
					Bounds nextTileBound=tileBound.clone();
					nextTileBound.maxLat=splitLat;				
					expTiles.push(new TileTuple(t.t1,nextTileBound));
					nextTileBound=tileBound.clone();
					nextTileBound.minLat=splitLat;				
					expTiles.push(new TileTuple(t.t2,nextTileBound));
				} else {
					// split to half longitude
					float splitLon=(tileBound.minLon+tileBound.maxLon)/2;
					Bounds nextTileBound=tileBound.clone();
					nextTileBound.maxLon=splitLon;				
					expTiles.push(new TileTuple(t.t1,nextTileBound));
					nextTileBound=tileBound.clone();
					nextTileBound.minLon=splitLon;				
					expTiles.push(new TileTuple(t.t2,nextTileBound));
				}
				t.ways=null;
				t.nodes=null;

				//			System.gc();
			} else {
				if (ways.size() > 0 || nodes.size() > 0){					
					// Write as dataTile
					t.fid=tileSeq.next();
					if (t.zl != ROUTEZOOMLEVEL) {
						t.setWays(ways);
						writeRenderTile(t, tileBound, realBound, nodes, out);
					} else {
						writeRouteTile(t, tileBound, realBound, nodes, out);
					}

				} else {
					//Write as empty box
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
		t.type=Tile.TYPE_MAP;
		t.bounds=tileBound.clone();
		t.type=Tile.TYPE_ROUTEDATA;
		for (RouteNode n:t.getRouteNodes()){
			n.node.used=true;
		}
	}
	/**
	 * @param t
	 * @param tileBound
	 * @param realBound
	 * @param ways
	 * @param nodes
	 * @param out
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void writeRenderTile(Tile t, Bounds tileBound, Bounds realBound,
			 Collection<Node> nodes, byte[] out)
			throws FileNotFoundException, IOException {
		//System.out.println("Writing route tile " + t.zl + ":" + t.fid + 
		//	" ways:" + t.ways.size() + " nodes:" + nodes.size());
		totalNodesWritten += nodes.size();
		totalWaysWritten += t.ways.size();
		
		//TODO: Is this safe to comment out??
		//Collections.sort(t.ways);
		for (Way w: t.ways){
			totalSegsWritten += w.getLineCount();
		}
		if (t.zl != ROUTEZOOMLEVEL) {
			for (Node n : nodes) {
				if (n.getType(null) > -1 )
					totalPOIsWritten++;
			}
		}
		
		t.type=Tile.TYPE_MAP;
		// RouteTiles will be written later because of renumbering
		if (t.zl != ROUTEZOOMLEVEL) {
			t.bounds = realBound.clone();
			FileOutputStream fo = new FileOutputStream(path + "/t" + t.zl
					+ t.fid + ".d");
			DataOutputStream tds = new DataOutputStream(fo);
			tds.write(out);
			fo.close();
			// mark nodes as written to MidStorage 
			for (Node n : nodes) { 
				if (n.fid != 0) {
					System.out.println("DATA DUPLICATION: This node has been written already! " + n);
				}
				n.fid = t.fid; 
			}
			// mark ways as written to MidStorage
			for (Iterator<Way> wi = t.ways.iterator(); wi.hasNext();) {
				Way w1 = wi.next();
				w1.used = true;
				w1.fid = t.fid;
			}
		} else {
			t.bounds = tileBound.clone();
			t.type = Tile.TYPE_ROUTEDATA;
			for (RouteNode n:t.getRouteNodes()){
				n.node.used = true;
			}
		}
	}

	private LinkedList<Way> getWaysInBound(Collection<Way> parentWays, int zl, 
			Bounds targetTile, Bounds realBound) {
		LinkedList<Way> ways = new LinkedList<Way>();
//		System.out.println("Searching for ways mostly in " + targetTile + " from " + 
//			parentWays.size() + " ways");
		// Collect all ways that are in this rectangle
		for (Way w1 : parentWays) {
			byte type = w1.getType();
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
			if (targetTile.isMostlyIn(wayBound)) {
				realBound.extend(wayBound);
				ways.add(w1);
			}
		}
//		System.out.println("getWaysInBound found " + ways.size() + " ways");
		return ways;
	}

	private LinkedList<Way> addWaysCompleteInBound(LinkedList<Way> ways,Collection<Way> parentWays,int zl,Bounds targetTile){
		// collect all way that are in this rectangle
//		System.out.println("Searching for ways total in " + targetTile + 
//			" from " + parentWays.size() + " ways");
		//This is a bit of a hack. We should probably propagate the TreeSet through out,
		//But that needs more effort and time than I currently have. And this way we get
		//rid of a O(n^2) bottle neck
		TreeSet<Way> waysTS = new TreeSet<Way>(ways);
		for (Way w1 : parentWays) {
			byte type=w1.getType();
			if (type == 0) {
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
			if (targetTile.isCompleteIn(wayBound)) {
				waysTS.add(w1);
				ways.add(w1);
			}
		}
//		System.out.println("addWaysCompleteInBound found " + ways.size() + " ways");
		return ways;
	}
	
	public Collection<Node> getNodesInBound(Collection<Node> parentNodes,int zl,Bounds targetBound){
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes){
			//Check to see if the node has already been written to MidStorage
			//If yes, then ignore the node here, to prevent duplicate nodes
			//due to overlapping tiles
			if (node.fid != 0) continue;
			if (node.getType(configuration) < 0) continue;
			if (node.getZoomlevel(configuration) != zl) continue;
			if (! targetBound.isIn(node.lat,node.lon)) continue;
			nodes.add(node);
		}
//		System.out.println("getNodesInBound found " + nodes.size() + " nodes");
		return nodes;
	}

	public Collection<Node> getRouteNodesInBound(Collection<Node> parentNodes,Bounds targetBound,Bounds realBound){
		Collection<Node> nodes = new LinkedList<Node>();
		for (Node node : parentNodes){
			if (node.routeNode == null) continue;
			if (! targetBound.isIn(node.lat,node.lon)) continue;
//			System.out.println(node.used);
			if (! node.used) {
				realBound.extend(node.lat,node.lon);
				nodes.add(node);
//				node.used=true;
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
	public byte[][] createMidContent(Collection<Node> interestNodes, Tile t) throws IOException{
		ByteArrayOutputStream nfo = new ByteArrayOutputStream();
		DataOutputStream nds = new DataOutputStream(nfo);
		ByteArrayOutputStream cfo = new ByteArrayOutputStream();
		DataOutputStream cds = new DataOutputStream(cfo);
		nds.writeByte(0x54); // magic number
		
		nds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			writeRouteNode(n,nds,cds);
				if (n.routeNode != null) {
					t.addRouteNode(n.routeNode);
				}
		}

		nds.writeByte(0x56); // magic number
		nfo.close();
		cfo.close();
		byte [][] ret = new byte[2][];
		ret[0]=nfo.toByteArray();
		ret[1]=cfo.toByteArray();
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
	public byte[] createMidContent(Collection<Way> ways,Collection<Node> interestNodes, Tile t) throws IOException{
		Map<Long,Node> wayNodes = new HashMap<Long,Node>();
		int ren=0;
		// reset all used flags of all Nodes that are part of ways in <code>ways</code>
		for (Way way : ways) {
			for (SubPath sp:way.getSubPaths()){
				for (Node n:sp.getNodes()){
					n.used=false;
				}
			}
		}
		// mark all interestNodes as used
		for (Node n1 : interestNodes){
			n1.used=true;
		}
		// find all nodes that are part of a way but not in interestNodes
		for (Way w1: ways) {
			for (SubPath sp:w1.getSubPaths()){
				for (Node n:sp.getNodes()){
					Long id=new Long(n.id);
					if ((!wayNodes.containsKey(id)) && !n.used){
						wayNodes.put(id, n);
					}

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
		ds.writeShort(interestNodes.size()+wayNodes.size());
		ds.writeShort(interestNodes.size());		
		for (Node n : interestNodes) {
			n.renumberdId=(short) ren++;
			//The exclusion of nodes is not perfect, as there
			//is a time between adding nodes to the write buffer
			//and before marking them as written, so we might
			//still hit the case when a node is written twice.
			//Warn about this fact to fix this correctly at a
			//later stage
			if (n.fid != 0) {
				System.out.println("WARNING: Writing interest node twice, " + n);
			}
			writeNode(n,ds,INODE,t);
		}
		for (Node n : wayNodes.values()) {
			n.renumberdId=(short) ren++;
			writeNode(n,ds,SEGNODE,t);
		}
		ds.writeByte(0x55); // Magic number
		ds.writeShort(ways.size());
		for (Way w : ways){
			w.write(ds, names1,t);
		}
		ds.writeByte(0x56); // Magic number
		fo.close();
		return fo.toByteArray();
	}
	
	private void writeRouteNode(Node n,DataOutputStream nds,DataOutputStream cds) throws IOException{
		nds.writeByte(4);
		nds.writeFloat(MyMath.degToRad(n.lat));
		nds.writeFloat(MyMath.degToRad(n.lon));
		nds.writeInt(cds.size());
		nds.writeByte(n.routeNode.connected.size());
		for (Connection c : n.routeNode.connected){
			cds.writeInt(c.to.node.renumberdId);
			// write out wayTravelModes flag
			cds.writeByte(c.connTravelModes);
			for (int i=0; i<TravelModes.travelModeCount; i++) {
				// only store times for available travel modes of the connection
				if ( (c.connTravelModes & (1<<i)) !=0 ) {
					/**
					 * If we can't fit the values into short,
					 * we write an int. In order for the other
					 * side to know if we wrote an int or a short,
					 * we encode the length in the top most (sign) bit
					 */
					int time = c.times[i];
					if (time > Short.MAX_VALUE) {
						cds.writeInt(-1*time);
					} else {
						cds.writeShort((short) time);
					}
				}
			}
			if (c.length > Short.MAX_VALUE) {
				cds.writeInt(-1*c.length);
			} else {
				cds.writeShort((short) c.length);
			}
			cds.writeByte(c.startBearing);
			cds.writeByte(c.endBearing);
		}
	}

	private void writeNode(Node n, DataOutputStream ds, int type, Tile t) throws IOException {
		int flags = 0;
		int nameIdx = -1;
		if (type == INODE){
			if (! "".equals(n.getName())){
				flags += Constants.NODE_MASK_NAME;
				nameIdx = names1.getNameIdx(n.getName());
				if (nameIdx >= Short.MAX_VALUE) {
					flags += Constants.NODE_MASK_NAMEHIGH;
				} 
			}
			if (n.getType(configuration) != -1){
				flags += Constants.NODE_MASK_TYPE;
			}
		}
		ds.writeByte(flags);
		
		/**
		 * Convert coordinates to relative fixpoint (integer) coordinates
		 * The reference point is the center of the tile.
		 * With 16bit shorts, this should allow for tile sizes of
		 * about 65 km in width and with 1 m accuracy at the equator.  
		 */
		double tmpLat = (MyMath.degToRad(n.lat - t.centerLat)) * MyMath.FIXPT_MULT;
		double tmpLon = (MyMath.degToRad(n.lon - t.centerLon)) * MyMath.FIXPT_MULT;
		if ((tmpLat > Short.MAX_VALUE) || (tmpLat < Short.MIN_VALUE)) {
			System.err.println("ERROR: Numeric overflow of latitude for node: " + n.id);
		}
		if ((tmpLon > Short.MAX_VALUE) || (tmpLon < Short.MIN_VALUE)) {
			System.err.println("ERROR: Numeric overflow of longitude for node: " + n.id);
		}
		ds.writeShort((short)tmpLat);
		ds.writeShort((short)tmpLon);
		
		
		if ((flags & Constants.NODE_MASK_NAME) > 0){
			if ((flags & Constants.NODE_MASK_NAMEHIGH) > 0) {
				ds.writeInt(nameIdx);
			} else {
				ds.writeShort(nameIdx);
			}
					
		}
		if ((flags & Constants.NODE_MASK_TYPE) > 0) {
			ds.writeByte(n.getType(configuration));
			if (configuration.enableEditingSupport) {
				if (n.id > Integer.MAX_VALUE) {
					System.err.println("ERROR: OSM-ID won't fit in 32 bits for way " + n);
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
