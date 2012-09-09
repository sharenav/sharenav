/**
 * OSM2ShareNav 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav.model;

/**
 * @author hmueller
 * 
 */
public class HiLo {
	int lo = Integer.MAX_VALUE;
	int hi = 0;

	public void extend(int val) {
		if (val > this.hi) {
			this.hi = val;
		}
		if (val < this.lo) {
			this.lo = val;
		}
	}
	public void extend(HiLo hiLo) {
		if (hiLo.hi > this.hi) {
			this.hi = hiLo.hi;
		}
		if (hiLo.lo < this.lo) {
			this.lo = hiLo.lo;
		}
	}
}
