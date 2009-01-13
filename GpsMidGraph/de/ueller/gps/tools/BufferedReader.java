package de.ueller.gps.tools;
/*
 * GpsMid - Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 * 
 * this class reimplements the BufferedReader class for j2me.
 */

import java.io.IOException;
import java.io.Reader;

public class BufferedReader extends Reader{

	Reader r;
	char [] buffer;
	int idx;
	int bufferlen;
	StringBuffer sb;
	public BufferedReader(Reader r) {
		this.r = r;
		buffer = new char[1024];
		idx = 0;
		bufferlen = 0;
		sb = new StringBuffer();
	}
	
	public void close() throws IOException {
		r.close();
	}

	
	
	public void mark (int readAheadLimit) throws IOException{
		throw new IOException("mark Not implemented");
	}
	
	public boolean markSupported() {
		return false;
	}
	
	public int read() throws IOException{
		if (idx < bufferlen)
			return buffer[idx++];
		idx = 0;
		bufferlen = r.read(buffer);
		if (idx < bufferlen)
			return buffer[idx++];
		return -1;
	}
	
	public int read(char[] buff, int off, int len) throws IOException {
		int len3 = 0;
		while (len > bufferlen - idx) {
			int len2 = bufferlen - idx;
			System.arraycopy(buffer, idx, buff, off, len2);
			off += len2;
			len -= len2;
			len3 += len2;
			idx = 0;
			bufferlen = r.read(buffer);
			if (bufferlen < 0);
				return len3;
		}
		System.arraycopy(buffer, idx, buff, off, len);
		idx += len;
		return len3;
	}
	
	public String readLine() throws IOException{
		sb.setLength(0);
		while (bufferlen >= 0) {
			if (idx >= bufferlen) {
				idx = 0;
				bufferlen = r.read(buffer);
			}
			for (int i = idx; i < bufferlen; i++) {
				char c = buffer[idx++];
				if (c == '\r') {
					if (idx < bufferlen) {
						if (buffer[idx] == '\n') {
							idx++;
						}
					} else {
						r.read();
					}
					return sb.toString();
				} else if (c == '\n') {
					return sb.toString();
				} else {
					sb.append(c);
				}
			}
		}
		if (sb.length() > 0)
			return sb.toString();
		System.out.println("Left in buffer: " + sb.toString());
		return null;
	}
}
