/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * Copyright (C) 2007  Kai Krueger
 * 
 */
package net.sharenav.osmToShareNav;


import net.sharenav.osmToShareNav.model.Node;

/**
 * Provides some constants and functions for sphere calculations.
 * @author Harald Mueller
 */
public class MyMath {
	
	/**
	 * Average earth radius of the WGS84 geoid in meters.
	 * The old value was 6378159.81 = circumference of the earth in meters / 2 pi.
	 */	
	public static final double PLANET_RADIUS = 6371000.8d;

	/**
	 * This constant is used as fixed point multiplier to convert
	 * latitude / longitude from radians to fixpoint representation.
	 * With this multiplier, one should get a resolution of 1 m at the equator.
	 * 
	 * This constant has to be in synchrony with the value in ShareNav.
	 */
	public static final double FIXPT_MULT = PLANET_RADIUS / Configuration.mapPrecisionInMeters;

	public static final float FEET_TO_METER = 0.3048f;

	public static final float CIRCUMMAX = 40075004f;

	public static final float CIRCUMMAX_PI = (float)(40075004.0 / (Math.PI * 2));
	final public static transient double HALF_PI=Math.PI/2d;
	
    public static float degToRad(double deg) {
        return (float) (deg * (Math.PI / 180.0d));
    }

    /**
     * Calculate spherical arc distance between two points.
     * <p>
     * Computes arc distance `c' on the sphere. equation (5-3a). (0
     * &lt;= c &lt;= PI)
     * <p>
     * 
     * @param phi1 latitude in radians of start point
     * @param lambda0 longitude in radians of start point
     * @param phi latitude in radians of end point
     * @param lambda longitude in radians of end point
     * @return arc distance `c' in meters
     *  
     */
//    final public static float spherical_distance(float phi1, float lambda0,
//                                                 float phi, float lambda) {
//        float pdiff = (float) Math.sin(((phi - phi1) / 2f));
//        float ldiff = (float) Math.sin((lambda - lambda0) / 2f);
//        float rval = (float) Math.sqrt((pdiff * pdiff) + (float) Math.cos(phi1)
//                * (float) Math.cos(phi) * (ldiff * ldiff));
//
//        return 2.0f * (float) Math.asin(rval);
//    }
    final public static double spherical_distance(double lat1, double lon1,
    		double lat2, double lon2) {
    	double c = Math.acos(Math.sin(lat1)*Math.sin(lat2) +
    			 Math.cos(lat1)*Math.cos(lat2) *
    			 Math.cos(lon2-lon1));
    	return PLANET_RADIUS * c;
    }
    
    /** TODO: Explain: What is a haversine distance?
     * 
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @return
     */
    final public static double haversine_distance(double lat1, double lon1,
    		double lat2, double lon2) {
    	double latSin = Math.sin((lat2 - lat1) / 2d);
    	double longSin = Math.sin((lon2 - lon1) / 2d);
		double a = (latSin * latSin) + 
				(Math.cos(lat1) * Math.cos(lat2) * longSin * longSin);
    	double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    	return PLANET_RADIUS * c;
    }
    
    final private static double bearing_int(double lat1, double lon1,
    		double lat2, double lon2) {
    	double dLon = lon2 - lon1;
    	double y = Math.sin(dLon) * Math.cos(lat2);
    	double x = Math.cos(lat1) * Math.sin(lat2) - 
    			Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
    	return Math.atan2(y, x);
    }
    
	
//	public final static long dist(Node from,Node to){
//		float c=spherical_distance(
//				(float)Math.toRadians(from.lat),
//				(float)Math.toRadians(from.lon),
//				(float)Math.toRadians(to.lat),
//				(float)Math.toRadians(to.lon));
//		return (long)(c*CIRCUMMAX_PI);
//	}

	public final static long dist(Node from,Node to){
		return (long)( spherical_distance(
				Math.toRadians(from.lat),
				Math.toRadians(from.lon),
				Math.toRadians(to.lat),
				Math.toRadians(to.lon)));
	}

	public final static long dist(Node from,Node to,double factor) {
		return (long)( factor * spherical_distance(
				Math.toRadians(from.lat),
				Math.toRadians(from.lon),
				Math.toRadians(to.lat),
				Math.toRadians(to.lon)));
	}
	
	/**
	 * calculate the start bearing in 1/2 degree so result 90 indicates 180 degree. 
	 * @param from
	 * @param to
	 * @return
	 */
	public final static byte bearing_start(Node from, Node to) {
		double b = bearing_int(
				Math.toRadians(from.lat),
				Math.toRadians(from.lon),
				Math.toRadians(to.lat),
				Math.toRadians(to.lon));
		return (byte) Math.round(Math.toDegrees(b)/2);
	}

	public final static byte inverseBearing(byte bearing) {
		int invBearing = bearing + 90;
		if (invBearing > 90) {
			invBearing -=180;
		}
		return (byte) invBearing;
	}

	/** Calculates X, Y and Z coordinates from latitude and longitude
	 * 
	 * @param n Node with latitude and longitude in degrees
	 * @return Array containing X, Y and Z in this order
	 */
	public final static double [] latlon2XYZ(Node n) {
		double lat = Math.toRadians(n.lat);
		double lon = Math.toRadians(n.lon);
		double [] res = new double[3];
				
		res[0] = PLANET_RADIUS * Math.cos(lat) * Math.cos(lon);
		res[1] = PLANET_RADIUS * Math.cos(lat) * Math.sin(lon);
		res[2] = PLANET_RADIUS * Math.sin(lat);		    
		
		return res;
	}
	
	/**
	 * calculate the destination point if you travel from n with initial bearing and dist in meters
	 * @param n startpoint
	 * @param bearing initial direction (great circle) in degree
	 * @param dist distance in meters
	 * @return the final destination
	 */
	public final static Node moveBearingDist(Node n,double bearing, double dist ){
		double lat1 = Math.toRadians(n.lat);
		double lon1 = Math.toRadians(n.lon);
		bearing= Math.toRadians(bearing);
		double cosDist = Math.cos(dist/PLANET_RADIUS);
		double sinLat1 = Math.sin(lat1);
		double sinDist = Math.sin(dist/PLANET_RADIUS);
		double lat2 = Math.asin( sinLat1*cosDist + Math.cos(lat1)*sinDist*Math.cos(bearing) );
		double lon2 = lon1 + Math.atan2(Math.sin(bearing)*sinDist*Math.cos(lat1),cosDist-sinLat1*Math.sin(lat2));
		return new Node((float)Math.toDegrees(lat2),(float)Math.toDegrees(lon2),-1);
	}
	
	/**
	 * Calculates the great circle distance between two nodes.
	 * @param lat1 Latitude of first point in radians
	 * @param lon1 Longitude of first point in radians
	 * @param lat2 Latitude of second point in radians
	 * @param lon2 Longitude of second point in radians
	 * @return Angular distance in radians
	 */
	public static double calcDistance(double lat1, double lon1, double lat2, double lon2) {
		// Taken from http://williams.best.vwh.net/avform.htm
		double p1 = Math.sin((lat1 - lat2) / 2);
		double p2 = Math.sin((lon1 - lon2) / 2);
		double d = 2 * Math.asin(Math.sqrt(p1 * p1
						+ Math.cos(lat1) * Math.cos(lat2) * p2 * p2));
		return d;
	}


	/**
	 * Calculates the great circle distance and course between two nodes.
	 * @param lat1 Latitude of first point in radians
	 * @param lon1 Longitude of first point in radians
	 * @param lat2 Latitude of second point in radians
	 * @param lon2 Longitude of second point in radians
	 * @return double array, with the distance in meters at [0]
	 * 		and the course in radians at [1]
	 */
	private static double[] calcDistanceAndCourse(double lat1, double lon1, double lat2, 
			double lon2) {
		double fDist = calcDistance(lat1, lon1, lat2, lon2);
		double fCourse;
		
		// Also taken from http://williams.best.vwh.net/avform.htm
		// The original formula checks for (cos(lat1) < double.MIN_VALUE) but I think 
		// it's worth to save a cosine.
		if (lat1 > (HALF_PI - 0.0001)) {
			// At the north pole, all points are south.
			fCourse = Math.PI;
		} else if (lat1 < -(HALF_PI - 0.0001)) {
			// And at the south pole, all points are north.
			fCourse = 0;
		} else {
			double tc = Math.acos(((Math.sin(lat2) - 
				Math.sin(lat1) * Math.cos(fDist)) /	(Math.sin(fDist) * Math.cos(lat1))));
			// The original formula has sin(lon2 - lon1) but that's if western
			// longitudes are positive (mentioned at the beginning of the page).
			if (Math.sin(lon1 - lon2) < 0) {
			    fCourse = tc;
			} else {
			    fCourse = Math.PI*2 - tc; 
			}
		}
		fDist *= PLANET_RADIUS;
		double[] result = new double[2];
		result[0] = fDist;
		result[1] = fCourse;
		return result;
	}

	/**
	 * Calculates the great circle distance and course between two nodes.
	 * @param n1 start point
	 * @param n2 end point
	 * @return double array, with the distance in meters at [0]
	 * 		and the course in degrees at [1]
	 */
	public static double[] calcDistanceAndCourse(Node n1,Node n2) {
		double ret[]=calcDistanceAndCourse(Math.toRadians(n1.lat), Math.toRadians(n1.lon), Math.toRadians(n2.lat), Math.toRadians(n2.lon));
		ret[1]=Math.toDegrees(ret[1]);
		return ret;
	}

	/**
	 * Returns the point of intersection of two paths defined by point and bearing
	 *
	 *   see http://williams.best.vwh.net/avform.htm#Intersection
	 *
	 * @param   {LatLon} p1: First point
	 * @param   {Number} brng1: Initial bearing from first point
	 * @param   {LatLon} p2: Second point
	 * @param   {Number} brng2: Initial bearing from second point
	 * @returns {LatLon} Destination point (null if no unique intersection defined)
	 */
	public static Node intersection(Node p1, double b1g, Node p2, double b2g) {
	  double lat1 = Math.toRadians(p1.lat), lon1 = Math.toRadians(p1.lon);
	  double lat2 = Math.toRadians(p2.lat), lon2 = Math.toRadians(p2.lon);
	  double b1 = Math.toRadians(b1g), b2 =  Math.toRadians(b2g);
	  double dLat = lat2-lat1, dLon = lon2-lon1;
	  
	  double dist12=calcDistance(lat1, lon1, lat2, lon2);
//	  double dist12 = 2*Math.asin( Math.sqrt( Math.sin(dLat/2)*Math.sin(dLat/2) + 
//	    Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2)*Math.sin(dLon/2) ) );
//	  if (dist12-dist12T > 0.0001d){
//		  System.out.println("not the same");
//	  }
	  if (dist12 == 0) return null;
	  
	  // initial/final bearings between points
	  double  brngA = Math.acos( ( Math.sin(lat2) - Math.sin(lat1)*Math.cos(dist12) ) / 
	    ( Math.sin(dist12)*Math.cos(lat1) ) );
	  if (Double.isNaN(brngA)) brngA = 0;  // protect against rounding
	  double brngB = Math.acos( ( Math.sin(lat1) - Math.sin(lat2)*Math.cos(dist12) ) / 
	    ( Math.sin(dist12)*Math.cos(lat2) ) );
	  double brng12;
	  double brng21;
	  if (Math.sin(lon2-lon1) > 0) { 
		brng12 = brngA;
		brng21 = 2*Math.PI - brngB;
	  } else {
	    brng12 = 2*Math.PI - brngA;
	    brng21 = brngB;
	  }
	  
	  double alpha1 = (b1 - brng12 + Math.PI) % (2*Math.PI) - Math.PI;  // angle 2-1-3
	  double alpha2 = (brng21 - b2 + Math.PI) % (2*Math.PI) - Math.PI;  // angle 1-2-3
	  
	  if (Math.sin(alpha1)==0 && Math.sin(alpha2)==0) return null;  // infinite intersections
	  if (Math.sin(alpha1)*Math.sin(alpha2) < 0) return null;       // ambiguous intersection
	  
	  //alpha1 = Math.abs(alpha1);
	  //alpha2 = Math.abs(alpha2);
	  // ... Ed Williams takes abs of alpha1/alpha2, but seems to break calculation?
	  
	  double alpha3 = Math.acos( -Math.cos(alpha1)*Math.cos(alpha2) + 
	                       Math.sin(alpha1)*Math.sin(alpha2)*Math.cos(dist12) );
	  double dist13 = Math.atan2( Math.sin(dist12)*Math.sin(alpha1)*Math.sin(alpha2), 
	                       Math.cos(alpha2)+Math.cos(alpha1)*Math.cos(alpha3) );
	  double lat3 = Math.asin( Math.sin(lat1)*Math.cos(dist13) + 
	                    Math.cos(lat1)*Math.sin(dist13)*Math.cos(b1) );
	  double dLon13 = Math.atan2( Math.sin(b1)*Math.sin(dist13)*Math.cos(lat1), 
	                       Math.cos(dist13)-Math.sin(lat1)*Math.sin(lat3) );
	  double lon3 = lon1+dLon13;
	  lon3 = (lon3+Math.PI) % (2*Math.PI) - Math.PI;  // normalize to -180..180 degrees
	  
	  return new Node((float)Math.toDegrees(lat3), (float)Math.toDegrees(lon3),-1);
	}
	
	

}
