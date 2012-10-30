/**
 * This file is part of OSM2ShareNav
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation. 
 * See COPYING.
 *
 * Copyright (c) 2011 sk750
 */

package net.sharenav.osmToShareNav.area;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.TreeMap;

import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;
import net.sharenav.osmToShareNav.Configuration;
import net.sharenav.osmToShareNav.MyMath;
import net.sharenav.osmToShareNav.OsmParser;
import net.sharenav.osmToShareNav.model.Bounds;
import net.sharenav.osmToShareNav.model.Member;
import net.sharenav.osmToShareNav.model.Node;
import net.sharenav.osmToShareNav.model.Relation;
import net.sharenav.osmToShareNav.model.Way;


public class SeaGenerator2 {
	public float minLat = Float.MAX_VALUE;
	public float minLon = Float.MAX_VALUE;
	public float maxLat = -Float.MAX_VALUE;
	public float maxLon = -Float.MAX_VALUE;

	public float minMapLat = Float.MAX_VALUE;
	public float minMapLon = Float.MAX_VALUE;
	public float maxMapLat = -Float.MAX_VALUE;
	public float maxMapLon = -Float.MAX_VALUE;

	// for debugging
	private boolean onlyOutlines = false;
    	// this helps some areas like Canary islands
        // but may be sub-optimal, may cause smaller triangles
        // than necessary; would be better
        // if tile splitting code and triangle splitting code
    	// would take care of things
	private boolean interimNodes = true;

	private static boolean generateSea = true;
	private static boolean generateSeaUsingMP = false;
	private static boolean allowSeaSectors = false;
	private static boolean extendSeaSectors = true;
	private static int maxCoastlineGap = 0;

	private static Configuration configuration;

	private boolean foundCoast = false;
	
	private static boolean debugSea = false;

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
	
	public void generateSea(OsmParser parser) {
		seaBounds = new Bounds();

		// remember coastlines and add them to landways
		String natural;
		ArrayList<Way> landWays = new ArrayList<Way>();
		for (Way w: parser.getWays()) {
			natural = w.getAttribute("natural");
			if (natural != null) {
				if ("coastline".equals(natural)) {
					if (debugSea) {
						//System.out.println("Create land from coastline  " + w.toUrl());
					}
					// for closed ways, save memory and do not create new ways
					if (w.isClosed()) {
						landWays.add(w);
					} else {
						long landId = FakeIdGenerator.makeFakeId();
						Way wLand = new Way(landId, w);
						landWays.add(wLand);
					}
					// find minimum/maximum lat and lon for the bundle
					seaBounds.extend(w.getBounds());
				}
			}
		}
		
		Node sw = new Node(seaBounds.minLat, seaBounds.minLon, FakeIdGenerator.makeFakeId());
		Node se = new Node(seaBounds.minLat, seaBounds.maxLon, FakeIdGenerator.makeFakeId());
		Node nw = new Node(seaBounds.maxLat, seaBounds.minLon, FakeIdGenerator.makeFakeId());
		Node ne = new Node(seaBounds.maxLat, seaBounds.maxLon, FakeIdGenerator.makeFakeId());

		// use sea tiles: divide map area into separate parts for more efficient processing (experimental)
		if (configuration.getUseSeaTiles()) {
			// divide map area into parts; run sea multipolygon generation for each part
			final int latDivider = (int) MyMath.dist(sw, nw) / 5000 + 2;
			final int lonDivider = (int) MyMath.dist(sw, se) / 5000 + 2;

			Bounds allMapBounds = seaBounds.clone();

			for (int lat = 0; lat < latDivider ; lat++) {
				for (int lon = 0; lon < lonDivider ; lon++) {
					// loop x & y
					sw = new Node(allMapBounds.minLat + (allMapBounds.maxLat - allMapBounds.minLat) / latDivider * lat,
							   allMapBounds.minLon + (allMapBounds.maxLon - allMapBounds.minLon) / lonDivider * lon,
							   FakeIdGenerator.makeFakeId());
					nw = new Node(allMapBounds.minLat + (allMapBounds.maxLat - allMapBounds.minLat) / latDivider * (lat + 1),
							   allMapBounds.minLon + (allMapBounds.maxLon - allMapBounds.minLon) / lonDivider * lon,
							   FakeIdGenerator.makeFakeId());
					se = new Node(allMapBounds.minLat + (allMapBounds.maxLat - allMapBounds.minLat) / latDivider * lat,
							   allMapBounds.minLon + (allMapBounds.maxLon - allMapBounds.minLon) / lonDivider * (lon + 1),
							   FakeIdGenerator.makeFakeId());
					ne = new Node(allMapBounds.minLat + (allMapBounds.maxLat - allMapBounds.minLat) / latDivider * (lat + 1),
							   allMapBounds.minLon + (allMapBounds.maxLon - allMapBounds.minLon) / lonDivider * (lon + 1),
							   FakeIdGenerator.makeFakeId());

					seaBounds.minLat = sw.lat;
					seaBounds.maxLat = nw.lat;
					seaBounds.minLon = sw.lon;
					seaBounds.maxLon = se.lon;
					// whole map area sea generation

					float seaMargin = 0.000005f;
					float seaTileMargin = 0.000010f;

					seaBounds.minLat -= seaMargin;
					seaBounds.minLon -= seaMargin;
					seaBounds.maxLat += seaMargin;
					seaBounds.maxLon += seaMargin;

					mapBounds = seaBounds.clone();

					mapBounds.minLat += seaTileMargin;
					mapBounds.minLon += seaTileMargin;
					mapBounds.maxLat -= seaTileMargin;
					mapBounds.maxLon -= seaTileMargin;

					if (configuration.verbose >= 0) {
						if (debugSea) {
							System.out.println("seaBounds: " + seaBounds);
						}
						if (debugSea) {
							System.out.println("mapBounds: " + mapBounds);
						}
					}

					landWays.clear();
					foundCoast = false;
					for (Way w: parser.getWays()) {
						natural = w.getAttribute("natural");
						if (natural != null) {
							if ("coastline".equals(natural)) {
								//System.out.println("Create land from coastline  " + w.toUrl());
								// for closed ways, save memory and do not create new ways

								// check if in map bounds; if not, skip this
								// FIXME should cut ways on sea tile boundary

								Way boundedWay = new Way(w.id);

								for (Node node : w.getNodes()) {
									if (mapBounds.isIn(node.getLat(), node.getLon())) {
										boundedWay.addNodeIfNotEqualToFirstNodeOfTwo(node);
									} else {
										// way is going out of sea tile, cut here, add the
										// first part
                                                                               if (boundedWay.getNodeCount() != 0) {
                                                                                       long readyId = FakeIdGenerator.makeFakeId();
                                                                                       Way wReady = new Way(readyId, boundedWay);
                                                                                       landWays.add(wReady);
                                                                                       foundCoast = true;

										       if (debugSea) {
											       System.out.println("Node out of bound, splitting way");
											       System.out.println("w: " + w +
                                                                                                          " wReady: " + wReady +
                                                                                                          " boundedWay: " + boundedWay);
										       }
                                                                               }
									       boundedWay = new Way(w.id);
									}
								}
								if (boundedWay.isValid()) {
									long landId = FakeIdGenerator.makeFakeId();
									Way wLand = new Way(landId, boundedWay);
									landWays.add(wLand);
									foundCoast = true;
								}
							}
						}
					}

					generateSeaMultiPolygon(parser, sw, se, nw, ne, landWays, mapBounds);
				}
			}					
		} else {
			// whole map area sea generation
			mapBounds = seaBounds.clone();

			float seaMargin = 0.000005f;

			seaBounds.minLat -= seaMargin;
			seaBounds.minLon -= seaMargin;
			seaBounds.maxLat += seaMargin;
			seaBounds.maxLon += seaMargin;

			if (debugSea) {
				System.out.println("seaBounds: " + seaBounds);
				System.out.println("mapBounds: " + mapBounds);
			}

			generateSeaMultiPolygon(parser, sw, se, nw, ne, landWays, mapBounds);
		}
	}
	public void generateSeaMultiPolygon(OsmParser parser, Node sw, Node se, Node nw, Node ne, ArrayList<Way> landWays, Bounds mapBounds) {
		long seaId = FakeIdGenerator.makeFakeId();
		Way sea = new Way(seaId);
		sea.addNode(sw);

		if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
			sea.addNodeIfNotEqualToLastNodeWithInterimNodes(nw);
			sea.addNodeIfNotEqualToLastNodeWithInterimNodes(ne);
			sea.addNodeIfNotEqualToLastNodeWithInterimNodes(se);
			sea.addNodeIfNotEqualToLastNodeWithInterimNodes(sw);
		} else {
			sea.addNodeIfNotEqualToLastNode(nw);
			sea.addNodeIfNotEqualToLastNode(ne);
			sea.addNodeIfNotEqualToLastNode(se);
			sea.addNodeIfNotEqualToLastNode(sw);
		}
		long multiId = FakeIdGenerator.makeFakeId();
		Relation seaRelation = new Relation(multiId);
		if (!onlyOutlines) {
			seaRelation.setAttribute("type", "multipolygon");
			seaRelation.setAttribute("natural", "sea");
		}

		Member mInner;

		// handle islands (closed shoreline components) first (they're easy)
		// add closed landways (islands, islets) to parser and to sea relation
		// these are inner members in the sea relation,
		// in other words holes in the sea 
		Iterator<Way> it = landWays.iterator();
		while (it.hasNext()) {
			Way w = it.next();

			// check if in map bounds; if not, skip this
			// FIXME should cut ways on sea tile boundary

			Way boundedWay = new Way(w.id);

			for (Node node : w.getNodes()) {
				if (mapBounds.isIn(node.getLat(), node.getLon())) {
					boundedWay.addNode(node);
				} else {
					// Nodes for this way are missing, problem in OSM or simply
					// out of bounding box.
					// Three different cases are possible:
					// missing at the start, in the middle or at the end.
					// We simply add the current way and start a new one
					// with shared attributes.
					// Degenerate ways are not added, so don't care about
					// this here.
					//if (boundedWay.getNodeCount() != 0) {
					//	Way tmp_way = new Way(boundedWay);
					//	parser.addWay(boundedWay);
					//	way = tmp_way;
					//}
				}
			}
			if (boundedWay.isValid() && boundedWay.getNodeCount() != 0) {
				foundCoast = true;
			} else {
				it.remove();
				continue;
			}

			if (boundedWay.isClosed()) {
				//System.out.println("adding island " + w);
				parser.addWay(boundedWay);
				it.remove();
				if (onlyOutlines || configuration.getDrawSeaOutlines()) {
					boundedWay.setAttribute("natural", "seaoutline");
				}
				mInner = new Member("way", boundedWay.id, "inner");
				seaRelation.add(mInner);			
			}
		}

		// while relation handling code does concatenate ways, we need
		// this to see where the start and end for coastlines are
		// so we can decide how to connect partial coastlines to e.g. map borders
		concatenateWays(landWays, mapBounds, parser, seaRelation, onlyOutlines);
		
		// there may be more islands now

		it = landWays.iterator();
		while (it.hasNext()) {
			Way w = it.next();

			if (w.isClosed()) {
				if (debugSea) {
					System.out.println("after concatenation: adding island " + w);
				}
				parser.addWay(w);
				it.remove();
				mInner = new Member("way", w.id, "inner");
				if (onlyOutlines || configuration.getDrawSeaOutlines()) {
					w.setAttribute("natural", "seaoutline");
				}
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
			// FIXME don't force this after coastlines are truly cut at map/seatile border
			hStart = null;
			hEnd = null;
			if (hStart == null || hEnd == null) {
				String msg = String.format(
						"Non-closed coastline segment does not hit bounding box: start %s end %s\n" +
						"  See %s and %s\n",
						pStart.toString(), pEnd.toString(),
						pStart.toUrl(), pEnd.toUrl());
				if (debugSea) {
					System.out.println(msg);
				}
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
				if (debugSea) {
					System.out.println("dist from coastline start to end: " + MyMath.dist(pStart, pEnd));
				}
				boolean nearlyClosed = (MyMath.dist(pStart, pEnd) < 0.1 * length);

				// FIXME enable again when coastlines are cut exactly at tile borders so this doesn't cause trouble
				if (false && nearlyClosed) {
					if (debugSea) {
						System.out.println("handling nearlyClosed coastline: " + w);
					}
					// close the way
					points.add(pStart);
					if (generateSeaUsingMP) {
						if (onlyOutlines || configuration.getDrawSeaOutlines()) {
							w.setAttribute("natural", "seaoutline");
						}
						mInner = new Member("way", w.id, "inner");
						seaRelation.add(mInner);
						// polish.api.bigstyles
						short t = w.getType(configuration);
						parser.addWay(w);
					} else {
						System.out.println("ERROR: SeaGenerator: can only create sea properly as relations");
					}
				}
				else if (allowSeaSectors && false) {  // this part appears to cause trouble, removed
					if (debugSea) {
						System.out.println("handling allowSeaSectors coastline: " + w);
					}
					seaId = FakeIdGenerator.makeFakeId();
					seaSector = new Way(seaId);

					EdgeHit startEdgeHit = getNextEdgeHit(mapBounds, pStart);
					EdgeHit endEdgeHit = getNextEdgeHit(mapBounds, pEnd);
					int startedge = startEdgeHit.edge;
					int endedge = endEdgeHit.edge;

					if (debugSea) {
						System.out.println("startedge: " + startedge + " endedge: " + endedge);
					}

					if (false || false) {
						Node p;
						//System.out.println("edge: " + edge + " val: " + val);
						if (endedge < startedge) {
							endedge += 4;
						}

						for (int i=endedge; i > startedge; i--) {
							int edge = i % 4;
							float val = 0.0f;
							if (debugSea) {
								System.out.println("edge: " + edge + " val: " + val);
							}
							EdgeHit corner = new EdgeHit(edge, val);
							p = corner.getPoint(mapBounds);
							//log.debug("way: ", corner, p);
							if (debugSea) {
								System.out.println("way: corner: " + corner + " p: " + p);
							}

							if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
								seaSector.addNodeIfNotEqualToLastNodeWithInterimNodes(p);
							} else {
								seaSector.addNodeIfNotEqualToLastNode(p);
							}
						}
					}
					if (generateSeaUsingMP) {
						parser.addWay(seaSector);
						mInner = new Member("way", seaSector.id, "inner");
						if (onlyOutlines || configuration.getDrawSeaOutlines()) {
							seaSector.setAttribute("natural", "seaoutline");
						}
						seaRelation.add(mInner);
						//System.out.println("Added inner to sea relation: " + seaRelation.toString());
					}
				}
				else if (extendSeaSectors) {
					// create additional points at next border to prevent triangles from point 2
					if (debugSea) {
						System.out.println("Extend sea sector, way id: " + w.id);
					}
					if (null == hStart) {
						// attach start of way to edge, with interim nodes
						// when necessary
						hStart = getNextEdgeHit(mapBounds, pStart);
						// without interim nodes
						//w.getNodes().add(0, hStart.getPoint(mapBounds));
						// with interim nodes
						Node p = hStart.getPoint(mapBounds);
						Way helperWay = new Way(FakeIdGenerator.makeFakeId());
						List<Node> oldpoints = w.getNodes();
						helperWay.addNode(p);
						if (debugSea) {
							System.out.println("building the helper way");
						}
						if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
							helperWay.addNodeIfNotEqualToLastNodeWithInterimNodes(oldpoints.get(0));
						} else {
							helperWay.addNodeIfNotEqualToLastNode(oldpoints.get(0));
						}
						List<Node> helperPoints = helperWay.getNodes();
						for (int i = helperPoints.size()-1 ; i >= 0; i--) {
							w.getNodes().add(0, helperPoints.get(i));
						}
						if (debugSea) {
							System.out.println("startedge: " + hStart.edge);
						}
					}
					if (null == hEnd) {
						hEnd = getNextEdgeHit(mapBounds, pEnd);
						Node p = hEnd.getPoint(mapBounds);
						//w.getNodes().add(hEnd.getPoint(mapBounds));
						if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
							w.addNodeIfNotEqualToLastNodeWithInterimNodes(p);
						} else {
							w.addNodeIfNotEqualToLastNode(p);
						}
						if (debugSea) {
							System.out.println("endedge: " + hEnd.edge);
						}
					}
					//log.debug("hits (second try): ", hStart, hEnd);
					mInner = new Member("way", w.id, "inner");
					if (onlyOutlines || configuration.getDrawSeaOutlines()) {
						w.setAttribute("natural", "seaoutline");
					}
					seaRelation.add(mInner);
					hitMap.put(hStart, w);
					hitMap.put(hEnd, null);
					parser.addWay(w);
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
			//parser.addWay(w);

			EdgeHit hit =  hits.first();
			EdgeHit hFirst = hit;
			do {
				Way segment = hitMap.get(hit);
				if (debugSea) {
					System.out.println("current hit: " + hit);
				}
				EdgeHit hNext;
				if (segment != null) {
					// could do better with adding segments to
					// relation
					// add the segment and get the "ending hit"
					if (debugSea) {
						System.out.println("adding sgement: " + segment);
					}
					for(Node p : segment.getNodes()) {
						if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
							w.addNodeIfNotEqualToLastNodeWithInterimNodes(p);
						} else {
							w.addNodeIfNotEqualToLastNode(p);
						}
					}
					hNext = getEdgeHit(mapBounds, segment.getNodes().get(segment.getNodes().size()-1));
				} else { // segment == null
					if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
						w.addNodeIfNotEqualToLastNodeWithInterimNodes(hit.getPoint(mapBounds));
					} else {
						w.addNodeIfNotEqualToLastNode(hit.getPoint(mapBounds));
					}
					hNext = hits.higher(hit);
					if (hNext == null) {
						hNext = hFirst;
					}

					Node p;
					if (hit.compareTo(hNext) < 0) {
						//log.info("joining: ", hit, hNext);
						if (debugSea) {
							System.out.println("joining compareTo < 0, hit: " +  hit + " hNext: " + hNext);
						}
						for (int i=hit.edge; i<hNext.edge; i++) {
							EdgeHit corner = new EdgeHit(i, 1.0);
							p = corner.getPoint(mapBounds);
							//log.debug("way: ", corner, p);
							if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
								w.addNodeIfNotEqualToLastNodeWithInterimNodes(p);
							} else {
								w.addNodeIfNotEqualToLastNode(p);
							}
						}
					}
					else if (hit.compareTo(hNext) > 0) {
						if (debugSea) {
							System.out.println("joining compareTo > 0: " + hit + " hNext: " + hNext);
						}
						int hNextEdge = hNext.edge;
						if (hit.edge >= hNext.edge) {
							hNextEdge += 4;
						}

						for (int i=hit.edge; i < hNextEdge; i++) {
							EdgeHit corner = new EdgeHit(i % 4, 1.0);
							p = corner.getPoint(mapBounds);
							//log.debug("way: ", corner, p);
							if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
								w.addNodeIfNotEqualToLastNodeWithInterimNodes(p);
							} else {
								w.addNodeIfNotEqualToLastNode(p);
							}
						}
					}
					if (onlyOutlines || interimNodes || configuration.getDrawSeaOutlines()) {
						w.addNodeIfNotEqualToLastNodeWithInterimNodes(hNext.getPoint(mapBounds));
					} else {
						w.addNodeIfNotEqualToLastNode(hNext.getPoint(mapBounds));
					}
				}
				hits.remove(hit);
				hit = hNext;
			} while (!hits.isEmpty() && !hit.equals(hFirst));

			if (!w.isClosed()) {
				w.getNodes().add(w.getNodes().get(0));
			}
			parser.addWay(w);
			if (onlyOutlines || configuration.getDrawSeaOutlines()) {
				w.setAttribute("natural", "seaoutline");
			}
			mInner = new Member("way", w.id, "inner");
			seaRelation.add(mInner);
			//System.out.println("Added inner member to sea relation: " + seaRelation.toString());
			//System.out.println("adding non-island landmass, hits.size()=" + hits.size());
			//islands.add(w);
			shorelineReachesBoundary = true;
			// FIXME instead of adding inner members to define land here,
			// we could track the outline and add outer members to define sea.
			// Depending on the map area, the resulting multipolygons could be lighter
			// to handle in triangulation and the tile & way splitting.
			// In country-level cases where a country has coast at only one border
			// the savings could be significant.
			// Some issues with this
			// * sea might not be in only one closed way, but multiple ways
			// ** relation code currently copes with one outer member, not mutiple
			// ** trianglulation code probably needs to know which inner polygons
			//    belong to which outer polygon, so code would be needed for that
			// * others?
			// I think the more typical case however is that sea is in one piece.
			// We can probably check if sea is in one or more pieces, and decide
			// which kind of multipolygon to produce.
		}

		// add sea relation
		if (foundCoast) {
			if (generateSeaUsingMP) {
				// create a multipolygon relation containing water as outer role
				mInner = new Member("way", sea.id, "outer");
				if (onlyOutlines || configuration.getDrawSeaOutlines()) {
					sea.setAttribute("natural", "seaoutline");
				}
				seaRelation.add(mInner);
				parser.addWay(sea);			
				parser.addRelation(seaRelation);
				//System.out.println("Adding sea relation: " + seaRelation.toString());
			} else {
				System.out.println("ERROR: SeaGenerator: can only create sea properly as relations");
			}
		} else {
			// FIXME sometimes it's sea, deduce from contents and/or neighbouring tiles
			if (debugSea) {
				System.out.println("SeaGenerator: didn't find any coastline ways, assuming this seatile is land");
			}
		}
	
		if (debugSea) {
			System.out.println(seaRelation.toString());
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
			//System.out.println("getPoint: " + a);
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
		// this might need adjustment - was 0.0004
		//return getEdgeHit(a, p, 0.04f);
		return getEdgeHit(a, p, 0.0004f);
	}

	private static EdgeHit getEdgeHit(Bounds a, Node p, float tolerance)
	{
		float lat = p.getLat();
		float lon = p.getLon();
		float minLat = a.getMinLat();
		float maxLat = a.getMaxLat();
		float minLong = a.getMinLon();
		float maxLong = a.getMaxLon();

		//System.out.println(String.format("getEdgeHit: (%f %f) (%f %f %f %f)", 
		//		lat, lon, minLat, minLong, maxLat, maxLong));
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

		if (debugSea) {
			System.out.println(String.format("getNextEdgeHit: (%f %f) (%f %f %f %f)", 
							 lat, lon, minLat, minLong, maxLat, maxLong));
		}
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
	private static void concatenateWays(List<Way> ways, Bounds bounds,
					    OsmParser parser, Relation seaRelation, boolean onlyOutlines) {
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
		// this could cause trouble near the edges
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
					// FIXME this causes trouble with the non-border-ending cut coastlines, disable for now
					if (false && nearest != null && smallestGap < maxCoastlineGap) {
						Node w2s = nearest.getNodes().get(0);
						if (debugSea) {
							System.out.println("SeaGenerator: Bridging " + (int)smallestGap + "m gap in coastline from " + 
									   w1e.toUrl() + " to " + w2s.toUrl());
						}
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
						// FIXME check if this is correct
						wm.getNodes().addAll(nearest.getNodes());
						ways.remove(nearest);
						// make a line that shows the filled gap
						Way w = new Way(FakeIdGenerator.makeFakeId());
						// TODO: So we need a style definition for this
						//w.setAttribute("natural", "coastline-gap");

						// no need for this probably, as we're bridging closeby nodes

						//if (onlyOutlines) {
						//	w.addNodeIfNotEqualToLastNodeWithInterimNodes(w1e);
						//	w.addNodeIfNotEqualToLastNodeWithInterimNodes(w2s);
						//} else {

						w.addNode(w1e);
						w.addNode(w2s);

						//}
						parser.addWay(w);
						if (onlyOutlines || configuration.getDrawSeaOutlines()) {
							w.setAttribute("natural", "seaoutline");
						}
						Member  mInner = new Member("way", w.id, "inner");
						seaRelation.add(mInner);			
						changed = true;
						break;
					}
				}
			}
		}
	}
}
