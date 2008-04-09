package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Image;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.names.Names;

public class C {
	/**
	 * Specifies the format of the map on disk we expect to see
	 * This constant must be in sync with Osm2GpsMid
	 */
	public final static short MAP_FORMAT_VERSION = 6;
	
	public final static byte NODE_MASK_ROUTENODELINK=0x1;
	public final static byte NODE_MASK_TYPE=0x2;
	public final static byte NODE_MASK_NAME=0x4;
	public final static byte NODE_MASK_ROUTENODE=0x8;
	public final static byte NODE_MASK_NAMEHIGH=0x10;
	
	/**
	 * minimum distances to set the is_in name to the next city
	 * to get the minimum distance use: <code>MAX_DIST_CITY[node.getType(null)]</code>
	 */
		
	public final static byte WAY_HIGHWAY_MOTORWAY=1;
	public final static byte WAY_HIGHWAY_MOTORWAY_LINK=2;
	public final static byte WAY_HIGHWAY_TRUNK=3;
	public final static byte WAY_HIGHWAY_PRIMARY=4;
	public final static byte WAY_HIGHWAY_SECONDARY=5;
	public final static byte WAY_HIGHWAY_MINOR=6;
	public final static byte WAY_HIGHWAY_RESIDENTIAL=7;
	public final static byte WAY_JUNCTION_ROUNDABOUT = 8;
	public final static byte WAY_HIGHWAY_TRACK=9;
	public final static byte WAY_HIGHWAY_UNCLASSIFIED=10;
	// ways after unclassified are not accessible by car
	public final static byte WAY_HIGHWAY_CYCLEWAY=11;
	public final static byte WAY_HIGHWAY_FOOTWAY=12;
	public final static byte WAY_RAILWAY_UNCLASSIFIED =13;
	public final static byte WAY_RAILWAY_RAIL = 14;
	public final static byte WAY_RAILWAY_SUBWAY = 15;
	public final static byte WAY_WATERWAY_RIVER=16;
	public final static byte WAY_HIGHWAY_STEPS=17;
	
	public final static byte AREA_AMENITY_UNCLASSIFIED=50;
	public final static byte AREA_AMENITY_PARKING=51;
	public final static byte AREA_AMENITY_PUBLIC_BUILDING=52;
	public final static byte AREA_NATURAL_WATER=60;
	public final static byte AREA_LEISURE_PARK=70;
	
	public final static byte AREA_LANDUSE_FARM=80;
	public final static byte AREA_LANDUSE_QUARRY=81;
	public final static byte AREA_LANDUSE_LANDFILL=82;
	public final static byte AREA_LANDUSE_BASIN=83;
	public final static byte AREA_LANDUSE_RESERVOIR=84;
	public final static byte AREA_LANDUSE_FOREST=85;
	public final static byte AREA_LANDUSE_ALLOTMENTS=86;
	public final static byte AREA_LANDUSE_RESIDENTIAL=87;
	public final static byte AREA_LANDUSE_RETAIL=88;
	public final static byte AREA_LANDUSE_COMMERCIAL=89;
	public final static byte AREA_LANDUSE_INDUSTRIAL=90;
	public final static byte AREA_LANDUSE_BROWNFIELD=91;
	public final static byte AREA_LANDUSE_GREENFIELD=92;
	public final static byte AREA_LANDUSE_CEMETERY=93;
	public final static byte AREA_LANDUSE_VILLAGE_GREEN=94;
	public final static byte AREA_LANDUSE_RECREATION_GROUND=95;
	
	public final static byte NAME_CITY=1;
	public final static byte NAME_SUBURB=2;
	public final static byte NAME_STREET=3;
	public final static byte NAME_AMENITY=4;
	
	private POIdescription[] pois;
	private final static Logger logger=Logger.getInstance(C.class,Logger.TRACE);
	
	public C() throws IOException {
		InputStream is = GpsMid.getInstance().getConfig().getMapResource("/legend.dat");
		
		if (is == null) {
			logger.error("Failed to open the legend file");
			return;			
		}

		DataInputStream ds = new DataInputStream(is);
		
		Image generic = Image.createImage("/unknown.png");
		
		/**
		 * Check to see if we have the right version of the Map format
		 */
		short mapVersion = ds.readShort();
		if (mapVersion != MAP_FORMAT_VERSION) {
			logger.fatal("The Map files are not the version we expected, " +
					"please ues the correct Osm2GpsMid to recreate the map " +
					"data.  Expected: " + MAP_FORMAT_VERSION + " Read: " + mapVersion);
			throw new IOException("Wrong map file format");
		}
		
		pois = new POIdescription[ds.readByte()];
		for (int i = 0; i < pois.length; i++) {
			pois[i] = new POIdescription();
			if (ds.readByte() != i)
				logger.error("Read legend had troubles");
			byte flags = ds.readByte();
			pois[i].description = ds.readUTF();
			//System.out.println("POI: " +  pois[i].description);
			pois[i].imageCenteredOnNode = ds.readBoolean();
			pois[i].maxImageScale = ds.readInt();
			if ((flags & 0x01) > 0) {
				String imageName = ds.readUTF();
				//System.out.println("trying to open image " + imageName);
				try {
					pois[i].image = Image.createImage(imageName);
				} catch (IOException e) {
					logger.info("could not open POI image " + imageName + " for " + pois[i].description);
					pois[i].image = generic;
				}				
			}
			if ((flags & 0x02) > 0)
				pois[i].maxTextScale = ds.readInt();
			else
				pois[i].maxTextScale = pois[i].maxImageScale; 
			if ((flags & 0x04) > 0)			
				pois[i].textColor = ds.readInt();
		}
		ds.close();				
	}

	public int getNodeTextColor(byte type) {
		return pois[typeTotype(type)].textColor;
	}
	
	public Image getNodeImage(byte type)  {
		return pois[typeTotype(type)].image;
	}
	
	public int getNodeMaxScale(byte type) {
		return pois[typeTotype(type)].maxImageScale;
	}
	public int getNodeMaxTextScale(byte type) {
		return pois[typeTotype(type)].maxTextScale;
	}
	
	public boolean isNodeImageCentered(byte type) {
		return pois[typeTotype(type)].imageCenteredOnNode;
	}
	
	public String getNodeTypeDesc(byte type) {
		if (type < 0 || type > pois.length) {
			System.out.println("ERROR: wrong type " + type);
			return null;
		}
		return pois[type].description;
	}
	
	public byte getMaxType() {
		return (byte)pois.length;
	}
	
	private byte typeTotype(byte type) {
		return type;		
	}
}
