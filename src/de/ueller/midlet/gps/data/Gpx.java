package de.ueller.midlet.gps.data;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

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
import de.ueller.midlet.gps.GuiGpx;
import de.ueller.midlet.gps.Logger;

public class Gpx extends PersistEntity implements Runnable {
	
	private final static Logger logger=Logger.getInstance(Gpx.class,Logger.DEBUG);
	private RecordStore database;
	public int recorded=0;
	private Thread processorThread=null;
	private String url=null;
	
	private ByteArrayOutputStream baos;
	private DataOutputStream dos;
	
	private byte[] trackRecordByteArray;
	
	private String trackName;
	
	private GuiGpx feedbackListener;

	public Gpx() throws RecordStoreFullException, RecordStoreNotFoundException, RecordStoreException {
		database = RecordStore.openRecordStore("GPX", true);
		initTrack();
	}

	public Gpx(String url, byte [] trba, GuiGpx parent) {
					
			if (url == null) {
				logger.error("GPX receiver URL is null");				
				return;
			}
			
			this.url=url;
			feedbackListener = parent;
			
			trackRecordByteArray = trba;
			
			processorThread = new Thread(this,"Names");
			processorThread.setPriority(Thread.MIN_PRIORITY);
			processorThread.start();		
	}
	
	/**
	 * This function adds a single position to the currently active tracklog.
	 * 
	 * @param p - Position p is the position to add 
	 * @return returns a PositionMark every 60 recorded positions
	 * @throws IOException
	 * @throws RecordStoreNotOpenException
	 * @throws RecordStoreFullException
	 * @throws RecordStoreException
	 */
	public PositionMark addPosition(Position p) throws IOException, RecordStoreNotOpenException, RecordStoreFullException, RecordStoreException{		
		try {
			dos.writeFloat(p.latitude);
			dos.writeFloat(p.longitude);
			dos.writeFloat(p.altitude);
			dos.writeLong(p.date.getTime());
			dos.writeFloat(p.speed);		
			recorded++;
			if (recorded % 60 == 0){
				return new PositionMark(p.latitude,p.longitude);
			} else {
				return null;
			}
		} catch (OutOfMemoryError oome) {
			logger.fatal("Out of memory, can't add trackpoint");
			return null;
		}
	}
	
	private void initTrack() {
		Date today = new Date();		
		trackName = today.toString();
		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);
		recorded = 0;
	}
	
	/**
	 * Commit the track log from an in-memory buffer to the
	 * RecordStore
	 */
	private void commitTrackToDb() {
		try {
			dos.flush();		
			ByteArrayOutputStream baosDb = new ByteArrayOutputStream();
			DataOutputStream dosDb = new DataOutputStream(baosDb);
			dosDb.writeUTF(trackName);
			dosDb.writeInt(recorded);
			dosDb.writeInt(baos.size());
			dosDb.write(baos.toByteArray());
			dosDb.flush();
			database.addRecord(baosDb.toByteArray(), 0, baosDb.size());
			
		} catch (IOException e) {
			logger.error("IOE: " + e.getMessage());
		} catch (RecordStoreNotOpenException e) {
			logger.error("RSNOE: " + e.getMessage());
		} catch (RecordStoreFullException e) {
			logger.error("RSFE: " + e.getMessage());
		} catch (RecordStoreException e) {
			logger.error("RSE: " + e.getMessage());
		} catch (OutOfMemoryError oome) {
			logger.fatal("Out of memory, can't save tracklog");			
		}
		//Reinitialise bufferes for the next position to be received in a new track
		initTrack();
	}

	public void close() {
		try {
			commitTrackToDb();
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

	
	
	public static void transfer(String gpxUrl, byte[] trba, GuiGpx parent) {		
		new Gpx(gpxUrl, trba, parent);		
	}

	public void run() {
		try {			
			DataInputStream dis1 = getByteInputStream(trackRecordByteArray);
			trackName = dis1.readUTF();
			recorded = dis1.readInt();
			int trackSize = dis1.readInt();
			byte[] trackArray = new byte[trackSize];
			dis1.read(trackArray);
			DataInputStream trackIS = getByteInputStream(trackArray);
			
			logger.trace("Starting to send a GPX file, about to open a connection to" + url);
			logger.info(new String(baos.toByteArray()));
			ClientSession session = (ClientSession)Connector.open(url);						
			HeaderSet headers = session.createHeaderSet();	        
			session.connect(headers);
			logger.debug("Connected");
			headers.setHeader(HeaderSet.NAME, "export.gpx");
			headers.setHeader(HeaderSet.TYPE, "text");
			Operation operation;
			operation = session.put(headers);			
			OutputStream oS = operation.openOutputStream();
			
			write(oS,"<?xml version='1.0' encoding='UTF-8'?>\r\n");
			write(oS,"<gpx version='1.1' creator='GPSMID' xmlns='http://www.topografix.com/GPX/1/1'>\r\n");
			write(oS,"<trk>\r\n<trkseg>\r\n");						
			
			for (int i = 1; i <= recorded; i++) {
				StringBuffer sb = new StringBuffer(128);
				sb.append("<trkpt lat='").append(trackIS.readFloat()).append("' lon='").append(trackIS.readFloat()).append("' >\r\n");
				sb.append("<ele>").append(trackIS.readFloat()).append("</ele>\r\n");
				sb.append("<time>").append(formatUTC(new Date(trackIS.readLong()))).append("</time>\r\n");
				sb.append("</trkpt>\r\n");				
				// Read extra bytes in the buffer, that are currently not written to the GPX file.
				// Will add these at a later time.
				trackIS.readFloat(); //Speed
				write(oS,sb.toString());
			}
			write(oS,"</trkseg>\r\n</trk>\r\n</gpx>\r\n\r\n");			
			
			oS.flush();
			oS.close();			
			session.close();
			
			int code = operation.getResponseCode();
			if (code == ResponseCodes.OBEX_HTTP_OK) {				
				logger.info("Successfully transfered file");				
			} else {
				logger.error("Unsuccessful return code in Opex push: " + code);
			}
			feedbackListener.completedUpload();
		} catch (IOException e) {			
			logger.error("IOE:" + e);	
		} catch (OutOfMemoryError oome) {
			logger.fatal("Out of memory, can't transmit tracklogs");
		} catch (Exception ee) {			
			logger.error("Error while sending tracklogs: " + ee);
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
	
	/**
	 * Formats an integer to 2 digits, as used for example in time.
	 * I.e. a 0 gets printed as 00. 
	 **/
	private static final String formatInt2(int n) {
		if (n < 10) {
			return "0" + n;
		} else {
			return Integer.toString(n);
		}
			
	}
	
	/**
	 * Date-Time formater that corresponds to the standard UTC time as used in XML
	 * @param time
	 * @return
	 */
	private static final String formatUTC(Date time) {
		// This function needs optimising. It has a too high object churn.
		Calendar c = null;
		if (c == null)
			c = Calendar.getInstance();
		c.setTime(time);
		return c.get(Calendar.YEAR) + "-" + formatInt2(c.get(Calendar.MONTH)) + "-" +
		formatInt2(c.get(Calendar.DAY_OF_MONTH)) + "T" + formatInt2(c.get(Calendar.HOUR_OF_DAY)) + ":" +
		formatInt2(c.get(Calendar.MINUTE)) + ":" + formatInt2(c.get(Calendar.SECOND)) + "Z";		 
		
	}
}
