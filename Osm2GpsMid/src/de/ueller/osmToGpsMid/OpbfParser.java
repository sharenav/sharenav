/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2010 Kai Krueger
 */
package de.ueller.osmToGpsMid;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;
import crosby.binary.Osmformat.DenseInfo;
import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.Osmformat.HeaderBlock;
import crosby.binary.file.BlockInputStream;

import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.TurnRestriction;
import de.ueller.osmToGpsMid.model.Way;

public class OpbfParser extends OsmParser {

	/**
	 * @param i
	 */

	private class OsmPbfHandler extends BinaryParser {

		@Override
		public void parse(HeaderBlock block) {
			for (String s : block.getRequiredFeaturesList()) {
				if (s.equals("OsmSchema-V0.6")) {
					continue; // We can parse this.
				}
				if (s.equals("DenseNodes")) {
					continue; // We can parse this.
				}
				throw new Error("File requires unknown feature: " + s);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {

			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * crosby.binary.BinaryParser#parseDense(crosby.binary.Osmformat.DenseNodes
		 * )
		 */
		@Override
		protected void parseDense(DenseNodes dn) {
			long last_id = 0, last_lat = 0, last_lon = 0;

			int j = 0; // Index into the keysvals array.

			for (int i = 0; i < dn.getIdCount(); i++) {
				// List<Tag> tags = new ArrayList<Tag>(0);
				long lat = dn.getLat(i) + last_lat;
				last_lat = lat;
				long lon = dn.getLon(i) + last_lon;
				last_lon = lon;
				long id = dn.getId(i) + last_id;
				last_id = id;
				float latf = (float) parseLat(lat), lonf = (float) parseLon(lon);
				if (nodeInArea(latf, lonf)) {
					Node nd = new Node(latf, lonf, id);
					if (dn.getKeysValsCount() > 0) {
						while (dn.getKeysVals(j) != 0) {
							int keyid = dn.getKeysVals(j++);
							int valid = dn.getKeysVals(j++);
							String key = getStringById(keyid);
							if (LegendParser.getRelevantKeys().contains(key)) {
								nd.setAttribute(key, getStringById(valid));
							}
						}
						j++; // Skip over the '0' delimiter.
					}
					previousNodeWithThisId = nodes.put(nd.id, nd);
					nodeIns++;
					if (nd.getAttribute("highway") != null
							&& nd.getAttribute("highway").equalsIgnoreCase(
									"traffic_signals")) {
						// decrement trafficSignalCount if a previous node with this
						// id got replaced but was a traffic signal node
						if (previousNodeWithThisId != null
								&& previousNodeWithThisId.isTrafficSignals()) {
							trafficSignalCount--;
							System.out.println("DUPLICATE TRAFFIC SIGNAL NODE ID: "
									+ previousNodeWithThisId.id
									+ " more than once in osm file");
						}
						nd.markAsTrafficSignals();
						trafficSignalCount++;
					}
				} else {
					//If the nodes are not in the area, we still need to skip over the dens key-value representation:
					if (dn.getKeysValsCount() > 0) {
						while (dn.getKeysVals(j) != 0) {
							j+= 2;
						}
						j++; // Skip over the '0' delimiter.
					}
				}
				// If empty, assume that nothing here has keys or vals.

			}

			nodeTot += dn.getLatCount();
			ele += dn.getLatCount();
			printProgress();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see crosby.binary.BinaryParser#parseNodes(java.util.List)
		 */
		@Override
		protected void parseNodes(List<crosby.binary.Osmformat.Node> nds) {
			// TODO Auto-generated method stub
			// System.out.println("Parsing nodes: " + nds);
			nodeTot += nds.size();
			ele += nds.size();
			if (nds.size() > 0) {
				System.out.println("Non dense nodes!");
			}
			printProgress();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see crosby.binary.BinaryParser#parseWays(java.util.List)
		 */
		@Override
		protected void parseWays(List<crosby.binary.Osmformat.Way> ways_chunk) {
			for (crosby.binary.Osmformat.Way i : ways_chunk) {
				Way way = new Way(i.getId());
				for (int j = 0; j < i.getKeysCount(); j++) {
					String key = getStringById(i.getKeys(j));
					if (LegendParser.getRelevantKeys().contains(key)) {
						way.setAttribute(key, getStringById(i.getVals(j)));
					}
				}

				long last_id = 0;
				for (long j : i.getRefsList()) {
					long ref = j + last_id;
					Node node = nodes.get(new Long(ref));
					if (node != null) {
						way.addNode(node);
					} else {
						// Nodes for this way are missing, problem in OSM or simply
						// out of bounding box.
						// Three different cases are possible:
						// missing at the start, in the middle or at the end.
						// We simply add the current way and start a new one
						// with shared attributes.
						// Degenerate ways are not added, so don't care about
						// this here.
						if (way.getNodeCount() != 0) {

							Way tmp_way = new Way(way);
							addWay(way);
							way = tmp_way;
						}
					}
					last_id = ref;
				}
				addWay(way);
				if (way.isArea()) {
					way.saveOutline();
				}
			}
			wayTot += ways.size();
			ele += ways.size();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see crosby.binary.BinaryParser#parseRelations(java.util.List)
		 */
		@Override
		protected void parseRelations(
				List<crosby.binary.Osmformat.Relation> rels) {

			for (Osmformat.Relation i : rels) {
				Relation r = new Relation(i.getId());
				for (int j = 0; j < i.getKeysCount(); j++) {
					String key = getStringById(i.getKeys(j));
					if (LegendParser.getRelevantKeys().contains(key)) {
						r.setAttribute(key, getStringById(i.getVals(j)));
					}
				}
				long last_mid = 0;
				for (int j = 0; j < i.getMemidsCount(); j++) {
					long mid = last_mid + i.getMemids(j);
					last_mid = mid;
					String role = getStringById(i.getRolesSid(j));
					Member m = null;
					if (i.getTypes(j) == Osmformat.Relation.MemberType.NODE) {
						m = new Member("node", mid, role);
						if (!nodes.containsKey(new Long(mid))) {
							r.setPartial();
							continue;
						}
					} else if (i.getTypes(j) == Osmformat.Relation.MemberType.WAY) {
						m = new Member("way", mid, role);
						if (!ways.containsKey(new Long(mid))) {
							r.setPartial();
							continue;
						}
					} else if (i.getTypes(j) == Osmformat.Relation.MemberType.RELATION) {
						m = new Member("relation", mid, role);
						;
						if (m.getRef() > r.id) {
							// We haven't parsed this relation yet, so
							// we have to assume it is valid for the moment
						} else {
							if (!relations.containsKey(new Long(m.getRef()))) {
								r.setPartial();
								continue;
							}
						}
					} else {
						assert false; // TODO; Illegal file?
					}
					r.add(m);
				}

				long viaNodeOrWayRef = 0;
				if (r.isValid()) {
					if (!r.isPartial()) {
						relIns++;
						viaNodeOrWayRef = r.getViaNodeOrWayRef();
					} else {
						relPart++;
					}
					if (viaNodeOrWayRef != 0) {
						TurnRestriction turnRestriction = new TurnRestriction(r);
						if (r.isViaWay()) {
							// Store the ref to the via way
							turnRestriction.viaWayRef = viaNodeOrWayRef;
							// add a flag to the turn restriction if it's a way
							turnRestriction.flags |= TurnRestriction.VIA_TYPE_IS_WAY;
							// add restrictions with viaWays into an ArrayList
							// to be resolved later
							turnRestrictionsWithViaWays.add(turnRestriction);
						} else { // remember normal turn restrictions now
									// because we already know the via node
							addTurnRestriction(viaNodeOrWayRef, turnRestriction);
						}
					} else {
						relations.put(r.id, r);
					}
				}
			}
			relTot += rels.size();
			ele += rels.size();
			printProgress();
		}

		private void printProgress() {

			if (ele > 1000000) {
				ele = 0;
				System.out.println("Nodes " + nodeTot + "/" + nodeIns
						+ ", Ways " + wayTot + "/" + wayIns + ", Relations "
						+ relTot + "/" + relPart + "/" + relIns);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see crosby.binary.file.BlockReaderAdapter#complete()
		 */
		@Override
		public void complete() {
			System.out.println("End of document");
		}
	}

	/**
	 * The current processed primitive
	 */
	protected Entity current = null;

	protected int nodeTot, nodeIns;
	protected int wayTot;
	protected int ele;
	protected int relTot, relPart, relIns;
	private long startTime;

	private Node previousNodeWithThisId;

	public OpbfParser(InputStream i) {
		super(i);

	}

	public OpbfParser(InputStream i, Configuration c) {
		super(i, c);

	}

	@Override
	protected String parserType() {
		return "Osm Pbf";
	}

	@Override
	protected void init(InputStream i) {
		try {
			startTime = System.currentTimeMillis();
			BlockInputStream bis = new BlockInputStream(i, new OsmPbfHandler());
			System.out.println("Start of Document");
			System.out
					.println("Nodes read/used, Ways read/used, Relations read/partial/used");
			bis.process();
			System.out.println("Nodes " + nodeTot + "/" + nodeIns + ", Ways "
					+ wayTot + "/" + wayIns + ", Relations " + relTot + "/"
					+ relPart + "/" + relIns);
			printMemoryUsage(2);
			System.out.println("Parsing took "
					+ (System.currentTimeMillis() - startTime) / 1000 + "s");
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			e.printStackTrace();
			/*
			 * The planet file is presumably corrupt. So there is no point in
			 * continuing, as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		} catch (Exception e) {
			System.out.println("Other Exception: " + e);
			e.printStackTrace();
			/*
			 * The planet file is presumably corrupt. So there is no point in
			 * continuing, as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		}
	}
}
