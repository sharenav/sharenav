/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.gpsMid.mapData;

import java.util.Vector;

import de.ueller.midlet.gps.ScreenContext;

import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Projection;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.tile.PaintContext;


public abstract class Tile {
	public static final byte TYPE_MAP = 1;
	public static final byte TYPE_CONTAINER = 2;
	public static final byte TYPE_FILETILE = 4;
	public static final byte TYPE_EMPTY = 3;
	public static final byte TYPE_ROUTEDATA = 5;
	public static final byte TYPE_ROUTECONTAINER = 6;
	public static final byte TYPE_ROUTEFILE = 7;
	public static final byte TYPE_WAYPOINT = 8;

	public static final int OPT_WAIT_FOR_LOAD = 0;
	public static final int OPT_PAINT = 1;
	public static final int OPT_FIND_TARGET = 2;
	public static final int OPT_FIND_CURRENT= 3;
	
	public static final byte LAYER_AREA = Byte.MIN_VALUE;
	public static final byte LAYER_NODE = Byte.MAX_VALUE;
	
	

	public float minLat;
	public float maxLat;
	public float minLon;
	public float maxLon;
	public float centerLat;
	public float centerLon;
	
	public short fileId=0;
	public byte	lastUse	= 0;
//	public static Trace				trace				= null;

	/**
	 * Paint all elements of a tile to the PaintContext that are in layer
	 * There are two special layers LAYER_AREA and LAYER_NODE
	 * @param pc
	 */
	public abstract void paint(PaintContext pc, byte layer);
	public abstract void walk(PaintContext pc,int opt);
	public abstract boolean cleanup(int level);
	//public abstract void getWay(PaintContext pc,PositionMark pm,Way w);
		
	boolean contain(ScreenContext pc){
//		System.out.println(this);
//		System.out.println(pc.screenLD + "   " + pc.screenRU);
		if(maxLat < pc.screenLD.radlat) {
			return false;
		}
		if(maxLon < pc.screenLD.radlon) {
			return false;
		}
		if(minLat > pc.screenRU.radlat) {
			return false;
		}
		if(minLon > pc.screenRU.radlon) {
			return false;
		}
//		System.out.println("Paint gpsMidMap");
		return true;
	}
	boolean contain(float lat, float lon){
//		System.out.println(this);
//		System.out.println(pc.screenLD + "   " + pc.screenRU);
		if(maxLat < lat) {
			return false;
		}
		if(maxLon < lon) {
			return false;
		}
		if(minLat > lat) {
			return false;
		}
		if(minLon > lon) {
			return false;
		}
//		System.out.println("Paint gpsMidMap");
		return true;
	}
	boolean contain(float lat, float lon,float epsilon){
//		System.out.println(this);
//		System.out.println(pc.screenLD + "   " + pc.screenRU);
		if((maxLat+epsilon) < lat) {
			return false;
		}
		if((maxLon+epsilon) < lon) {
			return false;
		}
		if((minLat-epsilon) > lat) {
			return false;
		}
		if((minLon-epsilon) > lon) {
			return false;
		}
//		System.out.println("Paint gpsMidMap");
		return true;
	}
	boolean contain(PositionMark pm){
		if(maxLat < pm.lat) {
			return false;
		}
		if(maxLon < pm.lon) {
			return false;
		}
		if(minLat > pm.lat) {
			return false;
		}
		if(minLon > pm.lon) {
			return false;
		}
		return true;
	}
	public void getCenter(Node center){
		center.radlat=(maxLat+minLat)/2;
		center.radlon=(maxLon+minLon)/2;
	}
	protected void drawBounds(PaintContext pc, int r, int g, int b) {
			pc.g.setColor(r,g,b);
			IntPoint p1=new IntPoint(0,0);
			IntPoint p2=new IntPoint(0,0);
			IntPoint p3=new IntPoint(0,0);
			IntPoint p4=new IntPoint(0,0);
			Projection p = pc.getP();
			p.forward(minLat,minLon,p1,true);
			p.forward(minLat,maxLon,p2,true);
			p.forward(maxLat,maxLon,p3,true);
			p.forward(maxLat,minLon,p4,true);
			pc.g.drawLine(p1.x, p1.y, p2.x, p2.y);
			pc.g.drawLine(p2.x, p2.y, p3.x, p3.y);
			pc.g.drawLine(p3.x, p3.y, p4.x, p4.y);
			pc.g.drawLine(p4.x, p4.y, p1.x, p1.y);
	//		logger.debug("draw bounds" );
//			System.out.println(this);
//			System.out.println(p1.x + "," + p1.y + "/" + p2.x + "," + p2.y);
//			pc.g.fillTriangle(p1.x, p1.y, p2.x, p2.y, p1.x, p2.y);
		}
	
	/**
	    * Returns a Vector of SearchResult containing POIs of
	    * type searchType close to lat/lon. The list is ordered
	    * by distance with the closest one first.  
	    */
	public Vector getNearestPoi(byte searchType, float lat, float lon, float maxDist) {
		/**
		 * All of the Tile types for which we expect to perform a nearest 
		 * POI search should have this method overwritten
		 */
		System.out.println("getNearestPoi: We shouldn't be in this base function " + this);
		return new Vector();
	}
	public String toString(){
		return "Tile " + this.getClass().getName() + " " + minLat+","+minLon+"/"+ maxLat+","+maxLon;
	}
}
