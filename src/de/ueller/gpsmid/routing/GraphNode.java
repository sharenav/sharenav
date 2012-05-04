package de.ueller.gpsmid.routing;

import de.ueller.gpsmid.ui.Trace;

public class GraphNode {
	Connection state;
	int costs;
	int distance;
	//int total;
	byte fromBearing;
	public GraphNode parent;
	
	public GraphNode(Connection theState, GraphNode theParent, int theCosts, int theDistance,byte bearing) {
		state = theState;
		parent = theParent;
		costs = theCosts;
		distance = theDistance;
		//total = theCosts + (theDistance);
		fromBearing = bearing;
	}
	
	public int getTotal() {
//		if (costs + distance != total) {
//			Trace.getInstance().alert("Routing","costs + distance != total", 3000);
//		}
		return costs + distance;
	}
}
