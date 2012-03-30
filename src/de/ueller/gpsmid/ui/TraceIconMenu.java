/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See file COPYING
 */

package de.ueller.gpsmid.ui;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.midlet.iconmenu.IconActionPerformer;
import de.ueller.midlet.iconmenu.IconMenuPage;
import de.ueller.midlet.iconmenu.IconMenuWithPagesGui;
import de.ueller.midlet.iconmenu.LayoutElement;

import javax.microedition.lcdui.Graphics;

import de.enough.polish.util.Locale;


public class TraceIconMenu extends IconMenuWithPagesGui {

	LayoutElement iconToggleGps;
	LayoutElement iconToggleTrackRec;
	LayoutElement iconPauseTrackRec;
	LayoutElement iconToggleAudioRec;
	LayoutElement iconToggleRoute; 
	LayoutElement iconOnlineInfo; 
	LayoutElement iconAddPOI;
	LayoutElement iconEditPOI;
	LayoutElement iconAddAddr;
	LayoutElement iconEditWay;
	LayoutElement iconHelpOnlineTouch;
	LayoutElement iconHelpOnlineWiki;
	
	private static int rememberedEleId = 0;
	private static int rememberedTabNr = 0;
	
	
	public TraceIconMenu(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;
		// Main
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.MainTop")/* Main */, 3, 4);
		iconToggleGps =		mp.createAndAddIcon(Locale.get("traceiconmenu.StartGPS")/*Start GPS*/, "i_gps", Trace.CONNECT_GPS_CMD);
		iconToggleGps.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		mp.createAndAddIcon(Locale.get("generic.Search")/*Search*/, "i_search", Trace.SEARCH_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.MapFeature")/*Map Features*/, "i_mapfeat", Trace.MAPFEATURES_CMD);
		
		mp.createAndAddIcon(Locale.get("traceiconmenu.Setup")/*Setup*/, "i_setup", Trace.SETUP_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.Tacho")/*Tacho*/, "i_tacho", Trace.DATASCREEN_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.Overview")/*Overview/Filter Map*/, "i_overview", Trace.OVERVIEW_MAP_CMD);
		
		iconOnlineInfo =	mp.createAndAddIcon(Locale.get("traceiconmenu.Online")/*Online*/, "i_online", Trace.ONLINE_INFO_CMD);		
		mp.createAndAddIcon(Locale.get("generic.About")/*About*/, "i_about", Trace.ABOUT_CMD);
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);

		mp.createAndAddIcon(Locale.get("generic.Exit")/*Exit*/, "i_exit", Trace.EXIT_CMD);

		// determine preferred ordering
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
			createAndAddRoutingMenu();
			createAndAddRecordingMenu();
		} else {
			createAndAddRecordingMenu();			
			createAndAddRoutingMenu();
		}
		// Osm
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.OsmTop")/* Osm */, 3, 3);
		iconEditWay =		mp.createAndAddIcon(Locale.get("traceiconmenu.EditWay")/*Edit way*/, "i_editway", Trace.RETRIEVE_XML);
		iconEditPOI =		mp.createAndAddIcon(Locale.get("traceiconmenu.EditPOI")/*Edit POI*/, "i_addpoi", Trace.EDIT_ENTITY);
		iconAddPOI =		mp.createAndAddIcon(Locale.get("traceiconmenu.AddPOI")/*Add POI*/, "i_addpoi", Trace.RETRIEVE_NODE);
		iconAddAddr =		mp.createAndAddIcon(Locale.get("traceiconmenu.AddAddr")/*Add Address*/, "i_addpoi", Trace.EDIT_ADDR_CMD);
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);

		//#if not polish.api.online
		iconOnlineInfo.makeImageGreyed();
		iconAddPOI.makeImageGreyed();
		iconEditWay.makeImageGreyed();
		iconAddAddr.makeImageGreyed();
		//#endif

		//#if not polish.api.osm-editing
		iconEditWay.makeImageGreyed();
		//#endif
		
		createAndAddHelpMenu();

		setActiveTabAndCursor(rememberedTabNr, rememberedEleId);
	}
	
	private void createAndAddRecordingMenu() {
		IconMenuPage mp;
		// Recordings
		mp = createAndAddMenuPage((this.getWidth() >= 176) 
				? Locale.get("traceiconmenu.RecordTop")/* Recordings */
				: Locale.get("traceiconmenu.RecTop")/* Rec */, 3, 4);
		
		iconToggleTrackRec = mp.createAndAddIcon(Locale.get("traceiconmenu.RecordTrack"), 
				"i_rectrack", Trace.START_RECORD_CMD);
		iconToggleTrackRec.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		
		iconPauseTrackRec = mp.createAndAddIcon(Locale.get("traceiconmenu.SuspendRec"), 
			"i_rectrack_pause", Trace.TOGGLE_RECORDING_SUSP_CMD);
		iconPauseTrackRec.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		iconPauseTrackRec.setToggleImageName("i_rectrack0");
		
		mp.createAndAddIcon(Locale.get("traceiconmenu.SaveWpt")/*Save Wpt*/, 
				"i_savewpt", Trace.SAVE_WAYP_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.EnterWpt")/*Enter Wpt*/, 
				"i_enterwpt", Trace.ENTER_WAYP_CMD);
		
		mp.createAndAddIcon(Locale.get("traceiconmenu.Tracks")/*Tracks*/, 
				"i_tracks", Trace.MANAGE_TRACKS_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.Waypoints")/*Waypoints*/, 
				"i_wpts", Trace.MANAGE_WAYP_CMD);
		
		mp.createAndAddIcon(Locale.get("traceiconmenu.Photo")/*Photo*/, 
				"i_photo", Trace.CAMERA_CMD);
		iconToggleAudioRec=	mp.createAndAddIcon(Locale.get("traceiconmenu.Voice")/*Voice*/, 
				"i_micro", Trace.TOGGLE_AUDIO_REC);
		iconToggleAudioRec.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		
		mp.createAndAddIcon(Locale.get("traceiconmenu.SendSMS")/*Send SMS*/, 
				"i_sendsms", Trace.SEND_MESSAGE_CMD);
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, 
				"i_back", IconActionPerformer.BACK_ACTIONID);
	}

	private void createAndAddRoutingMenu() {
		IconMenuPage mp;
		// Route
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.RoutePage")/* Route */, 3, 3);
		iconToggleRoute = mp.createAndAddIcon(Locale.get("traceiconmenu.Calc")/*Calculate*/, "i_calc", Trace.ROUTING_TOGGLE_CMD);
		iconToggleRoute.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		mp.createAndAddIcon(Locale.get("traceiconmenu.SetDest")/*Set dest*/, "i_setdest", Trace.SET_DEST_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.ShowDest")/*Show dest*/, "i_showdest", Trace.SHOW_DEST_CMD);		
		mp.createAndAddIcon(Locale.get("traceiconmenu.ClearDest")/*Clear dest*/, "i_cleardest", Trace.CLEAR_DEST_CMD);		
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);
	}

	private void createAndAddHelpMenu() {
		IconMenuPage mp;
		// Route
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.HelpPage")/* Help */, 3, 3);

		iconHelpOnlineTouch = mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Touch")/*Touch*/, "i_online", Trace.HELP_ONLINE_TOUCH_CMD);
		iconHelpOnlineWiki = mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Wiki")/*Wiki*/, "i_online", Trace.HELP_ONLINE_WIKI_CMD);
		mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Keys")/*Keys*/, "is_keys", Trace.KEYS_HELP_CMD);
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);
	}

	private void rememberActiveTabAndEleNr() {
		rememberedTabNr = tabNr;
		rememberedEleId = getActiveMenuPage().rememberEleId;
	}
	
	public void paint(Graphics g) {
		rememberActiveTabAndEleNr();
		
		Trace trace = Trace.getInstance();
		// for commands that can be toggled, fill in the current text and/or corresponding actionId before painting
		boolean gpsOn = trace.isGpsConnected();
		iconToggleGps.setText( gpsOn ? Locale.get("traceiconmenu.StopGPS")/*Stop GPS*/ 
				: Locale.get("traceiconmenu.StartGPS")/*Start GPS*/);
		iconToggleGps.setActionID( gpsOn ? Trace.DISCONNECT_GPS_CMD : Trace.CONNECT_GPS_CMD);
		iconToggleGps.setImageToggleState( !gpsOn );
		
		boolean recOn = trace.gpx.isRecordingTrk();
		iconToggleTrackRec.setText( recOn ? Locale.get("traceiconmenu.StopRec")/*Stop Rec*/ 
				: Locale.get("traceiconmenu.RecordTrack")/*Record Track*/);
		iconToggleTrackRec.setActionID( recOn ? Trace.STOP_RECORD_CMD : Trace.START_RECORD_CMD);
		iconToggleTrackRec.setImageToggleState( !recOn );

		boolean recRunning = (trace.gpx.isRecordingTrkSuspended() == false);
		iconPauseTrackRec.setText( recRunning ? Locale.get("traceiconmenu.SuspendRec")/*Suspend Rec*/ 
				: Locale.get("traceiconmenu.ResumeRec")/*Resume Rec*/);
		iconPauseTrackRec.setImageToggleState( !recRunning );
		if (recOn) {
			iconPauseTrackRec.makeImageColored();
		} else {
			iconPauseTrackRec.makeImageGreyed();
		}

		boolean audioOn = trace.audioRec.isRecording();
		iconToggleAudioRec.setText( audioOn ? Locale.get("traceiconmenu.StopVoice")/*Stop VoiceRec*/ 
				: Locale.get("traceiconmenu.Voice")/*Voice*/);
		iconToggleAudioRec.setImageToggleState( !audioOn );
		
		boolean routeOn = (trace.route != null || trace.routeCalc);
		iconToggleRoute.setText( routeOn ? Locale.get("traceiconmenu.StopRoute")/*Stop Route*/ 
				: Locale.get("traceiconmenu.Calc")/*Calculate*/);				
		iconToggleRoute.setImageToggleState( !routeOn );
				
		super.paint(g);
	}
}
