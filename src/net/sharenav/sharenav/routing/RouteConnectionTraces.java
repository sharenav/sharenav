package net.sharenav.sharenav.routing;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.data.PaintContext;
import net.sharenav.sharenav.mapdata.WaySegment;
import net.sharenav.util.IntPoint;

import java.util.Vector;

class ConnectionTrace {
	public Node node;
	public Node successorNode;
	public String name;
	public byte flags = 0;

	public static final byte WIDELINE = 0x01;
	public static final byte MARKNODE = 0x02;
	public static final byte MARKSUCCESSORNODE = 0x04;
	
	ConnectionTrace(float lat1, float lon1, float lat2, float lon2, int flags, String name) {
		this.name = name;
		node = new Node(lat1, lon1, true);
		successorNode = new Node(lat2, lon2, true);
		this.flags = (byte) flags;
	}
}

public class RouteConnectionTraces {
	static Vector routeConnectionTraces = null;

	public static void addRouteConnectionTrace(float lat1, float lon1, float lat2, float lon2, int flags, String name) {
		if (routeConnectionTraces == null) {
			routeConnectionTraces = new Vector();
		}
		routeConnectionTraces.addElement(new ConnectionTrace(lat1, lon1, lat2,
				lon2, flags, name));
	}

	public static void clear() {
		routeConnectionTraces = null;
	}

	public static void paint(PaintContext pc, int xo, int yo) {
		if (routeConnectionTraces == null) {
			return;
		}
		
		ConnectionTrace ci;
		WaySegment waySegment = new WaySegment();

		IntPoint p1 = new IntPoint();
		IntPoint p2 = new IntPoint();
		
		// Show helper connections
		for (int i = 0; i < routeConnectionTraces.size(); i++) {
			ci = (ConnectionTrace) routeConnectionTraces.elementAt(i);
			pc.getP().forward(ci.node.radlat, ci.node.radlon, p1);
			p1.x -= xo;
			p1.y -= yo;
			pc.getP().forward(ci.successorNode.radlat, ci.successorNode.radlon, p2);
			p2.x -= xo;
			p2.y -= yo;
			int size = 5;
			if ((ci.flags & ConnectionTrace.MARKNODE) > 0 ) {
				pc.g.drawRect(p1.x - size/2, p1.y - size/2, size, size); //Draw node
			}
			if ((ci.flags & ConnectionTrace.MARKSUCCESSORNODE) > 0 ) {
				pc.g.drawRect(p2.x - size/2, p2.y - size/2, size, size); //Draw node
			}
			waySegment.drawWideLineSimple(0x00FF4796, p1, p2, ((ci.flags & ConnectionTrace.WIDELINE) > 0) ? 2 : 1, pc);
			// pc.g.drawString(n.name, pc.lineP2.x+7, pc.lineP2.y+5,
			// Graphics.BOTTOM | Graphics.LEFT);
		}
	}

}
