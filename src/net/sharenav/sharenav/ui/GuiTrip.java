/*
 * ShareNav - Copyright (c) 2008 Markus Baeurle mbaeurle at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.sharenav.ui;

import java.util.Calendar;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import net.sharenav.gps.location.LocationUpdateListener;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.Position;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.midlet.graphics.LcdNumericFont;
import net.sharenav.util.IntTree;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;
import net.fatehi.SunCalc;

//#if polish.android
import android.view.KeyEvent;
//#endif

public class GuiTrip extends KeyCommandCanvas implements CommandListener,
		ShareNavDisplayable, LocationUpdateListener {

	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 5);
	private final Command NEXT_CMD = new Command(Locale.get("guitrip.Next")/*Next*/, Command.SCREEN, 5);
	private final static Logger mLogger = Logger.getInstance(GuiTrip.class,
			Logger.DEBUG);
	private Trace mParent = null;
	private LcdNumericFont mLcdFont;
	private SunCalc mSunCalc;

	double[] mSunRiseset;

	private int mKmWidth = -1;
	private int mMiWidth = -1;
	private int mfontHeight = -1;

	public GuiTrip() {
		init();
	}

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

		init();
	}

	protected void init() {
		mLcdFont = new LcdNumericFont();
		mSunCalc = null;
	}

	protected int paintTrip(Graphics g, int minX, int minY, int maxX, int maxY, int yDiff, Position pos, PositionMark destination, Trace trace) {
		if (mKmWidth < 0) {
			/**
			 * Cache the values of the width of these strings
			 */
			Font f = g.getFont();
			mKmWidth = f.stringWidth("km");
			mMiWidth = f.stringWidth("mi");
			mfontHeight = f.getHeight();
		}
		g.setColor(Legend.COLORS[Legend.COLOR_TACHO_BACKGROUND]);
		g.fillRect(minX, minY, maxX - minX, maxY - minY);

		g.setColor(Legend.COLORS[Legend.COLOR_TACHO_TEXT]);

		// Draw our own course
		// TODO: Filter this but not at the presentation layer again as Trace.java
		// did but in one place (a data layer) where all parts of the code can use it.
		mLcdFont.setFontSize(36);
		mLcdFont.drawInt(g, (int)(pos.course), maxX, minY + yDiff - 6);
		g.drawLine(minX, minY + yDiff, maxX, minY + yDiff);
		
		// Draw heading and distance to the destination
		yDiff += 48;
		if (destination == null) {
			mLcdFont.drawInvalid(g, minX + 3, maxX, minY + yDiff - 6);
			yDiff += 48;
			mLcdFont.drawInvalid(g, minX + 4, maxX - mKmWidth -1, minY + yDiff - 6);
			if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				g.drawString(Locale.get("guitacho.km"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
			} else {
				g.drawString(Locale.get("guitacho.mi"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
			}
		} else {
			float[] result = ProjMath.calcDistanceAndCourse(trace.center.radlat, 
					trace.center.radlon, destination.lat, 
					destination.lon);
			// Heading (result[1])
			int relHeading = (int)(result[1] - pos.course + 0.5);
			if (relHeading < 0)
			{
				relHeading += 360;
			}
			mLcdFont.drawInt(g, relHeading, maxX, minY + yDiff - 6);
			g.drawLine(minX, minY + yDiff, maxX, minY + yDiff);
		
			// Distance (result[0])
			yDiff += 48;
			if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
				if (result[0] > 100000) {
					mLcdFont.drawInt(g, (int)((result[0] / 1000.0f) + 0.5), 
							maxX - mKmWidth -1, minY + yDiff - 6);
					g.drawString(Locale.get("guitacho.km"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else if (result[0] > 1000) {
					mLcdFont.drawFloat(g, (int)((result[0] / 100.0f) + 0.5) / 
							10.0f, 1, maxX - mKmWidth - 1, minY + yDiff - 6);
					g.drawString(Locale.get("guitacho.km"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else {
					// Using width of "km" to avoid jumping of number between m and km ranges.
					mLcdFont.drawInt(g, (int)(result[0] + 0.5), 
							maxX - mKmWidth - 1, minY + yDiff - 6);
					g.drawString(Locale.get("guitacho.m"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
				}
			} else {
				if (result[0] > 160934) {
					mLcdFont.drawInt(g, (int)((result[0] / 1609.3f) + 0.5), 
							maxX - mMiWidth -1, minY + yDiff - 6);
					g.drawString(Locale.get("guitacho.mi"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else if (result[0] > 1609) {
					mLcdFont.drawFloat(g, (int)((result[0] / 160.9f) + 0.5) / 
							10.0f, 1, maxX - mMiWidth - 1, minY + yDiff - 6);
					g.drawString(Locale.get("guitacho.mi"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
				} else {
					// Using width of "km" to avoid jumping of number between m and km ranges.
					mLcdFont.drawInt(g, (int)(result[0] + 0.5), 
							maxX - mMiWidth - 1, minY + yDiff - 6);
					g.drawString(Locale.get("guitacho.yd"), maxX - 1, minY + yDiff - 3, Graphics.BOTTOM | Graphics.RIGHT);
				}
			}
		}
		g.drawLine(minX, minY + yDiff, maxX, minY + yDiff);
		return yDiff;
	}

	protected int paintSun(Graphics g, int minX, int minY, int maxX, int maxY, int yDiff) {
		//mLcdFont.setFontSize(18);
		g.drawLine(minX + (maxX - minX) /2, minY + yDiff - 24, minX + (maxX -minX) / 2, maxY);
		if (mSunRiseset != null) {
			g.drawString(Locale.get("guitrip.Sunrise")/*Sunrise: */ + mSunCalc.formatTime(mSunRiseset[SunCalc.RISE]), 
						 minX + ((maxX - minX) / 2) - 3, minY + yDiff, Graphics.BOTTOM | Graphics.RIGHT);

			g.drawString(Locale.get("guitrip.Sunset")/*Sunset: */ + mSunCalc.formatTime(mSunRiseset[SunCalc.SET]), 
					 	 maxX - 3, minY + yDiff, Graphics.BOTTOM | Graphics.RIGHT);
		} else {
			g.drawString(Locale.get("guitrip.SunriseNA")/*Sunrise: N/A*/, minX + ((maxX - minX) / 2) - 3, minY + yDiff, 
						 Graphics.BOTTOM | Graphics.RIGHT);
			g.drawString(Locale.get("guitrip.SunsetNA")/*Sunset: N/A*/, maxX - 3, minY + yDiff, 
					 	 Graphics.BOTTOM | Graphics.RIGHT);
		}
		return yDiff;
	}

	protected void calcSun(Trace trace) {
		// Calculate sunrise and sunset times at the first time
		// or when the map position changed more than 10 km
		if ((mSunCalc == null) ||
			( 	mSunCalc != null
			 &&	Math.abs(ProjMath.getDistance(trace.center.radlat,
											  trace.center.radlon,
											  mSunCalc.getLatitude() * MoreMath.FAC_DECTORAD,
											  mSunCalc.getLongitude() * MoreMath.FAC_DECTORAD
				) ) > 10000)
		) {
			if (mSunCalc == null) {
				mSunCalc = new SunCalc();
			}
			mSunCalc.setLatitude(trace.center.radlat * MoreMath.FAC_RADTODEC);
			mSunCalc.setLongitude(trace.center.radlon * MoreMath.FAC_RADTODEC);
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
		Position pos = mParent.getCurrentPosition();
		
		y = paintTrip(g, 0, 0, w, h, y, pos, mParent.getDestination(), mParent);

		calcSun(mParent);

		// Draw sunrise and sunset time
		y += 24;
		y = paintSun(g, 0, 0, w, h, y);
	}

	public void show() {
		// if device has a touch screen never use fullscreen mode as we have no Next-Button
		setFullScreenMode(!hasPointerEvents() && Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN));
		ShareNav.getInstance().show(this);
		synchronized (mParent.locationUpdateListeners) {
			mParent.locationUpdateListeners.addElement(this);
		}
	}

	public boolean isSunUp(Trace trace, int timeNow) {
		calcSun(trace);
		double timeDouble = timeNow / 60f;
		if (timeDouble < mSunRiseset[SunCalc.RISE]) {
			return false;
		}
		if (timeDouble > mSunRiseset[SunCalc.SET]) {
			return false;
		}
		return true;
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
