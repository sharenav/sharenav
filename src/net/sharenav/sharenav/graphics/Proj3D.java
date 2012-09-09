package net.sharenav.sharenav.graphics;

import net.sharenav.gps.Node;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.sharenav.tile.Tile;
import net.sharenav.util.IntPoint;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;
import net.sharenav.util.ProjMath;

public class Proj3D implements Projection {
	private float upDir;

	private int course;
	protected float ctrLat = 0.0f; // center latitude in radians
	protected float ctrLon = 0.0f; // center longitude in radians
	private final float scale;
	private final int width;
	private final int height;
	private float mapWidth;
	private float mapHeight;
	private float scaled_radius;
	private float planetPixelRadius;
	private int pixelsPerMeter = DEFAULT_PIXEL_PER_METER;
	protected float planetPixelCircumference = MoreMath.TWO_PI
			* planetPixelRadius;
	private float scaled_lat;
	private float hy, wx;
	/** percent of height of the hot spot on the screen */
	private float hFac = 0.60f;
	/** factor to compute hep out of screen height. This have to be greater as hFac */
	private float hep_fac = 0.68f;
//	private float hep_fac = 1.2f;
	private float hyp;
	/** height of the eye point */
	private float hep;
	/** distance factor of the eye point to the hotspot */
	private float dep_fac = 0.9f;
	/** distance of the eye point to the hotspot */
	private float loY;
	private float dep;
	private float tanCtrLat;
	private float asinh_of_tanCtrLat;
	private SingleTile tileCache;
	private int ctrLonRel;
	private int ctrLatRel;
	private float scaled_radius_rel;
	private float scaled_lat_rel;
	private float sinRoh;
	private float cosRoh;
	private float minLat = Float.MAX_VALUE;
	private float maxLat = -Float.MAX_VALUE;
	private float minLon = Float.MAX_VALUE;
	private float maxLon = -Float.MAX_VALUE;
	public Node borderLU,borderLD,borderRU,borderRD;
	private IntPoint centerScreen = new IntPoint();

	private IntPoint panP = new IntPoint();

	protected static final Logger logger = Logger.getInstance(Proj3D.class,
			Logger.INFO);

	public Proj3D(Node center, int upDir, float scale, int width, int height) {
		this.upDir = ProjMath.degToRad(upDir);
		this.course = upDir;
		this.ctrLat = center.radlat;
		this.ctrLon = center.radlon;
		this.scale = scale;
		this.width = width;
		this.height = height;
		computeParameters();
		// System.out.println(center + " " + upDir + " " + scale );
	}

	public void computeParameters() {
		planetPixelRadius = MoreMath.PLANET_RADIUS * pixelsPerMeter;
		scaled_radius = planetPixelRadius / scale;
		planetPixelCircumference = MoreMath.TWO_PI * planetPixelRadius;
		// do some precomputation of stuff
		tanCtrLat = (float) Math.tan(ctrLat);
		asinh_of_tanCtrLat = MoreMath.asinh(tanCtrLat);

		// compute the offsets
		hep = height * hep_fac;
		dep = height * dep_fac;
		hyp = height * hFac;
		hy = dep / ((hep / hyp) - 1);
		mapHeight = hy - (dep / ((hep / (height - hy)) - 1));
		loY=dep+hy-mapHeight;
		mapWidth=width;
		wx = width / 2;
//		System.out.println("hep=" + hep + " dep=" + dep + " hyp=" + hyp + "hy="
//				+ hy);

		centerScreen = new IntPoint((int) wx, (int) hyp);
		cosRoh = (float) Math.cos(upDir);
		sinRoh = (float) Math.sin(upDir);

		Node n1 = new Node();
		Node n2 = new Node();
		float tmp = upDir;
		upDir = 0f;
		n1 = inverse_wo_rot(width / 2, 0, n1);
		n2 = inverse_wo_rot(width / 2, (int) mapHeight, n2);
		upDir = tmp;
		scaled_lat = mapHeight / (n1.radlat - n2.radlat);
//		Node ret = new Node();
		int horizont=16;
		borderLU=inverse(0, horizont, borderLU);
//		System.out.println("borderLU " + borderLU);
		extendMinMax(borderLU);
		borderLD=inverse(0, height-16, borderLD);
//		System.out.println("borderLD " + borderLD);
		extendMinMax(borderLD);
		borderRU=inverse(width, horizont, borderRU);
//		System.out.println("borderRU" + borderRU);
		extendMinMax(borderRU);
		borderRD=inverse(width, height-16, borderRD);
//		System.out.println("borderRD" + borderRD);
		extendMinMax(borderRD);

//		System.out.println(this.toString());
//		System.out.println("cosRoh=" + cosRoh);
//		System.out.println("sinRoh=" + sinRoh);
//		System.out.println("scaled lat=" + scaled_lat);
//		System.out.println("scaled_Radius=" + scaled_radius);
//		System.out.println("tanCtrLat=" + tanCtrLat);
//		System.out.println("asinh_of_tanCtrLat=" + asinh_of_tanCtrLat);
//		System.out.println("Size = " + mapWidth + "/" + mapHeight);
//		System.out.println(MoreMath.FAC_RADTODEC * minLat + "/" + MoreMath.FAC_RADTODEC * minLon + " " + MoreMath.FAC_RADTODEC * maxLat + "/" + MoreMath.FAC_RADTODEC * maxLon);

	}

	private void extendMinMax(Node n) {
		if (n.radlat > maxLat) {
			maxLat = n.radlat;
		}
		if (n.radlat < minLat) {
			minLat = n.radlat;
		}
		if (n.radlon > maxLon) {
			maxLon = n.radlon;
		}
		if (n.radlon < minLon) {
			minLon = n.radlon;
		}
	}

	public float getUpDir() {
		return (float) upDir;
	}

	public void setUpDir(float upDir) {
		this.upDir = upDir;
		computeParameters();
	}

	// private void rotate(float x,float y,float[] ret){
	// ret[0]=x*cosRoh - y*sinRoh;
	// ret[1]=x*sinRoh - y*cosRoh;
	// }

	public IntPoint forward(Node n) {
		return forward(n.radlat, n.radlon, new IntPoint(0, 0));
	}

	public IntPoint forward(Node pt, IntPoint p) {
		return forward(pt.radlat, pt.radlon, p);
	}

	public IntPoint forward(float lat, float lon, IntPoint p) {
		// same as forward_x and forward_y, and convert to screen
		// coords
		// System.out.println("public IntPoint forward(" + lat + "," + lon + ","
		//		+ p + ")");
		float px = (scaled_radius * ProjMath.wrap_longitude(lon - ctrLon));
		float py = (scaled_radius * (MoreMath.asinh((float) Math.tan(lat)) - asinh_of_tanCtrLat));
		float x = px * cosRoh - py * sinRoh;
		float y = px * sinRoh + py * cosRoh;
		return projectionTo3D(x, y, p);
	}

	public IntPoint forward_app(float lat, float lon, IntPoint p) {
		//System.out.println("public IntPoint forward_app(" + lat + "," + lon
		//		+ "," + p + ")");
		float px = scaled_radius * ProjMath.wrap_longitude(lon - ctrLon);
		float py = scaled_lat * (lat - ctrLat);
		float x = px * cosRoh - py * sinRoh;
		float y = px * sinRoh + py * cosRoh;
		return projectionTo3D(x, y, p);
	}

	// TODO check if this doesn't cause any concurrent modification if
	// two thread are using this
	public IntPoint forward(short lat, short lon, IntPoint p, SingleTile t) {
//		System.out.println("public IntPoint forward(" + lat + "," + lon + ","
//				+ p + "," + t + ")");
		if (t != tileCache) {
			ctrLonRel = (int) ((ctrLon - t.centerLon) * MoreMath.FIXPT_MULT);
			ctrLatRel = (int) ((ctrLat - t.centerLat) * MoreMath.FIXPT_MULT);
			scaled_radius_rel = (scaled_radius * MoreMath.FIXPT_MULT_INV);
			scaled_lat_rel = (scaled_lat * MoreMath.FIXPT_MULT_INV);
			tileCache = t;
		}
		float px = scaled_radius_rel * (lon - ctrLonRel);
		float py = scaled_lat_rel * (lat - ctrLatRel);
		float x = px * cosRoh - py * sinRoh;
		float y = px * sinRoh + py * cosRoh;
//		System.out.println("ret " + upDir + ":" +px +"/"+py + " | "+x +"/"+y+ " ("+cosRoh+"/"+sinRoh+")");
		return projectionTo3D(x, y, p);
	}

//	private Node invProjectionTo3D(int x, int y, Node ret) {
//		if (ret == null) {
//			ret = new Node();
//		}
//		ret.radlon = x - wx;
//		ret.radlat = hyp - y;
//		return ret;
//	}
	
	// original function by Harald, the ints overflow
	//public int getScaleFor(int x, int y){
	//	float y_ = hyp- y;
	//	float y1 = (dep / ((hep / y_) - 1)); 
	//	float fac=dep * (dep + y1);
	//	return (int) (scale*fac);
	//}


	// FIXME check the math
	// a try by jkpj, seems to be a useful version
	public int getScaleFor(int x, int y){
		float y_ = hyp - y;

		float radlat = (dep / ((hep / y_) - 1));
		float y1 = radlat;
		float fac = 1 / dep * (dep + radlat);
		return (int) (scale*fac);
	} 

	private Node invProjectionTo3D(int x, int y, Node ret) {
		
		if (ret == null) {
			ret = new Node();
		}
		float y_ = hyp- y;
//		if (y_ <0.5f ) {
//			ret.radlat = 0.5f;
//		} else {
			ret.radlat = (dep / ((hep / y_) - 1));
//		}
		ret.radlon = (x - wx) / dep * (dep + ret.radlat);
		return ret;
	}

	/**
	 * convert from the flat map with origin on the hotspot to screen
	 * coordinates
	 * 
	 * @param y
	 * @param x
	 * @param p
	 * @return
	 */
	private IntPoint projectionTo3D(float x, float y, IntPoint p) {
		float z = dep + y;
		if (z < loY){
			// ugly way to avoid overflow;
//			System.out.println(" fit z from " + z + " to 2.0");
			z=loY;
		}
		p.setY((int) ((hyp - (y * hep / z)+0.5f)));
		p.setX((int) ((dep * x / z)+0.5f) + wx);
		return p;
	}

	public int getPPM() {
		return pixelsPerMeter;
	}

	public String getProjectionID() {
		return "Proj3D";
	}

	public float getScale() {
		return scale;
	}

	// TODO: needs adaption to rotated map???
	public float getScale(Node ll1, Node ll2, IntPoint IntPoint1,
			IntPoint IntPoint2) {
		try {
			float deltaDegrees;
			float pixPerDegree;
			int deltaPix;
			int dx = Math.abs(IntPoint2.getX() - IntPoint1.getX());
			int dy = Math.abs(IntPoint2.getY() - IntPoint1.getY());
			float dlat = Math.abs(ll1.getLatDeg() - ll2.getLatDeg());
			float dlon = Math.abs(ll1.getLonDeg() - ll2.getLonDeg());

			if (dlon / dx < dlat / dy) {
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
			// System.out.print("ProjMath.getScale(): caught null IntPointer exception.");
			return Float.MAX_VALUE;
		}
	}

	public Node inverse_app(int x, int y, Node llp) {
		if (llp == null) {
			llp = new Node();
		}
		llp = invProjectionTo3D(x, y, llp);
		float x1 = llp.radlat * sinRoh + llp.radlon * cosRoh;
		float y1 = -llp.radlon * sinRoh + llp.radlat * cosRoh;
		llp.setLatLonRad((y1 / scaled_lat + ctrLat), (x1 / scaled_radius)
				+ ctrLon);
		return llp;
	}
	

	public boolean isPlotable(short lat, short lon, SingleTile t) {
		return isPlotable(lat*MoreMath.FIXPT_MULT_INV+t.centerLat,
				lon*MoreMath.FIXPT_MULT_INV+t.centerLon);
	}

	int cMinLat,cMaxLat,cMinlon,cMaxLon,cLeft,cUpper,cRight,cDown,c;
	public void printClipstat(){
//		System.out.println("clipStat "+cMinLat+","+cMaxLat+","+cMinlon+","+cMaxLon+"  "+cLeft+","+cUpper+","+cRight+","+cDown+"  "+c);
	}
	public boolean isPlotable(float lat, float lon) {
		if (lat < minLat){
			cMinLat++;
			return false;
		}
		if (lat > maxLat){
			cMaxLat++;
			return false;
		}
		if (lon < minLon){
			cMinlon++;
			return false;
		}
		if (lon > maxLon){
			cMaxLon++;
			return false;
		}
		if (! isRightOf(borderLD,borderLU,lat,lon)){
			cLeft++;
			return false;
		}
		if (! isRightOf(borderLU,borderRU,lat,lon)){
			cUpper++;
			return false;
		}
		if (! isRightOf(borderRU,borderRD,lat,lon)){
			cRight++;
			return false;
		}
		if (! isRightOf(borderRD,borderLD,lat,lon)){
			cDown++;
			return false;
		}
		c++;
		return true;
	}
	
	private boolean isRightOf(Node p0,Node p1,float lat, float lon){
		// x=lon y=lat
		//(y - y0) (x1 - x0) - (x - x0) (y1 - y0)
		if (((lat - p0.radlat) * (p1.radlon - p0.radlon) - (lon - p0.radlon) * (p1.radlat - p0.radlat)) <= 0f){
			return true;
		}
		return false;
	}

	public Node inverse(int x, int y, Node llp) {
		// convert from screen to world coordinates
		// System.out.println("Inverse Full("+x+","+y+")");
		llp = invProjectionTo3D(x, y, llp);
		// System.out.println("sinRoh=" + sinRoh);
		// System.out.println("cosRoh=" + cosRoh);
		// System.out.println("1 x="+llp.radlon + "  y="+llp.radlat);
		float x1 = llp.radlat * sinRoh + llp.radlon * cosRoh;
		float y1 = -llp.radlon * sinRoh + llp.radlat * cosRoh;
		// System.out.println("1 x'="+x1 + "  y'="+y1);

		float y_ = (y1 / scaled_radius) + asinh_of_tanCtrLat;
		llp.setLatLonRad(MoreMath.atan(MoreMath.sinh(y_)), (x1 / scaled_radius)
				+ ctrLon);
		// System.out.println("1 x''="+llp.radlat + "  y''="+llp.radlon);
		return llp;
	}

	private Node inverse_wo_rot(int x, int y, Node llp) {
		// convert from screen to world coordinates
		x -= wx;
		float y_ = (((hy - y)) / scaled_radius) + asinh_of_tanCtrLat;

		llp.setLatLonRad(MoreMath.atan(MoreMath.sinh(y_)), (x / scaled_radius)
				+ ctrLon);
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
		forward(n, panP);
		inverse((width * xd / 100) + panP.x, (height * yd / 100) + panP.y, n);
	}

	public String toString() {
		return "Proj3d: " + (ctrLat * MoreMath.FAC_RADTODEC) + "/"
				+ (ctrLon * MoreMath.FAC_RADTODEC) + " s:" + scale + " u"
				+ (upDir * MoreMath.FAC_RADTODEC);
	}

	public float getCourse() {
		return course;
	}

	public boolean isOrthogonal() {
		return false;
	}

	/**
	 * Calculate the end points for 2 paralell lines with distance w which has
	 * the origin line (Xpoint[i]/Ypoint[i]) to (Xpoint[i+1]/Ypoint[i+1]) as
	 * centre line
	 * 
	 * @param xPoints
	 *            list off all XPoints for this Way
	 * @param yPoints
	 *            list off all YPoints for this Way
	 * @param i
	 *            the index out of (X/Y) Point for the line segement we looking
	 *            for
	 * @param w
	 *            the width of the segment out of the way
	 * @param p1
	 *            left start point
	 * @param p2
	 *            right start point
	 * @param p3
	 *            left end point
	 * @param p4
	 *            right end point
	 * @return the angle of the line in radians ( -Pi .. +Pi )
	 */
	public float getParLines(int xPoints[], int yPoints[], int i, int w,
			IntPoint p1, IntPoint p2, IntPoint p3, IntPoint p4) {
		int i1 = i + 1;
		Node n0=invProjectionTo3D(xPoints[i],yPoints[i],null);
		Node n1=invProjectionTo3D(xPoints[i1],yPoints[i1],null);
		float dx = n1.radlon - n0.radlon;
		float dy = n1.radlat - n0.radlat;
		float l2 = dx * dx + dy * dy;
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
		float xd = rfx * xb;
		float yd = rfy * yb;
		projectionTo3D(n0.radlon+xd, n0.radlat+yd, p2);
		projectionTo3D(n0.radlon-xd, n0.radlat-yd, p1);
		projectionTo3D(n1.radlon+xd, n1.radlat+yd, p4);
		projectionTo3D(n1.radlon-xd, n1.radlat-yd, p3);
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
	public IntPoint getScreenCenter() {
		return centerScreen;
	}

}
