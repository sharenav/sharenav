package net.sharenav.sharenav.importexport;

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
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

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
				logger.error(Locale.get("obexexportsession.UnsuccessfulReturnCodeInObexPush")/*Unsuccessful return code in Obex push: */ + code);
			}
		} catch (IOException e) {
			logger.error(Locale.get("obexexportsession.FailedClosingConnectionAfterGPX")/*Failed to close connection after transmitting GPX*/);
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
			logger.error(Locale.get("obexexportsession.CouldNotObtainConnectionWith")/*Could not obtain connection with */ + url + " (" + e.getMessage() + ")");
			e.printStackTrace();
		}
		//#else
		logger.fatal(Locale.get("obexexportsession.NoObexSupport")/*This version does not support OBEX over bluetooth, so we can not send files*/);		
		//#endif
		return oS;
	}

}
