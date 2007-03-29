package de.ueller.osmToGpsMid.model;

import java.util.LinkedList;
import java.util.List;

public class Way extends Entity implements Comparable<Way>{
	public List<Line> lines = new LinkedList<Line>();
	Bounds bound=null;
/**
 * indicate that this Way is already written to output;
 */
	public boolean used=false;


	public Way(long id) {
		this.id=id;
	}
	
	public byte getType(){
		byte type=0;
		String t = (String) tags.get("highway");
		if ("unclassified".equals(t)){
			type=10;
		} else if ("motorway".equals(t)){
			type=2;
		} else if ("motorway_link".equals(t)){
			type=2;
		} else if ("trunk".equals(t)){
			type=3;
		} else if ("primary".equals(t)){
			type=4;
		} else if ("secondary".equals(t)){
			type=5;
		} else if ("minor".equals(t)){
			type=6;
		} else if ("residential".equals(t)){
			type=7;
		}
		if (type!= 0)
			return type;
		// types > 50 represent Areas
		t = (String) tags.get("amenity");
		if ("parking".equals(t)){
			type=50;
//			System.out.println("found parkin Area");
		}
		if (type!= 0)
			return type;
		// types > 50 represent Areas
		t = (String) tags.get("natural");
		if ("water".equals(t)){
			type=60;
		}
		return type;
	}
	
	public byte getZoomlevel(){
		byte type=getType();
		switch (type){
			case 2:
			case 3: return 0;
			case 4: return 1;
			case 5:
			case 6: return 2;
			case 7: return 3;
			case 10: return 3;
			default: return 3;
		}
	}

	public int compareTo(Way o) {
		byte t1=getType();
		byte t2=o.getType();
		if (t1 < t2)
			return 1;
		else if (t1 > t2)
			return -1;
		return 0;
	}
	
	public Bounds getBounds(){
		if (bound == null){
			bound=new Bounds();
			for (Line line : lines) {
				if (line.isValid()){
					bound.extend(line.from.lat,line.to.lon);
					bound.extend(line.to.lat,line.to.lon);
				}
			}
		}
		return bound;
	}

	public void clearBounds() {
		bound=null;
	}
	public String toString(){
		return "Way "+ getName() + " with "+ lines.size() + " segments";
	}
}
