package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.TextField;


import de.ueller.gps.data.Configuration;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.gpsMid.mapData.WaypointsTile;

public class GuiDiscover implements CommandListener, ItemCommandListener, GpsMidDisplayable, SelectionListener {

	/** A menu list instance */
	private static final String[]	elements		= {
		"Location Receiver", "Recording Rules",
		"Display options", "Sounds & Alerts", "Routing options",
		"GPX Receiver", "Map source", "Debug options", "Key shortcuts", "OSM account"};
	
	/**
	 * The following MENU_ITEM constatants have to be in
	 * sync with the position in the elements array of the
	 * main menu
	 */
	private static final int MENU_ITEM_LOCATION = 0;
	private static final int MENU_ITEM_GPX_FILTER = 1;	
	private static final int MENU_ITEM_DISP_OPT = 2;
	private static final int MENU_ITEM_SOUNDS_OPT = 3;
	private static final int MENU_ITEM_ROUTING_OPT = 4;
	private static final int MENU_ITEM_GPX_DEVICE = 5;
	private static final int MENU_ITEM_MAP_SRC = 6;
	private static final int MENU_ITEM_DEBUG_OPT = 7;
	private static final int MENU_ITEM_KEYS_OPT = 8;
	private static final int MENU_ITEM_OSM_OPT = 9;

	private static final String[]	empty			= {};

	/** Soft button for exiting to RootMenu. */
	private final Command			EXIT_CMD		= new Command("Back",
															Command.BACK, 2);

	private final Command			BACK_CMD		= new Command("Cancel",
															Command.BACK, 2);

	/** Soft button for discovering BT. */
	private final Command			OK_CMD			= new Command("Ok",
															Command.OK, 1);

	private final Command			STORE_BT_URL	= new Command("Select",
															Command.OK, 2);

	private final Command			STORE_ROOTFS	= new Command("Select",
															Command.OK, 2);
	
	private final Command			FILE_MAP	= new Command("Select Directory",
			Command.ITEM, 2);
	private final Command			BT_MAP	= new Command("Select bluetooth device",
			Command.ITEM, 2);
	//#if api.polish.osm-editing
	private final Command			OSM_URL	= new Command("Upload to OSM", Command.ITEM, 2);
	//#endif
	private final Command			GPS_DISCOVER	= new Command("Discover GPS",
			Command.ITEM, 1);
	
	
	/** A menu list instance */
	private final List				menu			= new List("Setup",
															Choice.IMPLICIT, elements,
															null);

	private List					menuBT;

	private final List				menuFS			= new List("Devices",
															Choice.IMPLICIT, empty,
															null);
	private Form					menuSelectLocProv;
	
	private Form					menuSelectMapSource;
	
	private Form					menuDisplayOptions;
	
	private Form					menuGpx;
	
	private Form					menuDebug;

	private Form					menuRecordingOptions;
	
	private Form					menuRoutingOptions;
	
	//#if api.polish.osm-editing
	private Form					menuOsmAccountOptions;
	//#endif
	
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
	private final static int		STATE_ROUTING_OPT = 11;
	//#if api.polish.osm-editing
	private final static int		STATE_OSM_OPT = 12;
	//#endif
	
	private Vector urlList; 
	private Vector friendlyName;
	private ChoiceGroup locProv;
	private ChoiceGroup choiceGpxRecordRuleMode; 
	private TextField  tfGpxRecordMinimumSecs; 
	private TextField  tfGpxRecordMinimumDistanceMeters; 
	private TextField  tfGpxRecordAlwaysDistanceMeters;
	//#if api.polish.osm-editing
	private TextField  tfOsmUserName;
	private TextField  tfOsmPassword;
	private TextField  tfOsmUrl;
	//#endif
	private ChoiceGroup rawLog;
	private ChoiceGroup mapSrc;
	private Gauge gaugeDetailBoost; 
	private ChoiceGroup rotationGroup;
	private ChoiceGroup renderOpts;
	private ChoiceGroup sizeOpts;
	private ChoiceGroup backlightOpts;
	private ChoiceGroup debugLog;
	private ChoiceGroup debugSeverity;
	private ChoiceGroup debugOther;
	private StringItem  gpxUrl;
	private StringItem  gpsUrl;
	private ChoiceGroup btKeepAlive;
	private ChoiceGroup btAutoRecon;
	  
	private String gpsUrlStr;
	
	private Gauge gaugeRoutingEsatimationFac; 
	private ChoiceGroup stopAllWhileRouting;
	private ChoiceGroup routingOptsGroup;

	private final static Logger logger=Logger.getInstance(GuiDiscover.class,Logger.DEBUG);
	
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

		tfGpxRecordMinimumSecs =new TextField("Minimum seconds between trackpoints (0=disabled)","0",3,TextField.DECIMAL);
		tfGpxRecordMinimumDistanceMeters = new TextField("Minimum meters between trackpoints (0=disabled)","0",3,TextField.DECIMAL);				
		tfGpxRecordAlwaysDistanceMeters = new TextField("Always record when exceeding these meters between trackpoints (0=disabled)","0",3,TextField.DECIMAL);				

		menuRecordingOptions.append(choiceGpxRecordRuleMode);
		menuRecordingOptions.append(tfGpxRecordMinimumSecs);
		menuRecordingOptions.append(tfGpxRecordMinimumDistanceMeters);
		menuRecordingOptions.append(tfGpxRecordAlwaysDistanceMeters);
	}

	private void initRoutingOptions() {
		// Prepare routingOptions menu
		logger.info("Starting Routing setup menu");
		menuRoutingOptions = new Form("Routing Options");
		menuRoutingOptions.addCommand(BACK_CMD);
		menuRoutingOptions.addCommand(OK_CMD);
		menuRoutingOptions.setCommandListener(this);

		String [] routingBack = new String[2];
		routingBack[0] = "No";
		routingBack[1] = "Yes";
		stopAllWhileRouting = new ChoiceGroup("Continue Map while calculation:", Choice.EXCLUSIVE, routingBack ,null);
		stopAllWhileRouting.setSelectedIndex(Configuration.isStopAllWhileRouteing()?0:1,true);
		menuRoutingOptions.append(stopAllWhileRouting);
		gaugeRoutingEsatimationFac=new Gauge("Speed of route calculation", true, 10, Configuration.getRouteEstimationFac());
		menuRoutingOptions.append(gaugeRoutingEsatimationFac);

		String [] routingOpts = new String[1];
		boolean[] selRouting = new boolean[1];
		routingOpts[0] = "Auto Recalculation"; selRouting[0]=Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_AUTO_RECALC);
		routingOptsGroup = new ChoiceGroup("Other", Choice.MULTIPLE, routingOpts ,null);
		routingOptsGroup.setSelectedFlags(selRouting);
		menuRoutingOptions.append(routingOptsGroup);
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
		debugLog = new ChoiceGroup("Debug event logging to:", ChoiceGroup.MULTIPLE,loggings,null);
		debugLog.setSelectedFlags(selDebug);
		menuDebug.append(debugLog);

		loggings = new String[3];
		selDebug = new boolean[3];
		selDebug[0] = Configuration.getDebugSeverityInfo();
		selDebug[1] = Configuration.getDebugSeverityDebug();
		selDebug[2] = Configuration.getDebugSeverityTrace();
		loggings[0] = "Info"; loggings[1] = "Debug"; loggings[2] = "Trace"; 
		debugSeverity = new ChoiceGroup("Log severity:", ChoiceGroup.MULTIPLE,loggings,null);
		debugSeverity.setSelectedFlags(selDebug);
		menuDebug.append(debugSeverity);

		loggings = new String[2];
		loggings[0] = "Show route connections";
		loggings[1] = "Show inconsistent bearings";
		debugOther = new ChoiceGroup("Other:", ChoiceGroup.MULTIPLE,loggings,null);
		debugOther.setSelectedIndex(0, Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS, true));
		debugOther.setSelectedIndex(1, Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_BEARINGS, true));
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

		gpsUrl=new StringItem("GPS: ", null);
		gpsUrl.setDefaultCommand(GPS_DISCOVER);
		gpsUrl.setItemCommandListener(this);
		locProv=new ChoiceGroup("input from:",Choice.EXCLUSIVE,Configuration.LOCATIONPROVIDER,new Image[Configuration.LOCATIONPROVIDER.length]);
		String [] loggings = new String[1];      
		loggings[0] = Configuration.getGpsRawLoggerUrl(); 
		if (loggings[0] == null) { 
			loggings[0] = "Please select to destination first"; 
		} 
		boolean [] selraw = new boolean[1]; 
		selraw[0] = Configuration.getGpsRawLoggerEnable(); 

		rawLog = new ChoiceGroup("Raw gps logging to:", ChoiceGroup.MULTIPLE);

		String [] btka = new String[1];
		btka[0] = "Send keep alives"; 
		btKeepAlive = new ChoiceGroup("BT keep alive",ChoiceGroup.MULTIPLE, btka, null);

		String [] btar = new String[1];
		btar[0] = "Auto reconnect GPS"; 
		btAutoRecon = new ChoiceGroup("BT reconnect",ChoiceGroup.MULTIPLE, btar, null);

		menuSelectLocProv.append(gpsUrl);
		menuSelectLocProv.append(btKeepAlive);
		menuSelectLocProv.append(btAutoRecon);
		menuSelectLocProv.append(locProv);
		menuSelectLocProv.append(rawLog);

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

		menuSelectMapSource.append(mapSrc);
		menuSelectMapSource.setCommandListener(this);
	}

	private void initDisplay() {
		//Prepare Display options menu
		logger.info("Starting display setup menu");

		menuDisplayOptions = new Form("Display Options");

		menuDisplayOptions.addCommand(BACK_CMD);
		menuDisplayOptions.addCommand(OK_CMD);

		String [] rotation = new String[2];
		rotation[0] = "North Up";
		rotation[1] = "to Driving Direction";
		rotationGroup = new ChoiceGroup("Map Rotation", Choice.EXCLUSIVE, rotation ,null);
		menuDisplayOptions.append(rotationGroup);

		String [] renders = new String[2];
		renders[0] = "as lines";
		renders[1] = "as streets";
		renderOpts = new ChoiceGroup("Rendering Options:", Choice.EXCLUSIVE, renders ,null);
		menuDisplayOptions.append(renderOpts);

		// gaugeDetailBoost = new Gauge("Zoom Detail Boost", true, 3, 0);
		// gaugeDetailBoost = new Gauge("Scale Detail Level", true, 3, 0);
		gaugeDetailBoost = new Gauge("Increase Detail of lower Zoom Levels", true, 3, 0);
		menuDisplayOptions.append(gaugeDetailBoost);

		String [] backlights;
		byte i = 3;
		//#if polish.api.nokia-ui
		i += 2;
		//#endif
		//#if polish.api.min-siemapi
		i++;
		//#endif
		backlights = new String[i];

		backlights[0] = "Keep Backlight On";
		backlights[1] = "only in map screen";
		backlights[2] = "with MIDP2.0";
		i = 3;
		//#if polish.api.nokia-ui
		backlights[i++] = "with Nokia API";
		backlights[i++] = "with Nokia Flashlight";
		//#endif
		//#if polish.api.min-siemapi
		backlights[i++] = "with Siemens API";
		//#endif

		backlightOpts = new ChoiceGroup("Backlight Options:", Choice.MULTIPLE, backlights ,null);
		menuDisplayOptions.append(backlightOpts);

		String [] sizes = new String[2];
		sizes[0] = "larger POI labels";
		sizes[1] = "larger waypoint labels";
		sizeOpts = new ChoiceGroup("Size Options:", Choice.MULTIPLE, sizes ,null);
		menuDisplayOptions.append(sizeOpts);

		menuDisplayOptions.setCommandListener(this);
	}
	
	//#if api.polish.osm-editing
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

	public void commandAction(Command c, Item i) {
		// forward item command action to form
		commandAction(c, (Displayable) null);
	}
	
	public void commandAction(Command c, Displayable d) {

		if (c == EXIT_CMD) {
			destroy();
			parent.show();
			return;
		}
		if (c == BACK_CMD) {
			if (state==STATE_BT_GPX) {
				state=STATE_GPX;
			} else if (state==STATE_BT_GPS) {
				state=STATE_LP;
			} else {
				state = STATE_ROOT;
			}
			show();
			return;
		}
		if (c == FILE_MAP) {
			//#if polish.api.fileconnection
			String initialDir="";
			String title="Select Directory";
			switch (state) {
				case STATE_LP:
					title="Raw Log Directory";
					initialDir=rawLog.getString(0);
					break;			
				case STATE_MAP:
					title="Map Directory";					 
					// get initialDir from form 
					String url=mapSrc.getString(1); 
					// skip "Filesystem: " 
					url=url.substring(url.indexOf(":")+2); 
					initialDir=url;
					break;
				case STATE_GPX:
					title="Gpx Directory";
					initialDir=gpxUrl.getText();
					break;
				case STATE_DEBUG:
					title="Log Directory";
					initialDir=Configuration.getDebugRawLoggerUrl();
					break;
			}
			// no valid url, i.e. "Please select to destination first" ? 
			if(initialDir!=null && !initialDir.toLowerCase().startsWith("file:///")) { 
				initialDir=null; 
			} 
			FsDiscover fsd = new FsDiscover(this,this,initialDir,true,"",title);
			fsd.show();						
			//#else
			//logger.error("Files system support is not compiled into this version");
			//#endif	
		}
		if (c == GPS_DISCOVER || c == BT_MAP) {
			//#if polish.api.btapi
			initBluetoothSelect();
			urlList=new Vector();
			friendlyName=new Vector();
			menuBT.deleteAll(); 
			GpsMid.getInstance().show(menuBT); 
			if (state==STATE_LP) {
				logger.info("Discovering a bluetooth serial device");
				state = STATE_BT_GPS;
				gps = new DiscoverGps(this,DiscoverGps.UUDI_SERIAL);
			} else { 
				logger.info("Discovering a bluetooth obex file device");
				state = STATE_BT_GPX;
				gps = new DiscoverGps(this,DiscoverGps.UUDI_FILE);
			} 
			 
			//#else
				logger.error("Bluetooth is not compiled into this version");
			//#endif
		}
		//#if api.polish.osm-editing
		if (c == OSM_URL) {
			gpxUrl.setText(Configuration.getOsmUrl() + "gpx/create");
		}
		//#endif
		if (c == OK_CMD){			
			switch (state) {
			case STATE_ROOT:
				switch (menu.getSelectedIndex()) {
				case MENU_ITEM_LOCATION: // Location Receiver
					initLocationSetupMenu();
					gpsUrlStr=Configuration.getBtUrl();
					gpsUrl.setText(gpsUrlStr==null?"<Discover>":"<Discovered>");
					int selIdx = Configuration.getLocationProvider();
					locProv.setSelectedIndex(selIdx, true);
					
					String logUrl=Configuration.getGpsRawLoggerUrl();		
					if ( logUrl == null) {
						logUrl= "Please select to destination first";
					}
					boolean [] selraw = new boolean[1];
					selraw[0] = Configuration.getGpsRawLoggerEnable();
					rawLog.deleteAll();
					rawLog.append(logUrl, null);
					rawLog.setSelectedFlags(selraw);
					selraw[0] = Configuration.getBtKeepAlive();
					btKeepAlive.setSelectedFlags(selraw);
					selraw[0] = Configuration.getBtAutoRecon();
					btAutoRecon.setSelectedFlags(selraw);
					Display.getDisplay(parent).setCurrentItem(gpsUrl);
					//Display.getDisplay(parent).setCurrent(menuSelectLocProv);
					state = STATE_LP;
					break;
				case MENU_ITEM_GPX_FILTER: // Recording Rules
					initRecordingSetupMenu();
					choiceGpxRecordRuleMode.setSelectedIndex(Configuration.getGpxRecordRuleMode(), true);
					/*
					 * minimum seconds between trackpoints
					 */
					tfGpxRecordMinimumSecs.setString(
						getCleanFloatString( (float)(Configuration.getGpxRecordMinMilliseconds())/1000,3 )
					);				

					/*
					 * minimum meters between trackpoints
					 */
					tfGpxRecordMinimumDistanceMeters.setString(				
						getCleanFloatString( (float)(Configuration.getGpxRecordMinDistanceCentimeters())/100,3 )
					);				

					/*
					 * meters between trackpoints that will always create a new trackpoint
					 */
					tfGpxRecordAlwaysDistanceMeters.setString( 
						getCleanFloatString( (float)(Configuration.getGpxRecordAlwaysDistanceCentimeters())/100,3 )
					);				

					GpsMid.getInstance().show(menuRecordingOptions);
					state = STATE_RECORDING_OPTIONS;
					break;
				case MENU_ITEM_DISP_OPT: // Display Options
					initDisplay();
					rotationGroup.setSelectedIndex(Configuration.getProjDefault(), true);
					renderOpts.setSelectedIndex( Configuration.getCfgBitState(Configuration.CFGBIT_STREETRENDERMODE)?1:0, true);
					sizeOpts.setSelectedIndex(0, Configuration.getCfgBitState(Configuration.CFGBIT_POI_LABELS_LARGER));
					sizeOpts.setSelectedIndex(1, Configuration.getCfgBitState(Configuration.CFGBIT_WPT_LABELS_LARGER));
					SingleTile.newPOIFont();
					WaypointsTile.useNewWptFont();
					gaugeDetailBoost.setValue(Configuration.getDetailBoostDefault());
					// convert bits from backlight flag into selection states
					boolean[] sellight = new boolean[6];
					sellight[0]=Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON, true);
					sellight[1]=Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MAPONLY, true);
					sellight[2]=Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2, true);
					byte i = 3;
					//#if polish.api.nokia-ui
						sellight[i++]=Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA, true);
						sellight[i++]=Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH, true);
					//#endif	
					//#if polish.api.min-siemapi
						sellight[i++]=Configuration.getCfgBitState(Configuration.CFGBIT_BACKLIGHT_SIEMENS, true);
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
					//#if api.polish.osm-editing
					menuGpx.addCommand(OSM_URL);
					//#endif

					gpxUrl = new StringItem("Gpx Receiver Url: ","<Please select in menu>");
					menuGpx.append(gpxUrl);
					menuGpx.setCommandListener(this);
					gpxUrl.setText(Configuration.getGpxUrl()==null?"<Please select in menu>":Configuration.getGpxUrl());
					GpsMid.getInstance().show(menuGpx);
					state = STATE_GPX;
					break;
				case MENU_ITEM_MAP_SRC: // Map Source 
					initMapSource();
					mapSrc.setSelectedIndex(Configuration.usingBuiltinMap()?0:1, true); 
					mapSrc.set(1, "Filesystem: " + ( (Configuration.getMapUrl()==null)?"<Please select map directory first>":Configuration.getMapUrl() ), null);
					GpsMid.getInstance().show(menuSelectMapSource);
					state = STATE_MAP;
					break;			
				case MENU_ITEM_DEBUG_OPT:
					initDebugSetupMenu();
					GpsMid.getInstance().show(menuDebug);
					state = STATE_DEBUG;
					break;
				case MENU_ITEM_ROUTING_OPT:
					initRoutingOptions();
					GpsMid.getInstance().show(menuRoutingOptions);
					state = STATE_ROUTING_OPT;
					break;
				case MENU_ITEM_SOUNDS_OPT:
					GuiSetupSound gs = new GuiSetupSound(this);
					gs.show();
					break;
				case MENU_ITEM_KEYS_OPT:
					/**
					 * Display the current Keyboard mappings for the
					 * Map screen
					 */
					GuiKeyShortcuts gks = new GuiKeyShortcuts(this);
					gks.show();
					break;
				//#if api.polish.osm-editing
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
				}
				break;
			case STATE_BT_GPS: 
				// remember discovered BT URL and put status in form 
				if(urlList.size()!=0) { 
					gpsUrlStr=(String) urlList.elementAt(menuBT.getSelectedIndex()); 
					gpsUrl.setText(gpsUrlStr==null?"<Discover>":"<Discovered>"); 
				} 
				state = STATE_LP; 
				show(); 
				break; 
			case STATE_BT_GPX: 
				// put discovered BT Url in form 
				if(urlList.size()!=0) { 
					String gpxUrlStr=(String) urlList.elementAt(menuBT.getSelectedIndex()); 
					gpxUrl.setText(gpxUrlStr==null?"<Please select in menu>":gpxUrlStr); 
				} 
				state = STATE_GPX; 

				show();
				break;
			case STATE_GPX:
				// Save GpxUrl from form to Configuration
				String str=gpxUrl.getText(); 
				// don't save "Please select..." 
				if (str.indexOf(":")==-1) { 
					str=null; 
				} 
				Configuration.setGpxUrl(str); 
				state = STATE_ROOT; 
				show(); 
				break;           
			case STATE_RECORDING_OPTIONS: 
				String rule; 
				// Save Record Rules to Config 
				Configuration.setGpxRecordRuleMode(choiceGpxRecordRuleMode.getSelectedIndex()); 
				rule=tfGpxRecordMinimumSecs.getString(); 
				Configuration.setGpxRecordMinMilliseconds( 
						rule.length()==0 ? 0 :(int) (1000 * Float.parseFloat(rule)) 
				); 
				rule=tfGpxRecordMinimumDistanceMeters.getString(); 
				Configuration.setGpxRecordMinDistanceCentimeters( 
						rule.length()==0 ? 0 :(int) (100 * Float.parseFloat(rule)) 
				); 
				rule=tfGpxRecordAlwaysDistanceMeters.getString(); 
				Configuration.setGpxRecordAlwaysDistanceCentimeters( 
						rule.length()==0 ? 0 :(int) (100 * Float.parseFloat(rule)) 
				); 

				state = STATE_ROOT;
				show();
				break;			
			case STATE_LP:				
				Configuration.setBtUrl(gpsUrlStr); 
				Configuration.setLocationProvider(locProv.getSelectedIndex()); 
				boolean [] selraw = new boolean[1];
				rawLog.getSelectedFlags(selraw);
				// check if url is valid, i.e. not "Please select..."  
				if (rawLog.getString(0).indexOf(":")!=-1) { 
					Configuration.setGpsRawLoggerUrl(rawLog.getString(0)); 
					Configuration.setGpsRawLoggerEnable(selraw[0]);
				} else {
					Configuration.setGpsRawLoggerUrl(null); 
					Configuration.setGpsRawLoggerEnable(false);					
				}
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
				String url=mapSrc.getString(1); 
				// skip "Filesystem: " 
				url=url.substring(url.indexOf(":")+2); 
				// no valid url, i.e. "Please select..." 
				if (url.indexOf(":")==-1) { 
					url=null; 
				} 
				Configuration.setMapUrl(url); 
				state = STATE_ROOT;
				this.show();
				logger.fatal("Need to restart GpsMid, otherwise map is in an inconsistant state");
				break;			
			case STATE_DISPOPT:
				Configuration.setProjTypeDefault( (byte) rotationGroup.getSelectedIndex() );
				Configuration.setCfgBitState(Configuration.CFGBIT_STREETRENDERMODE,
						(renderOpts.getSelectedIndex()==1),
						true); 
				Configuration.setCfgBitState(Configuration.CFGBIT_POI_LABELS_LARGER, sizeOpts.isSelected(0), true);
				Configuration.setCfgBitState(Configuration.CFGBIT_WPT_LABELS_LARGER, sizeOpts.isSelected(1), true);
				Configuration.setDetailBoost(gaugeDetailBoost.getValue(), true); 
				
				// convert boolean array with selection states for backlight
				// to one flag with corresponding bits set
				boolean[] sellight = new boolean[6];
				backlightOpts.getSelectedFlags( sellight );
	            // save selected values to record store
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_ON, sellight[0], true);
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_MAPONLY, sellight[1], true);
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_MIDP2, sellight[2], true);
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIA , sellight[3], true);
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_NOKIAFLASH , sellight[4], true);
				Configuration.setCfgBitState(Configuration.CFGBIT_BACKLIGHT_SIEMENS , sellight[5], true);
				state = STATE_ROOT;
				show();

				parent.stopBackLightTimer();				
				parent.startBackLightTimer();			
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
				Configuration.setCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS, debugOther.isSelected(0), true);
				Configuration.setCfgBitState(Configuration.CFGBIT_ROUTE_BEARINGS, debugOther.isSelected(1), true);
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

			case STATE_ROUTING_OPT:
				Configuration.setRouteEstimationFac(gaugeRoutingEsatimationFac.getValue());
				logger.debug("set stopAllWhileRounting " + stopAllWhileRouting.isSelected(1));
				Configuration.setStopAllWhileRouteing(stopAllWhileRouting.isSelected(0));
				boolean[] selRouting = new boolean[1];
				routingOptsGroup.getSelectedFlags(selRouting);
				Configuration.setCfgBitState(Configuration.CFGBIT_ROUTE_AUTO_RECALC, selRouting[0], true);
				state = STATE_ROOT;
				this.show();			
				break;
			//#if api.polish.osm-editing
			case STATE_OSM_OPT:
				Configuration.setOsmUsername(tfOsmUserName.getString());
				Configuration.setOsmPwd(tfOsmPassword.getString());
				Configuration.setOsmUrl(tfOsmUrl.getString());
				state = STATE_ROOT;
				this.show();
				break;
			//#endif
			}
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
			if (sb.charAt(sb.length() - 1) == '.')
				hasDecimalPoint = false;
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
				GpsMid.getInstance().show(menu);
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
	public void addDevice(String url,String name) {
//		menuBT.append("add " + name,null);
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
		for (int i=0; i < friendlyName.size(); i++){
			try {
				menuBT.append(""+i + " "+(String) friendlyName.elementAt(i), null);
			} catch (RuntimeException e) {
				// TODO Auto-generated catch block
				menuBT.append(e.getMessage(), null);
			}
		}
		
	}
	
	public void selectionCanceled() {
		/**
		 * Not yet used
		 */
	}

	public void selectedFile(String url) {
		logger.info("Url selected: " + url);
		url = url.substring(0, url.lastIndexOf('/') + 1);
		switch (state) {
		case STATE_LP:
			rawLog.set(0, url, null);
			break;
		case STATE_MAP:
			mapSrc.set(1, "Filesystem: " + url, null);
			mapSrc.setSelectedIndex(1, true);
			//As the Filesystem chooser has called the show()
			//method of this class, it currently shows the root
			//menu, but we want't to continue to edit the MapSource
			//menue
			Display.getDisplay(parent).setCurrent(menuSelectMapSource);
			break;
		case STATE_GPX:
			gpxUrl.setText(url);				
			break;
		case STATE_DEBUG:
			url = url.substring(0, url.lastIndexOf('/') + 1);
			debugLog.set(0, url, null);						
			break;
		}		
	}
}
