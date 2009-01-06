package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 mbaeurle at users dot sourceforge dot net 
 * See Copying
 * Based on GuiWaypointSave.java by Kai Krueger apm at users dot sourceforge dot net
 */

import javax.microedition.lcdui.*;

import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Proj2DMoveUp;

/*
 * GUI to enter a waypoint with coordinates.
 * The gimmick is that you can enter decimal places in any field.
 * So you can either specify 48° 30' 45" or 48° 30.75' or 48.5125°
 * and leave the other fields empty. It will all be put together correctly.
 * It also checks if the values are in range to prevent the creation of 
 * waypoints that are outside the coordinate system.
 */
public class GuiWaypointEnter extends Form implements CommandListener {
	private TextField fldName;
	private TextField fldLatDeg = new TextField("Lat deg:", "", 10, TextField.DECIMAL);
	private TextField fldLatMin = new TextField("Lat min:", "", 10, TextField.DECIMAL);
	private TextField fldLatSec = new TextField("Lat sec:", "", 10, TextField.DECIMAL);
	private TextField fldLonDeg = new TextField("Lon deg:", "", 10, TextField.DECIMAL);
	private TextField fldLonMin = new TextField("Lon min:", "", 10, TextField.DECIMAL);
	private TextField fldLonSec = new TextField("Lon sec:", "", 10, TextField.DECIMAL);
	private static final Command saveCmd = new Command("Save", Command.OK, 1);
	private static final Command backCmd = new Command("Back", Command.BACK, 2);
	private Trace parent;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointEnter.class,Logger.TRACE);

	public GuiWaypointEnter(Trace tr) {
		super("Enter Waypoint");
		this.parent = tr;
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {
		fldName = new TextField("Name:", "", parent.getConfig().MAX_WAYPOINTNAME_LENGTH, TextField.ANY);
		// Set up this Displayable to listen to command events
		setCommandListener(this);
		// add the commands
		addCommand(backCmd);
		addCommand(saveCmd);
		this.append(fldName);
		this.append(fldLatDeg);
		this.append(fldLatMin);
		this.append(fldLatSec);
		this.append(fldLonDeg);
		this.append(fldLonMin);
		this.append(fldLonSec);
	}

	public void commandAction(Command cmd, Displayable displayable) {
		if (cmd == saveCmd) {
			// parseFloat() doesn't like empty strings...
			float latDeg = 0.0f;
			if (fldLatDeg.getString().length() != 0) {
				latDeg = Float.parseFloat( fldLatDeg.getString() );
			}
			float latMin = 0.0f;
			if (fldLatMin.getString().length() != 0) {
				latMin = Float.parseFloat( fldLatMin.getString() );
			}
			float latSec = 0.0f;
			if (fldLatSec.getString().length() != 0) {
				latSec = Float.parseFloat( fldLatSec.getString() );
			}
			float lonDeg = 0.0f;
			if (fldLonDeg.getString().length() != 0) {
				lonDeg = Float.parseFloat( fldLonDeg.getString() );
			}
			float lonMin = 0.0f;
			if (fldLonMin.getString().length() != 0) {
				lonMin = Float.parseFloat( fldLonMin.getString() );
			}
			float lonSec = 0.0f;
			if (fldLonSec.getString().length() != 0) {
				lonSec = Float.parseFloat( fldLonSec.getString() );
			}
			if (   ((latDeg >= -90.0f) && (latDeg <= 90.0f))
				&& ((latMin >= 0.0f) && (latMin < 60.0f))
				&& ((latSec >= 0.0f) && (latSec < 60.0f))
				&& ((lonDeg >= -180.0f) && (lonDeg <= 180.0f))
				&& ((lonMin >= 0.0f) && (lonMin < 60.0f))
				&& ((lonSec >= 0.0f) && (lonSec < 60.0f)) ) 
			{
				float lat, lon;

				// You can only enter a negative sign at the degrees to specify
				// North (+) or South (-), not at the minutes or seconds 
				// (would be too confusing and cumbersomely otherwise).
				// Same at longitude for East (+) or West (-).
				if (latDeg >= 0.0f) {
					lat = latDeg + (latMin / 60.0f) + (latSec / 3600.0f);
				} else {
					lat = latDeg - (latMin / 60.0f) - (latSec / 3600.0f);
				}
				if (lonDeg >= 0.0f) {
					lon = lonDeg + (lonMin / 60.0f) + (lonSec / 3600.0f);
				} else {
					lon = lonDeg - (lonMin / 60.0f) - (lonSec / 3600.0f);
				}
				PositionMark waypt = new PositionMark(lat,lon);
				// The sums could now be out of range, so check them as well.
				// Quite academic, but still possible. :-)
				if (   (waypt.lat >= -90.0f) && (waypt.lat <= 90.0f)
					&& (waypt.lon >= -180.0f) && (waypt.lon <= 180.0f) )
				{
					waypt.lat = waypt.lat / MoreMath.FAC_RADTODEC;
					waypt.lon = waypt.lon / MoreMath.FAC_RADTODEC;
					waypt.displayName = fldName.getString();
					logger.info("Waypoint entered: " + waypt.toString());
					parent.gpx.addWayPt(waypt);				
					parent.show();
				} else {
					GpsMid.getInstance().alert("Error", "Please enter valid coordinates", 5000);
				}
			} else {
				GpsMid.getInstance().alert("Error", "Please enter valid coordinates", 5000);
			}
			return;
		} else if (cmd == backCmd) {
			parent.show();
	    	return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}
}
