package de.ueller.midlet.gps.importexport;

import java.io.InputStream;

import de.ueller.midlet.gps.data.Gpx;

public interface GpxParser {
	public boolean parse(InputStream in, float maxDistance, Gpx gpx);
	public String getMessage();

}
