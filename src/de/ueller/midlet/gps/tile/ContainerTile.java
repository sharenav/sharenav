package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.ScreenContext;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.VisibleElements;


public class ContainerTile extends Tile {
	private final static Logger logger=Logger.getInstance(ContainerTile.class,Logger.ERROR);

	Tile t1;
	Tile t2;
//    ContainerTile parent=null;
    
    ContainerTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	minLat=dis.readFloat();
    	minLon=dis.readFloat();
    	maxLat=dis.readFloat();
    	maxLon=dis.readFloat();
    	t1=readTile(dis,deep+1,zl);
    	t2=readTile(dis,deep+1,zl);
//    	logger.debug(""+deep+":readed ContainerTile");
    }
    
    public Tile readTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	byte t=dis.readByte();
    	switch (t){
    		case 1:
    			logger.debug("r ST " + zl + " " + deep);
    			return new SingleTile(dis,deep,zl);
    		case 2:
    			logger.debug("r CT " + zl + " " + deep);
    			return new ContainerTile(dis,deep,zl);
    		case 3:
    			logger.debug("r ET " + zl + " " + deep);
    			return null;
    		default:
    			logger.debug("wrongTileType");
    			throw new IOException("wrong TileType");
    	}
    }

	public void paint(PaintContext pc) {
//		logger.debug("paint container");
		if (contain(pc)){
//			drawBounds(pc, 255, 255, 255);
			if (t1 != null)
				t1.paint(pc);
			if (t2 != null)
				t2.paint(pc);	
		} else {			
			cleanup();
		}
	}
	public void collect(ScreenContext sc,VisibleElements ve) {
		if (contain(sc)){
			if (t1 != null)
				t1.collect(sc,ve);
			if (t2 != null)
				t2.collect(sc,ve);	
		} else {			
			cleanup();
		}
	}

	public void cleanup() {
		lastUse++;
		if (t1 != null)
			t1.cleanup();
		if (t2 != null)
			t2.cleanup();
		
	}
}
