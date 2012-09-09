/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * @version $Revision$ ($Name$)
 * @author hmueller,jkpj
 * Copyright (C) 2007-2010 Harald Mueller, Jyrki Kuoppala
 * derived from Path.java
 */
package net.sharenav.osmToShareNav.model;

import java.util.ArrayList;
import java.util.List;


public class HouseNumber {
	private List<Node> nodeList = new ArrayList<Node>(1);
	
	public HouseNumber() {
		super();
	}
	
	public HouseNumber(ArrayList<Node> newNodeList) {
		nodeList = newNodeList;
	}

	public void add(Node n) {
		nodeList.add(n);
	}
	
	
	/**
	 * 
	 */
	public int getHouseNumberCount() {
		if (nodeList == null) {
			return 0;
		}
		if (nodeList.size() >= 1) {
			return (nodeList.size());
		}
		return 0;
	}
	
	public List<Node> getNodes() {
		return nodeList;
	}
}
