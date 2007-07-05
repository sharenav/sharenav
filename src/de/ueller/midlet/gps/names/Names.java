package de.ueller.midlet.gps.names;
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
import de.ueller.midlet.gps.tile.QueueReader;
import de.ueller.midlet.gps.tile.StringEntry;

public class Names implements Runnable {
//	#debug
	private final static Logger logger=Logger.getInstance(Names.class,Logger.TRACE);
	private Vector queue=new Vector();
	private Vector addQueue=new Vector();
	private short[] startIndexes=null;
//	private String[] startWords=null;
//	private String search=null;
	boolean shutdown=false;
	boolean cleanup=false;
	private Hashtable stringCache=new Hashtable(100);
	private Thread processorThread;
	boolean isReady=false;

	
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
//				logger.trace("cache has " + stringCache.size() + " entries ");				
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
		InputStream is = QueueReader.openFile("/names-idx.dat");
//		logger.info("read names-idx");
		DataInputStream ds = new DataInputStream(is);

		short[] nameIdxs = new short[255];
		short count=0;
		nameIdxs[count++]=0;
		while (ds.available() > 0) {
			nameIdxs[count++] = ds.readShort();
		}
		startIndexes = new short[count];
//		startWords = new String[count];
		for (int l=count; --l != 0;){
//		for (int l = 0; l < count; l++) {
			startIndexes[l] = nameIdxs[l];
//			if (l < count-1)
//				startWords[l]=getFirstWord(l);
		}
//		logger.info("read names-idx ready");
		isReady=true;
	}
	
	public synchronized boolean isReady(){
		while (!isReady){
			try {
				wait(2000);
			} catch (InterruptedException e) {
			}
		}
		return true;
	}
	public String getFirstWord(int fid){
		try {
//			System.out.println("readFirstWord: /names-" + fid + ".dat");
			InputStream is = QueueReader.openFile("/names-" + fid + ".dat");
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
//		for (int i=1;i < startIndexes.length;i++){
		for (int i=startIndexes.length-1;i>=0;i--){
//			logger.info("floop index["+ i +"]=" + startIndexes[i] + " idx=" + idx);
			if (startIndexes[i] < idx.shortValue()){
				is=QueueReader.openFile("/names-" + (i) + ".dat");
//				logger.trace("open names-"+(i) + " startIdx=" + startIndexes[i] + "-" + startIndexes[i+1]);
				count=startIndexes[i+1]-startIndexes[i];
				actIdx=startIndexes[i];
				fid=i;
				break;
			}
		}
		while (idx != null){
			for (int i=fid;i < startIndexes.length;i++){
//				logger.info("loop index["+ i +"]=" + startIndexes[i] + " idx=" + idx);
				if (startIndexes[i] > idx.shortValue()){
//					is = QueueReader.openFile("/names-" + fid + ".dat");
					is=QueueReader.openFile("/names-" + fid + ".dat");
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
//					se.isIn=new Short(bufferSe.isIn.shortValue());
//					getName(se.isIn);
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
//			short idx=ds.readShort();
//			System.out.println("nextName " + name + " idx=" + idx);
	//		se.isIn=new Short(idx);
			return pos;
		}
		name.setLength(0);
		return -1;
	}

	
	public String getName(Short idx){
		if (idx==null)
			return null;
		if (idx.shortValue() <0) {
			return null;
		}
		StringEntry ret=(StringEntry) stringCache.get(idx);
		if (ret != null) {
			ret.count=4;
//			logger.info("found Name '" + ret.name + "' for idx:" + idx);
//			String nameIn=getName(ret.isIn);
//			return (nameIn != null)? ret.name + ", " + nameIn : ret.name;
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
			is = QueueReader.openFile("/names-" + l1 + ".dat");
			ds= new DataInputStream(is);
			pos = readNextWord(ds, pos, name,se);
			do {
//				if (name.toString().startsWith("Ezel")){
//					System.out.println(name.toString());	
//				}
				if (s.startsWith(name.toString())){
//					System.out.println(name.toString());	
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
	
	private void addToCache(StringEntry se, Short key){
		stringCache.put(key, se);
		se.count=4;
	}
	

}
