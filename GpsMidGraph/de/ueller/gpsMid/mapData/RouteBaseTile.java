package de.ueller.gpsMid.mapData;

import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.routing.RouteTileRet;
import de.ueller.midlet.gps.routing.TurnRestriction;
import de.ueller.midlet.gps.tile.PaintContext;

public abstract class RouteBaseTile extends Tile {
	
	protected boolean permanent=false;

	protected int minId;
	protected int maxId;
	public boolean lastNodeHadTurnRestrictions = false;
	public RouteNode lastRouteNode = null;

	public abstract RouteNode getRouteNode(int id);
	/**
	 * search for node in RouteNodes that nearer then best
	 * @param best
	 * @param lat
	 * @param lon
	 * @return
	 */
	public abstract RouteNode getRouteNode(RouteNode best,float lat, float lon);
	/**
	 * search for node in RouteNodes that is exact at the
	 * same position
	 * @param lat
	 * @param lon
	 * @return
	 */
	public abstract RouteNode getRouteNode(float lat, float lon);
	public abstract TurnRestriction getTurnRestrictions(int rnId);
	public abstract Connection[] getConnections(int id,RouteBaseTile tile,boolean bestTime);
//	public abstract void printMinMax
	private final static float epsilon=0.0001f;
	
	public void walk(PaintContext pc, int opt) {
		// TODO implement walk		
	}

	/**
	 * @param lat
	 * @param lon
	 * @param retTile
	 * @return
	 */
	public abstract RouteNode getRouteNode(float lat,float lon,RouteTileRet retTile);

}
