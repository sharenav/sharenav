package net.sharenav.sharenav.ui;

import net.sharenav.sharenav.data.Configuration;

public interface GuiCameraInterface {
	
	// FIXME Maybe can be simplifies with setup/init ShareNavDiscoverable
	public void init(Trace parent);
	public void show();
	public void setup(GuiDiscover parent);

}
