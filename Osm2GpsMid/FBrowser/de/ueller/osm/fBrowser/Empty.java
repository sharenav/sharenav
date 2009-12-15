/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osm.fBrowser;

import java.awt.Graphics;

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
	 * @see de.ueller.osm.fBrowser.Tile#toString()
	 */
	@Override
	public String toString() {
		return "Empty";
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea#getLat()
	 */
	@Override
	public double getLat() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea#getLon()
	 */
	@Override
	public double getLon() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea#paint(java.awt.Graphics, org.openstreetmap.gui.jmapviewer.JMapViewer)
	 */
	@Override
	public void paint(Graphics g, JMapViewer map) {
		// TODO Auto-generated method stub
		
	}

}
