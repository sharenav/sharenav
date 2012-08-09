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

import java.awt.Polygon;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;
import de.ueller.osmToGpsMid.LegendParser;
import de.ueller.osmToGpsMid.MyMath;
import de.ueller.osmToGpsMid.area.Area;
import de.ueller.osmToGpsMid.area.Outline;
import de.ueller.osmToGpsMid.area.Triangle;
import de.ueller.osmToGpsMid.area.Vertex;
import de.ueller.osmToGpsMid.model.name.Names;
import de.ueller.osmToGpsMid.model.TravelModes;
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
	public static final int		WAY_FLAG2_ADDITIONALFLAG			= 128;

	public static final byte WAY_FLAG3_URL = 1;
	public static final byte WAY_FLAG3_URLHIGH = 2;
	public static final byte WAY_FLAG3_PHONE = 4;
	public static final byte WAY_FLAG3_PHONEHIGH = 8;
	public static final byte WAY_FLAG3_NAMEASFORAREA = 16;
	public static final byte WAY_FLAG3_HAS_HOUSENUMBERS = 32;
	public static final byte WAY_FLAG3_LONGHOUSENUMBERS = 64;
	public static final int	 WAY_FLAG3_ADDITIONALFLAG = 128;

	public static final byte WAY_FLAG4_ALERT = 1;
	public static final byte WAY_FLAG4_CLICKABLE = 2;
	public static final byte WAY_FLAG4_HOLES = 4;

	public Long id;

	private Path					path								= null;
	private Path					trianglePath								= null;
	private ArrayList<Path> 		holes = null;
	public HouseNumber			housenumber							= null;
	public List<Triangle>		triangles							= null;
	//private Bounds				bound								= null;

	/** Up to 4 travel modes for which this way can be used (motorcar, bicycle, etc.)
	 *  The upper 4 bits equal to Connection.CONNTYPE_* flags
	 */
	public byte					wayTravelModes						= 0;
	/** Toll flag for up to 4 travel modes for which this way can be used (motorcar, bicycle, etc.)
	 *  upper bytes are unused
	 */
	public byte					wayTravelModes2						= 0;

	public static Configuration	config;
	/**
	 * Indicates that this way was already written to output;
	 */
	public boolean				used								= false;
	// polish.api.bigstyles
	public short				type								= -1;

	/**
	 * Way id of the last unhandled maxSpeed - by using this to detect repeats,
	 * we can quiet down the console output for unhandled maxspeeds.
	 */
	public static long			lastUnhandledMaxSpeedWayId			= -1;

	private static boolean writingAreaOutlines = false;
	private static boolean deleteAreaOutlines = true;

	public Way(long id) {
		this.id = id;
		this.path = new Path();
		this.trianglePath = new Path();
	}

	/**
	 * create a new Way with path clone from the other way, but doesn't share tags
	 * used e.g. for relation expansion
	 * 
	 * @param other
	 */
	public Way(long id, Way other) {
		this.id = id;
		this.path = new Path(other.path);
		this.trianglePath = new Path(other.trianglePath);
	}

	public Way(long id, ArrayList<Node> newPath) {
		this.id = id;
		this.path = new Path(newPath);
		//this.trianglePath = new Path(newPath);
	}

	/**
	 * create a new Way which shares the tags with the other way, has the same type and id, but direction reversed
	 * 
	 * @param other
	 */
	public Way(Way other, boolean reverse) {
		super(other);
		this.id = other.id;
		this.type = other.type;
		this.path = new Path(other.path, reverse);
		this.trianglePath = new Path(other.trianglePath, reverse);
	}

	/**
	 * create a new Way which shares the tags with the other way, has the same type and id, but no Nodes
	 * 
	 * @param other
	 */
	public Way(Way other) {
		super(other);
		this.id = other.id;
		this.type = other.type;
		this.path = new Path();
		this.trianglePath = new Path();
	}

	public void addHole(Way holeWay) {
		if (holes == null) {
			holes = new ArrayList<Path>();
		}
		Path holePath = new Path((ArrayList<Node>) holeWay.getNodes());
		holes.add(holePath);
	}

	public ArrayList<Path> getHoles() {
		return holes;
	}

	public void deletePath() {
		path = null;
	}

	public void cloneTags(Way other) {
		super.cloneTags(other);
		this.id = other.id;
		this.type = other.type;
	}

	public boolean isMainstreet() {
		WayDescription wayDesc = config.getWayDesc(type);
		if (wayDesc == null) { 
			return false; 
		}
		return wayDesc.isMainstreet();
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
		if (wayDesc == null) { 
			return; 
		}

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
			// mark only connections of accessible ways as toll connections
			if (isAccessForRouting(i) && isTollRoad(i)) {
				wayTravelModes2 |= (1 << i);
			}
		}

		if (getAttribute("toll") != null && "|yes|true|".indexOf("|" + getAttribute("toll").toLowerCase() + "|") >= 0 ) {
			wayTravelModes |= Connection.CONNTYPE_TOLLROAD;			
		}
	}

	public boolean isAccessForRouting(int travelModeNr) {
		return ((wayTravelModes & (1 << travelModeNr)) != 0);
	}

	public boolean isAccessForAnyRouting() {
		return (wayTravelModes != 0);
	}
	
	public boolean isAccessForRoutingInAnyTurnRestrictionTravelMode() {
		return (wayTravelModes & TravelModes.applyTurnRestrictionsTravelModes) > 0;
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
	
	/**
	 * Check way tags for tollRules from style-file
	 * 
	 * @param travelModeNr
	 *            : e.g. for motorcar or bicycle
	 * @return true if toll road, false if no toll road
	 */
	public boolean isTollRoad(int travelModeNr) {
		String value;
		boolean isToll = false;
		for (TollRule rToll : TravelModes.getTravelMode(travelModeNr).getTollRules()) {
			value = getAttribute(rToll.key);
			if (value != null && rToll.values.indexOf(value) != -1) {
				if (rToll.enableToll) {
					isToll = true;
				} else {
					isToll = false;
				}
				if (rToll.debugTollRule) {
					System.out.println(this.toUrl() + " matches toll rule: " + rToll.toString());
				}
			}
		}
		return isToll;
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

	// FIXME should not be hard-coded but taken from style-file
	public boolean hasHouseNumberTag() {
		return (containsKey("addr:housenumber"));
	}

	public boolean hasHouseNumber() {
		return housenumber != null;
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

	public void resetType(Configuration c) {
		type = -1;
	}

	// polish.api.bigstyles
	public short getType(Configuration c) {
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

	// polish.api.bigstyles
	public short getType() {
		if (type > -1) {
			return type;
		} else {
			return -1;
		}
	}

	// polish.api.bigstyles
	private short calcType(Configuration c) {
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
		 * housenumber search for buildings also requires this
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
					if (type < 0) {
						type = -2;
					}
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
				if (name != null && nameFallback != null && (!desc.nameFallbackKey.equals("*") || !desc.key.equals(desc.nameKey))) {
					if (name.length() + nameFallback.length() > 125) {
						// cut too long names
						int namelen = name.length();
						if (namelen > 60) {
							namelen = 60;
						}
						int nameFallbackLen = nameFallback.length();
						if (namelen + nameFallbackLen > 125) {
							nameFallbackLen = 120-namelen;
						}
						name = name.substring(0, namelen) + ".. (" + nameFallback.substring(0, nameFallbackLen) + ")";
					} else {
						name += " (" + nameFallback + ")";
					}
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
		Bounds b = null;
		byte tileLevelFromDiameter = 0;
		
		// calculate tile level for areas based on diameter
		if (isArea()) {
			b = getBounds();
			int diameter = (int) (MyMath.calcDistance(
					Math.toRadians(b.minLat),
					Math.toRadians(b.minLon),
					Math.toRadians(b.maxLat),
					Math.toRadians(b.maxLon)
			) * MyMath.PLANET_RADIUS);
//			if (getName().indexOf("Volksbad") >= 0) {
//				System.out.println ("lat: " + b.minLat + " lon: " + b.minLon + " : " + diameter + "  " + getName());
//			}
			if (		diameter < LegendParser.tileLevelAttractsAreasWithSmallerBoundsDiameterThan[3] 
			    		&&
			    		(LegendParser.tileScaleLevelIsAllowedForRoutableWays[3] || !isAccessForAnyRouting())
			) {
				tileLevelFromDiameter = 3;
			} else if (	diameter < LegendParser.tileLevelAttractsAreasWithSmallerBoundsDiameterThan[2]
			    		&&
			    		(LegendParser.tileScaleLevelIsAllowedForRoutableWays[2] || !isAccessForAnyRouting())
			) {
				tileLevelFromDiameter = 2;
			} else if (	diameter < LegendParser.tileLevelAttractsAreasWithSmallerBoundsDiameterThan[1]
                        &&
                        (LegendParser.tileScaleLevelIsAllowedForRoutableWays[1] || !isAccessForAnyRouting())
            ) {
				tileLevelFromDiameter = 1;
			}
		}

		// polish.api.bigstyles
		short type = getType(c);
		
		if (type < 0) {
			// System.out.println("unknown type for node " + toString());
			return 3;
		}
		int maxScale = c.getWayDesc(type).minEntityScale;
		if (maxScale < LegendParser.tileScaleLevel[3]) { // 45000 in GpsMid 0.5.0
			if (LegendParser.tileScaleLevelIsAllowedForRoutableWays[3] || !isAccessForAnyRouting()) {
				return 3;
			}
		}
		if (maxScale < LegendParser.tileScaleLevel[2]) { // 180000 in GpsMid 0.5.0
			if (LegendParser.tileScaleLevelIsAllowedForRoutableWays[2]|| !isAccessForAnyRouting()) {
				if (tileLevelFromDiameter > 2) { // if based on diameter the area would be in a higher zoom level put it there
					return tileLevelFromDiameter;
				}
				return 2;
			}
		}
		if (maxScale < LegendParser.tileScaleLevel[1]) { // 900000 in GpsMid 0.5.0
			if (LegendParser.tileScaleLevelIsAllowedForRoutableWays[1] || !isAccessForAnyRouting()) {
				if (tileLevelFromDiameter > 1) { // if based on diameter the area would be in a higher zoom level put it there
					return tileLevelFromDiameter;
				}
				return 1;
			}
		}
		if (tileLevelFromDiameter > 0) {  // if based on diameter the area would be in a higher zoom level put it there
			return tileLevelFromDiameter;
		}
		return 0;
	}

	/**
	 * Returns the maximum speed in km/h if explicitly set for this way, if not, it returns -1.0.
	 */
	public float getMaxSpeed() {
		float maxSpeed = -1.0f;
		String maxSpeedAttr = getAttribute("maxspeed");
		if (maxSpeedAttr != null) {			
			maxSpeed = parseMaxSpeed(maxSpeedAttr, false);
		}
		return maxSpeed;
	}

	/**
	 * Returns the winter maximum speed in km/h if explicitly set for this way, if not, it returns -1.0.
	 */
	public float getMaxSpeedWinter() {
		float maxSpeed = -1.0f;
		String maxSpeedAttr = getAttribute("maxspeed:seasonal:winter");
		if (maxSpeedAttr != null) {
			maxSpeed = parseMaxSpeed(maxSpeedAttr, true);
		}
		return maxSpeed;
	}

	/** Parses the given maxspeed string
	 * @param maxSpeedAttr String to parse
	 * @param winterFlag Flag if it's a winter maxspeed, only needed for warning output
	 * @return Maxspeed as float or -1.0 if the template couldn't be found 
	 */
	private float parseMaxSpeed(String maxSpeedAttr, boolean winterFlag) {
		float maxSpeed = -1.0f;
		if (maxSpeedAttr.equalsIgnoreCase("default")) {
			/**
			 * We can't match this, so ignore it and notify about it.
			 */
			if (this.id != lastUnhandledMaxSpeedWayId) {
				System.out.println("Warning: Ignoring map data: Unhandled maxspeed"
					+ (winterFlag ? "winter" : "") + " for way " + 
						toString() + ": " + maxSpeedAttr);
				lastUnhandledMaxSpeedWayId = this.id;
			}
		} else if (maxSpeedAttr.equalsIgnoreCase("variable") 
				|| maxSpeedAttr.equalsIgnoreCase("signals")) {
			maxSpeed = Configuration.MAXSPEED_MARKER_VARIABLE;
		} else if (maxSpeedAttr.equalsIgnoreCase("none")
				|| maxSpeedAttr.equalsIgnoreCase("no")) {
			maxSpeed = Configuration.MAXSPEED_MARKER_NONE;
		} else {
			try {
				boolean mph = false;
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
						System.out.println("Warning: Ignoring map data: Unhandled maxspeed" 
							+ (winterFlag ? "winter" : "") + " for way " + 
								toString() + ": " + maxSpeedAttr);
						lastUnhandledMaxSpeedWayId = this.id;
					}
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
		// polish.api.bigstyles
		short t1 = getType();
		short t2 = o.getType();
		if (t1 < t2) {
			return 1;
		} else if (t1 > t2) { return -1; }
		return 0;
	}

	public Bounds getBounds() {
		// always calculate current bounds
		Bounds bound = new Bounds();
		if (triangles != null && triangles.size() > 0) {
			for (Triangle t : triangles) {
				bound.extend(t.getVert()[0].getLat(), t.getVert()[0].getLon());
				bound.extend(t.getVert()[1].getLat(), t.getVert()[1].getLon());
				bound.extend(t.getVert()[2].getLat(), t.getVert()[2].getLon());
			}
		} else {
			if (path != null) {
				path.extendBounds(bound);
			}
			if (trianglePath != null) {
				trianglePath.extendBounds(bound);
			}
		}
		return bound;
	}

	/*public void clearBounds() {
		bound = null;
	}*/

	/** Simplistic check to see if this way/area "contains" another - for
	 *  speed, all we do is check that all of the other way's points
	 *  are inside this way's polygon.
	 * @param other
	 * @return 
	 */
	public boolean containsPointsOf(Way other) {
		// This method was ported from mkgmap (uk.me.parabola.mkgmap.reader.osm.Way).

		Polygon thisPoly = new Polygon();
		for (Node n : getNodes()) {
			thisPoly.addPoint((int)(n.getLon() * MyMath.FIXPT_MULT), 
					(int)(n.getLat() * MyMath.FIXPT_MULT));
		}
		for (Node n : other.getNodes()) {
			if (!thisPoly.contains((int)(n.getLon() * MyMath.FIXPT_MULT), 
					(int)(n.getLat() * MyMath.FIXPT_MULT))) {
				return false;
			}
		}
		return true;
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

	/**
	 * @return String with the URL to inspect this way on the OSM website
	 */
	public String toUrl() {
		return "http://www.openstreetmap.org/browse/way/" + id;
	}

	/**
	 * @return The value of the attribute "is_in"
	 */
	public String getIsIn() {
		return getAttribute("is_in");
	}

	/**
	 * @return
	 */
	// polish.api.bigstyles
	public short getNameType() {
		String t = getAttribute("highway");
		if (t != null) { return (Constants.NAME_STREET); }
		return Constants.NAME_AMENITY;
	}

	/**
	 * @return Node in the middle or null if way has no nodes
	 */
	public Node getMidPointNodeByBounds() {
		Bounds b = getBounds();
		return (new Node((b.maxLat + b.minLat) / 2, (b.maxLon + b.minLon) / 2, -1));
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

	/**
	 * @return
	 */
	public boolean isOneWayMinusOne() {
		return ("-1".equals(getAttribute("oneway")));
	}

	/** Check if cycleway=opposite or cycleway=opposite_track or cycleway=opposite_lane is set */
	public boolean isOppositeDirectionForBicycleAllowed() {
		String s = getAttribute("cycleway");
		if (s == null) { return false; }
		return ("|opposite|opposite_track|opposite_lane|".indexOf("|" + s.toLowerCase() + "|") >= 0);
	}

	/** Writes this way's data (flags, type, travel modes, indices, node count etc.).
	 * 
	 * @param ds Stream to write to
	 * @param names1 The way's name index is in this list
	 * @param urls1 The way's URL index is in this list
	 * @param t Tile to which this way belongs - bounds coordinates are relative to its center
	 * @throws IOException
	 */
	public void write(DataOutputStream ds, Names names1, Urls urls1, Tile t, boolean outlineFormat) throws IOException {
		
		Bounds b = new Bounds();
		int flags = 0;
		int flags2 = 0;
		int flags3 = 0;
		int flags4 = 0;
		int maxspeed = 50;
		int maxspeedwinter = 50;
		int nameIdx = -1;
		int urlIdx = -1;
		int phoneIdx = -1;
		byte layer = 0;
		if (outlineFormat) {
			writingAreaOutlines = true;
		} else {
			writingAreaOutlines = false;
		}

		if (config == null) {
			config = Configuration.getConfiguration();
		}
		if (id == 39123631) {
			System.out.println("Write way 39123631");
		}

		// polish.api.bigstyles
		short type = getType();

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
		if (holes != null && writingAreaOutlines) {
			flags4 += WAY_FLAG4_HOLES;
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
		boolean longHouseNumbers = false;

		if (type < 1) {
			System.out.println("ERROR! Invalid way type for way " + toString());
		}

		if (getNodeCount() > 255) {
			longWays = true;
		}
		if (housenumber != null) {
			// FIXME maybe enable later for viewing way-related house numbers on map;
			// disable for now, as GpsMid doesn't use this yet
			// flags3 += WAY_FLAG3_HAS_HOUSENUMBERS;
			if (getHouseNumberCount() > 255) {
				longHouseNumbers = true;
			}
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
		if (flags4 != 0) {
			flags3 += WAY_FLAG3_ADDITIONALFLAG;
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

		// polish.api.bigstyles
		if (Configuration.getConfiguration().bigStyles) {
			ds.writeShort(type);
		} else {
			ds.writeByte(type);
		}
		
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
		if (flags4 != 0) {
			ds.writeByte(flags4);
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
		if ((flags3 & WAY_FLAG3_HAS_HOUSENUMBERS) == WAY_FLAG3_HAS_HOUSENUMBERS){
			System.out.println("Writing housenumbers (nodecount=" + getHouseNumberCount() + ") for way " + this);
			if (longHouseNumbers) {
				ds.writeShort(getHouseNumberCount());
			} else {
				ds.writeByte(getHouseNumberCount());
			}
			for (Node n : housenumber.getNodes()) {
				ds.writeLong(n.id);
				System.out.println("Writing node " + n);
			}

		}
		if (isArea() && !writingAreaOutlines) {
			if (longWays) {
				ds.writeShort(getNodeCount());
			} else {
				ds.writeByte(getNodeCount());
			}
			for (Triangle tri : checkTriangles()) {
				ds.writeShort(tri.getVert()[0].getNode().renumberdId);
				ds.writeShort(tri.getVert()[1].getNode().renumberdId);
				ds.writeShort(tri.getVert()[2].getNode().renumberdId);
			}
		} else {
			if (longWays) {
				ds.writeShort(path.getNodeCount());
			} else {
				ds.writeByte(path.getNodeCount());
			}
			// FIXME if there are holes in the area, we should write the holes here too,
			// probably requires changing map format. Probably a new flag bit for the way,
			// has_holes, and if set, after the nodes there would be a count of the holes
			// and then each hole stored like the outline.
			for (Node n : path.getNodes()) {
				ds.writeShort(n.renumberdId);
			}
		}
		if (isArea() && writingAreaOutlines && holes != null) {
			int holeCount = holes.size();
			//System.out.println("Way.java: holecount " + holes.size());
			//int n = 0;
			//for (Path hole : holes) {
			//	Path holeNodes = holes.get(n++);
			//System.out.println("Way.java: hole " + n + " nodecount: " + holeNodes.getNodeCount());
			//}
			ds.writeShort(holeCount);
			int nCount = 0;
			for (Path hole : holes) {
				//System.out.println("Way.java: hole " + nCount++ + " nodecount: " + hole.getNodeCount());
				ds.writeShort(hole.getNodeCount());
				for (Node n : hole.getNodes()) {
					ds.writeShort(n.renumberdId);
				}
			}
		}

		if (config.enableEditingSupport) {
			if (id > Integer.MAX_VALUE) {
				// commented out by jkpj 2011-04-24 as the fake id generates triggers a lot of these
				// FIXME make a scheme of marking fake ids so we can show real warnings
				//System.out.println("WARNING: OSM-ID won't fit in 32 bits for way " + this);
				ds.writeInt(-1);
			} else {
				ds.writeInt(id.intValue());
			}
		}

	}

	public void addNode(Node n) {
		path.add(n);
	}

	public void addNodeIfNotEqualToLastNode(Node node) {
		if (path.getNodeCount() == 0 || !node.equals(path.getNode(path.getNodeCount() - 1))) {
			path.add(node);
		}
	}

	public void addNodeIfNotEqualToFirstNodeOfTwo(Node node) {
		if (!(path.getNodeCount() == 2 && node.equals(path.getNode(0)))) {
			path.add(node);
		}
	}

	// add node with interim nodes to make splitting possible
	public void addNodeIfNotEqualToLastNodeWithInterimNodes(Node node) {
		//System.out.println("nodecount: " + path.getNodeCount());
		if (path.getNodeCount() != 0) {
			Node oldNode = path.getNode(path.getNodeCount() - 1);
			if (oldNode != null && !node.equals(oldNode)) {
				double dist = MyMath.dist(oldNode, node);
				int count = (int) dist / 50;
				//System.out.println("Dist = " + dist + " count= " + count);
				float oldLat = oldNode.getLat();
				float oldLon = oldNode.getLon();
				float latDiff = node.getLat() - oldLat;
				float lonDiff = node.getLon() - oldLon;
				for (int i = 1 ; i < count ; i++) {
					Node interim = new Node(oldLat + latDiff * i / count, oldLon + lonDiff * i / count, FakeIdGenerator.makeFakeId());					
					path.add(interim);
				}
			}
		}
		if (path.getNodeCount() == 0 || !node.equals(path.getNode(path.getNodeCount() - 1))) {
			path.add(node);
		}
	}

	public void houseNumberAdd(Node n) {
		if (housenumber == null) {
			housenumber = new HouseNumber();
		}
		housenumber.add(n);
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
		for (Node n : ((path != null ? path : trianglePath).getNodes())) {
			if (n.getType(config) == -1) {
				return n;
			}
		}
		return null;
	}

	public ArrayList<RouteNode> getAllRouteNodesOnTheWay() {
		ArrayList<RouteNode> returnNodes = new ArrayList<RouteNode>();
		for (Node n : ((path != null && getNodeCount() > 0) ? path : trianglePath).getNodes()) {
			if (n.routeNode != null) {
				returnNodes.add(n.routeNode);
			}
		}
		return returnNodes;
	}

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
		if (path != null) {
			path.replace(replaceNodes);
		}
		if (trianglePath != null) {
			trianglePath.replace(replaceNodes);
		}
	}

	// public List<SubPath> getSubPaths() {
	// return path.getSubPaths();
	// }

	/** Checks if this is an areas and triangulates it if this hasn't been done yet. 
	 * 
	 * @return List of triangles for this way
	 */
	public List<Triangle> checkTriangles() {
		if (!Configuration.getConfiguration().triangleAreaFormat) {
			return null;
		}
		if (isArea() && triangles == null) {
			triangulate();
		}
		return triangles;
	}

	public int getLineCount() {
		if (isArea() && Configuration.getConfiguration().triangleAreaFormat && !writingAreaOutlines) {
			return checkTriangles().size();
		} else {
			return path.getLineCount();
		}
	}

	public int getNodeCount() {
		if (isArea() && Configuration.getConfiguration().triangleAreaFormat && !writingAreaOutlines) {
			return checkTriangles().size() * 3;
		} else {
			return path.getNodeCount();
		}
	}

	public int getHouseNumberCount() {
		return housenumber.getHouseNumberCount();
	}

	public Way split() {
		if (isArea()) {
			//if (writingAreaOutlines) {
			if (Configuration.getConfiguration().outlineAreaFormat) {
				// FIXME write a splitter mode which works with outlined areas
				return null;
			} else {
				return splitArea();
			}
		} else {
			return splitNormalWay();
		}
	}


	private Way splitArea() {
		if (!Configuration.getConfiguration().triangleAreaFormat) {
			return null;
		}
		if (triangles == null) {
			triangulate();
		}
		// System.out.println("Split area id=" + id + " s=" + triangles.size());
		Way newWay = new Way(this);
		Bounds newBbounds = getBounds().split()[0];
		ArrayList<Triangle> tri = new ArrayList<Triangle>(1);
		ArrayList<Triangle> tri2 = new ArrayList<Triangle>(1);
		for (Triangle t : triangles) {
			Vertex midpoint = t.getMidpoint();
			if (!newBbounds.isIn(midpoint.getLat(), midpoint.getLon())) {
				tri.add(t);
			} else {
				tri2.add(t);
			}
		}
		if (tri.size() == triangles.size()) {
			// System.out.println("split area triangles: all " + tri.size() + " would go the other way");
			// all triangles would go to the other way
			return null;
		}

//		for (Triangle t : tri) {
//			triangles.remove(t);
//		}
		triangles = tri2;
		// System.out.println("split area triangles " + "s1=" + triangles.size() + " s2=" + tri.size());
		if (tri.size() == 0) { return null; }
		//clearBounds();
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
			//this.clearBounds();
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
		if ((path == null || path.getNodeCount() == 0) && (trianglePath == null || trianglePath.getNodeCount() == 0)) {
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

	/** 
	 * @return True if the way is a closed polygon with a clockwise direction.
	 */
	public boolean isCounterClockwise() {
		// adapted from isCclockwise()
		
		if (getNodes().size() < 3 || 
				!getNodes().get(0).equals(getNodes().get(getNodes().size() - 1))) {
			return false;
		}

		long area = 0;
		Node n1 = getNodes().get(getNodes().size()-1);
		for (int i = getNodes().size()-2; i >= 0; --i) {
			Node n2 = getNodes().get(i);
			area += ((long)n1.getLongLon() * n2.getLongLat() - 
					 (long)n2.getLongLon() * n1.getLongLat());
			n1 = n2;
		}

		// this test looks to be inverted but gives the expected result!
		//System.out.println("Counterclockwise for way " + toUrl() + " area = " + area);
		return area < 0;
	}

	/** 
	 * @return True if the way is a closed polygon with a clockwise direction.
	 */
	public boolean isClockwise() {
		// This method was ported from mkgmap (uk.me.parabola.mkgmap.reader.osm.Way).
		
		if (getNodes().size() < 3 || 
				!getNodes().get(0).equals(getNodes().get(getNodes().size() - 1))) {
			return false;
		}

		long area = 0;
		Node n1 = getNodes().get(0);
		for (int i = 1; i < getNodes().size(); ++i) {
			Node n2 = getNodes().get(i);
			area += ((long)n1.getLongLon() * n2.getLongLat() - 
					 (long)n2.getLongLon() * n1.getLongLat());
			n1 = n2;
		}

		// this test looks to be inverted but gives the expected result!
		return area < 0;
	}

	/**
	 * @return The OSM ID of this way
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return List of all nodes of this way
	 */
	public List<Node> getNodes() {
		return path.getNodes();
	}

	/**
	 * @param wayHashMap
	 * @param r
	 */
	public void triangulate() {
		if (!Configuration.getConfiguration().triangleAreaFormat) {
			return;
		}

		if (isArea()) {
			Outline o = null;
			o = new Outline();
			o.setWayId(id);
			
			if (Configuration.getConfiguration().outlineAreaFormat) {
				for (Node n : trianglePath.getNodes()) {
					o.append(new Vertex(n, o));
				}
			} else {
				for (Node n : getNodes()) {
					o.append(new Vertex(n, o));
				}
			}
			Area a = new Area();
			a.addOutline(o);
			triangles = a.triangulate();
			recreatePathAvoidDuplicates();
		} else {
			System.err.println("Can't triangulate normal ways");
		}
	}

	/**
	 * Regenerates this way's path object, rough version for speed
	 */
	public void recreatePath() {
		if (Configuration.getConfiguration().outlineAreaFormat) {
			deleteAreaOutlines = false;
		} else {
			deleteAreaOutlines = true;
		}
		if (isArea() && triangles.size() > 0) {
			trianglePath = new Path();
			if (deleteAreaOutlines) {
				deletePath();
			}
		}
		for (Triangle t : triangles) {
			for (int l = 0; l < 3; l++) {
				Node n = t.getVert()[l].getNode();
				//if (!trianglePath.getNodes().contains(n)) {
					trianglePath.add(n);
				//}
			}
		}
		((ArrayList)triangles).trimToSize();
		trimTrianglePath();
		//clearBounds();
	}
	/**
	 * Regenerates this way's path object, avoiding duplicates
	 */
	public void recreatePathAvoidDuplicates() {
		if (Configuration.getConfiguration().outlineAreaFormat) {
			deleteAreaOutlines = false;
		} else {
			deleteAreaOutlines = true;
		}
		if (isArea() && triangles.size() > 0) {
			trianglePath = new Path();
			if (deleteAreaOutlines) {
				path = trianglePath;
			}
		}
		for (Triangle t : triangles) {
			for (int l = 0; l < 3; l++) {
				Node n = t.getVert()[l].getNode();
				// FIXME this is costly
				if (!trianglePath.getNodes().contains(n)) {
					trianglePath.add(n);
				}
			}
		}
		((ArrayList)triangles).trimToSize();
		trimTrianglePath();
		//clearBounds();
	}

	public void trimTrianglePath()
	{
		if (trianglePath != null )
			trianglePath.trimPath();
	}
	public void trimPath()
	{
		if (path != null )
			path.trimPath();
	}

}
