package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.midlet.gps.Logger;

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

	public void paint(PaintContext pc) {
//		drawBounds(pc, 255, 55, 55);
		if (contain(pc)) {
//			logger.info("paint fid:" + fid);
			if (tile == null){
				if (!inLoad){
					inLoad=true;
//					logger.info("add to dictReader queue fid:" + fileId + " zoom:"+zl);
					pc.dictReader.add(this);
				} else {
//					logger.info("load already requestet");
				}
			} else {
				// delegate to tile
//				logger.info("delegate to tile");
				lastUse=0;
				tile.paint(pc);
			}
		}
	}
	public String toString() {
		return "FT" + zl + "-" + fileId + ":" + lastUse;
	}

}
