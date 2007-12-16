package de.ueller.osmToGpsMid.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;
import de.ueller.osmToGpsMid.MyMath;
import de.ueller.osmToGpsMid.model.name.Names;

public class Way extends Entity implements Comparable<Way>{
	public List<Line> lines = new LinkedList<Line>();
	public Path path=null;
	Bounds bound=null;
/**
 * indicate that this Way is already written to output;
 */
	public boolean used=false;
	private byte type;

	public Way(long id) {
		this.id=id;
	}
	
	/**
	 * create a new Way which shares the tags with the other way, has
	 * the same type and id, but no Nodes
	 * @param other
	 */
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
	public String getMotorcar(){
		return (tags.get("motorcar"));
	}
	public boolean isAccessByCar(){
		String way = tags.get("highway");
		if (way == null)
			return false;
		if (getType() > Constants.WAY_HIGHWAY_UNCLASSIFIED){
			return false;
		}
		if ("restricted".equalsIgnoreCase(getMotorcar())){
			return false;
		}
		return true;
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
		if ("cycleway".equals(t)){
			return Constants.WAY_HIGHWAY_CYCLEWAY;
		}
		if ("footway".equals(t)){
			return Constants.WAY_HIGHWAY_FOOTWAY;
		}
		if ("track".equalsIgnoreCase(t)){
			return Constants.WAY_HIGHWAY_TRACK;
		}
		if ("steps".equalsIgnoreCase(t)){
			return Constants.WAY_HIGHWAY_STEPS;
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
    /**
     * get or estimate speed in m/s
     * @return
     */
	public float getSpeed(){
		if (tags.containsKey("maxspeed")){
			try {
				int maxspeed=Integer.parseInt((String) tags.get("maxspeed"));
				return (maxspeed/3.6f);
			} catch (NumberFormatException e) {
			}
		}
		switch (type){
		case Constants.WAY_HIGHWAY_MOTORWAY_LINK:
			return 60f/3.6f;
		case Constants.WAY_HIGHWAY_MOTORWAY:
			return 130f/3.6f;
		case Constants.WAY_HIGHWAY_TRUNK: 
			return 100f/3.6f;
		case Constants.WAY_HIGHWAY_PRIMARY:
			return 100f/3.6f;
		case Constants.WAY_JUNCTION_ROUNDABOUT:
			return 30f/3.6f;
		case Constants.WAY_HIGHWAY_SECONDARY:
			return 80f/3.6f;
		case Constants.WAY_HIGHWAY_RESIDENTIAL:
			return 50f/3.6f;
		case Constants.WAY_HIGHWAY_MINOR: 
			return 60f/3.6f;
		case Constants.WAY_HIGHWAY_TRACK:
			return 25f/3.6f;
		default: return 60f/3.6f;
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
			path.extendBounds(bound);
		}
		return bound;
	}

	public void clearBounds() {
		bound=null;
	}
	public String toString(){
		return "Way(" + id + ") "+ getName()  + ((nearBy == null)?"":(" by " + nearBy)) + " type=" + getType();
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
		List<Node> nl=path.getSubPaths().getFirst().getNodes();
		int splitp =nl.size()/2;
		return (nl.get(splitp));
	}

	/**
	 * @return
	 */
	public boolean isOneWay() {
		String t = (String) tags.get("oneway");
		if (t==null)
			return false;
		if ("true".equalsIgnoreCase(t)){
			return true;
		}
		if ("yes".equalsIgnoreCase(t)){
			return true;
		}
		return false;
	}
	
	@Deprecated
	public void write_old(DataOutputStream ds,Names names1) throws IOException{
		Bounds b=new Bounds();
//		System.out.println("write way "+w);
		int flags=0;
		int maxspeed=50;
		if (getName() != null){
			flags+=1;
		}
		if (tags.containsKey("maxspeed")){
			try {
				maxspeed=Integer.parseInt((String) tags.get("maxspeed"));
				flags+=2;
			} catch (NumberFormatException e) {
			}
		}
		if (getIsIn() != null){
			flags+=16;
		}
		byte type=getType();
		Integer p1=null;
		ArrayList<ArrayList<Integer>> paths=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> path = new ArrayList<Integer>();
		boolean isWay=false;
		boolean multipath=false;
		paths.add(path);
		isWay=false;
		if (lines != null){
		for (Iterator iterw = lines.iterator(); iterw.hasNext();) {
			try {
				Line l=(Line) iterw.next();
				if (p1 == null){
					p1=new Integer(l.from.renumberdId);
//					System.out.println("Start Way at " + l.from);
					path.add(p1);
					b.extend(l.from.lat, l.from.lon);
				} else {
					if (l.from.renumberdId != p1.intValue()){
						if (getType() >= 50){
							// insert segment, because this is a area
							path.add(new Integer(l.from.renumberdId));
						}
						// non continues path so open a new Path
						multipath=true;
						path = new ArrayList<Integer>();
						paths.add(path);
						p1=new Integer(l.from.renumberdId);
//						System.out.println("\tStart Way-Segment at " + l.from);
						path.add(p1);
						b.extend(l.from.lat, l.from.lon);
					}
				}
//				System.out.println("\t\tContinues Way " + l.to);
				path.add(new Integer(l.to.renumberdId));
				isWay=true;
				p1=new Integer(l.to.renumberdId);
				b.extend(l.to.lat,l.to.lon);
			} catch (RuntimeException e) {
			}
		}
		} else {
			
		}
		if (isWay){
			boolean longWays=false;
			for (ArrayList<Integer> subPath : paths){
				if (subPath.size() >= 255){
					longWays=true;
				}
			}
			if (multipath ){
				flags+=4;
			}
			if (longWays ){
				flags+=8;
			}
			ds.writeByte(flags);
			ds.writeFloat(MyMath.degToRad(b.minLat));
			ds.writeFloat(MyMath.degToRad(b.minLon));
			ds.writeFloat(MyMath.degToRad(b.maxLat));
			ds.writeFloat(MyMath.degToRad(b.maxLon));
//			ds.writeByte(0x58);
			ds.writeByte(type);
			if ((flags & 1) == 1){
				ds.writeShort(names1.getNameIdx(getName()));
			}
			if ((flags & 2) == 2){
				ds.writeByte(maxspeed);
			}
			if ((flags & 16) == 16){
				ds.writeShort(names1.getNameIdx(getIsIn()));
			}
			if ((flags & 4) == 4){
				ds.writeByte(paths.size());
			}
//			System.out.print("Way Paths="+paths.size());
			for (ArrayList<Integer> subPath : paths){
				if (longWays){
					ds.writeShort(subPath.size());
				} else {
					ds.writeByte(subPath.size());
				}
//				System.out.print("Path="+subPath.size());
				for (Integer l : subPath) {
//					System.out.print(" "+l.intValue());
					ds.writeShort(l.intValue());
				}
// only for test integrity
//				System.out.println("   write magic code 0x59");
//				ds.writeByte(0x59);
			}
		} else {
			ds.write(128); // flag that mark there is no way
		}

	}
	
	public void write(DataOutputStream ds,Names names1) throws IOException{
		Bounds b=new Bounds();
		int flags=0;
		int maxspeed=50;
		if (getName() != null){
			flags+=1;
		}
		if (tags.containsKey("maxspeed")){
			try {
				maxspeed=Integer.parseInt((String) tags.get("maxspeed"));
				flags+=2;
			} catch (NumberFormatException e) {
			}
		}
		if (getIsIn() != null){
			flags+=16;
		}
		byte type=getType();
		boolean isWay=false;
		boolean longWays=false;
		for (SubPath s:path.getSubPaths()){
			if (s.size() >= 255){
				longWays=true;}

			if (s.size() >1){
				isWay=true;
			}
		}
		if (isWay){
			if (path.isMultiPath()){
				flags+=4;
			}
			if (longWays ){
				flags+=8;
			}
			ds.writeByte(flags);
			b=getBounds();
			ds.writeFloat(MyMath.degToRad(b.minLat));
			ds.writeFloat(MyMath.degToRad(b.minLon));
			ds.writeFloat(MyMath.degToRad(b.maxLat));
			ds.writeFloat(MyMath.degToRad(b.maxLon));
//			ds.writeByte(0x58);
			ds.writeByte(type);
			if ((flags & 1) == 1){
				ds.writeShort(names1.getNameIdx(getName()));
			}
			if ((flags & 2) == 2){
				ds.writeByte(maxspeed);
			}
			if ((flags & 16) == 16){
				ds.writeShort(names1.getNameIdx(getIsIn()));
			}
			if ((flags & 4) == 4){
				ds.writeByte(path.getPathCount());
			}
			for (SubPath s:path.getSubPaths()){
				if (longWays){
					ds.writeShort(s.size());
				} else {
					ds.writeByte(s.size());
				}
				for (Node n : s.getNodes()) {
					ds.writeShort(n.renumberdId);
				}
// only for test integrity
//				System.out.println("   write magic code 0x59");
//				ds.writeByte(0x59);
			}
		} else {
			ds.write(128); // flag that mark there is no way
		}

	}

	public void add(Node n){
		if (path == null){
			path=new Path();
		}
		path.add(n);
	}
	
	public void startNextSegment(){
		if (path == null){
			path=new Path();
		}
		path.addNewSegment();
	}
	

	/**
	 * @param no
	 * @param n
	 */
	public void replace(Node no, Node n) {
		path.replace(no,n);
	}
	public void replace(HashMap<Node,Node> replaceNodes) {
		path.replace(replaceNodes);
	}

	public List<SubPath> getSubPaths() {
		return path.getSubPaths();
	}
	
	public int getLineCount(){
		return path.getLineCount();
	}
	
	public Way split(){
		if (! isValid() )
			System.out.println("Way before split is not valid");
		Path split = path.split();
		if (split != null){
			Way newWay=new Way(this);
			newWay.path=split;
			if (! newWay.isValid() )
				System.out.println("new Way after split is not valid");
			if (! isValid() )
				System.out.println("old Way after split is not valid");
			return newWay;
		}
		return null;
	}
	public boolean isValid(){
		if (path==null)
			return false;
		path.clean();
		if (path.getPathCount() == 0)
			return false;
		return true;
	}
}
