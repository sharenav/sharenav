package de.ueller.midlet.gps.importexport;

import java.io.IOException;
import java.io.OutputStream;

//#if polish.api.fileconnection
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif

import de.ueller.midlet.gps.Logger;

public class FileExportSession implements ExportSession {
	private final static Logger logger = Logger.getInstance(FileExportSession.class,Logger.DEBUG);
	//#if polish.api.fileconnection
	private Connection session = null;
	//#endif

	public void closeSession() {
		//#if polish.api.fileconnection 
		try {
			session.close();			
		} catch (IOException e) {
			logger.error("Failed to close connection after storing to file");
			e.printStackTrace();
		}
		//#endif
	}

	public OutputStream openSession(String url, String name) {
		OutputStream oS = null;
		//#if polish.api.fileconnection
		try {
			url += name + ".gpx";
			//#debug info
			logger.info("Opening file " + url);
			session = Connector.open(url);
			FileConnection fileCon = (FileConnection) session;
			if (fileCon == null)
				throw new IOException("Couldn't open url " + url);
			if (!fileCon.exists()) {
				fileCon.create();
			} else {
				fileCon.truncate(0);
			}
			
			oS = fileCon.openOutputStream();
		} catch (IOException e) {
			logger.error("Could not obtain connection with " + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}
		//#endif
		return oS;
	}

}
