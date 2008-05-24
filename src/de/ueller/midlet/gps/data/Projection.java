package de.ueller.midlet.gps.data;

import de.ueller.gpsMid.mapData.SingleTile;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */






public interface Projection {
    /**
     * Get the scale.
     * 
     * @return float scale
     */
    public float getScale();

    /**
     * Get the maximum scale.
     * 
     * @return float maxscale
     */
    public float getMaxScale();

    /**
     * Get the minimum scale.
     * 
     * @return float minscale
     */
    public float getMinScale();

    /**
     * Get the center LatLonIntPoint.
     * 
     * @return center IntPoint
     */
    public Node getCenter();

    /**
     * Get the width of the map.
     * 
     * @return int width.
     */
    public int getWidth();

    /**
     * Get the height of the map.
     * 
     * @return int height.
     */
    public int getHeight();

    /**
     * Get the type of projection.
     * 
     * @return int type
     */
    public int getProjectionType();

    /**
     * Get the projection ID string.
     * 
     * @return String projID
     */
    public String getProjectionID();

    /**
     * Get the upper left (northwest) IntPoint of the projection.
     * <p>
     * Returns the upper left IntPoint (or closest equivalent) of the
     * projection based on the center IntPoint and height and width of
     * screen.
     * <p>
     * This is trivial for most cylindrical projections, but much more
     * complicated for azimuthal projections.
     * 
     * @return LatLonIntPoint
     */
    public Node getUpperLeft();

    /**
     * Get the lower right (southeast) IntPoint of the projection.
     * <p>
     * Returns the lower right IntPoint (or closest equivalent) of the
     * projection based on the center IntPoint and height and width of
     * screen.
     * <p>
     * This is trivial for most cylindrical projections, but much more
     * complicated for azimuthal projections.
     * 
     * @return Node
     */
    public Node getLowerRight();

    /**
     * Checks if a Node is plot-able.
     * <p>
     * Call this to check and see if a Node can be plotted.
     * This is meant to be used for checking before projecting and
     * rendering IntPoint objects (bitmaps or text objects tacked at a
     * Node for instance).
     * 
     * @param llIntPoint Node
     * @return boolean
     */
    public boolean isPlotable(Node llIntPoint);

    /**
     * Checks if a Node is plot-able.
     * <p>
     * Call this to check and see if a Node can be plotted.
     * This is meant to be used for checking before projecting and
     * rendering IntPoint objects (bitmaps or text objects tacked at a
     * Node for instance).
     * 
     * @param lat float latitude in decimal degrees
     * @param lon float longitude in decimal degrees
     * @return boolean
     */
    public boolean isPlotable(float lat, float lon);

    /**
     * Forward project a Node into XY space.
     * 
     * @param llIntPoint Node
     * @return IntPoint (new)
     */
    public IntPoint forward(Node llIntPoint);

    /**
     * Forward projects a Node into XY space and return a
     * IntPoint.
     * 
     * @param llp Node to be projected
     * @param pt Resulting XY IntPoint
     * @return IntPoint pt
     */
    public IntPoint forward(Node llp, IntPoint pt);

    /**
     * Forward project lat,lon coordinates into xy space.
     * 
     * @param lat float latitude in decimal degrees
     * @param lon float longitude in decimal degrees decimal degrees
     * @return IntPoint (new)
     */
    public IntPoint forward(float lat, float lon);
//    /**
//     * Forward project lat,lon coordinates into xy space.
//     * 
//     * @param lat float latitude in rad degrees
//     * @param lon float longitude in rad degrees
//     * @param dummy tha indicates rad param
//     */
//    public IntPoint forward(float lat, float lon,boolean i);

    /**
     * Forward projects lat,lon coordinates into XY space and returns
     * a IntPoint.
     * 
     * @param lat float latitude in decimal degrees
     * @param lon float longitude in decimal degrees
     * @param pt Resulting XY IntPoint
     * @return IntPoint pt
     */
    public IntPoint forward(float lat, float lon, IntPoint pt);

    /**
     * Forward projects lat,lon coordinates into XY space and returns
     * a IntPoint.
     * 
     * @param lat float latitude in radians
     * @param lon float longitude in radians
     * @param pt Resulting XY IntPoint
     * @param isRadian placeholder argument indicating that lat,lon
     *        arguments are in radians (can be true or false)
     * @see #forward(float,float,IntPoint)
     * @return IntPoint pt
     */
    public IntPoint forward(float lat, float lon, IntPoint pt, boolean isRadian);
    
    /**
     * Forward projects lat,lon coordinates into XY space and returns
     * a IntPoint. The lat, lon coordinates are in  16bit
     * coordinates relative to the single tile t
     * 
     * @param lat short relative coordinate
     * @param lon short relative coordinate
     * @param pt Resulting XY IntPoint
     * @param isRadian placeholder argument indicating that lat,lon
     *        arguments are in radians (can be true or false)
     * @param t SingleTile to which the coordinates are relative
     * @see #forward(float,float,IntPoint)
     * @return IntPoint pt
     */
    public IntPoint forward(short lat, short lon, IntPoint pt, boolean isRadian, SingleTile t);

    /**
     * Inverse project a IntPoint.
     * 
     * @param IntPoint XY IntPoint
     * @return Node (new)
     */
    public Node inverse(IntPoint IntPoint);

    /**
     * Inverse project a IntPoint with llpt.
     * 
     * @param IntPoint x,y IntPoint
     * @param llpt resulting Node
     * @return Node llpt
     */
    public Node inverse(IntPoint IntPoint, Node llpt);

    /**
     * Inverse project x,y coordinates.
     * 
     * @param x
     * @param y
     * @return Node (new)
     */
    public Node inverse(int x, int y);

    /**
     * Inverse project x,y coordinates into a Node.
     * 
     * @param x integer x coordinate
     * @param y integer y coordinate
     * @param llpt Node
     * @return Node llpt
     * @see Proj#inverse(IntPoint)
     */
    public Node inverse(int x, int y, Node llpt);

    /**
     * Pan the map/projection.
     * <ul>
     * <li><code>pan(±180, c)</code> pan south `c' degrees
     * <li><code>pan(-90, c)</code> pan west `c' degrees
     * <li><code>pan(0, c)</code> pan north `c' degrees
     * <li><code>pan(90, c)</code> pan east `c' degrees
     * </ul>
     * 
     * @param Az azimuth "east of north" in decimal degrees:
     *        <code>-180 &lt;= Az &lt;= 180</code>
     * @param c arc distance in decimal degrees
     */
    public void pan(float Az, float c);

    /**
     * Pan the map/projection.
     * <ul>
     * <li><code>pan(±180)</code> pan south
     * <li><code>pan(-90)</code> pan west
     * <li><code>pan(0)</code> pan north
     * <li><code>pan(90)</code> pan east
     * </ul>
     * 
     * @param Az azimuth "east of north" in decimal degrees:
     *        <code>-180 &lt;= Az &lt;= 180</code>
     */
    public void pan(float Az);


    /**
     * Get the String used as a name, usually as a type.
     */
    public String getName();

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
                          IntPoint IntPoint2);

    public int getPPM();
}
