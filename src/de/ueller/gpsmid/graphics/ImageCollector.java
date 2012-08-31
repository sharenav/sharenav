/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.gpsmid.graphics;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.enough.polish.util.Locale;

import de.ueller.gps.Node;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.data.PositionMark;
import de.ueller.gpsmid.data.RasterTile;
import de.ueller.gpsmid.data.ScreenContext;
import de.ueller.gpsmid.mapdata.DictReader;
import de.ueller.gpsmid.mapdata.Way;
import de.ueller.gpsmid.mapdata.WayDescription;
import de.ueller.gpsmid.routing.RouteInstructions;
import de.ueller.gpsmid.tile.Tile;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.gpsmid.ui.TraceLayout;
import de.ueller.midlet.iconmenu.LayoutElement;
//#if polish.api.finland
import de.ueller.util.ETRSTM35FINconvert;
//#endif
import de.ueller.util.IntPoint;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import java.util.Enumeration;

import de.ueller.util.HelperRoutines;
import de.ueller.midlet.ui.UploadListener;

/* This class collects all visible objects to an offline image for later painting.
 * It is run in a low priority to avoid interrupting the GUI.
 */
public class ImageCollector implements Runnable {
	private final static Logger logger = Logger.getInstance(ImageCollector.class, 
			Logger.TRACE);
	
	private volatile boolean shutdown = false;
	private volatile boolean suspended = true;
	private final Tile t[];
	private Thread processorThread;
	/** the next run of the createloop will take these parameters */
	private final ScreenContext nextSc = new ScreenContext();
	/** the next paint to screen */
//	private ScreenContext currentVisibleSc = null;
	private ScreenContext lastCreatedSc = null;

	private final Image[] img = new Image[2];
	private volatile PaintContext[] pc = new PaintContext[2];
	public static volatile Node mapCenter = new Node();
	public static volatile long icDuration = 0;
	byte nextCreate = 1;
	byte nextPaint = 0;
	/** width of the double buffer image (including overscan) */
	int xSize;
	/** hight of the double buffer image (including overscan) */
	int ySize;
	/** offset x for overscan */
	public int xScreenOverscan;
	/** offset y for overscan */
	public int yScreenOverscan;
	int yScreenSize;
	IntPoint newCenter = new IntPoint(0, 0);
	IntPoint oldCenter = new IntPoint(0, 0);
	float oldCourse;
	private volatile boolean needRedraw = false;
	public static volatile int createImageCount = 0;
	private final Trace tr;
	/** additional scale boost for Overview/Filter Map, bigger values load the tiles already when zoomed more out */
	public static float overviewTileScaleBoost = 1.0f;
	public static volatile int minTile = 0;
	boolean collectorReady=false;
	
	public int iDrawState = 0;
	
	public ImageCollector(Tile[] t, int x, int y, Trace tr, Images i) {
		super();
		this.t = t;
		this.tr = tr;
		Node n = new Node(2f, 0f);
		Projection p1 = ProjFactory.getInstance(n, 0, 1500, xSize, ySize);
		if (p1.isOrthogonal()) {
			// with overscan
			xScreenOverscan = x*12/100;
			yScreenOverscan = y*12/100;
			if (tr.isShowingSplitIconMenu()) {
				yScreenOverscan = 0;
			}
			xSize = x+2*xScreenOverscan;
			ySize = y+2*yScreenOverscan;
		} else {
			// without overscan
			xSize = x;
			ySize = y;
			xScreenOverscan = 0;
			yScreenOverscan = 0;
		}
		img[0] = Image.createImage(xSize, ySize);
		img[1] = Image.createImage(xSize, ySize);
		try {
			pc[0] = new PaintContext(tr, i);
			pc[0].setP(p1);
			pc[0].state = PaintContext.STATE_READY;
			pc[1] = new PaintContext(tr, i);
			pc[1].setP(ProjFactory.getInstance(n, 0, 1500, xSize, ySize));
			pc[1].state = PaintContext.STATE_READY;


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		nextSc.setP(ProjFactory.getInstance(mapCenter, 
						nextSc.course, nextSc.scale, xSize, ySize));
		processorThread = new Thread(this, "ImageCollector");
		//#if polish.android
		processorThread.setPriority(Thread.MAX_PRIORITY);
		//#else
		processorThread.setPriority(Thread.MIN_PRIORITY);
		//#endif
		//#if not polish.api.paintdirect
		processorThread.start();
		//#endif
	}

	public void run() {
		PaintContext createPC = null;
		final byte MAXCRASHES = 5;
		byte crash = 0;
		do {
		try {
			
			while (!shutdown) {

				if (!needRedraw || suspended) {
					synchronized (this) {
						try {
							/* FIXME: We still have some situations where redraw is not done automatically immediately,
							 * e.g. on Nokia 5800 after returning from another Displayable
							 * Therefore reduce the timeout for redrawing anyway from 30 seconds to 1 seconds 
							 * if the last user interaction happened less than 1.5 secs before 

							if (Trace.getDurationSinceLastUserActionTime() > 1500 ) {
								wait(30000);
							} else {
								wait(1000);
							}
							*/
							wait(30000);
						} catch (InterruptedException e) {
							continue; // Recheck condition of the loop
						}
					}
				}
				
				needRedraw = false;	//moved here and deleted from the bottom of this routine
					
				//#debug debug
				logger.debug("Redrawing Map");
				
				iDrawState = 1;
				
				synchronized (this) {
					while (pc[nextCreate].state != PaintContext.STATE_READY && !shutdown) {
						try {
							// System.out.println("img not ready");
							wait(1000);
						} catch (InterruptedException e) {
						}
					}
					if (suspended || shutdown) {
						continue;
					}
					pc[nextCreate].state = PaintContext.STATE_IN_CREATE;
				}
				
				iDrawState = 2;

				createPC = pc[nextCreate];

				long startTime = System.currentTimeMillis();

				// create PaintContext
				createPC.xSize = nextSc.xSize;
				createPC.ySize = nextSc.ySize;
				createPC.center = nextSc.center.copy();
				mapCenter = nextSc.center.copy();
				createPC.scale = nextSc.scale;
				createPC.course = nextSc.course;
//				Projection p = ProjFactory.getInstance(createPC.center, 
//						nextSc.course, nextSc.scale, xSize, ySize);
				createPC.setP(nextSc.getP());
//				p.inverse(xSize, 0, createPC.screenRU);
//				p.inverse(0, ySize, createPC.screenLD);
				// pcCollect.trace = nextSc.trace;
				// pcCollect.dataReader = nextSc.dataReader;
				// cleans the screen
				createPC.g = img[nextCreate].getGraphics();
				createPC.g.setColor(Legend.COLORS[Legend.COLOR_MAP_BACKGROUND]);
				createPC.g.fillRect(0, 0, xSize, ySize);
				
//				createPC.g.setColor(0x00FF0000);
//				createPC.g.drawRect(0, 0, xSize - 1, ySize - 1);
//				createPC.g.drawRect(20, 20, xSize - 41, ySize - 41);
				createPC.squareDstWithPenToWay = Float.MAX_VALUE;
				createPC.squareDstWithPenToActualRoutableWay = Float.MAX_VALUE;
				createPC.squareDstWithPenToRoutePath = Float.MAX_VALUE;
				createPC.squareDstToRoutePath = Float.MAX_VALUE;
				createPC.dest = nextSc.dest;
				createPC.waysPainted = 0;


				
				// System.out.println("create " + pcCollect);
				
				Way.setupDirectionalPenalty(createPC, tr.speed, tr.gpsRecenter && !tr.gpsRecenterInvalid);


				float boost = Configuration.getMaxDetailBoostMultiplier();
				
				/*
				 * layers containing highlighted path segments
				 */
				createPC.hlLayers = 0;
				
				/*
				 * highlighted path is on top if gps recentered, but if not it might still come to top
				 * when we determine during painting that the cursor is closer than 25 meters at the route line.
				 */
				createPC.highlightedPathOnTop = tr.gpsRecenter;
				
				/**
				 * At the moment we don't really have proper layer support
				 * in the data yet, so only split it into Area, Way and Node
				 * layers
				 */
				byte layersToRender[] = { Tile.LAYER_AREA, 1 | Tile.LAYER_AREA , 2 | Tile.LAYER_AREA,
						3 | Tile.LAYER_AREA, 4 | Tile.LAYER_AREA,  0, 1, 2, 3, 4,
						Tile.LAYER_NODE, 
						0 | Tile.LAYER_HIGHLIGHT, 1 | Tile.LAYER_HIGHLIGHT,
						2 | Tile.LAYER_HIGHLIGHT, 3 | Tile.LAYER_HIGHLIGHT
				};
				
				/**
				 * Draw each layer separately to enforce paint ordering:
				 *
				 * Go through the entire tile tree multiple times
				 * to get the drawing order correct.
				 * 
				 * The first 5 layers correspond to drawing areas with the osm
				 * layer tag of  (< -1, -1, 0, 1, >1),
				 * then next 5 layers are drawing streets with
				 * osm layer tag (< -1, -1, 0, 1, >1).
				 * 
				 * Then we draw the highlighted streets
				 * and finally we draw the POI layer.
				 * 
				 * So e. g. layer 7 corresponds to all streets that
				 * have no osm layer tag or layer = 0.
				 */
				boolean simplifyMap = Configuration.getCfgBitState(Configuration.CFGBIT_SIMPLIFY_MAP_WHEN_BUSY);
				boolean skippableLayer = false;

				if (Configuration.getCfgBitState(Configuration.CFGBIT_TMS_BACKGROUND)) {
					RasterTile.drawRasterMap(createPC, xSize, ySize);
				}
				for (byte layer = 0; layer < layersToRender.length; layer++) {
					if (simplifyMap) {
						skippableLayer = 
						    ((layer < 5 && layer > 1)
						     //#if polish.api.finland
						     // don't skip node layer where speed camera is if camera alert is on
						     || (layer == 14 && !Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDCAMERA_ALERT))
						     //#else
						     || (layer == 14)
						     //#endif
						     //#if polish.android
						     || (Trace.getInstance().mapBrowsing && ((layer < 5) || layer == 14))
						     //#else
						     || (Trace.getInstance().mapBrowsing && ((layer < 5 && layer > 1) || layer == 14))
						     //#endif
							    );
						// skip update if a new one is queued
						if (needRedraw && skippableLayer) {
							continue;
						}
					}

					if (layersToRender[layer] == Tile.LAYER_NODE) {
						tr.resetClickableMarkers();
					}

					// render only highlight layers which actually have highlighted path segments
					if (
						(layersToRender[layer] & Tile.LAYER_HIGHLIGHT) > 0
						&& layersToRender[layer] != Tile.LAYER_NODE
					) {
						/**
						 * as we do two passes for each way layer when gps recentered - one for the ways and one for the route line on top,
						 * we can use in the second pass the determined route path connection / idx
						 * to highlight the route line in the correct / prior route line color.
						 * when not gps recentered, this info will be by one image obsolete however
						 */ 
						if (layersToRender[layer] == Tile.LAYER_HIGHLIGHT /*(0 | Tile.LAYER_HIGHLIGHT) pointless bitwise operation*/) {
							/*
							 *  only take ImageCollector loops into account for dstToRoutePath if ways were painted
							 *  otherwise this would trigger wrong route recalculations
							*/
							if (createPC.waysPainted != 0) {
								//RouteInstructions.dstToRoutePath = createPC.getDstFromSquareDst(createPC.squareDstToRoutePath);
								RouteInstructions.dstToRoutePath = createPC.getDstFromRouteSegment();
								if (RouteInstructions.dstToRoutePath != RouteInstructions.DISTANCE_UNKNOWN) {
									RouteInstructions.routePathConnection = createPC.routePathConnection;
									RouteInstructions.pathIdxInRoutePathConnection = createPC.pathIdxInRoutePathConnection;
									RouteInstructions.actualRoutePathWay = createPC.actualRoutePathWay;
									// when we determine during painting that the cursor is closer than 25 meters at the route line, bring it to the top
									if (RouteInstructions.dstToRoutePath < 25) {
										createPC.highlightedPathOnTop = true;
									}
								}
								//System.out.println("waysPainted: " + createPC.waysPainted);
							} else {
								// FIXME: Sometimes there are ImageCollector loop with no way pained even when ways would be there and tile data is fully loaded
								// Update 2011-06-11: Might be fixed with the patch from gojkos at [ gpsmid-Bugs-3310178 ] Delayed map draw on LG cookie phone
								// Update 2012-03-17 (patch from walter9): The image collector has been started twice. This seems to be fixed now in Trace.java.startImageCollector()
								System.out.println("No ways painted in this ImageCollector loop");
							}
						}
						byte relLayer = (byte)(((int)layersToRender[layer]) & 0x0000000F);
						if ( (createPC.hlLayers & (1 << relLayer)) == 0) {
							continue;
						}
					}
					minTile = Legend.scaleToTile((int)(createPC.scale / (boost * overviewTileScaleBoost) ));
					
					if (t[0] != null) {
						if (needRedraw && skippableLayer) {
							continue;
						}
						t[0].paint(createPC, layersToRender[layer]);
						if (needRedraw && skippableLayer) {
							continue;
						}
					}
					if ((minTile >= 1) && (t[1] != null)) {
						if (needRedraw && skippableLayer) {
							continue;
						}
						t[1].paint(createPC, layersToRender[layer]);
						if (needRedraw && skippableLayer) {
							continue;
						}
						Thread.yield();
					}
					if ((minTile >= 2) && (t[2] != null)) {
						if (needRedraw && skippableLayer) {
							continue;
						}
						t[2].paint(createPC, layersToRender[layer]);
						if (needRedraw && skippableLayer) {
							continue;
						}
						Thread.yield();
					}
					if ((minTile >= 3) && (t[3] != null)) {
						if (needRedraw && skippableLayer) {
							continue;
						}
						t[3].paint(createPC, layersToRender[layer]);
						if (needRedraw && skippableLayer) {
							continue;
						}
						Thread.yield();
					}

					/**
					 * Drawing waypoints
					 */
					if (t[DictReader.GPXZOOMLEVEL] != null) {
						t[DictReader.GPXZOOMLEVEL].paint(createPC, layersToRender[layer]);
					}
					if (suspended) {
						// Don't continue rendering if suspended
						createPC.state = PaintContext.STATE_READY;
						break;
					}
				}
				/**
				 * Drawing debuginfo for routing
				 */
				if (!suspended && t[DictReader.ROUTEZOOMLEVEL] != null 
						&& (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS) 
								|| Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS))) {
					t[DictReader.ROUTEZOOMLEVEL].paint(createPC, (byte) 0);
				}
				
				iDrawState = 0;

				icDuration = System.currentTimeMillis() - startTime;
				//#mdebug
				logger.info("Painting map took " + icDuration + " ms");
				//#enddebug
				System.out.println("Painting map took " + icDuration + " ms " + xSize + "/" + ySize);

				createPC.state = PaintContext.STATE_READY;
				lastCreatedSc=createPC.cloneToScreenContext();
				if (!shutdown) {
					newCollected();
				}
				createImageCount++;				
				//needRedraw = false;
				tr.cleanup();
				// System.out.println("create ready");
				//System.gc();
			}
		} catch (OutOfMemoryError oome) {
			if (createPC != null) {
				createPC.state = PaintContext.STATE_READY;
			}
		   String recoverZoomedIn = "";
		   crash++;
		   if(tr.scale > 10000 && crash < MAXCRASHES) {
		    tr.scale /= 1.5f;
		    recoverZoomedIn = Locale.get("imagecollector.ZoomingInToRecover")/* Zooming in to recover.*/;
		   }   
		   logger.fatal(Locale.get("imagecollector.ImageCollectorRanOutOfMemory")/*ImageCollector ran out of memory: */ + oome.getMessage() + recoverZoomedIn);
		} catch (Exception e) {
			crash++;
			logger.exception(Locale.get("imagecollector.ImageCollectorCrashed")/*ImageCollector thread crashed unexpectedly with error */, e);
		}
		if(crash >= MAXCRASHES) {
			logger.fatal(Locale.get("imagecollector.ImageCollectorCrashedAborting")/*ImageCollector crashed too often. Aborting.*/);
		}
		} while (!shutdown && crash <MAXCRASHES);
		processorThread = null;
		synchronized (this) {
			notifyAll();
		}
	}

	//#if polish.api.paintdirect
	public Node paintDirect(PaintContext createPC) {
		tr.resetClickableMarkers();

		iDrawState = 2;

		long startTime = System.currentTimeMillis();

//				Projection p = ProjFactory.getInstance(createPC.center, 
//						nextSc.course, nextSc.scale, xSize, ySize);
//				p.inverse(xSize, 0, createPC.screenRU);
//				p.inverse(0, ySize, createPC.screenLD);
		// pcCollect.trace = nextSc.trace;
		// pcCollect.dataReader = nextSc.dataReader;
		// cleans the screen
		//jkp createPC.g = img[nextCreate].getGraphics();
		//jkp createPC.g.setColor(Legend.COLORS[Legend.COLOR_MAP_BACKGROUND]);
		// createPC.g.fillRect(0, 0, xSize, ySize);
				
//				createPC.g.setColor(0x00FF0000);
//				createPC.g.drawRect(0, 0, xSize - 1, ySize - 1);
//				createPC.g.drawRect(20, 20, xSize - 41, ySize - 41);
		createPC.squareDstWithPenToWay = Float.MAX_VALUE;
		createPC.squareDstWithPenToActualRoutableWay = Float.MAX_VALUE;
		createPC.squareDstWithPenToRoutePath = Float.MAX_VALUE;
		createPC.squareDstToRoutePath = Float.MAX_VALUE;
		createPC.dest = nextSc.dest;
		createPC.waysPainted = 0;
		//Projection p = ProjFactory.getInstance(createPC.center, nextSc.course, nextSc.scale, xSize,
		//				       (createPC.trace.isShowingSplitScreen()) ? (int) (ySize / 2) : ySize);
//		System.out.println("p  =" + p);
		Projection p = ProjFactory.getInstance(createPC.center,
				createPC.course, createPC.scale, xSize,
							(createPC.trace.isShowingSplitScreen()) ? (int) (ySize / 2) : ySize);
		Projection p1 = p;
		createPC.setP(p);
				
		// System.out.println("create " + pcCollect);
				
		Way.setupDirectionalPenalty(createPC, tr.speed, tr.gpsRecenter && !tr.gpsRecenterInvalid);


		float boost = Configuration.getMaxDetailBoostMultiplier();
				
		/*
		 * layers containing highlighted path segments
		 */
		createPC.hlLayers = 0;
				
		/*
		 * highlighted path is on top if gps recentered, but if not it might still come to top
		 * when we determine during painting that the cursor is closer than 25 meters at the route line.
		 */
		createPC.highlightedPathOnTop = tr.gpsRecenter;
				
		/**
		 * At the moment we don't really have proper layer support
		 * in the data yet, so only split it into Area, Way and Node
		 * layers
		 */
		byte layersToRender[] = { Tile.LAYER_AREA, 1 | Tile.LAYER_AREA , 2 | Tile.LAYER_AREA,
					  3 | Tile.LAYER_AREA, 4 | Tile.LAYER_AREA,  0, 1, 2, 3, 4,
					  Tile.LAYER_NODE, 
					  0 | Tile.LAYER_HIGHLIGHT, 1 | Tile.LAYER_HIGHLIGHT,
					  2 | Tile.LAYER_HIGHLIGHT, 3 | Tile.LAYER_HIGHLIGHT
		};
				
		/**
		 * Draw each layer separately to enforce paint ordering:
		 *
		 * Go through the entire tile tree multiple times
		 * to get the drawing order correct.
		 * 
		 * The first 5 layers correspond to drawing areas with the osm
		 * layer tag of  (< -1, -1, 0, 1, >1),
		 * then next 5 layers are drawing streets with
		 * osm layer tag (< -1, -1, 0, 1, >1).
		 * 
		 * Then we draw the highlighted streets
		 * and finally we draw the POI layer.
		 * 
		 * So e. g. layer 7 corresponds to all streets that
		 * have no osm layer tag or layer = 0.
		 */
		boolean simplifyMap = Configuration.getCfgBitState(Configuration.CFGBIT_SIMPLIFY_MAP_WHEN_BUSY);
		boolean skippableLayer = false;
		for (byte layer = 0; layer < layersToRender.length; layer++) {
			if (simplifyMap) {
				skippableLayer = 
					((layer < 5 && layer > 1)
					 //#if polish.api.finland
					 // don't skip node layer where speed camera is if camera alert is o
					 || (layer == 14 && !Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDCAMERA_ALERT))
					 //#else
					 || (layer == 14)
					 //#endif
					 //#if polish.android
					 || (Trace.getInstance().mapBrowsing && ((layer < 5) || layer == 14))
					 //#else
					 || (Trace.getInstance().mapBrowsing && ((layer < 5 && layer > 1) || layer == 14))
					 //#endif
						);
				// skip update if a new one is queued
 				if (needRedraw && skippableLayer) {
					//continue;
				}
			}

			// render only highlight layers which actually have highlighted path segments
			if (
				(layersToRender[layer] & Tile.LAYER_HIGHLIGHT) > 0
				&& layersToRender[layer] != Tile.LAYER_NODE
				) {
				/**
				 * as we do two passes for each way layer when gps recentered - one for the ways and one for the route line on top,
				 * we can use in the second pass the determined route path connection / idx
				 * to highlight the route line in the correct / prior route line color.
				 * when not gps recentered, this info will be by one image obsolete however
				 */ 
				if (layersToRender[layer] == Tile.LAYER_HIGHLIGHT /*(0 | Tile.LAYER_HIGHLIGHT) pointless bitwise operation*/) {
					/*
					 *  only take ImageCollector loops into account for dstToRoutePath if ways were painted
					 *  otherwise this would trigger wrong route recalculations
					 */
					if (createPC.waysPainted != 0) {
						//RouteInstructions.dstToRoutePath = createPC.getDstFromSquareDst(createPC.squareDstToRoutePath);
						RouteInstructions.dstToRoutePath = createPC.getDstFromRouteSegment();
						if (RouteInstructions.dstToRoutePath != RouteInstructions.DISTANCE_UNKNOWN) {
							RouteInstructions.routePathConnection = createPC.routePathConnection;
							RouteInstructions.pathIdxInRoutePathConnection = createPC.pathIdxInRoutePathConnection;
							RouteInstructions.actualRoutePathWay = createPC.actualRoutePathWay;
							// when we determine during painting that the cursor is closer than 25 meters at the route line, bring it to the top
							if (RouteInstructions.dstToRoutePath < 25) {
								createPC.highlightedPathOnTop = true;
							}
						}
						//System.out.println("waysPainted: " + createPC.waysPainted);
					} else {
						// FIXME: Sometimes there are ImageCollector loop with no way pained even when ways would be there and tile data is fully loaded
						// Update 2011-06-11: Might be fixed with the patch from gojkos at [ gpsmid-Bugs-3310178 ] Delayed map draw on LG cookie phone
						// Update 2012-03-17 (patch from walter9): The image collector has been started twice. This seems to be fixed now in Trace.java.startImageCollector()
						System.out.println("No ways painted in this ImageCollector loop");
					}
				}
				byte relLayer = (byte)(((int)layersToRender[layer]) & 0x0000000F);
				if ( (createPC.hlLayers & (1 << relLayer)) == 0) {
					continue;
				}
			}
			minTile = Legend.scaleToTile((int)(createPC.scale / (boost * overviewTileScaleBoost) ));
					
			if (t[0] != null) {
				if (needRedraw && skippableLayer) {
					continue;
				}
				t[0].paint(createPC, layersToRender[layer]);
				if (needRedraw && skippableLayer) {
					continue;
				}
			}
			if ((minTile >= 1) && (t[1] != null)) {
				if (needRedraw && skippableLayer) {
					continue;
				}
				t[1].paint(createPC, layersToRender[layer]);
				if (needRedraw && skippableLayer) {
					continue;
				}
				//Thread.yield();
			}
			if ((minTile >= 2) && (t[2] != null)) {
				if (needRedraw && skippableLayer) {
					continue;
				}
				t[2].paint(createPC, layersToRender[layer]);
				if (needRedraw && skippableLayer) {
					continue;
				}
				//Thread.yield();
			}
			if ((minTile >= 3) && (t[3] != null)) {
				if (needRedraw && skippableLayer) {
					continue;
				}
				t[3].paint(createPC, layersToRender[layer]);
				if (needRedraw && skippableLayer) {
					continue;
				}
				//Thread.yield();
			}

			/**
			 * Drawing waypoints
			 */
			if (t[DictReader.GPXZOOMLEVEL] != null) {
				t[DictReader.GPXZOOMLEVEL].paint(createPC, layersToRender[layer]);
			}
			if (suspended) {
				// Don't continue rendering if suspended
				createPC.state = PaintContext.STATE_READY;
				break;
			}
		}
		/**
		 * Drawing debuginfo for routing
		 */
		if (!suspended && t[DictReader.ROUTEZOOMLEVEL] != null 
		    && (Configuration.getCfgBitState(Configuration.CFGBIT_ROUTE_CONNECTIONS) 
			|| Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_TURN_RESTRICTIONS))) {
			t[DictReader.ROUTEZOOMLEVEL].paint(createPC, (byte) 0);
		}
				
		iDrawState = 0;

		icDuration = System.currentTimeMillis() - startTime;
		//#mdebug
		logger.info("Painting map took " + icDuration + " ms");
		//#enddebug
		System.out.println("Painting map took " + icDuration + " ms " + xSize + "/" + ySize);

		createPC.state = PaintContext.STATE_READY;
		//lastCreatedSc=createPC.cloneToScreenContext();
		if (!shutdown) {
			//newCollected();
		}
		createImageCount++;				
		//needRedraw = false;
		//tr.cleanup();
		// System.out.println("create ready");
		//System.gc();
		return createPC.center.copy();
	}
	//#endif

	public void suspend() {
		suspended = true;
	}

	public void resume() {
		suspended = false;
	}
	
	public synchronized void stop() {
		shutdown = true;
		notifyAll();
		try {
			while ((processorThread != null) && (processorThread.isAlive())) {
				wait(1000);
			}
		} catch (InterruptedException e) {
			//Nothing to do
		}
	}
	
	public boolean isRunning() {
		return !suspended && !shutdown;
	}

	/** copy the last created image to the real screen

	public void restart() {
		processorThread = new Thread(this, "ImageCollector");
		//#if polish.android
		processorThread.setPriority(Thread.MAX_PRIORITY);
		//#else
		processorThread.setPriority(Thread.MIN_PRIORITY);
		//#endif
		//#if not polish.api.paintdirect
		processorThread.start();
		//#endif
	}

	/** copy the last created image to the real screen
	 *  but with the last collected position and direction in the center
	 */
	public Node paint(PaintContext screenPc) {

		PaintContext paintPC;
		// System.out.println("paint this: " + screenPc);
		// System.out.println("paint image: " + pc[nextPaint]);
		if (suspended || !collectorReady) {
			return new Node(0, 0);
		}

		// Define the parameters for the next image that will be created
		nextSc.center = screenPc.center.copy();
		nextSc.course = screenPc.course;
		nextSc.scale = screenPc.scale;
		nextSc.dest = screenPc.dest;
		nextSc.xSize = screenPc.xSize;
		nextSc.ySize = screenPc.ySize;
		Projection p = ProjFactory.getInstance(nextSc.center, nextSc.course, nextSc.scale, xSize, ySize);
//		System.out.println("p  =" + p);
		Projection p1 = ProjFactory.getInstance(nextSc.center,
				pc[nextPaint].course, pc[nextPaint].scale, xSize, ySize);
//		System.out.println("p  =" + p1);
		nextSc.setP(p);
		screenPc.setP(p);

		synchronized (this) {
			if (pc[nextPaint].state != PaintContext.STATE_READY) {
				logger.error(Locale.get("imagecollector.ImageCollectorNonReadyPaintContext")/*ImageCollector was trying to draw a non ready PaintContext */
						+ pc[nextPaint].state);
				return new Node(0, 0);
			}
			paintPC = pc[nextPaint];
			paintPC.state = PaintContext.STATE_IN_PAINT;

		}
		int screenXCenter = xSize / 2 - xScreenOverscan;
		int screenYCenter = ySize / 2 - yScreenOverscan;
		int newXCenter = screenXCenter;
		int newYCenter = screenYCenter;

		// return center of the map image drawn to the caller
		Node getDrawnCenter = paintPC.center.copy();
		if (p.isOrthogonal()) {
			// maps can painted so that the hotspot is at the predefined point on the screen
			// therfore the offset is useful in that case its not necessary to create a new image
			// if the position has changed less then half of the offset
			if (lastCreatedSc != null) {
				p1.forward(lastCreatedSc.center, oldCenter);
				newXCenter = oldCenter.x - p.getImageCenter().x + screenXCenter;
				newYCenter = oldCenter.y - p.getImageCenter().y + screenYCenter;
				// System.out.println("Paint pos = " + newXCenter + "/" +
				// newYCenter);
				// System.out.println("Paint ysize=" + ySize + " nextSc.xSize="
				// + nextSc.ySize + " hotspot=" + p.getImageCenter());
				
			}
			screenPc.g.drawImage(img[nextPaint], newXCenter, newYCenter,
					     Graphics.VCENTER | Graphics.HCENTER);
			// Test if the new center is around the middle of the screen, in which
			// case we don't need to redraw (recreate a new image), as nothing has changed.
			if ( Math.abs(newXCenter - screenXCenter) > 4
					|| Math.abs(newYCenter - screenYCenter) > 4
					|| paintPC.course != nextSc.course) {
				// The center of the screen has moved or rotated, so need
				// to redraw the map image
				needRedraw = true;
				// System.out.println("wakeup thread because course or position changed");
				// System.out.println("Changed " + newXCenter + "->" +
				// screenXCenter + " and " + newYCenter + "->" + screenYCenter);
			}
		} else {
			screenPc.g.drawImage(img[nextPaint], screenXCenter,
					screenYCenter, Graphics.VCENTER | Graphics.HCENTER);
			p.forward(lastCreatedSc.center, oldCenter);
			newXCenter = oldCenter.x - p.getImageCenter().x + screenXCenter;
			newYCenter = oldCenter.y - p.getImageCenter().y + screenYCenter;
			if ( Math.abs(newXCenter - screenXCenter) > 1
			     || Math.abs(newYCenter - screenYCenter) > 1
			     || paintPC.course != nextSc.course) {
				needRedraw = true;
			}
		}
		
		// screenPc.g.drawArc(newXCenter-14, newYCenter-14, 28, 28, 0, 360);
//		if (p instanceof Proj3D){
//			screenPc.g.setColor(255,50,50);
//			IntPoint pt0 = new IntPoint();
//			IntPoint pt1 = new IntPoint();
//			Proj3D p3=(Proj3D)p;
//			p.forward(p3.borderLD,pt0);
//			p.forward(p3.borderLU,pt1);
//			screenPc.g.drawLine(pt0.x, pt0.y, pt1.x, pt1.y);
//			p.forward(p3.borderRU,pt0);
//			screenPc.g.drawLine(pt0.x, pt0.y, pt1.x, pt1.y);
//			p.forward(p3.borderRD,pt1);
//			screenPc.g.drawLine(pt0.x, pt0.y, pt1.x, pt1.y);
//			p.forward(p3.borderLD,pt0);
//			screenPc.g.drawLine(pt0.x, pt0.y, pt1.x, pt1.y);
//
//		}
		String name = null;
		Way wayForName = null;
		/**
		 * used to check for pixel distances because checking for meters from
		 * converted pixels requires to be exactly on the pixel when zoomed out
		 * far
		 */
		final int SQUARE_MAXPIXELS = 5 * 5;
		// Tolerance of 15 pixels converted to meters
		float pixDest = 15 / paintPC.ppm;
		if (pixDest < 15) {
			pixDest = 15;
		}
		if (paintPC.trace.gpsRecenter) {
			// Show closest routable way name if map is gpscentered and we are
			// closer
			// than SQUARE_MAXPIXELS or 30 m (including penalty) to it.
			// If the routable way is too far away, we try the closest way.
			if (paintPC.bUsedGpsCenter == false )	{
				if (paintPC.squareDstWithPenToActualRoutableWay < SQUARE_MAXPIXELS
					|| paintPC.getDstFromSquareDst(paintPC.squareDstWithPenToActualRoutableWay) < 30) {
					wayForName = paintPC.actualRoutableWay;
				} else if (paintPC.squareDstWithPenToWay < SQUARE_MAXPIXELS
					   || paintPC.getDstFromSquareDst(paintPC.squareDstWithPenToWay) < 30) {
					wayForName = paintPC.actualWay;
				}
			} else {
				if (paintPC.getDstFromRouteableWay() < 100) {
					wayForName = paintPC.actualRoutableWay;
				} else if ( paintPC.getDstFromWay() < 100) {
					wayForName = paintPC.actualWay;
				}
			}
		} else if (paintPC.getDstFromSquareDst(paintPC.squareDstWithPenToWay) <= pixDest) {
			// If not gpscentered show closest way name if it's no more than 15
			// pixels away.
			wayForName = paintPC.actualWay;
		}
		/*
		 * As we are double buffering pc, nothing should be writing to paintPC
		 * therefore it should be safe to access the volatile variable actualWay
		 */
		if (paintPC.actualWay != null) {
			screenPc.actualWay = paintPC.actualWay;
			screenPc.actualSingleTile = paintPC.actualSingleTile;
			tr.actualWay = paintPC.actualWay;
			tr.actualSingleTile = paintPC.actualSingleTile;
		}
		if (wayForName != null) {
			int nummaxspeed;
			String maxspeed = "";
			String winter = "";
			// store for OSM editing
			if (wayForName.getMaxSpeed() != 0) {
				nummaxspeed = wayForName.getMaxSpeed();
				if (Configuration
				    .getCfgBitState(Configuration.CFGBIT_MAXSPEED_WINTER)
				    && (wayForName.getMaxSpeedWinter() > 0)) {
					nummaxspeed = wayForName.getMaxSpeedWinter();
					winter = Locale.get("imagecollector.Winter")/*W */;
				}
				if (nummaxspeed == Legend.MAXSPEED_MARKER_NONE) {
					maxspeed = Locale.get("imagecollector.SL")/* SL:*/ + winter + 
						Locale.get("imagecollector.MaxSpeedNone")/*none*/;
				} else if (nummaxspeed == Legend.MAXSPEED_MARKER_VARIABLE) {
					maxspeed = Locale.get("imagecollector.SL")/* SL:*/ + winter + 
					Locale.get("imagecollector.MaxSpeedVariable")/*var*/;
				} else if (Configuration.getCfgBitState(Configuration.CFGBIT_METRIC)) {
					maxspeed = Locale.get("imagecollector.SL")/* SL:*/ + winter + nummaxspeed;
				} else {
					// Round up at this point, as the the previouse two
					// conversions
					// were rounded down already. (Seems to work better for
					// speed limits of
					// 20mph and 30mph)
				    maxspeed = Locale.get("imagecollector.SL")/* SL:*/ + winter + ((int)(nummaxspeed / 1.609344f + 0.5f));
				}
			}

			if (wayForName.nameIdx != -1) {
				name = screenPc.trace.getName(wayForName.nameIdx);
			} else {
				//#if polish.api.bigstyles
				WayDescription wayDesc = Legend
						.getWayDescription(wayForName.type);
				//#else
				WayDescription wayDesc = Legend
						.getWayDescription((short) (wayForName.type & 0xff));
				//#endif
				name = Locale.get("imagecollector.unnamed")/*(unnamed */ + wayDesc.description + ")";
			}
			if (name == null) {
				name = maxspeed;
			} else {
				name = name + maxspeed;
			}
			// If there's an URL associated with way, show a letter next to name
			if (wayForName.urlIdx != -1) {
			    name = name + Locale.get("imagecollector.W")/* W*/;
			}
			// Show 'P' for phone number
			if (wayForName.phoneIdx != -1) {
				name = name + Locale.get("imagecollector.P")/* P*/;
			}
		}
		// use the nearest routable way for the the speed limit detection if
		// it's
		// closer than 30 m or SQUARE_MAXPIXELS including penalty
		if (paintPC.squareDstWithPenToActualRoutableWay < SQUARE_MAXPIXELS
		    || paintPC
		    .getDstFromSquareDst(paintPC.squareDstWithPenToActualRoutableWay) < 30) {
			tr.actualSpeedLimitWay = paintPC.actualRoutableWay;
		} else {
			tr.actualSpeedLimitWay = null;
		}
		
		boolean showLatLon = Configuration
			.getCfgBitState(Configuration.CFGBIT_SHOWLATLON);

		LayoutElement e = Trace.tl.ele[TraceLayout.WAYNAME];
		if (showLatLon) {
//#if polish.api.finland
			// show Finnish ETRS-TM35FIN coordinates
			// FIXME: add a config option for selection of coordinates
			if (false) {
				PositionMark pmETRS = ETRSTM35FINconvert.latlonToEtrs(paintPC.center.radlat, paintPC.center.radlon);
				e.setText(Locale.get("imagecollector.lat")/* lat: */
					  + Float.toString(pmETRS.lat)
					  + " " + Locale.get("imagecollector.lon")/* lon: */
					  + Float.toString(pmETRS.lon));
			} else {
//#endif
			e.setText(Locale.get("imagecollector.lat")/* lat: */
				  + Float.toString(paintPC.center.radlat
						   * MoreMath.FAC_RADTODEC)
				  + " " + Locale.get("imagecollector.lon")/* lon: */
				  + Float.toString(paintPC.center.radlon
						   * MoreMath.FAC_RADTODEC));
//#if polish.api.finland
			}
//#endif
		} else {
			if (name != null && name.length() > 0) {
				e.setText(name);
			} else {
				e.setText(" ");
			}
		}

		if (paintPC.scale != screenPc.scale) {
//			System.out.println("wakeup thread because scale changed");
			needRedraw = true;
		}
		
		// when the projection has changed we must redraw
		if (!paintPC.getP().getProjectionID().equals(screenPc.getP().getProjectionID()) ) {
//			System.out.println("wakeup thread because projection changed");
			needRedraw = true;
		}
		
		synchronized (this) {
			paintPC.state = PaintContext.STATE_READY;
			if (needRedraw) {
				notify();
			} else {
//				System.out.println("No need to redraw after painting");
			}
		}
		// currentVisibleSc=lastCreatedSc.cloneToScreenContext();
		return getDrawnCenter;
	}
	
	private synchronized void newCollected() {
		while ((pc[nextPaint].state != PaintContext.STATE_READY) || (pc[nextCreate].state != PaintContext.STATE_READY)) {
			try {
				wait(50);
			} catch (InterruptedException e) {
			}
		}
		collectorReady=true;
		nextPaint = nextCreate;
		nextCreate = (byte) ((nextCreate + 1) % 2);
		tr.requestRedraw();
	}

	/**
	 * Inform the ImageCollector that new vector data is available
	 * and it's time to create a new image.
	 */
	public synchronized void newDataReady() {
		needRedraw = true;
		notify();
	}
	
	public Projection getCurrentProjection(){
		return pc[nextPaint].getP();
//		return currentVisibleSc.getP();
	}
	
	public synchronized ScreenContext getScreenContext() {
		return (ScreenContext) pc[0];
	}
}
