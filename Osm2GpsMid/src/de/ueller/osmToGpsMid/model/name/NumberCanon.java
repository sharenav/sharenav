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

}
