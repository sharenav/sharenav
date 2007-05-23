package de.ueller.midlet.graphics;

import javax.microedition.lcdui.Graphics;

import de.ueller.midlet.gps.data.MoreMath;

public class Street {
	public static void draw(Graphics g,
			                int w,
			                int xPoints[], int yPoints[]){
        int max = xPoints.length - 1;
        int w2=(w*w)/4;
        for(int i = 0; i < max; i++) {
        	int dx=xPoints[i+1]-xPoints[i];
        	int dy=yPoints[i+1]-yPoints[i];
        	int l2=dx*dx+dy*dy;
        	float l2f=(float) Math.sqrt(l2);
        	float lf=w/l2f;
        	int xb=(int) (Math.abs(lf*dy));
        	int yb=(int) (Math.abs(lf*dx));
        	int rfx=1;
        	int rfy=1;
        	if (dy < 0){rfx=-1;}
        	if (dx > 0){rfy=-1;}
        	
        	int end1x = xPoints[i+1]-(rfx*xb);
			int end1y = yPoints[i+1]-(rfy*yb);
        	int end2x = xPoints[i+1]+(rfx*xb);
			int end2y = yPoints[i+1]+(rfy*yb);
			int beg1x = xPoints[i]+(rfx*xb);
			int beg1y = yPoints[i]+(rfy*yb);
			int beg2x = xPoints[i]-(rfx*xb);
			int beg2y = yPoints[i]-(rfy*yb);
			//        	g.setColor(255,0,0);
        	g.fillTriangle(beg1x,
		               beg1y,
		               beg2x,
		               beg2y,
		               end2x,
		               end2y);
			//        	g.setColor(0,255,0);
        	g.fillTriangle(beg2x,
		               beg2y,
		               end1x,
		               end1y,
		               end2x,
		               end2y);
//        	g.setColor(0,0,0);
//        	g.drawLine(xPoints[i], yPoints[i],
//        			xPoints[i+1], yPoints[i+1]);
		}

	}
}
