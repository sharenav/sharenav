/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See file COPYING
 */

package net.sharenav.sharenav.mapdata;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.tile.ContainerTile;
import net.sharenav.sharenav.tile.FileTile;
import net.sharenav.sharenav.tile.SingleTile;
import net.sharenav.sharenav.tile.Tile;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.sharenav.ui.Trace;

import de.enough.polish.util.Locale;


public class QueueDictReader extends QueueReader implements Runnable {
	
	private final Trace trace;
	public QueueDictReader(Trace trace) {
		super("DictReader");
		this.trace = trace;
	}
		
	public void readData(Tile t, Object notifyReady) throws IOException {
		FileTile tt = (FileTile) t;
//		logger.info("open /d" + tt.getZoomLevel() + tt.fid + ".d");
		InputStream is = Configuration.getMapResource("/d" + tt.getZoomLevel() 
				+ "/" + tt.fileId + ".d");
		if (is == null) {
//			logger.error("file inputStream /d" + tt.getZoomLevel() + tt.fileId + ".d not found" );
			throw new IOException(Locale.get("queuedictreader.FileNotFound")/*File not found /d*/
					+ tt.getZoomLevel() + "/" + tt.fileId + ".d");
		}
//		logger.info("open DataInputStream");
		DataInputStream ds = new DataInputStream(is);
		if (ds == null) {
//			logger.error("file DataImputStream " + url + " not found" );
			is.close();
			throw new IOException(Locale.get("queuedictreader.DataStreamNotOpenForD")/*DataStream not open for /d*/
					+ tt.getZoomLevel() + "/" + tt.fileId + ".d" );
		}
//		end open data from JAR
//		logger.info("read Magic code");
		if (! "DictMid".equals(ds.readUTF())) {
			throw new IOException("not a DictMid-file");
		}
//		logger.trace("read TileType");
		byte type=ds.readByte();
//		logger.trace("TileType="+type);
		Tile dict = null;
		switch (type) {
		case 1:
			dict = new SingleTile(ds, 1, tt.getZoomLevel());
			break;
		case 2:
    		dict = new ContainerTile(ds, 1, tt.getZoomLevel());
    		break;
		case 3:
			// empty tile;
			return;
		case 4:
			dict = new FileTile(ds, 1, tt.getZoomLevel());
			break;
		default:
			break;
		}

		ds.close();

    	tt.setTile(dict);
		tt.setInLoad(false);
		tt.lastUse = 0;
		trace.newDataReady();
		if (notifyReady != null) {
			synchronized(notifyReady) {
				notifyReady.notifyAll();
			}
		}
//		logger.info("DictReader ready "+ tt.fid);

//		}
	}

	public String toString() {
		int loop;
		StringBuffer ret = new StringBuffer();
		FileTile tt;
		ret.append("\nliving ");
		for (loop = 0; loop < livingQueue.size(); loop++) {
			tt = (FileTile) livingQueue.elementAt(loop);
			ret.append(tt.toString())
			   .append(" ");
		}
		ret.append("\nrequest ");
		for (loop = 0; loop < requestQueue.size(); loop++) {
			tt = (FileTile) requestQueue.elementAt(loop);
			ret.append(tt.toString())
			   .append(" ");
		}
		return ret.toString();
	}
}
