/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 *          Copyright (c) 2008 Markus Baeurle mbaeurle at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.tile;

import java.util.Random;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.PaintContext;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;

public class WaypointsTile extends Tile {
	private final static Logger logger = Logger.getInstance(WaypointsTile.class,
			Logger.DEBUG);

	private static Font wptFont;

	// Vector holding the waypoints unless this tile was split.
	Vector wayPts;

	// Total number of waypoints that are stored in this tile or its subtiles.
	int totalWayPts;

	// Sub tiles, will be null if no split was yet made.
	WaypointsTile t1;
	WaypointsTile t2;

	/**
	 * Coordinate at which this tile was split. splitDimension determines if
	 * it's latitude or longitude.
	 */
	float splitCoord;

	// Dimension at which this tile was split: true = lat, false = lon.
	boolean splitDimension;

	public static float wptOutlineLat[] = null;
	public static float wptOutlineLon[] = null;

	public WaypointsTile() {
		wayPts = new Vector();
		totalWayPts = 0;
		t1 = null;
		t2 = null;
	}

	public synchronized void addWayPt(PositionMark wayPt) {
		// #debug debug
		logger.debug("Adding waypoint: " + wayPt);
		if ((t1 != null) && (t2 != null)) {
			if (t1t2WayPoint(wayPt)) {
				t1.addWayPt(wayPt);
			} else {
				t2.addWayPt(wayPt);
			}
		} else {
			wayPts.addElement(wayPt);
			
			if (wayPts.size() > 90) {
				splitTile();
			}
		}
		if (wayPt.lat < minLat) {
			minLat = wayPt.lat;
		}
		if (wayPt.lat > maxLat) {
			maxLat = wayPt.lat;
		}
		if (wayPt.lon < minLon) {
			minLon = wayPt.lon;
		}
		if (wayPt.lon > maxLon) {
			maxLon = wayPt.lon;
		}
		totalWayPts++;
	}

	public synchronized Vector listWayPt() {
		if ((t1 != null) && (t2 != null)) {
			Vector wayPtsAll = new Vector(t1.getNumberWaypoints()
					+ t2.getNumberWaypoints());
			// May be the result of another copy-together, so we should buffer them.
			Vector t1WayPts = t1.listWayPt();
			Vector t2WayPts = t2.listWayPt();
			for (int i = 0; i < t1.getNumberWaypoints(); i++) {
				wayPtsAll.addElement(t1WayPts.elementAt(i));
			}
			for (int i = 0; i < t2.getNumberWaypoints(); i++) {
				wayPtsAll.addElement(t2WayPts.elementAt(i));
			}
			return wayPtsAll;
		} else {
			return wayPts;
		}
	}

	public int getNumberWaypoints() {
		return totalWayPts;
	}

	public synchronized boolean cleanup(int level) {
		// TODO Auto-generated method stub
		return false;
	}

	public synchronized void walk(PaintContext pc, int opt) {
		// TODO Auto-generated method stub
	}
	public int getNameIdx(float lat, float lon, short type) {
		// only interesting for SingleTile	
		return -1;
	}
	
	public synchronized void paint(PaintContext pc, byte layer) {
		if (layer == Tile.LAYER_NODE) {
			if (contain(pc)) {
				if ((t1 != null) && (t2 != null)) {
					t1.paint(pc, layer);
					t2.paint(pc, layer);
				} else {
					paintLocal(pc);
				}
			}
		}
		// FIXME: should not be painted recursively
		if (wptOutlineLat != null) {
			paintWaypointsOutline(pc);
		}
	}

	private void paintWaypointsOutline(PaintContext pc) {
		// render waypoint outline
		if (wptOutlineLat.length > 1) {
			// FIXME: color should be in Legend from style-file
			pc.g.setColor(0x00FF8D6B);
			pc.g.setStrokeStyle(Graphics.SOLID);
			for (int i = 0; i < wptOutlineLat.length; i++) {
				pc.getP().forward(wptOutlineLat[i], wptOutlineLon[i], pc.lineP2);
				int x = pc.lineP2.x;
				int y = pc.lineP2.y;
				int idx = i + 1;
				if (idx == wptOutlineLat.length) {
					idx = 0;
				}
				pc.getP().forward(wptOutlineLat[idx], wptOutlineLon[idx], pc.lineP2);
				pc.g.drawLine(x, y, pc.lineP2.x, pc.lineP2.y);
			}
		}
	}

	public synchronized void dropWayPt() {
		totalWayPts = 0;
		wayPts.removeAllElements();
		if (t1 != null) {
			t1.dropWayPt();
			t2.dropWayPt();
		}
	}

	private void paintLocal(PaintContext pc) {
		/**
		 * Painting Waypoints
		 */
		if (wayPts.size() == 0)
		{
			return;
		}
		Font originalFont = pc.g.getFont();
		if (Configuration.getCfgBitState(Configuration.CFGBIT_WPTTEXTS))
		{
			pc.g.setColor(Legend.COLORS[Legend.COLOR_WAYPOINT_TEXT]);
			if (wptFont == null) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_WPT_LABELS_LARGER)) {
					wptFont = originalFont;
				} else {
					wptFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
				}
			}
			pc.g.setFont(wptFont);
		}
		int maxLen = Configuration.MAX_WAYPOINTNAME_DRAWLENGTH;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < wayPts.size(); i++) {
			PositionMark waypt = (PositionMark) (wayPts.elementAt(i));
			if (pc.getP().isPlotable(waypt.lat, waypt.lon)) {
				pc.getP().forward(waypt.lat, waypt.lon, pc.lineP2);
				// Always draw waypoint marker
				pc.g.drawImage(pc.images.IMG_WAYPT, pc.lineP2.x, pc.lineP2.y,
						Graphics.HCENTER | Graphics.VCENTER);
				// Draw waypoint text if enabled
				if (   (Configuration.getCfgBitState(Configuration.CFGBIT_WPTTEXTS)
					&& (waypt.displayName != null))) {
					if (waypt.displayName.length() > maxLen) {
						// Truncate name to maximum maxLen chars plus "..." where required.
						sb.setLength(0);
						sb.append(waypt.displayName.substring(0, maxLen))
						  .append("...");
						pc.g.drawString(sb.toString(), pc.lineP2.x, pc.lineP2.y,
										Graphics.HCENTER | Graphics.BOTTOM);
					}
					else
					{
						pc.g.drawString(waypt.displayName, pc.lineP2.x, pc.lineP2.y,
								Graphics.HCENTER | Graphics.BOTTOM);
 					}
 				}
			}
		} // for
		if (Configuration.getCfgBitState(Configuration.CFGBIT_WPTTEXTS))
		{
			pc.g.setFont(originalFont);
		}
	}

	private void splitTile() {
		logger.info("Trying to split tile containing " + wayPts.size()
				+ " waypoints");
		Random r = new Random();
		if (maxLat - minLat > maxLon - minLon) {
			splitDimension = true;
			splitCoord = ((PositionMark) wayPts.elementAt(r.nextInt(wayPts
					.size()))).lat;
		} else {
			splitDimension = false;
			splitCoord = ((PositionMark) wayPts.elementAt(r.nextInt(wayPts
					.size()))).lon;
		}
		/**
		 * Check to see that we reduce the number of waypoints / track points by
		 * at least 5, as otherwise we can get into an infinite recursion. This
		 * can happen for example if many points are recorded that have
		 * identical coordinates,
		 */
		int[] t1t2count = new int[2];
		for (int i = 0; i < wayPts.size(); i++) {
			t1t2count[t1t2WayPoint((PositionMark) (wayPts.elementAt(i))) ? 0
					: 1]++;
		}
		if (t1t2count[0] < 5 || t1t2count[1] < 5) {
			// #debug info
			logger
					.info("Split was unsuccessful, perhaps too many identical coordinates");
			// Vector will just keep adding elements.
			// Hopefully, the next split will succeed.
			return;
		}
		// #debug debug
		logger.debug("Splitting tile (" + splitDimension + ") " + splitCoord);
		t1 = new WaypointsTile();
		t2 = new WaypointsTile();
		for (int i = 0; i < wayPts.size(); i++) {
			if (t1t2WayPoint((PositionMark) (wayPts.elementAt(i)))) {
				t1.addWayPt((PositionMark) (wayPts.elementAt(i)));
			} else {
				t2.addWayPt((PositionMark) (wayPts.elementAt(i)));
			}
		}
		wayPts.removeAllElements();
	}

	public boolean existsWayPt(PositionMark newWayPt) {
		if ((t1 != null) && (t2 != null)) {
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
			for (int i = 0; i < wayPts.size(); i++) {
				PositionMark waypt = (PositionMark) (wayPts.elementAt(i));
				if (newWayPt.displayName.equals(waypt.displayName) &&
					MoreMath.approximately_equal(waypt.lat,newWayPt.lat,0.0000005f) &&
					MoreMath.approximately_equal(waypt.lon,newWayPt.lon,0.0000005f) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Determines whether the PositionMark has to be put in sub tile t1 or t2.
	 * It is only valid to call this method if there are sub tiles i.e. if this
	 * tile was split at all.
	 * 
	 * @param p
	 *            PositionMark to be checked
	 * @return true = p belongs in t1, false = p belongs in t2
	 */
	private boolean t1t2WayPoint(PositionMark p) {
		if (splitDimension) {
			if (p.lat < splitCoord) {
				return true;
			} else {
				return false;
			}
		} else {
			if (p.lon < splitCoord) {
				return true;
			} else {
				return false;
			}
		}
	}

	public static void useNewWptFont() {
		wptFont = null;
	}
}
