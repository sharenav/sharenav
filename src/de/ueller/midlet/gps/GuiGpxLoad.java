package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.IOException;

import javax.microedition.io.CommConnection;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.InputConnection;
//#if polish.api.pdaapi
import javax.microedition.io.file.FileConnection;
//#endif
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import de.ueller.gps.tools.StringTokenizer;

public class GuiGpxLoad extends List implements CommandListener,
		GpsMidDisplayable, SelectionListener{

	private final static Logger logger=Logger.getInstance(GuiGpxLoad.class,Logger.DEBUG);
	
	private final Command SELECT_CMD = new Command("Select", Command.OK, 1);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	
	private GpsMidDisplayable parent;
	
	public GuiGpxLoad(GpsMidDisplayable parent) {
		super("Load GPX files", List.EXCLUSIVE);
		this.parent = parent;
		addCommand(BACK_CMD);
		addCommand(SELECT_CMD);
		setCommandListener(this);
		//#if polish.api.fileconnectionapi
		this.append("Load from file",null);
		//#endif
		//#if polish.api.btapi
		this.append("Load via bluetooth",null);
		//#endif
		this.append("Load from commport",null);		
		if (this.size() == 0) {
			this.append("No method for loading available",null);
		}
		
	}
	
	public void commandAction(Command c, Displayable d) {
		logger.debug("got Command " + c);
		if (c == BACK_CMD) {
			parent.show();
		}
		if (c == SELECT_CMD) {
			String choice = this.getString(this.getSelectedIndex());
			logger.info(choice);
			if (choice.equalsIgnoreCase("Load via bluetooth")) {
				logger.error("Sorry, loading via bluetooth isn't implemented yet");
			} else if (choice.equalsIgnoreCase("Load from commport")) {
				String commports = System.getProperty("microedition.commports");			
				String[] commport = StringTokenizer.getArray(commports, ",");
				selectedFile("comm:" + commport[0] + ";baudrate=19200");				
			} else if(choice.equalsIgnoreCase("Load from file")) {
				//#if polish.api.fileconnectionapi
				FsDiscover fsd = new FsDiscover(this,this);
				fsd.show();				
				//#endif				
			}
		}
	}
	
	public void show() {
		Display.getDisplay(GpsMid.getInstance()).setCurrent(this);
	}

	public void selectedFile(String url) {
		try {
			logger.info("Receiving gpx: " + url);
			Connection c  = Connector.open(url);			
			if (c instanceof InputConnection) {
				InputConnection inConn = ((InputConnection)c);				
				Trace.getInstance().gpx.receiveGpx(inConn.openInputStream());
				return;
			}
			logger.error("Unknown url type to load from: " + url);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
