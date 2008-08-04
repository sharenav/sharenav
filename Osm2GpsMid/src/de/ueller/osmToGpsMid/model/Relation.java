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
		StringBuffer ret=new StringBuffer("Relation (" + id + ") [");
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
			if (type != null) {
				if (type.equalsIgnoreCase("route")) {				
					return true;
				}
				if (type.equalsIgnoreCase("restriction")) {					
					int  role_via = 0;	int role_from = 0;	int role_to = 0;
					for (Member m : members) {
						switch (m.getRole()) {
						case Member.ROLE_FROM: {
							role_from++;
							break;
						}
						case Member.ROLE_VIA: {
							role_via++;
							break;
						}
						case Member.ROLE_TO: {
							role_to++;
							break;
						}
						}						 
					}
					if (role_from == 0) {System.out.println("Missing from in restriction " + toString()); return false;}
					if (role_via == 0) {System.out.println("Missing via in restriction " + toString());return false;}
					if (role_to == 0) {System.out.println("Missing to in restriction " + toString());return false;}
					if (role_from > 1) {System.out.println("Too many \"from\" in restriction " + toString());return false;}
					if (role_via > 1) {System.out.println("Too many \"via\" in restriction " + toString());return false;}
					if (role_to > 1) {System.out.println("Too many \"to\" in restriction " + toString());return false;}
				}
				return !isPartial();
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
