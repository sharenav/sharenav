package de.ueller.osmToGpsMid.model;

import java.util.LinkedList;
import java.util.List;

import de.ueller.osmToGpsMid.Constants;

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
			type=9;
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
		if (type!= 0)
			return type;
		// types > 50 represent Areas
		t = (String) tags.get("landuse");
		if ("farm".equals(t)){
			type=Constants.AREA_LANDUSE_FARM;
		}
		if ("quarry".equals(t)){
			type=Constants.AREA_LANDUSE_QUARRY;
		}
		if ("landfill".equals(t)){
			type=Constants.AREA_LANDUSE_LANDFILL;
		}
		if ("basin".equals(t)){
			type=Constants.AREA_LANDUSE_BASIN;
		}
		if ("reservoir".equals(t)){
			type=Constants.AREA_LANDUSE_RESERVOIR;
		}
		if ("forest".equals(t)){
			type=Constants.AREA_LANDUSE_FOREST;
		}
		if ("allotments".equals(t)){
			type=Constants.AREA_LANDUSE_ALLOTMENTS;
		}
		if ("residential".equals(t)){
			type=Constants.AREA_LANDUSE_RESIDENTIAL;
		}
		if ("retail".equals(t)){
			type=Constants.AREA_LANDUSE_RETAIL;
		}
		if ("commercial".equals(t)){
			type=Constants.AREA_LANDUSE_COMMERCIAL;
		}
		if ("industrial".equals(t)){
			type=Constants.AREA_LANDUSE_INDUSTRIAL;
		}
		if ("brownfield".equals(t)){
			type=Constants.AREA_LANDUSE_BROWNFIELD;
		}
		if ("greenfield".equals(t)){
			type=Constants.AREA_LANDUSE_GREENFIELD;
		}
		if ("cementry".equals(t)){
			type=Constants.AREA_LANDUSE_CEMETERY;
		}
		if ("village_green".equals(t)){
			type=Constants.AREA_LANDUSE_VILLAGE_GREEN;
		}
		if ("recreation_ground".equals(t)){
			type=Constants.AREA_LANDUSE_RECREATION_GROUND;
		}
		if (type!= 0)
			return type;
		// types > 50 represent Areas
		t = (String) tags.get("leisure");
		if ("park".equals(t)){
			type=Constants.AREA_LEISURE_PARK;
		}
		if (type!= 0)
			return type;
		// types > 50 represent Areas
		t = (String) tags.get("waterway");
		if ("riverbank".equals(t)){
			type=Constants.AREA_NATURAL_WATER;
		}
		if ("river".equals(t)){
			type=Constants.WAY_WATERWAY_RIVER;
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
