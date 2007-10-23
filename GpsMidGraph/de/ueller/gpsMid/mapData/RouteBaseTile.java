package de.ueller.gpsMid.mapData;

import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.tile.PaintContext;

public abstract class RouteBaseTile extends Tile {

	public void getWay(PaintContext pc, PositionMark pm, Way w) {
		return;
	}
	protected int minId;
	protected int maxId;

	public abstract RouteNode getRouteNode(int id);
	public abstract RouteNode getRouteNode(RouteNode best,float lat, float lon);
	public abstract Connection[] getConnections(int id,RouteBaseTile tile,boolean bestTime);
//	public abstract void printMinMax
	private final static float epsilon=0.0001f;

}
