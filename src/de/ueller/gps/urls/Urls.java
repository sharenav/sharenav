package de.ueller.midlet.gps.urls;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
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

import de.ueller.gps.data.Configuration;
import de.ueller.gps.tools.intTree;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;

import de.ueller.midlet.gps.data.MapName;
import de.ueller.midlet.gps.tile.StringEntry;

public class Urls implements Runnable {
//	#debug
	private final static Logger logger = Logger.getInstance(Urls.class, Logger.TRACE);
	private intTree queue2 = new intTree();	
	private intTree addQueue2 = new intTree();
	private int[] startIndexes = null;
	boolean shutdown = false;
	boolean cleanup = false;
	private intTree stringCache = new intTree();
	private Thread processorThread;
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
				// change to give cleanup a chance
				if (addQueue2.size() != 0) {
					synchronized (addQueue2) {					
						queue2.clone(addQueue2);
						addQueue2.removeAll();					
					}
					readData(queue2);
				}
				if (cleanup) {
					cleanupStringCache();
				}
			}
		} catch (Exception e) {
			logger.fatal("Urls thread crashed unexpectedly with error " +  e.getMessage() + " at"  +e.toString());
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

	private void readData(intTree queue) throws IOException {
		int idx = queue.popFirstKey();
		int fid = 0;
		InputStream is = null;
		int count = 0;
		int actIdx = 0;

		while (idx != -1) {
			//#debug debug
			logger.debug("Looking up url " + idx);
			/* Lookup in which urls file the entry is contained */
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
				break;
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
					if (se == null) {
						/*
						 * We might have dropped the cache in between for low memory,
						 * in this case just read the entry now.
						 */
						se = new StringEntry(null);
						stringCache.put(idx, se);
						se.count = 4;
					}
					se.name = url.toString();
					
					if (queue.size() != 0) {
						idx = queue.popFirstKey();
					} else {
						idx = -1;
						break files;
					}
				}
				actIdx++;
			}
			ds.close();
		}
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
		if (idx < 0) {
			return null;
		}
		StringEntry ret = (StringEntry) stringCache.get(idx);
		if (ret != null) {
			ret.count = 4;
			return ret.name;
		}
		StringEntry newEntry = new StringEntry(null);
		stringCache.put(idx, newEntry);
		newEntry.count = 4;
		addQueue2.put(idx, null);
		notify();
		return null;
	}
	
	// FIXME consider removing, probably not needed
	/**
	 * Search linearly through all urls in the urls files and do a comparison
	 * to the search string. If the search string is contained somewhere in the url,
	 * return it as a hit. The comparison is done in lower case and so is case insensitive 
	 * @param snippet
	 * @return a Vector of Strings containing the url.
	 */
	public Vector fulltextSearch (String snippet) {
		logger.info("Beginning fulltext search for " + snippet);
		Vector hits = new Vector();
		int count;		
		try {
			for (int fid = 0; fid < (startIndexes.length - 1); fid++) {
				InputStream is = Configuration.getMapResource("/urls-" + fid + ".dat");
				count = startIndexes[fid + 1] - startIndexes[fid];
				if (is == null) {
					break;
				}
				DataInputStream ds = new DataInputStream(is);

				int pos = 0;
				StringBuffer url = new StringBuffer();
				StringEntry bufferSe = new StringEntry(null);
				//Search through all urls in the the given file
				//as we can only read linearly in this file 
				for (int l = 0; l < count; l++) {
					pos = readNextWord(ds, pos, url, bufferSe);
					String fullUrl = url.toString().toLowerCase();
					if (fullUrl.indexOf(snippet) > -1) {
						//#debug debug
						logger.debug("found fulltext match: " + fullUrl);
						hits.addElement(fullUrl);
					}
				}
				ds.close();
			}
			//#debug
			logger.info("Finished fulltext search. Found " + hits.size() + " hits");
		} catch (IOException e) {
			logger.exception("Could not perform fulltext search", e);
		}
		return hits;
	}
	
	private void cleanupStringCache() {
		//#debug info
		logger.info("cleanup urlsCache " + stringCache.size());
		boolean needsFreeing = GpsMid.getInstance().needsFreeingMemory();
		
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
