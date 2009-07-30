/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007        Harald Mueller
 * Copyright (C) 2007, 2008  Kai Krueger
 */
package de.ueller.osmToGpsMid;

import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Way;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;


public class CalcNearBy {
	OxParser parser;
	private int kdSize = 0; // Hack around the fact that KD-tree doesn't tell us it's size

	public CalcNearBy(OxParser parser) {
		super();
		this.parser = parser;
		KDTree nearByElements = getNearByElements();
		if (kdSize > 0) {
			calcCityNearBy(parser, nearByElements);
			calcWayIsIn(parser, nearByElements);
		}
	}

	/**
	 * @param parser2
	 * @param nearByElements
	 */
	private void calcWayIsIn(OxParser parser2, KDTree nearByElements) {		
		for (Way w : parser.getWays()) {
			if (w.isHighway() /*&& w.getIsIn() == null */){
				Node thisNode=w.getMidPoint();
				Node nearestPlace = null;				
				try {					
					nearestPlace = (Node) nearByElements.nearest(MyMath.latlon2XYZ(thisNode));					

					if (!(MyMath.dist(thisNode, nearestPlace) < Constants.MAX_DIST_CITY[nearestPlace.getType(null)])){					
						long maxDistanceTested = MyMath.dist(thisNode, nearestPlace);
						int retrieveN = 5;
						if (retrieveN > kdSize) retrieveN = kdSize;
						nearestPlace=null;
						while (maxDistanceTested < Constants.MAX_DIST_CITY[Constants.NODE_PLACE_CITY]) {							
							Object [] nearPlaces = nearByElements.nearest(MyMath.latlon2XYZ(thisNode),retrieveN);
							long dist = 0;
							for (Object o : nearPlaces) {
								Node other = (Node) o;								
								dist = MyMath.dist(thisNode, other);
								//As the list returned by the kd-tree is sorted by distance,
								//we can stop at the first found 
								if (dist < Constants.MAX_DIST_CITY[other.getType(null)]){								
									nearestPlace=other;									
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
							if (retrieveN > kdSize) retrieveN = kdSize;
						}
					}
				} catch (KeySizeException e) {
					// Something must have gone horribly wrong here,
					// This should never happen.					
					e.printStackTrace();
					return;
				}
				if (nearestPlace != null){
					w.setAttribute("is_in", nearestPlace.getName());					
					w.nearBy=nearestPlace;
				}				
			}
		}		
	}

	private void calcCityNearBy(OxParser parser, KDTree nearByElements) {
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

					if (kdSize < nneighbours)
						nneighbours = kdSize;
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
					if (nneighbours == kdSize)
						break;
					nneighbours *= 5;
				}
				if (nearestPlace != null){
					n.nearBy=nearestPlace;
					//n.nearByDist=nearesDist;					
//					System.out.println(n + " near " + n.nearBy);
				}
			}
		}
	}

	private KDTree getNearByElements() {
		
		System.out.println("create nearBy candidates");
		KDTree kd = new KDTree(3);
		//double [] latlonKey = new double[2]; 
		for (Node n : parser.getNodes()) {
			if (n.isPlace()) {
				//latlonKey[0] = n.lat;
				//latlonKey[1] = n.lon;
				if (n.getName() == null || n.getName().trim().length() == 0) {
					System.out.println("STRANGE: place without name, skipping: " + n);
					continue;
				}
				try {
					kd.insert(MyMath.latlon2XYZ(n), n);
					kdSize++;
				} catch (KeySizeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (KeyDuplicateException e) {
					System.out.println("KeyDuplication at " + n);
					
				}				
			}
		}
		System.out.println("Found " + kdSize + " placenames");
		return kd;
	}
}
