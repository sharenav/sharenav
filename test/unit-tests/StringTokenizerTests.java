import java.util.Random;
import java.util.Vector;

import de.ueller.gps.tools.StringTokenizer;

import junit.framework.TestCase;

public class StringTokenizerTests extends TestCase {

	private String initString() {
		char[] byteBuff = new char[100];
		Random r = new Random();
		byteBuff[0] = 65;
		for (int i = 1; i < byteBuff.length - 1; i++) {
			switch (r.nextInt(10)) {
			case 0: {
				byteBuff[i] = 44;
				if (i < byteBuff.length - 1) {
					// Need to add this, as the builtin Stringtokenizer
					// ignores multiple consecutive seperators, whereas
					// our implementation doesn't
					byteBuff[++i] = 65;
				}
				break;
			}
			case 1:
			case 2:
			case 3:
			case 4:
			case 5: {
				byteBuff[i] = (char) (r.nextInt(26) + 65);
				break;
			}
			default: {
				byteBuff[i] = (char) r.nextInt(Short.MAX_VALUE);
				byteBuff[i] = (char) (r.nextInt(127) + 65);
			}
			}

		}
		// Need to add this, as the builtin Stringtokenizer
		// ignores multiple consecutive seperators, whereas
		// our implementation doesn't
		byteBuff[byteBuff.length - 1] = 65;
		
		String untokenized = new String(byteBuff);
		return untokenized;
	}

	public void testGetVector() {
		for (int i = 0; i < 30; i++) {
			String untokenized = initString();
			java.util.StringTokenizer st = new java.util.StringTokenizer(untokenized,",");
			StringTokenizer stj2me = new StringTokenizer();
			Vector<String> tokens = StringTokenizer.getVector(untokenized, ",");
			assertEquals(st.countTokens(), tokens.size());
			for (String s : tokens) {
				assertEquals(st.nextElement(), s);
			}
		}
		String untokenized = ",,,,,";
		Vector<String> tokens = StringTokenizer.getVector(untokenized, ",");
		assertEquals(6, tokens.size());
		for (String s : tokens) {
			assertTrue(s.equalsIgnoreCase(""));
		}
	}

	public void testGetArray() {
		for (int i = 0; i < 30; i++) {
			String untokenized = initString();
			java.util.StringTokenizer st = new java.util.StringTokenizer(untokenized,",");
			StringTokenizer stj2me = new StringTokenizer();
			String[] tokens = StringTokenizer.getArray(untokenized, ",");
			//assertEquals(st.countTokens(), tokens.length);
			for (String s : tokens) {
				String ss = (String)st.nextElement();
				assertEquals(ss, s);
			}
		}
	}
}
