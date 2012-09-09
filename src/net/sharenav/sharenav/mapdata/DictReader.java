/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.mapdata;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.tile.ContainerTile;
import net.sharenav.sharenav.tile.FileTile;
import net.sharenav.sharenav.tile.RouteContainerTile;
import net.sharenav.sharenav.tile.RouteTile;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.sharenav.tile.Tile;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

/** Reads the dict-files (/dat/dict-[1..4].dat) from the map source
 *  and creates tiles from them.
 */
public class DictReader implements Runnable {
	private final Thread processorThread;
	private final static Logger logger = Logger.getInstance(DictReader.class, Logger.INFO);
	private Tile dict;
	private final Trace	t;

	public  final static int ROUTEZOOMLEVEL = 4;
	public  final static int GPXZOOMLEVEL = 5;
	public  final static int NUM_DICT_ZOOMLEVELS = GPXZOOMLEVEL + 1;
	
	public DictReader(Trace t) {
		super();
		this.t = t;
		processorThread = new Thread(this, "DictReader");
		processorThread.setPriority(Thread.MIN_PRIORITY + 2);
		processorThread.start();
		//#debug trace
		logger.trace("DictReader Thread started");

	}

	public void run() {
		try {
			for (byte i = 0; i <= ROUTEZOOMLEVEL; i++) {
				readData(i);
			}
			t.setBaseTilesRead(true);
		} catch (OutOfMemoryError oome) {
			t.setBaseTilesRead(true); // avoid endless loop in Trace
			logger.fatal(Locale.get("dictreader.DictReaderCrashOOM")/*DictReader thread crashed as out of memory: */ + oome.getMessage());
			oome.printStackTrace();
		} catch (IOException e) {
			t.setBaseTilesRead(true); // avoid endless loop in Trace
			ShareNav.getInstance().restart();
			logger.fatal(Locale.get("dictreader.FailedToLoadMap")/*Failed to load basic map data: */ + e.getMessage());
			e.printStackTrace();
		}

	}

	private void readData(byte zl) throws IOException {
		String filename = "/dict-" + zl + ".dat";
		//#debug info
		logger.info("open " + filename);
		try {
			InputStream is = Configuration.getMapResource(filename);
			if (is == null) {
				throw new IOException("Could not open " + filename);
			}
			// logger.info("open DataInputStream");
			DataInputStream ds = new DataInputStream(is);
			// logger.info("read Magic code");
			if (! "DictMid".equals(ds.readUTF())) {
				throw new IOException("Not a DictMid-file");
			}
			// logger.trace("read TileType");
			byte type = ds.readByte();
			// logger.trace("TileType=" + type);
			switch (type) {
			case Tile.TYPE_MAP:
				dict = new SingleTile(ds, 1, zl);
				break;
			case Tile.TYPE_CONTAINER:
				dict = new ContainerTile(ds, 1, zl);
				break;
			case Tile.TYPE_EMPTY:
				// empty tile;
				break;
			case Tile.TYPE_FILETILE:
				dict = new FileTile(ds, 1, zl);
				break;
			case Tile.TYPE_ROUTEDATA:
				// RouteData Tile
				dict = new RouteTile(ds, 1, zl);
				break;
			case Tile.TYPE_ROUTECONTAINER:
				// RouteData Tile
				dict = new RouteContainerTile(ds, 1, zl);
			default:
				break;
			}
			ds.close();
			
			t.setDict(dict, zl);
		} catch (IOException ioe) {
			// Special case zoom level 4, which is the routing zoom level.
			// If routing was disabled in Osm2ShareNav, this file won't
			// exist. Give a more helpful message.
			if (zl == ROUTEZOOMLEVEL) {
				logger.info("Routing is not enabled in this midlet");
				return;
			} else {
				throw ioe;
			}
		}
	}

}
