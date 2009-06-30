package de.ueller.midlet.gps.data;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */


public class IntPoint {
	public int x;
	public int y;
	
	public IntPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	public IntPoint() {
	}
	
	public void set(IntPoint other){
		x=other.x;
		y=other.y;
	}

	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	public void setX(float x) {
		this.x = (int)x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	public void setY(float y) {
		this.y = (int)y;
	}

	public boolean approximatelyEquals(IntPoint other, int diff) {
		if (Math.abs(x - other.x) > diff ){
			return false;
		}
		if (Math.abs(y - other.y) > diff ){
			return false;
		}
		return true;
	}
	public boolean approximatelyEquals(IntPoint other) {
		return approximatelyEquals(other,3);
	}
	
	public boolean equals(IntPoint other) {
		if (x != other.x ){
			return false;
		}
		if (y != other.y){
			return false;
		}
		return true;
	}
	public String toString(){
		return "IntPoint("+x+","+y+")";
	}
}
