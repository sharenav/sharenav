/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import de.ueller.osmToGpsMid.model.Node;

/**
 * @author hmueller
 *
 */
public class MyMath {
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
     * @return float arc distance `c'
     *  
     */
    final public static float spherical_distance(float phi1, float lambda0,
                                                 float phi, float lambda) {
        float pdiff = (float) Math.sin(((phi - phi1) / 2f));
        float ldiff = (float) Math.sin((lambda - lambda0) / 2f);
        float rval = (float) Math.sqrt((pdiff * pdiff) + (float) Math.cos(phi1)
                * (float) Math.cos(phi) * (ldiff * ldiff));

        return 2.0f * (float) Math.asin(rval);
    }
	public final static float ALT_NN=6378140f;
	final static float FEET_TO_M=0.34f;
	public final static float CIRCUMMAX=40075004f;
	public final static float CIRCUMMAX_PI=(float)(40075004.0/(Math.PI*2));
	
	public final static long dist(Node from,Node to){
		float c=spherical_distance(
				(float)Math.toRadians(from.lat),
				(float)Math.toRadians(from.lon),
				(float)Math.toRadians(to.lat),
				(float)Math.toRadians(to.lon));
		return (long)(c*CIRCUMMAX_PI);
	}

}
