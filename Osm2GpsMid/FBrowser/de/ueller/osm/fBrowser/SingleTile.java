/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net
 * See Copying
 */
package de.ueller.osm.fBrowser;

import java.awt.Graphics;
import java.awt.Point;
import java.io.DataInputStream;

import java.io.FileInputStream;

import java.io.IOException;


import javax.swing.tree.TreeNode;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

import de.ueller.osmToGpsMid.Constants;




public class SingleTile extends Tile{

	public static final byte STATE_NOTLOAD = 0;

	public static final byte STATE_LOADSTARTED = 1;

	public static final byte STATE_LOADREADY = 2;

	private static final byte STATE_CLEANUP = 3;

    
	// Node[] nodes;
	public short[] nodeLat;

	public short[] nodeLon;

	public int[] nameIdx;

	public byte[] type;

//	Way[][] ways;

//	byte state = 0;
//	
//	boolean abortPainting = false;


	public final byte zl;

	private final String	root;
	
	SingleTile(DataInputStream dis, int deep, byte zl,String root) throws IOException {
//		 logger.debug("load " + deep + ":ST Nr=" + fileId);
		this.zl = zl;
		this.root = root;
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
		fileId = (short) dis.readInt();
		readContent();
//		 logger.debug("ready " + deep + ":ST Nr=" + fileId);
	}

	private void readContent() throws IOException{
		String fn = root+"/t" + zl + fileId + ".d";
		System.out.println("read " + fn);
		FileInputStream f=new FileInputStream(fn);
		DataInputStream ds = new DataInputStream(f);
		if (ds.readByte()!=0x54) {
			throw new IOException("not a MapMid-file");
		}
		centerLat = ds.readFloat();
		centerLon = ds.readFloat();
		int nodeCount=ds.readShort();
		short[] radlat = new short[nodeCount];
		short[] radlon = new short[nodeCount];
		int iNodeCount=ds.readShort();
		System.out.println("nodes total: " + nodeCount + " interestNode: " + iNodeCount);
		int[] nameIdx=new int[iNodeCount];
		for (int i = 0; i < iNodeCount; i++) {
			nameIdx[i] = -1;
		}
		byte[] type = new byte[iNodeCount];
		for (int i=0; i< nodeCount;i++){
//			System.out.println("read coord :"+i+"("+nodeCount+")"+"("+iNodeCount+")");
			
			byte flag = ds.readByte();
							
			radlat[i] = ds.readShort();
			radlon[i] = ds.readShort();
			if (i < iNodeCount){
			if ((flag & Constants.NODE_MASK_NAME) > 0){
				if ((flag & Constants.NODE_MASK_NAMEHIGH) > 0) {
					nameIdx[i]=ds.readInt();
				} else {
					nameIdx[i]=ds.readShort();
				}
				type[i]=ds.readByte();
			} 
			}	
		}
		nodeLat=radlat;
		nodeLon=radlon;
	}

	

	public String toString() {
		return "Map " + zl + "-" + fileId;
	}


	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildCount()
	 */
	@Override
	public int getChildCount() {
		return 0;
	}
	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	@Override
	public TreeNode getChildAt(int childIndex) {
		// TODO Auto-generated method stub
		return null;
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
		System.out.println("draw "+this);
		for (int i=0;i<nodeLat.length;i++){
			
			float x = centerLat+nodeLat[i]*FIXPT_MULT_INV;
			float y = centerLon+nodeLon[i]*FIXPT_MULT_INV;
//			System.out.println("draw " + i + " "+ x + "/" + y);
			Point tl = map.getMapPosition(x*f, y*f, false);
			g.fillOval(tl.x - 2, tl.y - 2, 5, 5);
		}
	}


}
