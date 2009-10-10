/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author hmueller
 *
 */
public class SubPath {
	private List<Node> nodeList = new ArrayList<Node>();

	public SubPath() {
		super();
	}

	/**
	 * @param newNodeList
	 */
	public SubPath(ArrayList<Node> newNodeList) {
		nodeList = newNodeList;
	}

	/**
	 * @param n
	 */
	public void add(Node n) {
		if (nodeList == null) {
			nodeList = new ArrayList<Node>();
		}
		nodeList.add(n);
	}

	/**
	 * Replaces node1 with node2 in this subpath.
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
	 * This method applies this list to this subpath.
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
	
	public List<Node> getNodes() {
		return nodeList;
	}

	public boolean contains(Object o) {
		return nodeList.contains(o);
	}

	public Node get(int index) {
		return nodeList.get(index);
	}

	public int size() {
		return nodeList.size();
	}

	/**
	 * 
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
	 * 
	 */
	public SubPath split() {
//		System.out.println("split SubPath has " + getLineCount()+ " lines");
		if (nodeList == null || getLineCount() < 2) {
			return null;
		}
		int splitP = nodeList.size() / 2;
		ArrayList<Node> newNodeList = new ArrayList<Node>();
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
		SubPath newSubPath = new SubPath(newNodeList);
//		System.out.println("old SubPath has " + getLineCount()+ " lines");
//		System.out.println("new SubPath has " + newSubPath.getLineCount()+ " lines");
		return newSubPath;
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
