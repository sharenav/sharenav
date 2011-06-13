/**
 * This file is part of OSM2GpsMid
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation. 
 * See COPYING.
 *
 * Copyright (c) 2011 sk750
 */

package de.ueller.osmToGpsMid.area;

import java.util.ArrayList;

import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;
import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.OsmParser;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.Way;


public class SeaGenerator2 {
	public float minLat = Float.MAX_VALUE;
	public float minLon = Float.MAX_VALUE;
	public float maxLat = Float.MIN_VALUE;
	public float maxLon = Float.MIN_VALUE;

	
	public void generateSeaMultiPolygon(OsmParser parser) {
		
		// find minimum/maximum lat and lon for the midlet
		for (Node n: parser.getNodes()) {
			if (n.lat < minLat) {
				minLat = n.lat;
			}
			if (n.lat > maxLat) {
				maxLat = n.lat;
			}
			if (n.lon < minLon) {
				minLon = n.lon;
			}
			if (n.lon > maxLon) {
				maxLon = n.lon;
			}
		}
		
		// create a sea area covering the whole midlet
		Node nw = new Node(minLat, minLon, FakeIdGenerator.makeFakeId());
		Node ne = new Node(minLat, maxLon, FakeIdGenerator.makeFakeId());
		Node sw = new Node(maxLat, minLon, FakeIdGenerator.makeFakeId());
		Node se = new Node(maxLat, maxLon, FakeIdGenerator.makeFakeId());
		long seaId = FakeIdGenerator.makeFakeId();
		Way sea = new Way(seaId);
		sea.addNode(nw);
		sea.addNode(sw);
		sea.addNode(se);
		sea.addNode(ne);
		sea.addNode(nw);
		sea.setAttribute("natural", "water");
		sea.setAttribute("layer", "-2");

		// create a multipolygon relation containing water as outer role
		Member mOuter = new Member("way", sea.id, "outer");
		long multiId = FakeIdGenerator.makeFakeId();
		Relation seaRelation = new Relation(multiId);
		seaRelation.setAttribute("type", "multipolygon");
		seaRelation.add(mOuter);

		// remember coastlines and add them to landways
		String natural;
		Member mInner;
		ArrayList<Way> landWays = new ArrayList<Way>();
		for (Way w: parser.getWays()) {
			natural = w.getAttribute("natural");
			if (natural != null) {
				if ("coastline".equals(natural)) {
					System.out.println("Create land from coastline  " + w.toUrl());
					long landId = FakeIdGenerator.makeFakeId();
					Way wLand = new Way(landId, w);
					landWays.add(wLand);
				}
			}
		}
		
		// add landways to parser and to sea relation
		for (Way lw: landWays) {
			lw.setAttribute("natural", "land");
			lw.setAttribute("layer", "-1");
			lw.setAttribute("area", "yes");
			parser.addWay(lw);
			mInner = new Member("way", lw.id, "inner");
			seaRelation.add(mInner);			
		}

		// add sea relation
//		if (landWays.size() != 0 ) {
			parser.addWay(sea);			
			parser.addRelation(seaRelation);
//		}
	}

}
