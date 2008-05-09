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

public class Configuration {
	
	private static Logger logger;
	
	public final static int VERSION=1;
	public final static int RENDER_LINE=0;
	public final static int RENDER_STREET=1;

	public final static int LOCATIONPROVIDER_NONE=0;
	public final static int LOCATIONPROVIDER_SIRF=1; 
	public final static int LOCATIONPROVIDER_NMEA=2; 
	public final static int LOCATIONPROVIDER_JSR179=3;

	// bit 0: keep backlight on
	public final static int BACKLIGHT_ON=0;	
	// bit 1: backlight on map screen only
	public final static int BACKLIGHT_MAPONLY=1;	
	// bit 2: backlight method MIDP2
	public final static int BACKLIGHT_MIDP2=2;
	// bit 3: backlight method NOKIA
	public final static int BACKLIGHT_NOKIA=3;	
	// bit 4: backlight method NOKIA/FLASH
	public final static int BACKLIGHT_NOKIAFLASH=4;	
	// MUST contain maximum number of backlight flags
	public final static int BACKLIGHT_OPTIONS_COUNT=5;
	
	/**
	 * These are the database record ids for each configuration option	 * 
	 */
	private static final int  RECORD_ID_BT_URL = 1;
	private static final int RECORD_ID_LOCATION_PROVIDER  = 2;
	private static final int RECORD_ID_RENDER_OPT  = 3;
	private static final int RECORD_ID_GPX_URL = 4;
	private static final int RECORD_ID_MAP_FROM_JAR = 5;
	private static final int RECORD_ID_MAP_FILE_URL = 6;
	private static final int RECORD_ID_BACKLIGHT_DEFAULT = 7;
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

	// Gpx Recording modes
	// GpsMid determines adaptive if a trackpoint is written
	public final static int GPX_RECORD_ADAPTIVE=0;	
	// User specified options define if a trackpoint is written
	public final static int GPX_RECORD_MINIMUM_SECS_DIST=1;	
	
	public static int KEYCODE_CAMERA_COVER_OPEN = -34;
	public static int KEYCODE_CAMERA_COVER_CLOSE = -35;
	public static int KEYCODE_CAMERA_CAPTURE = -26;

	public final static String[] LOCATIONPROVIDER={"None","Sirf","NMEA","JSR179"};
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
	private int render=RENDER_STREET;
	private int detailBoost=0;
	private int backlight;
	private int backlightDefault;
	private String gpxUrl;
	private int debugSeverity;
		
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
				logger.info("No database loaded at the moment");
				return;
			}
			btUrl=readString(database, RECORD_ID_BT_URL);
			locationProvider=readInt(database, RECORD_ID_LOCATION_PROVIDER);
			render=readInt(database, RECORD_ID_RENDER_OPT);
			gpxUrl=readString(database, RECORD_ID_GPX_URL);
			mapFromJar=readInt(database, RECORD_ID_MAP_FROM_JAR) == 0;
			mapFileUrl=readString(database, RECORD_ID_MAP_FILE_URL);
			backlightDefault=readInt(database, RECORD_ID_BACKLIGHT_DEFAULT);
			backlight=backlightDefault;
			rawGpsLogUrl=readString(database, RECORD_ID_LOG_RAW_GPS_URL);
			rawGpsLogEnable = readInt(database, RECORD_ID_LOG_RAW_GPS_ENABLE) !=0;
			detailBoost=readInt(database,RECORD_ID_DETAIL_BOOST); 
			gpxRecordRuleMode=readInt(database, RECORD_ID_GPX_FILTER_MODE); 
			gpxRecordMinMilliseconds=readInt(database, RECORD_ID_GPX_FILTER_TIME); 
			gpxRecordMinDistanceCentimeters=readInt(database, RECORD_ID_GPX_FILTER_DIST); 
			gpxRecordAlwaysDistanceCentimeters=readInt(database, RECORD_ID_GPX_FILTER_ALWAYS_DIST); 
			rawDebugLogUrl=readString(database, RECORD_ID_LOG_DEBUG_URL);
			rawDebugLogEnable = readInt(database,  RECORD_ID_LOG_DEBUG_ENABLE) !=0;
			debugSeverity=readInt(database, RECORD_ID_LOG_DEBUG_SEVERITY);
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

	public int getRender() {
		return render;
	}
	
	public void setRender(int render) {
		this.render = render;
		write(render, RECORD_ID_RENDER_OPT);
	}
	
	public int getBacklight() {
		return backlight;
	}
	
	public void setBacklight(int backlight) {
		this.backlight = backlight;
	}

	public int getBacklightDefault() {
		return backlightDefault;
	}

	public void setBacklightDefault(int backlightDefault) {
		this.backlightDefault = backlightDefault;
			write(backlightDefault, RECORD_ID_BACKLIGHT_DEFAULT);
	}

	public int getDetailBoost() {
		return detailBoost;
	}

	public void setDetailBoost(int detailBoost) {
		this.detailBoost = detailBoost;
			write(detailBoost, RECORD_ID_DETAIL_BOOST);
	}

	
	
	public void setGpxUrl(String url) {
		this.gpxUrl = url;
		write(url, RECORD_ID_GPX_URL);
	}

	public String getGpxUrl() {
		return gpxUrl;
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
			String url = mapFileUrl + name;
			logger.info("Opening file: " + url);
			Connection session = Connector.open(url,Connector.READ);
			FileConnection fileCon = (FileConnection) session;
			if (fileCon == null)
				throw new IOException("Couldn't open url " + url);
						
			is = fileCon.openInputStream();				
			//#else
			//This should never happen.
			is = null;
			logger.fatal("Error, we don't have access to the filesystem, but our map data is supposed to be there!");
			//#endif
			
		}
		return is;
	}

}
