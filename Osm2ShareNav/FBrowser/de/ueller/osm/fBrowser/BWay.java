/**
 * OSM2ShareNav
 * 
 * @version $Revision$ ($Name$) Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osm.fBrowser;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;

import javax.swing.tree.TreeNode;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import net.sharenav.osmToShareNav.model.Way;

/**
 * @author hmu
 */
public class BWay implements TreeNode {

	public static final int	WAY_AREA	= 2 << 8;

	public short			minLat;
	public short			minLon;
	public short			maxLat;
	public short			maxLon;
	public int				nameIdx		= -1;
	public byte				type;
	public byte				flags;
	public byte				flags2;
	public short[]			path;
	public boolean			area		= false;
	private Tile			parent		= null;

	public BWay(DataInputStream is, byte f, int idx, Tile t) throws IOException {
		parent = t;
		minLat = is.readShort();
		minLon = is.readShort();
		maxLat = is.readShort();
		maxLon = is.readShort();

		type = is.readByte();
		// setWayRouteModes(
		is.readByte();

		if ((f & Way.WAY_FLAG_NAME) == Way.WAY_FLAG_NAME) {
			if ((f & Way.WAY_FLAG_NAMEHIGH) == Way.WAY_FLAG_NAMEHIGH) {
				nameIdx = is.readInt();
				// System.out.println("Name_High " + f );
			} else {
				nameIdx = is.readShort();
			}
		}
		if ((f & Way.WAY_FLAG_MAXSPEED) == Way.WAY_FLAG_MAXSPEED) {
			// logger.debug("read maxspeed");
			flags = is.readByte();
		}

		byte flags2 = 0;
		if ((f & Way.WAY_FLAG_ADDITIONALFLAG) > 0) {
			flags2 = is.readByte();
		}

		if ((flags2 & Way.WAY_FLAG2_MAXSPEED_WINTER) == Way.WAY_FLAG2_MAXSPEED_WINTER) {
			// setFlagswinter(
			is.readByte();
		}

		if ((f & Way.WAY_FLAG_LAYER) == Way.WAY_FLAG_LAYER) {
			is.readByte();
		}

		if ((f & Way.WAY_FLAG_AREA) > 0) {
			flags += WAY_AREA;
			area = true;
		}

		boolean longWays = false;
		if ((flags2 & Way.WAY_FLAG2_LONGWAY) > 0) {
			longWays = true;
		}
		int count;
		if (longWays) {
			count = is.readShort();
			if (count < 0) {
				count += 65536;
			}
		} else {
			count = is.readByte();
			if (count < 0) {
				count += 256;
			}

		}
		path = new short[count];
		for (short i = 0; i < count; i++) {
			path[i] = is.readShort();
		}
	}

	public boolean isArea() {
		return area;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#children()
	 */
	@Override
	public Enumeration children() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getAllowsChildren()
	 */
	@Override
	public boolean getAllowsChildren() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	@Override
	public TreeNode getChildAt(int childIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getChildCount()
	 */
	@Override
	public int getChildCount() {
		return 0;
		// if (isArea()){
		// return path.length/3;
		// } else {
		// return path.length;
		// }
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
	 */
	@Override
	public int getIndex(TreeNode node) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#getParent()
	 */
	@Override
	public TreeNode getParent() {
		return parent;
	}


	public SingleTile getTile() {
		return (SingleTile) parent;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.tree.TreeNode#isLeaf()
	 */
	@Override
	public boolean isLeaf() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (isArea()){
			return "Area " + path.length/3;
		} else {
			return "Way " + path.length;
		}
	}

}
