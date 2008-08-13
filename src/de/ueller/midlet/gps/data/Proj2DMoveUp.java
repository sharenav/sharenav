package de.ueller.midlet.gps.data;

public class Proj2DMoveUp {
	private double upDir;
	private float m00=1f;
	private float m01=0f;
	private float m10=0;
	private float m11=1f;

	public float getUpDir() {
		return (float)upDir;
	}

	public void setUpDir(float upDir) {
		this.upDir = upDir;
		m00=(float)Math.cos(upDir);
		m11=(float)Math.sin(upDir);
	}
	

}
