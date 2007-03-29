// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   GeomUtils.java

/**
 * this is decompiled from JMicroPolygon
 * @see http://sourceforge.net/projects/jmicropolygon
 * (C) 2007 by Simon Turner
 */

package net.sourceforge.jmicropolygon;


public abstract class GeomUtils
{

    public GeomUtils()
    {
    }

    static boolean withinBounds(int px, int py, int ax, int ay, int bx, int by, int cx, int cy)
    {
        if(px < min(ax, bx, cx) || px > max(ax, bx, cx) || py < min(ay, by, cy) || py > max(ay, by, cy))
            return false;
        boolean sameabc = sameSide(px, py, ax, ay, bx, by, cx, cy);
        boolean samebac = sameSide(px, py, bx, by, ax, ay, cx, cy);
        boolean samecab = sameSide(px, py, cx, cy, ax, ay, bx, by);
        return sameabc && samebac && samecab;
    }

    static int[][][] split(int xPoints[], int yPoints[], int aIndex, int bIndex)
    {
        int firstLen;
        if(bIndex < aIndex)
            firstLen = (xPoints.length - aIndex) + bIndex + 1;
        else
            firstLen = (bIndex - aIndex) + 1;
        int secondLen = (xPoints.length - firstLen) + 2;
        int first[][] = new int[2][firstLen];
        int second[][] = new int[2][secondLen];
        for(int i = 0; i < firstLen; i++)
        {
            int index = (aIndex + i) % xPoints.length;
            first[0][i] = xPoints[index];
            first[1][i] = yPoints[index];
        }

        for(int i = 0; i < secondLen; i++)
        {
            int index = (bIndex + i) % xPoints.length;
            second[0][i] = xPoints[index];
            second[1][i] = yPoints[index];
        }

        int result[][][] = new int[2][][];
        result[0] = first;
        result[1] = second;
        return result;
    }

    static int[][] trimEar(int xPoints[], int yPoints[], int earIndex)
    {
        int newXPoints[] = new int[xPoints.length - 1];
        int newYPoints[] = new int[yPoints.length - 1];
        int newPoly[][] = new int[2][];
        newPoly[0] = newXPoints;
        newPoly[1] = newYPoints;
        int p = 0;
        for(int i = 0; i < xPoints.length; i++)
            if(i != earIndex)
            {
                newXPoints[p] = xPoints[i];
                newYPoints[p] = yPoints[i];
                p++;
            }

        return newPoly;
    }

    static int indexOfLeast(int elements[])
    {
        int index = 0;
        int least = elements[0];
        for(int i = 1; i < elements.length; i++)
            if(elements[i] < least)
            {
                index = i;
                least = elements[i];
            }

        return index;
    }

    private static boolean sameSide(int p1x, int p1y, int p2x, int p2y, int l1x, int l1y, int l2x, int l2y)
    {
        long lhs = (p1x - l1x) * (l2y - l1y) - (l2x - l1x) * (p1y - l1y);
        long rhs = (p2x - l1x) * (l2y - l1y) - (l2x - l1x) * (p2y - l1y);
        long product = lhs * rhs;
        boolean result = product >= 0L;
        return result;
    }

    private static int min(int a, int b, int c)
    {
        return Math.min(Math.min(a, b), c);
    }

    private static int max(int a, int b, int c)
    {
        return Math.max(Math.max(a, b), c);
    }
}
