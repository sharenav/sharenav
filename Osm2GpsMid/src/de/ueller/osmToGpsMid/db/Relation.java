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
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.OneToMany;
import javax.persistence.Table;



/**
 * @author hmueller
 *
 */
@javax.persistence.Entity
@Table(name = "REL")
@DiscriminatorValue(value="R")
public class Relation extends Entity implements Serializable {
	@OneToMany(mappedBy = "relation",cascade={CascadeType.ALL})
	private final List<Member> members=new ArrayList<Member>();
	private boolean partialMembers = false; //Set if not all members of the realtion are available
	
	/**
	 * 
	 */
	public Relation() {
		// TODO Auto-generated constructor stub
	}
	
	public Relation(long id) {
		this.id = id;
	}
	/**
	 * @return the members
	 */
	public List<Member> getMembers() {
		return members;
	}

	/**
	 * @param m
	 */
	public void add(Member m) {
		m.setRelation(this);
		getMembers().add(m);		
	}
	
	public String toUrl() {
		return "http://www.openstreetmap.org/browse/relation/" + id;
	}
	
	public String toString(){
		StringBuffer ret=new StringBuffer("Relation (" + id + ") [");
		for (String key : getTags()) {
			ret.append(key + "=" + getAttribute(key) + " ");
		}
		for (Member m:getMembers()){
			ret.append(m.toString());
			ret.append(", ");
		}
		ret.setLength(ret.length()-2);
		ret.append("]");
		return ret.toString();
	}
	
	public boolean isValid() {
		if (getMembers().size() > 0) {
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
					for (Member m : getMembers()) {
						switch (m.getRole()) {
						case Member.ROLE_FROM: {
							if (m.getType() == Member.TYPE_WAY) {
								role_from++;
							}
							break;
						}
						case Member.ROLE_VIA: {
							role_via++;
							break;
						}
						case Member.ROLE_TO: {
							if (m.getType() == Member.TYPE_WAY) {
								role_to++;
							}
							break;
						}
						}						 
					}
					if (role_from == 0) {System.out.println(toUrl() + ": Missing from in restriction " + toString()); return false;}
					if (role_via == 0) {System.out.println(toUrl() + ": Missing via in restriction " + toString());return false;}
					if (role_to == 0) {System.out.println(toUrl() + ": Missing to in restriction " + toString());return false;}
					if (role_from > 1) {System.out.println(toUrl() + ": Too many \"from\" in restriction " + toString());return false;}
					if (role_via > 1) {System.out.println(toUrl() + ": Too many \"via\" in restriction " + toString());return false;}
					if (role_to > 1) {System.out.println(toUrl() + ": Too many \"to\" in restriction " + toString());return false;}

					String restrictionType = getAttribute("restriction");
					if (restrictionType == null) {
						System.out.println(toUrl() + ": No restriction type given in restriction " + toString());return false;						
					}
					restrictionType.toLowerCase();					
					if (! (restrictionType.startsWith("only_") || restrictionType.startsWith("no_"))) {
						System.out.println(toUrl() + ": Neither a no_ nor an only_ restriction " + toString());return false;
					}

				}
				return !isPartial();
			} else {
				return !isPartial();
			}			
		} else {
			return false;
		}
	}

	
	public long getViaNodeOrWayRef() {
		long ref = 0;
		if (getMembers().size() > 0) {
			String type = getAttribute("type");
			if (type != null) {
				if (type.equalsIgnoreCase("restriction")) {
					for (Member m : getMembers()) {
						if (m.getRole() == Member.ROLE_VIA) {
							if (m.getType() == Member.TYPE_NODE || m.getType() == Member.TYPE_WAY) {
								ref = m.getRef();
							} else {
								System.out.println(toUrl() + ": Turn restrictions: Can only handle ROLE_VIA node and way types. Restriction: " + toString());
							}
						}
					}
				}
			}	
		}
		return ref;
	}
	
	public boolean isViaWay() {
		if (getMembers().size() > 0) {
			for (Member m : getMembers()) {
				if (m.getRole() == Member.ROLE_VIA) {
					if (m.getType() == Member.TYPE_WAY) {
						return true;
					}
				}
			}	
		}
		return false;
	}
	
	public void setPartial() {
		partialMembers = true;
	}

	public boolean isPartial() {
		return partialMembers;
	}
}
