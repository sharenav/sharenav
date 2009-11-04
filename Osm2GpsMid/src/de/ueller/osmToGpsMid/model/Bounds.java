/**
 * This file is part of OSM2GpsMid 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model;

import de.ueller.osmToGpsMid.MyMath;

/**
 * This class can hold the four values of a bounding box and provides methods
 * to makes checks against such a bounding box.
 */
public class Bounds implements Cloneable {
		public float minLat = 90f;
		public float maxLat = -90f;
		public float minLon = 180f;
		public float maxLon = -180f;

		public void extend(float lat, float lon) {
			if (lat < minLat) {
				minLat = lat;
			}
			if (lat > maxLat) {
				maxLat = lat;
			}
			if (lon < minLon) {
				minLon = lon;
			}
			if (lon > maxLon) {
				maxLon = lon;
			}
		}

		public void extend(double lat, double lon) {
			extend((float)lat, (float)lon);
		}

		public void extend(Bounds b) {
			extend(b.minLat, b.minLon);
			extend(b.maxLat, b.maxLon);
		}
		
		public boolean isMostlyIn(Bounds testBound) {
			float centLat = (testBound.maxLat + testBound.minLat) / 2;
			float centLon = (testBound.maxLon + testBound.minLon) / 2;
			if (centLat < minLat) return false;
			if (centLon < minLon) return false;
			if (centLat > maxLat) return false;
			if (centLon > maxLon) return false;
			return true;
		}

		public boolean isCompleteIn(Bounds testBound) {
			if (testBound.minLat < minLat) return false;
			if (testBound.minLon < minLon) return false;
			if (testBound.maxLat > maxLat) return false;
			if (testBound.maxLon > maxLon) return false;
			return true;
		}

		public boolean isIn(float lat, float lon) {
			if (lat < minLat) return false;
			if (lon < minLon) return false;
			if (lat > maxLat) return false;
			if (lon > maxLon) return false;
			return true;
		}
		
		public boolean isInOrAlmostIn(float lat, float lon) {
			if (lat < minLat - 0.005) return false;
			if (lon < minLon - 0.005) return false;
			if (lat > maxLat + 0.005) return false;
			if (lon > maxLon + 0.005) return false;
			return true;
		}
		
		public boolean isIn(double lat, double lon) {
			return isIn((float)lat, (float) lon);
		}

		public Bounds clone() {
			Bounds b = new Bounds();
			b.maxLat = maxLat;
			b.minLat = minLat;
			b.maxLon = maxLon;
			b.minLon = minLon;
			return b;
		}

		public String toString() {
			return ("[Bound (" + minLat + "|" + minLon+")(" + maxLat + "|" + maxLon +
				") fixptlatspan=" + (int)(MyMath.degToRad(maxLat - minLat) * MyMath.PLANET_RADIUS) + 
				" fixptlonspan=" + (int)(MyMath.degToRad(maxLon - minLon) * MyMath.PLANET_RADIUS) + 
				"]");
		}

		public String toPropertyString(int regionNr) {
			return	"region." + regionNr + ".lat.min = " + minLat + "\r\n" +
					"region." + regionNr + ".lon.min = " + minLon + "\r\n" +
					"region." + regionNr + ".lat.max = " + maxLat + "\r\n" +
					"region." + regionNr + ".lon.max = " + maxLon + "\r\n";
		}

}
