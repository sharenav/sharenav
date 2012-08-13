/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.data;

import javax.microedition.lcdui.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Image;
import javax.microedition.media.Manager;

import de.ueller.gpsmid.mapdata.PoiDescription;
import de.ueller.gpsmid.mapdata.WayDescription;
import de.ueller.gpsmid.routing.TravelMode;
import de.ueller.gpsmid.ui.GpsMid;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;

import de.enough.polish.util.Locale;

public class Legend {
	/**
	 * Specifies the format of the map on disk we expect to see
	 * This constant must be in sync with Osm2GpsMid
	 */
	public final static short MAP_FORMAT_VERSION = 72;
	
	/** The waypoint format used in the RecordStore. See PositionMark.java. */
	public final static short WAYPT_FORMAT_VERSION = 2;

	public final static byte MIN_PLACETYPE = 1; // city
	public final static byte MAX_PLACETYPE = 5; // suburb
	
	//public final static byte NODE_MASK_ROUTENODELINK = 0x1; //obsolete
	public final static byte NODE_MASK_TYPE = 0x2;
	public final static byte NODE_MASK_NAME = 0x4;
	public final static byte NODE_MASK_ROUTENODE = 0x8;
	public final static byte NODE_MASK_NAMEHIGH = 0x10;
	public final static byte NODE_MASK_URL=0x20;
	public final static byte NODE_MASK_URLHIGH=0x40;
	public final static int NODE_MASK_ADDITIONALFLAG=0x80;
	public final static byte NODE_MASK2_PHONE=0x1;
	public final static byte NODE_MASK2_PHONEHIGH=0x2;
	
	public final static int LEGEND_MAPFLAG_OUTLINE_AREA_BLOCK = 0x01;
	public final static int LEGEND_MAPFLAG_TRIANGLE_AREA_BLOCK = 0x02;
	public final static int LEGEND_MAPFLAG_WORDSEARCH = 0x04;
	public final static int LEGEND_MAPFLAG_SOURCE_OSM_CC_BY_SA = 0x08;
	public final static int LEGEND_MAPFLAG_SOURCE_OSM_ODBL = 0x10;
	public final static int LEGEND_MAPFLAG_SOURCE_FI_LANDSURVEY = 0x20;
	public final static int LEGEND_MAPFLAG_SOURCE_FI_DIGIROAD = 0x40;

	public final static byte LEGEND_FLAG_IMAGE = 0x01;
	public final static byte LEGEND_FLAG_SEARCH_IMAGE = 0x02;
	public final static byte LEGEND_FLAG_MIN_IMAGE_SCALE = 0x04;
	public final static byte LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE = LEGEND_FLAG_MIN_IMAGE_SCALE;
	public final static byte LEGEND_FLAG_TEXT_COLOR = 0x08;
	public final static byte LEGEND_FLAG_NON_HIDEABLE = 0x10;
	// public final static byte LEGEND_FLAG_NON_ROUTABLE = 0x20;
	public final static byte LEGEND_FLAG_ALERT = 0x20;
	public final static byte LEGEND_FLAG_MIN_DESCRIPTION_SCALE = 0x40;
	public final static int LEGEND_FLAG_ADDITIONALFLAG = 0x80;

	public final static int LEGEND_FLAG2_CLICKABLE = 0x01;
	
	public final static short ROUTE_FLAG_MOTORWAY = 0x01;  // used in ConnectionWithNode AND WayDescription
	public final static short ROUTE_FLAG_MOTORWAY_LINK = 0x02; // used in ConnectionWithNode AND WayDescription
	public final static short ROUTE_FLAG_ROUNDABOUT = 0x04; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_TUNNEL = 0x08; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_BRIDGE = 0x10; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_INVISIBLE = 0x20; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_INCONSISTENT_BEARING = 0x40; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_QUIET = 0x80; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_LEADS_TO_MULTIPLE_SAME_NAMED_WAYS = 0x100; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_BEAR_LEFT = 0x200; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_BEAR_RIGHT = 0x400; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_ONEDIRECTION_ONLY = 0x800; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_AREA = 0x1000; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_VERY_SMALL_DISTANCE = 0x2000; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_TOLLROAD = 0x4000; // used in ConnectionWithNode
	
	/**
	 * minimum distances to set the is_in name to the next city
	 * to get the minimum distance use: <code>MAX_DIST_CITY[node.getType(null)]</code>
	 */

	public final static byte NAME_CITY = 1;
	public final static byte NAME_SUBURB = 2;
	public final static byte NAME_STREET = 3;
	public final static byte NAME_AMENITY = 4;
	
	public final static byte OM_HIDE = 0;
	public final static byte OM_SHOWNORMAL = 1;
	public final static byte OM_OVERVIEW = 2;
	// OM_OVERVIEW2 is used to set overview mode for the element without setting the checkbox in the UI.
	public final static byte OM_OVERVIEW2 = 4;
	public final static byte OM_MODE_MASK = OM_SHOWNORMAL | OM_OVERVIEW | OM_OVERVIEW2;
	
	public final static byte OM_NAME_ALL = 0;
	public final static byte OM_NO_NAME = 8;
	public final static byte OM_WITH_NAME = 16;
	public final static byte OM_WITH_NAMEPART = 32;
	public final static byte OM_NAME_MASK = OM_NO_NAME | OM_WITH_NAME | OM_WITH_NAMEPART;
	

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
	public static int COLOR_COUNT = 89;
	
	public static int COLORS[] = new int[COLOR_COUNT];
	
	/** Marker speed for maxspeed=none. 
	 * MUST be in sync with the value that Osm2GpsMid writes. */
	public static int MAXSPEED_MARKER_NONE = 250;
	/** Marker speed for maxspeed=variable/signals. 
	 * MUST be in sync with the value that Osm2GpsMid writes. */
	public static int MAXSPEED_MARKER_VARIABLE = 252;
	/** Estimated actual speed for route calculation for maxspeed=none.
	 * One reference is here: http://www.spiegel.de/spiegel/spiegelspecial/d-9157556.html
	 * About 130 km/h seems plausible for many situations because of the traffic density.
	 */
	public static int MAXSPEED_ESTIMATE_NONE = 130;
	/** Estimated actual speed for route calculation for maxspeed=variable/signals.
	 * As variable signals are usually found on roads with a lot of traffic,
	 * 100 km/h should be a good value.
	 */
	public static int MAXSPEED_ESTIMATE_VARIABLE = 100;

	public static String soundFormats[];
	public static String soundDirectories[];
	
	public static float mapPrecision;

	public static long mapFlags;

	public static String mapVersion;
	public static String bundleDate;
	public static boolean isValid = false;
	public static boolean enableEdits;
	public static boolean enableUrlTags;
	public static boolean enablePhoneTags;
	public static boolean enableBigStyles;
	public static boolean enableMap66Search;
	public static boolean enableMap67Sounds;
	public static boolean enableMap68Filenames;
	public static boolean enableMap69Precision;
	public static boolean enableMap70ConnTravelModes_Additional_Byte;
	public static boolean enableMap72MapFlags;
	public static short numUiLang;
	public static short numNaviLang;
	public static short numOnlineLang;
	public static short numWikipediaLang;
	public static short numNamesOnMapLang;
	public static String[] uiLang;
	public static String[] uiLangName;
	public static String[] naviLang;
	public static String[] naviLangName;
	public static String[] onlineLang;
	public static String[] onlineLangName;
	public static String[] wikipediaLang;
	public static String[] wikipediaLangName;
	public static String[] namesOnMapLang;
	public static String[] namesOnMapLangName;
	
	private static PoiDescription[] pois;
	private static WayDescription[] ways;
	
	/** name parts for Overview/Filter mode (index 0 for poi name part, index 1 for area name part, index 2 for way name part) */
	private static String namePartRequired[] = {"", "", ""};
	
	public static int tileScaleLevel[] = { Integer.MAX_VALUE, 900000, 180000, 45000 }; 
	public static boolean tileScaleLevelContainsRoutableWays[] = {true, true, true, true};
	public static int lowestTileScaleLevelWithRoutableWays; 

	
	private static TravelMode midletTravelModes[];	

	private final static Logger logger = Logger.getInstance(Legend.class, Logger.TRACE);
	
	//#if polish.api.finland
	private static int cameraType = 0;
	//#endif

	public static void reReadLegend() {
		try {
			Legend.readLegend();
		} catch (Exception e) {
			logger.fatal(Locale.get("legend.FailedRereadLegend")/*Failed to reread legend*/);
		}	
	}
	
	public static boolean getMapFlag(long mask) {
		return (mapFlags & mask) != 0;
	}
	
	public static void readLegend() throws IOException {
		COLOR_COUNT = 89;
		enableEdits = false;
		enableBigStyles = false;
		enableMap66Search = false;
		enableMap67Sounds = false;
		enableMap68Filenames = false;
		enableMap69Precision = false;
		enableMap70ConnTravelModes_Additional_Byte = false;
		enableMap72MapFlags = false;
		// map 71: added optional additional bytes for way & POI type flags for alert & clickable
		isValid = false;
		/* Set some essential default colors in case legend.dat can not be opened/read.
		 * Without this, it wouldn't be possible to configure external map usage 
		 * (which contains legend.dat with the colors) when using icon menus.  
		 */
		COLORS[COLOR_MAP_BACKGROUND] = 0x002020FF;
		COLORS[COLOR_WAYNAME_BACKGROUND] = 0x00FFFFFF;
		COLORS[COLOR_ICONMENU_ICON_BORDER_HIGHLIGHT] = 0x00FF0000;
		COLORS[COLOR_ICONMENU_ICON_TEXT] = 0x00FFFFFF;
		COLORS[COLOR_ICONMENU_TABBUTTON_BORDER] = 0x00707070;
		COLORS[COLOR_ICONMENU_TABBUTTON_TEXT] = 0x00FFFFFF;
		COLORS[COLOR_ICONMENU_TABBUTTON_TEXT_HIGHLIGHT] = 0x00FFFF00;
		COLORS[COLOR_ICONMENU_TABBUTTON_TEXT_INACTIVE] = 0x00808080;
		COLORS[COLOR_SEARCH_BACKGROUND] = 0x00FFFFFF;
		COLORS[COLOR_SEARCH_SELECTED_REST] = 0x00FF0000;
		
		InputStream is = Configuration.getMapResource("/legend.dat");

		if (is == null) {
			logger.error(Locale.get("legend.FailedOpeningLegend")/*Failed to open the legend file*/);
			return;			
		}
	
		DataInputStream ds = new DataInputStream(is);
		
		/**
		 * Check to see if we have the right version of the Map format
		 */
		short mapFormatVersion = ds.readShort();
		if (mapFormatVersion >= 66) {
			enableMap66Search = true;
		}
		if (mapFormatVersion >= 67) {
			enableMap67Sounds = true;
		}
		if (mapFormatVersion >= 68) {
			enableMap68Filenames = true;
		}
		if (mapFormatVersion >= 69) {
			enableMap69Precision = true;
		}
		if (mapFormatVersion >= 70) {
			enableMap70ConnTravelModes_Additional_Byte = true;
		}
		if (mapFormatVersion >= 72) {
			enableMap72MapFlags = true;
		}
		// we can read old versions
		if (mapFormatVersion != MAP_FORMAT_VERSION && mapFormatVersion != 65
		    && mapFormatVersion != 66
		    && mapFormatVersion != 67
		    && mapFormatVersion != 68
		    && mapFormatVersion != 69
		    && mapFormatVersion != 70
		    && mapFormatVersion != 71
		) {
		        Trace.getInstance().alert(Locale.get("legend.wrongmapvertitle"),
				     Locale.get("legend.wrongmapvermsg1") + " " + MAP_FORMAT_VERSION
				     + " " + Locale.get("legend.wrongmapvermsg2") + " " + mapFormatVersion,
				     Alert.FOREVER);
		}
		
		mapVersion = ds.readUTF();
		bundleDate = ds.readUTF();
		enableEdits = ds.readBoolean();
		numUiLang = ds.readShort();
		uiLang = new String[numUiLang];
		uiLangName = new String[numUiLang];
		for (int i = 0; i < numUiLang; i++) {
			uiLang[i] = ds.readUTF();
			uiLangName[i] = ds.readUTF();
		}
		numNaviLang = ds.readShort();
		naviLang = new String[numNaviLang];
		naviLangName = new String[numNaviLang];
		for (int i = 0; i < numNaviLang; i++) {
			naviLang[i] = ds.readUTF();
			naviLangName[i] = ds.readUTF();
		}
		numOnlineLang = ds.readShort();
		onlineLang = new String[numOnlineLang];
		onlineLangName = new String[numOnlineLang];
		for (int i = 0; i < numOnlineLang; i++) {
			onlineLang[i] = ds.readUTF();
			onlineLangName[i] = ds.readUTF();
		}
		numWikipediaLang = ds.readShort();
		wikipediaLang = new String[numWikipediaLang];
		wikipediaLangName = new String[numWikipediaLang];
		for (int i = 0; i < numWikipediaLang; i++) {
			wikipediaLang[i] = ds.readUTF();
			wikipediaLangName[i] = ds.readUTF();
		}
		numNamesOnMapLang = ds.readShort();
		namesOnMapLang = new String[numNamesOnMapLang];
		namesOnMapLangName = new String[numNamesOnMapLang];
		for (int i = 0; i < numNamesOnMapLang; i++) {
			namesOnMapLang[i] = ds.readUTF();
			namesOnMapLangName[i] = ds.readUTF();
		}

		enableUrlTags = ds.readBoolean();
		enablePhoneTags = ds.readBoolean();

		//#if polish.api.osm-editing

		//#else
		//if (enableEdits) {
		//	throw new IOException("The Map files are enabled for editing, but editing is not compiled into GpsMid." +
		//			"Please use the correct Osm2GpsMid settings to recreate the map or use a correct GpsMid to enable editing");
		//}
		//#endif
		
		/*
		 * Read colors
		 */
		int count = ds.readShort();

		if (mapFormatVersion < 67) {
			COLOR_COUNT = 87;
		}

		if (count != COLOR_COUNT) {
			throw new IOException("Map file contains " + count + "colors but midlet's COLOR_COUNT is " + COLOR_COUNT);
		}
		for (int i = 0; i < COLOR_COUNT; i++) {
			COLORS[i] = readDayOrNightColor(ds);
		}
		
		if (mapFormatVersion < 67) {
			COLORS[COLOR_ALTITUDE_BACKGROUND] = COLORS[COLOR_SOLUTION_BACKGROUND];
			COLORS[COLOR_ALTITUDE_TEXT] = COLORS[COLOR_SOLUTION_TEXT];
		}

		/*
		 * Read Tile Scale Levels
		 */
		for (int i = 0; i < 4; i++) {
			tileScaleLevel[i] = ds.readInt();
			if (tileScaleLevel[i] < 0) {
				tileScaleLevel[i] = - tileScaleLevel[i];
				tileScaleLevelContainsRoutableWays[i] = false;
			}
			if (tileScaleLevelContainsRoutableWays[i]) {
				lowestTileScaleLevelWithRoutableWays = tileScaleLevel[i];
			}
		}

		
		/*
		 * Read Travel Modes
		 */
		count = ds.readByte();
		midletTravelModes = new TravelMode[count];
		for (int i = 0; i < count; i++) {
			midletTravelModes[i] = new TravelMode();
			midletTravelModes[i].travelModeName = ds.readUTF();
			midletTravelModes[i].maxPrepareMeters = ds.readShort();
			midletTravelModes[i].maxInMeters = ds.readShort();
			midletTravelModes[i].maxEstimationSpeed = ds.readShort();
			midletTravelModes[i].travelModeFlags = ds.readByte();
		}
		
		// If we do not have the travel mode stored defined in the record store in the midlet data, use the first one 
		if (Configuration.getTravelModeNr() > Legend.getTravelModes().length-1) {
			Configuration.setTravelMode(0);
		}
		
		readPOIdescriptions(ds);
		readWayDescriptions(ds);

		/*
		 * Read sound formats
		 */
		count = ds.readByte();
		String soundFormatsToTry[] = new String[count];
		for (int i = 0; i < count; i++) {
			soundFormatsToTry[i] = ds.readUTF();
		}

		soundFormats = new String[count];
                String supportedFormat[] = Manager.getSupportedContentTypes(null);
                int supportedFormatsLast = 0;
                for (int i = 0; i < supportedFormat.length; i++) {
                	for (int t = 0; t < count; t++) {
                        	String mediaContentType = "";
                                if (soundFormatsToTry[t].equals("wav") ) {
                                	mediaContentType = "audio/x-wav";
                                } else if (soundFormatsToTry[t].equals("mp3") ) {
                                        mediaContentType = "audio/mpeg";
                                } else if (soundFormatsToTry[t].equals("amr") ) {
                                        mediaContentType = "audio/amr";
                                } else if (soundFormatsToTry[t].equals("ogg") ) {
                                        mediaContentType = "audio/x-ogg";
                                }
                                // for best work place SUPPORTED extensions at begining of
                                // 'must-to-use' list
                                if (!mediaContentType.equals("") && supportedFormat[i] == mediaContentType) {
                                        soundFormats[supportedFormatsLast++] = soundFormatsToTry[t];
                                        soundFormatsToTry[t] = "";
                                        break;
                                }
                        }
                }
                // finally, insert unsupported (by phone) extensions at the end of 'must-to-use'
                // list, just for try to play
		for (int i = 0; i < count; i++) {
                        if (soundFormatsToTry[i] != "") {
                                soundFormats[supportedFormatsLast++] = soundFormatsToTry[i];
                        }
		}

		/*
		 * Read sound directories
		 */
		count = ds.readByte();
		soundDirectories = new String[count];
		for (int i = 0; i < count; i++) {
			soundDirectories[i] = ds.readUTF();
		}

		isValid = true;

		mapPrecision = 1f;
		if (enableMap69Precision) {
			mapPrecision = ds.readFloat();
			MoreMath.setFIXPTValues();
		}
		if (enableMap72MapFlags) {
			mapFlags = ds.readLong();
		}
		ds.close();

		if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_TRIANGLE_AREA_BLOCK) == false &&
		    Legend.getMapFlag(Legend.LEGEND_MAPFLAG_OUTLINE_AREA_BLOCK)) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS)) {
				// use outline format if triangle not available
				// FIXME warn user
				Configuration.setCfgBitState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS, true, false);
			}
		}

		//#if polish.android
		//#if polish.api.areaoutlines
		//#else
		// If we don't have area outlines, fall back to triangles
		if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_TRIANGLE_AREA_BLOCK) &&
		    Legend.getMapFlag(Legend.LEGEND_MAPFLAG_OUTLINE_AREA_BLOCK)) {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS)) {
				// use outline format if triangle not available
				// FIXME warn user
				Configuration.setCfgBitState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS, false, false);
			}
		}
		//#endif
		//#endif
	}
	
	private static void readPOIdescriptions(DataInputStream ds) throws IOException {		
		Image generic = Image.createImage("/unknown.png");
		byte poiDescNumByte = ds.readByte();
		short numPoiDesc = 0;
		// 0 marks we have a short for styles (for backwards compatibility)
		if (poiDescNumByte == 0) {
			enableBigStyles = true;
			numPoiDesc = ds.readShort();
			//#if polish.api.bigstyles
			//#else
			// will have trouble handling bigger number of styles, try anyway for some fail-safety
		        Trace.getInstance().alert(Locale.get("legend.bigstyleserrtitle"),
						  Locale.get("legend.bigstyleserr"),
						  Alert.FOREVER);
			logger.error(Locale.get("legend.ReadLegendPOIErr")/*Read legend had trouble reading POI descriptions*/);
			//#endif
		}
		if (poiDescNumByte != 0) {
			numPoiDesc = (short) (poiDescNumByte & 0xff);
		}
		//#debug info
		logger.info("Reading " + (numPoiDesc - 1) + " POI descriptions (+ 1 bogus) from legend.dat");
		pois = new PoiDescription[numPoiDesc];
		for (int i = 0; i < pois.length; i++) {
			pois[i] = new PoiDescription();
			if (enableBigStyles) {
				if ((ds.readShort() & 0xffff) != i) {
					logger.error(Locale.get("legend.ReadLegendPOIErr")/*Read legend had trouble reading POI descriptions*/);
				}
			} else {
				if ((ds.readByte() & 0xff) != i) {
					logger.error(Locale.get("legend.ReadLegendPOIErr")/*Read legend had trouble reading POI descriptions*/);
				}
			}
			byte flags = ds.readByte();
			byte flags2 = 0;
			if ((flags & LEGEND_FLAG_ADDITIONALFLAG) > 0) {
				flags2 = ds.readByte();
			}
			pois[i].description = ds.readUTF();
			// for POI type searches
			if (i == 0) {
				pois[i].description = Locale.get("guipoitypeselectmenu.Everything")/*Everything*/;
			}
			//logger.debug("POI: " +  pois[i].description);
			pois[i].imageCenteredOnNode = ds.readBoolean();
			pois[i].maxImageScale = ds.readInt();
			pois[i].hideable = ((flags & LEGEND_FLAG_NON_HIDEABLE) == 0);	
			pois[i].alert = ((flags & LEGEND_FLAG_ALERT) != 0);	
			pois[i].clickable = ((flags2 & LEGEND_FLAG2_CLICKABLE) != 0);
			if ((flags & LEGEND_FLAG_IMAGE) > 0) {
				String imageName = ds.readUTF();
				//logger.debug("Trying to open image " + imageName);
				try {
					pois[i].image = Image.createImage(Configuration.getMapResource(imageName));
				} catch (IOException e) {
					//#debug error
					logger.error(Locale.get("legend.CouldNotOpenPOI")/*Could not open POI icon */ + imageName + Locale.get("legend.for")/* for */ + pois[i].description);
					pois[i].image = generic;
				}				
				//#if polish.api.finland
				// FIXME perhaps make this style-file-configurable with a field "camera"
				if ("/speed_trap.png".equals(imageName)) {
					cameraType = i;
				}
				//#endif
			}
			if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {
				String imageName = ds.readUTF();
				//logger.debug("Trying to open search image " + imageName);
				try {
					pois[i].searchIcon = Image.createImage(Configuration.getMapResource(imageName));
				} catch (IOException e) {
					//#debug error
					logger.error(Locale.get("legend.SearchIconOpenFail")/*Could not open search icon */ + imageName + Locale.get("legend.for")/* for */ + pois[i].description);
					pois[i].searchIcon = generic;
				}				
			} else if (pois[i].image != null) {
				pois[i].searchIcon = pois[i].image;
			}
			if ((flags & LEGEND_FLAG_MIN_IMAGE_SCALE) > 0) {
				pois[i].maxTextScale = ds.readInt();
			} else {
				pois[i].maxTextScale = pois[i].maxImageScale;
			}
			if ((flags & LEGEND_FLAG_TEXT_COLOR) > 0) {			
				pois[i].textColor = ds.readInt();
			} else {
				pois[i].textColor = COLORS[COLOR_POI_LABEL_TEXT];
			}
			pois[i].overviewMode = OM_SHOWNORMAL;
			//#if polish.api.osm-editing
			if (enableEdits) {
				int noKVpairs = ds.readShort();
				pois[i].osmTags = new String[noKVpairs * 2];
				for (int j = 0; j < noKVpairs * 2; j++) {
					pois[i].osmTags[j] =  ds.readUTF();
				}
			}
			//#else
			if (enableEdits) {
				int noKVpairs = ds.readShort();
				for (int j = 0; j < noKVpairs * 2; j++) {
					String x = ds.readUTF();
				}
			}
			//#endif
		}
	}

	//#if polish.api.finland
	public static boolean isCamera(short type) {
		return type == cameraType;
	}
	//#endif

	public static final PoiDescription[] getPoiDescriptions() {
		return pois;
	}

	private static int readDayOrNightColor(DataInputStream ds) throws IOException {
		int color = ds.readInt();
		int colorNight = color;
		if ( (color & 0x01000000) > 0) {
			colorNight = ds.readInt();
		}
		if (Configuration.getCfgBitState(Configuration.CFGBIT_NIGHT_MODE)) {
			color = colorNight;
		}
		return color & 0x00FFFFFF;
	}
	
	public static boolean getLegendMapFlag(long mask) {
		return (mapFlags & mask) == mask;
	}

	private static void readWayDescriptions(DataInputStream ds) throws IOException {
		Image generic = Image.createImage("/unknown.png");
		byte wayDescNumByte = ds.readByte();
		short numWayDesc = 0;
		// 0 marks we have a short for styles (for backwards compatibility)
		if (wayDescNumByte == 0) {
			enableBigStyles = true;
			numWayDesc = ds.readShort();
			//#if polish.api.bigstyles
			//#else
			// can't handle bigger number of styles, try anyway for some fail-safety
			logger.error(Locale.get("legend.ReadLegendPOIErr")/*Read legend had trouble reading POI descriptions*/);
			//#endif
		}
		if (wayDescNumByte != 0) {
			numWayDesc = (short) (wayDescNumByte & 0xff);
		}
		//#debug info
		logger.info("Reading " + (numWayDesc - 1) + " way descriptions (+ 1 bogus) from legend.dat");
		ways = new WayDescription[numWayDesc];		
		for (int i = 0; i < ways.length; i++) {
			ways[i] = new WayDescription();
			if (enableBigStyles) {
				if ((ds.readShort() & 0xffff) != i) {
					logger.error(Locale.get("legend.ReadLegendWayErr")/*Read legend had trouble reading way descriptions*/);
				}
			} else {
				if ((ds.readByte() & 0xff) != i) {
					logger.error(Locale.get("legend.ReadLegendWayErr")/*Read legend had trouble reading way descriptions*/);
				}
			}
			byte flags = ds.readByte();
			byte flags2 = 0;
			if ((flags & LEGEND_FLAG_ADDITIONALFLAG) > 0) {
				flags2 = ds.readByte();
			}
			ways[i].hideable = ((flags & LEGEND_FLAG_NON_HIDEABLE) == 0);
			ways[i].routeFlags = ds.readByte();			
			ways[i].description = ds.readUTF();
			ways[i].maxScale = ds.readInt();
			ways[i].maxTextScale = ds.readInt();
			ways[i].alert = ((flags & LEGEND_FLAG_ALERT) != 0);
			ways[i].clickable = ((flags2 & LEGEND_FLAG2_CLICKABLE) != 0);
			if ((flags & LEGEND_FLAG_IMAGE) > 0) {
				String imageName = ds.readUTF();
				//logger.debug("Trying to open image " + imageName);
				try {
					ways[i].image = Image.createImage(Configuration.getMapResource(imageName));
				} catch (IOException e) {
					//#debug error
					logger.error("Could not open way icon " + imageName + " for " + ways[i].description);
					ways[i].image = generic;
				}				
			}
			if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {
				String imageName = ds.readUTF();
				//logger.debug("Trying to open search image " + imageName);
				try {
					ways[i].searchIcon = Image.createImage(Configuration.getMapResource(imageName));
				} catch (IOException e) {
					//#debug error
					logger.error("Could not open search icon " + imageName + " for " + ways[i].description);
					ways[i].searchIcon = generic;
				}				
			} else if (ways[i].image != null) {
				ways[i].searchIcon = ways[i].image;
			}
			ways[i].isArea = ds.readBoolean();
			ways[i].lineColor = readDayOrNightColor(ds);
			ways[i].boardedColor = readDayOrNightColor(ds);
			ways[i].wayWidth = ds.readByte();
			ways[i].overviewMode = OM_SHOWNORMAL;
			ways[i].wayDescFlags = ds.readInt();
			if (ways[i].searchIcon != null) {
				ways[i].wayDescFlags |= WayDescription.WDFLAG_SEARCHICON_FROM_FILE;
			}

			if ((flags & LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE) > 0) {
				ways[i].maxOnewayArrowScale = ds.readInt();
			} else {
				ways[i].maxOnewayArrowScale = ways[i].maxScale; 
			}
			if ((flags & LEGEND_FLAG_MIN_DESCRIPTION_SCALE) > 0) {
				ways[i].maxDescriptionScale = ds.readInt();
			} else {
				ways[i].maxDescriptionScale = (int) Configuration.getRealBaseScale();
			}
			//#if polish.api.osm-editing
			if (enableEdits) {
				int noKVpairs = ds.readShort();
				ways[i].osmTags = new String[noKVpairs * 2];
				for (int j = 0; j < noKVpairs * 2; j++) {
					ways[i].osmTags[j] = ds.readUTF();
				}
			}
			//#else
			if (enableEdits) {
				int noKVpairs = ds.readShort();
				for (int j = 0; j < noKVpairs * 2; j++) {
					String x = ds.readUTF();
				}
			}
			//#endif
		}
	}	
	
	//#if polish.api.osm-editing
	public static final String[] getNodeOsmTags(short type) {
		return pois[type].osmTags;
	}
	//#endif
	
	public static final int getNodeTextColor(short type) {
		return pois[type].textColor;
	}
	
	public static final Image getNodeImage(short type)  {
		return pois[type].image;
	}
	
	public static final Image getNodeSearchImage(short type)  {
		return pois[type].searchIcon;
	}
	
	public static final int getNodeMaxScale(short type) {
		return pois[type].maxImageScale;
	}
	
	public static final int getNodeMaxTextScale(short type) {
		return pois[type].maxTextScale;
	}
	
	public static final boolean isNodeImageCentered(short type) {
		return pois[type].imageCenteredOnNode;
	}
	
	public static final boolean isNodeHideable(short type) {
		return pois[type].hideable;
	}
	
	public static final boolean isNodeAlert(short type) {
		return pois[type].alert;
	}
	
	public static final boolean isNodeClickable(short type) {
		return pois[type].clickable;
	}

	public static final byte getNodeOverviewMode(short type) {
		return pois[type].overviewMode;
	}
	
	public static void setNodeOverviewMode(short type, byte state) {
		pois[type].overviewMode = state;
	}
	
	public static void clearAllNodesOverviewMode() {
		for (short i = 1; i < getMaxType(); i++) {
			pois[i].overviewMode = OM_SHOWNORMAL;
		}
	}	
	public static final  String getNodeTypeDesc(short type) {
		if (type < 0 || type >= pois.length) {
			logger.error(Locale.get("legend.ERRORInvalidPOItype")/*ERROR: Invalid POI type */ + type + Locale.get("legend.requested")/* requested!*/);
			return null;
		}
		return pois[type].description;
	}
	
	public static final WayDescription getWayDescription(short type) {			
		if (type < 0 || type >= ways.length) {
			logger.error(Locale.get("legend.ERRORInvalidWaytype")/*ERROR: Invalid way type */ + type + Locale.get("legend.requested")/* requested*/);
			return null;
 		}
		return ways[type];
	}
	
	public static final boolean isWayHideable(short type) {
		return ways[type].hideable;
	}

	public static final Image getWayImage(short type)  {
		return ways[type].searchIcon;
	}
	
	public static final Image getWaySearchImage(short type)  {
		if (ways[type].searchIcon != null) {
			return ways[type].searchIcon;
		} else {
			WayDescription w = getWayDescription(type);
			Image img = Image.createImage(16,16);
			Graphics g = img.getGraphics();
			g.setColor(Legend.COLORS[Legend.COLOR_MAP_BACKGROUND]);
			g.fillRect(0, 0, 16, 16);
			g.setColor(w.lineColor);
			if (w.wayWidth == 1 || !Configuration.getCfgBitState(Configuration.CFGBIT_STREETRENDERMODE)) {
				g.setStrokeStyle(w.getGraphicsLineStyle());
				g.drawLine(0, 8, 15, 8);
			} else {
				g.fillRect(0, (16-w.wayWidth)/2, 16, w.wayWidth);
				g.setColor(w.boardedColor);
				g.drawLine(0, (16-w.wayWidth)/2-1, 15, (16-w.wayWidth)/2-1);
				g.drawLine(0, (16-w.wayWidth)/2 + w.wayWidth, 15, (16-w.wayWidth)/2 + w.wayWidth);
			}
			return img;
		}
	}
	
	public static Image getWayOrAreaSearchImage(short type)  {
		if (ways[type].searchIcon == null) {
			if (ways[type].isArea) {
				ways[type].searchIcon = getAreaSearchImage(type);
			} else {
				ways[type].searchIcon = getWaySearchImage(type);
			}
		}
		return ways[type].searchIcon;
	}
	
	public static void freeDrawnWayAndAreaSearchImages() {
		if (ways != null) {
			//#debug info
			int imagesFreed = 0;
			for(int i=0; i < ways.length; i++) {
				if (ways[i].searchIcon != null && !ways[i].hasSearchIconFromFile()) {
					ways[i].searchIcon = null;
					//#debug info
					imagesFreed++;
				}
			}
			//#debug info
			logger.info("Images freed: " + imagesFreed);
			//System.out.println("Images freed: " + imagesFreed);
		}
	}
	
	public static Image getAreaSearchImage(short type)  {
		if (ways[type].searchIcon != null) {
			return ways[type].searchIcon;
		} else {
			WayDescription w = Legend.getWayDescription(type);
			Image img = Image.createImage(16,16);
			Graphics g = img.getGraphics();
			g.setColor(w.lineColor);
			g.fillRect(1, 1, 14, 14);
			return img;        
		}
	}
	
	public static byte getWayOverviewMode(short type) {
		return ways[type].overviewMode;
	}

	public static void setWayOverviewMode(short type, byte state) {
		ways[type].overviewMode = state;
	}

	public static String get0Poi1Area2WayNamePart(byte nr) {
		return namePartRequired[nr];
	}
	
	public static void set0Poi1Area2WayNamePart(byte nr, String namePart) {
		namePartRequired[nr] = namePart;
	}

	public static void clearAllWaysOverviewMode() {
		for (short i = 1; i < getMaxWayType(); i++) {
			ways[i].overviewMode = OM_SHOWNORMAL;
		}
	}

	public static TravelMode [] getTravelModes() {
		return midletTravelModes;
	}
	
	public static final short getMaxWayType() {
		return (short)ways.length;
	}
	
	public static final short getMaxType() {
		return (short)pois.length;
	}
	
	public static final byte scaleToTile(int scale) {
		if (scale < tileScaleLevel[3]) { 		// 45000 in GpsMid 0.5.0
			return 3;
		} else if (scale < tileScaleLevel[2]) { // 180000 in GpsMid 0.5.0
			return 2;
		} else if (scale < tileScaleLevel[1]) { // 900000 in GpsMid 0.5.0
			return 1;
		}
		return 0;
	}

	public static String getMapVersion() {
		return mapVersion;
	}

	public static String getAppVersion() {
		// FIXME really give app version instead of map version
		String aV = GpsMid.getInstance().getAppProperty("MIDlet-Version");
		if (aV != null) {
			return aV;
		} else {
			return "??";
		}
	}

	public static String getBundleDate() {
		return bundleDate;
	}
	
	public static boolean isExtraMainstreetAvailable() {
		return false;
		//return true;
	}
	
}
