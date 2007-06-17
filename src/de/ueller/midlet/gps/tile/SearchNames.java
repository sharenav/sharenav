package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.ueller.gps.data.SearchResult;
import de.ueller.midlet.gps.GuiSearch;

public class SearchNames implements Runnable{

	private Thread processorThread;
	private boolean inSearch=false;
	private boolean startSearch=false;
	private boolean stopSearch=false;
	private boolean shutdown=false;
	private String search;
	private final GuiSearch gui;
	public SearchNames(GuiSearch gui) {
		super();
		this.gui = gui;
		processorThread = new Thread(this);
		processorThread.setPriority(Thread.MIN_PRIORITY+1);
		processorThread.start();
	}

	public void run() {
		while (! shutdown){
			while (! startSearch){
				synchronized (this) {
				try {
//					System.out.println("wait");
						wait();
				} catch (InterruptedException e) {
				}
				}
				if (shutdown)
					return;
				if (startSearch){
					inSearch=true;
					startSearch=false;
					try {
						doSearch(search);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					inSearch=false;
				}
			}
		}
		
	}
	
	private void doSearch(String search) throws IOException {
		try {
//			System.out.println("search");
			String fn=search.substring(0,2);
			String compare=search.substring(2);
			StringBuffer current=new StringBuffer();
			stopSearch=false;
			String fileName = "/s"+fn+".d";
//			System.out.println("open " +fileName);
			InputStream stream = QueueReader.openFile(fileName);
			if (stream == null){
				System.out.println("file not Found");
				return;
			}
			DataInputStream ds=new DataInputStream(stream);
			int pos=0;
			while (ds.available() > 0){
				if (stopSearch){
					ds.close();
					System.out.println("cancel Search");
					return;
				}
				int type=ds.readByte();
				int sign=1;
				if (type < 0){
					type += 128;
					sign=-1;
				}
//				System.out.println("type = " + type);
				int entryType=(type & 0x60);
				int delta=type & 0x9f;
				delta *=sign;
//				System.out.println("pos=" + pos + "  delta="+delta);
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
				Short idx=null;
				short shortIdx = ds.readShort();
				if (current.toString().startsWith(compare)){
					idx=new Short(shortIdx);
//					System.out.println("match");
				}
				type=ds.readByte();
				while (type != 0){
//					System.out.println("read entryType = " + type);
					if (stopSearch){
						ds.close();
//						System.out.println("cancel Search");
						return;
					}
					float lat=ds.readFloat();
					float lon=ds.readFloat();
					if (idx != null){
						SearchResult sr=new SearchResult();
						sr.nameIdx=idx;
						sr.type=(byte) type;
						sr.lat=lat;
						sr.lon=lon;
						gui.addResult(sr);
						
//						System.out.println("found " + current +"(" + shortIdx + ") type=" + type);
					}
					type=ds.readByte();
				}
			}
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void shutdown(){
		stopSearch=true;
		shutdown=true;
	}

	public void search(String search){
//		System.out.println("search for  " + search);
		stopSearch=true;
		this.search=search;
		startSearch=true;
		synchronized (this) {
			notify();			
		}
	}


}

