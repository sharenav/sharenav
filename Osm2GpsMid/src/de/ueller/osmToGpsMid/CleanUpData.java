/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007        Harald Mueller
 * Copyright (C) 2007, 2008  Kai Krueger
 */
package de.ueller.osmToGpsMid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import de.ueller.osmToGpsMid.model.Node;
import de.ueller.osmToGpsMid.model.SubPath;
import de.ueller.osmToGpsMid.model.Way;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * @author hmueller
 *
 */
public class CleanUpData {

	private final OxParser parser;
	private final Configuration conf;
	
	private HashMap<Node,Node> replaceNodes = new HashMap<Node,Node>(); 

	public CleanUpData(OxParser parser, Configuration conf) {
		this.parser = parser;
		this.conf = conf;
		removeDupNodes();
		removeUnusedNodes();
		parser.resize();
		System.out.println("Remaining after cleanup:");
		System.out.println("  Nodes: " + parser.getNodes().size());
		System.out.println("  Ways: " + parser.getWays().size());
		System.out.println("  Relations: " + parser.getRelations().size());
	}
	
	/**
	 * 
	 */
	private void removeDupNodes() {
		int progressCounter = 0;
		int duplicates = 0;
		int noNodes = parser.getNodes().size() / 20;
		KDTree kd = new KDTree(2);
		double [] latlonKey = new double[2];
		
		System.out.println("PLEASE HELP and fix reported duplicates in OpenStreetMap");

		long startTime = System.currentTimeMillis();
		
		for (Node n:parser.getNodes()) {
			
			progressCounter++;
			if (noNodes > 0 && progressCounter % noNodes == 0) {
				System.out.println("Processed " + progressCounter + " of " 
						+ noNodes * 20 + " nodes, " + duplicates + " duplicates found");
			}
			
			n.used = true;
			//latlonKey = MyMath.latlon2XYZ(n);
			latlonKey[0] = n.lat;
			latlonKey[1] = n.lon;
			
			try {
				kd.insert(latlonKey, n);					
			} catch (KeySizeException e) {				
				e.printStackTrace();
			}  catch (KeyDuplicateException e) {
				duplicates++;
				try {
					n.used = false;
					Node rn = (Node)kd.search(latlonKey);
					if (n.getType(conf) != rn.getType(conf)) {
						System.out.println("Differing duplicate nodes: " + n + " / " + rn);
						System.out.println("  Detail URL: " + n.toUrl());
						// Shouldn't substitute in this case;
						n.used = true;
					} else {
						replaceNodes.put(n, rn);
					}
				} catch (KeySizeException e1) {
					e1.printStackTrace();
				}
			}			
		}
		
		Iterator<Node> it = parser.getNodes().iterator();
		int rm = 0;
		while (it.hasNext()) {
			Node n = it.next();
			if (n.used == false) {
				it.remove();
				rm++;
			}
		}
		substitute();
		long time = (System.currentTimeMillis() - startTime);
		System.out.println("Removed " + rm + " duplicate nodes, took "
				+ time + " ms");
	}

	/**
	 * Replaces all duplicate nodes in the ways which use them.
	 * Uses the replaceNodes HashMap for this.
	 */
	private boolean substitute() {		
		for (Way w:parser.getWays()) {
			w.replace(replaceNodes);
		}
		return true;
	}

	/**
	 * 
	 */
	private void removeUnusedNodes() {
		long startTime = System.currentTimeMillis();

		for (Node n:parser.getNodes()) {
			if (n.getType(conf) < 0 ) {
				n.used = false;
			} else {
				n.used = true;
			}
		}

		for (Way w:parser.getWays()){
			for (SubPath s:w.getSubPaths()) {
				for (Node n:s.getNodes()) {
					n.used = true;
				}
			}
		}
		ArrayList<Node> rmNodes = new ArrayList<Node>();
		for (Node n:parser.getNodes()) {
			if (n.used == false) {
				rmNodes.add(n);
			}
		}
	    for (Node n:rmNodes) {
	    	parser.removeNode(n.id);
	    }
	    long time = (System.currentTimeMillis() - startTime);
		System.out.println("Removed " + rmNodes.size() + " unused nodes, took " 
				+ time + " ms");
	}
}
