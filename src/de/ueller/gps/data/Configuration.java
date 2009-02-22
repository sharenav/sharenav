package de.ueller.gps.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.lcdui.Command;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;

import de.ueller.gps.tools.BufferedReader;
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.gps.tools.intTree;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.GuiCamera;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.ProjFactory;

public class Configuration {
	
	private static Logger logger;
	
	public final static int VERSION=4;

	public final static int LOCATIONPROVIDER_NONE=0;
	public final static int LOCATIONPROVIDER_SIRF=1; 
	public final static int LOCATIONPROVIDER_NMEA=2; 
	public final static int LOCATIONPROVIDER_JSR179=3;
	
	// bit 0: render as street
	public final static byte CFGBIT_STREETRENDERMODE=0;
	// bit 1 have default values been once applied?
	public final static byte CFGBIT_DEFAULTVALUESAPPLIED=1;
	// bit 2: show POITEXT
	public final static byte CFGBIT_POITEXTS=2;
	// bit 3: show WAYTEXT
	public final static byte CFGBIT_WAYTEXTS=3;
	// bit 4: show AREATEXT
	public final static byte CFGBIT_AREATEXTS=4;
	// bit 5: show POIS
	public final static byte CFGBIT_POIS=5;
	// bit 6: show WPTTTEXT
	public final static byte CFGBIT_WPTTEXTS=6;
	// bit 7: show descriptions
	public final static byte CFGBIT_SHOWWAYPOITYPE=7;
	// bit 8: show latlon
	public final static byte CFGBIT_SHOWLATLON=8;
	// bit 9: full screen
	public final static byte CFGBIT_FULLSCREEN=9;
	// bit 10: keep backlight on
	public final static byte CFGBIT_BACKLIGHT_ON=10;	
	// bit 11: backlight on map screen only
	public final static byte CFGBIT_BACKLIGHT_MAPONLY=11;	
	// bit 12: backlight method MIDP2
	public final static byte CFGBIT_BACKLIGHT_MIDP2=12;
	// bit 13: backlight method NOKIA
	public final static byte CFGBIT_BACKLIGHT_NOKIA=13;	
	// bit 14: backlight method NOKIA/FLASH
	public final static byte CFGBIT_BACKLIGHT_NOKIAFLASH=14;	
	// bit 15: large nearest routing arrow
	public final static byte CFGBIT_ROUTING_HELP=15;	
	// bit 16: save map position on exit
	public final static byte CFGBIT_AUTOSAVE_MAPPOS=16;	
	// bit 17: Sound on Connect
	public final static byte CFGBIT_SND_CONNECT=17;	
	// bit 18: Sound on Disconnect
	public final static byte CFGBIT_SND_DISCONNECT=18;	
	// bit 19: Routing Instructions
	public final static byte CFGBIT_SND_ROUTINGINSTRUCTIONS=19;
	// bit 20: Gps Auto Reconnect
	public final static byte CFGBIT_GPS_AUTORECONNECT=20;
	// bit 21: Sound on target reached
	public final static byte CFGBIT_SND_TARGETREACHED=21;
	// bit 22: auto recalculate route
	public final static byte CFGBIT_ROUTE_AUTO_RECALC=22;
	// bit 23: use JSR135 or JSR 234 for taking pictures;
	public final static byte CFGBIT_USE_JSR_234=23;
	// bit 25: show point of compass in rotated map
	public final static byte CFGBIT_SHOW_POINT_OF_COMPASS=25;
	// bit 26: add geo reference into the exif of a photo;
	public final static byte CFGBIT_ADD_EXIF=26;
	// bit 27: show AREAS
	public final static byte CFGBIT_AREAS=27;
	// bit 28: big poi labels
	public final static byte CFGBIT_POI_LABELS_LARGER=28;
	// bit 29: big wpt labels
	public final static byte CFGBIT_WPT_LABELS_LARGER=29;
	// bit 30: show oneway arrows
	public final static byte CFGBIT_ONEWAY_ARROWS=30;
	// bit 31: Debug Option: show route connections
	public final static byte CFGBIT_ROUTE_CONNECTIONS=31;
	// bit 32: backlight method SIEMENS
	public final static byte CFGBIT_BACKLIGHT_SIEMENS=32;
	// bit 33: Skip initial splash screen
	public final static byte CFGBIT_SKIPP_SPLASHSCREEN=33;
	// bit 34: show place labels
	public final static byte CFGBIT_PLACETEXTS=34;
	// bit 35: Sound alert for speeding
	public final static byte CFGBIT_SPEEDALERT_SND=35;
	// bit 36: Visual alert for speeding
	public final static byte CFGBIT_SPEEDALERT_VISUAL=36;
	// bit 37: Debug Option: show route bearings
	public final static byte CFGBIT_ROUTE_BEARINGS=37;
	// bit 38: Debug Option: hide quiet arrows
	public final static byte CFGBIT_ROUTE_HIDE_QUIET_ARROWS=38;
	// bit 39: in route mode up/down keys are for route browsing
	public final static byte CFGBIT_ROUTE_BROWSING=39;
	
	/**
	 * These are the database record ids for each configuration option	 * 
	 */
	private static final int  RECORD_ID_BT_URL = 1;
	private static final int RECORD_ID_LOCATION_PROVIDER  = 2;
	private static final int RECORD_ID_CFGBITS  = 3;
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
	private static final int RECORD_ID_ROUTE_ESTIMATION_FAC=18;
	private static final int RECORD_ID_STOP_ALL_WHILE_ROUTING=19;
	private static final int RECORD_ID_BT_KEEPALIVE=20;
	private static final int RECORD_ID_STARTUP_RADLAT=21;
	private static final int RECORD_ID_STARTUP_RADLON=22;
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
	

	// Gpx Recording modes
	// GpsMid determines adaptive if a trackpoint is written
	public final static int GPX_RECORD_ADAPTIVE=0;	
	// User specified options define if a trackpoint is written
	public final static int GPX_RECORD_MINIMUM_SECS_DIST=1;	
	
	public static int KEYCODE_CAMERA_COVER_OPEN = -34;
	public static int KEYCODE_CAMERA_COVER_CLOSE = -35;
	public static int KEYCODE_CAMERA_CAPTURE = -26;

	public static final int MAX_WAYPOINTNAME_LENGTH = 255;
	public static final int MAX_WAYPOINTNAME_DRAWLENGTH = 25;
	public static final int MAX_TRACKNAME_LENGTH = 50;
	public static final int MAX_WAYPOINTS_NAME_LENGTH = 50;
	
	public final static String[] LOCATIONPROVIDER={"None","Bluetooth (Sirf)","Bluetooth (NMEA)","Internal (JSR179)"};
	
	private static final String[] compassDirections  =
	{ "N", "NNE", "NE", "NEE", "E", "SEE", "SE", "SSE",
	  "S", "SSW", "SW", "SWW", "W", "NWW", "NW", "NNW",
	  "N"};
	
	private final static byte[] empty="".getBytes();

	private static String btUrl;
	/** This URL is used to store logs of raw data received from the GPS receiver*/
	private static String rawGpsLogUrl; 
	private static boolean rawGpsLogEnable;
	private static String rawDebugLogUrl; 
	private static boolean rawDebugLogEnable;
	private static int locationProvider=0;
	private static int gpxRecordRuleMode;
	private static int gpxRecordMinMilliseconds;
	private static int gpxRecordMinDistanceCentimeters;
	private static int gpxRecordAlwaysDistanceCentimeters;
	private static long cfgBits=0;
	private static long cfgBitsDefault=0;
	private static int detailBoost=0;
	private static int detailBoostDefault=0;
	private static float detailBoostMultiplier;
	private static String gpxUrl;
	private static String photoUrl;
	private static String photoEncoding;
	private static int debugSeverity;
	private static int routeEstimationFac=6;
	private static boolean stopAllWhileRouteing=false;
	private static boolean btKeepAlive = false;
	private static boolean btAutoRecon = false;
	private static Node startupPos = new Node(0.0f, 0.0f);
	private static byte projTypeDefault = ProjFactory.NORTH_UP;
	
	private static boolean mapFromJar;
	private static String mapFileUrl;

	private static String smsRecipient; 
	private static int speedTolerance = 0; 
	
	private static String utf8encodingstring = null;
	
	private static String osm_username;
	private static String osm_pwd;
	private static String osm_url;

	private static int minRouteLineWidth=0;

	public static void read(){
	logger = Logger.getInstance(Configuration.class, Logger.DEBUG);
	RecordStore	database;
		try {			
			database = RecordStore.openRecordStore("Receiver", true);
			if (database == null) {
				//#debug info
				logger.info("No database loaded at the moment");
				return;
			}	
			cfgBits=readLong(database, RECORD_ID_CFGBITS);
			// set initial values if record store does not exist yet
			if( ! getCfgBitState(CFGBIT_DEFAULTVALUESAPPLIED) ) {
				cfgBits=1L<<CFGBIT_DEFAULTVALUESAPPLIED | 
				   		1L<<CFGBIT_STREETRENDERMODE |
				   		1L<<CFGBIT_POITEXTS |
				   		1L<<CFGBIT_AREATEXTS |
				   		1L<<CFGBIT_WPTTEXTS |
				   		// 1L<<CFGBIT_WAYTEXTS | // way texts are still experimental
				   		1L<<CFGBIT_ONEWAY_ARROWS |
				   		1L<<CFGBIT_POIS |
				   		1L<<CFGBIT_ROUTING_HELP |
				   		1L<<CFGBIT_AUTOSAVE_MAPPOS |
				   		1L<<CFGBIT_BACKLIGHT_MAPONLY |
				   		getDefaultDeviceBacklightMethodMask();
				// Record Rule Default
				setGpxRecordRuleMode(GPX_RECORD_MINIMUM_SECS_DIST);
				setGpxRecordMinMilliseconds(1000);				
				setGpxRecordMinDistanceCentimeters(300);
				setGpxRecordAlwaysDistanceCentimeters(500);
				// Routing defaults
				setStopAllWhileRouteing(true);
				setRouteEstimationFac(7);
				// set default location provider to JSR-179 if available
				//#if polish.api.locationapi
				if (getDeviceSupportsJSR179()) {
					setLocationProvider(LOCATIONPROVIDER_JSR179);
				}
				//#endif				
				//#debug info
				logger.info("Default config for version 0.4.0+ set.");
			}
			int configVersionStored = readInt(database, RECORD_ID_CONFIG_VERSION);
			// default values for config version 3
			if(configVersionStored < 3) {				
				cfgBits |=	1L<<CFGBIT_SND_CONNECT |
					   		1L<<CFGBIT_SND_DISCONNECT |
					   		1L<<CFGBIT_SND_ROUTINGINSTRUCTIONS |
					   		1L<<CFGBIT_SND_TARGETREACHED |
					   		1L<<CFGBIT_SHOW_POINT_OF_COMPASS |
					   		1L<<CFGBIT_AREAS |
					   		1L<<CFGBIT_ROUTE_AUTO_RECALC;

				// Auto-reconnect GPS
				setBtAutoRecon(true);
				// make MOVE_UP map the default
				setProjTypeDefault(ProjFactory.MOVE_UP);
				//#debug info
				logger.info("Default config for version 3+ set.");
			}			
			if(configVersionStored < 4) {				
				cfgBits |=	1L<<CFGBIT_PLACETEXTS |
							1L<<CFGBIT_SPEEDALERT_SND |
							1L<<CFGBIT_ROUTE_HIDE_QUIET_ARROWS |
							1L<<CFGBIT_ROUTE_BROWSING |
							1L<<CFGBIT_SPEEDALERT_VISUAL;
							setMinRouteLineWidth(3);
							// Speed alert tolerance
							setSpeedTolerance(5);
				//#debug info
				logger.info("Default config for version 4+ set.");
			}			
			setCfgBits(cfgBits, true);
			// remember for which version the default values were stored
			write(VERSION, RECORD_ID_CONFIG_VERSION);
			
			btUrl=readString(database, RECORD_ID_BT_URL);
			locationProvider=readInt(database, RECORD_ID_LOCATION_PROVIDER);
			gpxUrl=readString(database, RECORD_ID_GPX_URL);
			photoUrl=readString(database, RECORD_ID_PHOTO_URL);
			photoEncoding=readString(database, RECORD_ID_PHOTO_ENCODING);
			mapFromJar=readInt(database, RECORD_ID_MAP_FROM_JAR) == 0;
			mapFileUrl=readString(database, RECORD_ID_MAP_FILE_URL);
			rawGpsLogUrl=readString(database, RECORD_ID_LOG_RAW_GPS_URL);
			rawGpsLogEnable = readInt(database, RECORD_ID_LOG_RAW_GPS_ENABLE) !=0;
			detailBoost=readInt(database,RECORD_ID_DETAIL_BOOST);
			detailBoostDefault=detailBoost;
			calculateDetailBoostMultiplier();
			gpxRecordRuleMode=readInt(database, RECORD_ID_GPX_FILTER_MODE); 
			gpxRecordMinMilliseconds=readInt(database, RECORD_ID_GPX_FILTER_TIME); 
			gpxRecordMinDistanceCentimeters=readInt(database, RECORD_ID_GPX_FILTER_DIST); 
			gpxRecordAlwaysDistanceCentimeters=readInt(database, RECORD_ID_GPX_FILTER_ALWAYS_DIST); 
			rawDebugLogUrl=readString(database, RECORD_ID_LOG_DEBUG_URL);
			rawDebugLogEnable = readInt(database,  RECORD_ID_LOG_DEBUG_ENABLE) !=0;
			debugSeverity=readInt(database, RECORD_ID_LOG_DEBUG_SEVERITY);
			routeEstimationFac=readInt(database,RECORD_ID_ROUTE_ESTIMATION_FAC);
			stopAllWhileRouteing=readInt(database,  RECORD_ID_STOP_ALL_WHILE_ROUTING) !=0;
			btKeepAlive = readInt(database,  RECORD_ID_BT_KEEPALIVE) !=0;
			btAutoRecon = readInt(database,  RECORD_ID_GPS_RECONNECT) !=0;
			String s = readString(database, RECORD_ID_STARTUP_RADLAT);
			String s2 = readString(database, RECORD_ID_STARTUP_RADLON);
			if(s!=null && s2!=null) {
				startupPos.radlat=Float.parseFloat(s);
				startupPos.radlon=Float.parseFloat(s2);
			}
			//System.out.println("Map startup lat/lon: " + startupPos.radlat*MoreMath.FAC_RADTODEC + "/" + startupPos.radlon*MoreMath.FAC_RADTODEC);
			setProjTypeDefault((byte) readInt(database,  RECORD_ID_MAP_PROJECTION));
			smsRecipient = readString(database, RECORD_ID_SMS_RECIPIENT);
			speedTolerance = readInt(database, RECORD_ID_SPEED_TOLERANCE);
			osm_username = readString(database, RECORD_ID_OSM_USERNAME);
			osm_pwd = readString(database, RECORD_ID_OSM_PWD);
			osm_url = readString(database, RECORD_ID_OSM_URL);
			if (osm_url == null) {
				osm_url = "http://api.openstreetmap.org/api/0.5/";
			}
			minRouteLineWidth=readInt(database, RECORD_ID_MIN_ROUTELINE_WIDTH); 
			
			database.closeRecordStore();
		} catch (Exception e) {
			logger.exception("Problems with reading our configuration: ", e);
		}
	}
	
	private final static String sanitizeString(String s) {
		if (s == null) {
			return "!null!";
		}
		return s;
	}
	
	private final static String desanitizeString(String s) {
		if (s.equalsIgnoreCase("!null!"))
			return null;
		return s;
	}
	
	private static void write(String s, int idx) {
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			byte[] data;
			data = sanitizeString(s).getBytes();
			while (database.getNumRecords() < idx){
				database.addRecord(empty, 0, empty.length);
			}
			database.setRecord(idx, data,0,data.length);
			database.closeRecordStore();
			//#debug info
			logger.info("wrote " + s + " to " + idx);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private static void write(int i,int idx){
		write(""+i,idx);
	}
	private static void write(long i,int idx){
		write(""+i,idx);
	}


	private static String readString(RecordStore database,int idx){
		try {
			String ret;
			byte[] data;
			try {
				data = database.getRecord(idx);
			}
			catch (InvalidRecordIDException irie) {
				//Use defaults
				return null;
			}
			if (data == null) {
				ret = null;
			} else {
				ret = desanitizeString(new String(data));
			}
			//#debug info
			logger.info("Read from config database " + idx + ": " + ret);
			return ret;
		} catch (Exception e) {
			logger.exception("Failed to read string from config database", e);
			return null;
		} 
	}
	private static int readInt(RecordStore database,int idx){
		try {
			String tmp = readString(database, idx);
			//#debug info
			logger.info("Read from config database " + idx + ": " + tmp);
			if (tmp == null) {
				return 0;
			} else {
				return Integer.parseInt(tmp);
			}			
		} catch (Exception e){
			logger.exception("Failed to read int from config database", e);
			return 0;
		}
	}
	
	private static long readLong(RecordStore database,int idx){
		try {
			String tmp = readString(database, idx);
			//#debug info
			logger.info("Read from config database " + idx + ": " + tmp);
			if (tmp == null) {
				return 0;
			} else {
				return Long.parseLong(tmp);
			}			
		} catch (Exception e){
			logger.exception("Failed to read Long from config database", e);
			return 0;
		}
	}
	
	public static void serialise(OutputStream os) throws IOException{
		DataOutputStream dos = new DataOutputStream(os);
		dos.writeInt(VERSION);
		dos.writeLong(cfgBits);
		dos.writeUTF(sanitizeString(btUrl));
		dos.writeInt(locationProvider);
		dos.writeUTF(sanitizeString(gpxUrl));
		dos.writeUTF(sanitizeString(photoUrl));
		dos.writeUTF(sanitizeString(photoEncoding));
		dos.writeBoolean(mapFromJar);
		dos.writeUTF(sanitizeString(mapFileUrl));
		dos.writeUTF(sanitizeString(rawGpsLogUrl));
		dos.writeBoolean(rawGpsLogEnable);
		dos.writeInt(detailBoost);
		dos.writeInt(gpxRecordRuleMode);
		dos.writeInt(gpxRecordMinMilliseconds); 
		dos.writeInt(gpxRecordMinDistanceCentimeters); 
		dos.writeInt(gpxRecordAlwaysDistanceCentimeters);
		dos.writeUTF(sanitizeString(rawDebugLogUrl));
		dos.writeBoolean(rawDebugLogEnable);
		dos.writeInt(debugSeverity);
		dos.writeInt(routeEstimationFac);
		dos.writeBoolean(stopAllWhileRouteing);
		dos.writeBoolean(btKeepAlive);
		dos.writeBoolean(btAutoRecon);
		dos.writeUTF(sanitizeString(smsRecipient));
		dos.writeInt(speedTolerance);
		dos.writeUTF(sanitizeString(osm_username));
		dos.writeUTF(sanitizeString(osm_pwd));
		dos.writeUTF(sanitizeString(osm_url));
		dos.flush();
	}
	
	public static void deserialise(InputStream is) throws IOException{
		DataInputStream dis = new DataInputStream(is);
		int version = dis.readInt();
		if (version != VERSION) {
			throw new IOException("Version of the stored config does not match with current GpsMid");
		}
		setCfgBits(dis.readLong(), true);
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
		setGpxRecordRuleMode(dis.readInt());
		setGpxRecordMinMilliseconds(dis.readInt());
		setGpxRecordMinDistanceCentimeters(dis.readInt());
		setGpxRecordAlwaysDistanceCentimeters(dis.readInt());
		setDebugRawLoggerUrl(desanitizeString(dis.readUTF()));
		setDebugRawLoggerEnable(dis.readBoolean());
		debugSeverity = dis.readInt();
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
		setRouteEstimationFac(dis.readInt());
		setStopAllWhileRouteing(dis.readBoolean());
		setBtKeepAlive(dis.readBoolean());
		setBtAutoRecon(dis.readBoolean());
		setSmsRecipient(desanitizeString(dis.readUTF()));
		setSpeedTolerance(dis.readInt());
		setOsmUsername(desanitizeString(dis.readUTF()));
		setOsmPwd(desanitizeString(dis.readUTF()));
		setOsmUrl(desanitizeString(dis.readUTF()));
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
		if (rawGpsLogEnable) 
			write(1, RECORD_ID_LOG_RAW_GPS_ENABLE);
		else
			write(0, RECORD_ID_LOG_RAW_GPS_ENABLE);
	}
	
	public static boolean getDebugRawLoggerEnable() {		
		return rawDebugLogEnable;		
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
		if (rawDebugLogEnable) 
			write(1, RECORD_ID_LOG_DEBUG_ENABLE);
		else
			write(0, RECORD_ID_LOG_DEBUG_ENABLE);
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

	public static boolean getCfgBitState(byte bit,boolean getDefault) {
		if (getDefault) {
			return ((cfgBitsDefault & (1L<<bit)) !=0);			
		} else {
			return ((cfgBits & (1L<<bit)) !=0);
		}
	}

	public static boolean getCfgBitState(byte bit) {
		return getCfgBitState(bit, false);			
	}
	
	
	public static void setCfgBitState(byte bit, boolean state, boolean setAsDefault) {
		// set bit
		Configuration.cfgBits|= (1L<<bit);
		if (!state) {
			// clear bit
			Configuration.cfgBits^= (1L<<bit);
		}
		if (setAsDefault) {
			Configuration.cfgBitsDefault|= (1L<<bit);
			if (!state) {
				// clear bit
				Configuration.cfgBitsDefault^= (1L<<bit);
			}			
			write(cfgBitsDefault, RECORD_ID_CFGBITS);
		}	
	}	
	
	private static void setCfgBits(long cfgBits, boolean setAsDefault) {
		Configuration.cfgBits = cfgBits;
		if (setAsDefault) {
			Configuration.cfgBitsDefault = cfgBits;
			write(cfgBitsDefault, RECORD_ID_CFGBITS);
		}
	}
	
	public static int getDetailBoost() {
		return detailBoost;
	}

	public static void setDetailBoost(int detailBoost, boolean setAsDefault) {
		Configuration.detailBoost = detailBoost;
		calculateDetailBoostMultiplier();
		if (setAsDefault) {
			Configuration.detailBoostDefault = detailBoost;
			write(detailBoost, RECORD_ID_DETAIL_BOOST);		
		}
	}
	
	public static float getDetailBoostMultiplier() {
		return detailBoostMultiplier;
	}

	public static int getDetailBoostDefault() {
		return detailBoostDefault;
	}

/**
	There's no pow()-function in J2ME so manually
	calculate 1.5^detailBoost to get factor
	to multiply with Zoom Level limits
**/
	private static void calculateDetailBoostMultiplier() {
		detailBoostMultiplier=1;
		for(int i=1;i<=detailBoost;i++) {
			detailBoostMultiplier*=1.5;
		}
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
		write(mapFromJar?0:1, RECORD_ID_MAP_FROM_JAR);
		Configuration.mapFromJar = mapFromJar;
	}
	
	public static String getMapUrl() {
		return mapFileUrl;
	}
	
	public static void setMapUrl(String url) {
		write(url, RECORD_ID_MAP_FILE_URL);
		mapFileUrl = url;		
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

	public static void setMinRouteLineWidth(int w) {
		minRouteLineWidth = Math.max(w, 1);
		write(minRouteLineWidth, RECORD_ID_MIN_ROUTELINE_WIDTH);
	}
	
	
	
	public static InputStream getMapResource(String name) throws IOException{
		InputStream is;
		if (mapFromJar) {
			is = QueueReader.class.getResourceAsStream(name);			
		} else {			
			//#if polish.api.fileconnection
			if (mapFileUrl.endsWith("/"))
				mapFileUrl = mapFileUrl.substring(0, mapFileUrl.length() - 1);
			String url = mapFileUrl + name;
			//#debug info
			logger.info("Opening file: " + url);
			Connection session = Connector.open(url,Connector.READ);
			FileConnection fileCon = (FileConnection) session;			
			if (fileCon == null) {
				//#debug info
				logger.info("Couldn't open url: " + url);
				throw new IOException("Couldn't open url " + url);				
			}
				
						
			is = fileCon.openInputStream();				
			//#else
			//This should never happen.
			is = null;
			logger.fatal("Error, we don't have access to the filesystem, but our map data is supposed to be there!");
			//#endif
			
		}
		return is;
	}

	public static int getRouteEstimationFac() {
		return routeEstimationFac;
	}

	public static void setRouteEstimationFac(int routeEstimationFac) {
		write(routeEstimationFac,RECORD_ID_ROUTE_ESTIMATION_FAC);
		Configuration.routeEstimationFac = routeEstimationFac;
	}

	public static boolean isStopAllWhileRouteing() {
		return stopAllWhileRouteing;
	}

	public static void setStopAllWhileRouteing(boolean stopAllWhileRouteing) {
		write(stopAllWhileRouteing?1:0, RECORD_ID_STOP_ALL_WHILE_ROUTING);
		Configuration.stopAllWhileRouteing = stopAllWhileRouteing;
	}
	
	public static boolean getBtKeepAlive() {
		return btKeepAlive;
	}
	
	public static void setBtKeepAlive(boolean keepAlive) {
		write(keepAlive?1:0, RECORD_ID_BT_KEEPALIVE);
		Configuration.btKeepAlive = keepAlive;
	}
	
	public static boolean getBtAutoRecon() {
		return btAutoRecon;
	}
	
	public static void setBtAutoRecon(boolean autoRecon) {
		write(autoRecon?1:0, RECORD_ID_GPS_RECONNECT);
		Configuration.btAutoRecon = autoRecon;
	}

	public static void getStartupPos(Node pos) {
		pos.setLatLon(startupPos.radlat, startupPos.radlon, true);
	}

	public static void setStartupPos(Node pos) {
		//System.out.println("Save Map startup lat/lon: " + startupPos.radlat*MoreMath.FAC_RADTODEC + "/" + startupPos.radlon*MoreMath.FAC_RADTODEC);
		write(Double.toString(pos.radlat),RECORD_ID_STARTUP_RADLAT);
		write(Double.toString(pos.radlon),RECORD_ID_STARTUP_RADLON);
	}
	
	public static String getOsmUsername() {
		return osm_username;
	}

	public static void setOsmUsername(String name) {
		osm_username = name;
		write(name,RECORD_ID_OSM_USERNAME);
	}
	
	public static String getOsmPwd() {
		return osm_pwd;
	}

	public static void setOsmPwd(String pwd) {
		osm_pwd = pwd;
		write(pwd,RECORD_ID_OSM_PWD);
	}
	
	public static String getOsmUrl() {
		return osm_url;
	}
	
	public static void setOsmUrl(String url) {
		osm_url = url;
		write(url,RECORD_ID_OSM_URL);
	}

	public static void setProjTypeDefault(byte t) {
		ProjFactory.setProj(t);
		projTypeDefault = t;
		write((int) t, RECORD_ID_MAP_PROJECTION);
	}

	public static byte getProjDefault() {
		return projTypeDefault;
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
	
	public static boolean hasDeviceJSR120(){
		try {
			Class.forName("javax.wireless.messaging.MessageConnection" );
			return true;
		}
		catch( Exception e ){
			return false;
		}
	}
	
	private static long getDefaultDeviceBacklightMethodMask() {
		// a list of return codes for microedition.platform can be found at:
		// http://www.club-java.com/TastePhone/J2ME/MIDP_Benchmark.jsp

		//#if polish.api.nokia-ui || polish.api.min-siemapi
			String phoneModel = null;
			try {
				phoneModel = System.getProperty("microedition.platform");
			} catch (RuntimeException re) {
				/**
				 * Some phones throw exceptions if trying to access properties that don't
				 * exist, so we have to catch these and just ignore them.
				 */
				return 0;
			} catch (Exception e) {
				/**
				 * See above 
				 */
				return 0;
			}
			if (phoneModel != null) {
				// determine default backlight method for devices from the wiki
				if (phoneModel.startsWith("Nokia") ||
					phoneModel.startsWith("SonyEricssonC") ||
					phoneModel.startsWith("SonyEricssonK550")
				) {
					return 1L<<CFGBIT_BACKLIGHT_NOKIA;			
				} else if (phoneModel.startsWith("SonyEricssonK750") ||
					phoneModel.startsWith("SonyEricssonW800")
				) {
					return 1L<<CFGBIT_BACKLIGHT_NOKIAFLASH;
				} else if (phoneModel.endsWith("(NSG)") || 
				    phoneModel.startsWith("SIE")
				) {
					return 1<<CFGBIT_BACKLIGHT_SIEMENS;
		        } 			
			}
		//#endif
		return 0;
	}
	
	public static String getValidFileName(String fileName){
		return fileName.replace('\\','_').replace('/','_').replace('>','_').replace('<','_').replace(':','_').replace('?','_').replace('*','_');
	}
	
	public static String getCompassDirection(int course) {
		return compassDirections[(int) ((float) ((course%360 + 11.25f) / 22.5f)) ];
	}
	
	public static String getUtf8Encoding() {
		final String[] encodings  = { "UTF-8", "UTF8", "utf-8", "utf8", "" };
		
		if (utf8encodingstring != null)
			return utf8encodingstring;
		
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
	
	public static void loadKeyShortcuts(intTree gameKeys, intTree singleKeys, intTree repeatableKeys, intTree doubleKeys, intTree longKeys, intTree specialKeys, Command [] cmds) {
		int keyType = 0;
		
		logger.info("Initialising KeyShortCuts");
		try {
			InputStream is = getMapResource("/keyMap.txt");
			if (is == null)
				throw new IOException("keyMap.txt not found");
			InputStreamReader isr = new InputStreamReader(is, getUtf8Encoding());
			BufferedReader br = new BufferedReader(isr);
			String line;
			line = br.readLine();
			while (line != null) {
				line.trim();
				if (line.length() == 0) {
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
			logger.exception("Could not load key shortcuts", ioe);
		}
		
	}
}
