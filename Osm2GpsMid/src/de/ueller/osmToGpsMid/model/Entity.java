package de.ueller.osmToGpsMid.model;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import de.ueller.osmToGpsMid.SmallArrayMap;


public class Entity {

	/**
	 * the OSM id of this node
	 */
	//public Long	id;
	public Node nearBy;	
	/**
	 * The tags for this object  
	 * Key: String  Value: String
	 */
	private Map<String,String> tags;	
	
	public Entity() {
	}

	public Entity(Entity other) {
		//this.id = other.id;
		this.tags=other.tags;
	}
	
	public void cloneTags(Entity other) {
		//this.id = other.id;
		this.tags=other.tags;		
	}
	
	/**
	 * @param tags
	 */
	public void replaceTags(Entity other) {
		this.tags=other.tags;
		
	}

	/**
	 * Deletes the given tag from the list of this entity's tags.
	 * @param key Tag to delete
	 */
	public void deleteTag(String key) {
		if (tags != null) {
			tags.remove(key);
		}		
	}
	
	public String getName() {
		if (tags == null) {
			return null;
		}
		return tags.get("name");
	}
	
	public String getUrl() {
		if (tags == null) {
			return null;
		}
		return tags.get("url");
	}

	public String getPhone() {
		if (tags == null) {
			return null;
		}
		return tags.get("phone");
	}

	public void setAttribute(String key, String value) {
		if (tags == null) {
			tags = new SmallArrayMap<String,String>();
		}
		tags.put(key, value);
	}
	
	public String getAttribute(String key) {
		if (tags == null) {
			return null;
		}
		return tags.get(key);
	}
	
	public boolean containsKey(String key) {
		if (tags == null) {
			return false;
		}
		return tags.containsKey(key);
	}

	public Set<String> getTags() {
		if (tags == null) {
			return new HashSet<String>();
		}
		return tags.keySet();
	}
	
	protected EntityDescription calcType(Hashtable<String, Hashtable<String,Set<EntityDescription>>> legend){
		EntityDescription entityDes = null;

		//System.out.println("Calculating type for " + toString());
		if (legend != null) {
			Set<String> tags = getTags();
			if (tags != null) {
				byte currentPrio = Byte.MIN_VALUE;
				for (String s: tags) {
					Hashtable<String,Set<EntityDescription>> keyValues = legend.get(s);
//					System.out.println("Calculating type for " + toString() + " " + s + " " + keyValues);
					if (keyValues != null) {
//						System.out.println("found key index for " + s);
						Set<EntityDescription> ways = keyValues.get(getAttribute(s));
						if (ways == null) {
							ways = keyValues.get("*");
						}
						if (ways != null) {
							for (EntityDescription entity : ways) {
								if ((entity != null) && (entity.rulePriority > currentPrio)) {
									boolean failedSpecialisations = false;
									if (entity.specialisation != null) {
										boolean failedSpec = false;
										for (ConditionTuple ct : entity.specialisation) {
//											System.out.println("Testing specialisation " + ct + " on " + this);
											failedSpec = !ct.exclude;
											for (String ss : tags) {
												if ( (ss.equalsIgnoreCase(ct.key)) &&
													(
														getAttribute(ss).equalsIgnoreCase(ct.value) ||
														ct.value.equals("*")
													)
												) {
													failedSpec = ct.exclude;
												}
											}
											if (failedSpec) {
												failedSpecialisations = true;
											}
										}
									}
									if (!failedSpecialisations) {
										currentPrio = entity.rulePriority;
										entityDes = entity;
									}
								}
							}
						}
					}
				}
				return entityDes;
			}
		}
		return null;
	}
}
