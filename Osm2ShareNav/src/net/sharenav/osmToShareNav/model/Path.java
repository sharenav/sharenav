/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * @version $Revision$ ($Name$)
 * @author hmueller
 * Copyright (C) 2007-2010 Harald Mueller
 */
package net.sharenav.osmToShareNav.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class Path extends ArrayList<Node> {
	
	public Path() {
		super(2);
	}
	
	public Path(ArrayList<Node> newNodeList) {
		super(newNodeList);
	}

	// create a reversed way
	public Path(ArrayList<Node> nodeList, boolean reverse) {
		super(2);
		//System.out.println("size: " + nodeList.size());
		for (int i = nodeList.size()-1; i >= 0; i--) {
			//System.out.println("Adding node: " + nodeList.get(i));
			this.add(nodeList.get(i));
		}
	}

	public boolean add(Node n) {
		return super.add(n);
	}
	
	/**
	 * @param i Index of wanted node
	 * @return Node at index i or null if 
	 */
	public Node getNode(int i) {
		if (size() == 0) {
			return null;
		} else {
			if (i >= 0 && i < size()) {
				return get(i);
			} else {
				return null;
			}
		}
	}
	
	public List<Node> getNodes() {
		return this;
	}

	@Deprecated
	public boolean isMultiPath() {
//		if (subPathList != null) {
//			if (subPathList.size() == 1) {
//				return false;
//			} else {
//				return true;
//			}
//		}
		return false;
	}

	@Deprecated
	public int getPathCount() {
		return 1;
	}

	/**
	 * Replaces node1 with node2 in this path.
	 * @param node1 Node to be replaced
	 * @param node2 Node by which to replace node1.
	 */
	public void replace(Node node1, Node node2) {
		int occur = this.indexOf(node1);
		while (occur != -1) {
			this.set(occur, node2);
			occur = this.indexOf(node1);
		}
	}

	/** replaceNodes lists nodes and by which nodes they have to be replaced.
	 * This method applies this list to this path.
	 * @param replaceNodes Hashmap of pairs of nodes
	 */
	public void replace(HashMap<Node, Node> replaceNodes) {
		for (int i = 0; i < this.size(); i++) {
			Node newNode = replaceNodes.get(this.get(i));
			if (newNode != null) {
				this.set(i, newNode);
			}
		}		
	}

	/**
	 * @return The number of lines of this path, i.e. nodes - 1.
	 */
	public int getLineCount() {
		if (this.size() == 0) {
			return 0;
		}
		if (this.size() > 1) {
			return (this.size() - 1);
		}
		return 0;
	}
	
	/**
	 * TODO: This returns 0 if size() == 1, this is probably intentional but can causes trouble in some use cases
         * if it is made to be ..size() >= 1, messages like "no center for searchList for id=24429358 type=1 [ref=45 highway=motorway foot=no name=Tuusulanväylä oneway=yes maxspeed=80 bicycle=no ]"
         * will appear
	 * @return The number of nodes of this path or 0 if there is no node list.
	 */
	public int getNodeCount() {
		if (this.size() == 0) {
			return 0;
		}
		if (this.size() > 1) {
			return (this.size());
		}
		return 0;
	}
	
	/**
	 * split this Path at the half subPath elements
	 * @return null if the Path already have one Subpath, 
	 *         a new Path with the rest after split.  
	 */
	public Path split() {
		if (this.size() == 0 || getLineCount() < 2) {
			return null;
		}
		int splitP = this.size() / 2;
		ArrayList<Node> newNodeList = new ArrayList<Node>(1);
		int a = 0;
		for (Iterator<Node> si = this.iterator(); si.hasNext();) {
			Node t = si.next();
			if (a >= splitP) {
				newNodeList.add(t);
				if (a > splitP) {
					si.remove();
				}
			}
			a++;
		}
		newNodeList.trimToSize();
		Path newPath = new Path(newNodeList);
		// Shrink the list to the actual size.
		trimPath();
		return newPath;
	}

	public void trimPath()
	{
		this.trimToSize();
	}
	
	/**
	 * @param bound
	 */
	public void extendBounds(Bounds bound) {
		for (Node n:this) {
			bound.extend(n.lat, n.lon);
		}
	}
}
