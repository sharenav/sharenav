package net.sharenav.sharenav.importexport;

import javax.microedition.lcdui.Displayable;

import net.sharenav.midlet.ui.UploadListener;

public interface GpxImportSession {
	public void initImportServer(UploadListener feedback, float maxDistance, Displayable parent);

}
