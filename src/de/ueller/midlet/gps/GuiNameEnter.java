package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.*;

import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.Proj2DMoveUp;

/*
 * GpsMid - Copyright (c) 2007 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
public class GuiNameEnter extends Form implements CommandListener {
	private TextField fldName; 
	private static final Command saveCmd = new Command("Save", Command.OK, 1);
	private static final Command backCmd = new Command("Back", Command.OK, 2);
	private Trace parentTrace;
	private GpsMidDisplayable parentDisp;
	private String name;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointSave.class,Logger.TRACE);

	public GuiNameEnter(Trace parentTrace, GpsMidDisplayable parentDisp, String title, String defaultName, int maxLen) {
		super(title);
		this.parentTrace = parentTrace;
		this.parentDisp = parentDisp;

		fldName = new TextField("Name:", defaultName, maxLen, TextField.ANY);
		
		try {
			// Set up this Displayable to listen to command events
			setCommandListener(this);
			// add the commands
			addCommand(backCmd);
			addCommand(saveCmd);
			this.append(fldName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void commandAction(Command cmd, Displayable displayable) {
		CompletionListener compListener=null;
		compListener= (CompletionListener) parentDisp;
		if (cmd == saveCmd) {
			compListener.actionCompleted(true, fldName.getString());
			return;
		}
		else if (cmd == backCmd) {
			compListener.actionCompleted(false, null);
	    	return;
		}
	}
	
	public synchronized void show() {
		GpsMid.getInstance().show(this);
	}
}
