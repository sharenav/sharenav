/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See COPYING
 */

package net.sharenav.util;

import de.enough.polish.util.Locale;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.ui.ShareNav;

import javax.microedition.lcdui.Alert;

/** Provides an interface for the logging of messages.
 * It mainly implements the different logging levels.
 * There are many instances to distinguish the different "sources" (usually identical to
 * classes) and their logging levels, but the active levels are determined by 
 * class variables here. 
 * The actual logging is done by the ShareNav instance that is passed to the constructor. 
 * There can only be one such class.
 */
public class Logger {
	public final static int FATAL = 1;
	public final static int ERROR = 2;
	public final static int INFO = 3;
	public final static int DEBUG = 4;
	public final static int TRACE = 5;
	private static ShareNav app;
	private String source;
	private int level = ERROR;
	private static boolean infoEnabled;
	private static boolean debugEnabled;
	private static boolean traceEnabled;

	public Logger(ShareNav app) {
		Logger.app = app;
		this.source = getClassName(app.getClass());
	}

	public Logger(Class c) {
		this.source = getClassName(c);		
	}

	public Logger(Class c, int level) {
		this.source = getClassName(c);
		this.level = level;		
	}

	public static Logger getInstance(Class c) {
		if (app == null) {
			return null;
//			throw new Error("not initialized");
		}
		return new Logger(c);
	}

	public static Logger getInstance(Class c, int level) {
		if (app == null) {
			return null;
//			throw new Error("not initialized");
		}
		return new Logger(c, level);
	}

	/** Logs a fatal error. It is also displayed in an alert window. 
	 */
	public void fatal(String msg) {
		if (level >= FATAL) {
			app.log("F[" + source + msg);			
			ShareNav.getInstance().alert(Locale.get("logger.Fatal")/*Fatal*/, msg, Alert.FOREVER);
		}
	}

	/** Logs an error. It is also displayed in an alert window. 
	 */
	public void error(String msg) {		
		error(msg, false);
	}

	/** Logs an error. It is also displayed in an alert window if silent is false. 
	 */
	public void error(String msg, boolean silent) {		
		if (level >= ERROR) {
			app.log("E[" + source + msg);
			if (!silent) {
			    ShareNav.getInstance().alert(Locale.get("logger.Error")/*Error*/, msg, Alert.FOREVER);			
			}
		}		
	}

	/** Logs an exception. It is also displayed in an alert window. 
	 */
	public void exception(String msg, Exception e) {		
		error(msg + ": " + e + ": " + e.getMessage());
		e.printStackTrace();		
	}

	/** Logs an exception. It is *not* displayed in an alert window. 
	 */
	public void silentexception(String msg, Exception e) {		
		error(msg + ": " + e + ": " + e.getMessage(), true);
		e.printStackTrace();		
	}

	/** Logs an information message, the highest level of log messages. 
	 */
	public void info(String msg) {
		//#mdebug info
		if (level >= INFO && infoEnabled) {
			app.log("I[" + source + msg);
		}
		//#enddebug
	}

	/** Logs a debug message, the second highest level of log messages. 
	 */
	public void debug(String msg) {
		//#mdebug debug
		if (level >= DEBUG && debugEnabled) {
			app.log("D[" + source + msg);
		}
		//#enddebug
	}

	/** Logs a trace message, the lowest level of log messages. 
	 */
	public void trace(String msg) {
		//#mdebug debug
		if (level >= TRACE && traceEnabled) {
			app.log("T[" + source + msg);
		}
		//#enddebug
	}
	
	private static String getClassName(Class c) {
		if (c != null) {
			String n = c.getName();
			return n.substring(n.lastIndexOf('.') + 1, n.length()) + "] ";
		} else {
			return "";
		}
	}

	public int getLevel() {
		return level;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public static void setGlobalLevel() {
		infoEnabled = Configuration.getDebugSeverityInfo();
		debugEnabled = Configuration.getDebugSeverityDebug();
		traceEnabled = Configuration.getDebugSeverityTrace();
	}
}
