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

package de.ueller.midlet.gps.data;


//import de.ueller.m.midlet.gps.Logger;

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

	public static float  ptSegDistSq  (  int X1, int Y1,  
			int X2, int Y2,  
			int PX, int PY  
	)   
	{  
		float distSquare ;  
		int L12Square ;  
		int X12, Y12, X1P, Y1P, X2P, Y2P ;  


//		Find vector from  ( X1,Y1 )  to  ( X2,Y2 )   
//		and the Square of its length.   
		X12 = X2 - X1 ;  
		Y12 = Y2 - Y1 ;  
		L12Square = X12 * X12 + Y12 * Y12 ;   
//		Find vector from  ( X1,Y1 )  to  ( PX,PY )  .   
		X1P = PX - X1 ;  
		Y1P = PY - Y1 ;  
//		Do scalar product and check sign.   
		if  (  X12 * X1P + Y12 * Y1P     <= 0  )   
		{  
//			Closest point on segment is  ( X1,Y1 ) ;  
//			find its distance  ( squared )  from  ( PX,PY )  .   
			distSquare = X1P * X1P + Y1P * Y1P ;  
		}  
		else 
		{  
//			Find vector from  ( X2,Y2 )  to  ( PX,PY )  .   
			X2P = PX - X2 ;  
			Y2P = PY - Y2 ;  
//			Do scalar product and check sign.   
			if  (  X12 * X2P + Y12 * Y2P     >= 0  )   
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
				distSquare = distSquare * distSquare / L12Square ;  
				// Note that if L12Square be zero, the first  
				// of the three branches will be selected,  
				// so division by zero can not occur here.  
			}  
		}  


		return distSquare ;  
	}   


	
	/**
	 * The rest of this file is based on work of Nikolay Klimchuk but modifyed
	 * to use float insead of double.
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
	
	

}
