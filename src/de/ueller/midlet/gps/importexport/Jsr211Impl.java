/*
 * GpsMid - Copyright (c) 2009 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package de.ueller.midlet.gps.importexport;

//#if polish.api.contenthandler
import javax.microedition.content.ContentHandler;
import javax.microedition.content.ContentHandlerException;
import javax.microedition.content.ContentHandlerServer;
import javax.microedition.content.Invocation;
import javax.microedition.content.Registry;
import javax.microedition.content.RequestListener;

import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.PositionMark;

public class Jsr211Impl implements Jsr211ContentHandlerInterface,
		RequestListener

{

	private final static Logger logger = Logger.getInstance(Jsr211Impl.class,
			Logger.DEBUG);

	Invocation invoc;
	ContentHandlerServer handlerServer;

	public void registerContentHandler() {
		// register yourself
		try {
			logger.info("Registering Content handler");
			Registry reg = Registry.getRegistry(GpsMid.class.getName());

			// Create a content handler instance for the
			// Generic PNG Handler
			String[] chTypes = { "custom/gpslocation" };
			String[] chSuffixes = { ".gpsloc" };
			String[] chActions = { ContentHandler.ACTION_OPEN };

			ContentHandler handler = reg.register(GpsMid.class.getName(),
					chTypes, chSuffixes, chActions, null, null, null);

			try {
				handlerServer = Registry.getServer(GpsMid.class.getName());
				handlerServer.setListener(this);
			} catch (ContentHandlerException che) {
				logger.exception("Could not retriever ContentHandlerServer",
						che);
			}

			logger.info("Registered Content handler " + handler);

		} catch (ContentHandlerException e) {
			logger.exception("Failed to register Contenthandler", e);
		} catch (SecurityException e) {
			logger.exception(
					"Sorry, was not permitted to register Contenthandler", e);
		} catch (IllegalArgumentException e) {
			logger.exception(
					"Failed to register Contenthandler due to wrong arguments",
					e);
		} catch (ClassNotFoundException e) {
			logger.exception("Failed to find Contenthandler", e);
		}

	}

	public void invocationRequestNotify(ContentHandlerServer handler) {
		// Dequeue the next invocation
		invoc = handler.getRequest(false);
		if (invoc != null) {
			Trace trace = Trace.getInstance();
			if (trace != null) {
				logger
						.error(
								"Failed to handle content handler invocation as trace was null",
								true);
			}
			// args should be 2 coordinates and one name
			String[] coords = invoc.getArgs();
			if (coords.length != 3) {
				logger
						.error(
								"Received an invalid content handler invocation. Incorrect number of arguments",
								true);
				return;
			}
			String lat = coords[0];
			String lon = coords[1];
			String name = coords[2];

			// go and display this...
			try {
				float latf = Float.parseFloat(lat);
				float lonf = Float.parseFloat(lon);
				PositionMark pm = new PositionMark(latf, lonf);
				pm.displayName = name;
				trace.gpx.addWayPt(pm);
				trace.receivePosItion(latf, lonf, 1500);
				trace.show();
			} catch (NumberFormatException nfe) {
				logger
						.error("Content handler invocation contained invalid coordinates ("
								+ lat + "|" + lon + ")");
			}
		}

	}

}
//#endif