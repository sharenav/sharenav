package de.ueller.midlet.gps.data;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 *          Copyright (c) 2008 Markus Baeurle mbaeurle at users dot sourceforge dot net
 * See Copying
 */

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.tile.C;


public class PositionMark extends PersistEntity {
	private final static Logger logger = Logger.getInstance(PositionMark.class, Logger.DEBUG);
	
	/** Constant to use if the elevation is invalid. Hopefully, nobody will
	 * use GpsMid for deep sea exploration. ;-) */ 
	public static final int INVALID_ELEVATION = -1000;
	
	/** Name of this position mark. */
	public String displayName;
	/** Latitude in radians */
	public float lat;
	/** Longitude in radians */
	public float lon;
	/** Elevation above mean sea level in meters */
	public int ele;
	/** Timestamp when this position mark was created. */
	public Date timestamp;
	/** The type of GPS fix when this position mark was created. */
	public byte fix;
	/** The number of satellites in use when this position mark was created. */
	public byte sats;
	/** The symbol to be used when displaying this position mark. Not used yet. */
	public byte sym;
	/** The type of this position mark, e.g. to mark those imported from outside GpsMid. */
	public byte type;
	public Entity entity;
	public float[] nodeLat;
	public float[] nodeLon;
	public int nameIdx = -1;

	public PositionMark(float lat, float lon) {
		this.lat = lat;
		this.lon = lon;
		this.ele = INVALID_ELEVATION;
		this.timestamp = new Date();
		this.fix = -1;
		this.sats = -1;
		this.sym = -1;
		this.type = -1;
	}

	public PositionMark(float lat, float lon, int ele, Date timestamp,
						byte fix, byte sats, byte sym, byte type) {
		this.lat = lat;
		this.lon = lon;
		this.ele = ele;
		this.timestamp = timestamp;
		this.fix = fix;
		this.sats = sats;
		this.sym = sym;
		this.type = type;
	}
	
	public PositionMark(int i, byte[] data) {
		DataInputStream ds = getByteInputStream(data);
		try {
			// Version is ignored at the moment but in the future we can 
			// use it to call routines which read old formats.
			int dummy = ds.readShort();
			displayName = ds.readUTF();
			lat = ds.readFloat();
			lon = ds.readFloat();
			ele = ds.readInt();
			timestamp = new Date(ds.readLong());
			fix = ds.readByte();
			sats = ds.readByte();
			sym = ds.readByte();
			type = ds.readByte();
		} catch (IOException ioe) {
			// Maybe reading failed because it is the old format without 
			// version and all the new attributes, so let's try it:
			logger.debug("Wpt " + i + " is probably in the old format.");
			ds = getByteInputStream(data);
			try {
				displayName = ds.readUTF();
				lat = ds.readFloat();
				lon = ds.readFloat();
				// zl was never used.
				int dummy = ds.readByte();
				ele = INVALID_ELEVATION;
				timestamp = new Date();
				fix = -1;
				sats = -1;
				sym = -1;
				type = -1;
				logger.debug("  Yes it is.");
			} catch (IOException ioe2) {
				logger.debug("  No, reading old format also failed.");
				// TODO Auto-generated catch block
				ioe2.printStackTrace();
			}
		}
		this.id = i;
	}

	public void setEntity(Way w, float lat[], float lon[]) {
		entity = w;
//		nodeLat = lat;
//		nodeLon = lon;
	}

	public byte[] toByte() {
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(bs);
		try {
			ds.writeShort(C.WAYPT_FORMAT_VERSION);
			if (displayName == null) {
				ds.writeUTF("");
			} else {
				ds.writeUTF(displayName);
			}
			ds.writeFloat(lat);
			ds.writeFloat(lon);
			ds.writeInt(ele);
			ds.writeLong(timestamp.getTime());
			ds.writeByte(fix);
			ds.writeByte(sats);
			ds.writeByte(sym);
			ds.writeByte(type);
			ds.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return bs.toByteArray();
	}

	public String toString() {
		return new String(id + ": " + displayName + "(" + 
						  (lat * MoreMath.FAC_RADTODEC) + "/" + 
						  (lon * MoreMath.FAC_RADTODEC) + ") " + entity);
	}
}
