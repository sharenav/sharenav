/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.gpsmid.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
//#if polish.android
import de.enough.polish.android.midlet.MidletBridge;
import android.content.res.AssetManager;
import android.content.Context;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
//#endif
import javax.microedition.lcdui.Command;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;

import net.sourceforge.util.zip.ZipFile;

import de.ueller.gps.Node;
import de.ueller.gpsmid.graphics.ProjFactory;
import de.ueller.gpsmid.mapdata.QueueReader;
import de.ueller.gpsmid.routing.Routing;
import de.ueller.gpsmid.routing.TravelMode;
import de.ueller.gpsmid.ui.GuiDiscover;
import de.ueller.gpsmid.ui.Trace;
//#if not polish.android
import de.ueller.util.BufferedInputStream;
//#endif
import de.ueller.util.BufferedReader;
import de.ueller.util.IntTree;
import de.ueller.util.Logger;
import de.ueller.util.StringTokenizer;

import de.enough.polish.util.Locale;

/**
 * This class holds all the configurable data (i.e. the settings) that GpsMid has.
 */
public class Configuration {
	
	private static Logger logger;
	
	/** VERSION of the Configuration
	 *  If in the recordstore (configVersionStored) there is a lower version than this,
	 *  the default values for the features added between configVersionStored
	 *  and VERSION will be set, before the version in the recordstore is increased to VERSION.
	 */
	public final static int VERSION = 33;

	public final static int LOCATIONPROVIDER_NONE = 0;
	public final static int LOCATIONPROVIDER_SIRF = 1;
	public final static int LOCATIONPROVIDER_NMEA = 2;
	public final static int LOCATIONPROVIDER_JSR179 = 3;
	public final static int LOCATIONPROVIDER_SECELL = 4;
	public final static int LOCATIONPROVIDER_ANDROID = 5;
	
	// bit 0: render as street
	public final static short CFGBIT_STREETRENDERMODE = 0;
	// bit 1: Draw ways not useable in the current travel mode darker
	public final static short CFGBIT_DRAW_NON_TRAVELMODE_WAYS_DARKER = 1;
	// bit 2: show POITEXT
	public final static short CFGBIT_POITEXTS = 2;
	// bit 3: show WAYTEXT
	public final static short CFGBIT_WAYTEXTS = 3;
	// bit 4: show AREATEXT
	public final static short CFGBIT_AREATEXTS = 4;
	// bit 5: show POIS
	public final static short CFGBIT_POIS = 5;
	// bit 6: show WPTTTEXT
	public final static short CFGBIT_WPTTEXTS = 6;
	// bit 7: show descriptions
	public final static short CFGBIT_SHOWWAYPOITYPE = 7;
	// bit 8: show latlon
	public final static short CFGBIT_SHOWLATLON = 8;
	// bit 9: full screen
	public final static short CFGBIT_FULLSCREEN = 9;
	// bit 10: keep backlight on
	public final static short CFGBIT_BACKLIGHT_ON = 10;
	// bit 11: backlight on map screen as keep-alive (every 60 s) only
	public final static short CFGBIT_BACKLIGHT_ONLY_KEEPALIVE = 11;
	// bit 12: backlight method MIDP2
	public final static short CFGBIT_BACKLIGHT_MIDP2 = 12;
	// bit 13: backlight method NOKIA
	public final static short CFGBIT_BACKLIGHT_NOKIA = 13;
	// bit 14: backlight method NOKIA/FLASH
	public final static short CFGBIT_BACKLIGHT_NOKIAFLASH = 14;
	// bit 15: backlight only on while GPS is started
	public final static short CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED = 15;
	// bit 16: save map position on exit
	public final static short CFGBIT_AUTOSAVE_MAPPOS = 16;
	// bit 17: Sound on Connect
	public final static short CFGBIT_SND_CONNECT = 17;
	// bit 18: Sound on Disconnect
	public final static short CFGBIT_SND_DISCONNECT = 18;
	// bit 19: Routing Instructions
	public final static short CFGBIT_SND_ROUTINGINSTRUCTIONS = 19;
	// bit 20: Gps Auto Reconnect
	public final static short CFGBIT_GPS_AUTORECONNECT = 20;
	// bit 21: Sound when destination reached
	public final static short CFGBIT_SND_DESTREACHED = 21;
	// bit 22: auto recalculate route
	public final static short CFGBIT_ROUTE_AUTO_RECALC = 22;
	// bit 23: use JSR135 or JSR 234 for taking pictures;
	public final static short CFGBIT_USE_JSR_234 = 23;
	// bit 25: show point of compass in rotated map
	public final static short CFGBIT_SHOW_POINT_OF_COMPASS = 25;
	// bit 26: add geo reference into the exif of a photo;
	public final static short CFGBIT_ADD_EXIF = 26;
	// bit 27: show AREAS
	public final static short CFGBIT_AREAS = 27;
	// bit 28: big poi labels
	public final static short CFGBIT_POI_LABELS_LARGER = 28;
	// bit 29: big wpt labels
	public final static short CFGBIT_WPT_LABELS_LARGER = 29;
	// bit 30: show oneway arrows
	public final static short CFGBIT_ONEWAY_ARROWS = 30;
	// bit 31: Debug Option: show route connections
	public final static short CFGBIT_ROUTE_CONNECTIONS = 31;
	// bit 32: backlight method SIEMENS
	public final static short CFGBIT_BACKLIGHT_SIEMENS = 32;
	// bit 33: Skip initial splash screen
	public final static short CFGBIT_SKIPP_SPLASHSCREEN = 33;
	// bit 34: show place labels
	public final static short CFGBIT_PLACETEXTS = 34;
	// bit 35: Sound alert for speeding
	public final static short CFGBIT_SPEEDALERT_SND = 35;
	// bit 36: Visual alert for speeding
	public final static short CFGBIT_SPEEDALERT_VISUAL = 36;
	// bit 37: Debug Option: show route bearings
	public final static short CFGBIT_ROUTE_BEARINGS = 37;
	// bit 38: Debug Option: hide quiet arrows
	public final static short CFGBIT_ROUTE_HIDE_QUIET_ARROWS = 38;
	// bit 39: in route mode up/down keys are for route browsing
	public final static short CFGBIT_ROUTE_BROWSING = 39;
	// bit 40: Show scale bar on map
	public final static short CFGBIT_SHOW_SCALE_BAR = 40;
	// bit 41: Log cell-ids to directory
	public final static short CFGBIT_CELLID_LOGGING = 41;
	// bit 42: Flag whether to also put waypoints in GPX track
	public final static short CFGBIT_WPTS_IN_TRACK = 42;
	// bit 43: Ask for GPX track name when starting recording
	public final static short CFGBIT_GPX_ASK_TRACKNAME_START = 43;
	// bit 44: Ask for GPX track name when starting recording
	public final static short CFGBIT_GPX_ASK_TRACKNAME_STOP = 44;
	// bit 45: Flag whether to always upload cellid log to opencellid
	public final static short CFGBIT_CELLID_ALWAYS = 45;
	// bit 46: Flag whether to upload cellid log to opencellid after confirm
	public final static short CFGBIT_CELLID_CONFIRM = 46;
	// bit 47: Flag whether to fall back to cellid location when GPS fix not available
	public final static short CFGBIT_CELLID_FALLBACK = 47;
	// bit 48: Flag whether to cache cellid locations
	public final static short CFGBIT_CELLID_OFFLINEONLY = 48;
	// bit 49: Flag whether to cache cellid locations
	public final static short CFGBIT_CELLID_ONLINEONLY = 49;
	// bit 50: Flag whether to also put waypoints in waypoint store when recording GPX
	public final static short CFGBIT_WPTS_IN_WPSTORE = 50;
	// bit 51: Flag whether to show turn restrictions for debugging
	public final static short CFGBIT_SHOW_TURN_RESTRICTIONS = 51;
	// bit 52: Flag whether turn restrictions should be used for route calculation
	public final static short CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION = 52;
	// bit 53: Flag whether iconMenus should be used
	public final static short CFGBIT_ICONMENUS = 53;
	// bit 54: Flag whether iconMenus should be fullscreen
	public final static short CFGBIT_ICONMENUS_FULLSCREEN = 54;
	// bit 55: Flag whether iconMenus should be optimized for routing
	// public final static short CFGBIT_ICONMENUS_ROUTING_OPTIMIZED = 55;
	// removed 2012-08-19
	// bit 56: Flag whether night style should be applied
	public final static short CFGBIT_NIGHT_MODE = 56;
	// bit 57: Flag whether turbo route calc should be used
	public final static short CFGBIT_TURBO_ROUTE_CALC = 57;
	// bit 58: Flag whether speed should be displayed in map screen
	public final static short CFGBIT_SHOW_SPEED_IN_MAP = 58;
	// bit 59: Flag whether to start GPS reception when entering map
	public final static short CFGBIT_AUTO_START_GPS = 59;
	// bit 60: Flag whether to display in metric or imperial units
	public final static short CFGBIT_METRIC = 60;
	// bit 61: Flag whether air distance to destination should be displayed in map screen
	public final static short CFGBIT_SHOW_AIR_DISTANCE_IN_MAP = 61;
	// bit 62: Flag whether offset to route should be displayed in map screen
	public final static short CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP = 62;
	// bit 63: Flag whether route duration should be displayed in map screen
	public final static short CFGBIT_SHOW_ROUTE_DURATION_IN_MAP = 63;
	// bit 64: Flag whether altitude should be displayed in map screen
	public final static short CFGBIT_SHOW_ALTITUDE_IN_MAP = 64;
	// bit 65: Flag whether to show buildings in map
	public final static short CFGBIT_BUILDINGS = 65;
	// bit 66: Flag whether to show building labels in map
	public final static short CFGBIT_BUILDING_LABELS = 66;
	// bit 67: Flag if current time is shown on map
	public final static short CFGBIT_SHOW_CLOCK_IN_MAP = 67;
	// bit 68: Flag if ETA is shown on map
	public final static short CFGBIT_SHOW_ETA_IN_MAP = 68;
	// bit 69: Flag if seasonal (winter) speed limits are applied
	public final static short CFGBIT_MAXSPEED_WINTER = 69;
	// bit 70: Flag whether iconMenus should have icons mapped on keys
	public final static short CFGBIT_ICONMENUS_MAPPED_ICONS = 70;
	/** bit 71: Flag whether canvas specific defaults are set
	(we need a canvas to determine display size and hasPointerEvents(),
	 so we can't determine appropriate defaults in Configuration) */
	public final static short CFGBIT_CANVAS_SPECIFIC_DEFAULTS_DONE = 71;
	/** bit 72: Flag whether initial Setup Forms were shown to the user */
	public final static short CFGBIT_INITIAL_SETUP_DONE = 72;
	/** bit 73: Flag whether to add a Back command in fullscreen menu */
	public final static short CFGBIT_ICONMENUS_BACK_CMD = 73;
	/** bit 74: Flag whether the route algorithm should try to find a motorway within 20 km */
	public final static short CFGBIT_ROUTE_TRY_FIND_MOTORWAY = 74;
	/** bit 75: Flag whether the route algorithm deeply examines motorways */
	public final static short CFGBIT_ROUTE_BOOST_MOTORWAYS = 75;
	/** bit 76: Flag whether the route algorithm deeply examines trunks and primarys */
	public final static short CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS = 76;
	/** bit 77: Flag whether iconMenus should have bigger tabs */
	public final static short CFGBIT_ICONMENUS_BIG_TAB_BUTTONS = 77;
	/** bit 78: Flag whether the map should be auto scaled to speed */
	public final static short CFGBIT_AUTOZOOM = 78;
	/** bit 79: Flag whether, if available, tone sequences should be played instead of sound samples */
	public final static short CFGBIT_SND_TONE_SEQUENCES_PREFERRED = 79;
	/** bit 80: Flag whether internal PNG files should be preferred when using an external map (faster on some Nokias) */
	public final static short CFGBIT_PREFER_INTERNAL_PNGS = 80;
	/** bit 81: Flag whether the menu with predefined way points is shown. */
	public final static short CFGBIT_WAYPT_OFFER_PREDEF = 81;
	/** bit 82: Flag whether street borders are shown. */
	public final static short CFGBIT_NOSTREETBORDERS = 82;
	/** bit 83: Flag whether to show distances variably as km/m instead of just m. */
	public final static short CFGBIT_DISTANCE_VIEW = 83;
	/** bit 84: Flag whether to show round way (segment) ends. */
	public final static short CFGBIT_ROUND_WAY_ENDS = 84;
	/** bit 85: backlight method ANDROID_WAKELOCK */
	public final static short CFGBIT_BACKLIGHT_ANDROID_WAKELOCK = 85;
	/** bit 86: Flag whether the route algorithm uses motorways */
	public final static short CFGBIT_ROUTE_USE_MOTORWAYS = 86;
	/** bit 87: Flag whether the route algorithm uses toll roads */
	public final static short CFGBIT_ROUTE_USE_TOLLROADS = 87;
	/** bit 88: free from 2011-01-08, was Flag whether the touch zones for zoom & center & destination are symmetric */

	/** bit 89: Flag whether air distance to destination should be displayed even when routing */
	public final static short CFGBIT_SHOW_AIR_DISTANCE_WHEN_ROUTING = 89;
	/** bit 90: Flag whether ask for routing options before starting routing */
	public final static short CFGBIT_DONT_ASK_FOR_ROUTING_OPTIONS = 90;
	/** bit 91: Flag whether to use digital compass for map direction (rotation) */
	public final static short CFGBIT_COMPASS_DIRECTION = 91;
	/** bit 92: Flag whether cellid location is to be performed at startup */
	public final static short CFGBIT_CELLID_STARTUP = 92;
	/** bit 93: backlight method SAMSUNG */
	public final static short CFGBIT_BACKLIGHT_SAMSUNG = 93;
	/** bit 94: Flag whether internal sound files should be used instead of ones with external map */
	public final static short CFGBIT_PREFER_INTERNAL_SOUNDS = 94;
	/** bit 95: Flag whether to use a virtual on-screen number keypad in the incremental search screen */
	public final static short CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD = 95;
	/** bit 96: save destination position for next start */
	public final static short CFGBIT_AUTOSAVE_DESTPOS = 96;
	/** bit 97: indicate whether saved destination position is valid */
	public final static short CFGBIT_SAVED_DESTPOS_VALID = 97;
	/** bit 98: indicate whether to show online option */
	public final static short CFGBIT_ONLINE_GEOHACK = 98;
	/** bit 99: indicate whether to show online option */
	public final static short CFGBIT_ONLINE_WEATHER = 99;
	/** bit 100: indicate whether to show online option */
	public final static short CFGBIT_ONLINE_PHONE = 100;
	/** bit 101: indicate whether to show topographic map entry */
	public final static short CFGBIT_ONLINE_TOPOMAP = 101;
	/** bit 102: indicate whether to show online option */
	public final static short CFGBIT_ONLINE_WEBSITE = 102;
	/** bit 103: unused */
	/** bit 104: indicate whether to show online option */
	public final static short CFGBIT_ONLINE_WIKIPEDIA_RSS = 104;
	/** bit 105: indicate whether long map tap feature is enabled */
	public final static short CFGBIT_MAPTAP_LONG = 105;
	/** bit 106:  indicate whether double map tap feature is enabled */
	public final static short CFGBIT_MAPTAP_DOUBLE = 106;
	/** bit 107: indicate whether single map tap feature is enabled*/
	public final static short CFGBIT_MAPTAP_SINGLE = 107;
	/** bit 108: indicate whether gpsmid is running (for android which can kill the process and restart it)*/
	public final static short CFGBIT_RUNNING = 108;
	/** bit 109: Flag whether to use word search (in contrast to whole name search) in incremental search */
	public final static short CFGBIT_WORD_ISEARCH = 109;
	/** bit 110: Flag whether to scroll the search result under cursor horizontally */
	public final static short CFGBIT_TICKER_ISEARCH = 110;
	/** bit 111: Flag whether to scroll all search results horizontally */
	public final static short CFGBIT_TICKER_ISEARCH_ALL = 111;
	/** bit 112: Flag whether to have a large font for UI */
	public final static short CFGBIT_LARGE_FONT = 112;
	/** bit 113: Flag whether to suppress the warning about hitting the search max limit */
	public final static short CFGBIT_SUPPRESS_SEARCH_WARNING = 113;
	/** bit 114: Flag whether to suppress the warning about poor routes */
	public final static short CFGBIT_SUPPRESS_ROUTE_WARNING = 114;
	/** bit 115: Flag whether to simplify map drawing when busy */
	public final static short CFGBIT_SIMPLIFY_MAP_WHEN_BUSY = 115;
	/** bit 116: Flag whether to use compass and movement autoswitch for direction when moving (only compass or movement otherwise) */
	public final static short CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION = 116;
	/** bit 117: backlight method ANDROID_WINDOW_MANAGER */
	public final static short CFGBIT_BACKLIGHT_ANDROID_WINDOW_MANAGER = 117;
	/** bit 118: use GPS time for on-screen time */
	public final static short CFGBIT_GPS_TIME = 118;
	/** bit 119: fallback to device time when GPS time not available */
	public final static short CFGBIT_GPS_TIME_FALLBACK = 119;
	/** bit 120: stop routing when arriving at destination */
	public final static short CFGBIT_STOP_ROUTING_AT_DESTINATION = 120;
	/** bit 121: show accuracy wit solution string */
	public final static short CFGBIT_SHOW_ACCURACY = 121;
	/** bit 122: show big on-left-corner navigation icons */
	public final static short CFGBIT_NAVI_ARROWS_BIG = 122;
	/** bit 123: show in-map small navigation icons */
	public final static short CFGBIT_NAVI_ARROWS_IN_MAP = 123;
	/** bit 124: show favorite destinations in route icon menu */
	public final static short CFGBIT_FAVORITES_IN_ROUTE_ICON_MENU = 124;
	/** bit 125: show travel mode in map */
	public final static short CFGBIT_SHOW_TRAVEL_MODE_IN_MAP = 125;
	//#if polish.api.finland
	/** bit 126: alert for speed camera */
	public final static short CFGBIT_SPEEDCAMERA_ALERT = 126;
	//#endif
	// bit 127: Audio alert for alert node nearby
	public final static short CFGBIT_NODEALERT_SND = 127;
	// bit 128: Visual alert for alert node nearby
	public final static short CFGBIT_NODEALERT_VISUAL = 128;
	// bit 129: Enable clickable map objects
	public final static short CFGBIT_CLICKABLE_MAPOBJECTS = 129;
	// bit 130: Open icon menu in split-screen mode
	public final static short CFGBIT_ICONMENUS_SPLITSCREEN = 130;
	// bit 131: Open search in split-screen mode
        // FIXME use the same as for iconmenus for now
	public final static short CFGBIT_SEARCH_SPLITSCREEN = 130;
	// bit 132: Auto-calibration for compass deviation by movement
	public final static short CFGBIT_COMPASS_AUTOCALIBRATE = 132;
	// bit 133: Always rotate map by compass
	public final static short CFGBIT_COMPASS_ALWAYS_ROTATE = 133;
	// bit 134: display RouteHelpers
	public final static short CFGBIT_ROUTEHELPERS = 134;
	// bit 135: display RouteConnectionHelpers
	public final static short CFGBIT_ROUTECONNECTION_TRACES = 135;
	// bit 136: Aim for shortest distance in routing if true (otherwise shortest time)
	public final static short CFGBIT_ROUTE_AIM = 136;
	// bit 137: Estimation heuristic for best time simple 120 km/h
	public final static short CFGBIT_ROUTE_ESTIMATION_120KMH = 137;
	// bit 138: Ignore turn angles in route estimation heuristic
	public final static short CFGBIT_ROUTE_ESTIMATION_NO_ANGLES = 138;
	// bit 139: true if favorite search is sorted by distance (default is by name)
	public final static short CFGBIT_SEARCH_FAVORITES_BY_DISTANCE = 139;
	// bit 140: true if mapdata search result are sorted by name (default is by distance)
	public final static short CFGBIT_SEARCH_MAPDATA_BY_NAME = 140;
	// bit 141: true if application can be quit with Back-Button on the map screen
	public final static short CFGBIT_EXIT_APPLICATION_WITH_BACK_BUTTON = 141;
	/** bit 142: true if internet access is allowed */
	public final static short CFGBIT_INTERNET_ACCESS = 142;
	/** bit 143: indicate whether gpsmid is connected to gps (for android which can kill the process and restart it)*/
	public final static short CFGBIT_GPS_CONNECTED = 143;
	/** bit 144: prefer outline area format (only Android) */
	public final static short CFGBIT_PREFER_OUTLINE_AREAS = 144;
	/** bit 145: Debug / Show tile requests dropped message */
	public final static short CFGBIT_SHOW_TILE_REQUESTS_DROPPED = 145;
	/** bit 146: Omit map source info */
	public final static short CFGBIT_SHOW_MAP_CREDITS = 146;
	/** bit 147: Show NMEA errors */
	public final static short CFGBIT_SHOW_NMEA_ERRORS = 147;
	/** bit 148: Buffered file reading */
	public final static short CFGBIT_BUFFEREDINPUTSTREAM = 148;
	/** bit 149: resolve names after map tiles are read */
	public final static short CFGBIT_RESOLVE_NAMES_LAST = 149;
	/** bit 150: show device (Android for now) native keyboard in search */
	public final static short CFGBIT_SEARCH_SHOW_NATIVE_KEYBOARD = 150;
	/** bit 151: Flag whether iconMenus should be used for settings */
	public final static short CFGBIT_ICONMENUS_SETUP = 151;
	/** bit 152: Use TMS map as background */
	public final static short CFGBIT_TMS_BACKGROUND = 152;
	/** bit 153: Disable areas when background map is in use */
	public final static short CFGBIT_DISABLE_AREAS_WHEN_BACKGROUND_MAP = 153;
	/** bit 154: Disable buildings when background map is in use */
	public final static short CFGBIT_DISABLE_BUILDINGS_WHEN_BACKGROUND_MAP = 154;
	
	/**
	 * These are the database record IDs for each configuration option
	 */
	private static final int RECORD_ID_BT_URL = 1;
	private static final int RECORD_ID_LOCATION_PROVIDER = 2;
	private static final int RECORD_ID_CFGBITS_0_TO_63 = 3;
	private static final int RECORD_ID_GPX_URL = 4;
	private static final int RECORD_ID_MAP_FROM_JAR = 5;
	private static final int RECORD_ID_MAP_FILE_URL = 6;
	//private static final int RECORD_ID_BACKLIGHT_DEFAULT = 7;
	private static final int RECORD_ID_LOG_RAW_GPS_URL = 8;
	private static final int RECORD_ID_LOG_RAW_GPS_ENABLE = 9;
	private static final int RECORD_ID_LOG_DEBUG_URL = 10;
	private static final int RECORD_ID_LOG_DEBUG_ENABLE = 11;
	private static final int RECORD_ID_DETAIL_BOOST = 12;
	private static final int RECORD_ID_GPX_FILTER_MODE = 13;
	private static final int RECORD_ID_GPX_FILTER_TIME = 14;
	private static final int RECORD_ID_GPX_FILTER_DIST = 15;
	private static final int RECORD_ID_GPX_FILTER_ALWAYS_DIST = 16;
	private static final int RECORD_ID_LOG_DEBUG_SEVERITY = 17;
	private static final int RECORD_ID_ROUTE_ESTIMATION_FAC = 18;
	private static final int RECORD_ID_CONTINUE_MAP_WHILE_ROUTING = 19;
	private static final int RECORD_ID_BT_KEEPALIVE = 20;
	private static final int RECORD_ID_STARTUP_RADLAT = 21;
	private static final int RECORD_ID_STARTUP_RADLON = 22;
	private static final int RECORD_ID_PHOTO_URL = 23;
	private static final int RECORD_ID_GPS_RECONNECT = 24;
	private static final int RECORD_ID_PHOTO_ENCODING = 25;
	private static final int RECORD_ID_MAP_PROJECTION = 26;
	private static final int RECORD_ID_CONFIG_VERSION = 27;
	private static final int RECORD_ID_SMS_RECIPIENT = 28;
	private static final int RECORD_ID_SPEED_TOLERANCE = 29;
	private static final int RECORD_ID_OSM_USERNAME = 30;
	private static final int RECORD_ID_OSM_PWD = 31;
	private static final int RECORD_ID_OSM_URL = 32;
	private static final int RECORD_ID_MIN_ROUTELINE_WIDTH = 33;
	private static final int RECORD_ID_KEY_SHORTCUT = 34;
	private static final int RECORD_ID_AUTO_RECENTER_TO_GPS_MILLISECS = 35;
	private static final int RECORD_ID_ROUTE_TRAVEL_MODE = 36;
	private static final int RECORD_ID_OPENCELLID_APIKEY = 37;
	private static final int RECORD_ID_PHONE_ALL_TIME_MAX_MEMORY = 38;
	private static final int RECORD_ID_CFGBITS_64_TO_127 = 39;
	private static final int RECORD_ID_MAINSTREET_NET_DISTANCE_KM = 40;
	private static final int RECORD_ID_DETAIL_BOOST_POI = 41;
	private static final int RECORD_ID_TRAFFIC_SIGNAL_CALC_DELAY = 42;
	private static final int RECORD_ID_WAYPT_SORT_MODE = 43;
	private static final int RECORD_ID_BACKLIGHTLEVEL = 44;
	private static final int RECORD_ID_BASESCALE = 45;
	private static final int RECORD_ID_UI_LANG = 46;
	private static final int RECORD_ID_NAVI_LANG = 47;
	private static final int RECORD_ID_ONLINE_LANG = 48;
	private static final int RECORD_ID_WP_LANG = 49;
	private static final int RECORD_ID_NAME_LANG = 50;
	private static final int RECORD_ID_SOUND_DIRECTORY = 51;
	private static final int RECORD_ID_DEST_RADLAT = 52;
	private static final int RECORD_ID_DEST_RADLON = 53;
	// max number of search results to find before stopping search
	private static final int RECORD_ID_SEARCH_MAX = 54;
	private static final int RECORD_ID_POI_SEARCH_DIST = 55;
	private static final int RECORD_ID_DEST_LINE_WIDTH = 56;
	private static final int RECORD_ID_TIME_DIFF = 57;
	private static final int RECORD_ID_CFGBITS_128_TO_191 = 58;
	private static final int RECORD_ID_ALTITUDE_CORRECTION = 59;
	private static final int RECORD_ID_TMS_URL = 60;

	// Gpx Recording modes
	// GpsMid determines adaptive if a trackpoint is written
	public final static int GPX_RECORD_ADAPTIVE = 0;
	// User specified options define if a trackpoint is written
	public final static int GPX_RECORD_MINIMUM_SECS_DIST = 1;
	
	public static int KEYCODE_CAMERA_COVER_OPEN = -34;
	public static int KEYCODE_CAMERA_COVER_CLOSE = -35;
	public static int KEYCODE_CAMERA_CAPTURE = -26;

	public static final int MAX_WAYPOINTNAME_LENGTH = 255;
	public static final int MAX_WAYPOINTNAME_DRAWLENGTH = 25;
	public static final int MAX_TRACKNAME_LENGTH = 50;
	public static final int MAX_WAYPOINTS_NAME_LENGTH = 50;
	
	public static String[] LOCATIONPROVIDER;

	private static String[] compassDirections;
	
	private final static byte[] empty = "".getBytes();

	private static String btUrl;
	/** This URL is used to store logs of raw data received from the GPS receiver*/
	private static String rawGpsLogUrl;
	private static boolean rawGpsLogEnable;
	private static String rawDebugLogUrl;
	private static boolean rawDebugLogEnable;
	private static int locationProvider = 0;
	private static int gpxRecordRuleMode;
	private static int gpxRecordMinMilliseconds;
	private static int gpxRecordMinDistanceCentimeters;
	private static int gpxRecordAlwaysDistanceCentimeters;
	private static long cfgBits_0_to_63 = 0;
	private static long cfgBitsDefault_0_to_63 = 0;
	private static long cfgBits_64_to_127 = 0;
	private static long cfgBitsDefault_64_to_127 = 0;
	private static long cfgBits_128_to_191 = 0;
	private static long cfgBitsDefault_128_to_191 = 0;
	private static int detailBoost = 0;
	private static int detailBoostPOI = 0;
	private static int detailBoostDefault = 0;
	private static int detailBoostDefaultPOI = 0;
	private static float detailBoostMultiplier;
	private static float detailBoostMultiplierPOI;
	private static String gpxUrl;
	private static String photoUrl;
	private static String photoEncoding;
	private static int debugSeverity;
	private static int routeEstimationFac = 6;
	
	// Constants for continueMapWhileRouteing
	public static final int continueMap_Not = 0;
	public static final int continueMap_At_Route_Line_Creation = 1;
	public static final int continueMap_Always = 2;
		
	// the phone's locale
	public static String localeLang = null;

	/** 0 = do not continue map, 1 = continue map only during route line production, 2 = continue map all the time */
	private static int continueMapWhileRouteing = continueMap_At_Route_Line_Creation;

	private static boolean btKeepAlive = false;
	private static boolean btAutoRecon = false;
	private static Node startupPos = new Node(0.0f, 0.0f);
	private static Node destPos = new Node(0.0f, 0.0f);
	private static byte projTypeDefault = ProjFactory.NORTH_UP;
	
	private static boolean mapFromJar;
	private static String mapFileUrl;
	public static ZipFile mapZipFile;

	public static boolean zipFileIsApk;

	private static String smsRecipient;
	private static int speedTolerance = 0;
	
	private static String utf8encodingstring = null;
	
	private static String osm_username;
	private static String osm_pwd;
	private static String osm_url;

	private static String tms_url;

	private static String opencellid_apikey;

	private static long phoneAllTimeMaxMemory = 0;

	private static int searchMax = 0;
	private static int poiSearchDist = 0;
	
	private static int minRouteLineWidth = 0;
	private static int mainStreetDistanceKm = 0;
	private static int autoRecenterToGpsMilliSecs = 10;
	private static int currentTravelModeNr = 0;
	private static int currentTravelMask = 0;
	
	// Constants for way point sort mode
	public static final int WAYPT_SORT_MODE_NONE = 0;
	public static final int WAYPT_SORT_MODE_NEW_FIRST = 1;
	public static final int WAYPT_SORT_MODE_OLD_FIRST = 2;
	public static final int WAYPT_SORT_MODE_ALPHABET = 3;
	public static final int WAYPT_SORT_MODE_DISTANCE = 4;
	
	private static int wayptSortMode = WAYPT_SORT_MODE_NEW_FIRST;

	private static int trafficSignalCalcDelay = 5;

	private static volatile int backLightLevel = 50;

	private static int baseScale = 23;
	private static float realBaseScale = 15000;

	private static String uiLang;
	private static boolean uiLangLoaded = false;
	private static String naviLang;
	private static String onlineLang;
	private static String wikipediaLang;
	private static String namesOnMapLang;
	private static String soundDirectory;
	
	public static String[] projectionsString;
	
	private static boolean hasPointerEvents;
	private static boolean isSamsungS8000 = false;

	private static int destLineWidth = 2;
	private static int timeDiff = 0;
	private static int altitudeCorrection = 0;
	//#if polish.android
	private static AssetManager assets = null;
	//#endif
	
	public static void read() {
		logger = Logger.getInstance(Configuration.class, Logger.DEBUG);
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			if (database == null) {
				//#debug debug
				System.out.println("Could not open config"); // Logger won't work if config is not read yet
				return;
			}
			
			int configVersionStored = readInt(database, RECORD_ID_CONFIG_VERSION);
			//#debug info
			logger.info("Config version stored: " + configVersionStored);
			
			cfgBits_0_to_63 = readLong(database, RECORD_ID_CFGBITS_0_TO_63);
			cfgBits_64_to_127 = readLong(database, RECORD_ID_CFGBITS_64_TO_127);
			if (configVersionStored >= 29) {
				cfgBits_128_to_191 = readLong(database, RECORD_ID_CFGBITS_128_TO_191);
			}
			btUrl = readString(database, RECORD_ID_BT_URL);
			locationProvider = readInt(database, RECORD_ID_LOCATION_PROVIDER);
			gpxUrl = readString(database, RECORD_ID_GPX_URL);
			photoUrl = readString(database, RECORD_ID_PHOTO_URL);
			photoEncoding = readString(database, RECORD_ID_PHOTO_ENCODING);
			mapFromJar = (readInt(database, RECORD_ID_MAP_FROM_JAR) == 0);
			mapFileUrl = readString(database, RECORD_ID_MAP_FILE_URL);
			rawGpsLogUrl = readString(database, RECORD_ID_LOG_RAW_GPS_URL);
			rawGpsLogEnable = (readInt(database, RECORD_ID_LOG_RAW_GPS_ENABLE) !=0);
			detailBoost = readInt(database, RECORD_ID_DETAIL_BOOST);
			detailBoostDefault = detailBoost;
			detailBoostPOI = readInt(database, RECORD_ID_DETAIL_BOOST_POI);
			detailBoostDefaultPOI = detailBoostPOI;
			calculateDetailBoostMultipliers();
			gpxRecordRuleMode = readInt(database, RECORD_ID_GPX_FILTER_MODE);
			gpxRecordMinMilliseconds = readInt(database, RECORD_ID_GPX_FILTER_TIME);
			gpxRecordMinDistanceCentimeters = readInt(database, RECORD_ID_GPX_FILTER_DIST);
			gpxRecordAlwaysDistanceCentimeters = readInt(database, RECORD_ID_GPX_FILTER_ALWAYS_DIST);
			rawDebugLogUrl = readString(database, RECORD_ID_LOG_DEBUG_URL);
			rawDebugLogEnable = (readInt(database,  RECORD_ID_LOG_DEBUG_ENABLE) !=0);
			debugSeverity = readInt(database, RECORD_ID_LOG_DEBUG_SEVERITY);
			routeEstimationFac = readInt(database, RECORD_ID_ROUTE_ESTIMATION_FAC);
			continueMapWhileRouteing = readInt(database, RECORD_ID_CONTINUE_MAP_WHILE_ROUTING);
			btKeepAlive = (readInt(database, RECORD_ID_BT_KEEPALIVE) !=0);
			btAutoRecon = (readInt(database, RECORD_ID_GPS_RECONNECT) !=0);
			readPosition(database, startupPos, RECORD_ID_STARTUP_RADLAT, RECORD_ID_STARTUP_RADLON);
			readPosition(database, destPos, RECORD_ID_DEST_RADLAT, RECORD_ID_DEST_RADLON);
			projTypeDefault = (byte) readInt(database,  RECORD_ID_MAP_PROJECTION);
			ProjFactory.setProj(projTypeDefault);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_TMS_BACKGROUND)) {
				ProjFactory.setProj(ProjFactory.NORTH_UP);
				setCfgBitState(CFGBIT_AUTOZOOM, getCfgBitSavedState(CFGBIT_AUTOZOOM), false);
			}
			calculateRealBaseScale();
			smsRecipient = readString(database, RECORD_ID_SMS_RECIPIENT);
			speedTolerance = readInt(database, RECORD_ID_SPEED_TOLERANCE);
			osm_username = readString(database, RECORD_ID_OSM_USERNAME);
			osm_pwd = readString(database, RECORD_ID_OSM_PWD);
			osm_url = readString(database, RECORD_ID_OSM_URL);
			if (osm_url == null) {
				osm_url = "http://api.openstreetmap.org/api/0.6/";
			}

			uiLang = readString(database, RECORD_ID_UI_LANG);
			naviLang = readString(database, RECORD_ID_NAVI_LANG);
			onlineLang = readString(database, RECORD_ID_ONLINE_LANG);
			wikipediaLang = readString(database, RECORD_ID_WP_LANG);
			namesOnMapLang = readString(database, RECORD_ID_NAME_LANG);
			soundDirectory = readString(database, RECORD_ID_SOUND_DIRECTORY);
			
			opencellid_apikey = readString(database, RECORD_ID_OPENCELLID_APIKEY);

			minRouteLineWidth = readInt(database, RECORD_ID_MIN_ROUTELINE_WIDTH);
			mainStreetDistanceKm = readInt(database, RECORD_ID_MAINSTREET_NET_DISTANCE_KM);
			autoRecenterToGpsMilliSecs = readInt(database, RECORD_ID_AUTO_RECENTER_TO_GPS_MILLISECS);
			currentTravelModeNr = readInt(database, RECORD_ID_ROUTE_TRAVEL_MODE);
			currentTravelMask = 1 << currentTravelModeNr;
			phoneAllTimeMaxMemory = readLong(database, RECORD_ID_PHONE_ALL_TIME_MAX_MEMORY);
			trafficSignalCalcDelay = readInt(database, RECORD_ID_TRAFFIC_SIGNAL_CALC_DELAY);
			wayptSortMode = readInt(database, RECORD_ID_WAYPT_SORT_MODE);
			backLightLevel = readInt(database, RECORD_ID_BACKLIGHTLEVEL);
			searchMax = readInt(database, RECORD_ID_SEARCH_MAX);
			poiSearchDist = readInt(database, RECORD_ID_POI_SEARCH_DIST);
			/* there's been duplicate use of the id for RECORD_ID_BACKLIGHTLEVEL / RECORD_ID_WAYPTSORTMODE
			 * so we need to check if backlightlevel is 0 to not end up with a black display
			 */
			if (backLightLevel == 0) { 
				backLightLevel = 50;
			}
			baseScale = readInt(database, RECORD_ID_BASESCALE);
			calculateRealBaseScale();
			destLineWidth = readInt(database, RECORD_ID_DEST_LINE_WIDTH);
			timeDiff = readInt(database, RECORD_ID_TIME_DIFF);
			altitudeCorrection = readInt(database, RECORD_ID_ALTITUDE_CORRECTION);
			
			tms_url = readString(database, RECORD_ID_TMS_URL);
			if (tms_url == null) {
				tms_url = "http://tiles.kartat.kapsi.fi/taustakartta/%z/%x/%y.png";
			}

			/* close the record store before accessing it nested for writing
			 * might otherwise cause problems on some devices
			 * see [ gpsmid-Bugs-2983148 ] Recordstore error on startup, settings are not persistent 
			 */
			database.closeRecordStore();
			
			applyDefaultValues(configVersionStored);
			// remember for which version the default values were stored
			write(VERSION, RECORD_ID_CONFIG_VERSION);
			
			// setCfgBitSavedState(CFGBIT_ROUTEHELPERS, true);
			// setCfgBitSavedState(CFGBIT_ROUTEHELPERS, false);
			// setCfgBitSavedState(CFGBIT_ROUTECONNECTION_TRACES, true);
			// setCfgBitSavedState(CFGBIT_ROUTECONNECTION_TRACES, false);
			// setCfgBitSavedState(CFGBIT_ROUTE_ESTIMATION_120KMH, true);
			// setCfgBitSavedState(CFGBIT_ROUTE_ESTIMATION_120KMH, false);
			// setCfgBitSavedState(CFGBIT_ROUTE_ESTIMATION_NO_ANGLES, true);
			// setCfgBitSavedState(CFGBIT_ROUTE_ESTIMATION_NO_ANGLES, false);

		} catch (Exception e) {
			logger.exception(Locale.get("configuration.ProblemsWithReadingConfig")/*Problems with reading our configuration: */, e);
		}
	}
	
	/** If in the recordstore (configVersionStored) there is a lower version than VERSION
	 *  of the Configuration, the default values for the features added between configVersionStored
	 *  and VERSION will be set, before the version in the recordstore is increased to VERSION.
	 */
	private static void applyDefaultValues(int configVersionStored) {
		if (configVersionStored < 1) {
			cfgBits_0_to_63 =	1L << CFGBIT_STREETRENDERMODE |
			   			1L << CFGBIT_POITEXTS |
			   			1L << CFGBIT_AREATEXTS |
			   			1L << CFGBIT_WPTTEXTS |
			   			// 1L << CFGBIT_WAYTEXTS | // way texts are still experimental
			   			1L << CFGBIT_ADD_EXIF |
			   			1L << CFGBIT_ONEWAY_ARROWS |
			   			1L << CFGBIT_POIS |
			   			1L << CFGBIT_AUTOSAVE_MAPPOS;
			if (getDefaultDeviceBacklightMethodCfgBit() != 0) {
				setCfgBitSavedState(getDefaultDeviceBacklightMethodCfgBit(), true);
				cfgBits_0_to_63 |= 1L << CFGBIT_BACKLIGHT_ON;
			}
			//#if polish.android
			// (was) no bundle support for android yet, set a fixed location for map
			// bundle support works enough so we don't need this
			// setBuiltinMap(false);
			// setMapUrl("file:///sdcard/gpsmidmap/");
			// setMapUrl("file:///");
			//#endif
			// Record Rule Default
			setGpxRecordRuleMode(GPX_RECORD_MINIMUM_SECS_DIST);
			setGpxRecordMinMilliseconds(1000);
			setGpxRecordMinDistanceCentimeters(300);
			setGpxRecordAlwaysDistanceCentimeters(500);
			// Routing defaults
			setContinueMapWhileRouteing(continueMap_At_Route_Line_Creation);
			setRouteEstimationFac(7);
			// set photo encoding to image/jpeg if available
			//#if polish.api.mmapi
			String encodings = null;
			try {
				encodings = System.getProperty("video.snapshot.encodings");
				logger.debug("Encodings: " + encodings); 
			} catch (Exception e) {
				logger.info("Device does not support the encoding property");
			}
			String [] encStrings = new String[0];
			String setEnc = "encoding=image/jpeg";
			if (encodings != null) {
				encStrings = StringTokenizer.getArray(encodings, " ");
				for (int i = 0; i < encStrings.length; i++) {
					logger.debug("Enc: " + encStrings[i]);
					if (setEnc.equalsIgnoreCase(encStrings[i])) {
						setPhotoEncoding(setEnc);
						logger.debug("Set Enc: " + encStrings[i]);
					}
				}
			}
			//#endif
			// set default location provider to JSR-179 if available
			//#if polish.api.locationapi
			//#if polish.android
			setLocationProvider(LOCATIONPROVIDER_ANDROID);
			//#else
			if (getDeviceSupportsJSR179()) {
				setLocationProvider(LOCATIONPROVIDER_JSR179);
			}
			//#endif
			//#endif
			//#debug info
			logger.info("Default config for version 0.4.0+ set.");
		}
		if (configVersionStored < 3) {
			cfgBits_0_to_63 |=	1L << CFGBIT_SND_CONNECT |
				   		1L << CFGBIT_SND_DISCONNECT |
				   		1L << CFGBIT_SND_ROUTINGINSTRUCTIONS |
				   		1L << CFGBIT_SND_DESTREACHED |
				   		1L << CFGBIT_SHOW_POINT_OF_COMPASS |
				   		1L << CFGBIT_AREAS |
				   		1L << CFGBIT_ROUTE_AUTO_RECALC;

			// Auto-reconnect GPS
			setBtAutoRecon(true);
			// make MOVE_UP map the default
			setProjTypeDefault(ProjFactory.MOVE_UP);
			//#debug info
			logger.info("Default config for version 3+ set.");
		}
		if (configVersionStored < 5) {
			cfgBits_0_to_63 |=	1L << CFGBIT_PLACETEXTS |
						1L << CFGBIT_SPEEDALERT_SND |
						1L << CFGBIT_ROUTE_HIDE_QUIET_ARROWS |
						1L << CFGBIT_SHOW_SCALE_BAR |
						1L << CFGBIT_SPEEDALERT_VISUAL;
			setMinRouteLineWidth(3);
			// Speed alert tolerance
			setSpeedTolerance(5);
			//#debug info
			logger.info("Default config for version 5+ set.");
		}
		if (configVersionStored < 6) {
			setAutoRecenterToGpsMilliSecs(30000);
			cfgBits_0_to_63 |=	1L << CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED;
			logger.info("Default config for version 6+ set.");
//			if (getPhoneModel().startsWith("MicroEmulator")) {
//				cfgBits |= 	1L<<CFGBIT_ICONMENUS |
//							1L<<CFGBIT_ICONMENUS_FULLSCREEN;
//			}
		}
		if (configVersionStored < 7) {
			cfgBits_0_to_63 |=	1L << CFGBIT_SHOW_SPEED_IN_MAP |
						1L << CFGBIT_AUTO_START_GPS;
		}
		
		if (configVersionStored < 9) {
			cfgBits_0_to_63 |=	1L << CFGBIT_METRIC |
								1L << CFGBIT_SHOW_ROUTE_DURATION_IN_MAP |
								1L << CFGBIT_SHOW_OFF_ROUTE_DISTANCE_IN_MAP |
								1L << CFGBIT_SHOW_AIR_DISTANCE_IN_MAP |
								1L << CFGBIT_ICONMENUS |
				// 1L << CFGBIT_ICONMENUS_ROUTING_OPTIMIZED |
								1L << CFGBIT_ICONMENUS_FULLSCREEN;

			cfgBits_64_to_127 |=	1L << CFGBIT_SHOW_ALTITUDE_IN_MAP |
									1L << CFGBIT_SHOW_ETA_IN_MAP |
									1L << CFGBIT_BUILDINGS |
									1L << CFGBIT_BUILDING_LABELS |
									1L << CFGBIT_ICONMENUS_MAPPED_ICONS;
									
		}

		if (configVersionStored < 10) {
			setMainStreetDistanceKm(2);
		}
		
		if (configVersionStored < 11) {
			if (getDefaultIconMenuBackCmdSupport()) {
				cfgBits_64_to_127 |=	1L << CFGBIT_ICONMENUS_BACK_CMD;
			}
			
		}

		if (configVersionStored < 13) {
			// migrate boolean stopAllWhileRouteing to int continueMapWhileRouteing
			if (continueMapWhileRouteing == 0) {
				continueMapWhileRouteing = continueMap_Always;
			} else {
				continueMapWhileRouteing = continueMap_At_Route_Line_Creation;
			}
		}

		if (configVersionStored < 14) {
			cfgBits_64_to_127 |=	1L << CFGBIT_AUTOZOOM |
									1L << CFGBIT_ROUTE_TRY_FIND_MOTORWAY |
									1L << CFGBIT_ROUTE_BOOST_MOTORWAYS |
									1L << CFGBIT_ROUTE_BOOST_TRUNKS_PRIMARYS;
		}

		if (configVersionStored < 15) {
			// This bit was recycled as "backlight keep-alive only",
			// so it should be off by default.
			if (getCfgBitState(CFGBIT_BACKLIGHT_ONLY_KEEPALIVE))
			{
				cfgBits_0_to_63 ^= 1L << CFGBIT_BACKLIGHT_ONLY_KEEPALIVE;
			}
			//cfgBits_64_to_127 |= 1L << CFGBIT_WAYPT_OFFER_PREDEF;
			wayptSortMode = WAYPT_SORT_MODE_NEW_FIRST;
			setWaypointSortMode(wayptSortMode);
		}

		if (configVersionStored < 16) {
			cfgBits_0_to_63 |= 1L << CFGBIT_USE_TURN_RESTRICTIONS_FOR_ROUTE_CALCULATION;
			cfgBits_0_to_63 |= 1L << CFGBIT_WPTS_IN_TRACK;
			setTrafficSignalCalcDelay(5);
		}

		if (configVersionStored < 17) {
			backLightLevel = 50;
			setBackLightLevel(backLightLevel);
			setPhoneAllTimeMaxMemory(0);
			setBaseScale(23);
		}
		if (configVersionStored < 18) {
			setUiLang("devdefault");
			setNaviLang("en");
			setOnlineLang("en");
			setWikipediaLang("en");
			setNamesOnMapLang("en");
		}
		if (configVersionStored < 21) {
			cfgBits_64_to_127 |= 1L << CFGBIT_ROUTE_USE_MOTORWAYS |
								 1L << CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD |
								 1L << CFGBIT_AUTOSAVE_DESTPOS |
			 					 1L << CFGBIT_ROUTE_USE_TOLLROADS |
			 					 1L << CFGBIT_ONLINE_GEOHACK |
			 					 1L << CFGBIT_ONLINE_WEATHER |
			 					 1L << CFGBIT_ONLINE_PHONE |
			 					 1L << CFGBIT_ONLINE_WEBSITE |
			 					 1L << CFGBIT_ONLINE_WIKIPEDIA_RSS |
			 					 1L << CFGBIT_MAPTAP_DOUBLE |
			 					 1L << CFGBIT_MAPTAP_SINGLE;			 					 
		}
		if (configVersionStored < 23) {
			//#if polish.api.bigsearch
			setSearchMax(5000);
			//#else
			setSearchMax(isAndroid() ? 5000 : 500);
			//#endif
		}
		if (configVersionStored < 24) {
			cfgBits_64_to_127 |= 1L << CFGBIT_TICKER_ISEARCH;
		}
		if (configVersionStored < 25) {
			cfgBits_64_to_127 |= 1L << CFGBIT_ONLINE_TOPOMAP;
		}
		if (configVersionStored < 26) {
			// 10 km
			setPoiSearchDistance(10f);
		}
		if (configVersionStored < 27) {
			setDestLineWidth(2);
		}
		if (configVersionStored < 28) {
			setTimeDiff(0);
		}
		if (configVersionStored < 29) {
			cfgBits_64_to_127 |= 1L << CFGBIT_NAVI_ARROWS_IN_MAP;
		}
		if (configVersionStored < 30) {
			//#if polish.android
			cfgBits_64_to_127 |= 
				1L << CFGBIT_NOSTREETBORDERS |
				1L << CFGBIT_ROUND_WAY_ENDS;
			//#endif
			setAltitudeCorrection(0);
		}
		if (configVersionStored < 31) {
			//#if polish.api.online
			setCfgBitSavedState(CFGBIT_INTERNET_ACCESS, true);
			//#endif			
			//#if polish.api.finland
			setCfgBitSavedState(CFGBIT_PREFER_OUTLINE_AREAS, true);
			//#endif
		}
		if (configVersionStored < 32) {
			//#if polish.android
			setCfgBitSavedState(CFGBIT_PREFER_OUTLINE_AREAS, true);
			setCfgBitSavedState(CFGBIT_COMPASS_ALWAYS_ROTATE, true);
			setCfgBitSavedState(CFGBIT_CLICKABLE_MAPOBJECTS, true);
			setCfgBitSavedState(CFGBIT_SHOW_ACCURACY, true);
			setCfgBitSavedState(CFGBIT_COMPASS_DIRECTION, true);
			setCfgBitSavedState(CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION, true);
			setCfgBitSavedState(CFGBIT_ICONMENUS_FULLSCREEN, false);
			setCfgBitSavedState(CFGBIT_SEARCH_SHOW_NATIVE_KEYBOARD, true);
			//#else
			setCfgBitSavedState(CFGBIT_ICONMENUS_SETUP, getCfgBitState(CFGBIT_ICONMENUS));
			//#endif
			setCfgBitSavedState(CFGBIT_SHOW_MAP_CREDITS, true);
			setCfgBitSavedState(CFGBIT_DISTANCE_VIEW, true);
		}

		
		setCfgBits(cfgBits_0_to_63, cfgBits_64_to_127, cfgBits_128_to_191);
	}

	private final static String sanitizeString(String s) {
		if (s == null) {
			return "!null!";
		}
		return s;
	}
	
	private final static String desanitizeString(String s) {
		if (s.equalsIgnoreCase("!null!") || s.equalsIgnoreCase(Locale.get("configuration.nullexclmark")/*!null!*/)) {
			return null;
		}
		return s;
	}
	
	private static void write(String s, int idx) {
		writeBinary(sanitizeString(s).getBytes(), idx);
		//#debug info
		logger.info("wrote " + s + " to " + idx);
	}
	
	private static void writeBinary(byte [] data, int idx) {
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			while (database.getNumRecords() < idx) {
				database.addRecord(empty, 0, empty.length);
			}
			database.setRecord(idx, data, 0, data.length);
			database.closeRecordStore();
			//#debug info
			logger.info("wrote binary data to " + idx);
		} catch (Exception e) {
			logger.exception(Locale.get("configuration.CouldNotWriteData")/*Could not write data*/ + " (idx = " + idx + ") " + Locale.get("configuration.ToRecordstore")/*to recordstore*/, e);
		}
	}
	
	private static void write(int i, int idx) {
		write("" + i, idx);
	}

	private static void write(long i, int idx) {
		write("" + i, idx);
	}

	private static byte [] readBinary(RecordStore database, int idx) {
		try {
			byte[] data;
			try {
				data = database.getRecord(idx);
			}
			catch (InvalidRecordIDException irie) {
				logger.silentexception("Failed to read recordstore entry " + idx, irie);
				//Use defaults
				return null;
			}
			
			return data;
		} catch (Exception e) {
			logger.exception(Locale.get("configuration.FailedToReadBinaryFromConfig")/*Failed to read binary from config database*/, e);
			return null;
		}
	}

	private static String readString(RecordStore database, int idx) {
		byte [] data = readBinary(database, idx);
		if (data == null || data.length == 0 ) {
			return null;
		}
		String ret = desanitizeString(new String(data));
		//#debug info
		logger.info("Read from config database " + idx + ": " + ret);
		return ret;
	}

	private static int readInt(RecordStore database, int idx) {
		try {
			String tmp = readString(database, idx);
			//#debug info
			logger.info("Read from config database " + idx + ": " + tmp);
			if (tmp == null) {
				return 0;
			} else {
				return Integer.parseInt(tmp);
			}
		} catch (Exception e) {
			logger.exception(Locale.get("configuration.FailedReadingInt")/*Failed to read int from config database*/, e);
			return 0;
		}
	}
	
	private static long readLong(RecordStore database, int idx) {
		try {
			String tmp = readString(database, idx);
			//#debug info
			logger.info("Read from config database " + idx + ": " + tmp);
			if (tmp == null) {
				return 0;
			} else {
				return Long.parseLong(tmp);
			}
		} catch (Exception e) {
			logger.exception(Locale.get("configuration.FailedReadingLong")/*Failed to read Long from config database*/, e);
			return 0;
		}
	}
	
	
	private static void readPosition(RecordStore database, Node pos, int rec_id_radlat, int rec_id_radlon) {
		String s = readString(database, rec_id_radlat);
		String s2 = readString(database, rec_id_radlon);
		if (s != null && s2 != null) {
			try {
				pos.radlat = Float.parseFloat(s);
				pos.radlon = Float.parseFloat(s2);
			} catch (NumberFormatException nfe) {
				logger.exception(Locale.get("configuration.ErrorParsingPos")/*Error parsing pos: */, nfe);					
			}
		}
	}
	

	
	public static String getGpsRawLoggerUrl() {
		return rawGpsLogUrl;
	}
	
	public static void setGpsRawLoggerUrl(String url) {
		rawGpsLogUrl = url;
		write(rawGpsLogUrl, RECORD_ID_LOG_RAW_GPS_URL);
	}
	
	public static void setGpsRawLoggerEnable(boolean enabled) {
		rawGpsLogEnable = enabled;
		if (rawGpsLogEnable) {
			write(1, RECORD_ID_LOG_RAW_GPS_ENABLE);
		} else {
			write(0, RECORD_ID_LOG_RAW_GPS_ENABLE);
		}
	}
	
	public static boolean getDebugRawLoggerEnable() {
		return rawDebugLogEnable;
	}
	
	public static String getLocaleLang() {
		return localeLang;
	}

	public static String getOnlineLangString() {
		String lang = onlineLang;
		if (onlineLang.equalsIgnoreCase("devdefault")) {
			if (localeLang != null) {
				lang = localeLang;
			} else {
				if (Legend.numOnlineLang > 1) {
					lang = Legend.onlineLang[1];
				} else {
					lang = "en";
				}
			}
		}
		return lang;
	}

	public static String getDebugRawLoggerUrl() {
		return rawDebugLogUrl;
	}
	
	public static void setDebugRawLoggerUrl(String url) {
		rawDebugLogUrl = url;
		write(rawDebugLogUrl, RECORD_ID_LOG_DEBUG_URL);
	}
	
	public static void setDebugSeverityInfo(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x01;
		} else {
			debugSeverity &= ~0x01;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public static boolean getDebugSeverityInfo() {
		return ((debugSeverity & 0x01) > 0);
	}
	
	public static void setDebugSeverityDebug(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x02;
		} else {
			debugSeverity &= ~0x02;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public static boolean getDebugSeverityDebug() {
		return ((debugSeverity & 0x02) > 0);
	}
	
	public static void setDebugSeverityTrace(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x04;
		} else {
			debugSeverity &= ~0x04;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public static boolean getDebugSeverityTrace() {
		return ((debugSeverity & 0x04) > 0);
	}
	
	public static void setDebugRawLoggerEnable(boolean enabled) {
		rawDebugLogEnable = enabled;
		if (rawDebugLogEnable) {
			write(1, RECORD_ID_LOG_DEBUG_ENABLE);
		} else {
			write(0, RECORD_ID_LOG_DEBUG_ENABLE);
		}
	}
	
	public static boolean getGpsRawLoggerEnable() {
		return rawGpsLogEnable;
	}

	public static String getBtUrl() {
		return btUrl;
	}

	public static void setBtUrl(String btUrl) {
		Configuration.btUrl = btUrl;
		write(btUrl, RECORD_ID_BT_URL);
	}

	public static int getLocationProvider() {
		return locationProvider;
	}

	public static void setLocationProvider(int locationProvider) {
		Configuration.locationProvider = locationProvider;
		write(locationProvider, RECORD_ID_LOCATION_PROVIDER);
	}

	public static int getGpxRecordRuleMode() {
		return gpxRecordRuleMode;
	}

	public static void setGpxRecordRuleMode(int gpxRecordRuleMode) {
		Configuration.gpxRecordRuleMode = gpxRecordRuleMode;
			write(gpxRecordRuleMode, RECORD_ID_GPX_FILTER_MODE);
	}

	public static int getGpxRecordMinMilliseconds() {
		return gpxRecordMinMilliseconds;
	}

	public static void setGpxRecordMinMilliseconds(int gpxRecordMinMilliseconds) {
		Configuration.gpxRecordMinMilliseconds = gpxRecordMinMilliseconds;
			write(gpxRecordMinMilliseconds, RECORD_ID_GPX_FILTER_TIME);
	}

	public static int getGpxRecordMinDistanceCentimeters() {
		return gpxRecordMinDistanceCentimeters;
	}

	public static void setGpxRecordMinDistanceCentimeters(int gpxRecordMinDistanceCentimeters) {
		Configuration.gpxRecordMinDistanceCentimeters = gpxRecordMinDistanceCentimeters;
			write(gpxRecordMinDistanceCentimeters, RECORD_ID_GPX_FILTER_DIST);
	}

	public static int getGpxRecordAlwaysDistanceCentimeters() {
		return gpxRecordAlwaysDistanceCentimeters;
	}

	public static void setGpxRecordAlwaysDistanceCentimeters(int gpxRecordAlwaysDistanceCentimeters) {
		Configuration.gpxRecordAlwaysDistanceCentimeters = gpxRecordAlwaysDistanceCentimeters;
			write(gpxRecordAlwaysDistanceCentimeters, RECORD_ID_GPX_FILTER_ALWAYS_DIST);
	}
	
	public static void setPhoneAllTimeMaxMemory(long i) {
		phoneAllTimeMaxMemory = i;
		write(i, RECORD_ID_PHONE_ALL_TIME_MAX_MEMORY);
	}
	
	public static void setSearchMax(int max) {
		searchMax = max;
		write(max, RECORD_ID_SEARCH_MAX);
	}

	public static void setPoiSearchDistance(float dist) {
		poiSearchDist = (int) (dist * 10f);
		write(poiSearchDist, RECORD_ID_POI_SEARCH_DIST);
	}

	public static boolean getCfgBitState(short bit, boolean getDefault) {
		if (bit < 64) {
			if (getDefault) {
				return ((cfgBitsDefault_0_to_63 & (1L << bit)) != 0);
			} else {
				return ((cfgBits_0_to_63 & (1L << bit)) != 0);
			}
		} else if (bit < 128) {
			if (getDefault) {
				return ((cfgBitsDefault_64_to_127 & (1L << (bit - 64) )) != 0);
			} else {
				return ((cfgBits_64_to_127 & (1L << (bit - 64) )) != 0);
			}
		} else {
			if (getDefault) {
				return ((cfgBitsDefault_128_to_191 & (1L << (bit - 128) )) != 0);
			} else {
				return ((cfgBits_128_to_191 & (1L << (bit - 128) )) != 0);
			}
		}
	}

	public static boolean getCfgBitState(short bit) {
		return getCfgBitState(bit, false);
	}
	
	public static boolean getCfgBitSavedState(short bit) {
		return getCfgBitState(bit, true);
	}

	public static void toggleCfgBitState(short bit, boolean savePermanent) {
		setCfgBitState(bit, !getCfgBitState(bit), savePermanent);
	}
	
	public static void setCfgBitState(short bit, boolean state, boolean savePermanent) {
		if (bit < 64) {
			// set bit
			Configuration.cfgBits_0_to_63 |= (1L << bit);
			if (!state) {
				// clear bit
				Configuration.cfgBits_0_to_63 ^= (1L << bit);
			}
			if (savePermanent) {
				Configuration.cfgBitsDefault_0_to_63 |= (1L << bit);
				if (!state) {
					// clear bit
					Configuration.cfgBitsDefault_0_to_63 ^= (1L << bit);
				}
				write(cfgBitsDefault_0_to_63, RECORD_ID_CFGBITS_0_TO_63);
			}
		} else if (bit < 128) {
			bit -= 64;
			// set bit
			Configuration.cfgBits_64_to_127 |= (1L << bit);
			if (!state) {
				// clear bit
				Configuration.cfgBits_64_to_127 ^= (1L << bit);
			}
			if (savePermanent) {
				Configuration.cfgBitsDefault_64_to_127 |= (1L << bit);
				if (!state) {
					// clear bit
					Configuration.cfgBitsDefault_64_to_127 ^= (1L << bit);
				}
				write(cfgBitsDefault_64_to_127, RECORD_ID_CFGBITS_64_TO_127);
			}
		} else {
			bit -= 128;
			// set bit
			Configuration.cfgBits_128_to_191 |= (1L << bit);
			if (!state) {
				// clear bit
				Configuration.cfgBits_128_to_191 ^= (1L << bit);
			}
			if (savePermanent) {
				Configuration.cfgBitsDefault_128_to_191 |= (1L << bit);
				if (!state) {
					// clear bit
					Configuration.cfgBitsDefault_128_to_191 ^= (1L << bit);
				}
				write(cfgBitsDefault_128_to_191, RECORD_ID_CFGBITS_128_TO_191);
			}
		}
	}

	public static void setCfgBitSavedState(short bit, boolean state) {
		setCfgBitState(bit, state, true);
	}
	
	private static void setCfgBits(long cfgBits_0_to_63, long cfgBits_64_to_127, long cfgBits_128_to_191) {
		Configuration.cfgBits_0_to_63 = cfgBits_0_to_63;
		Configuration.cfgBitsDefault_0_to_63 = cfgBits_0_to_63;
		write(cfgBitsDefault_0_to_63, RECORD_ID_CFGBITS_0_TO_63);
		
		Configuration.cfgBits_64_to_127 = cfgBits_64_to_127;
		Configuration.cfgBitsDefault_64_to_127 = cfgBits_64_to_127;
		write(cfgBitsDefault_64_to_127, RECORD_ID_CFGBITS_64_TO_127);

		Configuration.cfgBits_128_to_191 = cfgBits_128_to_191;
		Configuration.cfgBitsDefault_128_to_191 = cfgBits_128_to_191;
		write(cfgBitsDefault_128_to_191, RECORD_ID_CFGBITS_128_TO_191);
	}
	
	public static int getDetailBoost() {
		return detailBoost;
	}
	
	public static int getDetailBoostPOI() {
		return detailBoostPOI;
	}

	public static void setDetailBoost(int detailBoost, boolean savePermanent) {
		Configuration.detailBoost = detailBoost;
		calculateDetailBoostMultipliers();
		if (savePermanent) {
			Configuration.detailBoostDefault = detailBoost;
			write(detailBoost, RECORD_ID_DETAIL_BOOST);
		}
	}

	public static void setDetailBoostPOI(int detailBoost, boolean savePermanent) {
		Configuration.detailBoostPOI = detailBoost;
		calculateDetailBoostMultipliers();
		if (savePermanent) {
			Configuration.detailBoostDefaultPOI = detailBoost;
			write(detailBoost, RECORD_ID_DETAIL_BOOST_POI);
		}
	}

	
	public static float getDetailBoostMultiplier() {
		return detailBoostMultiplier;
	}

	public static float getMaxDetailBoostMultiplier() {
		if (detailBoost >= detailBoostPOI) {
			return detailBoostMultiplier;
		}
		return detailBoostMultiplierPOI;
	}

	public static float getDetailBoostMultiplierPOI() {
		return detailBoostMultiplierPOI;
	}
	
	public static int getDetailBoostDefault() {
		return detailBoostDefault;
	}

	public static int getDetailBoostDefaultPOI() {
		return detailBoostDefaultPOI;
	}

    /**	There's no pow()-function in J2ME so this manually calculates
     * 1.5 ^ detailBoost to get factor to multiply with zoom level limits
    **/
	private static void calculateDetailBoostMultipliers() {
		detailBoostMultiplier = 1;
		for (int i = 1; i <= detailBoost; i++) {
			detailBoostMultiplier *= 1.5;
		}
		detailBoostMultiplierPOI = 1;
		for (int i = 1; i <= detailBoostPOI; i++) {
			detailBoostMultiplierPOI *= 1.5;
		}
	}
	
	public static boolean setUiLang(String uiLang) {
		boolean devDefaultChanged = false;
		String uiLangUse = uiLang;
		// get phone's locale
		String locale = System.getProperty("microedition.locale");
		if (locale != null) {
			if (!locale.substring(0,2).equals(localeLang)) {
				// locale has been changed 
			localeLang = locale.substring(0,2);
			devDefaultChanged = true;		
			}
		}
		if (uiLang.equalsIgnoreCase("devdefault")) {
			if (localeLang != null) {
				uiLangUse = localeLang;
			} else {
				if (Legend.numUiLang > 1) {
					uiLangUse = Legend.uiLang[1];
				} else {
					uiLangUse = "en";
				}
			}
		}
		if (!uiLangLoaded || !uiLangUse.equals(Configuration.uiLang) || devDefaultChanged) {
			try {
				Locale.loadTranslations( "/" + uiLangUse + ".loc" );
			} catch (IOException ioe) {
				System.out.println("Couldn't open translations file: " + uiLangUse);
				// FIXME check if logger initialized and do this if it is
				// logger.error("Couldn't set language to " + uiLangUse + ", defaulting to English");
				try {
					Locale.loadTranslations( "/en.loc" );
				} catch (IOException ioe2) {
					// shouldn't happen
					System.out.println("Couldn't open translations file for English");
				}
				uiLangUse = "en";
			}
			
			//#if polish.android
			LOCATIONPROVIDER = new String[6];
			//#else
			LOCATIONPROVIDER = new String[5];
			//#endif
			LOCATIONPROVIDER[LOCATIONPROVIDER_NONE] = Locale.get("configuration.LPNone")/*None*/;
			LOCATIONPROVIDER[LOCATIONPROVIDER_SIRF] = Locale.get("configuration.LPBluetoothSirf")/*Bluetooth (Sirf)*/;
			LOCATIONPROVIDER[LOCATIONPROVIDER_NMEA] = Locale.get("configuration.LPBluetoothNMEA")/*Bluetooth (NMEA)*/;
			LOCATIONPROVIDER[LOCATIONPROVIDER_JSR179] = Locale.get("configuration.LPInternalJSR179")/*Internal (JSR179)*/;
			LOCATIONPROVIDER[LOCATIONPROVIDER_SECELL] = Locale.get("configuration.LPCellID")/*Cell-ID (OpenCellId.org)*/;
			//#if polish.android
			LOCATIONPROVIDER[LOCATIONPROVIDER_ANDROID] = Locale.get("configuration.Android")/*Android*/;
			//#endif

			projectionsString = new String[ProjFactory.COUNT];
			projectionsString[ProjFactory.NORTH_UP] = Locale.get("projfactory.NorthUp")/*North Up*/;
			projectionsString[ProjFactory.MOVE_UP] = Locale.get("projfactory.Moving")/*Moving*/;
			projectionsString[ProjFactory.MOVE_UP_ENH] = Locale.get("projfactory.MovingEnhanced")/*MovingEnhanced*/;
			projectionsString[ProjFactory.EAGLE] = Locale.get("projfactory.Eagle")/*Eagle*/;

			if (uiLangLoaded) {
				Trace.uncacheIconMenu();
				GuiDiscover.uncacheIconMenu();
			}
			initCompassDirections();
	
			uiLangLoaded = true;
		}
		if (!uiLangLoaded || !uiLang.equals(Configuration.uiLang)) {
			Configuration.uiLang = uiLang;
			write(uiLang, RECORD_ID_UI_LANG);
		}
		return true;
	}

	public static String getUiLang() {
		return uiLang;
	}

	public static void setNaviLang(String naviLang) {
		Configuration.naviLang = naviLang;
		write(naviLang, RECORD_ID_NAVI_LANG);
	}

	public static String getNaviLang() {
		return naviLang;
	}

	public static void setOnlineLang(String onlineLang) {
		Configuration.onlineLang = onlineLang;
		write(onlineLang, RECORD_ID_ONLINE_LANG);
	}

	public static String getOnlineLang() {
		return onlineLang;
	}

	public static void setWikipediaLang(String wikipediaLang) {
		Configuration.wikipediaLang = wikipediaLang;
		write(wikipediaLang, RECORD_ID_WP_LANG);
	}

	public static String getWikipediaLang() {
		return wikipediaLang;
	}

	public static void setNamesOnMapLang(String namesOnMapLang) {
		Configuration.namesOnMapLang = namesOnMapLang;
		write(namesOnMapLang, RECORD_ID_NAME_LANG);
	}

	public static String getNamesOnMapLang() {
		return namesOnMapLang;
	}

	public static void setGpxUrl(String url) {
		Configuration.gpxUrl = url;
		write(url, RECORD_ID_GPX_URL);
	}

	public static String getGpxUrl() {
		return gpxUrl;
	}
	
	public static void setPhotoUrl(String url) {
		Configuration.photoUrl = url;
		write(url, RECORD_ID_PHOTO_URL);
	}

	public static String getPhotoUrl() {
		return photoUrl;
	}
	
	public static void setPhotoEncoding(String encoding) {
		photoEncoding = encoding;
		write(encoding, RECORD_ID_PHOTO_ENCODING);
	}

	public static String getPhotoEncoding() {
		return photoEncoding;
	}
	
	public static boolean usingBuiltinMap() {
		return mapFromJar;
	}
	
	public static void setBuiltinMap(boolean mapFromJar) {
		write(mapFromJar ? 0 : 1, RECORD_ID_MAP_FROM_JAR);
		Configuration.mapFromJar = mapFromJar;
	}
	
	public static String getMapUrl() {
		return mapFileUrl;
	}
	
	public static void setMapUrl(String url) {
		write(url, RECORD_ID_MAP_FILE_URL);
		mapFileUrl = url;
		Routing.dropToConnectionsCache();
	}

	public static String getSmsRecipient() {
		return smsRecipient;
	}
	
	public static void setSmsRecipient(String s) {
		write(s, RECORD_ID_SMS_RECIPIENT);
		smsRecipient = s;
	}
	
	public static int getSpeedTolerance() {
		return speedTolerance;
	}
	
	public static void setSpeedTolerance(int s) {
		write(s, RECORD_ID_SPEED_TOLERANCE);
		speedTolerance = s;
	}

	public static int getMinRouteLineWidth() {
		return minRouteLineWidth;
	}

	public static int getMainStreetDistanceKm() {
		return mainStreetDistanceKm;
	}
	
	
	public static void setMinRouteLineWidth(int w) {
		minRouteLineWidth = Math.max(w, 1);
		write(minRouteLineWidth, RECORD_ID_MIN_ROUTELINE_WIDTH);
	}

	public static void setMainStreetDistanceKm(int km) {
		mainStreetDistanceKm = km;
		write(mainStreetDistanceKm, RECORD_ID_MAINSTREET_NET_DISTANCE_KM);
	}

	
	public static int getAutoRecenterToGpsMilliSecs() {
		return autoRecenterToGpsMilliSecs;
	}

	public static void setAutoRecenterToGpsMilliSecs(int ms) {
		autoRecenterToGpsMilliSecs = ms;
		write(autoRecenterToGpsMilliSecs, RECORD_ID_AUTO_RECENTER_TO_GPS_MILLISECS);
	}
	
	public static void closeMapZipFile() {
		mapZipFile = null;
	}

	// set some defaults for this device; will be done only at first install
	// or after configuration has been reset to factory settings
	public static void setCanvasSpecificDefaults(int width, int height) {
		if (!getCfgBitState(CFGBIT_CANVAS_SPECIFIC_DEFAULTS_DONE)) {
			if (width > 219) {
				// if the map display is wide enough, show the clock in the map screen by default
				setCfgBitSavedState(CFGBIT_SHOW_CLOCK_IN_MAP, true);
				// if the map display is wide enough, use big tab buttons by default
				setCfgBitSavedState(CFGBIT_ICONMENUS_BIG_TAB_BUTTONS, true);
			}
			if (Math.min(width, height) > 300) {
				setCfgBitSavedState(CFGBIT_NAVI_ARROWS_BIG, true);
				setCfgBitSavedState(CFGBIT_SHOW_TRAVEL_MODE_IN_MAP, true);
			}
			if (Math.min(width, height) > 400) {
				setBaseScale(24);
				setMinRouteLineWidth(5);
			}
			if (Math.min(width, height) >= 800) {
				setBaseScale(26);
				setMinRouteLineWidth(6);
			}
			if (getHasPointerEvents()) {
				setCfgBitSavedState(CFGBIT_LARGE_FONT, true);
			}
			setCfgBitSavedState(CFGBIT_CANVAS_SPECIFIC_DEFAULTS_DONE, true);
		}
	}
	/**
	 * Opens a resource, either from the JAR, the file system or a ZIP archive,
	 * depending on the configuration, see mapFromJar and mapFileUrl.
	 * @param name Full path of the resource
	 * @return Stream which reads from the resource
	 * @throws IOException if file could not be found
	 */
	public static InputStream getMapResource(String name) throws IOException {
		InputStream is = null;
		if (name.toLowerCase().endsWith(".dat")) {
			// backwards compatibility - remove the enableMap68Filenames test after a time period
			if (Legend.enableMap68Filenames && !name.toLowerCase().equals("/legend.dat")) {
				name = "/dat" + name;
			}
		}
		if (mapFromJar
			|| (Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS)
				&& name.toLowerCase().endsWith(".png"))
		) {
			//#if polish.android
			//#debug debug
			logger.debug("Opening file as android asset: " + name);
			if (assets == null) {
				assets = MidletBridge.instance.getAssets();
			}
			is = assets.open(name.substring(1));
			//#else
			//#debug debug
			logger.debug("Opening file from JAR: " + name);
			is = QueueReader.class.getResourceAsStream(name);
			//#endif
			if (is != null) {
				if (getCfgBitState(CFGBIT_BUFFEREDINPUTSTREAM)) {
					return new BufferedInputStream(is, 512);
				}
				return is;
			} else if (!Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS)) {
				// getResourceAsStream() simply returns null on SE JP-5 phones if 
				// file not found, but some callers of getMapResource() only handle IOEs.
				throw new IOException(Locale.get("configuration.ExFNFCouldnt")
						/*Could not find file */ + name +
						Locale.get("configuration.ExFNFJar")/* in JAR.*/);
			}
		}
		if (mapFileUrl == null) {
			throw new IOException("mapFileUrl is null");					
		}
		//#if polish.api.fileconnection
		if (mapFileUrl.endsWith("/")) {
			//#if polish.android
			try {
				// directory mode
				name = mapFileUrl + name.substring(1);
				//#debug debug
				logger.debug("Opening file from filesystem: " + name);
				// strip file:/// prefix
				File f = new File(name.substring(8));
				is = new FileInputStream(f);
				if (is == null)	{
					// This is just to safeguard against the case that an implementation
					// might return null instead of throwing an IOE, see above.
					throw new IOException();
				}
				// FIXME: Android: test, if this works on Android before committing
				if (getCfgBitState(CFGBIT_BUFFEREDINPUTSTREAM)) {
					return new BufferedInputStream(is, 512);
				}
				return is;
			} catch (IOException ioe) {
				throw new IOException(Locale.get("configuration.ExFNFCouldnt")
						/*Could not find file */ + name +
						Locale.get("configuration.ExFNFFilesys")/* in file system.*/);					
			}
			//#else
			try {
				// directory mode
				name = mapFileUrl + name.substring(1);
				//#debug debug
				logger.debug("Opening file from filesystem: " + name);
				FileConnection fc = (FileConnection) Connector.open(name, Connector.READ);
				is = fc.openInputStream();
				if (is == null)	{
					// This is just to safeguard against the case that an implementation
					// might return null instead of throwing an IOE, see above.
					throw new IOException();
				}
				if (getCfgBitState(CFGBIT_BUFFEREDINPUTSTREAM)) {
					return new BufferedInputStream(is, 512);
				}
				return is;
			} catch (IOException ioe) {
				throw new IOException(Locale.get("configuration.ExFNFCouldnt")
						/*Could not find file */ + name +
						Locale.get("configuration.ExFNFFilesys")/* in file system.*/);					
			}
			//#endif
		} else {
			// zipfile mode
			if (mapZipFile == null) {
				mapZipFile = new ZipFile(mapFileUrl, -1);
				if (mapFileUrl.toLowerCase().endsWith(".apk")) {
					zipFileIsApk = true;
				} else {
					zipFileIsApk = false;
				}
			}
			//#debug debug
			logger.debug("Opening file from zip-file: " + name);
			if (zipFileIsApk) {
				name = "/assets" + name;
			}
			is = mapZipFile.getInputStream(mapZipFile.getEntry(name.substring(1)));
			if (is == null)	{
				// getInputStream() simply returns null if file not found,
				// but some callers of getMapResource() only handle IOEs.
				throw new IOException(Locale.get("configuration.ExFNFCouldnt")
						/*Could not find file */ + name +
						Locale.get("configuration.ExFNFZip")/* in ZIP.*/);					
			}
		}
		//#else
		//This should never happen.
		is = null;
		logger.fatal(Locale.get("configuration.ErrorNoFS")
				/*Error, we don't have a filesystem API, but...*/);
		//#endif

		return is;
	}

	public static int getRouteEstimationFac() {
		return routeEstimationFac;
	}

	public static void setRouteEstimationFac(int routeEstimationFac) {
		write(routeEstimationFac, RECORD_ID_ROUTE_ESTIMATION_FAC);
		Configuration.routeEstimationFac = routeEstimationFac;
	}

	
	public static int getDestLineWidth() {
		return destLineWidth;
	}

	public static int getTimeDiff() {
		return timeDiff;
	}

	public static int getAltitudeCorrection() {
		return altitudeCorrection;
	}

	public static void setDestLineWidth(int destLineWidth) {
		write(destLineWidth, RECORD_ID_DEST_LINE_WIDTH);
		Configuration.destLineWidth = destLineWidth;
	}

	public static void setTimeDiff(int timeDiff) {
		write(timeDiff, RECORD_ID_TIME_DIFF);
		Configuration.timeDiff = timeDiff;
	}
	
	public static void setAltitudeCorrection(int correction) {
		write(correction, RECORD_ID_ALTITUDE_CORRECTION);
		Configuration.altitudeCorrection = correction;
	}
	
	public static int getBaseScale() {
		return baseScale;
	}

	public static float getRealBaseScale() {
		return realBaseScale;
	}

	
	public static void setBaseScale(int baseScale) {
		calculateRealBaseScale();
		write(baseScale, RECORD_ID_BASESCALE);
		Configuration.baseScale = baseScale;
	}
	
	public static void calculateRealBaseScale() {
		realBaseScale = baseScale;
		// if we have a pseudo zoom level, calculate the real one
		if (baseScale < 50) {
			// if the pseudo zoom level is 0 use 23
			if (baseScale == 0) {
				baseScale = 23;
			}
			// don't zoom out too much by default
			if (baseScale < 10) {
				baseScale = 10;
			}
			realBaseScale = 15000f;
			for (int scale = 23; baseScale != scale;) {
				if ( baseScale > 23) {
					realBaseScale /= getZoomFactor();
					scale++;
				} else {
					realBaseScale *= getZoomFactor();
					scale--;
				}
			}
		}
		// Base scale in eagle view is zoomed once in
		if (projTypeDefault == ProjFactory.EAGLE) {
			realBaseScale /= getZoomFactor();
		}
	}
	
	
	// TMS maps use a factor of two between scale levels, GpsMid
	// uses 1.5
	// FIXME make necessary changes if GpsMid zoom levels change

	public static float getZoomFactor() {
		return getCfgBitState(Configuration.CFGBIT_TMS_BACKGROUND) ? 2.0f : 1.5f;
	}

	// FIXME should be calculated from basescale etc.
	public static float getRasterScale() {
		// FIXME probably not exactly correct
		return 488f; // seems to work right for east / west direction,
		// but is warped in north/south direction, shows esp. when looked at
		// the top of a raster map north of the Arctic Circle
	}

	public static int getBackLightLevel() {
		return backLightLevel;
	}

	public static void setBackLightLevel(int backLightLevel) {
		write(backLightLevel, RECORD_ID_BACKLIGHTLEVEL);
		Configuration.backLightLevel = backLightLevel;
	}
	
	public static boolean isBackLightDimmable() {
		return Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA) || Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WAKELOCK) || Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WINDOW_MANAGER);
	}
	
	public static void addToBackLightLevel(int backLightLevelIndexDiff) {
		byte[] backLightLevels = {1, 10, 25, 50, 75, 100};
		
		// find index of current backlight level
		int i = 0;
		for (; i < backLightLevels.length && backLightLevels[i] != backLightLevel; i++) {
			;
		}
		
		i += backLightLevelIndexDiff;
		if (i < 0) {
			i = 0;
		} else if ( i >= backLightLevels.length ) {
			i = backLightLevels.length - 1;
		}
		backLightLevel = backLightLevels[i]; 
 
		if (!isBackLightDimmable()) {
			backLightLevel = 100;
		}
		
		setBackLightLevel(backLightLevel);
	}

	
	public static TravelMode getTravelMode() {
		return Legend.getTravelModes()[currentTravelModeNr];
	}
	
	public static int getTravelModeNr() {
		return currentTravelModeNr;
	}

	public static int getTravelMask() {
		return currentTravelMask;
	}
	
	public static void setTravelMode(int travelModeNr) {
		write(travelModeNr, RECORD_ID_ROUTE_TRAVEL_MODE);
		Configuration.currentTravelModeNr = travelModeNr;
		Configuration.currentTravelMask = 1 << travelModeNr;
	}

	
	
	public static int getContinueMapWhileRouteing() {
		return continueMapWhileRouteing;
	}

	public static void setContinueMapWhileRouteing(int continueMapWhileRouteing) {
		write(continueMapWhileRouteing, RECORD_ID_CONTINUE_MAP_WHILE_ROUTING);
		Configuration.continueMapWhileRouteing = continueMapWhileRouteing;
	}
	
	public static boolean getBtKeepAlive() {
		return btKeepAlive;
	}
	
	public static void setBtKeepAlive(boolean keepAlive) {
		write(keepAlive ? 1 : 0, RECORD_ID_BT_KEEPALIVE);
		Configuration.btKeepAlive = keepAlive;
	}
	
	public static boolean getBtAutoRecon() {
		return btAutoRecon;
	}
	
	public static void setBtAutoRecon(boolean autoRecon) {
		write(autoRecon ? 1 : 0, RECORD_ID_GPS_RECONNECT);
		Configuration.btAutoRecon = autoRecon;
	}

	public static void getStartupPos(Node pos) {
		pos.setLatLon(startupPos);
	}
	
	public static void setStartupPos(Node pos) {
		//System.out.println("Save Map startup lat/lon: " + startupPos.radlat*MoreMath.FAC_RADTODEC + "/" + startupPos.radlon*MoreMath.FAC_RADTODEC);
		write(Float.toString(pos.radlat), RECORD_ID_STARTUP_RADLAT);
		write(Float.toString(pos.radlon), RECORD_ID_STARTUP_RADLON);
	}

	public static void getDestPos(Node pos) {
		pos.setLatLon(destPos);
	}

	public static void setDestPos(Node pos) {
		write(Float.toString(pos.radlat), RECORD_ID_DEST_RADLAT);
		write(Float.toString(pos.radlon), RECORD_ID_DEST_RADLON);
	}
	
	public static String getOsmUsername() {
		return osm_username;
	}

	public static void setOsmUsername(String name) {
		osm_username = name;
		write(name, RECORD_ID_OSM_USERNAME);
	}
	
	public static String getOsmPwd() {
		return osm_pwd;
	}

	public static void setOsmPwd(String pwd) {
		osm_pwd = pwd;
		write(pwd, RECORD_ID_OSM_PWD);
	}
	
	public static String getOsmUrl() {
		return osm_url;
	}
	
	public static String getTMSUrl() {
		return tms_url;
	}
	
	public static void setOsmUrl(String url) {
		osm_url = url;
		write(url, RECORD_ID_OSM_URL);
	}

	public static void setTMSUrl(String url) {
		tms_url = url;
		write(url, RECORD_ID_TMS_URL);
	}

	public static String getOpencellidApikey() {
		return opencellid_apikey;
	}

	public static void setOpencellidApikey(String name) {
		opencellid_apikey = name;
		write(name, RECORD_ID_OPENCELLID_APIKEY);
	}

	public static void setProjTypeDefault(byte t) {
		ProjFactory.setProj(t);
		projTypeDefault = t;
		write(t, RECORD_ID_MAP_PROJECTION);
		calculateRealBaseScale();
	}

	public static byte getProjDefault() {
		return projTypeDefault;
	}

	public static String getSoundDirectory() {
		if (soundDirectory == null) {
			soundDirectory = Legend.soundDirectories[0];
		}
		return soundDirectory;
	}

	public static void setSoundDirectory(String soundDir) {
		soundDirectory = soundDir;
		write(soundDir, RECORD_ID_SOUND_DIRECTORY);
	}

	
	
	public static boolean getDeviceSupportsJSR135() {
		//#if polish.api.mmapi
		String jsr135Version = null;
		try {
			jsr135Version = System.getProperty("video.snapshot.encodings");
		} catch (RuntimeException re) {
			/**
			 * Some phones throw exceptions if trying to access properties that don't
			 * exist, so we have to catch these and just ignore them.
			 */
		} catch (Exception e) {
			/**
			 * See above
			 */
		}
		if (jsr135Version != null && jsr135Version.length() > 0) {
			return true;
		}
		//#endif
		return false;
	}
	public static boolean getDeviceSupportsJSR179() {
		//#if polish.api.locationapi
		String jsr179Version = null;
		try {
			jsr179Version = System.getProperty("microedition.location.version");
		} catch (RuntimeException re) {
			/**
			 * Some phones throw exceptions if trying to access properties that don't
			 * exist, so we have to catch these and just ignore them.
			 */
		} catch (Exception e) {
			/**
			 * See above
			 */
		}
		if (jsr179Version != null && jsr179Version.length() > 0) {
			return true;
		}
		//#endif
		return false;
	}
	
	public static boolean hasDeviceJSR120() {
		try {
			Class.forName("javax.wireless.messaging.MessageConnection" );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	public static boolean isSamsungS8000() {
		return isSamsungS8000;
	}
	
	public final static boolean isAndroid() {
		//#if polish.android
			return true;
		//#else
			return false;
		//#endif
	}
	
	public static String getPhoneModel() {
		String  model = null;
		try {
			model = System.getProperty("microedition.platform");
		} catch (RuntimeException re) {
			/**
			 * Some phones throw exceptions if trying to access properties that don't
			 * exist, so we have to catch these and just ignore them.
			 */
		} catch (Exception e) {
			/**
			 * See above
			 */
		}
		if (model != null) {
			isSamsungS8000 = model.startsWith("S8000");
		    return model;
		} else {
//#if polish.android
		    return "Android";
//#else
		    return "";
//#endif
		}
	}
	
	public static byte getDefaultDeviceBacklightMethodCfgBit() {
		// a list of return codes for microedition.platform can be found at:
		// http://www.club-java.com/TastePhone/J2ME/MIDP_Benchmark.jsp

		//#if polish.android
		return CFGBIT_BACKLIGHT_ANDROID_WINDOW_MANAGER;
		//#else
		//#if polish.api.nokia-ui || polish.api.min-siemapi
		String phoneModel = getPhoneModel();
		// determine default backlight method for devices from the wiki
		if (phoneModel.startsWith("Nokia") ||
			phoneModel.startsWith("SonyEricssonC") ||
			phoneModel.startsWith("SonyEricssonK550")
		) {
			return CFGBIT_BACKLIGHT_NOKIA;
		} else if (phoneModel.startsWith("SonyEricssonK750") ||
			phoneModel.startsWith("SonyEricssonW800")
		) {
			return CFGBIT_BACKLIGHT_NOKIAFLASH;
		} else if (phoneModel.endsWith("(NSG)") ||
			    phoneModel.startsWith("SIE")
			) {
				return CFGBIT_BACKLIGHT_SIEMENS;
		} else if (isSamsungS8000 || phoneModel.startsWith("SAMSUNG-S5230")	) {
			return CFGBIT_BACKLIGHT_SAMSUNG;
        }
		//#endif
		return 0;
		//#endif
	}
	
	private static boolean getDefaultIconMenuBackCmdSupport() {
		String phoneModel = getPhoneModel();
		// Nokia phones don't handle the fire button correctly
		// when there is a command specified and they are in
		// fullscreen mode
		if (phoneModel.startsWith("Nokia")) {
			return false;
		} else {
			return true;
		}
	}
	
	public static String getValidFileName(String fileName) {
		return fileName.replace('\\', '_')
		               .replace('/', '_')
		               .replace('>', '_')
		               .replace('<', '_')
		               .replace(':', '_')
		               .replace('?', '_')
		               .replace('*', '_');
	}
	
	public static void initCompassDirections() {
		String compass = Locale.get("configuration.compass")/*N,NNE,NE,ENE,E,ESE,SE,SSE,S,SSW,SW,WSW,W,WNW,NW,NNW*/;
		compass += "," + compass.substring(0, compass.indexOf(","));
		//#debug debug
		logger.debug("compass dirs: " + compass);
		compassDirections = StringTokenizer.getArray(compass, ",");
	}

	public static String getCompassDirection(int course) {
		return compassDirections[(int)(((course % 360 + 11.25f) / 22.5f)) ];
	}

	public static long getPhoneAllTimeMaxMemory() {
		return phoneAllTimeMaxMemory;
	}

	public static int getSearchMax() {
		return searchMax;
	}

	public static float getPoiSearchDistance() {
		return (float) poiSearchDist / 10;
	}

	public static String getUtf8Encoding() {
		final String[] encodings  = { "UTF-8", "UTF8", "utf-8", "utf8", "" };
		
		if (utf8encodingstring != null) {
			return utf8encodingstring;
		}
		StringBuffer sb = new StringBuffer();
		sb.append("Testing String");
		for (int i = 0; i < encodings.length; i++) {
			try {
    			logger.info("Testing encoding " + encodings[i] + ": " + sb.toString().getBytes(encodings[i]));
    			utf8encodingstring = encodings[i];
    			return utf8encodingstring;
    		} catch (UnsupportedEncodingException e) {
    			continue;
    		}
		}
		return "";
	}
	
	public static boolean getHasPointerEvents() {
		return Configuration.hasPointerEvents;
	}

	public static void setHasPointerEvents(boolean hasPointerEvents) {
		Configuration.hasPointerEvents = hasPointerEvents;
	}
	
	public static int getWaypointSortMode() {
		return wayptSortMode;
	}

	public static void setWaypointSortMode(int mode) {
		if (mode >= WAYPT_SORT_MODE_NONE && mode <= WAYPT_SORT_MODE_DISTANCE) {
			wayptSortMode = mode;
			write(wayptSortMode, RECORD_ID_WAYPT_SORT_MODE);
		} else {
			throw new IllegalArgumentException(Locale.get("configuration.WaypointSortModeRangeError")/*Waypoint sort mode out of range!*/);
		}
	}
	
	public static int getTrafficSignalCalcDelay() {
		return trafficSignalCalcDelay ;
	}

	public static void setTrafficSignalCalcDelay(int i) {
		trafficSignalCalcDelay = i;
		write(i, RECORD_ID_TRAFFIC_SIGNAL_CALC_DELAY);
	}

	public static void loadKeyShortcuts(IntTree gameKeys, IntTree singleKeys,
			IntTree repeatableKeys, IntTree doubleKeys, IntTree longKeys,
			IntTree specialKeys, Command [] cmds) {
		logger.info("Loading key shortcuts");
		if (!loadKeyShortcutsDB(gameKeys, singleKeys, repeatableKeys, doubleKeys,
				longKeys, specialKeys, cmds)) {
			loadDefaultKeyShortcuts(gameKeys, singleKeys, repeatableKeys, doubleKeys,
					longKeys, specialKeys, cmds);
		}
	}
	
	private static void loadDefaultKeyShortcuts(IntTree gameKeys, IntTree singleKeys,
			IntTree repeatableKeys, IntTree doubleKeys, IntTree longKeys,
			IntTree specialKeys, Command [] cmds) {
		int keyType = 0;
		//#debug info
		logger.info("Initialising default key shortcuts");
		try {
			InputStream is = getMapResource("/keyMap.txt");
			if (is == null) {
				throw new IOException("keyMap.txt not found");
			}
			InputStreamReader isr = new InputStreamReader(is, getUtf8Encoding());
			BufferedReader br = new BufferedReader(isr);
			String line;
			line = br.readLine();
			while (line != null) {
				line.trim();
				if (line.length() == 0 || line.startsWith(";")) {
					line = br.readLine();
					continue;
				}
				if ((line.length() > 2) && line.charAt(0) == '[') {
					String sectionName = line.substring(1, line.length() - 1);
					if (sectionName.equalsIgnoreCase("repeatable")) {
						logger.debug("Starting repeatable section");
						keyType = 1;
					} else if (sectionName.equalsIgnoreCase("game")) {
						logger.debug("Starting game section");
						keyType = 4;
					} else  if (sectionName.equalsIgnoreCase("single")) {
						logger.debug("Starting single section");
						keyType = 0;
					} else  if (sectionName.equalsIgnoreCase("double")) {
						logger.debug("Starting double section");
						keyType = 2;
					} else  if (sectionName.equalsIgnoreCase("long")) {
						logger.debug("Starting long section");
						keyType = 3 ;
					} else  if (sectionName.equalsIgnoreCase("special")) {
						logger.debug("Starting special section");
						keyType = 5;
					} else {
						logger.info("unknown section: " + sectionName + " falling back to single");
						keyType = 0;
					}
				}
				Vector shortCut = StringTokenizer.getVector(line, "\t");
				if (shortCut.size() == 2) {
					try {
						int keyCode = Integer.parseInt(((String)shortCut.elementAt(0)));
						Command c = cmds[Integer.parseInt(((String)shortCut.elementAt(1)))];
						switch (keyType) {
						case 0: {
							logger.debug("Adding single key shortcut for key: " + keyCode + " and command " + c);
							singleKeys.put(keyCode, c);
							break;
						}
						case 1: {
							logger.debug("Adding repeatable key shortcut for key: " + keyCode + " and command " + c);
							repeatableKeys.put(keyCode, c);
							break;
						}
						case 2: {
							logger.debug("Adding double press key shortcut for key: " + keyCode + " and command " + c);
							doubleKeys.put(keyCode, c);
							break;
						}
						case 3: {
							logger.debug("Adding longpress key shortcut for key: " + keyCode + " and command " + c);
							longKeys.put(keyCode, c);
							break;
						}
						case 4: {
							logger.debug("Adding game action shortcut for key: " + keyCode + " and command " + c);
							gameKeys.put(keyCode, c);
							break;
						}
						case 5: {
							logger.debug("Adding special key shortcut for key: " + keyCode + " and command " + c);
							specialKeys.put(keyCode, c);
							break;
						}
						}
					} catch (NumberFormatException nfe) {
						logger.info("Invalid line in keyMap.txt: " + line + " Error: " +nfe.getMessage());
					} catch (ArrayIndexOutOfBoundsException aioobe) {
						logger.info("Invalid command number in keyMap.txt: " + line + " Error: " + aioobe.getMessage());
					}
				}
				line = br.readLine();
			};
		} catch (IOException ioe) {
			logger.exception(Locale.get("configuration.ErrShortcuts")/*Could not load key shortcuts*/, ioe);
		}
		
	}
	
	private static boolean loadKeyShortcutsDB(IntTree gameKeys, IntTree singleKeys,
			IntTree repeatableKeys, IntTree doubleKeys, IntTree longKeys,
			IntTree specialKeys, Command [] cmds) {
		try {
			//#debug info
			logger.info("Attempting to load keyboard shortcuts from record store");
			RecordStore database = RecordStore.openRecordStore("Receiver", true);
			if (database == null) {
				//#debug info
				logger.info("No database loaded at the moment");
				return false;
			}
			byte [] data = readBinary(database, RECORD_ID_KEY_SHORTCUT);
			if (data == null || data.length == 0) {
				logger.info("Record store did not contain key shortcut entry");
				database.closeRecordStore();
				return false;
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			DataInputStream dis = new DataInputStream(bais);
			
			IntTree keyTree;
			for (int k = 0; k < 6; k++) {
				keyTree = null;
				switch (k) {
				case 0 :
					keyTree = gameKeys;
					break;
				case 1:
					keyTree = singleKeys;
					break;
				case 2:
					keyTree = repeatableKeys;
					break;
				case 3:
					keyTree = doubleKeys;
					break;
				case 4:
					keyTree = longKeys;
					break;
				case 5:
					keyTree = specialKeys;
					break;
				}
				int treeLength = dis.readShort();
				for (int i = 0; i < treeLength; i++) {
					int keyCode = dis.readInt();
					int cmdCode = dis.readInt();
					keyTree.put(keyCode, cmds[cmdCode]);
				}

				dis.readUTF();
			}
		database.closeRecordStore();
		return true;
		} catch (Exception e) {
			logger.exception(Locale.get("configuration.ErrKeyshortcuts")/*Failed to load keyshortcuts*/, e);
			return false;
		}
	}
	
	public static void saveKeyShortcuts(IntTree gameKeys, IntTree singleKeys,
			IntTree repeatableKeys, IntTree doubleKeys, IntTree longKeys,
			IntTree specialKeys, Command [] cmds) {
		//#debug info
		logger.info("Saving key shortcuts");
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		try {
			IntTree keyTree;
			for (int k = 0; k < 6; k++) {
				keyTree = null;
				switch (k) {
				case 0 :
					keyTree = gameKeys;
					break;
				case 1:
					keyTree = singleKeys;
					break;
				case 2:
					keyTree = repeatableKeys;
					break;
				case 3:
					keyTree = doubleKeys;
					break;
				case 4:
					keyTree = longKeys;
					break;
				case 5:
					keyTree = specialKeys;
					break;
				}
				dos.writeShort(keyTree.size());
				for (int i = 0; i < keyTree.size(); i++) {
					int keyCode = keyTree.getKeyIdx(i);
					int cmdCode = -1;
					Command c = (Command)keyTree.getValueIdx(i);
					for (int j = 0; j < cmds.length; j++) {
						if (cmds[j] == c) {
							cmdCode = j;
							break;
						}
					}
					if (cmdCode < 0) {
						logger.error(Locale.get("configuration.ErrSaveKeyShortcuts")/*Could not associate cmd number. Failed to save key shortcuts*/);
						return;
					}
					dos.writeInt(keyCode);
					dos.writeInt(cmdCode);
				}

				dos.writeUTF("Next key Type");
			}
			dos.flush();
			baos.flush();
			writeBinary(baos.toByteArray(), RECORD_ID_KEY_SHORTCUT);
		
		} catch (IOException ioe) {
			logger.exception(Locale.get("configuration.Err2SaveKeyshortcuts")/*Failed to save keyshortcuts*/, ioe);
		}
	}

	public static String getIconPrefix() {
		// FIXME make this configurable - huge, large, etc.
		//#if polish.android
		return "huge_";
		//return "large_";
		//#else
		return "";
		//#endif
	}

	
	public static void serialise(OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(VERSION);
		dos.writeLong(cfgBitsDefault_0_to_63);
		dos.writeLong(cfgBitsDefault_64_to_127);
		dos.writeLong(cfgBitsDefault_128_to_191);
		dos.writeUTF(sanitizeString(btUrl));
		dos.writeInt(locationProvider);
		dos.writeUTF(sanitizeString(gpxUrl));
		dos.writeUTF(sanitizeString(photoUrl));
		dos.writeUTF(sanitizeString(photoEncoding));
		dos.writeBoolean(mapFromJar);
		dos.writeUTF(sanitizeString(mapFileUrl));
		dos.writeUTF(sanitizeString(rawGpsLogUrl));
		dos.writeBoolean(rawGpsLogEnable);
		dos.writeInt(detailBoostDefault);
		dos.writeInt(detailBoostDefaultPOI);
		dos.writeInt(gpxRecordRuleMode);
		dos.writeInt(gpxRecordMinMilliseconds);
		dos.writeInt(gpxRecordMinDistanceCentimeters);
		dos.writeInt(gpxRecordAlwaysDistanceCentimeters);
		dos.writeUTF(sanitizeString(rawDebugLogUrl));
		dos.writeBoolean(rawDebugLogEnable);
		dos.writeInt(debugSeverity);
		dos.writeInt(routeEstimationFac);
		dos.writeInt(continueMapWhileRouteing);
		dos.writeBoolean(btKeepAlive);
		dos.writeBoolean(btAutoRecon);
		dos.writeUTF(sanitizeString(smsRecipient));
		dos.writeInt(speedTolerance);
		dos.writeUTF(sanitizeString(osm_username));
		dos.writeUTF(sanitizeString(osm_pwd));
		dos.writeUTF(sanitizeString(osm_url));
		dos.writeUTF(sanitizeString(opencellid_apikey));
		dos.writeInt(getMinRouteLineWidth());
		dos.writeInt(getAutoRecenterToGpsMilliSecs());
		dos.writeInt(getTravelModeNr());
		dos.writeLong(getPhoneAllTimeMaxMemory());
		dos.writeInt(getMainStreetDistanceKm());
		dos.writeInt(getDetailBoostPOI());
		dos.writeInt(getTrafficSignalCalcDelay());
		dos.writeInt(getWaypointSortMode());
		dos.writeInt(getBackLightLevel());
		dos.writeInt(getBaseScale());
		dos.writeUTF(sanitizeString(getUiLang()));
		dos.writeUTF(sanitizeString(getNaviLang()));
		dos.writeUTF(sanitizeString(getOnlineLang()));
		dos.writeUTF(sanitizeString(getWikipediaLang()));
		dos.writeUTF(sanitizeString(getNamesOnMapLang()));
		dos.writeUTF(sanitizeString(getSoundDirectory()));
		dos.writeInt(getProjDefault());
		dos.writeInt(getSearchMax());
		dos.writeInt(getDestLineWidth());
		dos.writeInt(getTimeDiff());
		dos.writeInt(getAltitudeCorrection());
		dos.writeUTF(sanitizeString(tms_url));
		/*
		 * Don't store destpos in export - perhaps later add a function for "move the app" which would store also destpos
		dos.writeUTF(Float.toString(destPos.radlat));
		dos.writeUTF(Float.toString(destPos.radlon));
		*/
		dos.flush();
	}
	
	public static void deserialise(InputStream is) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		int version = dis.readInt();
		// below version 2 older GpsMid.cfg files could not be read
		if (version != VERSION && version < 22) {
			throw new IOException(Locale.get("configuration.ConfigVersionMismatch")/*Version of the stored config does not match with current GpsMid*/);
		}
		boolean destPosValid = getCfgBitSavedState(CFGBIT_SAVED_DESTPOS_VALID);
		setCfgBits(dis.readLong(), dis.readLong(), version >= 29 ? dis.readLong() : 0L);
		setCfgBitSavedState(CFGBIT_SAVED_DESTPOS_VALID, destPosValid);
		setBtUrl(desanitizeString(dis.readUTF()));
		setLocationProvider(dis.readInt());
		setGpxUrl(desanitizeString(dis.readUTF()));
		setPhotoUrl(desanitizeString(dis.readUTF()));
		setPhotoEncoding(desanitizeString(dis.readUTF()));
		setBuiltinMap(dis.readBoolean());
		setMapUrl(desanitizeString(dis.readUTF()));
		setGpsRawLoggerUrl(desanitizeString(dis.readUTF()));
		setGpsRawLoggerEnable(dis.readBoolean());
		setDetailBoost(dis.readInt(), true);
		setDetailBoostPOI(dis.readInt(), true);
		setGpxRecordRuleMode(dis.readInt());
		setGpxRecordMinMilliseconds(dis.readInt());
		setGpxRecordMinDistanceCentimeters(dis.readInt());
		setGpxRecordAlwaysDistanceCentimeters(dis.readInt());
		setDebugRawLoggerUrl(desanitizeString(dis.readUTF()));
		setDebugRawLoggerEnable(dis.readBoolean());
		debugSeverity = dis.readInt();
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
		setRouteEstimationFac(dis.readInt());
		setContinueMapWhileRouteing(dis.readInt());
		setBtKeepAlive(dis.readBoolean());
		setBtAutoRecon(dis.readBoolean());
		setSmsRecipient(desanitizeString(dis.readUTF()));
		setSpeedTolerance(dis.readInt());
		setOsmUsername(desanitizeString(dis.readUTF()));
		setOsmPwd(desanitizeString(dis.readUTF()));
		setOsmUrl(desanitizeString(dis.readUTF()));
		setOpencellidApikey(desanitizeString(dis.readUTF()));
		// compatibility with format 21
		if (version >= 22) {
			setMinRouteLineWidth(dis.readInt());
			setAutoRecenterToGpsMilliSecs(dis.readInt());			
			setTravelMode(dis.readInt());
			setPhoneAllTimeMaxMemory(dis.readLong());
			setMainStreetDistanceKm(dis.readInt());
			setDetailBoostPOI(dis.readInt(), true);
			setTrafficSignalCalcDelay(dis.readInt());
			setWaypointSortMode(dis.readInt());
			setBackLightLevel(dis.readInt());
			setBaseScale(dis.readInt());
			setUiLang(desanitizeString(dis.readUTF()));
			setNaviLang(desanitizeString(dis.readUTF()));
			setOnlineLang(desanitizeString(dis.readUTF()));
			setWikipediaLang(desanitizeString(dis.readUTF()));
			setNamesOnMapLang(desanitizeString(dis.readUTF()));
			setSoundDirectory(desanitizeString(dis.readUTF()));
			projTypeDefault = (byte) dis.readInt();
			ProjFactory.setProj(projTypeDefault);
			calculateRealBaseScale();
			/*
			Node pos = new Node(0.0f, 0.0f);
			try {
				pos.radlat = Float.parseFloat(desanitizeString(dis.readUTF()));
				pos.radlon = Float.parseFloat(desanitizeString(dis.readUTF()));
			} catch (NumberFormatException nfe) {
				logger.exception(Locale.get("configuration.ErrorParsingPos")Error parsing pos: , nfe);
			}
			setDestPos(pos);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_SAVED_DESTPOS_VALID)) {
				Node destNode = new Node();
				Configuration.getDestPos(destNode);
				Trace.getInstance().setDestination(new RoutePositionMark(destNode.radlat, destNode.radlon));
			}
			*/
		}
		if (version >= 23) {
			searchMax = dis.readInt();
		}
		if (version >= 27) {
			destLineWidth = dis.readInt();
		}
		if (version >= 28) {
			timeDiff = dis.readInt();
		}
		if (version >= 29) {
			altitudeCorrection = dis.readInt();
		}
		if (version >= 33) {
			setTMSUrl(desanitizeString(dis.readUTF()));
		}
		applyDefaultValues(version);
	}
	
	public static int getTouchMarkerDiameter() {
		// FIXME switch this based on pixels-per-inch value and/or make user-configurable
		return 80;
	}
}
