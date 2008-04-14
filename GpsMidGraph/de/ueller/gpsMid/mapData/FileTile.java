package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
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
	
	public void paint(PaintContext pc, byte layer) {	
		if (contain(pc)) {
			if (tile == null){
				if (!inLoad){
					inLoad=true;
					Trace.getInstance().getDictReader().add(this,null);
				} else {
				}
			} else {
				lastUse=0;
				tile.paint(pc,layer);
			}
		}
	}
	
	public void walk(PaintContext pc,int opt) {
		if (contain(pc)) {
			if (tile == null){
				if (!inLoad){
					inLoad=true;
					Trace.getInstance().getDictReader().add(this,null);
				} else {
//					logger.info("load already requestet");
				}
			} else {
				// delegate to tile
				lastUse=0;
				tile.walk(pc,opt);
			}
		}
	}
	
	public Vector getNearestPoi(byte searchType, float lat, float lon, float maxDist) {		
		if (tile == null){
			/**
			 * This hole case is not correctly handled. The POI data
			 * isn't loaded into memory, but we need it to search for
			 * points. However due to the asynchronous nature of implementation
			 * of dictReaded, we don't get the data in time
			 * 
			 * TODO: Need to think about this more of how to do this correctly
			 */
			if (!inLoad){				
				inLoad=true;
				Trace.getInstance().getDictReader().add(this,this);
			}
			synchronized(this) {
				try {
					/**
					 * Wait for the tile to be loaded in order to process it
					 * We should be notified once the data is loaded, but
					 * have a timeout of 500ms
					 */
					wait(500);
				} catch (InterruptedException e) {
					/**
					 * Nothing to do in this case, as we need to deal
					 * with the case that nothing has been returned anyway
					 */
				}					
			}							
		}
		/**
		 * Hopefully by now the tile is loaded, so try again
		 */
		if (tile != null){
			lastUse=0;
			return tile.getNearestPoi(searchType, lat, lon, maxDist);
		}
		/** 
		 * The tile was not loaded in time. In order not to slow down
		 * the processing too much, just return no result and continue
		 */
		return new Vector();
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

}
