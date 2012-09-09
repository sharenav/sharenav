package net.sharenav.osmToShareNav;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for net.sharenav.osmToShareNav");
		//$JUnit-BEGIN$		
		suite.addTestSuite(MyMathTest.class);		
		suite.addTestSuite(LongArrayMapTest.class);
		//$JUnit-END$
		return suite;
	}

}
