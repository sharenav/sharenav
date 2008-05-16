package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;
import de.ueller.midlet.gps.importexport.GpxImportSession;

public class GuiGpxLoad extends Form implements CommandListener,
		GpsMidDisplayable {

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
			GpxImportSession importSession = null;
			try {
				/**
				 * We jump through hoops here (Class.forName) in order to decouple
				 * the implementation of JSRs. The problem is, that not all phones have all
				 * of the JSRs, and if we simply called the Class directly, it would
				 * cause the whole app to crash. With this method, we can hopefully catch
				 * the missing JSRs and gracefully report an error to the user that the operation
				 * is not available on this phone. 
				 */
				/**
				 * The Class.forName and the instantition of the class must be separate
				 * statements, as otherwise this confuses the proguard obfuscator when
				 * rewriting the flattened renamed classes.
				 */
				Class tmp = null;
				if (choice.equalsIgnoreCase(LOADFROMBT)) {
					tmp = Class.forName("de.ueller.midlet.gps.importexport.BtObexServer");
				} else if (choice.equalsIgnoreCase(LOADFROMCOMM)) {
					tmp = Class.forName("de.ueller.midlet.gps.importexport.CommGpxImportSession");									
				} else if(choice.equalsIgnoreCase(LOADFROMFILE)) {
					tmp = Class.forName("de.ueller.midlet.gps.importexport.FileGpxImportSession");					
				}
				if (tmp != null)
					importSession = (GpxImportSession)(tmp.newInstance());
			} catch (ClassNotFoundException cnfe) {
				logger.error("The type of Gpx import you have selected is not supported by your phone");
			} catch (Exception e) {
				logger.exception("Could not start the import server", e);
			} 
			if (importSession != null) {
				importSession.initImportServer(feedbackListener, maxDistance, menuLoadGpx);
			} else {
				logger.error("The type of Gpx import you have selected is not supported by your phone");
			}
		}
	}
	
	public void show() {
		GpsMid.getInstance().show(menuLoadGpx);
		//Display.getDisplay(GpsMid.getInstance()).setCurrent(menuLoadGpx);
	}

	
}
