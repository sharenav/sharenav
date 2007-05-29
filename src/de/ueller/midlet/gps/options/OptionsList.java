package de.ueller.midlet.gps.options;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import de.ueller.midlet.gps.Displayable;
import javax.microedition.lcdui.List;

public abstract class OptionsList extends List implements CommandListener {

	protected final Command			BACK_CMD		= new Command("Back",
			Command.BACK, 2);

	protected final Command			OK_CMD			= new Command("Ok",
			Command.ITEM, 1);

	protected Displayable parent;

	protected OptionsList(String title,String[] options,Displayable parent) {
		super(title, List.EXCLUSIVE, options, null);
		this.parent = parent;
	}

}
