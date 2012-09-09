package net.sharenav.sharenav.ui;
/*
 * ShareNav - Copyright (c) 2008 Kai Krueger apmonkey at users dot sourceforge dot net 
 * See Copying
 */

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import net.sharenav.sharenav.importexport.GpxImportSession;
import net.sharenav.midlet.ui.UploadListener;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class GuiGpxLoad extends Form implements CommandListener,
		ShareNavDisplayable {

	private final static Logger logger = 
		Logger.getInstance(GuiGpxLoad.class, Logger.DEBUG);
	
	private final Command IMPORT_CMD = new Command(Locale.get("guigpxload.Import")/*Import*/, Command.OK, 1);
	private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 5);
	
	private final String LOADFROMFILE = Locale.get("guigpxload.FromFile")/*from file*/;
	private final String LOADFROMBT = Locale.get("guigpxload.ViaBluetooth")/*via bluetooth*/;
	private final String LOADFROMCOMM = Locale.get("guigpxload.FromCommport")/*from commport*/;
	
	private ShareNavDisplayable parent;
	private UploadListener feedbackListener;
	
	private final Form menuLoadGpx = new Form(Locale.get("guigpxload.ImportGPXFile")/*Import GPX file*/);
	private ChoiceGroup choiceFrom;
	private TextField tfMaxDist;
	private boolean getWpts;
	private float maxDistance;
	
	public GuiGpxLoad(ShareNavDisplayable parent, UploadListener ul, boolean getWpts) {
	    super(null);
	    this.parent = parent;
		this.feedbackListener = ul;
		this.getWpts = getWpts;
		
		maxDistance = 0;

		menuLoadGpx.addCommand(BACK_CMD);
		menuLoadGpx.addCommand(IMPORT_CMD);
		choiceFrom = new ChoiceGroup(Locale.get("guigpxload.LoadGPX")/*Load GPX*/, Choice.EXCLUSIVE);
		//#if polish.api.fileconnectionapi
		choiceFrom.append(LOADFROMFILE, null);
		//#endif
		//#if polish.api.obex
		choiceFrom.append(LOADFROMBT, null);
		//#endif
		//this.append(LOADFROMCOMM,null); //Not tested properly, so leave it out		
		if (choiceFrom.size() == 0) {
			choiceFrom.append(Locale.get("guigpxload.NoLoadingAvailable")/*No method for loading available*/, null);
		}
		menuLoadGpx.append(choiceFrom);
		if (getWpts) {
			tfMaxDist = new TextField(Locale.get("guigpxload.MaxDistanceKmPosition")/*Max. distance in km to current map position (0 = no limit)*/,
					"0", 3, TextField.DECIMAL);
			menuLoadGpx.append(tfMaxDist);
		}
		menuLoadGpx.setCommandListener(this);		
	}
	
	public void commandAction(Command c, Displayable d) {
		//#debug debug
		logger.debug("got Command " + c);
		if (c == BACK_CMD) {
			parent.show();
		}
		if (c == IMPORT_CMD) {
			maxDistance = 0;
			if (getWpts && tfMaxDist.getString().length() != 0) {
				try {
					maxDistance = Float.parseFloat(tfMaxDist.getString());
				} catch (NumberFormatException nfe) {
					//#debug info
					logger.info("Couldn't convert the distance into a float");
				}
				if (maxDistance < 0) {
					maxDistance = 0;
				}
			}
			String choice = choiceFrom.getString(choiceFrom.getSelectedIndex());
			//#debug info
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
				 * The Class.forName and the instantiation of the class must be separate
				 * statements, as otherwise this confuses the proguard obfuscator when
				 * rewriting the flattened renamed classes.
				 */
				Class tmp = null;
				if (choice.equalsIgnoreCase(LOADFROMBT)) {
					tmp = Class.forName("net.sharenav.sharenav.importexport.BtObexServer");
				} else if (choice.equalsIgnoreCase(LOADFROMCOMM)) {
					tmp = Class.forName("net.sharenav.sharenav.importexport.CommGpxImportSession");									
				} else if(choice.equalsIgnoreCase(LOADFROMFILE)) {
					tmp = Class.forName("net.sharenav.sharenav.importexport.FileGpxImportSession");					
				}
				if (tmp != null) {
					importSession = (GpxImportSession)(tmp.newInstance());
				}
			} catch (ClassNotFoundException cnfe) {
				ShareNav.getInstance().alert(Locale.get("guigpxload.Error")/*Error*/, Locale.get("guigpxload.GPXImportNotSupported")/*The type of GPX import you have selected is not supported by your phone.*/, Alert.FOREVER);
				logger.error(Locale.get("guigpxload.GPXImportNotSupported")/*The type of GPX import you have selected is not supported by your phone.*/);
			} catch (Exception e) {
				ShareNav.getInstance().alert(Locale.get("guigpxload.Error")/*Error*/, Locale.get("guigpxload.CouldNotStartImportServer")/*Could not start the import server.*/, Alert.FOREVER);
				logger.exception(Locale.get("guigpxload.CouldNotStartImportServer")/*Could not start the import server.*/, e);
			} 
			if (importSession != null) {
				// Trigger actual import 
				importSession.initImportServer(feedbackListener, maxDistance, menuLoadGpx);
			} else {
				ShareNav.getInstance().alert(Locale.get("guigpxload.Error")/*Error*/, Locale.get("guigpxload.GPXImportNotSupported")/*The type of GPX import you have selected is not supported by your phone.*/, Alert.FOREVER);
				logger.error(Locale.get("guigpxload.GPXImportNotSupported")/*The type of GPX import you have selected is not supported by your phone.*/);
			}
		}
	}
	
	public void show() {
		ShareNav.getInstance().show(menuLoadGpx);
		//Display.getDisplay(ShareNav.getInstance()).setCurrent(menuLoadGpx);
	}
}
