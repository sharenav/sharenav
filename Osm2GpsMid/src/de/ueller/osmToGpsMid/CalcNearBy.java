/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.util.ArrayList;

import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Way;


public class CalcNearBy {
	OxParser parser;

	public CalcNearBy(OxParser parser) {
		super();
		this.parser = parser;
		ArrayList<Node> nearByElements = getNearByElements();
		calcCityNearBy(parser, nearByElements);
		calcWayIsIn(parser, nearByElements);
	}

	/**
	 * @param parser2
	 * @param nearByElements
	 */
	private void calcWayIsIn(OxParser parser2, ArrayList<Node> nearByElements) {
		for (Way w : parser.ways) {
			if (w.isHighway() && w.getIsIn() == null){
				Node thisNode=w.getMidPoint();
				Node nearestPlace = null;
				long nearesDist = Long.MAX_VALUE;
				for (Node other : nearByElements) {
					long dist=MyMath.dist(thisNode, other);
					if (dist < nearesDist & dist < Constants.MAX_DIST_CITY[other.getType(null)]){
						nearesDist=dist;
						nearestPlace=other;
					}
				}
				if (nearestPlace != null && nearestPlace.getName() != null){
					w.tags.put("is_in", nearestPlace.getName());
					System.out.println("set is_in for " + w.getName() + " to " + nearestPlace.getName());
				}
			}
		}
		
	}

	/**
	 * @param parser
	 * @param nearByElements
	 */
	private void calcCityNearBy(OxParser parser, ArrayList<Node> nearByElements) {
		for (Node n : parser.nodes.values()) {
			String place = n.getPlace();
			if (place != null) {
				Node nearestPlace = null;
				long nearesDist = Long.MAX_VALUE;
				for (Node other : nearByElements) {
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
					System.out.println(n + " near " + n.nearBy);
				}
			}
		}
	}

	private ArrayList<Node> getNearByElements() {
		System.out.println("create nearBy candidates");
		ArrayList<Node> ret = new ArrayList<Node>();
		for (Node n : parser.nodes.values()) {
			if (n.getNameType() == Constants.NAME_CITY)
				ret.add(n);
		}
		return ret;
	}
}
