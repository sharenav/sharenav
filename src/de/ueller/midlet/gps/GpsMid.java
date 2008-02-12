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
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import de.ueller.gps.data.Configuration;
import de.ueller.midlet.gps.routing.RouteNodeTools;



public class GpsMid extends MIDlet implements CommandListener{
	/** */
	private static GpsMid instance;
    /** A menu list instance */
    private static final String[] elements = { "Trace","Search","Setup","About","Log"};

    /** Soft button for exiting GpsMid. */
    private final Command EXIT_CMD = new Command("Exit", Command.EXIT, 2);

    /** Soft button for launching a client or sever. */
    private final Command OK_CMD = new Command("Ok", Command.SCREEN, 1);
    /** Soft button to go back from about screen. */
    private final Command BACK_CMD = new Command("Back", Command.BACK, 1);
    /** Soft button to show Debug Log. */
 //   private final Command DEBUG_CMD = new Command("", Command.BACK, 1);
    /** Soft button to go back from about screen. */
    private final Command CLEAR_DEBUG_CMD = new Command("Clear", Command.BACK, 1);

    /** A menu list instance */
    private final List menu = new List("GPSMid", Choice.IMPLICIT, elements, null);
//	private boolean	isInit=false;

    private final List loghist=new List("Log Hist",Choice.IMPLICIT);
	private String	root;
	Configuration config=new Configuration();
//	#debug
	Logger l;

private Trace trace=null;


	public GpsMid() {
		instance = this;
		System.out.println("Init GpsMid");
		menu.addCommand(EXIT_CMD);
		menu.addCommand(OK_CMD);
		menu.setCommandListener(this);
		loghist.addCommand(BACK_CMD);
		loghist.addCommand(CLEAR_DEBUG_CMD);
		loghist.setCommandListener(this);
//		#debug
		l=new Logger(this);
		new Splash(this);
//		RouteNodeTools.initRecordStore();
	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
//		#debug
		System.out.println("destroy GpsMid");
	}

	protected void pauseApp() {
//		#debug
		System.out.println("Pause GpsMid");
		if (trace != null){
			trace.pause();
		}
	// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
//		#debug
		System.out.println("Start GpsMid");
		if (trace == null){
			try {
				trace = new Trace(this,config);
//				trace.show();
			} catch (Exception e) {
				trace=null;
				e.printStackTrace();
			}
			} else {
				trace.resume();
//				trace.show();
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
        if (c == CLEAR_DEBUG_CMD){
        	loghist.deleteAll();
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
            			trace = new Trace(this,config);
            			trace.show();
            		} else {
            			trace.resume();
            			trace.show();
            		}
				} catch (Exception e) {
					e.printStackTrace();
					Alert alert = new Alert("Error:" + e.getMessage());
					Display.getDisplay(this).setCurrent(alert, menu);
            		return;
				} 
                break;
            case 1:
        		try {
					if (trace == null){
						trace = new Trace(this,config);
					}
					GuiSearch search = new GuiSearch(trace);
					search.show();
				} catch (Exception e) {
					e.printStackTrace();
					Alert alert = new Alert("Error:" + e.getMessage());
					Display.getDisplay(this).setCurrent(alert, menu);
				}
            	break;
            case 2:
            	new GuiDiscover(this);
            	break;
            case 3:
				new Splash(this);
            	break;
            case 4:
				Display.getDisplay(this).setCurrent(loghist);
				break;
            default:
//            	#debug
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


	public void log(String msg){
		if (l != null){
//			#debug
        System.out.println(msg);
        loghist.append(msg, null);
		}
	}

	public Configuration getConfig() {
		return config;
	}

	public static GpsMid getInstance() {
		return instance;
	}
}
