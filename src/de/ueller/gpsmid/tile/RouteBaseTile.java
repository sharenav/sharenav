package de.ueller.gpsmid.tile;

import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.routing.Connection;
import de.ueller.gpsmid.routing.RouteNode;
import de.ueller.gpsmid.routing.RouteTileRet;
import de.ueller.gpsmid.routing.TurnRestriction;

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
	public abstract RouteNode getRouteNode(RouteNode best, float epsilon, float lat, float lon);
	/**
	 * search for node in RouteNodes
	 * that is exact same position
	 * (WARNING: actually not exact, only approximately the same
	 * which may cause the wrong RouteNode
	 * being found if there are two very closeby RouteNodes,
	 * therefore better use search for best route node)
	 * @param lat
	 * @param lon
	 * @return
	 */
	public abstract RouteNode getRouteNode(float lat, float lon);
	public abstract TurnRestriction getTurnRestrictions(int rnId);
	public abstract Connection[] getConnections(int id,RouteBaseTile tile,boolean bestTime);
//	public abstract void printMinMax
	private final static float epsilon=0.0001f;
	public final static float EPSILON_SEARCH_EXACT_MATCH=0.000001f;
	
	public void walk(PaintContext pc, int opt) {
		// TODO implement walk		
	}

	/**
	 * @param lat
	 * @param lon
	 * @param retTile
	 * @return
	 */
	public abstract RouteNode getRouteNode(int id,RouteTileRet retTile);

}
