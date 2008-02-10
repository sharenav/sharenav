package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.QueueableTile;

public class FileTile extends Tile implements QueueableTile {
	byte zl;
	Tile tile=null;
	boolean inLoad=false;
//	private final static Logger logger=Logger.getInstance(FileTile.class,Logger.INFO);

	public FileTile(DataInputStream dis, int deep, byte zl) throws IOException {
//       	logger.info("create deep:"+deep + " zoom:"+zl);
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
		fileId=dis.readShort();
		this.zl=zl;
//       	logger.debug("ready "+deep + " zl="+ zl + " fid="+fid);

	}

	public boolean cleanup(int level) {
		if (! inLoad && tile != null) {
			// logger.info("test tile unused fid:" + fileId + "c:"+lastUse);
			if (lastUse > level) {
				tile=null;
//				 logger.info("discard content for FileTile " + fileId);
				return true;
			}
		}
		return false;
	}


	public String toString() {
		return "FT" + zl + "-" + fileId + ":" + lastUse;
	}

/*	public void getWay(PaintContext pc,PositionMark pm, Way w) {
			if (contain(pm)) {
				if (tile == null){
					try {
						pc.dictReader.readData(this);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
				} else {
					lastUse=0;
					tile.getWay(pc,pm, w);
				}
			}
		}*/

	private void paint(PaintContext pc, int method) {
		if (contain(pc)) {
			if (tile == null){
				if (!inLoad){
					inLoad=true;
					pc.dictReader.add(this);
				} else {
				}
			} else {
				lastUse=0;
				switch (method) {
				case 0: {tile.paint(pc); break;}
				case 1: {tile.paintAreaOnly(pc); break;}
				case 2: {tile.paintNonArea(pc); break;}
				}
			}
		}
	}
	
	public void paint(PaintContext pc) {
		paint(pc,0);
	}
	
	public void paintAreaOnly(PaintContext pc) {
		paint(pc,1);
	}

	public void paintNonArea(PaintContext pc) {
		paint(pc,2);
	}

}
