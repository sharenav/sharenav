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
		Locale.get("splash.Discl6")/* for you applicable laws.*/,
		Locale.get("splash.Copyright")/*Copyright:*/,
		Locale.get("splash.author")/*Original program:*/, 
		" Harald Mueller",
		Locale.get("splash.currdevelopers")/*Other current developers:*/, 
		" Kai Krueger",
		" S. Hochmuth",
		" Markus Bäurle",
		" Jyrki Kuoppala",
		Locale.get("splash.artwork")/* Artwork: */,
		" Tobias Mueller",
		Locale.get("splash.contributors")/*Many other contributors*/,
		Locale.get("splash.codefrom")/*Includes code by:*/, 
		" Sualeh Fatehi " +
		"(" + Locale.get("splash.suncalc")/* SunCalc */ + ")",
		" Nikolay Klimchuk",
		" Simon Turner",
		Locale.get("splash.Application")/*Application:*/,
		Locale.get("splash.Application2")/* licensed under GPL2*/,
		Locale.get("splash.Application3")/* http://www.gnu.org/*/,
		Locale.get("splash.MapData")/*Map data:*/,
		Locale.get("splash.MapData2")/* from OpenStreetMap*/,
		Locale.get("splash.MapData3")/* licensed under CC 2.0*/,
		Locale.get("splash.MapData4")/* http://creativecommons.org/*/,
		Locale.get("splash.skip")/* Press '*' to skip this */,
		Locale.get("splash.screen")/* screen at startup. */,
		"Press '#' to switch to English" };
	private Font f;
	int top = 0;
	private Thread processorThread;
	private boolean shutdown = false;
	private int ssize;
	private int topStart = 106;
	private int space;
	private double scale = 1;
	private String strVersion; 


	public Splash(GpsMid main) {
		this.main = main;
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
			int newWidth =  (int)(scale* (double) splash.getWidth());
			int newHeight = (int)(scale* (double) splash.getHeight());
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
		if (GpsMid.legend != null) {
			strVersion = "V" + Legend.getAppVersion() + " (" + Legend.getBundleDate() + ")";
		} else {
			strVersion = "Error reading map!";
		}
		show();
		addCommand(BACK_CMD);
		addCommand(EXIT_CMD);
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
		g.drawString(strVersion, (getWidth() + splash.getWidth()) / 2 - 2 , 2, 
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
		if (GpsMid.legend == null) {
			main.alert("Splash", GpsMid.errorMsg, 6000);
		}
//#if polish.android
		// workaround for Android accept buttons; without this, accept/deny don't work.
		main.alert("Splash", "Android!", 500);
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
			try {
				Locale.loadTranslations( "/en.loc" );
				Trace.uncacheIconMenu();
			} catch (IOException ioe) {
				System.out.println("Couldn't open translations file");
			}
			Configuration.setUiLang("en");
			Configuration.setNaviLang("en");
			Configuration.setOnlineLang("en");
			Configuration.setWikipediaLang("en");
			Configuration.setNamesOnMapLang("en");
		}
	}
}
