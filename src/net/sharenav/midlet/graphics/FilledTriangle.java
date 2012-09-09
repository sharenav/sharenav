/*
 * ShareNav - Copyright (c) 2012 sk750 at users dot sourceforge dot net 
 * See file COPYING
 */

package net.sharenav.midlet.graphics;

import java.io.IOException;

import javax.microedition.lcdui.Graphics;

import net.sharenav.sharenav.data.PaintContext;


public class FilledTriangle {

	/* based on http://allwrong.wordpress.com/2009/06/02/a-better-j2me-filltriangle/ which is public domain code */
    public static void fillTriangle(PaintContext pc, int ax, int ay, int bx, int by, int cx, int cy)
    {
        // Sort the points so that ay <= by <= cy
        int temp;
        if (ay > by)
        {
            temp = ax;
            ax = bx;
            bx = temp;
            temp = ay;
            ay = by;
            by = temp;       
        }
        if (by > cy)
        {
            temp = bx;
            bx = cx;
            cx = temp;
            temp = by;
            by = cy;
            cy = temp; 
        }
        if (ay > by)
        {
            temp = ax;
            ax = bx;
            bx = temp;
            temp = ay;
            ay = by;
            by = temp; 
        }       

        // Calc the deltas for each edge.
        int ab_num;
        int ab_den;
        if (by - ay > 0)
        {
            ab_num = (bx - ax);
            ab_den = (by - ay);
        }
        else
        {
            ab_num = (bx - ax);
            ab_den = 1;
        }

        int ac_num;
        int ac_den;
        if (cy - ay > 0)
        {
            ac_num = (cx - ax);
            ac_den = (cy - ay);
        }
        else
        {
            ac_num = 0;
            ac_den = 1;
        }

        int bc_num;
        int bc_den;
        if (cy - by > 0)
        {
            bc_num = (cx - bx);
            bc_den = (cy - by);
        }
        else
        {
            bc_num = 0;
            bc_den = 1;
        }

        // The start and end of each line.
        int sx;
        int ex;
        
        // The heights of the two components of the triangle.
        int h1 = by - ay;
        int h2 = cy - by;
        
        // Some calculations extracted from the loops.
        int ab_num_x2 = ab_num * 2;
        int ab_den_x2 = ab_den * 2;
        
        int ac_num_x2 = ac_num * 2;
        int ac_den_x2 = ac_den * 2;
        
        int bc_num_x2 = bc_num * 2;
        int bc_den_x2 = bc_den * 2;

        // If a is to the left of b...
        if (ax < bx)
        {
            // For each row of the top component...
            for (int y = 0; y < h1; y++)
            {
                sx = ax + (ac_num_x2 * y +  ac_den) / ac_den_x2;
                ex = ax + (ab_num_x2 * y +  ab_den) / ab_den_x2;
                drawHorizontalLine(pc, sx, ex, ay + y);
            }
            // For each row of the bottom component...
            for (int y = 0; y < h2; y++)
            {
                int y2 = h1 + y;
                sx = ax + (ac_num_x2 * y2 + ac_den) / ac_den_x2;
                ex = bx + (bc_num_x2 * y + bc_den) / bc_den_x2;
                drawHorizontalLine(pc, sx, ex, by + y);
            }
        }
        else
        {
            // For each row of the bottom component...
            for (int y = 0; y < h1; y++)
            {
                sx = ax + (ab_num_x2 * y + ab_den) / ab_den_x2;
                ex = ax + (ac_num_x2 * y + ac_den) / ac_den_x2;
                drawHorizontalLine(pc, sx, ex, ay + y);
            }
            // For each row of the bottom component...
            for (int y = 0; y < h2; y++)
            {
                int y2 = h1 + y;
                sx = bx + (bc_num_x2 * y + bc_den) / bc_den_x2;
                ex = ax + (ac_num_x2 * y2 + ac_den) / ac_den_x2;
                drawHorizontalLine(pc, sx, ex, by + y);
            }
        }
    }

    private static void drawHorizontalLine(PaintContext pc, int x1, int x2, int y)
    {
        // draw only odd numbered lines...
        if ((y & 1) == 1)
        {
            pc.g.drawLine(x1, y, x2, y);
        }
    }
	
}
