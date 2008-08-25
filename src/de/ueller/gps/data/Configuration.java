package de.ueller.gps.data;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;

import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.GuiCamera;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;

public class Configuration {
	
	private static Logger logger;
	
	public final static int VERSION=1;

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
	

	// Gpx Recording modes
	// GpsMid determines adaptive if a trackpoint is written
	public final static int GPX_RECORD_ADAPTIVE=0;	
	// User specified options define if a trackpoint is written
	public final static int GPX_RECORD_MINIMUM_SECS_DIST=1;	
	
	public static int KEYCODE_CAMERA_COVER_OPEN = -34;
	public static int KEYCODE_CAMERA_COVER_CLOSE = -35;
	public static int KEYCODE_CAMERA_CAPTURE = -26;

	public final static String[] LOCATIONPROVIDER={"None","Bluetooth (Sirf)","Bluetooth (NMEA)","Internal (JSR179)"};
	private final static byte[] empty="".getBytes();

	private String btUrl;
	/** This URL is used to store logs of raw data received from the GPS receiver*/
	private String rawGpsLogUrl; 
	private boolean rawGpsLogEnable;
	private String rawDebugLogUrl; 
	private boolean rawDebugLogEnable;
	private int locationProvider=0;
	private int gpxRecordRuleMode;
	private int gpxRecordMinMilliseconds;
	private int gpxRecordMinDistanceCentimeters;
	private int gpxRecordAlwaysDistanceCentimeters;
	private int cfgBits=0;
	private int cfgBitsDefault=0;
	private int detailBoost=0;
	private int detailBoostDefault=0;
	private float detailBoostMultiplier;
	private String gpxUrl;
	private String photoUrl;
	private String photoEncoding;
	private int debugSeverity;
	private int routeEstimationFac=6;
	private boolean stopAllWhileRouteing=false;
	private boolean btKeepAlive = false;
	private boolean btAutoRecon = false;
	private Node startupPos = new Node(0.0f, 0.0f);
	
	private boolean mapFromJar;
	private String mapFileUrl;
	
	
	public Configuration() {		
		logger = Logger.getInstance(Configuration.class, Logger.DEBUG);
		read();
	}

	private void read(){
	RecordStore	database;
		try {			
			database = RecordStore.openRecordStore("Receiver", true);
			if (database == null) {
				//#debug info
				logger.info("No database loaded at the moment");
				return;
			}	
			cfgBits=readInt(database, RECORD_ID_CFGBITS);
			// set initial values if record store does not exist yet
			if( ! getCfgBitState(CFGBIT_DEFAULTVALUESAPPLIED) ) {
				cfgBits=1<<CFGBIT_DEFAULTVALUESAPPLIED | 
				   		1<<CFGBIT_STREETRENDERMODE |
				   		1<<CFGBIT_POITEXTS |
				   		1<<CFGBIT_AREATEXTS |
				   		1<<CFGBIT_WPTTEXTS |
				   		// 1<<CFGBIT_WAYTEXTS | // way texts are still experimental
				   		1<<CFGBIT_POIS |
				   		1<<CFGBIT_ROUTING_HELP |
				   		1<<CFGBIT_AUTOSAVE_MAPPOS |
				   		1<<CFGBIT_SND_CONNECT |
				   		1<<CFGBIT_SND_DISCONNECT |
				   		1<<CFGBIT_SND_ROUTINGINSTRUCTIONS |
				   		1<<CFGBIT_SND_TARGETREACHED |
				   		1<<CFGBIT_ROUTE_AUTO_RECALC |
				   		1<<CFGBIT_BACKLIGHT_MAPONLY |
				   		getDefaultDeviceBacklightMethodMask();
				setCfgBits(cfgBits, true);
				//#debug info
				logger.info("Default cfgBits where set.");
				// Record Rule Default
				setGpxRecordRuleMode(GPX_RECORD_MINIMUM_SECS_DIST);
				setGpxRecordMinMilliseconds(1000);				
				setGpxRecordMinDistanceCentimeters(300);
				setGpxRecordAlwaysDistanceCentimeters(500);
				// Routing defaults
				setStopAllWhileRouteing(true);
				setRouteEstimationFac(7);
				// Auto-reconnect GPS
				setBtAutoRecon(true);
				// set default location provider to JSR-179 if available
				//#if polish.api.locationapi
				if (getDeviceSupportsJSR179()) {
					setLocationProvider(LOCATIONPROVIDER_JSR179);
				}
				//#endif

				//#debug info
				logger.info("More initial default values where set.");
			}
			cfgBitsDefault=cfgBits;
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
			String s=readString(database, RECORD_ID_STARTUP_RADLAT);
			String s2=readString(database, RECORD_ID_STARTUP_RADLON);
			if(s!=null && s2!=null) {
				startupPos.radlat=Float.parseFloat(s);
				startupPos.radlon=Float.parseFloat(s2);
			}
			//System.out.println("Map startup lat/lon: " + startupPos.radlat*MoreMath.FAC_RADTODEC + "/" + startupPos.radlon*MoreMath.FAC_RADTODEC);
			database.closeRecordStore();
		} catch (Exception e) {
			logger.exception("Problems with reading our configuration: ", e);
		}
	}
	
	private void write(String s, int idx) {
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			byte[] data;
			if (s == null) {
				data ="!null!".getBytes();
			} else {
				data=s.getBytes();
			}
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
	private void write(int i,int idx){
		write(""+i,idx);
	}


	public String readString(RecordStore database,int idx){
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
				ret = new String(data);
				if (ret.equalsIgnoreCase("!null!"))
					ret = null;
			}
			//#debug info
			logger.info("Read from config database " + idx + ": " + ret);
			return ret;
		} catch (Exception e) {
			logger.exception("Failed to read string from config database", e);
			return null;
		} 
	}
	public int readInt(RecordStore database,int idx){
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
	
	public String getGpsRawLoggerUrl() {		
		return rawGpsLogUrl;
	}
	
	public void setGpsRawLoggerUrl(String url) {
		rawGpsLogUrl = url;
		write(rawGpsLogUrl, RECORD_ID_LOG_RAW_GPS_URL);
	}
	
	public void setGpsRawLoggerEnable(boolean enabled) {
		rawGpsLogEnable = enabled;
		if (rawGpsLogEnable) 
			write(1, RECORD_ID_LOG_RAW_GPS_ENABLE);
		else
			write(0, RECORD_ID_LOG_RAW_GPS_ENABLE);
	}
	
	public boolean getDebugRawLoggerEnable() {		
		return rawDebugLogEnable;		
	}
	
	public String getDebugRawLoggerUrl() {		
		return rawDebugLogUrl;		
	}
	
	public void setDebugRawLoggerUrl(String url) {
		rawDebugLogUrl = url;
		write(rawDebugLogUrl, RECORD_ID_LOG_DEBUG_URL);
	}
	
	public void setDebugSeverityInfo(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x01;
		} else {
			debugSeverity &= ~0x01;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public boolean getDebugSeverityInfo() {
		return ((debugSeverity & 0x01) > 0);
	}
	
	public void setDebugSeverityDebug(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x02;
		} else {
			debugSeverity &= ~0x02;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public boolean getDebugSeverityDebug() {
		return ((debugSeverity & 0x02) > 0);
	}
	
	public void setDebugSeverityTrace(boolean enabled) {
		if (enabled) {
			debugSeverity |= 0x04;
		} else {
			debugSeverity &= ~0x04;
		}
		write(debugSeverity, RECORD_ID_LOG_DEBUG_SEVERITY);
	}
	
	public boolean getDebugSeverityTrace() {
		return ((debugSeverity & 0x04) > 0);
	}
	
	public void setDebugRawLoggerEnable(boolean enabled) {
		rawDebugLogEnable = enabled;
		if (rawDebugLogEnable) 
			write(1, RECORD_ID_LOG_DEBUG_ENABLE);
		else
			write(0, RECORD_ID_LOG_DEBUG_ENABLE);
	}
	
	public boolean getGpsRawLoggerEnable() {		
		return rawGpsLogEnable;
	}

	public String getBtUrl() {
		return btUrl;
	}

	public void setBtUrl(String btUrl) {
		this.btUrl = btUrl;
		write(btUrl, RECORD_ID_BT_URL);
	}

	public int getLocationProvider() {
		return locationProvider;
	}

	public void setLocationProvider(int locationProvider) {
		this.locationProvider = locationProvider;
		write(locationProvider, RECORD_ID_LOCATION_PROVIDER);
	}

	public int getGpxRecordRuleMode() {
		return gpxRecordRuleMode;
	}
	public void setGpxRecordRuleMode(int gpxRecordRuleMode) {
		this.gpxRecordRuleMode = gpxRecordRuleMode;
			write(gpxRecordRuleMode, RECORD_ID_GPX_FILTER_MODE);
	}

	public int getGpxRecordMinMilliseconds() {
		return gpxRecordMinMilliseconds;
	}
	public void setGpxRecordMinMilliseconds(int gpxRecordMinMilliseconds) {
		this.gpxRecordMinMilliseconds = gpxRecordMinMilliseconds;
			write(gpxRecordMinMilliseconds, RECORD_ID_GPX_FILTER_TIME);
	}	

	public int getGpxRecordMinDistanceCentimeters() {
		return gpxRecordMinDistanceCentimeters;
	}
	public void setGpxRecordMinDistanceCentimeters(int gpxRecordMinDistanceCentimeters) {
		this.gpxRecordMinDistanceCentimeters = gpxRecordMinDistanceCentimeters;
			write(gpxRecordMinDistanceCentimeters, RECORD_ID_GPX_FILTER_DIST);
	}	

	public int getGpxRecordAlwaysDistanceCentimeters() {
		return gpxRecordAlwaysDistanceCentimeters;
	}
	public void setGpxRecordAlwaysDistanceCentimeters(int gpxRecordAlwaysDistanceCentimeters) {
		this.gpxRecordAlwaysDistanceCentimeters = gpxRecordAlwaysDistanceCentimeters;
			write(gpxRecordAlwaysDistanceCentimeters, RECORD_ID_GPX_FILTER_ALWAYS_DIST);
	}	

	public boolean getCfgBitState(byte bit,boolean getDefault) {
		if (getDefault) {
			return ((this.cfgBitsDefault & (1<<bit)) !=0);			
		} else {
			return ((this.cfgBits & (1<<bit)) !=0);
		}
	}

	public boolean getCfgBitState(byte bit) {
		return getCfgBitState(bit, false);			
	}
	
	
	public void setCfgBitState(byte bit, boolean state, boolean setAsDefault) {
		// set bit
		this.cfgBits|=(1<<bit);
		if (!state) {
			// clear bit
			this.cfgBits^=(1<<bit);
		}
		if (setAsDefault) {
			this.cfgBitsDefault|=(1<<bit);
			if (!state) {
				// clear bit
				this.cfgBitsDefault^=(1<<bit);
			}			
			write(cfgBitsDefault, RECORD_ID_CFGBITS);
		}	
	}	
	
	public void setCfgBits(int cfgBits, boolean setAsDefault) {
		this.cfgBits = cfgBits;
		if (setAsDefault) {
			this.cfgBitsDefault = cfgBits;
			write(cfgBitsDefault, RECORD_ID_CFGBITS);
		}
	}
	
	public int getCfgBitsDefault() {
		return cfgBitsDefault;
	}
	
	public int getDetailBoost() {
		return this.detailBoost;
	}

	public void setDetailBoost(int detailBoost, boolean setAsDefault) {
		this.detailBoost = detailBoost;
		calculateDetailBoostMultiplier();
		if (setAsDefault) {
			this.detailBoostDefault = detailBoost;
			write(detailBoost, RECORD_ID_DETAIL_BOOST);		
		}
	}
	
	public float getDetailBoostMultiplier() {
		return this.detailBoostMultiplier;
	}

	public int getDetailBoostDefault() {
		return this.detailBoostDefault;
	}

/**
	There's no pow()-function in J2ME so manually
	calculate 1.5^detailBoost to get factor
	to multiply with Zoom Level limits
**/
	private void calculateDetailBoostMultiplier() {
		this.detailBoostMultiplier=1;
		for(int i=1;i<=this.detailBoost;i++) {
			this.detailBoostMultiplier*=1.5;
		}
	}
	
	public void setGpxUrl(String url) {
		this.gpxUrl = url;
		write(url, RECORD_ID_GPX_URL);
	}

	public String getGpxUrl() {
		return gpxUrl;
	}
	
	public void setPhotoUrl(String url) {
		this.photoUrl = url;
		write(url, RECORD_ID_PHOTO_URL);
	}

	public String getPhotoUrl() {
		return photoUrl;
	}
	
	public void setPhotoEncoding(String encoding) {
		this.photoEncoding = encoding;
		write(encoding, RECORD_ID_PHOTO_ENCODING);
	}

	public String getPhotoEncoding() {
		return photoEncoding;
	}
	
	public boolean usingBuiltinMap() {
		return mapFromJar;
	}
	
	public void setBuiltinMap(boolean mapFromJar) {
		write(mapFromJar?0:1, RECORD_ID_MAP_FROM_JAR);
		this.mapFromJar = mapFromJar;
	}
	
	public String getMapUrl() {
		return mapFileUrl;
	}
	
	public void setMapUrl(String url) {
		write(url, RECORD_ID_MAP_FILE_URL);
		mapFileUrl = url;		
	}
	
	public InputStream getMapResource(String name) throws IOException{
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

	public int getRouteEstimationFac() {
		return routeEstimationFac;
	}

	public void setRouteEstimationFac(int routeEstimationFac) {
		write(routeEstimationFac,RECORD_ID_ROUTE_ESTIMATION_FAC);
		this.routeEstimationFac = routeEstimationFac;
	}

	public boolean isStopAllWhileRouteing() {
		return stopAllWhileRouteing;
	}

	public void setStopAllWhileRouteing(boolean stopAllWhileRouteing) {
		write(stopAllWhileRouteing?1:0, RECORD_ID_STOP_ALL_WHILE_ROUTING);
		this.stopAllWhileRouteing = stopAllWhileRouteing;
	}
	
	public boolean getBtKeepAlive() {
		return btKeepAlive;
	}
	
	public void setBtKeepAlive(boolean keepAlive) {
		write(keepAlive?1:0, RECORD_ID_BT_KEEPALIVE);
		this.btKeepAlive = keepAlive;
	}
	
	public boolean getBtAutoRecon() {
		return btAutoRecon;
	}
	
	public void setBtAutoRecon(boolean autoRecon) {
		write(autoRecon?1:0, RECORD_ID_GPS_RECONNECT);
		this.btAutoRecon = autoRecon;
	}

	public void getStartupPos(Node pos) {
		pos.setLatLon(startupPos.radlat, startupPos.radlon, true);
	}

	public void setStartupPos(Node pos) {
		//System.out.println("Save Map startup lat/lon: " + startupPos.radlat*MoreMath.FAC_RADTODEC + "/" + startupPos.radlon*MoreMath.FAC_RADTODEC);
		write(Double.toString(pos.radlat),RECORD_ID_STARTUP_RADLAT);
		write(Double.toString(pos.radlon),RECORD_ID_STARTUP_RADLON);
	}

	public boolean getDeviceSupportsJSR179() {
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
		return false;
	}
	
	private int getDefaultDeviceBacklightMethodMask() {
		// a list of return codes for microedition.platform can be found at:
		// http://www.club-java.com/TastePhone/J2ME/MIDP_Benchmark.jsp
		String phoneModel=System.getProperty("microedition.platform");
		if (phoneModel != null) {
			// determine default backlight method for devices from the wiki
			if (phoneModel.startsWith("Nokia") ||
				phoneModel.startsWith("SonyEricssonC") ||
				phoneModel.startsWith("SonyEricssonK550")
			) {
				return 1<<CFGBIT_BACKLIGHT_NOKIA;			
			}
			if (phoneModel.startsWith("SonyEricssonK750") ||
				phoneModel.startsWith("SonyEricssonW800")
			) {
				return 1<<CFGBIT_BACKLIGHT_NOKIAFLASH;
			}
		}
		return 0;
	}
}
