package net.sharenav.sharenav.importexport;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.CommConnection;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class CommExportSession implements ExportSession {
	
	private final static Logger logger=Logger.getInstance(CommExportSession.class,Logger.DEBUG);
	private Connection session = null;

	public void closeSession() {
		// TODO Auto-generated method stub

	}

	public OutputStream openSession(String url, String name) {
		OutputStream oS = null;		
		try {			
			session = Connector.open(url);			
			CommConnection commCon = (CommConnection) session;			
			oS = commCon.openOutputStream();			
		} catch (IOException e) {
			logger.error(Locale.get("commexportsession.CouldNotObtainConnectionWith")/*Could not obtain connection with */ + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}		
		return oS;
	}

}
