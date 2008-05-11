package de.ueller.osmToGpsMid.model;

import java.util.Hashtable;
import java.util.Set;


import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.Constants;


public class Node extends Entity{
	/**
	 * the position in target array of nodes
	 */
	public int renumberdId;
	/**
	 * Latitude of this node
	 */
	public float lat;
	/**
	 * Longitude of this node;
	 */
	public float lon;
	/**
	 * type of this Node
	 */
	private byte type=-1;
	//public byte noConfType=-1;
	public boolean used=false;
	public byte connectedLineCount=0;
//	private Set<Way> connectedWays = new HashSet<Way>();
	public RouteNode routeNode;
	
	public Node(float node_lat, float node_lon, long id) {
		lat = node_lat;
		lon = node_lon;
		this.id = id;
	}
	public String getName() {
		if (type != -1) {
			POIdescription desc = Configuration.getConfiguration().getpoiDesc(type);
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
	
	public String getPlace(){
		String place = (getAttribute("place"));
//		System.out.println("Read place for id="+id+" as=" + place);
		if (place != null) return place.trim();
		return null;
	}
	public boolean isPlace() {
		if (type != -1) {
			POIdescription desc = Configuration.getConfiguration().getpoiDesc(type);			
			if (desc.key.equalsIgnoreCase("place"))
				return true;
		}
		return false;
	}

	public byte getType(Configuration c){
		if (c != null){
			if (type == -1) {
				type = calcType(c);
			}
			return type;
		} else {
			if (type == -1) {
				type = calcType(c);
			}
			return type;			
		}
	

	}
	private byte calcType(Configuration c){		
		if (c != null) {
			Hashtable<String, Hashtable<String,POIdescription>> legend = c.getPOIlegend();
			if (legend != null) {				
				Set<String> tags = getTags();
				if (tags != null) {
					for (String s: tags) {						
						Hashtable<String,POIdescription> keyValues = legend.get(s);
						if (keyValues != null) {							
							POIdescription poi = keyValues.get(getAttribute(s));
							if (poi == null) {
								poi = keyValues.get("*");
							}
							if (poi != null) {
								type = poi.typeNum;
								//System.out.println(toString() + " is a " + poi.description);
								return poi.typeNum;
							}
						}
					}
				}			
			}
		}
		return -1;		
	}
	
	public byte getZoomlevel(Configuration c){
		if (type == -1) {
			//System.out.println("unknown type for node " + toString());
			return 3;
		}
		int maxScale = c.getpoiDesc(type).minImageScale;		
		if (maxScale < 45000)
			return 3;
		if (maxScale < 180000)
			return 2;
		if (maxScale < 900000)
			return 1;
		return 0;
	}
	public String toString(){
		return "node (" + lat + "|" + lon + ") " + ((getPlace() != null)?("(" + getPlace() + ") "):"") + " id=" + id + " name="+getName() + ((nearBy == null)?"":(" by " + nearBy.getName()));
	}
	/**
	 * @return
	 */
	public byte getNameType() {
		String t = getPlace();
		if (t != null){
			if ("suburb".equals(t)){
				return (Constants.NAME_SUBURB);
			} else {
			    return (Constants.NAME_CITY);
			}
		}
		return Constants.NAME_AMENITY;
	}


}
