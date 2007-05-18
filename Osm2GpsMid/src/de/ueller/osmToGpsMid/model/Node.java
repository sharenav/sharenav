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
		if (amenity != null) return amenity.trim();
		return null;
	}
	public String getRailway(){
		String railway = ((String)tags.get("railway"));
		if (railway != null) return railway.trim();
		return null;
	}
	public String getAeroway(){
		String aeroway = ((String)tags.get("aeroway"));
		if (aeroway != null) return aeroway.trim();
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
		p=getRailway();
		if (p != null){
			if ("station".equals(p)) return Constants.NODE_RAILWAY_STATION;
			if ("halt".equals(p)) return Constants.NODE_RAILWAY_STATION;
		}
		p=getAeroway();
		if (p != null){
			if ("aerodrome".equals(p)) return Constants.NODE_AEROWAY_AERODROME;
		}
		
		return 0;
		
	}
	
	public byte getZoomlevel(){
		switch (getType()) {
			case Constants.NODE_PLACE_CITY:
			case Constants.NODE_AEROWAY_AERODROME:				
				return 0;
			case Constants.NODE_RAILWAY_STATION: 
			case Constants.NODE_AMENITY_PARKING: 
			case Constants.NODE_PLACE_TOWN: 
				return 1;
			case Constants.NODE_PLACE_VILLAGE: 
				return 2;
			case Constants.NODE_PLACE_HAMLET: 
				return 3;
			case Constants.NODE_PLACE_SUBURB: 
				return 3;
		}
		return 3;
	}
	public String toString(){
		return "node id=" + id + " name="+getName();
	}

}
