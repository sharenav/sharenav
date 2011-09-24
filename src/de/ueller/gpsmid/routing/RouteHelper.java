package de.ueller.gpsmid.routing;

import de.ueller.gpsmid.data.Node;

public class RouteHelper {
	public Node node;
	public String name;
	public RouteHelper(float lat,float lon,String name){
		this.name=name;
		node=new Node(lat,lon,true);
	}
}
