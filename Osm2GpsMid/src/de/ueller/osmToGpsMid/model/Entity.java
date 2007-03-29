package de.ueller.osmToGpsMid.model;

import java.util.Hashtable;
import java.util.Map;


public class Entity {

	/**
	 * the OSM id of this node
	 */
	public long	id;
	/**
	 * The tags for this object  
	 * Key: String  Value: String
	 */
	public Map<String,String> tags = new Hashtable<String,String>();
	public String getName() {
		return tags.get("name");
	}

}
