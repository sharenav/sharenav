/**
 * This file is part of GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */

//#if polish.api.osm-editing
package de.ueller.midlet.gps.data;

import java.util.Hashtable;

import de.ueller.midlet.gps.Logger;

public abstract class OSMdataEntity {
	private final static Logger logger = Logger.getInstance(
			OSMdataEntity.class, Logger.DEBUG);
	
	protected String fullXML;
	protected int osmID;
	protected int version;
	protected int changesetID;
	
	protected String editTime;
	protected String editBy;
	protected Hashtable tags;
	
	protected boolean ignoring;

	public OSMdataEntity(int osmID) {
		fullXML = null;
		this.osmID = osmID;
		this.tags = new Hashtable();
	}
	public OSMdataEntity(String fullXML, int osmID) {
		this.fullXML = fullXML;
		this.osmID = osmID;
		this.tags = new Hashtable();
		parseXML();
	}
	
	protected abstract void parseXML();
	
	public String getXML() {
		return fullXML;
	}
	
	public Hashtable getTags() {
		return tags;
	}
	
	public int getVersion() {
		return version;
	}
	
	public String getEditor() {
		return editBy;
	}
	
	public String getEditTime() {
		return editTime;
	}
	
	public abstract String toXML(int commitChangesetID);

	public abstract String toDeleteXML(int commitChangesetID);
}
//#endif