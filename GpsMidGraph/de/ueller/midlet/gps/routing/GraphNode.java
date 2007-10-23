package de.ueller.midlet.gps.routing;


public class GraphNode {
	Connection state;
	int costs;
	int distance;
	int total;
	byte fromBearing;
	public GraphNode parent;
	
	public GraphNode(Connection theState, GraphNode theParent, int theCosts, int theDistance,byte bearing) {
		state = theState;
		parent = theParent;
		costs = theCosts;
		distance = theDistance;
		total = theCosts + (theDistance);
		fromBearing = bearing;
	}
}
