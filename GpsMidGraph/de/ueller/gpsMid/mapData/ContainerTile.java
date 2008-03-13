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



public class ContainerTile extends Tile {
	//#debug error
	private final static Logger logger=Logger.getInstance(ContainerTile.class,Logger.ERROR);

	Tile t1;
	Tile t2;
//    ContainerTile parent=null;
    
    ContainerTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	//#debug
       	logger.debug("start "+deep);
    	minLat=dis.readFloat();
    	minLon=dis.readFloat();
    	maxLat=dis.readFloat();
    	maxLon=dis.readFloat();
    	//#debug
    	logger.debug("start left "+deep);
    	t1=readTile(dis,deep+1,zl);
    	//#debug
       	logger.debug("start right "+deep);
       	t2=readTile(dis,deep+1,zl);
        //#debug
    	logger.debug("ready "+deep+":readed ContainerTile");
    }
    
    public Tile readTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	byte t=dis.readByte();
    	switch (t) {
    	case Tile.TYPE_MAP:
    		//#debug
    		logger.debug("r ST " + zl + " " + deep);
    		return new SingleTile(dis,deep,zl);
    	case Tile.TYPE_CONTAINER:
    		//#debug
    		logger.debug("r CT " + zl + " " + deep);
    		return new ContainerTile(dis,deep,zl);
    	case Tile.TYPE_EMPTY:
    		//#debug
    		logger.debug("r ET " + zl + " " + deep);
    		return null;
    	case Tile.TYPE_FILETILE:
    		//#debug
    		logger.debug("r FT " + zl + " " + deep);
    		return new FileTile(dis,deep,zl);
    	case Tile.TYPE_ROUTEFILE:
    		//#debug
    		logger.debug("r RFT " + zl + " " + deep);
    		return new RouteFileTile(dis,deep,zl);
    	default:
    		//#debug error
    		logger.error("wrongTileType");
    	throw new IOException("wrong TileType");
    	}
    }

	

	public boolean cleanup(int level) {
		return true;
//		lastUse++;
//		if (t1 != null) {
//			t1.cleanup();
//		}
//		if (t2 != null) {
//			t2.cleanup();
//		}
		
	}
/*	public void getWay(PaintContext pc,PositionMark pm,Way w){
		if (contain(pm)){
			if (t1 != null) {
				//#debug
				logger.debug("search container left");
				t1.getWay(pc,pm,w);
			}
			if (t2 != null) {
				//#debug
				logger.debug("search container right");
				t2.getWay(pc,pm,w);
			}	
		} else {			
			cleanup(4);
		}
		
	}*/
	
	public void paint(PaintContext pc) {
		//#debug
		logger.debug("paint container");
		if (contain(pc)){
//			drawBounds(pc, 255, 255, 255);
			if (t1 != null) {
				//#debug
				logger.debug("paint container left");
				t1.paint(pc);
			}
			if (t2 != null) {
				//#debug
				logger.debug("paint container right");
				t2.paint(pc);
			}	
		} else {			
			cleanup(4);
		}
	}
	public void walk(PaintContext pc,int opt) {
		//#debug
		logger.debug("paint container");
		if (contain(pc)){
//			drawBounds(pc, 255, 255, 255);
			if (t1 != null) {
				//#debug
				logger.debug("paint container left");
				t1.walk(pc,opt);
			}
			if (t2 != null) {
				//#debug
				logger.debug("paint container right");
				t2.walk(pc,opt);
			}	
		} else {			
			cleanup(4);
		}
	}
	

	public void paintAreaOnly(PaintContext pc) {
		//#debug
		logger.debug("paint container (Area only)");
		if (contain(pc)){
			if (t1 != null) {
				//#debug
				logger.debug("paint container left");
				t1.paintAreaOnly(pc);
			}
			if (t2 != null) {
				//#debug
				logger.debug("paint container right");
				t2.paintAreaOnly(pc);
			}	
		} else {			
			cleanup(4);
		}
		
	}
	
	public void paintNonArea(PaintContext pc) {
		//#debug
		logger.debug("paint container (apart from area ways");
		if (contain(pc)){
			if (t1 != null) {
				//#debug
				logger.debug("paint container left");
				t1.paintNonArea(pc);
			}
			if (t2 != null) {
				//#debug
				logger.debug("paint container right");
				t2.paintNonArea(pc);
			}	
		} else {			
			cleanup(4);
		}
	}
	public void getWay(PaintContext pc,PositionMark pm,Way w){
		if (contain(pm)){
			if (t1 != null) {
				//#debug
				logger.debug("search container left");
				t1.getWay(pc,pm,w);
			}
			if (t2 != null) {
				//#debug
				logger.debug("search container right");
				t2.getWay(pc,pm,w);
			}	
		} else {			
			cleanup(4);
		}
		
	}
}
