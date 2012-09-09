/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net
 * See Copying
 */
package net.sharenav.osm.fBrowser;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.DataInputStream;

import java.io.FileInputStream;

import java.io.IOException;


import javax.swing.tree.TreeNode;


import net.sharenav.osmToShareNav.Constants;




public class SingleTile extends Tile{

	public static final byte STATE_NOTLOAD = 0;

	public static final byte STATE_LOADSTARTED = 1;

	public static final byte STATE_LOADREADY = 2;

	private static final byte STATE_CLEANUP = 3;

	private BWay selected=null;
    
	public BWay getSelected() {
		return selected;
	}

	public void setSelected(BWay selected) {
		this.selected = selected;
	}

	// Node[] nodes;
	public short[] nodeLat;

	public short[] nodeLon;

	public int[] nameIdx;

	public byte[] type;
	BWay[] ways;

	int iNodeCount;

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
		String fn = root + "/t" + zl + "/" + fileId + ".d";
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
		iNodeCount=ds.readShort();
		System.out.println("nodes total: " + nodeCount + " interestNode: " + iNodeCount);
		int[] nameIdx=new int[iNodeCount];
		for (int i = 0; i < iNodeCount; i++) {
			nameIdx[i] = -1;
		}
		byte[] type = new byte[iNodeCount];
		try {
			for (int i=0; i< nodeCount;i++){
//			System.out.print("read coord :"+i+"("+nodeCount+")"+"("+iNodeCount+")");
				
				byte flag = ds.readByte();
								
				radlat[i] = ds.readShort();
				radlon[i] = ds.readShort();

				if ((flag & Constants.NODE_MASK_NAME) > 0){
//					System.out.print(" found NODE_MASK_NAME");
					if ((flag & Constants.NODE_MASK_NAMEHIGH) > 0) {
//						System.out.println(" found NODE_MASK_NAMEHIGH");
						nameIdx[i]=ds.readInt();
					} else {
						nameIdx[i]=ds.readShort();
					}
					
				} 
				if ((flag & Constants.NODE_MASK_TYPE) > 0){
					type[i]=ds.readByte();
				}
			}

			if (0x55 != ds.readByte()){
				System.out.println("No Magic code found, expect a data error");
			}
			// now we read the ways/areas
			int wayCount = ds.readShort();
			ways=new BWay[wayCount];
			for (int i = 0; i < wayCount; i++) {
				byte flags = ds.readByte();
				ways[i] = new BWay(ds, flags, i,this);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nodeLat=radlat;
		nodeLon=radlon;
	}

	public String toString() {
		return "Map " + zl + "-" + fileId + ":" +  ways.length;
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildCount()
	 */
	@Override
	public int getChildCount() {
		return ways.length;
	}

	/* (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	@Override
	public TreeNode getChildAt(int childIndex) {
		// TODO Auto-generated method stub
		return ways[childIndex];
	}

	/* (non-Javadoc)
	 * @see net.sharenav.osm.fBrowser.Tile#paint(java.awt.Graphics, java.awt.Point, java.awt.Point, int)
	 */
	@Override
	public void paint(Graphics g, Point topLeft, Point bottomRight, int deep) {
		paint(g,topLeft,bottomRight);
		
	}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle#paint(java.awt.Graphics, java.awt.Point, java.awt.Point)
	 */
	@Override
	public void paint(Graphics g, Point topLeft, Point bottomRight) {
		// Draw bounding box
		Point sw = map.getMapPosition(minLat * radToDeg, minLon * radToDeg, false);
		Point ne = map.getMapPosition(maxLat * radToDeg, maxLon * radToDeg, false);
		g.setColor(new Color(100, 00, 00, 60));
		g.drawRect(sw.x, sw.y, ne.x - sw.x, ne.y - sw.y);
		
		// Draw ways
		int[] tx=new int[3000];
		int[] ty=new int[3000];
		for (int i=0;i<ways.length;i++){
			BWay w=ways[i];
			if (selected != null && selected !=w){
				continue;
			}
			if (w.isArea()){
				g.setColor(new Color(00, 00, 00,60));
				for (int i1 = 0; i1 < w.path.length; ){
					for (int l=0;l<3;l++){
						short idx = w.path[i1++];
						Point p1 = map.getMapPosition(
								(centerLat + nodeLat[idx] * FIXPT_MULT_INV) * radToDeg, 
								(centerLon + nodeLon[idx] * FIXPT_MULT_INV) * radToDeg, 
								false);
						tx[l]=p1.x;
						ty[l]=p1.y;
					}
					g.fillPolygon(tx, ty, 3);
					g.drawPolygon(tx, ty, 3);
				}
			} else {
				g.setColor(new Color(00, 00, 00));
				for (int i1 = 0; i1 < w.path.length; i1++){
					short idx = w.path[i1];
					Point p1 = map.getMapPosition(
							(centerLat + nodeLat[idx] * FIXPT_MULT_INV) * radToDeg, 
							(centerLon + nodeLon[idx] * FIXPT_MULT_INV) * radToDeg, 
							false);
					tx[i1]=p1.x;
					ty[i1]=p1.y;
					}
				g.drawPolyline(tx, ty, w.path.length);
			}
		}
		
		// Draw nodes
		for (int i=0;i<iNodeCount;i++){
			
			float x = centerLat+nodeLat[i]*FIXPT_MULT_INV;
			float y = centerLon+nodeLon[i]*FIXPT_MULT_INV;
//			System.out.println("draw " + i + " "+ x + "/" + y);
			Point tl = map.getMapPosition(x * radToDeg, y * radToDeg, true);
			if (tl != null){
				g.fillOval(tl.x - 2, tl.y - 2, 5, 5);
			}
		}
	}

}
