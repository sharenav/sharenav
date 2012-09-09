/*
 * ShareNav - Copyright (c) 2009 Markus Baeurle mbaeurle at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.sharenav.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import de.enough.polish.util.Locale;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.util.Logger;
//#if polish.android
import de.enough.polish.android.lcdui.ViewItem;
//#endif

public class GuiWaypointPredefinedForm extends Form implements CommandListener, SaveButtonListener {
	//#if polish.android
	private ViewItem OKField;
	//#endif
	private TextField mFldInput;
	private static final Command mSaveCmd = new Command(Locale.get("generic.Save"), Command.OK, 1);
	private static final Command mBackCmd = new Command(Locale.get("generic.Back"), Command.BACK, 2);
	
	private final Trace mTrace;
	private final ShareNavDisplayable mParent;
	private String mWayptText;
	private int mType;
	private PositionMark mWaypt;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointPredefinedForm.class, Logger.TRACE);

	public GuiWaypointPredefinedForm(ShareNavDisplayable tr, ShareNavDisplayable parent) {
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
		mFldInput = new TextField(Locale.get("guiwaypointpre.Input")/*Input:*/, "", 
				Configuration.MAX_WAYPOINTNAME_LENGTH, TextField.ANY);

		// Set up this Displayable to listen to command events
		setCommandListener(this);
		// add the commands
		addCommand(mBackCmd);
		addCommand(mSaveCmd);
		//#if polish.android
		OKField = new SaveButton(Locale.get("traceiconmenu.SaveWpt"),
					 this, (Displayable) mTrace,
					 mSaveCmd);
		//#endif

		this.append(mFldInput);
		//#if polish.android
		//#style formItem
		this.append(OKField);
		Display.getDisplay(ShareNav.getInstance()).setCurrentItem(mFldInput);
		//#endif
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
			//#if polish.android
			mTrace.gpx.addWayPt(mWaypt);

			// Wait a bit before displaying the map again. Hopefully
			// this avoids the sporadic freezing after saving a waypoint.
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
			}
			mTrace.show();				
			return;
			//#else
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
			//#endif
		}
		else if (cmd == mBackCmd) {
			mParent.show();
	    	return;
		}
	}
	
	public void backPressed() {
		mParent.show();
	}

	public void show() {
		ShareNav.getInstance().show(this);
	}
}
