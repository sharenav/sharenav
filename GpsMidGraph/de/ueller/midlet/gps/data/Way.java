package de.ueller.midlet.gps.data;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Graphics;

import net.sourceforge.jmicropolygon.PolygonGraphics;
import de.ueller.gpsMid.mapData.SingleTile;
import de.ueller.gpsMid.mapData.Tile;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;

public class Way extends Entity{
	
	public static final byte WAY_FLAG_NAME = 1;
	public static final byte WAY_FLAG_MAXSPEED = 2;
	public static final byte WAY_FLAG_ONEWAY = 16;
//	public static final byte WAY_FLAG_MULTIPATH = 4;
	public static final byte WAY_FLAG_LONGWAY = 8;
	public static final byte WAY_FLAG_NAMEHIGH = 32;
//	public static final byte WAY_FLAG_ISINHIGH = 64;
	
	public static final byte DRAW_BORDER=1;
	public static final byte DRAW_AREA=2;
	public static final byte DRAW_FULL=3;
	
	public static final byte WAY_ONEWAY=1;
	
	public byte maxspeed;
	//This is not currently used, so save the 4 bytes of memory per way
	//public int isInIdx=-1;
	public byte mod=0;
//	public short[][] paths;

	public short[] path;
	public short minLat;
	public short minLon;
	public short maxLat;
	public short maxLon;

//	private final static Logger logger = Logger.getInstance(Way.class,
//			Logger.TRACE);

	/**
	 * the flag should be readed by caller. if Flag == 128 this is a dummy Way
	 * and can ignored.
	 * 
	 * @param is
	 * @param f
	 * @param nodes
	 * @throws IOException
	 */
	public Way(DataInputStream is, byte f, Tile t) throws IOException {
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
			maxspeed = is.readByte();
		}
		if ((f & WAY_FLAG_ONEWAY) == WAY_FLAG_ONEWAY) {
			mod += WAY_ONEWAY;
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

	public boolean isOnScreen(PaintContext pc, float refLat, float refLon) { 
		if ((((float)(maxLat*SingleTile.fpminv) + refLat) < pc.screenLD.radlat) || (((float)(minLat*SingleTile.fpminv) + refLat) > pc.screenRU.radlat)) { 
			return false; 
		} 
		if ((((float)(maxLon*SingleTile.fpminv) + refLon) < pc.screenLD.radlon) || (((float)(minLon*SingleTile.fpminv) + refLon) > pc.screenRU.radlon)) { 
			return false; 
		}        
		return true; 
	} 


	public void paintAsPath(PaintContext pc, SingleTile t) {

		IntPoint lineP1 = pc.lineP1;
		IntPoint lineP2 = pc.lineP2;
		IntPoint swapLineP = pc.swapLineP;
		Projection p = pc.getP();
			for (int i1 = 0; i1 < path.length; i1++) {
				int idx = path[i1];
				p.forward((float)(t.nodeLat[idx]*SingleTile.fpminv + t.centerLat), (float)(t.nodeLon[idx] *SingleTile.fpminv + t.centerLon), lineP2, true);				
				if (lineP1 == null) {
					lineP1 = lineP2;
					lineP2 = swapLineP;
				} else {
					float dst = MoreMath.ptSegDistSq(lineP1.x, lineP1.y,
							lineP2.x, lineP2.y, pc.xSize / 2, pc.ySize / 2);
					if (dst < pc.squareDstToWay) {
//						System.out.println("set new current Way1 "+ pc.trace.getName(this.nameIdx) + "new dist "+ dst + " old " + pc.squareDstToWay);
						pc.squareDstToWay = dst;
						pc.actualWay = this;
						pc.actualNodeLat = t.nodeLat[idx]; 
						pc.actualNodeLon = t.nodeLon[idx];
						pc.currentPos=new PositionMark(pc.center.radlat,pc.center.radlon);
						pc.currentPos.setEntity(this, getFloatNodes(t,t.nodeLat,t.centerLat), getFloatNodes(t,t.nodeLon,t.centerLon));						
					}
					pc.g.drawLine(lineP1.x, lineP1.y, lineP2.x, lineP2.y);
					swapLineP = lineP1;
					lineP1 = lineP2;
					lineP2 = swapLineP;
				}
			}
			swapLineP = lineP1;
			lineP1 = null;
	}
	/**
	 * draw ways with 2 lines left and right a black border and
	 * between them filled with the color of the way.
	 * @param pc
	 * @param w
	 * @param t
	 */
	public void paintAsPath(PaintContext pc, int w, SingleTile t) {
		
		IntPoint lineP1 = pc.lineP1;
		IntPoint lineP2 = pc.lineP2;
		IntPoint swapLineP = pc.swapLineP;
		Projection p = pc.getP();


			int pi=0;
			int[] x = new int[path.length];
			int[] y = new int[path.length];
			int i1=0;
			for (; i1 < path.length; i1++) {
				int idx = path[i1];
				p.forward((float)(t.nodeLat[idx]*SingleTile.fpminv + t.centerLat), (float)(t.nodeLon[idx] *SingleTile.fpminv + t.centerLon), lineP2, true);
				if (lineP1 == null) {
//					System.out.println(" startpoint " + lineP2.x + "/" + lineP2.y + " " +pc.trace.getName(nameIdx));
					lineP1 = lineP2;
					lineP2 = swapLineP;
					x[pi] = lineP1.x;
					y[pi++] = lineP1.y;
				} else {
					if (! lineP1.approximatelyEquals(lineP2)){
//						System.out.println(" midpoint " + lineP2.x + "/" + lineP2.y+ " " +pc.trace.getName(nameIdx));
						float dst = MoreMath.ptSegDistSq(lineP1.x, lineP1.y,
								lineP2.x, lineP2.y, pc.xSize / 2, pc.ySize / 2);
						if (dst < pc.squareDstToWay) {
//							System.out.println("set new current Way "+ pc.trace.getName(this.nameIdx) + "new dist "+ dst + " old " + pc.squareDstToWay);
							pc.squareDstToWay = dst;
							pc.actualWay = this;
							pc.actualNodeLat = t.nodeLat[idx]; 
							pc.actualNodeLon = t.nodeLon[idx]; 
							pc.currentPos=new PositionMark(pc.center.radlat,pc.center.radlon);
							pc.currentPos.setEntity(this, getFloatNodes(t,t.nodeLat,t.centerLat), getFloatNodes(t,t.nodeLon,t.centerLon));

						}
						x[pi] = lineP2.x;
						y[pi++] = lineP2.y;
						swapLineP = lineP1;
						lineP1 = lineP2;
						lineP2 = swapLineP;
					} else if ((i1+1) == path.length){
//						System.out.println(" endpoint " + lineP2.x + "/" + lineP2.y+ " " +pc.trace.getName(nameIdx));
						if (! lineP1.equals(lineP2)){
							x[pi] = lineP2.x;
							y[pi++] = lineP2.y;
						} 
					}else { 
//						System.out.println(" discard " + lineP2.x + "/" + lineP2.y+ " " +pc.trace.getName(nameIdx));
					}
				}
			}
			swapLineP = lineP1;
			lineP1 = null;
			// int ppm = pc.p.getPPM();
			// System.out.println("PPM=" + ppm);
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

	}

	public float getParLines(int xPoints[], int yPoints[], int i, int w,
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

	public void draw(PaintContext pc, int w, int xPoints[], int yPoints[],int count,boolean highlite/*,byte mode*/) {
		IntPoint l1b = new IntPoint();
		IntPoint l1e = new IntPoint();
		IntPoint l2b = new IntPoint();
		IntPoint l2e = new IntPoint();
		IntPoint l3b = new IntPoint();
		IntPoint l3e = new IntPoint();
		IntPoint l4b = new IntPoint();
		IntPoint l4e = new IntPoint();
		IntPoint s1 = new IntPoint();
		IntPoint s2 = new IntPoint();
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

	public IntPoint intersectionPoint(IntPoint p1, IntPoint p2, IntPoint p3,
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

	private boolean getLineLineIntersection(IntPoint p1, IntPoint p2,
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

	static float det(float a, float b, float c, float d) {
		return a * d - b * c;
	}

	public void paintAsArea(PaintContext pc, SingleTile t) {
		IntPoint lineP2 = pc.lineP2;
		Projection p = pc.getP();
//		for (int p1 = 0; p1 < paths.length; p1++) {
//			short[] path = paths[p1];
			int[] x = new int[path.length];
			int[] y = new int[path.length];
			for (int i1 = 0; i1 < path.length; i1++) {
				int idx = path[i1];
				p.forward((float)(t.nodeLat[idx]*SingleTile.fpminv + t.centerLat), (float)(t.nodeLon[idx] *SingleTile.fpminv + t.centerLon), lineP2, true);
				x[i1] = lineP2.x;
				y[i1] = lineP2.y;
			}
			// PolygonGraphics.drawPolygon(g, x, y);
			PolygonGraphics.fillPolygon(pc.g, x, y);
//		}

	}

	public void setColor(PaintContext pc) {
		pc.g.setStrokeStyle(Graphics.SOLID);
		switch (type) {
		case C.WAY_HIGHWAY_MOTORWAY:
		case C.WAY_HIGHWAY_MOTORWAY_LINK:
			pc.g.setColor(128, 155, 192);
			break;
		case C.WAY_HIGHWAY_TRUNK:
			pc.g.setColor(228, 109, 113);
		case C.WAY_HIGHWAY_PRIMARY:
			pc.g.setColor(127, 201, 127);
			break;
		case C.WAY_HIGHWAY_SECONDARY:
			pc.g.setColor(253, 191, 111);
			break;
		case C.WAY_HIGHWAY_MINOR:
		case C.WAY_HIGHWAY_UNCLASSIFIED:
			pc.g.setColor(255, 255, 255);
			break;
		case C.WAY_HIGHWAY_RESIDENTIAL:
			pc.g.setColor(180, 180, 180);
			break;
		case C.WAY_HIGHWAY_TRACK:
			pc.g.setColor(180, 180, 180);
			break;
		// case C.WAY_RAILROAD:
		case C.AREA_AMENITY_PARKING:
			pc.g.setColor(255, 255, 150);
			break;
		case C.AREA_NATURAL_WATER:
		case C.WAY_WATERWAY_RIVER:
			pc.g.setColor(50, 50, 255);
			break;
		case C.AREA_LANDUSE_FARM:
			pc.g.setColor(136, 107, 29);
			break;
		case C.AREA_LANDUSE_QUARRY:
			pc.g.setColor(205, 199, 182);
			break;
		case C.AREA_LANDUSE_LANDFILL:
			pc.g.setColor(75, 75, 75);
			break;
		case C.AREA_LANDUSE_BASIN:
			pc.g.setColor(10, 10, 205);
			break;
		case C.AREA_LANDUSE_RESERVOIR:
			pc.g.setColor(30, 30, 235);
			break;
		case C.AREA_LANDUSE_FOREST:
			pc.g.setColor(5, 82, 4);
			break;
		case C.AREA_LANDUSE_ALLOTMENTS:
			pc.g.setColor(25, 102, 24);
			break;
		case C.AREA_LANDUSE_RESIDENTIAL:
			pc.g.setColor(210, 210, 210);
			break;
		case C.AREA_LANDUSE_RETAIL:
			pc.g.setColor(57, 227, 231);
			break;
		case C.AREA_LANDUSE_COMMERCIAL:
			pc.g.setColor(129, 229, 231);
			break;
		case C.AREA_LANDUSE_INDUSTRIAL:
			pc.g.setColor(225, 223, 33);
			break;
		case C.AREA_LANDUSE_BROWNFIELD:
			pc.g.setColor(75, 75, 11);
			break;
		case C.AREA_LANDUSE_GREENFIELD:
			pc.g.setColor(167, 167, 132);
			break;
		case C.AREA_LANDUSE_CEMETERY:
			pc.g.setColor(20, 20, 20);
			break;
		case C.AREA_LANDUSE_VILLAGE_GREEN:
		case C.AREA_LANDUSE_RECREATION_GROUND:
		case C.AREA_LEISURE_PARK:
			pc.g.setColor(90, 186, 57);
			break;
		case C.WAY_RAILWAY_RAIL:
		case C.WAY_RAILWAY_SUBWAY:
		case C.WAY_RAILWAY_UNCLASSIFIED:
			pc.g.setStrokeStyle(Graphics.DOTTED);
			pc.g.setColor(0, 0, 0);
		default:
			// logger.error("unknown Type "+ w.type);
			pc.g.setColor(255, 255, 255);
		}
	}

	public int getWidth() {
		switch (type) {
		case C.WAY_HIGHWAY_MOTORWAY:
		case C.WAY_HIGHWAY_MOTORWAY_LINK:
			return 8;
		case C.WAY_HIGHWAY_TRUNK:
			return 6;
		case C.WAY_HIGHWAY_PRIMARY:
			return 6;
		case C.WAY_HIGHWAY_SECONDARY:
			return 5;
		case C.WAY_HIGHWAY_MINOR:
		case C.WAY_HIGHWAY_UNCLASSIFIED:
			return 4;
		case C.WAY_HIGHWAY_RESIDENTIAL:
			return 3;
		case C.WAY_HIGHWAY_TRACK:
			return 2;
		case C.WAY_WATERWAY_RIVER:
			return 10;
		case C.WAY_RAILWAY_RAIL:
			return 1;
		case C.WAY_RAILWAY_SUBWAY:
		case C.WAY_RAILWAY_UNCLASSIFIED:
			return 1;

		default:
			// logger.error("unknown Type "+ w.type);
			return 1;
		}
	}

	public void setBorderColor(PaintContext pc) {
		pc.g.setStrokeStyle(Graphics.SOLID);
		switch (type) {
		case C.WAY_HIGHWAY_MOTORWAY:
			pc.g.setColor(128, 155, 192);
			break;
		case C.WAY_HIGHWAY_TRUNK:
			pc.g.setColor(228, 109, 113);
		case C.WAY_HIGHWAY_PRIMARY:
			pc.g.setColor(127, 201, 127);
			break;
		case C.WAY_HIGHWAY_SECONDARY:
			pc.g.setColor(253, 191, 111);
			break;
		case C.WAY_HIGHWAY_MINOR:
		case C.WAY_HIGHWAY_UNCLASSIFIED:
			pc.g.setColor(255, 255, 255);
			break;
		case C.WAY_HIGHWAY_RESIDENTIAL:
			pc.g.setColor(180, 180, 180);
			break;
		// case C.WAY_RAILROAD:
		case C.AREA_AMENITY_PARKING:
			pc.g.setColor(255, 255, 150);
			break;
		case C.AREA_NATURAL_WATER:
		case C.WAY_WATERWAY_RIVER:
			pc.g.setColor(50, 50, 255);
			break;
		case C.AREA_LANDUSE_FARM:
			pc.g.setColor(136, 107, 29);
			break;
		case C.AREA_LANDUSE_QUARRY:
			pc.g.setColor(205, 199, 182);
			break;
		case C.AREA_LANDUSE_LANDFILL:
			pc.g.setColor(75, 75, 75);
			break;
		case C.AREA_LANDUSE_BASIN:
			pc.g.setColor(10, 10, 205);
			break;
		case C.AREA_LANDUSE_RESERVOIR:
			pc.g.setColor(30, 30, 235);
			break;
		case C.AREA_LANDUSE_FOREST:
			pc.g.setColor(5, 82, 4);
			break;
		case C.AREA_LANDUSE_ALLOTMENTS:
			pc.g.setColor(25, 102, 24);
			break;
		case C.AREA_LANDUSE_RESIDENTIAL:
			pc.g.setColor(210, 210, 210);
			break;
		case C.AREA_LANDUSE_RETAIL:
			pc.g.setColor(57, 227, 231);
			break;
		case C.AREA_LANDUSE_COMMERCIAL:
			pc.g.setColor(129, 229, 231);
			break;
		case C.AREA_LANDUSE_INDUSTRIAL:
			pc.g.setColor(225, 223, 33);
			break;
		case C.AREA_LANDUSE_BROWNFIELD:
			pc.g.setColor(75, 75, 11);
			break;
		case C.AREA_LANDUSE_GREENFIELD:
			pc.g.setColor(167, 167, 132);
			break;
		case C.AREA_LANDUSE_CEMETERY:
			pc.g.setColor(20, 20, 20);
			break;
		case C.AREA_LANDUSE_VILLAGE_GREEN:
		case C.AREA_LANDUSE_RECREATION_GROUND:
		case C.AREA_LEISURE_PARK:
			pc.g.setColor(90, 186, 57);
			break;

		default:
			// logger.error("unknown Type "+ w.type);
			pc.g.setColor(0, 0, 0);
		}
		pc.g.setColor(0, 0, 0);
	}
	
	public boolean isOneway(){
		return ((mod & WAY_ONEWAY) == WAY_ONEWAY);
	}

	private float[] getFloatNodes(SingleTile t, short[] nodes, float offset) {
	    float [] res = new float[nodes.length];
	    for (int i = 0; i < nodes.length; i++) {
		res[i] = nodes[i]*t.fpminv + offset;
	    }
	    return res;
	}
}
