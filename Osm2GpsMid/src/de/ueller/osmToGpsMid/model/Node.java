/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;


public class Node extends Entity{
	/**
	 * the position in target array of nodes
	 */
	public int renumberdId;
	/**
	 * Latitude of this node
	 */
	public float lat;
	/**
	 * Longitude of this node;
	 */
	public float lon;
	/**
	 * type of this Node
	 */
	private byte type = -1;
	//public byte noConfType = -1;
	public boolean used = false;
	public byte connectedLineCount = 0;
//	private Set<Way> connectedWays = new HashSet<Way>();
	public RouteNode routeNode;
	
	public Node() {
	}
	
	public Node(float node_lat, float node_lon, long id) {
		lat = node_lat;
		lon = node_lon;
		this.id = id;
	}
	public String getName() {
		if (type != -1) {
			POIdescription desc = Configuration.getConfiguration().getpoiDesc(type);
			if (desc != null) {
				String name = getAttribute(desc.nameKey);
				String nameFallback=null;
				if (desc.nameFallbackKey!= null && desc.nameFallbackKey.equals("*") ) {
					nameFallback = getAttribute(desc.key);
				} else {
					nameFallback = getAttribute(desc.nameFallbackKey);
				}
				if (name != null && nameFallback != null) {
					name += " (" + nameFallback + ")";
				} else if ((name == null) && (nameFallback != null)) {
					name = nameFallback;
				}
				//System.out.println("New style name: " + name);
				return (name != null ? name.trim() : "");
			}
		}
		return null;
	}
	
	public String getPlace() {
		String place = (getAttribute("place"));
//		System.out.println("Read place for id="+id+" as=" + place);
		if (place != null) {
			return place.trim();
		}
		return null;
	}
	public boolean isPlace() {
		if (type != -1) {
			POIdescription desc = Configuration.getConfiguration().getpoiDesc(type);			
			if (desc.key.equalsIgnoreCase("place"))
				return true;
		}
		return false;
	}

	public byte getType(Configuration c){
		if (type != -1) {
			return type;
		} else {
			type = calcType(c);
		}
		return type;
	}
	
	private byte calcType(Configuration c) {
		if (type != -1) {
			return type;
		}
		if (c == null) { 
			return -1;
		}
		EntityDescription poi = super.calcType(c.getPOIlegend());
		if (poi == null) {
			type = -1;
		} else {
			type = poi.typeNum;
		}
		return type;
	}
	
	public byte getZoomlevel(Configuration c) {
		if (type == -1) {
			//System.out.println("unknown type for node " + toString());
			return 3;
		}
		int maxScale = c.getpoiDesc(type).minEntityScale;
		if (maxScale < 45000) {
			return 3;
		} else if (maxScale < 180000) {
			return 2;
		} else if (maxScale < 900000) {
			return 1;
		}
		return 0;
	}

	public String toString() {
		return "id=" + id + " (" + lat + "|" + lon + ") " 
			+ ((getPlace() != null) ? ("(" + getPlace() + ") ") : "") 
			+ "name=" + getName() 
			+ ((nearBy == null) ? "" : (" near " + nearBy.getName()));
	}

	public String toUrl() {
		return "http://www.openstreetmap.org/browse/node/" + id;
	}
	
	/**
	 * @return
	 */
	public byte getNameType() {
		String t = getPlace();
		if (t != null) {
			if ("suburb".equals(t)) {
				return (Constants.NAME_SUBURB);
			} else {
			    return (Constants.NAME_CITY);
			}
		}
		return Constants.NAME_AMENITY;
	}
	
	/**
	 * wayToPOItransfer is used to transfer properties of a way
	 * onto a Node. This is used to represent area POIs.
	 * @param w The way from which to transfer the properties
	 * @param poi The type of POI to which it gets transfered. 
	 * @return if it was successful
	 */
	public boolean wayToPOItransfer(Way w, POIdescription poi) {
		if (type != -1) {
			System.out.println("WARNING: Node already has a type, can't assign way-poi type");
			System.out.println("  " + toString());
			return false;
		}
		type = poi.typeNum;
		String value = w.getAttribute(poi.nameKey);
		if (value != null) {
			setAttribute(poi.nameKey, value);
		}
		value = w.getAttribute(poi.nameFallbackKey);
		if (value != null) {
			setAttribute(poi.nameFallbackKey, value);
		}
		return true;
	}


}
