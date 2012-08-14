/*
 * GpsMid - Copyright (c) 2009 mbaeurle at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.midlet.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Gauge;

import de.ueller.gpsmid.ui.GpsMid;
import de.ueller.gpsmid.ui.GuiGpx;
import de.ueller.util.Logger;

import de.enough.polish.util.Locale;

/** Alert to display the progress of an operation. */
public class ProgressDisplay implements CommandListener {
	private final static Logger logger = Logger.getInstance(GuiGpx.class, Logger.INFO);
	
	private CompletionListener mListener;
	private Alert progressDisplay;
	private Gauge progressbar;
	private StringBuffer sbProgress;
	private boolean progressCloseable;

	public ProgressDisplay(CompletionListener listener) {
		mListener = listener;
	}

	/* Show an alert window with the given title that cannot be dismissed by
	 * the user, i.e. a modal progress dialog.
	 * @param title The title of the alert
	 */
	public void showProgressDisplay(String title) {
		// FIXME add proper Android code
		if (progressDisplay == null) {
			progressDisplay = new Alert(title);
			progressDisplay.setCommandListener(this);
			progressDisplay.setTimeout(Alert.FOREVER);
			// Creates a progress bar - not used in this case but it should be 
			// created when the alert is first created so it's present later.
			//#if not polish.android
			progressbar = new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
			//#endif
		} else {
			progressDisplay.setTitle(title);
			progressDisplay.setIndicator(null);
		}
		// Empty string buffer for alert text.
		sbProgress = new StringBuffer();
		// At least on Sony Ericsson phones, the alert won't be shown
		// until it contains some text, so let's put in something. 
		//#if polish.android
		progressDisplay.setString(Locale.get("generic.OK"));
		//#else
		progressDisplay.setString(" ");		
		//#endif
		try {
			GpsMid.getInstance().show(progressDisplay);
			progressCloseable = false;
		} catch (IllegalArgumentException iae) {
			/**
			 * Nokia S40 phones seem to throw an exception
			 * if one tries to set an Alert displayable when
			 * the current displayable is an alert too.
			 * 
			 * Not much we can do about this, other than just
			 * ignore the exception and not display the new
			 * alert. 
			 */
			logger.info("Could not display progress alert, " + iae.getMessage());
		}
	}
	
	/**
	 * Show an alert window with the given title that cannot be dismissed by
	 * the user, i.e. a modal progress dialog. This Alert Window also has a 
	 * progress bar indicating the progress.
	 * 
	 * @param title The title of the alert
	 * @param progEndValue The maximum value for the progress bar
	 */
	public void showProgressDisplay(String title, int progEndValue) {
		int progrMode = Gauge.INCREMENTAL_UPDATING;
		// Catch illegal argument
		if (progEndValue < 1) {
			progEndValue = Gauge.INDEFINITE;
			//Set mode for progressbar
			progrMode = Gauge.CONTINUOUS_RUNNING;
		}
		
		if (progressDisplay == null) {
			progressDisplay = new Alert(title);
			progressDisplay.setCommandListener(this);
			progressDisplay.setTimeout(Alert.FOREVER);
			// Create a progress bar that gives an indication about how much has 
			// already been exported.
			//#if not polish.android
			progressbar = new Gauge(null, false, progEndValue, progrMode);
			//#endif
		} else {
			progressDisplay.setTitle(title);
			//#if not polish.android
			progressbar.setMaxValue(progEndValue);
			progressbar.setValue(progrMode);
			//#endif
		}
		try {
			/* MicroEmulator throws an exception:
			 *  java.lang.IllegalArgumentException: This gauge cannot be added to an Alert
			 */ 
			//#if not polish.android
			progressDisplay.setIndicator(progressbar);			
			//#endif
		} catch (Exception e) {
			logger.info("Could not set progressbar, " + e.getMessage());
		}
		// Empty string buffer for alert text.
		sbProgress = new StringBuffer();
		// At least on Sony Ericsson phones, the alert won't be shown
		// until it contains some text, so let's put in something. 
		//#if polish.android
		progressDisplay.setString(Locale.get("generic.OK"));
		//#else
		progressDisplay.setString(" ");		
		//#endif
		try {
			GpsMid.getInstance().show(progressDisplay);
			progressCloseable = false;
		} catch (IllegalArgumentException iae) {
			/**
			 * Nokia S40 phones seem to throw an exception
			 * if one tries to set an Alert displayable when
			 * the current displayable is an alert too.
			 * 
			 * Not much we can do about this, other than just
			 * ignore the exception and not display the new
			 * alert. 
			 */
			logger.info("Could not display progress alert, " + iae.getMessage());
		}
	}

	/* Add text to the progress alert window.
	 * @param text Text to be added
	 */
	public void addProgressText(String text)	{
		if (sbProgress != null && progressDisplay != null) {
			sbProgress.append(text);
			progressDisplay.setString(sbProgress.toString());
		}
	}
	
	/**
	 * Updates the progress bar by increasing the progress by the given value.
	 * @param inc Value by which to *increase* the progress
	 */
	public void updateProgressValue(int inc) {
		//System.out.println("Progressbar: " + progressbar.getValue());
		progressbar.setValue(progressbar.getValue() + inc);
	}

	/* After this method was called, the user can dismiss the 
	 * alert window (which has no timeout).
	 */
	public void finishProgressDisplay() {
		// Some phones only show a progressbar that's continuous running. 
		// So we remove the bar to show that the action is completed.
		if (progressDisplay != null) {
			progressDisplay.setIndicator(null);
		}
		progressCloseable = true;
	}

	public void commandAction(Command c, Displayable d) {
		if (c == Alert.DISMISS_COMMAND) {
			if (progressCloseable && mListener != null) {
				mListener.actionCompleted();
			}
		}
	}
}
