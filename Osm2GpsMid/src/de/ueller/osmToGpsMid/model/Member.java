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

public class Member {

	public final byte TYPE_UNKOWN=0;
	public final byte TYPE_WAY=1;
	public final byte TYPE_NODE=2;
	public final byte ROLE_UNKOWN=0;
	public final byte ROLE_EMPTY=1;
	
	private byte type;
	private long ref;
	private byte role;
	
	public Member(String type,String ref, String role){
		setType(type);
		setRef(ref);
		setRole(role);
	}

	public byte getType() {
		return type;
	}
	public String getTypeName() {
		switch (type) {
		case TYPE_UNKOWN: return "unknown";
		case TYPE_WAY: return "way";
		case TYPE_NODE: return "node";
		}
		return "undef";
	}

	public void setType(byte type) {
		this.type = type;
	}
	public void setType(String type) {
		if ("way".equals(type)){
			this.type = TYPE_WAY;
		} else if ("node".equals(type)){
			this.type = TYPE_NODE;
		} else {
		    this.type = TYPE_UNKOWN;
		}
	}

	public long getRef() {
		return ref;
	}

	public void setRef(long ref) {
		this.ref = ref;
	}
	public void setRef(String ref) {
		this.ref = Long.parseLong(ref);
	}

	public byte getRole() {
		return role;
	}
	public String getRoleName() {
		switch (role) {
		case ROLE_UNKOWN: return "unknown";
		case ROLE_EMPTY: return "''";
		}
		return "undef";
	}

	public void setRole(byte role) {
		this.role = role;
	}
	public void setRole(String role) {
		if ("".equals(role)){
			this.role=ROLE_EMPTY;
		} else {
			this.role = ROLE_UNKOWN;
		}
	}
	
	public String toString(){
		return "member " + getTypeName() + "(" + ref + ") as " + getRoleName();
	}
}
