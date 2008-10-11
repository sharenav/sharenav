import java.util.Random;

import de.ueller.midlet.gps.data.MoreMath;
import junit.framework.TestCase;


public class MoreMathTests extends TestCase {
	public final double relPrecision = 0.0001;
	public final int testReps = 100;

	public void testApproximately_equalDoubleDoubleDouble() {
		Random r = new Random();
		double epsilon = 0.13;
		for (int i = 1; i < 50; i++) {
			double a = r.nextDouble();
			double b = r.nextDouble();
			if ((Math.abs(a - b) <= epsilon)) {
				assertTrue(MoreMath.approximately_equal(a, b, epsilon));
			} else {
				assertFalse(MoreMath.approximately_equal(a, b, epsilon));
			}
		}

	}

	public void testApproximately_equalFloatFloatFloat() {
		Random r = new Random();
		float epsilon = 0.13f;
		for (int i = 1; i < testReps; i++) {
			float a = r.nextFloat();
			float b = r.nextFloat();
			if ((Math.abs(a - b) <= epsilon)) {
				assertTrue(a + " should be approx equal to " + b,MoreMath.approximately_equal(a, b, epsilon));
			} else {
				assertFalse(a + " should not be approx equal to " + b,MoreMath.approximately_equal(a, b, epsilon));
			}
		}
	}

	public void testAsinh() {
		fail("Not yet implemented");
	}

	public void testSinh() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f = r.nextFloat();
			double res = Math.sinh(f);
			assertTrue(Math.abs(MoreMath.sinh((float)f) - res) < Math.abs(res)*relPrecision);
		}
	}

	public void testSignShort() {
		assertEquals("Positive failed", 1, MoreMath.sign((short)32));
		assertEquals("Negative failed", -1, MoreMath.sign((short)-47));
	}

	public void testSignFloat() {
		assertEquals("Positive failed", 1, MoreMath.sign(32.4f));
		assertEquals("Negative failed", -1, MoreMath.sign(-47.7f));
	}

	public void testSignDouble() {
		assertEquals("Positive failed", 1, MoreMath.sign(32.4));
		assertEquals("Negative failed", -1, MoreMath.sign(-47.7));
	}
	
	public void testLog() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f = r.nextFloat();
			double res = Math.log(f);
			assertTrue(Math.abs(MoreMath.log((float)f) - res) < Math.abs(res)*relPrecision);
		}
	}

	public void testPtSegDistSqIntIntIntIntIntInt() {
		fail("Not yet implemented");
	}

	public void testPtSegDistSqFloatFloatFloatFloatFloatFloat() {
		fail("Not yet implemented");
	}

	public void testAcos() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f = r.nextFloat();
			double res = Math.acos(f);
			assertTrue(Math.abs(MoreMath.acos((float)f) - res) < Math.abs(res)*relPrecision);
		}

	}

	public void testAsin() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f = r.nextFloat();
			double res = Math.asin(f);
			assertTrue(Math.abs(MoreMath.asin((float)f) - res) < Math.abs(res)*relPrecision);
		}

	}

	public void testAtan2FloatFloat() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f1 = r.nextFloat();
			double f2 = r.nextFloat();
			double res = Math.atan2(f1,f2);
			assertTrue(Math.abs(MoreMath.atan2((float)f1,(float)f2) - res) < Math.abs(res)*relPrecision);
		}
	}

	public void testAtan2DoubleDouble() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f1 = r.nextFloat();
			double f2 = r.nextFloat();
			double res = Math.atan2(f1,f2);
			assertTrue(Math.abs(MoreMath.atan2(f1,f2) - res) < Math.abs(res)*relPrecision);
		}
	}

	public void testPow() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f1 = r.nextFloat();
			double f2 = r.nextFloat();
			double res = Math.pow(f1,f2);
			assertTrue(Math.abs(MoreMath.pow((float)f1,(float)f2) - res) < Math.abs(res)*relPrecision);
		}
	}

	public void testExp() {
		Random r = new Random();
		for (int i = 1; i < testReps; i++) {
			double f = r.nextFloat();
			double res = Math.exp(f);
			assertTrue(Math.abs(MoreMath.exp((float)f) - res) < Math.abs(res)*relPrecision);
		}

	}

	public void testDist() {
		float actual = MoreMath.dist(1.531700952f,0.413643033f,1.531997658f,0.411897704f);
		float expected = 1947.94f;
		float error = Math.abs(actual - expected);
		System.out.println("Testing distance calculation: expected " + expected + " calculated " + actual + " error " + (error/expected));
		//assertTrue("Dist reported " + actual + " expected: " + expected,error < expected*(0.01));
		
		
		
		actual = MoreMath.dist(0.000017453f,0.000017453f,-0.000017453f,-0.000017453f);
		expected = 313.80693790037595f;
		error = Math.abs(actual - expected);
		System.out.println("Testing distance calculation: expected " + expected + " calculated " + actual + " error " + (error/expected));
		//assertTrue("Dist reported " + actual + " expected: " + expected,error < expected*(0.01));
		
		actual = MoreMath.dist(0.926598791f,0.026249752f,0.909698768f,0.001453859f);
		expected = 117922.112f;
		error = Math.abs(actual - expected);
		System.out.println("Testing distance calculation: expected " + expected + " calculated " + actual + " error " + (error/expected));
		//assertTrue("Dist reported " + actual + " expected: " + expected,error < expected*(0.01));
		
		actual = MoreMath.dist(0.926598791f,0.026249752f,0.926599314f,0.026248007f);
		expected = 7f;
		error = Math.abs(actual - expected);
		System.out.println("Testing distance calculation: expected " + expected + " calculated " + actual + " error " + (error/expected));
		//assertTrue("Dist reported " + actual + " expected: " + expected,error < expected*(0.01));
	}

}
