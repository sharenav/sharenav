package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
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


import de.ueller.gps.data.SearchResult;
import de.ueller.gps.tools.HelperRoutines;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.tile.SearchNames;

public class GuiSearch extends Canvas implements CommandListener,
		GpsMidDisplayable {

	private final static Logger logger = Logger.getInstance(GuiSearch.class,Logger.DEBUG);

	private final Command OK_CMD = new Command("Ok", Command.OK, 1);
	private final Command DISP_CMD = new Command("Display", Command.ITEM, 1);
	private final Command DEL_CMD = new Command("delete", Command.ITEM, 2);
	private final Command CLEAR_CMD = new Command("clear", Command.ITEM, 3);
	private final Command BOOKMARK_CMD = new Command("add to Waypoint", Command.ITEM, 4);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	private final Command POI_CMD = new Command("Nearest POI", Command.ITEM, 6);

	//private Form form;

	private final Image[] ico = { null, Image.createImage("/city.png"),
			Image.createImage("/city.png"), Image.createImage("/street.png"),
			Image.createImage("/parking.png") };

	private final Trace parent;

	private Vector result = new Vector();
	
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
	
	private int displayReductionLevel = 0;
	
	private TimerTask timerT;
	private Timer timer;
	
	private ChoiceGroup poiSelectionCG;
	private TextField poiSelectionMaxDistance;
	
	
	private byte state;
	
	private final static byte STATE_MAIN = 0;
	private final static byte STATE_POI = 1;
	
	private int fontSize;
	
	

	
	public GuiSearch(Trace parent) throws Exception {
		super();
		this.parent = parent;
		setCommandListener(this);
		
		searchThread = new SearchNames(this);
		setTitle("Search for name");
		addCommand(OK_CMD);
		addCommand(DISP_CMD);
		addCommand(DEL_CMD);
		addCommand(CLEAR_CMD);
		addCommand(BOOKMARK_CMD);
		addCommand(BACK_CMD);
		addCommand(POI_CMD);
		
		timerT = new TimerTask() {
			public void run() {
				repaint();				
			}			
		};
		timer = new Timer();
		
		//#debug
		System.out.println("GuiSearch initialisied");
		
	}

	public void commandAction(Command c, Displayable d) {
//		System.out.println("got Command " + c);
		if (state == STATE_MAIN) {
			if (c == OK_CMD) {			
				SearchResult sr = (SearchResult) result.elementAt(cursor);
				//			System.out.println("select " + sr);
				PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
				positionMark.nameIdx=sr.nameIdx;
				positionMark.displayName=parent.getName(sr.nameIdx);
				parent.setTarget(positionMark);
				parent.show();				
				destroy();
				return;
			}
			if (c == DISP_CMD) {			
				SearchResult sr = (SearchResult) result.elementAt(cursor);				
				parent.receivePosItion(sr.lat, sr.lon, 15000f);				
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
				GpsMid.getInstance().show(new Form("Searching..."));
				clearList();				
				Thread t = new Thread(new Runnable() {
					public void run() {
						try {
							byte poiType = (byte)poiSelectionCG.getSelectedIndex();
							int maxScale = Trace.getInstance().pc.c.getNodeMaxScale(poiType);
							
							Vector res = parent.t[Trace.getInstance().pc.c.scaleToTile(maxScale)].getNearestPoi(poiType, 
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
			SearchResult sr = (SearchResult) result.elementAt(cursor);
			PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
			positionMark.nameIdx=sr.nameIdx;
			positionMark.displayName=parent.getName(sr.nameIdx);
			parent.gpx.addWayPt(positionMark);
			parent.show();
			return;
		}
		
		if (c == POI_CMD) {
			state = STATE_POI;
			Form poiSelectionForm = new Form("POI selection");
			poiSelectionCG = new ChoiceGroup("search for type: ", ChoiceGroup.EXCLUSIVE);
			poiSelectionMaxDistance = new TextField("Maximum search distance", "10.0", 5, TextField.DECIMAL);
			for (byte i = 0; i < parent.pc.c.getMaxType(); i++) {				
				poiSelectionCG.append(parent.pc.c.getNodeTypeDesc(i), parent.pc.c.getNodeSearchImage(i));
			}
			poiSelectionForm.append(poiSelectionMaxDistance);
			poiSelectionForm.append(poiSelectionCG);
			poiSelectionForm.addCommand(BACK_CMD);
			poiSelectionForm.addCommand(OK_CMD);
			poiSelectionForm.setCommandListener(this);
			
			GpsMid.getInstance().show(poiSelectionForm);			
		}

	}

	private void destroy() {
		searchThread.shutdown();
		searchThread=null;
	}

	public void show() {
		GpsMid.getInstance().show(this);
		//Display.getDisplay(parent.getParent()).setCurrent(this);
		repaint();
	}

	protected void paint(Graphics gc) {
		//#debug info
		logger.info("Painting search screen with offset: " + scrollOffset);
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
				img = parent.pc.c.getNodeSearchImage((byte)(sr.type*-1));
			} else {
				if (sr.type < ico.length)
					img = ico[sr.type];
				else {
					logger.error("trying to find image icon for a POI of type: " + sr.type);
					img = null;
				}
			}
			if (img != null)
				gc.drawImage(img, 0, yc, Graphics.TOP | Graphics.LEFT);
			String name=parent.getName(sr.nameIdx);
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
				nameb.setLength(nameb.length()-2);
				if(reducedName==1) {
					nameb.append(name);
				}
				else {
					nameb.append(nearNameb.toString());					
				}
			}
			if (sr.dist >= 0) {
				nameb.append("  (").append(HelperRoutines.formatDistance(sr.dist)).append(")");
			}
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
		System.out.println("got key " + keyCode + " " + action);
		if (keyCode == KEY_NUM1) {
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
		} else if (keyCode == KEY_POUND) {
			searchCanon.insert(carret++,'0');
		} else if (keyCode == KEY_STAR) {
			displayReductionLevel++;
			if (displayReductionLevel > 3)
				displayReductionLevel = 0;
			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == FIRE) {
			SearchResult sr = (SearchResult) result.elementAt(cursor);
//			System.out.println("select " + sr);
			PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
			positionMark.nameIdx=sr.nameIdx;
			positionMark.displayName=parent.getName(sr.nameIdx);
			parent.setTarget(positionMark);
			destroy();
			parent.show();
			repaint(0, 0, getWidth(), getHeight());
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
		} else if (keyCode == -8) { 
			/** Non standard Key: hopefully is mapped to
			 * the delete / clear key. According to 
			 * www.j2meforums.com/wiki/index.php/Canvas_Keycodes
			 * most major mobiles that have this key map to -8 */
			
			if (carret > 0){
				searchCanon.deleteCharAt(--carret);				
			}			
		} else {
			return;
		}
		reSearch();
	}

	private void reSearch() {
		//#debug info
		logger.info("researching");
		scrollOffset = 0;
		setTitle(searchCanon.toString() + " " + carret);
		if (searchCanon.length() >= 2) {
			
//			result.removeAllElements();
			searchThread.search(searchCanon.toString());
		} else {
			clearList();
		}
		repaint(0, 0, getWidth(), getHeight());
	}

	public synchronized void addResult(SearchResult sr){		
		parent.getName(sr.nameIdx);
		//#debug info
		logger.info("Found matching name: " + sr);

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
	public void triggerRepaint(){
		repaint(0, 0, getWidth(), getHeight());
	}

	public synchronized void clearList() {
		result.removeAllElements();
		result2.removeAllElements();
		scrollOffset = 0;
	}
		
}
