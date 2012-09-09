/**
 * OSM2ShareNav 
 *
 *
 *
 * Copyright (C) 2007 Harald Mueller
 * Copyright (C) 2008 Kai Krueger
 */
package net.sharenav.osmToShareNav.model.name;

import java.io.InputStream;
import java.io.InputStreamReader;

import net.sharenav.osmToShareNav.Configuration;

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
			int line = 1;
			int column = 0;
			char canon = '1';
			int charType = canonType;
			while (readChars > 0) {
				for (int i = 0; i < readChars; i++) {
					char c = buf[idx++];
					column++;
					switch (charType) {
					case canonType: {
						if ((c == '\n') || (c == '\r')) {
							column = 0;
							line++;
							break;
						}
						if (c == '\t') {
							// increase the column at a tab char assuming a tab position is at every 4th column
							while ( (column % 4) != 0 ) {
								column++;
							}
						} else {
							charType = normType;
						}
						Integer.parseInt(new StringBuffer().append(c).toString());
						canon = c;
						break;
					}
					case normType: {
						if (c == '\t') {
							// increase the column at a tab char assuming a tab position is at every 4th column
							while ( (column % 4) != 0 ) {
								column++;
							}
							break;
						}
						if ((c == '\n') || (c == '\r')) {
							charType = canonType;
							column = 0;
							line++;
							break;
						}
						if ((c >= minFastRange) && (c < maxFastRange)) {
							/* check for duplicate in core map */
							if (charMapCore[c - minFastRange] != defaultChar) {
								System.out.println("! charmap.txt: " + c + " mapped to " + charMapCore[c - minFastRange] + " and would be mapped to " + canon + " at line " + line + " col " + column);								
							}
							charMapCore[c - minFastRange] = canon;
						} else {
							/* check for duplicate in extended map */
							for (int j = 0; j < charMapExtendedKey.length; j++) {
								if (charMapExtendedKey[j] == c) {
									System.out.println("! charmap.txt: " + c + " mapped to " + charMapExtendedValue[j] + " and would be mapped to " + canon + " at line " + line + " col " + column);								
								}
							}							
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
//		System.out.print("getNumberOf() s: '" + s + "' ");
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
		case 'Ã¤':
		case 'Ã„':
		case 'b':
		case 'B':
		case 'c':
		case 'C':
		case '2':
		case 'Ã�':
		case 'Ã¡':
		case 'Ä„':
		case 'Ä…':
		case 'ÄŒ':
		case 'Ä�':		
		case 'Ã€':
		case 'Ã ':
		case 'Ã‚':
		case 'Ã¢':
		case 'Ã‡':
		case 'Ã§':
// Greek
		case 'Î‘':
		case 'Î†':
		case 'Î±':
		case 'Î¬':
		case 'Î’':
		case 'Î²':
		case 'Î“':
		case 'Î³':
			return ('2');
		case 'd':
		case 'D':
		case 'e':
		case 'E':
		case 'f':
		case 'F':
		case '3':
		case 'Ãˆ':
		case 'Ã¨':
		case 'Ã‰':
		case 'Ã©':
		case 'ÃŠ':
		case 'Ãª':
		case 'Ã‹':
		case 'Ã«':
		case 'Ä˜':
		case 'Ä™':
		case 'Äš':
		case 'Ä›':
		case 'Ä�':
// Greek
		case 'Î”':
		case 'Î´':
		case 'Î•':
		case 'Îˆ':
		case 'Îµ':
		case 'Î­':
		case 'Î–':
		case 'Î¶':
			return ('3');
		case 'g':
		case 'G':
		case 'h':
		case 'H':
		case 'i':
		case 'I':
		case '4':
		case 'ÃŽ':
		case 'Ã®':
		case 'Ã�':
		case 'Ã¯':
		case 'Ã�':
		case 'Ã­':
		case 'ÃŒ':
		case 'Ã¬':
		case 'Ä°':
		case 'Ä±':
		case 'Î¯':
// Greek
		case 'Î—':
		case 'Î‰':
		case 'Î·':
		case 'Î®':
		case 'Î˜':
		case 'Î¸':
		case 'Î™':
		case 'Îª':
		case 'Î¹':
		case 'Î�':
		case 'ÏŠ':
			return ('4');
		case 'j':
		case 'J':
		case 'k':
		case 'K':
		case 'l':
		case 'L':
		case '5':
		case 'Ä¹':
		case 'Äº':
		case 'Å�':
		case 'Å‚':
// Greek
		case 'Îš':
		case 'Îº':
		case 'Î›':
		case 'Î»':
		case 'Î¼':
		case 'Îœ':
			return ('5');
		case 'm':
		case 'M':
		case 'n':
		case 'N':
		case 'o':
		case 'O':
		case 'Ã¶':
		case 'Ã–':
		case '6':
		case 'Ã“':
		case 'Ã³':
		case 'Ã”':
		case 'Ã´':
		case 'Å‡':
		case 'Åˆ':
		case 'Ã’':
		case 'Ã²':
		case 'Åƒ':
		case 'Å„':
		case 'Ã‘':
		case 'Ã±':
		case 'Å’':
		case 'Å“':
// Greek
		case 'Î�':
		case 'Î½':
		case 'Îž':
		case 'Î¾':
		case 'ÎŸ':
		case 'ÎŒ':
		case 'Î¿':
		case 'ÏŒ':
			return ('6');
		case 'p':
		case 'P':
		case 'q':
		case 'Q':
		case 'r':
		case 'R':
		case 's':
		case 'S':
		case 'ÃŸ':
		case '7':
		case 'Å”':
		case 'Å•':
		case 'Å˜':
		case 'Å™':
		case 'Å ':
		case 'Å¡':
		case 'Åš':
		case 'Å›':
		case 'È˜':
		case 'È™':
		case 'Åž':
		case 'ÅŸ':
// Greek
		case 'Î ':
		case 'Ï€':
		case 'Î¡':
		case 'Ï�':
		case 'Î£':
		case 'Ïƒ':
		case 'Ï‚':
			return ('7');
		case 't':
		case 'T':
		case 'u':
		case 'U':
		case 'Ã¼':
		case 'Ãœ':
		case 'v':
		case 'V':
		case '8':
		case 'Ãš':
		case 'Ãº':
		case 'Å®':
		case 'Å¯':
		case 'Ã™':
		case 'Ã¹':
		case 'Èš':
		case 'È›':
		case 'Å¢':
		case 'Å£':
		case 'Ã›':
		case 'Ã»':
// Greek
		case 'Î¤':
		case 'Ï„':
		case 'Î¥':
		case 'ÎŽ':
		case 'Î«':
		case 'Ï’':
		case 'Ï“':
		case 'Ï”':
		case 'Ï…':
		case 'Î°':
		case 'Ï‹':
		case 'Ï�':
		case 'Î¦':
		case 'Ï†':
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
		case 'Å¸':
		case 'Ã¿':
		case 'Ã�':
		case 'Ã½':
		case 'Å¹':
		case 'Åº':
		case 'Å»':
		case 'Å¼':
		case 'Å½':
		case 'Å¾':
// Greek
		case 'Î§':
		case 'Ï‡':
		case 'Î¨':
		case 'Ïˆ':
		case 'Î©':
		case 'Î�':
		case 'Ï‰':			
		case 'ÏŽ':			
			return ('9');
		case '\0':
			return ('\0');
		default:
			return ('1');
		}
	}
*/
}
