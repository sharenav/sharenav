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

import java.lang.Float;




/**
 * Proj is the base class of all Projections.
 * <p>
 * You probably don't want to use this class unless you are hacking
 * your own projections, or need extended functionality. To be safe
 * you will want to use the Projection interface.
 * 
 * <h3>Notes:</h3>
 * 
 * <ul>
 * 
 * <li>We deal in radians internally. The outside world usually deals
 * in decimal degrees. If you have data in radians, DON'T bother
 * converting it into DD's since we'll convert it right back into
 * radians for the projection step. For more optimization tips, see
 * the OMPoly class.
 * 
 * <li>We default to projecting our data using the WGS84 datum. You
 * can change the appropriate parameters of the projection after
 * construction if you need to use a different datum. And of course
 * you can derive your own projections from this class as you see fit.
 * 
 * <li>The forward() and inverse() methods are currently implemented
 * using the algorithms given in John Synder's <i>Map Projections --A
 * Working Manual </i> for the sphere. This is sufficient for display
 * purposes, but you should use ellipsoidal algorithms in the
 * GreatCircle class to calculate distance and azimuths on the
 * ellipsoid. See each projection individually for more information.
 * 
 * <li>This class is not thread safe. If two or more threads are
 * using the same Proj, then they could disrupt each other.
 * Occasionally you may need to call a <code>set</code> method of
 * this class. This might interfere with another thread that's using
 * the same projection for <code>forwardPoly</code> or another
 * Projection interface method. In general, you should not need to
 * call any of the <code>set</code> methods directly, but let the
 * MapBean do it for you.
 * 
 * <li>All the various <code>forwardOBJ()</code> methods for
 * ArrayList graphics ultimately go through <code>forwardPoly()</code>.
 * 
 * </ul>
 * 
 * @see Projection
 * @see Cylindrical
 * @see Mercator
 * @see CADRG
 * @see Azimuth
 * @see Orthographic
 * @see Planet
 * @see GreatCircle
 * @see com.bbn.openmap.omGraphics.OMPoly
 * 
 */
public abstract class Proj implements Projection {

    // SOUTH_POLE <= phi <= NORTH_POLE (radians)
    // -DATELINE <= lambda <= DATELINE (radians)

    /**
     * North pole latitude in radians.
     */
    public final static float NORTH_POLE = ProjMath.NORTH_POLE_F;

    /**
     * South pole latitude in radians.
     */
    public final static transient float SOUTH_POLE = ProjMath.SOUTH_POLE_F;

    /**
     * Dateline longitude in radians.
     */
    public final static transient float DATELINE = ProjMath.DATELINE_F;

    /**
     * Minimum width of projection.
     */
    public final static transient int MIN_WIDTH = 10; // pixels

    /**
     * Minimum height of projection.
     */
    public final static transient int MIN_HEIGHT = 10; // pixels

    // Used for generating segments of ArrayList objects
    protected static transient int NUM_DEFAULT_CIRCLE_VERTS = 64;
    protected static transient int NUM_DEFAULT_GREAT_SEGS = 512;

    // pixels per meter (an extra scaling factor).
    protected int pixelsPerMeter = Planet.defaultPixelsPerMeter; // PPM
    protected float planetRadius = Planet.wgs84_earthEquatorialRadiusMeters;// EARTH_RADIUS
    protected float planetPixelRadius = planetRadius * pixelsPerMeter; // EARTH_PIX_RADIUS
    protected float planetPixelCircumference = MoreMath.TWO_PI
            * planetPixelRadius; // EARTH_PIX_CIRCUMFERENCE

    protected int width = 640, height = 480;
    protected float minscale = 1.0f; // 1:minscale
    protected float maxscale = planetPixelCircumference / width;// good
    // for
    // cylindrical
    protected float scale = maxscale;
    protected float scaled_radius = planetPixelRadius / scale;
    protected float ctrLat = 0.0f; // center latitude in radians
    protected float ctrLon = 0.0f; // center longitude in radians
    protected int type = Mercator.MercatorType; // Mercator is default
    protected String projID = null; // identifies this projection
    protected Mercator mercator = null; // for rhumbline calculations
//	private final static Logger logger=Logger.getInstatance(Proj.class);

    // (if needed)

    /**
     * Construct a projection.
     * 
     * @param center LatLonIntPoint center of projection
     * @param s float scale of projection
     * @param w width of screen
     * @param h height of screen
     * @param type projection type
     * @see ProjectionFactory
     */
    public Proj(Node center, float s, int w, int h, int type) {
 
//    	logger.info("init");
        this.type = type;
        setParms(center, s, w, h);
        projID = null;

        // for rhumbline projecting
        if (!(this instanceof Mercator)) {
            mercator = new Mercator(center, scale, width, height);
        }
    }

    /**
     * Set the pixels per meter constant.
     * 
     * @param ppm int Pixels Per Meter scale-factor constant
     */
    public void setPPM(int ppm) {
        pixelsPerMeter = ppm;
        if (pixelsPerMeter < 1) {
            pixelsPerMeter = 1;
        }
        computeParameters();
    }

    /**
     * Get the pixels-per-meter constant.
     * 
     * @return int Pixels Per Meter scale-factor constant
     */
    public int getPPM() {
        return pixelsPerMeter;
    }

    /**
     * Set the planet radius.
     * 
     * @param radius float planet radius in meters
     */
    public void setPlanetRadius(float radius) {
        planetRadius = radius;
        if (planetRadius < 1.0f) {
            planetRadius = 1.0f;
        }
        computeParameters();
    }

    /**
     * Get the planet radius.
     * 
     * @return float radius of planet in meters
     */
    public float getPlanetRadius() {
        return planetRadius;
    }

    /**
     * Get the planet pixel radius.
     * 
     * @return float radius of planet in pixels
     */
    public float getPlanetPixelRadius() {
        return planetPixelRadius;
    }

    /**
     * Get the planet pixel circumference.
     * 
     * @return float circumference of planet in pixels
     */
    public float getPlanetPixelCircumference() {
        return planetPixelCircumference;
    }

    /**
     * Set the scale of the projection.
     * <p>
     * Sets the projection to the scale 1:s iff minscale &lt; s &lt;
     * maxscale. <br>
     * If s &lt; minscale, sets the projection to minscale. <br>
     * If s &gt; maxscale, sets the projection to maxscale. <br>
     * 
     * @param s float scale
     */
    public void setScale(float s) {
        scale = s;
        if (scale < minscale) {
            scale = minscale;
            computeParameters();
        } else if (scale > maxscale) {
            scale = maxscale;
            computeParameters();
        }
        computeParameters();
        projID = null;
    }

    /**
     * Set the minscale of the projection.
     * <p>
     * Usually you will not need to do this.
     * 
     * @param s float minscale
     */
    public void setMinScale(float s) {
        if (s > maxscale) {
			return;
		}

        minscale = s;
        if (scale < minscale) {
            scale = minscale;
        }
        computeParameters();
        projID = null;
    }

    /**
     * Set the maximum scale of the projection.
     * <p>
     * Usually you will not need to do this.
     * 
     * @param s float minscale
     */
    public void setMaxScale(float s) {
        if (s < minscale) {
			return;
		}

        maxscale = s;
        if (scale > maxscale) {
            scale = maxscale;
        }
        computeParameters();
        projID = null;
    }

    /**
     * Get the scale of the projection.
     * 
     * @return float scale value
     */
    public float getScale() {
        return scale;
    }

    /**
     * Get the maximum scale of the projection.
     * 
     * @return float max scale value
     */
    public float getMaxScale() {
        return maxscale;
    }

    /**
     * Get minimum scale of the projection.
     * 
     * @return float min scale value
     */
    public float getMinScale() {
        return minscale;
    }

    /**
     * Set center IntPoint of projection.
     * 
     * @param lat float latitude in decimal degrees
     * @param lon float longitude in decimal degrees
     */
    public void setCenter(float lat, float lon) {
        ctrLat = normalize_latitude(ProjMath.degToRad(lat));
        ctrLon = wrap_longitude(ProjMath.degToRad(lon));
        computeParameters();
        projID = null;
    }

    /**
     * Set center IntPoint of projection.
     * 
     * @param pt Node
     */
    public void setCenter(Node pt) {
        setCenter(pt.getLatitude(), pt.getLongitude());
    }

    /**
     * Get center IntPoint of projection.
     * 
     * @return Node center of projection
     */
    public Node getCenter() {
        return new Node(ctrLat, ctrLon, true);
    }

    /**
     * Set projection width.
     * 
     * @param width width of projection screen
     */
    public void setWidth(int width) {
        this.width = width;

        if (this.width < MIN_WIDTH) {

            this.width = MIN_WIDTH;
        }
        computeParameters();
        projID = null;
    }

    /**
     * Set projection height.
     * 
     * @param height height of projection screen
     */
    public void setHeight(int height) {
        this.height = height;
        if (this.height < MIN_HEIGHT) {
            this.height = MIN_HEIGHT;
        }
        computeParameters();
        projID = null;
    }

    /**
     * Get projection width.
     * 
     * @return width of projection screen
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get projection height.
     * 
     * @return height of projection screen
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets all the projection variables at once before calling
     * computeParameters().
     * 
     * @param center Node center
     * @param scale float scale
     * @param width width of screen
     * @param height height of screen
     */
    protected void setParms(Node center, float scale, int width,
                            int height) {
//    	logger.info("setParams " + center + " scale " + scale);
        ctrLat = normalize_latitude(center.radlat);
        ctrLon = wrap_longitude(center.radlon);

        this.scale = scale;
        if (this.scale < minscale) {
            this.scale = minscale;
        } else if (this.scale > maxscale) {
            this.scale = maxscale;
        }

        this.width = width;
        if (this.width < MIN_WIDTH) {
            this.width = MIN_WIDTH;
        }
        this.height = height;
        if (this.height < MIN_HEIGHT) {
            this.height = MIN_HEIGHT;
        }

        computeParameters();
//    	logger.info("setParams ready");
    }

    /**
     * Gets the projection type.
     * 
     * @return int projection type
     */
    public int getProjectionType() {
        return type;
    }

    /**
     * Sets the projection ID used for determining equality. The
     * projection ID String is intern()ed for efficient comparison.
     */
    protected void setProjectionID() {
        projID = (":" + type + ":" + scale + ":" + ctrLat + ":" + ctrLon + ":"
                + width + ":" + height + ":").intern();
    }

    /**
     * Gets the projection ID used for determining equality.
     * 
     * @return the projection ID, as an intern()ed String
     */
    public String getProjectionID() {
        if (projID == null) {
			setProjectionID();
		}
        return projID;
    }

    /**
     * Called when some fundamental parameters change.
     * <p>
     * Each projection will decide how to respond to this change. For
     * instance, they may need to recalculate "constant" paramters
     * used in the forward() and inverse() calls.
     */
    protected abstract void computeParameters();

    /**
     * Sets radian latitude to something sane.
     * <p>
     * Normalizes the latitude according to the particular projection.
     * 
     * @param lat float latitude in radians
     * @return float latitude (-PI/2 &lt;= y &lt;= PI/2)
     * @see ProjMath#normalize_latitude(float, float)
     * @see Node#normalize_latitude(float)
     */
    public abstract float normalize_latitude(float lat);

    /**
     * Sets radian longitude to something sane.
     * 
     * @param lon float longitude in radians
     * @return float longitude (-PI &lt;= x &lt; PI)
     * @see ProjMath#wrap_longitude(float)
     * @see Node#wrap_longitude(float)
     */
    public final static float wrap_longitude(float lon) {
        return ProjMath.wrap_longitude(lon);
    }

    /**
     * Stringify the projection.
     * 
     * @return stringified projection
     * @see #getProjectionID
     */
    public String toString() {
        return (" radius=" + planetRadius + " ppm=" + pixelsPerMeter
                + " center(" + ProjMath.radToDeg(ctrLat) + ","
                + ProjMath.radToDeg(ctrLon) + ") scale=" + scale + " maxscale="
                + maxscale + " minscale=" + minscale + " width=" + width
                + " height=" + height + "]");
    }

    /**
     * Test for equality.
     * 
     * @param o Object to compare.
     * @return boolean comparison
     */
    public boolean equals(Object o) {
        if (o == null) {
			return false;
		}
        if (o instanceof Projection) {
			return getProjectionID() == ((Projection) o).getProjectionID();
		}
        return false;
    }

    /**
     * Return hashcode value of projection.
     * 
     * @return int hashcode
     */
    public int hashCode() {
        return getProjectionID().hashCode();
    }



    /**
     * Checks if a Node is plot-able.
     * <p>
     * Call this to check and see if a Node can be plotted.
     * This is meant to be used for checking before projecting and
     * rendering IntPoint objects (bitmaps for instance).
     * 
     * @param llIntPoint Node
     * @return boolean
     */
    public boolean isPlotable(Node llIntPoint) {
        return isPlotable(llIntPoint.getLatitude(), llIntPoint.getLongitude());
    }

    /**
     * Forward project a Node.
     * <p>
     * Forward projects a LatLon IntPoint into XY space. Returns a IntPoint.
     * 
     * @param llp Node to be projected
     * @return IntPoint (new)
     */
    public final IntPoint forward(Node llp) {
        return forward(llp.radlat, llp.radlon, new IntPoint(0, 0), true);
    }

    /**
     * Forward project lat,lon coordinates.
     * 
     * @param lat float latitude in decimal degrees
     * @param lon float longitude in decimal degrees
     * @return IntPoint (new)
     */
    public final IntPoint forward(float lat, float lon) {
        return forward(lat, lon, new IntPoint(0, 0));
    }

    /**
     * Inverse project a IntPoint from x,y space to LatLon space.
     * 
     * @param IntPoint x,y IntPoint
     * @return Node (new)
     */
    public final Node inverse(IntPoint IntPoint) {
        return inverse(IntPoint, new Node());
    }

    /**
     * Inverse project x,y coordinates.
     * 
     * @param x integer x coordinate
     * @param y integer y coordinate
     * @return Node (new)
     * @see #inverse(IntPoint)
     */
    public final Node inverse(int x, int y) {
        return inverse(x, y, new Node());
    }

    public void pan(float Az, float c) {
        setCenter(spherical_between(ctrLat,
                ctrLon,
                ProjMath.degToRad(c),
                ProjMath.degToRad(Az)));
    }

    /**
     * Pan the map/projection.
     * <ul>
     * <li><code>pan(±180, c)</code> pan south
     * <li><code>pan(-90, c)</code> pan west
     * <li><code>pan(0, c)</code> pan north
     * <li><code>pan(90, c)</code> pan east
     * </ul>
     * 
     * @param Az azimuth "east of north" in decimal degrees:
     *        <code>-180 &lt;= Az &lt;= 180</code>
     */
    public void pan(float Az) {
        pan(Az, 45f);
    }

    /**
     * pan the map northwest.
     */
    final public void panNW() {
        pan(-45f);
    }

    final public void panNW(float c) {
        pan(-45f);
    }

    /**
     * pan the map north.
     */
    final public void panN() {
        pan(0f);
    }

    final public void panN(float c) {
        pan(0f);
    }

    /**
     * pan the map northeast.
     */
    final public void panNE() {
        pan(45f);
    }

    final public void panNE(float c) {
        pan(45f);
    }

    /**
     * pan the map east.
     */
    final public void panE() {
        pan(90f);
    }

    final public void panE(float c) {
        pan(90f);
    }

    /**
     * pan the map southeast.
     */
    final public void panSE() {
        pan(135f);
    }

    final public void panSE(float c) {
        pan(135f);
    }

    /**
     * pan the map south.
     */
    final public void panS() {
        pan(180f);
    }

    final public void panS(float c) {
        pan(180f);
    }

    /**
     * pan the map southwest.
     */
    final public void panSW() {
        pan(-135f);
    }

    final public void panSW(float c) {
        pan(-135f);
    }

    /**
     * pan the map west.
     */
    final public void panW() {
        pan(-90f);
    }

    final public void panW(float c) {
        pan(-90f);
    }

//    /**
//     * Draw the background for the projection.
//     * 
//     * @param g Graphics2D
//     * @param p Paint to use for the background
//     */
//    abstract public void drawBackground(Graphics2D g, Paint p);
//
//    /**
//     * Assume that the Graphics has been set with the Paint/Color
//     * needed, just render the shape of the background.
//     */
//    abstract public void drawBackground(Graphics g);

    /**
     * Get the name string of the projection.
     */
    public String getName() {
        return "Proj";
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
     */
    public float getScale(Node ll1, Node ll2, IntPoint IntPoint1,
                          IntPoint IntPoint2) {

        try {

            float deltaDegrees;
            float pixPerDegree;
            int deltaPix;
            float dx = Math.abs(IntPoint2.getX() - IntPoint1.getX());
            float dy = Math.abs(IntPoint2.getY() - IntPoint1.getY());

            if (dx < dy) {
                float dlat = Math.abs(ll1.getLatitude() - ll2.getLatitude());
                deltaDegrees = dlat;
                deltaPix = getHeight();

                // This might not be correct for all projection types
                pixPerDegree = getPlanetPixelCircumference() / 360f;
            } else {
                float dlon;
                float lat1, lon1, lon2;

                // IntPoint1 is to the right of IntPoint2. switch the
                // Nodes so that ll1 is west (left) of ll2.
                if (IntPoint1.getX() > IntPoint2.getX()) {
                    lat1 = ll1.getLatitude();
                    lon1 = ll1.getLongitude();
                    ll1.setLatLon(ll2);
                    ll2.setLatLon(lat1, lon1);
                }

                lon1 = ll1.getLongitude();
                lon2 = ll2.getLongitude();

                // allow for crossing dateline
                if (lon1 > lon2) {
                    dlon = (180 - lon1) + (180 + lon2);
                } else {
                    dlon = lon2 - lon1;
                }

                deltaDegrees = dlon;
                deltaPix = getWidth();

                // This might not be correct for all projection types
                pixPerDegree = getPlanetPixelCircumference() / 360f;
            }

            // The new scale...
            return pixPerDegree / (deltaPix / deltaDegrees);
        } catch (NullPointerException npe) {
        	System.out.print("ProjMath.getScale(): caught null IntPointer exception.");
            return Float.MAX_VALUE;
        }
    }
    /**
     * Calculate IntPoint at azimuth and distance from another IntPoint.
     * <p>
     * Returns a LatLonIntPoint at arc distance `c' in direction `Az'
     * from start IntPoint.
     * <p>
     * 
     * @param phi1 latitude in radians of start IntPoint
     * @param lambda0 longitude in radians of start IntPoint
     * @param c arc radius in radians (0 &lt; c &lt;= PI)
     * @param Az azimuth (direction) east of north (-PI &lt;= Az &lt;
     *        PI)
     * @return LatLonIntPoint
     *  
     */
    final public static Node spherical_between(float phi1,
                                                      float lambda0, float c,
                                                      float Az) {
        float cosphi1 = (float) Math.cos(phi1);
        float sinphi1 = (float) Math.sin(phi1);
        float cosAz = (float) Math.cos(Az);
        float sinAz = (float) Math.sin(Az);
        float sinc = (float) Math.sin(c);
        float cosc = (float) Math.cos(c);

        return new Node(ProjMath.radToDeg(
        		MoreMath.asin(sinphi1 * cosc + cosphi1 * sinc * cosAz)),
                (float) ProjMath.radToDeg(MoreMath.atan2(sinc * sinAz,
                                  cosphi1 * cosc - sinphi1 * sinc * cosAz)
                                  + lambda0));
    }

}
