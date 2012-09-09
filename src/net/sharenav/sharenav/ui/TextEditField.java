/*
 * ShareNav - Copyright (c) 2012 Jyrki Kuoppala jkpj at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.sharenav.ui;

import de.enough.polish.util.Locale;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;

//#if polish.android
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.TextView;
import javax.microedition.lcdui.TextField;
import de.enough.polish.android.lcdui.ViewItem;
import de.enough.polish.android.midlet.MidletBridge;
import net.sharenav.sharenav.ui.SaveButtonListener;
//#endif

//#if polish.android
public class TextEditField extends ViewItem {

	private TextView fldNameLabel;
	private ViewItem fldNameTitle;
	private EditText textField;

	public TextEditField (final String title, final String text,
			      final int length, int textType,
			      final BackKeyListener caller) {
		super(new EditText(MidletBridge.getInstance()));
		fldNameLabel = new TextView(MidletBridge.getInstance());
		fldNameLabel.setText(title);
		textField = (EditText) this._androidView;
		textField.setText(text);
		fldNameTitle = new ViewItem(fldNameLabel);
		textField.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					//check if the right key was pressed
					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
						caller.backPressed();
						return true;
					}
				}
				return false;
			}
		});
	}
	public ViewItem getTitle() {
		return fldNameTitle;
	}
	public void setString(String text) {
		textField.setText(text);
	}
	public String getString() {
		return textField.getText().toString();
	}
}
//#endif
