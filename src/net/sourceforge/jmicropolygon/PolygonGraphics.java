// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PolygonGraphics.java

/**
 * this is decompiled from JMicroPolygon
 * @see http://sourceforge.net/projects/jmicropolygon
 * (C) 2007 by Simon Turner
 */

package net.sourceforge.jmicropolygon;

import java.util.Stack;
import javax.microedition.lcdui.Graphics;

// Referenced classes of package net.sourceforge.jmicropolygon:
//            GeomUtils

public class PolygonGraphics
{

    public PolygonGraphics()
    {
    }

    public static void drawPolygon(Graphics g, int xPoints[], int yPoints[])
    {
        int max = xPoints.length - 1;
        for(int i = 0; i < max; i++) {
			g.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
		}

        g.drawLine(xPoints[max], yPoints[max], xPoints[0], yPoints[0]);
    }
    public static void drawOpenPolygon(Graphics g, int xPoints[], int yPoints[],int count)
    {
        for(int i = 0; i < count; i++) {
			g.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
		}
    }

    public static void fillPolygon(Graphics g, int xPoints[], int yPoints[])
    {
        Stack stack = new Stack();
        fillPolygon(g, xPoints, yPoints, stack);
        for(; !stack.isEmpty(); fillPolygon(g, (int[])stack.pop(), (int[])stack.pop(), stack)) {
			;
		}
    }

    private static void fillPolygon(Graphics g, int xPoints[], int yPoints[], Stack stack)
    {
        while(xPoints.length > 2) 
        {
            int a = GeomUtils.indexOfLeast(xPoints);
            int b = (a + 1) % xPoints.length;
            int c = a <= 0 ? xPoints.length - 1 : a - 1;
            int leastInternalIndex = -1;
            boolean leastInternalSet = false;
            if(xPoints.length > 3)
            {
                for(int i = 0; i < xPoints.length; i++) {
					if((i != a) && (i != b) && (i != c) && GeomUtils.withinBounds(xPoints[i], yPoints[i], xPoints[a], yPoints[a], xPoints[b], yPoints[b], xPoints[c], yPoints[c]) && (!leastInternalSet || (xPoints[i] < xPoints[leastInternalIndex])))
                    {
                        leastInternalIndex = i;
                        leastInternalSet = true;
                    }
				}

            }
            if(!leastInternalSet)
            {
                g.fillTriangle(xPoints[a], yPoints[a], xPoints[b], yPoints[b], xPoints[c], yPoints[c]);
                int trimmed[][] = GeomUtils.trimEar(xPoints, yPoints, a);
                xPoints = trimmed[0];
                yPoints = trimmed[1];
                continue;
            }
            int split[][][] = GeomUtils.split(xPoints, yPoints, a, leastInternalIndex);
            int poly1[][] = split[0];
            int poly2[][] = split[1];
            stack.push(poly2[1]);
            stack.push(poly2[0]);
            stack.push(poly1[1]);
            stack.push(poly1[0]);
            break;
        }
    }
}
