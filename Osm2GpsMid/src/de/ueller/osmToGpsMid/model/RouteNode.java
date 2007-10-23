/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.ArrayList;

/**
 * @author hmueller
 *
 */
public class RouteNode {
	public Node node;
	public ArrayList<Connection> connected=new ArrayList<Connection>();
	public ArrayList<Connection> connectedFrom=new ArrayList<Connection>();
	public int id;
//	public float g; // total cost 
//	public float h; //heuristic
//	public float f; //sum of g and h; 
	public RouteNode(Node n){
		node=n;
	}
	public String toString(){
		return ("RouteNode id=" + id+"(" + node.renumberdId + ")");
	}
}
