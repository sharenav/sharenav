package de.ueller.midlet.gps.data;

import de.ueller.gpsMid.mapData.SingleTile;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */






public interface Projection {
	
	public static final float PLANET_RADIUS=6378137.0f;
	public static final int DEFAULT_PIXEL_PER_METER = 3272;
	
    /**
     * Get the scale.
     * 
     * @return float scale
     */
    public float getScale();

    /**
     * Get the projection ID string.
     * 
     * @return String projID
     */
    public String getProjectionID();



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
     * Forward projects lat,lon coordinates into XY space and returns
     * a IntPoint.
     * 
     * @param lat float latitude in radians
     * @param lon float longitude in radians
     * @param pt Resulting XY IntPoint
     * @see #forward(float,float,IntPoint)
     * @return IntPoint pt
     */
    public IntPoint forward(float lat, float lon, IntPoint pt);
    
    /**
     * Forward projects lat,lon coordinates into XY space and returns
     * a IntPoint. The lat, lon coordinates are in  16bit
     * coordinates relative to the single tile t
     * 
     * @param lat short relative coordinate
     * @param lon short relative coordinate
     * @param pt Resulting XY IntPoint
     * @param t SingleTile to which the coordinates are relative
     * @see #forward(float,float,IntPoint)
     * @return IntPoint pt
     */
    public IntPoint forward(short lat, short lon, IntPoint pt, SingleTile t);




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

    /**
     * calculate the new center for pan
     * @param n the node (old center) that has to be moved
     * @param xd percent that has to be moved in x-direction
     * @param yd percent that has to be moved in y-direction
     */
    public void pan(Node n,int xd, int yd);
    
    public int getPPM();
    
	public float getMinLat();


	public float getMaxLat();


	public float getMinLon();


	public float getMaxLon();
	
	public float getCourse();

	public String toString();
}
