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

import net.sharenav.osmToShareNav.model.Bounds;
import net.sharenav.osmToShareNav.model.Node;


public class Outline {
	private ArrayList<Vertex> vertexList = new ArrayList<Vertex>();
	private long wayId = -1;
//	private ArrayList<Vertex> ordered;
	
	public long getWayId() {
		return wayId;
	}

	public void setWayId(long wayId) {
		this.wayId = wayId;
	}

	public ArrayList<Vertex> getVertexList() {
		return vertexList;
	}
	public boolean isValid() {
		if (vertexList.size() < 2) {
			return false;
		}
		return true;
	}

	public void clean() {
		vertexList = new ArrayList<Vertex>();
	}
	
	public void prepend(Vertex v) {
		vertexList.add(0, v);
		v.setOutline(this);
	}
	
	public void append(Vertex v) {
		vertexList.add(v);
		v.setOutline(this);
	}
	
	public boolean isClosed() {
		if (vertexList.size() < 1) {
			return false;
		}
		Vertex prev = null;
		Vertex first = null;
		first = vertexList.get(0);
		prev = vertexList.get(vertexList.size() - 1);
		if (first.equals(prev)) {
			return true;
		}
		return false;
	}
	
	public void connectPartWays(ArrayList<Outline> others) {
		boolean changed = false;
//		System.out.println("Entering connectPartWays");
		do {
			changed = false;
			Iterator<Outline> i = others.iterator();
			while (i.hasNext()) {
				Vertex last = null;
				Vertex first = null;
				last = vertexList.get(vertexList.size() - 1);
				first = vertexList.get(0);
				Outline o = i.next();
//				System.out.println("Iterating, node: " + o.vertexList.get(0).getNode());
				if (o == this) {
//					System.out.println("o == this");
					continue;
				}
				if (!o.isClosed()) {
//					System.out.println("not o.isClosed()");

//					System.out.println("last.getNode(): " + last.getNode());
//					System.out.println("first.getNode(): " + first.getNode());
//					System.out.println("vertexlist size: " + vertexList.size());
					if (o.vertexList.get(0).getNode().equals(last.getNode())) {
//						System.out.println("found way connecting to end of outline, so append it");
						changed = true;
						for (Vertex v : o.vertexList) {
							append(v);
						}
						i.remove();
					} else if (o.vertexList.get(o.vertexList.size()-1).getNode().equals(last.getNode())) {
//						System.out.println("found way reverse connecting to end of outline, so append it");
						changed = true;
						for (int loop = o.vertexList.size()-1; loop >= 0; loop--) {
						    Vertex v = o.vertexList.get(loop);
							append(v);
						}
						i.remove();
					} else if (o.vertexList.get(0).getNode().equals(first.getNode())) {
//						System.out.println("found way connecting to start of outline, so prepend it");
						changed = true;
						for (Vertex v : o.vertexList) {
							prepend(v);
						}
						i.remove();
					} else if (o.vertexList.get(o.vertexList.size()-1).getNode().equals(first.getNode())) {
//						System.out.println("found way reverse connecting to start of outline, so prepend it");
						changed = true;
						for (int loop = o.vertexList.size()-1; loop >= 0; loop--) {
							Vertex v = o.vertexList.get(loop);
							prepend(v);
						}
						i.remove();
					}
				}
			}
		} while (changed);

	}
	
	public void calcNextPrev() {
		if (vertexList == null || vertexList.size() == 0) {
			return;
		}
		Vertex prev = null;
		Vertex first = null;
		first = vertexList.get(0);
		prev = vertexList.get(vertexList.size() - 1);
		if (first.equals(prev)) {
			vertexList.remove(vertexList.size() - 1);
		}
		if (vertexList.size() < 3) {
			// this is a degenerated polygon make it empty
			vertexList.clear();
		}
		for (Vertex v:vertexList) {
			v.setOutline(this);
			if (first == null) {
				first = v;
				prev = v;
			} else {
				v.setPrev(prev);
				prev.setNext(v);
				prev = v;
			}
			
		}
		first.setPrev(prev);
		prev.setNext(first);
	}
	
    //	@SuppressWarnings("unchecked")
    //	public List<Vertex> getLonOrdered() {
    //		ArrayList<Vertex> ordered = (ArrayList<Vertex>) vertexList.clone();
    //		Collections.sort(ordered, new LonComperator());
    //		return ordered;
    //	}
	@SuppressWarnings("unchecked")
	public Vertex getLonMin() {
		ArrayList<Vertex> ordered = (ArrayList<Vertex>) vertexList.clone();
		return Collections.min(ordered, new LonComperator());
	}
    //	@SuppressWarnings("unchecked")
    //	public List<Vertex> getOrdered(int dir) {
    ////		return getLonOrdered();
    //		ArrayList<Vertex> ordered = (ArrayList<Vertex>) vertexList.clone();
    //		switch (dir) {
    //		case 0:
    //			    Collections.sort(ordered, new DirectionComperator0());
    //			    break;
    //		case 1:
    //			    Collections.sort(ordered, new DirectionComperator1());
    //			    break;
    //		case 2:
    //			    Collections.sort(ordered, new DirectionComperator2());
    //			    break;
    //		default:
    //			    Collections.sort(ordered, new DirectionComperatorX());
    //			    break;
    //		}
    //		return ordered;
    //	}
    //	
	public Vertex getMin(int dir) {
		ArrayList<Vertex> ordered = (ArrayList<Vertex>) vertexList.clone();
		switch (dir) {
		case 0:
			    return Collections.min(ordered, new DirectionComperator0());
		case 1:
			    return Collections.min(ordered, new DirectionComperator1());
		case 2:
			    return Collections.min(ordered, new DirectionComperator2());
		default:
			    return Collections.min(ordered, new DirectionComperatorX());
		}
	}
	
//	public Vertex findVertexInside(Triangle triangle) {
//		float leftmost = Float.MAX_VALUE;
//		Vertex leftmostNode = null;
//		for (Vertex v:vertexList) {
//			if (triangle.isVertexInside(v)) {
//				float lon = v.getX();
//				if (lon < leftmost) {
//					leftmost = lon;
//					leftmostNode = v;
//				}
//			}
//		}
//		return leftmostNode;
//	}

	public ArrayList<Vertex> findVertexInside(Triangle triangle, ArrayList<Vertex> ret) {
		for (int i=0; i<vertexList.size(); i++) {
			Vertex vertex = vertexList.get(i);
			if (triangle.isVertexInside(vertex)){
				if (ret == null) {
					ret = new ArrayList<Vertex>();
				}
				ret.add(vertex);
			}
		}
		return ret;
	}

	public void remove(Vertex v) {
		v.getPrev().setNext(v.getNext());
		v.getNext().setPrev(v.getPrev());
		vertexList.remove(v);
		
	}
	
	public int vertexCount() {
		return vertexList.size();
	}
	
	@Override
	public String toString() {
		StringBuffer b = new StringBuffer();
		for (Vertex n:vertexList) {
			b.append(n);
		}
		return b.toString();
	}
	
    //	public boolean isClockWise() {
    //		boolean cw = isClockWise3();
//		if (cw != isClockWise2()){
//			System.out.println("2 and 3 not the same");
//		}
//		return cw;
//	}
	
	/**
	 * Check if this outline (polygone) is clockwise. Therfore we get the leftmost vertex and the
	 * booth neighbors. This edge must be convex. 
	 * @return
	 */
	public boolean isClockWise() {
		calcNextPrev();
		Vertex v = getLonMin();
		Vertex vp = v.getPrev();
		Vertex vn = v.getNext();
		if (((v.getX()-vp.getX())*(vn.getY()-v.getY())-(v.getY()-vp.getY())*(vn.getX()-v.getX())) <0  ) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * Check if this outline (polygone) is clockwise. Therfore we get the leftmost vertex and the
	 * booth neighbors. This edge must be convex. 
	 * @return
	 */
	public boolean isClockWiseFast() {
		//calcNextPrev();
		Vertex v = getLonMin();
		Vertex vp = v.getPrev();
		Vertex vn = v.getNext();
		if (((v.getX()-vp.getX())*(vn.getY()-v.getY())-(v.getY()-vp.getY())*(vn.getX()-v.getX())) <0  ) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * this one is only valid for convex polygon
	 * @return
	 */
	public boolean isClockWise2() {
		float z = 0.0f;
		for (Vertex i : vertexList) {
			z += i.cross(i.getNext());
		}
		if (z < 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean isClockWise1() {
		Vertex j, k;
		int count = 0;
		double z;

		if (vertexCount() < 3) {
		   throw new IllegalArgumentException("polygone with < then 3 nodes is degenerated");
		}
		for (Vertex i:vertexList){
		   j=i.getNext();
		   k=j.getNext();
			z = (j.getX() - i.getX()) * (k.getY() - j.getY()) - (j.getY() - i.getY()) * (k.getX() - j.getX());
			if (z < 0) {
				count--;
			} else if (z > 0) {
				count++;
			}
		}
		if (count > 0) {
			return true;
		} else if (count < 0) {
			return false;
		} else {
			System.err.println("Triangulation Error! this should never happen");
//			 throw new IllegalArgumentException("this should never happen");
			return true;
		}
	}
	
	public Bounds extendBounds(Bounds b) {
		for (Vertex i:vertexList){
			i.extendBounds(b);
		}
		return b;
	}
	/**
	 * @param i
	 * @return
	 */
	public Node getNode(int i) {
		return (vertexList.get(i).getNode());
	}
}
