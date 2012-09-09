package net.sharenav.sharenav.importexport;

import java.io.IOException;
import java.io.OutputStream;

//#if polish.api.fileconnection
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
//#endif

//#if polish.android
import java.io.File;
import java.io.FileOutputStream;
//#endif

import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class FileExportSession implements ExportSession {
	private final static Logger logger = Logger.getInstance(FileExportSession.class,Logger.DEBUG);
	//#if polish.android
	private File session = null;
	//#else
	//#if polish.api.fileconnection
	private Connection session = null;
	//#endif
	//#endif

	public void closeSession() {
		//#if polish.android
		//#else
		//#if polish.api.fileconnection 
		try {
			session.close();			
		} catch (IOException e) {
			logger.error(Locale.get("fileexportsession.FailedToCloseConnection")/*Failed to close connection after storing to file*/);
			e.printStackTrace();
		}
		//#endif
		//#endif
	}

	public OutputStream openSession(String url, String name) {
		OutputStream oS = null;
		//#if polish.android
		try {
			url += name + ".gpx";
			String filename = url.substring("file://".length());
			session = new File(filename);
			//#debug info
			logger.info("Opening file " + filename);
			if (session == null)
				throw new IOException("Couldn't open file " + filename);
			oS = new FileOutputStream(session);
		} catch (IOException e) {
			logger.error(Locale.get("fileexportsession.CouldNotObtainConnection")/*Could not obtain connection with */ + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}
		//#else
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
			logger.error(Locale.get("fileexportsession.CouldNotObtainConnection")/*Could not obtain connection with */ + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}
		//#endif
		//#endif
		return oS;
	}

}
