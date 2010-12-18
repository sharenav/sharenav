/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.data.PositionMark;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;


public class GuiWaypointSave extends Form implements CommandListener {
	private TextField fldName;
	private TextField fldEle;
	private ChoiceGroup cg;
	private static final Command saveCmd = new Command(Locale.get("guiwaypointsave.Save")/*Save*/, Command.OK, 1);
	private static final Command backCmd = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 2);
	
	private final Trace parent;
	private String name;
	private String ele;
	private PositionMark waypt;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointSave.class,Logger.TRACE);

	public GuiWaypointSave(Trace tr) {
		super(Locale.get("guiwaypointsave.SaveWaypoint")/*Save Waypoint*/);
		this.parent = tr;
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {
		fldName = new TextField(Locale.get("guiwaypointsave.Name")/*Name:*/, "", 
				Configuration.MAX_WAYPOINTNAME_LENGTH, TextField.ANY);
		fldEle = new TextField(Locale.get("guiwaypointsave.AltitudeInM")/*Altitude (in m):*/, 
				"", 5, TextField.NUMERIC);
		cg = new ChoiceGroup(Locale.get("guiwaypointsave.Settings")/*Settings*/, Choice.MULTIPLE);
		cg.append(Locale.get("guiwaypointsave.Favorite")/*Favorite*/, null);
		cg.append(Locale.get("guiwaypointsave.RecenterToGPS")/*Recenter to GPS after saving*/, null);
		
		// Set up this Displayable to listen to command events
		setCommandListener(this);
		// add the commands
		addCommand(backCmd);
		addCommand(saveCmd);
		this.append(fldName);
		this.append(Locale.get("guiwaypointsave.OptionalData")/*Optional data*/);
		this.append(fldEle);		
		this.append(cg);
	}
	
	public void setData(PositionMark posMark)
	{
		this.waypt = posMark;
		this.name = "";
		fldName.setString(name);
		
		//only set default altitude if gps.ele is valid. 
		if (waypt.ele != PositionMark.INVALID_ELEVATION) {
			fldEle.setString(String.valueOf(waypt.ele));
		} else {
			fldEle.setString("");			
		}
	}

	public void commandAction(Command cmd, Displayable displayable) {
		if (cmd == saveCmd) {
			name = fldName.getString();
			if (cg.isSelected(0) && !name.endsWith("*")) {
				name += "*";
			}
			
			ele  = fldEle.getString();
			logger.info("Saving waypoint with name: " + name + " ele: " + ele);
			waypt.displayName = name;
			Thread adder = new Thread(new Runnable() {
				public void run()
				{
					try {
						// Use the elevation from textfield, if not "" 
						waypt.ele = Integer.parseInt(ele);
					} catch (NumberFormatException e) {
						waypt.ele = PositionMark.INVALID_ELEVATION;
					}
					
					parent.gpx.addWayPt(waypt);

					// Wait a bit before displaying the map again. Hopefully
					// this avoids the sporadic freezing after saving a waypoint.
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
					}

					// Recenter GPS after saving a Waypoint if this option is selected
					if (cg.isSelected(1)) {
						parent.gpsRecenter = true;
						parent.newDataReady();
					}
					parent.show();				
				}
			} );
			adder.setPriority(Thread.MAX_PRIORITY);
			adder.start();
			return;
		}
		else if (cmd == backCmd) {
			parent.show();
	    	return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}
}
