package de.ueller.gps.tools;

import java.util.Calendar;
import java.util.Date;

public class HelperRoutines {
	
	/**
	 * Formats an integer to 2 digits, as used for example in time.
	 * I.e. a 0 gets printed as 00. 
	 **/
	public static final String formatInt2(int n) {
		if (n < 10) {
			return "0" + n;
		} else {
			return Integer.toString(n);
		}
	}
	
	/**
	 * Date-Time formater that corresponds to the standard UTC time as used in XML
	 * @param time
	 * @return
	 */
	public static final String formatUTC(Date time) {
		// This function needs optimising. It has a too high object churn.
		Calendar c = null;
		if (c == null)
			c = Calendar.getInstance();
		c.setTime(time);
		return c.get(Calendar.YEAR) + "-" + formatInt2(c.get(Calendar.MONTH) + 1) + "-" +
		formatInt2(c.get(Calendar.DAY_OF_MONTH)) + "T" + formatInt2(c.get(Calendar.HOUR_OF_DAY)) + ":" +
		formatInt2(c.get(Calendar.MINUTE)) + ":" + formatInt2(c.get(Calendar.SECOND)) + "Z";		 
		
	}
	
	/**
	 * Creates a string of the current time of the format YYYY-MM-DD_hh-mm
	 * 
	 * @return
	 */
	public static final String formatSimpleDateNow() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		
		//Construct a track name from the current time
		StringBuffer formatedStr = new StringBuffer();
		formatedStr.append(cal.get(Calendar.YEAR)).append("-").append(formatInt2(cal.get(Calendar.MONTH) + 1));
		formatedStr.append("-").append(formatInt2(cal.get(Calendar.DAY_OF_MONTH))).append("_");
		formatedStr.append(formatInt2(cal.get(Calendar.HOUR_OF_DAY))).append("-").append(formatInt2(cal.get(Calendar.MINUTE)));
		
		return formatedStr.toString();
	}
	
	/**
	 * 
	 */
	public static final String formatDistance(float dist) {
		if (dist < 100) {
			return Integer.toString((int)dist) + "m";
		} else if (dist < 1000) {
			return Integer.toString((int)(dist/10)*10) + "m";
		} else if (dist < 10000) {
			return Float.toString(((int)(dist/100))/10.0f) + "km";
		} else {
			return Integer.toString((int)(dist/1000)) + "km";			
		}
	}
	

}
