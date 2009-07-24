package de.ueller.gps.data;

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
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.names.Names;
import de.ueller.midlet.gps.routing.TravelMode;
import de.ueller.midlet.gps.tile.POIdescription;
import de.ueller.midlet.gps.tile.SoundDescription;
import de.ueller.midlet.gps.tile.WayDescription;

public class Legend {
	/**
	 * Specifies the format of the map on disk we expect to see
	 * This constant must be in sync with Osm2GpsMid
	 */
	public final static short MAP_FORMAT_VERSION = 38;
	
	/** The waypoint format used in the RecordStore. See PositionMark.java. */
	public final static short WAYPT_FORMAT_VERSION = 2;

	public final static byte MIN_PLACETYPE = 1; // city
	public final static byte MAX_PLACETYPE = 5; // suburb
	
	//public final static byte NODE_MASK_ROUTENODELINK=0x1; //obsolete
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
	// public final static byte LEGEND_FLAG_NON_ROUTABLE = 0x20;
	public final static byte LEGEND_FLAG_MIN_DESCRIPTION_SCALE = 0x40;
	
	public final static short ROUTE_FLAG_MOTORWAY = 0x01;  // used in ConnectionWithNode AND WayDescription
	public final static short ROUTE_FLAG_MOTORWAY_LINK = 0x02; // used in ConnectionWithNode AND WayDescription
	public final static short ROUTE_FLAG_ROUNDABOUT = 0x04; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_TUNNEL = 0x08; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_BRIDGE = 0x10; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_INVISIBLE = 0x20; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_INCONSISTENT_BEARING = 0x40; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_QUIET = 0x80; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_LEADS_TO_MULTIPLE_SAME_NAMED_WAYS = 0x100; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_BEAR_LEFT = 0x200; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_BEAR_RIGHT = 0x400; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_COMING_FROM_ONEWAY = 0x800; // used in ConnectionWithNode
	public final static short ROUTE_FLAG_AREA = 0x1000; // used in ConnectionWithNode
	
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
	
	public final static int COLOR_MAP_BACKGROUND = 0;
	public final static int COLOR_MAP_TEXT = 1;
	public final static int COLOR_SPEED_BACKGROUND = 2;
	public final static int COLOR_SPEED_TEXT = 3;
	public final static int COLOR_TITLEBAR_BACKGROUND = 4;
	public final static int COLOR_TITLEBAR_TEXT = 5;
	public final static int COLOR_WAYNAME_BACKGROUND = 6;
	public final static int COLOR_WAYNAME_TEXT = 7;
	public final static int COLOR_AREA_LABEL_TEXT = 8;
	public final static int COLOR_WAY_LABEL_TEXT = 9;
	public final static int COLOR_WAY_LABEL_TEXT_ABBREVIATED = 10;
	public final static int COLOR_POI_LABEL_TEXT = 11;
	public final static int COLOR_WAYPOINT_TEXT = 12;
	public final static int COLOR_ONEWAY_ARROW = 13;
	public final static int COLOR_ONEWAY_ARROW_NON_FITTING = 14;
	public final static int COLOR_RECORDING_SUSPENDED_TEXT = 15;
	public final static int COLOR_RECORDING_ON_TEXT = 16;
	public final static int COLOR_CELLID_LOG_ON_TEXT = 17;
	public final static int COLOR_CELLID_LOG_ON_ATTEMPTING_TEXT = 18;
	public final static int COLOR_AUDIOREC_TEXT = 19;	
	public final static int COLOR_TARGET_TEXT = 20;	
	public final static int COLOR_TARGET_LINE = 21;	
	public final static int COLOR_MAP_CURSOR = 22;	
	public final static int COLOR_MAP_POSINDICATOR = 23;	
	public final static int COLOR_SCALEBAR = 24;	
	public final static int COLOR_ZOOM_BUTTON = 25;	
	public final static int COLOR_ZOOM_BUTTON_TEXT = 26;	
	public final static int COLOR_COMPASS_DIRECTION_BACKGROUND = 27;	
	public final static int COLOR_COMPASS_DIRECTION_TEXT = 28;	
	public final static int COLOR_RI_AT_TARGET = 29;
	public final static int COLOR_RI_ON_ROUTE = 30;
	public final static int COLOR_RI_OFF_ROUTE_SLIGHT = 31;
	public final static int COLOR_RI_OFF_ROUTE_FULL = 32;
	public final static int COLOR_RI_NOT_AT_ROUTE_LINE_YET = 33;
	public final static int COLOR_RI_CHECK_DIRECTION = 34;
	public final static int COLOR_RI_TEXT = 35;
	public final static int COLOR_RI_DISTANCE_BACKGROUND = 36;
	public final static int COLOR_RI_DISTANCE_TEXT = 37;
	public final static int COLOR_RI_OFF_DISTANCE_TEXT = 38;
	public final static int COLOR_ROUTE_ROUTELINE = 39;
	public final static int COLOR_ROUTE_ROUTELINE_BORDER = 40;
	public final static int COLOR_ROUTE_PRIOR_ROUTELINE = 41;
	public final static int COLOR_ROUTE_PRIOR_ROUTELINE_BORDER = 42;
	public final static int COLOR_ROUTE_ROUTEDOT = 43;
	public final static int COLOR_ROUTE_ROUTEDOT_BORDER = 44;
	public final static int COLOR_ICONMENU_BACKGROUND = 45;
	public final static int COLOR_ICONMENU_TABBUTTON_BORDER = 46;
	public final static int COLOR_ICONMENU_TABBUTTON_TEXT = 47;
	public final static int COLOR_ICONMENU_TABBUTTON_TEXT_HIGHLIGHT = 48;
	public final static int COLOR_ICONMENU_TABBUTTON_TEXT_INACTIVE = 49;
	public final static int COLOR_ICONMENU_ICON_TEXT = 50;
	public final static int COLOR_ICONMENU_ICON_BORDER_HIGHLIGHT = 51;
	public final static int COLOR_ALERT_BACKGROUND = 52;
	public final static int COLOR_ALERT_BORDER = 53;
	public final static int COLOR_ALERT_TITLE_BACKGROUND = 54;
	public final static int COLOR_ALERT_TEXT = 55;
	public final static int COLOR_CUSTOMMENU_BACKGROUND = 56;
	public final static int COLOR_CUSTOMMENU_BORDER = 57;
	public final static int COLOR_CUSTOMMENU_TITLE_BACKGROUND = 58;
	public final static int COLOR_CUSTOMMENU_TEXT = 59;
	public final static int COLOR_CUSTOMMENU_HIGHLIGHT_BACKGROUND = 60;
	public final static int COLOR_TACHO_BACKGROUND = 61;
	public final static int COLOR_TACHO_TEXT = 62;
	public final static int COLOR_COUNT = 63;

	public static int COLORS[] = new int[COLOR_COUNT];

	public static String appVersion;
	public static String bundleDate;
	public static boolean enableEdits;
	
	private static POIdescription[] pois;
	private static WayDescription[] ways;
	private static SoundDescription[] sounds;
	
	private static String namePartRequired[] = new String[3];
	
	private static TravelMode midletTravelModes[];	
	
	private final static Logger logger=Logger.getInstance(Legend.class,Logger.TRACE);
	
	public Legend() throws IOException {
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
					"please use the correct Osm2GpsMid to recreate the map " +
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
		
		// read colors
		int count = (int) ds.readShort();
		if (count != COLOR_COUNT) {
			throw new IOException("Map file contains " + count + "colors but midlet's COLOR_COUNT is " + COLOR_COUNT);
		}
		for (int i=0; i<COLOR_COUNT; i++) {
			COLORS[i] = ds.readInt();
		}
		
		/**
		 * Read Travel Modes
		 */
		count = (int) ds.readByte();
		midletTravelModes = new TravelMode[count];
		for (int i=0; i<count;i++) {
			midletTravelModes[i] = new TravelMode();
			midletTravelModes[i].travelModeName = ds.readUTF();
			midletTravelModes[i].maxPrepareMeters = ds.readShort();
			midletTravelModes[i].maxInMeters = ds.readShort();
			midletTravelModes[i].maxEstimationSpeed = ds.readShort();
			midletTravelModes[i].travelModeFlags = ds.readByte();
		}
		
		// If we do not have the travel mode stored defined in the record store in the midlet data, use the first one 
		if (Configuration.getTravelModeNr() > Legend.getTravelModes().length-1) {
			Configuration.setTravelMode(0);
		}
		
		
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
					pois[i].image = Image.createImage(Configuration.getMapResource(imageName));
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
					pois[i].searchIcon = Image.createImage(Configuration.getMapResource(imageName));
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
			if ((flags & LEGEND_FLAG_TEXT_COLOR) > 0) {			
				pois[i].textColor = ds.readInt();
			} else {
				pois[i].textColor = COLORS[COLOR_POI_LABEL_TEXT];
			}
			pois[i].overviewMode=OM_SHOWNORMAL;
			//#if polish.api.osm-editing
			if (enableEdits) {
				int noKVpairs = ds.readShort();
				pois[i].osmTags = new String[noKVpairs*2];
				for (int j = 0; j < noKVpairs*2; j++) {
					pois[i].osmTags[j] =  ds.readUTF();
				}
			}
			//#endif
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
			ways[i].routeFlags = ds.readByte();			
			ways[i].description = ds.readUTF();			
			ways[i].maxScale = ds.readInt();
			ways[i].maxTextScale = ds.readInt();
			ways[i].isArea = ds.readBoolean();
			ways[i].lineColor = ds.readInt();
			ways[i].boardedColor = ds.readInt();
			ways[i].wayWidth = ds.readByte();
			ways[i].overviewMode = OM_SHOWNORMAL;
			ways[i].lineStyle = ds.readInt();
			if ((flags & LEGEND_FLAG_MIN_ONEWAY_ARROW_SCALE) > 0)
				ways[i].maxOnewayArrowScale = ds.readInt();
			else
				ways[i].maxOnewayArrowScale = ways[i].maxScale; 
			
			if ((flags & LEGEND_FLAG_MIN_DESCRIPTION_SCALE) > 0)
				ways[i].maxDescriptionScale = ds.readInt();
			else
				ways[i].maxDescriptionScale = 15000;
			//#if polish.api.osm-editing
			if (enableEdits) {
				int noKVpairs = ds.readShort();
				ways[i].osmTags = new String[noKVpairs*2];
				for (int j = 0; j < noKVpairs*2; j++) {
					ways[i].osmTags[j] = ds.readUTF();
				}
			}
			//#endif
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
	
	//#if polish.api.osm-editing
	public static final String[] getNodeOsmTags(byte type) {
		return pois[type].osmTags;
	}
	//#endif
	
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

	public static TravelMode [] getTravelModes() {
		return midletTravelModes;
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
