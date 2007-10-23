package de.ueller.midlet.gps.routing;

public class Connection {
	/**
	 * represent time in s or length in m depending on the search mode
	 */
	public int cost;
//	public int time;
//	public int length;
	public Integer toId=null;
	public RouteNode to=null;
	public byte startBearing=0;
	public byte endBearing=0;
	
	public Connection(){
	}
	
//	public Connection(RouteNode to, int length, int time, byte bs, byte be) {
//		this.to = to;
//		this.toId=new Integer(to.id);
//		this.length = length;
//		this.time = time;
//		this.startBearing=bs;
//		this.endBearing=be;
//	}
	public Connection(RouteNode to, int cost, byte bs, byte be) {
	this.to = to;
	this.toId=new Integer(to.id);
	this.cost=cost;
	this.startBearing=bs;
	this.endBearing=be;
}

	public String toString(){
		return "connection to " + to; 
	}

}
