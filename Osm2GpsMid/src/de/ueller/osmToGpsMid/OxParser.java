/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2010 Kai Krueger
 */
package de.ueller.osmToGpsMid;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.TurnRestriction;
import de.ueller.osmToGpsMid.model.Way;

public class OxParser extends OsmParser {

	private class OsmXmlHandler extends DefaultHandler  {
		private final Hashtable<String, String> tagsCache = new Hashtable<String, String>();

		@Override
		public void startDocument() {
			System.out.println("Start of Document");
			System.out
					.println("Nodes read/used, Ways read/used, Relations read/partial/used");
		}

		@Override
		public void endDocument() {
			long time = (System.currentTimeMillis() - startTime) / 1000;

			System.out.println("Nodes " + nodeTot + "/" + nodeIns + ", Ways "
					+ wayTot + "/" + wayIns + ", Relations " + relTot + "/"
					+ relPart + "/" + relIns + ",  Read " + (forwardInputStream.lBytesRead/1000) + " KiB");
			printMemoryUsage(2);
			System.out.println("End of document, reading took " + time
					+ " seconds");
		}

		@Override
		public void startElement(String namespaceURI, String localName,
				String qName, Attributes atts) {
			// System.out.println("start " + localName + " " + qName);
			if (qName.equals("node")) {
				nodeTot++;
				float node_lat = Float.parseFloat(atts.getValue("lat"));
				float node_lon = Float.parseFloat(atts.getValue("lon"));

				if (nodeInArea(node_lat, node_lon)) {
					long id = Long.parseLong(atts.getValue("id"));
					current = new Node(node_lat, node_lon, id);
				} else {
					current = null;
				}
			}
			if (qName.equals("way")) {
				long id = Long.parseLong(atts.getValue("id"));
				current = new Way(id);
				((Way) current).used = true;
			}
			if (qName.equals("nd")) {
				if (current instanceof Way) {
					Way way = ((Way) current);
					long ref = Long.parseLong(atts.getValue("ref"));
					Node node = nodes.get(new Long(ref));
					if (node != null) {
						way.addNode(node);
					} else {
						// Node for this Way is missing, problem in OS or simply
						// out of Bounding Box
						// tree different cases are possible
						// missing at the start, in the middle or at the end
						// we simply add the current way and start a new one
						// with shared attributes.
						// degenerate ways are not added, so don't care about
						// this here.
						if (way.getNodeCount() != 0) {
							/**
							 * Attributes might not be fully known yet, so keep
							 * track of which ways are duplicates and clone the
							 * tags once the XML for this way is fully parsed
							 */
							if (duplicateWays == null) {
								duplicateWays = new LinkedList<Way>();
							}
							duplicateWays.add(way);
							current = new Way(way);
						}
					}
				}
			}
			if (qName.equals("tag")) {
				if (current != null) {
					String key = atts.getValue("k");
					String val = atts.getValue("v");
					/**
					 * atts.getValue creates a new String every time If we store
					 * key and val directly in current.setAttribute we end up
					 * having thousands of duplicate Strings for e.g. "name" and
					 * "highway". By filtering it through a Hashtable we can
					 * reuse the String objects thereby saving a significant
					 * amount of memory.
					 */
					if (key != null && val != null) {
						/**
						 * Filter out common tags that are definitely not used
						 * such as created_by If this is the only tag on a Node,
						 * we end up saving creating a Hashtable object to store
						 * the tags, saving some memory.
						 */
						if (LegendParser.getRelevantKeys().contains(key)) {
							if (!tagsCache.containsKey(key)) {
								tagsCache.put(key, key);
							}
							if (!tagsCache.containsKey(val)) {
								tagsCache.put(val, val);
							}
							current.setAttribute(tagsCache.get(key), tagsCache
									.get(val));
						}
					}
				}
			}
			if (qName.equals("relation")) {
				long id = Long.parseLong(atts.getValue("id"));
				current = new Relation(id);
			}
			if (qName.equals("member")) {
				if (current instanceof Relation) {
					Relation r = (Relation) current;
					Member m = new Member(atts.getValue("type"), atts
							.getValue("ref"), atts.getValue("role"));
					switch (m.getType()) {
					case Member.TYPE_NODE: {
						if (!nodes.containsKey(new Long(m.getRef()))) {
							r.setPartial();
							return;
						}
						break;
					}
					case Member.TYPE_WAY: {
						if (!ways.containsKey(new Long(m.getRef()))) {
							r.setPartial();
							return;
						}
						break;
					}
					case Member.TYPE_RELATION: {
						if (m.getRef() > r.id) {
							// We haven't parsed this relation yet, so
							// we have to assume it is valid for the moment
						} else {
							if (!relations.containsKey(new Long(m.getRef()))) {
								r.setPartial();
								return;
							}
						}
						break;
					}
					}
					r.add(m);
				}
			}

		} // startElement

		@Override
		public void endElement(String namespaceURI, String localName,
				String qName) {
			// System.out.println("end  " + localName + " " + qName);
			ele++;
			if (System.currentTimeMillis() - lLastStatusTime > 10000) {
				ele = 0;
				lLastStatusTime = System.currentTimeMillis();
				System.out.println("Nodes " + nodeTot + "/" + nodeIns
						+ ", Ways " + wayTot + "/" + wayIns + ", Relations "
						+ relTot + "/" + relPart + "/" + relIns + ",  Read " + (forwardInputStream.lBytesRead/1000) + " KiB");
			}
			if (qName.equals("node")) {
				if (current == null) {
					return; // Node not in bound
				}
				Node n = (Node) current;
				previousNodeWithThisId = nodes.put(n.id, n);
				nodeIns++;
				if (current.getAttribute("highway") != null
						&& current.getAttribute("highway").equalsIgnoreCase(
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
					n.markAsTrafficSignals();
					trafficSignalCount++;
				}
				current = null;
			}
			if (qName.equals("way")) {
				wayTot++;
				Way w = (Way) current;
				// TODO: this seems to be not useful, because the list of tags
				// is shared (only one list)
				// so a add of an attribute to one if the ways already adds it
				// to the
				// other as well.
				if (duplicateWays != null) {
					for (Way ww : duplicateWays) {
						ww.cloneTags(w);
						addWay(ww);
						if (ww.isArea()) {
							ww.saveOutline();
						}
					}
					duplicateWays = null;
				}
				addWay(w);
				if (w.isArea()) {
					w.saveOutline();
				}
				
				current = null;
			}
			if (qName.equals("relation")) {
				relTot++;
				/**
				 * Store way and node refs temporarily in the same variable Way
				 * refs must be resolved later to nodes when we actually know
				 * all the ways
				 */
				long viaNodeOrWayRef = 0;
				Relation r = (Relation) current;
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

				current = null;
			}
		} // endElement

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			System.out.println("Error: " + e);
			throw e;
		}
	}

	/**
	 *  This class represents an input stream which counts the bytes read.
	 */
	class ForwardInputStream  extends InputStream
	{
		InputStream inputStream = null;
		long lBytesRead = 0;

		public ForwardInputStream(InputStream inputStream)
		{
			this.inputStream = inputStream;
		}

		@Override
		public int available()
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public void close() throws IOException
		{
			inputStream.close();
		}

		@Override
		public void mark(int readlimit)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public boolean markSupported()
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public int read() throws IOException
		{
			int readByte = inputStream.read();
			if ( readByte != -1 )
				lBytesRead++;
			return readByte;
		}

		@Override
		public int read(byte[] b) throws IOException
		{
			int iNumReadBytes = inputStream.read(b);
			if ( iNumReadBytes != -1 )
				lBytesRead+=iNumReadBytes;
			return iNumReadBytes;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException
		{
			int iNumReadBytes = inputStream.read(b, off, len);
			if ( iNumReadBytes != -1 )
				lBytesRead+=iNumReadBytes;
			return iNumReadBytes;
		}

		@Override
		public  void reset()
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public long skip(long n)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}


	/**
	 * The current processed primitive
	 */
	private Entity current = null;
	private int nodeTot, nodeIns;
	private int wayTot, wayIns;
	private int ele;
	private int relTot, relPart, relIns;
	/**
	 * Keep track of ways that get split, as at the time of splitting not all
	 * tags have been added. So need to add them to all duplicates.
	 */
	private LinkedList<Way> duplicateWays;
	private long startTime;

	private Node previousNodeWithThisId;

	private ForwardInputStream forwardInputStream = null;
	private long lLastStatusTime = 0;

	/**
	 * @param i
	 *            InputStream from which planet file is read
	 */
	public OxParser(InputStream i) {
		super(i);
	}

	/**
	 * @param i
	 *            InputStream from which planet file is read
	 * @param c
	 *            Configuration which supplies the bounds
	 */
	public OxParser(InputStream i, Configuration c) {
		super(i, c);
	}

	@Override
	protected String parserType() {
		return "Osm XML";
	}

	@Override
	protected void init(InputStream i) {
		try {
			startTime = System.currentTimeMillis();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			// Parse the input
			factory.setValidating(false);
			SAXParser saxParser = factory.newSAXParser();
			lLastStatusTime = System.currentTimeMillis();
			forwardInputStream = new ForwardInputStream(i);
			saxParser.parse(forwardInputStream, new OsmXmlHandler());
			// parse(new InputStreamReader(new BufferedInputStream(i,10240),
			// "UTF-8"));
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			e.printStackTrace();
			/*
			 * The planet file is presumably corrupt. So there is no point in
			 * continuing, as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		} catch (SAXException e) {
			System.out.println("SAXException: " + e);
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
