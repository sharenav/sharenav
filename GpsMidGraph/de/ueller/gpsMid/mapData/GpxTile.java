package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import java.util.Random;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.tile.PaintContext;

public class GpxTile extends Tile {	
	private final static Logger logger=Logger.getInstance(GpxTile.class,Logger.DEBUG);
	
	private static Font wptFont;
	
	PositionMark [] waypts;
	float [] trkPtLat;
	float [] trkPtLon;
	int noWayPts;
	int noTrkPts;
	
	GpxTile t1;
	GpxTile t2;
	
	float splitCoord;
	boolean splitDimension;
	
	
	public GpxTile() {
		waypts = new PositionMark[10];		
		noWayPts = 0;
		trkPtLat = new float[10];
		trkPtLon = new float[10];
		noTrkPts = 0;
		t1 = null;
		t2 = null;
	}

	public void addTrkPt(float lat, float lon, boolean rad) {
		if (!rad) {
			lat = lat*MoreMath.FAC_DECTORAD;
			lon = lon*MoreMath.FAC_DECTORAD;
			rad = true;
		}
		//#debug info
		logger.debug("Adding trackpoint Nr: " + noTrkPts + " "  + lat + " " + lon);		
		if (lat < minLat)
			minLat = lat;
		if (lat > maxLat)
			maxLat = lat;
		if (lon < minLon)
			minLon = lon;
		if (lon > maxLon)
			maxLon = lon;
		
		if (trkPtLat == null) {
			if (t1t2TrackPoint(lat, lon))
				t1.addTrkPt(lat, lon, rad);
			else
				t2.addTrkPt(lat, lon, rad);
		} else {
			trkPtLat[noTrkPts] = lat;
			trkPtLon[noTrkPts++] = lon;
			if (noTrkPts + 3 > trkPtLat.length) {
				if (noTrkPts > 90) {									
					splitTile(false);
				} else {
					extendTile();
				}
			}
		}
	}
	
	public void addWayPt (PositionMark waypt) {
		//#debug debug
		logger.debug("Adding waypoint: " + waypt);
		if (trkPtLat == null) {
			if (t1t2WayPoint(waypt)) {
				t1.addWayPt(waypt);
			} else {
				t2.addWayPt(waypt);
			}						
		} else {
			waypts[noWayPts++] = waypt;

			//waypts.addElement(wp);
			if (waypt.lat < minLat)
				minLat = waypt.lat;
			if (waypt.lat > maxLat)
				maxLat = waypt.lat;
			if (waypt.lon < minLon)
				minLon = waypt.lon;
			if (waypt.lon > maxLon)
				maxLon = waypt.lon;

			if (noWayPts + 3 > waypts.length) {
				if (noWayPts > 90) {										
					splitTile(true);
				} else {
					PositionMark [] tmp;
					tmp = new PositionMark[waypts.length + 10];
					System.arraycopy(waypts, 0, tmp, 0, waypts.length);
					waypts = tmp;
				}
			}
		}
	}
	
	public PositionMark [] listWayPt() {
		if (waypts == null) {
			
			PositionMark [] waypts1 = t1.listWayPt();
			PositionMark [] waypts2 = t2.listWayPt();
			PositionMark [] waypts = new PositionMark[waypts1.length + waypts2.length];
			System.arraycopy(waypts1, 0, waypts, 0, waypts1.length);
			System.arraycopy(waypts2, 0, waypts, waypts1.length, waypts2.length);
			return waypts;
			
		} else {
		PositionMark [] waypts = new PositionMark[noWayPts];
		System.arraycopy(this.waypts, 0, waypts, 0, noWayPts);
		return waypts;
		}
	}
	
	public boolean cleanup(int level) {
		// TODO Auto-generated method stub
		return false;
	}

	public void paint(PaintContext pc, byte layer) {
		if (layer == Tile.LAYER_NODE) {
			if (contain(pc)) {			
				if (waypts == null) {
					t1.paint(pc, layer);
					t2.paint(pc, layer);
				} else
					paintLocal(pc);
			}
		}
	}
	
	public void dropTrk() {
		noTrkPts = 0;
		trkPtLat = null;
		trkPtLon = null;		
		if (t1 != null) {
			t1.dropTrk();		
			t2.dropTrk();
		} else {
			trkPtLat = new float[5];
			trkPtLon = new float[5];
		}
	}
	
	public void dropWayPt() {
		noWayPts = 0;
		waypts = null;
		if (t1 != null) {
			t1.dropWayPt();		
			t2.dropWayPt();
		} else {
			waypts = new PositionMark[5];
		}
	}
	
	private void paintLocal(PaintContext pc) {
		/**
		 * Painting Waypoints
		 */
		for (int i = 0; i < noWayPts; i++) {
			PositionMark waypt = waypts[i];
			
			if (pc.getP().isPlotable(waypt.lat, waypt.lon)) {
//				if (waypt.lat < pc.screenLD.radlat) {
//					continue;
//				}
//				if (waypt.lon < pc.screenLD.radlon) {
//					continue;
//				}
//				if (waypt.lat > pc.screenRU.radlat) {
//					continue;
//				}
//				if (waypt.lon > pc.screenRU.radlon) {
//					continue;
//				}					
				
				pc.getP().forward(waypt.lat, waypt.lon, pc.lineP2);
				pc.g.drawImage(pc.images.IMG_MARK,pc.lineP2.x,pc.lineP2.y,Graphics.HCENTER|Graphics.VCENTER);
				if (Trace.getInstance().getConfig().getCfgBitState(Configuration.CFGBIT_WPTTEXTS) ) {
					pc.g.setColor(0,0,0);
					Font originalFont = pc.g.getFont();
					if (wptFont==null) {
						if (Trace.getInstance().getConfig().getCfgBitState(Configuration.CFGBIT_WPT_LABELS_LARGER)) {
							wptFont = originalFont;
						} else {
							wptFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
						}
					}
					pc.g.setFont(wptFont);
					pc.g.drawString(waypt.displayName,pc.lineP2.x,pc.lineP2.y,Graphics.HCENTER|Graphics.BOTTOM);
					pc.g.setFont(originalFont);
				}
			}
			
		}
		/**
		 * Painting Tracklogs
		 */			
		for (int i = 0; i < noTrkPts; i++) {
			if (pc.getP().isPlotable(trkPtLat[i], trkPtLon[i])) {					
//				if (trkPtLat[i] < pc.screenLD.radlat) {
//					continue;
//				}
//				if (trkPtLon[i] < pc.screenLD.radlon) {
//					continue;
//				}
//				if (trkPtLat[i] > pc.screenRU.radlat) {
//					continue;
//				}
//				if (trkPtLon[i] > pc.screenRU.radlon) {
//					continue;
//				}
				pc.getP().forward(trkPtLat[i], trkPtLon[i], pc.lineP2);
				pc.g.drawImage(pc.images.IMG_MARK,pc.lineP2.x,pc.lineP2.y,Graphics.HCENTER|Graphics.VCENTER);					
			}
		}
	}
	
	private void extendTile() {
		logger.info("Extending trackpoint array to: " + (trkPtLat.length + 10));
		float [] tmp;
		tmp = new float[trkPtLat.length + 10];
		System.arraycopy(trkPtLat, 0, tmp, 0, trkPtLat.length);
		trkPtLat = tmp;
		tmp = new float[trkPtLat.length];
		System.arraycopy(trkPtLon, 0, tmp, 0, trkPtLon.length);
		trkPtLon = tmp;		
	}
	
	private void splitTile(boolean waypointSplit) {
		logger.info("Trying to split GPX tile containing " + noTrkPts + "track points and " + noWayPts + " Waypoints");
		Random r = new Random();
		if (waypointSplit) {
			if (maxLat - minLat > maxLon - minLon) {
				splitDimension = true;			
				splitCoord = waypts[r.nextInt(noWayPts)].lat;				
			} else {
				splitDimension = false;
				splitCoord = waypts[r.nextInt(noWayPts)].lon;
			}
			/**
			 * Check to see that we reduce the number of waypoints / track points
			 * by at least 5, as otherwise we can get into an infinite recurrsion.
			 * This can happen for example if many points are recorded that have
			 * identical coordinates		 *  
			 */
			int [] t1t2count = new int[2];
			for (int i = 0; i < noWayPts; i++) {
				t1t2count[t1t2WayPoint(waypts[i])?0:1]++;			
			}
			if (t1t2count[0] < 5 || t1t2count[1] < 5) {
				logger.info("Split was unsuccessfull, perhaps too many identical coordinates");
				extendTile();
				return;
			}
		} else {
			if (maxLat - minLat > maxLon - minLon) {
				splitDimension = true;
				splitCoord = HelperRoutines.medianElement(trkPtLat, noTrkPts);				
			} else {
				splitDimension = false;
				splitCoord = HelperRoutines.medianElement(trkPtLon, noTrkPts);
			}
			/**
			 * Check to see that we reduce the number of waypoints / track points
			 * by at least 5, as otherwise we can get into an infinite recurrsion.
			 * This can happen for example if many points are recorded that have
			 * identical coordinates		 *  
			 */
			int [] t1t2count = new int[2];
			for (int i = 0; i < noTrkPts; i++) {
				t1t2count[t1t2TrackPoint(trkPtLat[i], trkPtLon[i])?0:1]++;			
			}
			if (t1t2count[0] < 5 || t1t2count[1] < 5) {
				logger.info("Split was unsuccessfull, perhaps too many identical coordinates");
				extendTile();
				return;
			}
		}		
		//#debug debug
		logger.debug("Splitting GpxTile (" + splitDimension + ") "  + splitCoord);
		t1 = new GpxTile();
		t2 = new GpxTile();
		for (int i = 0; i < noTrkPts; i++) {
			if (t1t2TrackPoint(trkPtLat[i], trkPtLon[i]))
				t1.addTrkPt(trkPtLat[i], trkPtLon[i],true);
			else
				t2.addTrkPt(trkPtLat[i], trkPtLon[i], true);			
		}
		for (int i = 0; i < noWayPts; i++) {
			if (t1t2WayPoint(waypts[i]))
				t1.addWayPt(waypts[i]);
			else
				t2.addWayPt(waypts[i]);			
		}
		waypts = null;
		trkPtLat = null;
		trkPtLon = null;
	}
	
	public boolean existsWayPt(PositionMark newWayPt) {		
		if ( waypts == null) {
			if (splitDimension) {
				if (newWayPt.lat < splitCoord) {
					return t1.existsWayPt(newWayPt);
				} else {
					return t2.existsWayPt(newWayPt);
				}
			} else {
				if (newWayPt.lon < splitCoord) {
					return t1.existsWayPt(newWayPt);
				} else {
					return t2.existsWayPt(newWayPt);
				}			
			}			
		} else {			
			for (int i = 0; i < noWayPts; i++) {
				PositionMark wayPt = waypts[i];
				if (newWayPt.lat==wayPt.lat &&
					newWayPt.lon==wayPt.lon &&
					newWayPt.displayName.equals(wayPt.displayName))
				{
					return true;
				}
			}
		}
		return false;
	}

	public void walk(PaintContext pc, int opt) {
		// TODO Auto-generated method stub
		
	}
	
	private boolean t1t2TrackPoint(float lat, float lon) {		
		if (splitDimension) {
			if (lat < splitCoord) 
				return true;
			else
				return false;
		} else {
			if (lon < splitCoord) 
				return true;
			else
				return false;
		}

	}
	private boolean t1t2WayPoint(PositionMark p) {		
		if (splitDimension) {
			if (p.lat < splitCoord) 
				return true;
			else
				return false;
		} else {
			if (p.lon < splitCoord) 
				return true;
			else
				return false;
		}

	}

	public static void newWptFont() {
	   wptFont = null;
   }   	
}
