/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
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
package de.ueller.midlet.gps;

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.ImageTools;

import java.io.IOException;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.enough.polish.util.Locale;

public class Splash extends Canvas implements CommandListener,Runnable{
	private Image splash;
    /** Soft button to go back from about screen. */
    private final Command BACK_CMD = new Command(Locale.get("splash.Accept"), Command.OK, 2);
    private final Command EXIT_CMD = new Command(Locale.get("splash.Deny"), Command.EXIT, 1);
	private final GpsMid main;
	String[] txt = {
		Locale.get("splash.Discl1")/*Disclaimer:*/,
		Locale.get("splash.Discl2")/* No warranty, no liability.*/,
		Locale.get("splash.Discl3")/* Don't handle while driving!*/,
		Locale.get("splash.Discl4")/* Only use if legally allowed*/,
		Locale.get("splash.Discl5")/* to use the features under*/,
		Locale.get("splash.Discl6")/* the laws applicable for you.*/,
		Locale.get("splash.Copyright")/*Copyright:*/,
		" Harald Mueller",
		" Kai Krueger",
		" S. Hochmuth",
		Locale.get("splash.MarkusBaeurle"),
		" Jyrki Kuoppala",
		Locale.get("splash.artwork")/* Artwork (splash screen): */,
		" Tobias Mueller",
		Locale.get("splash.contributors")/*and many other contributors.*/,
		Locale.get("splash.codefrom")/*Includes code by:*/, 
		" Sualeh Fatehi ",
		" Nikolay Klimchuk",
		" Simon Turner",
		Locale.get("splash.MuellerHoenicke"),
		Locale.get("splash.Application")/*Application:*/,
		Locale.get("splash.Application2")/* licensed under GPL2*/,
		Locale.get("splash.Application3")/* http://www.gnu.org/*/,
		Locale.get("splash.MapData")/*Map data:*/,
		Locale.get("splash.MapData2")/* from OpenStreetMap*/,
		Locale.get("splash.MapData3")/* licensed under CC 2.0*/,
		Locale.get("splash.MapData4")/* http://creativecommons.org/*/,
		Configuration.getHasPointerEvents() ? 
		Locale.get("splash.skiptouch")/* Tap at top to skip this */:
		Locale.get("splash.skip")/* Press '*' to skip this */,
		Locale.get("splash.screen")/* screen at startup. */,
		Configuration.getHasPointerEvents() ? 
		"Tap middle of screen to": 
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


	public Splash(GpsMid main, boolean initDone) {
		this.main = main;
		this.initDone = initDone;
		try {
			splash = Image.createImage("/Gps-splash.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		appVersion = "V" + Legend.getAppVersion();
		addCommand(BACK_CMD);
		if (!initDone) {
			addCommand(EXIT_CMD);
		}
		show();
		setCommandListener(this);
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
		int x = (int) (5 * scale + (getWidth() - splash.getWidth()) / 2);
		
		g.drawImage(splash, getWidth() / 2, 0, Graphics.HCENTER | Graphics.TOP);

		g.setColor(0xFFFF99);
		g.drawString(appVersion + " " + mapVersion, (getWidth() + splash.getWidth()) / 2 - 2 , 2, 
					 Graphics.TOP | Graphics.RIGHT);		
		g.setColor(255, 40, 40);
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

	public void commandAction(Command c, Displayable d) {
        if (c == BACK_CMD) {
        	shutdown = true;
        	GpsMid.showMapScreen();
        	return;
        }
        if (c == EXIT_CMD) {
        	shutdown = true;
        	Configuration.setCfgBitState(Configuration.CFGBIT_SKIPP_SPLASHSCREEN, false, true);
        	main.exit();
        	return;
        }
	}

	public void show(){
		GpsMid.getInstance().show(this);
	}

	public void run() {
		if (!Legend.isValid) {
			main.alert("Splash", GpsMid.errorMsg, 6000);
		}
//#if polish.android
		// workaround for Android accept buttons; without this, accept/deny don't work.
		if (initDone) {
			main.alert("Splash", "Android!", 500);
		}
//#endif
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
		if (y < getHeight() / 5) {
			keyPressed(KEY_STAR);
		}
		// around the region where England is in the map
		if (y > (2 * getHeight() / 5) && y < (3 * getHeight() / 5) ) {
			keyPressed(KEY_POUND);
		}
	}
	
	protected void keyPressed(int keyCode) {
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
			main.alert("Splash", "Switching to English", 3000);
			Configuration.setUiLang("en");
			Configuration.setNaviLang("en");
			Configuration.setOnlineLang("en");
			Configuration.setWikipediaLang("en");
			Configuration.setNamesOnMapLang("en");
		}
	}
}
