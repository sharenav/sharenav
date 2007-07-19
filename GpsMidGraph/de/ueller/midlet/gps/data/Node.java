package de.ueller.midlet.gps.data;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.IOException;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

//import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.PaintContext;




public class Node extends Entity {
	public float	radlat;
	public float	radlon;
	public final static float NORTH_POLE = 90.0f;
    public final static float SOUTH_POLE = -NORTH_POLE;
    public final static float DATELINE = 180.0f;
    public final static float LON_RANGE = 360.0f;

    public final static float EQUIVALENT_TOLERANCE = 0.00001f;
//	private final static Logger logger=Logger.getInstance(Node.class,Logger.DEBUG);

	/** construct a node from the stream. Assumes
	 * that the id is readed befor.
	 * @param is
	 * @throws IOException
	 */
	public Node(DataInputStream	is) throws IOException{
//		logger.info("read node");
        radlat = is.readFloat();
        radlon = is.readFloat();
		byte f=is.readByte();
		if ((f & 1) == 1){
//			name=is.readUTF();
			nameIdx=new Short(is.readShort());
			type=is.readByte();
		}
		
	}

	
    /**
     * Construct a LatLonPoint from raw float lat/lon in decimal
     * degrees.
     * 
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     */
    public Node(float lat, float lon) {
        setLatLon(lat, lon);
    }

    /**
     * Construct a LatLonPoint from raw float lat/lon in radians.
     * 
     * @param lat latitude in radians
     * @param lon longitude in radians
     * @param isRadian placeholder indicates radians
     */
    public Node(float lat, float lon, boolean isRadian) {
        setLatLon(lat, lon, isRadian);
    }


	public Node() {
		// TODO Auto-generated constructor stub
	}


	public float getLatitude() {
		return radlat * MoreMath.FAC_RADTODEC;
	}

	
	public float getLongitude() {
		return radlon * MoreMath.FAC_RADTODEC;
	}
	public void setLatitude(float l) {
		radlat=l*MoreMath.FAC_DECTORAD;
	}

	
	public void setLongitude(float l) {
		radlon=l*MoreMath.FAC_DECTORAD;
	}
	
    /**
     * Set latitude and longitude.
     * 
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     */
    public void setLatLon(float lat, float lon) {
        radlat = normalize_latitude(lat)*MoreMath.FAC_DECTORAD;
        radlon = wrap_longitude(lon)*MoreMath.FAC_DECTORAD;
    }

    /**
     * Set latitude and longitude.
     * 
     * @param lat latitude in radians
     * @param lon longitude in radians
     * @param isRadian placeholder indicates radians
     */
    public void setLatLon(float lat, float lon, boolean isRadian) {
        if (isRadian) {
            radlat = lat;
            radlon = lon;
        } else {
            setLatLon(lat, lon);
        }
    }
    public void setLatLon(Node llpt) {
        radlat = llpt.radlat;
        radlon = llpt.radlon;
    }

    
    /**
     * Sets latitude to something sane.
     * 
     * @param lat latitude in decimal degrees
     * @return float normalized latitude in decimal degrees
     *         (&minus;90&deg; &le; &phi; &le; 90&deg;)
     */
    final public static float normalize_latitude(float lat) {
        if (lat > NORTH_POLE) {
            lat = NORTH_POLE;
        }
        if (lat < SOUTH_POLE) {
            lat = SOUTH_POLE;
        }
        return lat;
    }

    /**
     * Sets longitude to something sane.
     * 
     * @param lon longitude in decimal degrees
     * @return float wrapped longitude in decimal degrees
     *         (&minus;180&deg; &le; &lambda; &le; 180&deg;)
     */
    final public static float wrap_longitude(float lon) {
        if ((lon < -DATELINE) || (lon > DATELINE)) {
            //System.out.print("LatLonPoint: wrapping longitude " +
            // lon);
            lon += DATELINE;
            lon = lon % LON_RANGE;
            lon = (lon < 0) ? DATELINE + lon : -DATELINE + lon;
            //Debug.output(" to " + lon);
        }
        return lon;
    }

    public void paint(PaintContext pc){
    	Image img=null;
//    	logger.debug("set color "+pc);
    	//if (node.name == null) continue;
    	switch (type) {
    	case 1:
    		pc.g.setColor(255, 50, 50);
    		break;
    	case 2:
    		pc.g.setColor(200, 100, 100);
    		break;
    	case 3:
    		pc.g.setColor(180, 180, 50);
    		break;
    	case 4:
    		pc.g.setColor(160, 160, 90);
    		break;
    	case 5:
    		pc.g.setColor(0, 0, 0);
    		break;
    	case 6:
    		pc.g.setColor(0, 0, 0);
    		break;
    	case C.NODE_AMENITY_PARKING:
    		img=pc.images.IMG_PARKING;
    		break;
    	case C.NODE_AMENITY_TELEPHONE:
    		img=pc.images.IMG_TELEPHONE;
    		break;
    	case C.NODE_AMENITY_SCHOOL:
    		img=pc.images.IMG_SCHOOL;
    		break;
    	case C.NODE_AMENITY_FUEL:
    		img=pc.images.IMG_FUEL;
    		break;

    	}
//    	logger.debug("calc pos "+pc);
    	pc.getP().forward(radlat, radlon, pc.swapLineP, true);
    	if  (img != null){
//        	logger.debug("draw img " + img);
    		if (nameIdx == null) {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y, Graphics.VCENTER | Graphics.HCENTER);
			} else {
				pc.g.drawImage(img, pc.swapLineP.x, pc.swapLineP.y, Graphics.BOTTOM | Graphics.HCENTER);
			}
    	}
    	if (nameIdx != null){
//        	logger.debug("draw txt " + );
    		String name=pc.trace.getName(nameIdx);
    		if (name != null){
    			pc.g.setColor(0, 0, 0);
    			if  (img == null) {
					pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y, Graphics.BASELINE | Graphics.HCENTER);
				} else {
					pc.g.drawString(name, pc.swapLineP.x, pc.swapLineP.y, Graphics.TOP | Graphics.HCENTER);
				}
    		}
    	}
    }
    
    public Node clone(){
    	Node n=new Node();
    	if (nameIdx != null) {
			n.nameIdx=new Short(nameIdx.shortValue());
		}
    	n.type=type;
    	n.radlat=radlat;
    	n.radlon=radlon;
    	return n;
    }
    
    public String toString(){
    	return ("node: " + radlat + "/" + radlon);
    }
}
