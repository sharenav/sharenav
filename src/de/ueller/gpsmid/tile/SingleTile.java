/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net
 * See file COPYING
 */

package de.ueller.gpsmid.tile;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.data.SearchResult;
import de.ueller.gpsmid.graphics.Proj3D;
import de.ueller.gpsmid.graphics.Projection;
import de.ueller.gpsmid.mapdata.Way;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.midlet.util.ImageTools;
import de.ueller.util.CancelMonitorInterface;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import de.ueller.util.ProjMath;

import de.enough.polish.util.Locale;


/**
 * In general a SingleTile is a container that holds all graphical information for an rectangular array out of the world.
 * Single tiles are overlaping each other in the most cases, because the hold complete ways or arrays.
 * The data structure for the data file is:
 * 0x54 > indicates that this is a SingleTile file
 * 2 floats for the center in lat/lon
 * int nodeCount > count off all node in this file
 * int iNodeCount > count off interestNodes (POI) in this file 
 * nodeCount Nodes (first INodeCount nodes have i.e. a name) the rest up to nodeCount are only 2 coordinates
 * 0x55 >  indicate start of ways (to be able to detect structure errors)
 * short wayCount > count of ways and areas
 * wayCount ways/areas with different details
 * 0x56 > indicate end of tile (to be able to detect structure errors)
 * 
 * @author hmu
 *
 */
public class SingleTile extends Tile implements QueueableTile {

	public static final byte STATE_NOTLOAD = 0;

	public static final byte STATE_LOADSTARTED = 1;

	public static final byte STATE_LOADREADY = 2;

	private static final byte STATE_CLEANUP = 3;

    
	// Node[] nodes;
	public short[] nodeLat;

	public short[] nodeLon;

	public int[] nameIdx;

	public int[] urlIdx;

	public int[] phoneIdx;

	//#if polish.api.osm-editing
	public int[] osmID;
	//#endif
	//#if polish.api.bigstyles
	public short[] type;
	//#else
	public byte[] type;
	//#endif

	private Way[][] ways;

	byte state = 0;
	
	boolean abortPainting = false;

	private static Font poiFont;
	
	private final static Logger logger = Logger.getInstance(SingleTile.class, Logger.DEBUG);

	public final byte zl;
	

	public SingleTile(DataInputStream dis, int deep, byte zl) throws IOException {
//		 logger.debug("load " + deep + ":ST Nr=" + fileId);
		this.zl = zl;
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
		fileId = dis.readInt();
	
//		 logger.debug("ready " + deep + ":ST Nr=" + fileId);
	}

	public SingleTile() throws IOException {
		this.zl = 0;
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

	public void walk(PaintContext pc,int opt) {
		walk(pc, opt, Tile.LAYER_ALL);
	}
	
	public int getNameIdx(float lat, float lon, short type) {
		if (contain(lat, lon, 0.00001f)) {
			while (!isDataReady()) {
				synchronized (this) {
					try {
						wait(100);
						//#debug debug
						logger.debug("Walk: wait for data");
					} catch (InterruptedException e) {
					}						
				}
			}
			lastUse = 0;
			for (int i=0; i < nameIdx.length; i++) {
				if (MoreMath.approximately_equal(lat, nodeLat[i] * MoreMath.FIXPT_MULT_INV + centerLat,0.0000005f) &&
					MoreMath.approximately_equal(lon, nodeLon[i] * MoreMath.FIXPT_MULT_INV + centerLon,0.0000005f)
				) {
					if (type == -1 || type == this.type[i]) {
						return nameIdx[i];						
					}
				}
			}
		}
		return -1;
	}
	
	public void paint(PaintContext pc, byte layer) {
		walk(pc, Tile.OPT_PAINT, layer);
	}
	
	private synchronized void walk(PaintContext pc,int opt, byte layer) {
		boolean renderArea = ((layer & Tile.LAYER_AREA) != 0) || ((opt & Tile.OPT_CONNECTIONS2AREA) != 0);
		boolean renderAll = ((layer & Tile.LAYER_ALL) != 0);
		boolean renderHighlight = ((layer & Tile.LAYER_HIGHLIGHT) != 0);
		byte relLayer = (byte)(((int)layer) & ~(Tile.LAYER_AREA | Tile.LAYER_HIGHLIGHT));
		
		if (pc.getP() == null) {
			logger.error(Locale.get("singletile.NoProj")/*We do not have a projection to walk Tile*/);
			return;
		}
		
		if (contain(pc)) {
			while (!isDataReady()) {
				if ((opt & Tile.OPT_WAIT_FOR_LOAD) == 0){
					/**
					 * We don't have the data yet. No need to wait, we 
					 * will just render it the next time if the data is
					 * available then.
					 */
					//#debug debug
					logger.debug("Walk don't wait for TileData");
					return;
				} else {
					synchronized (this) {
						try {
							wait(1000);
							//#debug debug
							logger.debug("Walk Wait for TileData");
						} catch (InterruptedException e) {
						}
					}
				}
			}
			
			/**
			 * Calculate paint context coordinates in terms of relative single tile coordinates
			 */
			float pcLDlatF = ((pc.getP().getMinLat() - this.centerLat) * MoreMath.FIXPT_MULT);
			float pcLDlonF = ((pc.getP().getMinLon() - this.centerLon) * MoreMath.FIXPT_MULT);
			float pcRUlatF = ((pc.getP().getMaxLat() - this.centerLat) * MoreMath.FIXPT_MULT);
			float pcRUlonF = ((pc.getP().getMaxLon() - this.centerLon) * MoreMath.FIXPT_MULT);
			short pcLDlat;
			short pcLDlon;
			short pcRUlat;
			short pcRUlon;
			
			if (pcLDlatF > Short.MAX_VALUE || pcLDlatF < Short.MIN_VALUE) {
				if (pcLDlatF > Short.MAX_VALUE) 
					pcLDlat = Short.MAX_VALUE;
				else
					pcLDlat = Short.MIN_VALUE;
			} else {
				pcLDlat = (short)pcLDlatF;
			}
			
			if (pcRUlatF > Short.MAX_VALUE || pcRUlatF < Short.MIN_VALUE) {
				if (pcRUlatF > Short.MAX_VALUE) 
					pcRUlat = Short.MAX_VALUE;
				else
					pcRUlat = Short.MIN_VALUE;
			} else {
				pcRUlat = (short)pcRUlatF;
			}
			if (pcLDlonF > Short.MAX_VALUE || pcLDlonF < Short.MIN_VALUE) {
				if (pcLDlonF > Short.MAX_VALUE) 
					pcLDlon = Short.MAX_VALUE;
				else
					pcLDlon = Short.MIN_VALUE;
			} else {
				pcLDlon = (short)pcLDlonF;
			}
			if (pcRUlonF > Short.MAX_VALUE || pcRUlonF < Short.MIN_VALUE) {
				if (pcRUlonF > Short.MAX_VALUE) 
					pcRUlon = Short.MAX_VALUE;
				else
					pcRUlon = Short.MIN_VALUE;
			} else {
				pcRUlon = (short)pcRUlonF;
			}

			lastUse = 0;
			if (layer != Tile.LAYER_NODE) {
				if (getWays() == null) {
					return;
				}

				// are we zoomed out to tile level 0 ?
				boolean zoomedOutFar = (Legend.scaleToTile((int) pc.scale) == 0);
				
				for (int l = 0; l < getWays().length; l++) {
					if (((relLayer != l) && !renderAll) || (getWays()[l] == null)) {
						continue;
					}

					/**
					 * Render all ways in the appropriate layer
					 */
					for (int i = 0; i < getWays()[l].length; i++) {
						if (abortPainting)
							return;
						Way w = getWays()[l][i];
						if (w == null) continue;
						//Determine if the way is an area or not. 
						if (w.isArea() != renderArea)
							continue;

						// logger.debug("test Bounds of way");
						if (!w.isOnScreen(pcLDlat, pcLDlon, pcRUlat, pcRUlon)) continue; 
						
						// skip rendering small areas when zoomed out to tile level 0 and zoom level is small
						if (zoomedOutFar && w.isArea() && (!((w.isRatherBig() && pc.scale < 1100000 ) || w.isEvenBigger()))) {
							// if (w.nameIdx != -1) System.out.println("Skip rendering " + w.toString());
							continue;
						}
												
						/**
						 * In addition to rendering we also check for which way
						 * corresponds to the destination set in the paint context identified
						 * by the name of the way and the coordinates of a node on the way
						 */
						if (pc.dest != null ) {
							// logger.debug("search dest nameIdx" );
							if (pc.dest.entity == null && pc.dest.nameIdx == w.nameIdx) {
								// logger.debug("search dest way");
								/**
								 * The name of the way and the dest matches, now we
								 * check if the coordinates match.
								 * 
								 * We have to be careful here, to not get into trouble
								 * with the 32bit float to 16bit short conversion.
								 * To prevent rounding issues, test for approximate
								 * equality
								 */
								short destLat = (short)((pc.dest.lat - centerLat) * MoreMath.FIXPT_MULT);
								short destLon = (short)((pc.dest.lon - centerLon) * MoreMath.FIXPT_MULT);
								for (int i1 = 0; i1 < w.path.length; i1++) {
									int s = w.path[i1];
									if (s < 0) {
										s += 65536;
									}
									if ((Math.abs(nodeLat[s] - destLat) < 2) && 
											(Math.abs(nodeLon[s] - destLon) < 2)) {
										// logger.debug("found dest way");
										pc.dest.setEntity(w, w.getNodesLatLon(this, true), w.getNodesLatLon(this, false));
									}
								}
							}
						}
						if ((opt & Tile.OPT_PAINT) != 0){
							pc.waysPainted++;
							if (!w.isArea()) {
								w.setColor(pc);
								if (renderHighlight) {									
									if (pc.highlightedPathOnTop) {
										w.paintHighlightPath(pc, this, (byte) 0);
									}
								} else {
									w.paintAsPath(pc, this, relLayer);
									if (!pc.highlightedPathOnTop) {
										w.paintHighlightPath(pc, this, (byte) 0);
									}
								}
								
							} else {
								w.paintAsArea(pc, this);
								// draw also border. condition? don't draw name in path.
								//w.setBorderColor(pc);
								//w.paintAsPath(pc, this, relLayer);
							}
						} else if ((opt & Tile.OPT_CONNECTIONS2WAY) != 0) {
							if (!w.isArea()) {
								w.connections2WayMatch(pc, this);
							}
						} else if ((opt & Tile.OPT_CONNECTIONS2AREA) != 0) {
							if (w.isArea()) {
								w.connections2AreaMatch(pc, this);
							}
						} else if ((opt & Tile.OPT_FIND_CURRENT) != 0) {
							if (!w.isArea()) {
								w.processPath(pc, this, Tile.OPT_FIND_CURRENT, (byte) 0);
							}
						}
					}
				}
			} else {
				/**
				 * Drawing nodes
				 */
				if ((opt & Tile.OPT_PAINT) != 0){
					for (short i = 0; i < type.length; i++) {
						if (abortPainting)
							return;
						if (type[i] == 0) {
							break;
						}
//						if (!isNodeOnScreen(i, pcLDlat, pcLDlon, pcRUlat, pcRUlon))
						if (! pc.getP().isPlotable(nodeLat[i] , nodeLon[i],this))
							continue;
						paintNode(pc, i);
					}
				}
			}
		}
	}

	public boolean cleanup(int level) {
		if (state != STATE_NOTLOAD ) {
			// logger.info("test tile unused fid:" + fileId + "c:"+lastUse);
			// Changed to >= because cleanup(0) to make it possible to clean
			// lastUse 0 tiles.
			if (lastUse >= level) { // previously: if (lastUse > level) {
				abortPainting = true;
				synchronized(this) {
				// nodes = null;
				state = STATE_CLEANUP;
				nameIdx = null;
				urlIdx = null;
				phoneIdx = null;
				nodeLat = null;
				nodeLon = null;
				type = null;
				setWays(null);
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
		//#if polish.api.bigstyles
		short t=type[i];
		//#else
		short t=(short) (type[i] & 0xff);
		//#endif
		boolean hideable = Legend.isNodeHideable(t);

		boolean alert = Legend.isNodeAlert(t);
		//#if polish.api.finland
		boolean cameraAlert = Legend.isCamera(t);
		if (cameraAlert && Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDCAMERA_ALERT)) {
			// FIXME: get and pass coordinates to keep track of distance
			// to camera
			pc.trace.setCameraAlert(t);
		}
		//#endif
		if (alert && Configuration.getCfgBitState(Configuration.CFGBIT_NODEALERT_VISUAL)) {
			// FIXME: get and pass coordinates to keep track of distance
			// to alert POI
			pc.trace.setNodeAlert(t);
		}
		
		// addition by sk750 until "byte om ="
		Projection projection = pc.getP();
		projection.forward(nodeLat[i], nodeLon[i], pc.swapLineP, this);
		float scale = pc.scale;
		if (projection instanceof Proj3D) {
			Proj3D p = (Proj3D) projection;
			scale = p.getScaleFor(pc.swapLineP.x, pc.swapLineP.y);
			//System.out.println("pc.scale: " + pc.scale + " 3D scale at line " + pc.swapLineP.y + ": " + scale);
		}

		byte om = Legend.getNodeOverviewMode(t);
		switch (om & Legend.OM_MODE_MASK) {
			case Legend.OM_SHOWNORMAL: 
				//System.out.println( pc.scale + " " + Legend.getNodeMaxScale(t)  + " " +Configuration.getDetailBoostMultiplierPOI());
				
				// FIXME check cleanup after the functionality is in good enough condition
				// if not in Overview Mode check for scale
				// original by Harald
				//if (pc.scale > Legend.getNodeMaxScale(t) * Configuration.getDetailBoostMultiplierPOI()) {
				// by sk750
				if (scale > Legend.getNodeMaxScale(t) * Configuration.getDetailBoostMultiplierPOI()) {
					return;
				}
				// disabling POIs does not disable PLACE TEXTs (city, suburb, etc.) 
				if (! (t >= Legend.MIN_PLACETYPE && t <= Legend.MAX_PLACETYPE)) {
					if (hideable & !Configuration.getCfgBitState(Configuration.CFGBIT_POIS)) {
						return;
					}
				}
				break;
			case Legend.OM_HIDE: 				
				if (hideable) {
					return;
				}
				break;
		}
		switch (om & Legend.OM_NAME_MASK) {
			case Legend.OM_WITH_NAMEPART: 
				if (nameIdx[i] == -1) return;
				String name = pc.trace.getName(nameIdx[i]);
				/* code for later inclusion of URL filtering
				if (Legend.enableUrlTags) {
				 String url = null;
				 if (urlIdx[i] != -1) {
				 	url = pc.trace.getUrl(urlIdx[i]);
				 }
				}
				*/
				if (name == null) return;
				if (name.toUpperCase().indexOf(Legend.get0Poi1Area2WayNamePart((byte) 0).toUpperCase()) == -1) return;
				break;
			case Legend.OM_WITH_NAME: 
				if (nameIdx[i] == -1) return;
				break;
			case Legend.OM_NO_NAME: 
				if (nameIdx[i] != -1) return;
				break;
		}
	
		pc.g.setColor(Legend.getNodeTextColor(t));
		img = Legend.getNodeImage(t);
		// logger.debug("calc pos "+pc);
		
		// deletion by sk750
		//pc.getP().forward(nodeLat[i], nodeLon[i], pc.swapLineP, this);
		
		if (img != null ) {
			//FIXME make optional if alert by growing image on map happens
			//#if polish.api.finland
			if (cameraAlert && Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDCAMERA_ALERT)) {
				img = ImageTools.scaleImage(img, img.getWidth() * 2, img.getHeight() * 2);
			}
			//#endif
			if (alert && Configuration.getCfgBitState(Configuration.CFGBIT_NODEALERT_VISUAL)) {
				img = ImageTools.scaleImage(img, img.getWidth() * 2, img.getHeight() * 2);
			}
			// && Legend.isNodeClickable(t)) would limit to only those specified in style file
			// && pc.trace.tl.bigOnScreenButtons would limit activity to only when single-clicked
			if (Configuration.getCfgBitState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS)) {
				// We don't really need url or phone for anything here
				// String url = null;
				//if (Legend.enableUrlTags && urlIdx[i] != -1) {
				//		url = pc.trace.getUrl(urlIdx[i]);
				//}
				//String phone = null;
				//if (Legend.enablePhoneTags && phoneIdx[i] != -1) {
				//		phone = pc.trace.getUrl(phoneIdx[i]);
				//}
				int nodeID = -1;
				//#if polish.api.bigsearch
				//#if polish.api.osm-editing
				if (Legend.enableEdits) {
					nodeID = osmID[i];
				}
				//#endif
				//#endif
				if (Legend.enableUrlTags && urlIdx[i] != -1 || Legend.enablePhoneTags && phoneIdx[i] != -1 || Legend.isNodeClickable(t)) {
					int dia = Configuration.getTouchMarkerDiameter();
					// FIXME create a specific color (semi-transparent would be good) for this
					pc.g.setColor(Legend.COLORS[Legend.COLOR_ROUTE_ROUTELINE]);
					pc.g.drawArc(pc.swapLineP.x - dia / 2, pc.swapLineP.y -
						     (Legend.isNodeImageCentered(t) ? dia / 2 : dia / 2 + img.getHeight() / 2 ), dia, dia, 0, 360);
					//System.out.println("url: " + url + " phone: " + phone);
					// don't get urls or phones yet
					pc.trace.addClickableMarker(pc.swapLineP.x, pc.swapLineP.y, urlIdx[i], phoneIdx[i], nodeID);
				}
			}
			// FIXME check and cleanup after the functionality is in good enough condition
			// logger.debug("draw img " + img);
			// orig by Harald
			// if (nameIdx[i] == -1 || Legend.isNodeImageCentered(t) || pc.scale > Legend.getNodeMaxTextScale(t)) {
			// by jkpj
			if (nameIdx[i] == -1 || Legend.isNodeImageCentered(t) || scale > Legend.getNodeMaxTextScale(t)) {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.VCENTER | Graphics.HCENTER);
			} else {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.BOTTOM | Graphics.HCENTER);
			}
		}
		// FIXME check cleanup after the functionality is in good enough condition
		// orig by Harald
		//if (pc.scale > Legend.getNodeMaxTextScale(t) * Configuration.getDetailBoostMultiplierPOI()) {
		// by sk750
		if (scale > Legend.getNodeMaxTextScale(t) * Configuration.getDetailBoostMultiplierPOI()) {
			return;
		}
		
		// PLACE TEXTS (from city to suburb)
		if (t >= Legend.MIN_PLACETYPE && t <= Legend.MAX_PLACETYPE) {
			if (!Configuration.getCfgBitState(Configuration.CFGBIT_PLACETEXTS)) {
				return;
			}
		// OTHER POI texts
		} else {
			if (hideable && !Configuration.getCfgBitState(Configuration.CFGBIT_POITEXTS) ) {
				return;
			}
		}

		
		// logger.debug("draw txt " + );
		String name;
		if (Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE)) {
			name = Legend.getNodeTypeDesc(t);
		}
		else {
			name = pc.trace.getName(nameIdx[i]);
		}
		if (name != null) {			
			Font originalFont = pc.g.getFont();
			if (poiFont==null) {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_POI_LABELS_LARGER)) {
					poiFont = originalFont;
				} else {
					poiFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
				}
			}
			pc.g.setFont(poiFont);

			if (img == null) {
				pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y,
						Graphics.BASELINE | Graphics.HCENTER);
			} else {
				if (Legend.isNodeImageCentered(t)){
					pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y+8,
							Graphics.TOP | Graphics.HCENTER);						
				} else {
					pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y,
							Graphics.TOP | Graphics.HCENTER);
				}
			}
			pc.g.setFont(originalFont);
		}
		
	}

	public String toString() {
		return "ST" + zl + "-" + fileId+ ":" + lastUse;
	}

/*   private float[] getFloatNodes(short[] nodes, float offset) {
	    float [] res = new float[nodes.length];
	    for (int i = 0; i < nodes.length; i++) {
		res[i] = nodes[i]*fpminv + offset;
	    }
	    return res;
	}
*/
   /**
    * Returns a Vector of SearchResult containing POIs of
    * type searchType close to lat/lon. The list is ordered
    * by distance with the closest one first.  
    */
   public Vector getNearestPoi(boolean matchAnyPoi, short searchType, float lat, float lon, float maxDist, CancelMonitorInterface cmi) {	   
	   Vector resList = new Vector();
	   
	   if(cmi != null) {
		   if (cmi.monitorIsCanceled()) {
			   return resList;
		   }
	   }
	   
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
	    * not to slow down search too much
	    */
	   if (!isDataReady()) {		   
		   return new Vector();
	   }
	   
	   for (int i = 0; i < type.length; i++) {
		   if ((!matchAnyPoi) && type[i] != searchType) {
			   continue;
		   }
		   if (matchAnyPoi) {
			   boolean match = false;
			   for (short j = 1; j < Legend.getMaxType(); j++) {
				   if (type[i] == j) {
					   match = true;
				   }
			   }
			   if (! match) {
				   continue;
			   }
		   }
			   SearchResult sr = new SearchResult();
			   sr.lat = nodeLat[i] * MoreMath.FIXPT_MULT_INV + centerLat;
			   sr.lon = nodeLon[i] * MoreMath.FIXPT_MULT_INV + centerLon;
			   sr.nameIdx = nameIdx[i];
			   //#if polish.api.bigsearch
			   //#if polish.api.osm-editing
			   System.out.println("fileId: " + fileId + " i: " + i);
			   if (Legend.enableEdits) {
				   System.out.println("osm id: " + osmID[i]);
				   sr.osmID = osmID[i];
			   }
			   //#endif
			   //#endif
			   if (Legend.enableUrlTags) {
				   sr.urlIdx = urlIdx[i];
			   }
			   if (Legend.enablePhoneTags) {
				   sr.phoneIdx = phoneIdx[i];
			   }
			   //#if polish.api.bigstyles
			   sr.type = (short)(-1 * type[i]); //It is a node. They have the top bit set to distinguish them from ways in search results
			   //#else
			   sr.type = (byte)(-1 * type[i]); //It is a node. They have the top bit set to distinguish them from ways in search results
			   //#endif
			   sr.dist = ProjMath.getDistance(sr.lat, sr.lon, lat, lon);
			   if (sr.dist < maxDist) {
				   resList.addElement(sr);				   
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
   
   private boolean isNodeOnScreen(int nodeId, short pcLDlat, short pcLDlon, short pcRUlat, short pcRUlon) {
	   short testLat=nodeLat[nodeId]; 
	   if (testLat < pcLDlat) {
		   return false;
	   }
	   if (testLat > pcRUlat) {
		   return false;
	   }
	   short testLon=nodeLon[nodeId]; 
	   if (testLon < pcLDlon) {
		   return false;
	   }
	   if (testLon > pcRUlon) {
		   return false;
	   }
	   return true;
   }
   
   public static void newPOIFont() {
	   poiFont = null;
   }

	/**
	 * @param ways the ways to set
	 */
	public void setWays(Way[][] ways) {
		this.ways = ways;
	}
	
	/**
	 * @return the ways
	 */
	public Way[][] getWays() {
		return ways;
	}
	
	public void setState(byte newState) {
		state = newState;
	}
}
