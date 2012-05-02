package de.ueller.gpsmid.routing;

import de.ueller.gps.Node;
import de.ueller.gpsmid.data.PaintContext;
import java.util.Vector;

class ConnectionTrace {
	public Node node;
	public Node successorNode;
	public String name;

	ConnectionTrace(float lat1, float lon1, float lat2, float lon2, String name) {
		this.name = name;
		node = new Node(lat1, lon1, true);
		successorNode = new Node(lat2, lon2, true);
	}
}

public class RouteConnectionTraces {
	static Vector routeConnectionTraces = null;

	public static void addRouteConnectionTrace(float lat1, float lon1, float lat2, float lon2, String name) {
		if (routeConnectionTraces == null) {
			routeConnectionTraces = new Vector();
		}
		routeConnectionTraces.addElement(new ConnectionTrace(lat1, lon1, lat2,
				lon2, name));
	}

	public static void clear() {
		routeConnectionTraces = null;
	}

	public static void paint(PaintContext pc, int xo, int yo) {
		ConnectionTrace ci;
		int x;
		int y;
		pc.g.setColor(0x00FF5060);
		// Show helper connections
		for (int i = 0; i < routeConnectionTraces.size(); i++) {
			ci = (ConnectionTrace) routeConnectionTraces.elementAt(i);
			pc.getP().forward(ci.node.radlat, ci.node.radlon, pc.lineP2);
			x = pc.lineP2.x - xo;
			y = pc.lineP2.y - yo;
			pc.getP().forward(ci.successorNode.radlat, ci.successorNode.radlon, pc.lineP2);
			pc.lineP2.x -= xo;
			pc.lineP2.y -= yo;
			pc.g.drawLine(x, y, pc.lineP2.x, pc.lineP2.y);
			// pc.g.drawString(n.name, pc.lineP2.x+7, pc.lineP2.y+5,
			// Graphics.BOTTOM | Graphics.LEFT);
		}
	}

}
