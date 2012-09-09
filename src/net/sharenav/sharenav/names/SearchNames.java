package net.sharenav.sharenav.names;
/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import net.sharenav.util.Logger;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.data.Legend;
import net.sharenav.sharenav.data.PositionMark;
import net.sharenav.sharenav.data.SearchResult;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.sharenav.ui.GuiSearch;
import net.sharenav.sharenav.ui.Trace;

import de.enough.polish.util.Locale;

public class SearchNames implements Runnable {

	private Thread processorThread;
	private int foundEntries = 0;
	private boolean stopSearch = false;
	private String search;
	private final GuiSearch gui;
	private boolean newSearch = false;
	private boolean appendRes = false;
	private volatile static int indexType;
	private volatile static boolean noWaypointSearch = false;

	private final int SEARCH_MAX_COUNT = Configuration.getSearchMax();

	public static final int INDEX_NAME = 0;
	public static final int INDEX_WORD = 1;
	public static final int INDEX_WHOLEWORD = 2;
	public static final int INDEX_HOUSENUMBER = 3;
	public static final int INDEX_BIGNAME = 4;
	public static final int INDEX_WAYPOINTS = 5;

	public static final int FLAG_NODE = 0x80;
	public static final int FLAG_URL = 0x40;
	public static final int FLAG_PHONE = 0x20;

	protected static final Logger logger = 
		Logger.getInstance(SearchNames.class, Logger.TRACE);

	public SearchNames(GuiSearch gui) {
		super();
		this.gui = gui;
	}

	public void run() {
	    try {
		    //System.out.println ("dosearch: indexType is " + indexType + " newSearch: " + newSearch);
		    while (newSearch) {
			    //System.out.println ("dosearch: indexType is " + indexType);
			    doSearch(search, indexType);
			    // refresh display to give chance to fetch the names
			    for (int i = 8; i != 0; i--) {
					try {
					    // repaint moved out of if(stopSearch), caused results to not be updated
					    // when letters were typed rapidly which triggered frequent reSearch()es
					    gui.triggerRepaint();
					    synchronized (this) {
						    wait(300);
					    }
					    if (stopSearch) {
						    break;
					    }
					} catch (InterruptedException e) {
					}
			    }
			}
	    } catch (OutOfMemoryError oome ) {
		    logger.fatal(Locale.get("searchnames.SearchNamesCrashedOOM")/*SearchNames thread crashed as out of memory: */ + oome.getMessage());
		    oome.printStackTrace();
	    } catch (Exception e) {
		    logger.fatal(Locale.get("searchnames.SearchNamesCrashedWith")/*SearchNames thread crashed unexpectedly with error */ +  e.getMessage());
		    e.printStackTrace();
	    }		
	}
	
	//TODO: explain
	private void doSearch(String search, int iType) throws IOException {
		//System.out.println ("doSearch: string " + search + " iType " + iType);
		//System.out.println ("stopSearch: " + stopSearch + " newSearch: " + newSearch);
		try {
			synchronized(this) {
				stopSearch = false;
				if (newSearch) {
					if (!appendRes) {
						gui.clearList();
					}
					newSearch = false;
				}
			}
			
			if (!noWaypointSearch && search.length() < 2) {
				synchronized (this) {
					//#debug
					logger.info("Collecting waypoints");
					// We want each time a fresh copy of the waypoint array 
					// as they might be loading in the background
					// (you might already go to the Search screen while waypoints are still loading during ShareNav's startup) 
					// Waypoints ending with * are favorites (selectable for the user during "Save waypoint") and shown in GuiSearch
					// before typing more than one char 
					Vector wpt = Trace.getInstance().gpx.listWayPoints();
					gui.wayPts = new PositionMark[wpt.size()];
					wpt.copyInto(gui.wayPts);
					final int canonLen = search.length();
					final String cSearch = NumberCanon.canonial(search);
					boolean inserted = false;
					for (int i = 0; i < gui.wayPts.length; i++ ) {
						if (gui.wayPts[i].displayName.length() > 0) {
							if (gui.showAllWayPts || gui.wayPts[i].displayName.endsWith("*")) {
								if (canonLen == 0
								    ||
								    NumberCanon.canonial(gui.wayPts[i].displayName.substring(0, 1)).equals( cSearch )
									) {
									SearchResult sr = new SearchResult();
									sr.lat = gui.wayPts[i].lat;
									sr.lon = gui.wayPts[i].lon;
									//#if polish.api.bigsearch
									sr.source = INDEX_WAYPOINTS;
									//#endif
									sr.nameIdx = i;
									gui.insertWptSearchResultSortedByNameOrDist(gui.wayPts, sr);
									inserted = true;
								}
							}
						}
					}
					if (!inserted) {
						gui.state = GuiSearch.STATE_MAIN;
						gui.setTitle();
					} else {
						// try to avoid a race with GuiSearch
						// - don't go back to favorites state
						if (gui.searchCanonLength() < 2) {
							gui.state = GuiSearch.STATE_FAVORITES;
						}
						gui.displayReductionLevel = 0;
						gui.setTitle();
						return;
					}
				}
				return;
			}
			
			
			/* moved the return up to end of the last identical test
			 * if (search.length() < 2)
				return;*/
			
			noWaypointSearch = false;
			//#debug
			logger.info("Searching for " + search);
			String fn = search;
			String compare = "";
			if (search.length() >= 2) {
				fn = search.substring(0,2);
				compare = search.substring(2);
			}
			StringBuffer current = new StringBuffer();
//			System.out.println("compare: " + compare);
			
			String fnPrefix = "/search";
			// backwards compatibility - remove after a time period
			if (!Legend.enableMap68Filenames) {
				fnPrefix = "";
			}
			if (iType == INDEX_WORD) {
				fnPrefix += "/w";
			} else if (iType == INDEX_WHOLEWORD) {
				fnPrefix += "/ww";
			} else if (iType == INDEX_HOUSENUMBER) {
				fnPrefix += "/h";
			} else if (iType == INDEX_BIGNAME) {
				fnPrefix += "/n";
			} else if (iType == INDEX_NAME) {
				fnPrefix += "/s";
			}
			String fileName = fnPrefix + fn + ".d";
			//System.out.println("open " + fileName);
			InputStream stream;
			try {
				stream = Configuration.getMapResource(fileName);
			} catch (IOException e) {
				stream = null;				
			}			
			if (stream == null) {
				/**
				 * This presumably means, that the combination of two letters simply
				 * doesn't exist in the map. So just return and do nothing.
				 */
				//System.out.println("Couldn't open bigname index, trying to fall back to name index");

				/*
				 * However, if it's a BIGNAME index which fails
				 * with map format 65, try opening the NAME index as fallback
				 */
				if (iType == INDEX_BIGNAME && !Legend.enableMap66Search) {
					fnPrefix = "/search/s";
					// backwards compatibility - remove after a time period
					if (!Legend.enableMap68Filenames) {
						fnPrefix = "/s";
					}
					fileName = fnPrefix + fn + ".d";
					try {
						stream = Configuration.getMapResource(fileName);
						if (stream == null) {
							return;
						} else {
							iType = INDEX_NAME;
							//System.out.println("Opened name index");
						}
					} catch (IOException e) {
						/**
						 * This presumably means, that the combination of two letters simply
						 * doesn't exist in the map. So just return and do nothing.
						 */
						return;				
					}			
				} else {
					return;
				}
			}

			DataInputStream ds = new DataInputStream(stream);
			
			int pos = 0;
			int type = 0;
			/**
			 * InputStream.available doesn't seem to reliably give a correct value
			 * depending on what type of InputStream we actually get.
			 * Instead we will continue reading until we receive a EOFexpetion.
			 */
			while (true) {
				if (stopSearch) {
					ds.close();					
					return;
				}
				try {					
					type = ds.readByte();
				} catch (EOFException eof) {
					//Normal way of detecting the end of a file
					ds.close();
					return;
				}
				/**
				 * Encoding of delta plus flags in bits:
				 * 10000000 Sign bit
				 * 01100000 long 
				 * 01000000 int
				 * 00100000 short
				 * 00000000 byte
				 * 000xxxxx delta 
				 */				
//				System.out.println("type = " + type);
				int sign = 1;
				if ((type & 0x80) != 0) {
					sign = -1;
				}
				int delta = (type & 0x1f) * sign;				
//				System.out.println("type = " + type);
				int entryType = (type & 0x60);
				if (delta > Byte.MAX_VALUE) {
					delta -= Byte.MAX_VALUE;
				}
				pos += delta;				
				current.setLength(pos);
//				System.out.println("pos=" + pos + "  delta=" + delta);
				long value = 0;
				switch (entryType){
				case 0:
					value = ds.readByte();
//					System.out.println("read byte");
					break;
				case 0x20:
					value = ds.readShort();
//					System.out.println("read short");
					break;
				case 0x40:
					value = ds.readInt();
//					System.out.println("read int");
					break;
				case 0x60:
					value = ds.readLong();
//					System.out.println("read long");
					break;
				}
				current.append("" + value);
//				System.out.println("test " + current);
				int idx = Names.readNameIdx(ds);
				if (!current.toString().startsWith(compare)) {
					idx = -1;
				}
				if (Legend.enableBigStyles) {
					type = ds.readShort();
				} else {
					type = ds.readByte() & 0xff;
				}
								
				while (type != 0) {					
					if (stopSearch) {
						ds.close();						
						return;
					}					
					long id = 0;
					if (iType != INDEX_NAME) {
						id = ds.readLong();
					}
					byte isInCountByte = ds.readByte();
					// strip flags
					byte isInCount = (byte) (isInCountByte & 0x0f);

					// in map 65, node is negative in index, in 66, positive in index but negative in memory
					if (Legend.enableMap66Search && (isInCountByte & FLAG_NODE) == FLAG_NODE) {
						type = (short) -1*type;
					}
					int[] isInArray = null;
					if (isInCount > 0 ) {
						isInArray = new int[isInCount];
						for (int jj = isInCount; jj-- != 0;) {
							isInArray[jj] = Names.readNameIdx(ds);
						}
					}
					float lat = ds.readFloat();
					float lon = ds.readFloat();
					int urlidx = -1;
					int phoneidx = -1;
					int nameidx = Names.readNameIdx(ds);
					if (Legend.enableUrlTags) {
						if (!Legend.enableMap66Search || (isInCountByte & FLAG_URL) == FLAG_URL) {
							urlidx = Urls.readUrlIdx(ds);
						}
					}
					if (Legend.enablePhoneTags) {
						if (!Legend.enableMap66Search || (isInCountByte & FLAG_PHONE) == FLAG_PHONE) {
							phoneidx = Urls.readUrlIdx(ds);
						}
					}
					if (urlidx == 0) {
						urlidx = -1;
					}
					if (phoneidx == 0) {
						phoneidx = -1;
					}

					if (idx != -1 && nameidx != 0) {
						idx = nameidx;
					}

					if (idx != -1) {
						SearchResult sr = new SearchResult();
						sr.nameIdx = idx;
						//#if polish.api.bigsearch
						sr.resultid = id;
						sr.osmID = id;
						sr.source = (byte) iType;
						//#endif
						sr.urlIdx = urlidx;
						sr.phoneIdx = phoneidx;
						sr.type = (short) type;
						sr.lat = lat;
						sr.lon = lon;
						sr.nearBy = isInArray;
						// when there are no nearBys we add the distance so it will be shown by the GUI
						if (isInArray == null) {
							gui.addDistanceToSearchResult(sr);
						}
						// why would this need to be here, esp. the newSearch = false? seems to break searches sometimes
						// due to a race condition, 
						// see bug 3509277 in tracker
						// introduced in a large commit 2d554687f61fb47276f643396e6933b6920bbec3
						// in Feb 2008, 
						// if (newSearch) {
						//	if (!appendRes) {						
						//		gui.clearList();
						//	}
						//	newSearch = false;
						//}
						if (foundEntries < SEARCH_MAX_COUNT && gui.addResult(sr)) {
							foundEntries++;
							if (foundEntries >= SEARCH_MAX_COUNT) {
								//#debug info
								logger.info("Found SEARCH_MAX_COUNT entries. That's enough, stopping further search");
								if (
								    (foundEntries == SEARCH_MAX_COUNT)
								    && !Configuration.getCfgBitState(Configuration.CFGBIT_SUPPRESS_SEARCH_WARNING)) {
									ShareNav.getInstance().alert(Locale.get("SearchNames.SearchWarningTitle")/*Search warning*/,
										     Locale.get("SearchNames.SearchWarning")/*Maximum search count exceeded, search interrupted*/, 500);
								}
								ds.close();
								return;
							}
						}
						//System.out.println("found " + current +"(" + idx + ") type=" + type);
					}
					if (Legend.enableBigStyles) {
						type = ds.readShort();
					} else {
						type = ds.readByte() & 0xff;
					}
				} // while (type != 0)
			} // while (true)
		} catch (NullPointerException e) {
			logger.exception(Locale.get("searchnames.NullPointerInSearchNames")/*Null pointer exception in SearchNames: */, e);			
		}
	}
	
	public void shutdown() {
		stopSearch = true;
	}
	
	public synchronized void appendSearchBlocking(String search) {
 	        //#if polish.api.bigsearch
		if (Configuration.getCfgBitState(Configuration.CFGBIT_WORD_ISEARCH)) {
			appendSearchBlocking(search, INDEX_WORD);
		} else if (Legend.enableMap66Search) {
			appendSearchBlocking(search, INDEX_BIGNAME);
		} else {
			appendSearchBlocking(search, INDEX_NAME);
		}
 	        //#else
		if (Legend.enableMap66Search) {
			appendSearchBlocking(search, INDEX_BIGNAME);
		} else {
			appendSearchBlocking(search, INDEX_NAME);
		}
                //#endif
	}
	/**
	 * search for a canonicalised name and return a list of results through callbacks
	 * This call blocks until the search has finished.
	 * @param search
	 */
	public synchronized void appendSearchBlocking(String search, int iType) {
		logger.info("search for  " + search);
		stopSearch = true;
		newSearch = true;
		appendRes = true;
		foundEntries = 0;
		try {
			noWaypointSearch = true;
			doSearch(search, iType);
		} catch (IOException ioe) {
			//Do nothing
		}
	}

// 	/**
//	 * search for a canonicalised name and return a list of results through callbacks
//	 * @param search
//	 * @param type
//	 */
//	public synchronized void appendSearch(String search, final int type) {
//		logger.info("search for  " + search);
//		stopSearch = true;
//		newSearch = true;
//		appendRes = true;
//		//foundEntries = 0;
//		//System.out.println("appendSearch: " + search +  " " + type);
//		this.search = search;
//		final String searchvar = search;
//		Thread t = new Thread(new Runnable() {
//			public void run() {
//				try {
//					//System.out.println("starting doSearch type=" + type);
//					doSearch(searchvar, type);
//					//System.out.println("done doSearch type=" + type);
//				} catch (IOException ioe) {
//					//Do nothing
//				}
//			}
//		}, "wholeWordSearch");
//		t.start();
//	}

	/**
	 * search for a String and create a new search Thread if necessary
	 * @param search
	 */
	public synchronized void search(String search, int type) {
		//#debug
		logger.info("search for  " + search);
		//System.out.println ("search: string " + search + " set indexType to " + type);
		stopSearch = true;
		newSearch = true;
		appendRes = false;
		foundEntries = 0;
		this.search = search;
		indexType = type;
		//System.out.println ("search: set indexType to " + type);
		if (processorThread == null || !processorThread.isAlive()) {
			processorThread = new Thread(this);
			/* processorThread.setPriority(Thread.MIN_PRIORITY + 1);
			 * has been changed on 17-01-2011 to Thread.NORM_PRIORITY - 1
			 * for faster search results
			 */
			processorThread.setPriority(Thread.NORM_PRIORITY - 1);
			processorThread.start();
			//#debug info
			logger.info("started search thread");
		}		
	}

}
