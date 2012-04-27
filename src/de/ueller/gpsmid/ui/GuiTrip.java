/*
 * GpsMid - Copyright (c) 2008 Markus Baeurle mbaeurle at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import java.util.Calendar;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.gps.location.LocationUpdateListener;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.Position;
import de.ueller.midlet.graphics.LcdNumericFont;
import de.ueller.util.IntTree;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import de.ueller.util.ProjMath;
import net.fatehi.SunCalc;

//#if polish.android
import android.view.KeyEvent;
//#endif

public class GuiTrip extends KeyCommandCanvas implements CommandListener,
		GpsMidDisplayable, LocationUpdateListener {

	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 5);
	private final Command NEXT_CMD = new Command(Locale.get("guitrip.Next")/*Next*/, Command.SCREEN, 5);
	private final static Logger mLogger = Logger.getInstance(GuiTrip.class,
			Logger.DEBUG);
	private final Trace mParent;
	private LcdNumericFont mLcdFont;
	private SunCalc mSunCalc;

	double[] mSunRiseset;

	private int mKmWidth = -1;
	private int mMiWidth = -1;
	private int mfontHeight = -1;

	public GuiTrip(Trace parent) {
		// #debug
		mLogger.info("Init GuiTrip");

		this.mParent = parent;
		addCommand(BACK_CMD);
		addCommand(NEXT_CMD);
		setCommandListener(this);

		// We want to use the same keys (plural!) to go to the next screen
		// as the map (Trace) screen.
		// NOTE: I don't think repeatable, game or nonReleasable keys will
		// ever be used for this, but if they are this needs to be extended!
		IntTree singleKeys = mParent.getSingleKeyPressesForCommand(
				mParent.getDataScreenCommand());
		for (int i = 0; i < singleKeys.size(); i++) {
			singleKeyPressCommand.put(singleKeys.getKeyIdx(i), NEXT_CMD);
		}
		IntTree doubleKeys = mParent.getDoubleKeyPressesForCommand(
				mParent.getDataScreenCommand());
		for (int i = 0; i < doubleKeys.size(); i++) {
			doubleKeyPressCommand.put(doubleKeys.getKeyIdx(i), NEXT_CMD);
		}
		IntTree longKeys = mParent.getLongKeyPressesForCommand(
				mParent.getDataScreenCommand());
		for (int i = 0; i < longKeys.size(); i++) {
			longKeyPressCommand.put(longKeys.getKeyIdx(i), NEXT_CMD);
		}

		mLcdFont = new LcdNumericFont();
		mSunCalc = null;
	}

	protected int paintTrip(Graphics g, int h, int w, int y) {
		if (mKmWidth < 0) {
			/**
			 * Cache the values of the width of these strings
			 */
			Font f = g.getFont();
			mKmWidth = f.stringWidth("km");
			mMiWidth = f.stringWidth("mi");
			mfontHeight = f.getHeight();
		}
		Position pos = mParent.getCurrentPosition();
		
		g.setColor(Legend.COLORS[Legend.COLOR_TACHO_BACKGROUND]);
		g.fillRect(0, 0, w, h);

		g.setColor(Legend.COLORS[Legend.COLOR_TACHO_TEXT]);

		// Draw our own course
		// TODO: Filter this but not at the presentation layer again as Trace.java
		// did but in one place (a data layer) where all parts of the code can use it.
		mLcdFont.setFontSize(36);
		mLcdFont.drawInt(g, (int)(pos.course), w, y - 6);
		g.drawLine(0, y, w, y);
		
		// Draw heading and distance to the destination
		y += 48;
		if (mParent.getDestination() == null) {
			mLcdFont.drawInvalid(g, 3, w, y - 6);
			y += 48;
			mLcdFont.drawInvalid(g, 4, w - mKmWidth -1, y - 6);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				g.drawString(Locale.get("guitacho.km"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
			} else {
				g.drawString(Locale.get("guitacho.mi"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
			}
		} else {
			float[] result = ProjMath.calcDistanceAndCourse(mParent.center.radlat, 
					mParent.center.radlon, mParent.getDestination().lat, 
					mParent.getDestination().lon);
			// Heading (result[1])
			int relHeading = (int)(result[1] - pos.course + 0.5);
			if (relHeading < 0)
			{
				relHeading += 360;
			}
			mLcdFont.drawInt(g, relHeading, w, y - 6);
			g.drawLine(0, y, w, y);
		
			// Distance (result[0])
			y += 48;
			if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				if (result[0] > 100000) {
					mLcdFont.drawInt(g, (int)((result[0] / 1000.0f) + 0.5), 
							w - mKmWidth -1, y - 6);
					g.drawString(Locale.get("guitacho.km"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else if (result[0] > 1000) {
					mLcdFont.drawFloat(g, (int)((result[0] / 100.0f) + 0.5) / 
							10.0f, 1, w - mKmWidth - 1, y - 6);
					g.drawString(Locale.get("guitacho.km"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else {
					// Using width of "km" to avoid jumping of number between m and km ranges.
					mLcdFont.drawInt(g, (int)(result[0] + 0.5), 
							w - mKmWidth - 1, y - 6);
					g.drawString(Locale.get("guitacho.m"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
				}
			} else {
				if (result[0] > 160934) {
					mLcdFont.drawInt(g, (int)((result[0] / 1609.3f) + 0.5), 
							w - mMiWidth -1, y - 6);
					g.drawString(Locale.get("guitacho.mi"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else if (result[0] > 1609) {
					mLcdFont.drawFloat(g, (int)((result[0] / 160.9f) + 0.5) / 
							10.0f, 1, w - mMiWidth - 1, y - 6);
					g.drawString(Locale.get("guitacho.mi"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else {
					// Using width of "km" to avoid jumping of number between m and km ranges.
					mLcdFont.drawInt(g, (int)(result[0] + 0.5), 
							w - mMiWidth - 1, y - 6);
					g.drawString(Locale.get("guitacho.yd"), w - 1, y - 3, Graphics.BOTTOM | Graphics.RIGHT);
				}
			}
		}
		g.drawLine(0, y, w, y);
		return y;
	}

	protected int paintSun(Graphics g, int h, int w, int y) {
		//mLcdFont.setFontSize(18);
		g.drawLine(w >> 1, y - 24, w >> 1, h);
		if (mSunRiseset != null) {
			g.drawString(Locale.get("guitrip.Sunrise")/*Sunrise: */ + mSunCalc.formatTime(mSunRiseset[SunCalc.RISE]), 
						 (w >> 1) - 3, y, Graphics.BOTTOM | Graphics.RIGHT);

			g.drawString(Locale.get("guitrip.Sunset")/*Sunset: */ + mSunCalc.formatTime(mSunRiseset[SunCalc.SET]), 
					 	 w - 3, y, Graphics.BOTTOM | Graphics.RIGHT);
		} else {
			g.drawString(Locale.get("guitrip.SunriseNA")/*Sunrise: N/A*/, (w >> 1) - 3, y, 
						 Graphics.BOTTOM | Graphics.RIGHT);
			g.drawString(Locale.get("guitrip.SunsetNA")/*Sunset: N/A*/, w - 3, y, 
					 	 Graphics.BOTTOM | Graphics.RIGHT);
		}
		return y;
	}

	protected void calcSun() {
		// Calculate sunrise and sunset times at the first time
		// or when the map position changed more than 10 km
		if ((mSunCalc == null) ||
			( 	mSunCalc != null
			 &&	Math.abs(ProjMath.getDistance(mParent.center.radlat,
											  mParent.center.radlon,
											  mSunCalc.getLatitude() * MoreMath.FAC_DECTORAD,
											  mSunCalc.getLongitude() * MoreMath.FAC_DECTORAD
				) ) > 10000)
		) {
			if (mSunCalc == null) {
				mSunCalc = new SunCalc();
			}
			mSunCalc.setLatitude(mParent.center.radlat * MoreMath.FAC_RADTODEC);
			mSunCalc.setLongitude(mParent.center.radlon * MoreMath.FAC_RADTODEC);
			Calendar nowCal = Calendar.getInstance();
			mSunCalc.setYear( nowCal.get( Calendar.YEAR ) );
			mSunCalc.setMonth( nowCal.get( Calendar.MONTH ) + 1 );
			mSunCalc.setDay( nowCal.get( Calendar.DAY_OF_MONTH ) );
			// Sigh. Can this stuff be more complicated to use? I say no.
			int tzone = nowCal.getTimeZone().getOffset(/*era=AD*/ 1, 
					nowCal.get(Calendar.YEAR), nowCal.get(Calendar.MONTH), 
					nowCal.get(Calendar.DAY_OF_MONTH),
					nowCal.get(Calendar.DAY_OF_WEEK), /*ms in day*/ 0);
			mSunCalc.setTimeZoneOffset( tzone / 3600000 );
			mSunRiseset = mSunCalc.calcRiseSet( SunCalc.SUNRISE_SUNSET );
			mLogger.info("SunCalc result: " + mSunCalc.toString());
		}
	}

	protected void paint(Graphics g) {
		//#debug debug
		mLogger.debug("Drawing Trip screen");
		int h = getHeight();
		int w = getWidth();
		
		int y = 48;
		y = paintTrip(g, h, w, y);

		calcSun();

		// Draw sunrise and sunset time
		y += 24;
		y = paintSun(g, h, w, y);
	}

	public void show() {
		// if device has a touch screen never use fullscreen mode as we have no Next-Button
		setFullScreenMode(!hasPointerEvents() && Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		GpsMid.getInstance().show(this);
		synchronized (mParent.locationUpdateListeners) {
			mParent.locationUpdateListeners.addElement(this);
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			synchronized (mParent.locationUpdateListeners) {
				mParent.locationUpdateListeners.removeElement(this);
			}
			// Force recalculation next time the screen is entered.
			mSunCalc = null;
			mParent.show();
		} else if (c == NEXT_CMD) {
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
