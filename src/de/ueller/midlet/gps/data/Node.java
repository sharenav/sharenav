package de.ueller.midlet.gps.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.midlet.gps.Logger;




public class Node {
//	public float lat=0.0f;
//	public float lon=0.0f;
	public Short nameIdx;
	public float	radlat;
	public float	radlon;
	public byte type;
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
		return ProjMath.radToDeg(radlat);
	}

	
	public float getLongitude() {
		return ProjMath.radToDeg(radlon);
	}
	public void setLatitude(float l) {
		radlat=ProjMath.degToRad(l);
	}

	
	public void setLongitude(float l) {
		radlon=ProjMath.degToRad(l);
	}
	
    /**
     * Set latitude and longitude.
     * 
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     */
    public void setLatLon(float lat, float lon) {
        radlat = ProjMath.degToRad(normalize_latitude(lat));
        radlon = ProjMath.degToRad(wrap_longitude(lon));
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


}
