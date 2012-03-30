package de.ueller.gpsmid.ui;

/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PaintContext;
import de.ueller.gpsmid.data.Position;
import de.ueller.gpsmid.mapdata.Way;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;

import de.enough.polish.util.Locale;

public class GuiWebInfo extends List implements GpsMidDisplayable,
		CommandListener {

	private final static Logger mLogger = Logger.getInstance(GuiWebInfo.class,
			Logger.DEBUG);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 1);
	private final Command SELECT_CMD = new Command(Locale.get("guiwebinfo.Select")/*Select*/, Command.OK, 2);
	private GpsMidDisplayable mParent;
	private Position mPos;
	private Way actualWay;
	private Trace trace;

	public GuiWebInfo(GpsMidDisplayable parent, Position pos, PaintContext pc) {
		super(Locale.get("guiwebinfo.ContactWebOrPhone")/*Contact by web or phone*/, List.IMPLICIT);
		actualWay = pc.actualWay;
		trace = pc.trace;
		mParent = parent;
		mPos = pos;
		//#if polish.api.online
		this.append(Locale.get("guiwebinfo.helptouch")/*Online help (touchscreen)*/, null);
		this.append(Locale.get("guiwebinfo.helpwiki")/*Online help (GpsMid wiki)*/, null);
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WIKIPEDIA_RSS)) {
			this.append(Locale.get("guiwebinfo.WikipediaRSS")/*Wikipedia (RSS)*/, null);
		}
		//this.append("Wikipedia (Web)", null);
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEATHER)) {
			this.append(Locale.get("guiwebinfo.Weather")/*Weather*/, null);
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_GEOHACK)) {
			this.append(Locale.get("guiwebinfo.GeoHack")/*GeoHack*/, null);
		}
		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_TOPOMAP)) {
			this.append(Locale.get("guiwebinfo.TopoMapFi")/*Topographic Map (Finland)*/, null);
		}
		//#endif
		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE)) {
			this.append(Locale.get("guiwebinfo.Website")/*Website*/, null);
		}
		if (Legend.enablePhoneTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE)) {
			this.append(Locale.get("guiwebinfo.Phone")/*Phone*/, null);
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
			String url = getUrlForSite(site);
			if (url == null) {
				url = getStaticUrlForSite(site);
			}
			openUrl(url);
			mParent.show();
		}
	}

	public String getUrlForSite(String site) {
		String url = null;
		// checked url at 2010-01-09; free servers overloaded, can't test what's the difference
		// between full and non-full
		// http://ws.geonames.org/findNearbyWikipediaRSS?lat=47&lng=9&style=full
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.WikipediaRSS")/*Wikipedia (RSS)*/)) {
			String lang = "";
			if (! Configuration.getOnlineLangString().equals("en")) {
				lang = "lang=" + Configuration.getOnlineLangString() + "&";
			}
			url = "http://ws.geonames.org/findNearbyWikipediaRSS?" + lang + "lat="
				+ mPos.latitude * MoreMath.FAC_RADTODEC + "&lng="
				+ mPos.longitude * MoreMath.FAC_RADTODEC;
		}
		/*
		 * rss2html.com public service has closed down, 2010-08-01
		 *
		 if (site.equalsIgnoreCase("Wikipedia (Web)")) {
		 url = "http://www.rss2html.com/public/rss2html.php?TEMPLATE=template-1-2-1.htm&XMLFILE=http://ws.geonames.org/findNearbyWikipediaRSS?lat="
		 + mPos.latitude * MoreMath.FAC_RADTODEC + "%26lng="
		 + mPos.longitude * MoreMath.FAC_RADTODEC;
		 }
		*/

		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.Weather")/*Weather*/)) {
			// weather underground doesn't seem to have a language switch
			// url working at 2010-01-09
			url = "http://m.wund.com/cgi-bin/findweather/getForecast?brand=mobile&query="
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "%2C"
				+ (mPos.longitude * MoreMath.FAC_RADTODEC);
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.TopoMapFi")/*Topographic Map*/)) {
			// url working at 2011-07-29
			url = "http://kansalaisen.karttapaikka.fi/kartanhaku/koordinaattihaku.html?feature=ktjraja&y="
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&x="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC)
				+ "&scale=10000&srsName=EPSG%3A4258&lang=fi";
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.GeoHack")/*GeoHack*/)) {
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
			String lang = "";
			// checked on 2010-01-09: url syntax has changed to:
			// fi: http://toolserver.org/~geohack/fi/60_12_12.185211_N_24_39_39.566917_E_
			// en: http://toolserver.org/~geohack/en/60_12_12.185211_N_24_39_39.566917_E_

			url = "http://toolserver.org/~geohack/" + Configuration.getOnlineLangString() + "/"
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
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.Website")/*Website*/)) {
			// FIXME way urls are quite rare, should add support for POI urls
			if ((actualWay != null)) {
				url = trace.getUrl(actualWay.urlIdx);
			}
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.Phone")/*Phone*/)) {
			String phone;
			if ((actualWay != null) && ((phone = trace.getUrl(actualWay.phoneIdx)) != null)) {
				url = "tel:" + phone;
			}
		}
		return url;
	}

	public static String getStaticUrlForSite(String site) {
		String url = null;
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.helptouch")/*Online help (touchscreen)*/)) {
			url = "https://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=Touchscreen_Layout";
		}

		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.helpwiki")/*Online help (Gpsmid wiki)*/)) {
			url = "https://sourceforge.net/apps/mediawiki/gpsmid/index.php?title=Main_Page";				
		}
		return url;
	}

	public static void openUrl(String url) {
		try {
			if (url != null) {
				// #debug info
				mLogger.info("Platform request for " + url);
				//#if polish.api.online
				GpsMid.getInstance().platformRequest(url);
				//#else
				GpsMid.getInstance().alert (Locale.get("guisearch.OpenUrlTitle"),
							    Locale.get("guisearch.OpenUrl") +  " " + url, Alert.FOREVER);
				//#endif
			}
		} catch (Exception e) {
			mLogger.exception("Could not open url " + url, e);
		}
	}
}
