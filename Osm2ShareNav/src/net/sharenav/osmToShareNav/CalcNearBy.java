/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007        Harald Mueller
 * Copyright (C) 2007, 2008  Kai Krueger
 */

package net.sharenav.osmToShareNav;

import java.lang.Long;
import java.util.HashMap;
import java.util.List;

import net.sharenav.osmToShareNav.model.Entity;
import net.sharenav.osmToShareNav.model.Node;
import net.sharenav.osmToShareNav.model.Way;
import net.sharenav.osmToShareNav.model.POIdescription;
import net.sharenav.osmToShareNav.model.WayDescription;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;


public class CalcNearBy {
	private int kdSize = 0; // Hack around the fact that KD-tree doesn't tell us its size
	private static int kdWaysSize = 0;

	private static KDTree nearByWays;

	private int exactCount = 0;
	private int heuristicCount = 0;

	public CalcNearBy(OsmParser parser) {
		KDTree nearByElements = getNearByElements(parser);
		if (Configuration.getConfiguration().useHouseNumbers) {
			nearByWays = getNearByWays(parser);
		}
		if (kdSize > 0) {
			calcCityNearBy(parser, nearByElements);
			calcWayIsIn(parser, nearByElements);
		}
		if (kdWaysSize > 0) {
			CalcWaysForHouseNumberAreas(parser, nearByWays);
			calcWaysForHouseNumbers(parser, nearByWays);
		}
	}

	// FIXME: this is a pretty crude and error-prone way to do this, rewrite to be better
	// should use a better algorithm for findind the proper way

	// ShareNav has a place where the nearest way to a destination is found,
	// perhaps that can be used.

	// sk750 April 2011: What you want to do with finding the closest way for a house number in Osm2ShareNav
	// might be a mixture of closest point on a line in ShareNav
	// and the traffic signal route node marking in Osm2ShareNav -
	// though I this misses marking some route nodes
	// because it doesn't look over tile boundaries for performance reasons.
	// 
	// Plan:
	// 1) get a bunch of nearby (by midpoint) named ways (maybe two dozen or so),
	// 2) if addr:street matches to exactly one of the ways, mark that way as associated
	// 3) order the ways by distance from the node to the closest point of the way
        // 4) mark the nearest way as associated 
	// 5) maybe mark also one or two other nearest ways as associated, depending on .properties config
	//    (false nearby positives might be considered a lesser evil than not finding a housenumber)
	//    (the storage model currently doesn't allow for several way, so that would
        //    involve a change to storage or cloning of the node)
	// Update: plan carried out except for marking muptiple ways and sorting addr:street matches

	public long calcWayForHouseNumber(Entity n, HashMap<Long, Way> wayHashMap) {
		POIdescription poiDesc = null;
		WayDescription wayDesc = null;
		if (n instanceof Way) {
			wayDesc = Configuration.getConfiguration().getWayDesc(((Way) n).getType(null));
		} else {
			poiDesc = Configuration.getConfiguration().getpoiDesc(((Node) n).getType(null));
		}
		//System.out.println("poidesc: " + poiDesc + " waydesc: " + wayDesc);
		String streetNameAttr = "addr:street";
		if (poiDesc != null && poiDesc.houseNumberMatchTag != null) {
			streetNameAttr = poiDesc.houseNumberMatchTag;
			//System.out.println("Streetnameattr: " + streetNameAttr);
		}
		if (wayDesc != null && wayDesc.houseNumberMatchTag != null) {
			streetNameAttr = wayDesc.houseNumberMatchTag;
			//System.out.println("Streetnameattr: " + streetNameAttr);
		}
		String streetName = n.getAttribute(streetNameAttr);
		//System.out.println("Streetnameattr: " + streetNameAttr);
		Node nearestWay = null;				
		try {					
			Node thisNode = null;
			if (n instanceof Node) {
				thisNode = (Node) n;
			} else {
				Way w = (Way) n;
				thisNode = w.getMidPoint();
			}
			nearestWay = (Node) nearByWays.nearest(MyMath.latlon2XYZ(thisNode));					
			long maxDistanceTested = MyMath.dist(thisNode, nearestWay);

			int retrieveN = 25;
			int retrieveNforName = 100;
			if (retrieveN > kdWaysSize) {
				retrieveN = kdWaysSize;
			}
			if (retrieveNforName > kdWaysSize) {
				retrieveNforName = kdWaysSize;
			}
			nearestWay = null;
			long dist = 0;
			Object [] nearWays = null;
			// first look for matching street name in nearby streets
			dist = 0;
			nearWays = nearByWays.nearest(MyMath.latlon2XYZ(thisNode), retrieveNforName);
			for (Object o : nearWays) {
				Node other = (Node) o;								
				dist = MyMath.dist(thisNode, other);
				String otherName = other.getAttribute("__wayname");
				if (otherName != null && streetName != null) {
					//System.out.println ("trying to match addr:street, comparing " + streetName + " to " + otherName);
					// FIXME to be pedantically correct, could do this after finding closest points in the
					// candidate ways. Probably not worth the cost, as apparently the only thing this would
					// change in some cases ist that is_in info for housenumber streetname would be different
					// in some cases
					if (streetName.equalsIgnoreCase(otherName)) {
						nearestWay = other;
						exactCount++;
						break;
					}
				}
			}
			//if (nearestWay != null) {
			//	System.out.println ("found addr:street match for node " + n + " : street: " + nearestWay);
			//}
			maxDistanceTested = dist;
			if (nearestWay == null) {
				//System.out.println ("Start heuristic way search for " + thisNode);
				nearestWay = (Node) nearByWays.nearest(MyMath.latlon2XYZ(thisNode));					
				maxDistanceTested = MyMath.dist(thisNode, nearestWay);
				nearestWay = null;
				retrieveN = 25;
				dist = maxDistanceTested;
				if (retrieveN > kdWaysSize) {
					retrieveN = kdWaysSize;
				}
				//System.out.println ("First way " + nearestWay + " at dist " + dist);
				nearWays = nearByWays.nearest(MyMath.latlon2XYZ(thisNode), retrieveN);
				// then look for other named ways
				for (Object o : nearWays) {
					Node other = (Node) o;								
					if (nearestWay == null) {
						nearestWay = other;
					}
					//dist = MyMath.dist(thisNode, other);
					//As the list returned by the kd-tree is sorted by distance,
					//we can stop at the first found plus some (to match for street name)
					// FIXME add calculation for real distance to street
					long wayId = other.id;
					Way w = wayHashMap.get(wayId);
					long distToWay = distanceToWay(thisNode, w, dist);
					if (distToWay < dist) {
						dist = distToWay;
						nearestWay = other;
						//System.out.println ("Found closer way " + nearestWay + " at dist " + dist);
					}
				}
				if (nearestWay != null) {
					heuristicCount++;
					maxDistanceTested = dist;
					//found a suitable Way
					//System.out.println ("decided a heuristic match for node " + n
					//	    + " streetName: " + nearestWay);
				}
			}
		} catch (KeySizeException e) {
			// Something must have gone horribly wrong here,
			// This should never happen.					
			e.printStackTrace();
			return 0;
		}
		if (nearestWay != null) {
			return nearestWay.id;
		}				
		return (long) 0;
	}

	/**
	 * @param parser
	 * @param nearByElements
	 */
	private void calcWayIsIn(OsmParser parser, KDTree nearByElements) {		
		long startTime = System.currentTimeMillis();
		for (Way w : parser.getWays()) {
			if (w.getName() != null) {
				calcEntityIsIn(parser, nearByElements, (Entity) w);
			}
		}
		long time = (System.currentTimeMillis() - startTime);
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("info: calcEntityIsIn for ways took " + time / 1000 + " seconds");
		}
		startTime = System.currentTimeMillis();
		for (Node n : parser.getNodes()) {
			if (n.getName() != null) {
				calcEntityIsIn(parser, nearByElements, (Entity) n);
			}
		}
		time = (System.currentTimeMillis() - startTime);
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("info: calcEntityIsIn for nodes took " + time / 1000 + " seconds");
		}

	}

	private void calcEntityIsIn(OsmParser parser, KDTree nearByElements, Entity e) {
		// index way, poi or area with is_in
		Node thisNode = null;
		if (e instanceof Node) {
			thisNode = (Node) e;
		}
		if (e instanceof Way) {
			Way w = (Way) e;
			thisNode = w.getMidPoint();
		}
		if (thisNode == null) {
			return;
		}
		Node nearestPlace = null;				
		try {					
			nearestPlace = (Node) nearByElements.nearest(MyMath.latlon2XYZ(thisNode));

			if (nearestPlace.getType(null) <= 5 && !(MyMath.dist(thisNode, nearestPlace) < Constants.MAX_DIST_CITY[nearestPlace.getType(null)])) {					
				long maxDistanceTested = MyMath.dist(thisNode, nearestPlace);
				int retrieveN = 5;
				if (retrieveN > kdSize) {
					retrieveN = kdSize;
				}
				nearestPlace = null;
				while (maxDistanceTested < Constants.MAX_DIST_CITY[Constants.NODE_PLACE_CITY]) {							
					Object [] nearPlaces = nearByElements.nearest(MyMath.latlon2XYZ(thisNode), retrieveN);
					long dist = 0;
					for (Object o : nearPlaces) {
						Node other = (Node) o;								
						dist = MyMath.dist(thisNode, other);
						//As the list returned by the kd-tree is sorted by distance,
						//we can stop at the first found 
						if (other.getType(null) <= 5 && dist < Constants.MAX_DIST_CITY[other.getType(null)]) {								
							nearestPlace = other;									
							break;
						}							
					}
					if (nearestPlace != null) {
						//found a suitable Place, leaving loop
						break;
					}
					if (retrieveN == kdSize) {
						/**
						 * We have checked all available places and nothing was
						 * suitable, so abort with nearestPlace == null;
						 */
						break;
					}
					maxDistanceTested = dist;
					retrieveN = retrieveN * 5;
					if (retrieveN > kdSize) {
						retrieveN = kdSize;
					}
				}
			}
		} catch (KeySizeException exc) {
			// Something must have gone horribly wrong here,
			// This should never happen.					
			exc.printStackTrace();
			return;
		}
		if (nearestPlace != null) {
			if (!e.containsKey("is_in")) {
				e.setAttribute("is_in", nearestPlace.getName());					
			}
			// don't overwrite info from calcCityNearBy()
			if (e.nearBy == null) {
				e.nearBy = nearestPlace;
			}
		}				
	}		
		
	/**
	 * @param parser
	 * @param nearByElements
	 */
	private void calcWaysForHouseNumbers(OsmParser parser, KDTree nearByElements) {		
		long startTime = System.currentTimeMillis();
		int count = 0;
		int ignoreCount = 0;
		HashMap<Long, Way> wayHashMap = parser.getWayHashMap();
		for (Node n : parser.getNodes()) {
			if (n.hasHouseNumberTag()) {
				long way = calcWayForHouseNumber((Entity) n, wayHashMap);
				//System.out.println ("Got id " + way + " for housenumber node " + n);
				if (way != 0 && !n.containsKey("__wayid")) {
					count++;
					n.setAttribute("__wayid", Long.toString(way));
					Way w = wayHashMap.get(way);
					if (w != null) {
						w.houseNumberAdd(n);
					}
				} else {
					if (way == 0) {
						System.out.println("Warning: ignoring map housenumber data: node: "
								   + n + " result from calcWayForHouseNumber: " + way);
						ignoreCount++;
					}
				}
			}
		}		
		long time = (System.currentTimeMillis() - startTime);
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("info: node housenumbers: accepted " + count + " non-relation housenumber-to-street connections in " + time / 1000 + " seconds");
			System.out.println("info: node housenumbers: ignored " + ignoreCount + " non-relation housenumber-to-street connections");
			System.out.println("info: node+area housenumbers:  " + exactCount + " exact matches");
			System.out.println("info: node+area housenumbers:  " + heuristicCount + " heuristic matches (housenumbers without addr:street or addr:street not found)");
		}
	}

	private void CalcWaysForHouseNumberAreas(OsmParser parser, KDTree nearByElements) {		
		long startTime = System.currentTimeMillis();
		int count = 0;
		HashMap<Long, Way> wayHashMap = parser.getWayHashMap();
		for (Way w : parser.getWays()) {
			if (w.hasHouseNumberTag()) {
				count++;
				Node n = w.getMidPoint();
				long way = calcWayForHouseNumber((Entity) n, wayHashMap);
				if (way != (long) 0) {
					// if there's a relation, __wayid has already been set at relation handling
					if (!w.containsKey("__wayid")) {
						w.setAttribute("__wayid", Long.toString(way));
					}
					// FIXME if we want to have nodes for housenumbers in ShareNav
					// (if we want to list housenumber nodes in way data structure),
					// we'll probably need to create fake IDs and add the nodes to parser. Takes more space and gets more complicated.

					// w.houseNumberAdd(n);
					//System.out.println("Adding housenumber helper tag __wayid " + way + " to way " + w + " midpoint: " + n );
				}
				//n.wayToPOItransfer(w);
				//parser.addNode(n);
			}
		}		
		long time = (System.currentTimeMillis() - startTime);
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("info: area housenumbers: accepted " + count + " non-relation housenumber-to-street connections in " + time / 1000 + " seconds");
		}
	}

	private void calcCityNearBy(OsmParser parser, KDTree nearByElements) {
		long startTime = System.currentTimeMillis();
		//double [] latlonKey = new double[2];
		for (Node n : parser.getNodes()) {
			String place = n.getPlace();
			if (place != null) {
				Node nearestPlace = null;
				int nneighbours = 10;
				long nearesDist = Long.MAX_VALUE;
				while ((nearestPlace == null)) {
					nearesDist = Long.MAX_VALUE;
					Object[] nearNodes = null;

					if (kdSize < nneighbours) {
						nneighbours = kdSize;
					}
					try {
						nearNodes = nearByElements.nearest(
								MyMath.latlon2XYZ(n), nneighbours);
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
						return;
					} catch (KeySizeException e) {
						e.printStackTrace();
						return;
					} catch (ClassCastException cce) {
						System.out.println(nearNodes);
						return;
					}
					for (Object otherO : nearNodes) {
						Node other = (Node) otherO;
						if ((n.getType(null) > other.getType(null)) && (other.getType(null) > 0)) {
							long dist = MyMath.dist(n, other);
							if (dist < nearesDist) {
								nearesDist = dist;
								nearestPlace = other;
							}
						}
					}
					if (nneighbours == kdSize) {
						break;
					}
					nneighbours *= 5;
				}
				if (nearestPlace != null) {
					n.nearBy = nearestPlace;
					//n.nearByDist = nearesDist;					
//					System.out.println(n + " near " + n.nearBy);
				}
			}
		}
		long time = (System.currentTimeMillis() - startTime);
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("info: city nearbys created in " + time / 1000 + " seconds");
		}
	}

	private KDTree getNearByElements(OsmParser parser) {
		long startTime = System.currentTimeMillis();
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("Creating nearBy candidates");
		}
		KDTree kd = new KDTree(3);
		//double [] latlonKey = new double[2]; 
		for (Node n : parser.getNodes()) {
			if (n.isPlace()) {
				//latlonKey[0] = n.lat;
				//latlonKey[1] = n.lon;
				if (n.getName() == null || n.getName().trim().length() == 0) {
					System.out.println("STRANGE: place without name, skipping: " + n);
					System.out.println("  Please fix in OSM: " + n.toUrl());
					continue;
				}
				try {
					kd.insert(MyMath.latlon2XYZ(n), n);
					kdSize++;
				} catch (KeySizeException e) {
					e.printStackTrace();
				} catch (KeyDuplicateException e) {
					System.out.println("KeyDuplication at " + n);
					System.out.println("  Please fix in OSM: " + n.toUrl());					
				}				
			}
		}
		long time = (System.currentTimeMillis() - startTime);
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("Found " + kdSize + " placenames in " + time / 1000 + " seconds");
		}
		return kd;
	}
	private KDTree getNearByWays(OsmParser parser) {
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("Creating nearBy way candidates");
		}
		long startTime = System.currentTimeMillis();
		KDTree kd = new KDTree(3);
		//double [] latlonKey = new double[2]; 
		for (Way w : parser.getWays()) {
			if (w.isHighway() /*&& w.getIsIn() == null */) {
				if (w.getName() == null || w.getName().trim().length() == 0) {
					continue;
				}

				Node n = w.getMidPoint();
				if (n == null) {
					continue;
				}
				try {
					// FIXME would be better to make a real data
					// type for way proximity & name calculation instead of abusing nodes and node tags

					// replace node's id with way id so
					// we get the right id to add as tag
					// causes problems
					Node n2 = new Node();
					n2.id = w.id;
					// transfer coords to node
					n2.lat = n.lat;
					n2.lon = n.lon;
					// System.out.println("way name: " + w.getName());
					//n.setAttribute("name", w.getName());
					// is this needed? probably not
					//n2.cloneTags(w);
					//System.out.println("way " +  w.getName() + " converted into node: " + n2 + " for way matching");
					if (!n2.containsKey("__wayname")) {
						n2.setAttribute("__wayname", w.getName());
					}
					// FIXME: should find out about and eliminate duplicate warnings
					kd.insert(MyMath.latlon2XYZ(n2), n2);
					kdWaysSize++;
				} catch (KeySizeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KeyDuplicateException e) {
					// can be safely ignored
					//System.out.println("Warning: KeyDuplication bug at housenumber handling " + n.toUrl());
				}				
			}
		}
		long time = (System.currentTimeMillis() - startTime);
		if (Configuration.getConfiguration().verbose >= 0) {
			System.out.println("Found " + kdWaysSize + " waynames in " + time / 1000 + " seconds");
		}
		return kd;
	}
	// return distance from node to way
	private static long distanceToWay(Node node, Way w, long dist) {
		List<Node> nl = w.getNodes();
		for (int i = 0; i < nl.size() - 1 ; i++ ) {
			Node closestPoint = closestPointOnLine(nl.get(i), nl.get(i+1), node);
			long distNew = MyMath.dist(node, closestPoint);
			if (distNew < dist) {
				dist = distNew;
			}
		}
		return dist;
	}

	private static Node closestPointOnLine(Node node1, Node node2, Node offNode) {
		// avoid division by zero if node1 and node2 are at the same coordinates
		if (node1.lat == node2.lat && node1.lon == node2.lon) {
			return new Node(node1);
		}
		float uX = (float)Math.toRadians(node2.lat) - (float)Math.toRadians(node1.lat);
		float uY = (float)Math.toRadians(node2.lon) - (float)Math.toRadians(node1.lon);
		float  u = ( ((float)Math.toRadians(offNode.lat) - (float)Math.toRadians(node1.lat)) * uX 
			     + ((float)Math.toRadians(offNode.lon)- (float)Math.toRadians(node1.lon)) * uY) / (uX * uX + uY * uY);
		if (u > 1.0) {
			return new Node(node2);
		} else if (u <= 0.0) {
			return new Node(node1);
		} else {
			return new Node( (float)Math.toDegrees((node2.lat * u + node1.lat * (1.0 - u ))), (float) Math.toDegrees((node2.lon * u + node1.lon * (1.0-u))), 1);
		}
	}
       
}
