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
	private int render=RENDER_STREET;
	private int backlight;
	private int backlightDefault;
	private String gpxUrl;
		
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
			btUrl=readString(database, 1);
			locationProvider=readInt(database, 2);
			render=readInt(database, 3);
			gpxUrl=readString(database, 4);
			mapFromJar=readInt(database, 5) == 0;
			mapFileUrl=readString(database,6);
			backlightDefault=readInt(database,7);
			backlight=backlightDefault;
			rawGpsLogUrl=readString(database,8);
			rawGpsLogEnable = readInt(database,9) !=0;
			rawDebugLogUrl=readString(database,10);
			rawDebugLogEnable = readInt(database,11) !=0;
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
		write(rawGpsLogUrl, 8);
	}
	
	public void setGpsRawLoggerEnable(boolean enabled) {
		rawGpsLogEnable = enabled;
		if (rawGpsLogEnable) 
			write(1, 9);
		else
			write(0, 9);
	}
	
	public boolean getDebugRawLoggerEnable() {		
		return rawDebugLogEnable;		
	}
	
	public String getDebugRawLoggerUrl() {		
		return rawDebugLogUrl;		
	}
	
	public void setDebugRawLoggerUrl(String url) {
		rawDebugLogUrl = url;
		write(rawDebugLogUrl, 10);
	}
	
	public void setDebugRawLoggerEnable(boolean enabled) {
		rawDebugLogEnable = enabled;
		if (rawDebugLogEnable) 
			write(1, 11);
		else
			write(0, 11);
	}
	
	public boolean getGpsRawLoggerEnable() {		
		return rawGpsLogEnable;
	}

	public String getBtUrl() {
		return btUrl;
	}

	public void setBtUrl(String btUrl) {
		this.btUrl = btUrl;
		write(btUrl, 1);
	}

	public int getLocationProvider() {
		return locationProvider;
	}

	public void setLocationProvider(int locationProvider) {
		this.locationProvider = locationProvider;
		write(locationProvider,2);
	}

	public int getRender() {
		return render;
	}

	public void setRender(int render) {
		this.render = render;
		write(render,3);
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
			write(backlightDefault,7);
	}
	
	public void setGpxUrl(String url) {
		this.gpxUrl = url;
		write(url,4);
	}

	public String getGpxUrl() {
		return gpxUrl;
	}
	
	public boolean usingBuiltinMap() {
		return mapFromJar;
	}
	
	public void setBuiltinMap(boolean mapFromJar) {
		write(mapFromJar?0:1,5);
		this.mapFromJar = mapFromJar;
	}
	
	public String getMapUrl() {
		return mapFileUrl;
	}
	
	public void setMapUrl(String url) {
		write(url,6);
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
			Connection session = Connector.open(url);
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
