package de.ueller.midlet.gps.data;

import de.ueller.gpsMid.mapData.SingleTile;

public class Proj2DMoveUp  implements Projection {
	private float upDir;

	private int course;
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
    private float sinRoh;
    private float cosRoh;
    private float minLat=Float.MAX_VALUE;
    private float maxLat=-Float.MAX_VALUE;
    private float minLon=Float.MAX_VALUE;
    private float maxLon=-Float.MAX_VALUE;


	private IntPoint	panP=new IntPoint();



	public Proj2DMoveUp(Node center, int upDir, float scale, int width, int height) {
        this.upDir = ProjMath.degToRad(upDir);
        this.course=upDir;
		this.ctrLat = center.radlat;
        this.ctrLon = center.radlon;
		this.scale = scale;
		this.width = width;
		this.height = height;
		computeParameters();
//		System.out.println(center + " " + upDir + " " + scale );
    }

	public void computeParameters(){
		planetPixelRadius = PLANET_RADIUS * pixelsPerMeter;
		scaled_radius = planetPixelRadius / scale;
        planetPixelCircumference = MoreMath.TWO_PI * planetPixelRadius;
        // do some precomputation of stuff
        tanCtrLat = (float) Math.tan(ctrLat);
        asinh_of_tanCtrLat = MoreMath.asinh(tanCtrLat);

        // compute the offsets
        hy = height / 2;
        wx = width / 2;
    	cosRoh=(float) Math.cos(upDir);
    	sinRoh=(float) Math.sin(upDir);

    	Node n1 = new Node();
    	Node n2 = new Node();        	
    	n1 = inverse_wo_rot(width/2, 0, n1);
    	n2 = inverse_wo_rot(width/2, height, n2);
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
    	
//    	System.out.println("scaled lat=" + scaled_lat);
//    	System.out.println("scaled_Radius=" + scaled_radius);
//    	System.out.println("tanCtrLat=" + tanCtrLat);
//    	System.out.println("asinh_of_tanCtrLat=" + asinh_of_tanCtrLat);
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

	public float getUpDir() {
		return (float)upDir;
	}

	public void setUpDir(float upDir) {
		this.upDir = upDir;
	}

	
	
//	private void rotate(float x,float y,float[] ret){
//		ret[0]=x*cosRoh - y*sinRoh;
//		ret[1]=x*sinRoh - y*cosRoh;
//	}

	public IntPoint forward(Node n) {
        return forward(n.radlat, n.radlon, new IntPoint(0, 0));
	}
	
    public IntPoint forward(Node pt, IntPoint p) {
    	return forward(pt.radlat,pt.radlon,p);
    }
    public IntPoint forward(float lat, float lon, IntPoint p) {
        // same as forward_x and forward_y, and convert to screen
        // coords
        float px=(scaled_radius * ProjMath.wrap_longitude(lon - ctrLon));
        float py=(scaled_radius * (MoreMath.asinh((float) Math.tan(lat)) - asinh_of_tanCtrLat));
		p.setX((int)(px*cosRoh - py*sinRoh + 0.5f) + wx);
		p.setY(hy - (int)(px*sinRoh + py*cosRoh + 0.5f));

        return p;
    }


	public IntPoint forward_app(float lat, float lon, IntPoint p) {
	    float px=scaled_radius * ProjMath.wrap_longitude(lon - ctrLon);        
        float py=scaled_lat * (lat - ctrLat);
		p.setX((int)(px*cosRoh - py*sinRoh + 0.5f) + wx);
		p.setY(hy - (int)(px*sinRoh + py*cosRoh + 0.5f));
//		System.out.println("f x="+px+" y="+py+" x'="+p.getX()+" y'="+p.getY());
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
    	float px=scaled_radius_rel * (lon - ctrLonRel);        
    	float py=scaled_lat_rel * (lat - ctrLatRel);        
		p.setX((int)(px*cosRoh - py*sinRoh + 0.5f) + wx);
		p.setY(hy - (int)(px*sinRoh + py*cosRoh + 0.5f));
//		System.out.println("tf x="+px+" y="+py+" x'="+p.getX()+" y'="+p.getY());
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

	//TODO: needs adaption to rotated map???
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

	public Node inverse_app(int x, int y, Node llp) {
    	if (llp==null) llp = new Node();    	    	
    	x -= wx;
    	y = hy - y;
		float x1= y*sinRoh + x*cosRoh;
		float y1= -x*sinRoh + y*cosRoh;
       llp.setLatLon((y1/scaled_lat + ctrLat),
                (x1 / scaled_radius) + ctrLon,
                true);        
        
        return llp;
	}

	public boolean isPlotable(float lat, float lon) {
		if (lat < minLat) return false;
		if (lat > maxLat) return false;
		if (lon < minLon) return false;
		if (lon > maxLon) return false;
		return true;
	}
	
    public Node inverse(int x, int y, Node llp) {
        // convert from screen to world coordinates
//    	System.out.println("Inverse Full("+x+","+y+")");
    	x -= wx;
    	y = hy - y;
//    	System.out.println("sinRoh=" + sinRoh);
//    	System.out.println("cosRoh=" + cosRoh);
//    	System.out.println("1 x="+x + "  y="+y);
		float x1= y*sinRoh + x*cosRoh;
		float y1= -x*sinRoh + y*cosRoh;
//    	System.out.println("1 x'="+x1 + "  y'="+y1);
 
        float y_=(y1/scaled_radius)+asinh_of_tanCtrLat ;
        llp.setLatLon(MoreMath.atan(MoreMath.sinh(y_)),
                (x1 / scaled_radius) + ctrLon,
                true);
//    	System.out.println("1 x''="+llp.radlat + "  y''="+llp.radlon);
        return llp;
    }

    private Node inverse_wo_rot(int x, int y, Node llp) {
        // convert from screen to world coordinates
        x -= wx;
        float y_=(((hy-y))/scaled_radius)+asinh_of_tanCtrLat ;

        llp.setLatLon(MoreMath.atan(MoreMath.sinh(y_)),
                (x / scaled_radius) + ctrLon,
                true);
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

	public void pan(Node n, int xd,int yd) {
		System.out.println("Pan from " + n);
		forward(n,panP);
		inverse((width*xd/100)+panP.x,( height*yd/100)+panP.y, n);
		System.out.println("Pan to " + n);
	}

	public String toString() {
		return "P " + ProjMath.radToDeg(ctrLat) + "/"+ ProjMath.radToDeg(ctrLon) + " s:" + scale + " u"+upDir;
	}

	public float getCourse() {
		return course;
	}

}
