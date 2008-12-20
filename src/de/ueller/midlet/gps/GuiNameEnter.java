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
	private static final Command saveCmd = new Command("Ok", Command.OK, 1);
	private static final Command backCmd = new Command("Cancel", Command.OK, 2);
	private CompletionListener compListener;
	private Displayable oldDisplay;
	private String name;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointSave.class,Logger.TRACE);

	public GuiNameEnter(CompletionListener compListener, String title, String defaultName, int maxLen) {
		super(title);
		this.compListener = compListener;

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
		if (cmd == saveCmd) {
			this.compListener.actionCompleted(fldName.getString());
			return;
		}
		else if (cmd == backCmd) {
			this.compListener.actionCompleted(null);
	    	return;
		}
	}
	
	public synchronized void show() {
		GpsMid.getInstance().show(this);
	}
}
