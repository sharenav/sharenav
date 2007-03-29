package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import net.sourceforge.jmicropolygon.PolygonGraphics;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Projection;
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

	public void paint(PaintContext pc) {
		try {
			if (contain(pc)) {
				lastUse = 0;
				if (state == STATE_NOTLOAD) {
//					logger.debug("singleTile start load");
					state = STATE_LOADSTARED;
//					drawBounds(pc, 255, 55, 55);
					new DataReader(fileId, "", zl, this);
					return;
				}
				if (state == STATE_LOADSTARED) {
					// logger.debug("singleTile wait for load");
//					drawBounds(pc, 255, 255, 55);
					return;
				}
///				drawBounds(pc, 55, 255, 55);
//				if (ways != null)
//					logger.debug("paint tile " + zl + "-" + fileId + " with " + ways.length + " ways " + nodes.length
//							+ "nodes");
//				else
//					logger.debug("paint tile " + zl + "-" + fileId + " with " + nodes.length + "nodes");
				Projection p = pc.p;
				Graphics g = pc.g;
				if (ways != null) {
					for (int i = 0; i < ways.length; i++) {
						Way w = (Way) ways[i];
						// logger.debug("test Bounds of way");
						if (w.maxLat < pc.screenLD.radlat) continue;
						if (w.maxLon < pc.screenLD.radlon) continue;
						if (w.minLat > pc.screenRU.radlat) continue;
						if (w.minLon > pc.screenRU.radlon) continue;
//						logger.debug("draw " + w.name);
						switch (w.type) {
							case C.WAY_HIGHWAY_MOTORWAY:
								g.setColor(100, 100, 255);
								break;
							case C.WAY_HIGHWAY_TRUNK:
								g.setColor(255,150,150);
							case C.WAY_HIGHWAY_PRIMARY:
								g.setColor(255, 100, 100);
								break;
							case C.WAY_HIGHWAY_SECONDARY:
								g.setColor(255, 200, 60);
								break;
							case C.WAY_HIGHWAY_MINOR:
								g.setColor(255, 255, 150);
								break;
							case C.WAY_HIGHWAY_RESIDENTIAL:
								g.setColor(180, 180, 180);
								break;
							case C.AREA_AMENITY_PARKING:
								g.setColor(255,255,150);
								break;
							case C.AREA_NATURAL_WATER:
								g.setColor(255,255,150);
								break;
							default:
//								logger.error("unknown Type "+ w.type);
								g.setColor(0, 0, 0);
						}
						IntPoint lineP1 = pc.lineP1;
						IntPoint lineP2 = pc.lineP2;
						IntPoint swapLineP = pc.swapLineP;
						if (w.type < 50){
							for (int p1 = 0; p1 < w.paths.length; p1++) {
								short[] path = w.paths[p1];
								for (int i1 = 0; i1 < path.length; i1++) {
									Node node = nodes[path[i1]];
									if (node != null) {
										p.forward(node.radlat, node.radlon, lineP2, true);
										if (lineP1 == null) {
											lineP1 = lineP2;
											lineP2 = swapLineP;
										} else {
											g.drawLine(lineP1.x, lineP1.y, lineP2.x, lineP2.y);
											swapLineP = lineP1;
											lineP1 = lineP2;
											lineP2 = swapLineP;
										}
									}
								}
								swapLineP = lineP1;
								lineP1 = null;
							}
						} else {
							for (int p1 = 0; p1 < w.paths.length; p1++) {
								short[] path = w.paths[p1];
								int[] x=new int[path.length];
								int[] y=new int[path.length];
								for (int i1 = 0; i1 < path.length; i1++) {
									Node node = nodes[path[i1]];
									if (node != null) {
										p.forward(node.radlat, node.radlon, lineP2, true);
										x[i1]=lineP2.x;
										y[i1]=lineP2.y;
									}
								}
//								x[path.length+1]=x[0];
//								y[path.length+1]=y[0];
//								logger.info("draw polygon");
								PolygonGraphics.drawPolygon(g, x, y);
								PolygonGraphics.fillPolygon(g, x, y);
							}
						}
					}
				}
				for (short i = 0; i < nodes.length; i++) {
					Node node = nodes[i];
					if (node.type == 0) break;
					Image img=null;
					//if (node.name == null) continue;
					switch (node.type) {
						case 1:
							g.setColor(255, 50, 50);
							break;
						case 2:
							g.setColor(200, 100, 100);
							break;
						case 3:
							g.setColor(180, 180, 50);
							break;
						case 4:
							g.setColor(160, 160, 90);
							break;
						case 5:
							g.setColor(0, 0, 0);
							break;
						case 6:
							g.setColor(0, 0, 0);
							break;
						case C.NODE_AMENITY_PARKING:
							img=pc.IMG_PARKING;
							break;

					}
					p.forward(node.radlat, node.radlon, pc.swapLineP, true);
					if  (img != null){
						g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y, Graphics.HCENTER | Graphics.VCENTER);
					}
					if (node.nameIdx != null){
						String name=pc.trace.getName(node.nameIdx);
						if (name != null){
							g.drawString(name, pc.swapLineP.x, pc.swapLineP.y, Graphics.BASELINE | Graphics.HCENTER);
						}
					}

				}
			} else {
				cleanup();
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		SingleTile.trace = trace;
	}

}
