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
import android.view.View;
import android.widget.Button;
import de.enough.polish.android.lcdui.ViewItem;
import de.enough.polish.android.midlet.MidletBridge;
import net.sharenav.sharenav.ui.SaveButtonListener;
//#endif

//#if polish.android
public class SaveButton extends ViewItem {
	public SaveButton (final String text, final SaveButtonListener listener, final Displayable parent, final Command saveCommand) {
		super(new Button(MidletBridge.getInstance()));
		Button saveButton = (Button) this._androidView;
		saveButton.setText(text);
		saveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				listener.commandAction(saveCommand, parent);
			}
		    });
	}
}
//#endif
