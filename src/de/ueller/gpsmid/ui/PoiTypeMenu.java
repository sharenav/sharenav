/*
 * GpsMid - Copyright (c) 2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net 
 * See COPYING
 * Copyright (C) 2009  Kai Krueger for SelectedMenuItem derived from PoiTypeSelectMenuItem
 */

package de.ueller.gpsmid.ui;

import de.enough.polish.util.Locale;

import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.names.NumberCanon;
import de.ueller.gpsmid.ui.Trace;
import de.ueller.midlet.ui.KeySelectMenuItem;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;

//#if polish.android
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import de.enough.polish.android.lcdui.CanvasBridge;
import de.enough.polish.android.lcdui.ViewItem;
import de.enough.polish.android.midlet.MidletBridge;
import de.ueller.gpsmid.ui.SaveButtonListener;
//#endif

//#if polish.android
public class PoiTypeMenu extends ViewItem {

	private SimpleAdapter adapter;
	private Displayable parent;

	public PoiTypeMenu(final KeySelectMenuReducedListener caller) {
		super(new ListView(MidletBridge.getInstance()));
		ListView listView = (ListView) this._androidView;
		View androidView = (View) CanvasBridge.current();
		androidView.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_UP)
				{
					//check if the right key was pressed
					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
						//Trace.getInstance().commandAction(Trace.BACK_CMD);
						caller.keySelectMenuCancel();
						return true;
					}
				}
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
				return false;
			}
		});
		listView.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_UP)
				{
					//check if the right key was pressed
					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
						//Trace.getInstance().commandAction(Trace.BACK_CMD);
						caller.keySelectMenuCancel();
						return true;
					}
				}
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					return true;
				}
				return false;
			}
		});
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
				//System.out.println("Position: " + position);
				caller.keySelectMenuItemSelected((short) position);
				Toast.makeText(MidletBridge.getInstance(), Locale.get("poitypemenu.selected")/*POI type selected*/, Toast.LENGTH_LONG).show();				
			}
		});
		// FIXME make this work, using position or id doesn't work (only gives the position counted from the visible items,
		// not poitype which should be counted from all items)
		//listView.setTextFilterEnabled(true);
		listView.setAdapter(new ArrayAdapter<String>(MidletBridge.getInstance(), android.R.layout.simple_list_item_1, Legend.getPoiDescriptions()));
	}
}
//#endif
