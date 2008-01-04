package de.ueller.midlet.gps;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;

import de.ueller.midlet.gps.data.Gpx;


public class GuiGpx extends List implements CommandListener,
		GpsMidDisplayable {

	private final static Logger logger=Logger.getInstance(GuiGpx.class,Logger.DEBUG);
	
	private final Command SEND_CMD = new Command("Send", Command.OK, 1);
	private final Command DEL_CMD = new Command("delete", Command.ITEM, 2);	
	private final Command CLEAR_CMD = new Command("clear all", Command.ITEM, 3);	
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);

	private RecordStore database;
	private final Trace parent;
	
	private Vector recordIdxMap;
	
	public GuiGpx(Trace parent) throws Exception {
		super("GPX tracklogs", List.EXCLUSIVE);
		this.parent = parent;
		setCommandListener(this);
		initTracks();
		
		addCommand(SEND_CMD);
		addCommand(DEL_CMD);		
		addCommand(CLEAR_CMD);		
		addCommand(BACK_CMD);		
	}
	
	/**
	 * Read tracks from the GPX recordStore and display the names in the list on screen.
	 */
	private void initTracks() {
		this.deleteAll();
		
		byte [] record = new byte[16000];
		recordIdxMap = new Vector();
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(record)); 
		
		try {
			database = RecordStore.openRecordStore("GPX", false);
			logger.info("GPX database has " +database.getNumRecords() + " entries and a size of " + database.getSize());
			
			RecordEnumeration p = database.enumerateRecords(null, null, false);
			
			logger.info("Enumerating tracks");
			while (p.hasNextElement()) {
				int idx = p.nextRecordId();
				while (database.getRecordSize(idx) > record.length) {
					record = new byte[record.length + 16000];
					dis = new DataInputStream(new ByteArrayInputStream(record));
				}
				database.getRecord(idx, record, 0);
				dis.reset();
				
				String trackName = dis.readUTF();
				int noTrackPoints = dis.readInt();
				logger.trace("Found track " + trackName + " with " + noTrackPoints + "TrkPoints");
				this.append(trackName + " (" + noTrackPoints + ")", null);
				recordIdxMap.addElement(new Integer(idx));
			}
			
		} catch (RecordStoreFullException e) {
			logger.error("Record Store is full, can't load list" + e.getMessage());
		} catch (RecordStoreNotFoundException e) {
			logger.error("Record Store not found, can't load list" + e.getMessage());
		} catch (RecordStoreException e) {
			logger.error("Record Store exception, can't load list" + e.getMessage());
		} catch (IOException e) {
			logger.error("IO exception, can't load list" + e.getMessage());
		}
		
	}

	public void commandAction(Command c, Displayable d) {
		logger.debug("got Command " + c);
		if (c == SEND_CMD) {
			int idx = this.getSelectedIndex();
			byte[] record = null;
			try {
				record = database.getRecord(((Integer)recordIdxMap.elementAt(idx)).intValue());
			} catch (RecordStoreNotOpenException e) {
				e.printStackTrace();
			} catch (InvalidRecordIDException e) {
				e.printStackTrace();
			} catch (RecordStoreException e) {
				e.printStackTrace();
			}
			if (record == null)
				return;
			Gpx.transfer(parent.getConfig().getGpxUrl(), record, this);
			
			//parent.show();
			return;
		}
		if (c == DEL_CMD) {
			int idx = this.getSelectedIndex();
			try {
				database.deleteRecord(((Integer)recordIdxMap.elementAt(idx)).intValue());
			} catch (RecordStoreNotOpenException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidRecordIDException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RecordStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			initTracks();
			return;
		}
		/*if (c == CLEAR_CMD) {
			
			return;
		}*/		
		
		if (c == BACK_CMD) {			
			parent.show();
			return;
		}

	}
	
	public void completedUpload() {
		Alert alert = new Alert("Information");
		alert.setString("Completed GPX upload");
		Display.getDisplay(parent.getParent()).setCurrent(alert);
	}

	public void show() {
		Display.getDisplay(parent.getParent()).setCurrent(this);
	}
}
