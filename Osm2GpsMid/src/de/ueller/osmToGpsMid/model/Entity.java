package de.ueller.osmToGpsMid.model;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class Entity {

	/**
	 * the OSM id of this node
	 */
	public long	id;
	public Node nearBy;	
	/**
	 * The tags for this object  
	 * Key: String  Value: String
	 */
	private Map<String,String> tags;	
	public int fid;
	
	public Entity() {
		
	}
	public Entity(Entity other) {
		this.id = other.id;		
		this.tags=other.tags;
	}
	
	public void cloneTags(Entity other) {
		this.id = other.id;		
		this.tags=other.tags;		
	}
	
	public String getName() {
		if (tags == null)
			return null;
		return tags.get("name");
	}
	
	public void setAttribute(String key, String value) {
		if (tags == null)
			tags = new HashMap<String,String>(4,0.8f);
		
		tags.put(key, value);
	}
	
	public String getAttribute(String key) {
		if (tags == null)
			return null;
		return tags.get(key);
	}
	
	public boolean containsKey(String key) {
		if (tags == null)
			return false;
		return tags.containsKey(key);
	}

	public Set<String> getTags() {
		if (tags == null)
			return null;
		return tags.keySet();
	}
}
