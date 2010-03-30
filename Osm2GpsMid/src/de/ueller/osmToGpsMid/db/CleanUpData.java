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
package de.ueller.osmToGpsMid.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import de.ueller.osmToGpsMid.Configuration;
import de.ueller.osmToGpsMid.db.Node;

import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

/**
 * @author hmueller
 *
 */
public class CleanUpData {


	
	private HashMap<Node,Node> replaceNodes = new HashMap<Node,Node>();
	private Configuration	conf;
	private final EntityManagerFactory	entityManagerFactory; 

	public CleanUpData(EntityManagerFactory x, Configuration conf) {
		this.entityManagerFactory = x;
		this.conf = conf;
		removeDupNodes();
//		removeUnusedNodes();
		System.out.println("Remaining after cleanup:");
//		System.out.println("  Nodes: " + parser.getNodes().size());
//		System.out.println("  Ways: " + parser.getWays().size());
//		System.out.println("  Relations: " + parser.getRelations().size());
	}
	
	/**
	 * 
	 */
	private void removeDupNodes() {
		EntityManager em = entityManagerFactory.createEntityManager();
		int progressCounter = 0;
		int duplicates = 0;
//		int noNodes = parser.getNodes().size() / 20;
		KDTree kd = new KDTree(2);
		double [] latlonKey = new double[2];
		
		System.out.println("PLEASE HELP and fix reported duplicates in OpenStreetMap");

		long startTime = System.currentTimeMillis();
		Query q = em.createQuery("SELECT n FROM Node n");
		List<Node> list = q.getResultList();
		for (Node n:list) {
			
			progressCounter++;
//			if (noNodes > 0 && progressCounter % noNodes == 0) {
//				System.out.println("Processed " + progressCounter + " of " 
//						+ noNodes * 20 + " nodes, " + duplicates + " duplicates found");
//			}
			
			n.setUsed(true);
//			latlonKey = MyMath.latlon2XYZ(n);
			latlonKey[0] = n.getLat();
			latlonKey[1] = n.getLon();
			
			try {
				kd.insert(latlonKey, n);					
			} catch (KeySizeException e) {				
				e.printStackTrace();
			}  catch (KeyDuplicateException e) {
				duplicates++;
				try {
					n.setUsed(false);
					Node rn = (Node)kd.search(latlonKey);
					if (n.getType() != rn.getType()) {
						System.out.println("Differing duplicate nodes: " + n + " / " + rn);
						System.out.println("  Detail URL: " + n.toUrl());
						// Shouldn't substitute in this case;
						n.setUsed(true);
					} else {
						replaceNodes.put(n, rn);
					}
				} catch (KeySizeException e1) {
					e1.printStackTrace();
				}
			}			
		}
		
//		Iterator<Node> it = parser.getNodes().iterator();
//		int rm = 0;
//		while (it.hasNext()) {
//			Node n = it.next();
//			if (n.used == false) {
//				it.remove();
//				rm++;
//			}
//		}
//		substitute();
//		long time = (System.currentTimeMillis() - startTime);
//		System.out.println("Removed " + rm + " duplicate nodes, took "
//				+ time + " ms");
	}
//
//	/**
//	 * Replaces all duplicate nodes in the ways which use them.
//	 * Uses the replaceNodes HashMap for this.
//	 */
//	private boolean substitute() {		
//		for (Way w:parser.getWays()) {
//			w.replace(replaceNodes);
//		}
//		return true;
//	}
//
//	/**
//	 * 
//	 */
//	private void removeUnusedNodes() {
//		long startTime = System.currentTimeMillis();
//
//		for (Node n:parser.getNodes()) {
//			if (n.getType(conf) < 0 ) {
//				n.used = false;
//			} else {
//				n.used = true;
//			}
//		}
//
//		for (Way w:parser.getWays()){
//			for (SubPath s:w.getSubPaths()) {
//				for (Node n:s.getNodes()) {
//					n.used = true;
//				}
//			}
//		}
//		ArrayList<Node> rmNodes = new ArrayList<Node>();
//		for (Node n:parser.getNodes()) {
//			if (n.used == false) {
//				rmNodes.add(n);
//			}
//		}
//	    for (Node n:rmNodes) {
//	    	parser.removeNode(n.id);
//	    }
//	    long time = (System.currentTimeMillis() - startTime);
//		System.out.println("Removed " + rmNodes.size() + " unused nodes, took " 
//				+ time + " ms");
//	}
}
