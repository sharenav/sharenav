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
public class MapName implements Comparable<MapName>{
	private String name;
	private String is_in;
	private short is_in_idx;
	private byte type;
	
	public MapName(String name, String is_in) {
		super();
		this.name = name;
		this.is_in = is_in;
	}
	public String getIs_in() {
		return is_in;
	}
	public String getIsInNN() {
		if (is_in != null)
			return is_in;
		else return "";
	}
	public void setIs_in(String is_in) {
		this.is_in = is_in;
	}
	public short getIs_in_idx() {
		return is_in_idx;
	}
	public void setIs_in_idx(short is_in_idx) {
		this.is_in_idx = is_in_idx;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(MapName o) {
		int i = name.compareToIgnoreCase(o.getName());
		if (i==0 ){
			return (getIsInNN().compareToIgnoreCase(o.getIsInNN()));
		} else 
			return i;
	}
	public byte getType() {
		return type;
	}
	public void setType(byte type) {
		this.type = type;
	}
	
	
}
