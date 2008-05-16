package de.ueller.midlet.gps;

import java.io.InputStream;

public interface LocationMsgProducer {	
	public void init(InputStream is, LocationMsgReceiver receiver);
	public void close();

}