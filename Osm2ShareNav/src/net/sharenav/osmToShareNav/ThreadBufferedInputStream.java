/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 */
package net.sharenav.osmToShareNav;

import java.io.IOException;
import java.io.InputStream;

public class ThreadBufferedInputStream extends InputStream implements Runnable {
	private InputStream is;
	private byte[][] buffer;
	private int bufferReadIdx;
	private int bufferWriteIdx;
	private int readIdx;
	private int writeIdx;
	private boolean writeSwappReady;
	private boolean readSwappReady;
	private int readLength;
	private Thread workerThread;
	private boolean eof;
	private boolean eofIn;
	
	public ThreadBufferedInputStream(InputStream in) {
		is = in;
		buffer = new byte[2][];
		buffer[0] = new byte[1024 * 1024];
		buffer[1] = new byte[1024 * 1024];
		writeSwappReady = false;
		readSwappReady = false;
		bufferReadIdx = 0;
		bufferWriteIdx = 1;
		readLength = 0;
		readIdx = 0;
		writeIdx = 0;
		eof = false;
		eofIn = false;
		workerThread = new Thread(this, "ThreadBuffered-reader");
		workerThread.start();
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		if (readLength <= readIdx ) {
			synchronized (this) {
				if (eof) {
					return -1;
				}
				readSwappReady = true;
				if (writeSwappReady) {
					swapBuffers();
				}
				else {
					try {
						wait();						
					} catch (InterruptedException e) {
						System.out.println("Something went horribly wrong " + e.getMessage());
						System.exit(3);
					}
				}
			}
		}
		byte res = buffer[bufferReadIdx][readIdx++];
		return res;
	}
	
	public int read(byte[] buf) {
		if (readLength <= readIdx ) {
			synchronized (this) {
				if (eof) {
					return -1;
				}
				readSwappReady = true;				
				if (writeSwappReady) {
					swapBuffers();
				} else {
					try {
						wait();						
					} catch (InterruptedException e) {
						System.out.println("Something went horribly wrong " + e.getMessage());
						System.exit(3);
					}
				}
			}
		}
		int noRead = buf.length;
		if (noRead + readIdx > readLength) {
			noRead = readLength - readIdx;
		}
		System.arraycopy(buffer[bufferReadIdx], readIdx, buf, 0, noRead);
		readIdx += noRead;		
		return noRead;
	}
	
	public int read(byte[] buf, int off, int len) {
		if (readLength <= readIdx ) {
			synchronized (this) {
				readSwappReady = true;
				if (eof) {
					return -1;
				}
				if (writeSwappReady)
					swapBuffers();
				else {
					try {
						wait();
					} catch (InterruptedException e) {
						System.out.println("Something went horribly wrong " + e.getMessage());
						System.exit(3);
					}
				}
			}
		}
		int noRead = len;
		if (noRead + readIdx > readLength) {
			noRead = readLength - readIdx;
		}
		System.arraycopy(buffer[bufferReadIdx], readIdx, buf, off, noRead);
		readIdx += noRead;
		return noRead;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		int noRead = 0;
		while (noRead != -1) {
			try {
				noRead = is.read(buffer[bufferWriteIdx],writeIdx,buffer[bufferWriteIdx].length - writeIdx);
				if (noRead == -1) {
					eofIn = true;					
					//System.out.println("finished reading is");					
				}
			} catch (IOException e) {
				System.out.println("Something went horribly wrong " + e.getMessage());
				System.exit(2);
			}
			
			if (!eofIn) {
				writeIdx += noRead;
			}
			if (buffer[bufferWriteIdx].length <= writeIdx || eofIn) {
				synchronized (this) {
					writeSwappReady = true;
					if (readSwappReady) {
						swapBuffers();
					} else {
						try {
							wait();
						} catch (InterruptedException e) {
							System.out.println("Something went horribly wrong " + e.getMessage());
							System.exit(3);
						}
					}
				}
			}
		}		
	}
	
	private void swapBuffers () {
		readLength = writeIdx;
		readSwappReady = false;
		writeSwappReady = false;
		readIdx = 0;
		writeIdx= 0;
		int tmp = bufferReadIdx;
		bufferReadIdx = bufferWriteIdx;
		bufferWriteIdx = tmp;
		
		if (eofIn) {
			eof = true;
		}
		notifyAll();
	}

}
