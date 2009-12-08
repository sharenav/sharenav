/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.osm.fBrowser;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;



public abstract class Tile implements TreeNode {
	public static final byte TYPE_MAP = 1;
	public static final byte TYPE_CONTAINER = 2;
	public static final byte TYPE_FILETILE = 4;
	public static final byte TYPE_EMPTY = 3;
	public static final byte TYPE_ROUTEDATA = 5;
	public static final byte TYPE_ROUTECONTAINER = 6;
	public static final byte TYPE_ROUTEFILE = 7;
	public static final byte TYPE_WAYPOINT = 8;

	public static final int OPT_WAIT_FOR_LOAD = 1;
	public static final int OPT_PAINT = 2;
	public static final int OPT_FIND_TARGET = 4;
	public static final int OPT_FIND_CURRENT= 8;
	public static final int OPT_CONNECTIONS2WAY= 16;
	public static final int OPT_CONNECTIONS2AREA= 32;
	public static final int OPT_HIGHLIGHT = 64;
	
	public static final byte LAYER_AREA = (byte)0x80; //10000000 binary
	public static final byte LAYER_ALL = (byte)0x40; //01000000 binary
	public static final byte LAYER_HIGHLIGHT = (byte)0x20; //00100000 binary
	public static final byte LAYER_NODE = Byte.MAX_VALUE;
	
	

	public float minLat;
	public float maxLat;
	public float minLon;
	public float maxLon;
	public float centerLat;
	public float centerLon;
	
	public short fileId=0;
	public byte	lastUse	= 0;

	public String toString(){
		return " " + minLat+","+minLon+"/"+ maxLat+","+maxLon;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#children()
	 */
	@Override
	public Enumeration children() {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getAllowsChildren()
	 */
	@Override
	public boolean getAllowsChildren() {
		// TODO Auto-generated method stub
		return false;
	}






	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
	 */
	@Override
	public int getIndex(TreeNode node) {
		// TODO Auto-generated method stub
		return 0;
	}


	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getParent()
	 */
	@Override
	public TreeNode getParent() {
		// TODO Auto-generated method stub
		return null;
	}


	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}



}
