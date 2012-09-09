/*
 * Copyright (C) 2008 Steve Ratcliffe
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 * 
 * Author: Steve Ratcliffe
 * Create date: 30-Jun-2008
 */
package uk.me.parabola.mkgmap.general;

import java.util.ArrayList;
import java.util.List;

//import uk.me.parabola.imgfmt.app.Area;
//import uk.me.parabola.imgfmt.app.Coord;
import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;
import net.sharenav.osmToShareNav.model.Bounds;
import net.sharenav.osmToShareNav.model.Node;

/**
 * Routine to clip a polyline to a given bounding box.
 * @author Steve Ratcliffe
 * @see <a href="http://www.skytopia.com/project/articles/compsci/clipping.html">A very clear explaination of the Liang-Barsky algorithm</a>
 */
public class LineClipper {

	/**
	 * Clips a polyline by the given bounding box.  This may produce several
	 * separate lines if the line meanders in and out of the box.
	 * This will work even if no point is actually inside the box.
	 * @param a The bounding area.
	 * @param coords A list of the points in the line.
	 * @return Returns null if the line is completely in the bounding box and
	 * this is expected to be the normal case.
	 * If clipping is needed then an array of point lists is returned.
	 */
	public static List<ArrayList<Node>> clip(Bounds a, List<Node> coords) {

		// If all the points are inside the box then we just return null
		// to show that nothing was done and the line can be used.  This
		// is expected to be the normal case.
		if (a == null)
			return null;
		boolean inBounds = true;
		for (Node n : coords) {

			if (!a.isIn(n.lat, n.lon)) {
				inBounds = false;
			}
		}
		if (inBounds)
			return null;
		class LineCollector {
			private final ArrayList<ArrayList<Node>> ret = new ArrayList<ArrayList<Node>>(4);
			private ArrayList<Node> currentLine;
			private Node last;

			public void add(Node[] segment) {
				if (segment == null) {
					currentLine = null;
				} else {
					// we start a new line if there isn't a current one, or if the first
					// point of the segment is not equal to the last one in the line.
					if (currentLine == null || !segment[0].equals(last)) {
						currentLine = new ArrayList<Node>(5);
						currentLine.add(segment[0]);
						currentLine.add(segment[1]);
						ret.add(currentLine);
					} else {
						currentLine.add(segment[1]);
					}
					last = segment[1];
				}
			}

		}

		LineCollector seg = new LineCollector();

		// Step through each segment, clip it if necessary and create a list of
		// lines from it.
		for (int i = 0; i <= coords.size() - 2; i++) {
			Node[] pair = {coords.get(i), coords.get(i+1)};
			Node[] clippedPair = clip(a, pair);
			seg.add(clippedPair);
		}
		return seg.ret;
	}

	/**
	 * A straight forward implementation of the Liang-Barsky algorithm as described
	 * in the referenced web page.
	 * @param a The clipping area.
	 * @param ends The start and end of the line the contents of this will
	 * be changed if the line is clipped to contain the new start and end
	 * points.  A point that was inside the box will not be changed.
	 * @return An array of the new start and end points if any of the line is
	 * within the box.  If the line is wholly outside then null is returned.
	 * If a point is within the box then the same coordinate object will
	 * be returned as was passed in.
	 * @see <a href="http://www.skytopia.com/project/articles/compsci/clipping.html">Liang-Barsky algorithm</a>
	 */
	public static Node[] clip(Bounds a, Node[] ends) {
		assert ends.length == 2;

		float x0 = ends[0].lon;
		float y0 = ends[0].lat;

		float x1 = ends[1].lon;
		float y1 = ends[1].lat;

		float dx = x1 - x0;
		float dy = y1 - y0;

		double[] t = {0, 1};

		float p = -dx;
		float q = -(a.minLon - x0);
		boolean scrap = checkSide(t, p, q);
		if (scrap) return null;

		p = dx;
		q = a.maxLon - x0;
		scrap = checkSide(t, p, q);
		if (scrap) return null;

		p = -dy;
		q = -(a.minLat - y0);
		scrap = checkSide(t, p, q);
		if (scrap) return null;

		p = dy;
		q = a.maxLat - y0;
		scrap = checkSide(t, p, q);
		if (scrap) return null;

		assert t[0] >= 0;
		assert t[1] <= 1;

		Node orig0 = ends[0];
		Node orig1 = ends[1];
		if (a.isOnBoundary(ends[0])) {
			// consistency check
			//assert a.onBoundary(ends[0]) : "Point marked as boundary node at " + ends[0].toString() + " not on boundary of [" + a.minLat + ", " + a.minLon + ", " + a.maxLat + ", " + a.maxLon + "]";
		}
		else if (t[0] > 0) {
			// line requires clipping so create a new end point and if
			// its position (in map coordinates) is different from the
			// original point, use the new point as a boundary node
			Node new0 = new Node(calcCoord(y0, dy, t[0]), calcCoord(x0, dx, t[0]), FakeIdGenerator.makeFakeId());

			// check the maths worked out
			if(!new0.equals(orig0))
				ends[0] = new0;
			//ends[0].setOnBoundary(true);
		}
		else if (a.isOnBoundary(ends[0])) {
			// point lies on the boundary so it's a boundary node
			//ends[0].setOnBoundary(true);
		}
			
		if (a.isOnBoundary(ends[1])) {
			// consistency check
			//assert a.onBoundary(ends[1]) : "Point marked as boundary node at " + ends[1].toString() + " not on boundary of [" + a.minLat + ", " + a.minLon + ", " + a.maxLat + ", " + a.maxLon + "]";
		}
		else if (t[1] < 1) {
			// line requires clipping so create a new end point and if
			// its position (in map coordinates) is different from the
			// original point, use the new point as a boundary node
			Node new1 = new Node(calcCoord(y0, dy, t[1]), calcCoord(x0, dx, t[1]), FakeIdGenerator.makeFakeId());

			// check the maths worked out
			//assert a.isOnBoundary(new1) : "New boundary point at " + new1.toString() + " not on boundary of [" + a.minLat + ", " + a.minLon + ", " + a.maxLat + ", " + a.maxLon + "]";
			if(!new1.equals(orig1))
				ends[1] = new1;
			//ends[1].setOnBoundary(true);
		}
		else if(a.isOnBoundary(ends[1])) {
			// point lies on the boundary so it's a boundary node
			//ends[1].setOnBoundary(true);
		}

		// zero length segments can be created if one point lies on
		// the boundary and the other is outside of the area

		// try really hard to catch these as they will break the
		// routing 

		// the check for t[0] >= t[1] should quickly find all the zero
		// length segments but the extra check to see if the points
		// are equal could catch the situation where although t[0] and
		// t[1] differ, the coordinates come out the same for both
		// points

		if(t[0] >= t[1] || ends[0].equals(ends[1]))
			return null;

		return ends;
	}

	private static float calcCoord(float base, float delta, double t) {
		double d = 0.5;
		double y = (base + t * delta);
		return ((float) ((y >= 0f) ? y + d : y - d));
	}

	private static boolean checkSide(double[] t, double p, double q) {
		double r = q/p;

		if (p == 0) {
			if (q < 0)
				return true;
		} else if (p < 0) {
			if (r > t[1])
				return true;
			else if (r > t[0])
				t[0] = r;
		} else {
			if (r < t[0])
				return true;
			else if (r < t[1])
				t[1] = r;
		}
		return false;
	}
}
