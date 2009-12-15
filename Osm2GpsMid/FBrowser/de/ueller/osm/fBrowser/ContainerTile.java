package de.ueller.osm.fBrowser;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.awt.Graphics;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.swing.tree.TreeNode;

import org.openstreetmap.gui.jmapviewer.JMapViewer;





public class ContainerTile extends Tile {
	//#debug error


	Tile t1;
	Tile t2;
	private final int	deep;
	private final String	root;
    
    ContainerTile(DataInputStream dis,int deep,byte zl,String root) throws IOException{
    	this.deep = deep;
		this.root = root;
		minLat=dis.readFloat();
    	minLon=dis.readFloat();
    	maxLat=dis.readFloat();
    	maxLon=dis.readFloat();
    	t1=readTile(dis,deep+1,zl);
       	t2=readTile(dis,deep+1,zl);

    }
    
    public Tile readTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	byte t=dis.readByte();
    	switch (t) {
    	case Tile.TYPE_MAP:
    		return new SingleTile(dis,deep,zl,root);
    	case Tile.TYPE_CONTAINER:
    		return new ContainerTile(dis,deep,zl,root);
    	case Tile.TYPE_EMPTY:
    		return new Empty();
    	case Tile.TYPE_FILETILE:
    		return new FileTile(dis,deep,zl,root);
//    	case Tile.TYPE_ROUTEFILE:
//    		return new RouteFileTile(dis,deep,zl);
    	default:
    	throw new IOException("wrong TileType");
    	}
    }

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildCount()
	 */
	@Override
	public int getChildCount() {
		return 2;
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	@Override
	public TreeNode getChildAt(int childIndex) {
		switch (childIndex){
			case 0: return t1;
			case 1: return t2; 
			default: return null;
		}
	}

	/* (non-Javadoc)
	 * @see de.ueller.osm.fBrowser.Tile#toString()
	 */
	@Override
	public String toString() {
		return deep+ " Container" + super.toString();
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea#getLat()
	 */
	@Override
	public double getLat() {
		return (minLat+maxLat)/2*f;
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea#getLon()
	 */
	@Override
	public double getLon() {
		return (minLon+maxLon)/2*f;
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea#paint(java.awt.Graphics, org.openstreetmap.gui.jmapviewer.JMapViewer)
	 */
	@Override
	public void paint(Graphics g, JMapViewer map) {
		// TODO Auto-generated method stub
		
	}

}
