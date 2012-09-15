/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008, 2009 Kai Krueger apmonkey at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.ui;

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
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;


import net.sharenav.midlet.iconmenu.IconActionPerformer;
import net.sharenav.midlet.ui.SelectionListener;
import net.sharenav.util.Logger;
import net.sharenav.sharenav.data.ConfigExportImport;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Gpx;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.graphics.ProjFactory;
import net.sharenav.sharenav.graphics.Projection;
import net.sharenav.sharenav.routing.RouteSyntax;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.sharenav.tile.WaypointsTile;
import net.sharenav.sharenav.ui.ShareNavMenu;
import net.sharenav.sharenav.ui.GuiCamera;

import net.sharenav.gps.location.GetCompass;
import net.sharenav.gps.location.SECellId;
//#if polish.android
import net.sharenav.midlet.util.ImageTools;
//#endif

public class GuiDiscover implements CommandListener, ItemCommandListener, 
		ShareNavDisplayable, SelectionListener, IconActionPerformer {

	/** A menu list instance */
	private static final String[] elements = {
		//#if polish.android
		Locale.get("generic.Back")/*Back*/, 
		//#endif
		Locale.get("guidiscover.LocationReceiver")/*Location Receiver*/, 
		Locale.get("guidiscover.RecordingRules")/*Recording Rules*/,
		/*		"Languages & Units", */
		Locale.get("guidiscover.DisplayOptions")/*Display options*/, 
		Locale.get("guidiscover.SoundsAlerts")/*Sounds "Sounds & Alerts"*/,
		Locale.get("guidiscover.RoutingOptions")/*Routing options*/,
		Locale.get("guidiscover.GpxReceiver")/*Gpx Receiver*/, 
		Locale.get("guidiscover.GUIOptions")/*GUI Options*/,
		Locale.get("guidiscovericonmenu.Maps")/*Maps*/, 
		Locale.get("guidiscover.DebugOptions")/*Debug options*/,
		Locale.get("guidiscover.KeyShortcuts")/*Key shortcuts*/,
		Locale.get("guidiscover.Opencellid")/*Opencellid*/,
		//#if polish.api.osm-editing
		Locale.get("guidiscover.OSMAccount")/*OSM account*/,
		//#endif
		Locale.get("guidiscover.OnlineSetup")/*Online setup*/,
		//FIXME rename as generic string
		//#if polish.api.mmapi
		Locale.get("trace.Camera")/*Camera*/,
		//#endif
		//#if polish.api.fileconnection
		Locale.get("guidiscover.ExportConfig")/*Export config*/, 
		Locale.get("guidiscover.ImportConfig")/*Import config*/
		//#endif
		};

	private static final String LABEL_SELECT_LOGDIR_FIRST = Locale.get("guidiscover.PleaseSelectLogDir")/*Please select first the log directory for:*/;
	private static final String LOG_TO = Locale.get("guidiscover.LogTo")/*Log To: */;

	/**
	 * The following MENU_ITEM constants have to be in sync
	 * with the position in the elements array of the main menu.
	 */
	int i = 0;
	//#if polish.android
	protected static final int MENU_ITEM_BACK = 0;
	protected static final int MENU_ITEM_LOCATION = 1;
	protected static final int MENU_ITEM_GPX_FILTER = 2;
	protected static final int MENU_ITEM_DISP_OPT = 3;
	protected static final int MENU_ITEM_SOUNDS_OPT = 4;
	protected static final int MENU_ITEM_ROUTING_OPT = 5;
	protected static final int MENU_ITEM_GPX_DEVICE = 6;
	protected static final int MENU_ITEM_GUI_OPT = 7;
	protected static final int MENU_ITEM_MAP_SRC = 8;
	protected static final int MENU_ITEM_DEBUG_OPT = 9;
	protected static final int MENU_ITEM_KEYS_OPT = 10;
	protected static final int MENU_ITEM_OPENCELLID_OPT = 11;
	//#if polish.api.osm-editing
	protected static final int MENU_ITEM_OSM_OPT = 12;
	protected static final int MENU_ITEM_ONLINE_OPT = 13;
	//#if polish.api.mmapi
	protected static final int MENU_ITEM_CAMERA_OPT = 14;
	protected static final int MENU_ITEM_EXPORT_CONFIG = 15;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 16;
	protected static final int MENU_ITEM_BLUETOOTH = 17;
	//#else
	protected static final int MENU_ITEM_EXPORT_CONFIG = 14;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 15;
	protected static final int MENU_ITEM_BLUETOOTH = 16;
	//#endif
	//#else
	protected static final int MENU_ITEM_ONLINE_OPT = 12;
	//#if polish.api.mmapi
	protected static final int MENU_ITEM_CAMERA_OPT = 13;
	protected static final int MENU_ITEM_EXPORT_CONFIG = 14;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 15;
	protected static final int MENU_ITEM_BLUETOOTH = 16;
	//#else
	protected static final int MENU_ITEM_EXPORT_CONFIG = 13;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 14;
	protected static final int MENU_ITEM_BLUETOOTH = 15;
	//#endif
	//#endif
	//#else
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
	protected static final int MENU_ITEM_ONLINE_OPT = 12;
	//#if polish.api.mmapi
	protected static final int MENU_ITEM_CAMERA_OPT = 13;
	protected static final int MENU_ITEM_EXPORT_CONFIG = 14;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 15;
	protected static final int MENU_ITEM_BLUETOOTH = 16;
	//#else
	protected static final int MENU_ITEM_EXPORT_CONFIG = 13;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 14;
	protected static final int MENU_ITEM_BLUETOOTH = 15;
	//#endif
	//#else
	protected static final int MENU_ITEM_ONLINE_OPT = 11;
	//#if polish.api.mmapi
	protected static final int MENU_ITEM_CAMERA_OPT = 12;
	protected static final int MENU_ITEM_EXPORT_CONFIG = 13;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 14;
	protected static final int MENU_ITEM_BLUETOOTH = 15;
	//#else
	protected static final int MENU_ITEM_EXPORT_CONFIG = 12;
	protected static final int MENU_ITEM_IMPORT_CONFIG = 13;
	protected static final int MENU_ITEM_BLUETOOTH = 14;
	//#endif
	//#endif
	//#endif

	private int numLangDifference = 0;

	private String savedLang = null;
	private String langToAdd = null;

	private static final String[] empty = {};

	/** Soft button for exiting to RootMenu. */
	private final Command EXIT_CMD = new Command(Locale.get("generic.Back")/*Back*/, ShareNavMenu.BACK, 2);

	private final Command BACK_CMD = new Command(Locale.get("generic.Cancel")/*Cancel*/, ShareNavMenu.BACK, 2);

	/** Soft button for discovering BT. */
	private final Command OK_CMD = new Command(Locale.get("generic.OK")/*Ok*/, ShareNavMenu.OK, 1);

	private final Command STORE_BT_URL = new Command(Locale.get("guidiscover.Select")/*Select*/, Command.OK, 2);

	private final Command STORE_ROOTFS = new Command(Locale.get("guidiscover.Select")/*Select*/, Command.OK, 2);
	
	private final Command FILE_MAP = new Command(Locale.get("guidiscover.SelectMapSource")/*Select Map Source*/, Command.ITEM, 2);
	private final Command SELECT_DIR = new Command(Locale.get("guidiscover.SelectDirectory")/*Select Directory*/, Command.ITEM, 2);
	private final Command BT_MAP	= new Command(Locale.get("guidiscover.SelectBtDev")/*Select bluetooth device*/, Command.ITEM, 2);
	//#if polish.api.osm-editing
	private final Command OSM_URL = new Command(Locale.get("guidiscover.UploadOSM")/*Upload to OSM*/, Command.ITEM, 2);
	//#endif
	//#if polish.api.online
	private final Command OPENCELLID_APIKEY = new Command(Locale.get("guidiscover.OpencellidApikey")/*Opencellid apikey*/, Command.ITEM, 1);
	//#endif
	private final Command GPS_DISCOVER = new Command(Locale.get("guidiscover.DiscoverGPS")/*Discover GPS*/, Command.ITEM, 1);
	
	private final Command MANUAL_URL_CMD = new Command(Locale.get("guidiscover.EnterURL")/*Enter URL*/, Command.ITEM, 1);
	
	/** Soft button for cache reset. */
	private final Command CELLID_CACHE_RESET_CMD = new Command(Locale.get("guidiscover.ResetCellidCache")/*Reset cellid cache*/,
															Command.OK, 2);

	/** A menu list instance */
	private final List menu = new List(Locale.get("guidiscover.Setup")/*Setup*/, 
			Choice.IMPLICIT, elements, null);

	private List menuBT;
	private List menuNMEAOptsList;

	private final List menuFileSel = new List(Locale.get("guidiscover.Devices")/*Devices*/, 
			Choice.IMPLICIT, empty, null);

	private Form menuSelectLocProv;
	
	private Form menuSelectMapSource;
	
	private Form menuDisplayOptions;
	
	private Form menuGpx;
	
	private Form menuDebug;

	private Form menuRoutingOptions;
	
	private Form menuURLEnter;
	
	//#if polish.api.osm-editing
	private Form menuOsmAccountOptions;
	//#endif
	private Form menuOnlineOptions;

	private Form menuNMEAOptions;
	
	private Form menuOpencellidOptions;

	private final ShareNav			parent;

	private DiscoverGps				gps;

	private int						state;

	private final static int		STATE_ROOT		= 0;
	private final static int		STATE_BT_GPS	= 2;
	private final static int		STATE_LOC_PROV	= 3;
	private final static int		STATE_RBT		= 4;
	private final static int		STATE_GPX		= 5;
	private final static int		STATE_MAP		= 6;
	private final static int		STATE_DISP_OPT	= 7;
	private final static int 		STATE_BT_GPX	= 8;
	private final static int		STATE_DEBUG		= 9;
	//#if polish.api.osm-editing
	private final static int		STATE_OSM_OPT = 10;
	//#endif
	private final static int		STATE_OPENCELLID_OPT = 11;
	private final static int		STATE_IMPORT_CONFIG = 12;
	private final static int		STATE_EXPORT_CONFIG = 13;
	private final static int		STATE_URL_ENTER_GPS = 14;
	private final static int		STATE_URL_ENTER_GPX = 15;
	private final static int		STATE_ONLINE_OPT = 16;
	private final static int		STATE_BT_OPT = 17;
	
	private Vector urlList;
	private Vector friendlyName;
	private ChoiceGroup locProv;
	private TextField  tfURL;
	private TextField  addLang;
	//private TextField  naviLangURL;
	//private TextField  onlineLangURL;
	//#if polish.api.osm-editing
	private TextField  tfOsmUserName;
	private TextField  tfOsmPassword;
	private TextField  tfOsmUrl;
	//#endif
	private TextField  tfOpencellidApikey;
	private ChoiceGroup rawLogCG;
	private ChoiceGroup mapSrc;
	private ChoiceGroup mapSrcOptions;
	private TextField  tfTMSUrl;
	private TextField  tfFCPath;
	private ChoiceGroup perfTuneOptions;
	private ChoiceGroup tileMapOptions;
	private ChoiceGroup rotationGroup;
	private ChoiceGroup nightModeGroup;
	private ChoiceGroup uiLangGroup;
	private ChoiceGroup naviLangGroup;
	private ChoiceGroup onlineLangGroup;
	private ChoiceGroup onlineOptionGroup;
	private ChoiceGroup internetAccessGroup;
	private ChoiceGroup directionOpts;
	private ChoiceGroup directionDevOpts;
	private ChoiceGroup renderOpts;
	private ChoiceGroup visualOpts;
	private TextField	tfDestLineWidth;
	private TextField	tfTimeDiff;
	private ChoiceGroup metricUnits;
	private ChoiceGroup distanceViews;
	private TextField	tfAutoRecenterToGpsSecs;
	private ChoiceGroup backlightOpts;
	private ChoiceGroup sizeOpts;
	private ChoiceGroup mapInfoOpts;
	private ChoiceGroup clockOpts;
	private ChoiceGroup debugLog;
	private ChoiceGroup debugSeverity;
	private ChoiceGroup debugOther;
	private StringItem  gpxUrl;
	private StringItem  gpsUrl;
	private ImageItem mapLocationImage;
	private ChoiceGroup autoConnect;
	private ChoiceGroup cellIDStartup;
	private ChoiceGroup btKeepAlive;
	private ChoiceGroup btAutoRecon;
	private TextField tfAltitudeCorrection;
	  
	private String gpsUrlStr;
	private String rawLogDir;

	private ChoiceGroup cellidOptsGroup;

	private final static Logger logger = Logger.getInstance(GuiDiscover.class, Logger.DEBUG);

	private static GuiDiscoverIconMenu setupIconMenu = null;
	
	public GuiDiscover(ShareNav parent) {
		this.parent = parent;

		state = STATE_ROOT;

		//Prepare Main Menu
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		menu.setSelectCommand(OK_CMD);

		//Prepare file select menu
		menuFileSel.addCommand(BACK_CMD);
		menuFileSel.setCommandListener(this);

		if (!Trace.getInstance().isShowingSplitIconMenu()) {
			show();
		}
	}

	private void initBluetoothSelect() {
		//Prepare Bluetooth selection menu
		logger.info("Starting bluetooth setup menu");
		menuBT = new List(Locale.get("guidiscover.Devices")/*Devices*/, Choice.IMPLICIT, empty, null);
		menuBT.addCommand(OK_CMD);
		menuBT.addCommand(BACK_CMD);
		menuBT.addCommand(MANUAL_URL_CMD);
		menuBT.setSelectCommand(OK_CMD);
		menuBT.setCommandListener(this);
		menuBT.setTitle("Search Service");
	}

	private void initDebugSetupMenu() {
		//Prepare Debug selection menu
		logger.info("Starting Debug setup menu");
		menuDebug = new Form(Locale.get("guidiscover.DebugOptions")/*Debug options*/);
		String [] loggings = new String[1];
		menuDebug.addCommand(BACK_CMD);
		menuDebug.addCommand(OK_CMD);
		menuDebug.addCommand(SELECT_DIR);
		menuDebug.setCommandListener(this);
		boolean [] selDebug = new boolean[1];
		selDebug[0] = Configuration.getDebugRawLoggerEnable();
		loggings = new String[1];
		loggings[0] = Configuration.getDebugRawLoggerUrl();
		if (loggings[0] == null) {
			loggings[0] = Locale.get("guidiscover.PleaseSelectDir")/*Please select directory*/;
		}
		debugLog = new ChoiceGroup(Locale.get("guidiscover.DebugTo")/*Debug event logging to:*/, ChoiceGroup.MULTIPLE, loggings, null);
		debugLog.setSelectedFlags(selDebug);
		//#style formItem
		menuDebug.append(debugLog);

		loggings = new String[3];
		selDebug = new boolean[3];
		selDebug[0] = Configuration.getDebugSeverityInfo();
		selDebug[1] = Configuration.getDebugSeverityDebug();
		selDebug[2] = Configuration.getDebugSeverityTrace();
		loggings[0] = Locale.get("guidiscover.Info")/*Info*/;
		loggings[1] = Locale.get("guidiscover.Debug")/*Debug*/;
		loggings[2] = Locale.get("guidiscover.Trace")/*Trace*/;
		debugSeverity = new ChoiceGroup(Locale.get("guidiscover.LogSeverity")/*Log severity:*/, ChoiceGroup.MULTIPLE, loggings, null);
		debugSeverity.setSelectedFlags(selDebug);
		//#style formItem
		menuDebug.append(debugSeverity);

		loggings = new String[6];
		loggings[0] = Locale.get("guidiscover.ShowRouteConnections")/*Show route connections*/;
		loggings[1] = Locale.get("guidiscover.ShowRouteCalcTraces")/*Show route calculation traces*/;
		loggings[2] = Locale.get("guidiscover.ShowTurnRestrictions")/*Show turn restrictions*/;
		loggings[3] = Locale.get("guidiscover.ShowInconsistentBearings")/*Show inconsistent bearings*/;
		loggings[4] = Locale.get("guidiscover.ShowTileRequestsDropped")/*Show tile requests dropped */;
		loggings[5] = Locale.get("guidiscover.nmeaerrors")/*Show unknown NMEA errors */;
		debugOther = new ChoiceGroup(Locale.get("guidiscover.Other")/*Other:*/, ChoiceGroup.MULTIPLE, loggings, null);
		debugOther.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_CONNECTIONS));
		debugOther.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTECONNECTION_TRACES));
		debugOther.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS));
		debugOther.setSelectedIndex(3, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUTE_BEARINGS));
		debugOther.setSelectedIndex(4, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_TILE_REQUESTS_DROPPED));
		debugOther.setSelectedIndex(5, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_NMEA_ERRORS));
		//#style formItem
		menuDebug.append(debugOther);
	}

	private void initNMEASetupMenu() {
		//Prepare NMEA/Bluetooth setup menu
		logger.info("Starting NMEA setup menu");
		menuNMEAOptions = new Form(Locale.get("guidiscover.NMEAOptions")/*NMEA/BT options*/);
		menuNMEAOptions.addCommand(BACK_CMD);
		menuNMEAOptions.addCommand(OK_CMD);
		menuNMEAOptions.addCommand(GPS_DISCOVER);
		gpsUrl = new StringItem(Locale.get("guidiscover.GPS")/*GPS: */, null);
		gpsUrl.setDefaultCommand(GPS_DISCOVER);
		gpsUrl.setItemCommandListener(this);
		//#style formItem
		menuNMEAOptions.append(gpsUrl);
		menuNMEAOptions.setCommandListener(this);

		String [] btka = new String[1];
		btka[0] = Locale.get("guidiscover.Sendkeepalives")/*Send keep alives*/;
		btKeepAlive = new ChoiceGroup(Locale.get("guidiscover.BTkeepalive")/*BT keep alive*/, ChoiceGroup.MULTIPLE, btka, null);

		String [] btar = new String[1];
		btar[0] = Locale.get("guidiscover.AutoreconnectGPS")/*Auto reconnect GPS*/;
		btAutoRecon = new ChoiceGroup(Locale.get("guidiscover.BTreconnect")/*BT reconnect*/, ChoiceGroup.MULTIPLE, btar, null);
		//#style formItem
		menuNMEAOptions.append(btKeepAlive);
		//#style formItem
		menuNMEAOptions.append(btAutoRecon);
	}

	private void initLocationSetupMenu() {
		//Prepare Location Provider setup menu
		logger.info("Starting Locationreceiver setup menu");

		menuSelectLocProv = new Form(Locale.get("guidiscover.LocationReceiver")/*Location Receiver*/);

		menuSelectLocProv.addCommand(BACK_CMD);
		menuSelectLocProv.addCommand(OK_CMD);
		menuSelectLocProv.addCommand(MANUAL_URL_CMD);
		locProv = new ChoiceGroup(Locale.get("guidiscover.inputfrom")/*input from:*/, Choice.EXCLUSIVE, Configuration.LOCATIONPROVIDER, new Image[Configuration.LOCATIONPROVIDER.length]);
		//#style formItem
		menuSelectLocProv.append(locProv);
		//#style formItem
		menuSelectLocProv.addCommand(SELECT_DIR);

		final String[] logCategories = {Locale.get("guidiscover.CellIDs")/*Cell-IDs for OpenCellID.org*/, Locale.get("guidiscover.RawGpsData")/*Raw Gps Data*/ };
		rawLogCG = new ChoiceGroup(LABEL_SELECT_LOGDIR_FIRST, ChoiceGroup.MULTIPLE, logCategories, new Image[2]);
		String [] aconn = new String[1];
		aconn[0] = Locale.get("guidiscover.StartGPSAtStartup")/*Start GPS at startup*/;
		autoConnect = new ChoiceGroup(Locale.get("guidiscover.GPSstart")/*GPS start*/, ChoiceGroup.MULTIPLE, aconn, null);

		String [] cellidStart = new String[1];
		cellidStart[0] = Locale.get("guidiscover.cellIDAtStartup")/*Do a single lookup*/;
		cellIDStartup = new ChoiceGroup(Locale.get("guidiscover.cellIDStart")/*CellID lookup at startup*/, ChoiceGroup.MULTIPLE, cellidStart, null);

		//#style formItem
		menuSelectLocProv.append(autoConnect);
		//#style formItem
		menuSelectLocProv.append(cellIDStartup);
		//#style formItem
		menuSelectLocProv.append(rawLogCG);
		tfAltitudeCorrection = new TextField(Locale.get("guidiscover.AltitudeCorrection")/*Altitude correction*/, Integer.toString(Configuration.getAltitudeCorrection()), 3, TextField.ANY);
		//#style formItem
		menuSelectLocProv.append(tfAltitudeCorrection);
		menuSelectLocProv.setCommandListener(this);
	}

	private void initMapSource() {
		logger.info("Starting map source setup menu");
		menuSelectMapSource = new Form(Locale.get("guidiscover.SelectMapSource")/*Select Map Source*/);
		menuSelectMapSource.addCommand(BACK_CMD);
		menuSelectMapSource.addCommand(OK_CMD);
		menuSelectMapSource.addCommand(FILE_MAP);
		String [] sources = new String[2];
		sources[0] = Locale.get("guidiscover.Built-inMap")/*Built-in map*/;
		sources[1] = Locale.get("guidiscover.Filesystem")/*Filesystem: */;
		mapSrc = new ChoiceGroup(Locale.get("guidiscover.MapSource")/*Map source:*/, Choice.EXCLUSIVE, sources, null);

		//#if polish.android
		try {
			// FIXME would be better to have a dedicated icon
			Image image = Image.createImage("/" + Configuration.getIconPrefix() + "is_save" + ".png");

			float scale = 4 * image.getWidth() / Trace.getInstance().getWidth();
			if (scale < 1.0f) {
				scale = 1;
			}
			String name = Locale.get("guidiscover.SelectMapSource")/*Select Map Source*/;

			mapLocationImage = new ImageItem(name, 
							    ImageTools.scaleImage(image, (int) (image.getWidth() / scale), (int) (image.getHeight() / scale)),
							    ImageItem.LAYOUT_RIGHT, name);
		} catch (IOException ioe) {
		}
		mapLocationImage.addCommand(FILE_MAP);
		mapLocationImage.setDefaultCommand(FILE_MAP);
		mapLocationImage.setItemCommandListener(this);
		menuSelectMapSource.append(mapLocationImage);
		//#endif

		String [] mapOptions = new String[3];
		mapOptions[0]  = Locale.get("guidiscover.mapcredits")/*Show map credits*/;
		mapOptions[1] = Locale.get("guidiscover.PreferBuiltInPNGs")/*Prefer built-in POI PNGs (faster startup e.g. on some Nokias)*/;
		mapOptions[2] = Locale.get("guidiscover.PreferBuiltInSounds")/*Prefer built-in sounds*/;
		mapSrcOptions = new ChoiceGroup(Locale.get("guidiscover.Options")/*Options*/, ChoiceGroup.MULTIPLE, mapOptions, null);
		
		String [] performanceOptions;
		String [] TMSOptions;
		int i = 3;
		//#if polish.android
		i++;
		//#endif
		performanceOptions = new String[i];
		TMSOptions = new String[4];
		
		i = 0;
		//#if polish.android
		performanceOptions[i++] = Locale.get("guidiscover.PreferOutline")/*Prefer outline area format*/;
		//#endif
		performanceOptions[i++] = Locale.get("guidiscover.UseBufferedInputStream")/*use BufferedImputStream*/;
		performanceOptions[i++] = Locale.get("guidiscover.LoadNamesLast")/*load names last*/;
		performanceOptions[i++] = Locale.get("guidiscover.simplify")/*Simplify map when busy*/;
		perfTuneOptions = new ChoiceGroup(Locale.get("guidiscover.PerfTuneOptions")/*Performance tuning options:*/, ChoiceGroup.MULTIPLE, performanceOptions, null);
	
		i = 0;
		TMSOptions[i++] = Locale.get("guidiscover.TMSBackground")/*Use TMS map as background*/;
		TMSOptions[i++] = Locale.get("guidiscover.BackgroundDisableAreas")/*Disable areas when background map is shown*/;
		TMSOptions[i++] = Locale.get("guidiscover.BackgroundDisableBuildings")/*Disable buildings when background map is shown*/;
		TMSOptions[i++] = Locale.get("guidiscover.TMSSplitScreen")/*Show raster map in split-screen mode*/;
		tileMapOptions = new ChoiceGroup(Locale.get("guidiscover.TMSOptions")/*TMS options:*/, ChoiceGroup.MULTIPLE, TMSOptions, null);

		//#style formItem
		menuSelectMapSource.append(mapSrc);
		//#style formItem
		menuSelectMapSource.append(mapSrcOptions);
		//#style formItem
		menuSelectMapSource.append(perfTuneOptions);
		//#style formItem
		menuSelectMapSource.append(tileMapOptions);
		tfTMSUrl = new TextField(Locale.get("guidiscover.TMSURL")/*Raster map URL:*/, Configuration.getTMSUrl(), 512, TextField.URL);
		tfFCPath = new TextField(Locale.get("guidiscover.TMSFCPath")/*Raster map file cache path:*/, Configuration.getTMSFilecachePath(), 512, TextField.URL);

		menuSelectMapSource.append(tfTMSUrl);
		menuSelectMapSource.append(tfFCPath);
		menuSelectMapSource.setCommandListener(this);
	}

	private void initDisplay() {
		logger.info("Starting display setup menu");

		menuDisplayOptions = new Form(Locale.get("guidiscover.DisplayOptions")/*Display Options*/);

		menuDisplayOptions.addCommand(BACK_CMD);
		menuDisplayOptions.addCommand(OK_CMD);

		// device default (if it exists) is the first; check if device supports getting locale
		if (Configuration.getLocaleLang() == null && Legend.uiLang[0].equalsIgnoreCase("devdefault")) {
			// device doesn't support giving local language which is first in Legend, omit the choice for device default
			numLangDifference = -1;
		}
		boolean addSelectedUiLang = false;
		String selectedUiLang = Configuration.getUiLang();
		savedLang = selectedUiLang;
		if (!selectedUiLang.equals("") && !selectedUiLang.equals("devdefault")) {
			addSelectedUiLang = true;
		}
		for (int i = 0; i < Legend.numUiLang; i++) {
			if (addSelectedUiLang && Legend.uiLang[i].equalsIgnoreCase(selectedUiLang)) {
				addSelectedUiLang = false;
			}
		}
		int addedLang = 0;
		// add the lang selected in config
		if (addSelectedUiLang) {
			addedLang += 1;
		}
		if (Legend.numUiLang + numLangDifference > 1) {
			String [] uiLang = new String[Legend.numUiLang + numLangDifference + addedLang];

			for (int i = 0; i < Legend.numUiLang; i++) {
				if (i + numLangDifference >= 0) {
					uiLang[i + numLangDifference] = Legend.uiLangName[i];
				}
				if (Configuration.getLocaleLang() != null && Legend.uiLang[i].equalsIgnoreCase("devdefault")) {
					uiLang[i] = Locale.get("guidiscover.devicedefault")/*Device default*/ +
						" (" + Configuration.getLocaleLang() + ")";
				}
			}
			if (addSelectedUiLang) {
				uiLang[Legend.numUiLang - 1 + numLangDifference + addedLang] = selectedUiLang;
			}
			uiLangGroup = new ChoiceGroup(Locale.get("guidiscover.Language")/*Language*/, Choice.EXCLUSIVE, uiLang, null);
			//#style formItem
			menuDisplayOptions.append(uiLangGroup);
		}
		addLang = new TextField(Locale.get("guidiscover.addLang")/*Add language with 2-letter code*/,
					  Configuration.getUiLang(), 256, TextField.ANY);
		//#style formItem
		menuDisplayOptions.append(addLang);
//#if 0
		/* move these to another menu, advanced or i18n */
		if (Legend.numNaviLang  + numLangDifference > 1) {
			String [] naviLang = new String[Legend.numNaviLang + numLangDifference];
			for (int i = 0; i < Legend.numNaviLang; i++) {
				if (i + numLangDifference >= 0) {
					naviLang[i + numLangDifference] = Legend.naviLangName[i];
				}
				if (Configuration.getLocaleLang() != null && Legend.naviLang[i].equalsIgnoreCase("devdefault")) {
					naviLang[i] = Locale.get("guidiscover.devicedefault")/*Device default*/ +
						" (" + System.getProperty("microedition.locale").substring(0, 2) + ")";
				}
			}
			naviLangGroup = new ChoiceGroup(Locale.get("guidiscover.SoundNavilanguage")/*Sound/Navi language*/, Choice.EXCLUSIVE, naviLang, null);
			//#style formItem
			menuDisplayOptions.append(naviLangGroup);
		}
		// FIXME add dialogue for wikipedia & street name language switch,
		// maybe make a submenu or a separate language menu
//#endif
		String [] nightMode = new String[3];
		nightMode[0] = Locale.get("guidiscover.DayMode")/*Day Mode*/;
		nightMode[1] = Locale.get("guidiscover.NightMode")/*Night Mode*/;
		nightMode[2] = Locale.get("guidiscover.AutoDayNight")/*Autoswitch*/;
		//#style formItem
		nightModeGroup = new ChoiceGroup(Locale.get("guidiscover.Colors")/*Colors*/, Choice.EXCLUSIVE, nightMode, null);
		//#style formItem
		menuDisplayOptions.append(nightModeGroup);

		// FIXME rename string to generic
		//#style formItem
		rotationGroup = new ChoiceGroup(Locale.get("guidiscover.MapProjection")/*Map Projection*/, Choice.EXCLUSIVE, Configuration.projectionsString, null);
		//#style formItem
		menuDisplayOptions.append(rotationGroup);

		String [] direction = new String[3];
		direction[0] = Locale.get("guidiscover.movement")/*by movement*/;
		direction[1] = Locale.get("guidiscover.compass")/*by compass*/;
		direction[2] = Locale.get("guidiscover.autocompass")/*autoswitch*/;
		directionOpts = new ChoiceGroup(Locale.get("guidiscover.DirectionOptions")/*Rotate map*/, Choice.EXCLUSIVE, direction, null);
		//#style formItem
		menuDisplayOptions.append(directionOpts);

		String [] dirOpts = new String[2];
		dirOpts[0] = Locale.get("guidiscover.autocalibrate")/*Auto-calibrate dig.compass deviation by movement*/;
		dirOpts[1] = Locale.get("guidiscover.alwaysrotatebycompass")/*Always rotate map by compass*/;
		directionDevOpts = new ChoiceGroup(Locale.get("guidiscover.CompassOptions")/*Compass Options:*/, Choice.MULTIPLE, dirOpts, null);
		//#style formItem
		menuDisplayOptions.append(directionDevOpts);

		String [] renders = new String[2];
		renders[0] = Locale.get("guidiscover.aslines")/*as lines*/;
		renders[1] = Locale.get("guidiscover.asstreets")/*as streets*/;
		renderOpts = new ChoiceGroup(Locale.get("guidiscover.RenderingOptions")/*Rendering Options:*/, Choice.EXCLUSIVE, renders, null);
		//#style formItem
		menuDisplayOptions.append(renderOpts);
		
		String [] visuals = new String[2];
		visuals[0] = Locale.get("guidiscover.roadborders")/*road borders*/;
		visuals[1] = Locale.get("guidiscover.roundroadends")/*round road ends*/;
		visualOpts = new ChoiceGroup(Locale.get("guidiscover.VisualOptions")/*Visual Options:*/, Choice.MULTIPLE, visuals, null);
		//#style formItem
		menuDisplayOptions.append(visualOpts);
		
		tfDestLineWidth = new TextField(Locale.get("guidiscover.DestLineWidth")/*width of dest line*/, Integer.toString(Configuration.getDestLineWidth()), 1, TextField.DECIMAL);
		//#style formItem
		menuDisplayOptions.append(tfDestLineWidth);
				
		String [] metricUnit = new String[2];
		metricUnit[0] = Locale.get("guidiscover.metricunits")/*metric units*/;
		metricUnit[1] = Locale.get("guidiscover.englishunits")/*English units*/;
		metricUnits = new ChoiceGroup(Locale.get("guidiscover.Units")/*Units*/, Choice.EXCLUSIVE, metricUnit, null);
		//#style formItem
		menuDisplayOptions.append(metricUnits);

		String [] distanceView = new String[2];
		distanceView[0] = Locale.get("guidiscover.onlymeters")/*only meters*/;
		distanceView[1] = Locale.get("guidiscover.kmorm")/*km or m*/;
		distanceViews = new ChoiceGroup(Locale.get("guidiscover.Distances")/*Distances*/, Choice.EXCLUSIVE, distanceView, null);
		//#style formItem
		menuDisplayOptions.append(distanceViews);

		tfAutoRecenterToGpsSecs = new TextField(Locale.get("guidiscover.Autorecentertimeout")/*Auto-recenter to GPS after no user action for these seconds*/,
				Integer.toString(Configuration.getAutoRecenterToGpsMilliSecs() / 1000),
				2, TextField.DECIMAL);
		//#style formItem
		menuDisplayOptions.append(tfAutoRecenterToGpsSecs);

		String [] backlights;
		byte i = 4;
		//#if polish.api.nokia-ui
		i += 2;
		//#endif
		//#if polish.api.min-siemapi
		i++;
		//#endif
		//#if polish.api.min-samsapi
		i++;
		//#endif
		//#if polish.android
		i++;
		i++;
		//#endif
		backlights = new String[i];

		backlights[0] = Locale.get("guidiscover.Keepbacklighton")/*Keep backlight on*/;
		backlights[1] = Locale.get("guidiscover.onlywhileGPS")/*only while GPS started*/;
		backlights[2] = Locale.get("guidiscover.onlyaskeepalive")/*only as keep-alive*/;
		backlights[3] = Locale.get("guidiscover.withMIDP2")/*with MIDP2.0*/;
		i = 4;
		//#if polish.api.nokia-ui
		backlights[i++] = Locale.get("guidiscover.withNokiaAPI")/*with Nokia API*/;
		backlights[i++] = Locale.get("guidiscover.withNokiaFlashlight")/*with Nokia Flashlight*/;
		//#endif
		//#if polish.api.min-siemapi
		backlights[i++] = Locale.get("guidiscover.withSiemensAPI")/*with Siemens API*/;
		//#endif
		//#if polish.api.min-samsapi
		backlights[i++] = Locale.get("guidiscover.withSamsungAPI")/*with Samsung API*/;
		//#endif
		//#if polish.android
		backlights[i++] = Locale.get("guidiscover.withAndroidWakelock")/*with Android wakelock*/;
		backlights[i++] = Locale.get("guidiscover.withAndroidWindowManager")/*with Android WindowManager*/;
		//#endif

		backlightOpts = new ChoiceGroup(Locale.get("guidiscover.BacklightOptions")/*Backlight Options:*/,
				Choice.MULTIPLE, backlights, null);
		//#style formItem
		menuDisplayOptions.append(backlightOpts);

		String [] sizes = new String[3];
		sizes[0] = Locale.get("guidiscover.largerPOIlabels")/*larger POI labels*/;
		sizes[1] = Locale.get("guidiscover.largerwaypointlabels")/*larger waypoint labels*/;
		sizes[2] = Locale.get("guidiscover.largefont")/*larger font*/;
		sizeOpts = new ChoiceGroup(Locale.get("guidiscover.SizeOptions")/*Size Options:*/, Choice.MULTIPLE, sizes, null);
		//#style formItem
		menuDisplayOptions.append(sizeOpts);

		String [] mapInfos = new String[9];
		mapInfos[0] = Locale.get("guidiscover.Pointofcompass")/*Point of compass in rotated map*/;
		mapInfos[1] = Locale.get("guidiscover.Scalebar")/*Scale bar*/;
		mapInfos[2] = Locale.get("guidiscover.Speed")/*Speed when driving*/;
		mapInfos[3] = Locale.get("guidiscover.Altitude")/*Altitude from GPS*/;
		mapInfos[4] = Locale.get("guidiscover.Airdistance")/*Air distance to dest. when not routing*/;
		mapInfos[5] = Locale.get("guidiscover.AirdistanceWhenRouting")/*Air distance to dest. when routing*/;
		mapInfos[6] = Locale.get("guidiscover.Clock")/*Clock with current time*/;
		mapInfos[7] = Locale.get("guidiscover.Accuracy")/*Accuracy with GPS status*/;
		mapInfos[8] = Locale.get("guiroute.TravelBy")/*Travel by:*/;
		mapInfoOpts = new ChoiceGroup(Locale.get("guidiscover.Infos")/*Infos in Map Screen:*/,
				Choice.MULTIPLE, mapInfos, null);
		//#style formItem
		menuDisplayOptions.append(mapInfoOpts);

		String [] clockSettings = new String[2];
		clockSettings[0] = Locale.get("guidiscover.UseGpsTime")/*Use GPS time on display*/;
		clockSettings[1] = Locale.get("guidiscover.GpsFallback")/*Fallback to device clock when GPS unavailable*/;
		clockOpts = new ChoiceGroup(Locale.get("guidiscover.Clock")/*Clock options:*/,
				Choice.MULTIPLE, clockSettings, null);
		//#style formItem
		menuDisplayOptions.append(clockOpts);
				
		tfTimeDiff = new TextField(Locale.get("guidiscover.TimeDiff")/*Time diff. in minutes between device & display time*/, Integer.toString(Configuration.getTimeDiff()), 5, TextField.ANY);
		//#style formItem
		menuDisplayOptions.append(tfTimeDiff);

		menuDisplayOptions.setCommandListener(this);
	}
	
	//#if polish.api.osm-editing
	private void initOSMaccountOptions() {
		logger.info("Starting OSM account setup menu");
		menuOsmAccountOptions = new Form(Locale.get("guidiscover.OpenStreetMapAccount")/*OpenStreetMap account*/);
		menuOsmAccountOptions.addCommand(BACK_CMD);
		menuOsmAccountOptions.addCommand(OK_CMD);
		menuOsmAccountOptions.setCommandListener(this);
		
		tfOsmUserName = new TextField(Locale.get("guidiscover.Username")/*User name:*/, Configuration.getOsmUsername(), 100, TextField.ANY);
		tfOsmPassword = new TextField(Locale.get("guidiscover.Password")/*Password:*/, Configuration.getOsmPwd(), 100, TextField.ANY | TextField.PASSWORD);
		tfOsmUrl = new TextField(Locale.get("guidiscover.ServerURL")/*Server URL:*/, Configuration.getOsmUrl(), 255, TextField.URL);
		
		//#style formItem
		menuOsmAccountOptions.append(tfOsmUserName);
		//#style formItem
		menuOsmAccountOptions.append(tfOsmPassword);
		//#style formItem
		menuOsmAccountOptions.append(tfOsmUrl);
		
	}
	//#endif

	private void initOnlineOptions() {
		menuOnlineOptions = new Form(Locale.get("guidiscover.OnlineOptions")/*Online options*/);

		String [] internetAccess = { 
				Locale.get("guidiscover.AllowInternetAccess") /*Allow Internet Access*/
		};
		
		internetAccessGroup = new ChoiceGroup(Locale.get("guidiscover.InternetAccess")/*Internet Access*/, Choice.MULTIPLE, internetAccess, null);
		//#style formItem
		menuOnlineOptions.append(internetAccessGroup);
		internetAccessGroup.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_INTERNET_ACCESS));
		
		int langNum = 0;
		if (Legend.numOnlineLang + numLangDifference > 1) {
			String [] onlineLang = new String[Legend.numOnlineLang + numLangDifference];
			for (int i = 0; i < Legend.numOnlineLang; i++) {
				if (i + numLangDifference >= 0) {
					onlineLang[i + numLangDifference] = Legend.onlineLangName[i];
				}
				if (Configuration.getLocaleLang() != null && Legend.onlineLang[i].equalsIgnoreCase("devdefault")) {
					onlineLang[i] = Locale.get("guidiscover.devicedefault")/*Device default*/ +
						" (" + System.getProperty("microedition.locale").substring(0, 2) + ")";
				}
			}
			onlineLangGroup = new ChoiceGroup(Locale.get("guidiscover.OnlineLanguage")/*Online language*/, Choice.EXCLUSIVE, onlineLang, null);
			//#style formItem
			menuOnlineOptions.append(onlineLangGroup);
		}

		if (Legend.numOnlineLang + numLangDifference > 1) {
			String lang = Configuration.getOnlineLang();
			for (int i = 0; i < Legend.numOnlineLang; i++) {
				if (i + numLangDifference >= 0 && Legend.onlineLang[i].equalsIgnoreCase(lang)) {
					langNum = i + numLangDifference;
				}
			}
			onlineLangGroup.setSelectedIndex( langNum, true);
		}

		String [] onlineSetup = { 
			Locale.get("guiwebinfo.GeoHack"),
			Locale.get("guiwebinfo.Weather"),
			Locale.get("guiwebinfo.Phone"),
			Locale.get("guiwebinfo.Website"),
			Locale.get("guiwebinfo.WikipediaRSS"),
			Locale.get("guiwebinfo.TopoMap")
		};
		onlineOptionGroup = new ChoiceGroup(Locale.get("guidiscover.OnlineSetup")/*Online function setup*/, Choice.MULTIPLE, onlineSetup, null);
		//#style formItem
		menuOnlineOptions.append(onlineOptionGroup);

		onlineOptionGroup.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_GEOHACK));

		onlineOptionGroup.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEATHER));

		onlineOptionGroup.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE));

		onlineOptionGroup.setSelectedIndex(3, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE));

		onlineOptionGroup.setSelectedIndex(4, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WIKIPEDIA_RSS));

		onlineOptionGroup.setSelectedIndex(5, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_TOPOMAP));

		menuOnlineOptions.addCommand(BACK_CMD);
		menuOnlineOptions.addCommand(OK_CMD);
		menuOnlineOptions.setCommandListener(this);
	}

	private void initOpencellidOptions() {
		logger.info("Starting Opencellid apikey setup menu");
		menuOpencellidOptions = new Form(Locale.get("guidiscover.Opencellid")/*Opencellid*/);
		menuOpencellidOptions.addCommand(BACK_CMD);
		menuOpencellidOptions.addCommand(OK_CMD);
		menuOpencellidOptions.addCommand(CELLID_CACHE_RESET_CMD);
		menuOpencellidOptions.setCommandListener(this);
		//tfOpencellidApikey = new TextField(Locale.get("guidiscover.apikey")/*apikey:*/, Configuration.getOpencellidApikey(), 100, TextField.ANY);
		
		//menuOpencellidOptions.append(tfOpencellidApikey);

		String [] cellidOpts = new String[2];
		boolean[] opencellidFlags = new boolean[2];

		cellidOpts[0] = Locale.get("guidiscover.noonlinecellid")/*Do not use online cellid lookups*/;
		cellidOpts[1] = Locale.get("guidiscover.onlyonlinecellid")/*Use only online cellid lookups*/;
		//cellidOpts[2] = "Upload log always";
		//cellidOpts[3] = "Confirmation before log upload";
		//cellidOpts[4] = "Fallback to cellid when no fix";
		
		cellidOptsGroup = new ChoiceGroup(Locale.get("guidiscover.CellidOptions")/*Cellid options*/, Choice.MULTIPLE, cellidOpts, null);
		opencellidFlags[0] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_OFFLINEONLY);
		opencellidFlags[1] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_ONLINEONLY);
		//opencellidFlags[2] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_ALWAYS);
		//opencellidFlags[3] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_CONFIRM);
		//opencellidFlags[4] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_FALLBACK);

		cellidOptsGroup.setSelectedFlags(opencellidFlags);
		//#style formItem
		menuOpencellidOptions.append(cellidOptsGroup);
	}

	public void commandAction(Command c, Item i) {
		// forward item command action to form
		commandAction(c, (Displayable) null);
	}
	
	public void commandAction(Command c, Displayable d) {
		//#debug debug
		logger.debug("GuiDiscover got Command " + c);

		if (c == EXIT_CMD) {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)) {
				Trace.getInstance().performIconAction(IconActionPerformer.BACK_ACTIONID, null);
			} else {
				destroy();
				Trace.getInstance().show();
			}
			return;
		}
		if (c == BACK_CMD) {
			//#if polish.andoid
			if (state == STATE_ROOT) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SETUP)
				    && Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)) {
					Trace.getInstance().performIconAction(IconActionPerformer.BACK_ACTIONID, null);
				} else {
					destroy();
					Trace.getInstance().show();
				}
			}
			//#endif
			if (state == STATE_BT_GPX) {
				state = STATE_GPX;
			} else if (state == STATE_BT_GPS) {
				state = STATE_BT_OPT;
			} else if (state == STATE_URL_ENTER_GPS) {
				state = STATE_LOC_PROV;
			} else if (state == STATE_BT_OPT) {
				state = STATE_LOC_PROV;
			} else if (state == STATE_URL_ENTER_GPX) {
				state = STATE_GPX;
			} else {
				state = STATE_ROOT;
				//#if polish.android
				menu.setSelectedIndex(MENU_ITEM_BACK, true);
				//#endif
			}
			show();
			return;
		}
		if (c == FILE_MAP) {
			handleFileMapCommand();
		}
		if (c == SELECT_DIR) {
			handleFileMapCommand();
		}
		if (c == MANUAL_URL_CMD) {
			menuURLEnter = new Form(Locale.get("guidiscover.EnterConnectionUrl")/*Enter connection url*/);
			tfURL = new TextField(Locale.get("guidiscover.URL")/*URL*/, gpsUrlStr, 256, TextField.ANY);
			menuURLEnter.addCommand(OK_CMD);
			menuURLEnter.addCommand(BACK_CMD);
			menuURLEnter.setCommandListener(this);
			//#style formItem
			menuURLEnter.append(tfURL);
			ShareNav.getInstance().show(menuURLEnter);
			if (state == STATE_BT_GPS) {
				state = STATE_URL_ENTER_GPS;
			} else if (state == STATE_LOC_PROV) {
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
			ShareNav.getInstance().show(menuBT);
			if (state == STATE_BT_OPT) {
				logger.info("Discovering a bluetooth serial device");
				state = STATE_BT_GPS;
				gps = new DiscoverGps(this, DiscoverGps.UUDI_SERIAL);
			} else {
				logger.info("Discovering a bluetooth obex file device");
				state = STATE_BT_GPX;
				gps = new DiscoverGps(this, DiscoverGps.UUDI_FILE);
			}
			 
			//#else
			logger.error(Locale.get("guidiscover.BluetoothNotCompiledIn")/*Bluetooth is not compiled into this version*/);
			//#endif
		}
		//#if polish.api.osm-editing
		if (c == OSM_URL) {
			gpxUrl.setText(Configuration.getOsmUrl() + "gpx/create");
		}
		if (c == CELLID_CACHE_RESET_CMD) {
			SECellId.deleteCellIDRecordStore();
		}
		//#endif
		if (c == OK_CMD) {
			switch (state) {
			case STATE_ROOT:
				//#if polish.android
				if (menu.getSelectedIndex() == MENU_ITEM_BACK) {
					if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SETUP)
					    && Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)) {
						Trace.getInstance().performIconAction(IconActionPerformer.BACK_ACTIONID, null);
					} else {
						Trace.getInstance().show();
					}
					return;
				}
				//#endif
				showSetupDialog(menu.getSelectedIndex());
				break;
			case STATE_BT_GPS:
				// remember discovered BT URL and put status in form
				if(urlList.size() != 0) {
					gpsUrlStr = (String) urlList.elementAt(menuBT.getSelectedIndex());
					gpsUrl.setText((gpsUrlStr == null) ? Locale.get("guidiscover.Discover")/*<Discover>*/ : Locale.get("guidiscover.Discovered")/*<Discovered>*/);
					Configuration.setBtUrl(gpsUrlStr);
				}
				state = STATE_BT_OPT;
				show();
				break;
			case STATE_BT_GPX:
				// put discovered BT Url in form
				if(urlList.size() != 0) {
					String gpxUrlStr = (String) urlList.elementAt(menuBT.getSelectedIndex());
					gpxUrl.setText(gpxUrlStr == null ? Locale.get("guidiscover.xyzaoio")/*<Please select in menu>*/ : gpxUrlStr);
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
				//#if polish.android
				menu.setSelectedIndex(MENU_ITEM_BACK, true);
				//#endif
				show();
				break;
			case STATE_LOC_PROV:
				Configuration.setLocationProvider(locProv.getSelectedIndex());
				boolean [] selraw = new boolean[2];
				rawLogCG.getSelectedFlags(selraw);
				Configuration.setGpsRawLoggerUrl(rawLogDir);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_LOGGING, selraw[0]);
				Configuration.setGpsRawLoggerEnable(selraw[1]);

				autoConnect.getSelectedFlags(selraw);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_AUTO_START_GPS, selraw[0]);
				cellIDStartup.getSelectedFlags(selraw);
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_CELLID_STARTUP, selraw[0]);
				if (Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_SIRF || Configuration.getLocationProvider() == Configuration.LOCATIONPROVIDER_NMEA) {
					// show further NMEA/bluetooth options
					state = STATE_BT_OPT;
				} else {
					state = STATE_ROOT;
					//#if polish.android
					menu.setSelectedIndex(MENU_ITEM_BACK, true);
					//#endif
				}
				String w=tfAltitudeCorrection.getString();
				Configuration.setAltitudeCorrection(
							       (int) (Float.parseFloat(w))
							       );


				show();
				break;
			case STATE_BT_OPT:
				selraw = new boolean[2];
				btKeepAlive.getSelectedFlags(selraw);
				Configuration.setBtKeepAlive(selraw[0]);
				btAutoRecon.getSelectedFlags(selraw);
				Configuration.setBtAutoRecon(selraw[0]);
				state = STATE_ROOT;
				//#if polish.android
				menu.setSelectedIndex(MENU_ITEM_BACK, true);
				//#endif
				show();
				break;
			case STATE_MAP:
				returnFromMapOptions();
				break;
			case STATE_DISP_OPT:
				returnFromDisplayOptions();
				break;
			case STATE_DEBUG:
				returnFromDebugOptions();				
				break;
			//#if polish.api.osm-editing
			case STATE_OSM_OPT:
				Configuration.setOsmUsername(tfOsmUserName.getString());
				Configuration.setOsmPwd(tfOsmPassword.getString());
				Configuration.setOsmUrl(tfOsmUrl.getString());
				state = STATE_ROOT;
				//#if polish.android
				menu.setSelectedIndex(MENU_ITEM_BACK, true);
				//#endif
				this.show();
				break;
			//#endif
			case STATE_ONLINE_OPT:
				returnFromOnlineOptions();
				break;
			case STATE_OPENCELLID_OPT:
				returnFromOpenCellIdOptions();
				break;
			case STATE_URL_ENTER_GPS:
				gpsUrlStr = tfURL.getString();
				Configuration.setBtUrl(gpsUrlStr);
				//state = STATE_BT_OPT;
				state = STATE_LOC_PROV;
				// set method to NMEA
				Configuration.setLocationProvider(2);
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

	private void handleFileMapCommand() {
		//#if polish.api.fileconnection
		String initialDir = "";
		String title = Locale.get("guidiscover.SelectMapSource")/*Select Map Source*/;
		switch (state) {
			case STATE_LOC_PROV:
				title = Locale.get("guidiscover.RawLogDir")/*Raw GPS/CellID Log Directory*/;
				initialDir = (rawLogDir == null) ? "" : rawLogDir;
				break;
			case STATE_MAP:
				title = Locale.get("guidiscover.MapZipFileOrDirectory")/*Map ZipFile or Directory*/;
				// get initialDir from form
				String url = mapSrc.getString(1);
				// skip "Filesystem: " or equivalent
				url = url.substring(Locale.get("guidiscover.Filesystem").length());
				initialDir = url;
				break;
			case STATE_GPX:
				title = Locale.get("guidiscover.GpxDirectory")/*Gpx Directory*/;
				initialDir = gpxUrl.getText();
				break;
			case STATE_DEBUG:
				title = Locale.get("guidiscover.LogDirectory")/*Log Directory*/;
				initialDir = Configuration.getDebugRawLoggerUrl();
				break;
		}
		// no valid url, i.e. "Please select to destination first" ?
		if(initialDir != null && !initialDir.toLowerCase().startsWith("file:///")) {
			initialDir = null;
		}
		FsDiscover fsd = new FsDiscover(this, this, initialDir, 
				(state == STATE_MAP) ? FsDiscover.CHOOSE_FILE_OR_DIR : FsDiscover.CHOOSE_DIRONLY, 
				(state == STATE_MAP) ? ".zip;.jar;.apk" : null, title);
		fsd.show();
		//#else
		//logger.error("Files system support is not compiled into this version");
		//#endif
	}

	private void showSetupDialog(int menuNr) {
		switch (menuNr) {
			case MENU_ITEM_LOCATION: // Location Receiver
				initLocationSetupMenu();
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
				cellIDStartup.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_CELLID_STARTUP));
				//Display.getDisplay(parent).setCurrent(menuSelectLocProv);
				ShareNav.getInstance().show(menuSelectLocProv);
				state = STATE_LOC_PROV;
				break;
			case MENU_ITEM_BLUETOOTH: // NMEA Location Receiver setup
				initNMEASetupMenu();
				gpsUrlStr = Configuration.getBtUrl();
				gpsUrl.setText(gpsUrlStr == null ? Locale.get("guidiscover.Discover")/*<Discover>*/ : Locale.get("guidiscover.Discovered")/*<Discovered>*/);

				btKeepAlive.setSelectedIndex(0, Configuration.getBtKeepAlive());
				btAutoRecon.setSelectedIndex(0, Configuration.getBtAutoRecon());
				Display.getDisplay(parent).setCurrentItem(gpsUrl);
				ShareNav.getInstance().show(menuNMEAOptions);
				state = STATE_BT_OPT;
				break;
			case MENU_ITEM_GPX_FILTER: // Recording Rules
				GuiSetupRecordings guiRec = new GuiSetupRecordings(this);
				guiRec.show();
				break;
			case MENU_ITEM_DISP_OPT: // Display Options
				initDisplay();
				boolean addSelectedUiLang = false;
				String selectedUiLang = Configuration.getUiLang();
				if (!selectedUiLang.equals("") && !selectedUiLang.equals("devdefault")) {
					addSelectedUiLang = true;
				}
				for (int i = 0; i < Legend.numUiLang; i++) {
					if (addSelectedUiLang && Legend.uiLang[i].equalsIgnoreCase(selectedUiLang)) {
						addSelectedUiLang = false;
					}
				}
				int addedLang = 0;
				// add the lang selected in config
				if (addSelectedUiLang) {
					addedLang += 1;
				}
				int langNum = 0; // default is the first in bundle
				if (Legend.numUiLang + numLangDifference > 1) {
					String lang = Configuration.getUiLang();
					for (int i = 0; i < Legend.numUiLang + addedLang; i++) {
						if (i + numLangDifference >= 0  && i + numLangDifference < Legend.numUiLang && Legend.uiLang[i].equalsIgnoreCase(lang)) {
							langNum = i + numLangDifference;
						}
						// didn't find in legend languages, so it's the added language choice
						if (i + numLangDifference >= Legend.numUiLang && langNum == 0) {
							langNum = i + numLangDifference;
						}
					}
					uiLangGroup.setSelectedIndex( langNum, true);
				}
//#if 0
				/* disable for now, maybe later add in another menu, advanced or i18n */
				langNum = 0;
				if (Legend.numNaviLang  + numLangDifference > 1) {
					String lang = Configuration.getNaviLang();
					for (int i = 0; i < Legend.numNaviLang; i++) {
						if (i + numLangDifference >= 0 && Legend.naviLang[i].equalsIgnoreCase(lang)) {
							langNum = i + numLangDifference;
						}
					}
					naviLangGroup.setSelectedIndex( langNum, true);
				}
//#endif
				nightModeGroup.setSelectedIndex( Configuration.getCfgBitSavedState(Configuration.CFGBIT_NIGHT_MODE_AUTO) ? 2 : 
								 ( Configuration.getCfgBitSavedState(Configuration.CFGBIT_NIGHT_MODE) ? 1 : 0), true);
				rotationGroup.setSelectedIndex(Configuration.getProjDefault(), true);
				renderOpts.setSelectedIndex( Configuration.getCfgBitSavedState(Configuration.CFGBIT_STREETRENDERMODE) ? 1 : 0, true);
				directionDevOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_AUTOCALIBRATE));
				directionDevOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_ALWAYS_ROTATE));
				directionOpts.setSelectedIndex( Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_DIRECTION) ? 1 : 0, true);
				if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)) {
					directionOpts.setSelectedIndex(2, true);
				}
				distanceViews.setSelectedIndex( Configuration.getCfgBitSavedState(Configuration.CFGBIT_DISTANCE_VIEW) ? 1 : 0, true);
				sizeOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_POI_LABELS_LARGER));
				sizeOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_WPT_LABELS_LARGER));
				sizeOpts.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_LARGE_FONT));
				mapInfoOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS));
				mapInfoOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_SCALE_BAR));
				mapInfoOpts.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_SPEED_IN_MAP));
				mapInfoOpts.setSelectedIndex(3, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_ALTITUDE_IN_MAP));
				mapInfoOpts.setSelectedIndex(4, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_IN_MAP));
				mapInfoOpts.setSelectedIndex(5, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_WHEN_ROUTING));
				mapInfoOpts.setSelectedIndex(6, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_CLOCK_IN_MAP));
				mapInfoOpts.setSelectedIndex(7, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_ACCURACY));
				mapInfoOpts.setSelectedIndex(8, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_TRAVEL_MODE_IN_MAP));
				clockOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_GPS_TIME));
				clockOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_GPS_TIME_FALLBACK));
				metricUnits.setSelectedIndex(Configuration.getCfgBitSavedState(Configuration.CFGBIT_METRIC) ? 0 : 1, true);
				visualOpts.setSelectedIndex(0, ! Configuration.getCfgBitSavedState(Configuration.CFGBIT_NOSTREETBORDERS));
				visualOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ROUND_WAY_ENDS));
				SingleTile.newPOIFont();
				WaypointsTile.useNewWptFont();

				// convert bits from backlight flag into selection states
				boolean[] sellight = new boolean[10];
				sellight[0] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ON);
				sellight[1] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED);
				sellight[2] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_KEEPALIVE);
				sellight[3] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_MIDP2);
				int i = 4;
				//#if polish.api.nokia-ui
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIA);
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH);
				//#endif
				//#if polish.api.min-siemapi
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_SIEMENS);
				//#endif
				//#if polish.api.min-samsapi
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_SAMSUNG);
				//#endif
				//#if polish.android
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WAKELOCK);
					sellight[i++] = Configuration.getCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WINDOW_MANAGER);
				//#endif
		
				backlightOpts.setSelectedFlags(sellight);
				
				ShareNav.getInstance().show(menuDisplayOptions);
				state = STATE_DISP_OPT;
				break;
			case MENU_ITEM_GPX_DEVICE: // GPX Receiver
				//Prepare Gpx receiver selection menu
				menuGpx = new Form(Locale.get("guidiscover.GpxReceiver")/*Gpx Receiver*/);
				menuGpx.addCommand(BACK_CMD);
				menuGpx.addCommand(OK_CMD);
				menuGpx.addCommand(SELECT_DIR);
				menuGpx.addCommand(BT_MAP);
				//#if polish.api.osm-editing
				menuGpx.addCommand(OSM_URL);
				//#endif
		
				gpxUrl = new StringItem(Locale.get("guidiscover.GpxReceiverUrl")/*Gpx Receiver Url: */, Locale.get("guidiscover.xyzaoio")/*<Please select in menu>*/);
				//#style formItem
				menuGpx.append(gpxUrl);
				menuGpx.setCommandListener(this);
				gpxUrl.setText(Configuration.getGpxUrl() == null ?
					       Locale.get("guidiscover.xyzaoio")/*<Please select in menu>*/ : Configuration.getGpxUrl());
				ShareNav.getInstance().show(menuGpx);
				state = STATE_GPX;
				break;
			case MENU_ITEM_MAP_SRC: // Map Source
				initMapSource();
				mapSrc.set(1, Locale.get("guidiscover.Filesystem")/*Filesystem: */ + ( (Configuration.getMapUrl() == null) ?
								 Locale.get("guidiscover.PleaseSelectMapDirFirst")/*<Please select map directory or other .jar/zip file first>*/ :
							Configuration.getMapUrl() ), null);
				mapSrc.setSelectedIndex(Configuration.usingBuiltinMap() ? 0 : 1, true);
				mapSrcOptions.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_MAP_CREDITS));
				mapSrcOptions.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS));
				mapSrcOptions.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS));

				int j = 0;
				//#if polish.android
				perfTuneOptions.setSelectedIndex(j++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS));
				//#endif
				perfTuneOptions.setSelectedIndex(j++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_BUFFEREDINPUTSTREAM));
				perfTuneOptions.setSelectedIndex(j++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_RESOLVE_NAMES_LAST));
				perfTuneOptions.setSelectedIndex(j++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SIMPLIFY_MAP_WHEN_BUSY));

				tileMapOptions.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_TMS_BACKGROUND));
				tileMapOptions.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_DISABLE_AREAS_WHEN_BACKGROUND_MAP));
				tileMapOptions.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_DISABLE_BUILDINGS_WHEN_BACKGROUND_MAP));
				tileMapOptions.setSelectedIndex(3, Configuration.getCfgBitSavedState(Configuration.CFGBIT_TMS_SPLITSCREEN));

				ShareNav.getInstance().show(menuSelectMapSource);
				state = STATE_MAP;
				break;
			case MENU_ITEM_DEBUG_OPT:
				initDebugSetupMenu();
				ShareNav.getInstance().show(menuDebug);
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
			case MENU_ITEM_ONLINE_OPT:
				/**
				 * Online options (language, which services enabled)
				 */
				initOnlineOptions();
				ShareNav.getInstance().show(menuOnlineOptions);
				state = STATE_ONLINE_OPT;
				break;
			//#if polish.api.osm-editing
			case MENU_ITEM_OSM_OPT:
				/**
				 * OpenStreetMap account information
				 */
				initOSMaccountOptions();
				ShareNav.getInstance().show(menuOsmAccountOptions);
				state = STATE_OSM_OPT;
				break;
			//#endif
			case MENU_ITEM_OPENCELLID_OPT:
				/**
				 * Opencellid Apikey
				 */
				initOpencellidOptions();
				ShareNav.getInstance().show(menuOpencellidOptions);
				state = STATE_OPENCELLID_OPT;
				break;
			//#if polish.api.mmapi
			case MENU_ITEM_CAMERA_OPT:
				/**
				 * Camera options
				 */
				//GuiCamera.getInstance().init(this);
				try {
					Class GuiCameraClass = Class.forName("net.sharenav.sharenav.ui.GuiCamera");
					Object GuiCameraObject = GuiCameraClass.newInstance();
					GuiCameraInterface cam = (GuiCameraInterface)GuiCameraObject;
					//cam.commandAction(GuiCamera.getInstance().SETUP_CMD, (Displayable) null);
					//cam.init(this);
					//GuiCamera.getInstance().setup(this);
					cam.setup(this);
				} catch (ClassNotFoundException cnfe) {
					logger.exception(Locale.get("trace.YourPhoneNoCamSupport"), cnfe);
				} catch (InstantiationException ie) {
					logger.exception(Locale.get("trace.YourPhoneNoCamSupport"), ie);
				} catch (IllegalAccessException ie) {
					logger.exception(Locale.get("trace.YourPhoneNoCamSupport"), ie);
				}
				//GuiCamera.getInstance().commandAction(GuiCamera.getInstance().SETUP_CMD, (Displayable) null);
				//ShareNav.getInstance().show(menuOpencellidOptions);
				break;
			//#endif
			//#if polish.api.fileconnection
			case MENU_ITEM_EXPORT_CONFIG:
				state = STATE_EXPORT_CONFIG;
				FsDiscover fsd = new FsDiscover(this, this, null, FsDiscover.CHOOSE_DIRONLY, 
						null, Locale.get("guidiscover.ExportConfig")/*"Export config"*/);
				fsd.show();
				break;
			case MENU_ITEM_IMPORT_CONFIG:
				state = STATE_IMPORT_CONFIG;
				fsd = new FsDiscover(this, this, null, FsDiscover.CHOOSE_FILEONLY, "cfg", 
						Locale.get("guidiscover.ImportConfig")/*"Import config"*/);
				fsd.show();
				break;
			//#endif
		}
	}
	
	private void returnFromMapOptions() {
		Configuration.setBuiltinMap((mapSrc.getSelectedIndex() == 0));
		// extract map url from form and save it to Configuration
		String url = mapSrc.getString(1);
		// skip "Filesystem: " or translation
		url = url.substring(Locale.get("guidiscover.Filesystem").length());
		// no valid url, i.e. "Please select..."
		if (url.indexOf(":") == -1) {
			url = null;
		}
		Configuration.setMapUrl(url);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_MAP_CREDITS, mapSrcOptions.isSelected(0));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_PNGS, mapSrcOptions.isSelected(1));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_PREFER_INTERNAL_SOUNDS, mapSrcOptions.isSelected(2));
		int i=0;
		//#if polish.android
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_PREFER_OUTLINE_AREAS, perfTuneOptions.isSelected(i++));
		//#endif
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BUFFEREDINPUTSTREAM, perfTuneOptions.isSelected(i++));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_RESOLVE_NAMES_LAST, perfTuneOptions.isSelected(i++));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SIMPLIFY_MAP_WHEN_BUSY, perfTuneOptions.isSelected(i++));

		boolean bgMapOptionsChanged = false;
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_DISABLE_AREAS_WHEN_BACKGROUND_MAP) != tileMapOptions.isSelected(1)) {
			bgMapOptionsChanged = true;
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_DISABLE_BUILDINGS_WHEN_BACKGROUND_MAP) != tileMapOptions.isSelected(2)) {
			bgMapOptionsChanged = true;
		}

		Configuration.setCfgBitSavedState(Configuration.CFGBIT_DISABLE_AREAS_WHEN_BACKGROUND_MAP, tileMapOptions.isSelected(1));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_DISABLE_BUILDINGS_WHEN_BACKGROUND_MAP, tileMapOptions.isSelected(2));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_TMS_SPLITSCREEN, tileMapOptions.isSelected(3));

		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_TMS_BACKGROUND) &&
		    !tileMapOptions.isSelected(0)) {
			// raster tile map switched off, restore user-set state of projection
			// and autozoom
			Configuration.setCfgBitState(Configuration.CFGBIT_AUTOZOOM, Configuration.getCfgBitSavedState(Configuration.CFGBIT_AUTOZOOM), false);
			Configuration.setCfgBitState(Configuration.CFGBIT_AREAS, Configuration.getCfgBitSavedState(Configuration.CFGBIT_AREAS), false);
			Configuration.setCfgBitState(Configuration.CFGBIT_BUILDINGS, Configuration.getCfgBitSavedState(Configuration.CFGBIT_BUILDINGS), false);
			ProjFactory.setProj(Configuration.getProjDefault());
		}
		if ((bgMapOptionsChanged || !Configuration.getCfgBitSavedState(Configuration.CFGBIT_TMS_BACKGROUND))
		    && tileMapOptions.isSelected(0)) {
			// raster tile map switched on or settings changed, set projection to north up
			// and switch autozoom off
			Configuration.setCfgBitState(Configuration.CFGBIT_AUTOZOOM, false, false);
			ProjFactory.setProj(ProjFactory.NORTH_UP);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_DISABLE_AREAS_WHEN_BACKGROUND_MAP)) {
				Configuration.setCfgBitState(Configuration.CFGBIT_AREAS, false, false);
			} else {
				Configuration.setCfgBitState(Configuration.CFGBIT_AREAS, Configuration.getCfgBitSavedState(Configuration.CFGBIT_AREAS), false);
			}
			if (Configuration.getCfgBitState(Configuration.CFGBIT_DISABLE_BUILDINGS_WHEN_BACKGROUND_MAP)) {
				Configuration.setCfgBitState(Configuration.CFGBIT_BUILDINGS, false, false);
			} else {
				Configuration.setCfgBitState(Configuration.CFGBIT_BUILDINGS, Configuration.getCfgBitSavedState(Configuration.CFGBIT_BUILDINGS), false);
			}
		}
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_TMS_BACKGROUND, tileMapOptions.isSelected(0));

		Configuration.setTMSUrl(tfTMSUrl.getString());
		Configuration.setTMSFilecachePath(tfFCPath.getString());

		rereadMap();
		//logger.fatal(Locale.get("guidiscover.NeedRestart")/*Need to restart ShareNav, otherwise map is in an inconsistant state*/ + " " + Configuration.getMapUrl());
		//#if polish.android
		if (!Legend.getMapFlag(Legend.LEGEND_MAPFLAG_OUTLINE_AREA_BLOCK)) {
		        ShareNav.getInstance().alert(Locale.get("guidiscover.mapalert"),
						  Locale.get("guidiscover.noarea"),
						  Alert.FOREVER);
		}
		//#endif
	}

	private void rereadMap() {
		Configuration.closeMapZipFile();
		if (!Legend.isValid) {
			Trace.clearTraceInstance();
		}
		Legend.reReadLegend();
		Trace trace = Trace.getInstance();
		trace.restart();
		trace = Trace.getInstance();
		trace.recreateTraceLayout();
		state = STATE_ROOT;
		//#if polish.android
		menu.setSelectedIndex(MENU_ITEM_BACK, true);
		//#endif
		show();
	}

	private void returnFromDisplayOptions() {
		String uiLang = null;
		if (!addLang.getString().equalsIgnoreCase(Configuration.getUiLang())) {
			langToAdd = addLang.getString().toLowerCase();
			if (langToAdd.equals("")) {
				langToAdd = "devdefault";
			}
			uiLang = langToAdd;
		} else if (uiLangGroup != null && uiLangGroup.getSelectedIndex() - numLangDifference >= Legend.numUiLang) {
			// avoid array out of bounds, see https://sourceforge.net/projects/sharenav/forums/forum/677689/topic/5216306
			uiLang = langToAdd;
		} else if (Legend.numUiLang + numLangDifference > 1) {
			uiLang = Legend.uiLang[uiLangGroup.getSelectedIndex()-numLangDifference];
		}
		if (uiLang != null) {
			if (!Configuration.setUiLang(uiLang)) {
				logger.error(Locale.get("guidiscover.LangNotFound")/*Couldn't set language to */ + uiLang);
				System.out.println("Couldn't open translations file");
			}
			Configuration.setWikipediaLang(uiLang);
			Configuration.setNamesOnMapLang(uiLang);
		}
//#if 0
		/* move these to another menu, advanced or i18n */
		String naviLang = null;				
		if (langToAdd != null) {
			naviLang = langToAdd;
		} else if (Legend.numNaviLang + numLangDifference > 1) {
			naviLang = Legend.naviLang[naviLangGroup.getSelectedIndex()-numLangDifference];
			if (naviLang.equals(Configuration.getNaviLang()) && ! naviLang.equalsIgnoreCase("devdefault")) {
				naviLang = null;
			}
		}
		if (naviLang != null) {
			String locale = null;
			String naviLangUse = naviLang;
			boolean multipleDirsForLanguage = false;
			if (naviLang.equalsIgnoreCase("devdefault")) {
				// get phone's locale
				locale = System.getProperty("microedition.locale");

				if (locale != null) {
					naviLangUse = locale.substring(0, 2);
				} else {
					if (Legend.numNaviLang > 1) {
						naviLangUse = Legend.naviLang[1];
					} else {
						naviLangUse = "en";
					}
				}
			}
			// store language choice. Sound directory choice takes preference over it.
			Configuration.setNaviLang(naviLang);

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
				if (Legend.soundDirectories[i].substring(Legend.soundDirectories[i].length() - 3).equals("-" + naviLangUse)) {
					// first suitable-language sound dir is taken into use
					if (soundDir == null) {
						soundDir = Legend.soundDirectories[i];
					} else {
						multipleDirsForLanguage = true;
					}
				}
			}

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
				logger.error(Locale.get("guidiscover.SoundDirectoryFor")/*Sound directory for */ + naviLangUse + Locale.get("guidiscover.NotFound")/* not found!*/);
			}
			//if (multipleDirsForLanguage) {
			// FIXME: if more than one dir for lang available,
			// tell the user it can be switched at "sounds&alerts" menu
			// ("Changing language", "Selecting " + soundDir, 3000);
			//}
		}
//#endif
		boolean nightMode = (nightModeGroup.getSelectedIndex() == 1);
		boolean nightModeAuto = (nightModeGroup.getSelectedIndex() == 2);
		
		if (nightMode != Configuration.getCfgBitState(Configuration.CFGBIT_NIGHT_MODE) ) {
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_NIGHT_MODE, nightMode);
			Legend.reReadLegend();
			Trace trace = Trace.getInstance();
			trace.recreateTraceLayout();
		}

		if (nightModeAuto != Configuration.getCfgBitState(Configuration.CFGBIT_NIGHT_MODE_AUTO) ) {
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_NIGHT_MODE_AUTO, nightModeAuto);
			Legend.reReadLegend();
			Trace trace = Trace.getInstance();
			trace.recreateTraceLayout();
		}

		Configuration.setProjTypeDefault( (byte) rotationGroup.getSelectedIndex() );

		if ((!Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_DIRECTION)) &&
		    (directionOpts.getSelectedIndex() == 1 || directionOpts.getSelectedIndex() == 2)) {
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_COMPASS_DIRECTION, true);
			Trace.getInstance().startCompass();
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_DIRECTION)
		    && directionOpts.getSelectedIndex() == 0) {
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_COMPASS_DIRECTION, false);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION, false);
			Trace.getInstance().stopCompass();
		}

		if ((!Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION))
		    && directionOpts.getSelectedIndex() == 2) {
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION,
							  true);
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION)
		    && directionOpts.getSelectedIndex() != 2) {
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_COMPASS_AND_MOVEMENT_DIRECTION,
							  false);
		}
		boolean calibrateOpts[] = new boolean[2];
		directionDevOpts.getSelectedFlags(calibrateOpts);

		Configuration.setCfgBitSavedState(Configuration.CFGBIT_COMPASS_AUTOCALIBRATE, directionDevOpts.isSelected(0));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_COMPASS_ALWAYS_ROTATE, directionDevOpts.isSelected(1));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_STREETRENDERMODE,
				(renderOpts.getSelectedIndex() == 1)
		);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_DISTANCE_VIEW,
				(distanceViews.getSelectedIndex() == 1)
		);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_POI_LABELS_LARGER, sizeOpts.isSelected(0));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_WPT_LABELS_LARGER, sizeOpts.isSelected(1));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_LARGE_FONT, sizeOpts.isSelected(2));
		
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS, mapInfoOpts.isSelected(0));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_SCALE_BAR, mapInfoOpts.isSelected(1));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_SPEED_IN_MAP, mapInfoOpts.isSelected(2));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_ALTITUDE_IN_MAP, mapInfoOpts.isSelected(3));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_IN_MAP, mapInfoOpts.isSelected(4));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_AIR_DISTANCE_WHEN_ROUTING, mapInfoOpts.isSelected(5));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_CLOCK_IN_MAP, mapInfoOpts.isSelected(6));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_ACCURACY, mapInfoOpts.isSelected(7));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_TRAVEL_MODE_IN_MAP, mapInfoOpts.isSelected(8));
		
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_GPS_TIME, clockOpts.isSelected(0));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_GPS_TIME_FALLBACK, clockOpts.isSelected(1));

		String secs = tfAutoRecenterToGpsSecs.getString();
		Configuration.setAutoRecenterToGpsMilliSecs(
				(int) (Float.parseFloat(secs)) * 1000
		);
		
		// convert boolean array with selection states for backlight
		// to one flag with corresponding bits set
		boolean[] sel = new boolean[10];
		backlightOpts.getSelectedFlags( sel );
		// save selected values to record store
		int i = 0;
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ON, sel[i++]);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_WHILE_GPS_STARTED, sel[i++]);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ONLY_KEEPALIVE, sel[i++]);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_MIDP2, sel[i++]);
		//#if polish.api.nokia-ui
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIA, sel[i++]);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH, sel[i++]);
		//#endif
		//#if polish.api.min-siemapi
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_SIEMENS, sel[i++]);
		//#endif
		//#if polish.api.min-samsapi
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_SAMSUNG, sel[i++]);
		//#endif
		//#if polish.android
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WAKELOCK, sel[i++]);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ANDROID_WINDOW_MANAGER, sel[i++]);
		//#endif
		
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_METRIC, (metricUnits.getSelectedIndex() == 0));
		
		sel = new boolean[2];
		visualOpts.getSelectedFlags(sel);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_NOSTREETBORDERS, ! sel[0]);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUND_WAY_ENDS, sel[1]);

		String w=tfDestLineWidth.getString();
		Configuration.setDestLineWidth(
				(int) (Float.parseFloat(w))
		);

		String d=tfTimeDiff.getString();
		Configuration.setTimeDiff(
				(int) (Float.parseFloat(d))
		);

		state = STATE_ROOT;
		//#if polish.android
		menu.setSelectedIndex(MENU_ITEM_BACK, true);
		//#endif
		show();

		parent.restartBackLightTimer();
	}

	private void returnFromDebugOptions() {
		boolean [] selDebug = new boolean[1];
		debugLog.getSelectedFlags(selDebug);
		Configuration.setDebugRawLoggerEnable((selDebug[0]));
		Configuration.setDebugRawLoggerUrl(debugLog.getString(0));
		ShareNav.getInstance().enableDebugFileLogging();
		selDebug = new boolean[3];
		debugSeverity.getSelectedFlags(selDebug);
		
		Configuration.setDebugSeverityInfo(selDebug[0]);
		Configuration.setDebugSeverityDebug(selDebug[1]);
		Configuration.setDebugSeverityTrace(selDebug[2]);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_CONNECTIONS, debugOther.isSelected(0));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTECONNECTION_TRACES, debugOther.isSelected(1));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS, debugOther.isSelected(2));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ROUTE_BEARINGS, debugOther.isSelected(3));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_TILE_REQUESTS_DROPPED, debugOther.isSelected(4));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_SHOW_NMEA_ERRORS, debugOther.isSelected(5));
		Logger.setGlobalLevel();
		state = STATE_ROOT;
		this.show();

		/**
		 * In order to minimize surprise of the user that despite enabling logging here
		 * nothing shows up in the log file, we warn the user if logging at the specified level
		 * is not compiled into the current Version of ShareNav
		 * 
		 * The check needs to be after this.show() so that the alert messages can be shown
		 * and not disabled by the immediate call to this.show()
		 */
		boolean debugAvail = false;
		//#debug trace
		debugAvail = true;
		if (selDebug[2] && !debugAvail) {
			logger.error(Locale.get("guidiscover.LoggingAtTraceNotCompiledIn")/*Logging at Trace level is not compiled into this version of ShareNav so log will be empty*/);
		}
		debugAvail = false;
		//#debug debug
		debugAvail = true;
		if (selDebug[1] && !debugAvail) {
			logger.error(Locale.get("guidiscover.LoggingAtDebugNotCompiledIn")/*Logging at Debug level is not compiled into this version of ShareNav so log will be empty*/);
		}
		debugAvail = false;
		//#debug info
		debugAvail = true;
		if (selDebug[0] && !debugAvail) {
			logger.error(Locale.get("guidiscover.LoggingAtInfoNotCompiledIn")/*Logging at Info level is not compiled into this version of ShareNav so log will be empty*/);
		}
	}

	private void returnFromOnlineOptions() {
		boolean oldInternetAccess = Configuration.getCfgBitState(Configuration.CFGBIT_INTERNET_ACCESS);
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_INTERNET_ACCESS,
				internetAccessGroup.isSelected(0));
		
		String onlineLang = null;
		if (langToAdd != null) {
			onlineLang = langToAdd;
		} else if (Legend.numOnlineLang + numLangDifference > 1) {
			onlineLang = Legend.onlineLang[onlineLangGroup.getSelectedIndex()-numLangDifference];
		}
		if (onlineLang != null) {
			Configuration.setOnlineLang(onlineLang);
		}
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ONLINE_GEOHACK,
				onlineOptionGroup.isSelected(0));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEATHER,
				onlineOptionGroup.isSelected(1));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE,
				onlineOptionGroup.isSelected(2));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE,
				onlineOptionGroup.isSelected(3));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ONLINE_WIKIPEDIA_RSS,
				onlineOptionGroup.isSelected(4));
		Configuration.setCfgBitSavedState(Configuration.CFGBIT_ONLINE_TOPOMAP,
				onlineOptionGroup.isSelected(5));

		state = STATE_ROOT;
		//#if polish.android
		menu.setSelectedIndex(MENU_ITEM_BACK, true);
		//#endif
		if (internetAccessGroup.isSelected(0) != oldInternetAccess) {
			Trace.uncacheIconMenu();
		}
		this.show();
	}

	private void returnFromOpenCellIdOptions() {
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
		//#if polish.android
		menu.setSelectedIndex(MENU_ITEM_BACK, true);
		//#endif
		this.show();
		//#if polish.api.online
		//#else
		if (opencellidFlags[1]) {
			logger.error(Locale.get("guidiscover.OnlineAccessNotCompiledIn")/*Online access is not compiled into this midlet*/);
		}
		//#endif
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
		menuBT.setTitle(Locale.get("guidiscover.SearchDevice")/*Search Device*/);
	}

	/** Shows Setup menu of MIDlet on the screen. */
	public void show() {
		switch (state) {
			case STATE_ROOT:
				//#if polish.android
				menu.setSelectedIndex(MENU_ITEM_BACK, true);
				//#endif
				if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_SETUP)) {
					showIconMenu();
				} else {
					ShareNav.getInstance().show(menu);
				}
				break;
			case STATE_LOC_PROV:
				//showSetupDialog(MENU_ITEM_LOCATION);
				ShareNav.getInstance().show(menuSelectLocProv);
				break;
			case STATE_BT_OPT:
				showSetupDialog(MENU_ITEM_BLUETOOTH);
				//ShareNav.getInstance().show(menuNMEAOptions);
				break;
			case STATE_MAP:
				ShareNav.getInstance().show(menuSelectMapSource);
				break;
			case STATE_GPX:
				ShareNav.getInstance().show(menuGpx);
				break;
			case STATE_BT_GPX:
				ShareNav.getInstance().show(menuBT);
				break;
			case STATE_DEBUG:
				ShareNav.getInstance().show(menuDebug);
				break;
			case STATE_BT_GPS:
				ShareNav.getInstance().show(menuBT);
				break;
			default:
				logger.error(Locale.get("guidiscover.ShowCalledInvalidState")/*Show called without a valid state*/);
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
		menuBT.setTitle(Locale.get("guidiscover.Devices")/*Devices */ + a);
	}

	public void fsDiscoverReady() {
		menuFileSel.addCommand(STORE_ROOTFS);
		menuFileSel.setTitle(Locale.get("guidiscover.SelectRoot")/*Select Root*/);
	}

	public void addRootFs(String root) {
		//#style formItem
		menuFileSel.append(root, null);
	}

	public void btDiscoverReady() {
//		menuBT.deleteAll();
		menuBT.addCommand(STORE_BT_URL);
		for (int i = 0; i < friendlyName.size(); i++) {
			try {
				//#style formItem
				menuBT.append("" + i + " " + (String) friendlyName.elementAt(i), null);
			} catch (RuntimeException e) {
				//#style formItem
				menuBT.append(e.getMessage(), null);
			}
		}
	}
	
	public void selectionCanceled() {
		//#if polish.api.fileconnection
		switch (state) {
		case STATE_IMPORT_CONFIG:
		case STATE_EXPORT_CONFIG:
			state = STATE_ROOT;
			//#if polish.android
			menu.setSelectedIndex(MENU_ITEM_BACK, true);
			//#endif
			break;
		}
		//#endif
	}

	public void selectedFile(String url) {
		logger.info("Url selected: " + url);
		String url_trunc = url.substring(0, url.lastIndexOf('/') + 1);
		switch (state) {
		case STATE_LOC_PROV:
			rawLogCG.setLabel(LOG_TO + url_trunc);
			rawLogDir = url_trunc;
			break;
		case STATE_GPX:
			gpxUrl.setText(url_trunc);
			break;
		//#if polish.api.fileconnection
		case STATE_MAP:
			mapSrc.set(1, Locale.get("guidiscover.Filesystem")/*Filesystem: */ + url, null);
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
		case STATE_EXPORT_CONFIG:
			ConfigExportImport.exportConfig(url_trunc + "ShareNav.cfg");
			state = STATE_ROOT;
			//#if polish.android
			menu.setSelectedIndex(MENU_ITEM_BACK, true);
			//#endif
			show();
			break;
		case STATE_IMPORT_CONFIG:
			ConfigExportImport.importConfig(url);
			state = STATE_ROOT;
			//#if polish.android
			menu.setSelectedIndex(MENU_ITEM_BACK, true);
			//#endif
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
	
	/** Uncache the icon menu to reflect changes in the setup or save memory */
	public static void uncacheIconMenu() {
		//#mdebug trace
		if (setupIconMenu != null) {
			logger.trace("uncaching setupIconMenu");
		}
		//#enddebug
		setupIconMenu = null;
	}

	/** Interface for IconMenuWithPages: recreate the icon menu from scratch and show it (introduced for reflecting size change of the Canvas) */
	public void recreateAndShowIconMenu() {
		uncacheIconMenu();
		showIconMenu();
	}

	/** Interface for received actions from the IconMenu GUI */
	public void performIconAction(int actionId, String choiceName) {
		if (Trace.getInstance().isShowingSplitIconMenu()) {
			// To make setup forms work, exit from split-screen mode
			// so we return to non-split-screen setup icon menu
			if (actionId != IconActionPerformer.BACK_ACTIONID) {
				Trace.getInstance().clearShowingSplitSetup();
				showSetupDialog(actionId);
			} else {
				// back to normal trace icon menu
				Trace.getInstance().clearShowingSplitSetup();
				Trace.getInstance().setShowingSplitTraceIconMenu();
				Trace.getInstance().restartImageCollector();		
			}
		} else {
			if (actionId == IconActionPerformer.BACK_ACTIONID) {
				commandAction(EXIT_CMD, (Displayable) null);
			} else {
				showSetupDialog(actionId);
			}
		}
	}
}
