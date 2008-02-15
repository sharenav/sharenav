/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net
 * See Copying
 */
package de.ueller.gpsMid.mapData;

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.Logger;

import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.QueueableTile;

public class SingleTile extends Tile implements QueueableTile {

	public static final byte STATE_NOTLOAD = 0;

	public static final byte STATE_LOADSTARED = 1;

	public static final byte STATE_LOADREADY = 2;

	// Node[] nodes;
	public float[] nodeLat;

	public float[] nodeLon;

	public int[] nameIdx;

	public byte[] type;

	Way[] ways;

	byte state = 0;
	
	boolean abortPainting = false;


//	 private final static Logger logger= Logger.getInstance(SingleTile.class,
//	 Logger.DEBUG);

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

	public synchronized void paint(PaintContext pc, boolean area) {
//		logger.info("paint Single");
		float testLat;
		float testLon;
		if (contain(pc)) {
			if (!isDataReady(pc)) {
				return;
			}
			lastUse = 0;
			if (ways != null) {
				for (int i = 0; i < ways.length; i++) {
					if (abortPainting)
						return;
					Way w = ways[i];
					if (w == null) continue;
					//Determin if the way is an area or not. 
					if (!((w.type < 50 && area == false) || (w.type >= 50 && area ==  true)))
						continue;

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
					// fill the target fields if they are empty
//					logger.debug("search target" + pc.target);
					if (pc.target != null && pc.target.st == null){
//						logger.debug("search target nameIdx" );
						if (pc.target.e == null && pc.target.nameIdx == w.nameIdx){
//							logger.debug("search target way");
							for (int p1 = 0; p1 < w.paths.length; p1++) {
								short[] path = w.paths[p1];
								for (int i1 = 0; i1 < path.length; i1++) {
									short s = path[i1];
									if (nodeLat[s] == pc.target.lat &&
											nodeLon[s] == pc.target.lon){
//										logger.debug("found Target way");
										pc.target.st=this;
										pc.target.e=w;
									}
								}
							}
						}
					}
					w.setColor(pc);
					if (!area) {
						if (pc.config.getRender() == Configuration.RENDER_LINE){
						    w.paintAsPath(pc, this);
						} else {
							float witdh = (pc.ppm*w.getWidth()/2);
							w.paintAsPath(pc,(int)(witdh+0.5), this);
						}
					} else {
						w.paintAsArea(pc, this);
					}
				}
			}
			for (short i = 0; i < type.length; i++) {
				if (abortPainting)
					return;
				if (type[i] == 0) {
					break;
				}
				testLat=nodeLat[i];
				if (testLat < pc.screenLD.radlat) {
					continue;
				}
				if (testLat > pc.screenRU.radlat) {
					continue;
				}
				testLon=nodeLon[i];
				if (testLon < pc.screenLD.radlon) {
					continue;
				}
				if (testLon > pc.screenRU.radlon) {
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
				abortPainting = true;
				synchronized(this) {
					// nodes = null;
					nameIdx = null;
					nodeLat = null;
					nodeLon = null;
					type = null;
					ways = null;
					state = STATE_NOTLOAD;
				}
				abortPainting = false;
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
		case C.NODE_PLACE_CITY:
			pc.g.setColor(255, 50, 50);
			break;
		case C.NODE_PLACE_TOWN:
			pc.g.setColor(200, 100, 100);
			break;
		case C.NODE_PLACE_VILLAGE:
			pc.g.setColor(180, 180, 50);
			break;
		case C.NODE_PLACE_HAMLET:
			pc.g.setColor(160, 160, 90);
			break;
		case C.NODE_PLACE_SUBURB:
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
		case C.NODE_AEROWAY_AERODROME:
			img = pc.images.IMG_AERODROME;
			break;

		}
		// logger.debug("calc pos "+pc);
		pc.getP().forward(nodeLat[i], nodeLon[i], pc.swapLineP, true);
		if (img != null) {
			// logger.debug("draw img " + img);
			if (nameIdx[i] == -1 || t > 99) {
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

/*	public void getWay(PaintContext pc, PositionMark pm, Way bestWay) {
		if (contain(pm)) {
			if (state != STATE_LOADREADY){
				try {
					pc.dataReader.readData(this);
					state=STATE_LOADREADY;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return;
				}
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
					for (int p1 = 0; p1 < w.paths.length; p1++) {
						// read the name only if is used more memory efficicent
						// pc.trace.getName(nameIdx);
						short[] path = w.paths[p1];
						short pidx = path[0];
						for (int i1 = 1; i1 < path.length; i1++) {
							int idx = path[i1];
							float dist=MoreMath.ptSegDistSq(nodeLat[pidx] ,nodeLon[pidx], nodeLat[idx], nodeLon[idx], pm.lat, pm.lon);
							if (dist < pc.squareDstToWay){
								pc.squareDstToWay=dist;
								bestWay=w;
								pm.e=w;
								pm.st=this;
							}
						}
					}

				}
			}
		}


	}*/

	
	public void paint(PaintContext pc) {
		paint(pc,true);
		paint(pc,false);		
	}
	
	public void paintNonArea(PaintContext pc) {
		paint(pc,false);		
	}

	public void paintAreaOnly(PaintContext pc) {
		paint(pc,true);		
	}
}
