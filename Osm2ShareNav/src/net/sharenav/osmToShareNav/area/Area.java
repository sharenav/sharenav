/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2010 Harald Mueller
 */

package net.sharenav.osmToShareNav.area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.sharenav.osmToShareNav.Configuration;
import net.sharenav.osmToShareNav.MyMath;
import net.sharenav.osmToShareNav.OsmParser;
import net.sharenav.osmToShareNav.model.Bounds;
import net.sharenav.osmToShareNav.model.Node;
import uk.me.parabola.mkgmap.reader.osm.FakeIdGenerator;

public class Area {
	private ArrayList<Outline>	outlineList	= new ArrayList<Outline>();
	private ArrayList<Outline>	holeList	= new ArrayList<Outline>();
	Outline						outline		= new Outline();
	public Triangle			triangle;
	ArrayList<Triangle> triangleList = null;
	static DebugViewer			viewer		= null;
	public Vertex	edgeInside;
	public  boolean debug = false;

	public double maxdist = 0d;
	//double limitdist = 25000d;
	// mapmid format errors
	//double limitdist = 50000d;
	double limitdist = 32000d;
	//double limitdist = 1250000d;
	//double limitdist = 10000d;

	private static OsmParser parser;

	public static void setParser(OsmParser setParser) {
		parser = setParser;
	}
	

	public void addOutline(Outline p) {
		if (p.isValid()) {
			outlineList.add(p);
//			p.calcNextPrev();
		}
	}

	public void addHole(Outline p) {
		if (p.isValid()) {
			holeList.add(p);
		}
	}

	public void clean() {
		outlineList = new ArrayList<Outline>();
		holeList = new ArrayList<Outline>();
		// tri = new ArrayList<Triangle>();
		outline = new Outline();
	}

	public List<Triangle> triangulate() {
		if (debug) {
			if (viewer == null) {
				viewer = new DebugViewer(this);
			} else {
				viewer.setArea(this);
			}
		}
		// if there are more ways than one are used to build the outline, try to construct one outline for that
		ArrayList<Outline>	outlineTempList = new ArrayList<Outline>();
		while (outlineList.size() > 0) {
			outline = outlineList.get(0);
			if (!outline.isClosed()) {
				outline.connectPartWays(outlineList);
			}
			if (outline.isClosed()) {
				outlineTempList.add(outline);
			}
			outlineList.remove(0);
		}
		outlineList = outlineTempList;
		// the same for the holes
		outlineTempList = new ArrayList<Outline>();
		//System.err.println("Starting to connect part ways");
		while (holeList.size() > 0) {
			outline = holeList.get(0);
			if (!outline.isClosed()) {
				outline.connectPartWays(holeList);
			}
			if (outline.isClosed()) {
				outlineTempList.add(outline);
			}
			holeList.remove(0);
		}
		//System.err.println("Finished connecting part ways");
		holeList = outlineTempList;
		
		int dir = 0;
		ArrayList<Triangle> ret = new ArrayList<Triangle>(1);
		triangleList = ret;
		repaint();
		int loop = 0;
		while (outlineList.size() > 0) {
			outline = outlineList.get(0);
			
			if (! outline.isValid()) {
				outlineList.remove(0);
				continue;
			}

			outline.calcNextPrev();
			outlineList.remove(0);
			//System.err.println("Starting to do the cutOneEar thing");
			while (outline.vertexCount() > 2) {
				loop++;
				if (loop % 5000 == 0) {
					if (Configuration.getConfiguration().verbose >= 0) {
						System.err.println("Triangulating outline "
								   + outline.getWayId() + " looped "
								   + loop + " times");
					}
				}
				if (loop > 4000000) {
					System.err.println("Break because of infinite loop for outline " + outline.getWayId());
					System.err.println("  see http://www.openstreetmap.org/browse/way/" + outline.getWayId());
					break;
				}
				Triangle t = cutOneEar(outline, holeList, dir);
				splitTriangleIfNeeded(t, ret, 0);
				dir = (dir + 1) % 4;
			}
			//System.err.println("Finished doing the cutOneEar thing");
		}
		//System.out.println(ret);
		//System.out.println("loops :" + loop);
		//System.err.println("Starting to optimize");
		optimize();
		ret.trimToSize();
		//System.err.println("Finished optimizing");
		return ret;

	}
	private void splitTriangleIfNeeded(Triangle t, ArrayList<Triangle> ret, int recurselevel) {
		// check the size; if a line is too long, split the tringle
		Node n0 = t.getVert()[0].getNode();
		Node n1 = t.getVert()[1].getNode();
		Node n2 = t.getVert()[2].getNode();
		double dist0 = MyMath.dist(n0, n1);
		double dist1 = MyMath.dist(n1, n2);
		double dist2 = MyMath.dist(n2, n0);
		if (dist0 > limitdist ||
			    dist1 > limitdist ||
			    dist2 > limitdist) {

			if (recurselevel > 80) {
				System.out.println("WARNING: Recurselevel > 80, giving up splitting triangle " + t);
				ret.add(t);
				return;
			}

			Triangle t1 = new Triangle(t.getVert()[0], t.getVert()[1], t.getVert()[2]);
			Triangle t2 = new Triangle(t.getVert()[0], t.getVert()[1], t.getVert()[2]);
			int longest = 0;
			double longestDist = 0d;
			Node newNode = null;
			if (dist0 > longestDist) {
				longestDist = dist0;
				longest = 0;
			}
			if (dist1 > longestDist) {
				longestDist = dist1;
				longest = 1;
			}
			if (dist2 > longestDist) {
				longestDist = dist2;
				longest = 2;
			}
			//System.out.println("Splitting triangle " + t + ", dist= " + longestDist);
			//System.out.println("Longest edge: " + longest);

			switch(longest) {
			case 0: 
				newNode = n0.midNode(n1, FakeIdGenerator.makeFakeId());
				t1.getVert()[1] = new Vertex(newNode,null);
				t2.getVert()[0] = new Vertex(newNode,null);
				break;
			case 1: 
				newNode = n1.midNode(n2, FakeIdGenerator.makeFakeId());
				t1.getVert()[2] = new Vertex(newNode,null);
				t2.getVert()[1] = new Vertex(newNode,null);
				break;
			case 2: 
				newNode = n2.midNode(n0, FakeIdGenerator.makeFakeId());
				t1.getVert()[0] = new Vertex(newNode,null);
				t2.getVert()[2] = new Vertex(newNode,null);
				break;
			}
			splitTriangleIfNeeded(t1, ret, recurselevel + 1);
			//System.out.println("Split or add triangle t2: " + t2);
			splitTriangleIfNeeded(t2, ret, recurselevel + 1);
		} else {
			//System.out.println("Adding side " + side + " of triangle");
			ret.add(t);
		}
	}
	private void optimize() {
		for (Triangle t:triangleList) {
			t.opt = false;
		}
//		while (true) {
			Iterator<Triangle> it = triangleList.iterator();
			while (it.hasNext()) {
				Triangle t1 = it.next();
				if (t1.getVert()[0].getNode() == t1.getVert()[1].getNode() 
						|| t1.getVert()[0].getNode() == t1.getVert()[2].getNode()
						|| t1.getVert()[1].getNode() == t1.getVert()[2].getNode()) {
					it.remove();
//					System.out.println("remove degenerated Triangle");
				}
//				if (! t1.opt) {
//					for (Triangle t2:triangleList) {
//						if (t1.equalVert(t2) == 2) {
//							optimize(t1,t2);
//						}
//					}
//				}
			}
//		}
	}

	/**
	 * 
	 */
	private void repaint() {
		if (debug) {
			if (viewer == null) {
				viewer = DebugViewer.getInstanz(this);
			} else {
				viewer.setArea(this);
			}

			if (viewer != null) {
				viewer.repaint();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
				// System.out.println("Area.repaint()");
			}
		}
	}

	private Triangle cutOneEar(Outline outline, ArrayList<Outline> holeList, int dir) {
		//List<Vertex> orderedOutline = outline.getOrdered(dir);
		Vertex orderedOutlineMin = outline.getMin(dir);
		while (true) {
			Vertex n = orderedOutlineMin;
			triangle = new Triangle(n, n.getNext(), n.getPrev());
			edgeInside = findEdgeInside(outline, triangle,dir);
			repaint();
			if (edgeInside == null) {
				// this is an ear with nothing in it so cut the ear
				outline.remove(n);
				return triangle;
			} else {
				boolean handled = false;
				// at least one edge is inside this ear
				if (edgeInside.partOf(outline)) {
					handled = true;
					// node of the outline is in the ear so we have to cut the outline into two parts
					// one will handled now and the other goes to the stack
					outline.clean();
					Vertex nt = n;
					// create a fresh copy of the old outline starting from outer edge of the expected ear
					while (nt != edgeInside) {
						outline.append(nt);
						nt = nt.getNext();
					}
					// go ahead the edge that was found inside the test triangle
					outline.append(edgeInside);
					Outline newOutline = new Outline();
					newOutline.setWayId(outline.getWayId());
					while (nt != n) {
						newOutline.append(nt);
						nt = nt.getNext();
					}
					newOutline.append(n);
					if (newOutline.isValid()) {
						addOutline(newOutline);
					}
					// reinititalisize outline;
					outline.calcNextPrev();
					//orderedOutline = outline.getOrdered(dir);
					orderedOutlineMin = outline.getMin(dir);
				} else {
					for (Outline p : holeList) {
						if (edgeInside.partOf(p)) {
							// now we have an edge of a hole inside the rectangle
							// lets join the hole with the outline and have a next try
//							Outline hole = edgeInside.getOutline();
							Outline hole = p;
							if (hole != edgeInside.getOutline()) {
								System.out.println("Warning: something wrong with internal data!");
							}
							hole.calcNextPrev();
							repaint();
//							Outline newOutline = new Outline();
							Vertex nt = n;
							boolean clockWise = outline.isClockWiseFast();
							outline.clean();
							do {
								outline.append(nt);
								if (clockWise) {
									nt = nt.getNext();
								} else {
									nt = nt.getPrev();
								}
							} while (nt != n);
							repaint();
							outline.append(n.clone());
							repaint();
							nt = edgeInside;
							// the following makes triangulation
							// of Finnish sea fail after 75 000 triangles with:

							/* Triangulating outline 4611686018427401182 looped 75000 times
							   Something went wrong when trying to triangulate relation 
							    http://www.openstreetmap.org/browse/relation/4611686018427388922 I'll attempt to ignore this relation
							java.util.NoSuchElementException
							    at java.util.ArrayList$Itr.next(ArrayList.java:757)
							    at java.util.Collections.min(Collections.java:624)
							    at net.sharenav.osmToShareNav.area.Outline.getLonMin(Outline.java:174)
							    at net.sharenav.osmToShareNav.area.Outline.isClockWiseFast(Outline.java:290)
							    at net.sharenav.osmToShareNav.area.Area.cutOneEar(Area.java:326)
							    at net.sharenav.osmToShareNav.area.Area.triangulate(Area.java:131)
							    at net.sharenav.osmToShareNav.Relations.processRelations(Relations.java:316)
							    at net.sharenav.osmToShareNav.Relations.<init>(Relations.java:48)
							    at net.sharenav.osmToShareNav.BundleShareNav.run(BundleShareNav.java:516)
							    at java.lang.Thread.run(Thread.java:679) */

							//clockWise = hole.isClockWiseFast();
							clockWise = hole.isClockWise();
							do {
								outline.append(nt);
								if (clockWise) {
									nt = nt.getPrev();
								} else {
									nt = nt.getNext();
								}
//								repaint();
							} while (nt != edgeInside);
							outline.append(edgeInside.clone());
							holeList.remove(hole);
							outline.calcNextPrev();
							//orderedOutline = outline.getOrdered(dir);
							orderedOutlineMin = outline.getMin(dir);
							handled = true;
							break; // we found the hole so break this for loop
						}
					}
					if (!handled) {
						System.err.println("Something strange happened, there is an edge inside, but the member outline "
								+ edgeInside.getOutline().getWayId() + " wasn't found");
						System.err.println("  see http://www.openstreetmap.org/?node=" + edgeInside.getId());
	//					debug = true;
	//					repaint();
						return triangle;
					}
				}
			}
		}
	}

//	private Vertex findEdgeInside(Outline outline, Triangle triangle) {
//		Vertex leftmost = null;
//		Vertex n = outline.findVertexInside(triangle);
//		if (leftmost == null) {
//			leftmost = n;
//		} else {
//			if (n.getX() < leftmost.getX()) {
//				leftmost = n;
//			}
//		}
//		for (Outline p : holeList) {
//			n = p.findVertexInside(triangle);
//			if (leftmost == null) {
//				leftmost = n;
//			} else {
//				if (n != null && n.getX() < leftmost.getX()) {
//					leftmost = n;
//				}
//			}
//		}
//		return leftmost;
//	}

	private Vertex findEdgeInside(Outline outline, Triangle triangle, int dir) {
		ArrayList<Vertex> ret = outline.findVertexInside(triangle, null);
		for (Outline p : holeList) {
			ret = p.findVertexInside(triangle, ret);
		}
		if (ret == null) {
			return null;
		}
		//System.err.println("Starting to sort in findEdgeInside()");
//		switch (dir) {
//		case 0:
//			    Collections.sort(ret, new DirectionComperator0());
//			    break;
//		case 1:
//			    Collections.sort(ret, new DirectionComperator1());
//			    break;
//		case 2:
//			    Collections.sort(ret, new DirectionComperator2());
//			    break;
//		default:
//			    Collections.sort(ret, new DirectionComperatorX());
//			    break;
//		}
		//System.err.println("Starting to sort in findEdgeInside()");
		switch (dir) {
		case 0:
			    return Collections.min(ret, new DirectionComperator0());
		case 1:
			    return Collections.min(ret, new DirectionComperator1());
		case 2:
			    return Collections.min(ret, new DirectionComperator2());
		default:
			    return Collections.min(ret, new DirectionComperatorX());
		}
	}
	
	public Bounds extendBounds(Bounds b) {
		if (b == null) {
			b = new Bounds();
		}
		for (Outline o:outlineList) {
			o.extendBounds(b);
		}
		for (Outline o:holeList) {
			o.extendBounds(b);
		}
		if (triangleList != null) {
			for (Triangle t: triangleList) {
				t.extendBound(b);
			}
		}

		return b;
	}
	
	public ArrayList<Outline> getOutlineList() {
		return outlineList;
	}

	public ArrayList<Outline> getHoleList() {
		return holeList;
	}

}
