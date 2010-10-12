package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;

import de.ueller.gps.data.Configuration;

import de.enough.polish.util.Locale;


public class GuiSetupGui extends Form implements CommandListener {
	private ChoiceGroup guiOpts;

	// commands
	private static final Command CMD_SAVE = new Command(Locale.get("guisetupgui.Ok")/*Ok*/, Command.ITEM, 2);
	private static final Command CMD_CANCEL = new Command(Locale.get("guisetupgui.Cancel")/*Cancel*/, Command.BACK, 3);
	
	// other
	private GpsMidDisplayable parent;
	private boolean initialSetup;

	private TextField memField;
	
	public GuiSetupGui(GpsMidDisplayable parent, boolean initialSetup) {
		super(Locale.get("guisetupgui.GUIOptions")/*GUI Options*/);
		this.parent = parent;
		this.initialSetup = initialSetup;
		try {
			String [] guis = new String[5];
			guis[0] = Locale.get("guisetupgui.UseIconMenu")/*use icon menu*/;
			guis[1] = Locale.get("guisetupgui.FullscreenIconMenu")/*fullscreen icon menu*/;
			guis[2] = Locale.get("guisetupgui.LargeTabButtons")/*large tab buttons*/;
			guis[3] = Locale.get("guisetupgui.IconsMappedOnKeys")/*icons mapped on keys*/;
			guis[4] = Locale.get("guisetupgui.OptimiseForRouting")/*optimise for routing*/;
			guiOpts = new ChoiceGroup(Locale.get("guisetupgui.IconMenu")/*Icon Menu:*/, Choice.MULTIPLE, guis ,null);
			guiOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS));
			guiOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN));
			guiOpts.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS));
			guiOpts.setSelectedIndex(3, Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS));
			guiOpts.setSelectedIndex(4, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED));
			append(guiOpts);
			long mem=Configuration.getPhoneAllTimeMaxMemory();
			if (mem == 0){
				mem=Runtime.getRuntime().totalMemory();
			}
			mem=mem/1024;
			memField = new TextField(Locale.get("guisetupgui.DefineMaxMem")/*Define maxMem (kbyte)*/,
					Long.toString(mem), 8, TextField.DECIMAL);
			append(memField);
			
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
			Trace trace = Trace.getInstance();
			if (guiOpts.isSelected(0) != Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS)) {
				trace.removeAllCommands();
				Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS, guiOpts.isSelected(0));
				trace.addAllCommands();					
			}
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN, guiOpts.isSelected(1));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS, guiOpts.isSelected(2));
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS, guiOpts.isSelected(3));
			boolean optimizedForRouting = guiOpts.isSelected(4);
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED, optimizedForRouting);
			// when the GUI is to be optimized for routing and we have a default backlight method, turn the backlight on			
			if (initialSetup && optimizedForRouting) {
				if (Configuration.getDefaultDeviceBacklightMethodCfgBit() != 0) {
					Configuration.setCfgBitSavedState(Configuration.CFGBIT_BACKLIGHT_ON, true);
					GpsMid.getInstance().restartBackLightTimer();			
				}
			}
			try {
				long mem=Long.parseLong(memField.getString());
				Configuration.setPhoneAllTimeMaxMemory(mem*1024);
			} catch (NumberFormatException e) {
				// nothing to do (igore content)
			}
			Trace.uncacheIconMenu();
			GuiDiscover.uncacheIconMenu();
			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
