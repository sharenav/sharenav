package de.ueller.gps.tools;
/*
 * GpsMid - Copyright (c) 2010 sk750 at users dot sourceforge dot net 
 * See Copying
 */

public class StringTools  {
	//private final static Logger logger = Logger.getInstance(StringTools.class,Logger.DEBUG);

	public static String replace(String string, String findText, String replaceText) {
		StringBuffer sb = new StringBuffer();
		
		int iFoundPos = string.indexOf(findText);
		int iStartPos = 0;
		
		// append string till findText and replaceText while findText is found
		while (iFoundPos >= 0) {
			sb.append(string.substring(iStartPos, iFoundPos)).append(replaceText);
			iStartPos = iFoundPos + findText.length();
			iFoundPos = string.indexOf(findText, iStartPos);	
		}
		// append remaining string without match
		sb.append(string.substring(iStartPos, string.length()));
		//System.out.println("Replaced in " + string + " all " + findText + " by " + replaceText + ":" + sb.toString());
		return sb.toString();
	}
}
