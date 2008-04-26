package de.ueller.gps.nmea;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
/**
 * Geographic Location in Lat/Lon
 *  field #:  0   1      2  3       4
 * sentence: GLL,####.##,N,#####.##,W
 *1, Lat (deg, min, hundredths); 2, North or South; 3, Lon; 4, West 
 *or East.
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
 *1-2,  brg,  True; 3-4, brg, Mag; 5-6, speed, kNots;  7-8,  speed, 
 *Kilometers/hr.
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
 *
 */
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import de.ueller.gps.data.Position;
import de.ueller.gps.data.Satelit;
import de.ueller.gps.tools.StringTokenizer;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;

public class NmeaMessage {
	protected static final Logger logger = Logger.getInstance(NmeaMessage.class,Logger.TRACE);
	public StringBuffer buffer=new StringBuffer(80);
	private static String spChar=",";
	private float head,speed,alt;
	private final LocationMsgReceiver receiver;
	private int mAllSatellites;
	private int qual;
	private boolean lastMsgGSV=false;
	private Satelit satelit[]=new Satelit[12];
	private Calendar cal = Calendar.getInstance();
	public NmeaMessage(LocationMsgReceiver receiver) {
		this.receiver = receiver;
	}

	public StringBuffer getBuffer() {
		return buffer;
	}	
	
	public void decodeMessage() {
		decodeMessage(buffer.toString());
	}
	public void decodeMessage(String nmea_sentence) {
		
        Vector param = StringTokenizer.getVector(nmea_sentence, spChar);
		String sentence=(String)param.elementAt(0);
		try {
//			receiver.receiveMessage("got "+buffer.toString() );
			if (lastMsgGSV && ! "GSV".equals(sentence)){
	            receiver.receiveStatelit(satelit);
	            //satelit=new Satelit[12];
	            lastMsgGSV=false;
			}
			if ("GGA".equals(sentence)){
				// time
				// Time of when fix was taken in UTC
				int time_tmp = (int)getFloatToken((String)param.elementAt(1));
				cal.set(Calendar.SECOND, time_tmp % 100);
				cal.set(Calendar.MINUTE, (time_tmp / 100) % 100);
				cal.set(Calendar.HOUR_OF_DAY, (time_tmp / 10000) % 100);
				
				// lat
				float lat=getLat((String)param.elementAt(2));
				if ("S".equals((String)param.elementAt(3))){
					lat= -lat;
				}
				// lon
				float lon=getLon((String)param.elementAt(4));
				if ("W".equals((String)param.elementAt(5))){
					lon=-lon;
				}
				// quality
				qual = getIntegerToken((String)param.elementAt(6));
				
				// no of Sat;
				mAllSatellites = getIntegerToken((String)param.elementAt(7));
				
				// Relative accuracy of horizontal position
				
				// meters above mean sea level
				alt=getFloatToken((String)param.elementAt(9));
				// Height of geoid above WGS84 ellipsoid				
			} else if ("RMC".equals(sentence)){
				/* RMC encodes the recomended minimum information */
				 
				// Time of when fix was taken in UTC
				int time_tmp = (int)getFloatToken((String)param.elementAt(1));
				cal.set(Calendar.SECOND, time_tmp % 100);
				cal.set(Calendar.MINUTE, (time_tmp / 100) % 100);
				cal.set(Calendar.HOUR_OF_DAY, (time_tmp / 10000) % 100);
				
				//Status A=active or V=Void.
				String valSolution = (String)param.elementAt(2);
				if (valSolution.equals("V")) {
					this.qual = 0;
					receiver.receiveSolution("NoFix");
					return;
				}
				if (valSolution.equalsIgnoreCase("A") && this.qual == 0) this.qual = 1;
				
				//Latitude
				float lat=getLat((String)param.elementAt(3));
				if ("S".equals((String)param.elementAt(4))){
					lat= -lat;
				}
				//Longitude
				float lon=getLon((String)param.elementAt(5));
				if ("W".equals((String)param.elementAt(6))){
					lon=-lon;
				}				
				//Speed over the ground in knots
				//GpsMid uses m/s				
				speed=getFloatToken((String)param.elementAt(7))*0.5144444f;
			    //Track angle in degrees
				head=getFloatToken((String)param.elementAt(8));
				//Date
				int date_tmp = getIntegerToken((String)param.elementAt(9));				
				cal.set(Calendar.YEAR, 2000 + date_tmp % 100);
				cal.set(Calendar.MONTH, ((date_tmp / 100) % 100) - 1);
				cal.set(Calendar.DAY_OF_MONTH, (date_tmp / 10000) % 100);				
			    //Magnetic Variation
				Position p=new Position(lat,lon,alt,speed,head,0,cal.getTime());
				receiver.receivePosItion(p);
				if (this.qual > 1) {
					receiver.receiveSolution("D" + mAllSatellites + "S");
				} else {
					receiver.receiveSolution(mAllSatellites + "S");
				}
			} else if ("VTG".equals(sentence)){
				head=getFloatToken((String)param.elementAt(1));
				//Convert from knots to m/s
				speed=getFloatToken((String)param.elementAt(7))*0.5144444f;
			} else if ("GSA".equals(sentence)){
				logger.info("Decoding GSA");
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
					 * Resetting all the satelits to non locked
					 */
					if ((satelit[j] != null)) {
						satelit[j].isLocked(false);						
					}
				}
				for (int i = 0; i < 12; i++) {
					int prn = getIntegerToken((String)param.elementAt(i + 3));
					if (prn != 0) {
						logger.info("Satelit " + prn + " is part of fix");
						for (int j = 0; j < 12; j++) {
							if ((satelit[j] != null) && (satelit[j].id == prn)) {
								satelit[j].isLocked(true);				
							}
						}
					}
				}
				/**
				 *	PDOP (dilution of precision) 
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
	            j=(getIntegerToken((String)param.elementAt(2))-1)*4;
	            int noSatInView =(getIntegerToken((String)param.elementAt(3)));	            
	            for (int i=4; i < param.size() && j < 12; i+=4, j++) {
	            	if (satelit[j]==null){
	            		satelit[j]=new Satelit();
	            	}
	                satelit[j].id=getIntegerToken((String)param.elementAt(i));
	                satelit[j].elev=getIntegerToken((String)param.elementAt(i+1));
	                satelit[j].azimut=getIntegerToken((String)param.elementAt(i+2));
	                satelit[j].snr=getIntegerToken((String)param.elementAt(i+3));	                
	            }
	            lastMsgGSV=true;
	            for (int i = noSatInView; i < 12; i++) {
	            	satelit[i] = null;
	            }
			}
		} catch (RuntimeException e) {
			logger.error("Error while decoding "+sentence + " " + e.getMessage());
		}
		
	}

	private int getIntegerToken(String s){
		if (s==null || s.length()==0)
			return 0;
		return Integer.parseInt(s);
	}

	private float getFloatToken(String s){
		if (s==null || s.length()==0)
			return 0;
		return Float.parseFloat(s);
	}

	private float getLat(String s){
		if (s.length() < 2)
			return 0.0f;
		int lat=Integer.parseInt(s.substring(0,2));
		float latf=Float.parseFloat(s.substring(2));
		return lat+latf/60;
	}

	private float getLon(String s){
		if (s.length() < 3)
			return 0.0f;
		int lon=Integer.parseInt(s.substring(0,3));
		float lonf=Float.parseFloat(s.substring(3));
		return lon+lonf/60;
	}	
}
