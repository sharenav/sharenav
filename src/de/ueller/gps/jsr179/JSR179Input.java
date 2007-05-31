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

public class JSR179Input implements LocationListener ,LocationMsgProducer{

    /** location provider */
    private LocationProvider locationProvider = null;
	private final LocationMsgReceiver receiver;
	Date date=new Date();
	Position pos=new Position(0f,0f,0f,0f,0f,0,date);
	

	public JSR179Input(LocationMsgReceiver receiver){
		this.receiver = receiver;
		createLocationProvider();
	}
	
    /**
     * Initializes LocationProvider
     * uses default criteria
     * @throws Exception 
     */
    void createLocationProvider() {
        if (locationProvider == null) {
            Criteria criteria = new Criteria();

            try {
                locationProvider = LocationProvider.getInstance(criteria);
            } catch (LocationException le) {
                System.out.println("Cannot create LocationProvider for this criteria.");
        		receiver.locationDecoderEnd("no JSR179 Provider");
            }
            locationProvider.setLocationListener(this, 1, -1, -1);
            
        }
    }

	public void locationUpdated(LocationProvider provider, Location location) {
		Coordinates coordinates = location.getQualifiedCoordinates();
        pos.latitude = (float) coordinates.getLatitude();
        pos.longitude = (float) coordinates.getLongitude();
        pos.altitude = (float) coordinates.getAltitude();
        pos.course=location.getCourse();
        pos.speed=location.getSpeed();
        pos.date.setTime(location.getTimestamp());
        receiver.receivePosItion(pos);
//        System.out.println("got pos " + pos.latitude + " " + pos.longitude);

	}

	public void providerStateChanged(LocationProvider arg0, int arg1) {
		if (LocationProvider.AVAILABLE != arg1){
			receiver.receiveMessage("provider stopped");
			close();
		}
	}
	
	public void close() {
		if (locationProvider != null){
			locationProvider.reset();
			
		}
		locationProvider=null;
		receiver.locationDecoderEnd();
	}

}
