/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.LinkedList;

/**
 * @author hmueller
 *
 */
public class Relation extends Entity {
	private LinkedList<Member> members=new LinkedList<Member>();
	private boolean partialMembers = false; //Set if not all members of the realtion are available
	
	public Relation(long id) {
		this.id = id;
	}
	/**
	 * @param m
	 */
	public void add(Member m) {
		members.add(m);		
	}
	public String toString(){
		StringBuffer ret=new StringBuffer("Relation [");
		for (String key : getTags()) {
			ret.append(key + "=" + getAttribute(key) + " ");
		}
		for (Member m:members){
			ret.append(m.toString());
			ret.append(", ");
		}
		ret.setLength(ret.length()-2);
		ret.append("]");
		return ret.toString();
	}
	
	public boolean isValid() {
		if (members.size() > 0) {
			//TODO: A more elaborate check is needed
			//based on the type of relation and the
			//partial members attribute is needed
			String type = getAttribute("type");
			if ((type != null) && (type.equalsIgnoreCase("route"))) {				
				return true;
			} else {
				return !isPartial();
			}			
		} else {
			return false;
		}
	}

	public void setPartial() {
		partialMembers = true;
	}

	public boolean isPartial() {
		return partialMembers;
	}
}
