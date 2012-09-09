/*
 * ShareNav - Copyright (c) 2010 Olivier Cornu djinnn at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.util;

import java.util.Vector;


public class StringTokenizer {
	public static Vector getVector(String text, String separator) {
		Vector tokens = new Vector();
		int seplen = separator.length();
		int start = 0;
		int end;
		while ((end = text.indexOf(separator, start)) != -1) {
			tokens.addElement(text.substring(start, end));
			start = end + seplen;
		}
		if (start <= text.length()) {
			tokens.addElement(text.substring(start));
		}
		return tokens;
	}
	
	public static String[] getArray(String text, String separator) {
		Vector tmp = getVector(text, separator);
		String[] tokens = new String[tmp.size()];
		tmp.copyInto(tokens);
		return tokens;
	}
}
