package de.ueller.midlet.gps.importexport;

import javax.microedition.lcdui.Displayable;
import de.ueller.midlet.gps.UploadListener;

public interface GpxImportSession {
	public void initImportServer(UploadListener feedback, float maxDistance, Displayable parent);

}
