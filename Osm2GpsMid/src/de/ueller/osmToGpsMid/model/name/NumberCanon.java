/**
 * OSM2GpsMid 
 *  
 *
 * @version $Revision$ ($Name$)
 *
 * Copyright (C) 2007 Harald Mueller
 */
package de.ueller.osmToGpsMid.model.name;

/**
 * @author hmueller
 *
 */
public class NumberCanon {
	
	public static String canonial(String s){
		StringBuffer erg=new StringBuffer();
//		System.out.print("canonial '" + s + "' ");
		for (int i=0;i<s.length();i++){
			erg.append(getNumberOf(s.charAt(i)));
		}
//		System.out.println("'" + erg + "'");
		return erg.toString();
	}

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
			return ('2');
		case 'd':
		case 'D':
		case 'e':
		case 'E':
		case 'f':
		case 'F':
		case '3':
			return ('3');
		case 'g':
		case 'G':
		case 'h':
		case 'H':
		case 'i':
		case 'I':
		case '4':
			return ('4');
		case 'j':
		case 'J':
		case 'k':
		case 'K':
		case 'l':
		case 'L':
		case '5':
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
			return ('9');
		case '\0':
			return ('\0');
		default:
			return ('1');
		}
	}

}
