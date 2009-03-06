package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Proj2DMoveUp;


public class GuiWaypointSave extends Form implements CommandListener {
	private TextField fldName;
	private static final Command saveCmd = new Command("Save", Command.OK, 1);
	private static final Command backCmd = new Command("Back", Command.BACK, 2);
	private Trace parent;
	private String name;
	private PositionMark waypt;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointSave.class,Logger.TRACE);

	public GuiWaypointSave(Trace tr) {
		super("Enter Waypoint name");
		this.parent = tr;
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {
		fldName = new TextField("Name:", "", Configuration.MAX_WAYPOINTNAME_LENGTH, TextField.ANY);
		// Set up this Displayable to listen to command events
		setCommandListener(this);
		// add the commands
		addCommand(backCmd);
		addCommand(saveCmd);
		this.append(fldName);
	}
	
	public void setData(PositionMark pos)
	{
		this.waypt = pos;
		this.name = "";
		fldName.setString(name);
	}

	public void commandAction(Command cmd, Displayable displayable) {
		if (cmd == saveCmd) {
			name = fldName.getString();
			logger.info("Saving waypoint with name: " + name);
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
