package de.ueller.midlet.gps.data;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;

import javax.microedition.lcdui.Graphics;

import net.sourceforge.jmicropolygon.PolygonGraphics;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;
import de.ueller.midlet.gps.tile.SingleTile;


public class Way {
	public byte type;
	public Short nameIdx=null;
	public int maxspeed;
	public short[][] paths;
//	public short[] path;
	public float minLat;
	public float minLon;
	public float maxLat;
	public float maxLon;
	private final static Logger logger=Logger.getInstance(Way.class,Logger.ERROR);

	/**
	 * the flag should be readed by caller. if Flag == 128 this is a dummy
	 * Way and can ignored.
	 * @param is
	 * @param f
	 * @param nodes
	 * @throws IOException
	 */
	public Way(DataInputStream	is,byte f) throws IOException{
		// temporary removed for test
		minLat=is.readFloat();
		minLon=is.readFloat();
		maxLat=is.readFloat();
		maxLon=is.readFloat();
		// end temporary removed for test
//		if (is.readByte() != 0x58){
//			logger.error("worng magic after way bounds");
//		}
		type=is.readByte();
		if ((f & 1) == 1){
//			logger.debug("read name");
//			name=is.readUTF();
			nameIdx=new Short(is.readShort());
		}
		if ((f & 2) == 2){
//			logger.debug("read maxspeed");
			maxspeed=is.readByte();
		}		
		byte pathCount;
		if ((f & 4) == 4){
			pathCount = is.readByte();
//			logger.debug("Multipath "+ pathCount);
		} else {
			pathCount=1;
		}
		paths=new short[pathCount][];
//		logger.debug("read paths count="+pathCount);
		for (byte pc=0;pc < pathCount; pc++){
			short count = is.readByte();
			short[] path = new short[count];
			paths[pc]=path;
			logger.debug("read path count="+count);
			for (short i=0; i< count;i++){
				path[i]=is.readShort();
//				logger.debug("read node id=" + path[i]);
			}
//			if (is.readByte() != 0x59 ){
//				logger.error("wrong magic code after path");
//			}
		}
	}
	
	public void paintAsPath(PaintContext pc, SingleTile t) {
		IntPoint lineP1 = pc.lineP1;
		IntPoint lineP2 = pc.lineP2;
		IntPoint swapLineP = pc.swapLineP;

		for (int p1 = 0; p1 < paths.length; p1++) {
//			read the name only if is used more memory efficicent
//			pc.trace.getName(nameIdx);
			short[] path = paths[p1];
			for (int i1 = 0; i1 < path.length; i1++) {
				int idx=path[i1];
				pc.p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, true);
				if (lineP1 == null) {
					lineP1 = lineP2;
					lineP2 = swapLineP;
				} else {
					float dst=MoreMath.ptSegDistSq(lineP1.x, lineP1.y, lineP2.x, lineP2.y,pc.xSize/2,pc.ySize/2);
					if (dst < pc.squareDstToWay){
						pc.squareDstToWay=dst;
						pc.actualWay=this;

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
	}

	public void paintAsArea(PaintContext pc, SingleTile t){
		IntPoint lineP2 = pc.lineP2;
		for (int p1 = 0; p1 < paths.length; p1++) {
			short[] path = paths[p1];
			int[] x=new int[path.length];
			int[] y=new int[path.length];
			for (int i1 = 0; i1 < path.length; i1++) {
				int idx=path[i1];
					pc.p.forward(t.nodeLat[idx], t.nodeLon[idx], lineP2, true);
					x[i1]=lineP2.x;
					y[i1]=lineP2.y;
			}
//			PolygonGraphics.drawPolygon(g, x, y);
			PolygonGraphics.fillPolygon(pc.g, x, y);
		}

	}
	
	public void setColor(PaintContext pc){
		pc.g.setStrokeStyle(Graphics.SOLID);
		switch (type) {
		case C.WAY_HIGHWAY_MOTORWAY:
			pc.g.setColor(128, 155, 192);
			break;
		case C.WAY_HIGHWAY_TRUNK:
			pc.g.setColor(228,109,113);
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
//			case C.WAY_RAILROAD:
		case C.AREA_AMENITY_PARKING:
			pc.g.setColor(255,255,150);
			break;
		case C.AREA_NATURAL_WATER:
		case C.WAY_WATERWAY_RIVER:
			pc.g.setColor(50,50,255);
			break;
		case C.AREA_LANDUSE_FARM:
			pc.g.setColor(136,107,29);
			break;
		case C.AREA_LANDUSE_QUARRY:
			pc.g.setColor(205,199,182);
			break;
		case C.AREA_LANDUSE_LANDFILL:
			pc.g.setColor(75,75,75);
			break;
		case C.AREA_LANDUSE_BASIN:
			pc.g.setColor(10,10,205);
			break;
		case C.AREA_LANDUSE_RESERVOIR:
			pc.g.setColor(30,30,235);
			break;
		case C.AREA_LANDUSE_FOREST:
			pc.g.setColor(5,82,4);
			break;
		case C.AREA_LANDUSE_ALLOTMENTS:
			pc.g.setColor(25,102,24);
			break;
		case C.AREA_LANDUSE_RESIDENTIAL:
			pc.g.setColor(210,210,210);
			break;
		case C.AREA_LANDUSE_RETAIL:
			pc.g.setColor(57,227,231);
			break;
		case C.AREA_LANDUSE_COMMERCIAL:
			pc.g.setColor(129,229,231);
			break;
		case C.AREA_LANDUSE_INDUSTRIAL:
			pc.g.setColor(225,223,33);
			break;
		case C.AREA_LANDUSE_BROWNFIELD:
			pc.g.setColor(75,75,11);
			break;
		case C.AREA_LANDUSE_GREENFIELD:
			pc.g.setColor(167,167,132);
			break;
		case C.AREA_LANDUSE_CEMETERY:
			pc.g.setColor(20,20,20);
			break;
		case C.AREA_LANDUSE_VILLAGE_GREEN:
		case C.AREA_LANDUSE_RECREATION_GROUND:
		case C.AREA_LEISURE_PARK:
			pc.g.setColor(90,186,57);
			break;
		case C.WAY_RAILWAY_RAIL:
		case C.WAY_RAILWAY_SUBWAY:
		case C.WAY_RAILWAY_UNCLASSIFIED:
			pc.g.setStrokeStyle(Graphics.DOTTED);
		default:
//			logger.error("unknown Type "+ w.type);
			pc.g.setColor(0, 0, 0);
	}

	}
}
