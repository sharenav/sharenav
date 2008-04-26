package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Graphics;
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
	public final static short MAP_FORMAT_VERSION = 11;
	
	public final static byte NODE_MASK_ROUTENODELINK=0x1;
	public final static byte NODE_MASK_TYPE=0x2;
	public final static byte NODE_MASK_NAME=0x4;
	public final static byte NODE_MASK_ROUTENODE=0x8;
	public final static byte NODE_MASK_NAMEHIGH=0x10;
	
	public final static byte LEGEND_FLAG_IMAGE = 0x01;
	public final static byte LEGEND_FLAG_SEARCH_IMAGE = 0x02;
	public final static byte LEGEND_FLAG_MIN_IMAGE_SCALE = 0x04;
	public final static byte LEGEND_FLAG_TEXT_COLOR = 0x08;
	
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
	private WayDescription[] ways;
	
	private final static Logger logger=Logger.getInstance(C.class,Logger.TRACE);
	
	public C() throws IOException {
		InputStream is = GpsMid.getInstance().getConfig().getMapResource("/legend.dat");
		
		if (is == null) {
			logger.error("Failed to open the legend file");
			return;			
		}

		DataInputStream ds = new DataInputStream(is);
		
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
		
		readPOIdescriptions(ds);
		readWayDescriptions(ds);
		//readWayDescriptionsOld(ds);
		
		ds.close();				
	}
	
	private void readPOIdescriptions(DataInputStream ds) throws IOException {		
		Image generic = Image.createImage("/unknown.png");
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
			if ((flags & LEGEND_FLAG_IMAGE) > 0) {
				String imageName = ds.readUTF();
				//System.out.println("trying to open image " + imageName);
				try {
					pois[i].image = Image.createImage(imageName);
				} catch (IOException e) {
					logger.info("could not open POI image " + imageName + " for " + pois[i].description);
					pois[i].image = generic;
				}				
			}
			if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {
				String imageName = ds.readUTF();
				System.out.println("trying to open search image " + imageName);
				try {
					pois[i].searchIcon = Image.createImage(imageName);
				} catch (IOException e) {
					logger.info("could not open POI image " + imageName + " for " + pois[i].description);
					pois[i].searchIcon = generic;
				}				
			} else if (pois[i].image != null) {
				pois[i].searchIcon = pois[i].image;
			}
			if ((flags & LEGEND_FLAG_MIN_IMAGE_SCALE) > 0)
				pois[i].maxTextScale = ds.readInt();
			else
				pois[i].maxTextScale = pois[i].maxImageScale; 
			if ((flags & LEGEND_FLAG_TEXT_COLOR) > 0)			
				pois[i].textColor = ds.readInt();
		}
	}
	
	private void readWayDescriptions(DataInputStream ds) throws IOException {		
		Image generic = Image.createImage("/unknown.png");
		ways = new WayDescription[ds.readByte()];		
		for (int i = 0; i < ways.length; i++) {
			ways[i] = new WayDescription();
			if (ds.readByte() != i)
				logger.error("Read legend had troubles");
			byte flags = ds.readByte();
			ways[i].description = ds.readUTF();
			System.out.println("WayDesc: " +  ways[i].description);
			ways[i].maxScale = ds.readInt();
			ways[i].isArea = ds.readBoolean();
			ways[i].lineColor = ds.readInt();
			ways[i].boardedColor = ds.readInt();
			ways[i].wayWidth = ds.readByte();
			System.out.println("   WayWidth: " +  ways[i].wayWidth);
			boolean lineStyle = ds.readBoolean();
			if (lineStyle)
				ways[i].lineStyle = Graphics.DOTTED;
			else
				ways[i].lineStyle = Graphics.SOLID;			
		}
	}

	/**
	 * Currently we don't have the support in the legend file format to actually
	 * load the descriptions properly. For the moment, we therefore hard code these
	 * values in this function.
	 * 
	 * @param ds
	 */
	/**
	 * @deprecated
	 */
	private void readWayDescriptionsOld(DataInputStream ds) {
		
		ways = new WayDescription[39];
		
		WayDescription way = new WayDescription();
		way.description = "WAY_HIGHWAY_MOTORWAY";
		way.isArea = false;
		way.lineColor = 0x00809BC0;
		way.boardedColor = 0x00;
		way.maxScale = 45000;
		way.wayWidth = 8;
		way.lineStyle = Graphics.SOLID;
		ways[1] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_MOTORWAY_LINK";
		way.isArea = false;
		way.lineColor = 0x00809BC0;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 8;
		ways[2] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_TRUNK";
		way.isArea = false;
		way.lineColor = 0x00E46D71;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 6;
		ways[3] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_PRIMARY";
		way.isArea = false;
		way.lineColor = 0x007FC97F;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 6;
		ways[4] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_SECONDARY";
		way.isArea = false;
		way.lineColor = 0x00FDBF6F;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 5;
		ways[5] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_MINOR";
		way.isArea = false;
		way.lineColor = 0x00FFFFFF ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 4;
		ways[6] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_RESIDENTIAL";
		way.isArea = false;
		way.lineColor = 0x00B4B4B4;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 3;
		ways[7] = way;

		way = new WayDescription();
		way.description = "WAY_JUNCTION_ROUNDABOUT";
		way.isArea = false;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[8] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_TRACK";
		way.isArea = false;
		way.lineColor = 0x00B4B4B4;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 2;
		ways[9] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_UNCLASSIFIED";
		way.isArea = false;
		way.lineColor = 0x00FFFFFF ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		way.wayWidth = 4;
		ways[10] = way;
		// ways after unclassified are not accessible by car

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_CYCLEWAY";
		way.isArea = false;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[11] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_FOOTWAY";
		way.isArea = false;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[12] = way;

		way = new WayDescription();
		way.description = "WAY_RAILWAY_UNCLASSIFIED";
		way.isArea = false;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[13] = way;

		way = new WayDescription();
		way.description = "WAY_RAILWAY_RAIL";
		way.isArea = false;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[14] = way;

		way = new WayDescription();
		way.description = "WAY_RAILWAY_SUBWAY";
		way.isArea = false;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[15] = way;

		way = new WayDescription();
		way.description = "WAY_WATERWAY_RIVER";
		way.isArea = false;
		way.lineColor = 0x003232FF;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[16] = way;

		way = new WayDescription();
		way.description = "WAY_HIGHWAY_STEPS";
		way.isArea = false;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[17] = way;
		

		way = new WayDescription();
		way.description = "AREA_AMENITY_UNCLASSIFIED";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[18] = way;

		way = new WayDescription();
		way.description = "AREA_AMENITY_PARKING";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[19] = way;

		way = new WayDescription();
		way.description = "AREA_AMENITY_PUBLIC_BUILDING";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[20] = way;

		way = new WayDescription();
		way.description = "AREA_NATURAL_WATER";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[21] = way;

		way = new WayDescription();
		way.description = "AREA_LEISURE_PARK";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[22] = way;
		

		way = new WayDescription();
		way.description = "AREA_LANDUSE_FARM";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[23] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_QUARRY";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[24] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_LANDFILL";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[25] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_BASIN";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[26] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_RESERVOIR";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[27] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_FOREST";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[28] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_ALLOTMENTS";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[29] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_RESIDENTIAL";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[30] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_RETAIL";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[31] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_COMMERCIAL";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[32] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_INDUSTRIAL";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[33] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_BROWNFIELD";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[34] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_GREENFIELD";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[35] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_CEMETERY";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[36] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_VILLAGE_GREEN";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[37] = way;

		way = new WayDescription();
		way.description = "AREA_LANDUSE_RECREATION_GROUND";
		way.isArea = true;
		way.lineColor = 0x00 ;
		way.boardedColor = 0x00;
		way.maxScale = 45000 ;
		ways[38] = way;

		
		
	}
	
	public int getNodeTextColor(byte type) {
		return pois[typeTotype(type)].textColor;
	}
	
	public Image getNodeImage(byte type)  {
		return pois[typeTotype(type)].image;
	}
	public Image getNodeSearchImage(byte type)  {
		return pois[typeTotype(type)].searchIcon;
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
	
	public WayDescription getWayDescription(byte type) {			
		if (type >= ways.length) {
			logger.error("Invalid type request: " + type);
			return null;
 		}
		return ways[type];
	}
	
	public byte getMaxType() {
		return (byte)pois.length;
	}
	
	private byte typeTotype(byte type) {
		return type;		
	}

	/**
	 * Storing the information about waytypes requires the types to be in a 
	 * densly packed linear form. The types we get from the map files however
	 * have holes, as some of the information is encoded in the type range
	 * 
	 *  This is intermediate code until we change the map format.
	 * @param type
	 * @return
	 */
	private byte typetoWayTpye (byte type) {
		/*switch (type) {
		case WAY_HIGHWAY_MOTORWAY: {
			return 1;
		}
		case WAY_HIGHWAY_MOTORWAY_LINK: {
			return 2;
		}
		case WAY_HIGHWAY_TRUNK: {
			return 3;
		}
		case WAY_HIGHWAY_PRIMARY: {
			return 4;
		}
		case WAY_HIGHWAY_SECONDARY: {
			return 5;
		}
		case WAY_HIGHWAY_MINOR: {
			return 6;
		}
		case WAY_HIGHWAY_RESIDENTIAL: {
			return 7;
		}
		case WAY_JUNCTION_ROUNDABOUT: {
			return 8;
		}
		case WAY_HIGHWAY_TRACK: {
			return 9;
		}
		case WAY_HIGHWAY_UNCLASSIFIED: {
			return 10;
		}
		// ways after unclassified are not accessible by car
		case WAY_HIGHWAY_CYCLEWAY: {
			return 11;
		}
		case WAY_HIGHWAY_FOOTWAY: {
			return 12;
		}
		case WAY_RAILWAY_UNCLASSIFIED: {
			return 13;
		}
		case WAY_RAILWAY_RAIL: {
			return  14;
		}
		case WAY_RAILWAY_SUBWAY: {
			return  15;
		}
		case WAY_WATERWAY_RIVER: {
			return 16;
		}
		case WAY_HIGHWAY_STEPS: {
			return 17;
		}

		case AREA_AMENITY_UNCLASSIFIED: {
			return 18;
		}
		case AREA_AMENITY_PARKING: {
			return 19;
		}
		case AREA_AMENITY_PUBLIC_BUILDING: {
			return 20;
		}
		case AREA_NATURAL_WATER: {
			return 21;
		}
		case AREA_LEISURE_PARK: {
			return 22;
		}

		case AREA_LANDUSE_FARM: {
			return 23;
		}
		case AREA_LANDUSE_QUARRY: {
			return 24;
		}
		case AREA_LANDUSE_LANDFILL: {
			return 25;
		}
		case AREA_LANDUSE_BASIN: {
			return 26;
		}
		case AREA_LANDUSE_RESERVOIR: {
			return 27;
		}
		case AREA_LANDUSE_FOREST: {
			return 28;
		}
		case AREA_LANDUSE_ALLOTMENTS: {
			return 29;
		}
		case AREA_LANDUSE_RESIDENTIAL: {
			return 30;
		}
		case AREA_LANDUSE_RETAIL: {
			return 31;
		}
		case AREA_LANDUSE_COMMERCIAL: {
			return 32;
		}
		case AREA_LANDUSE_INDUSTRIAL: {
			return 33;
		}
		case AREA_LANDUSE_BROWNFIELD: {
			return 34;
		}
		case AREA_LANDUSE_GREENFIELD: {
			return 35;
		}
		case AREA_LANDUSE_CEMETERY: {
			return 36;
		}
		case AREA_LANDUSE_VILLAGE_GREEN: {
			return 37;
		}
		case AREA_LANDUSE_RECREATION_GROUND: {
			return 38;
		}
		}
		return 0;
		*/
		return type;
	}
}
