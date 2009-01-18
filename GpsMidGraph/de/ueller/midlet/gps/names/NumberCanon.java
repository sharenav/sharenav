/**
 * This is a close copy of the file in OSM2GpsMid 
 *
 *
 *
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2008 Kai Krueger
 */
package de.ueller.midlet.gps.names;

import java.io.InputStream;
import java.io.InputStreamReader;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.Trace;


public class NumberCanon {
	
	private static char [] charMapCore; //fast direct lookup table (Can't cover the entire unicode code range though)
	private static char [] charMapExtendedKey = new char [0]; //ordered Map to store the rest of the char -> num mappings
	private static char [] charMapExtendedValue = new char [0];
	private static char minFastRange = 47; //Specify the codepoint range that gets mapped in the lookup table
	private static char maxFastRange = 123;
	private static char defaultChar = '1'; //All unknown characters get mapped to this
	
	private static final int canonType = 0;
	private static final int normType = 2;
	private static NumberCanon o = new NumberCanon(); 
	
	public static void initCharMaps() {
		try {
			charMapCore = new char[maxFastRange - minFastRange];
			for (int i = 0; i < charMapCore.length; i++) {
				charMapCore[i] = defaultChar;
			}
			InputStream is = o.getClass().getResourceAsStream("/charMap.txt");
			InputStreamReader isr = new InputStreamReader(is,Configuration.getUtf8Encoding());
			char [] buf = new char[1024];
			int readChars = isr.read(buf);
			int idx = 0;
			char canon = '1';
			int charType = canonType;
			while (readChars > 0) {
				for (int i = 0; i < readChars; i++) {
					char c = buf[idx++];
					switch (charType) {
					case canonType: {
						if ((c == '\n') || (c == '\r'))
							break;
						if (c != '\t') {
							charType = normType;
						}
						Integer.parseInt(new StringBuffer().append(c).toString());
						canon = c;
						break;
					}
					case normType: {
						if ((c == '\n') || (c == '\r')) {
							charType = canonType;
							break;
						}
						if ((c >= minFastRange) && (c < maxFastRange)) {
							charMapCore[c - minFastRange] = canon;
						} else {
							char [] tmp = new char[charMapExtendedKey.length + 1];
							System.arraycopy(charMapExtendedKey, 0, tmp, 0, charMapExtendedKey.length);
							charMapExtendedKey = tmp;
							tmp = new char[charMapExtendedValue.length + 1];
							System.arraycopy(charMapExtendedValue, 0, tmp, 0, charMapExtendedValue.length);
							charMapExtendedValue = tmp;
							charMapExtendedKey[charMapExtendedKey.length - 1] = c;
							charMapExtendedValue[charMapExtendedKey.length - 1] = canon;
						}
						break;
					}
					}
				}
				readChars = isr.read(buf);

			}
			/* Bubble sort */
			boolean changed = true;
			char tmp;
			while (changed) {
				changed = false;
				for (int i = 0; i < charMapExtendedKey.length - 1; i++) {
					if (charMapExtendedKey[i] > charMapExtendedKey[i + 1]) {
						tmp = charMapExtendedKey[i];
						charMapExtendedKey[i] = charMapExtendedKey[i + 1];
						charMapExtendedKey[i + 1] = tmp;
						tmp = charMapExtendedValue[i];
						charMapExtendedValue[i] = charMapExtendedValue[i + 1];
						charMapExtendedValue[i + 1] = tmp;
						changed = true;
					}
				}
			}		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String canonial(String s){
		if (charMapCore == null)
			initCharMaps();
		StringBuffer erg=new StringBuffer();
		//System.out.print("canonial '" + s + "' ");
		for (int i=0;i<s.length();i++){
			erg.append(getNumberOf(s.charAt(i)));
		}
		//System.out.println("'" + erg + "'");
		return erg.toString();
	}
	
	private static char getNumberOf(char s){
		if (s == 0)
			return s;
		if ((s >= minFastRange) && (s < maxFastRange)) {
			/* This is the fast path */
			return charMapCore[s - minFastRange];
		} else {
			int minIdx, maxIdx, pivot;
			minIdx = 0;
			maxIdx = charMapExtendedKey.length - 1;
			pivot = (minIdx+maxIdx)/2;
			while (minIdx < maxIdx) {
				if (charMapExtendedKey[pivot] == s) {
					return charMapExtendedValue[pivot];
				}
				if (pivot == minIdx) {
					minIdx++;
					pivot++;
				}
				if (charMapExtendedKey[pivot] > s) {
					maxIdx = pivot;
				} else {
					minIdx = pivot;
				}
				pivot = (minIdx+maxIdx)/2;
			}
			if (charMapExtendedKey[pivot] == s) {
				return charMapExtendedValue[pivot];
			}
		}

		//We need some default character value, we might as well use 1
		return '1';
	}
}