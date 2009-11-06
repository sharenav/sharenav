package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;
import de.ueller.gps.data.Configuration;


public class GuiSetupGui extends Form implements CommandListener {
	private ChoiceGroup guiOpts;

	// commands
	private static final Command CMD_SAVE = new Command("Ok", Command.ITEM, 2);
	private static final Command CMD_CANCEL = new Command("Cancel", Command.BACK, 3);
	
	// other
	private GpsMidDisplayable parent;
	
	public GuiSetupGui(GpsMidDisplayable parent) {
		super("GUI Options");
		this.parent = parent;
		try {
			String [] guis = new String[5];
			guis[0] = "use icon menu";
			guis[1] = "fullscreen icon menu";
			guis[2] = "large tab buttons";
			guis[3] = "icons mapped on keys";
			guis[4] = "optimise for routing";
			guiOpts = new ChoiceGroup("Icon Menu:", Choice.MULTIPLE, guis ,null);
			guiOpts.setSelectedIndex(0, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS));
			guiOpts.setSelectedIndex(1, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_FULLSCREEN));
			guiOpts.setSelectedIndex(2, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_BIG_TAB_BUTTONS));
			guiOpts.setSelectedIndex(3, Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_MAPPED_ICONS));
			guiOpts.setSelectedIndex(4, Configuration.getCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED));
			append(guiOpts);

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
			Configuration.setCfgBitSavedState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED, guiOpts.isSelected(4));
			Trace.uncacheIconMenu();
			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
