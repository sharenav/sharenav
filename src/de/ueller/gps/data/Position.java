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

import java.util.Date;

import de.ueller.midlet.gps.Logger;

public class Position {
	private static final Logger logger = Logger.getInstance(
			Position.class, Logger.TRACE);
	/**
	 * position in degrees
	 */
	public float latitude;
	public float longitude;
	public float altitude;
	/**
	 * Speed over ground in m/s
	 */
	public float speed;
	public float course;
	public float pdop = 0.0f;
	public int mode = -1;
	public Date date;

	public Position(Position pos) {
		this.latitude = pos.latitude;
		this.longitude = pos.longitude;
		this.altitude = pos.altitude;
		this.speed = pos.speed;
		this.course = pos.course;
		this.mode = pos.mode;
		this.date = pos.date;
	}

	public Position(float latitude, float longitude, float altitude,
			float speed, float course, int mode, Date date) {
		this.latitude = latitude;
		this.longitude = longitude;
		this.altitude = altitude;
		this.speed = speed;
		this.course = course;
		this.mode = mode;
		this.date = date;
	}

	public Position(String latitude, String longitude, String altitude,
			String speed, String course, String mode, String date, String time) {
		try {
			this.latitude = Float.parseFloat(latitude);
			this.longitude = Float.parseFloat(longitude);
			this.altitude = Float.parseFloat(altitude);
			this.speed = Float.parseFloat(speed);
			this.course = Float.parseFloat(course);
			this.mode = Integer.parseInt(mode);
		} catch (NumberFormatException e) {
			logger.exception("Failed to parse position", e);
		}
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
