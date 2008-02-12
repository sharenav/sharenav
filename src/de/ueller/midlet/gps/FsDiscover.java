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
	implements Runnable, GpsMidDisplayable, CommandListener
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
	
	private List list;	

	public FsDiscover(GpsMidDisplayable parent, SelectionListener sl) {			
			this.parent = parent;
			this.sl = sl;			
			urlList = new Vector();
			root = true;
			processorThread = new Thread(this);
			processorThread.start();
			this.list = createEmptyList();
	}
	
	private List createEmptyList() {
		List list = new List(url, Choice.IMPLICIT);
		list.addCommand(BACK_CMD);
		list.addCommand(OK_CMD);		
		list.setCommandListener(this);
		list.setSelectCommand(DOWN_CMD);
		return list;
	}
	

	public void run() {
		try {
		if (root)
			getRoots();
		else
			getRootContent(url);
		} catch (SecurityException se) {
			logger.exception("Security exception: You are not permitted to access this restricted API :-(", se);
		}
	}
    private void getRoots() {    	
    	//Work around the problem that list.deleteAll might cause problems on a SE K750
    	list = createEmptyList();    	
        urlList.removeAllElements();
    	//#if polish.api.pdaapi
//    	logger.debug("getRoot");
        Enumeration drives = FileSystemRegistry.listRoots();        
//        logger.debug("The valid roots found are: ");
        while(drives.hasMoreElements()) {
           String root = (String) drives.nextElement();
           list.append(root, null);
           urlList.addElement(url + root);
        }
        //#else
        list.append("API not supported by device", null);
        //#endif
        //Display the new list. We just assume, that the old list was still displaying
        //As an attempt to check if that was the case didn't seem to work reliably on
        //some mobiles
        show();
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
           list = createEmptyList();
           urlList.removeAllElements();
           list.append("..", null);
           urlList.addElement(url + "Directory Up");
           Enumeration filelist = fc.list();
           while(filelist.hasMoreElements()) {
              String fileName = (String) filelist.nextElement();
              logger.debug("found file: " + fileName);
              list.append(fileName, null);
              urlList.addElement(url + fileName);              
           }   
           fc.close();
           show();
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
    	if (list.getSelectedIndex() < 0) {
    		logger.error("No element selected");
    		return;    		
    	}
    	if (c == OK_CMD) {
    		url = (String)urlList.elementAt(list.getSelectedIndex());
    		parent.show();
    		sl.selectedFile(url);			
			return;
		}
    	if (c == DOWN_CMD) {
    		root = false;
    		if (list.getString(list.getSelectedIndex()).equalsIgnoreCase("..")) {
    			c = UP_CMD;
    		} else {
    			url = (String)urlList.elementAt(list.getSelectedIndex());    		
    			processorThread = new Thread(this);
    			processorThread.start();
    			return;
    		}			
		}
    	if (c == UP_CMD) {
    		root = false;
    		url = url.substring(0, url.substring(0, url.length() - 1).lastIndexOf('/') + 1);
    		logger.debug("Moving up directory to :" + url);
    		if (url.length() < 9) {  //file:///
    			url = "file:///";
    			root = true;
    		}    		    		
    		processorThread = new Thread(this);
    		processorThread.start();
			return;
    	}
    	
    }


	public void show() {		
		Display.getDisplay(GpsMid.getInstance()).setCurrent(list);		
	}
	//#endif

}
