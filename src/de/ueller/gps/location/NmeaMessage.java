/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 * See file COPYING.
 */

package de.ueller.gps.location;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import de.ueller.gps.Satellite;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Position;
import de.ueller.util.Logger;
import de.ueller.util.StringTokenizer;

import de.enough.polish.util.Locale;

/**
 * This class takes NMEA0183 compliant sentences and extracts information from them:
 * <pre>
 * Geographic Location in Lat/Lon
 *  field #:  0   1      2  3       4
 * sentence: GLL,####.##,N,#####.##,W
 *1, Lat (deg, min, hundredths); 2, North or South; 3, Lon; 4, West or East.
 * 
 *Geographic Location in Time Differences
 *  field #:  0   1       2       3       4       5
 * sentence: GTD,#####.#,#####.#,#####.#,#####.#,#####.#
 *1-5, TD's for secondaries 1 through 5, respectively.
 * 
 *Bearing to Dest wpt from Origin wpt
 *  field #:  0   1  2  3  4  5    6
 * sentence: BOD,###,T,###,M,####,####
 *1-2, brg,True; 3-4, brg, Mag; 5, dest wpt; 6, org wpt.
 * 
 *Vector Track and Speed Over Ground (SOG)
 *  field #:  0   1  2  3  4  5   6  7   8
 * sentence: VTG,###,T,###,M,##.#,N,##.#,K
 *1-2,  brg,  True; 3-4, brg, Mag; 5-6, speed, kNots;  7-8,  speed, Kilometers/hr.
 * 
 *Cross Track Error
 *  field #:  0  1 2  3   4 5
 * sentence: XTE,A,A,#.##,L,N
 *1, blink/SNR (A=valid, V=invalid); 2, cycle lock (A/V); 3-5, dist
 *off, Left or Right, Nautical miles or Kilometers.
 * 
 *Autopilot (format A)
 *  field #:  0  1 2  3   4 5 6 7  8  9  10
 * sentence: APA,A,A,#.##,L,N,A,A,###,M,####
 *1,  blink/SNR (A/V); 2 cycle lock (A/V); 3-5, dist off,  Left  or
 *Right, Nautical miles or Kilometers; 6-7, arrival circle, arrival
 *perpendicular (A/V); 8-9, brg, Magnetic; 10, dest wpt.
 * 
 *Bearing to Waypoint along Great Circle
 * fld:  0   1      2      3  4       5  6  7  8  9  10  11  12
 * sen: BWC,HHMMSS,####.##,N,#####.##,W,###,T,###,M,###.#,N,####
 *1, Hours, Minutes, Seconds of universal time code; 2-3, Lat, N/S;
 *4-5,  Lon,  W/E;  6-7, brg, True; 8-9, brg,  Mag;  10-12,  range,
 *Nautical miles or Kilometers, dest wpt.
 * 
 *BWR:  Bearing  to Waypoint, Rhumbline, BPI: Bearing to  Point  of
 *Interest, all follow data field format of BWC.
 *</pre>
 */
public class NmeaMessage {
	/** Logger class for error messages */
	protected static final Logger logger = Logger.getInstance(NmeaMessage.class, Logger.TRACE);
	
	/** Buffer holding the current NMEA sentence (without the leading "GP" which
	 * identifies the source to be a GPS receiver */
	public StringBuffer buffer = new StringBuffer(80);
	
	/** The character which separates the fields of the NMEA sentence */
	private static String spChar = ",";

	/** The current heading in degrees */
	private float head;
	/** Speed in m/s (converted from knots) */
	private float speed;
	/** Altitude above mean sea level in meters */
	private float alt;
	/** Positional dilution of precision */
	private float pdop;
	/** This will receive the information extracted from NMEA. */
	private final LocationMsgReceiver receiver;
	/** The number of satellites received (field 6 from message GGA) */
	private int mAllSatellites;
	/** The number of satellites received (from GSV) */
	private int gsvSatellites;
	/** Quality of data, 0 = no fix, 1 = GPS, 2 = DGPS */
	private int qual;
	/** Flag if last received message was GSV */
	private boolean lastMsgGSV = false;
	/** Array with information about the satellites */
	private final Satellite satellites[] = new Satellite[12];
	/** The last received GPS time and date */
	private Date dateDecode = new Date();
	/** The last received position */
	private final Position pos = new Position(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1, 0);
	/** Needed to turn GPS time and date (which are in UTC) into timeMillis */
	private final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
	/** Count exceptions and stop giving them when there's a bunch */
	private int exceptionCount;

	public NmeaMessage(LocationMsgReceiver receiver) {
		this.receiver = receiver;
	}

	public StringBuffer getBuffer() {
		return buffer;
	}
	
	public void decodeMessage() {
		decodeMessage(buffer.toString(), true, true);
	}

	public Position getPosition() {
		return pos;
	}

	public int getMAllSatellites() {
		return mAllSatellites;
	}

	/** This method does the actual decoding work. It puts the data into
	 * member variables and forwards it to the LocationMsgReceiver.
	 * 
	 * @param nmea_sentence The NMEA sentence to be decoded
	 * @param receivePositionIsAllowed Set to true if it is allowed to forward 
	 * 		a new position from the NMEA sentence to the LocationMsgReceiver.
	 * @param receiveSatellitesIsAllowed Set to true if it is allowed to forward 
	 * 		satellite info from the NMEA sentence to the LocationMsgReceiver.
	 */
	public void decodeMessage(String nmea_sentence, boolean receivePositionIsAllowed,
				  boolean receiveSatellitesIsAllowed) {
		
        Vector param = StringTokenizer.getVector(nmea_sentence, spChar);
		String sentence = (String)param.elementAt(0);
		try {
//			receiver.receiveMessage("got " + buffer.toString() );
			if (lastMsgGSV && ! "GSV".equals(sentence)) {
				if (receiveSatellitesIsAllowed) {
					receiver.receiveSatellites(satellites);
				}
	            lastMsgGSV = false;
			}
			if ("GGA".equals(sentence)) {
				// time
				// Time of when fix was taken in UTC
				int time_tmp = (int)getFloatToken((String)param.elementAt(1));
				cal.set(Calendar.SECOND, time_tmp % 100);
				cal.set(Calendar.MINUTE, (time_tmp / 100) % 100);
				cal.set(Calendar.HOUR_OF_DAY, (time_tmp / 10000) % 100);
				
				// lat
				float lat = getLat((String)param.elementAt(2));
				if ("S".equals(param.elementAt(3))) {
					lat = -lat;
				}
				// lon
				float lon = getLon((String)param.elementAt(4));
				if ("W".equals(param.elementAt(5))) {
					lon = -lon;
				}
				// quality
				qual = getIntegerToken((String)param.elementAt(6));
				
				// no of Sat;
				mAllSatellites = getIntegerToken((String)param.elementAt(7));
				
				// Relative accuracy of horizontal position
				pos.hdop = getFloatToken((String)param.elementAt(8));

				// meters above mean sea level
				alt = getFloatToken((String)param.elementAt(9));
				// Height of geoid above WGS84 ellipsoid
			} else if ("RMC".equals(sentence)) {
				/* RMC encodes the recomended minimum information */
				 
				// Time of when fix was taken in UTC
				int time_tmp = (int)getFloatToken((String)param.elementAt(1));
				cal.set(Calendar.SECOND, time_tmp % 100);
				cal.set(Calendar.MINUTE, (time_tmp / 100) % 100);
				cal.set(Calendar.HOUR_OF_DAY, (time_tmp / 10000) % 100);
				
				// Status A=active or V=Void.
				String valSolution = (String)param.elementAt(2);
				if (valSolution.equals("V")) {
					this.qual = 0;
					receiver.receiveStatus(LocationMsgReceiver.STATUS_NOFIX, mAllSatellites);
					return;
				}
				if (valSolution.equalsIgnoreCase("A") && this.qual == 0) {
					this.qual = 1;
				}
				// Latitude
				float lat = getLat((String)param.elementAt(3));
				if ("S".equals(param.elementAt(4))) {
					lat =  -lat;
				}
				// Longitude
				float lon = getLon((String)param.elementAt(5));
				if ("W".equals(param.elementAt(6))) {
					lon = -lon;
				}
				// Speed over the ground in knots, but GpsMid uses m/s
				speed = getFloatToken((String)param.elementAt(7)) * 0.5144444f;
			    // Heading in degrees
				head = getFloatToken((String)param.elementAt(8));
				// Date
				int date_tmp = getIntegerToken((String)param.elementAt(9));
				cal.set(Calendar.YEAR, 2000 + date_tmp % 100);
				cal.set(Calendar.MONTH, ((date_tmp / 100) % 100) - 1);
				cal.set(Calendar.DAY_OF_MONTH, (date_tmp / 10000) % 100);
			    // Magnetic Variation is not used
				
				// Copy data to current position
				pos.latitude = lat;
				pos.longitude = lon;
				pos.altitude = alt;
				pos.speed = speed;
				pos.course = head;
				//pos.pdop = pdop;
				pos.type = Position.TYPE_GPS;

				// Get Date from Calendar
				dateDecode = cal.getTime();
				// Get milliSecs since 01-Jan-1970 from Date
				pos.gpsTimeMillis = dateDecode.getTime();
				// FIXME? does this make sense? Not to me, elsewhere pos.timeMillis is from system time, not GPS time.
				// in practice, this creates confusion at least in GuiTacho, which can show either system time or GPS
				// time on the tacho display
				pos.timeMillis = pos.gpsTimeMillis;
				
				if (receivePositionIsAllowed) {
					receiver.receivePosition(pos);
				}
				// TODO: Possible to differentiate 2D and 3D fix?
				if (this.qual > 1) {
					receiver.receiveStatus(LocationMsgReceiver.STATUS_DGPS, mAllSatellites);
				} else {
					receiver.receiveStatus(LocationMsgReceiver.STATUS_3D, mAllSatellites);
				}
			} else if ("VTG".equals(sentence)) {
				if (!"nan".equals((String)param.elementAt(1))) {
					head = getFloatToken((String)param.elementAt(1));
				}
				//Convert from knots to m/s
				if (!"nan".equals((String)param.elementAt(7))) {
					speed = getFloatToken((String)param.elementAt(7)) * 0.5144444f;
				}
			} else if ("GSA".equals(sentence)) {
				//#debug trace
				logger.trace("Decoding GSA");
				/**
				 * Encodes Satellite status
				 * 
				 * Auto selection of 2D or 3D fix (M = manual)
				 * 3D fix - values include: 1 = no fix
			     *                          2 = 2D fix
			     *                          3 = 3D fix
				 */
				/**
				 * A list of up to 12 PRNs which are used for the fix
				 */
				for (int j = 0; j < 12; j++) {
					/**
					 * Resetting all the satellites to non locked
					 */
					if ((satellites[j] != null)) {
						satellites[j].isLocked(false);
					}
				}
				for (int i = 0; i < 12; i++) {
					int prn = getIntegerToken((String)param.elementAt(i + 3));
					if (prn != 0) {
						//#debug debug
						logger.debug("Satelit " + prn + " is part of fix");
						for (int j = 0; j < 12; j++) {
							if ((satellites[j] != null) && (satellites[j].id == prn)) {
								satellites[j].isLocked(true);
							}
						}
					}
				}
				/**
				 * PDOP & HDOP (dilution of precision)
				 */
				pos.pdop = getFloatToken((String)param.elementAt(15));
				pos.hdop = getFloatToken((String)param.elementAt(16));
				pos.vdop = getFloatToken((String)param.elementAt(17));
				/**
			     *  Horizontal dilution of precision (HDOP)
			     *  Vertical dilution of precision (VDOP)
				 */
			} else if ("GSV".equals(sentence)) {
				/* GSV encodes the satellites that are currently in view
				 * A maximum of 4 satellites are reported per message,
				 * if more are visible, then they are split over multiple messages				 *
				 */
	            int j;
	            // Calculate which satellites are in this message (message number * 4)
	            j = (getIntegerToken((String)param.elementAt(2)) - 1) * 4;
		    if (getIntegerToken((String)param.elementAt(2)) == 1) {
			    gsvSatellites = 0;
		    }
	            int noSatInView =(getIntegerToken((String)param.elementAt(3)));
	            for (int i = 4; i < param.size() && j < 12; i += 4, j++) {
	            	if (satellites[j] == null) {
	            		satellites[j] = new Satellite();
	            	}
	            	satellites[j].id = getIntegerToken((String)param.elementAt(i));
	            	satellites[j].elev = getIntegerToken((String)param.elementAt(i + 1));
	            	satellites[j].azimut = getIntegerToken((String)param.elementAt(i + 2));
	            	satellites[j].snr = getIntegerToken((String)param.elementAt(i + 3));
			/** The number of satellites received (from GSV) */
			if (satellites[j].snr != 0) {
				gsvSatellites++;
			}
	            }
	            lastMsgGSV = true;
	            for (int i = noSatInView; i < 12; i++) {
	            	satellites[i] = null;
	            }
	            if (getIntegerToken((String)param.elementAt(2)) == getIntegerToken((String)param.elementAt(1))) {
			    mAllSatellites = gsvSatellites;
			    if (receiveSatellitesIsAllowed) {
				    receiver.receiveSatellites(satellites);
			    }
		            lastMsgGSV = false;
	            }
			}
		} catch (RuntimeException e) {
			if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SHOW_NMEA_ERRORS) && checkExceptionCount()) {
				logger.exception(Locale.get("nmeamessage.ErrorDecoding")/*Error while decoding */ + sentence, e);
			}
		}
	}

	private boolean checkExceptionCount() {
		exceptionCount++;
		if (exceptionCount < 3) {
			return true;
		} else if (exceptionCount == 3) {
			logger.error(Locale.get("nmeamessage.TooManyErrors")/*Too many exceptions, stoppping reporting them*/);
			return false;
		} else {
			return false;
		}
	}

	private int getIntegerToken(String s) {
		if (s == null || s.length() == 0) {
			return 0;
		}
		return Integer.parseInt(s);
	}

	private float getFloatToken(String s) {
		if (s == null || s.length() == 0) {
			return 0;
		}
		return Float.parseFloat(s);
	}

	private float getLat(String s) {
		if (s.length() < 2) {
			return 0.0f;
		}
		int lat = Integer.parseInt(s.substring(0, 2));
		float latf = Float.parseFloat(s.substring(2));
		return (lat + (latf / 60));
	}

	private float getLon(String s) {
		if (s.length() < 3) {
			return 0.0f;
		}
		int lon = Integer.parseInt(s.substring(0, 3));
		float lonf = Float.parseFloat(s.substring(3));
		return (lon + (lonf / 60));
	}
}
