/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;

import de.ueller.midlet.gps.ScreenContext;
import de.ueller.midlet.gps.Trace;

import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Way;

public class SingleTile extends Tile {

	private static final byte	STATE_NOTLOAD		= 0;

	private static final byte	STATE_LOADSTARED	= 1;

	private static final byte	STATE_LOADREADY		= 2;
	private static final byte	STATE_NAMESREADY		= 4;

	Node[]						nodes;

	Way[]						ways;

	byte						state				= 0;

	short							fileId				= 0;


//	private final static Logger	logger				= Logger.getInstance(SingleTile.class, Logger.TRACE);

	private final byte			zl;

	SingleTile(DataInputStream dis, int deep, byte zl) throws IOException {
		this.zl = zl;
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
		fileId = (short) dis.readInt();
//		logger.debug("" + deep + ":ST Nr=" + fileId);
	}
	
	private boolean isDataReady(ScreenContext pc){
		if (state == STATE_NOTLOAD) {
//			logger.debug("singleTile start load");
			state = STATE_LOADSTARED;
//			drawBounds(pc, 255, 55, 55);
			new DataReader(fileId, "", zl, this);
			return false;
		}
		if (state == STATE_LOADSTARED) {
			// logger.debug("singleTile wait for load");
//			drawBounds(pc, 255, 255, 55);
			return false;
		}
		return true;

	}

	public void paint(PaintContext pc) {
		if (contain(pc)) {
			if (! isDataReady(pc)){
				return;
			}
			lastUse = 0;
			if (ways != null) {
				for (int i = 0; i < ways.length; i++) {
					Way w = ways[i];
					// logger.debug("test Bounds of way");
					if (w.maxLat < pc.screenLD.radlat) {
						continue;
					}
					if (w.maxLon < pc.screenLD.radlon) {
						continue;
					}
					if (w.minLat > pc.screenRU.radlat) {
						continue;
					}
					if (w.minLon > pc.screenRU.radlon) {
						continue;
					}
//					logger.debug("draw " + w.name);
					w.setColor(pc);
					if (w.type < 50){
						w.paintAsPath(pc, nodes);
					} else {
						w.paintAsArea(pc, nodes);
					}
				}
			}
			for (short i = 0; i < nodes.length; i++) {
				Node node = nodes[i];
				if (node.type == 0) {
					break;
				}
				if (node.radlat < pc.screenLD.radlat) {
					continue;
				}
				if (node.radlon < pc.screenLD.radlon) {
					continue;
				}
				if (node.radlat > pc.screenRU.radlat) {
					continue;
				}
				if (node.radlon > pc.screenRU.radlon) {
					continue;
				}
				node.paint(pc);
			}
		} else {
			cleanup();
		}
}

	public void cleanup() {
		if (state == STATE_LOADREADY) {
			// logger.info("tile unused " + fileId);
			lastUse++;
			if (lastUse > 4) {
				nodes = null;
				ways = null;
				state = STATE_NOTLOAD;
//				logger.info("discard content for tile " + fileId);
			}
		}
	}

	public void dataReady() {
		state = STATE_LOADREADY;
	}

	public static Trace getTrace() {
		return trace;
	}

	public static void setTrace(Trace trace) {
		Tile.trace = trace;
	}

}
