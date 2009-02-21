package de.ueller.gpsMid.mapData;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 *          Copyright (c) 2008 Markus Baeurle mbaeurle at users dot sourceforge dot net
 * See Copying
 */

import java.util.Random;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.tile.PaintContext;

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
			totalWayPts++;

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
			if (wayPts.size() > 90) {
				splitTile();
			}
		}
	}

	public synchronized PositionMark[] listWayPt() {
		if ((t1 != null) && (t2 != null)) {
			PositionMark[] wayPts1 = t1.listWayPt();
			PositionMark[] wayPts2 = t2.listWayPt();
			PositionMark[] wayPtsAll = new PositionMark[wayPts1.length
					+ wayPts2.length];
			System.arraycopy(wayPts1, 0, wayPtsAll, 0, wayPts1.length);
			System.arraycopy(wayPts2, 0, wayPtsAll, wayPts1.length,
					wayPts2.length);
			return wayPtsAll;
		} else {
			PositionMark[] wayPtsAll = new PositionMark[wayPts.size()];
			wayPts.copyInto(wayPtsAll);
			return wayPtsAll;
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
			pc.g.setColor(0, 0, 0);
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
				pc.g.drawImage(pc.images.IMG_MARK, pc.lineP2.x, pc.lineP2.y,
						Graphics.HCENTER | Graphics.VCENTER);
				// Draw waypoint text if enabled
				if (   (Configuration.getCfgBitState(Configuration.CFGBIT_WPTTEXTS) 
					&& (waypt.displayName != null))) {
					if (waypt.displayName.length() > maxLen) {						
						// Truncate name to maximum maxLen chars plus "..." where required.
						sb.setLength(0);
						sb.append(waypt.displayName.substring(0, maxLen));
						sb.append("...");
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
				if (newWayPt.lat == waypt.lat && newWayPt.lon == waypt.lon
						&& newWayPt.displayName.equals(waypt.displayName)) {
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
