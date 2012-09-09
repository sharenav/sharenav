/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.area;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;

import javax.swing.JFrame;


import net.sharenav.osmToShareNav.model.Bounds;
import net.sharenav.osmToShareNav.model.Node;


/**
 * @author hmu
 *
 */
public class DebugViewer extends JFrame {
	private int xs=1200;
	private int ys=1000;
	private Area a;
	float f;
	float ox,oy;
	public ArrayList<Triangle> alt=null;
	static DebugViewer instanz=null;
	
	public static DebugViewer getInstanz(Area a){
		if (instanz == null){
			instanz=new DebugViewer(a);
		} else {
			instanz.setArea(a);
		}
		return instanz;
		}
	public static DebugViewer getInstanz(ArrayList<Triangle> triangleList){
		if (instanz == null){
			instanz=new DebugViewer(triangleList);
		} else {
			instanz.a.triangleList=(ArrayList<Triangle>) triangleList.clone();
		}
		return instanz;
		}
	/**
	 * 
	 */
	public DebugViewer(Area a) {
		super("Triangulator Test");
		setSize(xs, ys);
		setVisible(true);
		setArea(a);
	}
	public DebugViewer(ArrayList<Triangle> triangleList) {
		super("Triangulator Test");
		setSize(xs, ys);
		setVisible(true);
		setArea(new Area());
		a.triangleList=(ArrayList<Triangle>) triangleList.clone();
	}
	@Override
	public void paint(Graphics g) {
		Graphics2D g2=(Graphics2D) g;
		g.clearRect(0, 0, getWidth(), getHeight());
		try {
			g2.setColor(Color.WHITE);
			for (Outline o:a.getOutlineList()){
//			drawOutline(g2, o,400);
				drawOutline(g2, o,0);
			}
			for (Outline o:a.getHoleList()){
//			drawOutline(g2, o,400);
				drawOutline(g2, o,0);
			}
			g2.setColor(Color.cyan);
			drawOutline(g2, a.outline,0);
//		drawOutline(g2, a.outline,400);
			Color cf = new Color(0,255,0,50);		
			Color co = Color.BLACK;
			for (Triangle t:a.triangleList){
				drawTriangle(g2, t, cf, co);
			}
			if (a.triangle != null){
				drawTriangle(g2, a.triangle, new Color(255,0,0,40), Color.RED);
			}
			Color cAlt = new Color(255,255,0,40);
			if (alt != null){
			for (Triangle t:alt){
				drawTriangle(g2, t, cAlt, co);
			}
			if (a.edgeInside != null){
				Point ei=toScreen(a.edgeInside.getNode());
				g2.setColor(Color.magenta);
				g2.drawString("*", ei.x, ei.y);
			}
			}
		} catch (Exception e) {
			System.out.println("error while painting " + e.getLocalizedMessage());
		}
	}
	/**
	 * @param g2
	 * @param t
	 * @param cf
	 * @param co
	 */
	private void drawTriangle(Graphics2D g2, Triangle t, Color cf, Color co) {
		g2.setColor(cf);
		Point p;
		Vertex[] vert = t.getVert();
		Polygon po=new Polygon();
		for (int i=0;i<3;i++){
			p=toScreen(vert[i].getNode());
			po.addPoint(p.x,p.y);
		}
		g2.fillPolygon(po);
		g2.setColor(co);
		g2.drawPolygon(po);
	}
	/**
	 * @param g2
	 * @param o
	 */
	private void drawOutline(Graphics2D g2, Outline o,int xoff) {
		if (o==null || o.getVertexList().size()==0){
			return;
		}
//		System.out.println("DebugViewer.drawOutline()");
		Point s=null;
		Node n=null;
		Vertex vl=null;
		int i=0;
		for (Vertex v:o.getVertexList()){
			n=v.getNode();
			Point e=toScreen(n);
			if (s != null){
				g2.drawLine(s.x+xoff, s.y, e.x+xoff, e.y);
				if (xoff != 0){
					g2.drawString(""+i++, s.x+xoff+3, s.y-3);
				}
			}
			s=e;
			vl=v;
		}
		Point e=toScreen(vl.getNode());
		//close polygon from last endpoint to startpoint 
		g2.drawLine(s.x, s.y, e.x, e.y);
	}
	private Point toScreen(Node n) {
		int x = ys-(int)((n.lat-ox)*f + 20);
		int y = (int)(20+(n.lon-oy)*f);
//		System.out.println("DebugViewer.toScreen() " + x + " " +y);
		return new Point(y,x);
	}
	/**
	 * @param a the a to set
	 */
	public void setArea(Area a) {
		Bounds b=a.extendBounds(null);
		float fx=(xs-50)/(b.maxLat-b.minLat);
		float fy=(ys-50)/(b.maxLon-b.minLon);
		if (fx>fy){
			f=fy;
		} else {
			f=fx;
		}
		ox=b.minLat;
		oy=b.minLon;
		this.a=a;

	}

	public void recalcView(){
		Bounds b=a.extendBounds(null);
		if ( alt != null){
			for (Triangle t: alt){
				t.extendBound(b);
			}
		}
		float fx=(xs-50)/(b.maxLat-b.minLat);
		float fy=(ys-50)/(b.maxLon-b.minLon);
		if (fx>fy){
			f=fy;
		} else {
			f=fx;
		}
		ox=b.minLat;
		oy=b.minLon;
	}
	
	
	
}
