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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;



/**
 * @author hmueller
 * 
 * Attention nodes have to be there in a defined order
 *
 */
@javax.persistence.Entity
@Table(name = "WAY")
@DiscriminatorValue(value="W")
public class Way extends Entity implements Serializable {

	private byte wayTravelModes = 0;

	@OneToMany(mappedBy = "way",cascade={CascadeType.ALL})
	private final List<WayNode> nodeList = new ArrayList<WayNode>();

	
	public Way() {}
	/**
	 * @param id2
	 */
	public Way(long id) {
		id=id;
	}

	public byte getWayTravelModes() {
		return wayTravelModes;
	}
	public void setWayTravelModes(byte wayTravelModes) {
		this.wayTravelModes = wayTravelModes;
	}

	
	public List<WayNode> getNodeList() {
		return nodeList;
	}
	
	
	/**
	 * @param node
	 */
	public void add(Node node) {
		nodeList.add(new WayNode(node,this));
	}
	
	public String getName(){
		return getAttribute("name");
	}
	
	@Override
	public String toString() {
		return "Way" + super.toString() + " name=" + getName();
	}

	
}
