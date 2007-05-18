package de.ueller.gps.nmea;
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
import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.LocationMsgReceiver;

public class NmeaMessage {
	public StringBuffer buffer=new StringBuffer(80);
	private int tokenPointer;
	private int spChar=',';
	private final LocationMsgReceiver receiver;
	public NmeaMessage(LocationMsgReceiver receiver) {
		this.receiver = receiver;
	}

	public StringBuffer getBuffer() {
		return buffer;
	}

	public void decodeMessage() {
		int tp=0;
		String sentence=getStringToken();
		receiver.receiveMessage("got "+sentence );
		if ("GLL".equals(sentence)){
			float lat=getLat();
			if (getStringToken().startsWith("S")){
				lat *= -1f;
			}
			float lon=getLon();
			if (getStringToken().startsWith("W")){
				lat *= -1f;
			}

			Position p=new Position(lat,lon,0f,0f,0f,0,null);
			receiver.receivePosItion(p);
		}
		
	}
	
	private int nextSubstring(){
		return buffer.toString().indexOf(spChar, tokenPointer);
	}
	private String getStringToken(){
		int end=nextSubstring();
		String ret=buffer.toString().substring(tokenPointer, end-1);
		tokenPointer=end;
		return ret;
	}
	private int getIntegerToken(){
		return Integer.parseInt(getStringToken());
	}
	private float getFloatToken(){
		return Float.parseFloat(getStringToken());
	}
	private float getLat(){
		String s=getStringToken();
		int lat=Integer.parseInt(s.substring(0,1));
		float latf=Float.parseFloat(s.substring(2));
		return lat+latf/60;
	}
	private float getLon(){
		String s=getStringToken();
		int lon=Integer.parseInt(s.substring(0,2));
		float lonf=Float.parseFloat(s.substring(3));
		return lon+lonf/60;
	}
	
}
