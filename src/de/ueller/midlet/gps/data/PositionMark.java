package de.ueller.midlet.gps.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.ueller.midlet.gps.tile.SingleTile;

public class PositionMark {
	int id=-1;
	public String displayName;
	public float lat;
	public float lon;
	public Entity e;
	public SingleTile st=null;
	byte zl;
	byte searcType;
	public Short nameIdx=null;
	
	public byte[] toByte(){
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(bs);
		try {
			ds.writeUTF(displayName);
			ds.writeFloat(lat);
			ds.writeFloat(lon);
			ds.writeByte(zl);
			ds.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return bs.toByteArray();
	}
	
	public String toString(){
		return new String(id + ": "+displayName+"("+lat+"/"+lon+") " + st + e);
	}
	public PositionMark(float lat, float lon){
		this.lat = lat;
		this.lon = lon;
	}
	
	public PositionMark(int i, byte[] data) {
		DataInputStream ds = getByteInputStream(data);
		try {
			displayName=ds.readUTF();
			lat=ds.readLong();
			lon=ds.readLong();
			zl=ds.readByte();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.id=i;
	}
	
	protected DataInputStream getByteInputStream(byte[] data) {
		ByteArrayInputStream bs = new ByteArrayInputStream(data);
		DataInputStream ds = new DataInputStream(bs);
		return ds;
	}

}
