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
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

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
		System.out.println("after cleanup Nodes " + parser.nodes.size());
		System.out.println("after cleanup Ways  " + parser.ways.size());

//		System.exit(1);
	}
	
	/**
	 * 
	 */
	private void removeDupNodes() {
		int progressCounter = 0;
		int noNodes = parser.nodes.size()/20;
		KDTree kd = new KDTree(3);
		double [] latlonKey; 
		double [] lowk = new double[3];
		double [] uppk = new double[3];		
		for (Node n:parser.nodes.values()){
			
			progressCounter++;
			if (progressCounter % noNodes == 0) {
				System.out.println("Processed " + progressCounter + " out of " + noNodes*20 + " Nodes");
			}
			
			n.used=true;			
			latlonKey = MyMath.latlon2XYZ(n);			
			
			/*lowk[0] = latlonKey[0] - 10.0;
			lowk[1] = latlonKey[1] - 10.0;
			lowk[2] = latlonKey[2] - 10.0;			
			
			uppk[0] = latlonKey[0] + 10.0;
			uppk[1] = latlonKey[1] + 10.0;
			uppk[2] = latlonKey[2] + 10.0;
						
			try {
				
				Object[] neighbours = kd.range(lowk, uppk);
				if (neighbours.length == 1) {
					n.used = false;
					if (!substitute(n, (Node)neighbours[0]))
						kd.insert(latlonKey, n);
				} else if (neighbours.length > 1) {
					n.used = false;
					if (!substitute(n,(Node)kd.nearest(latlonKey))) 
						kd.insert(latlonKey, n);
				} else {*/
			try {
					kd.insert(latlonKey, n);					
				//}				
			} catch (KeySizeException e) {				
				e.printStackTrace();
			}  catch (KeyDuplicateException e) {				
				//System.out.println("Key Duplication");				
				try {
					n.used = false;
					Node rn = (Node)kd.search(latlonKey);
					if (n.getType(conf) != rn.getType(conf)){
						System.err.println("Warn " + n + " / " + rn);
						//Shouldn't substitute in this case;
						n.used = true;						
					} else {
						replaceNodes.put(n, rn);
					}					
				} catch (KeySizeException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
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
	private boolean substitute() {		
		for (Way w:parser.ways){
			w.replace(replaceNodes);
		}
		return true;
	}

	/**
	 * 
	 */
	private void removeUnusedNodes() {
		for (Node n:parser.nodes.values()){
			if (n.getType(conf) < 0 ){
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
}
