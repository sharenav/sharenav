package net.sharenav.sharenav.data;

import net.sharenav.sharenav.mapdata.Entity;
import net.sharenav.sharenav.mapdata.Way;

/*
 * ShareNav - Copyright (c) 2010 sk750 at users dot sourceforge dot net
 * See Copying
 */


public class RoutePositionMark extends PositionMark {
	/** the entity (e.g. way) of the route position mark */
	public Entity entity;
	/** the latitude coordinates of the way corresponding to this RoutePositionMark
		this is used to find matching routeNodes
	*/
	public float[] nodeLat;
	/** the longitude coordinates of the way corresponding to this RoutePositionMark
		this is used to find matching routeNodes
	*/
	public float[] nodeLon;
	/** the nameIdx of the RoutePositionMark, mainly used to recognize the destination way for highlighting */
	public int nameIdx = -1;

	public RoutePositionMark(float lat, float lon) {
		super(lat, lon);
	}

	public RoutePositionMark(PositionMark pm, int nameIdx) {
		super(pm.lat, pm.lon);
		this.displayName = pm.displayName;
		this.nameIdx = nameIdx;
	}

	
	public void setEntity(Way w, float lat[], float lon[]) {
		entity = w;
		nodeLat = lat;
		nodeLon = lon;
	}


}
