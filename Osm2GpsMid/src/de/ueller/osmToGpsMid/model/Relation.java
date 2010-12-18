/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.ArrayList;
import java.util.LinkedList;

import de.ueller.osmToGpsMid.Configuration;

/**
 * @author hmueller
 *
 */
public class Relation extends Entity {
	protected LinkedList<Member> members=new LinkedList<Member>();
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
	
	public String toUrl() {
		return "http://www.openstreetmap.org/browse/relation/" + id;
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
	
	public ArrayList<Long> getWayIds(int role){
		ArrayList<Long> ret=new ArrayList<Long>();
		for (Member m:members){
			if (m.getType() == Member.TYPE_WAY){
				if (m.getRole()==role){
					ret.add(new Long(m.getRef()));
				}
			}
		}
		return ret;
	}
	
	public ArrayList<Long> getNodeIds(int role){
		ArrayList<Long> ret=new ArrayList<Long>();
		for (Member m:members){
			if (m.getType() == Member.TYPE_NODE){
				if (m.getRole()==role){
					ret.add(new Long(m.getRef()));
				}
			}
		}
		return ret;
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
				if (Configuration.getConfiguration().useHouseNumbers && (type.equalsIgnoreCase("associatedStreet") || type.equalsIgnoreCase("street"))) {
					int waycount = 0;
					int nodecount = 0;
					boolean ok = false;
					for (Member m : members) {
						switch (m.getRole()) {
						case Member.ROLE_STREET: {
							if (m.getType() == Member.TYPE_WAY) {
								waycount++;
								//System.out.println("Relation " + id + " way ref = " + wref);
								break;
							}
						}
						case Member.ROLE_HOUSE: {
							// FIXME handle ways (buildings) with areapois or something
							if (m.getType() == Member.TYPE_NODE) {
								nodecount++;
								//System.out.println("Relation " + id + " node ref = " + nref);
								break;
							}
							if (m.getType() == Member.TYPE_WAY) {
								// FIXME handle buildings too
								System.out.println("warning: ignoring map data: Unable to handle area (typically building) with housenumber, relation url: " + toUrl());
								// nodecount++;
								break;
							}
						}
						}

					}
					if (waycount == 1 && nodecount >= 1) {
						//System.out.println("Housenumber relation ok, way count: " + waycount + " node count " + nodecount + " url: " + toUrl());
						return true;
					} else {
						System.out.println("warning: ignoring map data: Housenumber relation not ok, way count: " + waycount + " node count " + nodecount + " url: " + toUrl());
						return false;
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
		if (members.size() > 0) {
			String type = getAttribute("type");
			if (type != null) {
				if (type.equalsIgnoreCase("restriction")) {
					for (Member m : members) {
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
		if (members.size() > 0) {
			for (Member m : members) {
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
