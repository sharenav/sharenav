package net.sharenav.sharenav.mapdata;

import net.sharenav.util.IntPoint;

/** 
 * Class used to store/handle the data used in WaySegment.
 * A Waysegment is represented by 2 points (begin and end) and its width.
 * To paint the segment, this class calculates the 4 edge-points of the waysegment-rectangle,
 * it provides the middle-point for (later) painting the OnewayArrows there, etc...
 */
public class WaySegmentData {
	
	// The four edge-points of the segment
	public IntPoint a = new IntPoint();
	public IntPoint b = new IntPoint();
	public IntPoint c = new IntPoint();
	public IntPoint d = new IntPoint();
	
	/** Given the 2 Waysegment-points (begin and endpoint) and the width of a waysegment.
	 * It calculates the coodrinates of the four edgepoints a,b,c,d
	 * @param a
	 * @param b
	 * @param width waywidth. 1 for creating the way as a line
	 */
	public void set(IntPoint a1, IntPoint b1, int width) {
		if (width>0){
			getParLines(a1,b1,width);
		} else{
			this.a.set(a1);
			this.c.set(b1);
		}
	}

	public WaySegmentData(IntPoint a, IntPoint b, int width) {
		getParLines(a,b,width);
	}
	public WaySegmentData() {
	}

	/** Constructor used with 8 given integers to set the values for the 4 edgepoints of a waysegment
	 */
	public WaySegmentData(int x1, int y1, int x2, int y2, int x3, int y3, int x4, int y4) {
		this.a.set(x1, y1);
		this.b.set(x2, y2);
		this.c.set(x3, y3);
		this.d.set(x4, y4);
	}

	/** Constructor creates a WayElement using the 4 given edgepoints of the way-rectangle
	 */
	public WaySegmentData(IntPoint a,IntPoint b,IntPoint c,IntPoint d) {
		this.a.set(a);
		this.b.set(b);
		this.c.set(c);
		this.d.set(d);
	}

	// Use this if we only have 2 points to calculate
	// This is the optimized version of getParLines() fount in ways.java 	
	private void getParLines(IntPoint lineP1, IntPoint lineP2, int w) {
	
		float dx = lineP2.x - lineP1.x; // cache the substraction-result
		float dy = lineP2.y - lineP1.y; // cache the substraction-result

		float lf = w / (float)Math.sqrt(dx * dx + dy * dy);
		int xb = (int) (Math.abs(lf * dy) + 0.5f);
		int yb = (int) (Math.abs(lf * dx) + 0.5f);
		if (dy < 0) xb *= -1; 
		if (dx > 0) yb *= -1;
	
		this.a.set( lineP1.x + xb, lineP1.y + yb);
		this.b.set( lineP1.x - xb, lineP1.y - yb);
		this.c.set( lineP2.x + xb, lineP2.y + yb);
		this.d.set( lineP2.x - xb, lineP2.y - yb);
	}
	
	
	public String toString() {
		return "a: " + this.a.toString() + " b: " + this.b.toString() + " c: " + this.c.toString() + " d: " + this.d.toString();
	}
}
