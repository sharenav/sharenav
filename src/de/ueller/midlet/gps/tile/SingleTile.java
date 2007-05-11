/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.midlet.gps.Logger;

import de.ueller.midlet.gps.data.Way;

public class SingleTile extends Tile implements QueueableTile {

	public static final byte STATE_NOTLOAD = 0;

	public static final byte STATE_LOADSTARED = 1;

	public static final byte STATE_LOADREADY = 2;

	// Node[] nodes;
	public float[] nodeLat;

	public float[] nodeLon;

	public Short[] nameIdx;

	public byte[] type;

	Way[] ways;

	byte state = 0;


//	 private final static Logger logger= Logger.getInstance(SingleTile.class,
//	 Logger.ERROR);

	public final byte zl;

	SingleTile(DataInputStream dis, int deep, byte zl) throws IOException {
//		 logger.debug("load " + deep + ":ST Nr=" + fileId);
		this.zl = zl;
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
		fileId = (short) dis.readInt();
	
//		 logger.debug("ready " + deep + ":ST Nr=" + fileId);
	}

	private boolean isDataReady(PaintContext pc) {
		if (state == STATE_NOTLOAD) {
			// logger.debug("singleTile start load " + fileId );
			state = STATE_LOADSTARED;
			// drawBounds(pc, 255, 55, 55);
			pc.dataReader.add(this);
			return false;
		}
		if (state == STATE_LOADSTARED) {
			// logger.debug("singleTile wait for load " + fileId);
			// drawBounds(pc, 255, 255, 55);
			return false;
		}
//		switch (zl){
//		case 0: drawBounds(pc, 255, 0, 0); break;
//		case 1: drawBounds(pc, 0, 255, 0); break;
//		case 2: drawBounds(pc, 0, 0, 255); break;
//		case 3: drawBounds(pc, 255, 255, 0); break;
//		case 4: drawBounds(pc, 0, 255, 255); break;
//		}
		return true;

	}

	public void paint(PaintContext pc) {
//		logger.info("paint Single");
		if (contain(pc)) {
			if (!isDataReady(pc)) {
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
					// logger.debug("draw " + w.name);
					w.setColor(pc);
					if (w.type < 50) {
						// w.paintAsPath(pc, nodes);
						w.paintAsPath(pc, this);
					} else {
						// w.paintAsArea(pc, nodes);
						w.paintAsArea(pc, this);
					}
				}
			}
			for (short i = 0; i < type.length; i++) {
				if (type[i] == 0) {
					break;
				}
				if (nodeLat[i] < pc.screenLD.radlat) {
					continue;
				}
				if (nodeLon[i] < pc.screenLD.radlon) {
					continue;
				}
				if (nodeLat[i] > pc.screenRU.radlat) {
					continue;
				}
				if (nodeLon[i] > pc.screenRU.radlon) {
					continue;
				}
				paintNode(pc, i);
			}
		} else {

		}
	}

	public boolean cleanup(int level) {
		if (state != STATE_NOTLOAD ) {
			// logger.info("test tile unused fid:" + fileId + "c:"+lastUse);
			if (lastUse > level) {
				// nodes = null;
				nameIdx = null;
				nodeLat = null;
				nodeLon = null;
				type = null;
				ways = null;
				state = STATE_NOTLOAD;
				// logger.info("discard content for tile " + fileId);
				return true;
			}
		}
		return false;
	}

	public void dataReady() {
		lastUse = -1;
		state = STATE_LOADREADY;
	}

//	public static Trace getTrace() {
//		return trace;
//	}
//
//	public static void setTrace(Trace trace) {
//		Tile.trace = trace;
//	}

	public void paintNode(PaintContext pc, int i) {
		Image img = null;
		// logger.debug("set color "+pc);
		// if (node.name == null) continue;
		byte t=type[i];
		switch (t) {
		case 1:
			pc.g.setColor(255, 50, 50);
			break;
		case 2:
			pc.g.setColor(200, 100, 100);
			break;
		case 3:
			pc.g.setColor(180, 180, 50);
			break;
		case 4:
			pc.g.setColor(160, 160, 90);
			break;
		case 5:
			pc.g.setColor(0, 0, 0);
			break;
		case 6:
			pc.g.setColor(0, 0, 0);
			break;
		case C.NODE_AMENITY_PARKING:
			img = pc.images.IMG_PARKING;
			break;
		case C.NODE_AMENITY_TELEPHONE:
			img = pc.images.IMG_TELEPHONE;
			break;
		case C.NODE_AMENITY_SCHOOL:
			img = pc.images.IMG_SCHOOL;
			break;
		case C.NODE_AMENITY_FUEL:
			img = pc.images.IMG_FUEL;
			break;
		case C.NODE_RAILWAY_STATION:
			img = pc.images.IMG_RAILSTATION;
			break;

		}
		// logger.debug("calc pos "+pc);
		pc.p.forward(nodeLat[i], nodeLon[i], pc.swapLineP, true);
		if (img != null) {
			// logger.debug("draw img " + img);
			if (nameIdx[i] == null || t > 99) {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.VCENTER | Graphics.HCENTER);
			} else {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.BOTTOM | Graphics.HCENTER);
			}
		}
		if (nameIdx != null) {
			// logger.debug("draw txt " + );
			String name = pc.trace.getName(nameIdx[i]);
			if (name != null) {
				pc.g.setColor(0, 0, 0);
				if (img == null) {
					pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y,
							Graphics.BASELINE | Graphics.HCENTER);
				} else {
					if (t > 99){
						pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y+8,
								Graphics.TOP | Graphics.HCENTER);						
					} else {
						pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y,
							Graphics.TOP | Graphics.HCENTER);
					}
				}
			}
		}
	}

	public String toString() {
		return "ST" + zl + "-" + fileId+ ":" + lastUse;
	}
}
