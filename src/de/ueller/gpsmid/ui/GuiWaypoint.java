/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gps.Node;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.PositionMark;
import de.ueller.gpsmid.data.RoutePositionMark;
import de.ueller.gpsmid.graphics.Projection;
import de.ueller.gpsmid.graphics.ProjFactory;
import de.ueller.gpsmid.tile.WaypointsTile;
import de.ueller.midlet.ui.CompletionListener;
import de.ueller.midlet.ui.InputListener;
import de.ueller.midlet.ui.ProgressDisplay;
import de.ueller.midlet.ui.UploadListener;
import de.ueller.util.HelperRoutines;
import de.ueller.util.IntPoint;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;

import de.enough.polish.util.Locale;

public class GuiWaypoint extends /*GuiCustom*/List implements CommandListener,
		GpsMidDisplayable, UploadListener, InputListener, CompletionListener {

	private final static Logger mLogger = Logger.getInstance(GuiWaypoint.class, Logger.DEBUG);
	
	private final Command EXPORT_ALL_CMD = new Command(Locale.get("guiwaypoint.ExportAll")/*Export All*/, Command.ITEM, 2);
	private final Command IMPORT_CMD = new Command(Locale.get("guiwaypoint.Import")/*Import*/, Command.ITEM, 3);
	private final Command RENAME_CMD = new Command(Locale.get("guiwaypoint.Rename")/*Rename*/, Command.ITEM, 3);
	private final Command DEL_CMD = new Command(Locale.get("generic.Delete")/*Delete*/, Command.ITEM, 3);
	private final Command SALL_CMD = new Command(Locale.get("guiwaypoint.SelectAll")/*Select All*/, Command.ITEM, 3);
	private final Command DSALL_CMD = new Command(Locale.get("guiwaypoint.DeselectAll")/*Deselect All*/, Command.ITEM, 3);
	private final Command SORT_MENU_CMD = new Command(Locale.get("guiwaypoint.Sorting")/*Sorting*/, Command.ITEM, 3);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 5);
	private final Command DEST_CMD = new Command(Locale.get("guiwaypoint.AsDestination")/*As destination*/, Command.ITEM, 6);
	private final Command DISP_CMD = new Command(Locale.get("guiwaypoint.Display")/*Display*/, Command.OK, 6);
	private final Command DISP_OUTLINE_CMD = new Command(Locale.get("guiwaypoint.DisplayOutline")/*Display outline*/, Command.ITEM, 6);

	/** Vector containing all waypoints currently in the application. */
	private Vector mWaypoints;
	
	/** Reference to the Trace class, needed to access the Gpx class and
	 * to switch back to the map. */
	private final Trace mParent;
	
	/** Alert to display the progress of the operations. */
	private final ProgressDisplay mProgress;
	
	/** Index of the waypoint to be renamed. */
	private static int mWptIdx;
	
	/** Flag whether we are currently exporting waypoints */
	private boolean mExporting;

	/** Flag whether we are currently importing waypoints */
	private boolean mImporting;
	
	
	public GuiWaypoint(Trace parent) throws Exception {
		super(Locale.get("guiwaypoint.Waypoints")/*Waypoints*/, List.MULTIPLE);
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
		addCommand(SORT_MENU_CMD);
		addCommand(BACK_CMD);
		addCommand(DEST_CMD);
		addCommand(DISP_CMD);
		addCommand(DISP_OUTLINE_CMD);
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

		mWaypoints = mParent.gpx.listWayPoints();
		
		// save total waypoints
		int count_waypoints = mWaypoints.size();
		
		// Limit display of Waypoints to 255 because some Phones can only display 255 listitems
		if (count_waypoints > 255) {
			count_waypoints = 255;
		}
		for (int i = 0; i < count_waypoints; i++) {
			if ((getWaypoint(i).displayName == null) || (getWaypoint(i).displayName.equals(""))) {
				//#style listItem
				this.append("(" + Locale.get("guiwaypoint.unnamed")/*unnamed*/ + ")", null);
			} else {
				//#style listItem
				this.append(getWaypoint(i).displayName, null);
			}
		}
		this.setTitle(Locale.get("guiwaypoint.Waypoints")/*Waypoints*/ +  " (" + count_waypoints + ")");
	}

	public void commandAction(Command c, Displayable d) {
		//#debug debug
		mLogger.debug("GuiWaypoint got Command " + c);
		if (c == RENAME_CMD) {
			mExporting = false;
			mImporting = false;
			boolean[] sel = new boolean[mWaypoints.size()];
			this.getSelectedFlags(sel);
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					mWptIdx = i;
					GuiNameEnter gne = new GuiNameEnter(this, Locale.get("guiwaypoint.RenameWaypoint")/*Rename Waypoint*/,
							getWaypoint(i).displayName, Configuration.MAX_WAYPOINTNAME_LENGTH);
					gne.show();
					break;
				}
			}
			return;
		}
		if (c == DEL_CMD) {
			Vector idsToDelete = new Vector();
			boolean[] sel = new boolean[mWaypoints.size()];
			this.getSelectedFlags(sel);
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					// mParent.gpx.deleteWayPt(mWaypoints[i], this);
					idsToDelete.addElement(new Integer(getWaypoint(i).id));
				}
			}
			mLogger.info("Delete waypoints, idsToDelete=" + idsToDelete.size() +
					" sel.length=" + sel.length +
					" mWaypoints.length=" + mWaypoints.size());
			if (idsToDelete.size() > 0)
			{
				mExporting = false;
				mImporting = false;
				mProgress.showProgressDisplay(Locale.get("guiwaypoint.DeletingWayPoints")/*Deleting way points*/);
				mProgress.addProgressText(Locale.get("guiwaypoint.Deleting")/*Deleting*/ +  " " + idsToDelete.size() + " " + Locale.get("guiwaypoint.wayPoints")/*way point(s)*/ + ".\n");
				mParent.gpx.deleteWayPts(idsToDelete, this);
			}
			return;
		}
		if ((c == SALL_CMD) || (c == DSALL_CMD)) {
			boolean select = (c == SALL_CMD);
			boolean[] sel = new boolean[mWaypoints.size()];
			for (int i = 0; i < mWaypoints.size(); i++) {
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
			GuiNameEnter gne = new GuiNameEnter(this, Locale.get("guiwaypoint.SendAsWithoutGPX")/*Send as (without .gpx)*/,
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
		if (c == DISP_CMD) {
			WaypointsTile.wptOutlineLat = null;
			WaypointsTile.wptOutlineLon = null;
			handleDisplayWaypoint(false);
			return;
		}
		if (c == DISP_OUTLINE_CMD) {
			boolean[] sel = new boolean[mWaypoints.size()];
			this.getSelectedFlags(sel);
			int num = 0;
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					num++;
				}
			}
			WaypointsTile.wptOutlineLat = new float[num];
			WaypointsTile.wptOutlineLon = new float[num];
			int i2 = 0;
			for (int i = 0; i < sel.length; i++) {
				if (sel[i]) {
					WaypointsTile.wptOutlineLat[i2] = getWaypoint(i).lat; 
					WaypointsTile.wptOutlineLon[i2++] = getWaypoint(i).lon;
				}
			}		
			handleDisplayWaypoint(false);
			return;
		}
		if (c == DEST_CMD) {
			handleDisplayWaypoint(true);
			return;
		}
		if (c == SORT_MENU_CMD) {
			GuiWaypointSorting gws = new GuiWaypointSorting(this);
			gws.show();
			return;
		}
		// uncomment this for use with GuiCustomList.java
		//super.commandAction(c, d);
	}

	/**
	 * Sets the first selected way point as target (setAsDestination=true).
	 * or
	 * Moves the map to the first selected way point (setAsDestination=false).
	 * then tells the map to show itself 
	 */
	private void handleDisplayWaypoint(boolean setAsDestination) {
		float w = 0, e = 0, n = 0, s = 0;
		int idx = -1;
		boolean[] sel = new boolean[mWaypoints.size()];
		this.getSelectedFlags(sel);
		PositionMark wp;
		for (int i = 0; i < sel.length; i++) {
			if (sel[i]) {
				wp = getWaypoint(i);
				if (idx == -1) {
					idx = i;
					w =  wp.lon;
					e =  wp.lon;
					n = wp.lat;
					s = wp.lat;
				} else {
					idx = -2;
					if (wp.lon < w) {
						w = wp.lon;
					}
					if (wp.lon > e) {
						e = wp.lon;
					}
					if (wp.lat < s) {
						s = wp.lat;
					}
					if (wp.lat > n) {
						n = wp.lat;
					}
				}
			}
		}
		if (idx == -1) {
			mLogger.error("No waypoint selected");
			return;
		} else if (idx > -1) {  // only one waypoint selected
 			if (setAsDestination) {
				mParent.setDestination(new RoutePositionMark(getWaypoint(idx), -1));
			} else {
				mParent.receivePosition(n, w, Configuration.getRealBaseScale());
			}
		} else {		// two or more waypoints selected, won't set as destination
			IntPoint intPoint1 = new IntPoint(10, 10);
			IntPoint intPoint2 = new IntPoint(mParent.getWidth() - 10, mParent.getHeight() - 10);
			Node n1 = new Node(n, w, true);
			Node n2 = new Node(s, e, true);
			Node center = new Node((n-s) / 2 + s,(e-w) / 2 + w, true);
			Projection p = ProjFactory.getInstance(center,mParent.getCourse(),5000,mParent.getWidth(),mParent.getHeight());
			float scale = p.getScale(n1, n2, intPoint1, intPoint2);
			mParent.receivePosition(center.radlat, center.radlon, scale * 2.25f);
		}
		mParent.show();
		return;
	}
	
	/** Provides easy access to the way points
	 * @param i Index of way point
	 * @return Way point or null if no way points were loaded or index is out of range
	 */
	private PositionMark getWaypoint(int i) {
		if (mWaypoints != null && i < mWaypoints.size()) {
			return (PositionMark)mWaypoints.elementAt(i);
		} else {
			return null;
		}
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
		// Seems the activity is sometimes ready before the popup is open,
		// so let's wait a little to make sure the message isn't lost.
		try {
			Thread.sleep( 500 );
		} catch (InterruptedException ie) {
		}
		
		String alertMsg;
		if (mExporting) {
			if (success) {
				alertMsg = Locale.get("guiwaypoint.CompletedGPXExport")/*Completed GPX export: */ + message;
			} else {
				alertMsg = Locale.get("guiwaypoint.GPXExportFailed")/*GPX export failed: */ + message;
			}
		} else if (mImporting) {
			if (success) {
				alertMsg = Locale.get("guiwaypoint.CompletedGPXImport")/*Completed GPX import: */ + message;
				initWaypoints();
			} else {
				alertMsg = Locale.get("guiwaypoint.GPXImportFailed")/*GPX import failed: */ + message;
			}
		} else {
			if (success) {
				alertMsg = message;
				initWaypoints();
			} else {
				alertMsg = Locale.get("guiwaypoint.Failed")/*Failed: */ + message;
			}
			
		}
		mProgress.addProgressText(alertMsg);
		mProgress.finishProgressDisplay();

		//GpsMid.getInstance().alert("Information", alertMsg, Alert.FOREVER);
	}

	public void show() {
		show(false);
	}
	
	public void show(boolean refresh) {
			if (refresh) {
				initWaypoints();
			}
		
			GpsMid.getInstance().show(this);

			// Show Alert to inform the user right before showing the waypoints
			// (because the J2ME List class can not display more than
			// 255 items - at least on some phones).
			// Todo: Some Phones doesn't have this 255 Items-Limit so we may improve the code here
			if (mWaypoints.size() > 255) {
				GpsMid.getInstance().alert(Locale.get("guiwaypoint.Info")/*Info*/,
							   Locale.get("guiwaypoint.DueToPlatform")/*Due to a platform restriction we display only the first 255 of */ +
						   mWaypoints.size() + Locale.get("guiwaypoint.HintExport")/* waypoints. Hint: Export some waypoints to */ +
						   Locale.get("guiwaypoint.DeleteThem")/*a file and delete them to show the remaining waypoints.*/,
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
				mProgress.showProgressDisplay(Locale.get("guiwaypoint.ExportingWayPoints")/*Exporting way points*/);
				mProgress.addProgressText(Locale.get("guiwaypoint.ExportingAll")/*Exporting all */ + mWaypoints.size() + " " + Locale.get("guiwaypoint.wayPoints")/*way point(s)*/ + ".\n");
				mParent.gpx.exportWayPts(Configuration.getGpxUrl(), strResult, this);
			} else {
				// BACK action from name input
				show();
			}
		} else {
			// Must be rename
			if (strResult != null) {
    			// rename waypoint
    			getWaypoint(mWptIdx).displayName = strResult;
    			mParent.gpx.updateWayPt(getWaypoint(mWptIdx));
    			// change item in list
    			set(mWptIdx, strResult, null);
    			// unselect changed item
    			setSelectedIndex(mWptIdx, false);
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
