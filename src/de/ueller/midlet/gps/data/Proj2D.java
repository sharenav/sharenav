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

import de.ueller.gpsMid.mapData.SingleTile;


/**
 * Implements the Mercator projection.
 */
public final class Proj2D implements Projection {
	
    protected float ctrLat = 0.0f; // center latitude in radians
    protected float ctrLon = 0.0f; // center longitude in radians
	private final float	scale;
	private final int	width;
	private final int	height;
	private float	scaled_radius;
	private float	planetPixelRadius;
	private int	pixelsPerMeter=DEFAULT_PIXEL_PER_METER;
    protected float planetPixelCircumference = MoreMath.TWO_PI
    * planetPixelRadius; // EARTH_PIX_CIRCUMFERENCE
	private float	scaled_lat;
	private int hy, wx;
	private float	tanCtrLat;
	private float	asinh_of_tanCtrLat;
	private SingleTile tileCache;
    private int ctrLonRel;
    private int ctrLatRel;
    private float scaled_radius_rel;    
    private float scaled_lat_rel;


	public Proj2D(Node center, float scale, int width, int height) {
        this.ctrLat = center.radlat;
        this.ctrLon = center.radlon;
		this.scale = scale;
		this.width = width;
		this.height = height;
		computeParameters();
    }

	
	protected void computeParameters(){
		planetPixelRadius = PLANET_RADIUS * pixelsPerMeter;
		scaled_radius = planetPixelRadius / scale;
        planetPixelCircumference = MoreMath.TWO_PI * planetPixelRadius;
        // do some precomputation of stuff
        tanCtrLat = (float) Math.tan(ctrLat);
        asinh_of_tanCtrLat = MoreMath.asinh(tanCtrLat);

        // compute the offsets
        hy = height / 2;
        wx = width / 2;

    	Node n1 = new Node();
    	Node n2 = new Node();        	
    	n1 = inverse_full(width/2, 0, n1);
    	n2 = inverse_full(width/2, height, n2);
    	scaled_lat = height/(n1.radlat - n2.radlat);        	

	}

	public IntPoint forward(Node n) {
        return forward(n.radlat, n.radlon, new IntPoint(0, 0));
	}
	
    public IntPoint forward(Node pt, IntPoint p) {
    	return forward(pt.radlat,pt.radlon,p);
    }

	public IntPoint forward(float lat, float lon, IntPoint p) {
	    p.setX((int) (scaled_radius * ProjMath.wrap_longitude(lon - ctrLon) + 0.49f) + wx);        
        p.setY(hy - (int)(scaled_lat * (lat - ctrLat) + 0.49f));
        return p;
    	}

	//TODO check if this doesn't cause any concurrent modification if
	//     two thread are using this 
	public IntPoint forward(short lat, short lon, IntPoint p, SingleTile t) {
    	if (t != tileCache) {
    		ctrLonRel = (int)((ctrLon - t.centerLon)*SingleTile.fpm);
    		ctrLatRel = (int)((ctrLat - t.centerLat)*SingleTile.fpm);
    		scaled_radius_rel = (scaled_radius*SingleTile.fpminv);
    		scaled_lat_rel = (scaled_lat*SingleTile.fpminv);    		
    		tileCache = t;    		
    	}         
        p.setX(((int)((scaled_radius_rel * (lon - ctrLonRel))) + wx));        
        p.setY( hy - (int)(scaled_lat_rel * (lat - ctrLatRel)));        
        return p;
	}

	public int getPPM() {
		return pixelsPerMeter;
	}

	public String getProjectionID() {
		return "2DNorthUp";
	}

	public float getScale() {
		return scale;
	}

	public float getScale(Node ll1, Node ll2, IntPoint IntPoint1, IntPoint IntPoint2) {
		try {

			float deltaDegrees;
			float pixPerDegree;
			int deltaPix;
			int dx = Math.abs(IntPoint2.getX() - IntPoint1.getX());
			int dy = Math.abs(IntPoint2.getY() - IntPoint1.getY());
			float dlat = Math.abs(ll1.getLatitude() - ll2.getLatitude());
			float dlon = Math.abs(ll1.getLongitude() - ll2.getLongitude());
			
			if (dlon/dx < dlat/dy) {
				deltaDegrees = dlat;
				deltaPix = dy;                
			} else {            
				deltaDegrees = dlon;
				deltaPix = dx;
			}    
			// This might not be correct for all projection types
			pixPerDegree = planetPixelCircumference / 360f;            
			// The new scale...
			return pixPerDegree / (deltaPix / deltaDegrees);            
		} catch (NullPointerException npe) {
			//System.out.print("ProjMath.getScale(): caught null IntPointer exception.");
			return Float.MAX_VALUE;
		}
    }

	public Node inverse(int x, int y, Node llp) {
    	if (llp==null) llp = new Node();    	    	
        llp.setLatLon((-1*(y - hy)/scaled_lat + ctrLat),
                ((x - wx) / scaled_radius) + ctrLon,
                true);        
        
        return llp;
	}

	public boolean isPlotable(float lat, float lon) {
		return true;
	}
	
    private Node inverse_full(int x, int y, Node llp) {
        // convert from screen to world coordinates
        x -= wx;
        float y_=(((hy-y))/scaled_radius)+asinh_of_tanCtrLat ;

        llp.setLatLon(MoreMath.atan(MoreMath.sinh(y_)),
                (x / scaled_radius) + ctrLon,
                true);
        return llp;
    }


}
