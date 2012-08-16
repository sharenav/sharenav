/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 *          Copyright (c) 2008 mbaeurle at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import java.util.Vector;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.PersistEntity;
import de.ueller.gpsmid.data.TrackPlayer;
import de.ueller.midlet.ui.CompletionListener;
import de.ueller.midlet.ui.InputListener;
import de.ueller.midlet.ui.ProgressDisplay;
import de.ueller.midlet.ui.UploadListener;
import de.ueller.util.Logger;

import de.enough.polish.util.Locale;

/**
 * GuiGpx represents the track management screen. It allows export, import, 
 * deletion, display and renaming of GPX tracklogs. It handles the GUI part of
 * track management. The real GPX data is handled in the GPX class.
 */
public class GuiGpx extends List implements CommandListener,
				 GpsMidDisplayable, UploadListener, InputListener, CompletionListener {
	
	private final static Logger logger = Logger.getInstance(GuiGpx.class, Logger.DEBUG);
	
	private final Command SEND_CMD = new Command(Locale.get("guigpx.Export")/*Export*/, Command.OK, 1);
	private final Command LOAD_CMD = new Command(Locale.get("guigpx.Import")/*Import*/, Command.ITEM, 3);
	private final Command DISP_CMD = new Command(Locale.get("guigpx.Display")/*Display*/, Locale.get("guigpx.DispSelectedTracks")/*Display selected tracks*/, Command.ITEM, 3);
	private final Command UNDISP_CMD = new Command(Locale.get("guigpx.Undisplay")/*Undisplay*/, Locale.get("guigpx.UndispLoadedTracks")/*Undisplay loaded tracks*/, Command.ITEM, 3);
	private final Command RENAME_CMD = new Command(Locale.get("guigpx.Rename")/*Rename*/, Locale.get("guigpx.RenameFirstSelected")/*Rename first selected*/, Command.ITEM, 3);	
	private final Command REPLAY_START_CMD = new Command(Locale.get("guigpx.Replay")/*Replay*/, Command.ITEM, 3);	
	private final Command REPLAY_STOP_CMD = new Command(Locale.get("guigpx.StopReplay")/*Stop Replay*/, Command.ITEM, 3);	
	private final Command DEL_CMD = new Command(Locale.get("generic.Delete")/*Delete*/, Command.ITEM, 3);	
	private final Command SALL_CMD = new Command(Locale.get("guigpx.SelectAll")/*Select All*/, Command.ITEM, 4);
	private final Command DSALL_CMD = new Command(Locale.get("guigpx.DeselectAll")/*Deselect All*/, Command.ITEM, 4);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 5);

	/** Value for mJob: No job */
	private static final int JOB_IDLE = 0;
	/** Value for mJob: Exporting tracks to GPX */
	private static final int JOB_EXPORT_TRKS = 1;
	/** Value for mJobState: Importing GPX data */
	private static final int JOB_IMPORT_GPX = 2;
	/** Value for mJobState: Deleting tracks */
	private static final int JOB_DELETE_TRKS = 3;
	
	/** Information which job is running. */
	private int mJob;
	
	/** Index of first selected track in list. NOTE: This is only updated by 
	 * the operations that need it!
	 */
	private int idx;
	
	/** Reference to the Trace class, needed to access the Gpx class and 
	 * to switch back to the map. */ 
	private final Trace parent;
	
	/** Alert to display the progress of the operations. */
	private final ProgressDisplay progress;

	/** Tracks displayed to the user. */
	private PersistEntity [] trks;

	/** Tracks that are processed (exported, deleted). */
	private final Vector processTracks;
	
	public GuiGpx(Trace parent) throws Exception {
		super(Locale.get("guigpx.GPXlogs")/*GPX tracklogs*/, List.MULTIPLE);
		this.parent = parent;
		progress = new ProgressDisplay(this);
		processTracks = new Vector();

		setCommandListener(this);
		initTracks();
		
		if (TrackPlayer.isPlaying) {
			addCommand(REPLAY_STOP_CMD);
		}
		addCommand(SEND_CMD);
		addCommand(LOAD_CMD);
		addCommand(DISP_CMD);
		addCommand(UNDISP_CMD);
		addCommand(RENAME_CMD);		
		// to avoid trouble with user deleting currently recording track, disable deletions if recording is on
		if (!parent.gpx.isRecordingTrk()) {
			addCommand(DEL_CMD);
		}
		addCommand(SALL_CMD);
		addCommand(DSALL_CMD);		
		if (!TrackPlayer.isPlaying && !parent.gpx.isRecordingTrk()) {
			addCommand(REPLAY_START_CMD);				
		}
		addCommand(BACK_CMD);		
	}
	
	/**
	 * Read tracks from the GPX recordStore and display the names in the list on screen.
	 */
	private void initTracks() {
		int count = this.size();
		if (count != 0) {
			/* Workaround: On some SE phones the selection state of list elements 
			 * must be explicitely cleared before re-adding list elements - 
			 * otherwise they stay selected. 
			 */
			boolean[] boolSelected = new boolean[count];
			for (int i = 0; i < count; i++) {
				boolSelected[i] = false;
			}
			this.setSelectedFlags(boolSelected);
		}

		this.deleteAll();
		trks = parent.gpx.listTrks();
		for (int i = 0; i < trks.length; i++) {
			try {
				//#style listItem
				this.append(trks[i].displayName, null);
			} catch (NullPointerException e) {
				// Just ignore it, error is already generated by Gpx.listTrks().
			}
		}
		this.setTitle(Locale.get("guigpx.GPXTracklogs")/*GPX tracklogs (*/ + trks.length + ")");
	}
	
	public void commandAction(Command c, Displayable d) {
		//#debug debug
		logger.debug("got Command " + c.getLabel());
		if (c == SEND_CMD) {
			updateProcessVector();
			if (processTracks.size() > 0) {
				mJob = JOB_EXPORT_TRKS;
				int numAllPtsInTrack = 0;
				try {
					for (int i = 0; i < processTracks.size(); i++){
						numAllPtsInTrack += ((PersistEntity)processTracks.elementAt(i)).getTrackSize();
					}
					if (progress != null) {
						progress.showProgressDisplay(Locale.get("guigpx.ExportingTracks")
									     /*Exporting tracks*/, numAllPtsInTrack);
					}
				} catch (ClassCastException cce) {
					logger.exception(Locale.get("guigpx.ClassCastExceptionInCommandAction")
							/*ClassCastException in commandAction*/, cce);
				}
				parent.gpx.exportTracks(Configuration.getGpxUrl(), this, processTracks );
			}
			return;
		}
		if (c == LOAD_CMD) {
			mJob = JOB_IMPORT_GPX;
			GuiGpxLoad ggl = new GuiGpxLoad(this, this, false);
			ggl.show();
			return;
		}
		if (c == DISP_CMD) {
			updateProcessVector();
			if (processTracks.size() > 0) {
				parent.gpx.displayTrk(processTracks);
				parent.show();
			}
			return;
		}
		if (c == REPLAY_START_CMD) {
			if (parent.isGpsConnected()) {
				parent.show();
				parent.alert(Locale.get("guigpx.TrackPlayer")/*TrackPlayer*/, Locale.get("guigpx.CantReplayGPS")/* Can't replay while connected to GPS */, 2500);
				return;
			} else {
				updateProcessVector();
				if (processTracks.size() > 0) {
					parent.gpx.replayTrk(processTracks);
					parent.show();
				}
			}
			return;
		}
		if (c == REPLAY_STOP_CMD) {
			TrackPlayer.getInstance().stop();
			parent.show();			
			return;
		}
		if (c == UNDISP_CMD) {
			parent.gpx.undispLoadedTracks();
			parent.show();
			return;
		}
		if (c == RENAME_CMD) {
			idx = getFirstSelectedIndex();
			if (idx >= 0) {
				GuiNameEnter gne = new GuiNameEnter(this, Locale.get("guigpx.RenameTrack")/*Rename Track*/, 
						trks[idx].displayName, Configuration.MAX_TRACKNAME_LENGTH);
				gne.show();
			}
			return;
		}
		if (c == DEL_CMD) {
			updateProcessVector();
			if (processTracks.size() > 0)
			{
				mJob = JOB_DELETE_TRKS;
				if (progress != null) {
					progress.showProgressDisplay(Locale.get("guigpx.DeletingTracks")/*Deleting tracks*/);
					progress.addProgressText(Locale.get("guigpx.Deleting")/*Deleting */ + processTracks.size() + " " + Locale.get("guigpx.Tracks")/*track(s)*/ + ".\n");
				}
				parent.gpx.deleteTracks(processTracks, this);
			}
			return;
		}
		if ((c == SALL_CMD) || (c == DSALL_CMD)) {
			boolean select = (c == SALL_CMD);
			boolean[] sel = new boolean[trks.length];
			for (int i = 0; i < trks.length; i++) {
				sel[i] = select;
			}
			this.setSelectedFlags(sel);
			return;
		}
		if (c == BACK_CMD) {			
			parent.show();
			return;
		}
	}
	
	/** Updates the Vector of GPX tracks that should be processed by another method. */
	private void updateProcessVector() {	
		// find out which tracks should be exported **/
		boolean[] boolSelected = new boolean[this.size()];
		this.getSelectedFlags(boolSelected);
		// create new list of tracks which need to be processed / exported 
		processTracks.removeAllElements();
		for (int i = 0; i < boolSelected.length; i++) {
			if (boolSelected[i] == true) {
				processTracks.addElement(trks[i]);
			}
		}
	}
	
	public int getFirstSelectedIndex() {
		boolean[] boolSelected = new boolean[this.size()];
		this.getSelectedFlags(boolSelected);
		for (int i = 0; i < boolSelected.length; i++) {
			if (boolSelected[i] == true) {
				return i;
			}
		}
		// None selected -> return -1
		return -1;
	}
	
	public void startProgress(String title) {
		if (progress != null) {
			progress.showProgressDisplay(title);
		}
	}
	
	public void setProgress(String message) {
		// Not supported/used at the moment.
	}
	
	public void updateProgress(String message) {
		if (progress != null) {
			progress.addProgressText(message);
		}
	}
	
	/**
	 * Implementing the UploadListener interface.
	 * Updates the progress bar by increasing the progress by the given value.
	 */
	public void updateProgressValue(int inc) {
		//#if not polish.android
		if (progress != null) {
			progress.updateProgressValue(inc);
		}
		//#endif
	}

	public void completedUpload(boolean success, String message) {
		String alertMsg;		
		if (mJob == JOB_EXPORT_TRKS) {
			if (success) {
				alertMsg = Locale.get("guigpx.Finished")/*Finished!*/;
			} else {
				alertMsg = Locale.get("guigpx.GPXExportFailed")/*GPX export failed: */ + message;
			}
		} else if (mJob == JOB_IMPORT_GPX) {
			if (success) {
				alertMsg = "***********\n" + Locale.get("guigpx.CompletedGPXImport")/*Completed GPX import: */ + message;
			} else {
				alertMsg = Locale.get("guigpx.GPXImportFailed")/*GPX import failed: */ + message;
			}
			initTracks();
		} else {
			// Can only be JOB_DELETE_TRKS but if we check against it,
			// the compiler warns that alertMsg may not be initialized.
			if (success) {
				alertMsg = Locale.get("guigpx.Finished")/*Finished!*/;
			} else {
				alertMsg = Locale.get("guigpx.DeletingTrackFailed")/*Deleting track(s) failed: */ + message;
			}
			initTracks();
		}
		if (progress != null) {
			progress.addProgressText(alertMsg);
			progress.finishProgressDisplay();
		}
		mJob = JOB_IDLE;
	}

	public void uploadAborted() {
		initTracks();
		mJob = JOB_IDLE;
	}

	public void show() {
		// In case that we return to this screen, make sure our state is correct.
		mJob = JOB_IDLE;
		GpsMid.getInstance().show(this);
	}
	
	/** Called by the screen where the new track name is entered (GuiNameEnter).
	 * @param strResult The string entered by the user
	 */
	public void inputCompleted(String strResult) {
		if (strResult != null) {		
			// rename track
			trks[idx].displayName = strResult;
			parent.gpx.updateTrackName(trks[idx]);
			// change item in list
			set(idx, strResult, null);
		}
		show();
	}

	/** Called when the user closes the progress popup.
	 */
	public void actionCompleted() {
		show();
	}
}
