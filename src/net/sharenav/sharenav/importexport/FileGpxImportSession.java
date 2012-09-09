/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.sharenav.importexport;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
import javax.microedition.lcdui.Displayable;
//#if polish.api.fileconnection
import javax.microedition.io.file.FileConnection;
//#endif

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.ui.FsDiscover;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.sharenav.ui.ShareNavDisplayable;
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.midlet.ui.SelectionListener;
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class FileGpxImportSession implements GpxImportSession, SelectionListener, ShareNavDisplayable {
	
	private final static Logger logger = Logger.getInstance(FileGpxImportSession.class, Logger.DEBUG);
	float maxDistance;
	UploadListener feedbackListener;
	Displayable parent;

	public void initImportServer(UploadListener feedback, float maxDistance, Displayable parent) {
		this.maxDistance = maxDistance;
		this.parent = parent;
		feedbackListener = feedback;
		
		//#if polish.api.fileconnectionapi
		// if gpxUrl is export to OSM or perhaps bluetooth, use local file as GPX url as the menu entry is "load GPX from file"
		FsDiscover fsd = new FsDiscover(this, this, Configuration.getGpxUrl() == null
						|| Configuration.getGpxUrl().startsWith("file:") ? Configuration.getGpxUrl() :
						null, 
						FsDiscover.CHOOSE_FILEONLY, ".gpx", Locale.get("filegpximportsession.LoadGgpxFile")/*Load *.gpx file*/);
		fsd.show();				
		//#endif
	}

	public void selectionCanceled() {
		/**
		 * Nothing to do here at the moment
		 */
	}

	public void selectedFile(final String url) {
		//#if polish.api.fileconnection
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
					FileConnection fc = (FileConnection) Connector.open(url, Connector.READ);
					Trace.getInstance().gpx.receiveGpx(fc.openInputStream(), 
							feedbackListener, maxDistance);
					return;
					//TODO: remove this string from translations in messages_*.txt
					//logger.error(Locale.get("filegpximportsession.UnknownUrlType")/*Unknown url type to load from: */ + url);
				} catch (IOException e) {
					logger.exception(Locale.get("filegpximportsession.CouldNotOpenGPXImport")/*Could not open GPX file for import*/,e);
				} catch (SecurityException se) {
					logger.silentexception("Gpx file import was not allowed", se);
				}
				
			}
		} );
		importThread.setPriority(Thread.MIN_PRIORITY);
		importThread.start();
		//#endif
	}

	public void show() {
		ShareNav.getInstance().show(parent);		
	}

}
