/*
 * GpsMid - Copyright (c) 2009 Markus Baeurle mbaeurle at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GpsMidDisplayable;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.data.PositionMark;


public class GuiWaypointPredefinedForm extends Form implements CommandListener {
	private TextField mFldInput;
	private static final Command mSaveCmd = new Command("Save", Command.OK, 1);
	private static final Command mBackCmd = new Command("Back", Command.BACK, 2);
	
	private Trace mTrace;
	private GuiWaypointPredefined mParent;
	private String mWayptText;
	private int mType;
	private PositionMark mWaypt;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointPredefinedForm.class, Logger.TRACE);

	public GuiWaypointPredefinedForm(GpsMidDisplayable tr, GuiWaypointPredefined parent) {
		super("");
		mTrace = (Trace)tr;
		mParent = parent;
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void jbInit() throws Exception {
		mFldInput = new TextField("Input:", "", 
				Configuration.MAX_WAYPOINTNAME_LENGTH, TextField.ANY);
		
		// Set up this Displayable to listen to command events
		setCommandListener(this);
		// add the commands
		addCommand(mBackCmd);
		addCommand(mSaveCmd);
		this.append(mFldInput);
	}

	public void setData(String label, String wayptText, int type, PositionMark waypt) {
		super.setTitle(label);
		mWayptText = wayptText;
		mType = type;
		mWaypt = waypt;
		
		// Two chars of wayptText are the place holder (%s or %f)
		mFldInput.setMaxSize(Configuration.MAX_WAYPOINTNAME_LENGTH - wayptText.length() + 2);
		mFldInput.setString("");
		
		if (mType == TextField.DECIMAL) {
			mFldInput.setConstraints(TextField.DECIMAL);
		} else {
			mFldInput.setConstraints(TextField.ANY);
		}
	}

	public void commandAction(Command cmd, Displayable displayable) {
		if (cmd == mSaveCmd) {
			int placePos = mWayptText.indexOf("%");
			mWaypt.displayName = mWayptText.substring(0, placePos) + 
					mFldInput.getString() + 
					mWayptText.substring(placePos + 2, mWayptText.length());

			logger.info("Saving waypoint with name: " + mWaypt.displayName + 
					" ele: " + mWaypt.ele);
			Thread adder = new Thread(new Runnable() {
				public void run()
				{
					mTrace.gpx.addWayPt(mWaypt);

					// Wait a bit before displaying the map again. Hopefully
					// this avoids the sporadic freezing after saving a waypoint.
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
					}

					mTrace.show();				
				}
			} );
			adder.setPriority(Thread.MAX_PRIORITY);
			adder.start();
			return;
		}
		else if (cmd == mBackCmd) {
			mParent.show();
	    	return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}
}
