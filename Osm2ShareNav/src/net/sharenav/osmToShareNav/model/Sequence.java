/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.model;

/**
 * @author hmueller
 *
 */
public class Sequence {
	private int seq=1;
	public void inc(){
		seq++;
	}
	public int get(){
		return seq;
	}
	public String toString(){
		return "sequence="+seq;
	}
	public int next(){
		return seq++;
	}

}
