/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */
package net.sharenav.osm.fBrowser;

import java.awt.Graphics;
import java.awt.Point;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle;




public abstract class Tile implements TreeNode, MapRectangle {
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
	public static final int OPT_FIND_DEST = 4;
	public static final int OPT_FIND_CURRENT= 8;
	public static final int OPT_CONNECTIONS2WAY= 16;
	public static final int OPT_CONNECTIONS2AREA= 32;
	public static final int OPT_HIGHLIGHT = 64;
	
	public static final byte LAYER_AREA = (byte)0x80; //10000000 binary
	public static final byte LAYER_ALL = (byte)0x40; //01000000 binary
	public static final byte LAYER_HIGHLIGHT = (byte)0x20; //00100000 binary
	public static final byte LAYER_NODE = Byte.MAX_VALUE;
	public static final double PLANET_RADIUS_D = 6371000.8d;
	public static final float PLANET_RADIUS = 6371000.8f;

	/** Factor to get degrees from radians.
	 */
	public static final float radToDeg = (float) (180d / Math.PI);

	/**
	 * This constant is used as fixed point multiplier to convert
	 * latitude / longitude from radians to fixpoint representation.
	 * With this multiplier, one should get a resolution of 1m at the equator.
	 * 
	 * This constant has to be in synchrony with the value in Osm2ShareNav.
	 */
	public static final float FIXPT_MULT = PLANET_RADIUS; 

	/**
	 * 1 / FIXPT_MULT, this saves a floating point division.
	 */
	public static final float FIXPT_MULT_INV = (1.0f / FIXPT_MULT);


	public float minLat;
	public float maxLat;
	public float minLon;
	public float maxLon;
	public float centerLat;
	public float centerLon;
	
	public short fileId=0;
	public byte	lastUse	= 0;
	protected static MapFrame map;


	public String toString() {
		return " " + minLat * radToDeg + ", " + minLon * radToDeg + 
			" / " + maxLat * radToDeg + ", " + maxLon * radToDeg;
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

	@Override
	public Coordinate getBottomRight() {
		return new Coordinate(minLat, maxLon);
	}

	@Override
	public Coordinate getTopLeft() {
		return new Coordinate(maxLat, minLon);
	}

	public abstract void paint(Graphics g, Point topLeft, Point bottomRight,int deep);

	/**
	 * @param map
	 */
	public void setMap(MapFrame map) {
		this.map = map;
	}

}
