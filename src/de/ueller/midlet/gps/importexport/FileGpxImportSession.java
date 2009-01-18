	package de.ueller.midlet.gps.importexport;

import java.io.IOException;

import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.lcdui.Displayable;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.FsDiscover;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.GuiGpxLoad;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.SelectionListener;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.UploadListener;

public class FileGpxImportSession implements GpxImportSession, SelectionListener, GpsMidDisplayable {
	
	private final static Logger logger=Logger.getInstance(FileGpxImportSession.class,Logger.DEBUG);
	float maxDistance;
	UploadListener feedbackListener;
	Displayable parent;

	public void initImportServer(UploadListener feedback, float maxDistance, Displayable parent) {
		this.maxDistance = maxDistance;
		this.parent = parent;
		feedbackListener = feedback;
		
		//#if polish.api.fileconnectionapi
		FsDiscover fsd = new FsDiscover(this,this,Configuration.getGpxUrl(),false,".gpx","Load *.gpx file");
		fsd.show();				
		//#endif
	}

	public void selectedFile(String url) {
		try {
			//#debug info
			logger.info("Receiving gpx: " + url);
			Connection c  = Connector.open(url,Connector.READ);			
			if (c instanceof InputConnection) {
				InputConnection inConn = ((InputConnection)c);				
				Trace.getInstance().gpx.receiveGpx(inConn.openInputStream(), feedbackListener, maxDistance);
				return;
			}
			logger.error("Unknown url type to load from: " + url);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void show() {
		GpsMid.getInstance().show(parent);		
	}

}
