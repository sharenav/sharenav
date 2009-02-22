package de.ueller.midlet.gps.importexport;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */

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
	
	private final static Logger logger = Logger.getInstance(FileGpxImportSession.class, Logger.DEBUG);
	float maxDistance;
	UploadListener feedbackListener;
	Displayable parent;

	public void initImportServer(UploadListener feedback, float maxDistance, Displayable parent) {
		this.maxDistance = maxDistance;
		this.parent = parent;
		feedbackListener = feedback;
		
		//#if polish.api.fileconnectionapi
		FsDiscover fsd = new FsDiscover(this, this, Configuration.getGpxUrl(), 
										false, ".gpx", "Load *.gpx file");
		fsd.show();				
		//#endif
	}

	public void selectionCanceled() {
		/**
		 * Nothing to do here at the moment
		 */
	}

	public void selectedFile(final String url) {
		/**
		 * This method gets called from the UI thread,
		 * so we need to perform the slow, potentially blocking,
		 * operation of opening the input in a separate thread,
		 * even though receiveGpx already uses its own thread
		 * to perform the actual GPX parsing.
		 * 
		 * TODO: can we consolidate the two threads?
		 */
		Thread importThread = new Thread(new Runnable() {
			public void run()
			{
				try {
					//#debug info
					logger.info("Receiving gpx: " + url);
					Connection conn = Connector.open(url, Connector.READ);
					if (conn instanceof InputConnection) {
						InputConnection inConn = ((InputConnection)conn);
						Trace.getInstance().gpx.receiveGpx(inConn.openInputStream(), feedbackListener, maxDistance);
						return;
					}
					logger.error("Unknown url type to load from: " + url);
				} catch (IOException e) {
					logger.exception("Could not open GPX file for import",e);
				} catch (SecurityException se) {
					logger.silentexception("Gpx file import was not allowed", se);
				}
				
			}
		} );
		importThread.setPriority(Thread.MIN_PRIORITY);
		importThread.start();
	}

	public void show() {
		GpsMid.getInstance().show(parent);		
	}

}
