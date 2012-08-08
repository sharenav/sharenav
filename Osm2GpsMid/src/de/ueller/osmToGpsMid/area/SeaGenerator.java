/**
 * This file is part of OSM2GpsMid
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation. 
 * See COPYING.
 *
 * Copyright (c) 2010 Steve Ratcliffe, Markus Baeurle
 * Ported from the mkgmap project, class Osm5XmlHandler.
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

import uk.me.parabola.log.Logger;
import uk.me.parabola.mkgmap.general.LineClipper;
import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;
import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.MyMath;
import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.Tile;
import de.ueller.osmToGpsMid.model.Way;

/**
 * Static class which checks if there are coast lines in the tile which need to be
 * extended to a sea polygon. 
 */
public class SeaGenerator {
	private static final Logger log = Logger.getLogger(SeaGenerator.class);

	private static final String[] landTag = { "natural", "generated-land" };

	private static boolean generateSea = true;
	private static boolean generateSeaUsingMP = false;
	private static boolean allowSeaSectors = false;
	private static boolean extendSeaSectors = true;
	private static int maxCoastlineGap = 100;
	
	private static Configuration configuration;
	
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
	
	/**
	 * Checks for coast lines and creates one (or more) sea areas which extend(s) to
	 * the boundaries of this tile.
	 * 
	 * @param tile Tile to work on
	 */
	public static void generateSeaPolygon(Tile tile) {
		if (generateSea == false) {
			return;
		}
		
		// Bounds of tile are always initialized.
		Bounds seaBounds = tile.getBounds();
		
		// Pick out all coast line ways. This is from Osm5XmlHandler.endElement()
		List<Way> shoreline = new ArrayList<Way>();
		String natural;
		for (Way currentWay : tile.ways) {
			natural = currentWay.getAttribute("natural");
			if (natural != null) {
				if ("coastline".equals(natural)) {
					// FIXME should delete the tag
					//currentWay.deleteTag("natural");
					shoreline.add(currentWay);
				}
				else if (natural.contains(";")) {
					// cope with compound tag value
					String others = null;
					boolean foundCoastline = false;
					for (String n : natural.split(";")) {
						if ("coastline".equals(n.trim())) {
							foundCoastline = true;
						} else if (others == null) {
							others = n;
						} else {
							others += ";" + n;
						}
					}
					if (foundCoastline) {
						//currentWay.deleteTag("natural");
						if (others != null) {
							currentWay.setAttribute("natural", others);
						}
						shoreline.add(currentWay);
					}
				}
			}
		} // for

		log.info("generating sea, seaBounds=", seaBounds);
		float minLat = seaBounds.getMinLat();
		float maxLat = seaBounds.getMaxLat();
		float minLong = seaBounds.getMinLon();
		float maxLong = seaBounds.getMaxLon();
		Node nw = new Node(minLat, minLong, FakeIdGenerator.makeFakeId());
		Node ne = new Node(minLat, maxLong, FakeIdGenerator.makeFakeId());
		Node sw = new Node(maxLat, minLong, FakeIdGenerator.makeFakeId());
		Node se = new Node(maxLat, maxLong, FakeIdGenerator.makeFakeId());

		if(shoreline.isEmpty()) {
			// no sea required
			/* TODO: Cleanup after decision about background
			if(!generateSeaUsingMP) {
				// even though there is no sea, generate a land
				// polygon so that the tile's background colour will
				// match the land colour on the tiles that do contain
				// some sea
				long landId = FakeIdGenerator.makeFakeId();
				Way land = new Way(landId);
				land.addNode(nw);
				land.addNode(sw);
				land.addNode(se);
				land.addNode(ne);
				land.addNode(nw);
				land.setAttribute(landTag[0], landTag[1]);
				tile.addWay(land);
			}
			*/
			// nothing more to do
			return;
		}

		// Clip all shoreline segments
		// Moved this down because it only makes sense if there are coast lines at all. 
		// TODO: Do we need this in Osm2GpsMid or aren't the ways already clipped?
		// Apparently yes: otherwise there will be loads of messages like
		/*

===============================
2010/12/28 23:33:01 WARNING (SeaGenerator): Non-closed coastline segment does not hit bounding box: start id=248533560 (60.173336|24.822487) name=null end id=25197279 (60.173588|24.829788) name=null
  See http://www.openstreetmap.org/browse/node/248533560 and http://www.openstreetmap.org/browse/node/25197279
		  

		 */

		List<Way> toBeRemoved = new ArrayList<Way>();
		List<Way> toBeAdded = new ArrayList<Way>();
		for (Way segment : shoreline) {
			List<Node> points = segment.getNodes();
			List<ArrayList<Node>> clipped = LineClipper.clip(seaBounds, points);
			//List<List<Node>> clipped = null;
			if (clipped != null) {
				log.info("clipping " + segment);
				toBeRemoved.add(segment);
				for (ArrayList<Node> pts : clipped) {
					long id = FakeIdGenerator.makeFakeId();
					Way shore = new Way(id, pts);
					toBeAdded.add(shore);
				}
			}
		}
		log.info("clipping: adding " + toBeAdded.size() + ", removing " + toBeRemoved.size());
		shoreline.removeAll(toBeRemoved);
		shoreline.addAll(toBeAdded);

		long multiId = FakeIdGenerator.makeFakeId();
		Relation seaRelation = null;
		if(generateSeaUsingMP) {
			log.error("Sea multipolygons are not supported yet!");
/*			log.debug("Generate seabounds relation "+multiId);
			seaRelation = new Relation(multiId);
			seaRelation.setAttribute("type", "multipolygon");
			seaRelation.setAttribute("natural", "sea");
*/
		}

		List<Way> islands = new ArrayList<Way>();

		// handle islands (closes shoreline components) first (they're easy)
		Iterator<Way> it = shoreline.iterator();
		while (it.hasNext()) {
			Way w = it.next();
			if (w.isClosed()) {
				log.info("adding island " + w);
				islands.add(w);
				it.remove();
			}
		}
		concatenateWays(shoreline, seaBounds, tile);
		// there may be more islands now
		it = shoreline.iterator();
		while (it.hasNext()) {
			Way w = it.next();
			if (w.isClosed()) {
				log.debug("island after concatenating\n");
				islands.add(w);
				it.remove();
			}
		}

		boolean generateSeaBackground = true;

		// the remaining shoreline segments should intersect the boundary
		// find the intersection points and store them in a SortedMap
		SortedMap<EdgeHit, Way> hitMap = new TreeMap<EdgeHit, Way>();
		long seaId;
		Way sea;
		for (Way w : shoreline) {
			List<Node> points = w.getNodes();
			Node pStart = points.get(0);
			Node pEnd = points.get(points.size()-1);

			EdgeHit hStart = getEdgeHit(seaBounds, pStart);
			EdgeHit hEnd = getEdgeHit(seaBounds, pEnd);
			if (hStart == null || hEnd == null) {
				String msg = String.format(
						"Non-closed coastline segment does not hit bounding box: start %s end %s\n" +
						"  See %s and %s\n",
						pStart.toString(), pEnd.toString(),
						pStart.toUrl(), pEnd.toUrl());
				log.warn(msg);
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
				boolean nearlyClosed = (MyMath.dist(pStart, pEnd) < 0.1 * length);

				if (nearlyClosed) {
					// close the way
					points.add(pStart);
					if(generateSeaUsingMP) {
						// TODO seaRelation.addElement("inner", w);
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
						tile.addWay(w);
					}
				}
				else if(allowSeaSectors) {
					System.out.println("in allowSeaSectors");
					seaId = FakeIdGenerator.makeFakeId();
					sea = new Way(seaId);
					sea.getNodes().addAll(points);
					//sea.addNode(new Node(pEnd.getLat(), pStart.getLon(), 
					//						FakeIdGenerator.makeFakeId()));
					// FIXME seems to only work for one tile

					Node p;
					int startedge = 0;
					int endedge = 3;
					EdgeHit startEdgeHit = getNextEdgeHit(seaBounds, pStart) ;
					EdgeHit endEdgeHit = getNextEdgeHit(seaBounds, pEnd) ;
					startedge = startEdgeHit.edge;
					endedge = endEdgeHit.edge;
					System.out.println("startedge: " + startedge + " endedge: " + endedge);
					if (endedge < startedge) {
						endedge += 4;
					}

					for (int i=endedge; i > startedge; i--) {
						int edge = i % 4;
						float val = 0.0f;
						System.out.println("edge: " + edge + " val: " + val);
						EdgeHit corner = new EdgeHit(edge, val);
						p = corner.getPoint(seaBounds);
						log.debug("way: ", corner, p);
						System.out.println("way: corner: " + corner + " p: " + p);

						sea.addNodeIfNotEqualToLastNode(p);
					}
					sea.addNode(pStart);
					// TODO: Is natural=sea good to get it shown? 
					// Is something else (colours?) needed for Osm2GpsMid?
					sea.setAttribute("natural", "sea");
					sea.setAttribute("area", "yes");
					/* This line is not unnecessary as it triggers the calculation of the way's type
					 * (which is why making this method sound like it's a simple getter is 
					 * a bloody bad idea).
					 */
					// polish.api.bigstyles
					short t = sea.getType(configuration);
					log.info("sea (newly created): ", sea);
					System.out.println("  Sea (newly created): " + sea);
					tile.addWay(sea);
					if(generateSeaUsingMP) {
						// TODO seaRelation.addElement("outer", sea);
					}
					generateSeaBackground = false;
				}
				else if (extendSeaSectors) {
					// create additional points at next border to prevent triangles from point 2
					if (null == hStart) {
						hStart = getNextEdgeHit(seaBounds, pStart);
						w.getNodes().add(0, hStart.getPoint(seaBounds));
					}
					if (null == hEnd) {
						hEnd = getNextEdgeHit(seaBounds, pEnd);
						w.getNodes().add(hEnd.getPoint(seaBounds));
					}
					log.debug("hits (second try): ", hStart, hEnd);
					hitMap.put(hStart, w);
					hitMap.put(hEnd, null);
				}
				else {
					// show the coastline even though we can't produce
					// a polygon for the land
					w.setAttribute("natural", "coastline");
					tile.addWay(w);
				}
			}
			else {
				log.debug("hits: ", hStart, hEnd);
				hitMap.put(hStart, w);
				hitMap.put(hEnd, null);
			}
		}

		// now construct inner ways from these segments
		NavigableSet<EdgeHit> hits = (NavigableSet<EdgeHit>) hitMap.keySet();
		boolean shorelineReachesBoundary = false;
		while (!hits.isEmpty()) {
			long id = FakeIdGenerator.makeFakeId();
			Way w = new Way(id);
			tile.addWay(w);

			EdgeHit hit =  hits.first();
			EdgeHit hFirst = hit;
			do {
				Way segment = hitMap.get(hit);
				log.info("current hit: " + hit);
				EdgeHit hNext;
				if (segment != null) {
					// add the segment and get the "ending hit"
					log.info("adding: ", segment);
					for(Node p : segment.getNodes()) {
						w.addNodeIfNotEqualToLastNode(p);
					}
					hNext = getEdgeHit(seaBounds, segment.getNodes().get(segment.getNodes().size()-1));
				}
				else {
					w.addNodeIfNotEqualToLastNode(hit.getPoint(seaBounds));
					hNext = hits.higher(hit);
					if (hNext == null) {
						hNext = hFirst;
					}

					Node p;
					if (hit.compareTo(hNext) < 0) {
						log.info("joining: ", hit, hNext);
						for (int i=hit.edge; i<hNext.edge; i++) {
							EdgeHit corner = new EdgeHit(i, 1.0);
							p = corner.getPoint(seaBounds);
							log.debug("way: ", corner, p);
							w.addNodeIfNotEqualToLastNode(p);
						}
					}
					else if (hit.compareTo(hNext) > 0) {
						log.info("joining: ", hit, hNext);
						for (int i=hit.edge; i<4; i++) {
							EdgeHit corner = new EdgeHit(i, 1.0);
							p = corner.getPoint(seaBounds);
							log.debug("way: ", corner, p);
							w.addNodeIfNotEqualToLastNode(p);
						}
						for (int i=0; i<hNext.edge; i++) {
							EdgeHit corner = new EdgeHit(i, 1.0);
							p = corner.getPoint(seaBounds);
							log.debug("way: ", corner, p);
							w.addNodeIfNotEqualToLastNode(p);
						}
					}
					w.addNodeIfNotEqualToLastNode(hNext.getPoint(seaBounds));
				}
				hits.remove(hit);
				hit = hNext;
			} while (!hits.isEmpty() && !hit.equals(hFirst));

			if (!w.isClosed()) {
				w.getNodes().add(w.getNodes().get(0));
			}
			log.info("adding non-island landmass, hits.size()=" + hits.size());
			islands.add(w);
			shorelineReachesBoundary = true;
		}

		// TODO roadsReachBoundary if(!shorelineReachesBoundary && roadsReachBoundary) {
			// try to avoid tiles being flooded by anti-lakes or other
			// bogus uses of natural=coastline
			//generateSeaBackground = false;
		//}

		List<Way> antiIslands = new ArrayList<Way>();

		for (Way w : islands) {

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

			// determine where the water is
			if(w.isClockwise()) {
				// water on the inside of the poly, it's an
				// "anti-island" so tag with natural=water (to
				// make it visible above the land)
				w.setAttribute("natural", "water");
				antiIslands.add(w);
				tile.addWay(w);
			}
			else {
				// water on the outside of the poly, it's an island
				if(generateSeaUsingMP) {
					// create a "inner" way for each island
					// TODO seaRelation.addElement("inner", w);
				}
				else {
					// tag as land
					w.setAttribute(landTag[0], landTag[1]);
					w.setAttribute("area", "yes");
					/* This line is not unnecessary as it triggers the calculation of the way's type
					 * (which is why making this method sound like it's a simple getter is 
					 * a bloody bad idea).
					 */
					// polish.api.bigstyles
					short t = w.getType(configuration);
					tile.addWay(w);
				}
			}
		}

		islands.removeAll(antiIslands);

		if(islands.isEmpty()) {
			// the tile doesn't contain any islands so we can assume
			// that it's showing a land mass that contains some
			// enclosed sea areas - in which case, we don't want a sea
			// coloured background
			generateSeaBackground = false;
		}

		if (generateSeaBackground) {

			// the background is sea so all anti-islands should be
			// contained by land otherwise they won't be visible

			for(Way ai : antiIslands) {
				boolean containedByLand = false;
				for(Way i : islands) {
					if(i.containsPointsOf(ai)) {
						containedByLand = true;
						break;
					}
				}
				if(!containedByLand) {
					// found an anti-island that is not contained by
					// land so convert it back into an island
					//ai.deleteTag("natural");
					if(generateSeaUsingMP) {
						// create a "inner" way for the island
						// TODO seaRelation.addElement("inner", ai);
						// TODO tile.removeWay(ai.getId());
					} else {
						ai.setAttribute(landTag[0], landTag[1]);
					}
					log.warn("Converting anti-island starting at " + 
							ai.getNodes().get(0).toUrl() + 
							" into an island as it is surrounded by water");
				}
			}

			seaId = FakeIdGenerator.makeFakeId();
			sea = new Way(seaId);
			if (generateSeaUsingMP) {
				// the sea background area must be a little bigger than all
				// inner land areas. this is a workaround for a mp shortcoming:
				// mp is not able to combine outer and inner if they intersect
				// or have overlaying lines
				// the added area will be clipped later by the style generator

				// mkgmap uses ints for lat/lon where a digit is 1 / (2^24) of a degree
				// (see Utils.toMapUnit()). So a value of 1 is 0.0000214576721191 degrees
				// or about 0.077 arc seconds.

				sea.addNode(new Node(nw.getLat() - 0.00002f,
						nw.getLon() - 0.00002f, FakeIdGenerator.makeFakeId()));
				sea.addNode(new Node(sw.getLat() + 0.00002f,
						sw.getLon() - 0.00002f, FakeIdGenerator.makeFakeId()));
				sea.addNode(new Node(se.getLat() + 0.00002f,
						se.getLon() + 0.00002f, FakeIdGenerator.makeFakeId()));
				sea.addNode(new Node(ne.getLat() - 0.00002f,
						ne.getLon() + 0.00002f, FakeIdGenerator.makeFakeId()));
				sea.addNode(new Node(nw.getLat() - 0.00002f,
						nw.getLon() - 0.00002f, FakeIdGenerator.makeFakeId()));
			} else {
				sea.addNode(nw);
				sea.addNode(sw);
				sea.addNode(se);
				sea.addNode(ne);
				sea.addNode(nw);
			}
			sea.setAttribute("natural", "sea");
			sea.setAttribute("area", "yes");
			/* This line is not unnecessary as it triggers the calculation of the way's type
			 * (which is why making this method sound like it's a simple getter is 
			 * a bloody bad idea).
			 */
			// polish.api.bigstyles
			short t = sea.getType(configuration);
			log.info("sea background: ", sea);
			System.out.println("  Sea background: " + sea);
			tile.addWay(sea);
			if(generateSeaUsingMP) {
				// TODO seaRelation.addElement("outer", sea);
			}
		}
		else {
			// background is land
			if(!generateSeaUsingMP) {
				// generate a land polygon so that the tile's
				// background colour will match the land colour on the
				// tiles that do contain some sea
				long landId = FakeIdGenerator.makeFakeId();
				Way land = new Way(landId);
				land.addNode(nw);
				land.addNode(sw);
				land.addNode(se);
				land.addNode(ne);
				land.addNode(nw);
				land.setAttribute(landTag[0], landTag[1]);
				land.setAttribute("area", "yes");
				/* This line is not unnecessary as it triggers the calculation of the way's type
				 * (which is why making this method sound like it's a simple getter is 
				 * a bloody bad idea).
				 */
				// polish.api.bigstyles
				short t = land.getType(configuration);
				tile.addWay(land);
			}
		}

		if (generateSeaUsingMP) {
			Bounds mpBbox = tile.getBounds();
			// TODO seaRelation = new MultiPolygonRelation(seaRelation, wayMap, mpWayRemoveTags, mpBbox);
			//relationMap.put(multiId, seaRelation);
			//seaRelation.processElements();
		}
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
			log.info("getPoint: ", this, a);
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
		return getEdgeHit(a, p, 0.0002f);
	}

	private static EdgeHit getEdgeHit(Bounds a, Node p, float tolerance)
	{
		float lat = p.getLat();
		float lon = p.getLon();
		float minLat = a.getMinLat();
		float maxLat = a.getMaxLat();
		float minLong = a.getMinLon();
		float maxLong = a.getMaxLon();

		log.info(String.format("getEdgeHit: (%f %f) (%f %f %f %f)", 
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

		log.info(String.format("getNextEdgeHit: (%f %f) (%f %f %f %f)", 
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
	
	private static void concatenateWays(List<Way> ways, Bounds bounds, Tile tile) {
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
					log.info("merging: ", ways.size(), w1.getId(), w2.getId());
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
						log.warn("Bridging " + (int)smallestGap + "m gap in coastline from " + 
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
						tile.addWay(w);
						changed = true;
						break;
					}
				}
			}
		}
	}
}
