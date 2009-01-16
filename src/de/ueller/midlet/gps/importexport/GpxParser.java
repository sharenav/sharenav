package de.ueller.midlet.gps.importexport;

import java.io.InputStream;

import de.ueller.midlet.gps.UploadListener;
import de.ueller.midlet.gps.data.Gpx;

public interface GpxParser {
	public boolean parse(InputStream in, float maxDistance, Gpx gpx, UploadListener ul);
	public String getMessage();

}
