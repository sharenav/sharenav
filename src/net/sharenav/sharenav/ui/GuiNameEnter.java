/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.sharenav.ui;

import javax.microedition.lcdui.*;

import net.sharenav.midlet.ui.InputListener;
import de.enough.polish.util.Locale;


/*
 * Screen to enter a name.
 */
public class GuiNameEnter extends Form implements CommandListener {
	private TextField mTextFieldName; 
	private static final Command SAVE_CMD = new Command(Locale.get("generic.OK")/*OK*/, Command.OK, 1);
	private static final Command BACK_CMD = new Command(Locale.get("generic.Cancel")/*Cancel*/, Command.OK, 2);
	private InputListener mInputListener;


	public GuiNameEnter(InputListener inputListener, String title, 
				String defaultName, int maxLen) {
		super(title);
		createForm(inputListener, "Name:", defaultName, maxLen);
	}

	public GuiNameEnter(InputListener inputListener, String title, 
				String prompt, String defaultName, int maxLen) {
		super(title);
		createForm(inputListener, prompt, defaultName, maxLen);
	}

	private void createForm(InputListener inputListener, String prompt, 
				String defaultName, int maxLen) {
		mInputListener = inputListener;

		mTextFieldName = new TextField(prompt, defaultName, maxLen, TextField.ANY);
		
		try {
			// Set up this Displayable to listen to command events
			setCommandListener(this);
			// add the commands
			addCommand(BACK_CMD);
			addCommand(SAVE_CMD);
			this.append(mTextFieldName);
		} catch (Exception e) {
			e.printStackTrace();
		}				
	}

	public void commandAction(Command cmd, Displayable displayable) {
		if (cmd == SAVE_CMD) {
			mInputListener.inputCompleted(mTextFieldName.getString());
			return;
		}
		else if (cmd == BACK_CMD) {
			mInputListener.inputCompleted(null);
	    	return;
		}
	}
	
	public synchronized void show() {
		ShareNav.getInstance().show(this);
	}
}
