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
		mp = createAndAddMenuPage(" Basic ", 3, 3);
			mp.createAndAddIcon("Location Receiver", "is_loc", GuiDiscover.MENU_ITEM_LOCATION);
			mp.createAndAddIcon("Display", "is_display", GuiDiscover.MENU_ITEM_DISP_OPT);
			mp.createAndAddIcon("Sounds & Alerts", "is_sound", GuiDiscover.MENU_ITEM_SOUNDS_OPT);

			mp.createAndAddIcon("Routing", "is_route", GuiDiscover.MENU_ITEM_ROUTING_OPT);
			mp.createAndAddIcon("Recording Rules", "is_rec", GuiDiscover.MENU_ITEM_GPX_FILTER);
			mp.createAndAddIcon("Export", "is_export", GuiDiscover.MENU_ITEM_GPX_DEVICE);

			mp.createAndAddIcon("Back", "is_back", IconActionPerformer.BACK_ACTIONID);
			
		// Advanced
		mp = createAndAddMenuPage(" Advanced ", 3, 3);
			mp.createAndAddIcon("Gui", "is_gui", GuiDiscover.MENU_ITEM_GUI_OPT);
			mp.createAndAddIcon("Map Source", "is_map", GuiDiscover.MENU_ITEM_MAP_SRC);
			mp.createAndAddIcon("Debug", "is_debug", GuiDiscover.MENU_ITEM_DEBUG_OPT);

			mp.createAndAddIcon("OpenCellID", "is_cellid", GuiDiscover.MENU_ITEM_OPENCELLID_OPT);
			mp.createAndAddIcon("Keys", "is_keys", GuiDiscover.MENU_ITEM_KEYS_OPT);
			mp.createAndAddIcon("Load Config", "is_load", GuiDiscover.MENU_ITEM_LOAD_CONFIG);
			mp.createAndAddIcon("Save Config", "is_save", GuiDiscover.MENU_ITEM_SAVE_CONFIG);			
	}

}
