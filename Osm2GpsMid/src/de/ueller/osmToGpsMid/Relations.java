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
import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.name.Names;
import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;

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
		System.out.println("Triangulating relations");
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
		int validRelationCount = 0;
		int invalidRelationCount = 0;
		int houseNumberCount = 0;
		int houseNumberInterpolationIgnoreCount = 0;
		int houseNumberRelationAcceptCount = 0;
		int houseNumberRelationProblemCount = 0;
		int houseNumberRelationIgnoreCount = 0;
		int boundaryIgnore = 0;

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
				Way outerWay = null;
				validRelationCount++;
				if (validRelationCount % 100 == 0) {
					System.out.println("info: handled " + validRelationCount + " relations");
					System.out.println("info: currently handling relation type " + r.getAttribute("type"));
				}
				String tagType = r.getAttribute("type");
				String tagValue = r.getAttribute(tagType);
				if (tagType != null && conf.getRelationExpansions().get(tagType + "=" + tagValue) != null
				    && conf.getRelationExpansions().get(tagType + "=" + tagValue)
				    && r.getTags().size() > 1) {
					// System.out.println("Checking " + tagType + "=" + tagValue);
					// FIXME check also that specialisation matches
					r.getTags().remove("type");
					for (Long ref : r.getWayIds()) {
						Way w = wayHashMap.get(ref);
						String key = "_route_" + tagValue;
						//System.out.println ("way: " + w + " key: " + key);
						long newId = 0;
						if (w.containsKey(key) && tagType != null &&
						    conf.getRelationExpansionsCombine().get(tagType + "=" + tagValue) != null &&
						    conf.getRelationExpansionsCombine().get(tagType + "=" + tagValue)) {
							// Combine the tags into one way
							newId = Long.valueOf(w.getAttribute(key));
							Way w2 = wayHashMap.get(newId);
							//System.out.println ("found key: " + key + " w2 = " +  w2 + "newId =" + newId);
							if (w2 != null) {
								//System.out.println ("way w2: " + w2 + " key: " + key);
								for (String t : r.getTags()) {
									if (w2.containsKey(t)) {
										// don't add to name if already there
										String [] values = w2.getAttribute(t).split(";");
										boolean exists = false;
										for (int v = 0; v < values.length ; v++) {
											if (values[v].equals(r.getAttribute(t))) {
												exists = true;
											}
										}
										// Way.java truncates more when alternative name is also used
										// (name/Names.java in GpsMid is where the EOF exception happens in reading of name)
										// might be that it's a byte which is used as the string length in SearchList.java / Names.java
										if (!exists && (w2.getAttribute(t).length() + r.getAttribute(t).length()) < 125)
											w2.setAttribute(t, w2.getAttribute(t) + ";" + r.getAttribute(t));
									} else {
										w2.setAttribute(t, r.getAttribute(t));
									}
								}
								w2.resetType(conf);
								// polish.api.bigstyles
								short type = w2.getType(conf);
							}
						} else {
							newId = FakeIdGenerator.makeFakeId();
							w.setAttribute(key, Long.toString(newId));
							// copy path from w into w2 with new id
							// FIXME should reverse way if role is "backward"
							Way w2 = new Way(newId, w);
							for (String t : r.getTags()) {
								if (w2.containsKey(t)) {
									if (!w2.getAttribute(t).equals(r.getAttribute(t)))
										w2.setAttribute(t, w2.getAttribute(t) + ";" + r.getAttribute(t));
								} else {
									w2.setAttribute(t, r.getAttribute(t));
								}
							}
							w2.resetType(conf);
							w.setAttribute(key, Long.toString(newId));
							// polish.api.bigstyles
							short type = w2.getType(conf);
							//System.out.println("adding way: " + w2 + " newId = " + newId);
							parser.addWay(w2);
						}
					}
				} else if (conf.useHouseNumbers && ("associatedStreet".equals(r.getAttribute("type")) ||
											 "street".equals(r.getAttribute("type")))) {
					//System.out.println("Handling housenumber relation " + r.toUrl());
					int wayCount = 0;
					for (Long ref : r.getWayIds(Member.ROLE_STREET)) {
						wayCount++;
						// should only need to be stored for one way due to way match rewriting
						if (wayCount > 1) {
							break;
						}
						Way w = wayHashMap.get(ref);
						for (Long noderef : r.getNodeIds(Member.ROLE_HOUSE)) {
							Node n = nodeHashMap.get(noderef);
							if (n != null) {
								w.houseNumberAdd(n);
								// add tag to point from
								//  System.out.println("setting node " + n + " __wayid to " + w.id);
								houseNumberCount++;
								n.setAttribute("__wayid", w.id.toString());
							} else {
								System.out.println("Warning: Relation " + r.toUrl() + " - ignoring house node noderef " + noderef + ", nodeHashMap.get(" + noderef + ") returned null");
								System.out.println(" -- possibly because the way the node http://www.openstreetmap.org/browse/node/" + noderef + " belongs to is a member of two different housenumber relations");
							}
							//System.out.println("Housenumber relation " + r.toUrl() + " - added node " + );
						}
						for (Long wayref : r.getWayIds(Member.ROLE_HOUSE)) {
							Way housew = wayHashMap.get(wayref);
							//System.out.println("Adding area way " + housew + " to housenumber relation " + r.toUrl());
							if (housew.containsKey("addr:interpolation")) {
								// FIXME add handling of interpolations, see http://wiki.openstreetmap.org/wiki/Proposed_features/House_numbers/Karlsruhe_Schema
								//
								// How this is done:
								// * look at the existing housenumber and do the interpolation calculations
								// * for each housenumber to add, do housenumber adding like is done below inside if (!n.containsKey("__wayid"))
								// A practical note: Apparently the great majority of these (at least in Germany where they're used a lot) are simple straight lines, ways with
								// two nodes, so just adding the simple code for interpolating for two-node ways would go a long way in practice to have
								// more GpsMid-findable addresses.
								//System.out.println("Warning: Relation " + r.toUrl() + " - ignoring interpolation way " + housew.toUrl());
								houseNumberInterpolationIgnoreCount++;
							} else if (housew.isArea() || housew.isClockwise() || housew.isCounterClockwise()) {
								// won't use just isArea() as some house roles won't work properly:
								// fenced areas, caravan sites or others not recognized as areas by GpsMid
								// examples:
								// http://www.openstreetmap.org/browse/way/82294062
								// http://www.openstreetmap.org/browse/way/42270858
								// There's no need to add a node at this stage, as this will be indexed as a building.
								// Node n = housew.getMidPointNodeByBounds();
								if (!housew.containsKey("__wayid")) {
									// if (n != null && !n.containsKey("__wayid")) {
									// FIXME if we want to have nodes for housenumbers in GpsMid
									// (if we want to list housenumber nodes in way data structure),
									// we'll probably need to create fake IDs and add the nodes to parser. Takes more space and gets more complicated.
									// w.houseNumberAdd(n);
									// FIXME we should do this without fake ids like this is done for ordinary houses
									//n.id = FakeIdGenerator.makeFakeId();
									// n.setAttribute("__wayid", w.id.toString());
									w.setAttribute("__wayid", w.id.toString());
									houseNumberCount++;
									//System.out.println("Housenumber relation " + r.toUrl() + " - added node " + );
									//for (String t : housew.getTags()) {
									//	n.setAttribute(t, housew.getAttribute(t));
									//}
									//n.resetType(conf);
									//n.getType(conf);
									//parser.addNode(n);
								} else {
									System.out.println("Warning: ignoring map data: __wayid in house number relation was already set for way: " + housew.toUrl());
								}
							} else {
								houseNumberRelationProblemCount++;
								System.out.println("Warning: ignoring map data: house number relation way was not an area or a an interpolation way: " + housew.toUrl());
							}
						}
					}
					houseNumberRelationAcceptCount++;
					i.remove();
				} else if ("multipolygon".equals(r.getAttribute("type"))) {
					if (r.getAttribute("admin_level") != null){
						// FIXME should not be blatantly ignore, but instead should be handled
						// if enabled in style file
						//System.out.println("Warning: ignoring relation with admin_level tag , relation" + r);
						boundaryIgnore++;
						continue;
					}
					if ("archipelago".equalsIgnoreCase(r.getAttribute("place"))) {
						// FIXME should not be blatantly ignore, but instead should be handled
						// if enabled in style file
						System.out.println("Warning: ignoring relation with place=archipelago tag, relation " + r);
						//boundaryIgnore++;
						continue;
					}
					if ("administrative".equalsIgnoreCase(r.getAttribute("boundary"))) {
						// FIXME should not be blatantly ignore, but instead should be handled
						// if enabled in style file
						//System.out.println("Warning: ignoring relation with boundary=administrative tag, relation " + r);
						boundaryIgnore++;
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
						// FIXME can be removed when proper coastline support exists
						//if ("coastline".equalsIgnoreCase(w.getAttribute("natural"))) {
							// this shouldn't cause trouble anymore, but give a message just in case
							//System.out.println("Warning: saw natural=coastline way " + w + " in relation " + r);
							//continue rel;
						//}

//					System.out.println("Handling outer way http://www.openstreetmap.org/browse/way/" + ref);
						if (w == null) {
							System.out.println("Way " + w.toUrl() + " was not found but referred as outline in ");
							System.out.println("  relation " + r.toUrl() + " I'll ignore this relation");
							continue rel;
						}
						long clonedWayId = FakeIdGenerator.makeFakeId();
						Way w2 = new Way(clonedWayId, w);
						//w2.cloneTagsButNoId(w);
						Outline no = createOutline(w2);

						if (no != null) {
//						//System.out.println("Adding way " + w.toUrl() + " as OUTER");
							a.addOutline(no);
							if (firstWay == null) {
								if (w2.triangles != null) {
									System.out.println("Strange, this outline is already triangulated! May be a duplicate, I'll ignore it");
									System.out.println("  way " + w.toUrl());
									System.out.println("  relation " + r.toUrl());
									continue rel;
								}
								firstWay = w2;
							} else {
								removeWays.add(w2);
							}
						}
						parser.addWay(w2);
						outerWay = w2;
						removeWays.add(w);
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
							outerWay.addHole(w);
							removeWays.add(w);
						}
					}
					try {
						if (Configuration.getConfiguration().triangleAreaFormat) {
							List<Triangle> areaTriangles = a.triangulate();
							firstWay.triangles = areaTriangles;
							//firstWay.recreatePathAvoidDuplicates();
							firstWay.recreatePath();
							triangles += areaTriangles.size();
//					System.out.println("areaTriangles.size for relation " + r.toUrl() + " :" + areaTriangles.size());
							areas += 1;
						}
						// neither methdod below to remove type tag works
						//r.getTags().remove("type");
						//if (r.containsKey("type")) {
						//	r.deleteTag("type");
						//}
						// so move all tags except type
						for (String key : r.getTags()) {
							if (!"type".equals(key)) {
								firstWay.setAttribute(key, r.getAttribute(key));
							}
						}
						//if (r.getTags().size() > 0) {
						//	firstWay.replaceTags(r);
						//}
					} catch (Exception e) {
						System.out.println("Something went wrong when trying to triangulate relation ");
						System.out.println("  " + r.toUrl() + " I'll attempt to ignore this relation");
						e.printStackTrace();
					}
				
					i.remove();
				}
			} else { // ! r.isValid()
				invalidRelationCount++;
				System.out.println("Relation not valid: " + r);
				if (conf.useHouseNumbers && ("associatedStreet".equals(r.getAttribute("type"))
							     || "street".equals(r.getAttribute("type")))) {
					houseNumberRelationIgnoreCount++;
				}
			}
		}
		
		int numMultipolygonMembersNotRemoved = 0;
		for (Way w : removeWays) {
			if (!w.isAccessForAnyRouting()) {
				// all relations have been handled, remove ways not needed for routing
				// FIXME - I think this is wrong - ways which are members of a multipolygon
				// can have other tags which makes them renderable, and this removes them if they're not
				// routable. Actually we should check whether the way is used for any rendering (check the type?)
				// maybe like this: 
				if (w.getType(conf) < 1) {
					//System.out.println("Removing way: " + w);
					parser.removeWay(w);
				} else {
					//System.out.println("Not removing way because type >= 1: " + w);
				}
			}
			else {
				numMultipolygonMembersNotRemoved++;
				//System.out.println("  info: multipolygon member " + w.toUrl() + " not removed because it is routable");
			}
		}
		if (numMultipolygonMembersNotRemoved > 0) {
			System.out.println("  info: " + numMultipolygonMembersNotRemoved + " multipolygon members not removed because they are routable");
		}
		
		//parser.resize();
		System.out.println("info: processed " + validRelationCount + " valid relations");
		System.out.println("info: ignored " + invalidRelationCount + " non-valid relations");
		System.out.println("info: accepted " + houseNumberCount + " housenumber-to-street connections from associatedStreet relations");
		System.out.println("info: ignored " + houseNumberRelationIgnoreCount + " associatedStreet (housenumber) relations");
		System.out.println("info: ignored " + houseNumberInterpolationIgnoreCount + " associatedStreet (housenumber) relation interpolation ways");
		System.out.println("info: processed " + houseNumberRelationAcceptCount + " associatedStreet (housenumber) relations"
			+ ", of which " +  houseNumberRelationProblemCount + " had problems");
		System.out.println("info: ignored " + boundaryIgnore + " boundary=administrative multipolygon relations");
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
