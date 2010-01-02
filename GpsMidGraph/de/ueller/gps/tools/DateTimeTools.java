package de.ueller.gps.tools;
/*
 * GpsMid - Copyright (c) 2010 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import de.ueller.midlet.gps.Logger;

public class DateTimeTools  {
	private final static int milliSecsPerSecond = 1000; // 1000 milliSecs;
	private final static int milliSecsPerMinute = 60 * milliSecsPerSecond; // 60 seconds * 1000 milliSecs;
	private final static int milliSecsPerHour = 60 * milliSecsPerMinute; // 60 minutes * milliSecsPerHour;
	private final static int milliSecsPerDay = 24 * milliSecsPerHour; 

	private static int differenceMilliSecs = -1;
	private static long lastDateCallMillis = 0;


	/** returns a string containing the local clock time, e.g. "20:15"
	 *  we have our own code for calculating the clock time that calls "new Date()" only initially and once per minute
	 *  because "new Date()" is very slow on some Nokia devices and thus not suited for repeated calls
	 */ 
	public static String getClock(long timeMillisGMT) {
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
	
		return hoursLocal + ":" + formatInt2(minutesLocal);
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
