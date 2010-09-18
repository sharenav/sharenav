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

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;
import de.ueller.osmToGpsMid.LegendParser;
import de.ueller.osmToGpsMid.MyMath;
import de.ueller.osmToGpsMid.area.Area;
import de.ueller.osmToGpsMid.area.Outline;
import de.ueller.osmToGpsMid.area.Triangle;
import de.ueller.osmToGpsMid.area.Vertex;
import de.ueller.osmToGpsMid.model.name.Names;
import de.ueller.osmToGpsMid.model.url.Urls;


public class Way extends Entity implements Comparable<Way> {

	public static final byte	WAY_FLAG_NAME						= 1;
	public static final byte	WAY_FLAG_MAXSPEED					= 2;
	public static final byte	WAY_FLAG_LAYER						= 4;
	public static final byte	WAY_FLAG_RESERVED_FLAG				= 8;
	public static final byte	WAY_FLAG_ONEWAY						= 16;
	public static final byte	WAY_FLAG_NAMEHIGH					= 32;
	public static final byte	WAY_FLAG_AREA						= 64;
	public static final int		WAY_FLAG_ADDITIONALFLAG				= 128;

	public static final byte	WAY_FLAG2_ROUNDABOUT				= 1;
	public static final byte	WAY_FLAG2_TUNNEL					= 2;
	public static final byte	WAY_FLAG2_BRIDGE					= 4;
	public static final byte	WAY_FLAG2_CYCLE_OPPOSITE			= 8;
	/** TODO: Is this really in use??? */
	public static final byte	WAY_FLAG2_LONGWAY					= 16;
	public static final byte	WAY_FLAG2_MAXSPEED_WINTER			= 32;
	/** http://wiki.openstreetmap.org/wiki/WikiProject_Haiti */
	public static final byte	WAY_FLAG2_COLLAPSED_OR_IMPASSABLE	= 64;
	public static final int		WAY_FLAG2_ADDITIONALFLAG				= 128;

	public static final byte WAY_FLAG3_URL = 1;
	public static final byte WAY_FLAG3_URLHIGH = 2;
	public static final byte WAY_FLAG3_PHONE = 4;
	public static final byte WAY_FLAG3_PHONEHIGH = 8;
	public static final byte WAY_FLAG3_NAMEASFORAREA = 16;

	public Path					path								= null;
	public List<Triangle>		triangles							= null;
	Bounds						bound								= null;

	/** Travel modes for which this way can be used (motorcar, bicycle, etc.) */
	public byte					wayTravelModes						= 0;

	public static Configuration	config;
	/**
	 * Indicates that this way was already written to output;
	 */
	public boolean				used								= false;
	private byte				type								= -1;

	/**
	 * way id of the last unhandled maxSpeed - by using this to detect repeats we can quiet down the console output for
	 * unhandled maxspeeds
	 */
	public static long			lastUnhandledMaxSpeedWayId			= -1;

	public Way(long id) {
		this.id = id;
	}

	/**
	 * create a new Way which shares the tags with the other way, has the same type and id, but no Nodes
	 * 
	 * @param other
	 */
	public Way(Way other) {
		super(other);
		this.type = other.type;
	}

	public void cloneTags(Way other) {
		super.cloneTags(other);
		this.type = other.type;
	}

	public boolean isHighway() {
		return (getAttribute("highway") != null);
	}

	public void determineWayRouteModes() {
		if (config == null) {
			config = Configuration.getConfiguration();
		}

		// check if wayDesc is null otherwise we could route along a way we have no description how to render, etc.
		WayDescription wayDesc = config.getWayDesc(type);
		if (wayDesc == null) { return; }

		// for each way the default route accessibility comes from its way description
		wayTravelModes = wayDesc.wayDescTravelModes;

		// modify the way's route accessibility according to the route access restriction for each routeMode
		for (int i = 0; i < TravelModes.travelModeCount; i++) {
			switch (isAccessPermittedOrForbiddenFor(i)) {
				case 1:
					wayTravelModes |= 1 << i;
					break;
				case -1:
					wayTravelModes &= ~(1 << i);
					break;
			}
		}

		// Mark mainstreet net connections
		if (isAccessForAnyRouting()) {
			String wayValue = ";" + wayDesc.value.toLowerCase() + ";";
			if (";motorway;motorway_link;".indexOf(wayValue) >= 0) {
				wayTravelModes |= Connection.CONNTYPE_MOTORWAY;
			}
			if (";trunk;trunk_link;primary;primary_link;".indexOf(wayValue) >= 0) {
				wayTravelModes |= Connection.CONNTYPE_TRUNK_OR_PRIMARY;
			}
			if (";motorway;motorway_link;trunk;trunk_link;primary;primary_link;secondary;secondary_link;tertiary;".indexOf(wayValue) >= 0) {
				wayTravelModes |= Connection.CONNTYPE_MAINSTREET_NET;
			}
		}
		if (containsKey("toll")) {
			wayTravelModes |= Connection.CONNTYPE_TOLLROAD;			
		}
	}

	public boolean isAccessForRouting(int travelModeNr) {
		return ((wayTravelModes & (1 << travelModeNr)) != 0);
	}

	public boolean isAccessForAnyRouting() {
		return (wayTravelModes != 0);
	}

	/**
	 * Check way tags for routeAccessRestriction from style-file
	 * 
	 * @param travelModeNr
	 *            : e.g. for motorcar or bicycle
	 * @return -1 if restricted, 1 if permitted, 0 if neither
	 */
	public int isAccessPermittedOrForbiddenFor(int travelModeNr) {
		String value;
		for (RouteAccessRestriction rAccess : TravelModes.getTravelMode(travelModeNr).getRouteAccessRestrictions()) {
			value = getAttribute(rAccess.key);
			if (value != null && rAccess.values.indexOf(value) != -1) {
				if (rAccess.permitted) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		return 0;
	}

	public boolean isRoundabout() {
		String jType = getAttribute("junction");
		if (jType != null) {
			return (jType.equalsIgnoreCase("roundabout"));
		} else {
			return false;
		}
	}

	public boolean isTunnel() {
		return (containsKey("tunnel"));
	}

	public boolean isBridge() {
		return (containsKey("bridge"));
	}

	public boolean isDamaged() {
		Vector<Damage> damages = LegendParser.getDamages();
		if (damages.size() != 0) {
			for (Damage damage : LegendParser.getDamages()) {
				for (String s : getTags()) {
					if (damage.key.equals("*") || s.equalsIgnoreCase(damage.key)) {
						if (damage.values.equals("*") || ("|" + damage.values + "|").indexOf("|" + getAttribute(s) + "|") != -1) { return true; }
					}
				}
			}
		}
		return false;
	}

	public byte getType(Configuration c) {
		if (type == -1) {
			type = calcType(c);
		}
		//Different negative values are used to indicate internal state, canonicalise it back to -1
		if (type > -1) {
			return type;
		} else {
			return -1;
		}
	}

	public byte getType() {
		if (type > -1) {
			return type;
		} else {
			return -1;
		}
	}

	private byte calcType(Configuration c) {
		WayDescription way = (WayDescription) super.calcType(c.getWayLegend());

		if (way == null) {
			type = -1;
		} else {
			type = way.typeNum;
			way.noWaysOfType++;
		}

		/**
		 * Check to see if the way corresponds to any of the POI types If it does, then we insert a POI node to reflect
		 * this, as otherwise the nearest POI search or other POI features don't work on ways
		 */
		POIdescription poi = (POIdescription) super.calcType(c.getPOIlegend());
		if (poi != null && poi.createPOIsForAreas) {
			if (isValid()) {
				/**
				 * TODO: Come up with a sane solution to find out where to place the node to represent the area POI
				 */
				Node n = getFirstNodeWithoutPOIType();
				if (n != null) {
					n.wayToPOItransfer(this, poi);
					//Indicate that this way has been dealt with, even though the way itself has no type.
					//Some stylefiles might have both way styling and areaPOI styling for the same type.
					if (type < 0) type = -2;
				} else {
					System.out.println("WARNING: No way poi assigned because no node without a poi type has been available on way "
							+ toString());
				}
			}
		}
		return type;
	}

	@Override
	public String getName() {
		if (type > -1) {
			WayDescription desc = Configuration.getConfiguration().getWayDesc(type);
			if (desc != null) {
				String name = getAttribute(desc.nameKey);
				String nameFallback = null;
				if (desc.nameFallbackKey != null && desc.nameFallbackKey.equals("*")) {
					nameFallback = getAttribute(desc.key);
				} else {
					nameFallback = getAttribute(desc.nameFallbackKey);
				}
				if (name != null && nameFallback != null) {
					name += " (" + nameFallback + ")";
				} else if ((name == null) && (nameFallback != null)) {
					name = nameFallback;
				}
				// System.out.println("New style name: " + name);
				return (name != null ? name.trim() : "");
			}
		}
		return null;
	}

	@Override
	public String getUrl() {
		if (type > -1) {
			WayDescription desc = Configuration.getConfiguration().getWayDesc(type);
			if (desc != null) {
				if (containsKey("url")){
					String url = getAttribute("url");
					return url!=null ? url.trim() : "";
				}
			}
		}
		return null;
	}
	
	@Override
	public String getPhone() {
		if (type > -1) {
			WayDescription desc = Configuration.getConfiguration().getWayDesc(type);
			if (desc != null) {
				if (containsKey("phone")){
					String phone = getAttribute("phone");
					return phone!=null ? phone.trim() : ""; // prepend "tel:" ?
				}
			}
		}
		return null;
	}
	
	public byte getZoomlevel(Configuration c) {
		byte type = getType(c);

		if (type < 0) {
			// System.out.println("unknown type for node " + toString());
			return 3;
		}
		int maxScale = c.getWayDesc(type).minEntityScale;
		if (maxScale < LegendParser.tileScaleLevel[3]) { // 45000 in GpsMid 0.5.0
			return 3;
		} else if (maxScale < LegendParser.tileScaleLevel[2]) { // 180000 in GpsMid 0.5.0
			return 2;
		} else if (maxScale < LegendParser.tileScaleLevel[1]) { // 900000 in GpsMid 0.5.0
			return 1;
		}
		return 0;
	}

	/**
	 * Returns the maximum speed in km/h if explicitly set for this way, if not, it returns -1.0
	 * 
	 * @return
	 */
	public float getMaxSpeed() {
		float maxSpeed = -1.0f;
		if (containsKey("maxspeed")) {
			String maxSpeedAttr = getAttribute("maxspeed");
			try {
				boolean mph = false;

				if (maxSpeedAttr.equalsIgnoreCase("variable") || maxSpeedAttr.equalsIgnoreCase("default")
						|| maxSpeedAttr.equalsIgnoreCase("signals") || maxSpeedAttr.equalsIgnoreCase("none")
						|| maxSpeedAttr.equalsIgnoreCase("no")) {
					/**
					 * We can't really do anything sensible with these, so ignore them
					 */
					return maxSpeed;
				}
				if (maxSpeedAttr.toLowerCase().endsWith("mph")) {
					mph = true;
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("km/h")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 4).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kmh")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kph")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				}
				maxSpeed = (Float.parseFloat(maxSpeedAttr));
				if (mph) {
					maxSpeed *= 1.609; // Convert to km/h
				}
			} catch (NumberFormatException e) {
				try {
					int maxs = config.getMaxspeedTemplate(maxSpeedAttr);
					if (maxs > 0) {
						maxSpeed = maxs;
					}
				} catch (Exception ex) {
					if (this.id != lastUnhandledMaxSpeedWayId) {
						System.out.println("Unhandled maxspeed for way " + toString() + ": " + maxSpeedAttr);
						lastUnhandledMaxSpeedWayId = this.id;
					}
				}
			}
		}
		return maxSpeed;
	}

	/**
	 * Returns the winter maximum speed in km/h if explicitly set for this way, if not, it returns -1.0
	 * 
	 * @return
	 */
	public float getMaxSpeedWinter() {
		float maxSpeed = -1.0f;
		if (containsKey("maxspeed:seasonal:winter")) {
			String maxSpeedAttr = getAttribute("maxspeed:seasonal:winter");
			try {
				boolean mph = false;

				if (maxSpeedAttr.equalsIgnoreCase("variable") || maxSpeedAttr.equalsIgnoreCase("default")
						|| maxSpeedAttr.equalsIgnoreCase("signals") || maxSpeedAttr.equalsIgnoreCase("none")
						|| maxSpeedAttr.equalsIgnoreCase("no")) {
					/**
					 * We can't really do anything sensible with these, so ignore them
					 */
					return maxSpeed;
				}
				if (maxSpeedAttr.toLowerCase().endsWith("mph")) {
					mph = true;
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("km/h")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 4).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kmh")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				} else if (maxSpeedAttr.toLowerCase().endsWith("kph")) {
					maxSpeedAttr = maxSpeedAttr.substring(0, maxSpeedAttr.length() - 3).trim();
				}
				maxSpeed = (Float.parseFloat(maxSpeedAttr));
				if (mph) {
					maxSpeed *= 1.609; // Convert to km/h
				}
			} catch (NumberFormatException e) {
				try {
					int maxs = config.getMaxspeedTemplate(maxSpeedAttr);
					if (maxs > 0) {
						maxSpeed = maxs;
					}
				} catch (Exception ex) {
					System.out.println("Unhandled maxspeedwinter for way + " + toString() + ": " + getAttribute("maxspeed"));
				}
			}
		}
		return maxSpeed;
	}

	/**
	 * Get or estimate speed in m/s for routing purposes.
	 * 
	 * @param routeModeNr
	 *            Route mode to use
	 * @return routing speed
	 */
	public float getRoutingSpeed(int routeModeNr) {
		if (config == null) {
			config = Configuration.getConfiguration();
		}
		float maxSpeed = getMaxSpeed();
		float typicalSpeed = config.getWayDesc(type).typicalSpeed[routeModeNr];
		if (typicalSpeed != 0) {
			if (typicalSpeed < maxSpeed || maxSpeed < 0) {
				maxSpeed = typicalSpeed;
			}
		}
		if (maxSpeed <= 0) {
			maxSpeed = 60.0f; // Default case;
		}

		return maxSpeed / 3.6f;
	}

	public int compareTo(Way o) {
		byte t1 = getType();
		byte t2 = o.getType();
		if (t1 < t2) {
			return 1;
		} else if (t1 > t2) { return -1; }
		return 0;
	}

	public Bounds getBounds() {
		if (bound == null) {
			bound = new Bounds();
			if (triangles != null && triangles.size() > 0) {
				for (Triangle t : triangles) {
					bound.extend(t.getVert()[0].getLat(), t.getVert()[0].getLon());
					bound.extend(t.getVert()[1].getLat(), t.getVert()[1].getLon());
					bound.extend(t.getVert()[2].getLat(), t.getVert()[2].getLon());
				}
			} else {
				path.extendBounds(bound);
			}
		}
		return bound;
	}

	public void clearBounds() {
		bound = null;
	}

	@Override
	public String toString() {
		String res = "id=" + id + ((nearBy == null) ? "" : (" near " + nearBy)) + " type=" + getType() + " [";
		Set<String> tags = getTags();
		if (tags != null) {
			for (String key : tags) {
				res = res + key + "=" + getAttribute(key) + " ";
			}
		}
		res = res + "]";
		return res;
	}

	public String toUrl() {
		return "http://www.openstreetmap.org/browse/way/" + id;
	}

	/**
	 * @return
	 */
	public String getIsIn() {
		return getAttribute("is_in");
	}

	/**
	 * @return
	 */
	public byte getNameType() {
		String t = getAttribute("highway");
		if (t != null) { return (Constants.NAME_STREET); }
		return Constants.NAME_AMENITY;
	}

	/**
	 * @return Node in the middle or null if way has no nodes
	 */
	public Node getMidPoint() {
		if (isArea()) {
			Bounds b = getBounds();
			return (new Node((b.maxLat + b.minLat) / 2, (b.maxLon + b.minLon) / 2, -1));
		}
		List<Node> nl = path.getNodes();
		if (nl.size() > 1) {
			int splitp = nl.size() / 2;
			return (nl.get(splitp));
		} else {
			return null;
		}
	}

	/**
	 * @return
	 */
	public boolean isOneWay() {
		return (Configuration.attrToBoolean(getAttribute("oneway")) > 0);
	}

	/** Check if cycleway=opposite or cycleway=opposite_track or cycleway=opposite_lane is set */
	public boolean isOppositeDirectionForBicycleAllowed() {
		String s = getAttribute("cycleway");
		if (s == null) { return false; }
		return ("|opposite|opposite_track|opposite_lane|".indexOf("|" + s.toLowerCase() + "|") >= 0);
	}

	public void write(DataOutputStream ds, Names names1, Urls urls1, Tile t) throws IOException {
		Bounds b = new Bounds();
		int flags = 0;
		int flags2 = 0;
		int flags3 = 0;
		int maxspeed = 50;
		int maxspeedwinter = 50;
		int nameIdx = -1;
		int urlIdx = -1;
		int phoneIdx = -1;
		byte layer = 0;

		if (config == null) {
			config = Configuration.getConfiguration();
		}
		if (id == 39123631) {
			System.out.println("Write way 39123631");
		}

		byte type = getType();

		if (getName() != null && getName().trim().length() > 0) {
			flags += WAY_FLAG_NAME;
			nameIdx = names1.getNameIdx(getName());
			if (nameIdx >= Short.MAX_VALUE) {
				flags += WAY_FLAG_NAMEHIGH;
			}
		}
		if (config.useUrlTags && getUrl() != null && getUrl().trim().length() > 0){			
			flags3+=WAY_FLAG3_URL;
			urlIdx = urls1.getUrlIdx(getUrl());
			if (urlIdx >= Short.MAX_VALUE) {
				flags3 += WAY_FLAG3_URLHIGH;
			}
		}
		if (config.usePhoneTags && getPhone() != null && getPhone().trim().length() > 0){			
			flags3+=WAY_FLAG3_PHONE;
			phoneIdx = urls1.getUrlIdx(getPhone());
			if (phoneIdx >= Short.MAX_VALUE) {
				flags3 += WAY_FLAG3_PHONEHIGH;
			}
		}
		if (showNameAsForArea()) {
			flags3 += WAY_FLAG3_NAMEASFORAREA;
		}
		maxspeed = (int) getMaxSpeed();
		maxspeedwinter = (int) getMaxSpeedWinter();
		if (maxspeedwinter > 0) {
			flags2 += WAY_FLAG2_MAXSPEED_WINTER;
		} else {
			maxspeedwinter = 0;
		}
		if (maxspeed > 0) {
			flags += WAY_FLAG_MAXSPEED;
		}

		if (containsKey("layer")) {
			try {
				layer = (byte) Integer.parseInt(getAttribute("layer"));
				flags += WAY_FLAG_LAYER;
			} catch (NumberFormatException e) {
			}
		}
		if ((config.getWayDesc(type).forceToLayer != 0)) {
			layer = config.getWayDesc(type).forceToLayer;
			flags |= WAY_FLAG_LAYER;
		}

		boolean longWays = false;

		if (type < 1) {
			System.out.println("ERROR! Invalid way type for way " + toString());
		}

		if (getNodeCount() > 255) {
			longWays = true;
		}
		if (isOneWay()) {
			flags += WAY_FLAG_ONEWAY;
		}
		if (isArea()) {
			// if (isExplicitArea()) {
			flags += WAY_FLAG_AREA;
		}
		if (isRoundabout()) {
			flags2 += WAY_FLAG2_ROUNDABOUT;
		}
		if (isTunnel()) {
			flags2 += WAY_FLAG2_TUNNEL;
		}
		if (isBridge()) {
			flags2 += WAY_FLAG2_BRIDGE;
		}
		if (isDamaged()) {
			flags2 += WAY_FLAG2_COLLAPSED_OR_IMPASSABLE;
		}
		if (isOppositeDirectionForBicycleAllowed()) {
			flags2 += WAY_FLAG2_CYCLE_OPPOSITE;
		}
		if (longWays) {
			flags2 += WAY_FLAG2_LONGWAY;
		}
		if (flags3 != 0) {
			flags2 += WAY_FLAG2_ADDITIONALFLAG;
		}
		if (flags2 != 0) {
			flags += WAY_FLAG_ADDITIONALFLAG;
		}
		ds.writeByte(flags);

		b = getBounds();
		ds.writeShort((short) (MyMath.degToRad(b.minLat - t.centerLat) * MyMath.FIXPT_MULT));
		ds.writeShort((short) (MyMath.degToRad(b.minLon - t.centerLon) * MyMath.FIXPT_MULT));
		ds.writeShort((short) (MyMath.degToRad(b.maxLat - t.centerLat) * MyMath.FIXPT_MULT));
		ds.writeShort((short) (MyMath.degToRad(b.maxLon - t.centerLon) * MyMath.FIXPT_MULT));

		ds.writeByte(type);
		ds.writeByte(wayTravelModes);

		if ((flags & WAY_FLAG_NAME) == WAY_FLAG_NAME) {
			if ((flags & WAY_FLAG_NAMEHIGH) == WAY_FLAG_NAMEHIGH) {
				ds.writeInt(nameIdx);
			} else {
				ds.writeShort(nameIdx);
			}
		}
		if ((flags & WAY_FLAG_MAXSPEED) == WAY_FLAG_MAXSPEED) {
			ds.writeByte(maxspeed);
		}
		// must be below maxspeed as this is a combined flag and GpsMid relies it's below maxspeed
		if (flags2 != 0) {
			ds.writeByte(flags2);
		}
		if (flags3 != 0) {
			ds.writeByte(flags3);
		}
		if ((flags3 & WAY_FLAG3_URL) == WAY_FLAG3_URL){
			if ((flags3 & WAY_FLAG3_URLHIGH) == WAY_FLAG3_URLHIGH){
				ds.writeInt(urlIdx);
			} else {
				ds.writeShort(urlIdx);
			}
		}
		if ((flags3 & WAY_FLAG3_PHONE) == WAY_FLAG3_PHONE){
			if ((flags3 & WAY_FLAG3_PHONEHIGH) == WAY_FLAG3_PHONEHIGH){
				ds.writeInt(phoneIdx);
			} else {
				ds.writeShort(phoneIdx);
			}
		}
		if ((flags2 & WAY_FLAG2_MAXSPEED_WINTER) == WAY_FLAG2_MAXSPEED_WINTER) {
			ds.writeByte(maxspeedwinter);
		}
		if ((flags & WAY_FLAG_LAYER) == WAY_FLAG_LAYER) {
			ds.writeByte(layer);
		}
		if (isArea()) {
			if (longWays) {
				ds.writeShort(getNodeCount());
			} else {
				ds.writeByte(getNodeCount());
			}
			for (Triangle tri : getTriangles()) {
				ds.writeShort(tri.getVert()[0].getNode().renumberdId);
				ds.writeShort(tri.getVert()[1].getNode().renumberdId);
				ds.writeShort(tri.getVert()[2].getNode().renumberdId);
			}
		} else {
			if (longWays) {
				ds.writeShort(getNodeCount());
			} else {
				ds.writeByte(getNodeCount());
			}
			for (Node n : path.getNodes()) {
				ds.writeShort(n.renumberdId);
			}
		}

		if (config.enableEditingSupport) {
			if (id > Integer.MAX_VALUE) {
				System.out.println("WARNING: OSM-ID won't fit in 32 bits for way " + this);
				ds.writeInt(-1);
			} else {
				ds.writeInt(id.intValue());
			}
		}

	}

	public void add(Node n) {
		if (path == null) {
			path = new Path();
		}
		path.add(n);
	}

	public boolean containsNode(Node nSearch) {
		for (Node n : path.getNodes()) {
			if (nSearch.id == n.id) { 
				return true;
			}
		}
		return false;
	}

	public Node getFirstNodeWithoutPOIType() {
		for (Node n : path.getNodes()) {
			if (n.getType(config) == -1) {
				return n;
			}
		}
		return null;
	}

	public ArrayList<RouteNode> getAllRouteNodesOnTheWay() {
		ArrayList<RouteNode> returnNodes = new ArrayList<RouteNode>();
		for (Node n : path.getNodes()) {
			if (n.routeNode != null) {
				returnNodes.add(n.routeNode);
			}
		}
		return returnNodes;
	}

	// public void startNextSegment() {
	// if (path == null) {
	// path = new Path();
	// }
	// path.addNewSegment();
	// }

	/**
	 * Replaces node1 with node2 in this way.
	 * 
	 * @param node1 Node to be replaced
	 * @param node2 Node by which to replace node1.
	 */
	public void replace(Node node1, Node node2) {
		path.replace(node1, node2);
	}

	/**
	 * replaceNodes lists nodes and by which nodes they have to be replaced. 
	 * This method applies the specified list to this way.
	 * 
	 * @param replaceNodes Hashmap of pairs of nodes
	 */
	public void replace(HashMap<Node, Node> replaceNodes) {
		path.replace(replaceNodes);
	}

	// public List<SubPath> getSubPaths() {
	// return path.getSubPaths();
	// }

	public List<Triangle> getTriangles() {
		if (isArea() && triangles == null) {
			triangulate();
		}
		return triangles;
	}

	public int getLineCount() {
		if (isArea()) {
			return getTriangles().size();
		} else {
			return path.getLineCount();
		}
	}

	public int getNodeCount() {
		if (isArea()) {
			return getTriangles().size() * 3;
		} else {
			return path.getNodeCount();
		}
	}

	public Way split() {
		if (isArea()) {
			return splitArea();
		} else {
			return splitNormalWay();
		}
	}

	private Way splitArea() {
		if (triangles == null) {
			triangulate();
		}
		// System.out.println("Split area id=" + id + " s=" + triangles.size());
		Way newWay = new Way(this);
		Bounds newBbounds = getBounds().split()[0];
		ArrayList<Triangle> tri = new ArrayList<Triangle>();
		for (Triangle t : triangles) {
			Vertex midpoint = t.getMidpoint();
			if (!newBbounds.isIn(midpoint.getLat(), midpoint.getLon())) {
				tri.add(t);
			}
		}
		if (tri.size() == triangles.size()) {
			// all triangles would go to the other way
			return null;
		}
		for (Triangle t : tri) {
			triangles.remove(t);
		}
		// System.out.println("split area triangles " + "s1=" + triangles.size() + " s2=" + tri.size());
		if (tri.size() == 0) { return null; }
		clearBounds();
		newWay.triangles = tri;
		newWay.recreatePath();
		recreatePath();
		return newWay;
	}

	private Way splitNormalWay() {
		if (isValid() == false) {
			System.out.println("Way before split is not valid");
		}
		Path split = path.split();
		if (split != null) {
			// If we split the way, the bounds are no longer valid
			this.clearBounds();
			Way newWay = new Way(this);
			newWay.path = split;
			if (newWay.isValid() == false) {
				System.out.println("New Way after split is not valid");
			}
			if (isValid() == false) {
				System.out.println("Old Way after split is not valid");
			}
			return newWay;
		}
		return null;
	}

	public boolean isValid() {
		if (path == null || path.getNodeCount() == 0) {
			return false;
		}
		return true;
	}

	public boolean isClosed() {
		if (!isValid()) { 
			return false;
		}

		List<Node> nlist = path.getNodes();
		if (nlist.get(0) == nlist.get(nlist.size() - 1)) {
			return true;
		}

		return false;
	}

	public boolean isArea() {
		if (triangles != null) { 
			return true;
		}
		if (isExplicitArea()) { 
			return true;
		}
		if (type >= 0) { 
			return Configuration.getConfiguration().getWayDesc(type).isArea;
		}
		return false;
	}

	public boolean showNameAsForArea() {
		if (type >= 0) { 
			return Configuration.getConfiguration().getWayDesc(type).showNameAsForArea;
		}
		return false;
	}

	public boolean isExplicitArea() {
		// Ignore area=yes in OSM data if the style file for way type says so
		if (type >= 0 && Configuration.getConfiguration().getWayDesc(type).ignoreOsmAreaTag) {
			return false;
		} else {
			return Configuration.attrToBoolean(getAttribute("area")) > 0;
		}
	}

	public List<Node> getNodes() {
		return path.getNodes();
	}

	/**
	 * @param wayHashMap
	 * @param r
	 */
	private void triangulate() {
		if (isArea()) {
			Outline o = null;
			o = new Outline();
			o.setWayId(id);
			for (Node n : getNodes()) {
				o.append(new Vertex(n, o));
			}
			Area a = new Area();
			a.addOutline(o);
			triangles = a.triangulate();
			recreatePath();
		} else {
			System.err.println("Can't triangulate normal ways");
		}
	}

	/**
	 * 
	 */
	public void recreatePath() {
		// // regenrate Path
		if (isArea() && triangles.size() > 0) {
			path = new Path();
		}
		for (Triangle t : triangles) {
			for (int l = 0; l < 3; l++) {
				Node n = t.getVert()[l].getNode();
				if (!path.getNodes().contains(n)) {
					path.add(n);
				}
			}
		}
		clearBounds();
	}

}
