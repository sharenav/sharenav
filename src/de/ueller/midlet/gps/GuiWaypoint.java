/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.midlet.gps;

import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.IntPoint;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.screens.InputListener;
import de.ueller.midlet.screens.ProgressDisplay;


public class GuiWaypoint extends /*GuiCustom*/List implements CommandListener,
		GpsMidDisplayable, UploadListener, InputListener, CompletionListener {

	private final static Logger mLogger = Logger.getInstance(GuiWaypoint.class, Logger.DEBUG);
	
	private final Command EXPORT_ALL_CMD = new Command("Export All", Command.ITEM, 1);	
	private final Command IMPORT_CMD = new Command("Import", Command.ITEM, 2);
	private final Command RENAME_CMD = new Command("Rename", Command.ITEM, 2);	
	private final Command DEL_CMD = new Command("Delete", Command.ITEM, 2);	
	private final Command SALL_CMD = new Command("Select All", Command.ITEM, 2);
	private final Command DSALL_CMD = new Command("Deselect All", Command.ITEM, 2);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	private final Command GOTO_CMD = new Command("Display", Command.OK,6);

	/** Array containing all waypoints currently in the application. */
	private PositionMark[] mWaypoints;
	
	/** Reference to the Trace class, needed to access the Gpx class and 
	 * to switch back to the map. */ 
	private final Trace mParent;
	
	/** Alert to display the progress of the operations. */
	private ProgressDisplay mProgress;
	
	/** ID of the waypoint to be renamed. */
	private static int mWptId;
	
	/** Flag whether we are currently exporting waypoints */
	private boolean mExporting;

	/** Flag whether we are currently importing waypoints */
	private boolean mImporting;
	
	
	public GuiWaypoint(Trace parent) throws Exception {
		super("Waypoints", List.MULTIPLE);
		mParent = parent;
		mProgress = new ProgressDisplay(this);
		mExporting = false;
		mImporting = false;
		setCommandListener(this);
		initWaypoints();
		
		addCommand(EXPORT_ALL_CMD);
		addCommand(IMPORT_CMD);
		addCommand(RENAME_CMD);		
		addCommand(DEL_CMD);		
		addCommand(SALL_CMD);		
		addCommand(DSALL_CMD);
		addCommand(BACK_CMD);		
		addCommand(GOTO_CMD);
	}
	
	/**
	 * Read tracks from the GPX recordStore and display the names in the list on screen.
	 */
	private void initWaypoints() {
		int count = this.size();
		if (count != 0) {
			/*  Workaround: on some SE phones the selection state of list elements 
			 *  must be explicitly cleared before re-adding list elements - otherwise 
			 *  they stay selected .
			 */
			boolean[] boolSelected = new boolean[count];
			for (int i = 0; i < count; i++) {
				boolSelected[i] = false;
			}
			this.setSelectedFlags(boolSelected);
		}

		this.deleteAll();		

		mWaypoints = mParent.gpx.listWayPt();
		
		// save total waypoints
		int count_waypoints = mWaypoints.length;
		
		// Limit display of Waypoints to 255 because some Phones can only display 255 listitems
		if (count_waypoints > 255) {
			count_waypoints = 255;    
		}
		for (int i = 0; i < count_waypoints; i++) {
			if ((mWaypoints[i].displayName == null) || (mWaypoints[i].displayName.equals(""))) {
				this.append("(unnamed)", null);
			} else {
				this.append(mWaypoints[i].displayName, null);
			}
		}
		this.setTitle("Waypoints (" + count_waypoints + ")");
	}

	public void commandAction(Command c, Displayable d) {
		//#debug debug
		mLogger.debug("got Command " + c);
		if (c == RENAME_CMD) {
			mExporting = false;
			mImporting = false;
			boolean[] sel = new boolean[mWaypoints.length];
			this.getSelectedFlags(sel);			
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					mWptId = i;
					GuiNameEnter gne = new GuiNameEnter(this, "Rename Waypoint", 
							mWaypoints[i].displayName, Configuration.MAX_WAYPOINTNAME_LENGTH);
					gne.show();
					break;
				}
			}
			return;
		}
		if (c == DEL_CMD) {
			Vector idsToDelete = new Vector();
			boolean[] sel = new boolean[mWaypoints.length];
			this.getSelectedFlags(sel);			
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					// mParent.gpx.deleteWayPt(mWaypoints[i], this);
					idsToDelete.addElement(new Integer(mWaypoints[i].id));
				}
			}
			mLogger.info("Delete waypoints, idsToDelete=" + idsToDelete.size() + 
					" sel.length=" + sel.length + 
					" mWaypoints.length=" + mWaypoints.length);
			if (idsToDelete.size() > 0)
			{
				mExporting = false;
				mImporting = false;
				mProgress.showProgressDisplay("Deleting way points");
				mProgress.addProgressText("Deleting " + idsToDelete.size() + " way point(s).\n");
				mParent.gpx.deleteWayPts(idsToDelete, this);
			}
			return;
		}
		if ((c == SALL_CMD) || (c == DSALL_CMD)) {
			boolean select = (c == SALL_CMD);
			boolean[] sel = new boolean[mWaypoints.length];
			for (int i = 0; i < mWaypoints.length; i++) {
				sel[i] = select;
			}
			this.setSelectedFlags(sel);
			return;
		}		
		if (c == BACK_CMD) {			
			mParent.show();
			return;
		}
		if (c == EXPORT_ALL_CMD) {
			mExporting = true;
			mImporting = false;
			GuiNameEnter gne = new GuiNameEnter(this, "Send as (without .gpx)", 
					HelperRoutines.formatSimpleDateNow() + "-waypoints", 
					Configuration.MAX_WAYPOINTS_NAME_LENGTH);
			gne.show();
			return;
		}
		if (c == IMPORT_CMD) {
			mExporting = false;
			mImporting = true;
			GuiGpxLoad ggl = new GuiGpxLoad(this, this, true);
			ggl.show();
			return;			
		}
		if (c == GOTO_CMD) {
			float w = 0, e = 0, n = 0, s = 0; 
			int idx = -1;
			boolean[] sel = new boolean[mWaypoints.length];
			this.getSelectedFlags(sel);			
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					if (idx == -1) {
						idx = i;
						w =  mWaypoints[i].lon;
						e =  mWaypoints[i].lon;
						n = mWaypoints[i].lat;
						s = mWaypoints[i].lat;
					} else {
						idx = -2;
						if (mWaypoints[i].lon < w) {
							w = mWaypoints[i].lon;
						}
						if (mWaypoints[i].lon > e) {
							e = mWaypoints[i].lon;
						}
						if (mWaypoints[i].lat < s) {
							s = mWaypoints[i].lat;
						}
						if (mWaypoints[i].lat > n) {
							n = mWaypoints[i].lat;
						}
					}					
				}
			}
			if (idx == -1) {
				mLogger.error("No waypoint selected");
				return;
			} else if (idx > -1) {
				mParent.setTarget(mWaypoints[idx]);				
			} else {
				IntPoint intPoint1 = new IntPoint(10, 10);
				IntPoint intPoint2 = new IntPoint(getWidth() - 10, getHeight() - 10);
				Node n1 = new Node(n * MoreMath.FAC_RADTODEC, w * MoreMath.FAC_RADTODEC);
				Node n2 = new Node(s * MoreMath.FAC_RADTODEC, e * MoreMath.FAC_RADTODEC);
				float scale = mParent.pc.getP().getScale(n1, n2, intPoint1, intPoint2);
				mParent.receivePosition((n-s) / 2 + s, (e-w) / 2 + w, scale * 1.2f);
			}
			mParent.show();
			return;
		}
		// uncomment this for use with GuiCustomList.java
		//super.commandAction(c, d);
	}
	
	public void startProgress(String title) {
		mProgress.showProgressDisplay(title);
	}
	
	public void setProgress(String message) {
		// Not supported/used at the moment.
	}
	
	public void updateProgress(String message) {
		mProgress.addProgressText(message);
	}
	
	public void updateProgressValue(int inc) {
		mProgress.updateProgressValue(inc);
	}

	public void completedUpload(boolean success, String message) {
		String alertMsg;		
		if (mExporting) {
			if (success)
				alertMsg = "Completed GPX export: " + message;
			else {
				alertMsg = "GPX export failed: " + message;
			}
		} else if (mImporting) {
			if (success) {
				alertMsg = "Completed GPX import: " + message;
				initWaypoints();
			} else {
				alertMsg = "GPX import failed: " + message;
			}
		} else {
			if (success) {
				alertMsg = message;
				initWaypoints();
			} else {
				alertMsg = "Failed: " + message;
			}
			
		}
		mProgress.addProgressText(alertMsg);
		mProgress.finishProgressDisplay();

		//GpsMid.getInstance().alert("Information", alertMsg, Alert.FOREVER);
	}

	public void show() {
			GpsMid.getInstance().show(this);

			// Show Alert to inform the user right before showing the waypoints
			// (because the J2ME List class can not display more than
			// 255 items - at least on some phones).
			// Todo: Some Phones doesn't have this 255 Items-Limit so we may improve the code here
			if (mWaypoints.length > 255) {
				GpsMid.getInstance().alert("Info", 
					"Due to a platform restriction we display only the first 255 of " + 
					mWaypoints.length + " waypoints. Hint: Export some waypoints to " + 
					"a file and delete them to show the remaining waypoints.", 
					Alert.FOREVER);
			}
	}

	public void uploadAborted() {
		initWaypoints();				
	}

	/** Called when the user has entered the GPX filename or the new waypoint name.
	 */
	public void inputCompleted(String strResult) {
		if (mExporting) {
			if (strResult != null) {
				mProgress.showProgressDisplay("Exporting way points");
				mProgress.addProgressText("Exporting all " + mWaypoints.length + " way point(s).\n");
				mParent.gpx.exportWayPts(Configuration.getGpxUrl(), strResult, this);
			} else {
				// BACK action from name input
				show();
			}
		} else {
			// Must be rename
			if (strResult != null) {
    			// rename waypoint
    			mWaypoints[mWptId].displayName = strResult;
    			mParent.gpx.updateWayPt(mWaypoints[mWptId]);
    			// change item in list
    			set(mWptId, strResult, null);
    			// unselect changed item
    			setSelectedIndex(mWptId, false);
    		}
    		show();
		}		
	}

	/** Called when the user closes the progress popup.
	 */
	public void actionCompleted() {
		show();
	}
}
