/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net
 * See Copying
 */
package de.ueller.gpsMid.mapData;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.SearchResult;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.QueueableTile;

public class SingleTile extends Tile implements QueueableTile {

	public static final byte STATE_NOTLOAD = 0;

	public static final byte STATE_LOADSTARTED = 1;

	public static final byte STATE_LOADREADY = 2;

	private static final byte STATE_CLEANUP = 3;

	/**
	 * fpm is the fixed point multiplier used to convert
	 * latitude / logitude from radians to fixpoint representation
	 * 
	 * With this multiplier, one should get a resolution
	 * of 1m at the equator.
	 * 
	 * 6378159.81 = circumference of the earth in meters / 2 pi. 
	 * 
	 * This constant has to be in synchrony with the value in Osm2GpsMid
	 */	
	public static final float fpm = 6378159.81f;
    public static final float fpminv = 1/fpm; //Saves a floatingpoint devision

	// Node[] nodes;
	public short[] nodeLat;

	public short[] nodeLon;

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

	private boolean isDataReady() {
		if (state == STATE_NOTLOAD) {
			// logger.debug("singleTile start load " + fileId );
			state = STATE_LOADSTARTED;						
			Trace.getInstance().getDataReader().add(this,this);
			return false;
		}
		if (state == STATE_LOADSTARTED) {
			// logger.debug("singleTile wait for load " + fileId);
			// drawBounds(pc, 255, 255, 55);
			return false;
		}
		if (state == STATE_CLEANUP) {
			// logger.debug("singleTile wait for Cleanup " + fileId);			
			return false;
		}
		return true;

	}

	public synchronized void paint(PaintContext pc, boolean area) {
//		logger.info("paint Single");
		float testLat;
		float testLon;
		if (contain(pc)) {
			if (!isDataReady()) {
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
					if (!w.isOnScreen(pc, centerLat, centerLon)) continue; 
					// logger.debug("draw " + w.name);
					// fill the target fields if they are empty
//					logger.debug("search target" + pc.target);
					if (pc.target != null ){
//						logger.debug("search target nameIdx" );
						if (pc.target.e == null && pc.target.nameIdx == w.nameIdx){
//							logger.debug("search target way");
							for (int i1 = 0; i1 < w.path.length; i1++) {
								short s = w.path[i1];
								if ((nodeLat[s] + centerLat)*fpminv == pc.target.lat && 
										(nodeLon[s]  + centerLon)*fpminv == pc.target.lon){
//									logger.debug("found Target way");
									pc.target.setEntity(w, getFloatNodes(nodeLat,centerLat), getFloatNodes(nodeLon,centerLon));
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
				testLat=(float)(nodeLat[i]*fpminv + centerLat); 
				if (testLat < pc.screenLD.radlat) {
					continue;
				}
				if (testLat > pc.screenRU.radlat) {
					continue;
				}
				testLon=(float)(nodeLon[i]*fpminv + centerLon); 
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

	public void walk(PaintContext pc,int opt) {
		float testLat;
		float testLon;
		if (contain(pc)) {
			while (!isDataReady()) {
				if ((opt & Tile.OPT_WAIT_FOR_LOAD) == 0){
					return;
				} else {
					synchronized (this) {
						try {
							wait(100);
						} catch (InterruptedException e) {
						}						
					}
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
					// logger.debug("draw " + w.name);
					// fill the target fields if they are empty
//					logger.debug("search target" + pc.target);
					if (pc.target != null ){
//						logger.debug("search target nameIdx" );
						if (pc.target.e == null && pc.target.nameIdx == w.nameIdx){
//							logger.debug("search target way");
							for (int i1 = 0; i1 < w.path.length; i1++) {
								short s = w.path[i1];
								if (nodeLat[s] == pc.target.lat &&
										nodeLon[s] == pc.target.lon){
//									logger.debug("found Target way");										
									pc.target.setEntity(w, getFloatNodes(nodeLat,centerLat), getFloatNodes(nodeLon,centerLon));
								}
							}
						}
					}
					if ((opt & Tile.OPT_PAINT) != 0){
						w.setColor(pc);
						if (w.type < 50) {
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
			}
			if ((opt & Tile.OPT_PAINT) != 0){
				for (short i = 0; i < type.length; i++) {
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
			}
		}
	}

	public boolean cleanup(int level) {
		if (state != STATE_NOTLOAD ) {
			// logger.info("test tile unused fid:" + fileId + "c:"+lastUse);
			if (lastUse > level) {
				abortPainting = true;
				synchronized(this) {
				// nodes = null;
				state = STATE_CLEANUP;
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

	public void paintNode(PaintContext pc, int i) {
		Image img = null;
		byte t=type[i];

		pc.g.setColor(pc.c.getNodeTextColor(t));
		img = pc.c.getNodeImage(t);
		// logger.debug("calc pos "+pc);
		pc.getP().forward((float)((nodeLat[i]*fpminv + centerLat)), (float)((nodeLon[i]*fpminv + centerLon)), pc.swapLineP, true);
		if (pc.scale > pc.c.getNodeMaxScale(t)) {
			return;
		}
		if (img != null) {
			// logger.debug("draw img " + img);
			if (nameIdx[i] == -1 || pc.c.isNodeImageCentered(t) || pc.scale > pc.c.getNodeMaxTextScale(t)) {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.VCENTER | Graphics.HCENTER);
			} else {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.BOTTOM | Graphics.HCENTER);
			}
		}
		if (pc.scale > pc.c.getNodeMaxTextScale(t)) {
			return;
		}
		// logger.debug("draw txt " + );
		String name = pc.trace.getName(nameIdx[i]);
		if (name != null) {			
			if (img == null) {
				pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.BASELINE | Graphics.HCENTER);
			} else {
				if (pc.c.isNodeImageCentered(t)){
					pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y+8,
							Graphics.TOP | Graphics.HCENTER);						
				} else {
					pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y,
							Graphics.TOP | Graphics.HCENTER);
				}
			}
		}
		
	}

	public String toString() {
		return "ST" + zl + "-" + fileId+ ":" + lastUse;
	}

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
	
   private float[] getFloatNodes(short[] nodes, float offset) {
	    float [] res = new float[nodes.length];
	    for (int i = 0; i < nodes.length; i++) {
		res[i] = nodes[i]*fpminv + offset;
	    }
	    return res;
	}
   
   /**
    * Returns a Vector of SearchResult containing POIs of
    * type searchType close to lat/lon. The list is ordered
    * by distance with the closest one first.  
    */
   public Vector getNearestPoi(byte searchType, float lat, float lon, float maxDist) {	   
	   Vector resList = new Vector();
	   
	   if (!isDataReady()) {		   
		   synchronized(this) {
			   try {
				   /**
				    * Wait for the tile to be loaded in order to process it
				    * We should be notified once the data is loaded, but
				    * have a timeout of 500ms
				    */
				   wait(500);
			   } catch (InterruptedException e) {
				   /**
				    * Nothing to do in this case, as we need to deal
				    * with the case that nothing has been returned anyway
				    */
			   }			   
		   }
	   }
	   /**
	    * Try again and see if it has been loaded by now
	    * If not, then give up and skip this tile in order
	    * not to slow down surch too much
	    */
	   if (!isDataReady()) {		   
		   return new Vector();
	   }
	   
	   for (int i = 0; i < type.length; i++) {
		   if (type[i] == searchType) {
			   SearchResult sr = new SearchResult();
			   sr.lat = nodeLat[i]*fpminv + centerLat;
			   sr.lon = nodeLon[i]*fpminv + centerLon;
			   sr.nameIdx = nameIdx[i];
			   sr.type = (byte)(-1 * searchType); //It is a node. They have the top bit set to distinguish them from ways in search results
			   sr.dist = ProjMath.getDistance(sr.lat, sr.lon, lat, lon);
			   if (sr.dist < maxDist) {
				   resList.addElement(sr);				   
			   }
		   }
	   }
	   /**
	    * Perform a bubble sort on the distances of the search
	    * This is stupidly inefficient, but easy to code.
	    * Also we expect there only to be very few entries in
	    * the list, so shouldn't harm too much. 
	    */
	   boolean isSorted = false;
	   while(!isSorted) {
		   isSorted = true;
		   for (int i = 0; i < resList.size() - 1; i++) {
			   SearchResult a = (SearchResult) resList.elementAt(i);
			   SearchResult b = (SearchResult) resList.elementAt(i + 1);
			   if (a.dist > b.dist) {
				   resList.setElementAt(a, i + 1);
				   resList.setElementAt(b, i);
				   isSorted = false;
			   }
		   }
	   }
	   return resList;
   }
	
}
