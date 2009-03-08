package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

public class DictReader  implements Runnable {
	private Thread	processorThread;
	private final static Logger logger=Logger.getInstance(DictReader.class,Logger.INFO);
	private Tile dict;
	private final Trace	t;

	
	public DictReader(Trace t){
		super();
		this.t = t;
		processorThread = new Thread(this,"DictReader");
		processorThread.setPriority(Thread.MIN_PRIORITY+2);
		processorThread.start();
		//#debug trace
		logger.trace("DictReader Thread started");

	}

	public void run() {
		try {
			for (byte i=0;i<=4;i++) {
				readData(i);
			}
		} catch (OutOfMemoryError oome) {
			logger.fatal("DictReader thread crashed as out of memory: " + oome.getMessage());
			oome.printStackTrace();
		} catch (IOException e) {
			GpsMid.getInstance().restart();
			logger.fatal("Failed to load basic map data:" + e.getMessage());
			e.printStackTrace();
		}

	}
	private void readData(byte zl) throws IOException{

		//#debug error
		logger.info("open dict-"+zl+".dat");
		InputStream is = Configuration.getMapResource("/dict-"+zl+".dat");
		if (is == null) {
			//Special case zoom level 4, which is the routing zoom level
			//If routing was disabled in Osm2GpsMid, then this file shouln't
			//exist. Give a more helpful error message
			if (zl == 4) {
				logger.error("Routing is not enabled in this midlet");
				return;
			}
			logger.error("Could not open /dict-" + zl + ".dat");
			throw new IOException("DictMid-file not found");
		}
		//		    	logger.info("open DataInputStream");
		DataInputStream ds=new DataInputStream(is);
		//				logger.info("read Magic code");
		if (! "DictMid".equals(ds.readUTF())){
			throw new IOException("not a DictMid-file");
		}
		//				logger.trace("read TileType");
		byte type=ds.readByte();
		//				logger.trace("TileType="+type);
		switch (type) {
		case Tile.TYPE_MAP:
			dict=new SingleTile(ds,1,zl);
			break;
		case Tile.TYPE_CONTAINER:
			dict=new ContainerTile(ds,1,zl);
			break;
		case Tile.TYPE_EMPTY:
			// empty tile;
			break;
		case Tile.TYPE_FILETILE:
			dict=new FileTile(ds,1,zl);
			break;
		case Tile.TYPE_ROUTEDATA:
			// RouteData Tile
			dict=new RouteTile(ds,1,zl);
			break;
		case Tile.TYPE_ROUTECONTAINER:
			// RouteData Tile
			dict=new RouteContainerTile(ds,1,zl);
		default:
			break;
		}

		t.setDict(dict,zl);
	}

}
