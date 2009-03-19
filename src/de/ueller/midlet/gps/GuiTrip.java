package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 Markus Baeurle mbaeurle at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Calendar;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.graphics.LcdNumericFont;
import net.fatehi.SunCalc;

public class GuiTrip extends KeyCommandCanvas implements CommandListener,
		GpsMidDisplayable, LocationUpdateListener {

	private final Command NEXT_CMD = new Command("Next", Command.SCREEN, 5);
	private final static Logger mLogger = Logger.getInstance(GuiTrip.class,
			Logger.DEBUG);
	private final Trace mParent;
	private LcdNumericFont mLcdFont;
	private SunCalc mSunCalc;

	double[] mSunRiseset;

	private int mKmWidth = -1;
	private int mfontHeight = -1;

	public GuiTrip(Trace parent) {
		// #debug
		mLogger.info("Init GuiTrip");

		this.mParent = parent;
		addCommand(NEXT_CMD);
		setCommandListener(this);
		// TODO: Get the key for this from the configuration.
		singleKeyPressCommand.put(KEY_NUM7, NEXT_CMD);

		mLcdFont = new LcdNumericFont();
		mSunCalc = null;
	}

	protected void paint(Graphics g) {
		//#debug debug
		mLogger.debug("Drawing Trip screen");
		if (mKmWidth < 0) {
			/**
			 * Cache the values of the width of these strings
			 */
			Font f = g.getFont();
			mKmWidth = f.stringWidth("km");
			mfontHeight = f.getHeight();
		}
		Position pos = mParent.getCurrentPosition();
		
		int h = getHeight();
		int w = getWidth();
		
		g.setColor(0x00ffffff);
		g.fillRect(0, 0, w, h);

		g.setColor(0);
		int y = 48;

		// Draw our own course
		// TODO: Filter this but not at the presentation layer again as Trace.java
		// did but in one place (a data layer) where all parts of the code can use it.
		mLcdFont.setFontSize(36);
		mLcdFont.drawInt(g, (int)(pos.course), w, y - 6);
		g.drawLine(0, y, w, y);
		
		// Draw heading and distance to the destination
		y += 48;
		if (mParent.getTarget() == null) {
			mLcdFont.drawInvalid(g, 3, w, y - 6);
			y += 48;
			mLcdFont.drawInvalid(g, 4, w - mKmWidth -1, y - 6);
			g.drawString("km", w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
		} else {
			// Heading (result[1])
			// TODO: Will be added soon.
			mLcdFont.drawInvalid(g, 3, w, y - 6);

			// Distance (result[0])
			y += 48;
			float dist = ProjMath.getDistance(mParent.getTarget().lat, 
				mParent.getTarget().lon, mParent.center.radlat, mParent.center.radlon);
			if (dist > 10000) {
				mLcdFont.drawInt(g, (int)((dist / 1000.0f) + 0.5), 
						w - mKmWidth -1, y - 6);
				g.drawString("km", w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
			} else if (dist > 1000) {
				mLcdFont.drawFloat(g, (int)(((dist / 100.0f) + 0.5) / 
						10.0f), 1, w - mKmWidth - 1, y - 6);
				g.drawString("km", w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
			} else {
				// Using width of "km" to avoid jumping of number between m and km ranges.
				mLcdFont.drawInt(g, (int)(dist + 0.5), 
						w - mKmWidth - 1, y - 6);
				g.drawString("m", w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
			}
		}
		g.drawLine(0, y, w, y);
				
		// Calculate sunrise and sunset times only once.
		// Should the user move very far (many km) without leaving the screen,
		// the values will be slightly wrong so this needs to be improved in the future.
		// (Recalculation at a regular interval.)
		// On the other hand this can't justify to recalculate them every second.
		if (   (mSunCalc == null)
			&& ((pos.latitude != 0) || (pos.longitude != 0))
		   )
		{
			mSunCalc = new SunCalc();
			mSunCalc.setLatitude( pos.latitude );
			mSunCalc.setLongitude( pos.longitude );
			Calendar nowCal = Calendar.getInstance();
			mSunCalc.setYear( nowCal.get( Calendar.YEAR ) );
			mSunCalc.setMonth( nowCal.get( Calendar.MONTH ) + 1 );
			mSunCalc.setDay( nowCal.get( Calendar.DAY_OF_MONTH ) );
			mSunCalc.setTimeZoneOffset( nowCal.getTimeZone().getRawOffset() / 3600000 );
			mSunRiseset = mSunCalc.calcRiseSet( SunCalc.SUNRISE_SUNSET );
			mLogger.info("SunCalc result: " + mSunCalc.toString());
		}

		// Draw sunrise and sunset time
		y += 24;
		//mLcdFont.setFontSize(18);
		g.drawLine(w >> 1, y - 24, w >> 1, h);
		if (mSunRiseset != null) {
			g.drawString("Sunrise: " + mSunCalc.formatTime(mSunRiseset[SunCalc.RISE]), 
						 (w >> 1) - 3, y, Graphics.BOTTOM | Graphics.RIGHT);

			g.drawString("Sunset: " + mSunCalc.formatTime(mSunRiseset[SunCalc.SET]), 
					 	 w - 3, y, Graphics.BOTTOM | Graphics.RIGHT);
		} else {
			g.drawString("Sunrise: N/A", (w >> 1) - 3, y, 
						 Graphics.BOTTOM | Graphics.RIGHT);
			g.drawString("Sunset: N/A", w - 3, y, 
					 	 Graphics.BOTTOM | Graphics.RIGHT);
		}
	}

	public void show() {
		GpsMid.getInstance().show(this);
		synchronized (mParent.locationUpdateListeners) {
			mParent.locationUpdateListeners.addElement(this);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == NEXT_CMD) {
			synchronized (mParent.locationUpdateListeners) {
				mParent.locationUpdateListeners.removeElement(this);
			}
			// Force recalculation next time the screen is entered.
			mSunCalc = null;
			mParent.showNextDataScreen(Trace.DATASCREEN_TRIP);
		}
	}

	public void loctionUpdated() {
		repaint();
	}

}
