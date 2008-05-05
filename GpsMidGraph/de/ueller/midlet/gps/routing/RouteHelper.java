package de.ueller.midlet.gps.routing;

import de.ueller.midlet.gps.data.Node;

public class RouteHelper {
	public Node node;
	public String name;
	public RouteHelper(float lat,float lon,String name){
		this.name=name;
		node=new Node(lat,lon,true);
	}
}
