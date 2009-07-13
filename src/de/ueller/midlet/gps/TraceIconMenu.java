/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.gps.tools.IconMenuWithPagesGUI;
import de.ueller.gps.tools.IconMenuPage;
import de.ueller.gps.tools.LayoutElement;

import javax.microedition.lcdui.Graphics;


public class TraceIconMenu extends IconMenuWithPagesGUI {

	LayoutElement iconToggleGps; 
	LayoutElement iconToggleTrackRec; 
	LayoutElement iconToggleAudioRec; 
	LayoutElement iconToggleRoute; 
	LayoutElement iconOnlineInfo; 
	LayoutElement iconAddPOI; 
	LayoutElement iconEditWay; 
	
	public TraceIconMenu(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;
		// Main
		mp = createAndAddMenuPage(" Main ", 3, 4);
		iconToggleGps =		mp.createAndAddIcon("Start GPS", "satelit", Trace.CONNECT_GPS_CMD);
							mp.createAndAddIcon("Search", "cinema", Trace.SEARCH_CMD);
							mp.createAndAddIcon("Overview", "city", Trace.OVERVIEW_MAP_CMD);

							mp.createAndAddIcon("Setup", "taxi", Trace.SETUP_CMD);
							mp.createAndAddIcon("Map Features", "museum", Trace.MAPFEATURES_CMD);
							mp.createAndAddIcon("Tacho", "fuel", Trace.DATASCREEN_CMD);
		
		iconOnlineInfo =	mp.createAndAddIcon("Online", "left", Trace.ONLINE_INFO_CMD);		
							mp.createAndAddIcon("About", "GpsMid", Trace.ABOUT_CMD);
							mp.createAndAddIcon("Back", "recycling", Trace.BACK_CMD);

							mp.createAndAddIcon("Exit", "tunnel_end", Trace.EXIT_CMD);

		// Recordings
		mp = createAndAddMenuPage(this.getWidth() >= 176 ?" Recordings ":" Rec ", 3, 4);
							mp.createAndAddIcon("Tracks", "restaurant", Trace.MANAGE_TRACKS_CMD);
							mp.createAndAddIcon("Waypoints", "mark", Trace.MAN_WAYP_CMD);
							mp.createAndAddIcon("Save Wpt", "target", Trace.SAVE_WAYP_CMD);

		iconToggleTrackRec=	mp.createAndAddIcon("Rec Track", "target", Trace.START_RECORD_CMD);
							mp.createAndAddIcon("TakePic", "museum", Trace.CAMERA_CMD);
		iconToggleAudioRec=	mp.createAndAddIcon("AudioRec", "pub", Trace.TOGGLE_AUDIO_REC);
		
							mp.createAndAddIcon("Send SMS", "telephone", Trace.SEND_MESSAGE_CMD);
							mp.createAndAddIcon("Enter Wpt", "target", Trace.ENTER_WAYP_CMD);
		
		// Route
		mp = createAndAddMenuPage(" Route ", 3, 4);
		iconToggleRoute=	mp.createAndAddIcon("Calculate", "motorway", Trace.ROUTING_TOGGLE_CMD);
							mp.createAndAddIcon("Set target", "target", Trace.SETTARGET_CMD);
							mp.createAndAddIcon("Clear target", "parking", Trace.CLEARTARGET_CMD);

		// Osm
		mp = createAndAddMenuPage(" Osm ", 3, 4);
		iconEditWay =		mp.createAndAddIcon("Edit way", "motorway", Trace.RETRIEVE_XML);
		iconAddPOI =		mp.createAndAddIcon("Add POI", "unknown", Trace.RETRIEVE_NODE);

		//#if not polish.api.online
		iconOnlineInfo.makeImageGreyed();
		iconAddPOI.makeImageGreyed();
		iconEditWay.makeImageGreyed();
		//#endif

		//#if not polish.api.osm-editing
		iconEditWay.makeImageGreyed();
		//#endif
	}
	
	public void paint(Graphics g) {
		Trace trace = Trace.getInstance();
		// for commands that can be toggled, fill in the current text and/or corresponding actionId before painting
		iconToggleGps.setText( trace.isGpsConnected() ? "Stop GPS" : "Start GPS");
		iconToggleGps.setActionID( trace.isGpsConnected() ? Trace.DISCONNECT_GPS_CMD : Trace.CONNECT_GPS_CMD);

		iconToggleTrackRec.setText( trace.gpx.isRecordingTrk() ? "Stop Rec" : "Rec Track");
		iconToggleTrackRec.setActionID( trace.gpx.isRecordingTrk() ? Trace.STOP_RECORD_CMD : Trace.START_RECORD_CMD);
		
		iconToggleAudioRec.setText( trace.audioRec.isRecording() ? "Stop AudioRec" : "AudioRec");
		
		iconToggleRoute.setText( (trace.route != null || trace.routeCalc) ? "Stop Route" : "Calculate");				

		super.paint(g);
	}
	
	
}