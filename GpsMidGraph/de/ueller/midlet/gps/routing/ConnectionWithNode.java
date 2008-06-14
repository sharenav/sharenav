package de.ueller.midlet.gps.routing;

public class ConnectionWithNode {
	public int cost;
	public RouteNode to=null;
	public byte startBearing=0;
	public byte endBearing=0;

	public ConnectionWithNode(RouteNode n,Connection c){
		this.to=n;
		this.cost=c.cost;
		this.startBearing=c.startBearing;
		this.endBearing=c.endBearing;

	}
}
