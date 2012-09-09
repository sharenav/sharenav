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

public class Member {

	public static final byte TYPE_UNKOWN=0;
	public static final byte TYPE_WAY=1;
	public static final byte TYPE_NODE=2;
	public static final byte TYPE_RELATION=3;
	public static final byte ROLE_UNKOWN=0;
	public static final byte ROLE_EMPTY=1;
	
	public static final byte ROLE_FROM = 2;
	public static final byte ROLE_TO = 3;
	public static final byte ROLE_VIA = 4;
	public static final byte ROLE_OUTER = 5;
	public static final byte ROLE_INNER = 6;
	public static final byte ROLE_STREET = 7;
	public static final byte ROLE_HOUSE = 8;
	
	
	private byte type;
	private long ref;
	private byte role;
	
	public Member(String type,String ref, String role){
		setType(type);
		setRef(ref);
		setRole(role);
	}
	public Member(String type,long ref, String role){
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
		case TYPE_RELATION: return "relation";
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
		} else if ("relation".equals(type)){
			this.type = TYPE_RELATION;
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
		case ROLE_FROM: return "from";
		case ROLE_TO: return "to";
		case ROLE_VIA: return "via";
		case ROLE_OUTER: return "outer";
		case ROLE_INNER: return "inner";
		case ROLE_STREET: return "street";
		case ROLE_HOUSE: return "house";
		}
		return "undef";
	}

	public void setRole(byte role) {
		this.role = role;
	}
	public void setRole(String role) {
		if ("".equals(role)){
			this.role=ROLE_EMPTY;
		} else if ("from".equals(role)){ 
			this.role=ROLE_FROM;
		} else if ("to".equals(role)){ 
			this.role=ROLE_TO;
		} else if ("via".equals(role)){ 
			this.role=ROLE_VIA;
		} else if ("outer".equals(role)){ 
			this.role=ROLE_OUTER;
		} else if ("inner".equals(role)){ 
			this.role=ROLE_INNER;
		} else if ("street".equals(role)){ 
			this.role=ROLE_STREET;
		} else if ("house".equals(role) || "addr:houselink".equals(role) || "address".equals(role)){ 
			this.role=ROLE_HOUSE;
		} else {
			this.role = ROLE_UNKOWN;
		}
	}
	
	public String toString(){
		return "member " + getTypeName() + "(" + ref + ") as " + getRoleName();
	}
}
