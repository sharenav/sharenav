package de.ueller.midlet.gps.routing;

public class ConnectionWithNode {
	public int cost;
	public RouteNode to=null;
	public byte startBearing=0;
	public byte endBearing=0;
	public byte wayRouteInstruction=0;
	public int wayNameIdx=0; // used to vaguely identify ways that might contain a solution path for highlighting  
	public short wayFromConAt=0;
	public short wayToConAt=0;
	public byte wayType=0;
	public byte wayRouteFlags=0;
	public float wayDistanceToNext=Float.MAX_VALUE;
	
	public ConnectionWithNode(RouteNode n,Connection c){
		this.to=n;
		this.cost=c.cost;
		this.startBearing=c.startBearing;
		this.endBearing=c.endBearing;

	}
}
