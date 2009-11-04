/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * Copyright (C) 2007  Kai Krueger
 * 
 */
package de.ueller.osmToGpsMid;

import de.ueller.osmToGpsMid.model.Node;

/**
 * Provides some constants and functions for sphere calculations.
 * @author Harald Mueller
 */
public class MyMath {
	
	/**
	 * Average earth radius of the WGS84 geoid in meters.
	 * 
	 * This is also used as fixed point multiplier to convert
	 * latitude / longitude from radians to fixpoint representation.
	 * With this multiplier, one should get a resolution of 1 m at the equator.
	 * 
	 * This constant has to be in synchrony with the value in GpsMid.
	 *
	 * The old value was 6378159.81 = circumference of the earth in meters / 2 pi.
	 */	
	public static final double PLANET_RADIUS = 6371000.8d;

	public static final float FEET_TO_METER = 0.3048f;

	public static final float CIRCUMMAX = 40075004f;

	public static final float CIRCUMMAX_PI = (float)(40075004.0 / (Math.PI * 2));

	
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
}
