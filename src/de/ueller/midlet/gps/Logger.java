package de.ueller.midlet.gps;

import de.ueller.gps.data.Configuration;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Display;

/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 */



public class Logger {
	public final static int FATAL = 1;
	public final static int ERROR = 2;
	public final static int INFO = 3;
	public final static int DEBUG = 4;
	public final static int TRACE = 5;
	private static GpsMid app;
	private String source;
	private int level=ERROR;
	private static boolean infoEnabled;
	private static boolean debugEnabled;
	private static boolean traceEnabled;
	public Logger(GpsMid app){		
		Logger.app=app;
	}
	public Logger(Class c){
		this.source=getClassName(c);		
	}
	public Logger(Class c,int level){
		this.source=getClassName(c);
		this.level=level;		
	}
	public static Logger getInstance(Class c){
		if (app == null){
			return null;
//			throw new Error("not initialized");
		}
		return new Logger(c);
	}
	public static Logger getInstance(Class c,int level){
		if (app == null){
			return null;
//			throw new Error("not initialized");
		}
		return new Logger(c,level);
	}
	public void fatal(String msg){
		if (level >= FATAL) {
			app.log("F["+source + msg);			
			GpsMid.getInstance().alert("Fatal", msg, Alert.FOREVER);
		}
	}
	public void error(String msg){		
		error(msg, false);
	}
	public void error(String msg, boolean silent){		
		if (level >= ERROR) {
			app.log("E["+source + msg);
			if(!silent) {
				GpsMid.getInstance().alert("Error", msg, 5000);			
			}
		}		
	}
	public void exception(String msg, Exception e) {		
		error(msg + " (" + e + ": " + e.getMessage());
		e.printStackTrace();		
	}
	public void silentexception(String msg, Exception e) {		
		error(msg + " (" + e + ": " + e.getMessage(), true);
		e.printStackTrace();		
	}
	public void info(String msg){
		//#mdebug info
		if (level >= INFO && infoEnabled) {
			app.log("I["+source + msg);
		}
		//#enddebug
	}
	public void debug(String msg){
		//#mdebug debug
		if (level >= DEBUG && debugEnabled) {
			app.log("D["+source + msg);
		}
		//#enddebug
	}
	public void trace(String msg){
		//#mdebug debug
		if (level >= TRACE && traceEnabled) {
			app.log("T["+source + msg);
		}
		//#enddebug
	}
	
	private static String getClassName(Class c) {
		if (c != null) {
			String n=c.getName();
			return n.substring(n.lastIndexOf('.')+1, n.length()) + "] ";
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
