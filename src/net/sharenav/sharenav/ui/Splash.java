/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package net.sharenav.sharenav.ui;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.midlet.util.ImageTools;

import java.io.IOException;
//#if polish.android
import android.content.Context;
import android.content.pm.ApplicationInfo;
import de.enough.polish.android.midlet.MidletBridge;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
//#endif
import java.util.Date;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.Image;

import de.enough.polish.util.Locale;

//#if polish.android
import android.view.KeyEvent;
//#endif

public class Splash extends Canvas implements CommandListener,Runnable{
	private Image splash;
	/** Soft button to go back from about screen. */
	private final Command SELECT_CMD = new Command(Locale.get("splash.select"), Command.OK, 1);
	private final Command BACK_CMD = new Command(Locale.get("splash.Accept"), Command.OK, 2);
	private final Command ENGLISH_CMD = new Command("Switch to English", Command.ITEM, 3);
	private final Command ENABLE_CMD = new Command(Locale.get("splash.enable"), Command.ITEM, 3);
	private final Command DISABLE_CMD = new Command(Locale.get("splash.disable"), Command.ITEM, 3);
	private final Command EXIT_CMD = new Command(Locale.get("splash.Deny"), Command.EXIT, 1);
	private final ShareNav main;
	String[] txt = {
		Locale.get("splash.Discl1")/*Disclaimer:*/,
		Locale.get("splash.Discl2")/* No warranty, no liability.*/,
		Locale.get("splash.Discl3")/* Don't handle while driving!*/,
		Locale.get("splash.Discl4")/* Only use if legally allowed*/,
		Locale.get("splash.Discl5")/* to use the features under*/,
		Locale.get("splash.Discl6")/* the laws applicable for you.*/,
		Locale.get("splash.CodeForGpsMid")/*Based on code developed for GpsMid,*/,
		Locale.get("splash.GpsMidUrl")/*http://gpsmid.sourceforge.net*/,
		Locale.get("splash.Copyright")/*Copyright:*/,
		" Harald Mueller",
		" S. Hochmuth",
		" Kai Krueger",
		" Jyrki Kuoppala",
		Locale.get("splash.MarkusBaeurle"),
		// Locale.get("splash.artwork")/* Artwork (splash screen): */,
		// " Tobias Mueller",
		Locale.get("splash.contributors")/*and many other contributors.*/,
		Locale.get("splash.codefrom")/*Includes code by:*/, 
		" Sualeh Fatehi ",
		" Nikolay Klimchuk",
		" Simon Turner",
		"Sun Microsystems, Inc",
		Locale.get("splash.MuellerHoenicke"),
		Locale.get("splash.Application")/*Application:*/,
		Locale.get("splash.Application2")/* licensed under GPL2*/,
		Locale.get("splash.Application3")/* http://www.gnu.org/*/,
		Locale.get("splash.MapData")/*Map data:*/,
		Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_OSM_ODBL) ?
		Locale.get("trace.mapcreditOsmODbL")
		:
		(
		Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_FI_LANDSURVEY) ?
		Locale.get("trace.mapcreditFiLandSurvey12")
		:
		Locale.get("splash.MapData2")
		),
		Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_OSM_ODBL) ?
		""
		:
		(
		Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_FI_LANDSURVEY) ?
		""
		:
		Locale.get("splash.MapData3")/* licensed under CC 2.0*/
		),
		Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_OSM_ODBL) ?
		Locale.get("trace.mapcreditOsmODbLURL")
		:
		(
		Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_FI_LANDSURVEY) ?
		Locale.get("trace.mapcreditFiLandSurvey12URL")
		:
		Locale.get("splash.MapData4")/* http://creativecommons.org/*/
		),
		Locale.get("splash.skip")/* Press '*' to skip this */,
		Locale.get("splash.screen")/* screen at startup. */,
		"Press '#' if you want to",
		"switch to English." };
	private final Font f;
	int top = 0;
	private final Thread processorThread;
	private boolean shutdown = false;
	private final int ssize;
	private int topStart = 106;
	private final int space;
	private double scale = 1;
	private String mapVersion; 
	private String appVersion; 
	private boolean initDone = false;
	
	private long lastKeyPressTime = 0;
	private int lastKeyCode;

	List menuSplash = new List("ShareNav", List.IMPLICIT);

	public Splash(ShareNav main, boolean initDone) {
		this.main = main;
		this.initDone = initDone;

		Configuration.setHasPointerEvents(hasPointerEvents());

		// FIXME add splash image
		//#if false
		try {
			splash = Image.createImage("/Gps-splash.png");
		} catch (IOException e) {
			e.printStackTrace();
			splash = Image.createImage(176, 220);
		}
		//#else
		splash = Image.createImage(176, 220);
		//#endif
		if (splash.getWidth() < getWidth() ) {
			double scaleW = (double) getWidth() / (double) splash.getWidth();
			double scaleH = (double) getHeight() / (double) splash.getHeight();
			scale = scaleH;
			if (scaleW < scaleH) {
				scale = scaleW;
			}
	    	// if we would not be able to allocate memory for
			// at least the memory for the original and the scaled image
			// plus 25% do not scale
			int newWidth =  (int)(scale* splash.getWidth());
			int newHeight = (int)(scale* splash.getHeight());
			if (ImageTools.isScaleMemAvailable(splash, newWidth, newHeight)) {
				splash = ImageTools.scaleImage(splash, newWidth, newHeight);
			}
			if (splash.getWidth() != newWidth) {
				scale = 1;
			}
			topStart *= scale;
		}
	
		f = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL);
		space = getHeight() - topStart;
		ssize = f.getHeight() * txt.length + space;
		top = -space;
		if (Legend.isValid) {
			mapVersion = "M" + Legend.getMapVersion() + " (" + Legend.getBundleDate() + ")";
		}
		//#if polish.android
		try {
			ApplicationInfo ai = MidletBridge.instance.getPackageManager().getApplicationInfo(MidletBridge.instance.getPackageName(), 0);
			ZipFile zf = new ZipFile(ai.sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			long time = ze.getTime();
			String s = SimpleDateFormat.getInstance().format(new java.util.Date(time));
			appVersion = "V" + Legend.getAppVersion() + "(" + s + ")";
		} catch (Exception e) {
			appVersion = "V" + Legend.getAppVersion();
			System.out.println("Getting modification time: exception: " + e);
		}
		//String jarDate = url.openConnection().getLastModified();
		//appVersion = "V" + Legend.getAppVersion() + "(" + jarDate + ")";
		//#else
		appVersion = "V" + Legend.getAppVersion();
		//#endif
		addCommand(BACK_CMD);
		//addCommand(EXIT_CMD);
		//addCommand(ENGLISH_CMD);
		//addCommand(ENABLE_CMD);
		//addCommand(DISABLE_CMD);
		if (!initDone) {
			addCommand(EXIT_CMD);
		}
		setCommandListener(this);
		show();
		processorThread = new Thread(this, "Splash");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}

	protected void paint(Graphics g) {
		// cleans the screen
		g.setColor(150, 200, 250);
		g.setFont(f);
		g.fillRect(0, 0, getWidth(), getHeight());
		int sp = f.getHeight();
		int x = (getWidth() + splash.getWidth()) / 2 - 2;
		
		g.drawImage(splash, getWidth() / 2, 0, Graphics.HCENTER | Graphics.TOP);

		g.setColor(0xFFFF99);
		g.drawString(appVersion + " " + mapVersion, x , 2, Graphics.TOP | Graphics.RIGHT);		
		if (Math.abs( System.currentTimeMillis() - lastKeyPressTime ) < 1000) {
			g.drawString("keyCode: " + lastKeyCode, x , 2 + sp, Graphics.TOP | Graphics.RIGHT);
		}

		g.setColor(255, 40, 40);
		x = (int) (5 * scale + (getWidth() - splash.getWidth()) / 2);
		int startLine = top / sp;
		int yc = topStart - top % sp;
		g.setClip(0, topStart, getWidth(), getHeight() - topStart);
		boolean visible = false;
		for (int i = startLine; i < txt.length; i++) {
			visible=true;
			if (i >= 0) {
				g.drawString(txt[i], x, yc, 0);
			}
			yc += sp;
			if (! visible){
				top = -space;
			}
		}
	}

	public void menu() {
		//#style listItem
		menuSplash.append(Locale.get("splash.Accept"), null);
		//#style listItem
		menuSplash.append(Locale.get("splash.Deny"), null);
		//#style listItem
		menuSplash.append(Locale.get("splash.enable"), null);
		//#style listItem
		menuSplash.append(Locale.get("splash.disable"), null);
		//#style listItem
		menuSplash.append("Switch to English", null);
		menuSplash.setCommandListener(this);
		menuSplash.addCommand(BACK_CMD);
		menuSplash.addCommand(EXIT_CMD);
		menuSplash.addCommand(ENABLE_CMD);
		menuSplash.addCommand(DISABLE_CMD);
		menuSplash.addCommand(ENGLISH_CMD);
		menuSplash.setTitle("ShareNav");
		menuSplash.setSelectCommand(SELECT_CMD);
		ShareNav.getInstance().show(menuSplash);
	}
	// endif

	public void commandAction(Command c, Displayable d) {
		if (c == SELECT_CMD) {
			shutdown = true;
			int choice = menuSplash.getSelectedIndex();
			Command choices[] = {BACK_CMD, EXIT_CMD, ENABLE_CMD, DISABLE_CMD, ENGLISH_CMD};
			commandAction(choices[choice], null);
        	return;
        }
        if (c == BACK_CMD) {
        	shutdown = true;
		exitSplash();
        	return;
        }
        if (c == EXIT_CMD) {
        	shutdown = true;
        	Configuration.setCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN, false, true);
//#if polish.android
        	Configuration.setCfgBitState(Configuration.CFGBIT_RUNNING, false, true);
        	main.notifyDestroyed();
//#else
        	main.exit();
//#endif
        	return;
        }
        if (c == ENGLISH_CMD) {
        	main.alert("Splash", "Switching to English", 3000);
        	Configuration.setUiLang("en");
        	Configuration.setNaviLang("en");
        	Configuration.setOnlineLang("en");
        	Configuration.setWikipediaLang("en");
        	Configuration.setNamesOnMapLang("en");
        	return;
        }
        if (c == ENABLE_CMD) {
        	Configuration.setCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN, false, true);
        	main.alert("Splash", Locale.get("splash.ShowSplash"), 3000);
        	return;
        }
        if (c == DISABLE_CMD) {
        	Configuration.setCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN, true, true);
        	main.alert("Splash", Locale.get("splash.HideSplash"), 3000);
        	return;
        }
	}

	private void exitSplash() {
		if (Legend.isValid) {
			ShareNav.showMapScreen();
		} else {
			Trace.getInstance().commandAction(Trace.SETUP_CMD);
		}
	}
		
	public void show() {
		ShareNav.getInstance().show(this);
	}

	public void run() {
		if (!Legend.isValid) {
			main.alert("Splash", ShareNav.errorMsg, 6000);
		}
		while (! shutdown){
			synchronized (this) {
				try {
					wait(40);
				} catch (InterruptedException e) {
	
				}
				top++;
				if (top > (ssize)) {
					top = -space;
				}
				repaint();
			}
		}
	}

	protected void pointerPressed(int x, int y) {
		menu();
	}
	
	protected void keyPressed(int keyCode) {
		lastKeyPressTime = System.currentTimeMillis();
		lastKeyCode = keyCode;
		
		if (keyCode == KEY_STAR) {
			boolean current = Configuration.getCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN);
			Configuration.setCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN, !current, true);
			if (current) {
				main.alert("Splash", Locale.get("splash.ShowSplash"), 3000);
			} else {
				main.alert("Splash", Locale.get("splash.HideSplash"), 3000);
			}
		}
		if (keyCode == KEY_POUND) {
			commandAction(ENGLISH_CMD, this);
		}
	}
	//#if polish.android
	// not in keyPressed() to solve the problem that Back gets passed on
	// to the next menu
	// See http://developer.android.com/sdk/android-2.0.html
	// for a possible Native Android workaround
	protected void keyReleased(int keyCode) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (main.shouldBeShown().equals(menuSplash)) {
				// Return from menu to splash screen
				show();
			} else {
				if (initDone) {
					exitSplash();
					return;
				} else {
					shutdown = true;
					Configuration.setCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN, false, true);
					Configuration.setCfgBitState(Configuration.CFGBIT_RUNNING, false, true);
					main.notifyDestroyed();
					return;
				}
			}
		}
	}
	//#endif
}
