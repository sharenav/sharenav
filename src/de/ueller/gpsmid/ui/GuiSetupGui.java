/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import javax.microedition.lcdui.*;

import de.enough.polish.util.Locale;

import de.ueller.gpsmid.data.Configuration;


public class GuiSetupGui extends Form implements CommandListener {
	private ChoiceGroup imenuOpts;
	private ChoiceGroup mapTapFeatures;
	private ChoiceGroup searchSettings;
	private ChoiceGroup searchLayoutGroup;
	private ChoiceGroup otherGroup;

	// commands
	private static final Command CMD_SAVE = new Command(Locale.get("generic.Save")/*Save*/, 
			GpsMidMenu.OK, 2);
	private static final Command CMD_CANCEL = new Command(Locale.get("generic.Cancel")/*Cancel*/, 
			GpsMidMenu.BACK, 3);
	
	// other
	private final GpsMidDisplayable parent;
	private final boolean initialSetup;

	private TextField memField;
	private TextField searchField;
	private TextField poiSearchDistance;
	
	public GuiSetupGui(GpsMidDisplayable parent, boolean initialSetup) {
		super(Locale.get("guisetupgui.GUIOptions")/*GUI Options*/);
		this.parent = parent;
		this.initialSetup = initialSetup;
		try {
			long mem = Configuration.getPhoneAllTimeMaxMemory();
			if (mem == 0) {
				mem = Runtime.getRuntime().totalMemory();
			}
			mem = mem / 1024;
			memField = new TextField(Locale.get("guisetupgui.DefineMaxMem")/*Define maxMem (kbyte)*/,
					Long.toString(mem), 8, TextField.DECIMAL);
			append(memField);
			String [] imenu = new String[6];
			imenu[0] = Locale.get("guisetupgui.UseIconMenu")/*Use icon menu*/;
			imenu[1] = Locale.get("guisetupgui.FullscreenIconMenu")/*Fullscreen icon menu*/;
			imenu[2] = Locale.get("guisetupgui.SplitscreenIconMenu")/*Split screen icon menu*/;
			imenu[3] = Locale.get("guisetupgui.LargeTabButtons")/*Large tab buttons*/;
			imenu[4] = Locale.get("guisetupgui.IconsMappedOnKeys")/*Icons mapped on keys*/;
			//imenu[5] = Locale.get("guisetupgui.OptimiseForRouting")/*Optimise for routing*/;
			imenu[5] = Locale.get("guisetupgui.FavoritesInRouteIconMenu")/*Favorites in route icon menu*/;
			imenuOpts = new ChoiceGroup(Locale.get("guisetupgui.IconMenu")/*Icon Menu:*/, 
					Choice.MULTIPLE, imenu, null);
			imenuOpts.setSelectedIndex(0, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS));
			imenuOpts.setSelectedIndex(1, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN));
			imenuOpts.setSelectedIndex(2, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN));
			imenuOpts.setSelectedIndex(3, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS));
			imenuOpts.setSelectedIndex(4, 
					Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS));
			//imenuOpts.setSelectedIndex(5, 
			//		Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED));
			imenuOpts.setSelectedIndex(5, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_FAVORITES_IN_ROUTE_ICON_MENU));
			append(imenuOpts);
		
			if (Configuration.getHasPointerEvents()) {
				String [] touch = new String[4];
				int i = 0;
				touch[i++] = Locale.get("guisetupgui.longMapTap");
				touch[i++] = Locale.get("guisetupgui.doubleMapTap");
				touch[i++] = Locale.get("guisetupgui.singleMapTap");
				touch[i++] = Locale.get("guisetupgui.clickableMarkers");
				mapTapFeatures = new ChoiceGroup(Locale.get("guisetupgui.MapTapFeatures")/*Map Touch Features*/, 
						Choice.MULTIPLE, touch, null);
				i = 0;
				mapTapFeatures.setSelectedIndex(i++, Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_LONG));
				mapTapFeatures.setSelectedIndex(i++, Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_DOUBLE));
				mapTapFeatures.setSelectedIndex(i++, Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_SINGLE));
				mapTapFeatures.setSelectedIndex(i++, Configuration.getCfgBitState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS));
				append(mapTapFeatures);
			}

			// search options
			int iMax = 3;
			if (Configuration.getHasPointerEvents()) {
				iMax++;
			}
			//#if polish.android
			iMax++;
			//#endif
			String [] search = null;
			search = new String[iMax];
			int searchnum = 0;
			search[searchnum++] = Locale.get("guisetupgui.scroll")/*Scroll result under cursor*/;
			search[searchnum++] = Locale.get("guisetupgui.scrollall")/*Scroll all results*/;
			if (Configuration.getHasPointerEvents()) {
			    search[searchnum++] = Locale.get("guisetupgui.numberkeypad")/*Enable virtual keypad*/;
			}
			search[searchnum++] = Locale.get("guisetupgui.SuppressSearchWarning")/*Suppress warning about exceeding max results*/;
			//#if polish.android
			search[searchnum++] = Locale.get("guisetupgui.ShowNativeKeyboard")/*Show native keyboard in search*/;
			//#endif
			searchSettings = new ChoiceGroup(Locale.get("guisetupgui.searchopts")/*Search options:*/, Choice.MULTIPLE, search, null);
			/* only display search settings available on the device */
			// maximum search option entries
			searchnum = 0;
			searchSettings.setSelectedIndex(searchnum++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_TICKER_ISEARCH));
			searchSettings.setSelectedIndex(searchnum++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_TICKER_ISEARCH_ALL));
			if (Configuration.getHasPointerEvents()) {
			    searchSettings.setSelectedIndex(searchnum++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD));
			}
			searchSettings.setSelectedIndex(searchnum++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SUPPRESS_SEARCH_WARNING));
			//#if polish.android
			searchSettings.setSelectedIndex(searchnum++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_SHOW_NATIVE_KEYBOARD));
			//#endif
			append(searchSettings);
			String [] searchLayout = new String[2];
			searchLayout[0] = Locale.get("guidiscover.SearchWholeNames")/*Search for whole names*/;
			searchLayout[1] = Locale.get("guidiscover.SearchWords")/*Search for words*/;
			searchLayoutGroup = new ChoiceGroup(Locale.get("guidiscover.SearchStyle")/*Search style*/, Choice.EXCLUSIVE, searchLayout, null);
			searchLayoutGroup.setSelectedIndex( Configuration.getCfgBitSavedState(Configuration.CFGBIT_WORD_ISEARCH) ? 1 : 0, true);
			append(searchLayoutGroup);

			int searchMax = Configuration.getSearchMax();
			searchField = new TextField(Locale.get("guisetupgui.DefineMaxSearch")/*Max # of search results*/,
					Integer.toString(searchMax), 8, TextField.DECIMAL);
			append(searchField);

			float dist = Configuration.getPoiSearchDistance();
			poiSearchDistance = new TextField(Locale.get("guisetupgui.PoiDistance")/*POI Distance: */,
							  Float.toString(dist), 8, TextField.ANY);
			append(poiSearchDistance);

			String [] otherSettings = new String[1];
			otherSettings[0] = Locale.get("guisetupgui.ExitWithBackButton")/* Back button exits application*/;
			otherGroup = new ChoiceGroup(Locale.get("guisetupgui.otherOptions")/* Other options */, Choice.MULTIPLE, otherSettings, null);
			otherGroup.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_EXIT_APPLICATION_WITH_BACK_BUTTON) );
			append(otherGroup);
			
			addCommand(CMD_SAVE);
			addCommand(CMD_CANCEL);

			// Set up this Displayable to listen to command events
			setCommandListener(this);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_CANCEL) {
			parent.show();
			return;
		}

		if (c == CMD_SAVE) {
			try {
				long mem=Long.parseLong(memField.getString());
				Configuration.setPhoneAllTimeMaxMemory(mem*1024);
			} catch (NumberFormatException e) {
				// nothing to do (ignore content)
			}
			try {
				int searchMax=Integer.parseInt(searchField.getString());
				Configuration.setSearchMax(searchMax);
			} catch (NumberFormatException e) {
				// nothing to do (ignore content)
			}
			try {
				float dist=Float.parseFloat(poiSearchDistance.getString());
				Configuration.setPoiSearchDistance(dist);
			} catch (NumberFormatException e) {
				// nothing to do (ignore content)
			}
			
			Trace trace = Trace.getInstance();
			if (imenuOpts.isSelected(0) != Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS)) {
				trace.removeAllCommands();
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS, imenuOpts.isSelected(0));
				trace.addAllCommands();
			}
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN,
					imenuOpts.isSelected(1));
			if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN)
			    && !imenuOpts.isSelected(2)) {
				trace.stopShowingSplitScreen();
			}
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_SPLITSCREEN,
					imenuOpts.isSelected(2));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS,
					imenuOpts.isSelected(3));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS,
					imenuOpts.isSelected(4));
			//Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED,
			//		imenuOpts.isSelected(5));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_FAVORITES_IN_ROUTE_ICON_MENU,
					imenuOpts.isSelected(5));
			// When the GUI is to be optimized for routing and we have a default
			// backlight method, turn the backlight on.
			boolean optimizedForRouting = imenuOpts.isSelected(5);
			if (initialSetup && optimizedForRouting) {
				if (Configuration.getDefaultDeviceBacklightMethodCfgBit() != 0) {
					Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ON, true);
					GpsMid.getInstance().restartBackLightTimer();			
				}
			}

			Trace.uncacheIconMenu();
			GuiDiscover.uncacheIconMenu();

			boolean searchLayout = (searchLayoutGroup.getSelectedIndex() == 1);
				
			if (searchLayout != Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH) ) {
			    Configuration.setCfgBitSavedState(Configuration.CFGBIT_WORD_ISEARCH, searchLayout);
			}
			int i = 0;
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_TICKER_ISEARCH, searchSettings.isSelected(i++));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_TICKER_ISEARCH_ALL, searchSettings.isSelected(i++));
			if (Configuration.getHasPointerEvents()) {
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD, searchSettings.isSelected(i++));
			}

			Configuration.setCfgBitSavedState(Configuration.CFGBIT_SUPPRESS_SEARCH_WARNING, searchSettings.isSelected(i++));
			//#if polish.android
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_SEARCH_SHOW_NATIVE_KEYBOARD, searchSettings.isSelected(i++));
			//#endif
		
			i = 0;
			if (Configuration.getHasPointerEvents()) {
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_MAPTAP_LONG, mapTapFeatures.isSelected(i++));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_MAPTAP_DOUBLE, mapTapFeatures.isSelected(i++));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_MAPTAP_SINGLE, mapTapFeatures.isSelected(i++));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_CLICKABLE_MAPOBJECTS, mapTapFeatures.isSelected(i++));
			}

			Configuration.setCfgBitSavedState(Configuration.CFGBIT_EXIT_APPLICATION_WITH_BACK_BUTTON, otherGroup.isSelected(0));
			
			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
