/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2009        sk750
 * 
 */

package net.sharenav.osmToShareNav.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class FileTools {

	public static int countFiles(String source) {
		return countFiles(new File(source));
	}
	
	public static int countFiles(File source) {
		int numFiles = 0;
        File[] files = source.listFiles();
        if (files != null) {
	        for (File file : files) {
	            if (file.isDirectory()) {
            		numFiles += countFiles(file);
	            }
	            else {
	            	numFiles += 1;
	            }
	        }
        }
		return numFiles;
	}
	
	
	public static int copyDir(String source, String dest, boolean flat, boolean onlyReplaceExisting) {
    	return copyDir(new File(source), new File(dest), flat, onlyReplaceExisting);    	
    }

	
    public static int copyDir(File source, File dest, boolean flat, boolean onlyReplaceExisting) {
        int numFilesCopied = 0;
        
        File[] files = source.listFiles();
        if (files != null) {
	        dest.mkdirs();
	        for (File file : files) {
	            if (file.isDirectory()) {
	            	if (flat) {
	            		numFilesCopied += copyDir(file, dest, flat, onlyReplaceExisting);
	            	} else {
	            		numFilesCopied += copyDir(file, new File(dest.getAbsolutePath() + System.getProperty("file.separator") + file.getName()), flat, onlyReplaceExisting);	            		
	            	}
	            }
	            else {
	            	numFilesCopied += copyFile(file, new File(dest.getAbsolutePath() + System.getProperty("file.separator") + file.getName()), onlyReplaceExisting);
	            }
	        }
        }
        return numFilesCopied;
    }
    
    public static int copyFile(File source, File dest, boolean onlyReplaceExisting) {
    	if (onlyReplaceExisting && !dest.exists()) {
    		return 0;
    	}
    	if (dest.exists()) {
    		dest.delete();
    	}
        try {        	
        	BufferedInputStream in = new BufferedInputStream(new FileInputStream(source));
	    	BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(dest, true));
	        int bytes = 0;
	        while ((bytes = in.read()) != -1) {
	            out.write(bytes);
	        }
	        in.close();
	        out.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        	return 0;
        }	        
        return 1;
    }
    
    public static FileOutputStream createFileOutputStream(String name) throws FileNotFoundException{
    	File f=new File(name);
    	createPath(f.getParentFile());
    	return new FileOutputStream(f);	
    }
    
	/**
	 * Ensures that the path denoted with <code>f</code> will exist
	 * on the file-system.
	 * @param f File whose directory must exist
	 */
    public static void createPath(File f) {
		if (! f.canWrite()) {
			createPath(f.getParentFile());
		}
		f.mkdir();
	}

} 
