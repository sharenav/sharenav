package de.ueller.midlet.gps;

import java.io.InputStream;
import java.io.OutputStream;

public interface LocationMsgProducer {	
	public void init(InputStream is, OutputStream os, LocationMsgReceiver receiver);
	public void enableRawLogging(OutputStream os);
	public void disableRawLogging();
	public void close();

}