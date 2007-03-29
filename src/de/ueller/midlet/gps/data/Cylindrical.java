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


//import de.ueller.m.midlet.gps.Logger;


/**
 * Base of all cylindrical projections.
 * <p>
 * 
 * @see Projection
 * @see Proj
 * @see Mercator
 * @see CADRG
 *  
 */
public abstract class Cylindrical extends Proj {

    // used for calculating wrapping of ArrayList graphics
    protected IntPoint world; // world width in pixels.
    protected float half_world; // world.x / 2
//	private final static Logger logger=Logger.getInstatance(Cylindrical.class);

    /**
     * Construct a cylindrical projection.
     * <p>
     * 
     * @param center Node center of projection
     * @param scale float scale of projection
     * @param width width of screen
     * @param height height of screen
     * @param type projection type
     *  
     */
    public Cylindrical(Node center, float scale, int width, int height,
            int type) {
        super(center, scale, width, height, type);
//        logger.info("init");
    }

    /**
     * Return stringified description of this projection.
     * <p>
     * 
     * @return String
     * @see Projection#getProjectionID
     */
    public String toString() {
        return " world(" + world.getX() + "," + world.getY() + ")" + super.toString();
    }

    /**
     * Called when some fundamental parameters change.
     * <p>
     * Each projection will decide how to respond to this change. For
     * instance, they may need to recalculate "constant" parameters
     * used in the forward() and inverse() calls.
     * <p>
     */
    protected void computeParameters() {
//    	logger.info("enter computeParameters()");
        planetPixelRadius = planetRadius * pixelsPerMeter;
//    	logger.info("calc planetPixelCircumference");
        planetPixelCircumference = MoreMath.TWO_PI * planetPixelRadius;

        // minscale is the minimum scale allowable (before integer
        // wrapping
        // can occur)
//    	logger.info("calc minscale");
        minscale = (float) Math.ceil(planetPixelCircumference
                / (int) Integer.MAX_VALUE);
        if (minscale < 1)
            minscale = 1;
        if (scale < minscale)
            scale = minscale;

//         maxscale = scale at which world circumference fits in
        // window
//    	logger.info("calc maxscale");
        maxscale = planetPixelCircumference / width;
//        logger.info("calc maxscale 1");
        if (maxscale < minscale) {
            maxscale = minscale;
        }
//        logger.info("calc maxscale 2");
        if (scale > maxscale) {
            scale = maxscale;
        }
//        logger.info("calc scaled_radius");
        scaled_radius = planetPixelRadius / scale;

//        logger.info("create world");
        if (world == null)
            world = new IntPoint(0, 0);

        // width of the world in pixels at current scale
//    	logger.info("calc scale");
        world.setX((planetPixelCircumference / scale));
        half_world = world.getX() / 2;

//        // calculate cutoff scale for XWindows workaround
//        XSCALE_THRESHOLD = (int) (planetPixelCircumference / 64000);//fudge
//                                                                    // it a
//                                                                    // little
//                                                                    // bit
//    	logger.info("exit computeParameters()");
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
        if (MoreMath.approximately_equal(Math.abs(Az), 180f, 0.01f)) {
            setCenter(inverse(width / 2, height));//south
        } else if (MoreMath.approximately_equal(Az, -135f, 0.01f)) {
            setCenter(inverse(0, height));//southwest
        } else if (MoreMath.approximately_equal(Az, -90f, 0.01f)) {
            setCenter(inverse(0, height / 2));//west
        } else if (MoreMath.approximately_equal(Az, -45f, 0.01f)) {
            setCenter(inverse(0, 0));//northwest
        } else if (MoreMath.approximately_equal(Az, 0f, 0.01f)) {
            setCenter(inverse(width / 2, 0));//north
        } else if (MoreMath.approximately_equal(Az, 45f, 0.01f)) {
            setCenter(inverse(width, 0));//northeast
        } else if (MoreMath.approximately_equal(Az, 90f, 0.01f)) {
            setCenter(inverse(width, height / 2));//east
        } else if (MoreMath.approximately_equal(Az, 135f, 0.01f)) {
            setCenter(inverse(width, height));//southeast
        } else {
            super.pan(Az);
        }
    }

    /**
     * Get the upper left (northwest) point of the projection.
     * <p>
     * Returns the upper left point (or closest equivalent) of the
     * projection based on the center point and height and width of
     * screen.
     * <p>
     * 
     * @return Node
     *  
     */
    public Node getUpperLeft() {
        return inverse(0, 0);
    }

    /**
     * Get the lower right (southeast) point of the projection.
     * <p>
     * Returns the lower right point (or closest equivalent) of the
     * projection based on the center point and height and width of
     * screen.
     * <p>
     * 
     * @return Node
     *  
     */
    public Node getLowerRight() {
        return inverse(width, height);
    }



    /**
     * Get the name string of the projection.
     */
    public String getName() {
        return "Cylindrical";
    }
}
