package de.ueller.gpsMid.mapData;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.Way;


public class QueueDictReader extends QueueReader implements Runnable {
	
	private final Trace trace;
	public QueueDictReader(Trace trace){
		super();
		this.trace = trace;
		
	}
		
	public void readData(Tile t) throws IOException{
		FileTile tt=(FileTile) t;
//		logger.info("open /d"+tt.zl+tt.fid+".d");
		InputStream is = openFile("/d"+tt.zl+tt.fileId+".d");
		if (is == null){
//			logger.error("file inputStream /d"+tt.zl+tt.fileId+".d not found" );
			throw new IOException("File not found /d"+tt.zl+tt.fileId+".d" );
		}
//		logger.info("open DataInputStream");
		DataInputStream ds=new DataInputStream(is);
		if (ds == null){
//			logger.error("file DataImputStream "+url+" not found" );
			throw new IOException("DataStream not open for /d"+tt.zl+tt.fileId+".d" );
		}
//		end open data from JAR
//		logger.info("read Magic code");
		if (! "DictMid".equals(ds.readUTF())){
			throw new IOException("not a DictMid-file");
		}
//		logger.trace("read TileType");
		byte type=ds.readByte();
//		logger.trace("TileType="+type);
		Tile dict=null;
		switch (type) {
		case 1:
			dict=new SingleTile(ds,1,tt.zl);
			break;
		case 2:
    		dict=new ContainerTile(ds,1,tt.zl);
    		break;
		case 3:
			// empty tile;
			return;
		case 4:
			dict=new FileTile(ds,1,tt.zl);
			break;
		default:
			break;
		}
    	
    	tt.tile=dict;
		tt.inLoad=false;
		tt.lastUse=0;
		trace.newDataReady();
//		logger.info("DictReader ready "+ tt.fid);

//		}

	}
	public String toString(){
		int loop;
		StringBuffer ret=new StringBuffer();
		FileTile tt;
		ret.append("\nliving ");
		for (loop=0; loop < livingQueue.size(); loop++){
			tt=(FileTile) livingQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		ret.append("\nrequest ");
		for (loop=0; loop < requestQueue.size(); loop++){
			tt=(FileTile) requestQueue.elementAt(loop);
			ret.append(tt.toString());
			ret.append(" ");
		}
		return ret.toString();
	}
}
