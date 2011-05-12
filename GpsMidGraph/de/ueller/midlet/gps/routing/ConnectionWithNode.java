package de.ueller.midlet.gps.routing;

public class ConnectionWithNode {
	public int cost;
	/** time in 1/5 secs to the next node */
	public short durationFSecsToNext=0;
	public RouteNode to=null;
	public byte startBearing=0;
	public byte endBearing=0;
	public byte wayRouteInstruction=0;
	public byte numToRoutableWays=0;
	public int wayNameIdx=-1; // used to vaguely identify ways that might contain a solution path for highlighting  
	public short wayFromConAt=0;
	public short wayToConAt=0;
	public short wayType=0;
	public short wayRouteFlags=0;
	public short wayConEndBearing=0;
	public short wayConStartBearing=0;
	public float wayDistanceToNext=Float.MAX_VALUE;
	public short wayTypeOfAlternativeBearingWay = 0;
	
	public ConnectionWithNode(RouteNode n,Connection c){
		this.to=n;
		this.cost=c.getCost();
		this.durationFSecsToNext = c.durationFSecs;
		this.startBearing=c.startBearing;
		this.endBearing=c.endBearing;

	}
}
