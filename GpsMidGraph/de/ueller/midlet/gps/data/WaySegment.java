package de.ueller.midlet.gps.data;

import de.ueller.midlet.gps.tile.PaintContext;

/** 
 * Class which represents a waysegment. Waysegment is part of a whole way,
 * it is the part between one trackpoint and  the next trackpoint. It is given by 2 
 * IntPoints (with coordinates from the OSM-Database) and the waywidth (which is calculated by the current zoomlevel
 * in gpsmid and the specific waywidth given in the used stylefile while generating gpsmid with osm2gpsmid.)
 */
public class WaySegment {
	/** used for calculations */
	WaySegmentData point = new WaySegmentData();
	/** used to store calculated values */
	WaySegmentData linePoints = new WaySegmentData();

	// Predefined Colors. 
	// Todo: They should be made configurable in the stylefiles.
	public int COL_BRIDGE = 0x00FFFFFF;
	public int COL_BRIDGE_MIDDLE = 0x00FFFFFF;
	public int COL_TUNNEL = 0x00000000;
	
	public static boolean  waycolor = true;
	
	public void drawBridge(PaintContext pc, int x[],int y[],int i,int max,int waywidth, IntPoint edgeA,IntPoint edgeB,IntPoint edgeC,IntPoint edgeD){
		
		point.a.set(x[i],y[i]);
		point.b.set(x[i+1],y[i+1]);

		switch (i){
		case 0: // this is the first segment of the bridge, so paint the starttriangle

			// calculate the top of the starttriangle. the oter 2 points are already given
			point.c.set(						// set point.c to
					point.a.vectorAddRotate90(	// vector point.a, add rotated vector
					point.a.vectorSubstract(edgeA)));	// point.a - a
			
			fillTriangle(COL_BRIDGE,edgeA,edgeC,point.c,pc);
			draw2Wings(edgeA,edgeC,point.c,pc);  // draws the wings, two little lines indicating the start/end of a bridge
		default:
			drawLine(COL_BRIDGE,point.a,point.b,pc);
		
			// this is the last segment of the bridge, so paint the endtriangle
			if (i==max){ 
				point.c.set( 						// set point.c to...
						point.b.vectorAddRotate90( 	// point.b with added and rotated vector...
						edgeA.vectorSubstract(point.a))); // a - point.a
				fillTriangle(COL_BRIDGE,edgeB,edgeD,point.c,pc);
				draw2Wings(edgeB,edgeD,point.c,pc);
			}
		}
	}

	public void drawTunnel(PaintContext pc,int x[],int y[],int i,int max,int w,IntPoint edgeA,IntPoint edgeB,IntPoint edgeC,IntPoint edgeD){
		
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

				fillTriangle(COL_TUNNEL,edgeA,edgeC,point.c,pc);
			default:
				drawWideLine(COL_TUNNEL,point.a, point.b, (int)(w/4f), 0, pc);
				if (i==max){
					point.c.set( 						// set point.c to...
							point.b.vectorAddRotate90( 	// point.b with added and rotated vector...
							edgeA.vectorSubstract(point.a))); // a - point.a
					fillTriangle(COL_TUNNEL,edgeB,edgeD,point.c,pc);
				}
		}// end switch
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
	    if (linewidth <= 1){ // a line with px1 width is faster drawn directly
	    	pc.g.drawLine(point1.x,point1.y,point2.x,point2.y);
	    }
	    else if (linewidth == 0) {
	    	// do nothing
	    }
	    else{

	        switch (align){
	        case 2:        				
		        linePoints.set(point1,point2,linewidth*2);
	        						
				//Change the one side
				linePoints.a.x=point1.x;
				linePoints.a.y=point1.y;
				linePoints.c.x=point2.x;
				linePoints.c.y=point2.y;
				break;
	        case 1:
		        linePoints.set(point1,point2,linewidth*2);
	        	linePoints.b.x=point1.x;
	        	linePoints.b.y=point1.y;
	        	linePoints.d.x=point2.x;
	        	linePoints.d.y=point2.y;	        	
			    break;
	        case 0:
		        linePoints.set(point1,point2,linewidth);
				break;
	        }
	        
	        // now as we have calculated the coords, paint the line
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
