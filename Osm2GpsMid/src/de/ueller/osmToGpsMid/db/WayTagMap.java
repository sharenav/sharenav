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
public class WayTagMap implements Serializable{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	int id;
	@ManyToOne
	private Way way;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Way getWay() {
		return way;
	}
	public void setWay(Way way) {
		this.way = way;
	}
}
