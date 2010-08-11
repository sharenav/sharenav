/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

import javax.microedition.lcdui.*;
import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.Legend;


public class GuiSetupSound extends Form implements CommandListener {
	// Sound Directories
	private ChoiceGroup sndDirsGroup;
	
	// Groups
	private ChoiceGroup sndGpsGroup;
	private	String [] sndGps = new String[3];
	private boolean[] selSndGps = new boolean[3];

	private ChoiceGroup sndRoutingGroup=null;
	private	String [] sndRouting = new String[2];
	private	boolean[] selSndRouting = new boolean[2];

	private ChoiceGroup spdAlertGroup=null;
	private	String [] spdAlert = new String[3];
	private	boolean[] selSpdAlert = new boolean[3];

        private TextField spdAlertTolerance=null;

	// commands
	private static final Command CMD_SAVE = new Command("Ok"/* i:Ok */, Command.ITEM, 2);
	private static final Command CMD_CANCEL = new Command("Cancel"/* i:Cancel */, Command.BACK, 3);
	
	// other
	private GuiDiscover parent;
	
	public GuiSetupSound(GuiDiscover parent) {
		super("Sounds & Alerts"/* i:SoundsAlerts */);
		this.parent = parent;
		try {
			// add sound directories and set selected one.
			sndDirsGroup = new ChoiceGroup("Sound/Navigation"/* i:SoundNavigation */, Choice.EXCLUSIVE, Legend.soundDirectories ,null);
			for (int i=0; i < Legend.soundDirectories.length; i++) {
				if (Legend.soundDirectories[i].equals(Configuration.getSoundDirectory())) {
					sndDirsGroup.setSelectedIndex(i, true);
				}
			}
			append(sndDirsGroup);

			// set choice texts and convert bits from Configuration flag into selection states
			sndGps[0] = "Connect"/* i:Connect */; 			selSndGps[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT);
			sndGps[1] = "Disconnect"/* i:Disconnect */;		selSndGps[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_DISCONNECT);
			sndGps[2] = "Prefer tone sequences"/* i:PreferToneS */; selSndGps[2]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_TONE_SEQUENCES_PREFERRED);
			sndGpsGroup = new ChoiceGroup("GPS"/* i:GPS */, Choice.MULTIPLE, sndGps ,null);
			sndGpsGroup.setSelectedFlags(selSndGps);
			append(sndGpsGroup);
			
			sndRouting[0] = "Routing instructions"/* i:RoutingInstructions */;
			selSndRouting[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS);
			sndRouting[1] = "Destination reached"/* i:DestinationReached */;
			selSndRouting[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_DESTREACHED);
			sndRoutingGroup = new ChoiceGroup("Navigation / Routing"/* i:NavigationRouting */, Choice.MULTIPLE, sndRouting, null);
			sndRoutingGroup.setSelectedFlags(selSndRouting);
			append(sndRoutingGroup);
			
			spdAlert[0] = "Audio speeding alert"/* i:AudioSpeedingAlert */;
			selSpdAlert[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_SND);
			spdAlert[1] = "Visual speeding alert"/* i:VisualSpeedingAlert */;
			selSpdAlert[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_VISUAL);
			spdAlert[2] = "Winter limits active"/* i:WinterLimitsCctive */;
			selSpdAlert[2]=Configuration.getCfgBitState(Configuration.CFGBIT_MAXSPEED_WINTER);
			spdAlertGroup = new ChoiceGroup("Speeding alert"/* i:SpeedingAlert */, Choice.MULTIPLE, spdAlert ,null);
			spdAlertGroup.setSelectedFlags(selSpdAlert);
			append(spdAlertGroup);

			spdAlertTolerance = new TextField("Speed alert tolerance"/* i:SpeedAlertTolerance */,
					Integer.toString(Configuration.getSpeedTolerance()), 3, TextField.DECIMAL);
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
			// set sound directory
	        String newSoundDir = Legend.soundDirectories[sndDirsGroup.getSelectedIndex()];
			if (!newSoundDir.equals(Configuration.getSoundDirectory())) {
				Configuration.setSoundDirectory(newSoundDir);
				RouteSyntax.getInstance().readSyntax();
			}
			
			// convert boolean array with selection states for Configuration bits
			// to one flag with corresponding bits set
	        sndGpsGroup.getSelectedFlags(selSndGps);
	        Configuration.setCfgBitState(Configuration.CFGBIT_SND_CONNECT, selSndGps[0], true);
	        Configuration.setCfgBitState(Configuration.CFGBIT_SND_DISCONNECT, selSndGps[1], true);
	        Configuration.setCfgBitState(Configuration.CFGBIT_SND_TONE_SEQUENCES_PREFERRED, selSndGps[2], true);

			sndRoutingGroup.getSelectedFlags(selSndRouting);
			Configuration.setCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS, selSndRouting[0], true);
			Configuration.setCfgBitState(Configuration.CFGBIT_SND_DESTREACHED, selSndRouting[1], true);

			Configuration.setSpeedTolerance((int)Float.parseFloat(spdAlertTolerance.getString()));

			spdAlertGroup.getSelectedFlags(selSpdAlert);
			Configuration.setCfgBitState(Configuration.CFGBIT_SPEEDALERT_SND, selSpdAlert[0], true);
			Configuration.setCfgBitState(Configuration.CFGBIT_SPEEDALERT_VISUAL, selSpdAlert[1], true);
			Configuration.setCfgBitState(Configuration.CFGBIT_MAXSPEED_WINTER, selSpdAlert[2], true);

			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
