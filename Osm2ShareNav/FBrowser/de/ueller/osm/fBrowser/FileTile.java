package net.sharenav.osm.fBrowser;

/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net See Copying
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.tree.TreeNode;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

public class FileTile extends Tile {
	byte				zl;
	Tile				tile	= null;
	private final int	deep;
	String root;

	public FileTile(DataInputStream dis, int deep, byte zl,String root) throws IOException {
		this.root=root;
		this.deep = deep;
		// logger.info("create deep:"+deep + " zoom:"+zl);
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
		fileId = dis.readShort();
		this.zl = zl;
		// logger.debug("ready "+deep + " zl="+ zl + " fid="+fid);

	}

	public String toString() {
		return deep + " File " + "-" + fileId + super.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildCount()
	 */
	@Override
	public int getChildCount() {
		return 1;
	}

	public void readData() throws IOException {
		String fname = root + "/d" + zl + "/" + fileId + ".d";
		System.out.println("Read " + fname);
		InputStream is = new FileInputStream(fname);
		if (is == null) {
			// logger.error("file inputStream /d"+tt.zl+tt.fileId+".d not found" );
			throw new IOException("File not found " + fname);
		}
		// logger.info("open DataInputStream");
		DataInputStream ds = new DataInputStream(is);
		if (ds == null) {
			// logger.error("file DataImputStream "+url+" not found" );
			is.close();
			throw new IOException("DataStream not open for  " + fname);
		}
		// end open data from JAR
		// logger.info("read Magic code");
		if (!"DictMid".equals(ds.readUTF())) { throw new IOException("not a DictMid-file"); }
		// logger.trace("read TileType");
		byte type = ds.readByte();
		// logger.trace("TileType="+type);
		Tile dict = null;
		switch (type) {
			case 1:
				dict = new SingleTile(ds, 1, zl,root);
				break;
			case 2:
				dict = new ContainerTile(ds, 1, zl,root);
				break;
			case 3:
				dict = new Empty();
				return;
			case 4:
				dict = new FileTile(ds, 1, zl,root);
				break;
			default:
				break;
		}
		if (!"END".equals(ds.readUTF())) { throw new IOException("not a DictMid-file"); }

		ds.close();

		tile = dict;

	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	@Override
	public TreeNode getChildAt(int childIndex) {
		if (childIndex != 0) return null;
		try {
			if (tile == null) {
				readData();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return tile;
	}

	@Override
	public void paint(Graphics g, Point tl, Point br,int deep) {

		g.setColor(new Color(255/deep,0,0));
		g.drawRect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);	
		try {
			if (tile == null) {
				readData();
			}
			tile.paint(g, tl, br, deep);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


}

	/* (non-Javadoc)
	 * @see org.openstreetmap.gui.jmapviewer.interfaces.MapRectangle#paint(java.awt.Graphics, java.awt.Point, java.awt.Point)
	 */
	@Override
	public void paint(Graphics g, Point topLeft, Point bottomRight) {
		paint(g,topLeft,bottomRight,1);
	}

}
