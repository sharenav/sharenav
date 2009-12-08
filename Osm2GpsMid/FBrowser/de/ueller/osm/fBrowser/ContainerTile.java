package de.ueller.osm.fBrowser;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.swing.tree.TreeNode;





public class ContainerTile extends Tile {
	//#debug error


	Tile t1;
	Tile t2;
	private final int	deep;
    
    ContainerTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	this.deep = deep;
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
    		return new SingleTile(dis,deep,zl);
    	case Tile.TYPE_CONTAINER:
    		return new ContainerTile(dis,deep,zl);
    	case Tile.TYPE_EMPTY:
    		return new Empty();
    	case Tile.TYPE_FILETILE:
    		return new FileTile(dis,deep,zl);
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

}
