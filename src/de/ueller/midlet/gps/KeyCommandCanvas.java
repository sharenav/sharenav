/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.midlet.gps;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import de.ueller.gps.tools.intTree;

public abstract class KeyCommandCanvas extends Canvas implements
		CommandListener {
	protected static long pressedKeyTime = 0;
	protected static int pressedKeyCode = 0;
	protected static volatile long releasedKeyCode = 0;
	protected static int ignoreKeyCode = 0;

	protected boolean keyboardLocked = false;

	protected intTree singleKeyPressCommand = new intTree();
	protected intTree repeatableKeyPressCommand = new intTree();
	protected intTree doubleKeyPressCommand = new intTree();
	protected intTree longKeyPressCommand = new intTree();
	protected intTree gameKeyCommand = new intTree();
	protected intTree nonReleasableKeyPressCommand = new intTree();

	/*
	 * Explicitly make this function static, as otherwise some jvm implementations
	 * can't find the commandAction method in the inherited object.
	 */
	abstract public void commandAction(Command c, Displayable d);
	
	private final static Logger logger = Logger.getInstance(
			KeyCommandCanvas.class, Logger.DEBUG);

	protected void keyPressed(int keyCode) {
		logger.debug("keyPressed " + keyCode);
		ignoreKeyCode = 0;
		pressedKeyCode = keyCode;
		pressedKeyTime = System.currentTimeMillis();
		if (keyboardLocked && keyCode != KEY_NUM9) {
			GpsMid.getInstance().alert("GpsMid",
					"Keys locked. Hold down '9' to unlock.", 1500);
			ignoreKeyCode = keyCode;
			return;
		}

		// handle actions for repeatable keys like direction keys immediately
		Command c = (Command) repeatableKeyPressCommand.get(keyCode);
		if (c == null) {
			/**
			 * The camera cover switch does not report a keyreleased event, so
			 * we need to special case it here in the keypressed routine
			 */
			c = (Command) nonReleasableKeyPressCommand.get(keyCode);
		}
		if (c == null) {
			c = (Command) gameKeyCommand.get(getGameAction(keyCode));
		}
		if (c != null) {
			logger.debug("KeyPressed " + keyCode + " executing command " + c);
			commandAction(c, (Displayable) null);
		}
		repaint(0, 0, getWidth(), getHeight());
	}

	protected void keyRepeated(int keyCode) {
		// strange seem to be working in emulator only with this debug line
		logger.debug("keyRepeated " + keyCode);
		// Scrolling should work with repeated keys the same
		// as pressing the key multiple times
		if (keyCode == ignoreKeyCode) {
			logger.debug("key ignored " + keyCode);
			return;
		}

		// repeat actions for repeatable keys like direction keys and manual
		// rotation keys
		Command c = (Command) repeatableKeyPressCommand.get(keyCode);
		if (c == null) {
			c = (Command) gameKeyCommand.get(getGameAction(keyCode));
		}
		if (c != null) {
			keyPressed(keyCode);
			return;
		}

		long keyTime = System.currentTimeMillis();
		// other key is held down
		if ((keyTime - pressedKeyTime) >= 1000 && pressedKeyCode == keyCode) {
			Command longC = (Command) longKeyPressCommand.get(keyCode);
			// #debug debug
			logger.debug("long key pressed " + keyCode + " executing command "
					+ longC);
			if (longC != null) {
				ignoreKeyCode = keyCode;
				commandAction(longC, (Displayable) null);
			}
		}
	}

	// manage keys that would have different meanings when
	// held down in keyReleased
	protected void keyReleased(final int keyCode) {
		// show alert in keypressed() that keyboard is locked
		if (keyboardLocked && keyCode == KEY_NUM9) {
			keyPressed(0);
			return;
		}

		// if key was not handled as held down key
		// strange seem to be working in emulator only with this debug line
		logger.debug("keyReleased " + keyCode + " ignoreKeyCode: "
				+ ignoreKeyCode + " prevRelCode: " + releasedKeyCode);
		if (keyCode == ignoreKeyCode) {
			ignoreKeyCode = 0;
			return;
		}
		final Command doubleC = (Command) doubleKeyPressCommand.get(keyCode);
		// key was pressed twice quickly
		if (releasedKeyCode == keyCode) {
			releasedKeyCode = 0;
			// #debug debug
			logger.debug("double key pressed " + keyCode
					+ " executing command " + doubleC);
			if (doubleC != null) {
				commandAction(doubleC, (Displayable) null);
			}
		} else {
			releasedKeyCode = keyCode;
			final Command singleC = (Command) singleKeyPressCommand
					.get(keyCode);
			// #debug debug
			logger.debug("single key initiated " + keyCode
					+ " executing command " + singleC);
			if (singleC != null) {
				if (doubleC != null) {
					TimerTask timerT;
					Timer tm = new Timer();
					timerT = new TimerTask() {
						public void run() {
							if (releasedKeyCode == keyCode) {
								// #debug debug
								logger.debug("single key pressed " + keyCode
										+ " delayed executing command "
										+ singleC);
								// key was not pressed again within double press
								// time
								commandAction(singleC, (Displayable) null);
								releasedKeyCode = 0;
								repaint(0, 0, getWidth(), getHeight());
							}
						}
					};
					// set double press time
					tm.schedule(timerT, 300);
				} else {
					// #debug debug
					logger.debug("single key pressed " + keyCode
							+ " executing command " + singleC);
					commandAction(singleC, (Displayable) null);
					releasedKeyCode = 0;
				}
			}
		}
		repaint();
	}

}
