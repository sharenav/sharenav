package net.sharenav.sharenav.ui;

/*
 * ShareNav - Copyright (c) 2008 mbaeurle at users dot sourceforge dot net 
 * See Copying
 * Based on GuiWaypointSave.java by Kai Krueger apm at users dot sourceforge dot net
 */

import javax.microedition.lcdui.*;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.sharenav.graphics.Proj2DMoveUp;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;

import de.enough.polish.util.Locale;

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
	// really should be TextField.DECIMAL, but as many platforms have bugs, work around them with TextField.ANY
	private TextField fldLatDeg = new TextField(Locale.get("guiwaypointenter.LatDegOrSMS")/*Lat deg or received SMS:*/, "", 1024, TextField.ANY); // can also be used to paste complete received position SMS 
	private TextField fldLatMin = new TextField(Locale.get("guiwaypointenter.LatMin")/*Lat min:*/, "", 10, TextField.ANY);
	private TextField fldLatSec = new TextField(Locale.get("guiwaypointenter.LatSec")/*Lat sec:*/, "", 10, TextField.ANY);
	private TextField fldLonDeg = new TextField(Locale.get("guiwaypointenter.LonDeg")/*Lon deg:*/, "", 10, TextField.ANY);
	private TextField fldLonMin = new TextField(Locale.get("guiwaypointenter.LonMin")/*Lon min:*/, "", 10, TextField.ANY);
	private TextField fldLonSec = new TextField(Locale.get("guiwaypointenter.LonSec")/*Lon sec:*/, "", 10, TextField.ANY);
	private static final Command saveCmd = new Command(Locale.get("guiwaypointenter.Save")/*Save*/, Command.OK, 1);
	private static final Command backCmd = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 2);
	private Trace parent;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointEnter.class,Logger.TRACE);

	public GuiWaypointEnter(Trace tr) {
		super(Locale.get("guiwaypointenter.EnterWaypoint")/*Enter Waypoint*/);
		this.parent = tr;
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {
		fldName = new TextField(Locale.get("guiwaypointenter.Name")/*Name:*/, "", Configuration.MAX_WAYPOINTNAME_LENGTH, TextField.ANY);
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
			String s;
			// parseFloat() doesn't like empty strings...
			float latDeg = 0.0f;
			float lonDeg = 0.0f;
			s = fldLatDeg.getString();
			if (s.length() != 0) {
				// allow to paste gpx format as well by converting it to SMS format before
				StringBuffer sb = new StringBuffer(s);
				int i = s.indexOf("lat=");
				if (i >= 0) {
					sb.setCharAt(i + 3, ':');
				}
				i = s.indexOf("lon=");
				if (i >= 0) {
					sb.setCharAt(i + 3, ':');
				}
				i = sb.toString().indexOf("'");
				while (i >= 0) {
					sb.deleteCharAt(i);
					i = sb.toString().indexOf("'");					
				}
				s = sb.toString().toLowerCase() ;
				// parse pasted received position SMS in format, e. g. lat: 49.1234 lon: 11.56789
				if (s.indexOf("lat:") >= 0 && s.indexOf("lon:") >= 0) {
					int begin = s.indexOf("lat:") + 4;
					int end = begin + 4;
					// skip leading spaces before searching for end
					while (end < s.length() && s.charAt(end) == ' ') {
						end++;
					}
					final String floatChars = "0123456789.-";
					// search end of number
					while (end < s.length() && floatChars.indexOf(s.substring(end, end + 1)) >= 0) {
						end++;
					}
					latDeg = Float.parseFloat( s.substring(begin, end).trim() );
					begin = s.indexOf("lon:") + 4;
					end = begin + 4;
					// skip leading spaces before searching for end
					while (end < s.length() && s.charAt(end) == ' ') {
						end++;
					}
					// search end of number
					while (end < s.length() && floatChars.indexOf(s.substring(end, end + 1)) >= 0) {
						end++;
					}
					lonDeg = Float.parseFloat( s.substring(begin, end).trim() );

				} else {
					latDeg = Float.parseFloat( s );
				}
			}
			float latMin = 0.0f;
			if (fldLatMin.getString().length() != 0) {
				latMin = Float.parseFloat( fldLatMin.getString() );
			}
			float latSec = 0.0f;
			if (fldLatSec.getString().length() != 0) {
				latSec = Float.parseFloat( fldLatSec.getString() );
			}
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
					ShareNav.getInstance().alert(Locale.get("guiwaypointenter.Error")/*Error*/, Locale.get("guiwaypointenter.EnterValidCoordinates")/*Please enter valid coordinates*/, 5000);
				}
			} else {
				ShareNav.getInstance().alert(Locale.get("guiwaypointenter.Error")/*Error*/, Locale.get("guiwaypointenter.EnterValidCoordinates")/*Please enter valid coordinates*/, 5000);
			}
			return;
		} else if (cmd == backCmd) {
			parent.show();
	    	return;
		}
	}
	
	public void show() {
		ShareNav.getInstance().show(this);
	}
}
