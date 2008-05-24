package de.ueller.midlet.gps.importexport;

import java.io.IOException;
import java.io.OutputStream;


//#if polish.api.btapi
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;
//#endif
import de.ueller.midlet.gps.Logger;


public class ObexExportSession implements ExportSession {
	
	private final static Logger logger=Logger.getInstance(ObexExportSession.class,Logger.DEBUG);
	
	//#if polish.api.btapi
	private Connection session = null;
	private Operation operation = null;
	//#endif

	public void closeSession() {
		//#if polish.api.btapi		
		try {
			session.close();
			int code = operation.getResponseCode();
			if (code == ResponseCodes.OBEX_HTTP_OK) {				
				logger.info("Successfully transfered file");				
			} else {
				logger.error("Unsuccessful return code in Opex push: " + code);
			}
		} catch (IOException e) {
			logger.error("Failed to close connection after transmitting GPX");
			e.printStackTrace();
		}		
		//#endif
	}

	public OutputStream openSession(String url, String name) {
		OutputStream oS = null;
		//#if polish.api.btapi	
		try {			
			session = Connector.open(url);
			ClientSession csession = (ClientSession) session; 
			HeaderSet headers = csession.createHeaderSet();	        
			csession.connect(headers);
			//#debug debug
			logger.debug("Connected");
			headers.setHeader(HeaderSet.NAME, name + ".gpx");
			headers.setHeader(HeaderSet.TYPE, "text");
			
			operation = csession.put(headers);			
			oS = operation.openOutputStream();
		} catch (IOException e) {
			logger.error("Could not obtain connection with " + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}
		//#else
		logger.fatal("This version does not support OBEX over bluetooth, so we can't send files");		
		//#endif
		return oS;
	}

}
