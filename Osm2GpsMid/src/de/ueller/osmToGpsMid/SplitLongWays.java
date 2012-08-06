/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007  Harald Mueller
 * 
 */
package de.ueller.osmToGpsMid;

import java.util.LinkedList;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Way;


public class SplitLongWays {
	OsmParser parser;
	LinkedList<Way> added=new LinkedList<Way>();
	
	
	public SplitLongWays(OsmParser parser) {
		super();
		int count = 0;
		this.parser = parser;
		for (Way way : parser.getWays()) {
			testAndSplit(way);
			count++;
			//if (count % 500 == 0) {
			//	System.out.println("Tested " + count 
			//			   + " ways for splitting");
			//}
		}
		for (Way w : added) {			
			parser.addWay(w);
		}
		count = 0;
		for (Way way : parser.getWays()) {
			if (way.isArea()) {
				if (Configuration.getConfiguration().triangleAreaFormat) {
					if (way.triangles == null) {
						way.triangulate();
					}
					if (way.triangles != null && way.triangles.size() > 0) {
						count++;
						//if (count % 500 == 0) {
						//	System.out.println("Did " + count 
						//			   + " recreatePath()s");
						//}
						way.recreatePathAvoidDuplicates();
					}
				}
			}

		}		
		added=null;
	}

	private void testAndSplit(Way way) {
//		if (nonCont && way.getSegmentCount() == 1) return;
// if w way is an Area, it's now also splitable
//		if ( way.isArea()) return;
		// TODO: Length of one longitude degree gets smallen when aproaching the poles.
		Bounds b=way.getBounds();
		if ((b.maxLat-b.minLat) > 0.09f 
				|| (b.maxLon-b.minLon) > 0.09f ){
			//System.out.println("Splitting " + (way.isArea() ? "area " :"way ") + way.toUrl());
			Way newWay=way.split();
			if (newWay != null){
				added.add(newWay);
//				if (way.isArea()){
//					DebugViewer v=DebugViewer.getInstanz(new ArrayList<Triangle>(way.triangles));
//					v.alt=new ArrayList<Triangle>(newWay.triangles);
//					v.recalcView();
//					v.repaint();
//				}
				testAndSplit(way);
				testAndSplit(newWay);
			} 
		}
	}
}
