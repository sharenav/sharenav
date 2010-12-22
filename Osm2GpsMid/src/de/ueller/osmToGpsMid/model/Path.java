/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * @version $Revision$ ($Name$)
 * @author hmueller
 * Copyright (C) 2007-2010 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class Path {
	private List<Node> nodeList = new ArrayList<Node>();
	
	public Path() {
		super();
	}
	
	public Path(ArrayList<Node> newNodeList) {
		nodeList = newNodeList;
	}

	public void add(Node n) {
		nodeList.add(n);
	}
	
	/**
	 * @param i Index of wanted node
	 * @return Node at index i or null if 
	 */
	public Node getNode(int i) {
		if (nodeList == null) {
			return null;
		} else {
			if (i >= 0 && i < nodeList.size()) {
				return nodeList.get(i);
			} else {
				return null;
			}
		}
	}
	
	public List<Node> getNodes() {
		return nodeList;
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
		int occur = nodeList.indexOf(node1);
		while (occur != -1) {
			nodeList.set(occur, node2);
			occur = nodeList.indexOf(node1);
		}
	}

	/** replaceNodes lists nodes and by which nodes they have to be replaced.
	 * This method applies this list to this path.
	 * @param replaceNodes Hashmap of pairs of nodes
	 */
	public void replace(HashMap<Node, Node> replaceNodes) {
		for (int i = 0; i < nodeList.size(); i++) {
			Node newNode = replaceNodes.get(nodeList.get(i));
			if (newNode != null) {
				nodeList.set(i, newNode);	
			}
		}		
	}

	/**
	 * @return The number of lines of this path, i.e. nodes - 1.
	 */
	public int getLineCount() {
		if (nodeList == null) {
			return 0;
		}
		if (nodeList.size() > 1) {
			return (nodeList.size() - 1);
		}
		return 0;
	}
	
	/**
	 * TODO: This returns 0 if size() == 1, is this intentional?!?
	 * @return The number of nodes of this path or 0 if there is no node list.
	 */
	public int getNodeCount() {
		if (nodeList == null) {
			return 0;
		}
		if (nodeList.size() > 1) {
			return (nodeList.size());
		}
		return 0;
	}
	
	/**
	 * split this Path at the half subPath elements
	 * @return null if the Path already have one Subpath, 
	 *         a new Path with the rest after split.  
	 */
	public Path split() {
		if (nodeList == null || getLineCount() < 2) {
			return null;
		}
		int splitP = nodeList.size() / 2;
		ArrayList<Node> newNodeList = new ArrayList<Node>(1);
		int a = 0;
		for (Iterator<Node> si = nodeList.iterator(); si.hasNext();) {
			Node t = si.next();
			if (a >= splitP) {
				newNodeList.add(t);
				if (a > splitP) {
					si.remove();
				}
			}
			a++;
		}
		Path newPath = new Path(newNodeList);
		return newPath;
	}
	
	/**
	 * @param bound
	 */
	public void extendBounds(Bounds bound) {
		for (Node n:nodeList) {
			bound.extend(n.lat, n.lon);
		}
	}
}
