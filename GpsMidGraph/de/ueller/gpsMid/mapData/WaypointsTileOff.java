package de.ueller.gpsMid.mapData;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
import java.util.Vector;

import javax.microedition.lcdui.Graphics;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.tile.PaintContext;


public class WaypointsTileOff extends Tile {
	private final static Logger logger = Logger.getInstance(WaypointsTileOff.class,Logger.DEBUG);
	RecordStore rswaypoints;
	Vector waypts;	
	
	public WaypointsTileOff() {
		init();
	}
	
	public void init() {		
		try {
			//RecordStore.deleteRecordStore("waypoints");
			rswaypoints = RecordStore.openRecordStore("waypoints", true);
			deserialize();
		} catch (RecordStoreFullException e) {
			logger.error("Recordstore is full while trying to open waypoints");
		} catch (RecordStoreNotFoundException e) {
			logger.error("Waypoints recordstore not found");
		} catch (RecordStoreException e) {
			logger.exception("RecordStoreException", e);
		}  catch (OutOfMemoryError oome) {
			logger.error("Out of memory loading waypoints");
		}				
	}
	
	private void addWayLocal(PositionMark wp) {
		logger.info("Adding waypoint: " + wp);
		waypts.addElement(wp);
		if (wp.lat < minLat)
			minLat = wp.lat;
		if (wp.lat > maxLat)
			maxLat = wp.lat;
		if (wp.lon < minLon)
			minLon = wp.lon;
		if (wp.lon > maxLon)
			maxLon = wp.lon;		
	}
	
	public void addWayPoint(PositionMark wp) {
		
		byte[] buf = wp.toByte();
		try {
			int id = rswaypoints.addRecord(buf, 0, buf.length);
			wp.id = id;
		} catch (RecordStoreNotOpenException e) {			
			init();
			addWayPoint(wp);
		} catch (RecordStoreFullException e) {
			logger.error("Record store is full, could not store waypoint");
			e.printStackTrace();
		} catch (RecordStoreException e) {
			logger.exception("Exception storing waypoint", e);
		}
		addWayLocal(wp);
	}
	
	public PositionMark[] getWaypoints() {
		PositionMark[] res = new PositionMark[waypts.size()];
		waypts.copyInto(res);;
		return res;
	}
	
	public void deleteWayPoint(PositionMark wp) {
		waypts.removeElement(wp);
		try {
			rswaypoints.deleteRecord(wp.id);
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRecordIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
/*	public void delete(Waypoint wp) {
		waypts.removeElement(wp);
		try {
			if (wp.rsIdx > -1) rswaypoints.deleteRecord(wp.rsIdx);			
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidRecordIDException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < wpListeners.size(); i++)
			((WaypointListener)wpListeners.elementAt(i)).waypointsChangend();
	}
*/
/*
	public Vector getPts() {
		return waypts;
	}
*/	
	public void close() throws RecordStoreNotOpenException, RecordStoreNotFoundException, RecordStoreException {		
		if (rswaypoints.getNumRecords() == 0) {
            String fileName = rswaypoints.getName();
            rswaypoints.closeRecordStore();
            RecordStore.deleteRecordStore(fileName);
        } else {
            rswaypoints.closeRecordStore();
        }
	}
	
	private void deserialize() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException {
		logger.info("Loading waypoints into tile");		
		waypts = new Vector();
		RecordEnumeration renum;
		try {
			renum = rswaypoints.enumerateRecords(null, null, false);
		} catch (RecordStoreNotOpenException e) {
			logger.error("RecordStore was not open");
			return;
		}
		while (renum.hasNextElement()) {
			int id;			
			id = renum.nextRecordId();			
			PositionMark waypt = new PositionMark(id,rswaypoints.getRecord(id));
			addWayLocal(waypt);						
		}		
	}
		
	public boolean cleanup(int level) {
		// TODO Auto-generated method stub
		return false;
	}

	public void getWay(PaintContext pc, PositionMark pm, Way w) {
		// TODO Auto-generated method stub
		
	}

	public void paint(PaintContext pc, byte layer) {		
		if (contain(pc) && layer == Tile.LAYER_NODE) {
			for (int i = 0; i < waypts.size(); i++) {
				PositionMark waypt = (PositionMark)waypts.elementAt(i);
				
				if (pc.getP().isPlotable(waypt.lat, waypt.lon)) {
					if (waypt.lat < pc.screenLD.radlat) {
						continue;
					}
					if (waypt.lon < pc.screenLD.radlon) {
						continue;
					}
					if (waypt.lat > pc.screenRU.radlat) {
						continue;
					}
					if (waypt.lon > pc.screenRU.radlon) {
						continue;
					}
					

					pc.getP().forward(waypt.lat, waypt.lon, pc.lineP2,true);
					pc.g.drawImage(pc.images.IMG_MARK,pc.lineP2.x,pc.lineP2.y,Graphics.HCENTER|Graphics.VCENTER);
					if ( Trace.getInstance().getConfig().getCfgBitState(Configuration.CFGBIT_WPTTEXTS) ) {
						pc.g.setColor(0,0,0);
						pc.g.drawString(waypt.displayName,pc.lineP2.x,pc.lineP2.y,Graphics.HCENTER|Graphics.BOTTOM);
					}
				}
				
			}
		}
		
	}

	public void walk (PaintContext pc, int i) {
	}

}
