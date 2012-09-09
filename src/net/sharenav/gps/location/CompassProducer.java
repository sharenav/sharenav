package net.sharenav.gps.location;

import java.io.OutputStream;

public interface CompassProducer {
	public boolean init(CompassReceiver receiver);
	public boolean activate(CompassReceiver receiver);
	public boolean deactivate(CompassReceiver receiver);
	public void addCompassReceiver(CompassReceiver rec); 
	public boolean removeCompassReceiver(CompassReceiver rec);
	public void enableRawLogging(OutputStream os);
	public void disableRawLogging();
	public void triggerPositionUpdate();
	public void triggerLastKnownPositionUpdate();
	public void close();

}