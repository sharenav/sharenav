/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */
package de.ueller.midlet.gps;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;



public class GpsMid extends MIDlet implements CommandListener{
    /** A menu list instance */
    private static final String[] elements = { "Trace","Setup"};

    /** Soft button for exiting GpsMis. */
    private final Command EXIT_CMD = new Command("Exit", Command.EXIT, 2);

    /** Soft button for launching a client or sever. */
    private final Command OK_CMD = new Command("Ok", Command.SCREEN, 1);

    /** A menu list instance */
    private final List menu = new List("GPSMid", Choice.IMPLICIT, elements, null);

//	private boolean	isInit=false;

	private String	btUrl="btspp://000DB5315C50:1;authenticate=false;encrypt=false;master=false";

	private String	root;
//	PrintStream log;
	Logger l;


	public GpsMid() {
//		menu.setFont(Font.FACE_MONOSPACE, null);
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
//		System.out.println("sinh 0.8    " + MoreMath.asinh(MoreMath.sinh(0.8f)));
//		System.out.println("tanh 0.8    " + MoreMath.atan((float) Math.tan(0.8f)));
//		System.out.println("sinh 0.7    " + MoreMath.asinh(MoreMath.sinh(0.7f)));
//		System.out.println("tanh 0.7    " + MoreMath.atan((float) Math.tan(0.7f)));
//		System.out.println("sinh 0.9    " + MoreMath.asinh(MoreMath.sinh(0.9f)));
//		System.out.println("tanh 0.9    " + MoreMath.atan((float) Math.tan(0.9f)));
	}
	
	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
	}

	protected void pauseApp() {
	// TODO Auto-generated method stub

	}

	protected void startApp() throws MIDletStateChangeException {
		Display.getDisplay(this).setCurrent(menu);
		}

	public void commandAction(Command c, Displayable d) {
        if (c == EXIT_CMD) {
            try {
				destroyApp(true);
			} catch (MIDletStateChangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            notifyDestroyed();

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
					new Trace(this,btUrl,root);
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
            default:
                System.err.println("Unexpected choice...");

                break;
            }

//            isInit = true;


		
	}
    /** Shows main menu of MIDlet on the screen. */
    void show() {
        Display.getDisplay(this).setCurrent(menu);
    }

	public void setBTUrl(String btUrl) {
		this.btUrl = btUrl;
	}
	public void setRootFs(String root){
		this.root = root;
	}
	public void log(String msg){
		if (l != null){
//		log.print(msg+"\n");
//        Display.getDisplay(this).getCurrent().setTicker(new Ticker(msg));
//		Display.getDisplay(this).getCurrent().setTitle(msg);
        System.out.println(msg);
		}
	}

}
