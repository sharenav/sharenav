package org.openstreetmap.gui.jmapviewer;

//License: GPL. Copyright 2008

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

import org.openstreetmap.gui.jmapviewer.interfaces.MapMarkerArea;

/**
 * A implementation of the {@link MapMarkerArea} interface to draw rectangular regions on the map.
 * 
 * 
 */
public class MapMarkerRectangle implements MapMarkerArea {

	double latTL; double lonTL; double latBR; double lonBR;
	double lat; double lon;
	Color colorBoarder;
	Color colorFill;

	public MapMarkerRectangle(double latTL, double lonTL, double latBR, double lonBR) {
		this(Color.BLACK, Color.GREEN, latTL, lonTL, latBR, lonBR);
	}

	public MapMarkerRectangle(Color colorBoarder, Color colorFill, double latTL, double lonTL, double latBR, double lonBR) {
		super();
		this.colorBoarder = colorBoarder;
		this.colorFill = colorFill;
		setRectanlge(latTL, lonTL, latBR, lonBR);
	}
	
	public void setRectanlge( double latTL, double lonTL, double latBR, double lonBR) {
		if (latTL > latBR) {
			this.latTL = latTL;
			this.latBR = latBR;
		} else {
			this.latTL = latBR;
			this.latBR = latTL;
		}
		if (lonTL < lonBR) {
			this.lonTL = lonTL;
			this.lonBR = lonBR;
		} else {
			this.lonTL = lonBR;
			this.lonBR = lonTL;
		}
		
		lat = latBR - latTL;
		lon = lonTL - lonBR;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}

	public void paint(Graphics g, JMapViewer map) {
		Point tl; Point br;
		tl = map.getMapPosition(latTL, lonTL, false);
		br = map.getMapPosition(latBR, lonBR, false);
		if (tl == null || br == null)
			return;
		
		g.setColor(colorFill);
		g.fillRect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);
		g.setColor(colorBoarder);
		g.drawRect(tl.x, tl.y, br.x - tl.x, br.y - tl.y);
	}

	@Override
	public String toString() {
		return "Rectangular MapMarker at " + lat + " " + lon + "(" + latTL +"," + lonTL + "|" + latBR + "," + lonBR +")";
	}

}
