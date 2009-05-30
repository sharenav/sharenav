package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */
//#if polish.api.online
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.data.MoreMath;

public class GuiWebInfo extends List implements GpsMidDisplayable,
		CommandListener {

	private final static Logger mLogger = Logger.getInstance(GuiWebInfo.class,
			Logger.DEBUG);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 1);
	private final Command SELECT_CMD = new Command("Select", Command.OK, 2);
	private GpsMidDisplayable mParent;
	private Position mPos;

	public GuiWebInfo(GpsMidDisplayable parent, Position pos) {
		super("Info on the web", List.IMPLICIT);
		mParent = parent;
		mPos = pos;
		this.append("Wikipedia", null);
		this.append("Weather", null);
		this.addCommand(BACK_CMD);
		this.setCommandListener(this);
		this.setSelectCommand(SELECT_CMD);
	}

	public void show() {
		GpsMid.getInstance().show(this);

	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			mParent.show();
		}
		if (c == SELECT_CMD) {
			String site = getString(getSelectedIndex());
			String url = null;
			if (site.equalsIgnoreCase("Wikipedia")) {
				url = "http://ws.geonames.org/findNearbyWikipediaRSS?lat="
						+ mPos.latitude * MoreMath.FAC_RADTODEC + "&lng="
						+ mPos.longitude * MoreMath.FAC_RADTODEC;
			}
			if (site.equalsIgnoreCase("Weather")) {
				url = "http://m.wund.com/cgi-bin/findweather/getForecast?brand=mobile&query="
						+ (mPos.latitude * MoreMath.FAC_RADTODEC)
						+ "%2C"
						+ (mPos.longitude * MoreMath.FAC_RADTODEC);
			}
			try {
				if (url != null) {
					// #debug info
					mLogger.info("Platform request for " + url);
					GpsMid.getInstance().platformRequest(url);
				}
			} catch (Exception e) {
				mLogger.exception("Could not load Wikipedia", e);
			}
			mParent.show();
		}

	}

}
// #endif