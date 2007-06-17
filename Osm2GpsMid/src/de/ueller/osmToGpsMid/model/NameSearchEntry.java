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

/**
 * @author hmueller
 *
 */
public class NameSearchEntry {
	public String name;
	public ArrayList<Short> idx=new ArrayList<Short>();
	public ArrayList<Byte> type=new ArrayList<Byte>();
	
	public String toString(){
		StringBuffer ret=new StringBuffer();
		ret.append(name);
		ret.append('[');
		for (Short i: idx ){
			ret.append(i);
			ret.append(',');
		}
		ret.setLength(ret.length()-1);
		ret.append(']');
		return ret.toString();
	}
	
}
