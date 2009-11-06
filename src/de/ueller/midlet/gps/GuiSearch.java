package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.SearchResult;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.gpsMid.mapData.WaypointsTile;
import de.ueller.midlet.gps.data.MoreMath;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.data.ProjMath;
import de.ueller.midlet.gps.names.NumberCanon;
import de.ueller.midlet.gps.tile.SearchNames;

public class GuiSearch extends Canvas implements CommandListener,
		GpsMidDisplayable, CompletionListener {

	private final static Logger logger = Logger.getInstance(GuiSearch.class,Logger.DEBUG);

	private final Command OK_CMD = new Command("Ok", Command.OK, 1);
	private final Command DISP_CMD = new Command("Display", Command.ITEM, 1);
	private final Command DEL_CMD = new Command("delete", Command.ITEM, 2);
	private final Command CLEAR_CMD = new Command("clear", Command.ITEM, 3);
	private final Command BOOKMARK_CMD = new Command("add to Waypoint", Command.ITEM, 4);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	private final Command OVERVIEW_MAP_CMD = new Command("Overview/Filter Map", Command.ITEM, 6);
	private final Command POI_CMD = new Command("Nearest POI", Command.ITEM, 7);
	private final Command FULLT_CMD = new Command("Fulltext Search", Command.ITEM, 8);

	//private Form form;

	private final Image[] ico = {Image.createImage("/waypoint.png"), Image.createImage("/city.png"),
			Image.createImage("/city.png"), Image.createImage("/street.png"),
			Image.createImage("/parking.png")};

	private final Trace parent;

	private Vector result = new Vector();
	
	// this array is used to get a copy of the waypoints for the favorites
	public PositionMark[] wayPts = null;
	public boolean showAllWayPts = false;
	public boolean sortByDist = false;
	
	/**
	 * This vector is used to buffer writes,
	 * so that we only have to synchronize threads
	 * at the end of painting
	 */
	private Vector result2 = new Vector();

	private int carret=0;

	private int cursor=0;
	
	private int scrollOffset = 0;

	private StringBuffer searchCanon = new StringBuffer();

	private SearchNames searchThread;

	//private boolean abortPaint = false;
	private boolean needsPainting;
	
	public int displayReductionLevel = 0;
	
	private TimerTask timerT;
	private Timer timer;
	
	private ChoiceGroup poiSelectionCG;
	private TextField poiSelectionMaxDistance;
	private TextField fulltextSearchField;
	
	
	public volatile byte state;
	
	public final static byte STATE_MAIN = 0;
	public final static byte STATE_POI = 1;
	public final static byte STATE_FULLTEXT = 2;
	public final static byte STATE_FAVORITES = 3;
	
	private int fontSize;
	
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
	 * Stores the position of the Y coordinate at which the pointer started dragging since the last update 
	 */
	private int pointerYDragged;
	/**
	 * Stores the position of the initial pointerPress to identify dragging
	 */
	private int pointerXPressed;
	private int pointerYPressed;

	
	public GuiSearch(Trace parent) throws Exception {
		super();
		this.parent = parent;
		setCommandListener(this);
		
		searchThread = new SearchNames(this);
		addCommand(OK_CMD);
		addCommand(DISP_CMD);
		addCommand(DEL_CMD);
		addCommand(CLEAR_CMD);
		addCommand(BOOKMARK_CMD);
		addCommand(BACK_CMD);
		addCommand(OVERVIEW_MAP_CMD);
		addCommand(POI_CMD);
		addCommand(FULLT_CMD);
		
		timerT = new TimerTask() {
			public void run() {
				repaint();				
			}			
		};
		timer = new Timer();
		
		reSearch();

		//#debug
		logger.debug("GuiSearch initialisied");
		
	}

	private boolean isCursorValid() {
		return (cursor < result.size() && cursor >= 0 && result.size() != 0);
	}
	
	public void commandAction(Command c, Displayable d) {
//		System.out.println("got Command " + c);
		if (state == STATE_MAIN || state == STATE_FAVORITES) {
			if (c == OK_CMD) {			
				if (!isCursorValid()) {
					return;
				}
				SearchResult sr = (SearchResult) result.elementAt(cursor);
				//			System.out.println("select " + sr);
				PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
				if (state == STATE_FAVORITES) {
					positionMark.displayName = wayPts[sr.nameIdx].displayName;
				} else {
					positionMark.nameIdx=sr.nameIdx;
					positionMark.displayName=parent.getName(sr.nameIdx);
				}
				parent.setTarget(positionMark);
				parent.show();				
				destroy();
				return;
			}
			if (c == DISP_CMD) {			
				if (!isCursorValid()) {
					return;
				}
				SearchResult sr = (SearchResult) result.elementAt(cursor);				
				parent.receivePosition(sr.lat, sr.lon, 15000f);				
				parent.show();				
				destroy();
				return;
			}
			if (c == BACK_CMD) {
				destroy();
				parent.show();
				return;
			}
		} else if (state == STATE_POI) {
			if (c == OK_CMD) {
				setTitle();
				GpsMid.getInstance().show(new Form("Searching..."));
				clearList();
				searchCanon.setLength(0);
				Thread t = new Thread(new Runnable() {
					public void run() {
						try {
							byte poiType = (byte)poiSelectionCG.getSelectedIndex();
							int maxScale = parent.pc.legend.getNodeMaxScale(poiType);
							
							Vector res = parent.t[parent.pc.legend.scaleToTile(maxScale)].getNearestPoi(poiType, 
									parent.center.radlat, parent.center.radlon, 
									Float.parseFloat(poiSelectionMaxDistance.getString())*1000.0f);						
							for (int i = 0; i < res.size(); i++) {
								addResult((SearchResult)res.elementAt(i));
							}
							show();
							synchronized(this) {
								try {
									//Wait for the Names to be resolved
									//This is an arbitrary value, but hopefully
									//a reasonable compromise.
									wait(500);
									repaint();
								} catch (InterruptedException e) {
									//Nothing to do
								}							
							}
						} catch (Exception e) {
							logger.exception("Nearest POI search thread crashed ", e);
						} catch (OutOfMemoryError oome) {
							logger.error("Nearest POI search thread ran out of memory ");
						}
					}
				}, "nearestPOI");
				t.start();
				state = STATE_MAIN;
			}
			if (c == BACK_CMD) {
				state = STATE_MAIN;
				show();
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
				searchCanon.setLength(0);	
				Thread t = new Thread(new Runnable() {
					public void run() {
						setTitle("searching...");
						show();
						Vector names = parent.fulltextSearch(fulltextSearchField.getString().toLowerCase());
						for (int i = 0; i < names.size(); i++) {
							searchCanon.setLength(0);
							String name = (String)names.elementAt(i);
							//#debug debug
							logger.debug("Retrieving entries for " + name);							
							searchThread.appendSearchBlocking(NumberCanon.canonial(name));
						}
						setTitle("Search results:");
						triggerRepaint();
					}
				}, "fulltextSearch");
				t.start();
				state = STATE_MAIN;				
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
			carret=0;
			repaint();
			return;
		}
		if (c == BOOKMARK_CMD) {
			if (cursor >= result.size()) return;
			SearchResult sr = (SearchResult) result.elementAt(cursor);
			PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
			positionMark.nameIdx=sr.nameIdx;
			positionMark.displayName=parent.getName(sr.nameIdx);
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
			Form poiSelectionForm = new Form("POI selection");
			poiSelectionCG = new ChoiceGroup("search for type: ", ChoiceGroup.EXCLUSIVE);
			poiSelectionMaxDistance = new TextField("Maximum search distance", "10", 5, TextField.DECIMAL);
			for (byte i = 0; i < parent.pc.legend.getMaxType(); i++) {				
				poiSelectionCG.append(parent.pc.legend.getNodeTypeDesc(i), 
						parent.pc.legend.getNodeSearchImage(i));
			}
			poiSelectionForm.append(poiSelectionMaxDistance);
			poiSelectionForm.append(poiSelectionCG);
			poiSelectionForm.addCommand(BACK_CMD);
			poiSelectionForm.addCommand(OK_CMD);
			poiSelectionForm.setCommandListener(this);
			
			GpsMid.getInstance().show(poiSelectionForm);			
		}
		if (c == FULLT_CMD) {
			state = STATE_FULLTEXT;
			Form fulltextForm = new Form("Fulltext search");
			fulltextSearchField = new TextField("Find: ", "", 40, TextField.ANY);
			fulltextForm.append(fulltextSearchField);
			fulltextForm.addCommand(BACK_CMD);
			fulltextForm.addCommand(OK_CMD);
			fulltextForm.setCommandListener(this);
			GpsMid.getInstance().show(fulltextForm);			
		}

	}

	private void destroy() {
		searchThread.shutdown();
		searchThread=null;
	}

	public void show() {
		potentialDoubleClick = false;
		pointerDragged = false;
		GpsMid.getInstance().show(this);
		//Display.getDisplay(parent.getParent()).setCurrent(this);
		repaint();
	}

	protected void paint(Graphics gc) {
		//#debug debug
		logger.debug("Painting search screen with offset: " + scrollOffset);
		if (fontSize == 0)
			fontSize = gc.getFont().getHeight();		
		int yc=scrollOffset;
		int reducedName=0;
		gc.setColor(255,255, 255);
		gc.fillRect(0, 0, getWidth(), getHeight());
		gc.setColor(0, 0, 0);		
	    if (yc < 0) {
			gc.drawString("^", getWidth(), 0, Graphics.TOP | Graphics.RIGHT);
		}

	    // insert new results from search thread 
	    if (result2.size() > 0) {
	    	synchronized(this) {				
	    		for (int i = 0; i < result2.size(); i++ ) {
	    			result.addElement(result2.elementAt(i));
	    		}
	    		result2.removeAllElements();
	    	}
	    }
	    // keep cursor within bounds
		if (cursor!=0 && cursor >= result.size()) {
			cursor = result.size() - 1;
		}
	    needsPainting = false;
		StringBuffer nameb=new StringBuffer();
		StringBuffer nearNameb=new StringBuffer();
	    for (int i=0;i<result.size();i++){	    	
			if (yc < 0) {
				yc += fontSize;
				continue;
			}
			if (yc > getHeight()) {
				gc.setColor(0, 0, 0);
				gc.drawString("v", getWidth(), getHeight() - 7,
						Graphics.BOTTOM | Graphics.RIGHT);				
				return;
			}

			if (i == cursor){
				gc.setColor(255, 0, 0);
			} else {
				gc.setColor(0, 0, 0);
			}
			SearchResult sr=(SearchResult) result.elementAt(i);
			Image img;
			if (sr.type < 0) {
				img = parent.pc.legend.getNodeSearchImage((byte)(sr.type*-1));
			} else {
				if (sr.type < ico.length)
					img = ico[sr.type];
				else {
					logger.error("trying to find image icon for a POI of type: " + sr.type);
					img = null;
				}
			}
			if (img != null)
				gc.drawImage(img, 8, yc + fontSize / 2 - 1, Graphics.VCENTER | Graphics.HCENTER);
			String name = null;
			if (state != STATE_FAVORITES) {
				name = parent.getName(sr.nameIdx);
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
					nearNameb.append(parent.getName(sr.nearBy[ib]));
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
			// always show last name part unreduced
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
			appendCompassDirection(nameb, sr);
			name=nameb.toString();
			if (name != null) {
				// avoid index out of bounds 
				int imatch=searchCanon.length(); 
				if (name.length()<imatch) { 
					imatch=name.length(); 
				} 
				// when display is reduced only 1st char matches 
				if (displayReductionLevel > 0) { 
					imatch=1; 
				} 

				// name part identical to search string 
				if (i == cursor){ 
					gc.setColor(255, 0, 0); 
				} else { 
					gc.setColor(0, 0, 0); 
				}
				gc.drawString(name.substring(0,imatch), 17, yc, Graphics.TOP | Graphics.LEFT); 
				// remaining name part 
				if (i == cursor){ 
					gc.setColor(255, 100, 100); 
				} else { 
					gc.setColor(150, 150, 250); 
				} 
				gc.drawString(name.substring(imatch), 17 + gc.getFont().stringWidth(name.substring(0,imatch)) , yc, Graphics.TOP | Graphics.LEFT);

				// carret 
				if(carret<=imatch && displayReductionLevel<1) { 
					int cx=17 + gc.getFont().stringWidth(name.substring(0,carret)); 
					gc.setColor(255, 0, 0); 
					gc.drawLine(cx-1,yc+fontSize,cx+1,yc+fontSize); 
				}
			}
			else 
				gc.drawString("..." + sr.nameIdx,17, yc, Graphics.TOP | Graphics.LEFT);
			yc+=fontSize;
		}
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
		/** Ignore gameActions from unicode character keys (space char and above).
		 *  By not treating those keys as game actions they get added to the search canon
		 *  by default if not handled otherwise */
		if (keyCode >= 32) {
			action = 0;
		}
		logger.info("Search dialog: got key " + keyCode + " " + action);
/* the commented out code below is redundant because the KEY_NUM constants match "(char) keycode"
 * and any keyCode >= 32 will be inserted as default action
 */
/*		if (keyCode == KEY_NUM1) {
			searchCanon.insert(carret++,'1');
		} else if (keyCode == KEY_NUM2) {
			searchCanon.insert(carret++,'2');
		} else if (keyCode == KEY_NUM3) {
			searchCanon.insert(carret++,'3');
		} else if (keyCode == KEY_NUM4) {
			searchCanon.insert(carret++,'4');
		} else if (keyCode == KEY_NUM5) {
			searchCanon.insert(carret++,'5');
		} else if (keyCode == KEY_NUM6) {
			searchCanon.insert(carret++,'6');
		} else if (keyCode == KEY_NUM7) {
			searchCanon.insert(carret++,'7');
		} else if (keyCode == KEY_NUM8) {
			searchCanon.insert(carret++,'8');
		} else if (keyCode == KEY_NUM9) {
			searchCanon.insert(carret++,'9');
		} else if (keyCode == KEY_NUM0) {
			searchCanon.insert(carret++,'0');
		} else */
		if (keyCode == KEY_POUND) {
			if (state == STATE_FAVORITES) {
				sortByDist = !sortByDist;
				reSearch();
				return;
			} else {
				searchCanon.insert(carret++,'1');				
			}
		} else if (keyCode == KEY_STAR) {
			if (state == STATE_FAVORITES || searchCanon.length() < 2 ) {
				showAllWayPts = !showAllWayPts;
				reSearch();
				return;
			} else {
				displayReductionLevel++;
				if (displayReductionLevel > 3)
					displayReductionLevel = 0;
				repaint(0, 0, getWidth(), getHeight());
				return;
			}
			// Unicode character 10 is LF
			// so 10 should correspond to Enter key on QWERT keyboards
		} else if (keyCode == 10 || action == FIRE) {
			if (!isCursorValid()) {
				return;
			}
			SearchResult sr = (SearchResult) result.elementAt(cursor);
//			System.out.println("select " + sr);
			PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
			positionMark.nameIdx=sr.nameIdx;
			positionMark.displayName=parent.getName(sr.nameIdx);
			parent.setTarget(positionMark);
			//#debug info
			logger.info("Search selected: " + positionMark);
			destroy();
			parent.show();
			return;
		} else if (action == UP) {
			if (cursor > 0)
				cursor--;			
			if (cursor * fontSize + scrollOffset < 0) {
				scrollOffset += 3*fontSize;
			}
			if (scrollOffset > 0)
				scrollOffset = 0;
			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == DOWN) {
			if (cursor < result.size() - 1)
				cursor++;			
			if (((cursor + 1) * fontSize + scrollOffset) > getHeight()) {
				scrollOffset -= 3*fontSize;
			}

			if (scrollOffset > 0)
				scrollOffset = 0;

			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == LEFT) {
			if (carret > 0)
				carret--;
			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == RIGHT) {
			if (carret < searchCanon.length())
				carret++;
			repaint(0, 0, getWidth(), getHeight());
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
			}
		} else {
			// filter out special keys such as shift key (-50), volume keys, camera keys...
			if (keyCode > 0) {
				searchCanon.insert(carret++,(char)keyCode);
			}
		}
		if (searchCanon.length() > 1) {
			state = STATE_MAIN;			
		}
		reSearch();
	}
	
	public void pointerPressed(int x, int y) {
		//#debug debug
		logger.debug("PointerPressed: " + x + "," + y);
		long currTime = System.currentTimeMillis();
		if (potentialDoubleClick) {
			if ((currTime - pressedPointerTime > 400)) {
				potentialDoubleClick = false;
				pressedPointerTime = currTime;
			}
		} else {
			pressedPointerTime = currTime;
		}
		pointerYDragged = y;
		pointerXPressed = x;
		pointerYPressed = y;

		/** if clicking above or below the search results show a text field to enter the search string */
		int clickIdx = (y - scrollOffset)/fontSize;
		if ( (state == STATE_MAIN || state == STATE_FAVORITES)
			&& (clickIdx < 0 || clickIdx >= result.size() || ((clickIdx + 1) * fontSize + scrollOffset) > getHeight())
		) {
			GuiNameEnter gne = new GuiNameEnter(this, null, "Search for names starting with:", searchCanon.toString(), 20);
			gne.show();
		}
	}
	
	public void pointerReleased(int x, int y) {
		// avoid division by zero when releasing pointer before screen is drawn
		if (fontSize == 0) {
			return;
		}
		//#debug debug
		logger.debug("PointerReleased: " + x + "," + y);
		long currTime = System.currentTimeMillis();
		int clickIdx = (y - scrollOffset)/fontSize;
		if (pointerDragged) {
			pointerDragged = false;
			potentialDoubleClick = false;
			return;
		}
		if (potentialDoubleClick) {
			if ((currTime - pressedPointerTime < 400) && (clickIdx == cursor)) {
				//#debug debug
				logger.debug("PointerDoublePressed");
				keyPressed(10);
				potentialDoubleClick = false;
				return;
			}
		}
		potentialDoubleClick = true;
		cursor = clickIdx;
		
		repaint();
	}
	
	public void pointerDragged(int x, int y) {
		//#debug debug
		logger.debug("Pointer dragged: " + x + " " + y);
		if ((Math.abs(x - pointerXPressed) < 3) && (Math.abs(y - pointerYPressed) < 3)) {
			/**
			 * On some devices, such as PhoneME, every pointerPressed event also causes
			 * a pointerDragged event. We therefore need to filter out those pointerDragged
			 * events that haven't actually moved the pointer. Chose threshold of 2 pixels
			 */
			//#debug debug
			logger.debug("No real dragging, as pointer hasn't moved");
			return;
		}
		pointerDragged = true;
		
		scrollOffset += (y - pointerYDragged);
		
		if (scrollOffset > 0) {
			scrollOffset = 0;
		}
		if (scrollOffset < -1*(result.size() - 2) *fontSize) {
			scrollOffset = -1*(result.size() - 2) *fontSize;
		}
		pointerYDragged = y;
		repaint();
	}

	private void reSearch() {
		//#debug info
		logger.info("researching");
		scrollOffset = 0;
		//FIXME: in rare cases there occurs an NPE in the following line
		searchThread.search(NumberCanon.canonial(searchCanon.toString()));
		repaint(0, 0, getWidth(), getHeight());
		// title will be set by SearchName.doSearch when we need to determine first if we have favorites
		if (searchCanon.length() > 0) { 
			setTitle();
		}
	}

	private void appendCompassDirection(StringBuffer sb, SearchResult sr) {
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
				if (searchCanon.length() == 0) {
					sb.append("Search for name");
				} else {
					sb.append((searchCanon.toString() + " " + carret));
				}
				break;
			case STATE_FAVORITES:
				if (showAllWayPts) {
					sb.append("Waypoints");
				} else {
					sb.append("Favorites");					
				}
				if (searchCanon.length() > 0) {
					sb.append(" (key " + searchCanon.toString() + ")");
				} else {
					if (sortByDist) {
						sb.append(" by distance");
					} else {
						sb.append(" by name");
					}
				}
				break;
			case STATE_POI:
				sb.append("Nearest POIs"); break;			
			case STATE_FULLTEXT:
				sb.append("Fulltext Results"); break;			
		}
		setTitle(sb.toString());
	}
	
	public synchronized void addResult(SearchResult sr){		
		parent.getName(sr.nameIdx);
		//#debug debug
		logger.debug("Found matching name: " + sr);

		result2.addElement(sr);
		if (!needsPainting) {
			needsPainting = true;
			try {
				timer.schedule(timerT, 500);
			} catch (IllegalStateException ise) {
				//timer was already scheduled.
				//this doesn't matter
			}
		}
	}

	
	// TODO: optimize sort-in algorithm, e.g. by bisectioning
	public void insertWptSearchResultSortedByNameOrDist(PositionMark[] wpts, SearchResult srNew) {
		addDistanceToSearchResult(srNew);
		SearchResult sr = null;
		int i = 0;
		for (i=0; i<result2.size(); i++) {
			sr = (SearchResult) result2.elementAt(i);
			if (
				!sortByDist && wpts[srNew.nameIdx].displayName.compareTo(wpts[sr.nameIdx].displayName) < 0
				||				
				sortByDist && srNew.dist < sr.dist
			) {
				break;
			}
		}
		result2.insertElementAt(srNew, i);
	}
	
	public void triggerRepaint(){
		repaint(0, 0, getWidth(), getHeight());
	}

	public synchronized void clearList() {
		result.removeAllElements();
		result2.removeAllElements();
		scrollOffset = 0;
	}
	public void actionCompleted(String strResult) {
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
}
