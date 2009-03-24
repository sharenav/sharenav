package de.ueller.midlet.gps.tile;
/*
 * GpsMid - Copyright (c) 2007 Harald Mueller james22 at users dot sourceforge dot net
 * 			Copyright (c) 2008 Kai Krueger apm at users dot sourceforge dot net 
 * See Copying
 */

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gps.data.Configuration;
import de.ueller.gps.data.SearchResult;
import de.ueller.gpsMid.mapData.QueueReader;
import de.ueller.midlet.gps.GpsMid;
import de.ueller.midlet.gps.GuiSearch;
import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.Trace;
import de.ueller.midlet.gps.names.Names;
import de.ueller.midlet.gps.names.NumberCanon;

public class SearchNames implements Runnable{

	private Thread processorThread;
	private int foundEntries=0;
	private boolean stopSearch=false;
	private String search;
	private final GuiSearch gui;
	private boolean newSearch=false;
	private boolean appendRes=false;
	protected static final Logger logger = Logger.getInstance(SearchNames.class,Logger.TRACE);

	public SearchNames(GuiSearch gui) {
		super();
		this.gui = gui;
	}

	public void run() {
	
	    try {
		while (newSearch) {
		    doSearch(search);
		    // refresch display to give change to fetch the names
		    for (int i=8;i!=0;i--){
			try {
			    synchronized (this) {
				wait(300);						
			    }
			    if (stopSearch){
					break;
			    } else {
					gui.triggerRepaint();
			    }
			} catch (InterruptedException e) {
			}
		    }
		}
	    } catch (OutOfMemoryError oome ) {
		logger.fatal("SearchNames thread crashed as out of memory: " + oome.getMessage());
		oome.printStackTrace();
	    } catch (Exception e) {
		logger.fatal("SearchNames thread crashed unexpectadly with error " +  e.getMessage());
		e.printStackTrace();
	    }		
	}
	
	private void doSearch(String search) throws IOException {
		try {
			synchronized(this) {
				stopSearch=false;
				if (newSearch) {
					if (!appendRes) {
						gui.clearList();
					}
					newSearch=false;
				}
			}
			
			if (search.length() < 2) {
				synchronized (this) {
					//#debug
					logger.info("Collecting waypoints");
					// we want each time a fresh copy of the waypoint array as they might be loading in the background
					gui.wayPts = Trace.getInstance().gpx.listWayPt();
					final int canonLen = search.length();
					final String cSearch = NumberCanon.canonial(search);
					boolean inserted = false;
					for (int i = 0; i < gui.wayPts.length; i++ ) {
		    			if (gui.showAllWayPts || gui.wayPts[i].displayName.endsWith("*")) {
		    				if (
		    					canonLen == 0
		    					||
		    					NumberCanon.canonial(gui.wayPts[i].displayName.substring(0, 1)).equals( cSearch )
		    				) {
				    			SearchResult sr = new SearchResult();
				    			sr.lat = gui.wayPts[i].lat;
				    			sr.lon = gui.wayPts[i].lon;	    			
				    			sr.nameIdx = i; 
				    			gui.insertWptSearchResultSortedByNameOrDist(gui.wayPts, sr);
				    			inserted = true;
		    				}    					
		    			}
		    		}
					if (!inserted) {
						gui.state = GuiSearch.STATE_MAIN;
						gui.setTitle();
					} else {
						gui.state = GuiSearch.STATE_FAVORITES;
						gui.displayReductionLevel = 0;
						gui.setTitle();
						return;
					}
				}
			}
			
			if (search.length() < 2)
				return;
			
			//#debug
			logger.info("Searching for " + search);
			String fn=search.substring(0,2);
			String compare=search.substring(2);
			StringBuffer current=new StringBuffer();
			
			String fileName = "/s"+fn+".d";
//			System.out.println("open " +fileName);
			InputStream stream;
			try {
				 stream = Configuration.getMapResource(fileName);
				 if (stream == null) {
					 /**
					  * This presumably means, that the combination of two letters simply
					  * doesn't exist in the map. So just return and do nothing.
					  */
					 return;
				 }
			} catch (IOException e) {
				/**
				 * This presumably means, that the combination of two letters simply
				 * doesn't exist in the map. So just return and do nothing.
				 */
				return;				
			}			
			DataInputStream ds=new DataInputStream(stream);
			
			int pos=0;
			int type = 0;
			/**
			 * InputStream.available doesn't seem to reliably give a correct value
			 * depending on what type of InputStream we actually get.
			 * Instead we will continue reading until we receive a EOFexpetion.
			 */
			while (true){
				
				if (stopSearch){
					ds.close();					
					return;
				}
				try {					
					type = ds.readByte();
				} catch (EOFException eof) {
					//Normal way of detecting the end of a file
					ds.close();
					return;
				}
				/**
				 * Encoding of delta plus flags in bits:
				 * 10000000 Sign bit
				 * 01100000 long 
				 * 01000000 int
				 * 00100000 short
				 * 00000000 byte
				 * 000xxxxx delta 
				 */				
				//System.out.println("type = " + type);
				int sign=1;
				if ((type & 0x80) != 0) {
					sign = -1;
				}
				int delta = (type & 0x1f) * sign;				
				//System.out.println("type = " + type);
				int entryType=(type & 0x60);
				if (delta > Byte.MAX_VALUE)
					delta -= Byte.MAX_VALUE;
				pos+=delta;				
				current.setLength(pos);
//				System.out.println("pos=" + pos + "  delta="+delta);
				long value=0;
				switch (entryType){
				case 0:
					value=ds.readByte();
//					System.out.println("read byte");
					break;
				case 0x20:
					value=ds.readShort();
//					System.out.println("read short");
					break;
				case 0x40:
					value=ds.readInt();
//					System.out.println("read int");
					break;
				case 0x60:
					value=ds.readLong();
//					System.out.println("read long");
					break;
				}
				current.append(""+value);
//				System.out.println("test " + current);
				int idx = Names.readNameIdx(ds);
				if (!current.toString().startsWith(compare)){
					idx=-1;
				}
				type = ds.readByte();
								
				while (type != 0){					
					if (stopSearch){
						ds.close();						
						return;
					}					
					byte isInCount=ds.readByte();
					int[] isInArray=null;
					if (isInCount > 0 ){
						isInArray=new int[isInCount];
						for (int jj=isInCount;jj--!=0;){
							isInArray[jj]= Names.readNameIdx(ds);							
						}
					}
					float lat=ds.readFloat();
					float lon=ds.readFloat();
					if (idx != -1){
						SearchResult sr=new SearchResult();
						sr.nameIdx=idx;
						sr.type=(byte) type;
						sr.lat=lat;
						sr.lon=lon;
						sr.nearBy=isInArray;
						// when there are no nearBys we add the distance so it will be shown by the GUI
						if (isInArray == null) {
							gui.addDistanceToSearchResult(sr);
						}
						if (newSearch) {
							if (!appendRes) {						
								gui.clearList();
							}
							newSearch=false;
						}
						gui.addResult(sr);
						foundEntries++;
						if (foundEntries > 50) {
							//#debug info
							logger.info("Found 50 entries. Thats enough, stoping further search");
							ds.close();
							return;
						}
						//System.out.println("found " + current +"(" + idx + ") type=" + type);
					}
					type = ds.readByte();
				}
			}			
		} catch (NullPointerException e) {
			logger.exception("Null pointer exception in SearchNames: ", e);			
		}
	}
	
	public void shutdown(){
		stopSearch=true;
	}
	
	/**
	 * search for a canonicalised name and return a list of results through callbacks
	 * This call blocks until the search has finished.
	 * @param search
	 */
	public synchronized void appendSearchBlocking(String search) {
		logger.info("search for  " + search);
		stopSearch=true;
		newSearch=true;
		appendRes = true;
		foundEntries=0;
		try {
			doSearch(search);
		} catch (IOException ioe) {
			//Do nothing
		}
	}

	public synchronized void search(String search){
		//#debug
		logger.info("search for  " + search);
		stopSearch=true;
		newSearch=true;
		appendRes = false;
		foundEntries=0;
		this.search=search;
		if (processorThread == null || !processorThread.isAlive()) {
			processorThread = new Thread(this);
			processorThread.setPriority(Thread.MIN_PRIORITY+1);
			processorThread.start();
			//#debug info
			logger.info("started search thread");
		}		
	}


}

