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
	public static final String formatSimpleDateSecondNow() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		
		//Construct a track name from the current time
		StringBuffer formatedStr = new StringBuffer();
		formatedStr.append(cal.get(Calendar.YEAR)).append("-").append(formatInt2(cal.get(Calendar.MONTH) + 1));
		formatedStr.append("-").append(formatInt2(cal.get(Calendar.DAY_OF_MONTH))).append("_");
		formatedStr.append(formatInt2(cal.get(Calendar.HOUR_OF_DAY))).append("-").append(formatInt2(cal.get(Calendar.MINUTE)))
			.append("-").append(formatInt2(cal.get(Calendar.SECOND)));
		
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
		float [] tmpArray = new float[size];
		System.arraycopy(array, 0, tmpArray, 0, size);
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = 0; i < size - 1; i++) {
				if (tmpArray[i] > tmpArray[i+1]) {
					changed = true;
					float tmp = tmpArray[i];
					tmpArray[i] = tmpArray[i + 1];
					tmpArray[i+1] = tmp;					
				}
			}
		}
		return tmpArray[size/2];
	}
	
	public static void copyInt2ByteArray(byte[] array, int pos, int val) {
		array[pos] = (byte)val;
		array[pos + 1] = (byte)(val >> 8);
		array[pos + 2] = (byte)(val >> 16);
		array[pos + 3] = (byte)(val >> 24);
	}
	

}
