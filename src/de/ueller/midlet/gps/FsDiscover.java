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
	private boolean chooseDir;
	
	private Thread processorThread;
	private final static Logger logger=Logger.getInstance(FsDiscover.class,Logger.TRACE);
	
	private final Command			BACK_CMD		= new Command("Back", Command.BACK, 2);
	private final Command			OK_CMD			= new Command("Select", Command.ITEM, 1);

	private final Command			UP_CMD			= new Command("Directory up", Command.ITEM, 1);
	private final Command			DOWN_CMD		= new Command("Directory down", Command.ITEM, 1);
	
	private String url;
	private Vector urlList;
	//private boolean root;
	
	private List list;	

	private String orgTitle;
	private String suffix;
	
	public FsDiscover(GpsMidDisplayable parent, SelectionListener sl, String initUrl, boolean chooseDir, String suffix, String title) {			
			//orgTitle=Display.getDisplay(GpsMid.getInstance()).getCurrent().getTitle();
			this.parent = parent;
			this.sl = sl;
			this.chooseDir=chooseDir;
			this.suffix=suffix;
			this.orgTitle=title;
			urlList = new Vector();
			//#if polish.api.pdaapi
			// avoid NullPointer exception
			if (initUrl==null) {
				initUrl="";
			}
			if (initUrl.length() >= 9) {  //file:///
            	// if url is file get dir only
				if (!initUrl.endsWith("/")) {
            		initUrl = initUrl.substring(0, initUrl.substring(0, initUrl.length() - 1).lastIndexOf('/') + 1);
            	}
    		}
			// start browsing at initial url
        	url=initUrl;
			// if no initial dir usable use root
			if (url.length() < 9) {  //file:///
				url = "file:///";
			}
            //#endif

			
			processorThread = new Thread(this);
			processorThread.start();
			this.list = createEmptyList();
	}
	
	private List createEmptyList() {
		String title=orgTitle;
		// don't show "file:///"
		if (url.toLowerCase().startsWith("file:///")) {
			// if in root dir show original list title
			if(url.length()==8) {
				title=orgTitle;
			} else {
				title=url.substring(8);				
			}
		}
		List list = new List(title, Choice.IMPLICIT);
		list.addCommand(BACK_CMD);
		list.addCommand(OK_CMD);		
		list.setCommandListener(this);
		list.setSelectCommand(DOWN_CMD);
		return list;
	}
	

	public void run() {
		try {
		if (url.equalsIgnoreCase("file:///")) {
			getRoots();
		} else {
			// try to get url content, if nok get roots instead
			if(!getRootContent(url)) {
				url="file:///";
				getRoots();			
			}
		}
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
    private boolean getRootContent(String rootDir) {
    	//#if polish.api.pdaapi
        try {
        	logger.debug("List of files and directories under "+rootDir+":");        	
        	FileConnection fc = (FileConnection) Connector.open(rootDir,Connector.READ);
           // Get a filtered list of all files and directories.
           // True means: include hidden files.
           // To list just visible files and directories, use
           // list() with no arguments.
            if (!fc.exists()) {
				return false;
			}
           if (!fc.isDirectory()) {
        	   //If it is not a directory,
        	   //Then we assume it must be an
        	   //Ok selection        	  
        	   url = (String)urlList.elementAt(list.getSelectedIndex());
        	   parent.show();
        	   sl.selectedFile(url);
        	   return true;
           }
        	   
           list = createEmptyList();
           urlList.removeAllElements();
           list.append("..", null);
           urlList.addElement(url + "Directory Up");
           Enumeration filelist = fc.list();
           while(filelist.hasMoreElements()) {
              String fileName = (String) filelist.nextElement();
              logger.debug("found file: " + fileName);
              // add files too if not choosedir
    		  if(!chooseDir || fileName.endsWith("/")) {
    			  // for files check also if suffix matches
    			  if (chooseDir || fileName.endsWith("/") || ( suffix.length()>0 && fileName.toLowerCase().endsWith(suffix.toLowerCase()) ) ) {
      				  //System.out.println("Adding " + fileName);
	    			  list.append(fileName, null);
		              urlList.addElement(url + fileName);              
    			  }
			  }
           }   
           fc.close();
           show();
        } catch (IOException ioe) {
           logger.error(ioe.getMessage());
        }
        //#endif
        return true;
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
    		url = url.substring(0, url.substring(0, url.length() - 1).lastIndexOf('/') + 1);
    		logger.debug("Moving up directory to :" + url);
    		if (url.length() < 9) {  //file:///
    			url = "file:///";
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
