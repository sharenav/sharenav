/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 * See COPYING.
 *
 * Copyright (C) 2007 Harald Mueller
 */

package net.sharenav.osmToShareNav.model;

import net.sharenav.osmToShareNav.Configuration;
import net.sharenav.osmToShareNav.Constants;
import net.sharenav.osmToShareNav.LegendParser;


public class Node extends Entity {

	public long id;
	/**
	 * The position in the target array of nodes.
	 */
	public int renumberdId;

	/**
	 * Latitude (in degrees) of this node.
	 */
	public float lat;
	
	/**
	 * Longitude (in degrees) of this node.
	 */
	public float lon;
	
	/**
	 * Type of this Node
	 */
	// polish.api.bigstyles
	private short type = -1;
	//public byte noConfType = -1;
	public boolean used = false;
	private byte connectedLineCount = 0;
//	private Set<Way> connectedWays = new HashSet<Way>();
	public RouteNode routeNode;
	public boolean fid = false;
	
	// the upper flags of connectedLineCount are used to indicate special informations about the node
	public static final int CLC_MASK_CONNECTEDLINECOUNT = 31;
	public static final int CLC_NEVER_TRAFFICSIGNALS_ROUTENODE = 128;
	public static final int CLC_FLAG_TRAFFICSIGNALS = 64;
	public static final int CLC_FLAG_TRAFFICSIGNALS_ROUTENODE = 32;

	public Node() {
	}
	
	public Node(float node_lat, float node_lon, long id) {
		lat = node_lat;
		lon = node_lon;
		this.id = id;
	}
	
	public Node(Node old) {
		lat = old.lat;
		lon = old.lon;
		this.id = old.id;
	}
	
	/**
	 * @return Latitude (in degrees) of this node
	 */
	public float getLat() {
		return lat;
	}
	
	/**
	 * @return Longitude (in degrees) of this node
	 */
	public float getLon()	{
		return lon;
	}
	
	/**
	 * @return Node in the middle of this node and another node
	 */
	public Node midNode(Node old, long newID) {
		return new Node((lat + old.lat) / 2,
			    (lon + old.lon) / 2, newID);
	}
	
	/**
	 * @return Longitude (in map units) of this node for isClockWise()
	 */
	public long getLongLon() {
		return toMapUnit(lon);
	}
	
	/**
	 * @return Longitude (in map units) of this node for isClockWise()
	 */
	public long getLongLat() {
		return toMapUnit(lat);
	}
	
	/* from mkgmap by Steve Ratcliffe */

    	/**
         * A map unit is an integer value that is 1/(2^24) degrees of latitude or
         * longitude.
         *
         * @param l The lat or long as decimal degrees.
         * @return An integer value in map units.
         */
        private static int toMapUnit(float l) {
                double DELTA = 360.0D / (1 << 24) / 2; //Correct rounding
                if (l > 0)
                        return (int) ((l + DELTA) * (1 << 24)/360);
                else
                        return (int) ((l - DELTA) * (1 << 24)/360);
        }

	@Override
	public String getName() {
		if (type != -1) {
			POIdescription desc = Configuration.getConfiguration().getpoiDesc(type);
			if (desc != null) {
				String name = getAttribute(desc.nameKey);
				String nameFallback = null;
				if (desc.nameFallbackKey!= null && desc.nameFallbackKey.equals("*") ) {
					nameFallback = getAttribute(desc.key);
				} else {
					nameFallback = getAttribute(desc.nameFallbackKey);
				}
				if (name != null && nameFallback != null && (!desc.nameFallbackKey.equals("*") || !desc.key.equals(desc.nameKey))) {
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
//		System.out.println("Read place for id=" + id + " as=" + place);
		if (place != null) {
			return place.trim();
		}
		return null;
	}

	public boolean isPlace() {
		if (type != -1) {
			POIdescription desc = Configuration.getConfiguration().getpoiDesc(type);			
			if (desc.key.equalsIgnoreCase("place")) {
				return true;
			}
		}
		return false;
	}

	public void resetType(Configuration c) {
		type = -1;
	}

	// polish.api.bigstyles
	public short getType(Configuration c) {
		if (type != -1) {
			return type;
		} else {
			type = calcType(c);
		}
		return type;
	}

	public byte getConnectedLineCount() {
		return (byte)(connectedLineCount & CLC_MASK_CONNECTEDLINECOUNT);
	}

	private void setConnectedLineCount(byte count) {
		connectedLineCount &= ~CLC_MASK_CONNECTEDLINECOUNT;
		connectedLineCount |= count;
	}

	public void resetConnectedLineCount() {
		connectedLineCount &= ~CLC_MASK_CONNECTEDLINECOUNT;
	}
	
	public void incConnectedLineCount() {
		setConnectedLineCount((byte) (getConnectedLineCount() + 1));
	}

	public void decConnectedLineCount() {
		setConnectedLineCount((byte) (getConnectedLineCount() -1));
	}

	public void markAsTrafficSignals() {
		connectedLineCount |= CLC_FLAG_TRAFFICSIGNALS;
	}

	public boolean isTrafficSignals() {
		return ((connectedLineCount & CLC_FLAG_TRAFFICSIGNALS) > 0);
	}

	public void markAsTrafficSignalsRouteNode() {
		connectedLineCount |= CLC_FLAG_TRAFFICSIGNALS_ROUTENODE;
	}

	public void unMarkAsTrafficSignalsRouteNode() {
		connectedLineCount &= ~CLC_FLAG_TRAFFICSIGNALS_ROUTENODE;
	}
	
	public boolean isTrafficSignalsRouteNode() {
		return ((connectedLineCount & CLC_FLAG_TRAFFICSIGNALS_ROUTENODE) > 0);
	}	

	public void markAsNeverTrafficSignalsRouteNode() {
		connectedLineCount |= CLC_NEVER_TRAFFICSIGNALS_ROUTENODE;
	}	

	public boolean isNeverTrafficSignalsRouteNode() {
		return ((connectedLineCount & CLC_NEVER_TRAFFICSIGNALS_ROUTENODE) > 0);
	}	

	// FIXME should not be hard-coded but taken from style-file
	public boolean hasHouseNumberTag() {
		return (containsKey("addr:housenumber"));
	}

	// FIXME should not be hard-coded but taken from style-file
	public boolean isBarrier() {
		return (containsKey("barrier"));
	}
	
	// polish.api.bigstyles
	private short calcType(Configuration c) {
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
		if (maxScale < LegendParser.tileScaleLevel[3]) { 		// 45000 in 0.5.0
			return 3;
		} else if (maxScale < LegendParser.tileScaleLevel[2]) { // 180000 in 0.5.0
			return 2;
		} else if (maxScale < LegendParser.tileScaleLevel[1]) { // 900000 in 0.5.0
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return "id=" + id + " (" + lat + "|" + lon + ") " 
			+ ((getPlace() != null) ? ("(" + getPlace() + ") ") : "") 
			+ "name=" + getName() 
			+ ((nearBy == null) ? "" : (" near " + nearBy.getName()));
	}

	/**
	 * @return String with the URL to inspect this node on the OSM website
	 */
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
		//String value = w.getAttribute(poi.nameKey);
		//if (value != null) {
		//	setAttribute(poi.nameKey, value);
		//}
		//value = w.getAttribute(poi.nameFallbackKey);
		//if (value != null) {
		//	setAttribute(poi.nameFallbackKey, value);
		//}
		// FIXME could save some memory by copying only needed tags (namekey, fallback, addr:street)
		for (String t : w.getTags()) {
			if (w.containsKey(t)) {
				setAttribute(t, w.getAttribute(t));
			}
		}
		return true;
	}
	
}
