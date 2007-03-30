package de.ueller.midlet.gps.tile;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Display;

import de.ueller.midlet.gps.Logger;
import de.ueller.midlet.gps.data.Node;
import de.ueller.midlet.gps.data.Way;


public class DataReader implements Runnable {

	private final SingleTile st;
	private final String	url;
	private Thread	processorThread;
	private final String	root;
//	private final static Logger logger=Logger.getInstance(DataReader.class,Logger.TRACE);

	public DataReader(int fileId,String root,byte zl,SingleTile st){
		super();
		this.url = "/map/t"+zl+fileId+".d";
		this.root = root;
		this.st = st;
		processorThread = new Thread(this,url);
		processorThread.setPriority(Thread.MIN_PRIORITY+1);
		processorThread.start();
//		logger.info("DataReader Thread started " + url);

	}
	public void run() {
		try {
			readData(url);
		} catch (Exception e) {
//			logger.error("Error:" + e.getMessage());
//			e.printStackTrace();
		}

	}

	private void readData(String fileName) throws IOException{

//    	logger.info("open " + url);
		InputStream is = getClass().getResourceAsStream(url);
		if (is == null){
//			logger.error("file inputStream"+url+" not found" );
			st.state=0;
			return;
		}
//    	logger.info("open DataInputStream");
		DataInputStream ds=new DataInputStream(is);
		if (ds == null){
//			logger.error("file DataImputStream "+url+" not found" );
			st.state=0;
			return;
		}
// end open data from JAR
//		logger.info("read Magic code");
		if (ds.readByte()!=0x54){
//			logger.error("not a MapMid-file");
			throw new IOException("not a MapMid-file");
		}
		int nodeCount=ds.readShort();
		st.nodes = new Node[nodeCount];
//		logger.info("reading " + nodeCount + " nodes");
		for (int i=0; i< nodeCount;i++){
			Node n=new Node(ds);
			st.nodes[i]=n;
		}
		if (ds.readByte()!=0x55){
//			logger.error("Start of Ways not found");
			throw new IOException("MapMid-file corrupt: Nodes not OK");
		}
		int wayCount=ds.readByte();
//		logger.info("reading " + wayCount + " ways");
		if (wayCount < 0) wayCount+=256;
//		logger.info("reading " + wayCount + " ways");
		st.ways = new Way[wayCount];
		for (int i=0; i< wayCount;i++){
			byte flags=ds.readByte();
			if (flags != 128){
//				showAlert("create Way " + i);
			Way w=new Way(ds,flags);
			st.ways[i]=w;
			}
		}
		if (ds.readByte() != 0x56){
			throw new IOException("MapMid-file corrupt: Ways not OK");
		} else {
//			logger.info("ready");
		}
		ds.close();
		
		st.dataReady();
//		logger.info("DataReader ready "+ url + st.nodes.length + " Nodes " + st.ways.length + " Ways" );

//    	}
		
    }
}
