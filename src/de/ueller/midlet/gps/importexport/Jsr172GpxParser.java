//#if polish.api.webservice
package de.ueller.midlet.gps.importexport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.ImageCollector;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.Gpx;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.ProjMath;

public class Jsr172GpxParser extends DefaultHandler implements GpxParser {
	
	/**
	 * This class is close to a duplicate of MinML2GpxParser.
	 * If there is a more elegant way than to duplicate all
	 * code, please fix this.
	 * 
	 * The current reason for the duplication is the need to have
	 * to separate classes, that can be called via Class.forName()
	 * in order to guard jsr172 access
	 */
	
	
	private final static Logger logger=Logger.getInstance(MinML2GpxParser.class,Logger.DEBUG);
	
	private PositionMark wayPt;
	private Position p = new Position(0,0,0,0,0,0,new Date());
	private boolean name = false;
	private boolean ele = false;	
	private boolean time = false;
	private float maxDistance;
	private int importedWpts;
	private int importedTpts;
	private int tooFarWpts;
	private int duplicateWpts;
	
	private Gpx gpx;
	
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
		if (qName.equalsIgnoreCase("wpt")) {
			/**
			 * Gpx files have coordinates in degrees. We store them internally as radians.
			 * So we need to convert these.
			 */
			float node_lat = Float.parseFloat(atts.getValue("lat"))*MoreMath.FAC_DECTORAD;
			float node_lon = Float.parseFloat(atts.getValue("lon"))*MoreMath.FAC_DECTORAD;
			float distance=0;
			
			boolean inRadius=true;
			if (maxDistance!=0) {
				distance = ProjMath.getDistance(node_lat, node_lon, ImageCollector.mapCenter.radlat, ImageCollector.mapCenter.radlon); 
				distance=(int)(distance/100.0f)/10.0f;
				if (distance>maxDistance) {
					inRadius=false;
					tooFarWpts++;
				}
			}
			//System.out.println("MaxDist: " + maxDistance + " Distance: " + distance + " inRadius: " + inRadius);
			if (inRadius) {
				wayPt = new PositionMark(node_lat,node_lon);
			}
		} else if (qName.equalsIgnoreCase("name")) {
			name = true;				
		} else if (qName.equalsIgnoreCase("trk")) {
			gpx.newTrk();
		} else if (qName.equalsIgnoreCase("trkseg")) {
			
		} else if (qName.equalsIgnoreCase("trkpt")) {
			/**
			 * Positions seem to be handeled in degree rather than radians
			 * as all the other coordinates.
			 * Be careful with the conversions!
			 */
			p.latitude = Float.parseFloat(atts.getValue("lat"));
			p.longitude = Float.parseFloat(atts.getValue("lon"));
			p.altitude = 0;
			p.course = 0;
			p.speed = 0;				
		} else if (qName.equalsIgnoreCase("ele")) {
			ele = true;
		} else if (qName.equalsIgnoreCase("time")) {
			time = true;
		}
	}
	public void endElement(String namespaceURI, String localName, String qName) {
		if (qName.equalsIgnoreCase("wpt")) {
			if (wayPt != null) {
				//#debug info
				logger.info("Received waypoint: " + wayPt);
				if (!gpx.existsWayPt(wayPt)) {
					gpx.addWayPt(wayPt);
					importedWpts++;
				}
				else {
					duplicateWpts++;
				}
				wayPt = null;
			}
							
		} else if (qName.equalsIgnoreCase("name")) {
			name = false;
		} else if (qName.equalsIgnoreCase("trk")) {
			gpx.saveTrk();				
		} else if (qName.equalsIgnoreCase("trkseg")) {
			
		} else if (qName.equalsIgnoreCase("trkpt")) {
			gpx.addTrkPt(p);
			importedTpts++;
		} else if (qName.equalsIgnoreCase("ele")) {
			ele = false;
		} else if (qName.equalsIgnoreCase("time")) {
			time = false;
		}
	}
	public void startDocument() {
		//#debug debug
		logger.debug("Started parsing XML document");
	}
	public void endDocument() {
		//#debug debug
		logger.debug("Finished parsing XML document");
	}
	public void characters(char[] ch, int start, int length) {
		if (wayPt != null) {
			if (name) {
				if (wayPt.displayName == null) {
					wayPt.displayName = new String(ch,start,length);
				} else {
					wayPt.displayName += new String(ch,start,length);
				}
			}
		} else if (p != null) {
			if (ele) {
				p.altitude = Float.parseFloat(new String(ch,start,length));
			} else if (time) {					
			}				
		}
	}
	
	public boolean parse(InputStream in, float maxDistance, Gpx gpx) {
		this.maxDistance = maxDistance;
		this.gpx = gpx;
		importedTpts = 0;
		importedWpts = 0;
		duplicateWpts = 0;
		tooFarWpts = 0;
				
        try {
        	SAXParserFactory factory = SAXParserFactory.newInstance();
    		// Parse the input
            SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(in, this);
			return true;
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
		        
	}
	
	public String getMessage() {
		StringBuffer sb = new StringBuffer();
		if(maxDistance!=0) {
			sb.append("\n(max. distance: " + maxDistance + " km)");
		}
		sb.append("\n\n" + importedWpts + " waypoints imported");
		if(tooFarWpts!=0 || duplicateWpts!=0) {
			sb.append("\n\nSkipped waypoints:");
			if(maxDistance!=0) {
				sb.append("\n" + tooFarWpts + " too far away");
			}
			if(duplicateWpts!=0) {
				sb.append("\n" + duplicateWpts + " already existing");				
			}
		}
		sb.append("\n\n" + importedTpts + " trackpoints imported");
		return sb.toString();
	
	}
}
//#endif