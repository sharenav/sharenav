package de.ueller.gpsmid.importexport;

import javax.microedition.lcdui.Displayable;

import de.ueller.midlet.ui.UploadListener;

public interface GpxImportSession {
	public void initImportServer(UploadListener feedback, float maxDistance, Displayable parent);

}
