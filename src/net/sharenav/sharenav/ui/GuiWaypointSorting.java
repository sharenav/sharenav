/*
 * ShareNav - Copyright (c) 2010 mbaeurle at users dot sourceforge dot net
 * See COPYING
 */

package net.sharenav.sharenav.ui;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import net.sharenav.sharenav.data.Configuration;


/**
 * This class implements the form where the user can select which type
 * of way point sorting he wants.
 */
public class GuiWaypointSorting extends Form implements CommandListener {
	// Groups
	private ChoiceGroup mSortOptionsGroup;
	private final String [] mSortOptionsStr = new String[5];
	private final boolean[] mSelSortOptions = new boolean[5];

	private static final Command CMD_SAVE = new Command("Ok", Command.ITEM, 2);
	private static final Command CMD_CANCEL = new Command("Cancel", Command.BACK, 3);
	
	// other
	private final GuiWaypoint mParent;
	
	public GuiWaypointSorting(GuiWaypoint parent) {
		super("Waypoint sorting");
		mParent = parent;
		try {
			// Set choice texts and put configuration setting into selection states
			mSortOptionsStr[0] = "Newest first";
			mSelSortOptions[0] = (Configuration.getWaypointSortMode() == Configuration.WAYPT_SORT_MODE_NEW_FIRST);
			mSortOptionsStr[1] = "Oldest first";
			mSelSortOptions[1] = (Configuration.getWaypointSortMode() == Configuration.WAYPT_SORT_MODE_OLD_FIRST);
			mSortOptionsStr[2] = "Alphabetically";
			mSelSortOptions[2] = (Configuration.getWaypointSortMode() == Configuration.WAYPT_SORT_MODE_ALPHABET);
			mSortOptionsStr[3] = "Distance from map center";
			mSelSortOptions[3] = (Configuration.getWaypointSortMode() == Configuration.WAYPT_SORT_MODE_DISTANCE);
			mSortOptionsStr[4] = "No sorting";
			mSelSortOptions[4] = (Configuration.getWaypointSortMode() == Configuration.WAYPT_SORT_MODE_NONE);
			mSortOptionsGroup = new ChoiceGroup("Sorting", Choice.EXCLUSIVE, mSortOptionsStr, null);
			mSortOptionsGroup.setSelectedFlags(mSelSortOptions);
			append(mSortOptionsGroup);
			
			addCommand(CMD_SAVE);
			addCommand(CMD_CANCEL);

			// Set up this Displayable to listen to command events
			setCommandListener(this);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_CANCEL) {
			mParent.show();
			return;
		}
		else if (c == CMD_SAVE) {
			// Set configuration from boolean array with selection states
			mSortOptionsGroup.getSelectedFlags(mSelSortOptions);
			if (mSelSortOptions[0]) {
				Configuration.setWaypointSortMode(Configuration.WAYPT_SORT_MODE_NEW_FIRST);
			} else if(mSelSortOptions[1]) {
				Configuration.setWaypointSortMode(Configuration.WAYPT_SORT_MODE_OLD_FIRST);
			} else if(mSelSortOptions[2]) {
				Configuration.setWaypointSortMode(Configuration.WAYPT_SORT_MODE_ALPHABET);
			} else if(mSelSortOptions[3]) {
				Configuration.setWaypointSortMode(Configuration.WAYPT_SORT_MODE_DISTANCE);
			} else if(mSelSortOptions[4]) {
				Configuration.setWaypointSortMode(Configuration.WAYPT_SORT_MODE_NONE);
			}

			mParent.show(true);
			return;
		}
	}
	
	public void show() {
		ShareNav.getInstance().show(this);
	}

}
