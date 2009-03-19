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
	private	String [] elems = new String[8];
	private boolean[] selElems = new boolean[8];

	private ChoiceGroup altInfosGroup;
	private	String [] altInfos = new String[2];
	private	boolean[] selAltInfos = new boolean[2];

	private ChoiceGroup rotationGroup;
	private	String [] rotation = new String[2];
	
	private ChoiceGroup modesGroup;
	private	String [] modes = new String[3];
	private	boolean[] selModes = new boolean[3];

	private ChoiceGroup otherGroup;
	private	String [] other = new String[3];
	private	boolean[] selOther = new boolean[3];
	
	
	private Gauge gaugeDetailBoost; 

	// commands
	private static final Command CMD_APPLY = new Command("Apply", Command.BACK, 1);
	private static final Command CMD_SAVE = new Command("Save", Command.ITEM, 2);
	//private static final Command CMD_CANCEL = new Command("Cancel", Command.CANCEL, 3);
	
	// other
	private Trace parent;
	
	public GuiMapFeatures(Trace tr) {
		super("Map features");
		this.parent = tr;
		try {
			// set choice texts and convert bits from render flag into selection states
			elems[0] = "POIs";					selElems[0]=Configuration.getCfgBitState(Configuration.CFGBIT_POIS);
			elems[1] = "POI labels"; 			selElems[1]=Configuration.getCfgBitState(Configuration.CFGBIT_POITEXTS);
			elems[2] = "Way labels"; 			selElems[2]=Configuration.getCfgBitState(Configuration.CFGBIT_WAYTEXTS);
			elems[3] = "Oneway arrows"; 		selElems[3]=Configuration.getCfgBitState(Configuration.CFGBIT_ONEWAY_ARROWS);
			elems[4] = "Areas"; 				selElems[4]=Configuration.getCfgBitState(Configuration.CFGBIT_AREAS);
			elems[5] = "Area labels"; 			selElems[5]=Configuration.getCfgBitState(Configuration.CFGBIT_AREATEXTS);
			elems[6] = "Waypoint labels"; 		selElems[6]=Configuration.getCfgBitState(Configuration.CFGBIT_WPTTEXTS);
			elems[7] = "Place labels (cities, etc.)";	selElems[7]=Configuration.getCfgBitState(Configuration.CFGBIT_PLACETEXTS);
			elemsGroup = new ChoiceGroup("Elements", Choice.MULTIPLE, elems ,null);
			elemsGroup.setSelectedFlags(selElems);
			append(elemsGroup);
			
			altInfos[0] = "Lat/lon"; 			selAltInfos[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SHOWLATLON);
			altInfos[1] = "Type information"; 	selAltInfos[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE);
			altInfosGroup = new ChoiceGroup("Alternative info", Choice.MULTIPLE, altInfos ,null);
			altInfosGroup.setSelectedFlags(selAltInfos);
			append(altInfosGroup);

			rotation[0] = "North up";
			rotation[1] = "to driving direction";
			rotationGroup = new ChoiceGroup("Map rotation", Choice.EXCLUSIVE, rotation ,null);
			rotationGroup.setSelectedIndex((int) ProjFactory.getProj(), true);
			append(rotationGroup);			
			
			modes[0] = "Full screen"; 			selModes[0]=Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN);
			modes[1] = "Render as streets"; 	selModes[1]=Configuration.getCfgBitState(Configuration.CFGBIT_STREETRENDERMODE);
			modes[2] = "Routing help"; 	selModes[2]=Configuration.getCfgBitState(Configuration.CFGBIT_ROUTING_HELP);
			modesGroup = new ChoiceGroup("Mode", Choice.MULTIPLE, modes ,null);
			modesGroup.setSelectedFlags(selModes);			
			append(modesGroup);

			other[0] = "Show point of compass in rotated map";	selOther[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS);
			other[1] = "Show scale bar";	selOther[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SHOW_SCALE_BAR);
			other[2] = "Save map position on exit for next start";	selOther[2]=Configuration.getCfgBitState(Configuration.CFGBIT_AUTOSAVE_MAPPOS);
			otherGroup = new ChoiceGroup("Other", Choice.MULTIPLE, other, null);
			otherGroup.setSelectedFlags(selOther);			
			append(otherGroup);
			
			gaugeDetailBoost = new Gauge("Increase Detail of lower Zoom Levels", true, 3, 0);
			append(gaugeDetailBoost);
			gaugeDetailBoost.setValue(Configuration.getDetailBoost());
			
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
		if (c == CMD_APPLY || c == CMD_SAVE) {			
			// determine if changes should be written to recordstore
			boolean setAsDefault = (c == CMD_SAVE);
			
			// convert boolean array with selection states for renderOpts
			// to one flag with corresponding bits set
	        elemsGroup.getSelectedFlags(selElems);
	        Configuration.setCfgBitState(Configuration.CFGBIT_POIS, selElems[0], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_POITEXTS, selElems[1], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_WAYTEXTS, selElems[2], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_ONEWAY_ARROWS, selElems[3], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_AREAS, selElems[4], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_AREATEXTS, selElems[5], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_WPTTEXTS, selElems[6], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_PLACETEXTS, selElems[7], setAsDefault);

			altInfosGroup.getSelectedFlags(selAltInfos);
			Configuration.setCfgBitState(Configuration.CFGBIT_SHOWLATLON, selAltInfos[0], setAsDefault);
			Configuration.setCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE, selAltInfos[1], setAsDefault);

			byte t = (byte) rotationGroup.getSelectedIndex();
			ProjFactory.setProj(t);
			if (setAsDefault) {
				Configuration.setProjTypeDefault(t);
			}
			
			modesGroup.getSelectedFlags(selModes);
			Configuration.setCfgBitState(Configuration.CFGBIT_FULLSCREEN, selModes[0], setAsDefault);
			Configuration.setCfgBitState(Configuration.CFGBIT_STREETRENDERMODE, selModes[1], setAsDefault);
			Configuration.setCfgBitState(Configuration.CFGBIT_ROUTING_HELP, selModes[2], setAsDefault);
			
			otherGroup.getSelectedFlags(selOther);
			Configuration.setCfgBitState(Configuration.CFGBIT_SHOW_POINT_OF_COMPASS, selOther[0], setAsDefault);
			Configuration.setCfgBitState(Configuration.CFGBIT_SHOW_SCALE_BAR, selOther[1], setAsDefault);
			Configuration.setCfgBitState(Configuration.CFGBIT_AUTOSAVE_MAPPOS, selOther[2], setAsDefault);
			
			Configuration.setDetailBoost(gaugeDetailBoost.getValue(), setAsDefault); 

			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
