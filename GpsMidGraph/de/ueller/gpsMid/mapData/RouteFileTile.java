package de.ueller.gpsMid.mapData;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.routing.Connection;
import de.ueller.midlet.gps.routing.RouteNode;
import de.ueller.midlet.gps.routing.RouteTileRet;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.QueueableTile;

public class RouteFileTile extends RouteBaseTile {
	byte zl;
	RouteBaseTile tile=null;
	
	private final static Logger logger=Logger.getInstance(FileTile.class,Logger.INFO);

	public RouteFileTile(DataInputStream dis, int deep, byte zl) throws IOException {
//       	logger.info("create deep:"+deep + " zoom:"+zl);
		minLat = dis.readFloat();
		minLon = dis.readFloat();
		maxLat = dis.readFloat();
		maxLon = dis.readFloat();
    	minId=dis.readInt();
    	maxId=dis.readInt();
		fileId=dis.readShort();

		this.zl=zl;
//       	logger.debug("ready "+deep + " zl="+ zl + " fid="+fid);

	}

	public boolean cleanup(int level) {
		if (tile != null) {
			if (level > 0 && permanent){
				return false;
			}
			// logger.info("test tile unused fid:" + fileId + "c:"+lastUse);
			if (lastUse > level) {
				tile=null;
				//#debug debug
				 logger.debug("discard content for " + this);
				return true;
			} else {
				tile.cleanup(level);
			}
			lastUse++;
		}
		return false;
	}

	public String toString() {
		return "RFT" + zl + "-" + fileId + ":" + lastUse;
	}

	public RouteNode getRouteNode(int id) {
		if (minId <= id && maxId >= id){
			if (tile==null){
				try {
					loadTile();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
			return tile.getRouteNode(id);
		}
		return null;
	}

	public RouteNode getRouteNode(RouteNode best, float lat, float lon) {
		if (contain(lat,lon,0.03f)){
			if (tile == null){
				try {
					loadTile();
				} catch (IOException e) {
					logger.silentexception("loadTile failed", e);
					return null;
				}
			}
			return tile.getRouteNode(best, lat, lon);
		}
		return null;
	}

	private void loadTile() throws IOException {
            //#debug debug
			logger.debug("load Tile /d"+zl+fileId+".d");
			InputStream is = Configuration.getMapResource("/d"+zl+fileId+".d");
			if (is == null){
//				logger.error("file inputStream /d"+tt.zl+tt.fileId+".d not found" );
				throw new IOException("File not found /d"+zl+fileId+".d" );
			}
//			logger.info("open DataInputStream");
			DataInputStream ds=new DataInputStream(is);
			if (ds == null){
//				logger.error("file DataImputStream "+url+" not found" );
				throw new IOException("DataStream not open for /d"+zl+fileId+".d" );
			}
//			end open data from JAR
//			logger.info("read Magic code");
			if (! "DictMid".equals(ds.readUTF())){
				throw new IOException("not a DictMid-file");
			}
//			logger.trace("read TileType");
			byte type=ds.readByte();
//			logger.trace("TileType="+type);
			RouteBaseTile dict=null;
			switch (type) {
			case TYPE_ROUTEDATA:
				dict=new RouteTile(ds,1,zl);
				break;
			case TYPE_ROUTECONTAINER:
	    		dict=new RouteContainerTile(ds,1,zl);
	    		break;
			case 3:
				// empty tile;
				return;
			case TYPE_ROUTEFILE:
				dict=new RouteFileTile(ds,1,zl);
				break;
			default:
				break;
			}
	    	
	    	tile=dict;
			lastUse=0;
			//#debug debug
			logger.debug("DictReader ready "+ fileId);

//			}

	}

	public void paint(PaintContext pc, byte layer) {
		if (tile == null){
			try {
				loadTile();
			} catch (IOException e) {
				logger.silentexception("loadTile failed", e);
			}
		}
		tile.paint(pc, layer);
	}

	public Connection[] getConnections(int id, RouteBaseTile rootTile,boolean bestTime) {
		if (minId <= id && maxId >= id){
			if (tile==null){
				try {
					loadTile();
				} catch (IOException e) {
					logger.silentexception("loadTile failed", e);
					return null;
				}
			}
			lastUse=0;
			return tile.getConnections(id, rootTile,bestTime);
		}
		return null;
	}

	public RouteNode getRouteNode(float lat, float lon) {
		if (contain(lat,lon)){
			if (tile == null){
				try {
					loadTile();
				} catch (IOException e) {
					logger.silentexception("loadTile failed", e);
					return null;
				}
			}
			return tile.getRouteNode(lat, lon);
		}
		return null;
	}

	public RouteNode getRouteNode(float lat,float lon,RouteTileRet retTile){
		RouteNode ret=null;
		if (contain(lat,lon)){
			if (tile == null){
				try {
					loadTile();
				} catch (IOException e) {
					logger.silentexception("loadTile failed", e);
					return null;
				}
			}
			ret= tile.getRouteNode(lat, lon,retTile);
			if (ret != null){
				this.permanent=true;
			}
		}
		return ret;
	}	
}
