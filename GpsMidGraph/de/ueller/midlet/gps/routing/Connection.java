package de.ueller.midlet.gps.routing;

import de.ueller.midlet.gps.Logger;

public class Connection {
	//#debug debug
	private final static Logger logger = Logger.getInstance(Connection.class, Logger.ERROR);

	/**
	 * represent time in 1/10 s or length in m depending on the search mode
	 */
	private int cost;
	/** duration in 1/5 secs (this allows more than 100 mins per connection which hopefully is enough */
	public short durationFSecs;
//	public Integer toId=null;
	public int toId=-1;
	public int connectionId=-1;
//	public RouteNode to=null;
	public byte startBearing=0;
	public byte endBearing=0;
	public byte connTravelModes=0;
	
	// the upper bits of connTravelModes are used to indicate special informations about the connection rather than which travelModes are allowed
	public static final int CONNTYPE_MAINSTREET_NET = 128;
	public static final int CONNTYPE_MOTORWAY = 64;
	public static final int CONNTYPE_TRUNK_OR_PRIMARY = 32;
	
	public Connection(){
	}
	

	public Connection(RouteNode to, int cost, byte bs, byte be, int connectionId) {
//		this.to = to;
		this.toId=to.id;
		this.connectionId=connectionId;
		setCost(cost);
		if (to.isAtTrafficSignals()) {
			setStartsAtTrafficSignals();
		}
		this.startBearing=bs;
		this.endBearing=be;
	}

	
	public void setCost(int cost) {
		this.cost &= 0x80000000;
		this.cost |= cost;
	}

	public int getCost() {
		return this.cost & 0x7FFFFFFF;
	}
	
	public boolean startsAtTrafficSignals() {
		return (this.cost & 0x80000000) != 0;
	}
	
	public void setStartsAtTrafficSignals() {
		this.cost |= 0x80000000;
	}
	
	
	
	/**
	 * @param durationTSecs: duration in 1/10 secs
	 */
	public void setDurationFSecsFromTSecs(int durationTSecs) {
		durationTSecs /= 2;
		if (durationTSecs > 32767) {
			//#debug debug
			logger.debug("connection duration too long: " + durationTSecs);
			durationTSecs = 32767;			
		}
		this.durationFSecs = (short) durationTSecs;
	}
	
	public boolean isMainStreetNet() {
		return (connTravelModes & CONNTYPE_MAINSTREET_NET) > 0;
	}
	
	public boolean isMotorwayConnection() {
		return (connTravelModes & CONNTYPE_MOTORWAY) > 0;
	}

	public boolean isTrunkOrPrimaryConnection() {
		return (connTravelModes & CONNTYPE_TRUNK_OR_PRIMARY) > 0;
	}
	
	public String toString(){
		return "connection to " + toId; 
	}

}
