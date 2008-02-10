package de.ueller.midlet.gps;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

//#if polish.api.pdaapi
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
//#endif

public class FsDiscover
//#if polish.api.pdaapi
	extends List implements Runnable, GpsMidDisplayable, CommandListener
	//#endif
{
//#if polish.api.pdaapi
	private GpsMidDisplayable parent;
	private SelectionListener  sl;
	private Thread processorThread;
	private final static Logger logger=Logger.getInstance(FsDiscover.class,Logger.TRACE);
	
	private final Command			BACK_CMD		= new Command("Back", Command.BACK, 2);

	private final Command			OK_CMD			= new Command("Ok", Command.ITEM, 1);
	private final Command			UP_CMD			= new Command("Directory up", Command.ITEM, 1);
	private final Command			DOWN_CMD		= new Command("Directory down", Command.ITEM, 1);
	
	private String url = "file:///";
	private Vector urlList;
	private boolean root;

	public FsDiscover(GpsMidDisplayable parent, SelectionListener sl) {
			super("File chooser", Choice.IMPLICIT);
			this.parent = parent;
			this.sl = sl;			
			this.addCommand(BACK_CMD);
			this.addCommand(OK_CMD);
			this.addCommand(UP_CMD);
			this.addCommand(DOWN_CMD);
			this.setCommandListener(this);
			urlList = new Vector();
			this.setSelectCommand(DOWN_CMD);
			
			// we have to initialize a system in different thread...
//			logger.debug("start Thread");
			root = true;
			processorThread = new Thread(this);
			processorThread.start();
	}
	

	public void run() {
		if (root)
			getRoots();
		else
			getRootContent(url);
	}
    private void getRoots() {
    	this.deleteAll();
        urlList.removeAllElements();
    	//#if polish.api.pdaapi
//    	logger.debug("getRoot");
        Enumeration drives = FileSystemRegistry.listRoots();
//        logger.debug("The valid roots found are: ");
        while(drives.hasMoreElements()) {
           String root = (String) drives.nextElement();
           logger.debug("found "+root);
           this.append(root, null);
           urlList.addElement(url + root);
        }
        //#else
        this.append("API not supported by device", null);
        //#endif
     }
    private void getRootContent(String root) {
    	//#if polish.api.pdaapi
        try {
        	logger.debug("List of files and directories under "+root+":");
        	
           FileConnection fc = (FileConnection) Connector.open(root,Connector.READ);
           // Get a filtered list of all files and directories.
           // True means: include hidden files.
           // To list just visible files and directories, use
           // list() with no arguments.           
           if (!fc.isDirectory())
        	   return;
           this.deleteAll();
           urlList.removeAllElements();
           Enumeration filelist = fc.list();
           while(filelist.hasMoreElements()) {
              String fileName = (String) filelist.nextElement();
              logger.debug("found file: " + fileName);
              this.append(fileName, null);
              urlList.addElement(url + fileName);              
           }   
           fc.close();
        } catch (IOException ioe) {
           logger.error(ioe.getMessage());
        }
        //#endif
     }
    
    public void commandAction(Command c, Displayable d) {
    	if (processorThread.isAlive()) {
    		logger.error("Still busy, try again later");
    		return;
    	}
    	if (c == BACK_CMD) {
			parent.show();
			return;
		}
    	if (c == OK_CMD) {
    		url = (String)urlList.elementAt(this.getSelectedIndex());
    		sl.selectedFile(url);
			parent.show();
			return;
		}
    	if (c == DOWN_CMD) {
    		root = false;    		
    		url = (String)urlList.elementAt(this.getSelectedIndex());
    		this.setTitle(url);
    		processorThread = new Thread(this);
    		processorThread.start();
			return;
		}
    	if (c == UP_CMD) {
    		root = false;
    		url = url.substring(0, url.substring(0, url.length() - 1).lastIndexOf('/') + 1);
    		logger.debug("Moving up directory to :" + url);
    		if (url.length() < 9) {  //file:///
    			url = "file:///";
    			root = true;
    		}
    		this.setTitle(url);    		
    		processorThread = new Thread(this);
    		processorThread.start();
			return;
    	}
    	
    }


	public void show() {
		
		Display.getDisplay(GpsMid.getInstance()).setCurrent(this);		
	}
	//#endif

}
