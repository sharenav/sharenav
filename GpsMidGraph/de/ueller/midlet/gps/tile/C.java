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

//import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.names.Names;

public class C {
	/**
	 * Specifies the format of the map on disk we expect to see
	 * This constant must be in sync with Osm2GpsMid
	 */
	public final static short MAP_FORMAT_VERSION = 23;
	
	public final static byte MIN_PLACETYPE = 1; // city
	public final static byte MAX_PLACETYPE = 5; // suburb
	
	// FIXME: reading the RouteNodeLink is obsolete as it is no more written by Osm2GpsMid, it should be removed on the next MAP_VERSION update
	public final static byte NODE_MASK_ROUTENODELINK=0x1;
	public final static byte NODE_MASK_TYPE=0x2;
	public final static byte NODE_MASK_NAME=0x4;
	public final static byte NODE_MASK_ROUTENODE=0x8;
	public final static byte NODE_MASK_NAMEHIGH=0x10;
	
	public final static byte LEGEND_FLAG_IMAGE = 0x01;
	public final static byte LEGEND_FLAG_SEARCH_IMAGE = 0x02;
	public final static byte LEGEND_FLAG_MIN_IMAGE_SCALE = 0x04;
	public final static byte LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE = LEGEND_FLAG_MIN_IMAGE_SCALE;
	public final static byte LEGEND_FLAG_TEXT_COLOR = 0x08;
	public final static byte LEGEND_FLAG_NON_HIDEABLE = 0x10;
	public final static byte LEGEND_FLAG_NON_ROUTABLE = 0x20;
	public final static byte LEGEND_FLAG_MIN_DESCRIPTION_SCALE = 0x40;
	
	public final static byte ROUTE_FLAG_MOTORWAY = 0x01;  // used in ConnectionWithNode AND WayDescription
	public final static byte ROUTE_FLAG_MOTORWAY_LINK = 0x02; // used in ConnectionWithNode AND WayDescription
	public final static byte ROUTE_FLAG_ROUNDABOUT = 0x04; // used in ConnectionWithNode
	public final static byte ROUTE_FLAG_TUNNEL = 0x08; // used in ConnectionWithNode
	public final static byte ROUTE_FLAG_BRIDGE = 0x10; // used in ConnectionWithNode
	public final static byte ROUTE_FLAG_INVISIBLE = 0x20; // used in ConnectionWithNode
	public final static byte ROUTE_FLAG_INCONSISTENT_BEARING = 0x40; // used in ConnectionWithNode
	
	/**
	 * minimum distances to set the is_in name to the next city
	 * to get the minimum distance use: <code>MAX_DIST_CITY[node.getType(null)]</code>
	 */

	public final static byte NAME_CITY=1;
	public final static byte NAME_SUBURB=2;
	public final static byte NAME_STREET=3;
	public final static byte NAME_AMENITY=4;
	
	public final static byte OM_HIDE=0;
	public final static byte OM_SHOWNORMAL=1;
	public final static byte OM_OVERVIEW=2;
	public final static byte OM_OVERVIEW2=4; // this is used to set overview mode for the element without setting the checkbox in the UI
	public final static byte OM_MODE_MASK=OM_SHOWNORMAL | OM_OVERVIEW | OM_OVERVIEW2;
	
	public final static byte OM_NAME_ALL=0;
	public final static byte OM_NO_NAME=8;
	public final static byte OM_WITH_NAME=16;
	public final static byte OM_WITH_NAMEPART=32;
	public final static byte OM_NAME_MASK=OM_NO_NAME | OM_WITH_NAME | OM_WITH_NAMEPART;
	
	public static int BACKGROUND_COLOR = 0x009BFF9B;
	public static int ROUTE_COLOR = 0x0000C0C0;  //
	public static int ROUTE_BORDERCOLOR = 0x0064FFFF;  // not read from file yet
	public static int ROUTEPRIOR_COLOR = 0x0064FFFF;  // not read from file yet
	public static int ROUTEPRIOR_BORDERCOLOR = 0x0064FFFF;  // not read from file yet
	public static String appVersion;
	public static String bundleDate;
	public static boolean enableEdits;
	
	private static POIdescription[] pois;
	private static WayDescription[] ways;
	private static SoundDescription[] sounds;
	
	private static String namePartRequired[] = new String[3];
	
	private final static Logger logger=Logger.getInstance(C.class,Logger.TRACE);
	
	public C() throws IOException {
		InputStream is = Configuration.getMapResource("/legend.dat");
		
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
			throw new IOException("The Map files are not the version we expected, " +
					"please ues the correct Osm2GpsMid to recreate the map " +
					"data.  Expected: " + MAP_FORMAT_VERSION + " Read: " + mapVersion);
		}
		
		appVersion = ds.readUTF();
		bundleDate = ds.readUTF();
		enableEdits = ds.readBoolean();
		//#if polish.api.osm-editing
		
		//#else
		if (enableEdits) {
			throw new IOException("The Map files are enabled for editing, but editing is not compiled into GpsMid." +
					"Please ues the correct Osm2GpsMid to recreate the map ");
		}
		//#endif
		
		BACKGROUND_COLOR = ds.readInt();		
		ROUTE_COLOR = ds.readInt();
		ROUTE_BORDERCOLOR = ds.readInt();
		ROUTEPRIOR_COLOR = ds.readInt();
		ROUTEPRIOR_BORDERCOLOR = ds.readInt();
		
		readPOIdescriptions(ds);
		readWayDescriptions(ds);
		readSoundDescriptions(ds);
		//System.out.println(getSoundDescription("DISCONNECT").soundFile);
		//System.out.println(getSoundDescription("CONNECT").soundFile);
				
		ds.close();
		
		namePartRequired[0] = "";
		namePartRequired[1] = "";
		namePartRequired[2] = "";
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
			pois[i].hideable = ((flags & LEGEND_FLAG_NON_HIDEABLE) == 0);	
			if ((flags & LEGEND_FLAG_IMAGE) > 0) {
				String imageName = ds.readUTF();
				//System.out.println("trying to open image " + imageName);
				try {
					pois[i].image = Image.createImage(imageName);
				} catch (IOException e) {
					//#debug info
					logger.info("could not open POI image " + imageName + " for " + pois[i].description);
					pois[i].image = generic;
				}				
			}
			if ((flags & LEGEND_FLAG_SEARCH_IMAGE) > 0) {
				String imageName = ds.readUTF();
				logger.debug("trying to open search image " + imageName);
				try {
					pois[i].searchIcon = Image.createImage(imageName);
				} catch (IOException e) {
					//#debug info
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
			pois[i].overviewMode=OM_SHOWNORMAL;
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
			ways[i].hideable = ((flags & LEGEND_FLAG_NON_HIDEABLE) == 0);
			ways[i].routable = ((flags & LEGEND_FLAG_NON_ROUTABLE) == 0);
			ways[i].routeFlags = ds.readByte();			
			ways[i].description = ds.readUTF();			
			ways[i].maxScale = ds.readInt();
			ways[i].maxTextScale = ds.readInt();
			ways[i].isArea = ds.readBoolean();
			ways[i].lineColor = ds.readInt();
			ways[i].boardedColor = ds.readInt();
			ways[i].wayWidth = ds.readByte();
			ways[i].overviewMode = OM_SHOWNORMAL;
			boolean lineStyle = ds.readBoolean();
			if (lineStyle)
				ways[i].lineStyle = Graphics.DOTTED;
			else
				ways[i].lineStyle = Graphics.SOLID;
			if ((flags & LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE) > 0)
				ways[i].maxOnewayArrowScale = ds.readInt();
			else
				ways[i].maxOnewayArrowScale = ways[i].maxScale; 
			
			if ((flags & LEGEND_FLAG_MIN_DESCRIPTION_SCALE) > 0)
				ways[i].maxDescriptionScale = ds.readInt();
			else
				ways[i].maxDescriptionScale = 15000; 
		}
	}	
	
	private void readSoundDescriptions(DataInputStream ds) throws IOException {		
		sounds = new SoundDescription[ds.readByte()];
		for (int i = 0; i < sounds.length; i++) {
			sounds[i] = new SoundDescription();
			sounds[i].name= ds.readUTF();
			sounds[i].soundFile= ds.readUTF();
		}
	}
	
	
	public static final int getNodeTextColor(byte type) {
		return pois[type].textColor;
	}
	
	public static final Image getNodeImage(byte type)  {
		return pois[type].image;
	}
	public static final Image getNodeSearchImage(byte type)  {
		return pois[type].searchIcon;
	}
	
	public static final int getNodeMaxScale(byte type) {
		return pois[type].maxImageScale;
	}
	public static final int getNodeMaxTextScale(byte type) {
		return pois[type].maxTextScale;
	}
	
	public static final boolean isNodeImageCentered(byte type) {
		return pois[type].imageCenteredOnNode;
	}
	public static final boolean isNodeHideable(byte type) {
		return pois[type].hideable;
	}
	public static final byte getNodeOverviewMode(byte type) {
		return pois[type].overviewMode;
	}
	public static void setNodeOverviewMode(byte type, byte state) {
		pois[type].overviewMode = state;
	}
	public static void clearAllNodesOverviewMode() {
		for (byte i = 1; i < getMaxType(); i++) {
			pois[i].overviewMode = OM_SHOWNORMAL;
		}
	}	
	public static final  String getNodeTypeDesc(byte type) {
		if (type < 0 || type > pois.length) {
			logger.error("ERROR: wrong type " + type);
			return null;
		}
		return pois[type].description;
	}
	
	public static final WayDescription getWayDescription(byte type) {			
		if (type >= ways.length) {
			logger.error("Invalid type request: " + type);
			return null;
 		}
		return ways[type];
	}
	
	public static final boolean isWayHideable(byte type) {
		return ways[type].hideable;
	}

	public static byte getWayOverviewMode(byte type) {
		return ways[type].overviewMode;
	}

	public static void setWayOverviewMode(byte type, byte state) {
		ways[type].overviewMode = state;
	}

	public static String get0Poi1Area2WayNamePart(byte nr) {
		return namePartRequired[nr];
	}
	
	public static void set0Poi1Area2WayNamePart(byte nr, String namePart) {
		namePartRequired[nr] = namePart;
	}

	public static void clearAllWaysOverviewMode() {
		for (byte i = 1; i < getMaxWayType(); i++) {
			ways[i].overviewMode = OM_SHOWNORMAL;
		}
	}
	
	public static final byte getMaxWayType() {
		return (byte)ways.length;
	}
	
	public static final SoundDescription getSoundDescription(String Name) {			
		for(byte i=0;i<sounds.length;i++) {
			if (sounds[i].name.equals(Name)) {
				return sounds[i];
			}			
		}
		return null;
	}
	
	public static final byte getMaxType() {
		return (byte)pois.length;
	}
	
	public static final byte scaleToTile(int scale) {
		if (scale < 45000) {
			return 3;
		}
		if (scale < 180000) {
			return 2;
		}
		if (scale < 900000) {
			return 1;
		}		
		return 0;
	}

	public static String getAppVersion() {
		return appVersion;
	}

	public static String getBundleDate() {
		return bundleDate;
	}	
}
