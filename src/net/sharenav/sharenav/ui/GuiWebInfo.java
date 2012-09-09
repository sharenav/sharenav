package net.sharenav.sharenav.ui;

/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apmon at users dot sourceforge dot net 
 * See Copying
 */
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.PaintContext;
import net.sharenav.sharenav.data.Position;
import net.sharenav.sharenav.data.RoutePositionMark;
import net.sharenav.sharenav.mapdata.Way;
import net.sharenav.util.Logger;
import net.sharenav.util.MoreMath;

import de.enough.polish.util.Locale;

public class GuiWebInfo extends List implements ShareNavDisplayable,
		CommandListener {

	private final static Logger mLogger = Logger.getInstance(GuiWebInfo.class,
			Logger.DEBUG);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 1);
	private final Command SELECT_CMD = new Command(Locale.get("guiwebinfo.Select")/*Select*/, Command.OK, 2);
	private ShareNavDisplayable mParent;
	private Position mPos;
	private String mPoiUrl;
	private String mPoiPhone;
	private int mNodeID;
	private Way actualWay;
	private Trace trace;

//#if polish.api.finland
	// FIXME handle the API key some other way
	private final static String reittiopasAuth = "";
	private final static String reittiopasCoordSpec = "&epsg_in=wgs84&epsg_out=wgs84";
	private final static String reittiopasUrl = "http://api.reittiopas.fi/hsl/prod/?" + reittiopasAuth + reittiopasCoordSpec;
//#endif

	// if longtap is true, instantiate as context menu which also has nearby POI search
	public GuiWebInfo(ShareNavDisplayable parent, Position pos, PaintContext pc, boolean longtap, String poiUrl, String poiPhone,
		int nodeID) {
		super(Locale.get("guiwebinfo.ContactWebOrPhone")/*Contact by web or phone*/, List.IMPLICIT);
		actualWay = pc.trace.actualWay;
		trace = pc.trace;
		mPoiUrl = poiUrl;
		mPoiPhone = poiPhone;
		mParent = parent;
		mPos = pos;
		mNodeID = nodeID;
		if (longtap) {
			if (!trace.hasPointerEvents() && Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS)) {
				this.append(Locale.get("trace.Menu")/*Menu*/, null);
			}
			if (mNodeID == -1) {
				this.append(Locale.get("guisearch.nearestpois")/*Nearest POIs*/, null);
			}
			this.append(Locale.get("guiwaypoint.AsDestination")/*As destination*/, null);
			this.append(Locale.get("trace.CalculateRoute")/*Calculate route*/, null);
		}
		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE)) {
			//System.out.println("actualWay: " + actualWay + " urlIdx: " + actualWay.urlIdx + " url: " + trace.getUrl(actualWay.urlIdx));
			String url = null;
			if (mPoiUrl != null || ((actualWay != null) && ((url = trace.getUrl(actualWay.urlIdx)) != null))) {
				if (mPoiUrl != null) {
					url = mPoiUrl;
				}
				this.append(Locale.get("guiwebinfo.Website")/*Website*/ + " " + url, null);
			}
		}
		if (Legend.enablePhoneTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE)) {
			//System.out.println("actualWay: " + actualWay + " phoneIdx: " + actualWay.phoneIdx + " phone: " + trace.getUrl(actualWay.phoneIdx));
			String phone = null;
			if (mPoiPhone != null || ((actualWay != null) && ((phone = trace.getUrl(actualWay.phoneIdx)) != null))) {
				if (mPoiPhone != null) {
					phone = mPoiPhone;
				}
				this.append(Locale.get("guiwebinfo.Phone")/*Phone*/ + " " + phone, null);
			}
		}
		//#if polish.api.bigsearch
		//#if polish.api.osm-editing
		if (mNodeID != -1 && Legend.enableEdits) {
			this.append(Locale.get("guiwebinfo.EditPOI")/*Edit POI*/, null);
		}		
		//#endif 
		//#endif 
		//#if polish.api.online
		//this.append("Wikipedia (Web)", null);
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WIKIPEDIA_RSS)) {
			this.append(Locale.get("guiwebinfo.WikipediaRSS")/*Wikipedia (RSS)*/, null);
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEATHER)) {
			this.append(Locale.get("guiwebinfo.Weather")/*Weather*/, null);
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_GEOHACK)) {
			this.append(Locale.get("guiwebinfo.GeoHack")/*GeoHack*/, null);
		}
		//#if polish.api.osm-editing
		if (Legend.enableEdits) {
			this.append(Locale.get("guiwebinfo.editOSMWeb")/*Edit area in OSM web editor*/, null);
		}		
		//#endif
//#if polish.api.finland
		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_TOPOMAP)) {
			this.append(Locale.get("guiwebinfo.TopoMapFi")/*Topographic Map (Finland)*/, null);
		}
		this.append(Locale.get("guiwebinfo.ReittiopasAddress"), null);
		this.append(Locale.get("guiwebinfo.ReittiopasStop"), null);
//#endif
		//#endif
		// FIXME add "search for name on the web" for POI names once the code to select POIS is in place
		this.addCommand(BACK_CMD);
		this.setCommandListener(this);
		this.setSelectCommand(SELECT_CMD);
	}

//#if polish.api.finland
	public static String getReittiopasUrl() {
		return reittiopasUrl;
	}
//#endif

	public void show() {
		ShareNav.getInstance().show(this);

	}

	public void commandAction(Command c, Displayable d) {
		if (c == BACK_CMD) {
			mParent.show();
		}
		if (c == SELECT_CMD) {
			String site = getString(getSelectedIndex());
			if (site.equals(Locale.get("trace.Menu")/*Menu*/)) {
				trace.showIconMenu();
			} else if (site.equals(Locale.get("guisearch.nearestpois")/*Nearest POIs*/)) {
				try {
					GuiSearch guiSearch = new GuiSearch(trace, GuiSearch.ACTION_NEARBY_POI);
					guiSearch.show();
				} catch (Exception e) {
					mLogger.exception("Could not open GuiSearch for nearby POI search", e);
				}
			} else if (site.equals(Locale.get("guiwaypoint.AsDestination")/*As destination*/)) {
				RoutePositionMark pm1 = new RoutePositionMark(mPos.latitude, mPos.longitude);
				trace.setDestination(pm1);
				mParent.show();
			} else if (site.equals(Locale.get("trace.CalculateRoute")/*Calculate route*/)) {
				RoutePositionMark pm1 = new RoutePositionMark(mPos.latitude, mPos.longitude);
				trace.setDestination(pm1);
				trace.commandAction(Trace.ROUTING_START_CMD);
				mParent.show();
			//#if polish.api.bigsearch
			//#if polish.api.osm-editing
			} else if (site.equalsIgnoreCase(Locale.get("guiwebinfo.EditPOI")/*Edit POI*/) && trace.internetAccessAllowed()) {
					//System.out.println("Calling GuiOsmPoiDisplay: nodeID " + mNodeID);
					GuiOsmPoiDisplay guiNode = new GuiOsmPoiDisplay((int) mNodeID, null,
											mPos.latitude, mPos.longitude, mParent);
					guiNode.show();
					guiNode.refresh();
			//#endif 
			//#endif 
			} else if (trace.internetAccessAllowed()) {
				String url = getUrlForSite(site);
				openUrl(url);
				mParent.show();
			}
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
//#if polish.api.finland
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.TopoMapFi")/*Topographic Map*/)) {
			// url working at 2011-07-29
			url = "http://kansalaisen.karttapaikka.fi/kartanhaku/koordinaattihaku.html?feature=ktjraja&y="
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&x="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC)
				+ "&scale=10000&srsName=EPSG%3A4258&lang=fi";
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.ReittiopasAddress"))) {
			url = reittiopasUrl
				+ "&request=reverse_geocode&coordinate="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC) + ","
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&format=xml";
		}
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.ReittiopasStop"))) {
			url = reittiopasUrl
				+ "&request=reverse_geocode&coordinate="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC) + ","
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&result_contains=stop" + "&limit=5" + "&format=xml";
		}
//#endif
//#if polish.api.osm-editing
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.editOSMWeb"))) {
			url = "http://www.openstreetmap.org/edit?editor="
				// FIXME add configurability for other editors
				+ "potlatch"
				+ "&lat=" 
				+ (mPos.latitude * MoreMath.FAC_RADTODEC)
				+ "&lon="
				+ (mPos.longitude * MoreMath.FAC_RADTODEC)
				+ "&zoom=18";
		}
//#endif
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
		if (site.startsWith(Locale.get("guiwebinfo.Website")/*Website*/)) {
			if (mPoiUrl != null) {
				url = mPoiUrl;
			} else if (actualWay != null) {
				url = trace.getUrl(actualWay.urlIdx);
			}
		}
		if (site.startsWith(Locale.get("guiwebinfo.Phone")/*Phone*/)) {
			String phone;
			if (mPoiPhone != null) {
				url = "tel:" + mPoiPhone;
			} else if ((actualWay != null) && ((phone = trace.getUrl(actualWay.phoneIdx)) != null)) {
				url = "tel:" + phone;
			}
			// remove spaces and other special chars
			// FIXME check if better to just remove anything non-numeric
			// what, doesn't Android recognize SIP phone strings?
			//#if polish.android
			url = url.replaceAll(" ", "");
			url = url.replaceAll("/", "");
			url = url.replaceAll("-", "");
			url = url.replaceAll("\\(", "");
			url = url.replaceAll("\\)", "");
			//#endif
		}
		//#if polish.android
		String urls[] = url.split(";");
		// FIXME enable for J2ME also, present each url or phone number in menu
		url = urls[0];
		//#endif
		return url;
	}

	public static String getStaticUrlForSite(String site) {
		String url = null;
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.helptouch")/*Online help (touchscreen)*/)) {
			url = "http://sharenav.sourceforge.net/help/touch.php";
		}

		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.helpwiki")/*Online help (Gpsmid wiki)*/)) {
			url = "http://sharenav.sourceforge.net/help/wiki.php";				
		}
		//#if polish.android
		if (site.equalsIgnoreCase(Locale.get("guiwebinfo.helpwikiandroid")/*ShareNav on Android help (Gpsmid wiki)*/)) {
			url = "http://sharenav.sourceforge.net/help/android.php";
		}
		//#endif
		if (site.equalsIgnoreCase(Locale.get("trace.showmapcredit"))) {
			// FIXME add support for opening multiple URLs
			url = Locale.get("trace.mapcreditOsmCCLicenseURL");
			if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_OSM_CC_BY_SA)) {
				url = Locale.get("trace.mapcreditOsmCCLicenseURL");
			}
			if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_OSM_ODBL)) {
				url = Locale.get("trace.mapcreditOsmODbLURL");
			}
			//#if polish.api.finland
			if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_FI_LANDSURVEY)) {
				url = Locale.get("trace.mapcreditFiLandSurvey12URL");
			}
			if (Legend.getMapFlag(Legend.LEGEND_MAPFLAG_SOURCE_FI_DIGIROAD)) {
				url = Locale.get("trace.mapcreditFiDigiroadURL");
			}
			//#endif
		}
		return url;
	}

	public static void openUrl(String url) {
		try {
			if (url != null) {
				// #debug info
				mLogger.info("Platform request for " + url);
				//#if polish.api.online
				ShareNav.getInstance().platformRequest(url);
				//#else
				ShareNav.getInstance().alert (Locale.get("guisearch.OpenUrlTitle"),
							    Locale.get("guisearch.OpenUrl") +  " " + url, Alert.FOREVER);
				//#endif
			}
		} catch (Exception e) {
			mLogger.exception("Could not open url " + url, e);
		}
	}
}
