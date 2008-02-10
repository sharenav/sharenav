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
	private final static Logger logger = Logger.getInstance(JSR179Input.class,Logger.TRACE);

    /** location provider */
    private LocationProvider locationProvider = null;
	private final LocationMsgReceiver receiver;
	Date date=new Date();
	Position pos=new Position(0f,0f,0f,0f,0f,0,date);
	

	public JSR179Input(LocationMsgReceiver receiver){
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
    	logger.trace("enter createLocationProvider()");
        if (locationProvider == null) {
            Criteria criteria = new Criteria();

            try {
                locationProvider = LocationProvider.getInstance(criteria);
                logger.info(locationProvider.toString());
            } catch (LocationException le) {
            	//#debug
                logger.error("Cannot create LocationProvider for this criteria.");
                locationProvider=null;
        		receiver.locationDecoderEnd("no JSR179 Provider");
            }            
            updateSolution(locationProvider.getState());
        }
        //#debug
    	logger.trace("exit createLocationProvider()");
    }

	public void locationUpdated(LocationProvider provider, Location location) {		
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

	public void providerStateChanged(LocationProvider arg0, int state) {		
		logger.trace("enter providerStateChanged(locationProvider," + state + "");
		updateSolution(state);		
	}
	
	public void close() {
		//#debug
    	logger.trace("enter close()");
//		if (locationProvider != null){
//			locationProvider.setLocationListener(null, -1, -1, -1);
//		}
    	locationProvider.setLocationListener(null, 0, 0, 0);
		locationProvider=null;
		receiver.locationDecoderEnd();
		//#debug
    	logger.trace("exit close()");
	}
	
	private void updateSolution(int state) {
		if (state == LocationProvider.AVAILABLE) {
			locationProvider.setLocationListener(this, 1, -1, -1);
			receiver.receiveSolution("On");
		}
		if (state == LocationProvider.OUT_OF_SERVICE) {
			receiver.receiveSolution("Off");
			locationProvider.setLocationListener(this, 0, -1, -1);
			receiver.receiveMessage("provider stopped");						
		}
		if (state == LocationProvider.TEMPORARILY_UNAVAILABLE) {
			locationProvider.setLocationListener(this, 0, -1, -1);
			receiver.receiveSolution("0");
		}
	}

}
