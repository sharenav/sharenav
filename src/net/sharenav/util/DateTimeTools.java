package net.sharenav.util;
/*
 * ShareNav - Copyright (c) 2010 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeTools  {
	private final static int milliSecsPerSecond = 1000; // 1000 milliSecs;
	private final static int milliSecsPerMinute = 60 * milliSecsPerSecond; // 60 seconds * 1000 milliSecs;
	private final static int milliSecsPerHour = 60 * milliSecsPerMinute; // 60 minutes * milliSecsPerHour;
	private final static int milliSecsPerDay = 24 * milliSecsPerHour; 

	private static int differenceMilliSecs = -1;
	private static long lastDateCallMillis = 0;

	private static long lastDaysUTC = -1;
	private static String dateUTC;

	
	/** returns a string containing the local clock time, e.g. "20:15"
	 *  we have our own code for calculating the clock time that calls "new Date()" only initially and once per minute
	 *  because "new Date()" is very slow on some Nokia devices and thus not suited for repeated calls
	 */ 
	public static String getClock(long timeMillisGMT, boolean noSeconds) {
		long currentTimeMillis = System.currentTimeMillis();
		/* calculate the real difference between local time and GMT initially and once per minute */
		if ( Math.abs(currentTimeMillis - lastDateCallMillis) >= milliSecsPerMinute ) {
			/* calculate current GMT hours and minutes with modulo method */
			int currentMilliSecsSinceMidnightGMT = (int) (currentTimeMillis % milliSecsPerDay);		
			int currentHoursGMT = (int) (currentMilliSecsSinceMidnightGMT / milliSecsPerHour);
			int currentMinutesGMT = (int) ((currentMilliSecsSinceMidnightGMT / 1000 / 60) %  60);	
			/* calculate current local time from new Date() */
			Calendar currentTime = Calendar.getInstance();
			currentTime.setTime( new Date( currentTimeMillis ) );		
			int currentHoursLocal = currentTime.get(Calendar.HOUR_OF_DAY);
			int currentMinutesLocal = currentTime.get(Calendar.MINUTE);
			/* determine the difference */
			differenceMilliSecs = (currentHoursGMT - currentHoursLocal) * milliSecsPerHour + (currentMinutesGMT - currentMinutesLocal) * milliSecsPerMinute;
			lastDateCallMillis = currentTimeMillis;
		}
		/* own modulo and difference-to-GMT-based routines for calculating the local hour and minute of day for timeMillisGMT */
		int milliSecsSinceMidnightLocal = (int) ((timeMillisGMT - differenceMilliSecs) % milliSecsPerDay);
		int hoursLocal = (int) ((milliSecsSinceMidnightLocal / milliSecsPerHour) % 24);
		int minutesLocal = ((milliSecsSinceMidnightLocal / milliSecsPerMinute) % 60);
	
		if (noSeconds) {
			return hoursLocal + ":" + formatInt2(minutesLocal);
		} else {
			int secondsLocal = ((milliSecsSinceMidnightLocal / milliSecsPerSecond) % 60);
			return hoursLocal + ":" + formatInt2(minutesLocal) + ":" + formatInt2(secondsLocal);
		}
	}

	
	/** returns a string containing the UTC-Date-time, e.g. "2009-11-20-T20:15Z"
	 *  we have our own code for calculating the clock time that calls "new Date()" only seldom
	 *  because "new Date()" is very slow on some Nokia devices and thus not suited for frequent calls
	 */ 
	public static String getUTCDateTime(long timeMillisUTC) {
		// calculate the days since 01-01-1970 for timeMillisUTC
		long daysUTC = timeMillisUTC / milliSecsPerDay;		
		/*
		 * when the days since 01-01-1970 have changed, recalculate the dateUTC string
		 */
		if (lastDaysUTC != daysUTC ) {
			Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT")); // GMT and UTC only differ up to 0.9 secs
			c.setTime( new Date( timeMillisUTC ) );		
			dateUTC = c.get(Calendar.YEAR) + "-" + formatInt2(c.get(Calendar.MONTH) + 1) + "-" + formatInt2(c.get(Calendar.DAY_OF_MONTH));
			lastDaysUTC = daysUTC;
		}		
		
		// calculate the time values
		int milliSecsSinceMidnightUTC =  (int) ((timeMillisUTC) % milliSecsPerDay);
		int hoursUTC = (milliSecsSinceMidnightUTC / milliSecsPerHour) % 24;
		int minutesUTC = (milliSecsSinceMidnightUTC / milliSecsPerMinute) % 60;
		int secondsUTC = (milliSecsSinceMidnightUTC / milliSecsPerSecond) % 60;
		
		return dateUTC + "T" + formatInt2(hoursUTC) + ":" + formatInt2(minutesUTC) + ":" + formatInt2(secondsUTC) + "Z";
	}

	/**
	 * Formats an integer to 2 digits, as used for example in time.
	 * I.e. 3 gets printed as 03. 
	 **/
	public static final String formatInt2(int n) {
		if (n < 10) {
			return "0" + n;
		} else {
			return Integer.toString(n);
		}	
	}
	
}
