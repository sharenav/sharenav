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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.ueller.osmToGpsMid.area.Area;
import de.ueller.osmToGpsMid.area.Outline;
import de.ueller.osmToGpsMid.area.Triangle;
import de.ueller.osmToGpsMid.area.Vertex;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Way;

/**
 * @author hmueller
 *
 */
public class Relations {

	private final OsmParser parser;
	private final Configuration conf;
	int triangles = 0;
	int areas = 0;
	

	public Relations(OsmParser parser, Configuration conf) {
		this.parser = parser;
		this.conf = conf;
		processRelations();
		parser.resize();
		System.out.println("Remaining after relation processing (triangulation etc.):");
		System.out.println("  Nodes: " + parser.getNodes().size());
		System.out.println("  Ways: " + parser.getWays().size());
		System.out.println("  Relations: " + parser.getRelations().size());
		System.out.println("  Areas: " + areas);
		System.out.println("  Triangles: " + triangles);
	}


	/**
	 * 
	 */
	private void processRelations() {
		int relationCount = 0;
		HashMap<Long, Way> wayHashMap = parser.getWayHashMap();
		Map<Long, Node> nodeHashMap = parser.getNodeHashMap();
		ArrayList<Way> removeWays = new ArrayList<Way>();
		Way firstWay = null;
		Iterator<Relation> i = parser.getRelations().iterator();
		rel: while (i.hasNext()) {
			firstWay = null;
			Relation r = i.next();
//			System.out.println("check relation " + r + "is valid()=" + r.isValid() );
			if (r.isValid()) {
//				if (relationCount % 10 == 0) {
//					System.out.println("info: handled " + relationCount + " relations");
//					System.out.println("info: currently handling relation type " + r.getAttribute("type"));
//				}
				relationCount++;
				String type = r.getAttribute("type");
				if (type != null && conf.getRelationExpansions().get(type) != null && r.getTags().size() > 1 && 
				    conf.getRelationExpansions().get(type).equals(r.getAttribute(type))) {
					r.getTags().remove("type");
					for (Long ref : r.getWayIds()) {
						Way w = wayHashMap.get(ref);
						w.cloneTags(r);
						//System.out.println("way: " + w);
					}
				} else if (conf.useHouseNumbers && ("associatedStreet".equals(r.getAttribute("type")) ||
											 "street".equals(r.getAttribute("type")))) {
					//System.out.println("Handling housenumber relation " + r.toUrl());
					for (Long ref : r.getWayIds(Member.ROLE_STREET)) {
						Way w = wayHashMap.get(ref);
						for (Long noderef : r.getNodeIds(Member.ROLE_HOUSE)) {
							Node n = nodeHashMap.get(noderef);
							w.houseNumberAdd(n);
							// add tag to point from
							//  System.out.println("setting node " + n + " __wayid to " + w.id);
							n.setAttribute("__wayid", w.id.toString());
							//System.out.println("Housenumber relation " + r.toUrl() + " - added node " + );
						}
					}
					i.remove();
				} else if ("multipolygon".equals(r.getAttribute("type"))) {
					if (r.getAttribute("admin_level") != null){
						continue;
					}
					if ("administrative".equalsIgnoreCase(r.getAttribute("boundary"))) {
						continue;
					}
//				System.out.println("Starting to handle multipolygon relation");
//				System.out.println("  see http://www.openstreetmap.org/browse/relation/" + r.id);

					if (r.getWayIds(Member.ROLE_OUTER).size() == 0) {
						System.out.println("Relation has no outer member");
						System.out.println("  see " + r.toUrl() + " I'll ignore this relation");
						continue;
					}
//				System.out.println("outer size: " + r.getWayIds(Member.ROLE_OUTER).size());
//				System.out.println("Triangulate relation " + r.id);
					Area a = new Area();
//				if (r.id == 405925 ) {
//					a.debug=true;
//				}
					for (Long ref : r.getWayIds(Member.ROLE_OUTER)) {
//					if (ref == 39123631) {
//						a.debug = true;
//					}
						Way w = wayHashMap.get(ref);
						if (w.getAttribute("admin_level") != null) {
							continue rel;
						}
						if ("administrative".equalsIgnoreCase(w.getAttribute("boundary"))) {
							continue rel;
						}
						// FIXME can be removed when proper coastline support exists
						if ("coastline".equalsIgnoreCase(w.getAttribute("natural"))) {
							continue rel;
						}

//					System.out.println("Handling outer way http://www.openstreetmap.org/browse/way/" + ref);
						if (w == null) {
							System.out.println("Way " + w.toUrl() + " was not found but referred as outline in ");
							System.out.println("  relation " + r.toUrl() + " I'll ignore this relation");
							continue rel;
						}
						Outline no = createOutline(w);
						if (no != null) {
//						System.out.println("Adding way " + w.toUrl() + " as OUTER");
							a.addOutline(no);
							if (firstWay == null) {
								if (w.triangles != null) {
									System.out.println("Strange, this outline is already triangulated! May be a duplicate, I'll ignore it");
									System.out.println("  way " + w.toUrl());
									System.out.println("  relation " + r.toUrl());
									continue rel;
								}
								firstWay = w;
							} else {
								removeWays.add(w);
							}
						}
					}
					for (Long ref : r.getWayIds(Member.ROLE_INNER)) {
						Way w = wayHashMap.get(ref);
						if (w == null) {
							System.out.println("Way " + w.toUrl() + " was not found but referred as INNER in ");
							System.out.println("  relation "+ r.toUrl() + " I'll ignore this relation");
							continue rel;
						}
//					System.out.println("Adding way " + w.toUrl() + " as INNER");
						Outline no = createOutline(w);
						if (no != null) {
							a.addHole(no);
							if (w.getType(conf) < 1) {
								removeWays.add(w);
							}
						}
					}
					try {
						List<Triangle> areaTriangles = a.triangulate();
						firstWay.triangles = areaTriangles;
						firstWay.recreatePath();
						r.getTags().remove("type");
						if (r.getTags().size() > 0) {
							firstWay.replaceTags(r);
						}
						triangles += areaTriangles.size();
//					System.out.println("areaTriangles.size for relation " + r.toUrl() + " :" + areaTriangles.size());
						areas += 1;
					} catch (Exception e) {
						System.out.println("Something went wrong when trying to triangulate relation ");
						System.out.println("  " + r.toUrl() + " I'll attempt to ignore this relation");
						e.printStackTrace();
					}
				
					i.remove();
				}
			}
		}
		
		for (Way w : removeWays) {
			if (!w.isAccessForAnyRouting()) {
				parser.removeWay(w);
			}
			else {
				System.out.println("  info: multipolygon member " + w.toUrl() + " not removed because it is routable");
			}
		}
		//parser.resize();
	}

	/**
	 * @param wayHashMap
	 * @param r
	 */
	private Outline createOutline(Way w) {

		Outline o = null;
		if (w != null) {
			Node last = null;
			o = new Outline();
			o.setWayId(w.id);
			for (Node n : w.getNodes()) {
				if (last != n) {
					o.append(new Vertex(n, o));
				}
				last = n;
			}
		}
		return o;
	}

}
