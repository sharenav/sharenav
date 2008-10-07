package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 sk750 at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;

import de.ueller.gpsMid.mapData.SingleTile;

public class GuiOverviewElement extends Form implements CommandListener {
	private final Form	OverviewElement = new Form("Overview on Map");
	private ChoiceGroup ovElSelectionCG;

	// commands
	private static final Command CMD_OK = new Command("Ok", Command.OK, 1);
	private static final Command CMD_OFF = new Command("Off", Command.ITEM, 2);
	
	// other
	private Trace parent;
	
	public GuiOverviewElement(Trace tr) {
		super("Overview on Map");
		this.parent = tr;
		try {
			ovElSelectionCG = new ChoiceGroup("Show this POI: ", ChoiceGroup.EXCLUSIVE);
			for (byte i = 1; i < parent.pc.c.getMaxType(); i++) {				
				if (parent.pc.c.isNodeHideable(i)) {
					ovElSelectionCG.append(parent.pc.c.getNodeTypeDesc(i), parent.pc.c.getNodeSearchImage(i));
				}
			}
			append(ovElSelectionCG);

			addCommand(CMD_OK);
			addCommand(CMD_OFF);
			
			// Set up this Displayable to listen to command events
			setCommandListener(this);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void commandAction(Command c, Displayable d) {
		if (c == CMD_OK) {			
			// determine poi type of selected element
			byte ovElSel = (byte) (ovElSelectionCG.getSelectedIndex() + 1);
			byte ovElType = 0;
			for (byte i = 1; i < parent.pc.c.getMaxType(); i++) {				
				if (parent.pc.c.isNodeHideable(i)) {
					ovElType++;
					if(ovElSel == ovElType) {
						ovElType = i;
						break;
					}
				}
			}
			
			SingleTile.setOverviewElementType(ovElType);
			parent.show();
			return;
		}
		if (c == CMD_OFF) {			
			SingleTile.setOverviewElementType((byte)0);
			parent.show();
			return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}

}
