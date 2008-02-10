package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import de.ueller.midlet.gps.data.PersistEntity;


public class GuiGpx extends List implements CommandListener,
		GpsMidDisplayable, UploadListener {

	private final static Logger logger=Logger.getInstance(GuiGpx.class,Logger.DEBUG);
	
	private final Command SEND_CMD = new Command("Send", Command.OK, 1);
	private final Command LOAD_CMD = new Command("Load", Command.ITEM, 1);
	private final Command DISP_CMD = new Command("Display", Command.ITEM, 1);
	private final Command DEL_CMD = new Command("delete", Command.ITEM, 2);	
	private final Command CLEAR_CMD = new Command("clear all", Command.ITEM, 3);	
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);

	private final Trace parent;
	
	private PersistEntity [] trks;
	//private Vector recordIdxMap;
	
	public GuiGpx(Trace parent) throws Exception {
		super("GPX tracklogs", List.EXCLUSIVE);
		this.parent = parent;
		setCommandListener(this);
		initTracks();
		
		addCommand(SEND_CMD);
		addCommand(LOAD_CMD);
		addCommand(DISP_CMD);
		addCommand(DEL_CMD);		
		addCommand(CLEAR_CMD);		
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
		logger.debug("got Command " + c);
		if (c == SEND_CMD) {
			int idx = this.getSelectedIndex();
			parent.gpx.sendTrk(parent.getConfig().getGpxUrl(), this, trks[idx]);			
			return;
		}
		if (c == LOAD_CMD) {
			GuiGpxLoad ggl = new GuiGpxLoad(this);
			ggl.show();
			return;
		}
		if (c == DISP_CMD) {
			int idx = this.getSelectedIndex();
			parent.gpx.displayTrk(trks[idx]);
			parent.show();
			return;
		}
		if (c == DEL_CMD) {
			int idx = this.getSelectedIndex();
			parent.gpx.deleteTrk(trks[idx]);			
			initTracks();
			return;
		}		
		if (c == BACK_CMD) {			
			parent.show();
			return;
		}

	}
	
	public void completedUpload() {
		Alert alert = new Alert("Information");
		alert.setString("Completed GPX upload");
		Display.getDisplay(parent.getParent()).setCurrent(alert);
	}

	public void show() {
		Display.getDisplay(parent.getParent()).setCurrent(this);
	}
}
