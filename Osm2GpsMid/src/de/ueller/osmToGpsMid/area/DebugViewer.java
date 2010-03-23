/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.area;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;

import javax.swing.JFrame;


import de.ueller.osmToGpsMid.model.Bounds;
import de.ueller.osmToGpsMid.model.Node;


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
	/**
	 * 
	 */
	public DebugViewer(Area a) {
		super("Triangulator Test");
		setSize(xs, ys);
		setVisible(true);
		setArea(a);
	}
	@Override
	public void paint(Graphics g) {
		Graphics2D g2=(Graphics2D) g;
		g.clearRect(0, 0, getWidth(), getHeight());
		g2.setColor(Color.WHITE);
		for (Outline o:a.getOutlineList()){
			drawOutline(g2, o,400);
			drawOutline(g2, o,0);
		}
		for (Outline o:a.getHoleList()){
			drawOutline(g2, o,400);
			drawOutline(g2, o,0);
		}
		g2.setColor(Color.cyan);
		drawOutline(g2, a.outline,0);
		drawOutline(g2, a.outline,400);
		Color cf = new Color(0,255,0,50);		
		Color co = Color.BLACK;
		for (Triangle t:a.triangleList){
			drawTriangle(g2, t, cf, co);
		}
		drawTriangle(g2, a.triangle, new Color(255,0,0,40), Color.RED);
		if (a.edgeInside != null){
			Point ei=toScreen(a.edgeInside.getNode());
			g2.setColor(Color.magenta);
			g2.drawString("*", ei.x, ei.y);
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
		System.out.println("DebugViewer.drawOutline()");
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
		g2.drawLine(s.x, s.y, e.x, e.y);
	}
	private Point toScreen(Node n) {
		int x = (int)((n.lat-ox)*f + 20);
		int y = (int)(ys-20-(n.lon-oy)*f);
//		System.out.println("DebugViewer.toScreen() " + x + " " +y);
		return new Point(x,y);
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

	
	
	
}
