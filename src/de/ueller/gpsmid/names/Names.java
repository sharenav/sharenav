/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See COPYING
 * 
 */

package de.ueller.gpsmid.names;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import de.ueller.gpsmid.data.Configuration;
import de.ueller.gpsmid.ui.GpsMid;
import de.ueller.gpsmid.util.StringEntry;

import de.ueller.util.CancelMonitorInterface;
import de.ueller.util.IntTree;
import de.ueller.util.Logger;

import de.enough.polish.util.Locale;

/**
 * This class maintains all about names. Runs in a low priority, has a request queue and
 * a string cache. All done to avoid many and frequent memory allocations.
 */
public class Names implements Runnable {
//	#debug
	private final static Logger logger = Logger.getInstance(Names.class, Logger.TRACE);
	private final IntTree queue2 = new IntTree();	
	private final IntTree addQueue2 = new IntTree();
	private int[] startIndexes = null;
	boolean shutdown = false;
	boolean cleanup = false;
	private final IntTree stringCache = new IntTree();
	private final Thread processorThread;
	boolean isReady = false;

	public Names() {
		super();
		processorThread = new Thread(this, "Names");
		/* processorThread.setPriority(Thread.MIN_PRIORITY);
		 * has been changed on 17-01-2011 to Thread.NORM_PRIORITY - 2
		 * for faster search results
		 */
		processorThread.setPriority(Thread.NORM_PRIORITY - 2);
		processorThread.start();
	}

	public void run() {
		try {
			readIndex();
			while (! shutdown) {
				logger.debug("Names resolver thread looped");
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
				//name requests to come in, as this increases
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
			logger.fatal(Locale.get("names.NamesThreadCrashedWith")/*Names thread crashed unexpectedly with error */ + 
				     e.getMessage() + Locale.get("names.at")/* at */ + e.toString());
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
		InputStream is = Configuration.getMapResource("/names-idx.dat");
//		logger.info("read names-idx");
		DataInputStream ds = new DataInputStream(is);

		int[] nameIdxs = new int[255];
		short count = 0;
		nameIdxs[count++] = 0;
		while (true) {
			try {
				nameIdxs[count++] = ds.readInt();
			} catch (EOFException eofe) {
				ds.close();
				break;
			//#if polish.android
			} catch (IOException ioe) {
				ds.close();
				break;
			} catch (Exception e) {
				ds.close();
				break;
			//#endif
			}
			if (count >= nameIdxs.length) {
				int[] tmp = new int[count + 255];
				System.arraycopy(nameIdxs, 0, tmp, 0, nameIdxs.length);
				nameIdxs = tmp;
			}
		}
		count--;
		startIndexes = new int[count];
		System.arraycopy(nameIdxs, 0, startIndexes, 0, count);		
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

//	public String getFirstWord(int fid) {
//		try {
////			System.out.println("readFirstWord: /names-" + fid + ".dat");
//			InputStream is = Configuration.getMapResource("/names-" + fid + ".dat");
//			DataInputStream ds = new DataInputStream(is);
//			ds.readByte();
//			String firstWord = ds.readUTF();
//			ds.close();
//			return firstWord;
//		} catch (IOException e) {
//			// TODO Should the user be alerted or does this never happen?
//			e.printStackTrace();
//		}
//		return ("");
//	}

	private void readData(IntTree queue) throws IOException {
		int idx = queue.popFirstKey();
		int fid = 0;
		InputStream is = null;
		int count = 0;
		int actIdx = 0;

		while (idx != -1) {
			//#debug debug
			logger.debug("Looking up name " + idx);
			/* Lookup in which names file the entry is contained */
			for (int i = fid; i < startIndexes.length; i++) {
				if (startIndexes[i] > idx) {
					is = Configuration.getMapResource("/names-" + fid + ".dat");
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
			StringBuffer name = new StringBuffer();
			StringEntry bufferSe = new StringEntry(null);
			//Search through all names in the the given file
			//as we can only read linearly in this file 
			files:for (int l = 0; l < count; l++) {
				pos = readNextWord(ds, pos, name, bufferSe);
				//logger.info("test Name '" + name + "' at idx:" + actIdx);
				if (actIdx == idx) {
					StringEntry se = (StringEntry) stringCache.get(idx);
					if (se == null) {
						/* We might have dropped the cache in between for low memory,
						 * in this case just read the entry now.
						 */
						se = new StringEntry(null);
						stringCache.put(idx, se);
						se.count = 4;
					}
					se.name = name.toString();
					
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

	private int readNextWord(DataInputStream ds, int pos, StringBuffer name, StringEntry se) throws IOException {
		int delta;
		try { 
			delta = ds.readByte();
		} catch (EOFException eofe) {
			name.setLength(0);
			return -1;
		//#if polish.android
		} catch (IOException ioe) {
			name.setLength(0);
			return -1;
		} catch (Exception e) {
			name.setLength(0);
			return -1;
		//#endif
		}
		pos += delta;
		if (pos < 0) {
			return pos;
		}
		name.setLength(pos);
		try { 
			name.append(ds.readUTF());
		} catch (EOFException eofe) {
			// FIXME perhaps remove this after a while since 2011-05-26,
			// was a catch for a bug in handling of long names in Osm2GpsMid and GpsMid (byte for length)
			// The bug was triggered at least by route relation expansion code which created
			// long way names for route relations with rich-style
			System.out.println("A bug occurred: error reading name in Names, name=" + name);
			name.setLength(0);
			return -1;
		}
		return pos;		
	}

	
	public synchronized String getName(int idx) {
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
	
	/**
	 * Search linearly through all names in the names files and do a comparison
	 * to the search string. If the search string is contained somewhere in the name,
	 * return it as a hit. The comparison is done in lower case and so is case insensitive 
	 * @param snippet
	 * @return a Vector of Strings containing the name.
	 */
	public Vector fulltextSearch (String snippet, CancelMonitorInterface cmi) {
		logger.info("Beginning fulltext search for " + snippet);
		Vector hits = new Vector();
		int count;		
		try {
			for (int fid = 0; fid < (startIndexes.length - 1); fid++) {
				if(cmi != null) {
					if (cmi.monitorIsCanceled()) {
						return hits;
					}
				}
				InputStream is = Configuration.getMapResource("/names-" + fid + ".dat");
				count = startIndexes[fid + 1] - startIndexes[fid];				
				if (is == null) {
					break;
				}
				DataInputStream ds = new DataInputStream(is);

				int pos = 0;
				StringBuffer name = new StringBuffer();
				StringEntry bufferSe = new StringEntry(null);
				//Search through all names in the the given file
				//as we can only read linearly in this file 
				for (int l = 0; l < count; l++) {
					pos = readNextWord(ds, pos, name, bufferSe);
					String fullName = name.toString().toLowerCase();
					if (fullName.indexOf(snippet) > -1) {
						//#debug debug
						logger.debug("found fulltext match: " + fullName);
						hits.addElement(fullName);
					}
				}
				ds.close();
			}
			//#debug
			logger.info("Finished fulltext search. Found " + hits.size() + " hits");
		} catch (IOException e) {
			logger.exception(Locale.get("names.CouldNotPerformFulltextSearch")/*Could not perform fulltext search*/, e);
		}
		return hits;
	}
	
	private void cleanupStringCache() {
		//#debug info
		logger.info("cleanup namesCache " + stringCache.size());
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
	
	public int getNameCount() {
		return stringCache.size();
	}
	
	public void dropCache() {
		stringCache.removeAll();
	}
	
	public static final int readNameIdx(DataInputStream ds) {		
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
	
/*	
 * This function is currently unused. Might reintroduce it later
 * 
 * public Short[] search(String s) throws IOException {	
		StringBuffer name = new StringBuffer();
		InputStream is;
		DataInputStream ds;
		int pos = 0;
		Short[] posibilities = new Short[20];
		int filledPosi = 0;
		// suche file durch alle files
		char firstChar = s.charAt(0);
		StringEntry se = new StringEntry(null);
		for (int l1 = 0; (l1 < startIndexes.length && filledPosi<20); l1++) {
//			if (firstChar > startWords[l1 + 1].charAt(0))
//				continue;
			short idx = startIndexes[l1];
			pos = 0;
			is = QueueReader.openFile("/names-" + l1 + ".dat");
			ds = new DataInputStream(is);
			pos = readNextWord(ds, pos, name, se);
			do {
//				if (name.toString().startsWith("Ezel")) {
//					System.out.println(name.toString());	
//				}
				if (s.startsWith(name.toString())) {
//					System.out.println(name.toString());	
					Short idxS = new Short(idx);
					se.name = name.toString();
					addToCache(se, idxS);
					getName(se.isIn);
					posibilities[filledPosi++] = idxS;
					se = new StringEntry(null);
					if (filledPosi >= 20) {
						return posibilities;
					}
				}	
				pos = readNextWord(ds, pos, name, se);
			} while (pos >= 0);
		}
		return posibilities;
	}
	
	private void addToCache(StringEntry se, Short key) {
		stringCache.put(key, se);
		se.count = 4;
	}
 */	

}
