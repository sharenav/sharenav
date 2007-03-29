package de.ueller.midlet.gps.data;


public class IntPoint {
	public int x;
	public int y;
	
	public IntPoint(int x, int y) {
		super();
		this.x = x;
		this.y = y;
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
	
}
