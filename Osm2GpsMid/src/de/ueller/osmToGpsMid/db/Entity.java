/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.LockModeType;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import de.ueller.osmToGpsMid.model.ConditionTuple;
import de.ueller.osmToGpsMid.model.EntityDescription;





/**
 * @author hmueller
 *
 */


//@NamedQueries(value={
//		@NamedQuery(name="findWayByOsmId", lockMode=LockModeType.NONE,query="SELECT w FROM Way w WHERE w.osmId = :id"),
//		@NamedQuery(name="findRelByOsmId", lockMode=LockModeType.NONE,query="SELECT r FROM Relation r WHERE r.osmId = :id"),
//		@NamedQuery(name="findNodByOsmId", lockMode=LockModeType.NONE,query="SELECT n FROM Node n WHERE n.osmId = :id"),
//		
//	})



@javax.persistence.Entity
@Table(name = "ENTITY")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="EL_TYPE")
public  class Entity implements Serializable {
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	long id;
//	long osmId;
	private boolean used = false;
	private byte type = -1;
	@OneToMany(mappedBy = "parent",cascade={CascadeType.ALL})
	private final List<Tag> tagList = new ArrayList<Tag>();

	
	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}

	@Deprecated
	@Transient
	public long getOsmId() {
		return id;
	}


	public void setOsmId(long osmId) {
		this.id = osmId;
	}


	public boolean isUsed() {
		return used;
	}


	public void setUsed(boolean used) {
		this.used = used;
	}
	public byte getType() {
		return type;
	}
	public void setType(byte type) {
		this.type = type;
	}


	public List<Tag> getTagList() {
		return tagList;
	}


	public Tag addTag(String key, String value){
		Tag tag = new Tag(key,value,this);
		getTagList().add(tag);
		return tag;
	}

	public void cloneTags(Way other) {
		for (Tag t:other.getTagList()){	
			addTag(t.getStKey(), t.getStValue());
		}
	}

	public String getTag(String key){
	for (Tag t:tagList){
		if (key.equals(t.getStKey())){
			return t.getStValue();
		}
	}
	return null;
	}

	public List<String> getTags(){
		ArrayList<String> tags=new ArrayList<String>();
		for (Tag t:tagList){
			tags.add(t.getStKey());
		}
		return tags;
	}
	public String getAttribute(String key){
		return getTag(key);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ("id=" + id );
	}
	
	protected EntityDescription calcType(Hashtable<String, Hashtable<String,Set<EntityDescription>>> legend){
		EntityDescription entityDes = null;

		//System.out.println("Calculating type for " + toString());
		if (legend != null) {
			List<String> tags = getTags();
			if (tags != null) {
				byte currentPrio = Byte.MIN_VALUE;
				for (String s: tags) {
					Hashtable<String,Set<EntityDescription>> keyValues = legend.get(s);
					//System.out.println("Calculating type for " + toString() + " " + s + " " + keyValues);
					if (keyValues != null) {
						//System.out.println("found key index for " + s);
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
											//System.out.println("Testing specialisation " + ct + " on " + this);
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
											if (failedSpec) 
												failedSpecialisations = true;
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
