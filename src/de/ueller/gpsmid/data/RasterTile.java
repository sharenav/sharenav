/*
 * GpsMid - Copyright (c) 2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net 
 *
 * GPLv2 - See COPYING
 */

/**
 * class for handling and drawing raster tiles
 * @author jkpj
 *
 */

package de.ueller.gpsmid.data;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.ui.Trace;

import de.ueller.util.HelperRoutines;
import de.ueller.util.HttpHelper;
import de.ueller.util.MoreMath;
import de.ueller.midlet.ui.UploadListener;

public class RasterTile implements UploadListener {
	public static final int TILE_SIZE = 256;
	public static final int MAXTHREADS = 2;

	private int zoom = 0;
	private int x = 0;
	private int y = 0;
	private int xDiff = 0;
	private int yDiff = 0;
	private byte data[] = null;
	private Image image = null;

	private static final int cacheSize = 100;
	private static int cacheCount = 0;
	private static RasterTile[] rasterCache = null;
	private static int numThreads = 0;

	private boolean retrieving = false;
	private boolean retrieved = false;

	public RasterTile(int zoom, int x, int y, byte[] data) {
		this.zoom = zoom;
		this.x = x;
		this.y = y;
		this.data = data;
	}

	public RasterTile(int zoom, int x, int y) {
		this.zoom = zoom;
		this.x = x;
		this.y = y;
	}

	// based on algorithm and example
	// from http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java


	public RasterTile(final float lat, final float lon, final int zoom) {
		updateNumbers(lat, lon, zoom);
	}

	public RasterTile(final float lat, final float lon, final int zoom, int xd, int yd) {
		updateNumbers(lat, lon, zoom, xd, yd);
	}

	public void updateNumbers(final float lat, final float lon, final int zoom) {
		updateNumbers(lat, lon, zoom, 0, 0);
	}

	public void updateNumbers(final float lat, final float lon, final int zoom, int xd, int yd) {
		this.zoom = zoom;
		double x = (lon * MoreMath.FAC_RADTODEC + 180) / 360 * (1<<zoom);
		this.x = (int)Math.floor(x) - xd;
		this.xDiff = (int) ((x - Math.floor(x) + xd) * TILE_SIZE);
		double y = (1 - MoreMath.log((float)(Math.tan(lat) + 1 / Math.cos(lat))) / Math.PI) / 2 * (1<<zoom);
		this.y = (int)Math.floor(y) - yd;
		this.yDiff = (int) ((y - Math.floor(y) + yd) * TILE_SIZE);
	}

	public String getTileString() {
		return("" + this.zoom + "/" + this.x + "/" + this.y);
	}

	public String getTileString(int xdiff, int ydiff) {
		return("" + this.zoom + "/" + (this.x + xdiff) + "/" + (this.y + ydiff));
	}

	public String replaceUrl(String url) {
		// placeholder
		return("" + zoom + "/" + this.x + "/" + this.y);
	}

	public Image getImage() {
		if (image != null) {
			return image;
		}
		if (data != null) {
			Image image = Image.createImage(data, 0, data.length);
			this.image = image;
			// System.out.println("Image length: " + data.length);
			return image;
		}
		return null;
	}

	public void draw(PaintContext pc, int xSize, int ySize) {
		pc.g.drawImage(getImage(),
			       xSize / 2 - xDiff,
			       ySize / 2 - yDiff,
			       Graphics.LEFT | Graphics.TOP);
		//System.out.println("Drawing: xDiff = " + xDiff + " yDiff = " + yDiff);
		//System.out.println("Drawing: xDiff%256 = " + xDiff % 256 + " yDiff%256 = " + yDiff % 256);
	}

	public static int getRasterZoom(float floatScale) {
		final int base = (int) Configuration.getRasterScale();
		int scale = (int) floatScale;
		final int maxZoom = 19;
		int zoom = maxZoom;
				
		while (zoom >= 0) {
			if (scale <= base * (2 << (maxZoom - zoom))) {
				break;
			}
			zoom--;
		}
		if (zoom < 0) {
			zoom = 0;
		}
		System.out.println("For scale " + scale + " returning zoom " + zoom);
		return zoom;
	}

	public static void addCachedTile(RasterTile tile) {
		if (rasterCache == null) {
			rasterCache = new RasterTile[cacheSize];
		}
		if (cacheCount >= cacheSize) {
			cacheCount = 0;
		}
		rasterCache[cacheCount++] = tile;
	}

	public static RasterTile getCachedTile(float radlat,
					       float radlon, int zoom, int xdiff, int ydiff) {
		RasterTile foundTile = null;
		for (int i = 0; i < cacheCount; i++) {
			RasterTile tile = rasterCache[i];
			if (tile.getTileString().equals(new RasterTile(radlat,
								       radlon, zoom, xdiff, ydiff).getTileString())) {
				foundTile = tile;
				System.out.println("Cache hit: " + tile.getTileString());
			}
		}
		
		if (foundTile == null) {
			RasterTile newTile = new RasterTile(radlat, radlon, zoom, xdiff, ydiff);
			System.out.println("Cache miss: " + newTile.getTileString());
			addCachedTile(newTile);
			foundTile = newTile;
		}
		foundTile.updateNumbers(radlat, radlon, zoom, xdiff, ydiff);
		return foundTile;
	}

	public static void drawRasterMap(PaintContext pc, int xSize, int ySize) {
		int zoom = getRasterZoom(pc.scale);

		// snap to scale to correct zoom if necessary
		// (e.g. user has pinch zoomed)
		final int maxZoom = 19;
		pc.scale = Configuration.getRasterScale()
			* (2 << (maxZoom - zoom));
		Trace.getInstance().scale = pc.scale;

		int gridWidth = xSize / TILE_SIZE + 4;
		int gridHeight = ySize / TILE_SIZE + 4;
		
		System.out.println("gridWidth: " + gridWidth);
		System.out.println("gridHeight: " + gridHeight);

		for (int x = 0 - gridWidth / 2; x < gridWidth / 2; x++) {
			for (int y = 0 - gridHeight / 2; y < gridHeight / 2; y++) {
				final RasterTile tile = getCachedTile(pc.center.radlat, pc.center.radlon, zoom, x, y);

				String tileString = tile.getTileString();
				//System.out.println("Possibly loading: " + tileString);
				if (tile.data == null && !tile.retrieving) {
					System.out.println("Loading: " + tileString);
					Thread t = new Thread(new Runnable() {
						public void run() {
							tile.retrieving = true;
							while (numThreads >= MAXTHREADS) {
								try {
									Thread.sleep(20);
								} catch (InterruptedException ie) {
								}
							}
							numThreads++;
							tile.getData();
							numThreads--;
							tile.retrieving = false;
						}
					});
					t.start();
				}
				if (tile.data != null) {
					tile.draw(pc, xSize, ySize);
				}
			}
		}
	}

	public void getData() {
		//typically url is something like "http://tiles.some.provid.er/mapname/%z/%x/%y.ext";
		// where ext is e.g. png or jpg

		//
		// replace "%z/%x/%y" with tileString

		// FIXME should be able to replace %z, %x, %y separately
		String url = Configuration.getTMSUrl();
		url = HelperRoutines.replaceAll(url, "%z/%x/%y",
						getTileString());
		retrieved = false;
		HttpHelper http = new HttpHelper();
		System.out.println("Getting tile: " + url);
		http.getURL(url, this, true);
		try {
			if (!retrieved) {
				wait();
			}
		} catch (InterruptedException ie) {
			retrieving = false;
		}
		if (retrieved) {
			data = http.getBinaryData();
			System.out.println("Loaded tile: " + url);
		}
	}

	public synchronized void completedUpload(boolean success, String message) {
		Trace.getInstance().newDataReady();
		retrieved = true;
		notifyAll();
	}

	public void setProgress(String message) {
		// TODO Auto-generated method stub
		
	}

	public void startProgress(String title) {
		// TODO Auto-generated method stub
		
	}

	public void updateProgress(String message) {
		// TODO Auto-generated method stub
		
	}

	public void updateProgressValue(int increment) {
		// TODO Auto-generated method stub
		
	}

	public void uploadAborted() {
		// TODO Auto-generated method stub
		
	}
}
