

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for GpsMid");
		//$JUnit-BEGIN$
		suite.addTestSuite(MoreMathTests.class);
		suite.addTestSuite(IntTreeTests.class);
		suite.addTestSuite(StringTokenizerTests.class);
		//$JUnit-END$
		return suite;
	}

}
