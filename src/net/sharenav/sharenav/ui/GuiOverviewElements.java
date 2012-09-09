package net.sharenav.sharenav.ui;
/*
 * ShareNav - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.graphics.ImageCollector;
import net.sharenav.sharenav.mapdata.WayDescription;
import de.enough.polish.util.Locale;

public class GuiOverviewElements extends Form implements CommandListener, ItemStateListener {
	private ChoiceGroup ovElGroupCG;
	private ChoiceGroup ovElNameRequirementCG;
	private ChoiceGroup ovElHideOtherCG;

	private TextField fldNamePart; 
	
	private ChoiceGroup ovElSelectionCG;

	private static boolean[] showOther = new boolean[3];
	private static byte[] nameRequirement = new byte[3];
	private static byte ovElGroupNr = 0;
	
	// commands
	private static final Command CMD_OK = new Command(Locale.get("generic.OK")/*Ok*/, ShareNavMenu.OK, 1);
	// FIXME add cancel
	private static final Command CMD_OFF = new Command(Locale.get("generic.Off")/*Off*/, Command.ITEM, 2);
	
	// other
	private Trace parent;
	
	private int frmMaxEl = 0;
	private boolean variableGroupsAdded = false;
	private boolean namePartFieldAdded = false;
	private boolean hideOtherGroupAdded = false;
		
	private final static String[] ovElGroupNamesAttached = {
		Locale.get("guioverviewelements.POIsAttached")/*POIs*/,
		Locale.get("guioverviewelements.AreasAttached")/*Areas*/,
		Locale.get("guioverviewelements.WaysAttached")/*Ways*/
	};

	public GuiOverviewElements(Trace tr) {
		super(Locale.get("guioverviewelements.OverviewFilterMap")/*Overview/Filter Map*/);
		this.parent = tr;
		try {
			ovElGroupCG = new ChoiceGroup(Locale.get("guioverviewelements.ElementType")/*Element Type: */, ChoiceGroup.EXCLUSIVE);
			ovElGroupCG.append(Locale.get("guioverviewelements.POIs")/*POIs*/, null);
			ovElGroupCG.append(Locale.get("guioverviewelements.Areas")/*Areas*/, null);
			ovElGroupCG.append(Locale.get("guioverviewelements.Ways")/*Ways*/, null);
			ovElGroupCG.setSelectedIndex(ovElGroupNr, true);
			append(ovElGroupCG);
			setItemStateListener(this);
			
			variableGroupsAdded = false;
			itemStateChanged(ovElGroupCG);

			addCommand(CMD_OK);
			addCommand(CMD_OFF);
			
			// Set up this Displayable to listen to command events
			setCommandListener(this);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void applyElGroupElementStates() {
		short count = 0;
		byte nonOverviewMode = Legend.OM_SHOWNORMAL;
		byte overviewMode = Legend.OM_OVERVIEW;
		byte nameReq = Legend.OM_NAME_ALL;
		
		nameRequirement[ovElGroupNr] = (byte) ovElNameRequirementCG.getSelectedIndex();
		switch (nameRequirement[ovElGroupNr]) {
			case 1:
				nameReq = Legend.OM_NO_NAME;				
				break;
			case 2:				
				nameReq = Legend.OM_WITH_NAME;				
				break;
			case 3:				
				nameReq = Legend.OM_WITH_NAMEPART;				
				break;
		}
		if (namePartFieldAdded) {
			Legend.set0Poi1Area2WayNamePart(ovElGroupNr, fldNamePart.getString());
		}
		if (hideOtherGroupAdded) {
			showOther[ovElGroupNr] = ovElHideOtherCG.isSelected(0);
		}
		// default to put all elements in "silent" overview mode
		// if there's a name requirement but no overview element at all selected
		if (nameReq != Legend.OM_NAME_ALL) {
			nonOverviewMode = Legend.OM_OVERVIEW2;
		}
		if ( !showOther[ovElGroupNr] || nameReq != Legend.OM_NAME_ALL) {
			// only hide non-overview elements if at least one overview element is selected
			// This is implemented so because otherwise simple switching between POIs, areas and ways would
			// cause to disappear all the elements in the list unintentionally
			for(short i = 0; i < ovElSelectionCG.size(); i++ ) {
				if (ovElSelectionCG.isSelected(i)) {
					nonOverviewMode = Legend.OM_HIDE;
					break;
				}
			}
		}
		nonOverviewMode |= nameReq;
		overviewMode |= nameReq;
		
		switch (ovElGroupNr) {
			case 0:
				// save overview mode state to node description
				for (short i = 1; i < Legend.getMaxType(); i++) {				
					if (Legend.isNodeHideable(i)) {
						Legend.setNodeOverviewMode(i, ovElSelectionCG.isSelected(count)?overviewMode:nonOverviewMode);
						count++;
					}
				}
				break;
			case 1:
				// save overview mode state to 'area' description
				for (short i = 1; i < Legend.getMaxWayType(); i++) {				
					WayDescription w = Legend.getWayDescription(i);
					if (w.isArea && Legend.isWayHideable(i) ) {
						Legend.setWayOverviewMode(i, ovElSelectionCG.isSelected(count)?overviewMode:nonOverviewMode);
						count++;
					}
				}
				break;
			case 2:
				// save overview mode state to way description
				for (short i = 1; i < Legend.getMaxWayType(); i++) {				
					WayDescription w = Legend.getWayDescription(i);
					if (!w.isArea && Legend.isWayHideable(i) ) {
						Legend.setWayOverviewMode(i, ovElSelectionCG.isSelected(count)?overviewMode:nonOverviewMode);
						count++;
					}
				}
				break;
		}
		
	}
	
	
	public void commandAction(Command c, Displayable d) {
		if (c == CMD_OK) {				
			ImageCollector.overviewTileScaleBoost = 2.25f;
			ovElGroupNr = (byte) ovElGroupCG.getSelectedIndex();
			applyElGroupElementStates();
			parent.show();
			return;
		}
		if (c == CMD_OFF) {			
			ImageCollector.overviewTileScaleBoost = 1.0f;
			Legend.clearAllNodesOverviewMode();
			Legend.clearAllWaysOverviewMode();
			parent.show();
			return;
		}
	}
	
	public void itemStateChanged(Item item) {
		if (item == ovElGroupCG) {			
			// only delete variable Choice groups if they were added
			if (variableGroupsAdded) {
				applyElGroupElementStates();
				delete(frmMaxEl); // element list				
				frmMaxEl--;
				delete(1); // nameReq
				frmMaxEl--;
			}

			short count=0;

			// Warning: do not move this up - it must be after applyElGroupElementStates()
			ovElGroupNr = (byte) ovElGroupCG.getSelectedIndex();
			String ovElGroupName = ovElGroupNamesAttached[ovElGroupNr]; 

			// set NameRequirement state in form
			ovElNameRequirementCG = new ChoiceGroup(Locale.get("guioverviewelements.NameCheck")/*Name Check*/, ChoiceGroup.EXCLUSIVE);
			ovElNameRequirementCG.append(Locale.get("generic.Off")/*off*/, null);
			ovElNameRequirementCG.append(Locale.get("guioverviewelements.onlyunnamed")/*only unnamed */ + ovElGroupName, null);
			ovElNameRequirementCG.append(Locale.get("guioverviewelements.onlynamed")/*only named */ + ovElGroupName, null);
			ovElNameRequirementCG.append(ovElGroupName + Locale.get("guioverviewelements.containing")/* containing...*/, null);
			ovElNameRequirementCG.setSelectedIndex(nameRequirement[ovElGroupNr], true); 

			// set None-Overview state in form
			ovElHideOtherCG = new ChoiceGroup(Locale.get("guioverviewelements.NonOverview")/*Non-Overview */ + ovElGroupName, ChoiceGroup.EXCLUSIVE);
			ovElHideOtherCG.append(Locale.get("guioverviewelements.ShowNormally")/*Show normally*/, null);
			ovElHideOtherCG.append(Locale.get("guioverviewelements.FilterOut")/*Filter out*/, null);			
			if (showOther[ovElGroupNr]) {
				ovElHideOtherCG.setSelectedIndex(0, true);
			} else {
				ovElHideOtherCG.setSelectedIndex(1, true);				
			}

			ovElSelectionCG = new ChoiceGroup(Locale.get("guioverviewelements.Overview")/*Overview */ + ovElGroupName, ChoiceGroup.MULTIPLE);
			switch (ovElGroupNr) {
				case 0:
					// set POI overview states in form				
					for (short i = 1; i < Legend.getMaxType(); i++) {				
						if (Legend.isNodeHideable(i)) {
							ovElSelectionCG.append(Legend.getNodeTypeDesc(i), Legend.getNodeSearchImage(i));
							ovElSelectionCG.setSelectedIndex(count, ((Legend.getNodeOverviewMode(i) & Legend.OM_MODE_MASK) == Legend.OM_OVERVIEW) );
							count++;
						}
					}
					break;
				case 1:
					// set Area overview states in form
					for (short i = 1; i < Legend.getMaxWayType(); i++) {				
						WayDescription w = Legend.getWayDescription(i);
						if (w.isArea && Legend.isWayHideable(i) ) {
							ovElSelectionCG.append(w.description, Legend.getAreaSearchImage(i));
							ovElSelectionCG.setSelectedIndex(count, ((Legend.getWayOverviewMode(i) & Legend.OM_MODE_MASK) == Legend.OM_OVERVIEW) );
							count++;
						}
					}
					break;
				case 2:
					// set Way overview  states in form
					for (short i = 1; i < Legend.getMaxWayType(); i++) {				
						WayDescription w = Legend.getWayDescription(i);
						if (!w.isArea && Legend.isWayHideable(i) ) {
							ovElSelectionCG.append(w.description, Legend.getWaySearchImage(i));
							ovElSelectionCG.setSelectedIndex(count, ((Legend.getWayOverviewMode(i) & Legend.OM_MODE_MASK) == Legend.OM_OVERVIEW) );
							count++;
						}
					}
					break;
			}
			append(ovElSelectionCG);
			frmMaxEl++;
			insert(1, ovElNameRequirementCG);
			frmMaxEl++;
			variableGroupsAdded = true;
			ovElGroupNr = (byte) ovElGroupCG.getSelectedIndex();
		}
		ovElGroupNr = (byte) ovElGroupCG.getSelectedIndex();
		if (item == ovElNameRequirementCG || item == ovElGroupCG) {
			if (hideOtherGroupAdded) {
				frmMaxEl--;
				delete(frmMaxEl);
				hideOtherGroupAdded = false;
			}
			if ((byte) ovElNameRequirementCG.getSelectedIndex() == 0) {
				insert(frmMaxEl, ovElHideOtherCG);
				frmMaxEl++;
				hideOtherGroupAdded = true;
			}
			if (namePartFieldAdded) {
				delete(2);
				frmMaxEl--;
				namePartFieldAdded = false;
			}
			if ((byte) ovElNameRequirementCG.getSelectedIndex() == 3) { 
				fldNamePart = new TextField(Locale.get("guioverviewelements.thisNamePart")/*...this name part:*/, 
						Legend.get0Poi1Area2WayNamePart(ovElGroupNr), 
						20, TextField.ANY);
				insert(2, fldNamePart);
				frmMaxEl++;
				namePartFieldAdded = true;
			}
		}
	}
	
	public void show() {
		ShareNav.getInstance().show(this);
	}

}
