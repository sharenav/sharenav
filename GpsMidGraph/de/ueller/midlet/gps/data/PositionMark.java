package de.ueller.midlet.gps.data;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.ueller.gpsMid.mapData.SingleTile;

public class PositionMark extends PersistEntity{
	
	public float lat;
	public float lon;
	public Entity e;
	public SingleTile st=null;
	byte zl;
	byte searcType;
	public int nameIdx=-1;
	
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
			lat=ds.readFloat();
			lon=ds.readFloat();
			zl=ds.readByte();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.id=i;
	}

}
