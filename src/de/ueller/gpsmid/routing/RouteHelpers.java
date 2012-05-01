package de.ueller.gpsmid.routing;

import de.ueller.gps.Node;
import de.ueller.gpsmid.data.PaintContext;
import java.util.Vector;

import javax.microedition.lcdui.Graphics;

class RouteHelper {
	public Node node;
	public String name;
	
	public RouteHelper(float lat,float lon,String name){
		this.name=name;
		node=new Node(lat,lon,true);
	}
}

public class RouteHelpers {
	static Vector routeHelpers = null;
	
	public static void addRouteHelper(float lat, float lon, String name) {
		if (routeHelpers == null) {
			routeHelpers = new Vector();
		}
		routeHelpers.addElement(new RouteHelper(lat, lon, name));
	}

	public static void clear() {
		routeHelpers = null;
	}

	public static void paint(PaintContext pc, int xo, int yo) {
		// Show helper connections
		for (int i = 0; i < routeHelpers.size(); i++) {
			RouteHelper rh = (RouteHelper) routeHelpers.elementAt(i);
			pc.getP().forward(rh.node.radlat, rh.node.radlon, pc.lineP2);
			pc.lineP2.x -= xo;
			pc.lineP2.y -= yo;
			pc.g.setColor(0x0030A030);
			pc.g.drawRect(pc.lineP2.x-5, pc.lineP2.y-5, 10, 10);
			pc.g.drawString(rh.name, pc.lineP2.x+7, pc.lineP2.y+5, Graphics.BOTTOM | Graphics.LEFT);
		}
	}

}