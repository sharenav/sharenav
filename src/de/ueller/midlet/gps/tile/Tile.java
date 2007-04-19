/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.midlet.gps.tile;

import de.ueller.midlet.gps.ScreenContext;
import de.ueller.midlet.gps.Trace;

import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Node;


public abstract class Tile {
	float minLat;
	float maxLat;
	float minLon;
	float maxLon;
//	int fileId=0;//
	public byte	lastUse	= 0;
	public static Trace				trace				= null;

	public abstract void paint(PaintContext pc);
	public abstract boolean cleanup();
	
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
			pc.p.forward(minLat,minLon,p1,true);
			pc.p.forward(minLat,maxLon,p2,true);
			pc.p.forward(maxLat,maxLon,p3,true);
			pc.p.forward(maxLat,minLon,p4,true);
			pc.g.drawLine(p1.x, p1.y, p2.x, p2.y);
			pc.g.drawLine(p2.x, p2.y, p3.x, p3.y);
			pc.g.drawLine(p3.x, p3.y, p4.x, p4.y);
			pc.g.drawLine(p4.x, p4.y, p1.x, p1.y);
	//		logger.debug("draw bounds" );
//			System.out.println(this);
//			System.out.println(p1.x + "," + p1.y + "/" + p2.x + "," + p2.y);
//			pc.g.fillTriangle(p1.x, p1.y, p2.x, p2.y, p1.x, p2.y);
		}
	public String toString(){
		return "Tile " + this.getClass().getName() + " " + minLat+","+minLon+"/"+ maxLat+","+maxLon;
	}
}
