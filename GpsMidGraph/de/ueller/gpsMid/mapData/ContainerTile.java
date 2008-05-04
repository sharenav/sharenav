package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import de.ueller.gps.data.SearchResult;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.tile.PaintContext;



public class ContainerTile extends Tile {
	//#debug error
	private final static Logger logger=Logger.getInstance(ContainerTile.class,Logger.DEBUG);

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
	
	public void paint(PaintContext pc, byte layer) {
		if (contain(pc)){
			if (t1 != null) {
				t1.paint(pc, layer);
			}
			if (t2 != null) {
				t2.paint(pc, layer);
			}	
		} else {			
			cleanup(4);
		}
	}
	public void walk(PaintContext pc,int opt) {

		if (contain(pc)){
			if (t1 != null) {
				t1.walk(pc,opt);
			}
			if (t2 != null) {
				t2.walk(pc,opt);
			}	
		} else {	
			cleanup(4);
		}
	}
	
	
	/**
	    * Returns a Vector of SearchResult containing POIs of
	    * type searchType close to lat/lon. The list is ordered
	    * by distance with the closest one first.
	    * 
	    * It checks which of the two sub tiles are closest to the
	    * coordinate and traverses that one first to check for
	    * close by POI.
	    */
	public Vector getNearestPoi(byte searchType, float lat, float lon, float maxDist) {
		boolean t1closer;
		Vector res;
		Vector res2;
		float t1dist = 0.0f;
		float t2dist = 0.0f;
		float distClose;
		float distFar;
				
		/**
		 * Determine which of the tile bounding boxes is closer to the point
		 * to which we are trying to find close by POIs 
		 */
		if (t1.maxLat < lat && t1.minLat < lat && t1.maxLon > lon && t1.minLon < lon) {
			/**
			 * If the bounding box contains the point, then the distance is 0
			 */
			t1dist = 0.0f;
		}
		if (t2.maxLat < lat && t2.minLat < lat && t2.maxLon > lon && t2.minLon < lon) {
			t2dist = 0.0f;
		}
		/**
		 * Distance to t1
		 */
		if (t1.maxLat < lat && t1.maxLon < lon) {
			t1dist = ProjMath.getDistance(t1.maxLat, t1.maxLon, lat, lon);
		} else if (t1.maxLat < lat && t1.minLon > lon) {
			t1dist = ProjMath.getDistance(t1.maxLat, t1.minLon, lat, lon);
		} else if (t1.minLat > lat && t1.minLon > lon) {
			t1dist = ProjMath.getDistance(t1.minLat, t1.minLon, lat, lon);
		}  else if (t1.minLat > lat && t1.maxLon < lon) {
			t1dist = ProjMath.getDistance(t1.minLat, t1.maxLon, lat, lon);
		} else if (t1.maxLat < lat) {
			t1dist = ProjMath.getDistance(t1.maxLat, lon, lat, lon);
		} else if (t1.minLat > lat) {
			t1dist = ProjMath.getDistance(t1.minLat, lon, lat, lon);
		} else if (t1.maxLon < lon) {
			t1dist = ProjMath.getDistance(lat, t1.maxLon, lat, lon);
		} if (t1.minLon > lon) {
			t1dist = ProjMath.getDistance(lat, t1.minLon, lat, lon);
		}
		/**
		 * Distance to t2
		 */
		if (t2.maxLat < lat && t2.maxLon < lon) {
			t2dist = ProjMath.getDistance(t2.maxLat, t2.maxLon, lat, lon);
		} else if (t2.maxLat < lat && t2.minLon > lon) {
			t2dist = ProjMath.getDistance(t2.maxLat, t2.minLon, lat, lon);
		} else if (t2.minLat > lat && t2.minLon > lon) {
			t2dist = ProjMath.getDistance(t2.minLat, t2.minLon, lat, lon);
		}  else if (t2.minLat > lat && t2.maxLon < lon) {
			t2dist = ProjMath.getDistance(t2.minLat, t2.maxLon, lat, lon);
		} else if (t2.maxLat < lat) {
			t2dist = ProjMath.getDistance(t2.maxLat, lon, lat, lon);
		} else if (t2.minLat > lat) {
			t2dist = ProjMath.getDistance(t2.minLat, lon, lat, lon);
		} else if (t2.maxLon < lon) {
			t2dist = ProjMath.getDistance(lat, t2.maxLon, lat, lon);
		} if (t2.minLon > lon) {
			t2dist = ProjMath.getDistance(lat, t2.minLon, lat, lon);
		}
					
		if (t1dist < t2dist) {
			t1closer = true;
			distClose = t1dist;
			distFar = t2dist;
		} else {
			t1closer = false;
			distClose = t2dist;
			distFar = t1dist;
		}
		
		if (distClose < maxDist) {			
			if (t1closer) 			
				res = t1.getNearestPoi(searchType, lat, lon, maxDist);			
			else
				res = t2.getNearestPoi(searchType, lat, lon, maxDist);			
		} else {			
			res = new Vector();
		}
		
		float maxDistFound;
		if (res.size() > 20) {
			maxDistFound = ((SearchResult)res.elementAt(19)).dist;
		} else {
			maxDistFound = maxDist;
		}
		/**
		 * TODO: This whole algorithm is still broken!
		 * It hopefully works in many cases, but it doesn't correctly handle
		 * the cases of POIs close to tile boundaries and the extent of Tiles
		 * I.e. Although the tile it self might be closer, the POIs in the other
		 * tile might end up being closer than some of the POIs in the closer tile
		 */
		if ((distFar < maxDistFound)) { // This might be inexact at tile boundries. We want to check if tile dist < largest res dist
			//logger.info("traversing tile 2 of dist: " + distFar);
			if (t1closer) 
				res2 = t2.getNearestPoi(searchType, lat, lon, maxDistFound);				
			else
				res2 = t1.getNearestPoi(searchType, lat, lon, maxDistFound);
				
			/**
			 * Perform a merge sort of the two result lists.
			 * As they are both sorted them selves, this is easy
			 * and efficient
			 */
			Vector resMerge = new Vector();
			int it1 = 0;
			int it2 = 0;
			for (int i = 0; i < res2.size() + res.size(); i++) {
				SearchResult a;
				SearchResult b;
				if (it1 < res.size())
					a = (SearchResult) res.elementAt(it1);
				else {
					resMerge.addElement(res2.elementAt(it2));
					it2++;
					continue;
				}
				if (it2 < res2.size())
					b = (SearchResult) res2.elementAt(it2);
				else {
					resMerge.addElement(a);
					it1++;
					continue;
				}				
				if (a.dist < b.dist) {
					resMerge.addElement(a);
					it1++;
				} else {
					resMerge.addElement(b);
					it2++;
				}
			}
			return resMerge;
		}		
		return res;
	}

}
