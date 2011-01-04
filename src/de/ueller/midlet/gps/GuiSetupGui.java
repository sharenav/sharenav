/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.midlet.gps;

import javax.microedition.lcdui.*;

import de.enough.polish.util.Locale;

import de.ueller.gps.data.Configuration;



public class GuiSetupGui extends Form implements CommandListener {
	private ChoiceGroup imenuOpts;
	private ChoiceGroup mapTapFeatures;
	private ChoiceGroup otherOpts;
	private ChoiceGroup searchSettings;

	// commands
	private static final Command CMD_SAVE = new Command(Locale.get("generic.OK")/*Ok*/, 
			Command.ITEM, 2);
	private static final Command CMD_CANCEL = new Command(Locale.get("generic.Cancel")/*Cancel*/, 
			Command.BACK, 3);
	
	// other
	private final GpsMidDisplayable parent;
	private final boolean initialSetup;

	private TextField memField;
	
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
			
			String [] imenu = new String[5];
			imenu[0] = Locale.get("guisetupgui.UseIconMenu")/*Use icon menu*/;
			imenu[1] = Locale.get("guisetupgui.FullscreenIconMenu")/*Fullscreen icon menu*/;
			imenu[2] = Locale.get("guisetupgui.LargeTabButtons")/*Large tab buttons*/;
			imenu[3] = Locale.get("guisetupgui.IconsMappedOnKeys")/*Icons mapped on keys*/;
			imenu[4] = Locale.get("guisetupgui.OptimiseForRouting")/*Optimise for routing*/;
			imenuOpts = new ChoiceGroup(Locale.get("guisetupgui.IconMenu")/*Icon Menu:*/, 
					Choice.MULTIPLE, imenu, null);
			imenuOpts.setSelectedIndex(0, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS));
			imenuOpts.setSelectedIndex(1, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN));
			imenuOpts.setSelectedIndex(2, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS));
			imenuOpts.setSelectedIndex(3, 
					Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS));
			imenuOpts.setSelectedIndex(4, 
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED));
			append(imenuOpts);
		
			String [] other = new String[1];
			other[0] = Locale.get("guisetupgui.PredefWpts")/*Predefined way points*/;
			otherOpts = new ChoiceGroup(Locale.get("guisetupgui.OtherOpt")/*Other options:*/, Choice.MULTIPLE, other, null);
			otherOpts.setSelectedIndex(0,
					Configuration.getCfgBitSavedState(Configuration.CFGBIT_WAYPT_OFFER_PREDEF));
			append(otherOpts);
			
			/* only display search settings available on the device */
			// maximum search option entries
			int iMax = 0;
			if (Configuration.getHasPointerEvents()) {
				iMax++;
			}
			if (iMax > 0) {
				String [] search = new String[iMax];
				int i = 0;
				if (Configuration.getHasPointerEvents()) {
					search[i++] = Locale.get("guisetupgui.numberkeypad")/*Enable virtual keypad*/;
				}
				searchSettings = new ChoiceGroup(Locale.get("guisetupgui.searchopts")/*Search options:*/, Choice.MULTIPLE, search, null);
				i = 0;
				if (Configuration.getHasPointerEvents()) {
					searchSettings.setSelectedIndex(i++, Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD));
				}
				append(searchSettings);
			}
					
			if (Configuration.getHasPointerEvents()) {
				String [] touch = new String[3];
				int i = 0;
				touch[i++] = Locale.get("guisetupgui.longMapTap");
				touch[i++] = Locale.get("guisetupgui.doubleMapTap");
				touch[i++] = Locale.get("guisetupgui.singleMapTap");
				mapTapFeatures = new ChoiceGroup(Locale.get("guisetupgui.MapTapFeatures")/*Map Touch Features*/, 
						Choice.MULTIPLE, touch, null);
				i = 0;
				mapTapFeatures.setSelectedIndex(i++, Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_LONG));
				mapTapFeatures.setSelectedIndex(i++, Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_DOUBLE));
				mapTapFeatures.setSelectedIndex(i++, Configuration.getCfgBitState(Configuration.CFGBIT_MAPTAP_SINGLE));
				append(mapTapFeatures);
			}
			
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
				// nothing to do (igore content)
			}
			
			Trace trace = Trace.getInstance();
			if (imenuOpts.isSelected(0) != Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS)) {
				trace.removeAllCommands();
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS, imenuOpts.isSelected(0));
				trace.addAllCommands();
			}
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN,
					imenuOpts.isSelected(1));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS,
					imenuOpts.isSelected(2));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS,
					imenuOpts.isSelected(3));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED,
					imenuOpts.isSelected(4));
			boolean optimizedForRouting = imenuOpts.isSelected(4);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED,
					optimizedForRouting);
			// When the GUI is to be optimized for routing and we have a default
			// backlight method, turn the backlight on.
			if (initialSetup && optimizedForRouting) {
				if (Configuration.getDefaultDeviceBacklightMethodCfgBit() != 0) {
					Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ON, true);
					GpsMid.getInstance().restartBackLightTimer();			
				}
			}

			Trace.uncacheIconMenu();
			GuiDiscover.uncacheIconMenu();
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_WAYPT_OFFER_PREDEF,
					otherOpts.isSelected(0));
			
			int i = 0;
			if (Configuration.getHasPointerEvents()) {
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD, searchSettings.isSelected(i++));
			}
		
			i = 0;
			if (Configuration.getHasPointerEvents()) {
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_MAPTAP_LONG, mapTapFeatures.isSelected(i++));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_MAPTAP_DOUBLE, mapTapFeatures.isSelected(i++));
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_MAPTAP_SINGLE, mapTapFeatures.isSelected(i++));
			}

			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
