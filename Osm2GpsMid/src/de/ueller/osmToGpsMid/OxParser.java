package de.ueller.osmToGpsMid;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Member;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Relation;
import de.ueller.osmToGpsMid.model.Way;

public class OxParser extends DefaultHandler{
	/**
	 * The current processed primitive
	 */
	private Entity current = null;
	/**
	 * Maps id to already read nodes.
	 * Key: Long   Value: Node
	 */
	private HashMap<Long,Node> nodes = new HashMap<Long,Node>(80000,0.60f);
	private HashMap<Long,Way> ways = new HashMap<Long,Way>();
	private HashMap<Long,Relation> relations = new HashMap<Long,Relation>();
	private Hashtable<String, String> tagsCache = new Hashtable<String,String>();
	private int nodeTot,nodeIns,segTot,segIns,wayTot,wayIns,ele, relTot, relPart, relIns;
	private Bounds[] bounds=null;
	private Configuration configuration;
	/**
	 * Keep track of ways that get split, as at the time of splitting
	 * not all tags have been added. So need to add them to all duplicates
	 */
	private LinkedList<Way> dupplicateWays;

	public OxParser(InputStream i) {
		System.out.println("OSM XML parser started...");
		configuration=new Configuration();
		init(i);
	}

	private void init(InputStream i) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			// Parse the input
			factory.setValidating(false);
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse( i, this);
			//parse(new InputStreamReader(new BufferedInputStream(i,10240), "UTF-8"));
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			e.printStackTrace();
			/* The planet file is presumably corrupt. So there is no point in continuing,
			 * as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		} catch (SAXException e) {
			System.out.println("SAXException: " + e);
			e.printStackTrace();
			/* The planet file is presumably corrupt. So there is no point in continuing,
			 * as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		} catch (Exception e) {
			System.out.println("Other Exception: " + e);
			e.printStackTrace();
			/* The planet file is presumably corrupt. So there is no point in continuing,
			 * as it will most likely generate incorrect map data.
			 */
			System.exit(10);
		}
	}

	/**
	 * @param i
	 * @param bounds
	 */
	public OxParser(InputStream i, Configuration c) {
		this.configuration = c;
		this.bounds = c.getBounds();
		System.out.println("OSM XML parser with bounds started...");
		init(i);
	}

	public void startDocument() {
		System.out.println("Start of Document");
	}

	public void endDocument() {
		System.out.println("node "+ nodeTot+"/"+nodeIns + "  way "+ wayTot+"/"+wayIns + " relations " + relTot + "/" +relPart + "/" + relIns);	
		System.out.println("End of Document");
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {		
//		System.out.println("start " + localName + " " + qName);
		if (qName.equals("node")) {
			float node_lat = Float.parseFloat(atts.getValue("lat"));
			float node_lon = Float.parseFloat(atts.getValue("lon"));
			
			long id = Long.parseLong(atts.getValue("id"));
			current = new Node(node_lat, node_lon, id);
			
		}

		if (qName.equals("way")) {
			long id = Long.parseLong(atts.getValue("id"));
			current = new Way(id);
			((Way)current).used=true;
		}
		
		if (qName.equals("nd")) {
			if (current instanceof Way) {
				Way way = ((Way)current);
				long ref = Long.parseLong(atts.getValue("ref"));
				Node node = nodes.get(new Long(ref));
				if (node != null){
					way.add(node);
				} else {
					// Node for this Way is missing, problem in OS or simply out of Bounding Box
					// tree different cases are possible
					// missing at the start, in the middle or at the end
					// we simply add the current way and start a new one with shared attributes.
					// degenerate ways are not added, so don't care about this here.
					if (way.path != null){
						/**
						 * Attributes might not be fully known yet, so keep
						 * track of which ways are duplicates and clone
						 * the tags once the XML for this way is fully parsed
						 */
						if (dupplicateWays == null)
							dupplicateWays = new LinkedList<Way>();
						dupplicateWays.add(way);
						current = new Way(way);
					}
				}
			}
		}
		
		if(qName.equals("tag")) {
			if (current != null) {				
				String key = atts.getValue("k");
				String val = atts.getValue("v");
				/**
				 * atts.getValue creates a new String every time
				 * If we store key and val directly in current.setAttribute
				 * we end up having thousands of duplicate Strings for e.g.
				 * "name" and "highway". By filtering it through a Hashtable
				 * we can reuse the String objects thereby saving a significant
				 * amount of memory.
				 */
				if (key != null && val != null ){
					/**
					 * Filter out common tags that are definately not used such as created_by
					 * If this is the only tag on a Node, we end up saving creating a Hashtable
					 * object to store the tags, saving some memory.
					 */
					if (!key.equalsIgnoreCase("created_by") && !key.equalsIgnoreCase("converted_by")) {
						if (!tagsCache.containsKey(key)) {
							tagsCache.put(key, key);
						}
						if (!tagsCache.containsKey(val)) {
							tagsCache.put(val, val);
						}
						current.setAttribute(tagsCache.get(key), tagsCache.get(val));
					}
				}				
			} else {
				System.out.println("tag at unexepted position " + current);
			}
		}
		if (qName.equals("relation")) {
			long id = Long.parseLong(atts.getValue("id"));
			current=new Relation(id);
		}
		if (qName.equals("member")) {
			if (current instanceof Relation) {
				Relation r=(Relation)current;
				Member m=new Member(atts.getValue("type"),atts.getValue("ref"),atts.getValue("role"));
				switch(m.getType()) {
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
						//We haven't parsed this relation yet, so
						//we have to assume it is valid for the moment
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

	public void endElement(String namespaceURI, String localName, String qName) {		
//		System.out.println("end  " + localName + " " + qName);
		ele++;
		if (ele > 1000000){
			ele=0;
			System.out.println("node "+ nodeTot+"/"+nodeIns + "  way "+ wayTot+"/"+wayIns + " relations " + relTot + "/" +relPart + "/" + relIns);
		}
		if (qName.equals("node")) {
			Node n=(Node) current;
			boolean inBound=false;
			nodeTot++;
			if (bounds != null && bounds.length != 0){
				for (int i=0;i<bounds.length;i++){
					if (bounds[i].isIn(n.lat, n.lon)){
						inBound=true;
						break;
					}
				}
			} else {
				inBound=true;
			}

			if (inBound){
				nodes.put(new Long(current.id), (Node) current);
				nodeIns++;
			}
			current = null;
		} else if (qName.equals("way")) {
			wayTot++;
			Way w= (Way) current;
			if (dupplicateWays != null) {
				for (Way ww : dupplicateWays) {
					ww.cloneTags(w);
					addWay(ww);
				}
				dupplicateWays = null;
			}
			addWay(w);
			

			current = null;
		} else if (qName.equals("relation")) {
			relTot++;
			Relation r=(Relation) current;
			if (r.isValid()) {				
				if (!r.isPartial()) {
					relIns++;										
				} else {
					relPart++;
				}
				relations.put(new Long(r.id),r);
			}
			
			current=null;
		}
	} // endElement

	/**
	 * @param w
	 */
	public void addWay(Way w) {
		byte t=w.getType(configuration);
		/**
		 * We seem to have a bit of a mess with respect to type -1 and 0.
		 * Both are used to indicate invalid type it seems.
		 */
		if (w.isValid() && t > 0){
			if (ways.get(new Long(w.id)) != null) {
				/**
				 * This way is already in datastrorage.
				 * This results from splitting a single
				 * osm way into severals GpsMid ways.
				 * We can simply invent an id in this
				 * case, as we currently don't use them
				 * for anything other than checking if
				 * an id is valid for use in relations
				 */				
				ways.put(new Long(-1*wayIns),w);
			} else {
				ways.put(new Long(w.id),w);
			}
			wayIns++;
		}
	}

	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println("Error: " + e);
		throw e;
	}

	public Collection<Node> getNodes() {
		return nodes.values();
	}


	public Collection<Way> getWays() {
		return ways.values();
	}
	
	public Collection<Relation> getRelations() {
		return relations.values();
	}
	
	public void removeNode(long id) {
		nodes.remove(new Long(id));
	}
	
	/**
	 * 
	 */
	public void resize() {
		System.gc();
		System.out.println(Runtime.getRuntime().freeMemory());
		nodes=new HashMap<Long, Node>(nodes);
		System.gc();
		System.out.println(Runtime.getRuntime().freeMemory());
	}
}
