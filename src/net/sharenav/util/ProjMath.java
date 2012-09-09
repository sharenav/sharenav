// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source$
// $RCSfile$
// $Revision$
// $Date$
// $Author$
// 
// **********************************************************************

package net.sharenav.util;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.graphics.Projection;




/**
 * Math functions used by projection code.
 */
public final class ProjMath {

    /**
     * North pole latitude in radians.
     */
    public final static transient float NORTH_POLE_F = MoreMath.HALF_PI;

    /**
     * South pole latitude in radians.
     */
    public final static transient float SOUTH_POLE_F = -NORTH_POLE_F;

    /**
     * North pole latitude in radians.
     */
    public final static transient double NORTH_POLE_D = MoreMath.HALF_PI_D;

    /**
     * South pole latitude in radians.
     */
    public final static transient double SOUTH_POLE_D = -NORTH_POLE_D;

    /**
     * Dateline longitude in radians.
     */
    public final static transient float DATELINE_F = (float) Math.PI;

    /**
     * Dateline longitude in radians.
     */
    public final static transient double DATELINE_D = Math.PI;

    /**
     * Longitude range in radians.
     */
    public final static transient float LON_RANGE_F = MoreMath.TWO_PI;

    /**
     * Longitude range in radians.
     */
    public final static transient double LON_RANGE_D = MoreMath.TWO_PI_D;

    // cannot construct
    private ProjMath() {}

    /**
     * Rounds the quantity away from 0.
     * 
     * @param x in value
     * @return double
     * @see #qint(double)
     */
    public final static double roundAdjust(double x) {
        return qint_old(x);
    }

    /**
     * Rounds the quantity away from 0.
     * 
     * @param x value
     * @return double
     */
    public final static double qint(double x) {
        return qint_new(x);
    }

    final private static double qint_old(double x) {
        return (((int) x) < 0) ? (x - 0.5) : (x + 0.5);
    }

    final private static double qint_new(double x) {
        // -1 or +1 away from zero
        return (x <= 0.0) ? (x - 1.0) : (x + 1.0);
    }

    /**
     * Calculates the shortest arc distance between two lons.
     * 
     * @param lon1 radians
     * @param lon2 radians
     * @return float distance
     */
    public final static float lonDistance(float lon1, float lon2) {
        return (float) Math.min(Math.abs(lon1 - lon2), ((lon1 < 0) ? lon1
                + Math.PI : Math.PI - lon1)
                + ((lon2 < 0) ? lon2 + Math.PI : Math.PI - lon2));
    }

	/**
	 * Calculates the distance between two nodes along the great circle.
	 * @param n1 Reference to first node
	 * @param n2 Reference to second node
	 * @return Distance in meters, 0 if any of the two references is null
	 */
	public static float getDistance(Node n1, Node n2) {
		if (n1 == null || n2 == null) {
			return 0.0f;
		}
		
		float lat1 = n1.radlat;
		float lon1 = n1.radlon;
		float lat2 = n2.radlat;
		float lon2 = n2.radlon;
		return getDistance(lat1, lon1, lat2, lon2);
	}
	
	/**
	 * Calculates the great circle distance between two nodes.
	 * @param lat1 Latitude of first point in radians
	 * @param lon1 Longitude of first point in radians
	 * @param lat2 Latitude of second point in radians
	 * @param lon2 Longitude of second point in radians
	 * @return Angular distance in radians
	 */
	public static float calcDistance(float lat1, float lon1, float lat2, float lon2) {
		// Taken from http://williams.best.vwh.net/avform.htm
		double p1 = Math.sin((lat1 - lat2) / 2);
		double p2 = Math.sin((lon1 - lon2) / 2);
		float d = 2 * MoreMath.asin((float)Math.sqrt(p1 * p1
						+ Math.cos(lat1) * Math.cos(lat2) * p2 * p2));
		return d;
	}

	/**
	 * Calculates the great circle distance between two nodes.
	 * @param lat1 Latitude of first point in radians
	 * @param lon1 Longitude of first point in radians
	 * @param lat2 Latitude of second point in radians
	 * @param lon2 Longitude of second point in radians
	 * @return Distance in meters
	 */
	public static float getDistance(float lat1, float lon1, float lat2, float lon2) {
		float d = calcDistance(lat1, lon1, lat2, lon2);
		//taken from ITM Project Korbel
		//d *= 3437.7387; //radians to nautical miles
		//d *= 1.150779; //nautical miles to land miles
		//d *= 1.609; //land miles to kilometers
		//d *= 1000; //kilometers to meters  
		// Pretty complicated way to do this as this is simply the earth radius...
		return d * MoreMath.PLANET_RADIUS;
	}

	/**
	 * Calculates the great circle distance and course between two nodes.
	 * @param lat1 Latitude of first point in radians
	 * @param lon1 Longitude of first point in radians
	 * @param lat2 Latitude of second point in radians
	 * @param lon2 Longitude of second point in radians
	 * @return Float array, with the distance in meters at [0]
	 * 		and the course in degrees at [1]
	 */
	public static float[] calcDistanceAndCourse(float lat1, float lon1, float lat2, 
			float lon2) {
		float fDist = calcDistance(lat1, lon1, lat2, lon2);
		float fCourse;
		
		// Also taken from http://williams.best.vwh.net/avform.htm
		// The original formula checks for (cos(lat1) < Float.MIN_VALUE) but I think 
		// it's worth to save a cosine.
		if (lat1 > (MoreMath.HALF_PI - 0.0001)) {
			// At the north pole, all points are south.
			fCourse = (float)Math.PI;
		} else if (lat1 < -(MoreMath.HALF_PI - 0.0001)) {
			// And at the south pole, all points are north.
			fCourse = 0;
		} else {
			float tc = MoreMath.acos((float)((Math.sin(lat2) - 
				Math.sin(lat1) * Math.cos(fDist)) /	(Math.sin(fDist) * Math.cos(lat1))));
			tc = (int)(tc * MoreMath.FAC_RADTODEC + 0.5);
			// The original formula has sin(lon2 - lon1) but that's if western
			// longitudes are positive (mentioned at the beginning of the page).
			if (Math.sin(lon1 - lon2) < 0) {
			    fCourse = tc;
			} else {
			    fCourse = 360 - tc; 
			}
		}
		fDist *= MoreMath.PLANET_RADIUS;
		float[] result = new float[2];
		result[0] = fDist;
		result[1] = fCourse;
		return result;
	}

	/**
     * Converts between decimal degrees and scoords.
     * 
     * @param deg degrees
     * @return long scoords
     *  
     */
    public final static long DEG_TO_SC(double deg) {
        return (long) (deg * 3600000);
    }

    /**
     * Converts between decimal degrees and scoords.
     * 
     * @param sc scoords
     * @return double decimal degrees
     */
    public final static double SC_TO_DEG(int sc) {
        return ((sc) / (60.0 * 60.0 * 1000.0));
    }

    /**
     * Converts radians to degrees.
     * 
     * @param rad radians
     * @return double decimal degrees
     */
    public final static double radToDeg(double rad) {
        return (rad * (180.0d / Math.PI));
    }

    /**
     * Converts radians to degrees.
     * 
     * @param rad radians
     * @return float decimal degrees
     */
    public final static float radToDeg(float rad) {
        return (float) radToDeg((double)rad);
    }

    /**
     * Converts degrees to radians.
     * 
     * @param deg degrees
     * @return double radians
     */
    public final static double degToRad(double deg) {
        return (deg * (Math.PI / 180.0d));
    }

    /**
     * Converts degrees to radians.
     * 
     * @param deg degrees
     * @return float radians
     */
    public final static float degToRad(float deg) {
        return (float) degToRad((double)deg);
    }

    /**
     * Generates a hashCode value for a lat/lon pair.
     * 
     * @param lat latitude
     * @param lon longitude
     * @return int hashcode
     *  
     */
    public final static int hashLatLon(float lat, float lon) {
        if (lat == -0f) {
			lat = 0f;//handle negative zero (anything else?)
		}
        if (lon == -0f) {
			lon = 0f;
		}
        int tmp = Float.floatToIntBits(lat);
        int hash = (tmp << 5) | (tmp >> 27);//rotate the lat bits
        return hash ^ Float.floatToIntBits(lon);//XOR with lon
    }

    /**
     * Converts an array of decimal degrees float lat/lons to float
     * radians in place.
     * 
     * @param degs float[] lat/lons in decimal degrees
     * @return float[] lat/lons in radians
     */
    public final static float[] arrayDegToRad(float[] degs) {
        for (int i = 0; i < degs.length; i++) {
            degs[i] = degToRad(degs[i]);
        }
        return degs;
    }

    /**
     * Converts an array of radian float lat/lons to decimal degrees
     * in place.
     * 
     * @param rads float[] lat/lons in radians
     * @return float[] lat/lons in decimal degrees
     */
    public final static float[] arrayRadToDeg(float[] rads) {
        for (int i = 0; i < rads.length; i++) {
            rads[i] = radToDeg(rads[i]);
        }
        return rads;
    }

    /**
     * Converts an array of decimal degrees double lat/lons to double
     * radians in place.
     * 
     * @param degs double[] lat/lons in decimal degrees
     * @return double[] lat/lons in radians
     */
    public final static double[] arrayDegToRad(double[] degs) {
        for (int i = 0; i < degs.length; i++) {
            degs[i] = degToRad(degs[i]);
        }
        return degs;
    }

    /**
     * Converts an array of radian double lat/lons to decimal degrees
     * in place.
     * 
     * @param rads double[] lat/lons in radians
     * @return double[] lat/lons in decimal degrees
     */
    public final static double[] arrayRadToDeg(double[] rads) {
        for (int i = 0; i < rads.length; i++) {
            rads[i] = radToDeg(rads[i]);
        }
        return rads;
    }

    /**
     * Normalizes radian latitude. Normalizes latitude if at or
     * exceeds epsilon distance from a pole.
     * 
     * @param lat float latitude in radians
     * @param epsilon epsilon (&gt;= 0) radians distance from pole
     * @return float latitude (-PI/2 &lt;= phi &lt;= PI/2)
     * @see Proj#normalize_latitude(float)
     * @see com.bbn.openmap.LatLonIntPoint#normalize_latitude(float)
     */
    public final static float normalize_latitude(float lat, float epsilon) {
        if (lat > NORTH_POLE_F - epsilon) {
            return NORTH_POLE_F - epsilon;
        } else if (lat < SOUTH_POLE_F + epsilon) {
            return SOUTH_POLE_F + epsilon;
        }
        return lat;
    }

    /**
     * Normalizes radian latitude. Normalizes latitude if at or
     * exceeds epsilon distance from a pole.
     * 
     * @param lat double latitude in radians
     * @param epsilon epsilon (&gt;= 0) radians distance from pole
     * @return double latitude (-PI/2 &lt;= phi &lt;= PI/2)
     * @see Proj#normalize_latitude(float)
     * @see com.bbn.openmap.LatLonIntPoint#normalize_latitude(float)
     */
    public final static double normalize_latitude(double lat, double epsilon) {
        if (lat > NORTH_POLE_D - epsilon) {
            return NORTH_POLE_D - epsilon;
        } else if (lat < SOUTH_POLE_D + epsilon) {
            return SOUTH_POLE_D + epsilon;
        }
        return lat;
    }

    /**
     * Sets radian longitude to something sane.
     * 
     * @param lon float longitude in radians
     * @return float longitude (-PI &lt;= lambda &lt; PI)
     * @see com.bbn.openmap.LatLonIntPoint#wrap_longitude(float)
     */
    public final static float wrap_longitude(float lon) {
        if ((lon < -DATELINE_F) || (lon > DATELINE_F)) {
            lon += DATELINE_F;
            lon = (lon % LON_RANGE_F);
            lon += (lon < 0) ? DATELINE_F : -DATELINE_F;
        }
        return lon;
    }

    /**
     * Sets radian longitude to something sane.
     * 
     * @param lon double longitude in radians
     * @return double longitude (-PI &lt;= lambda &lt; PI)
     * @see #wrap_longitude(float)
     */
    public final static double wrap_longitude(double lon) {
        if ((lon < -DATELINE_D) || (lon > DATELINE_D)) {
            lon += DATELINE_D;
            lon = (lon % LON_RANGE_D);
            lon += (lon < 0) ? DATELINE_D : -DATELINE_D;
        }
        return lon;
    }

    /**
     * Converts units (km, nm, miles, etc) to decimal degrees for a
     * spherical planet. This does not check for arc distances &gt;
     * 1/2 planet circumference, which are better represented as (2pi -
     * calculated arc).
     * 
     * @param u units float value
     * @param uCircumference units circumference of planet
     * @return float decimal degrees
     */
    public final static float sphericalUnitsToDeg(float u, float uCircumference) {
        return 360f * (u / uCircumference);
    }

    /**
     * Converts units (km, nm, miles, etc) to arc radians for a
     * spherical planet. This does not check for arc distances &gt;
     * 1/2 planet circumference, which are better represented as (2pi -
     * calculated arc).
     * 
     * @param u units float value
     * @param uCircumference units circumference of planet
     * @return float arc radians
     */
    public final static float sphericalUnitsToRad(float u, float uCircumference) {
        return MoreMath.TWO_PI * (u / uCircumference);
    }

    /**
     * Calculates the geocentric latitude given a geographic latitude.
     * According to John Synder: <br>
     * "The geographic or geodetic latitude is the angle which a line
     * perpendicular to the surface of the ellipsoid at the given
     * IntPoint makes with the plane of the equator. ...The geocentric
     * latitude is the angle made by a line to the center of the
     * ellipsoid with the equatorial plane". ( <i>Map Projections --A
     * Working Manual </i>, p 13)
     * <p>
     * Translated from Ken Anderson's lisp code <i>Freeing the Essence
     * of Computation </i>
     * 
     * @param lat float geographic latitude in radians
     * @param flat float flatening factor
     * @return float geocentric latitude in radians
     * @see #geographic_latitude
     */
    public final static float geocentric_latitude(float lat, float flat) {
        float f = 1.0f - flat;
        return MoreMath.atan((f * f) * (float) Math.tan(lat));
    }

    /**
     * Calculates the geographic latitude given a geocentric latitude.
     * Translated from Ken Anderson's lisp code <i>Freeing the Essence
     * of Computation </i>
     * 
     * @param lat float geocentric latitude in radians
     * @param flat float flatening factor
     * @return float geographic latitude in radians
     * @see #geocentric_latitude
     */
    public final static float geographic_latitude(float lat, float flat) {
        float f = 1.0f - flat;
        return MoreMath.atan((float) Math.tan(lat) / (f * f));
    }

    /**
     * Given a couple of IntPoints representing a bounding box, find out
     * what the scale should be in order to make those IntPoints appear
     * at the corners of the projection.
     * 
     * @param ll1 the upper left coordinates of the bounding box.
     * @param ll2 the lower right coordinates of the bounding box.
     * @param projection the projection to use for other projection
     *        parameters, like map width and map height.
     *        @deprecated never used so far
     */
    public static float getScale(Node ll1,
                                 Node ll2,
                                 Projection projection) {
        if (projection == null) {
            return Float.MAX_VALUE;
        }

        IntPoint IntPoint1 = projection.forward(ll1);
        IntPoint IntPoint2 = projection.forward(ll2);

        return getScale(ll1, ll2, IntPoint1, IntPoint2, projection);
    }


    /**
     * Given a couple of IntPoints representing a bounding box, find out
     * what the scale should be in order to make those IntPoints appear
     * at the corners of the projection.
     * 
     * @param ll1 the upper left coordinates of the bounding box.
     * @param ll2 the lower right coordinates of the bounding box.
     * @param IntPoint1 a java.awt.IntPoint reflecting a pixel spot on the
     *        projection that matches the ll1 coordinate, the upper
     *        left corner of the area of interest.
     * @param IntPoint2 a java.awt.IntPoint reflecting a pixel spot on the
     *        projection that matches the ll2 coordinate, usually the
     *        lower right corner of the area of interest.
     * @param projection the projection to use to query to get the
     *        scale for, for projection type and height and width.
     *        @deprecated never used so far
     */
    protected static float getScale(Node ll1,
                                    Node ll2,
                                    IntPoint IntPoint1,
                                    IntPoint IntPoint2, Projection projection) {

        return projection.getScale(ll1, ll2, IntPoint1, IntPoint2);
    }

    /*
     * public static void main(String[] args) { float degs =
     * sphericalUnitsToRad( Planet.earthEquatorialRadius/2,
     * Planet.earthEquatorialRadius); Debug.output("degs = " + degs);
     * float LAT_DEC_RANGE = 90.0f; float LON_DEC_RANGE = 360.0f;
     * float lat, lon; for (int i = 0; i < 100; i++) { lat =
     * com.bbn.openmap.Node.normalize_latitude(
     * (float)Math.random()*LAT_DEC_RANGE); lon =
     * com.bbn.openmap.Node.wrap_longitude(
     * (float)Math.random()*LON_DEC_RANGE); Debug.output( "(" + lat +
     * "," + lon + ") : (" + degToRad(lat) + "," + degToRad(lon) + ") : (" +
     * radToDeg(degToRad(lat)) + "," + radToDeg(degToRad(lon)) + ")"); } }
     */
}
