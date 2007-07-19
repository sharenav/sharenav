package de.ueller.gps.jsr179;

import java.util.Date;

import javax.microedition.location.Coordinates;
import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;

import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.LocationMsgProducer;
import de.ueller.midlet.gps.LocationMsgReceiver;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

public class JSR179Input implements LocationListener ,LocationMsgProducer{
	//#debug
	private final static Logger logger = Logger.getInstance(JSR179Input.class,Logger.TRACE);

    /** location provider */
    private LocationProvider locationProvider = null;
	private final LocationMsgReceiver receiver;
	Date date=new Date();
	Position pos=new Position(0f,0f,0f,0f,0f,0,date);
	

	public JSR179Input(LocationMsgReceiver receiver){
//#debug
		logger.info("start JSR179 LocationProvider");
		this.receiver = receiver;
		createLocationProvider();
	}
	
    /**
     * Initializes LocationProvider
     * uses default criteria
     * @throws Exception 
     */
    void createLocationProvider() {
//#debug
    	logger.trace("enter createLocationProvider()");
        if (locationProvider == null) {
            Criteria criteria = new Criteria();

            try {
                locationProvider = LocationProvider.getInstance(criteria);
            } catch (LocationException le) {
            	//#debug
                logger.error("Cannot create LocationProvider for this criteria.");
                locationProvider=null;
        		receiver.locationDecoderEnd("no JSR179 Provider");
            }
            locationProvider.setLocationListener(this, 1, -1, -1);
            
        }
        //#debug
    	logger.trace("exit createLocationProvider()");
    }

	public void locationUpdated(LocationProvider provider, Location location) {
		//#debug
    	logger.trace("enter locationUpdated(provider,location)");
		Coordinates coordinates = location.getQualifiedCoordinates();
        pos.latitude = (float) coordinates.getLatitude();
        pos.longitude = (float) coordinates.getLongitude();
        pos.altitude = (float) coordinates.getAltitude();
        pos.course=location.getCourse();
        pos.speed=location.getSpeed();
        pos.date.setTime(location.getTimestamp());
        receiver.receivePosItion(pos);
//    	logger.trace("exit locationUpdated(provider,location)");
	}

	public void providerStateChanged(LocationProvider arg0, int arg1) {
		//#debug
		logger.trace("enter providerStateChanged(locationProvider,"+arg1+"");
		if (LocationProvider.AVAILABLE != arg1){
			receiver.receiveMessage("provider stopped");
			receiver.locationDecoderEnd();
		}
		//#debug
    	logger.trace("exit providerStateChanged(locationProvider,"+arg1+"");
	}
	
	public void close() {
		//#debug
    	logger.trace("enter close()");
//		if (locationProvider != null){
//			locationProvider.setLocationListener(null, -1, -1, -1);
//		}
		locationProvider=null;
		receiver.locationDecoderEnd();
		//#debug
    	logger.trace("exit close()");
	}

}
