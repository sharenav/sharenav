//#if polish.api.finland
/**
 * This file is part of ShareNav 
 *
 * Possibly copyright (C) 2012 and credits jkpj and others where applicable; constants and
 * formulas probably might not be covered by copyright, might thus be nothing much left to copyright
 * except the credits; in either case, thanks to the code writers below for publishing the code.
 * Written by jkpj for GpsMid (later development as ShareNav) based on the sources mentioned below; if jkpj's copyright
 * applies, the copyright info (GPLv2 or later) immediately below applies.
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
 */

package net.sharenav.util;

import java.lang.Math;
import java.io.InputStream;

import net.sharenav.util.MoreMath;
import net.sharenav.sharenav.data.PositionMark;

/*
 * convert between ETRS­TM35FIN and WGS84 coordinates
 * implements formulas from recommendation:
 *   http://docs.jhs-suositukset.fi/jhs-suositukset/JHS154_liite1/JHS154_liite1.html
 * java conversion functions based on formulas from Olli Lammi's fetch_map coordinates.py
 * from fetch_map at http://olammi.iki.fi/sw/fetch_map/
 * also looked at:
 * https://code.google.com/p/etrstm35fin2wgs84/source/browse/trunk/src/com/etrstm35fin2wgs84/MainActivity.java?r=2
 */

public class ETRSTM35FINconvert {
        private static final double ETRSTM35FIN_min_y=6582464.0358;
	private static final double ETRSTM35FIN_max_y=7799839.8902;
	private static final double ETRSTM35FIN_min_x=50199.4814;
	private static final double ETRSTM35FIN_max_x=761274.6247;

	private static final double Ca=6378137.0;
	private static final double Cb=6356752.314245;
	private static final double Cf=1.0 / 298.257223563;
	private static final double Ck0=0.9996;
	private static final double Clo0=Math.toRadians(27.0);
	private static final double CE0=500000.0;
	private static final double Cn=Cf/(2.0-Cf);
	private static final double CA1= Ca / (1.0 + Cn) * (1.0 + (MoreMath.pow((float)Cn, (float)2.0)) / 4.0 + (MoreMath.pow((float)Cn, (float)4.0)) / 64.0);
	private static final double Ce= Math.sqrt((2.0 * Cf - MoreMath.pow((float)Cf,(float)2.0)));

	private static final double Ch1 = 1.0/2.0 * Cn - 2.0/3.0 * (MoreMath.pow((float)Cn,(float)2.0)) + 37.0/96.0 * (MoreMath.pow((float)Cn,(float)3.0)) - 1.0/360.0 * (MoreMath.pow((float)Cn, (float)4.0));
	private static final double Ch2 = 1.0/48.0 * ( MoreMath.pow((float)Cn,(float)2.0)) + 1.0/15.0 * ( MoreMath.pow((float)Cn,(float)3.0)) - 437.0/1440.0 * ( MoreMath.pow((float)Cn,(float)4.0));
	private static final double Ch3 = 17.0/480.0 * (MoreMath.pow((float)Cn,(float)3.0)) - 37.0/840.0 * (MoreMath.pow((float)Cn,(float)4.0));
	private static final double Ch4 = 4397.0/161280.0 * ( MoreMath.pow((float)Cn,(float)4.0));

	private static final double Ch1p =  1.0/2.0 * Cn - 2.0/3.0 * ( MoreMath.pow((float)Cn,(float)2.0)) + 5.0/16.0 * (MoreMath.pow((float)Cn,(float)3.0)) - 41.0/180.0 * (MoreMath.pow((float)Cn, (float)4.0));
	private static final double Ch2p = 13.0/48.0 * ( MoreMath.pow((float)Cn,(float)2.0)) + 3.0/5.0 * ( MoreMath.pow((float)Cn,(float)3.0)) + 557.0/1440.0 * ( MoreMath.pow((float)Cn,(float)4.0));
	private static final double Ch3p = 61.0/240.0 * (MoreMath.pow((float)Cn,(float)3.0)) - 103.0/140.0 * (MoreMath.pow((float)Cn,(float)4.0));
	private static final double Ch4p = 49561.0/161280.0 * ( MoreMath.pow((float)Cn,(float)4.0));

	public static PositionMark etrsToLatLon(float Ncoord, float Ecoord) {
                // Check the values entered are inside the valid area
                if (Ecoord < ETRSTM35FIN_min_x || Ecoord > ETRSTM35FIN_max_x
		    || Ncoord < ETRSTM35FIN_min_y || Ncoord > ETRSTM35FIN_max_y) {
			// outside valid area, can't be converted
                        return null;
                }

		double E = Ncoord / (CA1 * Ck0);
		double nn = (Ecoord - CE0) / (CA1 * Ck0);
		double E1p = Ch1 * Math.sin(2.0 * E) * MoreMath.cosh((float) (2.0 * nn));
		double E2p = Ch2 * Math.sin(4.0 * E) * MoreMath.cosh((float) (4.0 * nn));
		double E3p = Ch2 * Math.sin(6.0 * E) * MoreMath.cosh((float) (6.0 * nn));
		double E4p = Ch3 * Math.sin(8.0 * E) * MoreMath.cosh((float) (8.0 * nn));

		double nn1p = Ch1 * Math.cos(2.0 * E) * MoreMath.sinh((float) (2.0 * nn));
		double nn2p = Ch2 * Math.cos(4.0 * E) * MoreMath.sinh((float) (4.0 * nn));
		double nn3p = Ch3 * Math.cos(6.0 * E) * MoreMath.sinh((float) (6.0 * nn));
		double nn4p = Ch4 * Math.cos(8.0 * E) * MoreMath.sinh((float) (8.0 * nn));

		double Ep = E - E1p - E2p - E3p - E4p;

		double nnp = nn - nn1p - nn2p - nn3p - nn4p;
		double be = MoreMath.asin((float) (Math.sin(Ep) / MoreMath.cosh((float) nnp)));

		double Q = MoreMath.asinh((float) Math.tan(be));
		double Qp = Q + Ce * MoreMath.atanh((float) (Ce * MoreMath.tanh((float) Q)));
		Qp = Q + Ce * MoreMath.atanh((float) (Ce * MoreMath.tanh((float) Qp)));
		Qp = Q + Ce * MoreMath.atanh((float) (Ce * MoreMath.tanh((float) Qp)));
		Qp = Q + Ce * MoreMath.atanh((float) (Ce * MoreMath.tanh((float) Qp)));

		PositionMark pm = new PositionMark((float) MoreMath.atan(MoreMath.sinh((float)Qp)),
				    (float) (Clo0 + MoreMath.asin((float) (MoreMath.tanh((float)nnp) / Math.cos(be)))));
		//System.out.println("EtrsToLatLon lat: " + (float) (float) MoreMath.atan(MoreMath.sinh((float)Qp)));
		//System.out.println("EtrsToLatLon lon: " + (float) (float) (Clo0 + MoreMath.asin((float) (MoreMath.tanh((float)nnp) / Math.cos(be)))));
		//System.out.println("EtrsToLatLon returned: " + pm);
		return pm;
	}
	public static PositionMark latlonToEtrs(float lat, float lon) {

		double Q = MoreMath.asinh((float) Math.tan(lat)) - Ce * MoreMath.atanh((float) (Ce * Math.sin(lat)));
		double be = MoreMath.atan(MoreMath.sinh((float) Q));
		double nnp = MoreMath.atanh((float) (Math.cos(be) * Math.sin(lon - Clo0)));
		double Ep = MoreMath.asin((float) (Math.sin(be) * MoreMath.cosh((float) nnp)));
		double E1 = Ch1p * Math.sin(2.0 * Ep) * MoreMath.cosh((float) (2.0 * nnp));
		double E2 = Ch2p * Math.sin(4.0 * Ep) * MoreMath.cosh((float) (4.0 * nnp));
		double E3 = Ch3p * Math.sin(6.0 * Ep) * MoreMath.cosh((float) (6.0 * nnp));
		double E4 = Ch4p * Math.sin(8.0 * Ep) * MoreMath.cosh((float) (8.0 * nnp));
		double nn1 = Ch1p * Math.cos(2.0 * Ep) * MoreMath.sinh((float) (2.0 * nnp));
		double nn2 = Ch2p * Math.cos(4.0 * Ep) * MoreMath.sinh((float) (4.0 * nnp));
		double nn3 = Ch3p * Math.cos(6.0 * Ep) * MoreMath.sinh((float) (6.0 * nnp));
		double nn4 = Ch4p * Math.cos(8.0 * Ep) * MoreMath.sinh((float) (8.0 * nnp));
		double E = Ep + E1 + E2 + E3 + E4;
		double nn = nnp + nn1 + nn2 + nn3 + nn4;

		//System.out.println("latLonToEtrs lat: " + (float) (CA1 * E * Ck0));
		//System.out.println("latLonToEtrs lon: " + (float) (CA1 * nn * Ck0 + CE0));
		PositionMark pm = new PositionMark((float) (CA1 * E * Ck0),
				    (float) (CA1 * nn * Ck0 + CE0));
		//System.out.println("latLonToEtrs returned: " + pm);
		return pm;
	}
}
//#endif
