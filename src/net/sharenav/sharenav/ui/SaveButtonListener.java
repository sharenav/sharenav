package net.sharenav.sharenav.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

public interface SaveButtonListener {
	public void commandAction(Command c, Displayable d);
	public void backPressed();
}
