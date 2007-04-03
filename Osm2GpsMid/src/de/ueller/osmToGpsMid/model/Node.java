package de.ueller.osmToGpsMid.model;

import de.ueller.osmToGpsMid.Constants;


public class Node extends Entity{
	/**
	 * the position in target array of nodes
	 */
	public short renumberdId;
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
	public byte type=-1;
	public boolean used=false;
	
	public Node(float node_lat, float node_lon, long id) {
		lat = node_lat;
		lon = node_lon;
		this.id = id;
	}
	public String getName() {
		String name = (String)tags.get("name");
		return name!=null ? name.trim() : "";
	}
	
	public String getPlace(){
		String place = ((String)tags.get("place"));
//		System.out.println("Read place for id="+id+" as=" + place);
		if (place != null) return place.trim();
		return null;
	}
	public String getAmenity(){
		String amenity = ((String)tags.get("amenity"));
//		System.out.println("Read place for id="+id+" as=" + place);
		if (amenity != null) return amenity.trim();
		return null;
	}
	
	public byte getType(){
		if (type == -1) {
			type = calcType();
		}
//		System.out.println("Read type for id="+id+" as=" + type);		
		return type;
	}
	public byte calcType(){
		String p=getPlace();
		if (p != null){
			if ("city".equals(p)) return Constants.NODE_PLACE_CITY;
			if ("town".equals(p)) return Constants.NODE_PLACE_TOWN;
			if ("village".equals(p)) return Constants.NODE_PLACE_VILLAGE;
			if ("hamlet".equals(p)) return Constants.NODE_PLACE_HAMLET;
			if ("suburb".equals(p)) return Constants.NODE_PLACE_SUBURB;
		}
		p=getAmenity();
		if (p != null){
			if ("parking".equals(p)) return Constants.NODE_AMENITY_PARKING;
			if ("school".equals(p)) return Constants.NODE_AMENITY_SCHOOL;
			if ("telephone".equals(p)) return Constants.NODE_AMENITY_TELEPHONE;
			if ("fuel".equals(p)) return Constants.NODE_AMENITY_FUEL;
		}
		
		return 0;
		
	}
	
	public byte getZoomlevel(){
		switch (getType()) {
			case 1: return 0;
			case 2: return 1;
			case 3: return 2;
			case 4: return 3;
			case 5: return 3;
		}
		return 3;
	}
	public String toString(){
		return "node id=" + id + " name="+getName();
	}

}
