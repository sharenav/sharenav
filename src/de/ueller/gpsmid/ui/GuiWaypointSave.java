/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemCommandListener;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.PositionMark;
import de.ueller.util.Logger;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
//#if polish.android
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.widget.AnalogClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import de.enough.polish.android.lcdui.ViewItem;
import de.enough.polish.android.midlet.MidletBridge;
import javax.microedition.lcdui.Display;
//#endif

public class GuiWaypointSave extends Form implements CommandListener, ItemCommandListener {
	//#if polish.android
	private ViewItem OKField;
	private ViewItem fldName;
	private TextView fldNameLabel;
	private EditText fldNameText;
	//#else
	private TextField fldName;
	//#endif
	private TextField fldEle;
	private ChoiceGroup cg;
	private static final Command saveCmd = new Command(Locale.get("guiwaypointsave.Save")/*Save*/, Command.OK, 1);
	private static final Command backCmd = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 2);
	
	private final Trace parent;
	private String name;
	private String ele;
	private PositionMark waypt;
	
	protected static final Logger logger = Logger.getInstance(GuiWaypointSave.class,Logger.TRACE);

	public GuiWaypointSave(Trace tr) {
		super(Locale.get("guiwaypointsave.SaveWaypoint")/*Save Waypoint*/);
		this.parent = tr;
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void jbInit() throws Exception {
		//#if polish.android
		fldNameLabel = new TextView(MidletBridge.getInstance());
		fldNameLabel.setText(Locale.get("guiwaypointsave.Name")/*Name:*/);
		ViewItem fldNameTitle = new ViewItem(fldNameLabel);
		fldNameText = new EditText(MidletBridge.getInstance());
		fldName = new ViewItem(fldNameText);
		//#else
		fldName = new TextField(Locale.get("guiwaypointsave.Name")/*Name:*/, "", 
				Configuration.MAX_WAYPOINTNAME_LENGTH, TextField.ANY);
		//#endif
		fldEle = new TextField(Locale.get("guiwaypointsave.AltitudeInM")/*Altitude (in m):*/, 
				"", 5, TextField.NUMERIC);
		cg = new ChoiceGroup(Locale.get("guiwaypointsave.Settings")/*Settings*/, Choice.MULTIPLE);
		cg.append(Locale.get("guiwaypointsave.Favorite")/*Favorite*/, null);
		cg.append(Locale.get("guiwaypointsave.RecenterToGPS")/*Recenter to GPS after saving*/, null);
		
		// Set up this Displayable to listen to command events
		setCommandListener(this);
		// add the commands
		addCommand(backCmd);
		addCommand(saveCmd);
		//#if polish.android
		Button saveButton = new Button(MidletBridge.getInstance());
		OKField = new ViewItem(saveButton);
		saveButton.setText(Locale.get("traceiconmenu.SaveWpt"));
		OKField.addCommand(saveCmd);
		OKField.setDefaultCommand(saveCmd);
		OKField.setItemCommandListener(this);
		this.append(fldNameTitle);
		//#endif
		this.append(fldName);
		this.append(Locale.get("guiwaypointsave.OptionalData")/*Optional data*/);
		this.append(fldEle);		
		this.append(cg);
		//#if polish.android
		fldNameText.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					//check if the right key was pressed
					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
						parent.show();
						return true;
					}
				}
				return false;
			}
		});
		Display.getDisplay(GpsMid.getInstance()).setCurrentItem(fldName);
		// perhaps this should be optional
		InputMethodManager imm = (InputMethodManager) MidletBridge.instance.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
		//#style formItem
		this.append(OKField);
		//#endif
	}
	
	public void setData(PositionMark posMark)
	{
		this.waypt = posMark;
		this.name = "";
		//#if polish.android
		fldNameText.setText(name);
		//#else
		fldName.setString(name);
		//#endif
		
		//only set default altitude if gps.ele is valid. 
		if (waypt.ele != PositionMark.INVALID_ELEVATION) {
			fldEle.setString(String.valueOf(waypt.ele));
		} else {
			fldEle.setString("");			
		}
	}

	public void saveWaypoint(PositionMark posMark) {
		parent.gpx.addWayPt(waypt);

		// Wait a bit before displaying the map again. Hopefully
		// this avoids the sporadic freezing after saving a waypoint.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ie) {
		}
	}

	public void commandAction(Command cmd, Item item) {
		commandAction(cmd, (Displayable) parent);
	}

	public void commandAction(Command cmd, Displayable displayable) {
		if (cmd == saveCmd) {
			//#if polish.android
			name = fldNameText.getText().toString();
			//#else
			name = fldName.getString();
			//#endif
			if (cg.isSelected(0) && !name.endsWith("*")) {
				name += "*";
			}
			
			ele  = fldEle.getString();
			logger.info("Saving waypoint with name: " + name + " ele: " + ele);
			waypt.displayName = name;
			// on android, there's a force close with the thread version
			// FIXME perhaps the thread version could be made to work somehow on android
			//#if polish.android
			try {
				// Use the elevation from textfield, if not "" 
				waypt.ele = Integer.parseInt(ele);
			} catch (NumberFormatException e) {
				waypt.ele = PositionMark.INVALID_ELEVATION;
			}
					
			saveWaypoint(waypt);
			// Recenter GPS after saving a Waypoint if this option is selected
			if (cg.isSelected(1)) {
				parent.gpsRecenter = true;
				parent.newDataReady();
			}
			parent.show();				
			return;
			//#else
			Thread adder = new Thread(new Runnable() {
				public void run()
				{
					try {
						// Use the elevation from textfield, if not "" 
						waypt.ele = Integer.parseInt(ele);
					} catch (NumberFormatException e) {
						waypt.ele = PositionMark.INVALID_ELEVATION;
					}
					
					saveWaypoint(waypt);
					// Recenter GPS after saving a Waypoint if this option is selected
					if (cg.isSelected(1)) {
						parent.gpsRecenter = true;
						parent.newDataReady();
					}
					parent.show();				
				}
			} );
			adder.setPriority(Thread.MAX_PRIORITY);
			adder.start();
			return;
			//#endif
		}
		else if (cmd == backCmd) {
			parent.show();
	    	return;
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(this);
	}
}
