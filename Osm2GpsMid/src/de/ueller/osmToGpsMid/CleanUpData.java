/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.ueller.osmToGpsMid.model.Line;
import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.SubPath;
import de.ueller.osmToGpsMid.model.Way;

/**
 * @author hmueller
 *
 */
public class CleanUpData {

	private final OxParser parser;
	private final Configuration conf;

	public CleanUpData(OxParser parser, Configuration conf) {
		this.parser = parser;
		this.conf = conf;
		removeDupNodes();
		removeUnusedNodes();
		parser.resize();
		System.out.println("after cleanup Nodes " + parser.nodes.size());
		System.out.println("after cleanup Ways  " + parser.ways.size());

//		System.exit(1);
	}
	
	/**
	 * 
	 */
	private void removeDupNodes() {
		HashMap<Integer,ArrayList<Node>> pm=new HashMap<Integer, ArrayList<Node>>();
		for (Node n:parser.nodes.values()){
			int la = hashLatLonCode(n);
			n.used=true;
			ArrayList<Node> ln;
			if (pm.containsKey(la)){
				ln=pm.get(la);
			} else {
				ln=new ArrayList<Node>();
				pm.put(la, ln);
			}
			ln.add(n);
		}
		System.out.println("Created " + pm.size() + " coord groups");
		int lonOf=1<<16;
		/**
		 * relative index to the neighbors
		 */
		int[] tiles={0,1,-1,lonOf,-lonOf,(1+lonOf),(1-lonOf),(-1+lonOf),(-1-lonOf)};
		for (Node n:parser.nodes.values()){
			if (n.used && n.getType(conf) == 0) { // means will deleted afterwards
				// create a list with all neighbors from 9 Tiles
				ArrayList<Node> candidates=new ArrayList<Node>();
				int la = hashLatLonCode(n);
				for (int i=0;i<9;i++){
					ArrayList<Node> tmplist = pm.get(la+tiles[i]);
					if (tmplist != null) {
						candidates.addAll(tmplist);
					}
				}
//				System.out.println(" Check against " + candidates.size() + " Nodes");
				for (Node no:candidates){
					if (n != no){
						if (Math.abs(no.lat-n.lat) < 0.000001f &&
							Math.abs(no.lon-n.lon) < 0.000001f	){
							no.used=false;
							substitute(no,n);
						}
					}
				}
			}
		}
		Iterator<Node> it=parser.nodes.values().iterator();
		int rm=0;
		while (it.hasNext()){
			Node n=it.next();
			if (! n.used){
				it.remove();
				rm++;
			}
		}
		System.out.println("remove " + rm + " dupLicate Nodes");
	}

	/**
	 * @param no
	 * @param n
	 */
	private void substitute(Node no, Node n) {
		if (no.getType(conf) != n.getType(conf)){
			System.err.println("Warn " + no + " / " + n);
		}
		for (Way w:parser.ways){
			w.replace(no,n);
		}
	}

	/**
	 * @param n
	 * @return
	 */
	private int hashLatLonCode(Node n) {
		int la=(short)(n.lat*1800)+(int)(((short)(n.lon*1800)))<<16;
		return la;
	}

	/**
	 * 
	 */
	private void removeUnusedNodes() {
		for (Node n:parser.nodes.values()){
			if (n.getType(conf) == 0 ){
				n.used=false;
			} else {
				n.used=true;
			}
		}

		for (Way w:parser.ways){
			for (SubPath s:w.getSubPaths()){
				for (Node n:s.getNodes()){
					n.used=true;
				}
			}
		}
		ArrayList<Node> rmNodes=new ArrayList<Node>();
		for (Node n:parser.nodes.values()){
			if (! n.used){
				rmNodes.add(n);
			}
		}
		System.out.println("remove " + rmNodes.size() + " unused Nodes");
	    for (Node n:rmNodes){
	    	parser.nodes.remove(n.id);
	    }
		
	}

	
	private boolean isIn(Collection<Line> lines,Line l1){
		for (Line l2 : lines){
			if ((l1.from == l2.from && l1.to == l2.to)
			 || (l1.from == l2.to && l1.to == l2.from)){
				return true;
			}
		}			
		return false;
	}
	

}
