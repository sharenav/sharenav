/**
 * This file is part of OSM2GpsMid
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * Copyright (C) 2008  Kai Krueger
 * Copyright (C) 2008  sk750
 * 
 */
package de.ueller.osmToGpsMid;

import java.awt.geom.Area;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.tools.bzip2.CBZip2InputStream;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.EntityDescription;
import de.ueller.osmToGpsMid.model.POIdescription;
import de.ueller.osmToGpsMid.model.WayDescription;
import de.ueller.osmToGpsMid.route.Location;
import de.ueller.osmToGpsMid.route.Route;

/**
 * This class reads and holds all the configuration information needed to generate
 * the target Midlet. These are mainly:
 * <ul>
 * <li>Where to get the OSM XML data</li>
 * <li>The bundle .properties file and the settings it contains</li>
 * <li>The name and path of the style-file</li>
 * <li>Reference to the LegendParser which parses the style-file data</li>
 * </ul>
 */
public class Configuration {
	
	/**
	 * Specifies the format of the map on disk we are about to write.
	 * This constant must be in sync with GpsMid.
	 */
	public final static short MAP_FORMAT_VERSION = 72;

	public final static int COLOR_MAP_BACKGROUND = 0;
	public final static int COLOR_MAP_TEXT = 1;
	public final static int COLOR_MAP_TOUCHED_BUTTON_BACKGROUND = 2;
	public final static int COLOR_SPEED_BACKGROUND = 3;
	public final static int COLOR_SPEED_TEXT = 4;
	public final static int COLOR_TITLEBAR_BACKGROUND = 5;
	public final static int COLOR_TITLEBAR_TEXT = 6;
	public final static int COLOR_SOLUTION_BACKGROUND = 7;
	public final static int COLOR_SOLUTION_TEXT = 8;
	public final static int COLOR_WAYNAME_BACKGROUND = 9;
	public final static int COLOR_WAYNAME_TEXT = 10;
	public final static int COLOR_AREA_LABEL_TEXT = 11;
	public final static int COLOR_WAY_LABEL_TEXT = 12;
	public final static int COLOR_WAY_LABEL_TEXT_ABBREVIATED = 13;
	public final static int COLOR_POI_LABEL_TEXT = 14;
	public final static int COLOR_WAYPOINT_TEXT = 15;
	public final static int COLOR_ONEWAY_ARROW = 16;
	public final static int COLOR_ONEWAY_ARROW_NON_FITTING = 17;
	public final static int COLOR_RECORDING_BACKGROUND = 18;
	public final static int COLOR_RECORDING_SUSPENDED_TEXT = 19;
	public final static int COLOR_RECORDING_ON_TEXT = 20;
	public final static int COLOR_CELLID_LOG_ON_TEXT = 21;
	public final static int COLOR_CELLID_LOG_ON_ATTEMPTING_TEXT = 22;
	public final static int COLOR_AUDIOREC_TEXT = 23;
	public final static int COLOR_DEST_TEXT = 24;
	public final static int COLOR_DEST_LINE = 25;	
	public final static int COLOR_MAP_CURSOR = 26;	
	public final static int COLOR_MAP_POSINDICATOR = 27;	
	public final static int COLOR_SCALEBAR = 28;
	public final static int COLOR_ZOOM_BUTTON = 29;	
	public final static int COLOR_ZOOM_BUTTON_TEXT = 30;	
	public final static int COLOR_COMPASS_DIRECTION_BACKGROUND = 31;	
	public final static int COLOR_COMPASS_DIRECTION_TEXT = 32;	
	public final static int COLOR_SPEEDING_SIGN_BORDER = 33;	
	public final static int COLOR_SPEEDING_SIGN_INNER = 34;	
	public final static int COLOR_SPEEDING_SIGN_TEXT = 35;	
	public final static int COLOR_RI_AT_DEST = 36;
	public final static int COLOR_RI_ON_ROUTE = 37;
	public final static int COLOR_RI_OFF_ROUTE_SLIGHT = 38;
	public final static int COLOR_RI_OFF_ROUTE_FULL = 39;
	public final static int COLOR_RI_NOT_AT_ROUTE_LINE_YET = 40;
	public final static int COLOR_RI_CHECK_DIRECTION = 41;
	public final static int COLOR_RI_TEXT = 42;
	public final static int COLOR_RI_DISTANCE_BACKGROUND = 43;
	public final static int COLOR_RI_DISTANCE_TEXT = 44;
	public final static int COLOR_RI_ETA_BACKGROUND = 45;
	public final static int COLOR_RI_ETA_TEXT = 46;
	public final static int COLOR_RI_OFF_DISTANCE_TEXT = 47;
	public final static int COLOR_ROUTE_ROUTELINE = 48;
	public final static int COLOR_ROUTE_ROUTELINE_BORDER = 49;
	public final static int COLOR_ROUTE_PRIOR_ROUTELINE = 50;
	public final static int COLOR_ROUTE_PRIOR_ROUTELINE_BORDER = 51;
	public final static int COLOR_ROUTE_ROUTEDOT = 52;
	public final static int COLOR_ROUTE_ROUTEDOT_BORDER = 53;
	public final static int COLOR_ICONMENU_BACKGROUND = 54;
	public final static int COLOR_ICONMENU_TABBUTTON_BORDER = 55;
	public final static int COLOR_ICONMENU_TABBUTTON_TEXT = 56;
	public final static int COLOR_ICONMENU_TABBUTTON_TEXT_HIGHLIGHT = 57;
	public final static int COLOR_ICONMENU_TABBUTTON_TEXT_INACTIVE = 58;
	public final static int COLOR_ICONMENU_ICON_TEXT = 59;
	public final static int COLOR_ICONMENU_ICON_BORDER_HIGHLIGHT = 60;
	public final static int COLOR_ICONMENU_TOUCHED_BUTTON_BACKGROUND_COLOR = 61;
	public final static int COLOR_ALERT_BACKGROUND = 62;
	public final static int COLOR_ALERT_BORDER = 63;
	public final static int COLOR_ALERT_TITLE_BACKGROUND = 64;
	public final static int COLOR_ALERT_TEXT = 65;
	public final static int COLOR_TACHO_BACKGROUND = 66;
	public final static int COLOR_TACHO_TEXT = 67;
	public final static int COLOR_CLOCK_BACKGROUND = 68;
	public final static int COLOR_CLOCK_TEXT = 69;
	public final static int COLOR_BRIDGE_DECORATION = 70;
	public final static int COLOR_TUNNEL_DECORATION = 71;
	public final static int COLOR_TOLLROAD_DECORATION = 72;	
	public final static int COLOR_WAY_DAMAGED_BORDER = 73;
	public final static int COLOR_WAY_DAMAGED_DECORATION = 74;
	public final static int COLOR_DAMAGED_BORDER = 75;
	public final static int COLOR_SEARCH_BACKGROUND = 76;
	public final static int COLOR_SEARCH_ARROWS = 77;
	public final static int COLOR_SEARCH_SELECTED_TYPED = 78;
	public final static int COLOR_SEARCH_SELECTED_REST = 79;
	public final static int COLOR_SEARCH_NONSELECTED_TYPED = 80;
	public final static int COLOR_SEARCH_NONSELECTED_REST = 81;
	public final static int COLOR_SEARCH_TOUCHED_BUTTON_BACKGROUND = 82;
	public final static int COLOR_SEARCH_BUTTON_TEXT = 83;
	public final static int COLOR_SEARCH_BUTTON_BORDER = 84;
	public final static int COLOR_MENU_BACKGROUND = 85;
	public final static int COLOR_MENU_TEXT = 86;
	public final static int COLOR_ALTITUDE_BACKGROUND = 87;
	public final static int COLOR_ALTITUDE_TEXT = 88;
	public final static int COLOR_COUNT = 89;
	
	public final static String COLORNAMES[] =
			{"map_background",
			 "map_text",
			 "map_touched_button_background",
			 "speed_background",
			 "speed_text",
			 "titleBar_background",
			 "titleBar_text",
			 "solution_background",
			 "solution_text",
			 "wayName_background",
			 "wayName_text",
			 "area_label_text",
			 "way_label_text",
			 "way_label_text_abbreviated",
			 "poi_label_text",
			 "wayPoint_label_text",
			 "oneway_arrow",
			 "oneway_arrow_non_fitting",
			 "recording_background",
			 "recording_suspended_text",
			 "recording_on_text",
			 "cellid_log_on_text",
			 "cellid_log_on_attempting_text",
			 "audioRec_text",
			 "dest_text",
			 "dest_line",
			 "map_cursor",
			 "map_posindicator",
			 "scalebar",
			 "zoom_button",
			 "zoom_text",
			 "compass_direction_background",
			 "compass_direction_text",
			 "speeding_sign_border",
			 "speeding_sign_inner",
			 "speeding_sign_text",
			 "ri_at_dest",
			 "ri_on_route",
			 "ri_off_route_slight",
			 "ri_off_route_full",
			 "ri_not_at_route_line_yet",
			 "ri_check_direction",
			 "ri_text",
			 "ri_distance_background",
			 "ri_distance_text",
			 "ri_eta_background",
			 "ri_eta_text",
			 "ri_off_distance_text",
			 "routeLine",
			 "routeLine_border",
			 "priorRouteLine",
			 "priorRouteLine_border",
			 "routeDot",
			 "routeDot_border",
			 "iconMenu_background",
			 "iconMenu_tabbutton_border",
			 "iconMenu_tabbutton_text",
			 "iconMenu_tabbutton_text_highlight",
			 "iconMenu_tabbutton_text_inactive",
			 "iconMenu_icon_text",
			 "iconMenu_icon_border_highlight",
			 "iconMenu_touched_button_background",
			 "alert_background",
			 "alert_border",
			 "alert_title_background",
			 "alert_text",
			 "tacho_background",
			 "tacho_text",
			 "clock_background",
			 "clock_text",
			 "bridge_decoration",
			 "tunnel_decoration",
			 "tollroad_decoration",
			 "way_damaged_border",
			 "way_damaged_decoration",
			 "area_damaged_border",
			 "search_background",
			 "search_arrows",
			 "search_selected_typed",
			 "search_selected_rest",
			 "search_nonselected_typed",
			 "search_nonselected_rest",
			 "search_touched_button_background",
			 "search_button_text",
			 "search_button_border",
			 "menu_background",
			 "menu_text",
			 "altitude_background",
			 "altitude_text"
			};

	public static int COLORS[] = new int[COLOR_COUNT];
	public static int COLORS_AT_NIGHT[] = new int[COLOR_COUNT];
	
	public final static String SOUNDNAMES[] = {
		"CONNECT", "DISCONNECT", "PREPARE", "HALF", "HARD", "BEAR", "LEFT", "RIGHT", "UTURN", "THEN", "SOON", "AGAIN", "TO",
		"ENTER_MOTORWAY", "LEAVE_MOTORWAY",	"RAB", "1ST", "2ND", "3RD", "4TH", "5TH", "6TH", "RABEXIT",
		"CHECK_DIRECTION", "ROUTE_RECALCULATION", "DEST_REACHED",
		"IN", "100", "200", "300", "400", "500", "600", "700", "800", "METERS", "YARDS", "INTO_TUNNEL", "OUT_OF_TUNNEL", "FOLLOW_STREET",
		"AREA_CROSS", "AREA_CROSSED", "SPEED_LIMIT"
	};

	/** Marker speed for maxspeed=none. 
	 * MUST be in sync with the value that GpsMid expects and 
	 * that's used in maxspeed_template.inc. */
	public static int MAXSPEED_MARKER_NONE = 250;
	/** Marker speed for maxspeed=variable/signals. 
	 * MUST be in sync with the value that GpsMid expects. */
	public static int MAXSPEED_MARKER_VARIABLE = 252;
	
	/** Maximum allowed number of bounding boxes */
	public static final int MAX_BOUND_BOXES = 9;

	public final static int TILESIZE_SMALL_MANY = 0;
	public final static int TILESIZE_MEDIUM = 1;
	public final static int TILESIZE_FEW_BIG = 2;
	
	class TileSizeDescription {
		int maxDictDepth;
		int maxTileWays[]; 
		int maxTileSize;
		int maxRouteTileSize;
		
		public TileSizeDescription(int maxDictDepth, int maxTileWays0, int maxTileWays1, int maxTileWays2, int maxTileWays3, int maxTileSize, int maxRouteTileSize) {
			this.maxDictDepth = maxDictDepth;
			this.maxTileWays = new int[4];
			this.maxTileWays[0] = maxTileWays0;
			this.maxTileWays[1] = maxTileWays1;
			this.maxTileWays[2] = maxTileWays2;
			this.maxTileWays[3] = maxTileWays3;
			this.maxTileSize = maxTileSize;
			this.maxRouteTileSize = maxRouteTileSize;
		}
	}

	public TileSizeDescription tileSize[];
	public int activeTileSizeId = 0;
	
		/** The bundle .properties file containing most settings */
		private ResourceBundle rb;

		/** The version.properties file containing default settings used as fallback */
		private ResourceBundle vb;

		/** Path of the temporary directory (which is relative to the current directory
		 * where the data is assembled before it is written to the target Midlet JAR.
		 */
		private String tempDir = null;

		/** Path name of the OSM XML file */
		private String planet;
		
		/** Path name of the file containing the cell IDs */
		private String cellSource;
		
		/** Path name of the file containing the cell IDs */
		private boolean cellIDnoLAC = false; 

		/** Whether to generate sea */
		private boolean generateSea = false; 

		/** TODO: Explain: Is this only true/false or can it have other values? */
		private String cellOperator;
		
		/** Path name of the bundle .properties file */
		private String propFile;

		/** Name to be used for the generated Midlet (as it will be shown on the phone). */
		private String midletName;

		/** Name to be used for the generated Map (as it will be shown on the phone). */
		private String mapName;

		/** Flag if zip (with no midlet) is to be built instead of a jar (as it will be shown on the phone). */
		public boolean mapzip;

		/** Name of the base Midlet (e.g. GpsMid-Generic-full-connected) to be used. */
		private String appParam;

		/** Flags if there are more way or poi styles than 126 or 255. */

		// polish.api.bigstyles	- use a short for type
		public boolean bigStyles = false;
		// 126 .. 255  - this flag is not used for anything now
		public boolean mediumStyles = false;
		// use a search format with flags in is_in count byte, starting with map format 66
		// with changing this to "false" and tweaking the map format version to 65
		// version 65 compatible maps can be produced when <= 126 style file elements
		public boolean map66search = true;

		/** Full name of the jar file of the base midlet */
		private String appJarFileName = null;

		/** Defines string to add to Manifest / .jad file*/
		public String addToManifest = "";
		
		/** Defines which routing options from the style-file get used. */
		public String useRouting = "motorcar";
		
		/** Defines if icons for icon menu are included in GpsMid or which ones, valid values: true|false|big|small */
		public String useIcons = "true";
		
		/** Defines if and what sound formats are included in GpsMid, valid values: true|false|amr, mp3, wav */
		public String useSounds = "amr";
		
		/** Defines what sound directory is included in GpsMid, e.g. sound or sounds-de */
		public String useSoundFiles = "sound";		
		
		/** Defines what languages are included in GpsMid */
		public String useLang = "en";

		/** Defines files with endings which won't be compressed */
		public String dontCompress = "";

		/** Defines if all languages will be included in GpsMid bundle */
		public boolean allLang = false;

		/** Defines what languages are included in GpsMid */
		public String useLangName = "English";
		
		/** Flag whether the generated Midlet will have editing support. */
		public boolean enableEditingSupport = false;

		/** Maximum tile size in bytes */
		public int maxTileSize = 20000;
		
		/** Maximum route tile size in bytes */
		public int maxRouteTileSize = 3000;

		/** Use or don't use url and phone tags from OSM */
		public boolean useUrlTags=false;
		public boolean usePhoneTags=false;

		/** Use or don't use house numbers for searches */
		public boolean useHouseNumbers=false;

		/** Use or don't use word search (index by words as well as whole names) */
		public boolean useWordSearch=false;

		/** TODO: Explain this, what is behind the "dict depth"? */
		private int maxDictDepth = 5;
		
		/** Max. map precision in meters */
	    	public static double mapPrecisionInMeters = 1d;

		/** Map flags */
	    	public static long mapFlags = 0L;

		/** Use sea tiles for more efficient sea generation */
	    	public boolean useSeaTiles = false;

		/** Draw debug outlines in sea generation */
	    	public boolean drawSeaOutlines = false;

		/** Maximum ways that are allowed to be stored into a tile of the zoom levels. */
		public int maxTileWays[] = new int[4];

		/** Is the source file an .apk file */
		public boolean sourceIsApk = false; 

		/** Do we want to sign the produced .apk file */
		public boolean signApk = false; 

		/** Password for jarsigner for .apk signing */
		public String signApkPassword = ""; 

		/** Do we want store areas as outlines */
		public boolean outlineAreaFormat = false; 

		/** Do we want store areas as triangles */
		public boolean triangleAreaFormat = true; 

		/** Path name of the style-file */
		public String styleFile;

		/** Directory of the style-file, with delimiter "/".
		 *  This directory is used for relative access to external media, like png and sound sub directory.
		 */
		public String styleFileDirectoryWithDelimiter;
		
		/** Bounding boxes, read from properties and/or drawn by the user on the map. */
		private final Vector<Bounds> bounds;
		
		/** Route Destinations, read from properties and/or set by the user on the map. */
		Vector<Location> routeList=new Vector<Location>();
		
		/** The parser for the style file */
		private LegendParser legend;
		
		/** The stream to read from the style file */
		private InputStream legendInputStream;
		
		/** Array containing real scale for pseudo zoom 0..32 */
		private static float realScale [] = new float[33];

		/** Singleton reference */
		private static Configuration conf;

		private Area area;


		/**
		 * @return the area
		 */
		public Area getArea() {
			return area;
		}

		public Hashtable<String, Boolean> getRelationExpansions() {
			return legend.getRelationExpansions();
		}

		public Hashtable<String, Boolean> getRelationExpansionsCombine() {
			return legend.getRelationExpansionsCombine();
		}

		/** Singleton getter */
		public static Configuration getConfiguration() {
			return conf;
		}
		
		public Configuration(String [] args) {
			//Set singleton
			conf = this;
			
			resetColors();
			
			bounds = new Vector<Bounds>(MAX_BOUND_BOXES);
			
			for (String arg : args) {
				if (arg.startsWith("--")) {
					if (arg.startsWith("--bounds=")) {
						String bound = arg.substring(9);
						System.out.println("Found bound: " + bound);
						String [] boundValues = bound.split(",");
						if (boundValues.length == 4) {
							Bounds b = new Bounds();
							try {
								b.minLon = Float.parseFloat(boundValues[0]);
								b.minLat = Float.parseFloat(boundValues[1]);
								b.maxLon = Float.parseFloat(boundValues[2]);
								b.maxLat = Float.parseFloat(boundValues[3]);
							} catch (NumberFormatException nfe) {
								System.out.println("ERROR: invalid coordinate specified in bounds");
								nfe.printStackTrace();
								System.exit(1);
							}
							bounds.add(b);
						} else {
							System.out.println("ERROR: Invalid bounds parameter, should be specified as --bounds=left,bottom,right,top");
							System.exit(1);
						}
					}
					if (arg.startsWith("--cellID=")) {
						// Filename for a list of GSM cellIDs with their coordinates.
						// This file can be obtained from http://dump.opencellid.org/cellsIdData/ (OpenCellID.org)
						cellSource = arg.substring(9);
						// TODO: Shouldn't cellOperator be set too?
					}
					if (arg.startsWith("--mapzip")) {
						// create a map zip instead of bundle jar
						mapzip = true;
					}
					if (arg.startsWith("--nogui")) {
						// makes argument length to be 2 so GUI will not be started
					}
					if (arg.startsWith("--properties=")) {
						// specify a properties file (which can specify the map file)
						propFile = arg.substring(13);
					}
					if (arg.startsWith("--map.name=")) {
						// give name to the map zip
						mapName = arg.substring(11);
					}
					if (arg.startsWith("--help")) {
						System.err.println("Usage: Osm2GpsMid [--bounds=left,bottom,right,top] [--cellID=filename] planet.osm.bz2 | planet.osm.pbf | http://address.to.planet/file.osm.pbf | properties.properties [properties] ");
						System.err.println("  \"--bounds=\" specifies the set of bounds to use in GpsMid ");
						System.err.println("       Can be left out to use the regions specified in location.properties");
						System.err.println("       or if you want to create a GpsMid for the whole region");
						System.err.println("       contained in the.osm(.bz2) file");
						System.err.println("  \"--cellID=\" specifies the file from which to load cellIDs for cell based positioning");
						System.err.println("       The data comes from OpenCellId.org and the file can be found at http://dump.opencellid.org/cellsIdData/");
						System.err.println("  \"--map.name=\" specifies the output map zip basename");
						System.err.println("  \"--mapzip\" builds a map zip named by properties midlet.name");
						System.err.println("  \"--properties=\" points to a .properties file specifying additional parameters");
						System.err.println("  \"--nogui\" don't start the GUI (to be used with --properties= if map name is specified in properties)");
						System.err.println("  planet.osm.bz2: points to a (compressed) .osm file, overrides possible .properties mapSource");
						System.err.println("       By specifying osmXapi, the data can be fetched straight from the server (only works for small areas)");
						System.err.println("  properties: points to a .properties file specifying additional parameters");
						System.exit(0);
					}
					
				} else if ((planet == null || planet.equals("")) && !arg.endsWith(".properties")) {
					planet = arg;
				} else {
					propFile = arg;
				}
			}

			initialiseRealScale();
			initialiseTileSize();
			
			try {
				InputStream cf;
				if (propFile != null) {
					try {
						System.out.println("Loading properties: " + propFile);
						if (propFile.endsWith(".properties")) {
							cf = new FileInputStream(propFile);
						} else {
							cf = new FileInputStream(propFile + ".properties");
						}
					} catch (FileNotFoundException e) {
						System.out.println(propFile + ".properties not found, trying bundled version");
						cf = getClass().getResourceAsStream("/" + propFile + ".properties");
						if (cf == null){
							throw new IOException(propFile + " is not a valid region");
						}
					}
				} else {
					// No .properties file was specified, so use the default one
					System.out.println("Loading built in default properties (version.properties)");
					cf = getClass().getResourceAsStream("/version.properties");
				}
				loadPropFile(cf);
			} catch (Exception e) {
				System.out.println("Could not load the configuration properly for conversion");
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		/**
		 * 
		 */
		public Configuration() {
			//Set singleton
			conf = this;
			resetColors();
			bounds = new Vector<Bounds>(MAX_BOUND_BOXES);
			initialiseRealScale();
			initialiseTileSize();
			resetConfig();
			planet = "TEST";
		}
		
		private void resetColors() {
			if (COLOR_COUNT != COLORNAMES.length) {
				System.out.println("WARNING: COLORNAMES.length (" + COLORNAMES.length
						+ ") does not match COLOR_COUNT (" + COLOR_COUNT + ")");
			}
			for (int i = 0; i < COLOR_COUNT; i++) {
				COLORS[i] = 0xFFFFFFFF; 			// mark that color is not specified
				COLORS_AT_NIGHT[i] = 0xFFFFFFFF;	// preset that no night color is specified
			}
		}
		
		private void initialiseRealScale() {
			// precalculate real scale levels for pseudo zoom levels
			// pseudo zoom level 0 equals to scale 0
			realScale[0] = 0;
			// startup pseudo zoom level 23
			realScale[23] = 15000;
			// pseudo zoom level 0..21
			for(int i = 22; i > 0; i--) {
				realScale[i] = realScale[i + 1] * 1.5f;
			}
			// pseudo zoom level 23..32
			for(int i = 24; i < realScale.length; i++) {
				realScale[i] = realScale[i - 1] / 1.5f;
			}
			// subtract 100 to avoid wrong bounds due to rounding errors
			for(int i = 1; i < realScale.length; i++) {
				realScale[i] -= 100;
				//System.out.println("Pseudo Zoom Level: " + i + " Real Scale: " + realScale[i]);
			}
		}
		
		private void initialiseTileSize() {
			tileSize = new TileSizeDescription[5];
			tileSize[0] = new TileSizeDescription(7, 255, 255, 255, 255, 20000, 3000);
			tileSize[1] = new TileSizeDescription(12, 1000, 1000, 1000, 1000, 20000, 6000);
			tileSize[2] = new TileSizeDescription(19, 2000, 3000, 3000, 3000, 30000, 15000);
			tileSize[3] = new TileSizeDescription(30, 2000, 5000, 5000, 5000, 40000, 25000);
			// last tileSize is custom setting from .properties
			tileSize[4] = new TileSizeDescription(7, 255, 255, 255, 255, 20000, 3000);
		}
		
		public void resetConfig() {
			try {
				System.out.println("Loading built in defaults (version.properties)");
				loadPropFile(getClass().getResourceAsStream("/version.properties"));
				bounds.removeAllElements();
				routeList.removeAllElements();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		public void loadPropFile(InputStream propIS) throws IOException {
			if (propIS == null) {
				throw new IOException("Properties file not found!");
			}
			
			// try reading .properties with UTF-8 encoding
			try {
				rb = new PropertyResourceBundle(new InputStreamReader(propIS, "UTF-8"));
			} catch (NoSuchMethodError nsme) {
				/* Give warning if creating the UTF-8-reader for .properties file fails and continue with default encoding,
				 * this can happen with Java 1.5 environments
				 * more information at: http://sourceforge.net/projects/gpsmid/forums/forum/677687/topic/4063854 
				 */
				System.out.println("Warning: Cannot use UTF-8 encoding for decoding .properties file, requires Java 1.6+");
				rb = new PropertyResourceBundle(propIS);
			}
			

			URL propertiesAddress = getClass().getResource("/version.properties");
			vb = new PropertyResourceBundle(propertiesAddress.openStream());
			
			readBounds();
			readRouteList();
			
			setRouting(getString("useRouting"));
			useUrlTags = getString("useUrlTags").equalsIgnoreCase("true");
			usePhoneTags = getString("usePhoneTags").equalsIgnoreCase("true");
			useHouseNumbers = getString("useHouseNumbers").equalsIgnoreCase("true");
			useWordSearch = getString("useWordSearch").equalsIgnoreCase("true");
			maxRouteTileSize = Integer.parseInt(getString("routing.maxTileSize"));

			setIcons(getString("useIcons"));
			if (attrToBoolean(useIcons) == 0 && !(useIcons.equals("small") || useIcons.equals("big") || useIcons.equals("large") || useIcons.equals("huge")) ) {
				System.out.println("ERROR: Invalid properties file parameter useIcons=" + getString("useIcons"));
				System.exit(1);
			}

			setAddToManifest(getString("addToManifest"));
			
			setSounds(getString("useSounds"));

			// don't override map source set by command line with one from .properties
			// also don't override one set by GUI with an empty one from properties
			if ((planet == null || planet.equals("")) && !getString("mapSource").equals("")) {
				setPlanetName(getString("mapSource"));
			}

			useSoundFiles = getString("useSoundFilesWithSyntax");

			// don't override cell source set by command line with one from .properties
			// also don't override one set by GUI with an empty one from properties
			if ((cellSource == null || cellSource.equals("")) && !getString("cellSource").equals("")) {
				setCellSource(getString("cellSource"));
			}

			setDontCompress(getString("dontCompress").toLowerCase());

			setUseLang(getString("lang"));
			if (! getString("useLang").equals("*")) {
				setUseLang(getString("useLang"));
			}
			setUseLangName(getString("langName"));
			if (! getString("useLangName").equals("All")) {
				setUseLangName(getString("useLangName"));
			}
			// default to language code for language name if not defined
			if (! getUseLang().equals("*") && getUseLangName().equals("All")) {
				useLangName = useLang;
			}

			if (useLang.indexOf(",*") > -1) {
				allLang = true;
				// * should be last, ignore it
				useLang = useLang.substring(0, useLang.indexOf(",*"));
			}
			if (useLang.indexOf("*") > -1) {
				allLang = true;
				// * should be last, ignore it
				useLang = useLang.substring(0, useLang.indexOf("*"));
			}
			// add English if not there
			//if (! (useLang.indexOf("en" ) > -1)) {
			//	useLang += ",en";
			//	useLangName += ",English";
			//}
			if (useLang.equals("")) {
				useLang = "devdefault";
				useLangName = "Device's default";
			} else {
				useLang = "devdefault," + useLang;
				useLangName = "Device's default," + useLangName;
			}
			maxTileSize = Integer.parseInt(getString("maxTileSize"));
			maxDictDepth = Integer.parseInt(getString("maxDictDepth"));
			mapPrecisionInMeters = Double.parseDouble(getString("mapPrecisionInMeters"));
			
			for (int i=0; i<=3; i++) {
				maxTileWays[i] = Integer.parseInt(getString("maxTileWays" + i));
			}
			
			tileSize[tileSize.length - 1] =  new TileSizeDescription(maxDictDepth, maxTileWays[0], maxTileWays[1], maxTileWays[2], maxTileWays[3], maxTileSize, maxRouteTileSize);		
			
			setStyleFileName(getString("style-file"));
			setCodeBase (getString("app"));
			enableEditingSupport = getString("enableEditing").equalsIgnoreCase("true");
			cellOperator = getString("useCellID");
			cellIDnoLAC = getString("cellIDnoLAC").equalsIgnoreCase("true");
			generateSea = getString("generateSea").equalsIgnoreCase("true");
			useSeaTiles = getString("useSeaTiles").equalsIgnoreCase("true");
			signApk = getString("signApk").equalsIgnoreCase("true");
			outlineAreaFormat = getString("outlineAreaFormat").equalsIgnoreCase("true");
			triangleAreaFormat = getString("triangleAreaFormat").equalsIgnoreCase("true");
			signApkPassword = getString("signApkPassword");
			drawSeaOutlines = getString("drawSeaOutlines").equalsIgnoreCase("true");
		}

		public void setPlanetName(String p) {
			planet = p;
		}

		public void setPropFileName(String p) {
			propFile = p;
		}
				
		public String getStyleFileName() {
			return styleFile;
		}

		public String getStyleFileDirectory() {
			return styleFileDirectoryWithDelimiter;
		}

		/** Sets the current name of the style-file and tries to open it.
		 * It first searches in the file system, then in the Osm2GpsMid JAR.
		 * Will also set legendInputStream to read from this file.
		 * @param name File name (may include path) of the style-file
		 */
		public void setStyleFileName(String name) throws IOException {
			styleFile = name;
			try {
				legendInputStream = new FileInputStream(styleFile);
				System.out.println("Using style file '" + styleFile + "' from file system");
			} catch (IOException e) {
				// Only add '/' once, getResource() doesn't like '//'!
				if (styleFile.startsWith("/") == false) {
					styleFile = "/" + styleFile;
				}
				//System.out.println("'" + styleFile + "' not found, searching in JAR");
				if (getClass().getResource(styleFile) != null) {
					legendInputStream = getClass().getResourceAsStream(styleFile);
					System.out.println("Using style file '" + styleFile + "' from JAR");
				} else {
					// When reading the bundle file, there is already a fallback
					// to the style-file specified in version.properties - see getString().
					// So this means we really couldn't find the file.
					legendInputStream = null;
					throw new IOException("Style file '" + styleFile
							+ "' not found in file system or Osm2GpsMid.jar!");
				}
			}
			
			/* 
			 * Determine the directory of the style-file
			 */
			// the default style-file directory is the current directory
			styleFileDirectoryWithDelimiter = "";
			File file = new File(styleFile);
			// if the style-file can be read from the file system.. 
			if (file.canRead()) {
				// ..then the style-file-directory is the parent directory of the style-file
				styleFileDirectoryWithDelimiter = file.getParent();
				// if the style-file's parent directory is not valid or it's simply a slash (linux) / back slash (windows)...
				if (styleFileDirectoryWithDelimiter == null
						|| styleFileDirectoryWithDelimiter.equalsIgnoreCase("\\")
						|| styleFileDirectoryWithDelimiter.equalsIgnoreCase("/")) {
					// ...then simply use the current directory as media directory
					styleFileDirectoryWithDelimiter = "";
				}
			}
			// make the style-file directory end on "/" (if it's not the current directory)
			if (styleFileDirectoryWithDelimiter != null && styleFileDirectoryWithDelimiter.length() > 1) {
				styleFileDirectoryWithDelimiter = styleFileDirectoryWithDelimiter.replace('\\', '/');
				styleFileDirectoryWithDelimiter = styleFileDirectoryWithDelimiter + "/";
			}
		}
		
		public void parseLegend() {
			legend = new LegendParser(legendInputStream);
		}
		
		public boolean use(String key) {
			if ("true".equalsIgnoreCase(getString(key))) {
				return true;
			} else {
				return false;
			}
		}

		/**
		 * Gets a string for the given key from the current .properties file or,
		 * if it doesn't exist there, from 'version.properties'.
		 * @param key The key to search
		 * @return The string for the given key
		 * @throws MissingResourceException if the key exists in neither of the two files
		 */
		public String getString(String key) {
			if ("region.1.lat.min".equals(key)){
				System.out.println("Try to fetch bounds");
			}
			try {
//				System.out.print("load arg.property for "+ key +" = ");
//				System.out.println(rb.getString(key));
				return rb.getString(key).trim();
			} catch (MissingResourceException e) {
//				System.out.print("load version.property for "+ key +" = ");
//				System.out.println(vb.getString(key));
				return vb.getString(key).trim();
			} catch (Exception e1){
				e1.printStackTrace();
				return null;
			}
		}

		/**
		 * Gets a number for the given key from the current .properties file or,
		 * if it doesn't exist there, from 'version.properties'.
		 * @param key The key to search
		 * @return The float number for the given key
		 * @throws MissingResourceException if the key exists in neither of the two files
		 * @throws NumberFormatException if the string for the key could not be parsed as a float
		 */
		public float getFloat(String key) {
			return Float.parseFloat(getString(key));
		}
		
		/** Allows to set the Midlet name.
		 * @param name Name to be set
		 */
		public void setMidletName(String name) {
			midletName = name;
		}
		
		/** Allows to set the Map name.
		 * @param name Name to be set
		 */
		public void setMapName(String name) {
			mapName = name;
		}
		
		/** Returns the name of the Midlet (as it will be shown on the phone).
		 * @return Name
		 */
		public String getMidletName() {
			if (midletName != null) {
				return midletName;
			}
			return getString("midlet.name");
		}
		
		/** Returns cell source file
		 * @return Name
		 */
		public String getCellSource() {
			if (cellSource != null) {
				return cellSource;
			}
			return getString("cellSource");
		}

		public boolean getCellIDnoLAC() {
			return cellIDnoLAC;
		}

		public boolean getGenerateSea() {
			return generateSea;
		}

		public boolean getUseWordSearch() {
			return useWordSearch;
		}

		public boolean getUseSeaTiles() {
			return useSeaTiles;
		}

		public boolean getSignApk() {
			return signApk;
		}

		public String getSignApkPassword() {
			return signApkPassword;
		}

		public boolean getDrawSeaOutlines() {
			return drawSeaOutlines;
		}
	
		public boolean getOutlineAreaFormat() {
			return outlineAreaFormat;
		}

		public boolean getTriangleAreaFormat() {
			return triangleAreaFormat;
		}

		/** Returns the name of the Map (as it will be shown on the phone).
		 * @return Name
		 */
		public String getMapName() {
			if (mapName != null) {
				return mapName;
			}
			if (mapzip && mapName == null) {
				return getString("midlet.name");
			} else {
				return getString("map.name");
			}
		}
		
		/** Returns the name for the Midlet files (JAR and JAD) without extension.
		 * @return File name
		 */
		public String getMidletFileName() {
			return getMidletName() + "-" + getVersion() + "-map" + MAP_FORMAT_VERSION;
		}
		
		/** Returns the name for the Map files with version and extension.
		 * @return File name
		 */
		public String getMapFileName() {
			return getMapName() + "-" + getVersion() + "-map" + MAP_FORMAT_VERSION + ".zip";
		}
		
		/** Allows to set the name of the base Midlet (e.g. GpsMid-Generic-full-connected).
		 * @param app Name of the base Midlet
		 */
		public void setCodeBase (String app) {
			if (app.equalsIgnoreCase("GpsMid-Generic-editing")) {
				app = "GpsMid-Generic-full-connected";
			}
			appParam = app;
			if (appParam.contains("android")) {
				sourceIsApk = true;
			} else {
				sourceIsApk = false;
			}
		}

		/** Allows to set parameters for tile size vscount
		 * @param 
		 */
		public void setTileSizeVsCountId (int tileSizeId) {
			activeTileSizeId = tileSizeId;
			TileSizeDescription ts = tileSize[activeTileSizeId]; 
			maxDictDepth = ts.maxDictDepth;
			maxTileWays[0] = ts.maxTileWays[0];
			maxTileWays[1] = ts.maxTileWays[1];
			maxTileWays[2] = ts.maxTileWays[2];
			maxTileWays[3] = ts.maxTileWays[3];
			maxTileSize = ts.maxTileSize;
			maxRouteTileSize = ts.maxRouteTileSize;
		}
		
		public int getTileSizeVsCountId () {
			TileSizeDescription ts;
			for(int i=0; i < tileSize.length; i++) {
				ts = tileSize[i]; 
				if (
					maxDictDepth == ts.maxDictDepth &&
					maxTileWays[0] == ts.maxTileWays[0] &&
					maxTileWays[1] == ts.maxTileWays[1] &&
					maxTileWays[2] == ts.maxTileWays[2] &&
					maxTileWays[3] == ts.maxTileWays[3] &&
					maxTileSize == ts.maxTileSize &&
					maxRouteTileSize == ts.maxRouteTileSize
				) {
					return i;
				}
			}
			return tileSize.length - 1;
		}

		
		
		/**
		 * Returns a stream to read from the JAR file containing the base Midlet -
		 * see appParam. Adds the version number and will also add the language
		 * abbreviation if necessary.
		 * On the side, it changes appJarFileName to be the name of this JAR file
		 * which is a very great idea to do in a method which is named like a simple
		 * getter.
		 * @return Stream to read from the JAR
		 */
		public InputStream getJarFile() {
			String baseName = appParam;
			if ("false".equals(baseName)) {
				return null;
			}
			baseName = "/" + appParam + "-" + getVersion() + "-map" + MAP_FORMAT_VERSION
				+ (sourceIsApk ? ".apk" : ".jar");
			InputStream is = getClass().getResourceAsStream(baseName);
			if (is == null) {
				baseName = "/" + appParam + "-" + getVersion() + "-map" + MAP_FORMAT_VERSION
					+ (sourceIsApk ? ".apk" : ".jar");
				System.out.println("Using lang=" + getUseLang() + " (" + getUseLangName() + ")");
				is = getClass().getResourceAsStream(baseName);
			}
			appJarFileName = baseName;
			return is;
		}

		/** Returns the name of the base Midlet (e.g. GpsMid-Generic-full-connected).
		 * @return Name of the base Midlet
		 */
		public String getAppParam() {
			return appParam;
		}

		/** Returns the JAR file name of base Midlet.
		 * @return JAR file name
		 */
		public String getJarFileName() {
			if (appJarFileName == null) {
				getJarFile();
			}
			return appJarFileName;
		}

		public String getTempDir() {
			return getTempBaseDir() + "/" + "map";
		}

		public String getTempBaseDir() {
			if (tempDir == null) {
				tempDir = "temp" + Math.abs(new Random(System.currentTimeMillis()).nextLong());
			}
			return tempDir;
//			return getString("tmp.dir");
		}
		
		public boolean cleanupTmpDirAfterUse() {
			if ("true".equalsIgnoreCase(getString("keepTemporaryFiles"))) {
				return false;
			} else {
				return true;
			}
		}
		
		public File getPlanet() {
			return new File(planet);
		}

		public String getPlanetName() {
			return planet;
		}

		public InputStream getPlanetSteam() throws IOException {
			InputStream fr = null;
			if (planet.equals("")) {
				System.out.println("Error: Map source not set - please set with GUI or in .properties or with command line");
				throw new IOException("Error: Map source not set");
			}
			if (planet.equalsIgnoreCase("osmxapi") || planet.equalsIgnoreCase("ROMA")) {
				if (bounds.size() > 1) {
					System.out.println("Can't deal with multiple bounds when requesting from a Server yet");
					throw new IOException("Can't handle specified bounds with online data");
				}
				Bounds bound = bounds.elementAt(0);
				URL url = null;
				if (planet.equalsIgnoreCase("osmxapi")) {
					//url = new URL("http://osmxapi.informationfreeway.org/api/0.6/*[bbox=" +
					url = new URL("http://open.mapquestapi.com/xapi/api/0.6/*[bbox=" +
							bound.minLon + "," + bound.minLat + "," + bound.maxLon + "," + bound.maxLat + "]");
				} else if (planet.equalsIgnoreCase("ROMA")){
					url = new URL("http://api1.osm.absolight.net/api/0.6/map?bbox=" +
							bound.minLon + "," + bound.minLat + "," + bound.maxLon + "," + bound.maxLat);
				}
				 
				System.out.println("Connecting to server: " + url);
				System.out.println("This may take a while!");
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setRequestProperty("User-Agent", "Osm2GpsMid");
				conn.setRequestProperty("Accept-Encoding", "gzip; deflate");
				conn.connect();
				String encoding = conn.getContentEncoding();
				System.out.println("Encoding: " + encoding);
				InputStream apiStream;
				if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
					apiStream = new GZIPInputStream(conn.getInputStream());
				} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
					apiStream = new InflaterInputStream(conn.getInputStream(), new Inflater(true));
				} else {
					apiStream = conn.getInputStream();
				}

				fr = new TeeInputStream(apiStream, new FileOutputStream(new File("Online.osm")));
			} else {
				System.out.println("Opening planet file: " + planet);
				
				if (planet.startsWith("http://")) {
					System.out.println("Opening map file from network, may take a while");
					URL url = new URL(planet);
					HttpURLConnection conn = (HttpURLConnection)url.openConnection();
					conn.setRequestProperty("User-Agent", "Osm2GpsMid");
					conn.connect();
					InputStream planetStream;
					fr = new BufferedInputStream(conn.getInputStream());
				} else {
					fr = new FileInputStream(planet);
				}
				if (planet.endsWith(".bz2") || planet.endsWith(".gz")){
					if (planet.endsWith(".bz2")) {
						fr.read();
						fr.read();
						fr = new CBZip2InputStream(fr);
					} else if (planet.endsWith(".gz")) {
						fr = new GZIPInputStream(fr);
					}
					/*int availableProcessors = Runtime.getRuntime().availableProcessors();
					if (availableProcessors > 1){
						System.out.println("Found " + availableProcessors + " CPU's: uncompress in seperate thread");
						fr = new ThreadBufferedInputStream(fr);
					} else {
						System.out.println("Only one CPU: uncompress in same thread");
					}*/
				}
				/**
				 * The BufferedInputStream isn't doing a particularly good job at buffering,
				 * so we need to use our own implementation of a BufferedInputStream.
				 * As it uses a separate thread for reading, it should paralyse any CPU intensive
				 * read operations such as decomressing bz2. However even when only reading from
				 * a named pipe and let an external program (bzCat) do the decompression, somehow
				 * still the standard BufferedInputStream does poorly and blocks a lot leaving
				 * CPUs idle.
				 */
				if (!planet.endsWith("osm.pbf")) {
					fr = new ThreadBufferedInputStream(fr);
				}
			}
			return fr;
		}
		
		public OsmParser getPlanetParser() throws IOException {
			if (planet.endsWith("osm.pbf")) {
				return new OpbfParser(getPlanetSteam(), this);
			} else {
				return new OxParser(getPlanetSteam(), this);
			}
		}
		
		public InputStream getCellStream() throws IOException {
			InputStream fr = null;
			
			System.out.println("Opening cellID file: " + cellSource);
			if (cellSource == null) {
				throw new IOException("No file for CellIDs was specified");
			}
			
			if (cellSource.startsWith("http://")) {
				System.out.println("Downloading cellid db from Opencellid.org, may take a while");
				URL url = new URL(cellSource);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
				conn.setRequestProperty("User-Agent", "Osm2GpsMid");
				conn.connect();
				InputStream cellStream;
				cellStream = new GZIPInputStream(new BufferedInputStream(conn.getInputStream()));
				fr = new TeeInputStream(cellStream, new FileOutputStream(new File("CellDB.txt")));
			} else {
				fr = new FileInputStream(cellSource);
				if (cellSource.endsWith(".bz2") || cellSource.endsWith(".gz")){
					if (cellSource.endsWith(".bz2")) {
						fr.read();
						fr.read();
						fr = new CBZip2InputStream(fr);
					} else if (cellSource.endsWith(".gz")) {
						fr = new GZIPInputStream(new BufferedInputStream(fr));
					}
				}
			}
			return fr;
		}
		
		public String getCellOperator() {
			if (cellOperator != null) {
				return cellOperator;
			} else {
				return getString("useCellID");
			}
		}
		
		public void setCellOperator(String cellOp) {
			cellOperator = cellOp;
		}
		
		public void setCellSource(String src) {
			cellSource = src;
		}

		public void setCellIDnoLAC(boolean noLAC) {
			cellIDnoLAC = noLAC;
		}

		public void setGenerateSea(boolean sea) {
			generateSea = sea;
		}

		public InputStream getCharMapStream() throws IOException{
			InputStream cmis = null;
			try {
				cmis = new FileInputStream("charMap.txt");
			} catch (FileNotFoundException e) {
				try {
					cmis = new FileInputStream(getTempDir()
								   + (sourceIsApk ? "/assets" : "")
								   + "/charMap.txt");
					if (cmis == null){
						throw new IOException("Could not find a valid charMap.txt");
					}
				} catch (FileNotFoundException fnfe) {
					throw new IOException("Could not find a valid charMap.txt");
				}
			}
			return cmis;
		}

		/** Returns the route destination that were retrieved from the current properties file.
		 * @return Vector of locations
		 */
		public Vector<Location> getRouteList() {
			return routeList;
		}

		/** Reads route destinations from the current properties file (defined by 'rb')
		 * or, theoretically from version.properties, but this doesn't contain any
		 * bounds, and puts them in the vector 'routeList'.
		 */
		public void readRouteList() {
			routeList.removeAllElements();
			int i = 0;
			try {
				while (true) {
					getFloat("routeDest." + (i + 1) + ".lat");
					getFloat("routeDest." + (i + 1) + ".lon");
					i++;
				}
			} catch (RuntimeException e) {
				;
			}
			if (i > 0) {
				System.out.println("Found " + i + " route destinations");
				for (int l = 0; l < i; l++) {
					Location location = new Location( getFloat("routeDest." + (l + 1) + ".lat"), getFloat("routeDest." + (l + 1) + ".lon") );
					addRouteDestination(location);
				}
			}
		}
		
		public void addRouteDestination(Location location) {
			routeList.add(location);
			new Route().revResolv(location);
		}


		/** Returns the bounds that were retrieved from the current properties file.
		 * @return Vector of bounds
		 */
		public Vector<Bounds> getBounds() {
			return bounds;
		}

		/** Reads bounds from the current properties file (defined by 'rb')
		 * or, theoretically from version.properties, but this doesn't contain any
		 * bounds, and puts them in the vector 'bounds'.
		 * The limit of MAX_BOUND_BOXES is considered.
		 */
		public void readBounds() {
			bounds.removeAllElements();
			int i = 0;
			try {
				while (i < MAX_BOUND_BOXES) {
					getFloat("region." + (i + 1) + ".lat.min");
					i++;
				}
			} catch (RuntimeException e) {
				;
			}

			if (i > 0) {
				System.out.println("Found " + i + " bound(s)");
				for (int l = 0; l < i; l++) {
					Bounds bound = new Bounds();
					bound.extend(getFloat("region." + (l + 1) + ".lat.min"),
							getFloat("region." + (l + 1) + ".lon.min"));
					bound.extend(getFloat("region." + (l + 1) + ".lat.max"),
							getFloat("region." + (l + 1) + ".lon.max"));
					bounds.add(bound);
				}
			} else {
				System.out.println("NOTE: No bounds were given. This will create a GpsMid");
				System.out.println("  for the whole region contained in " + planet);
			}
		}
		
		public void addBounds(Bounds bound) {
			bounds.add(bound);
		}
		
		public void removeBoundsAt(int i) {
			bounds.removeElementAt(i);
		}
		
		public void setRouting(String routing) {
			if (routing == null || attrToBoolean(routing) > 0) {
				useRouting = "motorcar, bicycle, foot";
			} else if (attrToBoolean(routing) < 0) {
				useRouting = "false";
			} else {
				useRouting = routing;
			}
		}

		public void setIcons(String icons) {
			if (attrToBoolean(icons) > 0 || icons.equalsIgnoreCase("medium")) {
				useIcons = "true";
			} else if (attrToBoolean(icons) < 0) {
				useIcons = "false";
			} else {
				useIcons = icons.toLowerCase();
			}
		}
	
		public String getUseIcons() {
			return useIcons;
		}


		public void setAddToManifest(String line) {
			addToManifest = line.trim();
		}

		public String getAddToManifest() {
			return addToManifest;
		}
		
		public void setSounds(String sounds) {
			if (attrToBoolean(sounds) > 0) {
				useSounds = "amr";
			} else if (attrToBoolean(sounds) < 0) {
				useSounds = "false";
			} else {
				useSounds = sounds.toLowerCase();
			}
		}

		public String getUseSounds() {
			return useSounds;
		}
		
		public String getSoundFiles() {
			return useSoundFiles;
		}
		
		public void setSoundFiles(String soundFiles) {
			useSoundFiles = soundFiles;
		}

		public void setDontCompress(String compressExts) {
			dontCompress = compressExts;
		}

		public String getUseLang() {
			return useLang;
		}
		
		public String getDontCompress() {
			return dontCompress;
		}
		
		public void setUseLang(String lang) {
			useLang = lang;
		}

		public void setAllLang(boolean flag) {
			allLang = flag;
		}

		public String getUseLangName() {
			return useLangName;
		}
		
		public void setUseLangName(String langName) {
			useLangName = langName;
		}

		/**
		 * Returns the application version as specified in version.properties.
		 * @return Version
		 */
		public String getVersion() {
			return vb.getString("version");
		}

		public String getBundleDate() {
	        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
	        return format.format(new Date());
		}

		public static String memoryWithUnit(long memoryInBytes) {
			String unit = "bytes";
			if (memoryInBytes > 1024) {
				memoryInBytes /= 1024;
				unit = "kB";
			}
			if (memoryInBytes > 1024) {
				memoryInBytes /= 1024;
				unit = "MB";
			}
			return "" + memoryInBytes + " " + unit;
		}
		
		public int getMaxTileSize() {
			return maxTileSize;
		}

		public int getMaxDictDepth() {
			return maxDictDepth;
		}
		
		public int getMaxTileWays(int zl) {
			return maxTileWays[zl];
		}
		
		public int getMaxRouteTileSize() {
			return maxRouteTileSize;
		}

		public Hashtable<String, Hashtable<String,Set<EntityDescription>>> getPOIlegend() {
			return legend.getPOIlegend();
		}
		
		public Hashtable<String, Hashtable<String,Set<EntityDescription>>> getWayLegend() {
			return legend.getWayLegend();
		}
		
		public POIdescription getpoiDesc(short t) {
			return legend.getPOIDesc(t);
		}
		
		// polish.api.bigstyles
		public WayDescription getWayDesc(short t) {
			return legend.getWayDesc(t);
		}
		
		public Collection<EntityDescription> getPOIDescs() {
			return legend.getPOIDescs();
		}

		public Collection<EntityDescription> getWayDescs() {
			return legend.getWayDescs();
		}

		public int getMaxspeedTemplate(String template) throws Exception {
			Integer maxspeed = legend.getMaxspeedTemplates().get(template);
			if (maxspeed == null) {
				throw new Exception();
			} else {
				return maxspeed.intValue();
			}
		}

		/**
		 * Returns the real scale level
		 * 
		 * For scale 0..32 a pseudo zoom level is assumed
		 * and it is converted to a real scale level.
		 */
		public int getRealScale(int scale) {
			if (scale < realScale.length) {
				return (int) realScale[scale];
			} else {
				return scale;
			}
		}
		
		/**
		 * Canonicalises textual boolean values e.g. 'yes', 'true' and '1'.
		 * 
		 * @param attr A string to be canonicalised
		 * @return 1 if it is a valid true, -1 if it is a valid false and 0 otherwise
		 */
		public static int attrToBoolean(String attr) {
			if (attr == null) {
				return 0;
			}
			if (attr.equalsIgnoreCase("yes")) {
				return 1;
			}
			if (attr.equalsIgnoreCase("true")) {
				return 1;
			}
			if (attr.equalsIgnoreCase("1")) {
				return 1;
			}
			if (attr.equalsIgnoreCase("no")) {
				return -1;
			}
			if (attr.equalsIgnoreCase("false")) {
				return -1;
			}
			if (attr.equalsIgnoreCase("0")) {
				return -1;
			}
			return 0;
		}
		
		@Override
		public String toString() {
			String confString = "Osm2GpsMid configuration:\n";
			if (getMapName().equals("")) {
				confString += "  Midlet name: " + getMidletName() + "\n";
			} else {
				confString += "  Map name: " + getMapName() + "\n";
				confString += "  Map file name: " + getMapFileName() + "\n";
			}
			confString += "  Code base: " + appParam + "\n";
			if (addToManifest.length() > 0) {
				confString += "  Add line to manifest: " + addToManifest + "\n";				
			}
			confString += "  Keeping map files after .jar creation: " + !cleanupTmpDirAfterUse() + "\n";
			confString += "  Enable routing: " + useRouting + "\n";
			confString += "  Include icons: " + useIcons + "\n";
			confString += "  Include sound format(s): " + useSounds + "\n";
			confString += "  Style-file: " + getStyleFileName() + "\n";
			confString += "  Planet source: " + planet + "\n";
			confString += "  Included CellID data: " + getCellOperator() + "\n";
			confString += "  CellID source: " + cellSource + "\n";
			confString += "  Include CellID data for phones with no LAC: " + cellIDnoLAC + "\n";
			confString += "  Generate sea: " + generateSea + "\n";
			confString += "  Use url tags: " + useUrlTags + "\n";
			confString += "  Use phone tags: " + usePhoneTags + "\n";
			confString += "  Use house numbers for search: " + useHouseNumbers + "\n";
			confString += "  Use words for search: " + useWordSearch + "\n";
			confString += "  Enable editing support: " + enableEditingSupport + "\n";
			confString += "  Map precision in meters: " + mapPrecisionInMeters + "\n";
			confString += "  triangleAreaFormat: " + triangleAreaFormat + "\n";
			confString += "  outlineAreaFormat: " + outlineAreaFormat + "\n";
			confString += "  Adding menu entries for languages: " + getUseLang() + " (" + getUseLangName() + ")" + "\n";
			confString += "  Don't compress files ending with: " + getDontCompress() + "\n";
			if (allLang) {
				confString += "  Including also all other supported languages\n";
			}

			if (bounds.size() > 0) {
				confString += "  Using " + bounds.size() + " bounding boxes\n";
				for (Bounds b : bounds) {
					confString += "    " + b + "\n";
				}
			} else {
				confString += "  Using the complete osm file\n";
			}
			confString += "  Limits for tiles:\n";
			confString += "   maximum size: " + maxTileSize + "  max. route tile size: " + maxRouteTileSize + "  max. dict depth: " + maxDictDepth + "\n";
			confString += "   maximum ways for level";
			for (int i=0;i < 4; i++) {
				confString += " " + i + ": " + maxTileWays[i] + " ";
			}
			confString += "\n";
						
			return confString;
		}

		/**
		 * @param a
		 */
		public void setArea(Area area) {
			this.area = area;
			// TODO Auto-generated method stub
			
		}
}
