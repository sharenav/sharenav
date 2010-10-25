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

import de.enough.polish.util.Locale;

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
	
	public TraceIconMenu(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;
		// Main
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.MainTop")/* Main */, 3, 4);
		iconToggleGps =		mp.createAndAddIcon(Locale.get("traceiconmenu.StartGPS")/*Start GPS*/, "i_gps", Trace.CONNECT_GPS_CMD);
		iconToggleGps.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		mp.createAndAddIcon(Locale.get("traceiconmenu.Search")/*Search*/, "i_search", Trace.SEARCH_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.MapFeature")/*Map Features*/, "i_mapfeat", Trace.MAPFEATURES_CMD);
		
		mp.createAndAddIcon(Locale.get("traceiconmenu.Setup")/*Setup*/, "i_setup", Trace.SETUP_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.Tacho")/*Tacho*/, "i_tacho", Trace.DATASCREEN_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.Overview")/*Overview/Filter Map*/, "i_overview", Trace.OVERVIEW_MAP_CMD);
		
		iconOnlineInfo =	mp.createAndAddIcon(Locale.get("traceiconmenu.Online")/*Online*/, "i_online", Trace.ONLINE_INFO_CMD);		
		mp.createAndAddIcon(Locale.get("traceiconmenu.About")/*About*/, "i_about", Trace.ABOUT_CMD);
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
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.OsmTop")/* Osm */, 3, 4);
		iconEditWay =		mp.createAndAddIcon(Locale.get("traceiconmenu.EditWay")/*Edit way*/, "i_editway", Trace.RETRIEVE_XML);
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
		
		setActiveTabAndCursor(rememberedTabNr, rememberedEleId);
	}
	
	private void createAndAddRecordingMenu() {
		IconMenuPage mp;
		// Recordings
		mp = createAndAddMenuPage(this.getWidth() >= 176 ?Locale.get("traceiconmenu.RecordTop")/* Recordings */:Locale.get("traceiconmenu.RecTop")/* Rec */, 3, 4);
		iconToggleTrackRec=	mp.createAndAddIcon("Record Track", "i_rectrack", Trace.START_RECORD_CMD);
		iconToggleTrackRec.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		mp.createAndAddIcon(Locale.get("traceiconmenu.SaveWpt")/*Save Wpt*/, "i_savewpt", Trace.SAVE_WAYP_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.EnterWpt")/*Enter Wpt*/, "i_enterwpt", Trace.ENTER_WAYP_CMD);

		mp.createAndAddIcon(Locale.get("traceiconmenu.Tracks")/*Tracks*/, "i_tracks", Trace.MANAGE_TRACKS_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.Waypoints")/*Waypoints*/, "i_wpts", Trace.MAN_WAYP_CMD);

		mp.createAndAddIcon(Locale.get("traceiconmenu.Photo")/*Photo*/, "i_photo", Trace.CAMERA_CMD);
		iconToggleAudioRec=	mp.createAndAddIcon(Locale.get("traceiconmenu.Voice")/*Voice*/, "i_micro", Trace.TOGGLE_AUDIO_REC);
		iconToggleAudioRec.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		
							mp.createAndAddIcon(Locale.get("traceiconmenu.SendSMS")/*Send SMS*/, "i_sendsms", Trace.SEND_MESSAGE_CMD);		
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);
	}


	private void createAndAddRoutingMenu() {
		IconMenuPage mp;
		// Route
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.RoutePage")/* Route */, 3, 4);
		iconToggleRoute=	mp.createAndAddIcon(Locale.get("traceiconmenu.Calc")/*Calculate*/, "i_calc", Trace.ROUTING_TOGGLE_CMD);
		iconToggleRoute.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		mp.createAndAddIcon(Locale.get("traceiconmenu.SetDest")/*Set dest*/, "i_setdest", Trace.SET_DEST_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.ShowDest")/*Show dest*/, "i_showdest", Trace.SHOW_DEST_CMD);		
		mp.createAndAddIcon(Locale.get("traceiconmenu.ClearDest")/*Clear dest*/, "i_cleardest", Trace.CLEAR_DEST_CMD);		
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
		boolean on = trace.isGpsConnected();
		iconToggleGps.setText( on ? Locale.get("traceiconmenu.StopGPS")/*Stop GPS*/ : Locale.get("traceiconmenu.StartGPS")/*Start GPS*/);
		iconToggleGps.setActionID( on ? Trace.DISCONNECT_GPS_CMD : Trace.CONNECT_GPS_CMD);
		iconToggleGps.setImageToggleState( !on );
		
		on = trace.gpx.isRecordingTrk();
		iconToggleTrackRec.setText( on ? Locale.get("traceiconmenu.StopRec")/*Stop Rec*/ : Locale.get("traceiconmenu.RecordTrack")/*Record Track*/);
		iconToggleTrackRec.setActionID( on ? Trace.STOP_RECORD_CMD : Trace.START_RECORD_CMD);
		iconToggleTrackRec.setImageToggleState( !on );
		
		on = trace.audioRec.isRecording();
		iconToggleAudioRec.setText( on ? Locale.get("traceiconmenu.StopVoice")/*Stop VoiceRec*/ : Locale.get("traceiconmenu.Voice")/*Voice*/);
		iconToggleAudioRec.setImageToggleState( !on );
		
		on = (trace.route != null || trace.routeCalc);
		iconToggleRoute.setText( on ? Locale.get("traceiconmenu.StopRoute")/*Stop Route*/ : Locale.get("traceiconmenu.Calc")/*Calculate*/);				
		iconToggleRoute.setImageToggleState( !on );
				
		super.paint(g);
	}
	
	
}
