/**
 * This file is part of OSM2ShareNav
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 * See COPYING.
 *
 * Copyright (C) 2007 Harald Mueller
 */

package net.sharenav.osmToShareNav.model;

import java.util.ArrayList;
import java.util.LinkedList;

import net.sharenav.osmToShareNav.Configuration;

/**
 *
 */
public class Relation extends Entity {
	protected LinkedList<Member> members = new LinkedList<Member>();
	public Long id;
	
	/** Set if not all members of the relation are available */
	private boolean partialMembers = false;
	
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
	
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer("Relation (" + id + ") [");
		for (String key : getTags()) {
			ret.append(key + "=" + getAttribute(key) + " ");
		}
		for (Member m : members) {
			ret.append(m.toString());
			ret.append(", ");
		}
		ret.setLength(ret.length() - 2);
		ret.append("]");
		return ret.toString();
	}
	
	public ArrayList<Long> getWayIds(int role) {
		ArrayList<Long> ret = new ArrayList<Long>();
		for (Member m : members) {
			if (m.getType() == Member.TYPE_WAY) { 
				if (m.getRole() == role) {
					ret.add(new Long(m.getRef()));
				}
			}
		}
		return ret;
	}
	
	public ArrayList<Long> getWayIds() {
		ArrayList<Long> ret = new ArrayList<Long>();
		for (Member m : members) {
			if (m.getType() == Member.TYPE_WAY) { 
				ret.add(new Long(m.getRef()));
			}
		}
		return ret;
	}
	
	public ArrayList<Long> getNodeIds(int role) {
		ArrayList<Long> ret = new ArrayList<Long>();
		for (Member m : members) {
			if (m.getType() == Member.TYPE_NODE) {
				if (m.getRole() == role) {
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
					int  role_via = 0;	
					int role_from = 0;	
					int role_to = 0;
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
					if (role_from == 0) {
						System.out.println(toUrl() + ": Missing from in restriction " + toString()); 
						return false;
					}
					if (role_via == 0) {
						System.out.println(toUrl() + ": Missing via in restriction " + toString());
						return false;
					}
					if (role_to == 0) {
						System.out.println(toUrl() + ": Missing to in restriction " + toString());
						return false;
					}
					if (role_from > 1) {
						System.out.println(toUrl() + ": Too many \"from\" in restriction " + toString());
						return false;
					}
					if (role_via > 1) {
						System.out.println(toUrl() + ": Too many \"via\" in restriction " + toString());
						return false;
					}
					if (role_to > 1) {
						System.out.println(toUrl() + ": Too many \"to\" in restriction " + toString());
						return false;
					}
					String restrictionType = getAttribute("restriction");
					if (restrictionType == null) {
						System.out.println(toUrl() + ": No restriction type given in restriction " + toString());
						return false;						
					}
					restrictionType.toLowerCase();					
					if (! (restrictionType.startsWith("only_") || restrictionType.startsWith("no_"))) {
						System.out.println(toUrl() + ": Neither a no_ nor an only_ restriction " + toString());
						return false;
					}
				}
				if (Configuration.getConfiguration().useHouseNumbers && (type.equalsIgnoreCase("associatedStreet") || type.equalsIgnoreCase("street"))) {
					int streetcount = 0;
					int housecount = 0;
					boolean ok = false;
					for (Member m : members) {
						switch (m.getRole()) {
						case Member.ROLE_STREET: {
							if (m.getType() == Member.TYPE_WAY) {
								streetcount++;
								//System.out.println("Relation " + id + " way ref = " + wref);
								break;
							}
						}
						case Member.ROLE_HOUSE: {
							if (m.getType() == Member.TYPE_NODE) {
								housecount++;
								//System.out.println("Relation " + id + " house ref = " + nref);
								break;
							}
							if (m.getType() == Member.TYPE_WAY) {
								//System.out.println("Info: trying to handle way (typically building, area) with housenumber, relation url: " 
								//		   + toUrl());
								housecount++;
								break;
							}
						}
						}

					}
					if (streetcount >= 1 && housecount >= 1) {
						//System.out.println("Housenumber relation ok, street count: " 
						//		+ streetcount + " house count " + housecount + " url: " + toUrl());
						return true;
					} else {
						// Warn only about associatedStreet relations with no housenumbers,
						// but not street relations, as there's nothing suspicious
						// about a street relation with no houses
						if (type.equalsIgnoreCase("street") && housecount == 0) {
							// do nothing
						} else {
							System.out.println("Warning: ignoring map data: Housenumber relation not ok, street count: " 
									   + streetcount + " house count " + housecount + " url: " + toUrl());
							return false;
						}
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
								System.out.println(toUrl() 
										+ ": Turn restrictions: Can only handle ROLE_VIA node and way types. Restriction: " 
										+ toString());
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
