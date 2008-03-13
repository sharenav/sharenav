package de.ueller.midlet.gps.routing;

public class Connection {
	/**
	 * represent time in s or length in m depending on the search mode
	 */
	public int cost;
//	public Integer toId=null;
	public int toId=-1;
	public RouteNode to=null;
	public byte startBearing=0;
	public byte endBearing=0;
	
	public Connection(){
	}
	

	public Connection(RouteNode to, int cost, byte bs, byte be) {
	this.to = to;
	this.toId=to.id;
	this.cost=cost;
	this.startBearing=bs;
	this.endBearing=be;
}

	public String toString(){
		return "connection to " + to; 
	}

}
