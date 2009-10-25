/**
 * This class is written from scratch and implements a double linked list
 * on top of the ZipEntry class.  It does not use java generics and is, at
 * the moment, closely tied to @ZipEntry.  It should not be too much work
 * to have it derive from Object and thus make it useful for other purposes.
 * 
 * Copyright (c) 2009 Christian Müller <cmue81 at \g\m\x dot \d\e>
 * 					 <trendypack at users dot sourceforge dot net>
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 */ 
package net.sourceforge.util.zip;

public class LinkedZipEntry extends ZipEntry {
	static public LinkedZipEntry lru_start;
	static public LinkedZipEntry lru_end;

	public LinkedZipEntry pred;
	public LinkedZipEntry succ;

	public LinkedZipEntry(String name) {
		super(name);
	}

	public LinkedZipEntry(ZipEntry e) {
		super(e);

		if (lru_start == null)
			lru_start = lru_end = this;
		else
			prepend();
	}

	public void moveToStart() {
		if (this.pred != null) {
			this.pred.succ = this.succ;
			if (this.succ != null)
				this.succ.pred = this.pred;
			else
				lru_end = this.pred;

			prepend();
		}
	}
	
	private void prepend() {
		// prepend this to lru_start
		// updating lru_start must be the last operation
		this.pred = null;
		this.succ = lru_start;
		lru_start.pred = this;
		lru_start = this;
		
	}

	/* static public void removeStart() {
		if (lru_start != null) {
			if (lru_start.succ != null) {
				lru_start = lru_start.succ;
				lru_start.pred = null;
			}
			else
				lru_start = lru_end = null;
		}
	} */

	static public LinkedZipEntry removeEnd() {
		LinkedZipEntry le = lru_end;
		
		if (le != null) {
			if (le.pred != null) {
				lru_end = le.pred;
				lru_end.succ = null;
			}
			else
				lru_start = lru_end = null;
		}
		
		return le;
	}
}

