/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;

import de.ueller.osmToGpsMid.model.RouteNode;

/**
 * @author hmueller
 *
 */
@javax.persistence.Entity
@Table(name = "NODE")
@DiscriminatorValue(value="N")
public class Node extends Entity implements Serializable {
	private int renumberedId;
	private float lat;
	private float lon;
	private byte connectedLineCount = 0;
//	private RouteNode routeNode;
	private Node nearBy;
	private boolean trafficSignal;
	private boolean SignalRouteNode;

	
	public Node(){	
	}
	
	/**
	 * @param nodeLat
	 * @param nodeLon
	 * @param id2
	 */
	public Node(float nodeLat, float nodeLon, long id2) {
		lat = nodeLat;
		lon = nodeLon;
		id = id2;
	}

	public int getRenumberedId() {
		return renumberedId;
	}




	public void setRenumberedId(int renumberedId) {
		this.renumberedId = renumberedId;
	}




	public float getLat() {
		return lat;
	}




	public void setLat(float lat) {
		this.lat = lat;
	}




	public float getLon() {
		return lon;
	}




	public void setLon(float lon) {
		this.lon = lon;
	}


	public byte getConnectedLineCount() {
		return connectedLineCount;
	}




	public void setConnectedLineCount(byte connectedLineCount) {
		this.connectedLineCount = connectedLineCount;
	}

	public Node getNearBy() {
		return nearBy;
	}




	public void setNearBy(Node nearBy) {
		this.nearBy = nearBy;
	}

	public boolean isTrafficSignal() {
		return trafficSignal;
	}

	public void setTrafficSignal(boolean trafficSignal) {
		this.trafficSignal = trafficSignal;
	}

	public boolean isSignalRouteNode() {
		return SignalRouteNode;
	}

	public void setSignalRouteNode(boolean signalRouteNode) {
		SignalRouteNode = signalRouteNode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Node" + super.toString();
	}
	public String toUrl() {
		return "http://www.openstreetmap.org/browse/node/" + id;
	}

}


