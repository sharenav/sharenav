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
	
	private boolean optimizedForRouting = false;
	
	public TraceIconMenu(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;
		// Main
		mp = createAndAddMenuPage(" Main ", 3, 4);
		iconToggleGps =		mp.createAndAddIcon("Start GPS", "i_gps", Trace.CONNECT_GPS_CMD);
							mp.createAndAddIcon("Search", "i_search", Trace.SEARCH_CMD);
							mp.createAndAddIcon("Map Features", "i_mapfeat", Trace.MAPFEATURES_CMD);

							mp.createAndAddIcon("Setup", "i_setup", Trace.SETUP_CMD);
							mp.createAndAddIcon("Tacho", "i_tacho", Trace.DATASCREEN_CMD);
							mp.createAndAddIcon("Overview/Filter Map", "i_overview", Trace.OVERVIEW_MAP_CMD);
		
		iconOnlineInfo =	mp.createAndAddIcon("Online", "i_online", Trace.ONLINE_INFO_CMD);		
							mp.createAndAddIcon("About", "i_about", Trace.ABOUT_CMD);
							mp.createAndAddIcon("Back", "i_back", IconActionPerformer.BACK_ACTIONID);

							mp.createAndAddIcon("Exit", "i_exit", Trace.EXIT_CMD);

		// determine preferred ordering
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
			createAndAddRoutingMenu();
			createAndAddRecordingMenu();
		} else {
			createAndAddRecordingMenu();			
			createAndAddRoutingMenu();
		}
		// Osm
		mp = createAndAddMenuPage(" Osm ", 3, 4);
		iconEditWay =		mp.createAndAddIcon("Edit way", "i_editway", Trace.RETRIEVE_XML);
		iconAddPOI =		mp.createAndAddIcon("Add POI", "i_addpoi", Trace.RETRIEVE_NODE);

		//#if not polish.api.online
		iconOnlineInfo.makeImageGreyed();
		iconAddPOI.makeImageGreyed();
		iconEditWay.makeImageGreyed();
		//#endif

		//#if not polish.api.osm-editing
		iconEditWay.makeImageGreyed();
		//#endif
	}
	
	
	private void createAndAddRecordingMenu() {
		IconMenuPage mp;
		// Recordings
		mp = createAndAddMenuPage(this.getWidth() >= 176 ?" Recordings ":" Rec ", 3, 4);
		iconToggleTrackRec=	mp.createAndAddIcon("Rec Track", "i_rectrack", Trace.START_RECORD_CMD);
							mp.createAndAddIcon("Save Wpt", "i_savewpt", Trace.SAVE_WAYP_CMD);
							mp.createAndAddIcon("Enter Wpt", "i_enterwpt", Trace.ENTER_WAYP_CMD);

							mp.createAndAddIcon("Tracks", "i_tracks", Trace.MANAGE_TRACKS_CMD);
							mp.createAndAddIcon("Waypoints", "i_wpts", Trace.MAN_WAYP_CMD);

							mp.createAndAddIcon("Photo", "i_photo", Trace.CAMERA_CMD);
		iconToggleAudioRec=	mp.createAndAddIcon("Voice", "i_micro", Trace.TOGGLE_AUDIO_REC);
		
							mp.createAndAddIcon("Send SMS", "i_sendsms", Trace.SEND_MESSAGE_CMD);		
	}


	private void createAndAddRoutingMenu() {
		IconMenuPage mp;
		// Route
		mp = createAndAddMenuPage(" Route ", 3, 4);
		iconToggleRoute=	mp.createAndAddIcon("Calculate", "i_calc", Trace.ROUTING_TOGGLE_CMD);
							mp.createAndAddIcon("Set target", "i_settarget", Trace.SETTARGET_CMD);
							mp.createAndAddIcon("Clear target", "i_cleartarget", Trace.CLEARTARGET_CMD);		
	}


	public void paint(Graphics g) {
		Trace trace = Trace.getInstance();
		// for commands that can be toggled, fill in the current text and/or corresponding actionId before painting
		iconToggleGps.setText( trace.isGpsConnected() ? "Stop GPS" : "Start GPS");
		iconToggleGps.setActionID( trace.isGpsConnected() ? Trace.DISCONNECT_GPS_CMD : Trace.CONNECT_GPS_CMD);

		iconToggleTrackRec.setText( trace.gpx.isRecordingTrk() ? "Stop Rec" : "Rec Track");
		iconToggleTrackRec.setActionID( trace.gpx.isRecordingTrk() ? Trace.STOP_RECORD_CMD : Trace.START_RECORD_CMD);
		
		iconToggleAudioRec.setText( trace.audioRec.isRecording() ? "Stop VoiceRec" : "Voice");
		
		iconToggleRoute.setText( (trace.route != null || trace.routeCalc) ? "Stop Route" : "Calculate");				

		super.paint(g);
	}
	
	
}