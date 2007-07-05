package de.ueller.midlet.gps.tile;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.midlet.gps.Logger;



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
    	switch (t){
    		case 1:
    			//#debug
    			logger.debug("r ST " + zl + " " + deep);
    			return new SingleTile(dis,deep,zl);
    		case 2:
    			//#debug
    			logger.debug("r CT " + zl + " " + deep);
    			return new ContainerTile(dis,deep,zl);
    		case 3:
    			//#debug
    			logger.debug("r ET " + zl + " " + deep);
    			return null;
    		case 4:
    			//#debug
    			logger.debug("r FT " + zl + " " + deep);
    			return new FileTile(dis,deep,zl);
    		default:
    			//#debug error
    			logger.error("wrongTileType");
    			throw new IOException("wrong TileType");
    	}
    }

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
}
