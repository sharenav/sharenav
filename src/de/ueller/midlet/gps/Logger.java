package de.ueller.midlet.gps;
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
	private Class source;
	private int level=ERROR;
	public Logger(GpsMid app){
		Logger.app=app;
	}
	public Logger(Class c){
		this.source=c;
	}
	public Logger(Class c,int level){
		this.source=c;
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
			app.log("F["+getClassName()+"] " + msg);
		}
	}
	public void error(String msg){
		if (level >= ERROR) {
			app.log("E["+getClassName()+"] " + msg);
		}
	}
	public void exception(String msg, Exception e) {
		//#mdebug error
		error(msg + " (" + e + ": " + e.getMessage());
		e.printStackTrace();
		//#enddebug
	}
	public void info(String msg){
		if (level >= INFO) {
			app.log("I["+getClassName()+"] " + msg);
		}
	}
	public void debug(String msg){
		if (level >= DEBUG) {
			app.log("D["+getClassName()+"] " + msg);
		}
	}
	public void trace(String msg){
		if (level >= TRACE) {
			app.log("T["+getClassName()+"] " + msg);
		}
	}
	
	private String getClassName() {
		String n=source.getName();
		return n.substring(n.lastIndexOf('.')+1, n.length());
	}

	public int getLevel() {
		return level;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
}
