/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.gps;

import net.sharenav.sharenav.mapdata.Entity;
import net.sharenav.util.MoreMath;

public class Node extends Entity {
	public float radlat = 0;
	public float radlon = 0;
	public final static float NORTH_POLE = 90.0f;
    public final static float SOUTH_POLE = -NORTH_POLE;
    public final static float DATELINE = 180.0f;
    public final static float LON_RANGE = 360.0f;

    public final static float EQUIVALENT_TOLERANCE = 0.00001f;

	
    /**
     * Constructs a LatLonPoint from raw float lat/lon in decimal degrees.
     * 
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     */
    public Node(float lat, float lon) {
        setLatLonDeg(lat, lon);
    }

    /**
     * Constructs a LatLonPoint from float lat/lon in radians or degrees.
     * 
     * @param lat latitude in radians
     * @param lon longitude in radians
     * @param isRadian placeholder indicates radians
     */
    public Node(float lat, float lon, boolean isRadian) {
    	if (isRadian) {
    		setLatLonRad(lat, lon);
    	} else {
    		setLatLonDeg(lat, lon);
    	}
    }

    /**
     * Constructs a node and copies lat/lon from the other node
     * @param node
     */
    public Node(Node node) {
        setLatLon(node);
    }
    
    /**
     * Constructs a Node with lat=0 / lon=0.
     */
	public Node() {
		setLatLonRad(0, 0);
	}

	public float getLatDeg() {
		return radlat * MoreMath.FAC_RADTODEC;
	}
	
	public float getLonDeg() {
		return radlon * MoreMath.FAC_RADTODEC;
	}

	public void setLatDeg(float l) {
		radlat = l * MoreMath.FAC_DECTORAD;
	}
	
	public void setLonDeg(float l) {
		radlon = l * MoreMath.FAC_DECTORAD;
	}
	
    /**
     * Set latitude and longitude.
     * 
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     */
    public void setLatLonDeg(float lat, float lon) {
        radlat = normalize_latitude(lat) * MoreMath.FAC_DECTORAD;
        radlon = wrap_longitude(lon) * MoreMath.FAC_DECTORAD;
    }

    /**
     * Set latitude and longitude.
     * 
     * @param lat latitude in radians
     * @param lon longitude in radians
     */
    public void setLatLonRad(float lat, float lon) {
        radlat = lat;
        radlon = lon;
    }

    /**
     * Copies latitude and longitude from the passed node.
     * @param node Node from which lat and lon are taken
     */
    public void setLatLon(Node node) {
        radlat = node.radlat;
        radlon = node.radlon;
    }

    /**
     * Sets latitude to something sane.
     * 
     * @param lat latitude in decimal degrees
     * @return float normalized latitude in decimal degrees
     *         (&minus;90&deg; &le; &phi; &le; 90&deg;)
     */
    public final static float normalize_latitude(float lat) {
        if (lat > NORTH_POLE) {
            lat = NORTH_POLE;
        }
        if (lat < SOUTH_POLE) {
            lat = SOUTH_POLE;
        }
        return lat;
    }

    /**
     * Sets longitude to something sane.
     * 
     * @param lon longitude in decimal degrees
     * @return float wrapped longitude in decimal degrees
     *         (&minus;180&deg; &le; &lambda; &le; 180&deg;)
     */
    public final static float wrap_longitude(float lon) {
        if ((lon < -DATELINE) || (lon > DATELINE)) {
            //System.out.print("LatLonPoint: wrapping longitude " + lon);
            lon += DATELINE;
            lon = lon % LON_RANGE;
            lon = (lon < 0) ? DATELINE + lon : -DATELINE + lon;
            //System.out.println(" to " + lon);
        }
        return lon;
    }

    public Node copy() {
    	Node n = new Node();
//    	if (nameIdx != -1) {
//		n.nameIdx = nameIdx;
//	}
//	if (Legend.enableUrlTags) {
//		if (urlIdx != -1) {
//			n.urlIdx=urlIdx;
//		}
//	}
//	if (Legend.enablePhoneTags) {
//		if (phoneIdx != -1) {
//			n.phoneIdx=phoneIdx;
//		}
//	}
    	n.type = type;
    	n.radlat = radlat;
    	n.radlon = radlon;
    	return n;
    }
    
    public String toString() {
    	return ("node: " + radlat * MoreMath.FAC_RADTODEC + "/" +
    			radlon * MoreMath.FAC_RADTODEC);
    }
}
