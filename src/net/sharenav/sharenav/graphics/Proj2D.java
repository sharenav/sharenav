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

package net.sharenav.sharenav.graphics;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.util.IntPoint;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;


/**
 * Implements the Mercator projection.
 */
public class Proj2D implements Projection {
	
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
    private float minLat=Float.MAX_VALUE;
    private float maxLat=-Float.MAX_VALUE;
    private float minLon=Float.MAX_VALUE;
    private float maxLon=-Float.MAX_VALUE;
	private IntPoint	panP=new IntPoint();
	private IntPoint	centerScreen=new IntPoint();

	
	protected static final Logger logger = Logger.getInstance(Proj2D.class,Logger.INFO);


	public Proj2D(Node center, float scale, int width, int height) {
        this.ctrLat = center.radlat;
        this.ctrLon = center.radlon;
		this.scale = scale;
		this.width = width;
		this.height = height;
		//#debug debug
		logger.debug("init projMU s=" + scale + " w="+ width + " h=" + height);		
		computeParameters();
    }

	
	protected void computeParameters(){
		planetPixelRadius = MoreMath.PLANET_RADIUS * pixelsPerMeter;
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
    	Node ret=new Node();
    	inverse(0, 0, ret);
    	extendMinMax(ret);
    	inverse(0, height, ret);
    	extendMinMax(ret);
    	inverse(width, 0, ret);
    	extendMinMax(ret);
    	inverse(width, height, ret);
    	extendMinMax(ret);
    	centerScreen=new IntPoint(wx, hy);
    	//#mdebug debug
    	logger.debug("scaled lat=" + scaled_lat);
    	logger.debug("scaled_Radius=" + scaled_radius);
    	logger.debug("tanCtrLat=" + tanCtrLat);
    	logger.debug("asinh_of_tanCtrLat=" + asinh_of_tanCtrLat);
    	logger.debug("Bounds: " + maxLat + " " + maxLon + " " + minLat + " " + minLon);
    	//#enddebug
	}
	
	private void extendMinMax(Node n) {
		if (n.radlat > maxLat){
			maxLat=n.radlat;
		}
		if (n.radlat < minLat){
			minLat=n.radlat;
		}
		if (n.radlon > maxLon){
			maxLon=n.radlon;
		}
		if (n.radlon < minLon){
			minLon=n.radlon;
		}
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
	//     two threads are using this 
	public IntPoint forward(short lat, short lon, IntPoint p, SingleTile t) {
    	if (t != tileCache) {
    		ctrLonRel = (int)((ctrLon - t.centerLon) * MoreMath.FIXPT_MULT);
    		ctrLatRel = (int)((ctrLat - t.centerLat) * MoreMath.FIXPT_MULT);
    		scaled_radius_rel = (scaled_radius * MoreMath.FIXPT_MULT_INV);
    		scaled_lat_rel = (scaled_lat * MoreMath.FIXPT_MULT_INV);    		
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
			float dlat = Math.abs(ll1.getLatDeg() - ll2.getLatDeg());
			float dlon = Math.abs(ll1.getLonDeg() - ll2.getLonDeg());
			
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
    	if (llp == null) {
    		llp = new Node();    	    	
    	}
        llp.setLatLonRad((-1 * (y - hy) / scaled_lat + ctrLat),
                ((x - wx) / scaled_radius) + ctrLon);        
        
        return llp;
	}
	
	
	public boolean isPlotable(short lat, short lon, SingleTile t) {
		return isPlotable(lat*MoreMath.FIXPT_MULT_INV+t.centerLat,
				lon*MoreMath.FIXPT_MULT_INV+t.centerLon);
	}

	public boolean isPlotable(float lat, float lon) {
		if (lat < minLat) return false;
		if (lat > maxLat) return false;
		if (lon < minLon) return false;
		if (lon > maxLon) return false;
		return true;
	}
	
    private Node inverse_full(int x, int y, Node llp) {
        // convert from screen to world coordinates
        x -= wx;
        float y_=(((hy-y))/scaled_radius)+asinh_of_tanCtrLat ;

        llp.setLatLonRad(MoreMath.atan(MoreMath.sinh(y_)),
                (x / scaled_radius) + ctrLon);
        return llp;
    }


	public float getMinLat() {
		return minLat;
	}


	public float getMaxLat() {
		return maxLat;
	}


	public float getMinLon() {
		return minLon;
	}


	public float getMaxLon() {
		return maxLon;
	}

	public void pan(Node n, int xd, int yd) {
		forward(n,panP);
		inverse((width*xd/100)+panP.x,( height*yd/100)+panP.y, n);		
	}


	public float getCourse() {
		return 0;
	}
	
	public String toString() {
		return "Proj2D " + (ctrLat * MoreMath.FAC_RADTODEC) + "/"+ (ctrLon * MoreMath.FAC_RADTODEC) + " s:" + scale;
	}
	public boolean isOrthogonal() {
		return true;
	}
	   /** Calculate the end points for 2 paralell lines with distance w which
	    * has the origin line (Xpoint[i]/Ypoint[i]) to (Xpoint[i+1]/Ypoint[i+1])
		* as centre line
	    * @param xPoints list off all XPoints for this Way
	    * @param yPoints list off all YPoints for this Way
	    * @param i the index out of (X/Y) Point for the line segement we looking for
	    * @param w the width of the segment out of the way
	    * @param p1 left start point
	    * @param p2	right start point
	    * @param p3 left end point
	    * @param p4 right end point
	    * @return the angle of the line in radians ( -Pi .. +Pi )
	    */
		public float getParLines(int xPoints[], int yPoints[], int i, int w,
				IntPoint p1, IntPoint p2, IntPoint p3, IntPoint p4) {
			int i1 = i + 1;
			int dx = xPoints[i1] - xPoints[i];
			int dy = yPoints[i1] - yPoints[i];
			int l2 = dx * dx + dy * dy;
			float l2f = (float) Math.sqrt(l2);
			float lf = w / l2f;
			int xb = (int) ((Math.abs(lf * dy)) + 0.5f);
			int yb = (int) ((Math.abs(lf * dx)) + 0.5f);
			int rfx = 1;
			int rfy = 1;
			if (dy < 0) {
				rfx = -1;
			}
			if (dx > 0) {
				rfy = -1;
			}
			int xd = rfx * xb;
			int yd = rfy * yb;
			p1.x = xPoints[i] + xd;	
			p1.y = yPoints[i] + yd;
			p2.x = xPoints[i] - xd;
			p2.y = yPoints[i] - yd;
			p3.x = xPoints[i1] + xd;
			p3.y = yPoints[i1] + yd;
			p4.x = xPoints[i1] - xd;
			p4.y = yPoints[i1] - yd;
			if (dx != 0) {
				return (MoreMath.atan(1f * dy / dx));
			} else {
				if (dy > 0) {
					return MoreMath.PiDiv2;
				} else {
					return -MoreMath.PiDiv2;
				}
			}
		}


	public IntPoint getImageCenter() {
		return centerScreen;
	}

}
