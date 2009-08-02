/**
 * OSM2GpsMid 
 *
 *
 *
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2008 Kai Krueger
 */
package de.ueller.osmToGpsMid.model.name;

import java.io.InputStream;
import java.io.InputStreamReader;

import de.ueller.osmToGpsMid.Configuration;

public class NumberCanon {
	
	private static char [] charMapCore; //fast direct lookup table (Can't cover the entire unicode code range though)
	private static char [] charMapExtendedKey = new char [0]; //ordered Map to store the rest of the char -> num mappings
	private static char [] charMapExtendedValue = new char [0];
	private static char minFastRange = 1; //Specify the codepoint range that gets mapped in the lookup table
	private static char maxFastRange = 256;
	private static char defaultChar = '1'; //All unknown characters get mapped to this
	
	private static final int canonType = 0;
	private static final int normType = 2;

	public static void initCharMaps() {
		try {
			charMapCore = new char[maxFastRange - minFastRange];
			for (int i = 0; i < charMapCore.length; i++) {
				charMapCore[i] = defaultChar;
			}
			InputStream is = Configuration.getConfiguration().getCharMapStream();
			InputStreamReader isr = new InputStreamReader(is,"UTF8");
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
			isr.close();
			is.close();
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

	/*
	private static char getNumberOf(char s){
		switch (s){
		case '0':
			return ('0');
		case 'a':
		case 'A':
		case 'ä':
		case 'Ä':
		case 'b':
		case 'B':
		case 'c':
		case 'C':
		case '2':
		case 'Á':
		case 'á':
		case 'Ą':
		case 'ą':
		case 'Č':
		case 'č':		
		case 'À':
		case 'à':
		case 'Â':
		case 'â':
		case 'Ç':
		case 'ç':
// Greek
		case 'Α':
		case 'Ά':
		case 'α':
		case 'ά':
		case 'Β':
		case 'β':
		case 'Γ':
		case 'γ':
			return ('2');
		case 'd':
		case 'D':
		case 'e':
		case 'E':
		case 'f':
		case 'F':
		case '3':
		case 'È':
		case 'è':
		case 'É':
		case 'é':
		case 'Ê':
		case 'ê':
		case 'Ë':
		case 'ë':
		case 'Ę':
		case 'ę':
		case 'Ě':
		case 'ě':
		case 'ď':
// Greek
		case 'Δ':
		case 'δ':
		case 'Ε':
		case 'Έ':
		case 'ε':
		case 'έ':
		case 'Ζ':
		case 'ζ':
			return ('3');
		case 'g':
		case 'G':
		case 'h':
		case 'H':
		case 'i':
		case 'I':
		case '4':
		case 'Î':
		case 'î':
		case 'Ï':
		case 'ï':
		case 'Í':
		case 'í':
		case 'Ì':
		case 'ì':
		case 'İ':
		case 'ı':
		case 'ί':
// Greek
		case 'Η':
		case 'Ή':
		case 'η':
		case 'ή':
		case 'Θ':
		case 'θ':
		case 'Ι':
		case 'Ϊ':
		case 'ι':
		case 'ΐ':
		case 'ϊ':
			return ('4');
		case 'j':
		case 'J':
		case 'k':
		case 'K':
		case 'l':
		case 'L':
		case '5':
		case 'Ĺ':
		case 'ĺ':
		case 'Ł':
		case 'ł':
// Greek
		case 'Κ':
		case 'κ':
		case 'Λ':
		case 'λ':
		case 'μ':
		case 'Μ':
			return ('5');
		case 'm':
		case 'M':
		case 'n':
		case 'N':
		case 'o':
		case 'O':
		case 'ö':
		case 'Ö':
		case '6':
		case 'Ó':
		case 'ó':
		case 'Ô':
		case 'ô':
		case 'Ň':
		case 'ň':
		case 'Ò':
		case 'ò':
		case 'Ń':
		case 'ń':
		case 'Ñ':
		case 'ñ':
		case 'Œ':
		case 'œ':
// Greek
		case 'Ν':
		case 'ν':
		case 'Ξ':
		case 'ξ':
		case 'Ο':
		case 'Ό':
		case 'ο':
		case 'ό':
			return ('6');
		case 'p':
		case 'P':
		case 'q':
		case 'Q':
		case 'r':
		case 'R':
		case 's':
		case 'S':
		case 'ß':
		case '7':
		case 'Ŕ':
		case 'ŕ':
		case 'Ř':
		case 'ř':
		case 'Š':
		case 'š':
		case 'Ś':
		case 'ś':
		case 'Ș':
		case 'ș':
		case 'Ş':
		case 'ş':
// Greek
		case 'Π':
		case 'π':
		case 'Ρ':
		case 'ρ':
		case 'Σ':
		case 'σ':
		case 'ς':
			return ('7');
		case 't':
		case 'T':
		case 'u':
		case 'U':
		case 'ü':
		case 'Ü':
		case 'v':
		case 'V':
		case '8':
		case 'Ú':
		case 'ú':
		case 'Ů':
		case 'ů':
		case 'Ù':
		case 'ù':
		case 'Ț':
		case 'ț':
		case 'Ţ':
		case 'ţ':
		case 'Û':
		case 'û':
// Greek
		case 'Τ':
		case 'τ':
		case 'Υ':
		case 'Ύ':
		case 'Ϋ':
		case 'ϒ':
		case 'ϓ':
		case 'ϔ':
		case 'υ':
		case 'ΰ':
		case 'ϋ':
		case 'ύ':
		case 'Φ':
		case 'φ':
			return ('8');
		case 'w':
		case 'W':
		case 'x':
		case 'X':
		case 'y':
		case 'Y':
		case 'z':
		case 'Z':
		case '9':
		case 'Ÿ':
		case 'ÿ':
		case 'Ý':
		case 'ý':
		case 'Ź':
		case 'ź':
		case 'Ż':
		case 'ż':
		case 'Ž':
		case 'ž':
// Greek
		case 'Χ':
		case 'χ':
		case 'Ψ':
		case 'ψ':
		case 'Ω':
		case 'Ώ':
		case 'ω':			
		case 'ώ':			
			return ('9');
		case '\0':
			return ('\0');
		default:
			return ('1');
		}
	}
*/
}
