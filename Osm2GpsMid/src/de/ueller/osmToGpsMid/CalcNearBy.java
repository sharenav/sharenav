/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
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
		calcCityNearBy(parser, nearByElements);
		calcWayIsIn(parser, nearByElements);
	}

	/**
	 * @param parser2
	 * @param nearByElements
	 */
	private void calcWayIsIn(OxParser parser2, KDTree nearByElements) {		
		for (Way w : parser.ways) {
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
								if (dist < Constants.MAX_DIST_CITY[other.getType(null)]){								
									nearestPlace=other;
								}							
							}
							if (nearestPlace != null) {
								//found a suitable Place, leaving loop
								break;
							}							
							maxDistanceTested = dist;
							retrieveN = retrieveN * 2;
							if (retrieveN > kdSize) retrieveN = kdSize;
						}					
					}
				} catch (KeySizeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (nearestPlace != null && nearestPlace.getName() != null){
					w.tags.put("is_in", nearestPlace.getName());
					//					System.out.println("set is_in for " + w.getName() + " to " + nearestPlace.getName());
					w.nearBy=nearestPlace;
				}
			}
		}
		
	}

	private void calcCityNearBy(OxParser parser, KDTree nearByElements) {
		//double [] latlonKey = new double[2];
		for (Node n : parser.nodes.values()) {
			String place = n.getPlace();
			if (place != null) {
				Node nearestPlace = null;
				long nearesDist = Long.MAX_VALUE;
				//latlonKey[0] = n.lat;
				//latlonKey[1] = n.lon;
				Object[] nearNodes = null;
				int nneighbours = 10;
				if (kdSize < nneighbours) nneighbours = kdSize;
				try {
					nearNodes = nearByElements.nearest(MyMath.latlon2XYZ(n), nneighbours);										
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
					if (n.getType(null) > other.getType(null)){
						long dist=MyMath.dist(n, other);
						if (dist < nearesDist){
							nearesDist=dist;
							nearestPlace=other;
						}
					}
				}
				if (nearestPlace != null){
					n.nearBy=nearestPlace;
					n.nearByDist=nearesDist;
//					System.out.println(n + " near " + n.nearBy);
				}
			}
		}
	}

	private KDTree getNearByElements() {
		
		System.out.println("create nearBy candidates");
		KDTree kd = new KDTree(3);
		//double [] latlonKey = new double[2]; 
		for (Node n : parser.nodes.values()) {
			if (n.getNameType() == Constants.NAME_CITY) {
				//latlonKey[0] = n.lat;
				//latlonKey[1] = n.lon;
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
