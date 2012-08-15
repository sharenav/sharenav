/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See COPYING
 */

package de.ueller.gpsmid.ui;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.lang.Integer;
//#if polish.api.bigsearch
import java.util.Hashtable;
//import java.util.Enumeration;
//#endif
//#if polish.android
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;
import de.enough.polish.android.lcdui.AndroidDisplay;
import de.enough.polish.android.lcdui.ViewItem;
import de.enough.polish.android.midlet.MidletBridge;
import javax.microedition.lcdui.Display;
//#endif

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.data.Legend;
import de.ueller.gpsmid.data.PositionMark;
import de.ueller.gpsmid.data.RoutePositionMark;
import de.ueller.gpsmid.data.SearchResult;
import de.ueller.gpsmid.names.NumberCanon;
import de.ueller.gpsmid.names.SearchNames;
import de.ueller.gpsmid.ui.GuiPoiTypeSelectMenu.PoiTypeSelectMenuItem;
//#if polish.api.bigsearch
//#if polish.api.osm-editing
import de.ueller.gpsmid.ui.GuiOsmPoiDisplay;
import de.ueller.gpsmid.ui.GuiOsmWayDisplay;
//#endif
//#endif
import de.ueller.midlet.iconmenu.LayoutElement;
import de.ueller.midlet.ui.InputListener;
import de.ueller.midlet.ui.KeySelectMenuItem;
import de.ueller.util.CancelMonitorInterface;
import de.ueller.util.HelperRoutines;
import de.ueller.util.Logger;
import de.ueller.util.MoreMath;
import de.ueller.util.ProjMath;

import de.enough.polish.util.Locale;

public class GuiSearch extends Canvas implements CommandListener,
		      GpsMidDisplayable, InputListener, KeySelectMenuReducedListener, CancelMonitorInterface, SaveButtonListener  {

	protected static final int VIRTUALKEY_PRESSED = 1;

	private final static Logger logger = Logger.getInstance(GuiSearch.class,Logger.DEBUG);

	/** OK_CMD for Nearest POI / Fulltext Search */
	private final Command OK_CMD = new Command(Locale.get("generic.OK")/*Ok*/, Command.OK, 1);
	/** OK1_CMD is used when GUI is not optimised for routing */
	private final Command OK1_CMD = new Command(Locale.get("generic.OK")/*Ok*/, Command.OK, 1);
	/** ROUTE2_CMD is used when GUI is not optimised for routing */
	private final Command ROUTE2_CMD = new Command(Locale.get("guisearch.Route1")/*Route*/, Command.ITEM, 3);
	/** ROUTE1_CMD is used when GUI is optimised for routing */
	private final Command ROUTE1_CMD = new Command(Locale.get("guisearch.Route1")/*Route*/, Command.OK, 1);
	/** OK2_CMD is used when GUI is optimised for routing */
	private final Command OK2_CMD = new Command(Locale.get("guisearch.Asdest")/*As destination*/, Command.ITEM, 3);
	private final Command DISP_CMD = new Command(Locale.get("guisearch.Disp")/*Display*/, Command.ITEM, 2);
	private final Command DEL_CMD = new Command(Locale.get("generic.Delete")/*Delete*/, Command.ITEM, 4);
	private final Command CLEAR_CMD = new Command(Locale.get("guisearch.Clear")/*Clear*/, Command.ITEM, 5);
	private final Command BOOKMARK_CMD = new Command(Locale.get("guisearch.Bookmark")/*Add to way points*/, Command.ITEM, 6);
	private final Command FAVORITE_CMD = new Command(Locale.get("guisearch.Favorite")/*Add to favorites*/, Command.ITEM, 6);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 7);
	private final Command OVERVIEW_MAP_CMD = new Command(Locale.get("guisearch.OverviewMap")/*Overview/Filter map*/, Command.ITEM, 8);
	private final Command POI_CMD = new Command(Locale.get("guisearch.NearestPoi")/*Nearest POI*/, Command.ITEM, 9);
	public final Command POI_URL_SEARCH_CMD = new Command(Locale.get("guiwebinfo.Websites")/*Nearby POIs with websites*/, Command.ITEM, 9);
	private final Command POI_PHONE_SEARCH_CMD = new Command(Locale.get("guiwebinfo.Phones")/*Nearby POIs with phone numbers*/, Command.ITEM, 9);
	private final Command SORT_CMD = new Command(Locale.get("guisearch.Sort")/*Toggle sort order (exper.)*/, Command.ITEM, 10);
	private final Command FULLT_CMD = new Command(Locale.get("guisearch.Fulltext")/*Fulltext search*/, Command.ITEM, 10);
	private final Command URL_CMD = new Command(Locale.get("guisearch.OpenURL")/*Open URL*/, Command.ITEM, 11);
	private final Command PHONE_CMD = new Command(Locale.get("guisearch.Phone")/*Call Phone*/, Command.ITEM, 12);
	//#if polish.api.bigsearch
	//#if polish.api.osm-editing
	private final Command EDIT_CMD = new Command(Locale.get("guisearch.Edit")/*Edit OSM data*/, Command.ITEM, 13);
	private final Command EDIT1_CMD = new Command(Locale.get("guisearch.Edit")/*Edit OSM data*/, Command.OK, 1);
	//#endif
	//#endif

	private final Image waypointIcon = Image.createImage("/waypoint.png");

	private final Trace parent;

	private final Vector result = new Vector();
	
	// this array is used to get a copy of the waypoints for the favorites
	public PositionMark[] wayPts = null;
	public boolean showAllWayPts = false;
	
	public static GuiSearchLayout gsl = null;

	/**
	 * This vector is used to buffer writes,
	 * so that we only have to synchronize threads
	 * at the end of painting
	 */
	private final Vector result2 = new Vector();

	private int carret=0;

	private int cursor=0;
	
	private volatile long resultAtCursor=0;
	
	private int scrollOffset = 0;

	private volatile static long lastPaintTime = 0;

	private volatile int defaultAction = ACTION_DEFAULT;
	private volatile boolean poisSearched = false;

	private final StringBuffer searchCanon = new StringBuffer();

	private boolean searchAlpha = false;

	private SearchNames searchThread;

	//private boolean abortPaint = false;
	private volatile boolean needsPainting;
	
	public int displayReductionLevel = 0;
	
	private volatile TimerTask timerT;
	private volatile TimerTask housenumberTimerTask = null;
	private volatile Timer timer;
	
	private boolean hideKeypad = false;
	private boolean cursorKeypad = false;

	private int width = 0;
	private	int height = 0;

	private ChoiceGroup poiSelectionCG;
	private TextField poiSelectionMaxDistance;
	private TextField fulltextSearchField;
	//#if polish.android
	private ViewItem poiSelectField;
	private ViewItem OKField;
	//#endif
	
	
	public volatile byte state;
	
	public volatile int filter;
	public final static byte FILTER_BIT_URLS = 1;
	public final static byte FILTER_BIT_PHONES = 2;
	
	public final static byte ACTION_DEFAULT = 0;
	public final static byte ACTION_EDIT_ENTITY = 1;
	public final static byte ACTION_NEARBY_POI = 2;
	
	public final static byte STATE_MAIN = 0;
	public final static byte STATE_POI = 1;
	public final static byte STATE_FULLTEXT = 2;
	public final static byte STATE_FAVORITES = 3;
	public final static byte STATE_SEARCH_PROGRESS = 4;
	public final static byte STATE_POI_URLS = 5;
	public final static byte STATE_POI_PHONES = 6;
	
	private volatile int fontSize;
	
	private int minX;
	private int maxX;
	private int minY;
	private int maxY;
	private int renderDiff;

	private volatile boolean isSearchCanceled;
	
	/**
	 * Record the time at which a pointer press was recorded to determine
	 * a double click
	 */
	private long pressedPointerTime;
	/**
	 * Stores if there was already a click that might be the first click in a double click
	 */
	private boolean potentialDoubleClick;
	/**
	 * Indicates that there was a drag event since the last pointerPressed
	 */
	private boolean pointerDragged;
	/**
	 * Indicates that there was a rather far drag event since the last pointerPressed
	 */
	private static volatile boolean pointerDraggedMuch = false;
	/**
	 * Stores the position of the X coordinate at which the pointer started dragging since the last update 
	 */
	private int pointerXDragged;
	/**
	 * Stores the position of the Y coordinate at which the pointer started dragging since the last update 
	 */
	private int pointerYDragged;
	/**
	 * Stores the position of the initial pointerPress to identify dragging
	 */
	private int pointerXPressed;
	private int pointerYPressed;
	private int clickIdxAtSlideStart;
	/**
	 * indicates if the next release event is valid or the corresponding pointer pressing has already been handled
	 */
	private volatile boolean pointerActionDone;
	
	/** timer checking for long tap */
	private volatile TimerTask tapAutoReleaseTimerTask = null;
	private final int TAP_AUTORELEASE_DELAY = 500;

	/** result ticker */
	private volatile int ticker = -4;
	private volatile int tickerDiff = 0;
	private volatile int tickerMax = 0;
	private volatile boolean needsTicker = false;

	private KeySelectMenu poiTypeForm;

	//#if polish.api.bigsearch
	private Hashtable matchSources = null;
	private Hashtable matchLats = null;
	private Hashtable matchLons = null;
	private Hashtable matchIdx = null;
	//#endif

	private boolean spacePressed = false;
	public volatile String words = "";

	public GuiSearch(Trace parent, int action) throws Exception {
		super();
		this.defaultAction = action;
		this.parent = parent;
		setCommandListener(this);
		
		this.minX = 0;
		this.minY = 0;

		if (parent.isShowingSplitSearch()) {
			this.maxX = parent.getWidth();
			this.maxY = parent.getHeight();
			this.renderDiff = parent.getHeight() / 2;
		} else {
			this.maxX = getWidth(); 
			this.maxY = getHeight(); 
		}

		//#if polish.android
		AndroidDisplay ad = AndroidDisplay.getDisplay(GpsMid.getInstance());
		ad.setOnKeyListener(new OnKeyListener()
		{
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (event.getAction() == KeyEvent.ACTION_DOWN)
				{
					//check if the right key was pressed
					if (keyCode == KeyEvent.KEYCODE_BACK)
					{
						backPressed();
						return true;
					}
				}
				return false;
			}
		});

		//#endif
		searchThread = new SearchNames(this);
		if (defaultAction == ACTION_EDIT_ENTITY) {
			//#if polish.api.bigsearch
			//#if polish.api.osm-editing
			addCommand(EDIT1_CMD);			
			//#endif
			//#endif
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
				addCommand(ROUTE2_CMD);			
			} else {
				addCommand(OK2_CMD);
			}
		} else if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
			addCommand(ROUTE1_CMD);			
			addCommand(OK2_CMD);
		} else {
			addCommand(OK1_CMD);
			addCommand(ROUTE2_CMD);			
		}
		addCommand(DISP_CMD);
		addCommand(DEL_CMD);
		addCommand(CLEAR_CMD);
		addCommand(BOOKMARK_CMD);
		addCommand(FAVORITE_CMD);
		addCommand(BACK_CMD);
		addCommand(OVERVIEW_MAP_CMD);
		addCommand(POI_CMD);
                if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE)) {
			addCommand(POI_URL_SEARCH_CMD);
		}
                if (Legend.enablePhoneTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE)) {
			addCommand(POI_PHONE_SEARCH_CMD);
		}
		addCommand(FULLT_CMD);
		addCommand(SORT_CMD);
		if (Legend.enableUrlTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_WEBSITE)) {
			addCommand(URL_CMD);
		}
		if (Legend.enablePhoneTags && Configuration.getCfgBitSavedState(Configuration.CFGBIT_ONLINE_PHONE)) {
			addCommand(PHONE_CMD);
		}

		//#if polish.api.bigsearch
		//#if polish.api.osm-editing
		if (Legend.enableEdits) {
			if (defaultAction != ACTION_EDIT_ENTITY) {
				addCommand(EDIT_CMD);
			}
		}
		//#endif
		//#endif
		
		timerT = new TimerTask() {
			public void run() {
				tickerTick();
				if (needsPainting) {
					doRepaint();
				}
			}			
		};
		
		needsPainting = false;
		try {
			GpsMid.getTimer().schedule(timerT, 250, 250);
		} catch (Exception e) {
			logger.exception("Failed to initialize GuiSearch repaint timer", e);
		}
		
		reSearch();

		//#debug
		logger.debug("GuiSearch initialisied");
		
	}

	public int searchCanonLength() {
		return searchCanon.length();
	}

	private boolean isCursorValid() {
		return (cursor < result.size() && cursor >= 0 && result.size() != 0);
	}
	
	public void commandAction(Command c, Displayable d) {
//		System.out.println("got Command " + c);
		if (state == STATE_MAIN || state == STATE_FAVORITES) {
			if (c == URL_CMD || c == PHONE_CMD) {
				if (!isCursorValid()) {
					return;
				}
				//System.out.println("Trying to handle url or phone cmd");
				SearchResult sr = (SearchResult) result.elementAt(cursor);
				String url = null;
				String phone = null;
				if (c == PHONE_CMD) {
					if (sr.phoneIdx != -1) {
						phone = parent.getUrl(sr.phoneIdx);
						if (phone != null) {
							url = "tel:" + phone;
						}
					}
				}
				if (c == URL_CMD) {
					//System.out.println("Trying to fetch url");
					if (sr.urlIdx != -1) {
						//System.out.println("Got url " + url);
						url = parent.getUrl(sr.urlIdx);
					}
				}
				try {
					if (url != null) {
						//#if polish.api.online
						if (parent.internetAccessAllowed()) {
							GpsMid.getInstance().platformRequest(url);
						}
						//#else
						GpsMid.getInstance().alert (Locale.get("guisearch.OpenUrlTitle"),
							      Locale.get("guisearch.OpenUrl") +  " " + url, Alert.FOREVER);
						//#endif
					}
				} catch (Exception e) {
					//mLogger.exception("Could not open url " + url, e);
				}
			}
			//#if polish.api.bigsearch
			//#if polish.api.osm-editing
			if (c == EDIT_CMD || c == EDIT1_CMD) {
				if (!isCursorValid()) {
					return;
				}
				editOSM((SearchResult) result.elementAt(cursor));
			}
			//#endif
			//#endif
			if (c == OK1_CMD || c == OK2_CMD || c == ROUTE1_CMD || c == ROUTE2_CMD) {			
				if (!isCursorValid()) {
					return;
				}
				//#if polish.api.bigsearch
				//#if polish.api.osm-editing
				if (defaultAction == ACTION_EDIT_ENTITY) {
					editOSM((SearchResult) result.elementAt(cursor));
					return;
				}
				//#endif
				//#endif
				SearchResult sr = (SearchResult) result.elementAt(cursor);
				//			System.out.println("select " + sr);
				RoutePositionMark positionMark = new RoutePositionMark(sr.lat,sr.lon);
				//#if polish.api.bigsearch
				if (state == STATE_FAVORITES && sr.source == SearchNames.INDEX_WAYPOINTS) {
					positionMark.displayName = wayPts[sr.nameIdx].displayName;
				} else {
					positionMark.nameIdx=sr.nameIdx;
					positionMark.displayName=nameForResult(sr);
				}
				//#else
				if (state == STATE_FAVORITES) {
					positionMark.displayName = wayPts[sr.nameIdx].displayName;
				} else {
					positionMark.nameIdx=sr.nameIdx;
					positionMark.displayName=nameForResult(sr);
				}
				//#endif
				parent.setDestination(positionMark);
				//#debug info
				logger.info("Search selected: " + positionMark);
				if (!parent.isShowingSplitSearch()) {
					parent.show();				
					destroy();
				}
				if (c == ROUTE1_CMD || c == ROUTE2_CMD) {
					parent.performIconAction(Trace.ROUTING_START_WITH_OPTIONAL_MODE_SELECT_CMD, null);
				}
				return;
			}
			if (c == DISP_CMD) {			
				if (!isCursorValid()) {
					return;
				}
				SearchResult sr = (SearchResult) result.elementAt(cursor);				
				parent.receivePosition(sr.lat, sr.lon, Configuration.getRealBaseScale());				
				if (!parent.isShowingSplitSearch()) {
					parent.show();				
					destroy();
				}
				return;
			}
			if (c == BACK_CMD) {
				destroy();
				parent.show();
				return;
			}
		} else if (state == STATE_FULLTEXT) {
			if (c == BACK_CMD) {
				state = STATE_MAIN;
				show();
				return;
			}
			if (c == OK_CMD) {
				clearList();
				setTitle();
				cursorKeypad = true;
				searchCanon.setLength(0);
				final CancelMonitorInterface cmi = this;
				isSearchCanceled = false;
				Thread t = new Thread(new Runnable() {
					public void run() {
						setTitle(Locale.get("guisearch.searchingdots")/*searching...*/);
						show();
						Vector names = parent.fulltextSearch(fulltextSearchField.getString().toLowerCase(), cmi);
						for (int i = 0; i < names.size(); i++) {
							if (cmi.monitorIsCanceled()) {
									break;
							}
							searchCanon.setLength(0);
							String name = (String)names.elementAt(i);
							//#debug debug
							logger.debug("Retrieving entries for " + name);							

							// FIXME: Workaround for the full text search sometimes failing for substrings included at the end of names
							// This change from "[ gpsmid-Patches-3002028 ] Improving full text search" reduces the number of failures
							// see also: http://sourceforge.net/projects/gpsmid/forums/forum/677687/topic/3708460
							// old code: searchThread.appendSearchBlocking(NumberCanon.canonial(name));							
							if (name.length() < 21) {						
								searchThread.appendSearchBlocking(NumberCanon.canonial(name));
							} else {
								searchThread.appendSearchBlocking(NumberCanon.canonial(name.substring(0, 20)));
							}
						}
						setTitle(Locale.get("guisearch.SearchResults")/*Search results:*/);
						state = STATE_MAIN;
						show();
						triggerRepaint();
					}
				}, "fulltextSearch");
				state = STATE_SEARCH_PROGRESS;
				show();
				t.start();
			}
		} else if (state == STATE_SEARCH_PROGRESS) {
			if (c == BACK_CMD) {
				state = STATE_MAIN;
				isSearchCanceled = true;
				show();
				return;
			}
		}
		if (c == DEL_CMD) {
			if (carret > 0){
				searchCanon.deleteCharAt(--carret);
				reSearch();
			}
			return;
		}
		if (c == CLEAR_CMD) {
			result.removeAllElements();
			searchCanon.setLength(0);
			searchAlpha = false;
			hideKeypad = false;
			//matchSources = new Hashtable();
			//matchLats = new Hashtable();
			//matchLons = new Hashtable();
			//#if polish.api.bigsearch
			matchSources = null;
			matchLats = null;
			matchLons = null;
			matchIdx = null;
			//#endif
			words = "";
			spacePressed = false;
			setTitle();
			carret=0;
			doRepaint();
			return;
		}
		if (c == BOOKMARK_CMD || c == FAVORITE_CMD) {
			if (cursor >= result.size()) {
				return;
			}
			SearchResult sr = (SearchResult) result.elementAt(cursor);
			PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
			positionMark.displayName=nameForResult(sr);
			if (c == FAVORITE_CMD) {
				positionMark.displayName += "*";
			}
			parent.gpx.addWayPt(positionMark);
			parent.show();
			return;
		}
		
		if (c == OVERVIEW_MAP_CMD) {
			GuiOverviewElements ovEl = new GuiOverviewElements(parent);
			ovEl.show();
		}
		
		if (c == POI_CMD) {
			state = STATE_POI;
			filter = 0;
			showPoiTypeForm();
		}
		if (c == POI_URL_SEARCH_CMD) {
			filter = 1 << FILTER_BIT_URLS;
			state = STATE_POI_URLS;
			showPoiTypeForm();
		}
		if (c == POI_PHONE_SEARCH_CMD) {
			filter = 1 << FILTER_BIT_PHONES;
			state = STATE_POI_URLS;
			showPoiTypeForm();
		}
		if (c == SORT_CMD) {
			short bit = (state == STATE_FAVORITES) ?
				Configuration.CFGBIT_SEARCH_FAVORITES_BY_DISTANCE
				: Configuration.CFGBIT_SEARCH_MAPDATA_BY_NAME;
			
			Configuration.setCfgBitState(bit,
						     !Configuration.getCfgBitState(bit), false);
			reSearch();
			return;
		}
		if (c == FULLT_CMD) {
			Form fulltextForm = new Form(Locale.get("guisearch.Fulltext")/*Fulltext search*/);
			String match = "";
			String name = null;
			if (isCursorValid()) {
				SearchResult sr = (SearchResult) result.elementAt(cursor);
				if (state == STATE_FAVORITES) {
					name = wayPts[sr.nameIdx].displayName;
				} else {
					name = nameForResult(sr);
				}
				int imatch=searchCanon.length(); 
				if (name != null && name.length()<imatch) { 
					imatch=name.length(); 
				}
				if (name != null) {
					match = name.substring(0,imatch);
				}
			}
			fulltextSearchField = new TextField(Locale.get("guisearch.Find")/*Find: */, 
							    (state == STATE_FAVORITES && name != null ) ?
							    name :
							    (searchAlpha ? searchCanon.toString() : match), 40, TextField.ANY);
			state = STATE_FULLTEXT;
			fulltextForm.append(fulltextSearchField);
			fulltextForm.addCommand(BACK_CMD);
			fulltextForm.addCommand(OK_CMD);
			fulltextForm.setCommandListener(this);
			//#if polish.android
			OKField = new SaveButton(Locale.get("generic.OK"),
						 this, (Displayable) parent,
						 OK_CMD);
			//#style formItem
			fulltextForm.append(OKField);
			Display.getDisplay(GpsMid.getInstance()).setCurrentItem(fulltextSearchField);
			//#endif
			GpsMid.getInstance().show(fulltextForm);			
		}
	}

	private void showPoiTypeForm() {
		try{
			//#if polish.android
			Form poiSelectForm = new Form(Locale.get("guisearch.NearestPoi")/*Nearest POI*/);
			poiSelectField = new PoiTypeMenu(this);
			//#style formItem
			poiSelectForm.append(poiSelectField);
			// FIXME perhaps this should be optional
			//InputMethodManager imm = (InputMethodManager) MidletBridge.instance.getSystemService(Context.INPUT_METHOD_SERVICE);
			//Display.getDisplay(GpsMid.getInstance()).setCurrentItem(poiSelectField);
			//imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
			GpsMid.getInstance().show(poiSelectForm);
			//#else
			poiTypeForm = new GuiPoiTypeSelectMenu(this, this);
			poiTypeForm.show();
			//#endif
		} catch (Exception e) {
			logger.exception(Locale.get("guisearch.FailedToSelectPOIType")/*Failed to select POI type*/, e);
			state = STATE_MAIN;
			show();
		}
	}

	//#if polish.api.bigsearch
	//#if polish.api.osm-editing
	private void editOSM(SearchResult sr) {
		if (Legend.enableEdits) {
			if (!parent.internetAccessAllowed()) {
				return;
			}
			//System.out.println("Trying to retrieve node " + sr.osmID + " lat: " + sr.lat + " lon " + sr.lon);
			if (sr.type < 0) {
				GuiOsmPoiDisplay guiNode = new GuiOsmPoiDisplay((int) sr.osmID, null,
										sr.lat, sr.lon, parent);
				guiNode.show();
				guiNode.refresh();
			} else {
				// FIXME add code for relation & area editing, at least for 
				// tags - probably needs some changes to data structures to pass
				// relation id & area way id from Osm2GpsMid to GpsMid

				// FIXME maybe there's a better method to get a way by sr.osmID?
				// (though strictly we'd only need the id for downloading XML)
				// try with showing the node and loading via RETRIEVE_XML
				// a side effect is that position changes, fix if we don't want that
				//parent.receivePosition(sr.lat, sr.lon, Configuration.getRealBaseScale());				
				//parent.show();				
				//parent.commandAction(Trace.RETRIEVE_XML);
				//EditableWay eway = (EditableWay)pc.actualWay;
				GuiOsmWayDisplay guiWay = new GuiOsmWayDisplay(sr.osmID, parent);
				guiWay.show();
				guiWay.refresh();

			}
		} else {
			logger.error(Locale.get("trace.EditingIsNotEnabled")/*Editing is not enabled in this map*/);
		}
		return;
	}
	//#endif
	//#endif

	private void destroy() {
		if (searchThread != null) {
			searchThread.shutdown();
			searchThread = null;
		}
	}

	public void show() {
		hideKeypad = false;
		gsl = new GuiSearchLayout(0, renderDiff, maxX, maxY);
		potentialDoubleClick = false;
		pointerDragged = false;
		if ((defaultAction == ACTION_EDIT_ENTITY || defaultAction == ACTION_NEARBY_POI) && !poisSearched) {
			System.out.println("Starting POI search");
			state = STATE_POI;
			filter = 0;
			showPoiTypeForm();
			poisSearched = true;
		} else {
		if (state == STATE_SEARCH_PROGRESS) {
			Form f = new Form(Locale.get("guisearch.SearchingdotsForm")/*Searching...*/);
			f.addCommand(BACK_CMD);
			f.setCommandListener(this);
			GpsMid.getInstance().show(f);
		} else {
			GpsMid.getInstance().show(this);
			//Display.getDisplay(parent.getParent()).setCurrent(this);
		}
		doRepaint();
		}
	}

	public void doRepaint() {
		if (parent.isShowingSplitSearch()) {
			parent.repaint();
		} else {
			repaint();
		}
	}

	public void sizeChanged(int w, int h) {
		maxX = w;
		maxY = h;
		if (parent.isShowingSplitSearch()) {
			renderDiff = parent.getHeight() / 2;
		}
		gsl = new GuiSearchLayout(0, renderDiff, w, h);
		doRepaint();
	}

	//#if polish.api.bigsearch
	private void nextWord() {
		words = words + searchCanon.toString() + " ";
		searchCanon.setLength(0);
		carret = 0;
		spacePressed = false;
	}
	//#endif

	public boolean sortByDist() {
		return sortByDist(state);
	}

	public boolean sortByDist(byte state) {
		if (state == STATE_FAVORITES) {
			return Configuration.getCfgBitState(Configuration.CFGBIT_SEARCH_FAVORITES_BY_DISTANCE);
		} else {
			return !Configuration.getCfgBitState(Configuration.CFGBIT_SEARCH_MAPDATA_BY_NAME);
		}
	}

	private void addToResult(SearchResult srNew) {
		if (!sortByDist()) {
			result.addElement(srNew);
		} else {
			SearchResult sr = null;
			int i = 0;
			// FIXME use a binary search or some other more efficient algorithm
			for (i=0; i<result.size(); i++) {
				sr = (SearchResult) result.elementAt(i);
				if (srNew.dist < sr.dist) {
					break;
				}
			}
			result.insertElementAt(srNew, i);
		}
	}

	// return true if name is a match to searchCanon, false otherwise
	private boolean canonMatches(StringBuffer searchCanon, String name) {
		// avoid string index out of bound
		int len = searchCanon.length();
		if (name != null && name.length() < len) {
			len = name.length();
		}
		return (matchMode() ||
		    !searchAlpha || name == null ||
			(hasWordSearch() ?
			 // FIXME this gives some extra matches, as it matches to strings in the middle of the name also
			 // Should instead break the name into words, and then match each of the words.
			 name.toLowerCase().indexOf(searchCanon.toString().toLowerCase()) >= 0 :
			 searchCanon.toString().equalsIgnoreCase(name.substring(0, len))));
	}

	// insert new results from search thread 
	private void insertResults() {
		if (result2.size() > 0) {
			synchronized(this) {				
				for (int i = 0; i < result2.size(); i++ ) {
					SearchResult res = (SearchResult) result2.elementAt(i);
					String name = null;
					//#if polish.api.bigsearch
					Long id = new Long(res.resultid);
					if (state == STATE_FAVORITES && res.source == SearchNames.INDEX_WAYPOINTS) {
						name = wayPts[res.nameIdx].displayName;
					} else {
						name = nameForResult(res);
					}
					//#else
					if (state == STATE_FAVORITES) {
						name = wayPts[res.nameIdx].displayName;
					} else {
						name = nameForResult(res);
					}
					//#endif
					//System.out.println ("MatchMode: " + matchMode());
					//System.out.println ("insertresults name: " + name);
					//System.out.println ("parent.getName: " + parent.getName(res.nameIdx));
					if (canonMatches(searchCanon, name)) {
						//#if polish.api.bigsearch
						// match multiword search or housenumber search
						//System.out.println ("MatchMode: " + matchMode() + " matchSources: " + matchSources);
						if (matchMode() && matchSources != null) {
							if (matchSources.get(id) != null &&
							    (((Integer) matchIdx.get(id)).intValue() != res.nameIdx
							     || hasWordSearch())) {
								res.preMatchIdx = ((Integer) matchIdx.get(id)).intValue();
								addToResult(res);
							}
						} else {
							addToResult(res);
							if (resultAtCursor != 0) {
								int cursorCandidate = findResult(resultAtCursor);
								System.out.println("findResult returned " + cursorCandidate);
								if (cursorCandidate != 0) {
									cursor = cursorCandidate;
									resultAtCursor = 0;
								}
							}
						}
						//#else
						addToResult(res);
						//#endif
					}
				}
				result2.removeAllElements();
			}
		}
	}

	protected void paint(Graphics gc) {
		//#debug debug
		logger.debug("Painting search screen with offset: " + scrollOffset);
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_LARGE_FONT)) {
			Font fontLarge = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);  
			gc.setFont(fontLarge);
		}
		lastPaintTime = System.currentTimeMillis();
		if (fontSize == 0) {
			fontSize = gc.getFont().getHeight();
		}		
		int yc=scrollOffset + renderDiff;
		int reducedName=0;
		gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BACKGROUND]);
		gc.fillRect(0, renderDiff, maxX, maxY);
		if (yc < renderDiff) {
			gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_ARROWS]);
			gc.drawString("^", maxX, renderDiff, Graphics.TOP | Graphics.RIGHT);
		}
		// insert new results from search thread 
		insertResults();

	    needsPainting = false;
	    
	    // keep cursor within bounds
	    if (cursor!=0 && cursor >= result.size()) {
		    cursor = result.size() - 1;
	    }
	    StringBuffer nameb=new StringBuffer();
	    StringBuffer nearNameb=new StringBuffer();

	    for (int i=0;i<result.size();i++){	    	
			if (yc < renderDiff) {
				yc += fontSize;
				continue;
			}
			if (yc > maxY) {
				gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_ARROWS]);
				gc.drawString("v", maxX, maxY - 7,
						Graphics.BOTTOM | Graphics.RIGHT);				
				break;
			}

			if (i == cursor){
				gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
			} else {
				gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_TYPED]);
			}
			SearchResult sr=(SearchResult) result.elementAt(i);
			String flags="";
			if (sr.urlIdx != -1) {
				flags = flags + "W";
			}
			if (sr.phoneIdx != -1) {
				flags = flags + "P";
			}
			if (sr.urlIdx != -1 || sr.phoneIdx != -1) {
				flags = flags + " ";
			}
			Image img = null;
			if (sr.type < 0) {
				img = Legend.getNodeSearchImage((short)(sr.type*-1));
			} else if (sr.type == 0 ) {
				img = waypointIcon;
			} else { // type > 0
				img = Legend.getWayOrAreaSearchImage(sr.type);
			}
			if (img != null) {
				gc.drawImage(img, 8, yc + fontSize / 2 - 1, Graphics.VCENTER | Graphics.HCENTER);
			}
			String name = null;
			//#if polish.api.bigsearch
			if (state != STATE_FAVORITES || sr.source != SearchNames.INDEX_WAYPOINTS) {
			//#else
			if (state != STATE_FAVORITES) {
			//#endif
				name = flags + nameForResult(sr);
			} else {
				if (wayPts.length > sr.nameIdx) {
					name = wayPts[sr.nameIdx].displayName;
				}
			}
			nameb.setLength(0);
			if (name != null){
				if (displayReductionLevel < 1) {
					nameb.append(name);
					reducedName=0;
				} else {
					reducedName=1;
					nameb.append(name.charAt(0));
					nameb.append('.');
				}
			}

			if (sr.nearBy != null){
				for (int ib=sr.nearBy.length; ib-- != 0;){
					nameb.append(" / ");
					nearNameb.setLength(0);
					nearNameb.append(getName(sr.nearBy[ib]));
					if (displayReductionLevel < (sr.nearBy.length - ib + 1)) {
						nameb.append(nearNameb);
						reducedName=0;
					} else {
						reducedName=2;
						nameb.append(nearNameb.charAt(0));
						nameb.append('.');
					}					
				}
			}
			if (displayReductionLevel == 4) {
				// show start of name & distance + compass
				nameb.setLength(0);
				nameb.append(name);
			} else {
				// show last name part unreduced unless reduced to compass
				if(reducedName!=0 && nameb.length()>=2) {
					// only if the result is more than once reduced (for POIs) or the result has a nearby entry
					if (displayReductionLevel > 1 || sr.nearBy != null) {
						nameb.setLength(nameb.length()-2);
						if(reducedName==1) {
							nameb.append(name);
						}
						else {
							nameb.append(nearNameb.toString());
						}
					}
				}
			}
			appendCompassDirection(nameb, sr);
			name=nameb.toString();
			if (i == cursor &&
			    (Configuration.getCfgBitState(Configuration.CFGBIT_TICKER_ISEARCH_ALL)
			     || Configuration.getCfgBitState(Configuration.CFGBIT_TICKER_ISEARCH))) {
				if (nameBiggerThanFits(gc, name)) {
					needsTicker = true;
				} else {
					needsTicker = false;
				}
			}
			int tickerUse = 0;
			if (Configuration.getCfgBitState(Configuration.CFGBIT_TICKER_ISEARCH_ALL)
			    && i != cursor && needsTicker && tickerDiff > 0) {
				tickerUse = tickerDiff;
			}
			if (Configuration.getCfgBitState(Configuration.CFGBIT_TICKER_ISEARCH)
			    && i == cursor && needsTicker && tickerDiff > 0) {
				tickerUse = tickerDiff;
			}
			if (tickerUse >= name.length()) {
				tickerUse = name.length() - 1;
			}
			name = name.substring(tickerUse);
			if (i == cursor && needsTicker && tickerDiff > 0) {
				if (!nameBiggerThanFits(gc, name) && ticker > 0) {
					tickerAtEnd();
				}
			}
			if (name != null) {
				// avoid index out of bounds 
				// FIXME add code to handle word search code and housenumber search result highlighting
				int imatch=searchCanon.length(); 
				if (name.length()<imatch) { 
					imatch=name.length(); 
				} 
				// when display is reduced only 1st char matches 
				if (displayReductionLevel > 0) { 
					imatch=1; 
				} 

				// name part identical to search string 
				if (hasWordSearch() || matchMode()) {
					// FIXME could use improvement, maybe put match in the middle of screen
					if (i == cursor){ 
						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
					} else { 
						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_TYPED]);
					}
					gc.drawString(name, 17, yc, Graphics.TOP | Graphics.LEFT); 
				} else {	
					// name part identical to search string 
					if (i == cursor){ 
						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
					} else { 
						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_TYPED]);
					}
					int len = imatch+flags.length()-tickerUse;
					if (len > name.length()) {
						len = name.length();
					} else if (len < 0) {
						len = 0;
					}
					gc.drawString(name.substring(0,len), 17, yc, Graphics.TOP | Graphics.LEFT); 
					// remaining name part 
					if (i == cursor){ 
						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_REST]);
					} else { 
						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_NONSELECTED_REST]);
					} 
					gc.drawString(name.substring(len), 17 + gc.getFont().stringWidth(name.substring(0,len)) , yc, Graphics.TOP | Graphics.LEFT);
				}
				// carret 
				if(!(hasWordSearch() || matchMode())
				   && carret<=imatch && displayReductionLevel<1) { 
					int len = carret+flags.length()-tickerUse;
					if (len >= 0) {
						int cx=17 + gc.getFont().stringWidth(name.substring(0,len)); 
						gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_SELECTED_TYPED]);
						gc.drawLine(cx-1,yc+fontSize,cx+1,yc+fontSize); 
					}
				}
			} else {
				gc.drawString("..." + sr.nameIdx,17, yc, Graphics.TOP | Graphics.LEFT);
			}
			yc+=fontSize;
		}
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD)) {
			gc.setColor(Legend.COLORS[Legend.COLOR_SEARCH_BUTTON_TEXT]);
			if (hasPointerEvents() && ! hideKeypad) {
				if (gsl == null) {
					gsl = new GuiSearchLayout(0, renderDiff, width, height);
				}
			
				String letters[] = {  Locale.get("guisearch.choose")/*choose*/, "  X  ", "  <- ", 
						      hasWordSearch() ?
						      Locale.get("guisearch.label1wordSearch")/* 1*- */ :
						      Locale.get("guisearch.label1")/*_1*- */,
						      Locale.get("guisearch.label2")/* abc2*/,
						      Locale.get("guisearch.label3")/* def3*/, Locale.get("guisearch.label4")/* ghi4*/,
						      Locale.get("guisearch.label5")/* jkl5*/, Locale.get("guisearch.label6")/* mno6*/,
						      Locale.get("guisearch.label7")/*pqrs7*/, Locale.get("guisearch.label8")/* tuv8*/,
						      Locale.get("guisearch.label9")/*wxyz9*/, 
						      Locale.get("guisearch.fulltextshort")/*fulltext*/, "  0  ", 
						      hasWordSearch() ?
						      Locale.get("guisearch.pound")/*_#end*/ :
						      Locale.get("guisearch.poundNameSearch")/*#end*/};
				if (cursorKeypad) {
					String keypadLetters[] = {  Locale.get("guisearch.abc")/*abc*/, "  X  ", "  <- ", 
						    hasWordSearch() ?
						       Locale.get("guisearch.clabel1wordSearch")/*exit search*/ :
						       Locale.get("guisearch.clabel1")/*exit search*/,
						       Locale.get("guisearch.clabel2")/* abc2*/,
						       Locale.get("guisearch.clabel3")/* def3*/, Locale.get("guisearch.clabel4")/* ghi4*/,
						       Locale.get("guisearch.clabel5")/* jkl5*/, Locale.get("guisearch.clabel6")/* mno6*/,
						       Locale.get("guisearch.clabel7")/*pqrs7*/, Locale.get("guisearch.clabel8")/* tuv8*/,
						       Locale.get("guisearch.sort")/*sort*/, 
						       Locale.get("guisearch.more")/*more*/,
           					    Locale.get("guisearch.clabel0")/*POIs*/,
						    hasWordSearch() ?
						       Locale.get("guisearch.pound")/*_#end*/ :
						       Locale.get("guisearch.poundNameSearch")/*#end*/};
					letters = keypadLetters;
				}
				if (hideKeypad) {
					String hideLetters[] = { " ", "  X  " };
					letters = hideLetters;
				}
				for (int i = 0; i < 15 ; i++) {
					if (!hideKeypad || i == 1) {
						gsl.ele[i].setText(letters[i]);
					}
				}
				gsl.paint(gc);
			}
		}
	}

	protected boolean hasWordSearch() {
		return Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH)
			&& (Legend.enableMap72MapFlags == false || Legend.getLegendMapFlag(Legend.LEGEND_MAPFLAG_WORDSEARCH));
	}

	protected void keyRepeated(int keyCode) {
		//Moving the cursor should work with repeated keys the same
		//as pressing the key multiple times
		int action = this.getGameAction(keyCode);
		// System.out.println("repeat key " + keyCode + " " + action);
		if ((action == UP) || (action == DOWN) ||
			(action == LEFT) || (action == RIGHT) ||
			(keyCode == -8) ) {
			keyPressed(keyCode);
			return;
		}
	}

	protected void keyPressed(int keyCode) {
		int action = getGameAction(keyCode);
		resetTicker();
		/** Ignore gameActions from unicode character keys (space char and above).
		 *  By not treating those keys as game actions they get added to the search canon
		 *  by default if not handled otherwise */
		if (keyCode >= 32) {
			action = 0;
		}
		if (spacePressed) {
			//#if polish.api.bigsearch
			filterMatches();
			nextWord();
			//#endif
		}
		logger.info("Search dialog: got key " + keyCode + " " + action);
		if (keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) {
			/*  KEY_NUM constants match "(char) keyCode" */
			searchCanon.insert(carret++, (char) keyCode);
		//#if polish.api.bigsearch
		} else if (keyCode == KEY_POUND || (keyCode == 32 && hasWordSearch())) {
			// switch to another word, start searching in AND mode
			// collect a list of long entity id's, mark

			// first wait for all results
			// then do a search for housenumbers and whole words

			if (!spacePressed) {
				// first time here, search for whole words

				// FIXME There is probably space for optimization here - I think the blocking search
				// starts a new search going through all of the data, which can result in
				// double the effort. With smarter communication between GuiSearch and the
				// search thread, probably could do this so that the search would continue,
				// and would be done only once. Not sure how the maximum number of search
				// results is used (and how it should be used) here.

				//System.out.println("space pressed, searching whole word index");
				String searchString = NumberCanon.canonial(searchCanon.toString());
				if (hasWordSearch()) {
					searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_WORD);
				} else {
					if (Legend.enableMap66Search) {
						searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_BIGNAME);
					} else {
						searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_NAME);
					}
				}
				//#if polish.api.bigsearch
				// moved with cursor right; match results only for this object
				if (resultAtCursor == 0) {
				//#endif
				searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_HOUSENUMBER);
				//searchThread.appendSearchBlocking(searchString, SearchNames.INDEX_WHOLEWORD);
				// insert new results from search thread 
				insertResults();
				//#if polish.api.bigsearch
				}
				//#endif
				if (matchMode()) {
					//filterMatches();
					//repaint(0, 0, getWidth(), getHeight());
					spacePressed = true;
					return;
				} else {
					//#if polish.api.bigsearch
					storeMatches();
					nextWord();
					//#endif
				}
				if (keyCode == KEY_POUND && state == STATE_FAVORITES) {
					short bit = Configuration.CFGBIT_SEARCH_FAVORITES_BY_DISTANCE;
					Configuration.toggleCfgBitState(bit, false);
					reSearch();
					return;
				}
			}
		//#else
		} else if (keyCode == KEY_POUND && state != STATE_FAVORITES) {
			searchCanon.insert(carret++,'1');				
		//#endif
		} else if (keyCode == KEY_STAR) {
			if (state == STATE_FAVORITES || searchCanon.length() < 2 ) {
				showAllWayPts = !showAllWayPts;
				reSearch();
				return;
			} else {
				displayReductionLevel++;
				if (displayReductionLevel > 4) {
					displayReductionLevel = 0;
				}
				repaint(0, renderDiff, maxX, maxY);
				return;
			}
			// Unicode character 10 is LF
			// so 10 should correspond to Enter key on QWERT keyboards
//#if polish.android
			// the SEARCH doesn't work with KeyEvent.KEYCODE_SEARCH, wonder if J2MEPolish
			// switches keycodes or something. 0 seems to be what is given in practice for the search key
			// in KeyCommandCanvas, but not here
		} else if (keyCode == 10 || action == FIRE || keyCode == 0) {
				commandAction( parent.getCommand(Trace.ROUTING_START_CMD), (Displayable) null);
		} else if (keyCode == 10 || action == FIRE || keyCode == KeyEvent.KEYCODE_BACK) {
			// FIXME With this there's the problem that Back gets passed on to the next menu
			// (e.g. route mode asking). See http://developer.android.com/sdk/android-2.0.html
			// for Native Android workaround; not sure how to do this with J2MEPolish
			if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 10) {
				destroy();
				parent.show();
			} else {
				if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
					commandAction( ROUTE1_CMD, (Displayable) null);
				} else {
					commandAction( OK1_CMD, (Displayable) null);
				}
			}
			return;
//#else
		} else if (keyCode == 10 || action == FIRE) {
			if (Configuration.getCfgBitState(Configuration.CFGBIT_ICONMENUS_ROUTING_OPTIMIZED)) {
				commandAction( ROUTE1_CMD, (Displayable) null);
			} else {
				commandAction( OK1_CMD, (Displayable) null);				
			}
			return;
//#endif
		} else if (action == UP) {
			if (cursor > 0) {
				cursor--;
			}			
			if (cursor * fontSize + scrollOffset < 0) {
				scrollOffset += 3*fontSize;
			}
			if (scrollOffset > 0) {
				scrollOffset = 0;
			}
			repaint(0, renderDiff, maxX, maxY);
			return;
		} else if (action == DOWN) {
			if (cursor < result.size() - 1) {
				cursor++;
			}			
			if (((cursor + 1) * fontSize + scrollOffset + renderDiff) > maxY) {
				scrollOffset -= 3*fontSize;
			}

			if (scrollOffset > 0) {
				scrollOffset = 0;
			}

			repaint(0, renderDiff, maxX, maxY);
			return;
		} else if (action == LEFT) {
			if (carret > 0) {
				carret--;
			} else if (matchMode()) {
				// FIXME in multi-word search should also handle situation with more than two words
			        // by redoing the word searches
				// FIXME this is copied from backspace handling;
				// might want to do a bit different thing
				// for cursor left
				int slen = words.length()-1;
				searchCanon.setLength(0);
				for (int i = 0; i < slen ; i++) {
					searchCanon.insert(carret++, (char) words.charAt(i));
				}
				//#if polish.api.bigsearch
				matchSources = null;
				matchLats = null;
				matchLons = null;
				matchIdx = null;
				//#endif
				//System.out.println("Searchcanon tostring: " + searchCanon.toString());
				//System.out.println("Searchcanon length: " + searchCanon.length());
				words = "";
				carret = slen;
				spacePressed = false;
				result.removeAllElements();
				reSearch();
			}
			repaint(0, renderDiff, maxX, maxY);
			return;
		} else if (action == RIGHT) {
			if (carret < searchCanon.length()) {
				carret++;
			} else {
				if (isCursorValid()) {
					SearchResult sr = (SearchResult) result.elementAt(cursor);
					String name = null;
					if (state == STATE_FAVORITES) {
						name = wayPts[sr.nameIdx].displayName;
					} else {
						name = nameForResult(sr);
					}
					if (carret < name.length()) {
						char nameLetters[] = name.toCharArray();
						if (searchAlpha) {
							searchCanon.insert(carret++,nameLetters[carret-1]);
						} else {
							searchCanon.insert(carret++,NumberCanon.canonial(String.valueOf(nameLetters[carret-1])));
						}
						//#if polish.api.bigsearch
						resultAtCursor = sr.resultid;
						//#endif
					} else if (carret == name.length()) {
						result.removeAllElements();
						result.addElement(sr);
						//#if polish.api.bigsearch
						// only match result under cursor
						//#if polish.api.bigsearch
						resultAtCursor = sr.resultid;
						//#endif
						matchSources = null;
						matchLats = null;
						matchLons = null;
						matchIdx = null;
						//#endif
						keyPressed(KEY_POUND);
						return;
					}
					reSearch();
					return;
				}
			}
			repaint(0, renderDiff, maxX, maxY);
			return;
		} else if (keyCode == -8 || keyCode == 8 || keyCode == 127) { 
			/** Non standard Key -8: hopefully is mapped to
			 * the delete / clear key. According to 
			 * www.j2meforums.com/wiki/index.php/Canvas_Keycodes
			 * most major mobiles that have this key map to -8
			 * 
			 * Unicode Character Key: 8 is backspace so this should be standard
			 * Keycode 127 is Clear-Key passed by MicroEmulator
			 **/
			
			if (carret > 0){
				searchCanon.deleteCharAt(--carret);
			} else if (matchMode()) {
				// FIXME in multi-word search should also handle situation with more than two words
			        // by redoing the word searches
				int slen = words.length()-1;
				searchCanon.setLength(0);
				for (int i = 0; i < slen ; i++) {
					searchCanon.insert(carret++, (char) words.charAt(i));
				}
				//#if polish.api.bigsearch
				matchSources = null;
				matchLats = null;
				matchLons = null;
				matchIdx = null;
				//#endif
				//System.out.println("Searchcanon tostring: " + searchCanon.toString());
				//System.out.println("Searchcanon length: " + searchCanon.length());
				words = "";
				carret = slen;
				spacePressed = false;
				result.removeAllElements();
				reSearch();
			}
		} else if (keyCode == -111) {
			/**
			 * Do not reSearch() after Android MENU key is pressed, otherwise selected result looses focus
			 **/
			return;
		} else {
			// filter out special keys such as shift key (-50), volume keys, camera keys...
			if (keyCode > 0) {
				// this is an alpha key (letter), set flag for post-filtering of results
				searchAlpha = true;
				searchCanon.insert(carret++,(char)keyCode);
			}
		}
		//#if polish.api.bigsearch
		if (housenumberTimerTask != null) {
			housenumberTimerTask.cancel();
			housenumberTimerTask = null;
		}
		if (housenumberTimerTask == null) {
			housenumberTimerTask = new TimerTask() {
				public void run() {
					if (matchMode()) {
						//System.out.println("housenumber timer fired");
						searchThread.appendSearchBlocking(NumberCanon.canonial(searchCanon.toString()),
										  SearchNames.INDEX_HOUSENUMBER);
						insertResults();
						boolean matchall = true;
						int limit = 0;
						if (matchall && searchCanon.toString().length() == 1) {
							limit = 10;
							// uncomment the following two lines to test showing all numbers for streetname
							// FIXME names will be missing however from the list, but will show an idea
							// of the performance
						//
						} else if (matchall && searchCanon.toString().length() == 0) {
							limit = 100;
						}
						for (int i = 0; i < limit; i++) {
							String ss = searchCanon.toString();
							if (limit > 1) {
								ss = ss + Integer.toString(i);
							}
							//System.out.println("search for: " + ss);

							searchThread.appendSearchBlocking(NumberCanon.canonial(ss),
											  SearchNames.INDEX_HOUSENUMBER);
							insertResults();
						}
						//searchThread.appendSearchBlocking(NumberCanon.canonial(ss),
						// SearchNames.INDEX_WHOLEWORD);
						// insert new results from search thread 
					}
				}
			};
		}
		if (matchMode() && housenumberTimerTask != null) {
			try {
				//System.out.println("scheduling housenumber timer");
				GpsMid.getTimer().schedule(housenumberTimerTask, 1500);
			} catch (Exception e) {
				logger.exception("Failed to initialize GuiSearch housenumber search timer", e);
			}
		}
		
		if (searchCanon.length() > 1 || matchMode()) {
			state = STATE_MAIN;
			reSearch();
		}
		if (spacePressed) {
			repaint(0, renderDiff, maxX, maxY);
		} else {
			//System.out.println("zeroing spacePressed");
			reSearch();
		}
		//#else
		if (searchCanon.length() > 1) {
			state = STATE_MAIN;
			reSearch();
		}
		//#endif
	}

	//#if polish.api.bigsearch
	private int findResult(long oldResult) {
		int newcursor = 0;
		for (int i = 0; i < result.size(); i++) {
			if (((SearchResult) result.elementAt(i)).resultid == oldResult) {
				newcursor = i;
			}
		}
		return newcursor;
	}
	//#endif

	private boolean nameBiggerThanFits(Graphics gc, String name) {
		return 17 + gc.getFont().stringWidth(name) > maxX;
	}

	private void tickerTick() {
				ticker++;
				if (ticker >= 0) {
					tickerDiff = ticker;
				} else if (ticker < -4) {
					tickerDiff = tickerMax;
				} else if (ticker < 0) {
					tickerDiff = 0;
				}
				if (needsTicker && isCursorValid() && (System.currentTimeMillis() - lastPaintTime) > 200) {
					needsPainting = true;
				}
				//System.out.println("tickerTick ending: ticker " + ticker + " tickerDiff " + tickerDiff);
	}
	private void resetTicker() {
		// stop a moment at string start (-4..0)
		ticker = -4;
		tickerDiff = 0;
	}
	private void tickerAtEnd() {
		// stop a moment at string end (-8..-5) and string start (-4..0)
		tickerMax = tickerDiff;
		ticker = -8;
	}

	private boolean matchMode() {
		return (!words.equals(""));
	}

	private String nameForResult(SearchResult sr) {
		String name = "";
		//#if polish.api.bigsearch
		if (sr.preMatchIdx != 0 && sr.preMatchIdx != sr.nameIdx) {
			name = getName(sr.preMatchIdx) + " ";
		}
		//#endif
		name += getName(sr.nameIdx);
		return name;
	}

	//#if polish.api.bigsearch
	private void storeMatches() {
		SearchResult sr = null;
		if (matchSources == null) {
			matchSources = new Hashtable();
		}
		if (matchLats == null) {
			matchLats = new Hashtable();
		}
		if (matchLons == null) {
			matchLons = new Hashtable();
		}
		if (matchIdx == null) {
			matchIdx = new Hashtable();
		}

		for (int i = 0; i < result.size(); i++) {
			sr = (SearchResult) result.elementAt(i);
			Long id = new Long(sr.resultid);
			Integer idx = new Integer(sr.nameIdx);
			Float Lat = new Float(sr.lat);
			Float Lon = new Float(sr.lon);
			Integer source = new Integer(sr.source);
			matchSources.put(id, source);
			matchLats.put(id, Lat);
			matchLons.put(id, Lon);
			matchIdx.put(id, idx);
			//System.out.println("Adding result: " + sr.resultid + " sr.source/housenum: " + sr.source + "/" + SearchNames.INDEX_HOUSENUMBER);
			//System.out.println("Store match, adding, source = " + ((Integer) matchSources.get(id)).intValue());
		}
		//showMatchSources();
	}
	// filter matches for possible second space press
	// also transfer node (housenumber) coordinates to ways when necessary
	private void filterMatches() {
		SearchResult sr = null;

		Hashtable matchNewSources = new Hashtable();

		for (int i = 0; i < result.size(); i++) {
			sr = (SearchResult) result.elementAt(i);
			Long id = new Long(sr.resultid);
			// transfer house number coordinates to street
			Integer sourceNew = new Integer(sr.source);
			if (matchSources.get(id) != null &&
			    (((Integer) matchIdx.get(id)).intValue() != sr.nameIdx
			     || hasWordSearch())) {
				//System.out.println("found match from old results, id = "
				//		   + id + "source = "
				//		   + ((Integer) matchSources.get(id)).intValue());
				//if (((Integer) matchSources.get(id)).intValue() == SearchNames.INDEX_HOUSENUMBER && matchLats.get(id) != null) {
				// get more exact coordinates from old match if current match is not from housenumber index
				if (sr.source != SearchNames.INDEX_HOUSENUMBER && matchLats != null && matchLats.get(id) != null) {
					sr.lat = ((Float) matchLats.get(id)).floatValue();
					sr.lon = ((Float) matchLons.get(id)).floatValue();
					sr.preMatchIdx = ((Integer) matchIdx.get(id)).intValue();
					sourceNew = (Integer) matchSources.get(id);
				}
				//	result.removeElementAt(i);
				//	result.addElement(sr);
			}
			if (sr.type < 0 && matchLats.get(id) != null) {
				// if new match is a node, save coordinates
				Float Lat = new Float(sr.lat);
				Float Lon = new Float(sr.lon);
				Integer idx = new Integer(sr.nameIdx);
				matchLats.put(id, Lat);
				matchLons.put(id, Lon);
				matchIdx.put(id, idx);
			}
			matchNewSources.put(id, sourceNew);
		}
		matchSources = matchNewSources;
		//showMatchSources();
 	}
	//private void showMatchSources() {
	//	Enumeration e = matchSources.keys();
	//	while(e.hasMoreElements()) {
	//		Long key = ((Long) e.nextElement());
	//		int value =((Integer) matchSources.get(key)).intValue();
	//		System.out.println ("key: " + key + " value: " + value);
	//	}
	//}
	//#endif

	public void pointerPressed(int x, int y) {
		//#debug debug
		logger.debug("PointerPressed: " + x + "," + y);
		pointerActionDone = false;
		pointerDraggedMuch = false;
		long currTime = System.currentTimeMillis();
		if (potentialDoubleClick) {
			if ((currTime - pressedPointerTime > 400)) {
				potentialDoubleClick = false;
				pressedPointerTime = currTime;
			}
		} else {
			pressedPointerTime = currTime;
		}
		pointerXDragged = x;
		pointerYDragged = y;

		// when the display is pressed again before the auto release,
		// cancel any outstanding auto release timer and perform the corresponding action immediately instead 
		if (tapAutoReleaseTimerTask != null) {
			tapAutoReleaseTimerTask.cancel();
			if (!pointerDraggedMuch) {
				autoPointerRelease(pointerXPressed, pointerYPressed);
			}	
		}

		pointerXPressed = x;
		pointerYPressed = y;

		/** if clicking above or below the search results show a text field to enter the search string */
		if (fontSize == 0) {
			return;
		}
		int clickIdx = (y - renderDiff - scrollOffset)/fontSize;
		if ( (state == STATE_MAIN || state == STATE_FAVORITES)
			&& (clickIdx < 0 || clickIdx >= result.size() || ((clickIdx + 1) * fontSize + scrollOffset) > maxY)
		     && (hideKeypad || !Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD))

		) {
			GuiNameEnter gne = new GuiNameEnter(this, null, Locale.get("guisearch.SearchForNamesStarting")/*Search for names starting with:*/, searchCanon.toString(), 20);
			gne.show();
		} else {
			if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD)
			    && !hideKeypad
			    && gsl.getElementIdAtPointer(x, y) >= 0 && gsl.isAnyActionIdAtPointer(x, y)) {
				int touchedElementId = gsl.getElementIdAtPointer(x, y);
				if (touchedElementId >= 0
				    &&
				    gsl.isAnyActionIdAtPointer(x, y)
					) {
					//System.out.println("setTouchedElement: " + touchedElementId);
					gsl.setTouchedElement((LayoutElement) gsl.elementAt(touchedElementId));
					doRepaint();
				}
			}
			tapAutoReleaseTimerTask = new TimerTask() {
				public void run() {
					tapAutoReleaseTimerTask = null;
					// if no action (e.g. from double tap) is already done
					// and the pointer did not move or if it was pressed on a control and not moved much
					if (!pointerDraggedMuch) {
						if (System.currentTimeMillis() - pressedPointerTime >= TAP_AUTORELEASE_DELAY){
							/* automatically release the pointer as a workaround for S60V5 devices
							 * which start drawing blue circles but give no pointerReleased() event
							 * when holding down the pointer just a few ms with the finger
							 */
							autoPointerRelease(pointerXPressed, pointerYPressed);
						}
					}
				}
			};
			try {
				// set timer to continue check if this is a long tap
				GpsMid.getTimer().schedule(tapAutoReleaseTimerTask, TAP_AUTORELEASE_DELAY);
			} catch (Exception e) {
				logger.error(Locale.get("trace.NoLongTapTimerTask")/*No LongTap TimerTask: */ + e.toString());
			}
		}
		clickIdxAtSlideStart = clickIdx;
	}

	public void autoPointerRelease(int x, int y) {
		if (fontSize == 0) {
			return;
		}
		int clickIdx = (y - renderDiff - scrollOffset)/fontSize;
		long currTime = System.currentTimeMillis();
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD)
		    && !hideKeypad
		    && gsl.getElementIdAtPointer(x, y) >= 0 && gsl.isAnyActionIdAtPointer(x, y)) {
			int touchedElementId = gsl.getElementIdAtPointer(x, y);
			if (touchedElementId >= 0
			    &&
			    gsl.isAnyActionIdAtPointer(x, y)
				) {
				//gsl.setTouchedElement((LayoutElement) gsl.elementAt(touchedElementId));
				//doRepaint();
				//#if polish.android
				// on Android, have long tap trigger the function from
				// the other screen of virtual kb
				if (!pointerActionDone) {
					cursorKeypad = !cursorKeypad;
				}
				//#endif
				if (touchedElementId == GuiSearchLayout.KEY_1) {
					if (cursorKeypad) {
						destroy();
						if (parent.isShowingSplitSearch()) {
							parent.stopShowingSplitScreen();
						} else {
							parent.show();
						}
					} else {
						keyPressed('1');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_2) {
					if (cursorKeypad) {
						//#if polish.android
						keyPressed(19);
						//#else
						keyPressed(getKeyCode(UP));
						//#endif
					} else {
						keyPressed('2');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_3) {
					if (cursorKeypad) {
						commandAction( ROUTE1_CMD, (Displayable) null);
						return;
					} else {
						keyPressed('3');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_4) {
					if (cursorKeypad) {
						//#if polish.android
						keyPressed(21);
						//#else
						keyPressed(getKeyCode(LEFT));
						//#endif
					} else {
						keyPressed('4');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_5) {
					if (cursorKeypad) {
						commandAction( OK1_CMD, (Displayable) null);
					} else {
						keyPressed('5');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_6) {
					if (cursorKeypad) {
						//#if polish.android
						keyPressed(22);
						//#else
						keyPressed(getKeyCode(RIGHT));
						//#endif
					} else {
						keyPressed('6');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_7) {
					if (cursorKeypad) {
						commandAction( DISP_CMD, (Displayable) null);
						return;
					} else {
						keyPressed('7');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_8) {
					if (cursorKeypad) {
						//#if polish.android
						keyPressed(20);
						//#else
						keyPressed(getKeyCode(DOWN));
						//#endif
					} else {
						keyPressed('8');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_9) {
					if (cursorKeypad) {
						short bit = (state == STATE_FAVORITES) ?
							Configuration.CFGBIT_SEARCH_FAVORITES_BY_DISTANCE
							: Configuration.CFGBIT_SEARCH_MAPDATA_BY_NAME;
			
						Configuration.setCfgBitState(bit,
									     !Configuration.getCfgBitState(bit), false);
						reSearch();
					} else {
						keyPressed('9');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_0) {
					if (cursorKeypad) {
						defaultAction = ACTION_NEARBY_POI;
						poisSearched = false;
						state = STATE_POI;
						filter = 0;
						showPoiTypeForm();
						poisSearched = true;
					} else {
						keyPressed('0');
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_STAR) {
					if (cursorKeypad) {
						keyPressed(KEY_STAR);
					} else {
						commandAction(FULLT_CMD, (Displayable) null);
					}
				} else if (touchedElementId == GuiSearchLayout.KEY_POUND) {
					keyPressed(KEY_POUND);
				} else if (touchedElementId == GuiSearchLayout.KEY_BACKSPACE) {
					keyPressed(8);
				} else if (touchedElementId == GuiSearchLayout.KEY_KEYPAD) {
					cursorKeypad = !cursorKeypad;
				} else if (touchedElementId == GuiSearchLayout.KEY_CLOSE) {
					hideKeypad = true;
				} else if (touchedElementId == GuiSearchLayout.KEY_POUND) {
					keyPressed(KEY_POUND);
				}
			}
		
		} else {
			// if touching the right side of the display (150% font height) this equals to the * key 
			if (x > maxX - fontSize * 3 / 2) {
				keyPressed(KEY_STAR);
			} else {
				// else position the cursor
				potentialDoubleClick = true;			
				cursor = clickIdx;
				resetTicker();
			}		
		}
		if (gsl != null) {
		    gsl.clearTouchedElement();
		}
		doRepaint();
	}

	public void pointerReleased(int x, int y) {
		// avoid division by zero when releasing pointer before screen is drawn
		if (fontSize == 0) {
			return;
		}
		long currTime = System.currentTimeMillis();
		int clickIdx = (y - renderDiff - scrollOffset)/fontSize;
		//#debug debug
		logger.debug("PointerReleased: " + x + "," + y);
		pointerActionDone = true;

		//#if polish.android
		if (Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD)
		    && gsl.getElementIdAtPointer(x, y) >= 0 && gsl.isAnyActionIdAtPointer(x, y)) {
			if (tapAutoReleaseTimerTask != null) {
				tapAutoReleaseTimerTask.cancel();
			}
			tapAutoReleaseTimerTask = null;
			autoPointerRelease(x, y);
		}
		//#endif

		// If this could be a double click
		if (potentialDoubleClick
				&&
			(
				// but never in the virtual keypad
				!Configuration.getCfgBitSavedState(Configuration.CFGBIT_SEARCH_TOUCH_NUMBERKEYPAD) || hideKeypad || !gsl.isAnyActionIdAtPointer(x, y)
			)
		) {
			if ((currTime - pressedPointerTime < 1500) && (clickIdx == cursor)) {
				//#debug debug
				logger.debug("PointerDoublePressed");
				keyPressed(10);
				potentialDoubleClick = false;
				return;
			}
		}
		if (pointerDragged) {
			 // Gestures: sliding horizontally with almost no vertical movement
			if ( Math.abs(y - pointerYPressed) < fontSize ) {
				int xDist = x - pointerXPressed; 
				logger.debug("Slide right " + xDist);
				// Sort mode Slide: Sliding right at least half the screen width is the same as the # key
				if (xDist > maxX / 2 ) {
					//#debug debug
					logger.debug("Sort mode slide");
					keyPressed(KEY_POUND);
				// Route Slide: sliding right at least the fontHeight
				} else if (xDist > fontSize ) {
					logger.debug("Route slide");
					cursor = clickIdxAtSlideStart;
					doRepaint();
					commandAction( ROUTE1_CMD, (Displayable) null);					
				// Search field slide: sliding left at least the fontHeight
				} else if (xDist < -maxX/2 ) {
					logger.debug("Search field slide");
					GuiNameEnter gne = new GuiNameEnter(this, null, Locale.get("guisearch.SearchForNamesStarting")/*Search for names starting with:*/, searchCanon.toString(), 20);
					gne.show();
				// Select entry slide: sliding left at least the fontHeight
				} else if (xDist < -fontSize ) {
					logger.debug("Select entry slide");
					cursor = clickIdxAtSlideStart;
					resetTicker();
					doRepaint();
				}
			}
			pointerDragged = false;
			pointerDraggedMuch = false;
			potentialDoubleClick = false;
			return;
		}
		if (gsl != null) {
		    gsl.clearTouchedElement();
		}		
		doRepaint();
	}
	
	public void pointerDragged(int x, int y) {
		//#debug debug
		logger.debug("Pointer dragged: " + x + " " + y);
		if ((Math.abs(x - pointerXPressed) < fontSize / 2) && (Math.abs(y - pointerYPressed) < fontSize / 2)) {
			/**
			 * On some devices, such as PhoneME, every pointerPressed event also causes
			 * a pointerDragged event. We therefore need to filter out those pointerDragged
			 * events that haven't actually moved the pointer.
			 * 
			 * Also for devices like Nokia 5800 we need some not too small threshold,
			 * thus use half of the fontSize as threshold
			 */
			//#debug debug
			logger.debug("No real dragging, as pointer hasn't moved");
			return;

		}
		pointerDragged = true;
		// check if there's been much movement, do this before the slide lock/unlock
		// to avoid a single tap action when not sliding enough
		if (Math.abs(x - pointerXPressed) > 8
				|| 
			Math.abs(y - pointerYPressed) > 8
		) {
			pointerDraggedMuch = true;
			// avoid double tap triggering on fast consecutive drag actions starting at almost the same position
			pressedPointerTime = 0; 
		}
		
		// only scroll if drag wasn't started at the first entry to avoid scrolling it out accidently during slide gestures
		if (pointerYPressed > fontSize) {		
			scrollOffset += (y - pointerYDragged);
			
			if (scrollOffset > 0) {
				scrollOffset = 0;
			}
			if (scrollOffset < -1*(result.size() - 2) *fontSize) {
				scrollOffset = -1*(result.size() - 2) *fontSize;
			}
			pointerXDragged = x;
			pointerYDragged = y;
			doRepaint();
		}
	}

	private void reSearch() {
		//System.out.println("reSearch starting, searchThread: " + searchThread + " searchCanon: " + searchCanon.toString());
		if (searchThread != null) {
			//#debug info
			logger.info("researching");
			scrollOffset = 0;
			searchThread.search(NumberCanon.canonial(searchCanon.toString()),
					    hasWordSearch() ? 
					    SearchNames.INDEX_WORD : SearchNames.INDEX_BIGNAME);
			repaint(0, renderDiff, maxX, maxY);
			// title will be set by SearchName.doSearch when we need to determine first if we have favorites
			//#if polish.api.bigsearch
			if (searchCanon.length() > 0 || matchMode()) { 
	 			setTitle();
	 		}
			//#else
			if (searchCanon.length() > 0) { 
				setTitle();
			}
			//#endif
		}
	}

	private void appendCompassDirection(StringBuffer sb, SearchResult sr) {
		if (parent.isShowingSplitSearch()) {
			sr.dist=ProjMath.getDistance(sr.lat, sr.lon, parent.center.radlat, parent.center.radlon);
		}
		if (sr.dist >= 0) {
			int courseToGo;
			courseToGo = (int) (MoreMath.bearing_int(
					parent.center.radlat,
					parent.center.radlon,
					sr.lat,
					sr.lon
			)  * MoreMath.FAC_RADTODEC);
			courseToGo %= 360;
			if (courseToGo < 0) {
				courseToGo += 360;
			}
			sb.append("  (").append(HelperRoutines.formatDistance(sr.dist)).append(" ").append(Configuration.getCompassDirection(courseToGo)).append(")");
		}
	}
	
	public void addDistanceToSearchResult(SearchResult sr) {
		sr.dist=ProjMath.getDistance(sr.lat, sr.lon, parent.center.radlat, parent.center.radlon);
	}

	public void setTitle() {
		StringBuffer sb = new StringBuffer();
		switch (state) {
			case STATE_MAIN:
				//#if polish.api.bigsearch
				if (searchCanon.length() == 0 && !matchMode()) {
					sb.append(Locale.get("guisearch.Searchforname")/*Search for name*/);
				} else {
					sb.append((words + searchCanon.toString() + " " + carret));
				}
				//#else
				if (searchCanon.length() == 0) {
					sb.append(Locale.get("guisearch.Searchforname")/*Search for name*/);
				} else {
					sb.append((searchCanon.toString() + " " + carret));
				}
				//#endif
				if (searchCanon.length() > 0) {
					sb.append(" (" + Locale.get("guisearch.key")/*key*/ + " " + searchCanon.toString() + ")");
				} else {
					if (sortByDist()) {
						sb.append(" (" + Locale.get("guisearch.distance")/*distance*/ + ")");
					} else {
						sb.append(" (" + Locale.get("guisearch.name")/*name*/ + ")");
					}
				}
				break;
			case STATE_FAVORITES:
				if (showAllWayPts) {
					sb.append(Locale.get("guisearch.Waypoints")/*Waypoints*/);
				} else {
					sb.append(Locale.get("guisearch.Favorites")/*Favorites*/);					
				}
				if (searchCanon.length() > 0) {
					sb.append(" (key " + searchCanon.toString() + ")");
				} else {
					if (sortByDist()) {
						sb.append(Locale.get("guisearch.bydistance")/* by distance*/);
					} else {
						sb.append(Locale.get("guisearch.byname")/* by name*/);
					}
				}
				break;
			case STATE_POI:
				sb.append(Locale.get("guisearch.nearestpois")/*Nearest POIs*/); break;			
			case STATE_POI_URLS:
				sb.append(Locale.get("guisearch.nearestpoiswithurls")/*Nearest POIs with URLs*/); break;
			case STATE_POI_PHONES:
				sb.append(Locale.get("guisearch.nearestpoiswithphones")/*Nearest POIs with phone #s*/); break;
		        case STATE_FULLTEXT:
				sb.append(Locale.get("guisearch.fulltextresults")/*Fulltext Results*/); break;			
		}
		setTitle(sb.toString());
	}
	
	private String getName(int idx) {
		String name = parent.getName(idx);
		if (name == null) {			
			needsPainting = true;
		}
		return name;
	}
	
	// TODO: optimize sort-in algorithm, e.g. by bisectioning
	/**
	 * @param srNew
	 * @return if SearchResult srNew was actually added (true = added, false = skipped)
	 */
	public synchronized boolean addResult(SearchResult srNew){		
		//#if polish.api.bigsearch
		if (matchMode() && state!= STATE_FAVORITES) {
			Long id = new Long(srNew.resultid);
			// if match is not from a housenumber index, try to get more exact coords from previous match
			if (srNew.source != SearchNames.INDEX_HOUSENUMBER) {
				// transfer house number coordinates to street
				if (matchLats != null && matchLats.get(id) != null) {
					srNew.lat = ((Float) matchLats.get(id)).floatValue();
					srNew.lon = ((Float) matchLons.get(id)).floatValue();
				}
			}
		}
                //#endif
		addDistanceToSearchResult(srNew);
		String name = null;
		//#if polish.api.bigsearch
		if (state == STATE_FAVORITES && srNew.source == SearchNames.INDEX_WAYPOINTS) {
			name = wayPts[srNew.nameIdx].displayName;
		} else {
			name = nameForResult(srNew);
		}
		//#else
		if (state == STATE_FAVORITES) {
			name = wayPts[srNew.nameIdx].displayName;
		} else {
			name = nameForResult(srNew);
		}
		//#endif
		//#debug debug
		logger.debug(Locale.get("guisearch.matchingnamefound")/*Found matching name: */ + srNew);

		//System.out.println ("addResult: resultid = " + srNew.resultid + " source: " + srNew.source + " name: " + name);
		// FIXME repeating code
		// avoid index out of bounds 
		int len = searchCanon.length();
		//System.out.println ("name: " + name);
		//System.out.println ("parent.getName: " + parent.getName(srNew.nameIdx));
		if (canonMatches(searchCanon, name)) {
			result2.addElement(srNew);
			needsPainting = true;
			return true;
		}
		return false;
	}

	
	// TODO: optimize sort-in algorithm, e.g. by bisectioning
	public synchronized void  insertWptSearchResultSortedByNameOrDist(PositionMark[] wpts, SearchResult srNew) {
		addDistanceToSearchResult(srNew);
		SearchResult sr = null;
		int i = 0;
		for (i=0; i<result2.size(); i++) {
			sr = (SearchResult) result2.elementAt(i);
			if (
				!sortByDist(STATE_FAVORITES) && wpts[srNew.nameIdx].displayName.compareTo(wpts[sr.nameIdx].displayName) < 0
				||				
				sortByDist(STATE_FAVORITES) && srNew.dist < sr.dist
			) {
				break;
			}
		}
		result2.insertElementAt(srNew, i);
	}
	
	public void triggerRepaint(){
		repaint(0, renderDiff, maxX, maxY);
	}

	public synchronized void clearList() {
		result.removeAllElements();
		result2.removeAllElements();
		scrollOffset = 0;
	}

	public void inputCompleted(String strResult) {
		if (strResult != null) {		
			searchCanon.setLength(0);
			searchCanon.append(strResult);
			carret = searchCanon.length();
			if (carret > 1) {
				state = STATE_MAIN;
			}
			reSearch();
		}
		show();
	}

	public void keySelectMenuCancel() {
		state = STATE_MAIN;
		parent.show();
	}

	public void keySelectMenuItemSelected(final short poiType) {
		setTitle();
		cursorKeypad = true;            
		clearList();
		searchCanon.setLength(0);
		searchAlpha = false;
		final CancelMonitorInterface cmi = this;
		isSearchCanceled = false;
		Thread t = new Thread(new Runnable() {
			public void run() {
				int foundEntries = 0;
				int SEARCH_MAX_COUNT = Configuration.getSearchMax();

				try {
					int maxScale = Legend.getNodeMaxScale(poiType);
					// index 0 is all POI types
					Vector res = parent.tiles[Legend.scaleToTile(maxScale)].getNearestPoi(
						(poiType == 0) ? true : false, poiType, 
						parent.center.radlat, parent.center.radlon,
						Configuration.getPoiSearchDistance()*1000.0f, cmi);
					for (int i = 0; i < res.size(); i++) {
						SearchResult sr=(SearchResult) res.elementAt(i);
						boolean match = false;
						// don't filter
						if (filter == 0) {
							match = true;
						}
						// show urls
						if ((filter & (1 << FILTER_BIT_URLS)) == (1 << FILTER_BIT_URLS) && sr.urlIdx != -1) {
							match = true;
						}
						// show phones
						if ((filter & (1 << FILTER_BIT_PHONES)) == (1 << FILTER_BIT_PHONES) && sr.phoneIdx != -1) {
							match = true;
						}
						if (match) {
							foundEntries++;
							if (foundEntries <= SEARCH_MAX_COUNT) {
								addResult(sr);
							} else if (foundEntries == SEARCH_MAX_COUNT + 1) {
									//#debug info
									logger.info("Found SEARCH_MAX_COUNT entries. That's enough, stopping further search");
									if (!Configuration.getCfgBitState(Configuration.CFGBIT_SUPPRESS_SEARCH_WARNING)) {
										GpsMid.getInstance().alert(Locale.get("SearchNames.SearchWarningTitle")/*Search warning*/,
													   Locale.get("SearchNames.SearchWarning")/*Maximum search count exceeded, search interrupted*/, 500);
										break;
									}
							} 
						}
					}
					state = STATE_MAIN;
					//#if polish.android
					// FIXME would be better to use AsyncTask,
					// see http://developer.android.com/resources/articles/painless-threading.html
					MidletBridge.instance.runOnUiThread(
						new Runnable() {
							public void run() {
								if (parent.isShowingSplitSearch()) {
									parent.show();
								} else {
									show();
								}
							}
						});
					//#else
					if (parent.isShowingSplitSearch()) {
						parent.show();
					} else {
						show();
					}
					//#endif
					synchronized(this) {
						try {
							//Wait for the Names to be resolved
							//This is an arbitrary value, but hopefully
							//a reasonable compromise.
							wait(500);
							doRepaint();
						} catch (InterruptedException e) {
							//Nothing to do
						}
					}
				} catch (Exception e) {
					logger.exception(Locale.get("guisearch.NearestPOISearchThreadCrashed")/*Nearest POI search thread crashed */, e);
				} catch (OutOfMemoryError oome) {
					logger.error(Locale.get("guisearch.NearestPOISearchOOM")/*Nearest POI search thread ran out of memory */);
				}
			}
		}, "nearestPOI");
		state = STATE_SEARCH_PROGRESS;
		t.start();
	}
	public void backPressed() {
		parent.show();
	}
	public boolean monitorIsCanceled() {
		return isSearchCanceled;
	}
}
