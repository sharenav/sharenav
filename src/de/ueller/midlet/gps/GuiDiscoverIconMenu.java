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
			mp.createAndAddIcon("Location Receiver", "satelit", GuiDiscover.MENU_ITEM_LOCATION);
			mp.createAndAddIcon("Display", "cinema", GuiDiscover.MENU_ITEM_DISP_OPT);
			mp.createAndAddIcon("Sounds & Alerts", "pub", GuiDiscover.MENU_ITEM_SOUNDS_OPT);

			mp.createAndAddIcon("Routing", "motorway", GuiDiscover.MENU_ITEM_ROUTING_OPT);
			mp.createAndAddIcon("Export", "railstation", GuiDiscover.MENU_ITEM_GPX_DEVICE);
			mp.createAndAddIcon("Back", "recycling", GuiDiscover.MENU_ITEM_BACK);
			
		// Advanced
		mp = createAndAddMenuPage(" Advanced ", 3, 3);
			mp.createAndAddIcon("Recording Rules", "restaurant", GuiDiscover.MENU_ITEM_GPX_FILTER);
			mp.createAndAddIcon("Map Source", "city", GuiDiscover.MENU_ITEM_MAP_SRC);
			mp.createAndAddIcon("OpenCellID", "satelitred", GuiDiscover.MENU_ITEM_OPENCELLID_OPT);

			mp.createAndAddIcon("Keys", "peak", GuiDiscover.MENU_ITEM_KEYS_OPT);
			mp.createAndAddIcon("Load Config", "mark", GuiDiscover.MENU_ITEM_LOAD_CONFIG);
			mp.createAndAddIcon("Save Config", "target", GuiDiscover.MENU_ITEM_SAVE_CONFIG);
			
			mp.createAndAddIcon("Debug", "unknown", GuiDiscover.MENU_ITEM_DEBUG_OPT);
	}

}
