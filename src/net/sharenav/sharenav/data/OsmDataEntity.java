/**
 * This file is part of ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009  Kai Krueger
 */

//#if polish.api.osm-editing
package net.sharenav.sharenav.data;

import java.util.Hashtable;

import net.sharenav.util.Logger;

public abstract class OsmDataEntity {
	private final static Logger logger = Logger.getInstance(
			OsmDataEntity.class, Logger.DEBUG);
	
	protected String fullXML;
	protected int osmID;
	protected int version;
	protected int changesetID;
	
	protected String editTime;
	protected String editBy;
	protected Hashtable tags;
	
	protected boolean ignoring;

	public OsmDataEntity(int osmID) {
		fullXML = null;
		this.osmID = osmID;
		this.tags = new Hashtable();
	}
	public OsmDataEntity(String fullXML, int osmID) {
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