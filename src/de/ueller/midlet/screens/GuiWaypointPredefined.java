/*
 * GpsMid - Copyright (c) 2009 Markus Baeurle mbaeurle at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.midlet.screens;

import java.util.Vector;

import javax.microedition.lcdui.TextField;

import de.enough.polish.util.Locale;

import de.ueller.gps.tools.IconActionPerformer;
import de.ueller.gps.tools.IconMenuPage;
import de.ueller.gps.tools.IconMenuWithPagesGUI;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.PositionMark;

/**
 * 
 */
public class GuiWaypointPredefined extends IconMenuWithPagesGUI {

	private final static Logger mLogger = Logger.getInstance(GuiWaypointPredefined.class, Logger.DEBUG);
	
	/** Vector holding the definitions of the waypoint templates */
	private final Vector mPredefines;
	
	/** Contains the position and elevation that will be saved */
	private PositionMark mWaypt = null;
	
	/** LCDUI form to input variable parts of the waypoint text (%f, %s) */
	private final GuiWaypointPredefinedForm mForm;

	/** Nothing more than a pair of strings with custom names. */
	class WaypointTemplate {
		public String mLabel;
		public String mWayptText;

		public WaypointTemplate(String label, String wayptText) {
			mLabel = label;
			mWayptText = wayptText;
		}
	}
	
	/** Inner class to perform the action when an item is chosen. */
	class GuiWaypointPredefinedActionPerformer implements IconActionPerformer {
		
		public GuiWaypointPredefinedActionPerformer() {
		}

		/* (non-Javadoc)
		 * @see de.ueller.gps.tools.IconActionPerformer#performIconAction(int)
		 */
		public void performIconAction(int actionId) {
			WaypointTemplate templ = (WaypointTemplate)mPredefines.elementAt(actionId);
			mLogger.info("Item " + templ.mLabel + " selected");
			if (templ.mWayptText.indexOf("%f") != -1) {
				mLogger.info("  Need to add a number");
				mForm.setData(templ.mLabel, templ.mWayptText, TextField.DECIMAL, mWaypt);
				mForm.show();
			} else if (templ.mWayptText.indexOf("%s") != -1) {
				mLogger.info("  Need to add a string");
				mForm.setData(templ.mLabel, templ.mWayptText, TextField.ANY, mWaypt);
				mForm.show();
			} else if (templ.mWayptText.indexOf("magic: normal input") != -1) {
				// Normal waypoint input, let Trace show it to preserve
				// settings in the dialog.
				((Trace)parent).showGuiWaypointSave(mWaypt);
			} else {
				// No variable parts, can be saved directly.
				mWaypt.displayName = templ.mWayptText;
				saveWaypoint();
			}
		}
		
		private void saveWaypoint() {
			((Trace)parent).gpx.addWayPt(mWaypt);

			// Wait a bit before displaying the map again. Hopefully
			// this avoids the sporadic freezing after saving a waypoint.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
			}

			parent.show();
		}

		/* (non-Javadoc)
		 * @see de.ueller.gps.tools.IconActionPerformer#recreateAndShowIconMenu()
		 */
		public void recreateAndShowIconMenu() {
			// TODO Can't do this here. There should be an internal solution inside
			// IconMenuWithPagesGUI for the resize (which seems to be what makes this necessary).
		}
	}
	
	/**
	 * @param parent
	 */
	public GuiWaypointPredefined(GpsMidDisplayable parent) {
		super(parent);
		super.setIconActionPerformer(new GuiWaypointPredefinedActionPerformer());
		
		mPredefines = new Vector(12);
		/*  1 */ mPredefines.addElement(new WaypointTemplate("City limit", "city_limit"));
		/*  2 */ mPredefines.addElement(new WaypointTemplate("Speed %f", "maxspeed=%f"));
		/*  3 */ mPredefines.addElement(new WaypointTemplate("Speed end", "maxspeed end"));
		/*  4 */ mPredefines.addElement(new WaypointTemplate("House nr.", "housenr %f"));
		/*  5 */ mPredefines.addElement(new WaypointTemplate("Bus stop", "bus stop %s"));
		/*  6 */ mPredefines.addElement(new WaypointTemplate("Agr 1 asph", "tracktype=1 asph"));
		/*  7 */ mPredefines.addElement(new WaypointTemplate("Agr %f gravel", "tracktype=%f gravel"));
		/*  8 */ mPredefines.addElement(new WaypointTemplate("Agr %f grass", "tracktype=%f grass"));
		/*  9 */ mPredefines.addElement(new WaypointTemplate("Waypoint", "magic: normal input"));
		/*  * */ mPredefines.addElement(new WaypointTemplate("Phone", "phone"));
		/*  0 */ mPredefines.addElement(new WaypointTemplate("Footway %s", "footway %s"));
		/*  # */ mPredefines.addElement(new WaypointTemplate("Path %s", "path %s"));
		
		IconMenuPage mp = createAndAddMenuPage(Locale.get("guiwaypointpre.PredefWpts")/*Predef. waypoints*/, 3, 4);
		for (int i = 0; i < mPredefines.size(); i++) {
			mp.createAndAddIcon(((WaypointTemplate)mPredefines.elementAt(i)).mLabel, 
					"i_addpoi", i);
		}
		
		mForm = new GuiWaypointPredefinedForm(parent, this);
	}

	public void setData(PositionMark posMark)
	{
		this.mWaypt = posMark;
	}
}
