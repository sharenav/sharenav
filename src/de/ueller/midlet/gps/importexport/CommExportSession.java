package de.ueller.midlet.gps.importexport;

import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.CommConnection;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;

import de.ueller.midlet.gps.Logger;

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
			logger.error("Could not obtain connection with " + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}		
		return oS;
	}

}
