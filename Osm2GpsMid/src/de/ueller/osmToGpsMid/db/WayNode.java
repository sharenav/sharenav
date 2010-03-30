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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author hmueller
 *
 */
@javax.persistence.Entity
public class WayNode implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	int id;
	@ManyToOne
    @JoinColumn(name="NodeId", nullable=false)
    private Node node;
	@ManyToOne
    @JoinColumn(name="WayId", nullable=false)
	private Way way;
	
	public WayNode(){
		
	}
	/**
	 * @param node2
	 */
	public WayNode(Node node2,Way way2) {
		node=node2;
		way=way2;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}
	
	public Way getWay() {
		return way;
	}
	public void setWay(Way way) {
		this.way = way;
	}

}
