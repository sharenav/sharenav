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


import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.options.OptionsRender;

public class GuiDiscover implements CommandListener, GpsMidDisplayable, SelectionListener {

	/** A menu list instance */
	private static final String[]	elements		= { "Location Receiver","Discover GPS","Display options",
			"GPX Receiver", "Map source", "Debug options"};

	private static final String[]	empty			= {};

	/** Soft button for exiting to RootMenu. */
	private final Command			EXIT_CMD		= new Command("Back",
															Command.BACK, 2);

	private final Command			BACK_CMD		= new Command("Back",
															Command.BACK, 2);

	/** Soft button for discovering BT. */
	private final Command			OK_CMD			= new Command("Ok",
															Command.ITEM, 1);

	private final Command			STORE_BT_URL	= new Command("Select",
															Command.OK, 2);

	private final Command			STORE_ROOTFS	= new Command("Select",
															Command.OK, 2);
	
	private final Command			FILE_MAP	= new Command("Select Directory",
			Command.ITEM, 2);
	private final Command			BT_MAP	= new Command("Select bluetooth device",
			Command.ITEM, 2);

	/** A menu list instance */
	private final List				menu			= new List("Setup",
															Choice.IMPLICIT, elements,
															null);

	private  List				menuBT			= new List("Devices",
															Choice.IMPLICIT, empty,
															null);

	private final List				menuFS			= new List("Devices",
															Choice.IMPLICIT, empty,
															null);
	private final Form				menuSelectLocProv = new Form("Location Receiver");
	
	private final Form				menuSelectMapSource = new Form("Select Map Source");
	
	private final Form				menuDisplayOptions = new Form("Display Options");
	
	private final Form				menuGpx = new Form("Gpx Receiver");
	
	private final Form				menuDebug = new Form("Debug options");

	private final GpsMid			parent;

	private DiscoverGps				gps;

	private int						state;

	private final static int		STATE_ROOT		= 0;
	private final static int		STATE_BT		= 2;
	private final static int		STATE_LP		= 3;
	private final static int		STATE_RBT		= 4;
	private final static int		STATE_GPX		= 5;
	private final static int		STATE_MAP		= 6;
	private final static int		STATE_DISPOPT	= 7;
	private final static int		STATE_DEBUG		= 8;
	
	private Vector urlList; 
	private Vector friendlyName;
	private ChoiceGroup locProv;
	private ChoiceGroup rawLog;
	private ChoiceGroup mapSrc;
	private ChoiceGroup renderOpts;
	private ChoiceGroup backlightOpts;
	private ChoiceGroup debugLog;
	private StringItem  gpxUrl;

	
	/*String[] devices={"None","SIRF GPS","NEMA GPS"
			//#if polish.api.locationapi
			,"JSR179"
			//#endif
			};
	int[] devicesSaveid={3,0,1
			//#if polish.api.locationapi
			,2
			//#endif
			};
	*/

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
		
		//Prepare Location Provider setup menu
		menuSelectLocProv.addCommand(BACK_CMD);
		menuSelectLocProv.addCommand(OK_CMD);
		menuSelectLocProv.addCommand(FILE_MAP);
		
		locProv=new ChoiceGroup("input from:",Choice.EXCLUSIVE,Configuration.LOCATIONPROVIDER,new Image[Configuration.LOCATIONPROVIDER.length]);
		int selIdx=Configuration.LOCATIONPROVIDER_NONE;
		selIdx = parent.getConfig().getLocationProvider();
		/*for (int i=0;i<devices.length;i++){
			if (devicesSaveid[i]==parent.getConfig().getLocationProvider()){
				selIdx=i;
			}
		}*/
		locProv.setSelectedIndex(selIdx, true);
		String [] loggings = new String[1];		
		loggings[0] = GpsMid.getInstance().getConfig().getGpsRawLoggerUrl();
		if (loggings[0] == null) {
			loggings[0] = "Please select to destination first";
		}
		boolean [] selraw = new boolean[1];
		selraw[0] = GpsMid.getInstance().getConfig().getGpsRawLoggerEnable();
		rawLog = new ChoiceGroup("Raw gps logging to:", ChoiceGroup.MULTIPLE,loggings,null);
		rawLog.setSelectedFlags(selraw);
		menuSelectLocProv.append(locProv);
		menuSelectLocProv.append(rawLog);
		menuSelectLocProv.setCommandListener(this);
		
		//Prepare Map Source selection menu
		menuSelectMapSource.addCommand(BACK_CMD);
		menuSelectMapSource.addCommand(OK_CMD);
		menuSelectMapSource.addCommand(FILE_MAP);
		String [] sources = new String[2];
		sources[0] = "Built-in map";
		sources[1] = "Filesystem: " + GpsMid.getInstance().getConfig().getMapUrl();		
		mapSrc = new ChoiceGroup("Map source:", Choice.EXCLUSIVE, sources, null);
		mapSrc.setSelectedIndex(GpsMid.getInstance().getConfig().usingBuiltinMap()?0:1, true);
		menuSelectMapSource.append(mapSrc);
		menuSelectMapSource.setCommandListener(this);
		
		//Prepare Display options menu
		menuDisplayOptions.addCommand(BACK_CMD);
		menuDisplayOptions.addCommand(OK_CMD);
		String [] renders = new String[2];
		renders[0] = "as lines";
		renders[1] = "as streets";
		renderOpts = new ChoiceGroup("Rendering Options:", Choice.EXCLUSIVE, renders ,null);
		menuDisplayOptions.append(renderOpts);
		//#if polish.api.nokia-ui
		String [] backlights = new String[5];
		//#else
		String [] backlights = new String[3];
		//#endif
		backlights[Configuration.BACKLIGHT_ON] = "Keep Backlight On";
		backlights[Configuration.BACKLIGHT_MAPONLY] = "only in map screen";
		backlights[Configuration.BACKLIGHT_MIDP2] = "with MIDP2.0";
		//#if polish.api.nokia-ui
		backlights[Configuration.BACKLIGHT_NOKIA] = "with Nokia API";
		backlights[Configuration.BACKLIGHT_NOKIAFLASH] = "with Nokia Flashlight";
		//#endif
		backlightOpts = new ChoiceGroup("Backlight Options:", Choice.MULTIPLE, backlights ,null);
		menuDisplayOptions.append(backlightOpts);

		
		
		menuDisplayOptions.setCommandListener(this);
		
		//Prepare Gpx receiver selection menu
		menuGpx.addCommand(BACK_CMD);
		menuGpx.addCommand(OK_CMD);
		menuGpx.addCommand(FILE_MAP);
		menuGpx.addCommand(BT_MAP);
		menuGpx.setCommandListener(this);
		gpxUrl = new StringItem("GpxUrl: ", GpsMid.getInstance().getConfig().getGpxUrl());
		menuGpx.append(gpxUrl);
		
		//Prepare Bluetooth selection menu
		menuBT	= new List("Devices",
				Choice.IMPLICIT, empty,
				null);
		menuBT.addCommand(OK_CMD);
		menuBT.addCommand(BACK_CMD);
		menuBT.setSelectCommand(OK_CMD);
		menuBT.setCommandListener(this);
		menuBT.setTitle("Search Service");
		
		//Prepare Debug selection menu
		menuDebug.addCommand(BACK_CMD);
		menuDebug.addCommand(OK_CMD);
		menuDebug.addCommand(FILE_MAP);
		menuDebug.setCommandListener(this);
		boolean [] selDebug = new boolean[1];
		selDebug[0] = GpsMid.getInstance().getConfig().getDebugRawLoggerEnable();
		loggings = new String[1];
		loggings[0] = GpsMid.getInstance().getConfig().getDebugRawLoggerUrl();
		if (loggings[0] == null) {
			loggings[0] = "Please select directory";
		}
		debugLog = new ChoiceGroup("Debug event logging to:", ChoiceGroup.MULTIPLE,loggings,null);
		debugLog.setSelectedFlags(selDebug);
		menuDebug.append(debugLog);
		
		show();
	}

	public void commandAction(Command c, Displayable d) {
		if (c == EXIT_CMD) {
			destroy();
			parent.show();
			return;
		}
		if (c == BACK_CMD) {
			state = STATE_ROOT;
			show();
			return;
		}
//		if (c == STORE_BT_URL) {
//			parent.getConfig().setBtUrl((String) urlList.elementAt(menu.getSelectedIndex()));
//			return;
//		}
		if (c == FILE_MAP) {
			//#if polish.api.fileconnection
			String initialDir="";
			String title="Select Directory";
			switch (state) {
				case STATE_LP:
					title="Raw Log Directory";
					initialDir=parent.getConfig().getGpsRawLoggerUrl();
					break;			
				case STATE_MAP:
					title="Map Directory";
					initialDir=GpsMid.getInstance().getConfig().getMapUrl();
					break;
				case STATE_GPX:
					title="Gpx Directory";
					initialDir=GpsMid.getInstance().getConfig().getGpxUrl();
					break;
				case STATE_DEBUG:
					title="Log Directory";
					initialDir=GpsMid.getInstance().getConfig().getDebugRawLoggerUrl();
					break;
			}
			FsDiscover fsd = new FsDiscover(this,this,initialDir,true,"",title);
			fsd.show();						
			//#else
			//logger.error("Files system support is not compiled into this version");
			//#endif	
		}
		if (c == BT_MAP) {
			//#if polish.api.btapi			
			urlList=new Vector();
			friendlyName=new Vector();
			Display.getDisplay(parent).setCurrent(menuBT);			
			gps = new DiscoverGps(this,DiscoverGps.UUDI_FILE);
			//#else
			logger.error("Bluetooth is not compiled into this version");
			//#endif
		}
		
		if (c == OK_CMD){			
			switch (state) {
			case STATE_ROOT:
				switch (menu.getSelectedIndex()) {
				case 0:
					Display.getDisplay(parent).setCurrent(menuSelectLocProv);
					state = STATE_LP;
					break;
				case 1:
					//#if polish.api.btapi
					//						gps.cancelDeviceSearch();
					
					urlList=new Vector();
					friendlyName=new Vector();
					Display.getDisplay(parent).setCurrent(menuBT);
					state = STATE_BT;
					gps = new DiscoverGps(this,DiscoverGps.UUDI_SERIAL);
					//#else
					logger.error("Bluetooth is not compiled into this version");
					//#endif

					break;
				case 2:
					/*OptionsRender render = new OptionsRender(this,parent.getConfig());
					Display.getDisplay(parent).setCurrent(render);
					break;*/
					renderOpts.setSelectedIndex(GpsMid.getInstance().getConfig().getRender(), true);
					// convert bits from backlight flag into selection states
					boolean[] sellight = new boolean[ Configuration.BACKLIGHT_OPTIONS_COUNT ];
	                int backlight=GpsMid.getInstance().getConfig().getBacklight();	                
	                for (int i=0;i<Configuration.BACKLIGHT_OPTIONS_COUNT;i++) {
	                	if ((backlight & (1<<i)) !=0) {
	                		sellight[i]=true;
	                	}
	                }
					backlightOpts.setSelectedFlags(sellight);
					
					Display.getDisplay(parent).setCurrent(menuDisplayOptions);
					state = STATE_DISPOPT;
					break;
				case 3:
					gpxUrl.setText(GpsMid.getInstance().getConfig().getGpxUrl());
					Display.getDisplay(parent).setCurrent(menuGpx);
					state = STATE_GPX;
					break;
					/*
					//#if polish.api.btapi
					menuBT	= new List("Devices",
							Choice.IMPLICIT, empty,
							null);
					menuBT.addCommand(BACK_CMD);
					menuBT.addCommand(OK_CMD);
					menuBT.setSelectCommand(OK_CMD);
					menuBT.setCommandListener(this);
					menuBT.setTitle("Search Service");
					urlList=new Vector();
					friendlyName=new Vector();
					Display.getDisplay(parent).setCurrent(menuBT);
					state = STATE_RBT;

					gps = new DiscoverGps(this,DiscoverGps.UUDI_FILE);
					//#endif
					break;
				case 4:
					//#if polish.api.fileconnection
					FsDiscover fsd = new FsDiscover(this,this);
					fsd.show();						
					//#else
					//logger.error("Files system support is not compiled into this version");
					//#endif
					break;*/
				case 4:
					mapSrc.setSelectedIndex(GpsMid.getInstance().getConfig().usingBuiltinMap()?0:1, true);
					Display.getDisplay(parent).setCurrent(menuSelectMapSource);
					state = STATE_MAP;
					break;			
				case 5:					
					Display.getDisplay(parent).setCurrent(menuDebug);
					state = STATE_DEBUG;
					break;
				}
				break;
			case STATE_BT:
				parent.getConfig().setBtUrl((String) urlList.elementAt(menuBT.getSelectedIndex()));
				state = STATE_ROOT;
				show();
				break;
			case STATE_GPX:
				parent.getConfig().setGpxUrl((String) urlList.elementAt(menuBT.getSelectedIndex()));
				gpxUrl.setText((String) urlList.elementAt(menuBT.getSelectedIndex()));
				state = STATE_ROOT;
				show();
				break;			
			case STATE_LP:
				parent.getConfig().setLocationProvider(locProv.getSelectedIndex());
				boolean [] selraw = new boolean[1];
				rawLog.getSelectedFlags(selraw);
				if (selraw[0] && !(rawLog.getString(0).equalsIgnoreCase("Please select to destination first"))) {
					parent.getConfig().setGpsRawLoggerUrl(rawLog.getString(0));
					parent.getConfig().setGpsRawLoggerEnable(true);
				} else {
					parent.getConfig().setGpsRawLoggerEnable(false);					
				}				
				state = STATE_ROOT;
				show();
				break;
			case STATE_MAP:
				GpsMid.getInstance().getConfig().setBuiltinMap((mapSrc.getSelectedIndex() == 0));
				state = STATE_ROOT;
				this.show();
				logger.fatal("Need to restart GpsMid, otherwise map is in an inconsistant state");
				break;			
			case STATE_DISPOPT:
				parent.getConfig().setRender(renderOpts.getSelectedIndex());

				// convert boolean array with selection states for backlight
				// to one flag with corresponding bits set
				boolean[] sellight = new boolean[ Configuration.BACKLIGHT_OPTIONS_COUNT ];
                backlightOpts.getSelectedFlags( sellight );
                int backlight=0;
                for (int i=0;i<Configuration.BACKLIGHT_OPTIONS_COUNT;i++) {
                	if (sellight[i]) {
                		backlight|=1<<i;
                	}
                }

				logger.info("Backlight Options:" + backlight);

                // value saved into recordstore as startup default
                parent.getConfig().setBacklightDefault(backlight);

				// value used by Backlight timer -
				// this value is toggleable using key on map screen
				parent.getConfig().setBacklight(backlight);
								
				state = STATE_ROOT;
				show();

				parent.stopBackLightTimer();				
				parent.startBackLightTimer();			
				break;
			case STATE_DEBUG:
				boolean [] selDebug = new boolean[1];
				debugLog.getSelectedFlags(selDebug);				
				GpsMid.getInstance().getConfig().setDebugRawLoggerEnable((selDebug[0]));
				GpsMid.getInstance().getConfig().setDebugRawLoggerUrl(debugLog.getString(0));
				state = STATE_ROOT;
				this.show();			
				break;
			}
		
		}
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

	/** Shows main menu of MIDlet on the screen. */
	public void show() {
		switch (state) {
			case STATE_ROOT:
				Display.getDisplay(parent).setCurrent(menu);
				break;
			case STATE_LP:
				Display.getDisplay(parent).setCurrent(menuSelectLocProv);
				break;
			case STATE_MAP:
				Display.getDisplay(parent).setCurrent(menuSelectMapSource);
			case STATE_GPX:
				Display.getDisplay(parent).setCurrent(menuGpx);
			case STATE_DEBUG:
				Display.getDisplay(parent).setCurrent(menuDebug);
				
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

	public void selectedFile(String url) {
		logger.info("Url selected: " + url);
		switch (state) {
		case STATE_LP:
			url = url.substring(0, url.lastIndexOf('/') + 1);
			rawLog.set(0, url, null);
			break;			
		case STATE_MAP:
			url = url.substring(0, url.lastIndexOf('/') + 1);
			GpsMid.getInstance().getConfig().setMapUrl(url);
			//As the Filesystem chooser has called the show()
			//method of this class, it currently shows the root
			//menu, but we want't to continue to edit the MapSource
			//menue
			Display.getDisplay(parent).setCurrent(menuSelectMapSource);
			mapSrc.set(1, "Filesystem: " + GpsMid.getInstance().getConfig().getMapUrl(), null);
			break;
		case STATE_GPX:
			url = url.substring(0, url.lastIndexOf('/') + 1);
			parent.getConfig().setGpxUrl(url);			
			break;
		case STATE_DEBUG:
			url = url.substring(0, url.lastIndexOf('/') + 1);
			debugLog.set(0, url, null);						
			break;
		}		
	}
}
