package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import de.ueller.gps.data.SearchResult;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.tile.SearchNames;

public class GuiSearch extends Canvas implements CommandListener,
		GpsMidDisplayable {

	private final static Logger logger = Logger.getInstance(GuiSearch.class,Logger.DEBUG);

	private final Command OK_CMD = new Command("Ok", Command.OK, 1);
	private final Command DEL_CMD = new Command("delete", Command.ITEM, 2);
	private final Command CLEAR_CMD = new Command("clear", Command.ITEM, 3);
	private final Command BOOKMARK_CMD = new Command("add to Waypoint", Command.ITEM, 4);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);

	//private Form form;

	private final Image[] ico = { null, Image.createImage("/city.png"),
			Image.createImage("/city.png"), Image.createImage("/street.png"),
			Image.createImage("/parking.png") };

	private final Trace parent;

	private Vector result = new Vector();

	private int carret=0;

	private int cursor=0;
	
	private int scrollOffset = 0;

	private StringBuffer searchCanon = new StringBuffer();

	private SearchNames searchThread;

	private boolean abortPaint = false;
	
	private int displayReductionLevel = 0;

	
	public GuiSearch(Trace parent) throws Exception {
		super();
		this.parent = parent;
		setCommandListener(this);
		
		searchThread = new SearchNames(this);
		setTitle("Search for name");
		addCommand(OK_CMD);
		addCommand(DEL_CMD);
		addCommand(CLEAR_CMD);
		addCommand(BOOKMARK_CMD);
		addCommand(BACK_CMD);
		//#debug
		System.out.println("GuiSearch initialisied");
		
	}

	public void commandAction(Command c, Displayable d) {
//		System.out.println("got Command " + c);
		if (c == OK_CMD) {			
			SearchResult sr = (SearchResult) result.elementAt(cursor);
//			System.out.println("select " + sr);
			PositionMark positionMark = new PositionMark(sr.lat,sr.lon);
			positionMark.nameIdx=sr.nameIdx;
			positionMark.displayName=parent.getName(sr.nameIdx);
			parent.setTarget(positionMark);
			parent.show();
			repaint(0, 0, getWidth(), getHeight());
			destroy();
			return;
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
		if (c == BACK_CMD) {
			destroy();
			parent.show();
			return;
		}

	}

	private void destroy() {
		searchThread.shutdown();
		searchThread=null;
	}

	public void show() {
		Display.getDisplay(parent.getParent()).setCurrent(this);
	}

	protected void paint(Graphics gc) {
		int yc=scrollOffset;
//		gc.setFont(Font.getFont(Font.FACE_MONOSPACE ));
		int carPos = gc.getFont().substringWidth(searchCanon.toString(), 0, carret)+17;
		gc.setColor(255,255, 255);
		gc.fillRect(0, 0, getWidth(), getHeight());
		gc.setColor(0, 0, 0);
		gc.drawLine(carPos, 0, carPos, 15*result.size());
	    if (yc < 0) {
			gc.drawString("^", getWidth(), 0, Graphics.TOP | Graphics.RIGHT);
		}

		synchronized(this) {
	    for (int i=0;i<result.size();i++){
	    	if (abortPaint)
	    		break;
			if (yc < 0) {
				yc += 15;
				continue;
			}
			if (yc > getHeight()) {
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
			gc.drawImage(ico[sr.type], 0, yc, Graphics.TOP | Graphics.LEFT);
			String name=parent.getName(sr.nameIdx);
			StringBuffer nameb=new StringBuffer();
			if (name != null){
				if (displayReductionLevel < 1) {
					nameb.append(name);
				} else {
					nameb.append(name.charAt(0));
					nameb.append('.');
				}
			}
			if (sr.nearBy != null){
				for (int ib=sr.nearBy.length; ib-- != 0;){
					nameb.append(" / ");
					String nearName = parent.getName(sr.nearBy[ib]);
					if (displayReductionLevel < (sr.nearBy.length - ib + 1)) {
						nameb.append(nearName);
					} else {
						nameb.append(nearName.charAt(0));
						nameb.append('.');
					}					
				}
			}
			name=nameb.toString();
			if (name != null)
				gc.drawString(name,17, yc, Graphics.TOP | Graphics.LEFT);
			else 
				gc.drawString("..." + sr.nameIdx,17, yc, Graphics.TOP | Graphics.LEFT);
			yc+=15;
		}
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
			if (cursor >= result.size())
				cursor = result.size() - 1;
			if (cursor * 15 + scrollOffset < 0) {
				scrollOffset += 45;
			}
			if (scrollOffset > 0)
				scrollOffset = 0;
			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == DOWN) {
			if (cursor < result.size())
				cursor++;
			if (cursor >= result.size())
				cursor = result.size() - 1;
			if (((cursor + 1) * 15 + scrollOffset) > getHeight()) {
				scrollOffset -= 45;
			}

			if (scrollOffset > 0)
				scrollOffset = 0;

			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == LEFT) {
			if (carret > 0)
				carret--;
			return;
		} else if (action == RIGHT) {
			if (carret < searchCanon.length())
				carret++;
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
		logger.info("researching");
		scrollOffset = 0;
		setTitle(searchCanon.toString() + " " + carret);
		if (searchCanon.length() >= 2) {
			
//			result.removeAllElements();
			searchThread.search(searchCanon.toString());
		}
		repaint(0, 0, getWidth(), getHeight());
	}

	public void addResult(SearchResult sr){
		parent.getName(sr.nameIdx);
		abortPaint = true;
		synchronized(this) {
			result.addElement(sr);
		}
		abortPaint = false;
		repaint(0, 0, getWidth(), getHeight());
	}
	public void triggerRepaint(){
		repaint(0, 0, getWidth(), getHeight());
	}

	public void clearList() {
		abortPaint = true;
		synchronized (this) {
			result.removeAllElements();
		}
		abortPaint = false;
		
	}
}
