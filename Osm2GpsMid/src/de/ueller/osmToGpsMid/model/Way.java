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
	public static final byte WAY_FLAG_LAYER = 4;
	public static final byte WAY_FLAG_LONGWAY = 8;
	public static final byte WAY_FLAG_ONEWAY = 16;	
	public static final byte WAY_FLAG_NAMEHIGH = 32;
	public static final byte WAY_FLAG_AREA = 64;
	
	//Deprecated
	//public static final byte WAY_FLAG_MULTIPATH = 4;
	
	
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
	
	public void cloneTags(Way other) {
		super.cloneTags(other);		
		this.type=other.type;
	}

	public boolean isHighway(){
		return (getAttribute("highway") != null);
	}
	public String getMotorcar(){
		return (getAttribute("motorcar"));
	}
	public boolean isAccessByCar(){
		if (config == null)
			config = Configuration.getConfiguration();
		WayDescription wayDesc = config.getWayDesc(getType());
		if (wayDesc == null)
			return false;
		
		return wayDesc.routable;		
	}
	
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
							if (way == null) {
								way = keyValues.get("*");
							}
							if (way != null) {
								type = way.typeNum;								
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
		if (maxScale < 900000)
			return 1;
		
		return 0;		
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
		return Configuration.attrToBoolean(getAttribute("oneway"));
	}
	
	public boolean isExplicitArea() {
		return Configuration.attrToBoolean(getAttribute("area"));
	}

	public void write(DataOutputStream ds,Names names1,Tile t) throws IOException{		
		Bounds b=new Bounds();
		int flags=0;
		int maxspeed=50;
		int nameIdx = -1;
		int isinIdx = -1;
		byte layer = 0;
		
		if (config == null)
			config = Configuration.getConfiguration();
		
		byte type=getType();
		
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
		
		if (containsKey("layer")) {
			try {
				layer=(byte)Integer.parseInt(getAttribute("layer"));
				flags+=WAY_FLAG_LAYER;
			} catch (NumberFormatException e) {
			}
		}
		if ((config.getWayDesc(type).forceToLayer != 0)) {
			layer = config.getWayDesc(type).forceToLayer;
			flags |= WAY_FLAG_LAYER;
		}
		
		

		
		boolean isWay=false;
		boolean longWays=false;
		
		if (type < 1) {
			System.out.println("ERROR! Invalid way type for way " + toString());
		}
		
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
			if (isExplicitArea()) {				
				flags+=WAY_FLAG_AREA;				
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
			if ((flags & WAY_FLAG_LAYER) == WAY_FLAG_LAYER){
				ds.writeByte(layer);
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
