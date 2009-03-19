package net.fatehi;

// <pre>
//
//    XXXXXX                      XXXX              XXX
//   XX    X                     XX  XX              XX
//   XX       XX  XXX   XX XXX  XX        XXXXX      XX     XXXXX
//    XXXXX   XX  XX    XXX XX  XX            X      XX    XX   XX
//        XX  XX  XX    XX  XX  XX    X  XXXXXX      XX    XX
//   X    XX  XX  XX    XX  XX   XX  XX  X   XX      XX    XX   XX
//   XXXXXX    XXX XX   XX  XX    XXXX   XXXXX X    XXXX    XXXXX
//
//                      Copyright 2001, Sualeh Fatehi
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//

// Needed for atan2
import de.ueller.midlet.gps.data.MoreMath;

/**
 * Computes the times of sunrise and sunset for a specified date and location.
 * Also finds the Sun's co-ordinates (declination and right ascension) for a
 * given hour.
 * <p>
 * Algorithms from "Astronomy on the Personal Computer" by Oliver Montenbruck
 * and Thomas Pfleger. Springer Verlag 1994. ISBN 3-540-57700-9.
 * <p>
 * This is a reasonably accurate and very robust procedure for sunrise that
 * will handle unusual cases, such as the one day in the year in arctic
 * latitudes that the sun rises, but does not set. It is, however, very
 * computationally-intensive.
 * <br><br>
 * Based on
 * <a href="http://www.merrymeet.com/minow/sunclock/SunClock.html">SunClock</a>
 * by Martin Minow.
 * Minor changes (2.8 -> 2.9) for use in GpsMid by mbaeurle at users.sourceforge.net.
 * @author <a href="mailto:sualeh@rocketmail.com?subject=SunCalc">Sualeh Fatehi</a>
 * @version 2.9
 */
public class SunCalc
{


    //******************  Class Members ******************
    //******************


    private double _latitude = 0;        // Latitude in degrees, North positive
    private double _longitude = 0;       // Longitude in degrees, East positive
    private double _tzOffset = 0;        // Timezone offset from GMT, in hours
    private int _year = 0;               // Four digit year
    private int _month = 0;              // Month, 1 to 12
    private int _day = 0;                // Day, 1 to 31


    //******************  Constants ************************
    //******************


   /*
    * The solar co-ordinates calculation returns values in an array.
    * Use these constants to access elements of the array.
    */
    public static final int DECLINATION      = 0;
    public static final int RIGHTASCENSION   = 1;

   /*
    * These values define the location of the horizon for an observer
    * at sea level.
    * <br><pre>
    *    -0� 50'    Sunrise/ Sunset
    *    -6�        Civil Twilight (default twilight)
    *   -12�        Nautical Twilight
    *   -18�        Astronomical Twilight
    * </pre><br>
    * Note that these values are related to the horizon (90� from the
    * azimuth). If the observer is above or below the horizon, the
    * correct adopted true sunrise/sunset altitude in degrees is
    * -(50/60) - 0.0353 * sqrt(height in meters);
    * <br>
    * Sunrise is defined as the time when the apparent altitlude of the upper
    * limb of the Sun will be -50 arc minutes below the horizon. This takes
    * into account refraction and solar semi-diameter effects.
    */
    public static final double SUNRISE_SUNSET        = -(50.0 / 60.0);
    public static final double CIVIL_TWILIGHT        = -6.0;
    public static final double NAUTICAL_TWILIGHT     = -12.0;
    public static final double ASTRONOMICAL_TWILIGHT = -18.0;
    public static final double TWILIGHT = CIVIL_TWILIGHT;

   /*
    * The sunrise algorithm returns values in an array.
    * Use these constants to access elements of the array.
    */
    public static final int RISE = 0;
    public static final int SET = 1;

   /*
    * ABOVE_HORIZON and BELOW_HORIZON are returned for sun
    * calculations where the astronomical object does not cross the
    * horizon.
    */
    public static final double ABOVE_HORIZON = Double.POSITIVE_INFINITY;
    public static final double BELOW_HORIZON = Double.NEGATIVE_INFINITY;


    //******************  Accessor methods ******************
    //******************


  /**
    * Latitude in degrees, North positive.
    */
    public double getLatitude()  {
        return _latitude;
    }

  /**
    * Latitude in degrees, North positive.
    */
    public void setLatitude(double latitude)  {

        if (Math.abs(latitude) > 90)  {
            throw new IllegalArgumentException("Out of range");
        }
        _latitude = latitude;
    }


  /**
    * Longitude in degrees, East positive.
    */
    public double getLongitude()  {
        return _longitude;
    }

  /**
    * Longitude in degrees, East positive.
    */
    public void setLongitude(double longitude)  {

        if (Math.abs(longitude) > 180)  {
            throw new IllegalArgumentException("Out of range");
        }
        _longitude = longitude;
    }


  /**
    * Timezone offset from GMT, in hours.
    */
    public double getTimeZoneOffset()  {
        return _tzOffset;
    }

  /**
    * Timezone offset from GMT, in hours.
    */
    public void setTimeZoneOffset(double timeZoneOffset)  {

        if (Math.abs(timeZoneOffset) > 13)  {
            throw new IllegalArgumentException("Out of range");
        }
        _tzOffset = timeZoneOffset;
    }


  /**
    * Four digit year.
    */
    public int getYear()  {
        return _year;
    }

  /**
    * Four digit year.
    */
    public void setYear(int year)  {
        if (year < 1500 || year > 3000)  {
            throw new IllegalArgumentException("Out of range");
        }
        _year = year;
    }


  /**
    * Month, 1 to 12.
    */
    public int getMonth()  {
        return _month;
    }

  /**
    * Month, 1 to 12.
    */
    public void setMonth(int month)  {
        if (month < 1 || month > 12)  {
            throw new IllegalArgumentException("Out of range");
        }
        _month = month;
    }


  /**
    * Day, 1 to 31.
    */
    public int getDay()  {
        return _day;
    }

  /**
    * Day, 1 to 31.
    */
    public void setDay(int day)  {
        if (day < 1 || day > 31)  {
            throw new IllegalArgumentException("Out of range");
        }
        _day = day;
    }



    //******************  Astronomical calculations ******************
    //******************


  /**
    * <br>
    * Modified Julian Day Number
    * <br><br>
    * Since  most days within about 150 years of  the  present  have Julian
    * day numbers beginning  with "24", Julian day numbers within this
    * 300-odd-year period can be abbreviated. In 1975 the convention of
    * the modified Julian day number was adopted:
    * <br>
    * Given  a Julian day number  JD,  the  modified Julian  day  number MJD
    * is defined as  MJD  = JD - 2,400,000.5.  This has two purposes:
    * <ol><li>
    * Days begin at  midnight rather  than noon.
    * </li><li>
    * For dates  in the period from 1859  to about 2130 only five  digits
    * need to be used to specify the date rather than seven.
    * </li></ol><br>
    * MJD 0  thus corresponds to  JD 2,400,000.5,  which is twelve  hours
    * after  noon on  JD 2,400,000 = 1858-11-16 (Gregorian or  Common
    * Era).  Thus MJD 0 designates the midnight of November 16th/17th,
    * 1858,  so day 0 in  the  system of modified Julian day numbers is
    * the day 1858-11-17 CE.
    * <br><br>
    * Excerpted  from "Julian  Day  Numbers" by Peter Meyer
    * @see
    * <a href="http://serendipity.magnet.ch/hermetic/cal_stud/jdn.htm#mjd">
    * Julian  Day  Numbers by Peter Meyer</a>
    */
    private double calcModifiedJulianDay()
    {

        double MJD_OFFSET = 2400000.5; // const

        int intYear = _year;
        int intMonth = _month;
        int intDay = _day;

        double JulianDayNumber;

        JulianDayNumber = (1461 * (intYear + 4800 + (intMonth - 14) / 12)) / 4 +
            (367 * (intMonth - 2 - 12 * ((intMonth - 14) / 12))) / 12 -
            (3 * ((intYear + 4900 + (intMonth - 14) / 12) / 100)) / 4 +
            intDay - 32075;

        return (JulianDayNumber - MJD_OFFSET);

    } // end calcModifiedJulianDay()


  /**
    * Compute the sine of the solar altitude (the angular distance
    * of the sun to the horizon from the view of an observer) for a given time,
    * and location.
    * <br><br>
    * Based on
    * <a href="http://www.merrymeet.com/minow/sunclock/SunClock.html">SunClock</a>
    * by Martin Minow.
    *
    * @param  hour Hour past midnight (for the specified day)
    * @return The sine of the solar altitude.
    *
    * @see <a href="http://www.merrymeet.com/minow/sunclock/Sun.java">Sun.java</a>
    */
    private double sinAltitude(double hour)
    {

        double latitide = _latitude;
        double longitude = _longitude;
        double TZOffset = _tzOffset;

        double tau;
        double[] solarcoords;
        double result;

        solarcoords = calcSolarCoordinates(hour);

        tau = 15.0 * (calcLocalMeanSiderealTime(hour) - solarcoords[RIGHTASCENSION]);

        result = sin(latitide) * sin(solarcoords[DECLINATION])
               + (cos(latitide) * cos(solarcoords[DECLINATION]) * cos(tau));

        return result;

    } // end sinAltitude()


  /**
    * Compute Local Mean Sidereal Time (LMST).
    * <br>
    * (Note: While Astronomy on the Personal Computer reckons longitude
    * positive towards the West, this routine recons it positive
    * towards the East.
    * <br><br>
    * Based on
    * <a href="http://www.merrymeet.com/minow/sunclock/SunClock.html">SunClock</a>
    * by Martin Minow.
    *
    * @param hour The actual time for local mean sidereal time.
    * @return  The local mean sidereal time.
    *
    * @see <a href="http://www.merrymeet.com/minow/sunclock/Astro.java">Astro.java</a>
    */
    private double calcLocalMeanSiderealTime(double hour)
    {

        double longitude = _longitude;
        double TZOffset = _tzOffset;

        double MJDHour;
        double MJDHour0;
        double UT;
        double T0;
        double GMST;
        double LMST;

        MJDHour = Math.floor(calcModifiedJulianDay()) - (TZOffset / 24.0) + (hour / 24.0);
        MJDHour0 = Math.floor(MJDHour);
        UT = (MJDHour - MJDHour0) * 24.0;
        T0 = (MJDHour0 - 51544.5) / 36525.0;
        GMST = 6.697374558 + 1.0027379093 * UT
               + (8640184.812866 + (0.093104 - 6.2E-6 * T0) * T0) * T0 / 3600.0;
        LMST = 24.0 * frac((GMST + longitude / 15.0) / 24.0);

        return LMST;

    } // end calcLocalMeanSiderealTime()


  /**
    * This is the low-precision solar co-ordinate calculation from Astronomy
    * on the Personal Computer. It is accurate to about 1'.
    * <br><br>
    * Based on
    * <a href="http://www.merrymeet.com/minow/sunclock/SunClock.html">SunClock</a>
    * by Martin Minow.
    *
    * @parameter hour The actual time for ephemeris to be computed.
    * @return   Array for solar ephemeris. Use RIGHTASCENSION and DECLENSION
    *           as indices into this array.
    *
    * @see <a href="http://www.merrymeet.com/minow/sunclock/Sun.java">Sun.java</a>
    */
    public double[] calcSolarCoordinates(double hour)
    {

        double TZOffset = _tzOffset;

        // constants
        double CosEPS = 0.91748;
        double SinEPS = 0.39778;

        double MJDHour;
        double T;
        double M;
        double DL;
        double L;
        double SL;
        double X;
        double Y;
        double Z;
        double Rho;
        double altitude;
        double[] coordinates = new double[2];

        MJDHour = Math.floor(calcModifiedJulianDay()) - (TZOffset / 24.0) + (hour / 24.0);

        T = (MJDHour - 51544.5) / 36525.0;
        M = (2.0 * Math.PI) * frac(0.993133 + 99.997361 * T);

        DL  = 6893.0 * Math.sin(M) + 72.0 * Math.sin(M * 2.0);
        L   = (2.0 * Math.PI) * frac(0.7859453 + M / (2.0 * Math.PI) +
                    (6191.2 * T + DL) / 1296e3);
        SL  = Math.sin(L);
        X   = Math.cos(L);
        Y   = CosEPS * SL;
        Z   = SinEPS * SL;
        Rho = Math.sqrt(1.0 - Z * Z);

        coordinates[DECLINATION] = (360.0 / (2.0 * Math.PI)) * MoreMath.atan2(Z, Rho);
        coordinates[RIGHTASCENSION]  = (48.0 / (2.0 * Math.PI)) * MoreMath.atan2(Y, (X + Rho));

        if (coordinates[RIGHTASCENSION] < 0.0) {
            coordinates[RIGHTASCENSION] += 24.0;
        }

        return coordinates;

    } // end calcSolarCoordinates


  /**
    * Checks if an event is in the valid range.
    *
    * @param   event The event to check.
    * @return  Boolean for valid.
    */
    private boolean isEvent(double event) {

        return ((event != ABOVE_HORIZON) && (event != BELOW_HORIZON));

    } // end isEvent()


  /**
    * Compute the time of sunrise and sunset for this date. This
    * uses an exhaustive search algorithm described in Astronomy on
    * the Personal Computer. Consequently, it is rather slow.
    * The times are returned in the observer's local time.
    * <br><br>
    * Based on
    * <a href="http://www.merrymeet.com/minow/sunclock/SunClock.html">SunClock</a>
    * by Martin Minow.
    *
    * @param    horizon  The adopted true altitude of the horizon
    *                    in degrees. Use one of the following values:
    * <br> &#8729; SUNRISE_SUNSET
    * <br> &#8729; CIVIL_TWILIGHT
    * <br> &#8729; NAUTICAL_TWILIGHT
    * <br> &#8729; ASTRONOMICAL_TWILIGHT
    * @return   Array for sunrise and sunset times. Use RISE and SET
    *           as indices into this array.
    *
    * @see <a href="http://www.merrymeet.com/minow/sunclock/Sun.java">Sun.java</a>
    */
    public double[] calcRiseSet(double horizon)  {

        double sinHorizon;
        double rise, set;
        double hour;
        double YMinus, YThis, YPlus;
        double XExtreme, YExtreme;
        double A, B, C;
        double discriminant;
        double root1, root2;
        int    numroots;
        double DX;
        double result[] = new double[2];

        sinHorizon = sin(horizon);
        YMinus = sinAltitude(0.0) - sinHorizon;

        if (YMinus > 0.0) {
            rise = ABOVE_HORIZON;
            set   = ABOVE_HORIZON;
        } else {
            rise = BELOW_HORIZON;
            set   = BELOW_HORIZON;
        } // end if

        for (hour = 1.0; hour <= 24.0; hour += 2.0)  {

            YThis = sinAltitude(hour) - sinHorizon;
            YPlus = sinAltitude(hour  + 1.0) - sinHorizon;

           /*
            * Quadratic interpolation through the three points:
            * [-1, YMinus], [0, YThis], [+1,  yNext]
            * (These must not lie on a straight line.)
            */
            root1 = 0.0;
            root2 = 0.0;
            numroots = 0;
            A = (0.5  * (YMinus +  YPlus))  - YThis;
            B = (0.5  * (YPlus - YMinus));
            C = YThis;
            XExtreme = -B /  (2.0 * A);
            YExtreme = (A *  XExtreme + B) *  XExtreme + C;
            discriminant = (B * B) - 4.0 * A * C;
            if (discriminant >=  0.0) { /* intersects x-axis? */
                DX = 0.5 * Math.sqrt(discriminant) / Math.abs(A);
                root1 =  XExtreme - DX;
                root2 =  XExtreme + DX;
                if (Math.abs(root1)  <= +1.0) {
                    numroots++;
                }
                if (Math.abs(root2)  <= +1.0) {
                    numroots++;
                }
                if (root1 <  -1.0) {
                  root1 =  root2;
                }
            } // end if

           /*
            * Quadratic interpolation result:
            * numroots  Number of roots found (0, 1,  or 2).
            *           If numroots ==  0, there is  no event in  this range.
            * root1     First root. (numroots >= 1)
            * root2     Second root.  (numroots  == 2)
            * YMinus    Y-value at interpolation  start. If <  0, root1 is
            *           a rise event.
            * YExtreme  Maximum value of y (numroots == 2) - this determines
            *           whether a 2-root event is a rise-set or a set-rise.
            */
            switch (numroots)  {
                case 0 : /*  No root at this hour */
                    break;

                case 1 : /*  Found either a rise or a set */
                    if (YMinus < 0.0) {
                        rise = hour  + root1;
                    } else {
                        set = hour + root1;
                    }
                    break;

                case 2 : /*  Found both a rise and a set  */
                    if (YExtreme < 0.0)  {
                        rise = hour  + root2;
                        set = hour  + root1;
                    } else {
                        rise = hour  + root1;
                        set = hour  + root2;
                    }
                    break;
            } /* root switch */

            if (isEvent(rise) && isEvent(set)) {
                break;
            } // end if

            YMinus = YPlus;

        } /* for loop */

        if (isEvent(rise)) {
            rise = mod(rise, 24.0);
        }
        if (isEvent(set)) {
            set  = mod(set, 24.0);
        }

        result[RISE] = rise;
        result[SET]  = set;

        return result;

    } // end calcRiseSet()


    //******************  Mathematical utility functions ******************
    //******************


    private static final double convDegreesToRadians = (Math.PI / 180.0);


  /**
    * Modulus function that always returns a positive value. For example,
    * mod(-3, 24) is 21
    */
    private double mod(double numerator, double denominator) {

        double result;

        //result = Math.IEEEremainder(numerator, denominator);
        // TODO: Check if substitution is OK.
        result = numerator % denominator;
        if (result < 0)  {
            result += denominator;
        }

        return result;

    } // end mod()


  /**
    * Rounds towards zero.
    *
    * @param   value Value to round.
    * @return  Value rounded towards zero (returned as double).
    */
    private double frac(double value) {

        double result;

        result = value - trunc(value);
        if (result < 0.0) {
            result += 1.0;
        }

        return result;

    } // end frac()


  /**
    * Returns the integer nearest
    * to zero. (This behaves differently than Math.floor()
    * for negative values.)
    *
    * @param   value The value to convert
    * @return  Integer value nearest zero (returned as double).
    */
    private int trunc(double value) {

        int result;

        result = (int) Math.floor(Math.abs(value));
        if (value < 0.0) {
            result *= -result;
        }

        return result;

    } // end trunc()



  /**
    * Sine of an angle expressed in degrees.
    */
    private double sin(double dblDegrees) {

        double result;

        result = Math.sin(dblDegrees * convDegreesToRadians);

        return result;

    } // end sin()


  /**
    * Cosine of an angle expressed in degrees.
    */
    private double cos(double dblDegrees) {

        double result;

        result = Math.cos(dblDegrees * convDegreesToRadians);

        return result;

    } // end cos()


    //******************  Other functions ******************
    //******************


    public String toString()
    {

        String strValue;
        double riseset[];

        riseset = calcRiseSet(SUNRISE_SUNSET);

        strValue =  "latitude=" + _latitude + ";" +
                    "longitude=" + _longitude + ";" +
                    "timezone=" + _tzOffset + ";" +
                    "date=" + _year + "." + _month + "." + _day + ";" +
                    "rise=" + formatTime(riseset[RISE]) + ";" +
                    "set=" + formatTime(riseset[SET]) + ";" +
                    "";

        return strValue;

    } // end toString()


    public String formatTime(double number)
    {

        String strTimeFormat = "";
        int hours, minutes;

        // get hours and minutes
        hours = trunc(number);
        minutes = (int) (frac(number) * 60);
        if (minutes == 60)  {
            hours++;
            minutes = 0;
        }

        strTimeFormat =
            (hours>9?(""+hours):("0"+hours)) +
            ":" +
            (minutes>9?(""+minutes):("0"+minutes));

        return strTimeFormat;

    } // end formatTime()


    //******************  Test ******************
    //******************


    public static void main(String[] args)
    {

        SunCalc objSunCalc = new SunCalc();
        double ephemeris[];

        // Geneva, Switzerland
        objSunCalc.setLatitude(46.2);
        objSunCalc.setLongitude(6.15);
        objSunCalc.setTimeZoneOffset(1);
        // Nov 28, 2001
        objSunCalc.setYear(2001);
        objSunCalc.setMonth(11);
        objSunCalc.setDay(28);

        System.out.print("Geneva, Switzerland: ");
        System.out.println(objSunCalc);

    } // end main()

} // end SunCalc

// </pre>

