package de.ueller.osmToGpsMid.model;

import java.util.LinkedList;
import java.util.List;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;

public class Way extends Entity implements Comparable<Way>{
	public List<Line> lines = new LinkedList<Line>();
	Bounds bound=null;
/**
 * indicate that this Way is already written to output;
 */
	public boolean used=false;
	private byte type;


	public Way(long id) {
		this.id=id;
	}
	
	public Way(Way other) {
		this.id=other.id;
		this.tags=other.tags;
		this.type=other.type;
	}

	private byte getJunctionType(){
		String t = (String) tags.get("junction");
		if ("roundabout".equals(t)){
			return Constants.WAY_JUNCTION_ROUNDABOUT;
		}
		return 0;
	}
		
	public boolean isHighway(){
		return (tags.get("highway") != null);
	}

	private byte getHighwayType(){
		String t = (String) tags.get("highway");
		if ("unclassified".equals(t)){
			return Constants.WAY_HIGHWAY_UNCLASSIFIED;
		}
		if ("motorway".equals(t)){
			return Constants.WAY_HIGHWAY_MOTORWAY;
		}
		if ("motorway_link".equals(t)){
			return Constants.WAY_HIGHWAY_MOTORWAY_LINK;
		}
		if ("trunk".equals(t)){
			return Constants.WAY_HIGHWAY_TRUNK;
		}
		if ("primary".equals(t)){
			return Constants.WAY_HIGHWAY_PRIMARY;
		}
		if ("secondary".equals(t)){
			return Constants.WAY_HIGHWAY_SECONDARY;
		}
		if ("minor".equals(t)){
			return Constants.WAY_HIGHWAY_MINOR;
		}
		if ("residential".equals(t)){
			return Constants.WAY_HIGHWAY_RESIDENTIAL;
		}
		return Constants.WAY_HIGHWAY_UNCLASSIFIED;
		
	}
	private byte getRailwayType(){
		String t = (String) tags.get("railway");
		if ("rail".equals(t)){
			return Constants.WAY_RAILWAY_RAIL;
		}
		if ("subway".equals(t)){
			return Constants.WAY_RAILWAY_SUBWAY;
		}
		return Constants.WAY_RAILWAY_UNCLASSIFIED;
		
	}

	private byte getAmenityType(){
		String t = (String) tags.get("amenity");
		if ("parking".equals(t)){
			return Constants.AREA_AMENITY_PARKING;
		}
		if ("public_building".equals(t)){
			return Constants.AREA_AMENITY_PUBLIC_BUILDING;
		}
		return Constants.AREA_AMENITY_UNCLASSIFIED;
		
	}
	private byte getNaturalType(){
		String t = (String) tags.get("natural");
		if ("water".equals(t)){
			return Constants.AREA_NATURAL_WATER;
		}
		return 0;
	}
	private byte getLanduseType(){
		String t = (String) tags.get("landuse");
		if ("farm".equals(t)){
			return Constants.AREA_LANDUSE_FARM;
		}
		if ("quarry".equals(t)){
			return Constants.AREA_LANDUSE_QUARRY;
		}
		if ("landfill".equals(t)){
			return Constants.AREA_LANDUSE_LANDFILL;
		}
		if ("basin".equals(t)){
			return Constants.AREA_LANDUSE_BASIN;
		}
		if ("reservoir".equals(t)){
			return Constants.AREA_LANDUSE_RESERVOIR;
		}
		if ("forest".equals(t)){
			return Constants.AREA_LANDUSE_FOREST;
		}
		if ("allotments".equals(t)){
			return Constants.AREA_LANDUSE_ALLOTMENTS;
		}
		if ("residential".equals(t)){
			return Constants.AREA_LANDUSE_RESIDENTIAL;
		}
		if ("retail".equals(t)){
			return Constants.AREA_LANDUSE_RETAIL;
		}
		if ("commercial".equals(t)){
			return Constants.AREA_LANDUSE_COMMERCIAL;
		}
		if ("industrial".equals(t)){
			return Constants.AREA_LANDUSE_INDUSTRIAL;
		}
		if ("brownfield".equals(t)){
			return Constants.AREA_LANDUSE_BROWNFIELD;
		}
		if ("greenfield".equals(t)){
			return Constants.AREA_LANDUSE_GREENFIELD;
		}
		if ("cementry".equals(t)){
			return Constants.AREA_LANDUSE_CEMETERY;
		}
		if ("village_green".equals(t)){
			return Constants.AREA_LANDUSE_VILLAGE_GREEN;
		}
		if ("recreation_ground".equals(t)){
			return Constants.AREA_LANDUSE_RECREATION_GROUND;
		}
		return 0;
	}

	private byte getLeisureType(){
		String t = (String) tags.get("leisure");
		if ("park".equals(t)){
			return Constants.AREA_LEISURE_PARK;
		}
		return 0;
	}
	private byte getWaterwayType(){
		String t = (String) tags.get("waterway");
		if ("river".equals(t)){
			return Constants.WAY_WATERWAY_RIVER;
		}
		if ("riverbank".equals(t)){
			return Constants.AREA_NATURAL_WATER;
		}
		return 0;
	}

	
	private byte get_Type(Configuration c){
		if (c.useHighway){
			if (tags.containsKey("highway")){
				return getHighwayType();
			}
			if (tags.containsKey("junction")){
				return getJunctionType();
			}			
		}
		if (c.useRailway){
			if (tags.containsKey("railway")){
				return getRailwayType();
			}
		}
		if (c.useAmenity){
			if (tags.containsKey("amenity")){
				return getAmenityType();
			}
		}
		if (c.useNatural){
			if (tags.containsKey("natural")){
				return getNaturalType();
			}
		}
		if (c.useLanduse){
			if (tags.containsKey("landuse")){
				return getLanduseType();
			}
		}
		if (c.useLeisure){
			if (tags.containsKey("leisure")){
				return getLeisureType();
			}
		}
		if (c.useWaterway){
			if (tags.containsKey("waterway")){
				return getWaterwayType();
			}
		}
		return 0;
		
	}
    public byte getType(Configuration c){
    	type=get_Type(c);
    	return type;
	}
    public byte getType(){
    	return type;
	}
	
	public byte getZoomlevel(){
		byte type=getType();
		switch (type){
			case Constants.WAY_HIGHWAY_MOTORWAY:
			case Constants.WAY_HIGHWAY_TRUNK: 
			case Constants.WAY_RAILWAY_RAIL:
				return 0;
			case Constants.WAY_HIGHWAY_PRIMARY:
			case Constants.WAY_JUNCTION_ROUNDABOUT:
			case Constants.AREA_NATURAL_WATER:
			case Constants.WAY_WATERWAY_RIVER:
				return 1;
			case Constants.WAY_HIGHWAY_SECONDARY:
			case Constants.WAY_HIGHWAY_MINOR: 
			case Constants.WAY_RAILWAY_SUBWAY:
				return 2;
			case Constants.WAY_HIGHWAY_RESIDENTIAL: 
				return 3;

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
		return "Way "+ getName()  + ((nearBy == null)?"":(" by " + nearBy));
	}

	/**
	 * @return
	 */
	public String getIsIn() {
		return tags.get("is_in");
	}
	/**
	 * @return
	 */
	public byte getNameType() {
		String t = (String) tags.get("highway");
		if (t != null){
			return (Constants.NAME_STREET);
		}
		return Constants.NAME_AMENITY;
	}

	/**
	 * @return
	 */
	public Node getMidPoint() {
		int splitp=lines.size()/2;
		Line splitLine=lines.get(splitp);
		return (splitLine.to);
	}
}
