/*
 * Wegpunktretter - Copyright (c) 2012 mbaeurle at users dot sourceforge dot net
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
 * Uses code from GpsMid.
 * 
 * See file COPYING.
 */

package de.mbaeurle.wegpunktretter;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Wegpunktretter {
	/**
	 * 180/Pi
	 */
	final public static transient float FAC_RADTODEC =  180.0f/(float)Math.PI;

	public static void main(String[] args) {
		if (args.length != 2)
		{
			System.out.println("Please specify persistency file and target file as parameters.");
		}
		else
		{
			int num = 0;
			try {
				FileInputStream fileIS = new FileInputStream(args[0]);
				DataInputStream wayIS = new DataInputStream(fileIS);
				FileOutputStream oS = new FileOutputStream(args[1]);
				
				oS.write("<?xml version='1.0' encoding='UTF-8'?>\r\n".getBytes());
				oS.write("<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n".getBytes());
	
				// Ein "Record" =
				// 4 Bytes Recordlänge (inkl. dieser Länge...)
				// 4 Bytes unbekannt, wahrsch. Nutzdatenlänge
				// 2 Bytes Stringlänge
				// Max. 20 Bytes Name
				// 4 Bytes lat
				// 4 Bytes lon
				// 1 Byte zl (was auch immer das ist)
				// Rest Füllung
				while (wayIS.available() >= 0)
				{
					StringBuffer sb = new StringBuffer(128);
					int recSize = wayIS.readInt() - 8;
					// Don't know exactly what this number is
					int dummy = wayIS.readInt();
					byte[] bytes = new byte[recSize];
					int read = wayIS.read(bytes);
					if (read != recSize)
					{
						throw new IOException("Couldn't read record (" + recSize + " Bytes) completely.");
					}
					ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
					DataInputStream dis = new DataInputStream(bis);
					StringBuffer name = new StringBuffer(dis.readUTF());
					float lat = dis.readFloat();
					float lon = dis.readFloat();
					sb.append("<wpt lat='").append(lat * FAC_RADTODEC);
					sb.append("' lon='").append(lon * FAC_RADTODEC).append("' >\r\n");
					sb.append("<name>").append(name).append("</name>\r\n");
					sb.append("</wpt>\r\n");
					oS.write(sb.toString().getBytes());
					num++;
				}
				oS.write("</gpx>\r\n\r\n".getBytes());
				oS.close();
				fileIS.close();
				System.out.println(num + " waypoints read.");
			}
			catch (Exception e)
			{
				System.out.println(num + " waypoints read.");
				e.printStackTrace();
			}
		}
	}
}
