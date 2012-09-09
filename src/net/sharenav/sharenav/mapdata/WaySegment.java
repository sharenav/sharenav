package net.sharenav.sharenav.mapdata;

import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.PaintContext;
import net.sharenav.util.IntPoint;

/** 
 * Class which represents a waysegment. Waysegment is part of a whole way,
 * it is the part between one trackpoint and  the next trackpoint. It is given by 2 
 * IntPoints (with coordinates from the OSM-Database) and the waywidth (which is calculated by the current zoomlevel
 * in sharenav and the specific waywidth given in the used stylefile while generating sharenav with osm2sharenav.)
 */
public class WaySegment {
	/** used for calculations */
	WaySegmentData point = new WaySegmentData();
	/** used to store calculated values */
	WaySegmentData linePoints = new WaySegmentData();

	
	public static boolean  waycolor = true;
	
	public void drawBridge(PaintContext pc, int x[],int y[],int i,int max,int waywidth, IntPoint edgeA,IntPoint edgeB,IntPoint edgeC,IntPoint edgeD){
		
		int colorBridge = Legend.COLORS[Legend.COLOR_BRIDGE_DECORATION];
		
		point.a.set(x[i],y[i]);
		point.b.set(x[i+1],y[i+1]);

		switch (i){
		case 0: // this is the first segment of the bridge, so paint the starttriangle

			// calculate the top of the starttriangle. the oter 2 points are already given
			point.c.set(						// set point.c to
					point.a.vectorAddRotate90(	// vector point.a, add rotated vector
					point.a.vectorSubstract(edgeA)));	// point.a - a
			
			fillTriangle(colorBridge,edgeA,edgeC,point.c,pc);
			draw2Wings(edgeA,edgeC,point.c,pc);  // draws the wings, two little lines indicating the start/end of a bridge
		default:
			drawLine(colorBridge,point.a,point.b,pc);
		
			// this is the last segment of the bridge, so paint the endtriangle
			if (i==max){ 
				point.c.set( 						// set point.c to...
						point.b.vectorAddRotate90( 	// point.b with added and rotated vector...
						edgeA.vectorSubstract(point.a))); // a - point.a
				fillTriangle(colorBridge,edgeB,edgeD,point.c,pc);
				draw2Wings(edgeB,edgeD,point.c,pc);
			}
		}
	}

	public void drawTunnel(PaintContext pc,int x[],int y[],int i,int max,int w,IntPoint edgeA,IntPoint edgeB,IntPoint edgeC,IntPoint edgeD){
		int colorTunnel = Legend.COLORS[Legend.COLOR_TUNNEL_DECORATION];
		
		// do nothing if way is too thin
		if (w <2){
			return;
		}
		
		point.a.set(x[i],y[i]);
		point.b.set(x[i+1],y[i+1]);
		
		switch (i){
			case 0:
				point.c.set(						// set point.c to
						point.a.vectorAddRotate90(	// vector point.a, add rotated vector
						point.a.vectorSubstract(edgeA)));	// point.a - a

				fillTriangle(colorTunnel,edgeA,edgeC,point.c,pc);
			default:
				drawWideLine(colorTunnel,point.a, point.b, (int)(w/4f), 0, pc);
				if (i==max){
					point.c.set( 						// set point.c to...
							point.b.vectorAddRotate90( 	// point.b with added and rotated vector...
							edgeA.vectorSubstract(point.a))); // a - point.a
					fillTriangle(colorTunnel,edgeB,edgeD,point.c,pc);
				}
		}// end switch
	}

	
	public void drawTollRoad(PaintContext pc,int x[],int y[],int i,int max,int w,IntPoint edgeA,IntPoint edgeB,IntPoint edgeC,IntPoint edgeD){
		// do nothing if way is too thin
		if (w < 1){
			return;
		}
		
		// old edge
		point.a.set(edgeC);
		// 0, 0
		point.b.set(0 , 0);
				
		pc.g.setColor(Legend.COLORS[Legend.COLOR_TOLLROAD_DECORATION]);
		
		double distanceLine = edgeA.vectorMagnitude(edgeB);
		double distanceDeco = edgeA.vectorMagnitude(edgeC);
		if (distanceLine != 0) {
			double dx = ( (double) (edgeA.x - edgeB.x) * distanceDeco / distanceLine);
			double dy = ( (double)(edgeA.y - edgeB.y) * distanceDeco / distanceLine);
			point.d.set((int) dx, (int) dy);
			if ( point.d.vectorMagnitude(point.b) * 1000 > distanceLine) { // only if the diff vector will not have to be added more than a 1000 times, i.e. is very small
				for (int j = 1; point.d.vectorMagnitude(point.b) < distanceLine; j++) {
					point.d.set((int) (dx * j), (int) (dy * j));
					if ( ( j & 0x1) == 0 ) {
						point.c.set(
							edgeC.vectorSubstract(point.d)
						);
					} else {
						point.c.set(
							edgeA.vectorSubstract(point.d)
						);
					}
					
					pc.g.drawLine(point.a.x, point.a.y, point.c.x, point.c.y);
					point.a.set(point.c);
//					if (j >= 999) {
//						System.out.println("Endless loop, j: " + j + " dx :" + dx + " dy :" + dy + " dl: " + distanceLine + " mag: " + point.d.vectorMagnitude(point.b) );
//						break;
//					}
				}
			}
		}
	}

	
	public void drawDamage(PaintContext pc,int x[],int y[],int i,int max,int w,IntPoint edgeA,IntPoint edgeB,IntPoint edgeC,IntPoint edgeD){
		int colorDamaged = Legend.COLORS[Legend.COLOR_WAY_DAMAGED_DECORATION];

		// do nothing if way is too thin
//		if (w <2){
//			return;
//		}
		
		point.a.set(x[i],y[i]);
		point.b.set(x[i+1],y[i+1]);
		
		drawWideLine(colorDamaged,point.a, point.b, (int)(w/1.5f), 0, pc);
	}
	
	
	private void fillTriangle(int color, IntPoint a,IntPoint b,IntPoint c,PaintContext pc){
		pc.g.setColor(color);
		pc.g.fillTriangle(a.x,a.y,b.x,b.y,c.x,c.y);
	}
	
	private void drawLine(int color, IntPoint a,IntPoint b,PaintContext pc){
		pc.g.setColor(color);
		pc.g.drawLine(a.x, a.y, b.x, b.y);
	}
	
    /** Draws a wide line from 2 given points.
	 *  @param point1
	 *  @param point2
	 *  @param linewidth how thick (in pixel) should we paint the line
	 *  @param align 0=center, 1 left, 2 richt 
	 *  @param pc paintcontext
	 *  @return Point in the direction of firstPoint->endPoint with distance multiplied by length
	 * */
    public void drawWideLine(int color, IntPoint point1,IntPoint point2,int linewidth,int align, PaintContext pc) {

    	pc.g.setColor(color);
	    
    	/* Always draw a line with px1 width also for wide lines because drawing a wide line might not work in every case.
    	 * This is at least the case on Nokia 5800 with very offscreen lines in some angles (line to destination several 100 km in normal zoom)
    	 */
	    pc.g.drawLine(point1.x,point1.y,point2.x,point2.y);
	    
	    if (linewidth > 1) {
	        switch (align){
	        case 0:
		        linePoints.set(point1,point2,linewidth);
				break;
	        case 1:
		        linePoints.set(point1,point2,linewidth*2);
	        	linePoints.b.x=point1.x;
	        	linePoints.b.y=point1.y;
	        	linePoints.d.x=point2.x;
	        	linePoints.d.y=point2.y;	        	
			    break;
	        case 2:        				
		        linePoints.set(point1,point2,linewidth*2);
	        						
				//Change the one side
				linePoints.a.x=point1.x;
				linePoints.a.y=point1.y;
				linePoints.c.x=point2.x;
				linePoints.c.y=point2.y;
				break;
	        }

	        // System.out.println(linePoints);
	        
	        // now as we have calculated the coords, paint the line
	        pc.g.fillTriangle(linePoints.a.x, linePoints.a.y, linePoints.b.x, linePoints.b.y,linePoints.d.x, linePoints.d.y);
	        pc.g.fillTriangle(linePoints.a.x, linePoints.a.y, linePoints.c.x, linePoints.c.y,linePoints.d.x, linePoints.d.y);
	
	    }
	}

	/**
	 *
	 * @param color
	 * @param point1
	 * @param point2
	 * @param linewidth
	 */
    public void drawWideLineSimple(int color, IntPoint point1, IntPoint point2, int linewidth, PaintContext pc) {

		if ( linewidth == 0 )
			return;

    	pc.g.setColor(color);

    	/* Always draw a line with px1 width also for wide lines because drawing a wide line might not work in every case.
    	 * This is at least the case on Nokia 5800 with very offscreen lines in some angles (line to destination several 100 km in normal zoom)
    	 */
	    pc.g.drawLine(point1.x, point1.y, point2.x, point2.y);

	    if (linewidth > 1) {
			linePoints.set(point1,point2,linewidth-1);

			pc.g.fillTriangle(linePoints.a.x, linePoints.a.y, linePoints.b.x, linePoints.b.y,linePoints.d.x, linePoints.d.y);
			pc.g.fillTriangle(linePoints.a.x, linePoints.a.y, linePoints.c.x, linePoints.c.y,linePoints.d.x, linePoints.d.y);
	     }
	}
	
    public void draw2Wings(IntPoint a,IntPoint b,IntPoint centerPoint, PaintContext pc) {
	    	pc.g.drawLine(a.x +(int)((a.x-centerPoint.x)*0.5f), a.y +(int)((a.y-centerPoint.y)*0.5f), a.x,a.y);
	    	pc.g.drawLine(b.x + (int)((b.x-centerPoint.x)*0.5f), b.y +(int)((b.y-centerPoint.y)*0.5f),b.x,b.y);
    }
	    
    public WaySegmentData getWingsCoos(IntPoint wa,IntPoint wb,float length) {
    	linePoints.a.set(wa.vectorSubstract(linePoints.a).vectorMultiply(length));
    	linePoints.b.set(wa.vectorSubstract(linePoints.a).vectorMultiply(length));
    	linePoints.c.x = 0;
    	linePoints.c.y = 0;
    	linePoints.d.x = 0;
    	linePoints.d.y = 0;
    	
    	return linePoints;
    }
}
