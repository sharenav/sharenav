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

public class Names implements Runnable {
//	private final static Logger logger=Logger.getInstance(Names.class,Logger.TRACE);
	private Vector queue=new Vector();
	private Vector addQueue=new Vector();
	private short[] startIndexes=null;
	boolean shutdown=false;
	boolean cleanup=false;
	private Hashtable stringCache=new Hashtable(100);
	private Thread processorThread;

	
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
		InputStream is = getClass().getResourceAsStream("/map/names-idx.dat");
//		logger.info("read names-idx");
		DataInputStream ds = new DataInputStream(is);

		short[] nameIdxs = new short[255];
		short count=0;
		nameIdxs[count++]=0;
		while (ds.available() > 0) {
			nameIdxs[count++] = ds.readShort();
		}
		startIndexes = new short[count];
		for (int l = 0; l < count; l++) {
			startIndexes[l] = nameIdxs[l];
		}
//		logger.info("read names-idx ready");
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
					is = getClass().getResourceAsStream("/map/names-" + fid + ".dat");
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
			files:for (int l=0;l<count;l++){
				int delta=ds.readByte();
				name.setLength(pos+delta);
				name.append(ds.readUTF());
//				logger.info("test Name '" + name + "' at idx:" + actIdx);
				pos+=delta;
				if (actIdx == idx.shortValue()){
					StringEntry se=(StringEntry) stringCache.get(idx);
					se.name=name.toString();
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

}
