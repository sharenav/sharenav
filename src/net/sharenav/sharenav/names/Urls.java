package net.sharenav.sharenav.names;
/*
 * ShareNav - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 * 
 * this class maintains all about Urls. Run in a low proirity, has a request queue and
 * a String cache. All done, to avoid many an frequent memory allocations.
 */

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.sharenav.sharenav.data.Configuration;
import net.sharenav.sharenav.mapdata.QueueReader;
import net.sharenav.sharenav.ui.ShareNav;
import net.sharenav.sharenav.ui.Trace;
import net.sharenav.sharenav.util.StringEntry;

import net.sharenav.util.IntTree;
import net.sharenav.util.Logger;

import de.enough.polish.util.Locale;

public class Urls implements Runnable {
//	#debug
	private final static Logger logger = Logger.getInstance(Urls.class, Logger.TRACE);
	private final IntTree queue2 = new IntTree();	
	private final IntTree addQueue2 = new IntTree();
	private int[] startIndexes = null;
	boolean shutdown = false;
	boolean cleanup = false;
	private final IntTree stringCache = new IntTree();
	private final Thread processorThread;
	boolean isReady = false;

	public Urls() {
		super();
		processorThread = new Thread(this, "Urls");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}

	public void run() {
		try {
			readIndex();
			while (! shutdown) {
				logger.debug("Urls resolver thread looped");
				synchronized (this) {
					try {
						if (addQueue2.size() == 0 && (!cleanup)) {
							wait(60000l);
						}
					} catch (InterruptedException e1) {
//						logger.error("interrupted");
						continue;
					}
				}
				//Sleep for 500ms to give time for several
				//url requests to come in, as this increases
				//efficiency. Should not effect perceived
				//Speed much
				Thread.sleep(500);
				if (cleanup) {
					cleanupStringCache();
				}
			}
		} catch (Exception e) {
			logger.fatal(Locale.get("urls.UrlsThreadCrashed")/*Urls thread crashed unexpectedly with error */ +  e.getMessage() + Locale.get("urls.at")/* at*/  +e.toString());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void stop() {
		shutdown = true;
	}

	public synchronized void cleanup() {
		cleanup = true;
		notify();
	}

	private void readIndex() throws IOException {
		InputStream is = Configuration.getMapResource("/urls-idx.dat");
//		logger.info("read urls-idx");
		DataInputStream ds = new DataInputStream(is);

		int[] urlIdxs = new int[255];
		short count = 0;
		urlIdxs[count++] = 0;
		while (true) {
			try {
				urlIdxs[count++] = ds.readInt();
			} catch (EOFException eofe) {
				ds.close();
				break;
			}
			if (count >= urlIdxs.length) {
				int[] tmp = new int[count + 255];
				System.arraycopy(urlIdxs, 0, tmp, 0, urlIdxs.length);
				urlIdxs = tmp;
			}
		}
		count--;
		startIndexes = new int[count];
		System.arraycopy(urlIdxs, 0, startIndexes, 0, count);		
		isReady = true;
	}
	
	public synchronized boolean isReady() {
		while (!isReady) {
			try {
				wait(2000);
			} catch (InterruptedException e) {
			}
		}
		return true;
	}

	public String getFirstWord(int fid) {
		try {
//			System.out.println("readFirstWord: /urls-" + fid + ".dat");
			InputStream is = Configuration.getMapResource("/urls-" + fid + ".dat");
			DataInputStream ds = new DataInputStream(is);
			ds.readByte();
			String firstWord = ds.readUTF();
			ds.close();
			return firstWord;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ("");
	}

	private int readNextWord(DataInputStream ds, int pos, StringBuffer url, StringEntry se) throws IOException {
		int delta;
		try { 
			delta = ds.readByte();
		} catch (EOFException eofe) {
			url.setLength(0);
			return -1;
		}
		pos += delta;
		if (pos < 0) {
			return pos;
		}
		url.setLength(pos);
		url.append(ds.readUTF());
		return pos;		
	}

	
	public synchronized String getUrl(int idx) {
		int fid = 0;
		InputStream is = null;
		int count = 0;
		int actIdx = 0;
		String urlret = null;
		//#debug debug
		logger.debug("Looking up url " + idx);
		/* Lookup in which urls file the entry is contained */
		try {
			for (int i = fid; i < startIndexes.length; i++) {
				if (startIndexes[i] > idx) {
					is = Configuration.getMapResource("/urls-" + fid + ".dat");
					count = startIndexes[i] - startIndexes[fid];
					actIdx = startIndexes[fid];
					break;
				}
				fid = i;
			}
			if (is == null) {
//				logger.error("no inputstream found");
				return null;
			}
			DataInputStream ds = new DataInputStream(is);
		
			int pos = 0;
			StringBuffer url = new StringBuffer();
			StringEntry bufferSe = new StringEntry(null);
			//Search through all urls in the the given file
			//as we can only read linearly in this file 
			files:for (int l = 0; l < count; l++) {
				pos = readNextWord(ds, pos, url, bufferSe);
				//logger.info("test Url '" + url + "' at idx:" + actIdx);
				if (actIdx == idx) {
					StringEntry se = (StringEntry) stringCache.get(idx);
					urlret = url.toString();
				}
				actIdx++;
			}
			ds.close();
		} catch (IOException ioe) {
		}
		return urlret;
	}
	private void cleanupStringCache() {
		//#debug info
		logger.info("cleanup urlsCache " + stringCache.size());
		boolean needsFreeing = ShareNav.getInstance().needsFreeingMemory();
		
		for (int i = 0; i < stringCache.capacity(); i++) {
			StringEntry ce = (StringEntry) stringCache.getValueIdx(i);
			if (ce == null) {
				continue;
			}
			if ((ce.count <= 0) && (needsFreeing)) {				
				stringCache.remove(stringCache.getKeyIdx(i));
			} else {
				ce.count--;
			}
		}
		cleanup = false;
	}

	public int getUrlCount() {
		return stringCache.size();
	}
	
	public void dropCache() {
		stringCache.removeAll();
	}

	public static final int readUrlIdx(DataInputStream ds) {		
		int idx = -1;
		try {			
			idx = ds.readShort();			
			if (idx < 0) {
				idx = idx & 0x7fff;
				int idx2 = ds.readShort() & 0xffff;				
				idx = (idx << 16 | idx2) & 0x7fffffff;				
			}			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return idx;
	}
	
}
