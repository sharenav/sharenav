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
//#if polish.api.online
import de.ueller.util.HttpHelper;
//#endif
import de.ueller.util.MoreMath;
import de.ueller.midlet.ui.UploadListener;

//#if polish.android
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.NullPointerException;
//#endif

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
	private boolean corrupt = false;

	//#if polish.android
	private static final int cacheSize = 40;
	//#else
	private static final int cacheSize = 100;
	//#endif
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
		if (data != null && data.length > 5) {
			Image image = Image.createImage(data, 0, data.length);
			this.image = image;
			// System.out.println("Image length: " + data.length);
			return image;
		}
		return null;
	}

	public void draw(PaintContext pc, int xSize, int ySize) {
		Image image = getImage();
		if (image != null) {
			// apparently if image is corrupted, we get a NPE at Graphics.java
			// on Android (Bitmap bitmap = img.getBitmap(); int width = bitmap.getWidth();)
			// when bitmap is null; catch and mark tile as corrupt. A reload is tried,
			// and if it's still corrupt, it's ignored.

			try {
				pc.g.drawImage(image,
					       xSize / 2 - xDiff,
					       ySize / 2 - yDiff,
					       Graphics.LEFT | Graphics.TOP);
			} catch (NullPointerException npe) {
				System.out.println("NPE drawing raster tile " + getTileString() + ", corrupt: " + corrupt);
				if (!corrupt) {
					data = null;
					image = null;
				}
				corrupt = true;
			}
		}
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
		// System.out.println("For scale " + scale + " returning zoom " + zoom);
		return zoom;
	}

	public static void addCachedTile(RasterTile tile) {
		if (rasterCache == null) {
			rasterCache = new RasterTile[cacheSize];
		}
		if (cacheCount >= cacheSize) {
			cacheCount = 0;
		}
		// don't overwrite a tile which is being retrieved
		while (rasterCache[cacheCount] != null && rasterCache[cacheCount].retrieving) {
			cacheCount++;
			if (cacheCount >= cacheSize) {
				cacheCount = 0;
			}
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
				//System.out.println("Cache hit: " + tile.getTileString());
			}
		}
		
		if (foundTile == null) {
			RasterTile newTile = new RasterTile(radlat, radlon, zoom, xdiff, ydiff);
			//System.out.println("Cache miss: " + newTile.getTileString());
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
		
		// System.out.println("gridWidth: " + gridWidth);
		// System.out.println("gridHeight: " + gridHeight);

		for (int x = 0 - gridWidth / 2; x < gridWidth / 2; x++) {
			for (int y = 0 - gridHeight / 2; y < gridHeight / 2; y++) {
				final RasterTile tile = getCachedTile(pc.center.radlat, pc.center.radlon, zoom, x, y);

				if (tile == null) {
					continue;
				}
				String tileString = tile.getTileString();
				//System.out.println("Possibly loading: " + tileString);
				// first try getting from file cache
				if (tile.data == null) {
					tile.readFileCache();
				}

				// then get from net
				if (tile.data == null && !tile.retrieving) {
					// System.out.println("Loading: " + tileString);
					Thread t = new Thread(new Runnable() {
						public void run() {
							tile.retrieving = true;
							while (numThreads >= MAXTHREADS) {
								try {
									Thread.sleep(100);
								} catch (InterruptedException ie) {
								}
							}
							numThreads++;
							tile.getData();
							numThreads--;
							tile.retrieving = false;
						}
					});
					if (numThreads < (cacheSize - MAXTHREADS)) {
						t.start();
					}
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

		String url = Configuration.getTMSUrl();
		url = HelperRoutines.replaceAll(url, "%z", "" + this.zoom);
		url = HelperRoutines.replaceAll(url, "%x", "" + this.x);
		url = HelperRoutines.replaceAll(url, "%y", "" + this.y);
		//url = HelperRoutines.replaceAll(url, "%z/%x/%y",
		//				getTileString());

		retrieved = false;
		//#if polish.api.online
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
			writeFileCache();
			System.out.println("Loaded tile: " + url + " length: " + data.length);
		}
		//#endif
	}

	public String getCacheFilePath() {
		// FIXME allow configurability of path

		String path = "/sdcard/GpsMid/tiles/" + "default"
			+ "/%z/%x/%y.png";
		path = HelperRoutines.replaceAll(path, "%z", "" + this.zoom);
		path = HelperRoutines.replaceAll(path, "%x", "" + this.x);
		path = HelperRoutines.replaceAll(path, "%y", "" + this.y);
		return path;
	}

	public void createDirs() {
		// FIXME allow configurability of path
		// FIXME move creation of top-level dirs to the time
		// of switching net-based raster maps on

		String path = "/sdcard/GpsMid/";
		checkAndCreate(path);
		path = "/sdcard/GpsMid/tiles/";
		checkAndCreate(path);
		path = "/sdcard/GpsMid/tiles/" + "default";
		checkAndCreate(path);
		path = "/sdcard/GpsMid/tiles/" + "default"
			+ "/%z";
		path = HelperRoutines.replaceAll(path, "%z", "" + this.zoom);
		checkAndCreate(path);
		path = "/sdcard/GpsMid/tiles/" + "default"
			+ "/%z/%x";
		path = HelperRoutines.replaceAll(path, "%z", "" + this.zoom);
		path = HelperRoutines.replaceAll(path, "%x", "" + this.x);
		checkAndCreate(path);
	}

	public void checkAndCreate(String path) {
		//#if polish.android
		File f = new File(path);
		if (! f.canWrite()) {
			f.mkdir();
		}
		//#endif
	}


	public void writeFileCache() {
		if (data == null) {
			return;
		}
		createDirs();
		// write the tile to a cache file

		String path = getCacheFilePath();

		//#if polish.android
		File f = new File(path);
		//if (! f.canWrite()) {
		//	System.out.println("Can't write file cache file " + path);
		//	return;
		//}
		OutputStream out = null;
		try {

			out = new FileOutputStream(f);
			out.write(data, 0, data.length);
			out.close();
		} catch (Exception e) {
			//logger.exception("Error writing file cache",e);
			// System.out.println("Error writing file cache: " + e);
		}
		//#endif
	}

	public void readFileCache() {
		String path = getCacheFilePath();

		// FIXME if file is too old, ignore

		// System.out.println("Checking if file is in file cache");

		//#if polish.android
		File f = new File(path);
		if (f != null) {
			int len = (int) f.length();
			if (len <= 5) {
				//System.out.println("File length is too short, not reading tile, length: " + len);
				return;
			}
			byte[] data = new byte[len];
			InputStream in = null;
			try {
				int tot = 0;
				in = new BufferedInputStream(new FileInputStream(f));
				while(tot < data.length){
					int rem = data.length - tot;
					int readB = in.read(data, tot, rem);
					if (readB > 0){
						tot += readB;
					}
				}
				in.close();
				this.data = data;
				//System.out.println("Read tile from file cache, length: " + len);
			} catch (IOException e) {
				System.out.println("Error reading file cache: " + e);
			}
		}
		//#endif
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
