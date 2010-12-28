/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.gps.tools.IconMenuWithPagesGUI;
import de.ueller.gps.tools.IconMenuPage;

import javax.microedition.lcdui.Graphics;

import de.enough.polish.util.Locale;

public class GuiDiscoverIconMenu extends IconMenuWithPagesGUI {
	private static int rememberedEleId = 0;
	private static int rememberedTabNr = 0;


	public GuiDiscoverIconMenu(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;
		// Basic
		mp = createAndAddMenuPage(Locale.get("guidiscovericonmenu.Basic")/* Basic */, 3, 3);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.LocReceiver")/*Location Receiver*/, "is_loc", GuiDiscover.MENU_ITEM_LOCATION);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Display")/*Display*/, "is_display", GuiDiscover.MENU_ITEM_DISP_OPT);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.SoundAlert")/*Sounds "Sounds & Alerts" */, "is_sound", GuiDiscover.MENU_ITEM_SOUNDS_OPT);

			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Routing")/*Routing*/, "is_route", GuiDiscover.MENU_ITEM_ROUTING_OPT);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.RecRules")/*Recording Rules*/, "is_rec", GuiDiscover.MENU_ITEM_GPX_FILTER);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Export")/*Export*/, "is_export", GuiDiscover.MENU_ITEM_GPX_DEVICE);

			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Online")/*Online*/, "i_online", GuiDiscover.MENU_ITEM_ONLINE_OPT);

			mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "is_back", IconActionPerformer.BACK_ACTIONID);
			
		// Advanced
		mp = createAndAddMenuPage(Locale.get("guidiscovericonmenu.Advanced")/* Advanced */, 3, 3);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Gui")/*Gui*/, "is_gui", GuiDiscover.MENU_ITEM_GUI_OPT);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.MapSource")/*Map Source*/, "is_map", GuiDiscover.MENU_ITEM_MAP_SRC);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Debug")/*Debug*/, "is_debug", GuiDiscover.MENU_ITEM_DEBUG_OPT);

			//#if polish.api.osm-editing
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.OSMAccount")/*OSM Account*/, "is_osmacc", GuiDiscover.MENU_ITEM_OSM_OPT);
			//#endif
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.OpenCellID")/*OpenCellID*/, "is_cellid", GuiDiscover.MENU_ITEM_OPENCELLID_OPT);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.Keys")/*Keys*/, "is_keys", GuiDiscover.MENU_ITEM_KEYS_OPT);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.LoadConfig")/*Load Config*/, "is_load", GuiDiscover.MENU_ITEM_LOAD_CONFIG);
			mp.createAndAddIcon(Locale.get("guidiscovericonmenu.SaveConfig")/*Save Config*/, "is_save", GuiDiscover.MENU_ITEM_SAVE_CONFIG);
			mp.createAndAddIcon(Locale.get("generic.Back")/*Back*/, "i_back", IconActionPerformer.BACK_ACTIONID);

			setActiveTabAndCursor(rememberedTabNr, rememberedEleId);
	}
	
	private void rememberActiveTabAndEleNr() {
		rememberedTabNr = tabNr;
		rememberedEleId = getActiveMenuPage().rememberEleId;
	}
	
	
	public void paint(Graphics g) {
		rememberActiveTabAndEleNr();
		super.paint(g);
	}
	
}
