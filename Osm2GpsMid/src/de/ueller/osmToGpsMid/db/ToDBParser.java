/**
 * OSM2GpsMid
 * 
 * @version $Revision$ ($Name$) Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.EntityDescription;
import de.ueller.osmToGpsMid.model.WayDescription;

// import de.ueller.osmToGpsMid.model.Entity;
// import de.ueller.osmToGpsMid.model.Member;
// import de.ueller.osmToGpsMid.model.Node;
// import de.ueller.osmToGpsMid.model.Relation;
// import de.ueller.osmToGpsMid.model.TurnRestriction;
// import de.ueller.osmToGpsMid.model.Way;

/**
 * @author hmueller
 */
public class ToDBParser extends DefaultHandler {
	private Vector<Bounds>	bounds	= null;
	private Configuration	configuration;
	private long			startTime;
	private int				ele;
	private int				nodeTot;
	private int				nodeIns;
	private int				wayTot;
	private int				wayIns;
	private int				relTot;
	private int				relPart;
	private int				relIns;
	private int				tagsCreated;
	private Entity			current	= null;
	private long			nextOutput;
	private LinkedList<Way>	duplicateWays;

	private EntityManager	em;
	EntityManagerFactory	factory;
	private int				trafficSignalCount;
	private long			commitDelay=7500;
	private long			lastStart;
	private boolean			noIndex=true;
	/**
	 * @param i
	 *            InputStream from which planet file is read
	 */
	public ToDBParser(InputStream i) {
		System.out.println("OSM XML parser started...");
		configuration = new Configuration();
		// this.em=em;
		init(i);
	}

	/**
	 * @param i
	 *            InputStream from which planet file is read
	 * @param c
	 *            Configuration which supplies the bounds
	 */
	public ToDBParser(EntityManagerFactory x, InputStream i, Configuration c) {
		this.configuration = c;
		this.bounds = c.getBounds();
		this.factory = x;
		System.out.println("OSM XML parser with bounds started...");
		init(i);
	}

	private void init(InputStream i) {
		try {
			startTime = System.currentTimeMillis();
			lastStart = startTime;
			// factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
			// em = factory.createEntityManager();

			SAXParserFactory factory = SAXParserFactory.newInstance();
			// Parse the input
			factory.setValidating(false);
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(i, this);
			// parse(new InputStreamReader(new BufferedInputStream(i,10240), "UTF-8"));
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			e.printStackTrace();
			/*
			 * The planet file is presumably corrupt. So there is no point in continuing, as it will most likely
			 * generate incorrect map data.
			 */
			System.exit(10);
		} catch (SAXException e) {
			System.out.println("SAXException: " + e);
			e.printStackTrace();
			/*
			 * The planet file is presumably corrupt. So there is no point in continuing, as it will most likely
			 * generate incorrect map data.
			 */
			System.exit(10);
		} catch (Exception e) {
			System.out.println("Other Exception: " + e);
			e.printStackTrace();
			/*
			 * The planet file is presumably corrupt. So there is no point in continuing, as it will most likely
			 * generate incorrect map data.
			 */
			System.exit(10);
		}
	}

	public void startDocument() {
		System.out.println("Start of Document");
		System.out.println("Nodes read/used, Ways read/used, Relations read/partial/used");
		em = factory.createEntityManager();
		em.getTransaction().begin();
		nextOutput = System.currentTimeMillis() + commitDelay;
	}

	public void endDocument() {

		long time = (System.currentTimeMillis() - startTime) / 60000;
		System.out.println("Nodes " + nodeTot + "/" + nodeIns + ", Ways " + wayTot + "/" + wayIns + ", Relations " + relTot + "/" + relPart
				+ "/" + relIns);
		em.getTransaction().commit();
		em.close();
		System.out.println("End of document, reading took " + time + " seconds");
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
		// System.out.println("start " + localName + " " + qName);
		if (qName.equals("node")) {
			float node_lat = Float.parseFloat(atts.getValue("lat"));
			float node_lon = Float.parseFloat(atts.getValue("lon"));

			long id = Long.parseLong(atts.getValue("id"));
			Node node = new Node(node_lat, node_lon, id);
			current = node;

		}
		if (qName.equals("way")) {
			long id = Long.parseLong(atts.getValue("id"));
			Way way = new Way(id);
			current = way;
			current.setUsed(true);
		}
		if (qName.equals("nd")) {
			if (current instanceof Way) {
				Way way = ((Way) current);
				long ref = Long.parseLong(atts.getValue("ref"));
				Node node = getNodeByOsmId(ref);
				if (node != null) {
					way.add(node);
				} else {
					// Node for this Way is missing, problem in OS or simply out of Bounding Box
					// tree different cases are possible
					// missing at the start, in the middle or at the end
					// we simply add the current way and start a new one with shared attributes.
					// degenerate ways are not added, so don't care about this here.
					if (!way.getNodeList().isEmpty()) {
						/**
						 * Attributes might not be fully known yet, so keep track of which ways are duplicates and clone
						 * the tags once the XML for this way is fully parsed
						 */
						if (duplicateWays == null) {
							duplicateWays = new LinkedList<Way>();
						}
						duplicateWays.add(way);
						current = new Way(way.getOsmId());
					}
				}
			}
		}
		if (qName.equals("tag")) {
			if (current != null) {
				String key = atts.getValue("k");
				String val = atts.getValue("v");
				/**
				 * atts.getValue creates a new String every time If we store key and val directly in
				 * current.setAttribute we end up having thousands of duplicate Strings for e.g. "name" and "highway".
				 * By filtering it through a Hashtable we can reuse the String objects thereby saving a significant
				 * amount of memory.
				 */
				if (key != null && val != null) {
					/**
					 * Filter out common tags that are definitely not used such as created_by If this is the only tag on
					 * a Node, we end up saving creating a Hashtable object to store the tags, saving some memory.
					 */
					if (!key.equalsIgnoreCase("created_by") && !key.equalsIgnoreCase("converted_by") && !key.equalsIgnoreCase("source")
							&& !key.startsWith("tiger") && !key.equalsIgnoreCase("attribution") && !key.equalsIgnoreCase("note")) {
						Tag addTag = current.addTag(key, val);
//						em.persist(addTag);
						tagsCreated++;
					}
				}
			} else {
				System.out.println("Tag at unexpected position " + current);
			}
		}
		if (qName.equals("relation")) {
			long id = Long.parseLong(atts.getValue("id"));
			current = new Relation(id);
		}
		if (qName.equals("member")) {
			if (current instanceof Relation) {
				Relation r = (Relation) current;
				Member m = new Member(atts.getValue("type"), atts.getValue("ref"), atts.getValue("role"));
				switch (m.getType()) {
					case Member.TYPE_NODE: {
						Node n = getNodeByOsmId(new Long(m.getRef()));
//						System.out.println("Rel got Node " + n);
						if (n == null) {
							r.setPartial();
							return;
						}
						break;
					}
					case Member.TYPE_WAY: {
						Way w = getWayByOsmId(new Long(m.getRef()));
//						System.out.println("Rel got Way " + w);
						if (w == null) {
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
							Relation re = getRelationByOsmId(new Long(m.getRef()));
							if (re == null) {
								r.setPartial();
								return;
							}
						}
						break;
					}
				}
				r.add(m);
//				em.persist(m);
			}
		}

	} // startElement

	/**
	 * @param osmId
	 * @return
	 */
	private Node getNodeByOsmId(Long osmId) {
//		Query q = em.createNamedQuery("findNodByOsmId");
////		Query q = em.createQuery("SELECT n FROM Node n WHERE n.osmId = :id");
//		q.setParameter("id", osmId);
//		try {
//			return (Node) q.getSingleResult();
//		} catch (javax.persistence.NoResultException e) {
//			return null;
//		}
		try {
			return em.find(Node.class, osmId);
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * @param osmId
	 * @return
	 */
	private Way getWayByOsmId(Long osmId) {
//		Query q = em.createNamedQuery("findWayByOsmId");
//		q.setParameter("id", osmId);
//		try {
//			List list = q.getResultList();
//			if (list.size() > 1){
//				System.out.println("warning: got "+ list.size() + " entries for wayByOsmId");
//			}
//			return (Way) q.getSingleResult();
//		} catch (javax.persistence.NoResultException e) {
//			return null;
//		}
		try {
			return em.find(Way.class, osmId);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @param osmId
	 * @return
	 */
	private Relation getRelationByOsmId(Long osmId) {
//		Query q = em.createNamedQuery("findRelByOsmId");
//		q.setParameter("id", osmId);
//		try {
//			return (Relation) q.getSingleResult();
//		} catch (javax.persistence.NoResultException e) {
//			return null;
//		}
		try {
			return em.find(Relation.class, osmId);
		} catch (Exception e) {
			return null;
		}

	}

	public void endElement(String namespaceURI, String localName, String qName) {
		// System.out.println("end  " + localName + " " + qName);

		// ele++;
		boolean canCommit = false;
		if (qName.equals("node")) {
			Node n = (Node) current;
			boolean inBound = false;
			nodeTot++;
			if (bounds != null && bounds.size() != 0) {
				for (Bounds b : bounds) {
					if (b.isIn(n.getLat(), n.getLon())) {
						inBound = true;
						break;
					}
				}
			} else {
				inBound = true;
			}

			if (inBound) {
				// nodes.put(new Long(current.id), (Node) current);
				nodeIns++;
				String tagH = getTag(current, "highway");
				if (tagH != null && tagH.equalsIgnoreCase("traffic_signals")) {
					n.setTrafficSignal(true);
					trafficSignalCount++;
				}
				EntityDescription poi = n.calcType(configuration.getPOIlegend());
				if (poi != null) {
					n.setType(poi.typeNum);
				}
				for (Tag t:n.getTagList()){
					em.persist(t);
				}
				em.persist(n);
				// System.out.println("persist node " + n.getOsmId()+ " " + n.getId());
			}
			canCommit = true;
			current = null;
		}
		if (qName.equals("way")) {
			wayTot++;
			Way w = (Way) current;
			WayDescription way = (WayDescription)w.calcType(configuration.getWayLegend());
			if (way != null){
				w.setType(way.typeNum);
			}
			if (duplicateWays != null) {
				for (Way ww : duplicateWays) {
					ww.cloneTags(w);
					em.persist(ww);
				}
				duplicateWays = null;
			}
			for (Tag t:w.getTagList()){
				em.persist(t);
			}
			em.persist(w);
			// System.out.println("persist way " + w.getOsmId()+ " " + w.getId());
			current = null;
			canCommit = true;
		}
		if (qName.equals("relation")) {
			relTot++;
			/**
			 * Store way and node refs temporarily in the same variable Way refs must be resolved later to nodes when we
			 * actually know all the ways
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
					System.out.println("Warning: a viaNodeOrWayRef");
					// TurnRestriction turnRestriction = new TurnRestriction(r);
					// if (r.isViaWay()) {
					// // Store the ref to the via way
					// turnRestriction.viaWayRef = viaNodeOrWayRef;
					// // add a flag to the turn restriction if it's a way
					// turnRestriction.flags |= TurnRestriction.VIA_TYPE_IS_WAY;
					// // add restrictions with viaWays into an ArrayList to be resolved later
					// turnRestrictionsWithViaWays.add(turnRestriction);
					// } else {
					// if (! turnRestrictions.containsKey(new Long(viaNodeOrWayRef))) {
					// turnRestrictions.put(new Long(viaNodeOrWayRef), turnRestriction);
					// } else {
					// turnRestriction = (TurnRestriction) turnRestrictions.get(new Long(viaNodeOrWayRef));
					// turnRestriction.nextTurnRestrictionAtThisNode = new TurnRestriction(r);
					// // System.out.println("Multiple turn restrictions at " + r.toString());
					// }
					// }
				} else {
					for (Member m:r.getMembers()){
						System.out.println("persist(" + m + ")");
						em.persist(m);
					}
					for (Tag t:r.getTagList()){
						em.persist(r);
					}
					System.out.println("persist(" + r + ")");
					em.persist(r);
					// relations.put(new Long(r.id),r);
				}
			} else {
				System.out.println("warning: Relation not valid");
			}
			canCommit = true;
			current = null;
		}
		if (canCommit) {
			if (nextOutput < System.currentTimeMillis()) {
				nextOutput = System.currentTimeMillis();
				System.out.println("Nodes " + nodeTot + "/" + nodeIns + ", Ways " + wayTot + "/" + wayIns + ", Relations " + relTot + "/"
						+ relPart + "/" + relIns + " tags:" + tagsCreated);
				em.getTransaction().commit();
				em.close();
				em = factory.createEntityManager();
//				if (wayTot > 0 && noIndex){
//					noIndex=false;
//					System.out.println("Create osm index ..");
//					em.getTransaction().begin();
//			        em.createNativeQuery("create index indexOsmId on entity (osmid)").executeUpdate();
//			        em.getTransaction().commit();
//					System.out.println("done");
//				}
				em.getTransaction().begin();
				long commitDuration = System.currentTimeMillis() - nextOutput;
				long duration = System.currentTimeMillis() - lastStart;
				float df=60000f/(float)duration;
				commitDelay=(long) ((0.9f*commitDelay)+(df*commitDelay*0.1f));
				System.out.println("Commit takes " + commitDuration + " ms wait " + commitDelay + " ("+df+") " + (System.currentTimeMillis()-lastStart));
				nextOutput = System.currentTimeMillis() + commitDelay;
				lastStart=System.currentTimeMillis();

			}
		}

	} // endElement

	private String getTag(Object c, String key) {
		if (c instanceof Node) { return ((Node) c).getTag(key); }
		if (c instanceof Way) { return ((Node) c).getTag(key); }
		System.err.println("Object " + c.getClass().getSimpleName() + " is not defined to have tags");
		return null;
	}
}
