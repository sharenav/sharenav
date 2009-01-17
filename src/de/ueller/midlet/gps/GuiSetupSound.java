package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;
import java.io.InputStream;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.SoundDescription;

public class GuiSetupSound extends Form implements CommandListener {
	private final Form	menuSetupSound = new Form("Sounds & Alerts");
	// Groups
	private ChoiceGroup sndGpsGroup;
	private	String [] sndGps = new String[2];
	private boolean[] selSndGps = new boolean[2];

	private ChoiceGroup sndRoutingGroup=null;
	private	String [] sndRouting = new String[2];
	private	boolean[] selSndRouting = new boolean[2];

	private ChoiceGroup spdAlertGroup=null;
	private	String [] spdAlert = new String[2];
	private	boolean[] selSpdAlert = new boolean[2];

        private TextField spdAlertTolerance=null;

	// commands
	private static final Command CMD_SAVE = new Command("Ok", Command.ITEM, 2);
	private static final Command CMD_CANCEL = new Command("Cancel", Command.CANCEL, 3);
	
	// other
	private GuiDiscover parent;
	private static Configuration config;
	
	public GuiSetupSound(GuiDiscover parent) {
		super("Sounds & Alerts");
		config=GpsMid.getInstance().getConfig();
		this.parent = parent;
		try {
			// set choice texts and convert bits from config flag into selection states
			sndGps[0] = "Connect"; 			selSndGps[0]=config.getCfgBitState(config.CFGBIT_SND_CONNECT);
			sndGps[1] = "Disconnect";		selSndGps[1]=config.getCfgBitState(config.CFGBIT_SND_DISCONNECT);
			sndGpsGroup = new ChoiceGroup("GPS", Choice.MULTIPLE, sndGps ,null);
			sndGpsGroup.setSelectedFlags(selSndGps);
			append(sndGpsGroup);
			
			sndRouting[0] = "Routing Instructions"; 	selSndRouting[0]=config.getCfgBitState(config.CFGBIT_SND_ROUTINGINSTRUCTIONS);
			sndRouting[1] = "Target Reached"; 			selSndRouting[1]=config.getCfgBitState(config.CFGBIT_SND_TARGETREACHED);
			sndRoutingGroup = new ChoiceGroup("Navigation / Routing", Choice.MULTIPLE, sndRouting ,null);
			sndRoutingGroup.setSelectedFlags(selSndRouting);
			append(sndRoutingGroup);
			
			spdAlert[0] = "Audio Speeding Alert";
			selSpdAlert[0]=config.getCfgBitState(config.CFGBIT_SPEEDALERT_SND);
			spdAlert[1] = "Visual Speeding Alert";
			selSpdAlert[1]=config.getCfgBitState(config.CFGBIT_SPEEDALERT_VISUAL);
			spdAlertGroup = new ChoiceGroup("Speeding Alert", Choice.MULTIPLE, spdAlert ,null);
			spdAlertGroup.setSelectedFlags(selSpdAlert);
			append(spdAlertGroup);

			spdAlertTolerance=new TextField("Speed Alert Tolerance",Integer.toString(config.getSpeedTolerance()),3,TextField.DECIMAL);
			append(spdAlertTolerance);

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
			// convert boolean array with selection states for config bits
			// to one flag with corresponding bits set
	        sndGpsGroup.getSelectedFlags(selSndGps);
			config.setCfgBitState(config.CFGBIT_SND_CONNECT, selSndGps[0], true);
			config.setCfgBitState(config.CFGBIT_SND_DISCONNECT, selSndGps[1], true);

			sndRoutingGroup.getSelectedFlags(selSndRouting);
			config.setCfgBitState(config.CFGBIT_SND_ROUTINGINSTRUCTIONS, selSndRouting[0], true);
			config.setCfgBitState(config.CFGBIT_SND_TARGETREACHED, selSndRouting[1], true);

		    config.setSpeedTolerance((int)Float.parseFloat(spdAlertTolerance.getString()));

			spdAlertGroup.getSelectedFlags(selSpdAlert);
			config.setCfgBitState(config.CFGBIT_SPEEDALERT_SND, selSpdAlert[0], true);
			config.setCfgBitState(config.CFGBIT_SPEEDALERT_VISUAL, selSpdAlert[1], true);

			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
