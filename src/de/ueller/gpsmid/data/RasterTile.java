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
	public static final int TILE_SIZE = 250;

	private int zoom = 0;
	private int x = 0;
	private int y = 0;
	private int xDiff = 0;
	private int yDiff = 0;
	private byte data[] = null;

	private static float oldCenterLat = 0f;
	private static float oldCenterLon = 0;
	private static float oldZoom = 0f;

	private static final int cacheSize = 500;
	private static int cacheCount = 0;
	private static RasterTile[] rasterCache = null;

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

	public void updateNumbers(final float lat, final float lon, final int zoom) {
		this.zoom = zoom;
		double x = (lon * MoreMath.FAC_RADTODEC + 180) / 360 * (1<<zoom);
		this.x = (int)Math.floor(x);
		this.xDiff = (int) ((x - Math.floor(x)) * 250);
		double y = (1 - MoreMath.log((float)(Math.tan(lat) + 1 / Math.cos(lat))) / Math.PI) / 2 * (1<<zoom);
		this.y = (int)Math.floor(y);
		this.yDiff = (int) ((y - Math.floor(y)) * 250);
	}

	public String getTileString() {
		return("" + this.zoom + "/" + this.x + "/" + this.y);
	}

	public String replaceUrl(String url) {
		// placeholder
		return("" + zoom + "/" + this.x + "/" + this.y);
	}

	// FIXME perhaps it would help to cache images
	public Image getImage() {
		if (data != null) {
			Image image = Image.createImage(data, 0, data.length);
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
			return;
		}
		rasterCache[cacheCount++] = tile;
	}

	public static RasterTile getCachedTile(float radlat,
					       float radlon, int zoom) {
		for (int i = 0; i < cacheCount; i++) {
			RasterTile tile = rasterCache[i];
			if (tile.getTileString().equals(new RasterTile(radlat,
								       radlon, zoom).getTileString())) {
				tile.updateNumbers(radlat, radlon,zoom);
				return tile;
			}
		}
		return null;
	}

	public static void drawRasterMap(PaintContext pc, int xSize, int ySize) {
		int zoom = getRasterZoom(pc.scale);

		// snap to scale to correct zoom if necessary
		// (e.g. user has pinch zoomed)
		final int maxZoom = 19;
		pc.scale = Configuration.getRasterScale()
			* (2 << (maxZoom - zoom));
		Trace.getInstance().scale = pc.scale;

		
		RasterTile centerTile = getCachedTile(pc.center.radlat, pc.center.radlon, zoom);

		if (centerTile == null) {
			centerTile = new RasterTile(pc.center.radlat,
						       pc.center.radlon, zoom);
		} else {
			System.out.println("Cache hit: " + centerTile.getTileString());
		}
		String tileString = centerTile.getTileString();
		System.out.println("Possibly loading: " + tileString);

		if (centerTile.data == null) {
			System.out.println("Loading: " + tileString);
			centerTile.getData();
		}
		if (centerTile.data != null) {
			centerTile.draw(pc, xSize, ySize);
			addCachedTile(centerTile);
		}
		oldCenterLat = pc.center.radlat;
		oldCenterLon = pc.center.radlon;
		oldZoom = zoom;
	}

	public void getData() {
		//String baseTMSUrl = "http://tiles.kartat.kapsi.fi/taustakartta/";
		// String url = baseTMSUrl + tileString + ".jpg";
		// String url = baseTMSUrl + zoom + "/" + x + "/" + y + ".jpg";
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
		}
	}

	public synchronized void completedUpload(boolean success, String message) {
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
