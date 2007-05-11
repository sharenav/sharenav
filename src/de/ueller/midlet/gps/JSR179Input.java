package de.ueller.midlet.gps;

import java.util.Date;

import javax.microedition.location.Coordinates;
import javax.microedition.location.Criteria;
import javax.microedition.location.Location;
import javax.microedition.location.LocationException;
import javax.microedition.location.LocationListener;
import javax.microedition.location.LocationProvider;

import de.ueller.gps.data.Position;
import de.ueller.gps.sirf.LocationMsgReceiver;

public class JSR179Input implements LocationListener {

    /** location provider */
    private LocationProvider locationProvider = null;
	private final LocationMsgReceiver receiver;
	Date date=new Date();
	Position pos=new Position(0f,0f,0f,0f,0f,0,date);
	

	public JSR179Input(LocationMsgReceiver receiver) throws Exception{
		this.receiver = receiver;
		createLocationProvider();
	}
	
    /**
     * Initializes LocationProvider
     * uses default criteria
     * @throws Exception 
     */
    void createLocationProvider() throws Exception {
        if (locationProvider == null) {
            Criteria criteria = new Criteria();

            try {
                locationProvider = LocationProvider.getInstance(criteria);
            } catch (LocationException le) {
                System.out.println("Cannot create LocationProvider for this criteria.");
                throw new Exception("no LocationProvider");
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
        System.out.println("got pos " + pos.latitude + " " + pos.longitude);

	}

	public void providerStateChanged(LocationProvider arg0, int arg1) {
		// TODO Auto-generated method stub

	}
	
	public void destroy(){
		if (locationProvider != null){
			locationProvider.reset();
		}
		locationProvider=null;
	}

}
