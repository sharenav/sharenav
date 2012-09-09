/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osm.fBrowser;

import java.awt.Graphics;
import java.awt.Point;

import javax.swing.tree.TreeNode;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

/**
 * @author hmueller
 *
 */
public class Empty extends Tile{

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	@Override
	public TreeNode getChildAt(int arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildCount()
	 */
	@Override
	public int getChildCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see net.sharenav.osm.fBrowser.Tile#toString()
	 */
	@Override
	public String toString() {
		return "Empty";
	}

	/* (non-Javadoc)
	 * @see net.sharenav.osm.fBrowser.Tile#paint(java.awt.Graphics, java.awt.Point, java.awt.Point, int)
	 */
	@Override
	public void paint(Graphics g, Point topLeft, Point bottomRight, int deep) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle#paint(java.awt.Graphics, java.awt.Point, java.awt.Point)
	 */
	@Override
	public void paint(Graphics g, Point topLeft, Point bottomRight) {
		// TODO Auto-generated method stub
		
	}


}
