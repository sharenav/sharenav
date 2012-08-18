/*
 * Trackretter - Copyright (c) 2012 mbaeurle at users dot sourceforge dot net
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

package de.mbaeurle.trackretter;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.Date;

public class Trackretter {

	public static void main(String[] args) {
		if (args.length != 2)
		{
			System.out.println("Please specify persistency file and target file as parameters.");
		}
		else
		{
			try {
				FileInputStream fileIS = new FileInputStream(args[0]);
				DataInputStream trackIS = new DataInputStream(fileIS);
				FileOutputStream oS = new FileOutputStream(args[1]);
				
				oS.write("<?xml version='1.0' encoding='UTF-8'?>\r\n".getBytes());
				oS.write("<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n".getBytes());
				oS.write("<trk>\r\n<trkseg>\r\n".getBytes());						
	
				// Ein "Record" = 19 Bytes:
				// 4 Bytes lat
				// 4 Bytes lon
				// 2 Bytes ele
				// 8 Bytes Zeitstempel
				// 1 Byte Geschw.
				while (trackIS.available() >= 19)
				{
					StringBuffer sb = new StringBuffer(128);
					sb.append("<trkpt lat='").append(trackIS.readFloat()).append("' lon='").append(trackIS.readFloat()).append("' >\r\n");
					sb.append("<ele>").append(trackIS.readShort()).append("</ele>\r\n");
					sb.append("<time>").append(formatUTC(new Date(trackIS.readLong()))).append("</time>\r\n");
					sb.append("</trkpt>\r\n");				
					// Read extra bytes in the buffer, that are currently not written to the GPX file.
					// Will add these at a later time.
					trackIS.readByte(); //Speed			
					oS.write(sb.toString().getBytes());
				}
				oS.write("</trkseg>\r\n</trk>\r\n".getBytes());
				oS.write("</gpx>\r\n\r\n".getBytes());
				oS.close();
				trackIS.close();
			}
			catch (Exception e)
			{
				System.out.println("Exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Formats an integer to 2 digits, as used for example in time.
	 * I.e. a 0 gets printed as 00. 
	 **/
	private static final String formatInt2(int n) {
		if (n < 10) {
			return "0" + n;
		} else {
			return Integer.toString(n);
		}
			
	}	

	/**
	 * Date-Time formater that corresponds to the standard UTC time as used in XML
	 * @param time
	 * @return
	 */
	private static final String formatUTC(Date time) {
		// This function needs optimising. It has a too high object churn.
		Calendar c = null;
		if (c == null)
			c = Calendar.getInstance();
		c.setTime(time);
		return c.get(Calendar.YEAR) + "-" + formatInt2(c.get(Calendar.MONTH) + 1) + "-" +
		formatInt2(c.get(Calendar.DAY_OF_MONTH)) + "T" + formatInt2(c.get(Calendar.HOUR_OF_DAY)) + ":" +
		formatInt2(c.get(Calendar.MINUTE)) + ":" + formatInt2(c.get(Calendar.SECOND)) + "Z";		 
		
	}

}
