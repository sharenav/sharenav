package de.ueller.osmToGpsMid.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;
import de.ueller.osmToGpsMid.MyMath;
import de.ueller.osmToGpsMid.model.name.Names;

public class Way extends Entity implements Comparable<Way>{
	
	public static final byte WAY_FLAG_NAME = 1;
	public static final byte WAY_FLAG_MAXSPEED = 2;
	public static final byte WAY_FLAG_ONEWAY = 16;
	public static final byte WAY_FLAG_MULTIPATH = 4;
	public static final byte WAY_FLAG_LONGWAY = 8;
	public static final byte WAY_FLAG_NAMEHIGH = 32;
//	public static final byte WAY_FLAG_ISINHIGH = 64;
	
	//public List<Line> lines = new LinkedList<Line>();
	public Path path=null;
	Bounds bound=null;
	
	public static Configuration config;
/**
 * indicate that this Way is already written to output;
 */
	public boolean used=false;
	private byte type = -1;

	public Way(long id) {
		this.id=id;
	}
	
	/**
	 * create a new Way which shares the tags with the other way, has
	 * the same type and id, but no Nodes
	 * @param other
	 */
	public Way(Way other) {
		super(other);		
		this.type=other.type;
	}

	/*
	private byte getJunctionType(){
		String t = getAttribute("junction");
		if ("roundabout".equals(t)){
			return Constants.WAY_JUNCTION_ROUNDABOUT;
		}
		return 0;
	}
	*/
		
	public boolean isHighway(){
		return (getAttribute("highway") != null);
	}
	public String getMotorcar(){
		return (getAttribute("motorcar"));
	}
	public boolean isAccessByCar(){
		if (config == null)
			config = Configuration.getConfiguration();
		/*String way =getAttribute("highway");
		if (way == null)
			return false;
		if (getType() > Constants.WAY_HIGHWAY_UNCLASSIFIED){
			return false;
		}
		if ("restricted".equalsIgnoreCase(getMotorcar())){
			return false;
		}
		return true;
		*/
		WayDescription wayDesc = config.getWayDesc(getType());
		if (wayDesc == null)
			return false;
		
		return wayDesc.routable;		
	}
/*
	private byte getHighwayType(){
		String t = getAttribute("highway");
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
		String t = getAttribute("railway");
		if ("rail".equals(t)){
			return Constants.WAY_RAILWAY_RAIL;
		}
		if ("subway".equals(t)){
			return Constants.WAY_RAILWAY_SUBWAY;
		}
		return Constants.WAY_RAILWAY_UNCLASSIFIED;
		
	}

	private byte getAmenityType(){
		String t = getAttribute("amenity");
		if ("parking".equals(t)){
			return Constants.AREA_AMENITY_PARKING;
		}
		if ("public_building".equals(t)){
			return Constants.AREA_AMENITY_PUBLIC_BUILDING;
		}
		return Constants.AREA_AMENITY_UNCLASSIFIED;
		
	}
	private byte getNaturalType(){
		String t = getAttribute("natural");
		if ("water".equals(t)){
			return Constants.AREA_NATURAL_WATER;
		}
		return 0;
	}
	private byte getLanduseType(){
		String t = getAttribute("landuse");
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
		String t = getAttribute("leisure");
		if ("park".equals(t)){
			return Constants.AREA_LEISURE_PARK;
		}
		return 0;
	}
	private byte getWaterwayType(){
		String t = getAttribute("waterway");
		if ("river".equals(t)){
			return Constants.WAY_WATERWAY_RIVER;
		}
		if ("canal".equals(t)){
			return Constants.WAY_WATERWAY_RIVER;
		}
		if ("riverbank".equals(t)){
			return Constants.AREA_NATURAL_WATER;
		}
		return 0;
	}

	
	private byte get_Type(Configuration c){
		if (c.useHighway){
			if (containsKey("highway")){
				return getHighwayType();
			}
			if (containsKey("junction")){
				return getJunctionType();
			}			
		}
		if (c.useRailway){
			if (containsKey("railway")){
				return getRailwayType();
			}
		}
		if (c.useAmenity){
			if (containsKey("amenity")){
				return getAmenityType();
			}
		}
		if (c.useNatural){
			if (containsKey("natural")){
				return getNaturalType();
			}
		}
		if (c.useLanduse){
			if (containsKey("landuse")){
				return getLanduseType();
			}
		}
		if (c.useLeisure){
			if (containsKey("leisure")){
				return getLeisureType();
			}
		}
		if (c.useWaterway){
			if (containsKey("waterway")){
				return getWaterwayType();
			}
		}
		return 0;
		
	}	
	*/
	
    public byte getType(Configuration c){
    	if (type == -1) {
			type = calcType(c);
		}
		return type;
	}
    public byte getType(){
    	return type;
	}   
    
	private byte calcType(Configuration c){
		//System.out.println("Calculating type for " + toString());
		if (c != null) {			
			Hashtable<String, Hashtable<String,WayDescription>> legend = c.getWayLegend();
			if (legend != null) {				
				Set<String> tags = getTags();
				if (tags != null) {
					for (String s: tags) {						
						Hashtable<String,WayDescription> keyValues = legend.get(s);
						//System.out.println("Calculating type for " + toString() + " " + s + " " + keyValues);
						if (keyValues != null) {
							//System.out.println("found key index for " + s);
							WayDescription way = keyValues.get(getAttribute(s));
							if (way != null) {
								type = way.typeNum;
								//System.out.println(toString() + " is a " + way.description);
								way.noWaysOfType++;
								return way.typeNum;								
							}
						}
					}
				}			
			}
		}
		return -1;		
	}
	
	public String getName() {
		if (type != -1) {
			WayDescription desc = Configuration.getConfiguration().getWayDesc(type);
			if (desc != null) {
				String name = getAttribute(desc.nameKey);
				String nameFallback = getAttribute(desc.nameFallbackKey); 
				if (name != null && nameFallback != null) {
					name += " (" + nameFallback + ")";
				} else if ((name == null) && (nameFallback != null)) {
					name = nameFallback;
				}
				//System.out.println("New style name: " + name);
				return name!=null ? name.trim() : "";
			}
		}
		return null;
		//String name = getAttribute("name");
		//return name!=null ? name.trim() : "";
	}
	
	

	public byte getZoomlevel(Configuration c){
		byte type=getType();
		
		if (type == -1) {
			//System.out.println("unknown type for node " + toString());
			return 3;
		}
		int maxScale = c.getWayDesc(type).minScale;		
		if (maxScale < 45000)
			return 3;
		if (maxScale < 180000)
			return 2;
		if (maxScale >= 180000)
			return 1;
		
		return 3;
		
		/*switch (type){
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
		}*/
	}
    /**
     * get or estimate speed in m/s for routing purposes
     * @return
     */
	public float getSpeed(){
		if (config == null)
			config = Configuration.getConfiguration();
		float maxSpeed = Float.MAX_VALUE;
		if (containsKey("maxspeed")){
			try {
				maxSpeed=(Integer.parseInt(getAttribute("maxspeed")) / 3.6f);				
			} catch (NumberFormatException e) {
			}
		}
		float typicalSpeed = config.getWayDesc(type).typicalSpeed;
		if (typicalSpeed != 0)
			if (typicalSpeed < maxSpeed)
				maxSpeed = typicalSpeed;
		if (maxSpeed == Float.MAX_VALUE)
			maxSpeed = 60.0f; //Default case;
		return maxSpeed / 3.6f;
		
		/*switch (type){
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
		}*/		
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
		return getAttribute("is_in");
	}
	/**
	 * @return
	 */
	public byte getNameType() {
		String t = getAttribute("highway");
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
		String t = getAttribute("oneway");
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
	
/*	@Deprecated
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
*/	
	public void write(DataOutputStream ds,Names names1,Tile t) throws IOException{		
		Bounds b=new Bounds();
		int flags=0;
		int maxspeed=50;
		int nameIdx = -1;
		int isinIdx = -1;
		if (getName() != null && getName().trim().length() > 0){			
			flags+=WAY_FLAG_NAME;
			nameIdx = names1.getNameIdx(getName());
			if (nameIdx >= Short.MAX_VALUE) {
				flags += WAY_FLAG_NAMEHIGH;
			}
		}
		if (containsKey("maxspeed")){
			try {
				maxspeed=Integer.parseInt(getAttribute("maxspeed"));
				flags+=WAY_FLAG_MAXSPEED;
			} catch (NumberFormatException e) {
			}
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
//				flags+=WAY_FLAG_MULTIPATH;
				System.err.println("MULTIPATH");
			}
			if (longWays ){
				flags+=WAY_FLAG_LONGWAY;
			}
			if (isOneWay()){
				flags+=WAY_FLAG_ONEWAY;
			}
			ds.writeByte(flags);
			
			b=getBounds();
			ds.writeShort((short)(MyMath.degToRad(b.minLat - t.centerLat) * Tile.fpm));
			ds.writeShort((short)(MyMath.degToRad(b.minLon - t.centerLon) * Tile.fpm));
			ds.writeShort((short)(MyMath.degToRad(b.maxLat - t.centerLat) * Tile.fpm));
			ds.writeShort((short)(MyMath.degToRad(b.maxLon - t.centerLon) * Tile.fpm));
			
//			ds.writeByte(0x58);
			ds.writeByte(type);
			if ((flags & WAY_FLAG_NAME) == WAY_FLAG_NAME){
				if ((flags & WAY_FLAG_NAMEHIGH) == WAY_FLAG_NAMEHIGH){
					ds.writeInt(nameIdx);
				} else {
					ds.writeShort(nameIdx);
				}
			}
			if ((flags & WAY_FLAG_MAXSPEED) == WAY_FLAG_MAXSPEED){
				ds.writeByte(maxspeed);
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
			//If we split the way, the bounds are no longer valid
			this.clearBounds();
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
