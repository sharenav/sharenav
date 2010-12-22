/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

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
