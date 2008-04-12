package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;

import de.ueller.midlet.gps.data.PositionMark;

/*
 * GpsMid - Copyright (c) 2007 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
public class GuiWaypointSave extends Form implements CommandListener {

	private TextField fldName = new TextField("Name:", "", 20, TextField.ANY);
	private static final Command saveCmd = new Command("Save", Command.OK, 1);
	private static final Command backCmd = new Command("Back", Command.OK, 2);
	private Trace parent;
	private String name;
	private PositionMark waypt;

	public GuiWaypointSave(Trace tr, PositionMark waypt) {
		super("Enter Waypoint name");
		this.parent = tr;
		this.waypt = waypt;
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {
		// Set up this Displayable to listen to command events
		setCommandListener(this);
		// add the commands
		addCommand(backCmd);
		addCommand(saveCmd);
		this.append(fldName);
	}

	public void commandAction(Command cmd, Displayable displayable) {
		if (cmd == saveCmd) {
			name = fldName.getString();
			System.out.println("Name:" + name);
			waypt.displayName = name;			
			parent.gpx.addWayPt(waypt);				
			parent.show();
			return;
		}

		else if (cmd == backCmd) {
			parent.show();
	    	return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
		//Display.getDisplay(parent.getParent()).setCurrent(this);
	}
}
