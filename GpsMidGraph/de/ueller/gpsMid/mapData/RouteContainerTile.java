package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.tile.PaintContext;



public class RouteContainerTile extends RouteBaseTile {
	//#debug error
	private final static Logger logger=Logger.getInstance(RouteContainerTile.class,Logger.DEBUG);

	RouteBaseTile t1;
	RouteBaseTile t2;
	
	RouteContainerTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	//#debug
       	logger.debug("start "+deep);
    	minLat=dis.readFloat();
    	minLon=dis.readFloat();
    	maxLat=dis.readFloat();
    	maxLon=dis.readFloat();
    	minId=dis.readInt();
    	maxId=dis.readInt();
    	//#debug
    	logger.debug("start left "+deep);
    	t1=(RouteBaseTile) readTile(dis,deep+1,zl);
    	//#debug
       	logger.debug("start right "+deep);
       	t2=(RouteBaseTile) readTile(dis,deep+1,zl);
        //#debug
    	logger.debug("ready "+deep+":readed ContainerTile");
    }
    
    public Tile readTile(DataInputStream dis,int deep,byte zl) throws IOException{
    	byte t=dis.readByte();
    	switch (t) {
    	case Tile.TYPE_EMPTY:
    		//#debug
    		logger.debug("r ET " + zl + " " + deep);
    		return null;
    	case Tile.TYPE_ROUTEFILE:
    		//#debug
    		logger.debug("r RFT " + zl + " " + deep);
    		return new RouteFileTile(dis,deep,zl);
    	case Tile.TYPE_ROUTEDATA:
    		// RouteData Tile
    		//#debug
    		logger.debug("r RT " + zl + " " + deep);
    		return new RouteTile(dis,deep,zl);
    	case Tile.TYPE_ROUTECONTAINER:
    		// RouteData Tile
    		//#debug
    		logger.debug("r RC " + zl + " " + deep);
    		return new RouteContainerTile(dis,deep,zl);
    	default:
    		//#debug error
    		logger.error("wrongTileType type=" + t);
    	throw new IOException("wrong TileType");
    	}

    }

	public void paint(PaintContext pc) {
		if (pc == null)
			return;
		if (contain(pc)){
			if (t1 != null){
				t1.paint(pc);
			}
			if (t2 != null){
				t2.paint(pc);
			}
		}
	}
	

	public boolean cleanup(int level) {
		lastUse++;
		if (t1 != null) {
			t1.cleanup(level);
		}
		if (t2 != null) {
			t2.cleanup(level);
		}
		return true;
	}

	public RouteNode getRouteNode(int id) {
		if (minId <= id && maxId >= id){
			RouteNode n=null;
			if (t1 != null){
				//#debug error
//				logger.debug("search left for " + id);
				n = t1.getRouteNode(id);
			}
			if (n==null && t2 != null){
				//#debug error
//				logger.debug("search right for " + id);
				n = t2.getRouteNode(id);
			}
			return n;
		} else {
			//#debug error
//			logger.debug("" + id + " not in tile");
		  return null;
		}
	}

	public RouteNode getRouteNode(RouteNode best, float lat, float lon) {
		if (contain(lat, lon)){
			if (t1 != null){
				best=t1.getRouteNode(best, lat, lon);
			}
			if (t2 != null){
				best=t2.getRouteNode(best, lat, lon);
			}
		}
		return best;
	}

	public Connection[] getConnections(int id, RouteBaseTile tile,boolean bestTime) {
		if (minId <= id && maxId >= id){
			lastUse=0;
			Connection[] n=null;
			if (t1 != null){
				n = t1.getConnections(id,tile,bestTime);
			}
			if (n==null && t2 != null){
				n = t2.getConnections(id,tile,bestTime);
			}
			return n;
		} else 
		  return null;
	}
}
