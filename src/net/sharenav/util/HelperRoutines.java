/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.util;

import java.util.Calendar;
import java.util.Date;

import net.sharenav.sharenav.data.Configuration;

public class HelperRoutines {
	
	/**
	 * Formats an integer to 2 digits, as used for example in time.
	 * I.e. a 0 gets printed as 00.
	 **/
	public static final String formatInt2(int n) {
		if (n > 9) {
			return Integer.toString(n);
		}
		return "0" + n;		
	}

	/**
	 * Same as {@link #formatInt2(int)} but appends to specified buffer.
	 */
	private static StringBuffer appendInt2(StringBuffer buf, int n) {
		if (n < 10) {
			buf.append('0');
		}
		buf.append(n);
		return buf;
	}

	/**
	 * Append date (in YYYY-MM-DD format) to specified buffer.
	 */
	private static StringBuffer appendDate(StringBuffer buf, Calendar cal) {
		buf.append(cal.get(Calendar.YEAR)).append('-');
		appendInt2(buf, cal.get(Calendar.MONTH)+1).append('-');
		appendInt2(buf, cal.get(Calendar.DAY_OF_MONTH));
		return buf;
	}
	
	/**
	 * Date-Time formatter that corresponds to the standard UTC time as used in XML
	 * @param time
	 * @return
	 */
	public static final String formatUTC(Date time) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(time);
		StringBuffer buf = new StringBuffer(20);
		appendDate(buf, cal).append('T');
		appendInt2(buf, cal.get(Calendar.HOUR_OF_DAY)).append(':');
		appendInt2(buf, cal.get(Calendar.MINUTE)).append(':');
		appendInt2(buf, cal.get(Calendar.SECOND)).append('Z');
		return buf.toString();
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
		formatedStr.append(cal.get(Calendar.YEAR)).append("-").append(formatInt2(cal.get(Calendar.MONTH) + 1))
		           .append("-").append(formatInt2(cal.get(Calendar.DAY_OF_MONTH))).append("_")
		           .append(formatInt2(cal.get(Calendar.HOUR_OF_DAY))).append("-").append(formatInt2(cal.get(Calendar.MINUTE)));
		
		return formatedStr.toString();
	}

	public static final String formatSimpleDateSecondNow() {
		return formatSimpleDateNow(true);
	}

	private static String formatSimpleDateNow(boolean seconds) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		//Construct a track name from the current time
		StringBuffer buf = new StringBuffer(19);
		appendDate(buf, cal).append("_");
		appendInt2(buf, cal.get(Calendar.HOUR_OF_DAY)).append("-");
		appendInt2(buf, cal.get(Calendar.MINUTE));
		if (seconds) {
			appendInt2(buf.append('-'), cal.get(Calendar.SECOND));
		}
		return buf.toString();
	}

	/**
	 * 
	 */
	public static final String formatDistance(float dist) {
		if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
			if (dist < 100) {
				return Integer.toString((int)dist) + "m";
			} else if (dist < 1000) {
				return Integer.toString((int)(dist/10)*10) + "m";
			} else if (dist < 10000) {
				return Float.toString(((int)(dist/100))/10.0f) + "km";
			} else {
				return Integer.toString((int)(dist/1000)) + "km";
			}
		} else {
			int distYd = (int) (dist / 0.9144f + 0.5);
			float distMi = (dist / 1609.344f + 0.05f);
			if (distYd < 100) {
				return Integer.toString((int)distYd) + "yd";
			} else if (distYd < 1000) {
				return Integer.toString((int)(distYd/10)*10) + "yd";
			} else if (distMi < 10) {
				return Float.toString(((int)(distMi*10))/10.0f) + "mi";
			} else {
				return Integer.toString((int)(distMi)) + "mi";
			}
		}
	}
	
	/**
	 * Calculate the median value of a float array.
	 * 
	 * This is currently done in a stupidly inefficient way
	 * using bubble sort.
	 * 
	 * TODO: Need to implement a proper algorithm
	 * to find the median
	 * 
	 * @param array
	 * @param size
	 * @return
	 */
	public static float medianElement(float[] array, int size) {
		float[] tmpArray = new float[size];
		System.arraycopy(array, 0, tmpArray, 0, size);
		boolean changed;
		float ival, jval;
		do {
			changed = false;
			for (int i = size - 1, j = i; --i >= 0; j = i) {
				ival = tmpArray[i];
				jval = tmpArray[j];
				if (ival <= jval) {
					continue;
				}
				tmpArray[i] = jval;
				tmpArray[j] = ival;
				changed = true;
			}
		} while (changed);
		return tmpArray[size/2];
	}
	
	public static void copyInt2ByteArray(byte[] array, int pos, int val) {
		array[pos++] = (byte)val;
		array[pos++] = (byte)(val >> 8);
		array[pos++] = (byte)(val >> 16);
		array[pos]   = (byte)(val >> 24);
	}

	/**
	 * Replaces every occurrence of <code>search</code> with <code>replace</code>
	 * in <code>text</code>.
	 * @param text
	 * @param search
	 * @param replace
	 * @return The changed string
	 */

	public static String replaceAll(String text, String search, String replace) {
		final int textlen   = text.length();
		final int searchlen = search.length();
		StringBuffer buf = new StringBuffer(textlen);
		int start = 0, end;
		while ((end = text.indexOf(search, start)) != -1) {
			buf.append(text.substring(start, end)).append(replace);
			start = end + searchlen;
		}
		if (start < textlen) {
			buf.append(text.substring(start));
		}
		return buf.toString();
	}
	
	/**
	 * Replaces control caracters to be XML-safe
	 * 
	 * @param toxml
	 * @return
	 */
	public static String utf2xml(String toxml) {
		toxml = HelperRoutines.replaceAll(toxml,"&", "&amp;" );
		toxml = HelperRoutines.replaceAll(toxml,"<", "&lt;"  );
		toxml = HelperRoutines.replaceAll(toxml,">", "&gt;"  );
		toxml = HelperRoutines.replaceAll(toxml,"\"", "&quot;");
		return toxml;
	}
}
