/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author hmueller
 *
 */
public class Path {
	private LinkedList<SubPath> subPathList;
	private SubPath currentSeg=null;
	
	public Path(){
		super();
	}
	
	
	public Path(LinkedList<SubPath> subPathList) {
		super();
		this.subPathList = subPathList;
	}

	public void add(Node n){
		if (subPathList == null){
			subPathList = new LinkedList<SubPath>();
		}
		if (currentSeg==null){
			currentSeg = new SubPath();
			subPathList.add(currentSeg);
		}
		currentSeg.add(n);
	}
	
	protected void addNewSegment(){
		if (subPathList == null){
			subPathList = new LinkedList<SubPath>();
		}
		if (currentSeg != null && currentSeg.getLineCount() >0){
			currentSeg=null;
		}
	}
	
	public boolean isMultiPath(){
		if (subPathList != null){
			if (subPathList.size()==1){
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	public int getPathCount(){
		if (subPathList != null){
			return subPathList.size();
		} else {
			return 0;
		}	
	}

	/**
	 * @param no
	 * @param n
	 */
	public void replace(Node no, Node n) {
		for (SubPath s:subPathList){
			s.replace(no,n);
		}
	}
	public void replace(HashMap<Node,Node> replaceNodes) {
		for (SubPath s:subPathList){
			s.replace(replaceNodes);
		}
	}
	

	/**
	 * @return
	 */
	public LinkedList<SubPath> getSubPaths() {
		return subPathList;
	}

	/**
	 * 
	 */
	public int getLineCount() {
		int count=0;
		for (SubPath s:subPathList){
			count += s.getLineCount();
		}
		return count;
	}
	
	/**
	 * split this Path at the half subPath elements
	 * @return null if the Path already have one Subpath, 
	 *         a new Path with the rest after split.  
	 */
	public Path split(){
		if (isMultiPath()){
//			System.out.println("split pathlist");
			int splitp=subPathList.size()/2;
			int a=0;
			LinkedList<SubPath> newSubPathList = new LinkedList<SubPath>();
			for (Iterator<SubPath> si=subPathList.iterator();si.hasNext();){
				SubPath t=si.next();
				if (a >= splitp){
					newSubPathList.add(t);
					si.remove();
				}
				a++;
			}
			Path newPath=new Path(newSubPathList);
			return newPath;
		} else {
			
			SubPath newPath= subPathList.getFirst().split();
			if (newPath != null){
				LinkedList<SubPath> newSubPathList = new LinkedList<SubPath>();
				newSubPathList.add(newPath);
				return new Path(newSubPathList);
			}
		}
		return null;
	}
	
	public void clean(){
		for (Iterator<SubPath> si=subPathList.iterator();si.hasNext();){
			SubPath t=si.next();
			if (t.getLineCount() == 0){
				si.remove();
			}
		}
	}


	/**
	 * @param bound
	 */
	public void extendBounds(Bounds bound) {
		for (SubPath s:subPathList){
			s.extendBounds(bound);
		}		
	}


	/**
	 * @return
	 */
	public SubPath getActualSeg() {
		return currentSeg;
	}
}
