package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.data.ProjFactory;

public class GuiMapFeatures extends Form implements CommandListener {
	// Groups
	private ChoiceGroup elemsGroup;
	private	String [] elems = new String[6];
	private boolean[] selElems = new boolean[6];

	private ChoiceGroup altInfosGroup;
	private	String [] altInfos = new String[2];
	private	boolean[] selAltInfos = new boolean[2];

	private ChoiceGroup rotationGroup;
	private	String [] rotation = new String[2];
	
	private ChoiceGroup modesGroup;
	private	String [] modes = new String[3];
	private	boolean[] selModes = new boolean[3];

	private ChoiceGroup otherGroup;
	private	String [] other = new String[2];
	private	boolean[] selOther = new boolean[2];
	
	
	private Gauge gaugeDetailBoost; 

	// commands
	private static final Command CMD_APPLY = new Command("Apply", Command.BACK, 1);
	private static final Command CMD_SAVE = new Command("Save", Command.ITEM, 2);
	//private static final Command CMD_CANCEL = new Command("Cancel", Command.CANCEL, 3);
	
	// other
	private Trace parent;
	private static Configuration config;
	
	public GuiMapFeatures(Trace tr) {
		super("Map features");
		config=GpsMid.getInstance().getConfig();
		this.parent = tr;
		try {
			// set choice texts and convert bits from render flag into selection states
			elems[0] = "POIs";					selElems[0]=config.getCfgBitState(config.CFGBIT_POIS);
			elems[1] = "POI labels"; 			selElems[1]=config.getCfgBitState(config.CFGBIT_POITEXTS);
			elems[2] = "Way labels"; 			selElems[2]=config.getCfgBitState(config.CFGBIT_WAYTEXTS);
			elems[3] = "Areas"; 				selElems[3]=config.getCfgBitState(config.CFGBIT_AREAS);
			elems[4] = "Area labels"; 			selElems[4]=config.getCfgBitState(config.CFGBIT_AREATEXTS);
			elems[5] = "Waypoint labels"; 		selElems[5]=config.getCfgBitState(config.CFGBIT_WPTTEXTS);
			elemsGroup = new ChoiceGroup("Elements", Choice.MULTIPLE, elems ,null);
			elemsGroup.setSelectedFlags(selElems);
			append(elemsGroup);
			
			altInfos[0] = "Lat/lon"; 			selAltInfos[0]=config.getCfgBitState(config.CFGBIT_SHOWLATLON);
			altInfos[1] = "Type information"; 	selAltInfos[1]=config.getCfgBitState(config.CFGBIT_SHOWWAYPOITYPE);
			altInfosGroup = new ChoiceGroup("Alternative Info", Choice.MULTIPLE, altInfos ,null);
			altInfosGroup.setSelectedFlags(selAltInfos);
			append(altInfosGroup);

			rotation[0] = "North Up";
			rotation[1] = "to Driving Direction";
			rotationGroup = new ChoiceGroup("Map Rotation", Choice.EXCLUSIVE, rotation ,null);
			rotationGroup.setSelectedIndex((int) ProjFactory.getProj(), true);
			append(rotationGroup);			
			
			modes[0] = "Full Screen"; 			selModes[0]=config.getCfgBitState(config.CFGBIT_FULLSCREEN);
			modes[1] = "Render as streets"; 	selModes[1]=config.getCfgBitState(config.CFGBIT_STREETRENDERMODE);
			modes[2] = "Routing help"; 	selModes[2]=config.getCfgBitState(config.CFGBIT_ROUTING_HELP);
			modesGroup = new ChoiceGroup("Mode", Choice.MULTIPLE, modes ,null);
			modesGroup.setSelectedFlags(selModes);			
			append(modesGroup);

			other[0] = "Show Point of Compass in rotated map";	selOther[0]=config.getCfgBitState(config.CFGBIT_SHOW_POINT_OF_COMPASS);
			other[1] = "Save map position on exit for next start";	selOther[1]=config.getCfgBitState(config.CFGBIT_AUTOSAVE_MAPPOS);
			otherGroup = new ChoiceGroup("Other", Choice.MULTIPLE, other ,null);
			otherGroup.setSelectedFlags(selOther);			
			append(otherGroup);
			
			gaugeDetailBoost = new Gauge("Increase Detail of lower Zoom Levels", true, 3, 0);
			append(gaugeDetailBoost);
			gaugeDetailBoost.setValue(config.getDetailBoost());
			
			addCommand(CMD_APPLY);
			addCommand(CMD_SAVE);
			//addCommand(CMD_CANCEL);
			
			// Set up this Displayable to listen to command events
			setCommandListener(this);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	

	public void commandAction(Command c, Displayable d) {

//		if (c == CMD_CANCEL) {			
//			parent.show();
//			return;
//		}

		if (c == CMD_APPLY || c == CMD_SAVE) {			
			// determine if changes should be written to recordstore
			boolean setAsDefault=(c==CMD_SAVE);
			
			// convert boolean array with selection states for renderOpts
			// to one flag with corresponding bits set
	        elemsGroup.getSelectedFlags(selElems);
			config.setCfgBitState(config.CFGBIT_POIS, selElems[0], setAsDefault);
			config.setCfgBitState(config.CFGBIT_POITEXTS, selElems[1], setAsDefault);
			config.setCfgBitState(config.CFGBIT_WAYTEXTS, selElems[2], setAsDefault);
			config.setCfgBitState(config.CFGBIT_AREAS, selElems[3], setAsDefault);
			config.setCfgBitState(config.CFGBIT_AREATEXTS, selElems[4], setAsDefault);
			config.setCfgBitState(config.CFGBIT_WPTTEXTS, selElems[5], setAsDefault);

			altInfosGroup.getSelectedFlags(selAltInfos);
			config.setCfgBitState(config.CFGBIT_SHOWLATLON, selAltInfos[0], setAsDefault);
			config.setCfgBitState(config.CFGBIT_SHOWWAYPOITYPE, selAltInfos[1], setAsDefault);

			byte t = (byte) rotationGroup.getSelectedIndex();
			ProjFactory.setProj(t);
			if (setAsDefault) {
				config.setProjTypeDefault(t);
			}
			
			modesGroup.getSelectedFlags(selModes);
			config.setCfgBitState(config.CFGBIT_FULLSCREEN, selModes[0], setAsDefault);
			config.setCfgBitState(config.CFGBIT_STREETRENDERMODE, selModes[1], setAsDefault);
			config.setCfgBitState(config.CFGBIT_ROUTING_HELP, selModes[2], setAsDefault);
			
			otherGroup.getSelectedFlags(selOther);
			config.setCfgBitState(config.CFGBIT_AUTOSAVE_MAPPOS, selOther[0], setAsDefault);
			config.setCfgBitState(config.CFGBIT_SHOW_POINT_OF_COMPASS, selOther[1], setAsDefault);
			
			config.setDetailBoost(gaugeDetailBoost.getValue(), setAsDefault); 

			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
