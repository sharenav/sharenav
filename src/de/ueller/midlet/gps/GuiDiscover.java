package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Vector;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

public class GuiDiscover implements CommandListener {

	/** A menu list instance */
	private static final String[]	elements		= { "Discover GPS",
			"Setup Database"						};

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

	private final List				menuBT			= new List("Devices",
															Choice.IMPLICIT, empty,
															null);

	private final List				menuFS			= new List("Devices",
															Choice.IMPLICIT, empty,
															null);

	private final GpsMid			parent;

	private DiscoverGps				gps;

	private int						state;

	private final static int		STATE_ROOT		= 0;

	private final static int		STATE_FS		= 1;

	private final static int		STATE_BT		= 2;
	private Vector urlList; 
	private Vector friendlyName;

	public GuiDiscover(GpsMid parent) {
		this.parent = parent;
		
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		menuBT.addCommand(BACK_CMD);
		menuBT.setCommandListener(this);
		menuFS.addCommand(BACK_CMD);
		menuFS.setCommandListener(this);
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
		if (c == STORE_BT_URL) {
			parent.setBTUrl(menu.getString(menu.getSelectedIndex()));
			return;
		}
		switch (state) {
			case STATE_ROOT:
				switch (menu.getSelectedIndex()) {
					case 0:
//						gps.cancelDeviceSearch();
						menuBT.setTitle("Search Service");
						menuBT.deleteAll();
						Display.getDisplay(parent).setCurrent(menuBT);
						state = STATE_BT;
						gps = new DiscoverGps(this);
						break;
					case 1:
						menuFS.setTitle("Search Root FSs");
						state = STATE_FS;
						Display.getDisplay(parent).setCurrent(menuFS);
						new FsDiscover(this);
						break;
				}
				break;
			case STATE_BT:
				parent.setBTUrl((String) urlList.elementAt(menuBT.getSelectedIndex()));
				parent.show();
				break;
			case STATE_FS:
				parent.setRootFs(menuFS.getString(menuFS.getSelectedIndex()));
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
		menuBT.addCommand(STORE_BT_URL);
		menuBT.setTitle("Select Device");
	}

	/** Shows main menu of MIDlet on the screen. */
	void show() {
		state = STATE_ROOT;
		Display.getDisplay(parent).setCurrent(menu);
	}

	public void addDevice(String s) {
		
	}
	public void addDevice(String url,String name) {
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
		for (int i=0; i < friendlyName.size(); i++){
			menuBT.append(""+i + " "+(String) friendlyName.elementAt(i), null);
		}
		
	}
}
