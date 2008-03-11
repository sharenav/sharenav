package de.ueller.midlet.gps.data;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.ueller.gpsMid.mapData.SingleTile;

public class PositionMark extends PersistEntity{
	public String displayName;
	public float lat;
	public float lon;
	public float onWayLat;
	public float onWayLon;
	public Entity e;
//	public SingleTile st=null;
	public float[] nodeLat;
	public float[] nodeLon;
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
		return new String(id + ": "+displayName+"("+(lat*MoreMath.FAC_RADTODEC)+"/"+lon*(MoreMath.FAC_RADTODEC)+") " + e);
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
	
	public void setEntity(Way w,float lat[],float lon[]){
		e=w;
		int count=w.path.length;
		nodeLat=new float[count];
		nodeLon=new float[count];

//		int pathCount = w.paths.length;
//		nodeLat = new float[pathCount][];
//		nodeLon = new float[pathCount][];
//		for (int pi=0; pi < pathCount; pi++){
//			short path[]=w.paths[pi];
//			int nodeCount=path.length;
//			nodeLat[pi]=new float[nodeCount];
//			nodeLon[pi]=new float[nodeCount];
//			float nlat[] = nodeLat[pi];
//			float nlon[] = nodeLon[pi];
//			for (int ni=0;ni < nodeCount; ni++){
//				int nodeIdx=path[ni];
//				nlat[ni]=lat[nodeIdx];	
//				nlon[ni]=lon[nodeIdx];	
//			}
//		}
		
	}
	

}
