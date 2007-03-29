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

package de.ueller.midlet.gps.data;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;





/**
 * Implements the Mercator projection.
 */
public class Mercator extends Cylindrical {

    /**
     * The Mercator name.
     */
    public final static transient String MercatorName = "Mercator";

    /**
     * The Mercator type of projection.
     */
    public final static transient int MercatorType = 2;

    // maximum number of segments to draw for rhumblines.
    protected static int MAX_RHUMB_SEGS = 512;

    // HACK epsilon: skirt the edge of the infinite. If this is too
    // small
    // then we get too close to +-INFINITY when we forward project.
    // Tweak
    // this if you start getting Infinity or NaN's for forward().
    protected static float epsilon = 0.01f;

    // world<->screen coordinate offsets
    protected int hy, wx;

    // almost constant projection parameters
    protected float tanCtrLat;
    protected float asinh_of_tanCtrLat;
//	private final static Logger logger=Logger.getInstatance(Mercator.class);


    /**
     * Construct a Mercator projection.
     * 
     * @param center LatLonIntPoint center of projection
     * @param scale float scale of projection
     * @param width width of screen
     * @param height height of screen
     */
    public Mercator(Node center, float scale, int width, int height) {

        super(center, scale, width, height, MercatorType);
 //       logger.info("created Mercator");
    }

    public Mercator(Node center, float scale, int width, int height,
            int type) {

        super(center, scale, width, height, type);
    }

    //    protected void finalize() {
    //      Debug.message("mercator", "Mercator finalized");
    //    }

    /**
     * Return stringified description of this projection.
     * 
     * @return String
     * @see Projection#getProjectionID
     */
    public String toString() {
        return "Mercator[" + super.toString();
    }

    /**
     * Called when some fundamental parameters change.
     * <p>
     * Each projection will decide how to respond to this change. For
     * instance, they may need to recalculate "constant" paramters
     * used in the forward() and inverse() calls.
     * <p>
     *  
     */
    protected void computeParameters() {
//    	logger.info("enter computeParameters()");
        super.computeParameters();

        // do some precomputation of stuff
        tanCtrLat = (float) Math.tan(ctrLat);
        asinh_of_tanCtrLat = (float) MoreMath.asinh(tanCtrLat);

        // compute the offsets
        hy = height / 2;
        wx = width / 2;
//    	logger.info("exit computeParameters()");
    }

    /**
     * Sets radian latitude to something sane. This is an abstract
     * function since some projections don't deal well with extreme
     * latitudes.
     * <p>
     * 
     * @param lat float latitude in radians
     * @return float latitude (-PI/2 &lt;= y &lt;= PI/2)
     * @see com.bbn.openmap.Node#normalize_latitude(float)
     *  
     */
    public float normalize_latitude(float lat) {
        if (lat > NORTH_POLE - epsilon) {
            return NORTH_POLE - epsilon;
        } else if (lat < SOUTH_POLE + epsilon) {
            return SOUTH_POLE + epsilon;
        }
        return lat;
    }

    //    protected float forward_x(float lambda) {
    //      return scaled_radius * wrap_longitude(lambda - ctrLon) +
    // (float)wx;
    //    }

    //    protected float forward_y(float phi) {
    //      return (float)hy - (scaled_radius *
    //          (MoreMath.asinh((float)Math.tan(phi)) -
    //           asinh_of_tanCtrLat));
    //    }

    /**
     * Checks if a Node is plot-able.
     * <p>
     * A IntPoint is always plot-able in the Mercator projection (even
     * the North and South poles since we normalize latitude).
     * 
     * @param lat float latitude in decimal degrees
     * @param lon float longitude in decimal degrees
     * @return boolean
     */
    public boolean isPlotable(float lat, float lon) {
        return true;
    }

    /**
     * Projects a IntPoint from Lat/Lon space to X/Y space.
     * <p>
     * 
     * @param pt Node
     * @param p IntPoint retval
     * @return IntPoint p
     */
    public IntPoint forward(Node pt, IntPoint p) {
    	return forward(pt.radlat,pt.radlon,p,true);
    }

    /**
     * Forward projects a lat,lon coordinates.
     * <p>
     * 
     * @param lat raw latitude in decimal degrees
     * @param lon raw longitude in decimal degrees
     * @param p Resulting XY IntPoint
     * @return IntPoint p
     */
    public IntPoint forward(float lat, float lon, IntPoint p) {
    	return forward(ProjMath.degToRad(lat),ProjMath.degToRad(lon),p,true);
    }

    /**
     * Forward projects lat,lon into XY space and returns a IntPoint.
     * <p>
     * 
     * @param lat float latitude in radians
     * @param lon float longitude in radians
     * @param p Resulting XY IntPoint
     * @param isRadian bogus argument indicating that lat,lon
     *        arguments are in radians
     * @return IntPoint p
     */
    public IntPoint forward(float lat, float lon, IntPoint p, boolean isRadian) {
        // same as forward_x and forward_y, and convert to screen
        // coords
        p.setX((int) (scaled_radius * wrap_longitude(lon - ctrLon) + wx));
        p.setY((int) (hy - scaled_radius * (MoreMath.asinh((float) Math.tan(lat)) - asinh_of_tanCtrLat)));
        return p;
    }

    /**
     * Inverse project a IntPoint.
     * 
     * @param pt x,y IntPoint
     * @param llp resulting Node
     * @return Node llp
     */
    public Node inverse(IntPoint pt, Node llp) {
        // convert from screen to world coordinates
        float x = pt.getX() - wx;
        float y = hy - pt.getY();

        // inverse project
        // See if you can take advantage of the precalculated array.
        float wc = asinh_of_tanCtrLat * scaled_radius;
        llp.setLatitude(ProjMath.radToDeg((float) MoreMath.atan(MoreMath.sinh((y + wc)
                / scaled_radius))));
        llp.setLongitude(ProjMath.radToDeg((float) x / scaled_radius + ctrLon));

        return llp;
    }

    /**
     * Inverse project x,y coordinates into a Node.
     * 
     * @param x integer x coordinate
     * @param y integer y coordinate
     * @param llp Node
     * @return Node llp
     * @see Proj#inverse(IntPoint)
     */
    public Node inverse(int x, int y, Node llp) {
        // convert from screen to world coordinates
        x -= wx;
        float y_=(((float)(hy-y))/scaled_radius)+asinh_of_tanCtrLat ;

        llp.setLatLon((float)MoreMath.atan(MoreMath.sinh(y_)),
                ((float)x / scaled_radius) + ctrLon,
                true);
        return llp;
    }


    /**
     * Get the name string of the projection.
     */
    public String getName() {
        return MercatorName;
    }

}
