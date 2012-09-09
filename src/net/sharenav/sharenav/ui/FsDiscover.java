package net.sharenav.sharenav.ui;

/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
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

import de.enough.polish.util.Locale;
import net.sharenav.sharenav.data.Configuration;
import net.sharenav.midlet.ui.SelectionListener;
import net.sharenav.util.Logger;
import net.sharenav.util.StringTokenizer;

public class FsDiscover
//#if polish.api.pdaapi
		implements Runnable, ShareNavDisplayable, CommandListener
//#endif
{
	//#if polish.api.pdaapi
	private ShareNavDisplayable parent;
	private SelectionListener sl;
	private int chooseType;
	
	public static final int CHOOSE_FILEONLY = 0;
	public static final int CHOOSE_DIRONLY = 1;
	public static final int CHOOSE_FILE_OR_DIR = 2;

	private Thread processorThread;
	private final static Logger logger = Logger.getInstance(FsDiscover.class,
			Logger.TRACE);

    private final Command BACK_CMD = new Command(Locale.get("generic.Back")/*Back*/, Command.BACK, 2);
    private final Command OK_CMD = new Command(Locale.get("fsdiscover.Select")/*Select*/, Command.ITEM, 1);

    private final Command UP_CMD = new Command(Locale.get("fsdiscover.DirUp")/*Directory up*/, Command.ITEM, 1);
    private final Command ROOT_CMD = new Command(Locale.get("fsdiscover.GoToRoot")/*Go to root(s)*/, Command.ITEM, 1);
    private final Command DOWN_CMD = new Command(Locale.get("fsdiscover.DirDown")/*Directory down*/,
			Command.ITEM, 1);

	private volatile String url;
	private Vector urlList;
	private Vector files;
	private Vector dirs;
	// private boolean root;

	private List list;

	private String orgTitle;
	private Vector suffixes = null;

	public FsDiscover(ShareNavDisplayable parent, SelectionListener sl,
			String initUrl, int chooseType, String suffix, String title) {
		//orgTitle=Display.getDisplay(ShareNav.getInstance()).getCurrent().getTitle
		// ();
		this.parent = parent;
		this.sl = sl;
		this.chooseType = chooseType;
		// multiple suffixes separated by ; can be given
		if (suffix != null) {
			suffixes = StringTokenizer.getVector(suffix, ";");
		} 
		this.orgTitle = title;
		urlList = new Vector();
		files = new Vector();
		dirs = new Vector();
		//#if polish.api.pdaapi
		// avoid NullPointer exception
		if (initUrl == null) {
			initUrl = "";
		}
		if (initUrl.length() >= 9) { // file:///
			// if url is file get dir only
			if (!initUrl.endsWith("/")) {
				initUrl = initUrl.substring(0, initUrl.substring(0,
						initUrl.length() - 1).lastIndexOf('/') + 1);
			}
		}
		// start browsing at initial url
		url = initUrl;
		// if no initial dir usable use root
		if (url.length() < 9) { // file:///
			url = "file:///";
		}
		//#endif

		this.list = createEmptyList();
		processorThread = new Thread(this);
		processorThread.start();
	}

	private List createEmptyList() {
		String title = orgTitle;
		// don't show "file:///"
		if (url.toLowerCase().startsWith("file:///")) {
			// if in root dir show original list title
			if (url.length() == 8) {
				title = orgTitle;
			} else {
				title = url.substring(8);
			}
		}
		List list = new List(title, Choice.IMPLICIT);
		list.addCommand(BACK_CMD);
		list.addCommand(OK_CMD);
		list.addCommand(UP_CMD);
		list.addCommand(ROOT_CMD);
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
				if (!getRootContent(url)) {
					url = "file:///";
					getRoots();
				}
			}
		} catch (SecurityException se) {
			logger
					.exception(
							"Security exception: You are not permitted to access this restricted API :-(",
							se);
		}
	}

	private void getRoots() {
		// Work around the problem that list.deleteAll might cause problems on a
		// SE K750
		list = createEmptyList();
		urlList.removeAllElements();
		//#if polish.api.pdaapi
		// logger.debug("getRoot");
		Enumeration drives = FileSystemRegistry.listRoots();
		// logger.debug("The valid roots found are: ");
		while (drives.hasMoreElements()) {
			String root = (String) drives.nextElement();
			list.append(root, null);
			urlList.addElement(url + root);
		}
		//#else
		list.append(Locale.get("fsdiscover.APINotSupByDevice")/*API not supported by device*/, null);
		//#endif
		// Display the new list. We just assume, that the old list was still
		// displaying
		// As an attempt to check if that was the case didn't seem to work
		// reliably on
		// some mobiles
		show();
	}

	private boolean getRootContent(String rootDir) {
		//#if polish.api.pdaapi
		try {
			//#debug debug
			logger
					.debug("List of files and directories under " + rootDir
							+ ":");
			FileConnection fc = (FileConnection) Connector.open(rootDir,
					Connector.READ);
			// Get a filtered list of all files and directories.
			// True means: include hidden files.
			// To list just visible files and directories, use
			// list() with no arguments.
			if (!fc.exists()) {
				url = "file:///";
				return false;
			}
			if (!fc.isDirectory()) {
				// If it is not a directory,
				// Then we assume it must be an
				// Ok selection
				selectEntry();
				return true;
			}

			list = createEmptyList();
			urlList.removeAllElements();
			list.append(Locale.get("fsdiscover.thisdir")/*[This directory]*/, null);
			urlList.addElement(url);
			list.append("..", null);
			//urlList.addElement(url + "Directory Up");
			urlList.addElement(url);
			String fileName;
			String suffix;
			//#if polish.android
			// workaround for J2MEPolish 2.3 bug, jkpj 2012-03-15
			Enumeration filelist = fc.list("*", false);
			//#else
			Enumeration filelist = fc.list();
			//#endif
			while (filelist.hasMoreElements()) {
				fileName = (String) filelist.nextElement();
				//#debug debug
				logger.debug("found file: " + fileName);
				// add files too if not choose dir only
				//#if polish.android
				// was workaround for J2MEPolish before 2.3
				//FileConnection fc2 = (FileConnection) Connector.open(url + fileName);
				//if (fc2.isDirectory()) {
				//	fileName += "/";
				//	fc2.close();
				//}
				//#endif
				
				// only add entries matching the criteria
				boolean matching = false;
				if (fileName.endsWith("/")) {
					matching = true;
				} else if (chooseType != CHOOSE_DIRONLY) {
					// when there's no suffix to check for, each file is matching
					if (suffixes == null) {
						matching = true;
					// otherwise check if one of the ;-separated suffixes matches
					} else {
						for (int i=0; i < suffixes.size(); i++) {
							suffix = (String) suffixes.elementAt(i);
							if (fileName.toLowerCase().endsWith(suffix.toLowerCase())) {
								matching = true;
							}
						}
					}
				}
				
				// insert matching entries in the appropriate list at the right index to have them sorted
				if (matching) {
					// System.out.println("Adding " + fileName);
					if (fileName.endsWith("/")) {
						dirs.insertElementAt(fileName, getSortIdx(dirs, fileName));
					} else {
						files.insertElementAt(fileName, getSortIdx(files, fileName));
					}
				}
			}

			// add sorted directory names to list and urlList
			for (int i=0; i < dirs.size(); i++) {
				fileName = (String) dirs.elementAt(i);
				list.append(fileName, null);
				urlList.addElement(url + fileName);
			}
			dirs.removeAllElements();
			// add sorted filenames to list and urlList
			for (int i=0; i < files.size(); i++) {
				fileName = (String) files.elementAt(i);
				list.append(fileName, null);
				urlList.addElement(url + fileName);
			}
			files.removeAllElements();

			fc.close();
			show();
		} catch (IOException ioe) {
			logger.error(ioe.getMessage());
		}
		catch( SecurityException e ){
			logger.error(e.getMessage());
		}
		//#endif
		return true;
	}

	/** get index where String s should be sorted into vector v consisting of Strings */
	private int getSortIdx(Vector v, String s) {
		String compare;
		int i = 0;
		while (i < v.size()) {
			compare = (String) v.elementAt(i);
			if (s.compareTo(compare) < 0) {
				break;
			}
			i++;
		}
		return i;
	}
	
	public void commandAction(Command c, Displayable d) {
		if (processorThread.isAlive()) {
			logger.error(Locale.get("fsdiscover.StillBusyTryAgain")/*Still busy, try again later*/);
			return;
		}
		if (c == BACK_CMD) {
			/**
			 * Need to call sl.selectedFile() before calling parent.show(),
			 * as we need to keep track of state for e.g. GuiDiscover
			 */
			sl.selectionCanceled();
			parent.show();
			return;
		}
		if (c == ROOT_CMD) {
			url = "file:///";
			processorThread = new Thread(this);
			processorThread.start();
			return;
		}
		if (list.getSelectedIndex() < 0) {
			logger.error(Locale.get("fsdiscover.NoElementSelected")/*No element selected*/);
			return;
		}
		if (c == OK_CMD) {
			selectEntry();
			return;
		}
		if (c == DOWN_CMD) {
			if (list.getString(list.getSelectedIndex()).equalsIgnoreCase("..")) {
				c = UP_CMD;
			} else if (list.getString(list.getSelectedIndex()).equalsIgnoreCase(Locale.get("fsdiscover.thisdir"))) {
				selectEntry();
				return;
			} else {
				url = (String) urlList.elementAt(list.getSelectedIndex());
				processorThread = new Thread(this);
				processorThread.start();
				return;
			}
		}
		if (c == UP_CMD) {
			url = url.substring(0, url.substring(0, url.length() - 1)
					.lastIndexOf('/') + 1);
			//#debug debug
			logger.debug("Moving up directory to :" + url);
			if (url.length() < 9) { // file:///
				url = "file:///";
			}
			processorThread = new Thread(this);
			processorThread.start();
			return;
		}

	}

	private void selectEntry() {
		url = (String) urlList.elementAt(list.getSelectedIndex());
		/**
		 * Need to call sl.selectedFile() before calling parent.show(),
		 * as we need to keep track of state for e.g. GuiDiscover
		 */
		if (chooseType == CHOOSE_FILEONLY && url.endsWith("/")) {
			logger.info("Requested a file but got a dir: " + url);
		} else {
			sl.selectedFile(url);
			parent.show();
		}		
	}
	
	public void show() {
		ShareNav.getInstance().show(list);
		// Display.getDisplay(ShareNav.getInstance()).setCurrent(list);
	}
	//#endif

}
