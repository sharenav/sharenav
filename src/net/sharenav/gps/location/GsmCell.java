/*
 * ShareNav - Copyright (c) 2009 Kai Krueger apmonkey at users dot sourceforge dot net 
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

/**
 * 
 * This object contains the location and information of a single cell
 * 
 */
public class GsmCell {
	public int cellID;
	public short mcc;
	public short mnc;
	public int lac;
	public float lat;
	public float lon;

	public GsmCell() {
		/**
		 * Default constructor;
		 */
	}

	public GsmCell(int cellID, short mcc, short mnc, int lac) {
		this.cellID = cellID;
		this.mcc = mcc;
		this.mnc = mnc;
		this.lac = lac;
	}

	public GsmCell(DataInputStream dis) throws IOException {
		mcc = (short) dis.readShort();
		mnc = (short) dis.readShort();
		lac = dis.readInt();
		cellID = dis.readInt();
		lat = dis.readFloat();
		lon = dis.readFloat();
	}

	public String toString() {
		String s = "Cell (id=" + cellID + " mcc=" + mcc + " mnc=" + mnc
				+ " lac=" + lac + "  coord=" + lat + "|" + lon + ")";
		return s;
	}

	public void serialise(DataOutputStream dos) throws IOException {
		dos.writeShort(mcc);
		dos.writeShort(mnc);
		dos.writeInt(lac);
		dos.writeInt(cellID);
		dos.writeFloat(lat);
		dos.writeFloat(lon);
	}
}