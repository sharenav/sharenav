/**
 * This file is part of OSM2ShareNav 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * Copyright (C) 2008  Kai Krueger
 * 
 */
package net.sharenav.osmToShareNav;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 
 *
 */
public class TeeInputStream extends InputStream {
	InputStream in;
	OutputStream out;

	TeeInputStream(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}
	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		int data = in.read();
		out.write(data);
		return data;
	}
	
	public int read(byte []  data) throws IOException {
		int noRead = in.read(data);
		out.write(data,0,noRead);
		return noRead;
	}
	public int read(byte[] data, int off, int len) throws IOException {
		int noRead = in.read(data, off, len);
		if (noRead > 0) {
			out.write(data,off,noRead);
		}
		return noRead;
	}
	
	public void close() throws IOException {
		out.close();
		in.close();
	}
}
