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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hmueller
 *
 */
public class SubPath {
	private List<Node> nodeList = new ArrayList<Node>();

	public SubPath(){
		super();
	}
	/**
	 * @param newNodeList
	 */
	public SubPath(ArrayList<Node> newNodeList) {
		nodeList=newNodeList;
	}

	/**
	 * @param n
	 */
	public void add(Node n) {
		if (nodeList == null){
			nodeList = new ArrayList<Node>();
		}
		nodeList.add(n);
	}

	/**
	 * @param no
	 * @param n
	 */
	public void replace(Node no, Node n) {
		int occur=nodeList.indexOf(no);
		while (occur != -1){
			nodeList.set(occur, n);
			occur=nodeList.indexOf(no);
		}
	}
	public List<Node> getNodes(){
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
		if (nodeList==null){
			return 0;
		}
		if (nodeList.size() > 1){
			return nodeList.size()-1;
		}
		return 0;
	}

	/**
	 * 
	 */
	public SubPath split() {
//		System.out.println("split SubPath has " + getLineCount()+ " lines");
		if (nodeList == null || getLineCount() < 2){
			return null;
		}
		int splitP=nodeList.size()/2;
		ArrayList<Node> newNodeList = new ArrayList<Node>();
		int a=0;
		for (Iterator<Node> si=nodeList.iterator();si.hasNext();){
			Node t=si.next();
			if (a >= splitP){
				newNodeList.add(t);
				if (a > splitP){
					si.remove();
				}
			}
			a++;
		}
		SubPath newSubPath=new SubPath(newNodeList);
//		System.out.println("old SubPath has " + getLineCount()+ " lines");
//		System.out.println("new SubPath has " + newSubPath.getLineCount()+ " lines");
		return newSubPath;

	}
	/**
	 * @param bound
	 */
	public void extendBounds(Bounds bound) {
		for (Node n:nodeList){
			bound.extend(n.lat, n.lon);
		}
	}
}
