/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See file COPYING.
 */

package net.sharenav.gps.location;

import de.enough.polish.util.Locale;
import net.sharenav.gps.Satellite;
import net.sharenav.sharenav.data.Position;


public interface LocationMsgReceiver {
	public static final byte SIRF_FAIL_NO_START_SIGN1 = 0;
	public static final byte SIRF_FAIL_NO_START_SIGN2 = 1;
	public static final byte SIRF_FAIL_MSG_TO_LONG = 2;
	public static final byte SIRF_FAIL_MSG_INTERUPTED = 3;
	public static final byte SIRF_FAIL_MSG_CHECKSUM_ERROR = 4;
	public static final byte SIRF_FAIL_NO_END_SIGN1 = 5;
	public static final byte SIRF_FAIL_NO_END_SIGN2 = 6;
	public static final byte SIRF_FAIL_COUNT = 7;

	public static final byte STATUS_OFF = 0; // LocationProducer is off
	public static final byte STATUS_SECEX = 1; // Security Exception occurred 
	public static final byte STATUS_NOFIX = 2; // No GPS fix
	public static final byte STATUS_RECONNECT = 3; // LocationProducer is currently reconnecting
	public static final byte STATUS_ON = 4; // On (JSR-179, num of sats unknown)
	public static final byte STATUS_2D = 5; // 2D fix
	public static final byte STATUS_3D = 6; // 3D fix
	public static final byte STATUS_DGPS = 7; // DGPS fix
	public static final byte STATUS_CELLID = 8; // CellID position
	public static final byte STATUS_MANUAL = 9; // Manually entered position
	
	/**
	 * Update of position
	 * The pos object may be reused in subsequent
	 * calls, so you need to clone it if you
	 * need a persistent state for later.
	 * @param pos
	 */
	public void receivePosition(Position pos);
	
	/** 
	 * Update of satellites in view
	 * @param sat Array of satellite data
	 */
	public void receiveSatellites(Satellite[] sats);
	
	/** 
	 * Message with state change of location producer
	 * @param s String that should be displayed directly in the GUI.
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
	 * Update of "solution" which describes the current state of reception.
	 * @param status Status of the receiver, see the STATUS_* constants for possible values.
	 * @param satsReceived Number of satellites received, may be invalid or old. Use only if
	 * 		the type of fix is 2D, 3D or DGPS.
	 */
	public void receiveStatus(byte status, int satsReceived);

    /** Notification that the location producer has stopped.
     */
	public void locationDecoderEnd();

    /** Notification that the location producer has stopped.
     * @param msg Error message for direct displaying in the GUI.
     */
	public void locationDecoderEnd(String msg);
}
