/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See file COPYING
 */

package de.ueller.gpsmid.ui;

import java.util.Timer;
import java.util.TimerTask;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

//#if polish.android
import android.view.KeyEvent;
import android.widget.Toast;
import de.enough.polish.android.midlet.MidletBridge;
//#endif

import de.ueller.util.IntTree;
import de.ueller.util.Logger;
import de.ueller.gpsmid.data.Configuration;
import de.ueller.midlet.iconmenu.IconActionPerformer;

public abstract class KeyCommandCanvas extends Canvas implements
		CommandListener {
	protected static long pressedKeyTime = 0;
	protected static int pressedKeyCode = 0;
	protected static volatile long releasedKeyCode = 0;
	protected static int ignoreKeyCode = 0;

	protected static int lastGameKeyCode = 0; 	 
	protected static int lastGameAction = 0;
	
	protected volatile boolean keyboardLocked = false;

	protected IntTree singleKeyPressCommand = new IntTree();
	protected IntTree repeatableKeyPressCommand = new IntTree();
	protected IntTree doubleKeyPressCommand = new IntTree();
	protected IntTree longKeyPressCommand = new IntTree();
	protected IntTree gameKeyCommand = new IntTree();
	protected IntTree nonReleasableKeyPressCommand = new IntTree();

	protected static boolean previousBackPress = false;

	/*
	 * Explicitly make this function static, as otherwise some JVM implementations
	 * can't find the commandAction method in the inherited object.
	 */
	abstract public void commandAction(Command c, Displayable d);
	
	private final static Logger logger = Logger.getInstance(
			KeyCommandCanvas.class, Logger.DEBUG);

	protected void keyPressed(int keyCode) {
		logger.debug("keyPressed " + keyCode);
		
		//#if polish.android
		if (keyCode != KeyEvent.KEYCODE_BACK) {
			previousBackPress = false;
		}
		//#endif

		ignoreKeyCode = 0;
		pressedKeyCode = keyCode;
		pressedKeyTime = System.currentTimeMillis();
		if (keyboardLocked &&
		    (keyCode != KEY_NUM9 && keyCode != 110)) {
			GpsMid.getInstance().alert("GpsMid",
				(hasPointerEvents() ? Locale.get("keycommandcanvas.KeysAndTouchscreen")/*Keys and touchscreen locked. Hold down 9 or slide right on way bar to unlock.*/ 
						: Locale.get("keycommandcanvas.KeysLocked")/*Keys locked. Hold down 9 to unlock.*/),					
				3000);
			ignoreKeyCode = keyCode;
			return;
		}

		//#if polish.android
		//GpsMid.getInstance().alert("keycode", "keycode = " + keyCode, 3000);
		if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
			// #debug debug
			logger.debug("  Not turning key into SEARCH_CMD");
			// ignore, the HTC Desire with ICS (BCM ROM) will give this keycode on
			// multitouch action
			//commandAction(Trace.getInstance().getCommand(Trace.SEARCH_CMD), (Displayable) null);
			return;
		}
		//#endif
		
		// Handle actions for repeatable keys like direction keys immediately
		Command c = (Command) repeatableKeyPressCommand.get(keyCode);
		if (c == null) {
			/**
			 * The camera cover switch does not report a keyreleased event, so
			 * we need to special case it here in the keypressed routine
			 */
			c = (Command) nonReleasableKeyPressCommand.get(keyCode);
		}
		if (c == null) {
			c = (Command) gameKeyCommand.get(getGameActionIfNotOverloaded(keyCode));
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
			c = (Command) gameKeyCommand.get(getGameActionIfNotOverloaded(keyCode));
		}
		if (c != null) {
			keyPressed(keyCode);
			return;
		}

		long keyTime = System.currentTimeMillis();
		// other key is held down
		if ((keyTime - pressedKeyTime) >= 1000 && (pressedKeyCode == keyCode
							   // special case for Nokia N95 & 6121c
							   // which for some reason give -50 as keypress for long
							   // press of # (35) _if_ a text entry field
							   // has been used, the repeats are 35 though
							   || (keyCode == KEY_POUND && pressedKeyCode == -50))) {
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
		if (keyboardLocked && keyCode != KEY_NUM9 && keyCode != 110) {
			keyPressed(0);
			return;
		}

		// if key was not handled as held down key
		// strange seem to be working in emulator only with this debug line
		logger.debug("keyReleased " + keyCode + " ignoreKeyCode: "
				+ ignoreKeyCode + " prevRelCode: " + releasedKeyCode);
		if (keyCode == ignoreKeyCode
			||
			// key must actually have been pressed in this Canvas and not in another one, e.g. in icon menu
			keyCode != pressedKeyCode
		) {
			ignoreKeyCode = 0;
			return;
		}
		//#if polish.android
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// #debug debug
			if (Trace.getInstance().isShowingSplitScreen()) {
				Trace.getInstance().performIconAction(IconActionPerformer.BACK_ACTIONID, "Back");
			} else {			
				if (Configuration.getCfgBitState(Configuration.CFGBIT_EXIT_APPLICATION_WITH_BACK_BUTTON) || previousBackPress) {
					logger.debug("  Turning BACK key into EXIT_CMD");
					commandAction(Trace.getInstance().getCommand(Trace.EXIT_CMD), (Displayable) null);
				} else {
					Toast.makeText(MidletBridge.getInstance(), Locale.get("keycommandcanvas.BackAgainToExit")/*Press the back key again to exit GpsMid*/, Toast.LENGTH_LONG).show();				
					previousBackPress = true;
				}
			}
			return;
		} else {
			previousBackPress = false;
		}
		// interpret menu key on key up
		// done instead by onKey() in Trace
		// if (keyCode == -111) {
		//commandAction(Trace.getInstance().getCommand(Trace.ICON_MENU), (Displayable) null);
		//	Trace.getInstance().commandAction(Trace.ICON_MENU);
		//	ignoreKeyCode = keyCode;
		//	return;
		//}
		//#endif
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
					TimerTask timerT = new TimerTask() {
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
								repaint();
							}
						}
					};
					// set double press time
					GpsMid.getTimer().schedule(timerT, 300);
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

	private int getGameActionIfNotOverloaded(int keyCode) {
		// speed up repeatedly asked keyCode by returning remembered gameAction 	 
		if (lastGameKeyCode == keyCode) {
			return lastGameAction;
		} else if  (
				repeatableKeyPressCommand.get(keyCode) != null ||
				singleKeyPressCommand.get(keyCode) != null ||
				longKeyPressCommand.get(keyCode) != null ||
				doubleKeyPressCommand.get(keyCode) != null ||
				nonReleasableKeyPressCommand.get(keyCode) != null
		 ) {
			// filter out game keys that are used for other commands
			lastGameAction = 0;
		} else {
			lastGameAction = this.getGameAction(keyCode);
		}
		lastGameKeyCode = keyCode;
		return lastGameAction;
	}

	
	/**
	 * Returns all single press key codes for the specified command. 
	 * @param comm The command for which the key codes are wanted.
	 * @return IntTree containing the key codes and the command.
	 */
	public IntTree getSingleKeyPressesForCommand(Command comm) {
		IntTree keys = new IntTree();
		for (int i = 0; i < singleKeyPressCommand.size(); i++) {
			if (singleKeyPressCommand.getValueIdx(i) == comm) {
				keys.put(singleKeyPressCommand.getKeyIdx(i), comm);
			}
		}
		return keys;
	}

	/**
	 * Returns all double press key codes for the specified command. 
	 * @param comm The command for which the key codes are wanted.
	 * @return IntTree containing the key codes and the command.
	 */
	public IntTree getDoubleKeyPressesForCommand(Command comm) {
		IntTree keys = new IntTree();
		for (int i = 0; i < doubleKeyPressCommand.size(); i++) {
			if (doubleKeyPressCommand.getValueIdx(i) == comm) {
				keys.put(doubleKeyPressCommand.getKeyIdx(i), comm);
			}
		}
		return keys;
	}

	/**
	 * Returns all long press key codes for the specified command. 
	 * @param comm The command for which the key codes are wanted.
	 * @return IntTree containing the key codes and the command.
	 */
	public IntTree getLongKeyPressesForCommand(Command comm) {
		IntTree keys = new IntTree();
		for (int i = 0; i < longKeyPressCommand.size(); i++) {
			if (longKeyPressCommand.getValueIdx(i) == comm) {
				keys.put(longKeyPressCommand.getKeyIdx(i), comm);
			}
		}
		return keys;
	}
}
