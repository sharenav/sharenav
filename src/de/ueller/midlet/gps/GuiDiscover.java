package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Vector;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.options.OptionsRender;

public class GuiDiscover implements CommandListener, GpsMidDisplayable {

	/** A menu list instance */
	private static final String[]	elements		= { "Input options","Discover GPS","Render options",
			"GPX reciever"						};

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

	/** A menu list instance */
	private final List				menu			= new List("Devices",
															Choice.IMPLICIT, elements,
															null);

	private  List				menuBT			= new List("Devices",
															Choice.IMPLICIT, empty,
															null);

	private final List				menuFS			= new List("Devices",
															Choice.IMPLICIT, empty,
															null);
	private final Form				menuSelectLocProv = new Form("Select Location Provider");

	private final GpsMid			parent;

	private DiscoverGps				gps;

	private int						state;

	private final static int		STATE_ROOT		= 0;

	private final static int		STATE_FS		= 1;

	private final static int		STATE_BT		= 2;
	private final static int		STATE_LP		= 3;
	private final static int		STATE_RBT		= 4;
	private Vector urlList; 
	private Vector friendlyName;
	ChoiceGroup locProv;
	String[] devices={"None","SIRF GPS","NEMA GPS"
			//#if polish.api.locationapi
			,"JSR179"
			//#endif
			};
	int[] devicesSaveid={3,0,1
			//#if polish.api.locationapi
			,2
			//#endif
			};
	

	public GuiDiscover(GpsMid parent) {
		this.parent = parent;
		
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		menuFS.addCommand(BACK_CMD);
		menuFS.setCommandListener(this);
		menuSelectLocProv.addCommand(BACK_CMD);
		menuSelectLocProv.addCommand(OK_CMD);
		locProv=new ChoiceGroup("input from:",Choice.EXCLUSIVE,devices,new Image[devices.length]);
		int selIdx=Configuration.LOCATIONPROVIDER_NONE;
		for (int i=0;i<devices.length;i++){
			if (devicesSaveid[i]==parent.getConfig().getLocationProvider()){
				selIdx=i;
			}
		}
		locProv.setSelectedIndex(selIdx, true);
		menuSelectLocProv.append(locProv);
		menuSelectLocProv.setCommandListener(this);
		show();
	}

	public void commandAction(Command c, Displayable d) {
		if (c == EXIT_CMD) {
			destroy();
			parent.show();
			return;
		}
		if (c == BACK_CMD) {
			show();
			return;
		}
//		if (c == STORE_BT_URL) {
//			parent.getConfig().setBtUrl((String) urlList.elementAt(menu.getSelectedIndex()));
//			return;
//		}
		if (c == OK_CMD){
			switch (state){
			case STATE_LP:
				parent.getConfig().setLocationProvider(devicesSaveid[locProv.getSelectedIndex()]);
			}
			show();
		}
		switch (state) {
			case STATE_ROOT:
				switch (menu.getSelectedIndex()) {
					case 0:
						Display.getDisplay(parent).setCurrent(menuSelectLocProv);
						state = STATE_LP;
						break;
					case 1:
//						gps.cancelDeviceSearch();
						menuBT	= new List("Devices",
								Choice.IMPLICIT, empty,
								null);
						menuBT.addCommand(BACK_CMD);
						menuBT.setCommandListener(this);
						menuBT.setTitle("Search Service");
						urlList=new Vector();
						friendlyName=new Vector();
						Display.getDisplay(parent).setCurrent(menuBT);
						state = STATE_BT;
						
						gps = new DiscoverGps(this,DiscoverGps.UUDI_SERIAL);
						break;
					case 2:
						OptionsRender render = new OptionsRender(this,parent.getConfig());
						Display.getDisplay(parent).setCurrent(render);
						break;
					case 3:
						menuBT	= new List("Devices",
								Choice.IMPLICIT, empty,
								null);
						menuBT.addCommand(BACK_CMD);
						menuBT.setCommandListener(this);
						menuBT.setTitle("Search Service");
						urlList=new Vector();
						friendlyName=new Vector();
						Display.getDisplay(parent).setCurrent(menuBT);
						state = STATE_RBT;
						
						gps = new DiscoverGps(this,DiscoverGps.UUDI_FILE);
						break;
				}
				break;
			case STATE_BT:
				parent.getConfig().setBtUrl((String) urlList.elementAt(menuBT.getSelectedIndex()));
				parent.show();
				break;
			case STATE_RBT:
				parent.getConfig().setGpxUrl((String) urlList.elementAt(menuBT.getSelectedIndex()));
				parent.show();
				break;
//			case STATE_FS:
//				parent.setRootFs(menuFS.getString(menuFS.getSelectedIndex()));
//				break;
			case STATE_LP:
				break;
		}
	}

	private void destroy() {
		if (gps != null) {
			gps.destroy();
		}

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
		state = STATE_ROOT;
		Display.getDisplay(parent).setCurrent(menu);
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
}
