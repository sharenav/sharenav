package de.ueller.midlet.gps.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public class PersistEntity {

	public int id = -1;
	public String displayName;
	private int tracksize;

	protected DataInputStream getByteInputStream(byte[] data) {
		ByteArrayInputStream bs = new ByteArrayInputStream(data);
		DataInputStream ds = new DataInputStream(bs);
		return ds;
	}
	protected DataOutputStream getByteOutputStream() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(os);
		return ds;
	}
	/** 
	 * if PersistEntity is used as a gpxTrack this returns the number of Points in the Track
	 * @return number of points in track, int.min_value if no size is set
	 */
	public int getTrackSize(){
		if (tracksize > 0)
			return tracksize;
		return Integer.MIN_VALUE;
	}
	/** 
	 * sets the number of Points if this is used as a gps-track
	 * @param size number of points in track must be >= 0
	 * @throws IllegalArgumentException
	 */
	public void setTrackSize(int size) throws IllegalArgumentException{
		if (tracksize >= 0){
			tracksize = size;
		} else {
			throw new IllegalArgumentException();
		}
	}
}
