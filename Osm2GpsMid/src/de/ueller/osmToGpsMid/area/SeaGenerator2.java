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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;

import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;
import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.MyMath;
import de.ueller.osmToGpsMid.OsmParser;
import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.Way;


public class SeaGenerator2 {
	public float minLat = Float.MAX_VALUE;
	public float minLon = Float.MAX_VALUE;
	public float maxLat = Float.MIN_VALUE;
	public float maxLon = Float.MIN_VALUE;

	public float minMapLat = Float.MAX_VALUE;
	public float minMapLon = Float.MAX_VALUE;
	public float maxMapLat = Float.MIN_VALUE;
	public float maxMapLon = Float.MIN_VALUE;

	private static boolean generateSea = true;
	private static boolean generateSeaUsingMP = false;
	private static boolean allowSeaSectors = false;
	private static boolean extendSeaSectors = true;
	private static int maxCoastlineGap = 100;

	private static Configuration configuration;

	private boolean foundCoast = false;
	
	private static final String[] landTag = { "natural", "generated-land" };

	Bounds seaBounds;
	Bounds mapBounds;

	/** 
	 * Set various options for generating sea areas
	 * @param aGenerateSea Generate sea areas at all
	 * @param aGenerateSeaUsingMP Use multi polygons for the sea areas
	 * @param aAllowSeaSectors Allow to create new ways for the sea area, 
	 * 		this excludes extendSeaSectors = true.
	 * @param aExtendSeaSectors Allow to extend ways to form the sea area
	 * @param aMaxCoastlineGap Gaps below this length (in meters) in coast line will be filled.
	 */
	public static void setOptions(Configuration conf, boolean aGenerateSea, 
			boolean aGenerateSeaUsingMP, boolean aAllowSeaSectors, 
			boolean aExtendSeaSectors, int aMaxCoastlineGap) {
		configuration = conf;
		generateSea = aGenerateSea;
		generateSeaUsingMP = aGenerateSeaUsingMP;
		allowSeaSectors = aAllowSeaSectors;
		extendSeaSectors = aExtendSeaSectors;
		maxCoastlineGap = aMaxCoastlineGap;
	}
	
	public void generateSeaMultiPolygon(OsmParser parser) {
		
		// find minimum/maximum lat and lon for the midlet
		for (Node n: parser.getNodes()) {
			if (n.lat <= minLat) {
				minLat = n.lat - 0.0002f;
				minMapLat = n.lat;
			}
			if (n.lat >= maxLat) {
				maxLat = n.lat + 0.0002f;
				maxMapLat = n.lat;
			}
			if (n.lon <= minLon) {
				minLon = n.lon - 0.0002f;
				minMapLon = n.lon;
			}
			if (n.lon >= maxLon) {
				maxLon = n.lon + 0.0002f;
				maxMapLon = n.lon;
			}
		}
		
		seaBounds = new Bounds();
		seaBounds.minLat = minLat;
		seaBounds.minLon = minLon;
		seaBounds.maxLat = maxLat;
		seaBounds.maxLon = maxLon;

		mapBounds = new Bounds();
		mapBounds.minLat = minMapLat;
		mapBounds.minLon = minMapLon;
		mapBounds.maxLat = maxMapLat;
		mapBounds.maxLon = maxMapLon;

		System.out.println("seaBounds: " + seaBounds);
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

		long multiId = FakeIdGenerator.makeFakeId();
		Relation seaRelation = new Relation(multiId);
		seaRelation.setAttribute("type", "multipolygon");
		seaRelation.setAttribute("natural", "sea");

		// remember coastlines and add them to landways
		String natural;
		Member mInner;
		ArrayList<Way> landWays = new ArrayList<Way>();
		for (Way w: parser.getWays()) {
			natural = w.getAttribute("natural");
			if (natural != null) {
				if ("coastline".equals(natural)) {
					System.out.println("Create land from coastline  " + w.toUrl());
					// for closed ways, save memory and do not create new ways
					if (w.isClosed()) {
						landWays.add(w);
					} else {
						long landId = FakeIdGenerator.makeFakeId();
						Way wLand = new Way(landId, w);
						landWays.add(wLand);
					}
				}
			}
		}
		
		boolean generateSeaBackground = false;

		if (landWays.size() > 0 ) {
			foundCoast = true;
		}

		// handle islands (closed shoreline components) first (they're easy)
		// add closed landways (islands, islets) to parser and to sea relation
		// these are inner members in the sea relation,
		// in other words holes in the sea 
		Iterator<Way> it = landWays.iterator();
		while (it.hasNext()) {
			Way w = it.next();
			if (w.isClosed()) {
				System.out.println("adding island " + w);
				parser.addWay(w);
				it.remove();
				mInner = new Member("way", w.id, "inner");
				seaRelation.add(mInner);			
			}
		}

		// while relation handling code does concatenate ways, we need
		// this to see where the start and end for coastlines are
		// so we can decide how to connect partial coastlines to e.g. map borders
		concatenateWays(landWays, seaBounds, parser);
		
		// there may be more islands now

		it = landWays.iterator();
		while (it.hasNext()) {
			Way w = it.next();
			if (w.isClosed()) {
				System.out.println("after concatenation: adding island " + w);
				parser.addWay(w);
				it.remove();
				mInner = new Member("way", w.id, "inner");
				seaRelation.add(mInner);			
			}
		}

		// handle non-closed coastlines
		// * those which intersect the map boundary
		// * possible map bugs
		SortedMap<EdgeHit, Way> hitMap = new TreeMap<EdgeHit, Way>();

		Way seaSector = null;

		for (Way w : landWays) {
			List<Node> points = w.getNodes();
			Node pStart = points.get(0);
			Node pEnd = points.get(points.size()-1);

			EdgeHit hStart = getEdgeHit(mapBounds, pStart);
			EdgeHit hEnd = getEdgeHit(mapBounds, pEnd);
			if (hStart == null || hEnd == null) {
				String msg = String.format(
						"Non-closed coastline segment does not hit bounding box: start %s end %s\n" +
						"  See %s and %s\n",
						pStart.toString(), pEnd.toString(),
						pStart.toUrl(), pEnd.toUrl());
				System.out.println(msg);
				/*
				 * This problem occurs usually when the shoreline is cut by osmosis (e.g. country-extracts from geofabrik)
				 * There are two possibilities to solve this problem:
				 * 1. Close the way and treat it as an island. This is sometimes the best solution (Germany: Usedom at the
				 *    border to Poland)
				 * 2. Create a "sea sector" only for this shoreline segment. This may also be the best solution
				 *    (see German border to the Netherlands where the shoreline continues in the Netherlands)
				 * The first choice may lead to "flooded" areas, the second may lead to "triangles".
				 *
				 * Usually, the first choice is appropriate if the segment is "nearly" closed.
				 */
				double length = 0;
				Node p0 = pStart;
				for (Node p1 : points.subList(1, points.size()-1)) {
					length += MyMath.dist(p0, p1);
					p0 = p1;
				}
				System.out.println("dist from coastline start to end: " + MyMath.dist(pStart, pEnd));
				boolean nearlyClosed = (MyMath.dist(pStart, pEnd) < 0.1 * length);

				if (nearlyClosed) {
					System.out.println("handling nearlyClosed coastline: " + w);
					// close the way
					points.add(pStart);
					if (generateSeaUsingMP) {
						mInner = new Member("way", w.id, "inner");
						seaRelation.add(mInner);
						// polish.api.bigstyles
						short t = w.getType(configuration);
						parser.addWay(w);
					} else {
						if(!FakeIdGenerator.isFakeId(w.getId())) {
							Way w1 = new Way(FakeIdGenerator.makeFakeId());
							w1.getNodes().addAll(w.getNodes());
							// only copy the name tags
							for(String tag : w.getTags()) {
								if(tag.equals("name") || tag.endsWith(":name")) {
									w1.setAttribute(tag, w.getAttribute(tag));
								}
							}
							w = w1;
						}
						w.setAttribute(landTag[0], landTag[1]);
						w.setAttribute("area", "yes");
						/* This line is not unnecessary as it triggers the calculation of the way's type
						 * (which is why making this method sound like it's a simple getter is 
						 * a bloody bad idea).
						 */
						// polish.api.bigstyles
						short t = w.getType(configuration);
						parser.addWay(w);
					}
				}
				else if(allowSeaSectors && false) {  // this part appears to cause trouble
					System.out.println("handling allowSeaSectors coastline: " + w);
					seaId = FakeIdGenerator.makeFakeId();
					seaSector = new Way(seaId);
					seaSector.getNodes().addAll(points);
					//seaSector.addNode(new Node(pEnd.getLat(), pStart.getLon(), 
					//						FakeIdGenerator.makeFakeId()));
					EdgeHit startEdgeHit = getNextEdgeHit(mapBounds, pStart);
					EdgeHit endEdgeHit = getNextEdgeHit(mapBounds, pEnd);
					int startedge = startEdgeHit.edge;
					int endedge = endEdgeHit.edge;

					System.out.println("startedge: " + startedge + " endedge: " + endedge);

					if (false || false) {
						Node p;
						//System.out.println("edge: " + edge + " val: " + val);
						if (endedge < startedge) {
							endedge += 4;
						}

						for (int i=endedge; i > startedge; i--) {
							int edge = i % 4;
							float val = 0.0f;
							System.out.println("edge: " + edge + " val: " + val);
							EdgeHit corner = new EdgeHit(edge, val);
							p = corner.getPoint(mapBounds);
							//log.debug("way: ", corner, p);
							System.out.println("way: corner: " + corner + " p: " + p);

							seaSector.addNodeIfNotEqualToLastNode(p);
						}
					}
					seaSector.addNode(pStart);
					//seaSector.setAttribute("natural", "sea");
					//seaSector.setAttribute("area", "yes");
					/* This line is not unnecessary as it triggers the calculation of the way's type
					 * (which is why making this method sound like it's a simple getter is 
					 * a bloody bad idea).
					 */
					// polish.api.bigstyles
					short t = sea.getType(configuration);
					if (generateSeaUsingMP) {
						parser.addWay(seaSector);
						mInner = new Member("way", seaSector.id, "inner");
						seaRelation.add(mInner);
						System.out.println("Added inner to sea relation: " + seaRelation.toString());
					}
					generateSeaBackground = false;
				}
			} else {
				//log.debug("hits: ", hStart, hEnd);
				hitMap.put(hStart, w);
				hitMap.put(hEnd, null);
			}
		}

		// now construct the outer sea polygon from the edge hits and 
		// map boundaries
		NavigableSet<EdgeHit> hits = (NavigableSet<EdgeHit>) hitMap.keySet();
		boolean shorelineReachesBoundary = false;
		while (!hits.isEmpty()) {
			long id = FakeIdGenerator.makeFakeId();
			Way w = new Way(id);
			parser.addWay(w);

			EdgeHit hit =  hits.first();
			EdgeHit hFirst = hit;
			do {
				Way segment = hitMap.get(hit);
				System.out.println("current hit: " + hit);
				EdgeHit hNext;
				if (segment != null) {
					// could do better with adding segments to
					// relation
					// add the segment and get the "ending hit"
					System.out.println("adding sgement: " + segment);
					for(Node p : segment.getNodes()) {
						w.addNodeIfNotEqualToLastNode(p);
					}
					hNext = getEdgeHit(mapBounds, segment.getNodes().get(segment.getNodes().size()-1));
				} else { // segment == null
					w.addNodeIfNotEqualToLastNode(hit.getPoint(mapBounds));
					hNext = hits.higher(hit);
					if (hNext == null) {
						hNext = hFirst;
					}

					Node p;
					if (hit.compareTo(hNext) < 0) {
						//log.info("joining: ", hit, hNext);
						for (int i=hit.edge; i<hNext.edge; i++) {
							EdgeHit corner = new EdgeHit(i, 1.0);
							p = corner.getPoint(mapBounds);
							//log.debug("way: ", corner, p);
							w.addNodeIfNotEqualToLastNode(p);
						}
					}
					else if (hit.compareTo(hNext) > 0) {
						System.out.println("joining: " + hit + " hNext: " + hNext);
						int hNextEdge = hNext.edge;
						if (hit.edge > hNext.edge) {
							hNextEdge += 4;
						}

						for (int i=hit.edge; i < hNextEdge; i++) {
							EdgeHit corner = new EdgeHit(i % 4, 1.0);
							p = corner.getPoint(mapBounds);
							//log.debug("way: ", corner, p);
							w.addNodeIfNotEqualToLastNode(p);
						}
					}
					w.addNodeIfNotEqualToLastNode(hNext.getPoint(mapBounds));
				}
				hits.remove(hit);
				hit = hNext;
			} while (!hits.isEmpty() && !hit.equals(hFirst));

			if (!w.isClosed()) {
				w.getNodes().add(w.getNodes().get(0));
			}
			parser.addWay(w);
			Member mOuter = new Member("way", w.id, "inner");
			seaRelation.add(mOuter);
			System.out.println("Added outer member to sea relation: " + seaRelation.toString());
			//System.out.println("adding non-island landmass, hits.size()=" + hits.size());
			//islands.add(w);
			shorelineReachesBoundary = true;
		}

		// add sea relation
		if (foundCoast) {
			if (generateSeaUsingMP) {
				// create a multipolygon relation containing water as outer role
				mInner = new Member("way", sea.id, "outer");
				seaRelation.add(mInner);
				parser.addWay(sea);			
				parser.addRelation(seaRelation);
				System.out.println("Adding sea relation: " + seaRelation.toString());
			} else {
				System.out.println("ERROR: SeaGenerator: can only create sea properly as relations");
			}
		} else {
			System.out.println("SeaGenerator: didn't find any coastline ways, assuming map is land");
		}
	
		System.out.println(seaRelation.toString());
	}
	/**
	 * Specifies where an edge of the bounding box is hit.
	 */
	private static class EdgeHit implements Comparable<EdgeHit>
	{
		private final int edge;
		private final double t;

		EdgeHit(int edge, double t) {
			this.edge = edge;
			this.t = t;
		}

		public int compareTo(EdgeHit o) {
			if (edge < o.edge) {
				return -1;
			} else if (edge > o.edge) {
				return +1;
			} else if (t > o.t) {
				return +1;
			} else if (t < o.t) {
				return -1;
			} else {
				return 0;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof EdgeHit) {
				EdgeHit h = (EdgeHit) o;
				return (h.edge == edge && Double.compare(h.t, t) == 0);
			} else {
				return false;
			}
		}

		private Node getPoint(Bounds a) {
			System.out.print("getPoint: " + a);
			switch (edge) {
			case 0:
				return new Node(a.getMinLat(), 
						(float)(a.getMinLon() + t * (a.getMaxLon() - a.getMinLon())),
						FakeIdGenerator.makeFakeId());

			case 1:
				return new Node((float)(a.getMinLat() + t * (a.getMaxLat() - a.getMinLat())), 
						a.getMaxLon(), FakeIdGenerator.makeFakeId());

			case 2:
				return new Node(a.getMaxLat(), 
						(float)(a.getMaxLon() - t * (a.getMaxLon() - a.getMinLon())),
						FakeIdGenerator.makeFakeId());

			case 3:
				return new Node((float)(a.getMaxLat() - t * (a.getMaxLat()-a.getMinLat())), 
						a.getMinLon(), FakeIdGenerator.makeFakeId());

			default:
				throw new IllegalArgumentException("edge has invalid value");
			}
		}

		@Override
		public String toString() {
			return "EdgeHit " + edge + "@" + t;
		}
	}

	private static EdgeHit getEdgeHit(Bounds a, Node p)
	{
		// mkgmap uses ints for lat/lon where a digit is 1 / (2^24) of a degree
		// (see Utils.toMapUnit()). So a tolerance of 10 is 0.000214576721191 degrees
		// or about 0.72 arc seconds.
		// this might need adjustment - was 0.0004, now is more to cover for ways
		// cut far from edge by Osm2GpsMid.
		// if we add a clipping polygon and clip ways, probably should be set back to 0.0004f.
		return getEdgeHit(a, p, 0.0006f);
	}

	private static EdgeHit getEdgeHit(Bounds a, Node p, float tolerance)
	{
		float lat = p.getLat();
		float lon = p.getLon();
		float minLat = a.getMinLat();
		float maxLat = a.getMaxLat();
		float minLong = a.getMinLon();
		float maxLong = a.getMaxLon();

		System.out.println(String.format("getEdgeHit: (%f %f) (%f %f %f %f)", 
				lat, lon, minLat, minLong, maxLat, maxLong));
		if (lat <= minLat+tolerance) {
			return new EdgeHit(0, ((double)(lon - minLong))/(maxLong-minLong));
		}
		else if (lon >= maxLong-tolerance) {
			return new EdgeHit(1, ((double)(lat - minLat))/(maxLat-minLat));
		}
		else if (lat >= maxLat-tolerance) {
			return new EdgeHit(2, ((double)(maxLong - lon))/(maxLong-minLong));
		}
		else if (lon <= minLong+tolerance) {
			return new EdgeHit(3, ((double)(maxLat - lat))/(maxLat-minLat));
		} else {
			return null;
		}
	}

	/*
	 * Find the nearest edge for supplied Node p.
	 */
	private static EdgeHit getNextEdgeHit(Bounds a, Node p)
	{
		float lat = p.getLat();
		float lon = p.getLon();
		float minLat = a.getMinLat();
		float maxLat = a.getMaxLat();
		float minLong = a.getMinLon();
		float maxLong = a.getMaxLon();

		System.out.println(String.format("getNextEdgeHit: (%f %f) (%f %f %f %f)", 
				lat, lon, minLat, minLong, maxLat, maxLong));
		// shortest distance to border (init with distance to southern border) 
		float min = lat - minLat;
		// number of edge as used in getEdgeHit. 
		// 0 = southern
		// 1 = eastern
		// 2 = northern
		// 3 = western edge of Area a
		int i = 0;
		// normalized position at border (0..1)
		double l = ((double)(lon - minLong))/(maxLong-minLong);
		// now compare distance to eastern border with already known distance
		if (maxLong - lon < min) {
			// update data if distance is shorter
			min = maxLong - lon;
			i = 1;
			l = ((double)(lat - minLat))/(maxLat-minLat);
		}
		// same for northern border
		if (maxLat - lat < min) {
			min = maxLat - lat;
			i = 2;
			l = ((double)(maxLong - lon))/(maxLong-minLong);
		}
		// same for western border
		if (lon - minLong < min) {
			i = 3;
			l = ((double)(maxLat - lat))/(maxLat-minLat);
		}
		// now created the EdgeHit for found values
		return new EdgeHit(i, l);
	} 
	private static void concatenateWays(List<Way> ways, Bounds bounds, OsmParser parser) {
		Map<Node, Way> beginMap = new HashMap<Node, Way>();

		for (Way w : ways) {
			if (!w.isClosed()) {
				List<Node> points = w.getNodes();
				beginMap.put(points.get(0), w);
			}
		}

		int merged = 1;
		while (merged > 0) {
			merged = 0;
			for (Way w1 : ways) {
				if (w1.isClosed()) {
					continue;
				}

				List<Node> points1 = w1.getNodes();
				Way w2 = beginMap.get(points1.get(points1.size()-1));
				if (w2 != null) {
					//log.info("merging: ", ways.size(), w1.getId(), w2.getId());
					List<Node> points2 = w2.getNodes();
					Way wm;
					if (FakeIdGenerator.isFakeId(w1.getId())) {
						wm = w1;
					} else {
						wm = new Way(FakeIdGenerator.makeFakeId());
						ways.remove(w1);
						ways.add(wm);
						wm.getNodes().addAll(points1);
						beginMap.put(points1.get(0), wm);
						// only copy the name tags
						for (String tag : w1.getTags()) {
							if (tag.equals("name") || tag.endsWith(":name")) {
								wm.setAttribute(tag, w1.getAttribute(tag));
							}
						}
					}
					wm.getNodes().addAll(points2);
					ways.remove(w2);
					beginMap.remove(points2.get(0));
					merged++;
					break;
				}
			}
		}

		// join up coastline segments whose end points are less than
		// maxCoastlineGap meters apart
		if(maxCoastlineGap > 0) {
			boolean changed = true;
			while(changed) {
				changed = false;
				for(Way w1 : ways) {
					if(w1.isClosed()) {
						continue;
					}
					List<Node> points1 = w1.getNodes();
					Node w1e = points1.get(points1.size() - 1);
					if(bounds.isOnBoundary(w1e)) {
						continue;
					}
					Way nearest = null;
					double smallestGap = Double.MAX_VALUE;
					for(Way w2 : ways) {
						if(w1 == w2 || w2.isClosed()) {
							continue;
						}
						List<Node> points2 = w2.getNodes();
						Node w2s = points2.get(0);
						if(bounds.isOnBoundary(w2s)) {
							continue;
						}
						double gap = MyMath.dist(w1e, w2s);
						if(gap < smallestGap) {
							nearest = w2;
							smallestGap = gap;
						}
					}
					if(nearest != null && smallestGap < maxCoastlineGap) {
						Node w2s = nearest.getNodes().get(0);
						System.out.println("SeaGenerator: Bridging " + (int)smallestGap + "m gap in coastline from " + 
								w1e.toUrl() + " to " + w2s.toUrl());
						Way wm;
						if (FakeIdGenerator.isFakeId(w1.getId())) {
							wm = w1;
						} else {
							wm = new Way(FakeIdGenerator.makeFakeId());
							ways.remove(w1);
							ways.add(wm);
							wm.getNodes().addAll(points1);
							wm.cloneTags(w1);
						}
						wm.getNodes().addAll(nearest.getNodes());
						ways.remove(nearest);
						// make a line that shows the filled gap
						Way w = new Way(FakeIdGenerator.makeFakeId());
						// TODO: So we need a style definition for this
						w.setAttribute("natural", "coastline-gap");
						w.addNode(w1e);
						w.addNode(w2s);
						parser.addWay(w);
						changed = true;
						break;
					}
				}
			}
		}
	}
}
