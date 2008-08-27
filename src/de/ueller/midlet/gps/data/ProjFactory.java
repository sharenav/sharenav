package de.ueller.midlet.gps.data;

public final class ProjFactory {

	public static final byte NORTH_UP=0;
	public static final byte MOVE_UP=1;
	
	public static byte type=MOVE_UP;
	
	public static Projection getInstance(Node center, int upDir, float scale, int width, int height){
		switch (type){
			case 0:return new Proj2D(center,scale,width,height);
			case 1:return new Proj2DMoveUp(center,upDir,scale,width,height);
		}
		return new Proj2D(center,scale,width,height);
	}
	
	public static void setProj(byte t){
		type=t;
	}
}
