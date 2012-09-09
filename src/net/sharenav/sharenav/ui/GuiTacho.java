package net.sharenav.sharenav.ui;

/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Calendar;
import java.util.Date;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import net.sharenav.gps.location.LocationUpdateListener;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.Position;
import net.sharenav.midlet.graphics.LcdNumericFont;
import net.sharenav.util.HelperRoutines;
import net.sharenav.util.IntTree;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

//#if polish.android
import android.view.KeyEvent;
//#endif

/** 
 * Implements the "Tacho" screen which displays numbers such as speed, height etc.
 */
public class GuiTacho extends KeyCommandCanvas implements CommandListener,
		ShareNavDisplayable, LocationUpdateListener {

	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 5);
	private final Command NEXT_CMD = new Command(Locale.get("guitacho.Next")/*Next*/, Command.SCREEN, 5);
	private final static Logger logger = Logger.getInstance(GuiTacho.class,
			Logger.DEBUG);
	private Trace parent = null;
	private LcdNumericFont lcdFont = null;

	private final Calendar cal = Calendar.getInstance();
	private final Date date = new Date();
	private StringBuffer timeString = null;

	private float alt_delta = 0.0f;
	private float odo = 0.0f;
	private long duration = 0;
	private float avg_spd = 0.0f;
	private float max_spd = 0.0f;

	private int kmhWidth = -1;
	private int mphWidth = -1;
	//private int msWidth = -1;
	private int mminWidth = -1;
	private int mWidth = -1;
	private int kmWidth = -1;
	private int miWidth = -1;
	private int fHeight = -1;

	public GuiTacho() {
		init();
	}

	public GuiTacho(Trace parent) {
		// #debug
		logger.info("Init GuiTacho");

		this.parent = parent;
		addCommand(BACK_CMD);
		addCommand(NEXT_CMD);
		setCommandListener(this);

		// We want to use the same keys (plural!) to go to the next screen
		// as the map (Trace) screen.
		// NOTE: I don't think repeatable, game or nonReleasable keys will
		// ever be used for this, but if they are this needs to be extended!
		IntTree singleKeys = parent.getSingleKeyPressesForCommand(
				parent.getDataScreenCommand());
		for (int i = 0; i < singleKeys.size(); i++) {
			singleKeyPressCommand.put(singleKeys.getKeyIdx(i), NEXT_CMD);
		}
		IntTree doubleKeys = parent.getDoubleKeyPressesForCommand(
				parent.getDataScreenCommand());
		for (int i = 0; i < doubleKeys.size(); i++) {
			doubleKeyPressCommand.put(doubleKeys.getKeyIdx(i), NEXT_CMD);
		}
		IntTree longKeys = parent.getLongKeyPressesForCommand(
				parent.getDataScreenCommand());
		for (int i = 0; i < longKeys.size(); i++) {
			longKeyPressCommand.put(longKeys.getKeyIdx(i), NEXT_CMD);
		}

		init();
	}

	protected void init() {
		lcdFont = new LcdNumericFont();

		timeString = new StringBuffer();
	}

	protected void paintTacho(Graphics g, int minX, int minY, int maxX, int maxY, Position pos) {
		g.setColor(Legend.COLORS[Legend.COLOR_TACHO_BACKGROUND]);
		g.fillRect(minX, minY, maxX, maxY);

		g.setColor(Legend.COLORS[Legend.COLOR_TACHO_TEXT]);
		int y = minY;
		
		date.setTime(pos.timeMillis);	// set Date to milliSecs since 01-Jan-1970
		cal.setTime(date);				// set Calendar to Date
		
		timeString.setLength(0);
		timeString
				.append(HelperRoutines.formatInt2(cal
								.get(Calendar.DAY_OF_MONTH)))
				.append(".")
				.append(HelperRoutines.formatInt2(cal.get(Calendar.MONTH) + 1))
				.append(".")
				.append(HelperRoutines.formatInt2(cal.get(Calendar.YEAR) % 100));
		g.drawString(timeString.toString(), minX + 3, y, Graphics.TOP | Graphics.LEFT);
		
		g.drawString("HDOP: " + pos.hdop
			     + " " + parent.solutionStr, minX + ((maxX - minX) >> 1) + 3, y, Graphics.TOP
				| Graphics.LEFT);
		
		timeString.setLength(0);
		y += fHeight;
		
		timeString.append(
				HelperRoutines.formatInt2(cal.get(Calendar.HOUR_OF_DAY)))
				.append(":").append(
						HelperRoutines.formatInt2(cal.get(Calendar.MINUTE)))
				.append(":").append(
						HelperRoutines.formatInt2(cal.get(Calendar.SECOND)));
		g.drawString(timeString.toString(), minX + 3, y, Graphics.TOP | Graphics.LEFT);
		
		g.drawString("PDOP: " + pos.pdop, minX + ((maxX - minX) >> 1) + 3, y, Graphics.TOP
				| Graphics.LEFT);

		y += fHeight;
		g.drawLine(minX + (maxX - minX) >> 1, minY, minX + (maxX - minX) >> 1, y);
		g.drawLine(minX, y, maxX, y);
		y += 64;
		lcdFont.setFontSize(48);
		
		if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
		        g.drawString(Locale.get("guitacho.kmh")/*km/h*/, maxX - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);

			if (pos.speed > 10) {
				lcdFont.drawInt(g, (int)(pos.speed * 3.6f), maxX - kmhWidth - 1, y - 5);
			} else {
				lcdFont.drawFloat(g, pos.speed * 3.6f, 1, maxX - kmhWidth - 1, y - 5);
			}
		} else {
		        g.drawString(Locale.get("guitacho.mph")/*mph*/, maxX - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);

			if (pos.speed > 10) {
				lcdFont.drawInt(g, (int)(pos.speed * 2.237f), maxX - kmhWidth - 1, y - 5);
			} else {
				lcdFont.drawFloat(g, pos.speed * 2.237f, 1, maxX - kmhWidth - 1, y - 5);
			}
		}
		
		
		g.drawLine(minX, y, maxX, y);
		
		lcdFont.setFontSize(18);
		g.drawLine(minX + (maxX - minX) >> 1, y, minX + (maxX - minX) >> 1, y + 32);
		y += 28;
		if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
		        g.drawString(Locale.get("guitacho.km")/*km*/, minX + ((maxX - minX) >> 1) - 1, y - 5, Graphics.BOTTOM
					| Graphics.RIGHT);
			if (odo > 10) {
				lcdFont.drawFloat(g, odo, 1, minX + ((maxX - minX) >> 1) - kmWidth - 2, y);
			} else {
				lcdFont.drawFloat(g, odo, 2, minX + ((maxX - minX) >> 1) - kmWidth - 2, y);
			}
			g.drawString(Locale.get("guitacho.kmh")/*km/h*/, maxX - 1, y - 5, Graphics.BOTTOM | Graphics.RIGHT);
			if (avg_spd > 30) {
				lcdFont.drawInt(g, (int)avg_spd, maxX - kmhWidth - 2, y);
			} else {
				lcdFont.drawFloat(g, avg_spd, 1, maxX - kmhWidth - 2, y);
			}
		} else {
		        g.drawString(Locale.get("guitacho.mi")/*mi*/, minX + ((maxX - minX) >> 1) - 1, y - 5, Graphics.BOTTOM
					| Graphics.RIGHT);
			if (odo > 10) {
				lcdFont.drawFloat(g, (odo / 1.609344f), 1, minX + ((maxX - minX) >> 1) - miWidth - 2, y);
			} else {
				lcdFont.drawFloat(g, (odo / 1.609344f), 2, minX + ((maxX - minX) >> 1) - miWidth - 2, y);
			}
			g.drawString(Locale.get("guitacho.mph")/*mph*/, maxX - 1, y - 5, Graphics.BOTTOM | Graphics.RIGHT);
			if (avg_spd > 30) {
				lcdFont.drawInt(g, (int)(avg_spd / 1.609344f), maxX - mphWidth - 2, y);
			} else {
				lcdFont.drawFloat(g, (avg_spd / 1.609344f), 1, maxX - mphWidth - 2, y);
			}
		}
		g.drawLine(minX, y, maxX, y);
		g.drawLine(minX + (maxX - minX) >> 1, y, minX + (maxX - minX) >> 1, y + 32);
		y += 28;
		
		g.drawString(Locale.get("guitacho.m")/*m*/, minX + ((maxX - minX) >> 1) - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
		lcdFont.drawInt(g, (int) pos.altitude, minX + ((maxX - minX) >> 1) - mWidth - 2, y);
		
		g.drawString(Locale.get("guitacho.mmin")/*m/min*/, maxX - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
		lcdFont.drawFloat(g, alt_delta * 60, 1, maxX - mminWidth - 2, y);
		g.drawLine(minX, y, maxX, y);
		y += fHeight;
		
		//dur.setTime(duration);
		//cal.setTime(dur);
		timeString.setLength(0);
		timeString.append(
				HelperRoutines.formatInt2((int)(duration / (1000 * 60 * 60))))
				.append(":").append(
						HelperRoutines.formatInt2((int)(duration / (1000 * 60)) % 60))
				.append(":").append(
						HelperRoutines.formatInt2((int)(duration / 1000) % 60));
		g.drawString(timeString.toString(), minX + ((maxX - minX) >> 1) - 1, y + 3,
				Graphics.BOTTOM | Graphics.RIGHT);
		if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
			g.drawString(max_spd + " " + Locale.get("guitacho.kmh")/*km/h*/, maxX - 1, y + 3, Graphics.BOTTOM
					| Graphics.RIGHT);
		} else {
			g.drawString((max_spd / 1.609334f) + " " + Locale.get("guitacho.mph")/*mph*/, maxX - 1, y + 3, Graphics.BOTTOM
					| Graphics.RIGHT);
		}
	}

	protected void setValues(Trace parent, Graphics g) {
		this.parent = parent;
		if (kmhWidth < 0) {
			/**
			 * Cache the values of the width of these Strings
			 */
			Font f = g.getFont();
			f = g.getFont();
			kmhWidth = f.stringWidth(Locale.get("guitacho.kmh")/*km/h*/);
			mphWidth = f.stringWidth(Locale.get("guitacho.mph")/*mph*/);
			kmWidth = f.stringWidth(Locale.get("guitacho.km")/*km*/);
			miWidth = f.stringWidth(Locale.get("guitacho.mi")/*mi*/);
			mminWidth = f.stringWidth(Locale.get("guitacho.mmin")/*m/min*/);
			mWidth = f.stringWidth(Locale.get("guitacho.m")/*m*/);
			fHeight = f.getHeight();
		}
		odo = parent.gpx.currentTrkLength() / 1000.0f;
		avg_spd = parent.gpx.currentTrkAvgSpd() * 3.6f;
		max_spd = ((int)(parent.gpx.maxTrkSpeed() * 36.0f))/10.0f;
		alt_delta = parent.gpx.deltaAltTrkSpeed();
		duration = parent.gpx.currentTrkDuration();
		
	}

	protected void paint(Graphics g) {
		//#debug debug
		logger.debug("Drawing Tacho screen");
		setValues(parent, g);
		Position pos = parent.getCurrentPosition();

		int h = getHeight();
		int w = getWidth();
		
		paintTacho(g, 0, 0, w, h, pos);
	}

	public void show() {
		// if device has a touch screen never use fullscreen mode as we have no Next-Button
		setFullScreenMode(!hasPointerEvents() && Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		ShareNav.getInstance().show(this);
		synchronized (parent.locationUpdateListeners) {
			parent.locationUpdateListeners.addElement(this);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			synchronized (parent.locationUpdateListeners) {
				parent.locationUpdateListeners.removeElement(this);
			}
			parent.show();			
		} else if (c == NEXT_CMD) {
			synchronized (parent.locationUpdateListeners) {
				parent.locationUpdateListeners.removeElement(this);
			}
			parent.showNextDataScreen(Trace.DATASCREEN_TACHO);
		}
	}

	public void loctionUpdated() {
		repaint();
	}

	//#if polish.android
	// not in keyPressed() to solve the problem that Back gets passed on
	// to the next menu
	// See http://developer.android.com/sdk/android-2.0.html
	// for a possible Native Android workaround
	protected void keyReleased(int keyCode) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			commandAction(BACK_CMD, (Displayable) null);
		}
	}
	//#endif
}
