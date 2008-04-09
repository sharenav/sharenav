package de.ueller.midlet.gps.options;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import de.ueller.midlet.gps.GpsMidDisplayable;

import de.ueller.gps.data.Configuration;

/**
 * @deprecated
 * 
 */
public class OptionsRender extends OptionsList {

	private static final String[] optionStrings={"as lines","as street"};
	private static final int[] options={Configuration.RENDER_LINE,Configuration.RENDER_STREET};
	private final Configuration config;

	public OptionsRender(GpsMidDisplayable parent,Configuration config) {
		super("Render Map", optionStrings,parent);
		this.config = config;
		addCommand(OK_CMD);
		addCommand(BACK_CMD);
		setCommandListener(this);
		setSelectedIndex(config.getRender(), true);
	}
	public void commandAction(Command c, javax.microedition.lcdui.Displayable d) {
		if (c == BACK_CMD){
			parent.show();
		} else if (c == OK_CMD){
			config.setRender(options[getSelectedIndex()]);
			parent.show();
		}
		
	}

}
