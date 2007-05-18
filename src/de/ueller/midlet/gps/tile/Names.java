package de.ueller.midlet.gps.tile;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net 
 * See Copying
 * 
 * this class maintains all about Names. Run in a low proirity, has a request queue and
 * a String cache. All done, to avoid many an frequent memory allocations.
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import de.ueller.midlet.gps.Logger;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.MapName;

public class Names implements Runnable {
//	private final static Logger logger=Logger.getInstance(Names.class,Logger.TRACE);
	private Vector queue=new Vector();
	private Vector addQueue=new Vector();
	private short[] startIndexes=null;
	private String[] startWords=null;
	private String search=null;
	boolean shutdown=false;
	boolean cleanup=false;
	private Hashtable stringCache=new Hashtable(100);
	private Thread processorThread;
	boolean isReady=false;
	private char letters[][] = {{'1','-','.',','},
							    {'2','A','a','B','b','C','c','ä','Ä'},
							    {'3','D','d','E','e','F','f'},
							    {'4','G','g','H','h','I','i'},
							    {'5','J','j','K','k','L','l'},
							    {'6','M','m','N','n','O','o','Ö','ö'},
							    {'7','P','p','Q','q','R','r','S','s','s'},
							    {'8','T','t','u','u','V','v','Ü','ü'},
							    {'9','W','w','X','x','Y','y','Z','z'},
							    {'*',' ','-'},
							    {'0','+'},
							    {'#'},
							    {'@'},
	};

	
	public Names() {
		super();
		processorThread = new Thread(this,"Names");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}

	public void run() {
		try {
			readIndex();
			while (! shutdown){
//				logger.trace("addQueue has " + addQueue.size() + " requests " + addQueue);
				while (! addQueue.isEmpty()){
					Short ne=(Short) addQueue.firstElement();
					addQueue.removeElementAt(0);
					if (queue.isEmpty()){
						queue.addElement(ne);
					} else {
						Short le=(Short) queue.lastElement();
						if (le.shortValue() < ne.shortValue()){
							queue.addElement(ne);
						} else {
							int idx=0;
							queueLoop:for (Enumeration e=queue.elements();e.hasMoreElements();){
								Short te=(Short) e.nextElement();
								if (te.shortValue() > ne.shortValue()){
									queue.insertElementAt(ne, idx);
									break queueLoop;
								}
								if (te.shortValue() == ne.shortValue()){
									break queueLoop;
								}
								idx++;
							}
						}
					}
				}
				if (! queue.isEmpty()){
//					logger.trace("queue : " + queue);
					Enumeration e=queue.elements();
					readData(e);
					queue.removeAllElements();
				}
				if (cleanup){
					cleanupStringCache();
				}
				synchronized (this) {
					try {
						wait(2000l);
					} catch (InterruptedException e1) {
//						logger.error("interrupted");
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void stop(){
		shutdown=true;
	}
	
	public void cleanup(){
		cleanup=true;
	}

	
	private void readIndex() throws IOException {
		InputStream is = QueueReader.openFile("/map/names-idx.dat");
//		logger.info("read names-idx");
		DataInputStream ds = new DataInputStream(is);

		short[] nameIdxs = new short[255];
		short count=0;
		nameIdxs[count++]=0;
		while (ds.available() > 0) {
			nameIdxs[count++] = ds.readShort();
		}
		count--;
		startIndexes = new short[count];
		startWords = new String[count];
		for (int l = 0; l < count; l++) {
			startIndexes[l] = nameIdxs[l];
			startWords[l]=getFirstWord(l);
		}
//		logger.info("read names-idx ready");
		isReady=true;
	}
	
	public synchronized boolean isReady(){
		while (!isReady){
			try {
				wait(200);
			} catch (InterruptedException e) {
			}
		}
		return true;
	}
	public String getFirstWord(int fid){
		try {
//			System.out.println("readFirstWord: /map/names-" + fid + ".dat");
			InputStream is = QueueReader.openFile("/map/names-" + fid + ".dat");
			DataInputStream ds = new DataInputStream(is);
			ds.readByte();
			String firstWord=ds.readUTF();
			ds.close();
			return firstWord;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ("");
	}
	
	private void readData(Enumeration e) throws IOException{
		Short idx=(Short) e.nextElement();
//		logger.info("search String for idx " + idx);
		// find file
		int fid=0;
		InputStream is=null;
		int count=0;
		int actIdx=0;
		while (idx != null){
			is=null;
			for (int i=fid;i < startIndexes.length;i++){
//				logger.info("loop index["+ i +"]=" + startIndexes[i]);
				if (startIndexes[i] > idx.shortValue()){
//					is = QueueReader.openFile("/map/names-" + fid + ".dat");
					is=QueueReader.openFile("/map/names-" + fid + ".dat");
//					logger.trace("open names-"+fid + " startIdx=" + startIndexes[fid] + "-" + startIndexes[fid+1]);
					count=startIndexes[i]-startIndexes[fid];
					actIdx=startIndexes[fid];
					break;
				}
				fid=i;
			}
			if (is==null){
//				logger.error("no inputstream found");
				break;
			}
			DataInputStream ds=new DataInputStream(is);

			int pos=0;
			StringBuffer name=new StringBuffer();
			StringEntry bufferSe = new StringEntry(null);
			files:for (int l=0;l<count;l++){
				pos = readNextWord(ds, pos, name,bufferSe);
//				logger.info("test Name '" + name + "' at idx:" + actIdx);
				if (actIdx == idx.shortValue()){
					StringEntry se=(StringEntry) stringCache.get(idx);
					se.name=name.toString();
					se.isIn=new Short(bufferSe.isIn.shortValue());
					getName(se.isIn);
//					logger.info("found Name '" + se.name + "' for idx:" + idx);
					if (e.hasMoreElements()){
						idx=(Short) e.nextElement();
					} else {
						idx=null;
						break files;
					}
				}
				actIdx++;
			}
			ds.close();
		}
	}

	private int readNextWord(DataInputStream ds, int pos, StringBuffer name,StringEntry se) throws IOException {
		if (ds.available() > 0){
			int delta=ds.readByte();
			pos+=delta;
			if (pos < 0) return pos;
			name.setLength(pos);
			name.append(ds.readUTF());
			System.out.println(name);
			short idx=ds.readShort();
			se.isIn=new Short(idx);
			return pos;
		}
		name.setLength(0);
		return -1;
	}

	
	public String getName(Short idx){
		if (idx.shortValue() <0) {
			return null;
		}
		StringEntry ret=(StringEntry) stringCache.get(idx);
		if (ret != null) {
			ret.count=4;
//			logger.info("found Name '" + ret.name + "' for idx:" + idx);
			return ret.name;
		}
		StringEntry newEntry = new StringEntry(null);
		stringCache.put(idx, newEntry);
		newEntry.count=4;
//		logger.info("add request for idx:" + idx);
		addQueue.addElement(idx);
		return null;
	}
	
	private void cleanupStringCache(){
//		logger.info("cleanup namesCache " + stringCache.size());
		for (Enumeration e=stringCache.keys();e.hasMoreElements();){
			Short key=(Short) e.nextElement();
			StringEntry ce=(StringEntry) stringCache.get(key);
			if (ce.count == 0){
				stringCache.remove(key);
			} else {
				ce.count--;
			}
		}
//		logger.info("ready cleanup namesCache " + stringCache.size());
		cleanup=false;
	}

	public int getNameCount(){
		return stringCache.size();
	}
	
	public Short[] search(String s) throws IOException{
		StringBuffer name=new StringBuffer();
		InputStream is;
		DataInputStream ds;
		int pos=0;
		Short[] posibilities=new Short[20];
		int filledPosi=0;
		// suche file durch alle files
		char firstChar=s.charAt(0);
		StringEntry se=new StringEntry(null);
		for (int l1=0;(l1 < startIndexes.length && filledPosi<20);l1++){
//			if (firstChar > startWords[l1+1].charAt(0))
//				continue;
			short idx=startIndexes[l1];
			pos=0;
			is = QueueReader.openFile("/map/names-" + l1 + ".dat");
			ds= new DataInputStream(is);
			pos = readNextWord(ds, pos, name,se);
			do {
//				if (name.toString().startsWith("Ezel")){
//					System.out.println(name.toString());	
//				}
				if (isMatch(s, name)){
					System.out.println(name.toString());	
					Short idxS = new Short(idx);
					se.name=name.toString();
					addToCache(se, idxS);
					getName(se.isIn);
					posibilities[filledPosi++]=idxS;
					se=new StringEntry(null);
					if (filledPosi >= 20)
						return posibilities;
				}	
				pos = readNextWord(ds, pos, name,se);
			} while (pos >= 0);
		}
		return posibilities;
	}
	
	private boolean isMatch(String search,StringBuffer found){
		if (found.length() < search.length())
			return false;
		for (int x=0;x<search.length();x++){
			char[] sChars=charList(search.charAt(x));
			char fChar=found.charAt(x);
			boolean isFound=false;
			for (int y=0;y<sChars.length;y++){
				if (fChar==sChars[y]){
					isFound=true;
					break;
				}
			}
			if (! isFound)
				return false;
		}
		return true;
	}
	
	private void addToCache(StringEntry se, Short key){
		stringCache.put(key, se);
		se.count=4;
	}
	
	private char[] charList(char s){
		switch (s){
		case '1':return letters[0];
		case '2':return letters[1];
		case '3':return letters[2];
		case '4':return letters[3];
		case '5':return letters[4];
		case '6':return letters[5];
		case '7':return letters[6];
		case '8':return letters[7];
		case '9':return letters[8];
		case '*':return letters[9];
		case '0':return letters[10];
		case '#':return letters[11];
		}
		return letters[12];
	}

}
