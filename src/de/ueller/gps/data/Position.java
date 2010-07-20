/**
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * See COPYING
 */
package de.ueller.gps.data;


/** TODO: Explain difference between Position and PositionMark */
public class Position {
	/** Latitude in degrees, Western values are negative */
	public float latitude;
	/** Longitude in degrees, Southern values are negative */
	public float longitude;
	/** Altitude above mean sea level or WGS84 geoid in meters */
	public float altitude;
	/** Speed over ground in m/s
	 * Normally filled with GPS speed directly from NMEA messages / JSR-179
	 * while position was created. Might also contain Float.NaN
	 * like it is returned by JSR-179 if the speed is unknown
	 */
	public float speed;
	/** Course in degrees (0..359)
	 * Normally filled with GPS course directly from NMEA messages / JSR-179
	 * while position was created. Might also contain Float.NaN
	 * like it is returned by JSR-179 if the course is unknown
	 */
	public float course;
	/** Positional dilution of precision */
	public float pdop = 0.0f;
	/** currentTimeMillis() of this position. */
	public long timeMillis;

	public Position(Position pos) {
		this.latitude = pos.latitude;
		this.longitude = pos.longitude;
		this.altitude = pos.altitude;
		this.speed = pos.speed;
		this.course = pos.course;
		this.timeMillis = pos.timeMillis;
	}

	public Position(float latitude, float longitude, float altitude,
			float speed, float course, int mode, long timeMillis) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.speed = speed;
		this.course = course;
		this.timeMillis = timeMillis;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("Position: ");
		sb.append(latitude).append("/").append(longitude).append("  ");
		sb.append("height: ").append(altitude).append("m   ");
		sb.append("Speed: ").append((speed * 3.6f)).append("km/h  ");
		sb.append("Course: ").append(course);
		return sb.toString();
	}
}
