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

import net.sharenav.osmToShareNav.model.Bounds;
import net.sharenav.osmToShareNav.model.Node;

public class Triangle {
	private final Vertex[] vert = new Vertex[3];
	public boolean opt = false;
	
	public Triangle(Vertex n1,Vertex n2, Vertex n3) {
		getVert()[0] = n1;
		getVert()[1] = n2;
		getVert()[2] = n3;
	}
		
	public boolean isVertexInside(Vertex n){
		if (n == getVert()[0] && n == getVert()[1] && n == getVert()[2]) {
			return false;
		}
		
		float n1 = n.getSideOfVector(getVert()[1], getVert()[0]);
		float n2 = n.getSideOfVector(getVert()[2], getVert()[1]);
		float n3 = n.getSideOfVector(getVert()[0], getVert()[2]);
		
		 if (n1 * n2 > 0.0 && n1 * n3 > 0.0 && n2 * n3 > 0.0) {
			 return true;
		 }
		 return false;
	}
	
	@Override
	public String toString() {
		return new String("tri " + getVert()[0] + getVert()[1] + getVert()[2]);
	}
	
	// probably is faster than the getMidpoint(), not used but retain for now
	public Vertex getAverageMidpoint() {
		float lat = 0f;
		float lon = 0f;
                for (int i = 0; i < 3; i++) {
			lat += getVert()[i].getLat();
			lon += getVert()[i].getLon();
                }
		return new Vertex(lat / 3, lon / 3, 0l);
        }
        
	public Vertex getMidpoint() {
		float lat = 0f;
		float lon = 0f;
		float minLat = Float.MAX_VALUE;
		float minLon = Float.MAX_VALUE;
		float maxLat = -Float.MAX_VALUE;
		float maxLon = -Float.MAX_VALUE;
		for (int i = 0; i < 3; i++) {
			if (getVert()[i].getLat() <= minLat) {
				minLat = getVert()[i].getLat();
			}
			if (getVert()[i].getLat() >= maxLat) {
				maxLat = getVert()[i].getLat();
			}
			if (getVert()[i].getLon() <= minLon) {
				minLon = getVert()[i].getLon();
			}
			if (getVert()[i].getLon() >= maxLon) {
				maxLon = getVert()[i].getLon();
			}
		}
		return new Vertex((maxLat - minLat) / 2 + minLat, (maxLon - minLon) / 2 + minLon,0l);
	}
	
	public Bounds extendBound(Bounds b) {
		if (b == null) {
			b = new Bounds();
		}
		for (int i = 0; i < 3; i++) {
			b.extend(getVert()[i].getLat(), getVert()[i].getLon());
		}
		return b;
	}

//	/**
//	 * @param vert the vert to set
//	 */
//	private void setVert(Vertex[] vert) {
//		this.vert = vert;
//	}

	/**
	 * @return the vert
	 */
	public Vertex[] getVert() {
		return vert;
	}
	
	public int equalVert(Triangle other){
		int ret = 0;
		for (int i = 0;i < 3; i++) {
			for (int j = 0;j < 3; j++) {
				if (getVert()[i].getNode() == other.getVert()[j].getNode()) {
					ret++;
				}
			}
		}
		return ret;
	}

}
