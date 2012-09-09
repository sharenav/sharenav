/*
 * ShareNav - Copyright (c) 2009 Markus Baeurle mbaeurle at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.ui;

import java.util.Vector;

import javax.microedition.lcdui.TextField;

import de.enough.polish.util.Locale;

import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.midlet.iconmenu.IconActionPerformer;
import net.sharenav.midlet.iconmenu.IconMenuPage;
import net.sharenav.midlet.iconmenu.IconMenuWithPagesGui;
import net.sharenav.util.Logger;

/**
 * 
 */
public class GuiWaypointPredefined extends IconMenuWithPagesGui {

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
		 * @see net.sharenav.gps.tools.IconActionPerformer#performIconAction(int)
		 */
		public void performIconAction(int actionId, String choiceName) {
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
			} else if (templ.mWayptText.indexOf("magic: back") != -1) {
				// Back "button"
				((Trace)parent).show();
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
		 * @see net.sharenav.gps.tools.IconActionPerformer#recreateAndShowIconMenu()
		 */
		public void recreateAndShowIconMenu() {
			// TODO Can't do this here. There should be an internal solution inside
			// IconMenuWithPagesGui for the resize (which seems to be what makes this necessary).
		}
	}
	
	/**
	 * @param parent
	 */
	public GuiWaypointPredefined(ShareNavDisplayable parent) {
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
		/*  0 */ mPredefines.addElement(new WaypointTemplate("Path %s", "path %s"));
		/*  # */ mPredefines.addElement(new WaypointTemplate("Back", "magic: back"));

		IconMenuPage mp = createAndAddMenuPage(Locale.get("guiwaypointpre.PredefWpts")/*Predef. waypoints*/, 3, 4);
		for (int i = 0; i < mPredefines.size(); i++) {
			if (((WaypointTemplate)mPredefines.elementAt(i)).mWayptText.indexOf("magic: normal input") != -1) {
				mp.createAndAddIcon(((WaypointTemplate)mPredefines.elementAt(i)).mLabel, 
						"i_savewpt", i);
			} else if (((WaypointTemplate)mPredefines.elementAt(i)).mWayptText.indexOf("magic: back") != -1) {
				mp.createAndAddIcon(((WaypointTemplate)mPredefines.elementAt(i)).mLabel, 
						"i_back", i);				
			} else {
				mp.createAndAddIcon(((WaypointTemplate)mPredefines.elementAt(i)).mLabel, 
						"i_addpoi", i);
			}
		}
		
		mForm = null;
		//mForm = new GuiWaypointPredefinedForm(parent, (ShareNavDIsplayable) this);
	}

	public void setData(PositionMark posMark)
	{
		this.mWaypt = posMark;
	}
}
