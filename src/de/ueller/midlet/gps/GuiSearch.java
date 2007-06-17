package de.ueller.midlet.gps;

import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextField;

import de.ueller.gps.data.Position;
import de.ueller.gps.data.SearchResult;
import de.ueller.midlet.gps.data.Mercator;
import de.ueller.midlet.gps.data.PositionMark;
import de.ueller.midlet.gps.names.Names;
import de.ueller.midlet.gps.tile.C;
import de.ueller.midlet.gps.tile.SearchNames;

public class GuiSearch extends Canvas implements CommandListener,
		GpsMidDisplayable {


	private final Command OK_CMD = new Command("Ok", Command.OK, 1);
	private final Command DEL_CMD = new Command("delete", Command.ITEM, 2);
	private final Command CLEAR_CMD = new Command("clear", Command.ITEM, 3);
	private final Command BOOKMARK_CMD = new Command("add Bookmark", Command.ITEM, 4);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);

	private Form form;

	private final Image[] ico = { null, Image.createImage("/city.png"),
			Image.createImage("/city.png"), Image.createImage("/street.png"),
			Image.createImage("/parking.png") };

	private final Trace parent;

	private Vector result = new Vector();

	private int carret=0;

	private int cursor=0;

	private StringBuffer searchCanon = new StringBuffer();

	private SearchNames searchThread;


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
		System.out.println("GuiSearch initialisied");
		
	}

	public void commandAction(Command c, Displayable d) {
		System.out.println("got Command " + c);
		if (c == OK_CMD) {
			destroy();
			parent.show();
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
			return;
		}
		if (c == OK_CMD) {
			repaint(0, 0, getWidth(), getHeight());
//			destroy();
//			parent.show();
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
		int yc=0;
//		gc.setFont(Font.getFont(Font.FACE_MONOSPACE ));
		int carPos = gc.getFont().substringWidth(searchCanon.toString(), 0, carret)+17;
		gc.setColor(255,255, 255);
		gc.fillRect(0, 0, getWidth(), getHeight());
		gc.setColor(0, 0, 0);
		gc.drawLine(carPos, 0, carPos, 15*result.size());
		for (int i=0;i<result.size();i++){
			if (i == cursor){
				gc.setColor(255, 0, 0);
			} else {
				gc.setColor(0, 0, 0);
			}
			SearchResult sr=(SearchResult) result.elementAt(i);
			gc.drawImage(ico[sr.type], 0, yc, Graphics.TOP | Graphics.LEFT);
			String name = parent.getName(sr.nameIdx);
			if (name != null)
				gc.drawString(name,17, yc, Graphics.TOP | Graphics.LEFT);
			else 
				gc.drawString("..." + sr.nameIdx,17, yc, Graphics.TOP | Graphics.LEFT);
			yc+=15;
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
			searchCanon.insert(carret++,'0');
		} else if (action == FIRE) {
			SearchResult sr = (SearchResult) result.elementAt(cursor);
			System.out.println("select " + sr);
			//parent.receivePosItion(sr.lat,sr.lon);
			parent.setTarget(new PositionMark(sr.lat,sr.lon));
			parent.show();
			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == UP) {
			if (cursor > 0)
				cursor--;
			if (cursor >= result.size())
				cursor = result.size()-1;
			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == DOWN) {
			if (cursor < result.size())
				cursor++;
			if (cursor >= result.size())
				cursor = result.size()-1;
			repaint(0, 0, getWidth(), getHeight());
			return;
		} else if (action == LEFT) {
			if (carret > 0)
				carret--;
		} else if (action == RIGHT) {
			if (carret < searchCanon.length())
				carret++;
		} else {
			return;
		}
		reSearch();
	}

	private void reSearch() {
		setTitle(searchCanon.toString() + " " + carret);
		if (searchCanon.length() >= 2) {
			
			result.removeAllElements();
			searchThread.search(searchCanon.toString());
		}
		repaint(0, 0, getWidth(), getHeight());
	}

	public void addResult(SearchResult sr){
		result.addElement(sr);
		repaint(0, 0, getWidth(), getHeight());
	}
}
