package de.ueller.osmToGpsMid.model;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;


public class Tile {
	
//	float minLat,maxLat,minLon,maxLon;
	public Bounds bounds=new Bounds();
	public Tile t1=null;
	public Tile t2=null;
	public int fid;
	public byte type;
	public byte zl;
	public LinkedList<Way> ways=null;
	
	public Tile() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Tile(byte zl) {
		this.zl = zl;
	}


	public Tile(Bounds b) {
		bounds=b.clone();		
	}
	
	public void write(DataOutputStream ds,Integer deep,Sequence fid,String path) throws IOException{
		DataOutputStream lds;
		boolean openStream;
		System.out.println("Write Tile type=" + type + " deep=" + deep + " fid=" + fid);
		if (type == 2 && deep >= 6){
			System.out.println("Type 4");
			ds.writeByte(4);
			ds.writeFloat(degToRad(bounds.minLat));
			ds.writeFloat(degToRad(bounds.minLon));
			ds.writeFloat(degToRad(bounds.maxLat));
			ds.writeFloat(degToRad(bounds.maxLon));
			ds.writeShort(fid.get());
			openStream=true;
			FileOutputStream fo = new FileOutputStream(path+"/d"+zl+fid.get()+".d");
			lds = new DataOutputStream(fo);
			lds.writeUTF("DictMid");
			fid.inc();
			deep=1;
		} else {
			openStream=false;
			lds=ds;
			deep++;
		}
		switch (type){
			case 1:
				System.out.println("Type 1");
				lds.writeByte(1);
				lds.writeFloat(degToRad(bounds.minLat));
				lds.writeFloat(degToRad(bounds.minLon));
				lds.writeFloat(degToRad(bounds.maxLat));
				lds.writeFloat(degToRad(bounds.maxLon));
				lds.writeInt(this.fid);
//				ds.writeInt(ds.size());
				break;
			case 2:
				System.out.println("Type 2");
				lds.writeByte(2);
				lds.writeFloat(degToRad(bounds.minLat));
				lds.writeFloat(degToRad(bounds.minLon));
				lds.writeFloat(degToRad(bounds.maxLat));
				lds.writeFloat(degToRad(bounds.maxLon));
				
				t1.write(lds,deep,fid,path);
				t2.write(lds,deep,fid,path);
				break;
			case 3:
				System.out.println("Type 3");
				lds.writeByte(3);
//			case 4:
//				System.out.println("Type 4");
//				lds.writeByte(4);
//				lds.writeFloat(degToRad(bounds.minLat));
//				lds.writeFloat(degToRad(bounds.minLon));
//				lds.writeFloat(degToRad(bounds.maxLat));
//				lds.writeFloat(degToRad(bounds.maxLon));
//				lds.writeShort(fid);
		}
		if (openStream){
			lds.writeUTF("END"); // magig number
			lds.close();
		}
	}
		
	    public float degToRad(double deg) {
	        return (float) (deg * (Math.PI / 180.0d));
	    }

	    public Bounds recalcBounds(){
	    	Bounds b1=null;
	    	Bounds b2=null;
	    	Bounds ret=bounds.clone();
	    	if (type == 1){
	    		return bounds.clone();
	    	}
			if (t1 != null && t1.type==2){
				b1=t1.recalcBounds();
				ret=b1.clone();
			}
			if (t2 != null && t1.type==2){
				b2=t1.recalcBounds();
				if (ret != null){
					ret.extend(b2);
				}
			}
			return ret;
	    }
		
		public LinkedList<Way> getWays() {
			return ways;
		}
		
		public void setWays(LinkedList<Way> ways) {
			this.ways = ways;
		}
}
