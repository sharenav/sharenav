package de.ueller.midlet.gps.importexport;

import java.io.OutputStream;

public interface ExportSession {
	public OutputStream openSession(String url, String name);
	public void closeSession();

}
