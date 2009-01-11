/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.midlet.gps;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gps.tools.intTree;

public class GuiKeyShortcuts extends List implements CommandListener,
		GpsMidDisplayable {

	private static final Command CMD_BACK = new Command("Back", Command.BACK, 3);
	private GuiDiscover parent;

	public GuiKeyShortcuts(GuiDiscover parent) {
		super("Key shortcuts in Map", List.IMPLICIT);
		addCommand(CMD_BACK);

		// Set up this Displayable to listen to command events
		setCommandListener(this);
		this.parent = parent;

		intTree keyMap = Trace.getInstance().singleKeyPressCommand;
		this.append("Single key presses:", null);
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
		this.append("Repeatable key presses:", null);
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
		this.append("Long key presses:", null);
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
		this.append("Double key presses:", null);
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
		this.append("Game Action key presses:", null);
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
		GpsMid.getInstance().show(this);
	}

}
