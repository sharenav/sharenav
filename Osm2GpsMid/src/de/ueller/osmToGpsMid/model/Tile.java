package de.ueller.osmToGpsMid.model;

import java.io.DataOutputStream;
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
	
	public void write(DataOutputStream ds) throws IOException{
		switch (type){
			case 1:
				System.out.println("Type 1");
				ds.writeByte(1);
				ds.writeFloat(degToRad(bounds.minLat));
				ds.writeFloat(degToRad(bounds.minLon));
				ds.writeFloat(degToRad(bounds.maxLat));
				ds.writeFloat(degToRad(bounds.maxLon));
				ds.writeInt(fid);
				break;
			case 2:
				System.out.println("Type 2");
				ds.writeByte(2);
				ds.writeFloat(degToRad(bounds.minLat));
				ds.writeFloat(degToRad(bounds.minLon));
				ds.writeFloat(degToRad(bounds.maxLat));
				ds.writeFloat(degToRad(bounds.maxLon));
				t1.write(ds);
				t2.write(ds);
				break;
			case 3:
				System.out.println("Type 3");
				ds.writeByte(3);
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
