/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
package net.sharenav.sharenav.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.enough.polish.util.Locale;

import net.sharenav.util.IntTree;

public class GuiKeyShortcuts extends List implements CommandListener,
		ShareNavDisplayable {

	private static final Command CMD_BACK = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 3);
	private ShareNavDisplayable parent;

	public GuiKeyShortcuts(ShareNavDisplayable parent) {
		super(Locale.get("guikeyshortcuts.KeyShortcutsInMap")/*Key shortcuts in Map*/, List.IMPLICIT);
		addCommand(CMD_BACK);

		// Set up this Displayable to listen to command events
		setCommandListener(this);
		this.parent = parent;

		IntTree keyMap = Trace.getInstance().singleKeyPressCommand;
		this.append(Locale.get("guikeyshortcuts.SingleKeyPresses")/*Single key presses:*/, null);
		for (int i = 0; i < keyMap.size(); i++) {
			int key = keyMap.getKeyIdx(i);
			if (key > 31) {
				this.append((char) keyMap.getKeyIdx(i) + ": "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			} else {
				this.append("'" + keyMap.getKeyIdx(i) + "': "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			}
		}
		keyMap = Trace.getInstance().repeatableKeyPressCommand;
		this.append(" ", null);
		this.append(Locale.get("guikeyshortcuts.RepeatableKeyPresses")/*Repeatable key presses:*/, null);
		for (int i = 0; i < keyMap.size(); i++) {
			int key = keyMap.getKeyIdx(i);
			if (key > 31) {
				this.append((char) keyMap.getKeyIdx(i) + ": "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			} else {
				this.append("'" + keyMap.getKeyIdx(i) + "': "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			}
		}
		keyMap = Trace.getInstance().longKeyPressCommand;
		this.append(" ", null);
		this.append(Locale.get("guikeyshortcuts.LongKeyPresses")/*Long key presses:*/, null);
		for (int i = 0; i < keyMap.size(); i++) {
			int key = keyMap.getKeyIdx(i);
			if (key > 31) {
				this.append((char) keyMap.getKeyIdx(i) + ": "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			} else {
				this.append("'" + keyMap.getKeyIdx(i) + "': "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			}
		}
		keyMap = Trace.getInstance().doubleKeyPressCommand;
		this.append(" ", null);
		this.append(Locale.get("guikeyshortcuts.DoubleKeyPresses")/*Double key presses:*/, null);
		for (int i = 0; i < keyMap.size(); i++) {
			int key = keyMap.getKeyIdx(i);
			if (key > 31) {
				this.append((char) keyMap.getKeyIdx(i) + ": "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			} else {
				this.append("'" + keyMap.getKeyIdx(i) + "': "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			}
		}

		keyMap = Trace.getInstance().gameKeyCommand;
		this.append(" ", null);
		this.append(Locale.get("guikeyshortcuts.GameActionKeyPresses")/*Game Action key presses:*/, null);
		for (int i = 0; i < keyMap.size(); i++) {
			int key = keyMap.getKeyIdx(i);
			if (key > 31) {
				this.append((char) keyMap.getKeyIdx(i) + ": "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			} else {
				this.append("'" + keyMap.getKeyIdx(i) + "': "
						+ ((Command) keyMap.getValueIdx(i)).getLabel(), null);
			}
		}

	}

	public void commandAction(Command c, Displayable d) {
		if (c == CMD_BACK) {
			parent.show();
		}
	}

	public void show() {
		ShareNav.getInstance().show(this);
	}

}
