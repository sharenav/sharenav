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
	public HashMap<Long,Node> nodes = new HashMap<Long,Node>(80000,0.60f);
	public LinkedList<Way> ways = new LinkedList<Way>();
	public LinkedList<Relation> relations = new LinkedList<Relation>();
	private Hashtable<String, String> tagsCache = new Hashtable<String,String>();
	private int nodeTot,nodeIns,segTot,segIns,wayTot,wayIns,ele;
	private Bounds[] bounds=null;
	private Configuration configuration;

	public OxParser(InputStream i) {
		System.out.println("OSM XML parser started...");
		configuration=new Configuration();
		init(i);
	}

	private void init(InputStream i) {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			// Parse the input
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse( i, this);
			//parse(new InputStreamReader(new BufferedInputStream(i,10240), "UTF-8"));
		} catch (IOException e) {
			System.out.println("IOException: " + e);
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("SAXException: " + e);
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Other Exception: " + e);
			e.printStackTrace();
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
					way.startNextSegment();
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
			current=new Relation();
		}
		if (qName.equals("member")) {
			if (current instanceof Relation) {
				Relation r=(Relation)current;
				Member m=new Member(atts.getValue("type"),atts.getValue("ref"),atts.getValue("role"));
				r.add(m);
			}
		}

	} // startElement

	public void endElement(String namespaceURI, String localName, String qName) {		
//		System.out.println("end  " + localName + " " + qName);
		ele++;
		if (ele > 1000000){
			ele=0;
			System.out.println("node "+ nodeTot+"/"+nodeIns + "  seg "+ segTot+"/"+segIns + "  way "+ wayTot+"/"+wayIns);
		}
		if (qName.equals("node")) {
			Node n=(Node) current;
			boolean inBound=false;
			nodeTot++;
			if (bounds != null){
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
			byte t=w.getType(configuration);
			if (w.isValid() && t != 0){
				ways.add(w);
				wayIns++;
			}

			current = null;
		} else if (qName.equals("relation")) {
			Relation r=(Relation) current;
			System.out.println("got " + r);
			relations.add(r);
			current=null;
		}
	} // endElement

	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println("Error: " + e);
		throw e;
	}

	public Collection<Node> getNodes() {
		return nodes.values();
	}


	public Collection<Way> getWays() {
		return ways;
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
