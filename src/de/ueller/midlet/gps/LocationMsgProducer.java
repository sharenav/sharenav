package de.ueller.midlet.gps;

import java.io.OutputStream;

public interface LocationMsgProducer {
	public boolean init(LocationMsgReceiver receiver);
	public void addLocationMsgReceiver(LocationMsgReceiver rec); 
	public boolean removeLocationMsgReceiver(LocationMsgReceiver rec);
	public void enableRawLogging(OutputStream os);
	public void disableRawLogging();
	public void close();

}