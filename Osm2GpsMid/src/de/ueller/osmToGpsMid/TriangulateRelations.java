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
public class TriangulateRelations {

	private final OxParser parser;
	private final Configuration conf;
	int triangles=0;
	int areas=0;
	

	public TriangulateRelations(OxParser parser, Configuration conf) {
		this.parser = parser;
		this.conf = conf;
		convertAreasToTriangles();
		parser.resize();
		System.out.println("Remaining after cleanup:");
		System.out.println("  Nodes: " + parser.getNodes().size());
		System.out.println("  Ways: " + parser.getWays().size());
		System.out.println("  Relations: " + parser.getRelations().size());
		System.out.println("  Areas: " + areas);
		System.out.println("  Triangles: " + triangles);
	}


	/**
	 * 
	 */
	private void convertAreasToTriangles() {
		HashMap<Long, Way> wayHashMap = parser.getWayHashMap();
		ArrayList<Way> removeWays = new ArrayList<Way>();
		Way firstWay = null;
		Iterator<Relation> i = parser.getRelations().iterator();
		rel: while (i.hasNext()) {
			firstWay = null;
			Relation r = i.next();
			if (r.isValid() && "multipolygon".equals(r.getAttribute("type"))) {
				if (r.getAttribute("admin_level") != null){
					continue;
				}
				System.err.println("Triangulate relation " + r.id);
				Area a = new Area();
//				if (r.id == 544315 ){
//					a.debug=true;
//				}
				for (Long ref : r.getWayIds(Member.ROLE_OUTER)) {
//					if (ref == 39123631) {
//						a.debug = true;
//					}
					Way w = wayHashMap.get(ref);
					if (w == null) {
						System.err.println("Way http://www.openstreetmap.org/?way=" + ref + " was not found but referred  as outline in ");
						System.err.println("    relation http://www.openstreetmap.org/?relation=" + r.id + "I'll ignore this relation");
						continue rel;
					}
					Outline no = createOutline(w);
					if (no != null) {
						a.addOutline(no);
						if (firstWay == null) {
							if (w.triangles != null){
								System.err.println("strange this outline is already triangulated ! maybe dublicate. I will ignore it");
								System.err.println("Way http://www.openstreetmap.org/?way=" + ref);
								System.err.println("please see http://www.openstreetmap.org/?relation=" + r.id);
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
						System.err.println("Way http://www.openstreetmap.org/?way=" + ref + " was not found but referred  as INNER in ");
						System.err.println("    relation http://www.openstreetmap.org/?relation=" + r.id + "I'll ignore this relation");
						continue rel;
					}
					Outline no = createOutline(w);
					if (no != null) {
						a.addHole(no);
						if (w.getType(conf) < 1) {
							removeWays.add(w);
						}
					}
				}
				List<Triangle> areaTriangles = a.triangulate();
				firstWay.triangles = areaTriangles;
				firstWay.recreatePath();
				triangles += areaTriangles.size();
				areas += 1;
				i.remove();
			}
		}
		
		for (Way w : removeWays) {
			parser.removeWay(w);
		}
		parser.resize();
	}

	/**
	 * @param wayHashMap
	 * @param r
	 */
	private Outline createOutline(Way w) {
			
			Outline o=null;
			if (w!=null){
				Node last=null;
				o = new Outline();
				o.setWayId(w.id);
				for (Node n:w.getNodes()){
					if (last != n){
					o.append(new Vertex(n,o));
					}
					last=n;
				}
			}			
		return o;
	}


}
