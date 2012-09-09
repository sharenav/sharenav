package net.sharenav.gps.location;

import java.io.OutputStream;

public interface LocationMsgProducer {
	public boolean init(LocationMsgReceiver receiver);
	public boolean activate(LocationMsgReceiver receiver);
	public boolean deactivate(LocationMsgReceiver receiver);
	public void addLocationMsgReceiver(LocationMsgReceiver rec); 
	public boolean removeLocationMsgReceiver(LocationMsgReceiver rec);
	public void enableRawLogging(OutputStream os);
	public void disableRawLogging();
	public void triggerPositionUpdate();
	public void triggerLastKnownPositionUpdate();
	public void close();

}