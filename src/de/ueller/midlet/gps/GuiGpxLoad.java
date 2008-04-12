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
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;

import de.ueller.gps.tools.StringTokenizer;

public class GuiGpxLoad extends Form implements CommandListener,
		GpsMidDisplayable, SelectionListener{

	private final static Logger logger=Logger.getInstance(GuiGpxLoad.class,Logger.DEBUG);
	
	private final Command IMPORT_CMD = new Command("Import", Command.OK, 1);
	private final Command BACK_CMD = new Command("Back", Command.BACK, 5);
	
	private final String LOADFROMFILE = "from file";
	private final String LOADFROMBT = "via bluetooth";
	private final String LOADFROMCOMM = "from commport";
	
	private GpsMidDisplayable parent;
	private UploadListener feedbackListener;
	
	private final Form menuLoadGpx = new Form("Import GPX file");
	private ChoiceGroup choiceFrom;
	private TextField tfMaxDist;
	private boolean getWpts;
	private float maxDistance;
	
	public GuiGpxLoad(GpsMidDisplayable parent, UploadListener ul, boolean getWpts) {
	    super(null);
	    this.parent = parent;
		this.feedbackListener = ul;
		this.getWpts=getWpts;
		
		maxDistance=0;

		menuLoadGpx.addCommand(BACK_CMD);
		menuLoadGpx.addCommand(IMPORT_CMD);
		choiceFrom=new ChoiceGroup("Load Gpx",Choice.EXCLUSIVE);
		//#if polish.api.fileconnectionapi
		choiceFrom.append(LOADFROMFILE,null);
		//#endif
		//#if polish.api.obex
		choiceFrom.append(LOADFROMBT,null);
		//#endif
		//this.append(LOADFROMCOMM,null); //Not tested properly, so leave it out		
		if (choiceFrom.size() == 0) {
			choiceFrom.append("No method for loading available",null);
		}
		menuLoadGpx.append(choiceFrom);
		if (getWpts) {
			tfMaxDist = new TextField("max. distance in km to current map position (0=no limit)","0",3,TextField.DECIMAL);
			menuLoadGpx.append(tfMaxDist);
		}
		menuLoadGpx.setCommandListener(this);		
	}
	
	public void commandAction(Command c, Displayable d) {
		logger.debug("got Command " + c);
		if (c == BACK_CMD) {
			parent.show();
		}
		if (c == IMPORT_CMD) {
			maxDistance=0;
			if (getWpts && tfMaxDist.getString().length()!=0) {
				try {
					maxDistance=Float.parseFloat(tfMaxDist.getString());
				} catch (NumberFormatException nfe) {
					logger.info("Couldn't convert the distance into a float");
					maxDistance = 0.0f;
				}
				if (maxDistance<0) {
					maxDistance=0;
				}
			}
			String choice = choiceFrom.getString(choiceFrom.getSelectedIndex());
			logger.info(choice);
			if (choice.equalsIgnoreCase(LOADFROMBT)) {
				//#if polish.api.obex
				Alert alert = new Alert("Information");
				alert.setTimeout(3000);
				alert.setString("Obex server started, please send your GPX file now");
				BtObexServer obexServer = new BtObexServer(feedbackListener, maxDistance);
				Display.getDisplay(Trace.getInstance().getParent()).setCurrent(alert);
				//#endif
			} else if (choice.equalsIgnoreCase(LOADFROMCOMM)) {
				String commports = System.getProperty("microedition.commports");			
				String[] commport = StringTokenizer.getArray(commports, ",");
				selectedFile("comm:" + commport[0] + ";baudrate=19200");				
			} else if(choice.equalsIgnoreCase(LOADFROMFILE)) {
				//#if polish.api.fileconnectionapi
				FsDiscover fsd = new FsDiscover(this,this,GpsMid.getInstance().getConfig().getGpxUrl(),false,".gpx","Load *.gpx file");
				fsd.show();				
				//#endif				
			}
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(menuLoadGpx);
		//Display.getDisplay(GpsMid.getInstance()).setCurrent(menuLoadGpx);
	}

	public void selectedFile(String url) {
		try {
			logger.info("Receiving gpx: " + url);
			Connection c  = Connector.open(url,Connector.READ);			
			if (c instanceof InputConnection) {
				InputConnection inConn = ((InputConnection)c);				
				Trace.getInstance().gpx.receiveGpx(inConn.openInputStream(), feedbackListener, maxDistance);
				return;
			}
			logger.error("Unknown url type to load from: " + url);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
