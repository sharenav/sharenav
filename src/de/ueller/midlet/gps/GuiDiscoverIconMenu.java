/*
 * GpsMid - Copyright (c) 2009 sk750 at users dot sourceforge dot net 
 * See Copying
 */

package de.ueller.midlet.gps;
import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.gps.tools.IconMenuWithPagesGUI;
import de.ueller.gps.tools.IconMenuPage;

import javax.microedition.lcdui.Graphics;


public class GuiDiscoverIconMenu extends IconMenuWithPagesGUI {

	public GuiDiscoverIconMenu(GpsMidDisplayable parent, IconActionPerformer actionPerformer) {
		super(parent, actionPerformer);
	
		IconMenuPage mp;
		// Basic
		mp = createAndAddMenuPage(" Basic "/*i:Basic*/, 3, 3);
			mp.createAndAddIcon("Location Receiver"/*i:LocReceiver*/, "is_loc", GuiDiscover.MENU_ITEM_LOCATION);
			mp.createAndAddIcon("Display"/*i:Display*/, "is_display", GuiDiscover.MENU_ITEM_DISP_OPT);
			mp.createAndAddIcon("Sounds & Alerts"/*i:SoundAlert*/, "is_sound", GuiDiscover.MENU_ITEM_SOUNDS_OPT);

			mp.createAndAddIcon("Routing"/*i:Routing*/, "is_route", GuiDiscover.MENU_ITEM_ROUTING_OPT);
			mp.createAndAddIcon("Recording Rules"/*i:RecRules*/, "is_rec", GuiDiscover.MENU_ITEM_GPX_FILTER);
			mp.createAndAddIcon("Export"/*i:Export*/, "is_export", GuiDiscover.MENU_ITEM_GPX_DEVICE);

			mp.createAndAddIcon("Back"/*i:Back*/, "is_back", IconActionPerformer.BACK_ACTIONID);
			
		// Advanced
		mp = createAndAddMenuPage(" Advanced "/*i:Advanced*/, 3, 3);
			mp.createAndAddIcon("Gui"/*i:Gui*/, "is_gui", GuiDiscover.MENU_ITEM_GUI_OPT);
			mp.createAndAddIcon("Map Source"/*i:MapSource*/, "is_map", GuiDiscover.MENU_ITEM_MAP_SRC);
			mp.createAndAddIcon("Debug"/*i:Debug*/, "is_debug", GuiDiscover.MENU_ITEM_DEBUG_OPT);

			//#if polish.api.osm-editing
			mp.createAndAddIcon("OSM Account"/*i:OSMAccount*/, "is_osmacc", GuiDiscover.MENU_ITEM_OSM_OPT);
			//#endif
			mp.createAndAddIcon("OpenCellID"/*i:OpenCellID*/, "is_cellid", GuiDiscover.MENU_ITEM_OPENCELLID_OPT);
			mp.createAndAddIcon("Keys"/*i:Keys*/, "is_keys", GuiDiscover.MENU_ITEM_KEYS_OPT);
			mp.createAndAddIcon("Load Config"/*i:LoadConfig*/, "is_load", GuiDiscover.MENU_ITEM_LOAD_CONFIG);
			mp.createAndAddIcon("Save Config"/*i:SaveConfig*/, "is_save", GuiDiscover.MENU_ITEM_SAVE_CONFIG);
	}

}
