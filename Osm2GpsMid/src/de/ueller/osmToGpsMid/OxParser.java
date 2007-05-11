package de.ueller.osmToGpsMid;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import uk.co.wilson.xml.MinML2;
import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Entity;
import de.ueller.osmToGpsMid.model.Line;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.Way;

public class OxParser extends MinML2 {
	/**
	 * The current processed primitive
	 */
	private Entity current = null;
	/**
	 * Maps id to already read nodes.
	 * Key: Long   Value: Node
	 */
	public Map<Long,Node> nodes = new HashMap<Long,Node>();
	/**
	 * Maps id to already read lines.
	 * Key: Long   Value: Line
	 */
	public Map<Long,Line> lines = new HashMap<Long,Line>();
	public Collection<Way> ways = new LinkedList<Way>();
	private int nodeTot,nodeIns,segTot,segIns,wayTot,wayIns,ele;
	private Bounds[] bounds=null;
	private Configuration configuration;

	public OxParser(InputStream i) {
		System.out.println("OSM XML parser started...");
		init(i);
	}

	private void init(InputStream i) {
		try {
			parse(new InputStreamReader(new BufferedInputStream(i, 1024), "UTF-8"));
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
	public OxParser(FileInputStream i, Configuration c) {
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
		if (qName.equals("node")) {
			float node_lat = Float.parseFloat(atts.getValue("lat"));
			float node_lon = Float.parseFloat(atts.getValue("lon"));
			
			long id = Long.parseLong(atts.getValue("id"));
			current = new Node(node_lat, node_lon, id);
			
		}

		if (qName.equals("segment")) {
			long line_from_id = Long.parseLong(atts.getValue("from"));
			long line_to_id = Long.parseLong(atts.getValue("to"));
			long id = Long.parseLong(atts.getValue("id"));
			current = new Line(nodes.get(new Long(line_from_id)),
					           nodes.get(new Long(line_to_id)),
					           id);
		}
		
		if (qName.equals("way")) {
			long id = Long.parseLong(atts.getValue("id"));
			current = new Way(id);
		}

		if (qName.equals("seg")) {
			long id = Long.parseLong(atts.getValue("id"));
			Line line = (Line)lines.get(new Long(id));
			if (line == null) {
//				line = new Line(id);
//				lines.put(new Long(id), line);
			} else {
				((Way)current).lines.add(line);
			}
		}

		if(qName.equals("tag")) {
			String key = atts.getValue("k");
			String val = atts.getValue("v");
			current.tags.put(key, val);
		}

	} // startElement

	public void endElement(String namespaceURI, String localName, String qName) {
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
		} else if (qName.equals("segment")) {
			Line l=(Line) current;
			segTot++;
			try {
				if (l.from != null && l.to != null){
					segIns++;
					lines.put(new Long(current.id), (Line) current);;
				}
			} catch (RuntimeException e) {
				System.out.println("should never hapen");
			}
			current = null;
		} else if (qName.equals("way")) {
			wayTot++;
			Way w= (Way) current;
			byte t=w.getType(configuration);
			if (w.lines.size() > 0 && t > 0){
				ways.add((Way) current);
				wayIns++;
			}
			current = null;
		}


	} // endElement

	public void fatalError(SAXParseException e) throws SAXException {
		System.out.println("Error: " + e);
		throw e;
	}

	public Collection getNodes() {
		return nodes.values();
	}

	public Collection getLines() {
		return lines.values();
	}
	
	public Collection getWays() {
		return ways;
	}
}
