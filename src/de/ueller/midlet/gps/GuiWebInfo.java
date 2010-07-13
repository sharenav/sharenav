package de.ueller.midlet.gps;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gps.data.Legend;
import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.Way;
import de.ueller.midlet.gps.urls.Urls;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.tile.PaintContext;

public class GuiWebInfo extends List implements GpsMidDisplayable,
		CommandListener {

	private final static Logger mLogger = Logger.getInstance(GuiWebInfo.class,
			Logger.DEBUG);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 1);
	private final Command SELECT_CMD = new Command("Select", Command.OK, 2);
	private GpsMidDisplayable mParent;
	private Position mPos;
	private Way actualWay;
	private Trace trace;

	public GuiWebInfo(GpsMidDisplayable parent, Position pos, PaintContext pc) {
		super("Info on the web", List.IMPLICIT);
		actualWay = pc.actualWay;
		trace = pc.trace;
		mParent = parent;
		mPos = pos;
		//#if polish.api.online
		this.append("Wikipedia (RSS)", null);
		this.append("Wikipedia (Web)", null);
		this.append("Weather", null);
		this.append("GeoHack", null);
		//#endif
		if (Legend.enableUrlTags) {
			this.append("Website", null);
		}
		if (Legend.enablePhoneTags) {
			this.append("Phone", null);
		}
		// FIXME add "search for name on the web" for POI names once the code to select POIS is in place
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
			if (site.equalsIgnoreCase("Wikipedia (RSS)")) {
				url = "http://ws.geonames.org/findNearbyWikipediaRSS?lat="
						+ mPos.latitude * MoreMath.FAC_RADTODEC + "&lng="
						+ mPos.longitude * MoreMath.FAC_RADTODEC;
			}
			if (site.equalsIgnoreCase("Wikipedia (Web)")) {
				url = "http://www.rss2html.com/public/rss2html.php?TEMPLATE=template-1-2-1.htm&XMLFILE=http://ws.geonames.org/findNearbyWikipediaRSS?lat="
						+ mPos.latitude * MoreMath.FAC_RADTODEC + "%26lng="
						+ mPos.longitude * MoreMath.FAC_RADTODEC;
			}
			if (site.equalsIgnoreCase("Weather")) {
				url = "http://m.wund.com/cgi-bin/findweather/getForecast?brand=mobile&query="
						+ (mPos.latitude * MoreMath.FAC_RADTODEC)
						+ "%2C"
						+ (mPos.longitude * MoreMath.FAC_RADTODEC);
			}
			if (site.equalsIgnoreCase("GeoHack")) {
				int deglat, minlat;
				float deglatf, seclat;
				int deglon, minlon;
				float deglonf, seclon;
				deglatf = Math.abs((mPos.latitude * MoreMath.FAC_RADTODEC));
				deglat = (int)deglatf;
				minlat = (int) ((deglatf - deglat) * 60);
				seclat = ((deglatf - deglat-minlat/60)*60);
				deglonf = Math.abs((mPos.longitude * MoreMath.FAC_RADTODEC));
				deglon = (int)deglonf;
				minlon = (int) ((deglonf - deglon) * 60);
				seclon = ((deglonf - deglon-minlon/60)*60);
				url = "http://toolserver.org/~geohack/geohack.php?params="
						+ deglat
						+ "_"
						+ minlat
						+ "_"
						+ seclat
						+ ((mPos.latitude < 0)?"_S_":"_N_")
						+ deglon
						+ "_"
						+ minlon
						+ "_"
						+ seclon
						+ ((mPos.longitude < 0)?"_W_":"_E_");
			}
			if (site.equalsIgnoreCase("Website")) {
				if ((actualWay != null)) {
					url = trace.getUrl(actualWay.urlIdx);
				}
			}
			if (site.equalsIgnoreCase("Phone")) {
				String phone;
				if ((actualWay != null) && ((phone = trace.getUrl(actualWay.phoneIdx)) != null)) {
					url = "tel:" + phone;
				}
			}
			try {
				if (url != null) {
					// #debug info
					mLogger.info("Platform request for " + url);
					//#if polish.api.online
					GpsMid.getInstance().platformRequest(url);
					//#else
					// FIXME: show user the url or phone number
					//#endif
				}
			} catch (Exception e) {
				mLogger.exception("Could not open url " + url, e);
			}
			mParent.show();
		}

	}

}
