/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008, 2009 Kai Krueger apmonkey at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.midlet.gps;

import java.util.Vector;
import java.io.IOException;

import de.enough.polish.util.Locale;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;


import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.data.ProjFactory;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.gpsMid.mapData.WaypointsTile;

import de.ueller.gps.SECellID;

public class GuiDiscover implements CommandListener, ItemCommandListener, 
		GpsMidDisplayable, SelectionListener, IconActionPerformer {

	/** A menu list instance */
	private static final String[] elements = {
		"Location Receiver", "Recording Rules",
		"Display options", "Sounds & Alerts", "Routing options",
		"GPX Receiver", "GUI Options", "Map source", "Debug options", "Key shortcuts",
		"Opencellid",
		//#if polish.api.osm-editing
		"OSM account",
		//#endif
		//#if polish.api.fileconnection
		"Save config", "Load config"
		//#endif
		};

	private static final String LABEL_SELECT_LOGDIR_FIRST = "Please select first the log directory for:";
	private static final String LOG_TO = "Log To: ";

	/**
	 * The following MENU_ITEM constants have to be in sync
	 * with the position in the elements array of the main menu.
	 */
	protected static final int MENU_ITEM_LOCATION = 0;
	protected static final int MENU_ITEM_GPX_FILTER = 1;
	protected static final int MENU_ITEM_DISP_OPT = 2;
	protected static final int MENU_ITEM_SOUNDS_OPT = 3;
	protected static final int MENU_ITEM_ROUTING_OPT = 4;
	protected static final int MENU_ITEM_GPX_DEVICE = 5;
	protected static final int MENU_ITEM_GUI_OPT = 6;
	protected static final int MENU_ITEM_MAP_SRC = 7;
	protected static final int MENU_ITEM_DEBUG_OPT = 8;
	protected static final int MENU_ITEM_KEYS_OPT = 9;
	protected static final int MENU_ITEM_OPENCELLID_OPT = 10;
	//#if polish.api.osm-editing
	protected static final int MENU_ITEM_OSM_OPT = 11;
	protected static final int MENU_ITEM_SAVE_CONFIG = 12;
	protected static final int MENU_ITEM_LOAD_CONFIG = 13;
	//#else
	protected static final int MENU_ITEM_SAVE_CONFIG = 11;
	protected static final int MENU_ITEM_LOAD_CONFIG = 12;
	//#endif

	private static final String[] empty = {};

	/** Soft button for exiting to RootMenu. */
	private final Command EXIT_CMD = new Command("Back", Command.BACK, 2);

	private final Command BACK_CMD = new Command("Cancel", Command.BACK, 2);

	/** Soft button for discovering BT. */
	private final Command OK_CMD = new Command("Ok", Command.OK, 1);

	private final Command STORE_BT_URL = new Command("Select", Command.OK, 2);

	private final Command STORE_ROOTFS = new Command("Select", Command.OK, 2);
	
	private final Command FILE_MAP = new Command("Select Directory", Command.ITEM, 2);
	private final Command BT_MAP	= new Command("Select bluetooth device", Command.ITEM, 2);
	//#if polish.api.osm-editing
	private final Command OSM_URL = new Command("Upload to OSM", Command.ITEM, 2);
	//#endif
	//#if polish.api.online
	private final Command OPENCELLID_APIKEY = new Command("Opencellid apikey", Command.ITEM, 1);
	//#endif
	private final Command GPS_DISCOVER = new Command("Discover GPS", Command.ITEM, 1);
	
	private final Command MANUAL_URL_CMD = new Command("Enter URL", Command.ITEM, 1);
	
	/** Soft button for cache reset. */
	private final Command CELLID_CACHE_RESET_CMD = new Command("Reset cellid cache",
															Command.OK, 2);

	/** A menu list instance */
	private final List menu = new List("Setup", Choice.IMPLICIT, elements, null);

	private List menuBT;

	private final List menuFS = new List("Devices", Choice.IMPLICIT, empty, null);

	private Form					menuSelectLocProv;
	
	private Form					menuSelectMapSource;
	
	private Form					menuDisplayOptions;
	
	private Form					menuGpx;
	
	private Form					menuDebug;

	private Form					menuRecordingOptions;
	
	private Form					menuRoutingOptions;
	
	private Form					menuURLEnter;
	
	//#if polish.api.osm-editing
	private Form					menuOsmAccountOptions;
	//#endif
	
	private Form					menuOpencellidOptions;

	private final GpsMid			parent;

	private DiscoverGps				gps;

	private int						state;

	private final static int		STATE_ROOT		= 0;
	private final static int		STATE_BT_GPS	= 2;
	private final static int		STATE_LP		= 3;
	private final static int		STATE_RBT		= 4;
	private final static int		STATE_GPX		= 5;
	private final static int		STATE_MAP		= 6;
	private final static int		STATE_DISPOPT	= 7;
	private final static int		STATE_RECORDING_OPTIONS	= 8;
	private final static int 		STATE_BT_GPX	= 9;
	private final static int		STATE_DEBUG		= 10;
	//#if polish.api.osm-editing
	private final static int		STATE_OSM_OPT = 11;
	//#endif
	private final static int		STATE_OPENCELLID_OPT = 12;
	private final static int		STATE_LOAD_CONFIG = 13;
	private final static int		STATE_SAVE_CONFIG = 14;
	private final static int		STATE_URL_ENTER_GPS = 15;
	private final static int		STATE_URL_ENTER_GPX = 16;
	
	private Vector urlList;
	private Vector friendlyName;
	private ChoiceGroup locProv;
	private ChoiceGroup choiceGpxRecordRuleMode;
	private ChoiceGroup choiceWptInTrack;
	private ChoiceGroup choiceWptInWpstore;
	private TextField  tfGpxRecordMinimumSecs;
	private TextField  tfGpxRecordMinimumDistanceMeters;
	private TextField  tfGpxRecordAlwaysDistanceMeters;
	private TextField  tfURL;
	//#if polish.api.osm-editing
	private TextField  tfOsmUserName;
	private TextField  tfOsmPassword;
	private TextField  tfOsmUrl;
	//#endif
	private TextField  tfOpencellidApikey;
	private ChoiceGroup rawLogCG;
	private ChoiceGroup mapSrc;
	private ChoiceGroup mapSrcOptions;
	private ChoiceGroup rotationGroup;
	private ChoiceGroup nightModeGroup;
	private ChoiceGroup uiLangGroup;
	private ChoiceGroup naviLangGroup;
	private ChoiceGroup renderOpts;
	private ChoiceGroup metricUnits;
	private TextField	tfAutoRecenterToGpsSecs;
	private ChoiceGroup backlightOpts;
	private ChoiceGroup sizeOpts;
	private ChoiceGroup mapInfoOpts;
	private ChoiceGroup debugLog;
	private ChoiceGroup debugSeverity;
	private ChoiceGroup debugOther;
	private StringItem  gpxUrl;
	private StringItem  gpsUrl;
	private ChoiceGroup autoConnect;
	private ChoiceGroup btKeepAlive;
	private ChoiceGroup btAutoRecon;
	  
	private String gpsUrlStr;
	private String rawLogDir;

	private ChoiceGroup gpxOptsGroup;
	private ChoiceGroup cellidOptsGroup;

	private final static Logger logger = Logger.getInstance(GuiDiscover.class, Logger.DEBUG);

	private static GuiDiscoverIconMenu setupIconMenu = null;
	
	public GuiDiscover(GpsMid parent) {
		this.parent = parent;

		state = STATE_ROOT;

		//Prepare Main Menu
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		menu.setSelectCommand(OK_CMD);

		//Prepare ??? menu
		menuFS.addCommand(BACK_CMD);
		menuFS.setCommandListener(this);

		show();
	}

	private void initBluetoothSelect() {
		//Prepare Bluetooth selection menu
		logger.info("Starting bluetooth setup menu");
		menuBT = new List("Devices", Choice.IMPLICIT, empty, null);
		menuBT.addCommand(OK_CMD);
		menuBT.addCommand(BACK_CMD);
		menuBT.addCommand(MANUAL_URL_CMD);
		menuBT.setSelectCommand(OK_CMD);
		menuBT.setCommandListener(this);
		menuBT.setTitle("Search Service");
	}

	private void initRecordingSetupMenu() {
		//Prepare Recording Options selection menu
		logger.info("Starting Recording setup menu");
		menuRecordingOptions = new Form("Recording Rules");
		menuRecordingOptions.addCommand(BACK_CMD);
		menuRecordingOptions.addCommand(OK_CMD);
		menuRecordingOptions.setCommandListener(this);
		String [] recModes = new String[2];
		recModes[0] = "adaptive to speed";
		recModes[1] = "manual rules:";
		choiceGpxRecordRuleMode = new ChoiceGroup("Record Trackpoints", Choice.EXCLUSIVE, recModes ,null);

		tfGpxRecordMinimumSecs = new TextField("Minimum seconds between trackpoints (0=disabled)", "0", 3, TextField.DECIMAL);
		tfGpxRecordMinimumDistanceMeters = new TextField("Minimum meters between trackpoints (0=disabled)", "0", 3, TextField.DECIMAL);
		tfGpxRecordAlwaysDistanceMeters = new TextField("Always record when exceeding these meters between trackpoints (0=disabled)", "0", 3, TextField.DECIMAL);
		
		String [] wptFlag = new String[1];
		wptFlag[0] = "Also put waypoints in track";
		choiceWptInTrack = new ChoiceGroup("Waypoints in track", Choice.MULTIPLE,
				wptFlag, null);
		choiceWptInTrack.setSelectedIndex(0, Configuration.getCfgBitSavedState(
				Configuration.CFGBIT_WPTS_IN_TRACK));
		String [] gpxNameOpts = new String[2];
		boolean[] selGpxName = new boolean[2];
		gpxNameOpts[0] = "Ask track name at start of recording";
		gpxNameOpts[1] = "Ask track name at end of recording";
		selGpxName[0] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_START);
		selGpxName[1] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_STOP);
		
		gpxOptsGroup = new ChoiceGroup("Track Naming", Choice.MULTIPLE, gpxNameOpts ,null);
		gpxOptsGroup.setSelectedFlags(selGpxName);

		menuRecordingOptions.append(choiceGpxRecordRuleMode);
		menuRecordingOptions.append(tfGpxRecordMinimumSecs);
		menuRecordingOptions.append(tfGpxRecordMinimumDistanceMeters);
		menuRecordingOptions.append(tfGpxRecordAlwaysDistanceMeters);
		menuRecordingOptions.append(gpxOptsGroup);
		menuRecordingOptions.append(choiceWptInTrack);
	}

	private void initDebugSetupMenu() {
		//Prepare Debug selection menu
		logger.info("Starting Debug setup menu");
		menuDebug = new Form("Debug options");
		String [] loggings = new String[1];
		menuDebug.addCommand(BACK_CMD);
		menuDebug.addCommand(OK_CMD);
		menuDebug.addCommand(FILE_MAP);
		menuDebug.setCommandListener(this);
		boolean [] selDebug = new boolean[1];
		selDebug[0] = Configuration.getDebugRawLoggerEnable();
		loggings = new String[1];
		loggings[0] = Configuration.getDebugRawLoggerUrl();
		if (loggings[0] == null) {
			loggings[0] = "Please select directory";
		}
		debugLog = new ChoiceGroup("Debug event logging to:", ChoiceGroup.MULTIPLE, loggings, null);
		debugLog.setSelectedFlags(selDebug);
		menuDebug.append(debugLog);

		loggings = new String[3];
		selDebug = new boolean[3];
		selDebug[0] = Configuration.getDebugSeverityInfo();
		selDebug[1] = Configuration.getDebugSeverityDebug();
		selDebug[2] = Configuration.getDebugSeverityTrace();
		loggings[0] = "Info";
		loggings[1] = "Debug";
		loggings[2] = "Trace";
		debugSeverity = new ChoiceGroup("Log severity:", ChoiceGroup.MULTIPLE, loggings, null);
		debugSeverity.setSelectedFlags(selDebug);
		menuDebug.append(debugSeverity);

		loggings = new String[3];
		loggings[0] = "Show route connections";
		loggings[1] = "Show turn restrictions";
		loggings[2] = "Show inconsistent bearings";
		debugOther = new ChoiceGroup("Other:", ChoiceGroup.MULTIPLE, loggings, null);
		debugOther.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_CONNECTIONS));
		debugOther.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS));
		debugOther.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BEARINGS));
		menuDebug.append(debugOther);

	}

	private void initLocationSetupMenu() {
		//Prepare Location Provider setup menu
		logger.info("Starting Locationreceiver setup menu");

		menuSelectLocProv = new Form("Location Receiver");

		menuSelectLocProv.addCommand(BACK_CMD);
		menuSelectLocProv.addCommand(OK_CMD);
		menuSelectLocProv.addCommand(GPS_DISCOVER);
		menuSelectLocProv.addCommand(FILE_MAP);

		gpsUrl = new StringItem("GPS: ", null);
		gpsUrl.setDefaultCommand(GPS_DISCOVER);
		gpsUrl.setItemCommandListener(this);
		locProv = new ChoiceGroup("input from:", Choice.EXCLUSIVE, Configuration.LOCATIONPROVIDER, new Image[Configuration.LOCATIONPROVIDER.length]);

		final String[] logCategories = {"Cell-IDs for OpenCellID.org", "Raw Gps Data"};
		rawLogCG = new ChoiceGroup(LABEL_SELECT_LOGDIR_FIRST, ChoiceGroup.MULTIPLE, logCategories, new Image[2]);

		String [] aconn = new String[1];
		aconn[0] = "Start GPS at startup";
		autoConnect = new ChoiceGroup("GPS start", ChoiceGroup.MULTIPLE, aconn, null);

		String [] btka = new String[1];
		btka[0] = "Send keep alives";
		btKeepAlive = new ChoiceGroup("BT keep alive", ChoiceGroup.MULTIPLE, btka, null);

		String [] btar = new String[1];
		btar[0] = "Auto reconnect GPS";
		btAutoRecon = new ChoiceGroup("BT reconnect", ChoiceGroup.MULTIPLE, btar, null);

		menuSelectLocProv.append(gpsUrl);
		menuSelectLocProv.append(autoConnect);
		menuSelectLocProv.append(btKeepAlive);
		menuSelectLocProv.append(btAutoRecon);
		menuSelectLocProv.append(locProv);
		menuSelectLocProv.append(rawLogCG);

		menuSelectLocProv.setCommandListener(this);
	}

	private void initMapSource() {
		//Prepare Map Source selection menu
		logger.info("Starting map source setup menu");
		menuSelectMapSource = new Form("Select Map Source");
		menuSelectMapSource.addCommand(BACK_CMD);
		menuSelectMapSource.addCommand(OK_CMD);
		menuSelectMapSource.addCommand(FILE_MAP);
		String [] sources = new String[2];
		sources[0] = "Built-in map";
		sources[1] = "Filesystem: ";
		mapSrc = new ChoiceGroup("Map source:", Choice.EXCLUSIVE, sources, null);

		String [] preferInternal = new String[1];
		preferInternal[0] = "Prefer built-in POI PNGs (faster startup e.g. on some Nokias)";
		mapSrcOptions = new ChoiceGroup("Options", ChoiceGroup.MULTIPLE, preferInternal, null);
		
		menuSelectMapSource.append(mapSrc);
		menuSelectMapSource.append(mapSrcOptions);
		menuSelectMapSource.setCommandListener(this);
	}

	private void initDisplay() {
		//Prepare Display options menu
		logger.info("Starting display setup menu");

		menuDisplayOptions = new Form("Display Options");

		menuDisplayOptions.addCommand(BACK_CMD);
		menuDisplayOptions.addCommand(OK_CMD);

		if (Legend.numUiLang > 1) {
			String [] uiLang = new String[Legend.numUiLang];
			for (int i = 0; i < Legend.numUiLang; i++) {
				uiLang[i] = Legend.uiLangName[i];
			}
			uiLangGroup = new ChoiceGroup("Language", Choice.EXCLUSIVE, uiLang, null);
			menuDisplayOptions.append(uiLangGroup);
		}
		if (Legend.numNaviLang > 1) {
			String [] naviLang = new String[Legend.numNaviLang];
			for (int i = 0; i < Legend.numNaviLang; i++) {
				naviLang[i] = Legend.naviLangName[i];
			}
			naviLangGroup = new ChoiceGroup("Sound/Navi language", Choice.EXCLUSIVE, naviLang, null);
			menuDisplayOptions.append(naviLangGroup);
		}
		// FIXME add dialogue for online & street name language switch,
		// maybe make a submenu or a separate language menu
		String [] nightMode = new String[2];
		nightMode[0] = "Day Mode";
		nightMode[1] = "Night Mode";
		nightModeGroup = new ChoiceGroup("Colors", Choice.EXCLUSIVE, nightMode, null);
		menuDisplayOptions.append(nightModeGroup);

		String [] rotation = ProjFactory.name;
		rotationGroup = new ChoiceGroup("Map Rotation", Choice.EXCLUSIVE, rotation, null);
		menuDisplayOptions.append(rotationGroup);

		String [] renders = new String[2];
		renders[0] = "as lines";
		renders[1] = "as streets";
		renderOpts = new ChoiceGroup("Rendering Options:", Choice.EXCLUSIVE, renders, null);
		menuDisplayOptions.append(renderOpts);
		
		String [] metricUnit = new String[1];
		metricUnit[0] = "metric units";
		metricUnits = new ChoiceGroup("Units", Choice.MULTIPLE, metricUnit, null);
		menuDisplayOptions.append(metricUnits);

		tfAutoRecenterToGpsSecs = new TextField("Auto-recenter to GPS after no user action for these seconds (0=disabled)",
				Integer.toString(Configuration.getAutoRecenterToGpsMilliSecs() / 1000),
				2, TextField.DECIMAL);
		menuDisplayOptions.append(tfAutoRecenterToGpsSecs);

		String [] backlights;
		byte i = 4;
		//#if polish.api.nokia-ui
		i += 2;
		//#endif
		//#if polish.api.min-siemapi
		i++;
		//#endif
		backlights = new String[i];

		backlights[0] = "Keep backlight on";
		backlights[1] = "only while GPS started";
		backlights[2] = "only as keep-alive";
		backlights[3] = "with MIDP2.0";
		i = 4;
		//#if polish.api.nokia-ui
		backlights[i++] = "with Nokia API";
		backlights[i++] = "with Nokia Flashlight";
		//#endif
		//#if polish.api.min-siemapi
		backlights[i++] = "with Siemens API";
		//#endif

		backlightOpts = new ChoiceGroup("Backlight Options:",
				Choice.MULTIPLE, backlights, null);
		menuDisplayOptions.append(backlightOpts);

		String [] sizes = new String[2];
		sizes[0] = "larger POI labels";
		sizes[1] = "larger waypoint labels";
		sizeOpts = new ChoiceGroup("Size Options:", Choice.MULTIPLE, sizes, null);
		menuDisplayOptions.append(sizeOpts);

		String [] mapInfos = new String[6];
		mapInfos[0] = "Point of compass in rotated map";
		mapInfos[1] = "Scale bar";
		mapInfos[2] = "Speed when driving";
		mapInfos[3] = "Altitude from GPS";
		mapInfos[4] = "Air distance to dest. when not routing";
		mapInfos[5] = "Clock with current time";
		mapInfoOpts = new ChoiceGroup("Infos in Map Screen:",
				Choice.MULTIPLE, mapInfos, null);
		menuDisplayOptions.append(mapInfoOpts);
				
		menuDisplayOptions.setCommandListener(this);
	}
	
	//#if polish.api.osm-editing
	private void initOSMaccountOptions() {
		//Prepare Debug selection menu
		logger.info("Starting OSM account setup menu");
		menuOsmAccountOptions = new Form("OpenStreetMap account");
		menuOsmAccountOptions.addCommand(BACK_CMD);
		menuOsmAccountOptions.addCommand(OK_CMD);
		menuOsmAccountOptions.setCommandListener(this);
		
		tfOsmUserName = new TextField("User name:", Configuration.getOsmUsername(), 100, TextField.ANY);
		tfOsmPassword = new TextField("Password:", Configuration.getOsmPwd(), 100, TextField.ANY | TextField.PASSWORD);
		tfOsmUrl = new TextField("Server URL:", Configuration.getOsmUrl(), 255, TextField.URL);
		
		menuOsmAccountOptions.append(tfOsmUserName);
		menuOsmAccountOptions.append(tfOsmPassword);
		menuOsmAccountOptions.append(tfOsmUrl);
		
	}
	//#endif

	private void initOpencellidOptions() {
		//Prepare Debug selection menu
		logger.info("Starting Opencellid apikey setup menu");
		menuOpencellidOptions = new Form("Opencellid");
		menuOpencellidOptions.addCommand(BACK_CMD);
		menuOpencellidOptions.addCommand(OK_CMD);
		menuOpencellidOptions.addCommand(CELLID_CACHE_RESET_CMD);
		menuOpencellidOptions.setCommandListener(this);
		//tfOpencellidApikey = new TextField("apikey:", Configuration.getOpencellidApikey(), 100, TextField.ANY);
		
		//menuOpencellidOptions.append(tfOpencellidApikey);

		String [] cellidOpts = new String[2];
		boolean[] opencellidFlags = new boolean[2];

		cellidOpts[0] = "Don't use online cellid lookups";
		cellidOpts[1] = "Use only online cellid lookups";
		//cellidOpts[2] = "Upload log always";
		//cellidOpts[3] = "Confirmation before log upload";
		//cellidOpts[4] = "Fallback to cellid when no fix";
		
		cellidOptsGroup = new ChoiceGroup("Cellid options", Choice.MULTIPLE, cellidOpts, null);
		opencellidFlags[0] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_OFFLINEONLY);
		opencellidFlags[1] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_ONLINEONLY);
		//opencellidFlags[2] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_ALWAYS);
		//opencellidFlags[3] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_CONFIRM);
		//opencellidFlags[4] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_FALLBACK);

		cellidOptsGroup.setSelectedFlags(opencellidFlags);
		menuOpencellidOptions.append(cellidOptsGroup);
	}

	public void commandAction(Command c, Item i) {
		// forward item command action to form
		commandAction(c, (Displayable) null);
	}
	
	public void commandAction(Command c, Displayable d) {

		if (c == EXIT_CMD) {
			destroy();
       		Trace.getInstance().show();
			return;
		}
		if (c == BACK_CMD) {
			if (state == STATE_BT_GPX) {
				state = STATE_GPX;
			} else if (state == STATE_BT_GPS) {
				state = STATE_LP;
			} else if (state == STATE_URL_ENTER_GPS) {
				state = STATE_LP;
			} else if (state == STATE_URL_ENTER_GPX) {
				state = STATE_GPX;
			} else {
				state = STATE_ROOT;
			}
			show();
			return;
		}
		if (c == FILE_MAP) {
			//#if polish.api.fileconnection
			String initialDir = "";
			String title = "Select Directory";
			switch (state) {
				case STATE_LP:
					title = "Raw GPS/CellID Log Directory";
					initialDir = (rawLogDir == null) ? "" : rawLogDir;
					break;
				case STATE_MAP:
					title = "Map ZipFile or Directory";
					// get initialDir from form
					String url = mapSrc.getString(1);
					// skip "Filesystem: "
					url = url.substring(url.indexOf(":") + 2);
					initialDir = url;
					break;
				case STATE_GPX:
					title = "Gpx Directory";
					initialDir = gpxUrl.getText();
					break;
				case STATE_DEBUG:
					title = "Log Directory";
					initialDir = Configuration.getDebugRawLoggerUrl();
					break;
			}
			// no valid url, i.e. "Please select to destination first" ?
			if(initialDir != null && !initialDir.toLowerCase().startsWith("file:///")) {
				initialDir = null;
			}
			FsDiscover fsd = new FsDiscover(this, this, initialDir, (state == STATE_MAP) ? false : true, "", title);
			fsd.show();
			//#else
			//logger.error("Files system support is not compiled into this version");
			//#endif
		}
		if (c == MANUAL_URL_CMD) {
			menuURLEnter = new Form("Enter connection url");
			tfURL = new TextField("URL", gpsUrlStr, 256, TextField.ANY);
			menuURLEnter.addCommand(OK_CMD);
			menuURLEnter.addCommand(BACK_CMD);
			menuURLEnter.setCommandListener(this);
			menuURLEnter.append(tfURL);
			GpsMid.getInstance().show(menuURLEnter);
			if (state == STATE_BT_GPS) {
				state = STATE_URL_ENTER_GPS;
			} else {
				state = STATE_BT_GPX;
			}
		}
		if (c == GPS_DISCOVER || c == BT_MAP) {
			//#if polish.api.btapi
			initBluetoothSelect();
			urlList = new Vector();
			friendlyName = new Vector();
			menuBT.deleteAll();
			GpsMid.getInstance().show(menuBT);
			if (state == STATE_LP) {
				logger.info("Discovering a bluetooth serial device");
				state = STATE_BT_GPS;
				gps = new DiscoverGps(this, DiscoverGps.UUDI_SERIAL);
			} else {
				logger.info("Discovering a bluetooth obex file device");
				state = STATE_BT_GPX;
				gps = new DiscoverGps(this, DiscoverGps.UUDI_FILE);
			}
			 
			//#else
				logger.error("Bluetooth is not compiled into this version");
			//#endif
		}
		//#if polish.api.osm-editing
		if (c == OSM_URL) {
			gpxUrl.setText(Configuration.getOsmUrl() + "gpx/create");
		}
		if (c == CELLID_CACHE_RESET_CMD) {
			SECellID.deleteCellIDRecordStore();
		}
		//#endif
		if (c == OK_CMD) {
			switch (state) {
			case STATE_ROOT:
				showSetupDialog(menu.getSelectedIndex());
				break;
			case STATE_BT_GPS:
				// remember discovered BT URL and put status in form
				if(urlList.size() != 0) {
					gpsUrlStr = (String) urlList.elementAt(menuBT.getSelectedIndex());
					gpsUrl.setText((gpsUrlStr == null) ? "<Discover>" : "<Discovered>");
				}
				state = STATE_LP;
				show();
				break;
			case STATE_BT_GPX:
				// put discovered BT Url in form
				if(urlList.size() != 0) {
					String gpxUrlStr = (String) urlList.elementAt(menuBT.getSelectedIndex());
					gpxUrl.setText(gpxUrlStr == null ? "<Please select in menu>" : gpxUrlStr);
				}
				state = STATE_GPX;

				show();
				break;
			case STATE_GPX:
				// Save GpxUrl from form to Configuration
				String str = gpxUrl.getText();
				// don't save "Please select..."
				if (str.indexOf(":") == -1) {
					str = null;
				}
				Configuration.setGpxUrl(str);
				state = STATE_ROOT;
				show();
				break;
			case STATE_RECORDING_OPTIONS:
				String rule;
				// Save Record Rules to Config
				Configuration.setGpxRecordRuleMode(choiceGpxRecordRuleMode.getSelectedIndex());
				rule = tfGpxRecordMinimumSecs.getString();
				Configuration.setGpxRecordMinMilliseconds(
						rule.length() == 0 ? 0 :(int) (1000 * Float.parseFloat(rule))
				);
				rule = tfGpxRecordMinimumDistanceMeters.getString();
				Configuration.setGpxRecordMinDistanceCentimeters(
						rule.length() == 0 ? 0 :(int) (100 * Float.parseFloat(rule))
				);
				rule = tfGpxRecordAlwaysDistanceMeters.getString();
				Configuration.setGpxRecordAlwaysDistanceCentimeters(
						rule.length() == 0 ? 0 :(int) (100 * Float.parseFloat(rule))
				);
				// Save "waypoints in track" flag to config
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_WPTS_IN_TRACK,
						choiceWptInTrack.isSelected(0));

				boolean[] selGpxName = new boolean[2];
				gpxOptsGroup.getSelectedFlags(selGpxName);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_START, selGpxName[0]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_GPX_ASK_TRACKNAME_STOP, selGpxName[1]);


				state = STATE_ROOT;
				show();
				break;
			case STATE_LP:
				Configuration.setBtUrl(gpsUrlStr);
				Configuration.setLocationProvider(locProv.getSelectedIndex());
				boolean [] selraw = new boolean[2];
				rawLogCG.getSelectedFlags(selraw);
				Configuration.setGpsRawLoggerUrl(rawLogDir);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_LOGGING, selraw[0]);
				Configuration.setGpsRawLoggerEnable(selraw[1]);

				autoConnect.getSelectedFlags(selraw);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_AUTO_START_GPS, selraw[0]);
				btKeepAlive.getSelectedFlags(selraw);
				Configuration.setBtKeepAlive(selraw[0]);
				btAutoRecon.getSelectedFlags(selraw);
				Configuration.setBtAutoRecon(selraw[0]);
				state = STATE_ROOT;
				show();
				break;
			case STATE_MAP:
				Configuration.setBuiltinMap((mapSrc.getSelectedIndex() == 0));
				// extract map url from form and save it to Configuration
				String url = mapSrc.getString(1);
				// skip "Filesystem: "
				url = url.substring(url.indexOf(":") + 2);
				// no valid url, i.e. "Please select..."
				if (url.indexOf(":") == -1) {
					url = null;
				}
				Configuration.setMapUrl(url);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS, mapSrcOptions.isSelected(0));
				state = STATE_ROOT;
				show();
				logger.fatal("Need to restart GpsMid, otherwise map is in an inconsistant state" + url+Configuration.getMapUrl());
				break;
			case STATE_DISPOPT:
				if (Legend.numUiLang > 1) {
					String uiLang = Legend.uiLang[uiLangGroup.getSelectedIndex()];
					try {
						Locale.loadTranslations( "/" + uiLang + ".loc" );
					} catch (IOException ioe) {
						System.out.println("Couldn't open translations file");
					}
					if (!uiLang.equals(Configuration.getUiLang())) { 
						Trace.uncacheIconMenu();
						uncacheIconMenu();
					}
					Configuration.setUiLang(uiLang);
					Configuration.setOnlineLang(Legend.uiLang[uiLangGroup.getSelectedIndex()]);
					Configuration.setWikipediaLang(Legend.uiLang[uiLangGroup.getSelectedIndex()]);
					Configuration.setNamesOnMapLang(Legend.uiLang[uiLangGroup.getSelectedIndex()]);
				}
				if (Legend.numNaviLang > 1) {
					String naviLang = Legend.naviLang[naviLangGroup.getSelectedIndex()];
					boolean multipleDirsForLanguage = false;
					if (!naviLang.equals(Configuration.getNaviLang())) { 
						// selected language changed
						String soundDirBase[] = new String[Legend.soundDirectories.length];
						int soundDirBaseCount = 0;
						String soundDir = null;
						for (int i = 0; i < Legend.soundDirectories.length; i++) {
							// build a table of basenames by matching "-xx" at end of dirname
							if (Legend.soundDirectories[i] != null && Legend.soundDirectories[i].length() > 3) {
								if (Legend.soundDirectories[i].substring(Legend.soundDirectories[i].length() - 3, Legend.soundDirectories[i].length() - 2).equals("-")) {
									String basename = new String(Legend.soundDirectories[i].substring(0, Legend.soundDirectories[i].length() - 3));
									boolean duplicate = false;
									for (int j = 0; j < soundDirBaseCount; j++) {
										if (basename.equals(soundDirBase[j])) {
											duplicate = true;
										}
									}
									if (! duplicate) {
										soundDirBase[soundDirBaseCount] = basename;
										soundDirBaseCount++;
									}
									}
								}
							// set dir if a language match is found
							if (Legend.soundDirectories[i].substring(Legend.soundDirectories[i].length() - 3).equals("-" + naviLang)) {
								// first suitable-language sound dir is taken into use
								if (soundDir == null) {
									soundDir = Legend.soundDirectories[i];
								} else {
									multipleDirsForLanguage = true;
								}
							}
						}
						// store language choice. Sound directory choice takes preference over it.
						Configuration.setNaviLang(Legend.naviLang[naviLangGroup.getSelectedIndex()]);
						if (soundDir != null) {
							Configuration.setSoundDirectory(soundDir);
							RouteSyntax.getInstance().readSyntax();
						} else {
							// special case English as default language
							// if -en was not found, use a matching basename 

							for (int i = 0; i < Legend.soundDirectories.length; i++) {
								for (int j = 0; j < soundDirBaseCount; j++) {
									if (soundDirBase[j].equals(Legend.soundDirectories[i])) {
										soundDir = soundDirBase[j];
									}
								}
							}
							if (soundDir != null) {
								Configuration.setSoundDirectory(soundDir);
								RouteSyntax.getInstance().readSyntax();
							}
						}

						if (soundDir == null) {
							logger.error("Sound directory for " + naviLang + " not found!");
						}
						//if (multipleDirsForLanguage) {
							// FIXME: if more than one dir for lang available,
							// tell the user it can be switched at "sounds&alerts" menu
							// ("Changing language", "Selecting " + soundDir, 3000);
						//}
					}
				}
				boolean nightMode = (nightModeGroup.getSelectedIndex() == 1);
				
				if (nightMode != Configuration.getCfgBitState(Configuration.CFGBIT_NIGHT_MODE) ) {
					Configuration.setCfgBitSavedState(Configuration.CFGBIT_NIGHT_MODE, nightMode);
					Legend.reReadLegend();
					Trace trace = Trace.getInstance();
					trace.recreateTraceLayout();
				}

				Configuration.setProjTypeDefault( (byte) rotationGroup.getSelectedIndex() );
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_STREETRENDERMODE,
						(renderOpts.getSelectedIndex() == 1)
				);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_POI_LABELS_LARGER, sizeOpts.isSelected(0));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_WPT_LABELS_LARGER, sizeOpts.isSelected(1));
				
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS, mapInfoOpts.isSelected(0));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_SCALE_BAR, mapInfoOpts.isSelected(1));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_SPEED_IN_MAP, mapInfoOpts.isSelected(2));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_ALTITUDE_IN_MAP, mapInfoOpts.isSelected(3));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_IN_MAP, mapInfoOpts.isSelected(4));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_CLOCK_IN_MAP, mapInfoOpts.isSelected(5));
				
				String secs = tfAutoRecenterToGpsSecs.getString();
				Configuration.setAutoRecenterToGpsMilliSecs(
						(int) (Float.parseFloat(secs)) * 1000
				);
				
				// convert boolean array with selection states for backlight
				// to one flag with corresponding bits set
				boolean[] sellight = new boolean[7];
				backlightOpts.getSelectedFlags( sellight );
	            // save selected values to record store
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ON, sellight[0]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED, sellight[1]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_KEEPALIVE, sellight[2]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_MIDP2, sellight[3]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIA, sellight[4]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH, sellight[5]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_SIEMENS, sellight[6]);
				
				sellight = new boolean[1];
				metricUnits.getSelectedFlags(sellight);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_METRIC, sellight[0]);
				
				state = STATE_ROOT;
				show();

				parent.restartBackLightTimer();
				break;
			case STATE_DEBUG:
				boolean [] selDebug = new boolean[1];
				debugLog.getSelectedFlags(selDebug);
				Configuration.setDebugRawLoggerEnable((selDebug[0]));
				Configuration.setDebugRawLoggerUrl(debugLog.getString(0));
				GpsMid.getInstance().enableDebugFileLogging();
				selDebug = new boolean[3];
				debugSeverity.getSelectedFlags(selDebug);
				
				Configuration.setDebugSeverityInfo(selDebug[0]);
				Configuration.setDebugSeverityDebug(selDebug[1]);
				Configuration.setDebugSeverityTrace(selDebug[2]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_CONNECTIONS, debugOther.isSelected(0));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS, debugOther.isSelected(1));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BEARINGS, debugOther.isSelected(2));
				Logger.setGlobalLevel();
				state = STATE_ROOT;
				this.show();
				
				/**
				 * In order to minimise surprise of the user that despite enabling logging here
				 * nothing shows up in the log file, we warn the user if logging at the specified level
				 * is not compiled into the current Version of GpsMid
				 * 
				 * The check needs to be after this.show() so that the alert messages can be shown
				 * and not disabled by the immediate call to this.show()
				 */
				boolean debugAvail = false;
				//#debug trace
				debugAvail = true;
				if (selDebug[2] && !debugAvail) {
					logger.error("Logging at \"Trace\" level is not compiled into this version of GpsMid so log will be empty");
				}
				debugAvail = false;
				//#debug debug
				debugAvail = true;
				if (selDebug[1] && !debugAvail) {
					logger.error("Logging at \"Debug\" level is not compiled into this version of GpsMid so log will be empty");
				}
				debugAvail = false;
				//#debug info
				debugAvail = true;
				if (selDebug[0] && !debugAvail) {
					logger.error("Logging at \"Info\" level is not compiled into this version of GpsMid so log will be empty");
				}
				
				break;

			//#if polish.api.osm-editing
			case STATE_OSM_OPT:
				Configuration.setOsmUsername(tfOsmUserName.getString());
				Configuration.setOsmPwd(tfOsmPassword.getString());
				Configuration.setOsmUrl(tfOsmUrl.getString());
				state = STATE_ROOT;
				this.show();
				break;
			//#endif
			case STATE_OPENCELLID_OPT:
				//Configuration.setOpencellidApikey(tfOpencellidApikey.getString());
				boolean[] opencellidFlags = new boolean[2];
				cellidOptsGroup.getSelectedFlags(opencellidFlags);

				Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_OFFLINEONLY,
							     opencellidFlags[0]);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_ONLINEONLY,
							     opencellidFlags[1]);
				//Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_ALWAYS,
				//			     opencellidFlags[2]);
				//Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_CONFIRM,
				//			     opencellidFlags[3]);
				//Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_FALLBACK,
				//			     opencellidFlags[4]);
				state = STATE_ROOT;
				this.show();
				//#if polish.api.online
				//#else
				if (opencellidFlags[1]) {
					logger.error("Online access is not compiled into this midlet");
				}
				//#endif

				break;
			case STATE_URL_ENTER_GPS:
				gpsUrlStr = tfURL.getString();
				state = STATE_LP;
				show();
				break;
			
			case STATE_URL_ENTER_GPX:
				gpsUrlStr = tfURL.getString();
				state = STATE_GPX;
				show();
				break;
			}
		}
	}

	private void showSetupDialog(int menuNr) {
		switch (menuNr) {
			case MENU_ITEM_LOCATION: // Location Receiver
				initLocationSetupMenu();
				gpsUrlStr = Configuration.getBtUrl();
				gpsUrl.setText(gpsUrlStr == null ? "<Discover>" : "<Discovered>");
				int selIdx = Configuration.getLocationProvider();
				locProv.setSelectedIndex(selIdx, true);
				
				String logUrl;
				rawLogDir = Configuration.getGpsRawLoggerUrl();
				if (rawLogDir == null) {
					logUrl = LABEL_SELECT_LOGDIR_FIRST;
				} else {
					logUrl = LOG_TO + rawLogDir;
				}
				rawLogCG.setLabel(logUrl);
		
				boolean [] selLog = new boolean[2];
				selLog[0] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_LOGGING);
				selLog[1] = Configuration.getGpsRawLoggerEnable();
				rawLogCG.setSelectedFlags(selLog);

				autoConnect.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_AUTO_START_GPS));
				btKeepAlive.setSelectedIndex(0, Configuration.getBtKeepAlive());
				btAutoRecon.setSelectedIndex(0, Configuration.getBtAutoRecon());
				Display.getDisplay(parent).setCurrentItem(gpsUrl);
				//Display.getDisplay(parent).setCurrent(menuSelectLocProv);
				GpsMid.getInstance().show(menuSelectLocProv);
				state = STATE_LP;
				break;
			case MENU_ITEM_GPX_FILTER: // Recording Rules
				initRecordingSetupMenu();
				choiceGpxRecordRuleMode.setSelectedIndex(Configuration.getGpxRecordRuleMode(), true);
				/*
				 * minimum seconds between trackpoints
				 */
				tfGpxRecordMinimumSecs.setString(
					getCleanFloatString( (float)(Configuration.getGpxRecordMinMilliseconds()) / 1000, 3 )
				);
		
				/*
				 * minimum meters between trackpoints
				 */
				tfGpxRecordMinimumDistanceMeters.setString(
					getCleanFloatString( (float)(Configuration.getGpxRecordMinDistanceCentimeters()) / 100, 3 )
				);
		
				/*
				 * meters between trackpoints that will always create a new trackpoint
				 */
				tfGpxRecordAlwaysDistanceMeters.setString(
					getCleanFloatString( (float)(Configuration.getGpxRecordAlwaysDistanceCentimeters()) / 100, 3 )
				);
		
				GpsMid.getInstance().show(menuRecordingOptions);
				state = STATE_RECORDING_OPTIONS;
				break;
			case MENU_ITEM_DISP_OPT: // Display Options
				initDisplay();
				int langNum = 0; // default is the first in bundle
				if (Legend.numUiLang > 1) {
					String lang = Configuration.getUiLang();
					for (int i = 0; i < Legend.numUiLang; i++) {
						if (Legend.uiLang[i].equalsIgnoreCase(lang)) {
							langNum = i;
						}
					}
					uiLangGroup.setSelectedIndex( langNum, true);
				}
				langNum = 0;
				if (Legend.numNaviLang > 1) {
					String lang = Configuration.getNaviLang();
					for (int i = 0; i < Legend.numNaviLang; i++) {
						if (Legend.naviLang[i].equalsIgnoreCase(lang)) {
							langNum = i;
						}
					}
					naviLangGroup.setSelectedIndex( langNum, true);
				}
				nightModeGroup.setSelectedIndex( Configuration.getCfgBitSavedState(Configuration.CFGBIT_NIGHT_MODE) ? 1 : 0, true);
				rotationGroup.setSelectedIndex(Configuration.getProjDefault(), true);
				renderOpts.setSelectedIndex( Configuration.getCfgBitSavedState(Configuration.CFGBIT_STREETRENDERMODE) ? 1 : 0, true);
				sizeOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_POI_LABELS_LARGER));
				sizeOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_WPT_LABELS_LARGER));
				mapInfoOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS));
				mapInfoOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_SCALE_BAR));
				mapInfoOpts.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_SPEED_IN_MAP));
				mapInfoOpts.setSelectedIndex(3, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_ALTITUDE_IN_MAP));
				mapInfoOpts.setSelectedIndex(4, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_IN_MAP));
				mapInfoOpts.setSelectedIndex(5, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_CLOCK_IN_MAP));
				metricUnits.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_METRIC));
				SingleTile.newPOIFont();
				WaypointsTile.useNewWptFont();

				// convert bits from backlight flag into selection states
				boolean[] sellight = new boolean[7];
				sellight[0] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ON);
				sellight[1] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED);
				sellight[2] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_KEEPALIVE);
				sellight[3] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_MIDP2);
				byte i = 4;
				//#if polish.api.nokia-ui
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIA);
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH);
				//#endif
				//#if polish.api.min-siemapi
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_SIEMENS);
				//#endif
		
				backlightOpts.setSelectedFlags(sellight);
				
				GpsMid.getInstance().show(menuDisplayOptions);
				state = STATE_DISPOPT;
				break;
			case MENU_ITEM_GPX_DEVICE: // GPX Receiver
				//Prepare Gpx receiver selection menu
				menuGpx = new Form("Gpx Receiver");
				menuGpx.addCommand(BACK_CMD);
				menuGpx.addCommand(OK_CMD);
				menuGpx.addCommand(FILE_MAP);
				menuGpx.addCommand(BT_MAP);
				//#if polish.api.osm-editing
				menuGpx.addCommand(OSM_URL);
				//#endif
		
				gpxUrl = new StringItem("Gpx Receiver Url: ", "<Please select in menu>");
				menuGpx.append(gpxUrl);
				menuGpx.setCommandListener(this);
				gpxUrl.setText(Configuration.getGpxUrl() == null ?
						"<Please select in menu>" : Configuration.getGpxUrl());
				GpsMid.getInstance().show(menuGpx);
				state = STATE_GPX;
				break;
			case MENU_ITEM_MAP_SRC: // Map Source
				initMapSource();
				mapSrc.set(1, "Filesystem: " + ( (Configuration.getMapUrl() == null) ?
						"<Please select map directory or other .jar/zip file first>" :
							Configuration.getMapUrl() ), null);
				mapSrc.setSelectedIndex(Configuration.usingBuiltinMap() ? 0 : 1, true);
				mapSrcOptions.setSelectedIndex(0,
						Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS));
				GpsMid.getInstance().show(menuSelectMapSource);
				state = STATE_MAP;
				break;
			case MENU_ITEM_DEBUG_OPT:
				initDebugSetupMenu();
				GpsMid.getInstance().show(menuDebug);
				state = STATE_DEBUG;
				break;
			case MENU_ITEM_ROUTING_OPT:
				GuiRoute guiRoute = new GuiRoute(this, true);
				guiRoute.show();
				break;
			case MENU_ITEM_SOUNDS_OPT:
				GuiSetupSound gs = new GuiSetupSound(this);
				gs.show();
				break;
			case MENU_ITEM_GUI_OPT:
				GuiSetupGui gsg = new GuiSetupGui(this, false);
				gsg.show();
				break;
			case MENU_ITEM_KEYS_OPT:
				/**
				 * Display the current Keyboard mappings for the
				 * Map screen
				 */
				GuiKeyShortcuts gks = new GuiKeyShortcuts(this);
				gks.show();
				break;
			//#if polish.api.osm-editing
			case MENU_ITEM_OSM_OPT:
				/**
				 * Display the current Keyboard mappings for the
				 * Map screen
				 */
				initOSMaccountOptions();
				GpsMid.getInstance().show(menuOsmAccountOptions);
				state = STATE_OSM_OPT;
				break;
			//#endif
			case MENU_ITEM_OPENCELLID_OPT:
				/**
				 * Opencellid Apikey
				 */
				initOpencellidOptions();
				GpsMid.getInstance().show(menuOpencellidOptions);
				state = STATE_OPENCELLID_OPT;
				break;
			//#if polish.api.fileconnection
			case MENU_ITEM_SAVE_CONFIG:
				state = STATE_SAVE_CONFIG;
				FsDiscover fsd = new FsDiscover(this, this, null, true, "", "Save configuration");
				fsd.show();
				break;
			case MENU_ITEM_LOAD_CONFIG:
				state = STATE_LOAD_CONFIG;
				fsd = new FsDiscover(this, this, null, false, "cfg", "Load configuration");
				fsd.show();
				break;
			//#endif
		}
	}
	
	
	// converts float f to string and
	// cuts off unnecessary trailing chars and digits
	private String getCleanFloatString(float f, int maxlen) {
		StringBuffer sb = new StringBuffer();
		// convert float to string
		sb.append(Float.toString(f));
		// limit to maximum length of TextField
		sb.setLength(maxlen);
		boolean hasDecimalPoint = sb.toString().indexOf(".") != -1;
		// cut unnecessary trailing chars and digits
		while((sb.length()>1) && ((sb.charAt(sb.length() - 1) == '.')||((sb.charAt(sb.length() - 1) == '0') && hasDecimalPoint))) {
			if (sb.charAt(sb.length() - 1) == '.') {
				hasDecimalPoint = false;
			}
			sb.setLength(sb.length()-1);
		}
		return sb.toString();
	}
	
	private void destroy() {
		//#if polish.api.btapi
		if (gps != null) {
			gps.destroy();
		}
		//#endif
	}

	public void clear() {
		menu.deleteAll();
	}

	public void completeInitialization(boolean isBTReady) {
		//menuBT.addCommand(STORE_BT_URL);
		menuBT.setTitle("Search Device");
	}

	/** Shows Setup menu of MIDlet on the screen. */
	public void show() {
		switch (state) {
			case STATE_ROOT:
				if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS)) {
					showIconMenu();
				} else {
					GpsMid.getInstance().show(menu);
				}
				break;
			case STATE_LP:
				GpsMid.getInstance().show(menuSelectLocProv);
				break;
			case STATE_MAP:
				GpsMid.getInstance().show(menuSelectMapSource);
				break;
			case STATE_GPX:
				GpsMid.getInstance().show(menuGpx);
				break;
			case STATE_BT_GPX:
				GpsMid.getInstance().show(menuBT);
				break;
			case STATE_DEBUG:
				GpsMid.getInstance().show(menuDebug);
				break;
			case STATE_BT_GPS:
				GpsMid.getInstance().show(menuBT);
				break;
			default:
				logger.error("Show called without a valid state");
		}
	}

	public void addDevice(String s) {
//		menuBT.append(s, null);
		
	}
	public void addDevice(String url, String name) {
//		menuBT.append("add " + name, null);
		urlList.addElement(url);
		friendlyName.addElement(name);
	}

	public void showState(String a) {
		menuBT.setTitle("Devices " + a);
	}

	public void fsDiscoverReady() {
		menuFS.addCommand(STORE_ROOTFS);
		menuFS.setTitle("Select Root");
	}

	public void addRootFs(String root) {
		menuFS.append(root, null);
	}

	public void btDiscoverReady() {
//		menuBT.deleteAll();
		menuBT.addCommand(STORE_BT_URL);
		for (int i = 0; i < friendlyName.size(); i++) {
			try {
				menuBT.append("" + i + " " + (String) friendlyName.elementAt(i), null);
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				menuBT.append(e.getMessage(), null);
			}
		}
		
	}
	
	public void selectionCanceled() {
		//#if polish.api.fileconnection
		switch (state) {
		case STATE_LOAD_CONFIG:
		case STATE_SAVE_CONFIG:
			state = STATE_ROOT;
			break;
		}
		//#endif
	}

	public void selectedFile(String url) {
		logger.info("Url selected: " + url);
		String url_trunc = url.substring(0, url.lastIndexOf('/') + 1);
		switch (state) {
		case STATE_LP:
			rawLogCG.setLabel(LOG_TO + url_trunc);
			rawLogDir = url_trunc;
			break;
		case STATE_GPX:
			gpxUrl.setText(url_trunc);
			break;
		//#if polish.api.fileconnection
		case STATE_MAP:
			mapSrc.set(1, "Filesystem: " + url, null);
			mapSrc.setSelectedIndex(1, true);
			//As the Filesystem chooser has called the show()
			//method of this class, it currently shows the root
			//menu, but we want't to continue to edit the MapSource
			//menue
			Display.getDisplay(parent).setCurrent(menuSelectMapSource);
			break;
		case STATE_DEBUG:
			debugLog.set(0, url_trunc, null);
			break;
		case STATE_SAVE_CONFIG:
			try {
				FileConnection con = (FileConnection)Connector.open(url_trunc + "GpsMid.cfg");
				if (!con.exists()) {
					con.create();
				}
				Configuration.serialise(con.openOutputStream());
				con.close();
			} catch (Exception e) {
				logger.exception("Could not save configuration", e);
			}
			state = STATE_ROOT;
			show();
			break;
		case STATE_LOAD_CONFIG:
			try {
				FileConnection con = (FileConnection)Connector.open(url);
				Configuration.deserialise(con.openInputStream());
				con.close();
			} catch (Exception e) {
				logger.exception("Could not load configuration", e);
			}
			state = STATE_ROOT;
			show();
			break;
		//#endif
		}
		
	}
	
	public void showIconMenu() {
		if (setupIconMenu == null) {
			setupIconMenu = new GuiDiscoverIconMenu(this, this);
		}
		setupIconMenu.show();
	}
	
	/** uncache the icon menu to reflect changes in the setup or save memory */
	public static void uncacheIconMenu() {
		//#mdebug trace
		if (setupIconMenu != null) {
			logger.trace("uncaching setupIconMenu");
		}
		//#enddebug
		setupIconMenu = null;
	}

	/** interface for IconMenuWithPages: recreate the icon menu from scratch and show it (introduced for reflecting size change of the Canvas) */
	public void recreateAndShowIconMenu() {
		uncacheIconMenu();
		showIconMenu();
	}

	/** interface for received actions from the IconMenu GUI */
	public void performIconAction(int actionId) {
		if (actionId == IconActionPerformer.BACK_ACTIONID) {
			uncacheIconMenu();
			System.gc();
			commandAction(EXIT_CMD, (Displayable) null);
		} else {
			showSetupDialog(actionId);
		}
	}
}
