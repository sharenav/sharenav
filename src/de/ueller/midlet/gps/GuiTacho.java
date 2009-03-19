package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Calendar;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.gps.data.Position;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.graphics.LcdNumericFont;

public class GuiTacho extends KeyCommandCanvas implements CommandListener,
		GpsMidDisplayable, LocationUpdateListener {

	private final Command NEXT_CMD = new Command("Next", Command.SCREEN, 5);
	private final static Logger logger = Logger.getInstance(GuiTacho.class,
			Logger.DEBUG);
	private final Trace parent;
	private LcdNumericFont lcdFont;

	private Calendar cal = Calendar.getInstance();
	private StringBuffer timeString;

	private float alt_delta = 0.0f;
	private float odo = 0.0f;
	private long duration = 0;
	private float avg_spd = 0.0f;
	private float max_spd = 0.0f;

	private int kmhWidth = -1;
	//private int msWidth = -1;
	private int mminWidth = -1;
	private int mWidth = -1;
	private int kmWidth = -1;
	private int fHeight = -1;

	public GuiTacho(Trace parent) {
		// #debug
		logger.info("Init GuiTacho");

		this.parent = parent;
		addCommand(NEXT_CMD);
		setCommandListener(this);
		// TODO: Get the key for this from the configuration.
		singleKeyPressCommand.put(KEY_NUM7, NEXT_CMD);

		lcdFont = new LcdNumericFont();

		timeString = new StringBuffer();
	}

	protected void paint(Graphics g) {
		//#debug debug
		logger.debug("Drawing Tacho screen");
		if (kmhWidth < 0) {
			/**
			 * Cache the values of the width of these Strings
			 */
			Font f = g.getFont();
			kmhWidth = f.stringWidth("km/h");
			kmWidth = f.stringWidth("km");
			mminWidth = f.stringWidth("m/min");
			mWidth = f.stringWidth("m");
			fHeight = f.getHeight();
		}
		Position pos = parent.getCurrentPosition();
		odo = parent.gpx.currentTrkLength() / 1000.0f;
		avg_spd = parent.gpx.currentTrkAvgSpd() * 3.6f;
		max_spd = ((int)(parent.gpx.maxTrkSpeed() * 36.0f))/10.0f;
		alt_delta = parent.gpx.deltaAltTrkSpeed();
		duration = parent.gpx.currentTrkDuration();
		
		int h = getHeight();
		int w = getWidth();
		
		g.setColor(0x00ffffff);
		g.fillRect(0, 0, w, h);

		g.setColor(0);
		int y = 0;
		
		cal.setTime(pos.date);
		timeString.setLength(0);
		timeString
				.append(HelperRoutines.formatInt2(cal
								.get(Calendar.DAY_OF_MONTH)))
				.append(".")
				.append(HelperRoutines.formatInt2(cal.get(Calendar.MONTH) + 1))
				.append(".")
				.append(HelperRoutines.formatInt2(cal.get(Calendar.YEAR) % 100));
		g.drawString(timeString.toString(), 3, y, Graphics.TOP | Graphics.LEFT);
		
		g.drawString(parent.solution, (w >> 1) + 3, y, Graphics.TOP
				| Graphics.LEFT);
		
		timeString.setLength(0);
		y += fHeight;
		
		timeString.append(
				HelperRoutines.formatInt2(cal.get(Calendar.HOUR_OF_DAY)))
				.append(":").append(
						HelperRoutines.formatInt2(cal.get(Calendar.MINUTE)))
				.append(":").append(
						HelperRoutines.formatInt2(cal.get(Calendar.SECOND)));
		g.drawString(timeString.toString(), 3, y, Graphics.TOP | Graphics.LEFT);
		
		g.drawString("DOP: " + pos.pdop, (w >> 1) + 3, y, Graphics.TOP
				| Graphics.LEFT);

		y += fHeight;
		g.drawLine(w >> 1, 0, w >> 1, y);
		g.drawLine(0, y, w, y);
		y += 64;
		lcdFont.setFontSize(48);
		
		g.drawString("km/h", w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);

		if (pos.speed > 10) {
			lcdFont.drawInt(g, (int)(pos.speed * 3.6f), w - kmhWidth - 1, y - 5);
		} else {
			lcdFont.drawFloat(g, pos.speed * 3.6f, 1, w - kmhWidth - 1, y - 5);
		}
		g.drawLine(0, y, w, y);
		
		lcdFont.setFontSize(18);
		g.drawLine(w >> 1, y, w >> 1, y + 32);
		y += 28;
		g.drawString("km", (w >> 1) - 1, y - 5, Graphics.BOTTOM
				| Graphics.RIGHT);
		if (odo > 10) {
			lcdFont.drawFloat(g, odo, 1, (w >> 1) - kmWidth - 2, y);
		} else {
			lcdFont.drawFloat(g, odo, 2, (w >> 1) - kmWidth - 2, y);
		}
		g.drawString("km/h", w - 1, y - 5, Graphics.BOTTOM | Graphics.RIGHT);
		if (avg_spd > 30) {
			lcdFont.drawInt(g, (int)avg_spd, w - kmhWidth - 2, y);
		} else {
			lcdFont.drawFloat(g, avg_spd, 1, w - kmhWidth - 2, y);
		}
		g.drawLine(0, y, w, y);
		g.drawLine(w >> 1, y, w >> 1, y + 32);
		y += 28;
		
		g.drawString("m", (w >> 1) - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
		lcdFont.drawInt(g, (int) pos.altitude, (w >> 1) - mWidth - 2, y);
		
		g.drawString("m/min", w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
		lcdFont.drawFloat(g, alt_delta * 60, 1, w - mminWidth - 2, y);
		g.drawLine(0, y, w, y);
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
		g.drawString(timeString.toString(), (w >> 1) - 1, y + 3,
				Graphics.BOTTOM | Graphics.RIGHT);
		g.drawString(max_spd + " km/h", w - 1, y + 3, Graphics.BOTTOM
				| Graphics.RIGHT);
	}

	public void show() {
		GpsMid.getInstance().show(this);
		synchronized (parent.locationUpdateListeners) {
			parent.locationUpdateListeners.addElement(this);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == NEXT_CMD) {
			synchronized (parent.locationUpdateListeners) {
				parent.locationUpdateListeners.removeElement(this);
			}
			parent.showNextDataScreen(Trace.DATASCREEN_TACHO);
		}
	}

	public void loctionUpdated() {
		repaint();
	}

}
