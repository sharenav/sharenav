package de.ueller.midlet.gps.data;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 			Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import net.sourceforge.jmicropolygon.PolygonGraphics;
import de.enough.polish.util.DrawUtil;
import de.ueller.gps.data.Configuration;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.WayDescription;

public class Way extends Entity{
	
	public static final byte WAY_FLAG_NAME = 1;
	public static final byte WAY_FLAG_MAXSPEED = 2;
	public static final byte WAY_FLAG_LAYER = 4;
	public static final byte WAY_FLAG_LONGWAY = 8;
	public static final byte WAY_FLAG_ONEWAY = 16;	
//	public static final byte WAY_FLAG_MULTIPATH = 4;	
	public static final byte WAY_FLAG_NAMEHIGH = 32;
	public static final byte WAY_FLAG_AREA = 64;
//	public static final byte WAY_FLAG_ISINHIGH = 64;
	
	public static final byte DRAW_BORDER=1;
	public static final byte DRAW_AREA=2;
	public static final byte DRAW_FULL=3;
	
	private static final int MaxSpeedMask = 0xff;
	private static final int MaxSpeedShift = 0;
	private static final int ModMask = 0xff00;
	private static final int ModShift = 8;
	
	public static final int WAY_ONEWAY=1 << ModShift;
	public static final int WAY_AREA=2 << ModShift;

	public static final byte PAINTMODE_COUNTFITTINGCHARS = 0;
	public static final byte PAINTMODE_DRAWCHARS = 1;
	public static final byte INDENT_PATHNAME = 2;
	
	protected static final Logger logger = Logger.getInstance(Way.class,Logger.TRACE);

	private int flags=0;


	public short[] path;
	public short minLat;
	public short minLon;
	public short maxLat;
	public short maxLon;
	
	/**
	 * This is a buffer for the drawing routines
	 * so that we don't have to allocate new
	 * memory at each time we draw a way. This
	 * saves some time on memory managment
	 * too.
	 * 
	 * This makes this function thread unsafe,
	 * so make sure we only have a single thread
	 * drawing a way at a time
	 */
	private static int [] x = new int[100];
	private static int [] y = new int[100];
	private static Font areaFont;
	private static int areaFontHeight;
	private static Font pathFont;
	private static int pathFontHeight;
	private static int pathFontMaxCharWidth;
	private static int pathFontBaseLinePos;
	
	

	static final IntPoint l1b = new IntPoint();
	static final IntPoint l1e = new IntPoint();
	static final IntPoint l2b = new IntPoint();
	static final IntPoint l2e = new IntPoint();
	static final IntPoint l3b = new IntPoint();
	static final IntPoint l3e = new IntPoint();
	static final IntPoint l4b = new IntPoint();
	static final IntPoint l4e = new IntPoint();
	static final IntPoint s1 = new IntPoint();
	static final IntPoint s2 = new IntPoint();
	

//	private final static Logger logger = Logger.getInstance(Way.class,
//			Logger.TRACE);

	/**
	 * the flag should be readed by caller. if Flag == 128 this is a dummy Way
	 * and can ignored.
	 * 
	 * @param is Tile inputstream
	 * @param f flags
	 * @param t Tile
	 * @param layers: this is somewhat awkward. We need to get the layer information back out to 
	 * 			the caller, so use a kind of call by reference
	 * @paran idx index into the layers array where to store the layer info.
	 * @param nodes
	 * @throws IOException
	 */
	public Way(DataInputStream is, byte f, Tile t, byte[] layers, int idx) throws IOException {
		minLat = is.readShort();
		minLon = is.readShort();
		maxLat = is.readShort();
		maxLon = is.readShort();
//		if (is.readByte() != 0x58){
//			logger.error("worng magic after way bounds");
//		}
		//System.out.println("Way flags: " + f);
		type = is.readByte();
		if ((f & WAY_FLAG_NAME) == WAY_FLAG_NAME) {
			if ((f & WAY_FLAG_NAMEHIGH) == WAY_FLAG_NAMEHIGH) {
				nameIdx = is.readInt();
				//System.out.println("Name_High " + f );
			} else {
				nameIdx = is.readShort();
			}			
		}
		if ((f & WAY_FLAG_MAXSPEED) == WAY_FLAG_MAXSPEED) {
//			logger.debug("read maxspeed");
			flags = is.readByte();
		}
		layers[idx] = 0;
		if ((f & WAY_FLAG_LAYER) == WAY_FLAG_LAYER) {
			/**
			 * TODO: We are currently ignoring the layer info
			 * Please implement proper support for this when rendering
			 */
			layers[idx] = is.readByte();
		}
		if ((f & WAY_FLAG_ONEWAY) == WAY_FLAG_ONEWAY) {
			flags += WAY_ONEWAY;
		}
		if (((f & WAY_FLAG_AREA) == WAY_FLAG_AREA) || C.getWayDescription(type).isArea) {
			if ((f & WAY_FLAG_AREA) == WAY_FLAG_AREA){
				logger.debug("Loading explicit Area: " + this);
				System.out.println("f: " + f);
			}
			flags += WAY_AREA;
		}

		boolean longWays=false;
		if ((f & 8) == 8) {
			longWays=true;
		}

			int count;
			if (longWays){
				count = is.readShort();
				if (count < 0) {
					count+=65536;
				}
			} else {
				count = is.readByte();
				if (count < 0) {
					count+=256;
				}
				
			}
			path = new short[count];
			for (short i = 0; i < count; i++) {
				path[i] = is.readShort();
//				logger.debug("read node id=" + path[i]);
			}
//			if (is.readByte() != 0x59 ){
//				logger.error("wrong magic code after path");
//			}			
	}

	public boolean isOnScreen( short pcLDlat, short pcLDlon, short pcRUlat, short pcRUlon) { 
		if ((maxLat < pcLDlat) || (minLat > pcRUlat)) {
			return false;
		}
		if ((maxLon < pcLDlon) || (minLon > pcRUlon)) {
			return false;
		}
		return true; 
	} 

	public void paintAsPath(PaintContext pc, SingleTile t) {
		WayDescription wayDesc = C.getWayDescription(type);
		int w = 0;

		byte om = C.getWayOverviewMode(type);
		switch (om & C.OM_MODE_MASK) {
		case C.OM_SHOWNORMAL: 
			// if not in Overview Mode check for scale
			if (pc.scale > wayDesc.maxScale * Configuration.getDetailBoostMultiplier()) {			
				return;
			}
			break;
		case C.OM_HIDE: 
			if (wayDesc.hideable) {
				return;
			}
			break;
		}

		switch (om & C.OM_NAME_MASK) {
			case C.OM_WITH_NAMEPART: 
				if (nameIdx == -1) return;
				String name = pc.trace.getName(nameIdx);
				if (name == null) return;
				if (name.toUpperCase().indexOf(C.get0Poi1Area2WayNamePart((byte) 2).toUpperCase()) == -1) return;
				break;
			case C.OM_WITH_NAME: 
				if (nameIdx == -1) return;
				break;
			case C.OM_NO_NAME: 
				if (nameIdx != -1) return;
				break;
		}
				
		/**
		 * Calculate the width of the path to be drawn. A width of 1 corresponds to
		 * it being draw as a thin line rather than as a street 
		 */
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_STREETRENDERMODE) && wayDesc.wayWidth>1 ){
			w = (int)(pc.ppm*wayDesc.wayWidth/2 + 0.5);
		}
		
		IntPoint lineP1 = pc.lineP1;
		IntPoint lineP2 = pc.lineP2;
		IntPoint swapLineP = pc.swapLineP;
		Projection p = pc.getP();
		
		int pi=0;
		
		/**
		 * If the static array is not large enough, increase it
		 */
		if (x.length < path.length) {		
			x = new int[path.length];
			y = new int[path.length];
		}
		
		
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2,t);
			if (lineP1 == null) {
				lineP1 = lineP2;
				lineP2 = swapLineP;	
				x[pi] = lineP1.x;
				y[pi++] = lineP1.y;				
			} else {
				/**
				 * We save some rendering time, by doing a line simplifation on the fly.
				 * If two nodes are very close by, then we can simply drop one of the nodes
				 * and draw the line between the other points. 
				 */
				if (! lineP1.approximatelyEquals(lineP2)){
					float dst = MoreMath.ptSegDistSq(lineP1.x, lineP1.y,
							lineP2.x, lineP2.y, pc.xSize / 2, pc.ySize / 2);
					if (dst < pc.squareDstToWay) {
						//System.out.println("set new current Way1 "+ pc.trace.getName(this.nameIdx) + " new dist "+ dst + " old " + pc.squareDstToWay);						
						pc.squareDstToWay = dst;
						pc.actualWay = this;												
					}
					if (dst < pc.squareDstToRoutableWay && wayDesc.routable) {
						pc.squareDstToRoutableWay = dst;
						pc.nearestRoutableWay = this;												
					}
					x[pi] = lineP2.x;
					y[pi++] = lineP2.y;					
					swapLineP = lineP1;
					lineP1 = lineP2;
					lineP2 = swapLineP;
				} else if ((i1+1) == path.length){
					/**
					 * This is an endpoint, so we can't simply drop it, as the lines would potentially look disconnected
					 */
					//System.out.println(" endpoint " + lineP2.x + "/" + lineP2.y+ " " +pc.trace.getName(nameIdx));					
					if (!lineP1.equals(lineP2)){
						x[pi] = lineP2.x;
						y[pi++] = lineP2.y;												
					} else {
						//System.out.println("   discarding never the less");
					}
				}else {
					/**
					 * Drop this point, it is redundant
					 */					
					//System.out.println(" discard " + lineP2.x + "/" + lineP2.y+ " " +pc.trace.getName(nameIdx));
				}
			}
		}
		swapLineP = lineP1;
		lineP1 = null;

		if (pc.target != null && this.equals(pc.target.e)){
			draw(pc,(w==0)?1:w,x,y,pi-1,true);
		} else {
			if (w == 0){
				setColor(pc);
				PolygonGraphics.drawOpenPolygon(pc.g, x, y,pi-1);
			} else {
				draw(pc, w, x, y,pi-1,false);
			}
		}		
		
		if ((pc.nearestRoutableWay == this) && ((pc.currentPos == null) || (pc.currentPos.e != this))) {
			pc.currentPos=new PositionMark(pc.center.radlat,pc.center.radlon);
			pc.currentPos.setEntity(this, getNodesLatLon(t, true), getNodesLatLon(t, false));
		}
		if (isOneway()) {
			paintPathOnewayArrows(pc, t);
		}
		paintPathName(pc, t);
	}
	

    public void paintPathName(PaintContext pc, SingleTile t) {
		//boolean info=false;
    	
    	// exit if not zoomed in enough
    	WayDescription wayDesc = C.getWayDescription(type);
		if (pc.scale > wayDesc.maxTextScale * Configuration.getDetailBoostMultiplier() ) {			
			return;
		}	

		if ( !Configuration.getCfgBitState(Configuration.CFGBIT_WAYTEXTS)) {
			return;
		}

		
		//remember previous font
		Font originalFont = pc.g.getFont();
		if (pathFont==null) {
			pathFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			pathFontHeight=pathFont.getHeight();
			pathFontMaxCharWidth = pathFont.charWidth('W');
			pathFontBaseLinePos = pathFont.getBaselinePosition();
		}
		// At least the half font height must fit to the on-screen-width of the way
		// (is calculation of w correct???)
		int w = (int)(pc.ppm*wayDesc.wayWidth);
		if (pathFontHeight/4>w) {
			return;
		}
		
		String name=null;
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE)) {
			name=wayDesc.description;
		} else {			
			if (nameIdx != -1) {
				name=Trace.getInstance().getName(nameIdx);
			}
		}

		
		if (name==null) {
			return;
		}		

		// determine region in which chars can be drawn
		int minCharScreenX = pc.g.getClipX() - pathFontMaxCharWidth;
		int minCharScreenY = pc.g.getClipY() - pathFontBaseLinePos - (w/2);
		int maxCharScreenX = minCharScreenX + pc.g.getClipWidth() + pathFontMaxCharWidth;
		int maxCharScreenY = minCharScreenY + pc.g.getClipHeight() + pathFontBaseLinePos * 2;
		
		StringBuffer sbName= new StringBuffer();
  	
		pc.g.setFont(pathFont);
		pc.g.setColor(0,0,0);

		IntPoint posChar = new IntPoint();
		char letter=' ';
		short charsDrawable=0;
		Projection p = pc.getP();

		//if(info)System.out.println("Draw "  + name + " from " + path.length + " points");
		
		boolean reversed=false;
		boolean abbreviated=false;
		int iNameRepeatable=0;
		int iNameRepeated=0;

    	// 2 passes:
    	// - 1st pass only counts fitting chars, so we can correctly
    	//   abbreviate reversed strings
    	// - 2nd pass actually draws
    	for (byte mode=PAINTMODE_COUNTFITTINGCHARS;mode<=PAINTMODE_DRAWCHARS; mode++) { 
    		double posChar_x = 0;
    		double posChar_y = 0;
    		double slope_x=0;
    		double slope_y=0;
    		double nextDeltaSub=0;
    		int delta=0;
    		IntPoint lineP1 = pc.lineP1;
    		IntPoint lineP2 = pc.lineP2;
    		IntPoint swapLineP = pc.swapLineP;
    		// do indent because first letter position would often
    		// be covered by other connecting  streets
			short streetNameCharIndex=-INDENT_PATHNAME;

			// draw name again and again until end of path
			for (int i1 = 0; i1 < path.length; i1++) {
				// get the next line point coordinates into lineP2
				int idx = this.path[i1];
				// forward() is in Mercator.java
				p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
				// if we got only one line point, get a second one 
				if (lineP1 == null) {
					lineP1 = lineP2;
					lineP2 = swapLineP;
					continue;
				}
				// calculate the slope of the new line 
				double distance = Math.sqrt( ((double)lineP2.y-(double)lineP1.y)*((double)lineP2.y-(double)lineP1.y) +
						((double)lineP2.x-(double)lineP1.x)*((double)lineP2.x-(double)lineP1.x) );
				if (distance!=0) {
					slope_x = ((double)lineP2.x-(double)lineP1.x)/distance;
					slope_y = ((double)lineP2.y-(double)lineP1.y)/distance;
				} else {
					//logger.debug("ZERO distance in path segment " + i1 + "/" + path.length + " of " + name);
					break;
				}
				// new char position is first line point position
				// minus the delta we've drawn over the previous
				// line point
				posChar_x = lineP1.x - nextDeltaSub * slope_x;
				posChar_y = lineP1.y - nextDeltaSub * slope_y;
				
				// as long as we have not passed the next line point
				while( 	(
							(slope_x<=0 && posChar_x >= lineP2.x) ||
							(slope_x>=0 && posChar_x <= lineP2.x)
						) && (
							(slope_y<=0 && posChar_y >= lineP2.y) ||
							(slope_y>=0 && posChar_y <= lineP2.y)
						)
				) {
					

					// get the street name into the buffer
					if (streetNameCharIndex==-INDENT_PATHNAME) {
						// use full name to count fitting chars
						sbName.setLength(0);
						sbName.append(name);
						abbreviated=false;
						reversed=false;
						if(mode==PAINTMODE_DRAWCHARS) {
							if (
								iNameRepeated>=iNameRepeatable &&
								charsDrawable>0 &&
								charsDrawable<name.length()
							) {
								//if(info)System.out.println(sbName.toString() + " i1: " + i1 + " lastFitI1 " + lastFittingI1 + " charsdrawable: " + charsDrawable );
								sbName.setLength(charsDrawable-1);
								abbreviated=true;
								if (sbName.length()==0) {
									sbName.append(".");
								}
							}
							// always begin drawing street names
							// left to right
							if (lineP1.x > lineP2.x) {
								sbName.reverse();
								reversed=true;
							}
						}
					}	
					// draw letter
					if (streetNameCharIndex >=0) {
						// char to draw
						letter=sbName.charAt(streetNameCharIndex);
						
						if (mode==PAINTMODE_DRAWCHARS) {
							// draw char only if it's at least partly on-screen
							if ( (int)posChar_x >= minCharScreenX &&
								 (int)posChar_x <= maxCharScreenX &&
								 (int)posChar_y >= minCharScreenX &&
								 (int)posChar_y <= maxCharScreenY									
							) {
								if (abbreviated) {
									pc.g.setColor(100,100,100);
								} else {
									pc.g.setColor(0,0,0);
								}
								pc.g.drawChar(
									letter,
									(int)posChar_x, (int)(posChar_y+(w/2)),
									Graphics.BASELINE | Graphics.HCENTER
								);
							}
						}
//						if (mode==PAINTMODE_COUNTFITTINGCHARS ) {
//							pc.g.setColor(150,150,150);
//							pc.g.drawChar(letter,
//							(int)posChar_x, (int)(posChar_y+(w/2)),
//							Graphics.BASELINE | Graphics.HCENTER);
//						}

						// delta calculation should be improved
						if (Math.abs(slope_x) > Math.abs(slope_y)) {
							delta=(pathFont.charWidth(letter) + pathFontHeight ) /2;							
						} else {
							delta=pathFontHeight*3/4;							
						}
					} else {
						// delta for indent 
						delta=pathFontHeight;
					}

					streetNameCharIndex++;
					if(mode==PAINTMODE_COUNTFITTINGCHARS) {
						charsDrawable=streetNameCharIndex;
					}
					// if at end of name
					if (streetNameCharIndex>=sbName.length()) {
						streetNameCharIndex=-INDENT_PATHNAME;
						if(mode==PAINTMODE_COUNTFITTINGCHARS) {
							// increase number of times the name fitted completely
							iNameRepeatable++;
						} else {
							iNameRepeated++;							
						}
					}
					
					// add slope to char position
					posChar_x += slope_x * delta;
					posChar_y += slope_y * delta;
					
					// how much would we start to draw the next char over the end point
					if (slope_x != 0) {
						nextDeltaSub=(lineP2.x-posChar_x) / slope_x;
					} else {
						nextDeltaSub=0;
					}
					
				} // end while loop
							
				
				// continue in next path segment
				swapLineP = lineP1;
				lineP1 = lineP2;
				lineP2 = swapLineP;	
			} // end segment for-loop

		} // end mode for-loop
		
		pc.g.setFont(originalFont);
    }

    public void paintPathOnewayArrows(PaintContext pc, SingleTile t) {
    	// exit if not zoomed in enough
    	WayDescription wayDesc = C.getWayDescription(type);
		if (pc.scale > wayDesc.maxOnewayArrowScale /* * pc.config.getDetailBoostMultiplier() */ ) {			
			return;
		}	

		if ( !Configuration.getCfgBitState(Configuration.CFGBIT_ONEWAY_ARROWS)) {
			return;
		}
		
		// calculate on-screen-width of the way
		double w = (int)(pc.ppm*wayDesc.wayWidth + 1);
		 
		// if arrow would get too small do not draw
		if(w<3) {
			return;
		}
		// if arrow would be very small make it a bit larger
		if(w<5) {
			w=5;
		}
		// limit maximum arrow width
		if (w > 10) {
			w=10;
		}
		// calculate arrow length
		int lenTriangle = (int) ((w * 5) / 4);
		int lenLine = (int) ((w * 4) / 3);

		int completeLen = lenTriangle + lenLine;
		int sumTooSmallLen = 0;
			
		// determine region in which arrows can be drawn
		int minArrowScreenX = pc.g.getClipX() - completeLen;
		int minArrowScreenY = pc.g.getClipY() - completeLen;
		int maxArrowScreenX = minArrowScreenX + pc.g.getClipWidth() + completeLen;
		int maxArrowScreenY = minArrowScreenY + pc.g.getClipHeight() + completeLen;
				
		Projection p = pc.getP();
		
		double posArrow_x = 0;
    	double posArrow_y = 0;    	
    	double slope_x=0;
    	double slope_y=0;
    	
//    	int delta=0;
//    	double nextDeltaSub=0;
    	
    	IntPoint lineP1 = pc.lineP1;
    	IntPoint lineP2 = pc.lineP2;
    	IntPoint swapLineP = pc.swapLineP;
    	
		// draw arrow in each segment of path
		for (int i1 = 0; i1 < path.length; i1++) {
			// get the next line point coordinates into lineP2
			int idx = this.path[i1];
			// forward() is in Mercator.java
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
			// if we got only one line point, get a second one 
			if (lineP1 == null) {
				lineP1 = lineP2;
				lineP2 = swapLineP;
				continue;
			}
			// calculate the slope of the new line 
			double distance = Math.sqrt( ((double)lineP2.y-(double)lineP1.y)*((double)lineP2.y-(double)lineP1.y) +
					((double)lineP2.x-(double)lineP1.x)*((double)lineP2.x-(double)lineP1.x) );

			if (distance > completeLen || sumTooSmallLen > completeLen) {
				if (sumTooSmallLen > completeLen) {
					sumTooSmallLen = 0;
					// special color for not completely fitting arrows
					pc.g.setColor(80,80,80);
				} else {
					// normal color
					pc.g.setColor(50,50,50);
				}
				if (distance!=0) {
					slope_x = ((double)lineP2.x-(double)lineP1.x)/distance;
					slope_y = ((double)lineP2.y-(double)lineP1.y)/distance;
				} else {
					//logger.debug("ZERO distance in path segment " + i1 + "/" + path.length + " of " + name);
					break;
				}
				// new arrow position is middle of way segment
				posArrow_x = lineP1.x + slope_x * (distance-completeLen)/2;
				posArrow_y = lineP1.y + slope_y * (distance-completeLen)/2;				
				
				// draw arrow only if it's at least partly on-screen
				if ( (int)posArrow_x >= minArrowScreenX &&
					 (int)posArrow_x <= maxArrowScreenX &&
					 (int)posArrow_y >= minArrowScreenX &&
					 (int)posArrow_y <= maxArrowScreenY									
				) {
					drawArrow(pc,
							  posArrow_x, posArrow_y,
							  slope_x, slope_y,
							  w, lenLine, lenTriangle 
					);					
				}
				
//				// delta calculation should be improved
//				delta = completeLen * 3;							
//				// add slope to arrow position
//				posArrow_x += slope_x * delta;
//				posArrow_y += slope_y * delta;
//				if (slope_x==0 && slope_y==0) {
//					break;
//				}
//
//				// how much would we start to draw the next arrow over the end point
//				if (slope_x != 0) {
//					nextDeltaSub=(lineP2.x-posArrow_x) / slope_x;
//				}
			} else {
				sumTooSmallLen += distance;
			}		
				
			// continue in next path segment
			swapLineP = lineP1;
			lineP1 = lineP2;
			lineP2 = swapLineP;	
		} // end segment for-loop		
    }
    
    private void drawArrow( PaintContext pc,
    						double x, double y,
    						double slopeX, double slopeY,
    						double w, int lenLine, int lenTriangle)
    {
    	double x2 = x + slopeX * (double) lenLine;
    	double y2 = y + slopeY * (double) lenLine;
    	
    	pc.g.drawLine((int) x, (int) y, (int) x2, (int) y2);
    	pc.g.fillTriangle(
			(int)(x2 + slopeY * w/2), (int)(y2 - slopeX * w/2),
			(int)(x2 - slopeY * w/2), (int)(y2 + slopeX * w/2),
			(int)(x2 + slopeX * lenTriangle), (int)(y2 + slopeY * lenTriangle)
    	);
    }
    
	private float getParLines(int xPoints[], int yPoints[], int i, int w,
			IntPoint p1, IntPoint p2, IntPoint p3, IntPoint p4) {
		int i1 = i + 1;
		int dx = xPoints[i1] - xPoints[i];
		int dy = yPoints[i1] - yPoints[i];
		int l2 = dx * dx + dy * dy;
		float l2f = (float) Math.sqrt(l2);
		float lf = w / l2f;
		int xb = (int) ((Math.abs(lf * dy)) + 0.5f);
		int yb = (int) ((Math.abs(lf * dx)) + 0.5f);
		int rfx = 1;
		int rfy = 1;
		if (dy < 0) {
			rfx = -1;
		}
		if (dx > 0) {
			rfy = -1;
		}
		int xd = rfx * xb;
		int yd = rfy * yb;
		p1.x = xPoints[i] + xd;	
		p1.y = yPoints[i] + yd;
		p2.x = xPoints[i] - xd;
		p2.y = yPoints[i] - yd;
		p3.x = xPoints[i1] + xd;
		p3.y = yPoints[i1] + yd;
		p4.x = xPoints[i1] - xd;
		p4.y = yPoints[i1] - yd;
		if (dx != 0) {
			return (MoreMath.atan(1f * dy / dx));
		} else {
			if (dy > 0) {
				return MoreMath.PiDiv2;
			} else {
				return -MoreMath.PiDiv2;
			}
		}
	}
	
	

	private void draw(PaintContext pc, int w, int xPoints[], int yPoints[],int count,boolean highlite/*,byte mode*/) {
		
		float roh1;
		float roh2;

		int max = count ;
		int beforeMax = max - 1;
		if (w <1) w=1;
		roh1 = getParLines(xPoints, yPoints, 0, w, l1b, l2b, l1e, l2e);
		for (int i = 0; i < max; i++) {
			if (i < beforeMax) {
				roh2 = getParLines(xPoints, yPoints, i + 1, w, l3b, l4b, l3e,
						l4e);
				if (!MoreMath.approximately_equal(roh1, roh2, 0.5f)) {
					intersectionPoint(l1b, l1e, l3b, l3e, s1);
					intersectionPoint(l2b, l2e, l4b, l4e, s2);
					l1e.set(s1);
					l2e.set(s2);
					l3b.set(s1);
					l4b.set(s2);
				}
			}
//			if (mode == DRAW_AREA){
				setColor(pc);
				pc.g.fillTriangle(l2b.x, l2b.y, l1b.x, l1b.y, l1e.x, l1e.y);
				pc.g.fillTriangle(l1e.x, l1e.y, l2e.x, l2e.y, l2b.x, l2b.y);
//			}
//			if (mode == DRAW_BORDER){
				if (highlite){
					pc.g.setColor(255,50,50);
				} else {
					setBorderColor(pc);
				}
				pc.g.drawLine(l1b.x, l1b.y, l1e.x, l1e.y);
				pc.g.drawLine(l2b.x, l2b.y, l2e.x, l2e.y);
//			}
			l1b.set(l3b);
			l2b.set(l4b);
			l1e.set(l3e);
			l2e.set(l4e);
		}

	}

	private IntPoint intersectionPoint(IntPoint p1, IntPoint p2, IntPoint p3,
			IntPoint p4, IntPoint ret) {
		int dx1 = p2.x - p1.x;
		int dx2 = p4.x - p3.x;
		int dx3 = p1.x - p3.x;
		int dy1 = p2.y - p1.y;
		int dy2 = p1.y - p3.y;
		int dy3 = p4.y - p3.y;
		float r = dx1 * dy3 - dy1 * dx2;
		if (r != 0f) {
			r = (1f * (dy2 * (p4.x - p3.x) - dx3 * dy3)) / r;
			ret.x = (int) ((p1.x + r * dx1) + 0.5);
			ret.y = (int) ((p1.y + r * dy1) + 0.5);
		} else {
			if (((p2.x - p1.x) * (p3.y - p1.y) - (p3.x - p1.x) * (p2.y - p1.y)) == 0) {
				ret.x = p3.x;
				ret.y = p3.y;
			} else {
				ret.x = p4.x;
				ret.y = p4.y;
			}
		}
		return ret;
	}

/*	private boolean getLineLineIntersection(IntPoint p1, IntPoint p2,
			IntPoint p3, IntPoint p4, IntPoint ret) {

		float x1 = p1.x;
		float y1 = p1.y;
		float x2 = p2.x;
		float y2 = p2.y;
		float x3 = p3.x;
		float y3 = p3.y;
		float x4 = p4.x;
		float y4 = p4.y;

		ret.x = (int) ((det(det(x1, y1, x2, y2), x1 - x2, det(x3, y3, x4, y4),
				x3 - x4) / det(x1 - x2, y1 - y2, x3 - x4, y3 - y4)) + 0.5);
		ret.y = (int) ((det(det(x1, y1, x2, y2), y1 - y2, det(x3, y3, x4, y4),
				y3 - y4) / det(x1 - x2, y1 - y2, x3 - x4, y3 - y4)) + 0.5);

		return true;
	}
*/
/*	private static float det(float a, float b, float c, float d) {
		return a * d - b * c;
	} */

	public void paintAsArea(PaintContext pc, SingleTile t) {
		WayDescription wayDesc = C.getWayDescription(type);
		
		byte om = C.getWayOverviewMode(type);
		switch (om & C.OM_MODE_MASK) {
			case C.OM_SHOWNORMAL: 
				// if not in Overview Mode check for scale
				if (pc.scale > wayDesc.maxScale * Configuration.getDetailBoostMultiplier()) {			
					return;
				}
				if (wayDesc.hideable && !Configuration.getCfgBitState(Configuration.CFGBIT_AREAS)) {
					return;
				}
				break;
			case C.OM_HIDE: 
				if (wayDesc.hideable) {
					return;
				}
				break;
		}
		switch (om & C.OM_NAME_MASK) {
			case C.OM_WITH_NAMEPART: 
				if (nameIdx == -1) return;
				String name = pc.trace.getName(nameIdx);
				if (name == null) return;
				if (name.toUpperCase().indexOf(C.get0Poi1Area2WayNamePart((byte) 1).toUpperCase()) == -1) return;
				break;
			case C.OM_WITH_NAME: 
				if (nameIdx == -1) return;
				break;
			case C.OM_NO_NAME: 
				if (nameIdx != -1) return;
				break;
		}
		
		IntPoint lineP2 = pc.lineP2;
		Projection p = pc.getP();
		/**
		 * we should probably use the static x and y variables
		 * but that would require to rewrite the fillPolygon
		 * function
		 */
		int[] x = new int[path.length];
		int[] y = new int[path.length];
		
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];			
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
			x[i1] = lineP2.x;
			y[i1] = lineP2.y;
		}
		/*if ((x[0] != x[path.length - 1]) || (y[0] != y[path.length - 1])){
			System.out.println("WARNING: start and end coordinates of area don't match " + this);			
			return;
		}*/
		//PolygonGraphics.drawPolygon(g, x, y);
		//DrawUtil.fillPolygon(x, y, wayDesc.lineColor, pc.g);
		PolygonGraphics.fillPolygon(pc.g, x, y);
		paintAreaName(pc,t);
	}

	public void paintAreaName(PaintContext pc, SingleTile t) {
		WayDescription wayDesc = C.getWayDescription(type);
		if (pc.scale > wayDesc.maxTextScale * Configuration.getDetailBoostMultiplier() ) {			
			return;
		}		
		
		if (wayDesc.hideable && !Configuration.getCfgBitState(Configuration.CFGBIT_AREATEXTS)) {
			return;
		}

		String name=null;
		if ( Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE)) {
			name=wayDesc.description;
		} else {			
			if (nameIdx != -1) {
				name=Trace.getInstance().getName(nameIdx);
			}
		}
		// if zoomed in enough, show description 
		if (pc.scale < wayDesc.maxDescriptionScale) {
		// show waydescription
			if (name==null) {
				name=wayDesc.description;
			} else {
				name=name + " (" + wayDesc.description + ")";
			}
		}
		if (name == null)
			return;
		IntPoint lineP2 = pc.lineP2;
		Projection p = pc.getP();
		int x;
		int y;

		// get screen clip
		int clipX=pc.g.getClipX();
		int clipY=pc.g.getClipY();
		int clipMaxX=clipX+pc.g.getClipWidth();
		int clipMaxY=clipY+pc.g.getClipHeight();;

		// find center of area
		int minX=clipMaxX;
		int maxX=clipX;
		int minY=clipMaxY;
		int maxY=clipY;
		for (int i1 = 0; i1 < path.length; i1++) {
			int idx = path[i1];			
			p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, t);
			x = lineP2.x;
			y = lineP2.y;
			if (minX>x) minX=x;
			if (minY>y) minY=y;
			if (maxX<x) maxX=x;
			if (maxY<y) maxY=y;
		}
	
		// System.out.println("name:" + name + " ClipX:" + clipX + " ClipMaxX:" + clipMaxX + " ClipY:" + clipY + " ClipMaxY:" + clipMaxY + " minx:" + minX + " maxX:"+maxX + " miny:"+minY+ " maxY" + maxY);

		Font originalFont = pc.g.getFont();
		if (areaFont==null) {
			areaFont=Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
			areaFontHeight=areaFont.getHeight();
		}
		// find out how many chars of the name fit into the area
		int i=name.length()+1;
		int w;
		do {
			i--;
			w=areaFont.substringWidth(name,0,i);
		} while (w>(maxX-minX) && i>1);
		// is area wide enough to draw at least a dot into it?
		if ((maxX-minX)>=3 ) {
			pc.g.setColor(0,0,0);
			// if at least two chars have fit or name is a fitting single char, draw name
			if (i>1 || (i==name.length() && w<=(maxX-minX))  ) {
				pc.g.setFont(areaFont);
				// center vertically in area
				int y1=(minY+maxY-areaFontHeight)/2;
				// draw centered into area
				pc.g.drawSubstring(name, 0, i, (minX+maxX-w)/2, y1, Graphics.TOP | Graphics.LEFT);
				// if name fits not completely, append "..."
				if (i!=name.length()) {
					pc.g.drawString("...", (minX+maxX+w)/2, y1, Graphics.TOP | Graphics.LEFT);
				}
				pc.g.setFont(originalFont);
				// else draw a dot to indicate there's a name for this area available
			} else {
				pc.g.drawRect((minX+maxX)/2, (minY+maxY)/2, 0, 0 );
			}
		}		
	}
	
	
	public void setColor(PaintContext pc) {		
		WayDescription wayDesc = C.getWayDescription(type);
		pc.g.setStrokeStyle(wayDesc.lineStyle);
		if ((pc.target != null) && (pc.target.e == this)) {
			/*Color the target way red */			
			pc.g.setColor(0x00ff0000);
		} else {
			pc.g.setColor(wayDesc.lineColor);
		}
	}

	public int getWidth(PaintContext pc) {
		WayDescription wayDesc = C.getWayDescription(type);
		return wayDesc.wayWidth;
	}

	public void setBorderColor(PaintContext pc) {
		pc.g.setStrokeStyle(Graphics.SOLID);
		WayDescription wayDesc = C.getWayDescription(type);
		pc.g.setColor(wayDesc.boardedColor);
	}
	
	public boolean isOneway(){
		return ((flags & WAY_ONEWAY) == WAY_ONEWAY);
	}
	
	public boolean isArea() {
		return ((flags & WAY_AREA) > 0);
	}
	
	public byte getMaxSpeed() {
		return (byte)((flags & MaxSpeedMask) >> MaxSpeedShift);
	}
	
/*	private float[] getFloatNodes(SingleTile t, short[] nodes, float offset) {
	    float [] res = new float[nodes.length];
	    for (int i = 0; i < nodes.length; i++) {
		res[i] = nodes[i]*SingleTile.fpminv + offset;
	    }
	    return res;
	}
*/
	
	public float[] getNodesLatLon(SingleTile t, boolean latlon) {
		float offset; 
		short [] nodes;
		int len = path.length;
		float [] lat = new float[len];
		
		if (latlon) { 
			offset = t.centerLat;
			nodes = t.nodeLat;
		} else {
			offset = t.centerLon;
			nodes = t.nodeLon;
		}
		for (int i = 0; i < len; i++) {
			lat[i] = nodes[path[i]]*SingleTile.fpminv + offset;
		}
		return lat;
	}
	
	public String toString() {
		return "Way " + Trace.getInstance().getName(nameIdx) + " type: " +  C.getWayDescription(type).description;
	}
}
