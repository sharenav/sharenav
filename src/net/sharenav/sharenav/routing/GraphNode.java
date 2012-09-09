package net.sharenav.sharenav.routing;

import net.sharenav.sharenav.ui.Trace;

public class GraphNode {
	Connection state;
	int costs;
	int distance;
	//int total;
	byte flags;
	byte fromBearing;
	public GraphNode parent;
	
	public static final int GN_FLAG_CONNECTION_STARTS_INSIDE_MAINSTREETDISTANCE = 0x01;
	
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
	
	public boolean getFlag(int flag) {
		return (flag & flags) > 0;
	}

	public void setFlag(int flag) {
		flags |= flag;
	}

}
