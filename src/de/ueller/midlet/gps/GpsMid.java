/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package de.ueller.midlet.gps;


import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.location.Location;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.rms.InvalidRecordIDException;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;



public class GpsMid extends MIDlet implements CommandListener{
    /** A menu list instance */
    private static final String[] elements = { "Trace","Setup","About"};

    /** Soft button for exiting GpsMid. */
    private final Command EXIT_CMD = new Command("Exit", Command.EXIT, 2);

    /** Soft button for launching a client or sever. */
    private final Command OK_CMD = new Command("Ok", Command.SCREEN, 1);
    /** Soft button to go back from about screen. */
    private final Command BACK_CMD = new Command("Back", Command.BACK, 1);

    /** A menu list instance */
    private final List menu = new List("GPSMid", Choice.IMPLICIT, elements, null);
//	private boolean	isInit=false;

	private String	btUrl="btspp://000DB5315C50:1;authenticate=false;encrypt=false;master=false";
	private int locationProvider=0;

	private String	root;
//	PrintStream log;
	Logger l;

private Trace trace;


	public GpsMid() {
		System.out.println("Init GpsMid");
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
//    	try {
//			FileConnection fc =(FileConnection)Connector.open("file:///midNav/nav.log" ,Connector.WRITE);
//			log = new PrintStream(fc.openOutputStream());
//		} catch (IOException e) {
//			
//		}
		l=new Logger(this);
		new Splash(this);
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", false);
			byte[] data=database.getRecord(1);
			btUrl=new String(data);
			data=database.getRecord(2);
			locationProvider=Integer.parseInt(new String(data));
			database.closeRecordStore();
		} catch (Exception e) {
			btUrl=null;
		}

	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		System.out.println("destroy GpsMid");
	}

	protected void pauseApp() {
		System.out.println("Pause GpsMid");
		if (trace != null){
			trace.pause();
		}
	// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
		System.out.println("Start GpsMid");
		if (trace == null){
			try {
				trace = new Trace(this,btUrl,root);
			} catch (Exception e) {
				trace=null;
				e.printStackTrace();
			}
			} else {
				trace.resume();
		}
		}

	public void commandAction(Command c, Displayable d) {
        if (c == EXIT_CMD) {
            exit();

            return;
        }
        if (c == BACK_CMD) {
        	show();
        	return;
        }
        switch (menu.getSelectedIndex()) {
            case 0:
//            	if (btUrl == null){
//            		Alert alert = new Alert("Please select Bluetooth device first");
//            		Display.getDisplay(this).setCurrent(alert, menu);
//            		return;
//            	}
            	try {
            		if (trace == null){
            			trace = new Trace(this,btUrl,root);
            		} else {
            			Display.getDisplay(this).setCurrent(trace);
            			trace.resume();
            		}
				} catch (Exception e) {
					e.printStackTrace();
					Alert alert = new Alert("Error:" + e.getMessage());
					Display.getDisplay(this).setCurrent(alert, menu);
            		return;
				} 
                break;
            case 1:
            	new GuiDiscover(this);
            	break;
            case 2:
				new Splash(this);
            	break;
            default:
                System.err.println("Unexpected choice...");

                break;
            }

//            isInit = true;


		
	}

	public void exit() {
		try {
			destroyApp(true);
		} catch (MIDletStateChangeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		notifyDestroyed();
	}
    /** Shows main menu of MIDlet on the screen. */
    void show() {
        Display.getDisplay(this).setCurrent(menu);
    }

	public void setBTUrl(String btUrl) {
		RecordStore	database;
		try {
			database = RecordStore.openRecordStore("Receiver", true);
			byte[] data=btUrl.getBytes();
			if (database.getNumRecords() == 0){
				database.addRecord(data, 0, data.length);
			} else {
				database.setRecord(1, data,0,data.length);
			}
			data=new String(""+locationProvider).getBytes();
			if (database.getNumRecords() == 1){
				database.addRecord(data, 0, data.length);
			} else {
				database.setRecord(2, data,0,data.length);
			}
			database.closeRecordStore();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			l.error("Store BTDevice "+e.getMessage());
		}
		this.btUrl = btUrl;
	}
	public String getBTUrl(){
		return this.btUrl;
	}
	public void setRootFs(String root){
		this.root = root;
	}
	public void log(String msg){
		if (l != null){
//		log.print(msg+"\n");
//        Display.getDisplay(this).getCurrent().setTicker(new Ticker(msg));
		Display.getDisplay(this).getCurrent().setTitle(msg);
        System.out.println(msg);
		}
	}

	public void setLocationProvider(int selectedIndex) {
		locationProvider=selectedIndex;
	}
	public int getLocationProvider(){
		return locationProvider;
	}

}
