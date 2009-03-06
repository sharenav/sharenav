package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 *          Copyright (c) 2008 mbaeurle at users dot sourceforge dot net
 * See Copying
 */


import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import java.util.Vector;
import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.data.PersistEntity;


public class GuiGpx extends List implements CommandListener,
		GpsMidDisplayable, UploadListener, CompletionListener {

	private final static Logger logger = Logger.getInstance(GuiGpx.class, Logger.DEBUG);
	
	private final Command SEND_CMD = new Command("Export", Command.OK, 1);
	private final Command LOAD_CMD = new Command("Import", Command.ITEM, 3);
	private final Command DISP_CMD = new Command("Display first selected", Command.ITEM, 3);
	private final Command RENAME_CMD = new Command("Rename first selected", Command.ITEM, 3);	
	private final Command DEL_CMD = new Command("Delete", Command.ITEM, 3);	
	private final Command SALL_CMD = new Command("Select All", Command.ITEM, 4);
	private final Command DSALL_CMD = new Command("Deselect All", Command.ITEM, 4);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);

	private boolean uploading;
	
	/* Index of first selected track in list. NOTE: This is only updated by 
	 * the operations that need it!
	 */
	private int idx;
	private String sCount;
	
	private final Trace parent;
	
	private Alert progressDisplay;
	private Gauge progressbar;
	private StringBuffer sbProgress;
	private boolean progressCloseable;
	
	/* Tracks displayed to the user. */
	private PersistEntity [] trks;
	/* Tracks that are processed (exported, deleted). */
	private Vector processTracks;
	
	public GuiGpx(Trace parent) throws Exception {
		super("GPX tracklogs", List.MULTIPLE);
		this.parent = parent;
		processTracks = new Vector();

		setCommandListener(this);
		initTracks();
		
		addCommand(SEND_CMD);
		addCommand(LOAD_CMD);
		addCommand(DISP_CMD);
		addCommand(RENAME_CMD);		
		addCommand(DEL_CMD);
		addCommand(SALL_CMD);
		addCommand(DSALL_CMD);		
		addCommand(BACK_CMD);		
	}
	
	/**
	 * Read tracks from the GPX recordStore and display the names in the list on screen.
	 */
	private void initTracks() {
		this.deleteAll();		
		trks = parent.gpx.listTrks();
		for (int i = 0; i < trks.length; i++) {
			this.append(trks[i].displayName, null);
		}
	}

	public void commandAction(Command c, Displayable d) {
		//#debug debug
		logger.debug("got Command " + c.getLabel());
		if (c == SEND_CMD) {
			uploading = true;
			updateProcessVector();
			if (processTracks.size() > 0)
			{
				int numAllWptsInTrack = 0;
				try{
					for (int i = 0; i < processTracks.size(); i++){
						numAllWptsInTrack += ((PersistEntity)processTracks.elementAt(i)).getTrackSize();
					}
					showProgressDisplay("Exporting tracks", numAllWptsInTrack);
				} catch (ClassCastException e){
					// TO-DO
				}
				parent.gpx.exportTracks(Configuration.getGpxUrl(), this, processTracks );
			}
			return;
		}
		if (c == LOAD_CMD) {
			uploading = false;
			GuiGpxLoad ggl = new GuiGpxLoad(this, this, false);
			ggl.show();
			return;
		}
		if (c == DISP_CMD) {
			idx = getFirstSelectedIndex();
			if (idx >= 0) {
				parent.gpx.displayTrk(trks[idx]);
				parent.show();
			}
			return;
		}
		if (c == RENAME_CMD) {
			idx = getFirstSelectedIndex();
			if (idx >= 0) {
				sCount = "";
				String sNameOnly = trks[idx].displayName;
				int iCountPos = sNameOnly.lastIndexOf('(');
				if (iCountPos > 0 && sNameOnly.lastIndexOf(' ') == (iCountPos - 1) ) {
					sCount = sNameOnly.substring(iCountPos - 1);
					sNameOnly = sNameOnly.substring(0, iCountPos - 1);
				}
				GuiNameEnter gne = new GuiNameEnter(this, "Rename Track", sNameOnly, Configuration.MAX_TRACKNAME_LENGTH);
				gne.show();
			}
			return;
		}
		if (c == DEL_CMD) {
			updateProcessVector();
			if (processTracks.size() > 0)
			{
				showProgressDisplay("Deleting tracks");
				addProgressText("Deleting " + processTracks.size() + " tracks.\n");
				parent.gpx.deleteTracks(processTracks);
				addProgressText("Finished!");
				finishProgressDisplay();
				initTracks();
			}
			return;
		}
		if ((c == SALL_CMD) || (c == DSALL_CMD)) {
			boolean select = (c == SALL_CMD);
			boolean[] sel = new boolean[trks.length];
			for (int i = 0; i < trks.length; i++)
				sel[i] = select;
			this.setSelectedFlags(sel);
			return;
		}
		if (c == BACK_CMD) {			
			parent.show();
			return;
		}
		if (c == Alert.DISMISS_COMMAND) {
			if (progressCloseable) {
				show();
			}
		}
	}
	
	/** updates the Vector of gpxtracks that should be processed by another method**/
	private void updateProcessVector()
	{	
		// find out which tracks should be exported **/
		boolean[] boolSelected = new boolean[this.size()];
		this.getSelectedFlags(boolSelected);
		// create new list of tracks which need to be processed /exported 
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
		showProgressDisplay(title);
	}
	
	public void setProgress(String message) {
		// Not supported/used at the moment.
		
	}
	
	public void updateProgress(String message) {
		addProgressText(message);
	}
	
	/**
	 * implementing the UploadListner Interface
	 * updates the progressbar by increasing the progress by the given value
	 */
	public void updateProgressValue(int inc){
		System.out.println("Prgressbar: " + progressbar.getValue());
		progressbar.setValue(progressbar.getValue() + inc);
	}

	public void completedUpload(boolean success, String message) {
		String alertMsg;		
		if (uploading) {
			if (success) {
				
				alertMsg = "\nFinished!";
			} else {
				alertMsg = "\nGPX export failed: " + message;
			}
		} else {
			if (success) {
				alertMsg = "\n***********\nCompleted GPX import: " + message;
				initTracks();
			} else {
				alertMsg = "GPX import failed: " + message;
			}
		}
		addProgressText(alertMsg);
		finishProgressDisplay();
	}

	public void uploadAborted() {
		initTracks();
	}

	public void show() {
		GpsMid.getInstance().show(this);
	}

	/* Show an alert window with the given title that cannot be dismissed by
	 * the user, i.e. a modal progress dialog.
	 * @param title The title of the alert
	 */
	protected void showProgressDisplay(String title)
	{
		if (progressDisplay == null) {
			progressDisplay = new Alert(title);
			progressDisplay.setCommandListener(this);
			progressDisplay.setTimeout(Alert.FOREVER);
			//creates a progressbar - not used in this case but it should be created when the alert is first created so it's present later
			progressbar = new Gauge(null, false, 0, 0);
		} else {
			progressDisplay.setTitle(title);
			progressDisplay.setIndicator(null);
		}
		// Empty string buffer for alert text.
		sbProgress = new StringBuffer();
		// At least on Sony Ericsson phones, the alert won't be shown
		// until it contains some text, so let's put in a dummy. 
		progressDisplay.setString(" ");		
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
	 * the user, i.e. a modal progress dialog. This Alert Window also has a progressbar indicating the progress
	 * 
	 * @param title The title of the alert
	 * @param progEndValue the maximum value for the progressbar
	 */
	protected void showProgressDisplay(String title, int progEndValue)
	{
		if (progressDisplay == null) {
			progressDisplay = new Alert(title);
			progressDisplay.setCommandListener(this);
			progressDisplay.setTimeout(Alert.FOREVER);
			//create a progressbar that gives an indication about how many waypoints have already been exported
			progressbar = new Gauge(null, false, progEndValue, 0);
			progressDisplay.setIndicator(progressbar);
		} else {
			progressDisplay.setTitle(title);
			progressbar.setMaxValue(progEndValue);
			progressbar.setValue(0);
		}
		// Empty string buffer for alert text.
		sbProgress = new StringBuffer();
		// At least on Sony Ericsson phones, the alert won't be shown
		// until it contains some text, so let's put in a dummy. 
		progressDisplay.setString(" ");		
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
	protected void addProgressText(String text)
	{
		if (sbProgress != null && progressDisplay != null) {
			sbProgress.append(text);
			progressDisplay.setString(sbProgress.toString());
		}
	}
	
	/* After this method was called, the user can dismiss the 
	 * alert window (which has no timeout).
	 */
	protected void finishProgressDisplay()
	{
		progressCloseable = true;
	}
	
	public void actionCompleted(String strResult) {
		if (strResult != null) {		
			// rename track
			trks[idx].displayName = strResult;
			parent.gpx.updateTrackName(trks[idx]);
			// change item in list
			set(idx, strResult + sCount, null);
		}
		show();
	}
	
}
