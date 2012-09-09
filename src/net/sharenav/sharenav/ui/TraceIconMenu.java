/*
 * ShareNav - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See file COPYING
 */

package net.sharenav.sharenav.ui;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.midlet.iconmenu.IconActionPerformer;
import net.sharenav.midlet.iconmenu.IconMenuPage;
import net.sharenav.midlet.iconmenu.IconMenuWithPagesGui;
import net.sharenav.midlet.iconmenu.LayoutElement;

import java.util.Vector;

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
	//#if polish.android
	LayoutElement iconHelpOnlineWikiAndroid;
	//#endif
	
	private final int favoriteMax = 20;

	private static int rememberedEleId = 0;
	private static int rememberedTabNr = 0;
	
	public PositionMark[] wayPts;
	
	// FIXME read from config file and/or add a UI to change these
	//#if polish.api.finland
	private final static String[] predefs = 
	{ "N30", "N40", "N50", "N60", "N70", "N80", "N100", "N120", "Nvaiht", "Tievalot", "Tievalotpois", "2kaist", "1kaist", "aita", "aitapois", "b", "bvas", "kam", "N %f", "b %s", "tie %s", "tieoik", "tievas" };
	//#else
	private final static String[] predefs = {
	    "30", "40", "50", "60", "70", "80", "90", "100", "110", "120",
		"City limit",
		"Speed %f",
		"Speed end",
		"House nr. %f",
		"Bus stop",
		"Agr 1 asph",
		"Agr %f gravel",
		"Agr %f grass",
		"Waypoint",
		"Phone",
		"Path %s",
		"Back" };
	//#endif
	private String[] predefsToShow = { };

	public TraceIconMenu(ShareNavDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;

		// Main
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.MainTop")/* Main */, 3, 3);
		iconToggleGps =		mp.createAndAddIcon(Locale.get("traceiconmenu.StartGPS")/*Start GPS*/, "i_gps", Trace.CONNECT_GPS_CMD);
		iconToggleGps.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		mp.createAndAddIcon(Locale.get("generic.Search")/*Search*/, "i_search", Trace.SEARCH_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.MapFeature")/*Map Features*/, "i_mapfeat", Trace.MAPFEATURES_CMD);
		
		mp.createAndAddIcon(Locale.get("traceiconmenu.Setup")/*Setup*/, "i_setup", Trace.SETUP_CMD);
		if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)) {
			mp.createAndAddIcon(Locale.get("traceiconmenu.Tacho")/*Tacho*/, "i_tacho", Trace.CMS_CMD);
		} else {
			mp.createAndAddIcon(Locale.get("traceiconmenu.Tacho")/*Tacho*/, "i_tacho", Trace.DATASCREEN_CMD);
		}
		mp.createAndAddIcon(Locale.get("traceiconmenu.Overview")/*Overview/Filter Map*/, "i_overview", Trace.OVERVIEW_MAP_CMD);
		
		iconOnlineInfo =	mp.createAndAddIcon(Locale.get("traceiconmenu.Online")/*Online*/, "i_online", Trace.ONLINE_INFO_CMD);		
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);

		mp.createAndAddIcon(Locale.get("generic.Exit")/*Exit*/, "i_exit", Trace.EXIT_CMD);

		// determine preferred ordering
		//if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
		createAndAddRoutingMenu();
		createAndAddRecordingMenu();
		//} else {
		//	createAndAddRecordingMenu();			
		//	createAndAddRoutingMenu();
		//}
		// Osm
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.OsmTop")/* Osm */, 3, 3);
		iconEditWay =		mp.createAndAddIcon(Locale.get("traceiconmenu.EditWay")/*Edit way*/, "i_editway", Trace.RETRIEVE_XML);
		iconEditPOI =		mp.createAndAddIcon(Locale.get("traceiconmenu.EditPOI")/*Edit POI*/, "i_addpoi", Trace.EDIT_ENTITY);
		iconAddPOI =		mp.createAndAddIcon(Locale.get("traceiconmenu.AddPOI")/*Add POI*/, "i_addpoi", Trace.RETRIEVE_NODE);
		iconAddAddr =		mp.createAndAddIcon(Locale.get("traceiconmenu.AddAddr")/*Add Address*/, "i_addpoi", Trace.EDIT_ADDR_CMD);
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);

		//#if not polish.api.online
		iconAddPOI.makeImageGreyed();
		iconEditWay.makeImageGreyed();
		iconAddAddr.makeImageGreyed();
		iconOnlineInfo.makeImageGreyed();
		//#endif
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_INTERNET_ACCESS)) {
			iconAddPOI.makeImageGreyed();
			iconEditPOI.makeImageGreyed();
			iconEditWay.makeImageGreyed();
			iconAddAddr.makeImageGreyed();
			iconOnlineInfo.makeImageGreyed();
		}
		
		//#if not polish.api.osm-editing
		iconEditWay.makeImageGreyed();
		//#endif
		
		createAndAddHelpMenu();

		setActiveTabAndCursor(rememberedTabNr, rememberedEleId);
	}
	
	private void createAndAddRecordingMenu() {
		IconMenuPage mp;
		// Recordings
		if (Configuration.getCfgBitState(Configuration.CFGBIT_WAYPT_OFFER_PREDEF)) {
			// FIXME read from config file and/or add a UI to change these
			predefsToShow = predefs;
		}
		mp = createAndAddMenuPage((this.getWidth() >= 176) 
				? Locale.get("traceiconmenu.RecordTop")/* Recordings */
					  : Locale.get("traceiconmenu.RecTop")/* Rec */, 4, 3);
		// to use a bigger number of icons in the same screen, e.g. this: 
		//			  (predefsToShow.length) / 3 + 4 : 4);
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

		// FIXME only if predefined waypoint function enabled
		addPredefsToWayptMenu(mp);
	}

	private void createAndAddRoutingMenu() {
		IconMenuPage mp;
		// Route
		//mp = createAndAddMenuPage(Locale.get("traceiconmenu.RoutePage")/* Route */, 3,
		//			  Configuration.getCfgBitSavedState(Configuration.CFGBIT_FAVORITES_IN_ROUTE_ICON_MENU) ? 
		//			  (countFavorites() - 2) / 3 + 3 : 3);
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.RoutePage")/* Route */, 3,
					  (false && Configuration.getCfgBitSavedState(Configuration.CFGBIT_FAVORITES_IN_ROUTE_ICON_MENU)) ? 
					  (countFavorites() - 2) / 3 + 3 : 3);
		iconToggleRoute = mp.createAndAddIcon(Locale.get("traceiconmenu.Calc")/*Calculate*/, "i_calc", Trace.ROUTING_TOGGLE_CMD);
		iconToggleRoute.setFlag(LayoutElement.FLAG_IMAGE_TOGGLEABLE);
		mp.createAndAddIcon(Locale.get("traceiconmenu.SetDest")/*Set dest*/, "i_setdest", Trace.SET_DEST_CMD);
		mp.createAndAddIcon(Locale.get("traceiconmenu.ShowDest")/*Show dest*/, "i_showdest", Trace.SHOW_DEST_CMD);		
		mp.createAndAddIcon(Locale.get("traceiconmenu.ClearDest")/*Clear dest*/, "i_cleardest", Trace.CLEAR_DEST_CMD);		
		// FIXME add separate icon for saving route as GPX
		mp.createAndAddIcon(Locale.get("traceiconmenu.SaveRouteGpx")/*Save route as GPX*/, "i_rectrack0", Trace.SAVE_ROUTE_AS_GPX);
		mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);
		if (Configuration.getCfgBitState(Configuration.CFGBIT_FAVORITES_IN_ROUTE_ICON_MENU)) {
			addFavoritesToRoutingMenu(mp);
		}
	}

	private int countFavorites() {
		Vector wpt = Trace.getInstance().gpx.listWayPoints(true);
		return wpt.size() > favoriteMax ? favoriteMax : wpt.size();
	}

	// FIXME when predefs are changed, the icon layout should be recreated
	private void addPredefsToWayptMenu(IconMenuPage mp) {
		for (int i = 0; i < predefsToShow.length; i++) {
			mp.createAndAddIcon(predefsToShow[i], "i_savewpt", Trace.SAVE_PREDEF_WAYP_CMD);
		}
	}

	// FIXME when favorites are changed, the icon layout should be recreated
	private void addFavoritesToRoutingMenu(IconMenuPage mp) {
		Vector wpt = Trace.getInstance().gpx.listWayPoints(true);
		wayPts = new PositionMark[wpt.size()];
		wpt.copyInto(wayPts);
		int count = 0;
		for (int i = 0; i < wayPts.length; i++ ) {
			String name = wayPts[i].displayName;
			name = name.substring(0, name.length()-1);
			count++;
			mp.createAndAddIcon(name, "i_calc1", Trace.ROUTE_TO_FAVORITE_CMD);
			if (count >= favoriteMax) {
				break;
			}
		}
	}
	private void createAndAddHelpMenu() {
		IconMenuPage mp;
		// Route
		mp = createAndAddMenuPage(Locale.get("traceiconmenu.HelpPage")/* Help */, 3, 3);

		mp.createAndAddIcon(Locale.get("generic.About")/*About*/, "i_about", Trace.ABOUT_CMD);
		if (hasPointerEvents()) {
			mp.createAndAddIcon(Locale.get("trace.touchhelp")/*Touchscreen functions*/, "is_gui", Trace.TOUCH_HELP_CMD);
		} else {
			mp.createAndAddIcon(Locale.get("trace.displayhelp")/**/, "is_gui", Trace.TOUCH_HELP_CMD);

		}
		mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Keys")/*Keys*/, "is_keys", Trace.KEYS_HELP_CMD);
		if (hasPointerEvents()) {
			iconHelpOnlineTouch = mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Touch")/*Touch*/, "i_online", Trace.HELP_ONLINE_TOUCH_CMD);
		}
		iconHelpOnlineWiki = mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Wiki")/*Wiki*/, "i_online", Trace.HELP_ONLINE_WIKI_CMD);
		//#if polish.android
		iconHelpOnlineWikiAndroid = mp.createAndAddIcon(Locale.get("guidiscovericonmenu.AndroidWiki")/*Android help*/, "i_online", Trace.HELP_ONLINE_WIKI_ANDROID_CMD);
		//#endif
		//#if not polish.api.online
		if (hasPointerEvents()) {
			iconHelpOnlineTouch.makeImageGreyed();
		}
		iconHelpOnlineWiki.makeImageGreyed();
		//#if polish.android
		iconHelpOnlineWikiAndroid.makeImageGreyed();
		//#endif
		//#endif
		if (!Configuration.getCfgBitState(Configuration.CFGBIT_INTERNET_ACCESS)) {
			if (hasPointerEvents()) {
				iconHelpOnlineTouch.makeImageGreyed();
			}
			iconHelpOnlineWiki.makeImageGreyed();
		}
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
