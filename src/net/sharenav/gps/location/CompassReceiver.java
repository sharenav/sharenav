/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See file COPYING.
 */

package net.sharenav.gps.location;


public interface CompassReceiver {
	public static final byte SIRF_FAIL_NO_START_SIGN1 = 0;
	public static final byte SIRF_FAIL_NO_START_SIGN2 = 1;
	public static final byte SIRF_FAIL_MSG_TO_LONG = 2;
	public static final byte SIRF_FAIL_MSG_INTERUPTED = 3;
	public static final byte SIRF_FAIL_MSG_CHECKSUM_ERROR = 4;
	public static final byte SIRF_FAIL_NO_END_SIGN1 = 5;
	public static final byte SIRF_FAIL_NO_END_SIGN2 = 6;
	public static final byte SIRF_FAIL_COUNT = 7;

	/**
	 * Update of position
	 * The pos object may be reused in subsequent
	 * calls, so you need to clone it if you
	 * need a persistent state for later.
	 * @param pos
	 */
	public void receiveCompass(float azimuth);
	
	/** 
	 * Update of satellites in view
	 * @param sat Array of satellite data
	 */
	public void receiveMessage(String msg);
	
	/**
	 * Update of statistics of the location producer.
	 * Probably only useful for NMEA and SIRF.
	 * @param statRecord Each element of this array describes how often an
	 *  error occurred, see the SIRF_FAIL_XXX constants above.
	 * @param quality Value that is supposed to decrease during problems
	 *  with Bluetooth.
	 */
	public void receiveStatistics(int[] statRecord, byte quality);

	/** 
	 * Update of "solution" which describes the current state of reception in
	 * a way that can be displayed directly in the GUI like "NoFix", "4S" etc.
	 * TODO: This has to be changed to something usable in code as the
	 * location receivers need to be able to behave differently depending on
	 * the quality of the position.
	 * @param s String that should be displayed directly in the GUI.
	 */
	public void receiveCompassStatus(int status);

    /** Notification that the location producer has stopped.
     */
	public void locationDecoderEnd();

    /** Notification that the location producer has stopped.
     * @param msg Error message for direct displaying in the GUI.
     */
	public void locationDecoderEnd(String msg);
}
