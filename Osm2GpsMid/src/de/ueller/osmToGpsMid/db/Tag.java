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


import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author hmueller
 *
 */
@javax.persistence.Entity
public class Tag implements Serializable{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	int id;
	private String stKey;
	private String stValue;
	private Entity parent;
	
	public Object getParent() {
		return parent;
	}

	public void setParent(Entity parent) {
		this.parent = parent;
	}

	/**
	 * 
	 */
	public Tag() {
		
	}
	
	public Tag(String key, String value,Entity e) {
		stKey=key;
		stValue=value;
		parent=e;
	}
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getStKey() {
		return stKey;
	}
	public void setStKey(String stKey) {
		this.stKey = stKey;
	}
	public String getStValue() {
		return stValue;
	}
	public void setStValue(String stValue) {
		this.stValue = stValue;
	}


}
