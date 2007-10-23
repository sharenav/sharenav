package de.ueller.midlet.gps.data;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
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

public class Gpx extends PersistEntity implements Runnable {
	
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
			StreamConnection conn = (StreamConnection) Connector.open(url);
			ClientSession session = (ClientSession)Connector.open(url);
	        HeaderSet headers = session.createHeaderSet();
	        headers.setHeader(HeaderSet.NAME, "export.gpx");
	        headers.setHeader(HeaderSet.TYPE, "text");
//	        headers.setHeader(HeaderSet.LENGTH, new Long(imageData.length));
	        Operation operation;
	        operation = session.put(headers);
	        OutputStream outputStream = operation.openOutputStream();

//			OutputStream outputStream = conn.openOutputStream();
			write(outputStream,"<?xml version='1.0' encoding='UTF-8'?>\r\n");
			write(outputStream,"<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n");
			write(outputStream,"<trk>\r\n");
			write(outputStream,"<trkseg>\r\n");

			byte [] result;
			for (int i = 1; i <= database.getNumRecords(); i++)

			  try {
			    if ((result = database.getRecord(i)) != null) {
			       DataInputStream bi = getByteInputStream(result);
			       write(outputStream,"<trkpt lat='"+ bi.readFloat() + "' lon='" + bi.readFloat() +"' />\r\n");
			    }
			  } catch (RecordStoreException e) {
			  }	
			  write(outputStream,"</trkseg>\r\n</trk>\r\n</gpx>\r\n");
			  write(outputStream,"\r\n");
			  outputStream.flush();
			  outputStream.close();
			  conn.close();
			  database.closeRecordStore();
		      int code = operation.getResponseCode();
		        if (code == ResponseCodes.OBEX_HTTP_OK) {
					RecordStore.deleteRecordStore("GPX");
		        } else {
		        	System.out.println("problem in File Opex " + code);
		        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreNotOpenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
