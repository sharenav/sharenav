package de.ueller.midlet.gps.data;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;
import javax.microedition.rms.RecordStoreNotFoundException;
import javax.microedition.rms.RecordStoreNotOpenException;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import de.ueller.gps.data.Position;
import de.ueller.midlet.gps.Logger;

public class Gpx extends PersistEntity implements Runnable {
	
	private final static Logger logger=Logger.getInstance(Gpx.class,Logger.DEBUG);
	private RecordStore database;
	public int recorded=0;
	private Thread processorThread=null;
	private String url=null;

	public Gpx() throws RecordStoreFullException, RecordStoreNotFoundException, RecordStoreException {
		database = RecordStore.openRecordStore("GPX", true);
	}

	public Gpx(String url) throws RecordStoreFullException, RecordStoreNotFoundException, RecordStoreException {
		database = RecordStore.openRecordStore("GPX", false);
		this.url=url;
		processorThread = new Thread(this,"Names");
		processorThread.setPriority(Thread.MIN_PRIORITY);
		processorThread.start();
	}
	
	public PositionMark addPosition(Position p) throws IOException, RecordStoreNotOpenException, RecordStoreFullException, RecordStoreException{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		DataOutputStream ds = new DataOutputStream(os);
		ds.writeFloat(p.latitude);
		ds.writeFloat(p.longitude);
		ds.writeFloat(p.altitude);
		ds.writeLong(p.date.getTime());
		ds.writeFloat(p.speed);
		byte[] byteArray = os.toByteArray();
		database.addRecord(byteArray, 0, byteArray.length);
		ds.close();
		recorded++;
		if (recorded % 60 == 0){
			return new PositionMark(p.latitude,p.longitude);
		} else {
			return null;
		}
	}

	public void close() {
		try {
			database.closeRecordStore();
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static boolean isGpxDataThere(){
		String[] r  = RecordStore.listRecordStores();
		for (int i=0; i<r.length;i++){
			if (r.equals("GPX"))
				return true;
		}
		return false;
	}

	
	
	public static void transfer(String gpxUrl) {
		try {
			new Gpx(gpxUrl);
		} catch (RecordStoreFullException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void run() {
		try {
			//Convert the data stored in the recordStore into a GPX file
			//Cache it completely in a ByteArrayOutputStream, as we need to
			//find out the file size of the complete file to send.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			write(baos,"<?xml version='1.0' encoding='UTF-8'?>\r\n");
			write(baos,"<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n");
			write(baos,"<trk>\r\n");
			write(baos,"<trkseg>\r\n");
			byte [] result;
			for (int i = 1; i <= database.getNumRecords(); i++) {
				try {
					if ((result = database.getRecord(i)) != null) {
						DataInputStream bi = getByteInputStream(result);
						write(baos,"<trkpt lat='"+ bi.readFloat() + "' lon='" + bi.readFloat() +"' />\r\n");
					}
				} catch (RecordStoreException e) {
					logger.error("RSE: " + e.getMessage());
				}
			}
			write(baos,"</trkseg>\r\n</trk>\r\n</gpx>\r\n");
			write(baos,"\r\n");
			database.closeRecordStore();

			logger.trace("Starting to send a GPX file, about to open a connection to" + url);			
			ClientSession session = (ClientSession)Connector.open(url);						
			HeaderSet headers = session.createHeaderSet();	        
			session.connect(headers);
			logger.debug("Connected");
			headers.setHeader(HeaderSet.NAME, "export.gpx");
			headers.setHeader(HeaderSet.TYPE, "text");
			headers.setHeader(HeaderSet.LENGTH, new Long(baos.size()));
			Operation operation;
			operation = session.put(headers);			
			OutputStream outputStream = operation.openOutputStream();
			logger.info("Writing file of length: " + baos.size());
			outputStream.write(baos.toByteArray());			
			outputStream.flush();
			outputStream.close();			
			session.close();
			
			int code = operation.getResponseCode();
			if (code == ResponseCodes.OBEX_HTTP_OK) {
				logger.info("Successfully transfered file");
				RecordStore.deleteRecordStore("GPX");
			} else {
				System.out.println("problem in File Opex " + code);
			}
		} catch (IOException e) {			
			e.printStackTrace();
			logger.error("IOE: " + e.getMessage());			
		} catch (RecordStoreNotOpenException e) {
			e.printStackTrace();			
			logger.error("RSNOE: " + e.getMessage());
		} catch (RecordStoreException e) {
			e.printStackTrace();			
			logger.error("RSE: " + e.getMessage());
		} catch (Exception ee) {
			ee.printStackTrace();
			logger.error("E: " + ee.getMessage());
		}
	}

	private void write(OutputStream outputStream, String string) throws IOException {
		outputStream.write(string.getBytes());
		
	}
	
	public static void delete(){
		try {
			RecordStore.deleteRecordStore("GPX");
		} catch (RecordStoreNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
