package net.sharenav.sharenav.ui;
/*
 * ShareNav - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.graphics.ProjFactory;
import de.enough.polish.util.Locale;

public class GuiMapFeatures extends Form implements CommandListener {
	// Groups
	private ChoiceGroup elemsGroup;
	private final	String [] elems = new String[11];
	private final boolean[] selElems = new boolean[11];

	private ChoiceGroup altInfosGroup;
	private final	String [] altInfos = new String[2];
	private final	boolean[] selAltInfos = new boolean[2];

	private ChoiceGroup rotationGroup;
	private	String [] rotation = new String[2];
	
	private ChoiceGroup modesGroup;
	private final	String [] modes = new String[3];
	private final	boolean[] selModes = new boolean[3];

	private TextField  tfBaseScale;
	
	private ChoiceGroup otherGroup;
	private final	String [] other = new String[2];
	private final	boolean[] selOther = new boolean[2];
	
	
	private Gauge gaugeDetailBoost; 
	private Gauge gaugeDetailBoostPOI; 

	// commands
	private static final Command CMD_APPLY = new Command(Locale.get("guimapfeatures.Apply")/*Apply*/, ShareNavMenu.BACK, 1);
	private static final Command CMD_SAVE = new Command(Locale.get("guimapfeatures.Save")/*Save*/, ShareNavMenu.OK, 2);
	//private static final Command CMD_CANCEL = new Command(Locale.get("generic.Cancel")/*Cancel*/, ShareNavMenu.BACK, 3);
	
	// other
	private final Trace parent;
	
	public GuiMapFeatures(Trace tr) {
		super(Locale.get("guimapfeatures.MapFeatures")/*Map features*/);
		this.parent = tr;
		try {
			// set choice texts and convert bits from render flag into selection states
			elems[0] = Locale.get("guimapfeatures.POIs")/*POIs*/;					selElems[0]=Configuration.getCfgBitState(Configuration.CFGBIT_POIS);
			elems[1] = Locale.get("guimapfeatures.POIlabels")/*POI labels*/; 			selElems[1]=Configuration.getCfgBitState(Configuration.CFGBIT_POITEXTS);
			elems[2] = Locale.get("guimapfeatures.Waylabels")/*Way labels*/; 			selElems[2]=Configuration.getCfgBitState(Configuration.CFGBIT_WAYTEXTS);
			elems[3] = Locale.get("guimapfeatures.Onewayarrows")/*Oneway arrows*/; 		selElems[3]=Configuration.getCfgBitState(Configuration.CFGBIT_ONEWAY_ARROWS);
			elems[4] = Locale.get("guimapfeatures.Areas")/*Areas*/; 				selElems[4]=Configuration.getCfgBitState(Configuration.CFGBIT_AREAS);
			elems[5] = Locale.get("guimapfeatures.AreaLabels")/*Area labels*/; 			selElems[5]=Configuration.getCfgBitState(Configuration.CFGBIT_AREATEXTS);
			elems[6] = Locale.get("guimapfeatures.Buildings")/*Buildings*/; 			selElems[6]=Configuration.getCfgBitState(Configuration.CFGBIT_BUILDINGS);
			elems[7] = Locale.get("guimapfeatures.BuildingLabels")/*Building labels*/; 		selElems[7]=Configuration.getCfgBitState(Configuration.CFGBIT_BUILDING_LABELS);
			elems[8] = Locale.get("guimapfeatures.WaypointLabels")/*Waypoint labels*/; 		selElems[8]=Configuration.getCfgBitState(Configuration.CFGBIT_WPTTEXTS);
			elems[9] = Locale.get("guimapfeatures.PlaceLabels")/*Place labels (cities, etc.)*/;	selElems[9]=Configuration.getCfgBitState(Configuration.CFGBIT_PLACETEXTS);
			elems[10] = Locale.get("guimapfeatures.DrawNonTravelModeWaysDarker")/*draw non-travelmode ways darker*/;	selElems[10]=Configuration.getCfgBitState(Configuration.CFGBIT_DRAW_NON_TRAVELMODE_WAYS_DARKER);
			elemsGroup = new ChoiceGroup(Locale.get("guimapfeatures.Elements")/*Elements*/, Choice.MULTIPLE, elems ,null);
			elemsGroup.setSelectedFlags(selElems);
			append(elemsGroup);
			
			altInfos[0] = Locale.get("guimapfeatures.LatLon")/*Lat/lon*/; 			selAltInfos[0]=Configuration.getCfgBitState(Configuration.CFGBIT_SHOWLATLON);
			altInfos[1] = Locale.get("guimapfeatures.TypeInformation")/*Type information*/; 	selAltInfos[1]=Configuration.getCfgBitState(Configuration.CFGBIT_SHOWWAYPOITYPE);
			altInfosGroup = new ChoiceGroup(Locale.get("guimapfeatures.AlternativeInfo")/*Alternative info*/, Choice.MULTIPLE, altInfos ,null);
			altInfosGroup.setSelectedFlags(selAltInfos);
			append(altInfosGroup);

			rotation = Configuration.projectionsString;
			rotationGroup = new ChoiceGroup(Locale.get("guimapfeatures.MapProjection")/*Map projection*/, Choice.EXCLUSIVE, rotation ,null);
			rotationGroup.setSelectedIndex((int) ProjFactory.getProj(), true);
			append(rotationGroup);			
			
			modes[0] = Locale.get("guimapfeatures.FullScreen")/*Full screen*/; 			selModes[0]=Configuration.getCfgBitState(Configuration.CFGBIT_FULLSCREEN);
			modes[1] = Locale.get("guimapfeatures.AutoZoom")/*Auto zoom*/; 	selModes[1]=Configuration.getCfgBitState(Configuration.CFGBIT_AUTOZOOM);
			modes[2] = Locale.get("guimapfeatures.RenderAsStreets")/*Render as streets*/; 	selModes[2]=Configuration.getCfgBitState(Configuration.CFGBIT_STREETRENDERMODE);
			modesGroup = new ChoiceGroup(Locale.get("guimapfeatures.Mode")/*Mode*/, Choice.MULTIPLE, modes ,null);
			modesGroup.setSelectedFlags(selModes);			
			append(modesGroup);

			tfBaseScale = new TextField(Locale.get("guimapfeatures.BaseZoomLevel")/*Base Zoom Level (23 = default)*/, Integer.toString(Configuration.getBaseScale()), 6, TextField.DECIMAL);
			append(tfBaseScale);
			
			other[0] = Locale.get("guimapfeatures.SaveMapPosition")/*Save map position on exit for next start*/;	selOther[0]=Configuration.getCfgBitState(Configuration.CFGBIT_AUTOSAVE_MAPPOS);
			other[1] = Locale.get("guimapfeatures.SaveDestinationPosition")/*Save destination position for next start*/;	selOther[1]=Configuration.getCfgBitState(Configuration.CFGBIT_AUTOSAVE_DESTPOS);
			otherGroup = new ChoiceGroup(Locale.get("guimapfeatures.Other")/*Other*/, Choice.MULTIPLE, other, null);
			otherGroup.setSelectedFlags(selOther);			
			append(otherGroup);
			
			gaugeDetailBoost = new Gauge(Locale.get("guimapfeatures.ZoomingWaysEarlier")/*Zooming: show ways earlier*/, true, 3, 0);
			append(gaugeDetailBoost);
			gaugeDetailBoost.setValue(Configuration.getDetailBoost());

			gaugeDetailBoostPOI = new Gauge(Locale.get("guimapfeatures.ZoomingPoisEarlier")/*Zooming: show POIs earlier*/, true, 3, 0);
			append(gaugeDetailBoostPOI);
			gaugeDetailBoostPOI.setValue(Configuration.getDetailBoostPOI());
			
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
	        Configuration.setCfgBitState(Configuration.CFGBIT_BUILDINGS, selElems[6], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_BUILDING_LABELS, selElems[7], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_WPTTEXTS, selElems[8], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_PLACETEXTS, selElems[9], setAsDefault);
	        Configuration.setCfgBitState(Configuration.CFGBIT_DRAW_NON_TRAVELMODE_WAYS_DARKER, selElems[10], setAsDefault);

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
			Configuration.setCfgBitState(Configuration.CFGBIT_AUTOZOOM, selModes[1], setAsDefault);
			Configuration.setCfgBitState(Configuration.CFGBIT_STREETRENDERMODE, selModes[2], setAsDefault);
			
			float oldRealBaseScale = Configuration.getRealBaseScale();
			Configuration.setBaseScale(
					(int) (Float.parseFloat(tfBaseScale.getString())) 
			);
			if (oldRealBaseScale != Configuration.getRealBaseScale()) {
				Legend.reReadLegend();
				Trace.getInstance().scale = Configuration.getRealBaseScale();
			}

			otherGroup.getSelectedFlags(selOther);
			Configuration.setCfgBitState(Configuration.CFGBIT_AUTOSAVE_MAPPOS, selOther[0], setAsDefault);
			Configuration.setCfgBitState(Configuration.CFGBIT_AUTOSAVE_DESTPOS, selOther[1], setAsDefault);
			
			Configuration.setDetailBoost(gaugeDetailBoost.getValue(), setAsDefault); 
			Configuration.setDetailBoostPOI(gaugeDetailBoostPOI.getValue(), setAsDefault); 

			parent.show();
			return;
		}
	}
	
	public void show() {
		ShareNav.getInstance().show(this);
	}

}
