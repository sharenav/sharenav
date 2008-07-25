package de.ueller.osmToGpsMid;

import java.util.LinkedList;

import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Way;


public class SplitLongWays {
	OxParser parser;
	LinkedList<Way> added=new LinkedList<Way>();
	LinkedList<Way> deleted=new LinkedList<Way>();
	
	
	public SplitLongWays(OxParser parser) {
		super();
		this.parser = parser;
		for (Way way : parser.getWays()) {
			testAndSplit(way);
		}
		for (Way w : added) {			
			parser.addWay(w);
		}
		added=null;
	}

	private void testAndSplit(Way way) {
//		if (nonCont && way.getSegmentCount() == 1) return;
		if (way.getType() >= 50) return;
		Bounds b=way.getBounds();
		if ((b.maxLat-b.minLat) > 0.09f 
				|| (b.maxLon-b.minLon) > 0.09f ){
			Way newWay=way.split();
			if (newWay != null){
				added.add(newWay);
				testAndSplit(way);
				testAndSplit(newWay);
			} 
		}
	}
}
