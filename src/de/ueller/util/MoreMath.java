// **********************************************************************
// 
// <copyright>
// 
//  BBN Technologies
//  10 Moulton Street
//  Cambridge, MA 02138
//  (617) 873-8000
// 
//  Copyright (C) BBNT Solutions LLC. All rights reserved.
// 
// </copyright>
// **********************************************************************
// 
// $Source$
// $RCSfile$
// $Revision$
// $Date$
// $Author$
// 
// **********************************************************************

package de.ueller.util;


//import de.ueller.m.midlet.gps.Logger;
import de.ueller.gps.Node;
import de.ueller.gpsmid.data.Legend;

/**
 * MoreMath provides functions that are not part of the standard Math class.
 * <p>
 * 
 * <pre>
 *  
 *   Functions:
 *        asinh(float x) - hyperbolic arcsine
 *        sinh(float x) - hyperbolic sine
 *  
 *   Need to Implement:
 *   Function                Definition                              
 *   Hyperbolic cosine       (e&circ;x+e&circ;-x)/2                            
 *   Hyperbolic tangent      (e&circ;x-e&circ;-x)/(e&circ;x+e&circ;-x)                   
 *   Hyperbolic arc cosine   2 log  (sqrt((x+1)/2) + sqrt((x-1)/2))  
 *   Hyperbolic arc tangent  (log  (1+x) - log (1-x))/2
 *   
 * </pre>
 */
public class MoreMath {

//	private final static Logger				logger			= Logger
//																	.getInstatance(MoreMath.class);

	/**
	 * 180/Pi
	 */
	final public static transient float FAC_RADTODEC =  180.0f/(float)Math.PI;
	/**
	 * Pi/180
	 */
	final public static transient float FAC_DECTORAD =  (float)Math.PI / 180f;
	/**
	 * 2*Math.PI
	 */
	final public static transient float		TWO_PI			= (float) Math.PI * 2.0f;

	/**
	 * 2*Math.PI
	 */
	final public static transient double	TWO_PI_D		= Math.PI * 2.0d;

	/**
	 * Math.PI/2
	 */
	final public static transient float		HALF_PI			= (float) Math.PI / 2.0f;

	/**
	 * Math.PI/2
	 */
	final public static transient double	HALF_PI_D		= Math.PI / 2.0d;

	final public static transient float		E				= 2.718281828459045f;

	final static public transient float		FLOAT_LOGFDIV2	= -0.6931471805599453094f;

	// Radiants per meter at equator
	final static public float 	RADIANT_PER_METER = 1 / MoreMath.PLANET_RADIUS;

	/**
	 * Average earth radius of the WGS84 geoid in meters.
	 * The old value used here was 6378140 and 6378159.81 for SingleTile."fpm".
	 */
	public static final double PLANET_RADIUS_D = 6371000.8d;
	public static final float PLANET_RADIUS = 6371000.8f;

	/**
	 * This constant is used as fixed point multiplier to convert
	 * latitude / longitude from radians to fixpoint representation.
	 * With this multiplier, one should get a resolution of 1m at the equator.
	 * 
	 * This constant has to be in synchrony with the value in Osm2GpsMid.
	 */
	public static float FIXPT_MULT = PLANET_RADIUS / Legend.mapPrecision; 

	/**
	 * 1 / FIXPT_MULT, this saves a floating point division.
	 */
	public static float FIXPT_MULT_INV = 1.0f / FIXPT_MULT;

	// cannot construct
	private MoreMath() {}

	/**
	 * Checks if a ~= b. Use this to test equality of floating point numbers.
	 * <p>
	 * 
	 * @param a
	 *            double
	 * @param b
	 *            double
	 * @param epsilon
	 *            the allowable error
	 * @return boolean
	 */
	final public static boolean approximately_equal(double a, double b,
			double epsilon) {
		return (Math.abs(a - b) <= epsilon);
	}

	/**
	 * Checks if a ~= b. Use this to test equality of floating point numbers.
	 * <p>
	 * 
	 * @param a
	 *            float
	 * @param b
	 *            float
	 * @param epsilon
	 *            the allowable error
	 * @return boolean
	 */
	final public static boolean approximately_equal(float a, float b, float epsilon) {
		return (Math.abs(a - b) <= epsilon);
	}

	/**
	 * Hyperbolic arcsin.
	 * <p>
	 * Hyperbolic arc sine: log (x+sqrt(1+x^2))
	 * 
	 * @param x
	 *            float
	 * @return float asinh(x)
	 */
	public static final float asinh(float x) {
//		logger.info("enter asinh " + x);
		return MoreMath.log(x + ((float) Math.sqrt(x * x + 1)));
	}

	public static void setFIXPTValues() {
		FIXPT_MULT = PLANET_RADIUS / Legend.mapPrecision; 
		FIXPT_MULT_INV = 1.0f / FIXPT_MULT;
	}

	/**
	 * Hyperbolic sin.
	 * <p>
	 * Hyperbolic sine: (e^x-e^-x)/2
	 * 
	 * @param x
	 *            float
	 * @return float sinh(x)
	 */
	public static final float sinh(float x) {
		return (MoreMath.pow(MoreMath.E, x) - MoreMath.pow(MoreMath.E, -x)) / 2.0f;
	}

	// HACK - are there functions that already exist?
	/**
	 * Return sign of number.
	 * 
	 * @param x
	 *            short
	 * @return int sign -1, 1
	 */
	public static final int sign(short x) {
		return (x < 0) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * 
	 * @param x
	 *            int
	 * @return int sign -1, 1
	 */
	public static final int sign(int x) {
		return (x < 0) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * 
	 * @param x
	 *            long
	 * @return int sign -1, 1
	 */
	public static final int sign(long x) {
		return (x < 0) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * 
	 * @param x
	 *            float
	 * @return int sign -1, 1
	 */
	public static final int sign(float x) {
		return (x < 0f) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * 
	 * @param x
	 *            double
	 * @return int sign -1, 1
	 */
	public static final int sign(double x) {
		return (x < 0d) ? -1 : 1;
	}

	/**
	 * Check if number is odd.
	 * 
	 * @param x
	 *            short
	 * @return boolean
	 */
	public static final boolean odd(short x) {
		return !even(x);
	}

	/**
	 * Check if number is odd.
	 * 
	 * @param x
	 *            int
	 * @return boolean
	 */
	public static final boolean odd(int x) {
		return !even(x);
	}

	/**
	 * Check if number is odd.
	 * 
	 * @param x
	 *            long
	 * @return boolean
	 */
	public static final boolean odd(long x) {
		return !even(x);
	}

	/**
	 * Check if number is even.
	 * 
	 * @param x
	 *            short
	 * @return boolean
	 */
	public static final boolean even(short x) {
		return ((x & 0x1) == 0);
	}

	/**
	 * Check if number is even.
	 * 
	 * @param x
	 *            int
	 * @return boolean
	 */
	public static final boolean even(int x) {
		return ((x & 0x1) == 0);
	}

	/**
	 * Check if number is even.
	 * 
	 * @param x
	 *            long
	 * @return boolean
	 */
	public static final boolean even(long x) {
		return ((x & 0x1) == 0);
	}

	/**
	 * Converts a byte in the range of -128 to 127 to an int in the range 0 - 255.
	 * 
	 * @param b
	 *            (-128 &lt;= b &lt;= 127)
	 * @return int (0 &lt;= b &lt;= 255)
	 */
	public static final int signedToInt(byte b) {
		return (b & 0xff);
	}

	/**
	 * Converts a short in the range of -32768 to 32767 to an int in the range 0 - 65535.
	 * 
	 * @param w
	 *            (-32768 &lt;= b &lt;= 32767)
	 * @return int (0 &lt;= b &lt;= 65535)
	 */
	public static final int signedToInt(short w) {
		return (w & 0xffff);
	}

	/**
	 * Convert an int in the range of -2147483648 to 2147483647 to a long in the range 0 to 4294967295.
	 * 
	 * @param x
	 *            (-2147483648 &lt;= x &lt;= 2147483647)
	 * @return long (0 &lt;= x &lt;= 4294967295)
	 */
	public static final long signedToLong(int x) {
		return (x & 0xFFFFFFFFL);
	}

	/**
	 * Calculate the shortest square distance of point (PX,PX) to the line (X1,Y1)-(X2,Y2)
	 * @param X1
	 * @param Y1
	 * @param X2
	 * @param Y2
	 * @param PX
	 * @param PY
	 * @return
	 */
	public static float  ptSegDistSq  (  int X1, int Y1,  
			int X2, int Y2,  
			int PX, int PY  
	)   
	{  
 		// all variables changed to long/double,
 		// because on big ZOOM levels (starting from '5 meters'
 		// on scale bar) override happens (far-far-away line
 		// calculated as very closer line)
		double distSquare;
		long X12, Y12, X1P, Y1P, X2P, Y2P;


//		Find vector from  ( X1,Y1 )  to  ( X2,Y2 )   
//		and the Square of its length.   
		X12 = (long)X2 - (long)X1;
		Y12 = (long)Y2 - (long)Y1;

//		Find vector from  ( X1,Y1 )  to  ( PX,PY )  .   
		X1P = (long)PX - (long)X1;
		Y1P = (long)PY - (long)Y1;
//		Do scalar product and check sign.   
		if  (  X12 * X1P + Y12 * Y1P     <= 0L  )
		{  
//			Closest point on segment is  ( X1,Y1 ) ;  
//			find its distance  ( squared )  from  ( PX,PY )  .   
			distSquare = X1P * X1P + Y1P * Y1P ;
		}  
		else 
		{  
//			Find vector from  ( X2,Y2 )  to  ( PX,PY )  .   
			X2P = (long)PX - (long)X2 ;
			Y2P = (long)PY - (long)Y2 ;
//			Do scalar product and check sign.   
			if  (  X12 * X2P + Y12 * Y2P     >= 0L  )
			{  
				// Closest point on segment is  ( X2,Y2 ) ;  
				// find its distance  ( squared )  from  ( PX,PY )  .   
				distSquare = X2P * X2P + Y2P * Y2P ;
			}  
			else 
			{  
				// Closest point on segment is between  ( X1,Y1 )  and  
				//   ( X2,Y2 )  . Use perpendicular distance formula.   
				distSquare = (double)(X12 * Y1P - Y12 * X1P) ;
				double L12Square = (double)(X12 * X12 + Y12 * Y12) ;
				distSquare = distSquare * distSquare / L12Square ;  
				// Note that if L12Square be zero, the first  
				// of the three branches will be selected,  
				// so division by zero can not occur here.  
			}  
		}  


		return (float)distSquare ;  
	}   
	public static float  ptSegDistSq  (  float X1, float Y1,  
			float X2, float Y2,  
			float PX, float PY  
	)   
	{  
		double distSquare ;  
		double X12, Y12, X1P, Y1P, X2P, Y2P ;


//		Find vector from  ( X1,Y1 )  to  ( X2,Y2 )   
//		and the Square of its length.   
		X12 = (double)X2 - (double)X1 ;  
		Y12 = (double)Y2 - (double)Y1 ;  

//		Find vector from  ( X1,Y1 )  to  ( PX,PY )  .   
		X1P = (double)PX - (double)X1 ;
		Y1P = (double)PY - (double)Y1 ;
//		Do scalar product and check sign.   
		if  (  X12 * X1P + Y12 * Y1P     <= 0D  )
		{  
//			Closest point on segment is  ( X1,Y1 ) ;  
//			find its distance  ( squared )  from  ( PX,PY )  .   
			distSquare = X1P * X1P + Y1P * Y1P ;
		}  
		else 
		{  
//			Find vector from  ( X2,Y2 )  to  ( PX,PY )  .   
			X2P = (double)PX - (double)X2 ;
			Y2P = (double)PY - (double)Y2 ;
//			Do scalar product and check sign.   
			if  (  X12 * X2P + Y12 * Y2P     >= 0D  )
			{  
				// Closest point on segment is  ( X2,Y2 ) ;  
				// find its distance  ( squared )  from  ( PX,PY )  .   
				distSquare = X2P * X2P + Y2P * Y2P ;
			}  
			else 
			{  
				// Closest point on segment is between  ( X1,Y1 )  and  
				//   ( X2,Y2 )  . Use perpendicular distance formula.   
				distSquare = X12 * Y1P - Y12 * X1P ;
				double L12Square = X12 * X12 + Y12 * Y12 ;
				distSquare = distSquare * distSquare / L12Square ;  
				// Note that if L12Square be zero, the first  
				// of the three branches will be selected,  
				// so division by zero can not occur here.  
			}  
		}  

		return (float)distSquare;
	}   


	/**
	 * The rest of this file is based on work of Nikolay Klimchuk but modified
	 * to use float instead of double.
	 * 
	 * <p>Title: Class for float-point calculations in J2ME applications CLDC 1.1</p>
	 * <p>Description: Useful methods for float-point calculations which absent in native Math class</p>
	 * <p>Copyright: Copyright (c) 2004 Nick Henson</p>
	 * <p>Company: UNTEH</p>
	 * <p>License: Free use only for non-commercial purpose</p>
	 * <p>If you want to use all or part of this class for commercial applications then take into account these conditions:</p>
	 * <p>1. I need a one copy of your product which includes my class with license key and so on</p>
	 * <p>2. Please append my copyright information henson.midp.Float (C) by Nikolay Klimchuk on 'About' screen of your product</p>
	 * <p>3. If you have web site please append link <a href="http://henson.newmail.ru">Nikolay Klimchuk</a> on the page with description of your product</p>
	 * <p>That's all, thank you!</p>
	 * @author Nikolay Klimchuk http://henson.newmail.ru
	 * @version 0.5
	 */

	public static  float acos(float x) {
		float f=asin(x);
		if(f==Float.NaN)
			return f;
		return PiDiv2-f;
	}

	public static final float asin(float x) {
		if ((x < -1f) || (x > 1f)) {
			return Float.NaN;
		}
		if (x == -1f) {
			return -HALF_PI;
		}
		if (x == 1f) {
			return HALF_PI;
		}
		
		return atan((float) (x / Math.sqrt(1 - x * x)));

	}

	  /** Square root from 3 */
	public final static float SQRT3 = 1.732050807568877294f;
	public final static float PiDiv12=0.26179938779914943653855361527329f;
	public final static float PiDiv6=0.52359877559829887307710723054658f;
	public final static float PiDiv2=1.5707963267948966192313216916398f;
	public final static double SQRT3D = 1.732050807568877294f;
	public final static double PiDiv12D=0.26179938779914943653855361527329f;
	public final static double PiDiv6D=0.52359877559829887307710723054658f;
	public final static double PiDiv2D=1.5707963267948966192313216916398f;

	  static public float atan2(float y, float x)
	  {
	    // if x=y=0
	    if(y==0. && x==0.)
	      return 0f;
	    // if x>0 atan(y/x)
	    if(x>0f)
	      return atan(y/x);
	    // if x<0 sign(y)*(pi - atan(|y/x|))
	    if(x<0f)
	    {
	      if(y<0f)
	        return (float) -(Math.PI-atan(y/x));
	      else
	        return (float) (Math.PI-atan(-y/x));
	    }
	    // if x=0 y!=0 sign(y)*pi/2
	    if(y<0.)
	      return (float) (-Math.PI/2.);
	    else
	      return (float) (Math.PI/2.);
	  }
	  
	  static public double atan2(double y, double x)
	  {
	    // if x=y=0
	    if(y==0. && x==0.)
	      return 0f;
	    // if x>0 atan(y/x)
	    if(x>0f)
	      return atan(y/x);
	    // if x<0 sign(y)*(pi - atan(|y/x|))
	    if(x<0f)
	    {
	      if(y<0f)
	        return  -(Math.PI-atan(y/x));
	      else
	        return  (Math.PI-atan(-y/x));
	    }
	    // if x=0 y!=0 sign(y)*pi/2
	    if(y<0.)
	      return  (-Math.PI/2.);
	    else
	      return  (Math.PI/2.);
	  }

	  static public float atan(float x)
	  {
	      boolean signChange=false;
	      boolean Invert=false;
	      int sp=0;
	      float x2, a;
	      // check up the sign change
	      if(x<0f)
	      {
	          x=-x;
	          signChange=true;
	      }
	      // check up the invertation
	      if(x>1f)
	      {
	          x=1/x;
	          Invert=true;
	      }
	      // process shrinking the domain until x<PI/12
	      while(x>PiDiv12)
	      {
	          sp++;
	          a=x+SQRT3;
	          a=1/a;
	          x=x*SQRT3;
	          x=x-1;
	          x=x*a;
	      }
	      // calculation core
	      x2=x*x;
	      a=x2+1.4087812f;
	      a=0.55913709f/a;
	      a=a+0.60310579f;
	      a=a-(x2*0.05160454f);
	      a=a*x;
	      // process until sp=0
	      while(sp>0)
	      {
	          a=a+PiDiv6;
	          sp--;
	      }
	      // invertation took place
	      if(Invert) {
			a=PiDiv2-a;
		}
	      // sign change took place
	      if(signChange) {
			a=-a;
		}
	      //
	      return a;
	  }
	  static public double atan(double x)
	  {
	      boolean signChange=false;
	      boolean Invert=false;
	      int sp=0;
	      double x2, a;
	      // check up the sign change
	      if(x<0f)
	      {
	          x=-x;
	          signChange=true;
	      }
	      // check up the invertation
	      if(x>1f)
	      {
	          x=1/x;
	          Invert=true;
	      }
	      // process shrinking the domain until x<PI/12
	      while(x>PiDiv12D)
	      {
	          sp++;
	          a=x+SQRT3D;
	          a=1/a;
	          x=x*SQRT3D;
	          x=x-1;
	          x=x*a;
	      }
	      // calculation core
	      x2=x*x;
	      a=x2+1.4087812f;
	      a=0.55913709f/a;
	      a=a+0.60310579f;
	      a=a-(x2*0.05160454f);
	      a=a*x;
	      // process until sp=0
	      while(sp>0)
	      {
	          a=a+PiDiv6D;
	          sp--;
	      }
	      // invertation took place
	      if(Invert) {
			a=PiDiv2D-a;
		}
	      // sign change took place
	      if(signChange) {
			a=-a;
		}
	      //
	      return a;
	  }


	public static final float log(float x) {
//		logger.info("enter log " + x);
		if (!(x > 0f)) {
			return Float.NaN;
		}
		//
		if (x == 1f) {
			return 0f;
		}
		// Argument of _log must be (0; 1]
		if (x > 1f) {
			x = 1 / x;
			return -_log(x);
		}
		;
		//
		return _log(x);
	}

	public static final float pow(float x, float y) {
		if (x == 0f) {
			return 0f;
		}
		if (x == 1f) {
			return 1f;
		}
		if (y == 0f) {
			return 1f;
		}
		if (y == 1f) {
			return x;
		}
		//
		int l;
		boolean neg;
		if (y < 0f) {
			neg = true;
			l = (int) (y);
		} else {
			neg = false;
			l = (int) (y);
		}
		boolean integerValue = (y == l);
		//
		if (integerValue) {
			//
			float result = x;
			for (long i = 1; i < (neg ? -l : l); i++) {
				result = result * x;
			}
			//
			if (neg) {
				return 1f / result;
			} else {
				return result;
			}
		} else {
			if (x > 0f) {
				return exp(y * log(x));
			} else {
				return Float.NaN;
			}
		}
	}

	static private float exact_log(float x) {
//		logger.info("enter _log " + x);
		if (!(x > 0f)) {
			return Float.NaN;
		}
		//
		float f = 0f;
		//
		int appendix = 0;
		while ((x > 0f) && (x <= 1f)) {
			x *= 2f;
			appendix++;
		}
		//
		x /= 2f;
		appendix--;
		//
		float y1 = x - 1f;
		float y2 = x + 1f;
		float y = y1 / y2;
		//
		float k = y;
		y2 = k * y;
		//
		for (long i = 1; i < 50; i += 2) {
			f += k / i;
			k *= y2;
		}
		//
		f *= 2f;
		for (int i = 0; i < appendix; i++) {
			f += FLOAT_LOGFDIV2;
		}
		//
//		logger.info("exit _log" + f);
		return f;
	}

	static private float _log(float x) {
//		logger.info("enter _log " + x);
		if (!(x > 0f)) {
			return Float.NaN;
		}
		//
		float f = 0f;
		//
		int appendix = 0;
		while ((x > 0f) && (x <= 1f)) {
			x *= 2f;
			appendix++;
		}
		//
		x /= 2f;
		appendix--;
		//
		float y1 = x - 1f;
		float y2 = x + 1f;
		float y = y1 / y2;
		//
		float k = y;
		y2 = k * y;
		//
		for (long i = 1; i < 10; i += 2) {
			f += k / i;
			k *= y2;
		}
		//
		f *= 2f;
		for (int i = 0; i < appendix; i++) {
			f += FLOAT_LOGFDIV2;
		}
		//
//		logger.info("exit _log" + f);
		return f;
	}

	static public float exp(float x) {
		if (x == 0f) {
			return 1f;
		}
		//
		float f = 1f;
		long d = 1;
		float k;
		boolean isless = (x < 0f);
		if (isless) {
			x = -x;
		}
		k = x / d;
		//
		for (long i = 2; i < 50; i++) {
			f = f + k;
			k = k * x / i;
		}
		//
		if (isless) {
			return 1 / f;
		} else {
			return f;
		}
	}

    final public static int dist(float lat1, float lon1,
    		float lat2, float lon2) {
//    	float c=acos((float) (Math.sin(lat1)*Math.sin(lat2) +
//    			 Math.cos(lat1)*Math.cos(lat2) *
//    			 Math.cos(lon2-lon1)));
//    	return (int)(ALT_NN*c+0.5f);
    	double latSin = Math.sin((lat2-lat1)/2d);
    	double longSin = Math.sin((lon2-lon1)/2d);
    	double a = (latSin * latSin) + (Math.cos(lat1)*Math.cos(lat2)*longSin*longSin);
    	double c = 2d * atan2(Math.sqrt(a),Math.sqrt(1d-a));
    	return (int)((PLANET_RADIUS_D * c) + 0.5d);
    }

    final public static double bearing_int(double lat1, double lon1,
    		double lat2, double lon2){
    	double dLon=lon2-lon1;
    	double y=Math.sin(dLon) * Math.cos(lat2);
    	double x=Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
    	return atan2(y,x);
    }        

	/**
	 * calculate the start bearing in 1/2 degree so result 90 indicates 180 degree. 
	 * @param from
	 * @param to
	 * @return
	 */
	public final static byte bearing_start(float fromLat, float fromLon, float toLat, float toLon){
		double b=bearing_int(
				fromLat,
				fromLon,
				toLat,
				toLon);
		return (byte) (Math.toDegrees(b)/2);
	}

	/**
	 * 
	 * @param lineP1x - in screen coordinates
	 * @param lineP1y - in screen coordinates
	 * @param lineP2x - in screen coordinates
	 * @param lineP2y - in screen coordinates
	 * @param offPointX - point outside the line in screen coordinates
	 * @param offPointY - point outside the line in screen coordinates
	 * @return IntPoint - closest point on line in screen coordinates
	 */
	public static IntPoint closestPointOnLine(int lineP1x, int lineP1y, int lineP2x, int lineP2y, int offPointX, int offPointY) {
		// avoid division by zero if lineP1 and lineP2 are at the same screen coordinates
		if (lineP1x == lineP2x && lineP1y == lineP2y) {
			return new IntPoint(lineP1x, lineP1y);
		}
		float uX = (float) (lineP2x - lineP1x);
		float uY = (float) (lineP2y - lineP1y);
		float  u = ( (offPointX - lineP1x) * uX + (offPointY  - lineP1y) * uY) / (uX * uX + uY * uY);
		if (u > 1.0) {
			return new IntPoint(lineP2x, lineP2y);
		} else if (u <= 0.0) {
			return new IntPoint(lineP1x, lineP1y);
		} else {
			return new IntPoint( (int)(lineP2x * u + lineP1x * (1.0 - u ) + 0.5), (int) (lineP2y * u + lineP1y * (1.0-u) + 0.5));
		}
	}

	public static Node closestPointOnLine(Node node1, Node node2, Node offNode) {
		// avoid division by zero if node1 and node2 are at the same coordinates
		if (node1.radlat == node2.radlat && node1.radlon == node2.radlon) {
			return new Node(node1);
		}
		float uX = node2.radlat - node1.radlat;
		float uY = node2.radlon - node1.radlon;
		float  u = ( (offNode.radlat - node1.radlat) * uX + (offNode.radlon  - node1.radlon) * uY) / (uX * uX + uY * uY);
		if (u > 1.0) {
			return new Node(node2);
		} else if (u <= 0.0) {
			return new Node(node1);
		} else {
			return new Node( (float)(node2.radlat * u + node1.radlat * (1.0 - u )), (float) (node2.radlon * u + node1.radlon * (1.0-u)), true);
		}
	}
	
	/**
	 * Hyperbolic cosine.
	 * <p>
	 * Hyperbolic cosine: (e^x+e^-x)/2
	 * 
	 * @param x
	 *            float
	 * @return float cosh(x)
	 */
	public static final float cosh(float x) {
		return (MoreMath.pow(MoreMath.E, x) + MoreMath.pow(MoreMath.E, -x)) / 2.0f;
	}
	/**
	 * Hyperbolic tangent.
	 * <p>
	 * Hyperbolic cosine: sinh(x)/cosh(x)
	 * 
	 * @param x
	 *            float
	 * @return float tanh(x)
	 */
	public static final float tanh(float x) {
		return (MoreMath.sinh(x) / MoreMath.cosh(x));
	}
	/**
	 * hyperbolic arctangent
	 * @param x
	 *            float
	 * @return float atanh(x)
	 */
	public static final float atanh(float x) {
		//return (float) (1.0 / (1.0 - (MoreMath.pow(x, (float) 2.0))));
		return (float) (log((float)((1.0 + x) / (1.0 -x))) / 2.0);
	}
	/**
	 * Hyperbolic arccos.
	 * <p>
	 * 
	 * @param x
	 *            float
	 * @return float acosh(x)
	 */
	public static final float acosh(float x) {
//		logger.info("enter acosh " + x);
		return MoreMath.log(x + ((float) Math.sqrt(x * x - 1)));
	}

}
