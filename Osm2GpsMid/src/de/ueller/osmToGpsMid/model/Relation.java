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
	/**
	 * @param m
	 */
	public void add(Member m) {
		members.add(m);		
	}
	public String toString(){
		StringBuffer ret=new StringBuffer("Relation [");
		for (Member m:members){
			ret.append(m.toString());
			ret.append(", ");
		}
		ret.setLength(ret.length()-2);
		ret.append("]");
		return ret.toString();
	}
}
