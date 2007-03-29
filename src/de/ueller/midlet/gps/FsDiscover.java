package de.ueller.midlet.gps;

import java.io.IOException;
import java.util.Enumeration;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;


public class FsDiscover implements Runnable {

	private GuiDiscover	parent;
	private Thread	processorThread;
//	private final static Logger logger=Logger.getInstance(FsDiscover.class,Logger.TRACE);

	public FsDiscover(GuiDiscover parent) {
			this.parent = parent;
			// we have to initialize a system in different thread...
//			logger.debug("start Thread");
			processorThread = new Thread(this);
			processorThread.start();

		}
	

	public void run() {
		getRoots();
		parent.fsDiscoverReady();
		
	}
    private void getRoots() {
//    	logger.debug("getRoot");
        Enumeration drives = FileSystemRegistry.listRoots();
//        logger.debug("The valid roots found are: ");
        while(drives.hasMoreElements()) {
           String root = (String) drives.nextElement();
//           logger.debug("found "+root);
           parent.addRootFs(root);
        }
     }
    private void getRootContent(String root) {
        try {
           FileConnection fc = (FileConnection)
              Connector.open("file:///"+root);
           // Get a filtered list of all files and directories.
           // True means: include hidden files.
           // To list just visible files and directories, use
           // list() with no arguments.
           System.out.println
              ("List of files and directories under "+root+":");
           Enumeration filelist = fc.list("*", true);
           while(filelist.hasMoreElements()) {
              String fileName = (String) filelist.nextElement();
              fc = (FileConnection)
                 Connector.open("file:///" + root + fileName);
              if(fc.isDirectory()) {
                 //m.setTitle("\tDirectory Name: " + fileName);
              } else {
                 System.out.println
                    ("\tFile Name: " + fileName + 
                     "\tSize: "+fc.fileSize());
              }
              
           }   
           fc.close();
        } catch (IOException ioe) {
           System.out.println(ioe.getMessage());
        }
     }

}
