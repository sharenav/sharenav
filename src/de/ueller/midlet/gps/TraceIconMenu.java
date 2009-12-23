/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See COPYING
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
	LayoutElement iconAddAddr;
	LayoutElement iconEditWay;
	
	
	private static int rememberedEleId = 0;
	private static int rememberedTabNr = 0;
	
	
	private boolean optimizedForRouting = false;
	
	public TraceIconMenu(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;
		// Main
		mp = createAndAddMenuPage(" Main "/*i:MainTop*/, 3, 4);
		iconToggleGps =		mp.createAndAddIcon("Start GPS"/*i:StartGPS*/, "i_gps", Trace.CONNECT_GPS_CMD);
		iconToggleGps.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
							mp.createAndAddIcon("Search"/*i:Search*/, "i_search", Trace.SEARCH_CMD);
							mp.createAndAddIcon("Map Features"/*i:MapFeature*/, "i_mapfeat", Trace.MAPFEATURES_CMD);

							mp.createAndAddIcon("Setup"/*i:Setup*/, "i_setup", Trace.SETUP_CMD);
							mp.createAndAddIcon("Tacho"/*i:Tacho*/, "i_tacho", Trace.DATASCREEN_CMD);
							mp.createAndAddIcon("Overview/Filter Map"/*i:Overview*/, "i_overview", Trace.OVERVIEW_MAP_CMD);
		
		iconOnlineInfo =	mp.createAndAddIcon("Online"/*i:Online*/, "i_online", Trace.ONLINE_INFO_CMD);		
							mp.createAndAddIcon("About"/*i:About*/, "i_about", Trace.ABOUT_CMD);
							mp.createAndAddIcon("Back"/*i:Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);

							mp.createAndAddIcon("Exit"/*i:Exit*/, "i_exit", Trace.EXIT_CMD);

		// determine preferred ordering
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
			createAndAddRoutingMenu();
			createAndAddRecordingMenu();
		} else {
			createAndAddRecordingMenu();			
			createAndAddRoutingMenu();
		}
		// Osm
		mp = createAndAddMenuPage(" Osm "/*i:OsmTop*/, 3, 4);
		iconEditWay =		mp.createAndAddIcon("Edit way"/*i:EditWay*/, "i_editway", Trace.RETRIEVE_XML);
		iconAddPOI =		mp.createAndAddIcon("Add POI"/*i:AddPOI*/, "i_addpoi", Trace.RETRIEVE_NODE);
		iconAddAddr =		mp.createAndAddIcon("Add Address"/*i:AddAddr*/, "i_addpoi", Trace.EDIT_ADDR_CMD);

		//#if not polish.api.online
		iconOnlineInfo.makeImageGreyed();
		iconAddPOI.makeImageGreyed();
		iconEditWay.makeImageGreyed();
		iconAddAddr.makeImageGreyed();
		//#endif

		//#if not polish.api.osm-editing
		iconEditWay.makeImageGreyed();
		//#endif
		
		setActiveTabAndCursor(rememberedTabNr, rememberedEleId);
	}
	
	private void createAndAddRecordingMenu() {
		IconMenuPage mp;
		// Recordings
		mp = createAndAddMenuPage(this.getWidth() >= 176 ?" Recordings "/*i:RecordTop*/:" Rec "/*i:RecTop*/, 3, 4);
		iconToggleTrackRec=	mp.createAndAddIcon("Record Track", "i_rectrack", Trace.START_RECORD_CMD);
		iconToggleTrackRec.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
							mp.createAndAddIcon("Save Wpt"/*i:SaveWpt*/, "i_savewpt", Trace.SAVE_WAYP_CMD);
							mp.createAndAddIcon("Enter Wpt"/*i:EnterWpt*/, "i_enterwpt", Trace.ENTER_WAYP_CMD);

							mp.createAndAddIcon("Tracks"/*i:Tracks*/, "i_tracks", Trace.MANAGE_TRACKS_CMD);
							mp.createAndAddIcon("Waypoints"/*i:Waypoints*/, "i_wpts", Trace.MAN_WAYP_CMD);

							mp.createAndAddIcon("Photo"/*i:Photo*/, "i_photo", Trace.CAMERA_CMD);
		iconToggleAudioRec=	mp.createAndAddIcon("Voice"/*i:Voice*/, "i_micro", Trace.TOGGLE_AUDIO_REC);
		iconToggleAudioRec.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		
							mp.createAndAddIcon("Send SMS"/*i:SendSMS*/, "i_sendsms", Trace.SEND_MESSAGE_CMD);		
	}


	private void createAndAddRoutingMenu() {
		IconMenuPage mp;
		// Route
		mp = createAndAddMenuPage(" Route "/*i:RoutePage*/, 3, 4);
		iconToggleRoute=	mp.createAndAddIcon("Calculate"/*i:Calc*/, "i_calc", Trace.ROUTING_TOGGLE_CMD);
		iconToggleRoute.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
							mp.createAndAddIcon("Set dest"/*i:SetDest*/, "i_setdest", Trace.SET_DEST_CMD);
							mp.createAndAddIcon("Show dest"/*i:ShowDest*/, "i_showdest", Trace.SHOW_DEST_CMD);		
							mp.createAndAddIcon("Clear dest"/*i:ClearDest*/, "i_cleardest", Trace.CLEAR_DEST_CMD);		
	}

	private void rememberActiveTabAndEleNr() {
		rememberedTabNr = tabNr;
		rememberedEleId = getActiveMenuPage().rememberEleId;
	}
	

	public void paint(Graphics g) {
		rememberActiveTabAndEleNr();
		
		Trace trace = Trace.getInstance();
		// for commands that can be toggled, fill in the current text and/or corresponding actionId before painting
		boolean on = trace.isGpsConnected();
		iconToggleGps.setText( on ? "Stop GPS"/*i:StopGPS*/ : "Start GPS"/*i:StartGPS*/);
		iconToggleGps.setActionID( on ? Trace.DISCONNECT_GPS_CMD : Trace.CONNECT_GPS_CMD);
		iconToggleGps.setImageToggleState( !on );
		
		on = trace.gpx.isRecordingTrk();
		iconToggleTrackRec.setText( on ? "Stop Rec"/*i:StopRec*/ : "Record Track"/*i:RecordTrack*/);
		iconToggleTrackRec.setActionID( on ? Trace.STOP_RECORD_CMD : Trace.START_RECORD_CMD);
		iconToggleTrackRec.setImageToggleState( !on );
		
		on = trace.audioRec.isRecording();
		iconToggleAudioRec.setText( on ? "Stop VoiceRec"/*i:StopVoice*/ : "Voice"/*i:Voice*/);
		iconToggleAudioRec.setImageToggleState( !on );
		
		on = (trace.route != null || trace.routeCalc);
		iconToggleRoute.setText( on ? "Stop Route"/*i:StopRoute*/ : "Calculate"/*i:Calc*/);				
		iconToggleRoute.setImageToggleState( !on );
				
		super.paint(g);
	}
	
	
}
