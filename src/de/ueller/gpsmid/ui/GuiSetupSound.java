/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import javax.microedition.lcdui.*;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.routing.RouteSyntax;
import de.enough.polish.util.Locale;


public class GuiSetupSound extends Form implements CommandListener {
	// Sound Directories
	private ChoiceGroup sndDirsGroup;
	
	// Groups
	private ChoiceGroup sndGpsGroup;
	private final	String [] sndGps = new String[3];
	private final boolean[] selSndGps = new boolean[3];

	private ChoiceGroup sndRoutingGroup=null;
	private final	String [] sndRouting = new String[2];
	private final	boolean[] selSndRouting = new boolean[2];

	private ChoiceGroup spdAlertGroup=null;
	private final	String [] spdAlert = new String[3];
	private final	boolean[] selSpdAlert = new boolean[3];

        private TextField spdAlertTolerance=null;

	// commands
	private static final Command CMD_SAVE = new Command(Locale.get("generic.OK")/*Ok*/, GpsMidMenu.OK, 2);
	private static final Command CMD_CANCEL = new Command(Locale.get("generic.Cancel")/*Cancel*/, GpsMidMenu.BACK, 3);
	
	// other
	private final GuiDiscover parent;
	
	public GuiSetupSound(GuiDiscover parent) {
		super(Locale.get("guisetupsound.SoundsAlerts")/*Sounds & Alerts*/);
		this.parent = parent;
		try {
			// add sound directories and set selected one.
			sndDirsGroup = new ChoiceGroup(Locale.get("guisetupsound.SoundNavigation")/*Sound/Navigation*/, Choice.EXCLUSIVE, Legend.soundDirectories ,null);
			for (int i=0; i < Legend.soundDirectories.length; i++) {
				if (Legend.soundDirectories[i].equals(Configuration.getSoundDirectory())) {
					sndDirsGroup.setSelectedIndex(i, true);
				}
			}
			append(sndDirsGroup);

			// set choice texts and convert bits from Configuration flag into selection states
			sndGps[0] = Locale.get("guisetupsound.Connect")/*Connect*/; 			selSndGps[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_CONNECT);
			sndGps[1] = Locale.get("guisetupsound.Disconnect")/*Disconnect*/;		selSndGps[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_DISCONNECT);
			sndGps[2] = Locale.get("guisetupsound.PreferToneS")/*Prefer tone sequences*/; selSndGps[2]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_TONE_SEQUENCES_PREFERRED);
			sndGpsGroup = new ChoiceGroup(Locale.get("guisetupsound.GPS")/*GPS*/, Choice.MULTIPLE, sndGps ,null);
			sndGpsGroup.setSelectedFlags(selSndGps);
			append(sndGpsGroup);
			
			sndRouting[0] = Locale.get("guisetupsound.RoutingInstructions")/*Routing instructions*/;
			selSndRouting[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_ROUTINGINSTRUCTIONS);
			sndRouting[1] = Locale.get("guisetupsound.DestinationReached")/*Destination reached*/;
			selSndRouting[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SND_DESTREACHED);
			sndRoutingGroup = new ChoiceGroup(Locale.get("guisetupsound.NavigationRouting")/*Navigation / Routing*/, Choice.MULTIPLE, sndRouting, null);
			sndRoutingGroup.setSelectedFlags(selSndRouting);
			append(sndRoutingGroup);
			
			spdAlert[0] = Locale.get("guisetupsound.AudioSpeedingAlert")/*Audio speeding alert*/;
			selSpdAlert[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_SND);
			spdAlert[1] = Locale.get("guisetupsound.VisualSpeedingAlert")/*Visual speeding alert*/;
			selSpdAlert[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SPEEDALERT_VISUAL);
			spdAlert[2] = Locale.get("guisetupsound.WinterLimitsCctive")/*Winter limits active*/;
			selSpdAlert[2]=Configuration.getCfgBitState(Configuration.CFGBIT_MAXSPEED_WINTER);
			spdAlertGroup = new ChoiceGroup(Locale.get("guisetupsound.SpeedingAlert")/*Speeding alert*/, Choice.MULTIPLE, spdAlert ,null);
			spdAlertGroup.setSelectedFlags(selSpdAlert);
			append(spdAlertGroup);

			spdAlertTolerance = new TextField(Locale.get("guisetupsound.SpeedAlertTolerance")/*Speed alert tolerance*/,
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
