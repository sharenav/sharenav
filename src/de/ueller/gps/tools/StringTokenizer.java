package de.ueller.gps.tools;

import java.util.Vector;

public class StringTokenizer {

    public static Vector getVector(String tokenList, String separator) {
        Vector tokens = new Vector();
        int commaPos = 0;
        String token = "";
        int cnt = 0;
        commaPos = tokenList.indexOf(separator);
        while (commaPos > 0) {
            commaPos = tokenList.indexOf(separator);
            if (commaPos > 0) {
                token = tokenList.substring(0, commaPos);
                tokenList = tokenList.substring(commaPos,tokenList.length());
            }
            if (!token.startsWith(separator))
                tokens.addElement(token);
            while (tokenList.startsWith(separator)) {
                cnt++;
                if (cnt >= 2)
                    tokens.addElement("");
                tokenList = tokenList.substring(1,tokenList.length());
                commaPos = tokenList.indexOf(separator);
            }
            cnt = 0;
        }
        if (commaPos < 0) {
            token = tokenList;
            tokens.addElement(token);
        }
        return tokens;
    }

    public static String[] getArray(String tokenList, String separator) {
        Vector tokens = getVector(tokenList,separator);
        String[] st = new String[tokens.size()];
        for (int i = 0; i <= tokens.size() - 1; i++)
            st[i] = (String)tokens.elementAt(i);
        return st;
    }

}
