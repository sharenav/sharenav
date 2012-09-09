/*
 * ShareNav - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
 *          Copyright (c) 2011,2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net 
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
package net.sharenav.gps.location;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.enough.polish.util.Locale;

/**
 * 
 * This object contains the compass data structures
 * 
 */
public class Compass {
	public int compassID;
	public float direction;

	public Compass() {
		/**
		 * Default constructor;
		 */
	}

	public Compass(DataInputStream dis) throws IOException {
		direction = dis.readFloat();
	}

	public String toString() {
		String s = Locale.get("compass.CompassDirection")/*Compass direction*/ + "=" + direction + ")";
		return s;
	}

	public void serialise(DataOutputStream dos) throws IOException {
		dos.writeFloat(direction);
	}
}