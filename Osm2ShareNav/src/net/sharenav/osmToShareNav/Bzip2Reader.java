/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2007 Harald Mueller
 */
package net.sharenav.osmToShareNav;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.tools.bzip2.CBZip2InputStream;

/**
 * @author hmueller
 *
 */
public class Bzip2Reader extends PipedInputStream implements Runnable {

	private Thread	processorThread;
	private static boolean fromJar=true;
	private final InputStream is;
	
	public Bzip2Reader(InputStream is){
		super();
		this.is = is;
			processorThread = new Thread(this,"BZi2Reader");
			processorThread.setPriority(Thread.NORM_PRIORITY+1);
			processorThread.start();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		int ch;
		byte[] buffer = new byte[512];
		int bytes_read;
		try {
			PipedOutputStream po=new PipedOutputStream(this);
			is.read();
			is.read();
			CBZip2InputStream cis = new CBZip2InputStream(new BufferedInputStream(is,10240));
			 for(;;) {
		            bytes_read = cis.read(buffer);
		            if (bytes_read == -1) { po.close(); return; }
		            po.write(buffer, 0, bytes_read);
		        }
			 } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
