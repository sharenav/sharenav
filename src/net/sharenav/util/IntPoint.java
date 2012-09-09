/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.util;


public class IntPoint {
	public int x;
	public int y;
	
	public IntPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public IntPoint() {
	}
	
	public IntPoint set(IntPoint other) {
		x = other.x;
		y = other.y;
		return this;
	}

	public IntPoint set(int x1, int y1) {
		x = x1;
		y = y1;
		return this;
	}

	/** Returns x + other.x, y + other.y
	 * @param other The IntPoint which gets added
	 * @return This point plus other
	 */
	public IntPoint vectorAdd(IntPoint other) {
		return new IntPoint(this.x + other.x, this.y + other.y);
	}

	public IntPoint add(IntPoint other) {
		x = this.x + other.x;
		y = this.y + other.y;
		return this;
	}
	
	/** Returns a new IntPoint. The given IntPoint gets rotated by 90 degrees and
	 * added to the current IntPoint.
	 * @param other The given IntPoint
	 * @return Rotated IntPoint
	 */
	public IntPoint vectorAddRotate90(IntPoint other) {
		return new IntPoint(this.x + other.y, this.y - other.x );
	}
	
	/** Returns a new IntPoint with the value (current IntPoint - given Intpoint)
	 * This doesn't change the value of the current IntPoint xy values
	 * @param other IntPoint. The Point which gets subtracted
	 * @return
	 */
	public IntPoint vectorSubstract(IntPoint other) {
			return new IntPoint(this.x - other.x, this.y - other.y);
	}

	/**
	 * Returns x, y from current IntPoint multiplied the parameter
	 * @param multi
	 * @return
	 */
	public IntPoint vectorMultiply(float multi) {
		return new IntPoint((int)(this.x * multi), (int)(this.y * multi));
	}
	
	public double vectorMagnitude(IntPoint other) {
		return Math.sqrt( (this.x - other.x) * (this.x - other.x) + (this.y - other.y) * (this.y - other.y)) ;
	}

	/**
	 * Adds the given IntPoint multiplied by factor to the object
	 * @param other
	 * @param factor
	 * @return return the new IntPoint in needed
	 */
	public IntPoint add(IntPoint other, float factor) {
		x = (int)(this.x + other.x * factor);
		y = (int)(this.y + other.y * factor);
		return this;
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
		if (Math.abs(x - other.x) > diff ) {
			return false;
		}
		if (Math.abs(y - other.y) > diff ) {
			return false;
		}
		return true;
	}

	public boolean approximatelyEquals(IntPoint other) {
		return approximatelyEquals(other, 3);
	}
	
	public boolean equals(IntPoint other) {
		if (x != other.x ) {
			return false;
		}
		if (y != other.y) {
			return false;
		}
		return true;
	}

	public String toString() {
		return "IntPoint(" + x + "," + y + ")";
	}
}
