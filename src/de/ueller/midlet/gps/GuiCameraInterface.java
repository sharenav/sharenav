package de.ueller.midlet.gps;

import de.ueller.gpsmid.data.Configuration;

public interface GuiCameraInterface {
	
	// FIXME Maybe can be simplifies with setup/init GpsMidDiscoverable
	public void init(Trace parent);
	public void show();
	public void setup(GuiDiscover parent);

}
